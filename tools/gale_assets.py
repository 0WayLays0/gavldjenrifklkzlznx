#!/usr/bin/env python3
"""The Gale's art: air so dense it bends the light (rippling streaks, light against dark, faintly violet), the edge of a
blade of air, the spell icons, the Codex of the Violet Gale (dusk-violet leather, pale silver fittings, a lion's heart
star among blades of wind on its covers, an amethyst at its heart) and its affinity ring.

Called by gen_assets.py (run that one, or this file alone). Original art, drawn here.
"""
import math

import numpy as np
from PIL import Image

import codex_kit as ck
import gen_assets as g

WHITE = (250, 246, 255)
LILAC = (214, 196, 255)
VIOLET = (170, 130, 255)
DEEP = (92, 60, 170)
SILVER = (232, 226, 248)
SILVER_DARK = (120, 106, 160)
NIGHT = (14, 10, 26)
BG = (40, 30, 70)


def warp_texture():
    """64 x 32, tiling along U: bent air: thin rippling lines, each light with a dark twin just under it (how a shimmer of
    heat looks), faint, faintly violet; clear between them."""
    w, h = 64, 32
    a = np.zeros((h, w, 4), np.float32)
    rng = np.random.default_rng(5101)
    for k in range(7):
        y0 = rng.uniform(2, h - 3)
        amp, freq, ph = rng.uniform(.6, 2.2), rng.choice([1, 2, 3]), rng.uniform(0, 2 * math.pi)
        strength = rng.uniform(.5, 1)
        for x in range(w):
            y = y0 + amp * math.sin(2 * math.pi * freq * x / w + ph)
            fade = .55 + .45 * math.sin(2 * math.pi * (x / w * 2 + k * .3)) ** 2
            for dy in range(-2, 4):
                yy = int(math.floor(y)) + dy
                if not 0 <= yy < h:
                    continue
                d = yy - y
                light = math.exp(-((d + .3) ** 2) / .5)
                dark = math.exp(-((d - 1.4) ** 2) / .6)
                alpha = (light * 150 + dark * 70) * strength * fade
                if alpha <= a[yy, x, 3]:
                    continue
                if light >= dark:
                    a[yy, x, :3] = (250, 244, 255)
                else:
                    a[yy, x, :3] = (70, 48, 120)
                a[yy, x, 3] = alpha
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def blade_texture():
    """64 x 16: the edge of a blade of air, U along it (tiling), V across: clear before it, a thin white edge, then the
    bent air trailing behind it, lilac fading to nothing, streaked along its length."""
    w, h = 64, 16
    a = np.zeros((h, w, 4), np.float32)
    rng = np.random.default_rng(5102)
    streak = rng.uniform(.6, 1, size=(h, w))
    for x in range(w):
        for y in range(h):
            v = y / (h - 1)
            if v < .18:
                continue
            edge = math.exp(-((v - .26) ** 2) / .003)
            trail = max(0.0, 1 - (v - .26) / .74) ** 1.6 if v > .26 else 0
            streaks = .5 + .5 * math.sin(x * .9 + y * 2.1 + 3 * math.sin(x * .21))
            alpha = 255 * min(1, edge + trail * (.35 + .4 * streaks) * streak[y, x])
            c = np.array(WHITE) * edge + np.array(LILAC) * (1 - edge)
            a[y, x, :3] = c
            a[y, x, 3] = alpha
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


# ---------------------------------------------------------------- icons

def _icon(draw):
    return ck.icon(draw, BG, NIGHT)


def _arc(d, S, cx, cy, r, a0, a1, c, width):
    pts = [((cx + math.cos(math.radians(a)) * r) * S, (cy + math.sin(math.radians(a)) * r) * S) for a in np.linspace(a0, a1, 24)]
    d.line(pts, fill=ck.a(c), width=width)


