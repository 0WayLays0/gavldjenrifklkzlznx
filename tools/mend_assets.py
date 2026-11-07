#!/usr/bin/env python3
"""Mending Magic art: the spell icons, the Codex of Mending (ivory leather, a green-gold tree of life on its covers, a
heart of emerald) and its affinity ring.

Called by gen_assets.py (run that one). Original art, drawn here.
"""
import math

import codex_kit as ck
import gen_assets as g

MINT = (124, 255, 176)
EMERALD = (40, 190, 110)
GOLD = (255, 214, 120)
GOLD_LIGHT = (255, 244, 200)
GOLD_DARK = (150, 106, 40)
WHITE = (255, 255, 255)
ROSE = (255, 120, 150)
NIGHT = (8, 26, 22)
BG = (24, 84, 64)


def _icon(draw):
    return ck.icon(draw, BG, NIGHT)


def _cross(d, S, x, y, r, c):
    w = r * .38
    d.rectangle(((x - w) * S, (y - r) * S, (x + w) * S, (y + r) * S), fill=ck.a(c))
    d.rectangle(((x - r) * S, (y - w) * S, (x + r) * S, (y + w) * S), fill=ck.a(c))


def _heart(d, S, x, y, r, c):
    pts = []
    for i in range(40):
        t = i / 40 * math.tau
        px = 16 * math.sin(t) ** 3
        py = -(13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t))
        pts.append(((x + px / 17 * r) * S, (y + py / 17 * r) * S))
    d.polygon(pts, fill=ck.a(c))


def lifethread_icon():
    def draw(d, img, S):
        pts = [((.15 + .7 * t) * S, (.75 - .5 * t + .08 * math.sin(t * 9)) * S) for t in [i / 30 for i in range(31)]]
        d.line(pts, fill=ck.a(GOLD_LIGHT), width=3)
        g.glow_spot(img, .15 * S, .75 * S, .14 * S, MINT, .8)
        g.glow_spot(img, .85 * S, .25 * S, .18 * S, GOLD, .9)
        _cross(d, S, .85, .25, .1, WHITE)
    return _icon(draw)


def suture_icon():
    def draw(d, img, S):
        d.line((.2 * S, .8 * S, .8 * S, .2 * S), fill=ck.a(ROSE), width=4)
        for k in range(5):
            t = .2 + k * .15
            x, y = .2 + .6 * t, .8 - .6 * t
            d.line(((x - .08) * S, (y - .08) * S, (x + .08) * S, (y + .08) * S), fill=ck.a(MINT), width=3)
        g.glow_spot(img, .5 * S, .5 * S, .3 * S, MINT, .5)
    return _icon(draw)


def echo_heal_icon():
    def draw(d, img, S):
        for k, r in enumerate((.12, .22, .32)):
            d.ellipse(((.5 - r) * S, (.5 - r) * S, (.5 + r) * S, (.5 + r) * S), outline=ck.a(MINT, 255 - k * 70), width=3)
        _cross(d, S, .5, .5, .1, WHITE)
    return _icon(draw)


def purge_icon():
    def draw(d, img, S):
        for k in range(6):
            a = k * math.tau / 6
            x, y = .5 + math.cos(a) * .3, .5 + math.sin(a) * .3
            d.ellipse(((x - .05) * S, (y - .05) * S, (x + .05) * S, (y + .05) * S), fill=ck.a((90, 40, 110)))
        g.glow_spot(img, .5 * S, .5 * S, .3 * S, WHITE, .9)
        _cross(d, S, .5, .5, .12, MINT)
    return _icon(draw)


def mending_bud_icon():
    def draw(d, img, S):
        d.line((.5 * S, .9 * S, .5 * S, .5 * S), fill=ck.a(EMERALD), width=4)
        d.ellipse((.28 * S, .58 * S, .5 * S, .7 * S), fill=ck.a(EMERALD))
        d.polygon([(.5 * S, .18 * S), (.68 * S, .42 * S), (.5 * S, .56 * S), (.32 * S, .42 * S)], fill=ck.a(ROSE))
        g.glow_spot(img, .5 * S, .38 * S, .2 * S, GOLD, .7)
    return _icon(draw)


def guardian_wisp_icon():
    def draw(d, img, S):
        d.arc((.15 * S, .15 * S, .85 * S, .85 * S), 200, 520, fill=ck.a(MINT), width=2)
        g.glow_spot(img, .78 * S, .3 * S, .18 * S, MINT, 1)
        g.glow_spot(img, .78 * S, .3 * S, .08 * S, WHITE, 1)
        _heart(d, S, .45, .55, .2, ROSE)
    return _icon(draw)


