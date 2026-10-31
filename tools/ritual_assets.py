#!/usr/bin/env python3
"""Ritual art: the Ritual Chalk (a stick of white chalk bound in violet cord, its tip glowing faintly), the chalk
strokes and the sixteen sigils the figures are drawn with (in chalk, and lit), the lights of the channelling, the
Portal's void, swirl, rim and amethyst shards, the menhirs, and the Altar of Rites (block and panel).

Called by gen_assets.py (run that one). Pixel style, shaded in bands. Original art.
"""
import math

import numpy as np
from PIL import Image, ImageDraw

import gen_assets as g
from water_assets import _tnoise, _quant, _rgba, _ramp

CHALK = (238, 234, 222)
CHALK_SHADE = (196, 190, 176)
CHALK_DARK = (150, 144, 132)
CORD = (122, 70, 170)
CORD_LIGHT = (170, 116, 220)
GLOW = (200, 170, 255)
OUTLINE = (40, 36, 44)


def ritual_chalk_icon():
    return g.from_grid([
        "................",
        "............gg..",
        "...........gwwg.",
        "..........owwso.",
        ".........owwso..",
        "........owwso...",
        ".......occso....",
        "......oCcso.....",
        ".....owwso......",
        "....owwso.......",
        "...owwso........",
        "..owwso.........",
        "..owso..........",
        "..oso...........",
        "...o............",
        "................",
    ], {"o": OUTLINE, "w": CHALK, "s": CHALK_SHADE, "c": CORD, "C": CORD_LIGHT, "g": GLOW})


# ----------------------------------------------------------------------------------------------- the chalk and sigils


def chalk_texture():
    """64 x 16, tiling along U: a stroke of chalk running along U, white (tinted in game), dense down its middle,
    grainy and broken at its edges, a few skips along it."""
    W, H = 64, 16
    ys, xs = np.mgrid[0:H, 0:W]
    v = (ys + .5) / H * 2 - 1
    wobble = (_tnoise(W, H, (4, 1), 501) - .5) * .35
    grain = _tnoise(W, H, (32, 8), 502) * .55 + _tnoise(W, H, (64, 16), 503) * .45
    skips = _tnoise(W, H, (6, 1), 504)
    body = np.clip(1 - np.abs(v - wobble) / .62, 0, 1) ** .6
    a = np.clip(body * 1.5 - (1 - grain) * .9 - np.clip(.32 - skips, 0, 1) * 2, 0, 1)
    shade = _quant(.75 + .25 * grain, 3)
    rgb = np.dstack([shade * 255] * 3)
    return _rgba(rgb, _quant(a, 4) * 255)


def chalk_glow_texture():
    """32 x 16, tiling along U: the light of a lit stroke, a bright thread down its middle in a soft halo, white on
    black (drawn added)."""
    W, H = 32, 16
    ys, xs = np.mgrid[0:H, 0:W]
    v = (ys + .5) / H * 2 - 1
    flick = .8 + .2 * _tnoise(W, H, (8, 1), 511)
    k = (np.exp(-v * v / .02) * .9 + np.exp(-v * v / .18) * .45) * flick
    return _rgba(np.dstack([np.clip(k, 0, 1) * 255] * 3), np.full((H, W), 255))


