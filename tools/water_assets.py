#!/usr/bin/env python3
"""Water Magic art: the water, foam, caustic light, drop, rain, cloud, bubble skin, jellyfish bell, fish, manta ray,
kelp and light shaft textures the spells are drawn with, the spell icons, the Tide Codex (icon and 3D book texture)
and its affinity ring.

Called by gen_assets.py (run that one). Deep sea blue, bright shallows, white foam, the cyan glow of the deep. Original art.
"""
import colorsys
import math

import numpy as np
from PIL import Image, ImageDraw

import gen_assets as g

ABYSS = (6, 18, 40)
DEEP_DARK = (14, 52, 104)
DEEP = (31, 111, 184)
SHALLOW = (79, 195, 240)
FOAM = (234, 248, 255)
GLOW = (124, 242, 255)
OUTLINE = (8, 26, 54)
PEARL = (244, 238, 226)
SAND = (214, 194, 140)
KELP_DARK = (52, 78, 22)
KELP = (98, 132, 38)
KELP_LIGHT = (156, 172, 64)


def _ramp(t, stops):
    stops = np.array(stops, float)
    tt = np.clip(t, 0, 1) * (len(stops) - 1)
    i = np.minimum(tt.astype(int), len(stops) - 2)
    f = (tt - i)[..., None]
    return stops[i] * (1 - f) + stops[i + 1] * f


def _tnoise(w, h, cells, seed):
    """Smooth value noise in [0, 1], tiling both across and down."""
    rnd = np.random.default_rng(seed)
    grid = rnd.random((cells[1], cells[0]))
    ys, xs = np.mgrid[0:h, 0:w]
    gx, gy = xs / w * cells[0], ys / h * cells[1]
    x0, y0 = gx.astype(int), gy.astype(int)
    fx, fy = gx - x0, gy - y0
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    x1, y1 = (x0 + 1) % cells[0], (y0 + 1) % cells[1]
    x0, y0 = x0 % cells[0], y0 % cells[1]
    a, b, c, d = grid[y0, x0], grid[y0, x1], grid[y1, x0], grid[y1, x1]
    return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy


def _worley(S, points, seed):
    """F1 and F2 (distances to the nearest and second nearest point) over a tile S wide, the points wrapping round."""
    rnd = np.random.default_rng(seed)
    pts = rnd.random((points, 2))
    ys, xs = np.mgrid[0:S, 0:S]
    u, v = (xs + .5) / S, (ys + .5) / S
    ds = []
    for px, py in pts:
        dx = np.abs(u - px)
        dy = np.abs(v - py)
        dx = np.minimum(dx, 1 - dx)
        dy = np.minimum(dy, 1 - dy)
        ds.append(np.hypot(dx, dy))
    ds = np.sort(np.stack(ds), axis=0)
    return ds[0], ds[1]


def _rgba(rgb, alpha):
    return Image.fromarray(np.dstack([np.clip(rgb, 0, 255), np.clip(alpha, 0, 255)]).astype(np.uint8), "RGBA")


def water_texture():
    """64 x 64, tiling: pale moving water (its colour and see-through set in game): soft ripples, brighter crests."""
    S = 64
    ys, xs = np.mgrid[0:S, 0:S]
    u, v = xs / S, ys / S
    warp = _tnoise(S, S, (4, 4), 11)
    ripples = .5 + .5 * np.sin((v * 3 + u + warp * 1.2) * math.tau)
    fine = _tnoise(S, S, (16, 16), 12)
    f1, f2 = _worley(S, 14, 13)
    crest = np.exp(-(f2 - f1) / .03)
    t = .55 + .2 * (ripples - .5) + .15 * (fine - .5) + .35 * crest
    rgb = _ramp(t, [(170, 205, 230), (205, 232, 248), (240, 250, 255), (255, 255, 255)])
    alpha = 200 + 55 * np.clip(t, 0, 1)
    return _rgba(rgb, alpha)


def foam_texture():
    """64 x 64, tiling: lacy white foam, its holes see-through, a little blue in its shadows."""
    S = 64
    f1, f2 = _worley(S, 22, 21)
    lace = np.clip(1 - (f2 - f1) / .06, 0, 1)
    blob = _tnoise(S, S, (6, 6), 22)
    fine = _tnoise(S, S, (24, 24), 23)
    t = np.clip(lace * .9 + (blob - .45) * 1.4 + (fine - .5) * .4, 0, 1)
    rgb = _ramp(t, [(190, 225, 245), (232, 246, 255), (255, 255, 255)])
    return _rgba(rgb, t ** .8 * 240)


