#!/usr/bin/env python3
"""Bone Magic art: the bone texture the spells' bones are drawn with, the spell icons, the Ossuary Codex (icon and 3D
book texture) and its affinity ring.

Called by gen_assets.py (run that one). Ivory bone, its marrow glowing crimson. Original art.
"""
import math

import numpy as np
from PIL import Image, ImageDraw

import gen_assets as g

IVORY = (232, 222, 196)
IVORY_LIGHT = (246, 240, 224)
IVORY_SHADE = (196, 182, 150)
IVORY_DARK = (140, 126, 98)
OUTLINE = (34, 20, 18)
CRIMSON = (194, 24, 43)
CRIMSON_LIGHT = (255, 106, 90)
CRIMSON_DARK = (96, 10, 20)
NIGHT = (14, 8, 10)


def _noise(w, h, cells, seed):
    """Smooth value noise in [0, 1], tiling across its width."""
    rnd = np.random.default_rng(seed)
    grid = rnd.random((cells[1] + 1, cells[0]))
    grid = np.hstack([grid, grid[:, :1]])
    ys, xs = np.mgrid[0:h, 0:w]
    gx, gy = xs / w * cells[0], ys / h * cells[1]
    x0, y0 = gx.astype(int), gy.astype(int)
    fx, fy = gx - x0, gy - y0
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    a, b = grid[y0, x0], grid[y0, x0 + 1]
    c, d = grid[np.minimum(y0 + 1, cells[1]), x0], grid[np.minimum(y0 + 1, cells[1]), x0 + 1]
    return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy


def bone_texture():
    """64 x 64, u round the bone, v along it: ivory with grain running along it, pores, and a sheen down one side."""
    w = h = 64
    grain = _noise(w, h, (24, 3), 5) * .6 + _noise(w, h, (48, 6), 6) * .4
    blotch = _noise(w, h, (6, 4), 7)
    u = np.arange(w)[None, :] / w
    sheen = np.exp(-((u - .35) ** 2) / .01) * .5 + np.exp(-((u - .8) ** 2) / .04) * .15
    t = .55 + .25 * (grain - .5) + .2 * (blotch - .5) + sheen
    stops = np.array([IVORY_DARK, IVORY_SHADE, IVORY, IVORY_LIGHT], float)
    tt = np.clip(t, 0, 1) * (len(stops) - 1)
    i = np.minimum(tt.astype(int), len(stops) - 2)
    f = (tt - i)[..., None]
    rgb = stops[i] * (1 - f) + stops[i + 1] * f
    rnd = np.random.default_rng(11)
    for _ in range(40):
        x, y = rnd.integers(0, w), rnd.integers(0, h)
        rgb[y, x] = np.array(IVORY_DARK) * .9
    img = Image.fromarray(np.clip(rgb, 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    return img


# ------------------------------------------------------------------------------------------------------- the icons


def _icon(draw, cx=.5, cy=.5):
    return g.painted(64, draw, (cx, cy, (78, 22, 28), NIGHT), 16)


def _bone(d, S, x0, y0, x1, y1, w):
    """A bone drawn flat: a shaft and two knobs at each end."""
    W = max(1, int(w * S))
    d.line((x0 * S, y0 * S, x1 * S, y1 * S), fill=IVORY + (255,), width=W)
    dx, dy = x1 - x0, y1 - y0
    L = math.hypot(dx, dy) or 1
    nx, ny = -dy / L, dx / L
    for x, y in ((x0, y0), (x1, y1)):
        for s in (-1, 1):
            cx, cy = x + nx * w * .45 * s, y + ny * w * .45 * s
            r = w * .62
            d.ellipse(((cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r) * S), fill=IVORY_LIGHT + (255,), outline=IVORY_SHADE + (255,))
    d.line(((x0 + nx * w * .25) * S, (y0 + ny * w * .25) * S, (x1 + nx * w * .25) * S, (y1 + ny * w * .25) * S), fill=IVORY_LIGHT + (255,),
           width=max(1, W // 4))


def _spike(d, S, x0, y0, x1, y1, w, color=IVORY):
    """A spike from a foot {w} wide at (x0, y0) to a point at (x1, y1)."""
    dx, dy = x1 - x0, y1 - y0
    L = math.hypot(dx, dy) or 1
    nx, ny = -dy / L * w * .5, dx / L * w * .5
    d.polygon([((x0 + nx) * S, (y0 + ny) * S), (x1 * S, y1 * S), ((x0 - nx) * S, (y0 - ny) * S)], fill=color + (255,), outline=IVORY_DARK + (255,))
    d.line(((x0 + nx * .5) * S, (y0 + ny * .5) * S, x1 * S, y1 * S), fill=IVORY_LIGHT + (255,), width=max(1, S // 64))


def _ground(d, S, y=.86):
    d.rectangle((0, y * S, S, S), fill=(40, 22, 22, 255))
    d.line((0, y * S, S, y * S), fill=CRIMSON + (255,), width=max(1, S // 32))


def _skull(d, S, cx, cy, r, eyes=True):
    d.ellipse(((cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r * .9) * S), fill=IVORY + (255,), outline=IVORY_DARK + (255,))
    d.rectangle(((cx - r * .55) * S, (cy + r * .5) * S, (cx + r * .55) * S, (cy + r * 1.15) * S), fill=IVORY + (255,), outline=IVORY_DARK + (255,))
    for k in range(-2, 3):
        x = cx + k * r * .2
        d.line((x * S, (cy + r * .8) * S, x * S, (cy + r * 1.15) * S), fill=IVORY_DARK + (255,), width=max(1, S // 64))
    for s in (-1, 1):
        ex, ey, er = cx + s * r * .38, cy + r * .1, r * .26
        d.ellipse(((ex - er) * S, (ey - er) * S, (ex + er) * S, (ey + er) * S), fill=(30, 10, 10, 255))
        if eyes:
            er2 = er * .5
            d.ellipse(((ex - er2) * S, (ey - er2) * S, (ex + er2) * S, (ey + er2) * S), fill=CRIMSON_LIGHT + (255,))
    d.polygon([(cx * S, (cy + r * .35) * S), ((cx - r * .1) * S, (cy + r * .6) * S), ((cx + r * .1) * S, (cy + r * .6) * S)], fill=(30, 10, 10, 255))


def bone_lance_icon():
    def draw(d, img, S):
        for k in range(4):
            t = k / 4
            d.line(((.1 + t * .3) * S, (.9 - t * .3) * S, (.18 + t * .3) * S, (.82 - t * .3) * S), fill=CRIMSON + (120 + 30 * k,), width=max(1, S // 20))
        _bone(d, S, .25, .75, .62, .38, .07)
        _spike(d, S, .6, .4, .9, .1, .1)
    return _icon(draw, .6, .4)


def bone_spikes_icon():
    def draw(d, img, S):
        _ground(d, S)
        for i, (x, h) in enumerate(((.12, .2), (.3, .32), (.5, .48), (.72, .66))):
            _spike(d, S, x, .88, x + .08, .88 - h, .12 + .03 * i)
            _spike(d, S, x + .07, .88, x + .14, .88 - h * .6, .08)
    return _icon(draw, .5, .7)


def spine_whip_icon():
    def draw(d, img, S):
        pts = [(.15 + .7 * t, .75 - .45 * math.sin(t * math.pi * .9) + .1 * t) for t in [k / 9 for k in range(10)]]
        for i, (x, y) in enumerate(pts):
            r = .06 - .03 * i / 9
            d.ellipse(((x - r) * S, (y - r) * S, (x + r) * S, (y + r) * S), fill=IVORY + (255,), outline=IVORY_DARK + (255,))
            d.line((x * S, (y - r) * S, (x - .02) * S, (y - r * 2) * S), fill=IVORY_LIGHT + (255,), width=max(1, S // 48))
        _spike(d, S, pts[-1][0], pts[-1][1], .95, .55, .06)
        d.arc((.05 * S, .2 * S, .95 * S, 1.2 * S), 200, 330, fill=CRIMSON + (150,), width=max(1, S // 32))
    return _icon(draw, .5, .5)


def bone_volley_icon():
    def draw(d, img, S):
        for k in range(7):
            a = math.radians(-60 + k * 20)
            x0, y0 = .15, .75
            L = .55 + .1 * (k % 2)
            _spike(d, S, x0 + math.cos(a) * .15, y0 + math.sin(a) * .15, x0 + math.cos(a) * L, y0 + math.sin(a) * L - .1, .06)
        d.ellipse((.06 * S, .66 * S, .24 * S, .84 * S), fill=CRIMSON + (255,))
    return _icon(draw, .3, .7)


def bone_ward_icon():
    def draw(d, img, S):
        d.ellipse((.4 * S, .28 * S, .6 * S, .48 * S), fill=(120, 110, 100, 255))
        d.rectangle((.42 * S, .46 * S, .58 * S, .8 * S), fill=(110, 100, 90, 255))
        for k in range(6):
            a = k * math.tau / 6 + .3
            x, y = .5 + math.cos(a) * .32, .55 + math.sin(a) * .3
            d.ellipse(((x - .09) * S, (y - .12) * S, (x + .09) * S, (y + .12) * S), fill=IVORY + (255,), outline=IVORY_DARK + (255,))
            d.line(((x - .07) * S, (y - .04) * S, (x + .07) * S, (y - .07) * S), fill=IVORY_DARK + (255,), width=max(1, S // 40))
        d.ellipse((.12 * S, .2 * S, .88 * S, .9 * S), outline=CRIMSON + (180,), width=max(1, S // 40))
    return _icon(draw, .5, .55)


def rib_cage_icon():
    def draw(d, img, S):
        _ground(d, S, .9)
        d.ellipse((.42 * S, .42 * S, .58 * S, .58 * S), fill=(200, 70, 70, 255))
        d.rectangle((.44 * S, .56 * S, .56 * S, .88 * S), fill=(170, 60, 60, 255))
        for k in range(4):
            y = .3 + k * .05
            for s in (-1, 1):
                box = ((.5 - .38 + k * .02) * S, y * S, (.5 + .38 - k * .02) * S, (y + 1.1) * S)
                d.arc(box, 180 if s < 0 else 270, 270 if s < 0 else 360, fill=IVORY + (255,), width=max(2, S // 22))
        d.line((.5 * S, .3 * S, .5 * S, .46 * S), fill=IVORY_LIGHT + (255,), width=max(2, S // 20))
    return _icon(draw, .5, .55)


def ossuary_icon():
    def draw(d, img, S):
        d.ellipse((.04 * S, .66 * S, .96 * S, .98 * S), fill=CRIMSON_DARK + (255,), outline=CRIMSON + (255,))
        rnd = np.random.default_rng(3)
        for _ in range(9):
            x = .12 + rnd.random() * .76
            y = .74 + rnd.random() * .16
            h = .2 + rnd.random() * .35
            _spike(d, S, x, y, x + (rnd.random() - .5) * .12, y - h, .08)
    return _icon(draw, .5, .7)


def skeletal_grasp_icon():
    def draw(d, img, S):
        _ground(d, S, .88)
        _bone(d, S, .44, .98, .46, .62, .07)
        _bone(d, S, .56, .98, .54, .62, .06)
        d.ellipse((.36 * S, .5 * S, .64 * S, .66 * S), fill=IVORY + (255,), outline=IVORY_DARK + (255,))
        for k, x in enumerate((.36, .45, .55, .64)):
            _bone(d, S, x, .52, x + (x - .5) * .3, .3, .04)
            _spike(d, S, x + (x - .5) * .3, .3, x + (x - .5) * .2 + .03, .16, .045)
        _bone(d, S, .64, .6, .8, .48, .045)
    return _icon(draw, .5, .5)


def bone_wyrm_icon():
    def draw(d, img, S):
        pts = [(.12 + .58 * t, .85 - .2 * math.sin(t * math.pi * 2.2) - .35 * t) for t in [k / 11 for k in range(12)]]
        for i, (x, y) in enumerate(pts):
            r = .03 + .04 * i / 11
            d.ellipse(((x - r) * S, (y - r) * S, (x + r) * S, (y + r) * S), fill=IVORY + (255,), outline=IVORY_DARK + (255,))
        _skull(d, S, .78, .28, .13)
    return _icon(draw, .6, .4)


def gashadokuro_icon():
    def draw(d, img, S):
        _skull(d, S, .5, .42, .32)
        for s in (-1, 1):
            d.ellipse(((.5 + s * .12 - .07) * S, (.46 - .07) * S, (.5 + s * .12 + .07) * S, (.46 + .07) * S), fill=CRIMSON_LIGHT + (120,))
    return _icon(draw, .5, .45)


def colossal_spike_icon():
    def draw(d, img, S):
        _ground(d, S, .9)
        d.ellipse((.2 * S, .84 * S, .8 * S, .96 * S), fill=CRIMSON + (200,))
        _spike(d, S, .5, .92, .56, .04, .34)
        for x, h in ((.2, .25), (.78, .3), (.32, .14), (.68, .16)):
            _spike(d, S, x, .92, x + (x - .5) * .3, .92 - h, .08)
    return _icon(draw, .5, .6)


def elbow_blade_icon():
    def draw(d, img, S):
        # An arm bent at the elbow, a blade of bone splitting out of it
        d.line((.1 * S, .9 * S, .42 * S, .62 * S), fill=(150, 110, 90, 255), width=max(2, S // 9))
        d.line((.42 * S, .62 * S, .4 * S, .3 * S), fill=(150, 110, 90, 255), width=max(2, S // 10))
        _spike(d, S, .44, .6, .92, .1, .13)
        _spike(d, S, .45, .64, .7, .78, .06)
        d.ellipse((.37 * S, .55 * S, .51 * S, .69 * S), fill=CRIMSON + (255,))
    return _icon(draw, .5, .5)


def howling_skull_icon():
    def draw(d, img, S):
        for k in range(5):
            t = k / 5
            r = .2 - t * .12
            x, y = .2 + t * .2, .8 - t * .2
            d.ellipse(((x - r) * S, (y - r) * S, (x + r) * S, (y + r) * S), fill=CRIMSON + (60 + 30 * k,))
        _skull(d, S, .62, .38, .26)
    return _icon(draw, .6, .4)


def bone_nova_icon():
    def draw(d, img, S):
        for k in range(14):
            a = k * math.tau / 14
            _spike(d, S, .5 + math.cos(a) * .1, .5 + math.sin(a) * .1, .5 + math.cos(a) * .46, .5 + math.sin(a) * .46, .09)
        d.ellipse((.36 * S, .36 * S, .64 * S, .64 * S), fill=IVORY + (255,), outline=CRIMSON + (255,), width=max(1, S // 32))
        d.ellipse((.44 * S, .44 * S, .56 * S, .56 * S), fill=CRIMSON + (255,))
    return _icon(draw, .5, .5)


def bone_tempest_icon():
    def draw(d, img, S):
        for ring, (r, n) in enumerate(((.18, 5), (.3, 7), (.42, 9))):
            d.ellipse(((.5 - r) * S, (.5 - r * .55) * S, (.5 + r) * S, (.5 + r * .55) * S), outline=CRIMSON + (150,), width=max(1, S // 48))
            for k in range(n):
                a = k * math.tau / n + ring
                x, y = .5 + math.cos(a) * r, .5 + math.sin(a) * r * .55
                dx, dy = math.cos(a + 1.2 + k) * .06, math.sin(a + 1.2 + k) * .06
                _bone(d, S, x - dx, y - dy, x + dx, y + dy, .025)
    return _icon(draw, .5, .5)


def bone_blade_item():
    """The Bone Blade in hand: a long curved blade of bone, its grip wrapped, a knob of bone for a pommel."""
    return g.from_grid([
        "..............wL",
        ".............wLs",
        "............wLs.",
        "...........wLs..",
        "..........wLs...",
        ".........wLs....",
        "........wLs.....",
        ".......wLs......",
        "..c...wLs.......",
        "...cdwLs........",
        "....dds.........",
        "...rrdc.........",
        "..rr..cc........",
        ".rr.............",
        "kk..............",
        "kk..............",
    ], {"w": IVORY_LIGHT, "L": IVORY, "s": IVORY_SHADE, "d": IVORY_DARK, "c": IVORY_SHADE, "r": CRIMSON_DARK, "k": IVORY})


ICONS = (("bone_lance", bone_lance_icon), ("bone_spikes", bone_spikes_icon), ("spine_whip", spine_whip_icon),
         ("bone_volley", bone_volley_icon), ("bone_ward", bone_ward_icon), ("rib_cage", rib_cage_icon), ("ossuary", ossuary_icon),
         ("skeletal_grasp", skeletal_grasp_icon), ("bone_wyrm", bone_wyrm_icon), ("gashadokuro", gashadokuro_icon),
         ("colossal_spike", colossal_spike_icon), ("elbow_blade", elbow_blade_icon), ("howling_skull", howling_skull_icon),
         ("bone_nova", bone_nova_icon), ("bone_tempest", bone_tempest_icon))


# --------------------------------------------------------------------------------------------------- the spellbook


def ossuary_codex_icon():
    img = g.from_grid([
        "................",
        "...oooooooooo...",
        "..oiiiiiiiiidco.",
        "..oisssssssidco.",
        "..oisiiiiisidco.",
        "..oisixxxisidcco",
        "..oisirirsidcco.",
        "..oisiiiiisidcco",
        "..oisixixisidcco",
        "..oisssssssidcco",
        "..oiiiiiiiiidco.",
        "..odddddddddxco.",
        "..odddddddddxco.",
        "...ooooooooooo..",
        "................",
        "................",
    ], {"o": OUTLINE, "i": IVORY, "s": IVORY_SHADE, "d": IVORY_DARK, "x": (40, 16, 14), "r": CRIMSON_LIGHT,
        "c": g.CREAM, })
    return img


def ossuary_codex_model_texture():
    """64x64 texture of the 3D book, laid out like the other books: covers of bone plates, a skull in the middle with
    crimson eyes, a spine of vertebrae, old pages."""
    s = 4
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    tex = bone_texture()
    W, H = 12 * s, 9 * s
    cover = tex.resize((W, H))
    img.paste(cover, (0, 0))
    # Plates of bone, a border of vertebrae
    for x in range(0, W, 6):
        d.ellipse((x, 0, x + 5, 4), fill=IVORY_LIGHT + (255,), outline=IVORY_DARK + (255,))
        d.ellipse((x, H - 5, x + 5, H - 1), fill=IVORY_LIGHT + (255,), outline=IVORY_DARK + (255,))
    cx, cy = W // 2, H // 2
    d.ellipse((cx - 8, cy - 9, cx + 8, cy + 5), fill=IVORY_LIGHT + (255,), outline=OUTLINE + (255,))
    d.rectangle((cx - 4, cy + 3, cx + 4, cy + 8), fill=IVORY_LIGHT + (255,), outline=OUTLINE + (255,))
    for sx in (-1, 1):
        d.ellipse((cx + sx * 4 - 3, cy - 4, cx + sx * 4 + 3, cy + 2), fill=(30, 10, 10, 255))
        d.ellipse((cx + sx * 4 - 1, cy - 2, cx + sx * 4 + 1, cy), fill=CRIMSON_LIGHT + (255,))
    d.rectangle((0, 0, W - 1, H - 1), outline=OUTLINE + (255,))
    d.rectangle((12 * s, 0, 13 * s - 1, 9 * s - 1), fill=IVORY_DARK + (255,))
    for y in range(0, 8 * s):
        for x in range(13 * s, 16 * s):
            img.putpixel((x, y), g.lerp(g.CREAM, (176, 168, 140), ((x * 7 + y * 13) % 11) / 22) + (255,))
    for y in range(1, 8 * s, 2):
        d.line((13 * s, y, 16 * s - 1, y), fill=g.CREAM_SHADE + (255,))
    # The spine: vertebrae
    d.rectangle((0, 9 * s, 12 * s - 1, 12 * s - 1), fill=IVORY_SHADE + (255,))
    for x in range(0, 12 * s, 6):
        d.ellipse((x, 9 * s + 1, x + 5, 12 * s - 2), fill=IVORY + (255,), outline=IVORY_DARK + (255,))
    d.rectangle((0, 12 * s, 12 * s - 1, 15 * s - 1), fill=g.CREAM + (255,))
    for x in range(0, 12 * s, 2):
        d.line((x, 12 * s, x, 15 * s - 1), fill=g.CREAM_SHADE + (255,))
    d.rectangle((12 * s, 9 * s, 14 * s - 1, 11 * s - 1), fill=IVORY_LIGHT + (255,))
    for y in range(9 * s, 11 * s):
        for x in range(14 * s, 16 * s):
            t = math.hypot(x - 14.8 * s, y - 9.8 * s) / (1.4 * s)
            img.putpixel((x, y), g.lerp(CRIMSON_LIGHT, CRIMSON_DARK, min(1, t ** .8)) + (255,))
    d.rectangle((12 * s, 11 * s, 16 * s - 1, 12 * s - 1), fill=IVORY_DARK + (255,))
    return img


def affinity_ring_bone():
    return g.from_grid([
        "................",
        "......oooo......",
        ".....oxRRxo.....",
        ".....oRCCRo.....",
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
    ], {"o": OUTLINE, "h": IVORY_LIGHT, "l": IVORY, "b": IVORY_SHADE, "s": IVORY_DARK,
        "R": CRIMSON, "C": CRIMSON_LIGHT, "x": CRIMSON_DARK})


def main():
    g.save(bone_texture(), "textures/entity/bone/bone.png")
    for name, icon in ICONS:
        g.save(icon(), f"textures/gui/spell_icons/{name}.png")
    g.save(affinity_ring_bone(), "textures/item/affinity_ring_bone.png")
    g.write_json("models/item/affinity_ring_bone.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/affinity_ring_bone"}})
    g.save(ossuary_codex_icon(), "textures/item/ossuary_codex.png")
    g.save(ossuary_codex_model_texture(), "textures/item/ossuary_codex_3d.png")
    g.book_models("ossuary_codex")
    g.save(bone_blade_item(), "textures/item/bone_blade.png")
    g.write_json("models/item/bone_blade.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": "arcanes_oublies:item/bone_blade"}})


if __name__ == "__main__":
    main()