def air_slash_icon():
    def draw(d, img, S):
        g.glow_spot(img, .5 * S, .5 * S, .34 * S, VIOLET, .6)
        _arc(d, S, .62, .7, .46, 195, 300, LILAC, max(1, S // 10))
        _arc(d, S, .62, .7, .44, 205, 290, WHITE, max(1, S // 22))
        for k in range(3):
            y = .62 + k * .1
            d.line((.12 * S, y * S, (.34 - k * .05) * S, (y - .02) * S), fill=ck.a(LILAC), width=1)
    return _icon(draw)


def air_sphere_icon():
    def draw(d, img, S):
        g.glow_spot(img, .5 * S, .5 * S, .36 * S, VIOLET, .8)
        for k, (r, c) in enumerate(((.3, LILAC), (.22, WHITE), (.14, LILAC))):
            _arc(d, S, .5, .5, r, k * 40, k * 40 + 300, c, max(1, S // 18))
        d.ellipse((.44 * S, .44 * S, .56 * S, .56 * S), fill=ck.a(WHITE))
    return _icon(draw)


def sweeping_gale_icon():
    def draw(d, img, S):
        g.glow_spot(img, .5 * S, .78 * S, .3 * S, VIOLET, .6)
        for k in range(3):
            y = .62 + k * .09
            d.line((.08 * S, y * S, .92 * S, (y - .06) * S), fill=ck.a(WHITE if k == 1 else LILAC), width=max(1, S // 16))
        d.polygon([(.18 * S, .2 * S), (.34 * S, .5 * S), (.28 * S, .52 * S)], fill=ck.a((210, 200, 230)))
        d.line((.06 * S, .9 * S, .94 * S, .9 * S), fill=ck.a(DEEP), width=2)
    return _icon(draw)


def cutting_dash_icon():
    def draw(d, img, S):
        for k in range(5):
            y = .28 + k * .11
            d.line((.06 * S, y * S, (.5 + k * .04) * S, y * S), fill=ck.a(LILAC if k % 2 else VIOLET), width=1)
        g.glow_spot(img, .72 * S, .5 * S, .2 * S, VIOLET, .9)
        d.line((.56 * S, .18 * S, .9 * S, .82 * S), fill=ck.a(WHITE), width=max(1, S // 14))
        d.line((.86 * S, .2 * S, .6 * S, .8 * S), fill=ck.a(LILAC), width=max(1, S // 20))
    return _icon(draw)


def crushing_gravity_icon():
    def draw(d, img, S):
        g.glow_spot(img, .5 * S, .3 * S, .34 * S, DEEP, .8)
        for x in (.25, .5, .75):
            d.line((x * S, .08 * S, x * S, .6 * S), fill=ck.a(LILAC), width=max(1, S // 20))
            d.polygon([((x - .08) * S, .56 * S), ((x + .08) * S, .56 * S), (x * S, .7 * S)], fill=ck.a(WHITE))
        d.rectangle((.1 * S, .82 * S, .9 * S, .9 * S), fill=ck.a(VIOLET))
        d.line((.3 * S, .82 * S, .4 * S, .9 * S), fill=ck.a(NIGHT), width=1)
        d.line((.62 * S, .82 * S, .56 * S, .9 * S), fill=ck.a(NIGHT), width=1)
    return _icon(draw)


def cor_leonis_icon():
    def draw(d, img, S):
        g.glow_spot(img, .5 * S, .5 * S, .42 * S, VIOLET, .9)
        for k in range(6):
            a0 = k * 60
            _arc(d, S, .5, .5, .36, a0, a0 + 38, WHITE if k % 2 else LILAC, max(1, S // 16))
        # The star at its heart
        for k in range(4):
            a = math.radians(k * 90 + 45)
            d.line((.5 * S, .5 * S, (.5 + math.cos(a) * .16) * S, (.5 + math.sin(a) * .16) * S), fill=ck.a(WHITE), width=max(1, S // 24))
        d.ellipse((.44 * S, .44 * S, .56 * S, .56 * S), fill=ck.a((255, 250, 220)))
    return _icon(draw)


ICONS = (("air_slash", air_slash_icon), ("air_sphere", air_sphere_icon), ("sweeping_gale", sweeping_gale_icon),
         ("cutting_dash", cutting_dash_icon), ("crushing_gravity", crushing_gravity_icon), ("cor_leonis", cor_leonis_icon))


def _cover(d, img, W, H):
    # Blades of wind sweeping round a four-pointed star (the lion's heart), an amethyst at its heart
    cx, cy = W / 2, H / 2
    for k in range(4):
        a0 = k * 90 + 20
        pts = [(cx + math.cos(math.radians(a)) * (16 + (a - a0) * .12), cy + math.sin(math.radians(a)) * (16 + (a - a0) * .12))
               for a in np.linspace(a0, a0 + 70, 16)]
        d.line(pts, fill=SILVER + (255,), width=2)
    for k in range(4):
        a = math.radians(k * 90)
        d.polygon([(cx + math.cos(a) * 12, cy + math.sin(a) * 12), (cx + math.cos(a + .5) * 3, cy + math.sin(a + .5) * 3),
                   (cx + math.cos(a - .5) * 3, cy + math.sin(a - .5) * 3)], fill=LILAC + (255,))
    d.ellipse((cx - 4, cy - 4, cx + 4, cy + 4), fill=VIOLET + (255,))
    d.ellipse((cx - 2, cy - 3, cx + 1, cy), fill=(255, 255, 255, 255))
    for x, y in ((8, 8), (W - 9, 8), (8, H - 9), (W - 9, H - 9)):
        d.line((x - 3, y + 2, x + 3, y - 2), fill=LILAC + (255,), width=1)


def main():
    g.save(warp_texture(), "textures/entity/gale/warp.png")
    g.save(blade_texture(), "textures/entity/gale/blade.png")
    for name, fn in ICONS:
        g.save(fn(), f"textures/gui/spell_icons/{name}.png")
    hide = [(26, 16, 44), (46, 30, 78), (78, 56, 126), (122, 96, 176)]
    tex = ck.codex_texture(hide, SILVER, SILVER_DARK, VIOLET, _cover, 5103)
    ck.write_book("violet_gale_codex", ck.codex_icon((26, 16, 44), (58, 40, 100), (120, 92, 180), SILVER, SILVER_DARK, VIOLET), tex)
    ck.write_ring("gale", ck.ring((30, 20, 50), SILVER, (255, 255, 255), SILVER_DARK, VIOLET, LILAC))


if __name__ == "__main__":
    main()
