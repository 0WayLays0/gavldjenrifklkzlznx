#!/usr/bin/env python3
"""Clockwork Magic art: the metals of the machines (brushed brass, polished copper, blued steel, a riveted brass plate,
red enamel), the blue energy of their cores, the oiled canvas of the airship, the sleepers of the rails, the spell icons,
the Clockmaker's Codex (icon, a finer 3D book texture and raised ornaments: meshing gears with a core of blue light on
each cover, a winding key on the spine) and its affinity ring.

Called by gen_assets.py (run that one). Original art, drawn here from noise and strokes.
"""
import math
import random

import numpy as np
from PIL import Image, ImageDraw

import book_ornaments as bo
import gen_assets as g
from water_assets import _tnoise, _rgba, _ramp

BRASS = (222, 170, 80)
BRASS_LIGHT = (255, 228, 152)
BRASS_DARK = (118, 76, 30)
COPPER = (206, 112, 70)
COPPER_LIGHT = (252, 178, 132)
COPPER_DARK = (98, 44, 26)
STEEL = (112, 120, 134)
STEEL_LIGHT = (186, 196, 210)
STEEL_DARK = (38, 42, 52)
CORE = (90, 204, 255)
CORE_WHITE = (226, 250, 255)
CORE_DEEP = (22, 72, 206)
EMBER = (255, 150, 60)
UMBER = (30, 20, 13)
LEATHER = (72, 30, 24)
LEATHER_DARK = (40, 16, 12)


# ------------------------------------------------------------------------------------------------- the metals

def _brushed(seed, stops, S=64):
    """S x S, tiling: metal brushed along U, with broad soft sheen and fine streaks."""
    broad = _tnoise(S, S, (4, 4), seed)
    streak = _tnoise(S, S, (2, 32), seed + 1)
    fine = _tnoise(S, S, (4, 64), seed + 2)
    t = np.clip(.18 + .55 * broad + .3 * (streak - .5) + .15 * (fine - .5), 0, 1)
    return _ramp(t, stops)


def brass_texture():
    return _rgba(_brushed(1801, [BRASS_DARK, (176, 120, 46), BRASS, (242, 202, 112), BRASS_LIGHT]), np.full((64, 64), 255.0))


def copper_texture():
    return _rgba(_brushed(1811, [COPPER_DARK, (150, 70, 40), COPPER, (236, 150, 100), COPPER_LIGHT]), np.full((64, 64), 255.0))


def steel_texture():
    return _rgba(_brushed(1821, [STEEL_DARK, (66, 74, 90), STEEL, (150, 160, 178), STEEL_LIGHT]), np.full((64, 64), 255.0))


def plate_texture():
    """64 x 64, tiling: a plate of brass, a groove round its edge and a row of round rivets inside it."""
    rgb = _brushed(1831, [BRASS_DARK, (170, 116, 44), BRASS, (236, 196, 108), BRASS_LIGHT])
    img = _rgba(rgb, np.full((64, 64), 255.0))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 63, 63), outline=(84, 52, 20, 255))
    d.rectangle((1, 1, 62, 62), outline=(250, 220, 150, 255))
    for k in range(4, 64, 10):
        for (x, y) in ((k, 5), (k, 58), (5, k), (58, k)):
            d.ellipse((x - 2, y - 2, x + 2, y + 2), fill=(150, 100, 40, 255))
            d.ellipse((x - 2, y - 2, x + 1, y + 1), fill=(240, 205, 120, 255))
            d.point((x - 1, y - 1), fill=(255, 245, 210, 255))
    return img


def enamel_texture():
    """64 x 64, tiling: deep red enamel, glossy (tinted in game for other colours), a fine gold line along it."""
    S = 64
    broad = _tnoise(S, S, (3, 3), 1841)
    rgb = _ramp(np.clip(.25 + .6 * broad, 0, 1), [(70, 8, 14), (128, 20, 28), (176, 36, 40), (214, 90, 84)])
    img = _rgba(rgb, np.full((S, S), 255.0))
    d = ImageDraw.Draw(img)
    d.line((0, 8, 63, 8), fill=BRASS_LIGHT + (255,))
    d.line((0, 56, 63, 56), fill=BRASS + (255,))
    return img


