#!/usr/bin/env python3
"""Shared pieces for the simpler grand spellbooks (the Codex of Mending, the Trapper's Codex): the 3D book's texture in
the grimoire layout (8 pixels a unit), drawn from a palette and a cover decoration, its 16 px icon and the school's
affinity ring. Called by the school's own asset script."""
import math

import numpy as np
from PIL import Image, ImageDraw

import gen_assets as g
from water_assets import _tnoise, _rgba, _ramp


def _metal(img, x0, y0, w, h, light, dark):
    for yy in range(y0, y0 + h):
        for xx in range(x0, x0 + w):
            t = .5 + .5 * math.sin((xx - x0) * .5 + (yy - y0) * .2)
            img.putpixel((xx, yy), g.lerp(dark, light, t * .85 + .1) + (255,))


def _jewel(img, x0, y0, w, h, color):
    for yy in range(y0, y0 + h):
        for xx in range(x0, x0 + w):
            t = abs((xx - x0) / w - .5) + abs((yy - y0) / h - .5)
            img.putpixel((xx, yy), g.lerp((255, 255, 255), color, min(1, t * 1.3)) + (255,))


def codex_texture(cover_ramp, metal_light, metal_dark, jewel, decorate, seed):
    """128 x 128: covers of {@code cover_ramp} leather, {@code decorate(d, img, W, H)} drawing on them, a metal frame,
    the cover's edge of metal, old pages, the spine, and the plain book's and ornaments' materials."""
    s = 8
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    W, H = 12 * s, 9 * s
    ys, xs = np.mgrid[0:H, 0:W].astype(np.float32)
    grain = _tnoise(W, H, (24, 18), seed) * .5 + _tnoise(W, H, (6, 4), seed + 1) * .5
    vign = np.clip(1 - (((xs - W / 2) / (W / 2)) ** 2 + ((ys - H / 2) / (H / 2)) ** 2) * .4, 0, 1)
    cover = _ramp(np.clip((.2 + .6 * grain) * (.55 + .45 * vign), 0, 1), cover_ramp)
    img.paste(_rgba(cover, np.full((H, W), 255)), (0, 0))
    d = ImageDraw.Draw(img)
    decorate(d, img, W, H)
    d.rectangle((1, 1, W - 2, H - 2), outline=metal_light + (255,))
    d.rectangle((3, 3, W - 4, H - 4), outline=metal_dark + (255,))
    d.rectangle((0, 0, W - 1, H - 1), outline=cover_ramp[0] + (255,))
    for yy in range(0, 9 * s):
        for xx in range(12 * s, 13 * s):
            img.putpixel((xx, yy), g.lerp(metal_dark, metal_light, .5 + .5 * math.sin(yy * .5)) + (255,))
    for yy in range(0, 8 * s):
        for xx in range(13 * s, 16 * s):
            img.putpixel((xx, yy), g.lerp(g.CREAM, (176, 168, 140), ((xx * 7 + yy * 13) % 11) / 22) + (255,))
    for yy in range(1, 8 * s, 2):
        d.line((13 * s, yy, 16 * s - 1, yy), fill=g.CREAM_SHADE + (255,))
    spine = _ramp(np.clip(.2 + .6 * _tnoise(12 * s, 3 * s, (12, 4), seed + 2), 0, 1), cover_ramp[:3])
    img.paste(_rgba(spine, np.full((3 * s, 12 * s), 255)), (0, 9 * s))
    for yy in (9 * s + 5, 12 * s - 6):
        d.line((0, yy, 12 * s - 1, yy), fill=metal_light + (255,))
    d.rectangle((0, 12 * s, 12 * s - 1, 15 * s - 1), fill=g.CREAM + (255,))
    for xx in range(0, 12 * s, 2):
        d.line((xx, 12 * s, xx, 15 * s - 1), fill=g.CREAM_SHADE + (255,))
    _metal(img, 12 * s, 9 * s, 2 * s, 2 * s, metal_light, metal_dark)
    _jewel(img, 14 * s, 9 * s, 2 * s, 2 * s, jewel)
    d.rectangle((12 * s, 11 * s, 16 * s - 1, 12 * s - 1), fill=cover_ramp[0] + (255,))
    d.line((12 * s, 11 * s + 3, 16 * s - 1, 11 * s + 3), fill=metal_light + (255,))
    _metal(img, 12 * s, 12 * s, 2 * s, 2 * s, metal_light, metal_dark)
    _metal(img, 14 * s, 12 * s, 2 * s, 2 * s, metal_dark, cover_ramp[0])
    _jewel(img, 12 * s, 14 * s, 2 * s, 2 * s, jewel)
    for yy in range(14 * s, 16 * s):
        for xx in range(14 * s, 16 * s):
            t = math.hypot(xx - 15 * s, yy - 15 * s) / (1.2 * s)
            img.putpixel((xx, yy), g.lerp((255, 255, 255), jewel, min(1, t)) + (255,))
    return img


def codex_icon(dark, mid, light, metal, metal_dark, jewel):
    return g.from_grid([
        "................",
        "...oooooooooo...",
        "..oppppppppppco.",
        "..oplllllllllco.",
        "..opppppJppppcco",
        "..oplllJJJllpco.",
        "..oppppJJJppcco.",
        "..oplllpJpllgcco",
        "..opppppppppco..",
        "..oplllllllllcco",
        "..oppppppppppco.",
        "..oddMdddMdddxco",
        "..odddddddddxcco",
        "...ooooooooooo..",
        "................",
        "................",
    ], {"o": dark, "p": mid, "l": light, "J": jewel, "M": metal, "g": metal_dark, "d": dark, "x": dark, "c": g.CREAM})


def ring(dark, metal, metal_light, metal_dark, jewel, jewel_light):
    return g.from_grid([
        "................",
        "......oooo......",
        ".....oYRRYo.....",
        ".....oRWWRo.....",
        "....oooooooo....",
        "...ohllllbbso...",
        "..ohlo....obso..",
        "..olo......oso..",
        "..ovo......oso..",
        "..olo......ovo..",
        "..obso....osso..",
        "...obbvbbssso...",
        "....oooooooo....",
        "................",
        "................",
        "................",
    ], {"o": dark, "h": metal_light, "l": metal, "b": metal, "s": metal_dark, "v": jewel, "Y": metal_light, "R": jewel, "W": jewel_light})


def icon(draw, bg, night):
    return g.painted(64, draw, (.5, .5, bg, night), 16)


def a(c, alpha=255):
    return tuple(int(x) for x in c) + (alpha,)


def write_book(name, icon_img, texture):
    g.save(icon_img, f"textures/item/{name}.png")
    g.save(texture, f"textures/item/{name}_3d.png")
    g.book_models(name)


def write_ring(school, img):
    g.save(img, f"textures/item/affinity_ring_{school}.png")
    g.write_json(f"models/item/affinity_ring_{school}.json", {"parent": "item/generated", "textures": {"layer0": f"arcanes_oublies:item/affinity_ring_{school}"}})
