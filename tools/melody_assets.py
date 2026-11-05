#!/usr/bin/env python3
"""Music Magic art: the staff of light its notes ride on, the varnished wood of its violins, the skin of its drums, the
spell icons, the Symphony Codex (icon, a finer 3D book texture and raised ornaments: a golden lyre on each cover, notes
around it, pegs and strings on the spine) and its affinity ring.

Called by gen_assets.py (run that one). Original art, drawn here.
"""
import math

import numpy as np
from PIL import Image, ImageDraw

import book_ornaments as bo
import gen_assets as g
from water_assets import _tnoise, _rgba, _ramp

GOLD = (255, 211, 106)
GOLD_LIGHT = (255, 240, 190)
GOLD_DARK = (150, 100, 30)
ROSE = (255, 111, 216)
VIOLET = (167, 123, 255)
PLUM = (58, 18, 64)
PLUM_DARK = (28, 8, 34)
NIGHT = (16, 8, 30)
WHITE = (255, 255, 255)


def staff_texture():
    """128 x 32, tiling along: the five lines of a staff of light (white, tinted in game), soft at their edges."""
    W, H = 128, 32
    ys = (np.arange(H) + .5)[:, None] * np.ones((1, W))
    v = np.zeros((H, W))
    for k in range(5):
        y0 = 4 + k * 6
        v += np.exp(-((ys - y0) / .9) ** 2) + .35 * np.exp(-((ys - y0) / 2.4) ** 2)
    shimmer = .8 + .2 * _tnoise(W, H, (8, 2), 2101)
    v = np.clip(v * shimmer, 0, 1)
    return _rgba(np.dstack([v * 255] * 3), np.full((H, W), 255.0))


def varnish_texture():
    """64 x 64, tiling: the varnished spruce and maple of a violin, its grain along U, a warm red-brown sheen."""
    S = 64
    grain = _tnoise(S, S, (2, 24), 2111)
    flame = .5 + .5 * np.sin((np.arange(S)[None, :] / S * 6 + _tnoise(S, S, (4, 4), 2112) * 2) * math.tau)
    t = np.clip(.25 + .45 * grain + .2 * flame, 0, 1)
    rgb = _ramp(t, [(70, 22, 10), (128, 52, 18), (176, 86, 30), (214, 132, 58)])
    return _rgba(rgb, np.full((S, S), 255.0))


def drumhead_texture():
    """64 x 64: a drum's skin, cream, a little mottled, darker toward its rim (laid over a disc)."""
    S = 64
    ys, xs = np.mgrid[0:S, 0:S].astype(np.float32)
    r = np.hypot(xs - S / 2 + .5, ys - S / 2 + .5) / (S / 2)
    mottle = _tnoise(S, S, (6, 6), 2121)
    t = np.clip(.75 - .35 * r ** 3 + .15 * (mottle - .5), 0, 1)
    rgb = _ramp(t, [(140, 110, 70), (210, 186, 140), (240, 224, 190)])
    return _rgba(rgb, np.full((S, S), 255.0))


# ------------------------------------------------------------------------------------------------------- icons

def _icon(draw, bg=(80, 26, 90)):
    return g.painted(64, draw, (.5, .5, bg, NIGHT), 16)


def _a(c, a=255):
    return tuple(int(x) for x in c) + (a,)


def _note(d, S, x, y, size, c, flag=True, angle=0.0):
    """An eighth note: its head at (x, y)."""
    d.ellipse(((x - size * .6) * S, (y - size * .42) * S, (x + size * .6) * S, (y + size * .42) * S), fill=_a(c))
    d.line(((x + size * .52) * S, y * S, (x + size * .52) * S, (y - size * 2.4) * S), fill=_a(c), width=max(2, int(S * size * .2)))
    if flag:
        d.line(((x + size * .52) * S, (y - size * 2.4) * S, (x + size * 1.3) * S, (y - size * 1.5) * S), fill=_a(c), width=max(2, int(S * size * .22)))


