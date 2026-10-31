#!/usr/bin/env python3
"""Stellar Fire art: tongues of flame and curtains of fire in each of the six heats (red, orange, yellow, white, blue,
violet: the colours of the star classes M, K, G, F, B and O), a halo, a spark, smoke, a ring of light, the heat
effect's icon, the spell icons, the Starfire Codex (icon and 3D book texture) and its affinity ring.

Called by gen_assets.py (run that one). Pixel style, shaded in bands. Original art.
"""
import math

import numpy as np
from PIL import Image, ImageDraw

import gen_assets as g
from water_assets import _tnoise, _quant, _rgba, _ramp

#: Per heat (see HeatColors.java): heart, body, tips.
CORE = [(255, 200, 144), (255, 226, 166), (255, 248, 220), (255, 255, 255), (234, 242, 255), (242, 232, 255)]
BODY = [(240, 64, 28), (255, 134, 30), (255, 203, 56), (255, 242, 220), (79, 147, 255), (155, 92, 255)]
TIP = [(126, 18, 6), (184, 64, 10), (216, 118, 14), (201, 214, 255), (28, 63, 216), (74, 26, 184)]
SOOT = (40, 30, 28)
NIGHT = (14, 10, 12)


def _heat_ramp(T, tier):
    """Colour of a flame by its temperature (0 its cool edge, 1 its white heart) for a heat."""
    return _ramp(T, [TIP[tier], TIP[tier], BODY[tier], BODY[tier], CORE[tier]])


def _tongue_field(S, seed, lean=0.0):
    """One tongue of flame, S x S: its temperature 0 to 1, wide and hot at its foot, licking up into split tips."""
    ys, xs = np.mgrid[0:S, 0:S]
    x = (xs + .5) / S * 2 - 1
    y = 1 - (ys + .5) / S
    n1 = _tnoise(S, S, (4, 3), seed) - .5
    n2 = _tnoise(S, S, (8, 6), seed + 1) - .5
    disp = n1 * .5 * y + lean * y * y
    width = .62 * np.clip(1 - y, 0, 1) ** .65 + .04
    shape = np.clip(1 - np.abs(x - disp) / width, 0, 1)
    holes = np.clip(n2 * 2.2 * y, 0, 1)
    T = np.clip(shape * (1.15 - .75 * y) - holes * .8, 0, 1)
    return T


def flame_texture(tier):
    """64 x 64: four tongues of flame (2 x 2 frames) in the colours of a heat, each flickering differently."""
    out = np.zeros((64, 64, 4))
    for k in range(4):
        T = _tongue_field(32, 300 + k * 7, lean=(k - 1.5) * .15)
        Tq = _quant(T, 6)
        rgb = _heat_ramp(Tq, tier)
        a = np.where(T > .08, _quant(np.clip(T * 2.2, 0, 1), 4), 0)
        ox, oy = (k & 1) * 32, (k >> 1) * 32
        out[oy:oy + 32, ox:ox + 32, :3] = rgb
        out[oy:oy + 32, ox:ox + 32, 3] = a * 255
    return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), "RGBA")


def wall_texture(tier):
    """32 x 32, tiling along U: a curtain of fire, dense and white-hot at its foot (V 1), licking up into tips (V 0)."""
    S = 32
    ys, xs = np.mgrid[0:S, 0:S]
    u = xs / S
    y = 1 - (ys + .5) / S
    n1 = _tnoise(S, S, (6, 3), 331)
    n2 = _tnoise(S, S, (12, 6), 332)
    tongues = .5 + .5 * np.sin((u * 4 + (n1 - .5) * .6) * math.tau)
    height = .45 + .45 * tongues + .2 * (n2 - .5)
    T = np.clip((height - y) / np.maximum(height, .1) * 1.3, 0, 1)
    T = np.clip(T - np.clip((n2 - .55) * 2.5 * y, 0, 1), 0, 1)
    Tq = _quant(T, 6)
    rgb = _heat_ramp(Tq, tier)
    a = np.where(T > .06, _quant(np.clip(T * 2, 0, 1), 4), 0)
    return _rgba(rgb, a * 255)


def glow_texture():
    """32 x 32 soft round light, white on black (drawn added)."""
    S = 32
    ys, xs = np.mgrid[0:S, 0:S]
    r = np.hypot((xs + .5) / S - .5, (ys + .5) / S - .5) * 2
    k = np.clip(1 - r, 0, 1) ** 2.2
    return _rgba(np.dstack([k * 255] * 3), np.full((S, S), 255))


