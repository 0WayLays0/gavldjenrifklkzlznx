#!/usr/bin/env python3
"""Trap Magic art: the ground textures of its traps, the spell icons, the Trapper's Codex (dark hide stitched with gut,
brass clasps, a jaw trap of iron and a poison-green eye on its covers) and its affinity ring.

Called by gen_assets.py (run that one). Original art, drawn here.
"""
import math

import numpy as np
from PIL import Image, ImageDraw

import codex_kit as ck
import gen_assets as g

TOXIC = (150, 255, 90)
VENOM = (60, 190, 60)
BLOOD = (200, 30, 40)
RUST = (190, 100, 40)
VIOLET = (160, 90, 255)
SOOT = (60, 60, 70)
BRASS = (214, 170, 80)
BRASS_DARK = (120, 86, 36)
IRON = (150, 150, 160)
NIGHT = (14, 10, 18)
BG = (46, 36, 44)


def sigil_texture():
    """128 x 128, white (tinted in game): the circle a trap is drawn on the ground with, its teeth and runes."""
    n = 512
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = n / 2
    for r, w in ((.96, 10), (.84, 5), (.5, 6)):
        d.ellipse((c - r * c, c - r * c, c + r * c, c + r * c), outline=(255, 255, 255, 255), width=w)
    for k in range(16):
        a = k * math.tau / 16
        r0, r1 = .84 * c, .62 * c
        d.polygon([(c + math.cos(a - .1) * r0, c + math.sin(a - .1) * r0), (c + math.cos(a + .1) * r0, c + math.sin(a + .1) * r0),
                   (c + math.cos(a) * r1, c + math.sin(a) * r1)], fill=(255, 255, 255, 230))
    for k in range(6):
        a = k * math.tau / 6
        x, y = c + math.cos(a) * .28 * c, c + math.sin(a) * .28 * c
        d.line((x - 14, y - 14, x + 14, y + 14), fill=(255, 255, 255, 200), width=6)
        d.line((x + 14, y - 14, x - 14, y + 14), fill=(255, 255, 255, 200), width=6)
    d.ellipse((c - 18, c - 18, c + 18, c + 18), fill=(255, 255, 255, 255))
    return img.resize((128, 128), Image.LANCZOS)


def web_texture():
    """64 x 16: a strand of web, white, soft at its edges, along U."""
    ys = (np.arange(16) + .5)[:, None] * np.ones((1, 64))
    v = np.exp(-((ys - 8) / 2.2) ** 2)
    a = np.clip(v * 255, 0, 255)
    return Image.fromarray(np.dstack([np.full((16, 64), 255), np.full((16, 64), 255), np.full((16, 64), 255), a]).astype(np.uint8), "RGBA")


def _icon(draw, bg=BG):
    return ck.icon(draw, bg, NIGHT)


def _jaws(d, S, cx, cy, r, c, open_=.5):
    for side in (-1, 1):
        pts = []
        for i in range(9):
            t = i / 8
            a = math.pi * (1 + t) if side < 0 else math.pi * t
            pts.append(((cx + math.cos(a) * r) * S, (cy + side * open_ * .3 * r + math.sin(a) * r * .5) * S))
        d.line(pts, fill=ck.a(c), width=3)
        for i in range(1, 8):
            t = i / 8
            a = math.pi * (1 + t) if side < 0 else math.pi * t
            x, y = cx + math.cos(a) * r, cy + side * open_ * .3 * r + math.sin(a) * r * .5
            d.polygon([(x * S - 2, y * S), (x * S + 2, y * S), (x * S, (y - side * .07) * S)], fill=ck.a(c))


def _circle(d, S, c, r=.36):
    d.ellipse(((.5 - r) * S, (.62 - r * .45) * S, (.5 + r) * S, (.62 + r * .45) * S), outline=ck.a(c), width=3)


def _drop(d, S, x, y, r, c):
    d.polygon([(x * S, (y - r * 1.6) * S), ((x + r) * S, y * S), (x * S, (y + r) * S), ((x - r) * S, y * S)], fill=ck.a(c))


