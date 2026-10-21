#!/usr/bin/env python3
"""Physical Prowess art: spell icons (the weapon styles' in physical_weapon_icons.py), the trace a blow leaves in the
air, the bleeding, sundered and pinned icons, the throwing knife and javelin, scroll and ring, and the fighters' own
body animations (player_animation/physical_skills.json, read by Player Animator; written by physical_animations.py).

Called by gen_assets.py (run that one). All art is original. Nothing here glows: these moves are not magic.
"""
import math

from PIL import Image, ImageDraw

import gen_assets as g
import physical_animations
from explosion_assets import smoothstep

LEATHER = (170, 110, 70)
DARK = (28, 16, 10)
SKIN = (232, 184, 146)
SKIN_SHADE = (186, 128, 92)
OUTLINE = (44, 26, 16)
AIR = (250, 250, 255)
AIR_DIM = (200, 206, 218)
STEEL = (216, 224, 238)
STEEL_DARK = (130, 140, 158)
WOOD = (122, 82, 46)
BLOOD = (168, 18, 24)
DUST = (196, 166, 124)
BG = (.5, .5, LEATHER, DARK)


# ---------------------------------------------------------------- textures

def strike_trail():
    """32 x 32 white streak: transparent at the inner edge (v = 0), solid toward the outer edge (v = 1), the same
    along u. The trails tint and fade it."""
    S = 32
    img = Image.new("RGBA", (S, S))
    px = img.load()
    for y in range(S):
        v = y / (S - 1)
        a = smoothstep(0, .85, v) * (1 - .35 * smoothstep(.9, 1, v))
        for x in range(S):
            px[x, y] = (255, 255, 255, int(255 * a))
    return img


