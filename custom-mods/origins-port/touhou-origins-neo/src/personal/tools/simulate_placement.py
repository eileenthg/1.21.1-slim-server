"""Reproduce BedrockModelParser + ModelPart.translateAndRotate so a rotated bundle can be
placed by measurement instead of guesswork."""
import json,math,sys

def R(rx,ry,rz):
    rx,ry,rz=map(math.radians,(rx,ry,rz))
    cx,sx=math.cos(rx),math.sin(rx); cy,sy=math.cos(ry),math.sin(ry); cz,sz=math.cos(rz),math.sin(rz)
    Rx=[[1,0,0],[0,cx,-sx],[0,sx,cx]]
    Ry=[[cy,0,sy],[0,1,0],[-sy,0,cy]]
    Rz=[[cz,-sz,0],[sz,cz,0],[0,0,1]]
    def mm(A,B): return [[sum(A[i][k]*B[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
    return mm(mm(Rz,Ry),Rx)                      # ModelPart uses rotationZYX(z,y,x)

def mul(M,v): return [sum(M[i][k]*v[k] for k in range(3)) for i in range(3)]
def add(a,b): return [a[i]+b[i] for i in range(3)]

def walk(bones, name, parent_pivot, M, origin, out, kids, by):
    b=by[name]
    piv=b['pivot']
    # parser: part.setPos(pivot.x-parent.x, parent.y-pivot.y, pivot.z-parent.z)
    local=[piv[0]-parent_pivot[0], parent_pivot[1]-piv[1], piv[2]-parent_pivot[2]]
    pos=add(origin, mul(M,local))
    rot=b.get('rotation') or [0,0,0]
    M2=[[sum(M[i][k]*R(*rot)[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
    for c in b.get('cubes',[]):
        o,s=c['origin'],c['size']
        # parser bake(): local cube corner relative to the bone pivot, Y inverted
        lo=[o[0]-piv[0], piv[1]-o[1]-s[1], o[2]-piv[2]]
        for dx in (0,s[0]):
            for dy in (0,s[1]):
                for dz in (0,s[2]):
                    out.append(add(pos, mul(M2,[lo[0]+dx, lo[1]+dy, lo[2]+dz])))
    for c in kids.get(name,[]):
        walk(bones,c,piv,M2,pos,out,kids,by)

def bundle(path, root='eye', exclude_root_cubes=True):
    g=json.load(open(path,encoding='utf-8'))['minecraft:geometry'][0]
    bones=g['bones']; by={b['name']:b for b in bones}
    kids={}
    for b in bones: kids.setdefault(b.get('parent'),[]).append(b['name'])
    tracker=bones[0]['name']
    pts=[]
    I=[[1,0,0],[0,1,0],[0,0,1]]
    # tracker sits at the origin of model space
    walk(bones, root, by[tracker]['pivot'], I, [0,0,0], pts, kids, by)
    if exclude_root_cubes:
        eye_only=[]; walk(bones, root, by[tracker]['pivot'], I, [0,0,0], eye_only, kids, {root:by[root], **{k:v for k,v in by.items()}})
    xs=[p[0] for p in pts]; ys=[p[1] for p in pts]; zs=[p[2] for p in pts]
    return (min(xs),max(xs)),(min(ys),max(ys)),(min(zs),max(zs))

if __name__=='__main__':
    for p in sys.argv[1:]:
        (x0,x1),(y0,y1),(z0,z1)=bundle(p)
        print(f"{p.split('/')[-1]}")
        print(f"   model space  x {x0:7.2f}..{x1:7.2f}   y {y0:7.2f}..{y1:7.2f}   z {z0:7.2f}..{z1:7.2f}")
        print(f"   centre       ({(x0+x1)/2:.2f}, {(y0+y1)/2:.2f}, {(z0+z1)/2:.2f})")
