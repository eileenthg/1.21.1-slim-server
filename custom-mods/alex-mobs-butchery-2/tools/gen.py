"""Generates Part 2's assets, data and Java tables from the Alex's Mobs models.

    python gen.py <decompiled-models-dir> <alexsmobs-jar-extract> [--preview out.png]

Writes into ../src/main/resources and ../src/main/java/dev/eteil/butcheryam2/gen.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import os
import re
import shutil
import sys

import numpy as np
from PIL import Image

import ammodel as am
import convert as cv
import mobs as M
import mobs_amcd as A
import mobs_rest as RST

MODID = "butchery_alexs_mobs_2"
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
RES = os.path.join(ROOT, "src", "main", "resources")
JAVA_GEN = os.path.join(ROOT, "src", "main", "java", "dev", "eteil", "butcheryam2", "gen")
ASSETS = os.path.join(RES, "assets", MODID)
DATA = os.path.join(RES, "data", MODID)

# Ground stages 0,6,7,8,9 and hanging stages 1..5 (see Part 1 / Butchery).
FULL_STAGES = ["ground_whole", "hanging_whole", "hanging_headless", "hanging_skinned",
               "hanging_cut1", "hanging_cut2", "ground_headless", "ground_skinned",
               "ground_cut1", "ground_cut2"]
STAGED_STAGES = ["ground_whole", "hanging_whole", "hanging_s1", "hanging_s2", "hanging_whole",
                 "hanging_whole", "ground_s1", "ground_s2", "ground_whole", "ground_whole"]


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


# ---------------------------------------------------------------- textures

def skinned_texture(src: Image.Image, seed: str) -> Image.Image:
    """Pale, fat-marbled flesh in the style of Part 1's skinned textures, keeping the alpha."""
    a = np.array(src.convert("RGBA")).astype(float)
    rgb, alpha = a[..., :3], a[..., 3]
    lum = rgb @ np.array([0.299, 0.587, 0.114])
    opaque = alpha > 0
    mean, std = lum[opaque].mean(), max(lum[opaque].std(), 1.0)
    h, w = lum.shape
    rng = np.random.default_rng(int(hashlib.md5(seed.encode()).hexdigest()[:8], 16))
    noise = rng.random((h, w))
    # soft vertical streaking so the marbling reads as grain rather than static
    streak = np.cumsum(rng.normal(0, 0.35, (h, w)), axis=0)
    streak = (streak - streak.min()) / max(np.ptp(streak), 1e-9)
    t = np.clip(0.35 + (lum - mean) / std * -0.12 + (noise - 0.5) * 0.2 + (streak - 0.5) * 0.6, 0, 1)
    fat = np.array([244, 231, 230], float)
    meat = np.array([222, 158, 156], float)
    out = fat * (1 - t[..., None]) + meat * t[..., None]
    red = (noise > 0.993) & opaque
    out[red] = np.array([190, 62, 60], float)
    res = np.dstack([np.clip(out, 0, 255), alpha]).astype(np.uint8)
    return Image.fromarray(res, "RGBA")


def ghost_texture(src: Image.Image, seed: str) -> Image.Image:
    """A pale, faintly blue-white body in place of clothing (the murmur without its kimono)."""
    a = np.array(src.convert("RGBA")).astype(float)
    rgb, alpha = a[..., :3], a[..., 3]
    lum = rgb @ np.array([0.299, 0.587, 0.114])
    t = np.clip(lum / 255.0, 0, 1)[..., None]
    pale = np.array([214, 224, 236], float) * (0.78 + 0.22 * t)
    res = np.dstack([np.clip(pale, 0, 255), alpha]).astype(np.uint8)
    return Image.fromarray(res, "RGBA")


TEXTURE_MAKERS = {"skinned": skinned_texture, "ghost": ghost_texture}


# ---------------------------------------------------------------- model building

class MobModel:
    def __init__(self, key, cfg, src_dir, am_dir):
        self.key, self.cfg = key, cfg
        self.model = am.parse(os.path.join(src_dir, cfg["model"] + ".java"), cfg["model"])
        for cls, prefix, offset in cfg.get("compose", []):
            extra = am.parse(os.path.join(src_dir, cls + ".java"), cls)
            for p in extra.parts.values():
                p.name = f"{prefix}_{p.name.strip()}"
                self.model.parts[p.name] = p
            for r in extra.roots:
                r.pivot = tuple(a + b for a, b in zip(r.pivot, offset))
                self.model.roots.append(r)
        self.tex_path = os.path.join(am_dir, "assets", "alexsmobs", "textures", "entity", cfg["texture"] + ".png")
        self.tex = np.array(Image.open(self.tex_path).convert("RGBA"))
        self.alpha = self.tex[..., 3]
        self.rs = cfg.get("render_scale", 1.0)
        excl = set()
        for p in cfg.get("exclude", []):
            excl |= set(self.model.subtree(p))
        self.all_parts = set(self.model.parts) - excl

    def parts_without(self, *groups):
        drop = set()
        for g in groups:
            for p in g:
                drop |= set(self.model.subtree(p))
        return self.all_parts - drop

    def boxes(self, include):
        return list(am.walk(self.model, include=include))

    def pose(self, kind):
        vis = cv.visible(self.boxes(self.all_parts), self.model.tex_w, self.model.tex_h, self.alpha)
        pts = cv.all_points(vis, self.rs)
        upright = self.cfg.get(f"{kind}_pose") == "upright"
        if kind == "hanging":
            # hung from one point, where the head or neck meets the body, as Butchery hangs its mobs
            at = self.cfg.get("hang_at", self.cfg.get("head"))
            anchor = None
            if at:
                anchor = cv.entity_point(am.part_matrix(self.model, at), np.zeros(3), self.rs)
            return cv.fit_hanging("upright" if upright else "hanging", pts, anchor)
        if upright:
            return cv.fit_pose("upright", pts, dict(cv.LIMITS["ground"], size=(30, 30, 30)))
        return cv.fit_pose(kind, pts, cv.LIMITS[kind])

    def elements(self, include, pose, tex_key="tex"):
        return cv.convert_boxes(self.boxes(include), pose, self.rs, self.model.tex_w, self.model.tex_h,
                                self.alpha, tex_key)


def display_for(elements):
    """Item display transforms that fit and centre an oversized model."""
    pts = []
    for el in elements:
        pts += [el["from"], el["to"]]
    pts = np.array(pts)
    lo, hi = pts.min(0), pts.max(0)
    ext = float((hi - lo).max())
    centre = (lo + hi) / 2
    f = min(1.0, 16.0 / max(ext, 1e-6))

    def entry(rot, base_scale, extra=(0, 0, 0)):
        s = base_scale * f
        r = (am.rot_x(math.radians(rot[0])) @ am.rot_y(math.radians(rot[1])) @ am.rot_z(math.radians(rot[2])))
        t = -(r @ ((centre - 8) * s)) + np.array(extra)
        return {"rotation": list(rot), "translation": [round(float(x), 3) for x in np.clip(t, -80, 80)],
                "scale": [round(s, 4)] * 3}

    return {
        "gui": entry((30, 225, 0), 0.625),
        "ground": entry((0, 0, 0), 0.25, (0, 3, 0)),
        "fixed": entry((0, 0, 0), 0.5),
        "head": entry((0, 0, 0), 1.0),
        "thirdperson_righthand": entry((75, 45, 0), 0.375, (0, 2.5, 0)),
        "thirdperson_lefthand": entry((75, 45, 0), 0.375, (0, 2.5, 0)),
        "firstperson_righthand": entry((0, 45, 0), 0.4),
        "firstperson_lefthand": entry((0, 225, 0), 0.4),
    }


