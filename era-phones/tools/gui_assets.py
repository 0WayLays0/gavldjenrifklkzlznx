"""The faces of the things (original art, drawn here), two texture pixels to a screen unit, drawn four times larger and
brought down so every edge is smooth: the video recorder's brushed aluminium front with its keys, the identity card's
laminated guilloche, the walnut radio with its printed dial, the walkie-talkie's rubber body, the newspaper's paper;
and the parts that move (a bakelite knob, a cassette reel, the card's sheen). Only what never changes is drawn here:
words that are read, and everything that moves, are drawn by the game over them."""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

import kit

K = 2          # texture pixels per screen unit
SS = 4         # drawn this many times larger, then brought down
FONTS = "/usr/share/fonts/truetype/dejavu/"


def font(name, size_units):
    return ImageFont.truetype(os.path.join(FONTS, name), max(6, int(size_units * K * SS)))


def canvas(w, h):
    return Image.new("RGBA", (w * K * SS, h * K * SS), (0, 0, 0, 0))


def finish(img, name):
    w, h = img.size
    kit.save(img.resize((w // SS, h // SS), Image.LANCZOS), f"textures/gui/gear/{name}.png")


def S(v):
    """Screen units to drawing pixels."""
    return v * K * SS


def box(x0, y0, x1, y1):
    return (S(x0), S(y0), S(x1), S(y1))


def vgrad(w, h, top, bottom):
    t = np.linspace(0, 1, h)[:, None, None]
    rgb = np.array(top[:3], float) * (1 - t) + np.array(bottom[:3], float) * t
    return np.broadcast_to(rgb, (h, w, 3)).copy()


def layer(rgb, alpha):
    a = np.clip(alpha, 0, 1) * 255
    return Image.fromarray(np.dstack([np.clip(rgb, 0, 255), a]).astype(np.uint8), "RGBA")


def mask_round(size, rect, r):
    m = Image.new("L", size, 0)
    ImageDraw.Draw(m).rounded_rectangle(rect, radius=r, fill=255)
    return m


def paste_masked(img, rgb, mask):
    """Puts {rgb} (an array the size of {img}) on {img} where {mask} is."""
    a = np.asarray(mask, float) / 255
    img.alpha_composite(layer(rgb, a))


def shadow(img, rect, r, blur, alpha=.5, dy=0):
    m = Image.new("L", img.size, 0)
    x0, y0, x1, y1 = rect
    ImageDraw.Draw(m).rounded_rectangle((x0, y0 + dy, x1, y1 + dy), radius=r, fill=int(255 * alpha))
    m = m.filter(ImageFilter.GaussianBlur(blur))
    sh = Image.new("RGBA", img.size, (0, 0, 0, 0))
    sh.putalpha(m)
    img.alpha_composite(sh)


def bevel(img, rect, r, light=60, dark=70, width=None):
    """A thin light edge along the top and a dark one along the bottom of a rounded shape."""
    width = width or S(.8)
    x0, y0, x1, y1 = rect
    d = ImageDraw.Draw(img, "RGBA")
    d.rounded_rectangle((x0, y0, x1, y1), radius=r, outline=(255, 255, 255, light), width=int(width))
    hl = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ImageDraw.Draw(hl).rounded_rectangle((x0, y0 + width, x1, y1), radius=r, outline=(0, 0, 0, dark), width=int(width))
    clip = Image.new("L", img.size, 0)
    ImageDraw.Draw(clip).rectangle((x0, (y0 + y1) / 2, x1, y1), fill=255)
    hl.putalpha(Image.fromarray(np.minimum(np.asarray(hl.split()[3]), np.asarray(clip))))
    img.alpha_composite(hl)


def brushed(w, h, base, rng, streak=10):
    """Brushed metal: fine horizontal streaks over a soft top-to-bottom light."""
    rgb = vgrad(w, h, tuple(min(255, c * 1.12) for c in base), tuple(c * .78 for c in base))
    lines = rng.normal(0, 1, (h, 1)) * streak
    fine = np.asarray(Image.fromarray(((rng.random((h, w)) - .5) * 255 + 128).astype(np.uint8)).filter(ImageFilter.BoxBlur(S(3))), float) - 128
    rgb += lines[..., None] + fine[..., None] * .25
    return rgb


def button(img, rect, base, glyph=None, glyph_color=(235, 236, 240), r=None):
    """A raised key: shadow under it, a soft dome of light, a crisp edge; its symbol drawn on it."""
    x0, y0, x1, y1 = rect
    r = r if r is not None else min(x1 - x0, y1 - y0) * .3
    shadow(img, rect, r, S(1.2), .55, dy=S(1.2))
    w, h = int(x1 - x0), int(y1 - y0)
    rgb = vgrad(w, h, tuple(min(255, c * 1.35) for c in base), tuple(c * .7 for c in base))
    piece = layer(rgb, np.ones((h, w)))
    m = mask_round((w, h), (0, 0, w - 1, h - 1), r)
    piece.putalpha(m)
    img.alpha_composite(piece, (int(x0), int(y0)))
    bevel(img, rect, r, light=90, dark=90)
    if glyph:
        glyph(ImageDraw.Draw(img, "RGBA"), (x0 + x1) / 2, (y0 + y1) / 2, min(w, h) * .32, glyph_color)


def tri(d, cx, cy, s, col, left=False, dx=0):
    if left:
        d.polygon([(cx + dx + s * .6, cy - s * .7), (cx + dx + s * .6, cy + s * .7), (cx + dx - s * .6, cy)], fill=col)
    else:
        d.polygon([(cx + dx - s * .6, cy - s * .7), (cx + dx - s * .6, cy + s * .7), (cx + dx + s * .6, cy)], fill=col)


GLYPHS = {
    "eject": lambda d, cx, cy, s, c: (d.polygon([(cx - s * .8, cy + s * .1), (cx + s * .8, cy + s * .1), (cx, cy - s * .8)], fill=c),
                                      d.rectangle((cx - s * .8, cy + s * .4, cx + s * .8, cy + s * .7), fill=c)),
    "rewind": lambda d, cx, cy, s, c: (tri(d, cx, cy, s * .8, c, True, -s * .45), tri(d, cx, cy, s * .8, c, True, s * .45)),
    "play": lambda d, cx, cy, s, c: tri(d, cx, cy, s * 1.1, c),
    "pause": lambda d, cx, cy, s, c: (d.rectangle((cx - s * .6, cy - s * .7, cx - s * .2, cy + s * .7), fill=c),
                                      d.rectangle((cx + s * .2, cy - s * .7, cx + s * .6, cy + s * .7), fill=c)),
    "stop": lambda d, cx, cy, s, c: d.rectangle((cx - s * .6, cy - s * .6, cx + s * .6, cy + s * .6), fill=c),
    "forward": lambda d, cx, cy, s, c: (tri(d, cx, cy, s * .8, c, False, -s * .45), tri(d, cx, cy, s * .8, c, False, s * .45)),
    "up": lambda d, cx, cy, s, c: d.polygon([(cx - s, cy + s * .5), (cx + s, cy + s * .5), (cx, cy - s * .6)], fill=c),
    "down": lambda d, cx, cy, s, c: d.polygon([(cx - s, cy - s * .5), (cx + s, cy - s * .5), (cx, cy + s * .6)], fill=c),
    "power": lambda d, cx, cy, s, c: (d.arc((cx - s, cy - s, cx + s, cy + s), 300, 240, fill=c, width=int(s * .3)),
                                      d.line((cx, cy - s * 1.1, cx, cy - s * .1), fill=c, width=int(s * .3))),
}


# ------------------------------------------------------------------------------------------------ the video recorder

VCR_KEYS = ["eject", "rewind", "play", "pause", "stop", "forward"]


def vcr():
    W, H = 320, 130
    img = canvas(W, H)
    rng = np.random.default_rng(3)
    body = box(2, 10, 318, 120)
    shadow(img, body, S(9), S(4), .6, dy=S(3))
    w, h = img.size
    rgb = brushed(w, h, (196, 200, 208), rng)
    paste_masked(img, rgb, mask_round(img.size, body, S(9)))
    # The dark lower band where the keys sit
    band = box(6, 84, 314, 116)
    rgb2 = brushed(w, h, (52, 54, 60), rng, 4)
    paste_masked(img, rgb2, mask_round(img.size, band, S(5)))
    bevel(img, band, S(5), light=30, dark=120)
    bevel(img, body, S(9), light=140, dark=110)
    d = ImageDraw.Draw(img, "RGBA")
    # The cassette door: a chrome frame, smoked plastic, a soft reflection
    door = box(14, 22, 138, 78)
    d.rounded_rectangle(door, radius=S(4), fill=(150, 154, 162, 255))
    bevel(img, door, S(4), light=150, dark=120)
    inner = box(17, 25, 135, 75)
    rgb3 = vgrad(w, h, (44, 46, 54), (16, 17, 20))
    paste_masked(img, rgb3, mask_round(img.size, inner, S(3)))
    refl = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ImageDraw.Draw(refl).polygon([(S(17), S(25)), (S(90), S(25)), (S(60), S(75)), (S(17), S(75))], fill=(255, 255, 255, 14))
    img.alpha_composite(refl)
    d.text((S(76), S(68)), "VIDEO CASSETTE", font=font("DejaVuSans.ttf", 3.2), fill=(150, 154, 166, 255), anchor="mm")
    # The display: black glass, a reflection, a frame
    disp = box(146, 22, 308, 64)
    d.rounded_rectangle(disp, radius=S(4), fill=(120, 124, 132, 255))
    dis2 = box(148, 24, 306, 62)
    rgb4 = vgrad(w, h, (16, 22, 30), (4, 6, 10))
    paste_masked(img, rgb4, mask_round(img.size, dis2, S(3)))
    ImageDraw.Draw(img, "RGBA").polygon([(S(148), S(24)), (S(306), S(24)), (S(306), S(32)), (S(148), S(40))], fill=(255, 255, 255, 10))
    # The indicator lamps under the display, unlit
    for k, label in enumerate(("PLAY", "REW", "FF", "TAPE")):
        cx = 160 + k * 40
        d.ellipse(box(cx - 2, 69, cx + 2, 73), fill=(40, 42, 46, 255), outline=(20, 20, 22, 255))
        d.text((S(cx + 4), S(71)), label, font=font("DejaVuSans-Bold.ttf", 3.6), fill=(70, 74, 82, 255), anchor="lm")
    # The keys, and the brand
    for i, k in enumerate(VCR_KEYS):
        x = 152 + i * 27
        button(img, box(x, 91, x + 24, 109), (150, 154, 162) if k != "play" else (160, 168, 176), GLYPHS[k], (40, 42, 48, 255))
    d = ImageDraw.Draw(img, "RGBA")
    d.text((S(14), S(14)), "VOXA", font=font("DejaVuSans-Bold.ttf", 6), fill=(70, 72, 80, 255), anchor="lm")
    d.text((S(44), S(14.5)), "VIDEO  HQ", font=font("DejaVuSans.ttf", 4), fill=(110, 114, 122, 255), anchor="lm")
    d.text((S(308), S(14.5)), "VHS", font=font("DejaVuSans-Bold.ttf", 5), fill=(70, 72, 80, 255), anchor="rm")
    d.text((S(70), S(100)), "VOXA  VR-80", font=font("DejaVuSans-Bold.ttf", 4.4), fill=(150, 152, 160, 255), anchor="mm")
    # Feet
    for x in (20, 296):
        d.rounded_rectangle(box(x - 8, 119, x + 8, 124), radius=S(1.5), fill=(20, 20, 22, 255))
    finish(img, "vcr")


def cassette_face():
    """The cassette as seen through the open door: black, its label, a window with the reels behind (drawn apart)."""
    W, H = 112, 44
    img = canvas(W, H)
    d = ImageDraw.Draw(img, "RGBA")
    d.rounded_rectangle(box(0, 0, W, H), radius=S(3), fill=(20, 20, 22, 255))
    d.rounded_rectangle(box(6, 4, W - 6, 17), radius=S(1.5), fill=(242, 236, 218, 255))
    d.line((S(9), S(8), S(W - 9), S(8)), fill=(200, 60, 50, 255), width=int(S(.6)))
    d.rounded_rectangle(box(26, 21, W - 26, 40), radius=S(3), fill=(54, 46, 44, 255), outline=(10, 10, 10, 255), width=int(S(.5)))
    for cx in (40, W - 40):
        d.ellipse(box(cx - 8, 22.5, cx + 8, 38.5), fill=(28, 24, 22, 255))
    d.rectangle(box(48, 28, W - 48, 32), fill=(70, 50, 36, 255))
    finish(img, "vcr_tape")


def reel():
    W = 32
    img = canvas(W, W)
    d = ImageDraw.Draw(img, "RGBA")
    c = S(W / 2)
    d.ellipse((c - S(14), c - S(14), c + S(14), c + S(14)), fill=(46, 30, 22, 255))
    for k in range(6):
        a = k * math.pi / 3
        d.line((c, c, c + math.cos(a) * S(6), c + math.sin(a) * S(6)), fill=(236, 236, 240, 255), width=int(S(1.6)))
    d.ellipse((c - S(6.5), c - S(6.5), c + S(6.5), c + S(6.5)), outline=(236, 236, 240, 255), width=int(S(1.4)))
    d.ellipse((c - S(2), c - S(2), c + S(2), c + S(2)), fill=(20, 20, 20, 255))
    finish(img, "reel")


# ------------------------------------------------------------------------------------------------ the identity card

def card():
    W, H = 300, 190
    img = canvas(W, H)
    w, h = img.size
    body = box(0, 0, 296, 182)
    shadow(img, body, S(12), S(5), .55, dy=S(3))
    rgb = vgrad(w, h, (238, 242, 230), (204, 220, 208))
    ys, xs = np.mgrid[0:h, 0:w].astype(float) / (K * SS)
    # The guilloche: two families of fine waves crossing
    g1 = np.abs(np.sin(ys * .9 + np.sin(xs * .045) * 3.2)) < .07
    g2 = np.abs(np.sin(ys * .9 - np.sin(xs * .05 + 1.3) * 3.6)) < .07
    rosette = np.hypot(xs - 238, ys - 104)
    g3 = (np.abs(np.sin(rosette * .5 + np.arctan2(ys - 104, xs - 238) * 6)) < .1) & (rosette < 38)
    rgb[g1 | g2] = rgb[g1 | g2] * .9 + np.array([60, 110, 90]) * .1
    rgb[g3] = rgb[g3] * .86 + np.array([90, 120, 170]) * .14
    paste_masked(img, rgb, mask_round(img.size, body, S(12)))
    # The band at the top, with a seal
    band = Image.new("RGBA", img.size, (0, 0, 0, 0))
    brgb = vgrad(w, h, (50, 96, 150), (26, 58, 102))
    bm = Image.new("L", img.size, 0)
    ImageDraw.Draw(bm).rounded_rectangle(body, radius=S(12), fill=255)
    bm = Image.fromarray(np.where(ys < 27, np.asarray(bm), 0).astype(np.uint8))
    paste_masked(img, brgb, bm)
    d = ImageDraw.Draw(img, "RGBA")
    d.line((0, S(27), S(296), S(27)), fill=(200, 170, 80, 255), width=int(S(.8)))
    cx, cy = 272, 13.5
    d.ellipse(box(cx - 10, cy - 10, cx + 10, cy + 10), fill=(214, 184, 96, 255), outline=(150, 120, 50, 255), width=int(S(.8)))
    for k in range(5):
        a = -math.pi / 2 + k * 2 * math.pi / 5
        a2 = a + math.pi / 5
        pts = []
        for j in range(5):
            o = -math.pi / 2 + j * 2 * math.pi / 5
            pts.append((S(cx + math.cos(o) * 6), S(cy + math.sin(o) * 6)))
            pts.append((S(cx + math.cos(o + math.pi / 5) * 2.5), S(cy + math.sin(o + math.pi / 5) * 2.5)))
        d.polygon(pts, fill=(150, 112, 40, 255))
        break
    # The photograph's frame
    shadow(img, box(14, 36, 88, 132), S(3), S(1.5), .4, dy=S(1))
    d.rounded_rectangle(box(14, 36, 88, 132), radius=S(3), fill=(255, 255, 255, 255))
    d.rectangle(box(18, 40, 84, 128), fill=(170, 184, 196, 255))
    # The writing lines and the reading strip at the bottom
    for y in (54, 82):
        d.line((S(100), S(y), S(284), S(y)), fill=(120, 150, 130, 120), width=int(S(.5)))
    d.line((S(100), S(160), S(220), S(160)), fill=(60, 80, 70, 160), width=int(S(.5)))
    strip = box(0, 166, 296, 182)
    sm = Image.new("L", img.size, 0)
    ImageDraw.Draw(sm).rounded_rectangle(body, radius=S(12), fill=255)
    sm = Image.fromarray(np.where(ys > 166, np.asarray(sm), 0).astype(np.uint8))
    paste_masked(img, vgrad(w, h, (226, 230, 226), (226, 230, 226)), sm)
    # The laminate: a faint edge and a gloss along the top
    bevel(img, body, S(12), light=170, dark=60)
    finish(img, "card")


def sheen():
    """A soft diagonal gleam, passed over the card."""
    W, H = 80, 200
    img = canvas(W, H)
    w, h = img.size
    xs = np.mgrid[0:h, 0:w][1].astype(float) / w
    a = np.exp(-((xs - .5) / .18) ** 2) * .22
    img.alpha_composite(layer(np.full((h, w, 3), 255.0), a))
    finish(img, "sheen")


# ------------------------------------------------------------------------------------------------ the radio

DX, DY, DW, DH = 134, 22, 148, 60


def fx(freq):
    return DX + 8 + (freq - 880) / 200 * (DW - 16)


def walnut(w, h, rng):
    ys, xs = np.mgrid[0:h, 0:w].astype(float)
    grain = np.sin(ys / S(1.1) + np.sin(xs / S(60)) * 1.6 + np.sin(xs / S(17) + ys / S(30)) * .5)
    fibre = np.asarray(Image.fromarray((rng.random((h // 8, w)) * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC), float) / 255
    t = (grain * .5 + .5) * .32 + fibre * .28 + .2
    dark, light = np.array([70, 38, 18]), np.array([142, 88, 46])
    rgb = dark * (1 - t[..., None]) + light * t[..., None]
    rgb *= (1.1 - .25 * ys / h)[..., None]
    return rgb


def radio():
    W, H = 300, 170
    img = canvas(W, H)
    rng = np.random.default_rng(5)
    w, h = img.size
    body = box(0, 0, 300, 170)
    shadow(img, box(4, 6, 296, 168), S(16), S(4), .6, dy=S(3))
    paste_masked(img, walnut(w, h, rng), mask_round(img.size, box(2, 2, 298, 166), S(16)))
    bevel(img, box(2, 2, 298, 166), S(16), light=90, dark=140)
    d = ImageDraw.Draw(img, "RGBA")
    # An inner brass trim
    d.rounded_rectangle(box(9, 9, 291, 159), radius=S(11), outline=(196, 160, 90, 200), width=int(S(.8)))
    # The speaker: a recess, the woven cloth, a soft light down it
    d.rounded_rectangle(box(15, 15, 119, 155), radius=S(9), fill=(40, 22, 10, 255))
    cloth = box(19, 19, 115, 151)
    ys, xs = np.mgrid[0:h, 0:w].astype(float) / (K * SS)
    weave = (np.sin(xs * 3.2) * np.sin(ys * 3.2)) * 10 + (np.sin((xs + ys) * 1.1)) * 4
    crgb = vgrad(w, h, (196, 176, 136), (150, 128, 92)) + weave[..., None]
    light = np.exp(-((xs - 50) / 40) ** 2)[..., None] * 18
    paste_masked(img, crgb + light, mask_round(img.size, cloth, S(7)))
    for x in range(30, 110, 16):
        d.line((S(x), S(19), S(x), S(151)), fill=(120, 90, 50, 70), width=int(S(1.6)))
    d.text((S(67), S(140)), "VOXA", font=font("DejaVuSerif-Bold.ttf", 8), fill=(70, 40, 16, 255), anchor="mm")
    # The dial: a brass frame, cream glass, the printed scale
    d.rounded_rectangle(box(DX - 6, DY - 6, DX + DW + 6, DY + DH + 6), radius=S(7), fill=(176, 136, 70, 255))
    bevel(img, box(DX - 6, DY - 6, DX + DW + 6, DY + DH + 6), S(7), light=160, dark=120)
    paste_masked(img, vgrad(w, h, (246, 232, 190), (220, 196, 140)), mask_round(img.size, box(DX, DY, DX + DW, DY + DH), S(5)))
    d = ImageDraw.Draw(img, "RGBA")
    for f in range(880, 1081, 5):
        x = fx(f)
        major = f % 20 == 0
        d.line((S(x), S(DY + 12), S(x), S(DY + (25 if major else 19))), fill=(70, 46, 26, 255), width=int(S(.45 if not major else .7)))
        if major:
            d.text((S(x), S(DY + 31)), str(f // 10), font=font("DejaVuSans.ttf", 5.2), fill=(70, 46, 26, 255), anchor="mm")
    d.line((S(DX + 8), S(DY + 12), S(DX + DW - 8), S(DY + 12)), fill=(70, 46, 26, 255), width=int(S(.5)))
    d.text((S(DX + 7), S(DY + 6)), "FM", font=font("DejaVuSans-Bold.ttf", 4.5), fill=(110, 70, 30, 255), anchor="lm")
    d.text((S(DX + DW - 7), S(DY + 6)), "MHz", font=font("DejaVuSans.ttf", 4), fill=(110, 70, 30, 255), anchor="rm")
    # The knobs' plates and the magic eye's bezel
    for cx in (152, 204, 256):
        d.ellipse(box(cx - 18, 110, cx + 18, 146), fill=(60, 34, 16, 120))
        d.ellipse(box(cx - 17, 111, cx + 17, 145), outline=(196, 160, 90, 180), width=int(S(.6)))
    d.ellipse(box(274, 120, 290, 136), fill=(176, 136, 70, 255))
    d.ellipse(box(276, 122, 288, 134), fill=(10, 20, 12, 255))
    finish(img, "radio")


def knob():
    """A bakelite knob seen from the front: fluted rim, a domed cap, a white line to read it by."""
    W = 40
    img = canvas(W, W)
    w, h = img.size
    c = w / 2
    ys, xs = np.mgrid[0:h, 0:w].astype(float)
    r = np.hypot(xs - c, ys - c) / (w / 2)
    ang = np.arctan2(ys - c, xs - c)
    flute = .9 + .1 * np.sign(np.sin(ang * 18))
    base = np.array([48, 28, 16])
    light = np.clip(1.15 - .6 * ((xs - c * .7) ** 2 + (ys - c * .6) ** 2) ** .5 / w, .4, 1.3)
    rgb = base[None, None, :] * (light * np.where(r > .78, flute, 1))[..., None]
    rgb += np.exp(-((xs - c * .75) ** 2 + (ys - c * .7) ** 2) / (w * .08) ** 2)[..., None] * 90
    a = np.clip((1 - r) * 40, 0, 1)
    img.alpha_composite(layer(rgb, a))
    d = ImageDraw.Draw(img, "RGBA")
    d.line((c, c - w * .1, c, c - w * .42), fill=(240, 228, 200, 255), width=int(w * .07))
    finish(img, "knob")


# ------------------------------------------------------------------------------------------------ the walkie-talkie

def walkie():
    W, H = 120, 230
    img = canvas(W, H)
    rng = np.random.default_rng(7)
    w, h = img.size
    # The aerial and the knob, behind the body
    d = ImageDraw.Draw(img, "RGBA")
    d.rounded_rectangle(box(84, 0, 96, 36), radius=S(5), fill=(26, 28, 26, 255))
    d.rounded_rectangle(box(20, 16, 42, 32), radius=S(4), fill=(30, 30, 32, 255))
    for x in range(22, 42, 3):
        d.line((S(x), S(17), S(x), S(31)), fill=(60, 60, 64, 255), width=int(S(.8)))
    body = box(6, 26, 114, 226)
    shadow(img, body, S(18), S(4), .6, dy=S(3))
    rgb = vgrad(w, h, (92, 102, 76), (44, 50, 38))
    rgb += (rng.random((h, w, 1)) - .5) * 14
    paste_masked(img, rgb, mask_round(img.size, body, S(18)))
    bevel(img, body, S(18), light=60, dark=120)
    d = ImageDraw.Draw(img, "RGBA")
    # The speaker grille: a recess drilled with holes
    d.rounded_rectangle(box(16, 38, 104, 82), radius=S(8), fill=(30, 34, 26, 255))
    for row in range(6):
        for col in range(13):
            x = 23 + col * 6 + (3 if row % 2 else 0)
            y = 44 + row * 6.4
            if x < 100:
                d.ellipse(box(x - 1.6, y - 1.6, x + 1.6, y + 1.6), fill=(8, 10, 8, 255))
    # The display's bezel and glass, unlit
    d.rounded_rectangle(box(16, 92, 104, 138), radius=S(6), fill=(14, 16, 12, 255))
    paste_masked(img, vgrad(w, h, (78, 92, 64), (56, 68, 46)), mask_round(img.size, box(20, 96, 100, 134), S(4)))
    # The keys
    button(img, box(18, 160, 42, 184), (170, 40, 36), GLYPHS["power"], (255, 236, 230, 255), r=S(12))
    button(img, box(52, 150, 102, 172), (66, 72, 58), GLYPHS["up"], (220, 224, 210, 255))
    button(img, box(52, 176, 102, 198), (66, 72, 58), GLYPHS["down"], (220, 224, 210, 255))
    d = ImageDraw.Draw(img, "RGBA")
    d.text((S(60), S(88)), "VOXA  TK-16", font=font("DejaVuSans-Bold.ttf", 3.6), fill=(150, 160, 130, 255), anchor="mm")
    finish(img, "walkie")


# ------------------------------------------------------------------------------------------------ the newspaper

def paper():
    W, H = 260, 300
    img = canvas(W, H)
    rng = np.random.default_rng(31)
    w, h = img.size
    shadow(img, box(0, 0, 256, 296), S(2), S(4), .55, dy=S(3))
    ys, xs = np.mgrid[0:h, 0:w].astype(float) / (K * SS)
    rgb = vgrad(w, h, (244, 236, 214), (226, 214, 184))
    fibres = np.asarray(Image.fromarray((rng.random((h // 4, w // 4)) * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC), float) / 255
    rgb += (fibres[..., None] - .5) * 12
    # Yellowed edges, a crease across the middle
    edge = np.minimum(np.minimum(xs, 256 - xs), np.minimum(ys, 296 - ys))
    rgb -= (np.exp(-edge / 10) * 30)[..., None] * np.array([.6, .8, 1.2])
    crease = np.exp(-((ys - 148) / 1.2) ** 2)
    rgb -= crease[..., None] * 18
    rgb += np.exp(-((ys - 150.5) / 1.6) ** 2)[..., None] * 8
    for _ in range(26):
        cx, cy, r = rng.uniform(0, 256), rng.uniform(0, 296), rng.uniform(1, 4)
        spot = np.exp(-((xs - cx) ** 2 + (ys - cy) ** 2) / (r * r))
        rgb -= spot[..., None] * np.array([20, 30, 50]) * rng.uniform(.3, .8)
    paste_masked(img, rgb, mask_round(img.size, box(0, 0, 256, 296), S(2)))
    d = ImageDraw.Draw(img, "RGBA")
    d.line((S(12), S(28), S(244), S(28)), fill=(26, 20, 16, 255), width=int(S(1)))
    d.line((S(12), S(31), S(244), S(31)), fill=(26, 20, 16, 255), width=int(S(.4)))
    d.line((S(12), S(43), S(244), S(43)), fill=(26, 20, 16, 160), width=int(S(.4)))
    finish(img, "paper")


# ------------------------------------------------------------------------------------------------ the sheets of paper

NOTE_W, NOTE_H = 200, 260
NOTE_STYLES = 8


def fibre_noise(h, w, rng, scales=((6, .5), (24, .3), (80, .2))):
    """Paper's grain: noise at several sizes, the fine fibres stretched a little along the sheet."""
    out = np.zeros((h, w))
    for div, weight in scales:
        small = rng.random((max(2, h // div), max(2, w // (div * 2))))
        out += np.asarray(Image.fromarray((small * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC), float) / 255 * weight
    return out


def creases(h, w, rng, count, strength):
    """Folds and crumples: lines across the sheet, light on one side, dark on the other."""
    ys, xs = np.mgrid[0:h, 0:w].astype(float)
    shade = np.zeros((h, w))
    for _ in range(count):
        a = rng.uniform(0, math.pi)
        nx, ny = math.cos(a), math.sin(a)
        c = rng.uniform(.2, .8) * w * nx + rng.uniform(.2, .8) * h * ny
        dist = xs * nx + ys * ny - c
        width = S(rng.uniform(.6, 1.6))
        shade += np.tanh(dist / width) * np.exp(-(dist / (width * 3)) ** 2) * strength * rng.uniform(.5, 1)
    return shade


def stain(h, w, cx, cy, r, rng):
    """A coffee ring: a darker rim, a faint inside."""
    ys, xs = np.mgrid[0:h, 0:w].astype(float)
    d = np.hypot(xs - cx, ys - cy) / r
    wob = 1 + .012 * np.sin(np.arctan2(ys - cy, xs - cx) * 3 + rng.uniform(0, 6))
    d = d * wob
    return np.exp(-((d - 1) / .035) ** 2) * .8 + (d < 1) * .18


def note(style):
    """A page to write on: a notebook leaf, a letter, an old parchment, a sticky note, a torn page, graph paper, an
    index card, an old stained sheet."""
    W, H = NOTE_W, NOTE_H
    img = canvas(W, H)
    rng = np.random.default_rng(200 + style)
    w, h = img.size
    ys, xs = np.mgrid[0:h, 0:w].astype(float) / (K * SS)
    colours = {0: ((252, 252, 248), (240, 240, 232)), 1: ((248, 243, 228), (236, 228, 206)), 2: ((232, 208, 156), (200, 166, 108)),
               3: ((255, 240, 128), (248, 218, 86)), 4: ((250, 250, 246), (236, 236, 228)), 5: ((250, 252, 250), (238, 242, 238)),
               6: ((252, 250, 244), (242, 238, 228)), 7: ((238, 226, 196), (214, 196, 156))}
    top, bottom = colours[style]
    rgb = vgrad(w, h, top, bottom)
    grain = fibre_noise(h, w, rng)
    rgb += (grain[..., None] - .5) * (34 if style in (2, 7) else 14)
    # The sheet's outline
    m = Image.new("L", img.size, 0)
    md = ImageDraw.Draw(m)
    if style == 3:
        md.rectangle(box(14, 30, 186, 202), fill=255)
    elif style == 6:
        md.rounded_rectangle(box(6, 50, 194, 186), radius=S(2), fill=255)
    elif style == 4:
        pts = [(S(4), S(256)), (S(196), S(256)), (S(196), S(16))]
        x = 196.0
        while x > 4:
            pts.append((S(x), S(12 + rng.uniform(0, 6) + 2 * math.sin(x * .07))))
            x -= rng.uniform(1.2, 4)
        pts.append((S(4), S(14)))
        md.polygon(pts, fill=255)
    elif style in (2, 7):
        pts = []
        n = 240
        for k in range(n):
            t = k / n * 4
            side = int(t)
            f = t - side
            amp = 2.6 if style == 2 else 1.2
            j = rng.uniform(-amp, amp) + math.sin(k * .3) * amp * .5
            if side == 0:
                x, y = 6 + f * 188, 6 + j
            elif side == 1:
                x, y = 194 + j, 6 + f * 248
            elif side == 2:
                x, y = 194 - f * 188, 254 + j
            else:
                x, y = 6 + j, 254 - f * 248
            pts.append((S(x), S(y)))
        md.polygon(pts, fill=255)
    else:
        md.rounded_rectangle(box(4, 4, 196, 256), radius=S(1.5), fill=255)
    # Holes punched through the notebook leaf
    if style == 0:
        for y in (40, 130, 220):
            md.ellipse(box(10, y - 3.6, 17.2, y + 3.6), fill=0)
    # The shadow under the sheet, softer away from the light
    sh = m.filter(ImageFilter.GaussianBlur(S(3.5)))
    shl = Image.new("RGBA", img.size, (0, 0, 0, 0))
    shl.putalpha(Image.fromarray((np.asarray(sh, float) * .45).astype(np.uint8)))
    img.alpha_composite(shl, (int(S(1.5)), int(S(3))))
    inside = np.asarray(m, float) / 255
    edge = np.asarray(m.filter(ImageFilter.GaussianBlur(S(7))), float) / 255
    # Light falls from the top left; the edges darken a touch; old papers darken and brown at their edges
    rgb *= (1.03 - .07 * (xs / W + ys / H) / 2)[..., None]
    if style in (2, 7):
        rgb *= (.5 + .5 * edge ** .7)[..., None]
        rgb -= ((1 - edge) * 40)[..., None] * np.array([.2, .5, 1])
    if style in (2, 4, 7):
        rgb += creases(h, w, rng, 7 if style != 4 else 4, 18)[..., None]
    if style == 7:
        for _ in range(2):
            st = stain(h, w, S(rng.uniform(50, 160)), S(rng.uniform(150, 230)), S(rng.uniform(18, 26)), rng)
            rgb -= st[..., None] * np.array([30, 48, 70])
        for _ in range(30):
            cx, cy, r = rng.uniform(10, 190), rng.uniform(10, 250), rng.uniform(.8, 3)
            sp = np.exp(-((xs - cx) ** 2 + (ys - cy) ** 2) / (r * r))
            rgb -= sp[..., None] * np.array([30, 45, 70]) * rng.uniform(.3, .9)
    if style == 3:
        # The sticky band, and the bottom corner lifting off the board
        band = (ys >= 30) & (ys < 44)
        rgb[band] *= .97
        curl = np.clip(((xs - 150) + (ys - 166)) / 36, 0, 1)
        rgb += (curl ** 2 * 26)[..., None]
        rgb -= (np.clip(1 - np.abs((xs - 150) + (ys - 166) - 34) / 3, 0, 1) * 30)[..., None]
    paste_masked(img, rgb, m)
    d = ImageDraw.Draw(img, "RGBA")
    if style in (0, 4):
        d.line((S(8), S(30 if style == 0 else 34), S(192), S(30 if style == 0 else 34)), fill=(120, 160, 220, 110), width=int(S(.4)))
        for y in range(44, 252, 10):
            d.line((S(8), S(y), S(192), S(y)), fill=(110, 150, 214, 150), width=int(S(.45)))
        d.line((S(30), S(16 if style == 4 else 5), S(30), S(255)), fill=(222, 86, 86, 170), width=int(S(.55)))
        if style == 0:
            for y in (40, 130, 220):
                d.ellipse(box(9.6, y - 4, 17.6, y + 4), outline=(190, 190, 186, 255), width=int(S(.5)))
    elif style == 1:
        d.rectangle(box(11, 11, 189, 249), outline=(170, 146, 100, 150), width=int(S(.5)))
        d.rectangle(box(13, 13, 187, 247), outline=(170, 146, 100, 90), width=int(S(.3)))
        for cx, cy in ((13, 13), (187, 13), (13, 247), (187, 247)):
            d.ellipse(box(cx - 2.2, cy - 2.2, cx + 2.2, cy + 2.2), outline=(170, 146, 100, 160), width=int(S(.4)))
        d.line((S(30), S(36), S(170), S(36)), fill=(170, 146, 100, 160), width=int(S(.5)))
        # A watermark in the middle of the sheet
        wm = Image.new("RGBA", img.size, (0, 0, 0, 0))
        wd = ImageDraw.Draw(wm)
        wd.ellipse(box(70, 100, 130, 160), outline=(160, 140, 100, 26), width=int(S(2)))
        wd.text((S(100), S(130)), "V", font=font("DejaVuSerif-Bold.ttf", 34), fill=(160, 140, 100, 22), anchor="mm")
        img.alpha_composite(wm)
    elif style == 5:
        for k in range(0, 256, 5):
            major = k % 25 == 0
            col = (120, 170, 150, 110 if major else 55)
            if 4 < k < 256:
                d.line((S(4), S(k), S(196), S(k)), fill=col, width=int(S(.35 if major else .25)))
            if 4 < k < 196:
                d.line((S(k), S(4), S(k), S(256)), fill=col, width=int(S(.35 if major else .25)))
    elif style == 6:
        d.line((S(8), S(64), S(192), S(64)), fill=(220, 80, 80, 200), width=int(S(.6)))
        for y in range(74, 184, 9):
            d.line((S(8), S(y), S(192), S(y)), fill=(110, 150, 214, 140), width=int(S(.4)))
    finish(img, f"note_{style}")


def main():
    for k in range(NOTE_STYLES):
        note(k)
    vcr()
    cassette_face()
    reel()
    card()
    sheen()
    radio()
    knob()
    walkie()
    paper()


if __name__ == "__main__":
    main()
