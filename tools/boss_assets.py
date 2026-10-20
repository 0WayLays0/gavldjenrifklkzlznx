#!/usr/bin/env python3
"""Boss Magic art: the warning zones' textures (rune circles, chevrons, glyphs, the zone fill and its edge), the
scenery (the judgment blade, beams, the prison wall), spell icons, scroll, affinity ring and the Tyrant's Codex.

Called by gen_assets.py (run that one). All art is original. Additive textures (drawn with RenderType.eyes) are
greyscale on black: black is invisible, the renderers tint them per style. The zone fill is white with alpha (drawn
alpha-blended, tinted by the zone's colour).
"""
import math
import random

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


def starfall_icon():
    img = _icon_base((20, 40, 90), NIGHT, 8, 4)
    for sx, sy in ((3, 9), (8, 5), (12, 11)):
        for i in range(1, 4):
            _px(img, sx - i, sy - i, g.lerp(STAR, NIGHT, i / 4))
        _px(img, sx, sy, (255, 255, 255))
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            _px(img, sx + dx, sy + dy, STAR)
    d = ImageDraw.Draw(img)
    d.arc((1, 12, 15, 17), 180, 360, fill=STAR + (255,))
    return img


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

def scroll_boss():
    return g.from_grid([
        "................",
        "..........oo....",
        ".........ohlo...",
        "........oclllo..",
        ".......occcbso..",
        "......occcccoo..",
        ".....occcrcco...",
        "....occcryrco...",
        "...occcxrrxo....",
        "..occccxxxo.....",
        ".oohccccxo......",
        "ohllocccco......",
        "olbbsocco.......",
        ".osssoco........",
        "..ooo.o.........",
        "................",
    ], {"o": NIGHT, "h": GOLD, "l": CRIMSON, "b": BLOOD, "s": BLOOD_DARK,
        "c": g.CREAM, "x": g.CREAM_SHADE, "r": CRIMSON, "y": GOLD})


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
    for name, icon in (("crushing_meteor", crushing_meteor_icon), ("judgment_blade", judgment_blade_icon),
                       ("annihilation_beam", annihilation_beam_icon), ("triple_nova", triple_nova_icon),
                       ("starfall", starfall_icon), ("seismic_rift", seismic_rift_icon), ("void_orb", void_orb_icon),
                       ("cross_of_light", cross_of_light_icon), ("rune_prison", rune_prison_icon),
                       ("last_judgment", last_judgment_icon)):
        g.save(icon(), f"textures/gui/spell_icons/{name}.png")
    g.save(scroll_boss(), "textures/item/scroll_boss.png")
    g.save(affinity_ring_boss(), "textures/item/affinity_ring_boss.png")
    g.save(tyrants_codex_icon(), "textures/item/tyrants_codex.png")
    g.save(tyrants_codex_model_texture(), "textures/item/tyrants_codex_3d.png")
    g.write_json("models/item/scroll_boss.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/scroll_boss"}})
    g.write_json("models/item/affinity_ring_boss.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/affinity_ring_boss"}})
    g.book_models("tyrants_codex")
    import boss_animations
    g.write_json("player_animation/boss_spells.json", boss_animations.animations())
