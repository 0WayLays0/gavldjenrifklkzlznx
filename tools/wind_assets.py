#!/usr/bin/env python3
"""Wind Magic art: the lines wind is drawn with (streaks, a sheen of moving air, curling wisps), leaves and dust caught in
it, a ring of blown air, the spell icons, the Codex of the Four Winds (icon and 3D book texture) and its affinity ring.

Called by gen_assets.py (run that one). Pixel style, shaded in bands. Original art.
"""
import math

import numpy as np
from PIL import Image, ImageDraw

import gen_assets as g
from water_assets import _tnoise, _quant, _rgba, _ramp

#: Air (almost white, a breath of jade), jade, deep teal, the grey-green of dust and shadow.
AIR = (239, 255, 250)
JADE = (158, 232, 207)
TEAL = (70, 176, 156)
DEEP = (30, 92, 88)
DUST = (176, 168, 146)
NIGHT = (12, 22, 24)


def streak_texture():
    """64 x 8: a line of wind, white, along U from its tail (U 0) to its head (U 1): thin, tapering at both ends,
    brightest just behind its head; soft across V (drawn see-through, tinted)."""
    W, H = 64, 8
    ys, xs = np.mgrid[0:H, 0:W]
    u, v = (xs + .5) / W, (ys + .5) / H * 2 - 1
    along = np.sin(np.pi * u) ** .5 * (.35 + .65 * u ** 1.5)
    across = np.exp(-v * v / .18)
    a = _quant(np.clip(along * across * 1.15, 0, 1), 6)
    return _rgba(np.full((H, W, 3), 255.0), a * 255)


def sheen_texture():
    """64 x 32, tiling: moving air seen against the light, faint streaks running along U (drawn see-through, scrolled)."""
    W, H = 64, 32
    n1 = _tnoise(W, H, (2, 8), 901)
    n2 = _tnoise(W, H, (4, 16), 902)
    s = np.clip((n1 - .45) * 2.2, 0, 1) ** 1.5 * .7 + np.clip((n2 - .6) * 2.5, 0, 1) * .5
    a = _quant(np.clip(s, 0, 1), 5)
    return _rgba(np.full((H, W, 3), 255.0), a * 200)


def _spiral(d, cx, cy, r0, r1, turns, w, alpha, S, phase=0.0):
    pts = []
    n = 60
    for i in range(n + 1):
        t = i / n
        a = phase + t * turns * math.tau
        r = r0 + (r1 - r0) * t
        pts.append(((cx + math.cos(a) * r) * S, (cy + math.sin(a) * r) * S))
    for i in range(n):
        t = i / n
        k = math.sin(math.pi * t)
        d.line([pts[i], pts[i + 1]], fill=(255, 255, 255, int(alpha * 255 * k)), width=max(1, int(w * S * (.4 + .6 * k))))