#: The sixteen sigils, as strokes in a unit square (y down): each a list of polylines; ("c", x, y, r) a circle,
#: ("a", x, y, r, from, to) an arc (degrees).
SIGILS = [
    # 0 the eye
    [("a", .5, .78, .42, 215, 325), ("a", .5, .22, .42, 35, 145), ("c", .5, .5, .1)],
    # 1 the star
    [[(.5, .1), (.74, .86), (.12, .38), (.88, .38), (.26, .86), (.5, .1)]],
    # 2 the moon
    [("a", .5, .5, .38, 60, 300), ("a", .66, .5, .3, 95, 265), ("c", .78, .5, .04)],
    # 3 the falling star
    [[(.5, .14), (.86, .8), (.14, .8), (.5, .14)], [(.5, .3), (.5, .9)], [(.3, .58), (.7, .58)]],
    # 4 the spiral
    [[(.5 + .04 * t * math.cos(t), .5 + .04 * t * math.sin(t)) for t in np.linspace(0, 10.5, 40)]],
    # 5 the spear
    [[(.5, .08), (.5, .92)], [(.24, .34), (.5, .08), (.76, .34)], [(.3, .62), (.7, .62)]],
    # 6 the bond
    [("c", .36, .5, .24), ("c", .64, .5, .24)],
    # 7 the trident
    [[(.5, .92), (.5, .2)], [(.2, .14), (.2, .42), (.8, .42), (.8, .14)]],
    # 8 the hourglass
    [[(.18, .12), (.82, .12), (.18, .88), (.82, .88), (.18, .12)]],
    # 9 the stone
    [[(.14, .86), (.36, .3), (.52, .56), (.68, .2), (.86, .86), (.14, .86)]],
    # 10 the tide
    [[(.1 + .8 * t, .3 + .06 * math.sin(t * 12)) for t in np.linspace(0, 1, 14)],
     [(.1 + .8 * t, .52 + .06 * math.sin(t * 12 + 1)) for t in np.linspace(0, 1, 14)],
     [(.1 + .8 * t, .74 + .06 * math.sin(t * 12 + 2)) for t in np.linspace(0, 1, 14)]],
    # 11 the key
    [("c", .5, .26, .16), [(.5, .42), (.5, .9)], [(.5, .66), (.7, .66)], [(.5, .8), (.66, .8)]],
    # 12 the sun
    [("c", .5, .5, .18)] + [[(.5 + .27 * math.cos(a), .5 + .27 * math.sin(a)), (.5 + .42 * math.cos(a), .5 + .42 * math.sin(a))]
                           for a in np.linspace(0, math.tau, 9)[:-1]],
    # 13 the gate
    [[(.2, .9), (.2, .4)] + [(.5 - .3 * math.cos(a), .4 - .3 * math.sin(a)) for a in np.linspace(0, math.pi, 10)] + [(.8, .9)], [(.5, .5), (.5, .9)]],
    # 14 the crystal
    [[(.5, .06), (.76, .3), (.76, .7), (.5, .94), (.24, .7), (.24, .3), (.5, .06)], [(.5, .06), (.5, .94)]],
    # 15 the bloom
    [("c", .5, .5, .08)] + [("a", .5 + .2 * math.cos(a), .5 + .2 * math.sin(a), .2, math.degrees(a) + 100, math.degrees(a) + 260)
                           for a in np.linspace(0, math.tau, 6)[:-1]],
]


def _sigil_mask(strokes, S=32, up=8, width=2.2):
    """A sigil drawn in strokes, S x S, anti-aliased then cut to the pixel grid."""
    big = Image.new("L", (S * up, S * up), 0)
    d = ImageDraw.Draw(big)
    w = int(width * up)
    pad = .1

    def P(x, y):
        return ((pad + x * (1 - 2 * pad)) * S * up, (pad + y * (1 - 2 * pad)) * S * up)

    for st in strokes:
        if isinstance(st, tuple) and st[0] == "c":
            _, x, y, r = st
            pts = [P(x + r * math.cos(a), y + r * math.sin(a)) for a in np.linspace(0, math.tau, 40)]
        elif isinstance(st, tuple) and st[0] == "a":
            _, x, y, r, a0, a1 = st
            pts = [P(x + r * math.cos(math.radians(a)), y + r * math.sin(math.radians(a))) for a in np.linspace(a0, a1, 30)]
        else:
            pts = [P(x, y) for x, y in st]
        d.line(pts, fill=255, width=w, joint="curve")
        for p in (pts[0], pts[-1]):
            d.ellipse([p[0] - w / 2, p[1] - w / 2, p[0] + w / 2, p[1] + w / 2], fill=255)
    return np.asarray(big.resize((S, S), Image.BOX), float) / 255


