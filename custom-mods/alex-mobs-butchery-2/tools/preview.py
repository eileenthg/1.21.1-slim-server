"""Software preview of block models and source entity models, for side-by-side checks.

Both renders share one rasteriser, so a mismatch between them is a conversion error. The block
side follows Minecraft's face UV rules as convert.py assumes them; confirming those rules
themselves needs the real game.
"""

from __future__ import annotations

import math

import numpy as np
from PIL import Image, ImageDraw

import ammodel as am
import convert as cv

SHADE = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8, "east": 0.6, "west": 0.6}


def _shade_for(n):
    n = n / (np.linalg.norm(n) + 1e-12)
    # Minecraft-ish directional shading
    return 0.5 + 0.5 * max(n[1], 0) + 0.3 * abs(n[2]) * (n[1] < 0.5) + 0.1 * abs(n[0])


class Camera:
    def __init__(self, direction=(0.45, -0.55, 1.0), ppu=6.0, size=(420, 360), center=(8, 8, 8)):
        d = np.array(direction, float)
        self.d = d / np.linalg.norm(d)
        up = np.array([0.0, 1.0, 0.0])
        r = np.cross(self.d, up)
        self.r = r / np.linalg.norm(r)
        self.u = np.cross(self.r, self.d)
        self.ppu = ppu
        self.w, self.h = size
        self.c = np.array(center, float)

    def project(self, p):
        q = p - self.c
        return np.array([self.w / 2 + (q @ self.r) * self.ppu, self.h / 2 - (q @ self.u) * self.ppu, q @ self.d])


def rasterise(quads, cam: Camera, bg=(46, 46, 52)):
    """quads: list of (corners[4] xyz, uv[4] in 0..1, texture RGBA array)."""
    img = np.zeros((cam.h, cam.w, 3), float)
    img[:] = bg
    zbuf = np.full((cam.h, cam.w), np.inf)
    for corners, uvs, tex in quads:
        corners = np.asarray(corners, float)
        n = np.cross(corners[1] - corners[0], corners[2] - corners[0])
        if np.linalg.norm(n) < 1e-9:
            n = np.cross(corners[2] - corners[0], corners[3] - corners[0])
        if n @ cam.d >= 0:  # back face
            continue
        shade = _shade_for(n)
        pr = np.array([cam.project(c) for c in corners])
        th, tw = tex.shape[:2]
        for tri in ((0, 1, 2), (0, 2, 3)):
            p = pr[list(tri)]
            t = np.array([uvs[i] for i in tri])
            x0, x1 = int(max(math.floor(p[:, 0].min()), 0)), int(min(math.ceil(p[:, 0].max()), cam.w - 1))
            y0, y1 = int(max(math.floor(p[:, 1].min()), 0)), int(min(math.ceil(p[:, 1].max()), cam.h - 1))
            if x1 < x0 or y1 < y0:
                continue
            xs, ys = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
            (ax, ay), (bx, by), (cx, cy) = p[:, :2]
            den = (by - cy) * (ax - cx) + (cx - bx) * (ay - cy)
            if abs(den) < 1e-9:
                continue
            l1 = ((by - cy) * (xs - cx) + (cx - bx) * (ys - cy)) / den
            l2 = ((cy - ay) * (xs - cx) + (ax - cx) * (ys - cy)) / den
            l3 = 1 - l1 - l2
            inside = (l1 >= -1e-6) & (l2 >= -1e-6) & (l3 >= -1e-6)
            if not inside.any():
                continue
            z = l1 * p[0, 2] + l2 * p[1, 2] + l3 * p[2, 2]
            u = l1 * t[0, 0] + l2 * t[1, 0] + l3 * t[2, 0]
            v = l1 * t[0, 1] + l2 * t[1, 1] + l3 * t[2, 1]
            tx = np.clip((u * tw).astype(int), 0, tw - 1)
            ty = np.clip((v * th).astype(int), 0, th - 1)
            texel = tex[ty, tx]
            ok = inside & (texel[..., 3] >= 128)
            sub = zbuf[y0:y1 + 1, x0:x1 + 1]
            ok &= z < sub
            sub[ok] = z[ok]
            img[y0:y1 + 1, x0:x1 + 1][ok] = texel[..., :3][ok] * shade
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8))


