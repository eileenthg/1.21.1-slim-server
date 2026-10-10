"""Reads an Alex's Mobs model class (decompiled Java) into a part tree.

Alex's Mobs builds its entity models in code with Citadel's AdvancedModelBox. The
constructors are machine-generated (Tabula/Blockbench export style), so a handful of
statement shapes cover every model this addon converts. Anything else in a constructor
is reported, never silently skipped.

Geometry follows Citadel exactly:
  * a part's transform is translate(pivot) . Rz . Ry . Rx   (AdvancedModelBox.translateAndRotate)
  * a box's quads and per-vertex UVs match TabulaModelRenderUtils.ModelBox / TexturedQuad
Units are model pixels (1/16 block); model space is y-down, as in every entity model.
"""

from __future__ import annotations

import math
import re
from dataclasses import dataclass, field

import numpy as np

F = r"(-?\d+(?:\.\d+)?(?:[eE]-?\d+)?)F?"


@dataclass
class Box:
    u: int
    v: int
    x: float
    y: float
    z: float
    w: float
    h: float
    d: float
    inflate: float
    mirror: bool


@dataclass
class Part:
    name: str
    pivot: tuple = (0.0, 0.0, 0.0)
    rot: tuple = (0.0, 0.0, 0.0)
    boxes: list = field(default_factory=list)
    children: list = field(default_factory=list)
    parent: "Part | None" = None


@dataclass
class Model:
    tex_w: int
    tex_h: int
    parts: dict
    roots: list

    def subtree(self, name: str) -> list[str]:
        out, stack = [], [self.parts[name]]
        while stack:
            p = stack.pop()
            out.append(p.name)
            stack.extend(p.children)
        return out


def _ctor_body(src: str, cls: str) -> str:
    m = re.search(r"public " + cls + r"\(([^)]*)\)\s*\{", src)
    if not m:
        raise ValueError(f"no constructor for {cls}")
    i, depth = m.end(), 1
    while depth:
        c = src[i]
        depth += c == "{"
        depth -= c == "}"
        i += 1
    return src[m.end(): i - 1]


def _select_case(body: str, case: "int | str | None") -> str:
    """Flattens `switch (type) { case N: ... break; }` to the chosen case's statements."""
    m = re.search(r"switch\s*\(\s*\w+\s*\)\s*\{", body)
    if not m:
        return body
    i, depth = m.end(), 1
    while depth:
        c = body[i]
        depth += c == "{"
        depth -= c == "}"
        i += 1
    inner = body[m.end(): i - 1]
    chunks = re.split(r"\bcase (\w+):", inner)
    chosen = ""
    for k in range(1, len(chunks), 2):
        if case is not None and chunks[k] == str(case):
            chosen = chunks[k + 1]
    return body[: m.start()] + chosen + body[i:]


STMTS = [
    ("tex", re.compile(r"this\.tex(Width|Height) = (\d+)")),
    ("type", re.compile(r"this\.type = type")),
    ("unused", re.compile(r"(?:this\.)?\w+ = \w+")),  # plain field copies, e.g. this.index = index
    ("new", re.compile(r"this\.(\w+) = new AdvancedModelBox\(this(?:, \"[^\"]*\")?\)")),
    ("pivot", re.compile(r"this\.(\w+)\.(?:setRotationPoint|setPos)\(" + F + r", " + F + r", " + F + r"\)")),
    ("child", re.compile(r"this\.(\w+)\.addChild\(this\.(\w+)\)")),
    ("angle", re.compile(r"this\.(?:setRotationAngle|setRotateAngle)\(this\.(\w+), " + F + r", " + F + r", " + F + r"\)")),
    ("box", re.compile(
        r"this\.(\w+)\.setTextureOffset\((\d+), (\d+)\)\.addBox\("
        + F + ", " + F + ", " + F + ", " + F + ", " + F + ", " + F
        + r"(?:, (?:" + F + r"|[a-z][\w .+-]*?))?(?:, (true|false))?\)")),  # inflate may be an expression
    ("axis", re.compile(r"this\.(\w+)\.rotateAngle([XYZ]) = " + F)),
    ("skip", re.compile(r"this\.updateDefaultPose\(\)|this\.animator = (?:ModelAnimator\.create\(\)|new ModelAnimator\(\))|break")),
]


