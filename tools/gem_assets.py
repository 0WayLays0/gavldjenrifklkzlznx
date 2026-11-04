#!/usr/bin/env python3
"""Gem Magic art: the textures of the cut gems drawn in game (a facet and a sparkle), the five-pointed gem sigil, the
spell icons, the Jeweller's Codex (icon and 3D book texture) and its affinity ring.

Called by gen_assets.py (run that one). All art is original. The gems themselves are meshes drawn in game; the facet
texture only gives each facet the sheen of polished stone (white, tinted in game).
"""
import math

from PIL import Image, ImageDraw, ImageFilter

import gen_assets as g

RUBY = (224, 24, 72)
RUBY_LIGHT = (255, 138, 158)
RUBY_DARK = (110, 6, 32)
LAPIS = (36, 80, 200)
LAPIS_DARK = (14, 30, 92)
GOLD = (242, 195, 60)
DIAMOND = (159, 234, 255)
DIAMOND_DARK = (70, 150, 190)
AMETHYST = (163, 91, 240)
AMETHYST_LIGHT = (226, 198, 255)
AMETHYST_DARK = (78, 36, 130)
EMERALD = (31, 203, 110)
EMERALD_LIGHT = (182, 255, 210)
EMERALD_DARK = (8, 92, 50)
WHITE = (255, 255, 255)
NIGHT = (10, 8, 20)
OUTLINE = (16, 12, 26)


def facet():
    """32x32 white facet (tinted in game): the triangle is mapped (0,0) (1,0) (0.5,1); brighter toward its edges, as
    light runs along the edges of a cut stone, with a faint streak across."""
    S = 32
    img = Image.new("RGBA", (S, S))
    px = img.load()
    for y in range(S):
        for x in range(S):
            u, v = (x + .5) / S, (y + .5) / S
            # Distance to the triangle's edges: v=0, and the two slanted ones
            e1 = v
            e2 = abs(2 * u - v - 0) / math.sqrt(5)
            e3 = abs(2 * (1 - u) - v) / math.sqrt(5)
            edge = min(e1, e2, e3)
            rim = max(0.0, 1 - edge * 7)
            streak = max(0.0, 1 - abs((u - .5) * 1.6 + (v - .45)) * 9) * .35
            a = int(min(255, 150 + 80 * rim + 60 * streak))
            c = int(min(255, 205 + 50 * max(rim, streak)))
            px[x, y] = (c, c, c, a)
    return img


def sparkle():
    """32x32 four-pointed star on black (drawn additive in game)."""
    S = 32
    img = Image.new("RGBA", (S, S), (0, 0, 0, 255))
    px = img.load()
    c = (S - 1) / 2
    for y in range(S):
        for x in range(S):
            dx, dy = abs(x - c) / c, abs(y - c) / c
            star = max(0.0, 1 - (dx * 9 + dy * .9)) + max(0.0, 1 - (dy * 9 + dx * .9))
            diag = max(0.0, 1 - (abs(dx - dy) * 8 + (dx + dy) * 1.6)) * .4
            core = max(0.0, 1 - math.hypot(dx, dy) * 3.2)
            v = min(1.0, star + diag + core)
            k = int(255 * v)
            px[x, y] = (k, k, k, 255)
    return img



def _icon(draw, cx=.5, cy=.5, inner=(60, 40, 80)):
    return g.painted(64, draw, (cx, cy, inner, NIGHT), 16)


def _c(c, a=255):
    return tuple(c) + (a,)


