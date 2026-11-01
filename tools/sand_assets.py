#!/usr/bin/env python3
"""Sand Magic art: sandstone, shards of glass, the linen of a sarcophagus' bandages, a band of glowing hieroglyphs, the
swirl of quicksand, the hide of a sandworm, the spell icons, the Codex of Dunes (icon and 3D book texture) and its
affinity ring.

Called by gen_assets.py (run that one). Pixel style, shaded in bands. Original art.
"""
import math
import random

import numpy as np
from PIL import Image, ImageDraw

import gen_assets as g
from water_assets import _tnoise, _quant, _rgba, _ramp

#: Sand pale and deep, sandstone, its shadow, desert gold, the turquoise of old glass and faience, linen.
SAND_PALE = (246, 226, 170)
SAND = (224, 184, 102)
SANDSTONE = (206, 160, 92)
UMBER = (120, 78, 40)
NIGHT = (40, 24, 12)
GOLD = (255, 204, 80)
TURQUOISE = (64, 200, 196)
GLASS = (200, 240, 236)
LINEN = (232, 222, 196)


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


def linen_texture():
    """32 x 64: a strip of old linen, along V, its weave showing, frayed and darker at its edges (U)."""
    W, H = 32, 64
    ys, xs = np.mgrid[0:H, 0:W]
    u = (xs + .5) / W
    weave = .5 + .5 * np.sin(xs * 1.7) * np.sin(ys * 1.3)
    edge = np.clip(np.minimum(u, 1 - u) / .15, 0, 1)
    n = _tnoise(W, H, (4, 8), 1302)
    t = np.clip(.55 + .15 * weave + .25 * n - .3 * (1 - edge), 0, 1)
    rgb = _ramp(_quant(t, 6), [UMBER, (196, 170, 120), LINEN, (250, 244, 228)])
    fray = np.random.default_rng(1303).random((H, W)) < (1 - edge) * .5
    a = np.where(fray, 0, 255.0)
    return _rgba(rgb, a)


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


def worm_texture():
    """32 x 64: the hide of a great sandworm along V: thick ridged rings of dusty plates, paler between them."""
    W, H = 32, 64
    ys, xs = np.mgrid[0:H, 0:W]
    ring = .5 + .5 * np.cos(ys / H * math.tau * 4)
    n = _tnoise(W, H, (4, 8), 1306)
    plate = ((xs % 8 == 0) & (ring > .3)) * 1.0
    t = np.clip(.35 + .4 * ring + .2 * (n - .5) - .25 * plate, 0, 1)
    rgb = _ramp(_quant(t, 7), [(70, 40, 30), (140, 92, 64), (196, 150, 104), (230, 200, 150)])
    return _rgba(rgb, np.full((H, W), 255.0))


# ------------------------------------------------------------------------------------------------------- the icons

def _icon(draw, cx=.5, cy=.5, bg=(110, 74, 34)):
    return g.painted(64, draw, (cx, cy, bg, NIGHT), 16)


def _a(c, a=255):
    return tuple(int(x) for x in c) + (a,)


def _grains(d, S, pts, r, col, alpha=255):
    for x, y in pts:
        d.ellipse(((x - r) * S, (y - r) * S, (x + r) * S, (y + r) * S), fill=_a(col, alpha))


def sand_blast_icon():
    def draw(d, img, S):
        rnd = random.Random(1)
        pts = []
        for _ in range(60):
            t = rnd.random()
            a = rnd.uniform(-.45, .45)
            pts.append((.12 + t * .8, .5 + math.sin(a) * t * .7))
        _grains(d, S, pts, .022, SAND_PALE)
        _grains(d, S, pts[::3], .03, SAND)
        g.glow_spot(img, .12 * S, .5 * S, .14 * S, SAND_PALE, .8)
    return _icon(draw, .2, .5)


