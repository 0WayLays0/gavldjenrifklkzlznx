#!/usr/bin/env python3
"""Boss Magic materials: the big textures the Boss models are made of (aged bronze, blackened iron, chased gold, veined
marble, obsidian, bone, dark cloth, blade steel, crimson glass), the glow masks laid over them (engraved runes, burning
cracks), the clock's face and the board's tiles.

Called by boss_assets.py. All art is original and drawn here from noise and strokes. Solid materials tile both ways;
glow masks are greyscale on black (drawn additively, tinted by the renderer).
"""
import math
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

import gen_assets as g

N = 256


# ---------------------------------------------------------------- noise

def noise(w, h, cells, seed):
    """Smooth value noise in [0, 1], tiling across and down; cells = (across, down)."""
    rnd = np.random.default_rng(seed)
    grid = rnd.random((cells[1], cells[0]))
    ys, xs = np.mgrid[0:h, 0:w]
    gx, gy = xs / w * cells[0], ys / h * cells[1]
    x0, y0 = gx.astype(int) % cells[0], gy.astype(int) % cells[1]
    fx, fy = gx - np.floor(gx), gy - np.floor(gy)
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    x1, y1 = (x0 + 1) % cells[0], (y0 + 1) % cells[1]
    a = grid[y0, x0] * (1 - fx) + grid[y0, x1] * fx
    b = grid[y1, x0] * (1 - fx) + grid[y1, x1] * fx
    return a * (1 - fy) + b * fy


