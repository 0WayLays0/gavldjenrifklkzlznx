#!/usr/bin/env python3
"""Boss Magic art: the warning zones' textures (rune circles, chevrons, glyphs, the zone fill and its edge), the
scenery (the judgment blade, beams, the prison wall), spell icons, scroll, affinity ring and the Tyrant's Codex.

Called by gen_assets.py (run that one). All art is original. Additive textures (drawn with RenderType.eyes) are
greyscale on black: black is invisible, the renderers tint them per style. The zone fill is white with alpha (drawn
alpha-blended, tinted by the zone's colour).
"""
import math
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

import gen_assets as g
from explosion_assets import bloom, fbm, grey

# Boss palette
NIGHT = (16, 6, 10)
BLOOD_DARK = (70, 8, 18)
BLOOD = (150, 18, 36)
CRIMSON = (224, 40, 60)
EMBER = (255, 120, 60)
GOLD = (255, 200, 80)
GOLD_DARK = (170, 110, 30)
PALE = (255, 236, 220)
IRON = (60, 52, 58)
IRON_LIGHT = (112, 100, 108)
VOID = (150, 60, 255)
VOID_DARK = (52, 14, 96)
STAR = (120, 200, 255)
EARTH = (200, 110, 40)

S = 512  # working size of the big additive textures, drawn then scaled down


# ---------------------------------------------------------------- runes

def glyph(d, cx, cy, size, seed, width, col=(255, 255, 255)):
    """A rune: three to five strokes between the points of a 3x3 grid, always with a stem."""
    rnd = random.Random(seed)
    pts = [(cx + (i - 1) * size * .5, cy + (j - 1) * size * .5) for j in range(3) for i in range(3)]
    stem = rnd.choice(((1, 7), (0, 6), (2, 8)))
    d.line([pts[stem[0]], pts[stem[1]]], fill=col, width=width)
    for _ in range(rnd.randint(2, 4)):
        a, b = rnd.sample(range(9), 2)
        if (pts[a][0] - pts[b][0]) ** 2 + (pts[a][1] - pts[b][1]) ** 2 > (size * 1.2) ** 2:
            continue
        d.line([pts[a], pts[b]], fill=col, width=width)
    if rnd.random() < .35:
        r = size * .14
        p = pts[rnd.choice((1, 3, 4, 5, 7))]
        d.ellipse((p[0] - r, p[1] - r, p[0] + r, p[1] + r), outline=col, width=max(1, width - 1))


def _circle(d, c, r, w, col=(255, 255, 255)):
    d.ellipse((c - r, c - r, c + r, c + r), outline=col, width=w)


def _star_polygon(d, c, r, n, step, w, rot=-math.pi / 2, col=(255, 255, 255)):
    pts = [(c + math.cos(rot + k * math.tau / n) * r, c + math.sin(rot + k * math.tau / n) * r) for k in range(n)]
    k = 0
    for _ in range(n):
        d.line([pts[k], pts[(k + step) % n]], fill=col, width=w)
        k = (k + step) % n


def _rune_band(img, c, r0, r1, count, seed, width):
    """Runes standing between two circles, all turned toward the centre."""
    mid = (r0 + r1) / 2
    size = (r1 - r0) * .62
    for k in range(count):
        a = k * math.tau / count
        cell = Image.new("L", (int(size * 2), int(size * 2)), 0)
        glyph(ImageDraw.Draw(cell), size, size, size * .9, seed * 100 + k, width, 255)
        cell = cell.rotate(-math.degrees(a) - 90, resample=Image.BICUBIC)
        x, y = c + math.cos(a) * mid, c + math.sin(a) * mid
        img.paste((255, 255, 255), (int(x - size), int(y - size)), cell)


def _ticks(d, c, r, n, length, w, every=1):
    for k in range(n):
        a = k * math.tau / n
        l = length * (2 if k % every == 0 else 1)
        d.line([(c + math.cos(a) * r, c + math.sin(a) * r), (c + math.cos(a) * (r - l), c + math.sin(a) * (r - l))], fill=(255, 255, 255), width=w)


def _finish(img, size=256, glow=.9):
    return bloom(img.resize((size, size), Image.LANCZOS), 2.5, glow)


def rune_circle_tyrant():
    """The tyrant's circle: a band of runes, a seven-pointed star, a ring of ticks and an eye in the middle."""
    img = Image.new("RGB", (S, S))
    d = ImageDraw.Draw(img)
    c = S / 2
    _circle(d, c, .975 * c, 9)
    _circle(d, c, .90 * c, 4)
    _rune_band(img, c, .78 * c, .90 * c, 22, 1, 5)
    _circle(d, c, .78 * c, 4)
    _star_polygon(d, c, .76 * c, 7, 3, 5)
    _circle(d, c, .58 * c, 6)
    _ticks(d, c, .58 * c, 72, 8, 3, every=6)
    _circle(d, c, .40 * c, 4)
    # Three hooked horns around the eye
    for k in range(3):
        a = k * math.tau / 3 - math.pi / 2
        pts = [(c + math.cos(a + t * .9) * (.40 - .16 * t) * c, c + math.sin(a + t * .9) * (.40 - .16 * t) * c) for t in [i / 8 for i in range(9)]]
        d.line(pts, fill=(255, 255, 255), width=6)
    # The eye
    w, h = .26 * c, .13 * c
    d.chord((c - w, c - h * 2, c + w, c + h * 2), 200, 340, outline=(255, 255, 255), width=6)
    d.chord((c - w, c - h * 2, c + w, c + h * 2), 20, 160, outline=(255, 255, 255), width=6)
    d.ellipse((c - .07 * c, c - .07 * c, c + .07 * c, c + .07 * c), fill=(255, 255, 255))
    return _finish(img)


def rune_circle_judgment():
    """The judge's circle: a dial of twelve small circles, an eight-pointed star in a double square and a sun."""
    img = Image.new("RGB", (S, S))
    d = ImageDraw.Draw(img)
    c = S / 2
    _circle(d, c, .975 * c, 8)
    _circle(d, c, .88 * c, 4)
    for k in range(12):
        a = k * math.tau / 12
        x, y = c + math.cos(a) * .93 * c, c + math.sin(a) * .93 * c
        r = .035 * c
        d.ellipse((x - r, y - r, x + r, y + r), outline=(255, 255, 255), width=4)
        glyph(d, c + math.cos(a + math.pi / 12) * .93 * c, c + math.sin(a + math.pi / 12) * .93 * c, .05 * c, 300 + k, 3)
    _star_polygon(d, c, .86 * c, 8, 3, 5)
    for rot in (0, math.pi / 4):
        _star_polygon(d, c, .62 * c, 4, 1, 4, rot)
    _circle(d, c, .44 * c, 5)
    _rune_band(img, c, .30 * c, .44 * c, 12, 3, 4)
    _circle(d, c, .30 * c, 4)
    for k in range(16):
        a = k * math.tau / 16
        r = (.27 if k % 2 == 0 else .19) * c
        d.line([(c + math.cos(a) * .1 * c, c + math.sin(a) * .1 * c), (c + math.cos(a) * r, c + math.sin(a) * r)], fill=(255, 255, 255), width=5)
    d.ellipse((c - .09 * c, c - .09 * c, c + .09 * c, c + .09 * c), fill=(255, 255, 255))
    return _finish(img)


def rune_circle_void():
    """The void's circle: spiral arms, a pentagram, crescents at its points and a dotted heart."""
    img = Image.new("RGB", (S, S))
    d = ImageDraw.Draw(img)
    c = S / 2
    _circle(d, c, .975 * c, 7)
    _rune_band(img, c, .84 * c, .965 * c, 18, 5, 5)
    _circle(d, c, .84 * c, 4)
    for arm in range(3):
        pts = []
        for i in range(60):
            t = i / 59
            a = arm * math.tau / 3 + t * 4.2
            r = (.82 - .62 * t) * c
            pts.append((c + math.cos(a) * r, c + math.sin(a) * r))
        d.line(pts, fill=(255, 255, 255), width=5)
    _star_polygon(d, c, .6 * c, 5, 2, 5)
    _circle(d, c, .6 * c, 4)
    for k in range(5):
        a = -math.pi / 2 + k * math.tau / 5
        x, y = c + math.cos(a) * .6 * c, c + math.sin(a) * .6 * c
        r = .09 * c
        d.ellipse((x - r, y - r, x + r, y + r), outline=(255, 255, 255), width=5)
        d.ellipse((x - r * .55 + math.cos(a) * r * .45, y - r * .55 + math.sin(a) * r * .45, x + r * .55 + math.cos(a) * r * .45, y + r * .55 + math.sin(a) * r * .45), fill=(0, 0, 0))
    for k in range(24):
        a = k * math.tau / 24
        x, y = c + math.cos(a) * .22 * c, c + math.sin(a) * .22 * c
        r = .018 * c
        d.ellipse((x - r, y - r, x + r, y + r), fill=(255, 255, 255))
    _circle(d, c, .12 * c, 5)
    return _finish(img)


