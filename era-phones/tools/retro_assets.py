"""The older phones' art: the bodies of the 1997 GSM (charcoal plastic, stub aerial), the 2004 flip phone (brushed
silver, opened on its hinge) and the 1985 radiotelephone (beige brick, long aerial); the GSM's one-bit menu icons, the
flip phone's glossy ones and its four wallpapers. Drawn at 3 pixels a unit. Original art."""
import math

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

import kit
from kit import S, W_, ell, poly


def _rr(d, box, r, fill, outline=None, width=1):
    d.rounded_rectangle(tuple(v * S for v in box), radius=r * S, fill=fill, outline=outline, width=width)


def _over(base, layer):
    return Image.alpha_composite(base, layer)


def _tint(img, box, r, fill):
    """A translucent rounded panel laid over {img} (blended, not punched through)."""
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ImageDraw.Draw(layer).rounded_rectangle(tuple(v * S for v in box), radius=r * S, fill=fill)
    return Image.alpha_composite(img, layer)


def gsm_body():
    w, h = 120, 260
    img = Image.new("RGBA", (w * S, h * S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # The stub aerial
    _rr(d, (88, 0, 100, 20), 4, (24, 24, 28, 255))
    _rr(d, (89, 1, 93, 18), 2, (70, 70, 78, 255))
    img = _over(img, kit.shell(w, h, (6, 14, 114, 254), 16, (.13, .14, .16), (.25, .26, .29), (.62, .64, .68), 6101, brushed=False))
    d = ImageDraw.Draw(img)
    # The earpiece's slots
    for k in range(4):
        _rr(d, (46 + k * 8, 24, 50 + k * 8, 30), 1, (8, 8, 10, 255))
    # The screen's bezel and window
    _rr(d, (12, 38, 108, 112), 7, (30, 32, 36, 255), outline=(90, 94, 100, 255), width=2)
    _rr(d, (17, 45, 103, 107), 3, (10, 12, 10, 255))
    # The keys' recess and the microphone
    img = _tint(img, (10, 110, 110, 240), 10, (10, 10, 12, 110))
    d = ImageDraw.Draw(img)
    for k in range(3):
        ell(d, S, 54 + k * 6, 248, .9, .9, fill=(6, 6, 8, 255))
    return img


def flip_body():
    w, h = 140, 320
    img = Image.new("RGBA", (w * S, h * S), (0, 0, 0, 0))
    silver = ((.48, .52, .58), (.74, .78, .84), (.97, .98, 1.0))
    img = _over(img, kit.shell(w, h, (6, 4, 134, 160), 14, *silver, 6201))
    img = _over(img, kit.shell(w, h, (6, 166, 134, 316), 14, *silver, 6202))
    d = ImageDraw.Draw(img)
    # The hinge, a cylinder
    hinge = Image.new("RGBA", (w * S, h * S), (0, 0, 0, 0))
    hd = ImageDraw.Draw(hinge)
    for k in range(14 * S):
        t = k / (14 * S)
        c = int(70 + 150 * math.sin(t * math.pi) ** 2)
        hd.line((14 * S, 157 * S + k, 126 * S, 157 * S + k), fill=(c, c + 4, c + 10, 255))
    img = _over(img, hinge)
    d = ImageDraw.Draw(img)
    # The lid: the earpiece, the screen's black glass
    _rr(d, (60, 10, 80, 14), 2, (20, 22, 26, 255))
    _rr(d, (15, 23, 125, 160), 6, (6, 8, 12, 255), outline=(40, 46, 56, 255), width=2)
    # The base: the keys' panel of darker metal
    img = _tint(img, (11, 168, 129, 312), 10, (30, 36, 46, 70))
    d = ImageDraw.Draw(img)
    for k in range(3):
        ell(d, S, 64 + k * 6, 306, .8, .8, fill=(20, 22, 26, 255))
    return img


def brick_body():
    w, h = 120, 380
    img = Image.new("RGBA", (w * S, h * S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # The aerial: a black rod, a knob at its top
    _rr(d, (16, 4, 21, 100), 2, (26, 24, 22, 255))
    _rr(d, (17, 6, 18.4, 98), 1, (80, 76, 70, 255))
    _rr(d, (14, 0, 23, 8), 4, (20, 18, 16, 255))
    img = _over(img, kit.shell(w, h, (4, 96, 116, 376), 12, (.50, .47, .42), (.70, .66, .60), (.92, .90, .85), 6301, brushed=False))
    d = ImageDraw.Draw(img)
    # The earpiece's grill
    for r in range(3):
        for c in range(9):
            ell(d, S, 32 + c * 7, 104 + r * 5, 1.1, 1.1, fill=(40, 36, 32, 255))
    # The smoked glass over the figures and lamps
    _rr(d, (12, 120, 108, 162), 4, (24, 10, 8, 255), outline=(70, 62, 54, 255), width=3)
    # The keys' recess, the microphone's grill
    img = _tint(img, (12, 165, 108, 326), 6, (40, 34, 28, 80))
    d = ImageDraw.Draw(img)
    for r in range(4):
        for c in range(7):
            ell(d, S, 39 + c * 7, 340 + r * 5, 1, 1, fill=(50, 46, 40, 255))
    return img


# ------------------------------------------------------------------------------------------------ GSM icons (24 x 16)

GSM_ICONS = {
    "messages": [
        "........................",
        "..####################..",
        "..##................##..",
        "..#.#..............#.#..",
        "..#..#............#..#..",
        "..#...#..........#...#..",
        "..#....#........#....#..",
        "..#.....#......#.....#..",
        "..#......#....#......#..",
        "..#.......####.......#..",
        "..#..................#..",
        "..#..................#..",
        "..####################..",
        "........................",
        "........................",
        "........................"],
    "contacts": [
        "........................",
        ".....##############.....",
        ".....#............##....",
        ".....#.....###....##....",
        ".....#....#####...##....",
        ".....#....#####...##....",
        ".....#.....###....##....",
        ".....#...#######..##....",
        ".....#..#########.##....",
        ".....#..#########.##....",
        ".....#............##....",
        ".....##############.....",
        "........................",
        "........................",
        "........................",
        "........................"],
    "calls": [
        "........................",
        "..######.........####...",
        ".########.........###...",
        ".##....##........#.##...",
        ".##....##.......#...#...",
        "..........#....#........",
        "..........##..#.........",
        "..........###...........",
        ".........#.###..........",
        "........#...###.........",
        ".##....##.....##........",
        ".##....##...............",
        ".########...............",
        "..######................",
        "........................",
        "........................"],
    "snake": [
        "........................",
        "........................",
        "..##########............",
        "..##########............",
        "..........##............",
        "..........##..##########",
        "..........##..##########",
        "..........##..##........",
        "..........######........",
        "..........######........",
        "........................",
        "...##...................",
        "...##...................",
        "........................",
        "........................",
        "........................"],
    "calc": [
        "........................",
        ".......##########.......",
        ".......#........#.......",
        ".......#.######.#.......",
        ".......#.######.#.......",
        ".......#........#.......",
        ".......#.##.##.##.......",
        ".......#........#.......",
        ".......#.##.##.##.......",
        ".......#........#.......",
        ".......#.##.##.##.......",
        ".......#........#.......",
        ".......##########.......",
        "........................",
        "........................",
        "........................"],
    "ring": [
        "........................",
        "...........##...........",
        ".........######.........",
        "........########........",
        "........########........",
        "........########........",
        ".......##########.......",
        ".......##########.......",
        "......############......",
        ".....##############.....",
        ".....##############.....",
        "...........##...........",
        "..#.......####.......#..",
        ".#....................#.",
        "........................",
        "........................"],
    "number": [
        "........................",
        "........##########......",
        ".......##........#......",
        "......##.........#......",
        "......#...#..#...#......",
        "......#..######..#......",
        "......#...#..#...#......",
        "......#...#..#...#......",
        "......#..######..#......",
        "......#...#..#...#......",
        "......#..........#......",
        "......############......",
        "........................",
        "........................",
        "........................",
        "........................"],
}


def gsm_icon(rows):
    img = Image.new("RGBA", (24, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == 24, (row, len(row))
        for x, ch in enumerate(row):
            if ch == "#":
                img.putpixel((x, y), (255, 255, 255, 255))
    return img


# ------------------------------------------------------------------------------------------------ flip phone icons (32)

def flip_icons():
    def envelope(d, s, img):
        d.rounded_rectangle((.16 * s, .28 * s, .84 * s, .74 * s), radius=.05 * s, fill=W_)
        d.line([(.17 * s, .3 * s), (.5 * s, .54 * s), (.83 * s, .3 * s)], fill=(60, 120, 210, 255), width=int(s * .06))

    def book(d, s, img):
        d.rounded_rectangle((.24 * s, .16 * s, .76 * s, .84 * s), radius=.05 * s, fill=W_)
        d.rectangle((.24 * s, .16 * s, .32 * s, .84 * s), fill=(200, 210, 230, 255))
        ell(d, s, .54, .4, .1, .1, fill=(80, 90, 120, 255))
        d.pieslice((.38 * s, .52 * s, .7 * s, .82 * s), 180, 360, fill=(80, 90, 120, 255))

    def calls(d, s, img):
        d.arc((.2 * s, .2 * s, .8 * s, .8 * s), 120, 330, fill=W_, width=int(s * .14))
        poly(d, s, [(.6, .2), (.82, .2), (.82, .42)], (255, 230, 120, 255))

    def camera(d, s, img):
        d.rounded_rectangle((.14 * s, .3 * s, .86 * s, .76 * s), radius=.08 * s, fill=(40, 40, 46, 255))
        ell(d, s, .5, .53, .17, .17, fill=(200, 210, 230, 255))
        ell(d, s, .5, .53, .1, .1, fill=(30, 60, 120, 255))
        d.rectangle((.66 * s, .22 * s, .8 * s, .3 * s), fill=(40, 40, 46, 255))

    def gallery(d, s, img):
        d.rectangle((.16 * s, .22 * s, .84 * s, .78 * s), fill=W_)
        d.rectangle((.2 * s, .26 * s, .8 * s, .74 * s), fill=(120, 190, 255, 255))
        poly(d, s, [(.2, .74), (.4, .46), (.56, .62), (.66, .52), (.8, .74)], (40, 140, 70, 255))
        ell(d, s, .66, .38, .06, .06, fill=(255, 240, 150, 255))

    def snake(d, s, img):
        pts = [(.2, .7), (.2, .5), (.45, .5), (.45, .3), (.75, .3), (.75, .6)]
        d.line([(x * s, y * s) for x, y in pts], fill=(120, 255, 120, 255), width=int(s * .12), joint="curve")
        ell(d, s, .75, .68, .06, .06, fill=(255, 80, 80, 255))

    def ring(d, s, img):
        ell(d, s, .38, .7, .12, .09, fill=W_)
        d.line((.49 * s, .7 * s, .49 * s, .24 * s), fill=W_, width=int(s * .07))
        poly(d, s, [(.49, .22), (.78, .32), (.78, .44), (.49, .34)], W_)

    def calc(d, s, img):
        d.rounded_rectangle((.24 * s, .16 * s, .76 * s, .84 * s), radius=.05 * s, fill=(40, 44, 52, 255))
        d.rectangle((.3 * s, .22 * s, .7 * s, .36 * s), fill=(170, 230, 170, 255))
        for r in range(3):
            for c in range(3):
                d.rectangle(((.31 + c * .14) * s, (.44 + r * .12) * s, (.41 + c * .14) * s, (.52 + r * .12) * s), fill=W_)

    def settings(d, s, img):
        for k in range(8):
            a = k * math.tau / 8
            poly(d, s, [(.5 + math.cos(a - .2) * .22, .5 + math.sin(a - .2) * .22), (.5 + math.cos(a - .12) * .34, .5 + math.sin(a - .12) * .34),
                        (.5 + math.cos(a + .12) * .34, .5 + math.sin(a + .12) * .34), (.5 + math.cos(a + .2) * .22, .5 + math.sin(a + .2) * .22)], W_)
        ell(d, s, .5, .5, .24, .24, fill=W_)
        ell(d, s, .5, .5, .1, .1, fill=(110, 110, 120, 255))

    spec = {"messages": ((90, 170, 255), (20, 70, 190), envelope), "contacts": ((255, 170, 80), (210, 90, 20), book),
            "calls": ((110, 230, 120), (20, 140, 50), calls), "camera": ((200, 200, 210), (110, 110, 125), camera),
            "gallery": ((255, 130, 190), (190, 40, 120), gallery), "snake": ((60, 70, 60), (14, 22, 14), snake),
            "ring": ((190, 120, 255), (100, 40, 200), ring), "calc": ((150, 160, 175), (70, 80, 95), calc),
            "settings": ((120, 200, 220), (30, 110, 140), settings)}
    return {k: kit.icon(t, b, f, n=32) for k, (t, b, f) in spec.items()}


def flip_wallpaper(top, bottom, accent, seed):
    w, h = 200, 248
    rgb = kit.gradient(w, h, [(0, top), (1, bottom)])
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
    for k in range(4):
        y0 = h * (.45 + k * .12)
        band = np.exp(-((ys - y0 - np.sin(xs / w * math.tau * (1 + k * .3) + k) * h * .06) / (h * .03)) ** 2)
        rgb = rgb + band[..., None] * np.array(accent) / 255 * (.35 - k * .06)
    img = kit.to_img(np.dstack([np.clip(rgb, 0, 1), np.ones((h, w))]))
    d = ImageDraw.Draw(img)
    rng = np.random.default_rng(seed)
    for _ in range(26):
        x, y, r = rng.random() * w, rng.random() * h, 3 + rng.random() * 14
        d.ellipse((x - r, y - r, x + r, y + r), outline=(255, 255, 255, 90), width=2)
        d.ellipse((x - r * .5, y - r * .6, x - r * .1, y - r * .2), fill=(255, 255, 255, 110))
    return img


FLIP_WALLS = {"blue": ((60, 140, 255), (8, 24, 80), (160, 220, 255), 6401), "pink": ((255, 120, 190), (90, 10, 60), (255, 200, 230), 6402),
              "green": ((90, 220, 140), (8, 60, 30), (190, 255, 210), 6403), "orange": ((255, 170, 70), (110, 30, 6), (255, 230, 170), 6404)}


def main():
    kit.save(gsm_body(), "textures/gui/phone/gsm.png")
    kit.save(flip_body(), "textures/gui/phone/clapet.png")
    kit.save(brick_body(), "textures/gui/phone/radiotelephone.png")
    for name, rows in GSM_ICONS.items():
        kit.save(gsm_icon(rows), f"textures/gui/phone/gsm_{name}.png")
    for name, img in flip_icons().items():
        kit.save(img, f"textures/gui/phone/clapet_{name}.png")
    for name, (t, b, a, seed) in FLIP_WALLS.items():
        kit.save(flip_wallpaper(t, b, a, seed), f"textures/gui/phone/clapet_wall_{name}.png")
