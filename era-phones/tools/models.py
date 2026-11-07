"""The phones held in hand: real 3D item models for the hands, the ground and the head, and a flat 16 x 16 picture for
the inventory and item frames (NeoForge's separate transforms). The bodies have rounded corners (the front and back
pictures are cut round, the edges stop short of the corners, and a slanted piece fills each corner), raised keys with
their figures, a camera block on the smartphone's back. The screen is on each model's south face. Original art."""
import math

import numpy as np
from PIL import Image, ImageDraw

import kit

D = 16  # texture pixels per model unit
R2 = math.sqrt(2)

# A 3 x 5 pixel font for the keys' figures and labels
FONT = {
    "0": ["###", "#.#", "#.#", "#.#", "###"], "1": [".#.", "##.", ".#.", ".#.", "###"], "2": ["###", "..#", "###", "#..", "###"],
    "3": ["###", "..#", "###", "..#", "###"], "4": ["#.#", "#.#", "###", "..#", "..#"], "5": ["###", "#..", "###", "..#", "###"],
    "6": ["###", "#..", "###", "#.#", "###"], "7": ["###", "..#", "..#", ".#.", ".#."], "8": ["###", "#.#", "###", "#.#", "###"],
    "9": ["###", "#.#", "###", "..#", "###"], "*": ["...", "#.#", ".#.", "#.#", "..."], "#": ["#.#", "###", "#.#", "###", "#.#"],
    ":": ["...", ".#.", "...", ".#.", "..."], "-": ["...", "...", "###", "...", "..."], "C": ["###", "#..", "#..", "#..", "###"],
    "S": ["###", "#..", "###", "..#", "###"], "N": ["#.#", "###", "###", "#.#", "#.#"], "D": ["##.", "#.#", "#.#", "#.#", "##."],
    "E": ["###", "#..", "##.", "#..", "###"], "R": ["##.", "#.#", "##.", "#.#", "#.#"], "L": ["#..", "#..", "#..", "#..", "###"],
    "T": ["###", ".#.", ".#.", ".#.", ".#."], "O": ["###", "#.#", "#.#", "#.#", "###"], "F": ["###", "#..", "##.", "#..", "#.."],
    "P": ["##.", "#.#", "##.", "#..", "#.."], "W": ["#.#", "#.#", "###", "###", "#.#"], "V": ["#.#", "#.#", "#.#", "#.#", ".#."],
    "X": ["#.#", "#.#", ".#.", "#.#", "#.#"], "A": [".#.", "#.#", "###", "#.#", "#.#"], "K": ["#.#", "##.", "#..", "##.", "#.#"],
    "M": ["#.#", "###", "###", "#.#", "#.#"], "U": ["#.#", "#.#", "#.#", "#.#", "###"], "G": ["###", "#..", "#.#", "#.#", "###"],
    " ": ["...", "...", "...", "...", "..."]}


def text(d, s, x, y, color, px=1):
    """Draws {s} in the little font, {px} pixels a dot; returns the width."""
    cx = x
    for ch in s:
        g = FONT.get(ch.upper(), FONT[" "])
        for r, row in enumerate(g):
            for c, v in enumerate(row):
                if v == "#":
                    d.rectangle((cx + c * px, y + r * px, cx + c * px + px - 1, y + r * px + px - 1), fill=color)
        cx += 4 * px
    return cx - x - px


def text_width(s, px=1):
    return len(s) * 4 * px - px


# ------------------------------------------------------------------------------------------------ painting faces

def vgrad(tile, top, bottom):
    w, h = tile.size
    t = np.linspace(0, 1, h)[:, None, None]
    rgb = np.array(top[:3], float) * (1 - t) + np.array(bottom[:3], float) * t
    rgb = np.broadcast_to(rgb, (h, w, 3))
    a = np.full((h, w, 1), 255.0)
    tile.paste(Image.fromarray(np.concatenate([rgb, a], axis=2).astype(np.uint8), "RGBA"))


def metal_edge(tile, dark, light):
    """An edge seen side on: dark at both rims, bright along the middle, as polished metal or plastic catches light."""
    w, h = tile.size
    across = w if w <= h else h
    t = np.abs(np.linspace(-1, 1, across)) ** 1.5
    col = np.array(light[:3], float)[None, :] * (1 - t[:, None]) + np.array(dark[:3], float)[None, :] * t[:, None]
    if w <= h:
        rgb = np.broadcast_to(col[None, :, :], (h, w, 3))
    else:
        rgb = np.broadcast_to(col[:, None, :], (h, w, 3))
    a = np.full((h, w, 1), 255.0)
    tile.paste(Image.fromarray(np.concatenate([rgb, a], axis=2).astype(np.uint8), "RGBA"))


