import zipfile, os, sys, shutil
HERE = os.path.dirname(os.path.abspath(__file__))
src = os.path.join(HERE, 'out', 'zz-unified-items')
dst = os.path.join(HERE, 'out', 'zz-unified-items.zip')
with zipfile.ZipFile(dst, 'w', zipfile.ZIP_DEFLATED) as z:
    for root, _, fs in os.walk(src):
        for f in fs:
            p = os.path.join(root, f)
            z.write(p, os.path.relpath(p, src).replace(os.sep, '/'))
for target in sys.argv[1:]:
    shutil.copy(dst, target)
    print('copied ->', target)
