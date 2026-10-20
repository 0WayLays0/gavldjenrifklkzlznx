#!/usr/bin/env python3
"""Draws every texture of the addon and writes the item models that go with them.

All art is original (Iron's Spells' license forbids reusing its assets). Run from the repository root:
    python3 tools/gen_assets.py
Needs Pillow. Re-running overwrites the files it writes (see main() here and in explosion_assets.py, book_assets.py,
errethys_assets.py, physical_assets.py and boss_assets.py) and nothing else.
"""
import json
import math
import os

from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "arcanes_oublies")

# Gold palette, darkest to brightest
OUTLINE = (78, 42, 6)
SHADOW = (160, 96, 14)
BASE = (226, 160, 30)
LIGHT = (250, 206, 66)
HIGH = (255, 240, 160)
GLINT = (255, 255, 236)

CREAM = (243, 229, 190)
CREAM_SHADE = (206, 184, 132)
PARCH_LINE = (118, 84, 42)
TOPAZ = (255, 214, 40)
TOPAZ_DARK = (196, 120, 0)
TOPAZ_LIGHT = (255, 250, 200)


def save(img, rel):
    path = os.path.join(ROOT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path, optimize=True)
    print("wrote", os.path.relpath(path))


def write_json(rel, data):
    path = os.path.join(ROOT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
        f.write("\n")
    print("wrote", os.path.relpath(path))


def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(len(a)))


def from_grid(rows, legend, size=16):
    """Pixel art from a character grid; '.' is transparent."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == size, (y, row, len(row))
        for x, ch in enumerate(row):
            if ch != ".":
                c = legend[ch]
                img.putpixel((x, y), c + (255,) if len(c) == 3 else c)
    return img


def spell_background(cx, cy, inner, outer, radius=11.0):
    """Opaque 16x16 icon background: a soft glow around (cx, cy) fading to a dark rim."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + .5 - cx, y + .5 - cy) / radius
            img.putpixel((x, y), lerp(inner, outer, d) + (255,))
    return img


