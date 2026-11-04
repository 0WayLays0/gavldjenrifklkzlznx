#!/usr/bin/env python3
"""Aurora Magic art: the rays of the northern lights (curtains), a band of light (ribbons), the night sky (the dome of
the Boreal Night), the spell icons, the Boreal Codex (icon, a finer 3D book texture and raised ornaments: a star of
ice on each cover, crystal finials on the spine) and its affinity ring.

Called by gen_assets.py (run that one). Original art, drawn here from noise and strokes.
"""
import math
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

import book_ornaments as bo
import gen_assets as g
from water_assets import _tnoise, _rgba, _ramp

GREEN = (70, 255, 160)
TEAL = (63, 232, 224)
BLUE = (90, 140, 255)
VIOLET = (164, 107, 255)
ROSE = (255, 106, 208)
ICE = (216, 244, 255)
NIGHT = (11, 22, 64)
DEEP = (5, 9, 30)
SILVER = (214, 226, 240)
SILVER_DARK = (120, 138, 170)


def hue(t):
    """The aurora's colour {t} of the way up a curtain (0 green, 1 rose)."""
    stops = [(0, GREEN), (.35, TEAL), (.6, BLUE), (.82, VIOLET), (1, ROSE)]
    t = max(0.0, min(1.0, t))
    for (a, ca), (b, cb) in zip(stops, stops[1:]):
        if t <= b:
            k = (t - a) / (b - a)
            return tuple(int(ca[i] + (cb[i] - ca[i]) * k) for i in range(3))
    return ROSE


# ------------------------------------------------------------------------------------------------------ textures

def rays_texture():
    """128 x 64, tiling across: the rays of a curtain of light, white (tinted in game): streaks rising from a bright
    hem along the bottom, fading toward the top, brighter rays here and there."""
    W, H = 128, 64
    ys, xs = np.mgrid[0:H, 0:W].astype(np.float32)
    t = (ys + .5) / H
    streak = .55 * _tnoise(W, H, (32, 1), 1701) + .3 * _tnoise(W, H, (64, 2), 1702) + .15 * _tnoise(W, H, (16, 1), 1703)
    bright = np.clip((_tnoise(W, H, (8, 1), 1704) - .55) * 4, 0, 1)
    rays = np.clip(streak * (.35 + .9 * bright), 0, 1) * (.62 + .38 * t ** .9) * np.clip(t * 10, 0, 1)
    hem = np.exp(-((1 - t) / .05) ** 2) * (.7 + .3 * _tnoise(W, H, (24, 1), 1705))
    v = np.clip(rays + hem * .9, 0, 1)
    return _rgba(np.dstack([v * 255] * 3), np.full((H, W), 255.0))


def veil_texture():
    """128 x 64, tiling across: the rays of rays_texture() as a veil: white, as opaque as the rays are bright (tinted in
    game and laid over what is behind a curtain, so its colours show against a bright sky)."""
    rays = np.asarray(rays_texture(), dtype=np.float32)[..., 0]
    a = np.clip(rays * 1.3, 0, 255)
    return _rgba(np.full(rays.shape + (3,), 255.0), a)


def band_texture():
    """32 x 64, tiling along: a band of light, bright down its middle, soft to its edges, shimmering along it."""
    W, H = 32, 64
    ys, xs = np.mgrid[0:H, 0:W].astype(np.float32)
    u = (xs + .5) / W * 2 - 1
    shimmer = .75 + .25 * _tnoise(W, H, (4, 8), 1711)
    v = np.exp(-(u / .45) ** 2) * shimmer + .6 * np.exp(-(u / .12) ** 2)
    v = np.clip(v, 0, 1)
    return _rgba(np.dstack([v * 255] * 3), np.full((H, W), 255.0))


def night_texture():
    """128 x 128: the night sky laid round a dome (U round it, V from the top (0) to the horizon (.5) and below): deep
    blue at the top, paler toward the horizon, a faint milky band, stars."""
    W, H = 128, 128
    ys, xs = np.mgrid[0:H, 0:W].astype(np.float32)
    v = (ys + .5) / H
    t = np.clip(v / .5, 0, 1)
    col = _ramp(t, [DEEP, NIGHT, (24, 52, 96), (40, 86, 110)])
    milk = np.exp(-((v - .22 - .08 * np.sin(xs / W * math.tau)) / .05) ** 2) * (.5 + .5 * _tnoise(W, H, (12, 6), 1721))
    col = col + 40 * milk[..., None]
    return _rgba(np.clip(col, 0, 255), np.full((H, W), 255.0))