def block_quads(model_elements, textures):
    """Block-model elements -> quads, following Minecraft's face UV + rotation rules."""
    out = []
    for el in model_elements:
        frm, to = np.array(el["from"], float), np.array(el["to"], float)
        rot = el.get("rotation")
        if rot:
            r = cv.AXES[rot["axis"]](math.radians(rot["angle"]))
            o = np.array(rot["origin"], float)
        for face, f in el["faces"].items():
            n, cu, cvv = (np.array(a, float) for a in cv.FACE_AXES[face])

            def corner(a, b):
                pos = np.zeros(3)
                for i in range(3):
                    if n[i]:
                        pos[i] = to[i] if n[i] > 0 else frm[i]
                    elif cu[i]:
                        pos[i] = frm[i] + a * (to[i] - frm[i]) if cu[i] > 0 else to[i] - a * (to[i] - frm[i])
                    else:
                        pos[i] = frm[i] + b * (to[i] - frm[i]) if cvv[i] > 0 else to[i] - b * (to[i] - frm[i])
                return pos

            u1, v1, u2, v2 = (x / 16.0 for x in f["uv"])
            fr = f.get("rotation", 0)

            def uv(a, b):
                if fr == 0:
                    return u1 + a * (u2 - u1), v1 + b * (v2 - v1)
                if fr == 90:
                    return u1 + b * (u2 - u1), v1 + (1 - a) * (v2 - v1)
                if fr == 180:
                    return u1 + (1 - a) * (u2 - u1), v1 + (1 - b) * (v2 - v1)
                return u1 + (1 - b) * (u2 - u1), v1 + a * (v2 - v1)

            ab = [(0, 0), (0, 1), (1, 1), (1, 0)]
            corners = [corner(a, b) for a, b in ab]
            if rot:
                corners = [o + r @ (c - o) for c in corners]
            # keep counter-clockwise-from-outside order for back-face culling
            c = np.array(corners)
            if np.cross(c[1] - c[0], c[2] - c[0]) @ (r @ n if rot else n) < 0:
                ab = ab[::-1]
                corners = corners[::-1]
            out.append((corners, [uv(a, b) for a, b in ab], textures[f["texture"].lstrip("#")]))
    return out


def source_quads(boxes, pose: cv.Pose, render_scale, tex):
    th, tw = tex.shape[:2]
    out = []
    for _, b, m in boxes:
        centre = np.array([b.x + b.w / 2, b.y + b.h / 2, b.z + b.d / 2])
        for verts, uvs, _ in am.box_quads(b, tw, th):
            corners = [pose.point(cv.entity_point(m, v, render_scale)) for v in verts]
            c = np.array(corners)
            n = np.cross(c[1] - c[0], c[2] - c[0])
            out_dir = pose.point(cv.entity_point(m, verts.mean(0), render_scale)) - pose.point(
                cv.entity_point(m, centre, render_scale))
            order = list(range(4))
            if np.linalg.norm(n) > 1e-9 and n @ out_dir < 0:
                order = order[::-1]
            out.append(([corners[i] for i in order], [(uvs[i][0] / tw, uvs[i][1] / th) for i in order], tex))
    return out


def label(img, text):
    d = ImageDraw.Draw(img)
    d.text((6, 4), text, fill=(235, 235, 235))
    return img


def sheet(images, cols):
    w, h = images[0].size
    rows = (len(images) + cols - 1) // cols
    out = Image.new("RGB", (w * cols, h * rows), (20, 20, 24))
    for i, im in enumerate(images):
        out.paste(im, ((i % cols) * w, (i // cols) * h))
    return out
