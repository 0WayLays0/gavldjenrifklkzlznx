#!/usr/bin/env python3
"""Render lab rasteriser: draws the quads a renderer recorded (see lab/renderlab/Recorder.java) the way the game would,
roughly: entity shading, see-through layers blended back to front, additive (eyes) layers added, a stone floor and a
sky behind, reference boxes for scale.

    python3 raster.py scene.txt out.png --eye 6,4,-8 --at 0,2,0 [--size 640x360] [--fov 70] [--night]

Textures are read from src/main/resources/assets/<namespace>/<path> (vanilla ones from tools/renderlab/vanilla/ if
present, else drawn as white).
"""
import argparse
import math
import os

import numpy as np
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets")
VANILLA = os.path.join(os.path.dirname(os.path.abspath(__file__)), "vanilla")

_textures = {}


def texture(path):
    if path in _textures:
        return _textures[path]
    arr = None
    if path != "-":
        ns, rel = path.split("/", 1)
        for base in (os.path.join(ASSETS, ns, rel), os.path.join(VANILLA, ns, rel)):
            if os.path.exists(base):
                arr = np.asarray(Image.open(base).convert("RGBA"), dtype=np.float32) / 255
                break
    if arr is None:
        arr = np.ones((1, 1, 4), np.float32)
    _textures[path] = arr
    return arr


def parse(path):
    quads, boxes = [], []
    cur = None
    verts = []
    for line in open(path):
        p = line.split()
        if not p:
            continue
        if p[0] == "B":
            cur = (p[1], p[2] == "1", p[3])
            verts = []
        elif p[0] == "V":
            verts.append([float(x) for x in p[1:]])
            if len(verts) == 4:
                quads.append((cur, np.array(verts, np.float32)))
                verts = []
        elif p[0] == "X":
            v = [float(x) for x in p[1:7]]
            boxes.append((v, int(p[7])))
    return quads, boxes


def box_quads(v, rgb):
    x0, y0, z0, x1, y1, z1 = v
    c = [(rgb >> 16 & 255), (rgb >> 8 & 255), rgb & 255, 255]
    out = []
    faces = [
        ((x0, y1, z0), (x0, y1, z1), (x1, y1, z1), (x1, y1, z0), (0, 1, 0)),
        ((x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1), (0, -1, 0)),
        ((x0, y0, z0), (x0, y1, z0), (x1, y1, z0), (x1, y0, z0), (0, 0, -1)),
        ((x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1), (0, 0, 1)),
        ((x0, y0, z0), (x0, y0, z1), (x0, y1, z1), (x0, y1, z0), (-1, 0, 0)),
        ((x1, y0, z0), (x1, y1, z0), (x1, y1, z1), (x1, y0, z1), (1, 0, 0)),
    ]
    for *pts, n in faces:
        vs = [[*pt, 0, 0, *c, 0xF000F0, *n] for pt in pts]
        out.append((("solid", True, "-"), np.array(vs, np.float32)))
    return out


