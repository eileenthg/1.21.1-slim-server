"""Per-destination data for generate.py.

Each destination may define:
  name, job, building, desc  - English names
  death                      - death message fragment ("%1$s ...")
  hut_textures               - replacement textures for the Netherworker hut model slots
  frame                      - {netherworker blueprint block: replacement} for the blueprint theme
  research                   - item costs for the unlock research (items from the dimension itself)
  encounters                 - adventure tokens: (entity, damage, xp, weight, min_tier)
  materials                  - (item, weight, min_count, max_count, min_tier)

Only destinations with "research", "encounters" and "materials" get trips, research and blueprints.
The rest get their hut, portal and damage type registered but stay unfinished.
"""

# Netherworker hut model slots: 0 figure, 3 metal block, 4 log side, 7 log top, 8 planks, 10 frame,
# 12 chest, 13 floor planks, 16 accent block, 9 portal (translucent).
DESTINATIONS = {
    "end": {
        "name": "The End",
        "job": "End Voyager",
        "building": "End Expedition Post",
        "desc": "Sends a voyager through an End portal to bring back End stone, chorus, shadoline and whatever the Endermen drop",
        "death": "%1$s was lost in the End",
        "hut_textures": {
            "3": "minecraft:block/purpur_block",
            "4": "enderscape:block/veiled_log",
            "7": "enderscape:block/veiled_log_top",
            "8": "enderscape:block/veiled_planks",
            "10": "minecraft:block/end_stone_bricks",
            "12": "minecraft:entity/chest/ender",
            "13": "minecraft:block/purpur_pillar",
            "16": "enderscape:block/nebulite_block",
            "9": "enderscape:block/void_shale_emissive",
        },
        "portal_texture": "enderscape:block/void_shale_emissive",
        "frame": {
            "minecraft:obsidian": "minecraft:end_stone_bricks",
            "minecraft:crying_obsidian": "minecraft:purpur_block",
        },
        "wood": {"crimson": "enderscape:veiled", "warped": "enderscape:murublight"},
        "blocks": {
            "minecraft:crimson_stem": "enderscape:veiled_log",
            "minecraft:stripped_crimson_stem": "enderscape:stripped_veiled_log",
            "minecraft:crimson_nylium": "enderscape:veiled_end_stone",
            "minecraft:warped_nylium": "enderscape:celestial_overgrowth",
            "minecraft:netherrack": "minecraft:end_stone",
            "minecraft:nether_bricks": "minecraft:end_stone_bricks",
            "minecraft:cracked_nether_bricks": "minecraft:end_stone_bricks",
            "minecraft:chiseled_nether_bricks": "enderscape:chiseled_end_stone",
            "minecraft:nether_brick_fence": "enderscape:veiled_fence",
            "minecraft:nether_brick_wall": "minecraft:end_stone_brick_wall",
            "minecraft:crimson_roots": "enderscape:chorus_sprouts",
            "minecraft:warped_roots": "enderscape:wisp_sprouts",
            "minecraft:nether_sprouts": "enderscape:chorus_sprouts",
            "minecraft:crimson_fungus": "enderscape:celestial_chanterelle",
            "minecraft:warped_fungus": "enderscape:murublight_chanterelle",
            "minecraft:soul_lantern": "enderscape:bulb_lantern",
        },
        "research": [
            ("minecraft:shulker_shell", 8),
            ("minecraft:popped_chorus_fruit", 32),
            ("enderscape:nebulite", 4),
        ],
        "encounters": [
            ("minecraft:enderman", 7.0, 5, 5500, 1),
            ("minecraft:endermite", 2.0, 3, 800, 1),
            ("enderscape:rubblemite", 4.0, 4, 800, 1),
            ("enderscape:drifter", 3.0, 4, 400, 1),
            ("enderscape:rustle", 1.0, 2, 200, 1),
            ("alexsmobs:enderiophage", 5.0, 6, 300, 2),
            ("minecraft:shulker", 6.0, 8, 400, 3),
            ("alexsmobs:mimicube", 8.0, 10, 150, 4),
        ],
        "materials": [
            ("minecraft:end_stone", 20, 5, 25, 1),
            ("minecraft:chorus_fruit", 15, 2, 8, 1),
            ("minecraft:chorus_flower", 2, 1, 1, 1),
            ("enderscape:veiled_log", 8, 1, 4, 1),
            ("enderscape:flanger_berry", 6, 1, 3, 1),
            ("enderscape:blinklight", 4, 1, 3, 1),
            ("enderscape:mirestone", 10, 2, 10, 2),
            ("enderscape:celestial_stem", 6, 1, 4, 2),
            ("enderscape:murublight_stem", 6, 1, 4, 2),
            ("enderscape:wisp_flower", 3, 1, 2, 2),
            ("enderscape:shadoline_ore", 8, 1, 3, 2),
            ("enderscape:kurodite", 8, 2, 8, 3),
            ("enderscape:veradite", 8, 2, 8, 3),
            ("minecraft:purpur_block", 8, 2, 8, 3),
            ("minecraft:end_rod", 3, 1, 3, 3),
            ("enderscape:void_shale", 4, 1, 4, 3),
            ("enderscape:nebulite_ore", 3, 1, 2, 4),
            ("minecraft:popped_chorus_fruit", 4, 2, 6, 4),
            ("enderscape:alluring_magnia", 2, 1, 2, 4),
            ("enderscape:repulsive_magnia", 2, 1, 2, 4),
            ("enderscape:nebulite", 1, 1, 1, 5),
            ("enderscape:end_city_key", 1, 1, 1, 5),
        ],
    },
    "otherside": {
        "name": "The Otherside",
        "job": "Otherside Delver",
        "building": "Otherside Expedition Post",
        "desc": "Sends a delver into the sculk-choked Otherside",
        "death": "%1$s was swallowed by the Otherside",
        "hut_textures": {
            "3": "deeperdarker:block/gloomslate_bricks",
            "8": "deeperdarker:block/echo_planks",
            "10": "deeperdarker:block/sculk_stone_bricks",
            "13": "deeperdarker:block/polished_gloomslate",
            "9": "deeperdarker:block/otherside_portal",
        },
        "portal_texture": "deeperdarker:block/otherside_portal",
    },
    "deeper_down": {
        "name": "The Deeper Down",
        "job": "Deeper Down Delver",
        "building": "Deeper Down Expedition Post",
        "desc": "Sends a delver below the bedrock floor",
        "death": "%1$s fell into the Deeper Down",
        "hut_textures": {
            "3": "spectrum:block/blackslag_bricks",
            "10": "spectrum:block/blackslag_tiles",
            "13": "spectrum:block/blackslag",
            "9": "spectrum:block/deeper_down_portal",
        },
        "portal_texture": "spectrum:block/deeper_down_portal",
    },
    "bumblezone": {
        "name": "The Bumblezone",
        "job": "Hive Forager",
        "building": "Bumblezone Expedition Post",
        "desc": "Sends a forager into the giant hive of the Bumblezone",
        "death": "%1$s was stung to death in the Bumblezone",
        "hut_textures": {
            "3": "the_bumblezone:block/filled_porous_honeycomb_block",
            "10": "the_bumblezone:block/beehive_beeswax",
            "9": "the_bumblezone:block/honey_crystal_main",
        },
        "portal_texture": "the_bumblezone:block/honey_crystal_main",
    },
    "comets": {"name": "Comets", "job": "Comet Voyager", "building": "Comets Expedition Post",
               "desc": "Sends a voyager to the drifting asteroids of Comets", "death": "%1$s drifted off into Comets"},
    "gardens_of_eden": {"name": "Gardens of Eden", "job": "Eden Wanderer", "building": "Eden Expedition Post",
                        "desc": "Sends a wanderer into the Gardens of Eden", "death": "%1$s was scorched in the Gardens of Eden"},
    "kingdom_of_amber": {"name": "Kingdom of Amber", "job": "Amber Envoy", "building": "Amber Expedition Post",
                         "desc": "Sends an envoy to the Kingdom of Amber", "death": "%1$s was sealed in amber"},
    "lands_of_karvat": {"name": "Lands of Karvat", "job": "Karvat Hunter", "building": "Karvat Expedition Post",
                        "desc": "Sends a hunter into the Lands of Karvat", "death": "%1$s was hunted in the Lands of Karvat"},
    "purgatorium": {"name": "Purgatorium", "job": "Purgatorium Pilgrim", "building": "Purgatorium Expedition Post",
                    "desc": "Sends a pilgrim through the bone wastes of Purgatorium", "death": "%1$s never left Purgatorium"},
    "rayana": {"name": "Rayana", "job": "Rayana Explorer", "building": "Rayana Expedition Post",
               "desc": "Sends an explorer into the jungles and mesas of Rayana", "death": "%1$s was lost in Rayana"},
    "volcanech": {"name": "Volcanech", "job": "Volcanech Miner", "building": "Volcanech Expedition Post",
                  "desc": "Sends a miner into the sulfur fields of Volcanech", "death": "%1$s burned up in Volcanech"},
}

# Supplies per trip tier, the same shape as the Netherworker's trips.
TRIP_INPUTS = [
    {"id": "minecraft:cobblestone", "count": 64},
    {"id": "minecraft:torch", "count": 32},
    {"id": "minecraft:ladder", "count": 16},
]


# Trip, research and theme data for the other destinations.
from destinations_more import MORE  # noqa: E402

for _dest, _data in MORE.items():
    DESTINATIONS[_dest].update(_data)
