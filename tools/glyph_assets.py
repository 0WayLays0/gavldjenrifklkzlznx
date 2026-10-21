#!/usr/bin/env python3
"""Glyph Magic art: the light textures the glyphs are written with (stroke, glow, ripple, the vortex's swirl and the
wave's crest), the spell icons (drawn from the same strokes as the game's glyphs), the mark effect icons, the Codex of
the Six Inks and its affinity ring.

Called by gen_assets.py (run that one). All art is original: the six element glyphs were drawn for the addon, and
GLYPHS below must stay the same as spells/glyph/GlyphStrokes.java. Additive textures (drawn with RenderType.eyes) are
greyscale on black, tinted by the renderers.
"""
import math

from PIL import Image, ImageChops, ImageDraw, ImageFilter

import gen_assets as g
from explosion_assets import fbm

# The six elements' ink, and the white-hot core of each (as in GlyphElement.java)
ELEMENTS = {
    "fire": ((255, 122, 42), (255, 224, 160)),
    "frost": ((143, 230, 255), (240, 252, 255)),
    "storm": ((255, 228, 92), (255, 253, 232)),
    "tide": ((63, 155, 255), (210, 235, 255)),
    "gale": ((154, 245, 200), (242, 255, 248)),
    "stone": ((208, 143, 82), (246, 222, 192)),
}
ORDER = ["fire", "frost", "storm", "tide", "gale", "stone"]
INK = (14, 11, 22)
INK_LIGHT = (40, 32, 58)
GOLD = (255, 210, 122)
GOLD_DARK = (176, 120, 40)
GOLD_LIGHT = (255, 240, 196)
NIGHT = (10, 8, 16)


# ---------------------------------------------------------------- the glyphs (same strokes as GlyphStrokes.java)

def _cr(a, b, c, d, t):
    return .5 * (2 * b + (-a + c) * t + (2 * a - 5 * b + 4 * c - d) * t * t + (-a + 3 * b - 3 * c + d) * t * t * t)


def smooth(p, steps=6):
    out = []
    for i in range(len(p) - 1):
        p0, p1, p2, p3 = p[max(0, i - 1)], p[i], p[i + 1], p[min(len(p) - 1, i + 2)]
        for k in range(steps):
            t = k / steps
            out.append((_cr(p0[0], p1[0], p2[0], p3[0], t), _cr(p0[1], p1[1], p2[1], p3[1], t)))
    out.append(p[-1])
    return out


def arc(cx, cy, r, a0, a1, segments):
    return [(cx + math.cos(a0 + (a1 - a0) * i / segments) * r, cy + math.sin(a0 + (a1 - a0) * i / segments) * r)
            for i in range(segments + 1)]


