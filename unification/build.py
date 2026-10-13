"""Builds the item merge:
  out/almostunified/tags.json, out/almostunified/unification/materials.json, out/almostunified/placeholders.json
  out/zz-unified-items/ (datapack): disabled duplicate recipes, MineColonies crafter recipes retargeted,
  MineColonies crafting-table shortcuts turned into colony-only crafter recipes. Crop-freedom content is folded in.
  out/report.json with every decision."""
import json, os, re, glob, zipfile, collections, shutil, copy
from decisions import D, FORCE_DISABLE, KEEP_EXTRA

HERE = os.path.dirname(os.path.abspath(__file__))
INST = r"C:\Users\eteil.000\AppData\Roaming\PrismLauncher\instances\1.21.1 slim\minecraft"
DUMP = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'baseline_dump2', 'tags.txt')
OUT = os.path.join(HERE, 'out')
shutil.rmtree(OUT, ignore_errors=True)
PACK = os.path.join(OUT, 'zz-unified-items')
os.makedirs(PACK)

idx = json.load(open(os.path.join(HERE, 'index.json')))
R, CR, TAGS = idx['recipes'], idx['crafter'], idx['tags']

# ---------------------------------------------------------------- namespace -> jar
ns_jar = {}
for f in glob.glob(os.path.join(INST, 'mods', '*.jar')):
    try:
        z = zipfile.ZipFile(f)
    except Exception:
        continue
    for n in z.namelist():
        m = re.match(r'^assets/([^/]+)/lang/en_us\.json$', n)
        if m:
            ns_jar.setdefault(m.group(1), os.path.basename(f))

dom_of = {}
groups = []
for dom, others, why in D:
    members = [dom] + others
    for m in members:
        dom_of[m] = dom
    groups.append({'dominant': dom, 'members': members, 'why': why})
nondom = {m: g['dominant'] for g in groups for m in g['members'] if m != g['dominant']}

# ---------------------------------------------------------------- AlmostUnified config
# Always build from the pristine pre-merge config, never the installed one: otherwise each run inherits
# the unify tags the previous run added, and items end up in two unify tags at once.
cfg = 'C:/Users/eteil.000/Claude/modpack-audit/proposed-configs/almostunified-BEFORE-MERGE'
tags_cfg = json.load(open(os.path.join(cfg, 'tags.json'), encoding='utf-8'))
mat = json.load(open(os.path.join(cfg, 'unification', 'materials.json'), encoding='utf-8'))
ph = json.load(open(os.path.join(cfg, 'placeholders.json'), encoding='utf-8'))
custom = tags_cfg['custom_tags']
# Mods removed from the pack since the pre-merge config was taken; their entries are dropped.
REMOVED_MODS = ('anotherscape', 'toughasnails', 'tanca')   # removed 19 Sep 2026 (TAN -> Thirst Was Taken + Cold Sweat)
for _t in list(custom):
    custom[_t] = [i for i in custom[_t] if i.split(':')[0] not in REMOVED_MODS]
mat['mod_priorities'] = [m for m in mat['mod_priorities'] if m not in REMOVED_MODS]

dump_tag_items = {}
item_utag = {}
for line in open(DUMP, encoding='utf-8'):
    if '=>' not in line:
        continue
    t, items = line.split('=>')
    t = t.strip()
    items = [x.strip() for x in items.split(',')]
    dump_tag_items[t] = items
    for i in items:
        item_utag[i] = t

# over-merges found in the current setup
REMOVE_UNIFY_TAGS = ['c:dough',
                     # Precaution (29 Sep): AU default tag that Rustic Delight fills with all nine pepper
                     # COLOURS. AU leaves it alone today because every member is one mod, but a second pepper
                     # mod would make it collapse the colours. Verified against logs/almostunified.
                     'c:crops/bell_pepper']
IGNORE_ITEMS = ['farmersdelight:cabbage_leaf', 'culturaldelights:cut_eggplant', 'culturaldelights:white_eggplant',
                'cluttered:chiseled_gold_block', 'quark:iron_plate', 'butchery:raw_pufferfish',
                # variants the shared tags really were collapsing, per logs/almostunified (29 Sep):
                # '#c:ores/gold' hid karvat/gloomslate/sculk_stone/nether gold ore, '#c:iron_golem_head' hid the
                # repaired head. amber_sand_ore is precautionary (same-mod tag, not merged today).
                'eternal_tales:amber_sand_ore', 'eternal_tales:karvat_gold_ore', 'eternal_tales:rayana_gold_ore',
                'butchery:repaired_iron_golem_head', 'minecraft:nether_gold_ore']
