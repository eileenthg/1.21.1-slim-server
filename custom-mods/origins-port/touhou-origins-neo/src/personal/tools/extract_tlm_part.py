#!/usr/bin/env python3
"""Pull a named bone subtree out of a Touhou Little Maid model and re-root it onto a
vanilla player part, so only that feature renders on the player.

Why this exists
---------------
`apoli:custom_model_render` in GEOMETRY mode renders a whole Bedrock model over the player.
Rendering a full TLM character replaces the player's look entirely; attaching only the
identifiable pieces (oni horns, satori's third eye and its cables) keeps the player's own skin
and reads as an origin trait rather than a costume.

How it works
------------
`GeometryRenderer.syncPlayer` binds bones *named* head/body/rightarm/leftarm/rightleg/leftleg to
the matching player part, adding that part's deviation from a rest pose. So a model containing a
bone named `head` with no cubes of its own acts as a tracker: whatever hangs off it follows the
player's head.

Two coordinate details, both read from Apoli's BedrockModelParser rather than assumed:

* Bedrock pivots are absolute; the parser converts them to offsets against the parent's pivot,
  with root bones measured from BEDROCK_FLOOR = y 24.
* Apoli scans `geo/` and `models/apoli/` but only accepts files ending `.geo.json`. Write output
  under `assets/<ns>/models/apoli/` -- the `geo/` prefix also gets scanned by GeckoLib, which
  warns about every file there that is not its own.
* TLM's maids are chibi: their head pivot sits at y 18 and body at y 10.5, where a vanilla
  player has both at y 24. So every pivot and cube origin in the subtree is lifted by the
  difference, or the part renders sunk into the player.

The lift aligns pivots; it does not match proportions. A chibi is roughly 24 units tall against a
vanilla player's 32, so a piece that sat at chest height on a maid can arrive at the neck. Use
`--offset` to place it where it belongs -- the third eye needs about -12.5 on Y, which very nearly
cancels the body lift, because the eye is positioned against the character's whole torso rather
than against the body pivot. Sizes usually want a tweak too, via `--scale` or `scale` on the power.

Useful reference points on a vanilla player, in Bedrock units: feet 0, waist 12, neck 24, top of
head 32. The tracker's pivot sits at the neck for both `head` and `body`.

Usage
-----
    python extract_tlm_part.py <tlm-model.json> <out.geo.json> --track head --roots hornLeft hornRight

Reads TLM's own model files, either era, so nothing needs converting first. Only the extracted
part is written into the mod, which also keeps borrowed assets down to what is actually used.
"""

import argparse
import json
import sys

# Where a vanilla player's parts sit in Bedrock coordinates (BEDROCK_FLOOR is y 24, and vanilla
# HumanoidModel places head, body and both arms at PartPose.offset(0, 0, 0)).
VANILLA_PIVOT = {
    "head": [0.0, 24.0, 0.0],
    "body": [0.0, 24.0, 0.0],
    "rightarm": [-5.0, 22.0, 0.0],
    "leftarm": [5.0, 22.0, 0.0],
    "rightleg": [-1.9, 12.0, 0.0],
    "leftleg": [1.9, 12.0, 0.0],
}


def read_geometry(src):
    """TLM's pack mixes eras: some models are modern `minecraft:geometry`, others are 1.10-era
    `geometry.model` with `texturewidth`/`textureheight`. Apoli parses only the modern shape, so
    normalise here and read TLM's own files directly rather than keeping converted copies around."""
    if "minecraft:geometry" in src:
        return src["minecraft:geometry"][0]
    legacy = src.get("geometry.model")
    if legacy is None:
        sys.exit("error: unrecognised model format (no minecraft:geometry or geometry.model)")
    return {
        "description": {
            "texture_width": legacy.get("texturewidth", 64),
            "texture_height": legacy.get("textureheight", 64),
        },
        "bones": legacy.get("bones", []),
    }


def collect(bones, roots):
    """Every bone in the subtrees rooted at `roots`, parents first."""
    children = {}
    for b in bones:
        children.setdefault(b.get("parent"), []).append(b)
    by_name = {b["name"]: b for b in bones}

    out, seen = [], set()

    def walk(name):
        if name in seen or name not in by_name:
            return
        seen.add(name)
        out.append(by_name[name])
        for child in children.get(name, []):
            walk(child["name"])

    for r in roots:
        if r not in by_name:
            sys.exit(f"error: '{r}' is not a bone in this model")
        walk(r)
    return out


