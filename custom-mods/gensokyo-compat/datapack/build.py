"""Builds zz-gensokyo-bridge.zip: the data half of the Gensokyo Legacy <-> Youkai's Homecoming bridge.
Sources are read from the Legacy, YH and Gensouworks jars; run with the jar paths below."""
import json, re, zipfile, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
GL_JAR, MODS = sys.argv[1], sys.argv[2]
OUT = os.path.join(HERE, 'zz-gensokyo-bridge.zip')

DROPPED = ['fairy_ice_crystal', 'frozen_frog_cold', 'frozen_frog_warm', 'frozen_frog_temperate', 'spell_reimu',
           'spell_marisa', 'spell_sanae', 'spell_yukari_butterfly', 'spell_yukari_laser', 'spell_mystia', 'straw_hat',
           'suwako_hat', 'koishi_hat', 'rumia_hairband', 'cirno_hairband', 'cirno_wings', 'donation_box', 'book_pile',
           'debug_glasses', 'debug_wand', 'structure_wand']
SWAP = re.compile(r'"gensokyolegacy:(' + '|'.join(DROPPED) + r')"')
files = {}

def put(path, obj):
    files[path] = json.dumps(obj, indent=2) + '\n'

gl = zipfile.ZipFile(GL_JAR)
def gl_json(path): return json.loads(gl.read(path).decode('utf-8-sig'))

# 1. Legacy data that names a dropped Legacy item -> the YH item of the same name
retarget = [
    'data/gensokyolegacy/loot_table/entities/hakurei_reimu.json', 'data/gensokyolegacy/loot_table/entities/kirisame_marisa.json',
    'data/gensokyolegacy/loot_table/entities/kochiya_sanae.json', 'data/gensokyolegacy/loot_table/entities/mystia_lorelei.json',
    'data/gensokyolegacy/loot_table/entities/yukari_yakumo.json',
    'data/gensokyolegacy/loot_table/blocks/donation_box.json',            # shrine's donation box drops YH's
    'data/gensokyolegacy/gensokyolegacy/dialog_starter/reimu/chat_frog.json',
    'data/gensokyolegacy/gensokyolegacy/trade/morichika/offer_koishi_hat.json',
]
# (the Marisa Koishi-hat quest and its advancement name a quest requirement type / criteria trigger called
#  gensokyolegacy:koishi_hat, not the item, so they are left alone)
ITEM_KEYS = {'item', 'items', 'name', 'id'}
def swap_items(node, key=None):
    if isinstance(node, dict): return {k: swap_items(v, k) for k, v in node.items()}
    if isinstance(node, list): return [swap_items(v, key) for v in node]
    if isinstance(node, str) and key in ITEM_KEYS and node.startswith('gensokyolegacy:') and node.split(':')[1] in DROPPED:
        return 'youkaishomecoming:' + node.split(':')[1]
    return node
for p in retarget:
    put(p, swap_items(gl_json(p)))

# tags: replace wholesale so the Legacy entries are gone
for p in ['data/gensokyolegacy/tags/item/touhou_hat.json', 'data/gensokyolegacy/tags/item/touhou_wings.json',
          'data/gensokyolegacy/tags/item/morichika_offers.json']:
    d = gl_json(p)
    put(p, {'replace': True, 'values': [('youkaishomecoming:' + v.split(':')[1]) if v.startswith('gensokyolegacy:') and v.split(':')[1] in DROPPED else v for v in d['values']]})

# Rinnosuke's stock data map: same entries, YH straw hat instead of Legacy's
d = gl_json('data/gensokyolegacy/data_maps/item/morichika_offer.json')
put('data/gensokyolegacy/data_maps/item/morichika_offer.json',
    {'replace': True, 'values': {('youkaishomecoming:' + k.split(':')[1] if k.split(':')[1] in DROPPED else k): v for k, v in d['values'].items()}})

# Cirno's frozen-frog drops: only Legacy's Cirno (YH's own rule already covers players in YH's hairband), giving YH frogs
for kind in ('cold', 'warm', 'temperate'):
    p = f'data/gensokyolegacy/loot_modifiers/cirno_frozen_frog_{kind}.json'
    d = gl_json(p)
    any_of = d['conditions'][0]
    any_of['terms'] = [t for t in any_of['terms'] if t.get('predicate', {}).get('type') == 'gensokyolegacy:cirno']
    d['item'] = f'youkaishomecoming:frozen_frog_{kind}'
    put(p, d)

