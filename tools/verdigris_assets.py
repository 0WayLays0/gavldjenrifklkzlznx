#!/usr/bin/env python3
"""Moss, Copper and Rust Magic art: the spell icons, the Rusted Codex (icon and 3D book texture), its affinity ring and
the Rusted and Entangled effect icons.

Called by gen_assets.py (run that one). The spells themselves are drawn in game from the game's own copper, stone,
moss and vine blocks, so they need no textures.
"""
import math

from PIL import Image, ImageDraw

import gen_assets as g
from explosion_assets import fbm

VERDIGRIS = (79, 191, 150)
VERDIGRIS_LIGHT = (160, 236, 206)
VERDIGRIS_DEEP = (38, 116, 92)
COPPER = (216, 129, 74)
COPPER_LIGHT = (246, 178, 120)
RUST = (160, 69, 30)
RUST_DARK = (96, 38, 16)
MOSS = (107, 142, 35)
MOSS_LIGHT = (150, 186, 60)
MOSS_DARK = (58, 86, 22)
STONE = (126, 126, 116)
STONE_LIGHT = (164, 164, 152)
STONE_DARK = (78, 78, 70)
NIGHT = (8, 18, 14)
OUTLINE = (14, 24, 18)


def _icon(draw, cx=.5, cy=.45, inner=(34, 78, 60)):
    """A painted 16x16 spell icon on a dark green glow."""
    return g.painted(64, draw, (cx, cy, inner, NIGHT), 16)


def _poly(d, S, points, fill, outline=OUTLINE):
    d.polygon([(x * S, y * S) for x, y in points], fill=fill + (255,), outline=outline + (255,) if outline else None)


def _rect(d, S, x0, y0, x1, y1, fill, outline=OUTLINE):
    d.rectangle((x0 * S, y0 * S, x1 * S, y1 * S), fill=fill + (255,), outline=outline + (255,) if outline else None)


