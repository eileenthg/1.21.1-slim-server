import os, shutil, re, hashlib, json
INST = r'C:/Users/eteil.000/AppData/Roaming/PrismLauncher/instances/1.21.1 slim/minecraft'
PRISM = r'C:/Users/eteil.000/AppData/Roaming/PrismLauncher/instances/1.21.1 slim'
AUDIT = r'C:/Users/eteil.000/Claude/modpack-audit'
MEM = r'C:/Users/eteil.000/.claude/projects/C--Users-eteil-000-Claude-modpack-audit/memory'
OUT = os.path.dirname(os.path.abspath(__file__)).replace(os.sep, '/')
BS = chr(92)

def fwd(p):
    return p.replace(BS, '/')

def clean(d):
    if os.path.isdir(d):
        shutil.rmtree(d)

EXC_CFG = re.compile(r'(\.bak|\.backup\d*|\.log|\.dat|\.tmp|\.disabled)$|\.bak[-.]|\.BEFORE-|/QuantifiedAPI/|/spark/|/crash_assistant/|/chunky/|/fast_travel_waypoints/|/mca/presets/|\.rpo$|/\.', re.I)

n = 0
clean(OUT + '/config')
for r, ds, fs in os.walk(INST + '/config'):
    for f in fs:
        p = fwd(os.path.join(r, f))
        rel = p[len(INST):]
        if EXC_CFG.search(rel) or os.path.getsize(p) == 0:
            continue
        if os.path.getsize(p) > 2_000_000:
            print('skip large', rel)
            continue
        dst = OUT + rel
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        shutil.copy2(p, dst)
        n += 1
print('config files', n)

clean(OUT + '/defaultconfigs')
if os.path.isdir(INST + '/defaultconfigs'):
    shutil.copytree(INST + '/defaultconfigs', OUT + '/defaultconfigs')

clean(OUT + '/datapacks')
os.makedirs(OUT + '/datapacks')
for f in sorted(os.listdir(INST + '/datapacks')):
    if re.search(r'\.bak', f):
        continue
    p = INST + '/datapacks/' + f
    if os.path.isfile(p):
        shutil.copy2(p, OUT + '/datapacks/' + f)

rp = []
for f in sorted(os.listdir(INST + '/resourcepacks')):
    p = INST + '/resourcepacks/' + f
    if os.path.isfile(p) and not f.endswith('.rpo'):
        rp.append({'file': f, 'sizeBytes': os.path.getsize(p), 'sha1': hashlib.sha1(open(p, 'rb').read()).hexdigest()})
json.dump(rp, open(OUT + '/resourcepacks.json', 'w'), indent=1)
shutil.copy2(PRISM + '/mmc-pack.json', OUT + '/mmc-pack.json')

clean(OUT + '/notes')
shutil.copytree(MEM, OUT + '/notes')

clean(OUT + '/custom-mods')
os.makedirs(OUT + '/custom-mods')
SKIP = {'build', '.gradle', 'libs', 'run', 'release', 'out', 'orig', 'decomp', '.git'}
BIN = ('.jar', '.class', '.zip', '.png', '.ogg', '.nbt', '.gz', '.npy')
count = 0
for d in sorted(os.listdir(AUDIT)):
    p = AUDIT + '/' + d
    if not os.path.isfile(p + '/build.gradle'):
        continue
    for r, ds, fs in os.walk(p):
        ds[:] = [x for x in ds if x not in SKIP]
        for f in fs:
            q = fwd(os.path.join(r, f))
            if f.endswith(BIN) or os.path.getsize(q) > 1_000_000:
                continue
            dst = OUT + '/custom-mods/' + d + '/' + os.path.relpath(q, p).replace(BS, '/')
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            shutil.copy2(q, dst)
            count += 1
print('custom-mod files', count)
