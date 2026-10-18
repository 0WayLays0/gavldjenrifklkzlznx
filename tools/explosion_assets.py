#!/usr/bin/env python3
"""Explosion school art: spell and effect icons, blast textures, scroll, affinity ring and the Detonation Codex.

Called by gen_assets.py (run that one). All art is original. Additive textures (drawn with RenderType.eyes) are
greyscale on black: black is invisible, and the renderers tint them per blast style.
"""
import math
import random

from PIL import Image, ImageDraw, ImageFilter

import gen_assets as g

# Explosion palette
CHAR = (22, 16, 20)
CHAR_LIGHT = (52, 40, 44)
IRON = (74, 66, 72)
IRON_LIGHT = (120, 110, 116)
EMBER = (196, 52, 14)
FIRE = (255, 120, 30)
FLAME = (255, 186, 70)
CORE = (255, 246, 214)
ARCANE = (186, 76, 255)
ARCANE_DARK = (88, 26, 140)
NIGHT = (14, 8, 20)


# ---------------------------------------------------------------- noise

def _hash(ix, iy, seed):
    h = (ix * 374761393 + iy * 668265263 + seed * 2147483647) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFF) / 65535.0


def value_noise(x, y, seed=0):
    ix, iy = math.floor(x), math.floor(y)
    fx, fy = x - ix, y - iy
    sx, sy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    a = _hash(ix, iy, seed)
    b = _hash(ix + 1, iy, seed)
    c = _hash(ix, iy + 1, seed)
    d = _hash(ix + 1, iy + 1, seed)
    return (a + (b - a) * sx) + ((c + (d - c) * sx) - (a + (b - a) * sx)) * sy


def fbm(x, y, octaves=4, seed=0):
    total, amp, norm = 0.0, 1.0, 0.0
    for o in range(octaves):
        total += value_noise(x, y, seed + o * 31) * amp
        norm += amp
        x, y, amp = x * 2.03, y * 2.03, amp * .5
    return total / norm


def smoothstep(e0, e1, x):
    t = max(0.0, min(1.0, (x - e0) / (e1 - e0)))
    return t * t * (3 - 2 * t)


def grey(v):
    v = max(0, min(255, int(v * 255)))
    return (v, v, v, 255)


def additive(size, fn):
    """Greyscale additive texture: fn(r, angle, u, v) -> intensity 0..1, r = 1 at the edge of the square."""
    img = Image.new("RGBA", (size, size))
    px = img.load()
    c = size / 2
    for y in range(size):
        for x in range(size):
            dx, dy = (x + .5 - c) / c, (y + .5 - c) / c
            px[x, y] = grey(fn(math.hypot(dx, dy), math.atan2(dy, dx), x / size, y / size))
    return img


def bloom(img, radius, strength):
    """Adds a blurred copy on top (additive glow)."""
    rgb = img.convert("RGB")
    glow = rgb.filter(ImageFilter.GaussianBlur(radius))
    out = Image.new("RGB", rgb.size)
    a, b, o = rgb.load(), glow.load(), out.load()
    for y in range(rgb.size[1]):
        for x in range(rgb.size[0]):
            o[x, y] = tuple(min(255, int(a[x, y][i] + b[x, y][i] * strength)) for i in range(3))
    return out.convert("RGBA")


# ---------------------------------------------------------------- entity textures

def blast_fire():
    """A turbulent fireball: bright billows with dark gaps between them, ragged tongues at the edge."""
    def fn(r, ang, u, v):
        edge = .66 + .26 * fbm(math.cos(ang) * 2.5 + 5, math.sin(ang) * 2.5 + 5, 4, 7)
        if r > edge:
            return 0
        # Domain-warped noise: billowing, folded lumps
        wx = fbm(u * 4, v * 4, 3, 13)
        wy = fbm(u * 4 + 7, v * 4 + 3, 3, 17)
        billow = fbm(u * 7 + wx * 2.5, v * 7 + wy * 2.5, 4, 3)
        billow = smoothstep(.3, .75, billow)
        body = smoothstep(edge, edge * .6, r)
        heat = (1 - r / edge) ** 2
        return min(1.0, body * (.18 + .82 * billow) * (.75 + .25 * heat) + heat * .35)
    return additive(128, fn)


