"""The 2026 smartphone's art: its graphite body (tinted in game by the finish chosen), four wallpapers, sixteen app
icons (original glyphs on glossy rounded squares), the shapes its screens are drawn with, a glow, and the red balloon
that drifts through a haunted screen. Drawn at 3 pixels a GUI pixel. Original art."""
import math

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

import kit
from kit import S, W_, ell, poly, icon

FW, FH = 184, 368
BODY = (4, 4, 180, 364)
SCREEN = (10, 10, 174, 358)
BODY_R, SCREEN_R = 30, 24


def frame():
    """Graphite titanium, light catching its rim, a black bezel round the screen's hole, keys on its sides."""
    w, h = FW * S, FH * S
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
    t = xs / w * .6 + ys / h * .4
    c0, c1, c2 = np.array([.36, .37, .39]), np.array([.60, .61, .63]), np.array([.95, .96, .97])
    k = (np.sin(t * math.tau * 1.1) * .5 + .5)[..., None]
    rgb = c0 * (1 - k) + c1 * k
    rgb = rgb * (.9 + kit.tnoise(w, h, (2, 90), 5401)[..., None] * .1)
    body = kit.mask_rounded(w, h, BODY, BODY_R)
    inner = kit.mask_rounded(w, h, (BODY[0] + 2.2, BODY[1] + 2.2, BODY[2] - 2.2, BODY[3] - 2.2), BODY_R - 2.2)
    edge = np.asarray(body, np.float32) / 255 - np.asarray(inner, np.float32) / 255
    blur = np.asarray(Image.fromarray((edge * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(2)), np.float32) / 255
    rgb = rgb + blur[..., None] * (c2 - rgb) * .55
    rim = np.asarray(kit.mask_rounded(w, h, (BODY[0] + .6, BODY[1] + .6, BODY[2] - .6, BODY[3] - .6), BODY_R - .6), np.float32) / 255
    rgb = rgb + (np.asarray(body, np.float32) / 255 - rim)[..., None] * .5
    bezel = np.array([.02, .02, .025])
    inner_a = np.asarray(inner, np.float32)[..., None] / 255
    rgb = rgb * (1 - inner_a) + bezel * inner_a
    alpha = np.asarray(body, np.float32) / 255
    hole = np.asarray(kit.mask_rounded(w, h, SCREEN, SCREEN_R), np.float32) / 255
    alpha = alpha * (1 - hole)
    refl = np.clip(1 - np.abs((xs - ys * .45) / w - .15) * 6, 0, 1) * .05
    rgb = rgb + refl[..., None] * inner_a
    img = kit.to_img(np.dstack([np.clip(rgb, 0, 1), alpha]))
    d = ImageDraw.Draw(img)

    def key(x0, y0, x1, y1):
        d.rounded_rectangle((x0 * S, y0 * S, x1 * S, y1 * S), radius=S * 1.2, fill=(110, 112, 118, 255), outline=(220, 222, 228, 255), width=2)
    key(1.6, 70, 4.6, 84)
    key(1.6, 96, 4.6, 120)
    key(1.6, 126, 4.6, 150)
    key(179.4, 104, 182.4, 138)
    return img


def _screen_size():
    return (SCREEN[2] - SCREEN[0]) * S, (SCREEN[3] - SCREEN[1]) * S


def wallpaper_nebula():
    w, h = _screen_size()
    rgb = kit.gradient(w, h, [(0, (70, 90, 200)), (.35, (40, 40, 120)), (.7, (14, 12, 40)), (1, (4, 4, 14))])
    n1, n2 = kit.tnoise(w, h, (3, 6), 5411), kit.tnoise(w, h, (8, 16), 5412)
    cloud = np.clip((n1 * .7 + n2 * .3 - .35) * 2, 0, 1)[..., None]
    rgb = rgb + cloud * np.array([.3, .3, .7]) * .6 + (cloud ** 3) * np.array([.8, .8, 1]) * .3
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
    rr = np.hypot(xs - w * .5, ys - h * 1.05) / w
    rgb = rgb + np.exp(-((rr - 1.0) / .02) ** 2)[..., None] * np.array([.8, .9, 1]) * .9
    rgb = np.where((rr < 1.0)[..., None], rgb * .35 + np.array([.02, .03, .06]), rgb)
    img = kit.to_img(np.dstack([np.clip(rgb, 0, 1), np.ones((h, w))]))
    return kit.stars(img, 260, 5413)


def wallpaper_aurora():
    """A small town under the northern lights: roofs and a steeple dark against the sky."""
    w, h = _screen_size()
    rgb = kit.gradient(w, h, [(0, (4, 10, 30)), (.5, (8, 30, 60)), (.8, (14, 40, 60)), (1, (4, 10, 18))])
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
    for k, (col, y0, amp) in enumerate((((.3, 1, .6), .28, .08), ((.4, .7, 1), .2, .06), ((.2, .9, .8), .36, .05))):
        band = h * (y0 + amp * np.sin(xs / w * math.tau * (1.2 + k * .4) + k))
        rays = .6 + .4 * np.sin(xs / w * 90 + k * 2)
        g = np.clip(np.exp(-((ys - band + h * .06) / (h * .09)) ** 2) * rays, 0, 1)
        rgb = rgb + g[..., None] * np.array(col) * .55
    img = kit.to_img(np.dstack([np.clip(rgb, 0, 1), np.ones((h, w))]))
    img = Image.alpha_composite(img, kit.stars(Image.new("RGBA", (w, h), (0, 0, 0, 0)), 160, 5421))
    d = ImageDraw.Draw(img)
    rng = np.random.default_rng(5422)
    x = 0
    base = h * .82
    while x < w:
        bw = int(w * (.06 + rng.random() * .08))
        bh = int(h * (.05 + rng.random() * .1))
        d.rectangle((x, base - bh, x + bw, h), fill=(6, 10, 18, 255))
        d.polygon([(x - 4, base - bh), (x + bw / 2, base - bh - bw * .45), (x + bw + 4, base - bh)], fill=(6, 10, 18, 255))
        for _ in range(int(rng.random() * 3)):
            wx, wy = x + 4 + rng.random() * (bw - 10), base - bh + 6 + rng.random() * (bh - 10)
            d.rectangle((wx, wy, wx + 4, wy + 5), fill=(255, 200, 110, 230))
        x += bw + int(rng.random() * 6)
    sx = int(w * .62)
    d.polygon([(sx, base - h * .28), (sx - 10, base - h * .14), (sx + 10, base - h * .14)], fill=(6, 10, 18, 255))
    d.rectangle((sx - 10, base - h * .14, sx + 10, h), fill=(6, 10, 18, 255))
    d.rectangle((0, base + 6, w, h), fill=(3, 5, 10, 255))
    return img


def wallpaper_abyss():
    """A lake at dusk under drifting fog, the far shore's pines black."""
    w, h = _screen_size()
    rgb = kit.gradient(w, h, [(0, (60, 70, 100)), (.4, (150, 130, 140)), (.55, (200, 160, 140)), (.56, (70, 70, 90)), (1, (10, 14, 22))])
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
    fog = kit.tnoise(w, h, (4, 12), 5431)
    rgb = rgb + (np.clip(fog - .45, 0, 1) * np.exp(-((ys / h - .55) / .12) ** 2))[..., None] * .6
    ripples = np.sin(ys / h * 400 + kit.tnoise(w, h, (6, 30), 5432) * 8) * .03 * (ys > h * .56)
    rgb = rgb + ripples[..., None]
    img = kit.to_img(np.dstack([np.clip(rgb, 0, 1), np.ones((h, w))]))
    d = ImageDraw.Draw(img)
    rng = np.random.default_rng(5433)
    for _ in range(60):
        x = rng.random() * w
        ph = h * (.03 + rng.random() * .06)
        d.polygon([(x, h * .555 - ph), (x - ph * .3, h * .556), (x + ph * .3, h * .556)], fill=(16, 18, 26, 255))
    return img


def wallpaper_ember():
    w, h = _screen_size()
    rgb = kit.gradient(w, h, [(0, (20, 6, 8)), (.45, (70, 14, 10)), (.8, (190, 70, 20)), (1, (255, 170, 60))])
    n = kit.tnoise(w, h, (5, 9), 5441)
    ys = np.linspace(0, 1, h)[:, None]
    rgb = rgb + (np.clip(n - .5, 0, 1) * 2 * ys)[..., None] * np.array([1, .45, .1]) * .5
    img = kit.to_img(np.dstack([np.clip(rgb, 0, 1), np.ones((h, w))]))
    return kit.stars(img, 160, 5442, 1.2, (255, 200, 120))


WALLPAPERS = (("nebula", wallpaper_nebula), ("aurora", wallpaper_aurora), ("abyss", wallpaper_abyss), ("ember", wallpaper_ember))


# ------------------------------------------------------------------------------------------------------- icons

def handset(d, s, cx, cy, size, fill):
    """A telephone handset, tilted."""
    r = size
    pts = []
    for k in range(40):
        a = math.pi * .75 + k / 39 * math.pi * 1.0
        pts.append((cx + math.cos(a) * r * .55, cy + math.sin(a) * r * .55))
    d.line([(x * s, y * s) for x, y in pts], fill=fill, width=int(s * r * .22))
    for a in (math.pi * .75, math.pi * 1.75):
        ex, ey = cx + math.cos(a) * r * .55, cy + math.sin(a) * r * .55
        ell(d, s, ex, ey, r * .2, r * .2, fill=fill)


def icon_telephone():
    def draw(d, s, img):
        handset(d, s, .5, .52, .5, W_)
    return icon((110, 240, 140), (20, 170, 80), draw)


def icon_messages():
    def draw(d, s, img):
        ell(d, s, .5, .46, .32, .25, fill=W_)
        poly(d, s, [(.3, .62), (.24, .8), (.44, .68)], W_)
    return icon((90, 200, 255), (30, 110, 230), draw)


def icon_contacts():
    def draw(d, s, img):
        ell(d, s, .5, .38, .14, .14, fill=W_)
        d.pieslice((.24 * s, .52 * s, .76 * s, 1.0 * s), 180, 360, fill=W_)
    return icon((170, 175, 185), (100, 105, 120), draw)


def icon_carte():
    def draw(d, s, img):
        poly(d, s, [(.14, .26), (.38, .18), (.62, .26), (.86, .18), (.86, .74), (.62, .82), (.38, .74), (.14, .82)], (240, 236, 220, 255))
        poly(d, s, [(.38, .18), (.62, .26), (.62, .82), (.38, .74)], (200, 230, 200, 255))
        d.line([(.2 * s, .7 * s), (.4 * s, .5 * s), (.55 * s, .58 * s), (.78 * s, .32 * s)], fill=(60, 140, 255, 255), width=int(s * .05))
        ell(d, s, .78, .32, .07, .07, fill=(255, 80, 90, 255))
        ell(d, s, .78, .32, .03, .03, fill=W_)
    return icon((240, 240, 236), (200, 204, 196), draw)


def icon_photo():
    def draw(d, s, img):
        d.rounded_rectangle((.16 * s, .32 * s, .84 * s, .76 * s), radius=.08 * s, fill=(60, 60, 70, 255))
        d.rounded_rectangle((.36 * s, .24 * s, .56 * s, .34 * s), radius=.03 * s, fill=(60, 60, 70, 255))
        ell(d, s, .5, .54, .16, .16, fill=(20, 20, 30, 255))
        ell(d, s, .5, .54, .12, .12, fill=(60, 110, 200, 255))
        ell(d, s, .46, .5, .04, .04, fill=(220, 235, 255, 255))
    return icon((235, 235, 240), (170, 170, 180), draw)


def icon_galerie():
    def draw(d, s, img):
        for k, col in enumerate(((255, 190, 60), (255, 100, 120), (150, 110, 255), (60, 180, 255), (80, 210, 140), (255, 150, 60))):
            a = k * math.tau / 6
            ell(d, s, .5 + math.cos(a) * .17, .5 + math.sin(a) * .17, .15, .15, fill=col + (200,))
    return icon((255, 255, 255), (235, 235, 240), draw)


def icon_meteo():
    def draw(d, s, img):
        ell(d, s, .4, .4, .16, .16, fill=(255, 220, 90, 255))
        for cx, cy, r in ((.52, .62, .14), (.66, .56, .16), (.78, .64, .11)):
            ell(d, s, cx, cy, r, r, fill=W_)
        d.rectangle((.52 * s, .62 * s, .78 * s, .75 * s), fill=W_)
    return icon((90, 170, 255), (30, 90, 200), draw)


def icon_journal():
    def draw(d, s, img):
        d.rounded_rectangle((.18 * s, .2 * s, .82 * s, .8 * s), radius=.04 * s, fill=(250, 246, 236, 255))
        d.rectangle((.24 * s, .26 * s, .76 * s, .34 * s), fill=(30, 26, 22, 255))
        d.rectangle((.24 * s, .4 * s, .46 * s, .58 * s), fill=(150, 140, 120, 255))
        for k in range(4):
            d.rectangle((.5 * s, (.41 + k * .05) * s, .76 * s, (.43 + k * .05) * s), fill=(120, 110, 100, 255))
        for k in range(3):
            d.rectangle((.24 * s, (.64 + k * .05) * s, .76 * s, (.66 + k * .05) * s), fill=(120, 110, 100, 255))
    return icon((200, 60, 50), (130, 20, 20), draw)


def icon_calcul():
    def draw(d, s, img):
        for r in range(4):
            for c in range(3):
                col = (255, 170, 60, 255) if c == 2 else (90, 90, 96, 255)
                ell(d, s, .3 + c * .2, .26 + r * .16, .07, .07, fill=col)
    return icon((60, 60, 66), (24, 24, 28), draw)


def icon_notes():
    def draw(d, s, img):
        d.rounded_rectangle((.2 * s, .16 * s, .8 * s, .84 * s), radius=.05 * s, fill=(255, 250, 225, 255))
        d.rectangle((.2 * s, .16 * s, .8 * s, .28 * s), fill=(250, 200, 60, 255))
        for k in range(4):
            d.line((.28 * s, (.38 + k * .11) * s, .72 * s, (.38 + k * .11) * s), fill=(200, 190, 160, 255), width=int(s * .02))
    return icon((255, 225, 110), (240, 180, 40), draw)


def icon_horloge():
    def draw(d, s, img):
        ell(d, s, .5, .5, .32, .32, fill=W_)
        for k in range(12):
            a = k * math.tau / 12
            r0 = .26 if k % 3 else .22
            d.line(((.5 + math.cos(a) * r0) * s, (.5 + math.sin(a) * r0) * s, (.5 + math.cos(a) * .29) * s, (.5 + math.sin(a) * .29) * s),
                   fill=(40, 40, 50, 255), width=int(s * .02))
        d.line((.5 * s, .5 * s, .5 * s, .3 * s), fill=(30, 30, 40, 255), width=int(s * .035))
        d.line((.5 * s, .5 * s, .66 * s, .58 * s), fill=(30, 30, 40, 255), width=int(s * .035))
        d.line((.5 * s, .5 * s, .36 * s, .7 * s), fill=(255, 120, 60, 255), width=int(s * .015))
    return icon((40, 40, 46), (10, 10, 14), draw)


def icon_boussole():
    def draw(d, s, img):
        ell(d, s, .5, .5, .33, .33, outline=W_, width=s * .025)
        poly(d, s, [(.5, .2), (.58, .5), (.5, .5), (.42, .5)], (255, 70, 80, 255))
        poly(d, s, [(.5, .8), (.58, .5), (.5, .5), (.42, .5)], (230, 230, 240, 255))
        ell(d, s, .5, .5, .035, .035, fill=(40, 40, 50, 255))
    return icon((60, 60, 70), (20, 20, 26), draw)


def icon_serpent():
    def draw(d, s, img):
        cells = [(2, 5), (3, 5), (4, 5), (4, 4), (4, 3), (5, 3), (6, 3), (6, 4), (6, 5), (6, 6), (7, 6)]
        for k, (cx, cy) in enumerate(cells):
            col = (120, 255, 200, 255) if k == len(cells) - 1 else (60, 220, 160, 255)
            d.rounded_rectangle(((.1 + cx * .09) * s, (.1 + cy * .09) * s, (.1 + cx * .09 + .085) * s, (.1 + cy * .09 + .085) * s), radius=.02 * s, fill=col)
        d.rounded_rectangle(((.1 + 2 * .09) * s, (.1 + 2 * .09) * s, (.1 + 2 * .09 + .085) * s, (.1 + 2 * .09 + .085) * s), radius=.04 * s,
                            fill=(255, 90, 140, 255))
    return icon((20, 50, 60), (6, 16, 22), draw)


def icon_des():
    def draw(d, s, img):
        poly(d, s, [(.5, .14), (.84, .34), (.84, .7), (.5, .9), (.16, .7), (.16, .34)], (250, 244, 236, 255))
        poly(d, s, [(.5, .52), (.84, .34), (.84, .7), (.5, .9)], (220, 210, 200, 255))
        poly(d, s, [(.5, .52), (.16, .34), (.16, .7), (.5, .9)], (236, 228, 218, 255))
        for x, y in ((.5, .32), (.33, .62), (.42, .74), (.67, .58), (.74, .72)):
            ell(d, s, x, y, .04, .035, fill=(170, 30, 50, 255))
    return icon((240, 70, 90), (150, 20, 40), draw)


def icon_reglages():
    def draw(d, s, img):
        for k in range(10):
            a = k * math.tau / 10
            poly(d, s, [(.5 + math.cos(a - .14) * .26, .5 + math.sin(a - .14) * .26), (.5 + math.cos(a - .1) * .34, .5 + math.sin(a - .1) * .34),
                        (.5 + math.cos(a + .1) * .34, .5 + math.sin(a + .1) * .34), (.5 + math.cos(a + .14) * .26, .5 + math.sin(a + .14) * .26)],
                 (230, 230, 236, 255))
        ell(d, s, .5, .5, .27, .27, fill=(230, 230, 236, 255))
        ell(d, s, .5, .5, .12, .12, fill=(120, 120, 130, 255))
    return icon((150, 150, 160), (90, 90, 100), draw)


def icon_murmures():
    def draw(d, s, img):
        d.rounded_rectangle((.18 * s, .24 * s, .74 * s, .62 * s), radius=.12 * s, fill=W_)
        poly(d, s, [(.3, .58), (.26, .74), (.42, .6)], W_)
        for k, r in enumerate((.08, .14)):
            d.arc(((.74 - r) * s, (.36 - r) * s, (.74 + r) * s, (.36 + r) * s), -60, 60, fill=W_, width=int(s * .035))
        for k in range(3):
            ell(d, s, .32 + k * .1, .43, .025, .025, fill=(40, 130, 220, 255))
    return icon((80, 180, 255), (20, 80, 200), draw)


ICONS = (("telephone", icon_telephone), ("messages", icon_messages), ("contacts", icon_contacts), ("carte", icon_carte), ("photo", icon_photo),
         ("galerie", icon_galerie), ("meteo", icon_meteo), ("journal", icon_journal), ("calcul", icon_calcul), ("notes", icon_notes),
         ("horloge", icon_horloge), ("boussole", icon_boussole), ("serpent", icon_serpent), ("des", icon_des), ("reglages", icon_reglages),
         ("murmures", icon_murmures))


def balloon():
    """A red balloon on its string (32 x 64), glossy, its knot at the bottom."""
    w, h = 32 * 4, 64 * 4
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
    u, v = (xs - w * .5) / (w * .42), (ys - h * .3) / (h * .27)
    v = np.where(v > 0, v * (1 - .18 * v), v)
    r = np.hypot(u, v)
    inside = np.clip((1 - r) * 30, 0, 1)
    shade = np.clip(1 - np.hypot(u + .35, v + .4) * .7, 0, 1)
    rgb = np.dstack([.55 + .45 * shade, .04 + .2 * shade ** 3, .05 + .2 * shade ** 3])
    spec = np.exp(-((u + .38) ** 2 + (v + .45) ** 2) / .02)[..., None]
    rgb = rgb + spec * .7
    layer = kit.to_img(np.dstack([np.clip(rgb, 0, 1), inside]))
    img = Image.alpha_composite(img, layer)
    d = ImageDraw.Draw(img)
    kx, ky = w * .5, h * .57
    d.polygon([(kx - 7, ky + 6), (kx + 7, ky + 6), (kx, ky - 4)], fill=(150, 10, 14, 255))
    pts = [(kx + math.sin(t * .35) * 6, ky + 6 + t * 3.4) for t in range(int((h - ky - 8) / 3.4))]
    d.line(pts, fill=(235, 235, 235, 230), width=2)
    return img.resize((32, 64), Image.LANCZOS)


def main():
    kit.save(frame(), "textures/gui/phone/frame.png")
    for name, fn in WALLPAPERS:
        kit.save(fn(), f"textures/gui/phone/wallpaper_{name}.png")
    for name, fn in ICONS:
        kit.save(fn(), f"textures/gui/phone/app_{name}.png")
    kit.save(kit.rounded(48, 12), "textures/gui/phone/round.png")
    kit.save(kit.glow(), "textures/gui/phone/glow.png")
    kit.save(balloon(), "textures/gui/phone/balloon.png")
