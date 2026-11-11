"""The things of the role play (original art, drawn here): a torch and its batteries, a walkie-talkie, a video cassette
as 3D models in hand; a table radio and a video recorder as blocks; money, an identity card and a newspaper as flat
pictures; the radio's hiss and the walkie-talkie's squelch (made from noise); their block states, loot and recipes.
Round things are octagons (a square and the same square turned an eighth), boxes have rounded corners."""
import json
import math
import os
import subprocess
import tempfile
import wave

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

import kit
import gear_items
import models
from models import Model, vgrad, metal_edge, round_alpha, text, text_width

models.FONT.setdefault("H", ["#.#", "#.#", "###", "#.#", "#.#"])

DATA = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "data", "eraphones")


def write_data(rel, data):
    path = os.path.join(DATA, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
        f.write("\n")
    print("wrote", os.path.relpath(path))


def shade(color, k):
    return tuple(max(0, min(255, int(c * k))) for c in color[:3]) + (255,)


def lengthwise(dark, light):
    """A side running along a cylinder: a band of light along its middle, as a turned surface catches it."""
    def p(tile):
        metal_edge(tile, dark, light)
    return p


T22 = math.tan(math.radians(22.5))


def octo(m, axis, a0, a1, cx, cy, r, side, end_a=None, end_b=None):
    """A regular eight-sided rod of in-radius {r} along {axis} ("x", "y" or "z") from {a0} to {a1}, centred on
    ({cx}, {cy}) in the two other axes: four thin boxes (a cross, and the same cross turned an eighth), only their
    outer faces painted, so its outline is a true octagon; a round cap at each end. {side} is a colour (each face then
    shaded by how it faces the light) or a painter for every face; {end_a}/{end_b} paint the caps."""
    a = r * T22
    if axis == "x":
        def span(u, v):
            return [a0, cx - u, cy - v], [a1, cx + u, cy + v]
        long_faces = (("up", "down"), ("north", "south"))
        ends = ("west", "east")
        origin = [(a0 + a1) / 2, cx, cy]
    elif axis == "y":
        def span(u, v):
            return [cx - u, a0, cy - v], [cx + u, a1, cy + v]
        long_faces = (("east", "west"), ("north", "south"))
        ends = ("down", "up")
        origin = [cx, (a0 + a1) / 2, cy]
    else:
        def span(u, v):
            return [cx - u, cy - v, a0], [cx + u, cy + v, a1]
        long_faces = (("east", "west"), ("up", "down"))
        ends = ("north", "south")
        origin = [cx, cy, (a0 + a1) / 2]
    light = {"up": 1.3, "south": 1.0, "east": 1.05, "west": .78, "north": .78, "down": .52}

    def paint(face, k=1.0):
        if callable(side):
            return side
        return facet(side, light.get(face, .9) * k)

    for turned in (False, True):
        rot = {"origin": origin, "axis": axis, "angle": 45} if turned else None
        k = .92 if turned else 1.0
        frm, to = span(r, a) if axis != "y" else span(r, a)
        m.box(frm, to, {f: paint(f, k) for f in long_faces[0]}, rotation=rot)
        frm, to = span(a, r)
        m.box(frm, to, {f: paint(f, k) for f in long_faces[1]}, rotation=rot)
    # The caps: a square just past each end, cut to the same octagon so nothing shows through, the end's own
    # picture (a lens, a disc) laid over it. Every end is closed: where the next piece is thinner, the step shows.
    base = side if not callable(side) else (54, 56, 62)
    for end, at, painter in ((ends[0], a0, end_a), (ends[1], a1, end_b)):
        lo, hi = (at - .02, at) if end in ("west", "down", "north") else (at, at + .02)
        if axis == "x":
            frm, to = [lo, cx - r, cy - r], [hi, cx + r, cy + r]
        elif axis == "y":
            frm, to = [cx - r, lo, cy - r], [cx + r, hi, cy + r]
        else:
            frm, to = [cx - r, cy - r, lo], [cx + r, cy + r, hi]
        m.box(frm, to, {end: octagon_cap(base, light.get(end, .9), painter)})


def octagon_cap(color, k, painter=None):
    """An octagon filling its square (flat sides on the square's sides), shaded {k}; {painter} drawn over it."""
    def p(tile):
        w, h = tile.size
        cap = Image.new("RGBA", tile.size, (0, 0, 0, 0))
        pts = [(.5 + .5 * x, .5 + .5 * y) for x, y in ((1, T22), (T22, 1), (-T22, 1), (-1, T22), (-1, -T22), (-T22, -1), (T22, -1), (1, -T22))]
        ImageDraw.Draw(cap).polygon([(x * (w - 1), y * (h - 1)) for x, y in pts], fill=shade(color, k))
        if painter is not None:
            top = Image.new("RGBA", tile.size, (0, 0, 0, 0))
            painter(top)
            cap.alpha_composite(top)
        tile.paste(cap)
    return p


def disc(color, rim, mark=None):
    """The end of a round thing: a disc on its square, a darker rim, a mark across it if wanted."""
    def p(tile):
        w, h = tile.size
        d = ImageDraw.Draw(tile)
        d.ellipse((0, 0, w - 1, h - 1), fill=shade(rim, 1))
        d.ellipse((1, 1, w - 2, h - 2), fill=shade(color, 1))
        d.ellipse((w * .2, h * .15, w * .55, h * .45), fill=shade(color, 1.25))
        if mark:
            d.line((w * .5, h * .2, w * .5, h * .8), fill=shade(mark, 1), width=max(1, w // 8))
    return p


# ------------------------------------------------------------------------------------------------ in hand

TORCH = {
    # Held like a torch: the lens forward, the red switch on top. In third person the item's frame has +y forward and
    # +z up, so a quarter turn about x sends the lens (north) forward, a little short of it to level the raised arm;
    # the grip sits in the fist. In first person the lens points ahead, turned a little toward the crosshair.
    "thirdperson_righthand": {"rotation": [75, 0, 0], "translation": [0, 0.5, 1.5], "scale": [0.62, 0.62, 0.62]},
    "thirdperson_lefthand": {"rotation": [75, 0, 0], "translation": [0, 0.5, 1.5], "scale": [0.62, 0.62, 0.62]},
    "firstperson_righthand": {"rotation": [4, 6, 0], "translation": [0, 3.5, -1], "scale": [0.42, 0.42, 0.42]},
    "firstperson_lefthand": {"rotation": [4, -6, 0], "translation": [0, 3.5, -1], "scale": [0.42, 0.42, 0.42]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.85, 0.85, 0.85]},
    "fixed": {"rotation": [0, 90, 0], "translation": [0, 0, 0], "scale": [0.9, 0.9, 0.9]},
}

def upright_display(gui_scale=0.85, fp_scale=0.4, tp_scale=0.6):
    """Something held upright: its face to the eyes in first person, a little turned toward the crosshair; upright in
    the fist in third person, its face to its holder (the raised arm tilts the hand's frame back eighteen degrees, 72
    rather than 90 rights it)."""
    return {
        "gui": {"rotation": [15, -25, 0], "translation": [0, 0, 0], "scale": [gui_scale] * 3},
        "firstperson_righthand": {"rotation": [-4, -28, 6], "translation": [-1, 3, -1], "scale": [fp_scale] * 3},
        "firstperson_lefthand": {"rotation": [-4, 28, -6], "translation": [-1, 3, -1], "scale": [fp_scale] * 3},
        "thirdperson_righthand": {"rotation": [72, 0, 0], "translation": [0, 1.5, 3], "scale": [tp_scale] * 3},
        "thirdperson_lefthand": {"rotation": [72, 0, 0], "translation": [0, 1.5, 3], "scale": [tp_scale] * 3},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.45] * 3},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.9, 0.9, 0.9]},
        "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
    }


