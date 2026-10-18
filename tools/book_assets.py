#!/usr/bin/env python3
"""Book school art: spell icons, pages, ink hands, the colossal tome, runes, scroll and affinity ring.

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
