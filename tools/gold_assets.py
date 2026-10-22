#!/usr/bin/env python3
"""Gold Magic art for the reworked school: the textures drawn in game (the white metal the gold solids are tinted
from, a coin's stamped face, the gold skin and the sheen laid on gilded bodies) and the Sun Stone of Eldorado sigil.

Called by gen_assets.py (run that one). All art is original.
"""
import math

from PIL import Image, ImageDraw

import gen_assets as g
from explosion_assets import fbm

GOLD = (255, 201, 60)
PALE = (255, 240, 160)
DEEP = (200, 132, 20)
DARK = (120, 70, 8)


def metal():
    """16x16 near-white metal with a faint brushed grain (tinted gold in game)."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            v = 222 + int(28 * fbm(x / 6, y / 2, 2, 71)) + (8 if (x + y) % 7 == 0 else 0)
            v = min(255, v)
            img.putpixel((x, y), (v, v, v, 255))
    return img


def coin():
    """64x64 coin face: a milled rim, a raised ring, and the sun of Eldorado stamped in the middle; clear outside."""
    S = 64
    B = S * 4
    img = Image.new("RGBA", (B, B), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = B / 2
    d.ellipse((4, 4, B - 4, B - 4), fill=DEEP + (255,))
    d.ellipse((18, 18, B - 18, B - 18), fill=GOLD + (255,))
    for k in range(48):
        a = k * math.tau / 48
        d.line((c + math.cos(a) * (c - 6), c + math.sin(a) * (c - 6), c + math.cos(a) * (c - 16), c + math.sin(a) * (c - 16)), fill=DARK + (255,), width=3)
    d.ellipse((34, 34, B - 34, B - 34), outline=PALE + (255,), width=5)
    # The sun: rays and a face
    for k in range(12):
        a = k * math.tau / 12
        p = [(c + math.cos(a) * 80, c + math.sin(a) * 80), (c + math.cos(a + .18) * 46, c + math.sin(a + .18) * 46),
             (c + math.cos(a - .18) * 46, c + math.sin(a - .18) * 46)]
        d.polygon(p, fill=PALE + (255,), outline=DEEP + (255,))
    d.ellipse((c - 44, c - 44, c + 44, c + 44), fill=PALE + (255,), outline=DEEP + (255,), width=4)
    # A solemn face: almond eyes, a straight nose, a closed mouth
    for ex in (-17, 17):
        d.polygon([(c + ex - 11, c - 9), (c + ex, c - 15), (c + ex + 11, c - 9), (c + ex, c - 4)], fill=DEEP + (255,))
    d.line((c, c - 6, c, c + 8), fill=DEEP + (255,), width=4)
    d.line((c - 14, c + 18, c + 14, c + 18), fill=DEEP + (255,), width=5)
    return img.resize((S, S), Image.LANCZOS)


def gold_skin():
    """64x64 polished gold laid on gilded bodies: warm gold with soft noise, brighter streaks, darker hollows."""
    S = 64
    img = Image.new("RGBA", (S, S))
    for y in range(S):
        for x in range(S):
            n = fbm(x / 9, y / 9, 3, 81)
            streak = max(0.0, 1 - abs(((x + y * .6) % 22) - 11) / 3) * .35
            t = min(1.0, .25 + .7 * n + streak)
            c = g.lerp(DEEP, PALE, t) if t > .5 else g.lerp(DARK, GOLD, t * 2)
            img.putpixel((x, y), c + (255,))
    return img


def gold_sheen():
    """64x64 light that slides over gilded bodies (drawn additive and scrolling): thin bright diagonal streaks on black."""
    S = 64
    img = Image.new("RGBA", (S, S), (0, 0, 0, 255))
    for y in range(S):
        for x in range(S):
            u = (x + y) % 32
            v = max(0.0, 1 - abs(u - 8) / 2.5) + max(0.0, 1 - abs(u - 20) / 1.2) * .6
            v *= .6 + .4 * fbm(x / 12, y / 12, 2, 91)
            k = int(255 * min(1.0, v))
            img.putpixel((x, y), (k, int(k * .85), int(k * .5), 255))
    return img


def sun_sigil():
    """256x256 white on black (tinted gold in game, glowing on the ground): the Sun Stone of Eldorado. Rings of carved
    glyph blocks, sixteen great rays and sixteen small ones between them, and the face of the sun in the middle."""
    S = 256
    B = S * 4
    img = Image.new("L", (B, B), 0)
    d = ImageDraw.Draw(img)
    c = B / 2

    def circle(r, **kw):
        d.ellipse((c - r, c - r, c + r, c + r), **kw)

    circle(B * .495, outline=255, width=14)
    circle(B * .455, outline=200, width=6)
    # The outer band of glyph blocks
    for k in range(40):
        a0, a1 = k * math.tau / 40 + .012, (k + 1) * math.tau / 40 - .012
        r0, r1 = B * .41, B * .45
        pts = [(c + math.cos(a) * r, c + math.sin(a) * r) for a, r in ((a0, r0), (a1, r0), (a1, r1), (a0, r1))]
        d.polygon(pts, outline=180, width=4)
        am = (a0 + a1) / 2
        if k % 2:
            d.ellipse((c + math.cos(am) * B * .43 - 8, c + math.sin(am) * B * .43 - 8, c + math.cos(am) * B * .43 + 8, c + math.sin(am) * B * .43 + 8), fill=220)
        else:
            d.line((c + math.cos(am) * r0, c + math.sin(am) * r0, c + math.cos(am) * r1, c + math.sin(am) * r1), fill=220, width=5)
    circle(B * .405, outline=255, width=8)
    # Rays: sixteen great, sixteen small
    for k in range(32):
        a = k * math.tau / 32
        big = k % 2 == 0
        tip = B * (.4 if big else .33)
        base = B * .22
        w = .1 if big else .06
        pts = [(c + math.cos(a) * tip, c + math.sin(a) * tip), (c + math.cos(a + w) * base, c + math.sin(a + w) * base),
               (c + math.cos(a - w) * base, c + math.sin(a - w) * base)]
        d.polygon(pts, outline=255 if big else 170, width=7 if big else 5)
        if big:
            d.line((c + math.cos(a) * base, c + math.sin(a) * base, c + math.cos(a) * tip * .9, c + math.sin(a) * tip * .9), fill=140, width=4)
    circle(B * .22, outline=255, width=10)
    circle(B * .19, outline=150, width=5)
    # The face of the sun
    for ex in (-.07, .07):
        d.ellipse((c + B * ex - 22, c - B * .06 - 14, c + B * ex + 22, c - B * .06 + 14), outline=255, width=7)
        d.ellipse((c + B * ex - 7, c - B * .06 - 7, c + B * ex + 7, c - B * .06 + 7), fill=255)
    d.line((c, c - B * .03, c - 10, c + B * .03), fill=220, width=6)
    d.line((c - 10, c + B * .03, c + 10, c + B * .03), fill=220, width=6)
    d.line((c - B * .06, c + B * .075, c + B * .06, c + B * .075), fill=255, width=8)
    for k in range(-2, 3):
        d.line((c + k * B * .022, c + B * .075, c + k * B * .022, c + B * .095), fill=200, width=5)
    for k in range(4):
        a = math.pi / 4 + k * math.tau / 4
        d.rectangle((c + math.cos(a) * B * .15 - 12, c + math.sin(a) * B * .15 - 12, c + math.cos(a) * B * .15 + 12, c + math.sin(a) * B * .15 + 12),
                    outline=200, width=5)
    light = img.resize((S, S), Image.LANCZOS)
    return Image.merge("RGBA", (light, light, light, Image.new("L", (S, S), 255)))


def main():
    g.save(metal(), "textures/entity/gold/metal.png")
    g.save(coin(), "textures/entity/gold/coin.png")
    g.save(gold_skin(), "textures/entity/gold/gold_skin.png")
    g.save(gold_sheen(), "textures/entity/gold/gold_sheen.png")
    g.save(sun_sigil(), "textures/entity/gold/sun_sigil.png")


if __name__ == "__main__":
    main()