def held(m, display):
    """Writes {m} as an item seen in 3D everywhere, the inventory included."""
    m.write(display)
    kit.write_json(f"models/item/{m.name}.json", {"parent": f"eraphones:item/{m.name}_3d"})


def facet(color, k):
    """A flat face of a round thing, lit by its angle to the light: a solid colour with a soft fall-off along it."""
    def p(tile):
        vgrad(tile, shade(color, k * 1.06), shade(color, k * .94))
    return p


def smooth_octo(m, a0, a1, cy, cz, r, color, end_a=None, end_b=None):
    octo(m, "x", a0, a1, cy, cz, r, color, end_a, end_b)


def lens(tile):
    """The torch's glass: a warm bulb in a silver reflector, rings of light round it."""
    w, h = tile.size
    d = ImageDraw.Draw(tile)
    d.ellipse((0, 0, w - 1, h - 1), fill=(150, 154, 162, 255))
    d.ellipse((1, 1, w - 2, h - 2), fill=(214, 216, 222, 255))
    for k, col in enumerate(((240, 236, 214), (255, 246, 214), (255, 250, 228))):
        inset = w * (.12 + k * .11)
        d.ellipse((inset, inset, w - 1 - inset, h - 1 - inset), fill=col + (255,))
    d.ellipse((w * .42, h * .42, w * .58, h * .58), fill=(255, 214, 120, 255))
    d.ellipse((w * .22, h * .18, w * .4, h * .34), fill=(255, 255, 255, 200))


