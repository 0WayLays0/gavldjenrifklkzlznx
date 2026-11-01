#!/usr/bin/env python3
"""Echo Magic art: what sound and the sculk of the deep dark are drawn with (rings and arcs of sound, a patch of sculk
and its glowing veins), the spell icons, the Codex of Echoes (icon and 3D book texture) and its affinity ring.

Called by gen_assets.py (run that one). Additive textures (drawn as light) are white on black and tinted in game; the
sculk patch is drawn see-through. Original art.
"""
import math
import random

import numpy as np
from PIL import Image, ImageDraw

import gen_assets as g
from water_assets import _tnoise, _quant, _rgba, _ramp

#: The deep dark: sculk almost black, sculk, the cyan of its light, its pale glow; bone of a shrieker.
NIGHT = (4, 18, 22)
SCULK_DARK = (6, 42, 48)
SCULK = (13, 79, 90)
ECHO = (44, 230, 240)
PALE = (200, 251, 255)
BONE = (232, 226, 207)


def wave_texture():
    """64 x 64: a ring of sound, white on black: a sharp bright rim, a softer ring just inside it."""
    S = 64
    ys, xs = np.mgrid[0:S, 0:S]
    r = np.hypot(xs + .5 - S / 2, ys + .5 - S / 2) / (S / 2)
    rim = np.exp(-((r - .86) / .045) ** 2)
    inner = .45 * np.exp(-((r - .7) / .08) ** 2)
    v = _quant(np.clip(rim + inner, 0, 1), 8)
    return _rgba(np.dstack([v * 255] * 3), np.full((S, S), 255.0))


def arc_texture():
    """64 x 16: an arc of sound seen side on, white on black: brightest along its middle (V), fading out at both ends (U),
    with a faint second line beside it."""
    W, H = 64, 16
    ys, xs = np.mgrid[0:H, 0:W]
    u, v = (xs + .5) / W, (ys + .5) / H * 2 - 1
    along = np.sin(np.pi * u) ** .6
    across = np.exp(-(v / .28) ** 2) + .35 * np.exp(-((np.abs(v) - .7) / .12) ** 2)
    val = _quant(np.clip(along * across, 0, 1), 7)
    return _rgba(np.dstack([val * 255] * 3), np.full((H, W), 255.0))


def _veins(S, seed, count):
    """A web of sculk veins (0 to 1) branching out from the middle of an S x S square."""
    img = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(img)
    rnd = random.Random(seed)
    for k in range(count):
        a = k * math.tau / count + rnd.uniform(-.3, .3)
        x, y = S / 2, S / 2
        w = 3.0
        for step in range(14):
            a += rnd.uniform(-.55, .55)
            nx, ny = x + math.cos(a) * S * .035, y + math.sin(a) * S * .035
            d.line([(x, y), (nx, ny)], fill=255, width=max(1, int(w)))
            if rnd.random() < .25:
                b = a + rnd.choice((-1, 1)) * rnd.uniform(.6, 1.2)
                bx, by = nx + math.cos(b) * S * .06, ny + math.sin(b) * S * .06
                d.line([(nx, ny), (bx, by)], fill=200, width=1)
            x, y = nx, ny
            w = max(1, w * .88)
    for _ in range(40):
        a, r = rnd.uniform(0, math.tau), rnd.uniform(.05, .45) * S
        cx, cy = S / 2 + math.cos(a) * r, S / 2 + math.sin(a) * r
        d.ellipse((cx - 1, cy - 1, cx + 1, cy + 1), fill=255)
    return np.asarray(img, dtype=np.float32) / 255


def sculk_texture():
    """64 x 64: a patch of sculk spread on the ground, drawn see-through: almost black teal, mottled, its edge ragged and
    fading, its veins a little lighter (their glow is drawn over it as light)."""
    S = 64
    ys, xs = np.mgrid[0:S, 0:S]
    r = np.hypot(xs + .5 - S / 2, ys + .5 - S / 2) / (S / 2)
    n = _tnoise(S, S, (6, 6), 1201)
    n2 = _tnoise(S, S, (12, 12), 1202)
    edge = np.clip((1 - r + .25 * (n - .5)) * 4, 0, 1)
    veins = _veins(S, 1203, 9)
    t = np.clip(.25 + .35 * n2 + .5 * veins, 0, 1)
    rgb = _ramp(t, [NIGHT, SCULK_DARK, SCULK, (30, 140, 150)])
    a = _quant(edge, 6) * (225 + 30 * veins)
    return _rgba(rgb, a)


