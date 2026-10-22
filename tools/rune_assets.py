#!/usr/bin/env python3
"""Rune Magic art: the runes (a sheet of glyphs, the carved and glowing circles, a path tile), the light effects
(pulse ring, glow, lightning arc, the ward's curtain), spell icons, the Elder Rune Codex, its affinity ring and the
effect icons.

Called by gen_assets.py (run that one). All art is original: the runes are drawn here, stave and branches, after no
real alphabet. Additive textures (drawn with RenderType.eyes) are greyscale on black, tinted by the renderers. Carved
textures (drawn alpha-blended, lit by the world) are dark grooves with a pale rim, on transparent.
"""
import math
import random

from PIL import Image, ImageChops, ImageDraw, ImageFilter

import gen_assets as g
from explosion_assets import bloom, fbm

# Rune palette
NIGHT = (16, 10, 24)
STONE_DARK = (58, 62, 70)
STONE = (104, 108, 116)
STONE_LIGHT = (150, 154, 162)
STONE_HIGH = (190, 194, 200)
GLOW = (181, 123, 255)
GLOW_DEEP = (106, 47, 200)
GLOW_WHITE = (242, 230, 255)
MEND = (242, 156, 255)
MEND_DARK = (150, 60, 170)
MEND_WHITE = (255, 222, 255)
STEEL = (222, 228, 236)
STEEL_DARK = (130, 138, 150)
HILT = (112, 74, 44)

S = 512  # working size of the big textures, drawn then scaled down


# ---------------------------------------------------------------- runes

#: Branches a rune can grow from its stave (top, middle and foot of the stave; ends in units of the stave's height)
BRANCHES = [
    ((0, 0), (.38, .3)), ((0, 0), (-.38, .3)),          # from the top, down to the right / left
    ((0, .5), (.38, .2)), ((0, .5), (-.38, .2)),        # from the middle, up
    ((0, .5), (.38, .8)), ((0, .5), (-.38, .8)),        # from the middle, down
    ((0, 1), (.38, .7)), ((0, 1), (-.38, .7)),          # from the foot, up
    ((0, .15), (.38, .45)), ((.38, .45), (0, .75)),     # a pointed bow on the right
    ((0, .15), (-.38, .45)), ((-.38, .45), (0, .75)),   # and on the left
]


def rune(d, cx, cy, h, seed, width, col=255):
    """A rune: an upright stave and two to four straight branches off it, sometimes a second stave."""
    rnd = random.Random(seed)
    top = cy - h / 2

    def p(pt, ox=0):
        return cx + (pt[0] + ox) * h, top + pt[1] * h

    d.line([p((0, 0)), p((0, 1))], fill=col, width=width)
    second = rnd.random() < .25
    if second:
        d.line([p((0, 0), .38), p((0, 1), .38)], fill=col, width=width)
    chosen = rnd.sample(range(len(BRANCHES)), rnd.randint(2, 3 if second else 4))
    for k in chosen:
        a, b = BRANCHES[k]
        d.line([p(a), p(b)], fill=col, width=width)
    # Rounded joints so the strokes read as carved
    for k in chosen:
        for pt in BRANCHES[k]:
            x, y = p(pt)
            r = width * .45
            d.ellipse((x - r, y - r, x + r, y + r), fill=col)


def _rune_ring(mask, c, r0, r1, count, seed, width):
    """Runes standing between two circles, their feet toward the centre."""
    mid = (r0 + r1) / 2
    h = (r1 - r0) * .72
    for k in range(count):
        a = k * math.tau / count
        cell = Image.new("L", (int(h * 2), int(h * 2)), 0)
        rune(ImageDraw.Draw(cell), h, h, h, seed * 100 + k, width)
        cell = cell.rotate(-math.degrees(a) - 90, resample=Image.BICUBIC)
        x, y = c + math.cos(a) * mid, c + math.sin(a) * mid
        mask.paste(255, (int(x - h), int(y - h)), cell)


def _circle(d, c, r, w, col=255):
    d.ellipse((c - r, c - r, c + r, c + r), outline=col, width=w)