def sigils_texture():
    """128 x 128: the sixteen sigils (4 x 4) in chalk, white (tinted in game) and grainy."""
    S = 32
    out = np.zeros((128, 128, 4))
    for k, strokes in enumerate(SIGILS):
        m = _sigil_mask(strokes, S)
        grain = _tnoise(S, S, (16, 16), 520 + k) * .6 + _tnoise(S, S, (32, 32), 540 + k) * .4
        a = np.clip(m * 1.6 - (1 - grain) * .7, 0, 1)
        a = np.where(m > .25, _quant(a, 4), 0)
        ox, oy = (k % 4) * S, (k // 4) * S
        out[oy:oy + S, ox:ox + S, :3] = 255 * _quant(.8 + .2 * grain, 3)[..., None]
        out[oy:oy + S, ox:ox + S, 3] = a * 255
    return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), "RGBA")


def sigils_glow_texture():
    """128 x 128: the sixteen sigils (4 x 4) lit, a bright line in a soft halo, white on black (drawn added)."""
    S = 32
    out = np.zeros((128, 128, 3))
    for k, strokes in enumerate(SIGILS):
        core = _sigil_mask(strokes, S, width=1.6)
        halo = _sigil_mask(strokes, S, width=5)
        k2 = np.clip(core + halo * .35, 0, 1)
        ox, oy = (k % 4) * S, (k // 4) * S
        out[oy:oy + S, ox:ox + S] = (_quant(k2, 6) * 255)[..., None]
    return _rgba(out, np.full((128, 128), 255))


# -------------------------------------------------------------------------------------------------------- the lights


def glow_texture():
    """32 x 32 soft round light, white on black (drawn added)."""
    S = 32
    ys, xs = np.mgrid[0:S, 0:S]
    r = np.hypot((xs + .5) / S - .5, (ys + .5) / S - .5) * 2
    k = np.clip(1 - r, 0, 1) ** 2.4
    return _rgba(np.dstack([k * 255] * 3), np.full((S, S), 255))


def mote_texture():
    """16 x 16 mote of light: a bright point with four short rays, white on black (drawn added)."""
    S = 16
    ys, xs = np.mgrid[0:S, 0:S]
    x, y = (xs + .5) / S - .5, (ys + .5) / S - .5
    r = np.hypot(x, y) * 2
    k = np.clip(1 - r * 2.2, 0, 1) ** 1.5 + .7 * np.exp(-(x * x) / .0015) * np.clip(1 - r, 0, 1) ** 2 \
        + .7 * np.exp(-(y * y) / .0015) * np.clip(1 - r, 0, 1) ** 2
    return _rgba(np.dstack([_quant(np.clip(k, 0, 1), 6) * 255] * 3), np.full((S, S), 255))


def beam_texture():
    """16 x 16 shaft of light, white on black (drawn added): a bright core across U, its edges soft, rippling along V."""
    S = 16
    ys, xs = np.mgrid[0:S, 0:S]
    u = (xs + .5) / S - .5
    ripple = .85 + .15 * np.sin((ys / S) * math.tau * 2)
    k = (np.exp(-u * u / .006) + .5 * np.exp(-u * u / .05)) * ripple
    return _rgba(np.dstack([np.clip(k, 0, 1) * 255] * 3), np.full((S, S), 255))


def shock_texture():
    """64 x 64 ring of light racing out over the ground, its front sharp, its back trailing off (white on black)."""
    S = 64
    ys, xs = np.mgrid[0:S, 0:S]
    r = np.hypot((xs + .5) / S - .5, (ys + .5) / S - .5) * 2
    k = np.where(r < .95, np.exp(-(r - .95) ** 2 / .02), np.exp(-(r - .95) ** 2 / .0008)) * (r < 1)
    k *= .85 + .15 * _tnoise(S, S, (12, 12), 551)
    return _rgba(np.dstack([_quant(np.clip(k, 0, 1), 8) * 255] * 3), np.full((S, S), 255))


# --------------------------------------------------------------------------------------------------------- the portal


def portal_void_texture():
    """64 x 64: the far side seen through the Portal, deep violet night with a haze and pinpoint stars, opaque in its
    middle, thinning to its edge (a disc, U and V across it)."""
    S = 64
    ys, xs = np.mgrid[0:S, 0:S]
    x, y = (xs + .5) / S - .5, (ys + .5) / S - .5
    r = np.hypot(x, y) * 2
    haze = _tnoise(S, S, (5, 5), 561) * .6 + _tnoise(S, S, (10, 10), 562) * .4
    t = _quant(np.clip(.2 + .55 * haze - .25 * r, 0, 1), 6)
    rgb = _ramp(t, [(8, 4, 20), (24, 10, 52), (58, 24, 108), (110, 60, 180)])
    rnd = np.random.default_rng(563)
    for _ in range(26):
        sx, sy = rnd.integers(4, S - 4, 2)
        if math.hypot(sx - S / 2, sy - S / 2) < S * .42:
            rgb[sy, sx] = (255, 240, 255) if rnd.random() < .5 else (210, 190, 255)
    a = np.clip((1 - r) * 3, 0, 1)
    return _rgba(rgb, _quant(a, 4) * 235)


def portal_swirl_texture():
    """64 x 64: three arms of light wound in a spiral into the middle, white on black (drawn added)."""
    S = 64
    ys, xs = np.mgrid[0:S, 0:S]
    x, y = (xs + .5) / S - .5, (ys + .5) / S - .5
    r = np.hypot(x, y) * 2
    a = np.arctan2(y, x)
    arms = .5 + .5 * np.cos(3 * (a + r * 5.5))
    n = _tnoise(S, S, (8, 8), 571)
    k = arms ** 3 * np.clip(1 - r, 0, 1) ** .8 * np.clip(r * 4, 0, 1) * (.7 + .5 * n) + np.clip(1 - r * 5, 0, 1) ** 2
    return _rgba(np.dstack([_quant(np.clip(k, 0, 1), 8) * 255] * 3), np.full((S, S), 255))


def rim_texture():
    """32 x 16, tiling along U: the Portal's burning rim, a white-hot thread in a band of light that flickers along it,
    white on black (drawn added)."""
    W, H = 32, 16
    ys, xs = np.mgrid[0:H, 0:W]
    v = (ys + .5) / H * 2 - 1
    flick = _tnoise(W, H, (8, 2), 581)
    k = np.exp(-v * v / .01) + (.55 + .45 * flick) * np.exp(-v * v / (.08 + .12 * flick))
    return _rgba(np.dstack([_quant(np.clip(k, 0, 1), 8) * 255] * 3), np.full((H, W), 255))


def crystal_texture():
    """16 x 16 amethyst: facets in violet bands, pale glints at their edges (a little see-through)."""
    S = 16
    ys, xs = np.mgrid[0:S, 0:S]
    u, v = (xs + .5) / S, (ys + .5) / S
    facets = _quant(.5 + .3 * np.sin((u * 2 + v) * math.pi * 1.5) + .2 * (_tnoise(S, S, (4, 4), 591) - .5), 5)
    rgb = _ramp(facets, [(70, 30, 120), (122, 64, 190), (170, 112, 232), (214, 176, 255), (246, 228, 255)])
    edge = (np.abs(u - .5) > .4) | (np.abs(v - .5) > .4)
    rgb[edge] = rgb[edge] * .8 + np.array([255, 240, 255]) * .2
    return _rgba(rgb, np.full((S, S), 220))


def menhir_texture():
    """16 x 32 standing stone: rough grey stone in bands, cracks, a little lichen, darker at its foot."""
    W, H = 16, 32
    ys, xs = np.mgrid[0:H, 0:W]
    n = _tnoise(W, H, (4, 8), 601) * .6 + _tnoise(W, H, (8, 16), 602) * .4
    t = _quant(.35 + .5 * n - .25 * (ys / H) ** 2, 5)
    rgb = _ramp(t, [(56, 56, 60), (88, 88, 92), (118, 116, 118), (146, 144, 142), (170, 168, 164)])
    lichen = _tnoise(W, H, (6, 12), 603) > .74
    rgb[lichen] = rgb[lichen] * .4 + np.array([128, 150, 70]) * .6
    rnd = np.random.default_rng(604)
    for _ in range(3):
        x, y = int(rnd.integers(2, W - 2)), int(rnd.integers(2, H - 8))
        for k in range(int(rnd.integers(4, 8))):
            rgb[min(H - 1, y + k), max(0, min(W - 1, x))] = (40, 40, 44)
            x += int(rnd.integers(-1, 2))
    return _rgba(rgb, np.full((H, W), 255))


# --------------------------------------------------------------------------------------------------- the Altar of Rites


ALTAR_DARK = (36, 34, 44)
ALTAR_STONE = (62, 60, 72)
ALTAR_LIGHT = (92, 90, 104)
ALTAR_EDGE = (124, 122, 136)
ALTAR_RUNE = (190, 140, 255)


def _altar_stone(seed, w=16, h=16):
    n = _tnoise(w, h, (4, 4), seed) * .6 + _tnoise(w, h, (8, 8), seed + 1) * .4
    t = _quant(.3 + .55 * n, 4)
    return _ramp(t, [ALTAR_DARK, ALTAR_STONE, ALTAR_LIGHT, ALTAR_EDGE])


def altar_side_texture():
    """16 x 16: the altar's carved side, dark polished stone in courses, a violet rune glowing in a recess."""
    rgb = _altar_stone(611)
    rgb[0, :] = ALTAR_EDGE
    rgb[15, :] = ALTAR_DARK
    rgb[7, :] = rgb[7, :] * .7
    rgb[2:14, 3:13] = rgb[2:14, 3:13] * .7
    rune = _sigil_mask(SIGILS[14], 12, up=8, width=1.3) > .3
    for y in range(12):
        for x in range(10):
            if rune[y, x + 1]:
                rgb[2 + y, 3 + x] = ALTAR_RUNE
    return _rgba(rgb, np.full((16, 16), 255))


def altar_top_texture():
    """16 x 16: the altar's top, a slab of stone framed in a lighter edge."""
    rgb = _altar_stone(621)
    rgb[0, :] = rgb[15, :] = rgb[:, 0] = rgb[:, 15] = ALTAR_EDGE
    rgb[1, 1:15] = rgb[1:15, 1] = ALTAR_LIGHT
    return _rgba(rgb, np.full((16, 16), 255))


def altar_book_texture():
    """32 x 32: the open book on the altar (pages on top, leather below): two cream pages of sigils, a violet ribbon,
    and its binding."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Pages (top half): two pages side by side
    d.rectangle([0, 0, 31, 15], fill=g.CREAM)
    d.line([(15, 0), (15, 15)], fill=g.CREAM_SHADE)
    d.line([(16, 0), (16, 15)], fill=(196, 172, 120))
    for k, (ox, oy) in enumerate([(2, 2), (9, 2), (2, 9), (19, 2), (26, 2), (19, 9), (26, 9)]):
        m = _sigil_mask(SIGILS[(k * 5 + 3) % 16], 5, up=8, width=1.1) > .35
        for y in range(5):
            for x in range(5):
                if m[y, x]:
                    img.putpixel((ox + x, oy + y), (96, 60, 150, 255))
    for y in (11, 13):
        d.line([(9, y), (13, y)], fill=g.PARCH_LINE)
    d.line([(17, 15), (17, 8)], fill=(150, 80, 220))
    # Leather (bottom half)
    d.rectangle([0, 16, 31, 31], fill=(70, 34, 50))
    d.rectangle([0, 16, 31, 16], fill=(110, 60, 80))
    d.rectangle([14, 16, 17, 31], fill=(50, 24, 36))
    d.rectangle([4, 20, 11, 27], outline=(200, 160, 80))
    d.rectangle([20, 20, 27, 27], outline=(200, 160, 80))
    return img


def altar_candle_texture():
    """16 x 16: a candle of white wax with its flame (for the altar's two candles)."""
    return g.from_grid([
        "......ff........",
        ".....fyyf.......",
        ".....fYYf.......",
        "......yy........",
        "......kk........",
        ".....wwww.......",
        ".....wwws.......",
        ".....wwws.......",
        ".....wwws.......",
        ".....wwws.......",
        ".....wwss.......",
        ".....wwss.......",
        ".....wwss.......",
        "....owwsso......",
        "....oooooo......",
        "................",
    ], {"f": (255, 160, 40), "y": (255, 220, 90), "Y": (255, 255, 210), "k": (40, 30, 30), "w": (238, 232, 218),
        "s": (200, 192, 176), "o": (90, 84, 96)})


def altar_panel_texture():
    """256 x 200: the panel the altar opens: an old page framed in dark wood with brass corners, the list of rites on
    its left, their page on its right, a seal at its foot."""
    W, H = 256, 200
    ys, xs = np.mgrid[0:H, 0:W]
    n = _tnoise(W, H, (16, 12), 631) * .6 + _tnoise(W, H, (48, 36), 632) * .4
    t = _quant(.55 + .35 * (n - .5) - .25 * (np.minimum(np.minimum(xs, W - 1 - xs), np.minimum(ys, H - 1 - ys)) < 16) * 0, 5)
    rgb = _ramp(t, [(196, 170, 120), (214, 192, 142), (230, 212, 166), (240, 226, 186), (248, 238, 204)])
    # Darkening toward the edges of the page
    edge = np.minimum(np.minimum(xs, W - 1 - xs), np.minimum(ys, H - 1 - ys))
    rgb = rgb * (.82 + .18 * np.clip(edge / 24, 0, 1))[..., None]
    img = _rgba(rgb, np.full((H, W), 255))
    d = ImageDraw.Draw(img)
    wood, wood_l, wood_d = (74, 46, 30), (104, 68, 42), (46, 28, 18)
    for k in range(6):
        c = wood_d if k in (0, 5) else (wood_l if k == 1 else wood)
        d.rectangle([k, k, W - 1 - k, H - 1 - k], outline=c)
    brass, brass_l = (176, 132, 52), (232, 196, 100)
    for cx, cy in ((0, 0), (W - 14, 0), (0, H - 14), (W - 14, H - 14)):
        d.rectangle([cx, cy, cx + 13, cy + 13], fill=brass, outline=wood_d)
        d.rectangle([cx + 3, cy + 3, cx + 10, cy + 10], outline=brass_l)
    # The list's column and the page's divider
    d.rectangle([12, 26, 90, H - 34], fill=(222, 202, 156), outline=(150, 118, 76))
    d.line([(98, 26), (98, H - 34)], fill=(150, 118, 76))
    d.line([(99, 26), (99, H - 34)], fill=(236, 220, 180))
    # Headband
    d.line([(12, 21), (W - 13, 21)], fill=(150, 118, 76))
    d.line([(12, 22), (W - 13, 22)], fill=(236, 220, 180))
    # A wax seal at the foot, left
    d.ellipse([18, H - 28, 34, H - 12], fill=(122, 48, 160), outline=(70, 24, 96))
    d.ellipse([22, H - 24, 30, H - 16], outline=(186, 130, 230))
    return img


def altar_block_model():
    """The Altar of Rites: a stepped plinth of dark stone, a column, a slanted top holding an open book, two candles."""
    def el(frm, to, faces, rotation=None):
        e = {"from": frm, "to": to, "faces": faces}
        if rotation:
            e["rotation"] = rotation
        return e

    def box(tex, top=None, uv=None):
        f = {}
        for side in ("north", "south", "east", "west", "down"):
            f[side] = {"texture": tex} if uv is None else {"texture": tex, "uv": uv}
        f["up"] = {"texture": top or tex}
        return f

    return {
        "parent": "block/block",
        "render_type": "minecraft:cutout",
        "textures": {"particle": "arcanes_oublies:block/altar_of_rites_side", "side": "arcanes_oublies:block/altar_of_rites_side",
                     "top": "arcanes_oublies:block/altar_of_rites_top", "book": "arcanes_oublies:block/altar_of_rites_book",
                     "candle": "arcanes_oublies:block/altar_of_rites_candle"},
        "elements": [
            el([0, 0, 0], [16, 3, 16], box("#side", "#top")),
            el([2, 3, 2], [14, 5, 14], box("#side", "#top")),
            el([4, 5, 4], [12, 11, 12], box("#side", "#top")),
            el([1, 11, 2], [15, 13, 14], box("#side", "#top"), {"origin": [8, 12, 8], "axis": "x", "angle": -22.5}),
            el([2, 13, 3.5], [14, 14, 12.5], {
                "up": {"texture": "#book", "uv": [0, 0, 16, 8]},
                "down": {"texture": "#book", "uv": [0, 8, 16, 16]},
                "north": {"texture": "#book", "uv": [0, 8, 16, 8.5]},
                "south": {"texture": "#book", "uv": [0, 8, 16, 8.5]},
                "east": {"texture": "#book", "uv": [0, 8, 4.5, 8.5]},
                "west": {"texture": "#book", "uv": [0, 8, 4.5, 8.5]}}, {"origin": [8, 12, 8], "axis": "x", "angle": -22.5}),
            el([0.5, 3, 0.5], [2.5, 8, 2.5], {
                "north": {"texture": "#candle", "uv": [5, 5, 7, 10]}, "south": {"texture": "#candle", "uv": [5, 5, 7, 10]},
                "east": {"texture": "#candle", "uv": [7, 5, 9, 10]}, "west": {"texture": "#candle", "uv": [7, 5, 9, 10]},
                "up": {"texture": "#candle", "uv": [5, 5, 7, 7]}}),
            el([13.5, 3, 13.5], [15.5, 7, 15.5], {
                "north": {"texture": "#candle", "uv": [5, 6, 7, 10]}, "south": {"texture": "#candle", "uv": [5, 6, 7, 10]},
                "east": {"texture": "#candle", "uv": [7, 6, 9, 10]}, "west": {"texture": "#candle", "uv": [7, 6, 9, 10]},
                "up": {"texture": "#candle", "uv": [5, 5, 7, 7]}}),
            el([1, 8, 1.5], [2, 10, 1.5], {"north": {"texture": "#candle", "uv": [5, 0, 7, 3]}, "south": {"texture": "#candle", "uv": [5, 0, 7, 3]}}),
            el([14, 7, 14.5], [15, 9, 14.5], {"north": {"texture": "#candle", "uv": [5, 0, 7, 3]}, "south": {"texture": "#candle", "uv": [5, 0, 7, 3]}}),
        ],
    }


def main():
    g.save(ritual_chalk_icon(), "textures/item/ritual_chalk.png")
    g.save(chalk_texture(), "textures/entity/ritual/chalk.png")
    g.save(chalk_glow_texture(), "textures/entity/ritual/chalk_glow.png")
    g.save(sigils_texture(), "textures/entity/ritual/sigils.png")
    g.save(sigils_glow_texture(), "textures/entity/ritual/sigils_glow.png")
    g.save(glow_texture(), "textures/entity/ritual/glow.png")
    g.save(mote_texture(), "textures/entity/ritual/mote.png")
    g.save(beam_texture(), "textures/entity/ritual/beam.png")
    g.save(shock_texture(), "textures/entity/ritual/shock.png")
    g.save(portal_void_texture(), "textures/entity/ritual/portal_void.png")
    g.save(portal_swirl_texture(), "textures/entity/ritual/portal_swirl.png")
    g.save(rim_texture(), "textures/entity/ritual/rim.png")
    g.save(crystal_texture(), "textures/entity/ritual/crystal.png")
    g.save(menhir_texture(), "textures/entity/ritual/menhir.png")
    g.save(altar_side_texture(), "textures/block/altar_of_rites_side.png")
    g.save(altar_top_texture(), "textures/block/altar_of_rites_top.png")
    g.save(altar_book_texture(), "textures/block/altar_of_rites_book.png")
    g.save(altar_candle_texture(), "textures/block/altar_of_rites_candle.png")
    g.save(altar_panel_texture(), "textures/gui/altar_of_rites.png")
    g.write_json("models/block/altar_of_rites.json", altar_block_model())
    g.write_json("models/item/altar_of_rites.json", {"parent": "arcanes_oublies:block/altar_of_rites"})
    g.write_json("blockstates/altar_of_rites.json", {"variants": {
        f"facing={f}": {"model": "arcanes_oublies:block/altar_of_rites", "y": y} for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    g.write_json("models/item/ritual_chalk.json", {"parent": "item/handheld", "textures": {"layer0": "arcanes_oublies:item/ritual_chalk"}})


if __name__ == "__main__":
    main()