def strokes(element):
    s = []
    if element == "fire":
        s.append(smooth([(0, -.9), (.28, -.5), (-.18, -.1), (.22, .3), (-.06, .66), (.02, .96)]))
        s.append(smooth([(-.18, -.78), (-.56, -.3), (-.44, .18), (-.62, .46)]))
        s.append(smooth([(.18, -.78), (.56, -.26), (.5, .14), (.64, .4)]))
        s.append(smooth([(-.68, -.84), (0, -.98), (.68, -.84)]))
    elif element == "frost":
        for k in range(3):
            a = k * math.pi / 3 + math.pi / 2
            c, d = math.cos(a), math.sin(a)
            s.append([(-c * .92, -d * .92), (c * .92, d * .92)])
        for k in range(6):
            a = k * math.pi / 3 + math.pi / 2
            bx, by = math.cos(a) * .55, math.sin(a) * .55
            for side in (-1, 1):
                b = a + side * .7
                s.append([(bx, by), (bx + math.cos(b) * .28, by + math.sin(b) * .28)])
    elif element == "storm":
        s.append(arc(0, 0, .82, math.pi / 2, math.pi / 2 + math.tau * .92, 34))
        s.append([(-.18, .98), (.28, .12), (-.12, .08), (.22, -.98)])
    elif element == "tide":
        s.append(smooth([(0, .98), (-.2, .68), (0, .52), (.2, .68), (0, .98)]))
        for k in range(3):
            y = .22 - k * .42
            s.append([(-.9 + i * 1.8 / 16, y + .14 * math.sin((-.9 + i * 1.8 / 16 + k * .3) * math.pi * 1.6)) for i in range(17)])
    elif element == "gale":
        spiral = []
        for i in range(30):
            t = i / 29
            a = t * math.tau * 1.5
            r = .08 + t * .62
            spiral.append((math.cos(a) * r, .15 + math.sin(a) * r))
        s.append(spiral)
        s.append(smooth([(-.92, -.52), (-.2, -.66), (.5, -.6), (.92, -.4)]))
        s.append(smooth([(-.7, -.86), (0, -.92), (.7, -.8)]))
    elif element == "stone":
        s.append([(-.86, -.72), (0, .86), (.86, -.72), (-.86, -.72)])
        s.append([(-.36, -.22), (.36, -.22)])
        s.append(arc(0, .22, .1, 0, math.tau, 10))
        s.append([(-.96, -.92), (.96, -.92)])
    return s


def star():
    """The star of the Primordial Glyph: two triangles through the six glyphs, a small ring in the middle."""
    s = []
    for k in range(2):
        s.append([(math.cos(k * math.pi / 3 + i * math.tau / 3), math.sin(k * math.pi / 3 + i * math.tau / 3)) for i in range(4)])
    s.append(arc(0, 0, .3, 0, math.tau, 20))
    return s


def stroke_mask(size, lines, cx, cy, scale, width):
    """A mask of polylines in glyph space (-1..1, y up) drawn at (cx, cy) in a size x size picture."""
    m = Image.new("L", (size, size), 0)
    d = ImageDraw.Draw(m)
    for line in lines:
        pts = [(cx + x * scale, cy - y * scale) for x, y in line]
        d.line(pts, fill=255, width=max(1, round(width)), joint="curve")
        r = width / 2
        for x, y in (pts[0], pts[-1]):
            d.ellipse((x - r, y - r, x + r, y + r), fill=255)
    return m


def paint(img, mask, color, strength=1.0):
    """Lays a colour over img where mask is white."""
    layer = Image.new("RGBA", img.size, color + (255,))
    alpha = mask.point(lambda v: int(v * strength))
    img.alpha_composite(Image.composite(layer, Image.new("RGBA", img.size, (0, 0, 0, 0)), alpha))
    return img


def written(img, lines, color, core, cx=8, cy=8, scale=6.4, width=1.0, glow=1.0):
    """A glyph written in light on img: a soft halo in its colour, then the stroke itself, crisp, one pixel wide
    ({@code width} > 1: wider), its brightest pixels white-hot."""
    k = 8
    w, h = img.size
    halo = stroke_mask(w * k, lines, cx * k, cy * k, scale * k, width * k * 2.2).filter(ImageFilter.GaussianBlur(k * .9))
    paint(img, halo.resize((w, h), Image.LANCZOS), color, .55 * glow)
    line = Image.new("L", (w, h), 0)
    d = ImageDraw.Draw(line)
    for stroke_ in lines:
        pts = [(cx - .5 + x * scale, cy - .5 - y * scale) for x, y in stroke_]
        d.line(pts, fill=255, width=max(1, round(width)))
    paint(img, line, color)
    # The core: where the stroke runs thickest (crossings, curls), it burns white
    dense = line.filter(ImageFilter.BoxBlur(1)).point(lambda v: 255 if v > 150 else 0)
    paint(img, ImageChops.multiply(dense, line), core, .85)
    return img


def dark(color, t=.3):
    return tuple(int(c * t) for c in color)


# ---------------------------------------------------------------- light textures (additive)

