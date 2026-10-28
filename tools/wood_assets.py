#!/usr/bin/env python3
"""Wood Magic art: the bark, vine, leaf, cut log end, split wood, shaving and amber textures the spells are drawn with
(and a streak of wind and a puff of dust), the spell icons, the Sylvan Codex
(icon and 3D book texture) and its affinity ring.

Called by gen_assets.py (run that one). Oak bark, spring-green leaves, the life in the wood glowing green. Original art.
"""
import math

import numpy as np
from PIL import Image, ImageDraw

import gen_assets as g
from bone_assets import _noise

BARK_DARK = (52, 34, 20)
BARK = (104, 72, 44)
BARK_LIGHT = (146, 106, 66)
BARK_HIGH = (184, 142, 96)
WOOD_PALE = (214, 178, 122)
OUTLINE = (30, 20, 12)
LEAF_DARK = (46, 104, 28)
LEAF = (98, 176, 52)
LEAF_LIGHT = (164, 226, 96)
LIFE = (143, 224, 74)
LIFE_CORE = (228, 255, 176)
PINK = (242, 166, 216)
NIGHT = (8, 18, 8)


def _ramp(t, stops):
    stops = np.array(stops, float)
    tt = np.clip(t, 0, 1) * (len(stops) - 1)
    i = np.minimum(tt.astype(int), len(stops) - 2)
    f = (tt - i)[..., None]
    return stops[i] * (1 - f) + stops[i + 1] * f


def bark_texture():
    """64 x 64, u round the wood, v along it: deep furrows running along it, ridges between, a little moss."""
    w = h = 64
    u = np.arange(w)[None, :] / w
    v = np.arange(h)[:, None] / h
    warp = _noise(w, h, (4, 3), 21) * .9
    ridges = .5 + .5 * np.sin((u * 7 + warp * .6 + .15 * np.sin(v * math.tau * 2)) * math.tau)
    fine = _noise(w, h, (32, 4), 22)
    blotch = _noise(w, h, (6, 5), 23)
    t = .2 + .55 * ridges ** 1.6 + .2 * (fine - .5) + .15 * (blotch - .5)
    rgb = _ramp(t, [BARK_DARK, BARK, BARK_LIGHT, BARK_HIGH])
    # Cross cracks here and there, moss in the deepest furrows
    rnd = np.random.default_rng(24)
    for _ in range(18):
        x, y, L = rnd.integers(0, w), rnd.integers(0, h), rnd.integers(2, 6)
        for k in range(L):
            rgb[y, (x + k) % w] = np.array(BARK_DARK) * .8
    moss = (_noise(w, h, (8, 8), 25) > .72) & (ridges < .25)
    rgb[moss] = rgb[moss] * .4 + np.array(LEAF_DARK) * .6
    return Image.fromarray(np.clip(rgb, 0, 255).astype(np.uint8), "RGB").convert("RGBA")


def vine_texture():
    """32 x 32, u round the stem, v along it: a green stem, lighter stripes along it, darker knots."""
    w = h = 32
    u = np.arange(w)[None, :] / w
    v = np.arange(h)[:, None] / h
    stripes = .5 + .5 * np.sin(u * math.tau * 4 + _noise(w, h, (4, 2), 31) * 2)
    t = .35 + .35 * stripes + .2 * (_noise(w, h, (16, 6), 32) - .5) + .25 * np.exp(-((u - .3) ** 2) / .01)
    rgb = _ramp(t, [LEAF_DARK, (72, 140, 40), LEAF, LEAF_LIGHT])
    knots = np.abs(((v * 4) % 1) - .5) < .04
    rgb[np.broadcast_to(knots, rgb.shape[:2])] *= .7
    return Image.fromarray(np.clip(rgb, 0, 255).astype(np.uint8), "RGB").convert("RGBA")