# Deeper Darker's ore stones are variants like deepslate, not duplicates of the vanilla ores.
for _v in ('gloomslate', 'sculk_stone'):
    if _v not in mat['stone_variants']:
        mat['stone_variants'].append(_v)
mat['tags'] = [t for i, t in enumerate(mat['tags']) if t not in REMOVE_UNIFY_TAGS and t not in mat['tags'][:i]]
for t in REMOVE_UNIFY_TAGS:
    for i in dump_tag_items.get(t, []):
        item_utag.pop(i, None)
for i in IGNORE_ITEMS:
    if i not in mat['ignored_items']:
        mat['ignored_items'].append(i)
    item_utag.pop(i, None)
if 'c:plates/iron' in custom and 'quark:iron_plate' in custom['c:plates/iron']:
    custom['c:plates/iron'].remove('quark:iron_plate')
if 'adamantite' not in ph['material']:
    ph['material'].append('adamantite')

METAL_TAG = {}
for metal in ('silver', 'tin', 'adamantite'):
    METAL_TAG['mythicmetals:raw_%s' % metal] = 'c:raw_materials/%s' % metal
    METAL_TAG['mythicmetals:raw_%s_block' % metal] = 'c:storage_blocks/raw_%s' % metal
    METAL_TAG['mythicmetals:%s_nugget' % metal] = 'c:nuggets/%s' % metal
    METAL_TAG['mythicmetals:%s_block' % metal] = 'c:storage_blocks/%s' % metal
    METAL_TAG['mythicmetals:%s_ingot' % metal] = 'c:ingots/%s' % metal

report = {'groups': [], 'conflicts': [], 'config': {}}
unify_tags = set(mat['tags'])
for g in groups:
    dom, members = g['dominant'], g['members']
    existing = sorted({item_utag[m] for m in members if m in item_utag})
    if dom in METAL_TAG:
        tag = METAL_TAG[dom]
    elif len(existing) == 1:
        tag = existing[0]
    elif len(existing) > 1:
        # members already sit in different unify tags: keep the dominant's tag, report the rest
        tag = item_utag.get(dom) or existing[0]
        report['conflicts'].append({'group': members, 'tags': existing, 'used': tag})
    else:
        tag = 'c:unified/' + dom.split(':')[1]
    extras = [i for i in dump_tag_items.get(tag, []) if i not in members and i not in IGNORE_ITEMS]
    lst = custom.setdefault(tag, [])
    for m in members:
        if m not in lst:
            lst.append(m)
    if tag not in unify_tags and not any('{material}' in t and re.fullmatch(t.replace('{material}', '[a-z_]+'), tag) for t in unify_tags):
        mat['tags'].append(tag)
        unify_tags.add(tag)
    mat['priority_overrides'][tag] = dom.split(':')[0]
    g['tag'] = tag
    g['tag_extras'] = extras

# corn dough/wheat dough replaced c:dough; make sure those custom tags exist (done by groups above)
os.makedirs(os.path.join(OUT, 'almostunified', 'unification'))
json.dump(tags_cfg, open(os.path.join(OUT, 'almostunified', 'tags.json'), 'w', encoding='utf-8'), indent=2, ensure_ascii=False)
for _rid in ['create:mixing/apple_cider', 'create:mixing/black_tea', 'create:mixing/green_tea', 'create:mixing/yellow_tea']:
    if _rid not in mat['ignored_recipe_ids']:
        mat['ignored_recipe_ids'].append(_rid)   # fluid results: unifying the id would swap a fluid for an item
json.dump(mat, open(os.path.join(OUT, 'almostunified', 'unification', 'materials.json'), 'w', encoding='utf-8'), indent=2, ensure_ascii=False)
json.dump(ph, open(os.path.join(OUT, 'almostunified', 'placeholders.json'), 'w', encoding='utf-8'), indent=2, ensure_ascii=False)

