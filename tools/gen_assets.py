#!/usr/bin/env python3
"""Draws every texture of the addon and writes the item models that go with them.

All art is original (Iron's Spells' license forbids reusing its assets). Run from the repository root:
    python3 tools/gen_assets.py
Needs Pillow. Re-running overwrites the files listed in OUTPUTS below and nothing else.
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
    """32x32 texture for the 3D book: cover, cover edge, spine, pages and clasp gem (2 px per model unit)."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Cover: uv [0,0,11,8] -> px 22x16
    d.rectangle((0, 0, 21, 15), fill=BASE + (255,))
    d.rectangle((0, 0, 21, 15), outline=OUTLINE + (255,))
    d.rectangle((1, 1, 20, 14), outline=LIGHT + (255,))
    d.rectangle((3, 3, 18, 12), outline=SHADOW + (255,))
    for (x, y) in ((1, 1), (20, 1), (1, 14), (20, 14)):
        d.rectangle((x - (0 if x < 10 else 1), y - (0 if y < 8 else 1), x + (1 if x < 10 else 0), y + (1 if y < 8 else 0)), fill=HIGH + (255,))
    # Sun sigil in the middle of the cover
    cx, cy = 10.5, 7.5
    for a in range(8):
        ang = a * math.pi / 4
        d.line((cx + math.cos(ang) * 2, cy + math.sin(ang) * 2, cx + math.cos(ang) * 4, cy + math.sin(ang) * 4), fill=HIGH + (255,))
    d.ellipse((cx - 2, cy - 2, cx + 2, cy + 2), fill=TOPAZ + (255,), outline=TOPAZ_DARK + (255,))
    img.putpixel((10, 6), TOPAZ_LIGHT + (255,))
    # Cover edge: uv [11,0,12,8] -> px 22..23 x 0..15
    d.rectangle((22, 0, 23, 15), fill=LIGHT + (255,))
    d.line((23, 0, 23, 15), fill=SHADOW + (255,))
    # Spine: uv [12,0,15,11] -> px 24..29 x 0..21
    d.rectangle((24, 0, 29, 21), fill=SHADOW + (255,))
    d.line((25, 0, 25, 21), fill=BASE + (255,))
    for y in (3, 10, 17):
        d.line((24, y, 29, y), fill=LIGHT + (255,))
        d.line((24, y + 1, 29, y + 1), fill=OUTLINE + (255,))
    # Pages: uv [0,8,11,16] -> px 0..21 x 16..31
    d.rectangle((0, 16, 21, 31), fill=CREAM + (255,))
    for y in range(17, 32, 2):
        d.line((0, y, 21, y), fill=CREAM_SHADE + (255,))
    # Clasp gem: uv [15,0,16,2] -> px 30..31 x 0..3
    d.rectangle((30, 0, 31, 3), fill=TOPAZ + (255,))
    img.putpixel((30, 0), TOPAZ_LIGHT + (255,))
    img.putpixel((31, 3), TOPAZ_DARK + (255,))
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


# ---------------------------------------------------------------- models

def item_models():
    write_json("models/item/scroll_gold.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/scroll_gold"}})
    write_json("models/item/affinity_ring_gold.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/affinity_ring_gold"}})
    write_json("models/item/golden_grimoire.json", {
        "parent": "minecraft:item/handheld",
        "loader": "forge:separate_transforms",
        "base": {"parent": "arcanes_oublies:item/golden_grimoire_3d"},
        "perspectives": {"gui": {"parent": "arcanes_oublies:item/golden_grimoire_gui"}},
    })
    write_json("models/item/golden_grimoire_gui.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/golden_grimoire"}})

    t = "#book"
    cover = {"uv": [0, 0, 11, 8], "texture": t}
    cover_flipped = {"uv": [11, 0, 0, 8], "texture": t}
    edge = {"uv": [11, 0, 12, 8], "texture": t}
    pages = {"uv": [0, 8, 11, 16], "texture": t}
    spine_top = {"uv": [12, 0, 15, 11], "texture": t}
    gem = {"uv": [15, 0, 16, 2], "texture": t}

    def box(frm, to, faces):
        return {"from": frm, "to": to, "faces": faces}

    edges = {k: edge for k in ("north", "south", "up", "down")}
    elements = [
        # Left and right covers (thin slabs standing on their edge, spine up)
        box([5.5, 0, 2.5], [6.5, 8, 13.5], dict(edges, west=cover_flipped, east=edge)),
        box([9.5, 0, 2.5], [10.5, 8, 13.5], dict(edges, east=cover, west=edge)),
        # Spine across the top
        box([6.5, 7, 2.5], [9.5, 8, 13.5], {"up": spine_top, "down": edge, "north": edge, "south": edge}),
        # Page block
        box([6.5, 0.5, 3], [9.5, 7, 13], {"north": pages, "south": pages, "down": pages}),
        # Clasp gem on the front cover
        box([10.5, 3, 7], [11, 5, 9], {"east": gem, "north": gem, "south": gem, "up": gem, "down": gem}),
    ]
    write_json("models/item/golden_grimoire_3d.json", {
        "textures": {"book": "arcanes_oublies:item/golden_grimoire_3d", "particle": "arcanes_oublies:item/golden_grimoire_3d"},
        "elements": elements,
        "display": {
            "thirdperson_righthand": {"rotation": [90, 180, 0], "translation": [0, -1.5, 2], "scale": [0.7, 0.7, 0.7]},
            "thirdperson_lefthand": {"rotation": [90, 180, 0], "translation": [0, -1.5, 2], "scale": [0.7, 0.7, 0.7]},
            "firstperson_righthand": {"rotation": [-130, 10, 90], "translation": [-4, 2, -3.5]},
            "firstperson_lefthand": {"rotation": [-130, 10, -90], "translation": [4, 2, -3]},
            "ground": {"rotation": [-90, 0, 0], "translation": [0, 0, -4], "scale": [0.7, 0.7, 0.7]},
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
    item_models()


if __name__ == "__main__":
    main()
