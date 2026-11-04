#!/usr/bin/env python3
"""Lotus Magic art: the lotus petal (white at its root blushing pink to its tip, veined), the round lotus leaf, the top of
the seed head with its seeds, plain fibre for stems and stamens, the knotted root's skin, black mud, the mandala of
lotus petals (a glow mask), the spell icons, the Lotus Codex (icon and 3D book texture) and its affinity ring.

Called by gen_assets.py (run that one). Original art, drawn here from noise and strokes.
"""
import math
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

import gen_assets as g
from water_assets import _tnoise, _quant, _rgba, _ramp

#: Petal white, blush, the school's pink, deep rose, the gold of the heart, leaf greens, mud.
WHITE = (255, 248, 250)
BLUSH = (255, 214, 232)
PINK = (245, 138, 192)
ROSE = (216, 65, 127)
GOLD = (255, 211, 110)
LEAF = (72, 134, 84)
LEAF_LIGHT = (128, 176, 104)
LEAF_DARK = (34, 78, 52)
JADE = (63, 184, 168)
NIGHT = (46, 18, 38)
MUD = (52, 38, 26)


def _lerp3(a, b, t):
    t = np.clip(t, 0, 1)[..., None]
    return np.array(a, dtype=np.float32) * (1 - t) + np.array(b, dtype=np.float32) * t


# ------------------------------------------------------------------------------------------------------ textures

def petal_texture():
    """64 x 128: a lotus petal, its root along the top (V = 0) and its tip at the bottom. Creamy white at the root,
    white, then blushing pink and deep rose at the tip, the pink creeping down its edges; fine veins running from the
    root and meeting at the tip; a faint sheen down its middle."""
    W, H = 64, 128
    ys, xs = np.mgrid[0:H, 0:W].astype(np.float32)
    u, v = (xs + .5) / W, (ys + .5) / H
    across = np.abs(u * 2 - 1)
    n = _tnoise(W, H, (8, 16), 1501)
    t = v + .28 * across ** 2 + .06 * (n - .5)
    col = _lerp3((236, 240, 214), WHITE, t / .22)
    col = np.where((t > .22)[..., None], _lerp3(WHITE, BLUSH, (t - .22) / .28), col)
    col = np.where((t > .5)[..., None], _lerp3(BLUSH, PINK, (t - .5) / .3), col)
    col = np.where((t > .8)[..., None], _lerp3(PINK, ROSE, (t - .8) / .25), col)
    # Veins: from the root, fanning, meeting again at the tip
    veins = np.zeros((H, W), np.float32)
    for k in range(-5, 6):
        x = .5 + k / 11 * np.sin(np.pi * np.clip(v, 0, 1) ** .8) * .95
        veins = np.maximum(veins, np.exp(-((u - x) / .012) ** 2) * (.5 + .5 * np.sin(np.pi * v)))
    col = col * (1 - .16 * veins[..., None]) + np.array(ROSE, np.float32) * (.1 * veins[..., None])
    sheen = np.exp(-((u - .5) / .08) ** 2) * (1 - v) * .12
    col = np.clip(col + 255 * sheen[..., None], 0, 255)
    return _rgba(col, np.full((H, W), 255.0))