# ---------------------------------------------------------------- recipe classification
ID = re.compile(r'^[a-z0-9_.-]+:[a-z0-9_/.-]+$')
def results(j):
    out = []
    for k in ('result', 'results', 'output', 'outputs', 'result_item'):
        v = j.get(k)
        vals = v if isinstance(v, list) else [v]
        if k in ('results', 'outputs'):
            vals = vals[:1]
        for x in vals:
            if isinstance(x, str) and ID.match(x):
                out.append(x)
            elif isinstance(x, dict):
                for kk in ('id', 'item'):
                    y = x.get(kk)
                    if isinstance(y, str) and ID.match(y):
                        out.append(y)
                    elif isinstance(y, dict) and isinstance(y.get('id'), str):
                        out.append(y['id'])
    return out

VANILLA_SHORT = {'minecraft:crafting_shapeless', 'minecraft:crafting_shaped', 'minecraft:smelting', 'minecraft:smoking',
                 'minecraft:campfire_cooking', 'minecraft:blasting'}
FLUID_CONV = {'brewinandchewin:keg_pouring', 'farmersrespite:kettle_pouring', 'create:emptying', 'extradelight:bottle_fluid'}

def is_conversion(rid, j):
    s = rid.lower()
    if re.search(r'uncraft|from_crate|from_block|from_bucket|from_bag|from_sack|_block_to_item|to_block|from_barrel|block_reverse|from_slices?$|_from_nugget|from_ingot', s):
        return True
    if re.search(r'wild', s):
        return True
    t = j.get('type', '')
    if t in FLUID_CONV:
        return True
    ings = json.dumps({k: v for k, v in j.items() if k not in ('result', 'results', 'output', 'outputs')})
    ids = re.findall(r'"(?:item|id|tag)": "([^"]+)"', ings)
    if t.startswith('minecraft:crafting') and len(set(ids)) == 1 and re.search(r'(crate|_bag|sack|bucket|_block|barrel)$', ids[0]):
        return True
    return False

LOADED = {}
for line in open(DUMP.replace('tags.txt', 'recipes_before.txt'), encoding='utf-8'):
    if '=>' not in line or line.startswith('#'):
        continue
    rid, js = line.split('=>', 1)
    try:
        LOADED[rid.strip()] = json.loads(js.strip())
    except Exception:
        pass

by_item = collections.defaultdict(list)
any_path = collections.defaultdict(int)
for rid, j in LOADED.items():
    ns, path = rid.split(':', 1)
    r = {'json': j, 'src': (R.get(rid) or {}).get('src', 'loaded'), 'path': 'data/%s/recipe/%s.json' % (ns, path)}
    for o in results(j):
        if o in dom_of:
            by_item[o].append((rid, r))
    blob = json.dumps({k: v for k, v in j.items() if k in ('result', 'results', 'output', 'outputs', 'result_item')})
    for o in re.findall(r'"([a-z0-9_.-]+:[a-z0-9_/.-]+)"', blob):
        if o in dom_of:
            any_path[o] += 1

# pods_cutting: Youkai's crop and pods bag still drop pods; without this recipe they are a dead end (AU retargets its output to ED soybeans)
KEEP_IDS = {'eternal_tales:shrimp_craft', 'extradelight:quiche_slice', 'farmersdelight:apple_pie_from_slices', 'ramadandelight:quiche_from_slice',
            'youkaishomecoming:cutting/pods_cutting'}
METAL_GROUP = lambda dom: dom.startswith('mythicmetals:')
KEEP_DECIDED = set(KEEP_EXTRA)

def n_results(j):
    for k in ('result', 'results', 'output', 'outputs'):
        v = j.get(k)
        if isinstance(v, list):
            return len(v)
        if v is not None:
            return 1
    return 1