def core_texture():
    """64 x 64, tiling: blue energy in swirling bands, white at its brightest, filaments of light through it."""
    S = 64
    ys, xs = np.mgrid[0:S, 0:S].astype(np.float32)
    warp = _tnoise(S, S, (4, 4), 1851)
    bands = .5 + .5 * np.sin((ys / S * 3 + xs / S + warp * 1.6) * math.tau)
    fine = _tnoise(S, S, (16, 8), 1852)
    t = np.clip(.15 + .55 * bands * (.6 + .4 * fine) + .3 * np.clip((fine - .7) * 4, 0, 1), 0, 1)
    rgb = _ramp(t, [(4, 16, 60), CORE_DEEP, CORE, CORE_WHITE])
    return _rgba(rgb, np.full((S, S), 255.0))


def canvas_texture():
    """64 x 64, tiling: oiled canvas in long gores (U round the envelope), a stitched seam between each."""
    S = 64
    weave = _tnoise(S, S, (32, 32), 1861) * .5 + _tnoise(S, S, (6, 6), 1862) * .5
    rgb = _ramp(np.clip(.3 + .5 * weave, 0, 1), [(150, 126, 90), (196, 174, 130), (224, 206, 162), (240, 228, 194)])
    img = _rgba(rgb, np.full((S, S), 255.0))
    d = ImageDraw.Draw(img)
    for x in (0, 32):
        d.line((x, 0, x, 63), fill=(110, 86, 56, 255))
        d.line((x + 1, 0, x + 1, 63), fill=(250, 238, 206, 255))
        for y in range(2, 64, 6):
            d.point((x + 3, y), fill=(120, 92, 60, 255))
    return img


def sleeper_texture():
    """32 x 32, tiling: a dark creosoted sleeper, its grain along U."""
    S = 32
    grain = _tnoise(S, S, (2, 16), 1871)
    knots = _tnoise(S, S, (6, 6), 1872)
    rgb = _ramp(np.clip(.2 + .5 * grain + .3 * (knots - .5), 0, 1), [(26, 16, 10), (52, 34, 22), (82, 56, 36), (110, 80, 54)])
    return _rgba(rgb, np.full((S, S), 255.0))


# ------------------------------------------------------------------------------------------------------- icons

def _icon(draw, cx=.5, cy=.5, bg=(58, 40, 26)):
    return g.painted(64, draw, (cx, cy, bg, UMBER), 16)


def _a(c, a=255):
    return tuple(int(x) for x in c) + (a,)


def _gear_poly(cx, cy, r, teeth, angle, S, depth=.22):
    pts = []
    for t in range(teeth):
        a = angle + t * math.tau / teeth
        w = math.tau / teeth
        for da, rr in ((0, r * (1 - depth)), (w * .15, r), (w * .38, r), (w * .52, r * (1 - depth))):
            pts.append(((cx + math.cos(a + da) * rr) * S, (cy + math.sin(a + da) * rr) * S))
    return pts


def _gear(d, S, cx, cy, r, teeth, color, dark, angle=0.0, spokes=4, hole=(36, 24, 16)):
    """A gear: toothed rim, its spokes, a hub with a blue core."""
    d.polygon(_gear_poly(cx, cy, r, teeth, angle, S), fill=_a(color), outline=_a(dark))
    ri = r * .62
    d.ellipse(((cx - ri) * S, (cy - ri) * S, (cx + ri) * S, (cy + ri) * S), fill=_a(hole), outline=_a(dark))
    lw = max(2, int(S * r * .16))
    for k in range(spokes):
        a = angle + k * math.tau / spokes
        d.line(((cx - math.cos(a) * ri) * S, (cy - math.sin(a) * ri) * S, (cx + math.cos(a) * ri) * S, (cy + math.sin(a) * ri) * S),
               fill=_a(color), width=lw)
    rh = r * .24
    d.ellipse(((cx - rh) * S, (cy - rh) * S, (cx + rh) * S, (cy + rh) * S), fill=_a(color), outline=_a(dark))
    rc = r * .12
    d.ellipse(((cx - rc) * S, (cy - rc) * S, (cx + rc) * S, (cy + rc) * S), fill=_a(CORE))


