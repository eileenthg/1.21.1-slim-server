"""Finds single-input workstation recipes where one input has several different outputs
(the case CuttingDelight exists to disambiguate). Reads a loaded-recipe dump."""
import json, os, sys, collections, re

HERE = os.path.dirname(os.path.abspath(__file__))
DUMP = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, 'baseline_dump2', 'recipes_before.txt')
TYPES = ('farmersdelight:cutting', 'extradelight:mortar', 'extradelight:drying_rack',
         'youkaishomecoming:drying_rack', 'extradelight:juicer', 'butchery:pestleandmortarrecipetype')

def ing_key(j):
    ing = j.get('ingredients') or j.get('ingredient')
    if isinstance(ing, list):
        if len(ing) != 1:
            return None
        ing = ing[0]
    if isinstance(ing, dict):
        return ing.get('item') or ('#' + ing['tag'] if 'tag' in ing else None)
    if isinstance(ing, str):
        return ing
    return None

def out_key(j):
    r = j.get('result')
    out = []
    def one(x):
        if isinstance(x, str):
            return (x, 1)
        it = x.get('item', x)
        if isinstance(it, str):
            return (it, x.get('count', 1))
        return (it.get('id') or it.get('item'), it.get('count', 1))
    if isinstance(r, list):
        out = [one(x) for x in r]
    elif isinstance(r, (dict, str)):
        out = [one(r)]
    return tuple(sorted(out))

by = collections.defaultdict(list)
for line in open(DUMP, encoding='utf-8'):
    if '=>' not in line or line.startswith('#'):
        continue
    rid, js = line.split('=>', 1)
    try:
        j = json.loads(js.strip())
    except Exception:
        continue
    if j.get('type') not in TYPES:
        continue
    k = ing_key(j)
    tool = json.dumps(j.get('tool', ''), sort_keys=True)
    if k:
        by[(j['type'], k, tool)].append((rid.strip(), out_key(j)))

n = 0
for (t, k, tool), lst in sorted(by.items()):
    outs = {o for _, o in lst}
    if len(outs) > 1:
        n += 1
        tl = re.findall(r'"(?:tag|item|action)":\s*"([^"]+)"', tool)
        print('%-24s %-30s tool=%s' % (t.split(':')[-1], k, '/'.join(tl) or 'any'))
        for rid, o in sorted(lst):
            print('      %-56s -> %s' % (rid, ', '.join('%s x%d' % (i, c) for i, c in o)))
print('\n%d inputs with more than one outcome' % n)