disabled, kept = [], []
mc_to_crafter = []
for g in groups:
    dom = g['dominant']
    entries = []
    for m in g['members']:
        for rid, r in by_item.get(m, []):
            j = r['json']
            t = j.get('type', '')
            if rid in FORCE_DISABLE:
                continue    # switched off by name below
            if rid in KEEP_DECIDED:
                kept.append({'recipe': rid, 'item': m, 'type': t, 'why': 'kept by decision (decisions.KEEP_EXTRA)'})
            elif n_results(j) > 1:
                kept.append({'recipe': rid, 'item': m, 'type': t, 'why': 'several outputs; this item may be a byproduct'})
            elif rid in KEEP_IDS or is_conversion(rid, j):
                kept.append({'recipe': rid, 'item': m, 'type': t, 'why': 'conversion'})
            elif t.startswith('create:') and m == dom:
                kept.append({'recipe': rid, 'item': m, 'type': t, 'why': 'Create automation of the kept item'})
            else:
                entries.append((m, rid, r, t))
    dom_entries = [e for e in entries if e[0] == dom]
    dom_station = [e for e in dom_entries if e[3] not in VANILLA_SHORT and not e[3].startswith('create:')]
    keep_nondom = set()
    if not dom_entries and not any_path.get(dom):
        # the kept item has no crafting recipe of its own (loot/harvest only); keep the best other recipe so it stays craftable
        others = [e for e in entries if not e[1].startswith('minecolonies:') and not e[1].startswith('minecolonies_compatibility:')]
        if others:
            best_station = [e for e in others if e[3] not in VANILLA_SHORT]
            pool = best_station or others
            best_mod = pool[0][1].split(':')[0]
            keep_nondom = {e[1] for e in pool if e[1].split(':')[0] == best_mod}
    for m, rid, r, t in entries:
        if rid in keep_nondom:
            kept.append({'recipe': rid, 'item': m, 'type': t, 'why': 'kept item has no recipe of its own'})
            continue
        if m == dom:
            if dom_station and t in VANILLA_SHORT and not METAL_GROUP(dom):
                disabled.append({'recipe': rid, 'item': m, 'type': t, 'src': r['src'], 'path': r['path'], 'why': 'shortcut; a station recipe exists'})
            else:
                kept.append({'recipe': rid, 'item': m, 'type': t, 'why': 'kept recipe'})
            continue
        disabled.append({'recipe': rid, 'item': m, 'type': t, 'src': r['src'], 'path': r['path'], 'why': 'duplicate of %s' % dom})
        if rid.startswith('minecolonies:') and t in VANILLA_SHORT and m.startswith('minecolonies:'):
            mc_to_crafter.append((rid, r, m, dom))

# recipes switched off by name (decisions.FORCE_DISABLE); FDE's are not in the 16 Sep baseline dump, so they come only from here
for rid, why in FORCE_DISABLE.items():
    ns, path = rid.split(':', 1)
    disabled.append({'recipe': rid, 'item': None, 'type': LOADED.get(rid, {}).get('type', '?'), 'src': 'forced',
                     'path': 'data/%s/recipe/%s.json' % (ns, path), 'why': why})

# write disabled recipe overrides
DISABLED_JSON = {"neoforge:conditions": [{"type": "neoforge:false"}], "type": "minecraft:crafting_shapeless",
                 "ingredients": [{"item": "minecraft:barrier"}], "result": {"id": "minecraft:barrier"}}
for e in disabled:
    p = os.path.join(PACK, *e['path'].split('/'))
    os.makedirs(os.path.dirname(p), exist_ok=True)
    json.dump(DISABLED_JSON, open(p, 'w'), indent=2)

# ---------------------------------------------------------------- MineColonies crafter recipes
def retarget(o):
    if isinstance(o, dict):
        return {k: (nondom.get(v, v) if k in ('id', 'item') and isinstance(v, str) else retarget(v)) for k, v in o.items()}
    if isinstance(o, list):
        return [retarget(x) for x in o]
    return o

retargeted = []
# crop-freedom content first (it overrides MineColonies files), then every crafter recipe
_cf = os.path.join(INST, 'datapacks', 'minecolonies-crop-freedom.zip')
if not os.path.exists(_cf):
    _cf = os.path.join(r'C:\Users\eteil.000\Claude\modpack-audit\datapacks-retired', 'minecolonies-crop-freedom.zip.FOLDED-INTO-zz-unified-items')
cf = zipfile.ZipFile(_cf)
cf_paths = set()
for n in cf.namelist():
    if n.endswith('/') or n == 'pack.mcmeta':
        continue
    cf_paths.add(n)
    p = os.path.join(PACK, *n.split('/'))
    if os.path.exists(p):
        continue  # already disabled above
    data = cf.read(n)
    if n.endswith('.json'):
        j = json.loads(data)
        j2 = retarget(j)
        if j2 != j:
            retargeted.append(n)
        os.makedirs(os.path.dirname(p), exist_ok=True)
        json.dump(j2, open(p, 'w'), indent=2)
    else:
        os.makedirs(os.path.dirname(p), exist_ok=True)
        open(p, 'wb').write(data)

