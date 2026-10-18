#!/usr/bin/env python3
"""Book school art: spell icons, pages, ink, tomes, runes, the flying grimoires, the Living Grimoire, scroll and
affinity ring.

Called by gen_assets.py (run that one). All art is original. Additive textures (RenderType.eyes) are drawn on black.
"""
import math
import random

from PIL import Image, ImageDraw, ImageFilter

import gen_assets as g
from explosion_assets import additive, bloom, fbm, smoothstep

VIOLET = (154, 92, 255)
VIOLET_DARK = (70, 30, 130)
VIOLET_LIGHT = (214, 180, 255)
GILT = (255, 214, 115)
GILT_DARK = (170, 120, 40)
INK = (12, 6, 18)
INK_SHEEN = (60, 30, 96)
PARCH = (238, 224, 188)
PARCH_DARK = (196, 170, 120)
BROWN_INK = (92, 62, 40)
LEATHER = (66, 24, 96)
LEATHER_DARK = (28, 8, 44)
NIGHT = (12, 8, 22)


# ---------------------------------------------------------------- pages

def _rune_glyphs(d, cx, cy, r, col, width, rnd):
    """A rune circle: two rings, little glyphs between them and a star in the middle."""
    d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=col, width=width)
    d.ellipse((cx - r * .8, cy - r * .8, cx + r * .8, cy + r * .8), outline=col, width=max(1, width // 2))
    for k in range(8):
        a = k * math.tau / 8
        x, y = cx + math.cos(a) * r * .9, cy + math.sin(a) * r * .9
        s = r * .07
        if rnd.random() < .5:
            d.line((x - s, y - s, x + s, y + s), fill=col, width=max(1, width // 2))
        else:
            d.ellipse((x - s, y - s, x + s, y + s), outline=col, width=max(1, width // 2))
    pts = [(cx + math.cos(k * math.tau * 2 / 5 - math.pi / 2) * r * .62, cy + math.sin(k * math.tau * 2 / 5 - math.pi / 2) * r * .62) for k in range(6)]
    d.line(pts, fill=col, width=width)


def page_texture():
    """48x64 parchment page with torn edges, lines of brown ink and a rune circle (cutout)."""
    S = 4
    w, h = 48 * S, 64 * S
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = img.load()
    for y in range(h):
        for x in range(w):
            # Torn, uneven edges
            edge = min(x, w - 1 - x, y, h - 1 - y) / S
            tear = 1.2 + 2.2 * fbm(x / 9.0 + (y < 8 or y > h - 9) * 5, y / 9.0, 3, 9)
            if edge < tear - 1.5:
                continue
            n = fbm(x / 40, y / 40, 4, 1)
            c = g.lerp(PARCH, PARCH_DARK, n * .9 + (1 - min(1, edge / 6)) * .5)
            px[x, y] = c + (255,)
    d = ImageDraw.Draw(img)
    rnd = random.Random(4)
    # Lines of handwriting
    for row in range(14):
        y = 8 * S + row * 4 * S
        if 22 * S < y < 46 * S:
            continue
        x = 6 * S
        while x < w - 7 * S:
            length = rnd.randint(3, 8) * S
            d.line((x, y, min(w - 7 * S, x + length), y), fill=BROWN_INK + (255,), width=S)
            x += length + 2 * S
    _rune_glyphs(d, w / 2, 34 * S, 11 * S, VIOLET_DARK + (255,), S, random.Random(8))
    return img.resize((48, 64), Image.LANCZOS)


def page_runes():
    """Additive glow of the page's rune circle (same layout as the page)."""
    S = 4
    img = Image.new("RGB", (48 * S, 64 * S), (0, 0, 0))
    d = ImageDraw.Draw(img)
    _rune_glyphs(d, 24 * S, 34 * S, 11 * S, VIOLET_LIGHT, S * 2, random.Random(8))
    # A few glowing letters in the text
    rnd = random.Random(4)
    for _ in range(9):
        x, y = rnd.randint(7, 40) * S, rnd.choice(list(range(8, 22, 4)) + list(range(48, 60, 4))) * S
        d.line((x, y, x + 3 * S, y), fill=VIOLET, width=S)
    img = img.resize((48, 64), Image.LANCZOS)
    return bloom(img, 1.5, 1.0)


def tome_edge():
    img = Image.new("RGBA", (32, 32))
    for y in range(32):
        for x in range(32):
            c = PARCH if y % 2 == 0 else PARCH_DARK
            img.putpixel((x, y), g.lerp(c, (150, 120, 80), fbm(x / 6, y / 3, 2, 3) * .4) + (255,))
    return img


def tome_cover():
    """Deep violet leather, gilded frame and corners, a gold eye-and-rune sigil in the middle."""
    S = 4
    size = 64 * S
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            n = fbm(x / 22, y / 22, 4, 5)
            px[x, y] = g.lerp(LEATHER, LEATHER_DARK, n * 1.1) + (255,)
    d = ImageDraw.Draw(img)
    d.rectangle((3 * S, 3 * S, size - 3 * S, size - 3 * S), outline=GILT + (255,), width=2 * S)
    d.rectangle((6 * S, 6 * S, size - 6 * S, size - 6 * S), outline=GILT_DARK + (255,), width=S)
    for cx, cy in ((0, 0), (size, 0), (0, size), (size, size)):
        d.polygon([(cx, cy), (cx + (12 * S if cx == 0 else -12 * S), cy), (cx, cy + (12 * S if cy == 0 else -12 * S))], fill=GILT + (255,))
    c = size / 2
    r = 18 * S
    d.ellipse((c - r, c - r, c + r, c + r), outline=GILT + (255,), width=2 * S)
    for k in range(12):
        a = k * math.tau / 12
        d.line((c + math.cos(a) * r, c + math.sin(a) * r, c + math.cos(a) * (r + 5 * S), c + math.sin(a) * (r + 5 * S)), fill=GILT + (255,), width=S)
    # Eye
    d.chord((c - 12 * S, c - 8 * S, c + 12 * S, c + 8 * S), 0, 360, fill=GILT_DARK + (255,), outline=GILT + (255,), width=S)
    d.ellipse((c - 5 * S, c - 5 * S, c + 5 * S, c + 5 * S), fill=VIOLET + (255,), outline=GILT + (255,), width=S)
    d.ellipse((c - 2 * S, c - 2 * S, c + 2 * S, c + 2 * S), fill=NIGHT + (255,))
    return img.resize((64, 64), Image.LANCZOS)


def chapter_rune():
    """Additive glyph: a ring of runes around an open-book sigil."""
    S = 512
    img = Image.new("RGB", (S, S), (0, 0, 0))
    d = ImageDraw.Draw(img)
    c = S / 2
    white = (255, 255, 255)
    d.ellipse((c - .92 * c, c - .92 * c, c + .92 * c, c + .92 * c), outline=white, width=12)
    d.ellipse((c - .74 * c, c - .74 * c, c + .74 * c, c + .74 * c), outline=white, width=6)
    rnd = random.Random(12)
    for k in range(12):
        a = k * math.tau / 12
        x, y = c + math.cos(a) * .83 * c, c + math.sin(a) * .83 * c
        s = 14
        shape = rnd.randrange(3)
        if shape == 0:
            d.line((x - s, y, x + s, y), fill=white, width=6)
            d.line((x, y - s, x, y + s), fill=white, width=6)
        elif shape == 1:
            d.polygon([(x, y - s), (x + s, y), (x, y + s), (x - s, y)], outline=white, width=5)
        else:
            d.ellipse((x - s * .7, y - s * .7, x + s * .7, y + s * .7), fill=white)
    # Open book sigil: two pages as arcs and a spine
    d.arc((c - .55 * c, c - .3 * c, c + .02 * c, c + .5 * c), 200, 340, fill=white, width=12)
    d.arc((c - .02 * c, c - .3 * c, c + .55 * c, c + .5 * c), 200, 340, fill=white, width=12)
    d.line((c, c - .25 * c, c, c + .4 * c), fill=white, width=12)
    d.line((c - .5 * c, c + .32 * c, c, c + .45 * c, c + .5 * c, c + .32 * c), fill=white, width=10)
    # A star rising from the book
    for k in range(4):
        a = k * math.pi / 2
        d.line((c, c - .45 * c, c + math.cos(a) * .14 * c, c - .45 * c + math.sin(a) * .14 * c), fill=white, width=8)
    img = img.resize((128, 128), Image.LANCZOS)
    return bloom(img, 3, .9)


# ---------------------------------------------------------------- ink

def ink_splash():
    """Glossy black ink splat (alpha blended) with a violet sheen and droplets around."""
    size = 128
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    c = size / 2
    for y in range(size):
        for x in range(size):
            dx, dy = (x + .5 - c) / c, (y + .5 - c) / c
            r = math.hypot(dx, dy)
            ang = math.atan2(dy, dx)
            edge = .62 + .18 * fbm(math.cos(ang) * 2 + 3, math.sin(ang) * 2 + 3, 3, 6) + .12 * math.sin(ang * 7)
            if r > edge:
                continue
            sheen = smoothstep(.2, .9, fbm(x / 14, y / 14, 3, 2)) * (1 - r / edge)
            col = g.lerp(INK, INK_SHEEN, sheen * .9)
            a = int(255 * min(1.0, (edge - r) * 12))
            px[x, y] = col + (a,)
    d = ImageDraw.Draw(img)
    rnd = random.Random(21)
    for _ in range(14):
        a = rnd.uniform(0, math.tau)
        r = rnd.uniform(.72, .95) * c
        s = rnd.uniform(2, 5)
        x, y = c + math.cos(a) * r, c + math.sin(a) * r
        d.ellipse((x - s, y - s, x + s, y + s), fill=INK + (235,))
    return img


def _hand_shape(d, W, H, fill):
    """A hand reaching up out of the ink: wrist at the bottom, fingers spread upward. Returns the fingers' and
    thumb's centre lines (wrist to tip)."""
    cx = W / 2
    palm_top = H * .48
    palm_bottom = H * .7
    # Arm / wrist emerging from the ink
    d.polygon([(cx - W * .2, H), (cx + W * .2, H), (cx + W * .17, palm_bottom), (cx - W * .17, palm_bottom)], fill=fill)
    d.ellipse((cx - W * .3, palm_top, cx + W * .3, palm_bottom + H * .05), fill=fill)
    lines = []
    # Fingers: (base x offset, tip x offset, tip height, thickness)
    for bx, tx, ty, t in ((-.2, -.36, .14, .1), (-.07, -.12, .04, .11), (.07, .1, .05, .11), (.2, .33, .16, .1)):
        base = (cx + bx * W, palm_top + H * .04)
        mid = (cx + (bx + tx) * .5 * W, (palm_top + ty * H) * .5 + H * .02)
        tip = (cx + tx * W, ty * H)
        d.line([base, mid, tip], fill=fill, width=int(t * W), joint="curve")
        d.ellipse((tip[0] - t * W / 2, tip[1] - t * W / 2, tip[0] + t * W / 2, tip[1] + t * W / 2), fill=fill)
        lines.append([(cx + bx * .3 * W, H * .92), (cx + bx * .8 * W, palm_top + H * .12), base, mid, tip])
    # Thumb
    thumb_base = (cx + W * .26, palm_top + H * .12)
    thumb_tip = (cx + W * .45, palm_top - H * .06)
    d.line([thumb_base, thumb_tip], fill=fill, width=int(.11 * W))
    d.ellipse((thumb_tip[0] - .055 * W, thumb_tip[1] - .055 * W, thumb_tip[0] + .055 * W, thumb_tip[1] + .055 * W), fill=fill)
    lines.append([(cx + W * .05, H * .92), (cx + W * .15, palm_top + H * .18), thumb_base, thumb_tip])
    return lines


def ink_hand():
    """48x96 black hand of ink (cutout) with glossy violet highlights and drips."""
    W, H = 192, 384
    mask = Image.new("L", (W, H), 0)
    _hand_shape(ImageDraw.Draw(mask), W, H, 255)
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px, mp = img.load(), mask.load()
    for y in range(H):
        for x in range(W):
            if mp[x, y] > 0:
                n = fbm(x / 18, y / 18, 3, 7)
                sheen = smoothstep(.55, .85, n)
                px[x, y] = g.lerp(INK, INK_SHEEN, sheen) + (255,)
    d = ImageDraw.Draw(img)
    rnd = random.Random(3)
    for _ in range(6):
        x = rnd.uniform(.3, .7) * W
        y0 = rnd.uniform(.55, .8) * H
        d.line((x, y0, x, y0 + rnd.uniform(.05, .15) * H), fill=INK + (255,), width=6)
    return img.resize((48, 96), Image.LANCZOS)


def ink_hand_glow():
    """Additive violet veins running up the arm into each finger, a glowing knot in the palm."""
    W, H = 192, 384
    mask = Image.new("L", (W, H), 0)
    lines = _hand_shape(ImageDraw.Draw(mask), W, H, 255)
    veins = Image.new("RGB", (W, H), (0, 0, 0))
    d = ImageDraw.Draw(veins)
    for pts in lines:
        d.line(pts, fill=VIOLET, width=6, joint="curve")
        d.line(pts[2:], fill=VIOLET_LIGHT, width=2)
    cx = W / 2
    d.ellipse((cx - 16, H * .56, cx + 16, H * .56 + 32), fill=VIOLET_LIGHT)
    out = Image.new("RGB", (W, H))
    vp, mp, op = veins.load(), mask.load(), out.load()
    for y in range(H):
        for x in range(W):
            if mp[x, y] > 0:
                op[x, y] = vp[x, y]
    out = out.resize((48, 96), Image.LANCZOS)
    return bloom(out, 1.2, .8)


# ---------------------------------------------------------------- razor pages, word of power, tome of judgment

def razor_edge():
    """Additive rim around a page, for a quad 1.18 times the page: the razor edge burning around it."""
    page = page_texture().split()[3]
    W, H = 48 * 4, 64 * 4
    inner = page.resize((int(W / 1.18), int(H / 1.18)), Image.LANCZOS)
    mask = Image.new("L", (W, H), 0)
    mask.paste(inner, ((W - inner.size[0]) // 2, (H - inner.size[1]) // 2))
    mask = mask.point(lambda v: 255 if v > 100 else 0)
    outer = mask.filter(ImageFilter.MaxFilter(9))
    core = mask.filter(ImageFilter.MinFilter(5))
    rim = Image.new("L", (W, H))
    rp, op, cp = rim.load(), outer.load(), core.load()
    for y in range(H):
        for x in range(W):
            rp[x, y] = max(0, op[x, y] - cp[x, y])
    glow = rim.filter(ImageFilter.GaussianBlur(10))
    out = Image.new("RGB", (W, H))
    o, r, gl = out.load(), rim.load(), glow.load()
    for y in range(H):
        for x in range(W):
            v = min(255, int(r[x, y] * .85 + gl[x, y] * 1.6))
            o[x, y] = (v, v, v)
    return out.resize((48, 64), Image.LANCZOS).convert("RGBA")


# Runic letters for Word of Power, in a unit cell: polylines (and circles as (cx, cy, r))
POWER_GLYPHS = [
    [[(.5, .1), (.5, .9)], [(.5, .45), (.82, .18)], [(.5, .68), (.82, .42)]],
    [[(.5, .08), (.82, .5), (.5, .92), (.18, .5), (.5, .08)], [(.5, .25), (.5, .75)]],
    [[(.5, .92), (.5, .3)], [(.18, .1), (.5, .38), (.82, .1)]],
    [[(.5, .92), (.5, .1)], [(.2, .38), (.5, .1), (.8, .38)], [(.3, .7), (.7, .7)]],
    [[(.18, .12), (.82, .88), (.82, .12), (.18, .88), (.18, .12)]],
    [(.5, .5, .3), [(.5, .08), (.5, .92)], [(.14, .5), (.86, .5)]],
    [[(.22, .92), (.22, .1), (.5, .45), (.78, .1), (.78, .92)], [(.22, .62), (.78, .62)]],
    [[(.72, .1), (.3, .38), (.7, .62), (.28, .9)]],
]


def power_glyphs():
    """Atlas of 8 runic letters of fire (4 x 2 cells of 64 px): white-hot strokes, an orange glow licking upward."""
    S = 4
    cell = 64 * S
    mask = Image.new("L", (cell * 4, cell * 2), 0)
    d = ImageDraw.Draw(mask)
    width = int(cell * .1)
    for i, glyph in enumerate(POWER_GLYPHS):
        ox, oy = (i % 4) * cell, (i // 4) * cell
        for part in glyph:
            if isinstance(part, tuple):
                cx, cy, r = part
                d.ellipse((ox + (cx - r) * cell, oy + (cy - r) * cell, ox + (cx + r) * cell, oy + (cy + r) * cell), outline=255, width=width)
            else:
                pts = [(ox + x * cell, oy + y * cell) for x, y in part]
                d.line(pts, fill=255, width=width, joint="curve")
                for x, y in (pts[0], pts[-1]):
                    d.ellipse((x - width / 2, y - width / 2, x + width / 2, y + width / 2), fill=255)
    mask = mask.resize((256, 128), Image.LANCZOS)
    near = mask.filter(ImageFilter.GaussianBlur(2.5))
    far = mask.filter(ImageFilter.GaussianBlur(7))
    out = Image.new("RGB", mask.size)
    o, m, n, f = out.load(), mask.load(), near.load(), far.load()
    w, h = mask.size
    for y in range(h):
        for x in range(w):
            # Flames: the glow is pushed up and torn by noise
            flick = .55 + .9 * fbm(x / 9, y / 5 + 3, 3, 23)
            up = f[x, min(h - 1, y + 3)] / 255
            core = m[x, y] / 255
            halo = n[x, y] / 255
            r = core * 255 + halo * 255 * .9 + up * 255 * .7 * flick
            g_ = core * 245 + halo * 150 * .9 + up * 70 * flick
            b = core * 200 + halo * 40 + up * 10 * flick
            o[x, y] = (min(255, int(r)), min(255, int(g_)), min(255, int(b)))
    return out.convert("RGBA")


def judgment_cover():
    """Crimson-black leather, a gilded frame and corners, scales of judgment over a sword in the middle."""
    S = 4
    size = 64 * S
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            n = fbm(x / 20, y / 20, 4, 41)
            px[x, y] = g.lerp((104, 16, 26), (30, 4, 10), n * 1.15) + (255,)
    d = ImageDraw.Draw(img)
    d.rectangle((3 * S, 3 * S, size - 3 * S, size - 3 * S), outline=GILT + (255,), width=2 * S)
    d.rectangle((6 * S, 6 * S, size - 6 * S, size - 6 * S), outline=GILT_DARK + (255,), width=S)
    for cx, cy in ((0, 0), (size, 0), (0, size), (size, size)):
        d.polygon([(cx, cy), (cx + (12 * S if cx == 0 else -12 * S), cy), (cx, cy + (12 * S if cy == 0 else -12 * S))], fill=GILT + (255,))
    c = size / 2
    gold = GILT + (255,)
    dark = GILT_DARK + (255,)
    # Sword, point down
    d.polygon([(c - 2 * S, 14 * S), (c + 2 * S, 14 * S), (c + 2 * S, 46 * S), (c, 52 * S), (c - 2 * S, 46 * S)], fill=(200, 200, 210, 255), outline=dark)
    d.rectangle((c - 9 * S, 17 * S, c + 9 * S, 19 * S), fill=gold)
    d.ellipse((c - 3 * S, 9 * S, c + 3 * S, 15 * S), fill=gold)
    # Scales hanging from the guard
    for side in (-1, 1):
        x = c + side * 8 * S
        d.line((x, 19 * S, x - 6 * S, 33 * S), fill=gold, width=S)
        d.line((x, 19 * S, x + 6 * S, 33 * S), fill=gold, width=S)
        d.chord((x - 8 * S, 28 * S, x + 8 * S, 40 * S), 0, 180, fill=gold, outline=dark)
    return img.resize((64, 64), Image.LANCZOS)


def judgment_frame():
    """Additive frame of runes burning on the ground under the Tome of Judgment (192 x 128)."""
    S = 4
    W, H = 192 * S, 128 * S
    img = Image.new("RGB", (W, H), (0, 0, 0))
    d = ImageDraw.Draw(img)
    white = (255, 255, 255)
    m = 6 * S
    d.rectangle((m, m, W - m, H - m), outline=white, width=3 * S)
    d.rectangle((m + 9 * S, m + 9 * S, W - m - 9 * S, H - m - 9 * S), outline=white, width=S + 2)
    rnd = random.Random(31)
    # Glyphs marching along the border band
    def glyph(x, y, s):
        k = rnd.randrange(4)
        if k == 0:
            d.line((x - s, y - s, x + s, y + s), fill=white, width=S)
            d.line((x - s, y + s, x + s, y - s), fill=white, width=S)
        elif k == 1:
            d.polygon([(x, y - s), (x + s, y), (x, y + s), (x - s, y)], outline=white, width=S)
        elif k == 2:
            d.line((x, y - s, x, y + s), fill=white, width=S)
            d.line((x - s, y - s * .3, x + s, y - s * .3), fill=white, width=S)
        else:
            d.ellipse((x - s * .6, y - s * .6, x + s * .6, y + s * .6), outline=white, width=S)
    band = m + 4.5 * S
    s = 2.6 * S
    for x in range(int(m + 18 * S), int(W - m - 14 * S), int(9 * S)):
        glyph(x, band, s)
        glyph(x, H - band, s)
    for y in range(int(m + 18 * S), int(H - m - 14 * S), int(9 * S)):
        glyph(band, y, s)
        glyph(W - band, y, s)
    for cx, cy in ((band, band), (W - band, band), (band, H - band), (W - band, H - band)):
        d.ellipse((cx - 7 * S, cy - 7 * S, cx + 7 * S, cy + 7 * S), outline=white, width=2 * S)
    # Faint scales in the middle
    c = (W / 2, H / 2)
    dim = (120, 120, 120)
    d.line((c[0], c[1] - 30 * S, c[0], c[1] + 26 * S), fill=dim, width=2 * S)
    d.line((c[0] - 26 * S, c[1] - 18 * S, c[0] + 26 * S, c[1] - 18 * S), fill=dim, width=2 * S)
    for side in (-1, 1):
        x = c[0] + side * 22 * S
        d.arc((x - 12 * S, c[1] - 4 * S, x + 12 * S, c[1] + 14 * S), 0, 180, fill=dim, width=2 * S)
        d.line((x, c[1] - 18 * S, x - 11 * S, c[1] + 5 * S), fill=dim, width=S)
        d.line((x, c[1] - 18 * S, x + 11 * S, c[1] + 5 * S), fill=dim, width=S)
    img = img.resize((192, 128), Image.LANCZOS)
    return bloom(img, 2, .9)


# ---------------------------------------------------------------- bookmark, seal of silence, rewrite

def bookmark_ribbon():
    """32 x 160 violet silk ribbon (cutout): gold clip at the top, embroidered border and runes, V-cut tail."""
    W, H = 32, 160
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px = img.load()
    for y in range(H):
        for x in range(W):
            notch = y > H - 18 and abs(x + .5 - W / 2) < (y - (H - 18)) * .9
            if notch:
                continue
            t = abs(x + .5 - W / 2) / (W / 2)
            sheen = .5 + .5 * math.sin(x / W * math.pi) + .12 * fbm(x / 4, y / 14, 2, 51)
            c = g.lerp((52, 18, 92), (150, 84, 230), sheen * (1 - t * .5))
            px[x, y] = c + (255,)
    d = ImageDraw.Draw(img)
    gold = GILT + (255,)
    for x in (3, W - 4):
        d.line((x, 16, x, H - 20), fill=gold)
    rnd = random.Random(61)
    for y in range(26, H - 26, 16):
        cx, cy = W / 2, y
        k = rnd.randrange(3)
        if k == 0:
            d.polygon([(cx, cy - 5), (cx + 5, cy), (cx, cy + 5), (cx - 5, cy)], outline=gold)
        elif k == 1:
            d.ellipse((cx - 4, cy - 4, cx + 4, cy + 4), outline=gold)
            d.point((cx, cy), fill=gold)
        else:
            d.line((cx, cy - 5, cx, cy + 5), fill=gold)
            d.line((cx - 4, cy - 2, cx + 4, cy + 2), fill=gold)
    # Gold clip
    d.rectangle((1, 0, W - 2, 13), fill=GILT_DARK + (255,))
    d.rectangle((3, 2, W - 4, 11), fill=gold)
    d.line((4, 3, W - 5, 3), fill=(255, 245, 200, 255))
    d.ellipse((W / 2 - 3, 4, W / 2 + 3, 10), fill=VIOLET + (255,))
    return img


def bookmark_glow():
    """Additive glow of the ribbon's runes and border."""
    rib = bookmark_ribbon()
    W, H = rib.size
    out = Image.new("RGB", (W, H))
    o, r = out.load(), rib.load()
    for y in range(H):
        for x in range(W):
            c = r[x, y]
            if c[3] and c[0] > 200 and c[1] > 150:
                o[x, y] = (255, 230, 170)
            elif c[3]:
                o[x, y] = (34, 18, 52)
    return bloom(out, 1.5, 1.2)


def _sealed_lips(d, cx, cy, r, fill, width):
    """The seal's glyph: lips pressed shut, stitched across."""
    d.arc((cx - r, cy - r * .55, cx + r, cy + r * .55), 200, 340, fill=fill, width=width)
    d.arc((cx - r, cy - r * .55, cx + r, cy + r * .55), 20, 160, fill=fill, width=width)
    d.line((cx - r * .95, cy, cx + r * .95, cy), fill=fill, width=width)
    for k in (-.5, 0, .5):
        x = cx + k * r
        d.line((x, cy - r * .35, x, cy + r * .35), fill=fill, width=width)


def silence_seal():
    """64 x 64 violet wax seal (cutout): uneven melted rim, raised ring, sealed-lips glyph pressed in."""
    S = 4
    size = 64 * S
    c = size / 2
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    for y in range(size):
        for x in range(size):
            dx, dy = (x - c) / c, (y - c) / c
            r = math.hypot(dx, dy)
            ang = math.atan2(dy, dx)
            edge = .86 + .08 * fbm(math.cos(ang) * 3 + 2, math.sin(ang) * 3 + 2, 3, 71)
            if r > edge:
                continue
            light = .55 - .35 * (dx + dy) * .7 + .15 * fbm(x / 30, y / 30, 2, 72)
            px[x, y] = g.lerp((54, 10, 70), (176, 70, 214), light) + (255,)
    d = ImageDraw.Draw(img)
    # Raised ring: light on top-left, shadow bottom-right
    ring = .62 * c
    d.arc((c - ring, c - ring, c + ring, c + ring), 135, 315, fill=(210, 140, 240, 255), width=3 * S)
    d.arc((c - ring, c - ring, c + ring, c + ring), 315, 135, fill=(48, 8, 64, 255), width=3 * S)
    _sealed_lips(d, c + S, c + S, .38 * c, (60, 12, 80, 255), 3 * S)
    _sealed_lips(d, c, c, .38 * c, (200, 130, 235, 255), 2 * S)
    return img.resize((64, 64), Image.LANCZOS)


def silence_glyph():
    """Additive glow of the seal's ring and glyph."""
    S = 4
    size = 64 * S
    c = size / 2
    img = Image.new("RGB", (size, size), (0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse((c - .62 * c, c - .62 * c, c + .62 * c, c + .62 * c), outline=(200, 140, 255), width=2 * S)
    _sealed_lips(d, c, c, .38 * c, (255, 230, 255), 2 * S)
    img = img.resize((64, 64), Image.LANCZOS)
    return bloom(img, 2, 1.0)


def silence_ring():
    """Additive ring of chain links and runes turning around a silenced head."""
    S = 512
    img = Image.new("RGB", (S, S), (0, 0, 0))
    d = ImageDraw.Draw(img)
    c = S / 2
    white = (255, 255, 255)
    d.ellipse((c - .9 * c, c - .9 * c, c + .9 * c, c + .9 * c), outline=white, width=6)
    d.ellipse((c - .62 * c, c - .62 * c, c + .62 * c, c + .62 * c), outline=white, width=6)
    links = 22
    for k in range(links):
        a = k * math.tau / links
        x, y = c + math.cos(a) * .76 * c, c + math.sin(a) * .76 * c
        # Chain links lie along the ring, every other one seen edge-on
        if k % 2 == 0:
            d.ellipse((x - 26, y - 26, x + 26, y + 26), outline=white, width=8)
        else:
            d.line((x - math.cos(a + math.pi / 2) * 24, y - math.sin(a + math.pi / 2) * 24,
                    x + math.cos(a + math.pi / 2) * 24, y + math.sin(a + math.pi / 2) * 24), fill=white, width=10)
    img = img.resize((128, 128), Image.LANCZOS)
    return bloom(img, 2.5, .8)


def script_band():
    """256 x 32 additive band of glowing handwriting that tiles around a ring."""
    S = 4
    W, H = 256 * S, 32 * S
    img = Image.new("RGB", (W, H), (0, 0, 0))
    d = ImageDraw.Draw(img)
    rnd = random.Random(81)
    white = (255, 255, 255)
    d.line((0, 3 * S, W, 3 * S), fill=(150, 150, 150), width=S)
    d.line((0, H - 3 * S, W, H - 3 * S), fill=(150, 150, 150), width=S)
    x = 4.0 * S
    while x < W - 10 * S:
        word = rnd.randint(3, 6)
        pts = []
        for k in range(word * 6):
            t = k / 6
            px_ = x + k * 1.6 * S
            py = H / 2 + math.sin(t * math.tau) * (5 + rnd.uniform(-1, 3)) * S * (.6 if k % 6 < 3 else 1)
            pts.append((px_, py))
        for dx in (0, -W):
            d.line([(p[0] + dx, p[1]) for p in pts], fill=white, width=int(1.3 * S), joint="curve")
        # A tall letter now and then
        if rnd.random() < .6:
            lx = x + rnd.uniform(0, word * 9) * S
            d.line((lx, H / 2 - 9 * S, lx + 2 * S, H / 2 + 5 * S), fill=white, width=int(1.3 * S))
        x = pts[-1][0] + 6 * S
    img = img.resize((256, 32), Image.LANCZOS)
    return bloom(img, 1.5, .9)


# ---------------------------------------------------------------- forbidden library

def grimoire_cover():
    """Black-teal leather, stitched border, rusty iron corner brackets and a chained lock in the middle."""
    S = 4
    size = 64 * S
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            n = fbm(x / 18, y / 18, 4, 91)
            px[x, y] = g.lerp((26, 46, 52), (6, 12, 16), n * 1.2) + (255,)
    d = ImageDraw.Draw(img)
    for k in range(6 * S, size - 6 * S, 4 * S):
        for a, b in (((k, 6 * S), (k + 2 * S, 6 * S)), ((k, size - 6 * S), (k + 2 * S, size - 6 * S)),
                     ((6 * S, k), (6 * S, k + 2 * S)), ((size - 6 * S, k), (size - 6 * S, k + 2 * S))):
            d.line((a, b), fill=(150, 140, 110, 255), width=S)
    iron = (92, 70, 58, 255)
    rust = (140, 70, 30, 255)
    for cx, cy in ((0, 0), (size, 0), (0, size), (size, size)):
        sx = 1 if cx == 0 else -1
        sy = 1 if cy == 0 else -1
        d.polygon([(cx, cy), (cx + sx * 14 * S, cy), (cx + sx * 14 * S, cy + sy * 4 * S), (cx + sx * 4 * S, cy + sy * 4 * S),
                   (cx + sx * 4 * S, cy + sy * 14 * S), (cx, cy + sy * 14 * S)], fill=iron, outline=rust)
    c = size / 2
    # Chains crossing the cover
    for k in range(-5, 6):
        for sgn in (-1, 1):
            x, y = c + k * 5 * S, c + sgn * k * 5 * S
            d.ellipse((x - 3 * S, y - 2 * S, x + 3 * S, y + 2 * S), outline=(120, 120, 128, 255), width=S)
    # Lock
    d.arc((c - 6 * S, c - 14 * S, c + 6 * S, c - 2 * S), 180, 360, fill=(150, 150, 160, 255), width=2 * S)
    d.rectangle((c - 8 * S, c - 8 * S, c + 8 * S, c + 8 * S), fill=(120, 96, 50, 255), outline=(60, 40, 20, 255), width=S)
    d.ellipse((c - 2 * S, c - 3 * S, c + 2 * S, c + 1 * S), fill=(20, 10, 10, 255))
    d.polygon([(c - S, c), (c + S, c), (c + 1.5 * S, c + 5 * S), (c - 1.5 * S, c + 5 * S)], fill=(20, 10, 10, 255))
    return img.resize((64, 64), Image.LANCZOS)


def grimoire_teeth():
    """64 x 16 row of fangs pointing down from a dark gum line (cutout)."""
    S = 4
    W, H = 64 * S, 16 * S
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, 3 * S), fill=(70, 12, 24, 255))
    n = 9
    for k in range(n):
        x0 = k * W / n + S
        x1 = (k + 1) * W / n - S
        long = 1 if k % 2 == 0 else .7
        tip = (x0 + x1) / 2
        d.polygon([(x0, 2 * S), (x1, 2 * S), (tip, (3 + 12 * long) * S)], fill=(236, 226, 196, 255), outline=(150, 130, 100, 255))
        d.line((x0 + S, 3 * S, tip, (2 + 12 * long) * S), fill=(255, 250, 235, 255), width=S)
    return img.resize((64, 16), Image.LANCZOS)


def grimoire_eyes():
    """Additive: two slit eyes glowing violet under angry brows."""
    S = 4
    size = 64 * S
    img = Image.new("RGB", (size, size), (0, 0, 0))
    d = ImageDraw.Draw(img)
    for side in (-1, 1):
        cx, cy = size / 2 + side * 15 * S, size / 2 + 2 * S
        d.ellipse((cx - 10 * S, cy - 6 * S, cx + 10 * S, cy + 6 * S), fill=(170, 90, 255))
        d.ellipse((cx - 6 * S, cy - 4 * S, cx + 6 * S, cy + 4 * S), fill=(255, 220, 255))
        d.ellipse((cx - 1.5 * S, cy - 5 * S, cx + 1.5 * S, cy + 5 * S), fill=(0, 0, 0))
        d.line((cx - side * 11 * S, cy - 13 * S, cx + side * 9 * S, cy - 7 * S), fill=(170, 90, 255), width=2 * S)
    img = img.resize((64, 64), Image.LANCZOS)
    return bloom(img, 2, 1.1)


# ---------------------------------------------------------------- paper cranes, quill of fate

def crane_paper():
    """32 x 32 pale lavender origami paper with crease lines and a gilded edge."""
    img = Image.new("RGBA", (32, 32))
    px = img.load()
    for y in range(32):
        for x in range(32):
            t = (x + y) / 62
            px[x, y] = g.lerp((250, 244, 255), (206, 184, 246), t + .1 * fbm(x / 6, y / 6, 2, 101)) + (255,)
    d = ImageDraw.Draw(img)
    crease = (176, 150, 220, 255)
    d.line((0, 0, 31, 31), fill=crease)
    d.line((0, 31, 31, 0), fill=crease)
    d.line((16, 0, 16, 31), fill=(190, 170, 228, 255))
    d.rectangle((0, 0, 31, 31), outline=GILT + (255,))
    return img


def ink_stroke():
    """64 x 160 glossy black ink stroke (alpha), tiling along its length; violet sheen and spatter on the edges."""
    W, H = 64, 160
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px = img.load()
    for y in range(H):
        a = y / H * math.tau
        for x in range(W):
            u = (x + .5) / W * 2 - 1
            # Periodic along the stroke so tiles join
            wob = .12 * fbm(math.cos(a) * 1.5 + 4, math.sin(a) * 1.5 + 4, 3, 111) + .06 * math.sin(a * 3)
            edge = .72 + wob
            r = abs(u - wob * .6)
            if r > edge:
                continue
            sheen = smoothstep(.5, .9, fbm(math.cos(a) * 3 + u * 2, math.sin(a) * 3 + 9, 3, 112)) * (1 - r / edge)
            col = g.lerp(INK, INK_SHEEN, sheen)
            alpha = int(240 * min(1.0, (edge - r) * 9))
            px[x, y] = col + (alpha,)
    d = ImageDraw.Draw(img)
    rnd = random.Random(113)
    for _ in range(10):
        y = rnd.uniform(4, H - 4)
        x = rnd.choice((rnd.uniform(1, 8), rnd.uniform(W - 8, W - 1)))
        s = rnd.uniform(1, 2.5)
        d.ellipse((x - s, y - s, x + s, y + s), fill=INK + (230,))
    return img


def ink_stroke_runes():
    """Additive runes shining inside the ink stroke, same tiling."""
    S = 4
    W, H = 64 * S, 160 * S
    img = Image.new("RGB", (W, H), (0, 0, 0))
    d = ImageDraw.Draw(img)
    col = VIOLET_LIGHT
    rnd = random.Random(121)
    for k in range(4):
        cx, cy = W / 2, (k + .5) * H / 4
        s = 11 * S
        kind = k % 4
        if kind == 0:
            d.polygon([(cx, cy - s), (cx + s, cy), (cx, cy + s), (cx - s, cy)], outline=col, width=2 * S)
            d.line((cx, cy - s * .5, cx, cy + s * .5), fill=col, width=2 * S)
        elif kind == 1:
            d.line((cx, cy - s, cx, cy + s), fill=col, width=2 * S)
            d.line((cx, cy - s * .2, cx + s * .8, cy - s), fill=col, width=2 * S)
            d.line((cx, cy + s * .3, cx - s * .8, cy - s * .4), fill=col, width=2 * S)
        elif kind == 2:
            d.ellipse((cx - s * .7, cy - s * .7, cx + s * .7, cy + s * .7), outline=col, width=2 * S)
            d.line((cx - s, cy, cx + s, cy), fill=col, width=2 * S)
        else:
            d.line([(cx - s * .7, cy - s), (cx + s * .5, cy - s * .2), (cx - s * .5, cy + s * .2), (cx + s * .7, cy + s)], fill=col, width=2 * S)
        for _ in range(3):
            y = cy + rnd.uniform(-.4, .4) * H / 4 + H / 8
            d.line((cx - rnd.uniform(2, 6) * S, y, cx + rnd.uniform(2, 6) * S, y), fill=VIOLET, width=S)
    img = img.resize((64, 160), Image.LANCZOS)
    return bloom(img, 1.5, 1.0)


def quill():
    """48 x 128 additive spectral quill: a long feather with barbs, the shaft running down to a golden nib."""
    S = 4
    W, H = 48 * S, 128 * S
    img = Image.new("RGB", (W, H), (0, 0, 0))
    d = ImageDraw.Draw(img)
    cx = W / 2
    top, bottom = 4 * S, 96 * S
    # Vane: barbs leaning up and out from the shaft, longest in the middle
    for k in range(70):
        t = k / 69
        y = top + t * (bottom - top)
        width = math.sin(min(1, t * 1.15) * math.pi) ** .7 * 19 * S * (1 - .25 * t)
        bright = int(170 + 85 * (1 - t))
        col = (bright - 30, bright - 50, 255)
        for side in (-1, 1):
            if (k + (side > 0)) % 9 == 0:
                continue
            d.line((cx, y, cx + side * width, y - width * .55), fill=col, width=int(S * 1.2))
    d.line((cx, top, cx, H - 22 * S), fill=(255, 255, 255), width=int(S * 1.6))
    # Nib
    d.polygon([(cx - 4 * S, H - 24 * S), (cx + 4 * S, H - 24 * S), (cx, H - S)], fill=(255, 210, 110))
    d.line((cx, H - 22 * S, cx, H - 4 * S), fill=(120, 80, 20), width=S)
    img = img.resize((48, 128), Image.LANCZOS)
    return bloom(img, 2, .9)


def rune_pillar():
    """48 x 160 additive pillar of rune light: violet beam with a white core, glyphs rising along it."""
    W, H = 48, 160
    img = Image.new("RGB", (W, H), (0, 0, 0))
    px = img.load()
    for y in range(H):
        v = y / H
        fade = smoothstep(0, .35, v) * (.6 + .4 * v)
        for x in range(W):
            u = abs((x + .5) / W * 2 - 1)
            streak = .7 + .3 * fbm(x / 3, y / 18, 2, 131)
            core = math.exp(-(u / .18) ** 2)
            beam = math.exp(-(u / .5) ** 2) * streak
            r = (beam * 150 + core * 255) * fade
            g_ = (beam * 80 + core * 240) * fade
            b = (beam * 255 + core * 255) * fade
            px[x, y] = (min(255, int(r)), min(255, int(g_)), min(255, int(b)))
    d = ImageDraw.Draw(img)
    rnd = random.Random(132)
    for k in range(5):
        cy = 22 + k * 26
        cx = W / 2 + rnd.uniform(-4, 4)
        s = 5
        col = (255, 236, 170)
        if k % 2 == 0:
            d.polygon([(cx, cy - s), (cx + s, cy), (cx, cy + s), (cx - s, cy)], outline=col)
        else:
            d.line((cx, cy - s, cx, cy + s), fill=col)
            d.line((cx - s, cy - 2, cx + s, cy + 2), fill=col)
    return bloom(img, 1.2, .8)


# ---------------------------------------------------------------- icons

BG = ((60, 28, 96), NIGHT)


def small_page(d, cx, cy, w, h, angle, fill=PARCH, rune=VIOLET):
    ca, sa = math.cos(angle), math.sin(angle)
    pts = [(cx + x * ca - y * sa, cy + x * sa + y * ca) for x, y in ((-w / 2, -h / 2), (w / 2, -h / 2), (w / 2, h / 2), (-w / 2, h / 2))]
    d.polygon(pts, fill=fill + (255,), outline=BROWN_INK + (255,))
    d.ellipse((cx - w * .18, cy - w * .18, cx + w * .18, cy + w * .18), outline=rune + (255,), width=max(1, int(w // 8)))


def page_storm_icon():
    def draw(d, img, S):
        c = S / 2
        g.glow_spot(img, c, c, .42 * S, (150, 90, 255), .9)
        for k in range(9):
            t = k / 9
            a = t * math.tau * 1.3
            r = (.12 + .3 * t) * S
            small_page(d, c + math.cos(a) * r, c + math.sin(a) * r * .8, .13 * S, .17 * S, a + .6)
        d.arc((.1 * S, .14 * S, .9 * S, .82 * S), 200, 340, fill=VIOLET_LIGHT + (200,), width=max(1, S // 40))
        d.arc((.16 * S, .2 * S, .84 * S, .9 * S), 20, 160, fill=VIOLET_LIGHT + (200,), width=max(1, S // 40))
    return g.painted(64, draw, (.5, .5) + BG)


def living_ink_icon():
    """Pixel art (a painted hand does not survive the downscale): a black hand rising from a pool of ink."""
    hand = g.from_grid([
        "................",
        "....v.v.v.v.....",
        "...vkvkvkvkv....",
        "...vkvkvkvkv....",
        "...vkvkvkvkv.v..",
        "...vkkkkkkkvvkv.",
        "...vkkkkkkkkkv..",
        "...vkkkkkkkkv...",
        "....vkkklkkv....",
        "....vkkkkkkv....",
        ".....vkkkkv.....",
        ".....vkkkkv.....",
        "..vvvvkkkkvvvv..",
        ".vkkkkkkkkkkkkv.",
        "vkkkkkkkkkkkkkkv",
        ".vvvvvvvvvvvvvv.",
    ], {"k": INK, "v": VIOLET, "l": VIOLET_LIGHT})
    img = g.spell_background(8, 6, (120, 70, 200), NIGHT)
    img.alpha_composite(hand)
    return img


def final_chapter_icon():
    def draw(d, img, S):
        c = S / 2
        g.glow_spot(img, c, .3 * S, .45 * S, (255, 220, 140), 1.0)
        # Dome of pages
        for k in range(11):
            a = math.pi + k * math.pi / 10
            small_page(d, c + math.cos(a) * .42 * S, .9 * S + math.sin(a) * .5 * S, .1 * S, .13 * S, a + math.pi / 2)
        # The open grimoire above
        d.polygon([(c, .2 * S), (.14 * S, .1 * S), (.14 * S, .36 * S), (c, .44 * S)], fill=LEATHER + (255,), outline=GILT + (255,))
        d.polygon([(c, .2 * S), (.86 * S, .1 * S), (.86 * S, .36 * S), (c, .44 * S)], fill=LEATHER + (255,), outline=GILT + (255,))
        d.polygon([(c, .24 * S), (.19 * S, .15 * S), (.19 * S, .33 * S), (c, .41 * S)], fill=PARCH + (255,))
        d.polygon([(c, .24 * S), (.81 * S, .15 * S), (.81 * S, .33 * S), (c, .41 * S)], fill=PARCH + (255,))
        d.line((c, .2 * S, c, .44 * S), fill=GILT + (255,), width=max(1, S // 32))
        # A rune going off below
        g.glow_spot(img, c, .78 * S, .12 * S, (190, 120, 255), 1.3)
        d.ellipse((c - .06 * S, .72 * S, c + .06 * S, .84 * S), outline=VIOLET_LIGHT + (255,), width=max(1, S // 40))
    return g.painted(64, draw, (.5, .3) + BG)


def razor_pages_icon():
    """Pixel art: three pages with gilded razor edges flying up and to the right."""
    img = g.spell_background(10, 6, (120, 74, 190), NIGHT)
    d = ImageDraw.Draw(img)
    for x0, y0 in ((1, 9), (5, 5), (9, 1)):
        # Speed streak behind each page
        d.line((x0 - 1, y0 + 6, x0 - 2, y0 + 7), fill=VIOLET_LIGHT + (200,))
        d.rectangle((x0, y0, x0 + 4, y0 + 5), fill=PARCH + (255,), outline=GILT + (255,))
        d.point((x0 + 4, y0), fill=(255, 250, 220, 255))
        d.line((x0 + 1, y0 + 2, x0 + 3, y0 + 2), fill=VIOLET + (255,))
        d.point((x0 + 2, y0 + 3), fill=VIOLET + (255,))
    return img


def tome_of_judgment_icon():
    def draw(d, img, S):
        c = S / 2
        g.glow_spot(img, c, .8 * S, .4 * S, (255, 120, 90), 1.0)
        # Ground and the impact
        d.line((.06 * S, .86 * S, .94 * S, .86 * S), fill=GILT + (255,), width=max(1, S // 24))
        for k in range(5):
            a = math.pi + (k + 1) * math.pi / 6
            d.line((c + math.cos(a) * .12 * S, .84 * S + math.sin(a) * .12 * S, c + math.cos(a) * .3 * S, .84 * S + math.sin(a) * .3 * S),
                   fill=(255, 220, 160, 255), width=max(1, S // 32))
        # The book clapping shut, spine up: two crimson covers meeting at the bottom
        d.polygon([(c, .1 * S), (.16 * S, .32 * S), (c - .03 * S, .8 * S)], fill=(130, 20, 34, 255), outline=GILT + (255,))
        d.polygon([(c, .1 * S), (.84 * S, .32 * S), (c + .03 * S, .8 * S)], fill=(150, 26, 40, 255), outline=GILT + (255,))
        d.polygon([(c, .16 * S), (.24 * S, .34 * S), (c - .02 * S, .74 * S)], fill=PARCH + (255,))
        d.polygon([(c, .16 * S), (.76 * S, .34 * S), (c + .02 * S, .74 * S)], fill=PARCH_DARK + (255,))
        d.line((c, .1 * S, c, .8 * S), fill=GILT + (255,), width=max(1, S // 30))
    return g.painted(64, draw, (.5, .8, (90, 20, 30), NIGHT))


def bookmark_icon():
    return g.from_grid([
        "................",
        "....ggggggg.....",
        "...gooooooog....",
        "...gopppppog....",
        "...goppvppog....",
        "...gopvvvpog....",
        "...goppvppog....",
        "...gopppppog....",
        "...goppvppog....",
        "...gopvlvpog....",
        "...goppvppog....",
        "...gopppppog....",
        "...goppopppog...",
        "...gopo.oppog...",
        "...goo...oog....",
        "...gg.....g.....",
    ], {"g": (34, 14, 52), "o": (70, 30, 120), "p": (140, 80, 220), "v": GILT, "l": (255, 245, 200)})


def seal_of_silence_icon():
    def draw(d, img, S):
        c = S / 2
        g.glow_spot(img, c, c, .48 * S, (170, 90, 255), .9)
        seal = silence_seal().resize((int(S * .86), int(S * .86)), Image.LANCZOS)
        img.alpha_composite(seal, (int(S * .07), int(S * .07)))
    return g.painted(64, draw, (.5, .5) + BG)


def forbidden_library_icon():
    """Pixel art: a grimoire gaping open, fangs along its pages, an eye on the cover, its clasp at the back."""
    book = g.from_grid([
        "................",
        "..oooooooo......",
        ".occccccccoo....",
        ".occcyyccccco...",
        "..oppppppppcco..",
        "...w.w.w.w.ppco.",
        "............pcob",
        "...........vpcob",
        "............pco.",
        "...w.w.w.w.ppco.",
        "..oppppppppcco..",
        ".occccccccccoo..",
        ".oooooooooooo...",
        "................",
        "................",
        "................",
    ], {"o": (8, 18, 22), "c": (34, 70, 80), "p": PARCH, "w": (255, 250, 236), "y": (220, 150, 255),
        "v": VIOLET_LIGHT, "b": (90, 70, 58)})
    img = g.spell_background(8, 8, (90, 50, 150), NIGHT)
    img.alpha_composite(book)
    return img


def word_of_power_icon():
    def draw(d, img, S):
        glyphs = power_glyphs().resize((S * 2, S), Image.LANCZOS)
        g.glow_spot(img, S / 2, .55 * S, .45 * S, (255, 110, 30), .9)
        for k, i in enumerate((0, 3, 6)):
            cell = glyphs.crop(((i % 4) * S // 2, (i // 4) * S // 2, (i % 4 + 1) * S // 2, (i // 4 + 1) * S // 2))
            cell = cell.resize((int(S * .42), int(S * .42)), Image.LANCZOS)
            px = cell.load()
            for y in range(cell.size[1]):
                for x in range(cell.size[0]):
                    r, gg, b, a = px[x, y]
                    px[x, y] = (r, gg, b, min(255, int(max(r, gg, b) * 1.4)))
            img.alpha_composite(cell, (int((.02 + k * .29) * S), int((.3 - (k % 2) * .06) * S)))
    return g.painted(64, draw, (.5, .55, (90, 30, 10), NIGHT))


def rewrite_icon():
    def draw(d, img, S):
        c = S / 2
        g.glow_spot(img, c, c, .45 * S, (255, 220, 140), .8)
        # Page with a line struck out and rewritten in gold
        small_page(d, .44 * S, .56 * S, .6 * S, .72 * S, -.12, rune=GILT)
        for k in range(4):
            y = (.36 + k * .1) * S
            d.line((.22 * S, y, .62 * S, y - .05 * S), fill=(BROWN_INK if k != 1 else INK) + (255,), width=max(1, S // 40))
        d.line((.2 * S, .44 * S, .64 * S, .38 * S), fill=GILT + (255,), width=max(1, S // 24))
        # Quill
        d.polygon([(.9 * S, .06 * S), (.7 * S, .5 * S), (.62 * S, .62 * S), (.66 * S, .48 * S)], fill=(250, 245, 255, 255), outline=VIOLET + (255,))
        d.line((.86 * S, .1 * S, .62 * S, .64 * S), fill=VIOLET_DARK + (255,), width=max(1, S // 40))
    return g.painted(64, draw, (.5, .5) + BG)


def paper_cranes_icon():
    """Pixel art: an origami crane gliding, neck raised, wings swept back."""
    crane = g.from_grid([
        "................",
        "...........w....",
        "..........wl....",
        ".........wl.....",
        ".l......wl......",
        ".wl....wwl......",
        "..wl..wwwl......",
        "...wwwwwwwl.....",
        "....wwwwwwwl....",
        ".....sswwwwwl...",
        "......ssswwwwl..",
        ".......sss...wl.",
        "........s.....w.",
        "................",
        "................",
        "................",
    ], {"w": (252, 246, 255), "l": (196, 170, 240), "s": (150, 110, 220)})
    img = g.spell_background(8, 8, (90, 50, 150), NIGHT)
    img.alpha_composite(crane)
    return img


def quill_of_fate_icon():
    def draw(d, img, S):
        g.glow_spot(img, .4 * S, .78 * S, .4 * S, (170, 100, 255), 1.0)
        # Ink line with pillars bursting from it
        d.line((.06 * S, .86 * S, .7 * S, .7 * S), fill=INK + (255,), width=max(2, S // 14))
        for k, t in enumerate((.15, .35, .55)):
            x = (.06 + .64 * t) * S
            y = (.86 - .16 * t) * S
            d.rectangle((x - .03 * S, y - (.3 + .08 * k) * S, x + .03 * S, y), fill=(200, 160, 255, 210))
            d.rectangle((x - .012 * S, y - (.3 + .08 * k) * S, x + .012 * S, y), fill=(255, 255, 255, 255))
        # The quill writing at the end of the line
        d.polygon([(.96 * S, .04 * S), (.78 * S, .5 * S), (.7 * S, .7 * S), (.72 * S, .48 * S)], fill=(240, 230, 255, 255), outline=VIOLET + (255,))
        d.line((.94 * S, .08 * S, .7 * S, .7 * S), fill=VIOLET_DARK + (255,), width=max(1, S // 40))
        d.polygon([(.68 * S, .62 * S), (.74 * S, .64 * S), (.7 * S, .72 * S)], fill=GILT + (255,))
    return g.painted(64, draw, (.4, .8) + BG)


def parchment_aegis_icon():
    def draw(d, img, S):
        c = S / 2
        g.glow_spot(img, c, c, .3 * S, (255, 220, 150), 1.0)
        d.polygon([(c, .3 * S), (.66 * S, .38 * S), (.62 * S, .62 * S), (c, .72 * S), (.38 * S, .62 * S), (.34 * S, .38 * S)],
                  outline=GILT + (255,), width=max(1, S // 28))
        for k in range(6):
            a = k * math.tau / 6 - math.pi / 2
            small_page(d, c + math.cos(a) * .36 * S, c + math.sin(a) * .36 * S, .16 * S, .21 * S, a + math.pi / 2)
    return g.painted(64, draw, (.5, .5) + BG)


def silence_effect_icon():
    def draw(d, img, S):
        seal = silence_seal().resize((S, S), Image.LANCZOS)
        img.alpha_composite(seal)
    return g.painted(72, draw, None, 18)


def parchment_aegis_effect_icon():
    def draw(d, img, S):
        c = S / 2
        d.polygon([(c, .08 * S), (.86 * S, .22 * S), (.8 * S, .62 * S), (c, .92 * S), (.2 * S, .62 * S), (.14 * S, .22 * S)],
                  fill=PARCH + (255,), outline=GILT_DARK + (255,), width=max(1, S // 20))
        _rune_glyphs(d, c, .48 * S, .2 * S, VIOLET + (255,), max(1, S // 24), random.Random(8))
    return g.painted(72, draw, None, 18)


# ---------------------------------------------------------------- items

def scroll_book():
    return g.from_grid([
        "................",
        "..........oo....",
        ".........ohlo...",
        "........oclllo..",
        ".......occcbso..",
        "......occcccoo..",
        ".....occccxco...",
        "....occcxxcco...",
        "...occcxggxo....",
        "..occccxggo.....",
        ".oohccccxo......",
        "ohllocccco......",
        "olbbsocco.......",
        ".osssoco........",
        "..ooo.o.........",
        "................",
    ], {"o": (26, 10, 40), "h": VIOLET_LIGHT, "l": VIOLET, "b": (110, 60, 190), "s": VIOLET_DARK,
        "c": g.CREAM, "x": g.CREAM_SHADE, "g": GILT})


def affinity_ring_book():
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
        "g": VIOLET, "t": VIOLET_DARK})


def living_grimoire_icon():
    return g.from_grid([
        "................",
        "...oooooooooo...",
        "..ohhllllllbco..",
        "..ohoooooooocco.",
        "..olo.vvvv.bcco.",
        "..olovyyyyvbccpp",
        "..oloyeekeybcppx",
        "..olovyyyyvbcco.",
        "..olo.vvvv.bcco.",
        "..olo..r.r.bcco.",
        "..olooooooobcco.",
        "..olbbbbbbbbcco.",
        "..osbbbbbbbsxco.",
        "...ooooooooooo..",
        "................",
        "................",
    ], {"o": LEATHER_DARK, "h": VIOLET_LIGHT, "l": VIOLET, "b": LEATHER, "s": (40, 14, 64),
        "c": PARCH, "x": PARCH_DARK, "p": (255, 250, 236), "v": VIOLET, "y": GILT, "e": (180, 120, 255), "k": NIGHT, "r": VIOLET_LIGHT})


GRIMOIRE_FRAMES = 8
BLINK_FRAME = 6


def living_grimoire_model_texture():
    """Animated texture of the 3D Living Grimoire (8 frames of 64 x 64 stacked, Golden Grimoire layout): violet
    leather with rune veins that pulse, an eye in the bezel that looks around and blinks, pages that riffle."""
    s = 4
    frames = []
    rnd = random.Random(141)
    veins = []
    W, H = 12 * s, 9 * s
    for k in range(6):
        x, y = W / 2, H / 2
        a = k * math.tau / 6 + rnd.uniform(-.3, .3)
        pts = [(x, y)]
        for _ in range(9):
            a += rnd.uniform(-.6, .6)
            x += math.cos(a) * 2.6
            y += math.sin(a) * 2.6
            pts.append((x, y))
        veins.append(pts)
    look = [-1.2, -.6, 0, .6, 1.2, .4, 0, -.4]
    for f in range(GRIMOIRE_FRAMES):
        img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)

        def box(u0, v0, u1, v1, fill):
            d.rectangle((u0 * s, v0 * s, u1 * s - 1, v1 * s - 1), fill=fill + (255,))

        pulse = .5 + .5 * math.sin(f / GRIMOIRE_FRAMES * math.tau)
        # Cover: leather and pulsing rune veins
        for y in range(H):
            for x in range(W):
                n = fbm(x / 7, y / 7, 3, 142)
                img.putpixel((x, y), g.lerp(LEATHER, LEATHER_DARK, n * 1.1) + (255,))
        vein_col = g.lerp(VIOLET_DARK, VIOLET_LIGHT, pulse)
        for pts in veins:
            d.line(pts, fill=vein_col + (255,), width=1)
        d.ellipse((W / 2 - 4, H / 2 - 4, W / 2 + 3, H / 2 + 3), fill=g.lerp(VIOLET, VIOLET_LIGHT, pulse) + (255,))
        d.rectangle((0, 0, W - 1, H - 1), outline=LEATHER_DARK + (255,))
        d.rectangle((1, 1, W - 2, H - 2), outline=GILT + (255,))
        # Cover edge
        box(12, 0, 13, 9, LEATHER)
        # Pages side: lines riffling
        for y in range(0, 8 * s):
            for x in range(13 * s, 16 * s):
                img.putpixel((x, y), g.lerp(PARCH, PARCH_DARK, fbm(x / 3, y / 3, 2, 143) * .7) + (255,))
        for y in range(1, 8 * s, 2):
            off = int(round(math.sin(y * .7 + f * .8) * 1.2))
            d.line((13 * s + max(0, off), y, 16 * s - 1 + min(0, off), y), fill=PARCH_DARK + (255,))
        # Spine with a glowing seam
        for y in range(9 * s, 12 * s):
            for x in range(0, 12 * s):
                t = abs(y - 10.5 * s) / (1.5 * s)
                img.putpixel((x, y), g.lerp((84, 40, 130), LEATHER_DARK, t) + (255,))
        d.line((0, 10 * s + 1, 12 * s - 1, 10 * s + 1), fill=g.lerp(VIOLET, VIOLET_LIGHT, pulse) + (255,))
        # Page edges: the riffle travels along them
        box(0, 12, 12, 15, PARCH)
        for x in range(0, 12 * s, 2):
            wave = int(round(math.sin(x * .4 - f * .8) * 1.5))
            d.line((x, 12 * s + 1 + max(0, wave), x, 15 * s - 2 + min(0, wave)), fill=PARCH_DARK + (255,))
        # Gilt fittings
        box(12, 9, 14, 11, GILT)
        d.rectangle((12 * s, 9 * s, 14 * s - 1, 11 * s - 1), outline=GILT_DARK + (255,))
        # The eye in the bezel
        gx0, gy0 = 14 * s, 9 * s
        box(14, 9, 16, 11, (240, 226, 200))
        if f == BLINK_FRAME:
            box(14, 9, 16, 11, LEATHER)
            d.line((gx0, gy0 + 4, gx0 + 7, gy0 + 4), fill=LEATHER_DARK + (255,))
        else:
            cx = gx0 + 3.5 + look[f]
            d.ellipse((cx - 2.5, gy0 + 1.5, cx + 2.5, gy0 + 6.5), fill=GILT + (255,))
            d.ellipse((cx - 1.5, gy0 + 2.5, cx + 1.5, gy0 + 5.5), fill=VIOLET + (255,))
            d.line((cx, gy0 + 2, cx, gy0 + 6), fill=NIGHT + (255,))
        # Strap with glowing stitches
        box(12, 11, 16, 12, LEATHER_DARK)
        for x in range(12 * s + 1, 16 * s, 3):
            img.putpixel((x, 11 * s + 1), vein_col + (255,))
            img.putpixel((x, 12 * s - 2), vein_col + (255,))
        frames.append(img)
    sheet = Image.new("RGBA", (64, 64 * GRIMOIRE_FRAMES))
    for f, img in enumerate(frames):
        sheet.paste(img, (0, 64 * f))
    return sheet


def living_grimoire_animation():
    """The eye looks left and right, blinks once in a while; the pages riffle now and then."""
    seq = [0, 1, 2, 3, 4, 5, 4, 3, 2, 1, 0, 1, 2, 3, {"index": BLINK_FRAME, "time": 2}, 3, 2, 1, 7, 7]
    return {"animation": {"frametime": 4, "frames": seq}}


def main():
    g.save(page_texture(), "textures/entity/page.png")
    g.save(page_runes(), "textures/entity/page_runes.png")
    g.save(tome_edge(), "textures/entity/tome_edge.png")
    g.save(tome_cover(), "textures/entity/tome_cover.png")
    g.save(chapter_rune(), "textures/entity/chapter_rune.png")
    g.save(ink_splash(), "textures/entity/ink_splash.png")
    g.save(ink_hand(), "textures/entity/ink_hand.png")
    g.save(ink_hand_glow(), "textures/entity/ink_hand_glow.png")
    g.save(page_storm_icon(), "textures/gui/spell_icons/page_storm.png")
    g.save(living_ink_icon(), "textures/gui/spell_icons/living_ink.png")
    g.save(final_chapter_icon(), "textures/gui/spell_icons/the_final_chapter.png")
    g.save(scroll_book(), "textures/item/scroll_book.png")
    g.save(affinity_ring_book(), "textures/item/affinity_ring_book.png")
    g.write_json("models/item/scroll_book.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/scroll_book"}})
    g.write_json("models/item/affinity_ring_book.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/affinity_ring_book"}})
    # Razor Pages, Word of Power, Tome of Judgment
    g.save(razor_edge(), "textures/entity/razor_edge.png")
    g.save(power_glyphs(), "textures/entity/power_glyphs.png")
    g.save(judgment_cover(), "textures/entity/judgment_cover.png")
    g.save(judgment_frame(), "textures/entity/judgment_frame.png")
    # Bookmark, Seal of Silence, Rewrite
    g.save(bookmark_ribbon(), "textures/entity/bookmark.png")
    g.save(bookmark_glow(), "textures/entity/bookmark_glow.png")
    g.save(silence_seal(), "textures/entity/silence_seal.png")
    g.save(silence_glyph(), "textures/entity/silence_glyph.png")
    g.save(silence_ring(), "textures/entity/silence_ring.png")
    g.save(script_band(), "textures/entity/script_band.png")
    # Forbidden Library
    g.save(grimoire_cover(), "textures/entity/grimoire_cover.png")
    g.save(grimoire_teeth(), "textures/entity/grimoire_teeth.png")
    g.save(grimoire_eyes(), "textures/entity/grimoire_eyes.png")
    # Paper Cranes, Quill of Fate
    g.save(crane_paper(), "textures/entity/crane_paper.png")
    g.save(ink_stroke(), "textures/entity/ink_stroke.png")
    g.save(ink_stroke_runes(), "textures/entity/ink_stroke_runes.png")
    g.save(quill(), "textures/entity/quill.png")
    g.save(rune_pillar(), "textures/entity/rune_pillar.png")
    for name, fn in (("razor_pages", razor_pages_icon), ("tome_of_judgment", tome_of_judgment_icon), ("bookmark", bookmark_icon),
                     ("seal_of_silence", seal_of_silence_icon), ("forbidden_library", forbidden_library_icon),
                     ("word_of_power", word_of_power_icon), ("rewrite", rewrite_icon), ("paper_cranes", paper_cranes_icon),
                     ("quill_of_fate", quill_of_fate_icon), ("parchment_aegis", parchment_aegis_icon)):
        g.save(fn(), f"textures/gui/spell_icons/{name}.png")
    g.save(silence_effect_icon(), "textures/mob_effect/silence.png")
    g.save(parchment_aegis_effect_icon(), "textures/mob_effect/parchment_aegis.png")
    # Living Grimoire
    g.save(living_grimoire_icon(), "textures/item/living_grimoire.png")
    g.save(living_grimoire_model_texture(), "textures/item/living_grimoire_3d.png")
    g.write_json("textures/item/living_grimoire_3d.png.mcmeta", living_grimoire_animation())
    g.book_models("living_grimoire")
