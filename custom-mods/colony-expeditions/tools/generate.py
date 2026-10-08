"""Generates Colony Expeditions resources from destinations.py.

Writes into src/main/resources: hut and portal models, lang, damage types, hut recipes and loot tables,
research, trip recipes and loot tables, and the "Colony Expeditions" blueprint pack.
Run from anywhere: python tools/generate.py
"""
import copy
import json
import re
import shutil
import sys
import zipfile
from pathlib import Path

import nbt
from destinations import DESTINATIONS, TRIP_INPUTS

MODID = "colony_expeditions"
MODS = Path(r"C:/Users/eteil.000/AppData/Roaming/PrismLauncher/instances/1.21.1/minecraft/mods")
MINECOLONIES = next(MODS.glob("minecolonies-*.jar"))
BASE_STYLE = "original"  # MineColonies style whose netherworker blueprints are the template
PACK_NAME = "Colony Expeditions"

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
ASSETS = RES / "assets" / MODID
DATA = RES / "data" / MODID
PACK_DIR = RES / "blueprints" / MODID / MODID  # pack root
BLUEPRINTS = PACK_DIR / "expeditions"  # virtual folder shown in every style (SharedBlueprints.FOLDER)

TIERS = 5
warnings = []


def write_json(path: Path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


# ---------------------------------------------------------------- registry lookups in installed jars

_jar_index = None


def jar_index():
    """namespace -> set of 'kind/path' entries (blockstates, textures) across the pack's jars."""
    global _jar_index
    if _jar_index is None:
        _jar_index = {}
        for jar in MODS.glob("*.jar"):
            try:
                names = zipfile.ZipFile(jar).namelist()
            except zipfile.BadZipFile:
                continue
            for n in names:
                m = re.match(r"assets/([a-z0-9_.-]+)/(blockstates|textures|models/item)/(.+)\.(json|png)$", n)
                if m:
                    _jar_index.setdefault(m.group(1), set()).add(f"{m.group(2)}/{m.group(3)}")
    return _jar_index


_lang_keys = None


def lang_keys():
    """Every en_us translation key in the pack; items, blocks and entities all have one."""
    global _lang_keys
    if _lang_keys is None:
        _lang_keys = set()
        for jar in MODS.glob("*.jar"):
            try:
                z = zipfile.ZipFile(jar)
                for n in z.namelist():
                    if re.match(r"assets/[a-z0-9_.-]+/lang/en_us\.json$", n):
                        try:
                            _lang_keys.update(json.loads(z.read(n).decode("utf-8-sig")).keys())
                        except ValueError:
                            pass
            except zipfile.BadZipFile:
                continue
    return _lang_keys


def check_id(dest: str, kind: str, rid: str):
    ns, path = rid.split(":")
    if ns == "minecraft":
        return
    if kind == "entity":
        if f"entity.{ns}.{path}" not in lang_keys():
            warnings.append(f"{dest}: entity {rid} not found in any mod")
    elif f"models/item/{path}" not in jar_index().get(ns, set()):
        # A block without an item form breaks MineColonies' recipe loading and hangs server start.
        warnings.append(f"{dest}: item {rid} has no item model (not an item?)")


def block_exists(block_id: str) -> bool:
    ns, path = block_id.split(":")
    return ns == "minecraft" or f"blockstates/{path}" in jar_index().get(ns, set())


def texture_exists(tex: str) -> bool:
    ns, path = tex.split(":") if ":" in tex else ("minecraft", tex)
    return ns == "minecraft" or f"textures/{path}" in jar_index().get(ns, set())


# ---------------------------------------------------------------- assets

def nether_hut_model():
    with zipfile.ZipFile(MINECOLONIES) as z:
        model = json.loads(z.read("assets/minecolonies/models/block/blockhutnetherworker.json"))
        item = json.loads(z.read("assets/minecolonies/models/item/blockhutnetherworker.json"))
        state = json.loads(z.read("assets/minecolonies/blockstates/blockhutnetherworker.json"))
    return model, item, state


def gen_assets():
    model, item, state = nether_hut_model()
    lang = {"itemGroup." + MODID: "Colony Expeditions",
            f"com.{MODID}.research.expeditions.name": "Expeditions",
            # Per-hut gear requirement, shown in the hut's Settings tab.
            f"com.minecolonies.coremod.setting.{MODID}:mingear": "Minimum gear before leaving:",
            f"com.{MODID}.setting.gear.default": "Config default",
            f"com.{MODID}.setting.gear.none": "No requirement",
            f"com.{MODID}.setting.gear.wood": "Wood tools, leather armour",
            f"com.{MODID}.setting.gear.stone": "Stone tools, gold armour",
            f"com.{MODID}.setting.gear.iron": "Iron tools, chain armour",
            f"com.{MODID}.setting.gear.diamond": "Diamond tools, iron armour",
            f"com.{MODID}.setting.gear.netherite": "Netherite tools, diamond armour"}

    for dest, d in DESTINATIONS.items():
        hut = f"blockhutexpedition_{dest}"
        work = f"expedition_{dest}"

        m = copy.deepcopy(model)
        for child in m["children"].values():
            for slot, tex in list(child.get("textures", {}).items()):
                new = d.get("hut_textures", {}).get(slot)
                if new and not texture_exists(new):
                    warnings.append(f"{dest}: hut texture {new} not found, keeping {tex}")
                    new = None
                child["textures"][slot] = new or ("minecraft:" + tex if ":" not in tex else tex)
        portal_tex = d.get("portal_texture", "minecraft:block/nether_portal")
        if not texture_exists(portal_tex):
            warnings.append(f"{dest}: portal texture {portal_tex} not found")
            portal_tex = "minecraft:block/nether_portal"
        m["textures"]["particle"] = portal_tex
        write_json(ASSETS / f"models/block/{hut}.json", m)

        it = copy.deepcopy(item)
        it["parent"] = f"{MODID}:block/{hut}"
        write_json(ASSETS / f"models/item/{hut}.json", it)

        st = copy.deepcopy(state)
        for variant in st["variants"].values():
            variant["model"] = f"{MODID}:block/{hut}"
        write_json(ASSETS / f"blockstates/{hut}.json", st)

        portal = f"portal_{dest}"
        for suffix, parent in (("ns", "minecraft:block/nether_portal_ns"), ("ew", "minecraft:block/nether_portal_ew")):
            write_json(ASSETS / f"models/block/{portal}_{suffix}.json", {
                "parent": parent,
                "render_type": "minecraft:translucent",
                "textures": {"particle": portal_tex, "portal": portal_tex},
            })
        write_json(ASSETS / f"blockstates/{portal}.json", {"variants": {
            "axis=x": {"model": f"{MODID}:block/{portal}_ns"},
            "axis=z": {"model": f"{MODID}:block/{portal}_ew"},
        }})

        lang[f"block.{MODID}.{hut}"] = d["building"]
        lang[f"block.{MODID}.{hut}.name"] = d["building"]
        lang[f"block.{MODID}.{portal}"] = f"{d['name']} Veil"
        lang[f"com.{MODID}.building.{work}"] = d["building"]
        lang[f"com.{MODID}.building.{work}.desc"] = d["desc"]
        lang[f"com.{MODID}.job.{work}"] = d["job"]
        lang[f"death.attack.{work}"] = d["death"]
        lang[f"death.attack.{work}.player"] = d["death"] + " while fighting %2$s"
        lang[f"com.{MODID}.research.expeditions.{dest}.name"] = f"Expedition: {d['name']}"
        lang[f"com.{MODID}.research.effects.{hut}.description"] = f"Unlocks the {d['building']}"

    write_json(ASSETS / "lang/en_us.json", lang)
    # The ender chest texture is not on the block atlas by default.
    write_json(RES / "assets/minecraft/atlases/blocks.json", {"sources": [
        {"type": "minecraft:single", "resource": "minecraft:entity/chest/ender"},
    ]})


# ---------------------------------------------------------------- data

def gen_common_data():
    for dest in DESTINATIONS:
        hut = f"blockhutexpedition_{dest}"
        work = f"expedition_{dest}"
        write_json(DATA / f"damage_type/{work}.json", {"exhaustion": 0.1, "message_id": work, "scaling": "always"})
        write_json(DATA / f"loot_table/blocks/{hut}.json", {
            "type": "minecraft:block",
            "pools": [{
                "bonus_rolls": 0.0, "rolls": 1.0,
                "conditions": [{"condition": "minecraft:survives_explosion"}],
                "entries": [{"type": "minecraft:item", "name": f"{MODID}:{hut}",
                             "functions": [{"function": "minecraft:copy_name", "source": "block_entity"}]}],
            }],
            "random_sequence": f"{MODID}:blocks/{hut}",
        })
        frame = DESTINATIONS[dest].get("frame", {}).get("minecraft:obsidian", "minecraft:obsidian")
        write_json(DATA / f"recipe/{hut}.json", {
            "type": "minecraft:crafting_shaped",
            "category": "misc",
            "key": {"B": {"item": frame}, "T": {"item": "structurize:sceptergold"}, "X": {"tag": "minecraft:planks"}},
            "pattern": ["XTX", "XBX", "XXX"],
            "result": {"count": 1, "id": f"{MODID}:{hut}"},
        })
    write_json(DATA / "tags/damage_type/expedition.json", {"values": [f"{MODID}:expedition_{d}" for d in DESTINATIONS]})
    write_json(DATA / "researches/expeditions.json", {
        "base-time": 1.0,
        "branch-name": f"com.{MODID}.research.expeditions.name",
        "sortOrder": 160,
    })


def ready(d):
    return all(k in d for k in ("research", "encounters", "materials"))


def gen_destination_data(dest, d, order):
    hut = f"blockhutexpedition_{dest}"
    work = f"expedition_{dest}"
    for item, *_ in d["research"] + d["materials"]:
        check_id(dest, "item", item)
    for entity, *_ in d["encounters"]:
        check_id(dest, "entity", entity)

    write_json(DATA / f"researches/effects/{hut}.json", {"effect": True, "levels": [5.0]})
    write_json(DATA / f"researches/expeditions/{dest}.json", {
        "branch": f"{MODID}:expeditions",
        "costs": [{"item": item, "count": count} for item, count in d["research"]],
        "effects": [{"id": f"{MODID}:effects/{hut}", "level": 1}],
        "icon": f"{MODID}:{hut}",
        "requirements": [{"type": "minecolonies:building", "building": "minecolonies:netherworker", "level": 1}],
        "researchLevel": 1,
        "sortOrder": order,
    })

    # How many encounters a trip rolls. Fewer means a shorter, less lethal trip.
    rolls = d.get("encounter_rolls", (2, 6))
    for tier in range(1, TIERS + 1):
        mats = [m for m in d["materials"] if m[4] <= tier]
        encounters = [e for e in d["encounters"] if e[4] <= tier]
        material_entries = []
        for item, weight, lo, hi, _ in mats:
            entry = {"type": "minecraft:item", "name": item, "weight": weight}
            if (lo, hi) != (1, 1):
                entry["functions"] = [{"function": "minecraft:set_count", "add": False,
                                       "count": {"type": "minecraft:uniform", "min": float(lo), "max": float(hi)}}]
            material_entries.append(entry)
        encounter_entries = [{
            "type": "minecraft:item",
            "name": "minecolonies:adventure_token",
            "weight": weight,
            "functions": [{"function": "minecraft:set_components",
                           "components": {"minecolonies:adventure": {"entity": entity, "damage": damage, "xp": xp}}}],
        } for entity, damage, xp, weight, _ in encounters]

        write_json(DATA / f"loot_table/recipes/{work}/trip{tier}.json", {
            "pools": [
                {"rolls": {"type": "minecraft:uniform", "min": 3.0, "max": 10.0},
                 "bonus_rolls": {"type": "minecraft:uniform", "min": 0.3, "max": 0.3},
                 "entries": material_entries},
                {"rolls": {"type": "minecraft:uniform", "min": float(rolls[0]), "max": float(rolls[1])},
                 "bonus_rolls": {"type": "minecraft:uniform", "min": 0.1, "max": 0.1},
                 "entries": encounter_entries},
            ]
        })
        write_json(DATA / f"crafterrecipes/{work}/trip{tier}.json", {
            "type": "recipe",
            "crafter": f"{work}_custom",
            "inputs": TRIP_INPUTS,
            "additional-output": [{"id": item} for item, *_ in mats],
            "intermediate": "minecraft:air",
            "loot-table": f"{MODID}:recipes/{work}/trip{tier}",
            "min-building-level": tier,
            "max-building-level": tier,
        })


# ---------------------------------------------------------------- blueprints

WOOD_SUFFIXES = ["planks", "stairs", "slab", "fence", "fence_gate", "button", "trapdoor", "door", "pressure_plate"]


def blueprint_theme(dest, d):
    """Exact block swaps, falling back to the nether wood families."""
    theme = dict(d.get("frame", {}))
    wood = d.get("wood", {})
    for nether_wood, target in wood.items():
        for suffix in WOOD_SUFFIXES:
            theme.setdefault(f"minecraft:{nether_wood}_{suffix}", f"{target}_{suffix}")
    theme.update(d.get("blocks", {}))
    return theme


def rewrite_blueprint(raw: bytes, dest: str, level: int, theme: dict) -> bytes:
    name, root = nbt.load(raw)
    hut = f"{MODID}:blockhutexpedition_{dest}"
    work = f"expedition_{dest}"
    file = f"{work}{level}.blueprint"

    for entry in root["palette"][1][1]:
        block = entry["Name"][1]
        if block == "minecolonies:blockhutnetherworker":
            entry["Name"] = (nbt.STRING, hut)
        elif block in theme:
            target = theme[block]
            if block_exists(target):
                entry["Name"] = (nbt.STRING, target)
            else:
                warnings.append(f"{dest}: blueprint block {target} not found, keeping {block}")

    for te in root["tile_entities"][1][1]:
        if te.get("type", (0, ""))[1] == "minecolonies:netherworker":
            te["type"] = (nbt.STRING, f"{MODID}:{work}")
            te["path"] = (nbt.STRING, file)
            provider = te["blueprintDataProvider"][1]
            provider["path"] = (nbt.STRING, file)
            provider["schematicName"] = (nbt.STRING, f"{work}{level}")

    root["name"] = (nbt.STRING, file)
    mods = root["required_mods"][1][1]
    if MODID not in mods:
        mods.append(MODID)
    return nbt.dump(name, root)


def gen_blueprints(ready_dests):
    for old in (RES / "blueprints").glob("*/*"):
        shutil.rmtree(old)
    write_json(PACK_DIR / "pack.json", {
        "name": PACK_NAME,
        "authors": ["eileenthg", "MineColonies (base: Netherworker)"],
        "desc": "Shared expedition posts. They also appear in every other style under the Expeditions folder.",
        "mods": ["structurize", "minecolonies", MODID],
        "icon": "",
        "version": "1",
        "pack-format": "1",
    })
    with zipfile.ZipFile(MINECOLONIES) as z:
        for dest, d in ready_dests.items():
            theme = blueprint_theme(dest, d)
            for level in range(1, TIERS + 1):
                raw = z.read(f"blueprints/minecolonies/{BASE_STYLE}/mystic/netherworker{level}.blueprint")
                out = BLUEPRINTS / dest / f"expedition_{dest}{level}.blueprint"
                out.parent.mkdir(parents=True, exist_ok=True)
                out.write_bytes(rewrite_blueprint(raw, dest, level, theme))


def main():
    gen_assets()
    gen_common_data()
    ready_dests = {k: v for k, v in DESTINATIONS.items() if ready(v)}
    for order, (dest, d) in enumerate(ready_dests.items(), start=1):
        gen_destination_data(dest, d, order)
    gen_blueprints(ready_dests)
    print("complete:", ", ".join(ready_dests))
    print("unfinished:", ", ".join(k for k in DESTINATIONS if k not in ready_dests))
    for w in sorted(set(warnings)):
        print("WARNING", w)
    return 1 if warnings else 0


if __name__ == "__main__":
    sys.exit(main())