def stroke():
    """Across the texture's width: a white core in a soft glow, black at both edges; the same all along."""
    img = Image.new("RGB", (32, 16))
    px = img.load()
    for x in range(32):
        d = abs(x + .5 - 16) / 16
        v = math.exp(-(d / .2) ** 2) + .4 * math.exp(-(d / .5) ** 2)
        v *= min(1, (1 - d) * 4)
        for y in range(16):
            px[x, y] = (int(min(1, v) * 255),) * 3
    return img.convert("RGBA")


def glow():
    img = Image.new("RGB", (64, 64))
    px = img.load()
    for y in range(64):
        for x in range(64):
            r = math.hypot(x + .5 - 32, y + .5 - 32) / 32
            v = max(0.0, 1 - r) ** 2
            px[x, y] = (int(v * 255),) * 3
    return img.convert("RGBA")


def ripple():
    """A ring of light, sharp outside, softening inwards."""
    img = Image.new("RGB", (128, 128))
    px = img.load()
    for y in range(128):
        for x in range(128):
            r = math.hypot(x + .5 - 64, y + .5 - 64) / 64
            v = (math.exp(-((r - .92) / .04) ** 2) + .3 * math.exp(-((r - .8) / .14) ** 2)) if r < 1 else 0
            px[x, y] = (int(min(1, v) * 255),) * 3
    return img.convert("RGBA")


def swirl():
    """Streaks winding up a column, tiling both ways (the vortex walls; the renderer fades them at both ends)."""
    img = Image.new("RGB", (64, 64))
    px = img.load()
    for y in range(64):
        for x in range(64):
            u, v = x / 64 * math.tau, y / 64 * math.tau
            band = .5 + .5 * math.sin(u * 2 + v * 1 + 1.4 * math.sin(u + v * 2))
            fine = .5 + .5 * math.sin(u * 7 + v * 3 + math.sin(v * 2))
            val = .15 + .85 * band ** 3 * (.55 + .45 * fine)
            px[x, y] = (int(min(1, val) * 255),) * 3
    return img.convert("RGBA")


def wave():
    """The wave's face: a bright crest along the top, foam streaks running down and fading (u tiles along the wave)."""
    img = Image.new("RGB", (64, 32))
    px = img.load()
    for y in range(32):
        for x in range(64):
            u, v = x / 64, y / 32
            crest = math.exp(-((v - .1) / .07) ** 2)
            streak = .5 + .5 * math.sin(u * math.tau * 5 + math.sin(u * math.tau * 2) * 2)
            body = (1 - v) ** 1.6 * (.35 + .4 * streak)
            val = crest + body
            px[x, y] = (int(min(1, val) * 255),) * 3
    return img.convert("RGBA")


# ---------------------------------------------------------------- icons

def element_icon(element, extra=None):
    color, core = ELEMENTS[element]
    img = g.spell_background(8, 8, dark(color, .42), NIGHT)
    if extra:
        extra(img, color, core)
    return written(img, strokes(element), color, core)


def fire_glyph_icon():
    return element_icon("fire")


def frost_glyph_icon():
    return element_icon("frost")


def storm_glyph_icon():
    return element_icon("storm")


def tide_glyph_icon():
    return element_icon("tide")


def gale_glyph_icon():
    return element_icon("gale")


def stone_glyph_icon():
    return element_icon("stone")


def spark(img, x, y, color, core):
    """A spark of ink: a white-hot pixel in a cross of its colour."""
    for dx, dy in ((0, -1), (-1, 0), (1, 0), (0, 1)):
        if 0 <= x + dx < img.width and 0 <= y + dy < img.height:
            img.putpixel((x + dx, y + dy), color + (255,))
    img.putpixel((x, y), core + (255,))


def ring(img, cx, cy, r, color):
    d = ImageDraw.Draw(img)
    d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=color + (255,))