def blood_pact_icon():
    def draw(d, img, S):
        _heart(d, S, .32, .5, .16, (180, 30, 50))
        _heart(d, S, .7, .5, .2, ROSE)
        d.line((.42 * S, .5 * S, .58 * S, .5 * S), fill=ck.a(GOLD_LIGHT), width=3)
        d.polygon([(.6 * S, .5 * S), (.54 * S, .44 * S), (.54 * S, .56 * S)], fill=ck.a(GOLD_LIGHT))
    return _icon(draw)


def rewind_icon():
    def draw(d, img, S):
        d.arc((.18 * S, .18 * S, .82 * S, .82 * S), 40, 330, fill=ck.a(GOLD), width=4)
        d.polygon([(.78 * S, .2 * S), (.86 * S, .4 * S), (.66 * S, .36 * S)], fill=ck.a(GOLD))
        d.line((.5 * S, .5 * S, .5 * S, .3 * S), fill=ck.a(GOLD_LIGHT), width=3)
        d.line((.5 * S, .5 * S, .36 * S, .58 * S), fill=ck.a(GOLD_LIGHT), width=3)
        g.glow_spot(img, .5 * S, .5 * S, .16 * S, MINT, .6)
    return _icon(draw)


def cocoon_icon():
    def draw(d, img, S):
        for k in range(7):
            y = .22 + k * .09
            w = .2 * math.sin(math.pi * (k + .5) / 7) + .04
            d.arc(((.5 - w) * S, (y - .05) * S, (.5 + w) * S, (y + .07) * S), 0, 180, fill=ck.a(GOLD_LIGHT if k % 2 else MINT), width=3)
        g.glow_spot(img, .5 * S, .52 * S, .26 * S, MINT, .6)
    return _icon(draw)


def second_life_icon():
    def draw(d, img, S):
        g.glow_spot(img, .5 * S, .45 * S, .4 * S, GOLD, .9)
        for k in range(2):
            x = .32 + k * .36
            d.polygon([(x * S, .5 * S), ((x + (.22 if k == 0 else -.22)) * S, .24 * S), ((x + (.18 if k == 0 else -.18)) * S, .6 * S)],
                      fill=ck.a(WHITE, 220))
        _heart(d, S, .5, .55, .16, ROSE)
        _cross(d, S, .5, .2, .06, WHITE)
    return _icon(draw)


ICONS = (("lifethread", lifethread_icon), ("suture", suture_icon), ("echo_heal", echo_heal_icon), ("purge", purge_icon),
         ("mending_bud", mending_bud_icon), ("guardian_wisp", guardian_wisp_icon), ("blood_pact", blood_pact_icon), ("rewind", rewind_icon),
         ("cocoon", cocoon_icon), ("second_life", second_life_icon))


def _cover(d, img, W, H):
    # A tree of life: a trunk of gold, branches spreading, leaves of green, an emerald heart at its middle
    cx, base = W / 2, H - 9
    d.line((cx, base, cx, H / 2), fill=GOLD + (255,), width=3)
    for k in range(-3, 4):
        if k == 0:
            continue
        x1 = cx + k * 11
        y1 = 10 + abs(k) * 3
        d.line((cx, H / 2 + 4 - abs(k) * 2, x1, y1), fill=GOLD_DARK + (255,) if abs(k) == 3 else GOLD + (255,), width=2)
        for j in range(3):
            lx, ly = x1 + (j - 1) * 4, y1 + (j % 2) * 3 - 2
            d.ellipse((lx - 2, ly - 1.5, lx + 2, ly + 1.5), fill=(MINT if j == 1 else EMERALD) + (255,))
    for k in range(-2, 3):
        d.line((cx, base, cx + k * 7, base + 4), fill=GOLD_DARK + (255,))
    d.polygon([(cx, H / 2 - 6), (cx + 5, H / 2), (cx, H / 2 + 6), (cx - 5, H / 2)], fill=EMERALD + (255,))
    d.polygon([(cx, H / 2 - 3), (cx + 2, H / 2), (cx, H / 2 + 3), (cx - 2, H / 2)], fill=MINT + (255,))


def main():
    for name, fn in ICONS:
        g.save(fn(), f"textures/gui/spell_icons/{name}.png")
    ivory = [(150, 132, 104), (206, 192, 160), (232, 222, 196), (246, 240, 222)]
    tex = ck.codex_texture(ivory, GOLD, GOLD_DARK, EMERALD, _cover, 3101)
    ck.write_book("codex_of_mending", ck.codex_icon((110, 96, 70), (232, 222, 196), (206, 192, 160), GOLD, GOLD_DARK, EMERALD), tex)
    ck.write_ring("mend", ck.ring((70, 56, 30), GOLD, GOLD_LIGHT, GOLD_DARK, EMERALD, MINT))


if __name__ == "__main__":
    main()
