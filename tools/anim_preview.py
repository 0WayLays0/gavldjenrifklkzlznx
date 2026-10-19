#!/usr/bin/env python3
"""Draws a player animation tick by tick, the way Player Animator plays it, to check a move before it ships.

    python3 tools/anim_preview.py <animations.json> <name> <out.png> [--item right|left|both] [--marks 3,5]

It reads the Bedrock-format JSON (the addon's own or Iron's), keys cut down to whole ticks as Player Animator does,
and poses the vanilla player model with Player Animator's own transforms: the body bone about the hips, then every
part on its pivot, the held item through the hand. Each column is a tick; the rows show the player from their right
side, from the front at three quarters, and from above. A held sword is drawn as a grey blade with a dark hilt.
"""
import argparse
import json
import math
import sys

import numpy as np
from PIL import Image, ImageDraw

DEFAULT_POS = {
    "head": (0, 0, 0), "torso": (0, 0, 0),
    "right_arm": (-5, 2, 0), "left_arm": (5, 2, 0),
    "right_leg": (-1.9, 12, .1), "left_leg": (1.9, 12, .1),
}
CUBES = {
    "head": ((-4, -8, -4), (8, 8, 8)),
    "torso": ((-4, 0, -2), (8, 12, 4)),
    "right_arm": ((-3, -2, -2), (4, 12, 4)),
    "left_arm": ((-1, -2, -2), (4, 12, 4)),
    "right_leg": ((-2, 0, -2), (4, 12, 4)),
    "left_leg": ((-2, 0, -2), (4, 12, 4)),
}
COLORS = {
    "head": (214, 166, 128), "torso": (40, 150, 160), "right_arm": (204, 150, 112), "left_arm": (176, 126, 94),
    "right_leg": (60, 66, 150), "left_leg": (46, 50, 120),
}
FRONT = {"head": (236, 196, 160), "torso": (90, 196, 200)}


def rx(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1]])


def ry(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1]])


def rz(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]])


def tr(x, y, z):
    m = np.eye(4)
    m[:3, 3] = (x, y, z)
    return m


def sc(x, y, z):
    return np.diag([x, y, z, 1.])


# ---------------------------------------------------------------- Player Animator's reading and playing

EASES = {
    "linear": lambda f: f,
    "easeinsine": lambda f: 1 - math.cos(f * math.pi / 2),
    "easeoutsine": lambda f: math.sin(f * math.pi / 2),
    "easeinoutsine": lambda f: -(math.cos(math.pi * f) - 1) / 2,
    "easeinquad": lambda f: f * f,
    "easeoutquad": lambda f: 1 - (1 - f) ** 2,
    "easeinoutquad": lambda f: 2 * f * f if f < .5 else 1 - (-2 * f + 2) ** 2 / 2,
    "easeincubic": lambda f: f ** 3,
    "easeoutcubic": lambda f: 1 - (1 - f) ** 3,
    "easeinoutcubic": lambda f: 4 * f ** 3 if f < .5 else 1 - (-2 * f + 2) ** 3 / 2,
}


def _tick(key):
    return int(np.float32(key) * np.float32(20))


def _num(v):
    try:
        return float(v)
    except (TypeError, ValueError):
        return 0.


def read_axis_keys(node):
    """[(tick, (a, b, c), ease)] from a rotation or position node."""
    if isinstance(node, list):
        return [(0, tuple(_num(v) for v in node), "linear")]
    keys = []
    for k, v in node.items():
        if k in ("vector", "easing"):
            if k == "vector":
                keys.append((0, tuple(_num(a) for a in v), "linear"))
            continue
        tick = _tick(k)
        if isinstance(v, list):
            keys.append((tick, tuple(_num(a) for a in v), "constant"))
            continue
        ease = "linear"
        if "lerp_mode" in v:
            ease = "easeinoutsine" if v["lerp_mode"] == "catmullrom" else v["lerp_mode"].lower()
        if "easing" in v:
            ease = v["easing"].lower()
        for part in ("pre", "vector", "post"):
            if part in v:
                vec = v[part]["vector"] if isinstance(v[part], dict) else v[part]
                keys.append((tick, tuple(_num(a) for a in vec), ease))
    keys.sort(key=lambda k: k[0])
    return keys