def circle_mask():
    """The lines of the rune circle (white on black, working size): a rim, a band of runes, an eight-pointed knot of
    straight lines, notches and a small ring at the heart."""
    mask = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(mask)
    c = S / 2
    _circle(d, c, .97 * c, 10)
    _circle(d, c, .90 * c, 4)
    _rune_ring(mask, c, .70 * c, .90 * c, 16, 7, 7)
    _circle(d, c, .70 * c, 6)
    # Two squares turned an eighth of a turn: an eight-pointed knot
    for rot in (0, math.pi / 4):
        pts = [(c + math.cos(rot + k * math.pi / 2) * .68 * c, c + math.sin(rot + k * math.pi / 2) * .68 * c) for k in range(4)]
        d.line(pts + [pts[0]], fill=255, width=5)
    _circle(d, c, .36 * c, 5)
    for k in range(8):
        a = k * math.tau / 8 + math.pi / 8
        d.line([(c + math.cos(a) * .36 * c, c + math.sin(a) * .36 * c), (c + math.cos(a) * .48 * c, c + math.sin(a) * .48 * c)], fill=255, width=5)
    rune(d, c, c, .34 * c, 4242, 9)
    return mask


def ring_glow():
    """The circle's lines glowing (additive)."""
    rgb = Image.merge("RGB", [circle_mask()] * 3).resize((256, 256), Image.LANCZOS)
    return bloom(rgb, 2.5, 1.0)