def outline_pass(img, color):
    """Adds a 1px outline around opaque pixels of a transparent sprite."""
    out = img.copy()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            if img.getpixel((x, y))[3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < w and 0 <= ny < h and img.getpixel((nx, ny))[3] > 0:
                        out.putpixel((x, y), color + (255,))
                        break
    return out


# ---------------------------------------------------------------- spell icons

def gold_bolt_icon():
    # Comet flying toward the bottom-left: a glowing ingot head and a tapering tail of molten gold
    img = spell_background(4.5, 11.5, (112, 66, 10), (26, 14, 6), 13)
    head = (4.6, 11.4)
    tail_dir = (1 / math.sqrt(2), -1 / math.sqrt(2))
    perp = (1 / math.sqrt(2), 1 / math.sqrt(2))
    sprite = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            px, py = x + .5 - head[0], y + .5 - head[1]
            s = px * tail_dir[0] + py * tail_dir[1]
            r = px * perp[0] + py * perp[1]
            if -2.6 <= s <= 2.2 and abs(r) <= 1.75:
                # Ingot: lit top-left face, shaded bottom-right face, bright ridge
                if r < -0.9:
                    c = HIGH
                elif r < 0.1:
                    c = LIGHT
                elif r < 0.9:
                    c = BASE
                else:
                    c = SHADOW
                if s < -2.0 and r < 0.5:
                    c = GLINT
                sprite.putpixel((x, y), c + (255,))
            elif s > 2.2 and abs(r) <= max(0.0, 2.3 - (s - 2.2) * .19):
                t = (s - 2.2) / 12.0
                if abs(r) < 0.8:
                    c = lerp(GLINT, LIGHT, t * 1.4)
                elif abs(r) < 1.5:
                    c = lerp(LIGHT, BASE, t * 1.2)
                else:
                    c = lerp(BASE, SHADOW, t)
                sprite.putpixel((x, y), c + (255,))
    sprite = outline_pass(sprite, OUTLINE)
    # Keep the tail end soft: no outline on the last stretch of the tail
    for y in range(16):
        for x in range(16):
            px, py = x + .5 - head[0], y + .5 - head[1]
            s = px * tail_dir[0] + py * tail_dir[1]
            if s > 7 and sprite.getpixel((x, y))[:3] == OUTLINE:
                sprite.putpixel((x, y), (0, 0, 0, 0))
    img.alpha_composite(sprite)
    for (x, y) in ((1, 8), (8, 14), (11, 9), (6, 2)):
        img.putpixel((x, y), GLINT + (255,))
    for (x, y) in ((1, 7), (1, 9), (0, 8), (2, 8)):
        img.putpixel((x, y), lerp(img.getpixel((x, y))[:3], LIGHT, .6) + (255,))
    return img


def gilded_chains_icon():
    img = spell_background(8, 8, (94, 58, 12), (22, 12, 10), 11)
    sprite = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    # Two thick chains crossing in an X: bright link faces alternate with shaded joints every 2 px
    for k in range(1, 15):
        joint = (k // 2) % 2 == 1
        for (x, y) in ((k, k), (k + 1, k), (15 - k, k), (14 - k, k)):
            if 0 <= x < 16:
                if joint:
                    c = SHADOW
                else:
                    c = HIGH if (x + y) % 3 == 0 else LIGHT
                sprite.putpixel((x, y), c + (255,))
    sprite = outline_pass(sprite, OUTLINE)
    img.alpha_composite(sprite)
    # Small padlock where the chains cross
    lock = from_grid([
        "................",
        "................",
        "................",
        "................",
        "................",
        "......oooo......",
        ".....ohoohs.....",
        ".....oo..oo.....",
        ".....oooooo.....",
        ".....ohlllo.....",
        ".....olbobo.....",
        ".....obbsso.....",
        ".....oooooo.....",
        "................",
        "................",
        "................",
    ], {"o": OUTLINE, "h": HIGH, "l": LIGHT, "b": BASE, "s": SHADOW})
    img.alpha_composite(lock)
    return img


# ---------------------------------------------------------------- items

def scroll_gold():
    return from_grid([
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
    ], {"o": OUTLINE, "h": HIGH, "l": LIGHT, "b": BASE, "s": SHADOW,
        "c": CREAM, "x": CREAM_SHADE, "g": TOPAZ})


def affinity_ring_gold():
    return from_grid([
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
    ], {"o": OUTLINE, "h": HIGH, "l": LIGHT, "b": BASE, "s": SHADOW,
        "g": TOPAZ, "t": TOPAZ_DARK})


def golden_grimoire_icon():
    return from_grid([
        "................",
        "...oooooooooo...",
        "..ohhllllllbco..",
        "..ohooooooobco..",
        "..olo.lhl.obco..",
        "..olo.hgh.obco..",
        "..olollglloobco.",
        "..olo.hgh.obcco.",
        "..olo.lhl.obcco.",
        "..olo.....obcco.",
        "..olooooooobcco.",
        "..olbbbbbbbbcco.",
        "..osbbbbbbbsxco.",
        "...ooooooooooo..",
        "................",
        "................",
    ], {"o": OUTLINE, "h": HIGH, "l": LIGHT, "b": BASE, "s": SHADOW,
        "g": TOPAZ, "c": CREAM, "x": CREAM_SHADE})


def golden_grimoire_model_texture():
    """64x64 texture of the 3D book, 4 px per model unit. Regions (in model units):
    cover [0,0,12,9], cover edge [12,0,13,9], pages side [13,0,16,8], spine [0,9,12,12], pages edge [0,12,12,15],
    gold fittings [12,9,14,11], gem [14,9,16,11], strap [12,11,16,12]."""
    s = 4
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    def box(u0, v0, u1, v1, fill, outline=None):
        d.rectangle((u0 * s, v0 * s, u1 * s - 1, v1 * s - 1), fill=fill + (255,), outline=None if outline is None else outline + (255,))

    # Cover: deep amber leather framed by gold
    W, H = 12 * s, 9 * s
    for y in range(H):
        for x in range(W):
            t = math.hypot(x - W / 2, y - H / 2) / (W / 2)
            c = lerp((176, 98, 18), (108, 52, 8), t)
            # leather grain
            if (x * 7 + y * 13) % 11 == 0:
                c = lerp(c, (90, 40, 6), .5)
            img.putpixel((x, y), c + (255,))
    d.rectangle((0, 0, W - 1, H - 1), outline=OUTLINE + (255,))
    d.rectangle((1, 1, W - 2, H - 2), outline=LIGHT + (255,))
    d.rectangle((2, 2, W - 3, H - 3), outline=BASE + (255,))
    d.rectangle((4, 4, W - 5, H - 5), outline=SHADOW + (255,))
    # Filigree in the corners
    for (cx, cy, sx, sy) in ((5, 5, 1, 1), (W - 6, 5, -1, 1), (5, H - 6, 1, -1), (W - 6, H - 6, -1, -1)):
        for i in range(5):
            img.putpixel((cx + sx * i, cy), HIGH + (255,))
            img.putpixel((cx, cy + sy * i), HIGH + (255,))
        img.putpixel((cx + sx * 2, cy + sy * 2), LIGHT + (255,))
        img.putpixel((cx + sx * 3, cy + sy * 3), BASE + (255,))
    # Sun sigil around the gem
    cx, cy = W / 2 - .5, H / 2 - .5
    for k in range(12):
        ang = k * math.pi / 6
        r1 = 9 if k % 2 == 0 else 7
        d.line((cx + math.cos(ang) * 6, cy + math.sin(ang) * 6, cx + math.cos(ang) * r1, cy + math.sin(ang) * r1), fill=HIGH + (255,))
    d.ellipse((cx - 5.5, cy - 5.5, cx + 5.5, cy + 5.5), outline=LIGHT + (255,))
    # Cover edge
    box(12, 0, 13, 9, BASE)
    d.line((12 * s, 0, 12 * s, H - 1), fill=HIGH + (255,))
    # Pages side (north/south faces of the page block)
    box(13, 0, 16, 8, CREAM)
    for y in range(1, 8 * s, 2):
        d.line((13 * s, y, 16 * s - 1, y), fill=CREAM_SHADE + (255,))
    # Spine: gold-tooled leather
    for y in range(9 * s, 12 * s):
        for x in range(0, 12 * s):
            t = abs(y - 10.5 * s) / (1.5 * s)
            img.putpixel((x, y), lerp((150, 80, 12), (96, 44, 6), t) + (255,))
    d.line((0, 9 * s, 12 * s - 1, 9 * s), fill=LIGHT + (255,))
    d.line((0, 12 * s - 1, 12 * s - 1, 12 * s - 1), fill=OUTLINE + (255,))
    for x in range(2, 12 * s, 6):
        img.putpixel((x, 10 * s + 2), HIGH + (255,))
    # Page edges (bottom of the book)
    box(0, 12, 12, 15, CREAM)
    for x in range(0, 12 * s, 2):
        d.line((x, 12 * s, x, 15 * s - 1), fill=CREAM_SHADE + (255,))
    # Gold fittings: corner guards, spine bands, gem bezel
    box(12, 9, 14, 11, LIGHT)
    d.rectangle((12 * s, 9 * s, 14 * s - 1, 11 * s - 1), outline=SHADOW + (255,))
    d.line((12 * s + 1, 9 * s + 1, 14 * s - 2, 9 * s + 1), fill=HIGH + (255,))
    # Topaz
    for y in range(9 * s, 11 * s):
        for x in range(14 * s, 16 * s):
            t = math.hypot(x - 14.6 * s, y - 9.6 * s) / (1.6 * s)
            img.putpixel((x, y), lerp(TOPAZ_LIGHT, TOPAZ_DARK, t) + (255,))
    # Strap: dark leather with gold stitching
    box(12, 11, 16, 12, (92, 46, 18))
    for x in range(12 * s + 1, 16 * s, 3):
        img.putpixel((x, 11 * s + 1), BASE + (255,))
        img.putpixel((x, 12 * s - 2), BASE + (255,))
    return img


# ---------------------------------------------------------------- entity textures

def gilded_chain_tile():
    """Vertically tiling chain: a face-on link then an edge-on link. Transparent background (cutout)."""
    return from_grid([
        "....oooooooo....",
        "...ohhllllbso...",
        "..ohlo....obso..",
        "..olo......oso..",
        "..olo......oso..",
        "..obo......oso..",
        "..obso....osso..",
        "...obbbbbbsso...",
        "....oooooooo....",
        "......ohso......",
        "......ohso......",
        "......olso......",
        "......olso......",
        "......obso......",
        "......obso......",
        "......ooso......",
    ], {"o": OUTLINE, "h": HIGH, "l": LIGHT, "b": BASE, "s": SHADOW})


def gold_glow():
    """Additive halo (black = invisible)."""
    size = 32
    img = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            d = math.hypot(x + .5 - size / 2, y + .5 - size / 2) / (size / 2)
            k = max(0.0, 1 - d) ** 2.2
            core = max(0.0, 1 - d * 3) ** 2
            r = min(255, int(255 * k + 40 * core))
            g = min(255, int(200 * k + 55 * core))
            b = min(255, int(70 * k + 120 * core))
            img.putpixel((x, y), (r, g, b, 255))
    return img


def gold_circle():
    """Additive rune circle (black = invisible): rings, a band of runes, a hexagram and a small sun."""
    s = 4
    size = 128 * s
    c = size / 2
    lines = Image.new("RGB", (size, size), (0, 0, 0))
    d = ImageDraw.Draw(lines)
    col = (255, 196, 64)
    bright = (255, 236, 150)

    def ring(radius, width, color):
        d.ellipse((c - radius, c - radius, c + radius, c + radius), outline=color, width=int(width))

    ring(61 * s, 2.2 * s, bright)
    ring(57.5 * s, 1 * s, col)
    ring(47 * s, 1.6 * s, bright)
    ring(22 * s, 1.2 * s, col)
    # Rune band between r=48 and r=57: 20 glyphs built from short strokes
    glyphs = [
        [(0, -1, 0, 1), (-.6, -1, .6, -1)],
        [(-.6, 1, 0, -1), (0, -1, .6, 1)],
        [(0, -1, 0, 1), (0, 0, .6, -.6), (0, 0, .6, .6)],
        [(-.6, -1, .6, 1), (-.6, 1, .6, -1)],
        [(0, -1, 0, 1), (-.6, 0, .6, 0)],
        [(-.6, -1, -.6, 1), (.6, -1, .6, 1), (-.6, 0, .6, 0)],
        [(0, -1, .6, 0), (.6, 0, 0, 1), (0, 1, -.6, 0), (-.6, 0, 0, -1)],
        [(-.6, -1, .6, -1), (.6, -1, -.6, 1), (-.6, 1, .6, 1)],
    ]
    for i in range(20):
        ang = i * 2 * math.pi / 20
        gx, gy = c + math.cos(ang) * 52.3 * s, c + math.sin(ang) * 52.3 * s
        scale = 3.0 * s
        # Glyphs stand upright relative to the circle (rotated with the angle)
        ca, sa = math.cos(ang + math.pi / 2), math.sin(ang + math.pi / 2)
        for (x0, y0, x1, y1) in glyphs[i % len(glyphs)]:
            p0 = (gx + (x0 * ca - y0 * sa) * scale, gy + (x0 * sa + y0 * ca) * scale)
            p1 = (gx + (x1 * ca - y1 * sa) * scale, gy + (x1 * sa + y1 * ca) * scale)
            d.line((p0, p1), fill=col, width=int(1.1 * s))
    # Hexagram inscribed in the inner ring
    for start in (-math.pi / 2, math.pi / 2):
        pts = [(c + math.cos(start + k * 2 * math.pi / 3) * 46 * s, c + math.sin(start + k * 2 * math.pi / 3) * 46 * s) for k in range(3)]
        d.line(pts + [pts[0]], fill=col, width=int(1.3 * s), joint="curve")
    # Sun in the middle
    for k in range(12):
        ang = k * math.pi / 6
        r0, r1 = 7 * s, (13 if k % 2 == 0 else 10) * s
        d.line((c + math.cos(ang) * r0, c + math.sin(ang) * r0, c + math.cos(ang) * r1, c + math.sin(ang) * r1), fill=bright, width=int(1.2 * s))
    d.ellipse((c - 5 * s, c - 5 * s, c + 5 * s, c + 5 * s), fill=bright)

    lines = lines.resize((128, 128), Image.LANCZOS)
    glow = lines.filter(ImageFilter.GaussianBlur(3))
    out = Image.new("RGB", (128, 128))
    lp, gp = lines.load(), glow.load()
    op = out.load()
    for y in range(128):
        for x in range(128):
            a, b = lp[x, y], gp[x, y]
            op[x, y] = tuple(min(255, int(a[i] + b[i] * .9)) for i in range(3))
    return out.convert("RGBA")


# ---------------------------------------------------------------- painted icons (drawn large, scaled down)

def painted(size, draw, background=None, final=16):
    """Draws at {size}x{size} with draw(d, img), then scales down to final x final (soft, painterly like Iron's icons)."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    if background is not None:
        cx, cy, inner, outer = background
        bg = Image.new("RGBA", (size, size))
        for y in range(size):
            for x in range(size):
                t = math.hypot(x - cx * size, y - cy * size) / (size * .75)
                bg.putpixel((x, y), lerp(inner, outer, t) + (255,))
        img = bg
    d = ImageDraw.Draw(img)
    draw(d, img, size)
    small = img.resize((final, final), Image.LANCZOS)
    # Bring back crisp edges lost in the downscale
    return small.filter(ImageFilter.UnsharpMask(radius=1, percent=90, threshold=0))


def glow_spot(img, cx, cy, radius, color, strength=1.0):
    """Adds light around (cx, cy) (additive)."""
    w, h = img.size
    px = img.load()
    for y in range(max(0, int(cy - radius)), min(h, int(cy + radius) + 1)):
        for x in range(max(0, int(cx - radius)), min(w, int(cx + radius) + 1)):
            t = math.hypot(x - cx, y - cy) / radius
            if t < 1:
                k = (1 - t) ** 2 * strength
                r, g, b, a = px[x, y]
                px[x, y] = (min(255, int(r + color[0] * k)), min(255, int(g + color[1] * k)), min(255, int(b + color[2] * k)), max(a, int(255 * min(1, k * 2))))


def ingot_poly(cx, cy, length, width, angle):
    """Top face of an ingot as a parallelogram-ish trapezoid, rotated."""
    ca, sa = math.cos(angle), math.sin(angle)
    pts = [(-length / 2, -width / 2), (length / 2, -width / 2), (length / 2 + width * .3, width / 2), (-length / 2 - width * .3, width / 2)]
    return [(cx + x * ca - y * sa, cy + x * sa + y * ca) for x, y in pts]


def goldfall_icon():
    def draw(d, img, S):
        for i, (x, y, l) in enumerate(((.3, .3, .2), (.62, .2, .16), (.5, .58, .24))):
            # falling streak
            d.line((x * S + S * .15, y * S - S * .25, x * S, y * S), fill=(255, 200, 80, 140), width=int(S * .08))
            poly = ingot_poly(x * S, y * S, l * S, l * S * .45, math.radians(-20))
            d.polygon(poly, fill=BASE + (255,), outline=OUTLINE + (255,))
            d.line(poly[0:2], fill=HIGH + (255,), width=max(1, S // 40))
        glow_spot(img, .5 * S, .9 * S, .45 * S, (255, 170, 40), 1.2)
        d.line((0, .93 * S, S, .93 * S), fill=(60, 30, 8, 255), width=int(S * .1))
    return painted(64, draw, (.5, .9, (90, 54, 14), (14, 10, 26)))


def golden_spikes_icon():
    def draw(d, img, S):
        glow_spot(img, .5 * S, .85 * S, .6 * S, (255, 180, 50), .9)
        for x, h, w in ((.18, .35, .14), (.42, .55, .17), (.7, .78, .2)):
            base = .9 * S
            tip = (x * S, base - h * S)
            left = (x * S - w * S / 2, base)
            right = (x * S + w * S / 2, base)
            d.polygon([left, tip, (x * S, base)], fill=HIGH + (255,))
            d.polygon([(x * S, base), tip, right], fill=SHADOW + (255,))
            d.line([left, tip, right], fill=OUTLINE + (255,), width=max(1, S // 40))
        d.rectangle((0, .9 * S, S, S), fill=(50, 26, 8, 255))
    return painted(64, draw, (.5, .8, (80, 46, 10), (16, 10, 8)))


def midas_curse_icon():
    def draw(d, img, S):
        glow_spot(img, .5 * S, .45 * S, .55 * S, (255, 190, 60), .7)
        # Golden bust: head and shoulders
        d.ellipse((.33 * S, .16 * S, .67 * S, .54 * S), fill=BASE + (255,), outline=OUTLINE + (255,), width=max(1, S // 32))
        d.pieslice((.12 * S, .5 * S, .88 * S, 1.3 * S), 180, 360, fill=SHADOW + (255,), outline=OUTLINE + (255,), width=max(1, S // 32))
        d.ellipse((.38 * S, .2 * S, .52 * S, .36 * S), fill=HIGH + (255,))
        # Cracks of the shattering statue
        crack = (40, 16, 4, 255)
        w = max(1, S // 40)
        d.line([(.55 * S, .2 * S), (.5 * S, .32 * S), (.58 * S, .42 * S), (.52 * S, .54 * S)], fill=crack, width=w)
        d.line([(.5 * S, .62 * S), (.4 * S, .74 * S), (.45 * S, .86 * S)], fill=crack, width=w)
        d.line([(.6 * S, .66 * S), (.7 * S, .8 * S)], fill=crack, width=w)
    return painted(64, draw, (.5, .4, (70, 40, 12), (18, 10, 6)))


def aurum_blade_icon():
    def draw(d, img, S):
        glow_spot(img, .5 * S, .55 * S, .55 * S, (255, 200, 80), .9)
        w = max(1, S // 40)
        # Blade pointing down
        d.polygon([(.43 * S, .3 * S), (.57 * S, .3 * S), (.57 * S, .82 * S), (.5 * S, .97 * S), (.43 * S, .82 * S)], fill=LIGHT + (255,), outline=OUTLINE + (255,))
        d.line((.5 * S, .32 * S, .5 * S, .92 * S), fill=GLINT + (255,), width=w)
        # Crossguard, grip and pommel
        d.rectangle((.24 * S, .22 * S, .76 * S, .3 * S), fill=BASE + (255,), outline=OUTLINE + (255,))
        d.rectangle((.45 * S, .08 * S, .55 * S, .22 * S), fill=(120, 60, 16, 255), outline=OUTLINE + (255,))
        d.ellipse((.42 * S, .01 * S, .58 * S, .12 * S), fill=TOPAZ + (255,), outline=OUTLINE + (255,))
    return painted(64, draw, (.5, .55, (84, 52, 14), (12, 10, 22)))


def molten_torrent_icon():
    def draw(d, img, S):
        glow_spot(img, .75 * S, .85 * S, .5 * S, (255, 110, 10), 1.2)
        w = int(S * .16)
        pts = [(.1 * S, .1 * S), (.35 * S, .3 * S), (.55 * S, .55 * S), (.72 * S, .82 * S)]
        d.line(pts, fill=(230, 110, 10, 255), width=w, joint="curve")
        d.line(pts, fill=(255, 190, 40, 255), width=int(w * .55), joint="curve")
        d.line(pts[:3], fill=(255, 240, 160, 255), width=int(w * .2), joint="curve")
        d.ellipse((.5 * S, .82 * S, .98 * S, .98 * S), fill=(255, 150, 20, 255))
        for x, y in ((.25 * S, .5 * S), (.45 * S, .78 * S), (.85 * S, .62 * S)):
            d.ellipse((x - .04 * S, y - .04 * S, x + .04 * S, y + .04 * S), fill=(255, 200, 60, 255))
    return painted(64, draw, (.6, .8, (70, 30, 6), (14, 8, 6)))


def chestplate_shape(d, S, ox=0.0, oy=0.0, scale=1.0):
    def p(x, y):
        return (ox * S + x * S * scale, oy * S + y * S * scale)
    body = [p(.2, .18), p(.38, .12), p(.5, .2), p(.62, .12), p(.8, .18), p(.92, .4), p(.8, .46), p(.78, .9), p(.22, .9), p(.2, .46), p(.08, .4)]
    d.polygon(body, fill=BASE + (255,), outline=OUTLINE + (255,))
    d.polygon([p(.3, .25), p(.5, .3), p(.5, .85), p(.3, .85)], fill=LIGHT + (255,))
    d.line([p(.5, .22), p(.5, .88)], fill=SHADOW + (255,), width=max(1, int(S * scale) // 40))
    d.line([p(.25, .55), p(.75, .55)], fill=SHADOW + (255,), width=max(1, int(S * scale) // 50))
    d.line([p(.32, .27), p(.32, .8)], fill=HIGH + (255,), width=max(1, int(S * scale) // 40))


def gilded_armor_icon():
    def draw(d, img, S):
        glow_spot(img, .5 * S, .5 * S, .6 * S, (255, 190, 60), .8)
        chestplate_shape(d, S)
        for x, y in ((.15 * S, .75 * S), (.85 * S, .7 * S), (.12 * S, .25 * S)):
            d.line((x - .05 * S, y, x + .05 * S, y), fill=GLINT + (255,), width=max(1, S // 32))
            d.line((x, y - .05 * S, x, y + .05 * S), fill=GLINT + (255,), width=max(1, S // 32))
    return painted(64, draw, (.5, .5, (76, 46, 12), (14, 10, 10)))


def helmet_shape(d, S):
    w = max(1, S // 32)
    d.chord((.18 * S, .1 * S, .82 * S, .8 * S), 180, 360, fill=BASE + (255,), outline=OUTLINE + (255,), width=w)
    d.rectangle((.18 * S, .44 * S, .82 * S, .86 * S), fill=BASE + (255,), outline=OUTLINE + (255,), width=w)
    d.rectangle((.26 * S, .2 * S, .4 * S, .8 * S), fill=LIGHT + (255,))
    d.rectangle((.47 * S, .08 * S, .53 * S, .5 * S), fill=HIGH + (255,))
    # Glowing visor slit
    d.rectangle((.24 * S, .5 * S, .76 * S, .58 * S), fill=(30, 14, 4, 255))
    d.rectangle((.3 * S, .52 * S, .42 * S, .56 * S), fill=GLINT + (255,))
    d.rectangle((.58 * S, .52 * S, .7 * S, .56 * S), fill=GLINT + (255,))


def golden_sentinels_icon():
    def draw(d, img, S):
        glow_spot(img, .5 * S, .45 * S, .6 * S, (255, 190, 60), .8)
        helmet_shape(d, S)
    return painted(64, draw, (.5, .45, (70, 44, 12), (12, 10, 18)))


def sun_of_eldorado_icon():
    def draw(d, img, S):
        c = S / 2
        for k in range(16):
            ang = k * math.pi / 8
            r = (.47 if k % 2 == 0 else .34) * S
            half = math.pi / 22
            d.polygon([(c + math.cos(ang - half) * .2 * S, c + math.sin(ang - half) * .2 * S),
                       (c + math.cos(ang) * r, c + math.sin(ang) * r),
                       (c + math.cos(ang + half) * .2 * S, c + math.sin(ang + half) * .2 * S)], fill=LIGHT + (255,))
        d.ellipse((c - .22 * S, c - .22 * S, c + .22 * S, c + .22 * S), fill=BASE + (255,), outline=OUTLINE + (255,), width=max(1, S // 40))
        glow_spot(img, c, c, .5 * S, (255, 220, 120), 1.0)
        d.ellipse((c - .1 * S, c - .1 * S, c + .1 * S, c + .1 * S), fill=GLINT + (255,))
    return painted(64, draw, (.5, .5, (110, 60, 10), (20, 10, 30)))


def midas_curse_effect_icon():
    def draw(d, img, S):
        d.ellipse((.12 * S, .12 * S, .88 * S, .88 * S), fill=BASE + (255,), outline=OUTLINE + (255,), width=max(1, S // 24))
        d.ellipse((.22 * S, .22 * S, .78 * S, .78 * S), outline=SHADOW + (255,), width=max(1, S // 30))
        d.ellipse((.3 * S, .26 * S, .5 * S, .44 * S), fill=HIGH + (255,))
        d.line([(.58 * S, .2 * S), (.5 * S, .45 * S), (.6 * S, .6 * S), (.48 * S, .82 * S)], fill=(40, 16, 4, 255), width=max(1, S // 24))
    return painted(72, draw, None, 18)


def gilded_armor_effect_icon():
    def draw(d, img, S):
        chestplate_shape(d, S, .02, 0.0, .96)
    return painted(72, draw, None, 18)


def gold_crystal():
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            t = y / 15
            c = lerp(GLINT, BASE, t * 1.2)
            if (x + y) % 6 == 0 or (x - y) % 7 == 0:
                c = lerp(c, HIGH, .6)
            if x in (0, 15):
                c = lerp(c, SHADOW, .5)
            img.putpixel((x, y), c + (255,))
    return img


def molten_pool():
    """Additive (black = invisible): a ragged pool, white-hot centre, orange rim."""
    size = 64
    img = Image.new("RGBA", (size, size))
    c = size / 2
    for y in range(size):
        for x in range(size):
            ang = math.atan2(y - c, x - c)
            edge = .82 + .1 * math.sin(ang * 5) + .06 * math.sin(ang * 11 + 1.3)
            t = math.hypot(x - c, y - c) / (c * edge)
            if t >= 1:
                img.putpixel((x, y), (0, 0, 0, 255))
                continue
            swirl = .5 + .5 * math.sin(t * 9 + ang * 3)
            col = lerp((255, 245, 190), (255, 140, 15), t ** .8)
            col = lerp(col, (255, 200, 60), swirl * .3)
            k = min(1, (1 - t) * 3)
            img.putpixel((x, y), tuple(int(v * k) for v in col) + (255,))
    return img


def gold_beam():
    """Additive beam cross-section, constant along the beam."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            t = abs(x + .5 - 8) / 8
            core = max(0.0, 1 - t * 3)
            k = (1 - t) ** 1.5
            img.putpixel((x, y), (min(255, int(255 * k + 40 * core)), min(255, int(190 * k + 60 * core)), min(255, int(50 * k + 150 * core)), 255))
    return img


def eldorado_sun():
    """Additive crown of rays around an empty centre (the core is drawn with the glow)."""
    s = 4
    size = 128 * s
    c = size / 2
    img = Image.new("RGB", (size, size), (0, 0, 0))
    d = ImageDraw.Draw(img)
    for k in range(24):
        ang = k * math.pi / 12
        r = (61 if k % 2 == 0 else 46) * s
        half = math.pi / (40 if k % 2 == 0 else 52)
        d.polygon([(c + math.cos(ang - half) * 20 * s, c + math.sin(ang - half) * 20 * s),
                   (c + math.cos(ang) * r, c + math.sin(ang) * r),
                   (c + math.cos(ang + half) * 20 * s, c + math.sin(ang + half) * 20 * s)], fill=(255, 190, 60))
    d.ellipse((c - 24 * s, c - 24 * s, c + 24 * s, c + 24 * s), outline=(255, 235, 150), width=3 * s)
    img = img.resize((128, 128), Image.LANCZOS)
    glow = img.filter(ImageFilter.GaussianBlur(4))
    out = Image.new("RGB", (128, 128))
    a, b, o = img.load(), glow.load(), out.load()
    for y in range(128):
        for x in range(128):
            o[x, y] = tuple(min(255, int(a[x, y][i] + b[x, y][i] * .8)) for i in range(3))
    return out.convert("RGBA")


def golden_sentinel_skin():
    """64x64 humanoid skin (zombie layout): dark bronze body under the armor, gold mask with a dark visor."""
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for y in range(64):
        for x in range(64):
            n = ((x * 13 + y * 7) % 9) / 9
            img.putpixel((x, y), lerp((120, 72, 14), (90, 50, 10), n) + (255,))
    d = ImageDraw.Draw(img)
    # Head front (8..16, 8..16): gold mask with a visor slit
    d.rectangle((8, 8, 15, 15), fill=BASE + (255,))
    d.rectangle((8, 8, 15, 8), fill=HIGH + (255,))
    d.rectangle((8, 11, 15, 12), fill=(30, 14, 4, 255))
    d.line((11, 13, 12, 15), fill=SHADOW + (255,))
    # Hat layer (40..64, 0..16) left empty
    d.rectangle((32, 0, 63, 15), fill=(0, 0, 0, 0))
    return img


def golden_sentinel_eyes():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for x in (9, 10, 13, 14):
        img.putpixel((x, 11), (255, 240, 150, 255))
        img.putpixel((x, 12), (255, 200, 60, 255))
    return img


# ---------------------------------------------------------------- models

def item_models():
    write_json("models/item/scroll_gold.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/scroll_gold"}})
    write_json("models/item/affinity_ring_gold.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/affinity_ring_gold"}})
    book_models("golden_grimoire")


