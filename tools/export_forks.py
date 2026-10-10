"""Exports the owner's changes to forked upstream mods as patch files (commits ahead of upstream plus any
uncommitted work), copies the Origins port sources and the other projects' Claude notes into the repo."""
import os, subprocess, shutil, glob

ROOT = r'C:/Users/eteil.000/Claude'
OUT = os.path.dirname(os.path.dirname(os.path.abspath(__file__))).replace(os.sep, '/')
BS = chr(92)
FORKS = {  # repo path -> name in custom-mods/
    'ED-cheesemaking-migrate/BrewinAndChewin': 'ED-cheesemaking-migrate/brewinandchewin',
    'ED-cheesemaking-migrate/ExpandedDelight': 'ED-cheesemaking-migrate/expandeddelight',
    'delightful-port': 'delightful-port',
    'tlm': 'tlm',
    'antos-games': 'antos-games',
    'maidsoul-fix/MaidsoulKitchen': 'maidsoul-fix/maidsoulkitchen',
    'dfr-jei-fix/fork': 'dfr-jei-fix/fork',
    'origins-port/build-deps/og-apoli': 'origins-port/og-apoli',
    'origins-port/build-deps/og-origins': 'origins-port/og-origins',
}

def git(repo, *args):
    r = subprocess.run(['git', '-C', repo, *args], capture_output=True, text=True, encoding='utf-8', errors='replace')
    return r.stdout

for src, name in FORKS.items():
    repo = ROOT + '/' + src
    if not os.path.isdir(repo + '/.git'):
        print('skip (no git)', src)
        continue
    dst = OUT + '/custom-mods/' + name + '/patches'
    if os.path.isdir(dst):
        shutil.rmtree(dst)
    os.makedirs(dst)
    base = ''
    for ref in ('origin/HEAD', 'origin/main', 'origin/master', 'origin/1.21.1', 'origin/1.21'):
        b = git(repo, 'merge-base', 'HEAD', ref).strip()
        if b:
            base = b
            break
    branch = git(repo, 'rev-parse', '--abbrev-ref', 'HEAD').strip()
    remote = git(repo, 'remote', 'get-url', 'origin').strip()
    log = git(repo, 'log', '--format=%h %ad %s', '--date=short', (base + '..HEAD') if base else '-15')
    if base:
        git(repo, 'format-patch', '-q', '-o', dst, base + '..HEAD')
    diff = git(repo, 'diff', 'HEAD')
    if diff.strip():
        open(dst + '/uncommitted.diff', 'w', encoding='utf-8').write(diff)
    status = git(repo, 'status', '--short')
    with open(dst + '/README.md', 'w', encoding='utf-8') as w:
        w.write(f'# Changes to {src}\n\nUpstream: {remote}\nBranch: {branch}\nBase (merge-base with upstream): {base or "not found"}\n\n')
        w.write('## Commits ahead of upstream\n\n```\n' + (log or '(none)') + '```\n\n')
        w.write('## Uncommitted files\n\n```\n' + (status or '(none)') + '```\n')
    print(src, 'patches:', len(glob.glob(dst + '/*.patch')), 'uncommitted:', bool(diff.strip()))

# Origins port: our own mod source (touhou-origins-neo) and the Player Scale port
for sub in ('touhou-origins-neo', 'player-scale'):
    s = ROOT + '/origins-port/' + sub
    d = OUT + '/custom-mods/origins-port/' + sub
    if os.path.isdir(d):
        shutil.rmtree(d)
    n = 0
    for r, ds, fs in os.walk(s):
        ds[:] = [x for x in ds if x not in {'build', '.gradle', 'run', 'runs', '.git', 'libs', 'out'}]
        for f in fs:
            q = os.path.join(r, f).replace(BS, '/')
            if f.endswith(('.jar', '.class', '.zip', '.ogg', '.psd')) or os.path.getsize(q) > 1_000_000:
                continue
            t = d + '/' + os.path.relpath(q, s).replace(BS, '/')
            os.makedirs(os.path.dirname(t), exist_ok=True)
            shutil.copy2(q, t)
            n += 1
    print('origins-port/' + sub, n)

# Claude notes kept by the other projects
MEM = r'C:/Users/eteil.000/.claude/projects'
dst = OUT + '/notes-other'
if os.path.isdir(dst):
    shutil.rmtree(dst)
for d in sorted(os.listdir(MEM)):
    m = MEM + '/' + d + '/memory'
    if d.endswith('modpack-audit') or not os.path.isdir(m):
        continue
    name = d.replace('C--Users-eteil-000-Claude-', '')
    shutil.copytree(m, dst + '/' + name)
    print('notes-other/' + name, len(os.listdir(m)))