def elemental_seal_icon():
    """The six glyphs as six sparks of their colours on a gold ring, turning around a gold seal."""
    img = g.spell_background(8, 8, INK_LIGHT, NIGHT)
    ring(img, 7.5, 7.5, 5.5, GOLD_DARK)
    for i, name in enumerate(ORDER):
        a = math.pi / 2 + i * math.tau / 6
        spark(img, round(7.5 + math.cos(a) * 5.5), round(7.5 - math.sin(a) * 5.5), *ELEMENTS[name])
    spark(img, 7, 7, GOLD, GOLD_LIGHT)
    spark(img, 8, 8, GOLD, GOLD_LIGHT)
    return img


def elemental_script_icon():
    """A line of glyphs written along the ground, each in the next element, erupting one after another."""
    img = g.spell_background(8, 9, INK_LIGHT, NIGHT)
    d = ImageDraw.Draw(img)
    d.line((1, 14, 14, 1), fill=GOLD_DARK + (255,))
    for (x, y), name in zip(((3, 12), (6, 9), (9, 6), (12, 3)), ("fire", "frost", "storm", "tide")):
        color, core = ELEMENTS[name]
        spark(img, x, y, color, core)
        img.putpixel((x - 1, y - 2), color + (255,))
        img.putpixel((x + 1, y - 2), color + (255,))
    return img


def glyph_resonance_icon():
    """Marks on four enemies answering at once: lines of light from a gold centre to sparks of four colours."""
    img = g.spell_background(8, 8, INK_LIGHT, NIGHT)
    d = ImageDraw.Draw(img)
    spots = ((2, 2), (13, 3), (3, 13), (13, 12))
    for (x, y), name in zip(spots, ("fire", "frost", "storm", "gale")):
        d.line((7.5, 7.5, x, y), fill=dark(ELEMENTS[name][0], .75) + (255,))
    for (x, y), name in zip(spots, ("fire", "frost", "storm", "gale")):
        spark(img, x, y, *ELEMENTS[name])
    ring(img, 7.5, 7.5, 2, GOLD)
    img.putpixel((7, 7), GOLD_LIGHT + (255,))
    img.putpixel((8, 8), GOLD_LIGHT + (255,))
    return img


def primordial_glyph_icon():
    """The great glyph in the sky: the star in a gold ring, the six elements at its points."""
    img = g.spell_background(8, 8, (60, 44, 70), NIGHT)
    d = ImageDraw.Draw(img)
    pts = [(7.5 + math.cos(i * math.tau / 6) * 6, 7.5 - math.sin(i * math.tau / 6) * 6) for i in range(6)]
    d.line([pts[0], pts[2], pts[4], pts[0]], fill=GOLD_DARK + (255,))
    d.line([pts[1], pts[3], pts[5], pts[1]], fill=GOLD_DARK + (255,))
    ring(img, 7.5, 7.5, 2, GOLD)
    for (x, y), name in zip(pts, ORDER):
        spark(img, round(x), round(y), *ELEMENTS[name])
    return img


def mark_effect_icon(element):
    """18x18: the element's glyph, glowing on a dark seal."""
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    color, core = ELEMENTS[element]
    d.ellipse((1, 1, 16, 16), fill=NIGHT + (235,), outline=dark(color, .55) + (255,))
    return written(img, strokes(element), color, core, cx=9, cy=9, scale=5.6, width=.95)


def glyph_frozen_effect_icon():
    """18x18: a block of ice, a frost glyph caught in it."""
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle((2, 2, 15, 15), fill=(120, 190, 230, 255), outline=(40, 80, 120, 255))
    d.rectangle((3, 3, 14, 14), outline=(200, 236, 255, 255))
    d.line((4, 5, 6, 3), fill=(240, 252, 255, 255))
    d.line((4, 8, 9, 3), fill=(240, 252, 255, 255))
    return written(img, strokes("frost"), (60, 150, 210), ELEMENTS["frost"][1], cx=9, cy=9, scale=4.4, width=.8, glow=.3)