def caustics_texture():
    """64 x 64, tiling: the bright web of light that shines through waves onto the sea bed, on black (drawn additive)."""
    S = 64
    f1, f2 = _worley(S, 12, 31)
    web = np.exp(-(f2 - f1) / .022)
    g1, g2 = _worley(S, 20, 32)
    web2 = np.exp(-(g2 - g1) / .016) * .5
    t = np.clip(web + web2, 0, 1) * (.75 + .25 * _tnoise(S, S, (4, 4), 33))
    rgb = np.dstack([t * 200, t * 250, t * 255])
    return _rgba(rgb, np.full((S, S), 255))


def drop_texture():
    """32 x 32 drop of water: clear in its middle, brighter round its rim, a glint of light at its top left."""
    S = 32
    ys, xs = np.mgrid[0:S, 0:S]
    x, y = (xs + .5) / S - .5, (ys + .5) / S - .5
    r = np.hypot(x, y) * 2.3
    inside = np.clip((1 - r) * 8, 0, 1)
    rim = .45 + .55 * r ** 3
    glint = np.exp(-((x + .14) ** 2 + (y + .14) ** 2) / .004)
    alpha = inside * np.clip(rim + glint, 0, 1) * 245
    t = np.clip(.4 + .6 * glint + .3 * r, 0, 1)
    rgb = _ramp(t, [(170, 220, 250), (230, 248, 255), (255, 255, 255)])
    return _rgba(rgb, alpha)


def rain_texture():
    """16 x 64 streak of rain (u across it, v along it): thin, white, fading at both ends."""
    w, h = 16, 64
    ys, xs = np.mgrid[0:h, 0:w]
    u, v = (xs + .5) / w, (ys + .5) / h
    alpha = np.exp(-((u - .5) ** 2) / .02) * np.sin(v * math.pi) ** .6 * 235
    rgb = np.dstack([np.full((h, w), 225), np.full((h, w), 240), np.full((h, w), 255)])
    return _rgba(rgb, alpha)


def cloud_texture():
    """64 x 64 puff of cloud: billowing, soft at its edge, white (shaded in game)."""
    S = 64
    ys, xs = np.mgrid[0:S, 0:S]
    x, y = (xs + .5) / S - .5, (ys + .5) / S - .5
    r = np.hypot(x, y) * 2
    billow = _tnoise(S, S, (5, 5), 41) * .6 + _tnoise(S, S, (11, 11), 42) * .4
    edge = np.clip((1 - r + (billow - .5) * .5) * 2.5, 0, 1)
    shade = np.clip(.75 + .25 * billow - .2 * y * 2, 0, 1)
    rgb = np.dstack([shade * 255, shade * 255, shade * 255])
    return _rgba(rgb, edge ** 1.3 * 235)


def bubble_texture():
    """64 x 32 soap bubble skin (u round it, v top to bottom): nearly clear, swirling bands of rainbow, a window of light."""
    w, h = 64, 32
    ys, xs = np.mgrid[0:h, 0:w]
    u, v = xs / w, (ys + .5) / h
    swirl = _tnoise(w, h, (4, 2), 51)
    hue = (v * 1.6 + swirl * .9 + .2 * np.sin(u * math.tau * 2)) % 1
    rgb = np.zeros((h, w, 3))
    for yy in range(h):
        for xx in range(w):
            rgb[yy, xx] = colorsys.hsv_to_rgb(hue[yy, xx], .45, 1)
    rgb *= 255
    sheen = .5 + .5 * np.sin((v * 5 + swirl * 2) * math.tau)
    alpha = 40 + 90 * sheen ** 2
    window = (np.abs(u - .18) < .06) & (np.abs(v - .3) < .1)
    alpha[window] = 230
    rgb[window] = 255
    return _rgba(rgb, alpha)


def jelly_texture():
    """64 x 64 jellyfish bell seen from above: clear, rings and canals running out from its middle, its rim glowing
    (pale, tinted in game)."""
    S = 64
    ys, xs = np.mgrid[0:S, 0:S]
    x, y = (xs + .5) / S - .5, (ys + .5) / S - .5
    r = np.hypot(x, y) * 2
    a = np.arctan2(y, x)
    canals = np.exp(-((np.sin(a * 4) ** 2) * r * r) / .004) * (r > .2)
    rings = sum(np.exp(-((r - k) ** 2) / .0012) for k in (.32, .62, .84))
    rim = np.exp(-((r - .97) ** 2) / .003)
    heart = np.exp(-(r ** 2) / .02)
    fine = .5 + .5 * np.sin(a * 32) * (r > .7)
    t = np.clip(.25 + .5 * canals + .45 * rings + .9 * rim + .6 * heart + .1 * fine, 0, 1)
    alpha = np.clip(70 + 180 * t, 0, 255) * (r < 1.02)
    rgb = _ramp(t, [(200, 230, 255), (240, 250, 255), (255, 255, 255)])
    return _rgba(rgb, alpha)