def gem_side(d, S, cx, cy, w, color, light, dark, h=None):
    """A cut gem seen from the side: a crown (trapezoid with a table) over a pointed pavilion, facets drawn."""
    h = h or w * 1.1
    top = cy - h * .35
    girdle = cy - h * .05
    tip = cy + h * .6
    tw = w * .55
    crown = [((cx - tw / 2) * S, top * S), ((cx + tw / 2) * S, top * S), ((cx + w / 2) * S, girdle * S), ((cx - w / 2) * S, girdle * S)]
    pav = [((cx - w / 2) * S, girdle * S), ((cx + w / 2) * S, girdle * S), (cx * S, tip * S)]
    d.polygon(pav, fill=_c(dark), outline=_c(OUTLINE))
    d.polygon(crown, fill=_c(color), outline=_c(OUTLINE))
    # Facets: the crown's slants, the pavilion's mains
    lw = max(1, S // 40)
    for k in (-1, 1):
        d.line(((cx + k * tw / 2) * S, top * S, (cx + k * w / 4) * S, girdle * S), fill=_c(light), width=lw)
        d.line(((cx + k * w / 4) * S, girdle * S, cx * S, tip * S), fill=_c(color), width=lw)
    d.line(((cx - tw / 2) * S, top * S, (cx + tw / 2) * S, top * S), fill=_c(light), width=lw * 2)
    d.polygon([((cx - tw / 2) * S, top * S), (cx * S, top * S), ((cx - w / 4) * S, girdle * S)], fill=_c(light, 200))


def sparkle_at(d, S, x, y, r, color=WHITE):
    lw = max(1, S // 48)
    d.line(((x - r) * S, y * S, (x + r) * S, y * S), fill=_c(color), width=lw)
    d.line((x * S, (y - r) * S, x * S, (y + r) * S), fill=_c(color), width=lw)
    d.ellipse(((x - r * .25) * S, (y - r * .25) * S, (x + r * .25) * S, (y + r * .25) * S), fill=_c(color))


def crystal(d, S, x, y, w, h, angle, color, light, dark):
    """A six-sided crystal point standing at (x, y), leaning by angle (radians, 0 = up)."""
    ca, sa = math.cos(angle), math.sin(angle)

    def P(u, v):
        return ((x + u * ca + v * sa) * S, (y + u * sa - v * ca) * S)
    body = [P(-w / 2, 0), P(w / 2, 0), P(w / 2, h * .72), P(0, h), P(-w / 2, h * .72)]
    d.polygon(body, fill=_c(color), outline=_c(OUTLINE))
    d.polygon([P(-w / 2, 0), P(-w * .1, 0), P(-w * .1, h * .8), P(0, h), P(-w / 2, h * .72)], fill=_c(light, 210))
    d.line([P(w * .15, 0), P(w * .15, h * .78)], fill=_c(dark), width=max(1, S // 48))


def ruby_ray_icon():
    def draw(d, img, S):
        d.line((.3 * S, .7 * S, .98 * S, .02 * S), fill=_c(RUBY), width=S // 6)
        d.line((.3 * S, .7 * S, .98 * S, .02 * S), fill=_c(RUBY_LIGHT), width=S // 12)
        d.line((.3 * S, .7 * S, .98 * S, .02 * S), fill=_c(WHITE), width=S // 32)
        gem_side(d, S, .28, .68, .36, RUBY, RUBY_LIGHT, RUBY_DARK)
        sparkle_at(d, S, .85, .15, .08)
    return _icon(draw, .3, .7, (90, 20, 30))


def ruby_heart_icon():
    def draw(d, img, S):
        for r, a in ((.42, 70), (.32, 120)):
            d.ellipse(((.5 - r) * S, (.52 - r) * S, (.5 + r) * S, (.52 + r) * S), outline=_c(RUBY_LIGHT, a), width=S // 24)
        gem_side(d, S, .5, .5, .5, RUBY, RUBY_LIGHT, RUBY_DARK)
        for x, y in ((.2, .25), (.8, .3), (.75, .8)):
            d.ellipse(((x - .03) * S, (y - .03) * S, (x + .03) * S, (y + .03) * S), fill=_c((255, 150, 60)))
    return _icon(draw, .5, .5, (110, 30, 20))


def lapis_seal_icon():
    def draw(d, img, S):
        for x in (.18, .82):
            d.rectangle(((x - .1) * S, .2 * S, (x + .1) * S, .82 * S), fill=_c(LAPIS), outline=_c(OUTLINE))
            for k in range(4):
                d.point(((x - .05 + .03 * k) * S, (.3 + .12 * k) * S), fill=_c(GOLD))
            d.line(((x - .04) * S, .45 * S, (x + .04) * S, .55 * S), fill=_c(GOLD), width=S // 24)
        for y in (.4, .6):
            d.line((.28 * S, y * S, .72 * S, (y + .02) * S), fill=_c(GOLD), width=S // 20)
        d.ellipse((.36 * S, .3 * S, .64 * S, .7 * S), outline=_c(GOLD), width=S // 16)
        d.line((.5 * S, .38 * S, .5 * S, .62 * S), fill=_c(GOLD), width=S // 20)
    return _icon(draw, .5, .5, (20, 30, 90))


def lapis_constellation_icon():
    stars = [(.2, .3), (.42, .18), (.66, .32), (.82, .55), (.6, .78), (.32, .72), (.5, .5)]

    def draw(d, img, S):
        order = [0, 1, 2, 3, 4, 5, 0]
        for a, b in zip(order, order[1:]):
            d.line((stars[a][0] * S, stars[a][1] * S, stars[b][0] * S, stars[b][1] * S), fill=_c(GOLD, 200), width=S // 32)
        d.line((stars[2][0] * S, stars[2][1] * S, stars[6][0] * S, stars[6][1] * S), fill=_c(GOLD, 200), width=S // 32)
        for i, (x, y) in enumerate(stars):
            r = .07 if i == 6 else .05
            d.rectangle(((x - r) * S, (y - r) * S, (x + r) * S, (y + r) * S), fill=_c(LAPIS), outline=_c(GOLD))
            sparkle_at(d, S, x, y, r * 1.6, GOLD)
    return _icon(draw, .5, .5, (16, 24, 70))


def diamond_prism_icon():
    def draw(d, img, S):
        for i, c in enumerate(((255, 70, 80), (255, 200, 60), (80, 240, 110), (80, 120, 255), (190, 90, 255))):
            a = -.6 + i * .3
            d.line((.5 * S, .45 * S, (.5 + math.sin(a) * .5) * S, (.45 + math.cos(a) * .55) * S), fill=_c(c), width=S // 22)
        gem_side(d, S, .5, .4, .62, DIAMOND, WHITE, DIAMOND_DARK)
        sparkle_at(d, S, .3, .2, .09)
        sparkle_at(d, S, .72, .3, .06)
    return _icon(draw, .5, .4, (40, 70, 100))


def diamond_shell_icon():
    def draw(d, img, S):
        d.ellipse((.4 * S, .25 * S, .6 * S, .45 * S), fill=_c((70, 70, 90)))
        d.rectangle((.38 * S, .45 * S, .62 * S, .78 * S), fill=_c((70, 70, 90)))
        for k in range(10):
            a = k * math.tau / 10
            x, y = .5 + math.cos(a) * .36, .52 + math.sin(a) * .36
            pts = [((x + math.cos(a + j * math.tau / 4 + .4) * .09) * S, (y + math.sin(a + j * math.tau / 4 + .4) * .09) * S) for j in range(4)]
            d.polygon(pts, fill=_c(DIAMOND), outline=_c(OUTLINE))
            d.line((pts[0], pts[2]), fill=_c(WHITE), width=max(1, S // 48))
        sparkle_at(d, S, .82, .2, .08)
    return _icon(draw, .5, .5, (40, 70, 100))


def amethyst_resonance_icon():
    def draw(d, img, S):
        for r, a in ((.46, 80), (.36, 130)):
            d.arc(((.5 - r) * S, (.7 - r) * S, (.5 + r) * S, (.7 + r) * S), 200, 340, fill=_c(AMETHYST_LIGHT, a), width=S // 24)
        d.rectangle((.2 * S, .78 * S, .8 * S, .9 * S), fill=_c(AMETHYST_DARK))
        for x, h, ang in ((.38, .34, -.4), (.5, .5, 0), (.63, .38, .35), (.28, .22, -.7), (.72, .24, .7)):
            crystal(d, S, x, .82, .1, h, ang, AMETHYST, AMETHYST_LIGHT, AMETHYST_DARK)
    return _icon(draw, .5, .6, (60, 30, 90))


def amethyst_geode_icon():
    def draw(d, img, S):
        d.ellipse((.08 * S, .1 * S, .92 * S, .94 * S), fill=_c((52, 50, 58)), outline=_c(OUTLINE))
        d.ellipse((.16 * S, .18 * S, .84 * S, .86 * S), fill=_c((226, 222, 210)))
        d.ellipse((.24 * S, .26 * S, .76 * S, .78 * S), fill=_c(AMETHYST_DARK))
        for k in range(9):
            a = k * math.tau / 9
            x, y = .5 + math.cos(a) * .24, .52 + math.sin(a) * .24
            crystal(d, S, x, y, .07, .17, a - math.pi / 2 + math.pi, AMETHYST, AMETHYST_LIGHT, AMETHYST_DARK)
        sparkle_at(d, S, .5, .52, .07, AMETHYST_LIGHT)
    return _icon(draw, .5, .5, (50, 30, 70))


def emerald_bloom_icon():
    def draw(d, img, S):
        for k in range(6):
            a = -math.pi / 2 + (k - 2.5) * .5
            x, y = .5 + math.cos(a) * .05, .78 + math.sin(a) * .05
            crystal(d, S, x, y, .12, .42, a + math.pi / 2, EMERALD, EMERALD_LIGHT, EMERALD_DARK)
        d.rectangle((.4 * S, .26 * S, .6 * S, .44 * S), fill=_c(EMERALD), outline=_c(OUTLINE))
        d.rectangle((.44 * S, .3 * S, .56 * S, .4 * S), fill=_c(EMERALD_LIGHT))
        for x, y in ((.2, .3), (.82, .25), (.7, .12)):
            d.ellipse(((x - .025) * S, (y - .025) * S, (x + .025) * S, (y + .025) * S), fill=_c(EMERALD_LIGHT))
    return _icon(draw, .5, .55, (20, 70, 40))


def crown_of_gems_icon():
    def draw(d, img, S):
        gold_dark = (170, 120, 20)
        d.polygon([(.12 * S, .78 * S), (.12 * S, .38 * S), (.3 * S, .55 * S), (.5 * S, .28 * S), (.7 * S, .55 * S), (.88 * S, .38 * S), (.88 * S, .78 * S)],
                  fill=_c(GOLD), outline=_c(OUTLINE))
        d.rectangle((.12 * S, .72 * S, .88 * S, .8 * S), fill=_c(gold_dark))
        for (x, y), (c, l, k) in zip(((.16, .42), (.3, .66), (.5, .34), (.7, .66), (.84, .42)),
                                     ((RUBY, RUBY_LIGHT, RUBY_DARK), (LAPIS, (120, 150, 255), LAPIS_DARK), (DIAMOND, WHITE, DIAMOND_DARK),
                                      (AMETHYST, AMETHYST_LIGHT, AMETHYST_DARK), (EMERALD, EMERALD_LIGHT, EMERALD_DARK))):
            d.ellipse(((x - .065) * S, (y - .065) * S, (x + .065) * S, (y + .065) * S), fill=_c(c), outline=_c(OUTLINE))
            d.ellipse(((x - .035) * S, (y - .04) * S, (x + .005) * S, (y - .005) * S), fill=_c(l))
        sparkle_at(d, S, .5, .14, .1)
    return _icon(draw, .5, .45, (80, 60, 30))


ICONS = (("ruby_ray", ruby_ray_icon), ("ruby_heart", ruby_heart_icon), ("lapis_seal", lapis_seal_icon),
         ("lapis_constellation", lapis_constellation_icon), ("diamond_prism", diamond_prism_icon), ("diamond_shell", diamond_shell_icon),
         ("amethyst_resonance", amethyst_resonance_icon), ("amethyst_geode", amethyst_geode_icon), ("emerald_bloom", emerald_bloom_icon),
         ("crown_of_gems", crown_of_gems_icon))


# ------------------------------------------------------------------------------------------- the newer spells' art

def lapis_texture():
    """64 x 64, tiling: polished lapis lazuli: deep ultramarine clouded darker and lighter, flecks of golden pyrite,
    a few pale veins of calcite."""
    import numpy as np
    from water_assets import _tnoise, _rgba
    S = 64
    n = _tnoise(S, S, (6, 6), 1601)
    fine = _tnoise(S, S, (24, 24), 1602)
    t = np.clip(.25 + .5 * n + .25 * fine, 0, 1)[..., None]
    col = np.array(LAPIS_DARK, np.float32) * (1 - t) + np.array((52, 104, 224), np.float32) * t
    vein = np.clip(1 - np.abs(_tnoise(S, S, (4, 4), 1603) - .5) / .025, 0, 1)
    col = col + (np.array((200, 214, 240), np.float32) - col) * (.45 * vein[..., None])
    rnd = np.random.default_rng(1604)
    flecks = rnd.random((S, S)) < .03
    col[flecks] = np.array(GOLD, np.float32) * (.8 + .4 * rnd.random((flecks.sum(), 1)))
    return _rgba(np.clip(col, 0, 255), np.full((S, S), 255.0))


def _star(d, S, x, y, r, fill, outline=None, inner=.44, turn=0.0):
    pts = []
    for k in range(10):
        a = -math.pi / 2 + turn + k * math.pi / 5
        rr = r if k % 2 == 0 else r * inner
        pts.append(((x + math.cos(a) * rr) * S, (y + math.sin(a) * rr) * S))
    d.polygon(pts, fill=_c(fill), outline=_c(outline) if outline else None)


def ruby_shards_icon():
    def draw(d, img, S):
        for k, (x, y) in enumerate(((.3, .78), (.5, .7), (.7, .62))):
            d.line((x * S - .2 * S, y * S + .2 * S, x * S, y * S), fill=_c((255, 140, 60), 180), width=S // 16)
            crystal(d, S, x, y, .1, .36, .75, RUBY, RUBY_LIGHT, RUBY_DARK)
        sparkle_at(d, S, .86, .2, .07)
    return _icon(draw, .5, .6, (90, 20, 30))


def ruby_nova_icon():
    def draw(d, img, S):
        for k in range(12):
            a = k * math.tau / 12
            d.line((.5 * S, .45 * S, (.5 + math.cos(a) * .48) * S, (.45 + math.sin(a) * .48) * S), fill=_c(RUBY_LIGHT, 220), width=S // 28)
        g.glow_spot(img, .5 * S, .45 * S, .3 * S, (255, 120, 120), .9)
        gem_side(d, S, .5, .42, .4, RUBY, RUBY_LIGHT, RUBY_DARK)
    return _icon(draw, .5, .45, (90, 20, 30))


def ruby_meteor_icon():
    def draw(d, img, S):
        d.line((.95 * S, .02 * S, .55 * S, .48 * S), fill=_c((255, 150, 60), 200), width=S // 6)
        d.line((.95 * S, .02 * S, .55 * S, .48 * S), fill=_c((255, 230, 160)), width=S // 14)
        d.ellipse((.1 * S, .76 * S, .9 * S, .96 * S), fill=_c((255, 110, 40), 220))
        gem_side(d, S, .48, .55, .44, RUBY, RUBY_LIGHT, RUBY_DARK)
        g.glow_spot(img, .5 * S, .86 * S, .3 * S, (255, 120, 40), .8)
    return _icon(draw, .5, .6, (70, 20, 20))


def shooting_star_icon():
    def draw(d, img, S):
        d.line((.08 * S, .92 * S, .6 * S, .4 * S), fill=_c(GOLD, 200), width=S // 10)
        d.line((.08 * S, .92 * S, .6 * S, .4 * S), fill=_c((255, 250, 220)), width=S // 26)
        _star(d, S, .64, .36, .28, LAPIS, GOLD, turn=.3)
        _star(d, S, .64, .36, .12, (90, 140, 255), None, turn=.3)
        sparkle_at(d, S, .2, .25, .07, GOLD)
    return _icon(draw, .5, .5, (16, 24, 70))


def celestial_vault_icon():
    def draw(d, img, S):
        d.ellipse((.06 * S, .06 * S, .94 * S, .5 * S), fill=_c(LAPIS_DARK), outline=_c(GOLD), width=S // 20)
        for x, y in ((.25, .26), (.42, .18), (.6, .3), (.75, .22)):
            sparkle_at(d, S, x, y, .05, GOLD)
        for x, y in ((.3, .62), (.62, .8)):
            d.line((x * S, (y - .2) * S, x * S, y * S), fill=_c(GOLD, 160), width=S // 30)
            _star(d, S, x, y, .09, GOLD, OUTLINE)
    return _icon(draw, .5, .3, (16, 24, 70))


def lapis_mirror_icon():
    def draw(d, img, S):
        d.rounded_rectangle((.26 * S, .1 * S, .74 * S, .9 * S), radius=int(.2 * S), fill=_c(GOLD), outline=_c(OUTLINE))
        d.rounded_rectangle((.32 * S, .16 * S, .68 * S, .84 * S), radius=int(.16 * S), fill=_c(LAPIS))
        for y in (.32, .5, .68):
            d.line((.42 * S, y * S, .58 * S, y * S), fill=_c(GOLD), width=S // 24)
            d.line((.5 * S, (y - .05) * S, .5 * S, (y + .05) * S), fill=_c(GOLD), width=S // 24)
        sparkle_at(d, S, .8, .2, .07)
    return _icon(draw, .5, .5, (16, 24, 70))


def diamond_lance_icon():
    def draw(d, img, S):
        for i, c in enumerate(((255, 70, 80), (255, 200, 60), (80, 240, 110), (80, 120, 255), (190, 90, 255))):
            d.line((.08 * S, (.84 + i * .025) * S, .55 * S, .45 * S), fill=_c(c, 200), width=S // 40)
        d.line((.08 * S, .9 * S, .62 * S, .38 * S), fill=_c(WHITE), width=S // 24)
        crystal(d, S, .66, .34, .12, .5, .8, DIAMOND, WHITE, DIAMOND_DARK)
        sparkle_at(d, S, .9, .1, .08)
    return _icon(draw, .5, .5, (40, 70, 100))


def diamond_dust_icon():
    def draw(d, img, S):
        import random
        rnd = random.Random(7)
        g.glow_spot(img, .5 * S, .55 * S, .4 * S, (220, 245, 255), .5)
        for k in range(16):
            x, y = .15 + rnd.random() * .7, .2 + rnd.random() * .65
            c = [(255, 120, 120), (255, 230, 120), (140, 255, 160), (140, 200, 255), (220, 160, 255), WHITE][k % 6]
            sparkle_at(d, S, x, y, .04 + rnd.random() * .05, c)
        gem_side(d, S, .5, .55, .22, DIAMOND, WHITE, DIAMOND_DARK)
    return _icon(draw, .5, .5, (40, 70, 100))


def diamond_bastion_icon():
    def draw(d, img, S):
        d.pieslice((.04 * S, .2 * S, .96 * S, 1.2 * S), 180, 360, fill=_c((180, 230, 255), 70), outline=_c(WHITE, 160))
        for x, h, a in ((.5, .6, 0), (.34, .38, -.4), (.66, .38, .4), (.22, .24, -.8), (.78, .24, .8)):
            crystal(d, S, x, .88, .14 if h > .5 else .1, h, a, DIAMOND, WHITE, DIAMOND_DARK)
        sparkle_at(d, S, .5, .2, .08)
    return _icon(draw, .5, .7, (40, 70, 100))


def amethyst_spikes_icon():
    def draw(d, img, S):
        d.rectangle((0, .82 * S, S, S), fill=_c(AMETHYST_DARK))
        for k in range(5):
            x = .12 + k * .19
            crystal(d, S, x, .86, .08 + k * .015, .14 + k * .1, .15 * (k - 2), AMETHYST, AMETHYST_LIGHT, AMETHYST_DARK)
    return _icon(draw, .5, .7, (60, 30, 90))


def budding_amethyst_icon():
    def draw(d, img, S):
        d.rectangle((.24 * S, .62 * S, .76 * S, .92 * S), fill=_c((120, 80, 170)), outline=_c(OUTLINE))
        for x, h, a in ((.5, .5, 0), (.34, .3, -.5), (.66, .32, .5)):
            crystal(d, S, x, .64, .12, h, a, AMETHYST, AMETHYST_LIGHT, AMETHYST_DARK)
        for r in (.34, .44):
            d.arc(((.5 - r) * S, (.55 - r) * S, (.5 + r) * S, (.55 + r) * S), 200, 340, fill=_c(AMETHYST_LIGHT, 140), width=S // 30)
    return _icon(draw, .5, .6, (60, 30, 90))


def amethyst_chimes_icon():
    def draw(d, img, S):
        d.line((.12 * S, .14 * S, .88 * S, .14 * S), fill=_c(GOLD), width=S // 16)
        for k, (x, ln) in enumerate(((.2, .5), (.36, .66), (.52, .58), (.68, .72), (.84, .46))):
            d.line((x * S, .14 * S, x * S, (.14 + ln * .3) * S), fill=_c((200, 200, 220)), width=max(1, S // 48))
            crystal(d, S, x, .14 + ln * .3, .08, ln * .5, math.pi, AMETHYST, AMETHYST_LIGHT, AMETHYST_DARK)
        for r in (.4, .48):
            d.arc(((.5 - r) * S, (.5 - r) * S, (.5 + r) * S, (.5 + r) * S), 20, 160, fill=_c(AMETHYST_LIGHT, 120), width=S // 32)
    return _icon(draw, .5, .4, (60, 30, 90))


def emerald_lifeline_icon():
    def draw(d, img, S):
        d.line((.18 * S, .72 * S, .82 * S, .3 * S), fill=_c(EMERALD, 200), width=S // 8)
        d.line((.18 * S, .72 * S, .82 * S, .3 * S), fill=_c(EMERALD_LIGHT), width=S // 20)
        for x, y in ((.36, .6), (.62, .43)):
            d.line(((x - .06) * S, y * S, (x + .06) * S, y * S), fill=_c(WHITE), width=S // 24)
            d.line((x * S, (y - .06) * S, x * S, (y + .06) * S), fill=_c(WHITE), width=S // 24)
        gem_side(d, S, .18, .72, .26, EMERALD, EMERALD_LIGHT, EMERALD_DARK)
        g.glow_spot(img, .82 * S, .3 * S, .14 * S, (180, 255, 210), .9)
    return _icon(draw, .5, .5, (20, 70, 40))


def emerald_aegis_icon():
    def draw(d, img, S):
        d.polygon([(.5 * S, .08 * S), (.86 * S, .22 * S), (.8 * S, .62 * S), (.5 * S, .92 * S), (.2 * S, .62 * S), (.14 * S, .22 * S)], fill=_c(EMERALD),
                  outline=_c(OUTLINE))
        d.polygon([(.5 * S, .18 * S), (.74 * S, .28 * S), (.7 * S, .58 * S), (.5 * S, .8 * S)], fill=_c(EMERALD_LIGHT, 200))
        d.line((.5 * S, .18 * S, .5 * S, .8 * S), fill=_c(EMERALD_DARK), width=S // 32)
        sparkle_at(d, S, .3, .3, .07)
    return _icon(draw, .5, .5, (20, 70, 40))


def emerald_tree_icon():
    def draw(d, img, S):
        d.line((.5 * S, .92 * S, .5 * S, .5 * S), fill=_c((120, 90, 60)), width=S // 10)
        for x, y, a in ((.5, .48, 0), (.32, .56, -.7), (.68, .56, .7), (.4, .36, -.35), (.6, .36, .35)):
            crystal(d, S, x, y, .1, .28, a, EMERALD, EMERALD_LIGHT, EMERALD_DARK)
        for x, y in ((.24, .72), (.78, .76)):
            d.ellipse(((x - .05) * S, (y - .05) * S, (x + .05) * S, (y + .05) * S), fill=_c(EMERALD_LIGHT), outline=_c(OUTLINE))
    return _icon(draw, .5, .5, (20, 70, 40))


def emerald_thorns_icon():
    def draw(d, img, S):
        d.rectangle((0, .84 * S, S, S), fill=_c((40, 50, 40)))
        for x, h, a in ((.2, .44, -.25), (.38, .6, -.1), (.56, .7, .08), (.76, .5, .25)):
            crystal(d, S, x, .88, .09, h, a, EMERALD, EMERALD_LIGHT, EMERALD_DARK)
        d.line((.7 * S, .2 * S, .9 * S, .08 * S), fill=_c((255, 120, 140)), width=S // 20)
        g.glow_spot(img, .86 * S, .12 * S, .12 * S, (180, 255, 210), .9)
    return _icon(draw, .5, .6, (20, 60, 40))


NEW_ICONS = (("ruby_shards", ruby_shards_icon), ("ruby_nova", ruby_nova_icon), ("ruby_meteor", ruby_meteor_icon),
             ("shooting_star", shooting_star_icon), ("celestial_vault", celestial_vault_icon), ("lapis_mirror", lapis_mirror_icon),
             ("diamond_lance", diamond_lance_icon), ("diamond_dust", diamond_dust_icon), ("diamond_bastion", diamond_bastion_icon),
             ("amethyst_spikes", amethyst_spikes_icon), ("budding_amethyst", budding_amethyst_icon), ("amethyst_chimes", amethyst_chimes_icon),
             ("emerald_lifeline", emerald_lifeline_icon), ("emerald_aegis", emerald_aegis_icon), ("emerald_tree", emerald_tree_icon),
             ("emerald_thorns", emerald_thorns_icon))


def jeweller_sigil():
    """256x256 white on black (tinted in game, drawn glowing on the ground): the plan of a brilliant cut seen from above
    (an octagonal table, star and kite facets, a sixteen-sided girdle) in two rings, five small gems set round it."""
    S = 256
    B = S * 4
    img = Image.new("L", (B, B), 0)
    d = ImageDraw.Draw(img)
    c = B / 2

    def ring(n, r, off=0.0):
        return [(c + math.cos(off + k * math.tau / n) * r, c + math.sin(off + k * math.tau / n) * r) for k in range(n)]
    d.ellipse((c - B * .49, c - B * .49, c + B * .49, c + B * .49), outline=255, width=14)
    d.ellipse((c - B * .44, c - B * .44, c + B * .44, c + B * .44), outline=170, width=6)
    # Ticks between the rings
    for k in range(80):
        a = k * math.tau / 80
        r0, r1 = B * .445, B * (.47 if k % 4 == 0 else .46)
        d.line((c + math.cos(a) * r0, c + math.sin(a) * r0, c + math.cos(a) * r1, c + math.sin(a) * r1), fill=200, width=5)
    girdle = ring(16, B * .38)
    table = ring(8, B * .2, math.pi / 8)
    d.polygon(girdle, outline=255, width=10)
    d.polygon(table, outline=255, width=10)
    for k in range(8):
        t0, t1 = table[k], table[(k + 1) % 8]
        g0, g1, g2 = girdle[2 * k], girdle[2 * k + 1], girdle[(2 * k + 2) % 16]
        # Star facet and kites
        mid = ((t0[0] + t1[0]) / 2, (t0[1] + t1[1]) / 2)
        d.line((t0, g1), fill=210, width=7)
        d.line((t1, g1), fill=210, width=7)
        d.line((t0, g0), fill=150, width=5)
        d.line((mid, g1), fill=110, width=4)
    # Lines from the table's corners to its middle: the culet seen through it
    for t in table:
        d.line((t, (c, c)), fill=90, width=4)
    # Five gems set round it
    for k in range(5):
        a = -math.pi / 2 + k * math.tau / 5
        x, y = c + math.cos(a) * B * .415, c + math.sin(a) * B * .415
        r = B * .035
        d.polygon([(x, y - r * 1.4), (x + r, y), (x, y + r * 1.4), (x - r, y)], fill=255)
    light = img.resize((S, S), Image.LANCZOS)
    return Image.merge("RGBA", (light, light, light, Image.new("L", (S, S), 255)))


def jewellers_codex_icon():
    img = g.from_grid([
        "................",
        "...oooooooooo...",
        "..obbbbbbbbbmco.",
        "..obgggggggbmco.",
        "..obgRgggDgbLco.",
        "..obgggggggbLcco",
        "..obggggAgggLcco",
        "..obgggggggbLcco",
        "..obgLggggEbLcco",
        "..obgggggggbLcco",
        "..obbbbbbbbbmco.",
        "..obddddddddmco.",
        "..odddddddddxco.",
        "...ooooooooooo..",
        "................",
        "................",
    ], {"o": OUTLINE, "b": (40, 28, 52), "g": (70, 50, 90), "m": (110, 84, 30), "L": GOLD, "d": (30, 20, 40),
        "c": g.CREAM, "x": g.CREAM_SHADE, "R": RUBY, "D": DIAMOND, "A": AMETHYST, "E": EMERALD})
    img.putpixel((4, 8), LAPIS + (255,))
    return img


def jewellers_codex_model_texture():
    """64x64 texture of the 3D book, laid out like the other books: a cover of deep violet leather with a gold border,
    five cut gems set in it in a ring, a gold-bound spine, gilt-edged pages."""
    s = 4
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    def box(u0, v0, u1, v1, fill):
        d.rectangle((u0 * s, v0 * s, u1 * s - 1, v1 * s - 1), fill=fill + (255,))
    W, H = 12 * s, 9 * s
    from explosion_assets import fbm
    for y in range(H):
        for x in range(W):
            n = fbm(x / 5, y / 5, 3, 61)
            img.putpixel((x, y), g.lerp((38, 24, 54), (70, 46, 92), n) + (255,))
    d.rectangle((1, 1, W - 2, H - 2), outline=GOLD + (255,))
    d.rectangle((3, 3, W - 4, H - 4), outline=(150, 110, 30, 255))
    cx, cy = W // 2, H // 2
    for k, col in enumerate((RUBY, LAPIS, DIAMOND, AMETHYST, EMERALD)):
        a = -math.pi / 2 + k * math.tau / 5
        x, y = cx + math.cos(a) * 11, cy + math.sin(a) * 10
        d.ellipse((x - 3, y - 3, x + 3, y + 3), fill=GOLD + (255,))
        d.ellipse((x - 2, y - 2, x + 2, y + 2), fill=col + (255,))
        img.putpixel((int(x) - 1, int(y) - 1), (255, 255, 255, 255))
    d.polygon([(cx, cy - 4), (cx + 3, cy), (cx, cy + 4), (cx - 3, cy)], fill=DIAMOND + (255,), outline=GOLD + (255,))
    box(12, 0, 13, 9, (110, 84, 30))
    for y in range(0, 8 * s):
        for x in range(13 * s, 16 * s):
            img.putpixel((x, y), g.lerp(g.CREAM, (176, 168, 140), fbm(x / 3, y / 3, 2, 23) * .8) + (255,))
    for y in range(1, 8 * s, 2):
        d.line((13 * s, y, 16 * s - 1, y), fill=g.CREAM_SHADE + (255,))
    for x in range(0, 12 * s):
        for y in range(9 * s, 12 * s):
            img.putpixel((x, y), g.lerp((52, 34, 70), (80, 56, 100), fbm(x / 4, y / 4, 2, 41)) + (255,))
    for y in (9 * s + 2, 12 * s - 3):
        d.line((0, y, 12 * s - 1, y), fill=GOLD + (255,))
    box(0, 12, 12, 15, g.CREAM)
    for x in range(0, 12 * s, 2):
        d.line((x, 12 * s, x, 15 * s - 1), fill=g.CREAM_SHADE + (255,))
    box(12, 9, 14, 11, GOLD)
    for y in range(9 * s, 11 * s):
        for x in range(14 * s, 16 * s):
            t = math.hypot(x - 14.8 * s, y - 9.8 * s) / (1.4 * s)
            img.putpixel((x, y), g.lerp(WHITE, DIAMOND, min(1, t ** .8)) + (255,))
    box(12, 11, 16, 12, (110, 84, 30))
    return img


def affinity_ring_gem():
    return g.from_grid([
        "................",
        "......oooo......",
        ".....oRDDEo.....",
        ".....oALDRo.....",
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
    ], {"o": OUTLINE, "h": (255, 236, 150), "l": GOLD, "b": (190, 140, 30), "s": (140, 100, 20),
        "R": RUBY, "D": DIAMOND, "E": EMERALD, "A": AMETHYST, "L": LAPIS})


def main():
    g.save(facet(), "textures/entity/gem/facet.png")
    g.save(sparkle(), "textures/entity/gem/sparkle.png")
    g.save(jeweller_sigil(), "textures/entity/gem/sigil.png")
    for name, icon in ICONS + NEW_ICONS:
        g.save(icon(), f"textures/gui/spell_icons/{name}.png")
    g.save(lapis_texture(), "textures/entity/gem/lapis.png")
    g.save(affinity_ring_gem(), "textures/item/affinity_ring_gem.png")
    g.save(jewellers_codex_icon(), "textures/item/jewellers_codex.png")
    g.save(jewellers_codex_model_texture(), "textures/item/jewellers_codex_3d.png")
    g.write_json("models/item/affinity_ring_gem.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/affinity_ring_gem"}})
    g.book_models("jewellers_codex")


if __name__ == "__main__":
    main()
