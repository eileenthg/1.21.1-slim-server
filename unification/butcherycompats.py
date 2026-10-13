import zipfile, json, re, os, shutil
SRC = r"C:\Users\eteil.000\Claude\modpack-audit\Resource packs and data packs list\datapacks\ButcheryCompats.zip"
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'out', 'ButcheryCompats-spectrum.zip')
DROP = {'data/butchery/loot_table/blocks/glowsquid_cut_1_drop.json',
        'data/butchery/loot_table/blocks/glowsquid_cut_2_drop.json'}   # squid-calamari-dedup owns these
src = zipfile.ZipFile(SRC)
changed = []
with zipfile.ZipFile(OUT, 'w', zipfile.ZIP_DEFLATED) as z:
    for n in src.namelist():
        if n.endswith('/') or n in DROP:
            continue
        data = src.read(n)
        if n.endswith('.json'):
            s = data.decode('utf-8')
            if 'pastel:' in s:
                s = s.replace('pastel:', 'spectrum:')
                changed.append(n)
            data = s.encode('utf-8')
        z.writestr(n, data)
print('retargeted %d files, dropped %d' % (len(changed), len(DROP)))
for c in changed: print('  ', c)
