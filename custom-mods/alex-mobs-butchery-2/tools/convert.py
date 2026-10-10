"""Turns Alex's Mobs entity models into Butchery-style carcass block models.

For each posed box of the source model this finds the block-model element that best matches
it. A block element is an axis-aligned box plus at most one rotation (x, y or z; -45..45 in
22.5 steps), so every box's orientation is factored as  R = R_theta . P  where P is one of the
24 right-angle rotations (exact: it only permutes and flips the box's axes) and R_theta is the
nearest legal element rotation. Whatever does not fit a single snapped rotation is the
conversion error, reported per model.

Texture coordinates are carried over per face: each source quad's corners keep their UVs, and
the face's `uv` + `rotation` are solved so Minecraft's block face mapping reproduces them.
"""

from __future__ import annotations

import itertools
import math
from dataclasses import dataclass

import numpy as np
from PIL import Image

import ammodel as am

ANGLES = (-45.0, -22.5, 0.0, 22.5, 45.0)
AXES = {"x": am.rot_x, "y": am.rot_y, "z": am.rot_z}

# Minecraft's canonical face axes: (normal, u direction, v direction), from the default UVs.
FACE_AXES = {
    "north": ((0, 0, -1), (-1, 0, 0), (0, -1, 0)),
    "south": ((0, 0, 1), (1, 0, 0), (0, -1, 0)),
    "east": ((1, 0, 0), (0, 0, -1), (0, -1, 0)),
    "west": ((-1, 0, 0), (0, 0, 1), (0, -1, 0)),
    "up": ((0, 1, 0), (1, 0, 0), (0, 0, 1)),
    "down": ((0, -1, 0), (1, 0, 0), (0, 0, -1)),
}


def _right_angle_rotations():
    out = []
    for perm in itertools.permutations(range(3)):
        for signs in itertools.product((1, -1), repeat=3):
            m = np.zeros((3, 3))
            for r, (c, s) in enumerate(zip(perm, signs)):
                m[r, c] = s
            if round(np.linalg.det(m)) == 1:
                out.append(m)
    return out


RIGHT = _right_angle_rotations()


def snap(r: np.ndarray):
    """Factor rotation r as R_theta . P; returns (axis, angle_deg, P, residual)."""
    best = None
    for axis, fn in AXES.items():
        for ang in ANGLES:
            if ang == 0 and axis != "y":
                continue
            rt = fn(math.radians(ang))
            q = rt.T @ r
            for p in RIGHT:
                err = np.linalg.norm(q - p)
                if best is None or err < best[3] - 1e-9:
                    best = (axis, ang, p, err)
    return best


# ---------------------------------------------------------------- poses

ENTITY_FLIP = np.diag([-1.0, -1.0, 1.0])  # LivingEntityRenderer scale(-1,-1,1)
ENTITY_DROP = np.array([0.0, -24.016, 0.0])  # translate(0,-1.501,0) in pixels

# Entity space E (y up, head towards -z) -> block space B.
POSE_ROT = {
    # lying on its side across the block, head west, legs towards the viewer (north)
    "ground": np.array([[0, 0, 1], [1, 0, 0], [0, 1, 0]], float),
    # hung head-up from the hook, belly towards the viewer
    "hanging": np.array([[1, 0, 0], [0, 0, -1], [0, 1, 0]], float),
    # upright, facing the viewer
    "upright": np.eye(3),
}


@dataclass
class Pose:
    rot: np.ndarray
    scale: float
    offset: np.ndarray

    def point(self, p_e):
        return self.scale * (self.rot @ p_e) + self.offset


def entity_point(m: np.ndarray, p_local: np.ndarray, render_scale: float) -> np.ndarray:
    p_model = (m @ np.append(p_local, 1.0))[:3]
    return render_scale * (ENTITY_FLIP @ (p_model + ENTITY_DROP))


def visible(boxes, tw, th, alpha):
    """Boxes with at least one face whose texture region is not fully transparent."""
    out = []
    for part, b, m in boxes:
        if any(not _region_transparent(alpha, uvs) for _, uvs, _ in am.box_quads(b, tw, th)):
            out.append((part, b, m))
    return out


def all_points(boxes, render_scale):
    pts = []
    for _, b, m in boxes:
        for verts, _, _ in am.box_quads(b, 1, 1):
            for v in verts:
                pts.append(entity_point(m, v, render_scale))
    return np.array(pts)


