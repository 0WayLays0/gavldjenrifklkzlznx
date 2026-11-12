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


RED = (210, 50, 32)
RED_LIGHT = (255, 186, 120)
RED_DEEP = (150, 28, 18)


def mana_skin(eyes, base=SKY, light=SKY_LIGHT):
    """64 x 64 translucent mana (alpha well under opaque), lighter where the veins run."""
    img = Image.new("RGBA", (64, 64))
    px = img.load()
    sparkles = _sparkles()
    for y in range(64):
        for x in range(64):
            v = _ridges(x, y)
            swirl = fbm(x / 9 + 5, y / 9, 3, 202)
            c = g.lerp(base, light, v * .9 + swirl * .2)
            a = int(130 + 90 * v + 25 * swirl)
            if (x, y) in sparkles:
                c, a = (255, 255, 255), 235
            px[x, y] = c + (min(255, a),)
    if eyes:
        for y in range(60, 64):
            for x in range(60, 64):
                px[x, y] = (255, 255, 255, 255)
    return img


def mana_glow(eyes, dim=(16, 46, 70), bright=(120, 215, 255), spark=(230, 250, 255)):
    """Additive light of the skin: a faint blue all over, the veins and sparkles bright."""
    img = Image.new("RGB", (64, 64))
    px = img.load()
    sparkles = _sparkles()
    for y in range(64):
        for x in range(64):
            v = _ridges(x, y)
            c = g.lerp(dim, bright, v)
            if (x, y) in sparkles:
                c = spark
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