def spark_texture():
    """8 x 16 spark, a bright line on black (drawn added), brightest at its head (V 0), fading along its tail."""
    w, h = 8, 16
    ys, xs = np.mgrid[0:h, 0:w]
    u, v = (xs + .5) / w, (ys + .5) / h
    k = np.exp(-((u - .5) ** 2) / .02) * (1 - v) ** 1.2
    return _rgba(np.dstack([k * 255] * 3), np.full((h, w), 255))


def beam_texture():
    """8 x 8 band of light, white on black (drawn added): bright across its middle (U), even along V."""
    w, h = 8, 8
    u = (np.arange(w) + .5) / w
    k = np.exp(-((u - .5) ** 2) / .035)[None, :].repeat(h, 0)
    return _rgba(np.dstack([k * 255] * 3), np.full((h, w), 255))


def ring_texture():
    """64 x 64 ring of light, white on black (drawn added): a bright band near the edge, soft inside."""
    S = 64
    ys, xs = np.mgrid[0:S, 0:S]
    r = np.hypot((xs + .5) / S - .5, (ys + .5) / S - .5) * 2
    k = np.exp(-((r - .86) ** 2) / .004) + .25 * np.clip(.86 - r, 0, 1) * (r < .86)
    k = np.clip(k, 0, 1) * (r < 1)
    return _rgba(np.dstack([k * 255] * 3), np.full((S, S), 255))


def smoke_texture():
    """32 x 32 puff of dark smoke, billowing, thinning at its edge."""
    S = 32
    ys, xs = np.mgrid[0:S, 0:S]
    x, y = (xs + .5) / S - .5, (ys + .5) / S - .5
    r = np.hypot(x, y) * 2
    billow = _tnoise(S, S, (4, 4), 341) * .6 + _tnoise(S, S, (8, 8), 342) * .4
    a = np.clip((1 - r) * 1.5 + (billow - .5) * .8, 0, 1)
    shade = _quant(.35 + .35 * billow - .2 * y, 4)
    rgb = _ramp(shade, [(40, 36, 36), (74, 68, 66), (110, 104, 100)])
    return _rgba(rgb, _quant(a, 4) * 220)


# ------------------------------------------------------------------------------------------------------- the icons


def _icon(draw, cx=.5, cy=.5, bg=(48, 22, 14)):
    return g.painted(64, draw, (cx, cy, bg, NIGHT), 16)


def _a(c, a=255):
    return tuple(int(x) for x in c) + (a,)


def _tongue(d, S, x, y, w, h, tier, lean=0.0):
    """A tongue of flame standing at (x, y) (its foot), w wide and h tall, in a heat's colours (tips, body, heart)."""
    for k, (col, f) in enumerate(((TIP[tier], 1.0), (BODY[tier], .72), (CORE[tier], .38))):
        ww, hh = w * f, h * (f * .9 + .1)
        pts = [((x - ww * .5) * S, y * S), ((x + ww * .5) * S, y * S)]
        for j in range(1, 7):
            t = j / 6
            pts.append(((x + ww * .5 * (1 - t) ** .8 + lean * t * t * h) * S, (y - hh * t) * S))
        pts += [((x - ww * .5 * (1 - t) ** .8 + lean * t * t * h) * S, (y - hh * t) * S) for t in [j / 6 for j in range(6, 0, -1)]]
        d.polygon(pts, fill=_a(col))


def _glow(img, S, x, y, r, tier, k=1.0):
    g.glow_spot(img, x * S, y * S, r * S, CORE[tier], k)


def ember_bolt_icon():
    def draw(d, img, S):
        for k in range(5):
            t = k / 4
            _tongue(d, S, .26 + .3 * t, .72 - .3 * t, .2 - .03 * k, .3, 1, lean=-.6)
        d.ellipse((.56 * S, .2 * S, .84 * S, .48 * S), fill=_a(BODY[1]))
        d.ellipse((.62 * S, .26 * S, .78 * S, .42 * S), fill=_a(CORE[1]))
        _glow(img, S, .7, .34, .22, 1)
    return _icon(draw, .65, .35)