def shift(bone, delta, scale, inflate):
    def move(p):
        return [round((p[i] + delta[i]) * scale, 5) for i in range(3)]

    if "pivot" in bone:
        bone["pivot"] = move(bone["pivot"])
    for cube in bone.get("cubes", []):
        if "origin" in cube:
            cube["origin"] = move(cube["origin"])
        if "pivot" in cube:
            cube["pivot"] = move(cube["pivot"])
        if scale != 1.0 and "size" in cube:
            cube["size"] = [round(v * scale, 5) for v in cube["size"]]
        if inflate:
            cube["inflate"] = round(cube.get("inflate", 0.0) + inflate, 5)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("source")
    ap.add_argument("out")
    ap.add_argument("--track", required=True, choices=sorted(VANILLA_PIVOT),
                    help="vanilla player part the extracted piece should follow")
    ap.add_argument("--roots", nargs="+", required=True, help="bone names to extract, with their children")
    ap.add_argument("--scale", type=float, default=1.0, help="bake a uniform scale into the geometry")
    ap.add_argument("--offset", type=float, nargs=3, metavar=("X", "Y", "Z"), default=[0.0, 0.0, 0.0],
                    help="extra shift applied after the pivot lift, in Bedrock units (+Y is up). "
                         "Aligning pivots is not the same as matching proportions, so a piece that "
                         "sat at chest height on a chibi can land at the neck on a vanilla player; "
                         "this is the dial for that")
    ap.add_argument("--drop-root-rotation", action="store_true",
                    help="zero the rotation on the extracted root bones. TLM parks some parts at a "
                         "large rotation because a driver bone re-aims them at runtime; kept "
                         "statically, that rotation swings the geometry away about a pivot that may "
                         "sit far from it. Children keep their rotations, since those build the shape")
    ap.add_argument("--inflate", type=float, default=0.0,
                    help="grow every cube by this on all sides. The usual reason is the vanilla "
                         "skin's hat/jacket overlay, which is the body box extended by 0.5 - a part "
                         "sitting exactly on that boundary has coplanar faces with it and z-fights. "
                         "Anything over 0.5 clears the overlay")
    ap.add_argument("--identifier", default=None)
    args = ap.parse_args()

    src = json.load(open(args.source, encoding="utf-8"))
    geo = read_geometry(src)

    bones = geo["bones"]
    by_name = {b["name"]: b for b in bones}

    # The subtree's pivots are absolute in TLM space. Lift them so the part that used to hang off
    # TLM's chibi head/body hangs off the vanilla player's instead.
    anchor = args.track
    if anchor not in by_name:
        # roots may hang off a bone that itself sits under the tracked part
        parent = by_name[args.roots[0]].get("parent")
        anchor = parent if parent in by_name else None
    if anchor is None:
        sys.exit(f"error: cannot find '{args.track}' or the roots' parent in the source model")

    src_pivot = by_name[anchor]["pivot"]
    dst_pivot = VANILLA_PIVOT[args.track]
    delta = [dst_pivot[i] - src_pivot[i] + args.offset[i] for i in range(3)]

    extracted = json.loads(json.dumps(collect(bones, args.roots)))
    for b in extracted:
        shift(b, delta, args.scale, args.inflate)
        if args.drop_root_rotation and b["name"] in args.roots:
            b.pop("rotation", None)

    # A cube-less tracker bone named after the vanilla part. syncPlayer binds it by name; the
    # extracted roots become its children.
    tracker = {"name": args.track, "pivot": [round(v * args.scale, 5) for v in dst_pivot]}
    for b in extracted:
        if b["name"] in args.roots:
            b["parent"] = args.track

    desc = geo.get("description", {})
    out = {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": args.identifier or "geometry." + args.out.replace("\\", "/").split("/")[-1][:-5],
                "texture_width": desc.get("texture_width", 64),
                "texture_height": desc.get("texture_height", 64),
            },
            "bones": [tracker] + extracted,
        }],
    }
    with open(args.out, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(out, fh, indent=1)
        fh.write("\n")

    cubes = sum(len(b.get("cubes", [])) for b in extracted)
    print(f"{args.out}: tracker '{args.track}' + {len(extracted)} bones, {cubes} cubes, "
          f"lifted by {delta}" + (f", scaled {args.scale}" if args.scale != 1.0 else "")
          + (f", inflated {args.inflate}" if args.inflate else ""))


if __name__ == "__main__":
    main()