def book_models(name):
    """A spellbook: flat icon in the GUI, the 3D book (textures/item/{name}_3d.png, grimoire layout) everywhere else."""
    write_json(f"models/item/{name}.json", {
        "parent": "minecraft:item/handheld",
        "loader": "neoforge:separate_transforms",
        "base": {"parent": f"arcanes_oublies:item/{name}_3d"},
        "perspectives": {"gui": {"parent": f"arcanes_oublies:item/{name}_gui"}},
    })
    write_json(f"models/item/{name}_gui.json", {"parent": "item/generated", "textures": {"layer0": f"arcanes_oublies:item/{name}"}})

    t = "#book"

    def f(u0, v0, u1, v1, rotation=0):
        face = {"uv": [u0, v0, u1, v1], "texture": t}
        if rotation:
            face["rotation"] = rotation
        return face

    cover = f(0, 0, 12, 9)
    cover_mirror = f(12, 0, 0, 9)
    edge = f(12, 0, 13, 9)
    pages_side = f(13, 0, 16, 8)
    spine = f(0, 9, 12, 12, 90)
    pages_edge = f(0, 12, 12, 15, 90)
    gold = f(12, 9, 14, 11)
    gem = f(14, 9, 16, 11)
    strap = f(12, 11, 16, 12)

    def box(frm, to, faces):
        return {"from": frm, "to": to, "faces": faces}

    def solid(frm, to, face):
        return box(frm, to, {k: face for k in ("north", "south", "east", "west", "up", "down")})

    edges = {k: edge for k in ("north", "south", "up", "down")}
    elements = [
        # Covers (book standing on its open edge, spine up)
        box([4.5, 0, 2], [5.5, 9, 14], dict(edges, west=cover_mirror, east=edge)),
        box([10.5, 0, 2], [11.5, 9, 14], dict(edges, east=cover, west=edge)),
        # Spine and page block
        box([5.5, 8, 2], [10.5, 9.2, 14], {"up": spine, "down": edge, "north": edge, "south": edge}),
        box([5.5, .4, 2.6], [10.5, 8, 13.4], {"north": pages_side, "south": pages_side, "down": pages_edge}),
        # Raised bands across the spine
        solid([4.3, 9.2, 4.2], [11.7, 9.6, 4.8], gold),
        solid([4.3, 9.2, 7.7], [11.7, 9.6, 8.3], gold),
        solid([4.3, 9.2, 11.2], [11.7, 9.6, 11.8], gold),
    ]
    # Gold corner guards on both covers
    for x0, x1 in ((4.1, 4.5), (11.5, 11.9)):
        for y0, y1 in ((0, 1.6), (7.4, 9)):
            for z0, z1 in ((1.8, 3.4), (12.6, 14.2)):
                elements.append(solid([x0, y0, z0], [x1, y1, z1], gold))
    elements += [
        # Gem in its bezel on the front cover
        solid([11.5, 3.1, 6.6], [11.9, 5.9, 9.4], gold),
        solid([11.9, 3.6, 7.1], [12.3, 5.4, 8.9], gem),
        # Clasp strap wrapping the open edge
        solid([4.3, -.3, 7.4], [11.7, 0, 8.6], strap),
        solid([11.5, 0, 7.4], [11.8, 2.4, 8.6], strap),
    ]
    write_json(f"models/item/{name}_3d.json", {
        "textures": {"book": f"arcanes_oublies:item/{name}_3d", "particle": f"arcanes_oublies:item/{name}_3d"},
        "elements": elements,
        "display": {
            "thirdperson_righthand": {"rotation": [90, 180, 0], "translation": [0, -1.5, 2], "scale": [0.65, 0.65, 0.65]},
            "thirdperson_lefthand": {"rotation": [90, 180, 0], "translation": [0, -1.5, 2], "scale": [0.65, 0.65, 0.65]},
            "firstperson_righthand": {"rotation": [-130, 10, 90], "translation": [-4, 2, -3.5], "scale": [0.9, 0.9, 0.9]},
            "firstperson_lefthand": {"rotation": [-130, 10, -90], "translation": [4, 2, -3], "scale": [0.9, 0.9, 0.9]},
            "ground": {"rotation": [-90, 0, 0], "translation": [0, 0, -4], "scale": [0.65, 0.65, 0.65]},
            "head": {"rotation": [0, 0, -90], "translation": [4, 8, 0]},
        },
    })


