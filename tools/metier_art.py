#!/usr/bin/env python3
"""Trades' minigame art: the five scenes painted behind the minigames (a mine gallery, a garden in cross-section, a
workbench, a smithy, an alchemist's bench), the pieces drawn over them (the boulder and its ore veins, the anvil, the
bellows, the flask, the crafting slot) and the soft sprites the effects are made of (glow, disc, ring, flame, star).

Called by metier_assets.py. Scenes are 784 x 360 (drawn at half size in game, so they stay sharp). Original art,
painted from noise and strokes.
"""
import math
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

import gen_assets as g
from boss_materials import emboss, fbm, image, mix, noise, ramp, smooth, worley

W, H = 784, 360
OUT = "textures/gui/metier/"


def _grid(w, h):
    ys, xs = np.mgrid[0:h, 0:w].astype(float)
    return xs, ys


def _blur(a, r):
    img = Image.fromarray(np.clip(a * 255, 0, 255).astype(np.uint8))
    return np.asarray(img.filter(ImageFilter.GaussianBlur(r)), dtype=float) / 255


def _mask(w, h, draw, blur=0):
    img = Image.new("L", (w, h), 0)
    draw(ImageDraw.Draw(img))
    if blur:
        img = img.filter(ImageFilter.GaussianBlur(blur))
    return np.asarray(img, dtype=float) / 255


def _glow(xs, ys, cx, cy, r):
    return np.exp(-((xs - cx) ** 2 + (ys - cy) ** 2) / (2 * r * r))


def _vignette(rgb, strength=.55):
    xs, ys = _grid(rgb.shape[1], rgb.shape[0])
    u, v = xs / rgb.shape[1] - .5, ys / rgb.shape[0] - .5
    d = np.clip(1 - (u * u * 1.6 + v * v * 2.2) * strength * 2, .25, 1)
    return rgb * d[..., None]