def _vine(d, S, x, y0, y1, width=None):
    """A vine hanging from (x, y0) to y1, wavering, with leaves."""
    w = width or max(2, S // 24)
    pts = []
    steps = 8
    for k in range(steps + 1):
        t = k / steps
        pts.append(((x + .025 * math.sin(t * 9)) * S, (y0 + (y1 - y0) * t) * S))
    d.line(pts, fill=MOSS_DARK + (255,), width=w)
    for k in range(1, steps, 2):
        px, py = pts[k]
        side = 1 if k % 4 == 1 else -1
        lx = px + side * S * .03
        d.ellipse((lx - S * .03, py - S * .02, lx + S * .03, py + S * .025), fill=MOSS_LIGHT + (255,))


def _ground(d, S, y=.86):
    _poly(d, S, [(0, y), (1, y - .02), (1, 1), (0, 1)], STONE_DARK, None)
    d.line((0, y * S, S, (y - .02) * S), fill=MOSS + (255,), width=max(2, S // 20))


def copper_spikes_icon():
    """A wedge of copper spikes bursting out of the ground, growing taller to the right."""
    def draw(d, img, S):
        _ground(d, S)
        for x, h, lean in ((.1, .28, -.04), (.3, .42, -.02), (.5, .56, .02), (.72, .7, .05)):
            base = .86
            tip = (x + .08 + lean, base - h)
            _poly(d, S, [(x, base), (x + .16, base), tip], VERDIGRIS_DEEP)
            _poly(d, S, [(x + .04, base - h * .35), (x + .12, base - h * .35), tip], COPPER, None)
            d.line(((x + .02) * S, base * S, tip[0] * S, tip[1] * S), fill=VERDIGRIS_LIGHT + (255,), width=max(1, S // 40))
        for x in (.18, .44, .66):
            d.ellipse(((x - .03) * S, .78 * S, (x + .05) * S, .86 * S), fill=MOSS + (255,))
    return _icon(draw, .6, .5)


def ancient_pillar_icon():
    """A mossy copper pillar bursting up, flinging a figure into the air."""
    def draw(d, img, S):
        _ground(d, S, .9)
        _rect(d, S, .3, .42, .62, .9, VERDIGRIS_DEEP)
        _rect(d, S, .34, .42, .44, .9, VERDIGRIS, None)
        _rect(d, S, .26, .34, .66, .44, COPPER)
        _rect(d, S, .26, .3, .66, .35, MOSS, None)
        _vine(d, S, .58, .44, .74)
        _vine(d, S, .32, .44, .6)
        # The figure thrown up, arms out
        d.ellipse((.42 * S, .05 * S, .52 * S, .15 * S), fill=STONE_LIGHT + (255,))
        d.line((.47 * S, .15 * S, .47 * S, .25 * S), fill=STONE_LIGHT + (255,), width=S // 16)
        d.line((.36 * S, .14 * S, .58 * S, .2 * S), fill=STONE_LIGHT + (255,), width=S // 20)
        for x in (.2, .74):
            d.line((x * S, .3 * S, (x + (.04 if x < .5 else -.04)) * S, .22 * S), fill=VERDIGRIS_LIGHT + (255,), width=S // 32)
    return _icon(draw, .5, .3)


def toppling_wall_icon():
    """A crenellated mossy wall falling forward, a band of copper across it."""
    def draw(d, img, S):
        _ground(d, S, .9)
        cx, cy = .2, .9
        a = math.radians(35)

        def rot(x, y):
            dx, dy = x - cx, y - cy
            return cx + dx * math.cos(a) - dy * math.sin(a), cy + dx * math.sin(a) + dy * math.cos(a)

        _poly(d, S, [rot(.2, .9), rot(.5, .9), rot(.5, .22), rot(.2, .22)], STONE)
        for y0 in (.38, .54, .7):
            p0, p1 = rot(.2, y0), rot(.5, y0)
            d.line((p0[0] * S, p0[1] * S, p1[0] * S, p1[1] * S), fill=STONE_DARK + (255,), width=max(1, S // 40))
        _poly(d, S, [rot(.2, .46), rot(.5, .46), rot(.5, .56), rot(.2, .56)], VERDIGRIS)
        for x0 in (.2, .38):
            _poly(d, S, [rot(x0, .22), rot(x0 + .12, .22), rot(x0 + .12, .14), rot(x0, .14)], STONE)
        _poly(d, S, [rot(.2, .26), rot(.5, .26), rot(.5, .22), rot(.2, .22)], MOSS, None)
        # Motion lines over it and the shadow it falls into
        for k in range(3):
            d.arc(((.3 + k * .08) * S, (.05 + k * .03) * S, (.95 + k * .02) * S, (.75 + k * .03) * S), 280, 330, fill=VERDIGRIS_LIGHT + (255,), width=max(1, S // 40))
        d.ellipse((.55 * S, .84 * S, .98 * S, .94 * S), fill=(20, 30, 24, 255))
    return _icon(draw, .45, .45)


def rust_bolt_icon():
    """A clot of rust flying, flakes trailing behind it."""
    def draw(d, img, S):
        for k in range(6):
            t = k / 6
            x, y = .12 + t * .45, .82 - t * .45
            r = .02 + t * .03
            d.ellipse(((x - r) * S, (y - r) * S, (x + r) * S, (y + r) * S), fill=g.lerp(RUST_DARK, RUST, t) + (255,))
        cx, cy, r = .68, .32, .18
        pts = [(cx + math.cos(k * math.tau / 9) * r * (.8 + .3 * ((k * 7) % 3) / 2), cy + math.sin(k * math.tau / 9) * r * (.8 + .3 * ((k * 5) % 3) / 2)) for k in range(9)]
        _poly(d, S, pts, RUST)
        d.ellipse(((cx - .1) * S, (cy - .09) * S, (cx + .02) * S, (cy + .02) * S), fill=COPPER + (255,))
        d.ellipse(((cx + .03) * S, (cy + .02) * S, (cx + .1) * S, (cy + .09) * S), fill=VERDIGRIS + (255,))
        img.putpixel((int((cx - .07) * S), int((cy - .06) * S)), COPPER_LIGHT + (255,))
    return _icon(draw, .65, .35, (60, 40, 30))


def strangling_vines_icon():
    """Vines coiling up around a figure from a patch of moss."""
    def draw(d, img, S):
        d.ellipse((.12 * S, .78 * S, .88 * S, .96 * S), fill=MOSS_DARK + (255,))
        d.ellipse((.2 * S, .8 * S, .8 * S, .92 * S), fill=MOSS + (255,))
        # The figure
        d.ellipse((.42 * S, .1 * S, .58 * S, .26 * S), fill=STONE_LIGHT + (255,))
        _rect(d, S, .4, .26, .6, .58, STONE_LIGHT, None)
        _rect(d, S, .41, .58, .48, .86, STONE, None)
        _rect(d, S, .52, .58, .59, .86, STONE, None)
        # The vines winding around it
        for k in range(5):
            y = .82 - k * .13
            d.arc((.3 * S, (y - .06) * S, .7 * S, (y + .06) * S), 0, 180, fill=MOSS_DARK + (255,), width=max(2, S // 20))
            d.arc((.3 * S, (y - .06) * S, .7 * S, (y + .06) * S), 200, 340, fill=MOSS_LIGHT + (255,), width=max(1, S // 32))
            d.ellipse((.66 * S, (y - .03) * S, .74 * S, (y + .03) * S), fill=MOSS_LIGHT + (255,))
    return _icon(draw, .5, .5)


def moss_spores_icon():
    """A mound of moss puffing out a cloud of spores."""
    def draw(d, img, S):
        d.ellipse((.06 * S, .62 * S, .94 * S, 1.1 * S), fill=MOSS_DARK + (255,))
        d.ellipse((.12 * S, .64 * S, .88 * S, 1.0 * S), fill=MOSS + (255,))
        for x, y in ((.3, .7), (.55, .68), (.72, .74), (.42, .8)):
            d.ellipse(((x - .05) * S, (y - .04) * S, (x + .05) * S, (y + .04) * S), fill=MOSS_LIGHT + (255,))
        d.ellipse((.44 * S, .58 * S, .56 * S, .68 * S), fill=(232, 120, 190, 255))
        for k in range(11):
            a = k * 2.4
            r = .1 + (k % 4) * .08
            x, y = .5 + math.cos(a) * r, .38 + math.sin(a) * r * .7
            rr = .02 + (k % 3) * .012
            d.ellipse(((x - rr) * S, (y - rr) * S, (x + rr) * S, (y + rr) * S), fill=(VERDIGRIS_LIGHT if k % 2 else (226, 236, 150)) + (255,))
    return _icon(draw, .5, .4)


def colonnade_icon():
    """A ring of columns seen from the side, risen around a glowing centre."""
    def draw(d, img, S):
        d.ellipse((.08 * S, .66 * S, .92 * S, .94 * S), outline=VERDIGRIS + (255,), width=max(2, S // 24))
        for x, h, back in ((.2, .44, True), (.5, .5, True), (.8, .44, True), (.1, .5, False), (.36, .56, False), (.64, .56, False), (.9, .5, False)):
            base = .76 if back else .88
            w = .07 if back else .09
            color = VERDIGRIS_DEEP if back else VERDIGRIS
            _rect(d, S, x - w, base - h, x + w, base, color)
            _rect(d, S, x - w - .02, base - h - .04, x + w + .02, base - h + .02, COPPER if not back else STONE)
        _vine(d, S, .38, .36, .6)
        _vine(d, S, .66, .36, .54)
    return _icon(draw, .5, .7)


def lightning_rod_icon():
    """A tall copper column crowned with a lightning rod, a bolt striking out from it."""
    def draw(d, img, S):
        _ground(d, S, .92)
        _rect(d, S, .38, .5, .56, .92, VERDIGRIS_DEEP)
        for y in (.58, .68, .78):
            d.line((.4 * S, y * S, .54 * S, y * S), fill=NIGHT + (255,), width=max(1, S // 40))
        _rect(d, S, .34, .4, .6, .5, COPPER)
        d.ellipse((.39 * S, .3 * S, .55 * S, .42 * S), fill=(250, 230, 160, 255), outline=OUTLINE + (255,))
        d.line((.47 * S, .08 * S, .47 * S, .3 * S), fill=COPPER_LIGHT + (255,), width=max(2, S // 20))
        bolt = [(.5, .1), (.72, .28), (.64, .32), (.9, .58), (.7, .4), (.78, .36)]
        _poly(d, S, bolt, (220, 250, 240), None)
        _vine(d, S, .4, .5, .76)
    return _icon(draw, .55, .25, (40, 80, 72))


def ancient_bell_icon():
    """A great green bell falling, rings of its toll spreading under it."""
    def draw(d, img, S):
        for k, r in enumerate((.42, .3)):
            d.arc(((.5 - r) * S, (.84 - r * .25) * S, (.5 + r) * S, (.84 + r * .25) * S), 0, 360, fill=(VERDIGRIS if k else VERDIGRIS_LIGHT) + (255,), width=max(1, S // 32))
        bell = [(.38, .12), (.62, .12), (.68, .3), (.72, .52), (.84, .64), (.16, .64), (.28, .52), (.32, .3)]
        _poly(d, S, bell, VERDIGRIS)
        _poly(d, S, [(.4, .14), (.48, .14), (.42, .56), (.32, .56)], VERDIGRIS_LIGHT, None)
        _rect(d, S, .2, .58, .8, .64, VERDIGRIS_DEEP, None)
        d.line((.3 * S, .38 * S, .7 * S, .38 * S), fill=VERDIGRIS_DEEP + (255,), width=max(1, S // 32))
        _rect(d, S, .45, .02, .55, .12, COPPER)
        d.ellipse((.44 * S, .62 * S, .56 * S, .72 * S), fill=COPPER + (255,), outline=OUTLINE + (255,))
        _vine(d, S, .66, .26, .5)
        d.ellipse((.36 * S, .1 * S, .64 * S, .18 * S), fill=MOSS + (255,))
    return _icon(draw, .5, .4)


def sunken_temple_icon():
    """A ruined temple front: a pediment on four copper columns, vines down them, half sunk in the ground."""
    def draw(d, img, S):
        _poly(d, S, [(.06, .3), (.5, .08), (.94, .3)], STONE)
        _poly(d, S, [(.4, .24), (.5, .16), (.6, .24)], VERDIGRIS, None)
        _rect(d, S, .04, .3, .96, .38, STONE_LIGHT)
        d.line((.06 * S, .31 * S, .94 * S, .31 * S), fill=MOSS + (255,), width=max(2, S // 24))
        for x in (.14, .38, .62, .86):
            _rect(d, S, x - .06, .38, x + .06, .86, VERDIGRIS_DEEP)
            _rect(d, S, x - .04, .38, x - .01, .86, VERDIGRIS, None)
        _vine(d, S, .22, .38, .7)
        _vine(d, S, .7, .38, .62)
        _poly(d, S, [(0, .82), (1, .8), (1, 1), (0, 1)], (52, 64, 40), None)
        d.line((0, .82 * S, S, .8 * S), fill=MOSS + (255,), width=max(2, S // 20))
        # Its stones falling in
        for x, y in ((.3, .6), (.7, .5)):
            _poly(d, S, [(x, y), (x + .06, y + .02), (x + .05, y + .07), (x - .01, y + .05)], STONE_LIGHT)
    return _icon(draw, .5, .45, (40, 80, 64))


# ---------------------------------------------------------------- effects

def rusted_effect_icon():
    def draw(d, img, S):
        # A chestplate eaten by rust, flakes falling off
        plate = [(.22, .2), (.4, .14), (.5, .24), (.6, .14), (.78, .2), (.84, .5), (.74, .52), (.72, .86), (.28, .86), (.26, .52), (.16, .5)]
        _poly(d, S, plate, (150, 156, 170))
        for x, y, r in ((.4, .4, .1), (.62, .62, .12), (.36, .72, .07), (.66, .3, .06)):
            d.ellipse(((x - r) * S, (y - r) * S, (x + r) * S, (y + r) * S), fill=RUST + (255,))
            d.ellipse(((x - r * .5) * S, (y - r * .5) * S, (x + r * .3) * S, (y + r * .3) * S), fill=RUST_DARK + (255,))
        for x, y in ((.8, .7), (.86, .82), (.2, .8)):
            d.rectangle((x * S, y * S, (x + .05) * S, (y + .05) * S), fill=COPPER + (255,))
    return g.painted(72, draw, None, 18)


def entangled_effect_icon():
    def draw(d, img, S):
        # Two legs bound by coiling vines
        _rect(d, S, .3, .1, .45, .9, STONE_LIGHT)
        _rect(d, S, .55, .1, .7, .9, STONE_LIGHT)
        for k in range(4):
            y = .3 + k * .17
            d.arc((.2 * S, (y - .07) * S, .8 * S, (y + .07) * S), 10, 170, fill=MOSS_DARK + (255,), width=max(3, S // 14))
            d.arc((.2 * S, (y - .07) * S, .8 * S, (y + .07) * S), 30, 150, fill=MOSS_LIGHT + (255,), width=max(1, S // 30))
            d.ellipse((.74 * S, (y - .05) * S, .86 * S, (y + .03) * S), fill=MOSS + (255,))
    return g.painted(72, draw, None, 18)


# ---------------------------------------------------------------- the Rusted Codex

def rusted_codex_icon():
    """A codex bound in green copper plates, rust at its corners, moss creeping over its spine."""
    img = g.from_grid([
        "................",
        "...oooooooooo...",
        "..ovvvvvvvvvmco.",
        "..ovoooooooomco.",
        "..ovo......oLco.",
        "..ovo......oLco.",
        "..ovo......oLcco",
        "..ovo......oLcco",
        "..ovo......omcco",
        "..ovo......omcco",
        "..ovoooooooomco.",
        "..ovddddddddmco.",
        "..oddddddddrxco.",
        "...ooooooooooo..",
        "................",
        "................",
    ], {"o": OUTLINE, "v": VERDIGRIS, "L": VERDIGRIS_LIGHT, "d": VERDIGRIS_DEEP, "m": MOSS, "r": RUST, "c": g.CREAM, "x": g.CREAM_SHADE})
    for x, y, c in ((5, 5, COPPER), (6, 5, COPPER_LIGHT), (5, 6, COPPER), (6, 6, RUST), (8, 7, COPPER), (8, 8, RUST),
                    (4, 2, RUST), (3, 3, RUST_DARK), (3, 11, MOSS), (4, 11, MOSS_DARK)):
        img.putpixel((x, y), c + (255,))
    return img


def rusted_codex_model_texture():
    """64x64 texture of the 3D book, laid out like the other books: covers of green copper plate riveted at the
    corners, rust creeping in from the edges, a copper sun in a ring of glyphs, moss on the spine, old pages."""
    s = 4
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    def box(u0, v0, u1, v1, fill):
        d.rectangle((u0 * s, v0 * s, u1 * s - 1, v1 * s - 1), fill=fill + (255,))

    W, H = 12 * s, 9 * s
    for y in range(H):
        for x in range(W):
            n = fbm(x / 6, y / 6, 3, 71)
            edge = min(x, y, W - 1 - x, H - 1 - y)
            rust = max(0.0, .55 - edge / 9) + (fbm(x / 3, y / 3, 2, 13) - .5) * .5
            c = g.lerp(VERDIGRIS_DEEP, VERDIGRIS, n * 1.1)
            if rust > .32:
                c = g.lerp(RUST, RUST_DARK, (rust - .32) * 2)
            img.putpixel((x, y), c + (255,))
    cx, cy = W // 2, H // 2
    d.ellipse((cx - 11, cy - 11, cx + 11, cy + 11), outline=VERDIGRIS_LIGHT + (255,))
    d.ellipse((cx - 9, cy - 9, cx + 9, cy + 9), outline=VERDIGRIS_DEEP + (255,))
    for k in range(8):
        a = k * math.tau / 8
        x, y = cx + math.cos(a) * 10, cy + math.sin(a) * 10
        d.rectangle((x - 1, y - 1, x + 1, y + 1), fill=COPPER + (255,))
    d.ellipse((cx - 5, cy - 5, cx + 5, cy + 5), fill=COPPER + (255,), outline=RUST_DARK + (255,))
    d.ellipse((cx - 3, cy - 3, cx + 1, cy + 1), fill=COPPER_LIGHT + (255,))
    d.rectangle((0, 0, W - 1, H - 1), outline=OUTLINE + (255,))
    for x0, y0 in ((2, 2), (W - 4, 2), (2, H - 4), (W - 4, H - 4)):
        d.rectangle((x0, y0, x0 + 1, y0 + 1), fill=COPPER_LIGHT + (255,))
    # Spine: moss over the copper
    box(12, 0, 13, 9, VERDIGRIS_DEEP)
    for y in range(H):
        for x in range(12 * s, 13 * s):
            if fbm(x / 2, y / 3, 2, 5) > .45:
                img.putpixel((x, y), g.lerp(MOSS_DARK, MOSS_LIGHT, fbm(x, y, 1, 3)) + (255,))
    for y in range(0, 8 * s):
        for x in range(13 * s, 16 * s):
            img.putpixel((x, y), g.lerp(g.CREAM, (176, 168, 128), fbm(x / 3, y / 3, 2, 19) * .8) + (255,))
    for y in range(1, 8 * s, 2):
        d.line((13 * s, y, 16 * s - 1, y), fill=g.CREAM_SHADE + (255,))
    for y in range(9 * s, 12 * s):
        for x in range(0, 12 * s):
            t = abs(y - 10.5 * s) / (1.5 * s)
            img.putpixel((x, y), g.lerp(VERDIGRIS, VERDIGRIS_DEEP, t) + (255,))
    for x in range(2, 12 * s - 2, 6):
        d.rectangle((x, 9 * s + 5, x + 1, 9 * s + 6), fill=COPPER + (255,))
    box(0, 12, 12, 15, g.CREAM)
    for x in range(0, 12 * s, 2):
        d.line((x, 12 * s, x, 15 * s - 1), fill=g.CREAM_SHADE + (255,))
    box(12, 9, 14, 11, COPPER)
    d.line((12 * s + 1, 9 * s + 1, 14 * s - 2, 9 * s + 1), fill=COPPER_LIGHT + (255,))
    for y in range(9 * s, 11 * s):
        for x in range(14 * s, 16 * s):
            t = math.hypot(x - 14.8 * s, y - 9.8 * s) / (1.4 * s)
            img.putpixel((x, y), g.lerp(VERDIGRIS_LIGHT, VERDIGRIS_DEEP, min(1, t ** .8)) + (255,))
    box(12, 11, 16, 12, VERDIGRIS_DEEP)
    for x in range(12 * s + 1, 16 * s, 3):
        img.putpixel((x, 11 * s + 1), COPPER_LIGHT + (255,))
        img.putpixel((x, 12 * s - 2), COPPER_LIGHT + (255,))
    return img


def affinity_ring_verdigris():
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
    ], {"o": OUTLINE, "h": COPPER_LIGHT, "l": COPPER, "b": (176, 96, 52), "s": RUST,
        "R": VERDIGRIS_DEEP, "C": VERDIGRIS, "L": VERDIGRIS_LIGHT})


ICONS = (("copper_spikes", copper_spikes_icon), ("ancient_pillar", ancient_pillar_icon), ("toppling_wall", toppling_wall_icon),
         ("rust_bolt", rust_bolt_icon), ("strangling_vines", strangling_vines_icon), ("moss_spores", moss_spores_icon),
         ("colonnade", colonnade_icon), ("ancient_lightning_rod", lightning_rod_icon), ("ancient_bell", ancient_bell_icon),
         ("sunken_temple", sunken_temple_icon))


def main():
    for name, icon in ICONS:
        g.save(icon(), f"textures/gui/spell_icons/{name}.png")
    g.save(rusted_effect_icon(), "textures/mob_effect/rusted.png")
    g.save(entangled_effect_icon(), "textures/mob_effect/entangled.png")
    g.save(affinity_ring_verdigris(), "textures/item/affinity_ring_verdigris.png")
    g.save(rusted_codex_icon(), "textures/item/rusted_codex.png")
    g.save(rusted_codex_model_texture(), "textures/item/rusted_codex_3d.png")
    g.write_json("models/item/affinity_ring_verdigris.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/affinity_ring_verdigris"}})
    g.book_models("rusted_codex")


if __name__ == "__main__":
    main()