# ---------------------------------------------------------------- the Codex of the Six Inks

def six_inks_codex_icon():
    """A book bound in night-blue leather, a gold glyph star on the cover and a drop of each of the six inks."""
    img = g.from_grid([
        "................",
        "...oooooooooo...",
        "..obbbbbbbbbsco.",
        "..oboooooooocco.",
        "..obo......oLco.",
        "..obo......oLco.",
        "..obo......oLcco",
        "..obo......oLcco",
        "..obo......oLcco",
        "..obo......oLcco",
        "..oboooooooosco.",
        "..obsssssssssco.",
        "..osssssssssxco.",
        "...ooooooooooo..",
        "................",
        "................",
    ], {"o": NIGHT, "b": (44, 50, 98), "L": (58, 66, 120), "s": (26, 28, 60), "c": g.CREAM, "x": g.CREAM_SHADE})
    spark(img, 7, 7, GOLD, GOLD_LIGHT)
    for i, name in enumerate(ORDER):
        a = math.pi / 2 + i * math.tau / 6
        x, y = round(7.5 + math.cos(a) * 3.2 - .5), round(7 - math.sin(a) * 3.2 - .5)
        img.putpixel((x, y), ELEMENTS[name][0] + (255,))
    return img


def six_inks_codex_model_texture():
    """64x64 texture of the 3D codex, laid out like the other books: covers of night-blue leather tooled in gold with
    the Primordial star, a drop of each ink at its points, old pages, an opal."""
    s = 4
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    def box(u0, v0, u1, v1, fill):
        d.rectangle((u0 * s, v0 * s, u1 * s - 1, v1 * s - 1), fill=fill + (255,))

    leather_dark, leather, leather_light = (24, 26, 58), (42, 48, 96), (66, 74, 132)
    W, H = 12 * s, 9 * s
    for y in range(H):
        for x in range(W):
            n = fbm(x / 5, y / 5, 3, 33)
            img.putpixel((x, y), g.lerp(leather_dark, leather_light, n * .9) + (255,))
    # The gold star, tooled into the cover
    cx, cy, r = W / 2, H / 2, H * .38
    written(img, [arc(0, 0, 1, 0, math.tau, 48)] + star(), GOLD_DARK, GOLD, cx=cx, cy=cy, scale=r, width=.9, glow=.35)
    for i, name in enumerate(ORDER):
        a = i * math.tau / 6
        x, y = cx + math.cos(a) * r, cy - math.sin(a) * r
        color, core = ELEMENTS[name]
        written(img, [arc(0, 0, .1, 0, math.tau, 8)], color, core, cx=x, cy=y, scale=8, width=2.2, glow=.8)
    d.rectangle((0, 0, W - 1, H - 1), outline=NIGHT + (255,))
    d.rectangle((1, 1, W - 2, H - 2), outline=GOLD_DARK + (255,))
    # Gold corners
    for x0, y0 in ((2, 2), (W - 6, 2), (2, H - 6), (W - 6, H - 6)):
        d.rectangle((x0, y0, x0 + 3, y0 + 3), fill=GOLD_DARK + (255,))
        img.putpixel((x0, y0), GOLD_LIGHT + (255,))
    box(12, 0, 13, 9, leather_dark)
    d.line((12 * s, 0, 12 * s, H - 1), fill=GOLD_DARK + (255,))
    # Pages
    for y in range(0, 8 * s):
        for x in range(13 * s, 16 * s):
            img.putpixel((x, y), g.lerp(g.CREAM, (180, 165, 140), fbm(x / 3, y / 3, 2, 9) * .7) + (255,))
    for y in range(1, 8 * s, 2):
        d.line((13 * s, y, 16 * s - 1, y), fill=g.CREAM_SHADE + (255,))
    # Spine: leather, six bands of ink
    for y in range(9 * s, 12 * s):
        for x in range(0, 12 * s):
            t = abs(y - 10.5 * s) / (1.5 * s)
            img.putpixel((x, y), g.lerp(leather, leather_dark, t) + (255,))
    for i, name in enumerate(ORDER):
        x = 3 + i * 7
        d.rectangle((x, 9 * s + 2, x + 2, 12 * s - 3), fill=ELEMENTS[name][0] + (255,))
    # Page edges
    box(0, 12, 12, 15, g.CREAM)
    for x in range(0, 12 * s, 2):
        d.line((x, 12 * s, x, 15 * s - 1), fill=g.CREAM_SHADE + (255,))
    # Fittings: gold
    box(12, 9, 14, 11, GOLD_DARK)
    d.line((12 * s + 1, 9 * s + 1, 14 * s - 2, 9 * s + 1), fill=GOLD_LIGHT + (255,))
    # Opal: all six inks swirling in it
    for y in range(9 * s, 11 * s):
        for x in range(14 * s, 16 * s):
            a = math.atan2(y - 10 * s, x - 15 * s)
            t = math.hypot(x - 14.8 * s, y - 9.8 * s) / (1.4 * s)
            ink = ELEMENTS[ORDER[int((a + math.pi) / math.tau * 6) % 6]][0]
            img.putpixel((x, y), g.lerp((250, 248, 255), ink, min(1, t * 1.2)) + (255,))
    # Strap with gold studs
    box(12, 11, 16, 12, leather_dark)
    for x in range(12 * s + 1, 16 * s, 3):
        img.putpixel((x, 11 * s + 1), GOLD + (255,))
        img.putpixel((x, 12 * s - 2), GOLD + (255,))
    return img