def sky_circle():
    """The great circle in the sky: a double band of runes, a hexagram with a small circle on each point, rings within rings."""
    img = Image.new("RGB", (S, S))
    d = ImageDraw.Draw(img)
    c = S / 2
    _circle(d, c, .98 * c, 10)
    _rune_band(img, c, .86 * c, .97 * c, 28, 7, 5)
    _circle(d, c, .86 * c, 5)
    _ticks(d, c, .86 * c, 120, 7, 2, every=10)
    for rot in (-math.pi / 2, math.pi / 2):
        _star_polygon(d, c, .74 * c, 3, 1, 6, rot)
    for k in range(6):
        a = -math.pi / 2 + k * math.tau / 6
        x, y = c + math.cos(a) * .74 * c, c + math.sin(a) * .74 * c
        r = .1 * c
        d.ellipse((x - r, y - r, x + r, y + r), fill=(0, 0, 0), outline=(255, 255, 255), width=5)
        glyph(d, x, y, r * .9, 700 + k, 4)
    _circle(d, c, .42 * c, 6)
    _rune_band(img, c, .30 * c, .42 * c, 14, 9, 4)
    _circle(d, c, .30 * c, 4)
    _circle(d, c, .16 * c, 7)
    _star_polygon(d, c, .16 * c, 6, 2, 4)
    return _finish(img)