def bramble_snare_icon():
    def draw(d, img, S):
        _circle(d, S, VENOM)
        for k in range(5):
            a = k * math.tau / 5
            d.arc(((.3 + .1 * math.cos(a)) * S, (.3 + .08 * math.sin(a)) * S, (.7 + .1 * math.cos(a)) * S, (.7 + .08 * math.sin(a)) * S),
                  k * 70, k * 70 + 160, fill=ck.a((90, 140, 50)), width=3)
        _drop(d, S, .5, .3, .07, TOXIC)
    return _icon(draw)


def hex_glyph_icon():
    def draw(d, img, S):
        _circle(d, S, VIOLET)
        d.polygon([(.5 * S, .2 * S), (.7 * S, .55 * S), (.3 * S, .55 * S)], outline=ck.a(VIOLET), width=3)
        g.glow_spot(img, .5 * S, .44 * S, .16 * S, VIOLET, .7)
        d.line((.42 * S, .7 * S, .58 * S, .84 * S), fill=ck.a(IRON), width=3)
    return _icon(draw)


def bloodletter_icon():
    def draw(d, img, S):
        _circle(d, S, BLOOD)
        for k in range(5):
            x = .3 + k * .1
            d.polygon([((x - .03) * S, .66 * S), ((x + .03) * S, .66 * S), (x * S, .32 * S)], fill=ck.a(IRON))
        _drop(d, S, .5, .2, .06, BLOOD)
    return _icon(draw)


def soot_mine_icon():
    def draw(d, img, S):
        for k in range(6):
            a = k * math.tau / 6
            g.glow_spot(img, (.5 + math.cos(a) * .2) * S, (.45 + math.sin(a) * .15) * S, .16 * S, (90, 90, 100), .9)
        d.ellipse((.38 * S, .55 * S, .62 * S, .75 * S), fill=ck.a(SOOT))
        d.line((.5 * S, .55 * S, .56 * S, .42 * S), fill=ck.a(BRASS), width=2)
    return _icon(draw)


def rust_jaw_icon():
    def draw(d, img, S):
        _jaws(d, S, .5, .55, .3, RUST)
        g.glow_spot(img, .5 * S, .55 * S, .14 * S, RUST, .5)
    return _icon(draw)


def echo_trap_icon():
    def draw(d, img, S):
        for k, r in enumerate((.18, .28, .38)):
            d.ellipse(((.5 - r) * S, (.62 - r * .45) * S, (.5 + r) * S, (.62 + r * .45) * S), outline=ck.a(VIOLET, 255 - k * 70), width=2)
        for k in range(3):
            d.polygon([((.38 + k * .12) * S, .4 * S), ((.44 + k * .12) * S, .26 * S), ((.5 + k * .12) * S, .4 * S)], fill=ck.a(TOXIC))
    return _icon(draw)


def plague_carrier_icon():
    def draw(d, img, S):
        g.glow_spot(img, .5 * S, .5 * S, .3 * S, TOXIC, .7)
        d.ellipse((.4 * S, .4 * S, .6 * S, .6 * S), fill=ck.a(VENOM))
        for k in range(6):
            a = k * math.tau / 6
            x, y = .5 + math.cos(a) * .32, .5 + math.sin(a) * .32
            d.ellipse(((x - .05) * S, (y - .05) * S, (x + .05) * S, (y + .05) * S), fill=ck.a(TOXIC))
            d.line((.5 * S, .5 * S, x * S, y * S), fill=ck.a(VENOM), width=2)
    return _icon(draw)


def leech_trap_icon():
    def draw(d, img, S):
        _circle(d, S, BLOOD)
        pts = [((.3 + .4 * t) * S, (.5 + .08 * math.sin(t * 12)) * S) for t in [i / 20 for i in range(21)]]
        d.line(pts, fill=ck.a((70, 30, 40)), width=7)
        d.line(pts, fill=ck.a(BLOOD), width=3)
        g.glow_spot(img, .76 * S, .5 * S, .1 * S, ROSE if False else (255, 120, 140), .8)
    return _icon(draw)


