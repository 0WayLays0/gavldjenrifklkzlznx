#!/usr/bin/env python3
"""Wood Magic art: the bark, vine and leaf textures the spells' wood is drawn with, the spell icons, the Sylvan Codex
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


def _tree(d, S, x, base, h, crown, seed, lean=0.0):
    top_x, top_y = x + lean * h, base - h
    _log(d, S, x, base, top_x, top_y + crown * .3, .07 + h * .05, ring=False)
    for s in (-1, 1):
        d.line((x * S, (base - .01) * S, (x + s * .07) * S, (base + .02) * S), fill=_rgba(BARK), width=max(2, S // 28))
    _crown(d, S, top_x, top_y, crown, seed)


def _ground(d, S, y=.86):
    d.rectangle((0, y * S, S, S), fill=(34, 26, 14, 255))
    d.line((0, y * S, S, y * S), fill=_rgba(LIFE), width=max(1, S // 32))


def _thorny(d, S, pts, w):
    d.line([(x * S, y * S) for x, y in pts], fill=_rgba((72, 140, 40)), width=max(2, int(w * S)), joint="curve")
    for i in range(1, len(pts) - 1, 2):
        x, y = pts[i]
        d.polygon([((x - .015) * S, y * S), ((x + .015) * S, y * S), ((x + .02) * S, (y - .06) * S)], fill=_rgba((110, 82, 54)))


def log_hurl_icon():
    def draw(d, img, S):
        for k in range(4):
            t = k / 4
            d.line(((.06 + t * .2) * S, (.86 - t * .16) * S, (.16 + t * .2) * S, (.8 - t * .16) * S), fill=_rgba(LIFE, 90 + 35 * k), width=max(1, S // 22))
        _log(d, S, .22, .76, .78, .3, .2)
        _leaf(d, S, .5, .44, .14, -2.3, LEAF_LIGHT)
    return _icon(draw, .6, .4)


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


def barkskin_icon():
    def draw(d, img, S):
        d.ellipse((.4 * S, .14 * S, .6 * S, .32 * S), fill=(150, 120, 100, 255))
        for k in range(3):
            y0 = .32 + k * .19
            for i in range(4):
                x0 = .26 + i * .12 + (k % 2) * .03
                d.rounded_rectangle((x0 * S, y0 * S, (x0 + .11) * S, (y0 + .18) * S), radius=max(1, S // 32), fill=_rgba(g.lerp(BARK, BARK_LIGHT, (i + k) % 3 / 2)),
                                    outline=_rgba(BARK_DARK))
        for x in (.37, .49, .61):
            d.line((x * S, .34 * S, x * S, .88 * S), fill=_rgba(LIFE, 200), width=max(1, S // 48))
        for x, y in ((.22, .42), (.78, .5), (.2, .7), (.8, .74)):
            d.polygon([(x * S, y * S), ((x + (.06 if x < .5 else -.06)) * S, (y - .02) * S), (x * S, (y + .04) * S)], fill=_rgba((110, 82, 54)))
    return _icon(draw, .5, .5)


def falling_tree_icon():
    def draw(d, img, S):
        _ground(d, S, .88)
        d.arc((.12 * S, .1 * S, 1.3 * S, 1.6 * S), 200, 250, fill=_rgba(LIFE, 160), width=max(1, S // 40))
        _log(d, S, .2, .88, .6, .36, .11, ring=False)
        _crown(d, S, .7, .3, .2, 7)
        for k in range(3):
            d.line(((.62 + k * .08) * S, (.06 + k * .02) * S, (.7 + k * .08) * S, (.12 + k * .02) * S), fill=_rgba(LIFE_CORE, 160), width=max(1, S // 48))
    return _icon(draw, .6, .5)


def leaf_storm_icon():
    def draw(d, img, S):
        for k in range(26):
            t = k / 25
            a = t * math.tau * 2.4
            r = .08 + .34 * t
            x, y = .5 + math.cos(a) * r, .9 - t * .78 + math.sin(a) * r * .25
            _leaf(d, S, x, y, .07 + .05 * t, a + 1.4, PINK if k % 7 == 3 else g.lerp(LEAF_DARK, LEAF_LIGHT, (k * 37 % 10) / 10))
    return _icon(draw, .5, .5)


def sudden_grove_icon():
    def draw(d, img, S):
        _ground(d, S, .84)
        d.ellipse((.06 * S, .76 * S, .94 * S, .94 * S), outline=_rgba(LIFE, 200), width=max(1, S // 32))
        _tree(d, S, .2, .84, .42, .14, 3)
        _tree(d, S, .8, .84, .44, .14, 4)
        _tree(d, S, .5, .86, .58, .18, 5)
    return _icon(draw, .5, .6)


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
         ("bramble_hedge", bramble_hedge_icon), ("barkskin", barkskin_icon), ("falling_tree", falling_tree_icon),
         ("leaf_storm", leaf_storm_icon), ("sudden_grove", sudden_grove_icon), ("wood_guardian", wood_guardian_icon),
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
    for name, icon in ICONS:
        g.save(icon(), f"textures/gui/spell_icons/{name}.png")
    g.save(affinity_ring_wood(), "textures/item/affinity_ring_wood.png")
    g.write_json("models/item/affinity_ring_wood.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/affinity_ring_wood"}})
    g.save(sylvan_codex_icon(), "textures/item/sylvan_codex.png")
    g.save(sylvan_codex_model_texture(), "textures/item/sylvan_codex_3d.png")
    g.book_models("sylvan_codex")


if __name__ == "__main__":
    main()