def block_model(elements, texture):
    return {
        "ambientocclusion": False,
        "render_type": "minecraft:cutout",
        "textures": {"tex": texture, "particle": "minecraft:block/nether_wart_block"},
        "display": display_for(elements),
        "elements": elements,
    }


def merged_boxes(elements, limit=6):
    """A few AABBs covering the model, for the block's outline and collision shape."""
    boxes = []
    for el in elements:
        frm, to = np.array(el["from"]), np.array(el["to"])
        corners = np.array([[x, y, z] for x in (frm[0], to[0]) for y in (frm[1], to[1]) for z in (frm[2], to[2])])
        rot = el.get("rotation")
        if rot:
            r = cv.AXES[rot["axis"]](math.radians(rot["angle"]))
            o = np.array(rot["origin"])
            corners = (corners - o) @ r.T + o
        lo, hi = corners.min(0), corners.max(0)
        hi = np.maximum(hi, lo + 0.5)  # flat planes still need some thickness
        boxes.append([lo, hi])

    def vol(b):
        return float(np.prod(b[1] - b[0]))

    while len(boxes) > limit:
        best = None
        for i in range(len(boxes)):
            for j in range(i + 1, len(boxes)):
                lo = np.minimum(boxes[i][0], boxes[j][0])
                hi = np.maximum(boxes[i][1], boxes[j][1])
                cost = vol([lo, hi]) - vol(boxes[i]) - vol(boxes[j])
                if best is None or cost < best[0]:
                    best = (cost, i, j, [lo, hi])
        _, i, j, m = best
        boxes = [b for k, b in enumerate(boxes) if k not in (i, j)] + [m]
    out = []
    for lo, hi in boxes:
        lo = np.clip(lo, -16, 32)
        hi = np.clip(hi, -16, 32)
        out.append([round(float(v), 2) for v in (*lo, *hi)])
    return out


# ---------------------------------------------------------------- loot

def loot_table(name, drops):
    pools = []
    for d in drops:
        item, lo, hi = d[:3]
        if item.startswith("#"):
            # an optional mod's item, through a tag with required: false (e.g. Eternal Tales' ectoplasm)
            entry = {"type": "minecraft:tag", "name": item[1:], "expand": False}
        else:
            entry = {"type": "minecraft:item", "name": item}
        if (lo, hi) != (1, 1):
            count = lo if lo == hi else {"type": "minecraft:uniform", "min": lo, "max": hi}
            entry["functions"] = [{"function": "minecraft:set_count", "count": count}]
        pool = {"rolls": 1, "entries": [entry]}
        if len(d) > 3:
            pool["conditions"] = [{"condition": "minecraft:random_chance", "chance": d[3]}]
        pools.append(pool)
    return {"type": "minecraft:block", "pools": pools, "random_sequence": f"{MODID}:blocks/{name}"}


def block_drop_table(name, item):
    return {
        "type": "minecraft:block",
        "random_sequence": f"{MODID}:blocks/{name}",
        "pools": [{"rolls": 1.0, "conditions": [{"condition": "minecraft:survives_explosion"}],
                   "entries": [{"type": "minecraft:item", "name": item}]}],
    }


# ---------------------------------------------------------------- blockstates

def blockstate(model_for_stage):
    """model_for_stage: list of model names per `blockstate` value, or a single name."""
    variants = {}
    facings = [("north", 0), ("east", 90), ("south", 180), ("west", 270)]
    if isinstance(model_for_stage, str):
        for f, y in facings:
            v = {"model": f"{MODID}:block/{model_for_stage}"}
            if y:
                v["y"] = y
            variants[f"facing={f}"] = v
    else:
        for stage, model in enumerate(model_for_stage):
            for f, y in facings:
                v = {"model": f"{MODID}:block/{model}"}
                if y:
                    v["y"] = y
                variants[f"blockstate={stage},facing={f}"] = v
    return {"variants": variants}


# ---------------------------------------------------------------- segmented (BER)

def java_layer(model: am.Model, method: str) -> str:
    """A vanilla LayerDefinition reproducing one body piece, for the block-entity renderer."""
    lines = [f"    public static LayerDefinition {method}() {{",
             "        MeshDefinition mesh = new MeshDefinition();",
             "        PartDefinition p0 = mesh.getRoot();"]
    counter = [0]

    def emit(part, parent_var):
        counter[0] += 1
        var = f"p{counter[0]}"
        cubes = "CubeListBuilder.create()"
        for b in part.boxes:
            cubes += f".texOffs({b.u}, {b.v}).mirror({str(b.mirror).lower()})" \
                     f".addBox({b.x}F, {b.y}F, {b.z}F, {b.w}F, {b.h}F, {b.d}F, new CubeDeformation({b.inflate}F))"
        px, py, pz = part.pivot
        rx, ry, rz = part.rot
        lines.append(f"        PartDefinition {var} = {parent_var}.addOrReplaceChild(\"{part.name.strip()}\", {cubes},")
        lines.append(f"                PartPose.offsetAndRotation({px}F, {py}F, {pz}F, {rx}F, {ry}F, {rz}F));")
        for c in part.children:
            emit(c, var)

    for r in model.roots:
        emit(r, "p0")
    lines.append(f"        return LayerDefinition.create(mesh, {model.tex_w}, {model.tex_h});")
    lines.append("    }")
    return "\n".join(lines)


def segment_flip(on_back):
    """Entity renderers flip models with scale(-1,-1,1) to stand them up; skipping that flip
    turns the piece over, onto its back."""
    return np.eye(3) if on_back else np.diag([-1.0, -1.0, 1.0])


def segment_lift(body_model, on_back):
    """Model pixels that raise a body piece so it rests on the ground."""
    flip = segment_flip(on_back)
    ys = [(flip @ (m @ np.append(v, 1))[:3])[1]
          for _, b, m in am.walk(body_model) for verts, _, _ in am.box_quads(b, 1, 1) for v in verts]
    return -min(ys)


def piece_sizes(spec):
    """Relative size of each piece, head first: Alex's Mobs grows void worm segments along the body."""
    n = max_pieces(spec)
    grow, tail_scale = spec.get("grow", 0.0), spec.get("tail_scale", 1.0)
    tails = sum(1 for p in spec["layout"] if p == "tail")
    sizes = [1.0]
    for i in range(1, n):
        s = 1.0 + grow * (i - 1) / max(n - 1, 1)
        if i >= n - tails:
            s *= tail_scale
        sizes.append(s)
    return sizes


def coil_layout(pieces, spacing_px, scale, r0, r1, sizes=None):
    """Coils the body on the ground: (x, z, yaw_degrees) per piece in block pixels, head first."""
    sizes = sizes or [1.0] * pieces
    out, angle = [], 0.0
    for i in range(pieces):
        r = r0 + (r1 - r0) * i / max(pieces - 1, 1)
        x, z = 8 + r * math.cos(angle), 8 + r * math.sin(angle)
        # face along the coil towards the head; Ry(yaw) turns the model's forward (-z) onto
        # the tangent (-sin a, cos a), which needs yaw = 180 - a
        yaw = 180.0 - math.degrees(angle)
        out.append((round(x, 3), round(z, 3), round(yaw, 3)))
        step_size = (sizes[i] + sizes[min(i + 1, pieces - 1)]) / 2
        angle -= spacing_px * scale * step_size / r
    return out


def max_pieces(spec):
    n = 0
    for p in spec["layout"]:
        n += spec.get("max_count", 1) if p.startswith("*") else 1
    return n



# ---------------------------------------------------------------- large (BER, whole model)

LARGE_MAX_PX = 128.0  # at most 8 blocks long, like Butchery's full-size Ender Dragon carcass


