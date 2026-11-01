#!/usr/bin/env python3
"""Stellar Fire art: the filaments flames are drawn with (tinted in game in each of the seven colours: red, orange,
yellow, white, blue, violet, the colours of the star classes M, K, G, F, B and O, and green, copper's), an ember, a
halo, a spark, smoke, a ring of light, each spell's icon in each colour, the Starfire Codex (icon and 3D book texture) and its affinity ring.

Called by gen_assets.py (run that one). Pixel style, shaded in bands. Original art.
"""
import math

import numpy as np
from PIL import Image, ImageDraw

import gen_assets as g
from water_assets import _tnoise, _quant, _rgba, _ramp

#: Per colour (see HeatColors.java): heart, body, tips.
CORE = [(255, 200, 144), (255, 226, 166), (255, 248, 220), (255, 255, 255), (234, 242, 255), (242, 232, 255), (234, 255, 216)]
BODY = [(240, 64, 28), (255, 134, 30), (255, 203, 56), (255, 242, 220), (79, 147, 255), (155, 92, 255), (59, 224, 90)]
TIP = [(126, 18, 6), (184, 64, 10), (216, 118, 14), (201, 214, 255), (28, 63, 216), (74, 26, 184), (10, 110, 44)]
SOOT = (40, 30, 28)
NIGHT = (14, 10, 12)


def _vnoise(x, y, cx, cy, seed):
    """Smooth value noise at (x, y) in [0, 1) (both tile), cx by cy cells, in [0, 1]."""
    grid = np.random.default_rng(seed).random((cy, cx))
    gx, gy = (x % 1) * cx, (y % 1) * cy
    x0, y0 = np.floor(gx).astype(int), np.floor(gy).astype(int)
    fx, fy = gx - x0, gy - y0
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    x1, y1 = (x0 + 1) % cx, (y0 + 1) % cy
    x0, y0 = x0 % cx, y0 % cy
    return (grid[y0, x0] * (1 - fx) + grid[y0, x1] * fx) * (1 - fy) + (grid[y1, x0] * (1 - fx) + grid[y1, x1] * fx) * fy


def _fbm(x, y, seed, cx=3, cy=2, octaves=4):
    out, amp, total = 0, 1, 0
    for o in range(octaves):
        out = out + amp * _vnoise(x, y, cx << o, cy << o, seed + o)
        total += amp
        amp *= .5
    return out / total


def _fire_field(W, H, seed):
    """Turbulent flame filaments, tiling: bright licks running up V, twisting and breaking, 0 to 1."""
    ys, xs = np.mgrid[0:H, 0:W]
    u, v = (xs + .5) / W, (ys + .5) / H
    # Warp the space twice, more across than along (flames stream up, twisting)
    w1 = _fbm(u, v, seed, 2, 2, 3)
    w2 = _fbm(u + .3, v + .7, seed + 10, 2, 2, 3)
    x = u + (w1 - .5) * .55
    y = v + (w2 - .5) * .25
    n = _fbm(x, y, seed + 20, 3, 2, 4)
    ridge = 1 - np.abs(n * 2 - 1)
    lick = np.clip((ridge - .62) / .36, 0, 1) ** 1.3
    fine = _fbm(x, y, seed + 30, 6, 3, 3)
    f = lick * (.7 + .5 * fine) + .18 * np.clip(fine * 2 - .8, 0, 1)
    return np.clip(f, 0, 1)


def flame_noise_texture():
    """32 x 64, tiling both ways: flame filaments, white on black (drawn added, tinted per vertex, scrolled up)."""
    f = _quant(_fire_field(32, 64, 801), 10)
    return _rgba(np.dstack([f * 255] * 3), np.full((64, 32), 255))


def flame_body_texture():
    """32 x 64, tiling both ways: the body of a flame, white (tinted per vertex), its see-through-ness following the
    filaments, so that it has substance in daylight (drawn see-through, scrolled up)."""
    f = _fire_field(32, 64, 811)
    a = _quant(np.clip(.35 + f * .9, 0, 1), 8)
    return _rgba(np.full((64, 32, 3), 255.0), a * 255)


