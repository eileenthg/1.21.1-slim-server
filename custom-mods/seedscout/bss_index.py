"""Print Better Sparse Structures' remembered starts nearest a point: bss_index.py <world> <x> <z> [n]"""
import gzip, struct, io, math, sys

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
            n = struct.unpack('>H', f.read(2))[0]; name = f.read(n).decode(); d[name] = payload(f, tt)
    if t == 11: n = struct.unpack('>i', f.read(4))[0]; return list(struct.unpack('>%di' % n, f.read(4 * n)))
    if t == 12: n = struct.unpack('>i', f.read(4))[0]; return list(struct.unpack('>%dq' % n, f.read(8 * n)))

world, x, z = sys.argv[1], int(sys.argv[2]), int(sys.argv[3])
count = int(sys.argv[4]) if len(sys.argv) > 4 else 15
f = io.BytesIO(gzip.open(world + '/data/bettersparsestructures_global_structure_index.dat').read())
f.read(3)
data = payload(f, 10)['data']
rows = []
for e in data['accepted_structures']:
    keys = {k.lower(): v for k, v in e.items()}
    mx = (keys.get('min_x', 0) + keys.get('max_x', 0)) // 2
    mz = (keys.get('min_z', 0) + keys.get('max_z', 0)) // 2
    rows.append((math.hypot(mx - x, mz - z), e))
rows.sort(key=lambda r: r[0])
print(len(rows), 'accepted structures; keys:', list(rows[0][1].keys()) if rows else '-')
for d, e in rows[:count]:
    print(f"{d:7.0f}  {e['structure_id']:45s} x {e['min_x']}..{e['max_x']}  y {e['min_y']}..{e['max_y']}  z {e['min_z']}..{e['max_z']}")
print('first-occurrence failures:', [(x['structure_id'], x['failure_count']) for x in data.get('first_occurrence_failures', [])])