for cid, r in CR.items():
    if r['path'] in cf_paths:
        continue
    j2 = retarget(r['json'])
    if j2 != r['json']:
        p = os.path.join(PACK, *r['path'].split('/'))
        os.makedirs(os.path.dirname(p), exist_ok=True)
        json.dump(j2, open(p, 'w'), indent=2)
        retargeted.append(r['path'])

# MineColonies vanilla recipes that are kept but still name merged items (AU also fixes these; explicit is safer for workers)
for rid, r in R.items():
    if not rid.startswith('minecolonies:') or '/recipes/' in r['path']:
        continue
    p = os.path.join(PACK, *r['path'].split('/'))
    if os.path.exists(p):
        continue
    j2 = retarget(r['json'])
    if j2 != r['json']:
        os.makedirs(os.path.dirname(p), exist_ok=True)
        json.dump(j2, open(p, 'w'), indent=2)
        retargeted.append(r['path'])

# crafting-table shortcuts from MineColonies become colony-only crafter recipes
def rep_item(tag):
    vals = TAGS.get(tag, {}).get('values', [])
    for v in vals:
        if not v.startswith('#'):
            return v
    for v in vals:
        if v.startswith('#'):
            x = rep_item(v[1:])
            if x:
                return x
    return None

cook = set(TAGS.get('minecolonies:cook_product', {}).get('values', []))
baker = set(TAGS.get('minecolonies:baker_product', {}).get('values', []))
converted = []
for rid, r, m, dom in mc_to_crafter:
    j = r['json']
    t = j['type']
    counts = collections.Counter()
    if t == 'minecraft:crafting_shapeless':
        ings = j['ingredients']
    elif t == 'minecraft:crafting_shaped':
        ings = [j['key'][ch] for row in j['pattern'] for ch in row if ch != ' ']
    else:
        ings = [j['ingredient']]
    ok = True
    for ing in ings:
        if isinstance(ing, list):
            ing = ing[0]
        iid = ing.get('item') or ing.get('id') or (rep_item(ing['tag']) if 'tag' in ing else None)
        if not iid:
            ok = False
            break
        counts[nondom.get(iid, iid)] += 1
    if not ok:
        continue
    if m in baker:
        crafter, folder = ('baker_smelting' if 'smelt' in t or 'smok' in t or 'campfire' in t else 'baker_crafting'), 'baker'
    else:
        crafter, folder = 'chef_crafting', 'chef'
    res = j.get('result', {})
    out = {"type": "recipe", "crafter": crafter,
           "inputs": [{"id": k, "count": v} if v > 1 else {"id": k} for k, v in counts.items()],
           "intermediate": "minecraft:furnace" if crafter == 'baker_smelting' else "minecraft:air",
           "result": {"id": dom, "count": res.get('count', 1)} if res.get('count', 1) > 1 else {"id": dom},
           "show-tooltip": True}
    name = 'unified_' + rid.split(':')[1].replace('/', '_')
    p = os.path.join(PACK, 'data', 'minecolonies', 'crafterrecipes', folder, name + '.json')
    os.makedirs(os.path.dirname(p), exist_ok=True)
    json.dump(out, open(p, 'w'), indent=2)
    converted.append({'from': rid, 'to': 'minecolonies:' + folder + '/' + name, 'crafter': crafter, 'result': dom})

json.dump({"pack": {"pack_format": 48, "description": "Merged duplicate items: only the most involved recipe per item is left; MineColonies workers use the merged items (includes the old minecolonies-crop-freedom pack)."}},
          open(os.path.join(PACK, 'pack.mcmeta'), 'w'), indent=2)

# ---------------------------------------------------------------- references to merged items elsewhere in MineColonies data
refs = collections.defaultdict(list)
for f in glob.glob(os.path.join(INST, 'mods', '*coloni*.jar')) + glob.glob(os.path.join(INST, 'mods', 'mctradepost*.jar')):
    z = zipfile.ZipFile(f)
    for n in z.namelist():
        if n.endswith('.json') and n.startswith('data/') and '/recipe' not in n and '/crafterrecipes/' not in n and '/tags/' not in n and '/loot_table' not in n and '/advancement' not in n:
            s = z.read(n).decode('utf-8', 'ignore')
            for m in nondom:
                if '"%s"' % m in s:
                    refs[m].append(os.path.basename(f) + ':' + n)