def flashlight():
    """A torch of the time: a matte black metal body with a knurled grip, a red push switch, a chrome bezel round a
    wide lens, a rounded tail cap. It lies along z, its lens to the north, which its hand transforms turn forward."""
    m = Model("lampe_torche")
    cx = cy = 8
    black, chrome = (46, 48, 54), (176, 180, 190)
    # The head and its lens to the north, then the neck, the body, the grip, the tail
    octo(m, "z", 2.2, 2.8, cx, cy, 2.2, chrome, end_a=lens)
    octo(m, "z", 2.8, 4.6, cx, cy, 2.05, black)
    octo(m, "z", 4.6, 5.6, cx, cy, 1.6, black)
    octo(m, "z", 5.6, 12.8, cx, cy, 1.3, black)
    octo(m, "z", 8.8, 11.6, cx, cy, 1.36, (34, 35, 40))
    octo(m, "z", 12.8, 13.8, cx, cy, 1.25, chrome, end_b=disc((150, 154, 162), (110, 114, 122)))
    # The push switch, red rubber, on top
    red = (200, 44, 40)
    m.box([cx - .55, cy + 1.25, 6.6], [cx + .55, cy + 1.7, 7.8], {"up": facet(red, 1.2), "north": facet(red, .9), "south": facet(red, .7),
                                                                "east": facet(red, .8), "west": facet(red, .8)})
    held(m, TORCH)


def batteries():
    """Two batteries side by side: black and gold, a copper cap."""
    m = Model("piles")
    for cx in (6.4, 9.6):
        octo(m, "y", 3, 11.5, cx, 8, 1.4, (34, 34, 38), end_a=disc((150, 150, 156), (80, 80, 86)))
        octo(m, "y", 7.2, 9.8, cx, 8, 1.45, (214, 160, 50))
        octo(m, "y", 11.5, 12.4, cx, 8, .55, (196, 130, 84), end_b=disc((220, 160, 110), (150, 90, 50)))
    held(m, upright_display(1.0))


def walkie():
    """A walkie-talkie: an olive body with rounded corners, its speaker grille, a green display, keys, the volume
    knob on top and the long aerial."""
    m = Model("talkie_walkie")
    x0, y0, x1, y1, z0, z1 = 5, 1.5, 11, 12.5, 7, 9

    def front(tile):
        w, h = tile.size
        vgrad(tile, (84, 92, 70), (52, 58, 44))
        d = ImageDraw.Draw(tile)
        # Speaker grille
        d.rounded_rectangle((8, 10, w - 9, 62), radius=8, fill=(30, 34, 26, 255))
        for k in range(7):
            d.rounded_rectangle((14, 16 + k * 6, w - 15, 18 + k * 6), radius=1, fill=(10, 12, 9, 255))
        # Display
        d.rounded_rectangle((10, 72, w - 11, 104), radius=4, fill=(16, 20, 14, 255))
        d.rectangle((14, 76, w - 15, 100), fill=(150, 186, 112, 255))
        text(d, "CH", 18, 84, (30, 44, 20, 255), 1)
        text(d, "07", w - 15 - text_width("07", 3), 80, (30, 44, 20, 255), 3)
        # Keys
        for k in range(3):
            for j in range(3):
                d.rounded_rectangle((14 + j * 22, 114 + k * 14, 30 + j * 22, 124 + k * 14), radius=3, fill=(40, 44, 36, 255))
        d.rounded_rectangle((14, 160, w - 15, 170), radius=4, fill=(150, 40, 36, 255))
        round_alpha(tile, 18)

    def back(tile):
        vgrad(tile, (70, 78, 60), (44, 50, 38))
        d = ImageDraw.Draw(tile)
        w, h = tile.size
        d.rounded_rectangle((10, 30, w - 11, h - 30), radius=6, fill=(36, 40, 32, 255))
        round_alpha(tile, 18)

    m.body(x0, y0, x1, y1, z0, z1, 1.2, front, back, (36, 40, 30), (110, 120, 92))
    # The aerial: a fat rubber root and a thin whip
    octo(m, "y", 12.5, 14, 9.6, 8, .55, (36, 36, 40))
    octo(m, "y", 14, 18.5, 9.6, 8, .28, (36, 36, 40), end_b=disc((40, 40, 44), (20, 20, 22)))
    # The knob and the push-to-talk key on the side
    octo(m, "y", 12.5, 13.6, 6.6, 8, .7, (50, 50, 56), end_b=disc((60, 60, 66), (30, 30, 34), mark=(220, 220, 220)))
    m.box([4.6, 7, 7.4], [5, 10, 8.6], {"*": (30, 32, 28), "west": (60, 64, 54)})
    held(m, upright_display())


