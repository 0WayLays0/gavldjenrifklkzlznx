#!/usr/bin/env python3
"""Lava Magic art: molten lava, black crust split by glowing cracks and veined obsidian for its spells, the spell
icons, the Magma Codex (black basalt hide cracked with fire, iron fittings, a magma gem) and its affinity ring.

Called by gen_assets.py (run that one). Original art, drawn here.
"""
import math

import numpy as np
from PIL import Image, ImageDraw

import codex_kit as ck
import gen_assets as g
from water_assets import _tnoise

HOT = (255, 230, 120)
ORANGE = (255, 122, 30)
RED = (216, 48, 26)
DARK = (60, 20, 14)
ROCK = (40, 32, 30)
IRON = (120, 116, 120)
IRON_DARK = (54, 50, 54)
NIGHT = (16, 6, 4)
BG = (58, 18, 10)


def _ramp(t, stops):
    t = np.clip(t, 0, 1)
    out = np.zeros(t.shape + (3,), np.float32)
    n = len(stops) - 1
    for i in range(n):
        lo, hi = i / n, (i + 1) / n
        m = (t >= lo) & (t <= hi)
        u = ((t - lo) / (hi - lo))[m][:, None]
        out[m] = np.array(stops[i]) * (1 - u) + np.array(stops[i + 1]) * u
    return out


def lava_texture():
    """64 x 64, tiling: molten lava, yellow-white where it is hottest, orange, and red-dark skins drifting on it."""
    n = 64
    t = _tnoise(n, n, (4, 4), 5101) * .55 + _tnoise(n, n, (8, 8), 5102) * .3 + _tnoise(n, n, (16, 16), 5103) * .15
    rgb = _ramp(t, [DARK, RED, ORANGE, (255, 180, 60), HOT, (255, 250, 210)])
    a = np.full((n, n, 1), 255, np.float32)
    return Image.fromarray(np.concatenate([rgb, a], 2).astype(np.uint8), "RGBA")


def crust_texture():
    """64 x 64, tiling: black crust of cooled lava, split by cracks still glowing."""
    n = 64
    t = _tnoise(n, n, (6, 6), 5201)
    cell = _tnoise(n, n, (5, 5), 5202)
    crack = np.exp(-((cell - .5) / .035) ** 2)
    base = _ramp(t, [(22, 18, 18), ROCK, (70, 58, 52)])
    hot = _ramp(np.clip(crack * 1.2, 0, 1), [(0, 0, 0), RED, ORANGE, HOT])
    rgb = np.clip(base * (1 - crack[..., None] * .8) + hot * crack[..., None], 0, 255)
    a = np.full((n, n, 1), 255, np.float32)
    return Image.fromarray(np.concatenate([rgb, a], 2).astype(np.uint8), "RGBA")


def obsidian_texture():
    """64 x 64: obsidian, near black with a violet glassy sheen, a thin vein of lava across it."""
    n = 64
    t = _tnoise(n, n, (5, 5), 5301)
    rgb = _ramp(t, [(10, 6, 16), (30, 18, 48), (70, 44, 110), (120, 90, 170)])
    img = Image.fromarray(np.concatenate([rgb, np.full((n, n, 1), 255, np.float32)], 2).astype(np.uint8), "RGBA")
    d = ImageDraw.Draw(img)
    pts = [(0, 40), (14, 34), (26, 38), (40, 26), (52, 30), (63, 20)]
    d.line(pts, fill=ORANGE + (255,), width=2)
    d.line(pts, fill=HOT + (255,), width=1)
    return img


def _icon(draw, bg=BG):
    return ck.icon(draw, bg, NIGHT)


def magma_spit_icon():
    def draw(d, img, S):
        d.arc((.12 * S, .2 * S, .9 * S, 1.1 * S), 200, 300, fill=ck.a(ORANGE, 160), width=2)
        g.glow_spot(img, .66 * S, .32 * S, .2 * S, ORANGE, .9)
        d.ellipse((.54 * S, .2 * S, .78 * S, .44 * S), fill=ck.a(RED))
        d.ellipse((.6 * S, .24 * S, .72 * S, .34 * S), fill=ck.a(HOT))
        d.ellipse((.14 * S, .74 * S, .62 * S, .9 * S), fill=ck.a(ORANGE))
        d.ellipse((.22 * S, .77 * S, .5 * S, .86 * S), fill=ck.a(HOT))
    return _icon(draw)


