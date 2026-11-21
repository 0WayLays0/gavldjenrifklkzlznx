#!/usr/bin/env python3
"""Acid Magic art: the bubbling green liquid and the froth of its spells, the spell icons, the Corrosive Codex (a
pitted, acid-stained hide, tarnished green-copper fittings, a vial of glowing acid set in its covers) and its affinity
ring.

Called by gen_assets.py (run that one). Original art, drawn here.
"""
import math

import numpy as np
from PIL import Image, ImageDraw

import codex_kit as ck
import gen_assets as g
from boss_materials import fbm, image, mix, ramp, smooth, worley
from water_assets import _tnoise

ACID = (180, 240, 58)
BRIGHT = (216, 255, 74)
DEEP = (60, 154, 20)
FUME = (154, 204, 58)
CLOUD = (76, 90, 46)
COPPER = (96, 170, 130)
COPPER_DARK = (40, 84, 62)
NIGHT = (10, 16, 6)
BG = (30, 42, 18)


def _norm(a):
    return (a - a.min()) / max(1e-6, a.max() - a.min())


def acid_texture():
    """256 x 256, tiling: acid, pale (tinted in game): a glossy liquid swirling, caustic lights rippling across it, deeper
    pools darker, bubbles of every size rising, each with its rim and a glint."""
    n = 256
    swirl = _norm(fbm(n, n, 3, 6, 7101))
    warp = _norm(fbm(n, n, 2, 4, 7102))
    f1, f2 = worley(n, n, 34, 7103)
    caustic = np.exp(-((f2 - f1) / .05) ** 2)
    t = .55 * swirl + .25 * warp
    rgb = ramp(t, [(0, (110, 176, 60)), (.5, (176, 226, 120)), (1, (226, 250, 196))])
    rgb = mix(rgb, (250, 255, 230), caustic * .3 * smooth(.3, .7, _norm(fbm(n, n, 3, 3, 7105))))
    img = image(rgb)
    d = ImageDraw.Draw(img)
    rng = np.random.default_rng(7104)
    for _ in range(46):
        x, y = rng.uniform(0, n, 2)
        r = rng.uniform(2, 11) if rng.random() < .8 else rng.uniform(12, 18)
        for ox in (-n, 0, n):
            for oy in (-n, 0, n):
                cx, cy = x + ox, y + oy
                d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(200, 240, 150, 255), outline=(250, 255, 236, 255), width=max(1, int(r / 5)))
                d.ellipse((cx - r * .55, cy - r * .6, cx - r * .1, cy - r * .2), fill=(255, 255, 255, 255))
                d.arc((cx - r * .8, cy - r * .8, cx + r * .8, cy + r * .8), 20, 110, fill=(120, 180, 70, 255), width=max(1, int(r / 6)))
    return img


def froth_texture():
    """128 x 128, tiling: froth: white bubbles crowded together, large and small, each a thin rim with a glint, clear
    between them."""
    n = 128
    img = Image.new("RGBA", (n * 2, n * 2), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    rng = np.random.default_rng(7201)
    for _ in range(140):
        x, y = rng.uniform(0, n * 2, 2)
        r = rng.uniform(3, 14)
        for ox in (-n * 2, 0, n * 2):
            for oy in (-n * 2, 0, n * 2):
                cx, cy = x + ox, y + oy
                d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(250, 255, 236, 70), outline=(250, 255, 236, 235), width=max(1, int(r / 4)))
                d.ellipse((cx - r * .55, cy - r * .6, cx - r * .15, cy - r * .2), fill=(255, 255, 255, 255))
    return img.resize((n, n), Image.LANCZOS)


def _icon(draw, bg=BG):
    return ck.icon(draw, bg, NIGHT)


def _drop(d, S, x, y, r, c):
    d.polygon([(x * S, (y - r * 1.9) * S), ((x - r * .8) * S, (y - r * .3) * S), ((x + r * .8) * S, (y - r * .3) * S)], fill=ck.a(c))
    d.ellipse(((x - r) * S, (y - r) * S, (x + r) * S, (y + r) * S), fill=ck.a(c))


def acid_spray_icon():
    def draw(d, img, S):
        g.glow_spot(img, .2 * S, .75 * S, .2 * S, BRIGHT, .9)
        rng = np.random.default_rng(7301)
        for k in range(14):
            a = -.95 + rng.uniform(0, .75)
            r = rng.uniform(.25, .75)
            x, y = .2 + math.cos(a) * r, .75 + math.sin(a) * r
            d.line((.2 * S, .75 * S, x * S, y * S), fill=ck.a(ACID, 90), width=1)
            d.ellipse(((x - .03) * S, (y - .03) * S, (x + .03) * S, (y + .03) * S), fill=ck.a(BRIGHT))
        d.ellipse((.12 * S, .67 * S, .28 * S, .83 * S), fill=ck.a((240, 255, 200)))
    return _icon(draw)


def corrosive_pool_icon():
    def draw(d, img, S):
        g.glow_spot(img, .5 * S, .7 * S, .4 * S, ACID, .6)
        d.ellipse((.1 * S, .55 * S, .9 * S, .88 * S), fill=ck.a(DEEP))
        d.ellipse((.16 * S, .58 * S, .84 * S, .84 * S), fill=ck.a(ACID))
        for x, y, r in ((.35, .68, .05), (.6, .72, .07), (.5, .64, .035), (.7, .66, .03)):
            d.ellipse(((x - r) * S, (y - r) * S, (x + r) * S, (y + r) * S), outline=ck.a((240, 255, 200)), width=2)
        for k, x in enumerate((.3, .5, .7)):
            for i in range(4):
                y = .5 - i * .1
                d.ellipse(((x - .04 + .02 * math.sin(i + k)) * S, (y - .04) * S, (x + .04 + .02 * math.sin(i + k)) * S, (y + .04) * S),
                          fill=ck.a(FUME, 150 - i * 30))
    return _icon(draw)


