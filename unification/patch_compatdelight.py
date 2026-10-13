"""Makes Compat Delight register compat modules for mods that are not installed, keyed to installed mods whose
items stand in for them (the recipes get the stand-ins in extras.py).
ModCompat/ModCompatA key each module by the mod id it waits for; this rewrites those key strings. Only
constant-pool UTF8 entries change; class files address constants by index, so a different length is safe.
Usage: patch_compatdelight.py <original jar> <patched jar>"""
import zipfile, sys, os, struct

REKEY = {
    'net/fixerlink/compatdelight/register/ModCompat.class': {
        'ecologics': 'crabbersdelight',          # coconut, crab
        'naturalist': 'aquaculture',             # bass (smallmouth bass); lizard recipes are off
        'hexerei': 'youkaishomecoming',          # mandrake
        'nourished_nether': 'eternal_tales',     # ectoplasm
    },
    'net/fixerlink/compatdelight/register/ModCompatA.class': {
        'tropicraft': 'pineapple_delight',       # pineapple
        'rats': 'brewinandchewin',               # cheese
    },
}

def utf8(s):
    b = s.encode()
    return b'\x01' + struct.pack('>H', len(b)) + b

src, dst = sys.argv[1], sys.argv[2]
zin = zipfile.ZipFile(src)
patched = {}
for cls, keys in REKEY.items():
    data = zin.read(cls)
    for old, new in keys.items():
        assert data.count(utf8(old)) == 1, (cls, old, data.count(utf8(old)))
        assert utf8(new) not in data, (cls, new, 'already a key')
        data = data.replace(utf8(old), utf8(new))
    patched[cls] = data
tmp = dst + '.tmp'
with zipfile.ZipFile(tmp, 'w', zipfile.ZIP_DEFLATED) as zout:
    for info in zin.infolist():
        zout.writestr(info, patched.get(info.filename, zin.read(info.filename)))
os.replace(tmp, dst)
for cls, keys in REKEY.items():
    print(cls.split('/')[-1], keys)