def leaf_texture():
    """128 x 128: a round lotus leaf seen from above, blue-green, a pale spot at its heart where the stalk meets it,
    veins running out from there and forking toward its rim, a darker rim, beads of wax shining here and there. Clear
    outside its circle."""
    S = 128
    ys, xs = np.mgrid[0:S, 0:S].astype(np.float32)
    x, y = (xs + .5) / S * 2 - 1, (ys + .5) / S * 2 - 1
    r = np.hypot(x, y)
    a = np.arctan2(y, x)
    n = _tnoise(S, S, (8, 8), 1511)
    fine = _tnoise(S, S, (32, 32), 1512)
    base = _lerp3(LEAF_DARK, LEAF, .55 + .35 * n)
    base = base * (.92 + .16 * fine[..., None])
    # Veins: 19 out from the heart, each forking half way
    vein = np.zeros_like(r)
    for k in range(19):
        ak = k / 19 * 2 * math.pi
        for fork in (-.09, 0, .09):
            start = 0 if fork == 0 else .45
            d = np.angle(np.exp(1j * (a - ak - fork * np.clip((r - start) / (1 - start), 0, 1))))
            w = .022 * (1.2 - r)
            vein = np.maximum(vein, np.exp(-(d * r / np.maximum(w, .005)) ** 2) * (r > start) * (1 if fork == 0 else .7))
    col = base + (np.array(LEAF_LIGHT, np.float32) - base) * (.75 * vein[..., None])
    # The pale heart, the darker rim
    heart = np.exp(-(r / .1) ** 2)
    col = col + (np.array((206, 220, 170), np.float32) - col) * heart[..., None]
    rim = np.clip((r - .86) / .12, 0, 1)
    col = col * (1 - .35 * rim[..., None])
    # Beads of wax catching the light
    rnd = np.random.default_rng(1513)
    for _ in range(36):
        bx, by = rnd.random() * 1.6 - .8, rnd.random() * 1.6 - .8
        if math.hypot(bx, by) > .85:
            continue
        d = np.hypot(x - bx, y - by)
        col = col + 90 * np.exp(-(d / .018) ** 2)[..., None]
    alpha = np.where(r < .99, 255, 0).astype(np.float32)
    return _rgba(np.clip(col, 0, 255), alpha)


#: The pits of the seed head (in pairs, fractions of its radius): as LotusMesh.PITS.
PITS = [(0.0, 0.0)] + [(math.cos(i * math.tau / 6) * .38, math.sin(i * math.tau / 6) * .38) for i in range(6)] + \
       [(math.cos((i + .5) * math.tau / 10) * .74, math.sin((i + .5) * math.tau / 10) * .74) for i in range(10)]


def pod_texture():
    """64 x 64: the flat top of a lotus seed head: spongy yellow-green, seventeen pits, a green seed sitting in each,
    a darker ring at its edge. Clear outside its circle."""
    S = 64
    ys, xs = np.mgrid[0:S, 0:S].astype(np.float32)
    x, y = (xs + .5) / S * 2 - 1, (ys + .5) / S * 2 - 1
    r = np.hypot(x, y)
    n = _tnoise(S, S, (16, 16), 1521)
    col = _lerp3((176, 186, 96), (222, 226, 132), .4 + .5 * n)
    col = col * (1 - .3 * np.clip((r - .82) / .16, 0, 1))[..., None]
    for px, py in PITS:
        d = np.hypot(x - px, y - py)
        pit = np.clip(1 - (d - .11) / .03, 0, 1)
        col = col * (1 - .55 * pit[..., None])
        seed = np.clip(1 - (d - .085) / .02, 0, 1)
        shade = np.clip(.65 + .5 * ((px - x) + (py - y)) / .1, .3, 1.3)
        col = col * (1 - seed[..., None]) + (np.array((126, 156, 64), np.float32) * shade[..., None]) * seed[..., None]
    alpha = np.where(r < .995, 255, 0).astype(np.float32)
    return _rgba(np.clip(col, 0, 255), alpha)


def fibre_texture():
    """32 x 64, tiling: plain near-white fibres running down it (V), for stems, stamens and seeds, tinted in game."""
    W, H = 32, 64
    streak = _tnoise(W, H, (16, 2), 1531)
    fine = _tnoise(W, H, (32, 8), 1532)
    t = np.clip(.84 + .12 * streak + .06 * (fine - .5), 0, 1)
    return _rgba(np.dstack([t * 255] * 3), np.full((H, W), 255.0))


def root_texture():
    """64 x 64, tiling: the skin of the lotus root, pale tan, faint streaks running along it, brown speckles and
    darker scars."""
    S = 64
    streak = _tnoise(S, S, (16, 4), 1541)
    n = _tnoise(S, S, (8, 8), 1542)
    col = _lerp3((206, 176, 134), (244, 226, 196), .4 + .4 * streak + .2 * n)
    rnd = np.random.default_rng(1543)
    ys, xs = np.mgrid[0:S, 0:S].astype(np.float32)
    for _ in range(40):
        cx, cy, rr = rnd.random() * S, rnd.random() * S, .6 + rnd.random() * 1.6
        d = np.hypot((xs - cx + S / 2) % S - S / 2, ((ys - cy + S / 2) % S - S / 2) * .6)
        col = col * (1 - .16 * np.exp(-(d / rr) ** 2))[..., None]
    return _rgba(np.clip(col, 0, 255), np.full((S, S), 255.0))