def lava_geyser_icon():
    def draw(d, img, S):
        g.glow_spot(img, .5 * S, .45 * S, .32 * S, ORANGE, .8)
        d.polygon([(.36 * S, .9 * S), (.42 * S, .2 * S), (.58 * S, .2 * S), (.64 * S, .9 * S)], fill=ck.a(ORANGE))
        d.polygon([(.44 * S, .9 * S), (.47 * S, .26 * S), (.53 * S, .26 * S), (.56 * S, .9 * S)], fill=ck.a(HOT))
        for x, y in ((.28, .22), (.72, .2), (.22, .38), (.8, .36)):
            d.ellipse(((x - .04) * S, (y - .04) * S, (x + .04) * S, (y + .04) * S), fill=ck.a(HOT))
        d.rectangle((.1 * S, .88 * S, .9 * S, .94 * S), fill=ck.a(ROCK))
    return _icon(draw)


def obsidian_carapace_icon():
    def draw(d, img, S):
        g.glow_spot(img, .5 * S, .5 * S, .3 * S, RED, .6)
        for k in range(6):
            a = k * math.tau / 6 + .3
            x, y = .5 + math.cos(a) * .28, .5 + math.sin(a) * .28
            pts = [((x + math.cos(a + b) * .12) * S, (y + math.sin(a + b) * .12) * S) for b in (0, 2.1, 4.2)]
            d.polygon(pts, fill=ck.a((40, 24, 64)), outline=ck.a((120, 90, 170)))
            d.line((x * S, y * S, (x + math.cos(a) * .06) * S, (y + math.sin(a) * .06) * S), fill=ck.a(ORANGE), width=1)
        d.ellipse((.4 * S, .4 * S, .6 * S, .6 * S), fill=ck.a(HOT))
    return _icon(draw)


def lava_river_icon():
    def draw(d, img, S):
        pts = [(.1 * S, .82 * S), (.35 * S, .62 * S), (.55 * S, .7 * S), (.9 * S, .3 * S)]
        d.line(pts, fill=ck.a(ROCK), width=12)
        d.line(pts, fill=ck.a(RED), width=9)
        d.line(pts, fill=ck.a(ORANGE), width=6)
        d.line(pts, fill=ck.a(HOT), width=2)
        g.glow_spot(img, .55 * S, .6 * S, .22 * S, ORANGE, .5)
    return _icon(draw)


def caldera_icon():
    def draw(d, img, S):
        g.glow_spot(img, .5 * S, .6 * S, .38 * S, ORANGE, .9)
        d.ellipse((.12 * S, .45 * S, .88 * S, .8 * S), fill=ck.a(ROCK))
        d.ellipse((.18 * S, .49 * S, .82 * S, .76 * S), fill=ck.a(ORANGE))
        d.ellipse((.3 * S, .54 * S, .7 * S, .7 * S), fill=ck.a(HOT))
        for x in (.32, .5, .68):
            d.polygon([((x - .04) * S, .55 * S), (x * S, .22 * S), ((x + .04) * S, .55 * S)], fill=ck.a(ORANGE))
    return _icon(draw)


ICONS = (("magma_spit", magma_spit_icon), ("lava_geyser", lava_geyser_icon), ("obsidian_carapace", obsidian_carapace_icon),
         ("lava_river", lava_river_icon), ("caldera", caldera_icon))


def _cover(d, img, W, H):
    # Cracks of fire across the black cover, meeting at a gem of magma
    cx, cy = W / 2, H / 2
    rng = np.random.default_rng(5401)
    for k in range(7):
        a = k * math.tau / 7 + rng.uniform(-.2, .2)
        x, y = cx, cy
        pts = [(x, y)]
        for _ in range(5):
            a += rng.uniform(-.5, .5)
            x, y = x + math.cos(a) * 7, y + math.sin(a) * 6
            pts.append((x, y))
        d.line(pts, fill=RED + (255,), width=3)
        d.line(pts, fill=ORANGE + (255,), width=1)
    d.ellipse((cx - 6, cy - 6, cx + 6, cy + 6), fill=ORANGE + (255,))
    d.ellipse((cx - 3, cy - 3, cx + 3, cy + 3), fill=HOT + (255,))


def main():
    g.save(lava_texture(), "textures/entity/lava/lava.png")
    g.save(crust_texture(), "textures/entity/lava/crust.png")
    g.save(obsidian_texture(), "textures/entity/lava/obsidian.png")
    for name, fn in ICONS:
        g.save(fn(), f"textures/gui/spell_icons/{name}.png")
    hide = [(14, 10, 10), (30, 24, 22), (50, 40, 36), (74, 60, 54)]
    tex = ck.codex_texture(hide, IRON, IRON_DARK, ORANGE, _cover, 5501)
    ck.write_book("magma_codex", ck.codex_icon((14, 10, 10), (40, 30, 28), (70, 54, 48), IRON, IRON_DARK, ORANGE), tex)
    ck.write_ring("lava", ck.ring((24, 12, 10), IRON, (190, 186, 190), IRON_DARK, ORANGE, HOT))


if __name__ == "__main__":
    main()
