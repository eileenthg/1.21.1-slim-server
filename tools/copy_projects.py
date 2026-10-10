"""Copies the owner's other mod projects (siblings of modpack-audit) into custom-mods/: sources for the small
addons, docs only for large upstream ports. Also copies the numbered session reports into reports/."""
import os, shutil, glob

ROOT = r'C:/Users/eteil.000/Claude'
OUT = os.path.dirname(os.path.dirname(os.path.abspath(__file__))).replace(os.sep, '/')
BS = chr(92)
SOURCE = ['alex-mobs-butchery', 'alex-mobs-butchery-2', 'chesspvp-seated', 'colony-handover', 'drink-fix', 'maid-friendly-fire',
          'mca-anim-compat', 'mca-rpg-bridge', 'pack-probe', 'rain-source-restore', 'seat-rest', 'settled-caves',
          'spawn-onboarding', 'tamable-arcadian-fairy', 'tlm-computer-addon', 'tlm-keyboard-music', 'tlm-ll-voices',
          'tlm-trumpet-addon', 'village-waystone-names']
DOCS_ONLY = ['origins-port', 'dfr-jei-fix', 'tlm', 'maidsoul-fix', 'delightful-port', 'antos-games']
SKIP_DIRS = {'build', '.gradle', 'libs', 'run', 'run-client', 'run-gametest', 'run-server', 'runs', 'out', 'orig', 'decomp',
             '.git', '_research', 'reference', 'node_modules', 'bin', '.idea', 'jei-src', 'blahaj', 'screenshots'}
BIN = ('.jar', '.class', '.zip', '.png', '.ogg', '.nbt', '.gz', '.npy', '.mp3', '.wav', '.sf2', '.psd', '.bbmodel', '.jpg')

def copy_tree(src, dst, docs_only):
    n = 0
    for r, ds, fs in os.walk(src):
        ds[:] = [x for x in ds if x not in SKIP_DIRS and not x.startswith('run')]
        for f in fs:
            q = os.path.join(r, f).replace(BS, '/')
            if f.endswith(BIN) or os.path.getsize(q) > 1_000_000:
                continue
            if docs_only and not f.lower().endswith(('.md', '.txt')):
                continue
            if docs_only and q.count('/') - src.count('/') > 2:
                continue
            target = dst + '/' + os.path.relpath(q, src).replace(BS, '/')
            os.makedirs(os.path.dirname(target), exist_ok=True)
            shutil.copy2(q, target)
            n += 1
    return n

for d in SOURCE + DOCS_ONLY:
    src = ROOT + '/' + d
    dst = OUT + '/custom-mods/' + d
    if os.path.isdir(dst):
        shutil.rmtree(dst)
    if os.path.isdir(src):
        print(d, copy_tree(src, dst, d in DOCS_ONLY))

rep = OUT + '/reports'
if os.path.isdir(rep):
    shutil.rmtree(rep)
os.makedirs(rep)
for f in sorted(glob.glob(ROOT + '/modpack-audit/[0-9][0-9]-*.md')):
    shutil.copy2(f, rep + '/' + os.path.basename(f))
for f in glob.glob(ROOT + '/modpack-audit/*/README.md') + glob.glob(ROOT + '/modpack-audit/*/audit-report.md'):
    p = f.replace(BS, '/')
    proj = p.split('/')[-2]
    if not os.path.isdir(OUT + '/custom-mods/' + proj):
        os.makedirs(rep + '/project-docs', exist_ok=True)
        shutil.copy2(p, rep + '/project-docs/' + proj + '--' + os.path.basename(p))
print('reports', len(os.listdir(rep)))