# Legacy book pile is dropped (YH's and Supplementaries' stay)
put('data/gensokyolegacy/recipe/book_pile.json', {'neoforge:conditions': [{'type': 'neoforge:false'}],
                                                    'type': 'minecraft:crafting_shapeless', 'ingredients': [], 'result': {'id': 'minecraft:air'}})

# YH straw hat: only obtainable from Rinnosuke's shop
put('data/youkaishomecoming/recipe/straw_hat.json', {'neoforge:conditions': [{'type': 'neoforge:false'}],
                                                       'type': 'minecraft:crafting_shapeless', 'ingredients': [], 'result': {'id': 'minecraft:air'}})

# 2. structures
YH_SHRINE_BIOMES = ['minecraft:cherry_grove', 'minecraft:flower_forest', 'minecraft:meadow', '#youkaishomecoming:youkai_biomes']
put('data/youkaishomecoming/tags/worldgen/biome/has_structure/hakurei_shrine.json', {'replace': True, 'values': []})
put('data/gensokyolegacy/tags/worldgen/biome/has_structure/hakurei_shrine.json', {'replace': True, 'values': YH_SHRINE_BIOMES})
FOREST_OF_MAGIC = ['gensokyolegacy:magical_forest', 'minecraft:forest', 'minecraft:flower_forest', 'minecraft:birch_forest',
                   'minecraft:old_growth_birch_forest', 'minecraft:dark_forest', '#youkaishomecoming:youkai_biomes']
for s in ('marisa_house', 'morichika_shop'):
    put(f'data/gensokyolegacy/tags/worldgen/biome/has_structure/{s}.json', {'replace': True, 'values': FOREST_OF_MAGIC})
gw = zipfile.ZipFile(os.path.join(MODS, 'gensouworks-neoforge-1.21.1-ykhu-0.1.1.jar'))
d = json.loads(gw.read('data/gensouworks/worldgen/structure_set/touhou_common_structures.json'))
d['structures'] = [s for s in d['structures'] if s['structure'] not in ('gensouworks:kirisame_house', 'gensouworks:kourindou')]
put('data/gensouworks/worldgen/structure_set/touhou_common_structures.json', d)