def ember_texture():
    """16 x 16 ember: a hot point in a soft glow, white on black (drawn added)."""
    S = 16
    ys, xs = np.mgrid[0:S, 0:S]
    r = np.hypot((xs + .5) / S - .5, (ys + .5) / S - .5) * 2
    k = np.clip(1 - r * 3, 0, 1) ** .7 + .45 * np.clip(1 - r, 0, 1) ** 2.5
    return _rgba(np.dstack([_quant(np.clip(k, 0, 1), 8) * 255] * 3), np.full((S, S), 255))


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


def _bg(t):
    """An icon's dark ground, tinged with the heat's colour."""
    return tuple(int(NIGHT[i] * .55 + TIP[t][i] * .32) for i in range(3))


def ember_bolt_icon(t):
    def draw(d, img, S):
        for k in range(5):
            u = k / 4
            _tongue(d, S, .26 + .3 * u, .72 - .3 * u, .2 - .03 * k, .3, t, lean=-.6)
        d.ellipse((.56 * S, .2 * S, .84 * S, .48 * S), fill=_a(BODY[t]))
        d.ellipse((.62 * S, .26 * S, .78 * S, .42 * S), fill=_a(CORE[t]))
        _glow(img, S, .7, .34, .22, t)
    return _icon(draw, .65, .35, _bg(t))


def stellar_breath_icon(t):
    def draw(d, img, S):
        d.ellipse((.04 * S, .42 * S, .2 * S, .6 * S), fill=_a((120, 90, 80)))
        for k in range(9):
            u = k / 8
            _tongue(d, S, .2 + .65 * u, .62 + .1 * math.sin(k * 2.3), .12 + .18 * u, .18 + .3 * u, t, lean=-.3)
        _glow(img, S, .22, .5, .16, t)
    return _icon(draw, .3, .5, _bg(t))