def round_alpha(tile, r):
    """Cuts the picture's corners round (radius {r} pixels), softly."""
    w, h = tile.size
    ys, xs = np.mgrid[0:h, 0:w].astype(float) + .5
    cx = np.clip(xs, r, w - r)
    cy = np.clip(ys, r, h - r)
    dist = np.hypot(xs - cx, ys - cy)
    a = np.clip(r - dist + .5, 0, 1)
    arr = np.asarray(tile, float).copy()
    arr[..., 3] *= a
    tile.paste(Image.fromarray(arr.astype(np.uint8), "RGBA"))


def cut_ends(tile, cut, vertical):
    """Clears {cut} pixels at both ends of an edge (so it stops short of the rounded corners)."""
    arr = np.asarray(tile).copy()
    if vertical:
        arr[:cut, :, 3] = 0
        arr[arr.shape[0] - cut:, :, 3] = 0
    else:
        arr[:, :cut, 3] = 0
        arr[:, arr.shape[1] - cut:, 3] = 0
    tile.paste(Image.fromarray(arr, "RGBA"))


class Model:
    def __init__(self, name, size=512):
        self.name, self.size = name, size
        self.img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        self.elements = []
        self.x = self.y = self.row = 0

    def _alloc(self, w, h):
        w, h = max(1, int(round(w))), max(1, int(round(h)))
        if self.x + w > self.size:
            self.x, self.y, self.row = 0, self.y + self.row, 0
        assert self.y + h <= self.size, f"{self.name}: texture full"
        r = (self.x, self.y, w, h)
        self.x += w
        self.row = max(self.row, h)
        return r

    def box(self, frm, to, paint, rotation=None):
        """A box; {paint} maps a face (or "*") to a function (tile) painting it, or a colour."""
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
                p(tile)
            else:
                vgrad(tile, p, p)
            self.img.alpha_composite(tile, (x, y))
            k = 16 / self.size
            faces[face] = {"uv": [x * k, y * k, (x + w) * k, (y + h) * k], "texture": "#t"}
        e = {"from": [round(v, 4) for v in frm], "to": [round(v, 4) for v in to], "faces": faces}
        if rotation:
            e["rotation"] = rotation
        self.elements.append(e)

    def body(self, x0, y0, x1, y1, z0, z1, r, front, back, edge_dark, edge_light, corners=True, rotation=None):
        """A slab with rounded corners: {front} and {back} paint its faces, the edges are polished. Without {corners}
        (a slab turned on another axis, whose corner pieces could not follow) the edges run full length."""
        rp = int(round(r * D))

        def face(paint):
            def p(tile):
                paint(tile)
                round_alpha(tile, rp)
            return p

        def edge(vertical):
            def p(tile):
                metal_edge(tile, edge_dark, edge_light)
                if corners:
                    cut_ends(tile, rp, vertical)
            return p

        self.box([x0, y0, z0], [x1, y1, z1], {"south": face(front), "north": face(back), "east": edge(True), "west": edge(True),
                                              "up": edge(False), "down": edge(False)}, rotation=rotation)
        if not corners:
            return
        # A slanted piece at each corner, filling between the edges
        s = r * R2 * .98
        zc = (z0 + z1) / 2
        for cx, cy in ((x0 + r, y0 + r), (x1 - r, y0 + r), (x0 + r, y1 - r), (x1 - r, y1 - r)):
            def corner(tile):
                metal_edge(tile, edge_dark, edge_light)
            self.box([cx - s / 2, cy - s / 2, z0 + .01], [cx + s / 2, cy + s / 2, z1 - .01],
                     {"east": corner, "west": corner, "up": corner, "down": corner}, rotation={"origin": [cx, cy, zc], "axis": "z", "angle": 45})

    def key(self, x, y, w, h, z, depth, color, label, label_color, sub=None):
        """A raised key standing out of the face at {z} (the front, south), its figure painted on it."""
        def top(tile):
            vgrad(tile, tuple(min(255, c + 30) for c in color[:3]), tuple(max(0, c - 25) for c in color[:3]))
            d = ImageDraw.Draw(tile)
            tw, th = tile.size
            px = 2 if tw >= 18 and th >= 13 else 1
            lw = text_width(label, px)
            ly = (th - 5 * px) // 2 - (2 if sub else 0)
            text(d, label, (tw - lw) // 2, max(1, ly), label_color, px)
            if sub:
                text(d, sub, (tw - text_width(sub)) // 2, th - 6, tuple(c // 2 + 60 for c in label_color[:3]) + (255,), 1)
            round_alpha(tile, 3)

        def side(tile):
            vgrad(tile, tuple(max(0, c - 30) for c in color[:3]), tuple(max(0, c - 60) for c in color[:3]))

        self.box([x, y, z], [x + w, y + h, z + depth], {"south": top, "east": side, "west": side, "up": side, "down": side})

    def write(self, display):
        # Trim the texture to what is used (a power of two)
        used = max(self.y + self.row, 16)
        size = self.size
        while size // 2 >= used and size // 2 >= 64:
            size //= 2
        img = self.img
        if size != self.size:
            # The UVs were laid in the large texture's 0-16 space: rescale them
            k = self.size / size
            img = self.img.crop((0, 0, self.size, size))
            img = img.crop((0, 0, size, size)) if self.size == size else self.img.crop((0, 0, self.size, self.size))
            size = self.size
        kit.save(img, f"textures/item/{self.name}_3d.png")
        kit.write_json(f"models/item/{self.name}_3d.json", {
            "textures": {"t": f"eraphones:item/{self.name}_3d", "particle": f"eraphones:item/{self.name}_3d"},
            "elements": self.elements, "display": display})


HELD = {
    "thirdperson_righthand": {"rotation": [-10, 90, 0], "translation": [0, 3, 0.5], "scale": [0.6, 0.6, 0.6]},
    "thirdperson_lefthand": {"rotation": [-10, -90, 0], "translation": [0, 3, 0.5], "scale": [0.6, 0.6, 0.6]},
    "firstperson_righthand": {"rotation": [0, -100, 12], "translation": [0, 4, -1], "scale": [0.34, 0.34, 0.34]},
    "firstperson_lefthand": {"rotation": [0, -80, -12], "translation": [0, 4, -1], "scale": [0.34, 0.34, 0.34]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.45, 0.45, 0.45]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
}


# ------------------------------------------------------------------------------------------------ the phones

def smartphone():
    m = Model("smartphone")
    x0, y0, x1, y1, z0, z1 = 5, 2, 11, 14, 7.6, 8.4

    def front(tile):
        w, h = tile.size
        d = ImageDraw.Draw(tile)
        d.rectangle((0, 0, w, h), fill=(150, 154, 162, 255))
        d.rounded_rectangle((2, 2, w - 3, h - 3), radius=13, fill=(8, 8, 10, 255))
        sx0, sy0, sx1, sy1 = 5, 5, w - 6, h - 6
        scr = Image.new("RGBA", (sx1 - sx0, sy1 - sy0))
        sw, sh = scr.size
        ys = np.linspace(0, 1, sh)[:, None]
        rgb = np.array([70, 90, 200])[None, None, :] * (1 - ys[..., None]) + np.array([12, 10, 36])[None, None, :] * ys[..., None]
        rgb = np.broadcast_to(rgb, (sh, sw, 3)).copy()
        yy, xx = np.mgrid[0:sh, 0:sw]
        rr = np.hypot(xx - sw * .5, yy - sh * 1.05) / sw
        rgb += (np.exp(-((rr - 1.0) / .02) ** 2) * 200)[..., None] * np.array([.8, .9, 1])
        scr.paste(Image.fromarray(np.dstack([np.clip(rgb, 0, 255), np.full((sh, sw), 255)]).astype(np.uint8), "RGBA"))
        sd = ImageDraw.Draw(scr)
        rng = np.random.default_rng(7)
        for _ in range(40):
            sd.point((rng.integers(0, sw), rng.integers(0, sh // 2)), fill=(255, 255, 255, 200))
        text(sd, "11:00", (sw - text_width("11:00", 3)) // 2, 34, (255, 255, 255, 255), 3)
        sd.rounded_rectangle((sw // 2 - 12, 6, sw // 2 + 12, 12), radius=3, fill=(0, 0, 0, 255))
        for k, col in enumerate(((110, 240, 140), (90, 200, 255), (235, 235, 240), (80, 180, 255))):
            sd.rounded_rectangle((10 + k * 18, sh - 26, 22 + k * 18, sh - 14), radius=3, fill=col + (255,))
        sd.rounded_rectangle((sw // 2 - 14, sh - 6, sw // 2 + 14, sh - 5), radius=1, fill=(255, 255, 255, 220))
        mask = Image.new("L", scr.size, 0)
        ImageDraw.Draw(mask).rounded_rectangle((0, 0, sw - 1, sh - 1), radius=10, fill=255)
        tile.paste(scr, (sx0, sy0), mask)
        # A reflection across the glass
        gl = Image.new("RGBA", tile.size, (0, 0, 0, 0))
        ImageDraw.Draw(gl).polygon([(w * .55, 0), (w, 0), (w, h * .12), (w * .1, h * .62), (0, h * .62), (0, h * .5)], fill=(255, 255, 255, 18))
        tile.alpha_composite(gl)

    def back(tile):
        vgrad(tile, (124, 128, 136), (88, 92, 100))
        d = ImageDraw.Draw(tile)
        w, h = tile.size
        d.rounded_rectangle((1, 1, w - 2, h - 2), radius=13, outline=(170, 174, 182, 255), width=1)

    m.body(x0, y0, x1, y1, z0, z1, .85, front, back, (70, 72, 78), (200, 204, 212))
    # The camera block on the back, its lenses
    def plate(tile):
        vgrad(tile, (96, 100, 108), (70, 74, 82))
        d = ImageDraw.Draw(tile)
        w, h = tile.size
        for cx, cy in ((w * .32, h * .3), (w * .32, h * .72), (w * .74, h * .3)):
            d.ellipse((cx - 8, cy - 8, cx + 8, cy + 8), fill=(20, 20, 24, 255), outline=(190, 192, 200, 255), width=2)
            d.ellipse((cx - 4, cy - 4, cx + 4, cy + 4), fill=(40, 60, 120, 255))
            d.point((cx - 2, cy - 2), fill=(200, 220, 255, 255))
        d.ellipse((w * .74 - 3, h * .72 - 3, w * .74 + 3, h * .72 + 3), fill=(255, 240, 200, 255))
        round_alpha(tile, 8)
    m.box([8.2, 10.6, 7.42], [10.6, 13.4, 7.6], {"north": plate, "*": (80, 84, 92)})
    # Keys on the sides
    for y in (10.2, 11.4):
        m.box([4.88, y, 7.85], [5, y + .9, 8.15], {"*": (150, 154, 162)})
    m.box([11, 10.2, 7.85], [11.12, 12, 8.15], {"*": (150, 154, 162)})
    m.write(HELD)


def keypad(m, x0, y0, cols, rows, kw, kh, gx, gy, z, color, label_color, labels, subs=None):
    for r in range(rows):
        for c in range(cols):
            i = r * cols + c
            if i >= len(labels):
                return
            x, y = x0 + c * (kw + gx), y0 - r * (kh + gy) - kh
            m.key(x, y, kw, kh, z, .18, color, labels[i], label_color, subs[i] if subs else None)


DIGITS = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"]


def clapet():
    m = Model("clapet")
    silver_dark, silver_light = (120, 126, 136), (236, 240, 246)

    def base_front(tile):
        vgrad(tile, (206, 212, 220), (160, 166, 176))

    def lid_front(tile):
        w, h = tile.size
        vgrad(tile, (206, 212, 220), (170, 176, 186))
        d = ImageDraw.Draw(tile)
        d.rounded_rectangle((5, 10, w - 6, h - 6), radius=5, fill=(6, 8, 12, 255))
        scr = Image.new("RGBA", (w - 18, h - 26))
        vgrad(scr, (70, 150, 255), (10, 30, 90))
        sd = ImageDraw.Draw(scr)
        sw, sh = scr.size
        sd.rectangle((0, 0, sw, 6), fill=(0, 0, 0, 170))
        for k in range(4):
            sd.rectangle((2 + k * 3, 5 - k, 3 + k * 3, 5), fill=(255, 255, 255, 255))
        text(sd, "11:00", (sw - text_width("11:00", 2)) // 2, sh // 2 - 8, (255, 255, 255, 255), 2)
        for k in range(6):
            cx, cy, r = (k * 37) % sw, sh * .6 + (k * 13) % (sh * .35), 2 + k % 3
            sd.ellipse((cx - r, cy - r, cx + r, cy + r), outline=(255, 255, 255, 120))
        tile.paste(scr, (9, 14))
        d.rounded_rectangle((w // 2 - 8, 3, w // 2 + 8, 5), radius=1, fill=(30, 32, 38, 255))

    def lid_back(tile):
        w, h = tile.size
        vgrad(tile, (180, 188, 200), (130, 138, 150))
        d = ImageDraw.Draw(tile)
        d.rounded_rectangle((w // 2 - 14, 18, w // 2 + 14, 42), radius=4, fill=(10, 14, 20, 255), outline=(220, 226, 236, 255))
        d.rectangle((w // 2 - 10, 26, w // 2 + 10, 33), fill=(60, 120, 220, 255))
        d.ellipse((w // 2 - 6, 56, w // 2 + 6, 68), fill=(20, 20, 24, 255), outline=(200, 206, 216, 255), width=2)

    m.body(5, 1, 11, 8, 7.6, 8.4, .6, base_front, base_front, silver_dark, silver_light)
    # The hinge: two knuckles
    for x in ((5.2, 7.2), (8.8, 10.8)):
        m.box([x[0], 7.6, 7.45], [x[1], 8.6, 8.45], {"*": lambda t: metal_edge(t, (110, 116, 126), (240, 244, 250))})
    # The lid, opened back
    m.body(5, 8, 11, 15, 7.65, 8.35, .3, lid_front, lid_back, silver_dark, silver_light, corners=False,
           rotation={"origin": [8, 8, 8], "axis": "x", "angle": -22.5})
    # Keys: the soft keys, the pad and its OK, green and red, the digits
    z = 8.4
    m.key(5.5, 6.6, 1.2, .6, z, .14, (170, 176, 186), "-", (40, 44, 52))
    m.key(9.3, 6.6, 1.2, .6, z, .14, (170, 176, 186), "-", (40, 44, 52))
    m.key(7.4, 6.3, 1.2, .9, z, .2, (90, 106, 130), "OK", (240, 244, 250))
    m.key(5.5, 5.6, 1.2, .7, z, .16, (70, 170, 90), "", (255, 255, 255))
    m.key(9.3, 5.6, 1.2, .7, z, .16, (200, 60, 60), "", (255, 255, 255))
    keypad(m, 5.6, 5.1, 3, 4, 1.4, .8, .3, .22, z, (190, 196, 206), (30, 34, 42), DIGITS)
    m.write(HELD)


def gsm():
    m = Model("gsm")

    def front(tile):
        w, h = tile.size
        vgrad(tile, (70, 72, 80), (40, 42, 48))
        d = ImageDraw.Draw(tile)
        for k in range(4):
            d.rounded_rectangle((w // 2 - 14 + k * 8, 8, w // 2 - 10 + k * 8, 12), radius=1, fill=(10, 10, 12, 255))
        text(d, "VOXA", (w - text_width("VOXA")) // 2, 17, (150, 154, 162, 255))
        d.rounded_rectangle((6, 25, w - 7, 82), radius=5, fill=(26, 28, 32, 255), outline=(110, 114, 122, 255))
        d.rectangle((11, 31, w - 12, 76), fill=(150, 186, 100, 255))
        ink = (30, 42, 18, 255)
        for k in range(4):
            d.rectangle((13, 70 - k * 8, 14 + k, 72 - k * 8), fill=ink)
        text(d, "VOXA", (w - text_width("VOXA", 2)) // 2, 40, ink, 2)
        text(d, "11:00", (w - text_width("11:00")) // 2, 56, ink)
        for k in range(3):
            d.ellipse((w // 2 - 6 + k * 5, h - 9, w // 2 - 4 + k * 5, h - 7), fill=(10, 10, 12, 255))

    m.body(5.5, 1.5, 10.5, 13.5, 7.25, 8.75, .7, front, lambda t: vgrad(t, (60, 62, 70), (34, 36, 42)), (24, 25, 30), (110, 114, 124))
    # The stub aerial: an eight-sided rod
    for a in (0, 45):
        m.box([9.35, 13.2, 7.7], [10.05, 15.4, 8.4], {"*": lambda t: metal_edge(t, (16, 16, 20), (80, 82, 90))},
              rotation={"origin": [9.7, 14, 8.05], "axis": "y", "angle": a} if a else None)
    z = 8.75
    m.key(6.0, 7.0, 1.1, .6, z, .14, (70, 150, 80), "", (255, 255, 255))
    m.key(7.5, 7.0, 1.0, .6, z, .14, (120, 128, 138), "OK", (240, 240, 240))
    m.key(8.9, 7.0, 1.1, .6, z, .14, (170, 60, 60), "", (255, 255, 255))
    keypad(m, 6.0, 6.6, 3, 4, 1.1, .75, .3, .25, z, (96, 104, 114), (236, 236, 236), DIGITS)
    m.write(HELD)


def radiotelephone():
    m = Model("radiotelephone")

    def front(tile):
        w, h = tile.size
        vgrad(tile, (204, 196, 180), (164, 156, 140))
        d = ImageDraw.Draw(tile)
        for r in range(2):
            for c in range(7):
                d.ellipse((12 + c * 9, 7 + r * 6, 14 + c * 9, 9 + r * 6), fill=(60, 54, 46, 255))
        d.rounded_rectangle((5, 22, w - 6, 48), radius=2, fill=(30, 10, 8, 255), outline=(110, 100, 88, 255))
        text(d, "555-0142", (w - text_width("555-0142", 2)) // 2, 30, (255, 70, 46, 255), 2)
        for r in range(3):
            for c in range(6):
                d.ellipse((16 + c * 9, h - 22 + r * 6, 18 + c * 9, h - 20 + r * 6), fill=(110, 100, 88, 255))

    m.body(5.5, 0, 10.5, 13, 7, 9, .55, front, lambda t: vgrad(t, (190, 182, 166), (150, 142, 128)), (130, 122, 108), (226, 220, 206))
    # The long aerial: an eight-sided rod, a knob at its top
    for a in (0, 45):
        rot = {"origin": [6.5, 17, 8], "axis": "y", "angle": a} if a else None
        m.box([6.2, 13, 7.7], [6.8, 21, 8.3], {"*": lambda t: metal_edge(t, (20, 18, 16), (90, 86, 80))}, rotation=rot)
        m.box([5.95, 21, 7.45], [7.05, 22.1, 8.55], {"*": lambda t: metal_edge(t, (14, 12, 10), (70, 66, 60))},
              rotation={"origin": [6.5, 21.5, 8], "axis": "y", "angle": a} if a else None)
    z = 9
    top = [("SND", (62, 122, 70)), ("CLR", (92, 86, 78)), ("END", (154, 58, 50)), ("RCL", (92, 86, 78)), ("STO", (92, 86, 78)), ("FCN", (106, 90, 48))]
    for i, (label, col) in enumerate(top):
        m.key(5.9 + i % 3 * 1.45, 9.1 - i // 3 * .95 - .75, 1.25, .75, z, .2, col, label, (240, 232, 216))
    keypad(m, 5.9, 7.1, 3, 4, 1.25, .8, .2, .2, z, (60, 56, 50), (240, 232, 216), DIGITS)
    m.write(HELD)


def chargeur():
    m = Model("chargeur")
    white = lambda t: metal_edge(t, (190, 190, 186), (250, 250, 246))
    m.body(6, 4, 10, 8, 6, 10, .5, lambda t: vgrad(t, (246, 246, 242), (214, 214, 210)), lambda t: vgrad(t, (240, 240, 236), (208, 208, 204)),
           (190, 190, 186), (250, 250, 246))
    for x in (6.9, 8.5):
        m.box([x, 8, 7.75], [x + .6, 10, 8.25], {"*": lambda t: metal_edge(t, (150, 130, 80), (230, 210, 150))})
    cable = lambda t: metal_edge(t, (16, 16, 18), (70, 70, 76))
    m.box([7.65, 1, 7.65], [8.35, 4, 8.35], {"*": cable})
    m.box([7.65, .4, 7.65], [12.4, 1.1, 8.35], {"*": cable})
    m.box([12, .1, 7.4], [13.6, 1.4, 8.6], {"*": lambda t: metal_edge(t, (40, 40, 44), (110, 110, 116))})
    m.write({**HELD, "firstperson_righthand": {"rotation": [0, -45, 0], "translation": [0, 4, 0], "scale": [0.4, 0.4, 0.4]},
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