def stellar_breath_icon():
    def draw(d, img, S):
        d.ellipse((.04 * S, .42 * S, .2 * S, .6 * S), fill=_a((120, 90, 80)))
        for k in range(9):
            t = k / 8
            _tongue(d, S, .2 + .65 * t, .62 + .1 * math.sin(k * 2.3), .12 + .18 * t, .18 + .3 * t, 2 if k < 3 else (1 if k < 6 else 0), lean=-.3)
        _glow(img, S, .22, .5, .16, 3)
    return _icon(draw, .3, .5)


def corona_icon():
    def draw(d, img, S):
        d.ellipse((.12 * S, .5 * S, .88 * S, .9 * S), outline=_a(BODY[4]), width=max(2, S // 16))
        for k in range(12):
            a = k * math.tau / 12
            x, y = .5 + math.cos(a) * .38, .7 + math.sin(a) * .2
            _tongue(d, S, x, y, .1, .22 + .08 * math.sin(a), 4)
        _glow(img, S, .5, .66, .2, 4, .8)
        d.rectangle((.46 * S, .5 * S, .54 * S, .7 * S), fill=_a((90, 70, 110)))
    return _icon(draw, .5, .65, (16, 24, 52))


def fire_pillars_icon():
    def draw(d, img, S):
        d.rectangle((0, .86 * S, S, S), fill=_a((50, 36, 30)))
        for k, (x, h) in enumerate(((.2, .45), (.45, .62), (.72, .78))):
            _tongue(d, S, x, .88, .2, h, 2 + (k == 2))
            _glow(img, S, x, .86, .1, 2)
    return _icon(draw, .5, .7)


def will_o_wisps_icon():
    def draw(d, img, S):
        for x, y, t in ((.28, .62, 1), (.7, .4, 4), (.42, .3, 5)):
            _tongue(d, S, x, y + .1, .14, .22, t)
            _glow(img, S, x, y, .12, t, 1.2)
        d.arc((.15 * S, .2 * S, .85 * S, .8 * S), 200, 340, fill=_a((255, 210, 150), 120), width=max(1, S // 40))
    return _icon(draw, .5, .5, (20, 16, 36))


def flame_crescent_icon():
    def draw(d, img, S):
        for k, w in enumerate((.11, .07, .035)):
            col = (BODY[2], (255, 236, 160), CORE[2])[k]
            d.arc((.12 * S, .16 * S, .9 * S, .94 * S), 200, 340, fill=_a(col), width=max(1, int(w * S)))
        for k in range(6):
            a = math.radians(205 + k * 25)
            x, y = .51 + math.cos(a) * .39, .55 + math.sin(a) * .39
            _tongue(d, S, x, y + .12, .09, .14, 2, lean=-.3)
    return _icon(draw, .5, .4)


def searing_brand_icon():
    def draw(d, img, S):
        d.rectangle((.42 * S, .52 * S, .58 * S, .92 * S), fill=_a((92, 80, 64)))
        d.ellipse((.4 * S, .4 * S, .6 * S, .56 * S), fill=_a((110, 96, 76)))
        d.ellipse((.18 * S, .14 * S, .82 * S, .34 * S), outline=_a(BODY[3]), width=max(2, S // 20))
        for k in range(6):
            a = k * math.tau / 6
            d.line((.5 * S, .24 * S, (.5 + math.cos(a) * .3) * S, (.24 + math.sin(a) * .09) * S), fill=_a(CORE[3]), width=max(1, S // 40))
        _glow(img, S, .5, .24, .2, 3, 1.2)
        for x in (.3, .7):
            _tongue(d, S, x, .3, .08, .12, 3)
    return _icon(draw, .5, .3)


def phoenix_icon():
    def draw(d, img, S):
        for side in (-1, 1):
            for k in range(5):
                t = k / 4
                pts = [(.5 * S, .5 * S), ((.5 + side * (.15 + .3 * t)) * S, (.2 + .2 * t) * S), ((.5 + side * (.2 + .28 * t)) * S, (.32 + .2 * t) * S)]
                d.polygon(pts, fill=_a(BODY[0] if k % 2 else TIP[0]))
        for k in range(3):
            _tongue(d, S, .5 + (k - 1) * .06, .95, .1, .38, 1, lean=(k - 1) * .2)
        d.ellipse((.42 * S, .4 * S, .58 * S, .62 * S), fill=_a(BODY[1]))
        d.ellipse((.45 * S, .3 * S, .55 * S, .42 * S), fill=_a(CORE[1]))
        _glow(img, S, .5, .45, .18, 1, 1.2)
    return _icon(draw, .5, .45)


def supernova_icon():
    def draw(d, img, S):
        for k in range(16):
            a = k * math.tau / 16
            r1 = .18 if k % 2 else .24
            d.line((.5 * S, .5 * S, (.5 + math.cos(a) * (r1 + .22)) * S, (.5 + math.sin(a) * (r1 + .22)) * S), fill=_a(BODY[5]), width=max(1, S // 28))
        d.ellipse((.22 * S, .22 * S, .78 * S, .78 * S), outline=_a(BODY[5]), width=max(2, S // 18))
        d.ellipse((.34 * S, .34 * S, .66 * S, .66 * S), fill=_a(BODY[5]))
        d.ellipse((.42 * S, .42 * S, .58 * S, .58 * S), fill=_a(CORE[5]))
        _glow(img, S, .5, .5, .3, 5, 1.2)
    return _icon(draw, .5, .5, (24, 12, 48))


def stellar_pyre_icon():
    def draw(d, img, S):
        d.rectangle((0, .88 * S, S, S), fill=_a((40, 30, 30)))
        for k in range(6):
            t = k / 5
            y = .86 - .62 * t
            w = .5 - .2 * t
            d.arc(((.5 - w) * S, (y - .06) * S, (.5 + w) * S, (y + .06) * S), 0, 180, fill=_a(BODY[k]), width=max(2, S // 18))
        for k in range(6):
            _tongue(d, S, .3 + .08 * k, .9, .12, .25 + .05 * k, k)
        _glow(img, S, .5, .4, .25, 5, .7)
    return _icon(draw, .5, .5, (28, 14, 30))


ICONS = [
    ("ember_bolt", ember_bolt_icon),
    ("stellar_breath", stellar_breath_icon),
    ("corona", corona_icon),
    ("fire_pillars", fire_pillars_icon),
    ("will_o_wisps", will_o_wisps_icon),
    ("flame_crescent", flame_crescent_icon),
    ("searing_brand", searing_brand_icon),
    ("phoenix", phoenix_icon),
    ("supernova", supernova_icon),
    ("stellar_pyre", stellar_pyre_icon),
]


def stellar_heat_effect_icon():
    def draw(d, img, S):
        for k in range(6):
            a = math.pi * (1.1 + .8 * k / 5)
            x, y = .5 + math.cos(a) * .3, .72 + math.sin(a) * .3
            _tongue(d, S, x, y + .12, .14, .24, k)
        _glow(img, S, .5, .6, .2, 3, .8)
    return g.painted(72, draw, None, 18)


# --------------------------------------------------------------------------------------------------- the spellbook

LEATHER = (34, 24, 22)
LEATHER_LIGHT = (62, 44, 38)
LEATHER_DARK = (18, 12, 12)
BRASS = (196, 150, 72)


def starfire_codex_icon():
    return g.from_grid([
        "................",
        "...oooooooooo...",
        "..obbbbbbbbbdco.",
        "..obsssssssbdco.",
        "..obsbrobybbdco.",
        "..obsbwWwbsbdcco",
        "..obsgWCWusbdco.",
        "..obsbwWwbsbdcco",
        "..obsbvbbbsbdcco",
        "..obsssssssbdcco",
        "..obbbbbbbbbdco.",
        "..oddssdddssxco.",
        "..oddddssdddxco.",
        "...ooooooooooo..",
        "................",
        "................",
    ], {"o": (10, 6, 6), "b": LEATHER, "s": BRASS, "d": LEATHER_DARK, "x": (8, 4, 4), "w": BODY[2], "W": BODY[3], "C": CORE[3],
        "r": BODY[0], "y": BODY[2], "g": BODY[1], "u": BODY[4], "v": BODY[5], "c": g.CREAM})


def starfire_codex_model_texture():
    """64x64 texture of the 3D book, laid out like the other books: covers of blackened leather framed in brass, a star
    on the front with six flames round it (red, orange, yellow, white, blue, violet), a brass spine, old pages."""
    s = 4
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    W, H = 12 * s, 9 * s
    ys, xs = np.mgrid[0:H, 0:W]
    grain = _tnoise(W, H, (6, 4), 401)
    cover = _ramp(.3 + .4 * grain, [LEATHER_DARK, LEATHER, LEATHER_LIGHT])
    img.paste(_rgba(cover, np.full((H, W), 255)), (0, 0))
    d.rectangle((1, 1, W - 2, H - 2), outline=BRASS + (255,))
    cx, cy = W // 2, H // 2
    for k in range(6):
        a = k * math.tau / 6 - math.pi / 2
        x, y = cx + math.cos(a) * 11, cy + math.sin(a) * 11
        d.polygon([(x - 2, y + 2), (x + 2, y + 2), (x, y - 4)], fill=BODY[k] + (255,))
        d.point((x, y), fill=CORE[k] + (255,))
    d.polygon([(cx, cy - 6), (cx + 2, cy - 2), (cx + 6, cy), (cx + 2, cy + 2), (cx, cy + 6), (cx - 2, cy + 2), (cx - 6, cy), (cx - 2, cy - 2)],
              fill=(255, 244, 220, 255), outline=BRASS + (255,))
    d.rectangle((0, 0, W - 1, H - 1), outline=(10, 6, 6, 255))
    d.rectangle((12 * s, 0, 13 * s - 1, 9 * s - 1), fill=LEATHER_DARK + (255,))
    for y in range(0, 8 * s):
        for x in range(13 * s, 16 * s):
            img.putpixel((x, y), g.lerp(g.CREAM, (176, 168, 140), ((x * 7 + y * 13) % 11) / 22) + (255,))
    for y in range(1, 8 * s, 2):
        d.line((13 * s, y, 16 * s - 1, y), fill=g.CREAM_SHADE + (255,))
    for y in range(9 * s, 12 * s):
        for x in range(0, 12 * s):
            t = .5 + .5 * math.sin(x * .4)
            img.putpixel((x, y), g.lerp((120, 86, 40), BRASS, t) + (255,))
    for x in range(2, 12 * s, 8):
        d.rectangle((x, 9 * s + 4, x + 2, 9 * s + 7), fill=BODY[(x // 8) % 6] + (255,))
    d.rectangle((0, 12 * s, 12 * s - 1, 15 * s - 1), fill=g.CREAM + (255,))
    for x in range(0, 12 * s, 2):
        d.line((x, 12 * s, x, 15 * s - 1), fill=g.CREAM_SHADE + (255,))
    d.rectangle((12 * s, 9 * s, 14 * s - 1, 11 * s - 1), fill=BRASS + (255,))
    for y in range(9 * s, 11 * s):
        for x in range(14 * s, 16 * s):
            t = math.hypot(x - 14.8 * s, y - 9.8 * s) / (1.4 * s)
            img.putpixel((x, y), g.lerp(CORE[3], BODY[1], min(1, t ** .8)) + (255,))
    d.rectangle((12 * s, 11 * s, 16 * s - 1, 12 * s - 1), fill=LEATHER_DARK + (255,))
    return img


def affinity_ring_starfire():
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
    ], {"o": (20, 10, 8), "h": (255, 226, 160), "l": BRASS, "b": (150, 106, 50), "s": (96, 64, 30), "v": BODY[1],
        "G": BODY[1], "C": CORE[2], "x": BODY[0]})


def main():
    for t in range(6):
        g.save(flame_texture(t), f"textures/entity/starfire/flame_{t}.png")
        g.save(wall_texture(t), f"textures/entity/starfire/wall_{t}.png")
    g.save(glow_texture(), "textures/entity/starfire/glow.png")
    g.save(spark_texture(), "textures/entity/starfire/spark.png")
    g.save(ring_texture(), "textures/entity/starfire/ring.png")
    g.save(beam_texture(), "textures/entity/starfire/beam.png")
    g.save(smoke_texture(), "textures/entity/starfire/smoke.png")
    for name, icon in ICONS:
        g.save(icon(), f"textures/gui/spell_icons/{name}.png")
    g.save(stellar_heat_effect_icon(), "textures/mob_effect/stellar_heat.png")
    g.save(affinity_ring_starfire(), "textures/item/affinity_ring_starfire.png")
    g.write_json("models/item/affinity_ring_starfire.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/affinity_ring_starfire"}})
    g.save(starfire_codex_icon(), "textures/item/starfire_codex.png")
    g.save(starfire_codex_model_texture(), "textures/item/starfire_codex_3d.png")
    g.book_models("starfire_codex")


if __name__ == "__main__":
    main()
