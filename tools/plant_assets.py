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
from boss_materials import fbm, image, mix, ramp, smooth, worley
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


def _norm(a):
    return (a - a.min()) / max(1e-6, a.max() - a.min())


def leaf_texture():
    """128 x 128: a leaf, pale (tinted in game): lighter along its midrib and veins, darker toward its serrated edge,
    a waxy sheen down one side, pointed at the top, its stalk at the bottom."""
    n = 512
    ys, xs = np.mgrid[0:n, 0:n].astype(np.float32) / n
    t = 1 - (ys - .02) / .94
    width = np.clip(np.sin(np.pi * np.clip(t, 0, 1) ** 1.3), 0, 1) ** .8 * .3
    serr = 1 + .02 * np.abs(np.sin(t * 70)) * (t < .95)
    dx = np.abs(xs - .5) / np.maximum(width * serr, 1e-3)
    inside = (dx < 1) & (t > 0) & (t < 1)
    # Veins: the midrib, and side veins sweeping up from it
    mid = np.exp(-(np.abs(xs - .5) / .008) ** 2)
    side = np.abs(xs - .5)
    vein_phase = (t - side * 1.3) * 9
    veins = np.exp(-((vein_phase - np.round(vein_phase)) / .05) ** 2) * (side > .01) * (dx < .92)
    shade = .78 + .22 * (1 - dx ** 2)
    sheen = np.exp(-((xs - .38) / .06) ** 2) * (dx < .9) * .18
    v = np.clip(shade + mid * .28 + veins * .07 + sheen, 0, 1.2)
    rgba = np.zeros((n, n, 4), np.float32)
    rgba[..., 0] = 214 * v
    rgba[..., 1] = 250 * v
    rgba[..., 2] = 196 * v
    rgba[..., 3] = inside * 255
    img = Image.fromarray(np.clip(rgba, 0, 255).astype(np.uint8), "RGBA")
    d = ImageDraw.Draw(img)
    d.line((n / 2, n * .96, n / 2, n * 1.0), fill=(190, 230, 170, 255), width=10)
    return img.filter(ImageFilter.GaussianBlur(.8)).resize((128, 128), Image.LANCZOS)


def petal_texture():
    """128 x 128: a petal, white blushing toward its base (tinted in game), fine veins fanning out, its tip rounded with a
    little notch, its edge a touch see-through."""
    n = 512
    ys, xs = np.mgrid[0:n, 0:n].astype(np.float32) / n
    t = 1 - ys
    w = np.clip(np.sin(np.pi * np.clip(t, 0, 1) ** .8), 0, 1) ** .5 * .44 + .005
    notch = 1 - .08 * np.exp(-((xs - .5) / .03) ** 2) * (t > .9)
    dx = np.abs(xs - .5) / w
    inside = (dx < 1) & (t > .02) & (t < notch)
    fan = np.arctan2(xs - .5, t + .1)
    veins = np.exp(-((np.sin(fan * 22)) / .25) ** 2) * .035 * (t < .85)
    v = .86 + .14 * t - veins
    rgba = np.zeros((n, n, 4), np.float32)
    rgba[..., 0] = 255 * v
    rgba[..., 1] = 255 * (v - .12 * (1 - t))
    rgba[..., 2] = 255 * (v - .02)
    rgba[..., 3] = inside * (255 - 70 * smooth(.85, 1, dx))
    img = Image.fromarray(np.clip(rgba, 0, 255).astype(np.uint8), "RGBA")
    return img.filter(ImageFilter.GaussianBlur(1)).resize((128, 128), Image.LANCZOS)


def stem_texture():
    """64 x 64, tiling: the green fibre of a stem, streaked along its length, lighter ridges and darker grooves."""
    n = 64
    fib = _norm(fbm(n, n, 2, 5, 6101, stretch=.12))
    t = .6 * fib + .4 * _norm(fbm(n, n, 6, 3, 6102))
    rgb = ramp(t, [(0, (30, 74, 26)), (.5, (70, 140, 52)), (1, (140, 200, 96))])
    return image(rgb)


def flesh_texture():
    """128 x 128, tiling: the inside of the carnivorous flower's mouth: wet red flesh in folds, a net of darker veins,
    glistening where the light catches it."""
    n = 128
    fold = _norm(fbm(n, n, 4, 5, 6201))
    f1, f2 = worley(n, n, 16, 6202)
    vein = smooth(.06, 0, f2 - f1) * smooth(.3, .6, _norm(fbm(n, n, 3, 3, 6204)))
    rgb = ramp(fold, [(0, (90, 8, 22)), (.5, (176, 34, 52)), (1, (236, 92, 104))])
    rgb = mix(rgb, (96, 10, 26), vein * .45)
    wet = smooth(.75, .9, _norm(fbm(n, n, 8, 3, 6203))) * fold
    rgb = mix(rgb, (255, 220, 224), wet * .5)
    return image(rgb)


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
