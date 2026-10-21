#!/usr/bin/env python3
"""Errethys school art: the translucent sky-blue mana skin of the summoned creatures, spell icons, scroll and ring.

Called by gen_assets.py (run that one). All art is original. The skin has no fixed layout so it fits any model; the
"eyes" variants add a bright corner (60..63, 60..63) that the addon's own models map their eyes to.
"""
import math
import random

from PIL import Image

import gen_assets as g
from explosion_assets import fbm, smoothstep

SKY = (135, 206, 235)
SKY_DEEP = (40, 120, 170)
SKY_LIGHT = (205, 240, 255)
NIGHT = (8, 16, 32)


def _ridges(x, y):
    """0..1, peaking along thin winding lines: the veins of mana."""
    n = fbm(x / 13, y / 13, 4, 201)
    return smoothstep(.78, .97, 1 - abs(2 * n - 1))


def _sparkles():
    rnd = random.Random(203)
    return {(rnd.randrange(64), rnd.randrange(64)) for _ in range(40)}


def mana_skin(eyes):
    """64 x 64 translucent mana (alpha well under opaque), lighter where the veins run."""
    img = Image.new("RGBA", (64, 64))
    px = img.load()
    sparkles = _sparkles()
    for y in range(64):
        for x in range(64):
            v = _ridges(x, y)
            swirl = fbm(x / 9 + 5, y / 9, 3, 202)
            c = g.lerp(SKY, SKY_LIGHT, v * .9 + swirl * .2)
            a = int(130 + 90 * v + 25 * swirl)
            if (x, y) in sparkles:
                c, a = (255, 255, 255), 235
            px[x, y] = c + (min(255, a),)
    if eyes:
        for y in range(60, 64):
            for x in range(60, 64):
                px[x, y] = (255, 255, 255, 255)
    return img


def mana_glow(eyes):
    """Additive light of the skin: a faint blue all over, the veins and sparkles bright."""
    img = Image.new("RGB", (64, 64))
    px = img.load()
    sparkles = _sparkles()
    for y in range(64):
        for x in range(64):
            v = _ridges(x, y)
            c = g.lerp((16, 46, 70), (120, 215, 255), v)
            if (x, y) in sparkles:
                c = (230, 250, 255)
            px[x, y] = c
    if eyes:
        for y in range(60, 64):
            for x in range(60, 64):
                px[x, y] = (255, 255, 255)
    return img.convert("RGBA")


LEGEND = {"b": SKY_DEEP, "L": SKY_LIGHT, "e": (255, 255, 255), "t": (110, 180, 220), "k": (230, 245, 255)}


def _icon(rows):
    img = g.spell_background(8, 8, (40, 96, 150), NIGHT)
    img.alpha_composite(g.from_grid(rows, LEGEND))
    return img


def mana_rats_icon():
    return _icon([
        "................",
        "................",
        "................",
        "................",
        "..........bb....",
        ".........bLLb...",
        "....bbbbbLLeLb..",
        "...bLLLLLLLLLLLb",
        "..bLLLLLLLLLLbb.",
        ".tbLLLLLLLLLb...",
        "t..bLbbbbbLb....",
        "t...b.....b.....",
        ".t..............",
        "..tt............",
        "................",
        "................",
    ])


def mana_salmon_icon():
    return _icon([
        "................",
        "................",
        "................",
        "................",
        "................",
        ".......bb.......",
        "......bLLb......",
        "t...bbLLLLbbb...",
        "tt.bLLLLLLLLLbb.",
        "tttLLLLLLLLLeLLb",
        "tt.bLLLLLLLLLLb.",
        "t...bbLLLLLbbb..",
        "......bLb.......",
        "................",
        "................",
        "................",
    ])


def mana_dromedary_icon():
    return _icon([
        "................",
        "................",
        "..........bb....",
        "....bb...bLLb...",
        "...bLLb..bLeLL..",
        "..bLLLLb.bLLb...",
        ".bLLLLLLbLLb....",
        ".bLLLLLLLLLb....",
        ".bLLLLLLLLb.....",
        "..bLLLLLLLb.....",
        "..L.L....L.L....",
        "..L.L....L.L....",
        "..L.L....L.L....",
        ".bb.bb..bb.bb...",
        "................",
        "................",
    ])


def mana_serpent_icon():
    return _icon([
        "................",
        "................",
        "................",
        "................",
        "................",
        "...........bbb..",
        "..bbb.....bLLeb.",
        ".bLLLb...bLLLLk.",
        "bLb.bLb.bLb.bb..",
        "Lb...bLLLb......",
        "b.....bbb.......",
        "................",
        "................",
        "................",
        "................",
        "................",
    ])


def mana_crows_icon():
    return _icon([
        "................",
        "................",
        "................",
        "................",
        ".bb..........bb.",
        "bLLb........bLLb",
        ".bLLb..bb..bLLb.",
        "..bLLbbLLbbLLb..",
        "...bLLLLLLLLb...",
        "....bbLeeLbb....",
        "......bkkb......",
        ".......kk.......",
        "................",
        "................",
        "................",
        "................",
    ])


def affinity_ring_errethys():
    return g.from_grid([
        "................",
        "......oooo......",
        ".....oghgo......",
        ".....ogtgo......",
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
    ], {"o": g.OUTLINE, "h": g.HIGH, "l": g.LIGHT, "b": g.BASE, "s": g.SHADOW,
        "g": SKY, "t": SKY_DEEP})


def main():
    g.save(mana_skin(False), "textures/entity/mana_skin.png")
    g.save(mana_skin(True), "textures/entity/mana_skin_eyes.png")
    g.save(mana_glow(False), "textures/entity/mana_glow.png")
    g.save(mana_glow(True), "textures/entity/mana_glow_eyes.png")
    for name, fn in (("mana_rats", mana_rats_icon), ("mana_salmon", mana_salmon_icon), ("mana_dromedary", mana_dromedary_icon),
                     ("mana_serpent", mana_serpent_icon), ("mana_crows", mana_crows_icon)):
        g.save(fn(), f"textures/gui/spell_icons/{name}.png")
    g.save(affinity_ring_errethys(), "textures/item/affinity_ring_errethys.png")
    g.write_json("models/item/affinity_ring_errethys.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/affinity_ring_errethys"}})