def _steam(img, S, x, y, r, strength=.6):
    g.glow_spot(img, x * S, y * S, r * S, (210, 210, 210), strength)


def clockwork_sawblade_icon():
    def draw(d, img, S):
        for k in range(4):
            y = .3 + k * .12
            d.line((.06 * S, y * S, (.3 - k * .03) * S, (y + .08) * S), fill=_a((200, 200, 210), 120), width=2)
        d.polygon(_gear_poly(.56, .52, .36, 14, .2, S, .3), fill=_a(STEEL_LIGHT), outline=_a(STEEL_DARK))
        d.ellipse((.36 * S, .32 * S, .76 * S, .72 * S), fill=_a(STEEL))
        d.ellipse((.44 * S, .4 * S, .68 * S, .64 * S), fill=_a(BRASS), outline=_a(BRASS_DARK))
        d.ellipse((.52 * S, .48 * S, .6 * S, .56 * S), fill=_a(CORE))
        for k in range(6):
            a = -1.2 + k * .25
            g.glow_spot(img, (.56 + math.cos(a) * .42) * S, (.52 + math.sin(a) * .42) * S, .05 * S, EMBER, .9)
    return _icon(draw)


def rivet_volley_icon():
    def draw(d, img, S):
        for k, (x, y) in enumerate(((.62, .3), (.72, .52), (.56, .74))):
            ang = -.5
            tail = (x - .4, y + .22)
            _steam(img, S, (x + tail[0]) / 2, (y + tail[1]) / 2, .08, .35)
            d.line((tail[0] * S, tail[1] * S, x * S, y * S), fill=_a((230, 230, 230), 140), width=3)
            d.polygon([((x - .1) * S, (y + .05) * S), ((x - .02) * S, (y - .03) * S), ((x + .04) * S, (y + .03) * S), ((x - .04) * S, (y + .11) * S)],
                      fill=_a(COPPER), outline=_a(COPPER_DARK))
            d.ellipse(((x - .02) * S, (y - .06) * S, (x + .09) * S, (y + .05) * S), fill=_a(EMBER), outline=_a(COPPER_DARK))
            g.glow_spot(img, (x + .04) * S, y * S, .1 * S, EMBER, .8)
    return _icon(draw)


def mainspring_leap_icon():
    def draw(d, img, S):
        for k in range(6):
            y = .82 - k * .085
            d.ellipse((.28 * S, (y - .05) * S, .72 * S, (y + .05) * S), outline=_a(BRASS_LIGHT if k % 2 else BRASS), width=3)
        d.rectangle((.24 * S, .84 * S, .76 * S, .9 * S), fill=_a(BRASS_DARK))
        d.rectangle((.3 * S, .3 * S, .7 * S, .35 * S), fill=_a(BRASS))
        d.polygon([(.5 * S, .06 * S), (.64 * S, .22 * S), (.36 * S, .22 * S)], fill=_a(CORE_WHITE))
        g.glow_spot(img, .5 * S, .16 * S, .14 * S, CORE, .8)
    return _icon(draw)


def gear_trap_icon():
    def draw(d, img, S):
        d.ellipse((.1 * S, .62 * S, .9 * S, .9 * S), fill=_a((60, 44, 30)), outline=_a(BRASS_DARK))
        for side in (-1, 1):
            jaw = []
            for k in range(13):
                a = math.pi * (k / 12)
                r = .32 if k % 2 == 0 else .25
                jaw.append(((.5 + side * (.04 + math.sin(a) * r * .55)) * S, (.76 - math.cos(a) * r * 1.3 + .02) * S))
            d.polygon(jaw, fill=_a(STEEL_LIGHT if side < 0 else STEEL), outline=_a(STEEL_DARK))
        _gear(d, S, .5, .8, .1, 10, BRASS, BRASS_DARK, .2, 3)
    return _icon(draw)