class Played:
    def __init__(self, anim):
        self.end = math.ceil(np.float32(anim["animation_length"]) * np.float32(20))
        self.bones = {}
        for bone, node in anim.get("bones", {}).items():
            self.bones[bone] = {ch: read_axis_keys(node[ch]) for ch in ("rotation", "position") if ch in node}

    def value(self, bone, channel, tick, rest):
        """The bone's channel at {@code tick} as Player Animator plays it (rest when the bone is not keyed)."""
        if bone not in self.bones:
            return None
        keys = self.bones[bone].get(channel)
        if not keys:
            # The bone is keyed, so its other channel sits at rest
            return np.array(rest, dtype=float)
        before = None
        for i, k in enumerate(keys):
            if k[0] <= tick:
                before = i
        if before is None:
            k0, k1 = (0, rest, "linear"), keys[0]
        elif before + 1 < len(keys):
            k0, k1 = keys[before], keys[before + 1]
        else:
            k0 = keys[before]
            k1 = (self.end, k0[1], k0[2])
        if k1[0] <= k0[0]:
            return np.array(k0[1], dtype=float)
        f = (tick - k0[0]) / (k1[0] - k0[0])
        f = 0 if k0[2] == "constant" else EASES.get(k0[2], EASES["linear"])(f)
        return np.array(k0[1], dtype=float) + (np.array(k1[1], dtype=float) - np.array(k0[1], dtype=float)) * f


def pose_matrices(played, tick, items):
    """Local-space (blocks, x right, y up, z back) matrices of each part and the held items' blade ends."""
    br = played.value("body", "rotation", tick, (0, 0, 0))
    bp = played.value("body", "position", tick, (0, 0, 0))
    body = np.eye(4)
    if br is not None or bp is not None:
        br = np.radians(br if br is not None else (0, 0, 0))
        bp = bp if bp is not None else (0, 0, 0)
        # PlayerRendererMixin: translate(x, y + 0.7, z), roll, pitch, yaw, translate(0, -0.7, 0); the parser turned
        # pitch and yaw around and the x position, and made positions blocks
        body = tr(-bp[0] / 16, bp[1] / 16 + .7, bp[2] / 16) @ rz(br[2]) @ ry(-br[1]) @ rx(-br[0]) @ tr(0, -.7, 0)
    model = body @ sc(-1, -1, 1) @ sc(.9375, .9375, .9375) @ tr(0, -1.501, 0)
    parts = {}
    for part in CUBES:
        rest = DEFAULT_POS[part]
        r = played.value(part, "rotation", tick, (0, 0, 0))
        p = played.value(part, "position", tick, (0, 0, 0))
        r = np.radians(r) if r is not None else np.zeros(3)
        p = (p[0] + rest[0], -p[1] + rest[1], p[2] + rest[2]) if p is not None else rest
        parts[part] = model @ tr(p[0] / 16, p[1] / 16, p[2] / 16) @ rz(r[2]) @ ry(r[1]) @ rx(r[0])
    blades = []
    for side in items:
        arm = parts[side + "_arm"]
        m = arm @ rx(-math.pi / 2) @ ry(math.pi) @ tr((-1 if side == "left" else 1) / 16, .125, -.625)
        ir = played.value(side + "_item", "rotation", tick, (0, 0, 0))
        if ir is not None:
            ir = np.radians(ir)
            m = m @ rz(ir[2]) @ ry(ir[1]) @ rx(ir[0])
        m = m @ tr(0, 4 / 16, .5 / 16) @ rx(0) @ ry(math.radians(-90)) @ rz(math.radians(55)) @ sc(.85, .85, .85) @ tr(-.5, -.5, -.5)
        pts = [m @ np.array([u, u, .5, 1]) for u in (.06, .3, .94)]
        blades.append([p[:3] for p in pts])
    return parts, blades


# ---------------------------------------------------------------- drawing

VIEWS = {
    "side": np.array([[0, 0, -1], [0, 1, 0], [1, 0, 0]], dtype=float),
    "top": np.array([[1, 0, 0], [0, 0, -1], [0, 1, 0]], dtype=float),
}
_front = np.array([[-1, 0, 0], [0, 1, 0], [0, 0, -1]], dtype=float)
_yaw = math.radians(-40)
_pitch = math.radians(15)
VIEWS["front"] = np.array([[1, 0, 0], [0, math.cos(_pitch), -math.sin(_pitch)], [0, math.sin(_pitch), math.cos(_pitch)]]) \
                 @ np.array([[math.cos(_yaw), 0, math.sin(_yaw)], [0, 1, 0], [-math.sin(_yaw), 0, math.cos(_yaw)]]) @ _front
LIGHT = np.array([-.4, .7, .6]) / np.linalg.norm([-.4, .7, .6])