def bleeding_effect_icon():
    def draw(d, img, S):
        # A cut and three drops of blood
        d.line((.18 * S, .28 * S, .78 * S, .5 * S), fill=(90, 8, 12, 255), width=max(2, S // 12))
        for cx, cy, r in ((.36, .62, .1), (.58, .72, .13), (.74, .58, .08)):
            x, y, rr = cx * S, cy * S, r * S
            d.ellipse((x - rr, y - rr, x + rr, y + rr), fill=BLOOD + (255,))
            d.polygon([(x - rr * .8, y - rr * .5), (x + rr * .8, y - rr * .5), (x, y - rr * 2.2)], fill=BLOOD + (255,))
            d.ellipse((x - rr * .45, y - rr * .2, x - rr * .1, y + rr * .15), fill=(240, 120, 120, 255))
    return g.painted(72, draw, None, 18)


def sundered_effect_icon():
    def draw(d, img, S):
        # A chestplate split by a cut
        plate = [(.22, .2), (.4, .14), (.5, .24), (.6, .14), (.78, .2), (.84, .5), (.74, .52), (.72, .86), (.28, .86), (.26, .52), (.16, .5)]
        d.polygon([(x * S, y * S) for x, y in plate], fill=(150, 156, 170, 255), outline=OUTLINE + (255,))
        d.polygon([(.36 * S, .36 * S), (.64 * S, .36 * S), (.62 * S, .74 * S), (.38 * S, .74 * S)], fill=(186, 192, 204, 255))
        d.line((.2 * S, .26 * S, .8 * S, .8 * S), fill=(40, 30, 26, 255), width=max(3, S // 10))
        d.line((.24 * S, .24 * S, .82 * S, .76 * S), fill=(236, 236, 240, 255), width=max(1, S // 30))
    return g.painted(72, draw, None, 18)


def pinned_effect_icon():
    def draw(d, img, S):
        # A knife driven point first into the ground, cracks around it
        d.rectangle((.08 * S, .74 * S, .92 * S, .9 * S), fill=(120, 92, 62, 255))
        for x0, x1, y1 in ((.5, .3, .9), (.5, .72, .88), (.5, .2, .78)):
            d.line((x0 * S, .76 * S, x1 * S, y1 * S), fill=(50, 34, 22, 255), width=max(1, S // 24))
        d.polygon([(.44 * S, .3 * S), (.56 * S, .3 * S), (.56 * S, .7 * S), (.5 * S, .8 * S), (.44 * S, .7 * S)], fill=STEEL + (255,),
                  outline=OUTLINE + (255,))
        d.line((.5 * S, .34 * S, .5 * S, .72 * S), fill=STEEL_DARK + (255,), width=max(1, S // 30))
        d.rectangle((.36 * S, .26 * S, .64 * S, .31 * S), fill=(96, 100, 114, 255), outline=OUTLINE + (255,))
        d.rectangle((.45 * S, .08 * S, .55 * S, .26 * S), fill=(92, 58, 30, 255), outline=OUTLINE + (255,))
    return g.painted(72, draw, None, 18)


def throwing_knife():
    """16 x 16, the knife's side seen flat in rows 6 to 9 (what the missile renderer draws), point to the right."""
    img = Image.new("RGBA", (16, 16))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 7, 4, 8), fill=(92, 58, 30, 255))
    for x in (1, 3):
        d.point((x, 7), fill=(60, 36, 18, 255))
        d.point((x, 8), fill=(60, 36, 18, 255))
    d.rectangle((5, 6, 5, 9), fill=(96, 100, 114, 255))
    d.polygon([(6, 7), (12, 7), (15, 8), (12, 8), (6, 8)], fill=STEEL + (255,))
    d.line((6, 8, 13, 8), fill=STEEL_DARK + (255,))
    return img


def javelin():
    """32 x 4, the javelin's side seen flat: a wooden shaft and a leaf-shaped iron head to the right."""
    img = Image.new("RGBA", (32, 4))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 1, 23, 2), fill=WOOD + (255,))
    d.line((0, 1, 23, 1), fill=(164, 116, 66, 255))
    d.rectangle((21, 1, 22, 2), fill=(60, 40, 24, 255))
    d.polygon([(23, 0), (28, 0), (31, 1), (31, 2), (28, 3), (23, 3)], fill=(150, 156, 170, 255))
    d.line((24, 1, 30, 1), fill=STEEL + (255,))
    d.line((24, 2, 30, 2), fill=STEEL_DARK + (255,))
    return img


# ---------------------------------------------------------------- icons

def _fist(d, x, y, s, facing=1):
    """A clenched fist seen from the side, knuckles toward {facing} (1 right, -1 left), (x, y) its middle."""
    w, h = s, s * .8
    d.rounded_rectangle((x - w / 2, y - h / 2, x + w / 2, y + h / 2), radius=s * .22, fill=SKIN + (255,), outline=OUTLINE + (255,), width=max(1, int(s / 9)))
    # Fingers folded in, the thumb across them
    for k in range(3):
        fx = x + facing * w * .18
        fy = y - h * .3 + k * h * .26
        d.line((fx, fy, x + facing * w * .45, fy), fill=SKIN_SHADE + (255,), width=max(1, int(s / 14)))
    d.line((x - facing * w * .1, y + h * .1, x + facing * w * .3, y + h * .1), fill=OUTLINE + (255,), width=max(1, int(s / 12)))
    # The wrist
    d.rectangle((x - facing * w * .5 - facing * w * .25, y - h * .25, x - facing * w * .5, y + h * .25), fill=SKIN_SHADE + (255,))


def _speed_lines(d, x0, x1, ys, width):
    for y in ys:
        d.line((x0, y, x1, y), fill=AIR + (200,), width=width)


def _arc(d, box, start, end, color, width):
    d.arc(box, start, end, fill=color + (255,), width=width)


PIXELS = {"o": OUTLINE, "s": SKIN, "S": SKIN_SHADE, "w": AIR, "a": AIR_DIM, "m": (238, 244, 252), "M": STEEL_DARK,
          "h": (92, 58, 30), "H": (178, 130, 82), "d": DUST, "r": BLOOD}


def _pixel_icon(rows, cx=8, cy=8):
    img = g.spell_background(cx, cy, LEATHER, DARK)
    img.alpha_composite(g.from_grid(rows, PIXELS))
    return img


def punch_icon():
    """Pixel art: a fist driving right, speed lines behind it, a burst of air at the knuckles."""
    return _pixel_icon([
        "................",
        "................",
        "................",
        "......ooooooo...",
        ".....osssssssow.",
        "ww..osSssSssSo.w",
        "....ossssssssow.",
        "www.oSssSssSso.w",
        "....ossssssssow.",
        "ww..osSSSSSsso.w",
        ".....ossssssso..",
        "......ooooooo...",
        "................",
        "................",
        "................",
        "................",
    ])


def punch_flurry_icon():
    """Pixel art: three fists one after another, speed lines behind them."""
    return _pixel_icon([
        "................",
        "....ooo.........",
        "ww.osSso.w......",
        "...ossso..w.....",
        "ww..ooo..w......",
        "........ooo.....",
        "www....osSso.w..",
        ".......ossso..w.",
        "www.....ooo..w..",
        "................",
        "....ooo.........",
        "ww.osSso.w......",
        "...ossso..w.....",
        "ww..ooo..w......",
        "................",
        "................",
    ])


def slash_icon():
    def draw(d, img, S):
        # A pale crescent from top right to bottom left
        _arc(d, (-.3 * S, .02 * S, .9 * S, 1.22 * S), 270, 345, AIR, max(4, S // 7))
        _arc(d, (-.3 * S, .02 * S, .9 * S, 1.22 * S), 272, 340, STEEL, max(2, S // 16))
    return g.painted(64, draw, BG)


def slash_flurry_icon():
    def draw(d, img, S):
        _arc(d, (-.3 * S, .02 * S, .9 * S, 1.22 * S), 270, 345, AIR, max(3, S // 9))
        _arc(d, (.1 * S, .02 * S, 1.3 * S, 1.22 * S), 195, 270, AIR, max(3, S // 9))
        _arc(d, (.05 * S, .55 * S, .95 * S, 1.25 * S), 200, 340, AIR_DIM, max(2, S // 14))
    return g.painted(64, draw, BG)


def dash_icon():
    def draw(d, img, S):
        # Two chevrons racing right, dust kicked up behind
        for k, x in enumerate((.36, .6)):
            c = AIR if k else AIR_DIM
            d.line((x * S, .26 * S, (x + .2) * S, .5 * S, x * S, .74 * S), fill=c + (255,), width=max(3, S // 10), joint="curve")
        _speed_lines(d, .04 * S, .3 * S, (.38 * S, .5 * S, .62 * S), max(1, S // 28))
        for k in range(5):
            x = (.08 + k * .06) * S
            y = (.82 + (k % 2) * .05) * S
            d.ellipse((x - .03 * S, y - .03 * S, x + .03 * S, y + .03 * S), fill=DUST + (255,))
    return g.painted(64, draw, BG)


def leap_icon():
    def draw(d, img, S):
        # An arrow shooting up from a puff of dust
        d.polygon([(.5 * S, .08 * S), (.76 * S, .38 * S), (.58 * S, .38 * S), (.58 * S, .68 * S), (.42 * S, .68 * S), (.42 * S, .38 * S), (.24 * S, .38 * S)],
                  fill=AIR + (255,), outline=AIR_DIM + (255,))
        for k in range(7):
            a = math.pi + k * math.pi / 6
            x = .5 * S + math.cos(a) * .3 * S
            y = .84 * S + math.sin(a) * .08 * S
            d.ellipse((x - .05 * S, y - .04 * S, x + .05 * S, y + .04 * S), fill=DUST + (255,))
    return g.painted(64, draw, BG)


SCYTHE = [
    "................",
    "......ooooo.....",
    "....oommmmmoo...",
    "...ommMMMMMmmoH.",
    "..omMoo.....oHh.",
    ".omMo.......Hh..",
    ".oMo.......Hh...",
    ".oo.......Hh....",
    ".........Hh.....",
    "........Hh......",
    ".......Hh.......",
    "......Hh........",
    ".....Hh.........",
    "....Hh..........",
    "...Hh...........",
    "................",
]


def _overlay(base, top):
    """{top} drawn over {base}, both character grids ('.' lets {base} show)."""
    return ["".join(t if t != "." else b for b, t in zip(rb, rt)) for rb, rt in zip(base, top)]


def scythe_reap_icon():
    """Pixel art: a scythe over a low sweep."""
    sweep = [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "...............w",
        "..............w.",
        "w............w..",
        ".w..........wa..",
        "..wwwaaaawww....",
        "................",
    ]
    return _pixel_icon(_overlay(sweep, SCYTHE))


def scythe_dance_icon():
    """Pixel art: a scythe inside the circle of a full spin."""
    ring = [
        ".....wwwwww.....",
        "...ww......ww...",
        "..w..........w..",
        ".a............a.",
        ".w............w.",
        "w..............w",
        "w..............w",
        "a..............a",
        "a..............a",
        "w..............w",
        "w..............w",
        ".w............w.",
        ".a............a.",
        "..w..........w..",
        "...ww......ww...",
        ".....wwwwww.....",
    ]
    return _pixel_icon(_overlay(ring, SCYTHE))


def _sword(d, S, x0, y0, x1, y1, guard=.12, width=None):
    """A sword from its pommel (x0, y0) to its point (x1, y1)."""
    dx, dy = x1 - x0, y1 - y0
    length = math.hypot(dx, dy)
    ux, uy = dx / length, dy / length
    px, py = -uy, ux
    gx, gy = x0 + ux * .18, y0 + uy * .18
    w = width or max(3, S // 12)
    d.line((x0 * S, y0 * S, gx * S, gy * S), fill=WOOD + (255,), width=w)
    d.line(((gx - px * guard) * S, (gy - py * guard) * S, (gx + px * guard) * S, (gy + py * guard) * S), fill=(200, 160, 70, 255), width=w)
    d.polygon([((gx + px * .04) * S, (gy + py * .04) * S), ((gx - px * .04) * S, (gy - py * .04) * S), (x1 * S, y1 * S)],
              fill=STEEL + (255,), outline=STEEL_DARK + (255,))


def sword_rising_cut_icon():
    def draw(d, img, S):
        _arc(d, (.1 * S, .05 * S, 1.3 * S, 1.25 * S), 180, 265, AIR, max(3, S // 9))
        _sword(d, S, .66, .94, .36, .1)
    return g.painted(64, draw, BG)


def sword_lunge_icon():
    def draw(d, img, S):
        _speed_lines(d, .02 * S, .3 * S, (.38 * S, .62 * S), max(1, S // 28))
        _sword(d, S, .1, .5, .96, .5, guard=.14)
        _arc(d, (.82 * S, .38 * S, 1.06 * S, .62 * S), 120, 240, AIR, max(1, S // 24))
    return g.painted(64, draw, BG)


def _dagger(d, S, x0, y0, x1, y1):
    _sword(d, S, x0, y0, x1, y1, guard=.08, width=max(2, S // 16))


def dagger_stabs_icon():
    def draw(d, img, S):
        _dagger(d, S, .08, .7, .62, .4)
        for k, y in enumerate((.24, .44, .64)):
            d.line((.7 * S, y * S, .96 * S, (y - .06) * S), fill=AIR + (230,), width=max(1, S // 26))
        for x, y in ((.78, .8), (.88, .7)):
            d.ellipse(((x - .04) * S, (y - .05) * S, (x + .04) * S, (y + .05) * S), fill=BLOOD + (255,))
    return g.painted(64, draw, BG)


def dagger_flank_icon():
    def draw(d, img, S):
        # A curved dash around a target, ending behind it with the dagger raised
        d.ellipse((.4 * S, .38 * S, .6 * S, .58 * S), fill=(90, 60, 40, 255), outline=OUTLINE + (255,))
        _arc(d, (.14 * S, .14 * S, .86 * S, .86 * S), 160, 340, AIR, max(2, S // 16))
        d.polygon([(.84 * S, .36 * S), (.94 * S, .5 * S), (.76 * S, .48 * S)], fill=AIR + (255,))
        _dagger(d, S, .64, .9, .9, .62)
    return g.painted(64, draw, BG)


# ---------------------------------------------------------------- scroll and ring

def affinity_ring_physical():
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
        "g": LEATHER, "t": (110, 66, 38)})


def main():
    g.save(strike_trail(), "textures/entity/strike_trail.png")
    g.save(bleeding_effect_icon(), "textures/mob_effect/bleeding.png")
    for name, fn in (("punch", punch_icon), ("punch_flurry", punch_flurry_icon), ("slash", slash_icon), ("slash_flurry", slash_flurry_icon),
                     ("dash", dash_icon), ("leap", leap_icon), ("scythe_reap", scythe_reap_icon), ("scythe_dance", scythe_dance_icon),
                     ("sword_rising_cut", sword_rising_cut_icon), ("sword_lunge", sword_lunge_icon),
                     ("dagger_stabs", dagger_stabs_icon), ("dagger_flank", dagger_flank_icon)):
        g.save(fn(), f"textures/gui/spell_icons/{name}.png")
    g.save(affinity_ring_physical(), "textures/item/affinity_ring_physical.png")
    g.write_json("models/item/affinity_ring_physical.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/affinity_ring_physical"}})
    g.save(sundered_effect_icon(), "textures/mob_effect/sundered.png")
    g.save(pinned_effect_icon(), "textures/mob_effect/pinned.png")
    g.save(throwing_knife(), "textures/entity/throwing_knife.png")
    g.save(javelin(), "textures/entity/javelin.png")
    # It draws with this module's helpers, so it is loaded once this module is
    import physical_weapon_icons
    physical_weapon_icons.main()
    g.write_json("player_animation/physical_skills.json", physical_animations.animations())