def mud_texture():
    """64 x 64, tiling: black mud, wet and glistening in patches, a few pale pebbles sunk in it."""
    S = 64
    n = _tnoise(S, S, (6, 6), 1551)
    fine = _tnoise(S, S, (24, 24), 1552)
    col = _lerp3((30, 22, 16), (78, 60, 42), .3 + .5 * n + .2 * fine)
    wet = np.clip((n - .55) / .2, 0, 1) * np.clip((fine - .5) / .3, 0, 1)
    col = col + 70 * wet[..., None]
    rnd = np.random.default_rng(1553)
    ys, xs = np.mgrid[0:S, 0:S].astype(np.float32)
    for _ in range(9):
        cx, cy, rr = rnd.random() * S, rnd.random() * S, 1 + rnd.random() * 1.5
        d = np.hypot((xs - cx + S / 2) % S - S / 2, (ys - cy + S / 2) % S - S / 2)
        peb = np.clip(1 - (d - rr) / .8, 0, 1)
        col = col * (1 - peb[..., None]) + np.array((128, 116, 98), np.float32) * peb[..., None]
    return _rgba(np.clip(col, 0, 255), np.full((S, S), 255.0))


def _petal_outline(cx, cy, r0, r1, theta, half):
    """A pointed lotus petal in a mandala, from radius r0 to r1 at angle theta, half as wide as {half} (radians)."""
    left, right = [], []
    for i in range(17):
        t = i / 16
        r = r0 + (r1 - r0) * t
        w = half * math.sin(math.pi * t ** .8) ** .7
        left.append((cx + math.cos(theta - w) * r, cy + math.sin(theta - w) * r))
        right.append((cx + math.cos(theta + w) * r, cy + math.sin(theta + w) * r))
    return left + right[::-1]


def mandala_texture():
    """256 x 256 glow mask (white on black): a mandala of lotus petals: a seed head at the middle, a ring of eight
    petals, then sixteen, then twenty-four, each veined down its middle, circles and rings of beads between them."""
    S = 1024
    img = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(img)
    c = S / 2
    R = S / 2 * .98
    lw = 7

    def circle(r, w=lw, fill=None):
        d.ellipse((c - r, c - r, c + r, c + r), outline=255, width=w, fill=fill)

    circle(R * .07, fill=200)
    for k in range(7):
        a = k * math.tau / 7
        d.ellipse((c + math.cos(a) * R * .04 - 7, c + math.sin(a) * R * .04 - 7, c + math.cos(a) * R * .04 + 7, c + math.sin(a) * R * .04 + 7), fill=0)
    circle(R * .1)
    for ring, (n, r0, r1, off) in enumerate(((8, .1, .34, 0), (16, .3, .55, .5), (24, .58, .8, 0))):
        for k in range(n):
            theta = (k + off) * math.tau / n
            half = math.tau / n * .55
            pts = _petal_outline(c, c, R * r0, R * r1, theta, half)
            d.line(pts + [pts[0]], fill=255, width=lw)
            d.line([(c + math.cos(theta) * R * (r0 + .03), c + math.sin(theta) * R * (r0 + .03)),
                    (c + math.cos(theta) * R * (r1 - .04), c + math.sin(theta) * R * (r1 - .04))], fill=150, width=lw - 3)
            tip = (c + math.cos(theta) * R * (r1 + .02), c + math.sin(theta) * R * (r1 + .02))
            d.ellipse((tip[0] - 5, tip[1] - 5, tip[0] + 5, tip[1] + 5), fill=255)
    circle(R * .57, lw - 2)
    circle(R * .84)
    for k in range(64):
        a = k * math.tau / 64
        x, y = c + math.cos(a) * R * .9, c + math.sin(a) * R * .9
        rr = 9 if k % 2 == 0 else 5
        d.ellipse((x - rr, y - rr, x + rr, y + rr), fill=255)
    circle(R * .96, lw + 2)
    circle(R * .99, lw - 3)
    glow = img.filter(ImageFilter.GaussianBlur(10))
    arr = np.maximum(np.asarray(img, np.float32), np.asarray(glow, np.float32) * 1.6)
    arr = np.clip(arr, 0, 255).astype(np.uint8)
    small = Image.fromarray(arr, "L").resize((256, 256), Image.LANCZOS)
    rgb = Image.merge("RGB", [small, small, small])
    rgba = rgb.convert("RGBA")
    rgba.putalpha(255)
    return rgba