def mana_piranha_icon():
    """A piranha: deep body, forked tail, the lower jaw jutting out, teeth of light."""
    return _icon([
        "................",
        "................",
        "......bbb.......",
        ".....bLLLb......",
        "....bLLLLLbbb...",
        "t..bLLLLLLLLLb..",
        "tbbLLLLLLLLLeLb.",
        ".tLLLLLLLLLLLLLb",
        "..LLLLLLLLLLLLLb",
        ".tLLLLLLLLLLbe.e",
        "tbbLLLLLLLLLb.e.",
        "t..bLLLLLLLLLLLL",
        "....bLLLLLLLLbb.",
        ".....bbLLbbb....",
        "......bLb.......",
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


def mana_fox_icon():
    """A fox at a run: pointed ears, a long snout, a great bushy tail streaming behind."""
    return _icon([
        "................",
        "................",
        "...........b.b..",
        "..........bLbLb.",
        "..........bLLLLb",
        ".........bLLeLLb",
        "tt......bLLLLLbb",
        "tLtbbbbbLLLLLb..",
        "tLLLLLLLLLLLLb..",
        ".tLLLLLLLLLLb...",
        "..tkkLLLLLLb....",
        "...bLb...bLb....",
        "...b.b...b.b....",
        "................",
        "................",
        "................",
    ])


def mana_salamander_icon():
    """A salamander of red mana seen from above, wriggling: splayed legs, a crest of flames down its back, a long tail."""
    img = g.spell_background(8, 8, (130, 40, 24), (30, 8, 6))
    img.alpha_composite(g.from_grid([
        "................",
        "......rr........",
        ".....rLLr.......",
        ".....eLLe.......",
        "..r..rLfr..r....",
        "..rr.rLfLrrr....",
        "....rrLfLr......",
        ".....rLfLr......",
        "..rrrrLfLr......",
        "..r..rLLLr..r...",
        "......rLLrrrr...",
        ".......rLr......",
        "........rLr.....",
        ".........rLr....",
        "..........rr....",
        "................",
    ], {"r": RED_DEEP, "L": RED, "f": (255, 214, 90), "e": (255, 255, 255)}))
    return img


def mana_wolf_icon():
    """A wolf's head, howling up: upright ears, heavy mane, the muzzle raised."""
    return _icon([
        "................",
        "..........bbb...",
        ".........bLLLb..",
        "....b.b.bLLeLb..",
        "...bLbLbLLLLb...",
        "...bLLLLLLLb....",
        "..bLLeLLLLb.....",
        "..bLLLLLLLb.....",
        ".bkLLLLLLLLb....",
        ".bkkLLLLLLLLb...",
        "..bkkLLLLLLLLb..",
        "...bkkkLLLLLLb..",
        "....bbkkkLLLLb..",
        "......bbbLLLb...",
        ".........bbb....",
        "................",
    ])


def mana_butterfly_icon():
    """A butterfly seen from above, wings spread, antennae curling."""
    return _icon([
        "................",
        "......b..b......",
        ".......bb.......",
        ".bbb...bb...bbb.",
        "bLLLb..bb..bLLLb",
        "bLeLLb.bb.bLLeLb",
        "bLLLLLbkkbLLLLLb",
        ".bLLLLbkkbLLLLb.",
        "..bbbbbkkbbbbb..",
        "..bLLLbkkbLLLb..",
        ".bLLeLbkkbLeLLb.",
        ".bLLLLb..bLLLLb.",
        "..bLLb....bLLb..",
        "...bb......bb...",
        "................",
        "................",
    ])


def mana_kangaroo_icon():
    """A kangaroo standing in a boxer's guard, tall ears, a big fist forward, the tail behind."""
    return _icon([
        "................",
        "......b.b.......",
        ".....bLbLb......",
        ".....bLbLb......",
        "....bLLLLLb.....",
        "...bLLeLLLb.....",
        "..bLLLLLLb......",
        "...bLLLLLbbb....",
        "...bLLLLLbkkb...",
        "..bLLLLLLbkkb...",
        "..bLLLLLLLbb....",
        "..bLLLLLLLb.....",
        ".tbLLb.bLLb.....",
        "tt.bLb..bLb.....",
        "t..bLLLb.bLLLb..",
        "................",
    ])


def mana_falcon_icon():
    """A falcon diving at us, wings swept back, hooked beak."""
    return _icon([
        "................",
        "................",
        "bb............bb",
        "bLbb........bbLb",
        ".bLLbb....bbLLb.",
        "..bLLLb..bLLLb..",
        "...bLLLbbLLLb...",
        "....bLLeeLLb....",
        ".....bLkkLb.....",
        "......bkkb......",
        "......bLLb......",
        ".....bLLLLb.....",
        ".....btLLtb.....",
        "......t..t......",
        "................",
        "................",
    ])


def mana_skin_tiger():
    """The tiger's mana: the sky-blue skin crossed by stripes of deep indigo, wavering down the texture's columns so
    that they run round the body, the legs and the tail like a tiger's; eyes in the bright corner."""
    img = mana_skin(True)
    px = img.load()
    for y in range(64):
        for x in range(64):
            if x >= 60 and y >= 60:
                continue
            s = _stripe(x, y)
            if s > 0:
                r, gg, b, a = px[x, y]
                c = g.lerp((r, gg, b), TIGER_STRIPE, s)
                px[x, y] = c + (min(255, int(a + 70 * s)),)
    return img


TIGER_STRIPE = (18, 30, 92)


def _stripe(x, y):
    """0..1: how much a tiger stripe covers (x, y): narrow bands every 9 or 10 pixels, wavering and tapering."""
    wobble = fbm(x / 11 + 3, y / 7, 3, 211) * 5.5
    phase = (x + wobble) / 9.5
    d = abs(phase - round(phase))
    taper = fbm(x / 5, y / 5 + 9, 2, 212)
    width = .05 + .13 * taper
    return smoothstep(width + .07, width - .03, d)


def mana_glow_tiger():
    """Its light: dim mana, the stripes glowing a vivid cyan from within."""
    img = mana_glow(True)
    px = img.load()
    for y in range(64):
        for x in range(64):
            if x >= 60 and y >= 60:
                continue
            s = _stripe(x, y)
            if s > 0:
                r, gg, b, a = px[x, y]
                px[x, y] = g.lerp((r, gg, b), (80, 210, 255), s * .75) + (255,)
    return img


def mana_tiger_icon():
    """A tiger's head seen from the front, roaring: rounded ears, glowing stripes, fangs of light."""
    img = g.spell_background(8, 8, (40, 96, 150), NIGHT)
    img.alpha_composite(g.from_grid([
        "................",
        "..bb........bb..",
        ".bLLb......bLLb.",
        ".bLdLbbbbbbLdLb.",
        "..bLLLdLLdLLLb..",
        ".bLdLLLddLLLdLb.",
        ".bLLeeLLLLeeLLb.",
        "bdLLLLLLLLLLLLdb",
        "bLdLLLbkkbLLLdLb",
        "bLLdLbkkkkbLdLLb",
        ".bLLLbkddkbLLLb.",
        ".bdLLbekkebLLdb.",
        "..bLLLbeebLLLb..",
        "...bbLLLLLLbb...",
        ".....bbbbbb.....",
        "................",
    ], dict(LEGEND, d=(30, 70, 150))))
    return img


def mana_scarab_icon():
    """A scarab seen from above: the toothed head, the shield of its thorax, the domed wing-cases parted by a seam of
    light, six legs splayed."""
    img = g.spell_background(8, 8, (40, 96, 150), NIGHT)
    img.alpha_composite(g.from_grid([
        "................",
        "......kbbk......",
        ".....bkLLkb.....",
        "..t..beLLeb..t..",
        "...tbLLLLLLbt...",
        "....bLLLLLLb....",
        ".tt.bbbkkbbb.tt.",
        "...bLLLbbLLLb...",
        "..bLLLLbbLLLLb..",
        "ttbLkLLbbLLkLbtt",
        "..bLLLLbbLLLLb..",
        "..bLLLLbbLLLLb..",
        ".tbLLLLkkLLLLbt.",
        "t..bLLLkkLLLb..t",
        "....bbLLLLbb....",
        "......bbbb......",
    ], LEGEND))
    return img


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
    # The salamander's red mana, embers in its veins
    g.save(mana_skin(True, RED, RED_LIGHT), "textures/entity/mana_skin_red_eyes.png")
    g.save(mana_glow(True, (34, 6, 3), (215, 92, 36), (255, 220, 160)), "textures/entity/mana_glow_red_eyes.png")
    # The tiger's stripes, dark on its skin and glowing in its light
    g.save(mana_skin_tiger(), "textures/entity/mana_skin_tiger_eyes.png")
    g.save(mana_glow_tiger(), "textures/entity/mana_glow_tiger_eyes.png")
    for name, fn in (("mana_rats", mana_rats_icon), ("mana_piranha", mana_piranha_icon), ("mana_dromedary", mana_dromedary_icon),
                     ("mana_serpent", mana_serpent_icon), ("mana_crows", mana_crows_icon), ("mana_fox", mana_fox_icon),
                     ("mana_wolf", mana_wolf_icon), ("mana_butterfly", mana_butterfly_icon), ("mana_kangaroo", mana_kangaroo_icon),
                     ("mana_falcon", mana_falcon_icon), ("mana_salamander", mana_salamander_icon),
                     ("mana_tiger", mana_tiger_icon), ("mana_scarab", mana_scarab_icon)):
        g.save(fn(), f"textures/gui/spell_icons/{name}.png")
    g.save(affinity_ring_errethys(), "textures/item/affinity_ring_errethys.png")
    g.write_json("models/item/affinity_ring_errethys.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/affinity_ring_errethys"}})
