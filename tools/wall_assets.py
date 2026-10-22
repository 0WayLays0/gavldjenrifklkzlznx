#!/usr/bin/env python3
"""Wall Magic art: the spell icons, the Rampart Codex (icon and 3D book texture) and its affinity ring.

Called by gen_assets.py (run that one). The school is built of three blocks only (stone bricks, mossy stone bricks,
cracked stone bricks); the spells draw those blocks in game, and the art here keeps to their greys and moss.
"""
import math

from PIL import Image, ImageDraw

import gen_assets as g
from explosion_assets import fbm

STONE = (122, 121, 122)
STONE_LIGHT = (160, 159, 160)
STONE_DARK = (82, 81, 82)
MORTAR = (58, 57, 58)
MOSS = (92, 120, 52)
MOSS_LIGHT = (128, 156, 72)
CRACK = (40, 40, 42)
NIGHT = (14, 14, 16)
OUTLINE = (20, 20, 22)


def _icon(draw, cx=.5, cy=.45, inner=(70, 70, 74)):
    return g.painted(64, draw, (cx, cy, inner, NIGHT), 16)


def bricks(d, S, x0, y0, x1, y1, rows, seed=0, moss=True, cracks=True):
    """A patch of brick wall: courses of bricks with mortar, some mossy, some cracked."""
    d.rectangle((x0 * S, y0 * S, x1 * S, y1 * S), fill=MORTAR + (255,))
    h = (y1 - y0) / rows
    for r in range(rows):
        bw = h * 2
        off = (r % 2) * bw * .5
        x = x0 - off
        k = 0
        while x < x1:
            a, b = max(x0, x) + .008, min(x1, x + bw) - .008
            if b > a:
                t = ((seed + r * 7 + k * 13) * 2654435761 % 1000) / 1000
                color = MOSS if moss and t < .2 else STONE_DARK if t < .35 else STONE
                y_top, y_bot = y0 + r * h + .008, y0 + (r + 1) * h - .008
                d.rectangle((a * S, y_top * S, b * S, y_bot * S), fill=color + (255,))
                d.line((a * S, y_top * S, b * S, y_top * S), fill=(MOSS_LIGHT if color == MOSS else STONE_LIGHT) + (255,), width=max(1, S // 48))
                if cracks and .35 <= t < .5:
                    m = (a + b) / 2
                    d.line((m * S, y_top * S, (m + .02) * S, ((y_top + y_bot) / 2) * S, (m - .01) * S, y_bot * S), fill=CRACK + (255,), width=max(1, S // 48))
            x += bw
            k += 1


def _ground(d, S, y=.88):
    d.rectangle((0, y * S, S, S), fill=STONE_DARK + (255,))
    d.line((0, y * S, S, y * S), fill=MOSS + (255,), width=max(2, S // 24))


def brick_bolt_icon():
    def draw(d, img, S):
        for k in range(5):
            t = k / 5
            x, y = .12 + t * .4, .8 - t * .4
            r = .015 + t * .02
            d.ellipse(((x - r) * S, (y - r) * S, (x + r) * S, (y + r) * S), fill=g.lerp(STONE_DARK, STONE_LIGHT, t) + (255,))
        bricks(d, S, .52, .2, .84, .42, 2, 3, moss=False)
        d.rectangle((.52 * S, .2 * S, .84 * S, .42 * S), outline=OUTLINE + (255,))
    return _icon(draw, .65, .35)


def rising_wall_icon():
    def draw(d, img, S):
        _ground(d, S)
        bricks(d, S, .1, .3, .9, .88, 5, 1)
        for x in (.1, .42, .74):
            bricks(d, S, x, .2, x + .16, .3, 1, 9)
        for x in (.2, .5, .8):
            d.line((x * S, .96 * S, x * S, .9 * S), fill=STONE_LIGHT + (255,), width=max(1, S // 32))
    return _icon(draw, .5, .5)


def brick_rain_icon():
    def draw(d, img, S):
        _ground(d, S, .9)
        for i, (x, y) in enumerate(((.15, .15), (.45, .05), (.72, .22), (.3, .45), (.6, .52), (.82, .6))):
            bricks(d, S, x, y, x + .14, y + .08, 1, i, cracks=False)
            d.line(((x + .07) * S, (y - .08) * S, (x + .07) * S, (y - .01) * S), fill=STONE_LIGHT + (180,), width=max(1, S // 48))
        for x in (.2, .55, .8):
            d.ellipse(((x - .06) * S, .85 * S, (x + .06) * S, .92 * S), fill=STONE_LIGHT + (255,))
    return _icon(draw, .5, .3)


def wall_burst_icon():
    def draw(d, img, S):
        _ground(d, S)
        bricks(d, S, .08, .3, .3, .88, 4, 5)
        for i, (x, y, s) in enumerate(((.45, .3, .12), (.62, .5, .1), (.7, .22, .09), (.86, .4, .08), (.55, .66, .09), (.86, .7, .07))):
            bricks(d, S, x, y, x + s, y + s * .6, 1, i + 2, cracks=False)
        for a in range(-2, 3):
            d.line((.32 * S, .55 * S, (.95) * S, (.55 + a * .14) * S), fill=STONE_LIGHT + (120,), width=max(1, S // 48))
    return _icon(draw, .4, .5)


def mortar_icon():
    def draw(d, img, S):
        _ground(d, S, .9)
        d.arc((.1 * S, .15 * S, .9 * S, 1.6 * S), 200, 340, fill=STONE_LIGHT + (255,), width=max(2, S // 28))
        bricks(d, S, .62, .14, .82, .3, 2, 7, cracks=False)
        for k in range(7):
            a = k * math.tau / 7
            d.line((.15 * S, .88 * S, (.15 + math.cos(a) * .1) * S, (.88 + math.sin(a) * .06 - .05) * S), fill=STONE_LIGHT + (255,), width=max(1, S // 40))
    return _icon(draw, .5, .4)


def crushing_walls_icon():
    def draw(d, img, S):
        _ground(d, S)
        bricks(d, S, .08, .25, .36, .88, 4, 2)
        bricks(d, S, .64, .25, .92, .88, 4, 6)
        d.polygon([(.4 * S, .55 * S), (.48 * S, .5 * S), (.48 * S, .6 * S)], fill=STONE_LIGHT + (255,))
        d.polygon([(.6 * S, .55 * S), (.52 * S, .5 * S), (.52 * S, .6 * S)], fill=STONE_LIGHT + (255,))
    return _icon(draw, .5, .5)


def brick_cage_icon():
    def draw(d, img, S):
        _ground(d, S)
        d.ellipse((.4 * S, .38 * S, .6 * S, .58 * S), fill=(200, 80, 80, 255))
        d.rectangle((.43 * S, .56 * S, .57 * S, .86 * S), fill=(170, 60, 60, 255))
        for x in (.12, .3, .62, .8):
            bricks(d, S, x, .2, x + .1, .88, 5, int(x * 50), moss=x < .5)
        bricks(d, S, .08, .14, .92, .22, 1, 11, cracks=False)
    return _icon(draw, .5, .5)


def rampart_charge_icon():
    def draw(d, img, S):
        _ground(d, S)
        bricks(d, S, .5, .28, .78, .88, 4, 4)
        for k, y in enumerate((.36, .52, .68, .82)):
            d.line(((.1 + k * .03) * S, y * S, .44 * S, y * S), fill=STONE_LIGHT + (200,), width=max(1, S // 32))
        d.polygon([(.84 * S, .4 * S), (.95 * S, .55 * S), (.84 * S, .7 * S)], fill=STONE_LIGHT + (255,))
    return _icon(draw, .6, .55)


def bastion_icon():
    def draw(d, img, S):
        _ground(d, S)
        bricks(d, S, .1, .45, .9, .88, 3, 8)
        for x in (.1, .3, .5, .7):
            bricks(d, S, x, .35, x + .12, .45, 1, int(x * 30), cracks=False)
        d.ellipse((.42 * S, .52 * S, .58 * S, .68 * S), fill=(230, 220, 160, 255))
        d.arc((.26 * S, .2 * S, .74 * S, .9 * S), 200, 340, fill=(230, 220, 160, 255), width=max(1, S // 32))
    return _icon(draw, .5, .55)


def fallen_citadel_icon():
    def draw(d, img, S):
        _ground(d, S, .9)
        bricks(d, S, .06, .2, .26, .9, 6, 1)
        bricks(d, S, .74, .2, .94, .9, 6, 3)
        for x in (.06, .18, .74, .86):
            bricks(d, S, x, .12, x + .08, .2, 1, int(x * 40), cracks=False)
        # The middle wall falling in, a crack across it
        pts = [(.28, .9), (.72, .9), (.66, .42), (.34, .42)]
        d.polygon([(x * S, y * S) for x, y in pts], fill=STONE + (255,), outline=OUTLINE + (255,))
        for y in (.54, .66, .78):
            d.line((.3 * S, y * S, .7 * S, y * S), fill=MORTAR + (255,), width=max(1, S // 48))
        d.line((.5 * S, .42 * S, .46 * S, .6 * S, .54 * S, .74 * S, .5 * S, .9 * S), fill=CRACK + (255,), width=max(1, S // 32))
        d.polygon([(.34 * S, .42 * S), (.66 * S, .42 * S), (.6 * S, .36 * S), (.4 * S, .36 * S)], fill=MOSS + (255,))
    return _icon(draw, .5, .4)


def rampart_codex_icon():
    img = g.from_grid([
        "................",
        "...oooooooooo...",
        "..obbbbbbbbbmco.",
        "..obmmmmmmmmmco.",
        "..oblllllllbLco.",
        "..obmmmmmmmmLco.",
        "..obllklllllLcco",
        "..obmmmmmmmmLcco",
        "..oblllllmllLcco",
        "..obmmmmmmmmLcco",
        "..obbbbbbbbbmco.",
        "..obddddddddmco.",
        "..odddddddddxco.",
        "...ooooooooooo..",
        "................",
        "................",
    ], {"o": OUTLINE, "b": STONE_DARK, "l": STONE, "m": MORTAR, "L": STONE_LIGHT, "d": STONE_DARK, "k": CRACK,
        "c": g.CREAM, "x": g.CREAM_SHADE})
    for x, y in ((4, 4), (5, 4), (9, 8), (10, 8), (4, 11)):
        img.putpixel((x, y), MOSS + (255,))
    return img


def rampart_codex_model_texture():
    """64x64 texture of the 3D book, laid out like the other books: covers of stone brick (some mossy, some cracked),
    a carved keystone in the middle, a mossy spine, old pages."""
    s = 4
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    def box(u0, v0, u1, v1, fill):
        d.rectangle((u0 * s, v0 * s, u1 * s - 1, v1 * s - 1), fill=fill + (255,))

    W, H = 12 * s, 9 * s
    cover = Image.new("RGBA", (W, H))
    cd = ImageDraw.Draw(cover)
    bricks(cd, 1, 0, 0, W, H, 6, 21)
    for y in range(H):
        for x in range(W):
            r, gg, b, a = cover.getpixel((x, y))
            n = (fbm(x / 4, y / 4, 2, 31) - .5) * 30
            cover.putpixel((x, y), (int(max(0, min(255, r + n))), int(max(0, min(255, gg + n))), int(max(0, min(255, b + n))), 255))
    img.paste(cover, (0, 0))
    cx, cy = W // 2, H // 2
    d.rectangle((cx - 7, cy - 6, cx + 7, cy + 6), fill=STONE_LIGHT + (255,), outline=OUTLINE + (255,))
    d.rectangle((cx - 5, cy - 4, cx + 5, cy + 4), outline=STONE_DARK + (255,))
    d.polygon([(cx - 3, cy + 3), (cx, cy - 3), (cx + 3, cy + 3)], outline=MORTAR + (255,))
    d.rectangle((0, 0, W - 1, H - 1), outline=OUTLINE + (255,))
    box(12, 0, 13, 9, STONE_DARK)
    for y in range(H):
        for x in range(12 * s, 13 * s):
            if fbm(x / 2, y / 3, 2, 5) > .5:
                img.putpixel((x, y), g.lerp(MOSS, MOSS_LIGHT, fbm(x, y, 1, 3)) + (255,))
    for y in range(0, 8 * s):
        for x in range(13 * s, 16 * s):
            img.putpixel((x, y), g.lerp(g.CREAM, (176, 168, 140), fbm(x / 3, y / 3, 2, 23) * .8) + (255,))
    for y in range(1, 8 * s, 2):
        d.line((13 * s, y, 16 * s - 1, y), fill=g.CREAM_SHADE + (255,))
    spine = Image.new("RGBA", (12 * s, 3 * s))
    bricks(ImageDraw.Draw(spine), 1, 0, 0, 12 * s, 3 * s, 2, 41, cracks=False)
    img.paste(spine, (0, 9 * s))
    box(0, 12, 12, 15, g.CREAM)
    for x in range(0, 12 * s, 2):
        d.line((x, 12 * s, x, 15 * s - 1), fill=g.CREAM_SHADE + (255,))
    box(12, 9, 14, 11, STONE_LIGHT)
    d.line((12 * s + 1, 9 * s + 1, 14 * s - 2, 9 * s + 1), fill=(200, 200, 200, 255))
    for y in range(9 * s, 11 * s):
        for x in range(14 * s, 16 * s):
            t = math.hypot(x - 14.8 * s, y - 9.8 * s) / (1.4 * s)
            img.putpixel((x, y), g.lerp(MOSS_LIGHT, MOSS, min(1, t ** .8)) + (255,))
    box(12, 11, 16, 12, STONE_DARK)
    return img


def affinity_ring_wall():
    return g.from_grid([
        "................",
        "......oooo......",
        ".....oRRRRo.....",
        ".....oRCLRo.....",
        "....oooooooo....",
        "...ohllllbbso...",
        "..ohlo....obso..",
        "..olo......oso..",
        "..olo......oso..",
        "..olo......oso..",
        "..obso....osso..",
        "...obbbbbssso...",
        "....oooooooo....",
        "................",
        "................",
        "................",
    ], {"o": OUTLINE, "h": STONE_LIGHT, "l": STONE, "b": STONE_DARK, "s": MORTAR,
        "R": STONE_DARK, "C": MOSS, "L": MOSS_LIGHT})


ICONS = (("brick_bolt", brick_bolt_icon), ("rising_wall", rising_wall_icon), ("brick_rain", brick_rain_icon),
         ("wall_burst", wall_burst_icon), ("mortar", mortar_icon), ("crushing_walls", crushing_walls_icon),
         ("brick_cage", brick_cage_icon), ("rampart_charge", rampart_charge_icon), ("bastion", bastion_icon),
         ("fallen_citadel", fallen_citadel_icon))


def main():
    for name, icon in ICONS:
        g.save(icon(), f"textures/gui/spell_icons/{name}.png")
    g.save(affinity_ring_wall(), "textures/item/affinity_ring_wall.png")
    g.save(rampart_codex_icon(), "textures/item/rampart_codex.png")
    g.save(rampart_codex_model_texture(), "textures/item/rampart_codex_3d.png")
    g.write_json("models/item/affinity_ring_wall.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/affinity_ring_wall"}})
    g.book_models("rampart_codex")


if __name__ == "__main__":
    main()
