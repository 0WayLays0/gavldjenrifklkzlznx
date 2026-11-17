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


def acid_texture():
    """64 x 64, tiling: acid, pale (tinted in game): a swirling liquid, brighter where it catches the light, with
    bubbles of every size in it."""
    n = 64
    t = _tnoise(n, n, (4, 4), 7101) * .6 + _tnoise(n, n, (8, 8), 7102) * .4
    swirl = np.sin((t * 6 + _tnoise(n, n, (2, 2), 7103) * 4) * math.pi) * .5 + .5
    rgb = np.zeros((n, n, 3), np.float32)
    lo, hi = (120, 200, 60), (235, 255, 200)
    for i in range(3):
        rgb[..., i] = lo[i] + (hi[i] - lo[i]) * (.55 * t + .45 * swirl)
    rng = np.random.default_rng(7104)
    ys, xs = np.mgrid[0:n, 0:n].astype(np.float32)
    for _ in range(26):
        cx, cy, r = rng.uniform(0, n), rng.uniform(0, n), rng.uniform(1.2, 4.5)
        dx = np.minimum(np.abs(xs - cx), n - np.abs(xs - cx))
        dy = np.minimum(np.abs(ys - cy), n - np.abs(ys - cy))
        d = np.sqrt(dx * dx + dy * dy) / r
        rim = np.exp(-((d - 1) / .18) ** 2)
        rgb += rim[..., None] * 60
        rgb += (np.exp(-((dx + r * .35) ** 2 + (dy + r * .35) ** 2) / (r * .25) ** 2))[..., None] * 90
    rgb = np.clip(rgb, 0, 255)
    return Image.fromarray(np.concatenate([rgb, np.full((n, n, 1), 255, np.float32)], 2).astype(np.uint8), "RGBA")


def froth_texture():
    """64 x 64, tiling: froth, white bubbles crowded together, thinning to clear between them."""
    n = 64
    img = np.zeros((n, n, 4), np.float32)
    rng = np.random.default_rng(7201)
    ys, xs = np.mgrid[0:n, 0:n].astype(np.float32)
    for _ in range(70):
        cx, cy, r = rng.uniform(0, n), rng.uniform(0, n), rng.uniform(1.5, 5)
        dx = np.minimum(np.abs(xs - cx), n - np.abs(xs - cx))
        dy = np.minimum(np.abs(ys - cy), n - np.abs(ys - cy))
        d = np.sqrt(dx * dx + dy * dy) / r
        a = np.clip(1 - d, 0, 1) ** .4 * .55 + np.exp(-((d - .9) / .15) ** 2) * .6
        img[..., 3] = np.maximum(img[..., 3], np.clip(a, 0, 1) * 255)
    img[..., 0] = 250
    img[..., 1] = 255
    img[..., 2] = 225
    return Image.fromarray(img.astype(np.uint8), "RGBA")


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
