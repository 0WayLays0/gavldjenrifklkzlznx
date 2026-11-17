#!/usr/bin/env python3
"""Snow Magic art: packed snow and clear ice for its spells, a snowflake, the spell icons, the Codex of the First Frost
(frost-blue leather, silver fittings, a great snowflake and an ice-blue gem on its covers) and its affinity ring.

Called by gen_assets.py (run that one). Original art, drawn here.
"""
import math

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

import codex_kit as ck
import gen_assets as g
from water_assets import _tnoise

WHITE = (244, 250, 255)
FROST = (207, 239, 255)
ICE = (140, 205, 255)
DEEP = (60, 120, 200)
SILVER = (226, 236, 248)
SILVER_DARK = (110, 130, 160)
NIGHT = (8, 14, 26)
BG = (26, 44, 72)


def snow_texture():
    """64 x 64: packed snow, white with soft blue hollows and a few glints (it wraps a snowball)."""
    n = 64
    t = _tnoise(n, n, (8, 8), 4101) * .6 + _tnoise(n, n, (16, 16), 4102) * .4
    img = np.zeros((n, n, 4), np.float32)
    for i, c in enumerate(WHITE):
        img[..., i] = c * (.82 + .18 * t)
    img[..., 2] = np.minimum(255, img[..., 2] + 10 * (1 - t))
    img[..., 3] = 255
    out = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGBA")
    rng = np.random.default_rng(4103)
    for _ in range(40):
        x, y = rng.integers(0, n, 2)
        out.putpixel((int(x), int(y)), (255, 255, 255, 255))
    return out


def ice_texture():
    """64 x 64: clear ice, pale blue and see-through, white cracks and frost across it."""
    n = 64
    t = _tnoise(n, n, (6, 6), 4201)
    img = np.zeros((n, n, 4), np.float32)
    for i, c in enumerate((200, 232, 255)):
        img[..., i] = c * (.88 + .12 * t)
    img[..., 3] = 150 + 70 * t
    out = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGBA")
    d = ImageDraw.Draw(out)
    rng = np.random.default_rng(4202)
    for _ in range(9):
        x, y = rng.uniform(0, n, 2)
        pts = [(x, y)]
        a = rng.uniform(0, math.tau)
        for _ in range(5):
            a += rng.uniform(-.6, .6)
            x, y = x + math.cos(a) * 6, y + math.sin(a) * 6
            pts.append((x, y))
        d.line(pts, fill=(255, 255, 255, 235), width=1)
    return out


def flake_texture():
    """64 x 64, white (tinted in game): a six-armed snowflake with its branches, in a soft glow."""
    n = 256
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = n / 2
    for k in range(6):
        a = k * math.pi / 3
        x1, y1 = c + math.cos(a) * c * .86, c + math.sin(a) * c * .86
        d.line((c, c, x1, y1), fill=(255, 255, 255, 255), width=9)
        for f, length in ((.38, .22), (.62, .17), (.8, .1)):
            bx, by = c + math.cos(a) * c * .86 * f, c + math.sin(a) * c * .86 * f
            for s in (-1, 1):
                b = a + s * math.pi / 3.2
                d.line((bx, by, bx + math.cos(b) * c * length, by + math.sin(b) * c * length), fill=(255, 255, 255, 255), width=7)
    d.ellipse((c - 16, c - 16, c + 16, c + 16), fill=(255, 255, 255, 255))
    halo = img.filter(ImageFilter.GaussianBlur(10))
    out = Image.alpha_composite(halo, img)
    return out.resize((64, 64), Image.LANCZOS)


def _icon(draw, bg=BG):
    return ck.icon(draw, bg, NIGHT)


def _flake(d, S, x, y, r, c, width=2):
    for k in range(6):
        a = k * math.pi / 3
        d.line((x * S, y * S, (x + math.cos(a) * r) * S, (y + math.sin(a) * r) * S), fill=ck.a(c), width=width)


def avalanche_ball_icon():
    def draw(d, img, S):
        for k in range(4):
            g.glow_spot(img, (.22 - k * .04) * S, (.62 + k * .03) * S, (.12 - k * .02) * S, FROST, .7 - k * .15)
        d.ellipse((.3 * S, .3 * S, .78 * S, .78 * S), fill=ck.a(WHITE))
        d.ellipse((.36 * S, .46 * S, .78 * S, .8 * S), fill=ck.a((206, 224, 244)))
        d.ellipse((.4 * S, .34 * S, .58 * S, .48 * S), fill=ck.a((255, 255, 255)))
        for x, y in ((.48, .62), (.64, .44), (.58, .7)):
            d.ellipse(((x - .05) * S, (y - .04) * S, (x + .05) * S, (y + .04) * S), fill=ck.a((226, 238, 252)))
        d.line((.08 * S, .84 * S, .92 * S, .84 * S), fill=ck.a(FROST), width=2)
    return _icon(draw)