# ------------------------------------------------------------------------------------------------------- icons

def _icon(draw, cx=.5, cy=.5, bg=(70, 30, 60)):
    return g.painted(64, draw, (cx, cy, bg, NIGHT), 16)


def _a(c, a=255):
    return tuple(int(x) for x in c) + (a,)


def _petal(d, S, x, y, length, width, angle, fill, outline=None):
    """A petal from (x, y) (its root), {length} long toward {angle} (radians, 0 = right, pi/2 = down)."""
    ca, sa = math.cos(angle), math.sin(angle)
    pts = []
    for side in (1, -1):
        rng = range(13) if side == 1 else range(12, -1, -1)
        for i in rng:
            t = i / 12
            w = width * .5 * math.sin(math.pi * t ** .8) ** .7 * side
            px, py = t * length, w
            pts.append(((x + px * ca - py * sa) * S, (y + px * sa + py * ca) * S))
    d.polygon(pts, fill=_a(fill), outline=_a(outline) if outline else None)


def _lotus(d, img, S, cx, cy, size, open_=1.0, tip=PINK):
    """A lotus seen from the side: outer petals spread, inner ones cupped, the heart glowing."""
    for k, (ang, ln, col) in enumerate(((-150, .9, BLUSH), (-30, .9, BLUSH), (-125, 1.0, PINK), (-55, 1.0, PINK), (-105, 1.05, tip), (-75, 1.05, tip),
                                        (-90, 1.0, WHITE))):
        a = math.radians(-90 + (ang + 90) * open_)
        _petal(d, S, cx, cy, size * ln, size * .55, a, col, ROSE)