def affinity_ring_glyph():
    img = g.from_grid([
        "................",
        "......oooo......",
        ".....oFWTo......",
        ".....oSGRo......",
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
    ], {"o": NIGHT, "h": GOLD_LIGHT, "l": GOLD, "b": GOLD_DARK, "s": (120, 80, 24),
        "F": ELEMENTS["fire"][0], "W": (250, 248, 255), "T": ELEMENTS["tide"][0],
        "S": ELEMENTS["storm"][0], "G": ELEMENTS["gale"][0], "R": ELEMENTS["frost"][0]})
    return img


def main():
    g.save(stroke(), "textures/entity/glyph/stroke.png")
    g.save(glow(), "textures/entity/glyph/glow.png")
    g.save(ripple(), "textures/entity/glyph/ripple.png")
    g.save(swirl(), "textures/entity/glyph/swirl.png")
    g.save(wave(), "textures/entity/glyph/wave.png")
    for name, icon in (("fire_glyph", fire_glyph_icon), ("frost_glyph", frost_glyph_icon), ("storm_glyph", storm_glyph_icon),
                       ("tide_glyph", tide_glyph_icon), ("gale_glyph", gale_glyph_icon), ("stone_glyph", stone_glyph_icon),
                       ("elemental_seal", elemental_seal_icon), ("elemental_script", elemental_script_icon),
                       ("glyph_resonance", glyph_resonance_icon), ("primordial_glyph", primordial_glyph_icon)):
        g.save(icon(), f"textures/gui/spell_icons/{name}.png")
    for element in ORDER:
        g.save(mark_effect_icon(element), f"textures/mob_effect/glyph_{element}.png")
    g.save(glyph_frozen_effect_icon(), "textures/mob_effect/glyph_frozen.png")
    g.save(affinity_ring_glyph(), "textures/item/affinity_ring_glyph.png")
    g.save(six_inks_codex_icon(), "textures/item/six_inks_codex.png")
    g.save(six_inks_codex_model_texture(), "textures/item/six_inks_codex_3d.png")
    g.write_json("models/item/affinity_ring_glyph.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/affinity_ring_glyph"}})
    g.book_models("six_inks_codex")


if __name__ == "__main__":
    main()