def snow_globe_icon():
    def draw(d, img, S):
        g.glow_spot(img, .5 * S, .45 * S, .3 * S, ICE, .4)
        d.ellipse((.2 * S, .14 * S, .8 * S, .74 * S), outline=ck.a(FROST), width=3)
        d.rectangle((.24 * S, .7 * S, .76 * S, .86 * S), fill=ck.a((196, 150, 70)))
        d.rectangle((.2 * S, .8 * S, .8 * S, .88 * S), fill=ck.a((140, 100, 40)))
        d.chord((.24 * S, .5 * S, .76 * S, .86 * S), 180, 360, fill=ck.a(WHITE))
        for x, y in ((.36, .3), (.6, .26), (.48, .42), (.66, .5), (.34, .52)):
            _flake(d, S, x, y, .04, WHITE, 1)
    return _icon(draw)


def frost_glide_icon():
    def draw(d, img, S):
        d.polygon([(.08 * S, .84 * S), (.3 * S, .92 * S), (.92 * S, .3 * S), (.74 * S, .2 * S)], fill=ck.a((190, 226, 255)))
        for k in range(5):
            x, y = .2 + k * .15, .82 - k * .15
            d.polygon([(x * S, y * S), ((x + .04) * S, (y - .14) * S), ((x + .08) * S, y * S)], fill=ck.a(WHITE))
        for k in range(3):
            d.arc(((.3 + k * .1) * S, (.1 + k * .05) * S, (.9 + k * .03) * S, (.6 + k * .05) * S), 200, 290, fill=ck.a(FROST, 220 - k * 60), width=2)
        g.glow_spot(img, .84 * S, .26 * S, .12 * S, WHITE, .8)
    return _icon(draw)


def hoarfrost_crown_icon():
    def draw(d, img, S):
        g.glow_spot(img, .5 * S, .55 * S, .32 * S, ICE, .5)
        d.ellipse((.18 * S, .62 * S, .82 * S, .82 * S), outline=ck.a(SILVER), width=3)
        for k, (x, h) in enumerate(((.24, .26), (.37, .38), (.5, .5), (.63, .38), (.76, .26))):
            d.polygon([((x - .06) * S, .7 * S), (x * S, (.7 - h) * S), ((x + .06) * S, .7 * S)], fill=ck.a(FROST))
            d.line((x * S, .68 * S, x * S, (.72 - h) * S), fill=ck.a(WHITE), width=1)
    return _icon(draw)


def whiteout_icon():
    def draw(d, img, S):
        g.glow_spot(img, .5 * S, .5 * S, .4 * S, FROST, .6)
        for k in range(4):
            r = .12 + k * .09
            d.arc(((.5 - r) * S, (.5 - r) * S, (.5 + r) * S, (.5 + r) * S), k * 70, k * 70 + 230, fill=ck.a(WHITE, 255 - k * 40), width=3)
        for x, y in ((.2, .2), (.82, .3), (.75, .82), (.22, .78)):
            _flake(d, S, x, y, .06, WHITE)
    return _icon(draw, (36, 56, 88))


ICONS = (("avalanche_ball", avalanche_ball_icon), ("snow_globe", snow_globe_icon), ("frost_glide", frost_glide_icon),
         ("hoarfrost_crown", hoarfrost_crown_icon), ("whiteout", whiteout_icon))


def _cover(d, img, W, H):
    # A great snowflake of silver across the cover, frost in its corners, an ice-blue gem at its heart
    cx, cy = W / 2, H / 2
    for k in range(6):
        a = k * math.pi / 3
        x1, y1 = cx + math.cos(a) * 26, cy + math.sin(a) * 26
        d.line((cx, cy, x1, y1), fill=SILVER + (255,), width=2)
        for f in (.45, .72):
            bx, by = cx + math.cos(a) * 26 * f, cy + math.sin(a) * 26 * f
            for s in (-1, 1):
                b = a + s * math.pi / 3.2
                d.line((bx, by, bx + math.cos(b) * 6, by + math.sin(b) * 6), fill=FROST + (255,), width=1)
    d.ellipse((cx - 5, cy - 5, cx + 5, cy + 5), fill=ICE + (255,))
    d.ellipse((cx - 2, cy - 3, cx + 1, cy), fill=(255, 255, 255, 255))
    for x, y in ((8, 8), (W - 9, 8), (8, H - 9), (W - 9, H - 9)):
        for k in range(3):
            a = k * math.pi / 3
            d.line((x - math.cos(a) * 4, y - math.sin(a) * 4, x + math.cos(a) * 4, y + math.sin(a) * 4), fill=FROST + (255,), width=1)


def main():
    g.save(snow_texture(), "textures/entity/snow/snow.png")
    g.save(ice_texture(), "textures/entity/snow/ice.png")
    g.save(flake_texture(), "textures/entity/snow/flake.png")
    for name, fn in ICONS:
        g.save(fn(), f"textures/gui/spell_icons/{name}.png")
    hide = [(16, 28, 48), (34, 62, 100), (70, 112, 160), (130, 176, 220)]
    tex = ck.codex_texture(hide, SILVER, SILVER_DARK, ICE, _cover, 4301)
    ck.write_book("first_frost_codex", ck.codex_icon((16, 28, 48), (40, 74, 120), (96, 146, 200), SILVER, SILVER_DARK, ICE), tex)
    ck.write_ring("snow", ck.ring((20, 30, 50), SILVER, (255, 255, 255), SILVER_DARK, ICE, WHITE))


if __name__ == "__main__":
    main()