# MineColonies recruitment costs and Trade Post values that name merged items
extra_files = []
mcj = zipfile.ZipFile(glob.glob(os.path.join(INST, 'mods', 'minecolonies-*.jar'))[0])
for n in mcj.namelist():
    if n.startswith('data/minecolonies/colony/recruitment_items/') and n.endswith('.json'):
        j = json.loads(mcj.read(n))
        if j.get('item') in nondom:
            j['item'] = nondom[j['item']]
            p = os.path.join(PACK, *n.split('/'))
            os.makedirs(os.path.dirname(p), exist_ok=True)
            json.dump(j, open(p, 'w'), indent=2)
            extra_files.append(n)
tpj = glob.glob(os.path.join(INST, 'mods', 'mctradepost-*.jar'))
if tpj:
    vals = json.loads(zipfile.ZipFile(tpj[0]).read('data/mctradepost/item_values.json'))['values']
    add = {}
    for m, dom in nondom.items():
        if m in vals and dom not in vals:
            add[dom] = max(add.get(dom, 0), vals[m])
    if add:
        p = os.path.join(PACK, 'data', 'mctradepost', 'item_values.json')
        os.makedirs(os.path.dirname(p), exist_ok=True)
        json.dump({"replace": False, "values": add}, open(p, 'w'), indent=2)
        extra_files.append('data/mctradepost/item_values.json (+%d values)' % len(add))
# ---------------------------------------------------------------- alternative pipelines the user asked to keep (16 Sep)
# Chocolate: Spectrum fresh chocolate, Create bar and Extra Delight milk bar are interchangeable via #c:chocolate/milk/bar.
# Spectrum's potion-workshop hot chocolate accepts any milk chocolate (tanca's hot chocolate is gone since 19 Sep).
# Expanded Delight's juicer keeps its juices, but takes purified water like Tough As Nails' recipe.
CHOC = 'c:chocolate/milk/bar'
def jar_json(glob_pat, path):
    return json.loads(zipfile.ZipFile(glob.glob(os.path.join(INST, 'mods', glob_pat))[0]).read(path))
def put(path, j, why):
    p = os.path.join(PACK, *path.split('/'))
    os.makedirs(os.path.dirname(p), exist_ok=True)
    json.dump(j, open(p, 'w'), indent=2)
    extra_files.append('%s (%s)' % (path, why))
def swap_choc(o):
    if isinstance(o, dict):
        if o.get('item') == 'spectrum:fresh_chocolate':
            o = {('tag' if k == 'item' else k): (CHOC if k == 'item' else v) for k, v in o.items()}
            return o
        return {k: ('#' + CHOC if v == 'spectrum:fresh_chocolate' and k != 'id' else swap_choc(v)) for k, v in o.items()}
    if isinstance(o, list):
        return [swap_choc(x) for x in o]
    return o
alt_paths = set()
for rp in ['pedestal/tier1/food/berliner', 'pedestal/tier1/food/bodacious_berry_bar', 'pedestal/tier1/food/chauve_souris_au_vin',
           'pedestal/tier1/food/slushslide', 'titration_barrel/le_fishe_au_chocolat', 'potion_workshop_crafting/hot_chocolate']:
    path = 'data/spectrum/recipe/%s.json' % rp
    j = swap_choc(jar_json('spectrum-*.jar', path))
    put(path, j, 'accepts any milk chocolate')
    alt_paths.add(path)
j = jar_json('create-1.21.1-*.jar', 'data/create/recipe/mixing/chocolate_melting.json')
j['ingredients'] = [{'tag': CHOC}]
put('data/create/recipe/mixing/chocolate_melting.json', j, 'melts any milk chocolate')
j = jar_json('extradelight-*.jar', 'data/extradelight/recipe/melting/milk_chocolate_bar.json')
j = json.loads(json.dumps(j).replace('{"item": "extradelight:milk_chocolate_bar"}', '{"tag": "%s"}' % CHOC))
put('data/extradelight/recipe/melting/milk_chocolate_bar.json', j, 'melts any milk chocolate')
for tag, vals in [('chocolate/milk/bar', ['spectrum:fresh_chocolate']),
                  ('foods/chocolate', ['extradelight:milk_chocolate_bar']),
                  ('chocolate', ['spectrum:fresh_chocolate', 'extradelight:milk_chocolate_bar'])]:
    put('data/c/tags/item/%s.json' % tag, {'replace': False, 'values': vals}, 'chocolate interchangeable')