def fish_texture():
    """64 x 32 small fish seen from its side, its head to the right: darker back, white belly, a stripe, an eye, fins
    and a forked tail (pale, tinted in game)."""
    w, h = 64, 32
    S = 4
    img = Image.new("RGBA", (w * S, h * S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cy = h * S * .5
    body = []
    for k in range(41):
        t = k / 40
        x = (12 + 44 * t) * S
        half = 9.5 * S * math.sin(math.pi * (.08 + .92 * t)) ** .8 * (1 - .25 * t)
        body.append((x, cy - half * 1.05))
    belly = []
    for k in range(40, -1, -1):
        t = k / 40
        x = (12 + 44 * t) * S
        half = 9.5 * S * math.sin(math.pi * (.08 + .92 * t)) ** .8 * (1 - .25 * t)
        belly.append((x, cy + half * .9))
    # Tail and fins first, the body over them
    d.polygon([(14 * S, cy), (2 * S, cy - 11 * S), (6 * S, cy), (2 * S, cy + 11 * S)], fill=(200, 210, 222, 235))
    d.polygon([(26 * S, cy - 8 * S), (32 * S, cy - 15 * S), (40 * S, cy - 8 * S)], fill=(190, 202, 216, 225))
    d.polygon([(30 * S, cy + 7 * S), (34 * S, cy + 12 * S), (40 * S, cy + 7 * S)], fill=(214, 222, 232, 220))
    d.polygon(body + belly, fill=(236, 242, 248, 255))
    arr = np.array(img).astype(float)
    ys, xs = np.mgrid[0:h * S, 0:w * S]
    vy = (ys - cy) / (10 * S)
    inside = arr[..., 3] > 0
    shade = np.clip(.55 + .45 * vy, 0, 1)
    tone = _ramp(shade, [(150, 166, 184), (214, 224, 236), (255, 255, 255)])
    stripe = np.exp(-((vy + .05) ** 2) / .004) * (xs > 16 * S)
    tone = tone * (1 - .15 * stripe[..., None]) + np.array([255, 255, 255]) * .15 * stripe[..., None]
    scales = (np.sin(xs / S * 1.6 + np.sin(ys / S * 1.6) * 1.2) > .85) & (vy < .3)
    tone[scales] *= .93
    arr[..., :3] = np.where(inside[..., None], tone, arr[..., :3])
    img = Image.fromarray(arr.astype(np.uint8), "RGBA")
    d = ImageDraw.Draw(img)
    d.arc((44 * S, cy - 9 * S, 52 * S, cy + 7 * S), 120, 240, fill=(150, 160, 176, 255), width=S)
    d.ellipse((49 * S, cy - 4.5 * S, 53.5 * S, cy), fill=(16, 20, 28, 255))
    d.ellipse((50 * S, cy - 4 * S, 51.5 * S, cy - 2.6 * S), fill=(255, 255, 255, 255))
    return img.resize((w, h), Image.LANCZOS)


def manta_texture():
    """128 x 128 manta ray seen from above, its head to the top: broad curved wings, horn-like fins either side of its
    mouth, a dark back with pale shoulder patches and a few white spots, its long thin tail trailing down."""
    S = 4
    N = 128 * S
    img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    def P(x, y):
        return x * S, y * S

    right = []
    for k in range(25):
        t = k / 24
        # Leading edge: from beside the head, bowing forward, out to the tip
        x = 64 + 8 + 54 * t
        y = 20 + 28 * t ** 1.6 - 8 * math.sin(math.pi * t)
        right.append(P(x, y))
    for k in range(1, 25):
        t = k / 24
        # Trailing edge: from the tip, curving back in to the root of the tail
        x = 126 - 56 * t
        y = 48 + 10 * math.sin(math.pi * t * .9) + 26 * t ** 2
        right.append(P(x, y))
    left = [(N - x, y) for x, y in reversed(right)]
    body = [P(58, 18), P(64, 16), P(70, 18)] + right + [P(66, 86), P(62, 86)] + left
    d.polygon(body, fill=(28, 40, 64, 255))
    # Horn-like fins either side of its mouth
    for sgn in (-1, 1):
        d.polygon([P(64 + sgn * 5, 20), P(64 + sgn * 11, 6), P(64 + sgn * 13, 9), P(64 + sgn * 10, 22)], fill=(24, 34, 56, 255))
    # Tail
    d.line([P(64, 84), P(64.5, 100), P(63.5, 114), P(64, 127)], fill=(22, 30, 50, 255), width=int(1.6 * S))
    arr = np.array(img).astype(float)
    inside = arr[..., 3] > 0
    ys, xs = np.mgrid[0:N, 0:N]
    x, y = xs / S, ys / S
    # A ridge down its back, lighter wing edges, pale patches on its shoulders
    ridge = np.exp(-((x - 64) ** 2) / 30) * (y < 84)
    edge = np.clip((np.abs(x - 64) - 30) / 40, 0, 1)
    patch = np.zeros_like(x)
    for sgn in (-1, 1):
        patch += np.exp(-(((x - 64 - sgn * 22) / 13) ** 2 + ((y - 36) / 7) ** 2))
    noise = np.array(Image.fromarray((_tnoise(128, 128, (12, 12), 61) * 255).astype(np.uint8)).resize((N, N))) / 255
    t = np.clip(.2 + .25 * ridge + .3 * edge + .7 * patch + .12 * (noise - .5), 0, 1)
    tone = _ramp(t, [(20, 30, 52), (40, 58, 88), (96, 120, 150), (214, 224, 232)])
    arr[..., :3] = np.where(inside[..., None], tone, arr[..., :3])
    img = Image.fromarray(arr.astype(np.uint8), "RGBA")
    d = ImageDraw.Draw(img)
    rnd = np.random.default_rng(62)
    for _ in range(14):
        sx, sy = 64 + rnd.uniform(-40, 40), rnd.uniform(40, 70)
        if abs(sx - 64) < 52 - (sy - 40) * .8:
            r = rnd.uniform(.8, 1.8)
            d.ellipse((P(sx - r, sy - r), P(sx + r, sy + r)), fill=(226, 232, 238, 255))
    # Its eyes, at the sides of its head
    for sgn in (-1, 1):
        d.ellipse((P(64 + sgn * 7 - 1.2, 21), P(64 + sgn * 7 + 1.2, 23.4)), fill=(6, 8, 14, 255))
    return img.resize((128, 128), Image.LANCZOS)


def kelp_texture():
    """64 x 32 frond of kelp lying along the texture (it tiles along u): a stipe down its middle with little floats on
    it, a wide ruffled blade either side, olive and gold, its veins slanting out."""
    w, h = 64, 32
    ys, xs = np.mgrid[0:h, 0:w]
    u, v = xs / w, (ys + .5) / h
    half = .3 + .12 * np.sin(u * math.tau * 2) + .04 * np.sin(u * math.tau * 12)
    blade = np.abs(v - .5) < half
    veins = .5 + .5 * np.sin((u * 10 + np.abs(v - .5) * 3) * math.tau)
    tone = np.clip(.45 + .25 * (veins - .5) + .25 * (_tnoise(w, h, (8, 4), 71) - .5) + .3 * (np.abs(v - .5) / .42), 0, 1)
    rgb = _ramp(tone, [KELP_DARK, KELP, KELP_LIGHT, (190, 186, 90)])
    stipe = np.abs(v - .5) < .05
    rgb[stipe] = (110, 98, 36)
    floats = (np.hypot((u % .5) - .25, (v - .5) * .5) < .04)
    rgb[floats] = (196, 176, 80)
    alpha = np.where(blade | stipe | floats, 245, 0)
    edge = blade & (np.abs(v - .5) > half - .04)
    alpha = np.where(edge, 170, alpha)
    return _rgba(rgb, alpha)


def ray_texture():
    """32 x 64 shaft of light (u across, v down it): bright at its top, fading down it and to its sides, faintly
    streaked; on black (drawn additive)."""
    w, h = 32, 64
    ys, xs = np.mgrid[0:h, 0:w]
    u, v = (xs + .5) / w, (ys + .5) / h
    streaks = .75 + .25 * _tnoise(w, h, (8, 2), 81)
    t = np.exp(-((u - .5) ** 2) / .035) * (1 - v) ** 1.3 * streaks * np.clip(v * 8, 0, 1)
    rgb = np.dstack([t * 190, t * 245, t * 255])
    return _rgba(rgb, np.full((h, w), 255))


# ------------------------------------------------------------------------------------------------------- the icons


def _icon(draw, cx=.5, cy=.5):
    return g.painted(64, draw, (cx, cy, (24, 82, 140), ABYSS), 16)


def _a(c, a=255):
    return tuple(c) + (a,)


def _sea(d, S, y=.8):
    """Waves along the bottom of an icon."""
    pts = [(0, S)]
    for k in range(33):
        x = k / 32
        pts.append((x * S, (y + .03 * math.sin(x * math.tau * 3)) * S))
    pts.append((S, S))
    d.polygon(pts, fill=_a(DEEP))
    d.line(pts[1:-1], fill=_a(FOAM), width=max(1, S // 32))


def tide_lance_icon():
    def draw(d, img, S):
        # A spear of water flying up to the right, a spiral round it, foam at its tip
        x0, y0, x1, y1 = .12, .86, .84, .16
        d.line((x0 * S, y0 * S, x1 * S, y1 * S), fill=_a(SHALLOW), width=int(S * .1))
        d.line((x0 * S, y0 * S, x1 * S, y1 * S), fill=_a(FOAM), width=int(S * .03))
        for k in range(7):
            t = .15 + k * .11
            x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
            d.arc(((x - .07) * S, (y - .07) * S, (x + .07) * S, (y + .07) * S), 200, 340, fill=_a(FOAM, 200), width=max(1, S // 32))
        d.polygon([(x1 * S, y1 * S), ((x1 - .16) * S, (y1 + .02) * S), ((x1 - .02) * S, (y1 + .16) * S)], fill=_a(FOAM))
        g.glow_spot(img, x1 * S, y1 * S, S * .12, GLOW, 1)
        for k in range(5):
            d.ellipse(((.1 + k * .05) * S, (.7 + .04 * (k % 2)) * S, (.13 + k * .05) * S, (.73 + .04 * (k % 2)) * S), fill=_a(FOAM, 180))
    return _icon(draw, .6, .4)


def pressure_jet_icon():
    def draw(d, img, S):
        # A hand of water at the left, a straight hard jet across, spray bursting where it strikes
        d.ellipse((.04 * S, .38 * S, .26 * S, .62 * S), fill=_a(SHALLOW), outline=_a(DEEP), width=max(1, S // 32))
        d.line((.2 * S, .5 * S, .82 * S, .5 * S), fill=_a(SHALLOW), width=int(S * .1))
        d.line((.2 * S, .5 * S, .82 * S, .5 * S), fill=_a(FOAM), width=int(S * .04))
        for k in range(10):
            a = -1.2 + k * .27
            d.line((.82 * S, .5 * S, (.82 + math.cos(a) * .14) * S, (.5 + math.sin(a) * .2) * S), fill=_a(FOAM, 220), width=max(1, S // 28))
        d.rectangle((.9 * S, .2 * S, S, .8 * S), fill=(70, 60, 56, 255))
        g.glow_spot(img, .84 * S, .5 * S, S * .14, GLOW, 1.1)
    return _icon(draw, .5, .5)


def geyser_icon():
    def draw(d, img, S):
        d.rectangle((0, .82 * S, S, S), fill=(60, 48, 36, 255))
        d.polygon([(.36 * S, .84 * S), (.42 * S, .1 * S), (.58 * S, .1 * S), (.64 * S, .84 * S)], fill=_a(SHALLOW))
        d.polygon([(.45 * S, .84 * S), (.47 * S, .14 * S), (.53 * S, .14 * S), (.55 * S, .84 * S)], fill=_a(FOAM))
        for k in range(9):
            a = math.pi + k * math.pi / 8
            d.ellipse(((.5 + math.cos(a) * .3 - .03) * S, (.18 + math.sin(a) * .12 - .03) * S, (.5 + math.cos(a) * .3 + .03) * S,
                       (.18 + math.sin(a) * .12 + .03) * S), fill=_a(FOAM))
        d.ellipse((.3 * S, .02 * S, .7 * S, .24 * S), fill=_a(FOAM, 220))
        for x in (.22, .78):
            d.ellipse(((x - .08) * S, .78 * S, (x + .08) * S, .88 * S), fill=_a(FOAM, 200))
    return _icon(draw, .5, .3)


def bubble_prison_icon():
    def draw(d, img, S):
        # A figure caught in a great bubble, a rainbow sheen round its rim
        d.ellipse((.12 * S, .12 * S, .88 * S, .88 * S), fill=_a(SHALLOW, 90))
        d.ellipse((.4 * S, .3 * S, .6 * S, .5 * S), fill=(214, 178, 140, 255), outline=_a(OUTLINE), width=max(1, S // 48))
        d.rectangle((.42 * S, .48 * S, .58 * S, .74 * S), fill=(176, 120, 72, 255), outline=_a(OUTLINE), width=max(1, S // 48))
        for k, col in enumerate([(255, 160, 200), (255, 230, 150), (160, 255, 200), (150, 210, 255)]):
            d.arc(((.12 + k * .01) * S, (.12 + k * .01) * S, (.88 - k * .01) * S, (.88 - k * .01) * S), 200 + k * 30, 300 + k * 30, fill=_a(col, 220),
                  width=max(1, S // 32))
        d.ellipse((.12 * S, .12 * S, .88 * S, .88 * S), outline=_a(FOAM), width=max(1, S // 24))
        d.ellipse((.24 * S, .22 * S, .36 * S, .32 * S), fill=_a((255, 255, 255)))
        for x, y in ((.3, .7), (.6, .62), (.7, .3)):
            d.ellipse(((x - .03) * S, (y - .03) * S, (x + .03) * S, (y + .03) * S), outline=_a(FOAM, 200), width=max(1, S // 48))
    return _icon(draw, .5, .5)


def cleansing_rain_icon():
    def draw(d, img, S):
        for k in range(9):
            x = .14 + k * .09
            d.line((x * S, .44 * S, (x - .04) * S, (.8 + .05 * (k % 2)) * S), fill=_a((200, 232, 255), 210), width=max(1, S // 32))
        for x, y, r in ((.3, .3, .17), (.52, .24, .2), (.72, .32, .15), (.5, .36, .18)):
            d.ellipse(((x - r) * S, (y - r * .8) * S, (x + r) * S, (y + r * .8) * S), fill=_a((214, 226, 238)))
        d.ellipse((.18 * S, .34 * S, .84 * S, .48 * S), fill=_a((150, 168, 190)))
        _sea(d, S, .88)
        # A glint of healing where it falls
        g.glow_spot(img, .72 * S, .76 * S, S * .14, (140, 255, 170), 1.1)
        d.line((.72 * S, .68 * S, .72 * S, .84 * S), fill=_a((230, 255, 236)), width=max(1, S // 28))
        d.line((.64 * S, .76 * S, .8 * S, .76 * S), fill=_a((230, 255, 236)), width=max(1, S // 28))
    return _icon(draw, .5, .3)


def maelstrom_icon():
    def draw(d, img, S):
        d.ellipse((.04 * S, .04 * S, .96 * S, .96 * S), fill=_a(DEEP))
        for arm in range(3):
            pts = []
            for k in range(40):
                t = k / 39
                a = arm * math.tau / 3 + t * 4.2
                r = .44 * (1 - t) + .02
                pts.append(((.5 + math.cos(a) * r) * S, (.5 + math.sin(a) * r) * S))
            d.line(pts, fill=_a(FOAM), width=max(1, S // 24), joint="curve")
            d.line(pts, fill=_a(SHALLOW), width=max(1, S // 64))
        d.ellipse((.42 * S, .42 * S, .58 * S, .58 * S), fill=_a(ABYSS))
        g.glow_spot(img, .5 * S, .5 * S, S * .1, GLOW, .7)
    return _icon(draw, .5, .5)


def jellyfish_swarm_icon():
    def draw(d, img, S):
        for x, y, s, col in ((.3, .3, .2, (255, 140, 220)), (.68, .42, .17, (140, 220, 255)), (.42, .62, .13, (190, 160, 255))):
            for k in range(5):
                tx = x - s * .7 + k * s * .35
                pts = [(tx * S, y * S)] + [((tx + .02 * math.sin(j * 1.3 + k)) * S, (y + j * .06) * S) for j in range(1, 6)]
                d.line(pts, fill=_a(col, 170), width=max(1, S // 48))
            d.pieslice(((x - s) * S, (y - s * .8) * S, (x + s) * S, (y + s * .8) * S), 180, 360, fill=_a(col, 200))
            d.arc(((x - s) * S, (y - s * .8) * S, (x + s) * S, (y + s * .8) * S), 180, 360, fill=_a((255, 255, 255)), width=max(1, S // 40))
            g.glow_spot(img, x * S, (y - s * .3) * S, S * s * .7, col, .9)
    return _icon(draw, .5, .5)


def typhoon_icon():
    def draw(d, img, S):
        _sea(d, S, .86)
        # A storm cloud on top, a funnel of water down from it, spray at its foot, a bolt in the cloud
        for k in range(10):
            t = k / 9
            r = .05 + .3 * t ** 1.3
            y = .84 - t * .62
            d.arc(((.5 - r) * S, (y - .04) * S, (.5 + r) * S, (y + .04) * S), 0, 180, fill=_a(FOAM if k % 2 else SHALLOW), width=max(1, S // 24))
        for x, y, r in ((.28, .16, .14), (.5, .1, .18), (.72, .16, .14)):
            d.ellipse(((x - r) * S, (y - r * .7) * S, (x + r) * S, (y + r * .7) * S), fill=_a((70, 84, 110)))
        d.line([(.62 * S, .08 * S), (.56 * S, .2 * S), (.64 * S, .2 * S), (.58 * S, .32 * S)], fill=_a((255, 250, 180)), width=max(1, S // 32))
        g.glow_spot(img, .6 * S, .2 * S, S * .1, (255, 250, 200), .8)
        d.ellipse((.36 * S, .8 * S, .64 * S, .9 * S), fill=_a(FOAM, 200))
    return _icon(draw, .5, .3)


def tidal_wave_icon():
    def draw(d, img, S):
        # A great wave curling over from the left, its face hollow, white water at its lip
        pts = [(0, S), (0, .5 * S)]
        for k in range(21):
            t = k / 20
            a = math.pi * (1.1 - 1.3 * t)
            pts.append(((.42 + math.cos(a) * .36) * S, (.48 - math.sin(a) * .36) * S))
        pts += [(.95 * S, .86 * S), (S, S)]
        d.polygon(pts, fill=_a(DEEP))
        lip = [((.42 + math.cos(math.pi * (1.1 - 1.3 * t)) * .36) * S, (.48 - math.sin(math.pi * (1.1 - 1.3 * t)) * .36) * S) for t in [k / 20 for k in range(21)]]
        d.line(lip, fill=_a(FOAM), width=max(2, S // 16), joint="curve")
        d.ellipse((.42 * S, .36 * S, .74 * S, .76 * S), fill=_a(SHALLOW))
        d.ellipse((.5 * S, .44 * S, .7 * S, .72 * S), fill=_a((24, 82, 140)))
        for k in range(6):
            x = .6 + k * .06
            d.ellipse(((x - .02) * S, (.2 + .05 * (k % 2)) * S, (x + .02) * S, (.24 + .05 * (k % 2)) * S), fill=_a(FOAM))
        d.line([(0, .86 * S), (S, .9 * S)], fill=_a(FOAM), width=max(1, S // 28))
    return _icon(draw, .7, .6)


def abyssal_dome_icon():
    def draw(d, img, S):
        d.rectangle((0, .8 * S, S, S), fill=_a(SAND))
        d.pieslice((.06 * S, .16 * S, .94 * S, 1.44 * S), 180, 360, fill=_a(DEEP, 255))
        # Light falling in, fish inside, kelp at its rim
        for x in (.32, .5, .66):
            d.polygon([(x * S, .24 * S), ((x + .06) * S, .24 * S), ((x + .02) * S, .8 * S), ((x - .06) * S, .8 * S)], fill=_a(SHALLOW, 120))
        for x, y in ((.36, .5), (.46, .44), (.58, .56), (.66, .46)):
            d.ellipse(((x - .04) * S, (y - .02) * S, (x + .04) * S, (y + .02) * S), fill=_a((255, 190, 110)))
            d.polygon([((x - .04) * S, y * S), ((x - .07) * S, (y - .025) * S), ((x - .07) * S, (y + .025) * S)], fill=_a((255, 190, 110)))
        for x in (.14, .86):
            d.line([(x * S, .8 * S), ((x + .02) * S, .66 * S), ((x - .01) * S, .54 * S)], fill=_a(KELP_LIGHT), width=max(1, S // 28))
        d.arc((.06 * S, .16 * S, .94 * S, 1.44 * S), 180, 360, fill=_a(GLOW), width=max(1, S // 24))
        g.glow_spot(img, .5 * S, .3 * S, S * .2, GLOW, .5)
    return _icon(draw, .5, .55)


ICONS = [
    ("tide_lance", tide_lance_icon),
    ("pressure_jet", pressure_jet_icon),
    ("geyser", geyser_icon),
    ("bubble_prison", bubble_prison_icon),
    ("cleansing_rain", cleansing_rain_icon),
    ("maelstrom", maelstrom_icon),
    ("jellyfish_swarm", jellyfish_swarm_icon),
    ("typhoon", typhoon_icon),
    ("tidal_wave", tidal_wave_icon),
    ("abyssal_dome", abyssal_dome_icon),
]


# --------------------------------------------------------------------------------------------------- the spellbook


def tide_codex_icon():
    return g.from_grid([
        "................",
        "...oooooooooo...",
        "..obbbbbbbbbdco.",
        "..obsssssssbdco.",
        "..obsbbwbbsbdco.",
        "..obsbwWwbsbdcco",
        "..obswWPWwsbdco.",
        "..obsbwWwbsbdcco",
        "..obsbbwbbsbdcco",
        "..obsssssssbdcco",
        "..obbbbbbbbbdco.",
        "..oddffdddffxco.",
        "..offddffddfxco.",
        "...ooooooooooo..",
        "................",
        "................",
    ], {"o": OUTLINE, "b": DEEP, "s": SHALLOW, "d": DEEP_DARK, "x": ABYSS, "w": GLOW, "W": FOAM,
        "P": PEARL, "f": FOAM, "c": g.CREAM})


def tide_codex_model_texture():
    """64x64 texture of the 3D book, laid out like the other books: covers of deep blue water rolling in waves, framed in
    silver, a pearl in a shell in the middle, a spine of rippling silver, old pages."""
    s = 4
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    W, H = 12 * s, 9 * s
    ys, xs = np.mgrid[0:H, 0:W]
    waves = .5 + .5 * np.sin((xs / W * 3 + ys / H * 1.5 + _tnoise(W, H, (4, 3), 91) * .8) * math.tau)
    cover = _ramp(.25 + .5 * waves * (ys / H), [ABYSS, DEEP_DARK, DEEP, SHALLOW])
    img.paste(_rgba(cover, np.full((H, W), 255)), (0, 0))
    silver = (200, 214, 226)
    d.rectangle((1, 1, W - 2, H - 2), outline=silver + (255,))
    for x in range(3, W - 2, 6):
        d.arc((x - 2, H - 6, x + 2, H - 2), 180, 360, fill=FOAM + (255,))
    cx, cy = W // 2, H // 2
    # A scallop shell holding a pearl
    for k in range(7):
        a = math.pi * (1.1 + .8 * k / 6)
        d.line((cx, cy + 6, cx + math.cos(a) * 11, cy + 6 + math.sin(a) * 11), fill=(236, 214, 196, 255))
    d.pieslice((cx - 11, cy - 5, cx + 11, cy + 17), 190, 350, outline=(150, 120, 110, 255))
    d.ellipse((cx - 3, cy - 2, cx + 3, cy + 4), fill=PEARL + (255,), outline=(180, 170, 160, 255))
    d.point((cx - 1, cy), fill=(255, 255, 255, 255))
    d.rectangle((0, 0, W - 1, H - 1), outline=OUTLINE + (255,))
    d.rectangle((12 * s, 0, 13 * s - 1, 9 * s - 1), fill=DEEP_DARK + (255,))
    for y in range(0, 8 * s):
        for x in range(13 * s, 16 * s):
            img.putpixel((x, y), g.lerp(g.CREAM, (176, 168, 140), ((x * 7 + y * 13) % 11) / 22) + (255,))
    for y in range(1, 8 * s, 2):
        d.line((13 * s, y, 16 * s - 1, y), fill=g.CREAM_SHADE + (255,))
    # The spine: silver rippling like water
    for y in range(9 * s, 12 * s):
        for x in range(0, 12 * s):
            t = .5 + .5 * math.sin(x * .5 + (y - 9 * s) * .9)
            img.putpixel((x, y), g.lerp((120, 140, 160), silver, t) + (255,))
    d.rectangle((0, 12 * s, 12 * s - 1, 15 * s - 1), fill=g.CREAM + (255,))
    for x in range(0, 12 * s, 2):
        d.line((x, 12 * s, x, 15 * s - 1), fill=g.CREAM_SHADE + (255,))
    d.rectangle((12 * s, 9 * s, 14 * s - 1, 11 * s - 1), fill=silver + (255,))
    for y in range(9 * s, 11 * s):
        for x in range(14 * s, 16 * s):
            t = math.hypot(x - 14.8 * s, y - 9.8 * s) / (1.4 * s)
            img.putpixel((x, y), g.lerp(FOAM, DEEP, min(1, t ** .8)) + (255,))
    d.rectangle((12 * s, 11 * s, 16 * s - 1, 12 * s - 1), fill=DEEP_DARK + (255,))
    return img


def affinity_ring_water():
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
    ], {"o": OUTLINE, "h": (236, 244, 250), "l": (200, 214, 226), "b": (150, 168, 186), "s": (96, 112, 132), "v": SHALLOW,
        "G": SHALLOW, "C": FOAM, "x": DEEP})


def main():
    g.save(water_texture(), "textures/entity/water/water.png")
    g.save(foam_texture(), "textures/entity/water/foam.png")
    g.save(caustics_texture(), "textures/entity/water/caustics.png")
    g.save(drop_texture(), "textures/entity/water/drop.png")
    g.save(rain_texture(), "textures/entity/water/rain.png")
    g.save(cloud_texture(), "textures/entity/water/cloud.png")
    g.save(bubble_texture(), "textures/entity/water/bubble.png")
    g.save(jelly_texture(), "textures/entity/water/jelly.png")
    g.save(fish_texture(), "textures/entity/water/fish.png")
    g.save(manta_texture(), "textures/entity/water/manta.png")
    g.save(kelp_texture(), "textures/entity/water/kelp.png")
    g.save(ray_texture(), "textures/entity/water/ray.png")
    for name, icon in ICONS:
        g.save(icon(), f"textures/gui/spell_icons/{name}.png")
    g.save(affinity_ring_water(), "textures/item/affinity_ring_water.png")
    g.write_json("models/item/affinity_ring_water.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/affinity_ring_water"}})
    g.save(tide_codex_icon(), "textures/item/tide_codex.png")
    g.save(tide_codex_model_texture(), "textures/item/tide_codex_3d.png")
    g.book_models("tide_codex")


if __name__ == "__main__":
    main()