def minefield_icon():
    def draw(d, img, S):
        cols = (TOXIC, VIOLET, BLOOD, RUST, (200, 200, 220), VENOM, BRASS)
        for k in range(7):
            a = k * math.tau / 7
            x, y = .5 + math.cos(a) * .3, .55 + math.sin(a) * .22
            d.ellipse(((x - .07) * S, (y - .04) * S, (x + .07) * S, (y + .04) * S), outline=ck.a(cols[k]), width=2)
            g.glow_spot(img, x * S, y * S, .06 * S, cols[k], .8)
    return _icon(draw)


def cursed_web_icon():
    def draw(d, img, S):
        for k in range(8):
            a = k * math.tau / 8
            d.line((.5 * S, .5 * S, (.5 + math.cos(a) * .42) * S, (.5 + math.sin(a) * .42) * S), fill=ck.a((220, 220, 230)), width=2)
        for r in (.14, .26, .38):
            pts = [((.5 + math.cos(k * math.tau / 8) * r) * S, (.5 + math.sin(k * math.tau / 8) * r) * S) for k in range(9)]
            d.line(pts, fill=ck.a(VIOLET), width=2)
        g.glow_spot(img, .5 * S, .5 * S, .14 * S, TOXIC, .9)
    return _icon(draw, (40, 20, 50))


ICONS = (("bramble_snare", bramble_snare_icon), ("hex_glyph", hex_glyph_icon), ("bloodletter", bloodletter_icon), ("soot_mine", soot_mine_icon),
         ("rust_jaw", rust_jaw_icon), ("echo_trap", echo_trap_icon), ("plague_carrier", plague_carrier_icon), ("leech_trap", leech_trap_icon),
         ("minefield", minefield_icon), ("cursed_web", cursed_web_icon))


def _cover(d, img, W, H):
    # Stitches of gut along the cover, a jaw trap of iron across the middle, a poison-green eye in it
    for x in range(6, W - 6, 6):
        d.line((x, 6, x + 3, 9), fill=(200, 180, 140, 255))
        d.line((x, H - 9, x + 3, H - 6), fill=(200, 180, 140, 255))
    cx, cy = W / 2, H / 2
    for side in (-1, 1):
        pts = [(cx + math.cos(math.pi * t) * 26 * side * -1, cy + side * (3 + math.sin(math.pi * t) * 14)) for t in [i / 16 for i in range(17)]]
        d.line(pts, fill=IRON + (255,), width=2)
        for i in range(1, 16, 2):
            t = i / 16
            x, y = cx - math.cos(math.pi * t) * 26 * side, cy + side * (3 + math.sin(math.pi * t) * 14)
            d.polygon([(x - 1.5, y), (x + 1.5, y), (x, y - side * 4)], fill=(200, 200, 210, 255))
    d.ellipse((cx - 7, cy - 4, cx + 7, cy + 4), fill=VENOM + (255,))
    d.ellipse((cx - 2, cy - 4, cx + 2, cy + 4), fill=NIGHT + (255,))
    d.ellipse((cx - 30, cy - 2, cx - 26, cy + 2), fill=BRASS + (255,))
    d.ellipse((cx + 26, cy - 2, cx + 30, cy + 2), fill=BRASS + (255,))


def main():
    g.save(sigil_texture(), "textures/entity/trap/sigil.png")
    g.save(web_texture(), "textures/entity/trap/web.png")
    for name, fn in ICONS:
        g.save(fn(), f"textures/gui/spell_icons/{name}.png")
    hide = [(22, 16, 18), (46, 34, 34), (70, 52, 48), (96, 74, 64)]
    tex = ck.codex_texture(hide, BRASS, BRASS_DARK, VENOM, _cover, 3201)
    ck.write_book("trappers_codex", ck.codex_icon((22, 16, 18), (60, 44, 42), (84, 64, 58), BRASS, BRASS_DARK, TOXIC), tex)
    ck.write_ring("trap", ck.ring((30, 20, 20), BRASS, (240, 210, 140), BRASS_DARK, VENOM, TOXIC))


if __name__ == "__main__":
    main()
