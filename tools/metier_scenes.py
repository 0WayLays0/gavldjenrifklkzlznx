#!/usr/bin/env python3
"""Scenes painted behind the trades' minigames, 784 x 360 each (drawn at about half size in game): a face of bare rock
(wherever the ore is), a meadow at golden hour over its soil in cross-section, a carpenter's workshop in
the sun and an infirmary. (The workbench, smithy and laboratory are painted in metier_art.py.)

Called by metier_art.py. Each scene keeps clear the places its minigame draws on (see the minigames' constants, in
window units: scene x = (x - 8) * 1.94, scene y = (y - 44) * 1.96). Original art, painted from noise, masks and strokes.
"""
import math
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

from boss_materials import emboss, fbm, image, mix, noise, ramp, smooth, worley

W, H = 784, 360


# ---------------------------------------------------------------- tools

def grid(w=W, h=H):
    ys, xs = np.mgrid[0:h, 0:w].astype(float)
    return xs, ys




def blur(a, r):
    img = Image.fromarray(np.clip(a * 255, 0, 255).astype(np.uint8))
    return np.asarray(img.filter(ImageFilter.GaussianBlur(r)), dtype=float) / 255


def glow(cx, cy, r, w=W, h=H):
    xs, ys = grid(w, h)
    return np.exp(-((xs - cx) ** 2 + (ys - cy) ** 2) / (2 * r * r))


def lit(rgb, light, ambient, color, strength):
    """Lights {rgb}: dark at {ambient}, up to full where {light} is 1, tinted {color}."""
    return rgb * (ambient + (1 - ambient) * light[..., None]) + np.asarray(color, float) * light[..., None] * strength


def vignette(rgb, strength=.5):
    xs, ys = grid(rgb.shape[1], rgb.shape[0])
    u, v = xs / rgb.shape[1] - .5, ys / rgb.shape[0] - .5
    d = np.clip(1 - (u * u * 1.4 + v * v * 2.2) * strength * 2, .3, 1)
    return rgb * d[..., None]


def grain(rgb, amount=6, seed=1):
    """A film of fine noise, so flat paint does not band."""
    rnd = np.random.default_rng(seed)
    return rgb + (rnd.random(rgb.shape[:2])[..., None] - .5) * amount


