"""The phones held in hand: real 3D item models (boxes, their faces painted on one texture each) for the hands, the
ground and the head, and a flat 16 x 16 picture for the inventory and item frames (NeoForge's separate transforms).
The screen is on each model's south face, as a flat item's picture is. Original art."""
import math

import numpy as np
from PIL import Image, ImageDraw

import kit

D = 8  # texture pixels per model unit


class Model:
    def __init__(self, name, size=256):
        self.name, self.size = name, size
        self.img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        self.elements = []
        self.x = self.y = self.row = 0

    def _alloc(self, w, h):
        w, h = max(1, int(math.ceil(w))), max(1, int(math.ceil(h)))
        if self.x + w > self.size:
            self.x, self.y, self.row = 0, self.y + self.row, 0
        assert self.y + h <= self.size, f"{self.name}: texture full"
        r = (self.x, self.y, w, h)
        self.x += w
        self.row = max(self.row, h)
        return r

    def box(self, frm, to, paint, rotation=None):
        """A box; {paint} maps a face to a function (draw, w, h) painting it, or a colour, or a (top, bottom) pair."""
        dx, dy, dz = to[0] - frm[0], to[1] - frm[1], to[2] - frm[2]
        sizes = {"north": (dx, dy), "south": (dx, dy), "east": (dz, dy), "west": (dz, dy), "up": (dx, dz), "down": (dx, dz)}
        faces = {}
        for face, (fw, fh) in sizes.items():
            p = paint.get(face, paint.get("*"))
            if p is None:
                continue
            x, y, w, h = self._alloc(fw * D, fh * D)
            tile = Image.new("RGBA", (w, h), (0, 0, 0, 0))
            if callable(p):
                p(ImageDraw.Draw(tile), w, h, tile)
            else:
                fill(tile, p)
            self.img.alpha_composite(tile, (x, y))
            k = 16 / self.size
            faces[face] = {"uv": [x * k, y * k, (x + w) * k, (y + h) * k], "texture": "#t"}
        e = {"from": list(frm), "to": list(to), "faces": faces}
        if rotation:
            e["rotation"] = rotation
        self.elements.append(e)

    def write(self, display):
        kit.save(self.img, f"textures/item/{self.name}_3d.png")
        kit.write_json(f"models/item/{self.name}_3d.json", {
            "textures": {"t": f"eraphones:item/{self.name}_3d", "particle": f"eraphones:item/{self.name}_3d"},
            "elements": self.elements, "display": display})


def fill(tile, p):
    """A face of one colour (a little noise, darker toward the bottom) or of a gradient (top, bottom)."""
    w, h = tile.size
    top, bottom = (p, p) if isinstance(p[0], int) else p
    ys = np.linspace(0, 1, h)[:, None]
    rgb = np.array(top)[None, None, :3] * (1 - ys[..., None]) + np.array(bottom)[None, None, :3] * ys[..., None]
    rgb = rgb * (1 - ys[..., None] * .12)
    rng = np.random.default_rng(len(top) * 31 + w * 7 + h)
    rgb = rgb + rng.normal(0, 2.0, (h, w, 1))
    a = np.full((h, w, 1), 255.0)
    tile.paste(Image.fromarray(np.clip(np.concatenate([rgb * np.ones((h, w, 3)), a], axis=2), 0, 255).astype(np.uint8), "RGBA"))


def screen_paint(top, bottom, draw_more=None):
    def p(d, w, h, tile):
        fill(tile, (top, bottom))
        if draw_more:
            draw_more(d, w, h, tile)
    return p


def bezel(color, inner, r=1):
    def p(d, w, h, tile):
        d.rounded_rectangle((0, 0, w - 1, h - 1), radius=r * 2, fill=color)
        inner(d, w, h, tile)
    return p


# ------------------------------------------------------------------------------------------------ the phones

HELD = {
    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 2.5, 1], "scale": [0.5, 0.5, 0.5]},
    "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 2.5, 1], "scale": [0.5, 0.5, 0.5]},
    "firstperson_righthand": {"rotation": [0, -100, 18], "translation": [1.13, 3.2, 1.13], "scale": [0.6, 0.6, 0.6]},
    "firstperson_lefthand": {"rotation": [0, -80, -18], "translation": [1.13, 3.2, 1.13], "scale": [0.6, 0.6, 0.6]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.45, 0.45, 0.45]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
}