def corona_icon(t):
    def draw(d, img, S):
        d.ellipse((.12 * S, .5 * S, .88 * S, .9 * S), outline=_a(BODY[t]), width=max(2, S // 16))
        for k in range(12):
            a = k * math.tau / 12
            x, y = .5 + math.cos(a) * .38, .7 + math.sin(a) * .2
            _tongue(d, S, x, y, .1, .22 + .08 * math.sin(a), t)
        _glow(img, S, .5, .66, .2, t, .8)
        d.rectangle((.46 * S, .5 * S, .54 * S, .7 * S), fill=_a((90, 70, 110)))
    return _icon(draw, .5, .65, _bg(t))


def fire_pillars_icon(t):
    def draw(d, img, S):
        d.rectangle((0, .86 * S, S, S), fill=_a((50, 36, 30)))
        for k, (x, h) in enumerate(((.2, .45), (.45, .62), (.72, .78))):
            _tongue(d, S, x, .88, .2, h, t)
            _glow(img, S, x, .86, .1, t)
    return _icon(draw, .5, .7, _bg(t))


def will_o_wisps_icon(t):
    def draw(d, img, S):
        for x, y in ((.28, .62), (.7, .4), (.42, .3)):
            _tongue(d, S, x, y + .1, .14, .22, t)
            _glow(img, S, x, y, .12, t, 1.2)
        d.arc((.15 * S, .2 * S, .85 * S, .8 * S), 200, 340, fill=_a(CORE[t], 120), width=max(1, S // 40))
    return _icon(draw, .5, .5, _bg(t))


def flame_crescent_icon(t):
    def draw(d, img, S):
        for k, w in enumerate((.11, .07, .035)):
            col = (BODY[t], tuple((BODY[t][i] + CORE[t][i]) // 2 for i in range(3)), CORE[t])[k]
            d.arc((.12 * S, .16 * S, .9 * S, .94 * S), 200, 340, fill=_a(col), width=max(1, int(w * S)))
        for k in range(6):
            a = math.radians(205 + k * 25)
            x, y = .51 + math.cos(a) * .39, .55 + math.sin(a) * .39
            _tongue(d, S, x, y + .12, .09, .14, t, lean=-.3)
    return _icon(draw, .5, .4, _bg(t))


def searing_brand_icon(t):
    def draw(d, img, S):
        d.rectangle((.42 * S, .52 * S, .58 * S, .92 * S), fill=_a((92, 80, 64)))
        d.ellipse((.4 * S, .4 * S, .6 * S, .56 * S), fill=_a((110, 96, 76)))
        d.ellipse((.18 * S, .14 * S, .82 * S, .34 * S), outline=_a(BODY[t]), width=max(2, S // 20))
        for k in range(6):
            a = k * math.tau / 6
            d.line((.5 * S, .24 * S, (.5 + math.cos(a) * .3) * S, (.24 + math.sin(a) * .09) * S), fill=_a(CORE[t]), width=max(1, S // 40))
        _glow(img, S, .5, .24, .2, t, 1.2)
        for x in (.3, .7):
            _tongue(d, S, x, .3, .08, .12, t)
    return _icon(draw, .5, .3, _bg(t))


def phoenix_icon(t):
    def draw(d, img, S):
        for side in (-1, 1):
            for k in range(5):
                u = k / 4
                pts = [(.5 * S, .5 * S), ((.5 + side * (.15 + .3 * u)) * S, (.2 + .2 * u) * S), ((.5 + side * (.2 + .28 * u)) * S, (.32 + .2 * u) * S)]
                d.polygon(pts, fill=_a(BODY[t] if k % 2 else TIP[t]))
        for k in range(3):
            _tongue(d, S, .5 + (k - 1) * .06, .95, .1, .38, t, lean=(k - 1) * .2)
        d.ellipse((.42 * S, .4 * S, .58 * S, .62 * S), fill=_a(BODY[t]))
        d.ellipse((.45 * S, .3 * S, .55 * S, .42 * S), fill=_a(CORE[t]))
        _glow(img, S, .5, .45, .18, t, 1.2)
    return _icon(draw, .5, .45, _bg(t))


def supernova_icon(t):
    def draw(d, img, S):
        for k in range(16):
            a = k * math.tau / 16
            r1 = .18 if k % 2 else .24
            d.line((.5 * S, .5 * S, (.5 + math.cos(a) * (r1 + .22)) * S, (.5 + math.sin(a) * (r1 + .22)) * S), fill=_a(BODY[t]), width=max(1, S // 28))
        d.ellipse((.22 * S, .22 * S, .78 * S, .78 * S), outline=_a(BODY[t]), width=max(2, S // 18))
        d.ellipse((.34 * S, .34 * S, .66 * S, .66 * S), fill=_a(BODY[t]))
        d.ellipse((.42 * S, .42 * S, .58 * S, .58 * S), fill=_a(CORE[t]))
        _glow(img, S, .5, .5, .3, t, 1.2)
    return _icon(draw, .5, .5, _bg(t))


def stellar_pyre_icon(t):
    def draw(d, img, S):
        d.rectangle((0, .88 * S, S, S), fill=_a((40, 30, 30)))
        for k in range(6):
            u = k / 5
            y = .86 - .62 * u
            w = .5 - .2 * u
            d.arc(((.5 - w) * S, (y - .06) * S, (.5 + w) * S, (y + .06) * S), 0, 180, fill=_a(BODY[t] if k % 2 else CORE[t]), width=max(2, S // 18))
        for k in range(6):
            _tongue(d, S, .3 + .08 * k, .9, .12, .25 + .05 * k, t)
        _glow(img, S, .5, .4, .25, t, .7)
    return _icon(draw, .5, .5, _bg(t))


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
#: The seven colours, as the spells are named (see HeatColors.ID).
HEAT_IDS = ["red", "orange", "yellow", "white", "blue", "violet", "green"]


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
    g.save(glow_texture(), "textures/entity/starfire/glow.png")
    g.save(flame_noise_texture(), "textures/entity/starfire/flame_noise.png")
    g.save(flame_body_texture(), "textures/entity/starfire/flame_body.png")
    g.save(ember_texture(), "textures/entity/starfire/ember.png")
    g.save(spark_texture(), "textures/entity/starfire/spark.png")
    g.save(ring_texture(), "textures/entity/starfire/ring.png")
    g.save(beam_texture(), "textures/entity/starfire/beam.png")
    g.save(smoke_texture(), "textures/entity/starfire/smoke.png")
    for name, icon in ICONS:
        for t, heat in enumerate(HEAT_IDS):
            g.save(icon(t), f"textures/gui/spell_icons/{name}_{heat}.png")
    g.save(affinity_ring_starfire(), "textures/item/affinity_ring_starfire.png")
    g.write_json("models/item/affinity_ring_starfire.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/affinity_ring_starfire"}})
    g.save(starfire_codex_icon(), "textures/item/starfire_codex.png")
    g.save(starfire_codex_model_texture(), "textures/item/starfire_codex_3d.png")
    g.book_models("starfire_codex")


if __name__ == "__main__":
    main()