# ------------------------------------------------------------------------------------------------------- icons

def _icon(draw, cx=.5, cy=.5, bg=(18, 32, 80)):
    return g.painted(64, draw, (cx, cy, bg, DEEP), 16)


def _a(c, a=255):
    return tuple(int(x) for x in c) + (a,)


def _curtain(d, S, x0, x1, y_hem, height, n=12, sway=.04):
    for i in range(n):
        x = x0 + (x1 - x0) * (i + .5) / n
        top = y_hem - height * (.7 + .3 * math.sin(i * 1.7))
        steps = 8
        for k in range(steps):
            ya = y_hem - (y_hem - top) * k / steps
            yb = y_hem - (y_hem - top) * (k + 1) / steps
            xx = x + sway * math.sin(i * .8 + k * .5)
            c = hue(k / steps)
            d.line([(xx * S, ya * S), (xx * S, yb * S)], fill=_a(c, int(255 * (1 - k / steps * .7))), width=max(1, int(S / n * .8)))


def _star(d, S, x, y, r, c=(255, 255, 255)):
    lw = max(1, S // 48)
    d.line(((x - r) * S, y * S, (x + r) * S, y * S), fill=_a(c), width=lw)
    d.line((x * S, (y - r) * S, x * S, (y + r) * S), fill=_a(c), width=lw)


def _ice(d, S, x, y, w, h, angle):
    ca, sa = math.cos(angle), math.sin(angle)

    def P(u, v):
        return ((x + u * ca + v * sa) * S, (y + u * sa - v * ca) * S)
    d.polygon([P(-w / 2, 0), P(w / 2, 0), P(w / 2, h * .75), P(0, h), P(-w / 2, h * .75)], fill=_a(ICE), outline=_a((90, 130, 180)))
    d.polygon([P(-w / 2, 0), P(-w * .1, 0), P(-w * .1, h * .8), P(0, h), P(-w / 2, h * .75)], fill=_a((255, 255, 255)))


def _ribbon(d, S, pts, width, t0=0.0):
    for i in range(len(pts) - 1):
        c = hue((t0 + i / len(pts)) % 1)
        d.line([(pts[i][0] * S, pts[i][1] * S), (pts[i + 1][0] * S, pts[i + 1][1] * S)], fill=_a(c), width=max(1, int(S * width * (1 - .5 * i / len(pts)))))


def aurora_lance_icon():
    def draw(d, img, S):
        pts = [(.1 + .55 * t, .9 - .55 * t + .06 * math.sin(t * 9)) for t in [i / 14 for i in range(15)]]
        _ribbon(d, S, pts, .09)
        _ice(d, S, .62, .4, .12, .42, .78)
        g.glow_spot(img, .78 * S, .22 * S, .14 * S, (200, 250, 255), .8)
    return _icon(draw)


def polar_wisps_icon():
    def draw(d, img, S):
        for k, (x, y) in enumerate(((.3, .3), (.72, .42), (.42, .72))):
            pts = [(x - .25 * t, y + .2 * t + .05 * math.sin(t * 8 + k)) for t in [i / 8 for i in range(9)]]
            _ribbon(d, S, pts, .07, k * .3)
            g.glow_spot(img, x * S, y * S, .13 * S, hue(k * .35), 1)
            d.ellipse(((x - .04) * S, (y - .04) * S, (x + .04) * S, (y + .04) * S), fill=_a((255, 255, 255)))
    return _icon(draw)


def borealis_curtain_icon():
    def draw(d, img, S):
        _curtain(d, S, .06, .94, .82, .72, 14)
        d.rectangle((0, .84 * S, S, S), fill=_a((30, 50, 70)))
        d.line((.06 * S, .82 * S, .94 * S, .82 * S), fill=_a(GREEN), width=max(1, S // 20))
        _star(d, S, .2, .14, .05)
        _star(d, S, .82, .1, .04)
    return _icon(draw, .5, .5, (12, 24, 60))


def aurora_glide_icon():
    def draw(d, img, S):
        for k in range(3):
            pts = [(.08 + .84 * t, .7 - .3 * t + .1 * math.sin(t * 7 + k) - k * .06) for t in [i / 14 for i in range(15)]]
            _ribbon(d, S, pts, .06, k * .25)
        d.polygon([(.95 * S, .3 * S), (.78 * S, .22 * S), (.82 * S, .4 * S)], fill=_a((255, 255, 255)))
    return _icon(draw)


def aurora_mantle_icon():
    def draw(d, img, S):
        d.ellipse((.42 * S, .2 * S, .58 * S, .36 * S), fill=_a((70, 80, 120)))
        d.rounded_rectangle((.4 * S, .36 * S, .6 * S, .82 * S), radius=int(.05 * S), fill=_a((70, 80, 120)))
        _curtain(d, S, .12, .4, .86, .4, 5, .02)
        _curtain(d, S, .6, .88, .86, .4, 5, .02)
        d.arc((.1 * S, .74 * S, .9 * S, .98 * S), 0, 360, fill=_a(GREEN), width=max(1, S // 24))
    return _icon(draw)


def boreal_blessing_icon():
    def draw(d, img, S):
        _curtain(d, S, .05, .95, .5, .45, 14)
        g.glow_spot(img, .5 * S, .7 * S, .25 * S, (180, 255, 220), 1)
        d.rectangle((.44 * S, .58 * S, .56 * S, .9 * S), fill=_a((255, 255, 255)))
        d.rectangle((.32 * S, .68 * S, .68 * S, .8 * S), fill=_a((255, 255, 255)))
    return _icon(draw)


def celestial_spiral_icon():
    def draw(d, img, S):
        for k in range(4):
            pts = []
            for i in range(22):
                t = i / 21
                a = k * math.pi / 2 + t * 7
                r = .08 + .32 * t
                pts.append((.5 + math.cos(a) * r, .85 - .7 * t + math.sin(a) * r * .25))
            _ribbon(d, S, pts, .05, k * .2)
        g.glow_spot(img, .5 * S, .55 * S, .16 * S, (180, 255, 240), .6)
    return _icon(draw)


def aurora_serpent_icon():
    def draw(d, img, S):
        pts = [(.12 + .7 * t, .78 - .25 * math.sin(t * math.pi * 1.6) - .25 * t) for t in [i / 24 for i in range(25)]]
        pts.reverse()
        _ribbon(d, S, pts, .14)
        hx, hy = pts[0]
        _ice(d, S, hx, hy, .1, .2, .9)
        _ice(d, S, hx - .04, hy - .02, .05, .16, -.5)
        d.ellipse(((hx - .02) * S, (hy - .02) * S, (hx + .02) * S, (hy + .02) * S), fill=_a((255, 255, 255)))
        _star(d, S, .2, .18, .05)
    return _icon(draw, .5, .5, (12, 24, 60))


def polar_halo_icon():
    def draw(d, img, S):
        for k in range(5):
            x = .22 + .14 * k
            d.line((x * S, .3 * S, (x + .02) * S, .92 * S), fill=_a(hue(k / 4), 200), width=max(1, S // 24))
        d.ellipse((.1 * S, .14 * S, .9 * S, .42 * S), outline=_a((255, 255, 255)), width=max(2, S // 16))
        d.ellipse((.1 * S, .14 * S, .9 * S, .42 * S), outline=_a(TEAL), width=max(1, S // 40))
        for x in (.18, .5, .82):
            _ice(d, S, x, .16 if x == .5 else .24, .05, .14, 0)
    return _icon(draw, .5, .3)


def boreal_night_icon():
    def draw(d, img, S):
        d.pieslice((-.05 * S, .1 * S, 1.05 * S, 1.6 * S), 180, 360, fill=_a(DEEP), outline=_a(SILVER_DARK))
        _curtain(d, S, .12, .88, .62, .38, 12, .05)
        rnd = random.Random(3)
        for _ in range(12):
            _star(d, S, .1 + rnd.random() * .8, .2 + rnd.random() * .3, .02)
        d.rectangle((0, .84 * S, S, S), fill=_a((20, 30, 50)))
        g.glow_spot(img, .5 * S, .8 * S, .1 * S, (220, 255, 255), 1)
        d.line((.5 * S, .2 * S, .5 * S, .84 * S), fill=_a((230, 255, 250)), width=max(1, S // 22))
    return _icon(draw, .5, .4, (8, 14, 40))


ICONS = (("aurora_lance", aurora_lance_icon), ("polar_wisps", polar_wisps_icon), ("borealis_curtain", borealis_curtain_icon),
         ("aurora_glide", aurora_glide_icon), ("aurora_mantle", aurora_mantle_icon), ("boreal_blessing", boreal_blessing_icon),
         ("celestial_spiral", celestial_spiral_icon), ("aurora_serpent", aurora_serpent_icon), ("polar_halo", polar_halo_icon),
         ("boreal_night", boreal_night_icon))


# --------------------------------------------------------------------------------------------------- the spellbook

def boreal_codex_icon():
    return g.from_grid([
        "................",
        "...oooooooooo...",
        "..onnnnnnnnnsco.",
        "..onGTTVVRRnsco.",
        "..onGGTTVVRnscco",
        "..onnGGTTVnnsco.",
        "..onnnIInnnnscco",
        "..onnIWWInnnscco",
        "..onnnIInnnnsco.",
        "..onmmmnnmmnscco",
        "..ommmmmmmmmsco.",
        "..oddsdddsddxco.",
        "..odddddddddxcco",
        "...ooooooooooo..",
        "................",
        "................",
    ], {"o": DEEP, "n": NIGHT, "s": SILVER, "d": (8, 14, 44), "x": (4, 8, 24), "G": GREEN, "T": TEAL, "V": VIOLET, "R": ROSE, "I": ICE,
        "W": (255, 255, 255), "m": (40, 52, 80), "c": g.CREAM})


def boreal_codex_model_texture():
    """128 x 128 texture of the 3D book (the grimoire layout, 8 pixels a unit): covers of night-blue leather painted with
    the northern lights over a line of snowy peaks, stars, a frame of silver filigree; a silver spine; old pages; the
    ornaments' materials (silver, dark silver, ice, glowing ice)."""
    s = 8
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    W, H = 12 * s, 9 * s
    ys, xs = np.mgrid[0:H, 0:W].astype(np.float32)
    grain = _tnoise(W, H, (12, 8), 1731)
    cover = _ramp(np.clip(.2 + .5 * grain, 0, 1), [DEEP, NIGHT, (22, 40, 96)])
    # The aurora: rays hanging over the peaks, green at their hem, violet and rose above
    streak = .6 * _tnoise(W, H, (24, 1), 1732) + .4 * _tnoise(W, H, (48, 2), 1733)
    hem_y = H * (.52 + .06 * np.sin(xs / W * math.tau * 1.3))
    up = np.clip((hem_y - ys) / (H * .42), 0, 1)
    on = (ys < hem_y) & (up < 1)
    k = np.where(on, (1 - up) ** .8 * streak * 1.4, 0)
    hues = np.zeros((H, W, 3))
    for i in range(H):
        for j in range(W):
            if on[i, j]:
                hues[i, j] = hue(up[i, j])
    cover = cover * (1 - np.clip(k, 0, 1)[..., None] * .85) + hues * np.clip(k, 0, 1)[..., None]
    img.paste(_rgba(cover, np.full((H, W), 255)), (0, 0))
    d = ImageDraw.Draw(img)
    # Snowy peaks along the bottom
    peaks = [(0, H)]
    rnd = random.Random(1734)
    x = 0
    while x < W:
        x += rnd.randint(6, 12)
        peaks.append((x, H * (.68 + .12 * rnd.random())))
        x += rnd.randint(6, 12)
        peaks.append((x, H * (.8 + .08 * rnd.random())))
    peaks.append((W, H))
    d.polygon(peaks, fill=(18, 28, 56, 255))
    for i in range(1, len(peaks) - 1, 2):
        px, py = peaks[i]
        d.polygon([(px, py), (px - 3, py + 4), (px + 3, py + 4)], fill=(220, 236, 255, 255))
    # Stars
    for _ in range(30):
        sx, sy = rnd.randint(2, W - 3), rnd.randint(2, int(H * .45))
        b = rnd.randint(170, 255)
        d.point((sx, sy), fill=(b, b, 255, 255))
    for sx, sy in ((14, 8), (78, 12), (60, 5)):
        d.line((sx - 2, sy, sx + 2, sy), fill=(255, 255, 255, 255))
        d.line((sx, sy - 2, sx, sy + 2), fill=(255, 255, 255, 255))
    # A frame of silver filigree, curls at the corners
    d.rectangle((1, 1, W - 2, H - 2), outline=SILVER + (255,))
    d.rectangle((3, 3, W - 4, H - 4), outline=SILVER_DARK + (255,))
    for cx, cy in ((7, 7), (W - 8, 7), (7, H - 8), (W - 8, H - 8)):
        d.arc((cx - 4, cy - 4, cx + 4, cy + 4), 0, 360, fill=SILVER + (255,))
        d.point((cx, cy), fill=ICE + (255,))
    d.rectangle((0, 0, W - 1, H - 1), outline=(4, 6, 20, 255))
    # The cover's edge
    d.rectangle((12 * s, 0, 13 * s - 1, 9 * s - 1), fill=(8, 14, 44, 255))
    for yy in range(0, 9 * s, 4):
        d.point((12 * s + 3, yy), fill=SILVER_DARK + (255,))
    # The page edges
    for yy in range(0, 8 * s):
        for xx in range(13 * s, 16 * s):
            img.putpixel((xx, yy), g.lerp(g.CREAM, (176, 168, 140), ((xx * 7 + yy * 13) % 11) / 22) + (255,))
    for yy in range(1, 8 * s, 2):
        d.line((13 * s, yy, 16 * s - 1, yy), fill=g.CREAM_SHADE + (255,))
    # The spine: silver with a band of the aurora
    for yy in range(9 * s, 12 * s):
        for xx in range(0, 12 * s):
            t = .5 + .5 * math.sin(xx * .25)
            img.putpixel((xx, yy), g.lerp(SILVER_DARK, SILVER, t) + (255,))
    for xx in range(0, 12 * s):
        c = hue((xx / (12 * s) * 2) % 1 if xx < 6 * s else 2 - xx / (12 * s) * 2)
        for yy in range(int(10.2 * s), int(10.8 * s)):
            img.putpixel((xx, yy), c + (255,))
    d.rectangle((0, 12 * s, 12 * s - 1, 15 * s - 1), fill=g.CREAM + (255,))
    for xx in range(0, 12 * s, 2):
        d.line((xx, 12 * s, xx, 15 * s - 1), fill=g.CREAM_SHADE + (255,))
    # The plain book's metal (corners, bands), gem (headbands, bezel) and strap
    _metal(img, 12 * s, 9 * s, 2 * s, 2 * s, SILVER, SILVER_DARK)
    _jewel(img, 14 * s, 9 * s, 2 * s, 2 * s)
    d.rectangle((12 * s, 11 * s, 16 * s - 1, 12 * s - 1), fill=(10, 18, 52, 255))
    d.line((12 * s, 11 * s + 3, 16 * s - 1, 11 * s + 3), fill=SILVER_DARK + (255,))
    # The ornaments' materials
    _metal(img, 12 * s, 12 * s, 2 * s, 2 * s, SILVER, SILVER_DARK)
    _metal(img, 14 * s, 12 * s, 2 * s, 2 * s, SILVER_DARK, (60, 72, 100))
    _jewel(img, 12 * s, 14 * s, 2 * s, 2 * s)
    for yy in range(14 * s, 16 * s):
        for xx in range(14 * s, 16 * s):
            t = math.hypot(xx - 15 * s, yy - 15 * s) / (1.2 * s)
            img.putpixel((xx, yy), g.lerp((255, 255, 255), TEAL, min(1, t)) + (255,))
    return img


def _metal(img, x0, y0, w, h, light, dark):
    for yy in range(y0, y0 + h):
        for xx in range(x0, x0 + w):
            t = .5 + .5 * math.sin((xx - x0 + yy - y0) * .45)
            img.putpixel((xx, yy), g.lerp(dark, light, t) + (255,))


def _jewel(img, x0, y0, w, h):
    for yy in range(y0, y0 + h):
        for xx in range(x0, x0 + w):
            u, v = (xx - x0) / w, (yy - y0) / h
            t = abs(u - .5) + abs(v - .5)
            img.putpixel((xx, yy), g.lerp((255, 255, 255), (120, 200, 255), min(1, t * 1.4)) + (255,))


def boreal_codex_ornaments():
    """A star of ice on each cover (two plates turned an eighth apart, a cross of light behind them, a jewel at its heart),
    crystal finials at both ends of the spine."""
    e = []
    cy, cz = 3.8, 8.0
    for front in (True, False):
        x0, x1 = bo.on_cover(front, 0, .1)
        # The cross of light behind the star
        e.append(bo.turned(front, x0, x1, cy, cz, 2.6, .12, bo.GLOW, 0))
        e.append(bo.turned(front, x0, x1, cy, cz, .12, 3.4, bo.GLOW, 0))
        x0, x1 = bo.on_cover(front, .1, .22)
        e.append(bo.turned(front, x0, x1, cy, cz, 1.25, 1.25, bo.JEWEL, 0))
        e.append(bo.turned(front, x0, x1, cy, cz, 1.25, 1.25, bo.JEWEL, 45))
        x0, x1 = bo.on_cover(front, .22, .3)
        e.append(bo.turned(front, x0, x1, cy, cz, .8, .8, bo.METAL, 22.5))
        x0, x1 = bo.on_cover(front, .3, .38)
        e.append(bo.turned(front, x0, x1, cy, cz, .45, .45, bo.GLOW, -22.5))
        # Small stars at the cover's corners
        for sy, sz in ((1.4, 4.6), (1.4, 11.4), (6.2, 4.6), (6.2, 11.4)):
            x0, x1 = bo.on_cover(front, 0, .12)
            e.append(bo.turned(front, x0, x1, sy, sz, .38, .38, bo.METAL, 45))
    # Crystal finials at both ends of the spine
    for z in (bo.Z0 - .25, bo.Z1 + .25):
        e.append(bo.solid([7.55, bo.H - .2, z - .45], [8.45, bo.H + .7, z + .45], bo.JEWEL,
                          {"origin": [8, bo.H + .25, z], "axis": "y", "angle": 45}))
        e.append(bo.solid([7.75, bo.H + .7, z - .25], [8.25, bo.H + 1.2, z + .25], bo.GLOW,
                          {"origin": [8, bo.H + .95, z], "axis": "y", "angle": 45}))
    return e


def affinity_ring_aurora():
    return g.from_grid([
        "................",
        "......oooo......",
        ".....oGTVRo.....",
        ".....oTIIVo.....",
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
    ], {"o": DEEP, "h": (255, 255, 255), "l": SILVER, "b": (170, 186, 210), "s": SILVER_DARK, "v": TEAL, "G": GREEN, "T": TEAL, "V": VIOLET, "R": ROSE,
        "I": ICE})


def main():
    g.save(rays_texture(), "textures/entity/aurora/rays.png")
    g.save(band_texture(), "textures/entity/aurora/band.png")
    g.save(veil_texture(), "textures/entity/aurora/veil.png")
    g.save(night_texture(), "textures/entity/aurora/night.png")
    for name, icon in ICONS:
        g.save(icon(), f"textures/gui/spell_icons/{name}.png")
    g.save(affinity_ring_aurora(), "textures/item/affinity_ring_aurora.png")
    g.write_json("models/item/affinity_ring_aurora.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/affinity_ring_aurora"}})
    g.save(boreal_codex_icon(), "textures/item/boreal_codex.png")
    g.save(boreal_codex_model_texture(), "textures/item/boreal_codex_3d.png")
    g.book_models("boreal_codex")
    bo.add("boreal_codex", boreal_codex_ornaments())


if __name__ == "__main__":
    main()