def clockwork_turret_icon():
    def draw(d, img, S):
        for x in (.22, .5, .78):
            d.line((.5 * S, .6 * S, x * S, .92 * S), fill=_a(BRASS_DARK), width=4)
        d.ellipse((.3 * S, .42 * S, .7 * S, .72 * S), fill=_a(BRASS), outline=_a(BRASS_DARK))
        d.ellipse((.43 * S, .5 * S, .57 * S, .64 * S), fill=_a(CORE))
        g.glow_spot(img, .5 * S, .57 * S, .12 * S, CORE, .7)
        for k in range(3):
            y = .38 + k * .05
            d.line((.45 * S, y * S, .86 * S, (y - .16) * S), fill=_a(STEEL_LIGHT if k == 1 else STEEL), width=4)
        g.glow_spot(img, .9 * S, .22 * S, .14 * S, EMBER, 1)
    return _icon(draw)


def gyro_cage_icon():
    def draw(d, img, S):
        g.glow_spot(img, .5 * S, .5 * S, .3 * S, CORE, .9)
        d.ellipse((.12 * S, .12 * S, .88 * S, .88 * S), outline=_a(BRASS), width=4)
        d.ellipse((.12 * S, .34 * S, .88 * S, .66 * S), outline=_a(BRASS_LIGHT), width=3)
        d.ellipse((.34 * S, .12 * S, .66 * S, .88 * S), outline=_a(COPPER), width=3)
        d.ellipse((.42 * S, .42 * S, .58 * S, .58 * S), fill=_a(CORE_WHITE))
    return _icon(draw)


def brass_automaton_icon():
    def draw(d, img, S):
        d.rounded_rectangle((.22 * S, .46 * S, .78 * S, .96 * S), radius=int(.08 * S), fill=_a(BRASS), outline=_a(BRASS_DARK))
        d.ellipse((.4 * S, .6 * S, .6 * S, .8 * S), fill=_a(CORE), outline=_a(BRASS_DARK))
        g.glow_spot(img, .5 * S, .7 * S, .14 * S, CORE, .8)
        d.ellipse((.3 * S, .1 * S, .7 * S, .5 * S), fill=_a(COPPER), outline=_a(COPPER_DARK))
        d.rectangle((.36 * S, .27 * S, .64 * S, .33 * S), fill=_a(CORE_WHITE))
        g.glow_spot(img, .5 * S, .3 * S, .14 * S, CORE, .7)
        d.line((.78 * S, .62 * S, .9 * S, .62 * S), fill=_a(BRASS_DARK), width=3)
        d.ellipse((.86 * S, .5 * S, .96 * S, .74 * S), outline=_a(BRASS_LIGHT), width=3)
    return _icon(draw)


def brass_locomotive_icon():
    def draw(d, img, S):
        _steam(img, S, .32, .14, .16, .7)
        _steam(img, S, .2, .08, .12, .5)
        d.rectangle((.26 * S, .16 * S, .4 * S, .34 * S), fill=_a(STEEL_DARK))
        d.ellipse((.16 * S, .26 * S, .84 * S, .82 * S), fill=_a((150, 26, 32)), outline=_a(BRASS))
        d.ellipse((.28 * S, .38 * S, .72 * S, .74 * S), fill=_a(BRASS), outline=_a(BRASS_DARK))
        d.ellipse((.4 * S, .48 * S, .6 * S, .66 * S), fill=_a(CORE_WHITE))
        g.glow_spot(img, .5 * S, .57 * S, .2 * S, CORE, .9)
        d.polygon([(.18 * S, .82 * S), (.82 * S, .82 * S), (.66 * S, .98 * S), (.34 * S, .98 * S)], fill=_a(STEEL), outline=_a(STEEL_DARK))
        for x in range(6):
            d.line(((.24 + x * .1) * S, .83 * S, (.38 + x * .05) * S, .97 * S), fill=_a(STEEL_DARK), width=1)
    return _icon(draw)


