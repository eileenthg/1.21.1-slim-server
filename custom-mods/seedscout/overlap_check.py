"""Report overlapping structure starts in a generated world: overlap_check.py <world> <cx> <cz> <radius>"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import region_starts as rs

world, ccx, ccz, r = sys.argv[1], int(sys.argv[2]), int(sys.argv[3]), int(sys.argv[4])
boxes = {}
for cx in range(ccx - r, ccx + r + 1):
    for cz in range(ccz - r, ccz + r + 1):
        c = rs.chunk(world, cx, cz)
        if c is None: continue
        for sid, st in c.get('structures', {}).get('starts', {}).items():
            if st.get('id') == 'INVALID': continue
            for i, p in enumerate(st.get('Children', [])):
                bb = p.get('BB')
                if bb: boxes.setdefault((sid, cx, cz), []).append(bb)

def hull(bbs):
    return [min(b[0] for b in bbs), min(b[1] for b in bbs), min(b[2] for b in bbs),
            max(b[3] for b in bbs), max(b[4] for b in bbs), max(b[5] for b in bbs)]

def inter(a, b):  # 3D piece overlap
    return all(a[i] <= b[i + 3] and b[i] <= a[i + 3] for i in range(3))

keys = list(boxes)
print(f"{len(keys)} structure starts with pieces near ({ccx},{ccz})")
pairs = 0
for i in range(len(keys)):
    for j in range(i + 1, len(keys)):
        a, b = keys[i], keys[j]
        hits = [(pa, pb) for pa in boxes[a] for pb in boxes[b] if inter(pa, pb)]
        if hits:
            pairs += 1
            print(f"OVERLAP {a[0]} (chunk {a[1]},{a[2]}) x {b[0]} (chunk {b[1]},{b[2]}): {len(hits)} piece pairs, e.g. {hits[0][0]} vs {hits[0][1]}")
print('overlapping pairs:', pairs)
for k in keys:
    h = hull(boxes[k])
    print(f"  {k[0]:55s} chunk {k[1]},{k[2]}  x {h[0]}..{h[3]}  z {h[2]}..{h[5]}  pieces {len(boxes[k])}")
