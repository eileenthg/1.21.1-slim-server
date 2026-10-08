"""List structure starts stored in a world's overworld region files near a chunk: region_starts.py <world> <cx> <cz> <radius>"""
import io, os, struct, sys, zlib, gzip

def payload(f, t):
    if t == 1: return struct.unpack('>b', f.read(1))[0]
    if t == 2: return struct.unpack('>h', f.read(2))[0]
    if t == 3: return struct.unpack('>i', f.read(4))[0]
    if t == 4: return struct.unpack('>q', f.read(8))[0]
    if t == 5: return struct.unpack('>f', f.read(4))[0]
    if t == 6: return struct.unpack('>d', f.read(8))[0]
    if t == 7: n = struct.unpack('>i', f.read(4))[0]; return f.read(n)
    if t == 8: n = struct.unpack('>H', f.read(2))[0]; return f.read(n).decode('utf-8', 'replace')
    if t == 9:
        et = f.read(1)[0]; n = struct.unpack('>i', f.read(4))[0]; return [payload(f, et) for _ in range(n)]
    if t == 10:
        d = {}
        while True:
            tt = f.read(1)[0]
            if tt == 0: return d
            n = struct.unpack('>H', f.read(2))[0]; name = f.read(n).decode('utf-8', 'replace'); d[name] = payload(f, tt)
    if t == 11: n = struct.unpack('>i', f.read(4))[0]; return list(struct.unpack('>%di' % n, f.read(4 * n)))
    if t == 12: n = struct.unpack('>i', f.read(4))[0]; f.read(8 * n); return n

def chunk(world, cx, cz):
    path = os.path.join(world, 'region', 'r.%d.%d.mca' % (cx >> 5, cz >> 5))
    if not os.path.exists(path): return None
    with open(path, 'rb') as fh:
        fh.seek(4 * ((cx & 31) + (cz & 31) * 32))
        off = int.from_bytes(fh.read(3), 'big'); 
        if off == 0: return None
        fh.seek(off * 4096)
        length, comp = struct.unpack('>iB', fh.read(5))
        raw = fh.read(length - 1)
    data = zlib.decompress(raw) if comp == 2 else gzip.decompress(raw) if comp == 1 else raw
    f = io.BytesIO(data); f.read(3)
    return payload(f, 10)

world, ccx, ccz, r = sys.argv[1], int(sys.argv[2]), int(sys.argv[3]), int(sys.argv[4])
statuses = {}
for cx in range(ccx - r, ccx + r + 1):
    for cz in range(ccz - r, ccz + r + 1):
        c = chunk(world, cx, cz)
        if c is None: continue
        statuses[c.get('Status')] = statuses.get(c.get('Status'), 0) + 1
        for sid, st in c.get('structures', {}).get('starts', {}).items():
            if st.get('id') == 'INVALID': continue
            bb = None
            for p in st.get('Children', [])[:1]: bb = p.get('BB')
            print(f"start ({cx},{cz}) ring={max(abs(cx-ccx),abs(cz-ccz))} {sid} firstPieceBB={bb}")
print('chunk statuses:', statuses)