def fit_pose(kind: str, pts_e: np.ndarray, limits: dict, extra_scale: float = 1.0) -> Pose:
    """kind picks the rotation (ground, hanging, upright, head); limits pick size and anchoring."""
    rot = POSE_ROT["upright" if kind == "head" else kind]
    p = pts_e @ rot.T
    lo, hi = p.min(0), p.max(0)
    ext = hi - lo
    k = min(1.0, *(limits["size"][i] / ext[i] for i in range(3) if ext[i] > 1e-6)) * extra_scale
    lo, hi = lo * k, hi * k
    off = np.zeros(3)
    off[0] = 8 - (lo[0] + hi[0]) / 2
    off[2] = 8 - (lo[2] + hi[2]) / 2
    off[1] = limits["top"] - hi[1] if "top" in limits else limits.get("floor", 0.0) - lo[1]
    return Pose(rot, k, off)


HOOK_Y = 16.0  # the hook point: top centre of the block, where Butchery's carcasses hang from


def fit_hanging(rot_kind: str, pts_e: np.ndarray, anchor_e: np.ndarray | None) -> Pose:
    """Hangs a model from one point (its shoulders) at the hook: the head rises above it,
    the body hangs below. Without an anchor the top of the model is hooked instead."""
    rot = POSE_ROT[rot_kind]
    p = pts_e @ rot.T
    lo, hi = p.min(0), p.max(0)
    a = rot @ anchor_e if anchor_e is not None else np.array([0, hi[1], 0])
    ext = hi - lo
    limits = [40 / max(ext[0], 1e-6), 40 / max(ext[2], 1e-6), 31.0 / max(a[1] - lo[1], 1e-6)]
    if hi[1] - a[1] > 1e-6:
        limits.append(15.5 / (hi[1] - a[1]))
    k = min(1.0, *limits)
    off = np.array([8 - k * (lo[0] + hi[0]) / 2, HOOK_Y - k * a[1], 8 - k * (lo[2] + hi[2]) / 2])
    return Pose(rot, k, off)


LIMITS = {
    "ground": {"size": (44, 30, 44), "floor": 0.05},
    "hanging": {"size": (40, 34, 40), "top": 19.4},
    "head": {"size": (14, 16, 14), "floor": 0.0},
}


# ---------------------------------------------------------------- elements

def _uv_for_face(face, corners_b, uvs_px, tw, th):
    """Solve a block face's uv [u1,v1,u2,v2] and rotation from corner positions and UVs."""
    _, cu, cv = (np.array(a, float) for a in FACE_AXES[face])
    a = np.array([c @ cu for c in corners_b])
    b = np.array([c @ cv for c in corners_b])
    uv = np.array([(u * 16.0 / tw, v * 16.0 / th) for u, v in uvs_px])
    ia = (a - a.min()) / max(a.max() - a.min(), 1e-9)
    ib = (b - b.min()) / max(b.max() - b.min(), 1e-9)

    def at(x, y):
        k = int(np.argmin((ia - x) ** 2 + (ib - y) ** 2))
        return uv[k]

    tl, tr, bl = at(0, 0), at(1, 0), at(0, 1)
    straight = abs(tr[0] - tl[0]) + abs(bl[1] - tl[1])
    turned = abs(bl[0] - tl[0]) + abs(tr[1] - tl[1])
    if straight >= turned:
        # texture u runs along the face's u axis: no rotation
        return [tl[0], tl[1], tr[0], bl[1]], 0
    # texture u runs down the face: rotation 90 maps (a,b)->(u1+b(u2-u1), v1+(1-a)(v2-v1))
    return [tl[0], tr[1], bl[0], tl[1]], 90


def _region_transparent(alpha, uvs_px):
    us = [u for u, _ in uvs_px]
    vs = [v for _, v in uvs_px]
    x1, x2 = int(math.floor(min(us))), int(math.ceil(max(us)))
    y1, y2 = int(math.floor(min(vs))), int(math.ceil(max(vs)))
    if x2 <= x1 or y2 <= y1:
        return False
    region = alpha[max(y1, 0):y2, max(x1, 0):x2]
    return region.size > 0 and region.max() == 0