def glass_shard_icon():
    def draw(d, img, S):
        for k, (x, y, a) in enumerate(((.3, .6, -20), (.55, .4, 10), (.75, .62, 30))):
            r = math.radians(a)
            pts = [(x + math.cos(r) * .16, y + math.sin(r) * .16), (x + math.cos(r + 2.2) * .07, y + math.sin(r + 2.2) * .07),
                   (x - math.cos(r) * .1, y - math.sin(r) * .1), (x + math.cos(r - 2.2) * .07, y + math.sin(r - 2.2) * .07)]
            d.polygon([(px * S, py * S) for px, py in pts], fill=_a(GLASS, 220), outline=_a((255, 255, 255)))
        _grains(d, S, [(.2, .8), (.85, .3), (.5, .78)], .015, TURQUOISE)
    return _icon(draw, .5, .5, (60, 90, 90))


def quicksand_icon():
    def draw(d, img, S):
        d.ellipse((.08 * S, .45 * S, .92 * S, .95 * S), fill=_a(SANDSTONE))
        for k in range(4):
            r = .38 - k * .08
            d.arc(((.5 - r) * S, (.7 - r * .55) * S, (.5 + r) * S, (.7 + r * .55) * S), 200 + k * 30, 470 + k * 30, fill=_a(SAND_PALE if k % 2 == 0 else UMBER),
                  width=max(1, S // 30))
        d.ellipse((.42 * S, .64 * S, .58 * S, .76 * S), fill=_a(NIGHT))
        d.rectangle((.46 * S, .25 * S, .54 * S, .66 * S), fill=_a((90, 70, 120)))
    return _icon(draw, .5, .7)


def sandstone_pillar_icon():
    def draw(d, img, S):
        for k in range(4):
            y = .9 - k * .18
            d.rectangle((.34 * S, (y - .17) * S, .66 * S, y * S), fill=_a(SAND if k % 2 else SANDSTONE), outline=_a(UMBER))
        d.rectangle((.42 * S, .02 * S, .58 * S, .16 * S), fill=_a((90, 70, 120)))
        _grains(d, S, [(.2, .85), (.8, .8), (.25, .7), (.78, .62)], .03, SAND_PALE)
    return _icon(draw, .5, .6)


def sandstorm_icon():
    def draw(d, img, S):
        for k in range(6):
            r = .1 + k * .07
            d.arc(((.5 - r) * S, (.5 - r * .7) * S, (.5 + r) * S, (.5 + r * .7) * S), k * 50, k * 50 + 250, fill=_a(SAND_PALE if k % 2 else SAND),
                  width=max(2, S // 24))
        _grains(d, S, [(.15, .3), (.85, .7), (.2, .75), (.8, .25)], .025, SAND_PALE)
    return _icon(draw, .5, .5)


def dune_wave_icon():
    def draw(d, img, S):
        d.pieslice((.05 * S, .3 * S, .95 * S, 1.3 * S), 180, 360, fill=_a(SANDSTONE))
        d.arc((.05 * S, .3 * S, .95 * S, 1.3 * S), 200, 340, fill=_a(SAND_PALE), width=max(2, S // 16))
        for k in range(5):
            x = .2 + k * .15
            d.line([(x * S, .3 * S), ((x + .06) * S, .16 * S)], fill=_a(SAND_PALE), width=max(1, S // 40))
        d.rectangle((0, .9 * S, S, S), fill=_a(UMBER))
    return _icon(draw, .5, .6)


def sand_worm_icon():
    def draw(d, img, S):
        pts = [(.1 + .8 * t, .9 - math.sin(t * math.pi) * .75) for t in [i / 10 for i in range(11)]]
        for i in range(len(pts) - 1):
            w = int(S * (.16 - .06 * abs(i - 5) / 5))
            d.line([(pts[i][0] * S, pts[i][1] * S), (pts[i + 1][0] * S, pts[i + 1][1] * S)], fill=_a((176, 128, 88)), width=max(2, w))
        hx, hy = pts[6]
        d.ellipse(((hx - .1) * S, (hy - .1) * S, (hx + .1) * S, (hy + .1) * S), fill=_a((70, 40, 30)), outline=_a((236, 226, 206)), width=max(1, S // 40))
        d.rectangle((0, .88 * S, S, S), fill=_a(SAND))
    return _icon(draw, .5, .5)


def sarcophagus_icon():
    def draw(d, img, S):
        d.rounded_rectangle((.32 * S, .1 * S, .68 * S, .92 * S), radius=int(.15 * S), fill=_a((90, 70, 120)))
        for k in range(7):
            y = .16 + k * .11
            d.line([(.28 * S, y * S), (.72 * S, (y + .06) * S)], fill=_a(LINEN), width=max(2, S // 16))
        d.ellipse((.42 * S, .14 * S, .58 * S, .28 * S), fill=_a(GOLD))
    return _icon(draw, .5, .5)


def obelisk_icon():
    def draw(d, img, S):
        d.polygon([(.4 * S, .92 * S), (.6 * S, .92 * S), (.56 * S, .2 * S), (.44 * S, .2 * S)], fill=_a(SANDSTONE), outline=_a(UMBER))
        d.polygon([(.44 * S, .2 * S), (.56 * S, .2 * S), (.5 * S, .08 * S)], fill=_a(GOLD))
        for k in range(4):
            d.line([(.47 * S, (.35 + k * .12) * S), (.53 * S, (.35 + k * .12) * S)], fill=_a(GOLD), width=max(1, S // 48))
        d.line([(.5 * S, .08 * S), (.92 * S, .55 * S)], fill=_a((255, 236, 160)), width=max(2, S // 22))
        g.glow_spot(img, .5 * S, .08 * S, .12 * S, GOLD, 1)
    return _icon(draw, .5, .3)


def pyramid_icon():
    def draw(d, img, S):
        d.polygon([(.06 * S, .9 * S), (.94 * S, .9 * S), (.5 * S, .14 * S)], fill=_a(SAND))
        d.polygon([(.5 * S, .9 * S), (.94 * S, .9 * S), (.5 * S, .14 * S)], fill=_a(SANDSTONE))
        for k in range(1, 6):
            y = .14 + k * .126
            half = (y - .14) / .76 * .44
            d.line([((.5 - half) * S, y * S), ((.5 + half) * S, y * S)], fill=_a(UMBER), width=max(1, S // 64))
        d.polygon([(.44 * S, .24 * S), (.56 * S, .24 * S), (.5 * S, .14 * S)], fill=_a(GOLD))
        g.glow_spot(img, .5 * S, .16 * S, .14 * S, GOLD, 1)
    return _icon(draw, .5, .3)


ICONS = (("sand_blast", sand_blast_icon), ("glass_shard", glass_shard_icon), ("quicksand", quicksand_icon),
         ("sandstone_pillar", sandstone_pillar_icon), ("sandstorm", sandstorm_icon), ("dune_wave", dune_wave_icon),
         ("sand_worm", sand_worm_icon), ("sarcophagus", sarcophagus_icon), ("obelisk", obelisk_icon), ("pyramid", pyramid_icon))


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
    g.save(linen_texture(), "textures/entity/sand/linen.png")
    g.save(hieroglyphs_texture(), "textures/entity/sand/hieroglyphs.png")
    g.save(swirl_texture(), "textures/entity/sand/swirl.png")
    g.save(worm_texture(), "textures/entity/sand/worm.png")
    for name, icon in ICONS:
        g.save(icon(), f"textures/gui/spell_icons/{name}.png")
    g.save(affinity_ring_sand(), "textures/item/affinity_ring_sand.png")
    g.write_json("models/item/affinity_ring_sand.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/affinity_ring_sand"}})
    g.save(dunes_codex_icon(), "textures/item/dunes_codex.png")
    g.save(dunes_codex_model_texture(), "textures/item/dunes_codex_3d.png")
    g.book_models("dunes_codex")


if __name__ == "__main__":
    main()