def fbm(w, h, base, octaves, seed, stretch=1):
    """Fractal noise in [0, 1]; stretch > 1 draws it out across (brushed metal, wood grain)."""
    total, amp, norm = np.zeros((h, w)), 1.0, 0.0
    for o in range(octaves):
        c = base * 2 ** o
        total += noise(w, h, (max(1, int(c / stretch)), max(1, c * h // w)), seed + o * 17) * amp
        norm += amp
        amp *= .5
    return total / norm


def worley(w, h, count, seed):
    """Distances to the nearest and second nearest of count scattered points (tiling), scaled so cells are ~1 across."""
    rnd = np.random.default_rng(seed)
    pts = rnd.random((count, 2)) * (w, h)
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
    f1 = np.full((h, w), 1e9, np.float32)
    f2 = np.full((h, w), 1e9, np.float32)
    for px, py in pts:
        for ox in (-w, 0, w):
            for oy in (-h, 0, h):
                d = np.hypot(xs - (px + ox), ys - (py + oy))
                f2 = np.where(d < f1, f1, np.minimum(f2, d))
                f1 = np.minimum(f1, d)
    cell = math.sqrt(w * h / count)
    return f1 / cell, f2 / cell


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def ramp(v, stops):
    """v (0 to 1) through colour stops [(pos, (r, g, b)), ...] -> h x w x 3 floats 0..255."""
    v = np.clip(v, 0, 1)
    out = np.zeros(v.shape + (3,))
    for (p0, c0), (p1, c1) in zip(stops, stops[1:]):
        t = np.clip((v - p0) / max(p1 - p0, 1e-6), 0, 1)
        inside = (v >= p0) & (v <= p1)
        for i in range(3):
            out[..., i] = np.where(inside, c0[i] + (c1[i] - c0[i]) * t, out[..., i])
    return out


def image(rgb, alpha=None):
    rgb = np.clip(rgb, 0, 255)
    a = np.full(rgb.shape[:2], 255.0) if alpha is None else np.clip(alpha, 0, 255)
    return Image.fromarray(np.dstack([rgb, a]).astype(np.uint8), "RGBA")


def mix(a, b, t):
    t = t[..., None] if np.ndim(t) == 2 else t
    return a * (1 - t) + np.asarray(b, dtype=float) * t


def scratches(w, h, count, seed, length=(8, 40), light=True):
    """A layer of thin straight scratches, 0 to 1."""
    img = Image.new("L", (w, h), 0)
    d = ImageDraw.Draw(img)
    rnd = random.Random(seed)
    for _ in range(count):
        x, y = rnd.random() * w, rnd.random() * h
        a = rnd.random() * math.tau if rnd.random() < .4 else rnd.gauss(0, .25)
        l = rnd.uniform(*length)
        for ox in (-w, 0, w):
            for oy in (-h, 0, h):
                d.line([(x + ox, y + oy), (x + ox + math.cos(a) * l, y + oy + math.sin(a) * l)], fill=rnd.randint(90, 200), width=1)
    return np.asarray(img, dtype=float) / 255


def emboss(mask, strength):
    """Light from the top left on a height mask: -1 (shadowed) to 1 (lit) where the mask's edges are."""
    hgt = np.asarray(Image.fromarray((mask * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(.8)), dtype=float) / 255
    return np.clip((np.roll(hgt, 1, 0) + np.roll(hgt, 1, 1) - np.roll(hgt, -1, 0) - np.roll(hgt, -1, 1)) * strength, -1, 1)


def glyph(d, cx, cy, size, seed, width, col=255):
    """A rune: three to five strokes between the points of a 3x3 grid, always with a stem (as the Boss circles draw them)."""
    rnd = random.Random(seed)
    pts = [(cx + (i - 1) * size * .5, cy + (j - 1) * size * .5) for j in range(3) for i in range(3)]
    stem = rnd.choice(((1, 7), (0, 6), (2, 8)))
    d.line([pts[stem[0]], pts[stem[1]]], fill=col, width=width)
    for _ in range(rnd.randint(2, 4)):
        a, b = rnd.sample(range(9), 2)
        if (pts[a][0] - pts[b][0]) ** 2 + (pts[a][1] - pts[b][1]) ** 2 > (size * 1.2) ** 2:
            continue
        d.line([pts[a], pts[b]], fill=col, width=width)
    if rnd.random() < .35:
        r = size * .14
        p = pts[rnd.choice((1, 3, 4, 5, 7))]
        d.ellipse((p[0] - r, p[1] - r, p[0] + r, p[1] + r), outline=col, width=max(1, width - 1))


def glow_mask(img, blur=2.0, strength=.8):
    """A white-on-black mask with a soft halo round its strokes."""
    a = np.asarray(img.convert("L"), dtype=float)
    halo = np.asarray(img.convert("L").filter(ImageFilter.GaussianBlur(blur)), dtype=float)
    v = np.clip(a + halo * strength, 0, 255)
    return image(np.dstack([v, v, v]))


# ---------------------------------------------------------------- metals

def bronze():
    """Old cast bronze, hammered, green with verdigris in its hollows, scratched bright here and there."""
    f1, _ = worley(N, N, 260, 11)
    # The hammer's dents: catching the light on one side, shadowed on the other
    dents = emboss(np.clip(f1 / 1.2, 0, 1), 1.5)
    base = fbm(N, N, 4, 5, 12)
    rgb = ramp(base + dents * .08, [(0, (66, 38, 16)), (.4, (128, 78, 34)), (.7, (178, 120, 58)), (1, (226, 172, 98))])
    # Verdigris where the rain ran down: streaks, not blots
    runs = fbm(N, N, 3, 5, 13, stretch=1 / 6)
    patina = smooth(.62, .8, runs * .7 + fbm(N, N, 6, 3, 16) * .3)
    speck = noise(N, N, (96, 96), 14)
    rgb = mix(rgb, (52, 118, 100), patina * (.45 + .3 * speck))
    rgb = mix(rgb, (110, 176, 150), patina * smooth(.75, .95, speck) * .35)
    rgb = mix(rgb, (255, 222, 160), scratches(N, N, 30, 15, (5, 22)) * .3)
    return image(rgb)


def iron():
    """Blackened iron: brushed dark steel, worn bright on its high spots, a bloom of rust here and there."""
    brushed = fbm(N, N, 16, 4, 21, stretch=8)
    wear = fbm(N, N, 4, 5, 22)
    rgb = ramp(wear * .75 + brushed * .25, [(0, (16, 14, 20)), (.45, (36, 32, 42)), (.75, (62, 56, 68)), (1, (104, 96, 110))])
    rust = smooth(.66, .8, fbm(N, N, 3, 5, 23))
    rgb = mix(rgb, (96, 46, 24), rust * .7)
    rgb = mix(rgb, (150, 150, 165), scratches(N, N, 45, 24, (6, 30)) * .25)
    return image(rgb)


def steel():
    """Blade steel, folded: pale waves of the folds under a mirror polish."""
    f = fbm(N, N, 3, 4, 31)
    waves = .5 + .5 * np.sin((np.mgrid[0:N, 0:N][0] / N * 18 + f * 9) * math.pi)
    rgb = ramp(waves * .35 + fbm(N, N, 6, 3, 32) * .65, [(0, (110, 116, 130)), (.5, (168, 174, 188)), (1, (226, 232, 242))])
    rgb = mix(rgb, (250, 252, 255), scratches(N, N, 40, 33, (10, 40)) * .3)
    return image(rgb)


def gold():
    """Chased gold: polished, with scrolls of engraved filigree."""
    base = fbm(N, N, 4, 4, 41)
    rgb = ramp(base, [(0, (150, 92, 22)), (.5, (222, 164, 52)), (1, (255, 226, 132))])
    lines = Image.new("L", (N, N), 0)
    d = ImageDraw.Draw(lines)
    rnd = random.Random(42)
    # Scrolls: spirals curling out of a running vine
    for row in range(4):
        cy = (row + .5) * N / 4
        pts = [(x, cy + math.sin(x / N * math.tau * 2 + row) * N * .05) for x in range(-8, N + 9, 4)]
        d.line(pts, fill=255, width=2)
        for k in range(5):
            cx0 = (k + .5 + (row % 2) * .5) * N / 5
            sign = 1 if (k + row) % 2 else -1
            spiral = []
            for i in range(40):
                t = i / 39
                a = t * math.tau * 1.6
                r = N * .07 * (1 - t * .85)
                spiral.append((cx0 + math.cos(a) * r * sign, cy + sign * N * .06 + math.sin(a) * r))
            d.line(spiral, fill=255, width=2)
            leaf = rnd.uniform(.03, .05) * N
            d.ellipse((cx0 - leaf * .4, cy - leaf, cx0 + leaf * .4, cy + leaf * .2), outline=200, width=1)
    m = np.asarray(lines, dtype=float) / 255
    rgb = mix(rgb, (110, 64, 12), m * .75)
    rgb = mix(rgb, (255, 240, 180), np.clip(emboss(m, 3), 0, 1) * .5)
    return image(rgb)


# ---------------------------------------------------------------- stone, bone, glass, cloth

def marble():
    """White marble, grey veins wandering through it, warm in places."""
    ys, xs = np.mgrid[0:N, 0:N] / N
    turb = fbm(N, N, 3, 5, 51)
    veins = np.abs(np.sin((xs * 2 + ys * 3 + turb * 3.5) * math.pi * 2))
    fine = np.abs(np.sin((xs * 5 - ys * 2 + fbm(N, N, 4, 4, 52) * 4) * math.pi * 2))
    rgb = ramp(fbm(N, N, 4, 4, 53), [(0, (210, 204, 196)), (.6, (236, 232, 222)), (1, (250, 248, 242))])
    rgb = mix(rgb, (118, 116, 124), (1 - smooth(0, .07, veins)) * .7)
    rgb = mix(rgb, (160, 156, 160), (1 - smooth(0, .04, fine)) * .4)
    rgb = mix(rgb, (226, 206, 176), smooth(.6, .9, fbm(N, N, 2, 3, 54)) * .35)
    return image(rgb)


def obsidian():
    """Obsidian: black glass, a violet sheen in its curved fractures."""
    f1, f2 = worley(N, N, 26, 61)
    edge = 1 - smooth(0, .12, f2 - f1)
    sheen = fbm(N, N, 3, 4, 62, stretch=3)
    rgb = ramp(sheen, [(0, (8, 6, 12)), (.6, (22, 16, 32)), (1, (58, 38, 86))])
    rgb = mix(rgb, (96, 70, 140), edge * .5)
    rgb = mix(rgb, (150, 130, 200), smooth(.85, 1, sheen) * .3)
    return image(rgb)


def bone():
    """Old bone: ivory, yellowed along its grain, pitted."""
    grain = fbm(N, N, 4, 5, 71, stretch=1 / 5)
    rgb = ramp(grain, [(0, (150, 128, 96)), (.5, (212, 196, 164)), (1, (240, 232, 210))])
    pits = noise(N, N, (90, 90), 72)
    rgb = mix(rgb, (96, 78, 58), smooth(.82, .95, pits) * .6)
    return image(rgb)


def cloth():
    """Dark cloth: a deep violet-black weave, threads running down it, faded in its folds."""
    ys, xs = np.mgrid[0:N, 0:N]
    weave = (.5 + .25 * np.sin(xs * math.pi / 2) + .25 * np.sin(ys * math.pi / 2)) * .25
    folds = .5 + .5 * np.sin((xs / N * 6 + fbm(N, N, 2, 3, 81) * 2) * math.tau)
    rgb = ramp(folds * .22 + weave + fbm(N, N, 8, 3, 82) * .3 + fbm(N, N, 3, 4, 83) * .25, [(0, (10, 6, 14)), (.5, (30, 20, 40)), (1, (62, 44, 78))])
    return image(rgb)


def crimson_glass():
    """Crimson glass: deep red, brighter along its veins, clouded; seen through (alpha)."""
    f1, f2 = worley(N, N, 18, 91)
    vein = 1 - smooth(0, .1, f2 - f1)
    cloud = fbm(N, N, 3, 4, 92)
    rgb = ramp(cloud, [(0, (120, 6, 26)), (.6, (200, 24, 48)), (1, (255, 90, 110))])
    rgb = mix(rgb, (255, 190, 200), vein * .6)
    alpha = 150 + cloud * 60 + vein * 45
    return image(rgb, alpha)


def flesh():
    """Raw flesh of the abyss: dark crimson going to violet, veins standing out, a wet sheen in places."""
    f1, f2 = worley(N, N, 30, 151)
    vein = 1 - smooth(0, .07, f2 - f1)
    ys, xs = np.mgrid[0:N, 0:N]
    dx = ((fbm(N, N, 4, 3, 152) - .5) * 30).astype(int)
    dy = ((fbm(N, N, 4, 3, 153) - .5) * 30).astype(int)
    vein = vein[(ys + dy) % N, (xs + dx) % N]
    base = fbm(N, N, 4, 5, 154)
    rgb = ramp(base, [(0, (40, 6, 22)), (.5, (96, 16, 44)), (1, (150, 40, 80))])
    rgb = mix(rgb, (70, 20, 90), smooth(.55, .8, fbm(N, N, 3, 4, 155)) * .5)
    rgb = mix(rgb, (190, 50, 70), vein * .7)
    rgb = mix(rgb, (255, 200, 220), smooth(.86, .97, noise(N, N, (48, 48), 156)) * .4)
    return image(rgb)


# ---------------------------------------------------------------- glow masks

def rune_band(count=16, w=512, h=64, seed=101):
    """A strip of runes side by side, glowing (wraps round a bell, a gauntlet's cuff, a column)."""
    img = Image.new("L", (w, h), 0)
    d = ImageDraw.Draw(img)
    d.line([(0, 4), (w, 4)], fill=170, width=2)
    d.line([(0, h - 5), (w, h - 5)], fill=170, width=2)
    for k in range(count):
        cx = (k + .5) * w / count
        glyph(d, cx, h / 2, h * .5, seed + k, 3)
        d.ellipse((cx + w / count / 2 - 2, h / 2 - 2, cx + w / count / 2 + 2, h / 2 + 2), fill=200)
    return glow_mask(img, 2.2, .9)


def cracks():
    """Burning cracks: a net of fissures, wider where they meet (laid on obsidian, iron, the board)."""
    f1, f2 = worley(N, N, 22, 111)
    fine1, fine2 = worley(N, N, 60, 112)
    v = (1 - smooth(0, .035, f2 - f1)) + (1 - smooth(0, .025, fine2 - fine1)) * .5 * smooth(.45, .7, fbm(N, N, 2, 3, 113))
    # Bent about so they wander like real cracks
    ys, xs = np.mgrid[0:N, 0:N]
    dx = ((fbm(N, N, 4, 3, 114) - .5) * 28).astype(int)
    dy = ((fbm(N, N, 4, 3, 115) - .5) * 28).astype(int)
    v = v[(ys + dy) % N, (xs + dx) % N]
    img = Image.fromarray((np.clip(v, 0, 1) * 255).astype(np.uint8), "L")
    return glow_mask(img, 2.5, 1.0)


def engraving():
    """Engraved glowing tracery for armour plates: framing lines, a diamond, runes in the corners."""
    img = Image.new("L", (N, N), 0)
    d = ImageDraw.Draw(img)
    m = N * .08
    d.rectangle((m, m, N - m, N - m), outline=200, width=3)
    d.rectangle((m * 1.6, m * 1.6, N - m * 1.6, N - m * 1.6), outline=120, width=1)
    c = N / 2
    d.polygon([(c, m * 2.4), (N - m * 2.4, c), (c, N - m * 2.4), (m * 2.4, c)], outline=255, width=3)
    for i, (x, y) in enumerate(((m * 3, m * 3), (N - m * 3, m * 3), (m * 3, N - m * 3), (N - m * 3, N - m * 3))):
        glyph(d, x, y, N * .07, 120 + i, 2)
    glyph(d, c, c, N * .16, 125, 4)
    return glow_mask(img, 2.0, .8)


# ---------------------------------------------------------------- the clock and the board

def roman(d, cx, cy, h, numeral, angle, width, col):
    """A Roman numeral drawn in strokes (I, V, X), standing on (cx, cy) turned by angle (radians, 0 upright)."""
    glyphs = []
    x = 0.0
    for ch in numeral:
        if ch == "I":
            glyphs.append([((x, 0), (x, 1))])
            x += .35
        elif ch == "V":
            glyphs.append([((x, 1), (x + .3, 0)), ((x + .3, 0), (x + .6, 1))])
            x += .75
        elif ch == "X":
            glyphs.append([((x, 1), (x + .6, 0)), ((x, 0), (x + .6, 1))])
            x += .75
    total = x - .15
    ca, sa = math.cos(angle), math.sin(angle)
    for strokes in glyphs:
        for (x0, y0), (x1, y1) in strokes:
            pts = []
            for px, py in ((x0, y0), (x1, y1)):
                lx, ly = (px - total / 2) * h * .55, (py - .5) * h
                pts.append((cx + lx * ca - ly * sa, cy + lx * sa + ly * ca))
            d.line(pts, fill=col, width=width)
    # Serifs top and bottom
    for sy in (-.5, .5):
        lx0, lx1, ly = -total / 2 * h * .55 - h * .06, total / 2 * h * .55 + h * .06, sy * h
        d.line([(cx + lx0 * ca - ly * sa, cy + lx0 * sa + ly * ca), (cx + lx1 * ca - ly * sa, cy + lx1 * sa + ly * ca)], fill=col, width=max(1, width - 2))


NUMERALS = ("XII", "I", "II", "III", "IIII", "V", "VI", "VII", "VIII", "IX", "X", "XI")


def clock_face(size=512):
    """The Clock of Fate's face: black lacquer, rings of gold, twelve numerals, the minutes, a rose of runes in the middle."""
    w = size
    c = w / 2
    lac = ramp(fbm(w, w, 3, 4, 131), [(0, (12, 8, 18)), (1, (34, 24, 44))])
    img = image(lac)
    d = ImageDraw.Draw(img)
    goldc, dark = (230, 178, 74, 255), (120, 80, 24, 255)

    def ring(r, width, col):
        d.ellipse((c - r, c - r, c + r, c + r), outline=col, width=width)

    ring(c * .985, int(w * .02), goldc)
    ring(c * .94, int(w * .006), dark)
    ring(c * .74, int(w * .01), goldc)
    ring(c * .7, int(w * .004), dark)
    ring(c * .3, int(w * .008), goldc)
    for k in range(60):
        a = k * math.tau / 60 - math.pi / 2
        r0, r1 = c * .93, c * (.86 if k % 5 == 0 else .9)
        d.line([(c + math.cos(a) * r0, c + math.sin(a) * r0), (c + math.cos(a) * r1, c + math.sin(a) * r1)], fill=goldc, width=4 if k % 5 == 0 else 2)
    for k, numeral in enumerate(NUMERALS):
        a = k * math.tau / 12
        r = c * .8
        roman(d, c + math.sin(a) * r, c - math.cos(a) * r, w * .075, numeral, a, max(2, w // 120), goldc)
    # A rose of runes and a star between the rings
    for k in range(12):
        a = (k + .5) * math.tau / 12 - math.pi / 2
        glyph(d, c + math.cos(a) * c * .52, c + math.sin(a) * c * .52, w * .06, 140 + k, max(2, w // 200), goldc)
    pts = [(c + math.cos(k * math.tau / 12 - math.pi / 2) * c * .3, c + math.sin(k * math.tau / 12 - math.pi / 2) * c * .3) for k in range(12)]
    for k in range(12):
        d.line([pts[k], pts[(k + 5) % 12]], fill=dark, width=3)
    return img


def clock_glow(size=512):
    """What of the clock's face glows on the ground (its numerals stand on it in gold): the hour marks, the minutes, a rose of runes."""
    w = size
    c = w / 2
    img = Image.new("L", (w, w), 0)
    d = ImageDraw.Draw(img)
    d.ellipse((c * .06, c * .06, w - c * .06, w - c * .06), outline=200, width=3)
    d.ellipse((c * .36, c * .36, w - c * .36, w - c * .36), outline=140, width=2)
    for k in range(60):
        a = k * math.tau / 60
        d.line([(c + math.cos(a) * c * .9, c + math.sin(a) * c * .9), (c + math.cos(a) * c * .86, c + math.sin(a) * c * .86)], fill=170, width=2)
    for k in range(12):
        a = k * math.tau / 12 - math.pi / 2
        d.line([(c + math.cos(a) * c * .93, c + math.sin(a) * c * .93), (c + math.cos(a) * c * .86, c + math.sin(a) * c * .86)], fill=255, width=5)
        glyph(d, c + math.cos(a + math.tau / 24) * c * .52, c + math.sin(a + math.tau / 24) * c * .52, w * .06, 140 + k, max(2, w // 200), 150)
    return glow_mask(img, 3, .9)


def tile(dark):
    """A square of the infernal board: obsidian (dark) or bone-white stone, a carved border and a sigil in its middle."""
    rgb = np.asarray(obsidian() if dark else marble(), dtype=float)[..., :3]
    if not dark:
        rgb = mix(rgb, (226, 210, 182), .35)
    lines = Image.new("L", (N, N), 0)
    d = ImageDraw.Draw(lines)
    m = N * .06
    d.rectangle((m, m, N - m, N - m), outline=255, width=6)
    d.rectangle((m * 2.2, m * 2.2, N - m * 2.2, N - m * 2.2), outline=255, width=2)
    c = N / 2
    r = N * .26
    pts = [(c + math.cos(-math.pi / 2 + k * math.tau / 5) * r, c + math.sin(-math.pi / 2 + k * math.tau / 5) * r) for k in range(5)]
    for k in range(5):
        d.line([pts[k], pts[(k + 2) % 5]], fill=255, width=3)
    d.ellipse((c - r * 1.15, c - r * 1.15, c + r * 1.15, c + r * 1.15), outline=255, width=3)
    m_ = np.asarray(lines, dtype=float) / 255
    rgb = mix(rgb, (4, 2, 6) if dark else (110, 92, 70), m_ * .8)
    rgb = mix(rgb, (255, 255, 255) if not dark else (90, 70, 120), np.clip(emboss(m_, 2.5), 0, 1) * .35)
    return image(rgb)


def tile_glow():
    """The glowing part of a board square: its sigil and border."""
    lines = Image.new("L", (N, N), 0)
    d = ImageDraw.Draw(lines)
    m = N * .06
    d.rectangle((m, m, N - m, N - m), outline=255, width=6)
    c = N / 2
    r = N * .26
    pts = [(c + math.cos(-math.pi / 2 + k * math.tau / 5) * r, c + math.sin(-math.pi / 2 + k * math.tau / 5) * r) for k in range(5)]
    for k in range(5):
        d.line([pts[k], pts[(k + 2) % 5]], fill=255, width=4)
    d.ellipse((c - r * 1.15, c - r * 1.15, c + r * 1.15, c + r * 1.15), outline=255, width=4)
    return glow_mask(lines, 3, 1.0)


MATERIALS = (("bronze", bronze), ("iron_plate", iron), ("steel", steel), ("gold_chased", gold), ("marble", marble), ("obsidian", obsidian),
             ("bone", bone), ("flesh", flesh), ("cloth", cloth), ("crimson_glass", crimson_glass), ("rune_band", rune_band), ("cracks", cracks),
             ("engraving", engraving), ("clock_face", clock_face), ("clock_glow", clock_glow), ("tile_dark", lambda: tile(True)),
             ("tile_light", lambda: tile(False)), ("tile_glow", tile_glow))


def main():
    for name, fn in MATERIALS:
        g.save(fn(), f"textures/entity/boss/m_{name}.png")


if __name__ == "__main__":
    main()