def cassette():
    """A video cassette: black, a white label across the top, two reels behind a smoked window."""
    m = Model("cassette")
    x0, y0, x1, y1, z0, z1 = 1.5, 4.6, 14.5, 11.4, 7.1, 8.9

    def front(tile):
        w, h = tile.size
        vgrad(tile, (34, 34, 38), (16, 16, 18))
        d = ImageDraw.Draw(tile)
        d.rounded_rectangle((10, 8, w - 11, 40), radius=3, fill=(240, 234, 214, 255))
        d.line((14, 22, w - 15, 22), fill=(200, 60, 50, 255), width=2)
        text(d, "VHS", 16, 28, (60, 54, 48, 255), 2)
        d.rounded_rectangle((44, 52, w - 45, h - 14), radius=6, fill=(46, 40, 38, 255))
        for cx in (w * .33, w * .67):
            d.ellipse((cx - 16, 56, cx + 16, 88), fill=(20, 18, 18, 255))
            d.ellipse((cx - 6, 66, cx + 6, 78), fill=(220, 220, 224, 255))
        round_alpha(tile, 6)

    def back(tile):
        vgrad(tile, (30, 30, 34), (14, 14, 16))
        round_alpha(tile, 6)

    m.body(x0, y0, x1, y1, z0, z1, .4, front, back, (12, 12, 14), (70, 70, 78))
    held(m, upright_display(1.0))


# ------------------------------------------------------------------------------------------------ blocks

def write_block(m, display, render_type="minecraft:cutout"):
    kit.save(m.img, f"textures/block/{m.name}.png")
    extra = {"render_type": render_type} if render_type else {}
    kit.write_json(f"models/block/{m.name}.json", {
        **extra,
        "textures": {"t": f"eraphones:block/{m.name}", "particle": f"eraphones:block/{m.name}"},
        "elements": [dict(e, faces={k: dict(v, texture="#t") for k, v in e["faces"].items()}) for e in m.elements],
        "display": display})
    kit.write_json(f"models/item/{m.name}.json", {"parent": f"eraphones:block/{m.name}"})
    variants = {f"facing={f}": ({"model": f"eraphones:block/{m.name}", "y": y} if y else {"model": f"eraphones:block/{m.name}"})
                for f, y in (("south", 0), ("west", 90), ("north", 180), ("east", 270))}
    kit.write_json(f"blockstates/{m.name}.json", {"variants": variants})
    write_data(f"loot_table/blocks/{m.name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": f"eraphones:{m.name}"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


BLOCK_DISPLAY = {
    "gui": {"rotation": [30, 35, 0], "translation": [0, 1.5, 0], "scale": [0.72, 0.72, 0.72]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.3, 0.3, 0.3]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.6, 0.6, 0.6]},
    # Carried in front by its handle side, its face out; seen from behind and above in first person
    "thirdperson_righthand": {"rotation": [72, 180, 0], "translation": [0, 2, 1], "scale": [0.36, 0.36, 0.36]},
    "thirdperson_lefthand": {"rotation": [72, 180, 0], "translation": [0, 2, 1], "scale": [0.36, 0.36, 0.36]},
    "firstperson_righthand": {"rotation": [8, -24, 0], "translation": [-1, 5.5, -2], "scale": [0.32, 0.32, 0.32]},
    "firstperson_lefthand": {"rotation": [8, 24, 0], "translation": [-1, 5.5, -2], "scale": [0.32, 0.32, 0.32]},
}


def wood(tile, dark=(78, 44, 22), light=(130, 82, 44), seed=3):
    w, h = tile.size
    rng = np.random.default_rng(seed)
    ys = np.arange(h)[:, None]
    xs = np.arange(w)[None, :]
    grain = np.sin(xs / max(1, w) * 3 + np.sin(ys / 7.0 + rng.uniform(0, 6)) * 1.2 + ys / 3.1) * .5 + .5
    t = (grain * .55 + rng.random((h, w)) * .12)[..., None]
    rgb = np.array(dark)[None, None, :] * (1 - t) + np.array(light)[None, None, :] * t
    tile.paste(Image.fromarray(np.dstack([rgb, np.full((h, w), 255)]).astype(np.uint8), "RGBA"))


