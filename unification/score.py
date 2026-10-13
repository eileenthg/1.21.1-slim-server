"""For each candidate group: list every recipe whose primary result is a member, classify storage conversions,
score the rest by process depth, and propose a dominant. Writes score.json and prints a review table."""
import json, os, re, collections
from candidates import GROUPS
OUT = os.path.dirname(os.path.abspath(__file__))
d = json.load(open(os.path.join(OUT, 'index.json')))
names, R, C, rawtags = d['names'], d['recipes'], d['crafter'], d['tags']
ID = re.compile(r'^[a-z0-9_.-]+:[a-z0-9_/.-]+$')

def result_ids(j):
    out = []
    for k in ('result', 'results', 'output', 'outputs', 'result_item'):
        v = j.get(k)
        vals = v if isinstance(v, list) else [v]
        for x in vals[:1] if k in ('results', 'outputs') else vals:
            if isinstance(x, str) and ID.match(x): out.append(x)
            elif isinstance(x, dict):
                for kk in ('id', 'item'):
                    y = x.get(kk)
                    if isinstance(y, str) and ID.match(y): out.append(y)
                    elif isinstance(y, dict) and isinstance(y.get('id'), str): out.append(y['id'])
    return out

produced = collections.defaultdict(list)
for rid, r in R.items():
    for o in result_ids(r['json']):
        produced[o].append(('recipe', rid, r))
for cid, r in C.items():
    for o in result_ids(r['json']):
        produced[o].append(('crafter', cid, r))
all_products = set(produced)

TYPE_W = {
    'minecraft:crafting_shapeless': 1, 'minecraft:crafting_shaped': 1, 'minecraft:stonecutting': 1,
    'minecraft:smelting': 1.2, 'minecraft:smoking': 1.2, 'minecraft:campfire_cooking': 1.2, 'minecraft:blasting': 1.2,
    'farmersdelight:cutting': 1.5, 'farmersdelight:cooking': 3, 'extradelight:oven': 3, 'extradelight:mixing_bowl': 3,
    'extradelight:mortar': 2.5, 'extradelight:chiller': 3, 'extradelight:juicer': 2.5, 'extradelight:feast': 3,
    'extradelight:dough_shaping': 2.5, 'extradelight:vat': 5, 'extradelight:evaporator': 4, 'extradelight:melting_pot': 3,
    'brewinandchewin:fermenting': 5, 'youkaishomecoming:simple_fermentation': 4, 'youkaishomecoming:unordered_cooking': 3.5,
    'youkaishomecoming:cuisine_mixed': 3, 'youkaishomecoming:cuisine_ordered': 3, 'youkaishomecoming:boiling': 3,
    'youkaishomecoming:kettle': 3, 'youkaishomecoming:drying_rack': 2.5, 'youkaishomecoming:steaming': 3,
    'spectrum:pedestal': 2, 'spectrum:titration_barrel': 5, 'spectrum:pedestal_shapeless': 2,
    'snowyspirit:gingerbread': 2, 'toughasnails:water_purifying': 2,
}
def storage_conversion(rid, j):
    s = rid.lower()
    if re.search(r'uncraft|from_crate|from_block|from_bucket|from_bag|from_sack|_block_to_item|to_item|from_pancake|compress|decompress|_storage', s):
        return True
    ings = json.dumps(j.get('ingredients', j.get('ingredient', j.get('key', ''))))
    if re.search(r'(crate|_bag|sack|bucket|_block)"', ings) and j.get('type', '').startswith('minecraft:crafting'):
        n_ing = len(re.findall(r'"(?:item|tag|id)"', ings))
        return n_ing <= 1
    return False

def ing_ids(j):
    blob = {k: v for k, v in j.items() if k not in ('result', 'results', 'output', 'outputs', 'result_item', 'container')}
    items = re.findall(r'"(?:item|id)": "([a-z0-9_.-]+:[a-z0-9_/.-]+)"', json.dumps(blob))
    tags = re.findall(r'"tag": "([a-z0-9_.-]+:[a-z0-9_/.-]+)"', json.dumps(blob))
    return items, tags

def score(kind, j):
    t = j.get('type', '') if kind == 'recipe' else 'crafter'
    w = TYPE_W.get(t, 2 if kind == 'recipe' else 1)
    items, tags = ing_ids(j)
    n = len(items) + len(tags)
    inter = sum(1 for i in items if i in all_products and not i.startswith('minecraft:'))
    fluids = len(re.findall(r'"fluid', json.dumps(j)))
    stages = len(j.get('stage_ingredients', [])) if isinstance(j.get('stage_ingredients'), list) else 0
    return round(w + 0.25 * n + 0.5 * inter + 0.5 * min(fluids, 2) + 1.0 * stages, 2), t

rows = []
for g in GROUPS:
    missing = [i for i in g if i not in names and i not in all_products]
    members = []
    for i in g:
        recs = []
        for kind, rid, r in produced.get(i, []):
            j = r['json']
            conv = storage_conversion(rid, j)
            s, t = score(kind, j)
            recs.append({'kind': kind, 'id': rid, 'type': t, 'score': s, 'conversion': conv, 'src': r['src']})
        members.append({'id': i, 'name': names.get(i), 'recipes': recs})
    creation = [(r['score'], m['id'], r) for m in members for r in m['recipes'] if not r['conversion'] and r['kind'] == 'recipe']
    best = max(creation, key=lambda x: x[0]) if creation else None
    rows.append({'group': g, 'missing': missing, 'members': members, 'best': best and {'item': best[1], 'recipe': best[2]['id'], 'score': best[0]}})
json.dump(rows, open(os.path.join(OUT, 'score.json'), 'w'), indent=1)
for row in rows:
    b = row['best']
    print('== %s  -> best: %s' % (' / '.join(row['group']), ('%s via %s (%.2f)' % (b['item'], b['recipe'], b['score'])) if b else 'NONE'))
    if row['missing']: print('   MISSING IDS:', row['missing'])
    for m in row['members']:
        print('   %-42s %-28s %s' % (m['id'], (m['name'] or '')[:28], ' | '.join('%s%s<%s>%.1f' % ('*' if r['conversion'] else '', r['id'], r['type'], r['score']) for r in m['recipes'])[:420]))