def _faces(part, m):
    (x0, y0, z0), (w, h, d) = CUBES[part]
    corners = {}
    for i in (0, 1):
        for j in (0, 1):
            for k in (0, 1):
                v = m @ np.array([(x0 + i * w) / 16, (y0 + j * h) / 16, (z0 + k * d) / 16, 1])
                corners[i, j, k] = v[:3]
    quads = {
        "x0": [(0, 0, 0), (0, 1, 0), (0, 1, 1), (0, 0, 1)], "x1": [(1, 0, 0), (1, 0, 1), (1, 1, 1), (1, 1, 0)],
        "y0": [(0, 0, 0), (0, 0, 1), (1, 0, 1), (1, 0, 0)], "y1": [(0, 1, 0), (1, 1, 0), (1, 1, 1), (0, 1, 1)],
        "z0": [(0, 0, 0), (1, 0, 0), (1, 1, 0), (0, 1, 0)], "z1": [(0, 0, 1), (0, 1, 1), (1, 1, 1), (1, 0, 1)],
    }
    return {name: [corners[c] for c in q] for name, q in quads.items()}


def draw_cell(img, ox, oy, size, view, parts, blades, scale, center):
    d = ImageDraw.Draw(img)
    V = VIEWS[view]
    polys = []
    for part, m in parts.items():
        faces = _faces(part, m)
        middle = V @ (sum(sum(q) for q in faces.values()) / 24 - center)
        for name, quad in faces.items():
            pts = [V @ (p - center) for p in quad]
            n = sum(pts) / 4 - middle
            if np.linalg.norm(n) < 1e-9:
                continue
            n /= np.linalg.norm(n)
            # Only the faces turned to the viewer
            if n[2] <= 1e-6:
                continue
            color = COLORS[part]
            if name == "z0" and part in FRONT:
                color = FRONT[part]
            shade = .55 + .45 * max(0., float(n @ LIGHT))
            color = tuple(int(c * shade) for c in color)
            depth = sum(p[2] for p in pts) / 4
            polys.append((depth, [(ox + size / 2 + p[0] * scale, oy + size * .62 - p[1] * scale) for p in pts], color))
    for blade in blades:
        pts = [V @ (p - center) for p in blade]
        depth = max(p[2] for p in pts) + .05
        polys.append((depth, [(ox + size / 2 + p[0] * scale, oy + size * .62 - p[1] * scale) for p in pts], "blade"))
    polys.sort(key=lambda p: p[0])
    for depth, pts, color in polys:
        if color == "blade":
            d.line([pts[1], pts[2]], fill=(200, 205, 215), width=3)
            d.line([pts[0], pts[1]], fill=(70, 45, 25), width=3)
            d.ellipse([pts[2][0] - 2, pts[2][1] - 2, pts[2][0] + 2, pts[2][1] + 2], fill=(240, 240, 250))
        else:
            d.polygon(pts, fill=color, outline=(20, 20, 20))


def render(anim, items, marks, scale=46, size=130, ticks=None):
    played = Played(anim)
    ticks = list(range(played.end + 1)) if ticks is None else ticks
    rows = ["side", "front", "top"]
    img = Image.new("RGB", (size * len(ticks), size * len(rows) + 14), (236, 236, 228))
    d = ImageDraw.Draw(img)
    center = np.array([0, .9, 0])
    for c, tick in enumerate(ticks):
        parts, blades = pose_matrices(played, tick, items)
        for r, view in enumerate(rows):
            ox, oy = c * size, 14 + r * size
            d.rectangle([ox, oy, ox + size - 1, oy + size - 1], outline=(200, 200, 190))
            if view == "side":
                gy = oy + size * .62 + .9 * scale
                d.line([ox, gy, ox + size, gy], fill=(120, 110, 90))
                # A block of reach, forward
                for b in (1, 2, 3):
                    x = ox + size / 2 + b * scale
                    d.line([x, gy - 3, x, gy + 3], fill=(150, 140, 120))
            draw_cell(img, ox, oy, size, view, parts, blades, scale if view != "top" else scale * .9,
                      center if view != "top" else np.array([0, 0, 0]))
        d.text((c * size + 4, 1), f"t{tick}", fill=(200, 30, 30) if tick in marks else (40, 40, 40))
    return img


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("file")
    ap.add_argument("name")
    ap.add_argument("out")
    ap.add_argument("--item", default="right", choices=["right", "left", "both", "none"])
    ap.add_argument("--marks", default="")
    ap.add_argument("--ticks", default="")
    ap.add_argument("--zoom", type=float, default=1)
    args = ap.parse_args(argv)
    anims = json.load(open(args.file))["animations"]
    if args.name not in anims:
        sys.exit(f"no animation {args.name}; there are: {', '.join(sorted(anims))}")
    items = {"right": ["right"], "left": ["left"], "both": ["right", "left"], "none": []}[args.item]
    marks = {int(m) for m in args.marks.split(",") if m}
    ticks = [int(t) for t in args.ticks.split(",") if t] or None
    render(anims[args.name], items, marks, scale=46 * args.zoom, size=int(130 * args.zoom), ticks=ticks).save(args.out)


if __name__ == "__main__":
    main()