def main():
    save(gold_bolt_icon(), "textures/gui/spell_icons/gold_bolt.png")
    save(gilded_chains_icon(), "textures/gui/spell_icons/gilded_chains.png")
    save(scroll_gold(), "textures/item/scroll_gold.png")
    save(affinity_ring_gold(), "textures/item/affinity_ring_gold.png")
    save(golden_grimoire_icon(), "textures/item/golden_grimoire.png")
    save(golden_grimoire_model_texture(), "textures/item/golden_grimoire_3d.png")
    save(gilded_chain_tile(), "textures/entity/gilded_chain.png")
    save(gold_glow(), "textures/entity/gold_glow.png")
    save(gold_circle(), "textures/entity/gold_circle.png")
    save(goldfall_icon(), "textures/gui/spell_icons/goldfall.png")
    save(golden_spikes_icon(), "textures/gui/spell_icons/golden_spikes.png")
    save(midas_curse_icon(), "textures/gui/spell_icons/midas_curse.png")
    save(aurum_blade_icon(), "textures/gui/spell_icons/aurum_blade.png")
    save(molten_torrent_icon(), "textures/gui/spell_icons/molten_torrent.png")
    save(gilded_armor_icon(), "textures/gui/spell_icons/gilded_armor.png")
    save(golden_sentinels_icon(), "textures/gui/spell_icons/golden_sentinels.png")
    save(sun_of_eldorado_icon(), "textures/gui/spell_icons/sun_of_eldorado.png")
    save(midas_curse_effect_icon(), "textures/mob_effect/midas_curse.png")
    save(gilded_armor_effect_icon(), "textures/mob_effect/gilded_armor.png")
    save(gold_crystal(), "textures/entity/gold_crystal.png")
    save(molten_pool(), "textures/entity/molten_pool.png")
    save(gold_beam(), "textures/entity/gold_beam.png")
    save(eldorado_sun(), "textures/entity/eldorado_sun.png")
    save(golden_sentinel_skin(), "textures/entity/golden_sentinel.png")
    save(golden_sentinel_eyes(), "textures/entity/golden_sentinel_eyes.png")
    item_models()
    import explosion_assets
    explosion_assets.main()
    import book_assets
    book_assets.main()
    import errethys_assets
    errethys_assets.main()
    import physical_assets
    physical_assets.main()
    import boss_assets
    boss_assets.main()


if __name__ == "__main__":
    main()