def _region_holed(alpha, uvs_px):
    """Whether a face's texture region has any fully transparent texels."""
    us = [u for u, _ in uvs_px]
    vs = [v for _, v in uvs_px]
    x1, x2 = int(math.floor(min(us))), int(math.ceil(max(us)))
    y1, y2 = int(math.floor(min(vs))), int(math.ceil(max(vs)))
    region = alpha[max(y1, 0):y2, max(x1, 0):x2]
    return region.size > 0 and region.min() == 0


OPPOSITE = {"north": "south", "south": "north", "east": "west", "west": "east", "up": "down", "down": "up"}


def convert_boxes(boxes, pose: Pose, render_scale, tw, th, alpha, tex_key="tex"):
    elements, worst = [], 0.0
    for part, b, m in boxes:
        r_model = m[:3, :3]
        r_total = pose.rot @ (render_scale * ENTITY_FLIP) @ r_model
        scale = np.cbrt(abs(np.linalg.det(r_total)))
        rn = r_total / scale
        axis, ang, p, err = snap(rn)
        worst = max(worst, err)
        k = pose.scale * scale

        x1, y1, z1 = b.x - b.inflate, b.y - b.inflate, b.z - b.inflate
        x2, y2, z2 = b.x + b.w + b.inflate, b.y + b.h + b.inflate, b.z + b.d + b.inflate
        c_local = np.array([(x1 + x2) / 2, (y1 + y2) / 2, (z1 + z2) / 2])
        origin = pose.point(entity_point(m, c_local, render_scale))
        half = np.abs(p) @ (np.array([x2 - x1, y2 - y1, z2 - z1]) / 2) * k
        frm, to = origin - half, origin + half

        faces, quads, see_through = {}, {}, False
        for verts, uvs, _ in am.box_quads(b, tw, th):
            n = np.cross(verts[1] - verts[0], verts[2] - verts[0])
            if np.linalg.norm(n) < 1e-9:
                continue  # zero-area side of a flat box
            if _region_transparent(alpha, uvs):
                see_through = True
                continue
            see_through |= _region_holed(alpha, uvs)
            n = n / np.linalg.norm(n)
            if (verts.mean(0) - c_local) @ n < 0:
                n = -n
            d = p @ n
            face = next(f for f, (fn, _, _) in FACE_AXES.items() if np.allclose(d, fn, atol=1e-6))
            corners = [origin + k * (p @ (v - c_local)) for v in verts]
            uv, rot = _uv_for_face(face, corners, uvs, tw, th)
            f = {"uv": [round(x, 4) for x in uv], "texture": "#" + tex_key}
            if rot:
                f["rotation"] = rot
            faces[face] = f
            quads[face] = (corners, uvs)
        if not faces:
            continue
        el = {
            "name": part.name,
            "from": [round(float(x), 4) for x in frm],
            "to": [round(float(x), 4) for x in to],
            "faces": faces,
        }
        if ang != 0:
            el["rotation"] = {"angle": ang, "axis": axis, "origin": [round(float(x), 4) for x in origin]}
        elements.append(el)
        flat = min(abs(t - f) for f, t in zip(frm, to)) < 1e-6
        if see_through and not flat:
            # Alex's Mobs draws both sides of every face, so through a box's transparent faces
            # (a cockroach wing's) its far faces show from behind. Block faces are one-sided: give
            # each face a back side on the same plane, mapped to the same texels.
            for face, (corners, uvs) in quads.items():
                normal = FACE_AXES[face][0]
                a = next(i for i in range(3) if normal[i] != 0)
                plane = float(to[a]) if normal[a] > 0 else float(frm[a])
                bf, bt = [float(x) for x in frm], [float(x) for x in to]
                bf[a] = bt[a] = plane
                back = OPPOSITE[face]
                uv, rot = _uv_for_face(back, corners, uvs, tw, th)
                f = {"uv": [round(x, 4) for x in uv], "texture": "#" + tex_key}
                if rot:
                    f["rotation"] = rot
                bel = {"name": part.name + "_back", "from": [round(x, 4) for x in bf],
                       "to": [round(x, 4) for x in bt], "faces": {back: f}}
                if "rotation" in el:
                    bel["rotation"] = el["rotation"]
                elements.append(bel)
    return elements, worst


def clamp_check(elements, name):
    for el in elements:
        for c in el["from"] + el["to"]:
            if c < -16 or c > 32:
                raise ValueError(f"{name}: element {el['name']} out of range: {el['from']} {el['to']}")


def load_alpha(path):
    return np.array(Image.open(path).convert("RGBA"))[:, :, 3]