def radio():
    """A table radio: a walnut case with rounded corners, a cloth speaker, the lit dial, two knobs, a telescopic aerial."""
    m = Model("radio", size=1024)
    x0, y0, x1, y1, z0, z1 = 2, .6, 14, 10, 4, 11

    def front(tile):
        wood(tile)
        w, h = tile.size
        d = ImageDraw.Draw(tile)
        d.rounded_rectangle((6, 6, w * .52, h - 7), radius=8, fill=(52, 30, 16, 255))
        cloth = Image.new("RGBA", (int(w * .52) - 14, h - 21))
        cw, ch = cloth.size
        cd = ImageDraw.Draw(cloth)
        cd.rectangle((0, 0, cw, ch), fill=(184, 162, 122, 255))
        for y in range(0, ch, 3):
            cd.line((0, y, cw, y), fill=(160, 138, 100, 255))
        for x in range(0, cw, 3):
            cd.line((x, 0, x, ch), fill=(170, 148, 110, 255))
        tile.alpha_composite(cloth, (10, 10))
        text(d, "VOXA", int(w * .27) - text_width("VOXA", 2) // 2, h - 30, (60, 36, 20, 255), 2)
        # The dial
        dx0, dy0, dx1, dy1 = int(w * .58), 12, w - 9, 44
        d.rounded_rectangle((dx0 - 3, dy0 - 3, dx1 + 3, dy1 + 3), radius=5, fill=(40, 24, 12, 255))
        d.rounded_rectangle((dx0, dy0, dx1, dy1), radius=4, fill=(246, 220, 154, 255))
        for k in range(0, 21):
            x = dx0 + 5 + k * (dx1 - dx0 - 10) / 20
            d.line((x, dy0 + 6, x, dy0 + (14 if k % 4 == 0 else 10)), fill=(70, 50, 30, 255))
        d.line((dx0 + (dx1 - dx0) * .38, dy0 + 3, dx0 + (dx1 - dx0) * .38, dy1 - 3), fill=(210, 30, 20, 255), width=2)
        text(d, "FM", dx0 + 5, dy1 - 10, (90, 60, 30, 255), 1)
        # The magic eye
        d.ellipse((dx1 - 16, dy1 + 8, dx1 - 4, dy1 + 20), fill=(20, 40, 24, 255))
        d.ellipse((dx1 - 13, dy1 + 11, dx1 - 7, dy1 + 17), fill=(90, 230, 120, 255))
        round_alpha(tile, 18)

    def back(tile):
        wood(tile, seed=5)
        d = ImageDraw.Draw(tile)
        w, h = tile.size
        for k in range(6):
            d.rectangle((20, 20 + k * 12, w - 20, 24 + k * 12), fill=(40, 24, 12, 255))
        round_alpha(tile, 18)

    m.body(x0, y0, x1, y1, z0, z1, 1.3, front, back, (64, 36, 18), (150, 96, 54))
    # The knobs, standing out of the front
    for cx in (9.6, 12.4):
        octo(m, "z", 11, 11.8, cx, 2.6, .8, (120, 80, 44), end_b=disc((120, 80, 44), (60, 36, 20), mark=(240, 220, 180)))
    # Feet
    for cx in (3.2, 12.8):
        m.box([cx - .6, 0, 5], [cx + .6, .6, 10], {"*": (30, 20, 12)})
    # The aerial, leaning back
    m.box([12.6, 10, 5.1], [13, 17, 5.5], {"*": lengthwise((120, 124, 132), (240, 242, 248))},
          rotation={"origin": [12.8, 10, 5.3], "axis": "x", "angle": -22.5})
    write_block(m, BLOCK_DISPLAY)


def vcr():
    """A video recorder: brushed silver, the cassette flap, a blue display, a row of keys, little feet."""
    m = Model("magnetoscope", size=1024)
    x0, y0, x1, y1, z0, z1 = 1, .5, 15, 4.6, 2, 13

    def front(tile):
        w, h = tile.size
        vgrad(tile, (214, 216, 222), (150, 154, 160))
        # Brushed lines, blended over (drawn straight on, their faint alpha would make holes in the face)
        lines = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        ld = ImageDraw.Draw(lines)
        for y in range(0, h, 2):
            ld.line((0, y, w, y), fill=(255, 255, 255, 18))
        tile.alpha_composite(lines)
        d = ImageDraw.Draw(tile)
        d.rounded_rectangle((10, 12, int(w * .45), h - 14), radius=3, fill=(26, 28, 32, 255))
        d.line((12, 16, int(w * .45) - 2, 16), fill=(70, 72, 80, 255))
        d.rounded_rectangle((int(w * .52), 12, w - 12, 38), radius=3, fill=(10, 12, 16, 255))
        text(d, "12:00", w - 16 - text_width("12:00", 2), 18, (110, 224, 255, 255), 2)
        for k in range(6):
            kx = int(w * .52) + k * 18
            d.rounded_rectangle((kx, h - 22, kx + 14, h - 12), radius=2, fill=(60, 62, 70, 255))
        text(d, "VOXA", 12, h - 10, (90, 92, 100, 255), 1)
        round_alpha(tile, 6)

    def top(tile):
        vgrad(tile, (190, 192, 198), (140, 142, 150))

    m.body(x0, y0, x1, y1, z0, z1, .6, front, top, (100, 104, 112), (230, 232, 238), corners=False)
    for cx in (2.5, 13.5):
        for cz in (3.5, 11.5):
            m.box([cx - .7, 0, cz - .7], [cx + .7, .5, cz + .7], {"*": (24, 24, 26)})
    write_block(m, BLOCK_DISPLAY)


# ------------------------------------------------------------------------------------------------ flat things

def flat(name, img):
    kit.save(img, f"textures/item/{name}.png")
    kit.write_json(f"models/item/{name}.json", {"parent": "item/generated", "textures": {"layer0": f"eraphones:item/{name}"}})


def bill(value, tint):
    """A dollar bill of no real country: a border, a portrait in an oval, the value in the corners."""
    s = 4
    img = Image.new("RGBA", (32 * s, 32 * s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    x0, y0, x1, y1 = 2 * s, 9 * s, 30 * s, 23 * s
    paper = tuple(int(c) for c in tint)
    d.rectangle((x0, y0, x1, y1), fill=paper + (255,))
    d.rectangle((x0 + s, y0 + s, x1 - s, y1 - s), outline=shade(paper, .55), width=s)
    d.ellipse((12 * s, y0 + 2 * s, 20 * s, y1 - 2 * s), fill=shade(paper, 1.18), outline=shade(paper, .5), width=s // 2)
    d.ellipse((14.6 * s, y0 + 3.4 * s, 17.4 * s, y0 + 6.4 * s), fill=shade(paper, .55))
    d.pieslice((13.4 * s, y0 + 6 * s, 18.6 * s, y1 - .5 * s), 180, 360, fill=shade(paper, .55))
    label = str(value)
    text(d, label, x0 + 2 * s, y0 + 2 * s, shade(paper, .4), s // 2 + 1)
    text(d, label, x1 - 2 * s - text_width(label, s // 2 + 1), y1 - 2 * s - 5 * (s // 2 + 1), shade(paper, .4), s // 2 + 1)
    img = img.filter(ImageFilter.GaussianBlur(.6))
    return img.resize((32, 32), Image.LANCZOS)


def coin(cents):
    s = 4
    img = Image.new("RGBA", (32 * s, 32 * s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    r = 12 if cents == 25 else 10
    c = 16 * s
    metal = (196, 198, 204) if cents >= 10 else (190, 120, 70)
    d.ellipse((c - r * s, c - r * s, c + r * s, c + r * s), fill=shade(metal, .7))
    d.ellipse((c - (r - 1) * s, c - (r - 1) * s, c + (r - 1) * s, c + (r - 1) * s), fill=shade(metal, 1))
    d.ellipse((c - (r - 1) * s, c - (r - 1) * s, c + (r - 4) * s, c + (r - 4) * s), fill=shade(metal, 1.15))
    label = str(cents)
    text(d, label, c - text_width(label, 2 * s // 2) // 2, c - 5 * s // 2, shade(metal, .5), 2 * s // 2)
    return img.filter(ImageFilter.GaussianBlur(.5)).resize((32, 32), Image.LANCZOS)


def id_card():
    s = 4
    img = Image.new("RGBA", (32 * s, 32 * s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((2 * s, 8 * s, 30 * s, 25 * s), radius=2 * s, fill=(232, 238, 226, 255), outline=(120, 140, 130, 255), width=s // 2)
    d.rectangle((2 * s + 2, 8 * s + 2, 30 * s - 2, 11 * s), fill=(46, 90, 138, 255))
    d.rectangle((4 * s, 13 * s, 11 * s, 23 * s), fill=(180, 196, 204, 255))
    d.ellipse((5.6 * s, 14 * s, 9.4 * s, 18 * s), fill=(120, 90, 70, 255))
    d.pieslice((4.6 * s, 17.6 * s, 10.4 * s, 25 * s), 180, 360, fill=(70, 80, 100, 255))
    for k in range(4):
        d.rectangle((13 * s, (14 + k * 2.4) * s, (27 - k * 2) * s, (14.8 + k * 2.4) * s), fill=(70, 86, 80, 255))
    return img.filter(ImageFilter.GaussianBlur(.4)).resize((32, 32), Image.LANCZOS)


def sheet():
    """A sheet of paper, a corner turned, a few lines written on it."""
    s = 4
    img = Image.new("RGBA", (32 * s, 32 * s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.polygon([(7 * s, 4 * s), (21 * s, 4 * s), (26 * s, 9 * s), (26 * s, 28 * s), (7 * s, 28 * s)], fill=(246, 244, 236, 255), outline=(150, 146, 132, 255))
    d.polygon([(21 * s, 4 * s), (21 * s, 9 * s), (26 * s, 9 * s)], fill=(216, 212, 198, 255), outline=(150, 146, 132, 255))
    for k in range(6):
        y = (11 + k * 2.6) * s
        d.line((9.5 * s, y, (23 - (k % 3) * 2.5) * s, y), fill=(40, 60, 130, 255), width=s // 2 + 1)
    d.line((15 * s, 25.5 * s, 23 * s, 25 * s), fill=(40, 60, 130, 255), width=s // 2)
    return img.filter(ImageFilter.GaussianBlur(.4)).resize((32, 32), Image.LANCZOS)


def newspaper():
    s = 4
    img = Image.new("RGBA", (32 * s, 32 * s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.polygon([(4 * s, 5 * s), (27 * s, 4 * s), (28 * s, 28 * s), (5 * s, 28 * s)], fill=(236, 228, 204, 255), outline=(150, 140, 116, 255))
    d.rectangle((7 * s, 7 * s, 25 * s, 10 * s), fill=(30, 26, 22, 255))
    d.line((7 * s, 11.5 * s, 25 * s, 11.5 * s), fill=(30, 26, 22, 255), width=s // 2)
    d.rectangle((7 * s, 13 * s, 15 * s, 20 * s), fill=(110, 104, 92, 255))
    for k in range(6):
        d.rectangle((17 * s, (13 + k * 1.6) * s, 25 * s, (13.6 + k * 1.6) * s), fill=(120, 112, 98, 255))
    for k in range(3):
        d.rectangle((7 * s, (22 + k * 1.8) * s, 25 * s, (22.6 + k * 1.8) * s), fill=(120, 112, 98, 255))
    d.line((5 * s, 16 * s, 28 * s, 16.4 * s), fill=(200, 190, 166, 255), width=s // 2)
    return img.filter(ImageFilter.GaussianBlur(.4)).resize((32, 32), Image.LANCZOS)


MONEY = {10: None, 25: None, 100: (150, 190, 140), 500: (170, 196, 150), 1000: (190, 200, 150), 2000: (150, 186, 160), 5000: (176, 166, 196),
         10000: (150, 190, 180)}


def money_id(cents):
    return f"piece_{cents}c" if cents < 100 else f"billet_{cents // 100}"


# ------------------------------------------------------------------------------------------------ sounds

def ogg(name, samples, rate=48000):
    samples = np.clip(samples, -1, 1)
    path = os.path.join(kit.ROOT, "sounds", f"{name}.ogg")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, "s.wav")
        with wave.open(wav, "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(rate)
            w.writeframes((samples * 32767).astype("<i2").tobytes())
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "4", path], check=True)
    print("wrote", os.path.relpath(path))


def band(x, lo, hi, rate=48000):
    spec = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / rate)
    spec[(f < lo) | (f > hi)] = 0
    return np.fft.irfft(spec, len(x))


def sounds():
    rng = np.random.default_rng(9)
    rate = 48000
    # The hiss between stations: band-limited noise that breathes, crackles, a faint whistle drifting; loops seamlessly
    n = rate * 4
    noise = band(rng.normal(0, 1, n), 300, 7000)
    noise /= np.abs(noise).max()
    t = np.arange(n) / rate
    breath = .75 + .25 * np.sin(2 * np.pi * t / 4 * 3) * np.sin(2 * np.pi * t / 4 * 5)
    crack = np.zeros(n)
    for _ in range(60):
        k = rng.integers(0, n - 200)
        crack[k:k + 200] += rng.normal(0, 1, 200) * np.exp(-np.arange(200) / 25) * rng.uniform(.3, 1)
    whistle = np.sin(2 * np.pi * (2100 * t + 60 * np.sin(2 * np.pi * t / 4))) * .03
    hiss = noise * .55 * breath + crack * .35 + whistle
    ogg("radio_static", hiss / np.abs(hiss).max() * .8)
    # The squelch: a short burst of noise, cut off
    m = int(rate * .32)
    t = np.arange(m) / rate
    burst = band(rng.normal(0, 1, m), 500, 5000)
    burst /= np.abs(burst).max()
    env = np.clip(t / .01, 0, 1) * np.clip((.3 - t) / .03, 0, 1)
    beep = np.sin(2 * np.pi * 1250 * t) * (t < .07) * .5
    ogg("talkie_squelch", (burst * .6 * env + beep) * .9)
    import horror_items
    ogg("boite_a_musique", horror_items.music_box_tune(rate))
    # Fear: one heartbeat as heard from inside (a deep thud, its echo)
    n = int(rate * .62)
    t = np.arange(n) / rate

    def thud(at, f, k):
        tt = np.clip(t - at, 0, None)
        return np.sin(2 * np.pi * f * tt * (1 - tt * .8)) * np.exp(-tt * 22) * (t >= at) * k

    beat = thud(0, 52, 1) + thud(.21, 44, .7) + band(rng.normal(0, 1, n), 30, 160) * np.exp(-t * 18) * .25
    ogg("coeur_peur", beat / np.abs(beat).max() * .95)
    # Fear, past two thirds: a short shaking breath, in through the mouth then out (its own noise, the rest unchanged)
    br = np.random.default_rng(31)
    n = int(rate * 1.45)
    t = np.arange(n) / rate

    def puff(at, length, lo, hi, k):
        x = band(br.normal(0, 1, n), lo, hi)
        x /= np.abs(x).max()
        tt = (t - at) / length
        env = np.where((tt >= 0) & (tt <= 1), np.sin(np.clip(tt, 0, 1) * np.pi) ** 1.5, 0)
        shake = 1 + .35 * np.sin(2 * np.pi * 9.5 * t + br.uniform(0, 6)) * np.sin(2 * np.pi * 3.1 * t)
        return x * env * shake * k

    breath = puff(.02, .42, 700, 4200, .8) + puff(.52, .7, 300, 2600, 1.0)
    ogg("souffle_peur", breath / np.abs(breath).max() * .7)
    import photo_assets
    ogg("photo_declic", photo_assets.shutter(rate))
    import booth_assets
    ogg("cabine_sonnerie", booth_assets.bell(rate))
    ogg("cabine_piece", booth_assets.coin(rate))
    ogg("cabine_touche", booth_assets.key(rate))
    import bike_assets
    ogg("velo_sonnette", bike_assets.bell(rate))
    import machine_assets
    ogg("repondeur_bip", machine_assets.beep(rate))
    import lamp_assets
    ogg("lampadaire_bourdonnement", lamp_assets.buzz(rate))
    ogg("lampadaire_claquement", lamp_assets.click(rate))
    ogg("coupure_courant", lamp_assets.power_down(rate))
    kit.write_json("sounds.json", {
        "radio_static": {"sounds": [{"name": "eraphones:radio_static"}], "subtitle": "subtitles.eraphones.radio_static"},
        "talkie_squelch": {"sounds": [{"name": "eraphones:talkie_squelch"}], "subtitle": "subtitles.eraphones.talkie_squelch"},
        "boite_a_musique": {"sounds": [{"name": "eraphones:boite_a_musique", "attenuation_distance": 20}],
                            "subtitle": "subtitles.eraphones.boite_a_musique"},
        "coeur_peur": {"sounds": [{"name": "eraphones:coeur_peur"}]},
        "souffle_peur": {"sounds": [{"name": "eraphones:souffle_peur"}]},
        "photo_declic": {"sounds": [{"name": "eraphones:photo_declic"}], "subtitle": "subtitles.eraphones.photo_declic"},
        "cabine_sonnerie": {"sounds": [{"name": "eraphones:cabine_sonnerie", "attenuation_distance": 32}],
                            "subtitle": "subtitles.eraphones.cabine_sonnerie"},
        "cabine_piece": {"sounds": [{"name": "eraphones:cabine_piece"}], "subtitle": "subtitles.eraphones.cabine_piece"},
        "cabine_touche": {"sounds": [{"name": "eraphones:cabine_touche"}]},
        "velo_sonnette": {"sounds": [{"name": "eraphones:velo_sonnette", "attenuation_distance": 24}], "subtitle": "subtitles.eraphones.velo_sonnette"},
        "lampadaire_bourdonnement": {"sounds": [{"name": "eraphones:lampadaire_bourdonnement", "attenuation_distance": 20}],
                                     "subtitle": "subtitles.eraphones.lampadaire_bourdonnement"},
        "lampadaire_claquement": {"sounds": [{"name": "eraphones:lampadaire_claquement", "attenuation_distance": 20}],
                                  "subtitle": "subtitles.eraphones.lampadaire_claquement"},
        "coupure_courant": {"sounds": [{"name": "eraphones:coupure_courant"}], "subtitle": "subtitles.eraphones.coupure_courant"},
        "repondeur_bip": {"sounds": [{"name": "eraphones:repondeur_bip"}], "subtitle": "subtitles.eraphones.repondeur_bip"}})


# ------------------------------------------------------------------------------------------------ recipes

def shaped(name, pattern, key, count=1):
    write_data(f"recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
                                       "key": {k: {"item": v} for k, v in key.items()}, "result": {"id": f"eraphones:{name}", "count": count}})


def recipes():
    shaped("lampe_torche", ["  G", " I ", "B  "], {"G": "minecraft:glass_pane", "I": "minecraft:iron_ingot", "B": "eraphones:piles"})
    shaped("piles", ["C", "R", "I"], {"C": "minecraft:copper_ingot", "R": "minecraft:redstone", "I": "minecraft:iron_nugget"}, 4)
    shaped("talkie_walkie", [" I ", "BRB", " C "], {"I": "minecraft:iron_ingot", "B": "minecraft:black_dye", "R": "minecraft:redstone",
                                                    "C": "minecraft:copper_ingot"})
    shaped("radio", ["  I", "PRP", "PWP"], {"I": "minecraft:iron_ingot", "P": "minecraft:dark_oak_planks", "R": "minecraft:redstone",
                                             "W": "minecraft:white_wool"})
    shaped("magnetoscope", ["III", "RCR", "III"], {"I": "minecraft:iron_ingot", "R": "minecraft:redstone", "C": "minecraft:copper_ingot"})
    shaped("carte_identite", ["P", "D"], {"P": "minecraft:paper", "D": "minecraft:blue_dye"})


def main():
    flashlight()
    batteries()
    walkie()
    cassette()
    radio()
    vcr()
    gear_items.main(MONEY, money_id)
    import horror_items
    horror_items.main()
    import photo_assets
    photo_assets.main()
    import booth_assets
    booth_assets.main()
    import bike_assets
    bike_assets.main()
    import lamp_assets
    lamp_assets.main()
    import machine_assets
    machine_assets.main()
    import camcorder_assets
    camcorder_assets.main()
    sounds()
    recipes()


if __name__ == "__main__":
    main()