def quat_from_matrix(r):
    """(x, y, z, w) of a rotation matrix."""
    t = np.trace(r)
    if t > 0:
        s = math.sqrt(t + 1.0) * 2
        return ((r[2, 1] - r[1, 2]) / s, (r[0, 2] - r[2, 0]) / s, (r[1, 0] - r[0, 1]) / s, 0.25 * s)
    i = int(np.argmax(np.diag(r)))
    j, k = (i + 1) % 3, (i + 2) % 3
    s = math.sqrt(1.0 + r[i, i] - r[j, j] - r[k, k]) * 2
    q = [0.0, 0.0, 0.0, 0.0]
    q[i] = 0.25 * s
    q[j] = (r[j, i] + r[i, j]) / s
    q[k] = (r[k, i] + r[i, k]) / s
    q[3] = (r[k, j] - r[j, k]) / s
    return tuple(q)


def part_path(model, name):
    chain, p = [], model.parts[name]
    while p is not None:
        chain.append(p.name.strip())
        p = p.parent
    return "/".join(reversed(chain))


def large_steps(cfg):
    """The cut sequence of a renderer-drawn carcass: (tool, loot suffix, organs, hide, skinned)."""
    if cfg["kind"] == RST.STEPS:
        return [(s["tool"], f"step_{i + 1}_drop", s["organs"], s["hide"], bool(s["texture"]))
                for i, s in enumerate(cfg["steps"])]
    if cfg["kind"] == M.FULL:
        return [("CLEAVER", "head_drop", False, [cfg["head"]], False),
                ("KNIFE", "skin_drop", False, [], True),
                ("CLEAVER", "cut_1_drop", False, cfg["cut1"], True),
                ("CLEAVER", "cut_2_drop", False, cfg["cut2"], True),
                ("CLEAVER", "cut_3_drop", True, [], True)]
    skinned = bool(cfg.get("stage2_skinned"))
    return [("CLEAVER", "cut_1_drop", False, cfg["stage1"], False),
            ("KNIFE", "peel_drop", False, cfg["stage2"], skinned),
            ("CLEAVER", "cut_2_drop", bool(cfg.get("organs")), [], skinned)]


def large_look(cfg, mm):
    """Where the whole model lies (on its side, centred, on the ground) and how it comes apart."""
    vis = cv.visible(mm.boxes(mm.all_parts), mm.model.tex_w, mm.model.tex_h, mm.alpha)
    pts = cv.all_points(vis, mm.rs)
    rot = cv.POSE_ROT["upright" if cfg.get("ground_pose") == "upright" else "ground"]
    p = pts @ rot.T
    lo, hi = p.min(0), p.max(0)
    k = min(1.0, LARGE_MAX_PX / float((hi - lo).max()))
    off = np.array([8 - k * (lo[0] + hi[0]) / 2, 0.05 - k * lo[1], 8 - k * (lo[2] + hi[2]) / 2])
    steps, hidden = [], []
    for tool, loot, organs, hide, skinned in large_steps(cfg):
        hidden = hidden + [part_path(mm.model, h) for h in hide]
        steps.append(dict(tool=tool, loot=loot, organs=organs, hide=list(hidden), skinned=skinned))
    always = [part_path(mm.model, e) for e in cfg.get("exclude", [])]
    return dict(offset=off, quat=quat_from_matrix(rot @ cv.ENTITY_FLIP), scale=k * mm.rs, steps=steps,
                always=always, extent=float(max(hi[0] - lo[0], hi[2] - lo[2]) * k))

# ---------------------------------------------------------------- main

def validate_items(ids, known):
    missing = sorted(i for i in ids if i not in known and not i.startswith(("@", "#")))
    if missing:
        raise SystemExit(f"unknown item ids in drops: {missing}")


def entry_for(key, cfg, kind, head):
    """One row of GeneratedMobs: variant match and death extras included."""
    match = cfg.get("match")
    if match is None and "nbt" in cfg:
        match = ("nbt", cfg["nbt"][0], "==", cfg["nbt"][1])
    extra = (cfg.get("death_extras") or {}).get("when")
    return dict(name=key, label=cfg["name"], kind=kind, head=head, match=match, extra=extra,
                has_extra=bool(cfg.get("death_extras")))


OPTIONAL_ITEM_TAGS = {"ectoplasm": ["eternal_tales:ectoplasm"], "arthropod": ["minersdelight:arthropod"]}

# this mod's own items: (id, name, texture maker)
ITEMS = {
    "warped_mosco_proboscis": "Warped Mosco Proboscis",
    "insect_wing": "Insect Wing",
}
# interchangeable items share a tag; recipes that took the original take the tag instead
SHARED_TAGS = {
    "proboscises": ["alexsmobs:mosquito_proboscis", "butchery_alexs_mobs_2:warped_mosco_proboscis"],
    "insect_wings": ["butchery_alexs_mobs_2:insect_wing", "butchery:bee_wings"],
}
RETAG_RECIPES = {"alexsmobs:mosquito_proboscis": "proboscises"}


def recolor(img: Image.Image, hue_from, hue_to, width=0.12, sat=None, light=None):
    """Moves pixels near one hue to another, keeping shading (red blood -> blue)."""
    import colorsys
    a = np.array(img.convert("RGBA")).astype(float) / 255.0
    out = a.copy()
    h, w = a.shape[:2]
    for y in range(h):
        for x in range(w):
            r, gg, b, al = a[y, x]
            if al == 0:
                continue
            hh, ll, ss = colorsys.rgb_to_hls(r, gg, b)
            d = min(abs(hh - hue_from), 1 - abs(hh - hue_from))
            if d <= width and ss > 0.15:
                nh = (hue_to + (hh - hue_from)) % 1.0
                ns = ss if sat is None else sat
                nl = ll if light is None else light(ll)
                out[y, x, :3] = colorsys.hls_to_rgb(nh, nl, ns)
    return Image.fromarray((np.clip(out, 0, 1) * 255).astype(np.uint8), "RGBA")