def glyphs():
    """Sixteen floating runes, 4 x 4, for the symbols rising from a warning zone."""
    img = Image.new("RGB", (512, 512))
    d = ImageDraw.Draw(img)
    for k in range(16):
        cx, cy = (k % 4) * 128 + 64, (k // 4) * 128 + 64
        glyph(d, cx, cy, 70, 900 + k, 9)
        d.ellipse((cx - 56, cy - 56, cx + 56, cy + 56), outline=(110, 110, 110), width=4)
    return bloom(img.resize((128, 128), Image.LANCZOS), 2, 1.0)


def chevrons():
    """Chevrons pointing down the texture (+v), the way a line's attack travels, with runes between them."""
    W = 256
    img = Image.new("RGB", (W, W))
    d = ImageDraw.Draw(img)
    for y0 in (0, W // 2):
        pts = [(W * .12, y0 + W * .08), (W * .5, y0 + W * .36), (W * .88, y0 + W * .08)]
        d.line(pts, fill=(255, 255, 255), width=18)
        pts = [(W * .26, y0 + W * .02), (W * .5, y0 + W * .2), (W * .74, y0 + W * .02)]
        d.line(pts, fill=(150, 150, 150), width=8)
    for x in (W * .08, W * .92):
        d.line([(x, 0), (x, W)], fill=(200, 200, 200), width=6)
    img = img.resize((64, 64), Image.LANCZOS)
    return bloom(img, 1.5, .8)


def zone():
    """The fill of a danger zone: white, fainter in its heart and solid at its edge (u: 0 heart, 1 edge)."""
    img = Image.new("RGBA", (64, 4))
    for x in range(64):
        u = x / 63
        a = .42 + .58 * g_smooth(.55, 1, u) ** 1.4
        # A faint hatching rhythm toward the edge makes it read as a marked area
        if .55 < u < .9 and (x // 3) % 2 == 0:
            a = min(1, a + .08)
        for y in range(4):
            img.putpixel((x, y), (255, 255, 255, int(a * 255)))
    return img


def g_smooth(e0, e1, x):
    t = max(0.0, min(1.0, (x - e0) / (e1 - e0)))
    return t * t * (3 - 2 * t)


def edge():
    """A bright band, brightest in its middle (u across it)."""
    img = Image.new("RGB", (32, 32))
    for x in range(32):
        u = (x + .5) / 32
        v = math.exp(-((u - .5) / .2) ** 2)
        for y in range(32):
            img.putpixel((x, y), grey(v)[:3])
    return img.convert("RGBA")


def beam():
    """Energy running along a beam: streaks along v, a bright core across u."""
    W, H = 64, 128
    img = Image.new("RGB", (W, H))
    for y in range(H):
        for x in range(W):
            u = (x + .5) / W
            core = math.exp(-((u - .5) / .18) ** 2)
            streak = fbm(x / 3.0, y / 18.0, 3, 21)
            v = core * (.55 + .6 * streak) + .25 * math.exp(-((u - .5) / .06) ** 2)
            img.putpixel((x, y), grey(min(1, v))[:3])
    return bloom(img, 2, .6)


def judgment_blade():
    """The giant blade of light: point at the top, hilt at the bottom, runes down the fuller. Greyscale, additive."""
    W, H = 128, 512
    img = Image.new("RGB", (W, H))
    d = ImageDraw.Draw(img)
    cx = W / 2
    guard_y, pommel_y = H * .78, H * .985
    # Blade: tapered, with a bevelled edge
    d.polygon([(cx, 6), (cx + W * .2, H * .12), (cx + W * .17, guard_y), (cx - W * .17, guard_y), (cx - W * .2, H * .12)], fill=(150, 150, 150))
    d.polygon([(cx, 16), (cx + W * .1, H * .13), (cx + W * .08, guard_y - 6), (cx - W * .08, guard_y - 6), (cx - W * .1, H * .13)], fill=(230, 230, 230))
    d.line([(cx, 20), (cx, guard_y - 10)], fill=(255, 255, 255), width=4)
    for k in range(9):
        glyph(d, cx, H * .2 + k * H * .062, 18, 1200 + k, 3, (60, 60, 60))
    # Crossguard with hooked ends, grip, pommel
    d.polygon([(cx - W * .48, guard_y - 10), (cx + W * .48, guard_y - 10), (cx + W * .4, guard_y + 12), (cx - W * .4, guard_y + 12)], fill=(255, 255, 255))
    d.ellipse((cx - 12, guard_y - 13, cx + 12, guard_y + 13), fill=(120, 120, 120), outline=(255, 255, 255), width=3)
    d.rectangle((cx - 9, guard_y + 12, cx + 9, pommel_y - 22), fill=(170, 170, 170))
    for y in range(int(guard_y + 16), int(pommel_y - 24), 8):
        d.line([(cx - 9, y), (cx + 9, y + 4)], fill=(90, 90, 90), width=2)
    d.ellipse((cx - 18, pommel_y - 30, cx + 18, pommel_y + 4), fill=(255, 255, 255))
    return bloom(img.resize((64, 256), Image.LANCZOS), 2.5, .9)


def rune_wall():
    """A band of standing runes for the prison wall: bright at the foot, fading toward the top. Tiles along u."""
    W, H = 128, 128
    img = Image.new("RGB", (W, H))
    d = ImageDraw.Draw(img)
    for k in range(2):
        cx = W * (.25 + .5 * k)
        d.line([(cx - 26, H * .1), (cx - 26, H)], fill=(160, 160, 160), width=4)
        d.line([(cx + 26, H * .1), (cx + 26, H)], fill=(160, 160, 160), width=4)
        for j in range(3):
            glyph(d, cx, H * (.3 + .25 * j), 26, 1500 + k * 3 + j, 5)
    px = img.load()
    for y in range(H):
        f = (y / (H - 1)) ** 1.3
        for x in range(W):
            px[x, y] = tuple(int(v * (.25 + .75 * f)) for v in px[x, y])
    d = ImageDraw.Draw(img)
    d.line([(0, H - 3), (W, H - 3)], fill=(255, 255, 255), width=5)
    return bloom(img.resize((64, 64), Image.LANCZOS), 1.5, .8)


# ---------------------------------------------------------------- spell icons

def _icon_base(inner, outer, cx=8, cy=8):
    return g.spell_background(cx, cy, inner, outer)


def _px(img, x, y, col):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), col + (255,))


def crushing_meteor_icon():
    img = _icon_base((90, 30, 20), NIGHT, 6, 11)
    d = ImageDraw.Draw(img)
    # Flaming tail from the upper right
    for i in range(9):
        t = i / 8
        x, y = 14 - i * 1.1, 1 + i * 1.0
        col = g.lerp((255, 240, 160), (200, 40, 20), 1 - t)
        r = .6 + t * 1.6
        d.ellipse((x - r, y - r, x + r, y + r), fill=col + (255,))
    d.ellipse((2, 8, 9, 15), fill=(60, 30, 26, 255), outline=(30, 12, 10, 255))
    d.ellipse((3, 9, 7, 13), fill=(110, 50, 36, 255))
    _px(img, 4, 10, (255, 150, 60))
    _px(img, 6, 12, (255, 120, 40))
    _px(img, 7, 10, (255, 190, 80))
    return img


def judgment_blade_icon():
    img = _icon_base((120, 90, 30), NIGHT, 8, 6)
    d = ImageDraw.Draw(img)
    d.polygon([(7, 1), (9, 1), (9, 10), (8, 12), (7, 10)], fill=PALE + (255,))
    d.line([(8, 2), (8, 10)], fill=(255, 255, 255, 255))
    d.line([(5, 11), (11, 11)], fill=GOLD + (255,))
    d.line([(8, 12), (8, 14)], fill=GOLD_DARK + (255,))
    _px(img, 8, 15, GOLD)
    for x in range(2, 15):
        if x % 2 == 0:
            _px(img, x, 15, (255, 80, 60))
    return img


def annihilation_beam_icon():
    img = _icon_base((80, 10, 20), NIGHT, 4, 8)
    d = ImageDraw.Draw(img)
    d.ellipse((0, 4, 7, 11), outline=CRIMSON + (255,))
    d.ellipse((2, 6, 5, 9), fill=(255, 220, 220, 255))
    d.rectangle((5, 6, 15, 9), fill=CRIMSON + (255,))
    d.rectangle((5, 7, 15, 8), fill=(255, 245, 240, 255))
    for x in range(7, 16, 3):
        _px(img, x, 5, (255, 120, 120))
        _px(img, x + 1, 10, (255, 120, 120))
    return img


def triple_nova_icon():
    img = _icon_base((60, 12, 16), NIGHT)
    d = ImageDraw.Draw(img)
    d.ellipse((0, 0, 15, 15), outline=CRIMSON + (255,))
    d.ellipse((3, 3, 12, 12), outline=EMBER + (255,))
    d.ellipse((6, 6, 9, 9), fill=(255, 230, 180, 255))
    for x, y in ((7, 1), (1, 7), (14, 8), (8, 14)):
        _px(img, x, y, (255, 200, 120))
    return img


def pendulum_icon():
    def draw(d, img, S):
        # A crescent blade hanging from a long rod, swinging, a trail of light behind it
        d.line([(.5 * S, .04 * S), (.36 * S, .62 * S)], fill=_a(IRON_LIGHT), width=max(1, S // 22))
        d.ellipse((.42 * S, .0 * S, .58 * S, .1 * S), fill=_a(GOLD))
        d.pieslice((.08 * S, .38 * S, .64 * S, .94 * S), 20, 160, fill=_a((210, 214, 226)))
        d.pieslice((.16 * S, .44 * S, .56 * S, .76 * S), 20, 160, fill=_a(NIGHT))
        d.arc((.08 * S, .38 * S, .64 * S, .94 * S), 20, 160, fill=_a((255, 90, 90)), width=max(1, S // 30))
        d.arc((.3 * S, .4 * S, .96 * S, .96 * S), 30, 120, fill=_a(CRIMSON, 160), width=max(1, S // 26))
        d.ellipse((.31 * S, .56 * S, .41 * S, .66 * S), fill=_a(PALE))
        g.glow_spot(img, .36 * S, .78 * S, .2 * S, (255, 60, 70), .6)
    return _paint(draw, .5, .5, (60, 10, 20))


def seismic_rift_icon():
    img = _icon_base((80, 44, 20), NIGHT, 8, 12)
    d = ImageDraw.Draw(img)
    pts = [(0, 14), (4, 11), (6, 12), (9, 7), (11, 8), (15, 2)]
    d.line(pts, fill=(255, 140, 40, 255), width=2)
    d.line(pts, fill=(255, 230, 150, 255), width=1)
    for x, y in ((3, 8), (8, 4), (12, 5), (5, 14)):
        _px(img, x, y, EARTH)
        _px(img, x + 1, y - 1, (120, 70, 40))
    return img


def void_orb_icon():
    img = _icon_base((40, 10, 70), NIGHT)
    d = ImageDraw.Draw(img)
    d.ellipse((2, 2, 13, 13), fill=(6, 2, 12, 255), outline=VOID + (255,))
    d.arc((0, 0, 15, 15), 200, 330, fill=(200, 140, 255, 255))
    d.arc((0, 0, 15, 15), 20, 150, fill=VOID_DARK + (255,))
    _px(img, 6, 5, (180, 120, 255))
    _px(img, 5, 6, (120, 60, 200))
    return img


def cross_of_light_icon():
    img = _icon_base((110, 90, 40), NIGHT)
    d = ImageDraw.Draw(img)
    d.line([(2, 2), (13, 13)], fill=(200, 60, 60, 255))
    d.line([(13, 2), (2, 13)], fill=(200, 60, 60, 255))
    d.rectangle((7, 0, 8, 15), fill=GOLD + (255,))
    d.rectangle((0, 7, 15, 8), fill=GOLD + (255,))
    d.rectangle((6, 6, 9, 9), fill=(255, 250, 220, 255))
    return img


def rune_prison_icon():
    img = _icon_base((50, 16, 80), NIGHT)
    d = ImageDraw.Draw(img)
    d.ellipse((1, 9, 14, 15), outline=VOID + (255,))
    for x in (2, 5, 8, 11, 13):
        top = 3 if x in (5, 8, 11) else 5
        d.line([(x, top), (x, 12)], fill=(200, 150, 255, 255))
    d.ellipse((1, 2, 14, 7), outline=(140, 80, 220, 255))
    _px(img, 7, 9, (255, 255, 255))
    return img


def last_judgment_icon():
    img = _icon_base((110, 20, 30), NIGHT, 8, 7)
    d = ImageDraw.Draw(img)
    d.ellipse((0, 0, 15, 15), outline=GOLD + (255,))
    d.ellipse((2, 2, 13, 13), outline=CRIMSON + (255,))
    d.chord((3, 5, 12, 10), 0, 360, fill=PALE + (255,))
    d.ellipse((6, 6, 9, 9), fill=(160, 10, 20, 255))
    _px(img, 7, 7, (20, 0, 0))
    for k in range(8):
        a = k * math.tau / 8
        _px(img, int(round(7.5 + math.cos(a) * 7.4)), int(round(7.5 + math.sin(a) * 7.4)), (255, 240, 180))
    return img


# ---------------------------------------------------------------- items

def affinity_ring_boss():
    return g.from_grid([
        "................",
        "......oooo......",
        ".....orhro......",
        ".....orrro......",
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
    ], {"o": NIGHT, "h": (255, 230, 150), "l": GOLD, "b": GOLD_DARK, "s": (110, 70, 20),
        "r": CRIMSON})


def tyrants_codex_icon():
    return g.from_grid([
        "................",
        "...oooooooooo...",
        "..ohhllllllbco..",
        "..ohoooooooocco.",
        "..olo.yrry.obco.",
        "..olo.r..r.obco.",
        "..olo.reer.obcco",
        "..olo.r..r.obcco",
        "..olo.yrry.obcco",
        "..olo......obcco",
        "..olooooooobcco.",
        "..olbbbbbbbbcco.",
        "..osbbbbbbbsxco.",
        "...ooooooooooo..",
        "................",
        "................",
    ], {"o": (8, 4, 6), "h": GOLD, "l": BLOOD, "b": BLOOD_DARK, "s": (30, 4, 10),
        "c": g.CREAM, "x": g.CREAM_SHADE, "r": CRIMSON, "e": (255, 220, 200), "y": GOLD})


def tyrants_codex_model_texture():
    """64x64 texture of the 3D codex, laid out like the other books: blood-red leather, a glowing rune circle on the
    cover, blackened iron and gold fittings, a crimson gem."""
    s = 4
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    def box(u0, v0, u1, v1, fill):
        d.rectangle((u0 * s, v0 * s, u1 * s - 1, v1 * s - 1), fill=fill + (255,))

    W, H = 12 * s, 9 * s
    for y in range(H):
        for x in range(W):
            n = fbm(x / 5, y / 5, 3, 8)
            img.putpixel((x, y), g.lerp((52, 6, 14), (120, 16, 30), n) + (255,))
    # The rune circle burnt into the cover
    cover = Image.new("RGB", (W * 4, H * 4))
    cd = ImageDraw.Draw(cover)
    cx, cy, r = W * 2, H * 2, H * 1.6
    cd.ellipse((cx - r, cy - r, cx + r, cy + r), outline=(255, 90, 90), width=4)
    cd.ellipse((cx - r * .8, cy - r * .8, cx + r * .8, cy + r * .8), outline=(255, 90, 90), width=2)
    pts = [(cx + math.cos(-math.pi / 2 + k * math.tau / 7) * r * .78, cy + math.sin(-math.pi / 2 + k * math.tau / 7) * r * .78) for k in range(7)]
    k = 0
    for _ in range(7):
        cd.line([pts[k], pts[(k + 3) % 7]], fill=(255, 120, 100), width=2)
        k = (k + 3) % 7
    cd.ellipse((cx - r * .2, cy - r * .2, cx + r * .2, cy + r * .2), fill=(255, 220, 200))
    cover = cover.resize((W, H), Image.LANCZOS)
    glow = cover.filter(ImageFilter.GaussianBlur(1))
    cp, gp = cover.load(), glow.load()
    for y in range(H):
        for x in range(W):
            add = tuple(min(255, int(cp[x, y][i] + gp[x, y][i] * .7)) for i in range(3))
            if sum(add) > 40:
                base = img.getpixel((x, y))
                img.putpixel((x, y), tuple(min(255, base[i] + add[i]) for i in range(3)) + (255,))
    d.rectangle((0, 0, W - 1, H - 1), outline=(10, 2, 4, 255))
    d.rectangle((1, 1, W - 2, H - 2), outline=GOLD_DARK + (255,))
    d.rectangle((2, 2, W - 3, H - 3), outline=IRON + (255,))
    # Gold corner pieces
    for x0, y0 in ((3, 3), (W - 7, 3), (3, H - 7), (W - 7, H - 7)):
        d.rectangle((x0, y0, x0 + 3, y0 + 3), fill=GOLD + (255,))
    box(12, 0, 13, 9, BLOOD_DARK)
    d.line((12 * s, 0, 12 * s, H - 1), fill=GOLD + (255,))
    # Pages: old parchment
    for y in range(0, 8 * s):
        for x in range(13 * s, 16 * s):
            img.putpixel((x, y), g.lerp(g.CREAM, (180, 150, 120), fbm(x / 3, y / 3, 2, 9) * .7) + (255,))
    for y in range(1, 8 * s, 2):
        d.line((13 * s, y, 16 * s - 1, y), fill=g.CREAM_SHADE + (255,))
    # Spine
    for y in range(9 * s, 12 * s):
        for x in range(0, 12 * s):
            t = abs(y - 10.5 * s) / (1.5 * s)
            img.putpixel((x, y), g.lerp((110, 14, 28), (40, 4, 10), t) + (255,))
    d.line((0, 10 * s + 1, 12 * s - 1, 10 * s + 1), fill=GOLD + (255,))
    # Page edges
    box(0, 12, 12, 15, g.CREAM)
    for x in range(0, 12 * s, 2):
        d.line((x, 12 * s, x, 15 * s - 1), fill=g.CREAM_SHADE + (255,))
    # Fittings: black iron edged in gold
    box(12, 9, 14, 11, IRON)
    d.rectangle((12 * s, 9 * s, 14 * s - 1, 11 * s - 1), outline=GOLD_DARK + (255,))
    d.line((12 * s + 1, 9 * s + 1, 14 * s - 2, 9 * s + 1), fill=IRON_LIGHT + (255,))
    # Crimson gem
    for y in range(9 * s, 11 * s):
        for x in range(14 * s, 16 * s):
            t = math.hypot(x - 14.8 * s, y - 9.8 * s) / (1.4 * s)
            img.putpixel((x, y), g.lerp((255, 200, 200), BLOOD, t ** .8) + (255,))
    # Strap with gold studs
    box(12, 11, 16, 12, (44, 8, 14))
    for x in range(12 * s + 1, 16 * s, 3):
        img.putpixel((x, 11 * s + 1), GOLD + (255,))
        img.putpixel((x, 12 * s - 2), GOLD + (255,))
    return img



# ---------------------------------------------------------------- the ten new attacks' art

def _np_rgba(rgb, alpha):
    return Image.fromarray(np.dstack([np.clip(rgb, 0, 255), np.clip(alpha, 0, 255)]).astype(np.uint8), "RGBA")


def _wnoise(w, h, cells, seed):
    """Smooth value noise in [0, 1], tiling across and down."""
    rnd = np.random.default_rng(seed)
    grid = rnd.random((cells[1], cells[0]))
    ys, xs = np.mgrid[0:h, 0:w]
    gx, gy = xs / w * cells[0], ys / h * cells[1]
    x0, y0 = gx.astype(int), gy.astype(int)
    fx, fy = gx - x0, gy - y0
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    x1, y1 = (x0 + 1) % cells[0], (y0 + 1) % cells[1]
    a = grid[y0, x0] * (1 - fx) + grid[y0, x1] * fx
    b = grid[y1, x0] * (1 - fx) + grid[y1, x1] * fx
    return a * (1 - fy) + b * fy


def _wfbm(w, h, base, octaves, seed):
    total, amp, norm = np.zeros((h, w)), 1.0, 0.0
    for o in range(octaves):
        c = base * 2 ** o
        total += _wnoise(w, h, (c, max(1, c * h // w)), seed + o * 17) * amp
        norm += amp
        amp *= .5
    return total / norm


def moon_texture():
    """512 x 256, wrapping round (an equirectangular map): grey dust, darker seas, craters with bright rims and their rays."""
    W, H = 512, 256
    ys, xs = np.mgrid[0:H, 0:W]
    lon = (xs + .5) / W * math.tau
    lat = ((ys + .5) / H - .5) * math.pi
    # Points on the sphere, for craters that are round on it
    px, py, pz = np.cos(lat) * np.cos(lon), np.sin(lat), np.cos(lat) * np.sin(lon)
    dust = _wfbm(W, H, 4, 5, 2101)
    seas = np.clip((_wfbm(W, H, 2, 3, 2102) - .52) * 4, 0, 1)
    tone = .62 + .22 * (dust - .5) * 2 - .22 * seas
    rnd = random.Random(2103)
    for k in range(170):
        # A crater: a dark floor, a bright rim, a faint halo of rays for the young ones
        cl, cn = rnd.uniform(-1.3, 1.3), rnd.uniform(0, math.tau)
        cx, cy, cz = math.cos(cl) * math.cos(cn), math.sin(cl), math.cos(cl) * math.sin(cn)
        r = rnd.choice((.025, .035, .05, .06, .08, .1, .13, .18, .25)) * rnd.uniform(.7, 1.1)
        d = np.arccos(np.clip(px * cx + py * cy + pz * cz, -1, 1)) / r
        floor = np.clip(1 - d, 0, 1)
        rim = np.exp(-((d - 1) / .18) ** 2)
        tone = tone - .16 * (floor > 0) * (.6 + .4 * floor) + .2 * rim
        if r < .12 and rnd.random() < .35:
            tone = tone + .1 * np.clip(1 - (d - 1) / 3, 0, 1) * (d > 1)
    tone = np.clip(tone + (_wfbm(W, H, 32, 3, 2106) - .5) * .12, 0, 1)
    rgb = np.dstack([tone * 236 + 10, tone * 230 + 12, tone * 222 + 18])
    return _np_rgba(rgb, np.full((H, W), 255.0))


def moon_cracks_texture():
    """512 x 256, wrapping round like the moon's map: the glowing cracks of a moon about to burst (white on black,
    drawn as light): the edges of big cells, brightest where they meet."""
    W, H = 512, 256
    rnd = np.random.default_rng(2104)
    ys, xs = np.mgrid[0:H, 0:W]
    pts = rnd.random((70, 2)) * (W, H)
    best = np.full((H, W), 1e9)
    second = np.full((H, W), 1e9)
    for (x, y) in pts:
        for ox in (-W, 0, W):
            d = np.hypot(xs - x - ox, (ys - y) * 1.6)
            second = np.where(d < best, best, np.minimum(second, d))
            best = np.minimum(best, d)
    edge = np.clip(1 - (second - best) / 6, 0, 1) ** 2
    wobble = _wfbm(W, H, 8, 3, 2105)
    v = np.clip(edge * (.6 + .8 * wobble), 0, 1)
    return _np_rgba(np.dstack([v * 255] * 3), np.full((H, W), 255.0))


def disc_texture():
    """64 x 64: a white disc, its edge soft over a pixel or two (tinted in game: the dark of an eclipse)."""
    S2 = 64
    ys, xs = np.mgrid[0:S2, 0:S2]
    r = np.hypot(xs + .5 - S2 / 2, ys + .5 - S2 / 2) / (S2 / 2)
    a = np.clip((1 - r) * 18, 0, 1)
    return _np_rgba(np.full((S2, S2, 3), 255.0), a * 255)


def spike_texture():
    """32 x 64: the skin of a fang or a thorn, pale (tinted in game), from its root (V 1) to its point (V 0): a glossy
    streak down one side, fine streaks along it, rings of growth near the root (the game shades it round)."""
    W, H = 32, 64
    ys, xs = np.mgrid[0:H, 0:W]
    u, v = (xs + .5) / W, (ys + .5) / H
    round_ = .86 + .14 * np.cos((u - .3) * math.tau)
    gloss = np.exp(-((u - .3) / .06) ** 2) * .25
    streak = (_wnoise(W, H, (8, 2), 2106) - .5) * .25
    rings = .08 * np.clip(np.sin(v * 40) * 2 - 1, 0, 1) * np.clip((v - .55) * 3, 0, 1)
    t = np.clip(round_ * .95 + gloss + streak * .6 - rings + .05 * (1 - v), 0, 1)
    t = np.round(t * 8) / 8
    return _np_rgba(np.dstack([t * 255] * 3), np.full((H, W), 255.0))


def feather_texture():
    """32 x 64: a long feather of light, white (tinted in game), its quill at V 1 and its tip at V 0: a bright shaft
    down its middle, the vane either side combed into barbs sweeping toward the tip, a few splits in it."""
    W, H = 64, 128
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px = img.load()
    rnd = random.Random(2107)
    splits = [rnd.uniform(.25, .8) for _ in range(3)]
    for y in range(H):
        v = (y + .5) / H
        # Width of the vane along it: round tip, widest a third down, narrowing to the bare quill
        half = .46 * math.sin(math.pi * min(1, (v + .02) / .92) ** .8) * (1 if v < .86 else max(0, (1 - v) / .14))
        for x in range(W):
            u = (x + .5) / W * 2 - 1
            if abs(u) > half:
                continue
            k = abs(u) / max(half, 1e-3)
            barb = .5 + .5 * math.cos((v * 70 + abs(u) * 26) * 1)
            split = any(abs(v + abs(u) * .35 - s) < .008 for s in splits)
            a = (1 - k ** 3) * (.55 + .25 * barb) * (0 if split else 1)
            shade = .7 + .3 * (1 - k) + .15 * barb
            px[x, y] = (int(255 * min(1, shade)), int(255 * min(1, shade)), int(255 * min(1, shade)), int(255 * min(1, a)))
        # The shaft
        if v < .97:
            for x in (W // 2 - 1, W // 2):
                px[x, y] = (255, 255, 255, 255)
    return img.resize((32, 64), Image.LANCZOS)


def chain_texture():
    """32 x 64: one link of a chain seen flat, dark iron with a bright edge where it catches the light (an oval ring,
    see-through outside and in its hole)."""
    W, H = 32, 64
    ys, xs = np.mgrid[0:H, 0:W]
    x = (xs + .5 - W / 2) / (W / 2)
    y = (ys + .5 - H / 2) / (H / 2)
    # An oval with straight sides: a stadium
    yy = np.clip(np.abs(y) - .45, 0, None) / .55
    d = np.hypot(x, yy * 1.0)
    ring = (d < .98) & (d > .52)
    band = np.clip((d - .52) / .46, 0, 1)
    light = .35 + .65 * np.clip(np.sin(band * math.pi), 0, 1) ** 1.5
    light = light * (.8 + .2 * np.clip(-x - y * .5, -1, 1))
    t = np.round(np.clip(light, 0, 1) * 7) / 7
    rgb = np.dstack([t * 150 + 30, t * 140 + 28, t * 150 + 36])
    return _np_rgba(rgb, ring * 255.0)


def iron_texture():
    """32 x 32, tiling: plates of black iron, riveted, worn bright at their edges."""
    W = 32
    ys, xs = np.mgrid[0:W, 0:W]
    n = _wnoise(W, W, (8, 8), 2108)
    plate = ((xs % 16 == 0) | (ys % 16 == 0)) * 1.0
    rivet = (((xs % 16 - 3) ** 2 + (ys % 16 - 3) ** 2) <= 1) * 1.0 + (((xs % 16 - 12) ** 2 + (ys % 16 - 12) ** 2) <= 1) * 1.0
    t = .3 + .25 * n - .18 * plate + .45 * rivet + .2 * ((xs % 16 == 1) | (ys % 16 == 1))
    t = np.round(np.clip(t, 0, 1) * 7) / 7
    rgb = np.dstack([t * 150 + 22, t * 130 + 18, t * 140 + 24])
    return _np_rgba(rgb, np.full((W, W), 255.0))


def sword_texture():
    """32 x 64: the blade of a sword of light, white (tinted in game) from its hilt (V 1) to its point (V 0): bright
    edges, a groove down its middle with runes in it, the steel between half see-through."""
    W, H = 32, 64
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px = img.load()
    for y in range(H):
        for x in range(W):
            u = abs((x + .5) / W * 2 - 1)
            edge = math.exp(-((u - .92) / .07) ** 2)
            groove = math.exp(-(u / .1) ** 2)
            a = .45 + .5 * edge + .3 * groove
            t = .65 + .35 * edge + .25 * groove
            px[x, y] = (int(255 * min(1, t)), int(255 * min(1, t)), int(255 * min(1, t)), int(255 * min(1, a)))
    d = ImageDraw.Draw(img)
    rnd = random.Random(2109)
    y = 10
    while y < H - 8:
        h = rnd.choice((3, 4))
        shape = rnd.randrange(4)
        if shape == 0:
            d.line([(16, y), (16, y + h)], fill=(255, 255, 255, 255))
        elif shape == 1:
            d.line([(15, y), (17, y + h)], fill=(255, 255, 255, 255))
        elif shape == 2:
            d.line([(15, y), (17, y), (16, y + h)], fill=(255, 255, 255, 255))
        else:
            d.point([(15, y), (17, y + 1), (16, y + h)], fill=(255, 255, 255, 255))
        y += h + 2
    return img


def prism_texture():
    """32 x 32: the inside of a crystal, white (tinted in game): faint planes of light crossing it, half see-through."""
    W = 32
    ys, xs = np.mgrid[0:W, 0:W]
    u, v = (xs + .5) / W, (ys + .5) / W
    lines = np.zeros((W, W))
    rnd = np.random.default_rng(2110)
    for _ in range(7):
        a = rnd.random() * math.pi
        o = rnd.random()
        dd = np.abs((u - .5) * math.cos(a) + (v - .5) * math.sin(a) - (o - .5) * .8)
        lines += np.exp(-(dd / .02) ** 2) * rnd.uniform(.3, .8)
    t = np.clip(.55 + .45 * lines + .15 * (1 - v), 0, 1)
    a = np.clip(.35 + .5 * lines + .25 * np.abs(u - .5) * 2, 0, 1)
    t = np.round(t * 8) / 8
    return _np_rgba(np.dstack([t * 255] * 3), a * 255)


def new_attack_textures():
    g.save(moon_texture(), "textures/entity/boss/moon.png")
    g.save(moon_cracks_texture(), "textures/entity/boss/moon_cracks.png")
    g.save(disc_texture(), "textures/entity/boss/disc.png")
    g.save(spike_texture(), "textures/entity/boss/spike.png")
    g.save(feather_texture(), "textures/entity/boss/feather.png")
    g.save(chain_texture(), "textures/entity/boss/chain.png")
    g.save(iron_texture(), "textures/entity/boss/iron.png")
    g.save(sword_texture(), "textures/entity/boss/sword.png")
    g.save(prism_texture(), "textures/entity/boss/prism.png")



# ---------------------------------------------------------------- the ten new attacks' icons (painted, 64 then 16)

def _a(c, a=255):
    return tuple(int(x) for x in c) + (a,)


def _paint(draw, cx, cy, inner, outer=NIGHT):
    return g.painted(64, draw, (cx, cy, inner, outer), 16)


def thousand_blades_icon():
    def draw(d, img, S):
        for k in range(5):
            x = .16 + k * .17
            top = .1 + .08 * abs(k - 2)
            d.polygon([(x * S, top * S), ((x + .05) * S, (top + .1) * S), ((x + .04) * S, .66 * S), ((x - .04) * S, .66 * S), ((x - .05) * S, (top + .1) * S)],
                      fill=_a(PALE))
            d.line([(x * S, (top + .06) * S), (x * S, .62 * S)], fill=_a((255, 255, 255)), width=max(1, S // 40))
            d.rectangle(((x - .08) * S, .66 * S, (x + .08) * S, .7 * S), fill=_a(GOLD))
            d.rectangle(((x - .02) * S, .7 * S, (x + .02) * S, .82 * S), fill=_a(GOLD_DARK))
        g.glow_spot(img, .5 * S, .4 * S, .45 * S, (255, 210, 90), .6)
    return _paint(draw, .5, .35, (110, 90, 40))


def executioners_wheel_icon():
    def draw(d, img, S):
        c, r = .5 * S, .3 * S
        for k in range(8):
            a = k * math.tau / 8
            pts = []
            for i in range(7):
                t = i / 6
                aa = a + .55 * t
                rr = r * (1 + .5 * t)
                pts.append((c + math.cos(aa) * rr, c + math.sin(aa) * rr))
            d.line(pts, fill=_a((210, 200, 205)), width=max(2, S // 22))
            d.line(pts, fill=_a((255, 120, 90)), width=max(1, S // 64))
        d.ellipse((c - r, c - r, c + r, c + r), outline=_a(IRON_LIGHT), width=max(2, S // 14))
        for k in range(6):
            a = k * math.tau / 6
            d.line([(c, c), (c + math.cos(a) * r, c + math.sin(a) * r)], fill=_a(IRON_LIGHT), width=max(1, S // 30))
        d.ellipse((c - .09 * S, c - .09 * S, c + .09 * S, c + .09 * S), fill=_a(CRIMSON))
        g.glow_spot(img, c, c, .2 * S, (255, 80, 50), .8)
    return _paint(draw, .5, .5, (90, 20, 16))


def abyssal_maw_icon():
    def draw(d, img, S):
        d.ellipse((.12 * S, .5 * S, .88 * S, .9 * S), fill=_a((20, 6, 34)), outline=_a(VOID), width=max(1, S // 30))
        d.ellipse((.3 * S, .6 * S, .7 * S, .8 * S), fill=_a((90, 40, 160)))
        for k in range(7):
            x = .17 + k * .11
            h = .32 + .1 * math.sin(k * 1.7)
            lean = (.5 - x) * .25
            d.polygon([((x - .04) * S, .7 * S), ((x + .04) * S, .7 * S), ((x + lean) * S, (.7 - h) * S)], fill=_a((236, 226, 206)))
            d.polygon([((x - .04) * S, .7 * S), ((x + .04) * S, .7 * S), ((x + lean * .5) * S, .62 * S)], fill=_a((70, 30, 90)))
    return _paint(draw, .5, .7, (60, 20, 90))


def divine_prism_icon():
    def draw(d, img, S):
        cols = [(255, 74, 74), (255, 154, 48), (255, 226, 74), (90, 255, 122), (74, 180, 255), (184, 106, 255)]
        for k, col in enumerate(cols):
            a = math.radians(35 + k * 22)
            d.line([(.5 * S, .5 * S), ((.5 + math.cos(a) * .62) * S, (.5 + math.sin(a) * .62) * S)], fill=_a(col), width=max(2, S // 20))
        d.polygon([(.5 * S, .06 * S), (.66 * S, .3 * S), (.5 * S, .54 * S), (.34 * S, .3 * S)], fill=_a((220, 240, 255)), outline=_a((255, 255, 255)))
        d.line([(.5 * S, .06 * S), (.5 * S, .54 * S)], fill=_a((160, 200, 255)), width=max(1, S // 48))
        g.glow_spot(img, .5 * S, .3 * S, .22 * S, (255, 255, 255), .7)
    return _paint(draw, .5, .3, (60, 60, 100))


def chains_of_the_damned_icon():
    def draw(d, img, S):
        for k in range(4):
            x0 = (.08, .92)[k % 2]
            y0 = (.2, .85)[k // 2]
            for i in range(5):
                t = i / 4
                x = x0 + (.5 - x0) * t
                y = y0 + (.48 - y0) * t
                w = .07 if i % 2 == 0 else .035
                d.ellipse(((x - w) * S, (y - .035) * S, (x + w) * S, (y + .035) * S), outline=_a((170, 160, 170)), width=max(1, S // 40))
        d.ellipse((.36 * S, .34 * S, .64 * S, .62 * S), outline=_a(CRIMSON), width=max(2, S // 22))
        d.ellipse((.45 * S, .43 * S, .55 * S, .53 * S), fill=_a((255, 200, 160)))
        g.glow_spot(img, .5 * S, .48 * S, .2 * S, (255, 60, 50), .8)
    return _paint(draw, .5, .48, (90, 14, 20))


def pentacle_of_thorns_icon():
    def draw(d, img, S):
        pts = [(.5 + math.cos(-math.pi / 2 + j * math.tau / 5) * .4, .53 + math.sin(-math.pi / 2 + j * math.tau / 5) * .4) for j in range(5)]
        for j in range(5):
            a, b = pts[(j * 2) % 5], pts[(j * 2 + 2) % 5]
            d.line([(a[0] * S, a[1] * S), (b[0] * S, b[1] * S)], fill=_a((255, 70, 80)), width=max(2, S // 26))
            for i in range(1, 4):
                t = i / 4
                x, y = a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t
                d.polygon([((x - .03) * S, y * S), ((x + .03) * S, y * S), (x * S, (y - .1) * S)], fill=_a((200, 30, 50)))
        g.glow_spot(img, .5 * S, .53 * S, .14 * S, (255, 80, 90), .8)
    return _paint(draw, .5, .53, (80, 10, 20))


def seraph_wings_icon():
    def draw(d, img, S):
        for sd in (-1, 1):
            for k in range(6):
                a = math.radians(-20 - k * 22)
                x0, y0 = .5 + sd * .06, .38
                ln = .42 - k * .035
                x1 = x0 + sd * math.cos(a) * ln
                y1 = y0 + math.sin(-a) * ln * .9
                d.line([(x0 * S, y0 * S), (x1 * S, y1 * S)], fill=_a((255, 238, 190) if k % 2 else GOLD), width=max(2, S // 16))
        d.ellipse((.4 * S, .12 * S, .6 * S, .2 * S), outline=_a(GOLD), width=max(1, S // 32))
        g.glow_spot(img, .5 * S, .4 * S, .25 * S, (255, 220, 120), .8)
    return _paint(draw, .5, .4, (110, 90, 40))


def wrath_of_the_heavens_icon():
    def draw(d, img, S):
        for k in range(7):
            x = .12 + k * .13
            d.ellipse(((x - .14) * S, (.1 + .05 * (k % 2)) * S, (x + .14) * S, (.34 + .05 * (k % 2)) * S), fill=_a((60, 66, 84)))
        bolt = [(.55, .3), (.42, .55), (.54, .55), (.4, .9)]
        d.line([(x * S, y * S) for x, y in bolt], fill=_a((140, 180, 255)), width=max(3, S // 14))
        d.line([(x * S, y * S) for x, y in bolt], fill=_a((255, 255, 255)), width=max(1, S // 32))
        g.glow_spot(img, .47 * S, .6 * S, .22 * S, (120, 160, 255), .7)
    return _paint(draw, .5, .6, (30, 40, 80))


def eclipse_icon():
    def draw(d, img, S):
        g.glow_spot(img, .5 * S, .5 * S, .48 * S, (255, 210, 120), 1)
        for k in range(12):
            a = k * math.tau / 12
            d.line([((.5 + math.cos(a) * .26) * S, (.5 + math.sin(a) * .26) * S), ((.5 + math.cos(a) * (.42 + .05 * (k % 2))) * S,
                    (.5 + math.sin(a) * (.42 + .05 * (k % 2))) * S)], fill=_a((255, 230, 170) if k % 3 else (190, 120, 255)), width=max(1, S // 40))
        d.ellipse((.25 * S, .25 * S, .75 * S, .75 * S), fill=_a((255, 246, 222)))
        d.ellipse((.27 * S, .27 * S, .73 * S, .73 * S), fill=_a((8, 2, 14)))
    return _paint(draw, .5, .5, (60, 30, 90))


def fallen_moon_icon():
    def draw(d, img, S):
        for i in range(8):
            t = i / 7
            x, y = .78 - t * .2, .08 + t * .3
            r = .05 + t * .08
            d.ellipse(((x - r) * S, (y - r) * S, (x + r) * S, (y + r) * S), fill=_a(g.lerp((255, 220, 140), (255, 90, 40), t)))
        d.ellipse((.18 * S, .3 * S, .72 * S, .84 * S), fill=_a((176, 172, 168)))
        for x, y, r in ((.34, .45, .06), (.55, .62, .08), (.38, .7, .04), (.58, .42, .04)):
            d.ellipse(((x - r) * S, (y - r) * S, (x + r) * S, (y + r) * S), fill=_a((120, 116, 114)))
        d.arc((.18 * S, .3 * S, .72 * S, .84 * S), 20, 160, fill=_a((255, 120, 50)), width=max(2, S // 20))
        d.rectangle((0, .9 * S, S, S), fill=_a((90, 60, 40)))
    return _paint(draw, .45, .55, (70, 40, 60))


NEW_ICONS = (("thousand_blades", thousand_blades_icon), ("executioners_wheel", executioners_wheel_icon), ("abyssal_maw", abyssal_maw_icon),
             ("divine_prism", divine_prism_icon), ("chains_of_the_damned", chains_of_the_damned_icon), ("pentacle_of_thorns", pentacle_of_thorns_icon),
             ("seraph_wings", seraph_wings_icon), ("wrath_of_the_heavens", wrath_of_the_heavens_icon), ("eclipse", eclipse_icon),
             ("fallen_moon", fallen_moon_icon))


# ---------------------------------------------------------------- the third ten attacks' icons (painted, 64 then 16)

def bell_of_doom_icon():
    def draw(d, img, S):
        d.polygon([(.3 * S, .62 * S), (.34 * S, .3 * S), (.42 * S, .2 * S), (.58 * S, .2 * S), (.66 * S, .3 * S), (.7 * S, .62 * S), (.82 * S, .74 * S),
                   (.18 * S, .74 * S)], fill=_a((176, 120, 60)), outline=_a(GOLD))
        d.ellipse((.44 * S, .1 * S, .56 * S, .22 * S), outline=_a(GOLD), width=max(1, S // 30))
        d.ellipse((.45 * S, .72 * S, .55 * S, .82 * S), fill=_a((90, 60, 30)))
        for k in range(2):
            r = .38 + k * .08
            d.arc(((.5 - r) * S, (.78 - r * .3) * S, (.5 + r) * S, (.78 + r * .3) * S), 0, 180, fill=_a(GOLD if k == 0 else PALE), width=max(1, S // 30))
        g.glow_spot(img, .5 * S, .45 * S, .3 * S, (255, 210, 90), .5)
    return _paint(draw, .5, .45, (90, 60, 20))


def infernal_checkerboard_icon():
    def draw(d, img, S):
        for gx in range(4):
            for gy in range(4):
                x0, y0 = .1 + gx * .2, .1 + gy * .2
                if (gx + gy) % 2 == 0:
                    d.rectangle((x0 * S, y0 * S, (x0 + .2) * S, (y0 + .2) * S), fill=_a(CRIMSON))
                    d.polygon([((x0 + .04) * S, (y0 + .18) * S), ((x0 + .1) * S, (y0 + .02) * S), ((x0 + .16) * S, (y0 + .18) * S)], fill=_a(EMBER))
                else:
                    d.rectangle((x0 * S, y0 * S, (x0 + .2) * S, (y0 + .2) * S), fill=_a(VOID_DARK))
        g.glow_spot(img, .5 * S, .5 * S, .4 * S, (255, 90, 40), .4)
    return _paint(draw, .5, .5, (60, 10, 20))


def titans_fist_icon():
    def draw(d, img, S):
        d.rectangle((.36 * S, .02 * S, .64 * S, .44 * S), fill=_a(IRON_LIGHT), outline=_a(IRON))
        d.rectangle((.36 * S, .4 * S, .64 * S, .44 * S), fill=_a(GOLD))
        for k in range(4):
            d.rectangle(((.28 + k * .11) * S, .44 * S, (.38 + k * .11) * S, .66 * S), fill=_a((150, 140, 150)), outline=_a(IRON))
        d.line([(.1 * S, .82 * S), (.9 * S, .82 * S)], fill=_a(EARTH), width=max(2, S // 20))
        for k in range(5):
            a = math.pi + k * math.pi / 4
            d.line([(.5 * S, .8 * S), ((.5 + math.cos(a) * .4) * S, (.8 + math.sin(a) * .12) * S)], fill=_a(EMBER), width=max(1, S // 40))
        g.glow_spot(img, .5 * S, .78 * S, .2 * S, (255, 140, 60), .8)
    return _paint(draw, .5, .7, (80, 40, 20))


def toppling_columns_icon():
    def draw(d, img, S):
        # A column toppling, a second still standing behind it, dust where it lands
        d.polygon([(.12 * S, .78 * S), (.2 * S, .7 * S), (.82 * S, .22 * S), (.9 * S, .3 * S)], fill=_a((236, 230, 218)))
        for k in range(5):
            t = .2 + k * .15
            x, y = .16 + (.86 - .16) * t, .74 + (.26 - .74) * t
            d.line([((x - .03) * S, (y - .03) * S), ((x + .03) * S, (y + .03) * S)], fill=_a((150, 146, 140)), width=max(1, S // 40))
        d.rectangle((.06 * S, .76 * S, .26 * S, .84 * S), fill=_a((210, 204, 194)))
        d.rectangle((.78 * S, .14 * S, .96 * S, .22 * S), fill=_a((210, 204, 194)))
        d.rectangle((.6 * S, .3 * S, .7 * S, .86 * S), fill=_a((200, 194, 184)))
        d.rectangle((.56 * S, .26 * S, .74 * S, .31 * S), fill=_a((220, 214, 204)))
        d.line([(.2 * S, .66 * S), (.36 * S, .58 * S), (.3 * S, .5 * S)], fill=_a((255, 220, 120)), width=max(1, S // 40))
        g.glow_spot(img, .3 * S, .6 * S, .16 * S, (255, 220, 120), .6)
        d.ellipse((.02 * S, .82 * S, .4 * S, .96 * S), fill=_a((200, 190, 170), 150))
    return _paint(draw, .5, .5, (70, 60, 40))


def reapers_scythe_icon():
    def draw(d, img, S):
        d.line([(.22 * S, .92 * S), (.62 * S, .14 * S)], fill=_a((60, 40, 70)), width=max(2, S // 16))
        pts = [((.62 + .32 * math.cos(a)) * S, (.14 + .32 * math.sin(a) * .7 + .12) * S) for a in [math.pi * (1.05 + .9 * t / 10) for t in range(11)]]
        pts2 = [((.62 + .22 * math.cos(a)) * S, (.14 + .2 * math.sin(a) * .7 + .12) * S) for a in [math.pi * (1.05 + .9 * t / 10) for t in range(11)]]
        d.polygon(pts + pts2[::-1], fill=_a((210, 190, 255)), outline=_a(VOID))
        g.glow_spot(img, .5 * S, .25 * S, .3 * S, (160, 80, 255), .7)
    return _paint(draw, .5, .3, (50, 14, 80))


def iron_maidens_icon():
    def draw(d, img, S):
        # An iron casket, its doors open, spikes inside, red light in its hollow
        body = [(.5 * S, .08 * S), (.62 * S, .14 * S), (.64 * S, .28 * S), (.7 * S, .36 * S), (.66 * S, .6 * S), (.62 * S, .92 * S), (.38 * S, .92 * S),
                (.34 * S, .6 * S), (.3 * S, .36 * S), (.36 * S, .28 * S), (.38 * S, .14 * S)]
        d.polygon(body, fill=_a((40, 34, 44)))
        d.polygon([(.4 * S, .36 * S), (.6 * S, .36 * S), (.58 * S, .88 * S), (.42 * S, .88 * S)], fill=_a((150, 20, 30)))
        for k in range(4):
            y = (.44 + k * .11) * S
            d.line([(.42 * S, y), (.48 * S, y)], fill=_a(GOLD), width=max(1, S // 40))
            d.line([(.58 * S, y), (.52 * S, y)], fill=_a(GOLD), width=max(1, S // 40))
        d.polygon([(.3 * S, .36 * S), (.14 * S, .42 * S), (.18 * S, .86 * S), (.34 * S, .88 * S)], fill=_a((58, 50, 62)))
        d.polygon([(.7 * S, .36 * S), (.86 * S, .42 * S), (.82 * S, .86 * S), (.66 * S, .88 * S)], fill=_a((58, 50, 62)))
        d.line([(.44 * S, .2 * S), (.48 * S, .2 * S)], fill=_a((255, 60, 60)), width=max(1, S // 40))
        d.line([(.52 * S, .2 * S), (.56 * S, .2 * S)], fill=_a((255, 60, 60)), width=max(1, S // 40))
        for x in (.4, .5, .6):
            d.line([(x * S, .1 * S), (x * S, .02 * S)], fill=_a(GOLD), width=max(1, S // 40))
        g.glow_spot(img, .5 * S, .6 * S, .2 * S, (255, 40, 50), .7)
    return _paint(draw, .5, .5, (60, 10, 20))


def spectral_kings_icon():
    def draw(d, img, S):
        for k, (x, y) in enumerate(((.25, .3), (.75, .3), (.25, .75), (.75, .75))):
            d.rectangle(((x - .07) * S, (y - .05) * S, (x + .07) * S, (y + .14) * S), fill=_a((60, 40, 90)))
            d.rectangle(((x - .05) * S, (y - .15) * S, (x + .05) * S, (y - .05) * S), fill=_a((90, 70, 120)))
            d.polygon([((x - .06) * S, (y - .15) * S), ((x - .03) * S, (y - .22) * S), (x * S, (y - .16) * S), ((x + .03) * S, (y - .22) * S),
                       ((x + .06) * S, (y - .15) * S)], fill=_a(GOLD))
        d.line([(.25 * S, .5 * S), (.75 * S, .5 * S)], fill=_a(VOID), width=max(1, S // 30))
        d.line([(.5 * S, .2 * S), (.5 * S, .9 * S)], fill=_a(VOID), width=max(1, S // 30))
        g.glow_spot(img, .5 * S, .52 * S, .15 * S, (170, 90, 255), .9)
    return _paint(draw, .5, .5, (40, 14, 70))


def clock_of_fate_icon():
    def draw(d, img, S):
        c = .5 * S
        d.ellipse((.1 * S, .1 * S, .9 * S, .9 * S), fill=_a((26, 18, 34)), outline=_a(GOLD), width=max(1, S // 22))
        d.ellipse((.17 * S, .17 * S, .83 * S, .83 * S), outline=_a(GOLD_DARK), width=max(1, S // 60))
        for k in range(12):
            a = k * math.tau / 12 - math.pi / 2
            r0, r1 = .36 * S, (.27 if k % 3 == 0 else .31) * S
            d.line([(c + math.cos(a) * r0, c + math.sin(a) * r0), (c + math.cos(a) * r1, c + math.sin(a) * r1)], fill=_a(GOLD), width=max(1, S // 40))
        # A burning hour under the minute hand
        d.pieslice((.14 * S, .14 * S, .86 * S, .86 * S), -60, -30, fill=_a((255, 170, 60), 150))
        a = -math.pi / 2 + math.tau * 1.5 / 12
        d.line([(c, c), (c + math.cos(a) * .36 * S, c + math.sin(a) * .36 * S)], fill=_a((255, 236, 160)), width=max(1, S // 24))
        b = -math.pi / 2 - math.tau / 12
        d.line([(c, c), (c + math.cos(b) * .22 * S, c + math.sin(b) * .22 * S)], fill=_a(GOLD), width=max(1, S // 18))
        d.ellipse((c - .05 * S, c - .05 * S, c + .05 * S, c + .05 * S), fill=_a(GOLD))
        g.glow_spot(img, c + .14 * S, c - .2 * S, .18 * S, (255, 190, 90), .7)
    return _paint(draw, .5, .5, (70, 50, 20))


def crimson_lotus_icon():
    def draw(d, img, S):
        c = .5 * S
        for k in range(6):
            a = k * math.tau / 6 - math.pi / 2
            tip = (c + math.cos(a) * .42 * S, c + math.sin(a) * .42 * S)
            l = (c + math.cos(a - .5) * .15 * S, c + math.sin(a - .5) * .15 * S)
            r = (c + math.cos(a + .5) * .15 * S, c + math.sin(a + .5) * .15 * S)
            d.polygon([l, tip, r, (c, c)], fill=_a(CRIMSON if k % 2 else BLOOD), outline=_a((255, 120, 140)))
        d.ellipse((c - .07 * S, c - .07 * S, c + .07 * S, c + .07 * S), fill=_a((255, 220, 200)))
        g.glow_spot(img, c, c, .18 * S, (255, 80, 100), .9)
    return _paint(draw, .5, .5, (70, 10, 24))


def collapse_icon():
    def draw(d, img, S):
        d.ellipse((.08 * S, .38 * S, .92 * S, .62 * S), outline=_a(VOID), width=max(2, S // 18))
        d.ellipse((.18 * S, .42 * S, .82 * S, .58 * S), outline=_a((220, 180, 255)), width=max(1, S // 40))
        d.ellipse((.32 * S, .32 * S, .68 * S, .68 * S), fill=_a((4, 2, 8)), outline=_a((120, 60, 200)))
        for k in range(8):
            a = k * math.tau / 8
            d.line([((.5 + math.cos(a) * .46) * S, (.5 + math.sin(a) * .46) * S), ((.5 + math.cos(a) * .3) * S, (.5 + math.sin(a) * .3) * S)],
                   fill=_a(VOID), width=max(1, S // 48))
        g.glow_spot(img, .5 * S, .5 * S, .45 * S, (150, 60, 255), .4)
    return _paint(draw, .5, .5, (30, 10, 50))


THIRD_ICONS = (("bell_of_doom", bell_of_doom_icon), ("infernal_checkerboard", infernal_checkerboard_icon), ("titans_fist", titans_fist_icon),
               ("toppling_columns", toppling_columns_icon), ("reapers_scythe", reapers_scythe_icon), ("iron_maidens", iron_maidens_icon),
               ("spectral_kings", spectral_kings_icon), ("clock_of_fate", clock_of_fate_icon), ("crimson_lotus", crimson_lotus_icon),
               ("collapse", collapse_icon))


def main():
    for i, art in enumerate((rune_circle_tyrant(), rune_circle_judgment(), rune_circle_void())):
        g.save(art, f"textures/entity/boss/rune_circle_{i}.png")
    g.save(sky_circle(), "textures/entity/boss/sky_circle.png")
    g.save(glyphs(), "textures/entity/boss/glyphs.png")
    g.save(chevrons(), "textures/entity/boss/chevrons.png")
    g.save(zone(), "textures/entity/boss/zone.png")
    g.save(edge(), "textures/entity/boss/edge.png")
    g.save(beam(), "textures/entity/boss/beam.png")
    g.save(judgment_blade(), "textures/entity/boss/judgment_blade.png")
    g.save(rune_wall(), "textures/entity/boss/rune_wall.png")
    new_attack_textures()
    import boss_materials
    boss_materials.main()
    for name, icon in (("crushing_meteor", crushing_meteor_icon), ("judgment_blade", judgment_blade_icon),
                       ("annihilation_beam", annihilation_beam_icon), ("triple_nova", triple_nova_icon),
                       ("pendulum", pendulum_icon), ("seismic_rift", seismic_rift_icon), ("void_orb", void_orb_icon),
                       ("cross_of_light", cross_of_light_icon), ("rune_prison", rune_prison_icon),
                       ("last_judgment", last_judgment_icon)):
        g.save(icon(), f"textures/gui/spell_icons/{name}.png")
    for name, icon in NEW_ICONS + THIRD_ICONS:
        g.save(icon(), f"textures/gui/spell_icons/{name}.png")
    g.save(affinity_ring_boss(), "textures/item/affinity_ring_boss.png")
    g.save(tyrants_codex_icon(), "textures/item/tyrants_codex.png")
    g.save(tyrants_codex_model_texture(), "textures/item/tyrants_codex_3d.png")
    g.write_json("models/item/affinity_ring_boss.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/affinity_ring_boss"}})
    g.book_models("tyrants_codex")
    import boss_animations
    g.write_json("player_animation/boss_spells.json", boss_animations.animations())