def leaf_texture():
    """32 x 32 leaf, pale (it is tinted in game), pointed, a vein down its middle and its side veins, a darker rim."""
    S = 256
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    pts = []
    for k in range(41):
        t = k / 40
        y = .06 + .88 * t
        wdt = .36 * math.sin(math.pi * t) ** .8 * (1 - .25 * t)
        pts.append((.5 + wdt, y))
    for k in range(40, -1, -1):
        t = k / 40
        y = .06 + .88 * t
        wdt = .36 * math.sin(math.pi * t) ** .8 * (1 - .25 * t)
        pts.append((.5 - wdt, y))
    d.polygon([(x * S, y * S) for x, y in pts], fill=(214, 214, 214, 255), outline=(150, 150, 150, 255))
    for k in range(5):
        y = .25 + k * .14
        for s in (-1, 1):
            d.line((.5 * S, y * S, (.5 + s * .22) * S, (y - .1) * S), fill=(170, 170, 170, 255), width=5)
    d.line((.5 * S, .08 * S, .5 * S, .97 * S), fill=(160, 160, 160, 255), width=8)
    # A light sheen down one side
    sheen = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    sd = ImageDraw.Draw(sheen)
    sd.ellipse((.32 * S, .2 * S, .48 * S, .7 * S), fill=(255, 255, 255, 70))
    img = Image.alpha_composite(img, Image.composite(sheen, Image.new("RGBA", (S, S), (0, 0, 0, 0)), img.split()[3]))
    small = img.resize((32, 32), Image.LANCZOS)
    a = np.array(small)
    a[..., 3] = np.where(a[..., 3] > 110, 255, 0)
    return Image.fromarray(a, "RGBA")


def log_end_texture():
    """64 x 64 cut end of a log, laid on a disc: rings (pale early wood, darker late wood) wavering round a dark heart,
    short cracks running in from a rim of bark."""
    S = 64
    ys, xs = np.mgrid[0:S, 0:S]
    dx, dy = (xs + .5) / S - .5, (ys + .5) / S - .5
    r = np.hypot(dx, dy) * 2
    a = np.arctan2(dy, dx)
    warp = .025 * np.sin(a * 3 + 1) + .015 * np.sin(a * 7) + (_noise(S, S, (6, 6), 41) - .5) * .05
    rings = r + warp
    band = (rings * 11) % 1
    late = np.clip((band - .72) / .2, 0, 1) * np.clip((1 - band) / .08, 0, 1)
    t = .75 - .45 * late + .12 * (_noise(S, S, (24, 24), 42) - .5) - .12 * r
    rgb = _ramp(t, [(120, 78, 40), (176, 124, 70), (214, 170, 110), (232, 196, 140)])
    heart = r < .08
    rgb[heart] = np.array((110, 70, 36))
    # Checks: short cracks running in from the bark, as a cut log dries
    for ca, r0 in ((.6, .5), (2.4, .62), (3.9, .45), (5.3, .66)):
        crack = (np.abs(((a - ca + np.pi) % (2 * np.pi)) - np.pi) < .028 / np.maximum(r, .1)) & (r > r0) & (r < .87)
        rgb[crack] = np.array((92, 58, 30))
    bark = r > .88
    ridge = .5 + .5 * np.sin(a * 22 + _noise(S, S, (8, 8), 43) * 3)
    rgb[bark] = _ramp(.25 + .4 * ridge[bark], [BARK_DARK, BARK, BARK_LIGHT])
    edge = (r > .86) & (r <= .9)
    rgb[edge] = np.array(BARK_DARK)
    return Image.fromarray(np.clip(rgb, 0, 255).astype(np.uint8), "RGB").convert("RGBA")


def grain_texture():
    """32 x 32 pale split wood (tinted in game for each wood), its grain running along v."""
    S = 32
    ys, xs = np.mgrid[0:S, 0:S]
    u = (xs + .5) / S
    wav = (_noise(S, S, (2, 4), 51) - .5) * .25
    lines = .5 + .5 * np.sin((u + wav) * math.tau * 5)
    t = .7 + .18 * lines ** 3 + .1 * (_noise(S, S, (16, 4), 52) - .5)
    rgb = _ramp(t, [(170, 150, 120), (215, 200, 172), (240, 230, 208), (252, 246, 232)])
    return Image.fromarray(np.clip(rgb, 0, 255).astype(np.uint8), "RGB").convert("RGBA")


