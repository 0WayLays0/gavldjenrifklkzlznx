#!/usr/bin/env python3
"""Plant Magic art: a leaf, a petal, the green fibre of stems and the red flesh of the carnivorous flower's mouth for its
spells, the spell icons, the Herbarium Codex (moss-green hide, bronze fittings, a pressed flower and an emerald on its
covers) and its affinity ring.

Called by gen_assets.py (run that one). Original art, drawn here.
"""
import math

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

import codex_kit as ck
import gen_assets as g
from water_assets import _tnoise

GREEN = (90, 207, 72)
LEAF = (106, 207, 88)
DARK_GREEN = (36, 90, 30)
PINK = (255, 138, 200)
GOLD = (255, 216, 74)
SPORE = (154, 216, 96)
RED = (200, 40, 60)
BRONZE = (196, 140, 70)
BRONZE_DARK = (100, 64, 28)
NIGHT = (8, 18, 8)
BG = (24, 52, 26)


def leaf_texture():
    """64 x 64: a leaf, white-green (tinted in game), its midrib and veins, pointed at the top, its stalk at the bottom."""
    n = 256
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    pts = []
    for i in range(41):
        t = i / 40
        y = n * (.96 - .9 * t)
        w = math.sin(math.pi * t) ** .8 * n * .36
        pts.append((n / 2 + w, y))
    pts += [(n - x, y) for x, y in reversed(pts)]
    d.polygon(pts, fill=(235, 255, 225, 255))
    d.line((n / 2, n * .98, n / 2, n * .08), fill=(190, 225, 170, 255), width=6)
    for k in range(6):
        y = n * (.85 - k * .12)
        for s in (-1, 1):
            d.line((n / 2, y, n / 2 + s * n * .26, y - n * .1), fill=(200, 235, 185, 255), width=3)
    return img.resize((64, 64), Image.LANCZOS)


def petal_texture():
    """64 x 64: a petal, white with a blush toward its base (tinted in game), rounded at the tip."""
    n = 256
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    ys, xs = np.mgrid[0:n, 0:n].astype(np.float32) / n
    t = 1 - ys
    w = np.clip(np.sin(np.pi * np.clip(t, 0, 1)), 0, 1) ** .6 * .42 + .02
    inside = np.abs(xs - .5) < w * (t > .02)
    shade = .75 + .25 * t
    rgba = np.zeros((n, n, 4), np.float32)
    rgba[..., 0] = 255
    rgba[..., 1] = 255 * shade
    rgba[..., 2] = 255 * (.85 + .15 * t)
    rgba[..., 3] = inside * 255
    img = Image.fromarray(rgba.astype(np.uint8), "RGBA").filter(ImageFilter.GaussianBlur(1.5))
    return img.resize((64, 64), Image.LANCZOS)


def stem_texture():
    """32 x 32, tiling: the green fibre of a stem, streaked along its length."""
    n = 32
    t = _tnoise(n, n, (2, 8), 6101) * .6 + _tnoise(n, n, (4, 16), 6102) * .4
    rgb = np.zeros((n, n, 3), np.float32)
    for i, (a, b) in enumerate(zip(DARK_GREEN, LEAF)):
        rgb[..., i] = a + (b - a) * t
    return Image.fromarray(np.concatenate([rgb, np.full((n, n, 1), 255, np.float32)], 2).astype(np.uint8), "RGBA")


def flesh_texture():
    """64 x 64: the inside of the carnivorous flower's mouth: wet red flesh with darker veins and pale glistening."""
    n = 64
    t = _tnoise(n, n, (6, 6), 6201)
    cell = _tnoise(n, n, (4, 4), 6202)
    vein = np.exp(-((cell - .5) / .05) ** 2)
    rgb = np.zeros((n, n, 3), np.float32)
    for i, (a, b) in enumerate(zip((120, 16, 30), (230, 70, 90))):
        rgb[..., i] = a + (b - a) * t
    rgb *= (1 - .45 * vein)[..., None]
    rgb = np.clip(rgb + (t > .85)[..., None] * 50, 0, 255)
    return Image.fromarray(np.concatenate([rgb, np.full((n, n, 1), 255, np.float32)], 2).astype(np.uint8), "RGBA")


def _icon(draw, bg=BG):
    return ck.icon(draw, bg, NIGHT)


def _petals(d, S, cx, cy, r, c, n=6, w=.5):
    for k in range(n):
        a = k * math.tau / n
        x, y = cx + math.cos(a) * r * .55, cy + math.sin(a) * r * .55
        d.ellipse(((x - r * w * .5) * S, (y - r * w * .5) * S, (x + r * w * .5) * S, (y + r * w * .5) * S), fill=ck.a(c))


def thorn_bloom_icon():
    def draw(d, img, S):
        d.line((.5 * S, .95 * S, .5 * S, .55 * S), fill=ck.a(GREEN), width=3)
        _petals(d, S, .5, .45, .32, PINK, 7, .7)
        d.ellipse((.43 * S, .38 * S, .57 * S, .52 * S), fill=ck.a(GOLD))
        for k in range(8):
            a = k * math.tau / 8
            x, y = .5 + math.cos(a) * .42, .45 + math.sin(a) * .38
            d.polygon([(x * S, y * S), ((x - math.cos(a) * .1 + math.sin(a) * .03) * S, (y - math.sin(a) * .1 - math.cos(a) * .03) * S),
                       ((x - math.cos(a) * .1 - math.sin(a) * .03) * S, (y - math.sin(a) * .1 + math.cos(a) * .03) * S)], fill=ck.a((200, 230, 160)))
    return _icon(draw)