def ring_carved():
    """The circle cut into the ground: dark grooves with a pale rim on their lower edge, on transparent."""
    mask = circle_mask().filter(ImageFilter.MaxFilter(3))
    rim = ImageChops.subtract(ImageChops.offset(mask, 4, 4), mask)
    out = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    groove = Image.new("RGBA", (S, S), (24, 26, 32, 255))
    light = Image.new("RGBA", (S, S), (176, 182, 192, 255))
    out.paste(light, (0, 0), rim.point(lambda v: v * 5 // 10))
    out.paste(groove, (0, 0), mask.point(lambda v: v * 22 // 25))
    return out.resize((256, 256), Image.LANCZOS)


def glyph_sheet():
    """Sixteen runes, 4 x 4 (64 pixels each), glowing (additive)."""
    big = Image.new("L", (1024, 1024), 0)
    d = ImageDraw.Draw(big)
    for k in range(16):
        cx, cy = (k % 4) * 256 + 128, (k // 4) * 256 + 128
        rune(d, cx, cy, 170, 300 + k * 7, 22)
    rgb = Image.merge("RGB", [big] * 3).resize((256, 256), Image.LANCZOS)
    return bloom(rgb, 3, 1.1)


def tile_carved():
    """A path tile cut into the ground: a square groove with notched corners and a pale rim, on transparent."""
    W = 256
    mask = Image.new("L", (W, W), 0)
    d = ImageDraw.Draw(mask)
    m = 18
    d.rectangle((m, m, W - m, W - m), outline=255, width=14)
    for x, y in ((m, m), (W - m, m), (m, W - m), (W - m, W - m)):
        d.ellipse((x - 16, y - 16, x + 16, y + 16), fill=255)
    d.rectangle((m + 30, m + 30, W - m - 30, W - m - 30), outline=255, width=5)
    rim = ImageChops.subtract(ImageChops.offset(mask, 4, 4), mask)
    out = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    # A faint wash of dust over the tile
    dust = Image.new("RGBA", (W, W), (40, 42, 48, 255))
    out.paste(dust, (0, 0), Image.new("L", (W, W), 38))
    out.paste(Image.new("RGBA", (W, W), (176, 182, 192, 255)), (0, 0), rim.point(lambda v: v // 2))
    out.paste(Image.new("RGBA", (W, W), (24, 26, 32, 255)), (0, 0), mask.point(lambda v: v * 9 // 10))
    return out.resize((64, 64), Image.LANCZOS)


def pulse():
    """A soft ring of light, bright on its outer edge (additive)."""
    img = Image.new("RGB", (128, 128))
    px = img.load()
    for y in range(128):
        for x in range(128):
            r = math.hypot(x + .5 - 64, y + .5 - 64) / 64
            v = math.exp(-((r - .9) / .06) ** 2) + .25 * math.exp(-((r - .78) / .12) ** 2) if r < 1 else 0
            px[x, y] = (int(min(1, v) * 255),) * 3
    return img.convert("RGBA")


def glow():
    """A round glow (additive)."""
    img = Image.new("RGB", (64, 64))
    px = img.load()
    for y in range(64):
        for x in range(64):
            r = math.hypot(x + .5 - 32, y + .5 - 32) / 32
            v = max(0.0, 1 - r) ** 2.2
            px[x, y] = (int(v * 255),) * 3
    return img.convert("RGBA")


def arc():
    """A strand of lightning, along the texture's height: a white core in a soft glow (additive)."""
    img = Image.new("RGB", (32, 128))
    px = img.load()
    for y in range(128):
        flicker = .8 + .2 * fbm(0, y / 9, 2, 61)
        for x in range(32):
            d = abs(x + .5 - 16) / 16
            v = (math.exp(-(d / .14) ** 2) + .45 * math.exp(-(d / .5) ** 2)) * flicker
            px[x, y] = (int(min(1, v) * 255),) * 3
    return img.convert("RGBA")


def curtain():
    """The ward's curtain: faint streaks rising, tiling both ways; the renderer fades it towards its top (additive)."""
    img = Image.new("RGB", (64, 64))
    px = img.load()
    for y in range(64):
        for x in range(64):
            # Periodic noise, so the curtain tiles around the circle and as it scrolls
            u, v = x / 64 * math.tau, y / 64 * math.tau
            n = (.5 + .5 * math.sin(u * 3 + math.sin(v) * 1.3)) * (.6 + .4 * math.sin(v * 2 + u))
            streak = .5 + .5 * math.sin(u * 7 + 2 * math.sin(v))
            val = .35 + .4 * n * streak
            px[x, y] = (int(val * 255),) * 3
    return img.convert("RGBA")


# ---------------------------------------------------------------- icons

def _icon_base(inner, outer, cx=8, cy=8):
    return g.spell_background(cx, cy, inner, outer)


def _px(img, x, y, col):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), col + (255,))


def _grid(img, rows, legend, ox=0, oy=0):
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                _px(img, x + ox, y + oy, legend[ch])
    return img


def ward_rune_icon():
    """A ring of light on stone with a shield of runes in it."""
    img = _icon_base((72, 40, 104), NIGHT)
    d = ImageDraw.Draw(img)
    d.ellipse((1, 3, 14, 14), outline=GLOW_DEEP + (255,))
    d.ellipse((2, 4, 13, 13), outline=GLOW + (255,))
    return _grid(img, [
        "......oooo......",
        ".....oWWWWo.....",
        "....oWgGGgWo....",
        "....oWGwwGWo....",
        "....oWgwgGWo....",
        "....oWGwGgWo....",
        ".....oWGGWo.....",
        "......oWWo......",
        ".......oo.......",
    ], {"o": NIGHT, "W": STEEL, "g": GLOW_DEEP, "G": GLOW, "w": GLOW_WHITE}, 0, 3)


def mending_runestone_icon():
    """A standing stone, orchid runes glowing down its face, a spark of healing over it."""
    img = _icon_base((84, 34, 90), NIGHT, 8, 9)
    return _grid(img, [
        "...........m....",
        "..........mMm...",
        "......ooo..m....",
        ".....oHLLo......",
        ".....oLmLo......",
        ".....oLMso......",
        "....oHLmLso.....",
        "....oLLLLso.....",
        "....oLMmLso.....",
        "....oLLMLso.....",
        "....oLmLLso.....",
        "...oHLLLLsso....",
        "...oLLLLLLso....",
        "..ddddddddddd...",
        "................",
        "................",
    ], {"o": NIGHT, "H": STONE_HIGH, "L": STONE_LIGHT, "s": STONE_DARK, "m": MEND, "M": MEND_WHITE, "d": (36, 30, 40)}, 0, 1)


def edge_rune_icon():
    """A sword, a rune glowing on its blade."""
    img = _icon_base((66, 36, 96), NIGHT, 9, 7)
    return _grid(img, [
        "..............o.",
        ".............oWo",
        "............oWwo",
        "...........oWwo.",
        "..........oWGo..",
        ".........oGwGo..",
        "........oWGGo...",
        ".......oWwGo....",
        "...o..oWwo......",
        "...oooWwo.......",
        "....oyoo........",
        "...oyhyo........",
        "..ohoooyo.......",
        ".oho....o.......",
        ".oo.............",
        "................",
    ], {"o": NIGHT, "W": STEEL, "w": STEEL_DARK, "G": GLOW, "y": (200, 160, 70), "h": HILT})


def runic_path_icon():
    """Rune tiles leading away to the upper right, each lit, light streaming along them."""
    img = _icon_base((60, 30, 90), NIGHT, 6, 10)
    d = ImageDraw.Draw(img)
    for i, (x, y, r) in enumerate(((3.5, 12.5, 3), (7.5, 8.5, 2.5), (11, 5, 2), (13.5, 2.5, 1.5))):
        pts = [(x, y - r), (x + r, y), (x, y + r), (x - r, y)]
        d.polygon(pts, fill=STONE_DARK + (255,), outline=STONE_LIGHT + (255,))
        _px(img, round(x - .5), round(y - .5), g.lerp(GLOW_WHITE, GLOW_DEEP, i / 4))
        if r >= 2.5:
            _px(img, round(x + .5), round(y - .5), GLOW)
    # Light streaming along the path
    for k in range(5):
        _px(img, 2 + k * 2, 9 - k * 2, g.lerp(GLOW, NIGHT, .45))
    return img


def runic_bulwark_icon():
    """A wall of carved slabs, a great rune on it, an arrow glancing off."""
    img = _icon_base((60, 34, 86), NIGHT, 8, 10)
    return _grid(img, [
        "..............w.",
        ".............w..",
        "......y.....w...",
        ".......y...w....",
        "........yo......",
        ".oooooooooooooo.",
        ".oLLLLLsLLLLLLo.",
        ".oLLLGLsLLGLLso.",
        ".ossGGssssGGsso.",
        ".oLLLGLLsLLGLLo.",
        ".oLLGLGLsLGLGso.",
        ".osssssssssssso.",
        ".oLLLsLLLLLsLLo.",
        ".oLLLsLLLLLsLso.",
        ".oooooooooooooo.",
        "................",
    ], {"o": NIGHT, "L": STONE, "s": STONE_DARK, "G": GLOW, "y": STEEL_DARK, "w": (200, 180, 140)})


def snare_rune_icon():
    """Stone spikes bursting out of a rune on the ground."""
    img = _icon_base((60, 40, 84), NIGHT, 8, 11)
    d = ImageDraw.Draw(img)
    d.ellipse((1, 10, 14, 15), outline=GLOW + (255,))
    return _grid(img, [
        ".......o........",
        "......oHo.......",
        "..o...oLo...o...",
        ".oHo..oLo..oHo..",
        ".oLo.oLLso.oLo..",
        ".oLso.oLso.oLso.",
        "oLLso.oLso.oLLso",
        "oLLsooLLLsooLLso",
        "oLLLsoLLLsoLLLso",
    ], {"o": NIGHT, "H": STONE_HIGH, "L": STONE_LIGHT, "s": STONE_DARK}, 0, 2)


def rune_shards_icon():
    """Three carved shards flying, each trailing light."""
    img = _icon_base((64, 34, 96), NIGHT, 6, 10)
    d = ImageDraw.Draw(img)
    for (x, y) in ((10, 3), (12, 8), (8, 11)):
        for k in range(1, 6):
            col = g.lerp(GLOW, NIGHT, k / 6)
            _px(img, x - k, y + k // 2, col)
        d.polygon([(x - 1, y), (x + 2, y - 1), (x + 3, y + 1), (x, y + 2)], fill=STONE_LIGHT + (255,), outline=NIGHT + (255,))
        _px(img, x + 1, y, GLOW_WHITE)
    return img


def petrifying_rune_icon():
    """A figure turned to stone up to the waist, a rune glowing under its feet."""
    img = _icon_base((60, 40, 80), NIGHT, 8, 12)
    d = ImageDraw.Draw(img)
    d.ellipse((2, 12, 13, 15), outline=GLOW + (255,))
    return _grid(img, [
        "......ooo.......",
        ".....offfo......",
        ".....offfo......",
        "......ofo.......",
        "....ooffooo.....",
        "...ofofffofo....",
        "...of.fff.fo....",
        "...oo.LLL.oo....",
        "....oLLLsLo.....",
        "....oLsLLso.....",
        "...oLLLsLLso....",
        "...oLsLLLsso....",
        "....ooooooo.....",
    ], {"o": NIGHT, "f": (200, 170, 140), "L": STONE_LIGHT, "s": STONE_DARK}, 0, 1)


def menhir_surge_icon():
    """A line of menhirs bursting out of the ground, each taller than the last."""
    img = _icon_base((64, 40, 90), NIGHT, 10, 8)
    return _grid(img, [
        "............oo..",
        "...........oLLo.",
        "...........oLGo.",
        "...........oLLo.",
        ".......oo..oGLs.",
        "......oLLo.oLLs.",
        "......oLGo.oLGs.",
        "......oGLs.oLLs.",
        "..oo..oLLs.oLLs.",
        ".oLLo.oLGs.oGLs.",
        ".oGLs.oLLs.oLLs.",
        ".oLLs.oLLs.oLLs.",
        "dddddddddddddddd",
        "..d.....d.....d.",
    ], {"o": NIGHT, "L": STONE_LIGHT, "s": STONE_DARK, "G": GLOW, "d": (34, 30, 30)}, 0, 1)


def circle_of_elders_icon():
    """A ring of standing stones seen from above, lightning leaping in it."""
    img = _icon_base((90, 50, 130), NIGHT)
    d = ImageDraw.Draw(img)
    d.ellipse((2, 2, 13, 13), outline=GLOW_DEEP + (255,))
    for k in range(8):
        a = k * math.tau / 8
        x, y = 8 + math.cos(a) * 6, 8 + math.sin(a) * 6
        d.rectangle((round(x) - 1, round(y) - 1, round(x), round(y)), fill=STONE_LIGHT + (255,), outline=NIGHT + (255,))
        _px(img, round(x) - 1, round(y) - 1, STONE_HIGH)
    d.line([(4, 5), (7, 8), (6, 9), (11, 11)], fill=GLOW_WHITE + (255,))
    d.line([(8, 3), (9, 7), (8, 8)], fill=GLOW + (255,))
    return img


def affinity_ring_rune():
    return g.from_grid([
        "................",
        "......oooo......",
        ".....ogwgo......",
        ".....oGGGo......",
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
    ], {"o": NIGHT, "h": STEEL, "l": (196, 202, 212), "b": STEEL_DARK, "s": (84, 90, 100),
        "G": GLOW, "g": GLOW_DEEP, "w": GLOW_WHITE})


def elder_rune_codex_icon():
    """A book bound between two carved stone plates, a rune glowing on the cover."""
    return g.from_grid([
        "................",
        "...oooooooooo...",
        "..ohhLLLLLLsco..",
        "..ohoooooooocco.",
        "..oLo..GG..oLco.",
        "..oLo.GwwG.oLco.",
        "..oLo..Gg..oLcco",
        "..oLo..gG..oLcco",
        "..oLo.Gg.G.oLcco",
        "..oLo......oLcco",
        "..oLoooooooosco.",
        "..oLsssssssssco.",
        "..osssssssssxco.",
        "...ooooooooooo..",
        "................",
        "................",
    ], {"o": NIGHT, "h": STONE_HIGH, "L": STONE_LIGHT, "s": STONE_DARK, "c": g.CREAM, "x": g.CREAM_SHADE,
        "G": GLOW, "g": GLOW_DEEP, "w": GLOW_WHITE})


def elder_rune_codex_model_texture():
    """64x64 texture of the 3D codex, laid out like the other books: covers of carved grey stone with a glowing rune
    circle, iron corners, old pages, a violet gem."""
    s = 4
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    def box(u0, v0, u1, v1, fill):
        d.rectangle((u0 * s, v0 * s, u1 * s - 1, v1 * s - 1), fill=fill + (255,))

    W, H = 12 * s, 9 * s
    for y in range(H):
        for x in range(W):
            n = fbm(x / 4, y / 4, 3, 21)
            img.putpixel((x, y), g.lerp(STONE_DARK, STONE_LIGHT, n) + (255,))
    # Cracks and chisel marks
    rnd = random.Random(5)
    for _ in range(6):
        x, y = rnd.randrange(W), rnd.randrange(H)
        for _ in range(5):
            img.putpixel((x % W, y % H), (44, 46, 52, 255))
            x += rnd.choice((-1, 0, 1))
            y += 1
    # The rune circle cut into the cover, glowing
    cover = Image.new("L", (W * 4, H * 4), 0)
    cd = ImageDraw.Draw(cover)
    cx, cy, r = W * 2, H * 2, H * 1.6
    cd.ellipse((cx - r, cy - r, cx + r, cy + r), outline=255, width=4)
    rune(cd, cx, cy, r * 1.1, 77, 6)
    cover = cover.resize((W, H), Image.LANCZOS)
    halo = cover.filter(ImageFilter.GaussianBlur(1))
    cp, hp = cover.load(), halo.load()
    for y in range(H):
        for x in range(W):
            v = min(255, cp[x, y] + hp[x, y] * .8)
            if v > 20:
                base = img.getpixel((x, y))
                t = v / 255
                img.putpixel((x, y), g.lerp(base[:3], GLOW_WHITE if t > .7 else GLOW, min(1, t * 1.3)) + (255,))
    d.rectangle((0, 0, W - 1, H - 1), outline=NIGHT + (255,))
    d.rectangle((1, 1, W - 2, H - 2), outline=STONE_DARK + (255,))
    # Iron corners
    for x0, y0 in ((2, 2), (W - 6, 2), (2, H - 6), (W - 6, H - 6)):
        d.rectangle((x0, y0, x0 + 3, y0 + 3), fill=STEEL_DARK + (255,))
        img.putpixel((x0, y0), STEEL + (255,))
    box(12, 0, 13, 9, STONE_DARK)
    d.line((12 * s, 0, 12 * s, H - 1), fill=STEEL_DARK + (255,))
    # Pages
    for y in range(0, 8 * s):
        for x in range(13 * s, 16 * s):
            img.putpixel((x, y), g.lerp(g.CREAM, (180, 165, 140), fbm(x / 3, y / 3, 2, 9) * .7) + (255,))
    for y in range(1, 8 * s, 2):
        d.line((13 * s, y, 16 * s - 1, y), fill=g.CREAM_SHADE + (255,))
    # Spine: stone, a band of runes
    for y in range(9 * s, 12 * s):
        for x in range(0, 12 * s):
            t = abs(y - 10.5 * s) / (1.5 * s)
            img.putpixel((x, y), g.lerp(STONE, STONE_DARK, t) + (255,))
    for x in range(2, 12 * s - 2, 4):
        img.putpixel((x, 10 * s + 1), GLOW + (255,))
        img.putpixel((x + 1, 10 * s), GLOW_DEEP + (255,))
    # Page edges
    box(0, 12, 12, 15, g.CREAM)
    for x in range(0, 12 * s, 2):
        d.line((x, 12 * s, x, 15 * s - 1), fill=g.CREAM_SHADE + (255,))
    # Fittings: iron
    box(12, 9, 14, 11, STEEL_DARK)
    d.line((12 * s + 1, 9 * s + 1, 14 * s - 2, 9 * s + 1), fill=STEEL + (255,))
    # Violet gem
    for y in range(9 * s, 11 * s):
        for x in range(14 * s, 16 * s):
            t = math.hypot(x - 14.8 * s, y - 9.8 * s) / (1.4 * s)
            img.putpixel((x, y), g.lerp(GLOW_WHITE, GLOW_DEEP, t ** .8) + (255,))
    # Strap with iron studs
    box(12, 11, 16, 12, (44, 48, 56))
    for x in range(12 * s + 1, 16 * s, 3):
        img.putpixel((x, 11 * s + 1), STEEL + (255,))
        img.putpixel((x, 12 * s - 2), STEEL + (255,))
    return img


def petrified_effect_icon():
    """18x18: a boot set in stone."""
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    return _grid_any(img, [
        "..................",
        "......oooo........",
        ".....obbbbo.......",
        ".....obbbbo.......",
        ".....obbbbo.......",
        ".....obbbbo.......",
        "....oLLLLLLoo.....",
        "...oLLsLLLLLLoo...",
        "..oLLLLLsLLLLLLo..",
        "..oLsLLLLLLsLLLo..",
        "..oLLLLsLLLLLLso..",
        "..oLLLLLLLLsLLLo..",
        "...oooooooooooo...",
    ], {"o": NIGHT, "b": (110, 74, 44), "L": STONE_LIGHT, "s": STONE_DARK}, 0, 3)


def rune_edge_effect_icon():
    """18x18: a blade, a rune glowing on it."""
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    return _grid_any(img, [
        "...............oo.",
        "..............oWo.",
        ".............oWwo.",
        "............oWwo..",
        "...........oGwo...",
        "..........oGGo....",
        ".........oWGo.....",
        "........oWwo......",
        "...o...oWwo.......",
        "...oo.oWwo........",
        "....ooWwo.........",
        ".....yoo..........",
        "....yhyo..........",
        "...ohoo.o.........",
        "..oho.............",
        "..oo..............",
    ], {"o": NIGHT, "W": STEEL, "w": STEEL_DARK, "G": GLOW, "y": (200, 160, 70), "h": HILT}, 0, 1)


def _grid_any(img, rows, legend, ox=0, oy=0):
    w, h = img.size
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != "." and 0 <= x + ox < w and 0 <= y + oy < h:
                img.putpixel((x + ox, y + oy), legend[ch] + (255,))
    return img


def main():
    g.save(glyph_sheet(), "textures/entity/rune/glyphs.png")
    g.save(ring_glow(), "textures/entity/rune/ring_glow.png")
    g.save(ring_carved(), "textures/entity/rune/ring_carved.png")
    g.save(tile_carved(), "textures/entity/rune/tile_carved.png")
    g.save(pulse(), "textures/entity/rune/pulse.png")
    g.save(glow(), "textures/entity/rune/glow.png")
    g.save(arc(), "textures/entity/rune/arc.png")
    g.save(curtain(), "textures/entity/rune/curtain.png")
    for name, icon in (("ward_rune", ward_rune_icon), ("mending_runestone", mending_runestone_icon), ("edge_rune", edge_rune_icon),
                       ("runic_path", runic_path_icon), ("runic_bulwark", runic_bulwark_icon), ("snare_rune", snare_rune_icon),
                       ("rune_shards", rune_shards_icon), ("petrifying_rune", petrifying_rune_icon),
                       ("menhir_surge", menhir_surge_icon), ("circle_of_elders", circle_of_elders_icon)):
        g.save(icon(), f"textures/gui/spell_icons/{name}.png")
    g.save(petrified_effect_icon(), "textures/mob_effect/petrified.png")
    g.save(rune_edge_effect_icon(), "textures/mob_effect/rune_edge.png")
    g.save(affinity_ring_rune(), "textures/item/affinity_ring_rune.png")
    g.save(elder_rune_codex_icon(), "textures/item/elder_rune_codex.png")
    g.save(elder_rune_codex_model_texture(), "textures/item/elder_rune_codex_3d.png")
    g.write_json("models/item/affinity_ring_rune.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/affinity_ring_rune"}})
    g.book_models("elder_rune_codex")