def shaving_texture():
    """32 x 16 shaving, u along the strip: pale wood with its grain along it, darker thin edges, ragged."""
    W, H = 32, 16
    ys, xs = np.mgrid[0:H, 0:W]
    v = (ys + .5) / H
    lines = .5 + .5 * np.sin(v * math.tau * 3 + _noise(W, H, (4, 2), 61) * 2)
    t = .75 + .15 * lines + .1 * (_noise(W, H, (16, 4), 62) - .5) - .35 * (np.abs(v - .5) > .38)
    rgb = _ramp(t, [(150, 118, 80), (210, 190, 156), (238, 226, 200), (250, 244, 228)])
    alpha = np.where((np.abs(v - .5) < .44 + .06 * (_noise(W, H, (12, 2), 63) - .5)), 255, 0)
    out = np.dstack([np.clip(rgb, 0, 255), alpha]).astype(np.uint8)
    return Image.fromarray(out, "RGBA")


def wind_texture():
    """32 x 32 soft streak of wind: white, see-through at its edges (u across it), a little streaky along it."""
    S = 32
    ys, xs = np.mgrid[0:S, 0:S]
    u = (xs + .5) / S
    streak = .7 + .3 * _noise(S, S, (3, 6), 71)
    alpha = np.exp(-((u - .5) ** 2) / .03) * streak * 230
    out = np.dstack([np.full((S, S), 255), np.full((S, S), 255), np.full((S, S), 255), alpha]).astype(np.uint8)
    return Image.fromarray(out, "RGBA")


def dust_texture():
    """64 x 64 ring of stirred dust: white, cloudy, see-through at its middle and its rim."""
    S = 64
    ys, xs = np.mgrid[0:S, 0:S]
    r = np.hypot((xs + .5) / S - .5, (ys + .5) / S - .5) * 2
    cloud = .55 + .45 * _noise(S, S, (8, 8), 81)
    alpha = np.clip(np.exp(-((r - .62) ** 2) / .06), 0, 1) * cloud * 220 * (r < 1)
    out = np.dstack([np.full((S, S), 255), np.full((S, S), 255), np.full((S, S), 255), alpha]).astype(np.uint8)
    return Image.fromarray(out, "RGBA")


def amber_texture():
    """32 x 32 amber (its colour and see-through set in game): warm gold, flowing darker streaks, little bubbles."""
    S = 32
    ys, xs = np.mgrid[0:S, 0:S]
    u, v = (xs + .5) / S, (ys + .5) / S
    flow = .5 + .5 * np.sin((v * 3 + _noise(S, S, (3, 3), 91) * 1.5) * math.tau)
    t = .62 + .2 * flow + .12 * (_noise(S, S, (8, 8), 92) - .5)
    rgb = _ramp(t, [(200, 120, 40), (235, 170, 70), (255, 214, 120), (255, 240, 190)])
    rnd = np.random.default_rng(93)
    for _ in range(9):
        x, y = rnd.integers(1, S - 1), rnd.integers(1, S - 1)
        rgb[y, x] = (255, 250, 225)
        rgb[y, x + 1] = (255, 236, 180)
    return Image.fromarray(np.clip(rgb, 0, 255).astype(np.uint8), "RGB").convert("RGBA")


# ------------------------------------------------------------------------------------------------------- the icons


def _icon(draw, cx=.5, cy=.5):
    return g.painted(64, draw, (cx, cy, (34, 66, 26), NIGHT), 16)


def _rgba(c, a=255):
    return tuple(c) + (a,)