def razor_petals_icon():
    def draw(d, img, S):
        for k, (x, y, a) in enumerate(((.18, .78, -.75), (.3, .62, -.55), (.22, .45, -.35))):
            d.line([(x * S - .12 * S, y * S + .1 * S), (x * S + .05 * S, y * S - .02 * S)], fill=_a((255, 230, 240), 150), width=max(1, S // 40))
            _petal(d, S, x, y, .55, .2, a, (PINK, BLUSH, ROSE)[k], ROSE)
        g.glow_spot(img, .8 * S, .2 * S, .14 * S, (255, 200, 230), .8)
    return _icon(draw)


def lotus_bud_icon():
    def draw(d, img, S):
        d.pieslice((-.1 * S, .76 * S, 1.1 * S, 1.3 * S), 180, 360, fill=_a((60, 46, 34)))
        for x, a in ((.24, 3.4), (.76, -.25)):
            d.ellipse(((x - .17) * S, .74 * S, (x + .17) * S, .86 * S), fill=_a(LEAF), outline=_a(LEAF_DARK))
        for k, (a, col) in enumerate(((-1.85, PINK), (-1.29, PINK), (-1.62, BLUSH), (-1.52, WHITE))):
            _petal(d, S, .5, .8, .62, .3 if k < 2 else .22, a, col, ROSE)
        g.glow_spot(img, .5 * S, .16 * S, .18 * S, GOLD, 1)
    return _icon(draw, .5, .3)


def sacred_pond_icon():
    def draw(d, img, S):
        d.ellipse((.04 * S, .45 * S, .96 * S, .95 * S), fill=_a(JADE), outline=_a((140, 230, 220)), width=max(1, S // 32))
        for x, y, r in ((.25, .62, .12), (.72, .8, .1), (.68, .58, .08)):
            d.ellipse(((x - r) * S, (y - r * .45) * S, (x + r) * S, (y + r * .45) * S), fill=_a(LEAF), outline=_a(LEAF_DARK))
        _lotus(d, img, S, .48, .7, .3, .9)
        g.glow_spot(img, .5 * S, .55 * S, .2 * S, GOLD, .6)
    return _icon(draw, .5, .7, (40, 60, 70))


def lotus_throne_icon():
    def draw(d, img, S):
        d.ellipse((.28 * S, .06 * S, .72 * S, .5 * S), outline=_a(GOLD), width=max(2, S // 16))
        g.glow_spot(img, .5 * S, .28 * S, .26 * S, GOLD, .7)
        d.ellipse((.4 * S, .2 * S, .6 * S, .4 * S), fill=_a((90, 60, 120)))
        _lotus(d, img, S, .5, .86, .38, 1.15)
    return _icon(draw, .5, .4)


def mire_roots_icon():
    def draw(d, img, S):
        d.ellipse((-.1 * S, .66 * S, 1.1 * S, 1.2 * S), fill=_a(MUD))
        d.ellipse((.1 * S, .7 * S, .9 * S, .86 * S), fill=_a((84, 64, 44)))
        for x0, side in ((.2, -1), (.8, 1)):
            pts = [((x0 + side * .1 * math.sin(t * 2.4)) * S, (.8 - t * .5) * S) for t in [i / 10 for i in range(11)]]
            for i in range(len(pts) - 1):
                w = int(S * (.12 - .05 * i / 10)) if i % 3 != 2 else int(S * .05)
                d.line([pts[i], pts[i + 1]], fill=_a((240, 222, 188)), width=w)
            hx, hy = pts[-1]
            d.ellipse((hx - .03 * S, hy - .03 * S, hx + .03 * S, hy + .03 * S), fill=_a((200, 170, 130)))
        d.line((.5 * S, .78 * S, .5 * S, .5 * S), fill=_a((110, 158, 74)), width=max(2, S // 22))
        _lotus(d, img, S, .5, .52, .22, .7)
        g.glow_spot(img, .5 * S, .4 * S, .14 * S, GOLD, .5)
    return _icon(draw, .5, .5, (50, 40, 50))


def seed_pod_icon():
    def draw(d, img, S):
        d.line([(.5 * S, .62 * S), (.42 * S, .95 * S)], fill=_a((110, 150, 70)), width=max(2, S // 14))
        d.polygon([(.18 * S, .3 * S), (.82 * S, .3 * S), (.58 * S, .66 * S), (.42 * S, .66 * S)], fill=_a((150, 180, 90)), outline=_a(LEAF_DARK))
        d.ellipse((.16 * S, .18 * S, .84 * S, .42 * S), fill=_a((214, 220, 128)), outline=_a((120, 140, 60)), width=max(1, S // 40))
        for px, py in PITS:
            x, y = .5 + px * .3, .3 + py * .1
            d.ellipse(((x - .035) * S, (y - .02) * S, (x + .035) * S, (y + .02) * S), fill=_a((90, 120, 50)))
        g.glow_spot(img, .5 * S, .25 * S, .2 * S, GOLD, .5)
    return _icon(draw, .5, .4, (50, 60, 40))


def lotus_mandala_icon():
    def draw(d, img, S):
        c = .5 * S
        for r, w in ((.44, 2), (.3, 2), (.12, 3)):
            d.ellipse((c - r * S, c - r * S, c + r * S, c + r * S), outline=_a(PINK), width=max(1, S // (16 * w)))
        for k in range(8):
            a = k * math.tau / 8
            _petal(d, S, .5 + math.cos(a) * .12, .5 + math.sin(a) * .12, .2, .12, a, BLUSH, PINK)
        for k in range(16):
            a = (k + .5) * math.tau / 16
            _petal(d, S, .5 + math.cos(a) * .31, .5 + math.sin(a) * .31, .12, .08, a, PINK)
        g.glow_spot(img, c, c, .14 * S, GOLD, 1)
    return _icon(draw, .5, .5, (80, 30, 70))


def lotus_lanterns_icon():
    def draw(d, img, S):
        for cx, cy, s in ((.62, .58, .3), (.26, .34, .18)):
            g.glow_spot(img, cx * S, (cy - s * .3) * S, s * 1.3 * S, (255, 170, 90), .8)
            d.ellipse(((cx - s * .8) * S, (cy - s * .06) * S, (cx + s * .8) * S, (cy + s * .2) * S), fill=_a(LEAF))
            _lotus(d, img, S, cx, cy, s, .55, PINK)
            g.glow_spot(img, cx * S, (cy - s * .45) * S, s * .4 * S, (255, 240, 200), 1)
    return _icon(draw, .5, .5, (40, 30, 70))


def petal_storm_icon():
    def draw(d, img, S):
        for k, (y, r) in enumerate(((.82, .14), (.66, .24), (.5, .34), (.32, .42))):
            d.arc(((.5 - r) * S, (y - r * .3) * S, (.5 + r) * S, (y + r * .3) * S), 0, 360, fill=_a((255, 200, 225), 110), width=max(1, S // 40))
        for k in range(16):
            t = k / 15
            a = t * math.tau * 2.2
            r = .14 + .3 * t
            y = .84 - .54 * t
            x = .5 + math.cos(a) * r
            yy = y + math.sin(a) * r * .3
            _petal(d, S, x, yy, .13 + .04 * t, .08, a + 1.7, (WHITE, PINK, ROSE)[k % 3], ROSE)
        g.glow_spot(img, .5 * S, .6 * S, .16 * S, (255, 190, 225), .5)
    return _icon(draw, .5, .55)


def thousand_petal_lotus_icon():
    def draw(d, img, S):
        for k in range(16):
            a = k * math.tau / 16
            d.line([(.5 * S, .5 * S), ((.5 + math.cos(a) * .5) * S, (.5 + math.sin(a) * .5) * S)], fill=_a(GOLD, 140), width=max(1, S // 48))
        g.glow_spot(img, .5 * S, .45 * S, .4 * S, GOLD, .9)
        for ring, (n, ln, col) in enumerate(((10, .4, ROSE), (8, .34, PINK), (6, .26, BLUSH), (5, .18, WHITE))):
            for k in range(n):
                a = math.pi + (k + .5) / n * math.pi + (ring % 2) * .1
                _petal(d, S, .5, .66, ln, ln * .5, a, col, ROSE)
        g.glow_spot(img, .5 * S, .56 * S, .1 * S, (255, 250, 220), 1)
        d.ellipse((.14 * S, .68 * S, .86 * S, .82 * S), fill=_a(JADE, 200))
    return _icon(draw, .5, .5, (90, 40, 80))


ICONS = (("razor_petals", razor_petals_icon), ("lotus_bud", lotus_bud_icon), ("sacred_pond", sacred_pond_icon), ("lotus_throne", lotus_throne_icon),
         ("mire_roots", mire_roots_icon), ("seed_pod", seed_pod_icon), ("lotus_mandala", lotus_mandala_icon), ("lotus_lanterns", lotus_lanterns_icon),
         ("petal_storm", petal_storm_icon), ("thousand_petal_lotus", thousand_petal_lotus_icon))


# --------------------------------------------------------------------------------------------------- the spellbook

COVER = (196, 92, 142)
COVER_LIGHT = (232, 140, 184)
COVER_DARK = (122, 44, 86)


def lotus_codex_icon():
    return g.from_grid([
        "................",
        "...oooooooooo...",
        "..obbbbbbbbbdco.",
        "..obGbbbbbbGdco.",
        "..obbbbwbbbbdco.",
        "..obbbwPwbbbdcco",
        "..obbwPPPwbbdco.",
        "..obwPPYPPwbdcco",
        "..obbwPPPwbbdcco",
        "..obgggggggbdcco",
        "..obbbbbbbbbdco.",
        "..oddGddddGdxco.",
        "..oddddddddddxco",
        "...ooooooooooo..",
        "................",
        "................",
    ], {"o": NIGHT, "b": COVER, "d": COVER_DARK, "x": (60, 20, 44), "G": GOLD, "w": WHITE, "P": PINK, "Y": GOLD, "g": LEAF, "c": g.CREAM})


def lotus_codex_model_texture():
    """64x64 texture of the 3D book, laid out like the other books: covers of rose silk framed in gold, a lotus in
    white and pink on a jade leaf on the front, gold corners, a gold spine, old pages."""
    s = 4
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    W, H = 12 * s, 9 * s
    grain = _tnoise(W, H, (6, 4), 1561)
    cover = _ramp(.3 + .4 * grain, [COVER_DARK, COVER, COVER_LIGHT])
    img.paste(_rgba(cover, np.full((H, W), 255)), (0, 0))
    d.rectangle((1, 1, W - 2, H - 2), outline=GOLD + (255,))
    cx, cy = W // 2, H // 2 + 3
    d.ellipse((cx - 13, cy + 2, cx + 13, cy + 8), fill=LEAF + (255,), outline=LEAF_DARK + (255,))
    for ang, ln, col in ((-150, 11, BLUSH), (-30, 11, BLUSH), (-120, 12, PINK), (-60, 12, PINK), (-90, 13, WHITE)):
        a = math.radians(ang)
        pts = []
        for side in (1, -1):
            rng = range(9) if side == 1 else range(8, -1, -1)
            for i in rng:
                t = i / 8
                w = 3.2 * math.sin(math.pi * t ** .8) ** .7 * side
                px, py = t * ln, w
                pts.append((cx + px * math.cos(a) - py * math.sin(a), cy + 3 + px * math.sin(a) + py * math.cos(a)))
        d.polygon(pts, fill=col + (255,), outline=ROSE + (255,))
    d.ellipse((cx - 2, cy - 1, cx + 2, cy + 3), fill=GOLD + (255,))
    for x, y in ((3, 3), (W - 6, 3), (3, H - 6), (W - 6, H - 6)):
        d.rectangle((x, y, x + 2, y + 2), fill=GOLD + (255,))
    d.rectangle((0, 0, W - 1, H - 1), outline=NIGHT + (255,))
    d.rectangle((12 * s, 0, 13 * s - 1, 9 * s - 1), fill=COVER_DARK + (255,))
    for y in range(0, 8 * s):
        for x in range(13 * s, 16 * s):
            img.putpixel((x, y), g.lerp(g.CREAM, (176, 168, 140), ((x * 7 + y * 13) % 11) / 22) + (255,))
    for y in range(1, 8 * s, 2):
        d.line((13 * s, y, 16 * s - 1, y), fill=g.CREAM_SHADE + (255,))
    for y in range(9 * s, 12 * s):
        for x in range(0, 12 * s):
            t = .5 + .5 * math.sin(x * .4)
            img.putpixel((x, y), g.lerp((180, 130, 50), GOLD, t) + (255,))
    d.rectangle((0, 12 * s, 12 * s - 1, 15 * s - 1), fill=g.CREAM + (255,))
    for x in range(0, 12 * s, 2):
        d.line((x, 12 * s, x, 15 * s - 1), fill=g.CREAM_SHADE + (255,))
    d.rectangle((12 * s, 9 * s, 14 * s - 1, 11 * s - 1), fill=GOLD + (255,))
    for y in range(9 * s, 11 * s):
        for x in range(14 * s, 16 * s):
            t = math.hypot(x - 14.8 * s, y - 9.8 * s) / (1.4 * s)
            img.putpixel((x, y), g.lerp(WHITE, PINK, min(1, t ** .8)) + (255,))
    d.rectangle((12 * s, 11 * s, 16 * s - 1, 12 * s - 1), fill=COVER_DARK + (255,))
    return img


def affinity_ring_lotus():
    return g.from_grid([
        "................",
        "......oooo......",
        ".....oPWWPo.....",
        ".....oWYYWo.....",
        "....oooooooo....",
        "...ohllllbbso...",
        "..ohlo....obso..",
        "..olo......oso..",
        "..ovo......oso..",
        "..olo......ovo..",
        "..obso....osso..",
        "...obbvbbssso...",
        "....oooooooo....",
        "................",
        "................",
        "................",
    ], {"o": NIGHT, "h": (255, 244, 220), "l": GOLD, "b": (210, 150, 60), "s": (130, 80, 30), "v": PINK, "P": ROSE, "W": PINK, "Y": GOLD})


def main():
    g.save(petal_texture(), "textures/entity/lotus/petal.png")
    g.save(leaf_texture(), "textures/entity/lotus/leaf.png")
    g.save(pod_texture(), "textures/entity/lotus/pod.png")
    g.save(fibre_texture(), "textures/entity/lotus/fibre.png")
    g.save(root_texture(), "textures/entity/lotus/root.png")
    g.save(mud_texture(), "textures/entity/lotus/mud.png")
    g.save(mandala_texture(), "textures/entity/lotus/mandala.png")
    for name, icon in ICONS:
        g.save(icon(), f"textures/gui/spell_icons/{name}.png")
    g.save(affinity_ring_lotus(), "textures/item/affinity_ring_lotus.png")
    g.write_json("models/item/affinity_ring_lotus.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/affinity_ring_lotus"}})
    g.save(lotus_codex_icon(), "textures/item/lotus_codex.png")
    g.save(lotus_codex_model_texture(), "textures/item/lotus_codex_3d.png")
    g.book_models("lotus_codex")


if __name__ == "__main__":
    main()