def wood(w, h, seed, dark=(58, 34, 20), mid=(110, 72, 42), light=(156, 108, 64), along_x=True, rings=38):
    g = fbm(w, h, 6, 5, seed, stretch=10) if along_x else fbm(h, w, 6, 5, seed, stretch=10).T
    r = .5 + .5 * np.sin(g * rings)
    t = .55 * g + .3 * r + .15 * noise(w, h, (w // 3 + 1, h // 3 + 1), seed + 5)
    return ramp(t, [(0, dark), (.5, mid), (1, light)])










def rgba_layer(draw, w=W, h=H, ss=2, blur_r=0):
    """An RGBA layer drawn with ImageDraw at {ss} times, brought down to size: (rgb, alpha 0..1)."""
    img = Image.new("RGBA", (w * ss, h * ss), (0, 0, 0, 0))
    draw(ImageDraw.Draw(img), ss)
    img = img.resize((w, h), Image.LANCZOS)
    if blur_r:
        img = img.filter(ImageFilter.GaussianBlur(blur_r))
    a = np.asarray(img, dtype=float)
    return a[..., :3], a[..., 3] / 255


def over(rgb, layer):
    col, a = layer
    return rgb * (1 - a[..., None]) + col * a[..., None]


def finish(rgb, vig=.5, seed=9):
    return image(grain(vignette(rgb, vig), 5, seed))


# ---------------------------------------------------------------- the rock face

def rock_face():
    """A face of bare rock seen up close, wherever the ore is: in a cave, a cliff or a quarry. Layers of stone, their
    ledges catching the light, fractures, a little moss, loose stones at its foot."""
    xs, ys = grid()
    # Layers of stone, tilted a little and warped, each its own shade
    tilt = ys + xs * .08 + 30 * fbm(W, H, 3, 4, 701)
    layer = np.floor(tilt / 46)
    tone = (np.sin(layer * 12.9898) * 43758.5453) % 1
    within = (tilt % 46) / 46
    f1, f2 = worley(W, H, 46, 702)
    facets = emboss(np.clip(f1, 0, 1), 1.4)
    tex = fbm(W, H, 6, 6, 703)
    t = .25 + tone * .42 + facets * .12 + (tex - .5) * .6
    rgb = ramp(t, [(0, (62, 58, 60)), (.3, (98, 92, 90)), (.55, (134, 126, 118)), (.8, (170, 160, 146)), (1, (198, 188, 172))])
    rgb = mix(rgb, (128, 100, 76), smooth(.6, .9, tone) * .22)
    # Each layer's top edge is a ledge, lit; its underside in shadow
    ledge = smooth(.1, 0, within) * smooth(.3, .6, fbm(W, H, 5, 3, 704))
    under = smooth(.86, 1, within) * smooth(.3, .6, fbm(W, H, 5, 3, 704))
    rgb = rgb * (1 + .28 * ledge[..., None] - .3 * under[..., None])
    # Cracks running through it
    cracks = np.zeros((H, W))
    # Moss in the hollows near the top, damp darker streaks below it
    moss = smooth(.62, .78, fbm(W, H, 5, 4, 706)) * smooth(160, 0, ys) * (under + cracks * .5 + .2)
    rgb = mix(rgb, (74, 104, 52), np.clip(moss, 0, 1) * .55)
    damp = smooth(.55, .75, fbm(W, H, 12, 3, 707, stretch=.15)) * smooth(0, H, ys)
    rgb = mix(rgb, (36, 34, 36), damp * .25)
    rnd = random.Random(708)

    def ore(d, s):
        # Fractures: joints across the layers, zigzagging sharply, widest in the middle and closing at both ends; a dark
        # gap, its shadow below, its lower lip catching the light
        for k in range(6):
            x, y = rnd.uniform(40, W - 40), rnd.uniform(10, 90)
            base = rnd.uniform(1.25, 1.85)
            pts = [(x, y)]
            n = rnd.randint(7, 12)
            for i in range(n):
                a = base + (.55 if i % 2 == 0 else -.55) * rnd.uniform(.4, 1)
                step = rnd.uniform(10, 20)
                x, y = x + math.cos(a) * step, y + math.sin(a) * step
                pts.append((x, y))
            for i in range(len(pts) - 1):
                t = i / (len(pts) - 2)
                w = .6 + 3.2 * math.sin(math.pi * t) ** .7
                (x0, y0), (x1, y1) = pts[i], pts[i + 1]
                d.line(((x0 + 1.5) * s, (y0 + 2) * s, (x1 + 1.5) * s, (y1 + 2) * s), fill=(10, 8, 10, 70), width=int((w + 3) * s))
                d.line((x0 * s, (y0 + w * .6) * s, x1 * s, (y1 + w * .6) * s), fill=(206, 198, 184, 110), width=max(1, int(1.1 * s)))
                d.line((x0 * s, y0 * s, x1 * s, y1 * s), fill=(16, 14, 16, 245), width=max(1, int(w * s)))
        # Loose stones and grit at the foot of the face
        for _ in range(90):
            x, y = rnd.uniform(0, W), rnd.uniform(H - 46, H + 6)
            r = rnd.uniform(2, 9)
            c = rnd.randint(80, 140)
            d.ellipse(((x - r) * s, (y - r * .7) * s, (x + r) * s, (y + r * .7) * s), fill=(c - 36, c - 38, c - 40, 255))
            d.ellipse(((x - r * .85) * s, (y - r * .7) * s, (x + r * .55) * s, (y + r * .15) * s), fill=(c, c - 4, c - 10, 255))
            d.ellipse(((x - r * .5) * s, (y - r * .6) * s, (x - r * .1) * s, (y - r * .3) * s), fill=(c + 46, c + 42, c + 36, 255))
        for _ in range(500):
            x, y = rnd.uniform(0, W), rnd.uniform(H - 40, H)
            c = rnd.randint(70, 150)
            d.point((x * s, y * s), fill=(c, c - 4, c - 8, 220))
    rgb = over(rgb, rgba_layer(ore))
    rgb = mix(rgb, (30, 26, 26), smooth(H - 60, H - 30, ys) * .25)
    # Soft light from the top left, the face falling into shade toward the bottom right
    light = np.clip(glow(150, 40, 420) * 1.1, 0, 1.1)
    rgb = lit(rgb, light, .62, (40, 32, 18), .2)
    return finish(rgb, .45, 709)


# ---------------------------------------------------------------- the meadow

GROUND = 160


def meadow():
    xs, ys = grid()
    # Sky at golden hour: deep blue above, warm at the horizon, the sun low on the right
    sky = ramp(ys / GROUND, [(0, (40, 104, 200)), (.45, (104, 166, 236)), (.78, (226, 214, 190)), (1, (255, 214, 150))])
    sun = glow(640, 64, 70)
    sky = sky + np.array([255, 200, 120]) * sun[..., None] * .55 + np.array([255, 250, 230]) * glow(640, 64, 13)[..., None] * 1.4
    puff = fbm(W, H, 3, 6, 620, stretch=3.4)
    puff = (puff - puff.min()) / (puff.max() - puff.min())
    cloud = smooth(.55, .78, puff) * smooth(GROUND - 30, 10, ys)
    belly = smooth(.55, .62, puff) - smooth(.62, .8, puff)
    sky = mix(sky, (252, 244, 236), cloud * .9)
    sky = mix(sky, (214, 190, 196), np.clip(belly, 0, 1) * cloud * .5)
    rim = np.clip(cloud - np.roll(np.roll(cloud, 2, 1), 3, 0), 0, 1)
    rim[:4] = 0
    sky = sky + np.array([255, 196, 130]) * (rim * glow(640, 64, 260) * 2)[..., None] * .5
    # Mountains far off, hazy blue; hills nearer, green; trees on them
    ridge = GROUND - 62 - 26 * fbm(W, 1, 6, 4, 621)[0] - 12 * np.sin(xs[0] / 120)
    rgb = mix(sky, (150, 168, 196), smooth(ridge[None, :], ridge[None, :] + 2, ys) * .85)
    snow = smooth(ridge[None, :] + 8, ridge[None, :] + 2, ys) * smooth(ridge[None, :], ridge[None, :] + 2, ys) * (ridge[None, :] < GROUND - 76)
    rgb = mix(rgb, (236, 236, 246), snow * .6)
    hill = GROUND - 32 - 16 * np.sin(xs[0] / 90 + 1) - 9 * np.sin(xs[0] / 37) - 8 * fbm(W, 1, 8, 3, 622)[0]
    hill_mask = smooth(hill[None, :], hill[None, :] + 2, ys)
    hill_col = ramp(np.clip((ys - hill[None, :]) / 30, 0, 1), [(0, (132, 176, 96)), (1, (96, 144, 72))])
    rgb = mix(rgb, hill_col, hill_mask)
    rnd = random.Random(623)

    def trees(d, s):
        for _ in range(26):
            x = rnd.uniform(0, W)
            base = hill[int(x) % W] + rnd.uniform(4, 16)
            r = rnd.uniform(7, 15)
            d.rectangle(((x - 1.4) * s, (base - r * .5) * s, (x + 1.4) * s, (base + 2) * s), fill=(70, 50, 34, 255))
            for k in range(4):
                ox, oy, rr = x + rnd.uniform(-r * .5, r * .5), base - r - rnd.uniform(0, r * .6), r * rnd.uniform(.55, .85)
                d.ellipse(((ox - rr) * s, (oy - rr) * s, (ox + rr) * s, (oy + rr) * s), fill=(58, 104, 52, 255))
                d.ellipse(((ox - rr * .7 + rr * .25) * s, (oy - rr * .8) * s, (ox + rr * .6) * s, (oy + rr * .2) * s), fill=(110, 158, 78, 255))
    rgb = over(rgb, rgba_layer(trees))
    # The near meadow: a band of grass, flowers, a fence
    near = GROUND - 14 + 3 * np.sin(xs[0] / 50)
    rgb = mix(rgb, ramp(np.clip((ys - near[None, :]) / 14, 0, 1), [(0, (104, 160, 64)), (1, (70, 124, 44))]),
              smooth(near[None, :], near[None, :] + 2, ys))

    def fence(d, s):
        for x0, x1 in ((0, 210), (560, W)):
            for x in range(x0 + 10, x1, 46):
                d.rectangle(((x - 3) * s, (GROUND - 40) * s, (x + 3) * s, (GROUND + 2) * s), fill=(110, 82, 56, 255))
                d.rectangle(((x - 3) * s, (GROUND - 40) * s, (x - 1) * s, (GROUND + 2) * s), fill=(150, 116, 80, 255))
            for y in (GROUND - 34, GROUND - 20):
                d.rectangle((x0 * s, y * s, x1 * s, (y + 4) * s), fill=(124, 94, 64, 255))
                d.rectangle((x0 * s, y * s, x1 * s, (y + 1) * s), fill=(170, 134, 96, 255))
        for _ in range(1400):
            x = rnd.uniform(0, W)
            h = rnd.uniform(5, 20)
            lean = rnd.uniform(-5, 5)
            c = (rnd.randint(56, 110), rnd.randint(124, 184), rnd.randint(44, 80), 255)
            d.line((x * s, (GROUND + 4) * s, (x + lean) * s, (GROUND + 4 - h) * s), fill=c, width=int(1.6 * s))
        for _ in range(130):
            x, y = rnd.uniform(0, W), GROUND - rnd.uniform(2, 14)
            c = rnd.choice([(230, 60, 50), (250, 210, 70), (250, 250, 240), (170, 110, 220), (250, 150, 190)])
            r = rnd.uniform(1.4, 2.6)
            d.ellipse(((x - r) * s, (y - r) * s, (x + r) * s, (y + r) * s), fill=c + (255,))
            d.ellipse(((x - r * .35) * s, (y - r * .35) * s, (x + r * .35) * s, (y + r * .35) * s), fill=(250, 220, 110, 255))
    rgb = over(rgb, rgba_layer(fence))
    # The soil in cross-section: rich topsoil with roots, clay, stony earth, bedrock
    depth = (ys - GROUND) / (H - GROUND)
    wave = 10 * fbm(W, H, 4, 3, 624) + 6 * np.sin(xs / 70)
    layer = np.clip((ys - GROUND + wave * (ys > GROUND)) / (H - GROUND), 0, 1)
    soil = ramp(layer, [(0, (78, 52, 34)), (.28, (64, 42, 28)), (.36, (138, 92, 56)), (.55, (118, 78, 48)), (.62, (90, 76, 66)),
                        (.85, (74, 64, 58)), (1, (52, 46, 44))])
    soil = soil * (.86 + .28 * fbm(W, H, 18, 4, 625))[..., None]
    rgb = np.where((ys >= GROUND)[..., None], soil, rgb)

    def under(d, s):
        # Roots hanging from the grass, finer as they go
        for _ in range(60):
            x, y = rnd.uniform(0, W), GROUND + 4
            w = rnd.uniform(1.2, 2.6)
            for k in range(rnd.randint(4, 9)):
                nx, ny = x + rnd.uniform(-8, 8), y + rnd.uniform(5, 11)
                d.line((x * s, y * s, nx * s, ny * s), fill=(176, 140, 96, 200), width=max(1, int(w * s)))
                x, y, w = nx, ny, w * .8
        # Pebbles and stones, lit from above
        for _ in range(130):
            x, y = rnd.uniform(0, W), rnd.uniform(GROUND + 40, H + 4)
            r = rnd.uniform(2, 3 + 12 * (y - GROUND) / (H - GROUND))
            c = rnd.randint(96, 150)
            d.ellipse(((x - r) * s, (y - r * .7) * s, (x + r) * s, (y + r * .7) * s), fill=(c - 40, c - 44, c - 48, 255))
            d.ellipse(((x - r * .85) * s, (y - r * .7) * s, (x + r * .6) * s, (y + r * .2) * s), fill=(c, c - 6, c - 14, 255))
            d.ellipse(((x - r * .5) * s, (y - r * .6) * s, (x - r * .05) * s, (y - r * .25) * s), fill=(c + 50, c + 44, c + 36, 255))
        # A few white threads of fungus near the top
        for _ in range(30):
            x, y = rnd.uniform(0, W), rnd.uniform(GROUND + 10, GROUND + 50)
            pts = [(x * s, y * s)]
            for k in range(6):
                x, y = x + rnd.uniform(-6, 6), y + rnd.uniform(-3, 4)
                pts.append((x * s, y * s))
            d.line(pts, fill=(236, 228, 210, 90), width=max(1, int(.8 * s)))
    rgb = over(rgb, rgba_layer(under))
    # The grass's shadow on the soil; the sun's warmth over everything above
    sh = smooth(GROUND + 34, GROUND + 4, ys) * (ys > GROUND + 4)
    rgb = rgb * (1 - sh[..., None] * .4)
    rgb = rgb + np.array([40, 24, 0]) * (smooth(GROUND, 0, ys) * glow(640, 64, 400) * .6)[..., None]
    return finish(rgb, .42, 626)


# ---------------------------------------------------------------- the carpenter's workshop

def workshop():
    xs, ys = grid()
    # Wall of upright boards, warm, a floor of planks below
    rgb = np.zeros((H, W, 3))
    bw = 52
    for i, x0 in enumerate(range(0, W, bw)):
        board = wood(bw, H, 660 + i, dark=(70, 46, 28), mid=(120, 82, 50), light=(160, 116, 74), along_x=False, rings=24)
        w = min(bw, W - x0)
        rgb[:, x0:x0 + w] = board[:, :w] * (1 + (random.Random(661 + i).random() - .5) * .14)
        rgb[:, x0:x0 + 2] *= .4
    floor_y = 292
    floor = ramp(np.clip((ys - floor_y) / (H - floor_y), 0, 1), [(0, (112, 78, 48)), (1, (150, 108, 68))])
    fl = wood(W, H, 662, dark=(80, 54, 32), mid=(124, 88, 54), light=(166, 122, 78), rings=20)
    rgb = np.where((ys > floor_y)[..., None], fl * .6 + floor * .4, rgb)
    rgb = mix(rgb, (30, 20, 12), smooth(floor_y + 8, floor_y, ys) * (ys > floor_y) * .6)
    rnd = random.Random(663)

    def build(d, s):
        # A window on the left, the sun coming in
        wx, wy, ww, wh = 40, 30, 150, 120
        d.rectangle(((wx - 8) * s, (wy - 8) * s, (wx + ww + 8) * s, (wy + wh + 8) * s), fill=(90, 62, 38, 255))
        d.rectangle((wx * s, wy * s, (wx + ww) * s, (wy + wh) * s), fill=(206, 230, 246, 255))
        d.polygon([(wx * s, (wy + wh) * s), ((wx + ww) * s, (wy + wh * .55) * s), ((wx + ww) * s, (wy + wh) * s)], fill=(150, 196, 120, 255))
        for k in (1, 2):
            d.rectangle(((wx + ww * k / 3 - 2) * s, wy * s, (wx + ww * k / 3 + 2) * s, (wy + wh) * s), fill=(90, 62, 38, 255))
        d.rectangle((wx * s, (wy + wh / 2 - 2) * s, (wx + ww) * s, (wy + wh / 2 + 2) * s), fill=(90, 62, 38, 255))
        # A rack of tools along the wall: saws, a hammer, chisels, a mallet, a square
        d.rectangle((250 * s, 56 * s, 740 * s, 66 * s), fill=(84, 56, 34, 255))
        d.rectangle((250 * s, 56 * s, 740 * s, 58 * s), fill=(140, 100, 64, 255))
        for k, x in enumerate((280, 380)):
            # A hand saw hanging by its handle
            d.polygon([(x * s, 70 * s), ((x + 70) * s, 76 * s), ((x + 70) * s, 92 * s), (x * s, 160 * s)], fill=(186, 190, 198, 255))
            d.line((x * s, 160 * s, (x + 70) * s, 92 * s), fill=(110, 112, 120, 255), width=int(2 * s))
            for t in range(16):
                px = x + 70 * t / 16
                py = 160 - 68 * t / 16
                d.polygon([(px * s, py * s), ((px + 3) * s, (py - 2) * s), ((px + 1) * s, (py + 3) * s)], fill=(140, 142, 150, 255))
            d.rounded_rectangle(((x + 58) * s, 62 * s, (x + 90) * s, 96 * s), radius=8 * s, fill=(110, 60, 30, 255))
            d.rounded_rectangle(((x + 66) * s, 70 * s, (x + 82) * s, 88 * s), radius=5 * s, fill=(84, 56, 34, 255))
        hx = 488
        d.line((hx * s, 70 * s, hx * s, 150 * s), fill=(150, 104, 62, 255), width=int(6 * s))
        d.rectangle(((hx - 20) * s, 66 * s, (hx + 14) * s, 80 * s), fill=(70, 70, 78, 255))
        d.rectangle(((hx - 20) * s, 66 * s, (hx + 14) * s, 69 * s), fill=(160, 160, 170, 255))
        for k in range(4):
            cx = 530 + k * 18
            d.rectangle(((cx - 3) * s, 66 * s, (cx + 3) * s, 100 * s), fill=(150, 100, 56, 255))
            d.rectangle(((cx - 2) * s, 100 * s, (cx + 2) * s, (124 + k * 6) * s), fill=(176, 180, 190, 255))
        d.rectangle((616 * s, 66 * s, 650 * s, 90 * s), fill=(150, 108, 64, 255))
        d.line((633 * s, 90 * s, 633 * s, 150 * s), fill=(120, 82, 48, 255), width=int(5 * s))
        d.polygon([(674 * s, 70 * s), (724 * s, 70 * s), (724 * s, 80 * s), (684 * s, 80 * s), (684 * s, 160 * s), (674 * s, 160 * s)],
                  fill=(182, 186, 196, 255))
        # Lumber leaning on the right, boards stacked on the floor
        for k in range(5):
            x = 690 + k * 14
            d.polygon([(x * s, 120 * s), ((x + 12) * s, 116 * s), ((x + 40) * s, (floor_y + 4) * s), ((x + 28) * s, (floor_y + 6) * s)],
                      fill=(170 - k * 10, 124 - k * 8, 78 - k * 6, 255))
        for k in range(4):
            y = floor_y + 26 - k * 7
            d.rectangle((20 * s, y * s, 150 * s, (y + 7) * s), fill=(150 - k * 8, 106 - k * 6, 64 - k * 4, 255))
            d.rectangle((20 * s, y * s, 150 * s, (y + 1.5) * s), fill=(196, 150, 100, 255))
        # Sawdust heaps and shavings on the floor
        for _ in range(700):
            x, y = rnd.uniform(0, W), rnd.uniform(floor_y + 6, H)
            c = rnd.randint(196, 236)
            d.point((x * s, y * s), fill=(c, c - 36, c - 90, 220))
        for _ in range(30):
            x, y = rnd.uniform(160, 680), rnd.uniform(floor_y + 12, H - 6)
            r = rnd.uniform(5, 10)
            d.arc(((x - r) * s, (y - r * .5) * s, (x + r) * s, (y + r * .5) * s), rnd.uniform(0, 120), rnd.uniform(230, 360),
                  fill=(228, 186, 126, 255), width=int(2.4 * s))
    rgb = over(rgb, rgba_layer(build))
    # Sunbeams from the window, slanting down to the right; motes of dust in them
    u = (xs - 40) - (ys - 30) * -1.25
    beam = smooth(-10, 30, u) * smooth(200, 150, u) * smooth(30, 60, ys) * smooth(H, floor_y - 40, ys)
    beam = blur(beam, 6)
    rgb = rgb * (1 + .35 * beam[..., None]) + np.array([255, 230, 170]) * beam[..., None] * .22
    rnd2 = np.random.default_rng(664)
    for _ in range(120):
        x, y = rnd2.uniform(40, 520), rnd2.uniform(40, 300)
        k = beam[int(y), int(x)] if 0 <= int(x) < W and 0 <= int(y) < H else 0
        if k > .2:
            rgb = rgb + np.array([255, 240, 200]) * (glow(x, y, rnd2.uniform(.5, 1.2)) * k * .8)[..., None]
    light = np.clip(glow(110, 90, 330) * 1.1, 0, 1.1)
    rgb = lit(rgb, light, .55, (60, 40, 10), .25)
    return finish(rgb, .5, 665)


# ---------------------------------------------------------------- the infirmary

def infirmary():
    xs, ys = grid()
    # Whitewashed plaster, a little mottled and cracked, a dark beam along the top
    t = fbm(W, H, 5, 6, 670)
    rgb = ramp(t, [(0, (196, 188, 172)), (.5, (226, 220, 204)), (1, (242, 238, 226))])
    stains = smooth(.62, .8, fbm(W, H, 3, 4, 671)) * smooth(0, H, ys)
    rgb = mix(rgb, (190, 176, 150), stains * .3)
    f1, f2 = worley(W, H, 26, 672)
    rgb = mix(rgb, (170, 160, 144), smooth(.006, 0, f2 - f1) * smooth(.62, .8, fbm(W, H, 2, 3, 673)) * .35)
    rnd = random.Random(674)

    def build(d, s):
        d.rectangle((0, 0, W * s, 16 * s), fill=(76, 52, 34, 255))
        d.rectangle((0, 12 * s, W * s, 16 * s), fill=(46, 30, 20, 255))
        # Herbs drying from the beam, on the left
        for x in (40, 84, 132, 180):
            d.line((x * s, 16 * s, x * s, 26 * s), fill=(140, 116, 80, 255), width=max(1, int(s)))
            c = rnd.choice([(92, 140, 70), (150, 110, 180), (180, 170, 90), (110, 150, 90)])
            for k in range(8):
                a = (k - 3.5) * .16
                ex, ey = x + 26 * math.sin(a), 26 + 46 * math.cos(a)
                d.line((x * s, 26 * s, ex * s, ey * s), fill=c + (255,), width=int(2.2 * s))
                d.ellipse(((ex - 2.5) * s, (ey - 2.5) * s, (ex + 2.5) * s, (ey + 2.5) * s), fill=tuple(min(255, v + 60) for v in c) + (255,))
            d.rectangle(((x - 3) * s, 24 * s, (x + 3) * s, 29 * s), fill=(220, 210, 180, 255))
        # A shelf of jars and a mortar, in the middle
        d.rectangle((250 * s, 84 * s, 520 * s, 92 * s), fill=(110, 76, 48, 255))
        d.rectangle((250 * s, 84 * s, 520 * s, 86 * s), fill=(160, 120, 80, 255))
        x = 262
        while x < 470:
            w = rnd.uniform(18, 26)
            h = rnd.uniform(26, 40)
            body = rnd.choice([(196, 182, 150), (120, 84, 56), (90, 120, 140), (160, 70, 60)])
            d.rounded_rectangle((x * s, (84 - h) * s, (x + w) * s, 84 * s), radius=4 * s, fill=body + (255,))
            d.rectangle(((x + 3) * s, (84 - h * .6) * s, (x + w - 3) * s, (84 - h * .3) * s), fill=(240, 232, 210, 255))
            d.rectangle(((x + 2) * s, (84 - h - 5) * s, (x + w - 2) * s, (84 - h) * s), fill=(160, 124, 80, 255))
            d.line(((x + 3) * s, (84 - h + 4) * s, (x + 3) * s, (82) * s), fill=(255, 255, 255, 120), width=max(1, int(1.4 * s)))
            x += w + rnd.uniform(6, 12)
        d.pieslice((476 * s, 56 * s, 516 * s, 96 * s), 0, 180, fill=(150, 150, 140, 255))
        d.line((490 * s, 68 * s, 506 * s, 48 * s), fill=(120, 96, 70, 255), width=int(4 * s))
        # The healer's red cross on the wall, on the right, and a window
        cx, cy = 700, 50
        d.rectangle(((cx - 32) * s, (cy - 32) * s, (cx + 32) * s, (cy + 32) * s), fill=(244, 240, 232, 255))
        d.rectangle(((cx - 32) * s, (cy - 32) * s, (cx + 32) * s, (cy + 32) * s), outline=(150, 130, 100, 255), width=int(2 * s))
        d.rectangle(((cx - 8) * s, (cy - 24) * s, (cx + 8) * s, (cy + 24) * s), fill=(190, 34, 40, 255))
        d.rectangle(((cx - 24) * s, (cy - 8) * s, (cx + 24) * s, (cy + 8) * s), fill=(190, 34, 40, 255))
        wx, wy = 560, 26
        d.rectangle(((wx - 6) * s, (wy - 6) * s, (wx + 86) * s, (wy + 74) * s), fill=(110, 80, 52, 255))
        d.rectangle((wx * s, wy * s, (wx + 80) * s, (wy + 68) * s), fill=(214, 234, 246, 255))
        d.rectangle(((wx + 38) * s, wy * s, (wx + 42) * s, (wy + 68) * s), fill=(110, 80, 52, 255))
        d.rectangle((wx * s, (wy + 32) * s, (wx + 80) * s, (wy + 36) * s), fill=(110, 80, 52, 255))
        # The table's linen, folded, its hem; the remedy is made on it
        top = 212
        d.rectangle((0, top * s, W * s, H * s), fill=(236, 232, 222, 255))
        for k in range(12):
            x = 30 + k * 66 + rnd.uniform(-10, 10)
            d.line((x * s, (top + 2) * s, (x + rnd.uniform(-20, 20)) * s, H * s), fill=(214, 208, 196, 255), width=int(3 * s))
        d.rectangle((0, top * s, W * s, (top + 4) * s), fill=(200, 194, 182, 255))
        d.rectangle((0, (H - 14) * s, W * s, (H - 10) * s), fill=(190, 40, 46, 255))
        # Instruments laid out on it: scissors, a roll of bandage, a phial, a spoon
        d.ellipse((470 * s, 300 * s, 486 * s, 316 * s), outline=(150, 150, 160, 255), width=int(2 * s))
        d.ellipse((490 * s, 300 * s, 506 * s, 316 * s), outline=(150, 150, 160, 255), width=int(2 * s))
        d.line((484 * s, 304 * s, 540 * s, 286 * s), fill=(176, 178, 188, 255), width=int(3 * s))
        d.line((492 * s, 304 * s, 540 * s, 296 * s), fill=(176, 178, 188, 255), width=int(3 * s))
        d.ellipse((660 * s, 262 * s, 716 * s, 306 * s), fill=(244, 238, 222, 255))
        d.ellipse((674 * s, 274 * s, 702 * s, 294 * s), outline=(214, 206, 188, 255), width=int(2 * s))
        d.rounded_rectangle((736 * s, 248 * s, 760 * s, 296 * s), radius=6 * s, fill=(90, 170, 120, 255))
        d.rectangle((742 * s, 240 * s, 754 * s, 250 * s), fill=(160, 124, 80, 255))
        d.line((560 * s, 330 * s, 620 * s, 318 * s), fill=(150, 150, 160, 255), width=int(3 * s))
        d.ellipse((616 * s, 312 * s, 632 * s, 324 * s), fill=(160, 160, 170, 255))
    rgb = over(rgb, rgba_layer(build))
    # Soft daylight from the window, a clean shadow under the beam
    light = np.clip(glow(600, 60, 360) * 1.0 + .25, 0, 1.15)
    rgb = lit(rgb, light, .62, (30, 30, 20), .2)
    rgb = rgb * (1 - smooth(30, 16, ys)[..., None] * .25 * (ys > 16)[..., None])
    return finish(rgb, .4, 675)