def wisp_texture():
    """64 x 64: four curls of air (2 x 2), white strokes fading at their ends (drawn see-through, tinted)."""
    up = 4
    S = 32 * up
    img = Image.new("RGBA", (64 * up, 64 * up), (0, 0, 0, 0))
    for k in range(4):
        tile = Image.new("RGBA", (S, S), (0, 0, 0, 0))
        d = ImageDraw.Draw(tile)
        rnd = np.random.default_rng(910 + k)
        for j in range(3):
            _spiral(d, .5 + rnd.uniform(-.08, .08), .5 + rnd.uniform(-.08, .08), .04 + .03 * j, .38 - .06 * j, .9 + .25 * j, .05 - .01 * j,
                    .9 - .2 * j, S, rnd.uniform(0, math.tau))
        img.paste(tile, ((k % 2) * S, (k // 2) * S))
    img = img.resize((64, 64), Image.BOX)
    a = np.asarray(img, float)[..., 3] / 255
    return _rgba(np.full((64, 64, 3), 255.0), _quant(np.clip(a * 1.3, 0, 1), 5) * 255)


def leaf_texture():
    """32 x 32: four leaves caught by the wind (2 x 2): an oak leaf, a birch leaf, an autumn leaf, a blossom petal."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    cols = [((76, 140, 52), (112, 180, 70), (52, 96, 36)), ((150, 186, 60), (196, 220, 96), (106, 132, 40)),
            ((208, 110, 40), (240, 160, 70), (150, 70, 26)), ((240, 170, 196), (255, 214, 228), (196, 120, 150))]
    for k, (base, light, dark) in enumerate(cols):
        ox, oy = (k % 2) * 16, (k // 2) * 16
        d = ImageDraw.Draw(img)
        if k == 3:
            d.ellipse((ox + 4, oy + 3, ox + 12, oy + 13), fill=base + (255,))
            d.ellipse((ox + 6, oy + 4, ox + 10, oy + 9), fill=light + (255,))
            d.point((ox + 8, oy + 12), fill=dark + (255,))
            continue
        pts = [(ox + 8, oy + 1), (ox + 13, oy + 6), (ox + 12, oy + 11), (ox + 8, oy + 14), (ox + 4, oy + 11), (ox + 3, oy + 6)]
        if k == 0:
            pts = [(ox + 8, oy + 1), (ox + 11, oy + 4), (ox + 13, oy + 5), (ox + 12, oy + 8), (ox + 13, oy + 11), (ox + 8, oy + 14),
                   (ox + 3, oy + 11), (ox + 4, oy + 8), (ox + 3, oy + 5), (ox + 5, oy + 4)]
        d.polygon(pts, fill=base + (255,))
        d.polygon([(x, y) for x, y in pts[:len(pts) // 2 + 1]] + [(ox + 8, oy + 8)], fill=light + (255,))
        d.line((ox + 8, oy + 2, ox + 8, oy + 15), fill=dark + (255,))
    return img


def dust_texture():
    """32 x 32: a puff of blown dust, soft, thinning at its edge (drawn see-through, tinted)."""
    S = 32
    ys, xs = np.mgrid[0:S, 0:S]
    x, y = (xs + .5) / S - .5, (ys + .5) / S - .5
    r = np.hypot(x, y) * 2
    billow = _tnoise(S, S, (4, 4), 921) * .6 + _tnoise(S, S, (8, 8), 922) * .4
    a = np.clip((1 - r) * 1.6 + (billow - .5) * .9, 0, 1)
    shade = _quant(.55 + .35 * billow - .15 * y, 4)
    rgb = _ramp(shade, [(150, 142, 124), (196, 188, 166), (226, 220, 202)])
    return _rgba(rgb, _quant(a, 4) * 210)


def ring_texture():
    """64 x 64: a ring of blown air racing out, its front sharp and streaked, trailing off inside (white, see-through)."""
    S = 64
    ys, xs = np.mgrid[0:S, 0:S]
    x, y = (xs + .5) / S - .5, (ys + .5) / S - .5
    r = np.hypot(x, y) * 2
    ang = np.arctan2(y, x)
    streaks = .6 + .4 * np.cos(ang * 24 + _tnoise(S, S, (6, 6), 931) * 6)
    k = np.where(r < .93, np.exp(-(r - .93) ** 2 / .03), np.exp(-(r - .93) ** 2 / .0012)) * (r < 1) * streaks
    return _rgba(np.full((S, S, 3), 255.0), _quant(np.clip(k, 0, 1), 6) * 255)


# ------------------------------------------------------------------------------------------------------- the icons


def _icon(draw, cx=.5, cy=.5, bg=(26, 54, 56)):
    return g.painted(64, draw, (cx, cy, bg, NIGHT), 16)


def _a(c, a=255):
    return tuple(int(x) for x in c) + (a,)


def _swoosh(d, S, cx, cy, r, a0, a1, w, col, alpha=255):
    """An arc of wind, thick in its middle, thin at its ends."""
    n = 24
    pts = [((cx + math.cos(math.radians(a0 + (a1 - a0) * i / n)) * r) * S, (cy + math.sin(math.radians(a0 + (a1 - a0) * i / n)) * r) * S) for i in range(n + 1)]
    for i in range(n):
        k = math.sin(math.pi * (i + .5) / n)
        d.line([pts[i], pts[i + 1]], fill=_a(col, alpha), width=max(1, int(w * S * (.25 + .75 * k))))


def _line(d, S, pts, w, col, alpha=255):
    n = len(pts) - 1
    for i in range(n):
        k = math.sin(math.pi * (i + .5) / n)
        d.line([(pts[i][0] * S, pts[i][1] * S), (pts[i + 1][0] * S, pts[i + 1][1] * S)], fill=_a(col, alpha), width=max(1, int(w * S * (.3 + .7 * k))))


def _leaf(d, S, x, y, r, col):
    d.ellipse(((x - r) * S, (y - r * .6) * S, (x + r) * S, (y + r * .6) * S), fill=_a(col))


def gust_blade_icon():
    def draw(d, img, S):
        for k, (w, col) in enumerate(((.12, TEAL), (.07, JADE), (.035, AIR))):
            _swoosh(d, S, .38, .5, .36, -70, 70, w, col)
        for k in range(4):
            y = .3 + k * .13
            _line(d, S, [(.08, y), (.32, y + .02)], .03, AIR, 200)
    return _icon(draw, .6, .5)


def gale_icon():
    def draw(d, img, S):
        for k in range(7):
            y0 = .5 + (k - 3) * .03
            y1 = .5 + (k - 3) * .11
            _line(d, S, [(.1, y0), (.45, (y0 + y1) * .5 + .02 * math.sin(k)), (.9, y1)], .045, AIR if k % 2 else JADE)
        _leaf(d, S, .78, .3, .05, (110, 180, 70))
        _leaf(d, S, .7, .72, .04, (220, 140, 60))
        g.glow_spot(img, .14 * S, .5 * S, .16 * S, JADE, .8)
    return _icon(draw, .2, .5)


def updraft_icon():
    def draw(d, img, S):
        d.rectangle((.44 * S, .18 * S, .56 * S, .42 * S), fill=_a((90, 70, 120)))
        for k in range(3):
            _swoosh(d, S, .5, .72 - k * .16, .22 + .05 * k, 200, 340, .05, (AIR, JADE, AIR)[k])
        d.ellipse((.18 * S, .86 * S, .82 * S, .96 * S), outline=_a(JADE), width=max(1, S // 32))
    return _icon(draw, .5, .7)


def tailwind_icon():
    def draw(d, img, S):
        d.rectangle((.44 * S, .3 * S, .56 * S, .8 * S), fill=_a((90, 70, 120)))
        d.ellipse((.42 * S, .16 * S, .58 * S, .32 * S), fill=_a((120, 96, 150)))
        for k, (y, t) in enumerate(((.35, -12), (.55, 8), (.75, -6))):
            d.arc((.2 * S, (y - .08) * S, .8 * S, (y + .08) * S), 160 + t, 380 + t, fill=_a(AIR if k != 1 else JADE), width=max(2, S // 22))
        _line(d, S, [(.05, .6), (.3, .58)], .03, AIR)
    return _icon(draw, .5, .5)


def cyclone_icon():
    def draw(d, img, S):
        for k in range(7):
            y = .82 - k * .1
            r = .08 + .05 * k
            _swoosh(d, S, .5 + .03 * math.sin(k), y, r, 160, 380, .05, AIR if k % 2 else JADE)
        d.ellipse((.3 * S, .86 * S, .7 * S, .94 * S), fill=_a(DUST, 160))
    return _icon(draw, .5, .55)


def vacuum_sphere_icon():
    def draw(d, img, S):
        for k in range(12):
            a = k * math.tau / 12
            _line(d, S, [(.5 + math.cos(a) * .44, .5 + math.sin(a) * .44), (.5 + math.cos(a + .3) * .22, .5 + math.sin(a + .3) * .22)], .035, AIR)
        d.ellipse((.34 * S, .34 * S, .66 * S, .66 * S), fill=_a(DEEP))
        d.ellipse((.4 * S, .4 * S, .6 * S, .6 * S), fill=_a((10, 30, 30)))
        _swoosh(d, S, .5, .5, .13, 0, 270, .03, JADE)
    return _icon(draw, .5, .5, (30, 40, 44))


def wind_wall_icon():
    def draw(d, img, S):
        for k in range(6):
            x = .2 + k * .12
            _line(d, S, [(x, .9), (x + .04, .6), (x - .03, .35), (x + .02, .1)], .045, AIR if k % 2 else JADE)
        d.rectangle((.1 * S, .9 * S, .9 * S, .96 * S), fill=_a(DUST))
        _line(d, S, [(.05, .5), (.15, .48)], .03, (200, 80, 60))
    return _icon(draw, .5, .5)


def blade_storm_icon():
    def draw(d, img, S):
        d.rectangle((.45 * S, .35 * S, .55 * S, .7 * S), fill=_a((90, 70, 120)))
        for k in range(6):
            a = k * 60
            cx, cy = .5 + math.cos(math.radians(a)) * .32, .52 + math.sin(math.radians(a)) * .32
            _swoosh(d, S, cx, cy, .1, a + 30, a + 150, .05, AIR if k % 2 else JADE)
        d.ellipse((.16 * S, .2 * S, .84 * S, .84 * S), outline=_a(TEAL, 160), width=max(1, S // 40))
    return _icon(draw, .5, .5)


def skyfall_icon():
    def draw(d, img, S):
        d.rectangle((.44 * S, .1 * S, .56 * S, .3 * S), fill=_a((140, 110, 80)))
        for k in range(5):
            x = .26 + k * .12
            _line(d, S, [(x, .15), (x, .55), (x + (k - 2) * .04, .8)], .04, AIR if k % 2 else JADE)
        d.ellipse((.12 * S, .82 * S, .88 * S, .96 * S), outline=_a(AIR), width=max(2, S // 20))
        d.rectangle((.05 * S, .92 * S, .95 * S, S), fill=_a(DUST))
    return _icon(draw, .5, .3)


def hurricane_icon():
    def draw(d, img, S):
        for k in range(3):
            a0 = k * 120
            pts = []
            for i in range(30):
                t = i / 29
                a = math.radians(a0 + t * 300)
                r = .06 + .38 * t
                pts.append((.5 + math.cos(a) * r, .5 + math.sin(a) * r))
            _line(d, S, pts, .09, (STORM_GREY if k == 0 else AIR if k == 1 else JADE))
        d.ellipse((.44 * S, .44 * S, .56 * S, .56 * S), fill=_a((140, 200, 220)))
    return _icon(draw, .5, .5, (34, 44, 48))


STORM_GREY = (110, 126, 128)

ICONS = [
    ("gust_blade", gust_blade_icon),
    ("gale", gale_icon),
    ("updraft", updraft_icon),
    ("tailwind", tailwind_icon),
    ("cyclone", cyclone_icon),
    ("vacuum_sphere", vacuum_sphere_icon),
    ("wind_wall", wind_wall_icon),
    ("blade_storm", blade_storm_icon),
    ("skyfall", skyfall_icon),
    ("hurricane", hurricane_icon),
]

# --------------------------------------------------------------------------------------------------- the spellbook

COVER = (52, 92, 90)
COVER_LIGHT = (84, 132, 126)
COVER_DARK = (30, 56, 56)
SILVER = (196, 210, 206)


def winds_codex_icon():
    return g.from_grid([
        "................",
        "...oooooooooo...",
        "..obbbbbbbbbdco.",
        "..obsbbbbbbbdco.",
        "..obbbbJbbbbdco.",
        "..obbbJAJbbbdcco",
        "..obJJAWAJJbdco.",
        "..obbbJAJbbbdcco",
        "..obbbbJbbbbdcco",
        "..obbbbbbbsbdcco",
        "..obbbbbbbbbdco.",
        "..oddssdddssxco.",
        "..oddddssdddxco.",
        "...ooooooooooo..",
        "................",
        "................",
    ], {"o": (8, 18, 18), "b": COVER, "s": SILVER, "d": COVER_DARK, "x": (6, 14, 14), "J": JADE, "A": AIR, "W": (255, 255, 255), "c": g.CREAM})


def winds_codex_model_texture():
    """64x64 texture of the 3D book, laid out like the other books: covers of sea-green leather framed in silver, a
    compass rose of the four winds on the front with lines of wind curling round it, a silver spine, old pages."""
    s = 4
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    W, H = 12 * s, 9 * s
    grain = _tnoise(W, H, (6, 4), 941)
    cover = _ramp(.3 + .4 * grain, [COVER_DARK, COVER, COVER_LIGHT])
    img.paste(_rgba(cover, np.full((H, W), 255)), (0, 0))
    d.rectangle((1, 1, W - 2, H - 2), outline=SILVER + (255,))
    cx, cy = W // 2, H // 2
    for k in range(4):
        a = k * math.pi / 2
        tip = (cx + math.cos(a) * 12, cy + math.sin(a) * 12)
        side1 = (cx + math.cos(a + math.pi / 2) * 3, cy + math.sin(a + math.pi / 2) * 3)
        side2 = (cx + math.cos(a - math.pi / 2) * 3, cy + math.sin(a - math.pi / 2) * 3)
        d.polygon([tip, side1, (cx, cy), side2], fill=(AIR if k % 2 == 0 else JADE) + (255,))
    for k in range(3):
        d.arc((cx - 16 + k, cy - 13 + k, cx + 16 - k, cy + 13 - k), 200 + k * 40, 320 + k * 40, fill=AIR + (255,))
    d.ellipse((cx - 2, cy - 2, cx + 2, cy + 2), fill=SILVER + (255,))
    d.rectangle((0, 0, W - 1, H - 1), outline=(8, 18, 18, 255))
    d.rectangle((12 * s, 0, 13 * s - 1, 9 * s - 1), fill=COVER_DARK + (255,))
    for y in range(0, 8 * s):
        for x in range(13 * s, 16 * s):
            img.putpixel((x, y), g.lerp(g.CREAM, (176, 168, 140), ((x * 7 + y * 13) % 11) / 22) + (255,))
    for y in range(1, 8 * s, 2):
        d.line((13 * s, y, 16 * s - 1, y), fill=g.CREAM_SHADE + (255,))
    for y in range(9 * s, 12 * s):
        for x in range(0, 12 * s):
            t = .5 + .5 * math.sin(x * .4)
            img.putpixel((x, y), g.lerp((120, 136, 132), SILVER, t) + (255,))
    d.rectangle((0, 12 * s, 12 * s - 1, 15 * s - 1), fill=g.CREAM + (255,))
    for x in range(0, 12 * s, 2):
        d.line((x, 12 * s, x, 15 * s - 1), fill=g.CREAM_SHADE + (255,))
    d.rectangle((12 * s, 9 * s, 14 * s - 1, 11 * s - 1), fill=SILVER + (255,))
    for y in range(9 * s, 11 * s):
        for x in range(14 * s, 16 * s):
            t = math.hypot(x - 14.8 * s, y - 9.8 * s) / (1.4 * s)
            img.putpixel((x, y), g.lerp(AIR, TEAL, min(1, t ** .8)) + (255,))
    d.rectangle((12 * s, 11 * s, 16 * s - 1, 12 * s - 1), fill=COVER_DARK + (255,))
    return img


def affinity_ring_wind():
    return g.from_grid([
        "................",
        "......oooo......",
        ".....oxJJxo.....",
        ".....oJAAJo.....",
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
    ], {"o": (10, 22, 22), "h": (236, 246, 244), "l": SILVER, "b": (120, 136, 132), "s": (70, 84, 82), "v": JADE, "J": JADE, "A": AIR, "x": TEAL})


def main():
    g.save(streak_texture(), "textures/entity/wind/streak.png")
    g.save(sheen_texture(), "textures/entity/wind/sheen.png")
    g.save(wisp_texture(), "textures/entity/wind/wisp.png")
    g.save(leaf_texture(), "textures/entity/wind/leaf.png")
    g.save(dust_texture(), "textures/entity/wind/dust.png")
    g.save(ring_texture(), "textures/entity/wind/ring.png")
    for name, icon in ICONS:
        g.save(icon(), f"textures/gui/spell_icons/{name}.png")
    g.save(affinity_ring_wind(), "textures/item/affinity_ring_wind.png")
    g.write_json("models/item/affinity_ring_wind.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/affinity_ring_wind"}})
    g.save(winds_codex_icon(), "textures/item/winds_codex.png")
    g.save(winds_codex_model_texture(), "textures/item/winds_codex_3d.png")
    g.book_models("winds_codex")


if __name__ == "__main__":
    main()