def acid_rain_icon():
    def draw(d, img, S):
        for x, y, r in ((.3, .25, .17), (.5, .2, .2), (.7, .26, .16), (.45, .32, .15), (.62, .33, .14)):
            d.ellipse(((x - r) * S, (y - r) * S, (x + r) * S, (y + r) * S), fill=ck.a(CLOUD))
        g.glow_spot(img, .5 * S, .28 * S, .2 * S, ACID, .5)
        for x, y in ((.25, .6), (.42, .72), (.58, .58), (.74, .7), (.34, .88), (.66, .9)):
            _drop(d, S, x, y, .035, BRIGHT)
    return _icon(draw)


def dissolve_icon():
    def draw(d, img, S):
        g.glow_spot(img, .5 * S, .5 * S, .42 * S, ACID, .7)
        d.ellipse((.18 * S, .14 * S, .82 * S, .88 * S), outline=ck.a(BRIGHT), width=3)
        # A helmet melting inside
        d.chord((.32 * S, .34 * S, .68 * S, .7 * S), 180, 360, fill=ck.a((150, 160, 150)))
        d.rectangle((.32 * S, .52 * S, .68 * S, .56 * S), fill=ck.a((150, 160, 150)))
        for x in (.36, .45, .55, .63):
            d.line((x * S, .56 * S, x * S, (.62 + .08 * math.sin(x * 30)) * S), fill=ck.a((150, 160, 150)), width=3)
        for x, y, r in ((.3, .78, .04), (.6, .8, .05), (.7, .3, .03), (.26, .4, .03)):
            d.ellipse(((x - r) * S, (y - r) * S, (x + r) * S, (y + r) * S), outline=ck.a((240, 255, 200)), width=1)
    return _icon(draw)


def corroding_tide_icon():
    def draw(d, img, S):
        g.glow_spot(img, .5 * S, .62 * S, .45 * S, ACID, .5)
        for k, (r, c) in enumerate(((.42, DEEP), (.3, ACID), (.18, BRIGHT))):
            d.arc(((.5 - r) * S, (.62 - r * .55) * S, (.5 + r) * S, (.62 + r * .55) * S), 180, 360, fill=ck.a(c), width=4 - k)
        for k in range(9):
            a = math.pi + k * math.pi / 8
            x, y = .5 + math.cos(a) * .44, .62 + math.sin(a) * .26
            d.ellipse(((x - .025) * S, (y - .06) * S, (x + .025) * S, (y - .01) * S), fill=ck.a((240, 255, 210)))
        d.ellipse((.44 * S, .56 * S, .56 * S, .68 * S), fill=ck.a((240, 255, 200)))
    return _icon(draw)


ICONS = (("acid_spray", acid_spray_icon), ("corrosive_pool", corrosive_pool_icon), ("acid_rain", acid_rain_icon),
         ("dissolve", dissolve_icon), ("corroding_tide", corroding_tide_icon))


def _cover(d, img, W, H):
    # A vial of glowing acid on the cover, drips running from it, pits eaten into the hide around
    cx, cy = W / 2, H / 2
    d.rectangle((cx - 3, cy - 18, cx + 3, cy - 12), fill=COPPER + (255,))
    d.ellipse((cx - 11, cy - 12, cx + 11, cy + 12), fill=(40, 60, 30, 255))
    d.ellipse((cx - 9, cy - 4, cx + 9, cy + 10), fill=ACID + (255,))
    d.ellipse((cx - 4, cy - 1, cx + 1, cy + 3), fill=(240, 255, 200, 255))
    for k, x in enumerate((cx - 6, cx + 2, cx + 7)):
        d.line((x, cy + 11, x, cy + 18 + k * 4), fill=ACID + (255,), width=2)
        d.ellipse((x - 2, cy + 17 + k * 4, x + 2, cy + 21 + k * 4), fill=BRIGHT + (255,))
    rng = np.random.default_rng(7401)
    for _ in range(14):
        x, y, r = rng.uniform(8, W - 8), rng.uniform(8, H - 8), rng.uniform(1, 2.5)
        if abs(x - cx) < 14 and abs(y - cy) < 26:
            continue
        d.ellipse((x - r, y - r, x + r, y + r), fill=(30, 40, 20, 255))


def main():
    g.save(acid_texture(), "textures/entity/acid/acid.png")
    g.save(froth_texture(), "textures/entity/acid/froth.png")
    for name, fn in ICONS:
        g.save(fn(), f"textures/gui/spell_icons/{name}.png")
    hide = [(20, 26, 12), (40, 52, 24), (70, 88, 40), (110, 130, 64)]
    tex = ck.codex_texture(hide, COPPER, COPPER_DARK, (180, 255, 60), _cover, 7501)
    ck.write_book("corrosive_codex", ck.codex_icon((20, 26, 12), (48, 64, 28), (86, 108, 48), COPPER, COPPER_DARK, (180, 255, 60)), tex)
    ck.write_ring("acid", ck.ring((22, 30, 12), COPPER, (200, 240, 210), COPPER_DARK, (180, 255, 60), (230, 255, 160)))


if __name__ == "__main__":
    main()
