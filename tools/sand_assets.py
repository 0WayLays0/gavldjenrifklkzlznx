#!/usr/bin/env python3
"""Sand Magic art: flowing sand, sandstone, glass, the shell of a scarab, chased gold, a band of glowing hieroglyphs, the
swirl of sand on the ground, the spell icons, the Codex of Dunes (icon and 3D book texture) and its affinity ring.

Called by gen_assets.py (run that one). Pixel style, shaded in bands. Original art.
"""
import math
import random

import numpy as np
from PIL import Image, ImageDraw

import gen_assets as g
from water_assets import _tnoise, _quant, _rgba, _ramp

#: Sand pale and deep, sandstone, its shadow, desert gold, the turquoise of old glass and faience.
SAND_PALE = (246, 226, 170)
SAND = (224, 184, 102)
SANDSTONE = (206, 160, 92)
UMBER = (120, 78, 40)
NIGHT = (40, 24, 12)
GOLD = (255, 204, 80)
TURQUOISE = (64, 200, 196)
GLASS = (200, 240, 236)


def sandstone_texture():
    """32 x 32, tiling: carved sandstone, layered in bands across, weathered, chiselled joints between its blocks."""
    S = 32
    ys, xs = np.mgrid[0:S, 0:S]
    n = _tnoise(S, S, (8, 8), 1301)
    bands = .5 + .5 * np.sin(ys / S * math.tau * 4 + n * 2)
    joint = ((ys % 16 == 0) | ((xs + (ys // 16) * 8) % 16 == 0)) * 1.0
    t = np.clip(.45 + .25 * bands + .2 * (n - .5) - .35 * joint, 0, 1)
    rgb = _ramp(_quant(t, 7), [UMBER, SANDSTONE, SAND, SAND_PALE])
    return _rgba(rgb, np.full((S, S), 255.0))


def glass_texture():
    """32 x 32: a shard of glass, white (tinted in game) and see-through, bright along its edges and a streak of shine."""
    S = 32
    ys, xs = np.mgrid[0:S, 0:S]
    u, v = (xs + .5) / S, (ys + .5) / S
    edge = np.maximum(np.exp(-(np.minimum(u, 1 - u) / .06) ** 2), np.exp(-(np.minimum(v, 1 - v) / .06) ** 2))
    shine = np.exp(-(((u - v) - .1) / .05) ** 2) * .8
    a = np.clip(.3 + .55 * edge + .4 * shine, 0, 1)
    t = np.clip(.75 + .25 * edge + .25 * shine, 0, 1)
    return _rgba(np.dstack([t * 255] * 3), _quant(a, 6) * 255)


def sand_flow_texture():
    """64 x 128 flowing sand (V runs along the flow and tiles): streaks of pale and deep sand, grains glinting, see-through
    toward its edges (U) so a ribbon or a tube of it melts into the air."""
    W, H = 64, 128
    ys, xs = np.mgrid[0:H, 0:W]
    u = (xs + .5) / W
    streak = _tnoise(W, H, (16, 2), 1311)
    fine = _tnoise(W, H, (32, 8), 1312)
    rnd = np.random.default_rng(1313)
    glint = (rnd.random((H, W)) < .02).astype(np.float32)
    t = np.clip(.3 + .45 * streak + .2 * fine + .3 * glint, 0, 1)
    rgb = _ramp(_quant(t, 9), [UMBER, SANDSTONE, SAND, SAND_PALE, (255, 246, 214)])
    edge = np.clip(np.sin(np.pi * u) * 1.6, 0, 1) ** .8
    a = _quant(np.clip(edge * (.65 + .35 * streak), 0, 1), 8) * 255
    return _rgba(rgb, a)


def scarab_texture():
    """32 x 32 the shell of a scarab, wrapped round its body (U round it, V from its head to its tail): deep blue-green
    shining to gold along its back, the line between its wing cases, small dimples."""
    S = 32
    ys, xs = np.mgrid[0:S, 0:S]
    u, v = (xs + .5) / S, (ys + .5) / S
    back = np.exp(-((u - .25) / .2) ** 2) + np.exp(-((u - 1.25) / .2) ** 2)
    seam = np.exp(-((u - .25) / .02) ** 2) * (v > .3)
    n = _tnoise(S, S, (8, 8), 1321)
    t = np.clip(.25 + .55 * back + .15 * n - .5 * seam - .2 * (v < .25), 0, 1)
    rgb = _ramp(_quant(t, 8), [(10, 30, 40), (18, 84, 96), (40, 150, 130), (200, 170, 70), (255, 226, 130)])
    return _rgba(rgb, np.full((S, S), 255.0))


def gold_texture():
    """32 x 32 chased gold: warm gold, a band of engraved zigzag across it, bright edges."""
    S = 32
    ys, xs = np.mgrid[0:S, 0:S]
    n = _tnoise(S, S, (4, 4), 1331)
    zig = np.abs(((xs + np.abs((ys % 8) - 4)) % 8) - 4) < 1
    band = (ys > 10) & (ys < 22)
    t = np.clip(.55 + .3 * n - .35 * (zig & band) + .2 * ((ys == 10) | (ys == 21)), 0, 1)
    rgb = _ramp(_quant(t, 7), [(110, 70, 20), (190, 130, 40), GOLD, (255, 240, 170)])
    return _rgba(rgb, np.full((S, S), 255.0))


def hieroglyphs_texture():
    """64 x 16: a band of glyphs of an old desert script, white on black (drawn as golden light)."""
    W, H = 64, 16
    img = Image.new("RGB", (W, H), (0, 0, 0))
    d = ImageDraw.Draw(img)
    rnd = random.Random(1304)
    x = 2
    while x < W - 6:
        k = rnd.randrange(6)
        if k == 0:
            d.ellipse((x, 3, x + 5, 8), outline=(255, 255, 255))
            d.line((x + 2, 8, x + 2, 13), fill=(255, 255, 255))
        elif k == 1:
            d.line((x, 12, x + 3, 3, x + 6, 12), fill=(255, 255, 255))
        elif k == 2:
            d.line((x, 4, x + 5, 4), fill=(255, 255, 255))
            d.line((x + 2, 4, x + 2, 13), fill=(255, 255, 255))
            d.line((x, 13, x + 5, 13), fill=(255, 255, 255))
        elif k == 3:
            d.arc((x, 3, x + 6, 13), 90, 270, fill=(255, 255, 255))
            d.point((x + 4, 8), fill=(255, 255, 255))
        elif k == 4:
            d.ellipse((x + 1, 6, x + 5, 10), fill=(255, 255, 255))
            d.line((x, 12, x + 6, 12), fill=(255, 255, 255))
        else:
            d.line((x, 3, x + 5, 13), fill=(255, 255, 255))
            d.line((x + 5, 3, x, 13), fill=(255, 255, 255))
        x += 8
    d.line((0, 0, W, 0), fill=(255, 255, 255))
    d.line((0, H - 1, W, H - 1), fill=(255, 255, 255))
    return img.convert("RGBA")


def swirl_texture():
    """64 x 64: quicksand seen from above, sand swirling in to a dark middle, drawn see-through at its edge."""
    S = 64
    ys, xs = np.mgrid[0:S, 0:S]
    dx, dy = xs + .5 - S / 2, ys + .5 - S / 2
    r = np.hypot(dx, dy) / (S / 2)
    a = np.arctan2(dy, dx)
    spiral = .5 + .5 * np.sin(a * 3 + r * 9)
    n = _tnoise(S, S, (8, 8), 1305)
    t = np.clip(.25 + .45 * spiral * r + .25 * n + .2 * r, 0, 1)
    rgb = _ramp(_quant(t, 7), [NIGHT, UMBER, SANDSTONE, SAND, SAND_PALE])
    alpha = np.clip((1 - r) * 5, 0, 1) * 255
    return _rgba(rgb, alpha)


# ------------------------------------------------------------------------------------------------------- the icons

def _icon(draw, cx=.5, cy=.5, bg=(110, 74, 34)):
    return g.painted(64, draw, (cx, cy, bg, NIGHT), 16)


def _a(c, a=255):
    return tuple(int(x) for x in c) + (a,)


def _grains(d, S, pts, r, col, alpha=255):
    for x, y in pts:
        d.ellipse(((x - r) * S, (y - r) * S, (x + r) * S, (y + r) * S), fill=_a(col, alpha))


def sand_lance_icon():
    def draw(d, img, S):
        rnd = random.Random(2)
        x0, y0, x1, y1 = .12, .88, .82, .18
        for k in range(3):
            pts = []
            for i in range(21):
                t = i / 20
                x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
                w = .07 * (1 - t * .8)
                a = t * 14 + k * 2.1
                pts.append(((x + math.cos(a) * w * .7) * S, (y + math.sin(a) * w * .7) * S))
            d.line(pts, fill=_a((SAND_PALE, SAND, SANDSTONE)[k]), width=max(2, S // 22))
        d.polygon([(.82 * S, .18 * S), (.95 * S, .05 * S), (.86 * S, .26 * S)], fill=_a(GLASS, 230), outline=_a((255, 255, 255)))
        _grains(d, S, [(x0 - .03 + rnd.random() * .2, y0 - .05 + rnd.random() * .15) for _ in range(10)], .015, SAND_PALE)
        g.glow_spot(img, .9 * S, .1 * S, .1 * S, (255, 250, 230), .8)
    return _icon(draw, .5, .5)


def _scarab(d, S, x, y, sc, ang):
    ca, sa = math.cos(ang), math.sin(ang)

    def at(px, py):
        return ((x + (px * ca - py * sa) * sc) * S, (y + (px * sa + py * ca) * sc) * S)
    for side in (-1, 1):
        for k in range(3):
            d.line([at(side * .3, -.3 + k * .3), at(side * .62, -.45 + k * .38)], fill=_a(NIGHT), width=max(1, S // 64))
    d.polygon([at(0, -.75), at(.42, -.25), at(.4, .45), at(0, .8), at(-.4, .45), at(-.42, -.25)], fill=_a((30, 120, 120)))
    d.polygon([at(0, -.55), at(.25, -.1), at(.22, .4), at(0, .6)], fill=_a((210, 176, 72)))
    d.line([at(0, -.5), at(0, .75)], fill=_a(NIGHT), width=max(1, S // 64))
    d.polygon([at(-.2, -.66), at(-.24, -.86), at(0, -1.0), at(.24, -.86), at(.2, -.66)], fill=_a((24, 60, 70)))


def scarab_swarm_icon():
    def draw(d, img, S):
        for x, y, sc, a in ((.32, .64, .22, -.5), (.66, .38, .2, .4), (.7, .76, .14, 1.2), (.26, .26, .12, -1.0)):
            _scarab(d, S, x, y, sc, a)
        _grains(d, S, [(.5, .55), (.45, .45), (.55, .2), (.15, .5)], .015, GOLD)
    return _icon(draw, .5, .5, (60, 70, 50))


def desert_grasp_icon():
    def draw(d, img, S):
        d.pieslice((-.1 * S, .72 * S, 1.1 * S, 1.4 * S), 180, 360, fill=_a(SANDSTONE))
        d.rounded_rectangle((.32 * S, .5 * S, .7 * S, .86 * S), radius=int(.1 * S), fill=_a(SAND))
        for k, (x, h) in enumerate(((.34, .22), (.43, .12), (.52, .1), (.61, .16))):
            d.rounded_rectangle((x * S, h * S, (x + .08) * S, .6 * S), radius=int(.04 * S), fill=_a(SAND if k % 2 else SAND_PALE), outline=_a(UMBER))
        d.rounded_rectangle((.66 * S, .46 * S, .84 * S, .56 * S), radius=int(.04 * S), fill=_a(SAND_PALE), outline=_a(UMBER))
        _grains(d, S, [(.3, .4), (.75, .3), (.25, .62), (.8, .66), (.5, .05)], .02, SAND_PALE)
    return _icon(draw, .5, .55)


def sand_veil_icon():
    def draw(d, img, S):
        d.ellipse((.42 * S, .18 * S, .58 * S, .34 * S), fill=_a((90, 70, 120)))
        d.rounded_rectangle((.4 * S, .34 * S, .6 * S, .78 * S), radius=int(.05 * S), fill=_a((90, 70, 120)))
        for k in range(3):
            r = .3 + k * .07
            d.arc(((.5 - r) * S, (.5 - r * .35) * S, (.5 + r) * S, (.5 + r * .35) * S), 160 + k * 40, 380 + k * 40, fill=_a((SAND_PALE, SAND, SANDSTONE)[k]),
                  width=max(2, S // 20))
        _grains(d, S, [(.12, .5), (.88, .44), (.2, .3), (.82, .7)], .02, SAND_PALE)
    return _icon(draw, .5, .5)


def hourglass_icon():
    def draw(d, img, S):
        d.rectangle((.24 * S, .06 * S, .76 * S, .12 * S), fill=_a(GOLD))
        d.rectangle((.24 * S, .88 * S, .76 * S, .94 * S), fill=_a(GOLD))
        d.polygon([(.3 * S, .12 * S), (.7 * S, .12 * S), (.53 * S, .5 * S), (.7 * S, .88 * S), (.3 * S, .88 * S), (.47 * S, .5 * S)], fill=_a(GLASS, 150),
                  outline=_a((255, 255, 255)))
        d.polygon([(.37 * S, .28 * S), (.63 * S, .28 * S), (.5 * S, .48 * S)], fill=_a(SAND))
        d.line([(.5 * S, .48 * S), (.5 * S, .8 * S)], fill=_a(SAND_PALE), width=max(1, S // 40))
        d.polygon([(.34 * S, .88 * S), (.66 * S, .88 * S), (.5 * S, .72 * S)], fill=_a(SANDSTONE))
        for x in (.27, .73):
            d.line([(x * S, .12 * S), (x * S, .88 * S)], fill=_a(GOLD), width=max(2, S // 28))
    return _icon(draw, .5, .5, (60, 70, 90))


def dune_serpent_icon():
    def draw(d, img, S):
        d.pieslice((-.1 * S, .74 * S, 1.1 * S, 1.4 * S), 180, 360, fill=_a(SANDSTONE))
        pts = [((.15 + .6 * t) * S, (.8 - .55 * t + .12 * math.sin(t * 9)) * S) for t in [i / 16 for i in range(17)]]
        for i in range(len(pts) - 1):
            d.line([pts[i], pts[i + 1]], fill=_a(SAND if i % 2 else SAND_PALE), width=max(2, int(S * (.12 - .05 * i / 16))))
        hx, hy = pts[-1]
        d.polygon([(hx - .06 * S, hy + .04 * S), (hx + .16 * S, hy - .08 * S), (hx + .02 * S, hy - .1 * S)], fill=_a(SAND_PALE), outline=_a(UMBER))
        d.ellipse((hx + .02 * S, hy - .08 * S, hx + .06 * S, hy - .04 * S), fill=_a(TURQUOISE))
    return _icon(draw, .5, .5)


def sun_lens_icon():
    def draw(d, img, S):
        g.glow_spot(img, .5 * S, .1 * S, .2 * S, (255, 236, 160), 1)
        d.polygon([(.3 * S, .34 * S), (.7 * S, .34 * S), (.52 * S, .86 * S), (.48 * S, .86 * S)], fill=_a((255, 230, 150), 120))
        d.ellipse((.22 * S, .3 * S, .78 * S, .4 * S), fill=_a(GLASS, 200), outline=_a(GOLD), width=max(1, S // 28))
        d.ellipse((.4 * S, .82 * S, .6 * S, .92 * S), fill=_a((255, 140, 40)))
        g.glow_spot(img, .5 * S, .87 * S, .1 * S, (255, 200, 80), 1)
        d.rectangle((0, .9 * S, S, S), fill=_a(SAND))
    return _icon(draw, .5, .4)


def sandfall_icon():
    def draw(d, img, S):
        d.ellipse((.2 * S, .04 * S, .8 * S, .18 * S), fill=_a(NIGHT), outline=_a(SAND_PALE))
        for k in range(5):
            x = .38 + k * .06
            d.line([(x * S, .12 * S), ((x + .02 * (k - 2)) * S, .8 * S)], fill=_a(SAND if k % 2 else SAND_PALE), width=max(2, S // 22))
        d.pieslice((.1 * S, .72 * S, .9 * S, 1.2 * S), 180, 360, fill=_a(SANDSTONE))
        _grains(d, S, [(.25, .7), (.75, .68), (.3, .62), (.7, .6)], .025, SAND_PALE)
    return _icon(draw, .5, .3, (70, 60, 80))


def sand_djinn_icon():
    def draw(d, img, S):
        for k in range(5):
            y = .6 + k * .07
            r = .2 - k * .035
            d.arc(((.5 - r) * S, (y - .03) * S, (.5 + r) * S, (y + .03) * S), 0, 300, fill=_a(SAND if k % 2 else SAND_PALE), width=max(2, S // 24))
        d.ellipse((.34 * S, .3 * S, .66 * S, .62 * S), fill=_a(SAND))
        d.ellipse((.4 * S, .12 * S, .6 * S, .32 * S), fill=_a(SAND_PALE))
        d.line([(.36 * S, .4 * S), (.16 * S, .26 * S)], fill=_a(SAND), width=max(2, S // 14))
        d.line([(.64 * S, .4 * S), (.84 * S, .3 * S)], fill=_a(SAND), width=max(2, S // 14))
        for x in (.46, .54):
            d.ellipse(((x - .02) * S, .2 * S, (x + .02) * S, .24 * S), fill=_a(TURQUOISE))
            g.glow_spot(img, x * S, .22 * S, .05 * S, TURQUOISE, .9)
        d.rectangle((.15 * S, .27 * S, .2 * S, .31 * S), fill=_a(GOLD))
    return _icon(draw, .5, .45, (70, 60, 80))


def simoom_icon():
    def draw(d, img, S):
        for k in range(8):
            y = .1 + k * .1
            r = .4 - k * .045
            x = .5 + math.sin(k * .7) * .05
            d.arc(((x - r) * S, (y - .05) * S, (x + r) * S, (y + .05) * S), 10, 330, fill=_a(SAND_PALE if k % 2 else SAND), width=max(2, S // 20))
        d.line([(.3 * S, .2 * S), (.36 * S, .36 * S), (.3 * S, .4 * S), (.38 * S, .58 * S)], fill=_a((220, 250, 255)), width=max(1, S // 40))
        g.glow_spot(img, .36 * S, .4 * S, .08 * S, (200, 240, 255), .8)
        d.rectangle((0, .92 * S, S, S), fill=_a(SANDSTONE))
    return _icon(draw, .5, .5, (90, 70, 50))


ICONS = (("sand_lance", sand_lance_icon), ("scarab_swarm", scarab_swarm_icon), ("desert_grasp", desert_grasp_icon), ("sand_veil", sand_veil_icon),
         ("hourglass", hourglass_icon), ("dune_serpent", dune_serpent_icon), ("sun_lens", sun_lens_icon), ("sandfall", sandfall_icon),
         ("sand_djinn", sand_djinn_icon), ("simoom", simoom_icon))


# --------------------------------------------------------------------------------------------------- the spellbook

COVER = (176, 128, 64)
COVER_LIGHT = (214, 168, 96)
COVER_DARK = (110, 74, 34)


def dunes_codex_icon():
    return g.from_grid([
        "................",
        "...oooooooooo...",
        "..obbbbbbbbbdco.",
        "..obsbbGbbbbdco.",
        "..obbbGTGbbbdco.",
        "..obbGTTTGbbdcco",
        "..obGTTTTTGbdco.",
        "..obbbbbbbbbdcco",
        "..obsssssssbdcco",
        "..obbbbbbbsbdcco",
        "..obbbbbbbbbdco.",
        "..oddssdddssxco.",
        "..oddddssdddxco.",
        "...ooooooooooo..",
        "................",
        "................",
    ], {"o": (40, 24, 12), "b": COVER, "s": TURQUOISE, "d": COVER_DARK, "x": (30, 18, 10), "G": GOLD, "T": SAND_PALE, "c": g.CREAM})


def dunes_codex_model_texture():
    """64x64 texture of the 3D book, laid out like the other books: covers of sand-coloured leather framed in turquoise
    faience, a golden pyramid under a sun on the front, a band of glyphs, a gold spine, old pages."""
    s = 4
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    W, H = 12 * s, 9 * s
    grain = _tnoise(W, H, (6, 4), 1311)
    cover = _ramp(.3 + .4 * grain, [COVER_DARK, COVER, COVER_LIGHT])
    img.paste(_rgba(cover, np.full((H, W), 255)), (0, 0))
    d.rectangle((1, 1, W - 2, H - 2), outline=TURQUOISE + (255,))
    cx, cy = W // 2, H // 2
    d.polygon([(cx - 12, cy + 9), (cx + 12, cy + 9), (cx, cy - 7)], fill=GOLD + (255,))
    d.polygon([(cx, cy + 9), (cx + 12, cy + 9), (cx, cy - 7)], fill=(214, 160, 50, 255))
    d.ellipse((cx + 8, cy - 13, cx + 14, cy - 7), fill=SAND_PALE + (255,))
    glyphs = hieroglyphs_texture().resize((W - 6, 6), Image.NEAREST)
    gp = glyphs.load()
    px = img.load()
    for y in range(6):
        for x in range(W - 6):
            if gp[x, y][0] > 128:
                px[3 + x, H - 9 + y] = TURQUOISE + (255,)
    d.rectangle((0, 0, W - 1, H - 1), outline=(40, 24, 12, 255))
    d.rectangle((12 * s, 0, 13 * s - 1, 9 * s - 1), fill=COVER_DARK + (255,))
    for y in range(0, 8 * s):
        for x in range(13 * s, 16 * s):
            img.putpixel((x, y), g.lerp(g.CREAM, (176, 168, 140), ((x * 7 + y * 13) % 11) / 22) + (255,))
    for y in range(1, 8 * s, 2):
        d.line((13 * s, y, 16 * s - 1, y), fill=g.CREAM_SHADE + (255,))
    for y in range(9 * s, 12 * s):
        for x in range(0, 12 * s):
            t = .5 + .5 * math.sin(x * .4)
            img.putpixel((x, y), g.lerp((170, 120, 40), GOLD, t) + (255,))
    d.rectangle((0, 12 * s, 12 * s - 1, 15 * s - 1), fill=g.CREAM + (255,))
    for x in range(0, 12 * s, 2):
        d.line((x, 12 * s, x, 15 * s - 1), fill=g.CREAM_SHADE + (255,))
    d.rectangle((12 * s, 9 * s, 14 * s - 1, 11 * s - 1), fill=GOLD + (255,))
    for y in range(9 * s, 11 * s):
        for x in range(14 * s, 16 * s):
            t = math.hypot(x - 14.8 * s, y - 9.8 * s) / (1.4 * s)
            img.putpixel((x, y), g.lerp(SAND_PALE, TURQUOISE, min(1, t ** .8)) + (255,))
    d.rectangle((12 * s, 11 * s, 16 * s - 1, 12 * s - 1), fill=COVER_DARK + (255,))
    return img


def affinity_ring_sand():
    return g.from_grid([
        "................",
        "......oooo......",
        ".....oxTTxo.....",
        ".....oTPPTo.....",
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
    ], {"o": (40, 24, 12), "h": (255, 244, 200), "l": GOLD, "b": (196, 140, 40), "s": (120, 80, 20), "v": TURQUOISE, "T": TURQUOISE, "P": GLASS,
        "x": (20, 110, 110)})


def main():
    g.save(sandstone_texture(), "textures/entity/sand/sandstone.png")
    g.save(glass_texture(), "textures/entity/sand/glass.png")
    g.save(sand_flow_texture(), "textures/entity/sand/sand_flow.png")
    g.save(scarab_texture(), "textures/entity/sand/scarab.png")
    g.save(gold_texture(), "textures/entity/sand/gold.png")
    g.save(hieroglyphs_texture(), "textures/entity/sand/hieroglyphs.png")
    g.save(swirl_texture(), "textures/entity/sand/swirl.png")
    for name, icon in ICONS:
        g.save(icon(), f"textures/gui/spell_icons/{name}.png")
    g.save(affinity_ring_sand(), "textures/item/affinity_ring_sand.png")
    g.write_json("models/item/affinity_ring_sand.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/affinity_ring_sand"}})
    g.save(dunes_codex_icon(), "textures/item/dunes_codex.png")
    g.save(dunes_codex_model_texture(), "textures/item/dunes_codex_3d.png")
    g.book_models("dunes_codex")


if __name__ == "__main__":
    main()