def _log(d, S, x0, y0, x1, y1, w, ring=True):
    """A log lying from (x0, y0) to (x1, y1), {w} thick, its cut end showing rings."""
    W = max(2, int(w * S))
    d.line((x0 * S, y0 * S, x1 * S, y1 * S), fill=_rgba(BARK), width=W)
    dx, dy = x1 - x0, y1 - y0
    L = math.hypot(dx, dy) or 1
    nx, ny = -dy / L, dx / L
    d.line(((x0 + nx * w * .25) * S, (y0 + ny * w * .25) * S, (x1 + nx * w * .25) * S, (y1 + ny * w * .25) * S), fill=_rgba(BARK_LIGHT), width=max(1, W // 4))
    d.line(((x0 - nx * w * .3) * S, (y0 - ny * w * .3) * S, (x1 - nx * w * .3) * S, (y1 - ny * w * .3) * S), fill=_rgba(BARK_DARK), width=max(1, W // 5))
    if ring:
        r = w * .55
        d.ellipse(((x1 - r) * S, (y1 - r) * S, (x1 + r) * S, (y1 + r) * S), fill=_rgba(WOOD_PALE), outline=_rgba(BARK_DARK), width=max(1, S // 48))
        d.ellipse(((x1 - r * .55) * S, (y1 - r * .55) * S, (x1 + r * .55) * S, (y1 + r * .55) * S), outline=_rgba(BARK_LIGHT), width=max(1, S // 64))


def _leaf(d, S, x, y, size, angle, color=LEAF):
    ca, sa = math.cos(angle), math.sin(angle)
    pts = []
    for k in range(9):
        t = k / 8
        wdt = .4 * math.sin(math.pi * t)
        pts.append((t, wdt))
    pts += [(t, -wdt) for t, wdt in reversed(pts)]
    poly = [((x + (px * ca - py * sa) * size) * S, (y + (px * sa + py * ca) * size) * S) for px, py in pts]
    d.polygon(poly, fill=_rgba(color), outline=_rgba(g.lerp(color, OUTLINE, .5)))
    d.line((x * S, y * S, (x + ca * size * .9) * S, (y + sa * size * .9) * S), fill=_rgba(g.lerp(color, OUTLINE, .3)), width=max(1, S // 64))


def _crown(d, S, cx, cy, r, seed, pink=True):
    rnd = np.random.default_rng(seed)
    for k in range(9):
        a = rnd.random() * math.tau
        rr = r * (.35 + .4 * rnd.random())
        x, y = cx + math.cos(a) * rr * .8, cy + math.sin(a) * rr * .6
        q = r * (.45 + .25 * rnd.random())
        col = g.lerp(LEAF_DARK, LEAF_LIGHT, rnd.random())
        d.ellipse(((x - q) * S, (y - q) * S, (x + q) * S, (y + q) * S), fill=_rgba(col), outline=_rgba(LEAF_DARK))
    if pink:
        for k in range(4):
            a = rnd.random() * math.tau
            x, y = cx + math.cos(a) * r * .5, cy + math.sin(a) * r * .35
            q = r * .1
            d.ellipse(((x - q) * S, (y - q) * S, (x + q) * S, (y + q) * S), fill=_rgba(PINK))


def _ground(d, S, y=.86):
    d.rectangle((0, y * S, S, S), fill=(34, 26, 14, 255))
    d.line((0, y * S, S, y * S), fill=_rgba(LIFE), width=max(1, S // 32))


def _thorny(d, S, pts, w):
    d.line([(x * S, y * S) for x, y in pts], fill=_rgba((72, 140, 40)), width=max(2, int(w * S)), joint="curve")
    for i in range(1, len(pts) - 1, 2):
        x, y = pts[i]
        d.polygon([((x - .015) * S, y * S), ((x + .015) * S, y * S), ((x + .02) * S, (y - .06) * S)], fill=_rgba((110, 82, 54)))


def _rings(d, S, cx, cy, r):
    d.ellipse(((cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r) * S), fill=_rgba(WOOD_PALE), outline=_rgba(BARK_DARK), width=max(1, S // 32))
    for k in (.72, .48, .25):
        d.ellipse(((cx - r * k) * S, (cy - r * k) * S, (cx + r * k) * S, (cy + r * k) * S), outline=_rgba(BARK_LIGHT), width=max(1, S // 64))


def log_hurl_icon():
    def draw(d, img, S):
        _ground(d, S, .84)
        for k in range(4):
            y = .5 + k * .08
            d.line((.04 * S, y * S, (.24 + .05 * k) * S, y * S), fill=_rgba(WOOD_PALE, 110 + 30 * k), width=max(1, S // 28))
        # A log rolling towards the viewer's right: its body and its ringed end
        d.rounded_rectangle((.3 * S, .44 * S, .84 * S, .82 * S), radius=S // 8, fill=_rgba(BARK), outline=_rgba(BARK_DARK), width=max(1, S // 40))
        for x in (.42, .56, .7):
            d.line((x * S, .46 * S, (x - .03) * S, .8 * S), fill=_rgba(BARK_DARK), width=max(1, S // 48))
        d.line((.36 * S, .52 * S, .8 * S, .5 * S), fill=_rgba(LEAF), width=max(1, S // 40))
        _rings(d, S, .78, .63, .19)
        for x in (.36, .55):
            d.ellipse(((x - .03) * S, .86 * S, (x + .05) * S, .92 * S), fill=_rgba((110, 90, 70)))
    return _icon(draw, .6, .55)


def vine_lash_icon():
    def draw(d, img, S):
        pts = [(.12 + .76 * t, .82 - .62 * t + .1 * math.sin(t * math.pi * 3)) for t in [k / 14 for k in range(15)]]
        _thorny(d, S, pts, .05)
        for i in (3, 7, 11):
            x, y = pts[i]
            _leaf(d, S, x, y, .11, -1.2 + i * .4, LEAF_LIGHT if i % 2 else LEAF)
        # The catch: coils round a figure
        d.ellipse((.74 * S, .08 * S, .9 * S, .22 * S), fill=(150, 110, 110, 255))
        d.rectangle((.77 * S, .2 * S, .87 * S, .42 * S), fill=(130, 96, 96, 255))
        for k in range(3):
            y = .24 + k * .06
            d.arc((.72 * S, (y - .03) * S, .92 * S, (y + .04) * S), 0, 180, fill=_rgba((72, 140, 40)), width=max(1, S // 24))
    return _icon(draw, .5, .5)


def ring_saw_icon():
    def draw(d, img, S):
        d.arc((.04 * S, .1 * S, .9 * S, .96 * S), 120, 230, fill=_rgba(LIFE, 160), width=max(1, S // 32))
        cx, cy, r = .56, .48, .3
        for k in range(14):
            a = k * math.tau / 14
            b = a - .4
            d.polygon([((cx + math.cos(a - .14) * r) * S, (cy + math.sin(a - .14) * r) * S), ((cx + math.cos(a + .14) * r) * S, (cy + math.sin(a + .14) * r) * S),
                       ((cx + math.cos(b) * (r + .12)) * S, (cy + math.sin(b) * (r + .12)) * S)], fill=_rgba((94, 58, 38)))
        d.ellipse(((cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r) * S), fill=_rgba(BARK), outline=_rgba(BARK_DARK))
        _rings(d, S, cx, cy, r * .86)
        g.glow_spot(img, cx * S, cy * S, S * .06, LIFE, 1.2)
    return _icon(draw, .5, .5)


def splinter_storm_icon():
    def draw(d, img, S):
        d.ellipse((.2 * S, .84 * S, .8 * S, .96 * S), fill=(120, 100, 76, 180))
        for k in range(5):
            y = .18 + k * .15
            w = .38 - k * .06
            d.arc(((.5 - w) * S, (y - .04) * S, (.5 + w) * S, (y + .05) * S), 200, 520, fill=(216, 196, 160, 170), width=max(1, S // 40))
        rnd = np.random.default_rng(5)
        for k in range(26):
            t = rnd.random()
            w = .38 - t * .3
            a = rnd.random() * math.tau
            x, y = .5 + math.cos(a) * w * (1 - .7 * t + .6), .2 + t * .66
            x = .5 + (x - .5) * .9
            col = [(244, 234, 210), (232, 196, 140), (154, 112, 72), (192, 138, 88)][k % 4]
            if k % 3 == 0:
                d.arc(((x - .04) * S, (y - .03) * S, (x + .04) * S, (y + .03) * S), 0, 300, fill=_rgba(col), width=max(1, S // 40))
            else:
                dx, dy = math.cos(a + k) * .04, math.sin(a + k) * .025
                d.line(((x - dx) * S, (y - dy) * S, (x + dx) * S, (y + dy) * S), fill=_rgba(col), width=max(1, S // 28))
    return _icon(draw, .5, .5)


def amber_prison_icon():
    def draw(d, img, S):
        _ground(d, S, .88)
        d.ellipse((.2 * S, .82 * S, .8 * S, .94 * S), fill=(220, 140, 40, 200))
        d.ellipse((.42 * S, .26 * S, .58 * S, .42 * S), fill=(140, 100, 100, 255))
        d.rectangle((.44 * S, .4 * S, .56 * S, .84 * S), fill=(120, 88, 88, 255))
        amber = Image.new("RGBA", img.size, (0, 0, 0, 0))
        ad = ImageDraw.Draw(amber)
        ad.ellipse((.2 * S, .14 * S, .8 * S, .9 * S), fill=(236, 150, 34, 150), outline=(255, 214, 120, 230), width=max(1, S // 32))
        ad.ellipse((.28 * S, .2 * S, .42 * S, .4 * S), fill=(255, 236, 180, 110))
        img.alpha_composite(amber)
        d2 = ImageDraw.Draw(img)
        for x, y in ((.3, .62), (.68, .5), (.62, .76)):
            d2.ellipse(((x - .02) * S, (y - .02) * S, (x + .02) * S, (y + .02) * S), fill=(255, 246, 220, 255))
    return _icon(draw, .5, .55)


def grasping_roots_icon():
    def draw(d, img, S):
        _ground(d, S, .88)
        d.ellipse((.4 * S, .26 * S, .6 * S, .44 * S), fill=(140, 100, 100, 255))
        d.rectangle((.42 * S, .42 * S, .58 * S, .86 * S), fill=(120, 86, 86, 255))
        for k in range(3):
            pts = []
            for i in range(14):
                t = i / 13
                a = k * 2.1 + t * 7
                pts.append((.5 + math.cos(a) * (.16 - .03 * t), .88 - t * .5 + math.sin(a) * .03))
            d.line([(x * S, y * S) for x, y in pts], fill=_rgba(BARK_LIGHT if k == 1 else BARK), width=max(2, S // 16), joint="curve")
    return _icon(draw, .5, .6)


def seed_pods_icon():
    def draw(d, img, S):
        for k, (x, y) in enumerate(((.22, .62), (.5, .36), (.78, .6))):
            d.arc(((x - .25) * S, (y - .05) * S, (x + .05) * S, (y + .4) * S), 200, 290, fill=_rgba(LIFE, 140), width=max(1, S // 40))
            d.ellipse(((x - .1) * S, (y - .06) * S, (x + .1) * S, (y + .18) * S), fill=_rgba(WOOD_PALE), outline=_rgba(BARK_DARK))
            d.chord(((x - .12) * S, (y - .1) * S, (x + .12) * S, (y + .08) * S), 180, 360, fill=_rgba(BARK), outline=_rgba(BARK_DARK))
            d.line((x * S, (y - .1) * S, (x + .03) * S, (y - .16) * S), fill=_rgba(BARK_DARK), width=max(1, S // 40))
            d.line((x * S, (y + .0) * S, x * S, (y + .16) * S), fill=_rgba(LIFE_CORE), width=max(1, S // 40))
    return _icon(draw, .5, .5)


def bramble_hedge_icon():
    def draw(d, img, S):
        _ground(d, S, .84)
        for k in range(5):
            x0 = .05 + k * .2
            pts = [(x0 + t * .32, .86 - math.sin(t * math.pi) * (.42 + .1 * (k % 2))) for t in [i / 10 for i in range(11)]]
            _thorny(d, S, pts, .045)
        for x, y in ((.28, .5), (.62, .46), (.86, .56)):
            d.ellipse(((x - .035) * S, (y - .035) * S, (x + .035) * S, (y + .035) * S), fill=_rgba(PINK))
    return _icon(draw, .5, .7)


def wood_guardian_icon():
    def draw(d, img, S):
        d.rounded_rectangle((.26 * S, .34 * S, .74 * S, .96 * S), radius=S // 10, fill=_rgba(BARK), outline=_rgba(BARK_DARK), width=max(1, S // 40))
        for x in (.34, .5, .64):
            d.line((x * S, .72 * S, (x + .02) * S, .94 * S), fill=_rgba(BARK_DARK), width=max(1, S // 40))
        _crown(d, S, .5, .2, .2, 9)
        # Deep sockets, green eyes glowing in them
        for s in (-1, 1):
            ex, ey = .5 + s * .12, .52
            d.ellipse(((ex - .08) * S, (ey - .06) * S, (ex + .08) * S, (ey + .06) * S), fill=_rgba(BARK_DARK))
            g.glow_spot(img, ex * S, ey * S, S * .1, LIFE, 1.6)
            d.ellipse(((ex - .045) * S, (ey - .03) * S, (ex + .045) * S, (ey + .03) * S), fill=_rgba(LIFE_CORE))
        d.line((.38 * S, .68 * S, .62 * S, .7 * S), fill=_rgba(BARK_DARK), width=max(1, S // 28))
        for k in range(5):
            x = .4 + k * .05
            d.line((x * S, .7 * S, (x - .01) * S, (.82 + .04 * (k % 2)) * S), fill=_rgba(LEAF), width=max(1, S // 40))
    return _icon(draw, .5, .5)


def world_tree_icon():
    def draw(d, img, S):
        g.glow_spot(img, .5 * S, .5 * S, S * .5, (40, 90, 20), 1)
        _ground(d, S, .9)
        for k in range(7):
            a = math.pi * (.08 + .84 * k / 6)
            d.line((.5 * S, .84 * S, (.5 - math.cos(a) * .47) * S, (.93 + math.sin(a) * .02) * S), fill=_rgba(BARK), width=max(2, S // 22))
        for s in (-1, 0, 1):
            d.line(((.5 + s * .07) * S, .92 * S, (.5 - s * .05) * S, .36 * S), fill=_rgba(BARK if s else BARK_LIGHT), width=max(2, S // 10))
        for k in range(5):
            a = math.pi * (1.1 + .8 * k / 4)
            d.line((.5 * S, .42 * S, (.5 + math.cos(a) * .34) * S, (.34 + math.sin(a) * .14) * S), fill=_rgba(BARK), width=max(1, S // 20))
        _crown(d, S, .5, .2, .3, 11)
        for x in (.18, .82):
            _crown(d, S, x, .3, .13, 12 + int(x * 10))
        g.glow_spot(img, .5 * S, .7 * S, S * .1, LIFE, 1.5)
    return _icon(draw, .5, .4)


ICONS = (("log_hurl", log_hurl_icon), ("grasping_roots", grasping_roots_icon), ("seed_pods", seed_pods_icon),
         ("bramble_hedge", bramble_hedge_icon), ("vine_lash", vine_lash_icon), ("ring_saw", ring_saw_icon),
         ("splinter_storm", splinter_storm_icon), ("amber_prison", amber_prison_icon), ("wood_guardian", wood_guardian_icon),
         ("world_tree", world_tree_icon))


# --------------------------------------------------------------------------------------------------- the spellbook


def sylvan_codex_icon():
    return g.from_grid([
        "................",
        "...oooooooooo...",
        "..obbbbbbbbbdco.",
        "..obhhhhhhhbdco.",
        "..obhbbgbbhbdco.",
        "..obhbgGgbhbdcco",
        "..obhgGLGghbdco.",
        "..obhbgGgbhbdcco",
        "..obhbbsbbhbdcco",
        "..obhhhhhhhbdcco",
        "..obbbbbbbbbdco.",
        "..oddddvddddxco.",
        "..oddvvddddvxco.",
        "...ooooooooooo..",
        "................",
        "................",
    ], {"o": OUTLINE, "b": BARK, "h": BARK_LIGHT, "d": BARK_DARK, "x": (40, 26, 14), "g": LEAF, "G": LEAF_LIGHT,
        "L": LIFE_CORE, "s": BARK_HIGH, "v": LEAF_DARK, "c": g.CREAM})


def sylvan_codex_model_texture():
    """64x64 texture of the 3D book, laid out like the other books: covers of bark framed in vines, a carved leaf with a
    green gem in the middle, a spine wound with vines, old pages."""
    s = 4
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    W, H = 12 * s, 9 * s
    img.paste(bark_texture().resize((W, H)), (0, 0))
    vine = (72, 140, 40)
    for x in range(0, W, 2):
        y = 1 + int(1.2 * math.sin(x * .5))
        d.point((x, y), fill=vine + (255,))
        d.point((x, H - 2 - int(1.2 * math.sin(x * .5 + 1))), fill=vine + (255,))
    for x in range(3, W, 8):
        d.ellipse((x, 0, x + 2, 2), fill=LEAF_LIGHT + (255,))
        d.ellipse((x + 3, H - 3, x + 5, H - 1), fill=PINK + (255,))
    cx, cy = W // 2, H // 2
    d.polygon([(cx, cy - 11), (cx + 7, cy - 2), (cx + 4, cy + 7), (cx, cy + 10), (cx - 4, cy + 7), (cx - 7, cy - 2)], fill=BARK_DARK + (255,))
    d.polygon([(cx, cy - 9), (cx + 5, cy - 2), (cx + 3, cy + 5), (cx, cy + 8), (cx - 3, cy + 5), (cx - 5, cy - 2)], fill=LEAF + (255,), outline=LEAF_DARK + (255,))
    d.line((cx, cy - 8, cx, cy + 8), fill=LEAF_DARK + (255,))
    for k in (-4, 0, 4):
        d.line((cx, cy + k, cx - 3, cy + k - 3), fill=LEAF_DARK + (255,))
        d.line((cx, cy + k, cx + 3, cy + k - 3), fill=LEAF_DARK + (255,))
    d.ellipse((cx - 2, cy - 2, cx + 2, cy + 2), fill=LIFE_CORE + (255,), outline=LIFE + (255,))
    d.rectangle((0, 0, W - 1, H - 1), outline=OUTLINE + (255,))
    d.rectangle((12 * s, 0, 13 * s - 1, 9 * s - 1), fill=BARK_DARK + (255,))
    for y in range(0, 8 * s):
        for x in range(13 * s, 16 * s):
            img.putpixel((x, y), g.lerp(g.CREAM, (176, 168, 140), ((x * 7 + y * 13) % 11) / 22) + (255,))
    for y in range(1, 8 * s, 2):
        d.line((13 * s, y, 16 * s - 1, y), fill=g.CREAM_SHADE + (255,))
    # The spine: bark wound with a vine
    img.paste(bark_texture().resize((12 * s, 3 * s)), (0, 9 * s))
    for x in range(0, 12 * s):
        y = 9 * s + 6 + int(4 * math.sin(x * .4))
        d.point((x, y), fill=vine + (255,))
        d.point((x, y + 1), fill=LEAF_DARK + (255,))
    d.rectangle((0, 12 * s, 12 * s - 1, 15 * s - 1), fill=g.CREAM + (255,))
    for x in range(0, 12 * s, 2):
        d.line((x, 12 * s, x, 15 * s - 1), fill=g.CREAM_SHADE + (255,))
    d.rectangle((12 * s, 9 * s, 14 * s - 1, 11 * s - 1), fill=BARK_LIGHT + (255,))
    for y in range(9 * s, 11 * s):
        for x in range(14 * s, 16 * s):
            t = math.hypot(x - 14.8 * s, y - 9.8 * s) / (1.4 * s)
            img.putpixel((x, y), g.lerp(LIFE_CORE, LEAF_DARK, min(1, t ** .8)) + (255,))
    d.rectangle((12 * s, 11 * s, 16 * s - 1, 12 * s - 1), fill=BARK_DARK + (255,))
    return img


def affinity_ring_wood():
    return g.from_grid([
        "................",
        "......oooo......",
        ".....oxGGxo.....",
        ".....oGCCGo.....",
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
    ], {"o": OUTLINE, "h": BARK_HIGH, "l": BARK_LIGHT, "b": BARK, "s": BARK_DARK, "v": LEAF,
        "G": LIFE, "C": LIFE_CORE, "x": LEAF_DARK})


def main():
    g.save(bark_texture(), "textures/entity/wood/bark.png")
    g.save(vine_texture(), "textures/entity/wood/vine.png")
    g.save(leaf_texture(), "textures/entity/wood/leaf.png")
    g.save(log_end_texture(), "textures/entity/wood/log_end.png")
    g.save(grain_texture(), "textures/entity/wood/grain.png")
    g.save(shaving_texture(), "textures/entity/wood/shaving.png")
    g.save(wind_texture(), "textures/entity/wood/wind.png")
    g.save(dust_texture(), "textures/entity/wood/dust.png")
    g.save(amber_texture(), "textures/entity/wood/amber.png")
    for name, icon in ICONS:
        g.save(icon(), f"textures/gui/spell_icons/{name}.png")
    g.save(affinity_ring_wood(), "textures/item/affinity_ring_wood.png")
    g.write_json("models/item/affinity_ring_wood.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/affinity_ring_wood"}})
    g.save(sylvan_codex_icon(), "textures/item/sylvan_codex.png")
    g.save(sylvan_codex_model_texture(), "textures/item/sylvan_codex_3d.png")
    g.book_models("sylvan_codex")


if __name__ == "__main__":
    main()