# 3. chest loot in Legacy's buildings. Legacy ships every container empty; they get the tables of the building each
# replaced (Gensouworks Kirisame house / Kourindou, YH shrine; Gensouworks has no shrine), on as many containers as
# that building had. Legacy's cabinets are not loot containers, so only barrels/chests; one half per double chest.
# Legacy's repair only places block states, so it never refills a container.
import gzip, io, nbtlib
# Marisa: every container filled; each one (shop, junk and kitchen alike) has an independent 37.5% chance at one rare
# from the Kirisame shop's rare pool (minus netherite, heavy core and nether star): ~9 per house on average,
# 12+ in about 1 house in 7, 24 possible but astronomically unlikely.
RARE_DROPPED = {'minecraft:netherite_ingot', 'minecraft:heavy_core', 'minecraft:nether_star'}
RARE_CHANCE = 0.375
gw_shop = json.loads(gw.read('data/gensouworks/loot_table/kirisame_house/shop.json'))
common_pool, rare_pool = gw_shop['pools']
rare_pool['entries'] = [e for e in rare_pool['entries'] if e.get('name') not in RARE_DROPPED]
rare_pool['conditions'] = [{'condition': 'minecraft:random_chance', 'chance': RARE_CHANCE}]
put('data/gensokyocompat/loot_table/chests/marisa_house/shop.json', {'type': 'minecraft:chest', 'pools': [common_pool, rare_pool]})
def item(name, w, lo, hi):
    return {'type': 'minecraft:item', 'name': name, 'weight': w,
            'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}]}
knick_knacks = {'rolls': {'type': 'minecraft:uniform', 'min': 2, 'max': 4}, 'bonus_rolls': 0, 'entries': [
    item('minecraft:glass_bottle', 4, 1, 4), item('minecraft:string', 4, 2, 6), item('minecraft:feather', 3, 1, 4),
    item('minecraft:paper', 3, 2, 6), item('minecraft:candle', 3, 1, 3), item('minecraft:glowstone_dust', 3, 1, 4),
    item('minecraft:gunpowder', 3, 1, 4), item('minecraft:sugar', 2, 1, 4), item('minecraft:spider_eye', 2, 1, 2),
    item('minecraft:bone', 2, 1, 3), item('minecraft:ink_sac', 2, 1, 3), item('minecraft:amethyst_shard', 1, 1, 3),
    item('minecraft:flower_pot', 1, 1, 1), item('minecraft:compass', 1, 1, 1)]}
put('data/gensokyocompat/loot_table/chests/marisa_house/junk.json',
    {'type': 'minecraft:chest', 'pools': [common_pool, knick_knacks, rare_pool]})
gw_kitchen = json.loads(gw.read('data/gensouworks/loot_table/kirisame_house/kitchen.json'))
gw_kitchen['pools'].append(rare_pool)
put('data/gensokyocompat/loot_table/chests/marisa_house/kitchen.json', gw_kitchen)
LOOT = {
    'marisa_house': {
        'gensokyocompat:chests/marisa_house/shop': [(19, 8, 11), (19, 8, 12), (21, 8, 9), (22, 8, 9), (20, 7, 9), (20, 8, 9),
                                                    (10, 7, 21), (10, 8, 21), (26, 5, 19), (26, 5, 20), (26, 5, 21), (26, 8, 17)],
        'gensokyocompat:chests/marisa_house/junk': [(19, 3, 22), (23, 3, 9), (15, 5, 22), (16, 5, 22), (19, 5, 22), (20, 5, 22),
                                                    (4, 7, 15), (5, 7, 15), (3, 9, 16)],
        'gensokyocompat:chests/marisa_house/kitchen': [(3, 4, 14), (4, 4, 14), (24, 2, 22)],
    },
    'morichika_shop': {
        'gensouworks:kourindou/shop': [(21, 1, 14), (21, 1, 15), (22, 1, 14), (22, 1, 15), (21, 2, 14), (21, 2, 15),
                                       (22, 2, 14), (22, 2, 15), (29, 1, 11), (29, 2, 14)],
        'gensouworks:kourindou/kitchen': [(24, 8, 19)],
    },
    'hakurei_shrine/root': {
        'youkaishomecoming:chests/hakurei_shrine/chest': [(15, 3, 11)],
        'youkaishomecoming:chests/hakurei_shrine/cabinet': [(15, 2, 11), (3, 4, 12), (3, 4, 13), (12, 5, 11), (12, 5, 12), (12, 5, 13)],
    },
    'hakurei_shrine/warehouse': {
        'youkaishomecoming:chests/hakurei_shrine/chest': [(8, 4, 4)],
        'youkaishomecoming:chests/hakurei_shrine/barrel': [(8, 4, 2), (8, 4, 5), (8, 5, 2), (8, 5, 5), (8, 6, 5)],
    },
}
for name, tables in LOOT.items():
    path = f'data/gensokyolegacy/structure/{name}.nbt'
    t = nbtlib.File.parse(io.BytesIO(gzip.decompress(gl.read(path))))
    want = {p: lt for lt, ps in tables.items() for p in ps}
    done = set()
    for b in t['blocks']:
        p = tuple(int(v) for v in b['pos'])
        if p not in want: continue
        block = str(t['palette'][int(b['state'])]['Name'])
        assert block in ('minecraft:barrel', 'minecraft:chest'), (name, p, block)
        b.setdefault('nbt', nbtlib.Compound())['LootTable'] = nbtlib.String(want[p])
        done.add(p)
    assert done == set(want), (name, set(want) - done)
    buf = io.BytesIO(); t.write(buf)
    files[path] = gzip.compress(buf.getvalue())

# 3. density: Legacy tries one spot per 32-chunk (512-block) cell, which put ~1 shrine per 1.6 km2. 64-chunk cells give
# 4x fewer. spacing lives in BOTH the structure set and each structure (the flat check re-derives the candidates from
# its own copy), so the two must always change together.
GL_SPACING = 64
for s in ('hakurei_shrine', 'magical_forest_houses'):
    d = gl_json(f'data/gensokyolegacy/worldgen/structure_set/{s}.json')
    d['placement']['spacing'] = GL_SPACING
    put(f'data/gensokyolegacy/worldgen/structure_set/{s}.json', d)
for s in ('hakurei_shrine', 'marisa_house', 'morichika_shop'):
    d = gl_json(f'data/gensokyolegacy/worldgen/structure/{s}.json')
    d['spacing'] = GL_SPACING
    put(f'data/gensokyolegacy/worldgen/structure/{s}.json', d)

files['pack.mcmeta'] =json.dumps({'pack': {'pack_format': 48, 'description':
    'Gensokyo bridge: Legacy uses YH items; Legacy shrine/house/shop replace YH and Gensouworks versions'}}, indent=2)
with zipfile.ZipFile(OUT, 'w', zipfile.ZIP_DEFLATED) as z:
    for p, t in sorted(files.items()):
        z.writestr(p, t)
for p in sorted(files): print(p)
print('->', OUT)