def smartphone():
    m = Model("smartphone")
    frame = ((150, 152, 158), (96, 98, 106))

    def front(d, w, h, tile):
        d.rounded_rectangle((0, 0, w - 1, h - 1), radius=7, fill=(18, 18, 22, 255))
        sx0, sy0, sx1, sy1 = 3, 3, w - 4, h - 4
        scr = Image.new("RGBA", (sx1 - sx0, sy1 - sy0))
        fill(scr, ((70, 90, 200), (14, 12, 40)))
        sd = ImageDraw.Draw(scr)
        sw, sh = scr.size
        for k in range(18):
            x, y = (k * 37) % sw, (k * 53) % (sh // 2)
            sd.point((x, y), fill=(255, 255, 255, 200))
        sd.rectangle((sw // 2 - 9, 14, sw // 2 + 9, 20), fill=(255, 255, 255, 230))
        sd.rectangle((sw // 2 - 6, 24, sw // 2 + 6, 25), fill=(200, 210, 255, 200))
        for k in range(4):
            sd.rounded_rectangle((5 + k * 9, sh - 16, 11 + k * 9, sh - 10), radius=2, fill=[(110, 240, 140), (90, 200, 255), (235, 235, 240),
                                                                                          (80, 180, 255)][k] + (255,))
        sd.rounded_rectangle((sw // 2 - 7, 4, sw // 2 + 7, 7), radius=2, fill=(0, 0, 0, 255))
        sd.rectangle((sw // 2 - 8, sh - 4, sw // 2 + 8, sh - 3), fill=(255, 255, 255, 220))
        tile.alpha_composite(scr, (sx0, sy0))

    def back(d, w, h, tile):
        fill(tile, ((120, 122, 130), (84, 86, 94)))
        d.rounded_rectangle((0, 0, w - 1, h - 1), radius=7, outline=(170, 172, 180, 255), width=1)

    m.box([5, 2, 7.4], [11, 14, 8.6], {"south": front, "north": back, "*": frame})
    # The camera block on the back, its two lenses
    def lenses(d, w, h, tile):
        fill(tile, ((70, 72, 80), (50, 52, 60)))
        for cx, cy in ((5, 5), (5, 13)):
            d.ellipse((cx - 3, cy - 3, cx + 3, cy + 3), fill=(12, 12, 16, 255), outline=(150, 150, 160, 255))
            d.point((cx - 1, cy - 1), fill=(120, 160, 255, 255))
    m.box([8.4, 11, 7.15], [10.6, 13.6, 7.4], {"north": lenses, "*": (60, 62, 70)})
    m.box([4.85, 9, 7.8], [5, 10.4, 8.2], {"*": (130, 132, 140)})
    m.box([11, 9.6, 7.8], [11.15, 11.6, 8.2], {"*": (130, 132, 140)})
    m.write(HELD)


def keypad(d, w, h, tile, rows, cols, x0, y0, kw, kh, gx, gy, color, text):
    for r in range(rows):
        for c in range(cols):
            x, y = x0 + c * (kw + gx), y0 + r * (kh + gy)
            d.rounded_rectangle((x, y, x + kw, y + kh), radius=2, fill=color)
            d.line((x + kw // 2 - 1, y + kh // 2, x + kw // 2 + 1, y + kh // 2), fill=text)


def clapet():
    m = Model("clapet")
    silver = ((200, 206, 214), (150, 156, 166))

    def base_front(d, w, h, tile):
        fill(tile, silver)
        d.ellipse((w // 2 - 7, 3, w // 2 + 7, 13), fill=(120, 126, 136, 255), outline=(80, 86, 96, 255))
        d.ellipse((w // 2 - 3, 6, w // 2 + 3, 10), fill=(90, 100, 120, 255))
        d.rounded_rectangle((3, 4, 11, 8), radius=2, fill=(70, 170, 90, 255))
        d.rounded_rectangle((w - 12, 4, w - 4, 8), radius=2, fill=(200, 60, 60, 255))
        keypad(d, w, h, tile, 4, 3, 5, 17, 10, 6, 4, 3, (170, 176, 186, 255), (40, 44, 52, 255))

    def lid_front(d, w, h, tile):
        fill(tile, silver)
        d.rounded_rectangle((3, 4, w - 4, h - 3), radius=3, fill=(8, 10, 14, 255))
        scr = Image.new("RGBA", (w - 12, h - 16))
        fill(scr, ((60, 140, 255), (8, 24, 80)))
        sd = ImageDraw.Draw(scr)
        sd.rectangle((0, 0, scr.width, 3), fill=(0, 0, 0, 160))
        sd.text((scr.width // 2 - 9, scr.height // 2 - 6), "12:00", fill=(255, 255, 255, 255))
        tile.alpha_composite(scr, (6, 8))
        d.rounded_rectangle((w // 2 - 5, 1, w // 2 + 5, 2), radius=1, fill=(30, 32, 36, 255))

    def lid_back(d, w, h, tile):
        fill(tile, ((170, 178, 190), (130, 138, 150)))
        d.rounded_rectangle((w // 2 - 8, 10, w // 2 + 8, 24), radius=2, fill=(10, 14, 20, 255))
        d.rectangle((w // 2 - 6, 15, w // 2 + 6, 18), fill=(80, 140, 220, 255))
        d.ellipse((w // 2 - 3, 30, w // 2 + 3, 36), fill=(20, 20, 24, 255), outline=(180, 186, 196, 255))

    m.box([5, 1, 7.6], [11, 8, 8.5], {"south": base_front, "*": silver})
    m.box([5.3, 7.6, 7.3], [10.7, 8.6, 8.4], {"*": ((230, 234, 240), (110, 116, 126))})
    m.box([5, 8, 7.6], [11, 15, 8.4], {"south": lid_front, "north": lid_back, "*": silver},
          rotation={"origin": [8, 8, 8], "axis": "x", "angle": -22.5})
    m.write(HELD)


def gsm():
    m = Model("gsm")
    shell = ((64, 66, 72), (36, 38, 44))

    def front(d, w, h, tile):
        fill(tile, shell)
        for k in range(3):
            d.rectangle((w // 2 - 5 + k * 4, 4, w // 2 - 4 + k * 4, 6), fill=(10, 10, 12, 255))
        d.rounded_rectangle((3, 10, w - 4, 38), radius=2, fill=(30, 32, 36, 255))
        d.rectangle((6, 13, w - 7, 35), fill=(150, 186, 100, 255))
        for k in range(4):
            d.rectangle((8, 30 - k * 4, 9 + k, 32 - k * 4), fill=(30, 42, 18, 255))
        d.text((w // 2 - 11, 18), "VOXA", fill=(30, 42, 18, 255))
        d.rounded_rectangle((w // 2 - 6, 42, w // 2 + 6, 47), radius=2, fill=(110, 118, 128, 255))
        d.rounded_rectangle((4, 42, 11, 47), radius=2, fill=(70, 150, 80, 255))
        d.rounded_rectangle((w - 12, 42, w - 5, 47), radius=2, fill=(170, 60, 60, 255))
        keypad(d, w, h, tile, 4, 3, 5, 51, 8, 7, 4, 3, (96, 104, 112, 255), (230, 230, 230, 255))

    m.box([5.5, 1.5, 7.3], [10.5, 13.5, 8.7], {"south": front, "*": shell})
    m.box([9.2, 13.5, 7.6], [10.2, 15.5, 8.4], {"*": (24, 24, 28)})
    m.write(HELD)


def radiotelephone():
    m = Model("radiotelephone")
    beige = ((196, 188, 172), (150, 142, 128))

    def front(d, w, h, tile):
        fill(tile, beige)
        for r in range(2):
            for c in range(6):
                d.point((8 + c * 5, 4 + r * 3), fill=(60, 54, 48, 255))
        d.rounded_rectangle((3, 9, w - 4, 22), radius=1, fill=(30, 10, 8, 255))
        for k in range(7):
            x = 6 + k * 4
            d.rectangle((x, 13, x + 2, 13), fill=(255, 70, 50, 255))
            d.rectangle((x, 16, x + 2, 16), fill=(255, 70, 50, 255))
            d.rectangle((x + 2, 13, x + 2, 18), fill=(255, 70, 50, 255))
        keypad(d, w, h, tile, 2, 3, 5, 27, 8, 6, 4, 3, (70, 64, 56, 255), (230, 220, 200, 255))
        keypad(d, w, h, tile, 4, 3, 5, 47, 8, 7, 4, 4, (52, 48, 44, 255), (230, 220, 200, 255))
        for r in range(3):
            for c in range(5):
                d.point((10 + c * 5, h - 12 + r * 3), fill=(80, 72, 64, 255))

    m.box([5.5, 0, 7], [10.5, 13, 9], {"south": front, "*": beige})
    m.box([6.2, 13, 7.7], [6.8, 21, 8.3], {"*": (30, 28, 26)})
    m.box([6, 21, 7.5], [7, 22, 8.5], {"*": (20, 18, 16)})
    m.write(HELD)


def chargeur():
    m = Model("chargeur")
    white = ((240, 240, 236), (200, 200, 196))
    m.box([6, 4, 6], [10, 8, 10], {"*": white})
    m.box([6.9, 8, 7.7], [7.5, 10, 8.3], {"*": (200, 180, 120)})
    m.box([8.5, 8, 7.7], [9.1, 10, 8.3], {"*": (200, 180, 120)})
    m.box([7.6, 1, 7.6], [8.4, 4, 8.4], {"*": (30, 30, 32)})
    m.box([7.6, 0.4, 7.6], [12.4, 1.2, 8.4], {"*": (30, 30, 32)})
    m.box([12, 0.2, 7.4], [13.4, 1.6, 8.6], {"*": (60, 60, 64)})
    m.write({**HELD, "firstperson_righthand": {"rotation": [0, -45, 0], "translation": [1, 3, 1], "scale": [0.6, 0.6, 0.6]},
             "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.5, 0.5, 0.5]}})


# ------------------------------------------------------------------------------------------------ the flat pictures

ICONS = {
    "smartphone": ([
        "................",
        ".....oooooo.....",
        "....oGGGGGGo....",
        "....oKkkkkKo....",
        "....oKbBBbKo....",
        "....oKBbbBKo....",
        "....oKbBBbKo....",
        "....oKBWWBKo....",
        "....oKbBBbKo....",
        "....oKBbbBKo....",
        "....oKbBBbKo....",
        "....oKkkkkKo....",
        "....oGGGGGGo....",
        ".....oooooo.....",
        "................",
        "................"], {"o": (40, 42, 48), "G": (170, 172, 180), "K": (16, 16, 20), "k": (30, 30, 40), "b": (50, 80, 190),
                             "B": (90, 130, 230), "W": (240, 240, 255)}),
    "clapet": ([
        "................",
        "....oooooooo....",
        "...oSSSSSSSSo...",
        "...oSkkkkkkSo...",
        "...oSkBBBBkSo...",
        "...oSkBWWBkSo...",
        "...oSkkkkkkSo...",
        "....oHHHHHHo....",
        "...oSSSSSSSSo...",
        "...oSgSsSrSSo...",
        "...oSsSsSsSSo...",
        "...oSsSsSsSSo...",
        "...oSsSsSsSSo...",
        "....oooooooo....",
        "................",
        "................"], {"o": (60, 66, 76), "S": (200, 206, 216), "s": (130, 136, 146), "k": (10, 12, 16), "B": (60, 140, 255),
                             "W": (230, 240, 255), "H": (120, 126, 136), "g": (70, 170, 90), "r": (200, 60, 60)}),
    "gsm": ([
        "................",
        "...........oo...",
        ".....ooooooKo...",
        "....oCCCCCCCo...",
        "....oCLLLLLCo...",
        "....oCLdLLLCo...",
        "....oCLLLLLCo...",
        "....oCCCCCCCo...",
        "....oCgCmCrCo...",
        "....oCkCkCkCo...",
        "....oCkCkCkCo...",
        "....oCkCkCkCo...",
        "....oCCCCCCCo...",
        ".....ooooooo....",
        "................",
        "................"], {"o": (20, 20, 24), "C": (60, 62, 70), "K": (30, 30, 34), "L": (150, 186, 100), "d": (40, 52, 26),
                             "g": (70, 150, 80), "m": (120, 128, 136), "r": (170, 60, 60), "k": (140, 146, 156)}),
    "radiotelephone": ([
        "...o............",
        "...o............",
        "...o............",
        "...o............",
        "..oooooooooo....",
        "..oBBBBBBBBo....",
        "..oBRRRRRRBo....",
        "..oBBBBBBBBo....",
        "..oBkBkBkBBo....",
        "..oBkBkBkBBo....",
        "..oBkBkBkBBo....",
        "..oBkBkBkBBo....",
        "..oBBBBBBBBo....",
        "..oBBBBBBBBo....",
        "..oooooooooo....",
        "................"], {"o": (70, 64, 56), "B": (200, 192, 176), "R": (220, 50, 40), "k": (70, 64, 56)}),
    "chargeur": ([
        "................",
        "......y..y......",
        "......y..y......",
        ".....oooooo.....",
        ".....oWWWWo.....",
        ".....oWWWWo.....",
        ".....oWWWWo.....",
        ".....oooooo.....",
        ".......kk.......",
        ".......kk.......",
        ".......kk.......",
        ".......kkkkkk...",
        "............kk..",
        "............ss..",
        "................",
        "................"], {"o": (150, 150, 146), "W": (245, 245, 240), "y": (210, 190, 120), "k": (30, 30, 32), "s": (90, 90, 96)}),
}


def main():
    smartphone()
    clapet()
    gsm()
    radiotelephone()
    chargeur()
    for name, (rows, legend) in ICONS.items():
        kit.save(kit.from_grid(rows, legend), f"textures/item/{name}.png")
        kit.write_json(f"models/item/{name}_icon.json", {"parent": "item/generated", "textures": {"layer0": f"eraphones:item/{name}"}})
        flat = {"parent": f"eraphones:item/{name}_icon"}
        kit.write_json(f"models/item/{name}.json", {
            "loader": "neoforge:separate_transforms",
            "base": {"parent": f"eraphones:item/{name}_3d"},
            "perspectives": {"gui": flat, "fixed": flat}})