def floor_quads(half=24):
    out = []
    for x in range(-half, half):
        for z in range(-half, half):
            light = (x + z) % 2 == 0
            g = 150 if light else 128
            if (x // 4 + z // 4) % 3 == 0:
                g += 40
            c = [g, g, g + 4, 255]
            vs = [[x, 0, z, 0, 0, *c, 0xF000F0, 0, 1, 0], [x, 0, z + 1, 0, 0, *c, 0xF000F0, 0, 1, 0],
                  [x + 1, 0, z + 1, 0, 0, *c, 0xF000F0, 0, 1, 0], [x + 1, 0, z, 0, 0, *c, 0xF000F0, 0, 1, 0]]
            out.append((("solid", True, "-"), np.array(vs, np.float32)))
    return out


L0 = np.array([0.2, 1.0, -0.7], np.float32)
L0 /= np.linalg.norm(L0)
L1 = np.array([-0.2, 1.0, 0.7], np.float32)
L1 /= np.linalg.norm(L1)


def shade(n):
    n = n / max(1e-6, np.linalg.norm(n))
    return min(1.0, (max(0.0, float(L0 @ n)) + max(0.0, float(L1 @ n))) * 0.6 + 0.4)


def lightmap(packed, night):
    block = (int(packed) & 0xFFFF) >> 4
    sky = (int(packed) >> 20) & 0xFFFF
    sky_level = sky * (0.25 if night else 1.0)
    lv = max(block, sky_level) / 15
    return 0.05 + 0.95 * lv ** 1.4


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("scene")
    ap.add_argument("out")
    ap.add_argument("--eye", default="6,4,-8")
    ap.add_argument("--at", default="0,2,0")
    ap.add_argument("--size", default="640x360")
    ap.add_argument("--fov", type=float, default=70)
    ap.add_argument("--night", action="store_true")
    ap.add_argument("--nofloor", action="store_true")
    a = ap.parse_args()
    W, H = (int(x) for x in a.size.split("x"))
    eye = np.array([float(x) for x in a.eye.split(",")], np.float32)
    at = np.array([float(x) for x in a.at.split(",")], np.float32)
    fwd = at - eye
    fwd /= np.linalg.norm(fwd)
    right = np.cross(fwd, np.array([0, 1, 0], np.float32))
    right /= np.linalg.norm(right)
    up = np.cross(right, fwd)
    f = 1 / math.tan(math.radians(a.fov) / 2)
    aspect = W / H

    def project(p):
        d = p - eye
        x, y, z = d @ right, d @ up, d @ fwd
        return x, y, z

    quads, boxes = parse(a.scene)
    scene = ([] if a.nofloor else floor_quads()) + [q for b in boxes for q in box_quads(*b)] + quads

    # Sky
    sky_top = np.array([0.36, 0.55, 0.95] if not a.night else [0.02, 0.03, 0.08])
    sky_bot = np.array([0.72, 0.84, 1.0] if not a.night else [0.08, 0.1, 0.18])
    t = np.linspace(0, 1, H)[:, None, None]
    img = (sky_top * (1 - t) + sky_bot * t) * np.ones((H, W, 3))
    zbuf = np.full((H, W), np.inf, np.float32)

    def tris_of(q):
        (blend, cull, tex), v = q
        pts = v[:, 0:3]
        cam = np.array([project(p) for p in pts], np.float32)
        return blend, cull, tex, v, cam

    opaque, trans = [], []
    for q in scene:
        blend, cull, tex, v, cam = tris_of(q)
        if (cam[:, 2] < 0.05).any():
            continue
        for idx in ((0, 1, 2), (0, 2, 3)):
            item = (blend, cull, tex, v[list(idx)], cam[list(idx)])
            if blend in ("solid", "cutout"):
                opaque.append(item)
            else:
                trans.append(item)

    def raster(item, write_depth, mode):
        blend, cull, tex, v, cam = item
        sx = (cam[:, 0] / cam[:, 2] * f / aspect * 0.5 + 0.5) * W
        sy = (0.5 - cam[:, 1] / cam[:, 2] * f * 0.5) * H
        x0, x1 = int(max(0, math.floor(sx.min()))), int(min(W - 1, math.ceil(sx.max())))
        y0, y1 = int(max(0, math.floor(sy.min()))), int(min(H - 1, math.ceil(sy.max())))
        if x0 > x1 or y0 > y1:
            return
        area = (sx[1] - sx[0]) * (sy[2] - sy[0]) - (sx[2] - sx[0]) * (sy[1] - sy[0])
        if abs(area) < 1e-9:
            return
        if cull and area > 0:
            return
        ys, xs = np.mgrid[y0:y1 + 1, x0:x1 + 1].astype(np.float32) + 0.5
        w0 = ((sx[1] - xs) * (sy[2] - ys) - (sx[2] - xs) * (sy[1] - ys)) / area
        w1 = ((sx[2] - xs) * (sy[0] - ys) - (sx[0] - xs) * (sy[2] - ys)) / area
        w2 = 1 - w0 - w1
        m = (w0 >= 0) & (w1 >= 0) & (w2 >= 0)
        if not m.any():
            return
        iz = 1 / cam[:, 2]
        wz = w0 * iz[0] + w1 * iz[1] + w2 * iz[2]
        depth = 1 / wz
        zb = zbuf[y0:y1 + 1, x0:x1 + 1]
        m &= depth < zb
        if not m.any():
            return
        b0, b1, b2 = (w0 * iz[0] / wz)[m], (w1 * iz[1] / wz)[m], (w2 * iz[2] / wz)[m]

        def interp(col):
            return b0 * v[0, col] + b1 * v[1, col] + b2 * v[2, col]

        u, vv = interp(3), interp(4)
        tx = texture(tex)
        th, tw = tx.shape[:2]
        ti = (np.floor(vv * th).astype(int) % th, np.floor(u * tw).astype(int) % tw)
        texel = tx[ti[0], ti[1]]
        vc = np.stack([interp(5), interp(6), interp(7), interp(8)], -1) / 255
        col = texel * vc
        if mode != "add":
            n = (v[0, 10:13] + v[1, 10:13] + v[2, 10:13])
            light = lightmap(v[0, 9], a.night) * shade(n)
            col[:, :3] *= light
        region = img[y0:y1 + 1, x0:x1 + 1]
        if mode == "opaque":
            if blend == "cutout":
                keep = col[:, 3] > 0.1
                mm = m.copy()
                mm[m] = keep
                region[mm] = col[keep, :3]
                zb[mm] = depth[mm]
            else:
                region[m] = col[:, :3]
                zb[m] = depth[m]
        elif mode == "alpha":
            al = col[:, 3:4]
            region[m] = region[m] * (1 - al) + col[:, :3] * al
        else:
            region[m] = np.minimum(1, region[m] + col[:, :3] * col[:, 3:4] ** 0 * 1)

    for item in opaque:
        raster(item, True, "opaque")
    trans.sort(key=lambda it: -float(it[4][:, 2].mean()))
    for item in trans:
        raster(item, False, "add" if item[0] == "add" else "alpha")
    Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8)).save(a.out)
    print("wrote", a.out, len(quads), "quads")


if __name__ == "__main__":
    main()