def airship_bombardment_icon():
    def draw(d, img, S):
        d.ellipse((.08 * S, .08 * S, .86 * S, .42 * S), fill=_a((214, 196, 156)), outline=_a(BRASS_DARK))
        for x in (.27, .47, .67):
            d.line((x * S, .1 * S, x * S, .4 * S), fill=_a(BRASS), width=2)
        d.rectangle((.34 * S, .44 * S, .6 * S, .52 * S), fill=_a(BRASS), outline=_a(BRASS_DARK))
        d.polygon([(.86 * S, .25 * S), (.96 * S, .14 * S), (.96 * S, .36 * S)], fill=_a(COPPER))
        for x, y in ((.36, .64), (.56, .74)):
            d.ellipse(((x - .04) * S, (y - .04) * S, (x + .04) * S, (y + .04) * S), fill=_a(STEEL_DARK))
            g.glow_spot(img, x * S, (y - .05) * S, .03 * S, EMBER, 1)
        g.glow_spot(img, .7 * S, .92 * S, .18 * S, EMBER, 1)
        g.glow_spot(img, .7 * S, .92 * S, .08 * S, (255, 240, 200), 1)
    return _icon(draw)


def great_mechanism_icon():
    def draw(d, img, S):
        _gear(d, S, .5, .52, .42, 18, BRASS, BRASS_DARK, .1, 6)
        _gear(d, S, .16, .2, .16, 10, COPPER, COPPER_DARK, .3, 3)
        _gear(d, S, .86, .86, .14, 10, COPPER, COPPER_DARK, .5, 3)
        g.glow_spot(img, .5 * S, .52 * S, .22 * S, CORE, 1)
        for k in range(5):
            a = k * 1.25 + .4
            x0, y0 = .5 + math.cos(a) * .06, .52 + math.sin(a) * .06
            x1, y1 = .5 + math.cos(a + .3) * .3, .52 + math.sin(a + .3) * .3
            xm, ym = (x0 + x1) / 2 + .04 * math.sin(a * 3), (y0 + y1) / 2 + .04 * math.cos(a * 2)
            d.line((x0 * S, y0 * S, xm * S, ym * S, x1 * S, y1 * S), fill=_a(CORE_WHITE), width=2)
    return _icon(draw)


ICONS = (("clockwork_sawblade", clockwork_sawblade_icon), ("rivet_volley", rivet_volley_icon), ("mainspring_leap", mainspring_leap_icon),
         ("gear_trap", gear_trap_icon), ("clockwork_turret", clockwork_turret_icon), ("gyro_cage", gyro_cage_icon),
         ("brass_automaton", brass_automaton_icon), ("brass_locomotive", brass_locomotive_icon), ("airship_bombardment", airship_bombardment_icon),
         ("great_mechanism", great_mechanism_icon))


# --------------------------------------------------------------------------------------------------- the spellbook

def clockmakers_codex_icon():
    return g.from_grid([
        "................",
        "...oooooooooo...",
        "..olllllllllbco.",
        "..olbBBlBBlbbco.",
        "..olBYyYyBlbbcco",
        "..olBy..yBlbbco.",
        "..olByCCyBlbbcco",
        "..olBy..yBlbbcco",
        "..olBYyYyBlbbco.",
        "..olbBBlBBlbbcco",
        "..ollllllllbbco.",
        "..oddydddyddxco.",
        "..odddddddddxcco",
        "...ooooooooooo..",
        "................",
        "................",
    ], {"o": UMBER, "l": LEATHER, "b": BRASS_DARK, "B": BRASS, "Y": BRASS_LIGHT, "y": (200, 150, 70), "C": CORE, "d": LEATHER_DARK,
        "x": (24, 10, 8), "c": g.CREAM})


