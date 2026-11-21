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
from boss_materials import fbm, image, mix, noise, ramp, smooth, worley
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


def _norm(a):
    return (a - a.min()) / max(1e-6, a.max() - a.min())


def lava_texture():
    """256 x 256, tiling: molten lava: rivers of white-yellow heat swirling between drifting skins of darker red, the
    skins split by bright seams where they part."""
    n = 256
    warp = fbm(n, n, 2, 4, 5105)
    flow = _norm(fbm(n, n, 3, 6, 5101, stretch=2.5))
    swirl = _norm(fbm(n, n, 4, 5, 5102))
    heat = _norm(.6 * flow + .4 * swirl + .5 * np.sin((flow + warp) * 9) * .25)
    f1, f2 = worley(n, n, 18, 5103)
    skin = smooth(.42, .25, heat) * smooth(.05, .14, f2 - f1)
    heat = np.clip(heat - .35 * skin + .35 * smooth(.05, 0, f2 - f1) * smooth(.35, .2, heat), 0, 1)
    rgb = ramp(heat, [(0, (64, 12, 8)), (.22, (140, 26, 10)), (.42, (222, 70, 14)), (.62, (255, 144, 34)), (.8, (255, 210, 86)),
                      (1, (255, 250, 210))])
    return image(rgb)


def crust_texture():
    """256 x 256, tiling: crust of cooled lava, black and rough, split into plates by cracks still glowing: white-hot in
    their depth, orange, then red where the crust closes over them."""
    n = 256
    f1, f2 = worley(n, n, 22, 5201)
    gap = f2 - f1
    crack = smooth(.07, 0, gap)
    halo = smooth(.2, 0, gap)
    rough = _norm(fbm(n, n, 8, 5, 5202))
    plate = _norm(f1)
    base = ramp(.25 + rough * .5 - plate * .2, [(0, (14, 11, 11)), (.5, (40, 32, 30)), (1, (82, 68, 60))])
    # Each plate a little raised in its middle, catching light on its top left
    lit = np.clip(.5 + (np.roll(plate, 2, 0) - plate) * 6 + (np.roll(plate, 2, 1) - plate) * 4, 0, 1)
    base = base * (.75 + .5 * lit)[..., None]
    base = mix(base, (120, 30, 10), halo * .55)
    hot = ramp(np.clip(crack * 1.15, 0, 1), [(0, (150, 24, 8)), (.4, (240, 90, 16)), (.75, (255, 190, 60)), (1, (255, 248, 200))])
    rgb = mix(base, hot, np.clip(crack * 1.3, 0, 1))
    return image(rgb)


def obsidian_texture():
    """256 x 256: obsidian, glassy black with violet depths, curved sheens where it broke, a vein of lava across."""
    n = 256
    t = _norm(fbm(n, n, 3, 5, 5301))
    rgb = ramp(t, [(0, (6, 4, 10)), (.5, (24, 14, 38)), (.8, (58, 34, 92)), (1, (110, 80, 160))])
    xs, ys = np.mgrid[0:n, 0:n][1].astype(float), np.mgrid[0:n, 0:n][0].astype(float)
    # Conchoidal sheens: soft curved bands of light, as glass breaks
    for cx, cy, r, w in ((60, 70, 70, 7), (190, 160, 90, 9), (120, 220, 60, 6)):
        d = np.hypot(xs - cx, ys - cy)
        side = .5 + .5 * np.cos(np.arctan2(ys - cy, xs - cx) + 2.2)
        band = np.exp(-((d - r) / w) ** 2) * side ** 2
        rgb = mix(rgb, (190, 170, 230), band * .5)
    img = image(rgb)
    d = ImageDraw.Draw(img)
    pts = [(-8, 128), (40, 140), (80, 150), (120, 110), (170, 120), (210, 100), (264, 128)]
    d.line(pts, fill=(140, 30, 10, 255), width=9, joint="curve")
    d.line(pts, fill=ORANGE + (255,), width=5, joint="curve")
    d.line(pts, fill=HOT + (255,), width=2, joint="curve")
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
