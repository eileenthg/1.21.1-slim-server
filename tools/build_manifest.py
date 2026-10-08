"""Builds the mod reference data for the slim-server repo from the live Prism instance (read-only)."""
import zipfile, os, json, hashlib, re, tomllib, sys
INST = r'C:/Users/eteil.000/AppData/Roaming/PrismLauncher/instances/1.21.1 slim/minecraft'
OUT = os.path.dirname(os.path.abspath(__file__))
os.makedirs(f'{OUT}/mods', exist_ok=True)
rows, names = [], []
PFX = ('item.', 'block.', 'entity.', 'effect.', 'biome.', 'enchantment.', 'tag.', 'itemGroup.', 'death.attack.')
def meta_from_toml(z):
    for p in ('META-INF/neoforge.mods.toml', 'META-INF/mods.toml'):
        if p in z.namelist():
            t = tomllib.loads(z.read(p).decode('utf-8-sig'))
            out = []
            for m in t.get('mods', []):
                mid = m.get('modId')
                deps = [{'id': d.get('modId'), 'type': d.get('type', 'required'), 'range': d.get('versionRange', ''), 'side': d.get('side', 'BOTH')}
                        for d in t.get('dependencies', {}).get(mid, [])]
                out.append({'modId': mid, 'name': m.get('displayName'), 'version': m.get('version'), 'authors': m.get('authors'),
                            'description': (m.get('description') or '').strip(), 'url': m.get('displayURL'),
                            'license': t.get('license'), 'deps': deps})
            return 'neoforge', out
    return None, []
def meta_from_fabric(z):
    if 'fabric.mod.json' in z.namelist():
        j = json.loads(z.read('fabric.mod.json').decode('utf-8-sig'))
        a = j.get('authors', [])
        a = ', '.join(x if isinstance(x, str) else x.get('name', '') for x in a)
        deps = [{'id': k, 'type': 'required', 'range': v if isinstance(v, str) else json.dumps(v), 'side': j.get('environment', '*')}
                for k, v in (j.get('depends') or {}).items()]
        return 'fabric(Connector)', [{'modId': j.get('id'), 'name': j.get('name'), 'version': j.get('version'), 'authors': a,
                'description': (j.get('description') or '').strip(), 'url': (j.get('contact') or {}).get('homepage'),
                'license': j.get('license') if isinstance(j.get('license'), str) else json.dumps(j.get('license')), 'deps': deps}]
    return None, []
for f in sorted(os.listdir(f'{INST}/mods'), key=str.lower):
    if not f.endswith('.jar'): continue
    p = f'{INST}/mods/{f}'
    sha1 = hashlib.sha1(open(p, 'rb').read()).hexdigest()
    row = {'file': f, 'sizeBytes': os.path.getsize(p), 'sha1': sha1, 'loader': '?', 'mods': []}
    try:
        z = zipfile.ZipFile(p)
        ld, mods = meta_from_toml(z)
        if not mods: ld, mods = meta_from_fabric(z)
        if mods and ld == 'neoforge' and 'fabric.mod.json' in z.namelist(): ld = 'neoforge+fabric'
        row['loader'] = ld or 'library/none'
        row['mods'] = mods
        # item/block/entity display names from every en_us.json in the jar
        for n in z.namelist():
            m = re.match(r'assets/([^/]+)/lang/en_us\.json$', n)
            if not m: continue
            try: d = json.loads(z.read(n).decode('utf-8-sig'))
            except Exception: continue
            for k, v in d.items():
                if k.startswith(PFX) and isinstance(v, str): names.append((m.group(1), k, v.replace('\t', ' ').replace('\n', ' ')))
    except Exception as e:
        row['error'] = str(e)
    rows.append(row)
json.dump(rows, open(f'{OUT}/mods/manifest.json', 'w', encoding='utf-8'), indent=1, ensure_ascii=False)
with open(f'{OUT}/mods/MODS.md', 'w', encoding='utf-8') as w:
    w.write('# Installed mods (generated from the live instance)\n\n| jar | modId | name | version | loader | description |\n|---|---|---|---|---|---|\n')
    for r in rows:
        if not r['mods']: w.write(f"| {r['file']} | - | - | - | {r['loader']} | |\n"); continue
        for m in r['mods']:
            d = re.sub(r'\s+', ' ', (m['description'] or ''))[:160].replace('|', '/')
            w.write(f"| {r['file']} | {m['modId']} | {(m['name'] or '').replace('|','/')} | {m['version']} | {r['loader']} | {d} |\n")
seen = set()
with open(f'{OUT}/mods/registry-names.tsv', 'w', encoding='utf-8') as w:
    w.write('namespace\tlang_key\tdisplay_name\n')
    for ns, k, v in sorted(names):
        if (ns, k) in seen: continue
        seen.add((ns, k)); w.write(f'{ns}\t{k}\t{v}\n')
print('jars', len(rows), 'mod entries', sum(len(r['mods']) for r in rows), 'name rows', len(seen))