def blast_core():
    return additive(64, lambda r, a, u, v: max(0.0, 1 - r) ** 2.2)


def shock_ring():
    def fn(r, ang, u, v):
        ring = math.exp(-((r - .86) / .04) ** 2)
        haze = .3 * math.exp(-((r - .74) / .13) ** 2) if r < .86 else 0
        streaks = .6 + .4 * fbm(math.cos(ang) * 6 + 9, math.sin(ang) * 6 + 9, 2, 5)
        return min(1.0, (ring + haze) * streaks)
    return additive(128, fn)


def unstable_orb():
    """A shell of crackling arcs, orange-white (drawn over the orb's glow)."""
    S = 256
    img = Image.new("RGB", (S, S), (0, 0, 0))
    d = ImageDraw.Draw(img)
    rnd = random.Random(42)
    c = S / 2
    for arc in range(9):
        ang = arc * math.tau / 9 + rnd.uniform(-.3, .3)
        radius = rnd.uniform(.45, .85) * c
        pts = []
        for step in range(10):
            a = ang + step * .22
            rr = radius + rnd.uniform(-.09, .09) * c
            pts.append((c + math.cos(a) * rr, c + math.sin(a) * rr))
        col = CORE if arc % 3 == 0 else FLAME if arc % 3 == 1 else FIRE
        d.line(pts, fill=col, width=5 if arc % 3 == 0 else 3, joint="curve")
    img = img.resize((64, 64), Image.LANCZOS)
    return bloom(img, 2.5, 1.1)


def ruin_sigil():
    """A rune of ruin: double circle with runes between, a hexagram and a burst glyph in the middle."""
    S = 512
    img = Image.new("RGB", (S, S), (0, 0, 0))
    d = ImageDraw.Draw(img)
    c = S / 2
    white, dim = (255, 255, 255), (150, 150, 150)

    def circle(r, w, col=white):
        d.ellipse((c - r, c - r, c + r, c + r), outline=col, width=w)

    circle(.95 * c, 10)
    circle(.82 * c, 6)
    # Runes between the two circles
    rnd = random.Random(7)
    for k in range(16):
        a = k * math.tau / 16
        ca, sa = math.cos(a), math.sin(a)
        r0, r1 = .85 * c, .92 * c
        mid = (r0 + r1) / 2
        tx, ty = -sa, ca
        shape = rnd.randrange(4)
        p = lambda rr, t: (c + ca * rr + tx * t, c + sa * rr + ty * t)
        if shape == 0:
            d.line([p(r0, -10), p(r1, 0), p(r0, 10)], fill=white, width=5)
        elif shape == 1:
            d.line([p(r0, 0), p(r1, 0)], fill=white, width=5)
            d.line([p(mid, -10), p(mid, 10)], fill=white, width=5)
        elif shape == 2:
            d.line([p(r0, -9), p(r1, -9), p(r1, 9)], fill=white, width=5)
        else:
            d.ellipse((p(mid, 0)[0] - 7, p(mid, 0)[1] - 7, p(mid, 0)[0] + 7, p(mid, 0)[1] + 7), outline=white, width=4)
    # Hexagram
    for rot in (0, math.pi / 3):
        tri = [(c + math.cos(rot + k * math.tau / 3 - math.pi / 2) * .74 * c, c + math.sin(rot + k * math.tau / 3 - math.pi / 2) * .74 * c) for k in range(3)]
        d.polygon(tri, outline=white, width=6)
    circle(.38 * c, 6)
    circle(.3 * c, 3, dim)
    # Burst glyph: eight spikes
    for k in range(8):
        a = k * math.tau / 8 + math.pi / 8
        r = (.27 if k % 2 == 0 else .18) * c
        half = .2
        d.polygon([(c + math.cos(a - half) * .08 * c, c + math.sin(a - half) * .08 * c), (c + math.cos(a) * r, c + math.sin(a) * r),
                   (c + math.cos(a + half) * .08 * c, c + math.sin(a + half) * .08 * c)], fill=white)
    d.ellipse((c - .07 * c, c - .07 * c, c + .07 * c, c + .07 * c), fill=white)
    img = img.resize((128, 128), Image.LANCZOS)
    return bloom(img, 3, .9)


