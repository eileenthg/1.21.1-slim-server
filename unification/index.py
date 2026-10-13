"""Builds a recipe/item index of the instance: names, recipes (jar + instance datapacks, later overrides earlier),
MineColonies-style crafter recipes, and item tags. Output: index.json"""
import zipfile, glob, os, re, json, collections
INST = r"C:\Users\eteil.000\AppData\Roaming\PrismLauncher\instances\1.21.1 slim\minecraft"
OUT = os.path.dirname(os.path.abspath(__file__))

names, item_models = {}, set()
recipes = {}          # id -> {type, src, json}
crafter = {}          # path -> {src, json}
tags = collections.defaultdict(lambda: {'values': [], 'replace': False})
loot_mods = {}

def load_json(b):
    try:
        return json.loads(b.decode('utf-8-sig', 'ignore'), strict=False)
    except Exception:
        return None

def scan(z, src):
    for n in z.namelist():
        if not n.endswith('.json'):
            continue
        m = re.match(r'^data/([^/]+)/recipes?/(.+)\.json$', n)
        if m:
            j = load_json(z.read(n))
            if isinstance(j, dict):
                recipes[m.group(1) + ':' + m.group(2)] = {'src': src, 'json': j, 'path': n}
            continue
        m = re.match(r'^data/([^/]+)/crafterrecipes/(.+)\.json$', n)
        if m:
            j = load_json(z.read(n))
            if isinstance(j, dict):
                crafter[m.group(1) + ':' + m.group(2)] = {'src': src, 'json': j, 'path': n}
            continue
        m = re.match(r'^data/([^/]+)/tags/items?/(.+)\.json$', n)
        if m:
            j = load_json(z.read(n))
            if isinstance(j, dict):
                key = m.group(1) + ':' + m.group(2)
                if j.get('replace'):
                    tags[key] = {'values': [], 'replace': True}
                for v in j.get('values', []):
                    v = v.get('id') if isinstance(v, dict) else v
                    if isinstance(v, str):
                        tags[key]['values'].append(v)
            continue
        m = re.match(r'^assets/([^/]+)/models/item/([^/]+)\.json$', n)
        if m:
            item_models.add(m.group(1) + ':' + m.group(2))
            continue
        m = re.match(r'^assets/([^/]+)/lang/en_us\.json$', n)
        if m:
            d = load_json(z.read(n))
            if isinstance(d, dict):
                for k, v in d.items():
                    mm = re.match(r'^(item|block)\.([a-z0-9_.-]+)\.([a-z0-9_/.-]+)$', k)
                    if mm and isinstance(v, str):
                        rid = mm.group(2) + ':' + mm.group(3)
                        if rid not in names or mm.group(1) == 'item':
                            names[rid] = v

jars = sorted(glob.glob(os.path.join(INST, 'mods', '*.jar')))
for f in jars:
    try:
        z = zipfile.ZipFile(f)
    except Exception:
        continue
    scan(z, 'jar:' + os.path.basename(f))
    # jar-in-jar (e.g. bundled libraries with data)
    for n in z.namelist():
        if n.startswith('META-INF/jarjar/') and n.endswith('.jar'):
            try:
                import io
                scan(zipfile.ZipFile(io.BytesIO(z.read(n))), 'jij:' + os.path.basename(n))
            except Exception:
                pass
for f in sorted(glob.glob(os.path.join(INST, 'datapacks', '*.zip'))):
    scan(zipfile.ZipFile(f), 'dp:' + os.path.basename(f))

json.dump({'names': names, 'item_models': sorted(item_models), 'recipes': recipes, 'crafter': crafter,
           'tags': {k: v for k, v in tags.items()}}, open(os.path.join(OUT, 'index.json'), 'w'))
print('names', len(names), 'recipes', len(recipes), 'crafter', len(crafter), 'tags', len(tags))
print('recipe types', collections.Counter(r['json'].get('type') for r in recipes.values()).most_common(60))