def veins_texture():
    """64 x 64: the glowing veins of the sculk patch alone, white on black, brightest near the middle."""
    S = 64
    ys, xs = np.mgrid[0:S, 0:S]
    r = np.hypot(xs + .5 - S / 2, ys + .5 - S / 2) / (S / 2)
    v = _veins(S, 1203, 9) * np.clip(1.2 - r, 0, 1)
    v = _quant(v, 6)
    return _rgba(np.dstack([v * 255] * 3), np.full((S, S), 255.0))


# ------------------------------------------------------------------------------------------------------- the icons

def _icon(draw, cx=.5, cy=.5, bg=(10, 50, 58)):
    return g.painted(64, draw, (cx, cy, bg, NIGHT), 16)


def _a(c, a=255):
    return tuple(int(x) for x in c) + (a,)


def _arc(d, S, cx, cy, r, a0, a1, w, col, alpha=255):
    d.arc(((cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r) * S), a0, a1, fill=_a(col, alpha), width=max(1, int(w * S)))


def sound_wave_icon():
    def draw(d, img, S):
        for k in range(4):
            _arc(d, S, .2, .5, .18 + k * .17, -45, 45, .05 - k * .006, PALE if k % 2 == 0 else ECHO)
        g.glow_spot(img, .2 * S, .5 * S, .15 * S, ECHO, .8)
    return _icon(draw, .2, .5)


def shriek_icon():
    def draw(d, img, S):
        d.polygon([(.08 * S, .5 * S), (.92 * S, .12 * S), (.92 * S, .88 * S)], fill=_a(SCULK, 120))
        for k in range(4):
            _arc(d, S, .08, .5, .22 + k * .2, -24, 24, .04, PALE if k % 2 else ECHO)
        for y in (.38, .62):
            d.polygon([(.06 * S, (y - .06) * S), (.16 * S, y * S), (.06 * S, (y + .06) * S)], fill=_a(BONE))
    return _icon(draw, .2, .5)


def echo_step_icon():
    def draw(d, img, S):
        for k, (x, al) in enumerate(((.25, 90), (.45, 160), (.68, 255))):
            d.rectangle(((x - .07) * S, .3 * S, (x + .07) * S, .75 * S), fill=_a(ECHO if k < 2 else PALE, al))
            d.ellipse(((x - .07) * S, .12 * S, (x + .07) * S, .28 * S), fill=_a(ECHO if k < 2 else PALE, al))
        _arc(d, S, .25, .5, .2, 0, 360, .025, ECHO, 180)
    return _icon(draw, .6, .5)