def pollen_cloud_icon():
    def draw(d, img, S):
        g.glow_spot(img, .5 * S, .45 * S, .36 * S, GOLD, .7)
        rng = np.random.default_rng(6301)
        for _ in range(22):
            x, y = rng.uniform(.15, .85), rng.uniform(.15, .75)
            d.ellipse(((x - .025) * S, (y - .025) * S, (x + .025) * S, (y + .025) * S), fill=ck.a((255, 240, 140)))
        _petals(d, S, .3, .82, .14, (255, 255, 255), 5, .7)
        _petals(d, S, .7, .84, .14, PINK, 5, .7)
    return _icon(draw)


def seed_volley_icon():
    def draw(d, img, S):
        for k in range(5):
            a = -.5 + k * .25
            x, y = .2 + math.cos(a) * .55, .8 + math.sin(a) * .55 - .45
            d.line((.2 * S, .8 * S, x * S, y * S), fill=ck.a(SPORE, 120), width=1)
            d.ellipse(((x - .05) * S, (y - .035) * S, (x + .05) * S, (y + .035) * S), fill=ck.a((150, 110, 60)))
        g.glow_spot(img, .74 * S, .7 * S, .16 * S, SPORE, .8)
        d.ellipse((.64 * S, .58 * S, .84 * S, .78 * S), fill=ck.a((232, 220, 176)))
    return _icon(draw)


def photosynthesis_icon():
    def draw(d, img, S):
        d.polygon([(.36 * S, 0), (.64 * S, 0), (.74 * S, .9 * S), (.26 * S, .9 * S)], fill=ck.a((255, 236, 150), 150))
        g.glow_spot(img, .5 * S, .5 * S, .3 * S, GOLD, .6)
        for k, (x, y) in enumerate(((.32, .7), (.66, .55), (.42, .32), (.6, .82))):
            d.ellipse(((x - .07) * S, (y - .035) * S, (x + .07) * S, (y + .035) * S), fill=ck.a(LEAF))
    return _icon(draw)


def verdant_maw_icon():
    def draw(d, img, S):
        d.chord((.12 * S, .42 * S, .88 * S, .98 * S), 0, 180, fill=ck.a(GREEN))
        d.chord((.2 * S, .5 * S, .8 * S, .9 * S), 0, 180, fill=ck.a(RED))
        d.chord((.14 * S, .08 * S, .86 * S, .6 * S), 180, 360, fill=ck.a(GREEN))
        for k in range(7):
            x = .22 + k * .093
            d.polygon([((x - .03) * S, .7 * S), (x * S, .6 * S), ((x + .03) * S, .7 * S)], fill=ck.a((250, 245, 230)))
            d.polygon([((x - .03) * S, .34 * S), (x * S, .44 * S), ((x + .03) * S, .34 * S)], fill=ck.a((250, 245, 230)))
        for x, y in ((.36, .22), (.6, .18), (.5, .3)):
            g.glow_spot(img, x * S, y * S, .05 * S, (220, 255, 90), .9)
    return _icon(draw)


ICONS = (("thorn_bloom", thorn_bloom_icon), ("pollen_cloud", pollen_cloud_icon), ("seed_volley", seed_volley_icon),
         ("photosynthesis", photosynthesis_icon), ("verdant_maw", verdant_maw_icon))


def _cover(d, img, W, H):
    # A pressed flower on the cover, its stem and two leaves, a vine running round the edge
    cx, cy = W / 2, H / 2
    d.line((cx, cy + 26, cx, cy), fill=GREEN + (255,), width=2)
    for s in (-1, 1):
        d.ellipse((cx + s * 4 - 7, cy + 12, cx + s * 4 + 7, cy + 18), fill=LEAF + (255,))
    for k in range(6):
        a = k * math.tau / 6
        x, y = cx + math.cos(a) * 8, cy - 4 + math.sin(a) * 8
        d.ellipse((x - 6, y - 6, x + 6, y + 6), fill=PINK + (255,))
    d.ellipse((cx - 4, cy - 8, cx + 4, cy), fill=GOLD + (255,))
    for x in range(8, W - 8, 10):
        d.arc((x, 4, x + 10, 12), 0, 180, fill=GREEN + (255,))
        d.arc((x, H - 12, x + 10, H - 4), 180, 360, fill=GREEN + (255,))


def main():
    g.save(leaf_texture(), "textures/entity/plant/leaf.png")
    g.save(petal_texture(), "textures/entity/plant/petal.png")
    g.save(stem_texture(), "textures/entity/plant/stem.png")
    g.save(flesh_texture(), "textures/entity/plant/flesh.png")
    for name, fn in ICONS:
        g.save(fn(), f"textures/gui/spell_icons/{name}.png")
    hide = [(14, 30, 16), (30, 64, 32), (56, 104, 52), (96, 150, 82)]
    tex = ck.codex_texture(hide, BRONZE, BRONZE_DARK, (60, 220, 120), _cover, 6401)
    ck.write_book("herbarium_codex", ck.codex_icon((14, 30, 16), (36, 80, 40), (70, 130, 64), BRONZE, BRONZE_DARK, (60, 220, 120)), tex)
    ck.write_ring("plant", ck.ring((20, 30, 14), BRONZE, (240, 200, 140), BRONZE_DARK, (60, 220, 120), (180, 255, 200)))


if __name__ == "__main__":
    main()