def _staff(d, S, x0, x1, y, gap, c, a=200, wave=0.0):
    for k in range(5):
        pts = []
        for i in range(21):
            t = i / 20
            x = x0 + (x1 - x0) * t
            pts.append((x * S, (y + k * gap + wave * math.sin(t * math.tau)) * S))
        d.line(pts, fill=_a(c, a), width=max(1, S // 64))


def note_volley_icon():
    def draw(d, img, S):
        for k, (x, y) in enumerate(((.3, .7), (.5, .5), (.72, .32))):
            g.glow_spot(img, x * S, y * S, .14 * S, ROSE, .6)
            _note(d, S, x, y, .1, GOLD if k != 1 else GOLD_LIGHT)
    return _icon(draw)


def staff_slash_icon():
    def draw(d, img, S):
        for k in range(5):
            r = .3 + k * .04
            d.arc(((.5 - r) * S, (.55 - r) * S, (.5 + r) * S, (.55 + r) * S), 200, 340, fill=_a(GOLD_LIGHT if k == 2 else GOLD), width=2)
        _note(d, S, .3, .42, .07, ROSE)
        _note(d, S, .66, .34, .07, ROSE)
        g.glow_spot(img, .5 * S, .3 * S, .2 * S, ROSE, .5)
    return _icon(draw)


def soothing_melody_icon():
    def draw(d, img, S):
        g.glow_spot(img, .5 * S, .55 * S, .3 * S, (120, 255, 170), .6)
        for k in range(6):
            a = k * math.tau / 6
            _note(d, S, .5 + math.cos(a) * .3, .58 + math.sin(a) * .22, .06, GOLD if k % 2 else (150, 255, 190))
        d.polygon([(.5 * S, .42 * S), (.56 * S, .5 * S), (.5 * S, .6 * S), (.44 * S, .5 * S)], fill=_a(WHITE))
    return _icon(draw)


def crescendo_icon():
    def draw(d, img, S):
        d.line((.14 * S, .5 * S, .86 * S, .2 * S), fill=_a(GOLD), width=4)
        d.line((.14 * S, .5 * S, .86 * S, .8 * S), fill=_a(GOLD), width=4)
        for k in range(3):
            r = .2 + k * .14
            d.arc(((.14 - r) * S, (.5 - r) * S, (.14 + r) * S, (.5 + r) * S), -25, 25, fill=_a(ROSE, 200 - k * 50), width=3)
        g.glow_spot(img, .8 * S, .5 * S, .2 * S, ROSE, .7)
    return _icon(draw)


def lullaby_icon():
    def draw(d, img, S):
        d.ellipse((.2 * S, .14 * S, .62 * S, .56 * S), fill=_a(GOLD_LIGHT))
        d.ellipse((.32 * S, .1 * S, .74 * S, .5 * S), fill=_a((60, 30, 90)))
        for k, (x, y, s) in enumerate(((.6, .62, .09), (.74, .44, .07), (.84, .28, .05))):
            d.text((x * S, y * S), "z", fill=_a(VIOLET))
            d.line((x * S, y * S, (x + s) * S, y * S, x * S, (y + s) * S, (x + s) * S, (y + s) * S), fill=_a(VIOLET if k else WHITE), width=3)
        _note(d, S, .3, .82, .07, VIOLET)
    return _icon(draw, (50, 30, 90))


def war_drum_icon():
    def draw(d, img, S):
        d.rectangle((.2 * S, .42 * S, .8 * S, .78 * S), fill=_a((150, 30, 36)))
        d.ellipse((.2 * S, .32 * S, .8 * S, .52 * S), fill=_a((236, 218, 180)), outline=_a(GOLD))
        d.ellipse((.2 * S, .68 * S, .8 * S, .88 * S), fill=_a((120, 22, 28)), outline=_a(GOLD))
        for x in (.28, .4, .52, .64, .76):
            d.line((x * S, .48 * S, (x + .06) * S, .8 * S), fill=_a(GOLD), width=2)
        d.line((.1 * S, .12 * S, .42 * S, .4 * S), fill=_a((200, 160, 100)), width=4)
        d.line((.9 * S, .12 * S, .58 * S, .4 * S), fill=_a((200, 160, 100)), width=4)
        g.glow_spot(img, .5 * S, .42 * S, .22 * S, ROSE, .5)
    return _icon(draw)


def harp_of_light_icon():
    def draw(d, img, S):
        d.line((.22 * S, .88 * S, .22 * S, .16 * S), fill=_a(GOLD), width=5)
        d.arc((.22 * S, .02 * S, .9 * S, .5 * S), 180, 330, fill=_a(GOLD), width=5)
        d.line((.22 * S, .88 * S, .8 * S, .3 * S), fill=_a(GOLD_DARK), width=4)
        for k in range(6):
            x = .3 + k * .085
            d.line((x * S, (.86 - k * .085) * S, x * S, (.2 + .03 * math.sin(k)) * S), fill=_a(GOLD_LIGHT), width=1)
        g.glow_spot(img, .5 * S, .45 * S, .3 * S, (255, 220, 140), .5)
    return _icon(draw)


def phantom_trumpets_icon():
    def draw(d, img, S):
        for k, (y, ang) in enumerate(((.3, -.3), (.55, 0), (.8, .3))):
            x0, x1 = .12, .62
            d.line((x0 * S, y * S, x1 * S, (y + ang * .2) * S), fill=_a(GOLD), width=4)
            bx, by = x1, y + ang * .2
            d.polygon([(bx * S, (by - .03) * S), ((bx + .22) * S, (by - .12) * S), ((bx + .22) * S, (by + .12) * S), (bx * S, (by + .03) * S)],
                      fill=_a(GOLD_LIGHT), outline=_a(GOLD_DARK))
            g.glow_spot(img, (bx + .26) * S, by * S, .12 * S, (200, 220, 255), .7)
    return _icon(draw, (40, 40, 100))


def storm_organ_icon():
    def draw(d, img, S):
        for k in range(7):
            h = .3 + .38 * math.sin(k / 6 * math.pi)
            x = .14 + k * .11
            d.rectangle((x * S, (.86 - h) * S, (x + .08) * S, .86 * S), fill=_a(GOLD if k % 2 else GOLD_LIGHT), outline=_a(GOLD_DARK))
            d.rectangle((x * S, (.72 - h * .1) * S, (x + .08) * S, (.75 - h * .1) * S), fill=_a((40, 20, 40)))
        d.rectangle((.1 * S, .84 * S, .9 * S, .92 * S), fill=_a((90, 40, 60)))
        g.glow_spot(img, .5 * S, .22 * S, .25 * S, VIOLET, .7)
    return _icon(draw)


def phantom_orchestra_icon():
    def draw(d, img, S):
        g.glow_spot(img, .5 * S, .5 * S, .4 * S, ROSE, .5)
        _staff(d, S, .06, .94, .16, .035, GOLD_LIGHT, 220, .02)
        # A treble clef of gold
        pts = []
        for i in range(60):
            t = i / 59
            a = t * 4.4 * math.pi
            r = .16 * (1 - t) + .03
            pts.append(((.5 + math.cos(a) * r * .7) * S, (.55 + math.sin(a) * r) * S))
        d.line(pts, fill=_a(GOLD), width=4)
        d.line((.55 * S, .2 * S, .5 * S, .9 * S), fill=_a(GOLD), width=4)
        d.ellipse((.42 * S, .84 * S, .5 * S, .92 * S), fill=_a(GOLD))
        _note(d, S, .2, .74, .06, ROSE)
        _note(d, S, .82, .7, .06, ROSE)
    return _icon(draw)


ICONS = (("note_volley", note_volley_icon), ("staff_slash", staff_slash_icon), ("soothing_melody", soothing_melody_icon),
         ("crescendo", crescendo_icon), ("lullaby", lullaby_icon), ("war_drum", war_drum_icon), ("harp_of_light", harp_of_light_icon),
         ("phantom_trumpets", phantom_trumpets_icon), ("storm_organ", storm_organ_icon), ("phantom_orchestra", phantom_orchestra_icon))


# --------------------------------------------------------------------------------------------------- the spellbook

def symphony_codex_icon():
    return g.from_grid([
        "................",
        "...oooooooooo...",
        "..oppppppppppco.",
        "..opgggggggggco.",
        "..opppppYpppgcco",
        "..opgggYYYggpco.",
        "..oppppYpYppgcco",
        "..opgggYYYgggcco",
        "..opppppYppppco.",
        "..opgggggggggcco",
        "..oppppppppppco.",
        "..oddGdddGdddxco",
        "..odddddddddxcco",
        "...ooooooooooo..",
        "................",
        "................",
    ], {"o": PLUM_DARK, "p": PLUM, "g": (120, 70, 110), "Y": GOLD, "G": GOLD_DARK, "d": (40, 12, 46), "x": (20, 6, 24), "c": g.CREAM})


def symphony_codex_model_texture():
    """128 x 128 texture of the 3D book (the grimoire layout, 8 pixels a unit): covers of deep plum velvet, staves of gold
    running across them with notes on them, a gold frame with little lyres at its corners; a spine of gold strings over
    plum; old pages; the ornaments' materials (gold, dark gold, a rose jewel, its glow)."""
    s = 8
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    W, H = 12 * s, 9 * s
    ys, xs = np.mgrid[0:H, 0:W].astype(np.float32)
    nap = _tnoise(W, H, (24, 18), 2131) * .5 + _tnoise(W, H, (6, 4), 2132) * .5
    vign = np.clip(1 - (((xs - W / 2) / (W / 2)) ** 2 + ((ys - H / 2) / (H / 2)) ** 2) * .4, 0, 1)
    cover = _ramp(np.clip((.2 + .6 * nap) * (.55 + .45 * vign), 0, 1), [PLUM_DARK, PLUM, (96, 34, 100), (128, 56, 132)])
    img.paste(_rgba(cover, np.full((H, W), 255)), (0, 0))
    d = ImageDraw.Draw(img)
    # Two staves of gold across the cover, waving, notes on them
    for y0 in (12, 52):
        for k in range(5):
            pts = [(x, y0 + k * 2.2 + 2.2 * math.sin(x / W * math.tau + y0)) for x in range(4, W - 4, 2)]
            d.line(pts, fill=GOLD_DARK + (255,) if k % 2 else GOLD + (255,), width=1)
        for j, x in enumerate(range(16, W - 12, 13)):
            y = y0 + (j * 3) % 9 + 2.2 * math.sin(x / W * math.tau + y0)
            d.ellipse((x - 2, y - 1.4, x + 2, y + 1.4), fill=GOLD_LIGHT + (255,))
            d.line((x + 2, y, x + 2, y - 6), fill=GOLD_LIGHT + (255,))
            if j % 2:
                d.line((x + 2, y - 6, x + 5, y - 4), fill=GOLD_LIGHT + (255,))
    # A frame of gold, small lyres at its corners
    d.rectangle((1, 1, W - 2, H - 2), outline=GOLD + (255,))
    d.rectangle((3, 3, W - 4, H - 4), outline=GOLD_DARK + (255,))
    for cx, cy in ((7, 7), (W - 8, 7), (7, H - 8), (W - 8, H - 8)):
        d.arc((cx - 4, cy - 4, cx, cy + 3), 90, 270, fill=GOLD + (255,))
        d.arc((cx, cy - 4, cx + 4, cy + 3), 270, 90, fill=GOLD + (255,))
        d.line((cx - 1, cy - 2, cx - 1, cy + 3), fill=GOLD_LIGHT + (255,))
        d.line((cx + 1, cy - 2, cx + 1, cy + 3), fill=GOLD_LIGHT + (255,))
    d.rectangle((0, 0, W - 1, H - 1), outline=PLUM_DARK + (255,))
    # The cover's edge: gold
    for yy in range(0, 9 * s):
        for xx in range(12 * s, 13 * s):
            img.putpixel((xx, yy), g.lerp(GOLD_DARK, GOLD, .5 + .5 * math.sin(yy * .5)) + (255,))
    # The page edges
    for yy in range(0, 8 * s):
        for xx in range(13 * s, 16 * s):
            img.putpixel((xx, yy), g.lerp(g.CREAM, (176, 168, 140), ((xx * 7 + yy * 13) % 11) / 22) + (255,))
    for yy in range(1, 8 * s, 2):
        d.line((13 * s, yy, 16 * s - 1, yy), fill=g.CREAM_SHADE + (255,))
    # The spine: plum under strings of gold
    spine = _ramp(np.clip(.2 + .6 * _tnoise(12 * s, 3 * s, (12, 4), 2133), 0, 1), [PLUM_DARK, PLUM, (96, 34, 100)])
    img.paste(_rgba(spine, np.full((3 * s, 12 * s), 255)), (0, 9 * s))
    for yy in range(9 * s + 3, 12 * s - 2, 4):
        d.line((0, yy, 12 * s - 1, yy), fill=GOLD_LIGHT + (255,))
    d.rectangle((0, 12 * s, 12 * s - 1, 15 * s - 1), fill=g.CREAM + (255,))
    for xx in range(0, 12 * s, 2):
        d.line((xx, 12 * s, xx, 15 * s - 1), fill=g.CREAM_SHADE + (255,))
    # The plain book's metal (corners, bands), gem (headbands, bezel) and strap
    _metal(img, 12 * s, 9 * s, 2 * s, 2 * s, GOLD_LIGHT, GOLD_DARK)
    _jewel(img, 14 * s, 9 * s, 2 * s, 2 * s)
    d.rectangle((12 * s, 11 * s, 16 * s - 1, 12 * s - 1), fill=PLUM_DARK + (255,))
    d.line((12 * s, 11 * s + 3, 16 * s - 1, 11 * s + 3), fill=GOLD + (255,))
    # The ornaments' materials
    _metal(img, 12 * s, 12 * s, 2 * s, 2 * s, GOLD_LIGHT, GOLD_DARK)
    _metal(img, 14 * s, 12 * s, 2 * s, 2 * s, GOLD, (100, 60, 20))
    _jewel(img, 12 * s, 14 * s, 2 * s, 2 * s)
    for yy in range(14 * s, 16 * s):
        for xx in range(14 * s, 16 * s):
            t = math.hypot(xx - 15 * s, yy - 15 * s) / (1.2 * s)
            img.putpixel((xx, yy), g.lerp((255, 255, 255), ROSE, min(1, t)) + (255,))
    return img


def _metal(img, x0, y0, w, h, light, dark):
    for yy in range(y0, y0 + h):
        for xx in range(x0, x0 + w):
            t = .5 + .5 * math.sin((xx - x0) * .5 + (yy - y0) * .2)
            img.putpixel((xx, yy), g.lerp(dark, light, t * .85 + .1) + (255,))


def _jewel(img, x0, y0, w, h):
    for yy in range(y0, y0 + h):
        for xx in range(x0, x0 + w):
            u, v = (xx - x0) / w, (yy - y0) / h
            t = abs(u - .5) + abs(v - .5)
            img.putpixel((xx, yy), g.lerp((255, 230, 250), ROSE, min(1, t * 1.3)) + (255,))


def symphony_codex_ornaments():
    """On each cover: a lyre of gold (its two arms, its crossbar, its strings over a rose glow), notes about it; on the
    spine, tuning pegs; a gold scroll at each end of the spine, like a violin's."""
    e = []
    for front in (True, False):
        # The glow behind the strings
        x0, x1 = bo.on_cover(front, 0, .05)
        e.append(bo.turned(front, x0, x1, 3.6, 8.0, 1.4, .9, bo.GLOW, 0))
        # The arms: plates leaning out, a curl at each top
        x0, x1 = bo.on_cover(front, .05, .2)
        e.append(bo.turned(front, x0, x1, 3.4, 6.75, 1.7, .22, bo.METAL, -22.5))
        e.append(bo.turned(front, x0, x1, 3.4, 9.25, 1.7, .22, bo.METAL, 22.5))
        e.append(bo.turned(front, x0, x1, 5.2, 6.35, .45, .45, bo.METAL, 45))
        e.append(bo.turned(front, x0, x1, 5.2, 9.65, .45, .45, bo.METAL, 45))
        # The crossbar and the base
        e.append(bo.turned(front, x0, x1, 5.0, 8.0, .16, 1.55, bo.METAL_DARK, 0))
        e.append(bo.turned(front, x0, x1, 1.75, 8.0, .3, 1.2, bo.METAL_DARK, 0))
        # Its strings
        x0, x1 = bo.on_cover(front, .08, .14)
        for z in (7.4, 7.8, 8.2, 8.6):
            e.append(bo.solid([x0, 2.05, z - .04], [x1, 4.85, z + .04], bo.GLOW))
        # A rose jewel on the base
        x0, x1 = bo.on_cover(front, .2, .32)
        e.append(bo.turned(front, x0, x1, 1.75, 8.0, .28, .28, bo.JEWEL, 45))
        # Notes about it: a head and a stem each
        for cy, cz in ((6.2, 4.6), (1.3, 11.3), (6.0, 11.6)):
            x0, x1 = bo.on_cover(front, 0, .12)
            e.append(bo.turned(front, x0, x1, cy, cz, .26, .36, bo.METAL, 22.5))
            e.append(bo.turned(front, x0, x1, cy + .55, cz + .3, .55, .06, bo.METAL, 0))
    # Tuning pegs along the spine's top
    h = bo.H
    for z in (5.0, 6.6, 9.4, 11.0):
        e.append(bo.solid([7.85, h + .3, z - .12], [8.15, h + .75, z + .12], bo.METAL_DARK))
        e.append(bo.solid([7.7, h + .7, z - .25], [8.3, h + .85, z + .25], bo.METAL, {"origin": [8, h + .78, z], "axis": "y", "angle": 45}))
    # Scrolls at both ends of the spine
    for z in (bo.Z0 - .2, bo.Z1 + .2):
        e.append(bo.solid([7.55, h - .35, z - .3], [8.45, h + .45, z + .3], bo.METAL))
        e.append(bo.solid([7.7, h - .1, z - .38], [8.3, h + .3, z + .38], bo.JEWEL, {"origin": [8, h + .1, z], "axis": "x", "angle": 45}))
    return e


def affinity_ring_melody():
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
    ], {"o": PLUM_DARK, "h": GOLD_LIGHT, "l": GOLD, "b": (200, 150, 70), "s": GOLD_DARK, "v": ROSE, "Y": GOLD_LIGHT, "R": ROSE, "W": WHITE})


def main():
    g.save(staff_texture(), "textures/entity/melody/staff.png")
    g.save(varnish_texture(), "textures/entity/melody/varnish.png")
    g.save(drumhead_texture(), "textures/entity/melody/drumhead.png")
    for name, icon in ICONS:
        g.save(icon(), f"textures/gui/spell_icons/{name}.png")
    g.save(affinity_ring_melody(), "textures/item/affinity_ring_melody.png")
    g.write_json("models/item/affinity_ring_melody.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/affinity_ring_melody"}})
    g.save(symphony_codex_icon(), "textures/item/symphony_codex.png")
    g.save(symphony_codex_model_texture(), "textures/item/symphony_codex_3d.png")
    g.book_models("symphony_codex")
    orn = symphony_codex_ornaments()
    for el in orn:
        rot = el.get("rotation")
        assert rot is None or rot["angle"] in (-45, -22.5, 0, 22.5, 45), rot
    bo.add("symphony_codex", orn)


if __name__ == "__main__":
    main()
