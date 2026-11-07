#!/usr/bin/env python3
"""The Arcanophone's art: its body (a frame of arcane titanium shading from violet to gold, a black bezel, side keys),
its wallpapers (a nebula, an aurora over mountains, the abyss, embers), its app icons (original glyphs on glossy rounded
squares), the white rounded shapes its screens are drawn with, and a soft glow.

Everything is drawn at 3 pixels a GUI pixel. Called by gen_assets.py (run that one). Original art, drawn here.
"""
import math

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

import gen_assets as g
from water_assets import _tnoise

S = 3
#: The phone's texture (GUI pixels): its body inside a margin for the side keys, its screen.
FW, FH = 184, 368
BODY = (4, 4, 180, 364)
SCREEN = (10, 10, 174, 358)
BODY_R, SCREEN_R = 30, 24
ICON = 32


def _np(img):
    return np.asarray(img, dtype=np.float32) / 255.0


def _img(a):
    return Image.fromarray(np.clip(a * 255, 0, 255).astype(np.uint8), "RGBA")


def _mask_rounded(w, h, box, r, scale=S):
    m = Image.new("L", (w, h), 0)
    ImageDraw.Draw(m).rounded_rectangle(tuple(v * scale for v in box), radius=r * scale, fill=255)
    return m


def frame():
    """The body: arcane titanium, a sheen running along it, a dark bezel, the screen's hole, three keys on its sides."""
    w, h = FW * S, FH * S
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
    t = (xs / w * .6 + ys / h * .4)
    # Deep violet titanium, darker in its middle, catching light along its edges like polished metal
    c0, c1, c2 = np.array([.20, .12, .30]), np.array([.42, .28, .58]), np.array([.86, .74, .98])
    k = (np.sin(t * math.tau * 1.1) * .5 + .5)[..., None]
    rgb = c0 * (1 - k) + c1 * k
    # Brushed lines and a polished highlight
    brushed = _tnoise(w, h, (2, 90), 4401)[..., None] * .12
    rgb = rgb * (.88 + brushed)
    body = _mask_rounded(w, h, BODY, BODY_R)
    inner = _mask_rounded(w, h, (BODY[0] + 2.2, BODY[1] + 2.2, BODY[2] - 2.2, BODY[3] - 2.2), BODY_R - 2.2)
    edge = np.asarray(body, np.float32) / 255 - np.asarray(inner, np.float32) / 255
    blur_edge = np.asarray(Image.fromarray((edge * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(2)), np.float32) / 255
    rgb = rgb + blur_edge[..., None] * (c2 - rgb) * .55
    # A thin bright line just inside the edge, and one on the outer rim
    rim = np.asarray(_mask_rounded(w, h, (BODY[0] + .6, BODY[1] + .6, BODY[2] - .6, BODY[3] - .6), BODY_R - .6), np.float32) / 255
    outer = np.asarray(body, np.float32) / 255 - rim
    rgb = rgb + outer[..., None] * .5
    # The bezel: near-black glass round the screen
    bezel = np.array([.03, .02, .05])
    inner_a = np.asarray(inner, np.float32)[..., None] / 255
    rgb = rgb * (1 - inner_a) + bezel * inner_a
    alpha = np.asarray(body, np.float32) / 255
    # The screen's hole
    hole = np.asarray(_mask_rounded(w, h, SCREEN, SCREEN_R), np.float32) / 255
    alpha = alpha * (1 - hole)
    # A faint reflection across the bezel
    refl = np.clip(1 - np.abs((xs - ys * .45) / w - .15) * 6, 0, 1) * .06
    rgb = rgb + refl[..., None] * inner_a
    img = _img(np.dstack([np.clip(rgb, 0, 1), alpha]))
    d = ImageDraw.Draw(img)
    # Side keys: action and volume on the left, power on the right
    def key(x0, y0, x1, y1):
        d.rounded_rectangle((x0 * S, y0 * S, x1 * S, y1 * S), radius=S * 1.2, fill=(84, 58, 112, 255), outline=(200, 180, 230, 255), width=2)
    key(1.6, 70, 4.6, 84)
    key(1.6, 96, 4.6, 120)
    key(1.6, 126, 4.6, 150)
    key(179.4, 104, 182.4, 138)
    return img


def _stars(img, n, seed, size=1.0, color=(255, 255, 255)):
    rng = np.random.default_rng(seed)
    d = ImageDraw.Draw(img)
    w, h = img.size
    for _ in range(n):
        x, y = rng.random() * w, rng.random() * h
        r = (rng.random() ** 3) * 2.2 * size + .5
        a = int(120 + rng.random() * 135)
        d.ellipse((x - r, y - r, x + r, y + r), fill=color + (a,))
    return img


def _gradient(w, h, stops):
    ys = np.linspace(0, 1, h)[:, None, None] * np.ones((1, w, 1))
    out = np.zeros((h, w, 3))
    for (p0, c0), (p1, c1) in zip(stops, stops[1:]):
        t = np.clip((ys - p0) / max(1e-6, p1 - p0), 0, 1)
        inside = ((ys >= p0) & (ys <= p1)).astype(float)
        col = np.array(c0) * (1 - t) + np.array(c1) * t
        out = out * (1 - inside) + col / 255 * inside
    return out


def wallpaper_nebula():
    w, h = (SCREEN[2] - SCREEN[0]) * S, (SCREEN[3] - SCREEN[1]) * S
    rgb = _gradient(w, h, [(0, (190, 60, 220)), (.35, (90, 30, 150)), (.7, (24, 10, 52)), (1, (6, 4, 18))])
    n1 = _tnoise(w, h, (3, 6), 4411)
    n2 = _tnoise(w, h, (8, 16), 4412)
    cloud = np.clip((n1 * .7 + n2 * .3 - .35) * 2, 0, 1)[..., None]
    rgb = rgb + cloud * np.array([.55, .2, .65]) * .6 + (cloud ** 3) * np.array([1, .7, .9]) * .3
    # A great glowing arc across the middle, like a planet's rim
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
    rr = np.hypot(xs - w * .5, ys - h * 1.05) / w
    arc = np.exp(-((rr - 1.0) / .02) ** 2)
    rgb = rgb + arc[..., None] * np.array([1, .75, 1]) * .9 + np.exp(-((rr - 1.0) / .12) ** 2)[..., None] * np.array([.5, .2, .8]) * .5
    inside = (rr < 1.0)[..., None]
    rgb = np.where(inside, rgb * .35 + np.array([.03, .02, .08]), rgb)
    img = _img(np.dstack([np.clip(rgb, 0, 1), np.ones((h, w))]))
    return _stars(img, 260, 4413)


def wallpaper_aurora():
    w, h = (SCREEN[2] - SCREEN[0]) * S, (SCREEN[3] - SCREEN[1]) * S
    rgb = _gradient(w, h, [(0, (4, 10, 30)), (.5, (8, 30, 60)), (.8, (14, 40, 60)), (1, (4, 10, 18))])
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
    for k, (col, y0, amp) in enumerate((((.3, 1, .6), .28, .08), ((.6, .4, 1), .2, .06), ((.2, .9, .8), .36, .05))):
        band_y = h * (y0 + amp * np.sin(xs / w * math.tau * (1.2 + k * .4) + k))
        d = (ys - band_y) / (h * .05)
        curtain = np.exp(-d ** 2) * (d < 0) + np.exp(-(d / 4) ** 2) * (d >= 0) * .0
        rays = .6 + .4 * np.sin(xs / w * 90 + k * 2)
        glow = np.clip(np.exp(-((ys - band_y + h * .06) / (h * .09)) ** 2) * rays, 0, 1)
        rgb = rgb + glow[..., None] * np.array(col) * .55 + curtain[..., None] * np.array(col) * .4
    # Mountains, layered, dark against it
    for k, (base, col) in enumerate(((.78, (.05, .1, .16)), (.86, (.03, .06, .1)), (.93, (.01, .02, .04)))):
        ridge = h * (base - .08 * _tnoise(w, 1, (6 + 3 * k, 1), 4420 + k)[0][None, :] - .04 * np.sin(xs / w * 7 + k))
        m = (ys > ridge)[..., None]
        rgb = np.where(m, np.array(col), rgb)
    img = _img(np.dstack([np.clip(rgb, 0, 1), np.ones((h, w))]))
    star_layer = _stars(Image.new("RGBA", (w, h), (0, 0, 0, 0)), 220, 4421)
    sl = np.asarray(star_layer, np.float32) / 255
    sl[..., 3] *= np.clip(1 - ys / (h * .6), 0, 1)
    return Image.alpha_composite(img, _img(sl))


def wallpaper_abyss():
    w, h = (SCREEN[2] - SCREEN[0]) * S, (SCREEN[3] - SCREEN[1]) * S
    rgb = _gradient(w, h, [(0, (40, 140, 220)), (.3, (12, 60, 130)), (.7, (4, 20, 60)), (1, (2, 6, 20))])
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
    for k in range(7):
        x0 = w * (.1 + .13 * k)
        ray = np.exp(-((xs - x0 - (ys / h) * w * .25) / (w * (.02 + .01 * (k % 3)))) ** 2) * np.clip(1 - ys / (h * .8), 0, 1)
        rgb = rgb + ray[..., None] * np.array([.4, .7, 1]) * .25
    caust = _tnoise(w, h, (10, 20), 4431)
    rgb = rgb + (np.clip(caust - .6, 0, 1) * 2 * np.clip(1 - ys / (h * .4), 0, 1))[..., None] * np.array([.5, .9, 1]) * .4
    img = _img(np.dstack([np.clip(rgb, 0, 1), np.ones((h, w))]))
    rng = np.random.default_rng(4432)
    d = ImageDraw.Draw(img)
    for _ in range(70):
        x, y = rng.random() * w, h * (.2 + .8 * rng.random())
        r = 1 + rng.random() * 4
        d.ellipse((x - r, y - r, x + r, y + r), outline=(180, 230, 255, 140), width=1)
    return img


def wallpaper_ember():
    w, h = (SCREEN[2] - SCREEN[0]) * S, (SCREEN[3] - SCREEN[1]) * S
    rgb = _gradient(w, h, [(0, (20, 6, 8)), (.45, (70, 14, 10)), (.8, (190, 70, 20)), (1, (255, 170, 60))])
    n = _tnoise(w, h, (5, 9), 4441)
    ys = np.linspace(0, 1, h)[:, None]
    rgb = rgb + (np.clip(n - .5, 0, 1) * 2 * ys)[..., None] * np.array([1, .45, .1]) * .5
    img = _img(np.dstack([np.clip(rgb, 0, 1), np.ones((h, w))]))
    return _stars(img, 160, 4442, 1.2, (255, 200, 120))


WALLPAPERS = (("nebula", wallpaper_nebula), ("aurora", wallpaper_aurora), ("abyss", wallpaper_abyss), ("ember", wallpaper_ember))


# ------------------------------------------------------------------------------------------------------- icons

def _squircle(n, top, bottom):
    """A glossy rounded square, {top} to {bottom} colour, a sheen across its upper half."""
    ss = n * 4
    ys, xs = np.mgrid[0:ss, 0:ss].astype(np.float32)
    u, v = (xs + .5) / ss * 2 - 1, (ys + .5) / ss * 2 - 1
    sq = (np.abs(u) ** 4.6 + np.abs(v) ** 4.6) ** (1 / 4.6)
    alpha = np.clip((1 - sq) * ss * .5, 0, 1)
    t = ((v + 1) / 2)[..., None]
    rgb = np.array(top) / 255 * (1 - t) + np.array(bottom) / 255 * t
    sheen = np.clip(1 - (v + .9) * 1.4, 0, 1) * .22 * np.clip(1 - sq, 0, 1) * 3
    rgb = rgb + sheen[..., None]
    rim = np.clip(1 - np.abs(sq - .95) * 40, 0, 1) * .25
    rgb = rgb + rim[..., None]
    return _img(np.dstack([np.clip(rgb, 0, 1), alpha])), ss


def _finish(img, ss, n):
    return img.resize((n, n), Image.LANCZOS)


def _icon(top, bottom, draw, n=96):
    img, ss = _squircle(n, top, bottom)
    glyph = Image.new("RGBA", (ss, ss), (0, 0, 0, 0))
    d = ImageDraw.Draw(glyph)
    draw(d, ss, glyph)
    shadow = glyph.filter(ImageFilter.GaussianBlur(ss * .02))
    sh = np.asarray(shadow, np.float32).copy()
    sh[..., :3] = 0
    sh[..., 3] *= .35
    img = Image.alpha_composite(img, Image.fromarray(sh.astype(np.uint8), "RGBA").transform(img.size, Image.AFFINE, (1, 0, 0, 0, 1, -ss * .012)))
    img = Image.alpha_composite(img, glyph)
    return _finish(img, ss, n)


W_ = (255, 255, 255, 255)


def _poly(d, s, pts, fill):
    d.polygon([(x * s, y * s) for x, y in pts], fill=fill)


def _ell(d, s, x, y, rx, ry, fill=None, outline=None, width=1):
    d.ellipse(((x - rx) * s, (y - ry) * s, (x + rx) * s, (y + ry) * s), fill=fill, outline=outline, width=int(width))


def _sparkle(d, s, x, y, r, fill):
    _poly(d, s, [(x, y - r), (x + r * .25, y - r * .25), (x + r, y), (x + r * .25, y + r * .25), (x, y + r), (x - r * .25, y + r * .25), (x - r, y),
                 (x - r * .25, y - r * .25)], fill)


def icon_missives():
    def draw(d, s, img):
        d.rounded_rectangle((.2 * s, .24 * s, .8 * s, .66 * s), radius=.14 * s, fill=W_)
        _poly(d, s, [(.32, .62), (.28, .8), (.46, .64)], W_)
        for k in range(3):
            d.line((.3 * s, (.36 + k * .09) * s, (.7 - k * .08) * s, (.36 + k * .09) * s), fill=(60, 170, 110, 255), width=int(s * .035))
        _sparkle(d, s, .76, .24, .09, (255, 250, 200, 255))
    return _icon((110, 240, 140), (20, 160, 90), draw)


def icon_contacts():
    def draw(d, s, img):
        _ell(d, s, .5, .38, .14, .14, fill=W_)
        d.pieslice((.24 * s, .52 * s, .76 * s, 1.0 * s), 180, 360, fill=W_)
        _ell(d, s, .5, .38, .2, .2, outline=(220, 230, 255, 160), width=s * .02)
    return _icon((150, 165, 190), (80, 92, 120), draw)


def icon_carte():
    def draw(d, s, img):
        _poly(d, s, [(.14, .26), (.38, .18), (.62, .26), (.86, .18), (.86, .74), (.62, .82), (.38, .74), (.14, .82)], (240, 236, 220, 255))
        _poly(d, s, [(.38, .18), (.62, .26), (.62, .82), (.38, .74)], (200, 230, 200, 255))
        d.line([(.2 * s, .7 * s), (.4 * s, .5 * s), (.55 * s, .58 * s), (.78 * s, .32 * s)], fill=(160, 90, 255, 255), width=int(s * .05))
        _ell(d, s, .78, .32, .07, .07, fill=(255, 80, 120, 255))
        _ell(d, s, .78, .32, .03, .03, fill=W_)
    return _icon((90, 200, 255), (40, 120, 220), draw)


def icon_photo():
    def draw(d, s, img):
        d.rounded_rectangle((.16 * s, .32 * s, .84 * s, .76 * s), radius=.08 * s, fill=(60, 60, 70, 255))
        d.rounded_rectangle((.36 * s, .24 * s, .56 * s, .34 * s), radius=.03 * s, fill=(60, 60, 70, 255))
        _ell(d, s, .5, .54, .16, .16, fill=(30, 20, 50, 255))
        _ell(d, s, .5, .54, .12, .12, fill=(150, 80, 255, 255))
        _ell(d, s, .46, .5, .04, .04, fill=(240, 220, 255, 255))
        _ell(d, s, .74, .4, .03, .03, fill=(255, 210, 120, 255))
    return _icon((235, 235, 240), (170, 170, 180), draw)


def icon_galerie():
    def draw(d, s, img):
        d.rounded_rectangle((.16 * s, .2 * s, .84 * s, .8 * s), radius=.08 * s, fill=(255, 255, 255, 255))
        d.rounded_rectangle((.2 * s, .24 * s, .8 * s, .76 * s), radius=.05 * s, fill=(255, 190, 120, 255))
        _poly(d, s, [(.2, .76), (.42, .46), (.58, .64), (.68, .54), (.8, .76)], (60, 120, 90, 255))
        _ell(d, s, .66, .36, .07, .07, fill=(255, 245, 210, 255))
    return _icon((255, 140, 170), (200, 60, 120), draw)


def icon_meteo():
    def draw(d, s, img):
        _ell(d, s, .4, .4, .16, .16, fill=(255, 220, 90, 255))
        for k in range(8):
            a = k * math.tau / 8
            d.line(((.4 + math.cos(a) * .2) * s, (.4 + math.sin(a) * .2) * s, (.4 + math.cos(a) * .27) * s, (.4 + math.sin(a) * .27) * s),
                   fill=(255, 220, 90, 255), width=int(s * .035))
        for cx, cy, r in ((.52, .62, .14), (.66, .56, .16), (.78, .64, .11)):
            _ell(d, s, cx, cy, r, r, fill=W_)
        d.rectangle((.52 * s, .62 * s, .78 * s, .75 * s), fill=W_)
    return _icon((90, 170, 255), (30, 90, 200), draw)


def icon_musique():
    def draw(d, s, img):
        _ell(d, s, .36, .68, .1, .08, fill=W_)
        _ell(d, s, .66, .62, .1, .08, fill=W_)
        d.line((.45 * s, .68 * s, .45 * s, .28 * s), fill=W_, width=int(s * .05))
        d.line((.75 * s, .62 * s, .75 * s, .22 * s), fill=W_, width=int(s * .05))
        _poly(d, s, [(.43, .26), (.77, .2), (.77, .3), (.43, .36)], W_)
    return _icon((255, 90, 140), (200, 30, 90), draw)


def icon_calcul():
    def draw(d, s, img):
        for r in range(4):
            for c in range(3):
                col = (255, 170, 60, 255) if c == 2 else (90, 90, 96, 255)
                _ell(d, s, .3 + c * .2, .26 + r * .16, .07, .07, fill=col)
        d.line((.24 * s, .26 * s, .36 * s, .26 * s), fill=W_, width=int(s * .03))
    return _icon((60, 60, 66), (24, 24, 28), draw)


def icon_reglages():
    def draw(d, s, img):
        for k in range(10):
            a = k * math.tau / 10
            _poly(d, s, [(.5 + math.cos(a - .14) * .26, .5 + math.sin(a - .14) * .26), (.5 + math.cos(a - .1) * .34, .5 + math.sin(a - .1) * .34),
                         (.5 + math.cos(a + .1) * .34, .5 + math.sin(a + .1) * .34), (.5 + math.cos(a + .14) * .26, .5 + math.sin(a + .14) * .26)], (230, 230, 236, 255))
        _ell(d, s, .5, .5, .27, .27, fill=(230, 230, 236, 255))
        _ell(d, s, .5, .5, .14, .14, fill=(120, 120, 130, 255))
        _sparkle(d, s, .5, .5, .1, (180, 140, 255, 255))
    return _icon((150, 150, 160), (90, 90, 100), draw)


def icon_notes():
    def draw(d, s, img):
        d.rounded_rectangle((.2 * s, .16 * s, .8 * s, .84 * s), radius=.05 * s, fill=(255, 250, 225, 255))
        d.rectangle((.2 * s, .16 * s, .8 * s, .28 * s), fill=(250, 200, 60, 255))
        for k in range(4):
            d.line((.28 * s, (.38 + k * .11) * s, .72 * s, (.38 + k * .11) * s), fill=(200, 190, 160, 255), width=int(s * .02))
        _poly(d, s, [(.64, .78), (.86, .44), (.9, .48), (.68, .82)], (120, 70, 40, 255))
    return _icon((255, 225, 110), (240, 180, 40), draw)


def icon_banque():
    def draw(d, s, img):
        _poly(d, s, [(.5, .16), (.78, .4), (.5, .86), (.22, .4)], (120, 255, 170, 255))
        _poly(d, s, [(.5, .16), (.64, .4), (.5, .86), (.36, .4)], (60, 220, 130, 255))
        _poly(d, s, [(.22, .4), (.78, .4), (.64, .28), (.36, .28)], (200, 255, 220, 255))
        _sparkle(d, s, .72, .26, .08, W_)
    return _icon((20, 120, 80), (6, 60, 40), draw)


def icon_echos():
    def draw(d, s, img):
        for k, r in enumerate((.12, .22, .32)):
            d.arc(((.5 - r) * s, (.5 - r) * s, (.5 + r) * s, (.5 + r) * s), -60, 60, fill=W_, width=int(s * .045))
            d.arc(((.5 - r) * s, (.5 - r) * s, (.5 + r) * s, (.5 + r) * s), 120, 240, fill=W_, width=int(s * .045))
        _ell(d, s, .5, .5, .07, .07, fill=W_)
    return _icon((255, 120, 60), (150, 50, 220), draw)


def icon_grimoire():
    def draw(d, s, img):
        d.rounded_rectangle((.22 * s, .18 * s, .78 * s, .82 * s), radius=.05 * s, fill=(120, 40, 90, 255))
        d.rectangle((.22 * s, .18 * s, .3 * s, .82 * s), fill=(80, 20, 60, 255))
        _sparkle(d, s, .54, .48, .17, (255, 220, 120, 255))
        d.rectangle((.74 * s, .22 * s, .78 * s, .78 * s), fill=(245, 235, 210, 255))
    return _icon((140, 70, 230), (60, 20, 120), draw)


def icon_horloge():
    def draw(d, s, img):
        _ell(d, s, .5, .5, .32, .32, fill=W_)
        for k in range(12):
            a = k * math.tau / 12
            r0 = .26 if k % 3 else .22
            d.line(((.5 + math.cos(a) * r0) * s, (.5 + math.sin(a) * r0) * s, (.5 + math.cos(a) * .29) * s, (.5 + math.sin(a) * .29) * s),
                   fill=(40, 40, 50, 255), width=int(s * .02))
        d.line((.5 * s, .5 * s, .5 * s, .3 * s), fill=(30, 30, 40, 255), width=int(s * .035))
        d.line((.5 * s, .5 * s, .66 * s, .58 * s), fill=(30, 30, 40, 255), width=int(s * .035))
        d.line((.5 * s, .5 * s, .36 * s, .7 * s), fill=(255, 120, 60, 255), width=int(s * .015))
        _ell(d, s, .5, .5, .03, .03, fill=(255, 120, 60, 255))
    return _icon((40, 40, 46), (10, 10, 14), draw)


def icon_boussole():
    def draw(d, s, img):
        _ell(d, s, .5, .5, .33, .33, outline=W_, width=s * .025)
        for k in range(4):
            a = k * math.tau / 4
            d.line(((.5 + math.cos(a) * .26) * s, (.5 + math.sin(a) * .26) * s, (.5 + math.cos(a) * .31) * s, (.5 + math.sin(a) * .31) * s), fill=W_,
                   width=int(s * .025))
        _poly(d, s, [(.5, .2), (.58, .5), (.5, .5), (.42, .5)], (255, 70, 80, 255))
        _poly(d, s, [(.5, .8), (.58, .5), (.5, .5), (.42, .5)], (230, 230, 240, 255))
        _ell(d, s, .5, .5, .035, .035, fill=(40, 40, 50, 255))
    return _icon((60, 60, 70), (20, 20, 26), draw)


def icon_des():
    def draw(d, s, img):
        _poly(d, s, [(.5, .14), (.84, .34), (.84, .7), (.5, .9), (.16, .7), (.16, .34)], (250, 244, 236, 255))
        _poly(d, s, [(.5, .52), (.84, .34), (.84, .7), (.5, .9)], (220, 210, 200, 255))
        _poly(d, s, [(.5, .52), (.16, .34), (.16, .7), (.5, .9)], (236, 228, 218, 255))
        for x, y in ((.5, .32), (.33, .62), (.42, .74), (.67, .58), (.74, .72)):
            _ell(d, s, x, y, .04, .035, fill=(170, 30, 50, 255))
    return _icon((240, 70, 90), (150, 20, 40), draw)


ICONS = (("missives", icon_missives), ("contacts", icon_contacts), ("carte", icon_carte), ("photo", icon_photo), ("galerie", icon_galerie),
         ("meteo", icon_meteo), ("musique", icon_musique), ("calcul", icon_calcul), ("reglages", icon_reglages), ("notes", icon_notes),
         ("banque", icon_banque), ("echos", icon_echos), ("grimoire", icon_grimoire),
         ("horloge", icon_horloge), ("boussole", icon_boussole),
         ("des", icon_des))


def rounded(n, r):
    """{n} x {n}, white, a rounded square of corner {r} (for nine-slice drawing)."""
    ss = n * 4
    img = Image.new("RGBA", (ss, ss), (0, 0, 0, 0))
    ImageDraw.Draw(img).rounded_rectangle((0, 0, ss - 1, ss - 1), radius=r * 4, fill=(255, 255, 255, 255))
    return img.resize((n, n), Image.LANCZOS)


def glow():
    n = 64
    ys, xs = np.mgrid[0:n, 0:n].astype(np.float32)
    r = np.hypot(xs - n / 2 + .5, ys - n / 2 + .5) / (n / 2)
    a = np.clip(1 - r, 0, 1) ** 2
    return _img(np.dstack([np.ones((n, n)), np.ones((n, n)), np.ones((n, n)), a]))


def item_icon():
    return g.from_grid([
        "................",
        ".....oooooo.....",
        "....oVVVVVVo....",
        "....oVkkkkVo....",
        "....oVpPPpVo....",
        "....oVPppPVo....",
        "....oVpPPpVo....",
        "....oVPppPVo....",
        "....oVpPPpVo....",
        "....oVPppPVo....",
        "....oVkkkkVo....",
        "....oVVGVVVo....",
        ".....oooooo.....",
        "................",
        "................",
        "................",
    ], {"o": (40, 20, 60), "V": (180, 120, 210), "G": (250, 210, 140), "k": (20, 10, 30), "p": (150, 60, 220), "P": (230, 120, 240)})


def main():
    g.save(frame(), "textures/gui/phone/frame.png")
    for name, fn in WALLPAPERS:
        g.save(fn(), f"textures/gui/phone/wallpaper_{name}.png")
    for name, fn in ICONS:
        g.save(fn(), f"textures/gui/phone/app_{name}.png")
    g.save(rounded(48, 12), "textures/gui/phone/round.png")
    g.save(glow(), "textures/gui/phone/glow.png")
    g.save(item_icon(), "textures/item/arcanophone.png")
    g.write_json("models/item/arcanophone.json", {"parent": "item/handheld", "textures": {"layer0": "arcanes_oublies:item/arcanophone"}})


if __name__ == "__main__":
    main()