def implosion_core():
    """Black disc with a soft edge (alpha blended, not additive): darkens what is behind it."""
    size = 64
    img = Image.new("RGBA", (size, size))
    px = img.load()
    c = size / 2
    for y in range(size):
        for x in range(size):
            r = math.hypot(x + .5 - c, y + .5 - c) / c
            a = 1 - smoothstep(.5, 1.0, r)
            rim = smoothstep(.35, .62, r) * (1 - smoothstep(.62, .9, r))
            px[x, y] = (int(40 * rim), int(6 * rim), int(60 * rim), int(255 * a))
    return img


def implosion_swirl():
    """Two spiral arms of light winding into an empty middle."""
    def fn(r, ang, u, v):
        if r > 1 or r < .2:
            return 0
        window = smoothstep(.2, .42, r) * (1 - smoothstep(.75, 1.0, r))
        best = 0
        for arm in range(2):
            theta = math.log(r) * 3.2 + arm * math.pi
            delta = math.atan2(math.sin(ang - theta), math.cos(ang - theta))
            best = max(best, math.exp(-(delta / .45) ** 2))
        wisps = .55 + .45 * fbm(u * 8, v * 8, 3, 21)
        return min(1.0, best * window * wisps * 1.2 + window * .08)
    return additive(128, fn)


def shield_bubble():
    """A sphere of hexagonal cells, brighter towards its rim (seen face-on)."""
    cell = .22

    def hex_edge(px_, py_):
        # Distance to the nearest edge of a flat-top hexagonal grid (0 on an edge)
        q = (2 / 3 * px_) / cell
        rr = (-1 / 3 * px_ + math.sqrt(3) / 3 * py_) / cell
        x, z = q, rr
        y = -x - z
        rx, ry, rz = round(x), round(y), round(z)
        dx, dy, dz = abs(rx - x), abs(ry - y), abs(rz - z)
        if dx > dy and dx > dz:
            rx = -ry - rz
        elif dy > dz:
            ry = -rx - rz
        else:
            rz = -rx - ry
        cx_ = cell * 1.5 * rx
        cy_ = cell * math.sqrt(3) * (rz + rx / 2)
        ox, oy = abs(px_ - cx_), abs(py_ - cy_)
        inner = cell * math.sqrt(3) / 2
        return inner - max(oy, (ox * math.sqrt(3) + oy) / 2)

    def fn(r, ang, u, v):
        if r > 1:
            return 0
        # Bulge the grid like a sphere seen from the front
        k = math.asin(min(1.0, r)) / (math.pi / 2) / max(r, 1e-4)
        e = hex_edge(math.cos(ang) * r * k, math.sin(ang) * r * k)
        line = math.exp(-(e / .012) ** 2)
        fres = r ** 3
        rim = math.exp(-((r - .96) / .04) ** 2)
        return min(1.0, .06 + line * (.25 + .75 * fres) + rim * .9 + fres * .15)
    return additive(128, fn)


# ---------------------------------------------------------------- spell icons

def starburst(d, cx, cy, r, inner, col, spikes=10, rot=0.0):
    pts = []
    for k in range(spikes * 2):
        a = rot + k * math.pi / spikes
        rr = r if k % 2 == 0 else r * inner
        pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr))
    d.polygon(pts, fill=col)


def blast(d, img, cx, cy, r):
    """A small painted explosion: glow, orange burst, yellow burst, white heart."""
    g.glow_spot(img, cx, cy, r * 1.8, (255, 120, 30), 1.0)
    starburst(d, cx, cy, r, .55, FIRE + (255,), 9, .2)
    starburst(d, cx, cy, r * .72, .5, FLAME + (255,), 7, .5)
    d.ellipse((cx - r * .32, cy - r * .32, cx + r * .32, cy + r * .32), fill=CORE + (255,))


def zigzag(d, a, b, col, width, rnd, steps=6, jitter=.06, S=64):
    pts = [a]
    for i in range(1, steps):
        t = i / steps
        pts.append((a[0] + (b[0] - a[0]) * t + rnd.uniform(-jitter, jitter) * S, a[1] + (b[1] - a[1]) * t + rnd.uniform(-jitter, jitter) * S))
    pts.append(b)
    d.line(pts, fill=col, width=width)


BG_FIRE = ((70, 22, 10), NIGHT)
BG_ARCANE = ((52, 18, 70), NIGHT)