def parse(path: str, cls: str, case: int | None = None) -> Model:
    src = open(path, encoding="utf-8").read()
    body = _select_case(_ctor_body(src, cls), case)
    # Maths.rad(deg) -> radians, as Alex's Mobs' Maths helper does
    body = re.sub(r"Maths\.rad\((-?\d+(?:\.\d+)?)\)", lambda m: f"{math.radians(float(m.group(1)))}F", body)
    tex = {}
    parts: dict[str, Part] = {}
    unknown = []
    for raw in body.split(";"):
        s = " ".join(raw.split())
        if not s or s in ("}", "{"):
            continue
        s = s.strip("{} ")
        if not s:
            continue
        for kind, rx in STMTS:
            m = rx.fullmatch(s)
            if m:
                break
        else:
            unknown.append(s)
            continue
        g = m.groups()
        if kind == "tex":
            tex[g[0]] = int(g[1])
        elif kind == "new":
            parts[g[0]] = Part(g[0])
        elif kind == "pivot":
            parts[g[0]].pivot = tuple(float(x) for x in g[1:4])
        elif kind == "child":
            parts[g[0]].children.append(parts[g[1]])
            parts[g[1]].parent = parts[g[0]]
        elif kind == "angle":
            parts[g[0]].rot = tuple(float(x) for x in g[1:4])
        elif kind == "axis":
            r = list(parts[g[0]].rot)
            r["XYZ".index(g[1])] = float(g[2])
            parts[g[0]].rot = tuple(r)
        elif kind == "box":
            name, u, v = g[0], int(g[1]), int(g[2])
            x, y, z, w, h, d = (float(t) for t in g[3:9])
            inflate = float(g[9]) if g[9] else 0.0
            mirror = g[10] == "true"
            parts[name].boxes.append(Box(u, v, x, y, z, w, h, d, inflate, mirror))
    if unknown:
        raise ValueError(f"{cls}: unhandled constructor statements: {unknown}")
    roots = [p for p in parts.values() if p.parent is None and (p.boxes or p.children)]
    return Model(tex["Width"], tex["Height"], parts, roots)


# ---------------------------------------------------------------- geometry

def rot_x(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


def rot_y(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


def rot_z(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def affine(r=np.eye(3), t=(0, 0, 0)):
    m = np.eye(4)
    m[:3, :3] = r
    m[:3, 3] = t
    return m


def part_local(p: Part, zero_rot=False) -> np.ndarray:
    """Citadel's translateAndRotate: translate(pivot) then Z, Y, X rotations."""
    r = np.eye(3) if zero_rot else rot_z(p.rot[2]) @ rot_y(p.rot[1]) @ rot_x(p.rot[0])
    return affine(r, p.pivot)


def box_quads(b: Box, tw: int, th: int):
    """The six quads of a Citadel ModelBox as [(positions[4], uvs[4], name)], in box-local pixels.

    UVs are texture pixels. Mirrored boxes swap min/max x before the vertices are built and
    reverse each quad's vertex order afterwards; the reversal does not change which UV sits on
    which position, so only the swap matters here.
    """
    x1, y1, z1 = b.x - b.inflate, b.y - b.inflate, b.z - b.inflate
    x2, y2, z2 = b.x + b.w + b.inflate, b.y + b.h + b.inflate, b.z + b.d + b.inflate
    if b.mirror:
        x1, x2 = x2, x1
    v18 = (x1, y1, z1); v19 = (x2, y1, z1); v20 = (x2, y2, z1); v21 = (x1, y2, z1)
    v22 = (x1, y1, z2); v23 = (x2, y1, z2); v24 = (x2, y2, z2); v25 = (x1, y2, z2)
    U, V, w, h, d = b.u, b.v, b.w, b.h, b.d
    u26, u27, u28, u29 = U, U + d, U + d + w, U + d + w + w
    u30, u31 = U + d + w + d, U + d + w + d + w
    v32, v33, v34 = V, V + d, V + d + h

    def quad(verts, ua, va, ub, vb, name):
        # TexturedQuad: [0]->(ub,va) [1]->(ua,va) [2]->(ua,vb) [3]->(ub,vb)
        uvs = [(ub, va), (ua, va), (ua, vb), (ub, vb)]
        return (np.array(verts, float), uvs, name)

    return [
        quad([v23, v22, v18, v19], u27, v32, u28, v33, "down"),
        quad([v20, v21, v25, v24], u28, v33, u29, v32, "up"),
        quad([v18, v22, v25, v21], u26, v33, u27, v34, "west"),
        quad([v19, v18, v21, v20], u27, v33, u28, v34, "north"),
        quad([v23, v19, v20, v24], u28, v33, u30, v34, "east"),
        quad([v22, v23, v24, v25], u30, v33, u31, v34, "south"),
    ]


def part_matrix(model: Model, name: str) -> np.ndarray:
    """Model-space transform of a part; its origin is the part's pivot."""
    chain, p = [], model.parts[name]
    while p is not None:
        chain.append(p)
        p = p.parent
    m = np.eye(4)
    for q in reversed(chain):
        m = m @ part_local(q)
    return m


def walk(model: Model, include: set | None = None, start: str | None = None):
    """Yields (part, box, world_matrix) for every box, world = model space.

    With `start`, only that part's subtree is walked, rooted at identity with the start part's
    own rotation removed (used for upright head blocks)."""
    def rec(p: Part, parent_m, first=False):
        m = parent_m @ part_local(p, zero_rot=first)
        if first:
            m = affine(m[:3, :3])  # drop the pivot offset too; heads are re-centred later
        if include is None or p.name in include:
            for b in p.boxes:
                yield p, b, m
        for c in p.children:
            yield from rec(c, m)

    if start:
        yield from rec(model.parts[start], np.eye(4), first=True)
    else:
        for r in model.roots:
            yield from rec(r, np.eye(4))