def _wood(w, h, seed, dark=(58, 34, 20), mid=(110, 72, 42), light=(156, 108, 64), along_x=True):
    """Wood grain running across (or down)."""
    grain = fbm(w, h, 6, 5, seed, stretch=10) if along_x else fbm(h, w, 6, 5, seed, stretch=10).T
    rings = .5 + .5 * np.sin(grain * 38)
    t = .55 * grain + .3 * rings + .15 * noise(w, h, (w // 3 + 1, h // 3 + 1), seed + 5)
    return ramp(t, [(0, dark), (.5, mid), (1, light)])


# ---------------------------------------------------------------- sprites

def soft():
    xs, ys = _grid(64, 64)
    d = np.hypot(xs - 31.5, ys - 31.5) / 32
    a = np.clip(1 - d, 0, 1) ** 2.2
    return image(np.full((64, 64, 3), 255.0), a * 255)


def disc():
    """A smooth-edged white disc (4x supersampled)."""
    big = Image.new("L", (256, 256), 0)
    ImageDraw.Draw(big).ellipse((6, 6, 249, 249), fill=255)
    a = np.asarray(big.resize((64, 64), Image.LANCZOS), dtype=float)
    return image(np.full((64, 64, 3), 255.0), a)


def ring():
    big = Image.new("L", (512, 512), 0)
    d = ImageDraw.Draw(big)
    d.ellipse((8, 8, 503, 503), fill=255)
    d.ellipse((44, 44, 467, 467), fill=0)
    a = np.asarray(big.resize((128, 128), Image.LANCZOS), dtype=float)
    return image(np.full((128, 128, 3), 255.0), a)


def flame():
    """A tongue of flame, point up, soft at its edge (tinted and drawn additively in game)."""
    xs, ys = _grid(64, 96)
    u = (xs - 31.5) / 32
    v = ys / 96
    # Width along its height: round at the foot, drawn to a point at the top
    width = np.where(v > .62, np.sqrt(np.clip(1 - ((v - .62) / .38) ** 2, 0, 1)) * .95, (v / .62) ** 1.6 * .95)
    width = np.maximum(width, .02)
    d = np.abs(u) / width
    a = np.clip(1 - d, 0, 1) ** 1.3 * np.clip(v * 4, 0, 1)
    core = np.clip(1 - d * 1.6, 0, 1) * np.clip((v - .4) * 3, 0, 1)
    a = np.clip(a * .8 + core * .5, 0, 1)
    return image(np.full((96, 64, 3), 255.0), a * 255)


def star():
    xs, ys = _grid(64, 64)
    u, v = (xs - 31.5) / 32, (ys - 31.5) / 32
    r = np.hypot(u, v)
    rays = np.exp(-np.abs(u) * 14) * np.exp(-np.abs(v) * 1.6) + np.exp(-np.abs(v) * 14) * np.exp(-np.abs(u) * 1.6)
    a = np.clip(rays + np.exp(-r * r * 30), 0, 1) * np.clip(1 - r, 0, 1)
    return image(np.full((64, 64, 3), 255.0), a * 255)


# ---------------------------------------------------------------- mining

def mine_scene():
    xs, ys = _grid(W, H)
    f1, f2 = worley(W, H, 90, 501)
    facets = emboss(np.clip(f1, 0, 1), .8)
    base = fbm(W, H, 4, 6, 502)
    t = base * .75 + facets * .12 + noise(W, H, (160, 80), 503) * .12
    rgb = ramp(t, [(0, (22, 20, 24)), (.4, (52, 48, 50)), (.7, (84, 78, 76)), (1, (122, 114, 106))])
    cracks = smooth(.035, .0, f2 - f1) * smooth(.35, .6, fbm(W, H, 3, 3, 507))
    rgb = mix(rgb, (12, 10, 12), cracks * .55)
    # Specks of ore in the wall
    speck = smooth(.86, .93, noise(W, H, (260, 120), 504)) * smooth(.5, .7, fbm(W, H, 3, 3, 505))
    rgb = mix(rgb, (196, 112, 70), speck * .7)
    # Timbering: two posts and a beam
    for x0 in (14, W - 46):
        post = _wood(32, H, 506 + x0, along_x=False)
        shade = np.linspace(.75, 1.1, 32)[None, :, None]
        rgb[:, x0:x0 + 32] = post * shade
        rgb[:, x0 - 3:x0] *= .45
        rgb[:, x0 + 32:x0 + 36] *= .5
    beam = _wood(W, 30, 510)
    rgb[0:30] = beam * np.linspace(1.1, .75, 30)[:, None, None]
    rgb[30:36] *= .45
    # A lantern's warm light at the top left, the cold dark further in
    light = _glow(xs, ys, 120, 60, 220)
    rgb = rgb * (.55 + .75 * light[..., None]) + np.array([60, 30, 6]) * light[..., None] * .6
    return image(_vignette(rgb, .7))


def boulder():
    """The boulder (rock) and its ore veins (white, tinted in game), 512 x 288."""
    w, h = 512, 288
    xs, ys = _grid(w, h)
    u, v = (xs - w / 2) / (w * .47), (ys - h / 2) / (h * .45)
    ang = np.arctan2(v, u)
    edge = 1 + .05 * np.sin(ang * 3 + 1) + .03 * np.sin(ang * 5 + 2) + .012 * np.sin(ang * 11)
    r = np.hypot(u, v) / edge
    inside = smooth(1.0, .985, r)
    # Rounded light from the top left
    nz = np.sqrt(np.clip(1 - r * r, 0, 1))
    lit = np.clip((-u * .45 - v * .6 + nz * .65), 0, 1.2)
    f1, f2 = worley(w, h, 70, 520)
    facets = emboss(np.clip(f1, 0, 1), 1.6)
    tex = fbm(w, h, 5, 5, 521)
    t = .2 + lit * .55 + facets * .1 + (tex - .5) * .25
    rgb = ramp(t, [(0, (26, 24, 26)), (.35, (64, 60, 62)), (.65, (112, 106, 102)), (.9, (160, 152, 142)), (1.1, (196, 188, 176))])
    rgb = mix(rgb, (18, 16, 18), smooth(.03, .0, f2 - f1) * .45)
    rock = image(rgb, inside * 255)
    # Ore: clusters of rounded nuggets, lit like the rock
    rnd = random.Random(522)
    veins = np.zeros((h, w))
    for _ in range(26):
        cx, cy = rnd.uniform(.15, .85) * w, rnd.uniform(.2, .8) * h
        for _ in range(rnd.randint(3, 7)):
            ox, oy, rr = cx + rnd.gauss(0, 14), cy + rnd.gauss(0, 9), rnd.uniform(5, 11)
            veins = np.maximum(veins, smooth(rr, rr * .6, np.hypot(xs - ox, ys - oy)))
    veins *= inside * smooth(.92, .8, r)
    shade = np.clip(.45 + lit * .5 + emboss(veins, 2.2) * .35, 0, 1.2)
    ore = image(np.dstack([shade * 230] * 3), veins * 255)
    return rock, ore


# ---------------------------------------------------------------- gathering

GROUND = 160


def garden_scene():
    xs, ys = _grid(W, H)
    # Sky, warm at the horizon
    sky = ramp(ys / GROUND, [(0, (110, 168, 222)), (.7, (176, 214, 236)), (1, (246, 226, 186))])
    clouds = smooth(.55, .75, fbm(W, H, 3, 5, 530, stretch=3)) * smooth(GROUND, 20, ys)
    sky = mix(sky, (252, 252, 255), clouds * .85)
    # Hills far off
    hill = GROUND - 34 - 18 * np.sin(xs / 90 + 1) - 10 * np.sin(xs / 37) - fbm(W, H, 8, 3, 531)[0][None, :] * 10
    sky = mix(sky, (120, 164, 108), smooth(hill, hill + 2, ys) * .9)
    hill2 = GROUND - 14 - 9 * np.sin(xs / 60 + 3)
    sky = mix(sky, (86, 138, 74), smooth(hill2, hill2 + 2, ys))
    # Soil in layers, pebbles
    soil_t = (ys - GROUND) / (H - GROUND) * .6 + fbm(W, H, 6, 5, 532) * .4
    soil = ramp(soil_t, [(0, (122, 84, 52)), (.4, (96, 64, 40)), (.7, (72, 48, 30)), (1, (48, 32, 22))])
    layers = .5 + .5 * np.sin((ys - GROUND) / 18 + fbm(W, H, 4, 3, 533) * 6)
    soil *= (.88 + .12 * layers)[..., None]
    rgb = np.where((ys >= GROUND)[..., None], soil, sky)
    img = image(rgb)
    d = ImageDraw.Draw(img)
    rnd = random.Random(534)
    for _ in range(70):
        x, y = rnd.uniform(0, W), rnd.uniform(GROUND + 18, H)
        rx, ry = rnd.uniform(3, 9), rnd.uniform(2, 6)
        c = rnd.randint(110, 160)
        d.ellipse((x - rx, y - ry, x + rx, y + ry), fill=(c, c - 8, c - 16))
        d.ellipse((x - rx * .6, y - ry * .8, x + rx * .2, y - ry * .1), fill=(c + 30, c + 24, c + 14))
    # Grass: a band and blades
    d.rectangle((0, GROUND - 4, W, GROUND + 8), fill=(76, 132, 52))
    for _ in range(900):
        x = rnd.uniform(0, W)
        hgt = rnd.uniform(6, 22)
        lean = rnd.uniform(-5, 5)
        c = (rnd.randint(60, 110), rnd.randint(130, 180), rnd.randint(50, 80))
        d.line([(x, GROUND + 6), (x + lean, GROUND + 6 - hgt)], fill=c, width=2)
    rgb = np.asarray(img, dtype=float)[..., :3]
    # A soft shadow under the grass
    sh = smooth(GROUND + 30, GROUND + 6, ys) * (ys > GROUND + 6)
    rgb = rgb * (1 - sh[..., None] * .35)
    return image(_vignette(rgb, .45))


# ---------------------------------------------------------------- crafting

def bench_scene():
    xs, ys = _grid(W, H)
    rgb = np.zeros((H, W, 3))
    plank = 60
    for i, y0 in enumerate(range(0, H, plank)):
        wood = _wood(W, plank, 540 + i, dark=(64, 40, 22), mid=(122, 82, 48), light=(170, 120, 72))
        tone = 1 + (random.Random(541 + i).random() - .5) * .18
        rgb[y0:y0 + plank] = wood[:min(plank, H - y0)] * tone
        rgb[y0:y0 + 3] *= .4
        rgb[y0 + 3:y0 + 5] *= 1.15
    img = image(rgb)
    d = ImageDraw.Draw(img)
    rnd = random.Random(542)
    for y0 in range(0, H, plank):
        for x in (40, W // 2 - 10, W - 50):
            cy = y0 + plank // 2
            d.ellipse((x - 4, cy - 4, x + 4, cy + 4), fill=(52, 48, 50))
            d.ellipse((x - 2, cy - 3, x + 1, cy - 1), fill=(150, 146, 140))
    # Sawdust and shavings
    for _ in range(160):
        x, y = rnd.uniform(0, W), rnd.uniform(0, H)
        c = rnd.randint(190, 230)
        d.point((x, y), fill=(c, c - 30, c - 80))
    for _ in range(14):
        x, y = rnd.uniform(0, W), rnd.uniform(0, H)
        d.arc((x, y, x + rnd.uniform(14, 30), y + rnd.uniform(8, 16)), rnd.uniform(0, 180), rnd.uniform(200, 340), fill=(214, 172, 116), width=3)
    rgb = np.asarray(img, dtype=float)[..., :3]
    light = _glow(xs, ys, W * .5, H * .35, 330)
    rgb = rgb * (.6 + .55 * light[..., None])
    return image(_vignette(rgb, .6))


def slot():
    """A crafting slot carved in the bench: dark inset, its lower and right edges catching the light, iron corners."""
    s = 72
    xs, ys = _grid(s, s)
    rgb = np.zeros((s, s, 3)) + (34, 22, 14)
    inner = fbm(s, s, 4, 3, 550)
    rgb = rgb + (inner[..., None] - .5) * 18
    top = smooth(8, 2, ys) * (ys < 8)
    left = smooth(8, 2, xs) * (xs < 8)
    bottom = smooth(s - 8, s - 2, ys) * (ys > s - 8)
    right = smooth(s - 8, s - 2, xs) * (xs > s - 8)
    rgb = mix(rgb, (14, 8, 4), np.maximum(top, left) * .8)
    rgb = mix(rgb, (176, 128, 80), np.maximum(bottom, right) * .7)
    # Rounded corners and iron corner plates
    a = np.ones((s, s)) * 255
    corner = 6
    for cx, cy in ((corner, corner), (s - corner - 1, corner), (corner, s - corner - 1), (s - corner - 1, s - corner - 1)):
        box = (np.abs(xs - cx) >= 0) & ((xs - cx) * np.sign(cx - s / 2) >= 0) & ((ys - cy) * np.sign(cy - s / 2) >= 0)
        out = box & (np.hypot(xs - cx, ys - cy) > corner)
        a = np.where(out, 0, a)
    img = image(rgb, a)
    d = ImageDraw.Draw(img)
    for x, y in ((3, 3), (s - 13, 3), (3, s - 13), (s - 13, s - 13)):
        d.rectangle((x, y, x + 9, y + 9), fill=(70, 70, 78))
        d.rectangle((x + 1, y + 1, x + 8, y + 2), fill=(140, 140, 150))
        d.ellipse((x + 3, y + 3, x + 6, y + 6), fill=(40, 40, 46))
    return img


# ---------------------------------------------------------------- forging

HEARTH = (40, 150, 220, 320)


def smithy_scene():
    xs, ys = _grid(W, H)
    # Stone bricks
    bh, bw = 30, 70
    row = (ys // bh).astype(int)
    off = np.where(row % 2 == 0, 0, bw // 2)
    col = ((xs + off) // bw).astype(int)
    rnd = np.random.default_rng(560)
    tone = rnd.random((H // bh + 2, W // bw + 3))[row, col]
    bx, by = (xs + off) % bw, ys % bh
    mortar = np.maximum(smooth(3, 0, bx) + smooth(bw - 3, bw, bx), smooth(3, 0, by) + smooth(bh - 3, bh, by))
    tex = fbm(W, H, 8, 4, 561)
    t = .3 + tone * .35 + (tex - .5) * .4
    rgb = ramp(t, [(0, (34, 30, 32)), (.5, (70, 62, 60)), (1, (112, 100, 94))])
    rgb = mix(rgb, (18, 15, 16), np.clip(mortar, 0, 1) * .85)
    # Floor
    floor = ys > H - 34
    rgb = np.where(floor[..., None], ramp(fbm(W, H, 10, 3, 562), [(0, (30, 26, 26)), (1, (64, 56, 52))]), rgb)
    rgb = mix(rgb, (10, 8, 8), smooth(H - 30, H - 36, ys) * floor * .8)
    # The hearth: a dark mouth with embers, a hood above
    x0, y0, x1, y1 = HEARTH
    img = image(rgb)
    d = ImageDraw.Draw(img)
    d.polygon([(x0 - 24, y0 - 40), (x1 + 24, y0 - 40), (x1 + 8, y0 - 6), (x0 - 8, y0 - 6)], fill=(46, 40, 40))
    d.rectangle((x0 - 8, y0 - 8, x1 + 8, y0 - 2), fill=(26, 22, 22))
    d.rectangle((x0 + 30, 0, x1 - 30, y0 - 40), fill=(40, 34, 34))
    d.rectangle((x0, y0, x1, y1), fill=(16, 10, 8))
    d.rectangle((x0 - 10, y1, x1 + 10, y1 + 14), fill=(58, 50, 48))
    rgb = np.asarray(img, dtype=float)[..., :3]
    embers = smooth(.45, .8, fbm(W, H, 18, 4, 563)) * ((xs > x0) & (xs < x1) & (ys > y1 - 34) & (ys < y1))
    rgb = mix(rgb, (255, 140, 40), embers * .9)
    rgb = mix(rgb, (255, 220, 120), embers * smooth(.75, .95, noise(W, H, (90, 30), 564)) * .8)
    # Tools hanging on the right
    img = image(rgb)
    d = ImageDraw.Draw(img)
    for i, x in enumerate((W - 170, W - 130, W - 95)):
        d.line([(x, 70), (x, 76)], fill=(30, 26, 26), width=3)
        if i == 0:
            d.line([(x, 76), (x, 180)], fill=(38, 32, 30), width=6)
            d.rectangle((x - 18, 176, x + 18, 196), fill=(44, 40, 42))
        elif i == 1:
            d.line([(x - 6, 76), (x - 10, 200)], fill=(40, 38, 40), width=4)
            d.line([(x + 6, 76), (x + 10, 200)], fill=(40, 38, 40), width=4)
        else:
            d.line([(x, 76), (x, 160)], fill=(60, 40, 26), width=6)
            d.polygon([(x - 14, 160), (x + 14, 160), (x + 8, 190), (x - 8, 190)], fill=(44, 40, 42))
    rgb = np.asarray(img, dtype=float)[..., :3]
    # Firelight from the hearth
    light = _glow(xs, ys, (x0 + x1) / 2, y1 - 30, 260)
    rgb = rgb * (.35 + .9 * light[..., None]) + np.array([90, 36, 0]) * light[..., None] * .55
    return image(_vignette(rgb, .55))


def anvil():
    w, h = 300, 170
    def shape(d):
        d.polygon([(10, 34), (70, 30), (250, 30), (292, 22), (292, 40), (250, 62), (210, 66), (196, 104), (232, 150), (232, 166),
                   (68, 166), (68, 150), (104, 104), (96, 66), (60, 62), (20, 52)], fill=255)
    m = _mask(w, h, shape)
    m2 = Image.fromarray((m * 255).astype(np.uint8)).resize((w * 2, h * 2)).filter(ImageFilter.GaussianBlur(1))
    m = np.asarray(m2.resize((w, h), Image.LANCZOS), dtype=float) / 255
    xs, ys = _grid(w, h)
    brushed = fbm(w, h, 12, 4, 570, stretch=6)
    t = .35 + .4 * smooth(170, 20, ys) + (brushed - .5) * .2 + emboss(m, 3) * .3
    rgb = ramp(t, [(0, (16, 16, 20)), (.4, (44, 44, 52)), (.7, (84, 84, 96)), (1, (150, 150, 162))])
    # The face, polished
    face = (ys >= 28) & (ys < 36) & (xs > 20) & (xs < 290)
    rgb = mix(rgb, (196, 198, 210), face * .7)
    return image(rgb, m * 255)


def bellows():
    w, h = 160, 110
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Leather body
    d.polygon([(10, 20), (110, 40), (110, 70), (10, 90)], fill=(96, 56, 32, 255))
    for i in range(5):
        x = 20 + i * 18
        d.line([(x, 22 + i * 4), (x, 88 - i * 4)], fill=(70, 38, 20, 255), width=3)
    # Boards
    d.polygon([(4, 12), (112, 36), (112, 42), (4, 22)], fill=(132, 92, 52, 255))
    d.polygon([(4, 88), (112, 68), (112, 74), (4, 98)], fill=(110, 76, 42, 255))
    d.rectangle((0, 8, 8, 102), fill=(90, 60, 34, 255))
    # Nozzle
    d.polygon([(110, 46), (156, 52), (156, 58), (110, 64)], fill=(120, 120, 130, 255))
    d.line([(112, 48), (154, 53)], fill=(190, 190, 200, 255), width=2)
    return img.filter(ImageFilter.SMOOTH)


# ---------------------------------------------------------------- distilling

def lab_scene():
    xs, ys = _grid(W, H)
    # Dark panelled wall
    panel = _wood(W, H, 580, dark=(30, 20, 24), mid=(56, 38, 44), light=(84, 58, 64), along_x=False)
    seams = smooth(3, 0, (xs % 98)) + smooth(95, 98, xs % 98)
    rgb = mix(panel, (14, 10, 12), np.clip(seams, 0, 1) * .8)
    # Shelves with bottles
    img = image(rgb)
    d = ImageDraw.Draw(img)
    rnd = random.Random(581)
    colors = [(170, 70, 200), (60, 160, 190), (200, 80, 80), (90, 190, 110), (220, 170, 60)]
    for sy in (92, 186):
        d.rectangle((300, sy, W - 20, sy + 10), fill=(70, 46, 30))
        d.rectangle((300, sy + 10, W - 20, sy + 14), fill=(34, 22, 16))
        x = 316
        while x < W - 50:
            bw, bh = rnd.randint(16, 30), rnd.randint(28, 62)
            c = rnd.choice(colors)
            dark = tuple(int(v * .45) for v in c)
            if rnd.random() < .5:
                d.ellipse((x, sy - bh * .7, x + bw + 6, sy), fill=dark)
                d.rectangle((x + bw / 2 - 2, sy - bh, x + bw / 2 + 8, sy - bh * .6), fill=dark)
            else:
                d.rectangle((x, sy - bh, x + bw, sy), fill=dark)
                d.rectangle((x + bw / 2 - 4, sy - bh - 8, x + bw / 2 + 4, sy - bh), fill=dark)
            d.rectangle((x + 3, sy - bh * .55, x + 5, sy - 6), fill=tuple(min(255, int(v * .9)) for v in c))
            d.rectangle((x + bw / 2 - 5, sy - bh - 12, x + bw / 2 + 5, sy - bh - 7), fill=(110, 80, 50))
            x += bw + rnd.randint(8, 20)
    # Stone bench top
    rgb = np.asarray(img, dtype=float)[..., :3]
    top = ys > H - 70
    stone = ramp(fbm(W, H, 10, 5, 582), [(0, (46, 42, 50)), (.6, (82, 76, 88)), (1, (120, 112, 124))])
    rgb = np.where(top[..., None], stone, rgb)
    rgb = mix(rgb, (150, 140, 150), smooth(H - 72, H - 68, ys) * smooth(H - 64, H - 68, ys) * .9)
    rgb = mix(rgb, (8, 6, 8), smooth(H - 70, H - 80, ys) * (~top) * .5)
    # Candlelight and a violet glow
    light = _glow(xs, ys, 200, 240, 240)
    rgb = rgb * (.45 + .8 * light[..., None]) + np.array([60, 20, 80]) * light[..., None] * .4
    return image(_vignette(rgb, .6))


def flask():
    """A round-bottomed flask, 160 x 200: clear glass, its rim and highlights; inside see-through."""
    w, h = 160, 200
    s = 4
    big = Image.new("L", (w * s, h * s), 0)
    d = ImageDraw.Draw(big)
    cx, cy, r = 80 * s, 128 * s, 66 * s
    d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=255)
    d.rectangle((cx - 14 * s, 8 * s, cx + 14 * s, cy - 50 * s), fill=255)
    outer = np.asarray(big.resize((w, h), Image.LANCZOS), dtype=float) / 255
    big2 = Image.new("L", (w * s, h * s), 0)
    d2 = ImageDraw.Draw(big2)
    ri = r - 5 * s
    d2.ellipse((cx - ri, cy - ri, cx + ri, cy + ri), fill=255)
    d2.rectangle((cx - 9 * s, 4 * s, cx + 9 * s, cy - 50 * s), fill=255)
    inner = np.asarray(big2.resize((w, h), Image.LANCZOS), dtype=float) / 255
    rim = np.clip(outer - inner, 0, 1)
    xs, ys = _grid(w, h)
    # Highlights: a long arc at the top left, a dot
    hl = _mask(w, h, lambda dd: dd.arc((30, 78, 130, 178), 190, 250, fill=255, width=6), blur=1.2)
    hl += _mask(w, h, lambda dd: dd.ellipse((112, 110, 120, 120), fill=255), blur=1.5)
    hl += _mask(w, h, lambda dd: dd.line([(72, 14), (72, 70)], fill=255, width=3), blur=1)
    a = np.clip(rim * 230 + inner * 28 + hl * 220, 0, 255)
    rgb = np.dstack([np.full((h, w), 225.0), np.full((h, w), 240.0), np.full((h, w), 255.0)])
    # The lip
    lip = _mask(w, h, lambda dd: dd.rectangle((62, 4, 98, 12), fill=255), blur=.6)
    a = np.maximum(a, lip * 240)
    return image(rgb, a)


def flask_fill():
    """The flask's inside (white), for its liquid."""
    w, h = 160, 200
    s = 4
    big = Image.new("L", (w * s, h * s), 0)
    d = ImageDraw.Draw(big)
    cx, cy, r = 80 * s, 128 * s, 61 * s
    d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=255)
    d.rectangle((cx - 9 * s, 4 * s, cx + 9 * s, cy - 50 * s), fill=255)
    a = np.asarray(big.resize((w, h), Image.LANCZOS), dtype=float)
    xs, ys = _grid(w, h)
    shade = np.clip(1.05 - np.hypot(xs - 64, ys - 110) / 120, .55, 1)
    return image(np.dstack([shade * 255] * 3), a)


def main():
    g.save(soft(), OUT + "soft.png")
    g.save(disc(), OUT + "disc.png")
    g.save(ring(), OUT + "ring.png")
    g.save(flame(), OUT + "flame.png")
    g.save(star(), OUT + "star.png")
    g.save(mine_scene(), OUT + "bg_mining.png")
    rock, ore = boulder()
    g.save(rock, OUT + "boulder.png")
    g.save(ore, OUT + "boulder_ore.png")
    g.save(garden_scene(), OUT + "bg_harvest.png")
    g.save(bench_scene(), OUT + "bg_assembly.png")
    g.save(slot(), OUT + "slot.png")
    g.save(smithy_scene(), OUT + "bg_forge.png")
    g.save(anvil(), OUT + "anvil.png")
    g.save(bellows(), OUT + "bellows.png")
    g.save(lab_scene(), OUT + "bg_distill.png")
    g.save(flask(), OUT + "flask.png")
    g.save(flask_fill(), OUT + "flask_fill.png")


if __name__ == "__main__":
    main()