def sculk_tendrils_icon():
    def draw(d, img, S):
        d.ellipse((.1 * S, .72 * S, .9 * S, .96 * S), fill=_a(SCULK_DARK))
        for k in range(5):
            x = .2 + k * .15
            pts = [(x * S, .84 * S), ((x + .05 * math.sin(k)) * S, .6 * S), ((x - .06 * math.cos(k)) * S, .38 * S), ((x + .03) * S, (.18 + .05 * (k % 2)) * S)]
            d.line(pts, fill=_a(SCULK), width=max(2, S // 14))
            d.line(pts, fill=_a(ECHO), width=max(1, S // 48))
            d.ellipse(((x - .03) * S, (.15 + .05 * (k % 2)) * S, (x + .06) * S, (.22 + .05 * (k % 2)) * S), fill=_a(PALE))
    return _icon(draw, .5, .7)


def sculk_sensor_icon():
    def draw(d, img, S):
        d.rectangle((.22 * S, .55 * S, .78 * S, .9 * S), fill=_a(SCULK_DARK), outline=_a(SCULK))
        d.rectangle((.3 * S, .62 * S, .7 * S, .7 * S), fill=_a(ECHO))
        for x, lean in ((.3, -.08), (.42, -.03), (.58, .03), (.7, .08)):
            d.line([(x * S, .55 * S), ((x + lean) * S, .2 * S)], fill=_a(ECHO), width=max(2, S // 20))
        for k in range(2):
            _arc(d, S, .5, .3, .3 + k * .14, 200, 340, .02, PALE)
    return _icon(draw, .5, .4)


def resonant_burst_icon():
    def draw(d, img, S):
        for k in range(4):
            r = .1 + k * .11
            d.ellipse(((.5 - r) * S, (.5 - r) * S, (.5 + r) * S, (.5 + r) * S), outline=_a(PALE if k % 2 == 0 else ECHO), width=max(1, S // 30))
        for k in range(8):
            a = k * math.tau / 8
            d.line([((.5 + math.cos(a) * .2) * S, (.5 + math.sin(a) * .2) * S), ((.5 + math.cos(a) * .47) * S, (.5 + math.sin(a) * .47) * S)],
                   fill=_a(ECHO), width=max(1, S // 40))
        g.glow_spot(img, .5 * S, .5 * S, .2 * S, PALE, 1)
    return _icon(draw, .5, .5)


def sonic_boom_icon():
    def draw(d, img, S):
        d.rectangle((0, .44 * S, S, .56 * S), fill=_a(ECHO, 140))
        for k in range(6):
            x = .1 + k * .16
            r = .08 + .03 * k
            d.ellipse(((x - .03) * S, (.5 - r) * S, (x + .03) * S, (.5 + r) * S), outline=_a(PALE), width=max(1, S // 32))
        g.glow_spot(img, .08 * S, .5 * S, .14 * S, PALE, 1)
    return _icon(draw, .1, .5)


def silence_dome_icon():
    def draw(d, img, S):
        d.pieslice((.1 * S, .2 * S, .9 * S, 1.0 * S), 180, 360, fill=_a(SCULK, 150), outline=_a(ECHO))
        for k in range(3):
            y = .4 + k * .13
            d.line([((.2 + k * .06) * S, y * S), ((.8 - k * .06) * S, y * S)], fill=_a(ECHO, 160), width=max(1, S // 48))
        d.line([(.38 * S, .5 * S), (.62 * S, .74 * S)], fill=_a(PALE), width=max(2, S // 18))
        d.line([(.62 * S, .5 * S), (.38 * S, .74 * S)], fill=_a(PALE), width=max(2, S // 18))
    return _icon(draw, .5, .6)


def deep_choir_icon():
    def draw(d, img, S):
        for x in (.22, .5, .78):
            d.rectangle(((x - .1) * S, .62 * S, (x + .1) * S, .86 * S), fill=_a(SCULK_DARK), outline=_a(SCULK))
            for k in range(3):
                bx = x - .08 + k * .08
                d.polygon([((bx - .02) * S, .62 * S), ((bx + .02) * S, .62 * S), (bx * S, .5 * S)], fill=_a(BONE))
            g.glow_spot(img, x * S, .6 * S, .07 * S, ECHO, 1)
        for k in range(3):
            _arc(d, S, .5, .5, .2 + k * .12, 210, 330, .025, PALE if k % 2 == 0 else ECHO)
    return _icon(draw, .5, .6)


def requiem_icon():
    def draw(d, img, S):
        for k in range(5):
            r = .08 + k * .09
            d.ellipse(((.5 - r) * S, (.55 - r * .45) * S, (.5 + r) * S, (.55 + r * .45) * S), outline=_a(ECHO if k % 2 else PALE), width=max(1, S // 36))
        d.ellipse((.4 * S, .14 * S, .6 * S, .34 * S), fill=_a(SCULK), outline=_a(ECHO))
        g.glow_spot(img, .5 * S, .24 * S, .14 * S, PALE, 1)
        for k in range(6):
            a = k * math.tau / 6
            d.line([(.5 * S, .55 * S), ((.5 + math.cos(a) * .4) * S, (.55 + math.sin(a) * .18) * S)], fill=_a(SCULK), width=max(1, S // 40))
    return _icon(draw, .5, .4, (16, 70, 80))


ICONS = (("sound_wave", sound_wave_icon), ("shriek", shriek_icon), ("echo_step", echo_step_icon), ("sculk_tendrils", sculk_tendrils_icon),
         ("sculk_sensor", sculk_sensor_icon), ("resonant_burst", resonant_burst_icon), ("sonic_boom", sonic_boom_icon),
         ("silence_dome", silence_dome_icon), ("deep_choir", deep_choir_icon), ("requiem", requiem_icon))


# --------------------------------------------------------------------------------------------------- the spellbook

COVER = (14, 52, 60)
COVER_LIGHT = (26, 84, 94)
COVER_DARK = (6, 28, 34)
BRASS = (196, 220, 214)


def echoes_codex_icon():
    return g.from_grid([
        "................",
        "...oooooooooo...",
        "..obbbbbbbbbdco.",
        "..obsbbbbbbbdco.",
        "..obbbEEEbbbdco.",
        "..obbEbbbEbbdcco",
        "..obEbPPPbEbdco.",
        "..obbEbbbEbbdcco",
        "..obbbEEEbbbdcco",
        "..obbbbbbbsbdcco",
        "..obbbbbbbbbdco.",
        "..oddssdddssxco.",
        "..oddddssdddxco.",
        "...ooooooooooo..",
        "................",
        "................",
    ], {"o": (2, 10, 12), "b": COVER, "s": BRASS, "d": COVER_DARK, "x": (2, 8, 10), "E": ECHO, "P": PALE, "c": g.CREAM})


def echoes_codex_model_texture():
    """64x64 texture of the 3D book, laid out like the other books: covers of sculk-dark leather framed in pale bone, rings
    of sound spreading from a glowing heart on the front, veins of sculk creeping in from the corners, old pages."""
    s = 4
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    W, H = 12 * s, 9 * s
    grain = _tnoise(W, H, (6, 4), 1211)
    cover = _ramp(.3 + .4 * grain, [COVER_DARK, COVER, COVER_LIGHT])
    img.paste(_rgba(cover, np.full((H, W), 255)), (0, 0))
    veins = _veins(64, 1212, 7)[14:14 + H, 8:8 + W]
    px = img.load()
    for y in range(H):
        for x in range(W):
            if veins[y, x] > .5:
                px[x, y] = g.lerp(px[x, y][:3], ECHO, .45) + (255,)
    d.rectangle((1, 1, W - 2, H - 2), outline=BONE + (255,))
    cx, cy = W // 2, H // 2
    for k in range(4):
        r = 4 + k * 3.5
        d.ellipse((cx - r, cy - r * .8, cx + r, cy + r * .8), outline=(ECHO if k % 2 else PALE) + (255,))
    d.ellipse((cx - 2, cy - 2, cx + 2, cy + 2), fill=PALE + (255,))
    d.rectangle((0, 0, W - 1, H - 1), outline=(2, 10, 12, 255))
    d.rectangle((12 * s, 0, 13 * s - 1, 9 * s - 1), fill=COVER_DARK + (255,))
    for y in range(0, 8 * s):
        for x in range(13 * s, 16 * s):
            img.putpixel((x, y), g.lerp(g.CREAM, (176, 168, 140), ((x * 7 + y * 13) % 11) / 22) + (255,))
    for y in range(1, 8 * s, 2):
        d.line((13 * s, y, 16 * s - 1, y), fill=g.CREAM_SHADE + (255,))
    for y in range(9 * s, 12 * s):
        for x in range(0, 12 * s):
            t = .5 + .5 * math.sin(x * .4)
            img.putpixel((x, y), g.lerp((40, 70, 76), BONE, t * .6) + (255,))
    d.rectangle((0, 12 * s, 12 * s - 1, 15 * s - 1), fill=g.CREAM + (255,))
    for x in range(0, 12 * s, 2):
        d.line((x, 12 * s, x, 15 * s - 1), fill=g.CREAM_SHADE + (255,))
    d.rectangle((12 * s, 9 * s, 14 * s - 1, 11 * s - 1), fill=BONE + (255,))
    for y in range(9 * s, 11 * s):
        for x in range(14 * s, 16 * s):
            t = math.hypot(x - 14.8 * s, y - 9.8 * s) / (1.4 * s)
            img.putpixel((x, y), g.lerp(PALE, SCULK, min(1, t ** .8)) + (255,))
    d.rectangle((12 * s, 11 * s, 16 * s - 1, 12 * s - 1), fill=COVER_DARK + (255,))
    return img


def affinity_ring_echo():
    return g.from_grid([
        "................",
        "......oooo......",
        ".....oxEExo.....",
        ".....oEPPEo.....",
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
    ], {"o": (2, 12, 14), "h": (236, 246, 244), "l": BONE, "b": (150, 146, 132), "s": (70, 74, 70), "v": ECHO, "E": ECHO, "P": PALE, "x": SCULK})


def main():
    g.save(wave_texture(), "textures/entity/echo/wave.png")
    g.save(arc_texture(), "textures/entity/echo/arc.png")
    g.save(sculk_texture(), "textures/entity/echo/sculk.png")
    g.save(veins_texture(), "textures/entity/echo/veins.png")
    for name, icon in ICONS:
        g.save(icon(), f"textures/gui/spell_icons/{name}.png")
    g.save(affinity_ring_echo(), "textures/item/affinity_ring_echo.png")
    g.write_json("models/item/affinity_ring_echo.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/affinity_ring_echo"}})
    g.save(echoes_codex_icon(), "textures/item/echoes_codex.png")
    g.save(echoes_codex_model_texture(), "textures/item/echoes_codex_3d.png")
    g.book_models("echoes_codex")


if __name__ == "__main__":
    main()