# Juicers: Extra Delight's crank juicer is the only one. Expanded Delight's juicer block and its juicing
# recipes go away; juices that its juicer made come from the Extra Delight juicer or its mixing bowl instead.
# Every juice keeps the purified-water step. Since 19 Sep that is Thirst Was Taken's purified water: a water bottle
# with thirst:purity 3 (Tough As Nails and its purified water bottle are gone).
PURIFIED_COMPONENTS = {'minecraft:potion_contents': {'potion': 'minecraft:water'}, 'thirst:purity': 3}
PURIFIED_ING = {'type': 'neoforge:components', 'items': 'minecraft:potion', 'components': PURIFIED_COMPONENTS, 'strict': False}
# Extra Delight's mixing bowl compares the container item only, so there purity can't be enforced.
PURIFIED_STACK = {'count': 1, 'id': 'minecraft:potion', 'components': PURIFIED_COMPONENTS}
for path in ['data/expandeddelight/recipe/juicer.json'] +             ['data/expandeddelight/recipe/juicing/%s_juice.json' % f for f in
             ['apple', 'cranberry', 'glow_berry', 'melon', 'sweet_berry']]:
    put(path, DISABLED_JSON, 'juicer-only juices')
put('data/zz_unified/recipe/juicer_from_expanded_delight.json',
    {"type": "minecraft:crafting_shapeless", "category": "misc",
     "ingredients": [{"item": "expandeddelight:juicer"}], "result": {"count": 1, "id": "extradelight:juicer"}},
    'old Expanded Delight juicers convert')
# bottling the Extra Delight juicer's tank now takes purified water bottles
for name in ['cactus_juice', 'glow_berry_juice', 'melon_juice', 'sweet_berry_juice']:
    path = 'data/extradelight/recipe/%s_from_bucket.json' % name
    j = jar_json('extradelight-*.jar', path)
    j['ingredients'] = [i if i.get('item') != 'minecraft:glass_bottle' else PURIFIED_ING for i in j['ingredients']]
    put(path, j, 'bottled with purified water')
    alt_paths.add(path)
# fruits the juicer has no fluid for: Extra Delight mixing bowl, purified water bottle as the container
# (pumpkin and chorus juice were Tough As Nails items; chorus juice now only comes from Ender's Delight)
for name, fruit in [('apple_juice', {'item': 'minecraft:apple'}),
                    ('cranberry_juice', {'item': 'expandeddelight:cranberries'})]:
    ns = 'expandeddelight'
    put('data/zz_unified/recipe/mixing/%s.json' % name,
        {"type": "extradelight:mixing_bowl", "container": PURIFIED_STACK, "fluids": [],
         "ingredients": [fruit, {"item": "minecraft:sugar"}], "result": {"count": 1, "id": "%s:%s" % (ns, name)},
         "stirs": 4, "utensil": {"tag": "c:spoons"}},
        'mixing bowl juice')
for e in [e for e in disabled if e['path'] in alt_paths]:
    disabled.remove(e)
    kept.append({'recipe': e['recipe'], 'item': e['item'], 'type': e['type'], 'why': 'alternative pipeline (user request)'})
import extras
extras.run({'INST': INST, 'PACK': PACK, 'OUT': OUT, 'idx': idx, 'put': put, 'jar_json': jar_json,
            'DISABLED_JSON': DISABLED_JSON, 'item_utag': item_utag, 'custom': custom, 'mat': mat, 'tags_cfg': tags_cfg, 'ph': ph,
            'report': report})
report['extra_files'] = extra_files

report.update({'disabled': disabled, 'kept': kept, 'retargeted': sorted(set(retargeted)), 'converted': converted,
               'other_references': refs, 'groups': groups,
               'config': {'removed_unify_tags': REMOVE_UNIFY_TAGS, 'ignored_items_added': IGNORE_ITEMS}})
json.dump(report, open(os.path.join(OUT, 'report.json'), 'w'), indent=1)
print('groups', len(groups), '| disabled', len(disabled), '| kept', len(kept), '| retargeted files', len(set(retargeted)),
      '| converted to crafter', len(converted), '| conflicts', len(report['conflicts']))
for c in report['conflicts']:
    print('CONFLICT', c)
for g in groups:
    if g['tag_extras']:
        print('EXTRAS in', g['tag'], g['tag_extras'])
print('other refs:', {k: v[:3] for k, v in refs.items()})