def single_wing_texture() -> Image.Image:
    """One veined insect wing, drawn in Butchery's bee-wing palette: dark brown rim, pale membrane."""
    size = 16
    out = np.zeros((size, size, 4), np.uint8)
    rim, vein = (74, 45, 28, 255), (98, 64, 40, 255)
    light, mid, deep = (196, 226, 232, 255), (160, 200, 210, 255), (128, 172, 186, 255)
    # a long teardrop from the root (lower left) to the tip (upper right)
    ax, ay, bx, by = 2.5, 13.5, 13.5, 2.5
    inside = np.zeros((size, size), bool)
    for y in range(size):
        for x in range(size):
            px, py = x + 0.5, y + 0.5
            dx, dy = bx - ax, by - ay
            t = ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)
            if not 0 <= t <= 1:
                continue
            cx, cy = ax + t * dx, ay + t * dy
            half = 0.6 + 3.4 * math.sin(math.pi * min(1.0, t * 1.15)) ** 0.8
            if math.hypot(px - cx, py - cy) <= half:
                inside[y, x] = True
    for y in range(size):
        for x in range(size):
            if not inside[y, x]:
                continue
            edge = any(not (0 <= x + ox < size and 0 <= y + oy < size and inside[y + oy, x + ox])
                       for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            on_axis = abs((x - 2) - (13 - y)) <= 0 and 3 <= x <= 12  # the main vein
            branch = (x + y) % 5 == 0 and abs((x - 2) - (13 - y)) <= 2 and 5 <= x <= 11
            if edge:
                out[y, x] = rim
            elif on_axis:
                out[y, x] = vein
            elif branch:
                out[y, x] = deep
            else:
                out[y, x] = light if (x - y) > 2 else mid
    return Image.fromarray(out, "RGBA")


def head_sprite(els, tex):
    """A 16x16 portrait of a head model, as Butchery's Ender Dragon carcass item shows its head."""
    import preview as pv
    key = (255, 0, 255)
    lo = np.min([e["from"] for e in els], 0)
    hi = np.max([e["to"] for e in els], 0)
    n = 128
    cam = pv.Camera(direction=(1.0, -0.35, 0.3), center=tuple((lo + hi) / 2),
                    ppu=n * 0.8 / float((hi - lo).max()), size=(n, n))
    big = np.array(pv.rasterise(pv.block_quads(els, {"tex": tex}), cam, bg=key).convert("RGB")).astype(float)
    solid = np.abs(big - key).sum(-1) > 30
    k = n // 16
    out = np.zeros((16, 16, 4), np.uint8)
    for y in range(16):
        for x in range(16):
            cell, mask = big[y*k:(y+1)*k, x*k:(x+1)*k], solid[y*k:(y+1)*k, x*k:(x+1)*k]
            if mask.mean() >= 0.4:
                c = cell[mask].mean(0)
                out[y, x] = (*[int(min(255, v * 1.35)) for v in c], 255)
    body = out[..., 3] > 0
    for y in range(16):
        for x in range(16):
            if not body[y, x] and any(0 <= x + ox < 16 and 0 <= y + oy < 16 and body[y + oy, x + ox]
                                      for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                out[y, x] = (16, 12, 28, 255)
    return Image.fromarray(out, "RGBA")


def insect_wing_texture(bee_wings: Image.Image) -> Image.Image:
    """A generic veined insect wing: Butchery's bee wings, made glassy grey-green."""
    import colorsys
    a = np.array(bee_wings.convert("RGBA")).astype(float) / 255.0
    out = a.copy()
    for y in range(a.shape[0]):
        for x in range(a.shape[1]):
            r, gg, b, al = a[y, x]
            if al == 0:
                continue
            hh, ll, ss = colorsys.rgb_to_hls(r, gg, b)
            if ll < 0.35:
                continue  # keep the dark veins and outline
            # pale, faintly iridescent membrane
            out[y, x, :3] = colorsys.hls_to_rgb(0.47 + (ll - 0.5) * 0.1, 0.55 + ll * 0.35, 0.22)
    return Image.fromarray((np.clip(out, 0, 1) * 255).astype(np.uint8), "RGBA")


def emit_items(args, lang, butchery_extract):
    """Item textures (generated from existing art), models, names, tags and retagged recipes."""
    tex_dir = _mk(os.path.join(ASSETS, "textures", "item"))
    prob = Image.open(os.path.join(args.am_extract, "assets", "alexsmobs", "textures", "item", "mosquito_proboscis.png"))
    # crimson (red) blood -> warped (blue) fluid
    recolor(prob, 0.0, 0.58, width=0.09).save(os.path.join(tex_dir, "warped_mosco_proboscis.png"))
    bee = Image.open(os.path.join(butchery_extract, "assets", "butchery", "textures", "item", "bee_wing.png"))
    single_wing_texture().save(os.path.join(tex_dir, "insect_wing.png"))
    for item, name in ITEMS.items():
        write_json(os.path.join(ASSETS, "models", "item", f"{item}.json"),
                   {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MODID}:item/{item}"}})
        lang[f"item.{MODID}.{item}"] = name
    for tag, items in SHARED_TAGS.items():
        write_json(os.path.join(DATA, "tags", "item", f"{tag}.json"),
                   {"replace": False, "values": [{"id": i, "required": False} for i in items]})
    # recipes that name an item now accept anything in its shared tag
    import zipfile
    am_jar_dir = os.path.join(args.am_extract, "data", "alexsmobs", "recipe")
    for fname in sorted(os.listdir(am_jar_dir)):
        path = os.path.join(am_jar_dir, fname)
        text = open(path, encoding="utf-8").read()
        changed = text
        for item, tag in RETAG_RECIPES.items():
            changed = re.sub(r'\{\s*"item"\s*:\s*"%s"\s*\}' % re.escape(item),
                             '{"tag": "%s:%s"}' % (MODID, tag), changed)
        if changed != text:
            write_json(os.path.join(RES, "data", "alexsmobs", "recipe", fname), json.loads(changed))



KIND_NAMES = {M.FULL: "FULL", M.STAGED: "STAGED", M.SINGLE: "SINGLE", RST.STEPS: "STEPS"}
GROUND_CODES = [0, 6, 7, 8, 9]
HANGING_CODES = [1, 2, 3, 4, 5]


def step_texture_kind(cfg):
    kinds = {s["texture"] for s in cfg.get("steps", []) if s.get("texture")}
    if len(kinds) > 1:
        raise SystemExit(f"{cfg['name']}: one texture change per carcass")
    return next(iter(kinds), None)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("models")
    ap.add_argument("am_extract")
    ap.add_argument("--known-items", help="text file of valid item ids, one per line")
    ap.add_argument("--preview")
    ap.add_argument("--only", help="comma-separated mob keys, for quick previews")
    ap.add_argument("--butchery-extract", required=True, help="extracted Butchery jar (for the wing art)")
    args = ap.parse_args()

    only = set(args.only.split(",")) if args.only else None
    if not only:
        for d in (os.path.join(ASSETS, "models"), os.path.join(ASSETS, "blockstates"),
                  os.path.join(ASSETS, "textures", "item"), os.path.join(RES, "data", "alexsmobs", "recipe"),
                  os.path.join(ASSETS, "textures", "block"), os.path.join(DATA, "loot_table"),
                  os.path.join(DATA, "tags", "entity_type"), JAVA_GEN):
            shutil.rmtree(d, ignore_errors=True)

    lang = {f"itemGroup.{MODID}": "Butchery: Alex's Mobs Part 2"}
    atlas_sources = []
    shapes_models = {}
    block_stages = {}
    heads, entity_tags, all_drop_ids = [], {}, set()
    entries = []  # for GeneratedMobs.java
    large_looks = {}
    report = []
    previews = []

    def emit_model(name, elements, texture):
        cv.clamp_check(elements, name)
        write_json(os.path.join(ASSETS, "models", "block", f"{name}.json"), block_model(elements, texture))
        shapes_models[name] = merged_boxes(elements)

    def item_model(block, model):
        write_json(os.path.join(ASSETS, "models", "item", f"{block}.json"), {"parent": f"{MODID}:block/{model}"})

    def emit_head(key, name, model, part, tex_id, alpha, include=None):
        """A severed head: the head part upright, facing the viewer."""
        head = f"{key}_head"
        head_boxes = list(am.walk(model, include=include, start=part))
        pts = cv.all_points(cv.visible(head_boxes, model.tex_w, model.tex_h, alpha), 1.0)
        hpose = cv.fit_pose("head", pts, cv.LIMITS["head"])
        els, err = cv.convert_boxes(head_boxes, hpose, 1.0, model.tex_w, model.tex_h, alpha)
        emit_model(head, els, tex_id)
        write_json(os.path.join(ASSETS, "blockstates", f"{head}.json"), blockstate(head))
        block_stages[head] = [head]
        item_model(head, head)
        lang[f"block.{MODID}.{head}"] = f"{name} Head"
        write_json(os.path.join(DATA, "loot_table", "blocks", f"{head}.json"),
                   block_drop_table(head, f"{MODID}:{head}"))
        heads.append(f"{MODID}:{head}")
        if args.preview:
            previews.append((head, els, tex_id, hpose))
        return err

    def emit_tables(key, tables):
        for suffix, drops in tables.items():
            drops = [(f"{MODID}:{key}_head" if d[0] == "@head" else d[0], *d[1:]) for d in drops]
            all_drop_ids.update(d[0] for d in drops)
            write_json(os.path.join(DATA, "loot_table", "blocks", f"{key}_{suffix}.json"),
                       loot_table(f"{key}_{suffix}", drops))

    def step_tables(key, cfg):
        tables = {}
        for i, s in enumerate(cfg["steps"]):
            if s["drops"]:
                tables[f"step_{i + 1}_drop"] = s["drops"]
            if s.get("fragile"):
                if s["fragile"]["fail_drops"]:
                    tables[f"step_{i + 1}_fail"] = s["fragile"]["fail_drops"]
                all_drop_ids.add(s["fragile"]["item"])
        return tables

    def emit_extras(key, cfg):
        extra = cfg.get("death_extras")
        if extra:
            emit_tables(key, {"death_extra": extra["drops"]})

    def save_skinned(key, mm, kind="skinned"):
        Image.fromarray(np.array(TEXTURE_MAKERS[kind](Image.open(mm.tex_path), key))).save(
            os.path.join(_mk(os.path.join(ASSETS, "textures", "block")), f"{key}_skinned.png"))

    all_mobs = {**M.MOBS, **A.MOBS, **RST.MOBS}
    step_specs = {}  # mob -> step dicts, for StepSpecs.java
    for key, cfg in all_mobs.items():
        if only and key not in only:
            continue
        mm = MobModel(key, cfg, args.models, args.am_extract)
        tex_id = f"alexsmobs:entity/{cfg['texture']}"
        if tex_id not in atlas_sources:
            atlas_sources.append(tex_id)
        skinned_id = f"{MODID}:block/{key}_skinned"
        kind = cfg["kind"]
        poses = {"ground": mm.pose("ground"), "hanging": mm.pose("hanging")}
        worst = 0.0

        def build(stage_name, include, texture):
            nonlocal worst
            pose = poses[stage_name.split("_")[0]]
            els, err = mm.elements(include, pose)
            worst = max(worst, err)
            model_name = f"{key}_{stage_name}"
            emit_model(model_name, els, texture)
            if args.preview and stage_name.endswith(("whole", "cut2", "s2", "skinned")):
                previews.append((model_name, els, texture, pose))
            return model_name

        carcass = f"{key}_carcass"
        entity_tags[key] = dict(entity=cfg["entity"])
        lang[f"block.{MODID}.{carcass}"] = f"{cfg['name']} Carcass"
        write_json(os.path.join(DATA, "loot_table", "blocks", f"{carcass}.json"),
                   block_drop_table(carcass, "minecraft:air"))
        has_head = False

        if cfg.get("large"):
            # drawn by a renderer (Butchery's Ender Dragon scheme); the item shows a shrunk block model
            if kind == M.FULL or cfg.get("stage2_skinned"):
                save_skinned(key, mm)
            elif kind == RST.STEPS and step_texture_kind(cfg):
                save_skinned(key, mm, step_texture_kind(cfg))
            look = large_look(cfg, mm)
            large_looks[key] = (cfg, mm, look)
            write_json(os.path.join(ASSETS, "blockstates", f"{carcass}.json"),
                       {"variants": {"": {"model": f"{MODID}:block/{carcass}"}}})
            write_json(os.path.join(ASSETS, "models", "block", f"{carcass}.json"), {"textures": {"particle": tex_id}})
            icon = build("ground_whole", mm.all_parts, tex_id)
            item_model(carcass, icon)
            if cfg.get("head_block"):
                worst = max(worst, emit_head(key, cfg["name"], mm.model, cfg["head_block"], tex_id, mm.alpha,
                                             mm.all_parts))
                has_head = True
            if kind == RST.STEPS:
                tables = step_tables(key, cfg)
                step_specs[key] = cfg["steps"]
            else:
                d = cfg["drops"]
                tables = ({"head_drop": d["head"], "skin_drop": d["skin"], "cut_1_drop": d["cut1"],
                           "cut_2_drop": d["cut2"], "cut_3_drop": d["cut3"]} if kind == M.FULL else
                          {"cut_1_drop": d["cut1"], "peel_drop": d["peel"], "cut_2_drop": d["cut2"]})
                large_step_list = [dict(tool=t, organs=o, fragile=None) for t, _, o, _, _ in large_steps(cfg)]
                step_specs[key] = [dict(s, loot=l) for s, (_, l, _, _, _) in zip(large_step_list, large_steps(cfg))]
            emit_tables(key, tables)
            emit_extras(key, cfg)
            entries.append(entry_for(key, cfg, "LARGE", has_head))
            report.append(f"{key:26s} drawn x{look['scale']:.2f} ({look['extent'] / 16:.1f} blocks across)")
            continue

        if kind == RST.STEPS:
            tex_kind = step_texture_kind(cfg)
            if tex_kind:
                save_skinned(key, mm, tex_kind)
            ground, hanging = {}, {}
            hidden, textured = [], False
            for i in range(len(cfg["steps"]) + 1):
                if i > 0:
                    hidden = hidden + cfg["steps"][i - 1]["hide"]
                    textured = textured or bool(cfg["steps"][i - 1]["texture"])
                if i == len(cfg["steps"]):
                    break
                include = mm.parts_without(hidden)
                tex = skinned_id if textured else tex_id
                ground[i] = build(f"ground_{i}", include, tex)
                hanging[i] = build(f"hanging_{i}", include, tex)
            if len(cfg["steps"]) > RST.MAX_STEPS:
                raise SystemExit(f"{key}: at most {RST.MAX_STEPS} steps")
            models = [ground[0]] * (2 * RST.MAX_STEPS)
            for i in ground:
                models[2 * i] = ground[i]
                models[2 * i + 1] = hanging[i]
            write_json(os.path.join(ASSETS, "blockstates", f"{carcass}.json"), blockstate(models))
            block_stages[carcass] = models
            item_model(carcass, ground[0])
            tables = step_tables(key, cfg)
            step_specs[key] = cfg["steps"]
        elif kind == M.FULL:
            save_skinned(key, mm)
            headless = mm.parts_without([cfg["head"]])
            cut1 = mm.parts_without([cfg["head"]], cfg["cut1"])
            cut2 = mm.parts_without([cfg["head"]], cfg["cut1"], cfg["cut2"])
            names = {}
            for pose_kind in ("ground", "hanging"):
                names[f"{pose_kind}_whole"] = build(f"{pose_kind}_whole", mm.all_parts, tex_id)
                names[f"{pose_kind}_headless"] = build(f"{pose_kind}_headless", headless, tex_id)
                names[f"{pose_kind}_skinned"] = build(f"{pose_kind}_skinned", headless, skinned_id)
                names[f"{pose_kind}_cut1"] = build(f"{pose_kind}_cut1", cut1, skinned_id)
                names[f"{pose_kind}_cut2"] = build(f"{pose_kind}_cut2", cut2, skinned_id)
            fresh_models = [names["ground_whole"], names["hanging_whole"]]
            drained = f"drained_{key}_carcass"
            drained_models = [names[s] for s in FULL_STAGES]
            write_json(os.path.join(ASSETS, "blockstates", f"{carcass}.json"), blockstate(fresh_models))
            write_json(os.path.join(ASSETS, "blockstates", f"{drained}.json"), blockstate(drained_models))
            block_stages[carcass] = fresh_models
            block_stages[drained] = drained_models
            item_model(carcass, names["ground_whole"])
            item_model(drained, names["ground_whole"])
            lang[f"block.{MODID}.{drained}"] = f"Drained {cfg['name']} Carcass"
            write_json(os.path.join(DATA, "loot_table", "blocks", f"{drained}.json"),
                       block_drop_table(drained, "minecraft:air"))
            worst = max(worst, emit_head(key, cfg["name"], mm.model, cfg["head_block"], tex_id, mm.alpha,
                                         mm.all_parts))
            has_head = True
            d = cfg["drops"]
            tables = {"head_drop": d["head"], "skin_drop": d["skin"], "cut_1_drop": d["cut1"],
                      "cut_2_drop": d["cut2"], "cut_3_drop": d["cut3"]}
        elif kind == M.STAGED:
            s1 = mm.parts_without(cfg["stage1"])
            s2 = mm.parts_without(cfg["stage1"], cfg["stage2"])
            s2_tex = tex_id
            if cfg.get("stage2_skinned"):
                save_skinned(key, mm)
                s2_tex = skinned_id
            names = {}
            for pose_kind in ("ground", "hanging"):
                names[f"{pose_kind}_whole"] = build(f"{pose_kind}_whole", mm.all_parts, tex_id)
                names[f"{pose_kind}_s1"] = build(f"{pose_kind}_s1", s1, tex_id)
                names[f"{pose_kind}_s2"] = build(f"{pose_kind}_s2", s2, s2_tex)
            models = [names[s] for s in STAGED_STAGES]
            write_json(os.path.join(ASSETS, "blockstates", f"{carcass}.json"), blockstate(models))
            block_stages[carcass] = models
            item_model(carcass, names["ground_whole"])
            if cfg.get("head_block"):
                worst = max(worst, emit_head(key, cfg["name"], mm.model, cfg["head_block"], tex_id, mm.alpha,
                                             mm.all_parts))
                has_head = True
            d = cfg["drops"]
            tables = {"cut_1_drop": d["cut1"], "peel_drop": d["peel"], "cut_2_drop": d["cut2"]}
        else:
            g = build("ground_whole", mm.all_parts, tex_id)
            h = build("hanging_whole", mm.all_parts, tex_id)
            write_json(os.path.join(ASSETS, "blockstates", f"{carcass}.json"), blockstate([g, h]))
            block_stages[carcass] = [g, h]
            item_model(carcass, g)
            tables = {"cut_drop": cfg["drops"]["cut"]}

        emit_tables(key, tables)
        emit_extras(key, cfg)
        entries.append(entry_for(key, cfg, KIND_NAMES[kind], has_head))
        report.append(f"{key:26s} ground x{poses['ground'].scale:.2f}  hanging x{poses['hanging'].scale:.2f}"
                      f"  worst snap {worst:.2f}")

    # ------------------------------------------------ segmented creatures
    seg_pieces = {}
    for key, spec in {**A.SEGMENTED, **RST.SEGMENTED}.items():
        pieces = {}
        for name, where in spec["pieces"].items():
            cls, case = where if isinstance(where, tuple) else (spec["model"], where)
            pieces[name] = am.parse(os.path.join(args.models, cls + ".java"), cls, case)
        seg_pieces[key] = pieces
        tex_file = os.path.join(args.am_extract, "assets", "alexsmobs", "textures", "entity", spec["texture"] + ".png")
        alpha = np.array(Image.open(tex_file).convert("RGBA"))[..., 3]
        tex_id = f"alexsmobs:entity/{spec['texture']}"
        atlas_sources.append(tex_id)
        if spec.get("head_block", True):
            err = emit_head(key, spec["name"], pieces[spec["head_piece"]], spec["head_part"], tex_id, alpha)
            icon = f"{key}_head"
        else:
            hm = pieces[spec["head_piece"]]
            hb = list(am.walk(hm, start=spec["head_part"]))
            hpose = cv.fit_pose("head", cv.all_points(cv.visible(hb, hm.tex_w, hm.tex_h, alpha), 1.0), cv.LIMITS["head"])
            els, err = cv.convert_boxes(hb, hpose, 1.0, hm.tex_w, hm.tex_h, alpha)
            icon = f"{key}_icon"
            emit_model(icon, els, tex_id)
        carcass = f"{key}_carcass"
        # drawn by its renderer; the item shows the head model
        write_json(os.path.join(ASSETS, "blockstates", f"{carcass}.json"),
                   {"variants": {"": {"model": f"{MODID}:block/{carcass}"}}})
        write_json(os.path.join(ASSETS, "models", "block", f"{carcass}.json"), {"textures": {"particle": tex_id}})
        if spec.get("sprite"):
            # too big to show as a model: a flat sprite, as Butchery does for the Ender Dragon
            hm = pieces[spec["head_piece"]]
            hb = list(am.walk(hm, start=spec["head_part"]))
            hpose = cv.fit_pose("head", cv.all_points(cv.visible(hb, hm.tex_w, hm.tex_h, alpha), 1.0), cv.LIMITS["head"])
            sels, _ = cv.convert_boxes(hb, hpose, 1.0, hm.tex_w, hm.tex_h, alpha)
            os.makedirs(os.path.join(ASSETS, "textures", "item"), exist_ok=True)
            # hand-drawn art in tools/art wins; the splitter shares the void worm's
            drawn = os.path.join(os.path.dirname(os.path.abspath(__file__)), "art",
                                 spec.get("sprite_art", f"{carcass}.png"))
            if os.path.exists(drawn):
                shutil.copyfile(drawn, os.path.join(ASSETS, "textures", "item", f"{carcass}.png"))
            else:
                head_sprite(sels, np.array(Image.open(tex_file).convert("RGBA"))).save(
                    os.path.join(ASSETS, "textures", "item", f"{carcass}.png"))
            write_json(os.path.join(ASSETS, "models", "item", f"{carcass}.json"),
                       {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MODID}:item/{carcass}"},
                        "render_type": "translucent"})
        else:
            item_model(carcass, icon)
        lang[f"block.{MODID}.{carcass}"] = f"{spec['name']} Carcass"
        write_json(os.path.join(DATA, "loot_table", "blocks", f"{carcass}.json"),
                   block_drop_table(carcass, "minecraft:air"))
        emit_tables(key, {f"{s}_drop": d for s, d in spec["drops"].items()})
        entity_tags[key] = dict(entity=spec["entity"])
        entries.append(entry_for(key, spec, "SEGMENTED", spec.get("head_block", True)))
        report.append(f"{key + ' head':26s} worst snap {err:.2f}")

    # ------------------------------------------------ shared data
    if args.known_items:
        known = set(open(args.known_items, encoding="utf-8").read().split())
        known |= {f"{MODID}:{h.split(':')[1]}" for h in heads}
        known |= {f"{MODID}:{i}" for i in ITEMS}
        validate_items(all_drop_ids, known)

    if not only:
        write_json(os.path.join(RES, "assets", "minecraft", "atlases", "blocks.json"),
                   {"sources": [{"type": "minecraft:single", "resource": t} for t in atlas_sources]})
        write_json(os.path.join(ASSETS, "lang", "en_us.json"), lang)
        write_json(os.path.join(RES, "data", "butchery", "tags", "block", "heads.json"),
                   {"replace": False, "values": heads})
        emit_items(args, lang, args.butchery_extract)
        # optional items used through tags
        for tag, items in OPTIONAL_ITEM_TAGS.items():
            write_json(os.path.join(DATA, "tags", "item", f"{tag}.json"),
                       {"replace": False, "values": [{"id": i, "required": False} for i in items]})
        # a creature Alex's Mobs gives no loot table has nothing for the carcass modifier to replace
        entity_ids = {t["entity"].split(":")[1] for t in entity_tags.values()}
        for e in sorted(entity_ids):
            own = os.path.join(args.am_extract, "data", "alexsmobs", "loot_table", "entities", e + ".json")
            if not os.path.exists(own):
                write_json(os.path.join(RES, "data", "alexsmobs", "loot_table", "entities", e + ".json"),
                           {"type": "minecraft:entity", "pools": []})
        for key, t in entity_tags.items():
            write_json(os.path.join(DATA, "tags", "entity_type", f"{key}.json"),
                       {"replace": False, "values": [{"id": t["entity"], "required": False}]})
        write_java(shapes_models, block_stages, entries, seg_pieces, large_looks, step_specs)
    print("\n".join(report))

    if args.preview:
        make_preview(previews, args)


def _mk(p):
    os.makedirs(p, exist_ok=True)
    return p


def _java_str(s):
    return "null" if s is None else f"\"{s}\""


def write_java(shapes_models, block_stages, entries, seg_pieces, large_looks, step_specs):
    os.makedirs(JAVA_GEN, exist_ok=True)
    # outline boxes per model and the model per block state, read at runtime by ModelShapes
    # (a Java table this size would pass the 64 KB static-initialiser limit)
    write_json(os.path.join(RES, "butcheryam2", "shapes.json"),
               {"models": dict(sorted(shapes_models.items())), "blocks": dict(sorted(block_stages.items()))})

    # ---- the mob table the Java side registers from
    def cond(c):
        if not c:
            return "null"
        return 'new Match("%s", "%s", "%s", %s)' % (c[0], c[1], c[2], c[3])

    mob_lines = ["package dev.eteil.butcheryam2.gen;", "",
                 "import java.util.List;", "",
                 "/** Generated by tools/gen.py from tools/mobs*.py. Do not edit. */",
                 "public final class GeneratedMobs {", "",
                 "    /** A test on a dying entity: saved NBT ({@code nbt}) or a getter ({@code method}). */",
                 "    public record Match(String source, String key, String op, int value) {}", "",
                 "    /**",
                 "     * One butcherable creature.",
                 "     *",
                 "     * @param name       short id; blocks are {@code <name>_carcass}, {@code drained_<name>_carcass},",
                 "     *                   {@code <name>_head}; also the entity tag and loot table prefix",
                 "     * @param label      display name, for the config",
                 "     * @param kind       FULL, STAGED, SINGLE, STEPS, SEGMENTED or LARGE",
                 "     * @param match      for creatures split into variants, which variant this is, else null",
                 "     * @param head       whether it has a severed-head block",
                 "     * @param deathExtra whether {@code <name>_death_extra} drops alongside the carcass",
                 "     * @param extraWhen  when the death extra applies, or null for always",
                 "     */",
                 "    public record Entry(String name, String label, String kind, Match match, boolean head,",
                 "                        boolean deathExtra, Match extraWhen) {}", "",
                 "    public static final List<Entry> ALL = List.of("]
    mob_lines.append(",\n".join(
        '            new Entry("%s", "%s", "%s", %s, %s, %s, %s)' % (
            e["name"], e["label"].replace('"', ''), e["kind"], cond(e["match"]), str(e["head"]).lower(),
            str(e["has_extra"]).lower(), cond(e["extra"])) for e in entries) + ");")
    mob_lines += ["", "    private GeneratedMobs() {}", "}", ""]
    with open(os.path.join(JAVA_GEN, "GeneratedMobs.java"), "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(mob_lines))

    # ---- segmented creatures: common data (server-safe) and client layers
    specs = ["package dev.eteil.butcheryam2.gen;", "",
             "import java.util.List;", "import java.util.Map;", "",
             "/** Segmented carcasses (Butchery's Ender Dragon scheme). Generated by tools/gen.py. Do not edit. */",
             "public final class SegmentedSpecs {", "",
             "    /**",
             "     * @param layout       body pieces head first; a piece starting with {@code *} repeats",
             "     *                     {@code count} times",
             "     * @param countNbt     entity NBT int holding the repeat count, or null",
             "     * @param variantNbt   entity NBT boolean selecting the variant texture, or null",
             "     * @param countMethod  entity getter for the length when it is not saved (void worm), or null",
             "     * @param countOffset  added to the entity's length (pieces the layout already names)",
             "     * @param neckDrop     whether the piece right behind the head has its own drop",
             "     */",
             "    public record Spec(List<String> layout, String countNbt, int defaultCount, int maxCount,",
             "                       String variantNbt, String countMethod, int countOffset, boolean neckDrop) {", "",
             "        /** The pieces of a body with {@code count} repeats, head first. */",
             "        public List<String> pieces(int count) {",
             "            java.util.ArrayList<String> out = new java.util.ArrayList<>();",
             "            for (String p : layout) {",
             "                if (p.startsWith(\"*\")) {",
             "                    for (int i = 0; i < count; i++) {",
             "                        out.add(p.substring(1));",
             "                    }",
             "                } else {",
             "                    out.add(p);",
             "                }",
             "            }",
             "            return out;",
             "        }",
             "    }", "",
             "    public static final Map<String, Spec> SPECS = Map.of("]
    items = []
    for key, spec in {**A.SEGMENTED, **RST.SEGMENTED}.items():
        layout = ", ".join(f"\"{p}\"" for p in spec["layout"])
        items.append(f"            \"{key}\", new Spec(List.of({layout}), {_java_str(spec.get('count_nbt'))}, "
                     f"{spec.get('default_count', 1)}, {spec.get('max_count', 1)}, "
                     f"{_java_str(spec.get('variant_nbt'))}, {_java_str(spec.get('count_method'))}, "
                     f"{spec.get('count_offset', 0)}, {str('neck' in spec['drops']).lower()})")
    specs.append(",\n".join(items) + ");")
    specs += ["", "    private SegmentedSpecs() {}", "}", ""]
    with open(os.path.join(JAVA_GEN, "SegmentedSpecs.java"), "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(specs))

    body = ["package dev.eteil.butcheryam2.gen;", "",
            "import java.util.Map;",
            "import java.util.function.Supplier;",
            "import net.minecraft.client.model.geom.PartPose;",
            "import net.minecraft.client.model.geom.builders.CubeDeformation;",
            "import net.minecraft.client.model.geom.builders.CubeListBuilder;",
            "import net.minecraft.client.model.geom.builders.LayerDefinition;",
            "import net.minecraft.client.model.geom.builders.MeshDefinition;",
            "import net.minecraft.client.model.geom.builders.PartDefinition;", "",
            "/**",
            " * Client half of the segmented carcasses: Alex's Mobs' body pieces as vanilla layers and where",
            " * they lie in the coil. Generated by tools/gen.py. Do not edit.",
            " */",
            "public final class SegmentedLayers {", "",
            "    /**",
            "     * @param texture        Alex's Mobs texture",
            "     * @param variantTexture texture for the variant, or null",
            "     * @param scale          model scale of the coiled body",
            "     * @param lift           model pixels lifting a piece onto the ground",
            "     * @param onBack         whether the pieces lie on their backs",
            "     * @param coil           x, z (block pixels) and yaw (degrees) per piece, head first",
            "     * @param layers         piece name -> layer",
            "     */",
            "    public record Look(String texture, String variantTexture, float scale, float lift, boolean onBack,",
            "                       float[][] coil, Map<String, Supplier<LayerDefinition>> layers,",
            "                       Map<String, String> pieceTextures, float reach, float grow, float tailScale) {}", "",
            "    public static final Map<String, Look> LOOKS = Map.ofEntries("]
    items = []
    for key, spec in {**A.SEGMENTED, **RST.SEGMENTED}.items():
        pieces = seg_pieces[key]
        lift = segment_lift(pieces["body"], spec["on_back"])
        coil = coil_layout(max_pieces(spec), spec["spacing"], spec["scale"], *spec["radius"],
                           sizes=piece_sizes(spec))
        coil_s = ", ".join(f"{{{x}F, {z}F, {y}F}}" for x, z, y in coil)
        layers = ", ".join(f"\"{p}\", SegmentedLayers::{key}_{p}" for p in pieces)
        vt = spec.get("variant_texture")
        ptex = ", ".join('"%s", "alexsmobs:textures/entity/%s.png"' % (p, t)
                         for p, t in spec.get("piece_textures", {}).items())
        items.append(
            f"            Map.entry(\"{key}\", new Look(\"alexsmobs:textures/entity/{spec['texture']}.png\", "
            f"{_java_str(f'alexsmobs:textures/entity/{vt}.png' if vt else None)}, {spec['scale']}F, "
            f"{round(float(lift), 3)}F, {str(spec['on_back']).lower()},\n"
            f"                    new float[][]{{{coil_s}}},\n"
            f"                    Map.of({layers}),\n"
            f"                    Map.of({ptex}), {float(spec.get('reach', 5.0))}F, {float(spec.get('grow', 0.0))}F, "
            f"{float(spec.get('tail_scale', 1.0))}F))")
    body.append(",\n".join(items) + ");")
    body.append("")
    for key, pieces in seg_pieces.items():
        for p, model in pieces.items():
            body.append(java_layer(model, f"{key}_{p}"))
            body.append("")
    body += ["    private SegmentedLayers() {}", "}", ""]
    with open(os.path.join(JAVA_GEN, "SegmentedLayers.java"), "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(body))

    write_large(large_looks)
    write_steps(step_specs)


def write_steps(step_specs):
    """The cut list of every step-driven carcass (step lists and renderer-drawn giants)."""
    ls = ["package dev.eteil.butcheryam2.gen;", "",
          "import java.util.List;", "import java.util.Map;", "",
          "/** Step-driven carcasses: the cuts in order. Generated by tools/gen.py. Do not edit. */",
          "public final class StepSpecs {", "",
          "    /**",
          "     * One cut.",
          "     *",
          "     * @param tool    CLEAVER, KNIFE or ANY",
          "     * @param loot    loot table suffix rolled for this cut",
          "     * @param organs  Butchery organ tables rolled too (if the Organs option is on)",
          "     * @param fragile an item that only comes off whole by chance (Butchery's Enderman heart), or null",
          "     */",
          "    public record Step(String tool, String loot, List<String> organs, Fragile fragile) {}", "",
          "    /**",
          "     * @param item     the item on success",
          "     * @param chance   the chance it comes off whole",
          "     * @param failLoot loot table suffix rolled when it does not",
          "     */",
          "    public record Fragile(String item, float chance, String failLoot, String sound, String particle,",
          "                          String okMessage, String failMessage) {}", "",
          "    public static final Map<String, List<Step>> STEPS = Map.ofEntries("]
    items = []

    def jstr(s):
        return '"' + s.replace('\\', '\\\\').replace('"', '\\"') + '"'

    for key, steps in step_specs.items():
        parts = []
        for i, s in enumerate(steps):
            # a step that drops nothing has no loot table ("")
            loot = s.get("loot") or (f"step_{i + 1}_drop" if s.get("drops") else "")
            fr = s.get("fragile")
            frag = "null"
            if fr:
                frag = "new Fragile(%s, %sF, %s, %s, %s, %s, %s)" % (
                    jstr(fr["item"]), fr["chance"], jstr(f"step_{i + 1}_fail" if fr["fail_drops"] else ""), jstr(fr["sound"]),
                    jstr(fr["particle"]), jstr(fr["ok"]), jstr(fr["fail"]))
            org = s.get("organs", False)
            if org is True:
                org = ["butchery:blocks/organs_drop_1", "butchery:blocks/organs_drop_2", "butchery:blocks/organs_drop_3"]
            org_s = "List.of(" + ", ".join('"%s"' % t for t in (org or [])) + ")"
            parts.append('new Step("%s", "%s", %s, %s)' % (s["tool"], loot, org_s, frag))
        items.append('            Map.entry("%s", List.of(\n                    %s))' % (key, ",\n                    ".join(parts)))
    ls.append(",\n".join(items) + ");")
    ls += ["", "    private StepSpecs() {}", "}", ""]
    with open(os.path.join(JAVA_GEN, "StepSpecs.java"), "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(ls))


def write_large(large_looks):
    """The client look of the renderer-drawn carcasses."""
    ll = ["package dev.eteil.butcheryam2.gen;", "",
          "import java.util.List;", "import java.util.Map;", "import java.util.function.Supplier;",
          "import net.minecraft.client.model.geom.PartPose;",
          "import net.minecraft.client.model.geom.builders.CubeDeformation;",
          "import net.minecraft.client.model.geom.builders.CubeListBuilder;",
          "import net.minecraft.client.model.geom.builders.LayerDefinition;",
          "import net.minecraft.client.model.geom.builders.MeshDefinition;",
          "import net.minecraft.client.model.geom.builders.PartDefinition;", "",
          "/** Client half of the renderer-drawn carcasses. Generated by tools/gen.py. Do not edit. */",
          "public final class LargeLayers {", "",
          "    /**",
          "     * @param texture        Alex's Mobs texture",
          "     * @param skinnedTexture the generated skinned texture, or null",
          "     * @param offset         block pixels to the model origin",
          "     * @param rotation       quaternion x, y, z, w turning the model onto its side",
          "     * @param scale          model scale",
          "     * @param always         part paths never drawn (variant extras)",
          "     * @param hidden         per number of cuts made, the part paths already cut away",
          "     * @param skinned        per number of cuts made, whether the skinned texture shows",
          "     */",
          "    public record Look(String texture, String skinnedTexture, float[] offset, float[] rotation,",
          "                       float scale, List<String> always, List<List<String>> hidden, boolean[] skinned,",
          "                       Supplier<LayerDefinition> layer) {}", "",
          "    public static final Map<String, Look> LOOKS = Map.ofEntries("]
    items = []
    for key, (cfg, mm, look) in large_looks.items():
        tex = "alexsmobs:textures/entity/%s.png" % cfg["texture"]
        skin = ('"%s:textures/block/%s_skinned.png"' % (MODID, key)
                if any(s["skinned"] for s in look["steps"]) else "null")
        hidden = [[]] + [s["hide"] for s in look["steps"]]
        skinned = [False] + [s["skinned"] for s in look["steps"]]
        hid = ", ".join("List.of(" + ", ".join('"%s"' % h for h in hs) + ")" for hs in hidden)
        off = ", ".join("%.4fF" % v for v in look["offset"])
        quat = ", ".join("%.6fF" % v for v in look["quat"])
        always = ", ".join('"%s"' % a for a in look["always"])
        sk = ", ".join(str(b).lower() for b in skinned)
        items.append(
            '            Map.entry("%s", new Look("%s", %s,\n'
            '                    new float[]{%s},\n'
            '                    new float[]{%s}, %.5fF,\n'
            '                    List.of(%s),\n'
            '                    List.of(%s),\n'
            '                    new boolean[]{%s},\n'
            '                    LargeLayers::%s))' % (key, tex, skin, off, quat, look["scale"], always, hid, sk, key))
    ll.append(",\n".join(items) + ");")
    ll.append("")
    for key, (cfg, mm, look) in large_looks.items():
        ll.append(java_layer(mm.model, key))
        ll.append("")
    ll += ["    private LargeLayers() {}", "}", ""]
    with open(os.path.join(JAVA_GEN, "LargeLayers.java"), "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(ll))


def make_preview(previews, args):
    import preview as pv
    tiles = []
    tex_cache = {}

    def load(tex_id):
        if tex_id not in tex_cache:
            ns, path = tex_id.split(":")
            if ns == "alexsmobs":
                p = os.path.join(args.am_extract, "assets", "alexsmobs", "textures", path + ".png")
            else:
                p = os.path.join(ASSETS, "textures", path + ".png")
            tex_cache[tex_id] = np.array(Image.open(p).convert("RGBA"))
        return tex_cache[tex_id]

    for name, els, tex_id, pose in previews:
        lo = np.min([e["from"] for e in els], 0)
        hi = np.max([e["to"] for e in els], 0)
        cam = pv.Camera(center=tuple((lo + hi) / 2), ppu=min(9.0, 190.0 / float((hi - lo).max())), size=(300, 260))
        tiles.append(pv.label(pv.rasterise(pv.block_quads(els, {"tex": load(tex_id)}), cam), name))
    pv.sheet(tiles, 6).save(args.preview)


if __name__ == "__main__":
    sys.exit(main())