def clockmakers_codex_model_texture():
    """128 x 128 texture of the 3D book (the grimoire layout, 8 pixels a unit): covers of oxblood leather tooled with a
    clock's face and its gears in brass, a brass frame with rivets and gears at its corners; a spine banded in brass;
    old pages; the ornaments' materials (bright brass, copper, the blue core, its glow)."""
    s = 8
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    W, H = 12 * s, 9 * s
    grain = _tnoise(W, H, (16, 12), 1881) * .6 + _tnoise(W, H, (48, 36), 1882) * .4
    ys, xs = np.mgrid[0:H, 0:W].astype(np.float32)
    vign = np.clip(1 - (((xs - W / 2) / (W / 2)) ** 2 + ((ys - H / 2) / (H / 2)) ** 2) * .35, 0, 1)
    cover = _ramp(np.clip((.15 + .6 * grain) * (.55 + .45 * vign), 0, 1), [LEATHER_DARK, LEATHER, (110, 46, 32), (140, 64, 44)])
    img.paste(_rgba(cover, np.full((H, W), 255)), (0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = W * .5, H * .5
    # A clock's face tooled in brass: rings, the hours, the minutes
    for r, col in ((30, BRASS_DARK), (29, BRASS), (24, BRASS_DARK), (13, BRASS)):
        d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=col + (255,))
    for k in range(60):
        a = k * math.tau / 60
        r0 = 25 if k % 5 else 22
        d.line((cx + math.cos(a) * r0, cy + math.sin(a) * r0, cx + math.cos(a) * 28, cy + math.sin(a) * 28),
               fill=(BRASS_LIGHT if k % 5 == 0 else BRASS) + (255,))
    # Gears tooled round it, at its sides
    for gx, gy, gr, n in ((14, 14, 9, 10), (W - 14, H - 14, 9, 10), (W - 13, 13, 6, 8), (13, H - 13, 6, 8)):
        d.polygon(_gear_poly(gx / W, gy / H, gr / W, n, .3, W, .3), outline=BRASS + (255,))
        d.ellipse((gx - gr * .45, gy - gr * .45, gx + gr * .45, gy + gr * .45), outline=BRASS_DARK + (255,))
    # The clock's hands, under the raised gear
    d.line((cx, cy, cx + 10, cy - 16), fill=BRASS_LIGHT + (255,), width=2)
    d.line((cx, cy, cx - 18, cy + 4), fill=BRASS + (255,), width=2)
    # A brass frame with rivets
    d.rectangle((1, 1, W - 2, H - 2), outline=BRASS + (255,))
    d.rectangle((2, 2, W - 3, H - 3), outline=BRASS_LIGHT + (255,))
    d.rectangle((4, 4, W - 5, H - 5), outline=BRASS_DARK + (255,))
    for x in range(8, W - 6, 8):
        for y in (3, H - 4):
            d.point((x, y), fill=(255, 245, 210, 255))
    for y in range(8, H - 6, 8):
        for x in (3, W - 4):
            d.point((x, y), fill=(255, 245, 210, 255))
    d.rectangle((0, 0, W - 1, H - 1), outline=UMBER + (255,))
    # The cover's edge: brass
    for yy in range(0, 9 * s):
        for xx in range(12 * s, 13 * s):
            t = .5 + .5 * math.sin(yy * .5)
            img.putpixel((xx, yy), g.lerp(BRASS_DARK, BRASS, t) + (255,))
    # The page edges
    for yy in range(0, 8 * s):
        for xx in range(13 * s, 16 * s):
            img.putpixel((xx, yy), g.lerp(g.CREAM, (176, 168, 140), ((xx * 7 + yy * 13) % 11) / 22) + (255,))
    for yy in range(1, 8 * s, 2):
        d.line((13 * s, yy, 16 * s - 1, yy), fill=g.CREAM_SHADE + (255,))
    # The spine: leather between bands of brass, a line of blue light down its middle
    spine = _ramp(np.clip(.2 + .6 * _tnoise(12 * s, 3 * s, (12, 4), 1883), 0, 1), [LEATHER_DARK, LEATHER, (110, 46, 32)])
    img.paste(_rgba(spine, np.full((3 * s, 12 * s), 255)), (0, 9 * s))
    for xx in range(0, 12 * s):
        if (xx // 6) % 4 == 0:
            for yy in range(9 * s, 12 * s):
                img.putpixel((xx, yy), g.lerp(BRASS_DARK, BRASS_LIGHT, .5 + .5 * math.sin(yy * .6)) + (255,))
        for yy in range(int(10.35 * s), int(10.65 * s)):
            img.putpixel((xx, yy), CORE + (255,))
    d.rectangle((0, 12 * s, 12 * s - 1, 15 * s - 1), fill=g.CREAM + (255,))
    for xx in range(0, 12 * s, 2):
        d.line((xx, 12 * s, xx, 15 * s - 1), fill=g.CREAM_SHADE + (255,))
    # The plain book's metal (corners, bands), gem (headbands, bezel) and strap
    _metal(img, 12 * s, 9 * s, 2 * s, 2 * s, BRASS_LIGHT, BRASS_DARK)
    _core(img, 14 * s, 9 * s, 2 * s, 2 * s)
    d.rectangle((12 * s, 11 * s, 16 * s - 1, 12 * s - 1), fill=LEATHER_DARK + (255,))
    d.line((12 * s, 11 * s + 3, 16 * s - 1, 11 * s + 3), fill=BRASS + (255,))
    # The ornaments' materials
    _metal(img, 12 * s, 12 * s, 2 * s, 2 * s, BRASS_LIGHT, BRASS_DARK)
    _metal(img, 14 * s, 12 * s, 2 * s, 2 * s, COPPER_LIGHT, COPPER_DARK)
    _core(img, 12 * s, 14 * s, 2 * s, 2 * s)
    for yy in range(14 * s, 16 * s):
        for xx in range(14 * s, 16 * s):
            t = math.hypot(xx - 15 * s, yy - 15 * s) / (1.2 * s)
            img.putpixel((xx, yy), g.lerp(CORE_WHITE, CORE, min(1, t)) + (255,))
    return img


def _metal(img, x0, y0, w, h, light, dark):
    for yy in range(y0, y0 + h):
        for xx in range(x0, x0 + w):
            t = .5 + .5 * math.sin((xx - x0) * .5 + (yy - y0) * .2)
            img.putpixel((xx, yy), g.lerp(dark, light, t * .85 + .1) + (255,))


def _core(img, x0, y0, w, h):
    for yy in range(y0, y0 + h):
        for xx in range(x0, x0 + w):
            u, v = (xx - x0) / w, (yy - y0) / h
            t = abs(u - .5) + abs(v - .5)
            img.putpixel((xx, yy), g.lerp(CORE_WHITE, CORE_DEEP, min(1, t * 1.3)) + (255,))


def _gear_plates(front, cy, cz, r, depth, teeth_region, hub_region):
    """A gear raised on a cover: four square plates turned an eighth of a right angle apart (its sixteen teeth), a hub
    turned between them, the blue core at its heart. Each plate stands a little prouder than the one before."""
    e = []
    h = r / math.sqrt(2)
    for k, angle in enumerate((0, 22.5, 45, -22.5)):
        x0, x1 = bo.on_cover(front, depth, depth + .1 + k * .025)
        e.append(bo.turned(front, x0, x1, cy, cz, h, h, teeth_region, angle))
    top = depth + .1 + 3 * .025
    x0, x1 = bo.on_cover(front, top, top + .06)
    e.append(bo.turned(front, x0, x1, cy, cz, r * .5, r * .5, hub_region, 22.5))
    x0, x1 = bo.on_cover(front, top + .06, top + .14)
    e.append(bo.turned(front, x0, x1, cy, cz, r * .26, r * .26, bo.JEWEL, 45))
    x0, x1 = bo.on_cover(front, top + .14, top + .2)
    e.append(bo.turned(front, x0, x1, cy, cz, r * .12, r * .12, bo.GLOW, 0))
    return e


def clockmakers_codex_ornaments():
    """On each cover: a great gear over the clock's face meshing with two smaller ones, brass brackets at the corners;
    on the spine, a winding key; brass caps at both its ends."""
    e = []
    for front in (True, False):
        e += _gear_plates(front, 3.8, 8.0, 2.05, 0, bo.METAL, bo.METAL_DARK)
        e += _gear_plates(front, 5.75, 4.75, 1.1, 0, bo.METAL_DARK, bo.METAL)
        e += _gear_plates(front, 1.45, 11.0, .95, 0, bo.METAL_DARK, bo.METAL)
        for sy, sz in ((.55, 3.55), (.55, 12.45), (7.05, 3.55), (7.05, 12.45)):
            x0, x1 = bo.on_cover(front, 0, .1)
            e.append(bo.turned(front, x0, x1, sy, sz, .32, .32, bo.METAL, 45))
    # The winding key on the spine: its shaft, a collar, its bow of two lobes
    h = bo.H
    e.append(bo.solid([7.8, h + .3, 7.8], [8.2, h + 1.0, 8.2], bo.METAL_DARK))
    e.append(bo.solid([7.65, h + .5, 7.65], [8.35, h + .65, 8.35], bo.METAL))
    e.append(bo.solid([7.88, h + .95, 7.25], [8.12, h + 1.35, 8.75], bo.METAL))
    for z in (7.15, 8.85):
        e.append(bo.solid([7.86, h + .78, z - .42], [8.14, h + 1.62, z + .42], bo.METAL,
                          {"origin": [8, h + 1.2, z], "axis": "x", "angle": 45}))
        e.append(bo.solid([7.84, h + 1.05, z - .14], [8.16, h + 1.35, z + .14], bo.GLOW))
    # Brass caps at both ends of the spine
    for z in (bo.Z0 - .2, bo.Z1 + .2):
        e.append(bo.solid([7.5, h - .4, z - .3], [8.5, h + .5, z + .3], bo.METAL))
        e.append(bo.solid([7.75, h - .15, z - .36], [8.25, h + .25, z + .36], bo.JEWEL))
    return e


def affinity_ring_clockwork():
    return g.from_grid([
        "................",
        "......oooo......",
        ".....oYBBYo.....",
        ".....oBCCBo.....",
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
    ], {"o": UMBER, "h": BRASS_LIGHT, "l": BRASS, "b": (190, 140, 60), "s": BRASS_DARK, "v": CORE, "Y": BRASS_LIGHT, "B": BRASS, "C": CORE})


def main():
    g.save(brass_texture(), "textures/entity/clockwork/brass.png")
    g.save(copper_texture(), "textures/entity/clockwork/copper.png")
    g.save(steel_texture(), "textures/entity/clockwork/steel.png")
    g.save(plate_texture(), "textures/entity/clockwork/plate.png")
    g.save(enamel_texture(), "textures/entity/clockwork/enamel.png")
    g.save(core_texture(), "textures/entity/clockwork/core.png")
    g.save(canvas_texture(), "textures/entity/clockwork/canvas.png")
    g.save(sleeper_texture(), "textures/entity/clockwork/sleeper.png")
    for name, icon in ICONS:
        g.save(icon(), f"textures/gui/spell_icons/{name}.png")
    g.save(affinity_ring_clockwork(), "textures/item/affinity_ring_clockwork.png")
    g.write_json("models/item/affinity_ring_clockwork.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/affinity_ring_clockwork"}})
    g.save(clockmakers_codex_icon(), "textures/item/clockmakers_codex.png")
    g.save(clockmakers_codex_model_texture(), "textures/item/clockmakers_codex_3d.png")
    g.book_models("clockmakers_codex")
    bo.add("clockmakers_codex", clockwork_ornaments_checked())


def clockwork_ornaments_checked():
    e = clockmakers_codex_ornaments()
    for el in e:
        rot = el.get("rotation")
        assert rot is None or rot["angle"] in (-45, -22.5, 0, 22.5, 45), rot
    return e


if __name__ == "__main__":
    main()