def blast_orb_icon():
    def draw(d, img, S):
        c = S / 2
        d.ellipse((c - .44 * S, c - .44 * S, c + .44 * S, c + .44 * S), outline=ARCANE + (255,), width=max(1, S // 28))
        d.ellipse((c - .36 * S, c - .36 * S, c + .36 * S, c + .36 * S), outline=CORE + (200,), width=max(1, S // 40))
        g.glow_spot(img, c, c, .38 * S, (255, 140, 40), 1.2)
        d.ellipse((c - .2 * S, c - .2 * S, c + .2 * S, c + .2 * S), fill=FIRE + (255,))
        d.ellipse((c - .13 * S, c - .13 * S, c + .13 * S, c + .13 * S), fill=FLAME + (255,))
        d.ellipse((c - .07 * S, c - .07 * S, c + .07 * S, c + .07 * S), fill=CORE + (255,))
        rnd = random.Random(3)
        for k in range(4):
            a = k * math.tau / 4 + .4
            zigzag(d, (c + math.cos(a) * .16 * S, c + math.sin(a) * .16 * S), (c + math.cos(a) * .33 * S, c + math.sin(a) * .33 * S),
                   CORE + (255,), max(1, S // 40), rnd, 3, .03, S)
    return g.painted(64, draw, (.5, .5) + BG_ARCANE)


def chain_detonation_icon():
    def draw(d, img, S):
        rnd = random.Random(5)
        spots = [(.2 * S, .78 * S, .1 * S), (.47 * S, .5 * S, .14 * S), (.76 * S, .24 * S, .19 * S)]
        for (ax, ay, _), (bx, by, _) in zip(spots, spots[1:]):
            zigzag(d, (ax, ay), (bx, by), ARCANE + (255,), max(1, S // 22), rnd, 5, .05, S)
            zigzag(d, (ax, ay), (bx, by), (230, 190, 255, 255), max(1, S // 50), rnd, 5, .03, S)
        for x, y, r in spots:
            blast(d, img, x, y, r)
    return g.painted(64, draw, (.6, .4) + BG_FIRE)


def sigils_of_ruin_icon():
    def draw(d, img, S):
        c = S / 2
        box = (.06 * S, .5 * S, .94 * S, .9 * S)
        g.glow_spot(img, c, .7 * S, .45 * S, (255, 90, 20), .9)
        d.ellipse(box, outline=FIRE + (255,), width=max(1, S // 22))
        d.ellipse((.2 * S, .57 * S, .8 * S, .83 * S), outline=FLAME + (255,), width=max(1, S // 40))
        for k in range(6):
            a = k * math.tau / 6
            x, y = c + math.cos(a) * .3 * S, .7 * S + math.sin(a) * .13 * S
            d.line((x - .03 * S, y, x + .03 * S, y), fill=CORE + (255,), width=max(1, S // 40))
        # Flame rising from the rune
        d.polygon([(.4 * S, .7 * S), (.5 * S, .18 * S), (.6 * S, .7 * S)], fill=FIRE + (255,))
        d.polygon([(.45 * S, .7 * S), (.5 * S, .36 * S), (.55 * S, .7 * S)], fill=FLAME + (255,))
        d.ellipse((.46 * S, .6 * S, .54 * S, .72 * S), fill=CORE + (255,))
    return g.painted(64, draw, (.5, .7) + BG_FIRE)


def detonation_wave_icon():
    def draw(d, img, S):
        c = S / 2
        cy = .6 * S
        # Shock rings on the ground
        for rx, ry, col in ((.46, .25, FIRE), (.3, .16, FLAME)):
            d.ellipse((c - rx * S, cy - ry * S, c + rx * S, cy + ry * S), outline=col + (255,), width=max(1, S // 26))
        g.glow_spot(img, c, cy, .16 * S, (255, 230, 170), 1.2)
        d.ellipse((c - .05 * S, cy - .035 * S, c + .05 * S, cy + .035 * S), fill=CORE + (255,))
        # A ring of explosions on the outer wave, smaller at the back
        for k in range(6):
            a = k * math.tau / 6 + math.pi / 6
            depth = (math.sin(a) + 1) / 2
            blast(d, img, c + math.cos(a) * .46 * S, cy + math.sin(a) * .25 * S - .04 * S, (.07 + .06 * depth) * S)
    return g.painted(64, draw, (.5, .6) + BG_FIRE)


def implosion_icon():
    def draw(d, img, S):
        c = S / 2
        for arm in range(2):
            pts = []
            for i in range(40):
                t = i / 39
                r = (.46 - .34 * t) * S
                a = arm * math.pi + t * 4.2
                pts.append((c + math.cos(a) * r, c + math.sin(a) * r))
            d.line(pts, fill=ARCANE + (255,), width=max(1, S // 14))
            d.line(pts, fill=(230, 190, 255, 255), width=max(1, S // 40))
        g.glow_spot(img, c, c, .24 * S, (200, 120, 255), .9)
        d.ellipse((c - .16 * S, c - .16 * S, c + .16 * S, c + .16 * S), fill=(4, 0, 8, 255), outline=CORE + (255,), width=max(1, S // 32))
    return g.painted(64, draw, (.5, .5) + BG_ARCANE)


def volatile_brand_mark(d, S, lit, ox=0.0, oy=0.0, scale=1.0):
    def p(x, y):
        return (ox * S + x * S * scale, oy * S + y * S * scale)
    w = max(1, int(S * scale) // 14)
    c = p(.5, .5)
    r = .32 * S * scale
    d.ellipse((c[0] - r, c[1] - r, c[0] + r, c[1] + r), outline=EMBER + (255,), width=w)
    for k in range(5):
        a = -math.pi / 2 + k * math.tau / 5
        col = FLAME if k < lit else (90, 30, 20)
        x0, y0 = c[0] + math.cos(a) * r * .55, c[1] + math.sin(a) * r * .55
        x1, y1 = c[0] + math.cos(a) * r * 1.3, c[1] + math.sin(a) * r * 1.3
        d.line((x0, y0, x1, y1), fill=col + (255,), width=w)
    d.ellipse((c[0] - r * .3, c[1] - r * .3, c[0] + r * .3, c[1] + r * .3), fill=CORE + (255,))


def volatile_brand_icon():
    def draw(d, img, S):
        g.glow_spot(img, S / 2, S / 2, .45 * S, (255, 110, 30), 1.0)
        volatile_brand_mark(d, S, 3)
    return g.painted(64, draw, (.5, .5) + BG_FIRE)


def blast_leap_icon():
    def draw(d, img, S):
        blast(d, img, .5 * S, .82 * S, .16 * S)
        # Comet trail and the leaping figure as an arrowhead
        d.line([(.5 * S, .72 * S), (.55 * S, .45 * S), (.66 * S, .22 * S)], fill=FIRE + (255,), width=int(S * .1), joint="curve")
        d.line([(.5 * S, .72 * S), (.55 * S, .45 * S), (.66 * S, .22 * S)], fill=FLAME + (255,), width=int(S * .045), joint="curve")
        d.polygon([(.58 * S, .26 * S), (.74 * S, .06 * S), (.78 * S, .3 * S)], fill=CORE + (255,), outline=FIRE + (255,))
    return g.painted(64, draw, (.5, .8) + BG_FIRE)


def cluster_barrage_icon():
    def draw(d, img, S):
        c = S / 2
        g.glow_spot(img, c, .22 * S, .2 * S, (200, 120, 255), 1.0)
        d.ellipse((c - .1 * S, .12 * S, c + .1 * S, .32 * S), fill=ARCANE + (255,), outline=CORE + (255,), width=max(1, S // 40))
        for k in range(8):
            a = math.pi * (.08 + .84 * k / 7)
            x = c + math.cos(a) * .4 * S
            y = .3 * S + math.sin(a) * .5 * S
            d.line((c, .25 * S, x, y), fill=(255, 150, 60, 210), width=max(1, S // 32))
            g.glow_spot(img, x, y, .07 * S, (255, 150, 50), 1.0)
            d.ellipse((x - .035 * S, y - .035 * S, x + .035 * S, y + .035 * S), fill=FLAME + (255,))
    return g.painted(64, draw, (.5, .3) + BG_ARCANE)


def reactive_shield_icon():
    def draw(d, img, S):
        c = S / 2
        g.glow_spot(img, c, c, .48 * S, (180, 90, 255), .7)
        hexagon = [(c + math.cos(k * math.tau / 6) * .42 * S, c + math.sin(k * math.tau / 6) * .42 * S) for k in range(6)]
        d.polygon(hexagon, outline=ARCANE + (255,), width=max(1, S // 18))
        for k in range(6):
            a = k * math.tau / 6
            d.line((c, c, c + math.cos(a) * .42 * S, c + math.sin(a) * .42 * S), fill=(150, 80, 220, 200), width=max(1, S // 50))
        inner = [(c + math.cos(k * math.tau / 6) * .22 * S, c + math.sin(k * math.tau / 6) * .22 * S) for k in range(6)]
        d.polygon(inner, outline=(220, 180, 255, 255), width=max(1, S // 40))
        # The shell going off on one side
        blast(d, img, .78 * S, .3 * S, .12 * S)
    return g.painted(64, draw, (.5, .5) + BG_ARCANE)


def cataclysm_icon():
    def draw(d, img, S):
        c = S / 2
        horizon = .78 * S
        for k in range(18):
            a = math.pi + k * math.pi / 17
            r = (.5 if k % 2 == 0 else .38) * S
            d.line((c, horizon, c + math.cos(a) * r, horizon + math.sin(a) * r), fill=(255, 220, 160, 160), width=max(1, S // 40))
        g.glow_spot(img, c, horizon, .5 * S, (255, 230, 180), 1.4)
        d.pieslice((c - .3 * S, horizon - .3 * S, c + .3 * S, horizon + .3 * S), 180, 360, fill=CORE + (255,))
        d.arc((c - .4 * S, horizon - .4 * S, c + .4 * S, horizon + .4 * S), 180, 360, fill=ARCANE + (255,), width=max(1, S // 26))
        for x in (.1, .9):
            blast(d, img, x * S, horizon - .02 * S, .08 * S)
        d.rectangle((0, horizon, S, S), fill=(30, 10, 12, 255))
    return g.painted(64, draw, (.5, .75, (90, 40, 30), NIGHT))


# ---------------------------------------------------------------- effect icons

def volatile_brand_effect_icon():
    return g.painted(72, lambda d, img, S: volatile_brand_mark(d, S, 5, .02, .02, .96), None, 18)


def blast_leap_effect_icon():
    def draw(d, img, S):
        d.polygon([(.5 * S, .06 * S), (.86 * S, .5 * S), (.64 * S, .5 * S), (.64 * S, .78 * S), (.36 * S, .78 * S), (.36 * S, .5 * S), (.14 * S, .5 * S)],
                  fill=FLAME + (255,), outline=EMBER + (255,))
        d.polygon([(.36 * S, .8 * S), (.5 * S, .98 * S), (.64 * S, .8 * S)], fill=FIRE + (255,))
    return g.painted(72, draw, None, 18)


def reactive_shield_effect_icon():
    def draw(d, img, S):
        c = S / 2
        hexagon = [(c + math.cos(k * math.tau / 6) * .44 * S, c + math.sin(k * math.tau / 6) * .44 * S) for k in range(6)]
        d.polygon(hexagon, fill=ARCANE_DARK + (255,), outline=ARCANE + (255,))
        inner = [(c + math.cos(k * math.tau / 6) * .24 * S, c + math.sin(k * math.tau / 6) * .24 * S) for k in range(6)]
        d.polygon(inner, outline=(230, 200, 255, 255))
        d.ellipse((c - .08 * S, c - .08 * S, c + .08 * S, c + .08 * S), fill=FLAME + (255,))
    return g.painted(72, draw, None, 18)


# ---------------------------------------------------------------- items

def scroll_explosion():
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
    ], {"o": CHAR, "h": FLAME, "l": FIRE, "b": EMBER, "s": (110, 24, 8),
        "c": g.CREAM, "x": g.CREAM_SHADE, "g": ARCANE})


def affinity_ring_explosion():
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
    ], {"o": CHAR, "h": IRON_LIGHT, "l": IRON, "b": CHAR_LIGHT, "s": (34, 26, 30),
        "g": FIRE, "t": EMBER})


def detonation_codex_icon():
    return g.from_grid([
        "................",
        "...oooooooooo...",
        "..ohhllllllbco..",
        "..ohoooooooocco.",
        "..olo.fe..obco..",
        "..olo..ef.obco..",
        "..olo.eyfeobcco.",
        "..olo.fye.obcco.",
        "..olo..fe.obcco.",
        "..olo.ef..obcco.",
        "..olooooooobcco.",
        "..olbbbbbbbbcco.",
        "..osbbbbbbbsxco.",
        "...ooooooooooo..",
        "................",
        "................",
    ], {"o": (8, 6, 8), "h": IRON_LIGHT, "l": CHAR_LIGHT, "b": CHAR, "s": (12, 8, 10),
        "c": g.CREAM, "x": g.CREAM_SHADE, "f": FIRE, "e": EMBER, "y": FLAME})


def crack_paths(rnd, x0, y0, length, angle, depth, out):
    """Branching cracks for the codex cover."""
    pts = [(x0, y0)]
    x, y = x0, y0
    for _ in range(int(length)):
        angle += rnd.uniform(-.5, .5)
        x += math.cos(angle) * 2
        y += math.sin(angle) * 2
        pts.append((x, y))
        if depth > 0 and rnd.random() < .08:
            crack_paths(rnd, x, y, length * .5, angle + rnd.choice((-1, 1)) * rnd.uniform(.6, 1.1), depth - 1, out)
    out.append(pts)


def detonation_codex_model_texture():
    """64x64 texture of the 3D codex, same layout as the Golden Grimoire's: black cracked leather, magma in the
    cracks, blackened iron fittings and an ember gem."""
    s = 4
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    def box(u0, v0, u1, v1, fill):
        d.rectangle((u0 * s, v0 * s, u1 * s - 1, v1 * s - 1), fill=fill + (255,))

    W, H = 12 * s, 9 * s
    for y in range(H):
        for x in range(W):
            n = fbm(x / 6, y / 6, 3, 2)
            img.putpixel((x, y), g.lerp((14, 10, 12), (44, 32, 36), n) + (255,))
    # Magma cracks across the cover, glowing from a burst in the middle
    cover = Image.new("RGB", (W, H), (0, 0, 0))
    cd = ImageDraw.Draw(cover)
    rnd = random.Random(11)
    paths = []
    for k in range(7):
        a = k * math.tau / 7 + rnd.uniform(-.2, .2)
        crack_paths(rnd, W / 2, H / 2, 9, a, 2, paths)
    for pts in paths:
        cd.line(pts, fill=FIRE, width=1)
    cd.ellipse((W / 2 - 4, H / 2 - 4, W / 2 + 3, H / 2 + 3), fill=FLAME)
    cd.ellipse((W / 2 - 2, H / 2 - 2, W / 2 + 1, H / 2 + 1), fill=CORE)
    glow = cover.filter(ImageFilter.GaussianBlur(1.2))
    cp, gp = cover.load(), glow.load()
    for y in range(H):
        for x in range(W):
            base = img.getpixel((x, y))
            add = tuple(min(255, int(cp[x, y][i] + gp[x, y][i] * .8)) for i in range(3))
            if sum(add) > 30:
                img.putpixel((x, y), tuple(min(255, base[i] + add[i]) for i in range(3)) + (255,))
    d.rectangle((0, 0, W - 1, H - 1), outline=(6, 4, 6, 255))
    d.rectangle((1, 1, W - 2, H - 2), outline=IRON + (255,))
    d.rectangle((2, 2, W - 3, H - 3), outline=CHAR_LIGHT + (255,))
    # Cover edge
    box(12, 0, 13, 9, CHAR_LIGHT)
    d.line((12 * s, 0, 12 * s, H - 1), fill=EMBER + (255,))
    # Pages side: scorched parchment
    for y in range(0, 8 * s):
        for x in range(13 * s, 16 * s):
            img.putpixel((x, y), g.lerp(g.CREAM, (150, 110, 70), fbm(x / 3, y / 3, 2, 4) * .8) + (255,))
    for y in range(1, 8 * s, 2):
        d.line((13 * s, y, 16 * s - 1, y), fill=g.CREAM_SHADE + (255,))
    # Spine with a glowing seam
    for y in range(9 * s, 12 * s):
        for x in range(0, 12 * s):
            t = abs(y - 10.5 * s) / (1.5 * s)
            img.putpixel((x, y), g.lerp((48, 36, 40), (14, 10, 12), t) + (255,))
    d.line((0, 10 * s + 2, 12 * s - 1, 10 * s + 2), fill=EMBER + (255,))
    d.line((0, 10 * s + 1, 12 * s - 1, 10 * s + 1), fill=(120, 30, 10, 255))
    # Page edges, charred at the rims
    box(0, 12, 12, 15, g.CREAM)
    for x in range(0, 12 * s, 2):
        d.line((x, 12 * s, x, 15 * s - 1), fill=g.CREAM_SHADE + (255,))
    d.line((0, 12 * s, 12 * s - 1, 12 * s), fill=(60, 40, 30, 255))
    d.line((0, 15 * s - 1, 12 * s - 1, 15 * s - 1), fill=(60, 40, 30, 255))
    # Fittings: blackened iron with an ember edge
    box(12, 9, 14, 11, IRON)
    d.rectangle((12 * s, 9 * s, 14 * s - 1, 11 * s - 1), outline=CHAR + (255,))
    d.line((12 * s + 1, 9 * s + 1, 14 * s - 2, 9 * s + 1), fill=IRON_LIGHT + (255,))
    d.line((12 * s + 1, 11 * s - 2, 14 * s - 2, 11 * s - 2), fill=EMBER + (255,))
    # Ember gem
    for y in range(9 * s, 11 * s):
        for x in range(14 * s, 16 * s):
            t = math.hypot(x - 14.8 * s, y - 9.8 * s) / (1.4 * s)
            img.putpixel((x, y), g.lerp(CORE, EMBER, t ** .8) + (255,))
    # Strap: charred leather with glowing stitching
    box(12, 11, 16, 12, (36, 22, 20))
    for x in range(12 * s + 1, 16 * s, 3):
        img.putpixel((x, 11 * s + 1), FIRE + (255,))
        img.putpixel((x, 12 * s - 2), FIRE + (255,))
    return img


def main():
    g.save(blast_fire(), "textures/entity/blast_fire.png")
    g.save(blast_core(), "textures/entity/blast_core.png")
    g.save(shock_ring(), "textures/entity/shock_ring.png")
    g.save(unstable_orb(), "textures/entity/unstable_orb.png")
    g.save(ruin_sigil(), "textures/entity/ruin_sigil.png")
    g.save(implosion_core(), "textures/entity/implosion_core.png")
    g.save(implosion_swirl(), "textures/entity/implosion_swirl.png")
    g.save(shield_bubble(), "textures/entity/shield_bubble.png")
    g.save(blast_orb_icon(), "textures/gui/spell_icons/blast_orb.png")
    g.save(chain_detonation_icon(), "textures/gui/spell_icons/chain_detonation.png")
    g.save(sigils_of_ruin_icon(), "textures/gui/spell_icons/sigils_of_ruin.png")
    g.save(detonation_wave_icon(), "textures/gui/spell_icons/detonation_wave.png")
    g.save(implosion_icon(), "textures/gui/spell_icons/implosion.png")
    g.save(volatile_brand_icon(), "textures/gui/spell_icons/volatile_brand.png")
    g.save(blast_leap_icon(), "textures/gui/spell_icons/blast_leap.png")
    g.save(cluster_barrage_icon(), "textures/gui/spell_icons/cluster_barrage.png")
    g.save(reactive_shield_icon(), "textures/gui/spell_icons/reactive_shield.png")
    g.save(cataclysm_icon(), "textures/gui/spell_icons/cataclysm.png")
    g.save(volatile_brand_effect_icon(), "textures/mob_effect/volatile_brand.png")
    g.save(blast_leap_effect_icon(), "textures/mob_effect/blast_leap.png")
    g.save(reactive_shield_effect_icon(), "textures/mob_effect/reactive_shield.png")
    g.save(scroll_explosion(), "textures/item/scroll_explosion.png")
    g.save(affinity_ring_explosion(), "textures/item/affinity_ring_explosion.png")
    g.save(detonation_codex_icon(), "textures/item/detonation_codex.png")
    g.save(detonation_codex_model_texture(), "textures/item/detonation_codex_3d.png")
    g.write_json("models/item/scroll_explosion.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/scroll_explosion"}})
    g.write_json("models/item/affinity_ring_explosion.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/affinity_ring_explosion"}})
    g.book_models("detonation_codex")
