"""The 1980s bicycle (original design, drawn here): a candy-red frame with its maker's name on the down tube, a
long black banana seat with a chrome sissy bar, high-rise chrome handlebars with black foam grips and a padded
crossbar, white-wall tyres on chrome rims with thin spokes, chrome mudguards, a round front lamp, a bell, a chain
under its guard. It is drawn in parts the game moves: the frame; a wheel (drawn once, used twice, turning); the
cranks with the chainring (turning); a pedal (twice, staying level); the steering (fork, front mudguard, lamp,
stem, bars, bell), turning about the head tube. And the whole bike as one model for the item.

Bike units are sixteenths of a block: x across (the drive side at -x), y up from the ground, z forward. A part's
model holds bike coordinates moved by (8, 0, 8); the wheel, the cranks and the pedal are drawn round (8, 8, 8)."""
import math

import numpy as np
from PIL import Image, ImageDraw, ImageFont

import gear_assets as ga
import kit
from models import Model, vgrad, metal_edge, text, text_width

# Where things are (bike units)
WHEEL_R = 5.3
REAR = (5.3, -8.5)          # (y, z) of the rear axle
FRONT = (5.3, 7.36)         # the front axle
BB = (4.8, -1.5)            # the bottom bracket
CLUSTER = (13.4, -5.15)     # where the seat tube meets the top tube
HEAD_LOW, HEAD_TOP = (11.0, 5.0), (14.0, 3.76)
CRANK_R = 2.6

RED_DARK, RED_LIGHT = (110, 8, 14), (236, 64, 60)
CHROME_DARK, CHROME_LIGHT = (96, 100, 108), (246, 248, 252)
BLACK_DARK, BLACK_LIGHT = (12, 12, 14), (70, 70, 76)

ANGLES = (-45, -22.5, 0, 22.5, 45)


def red(tile):
    metal_edge(tile, RED_DARK, RED_LIGHT)


def chrome(tile):
    metal_edge(tile, CHROME_DARK, CHROME_LIGHT)


def foam(tile):
    """Black foam: dull, a fine grain."""
    w, h = tile.size
    rng = np.random.default_rng(w * 31 + h)
    v = 30 + rng.normal(0, 5, (h, w))
    tile.paste(Image.fromarray(np.dstack([v, v, v + 2, np.full((h, w), 255)]).clip(0, 255).astype(np.uint8), "RGBA"))


def snap(a):
    best = min(ANGLES, key=lambda x: abs(x - a))
    assert abs(best - a) < 3.5, f"angle {a} cannot be drawn"
    return best


def tube(m, p0, p1, r, paint, x=0.0, ends=None):
    """A straight tube in the bike's side plane, from p0 to p1 ((y, z) each), {r} its half thickness."""
    (y0, z0), (y1, z1) = p0, p1
    dy, dz = y1 - y0, z1 - z0
    length = math.hypot(dy, dz)
    theta = math.degrees(math.atan2(dz, dy))
    my, mz = (y0 + y1) / 2, (z0 + z1) / 2 + 8
    cx = 8 + x
    faces = {"*": paint}
    if ends is not None:
        faces.update(ends)
    if abs(theta) <= 47:
        a = snap(theta)
        frm, to = [cx - r, my - length / 2, mz - r], [cx + r, my + length / 2, mz + r]
    else:
        a = snap(theta - 90 if theta > 0 else theta + 90)
        frm, to = [cx - r, my - r, mz - length / 2], [cx + r, my + r, mz + length / 2]
    m.box(frm, to, faces, rotation={"origin": [cx, my, mz], "axis": "x", "angle": a} if a else None)


def ring(m, cy, cz, radius, thick, half_w, outer, inner, side, x=0.0, keep=None):
    """A ring in the side plane round (cy, cz) (model units), sixteen straight pieces; {keep} chooses which by the angle
    (degrees from the top, forward positive) of their middle."""
    seg = 2 * radius * math.tan(math.pi / 16) * 1.04
    cx = 8 + x
    pieces = []
    for a in ANGLES:
        pieces.append(("top", a, a))
        pieces.append(("bottom", a, 180 + a))
    for a in (-22.5, 0, 22.5):
        pieces.append(("front", a, 90 - a))
        pieces.append(("back", a, 270 - a))
    for where, a, at in pieces:
        at = (at + 360) % 360
        if keep is not None and not keep(at):
            continue
        if where == "top":
            frm, to = [cx - half_w, cy + radius - thick, cz - seg / 2], [cx + half_w, cy + radius, cz + seg / 2]
            faces = {"up": outer, "down": inner}
        elif where == "bottom":
            frm, to = [cx - half_w, cy - radius, cz - seg / 2], [cx + half_w, cy - radius + thick, cz + seg / 2]
            faces = {"down": outer, "up": inner}
        elif where == "front":
            frm, to = [cx - half_w, cy - seg / 2, cz + radius - thick], [cx + half_w, cy + seg / 2, cz + radius]
            faces = {"south": outer, "north": inner}
        else:
            frm, to = [cx - half_w, cy - seg / 2, cz - radius], [cx + half_w, cy + seg / 2, cz - radius + thick]
            faces = {"north": outer, "south": inner}
        faces["east"] = side
        faces["west"] = side
        m.box(frm, to, faces, rotation={"origin": [cx, cy, cz], "axis": "x", "angle": a} if a else None)


# ------------------------------------------------------------------------------------------------ the parts

def tread(tile):
    w, h = tile.size
    vgrad(tile, (40, 40, 44), (22, 22, 24))
    d = ImageDraw.Draw(tile)
    along_w = w >= h
    n = (w if along_w else h)
    for k in range(0, n, 3):
        if along_w:
            d.line((k, 0, k, h), fill=(14, 14, 16, 255), width=1)
        else:
            d.line((0, k, w, k), fill=(14, 14, 16, 255), width=1)


def whitewall(tile):
    """The tyre's side: black rubber, a white band round it."""
    w, h = tile.size
    tile.paste(Image.new("RGBA", tile.size, (26, 26, 28, 255)))
    d = ImageDraw.Draw(tile)
    radial_is_h = h <= w
    if radial_is_h:
        d.rectangle((0, h * .3, w, h * .72), fill=(236, 234, 226, 255))
    else:
        d.rectangle((w * .3, 0, w * .72, h), fill=(236, 234, 226, 255))


def spokes(n, seed):
    """A disc of thin spokes from the hub to the rim (the rest clear), the hub's flange in the middle."""
    def p(tile):
        w, h = tile.size
        S = 4
        img = Image.new("RGBA", (w * S, h * S), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        cx, cy = w * S / 2, h * S / 2
        r_out, r_hub = w * S / 2 * .99, w * S / 2 * .12
        for k in range(n):
            a = 2 * math.pi * (k + seed * .5) / n
            # Crossed lacing: each spoke leaves the hub at a tangent
            b = a + (.38 if k % 2 else -.38)
            d.line((cx + r_hub * math.cos(b), cy + r_hub * math.sin(b), cx + r_out * math.cos(a), cy + r_out * math.sin(a)),
                   fill=(214, 218, 226, 255), width=S)
        d.ellipse((cx - r_hub * 1.3, cy - r_hub * 1.3, cx + r_hub * 1.3, cy + r_hub * 1.3), fill=(196, 200, 208, 255),
                  outline=(120, 124, 132, 255), width=S)
        tile.alpha_composite(img.resize((w, h), Image.LANCZOS))
    return p


def wheel():
    m = Model("velo_roue", 512, 16)
    c = 8
    ring(m, c, c, WHEEL_R, .8, .6, tread, ga.facet((30, 30, 32), .8), whitewall)
    ring(m, c, c, WHEEL_R - .78, .32, .42, chrome, chrome, chrome)
    r = WHEEL_R - 1.05
    for x, seed in ((-.18, 0), (.18, 1)):
        m.box([c + x, c - r, c - r], [c + x, c + r, c + r], {"east": spokes(18, seed), "west": spokes(18, seed)})
    ga.octo(m, "x", c - .75, c + .75, c, c, .42, (210, 214, 220), end_a=ga.disc((180, 184, 190), (110, 114, 120)),
            end_b=ga.disc((180, 184, 190), (110, 114, 120)))
    return m


def chainring(tile):
    """The chainring: a toothed chrome ring on five arms, clear between them."""
    w, h = tile.size
    S = 4
    img = Image.new("RGBA", (w * S, h * S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy, R = w * S / 2, h * S / 2, w * S / 2 * .97
    teeth = 36
    pts = []
    for k in range(teeth * 2):
        a = math.pi * k / teeth
        rr = R if k % 2 == 0 else R * .93
        pts.append((cx + rr * math.cos(a), cy + rr * math.sin(a)))
    d.polygon(pts, fill=(200, 204, 212, 255))
    d.ellipse((cx - R * .8, cy - R * .8, cx + R * .8, cy + R * .8), fill=(0, 0, 0, 0))
    for k in range(5):
        a = 2 * math.pi * k / 5
        d.line((cx, cy, cx + R * .82 * math.cos(a), cy + R * .82 * math.sin(a)), fill=(176, 180, 188, 255), width=int(S * 2.2))
    d.ellipse((cx - R * .2, cy - R * .2, cx + R * .2, cy + R * .2), fill=(150, 154, 162, 255))
    tile.alpha_composite(img.resize((w, h), Image.LANCZOS))


def crank():
    m = Model("velo_pedalier", 256, 24)
    c = 8
    m.box([c - 1.32, c - 2.0, c - 2.0], [c - 1.2, c + 2.0, c + 2.0], {"east": chainring, "west": chainring})
    arm = {"*": chrome}
    m.box([c - 1.75, c - .3, c - .32], [c - 1.4, c + CRANK_R + .3, c + .32], arm)
    m.box([c + 1.4, c - CRANK_R - .3, c - .32], [c + 1.75, c + .3, c + .32], arm)
    ga.octo(m, "x", c - 1.4, c + 1.4, c, c, .32, (200, 204, 210))
    return m


def pedal():
    m = Model("velo_pedale", 128, 24)
    c = 8

    def grip(tile):
        w, h = tile.size
        vgrad(tile, (40, 40, 44), (20, 20, 22))
        d = ImageDraw.Draw(tile)
        for x in range(1, w, 3):
            d.line((x, 0, x, h), fill=(80, 80, 86, 255), width=1)

    m.box([c - .9, c - .2, c - .6], [c + .9, c + .2, c + .6], {"up": grip, "down": grip, "east": chrome, "west": chrome,
                                                                 "north": (230, 120, 20), "south": (230, 120, 20)})
    return m


def decal(word, color=(250, 248, 240)):
    """The maker's name along a tube, white with a thin black edge."""
    def p(tile):
        red(tile)
        w, h = tile.size
        long = max(w, h)
        img = Image.new("RGBA", (long, min(w, h)), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        size = int(min(w, h) * .8)
        font = ImageFont.truetype(kit_font(), size)
        d.text((long / 2, min(w, h) / 2), word, font=font, fill=color + (255,), anchor="mm", stroke_width=1, stroke_fill=(30, 10, 10, 255))
        if h > w:
            img = img.rotate(90, expand=True)
        tile.alpha_composite(img)
    return p


def kit_font():
    import os
    return os.path.join(os.path.dirname(os.path.abspath(__file__)), "fonts", "Oswald-Bold.woff")


def chain(tile):
    w, h = tile.size
    tile.paste(Image.new("RGBA", tile.size, (60, 60, 64, 255)))
    d = ImageDraw.Draw(tile)
    for x in range(0, w, 3):
        d.rectangle((x, 0, x + 1, h), fill=(140, 142, 148, 255))


def guard(tile):
    chrome(tile)
    w, h = tile.size
    d = ImageDraw.Draw(tile)
    font = ImageFont.truetype(kit_font(), max(8, int(h * .45)))
    d.text((w / 2, h / 2), "VOXA", font=font, fill=(150, 20, 24, 255), anchor="mm")


def banana_seat(tile):
    """Glossy black vinyl, a sparkle of silver flecks, a pale highlight along it."""
    w, h = tile.size
    rng = np.random.default_rng(w + 7 * h)
    v = np.full((h, w), 24.0)
    ys = np.linspace(-1, 1, h)[:, None]
    v += 40 * np.exp(-((ys + .35) / .25) ** 2)
    fleck = (rng.random((h, w)) < .03) * 120
    v = v + fleck
    tile.paste(Image.fromarray(np.dstack([v, v, v + 6, np.full((h, w), 255)]).clip(0, 255).astype(np.uint8), "RGBA"))


def frame():
    m = Model("velo_cadre", 1024, 16)
    redp = red
    tube(m, BB, CLUSTER, .45, redp)
    tube(m, CLUSTER, (CLUSTER[0], HEAD_TOP[1] + .3), .42, redp)
    tube(m, BB, (11.21, 4.91), .5, decal("VOXA"))
    tube(m, HEAD_LOW, HEAD_TOP, .62, redp)
    for x in (-.95, .95):
        tube(m, (5.05, BB[1] - .3), (5.05, REAR[1]), .28, redp, x=x)
        tube(m, REAR, CLUSTER, .28, redp, x=x)
    # Chrome bands on the head tube
    tube(m, (11.0, 5.0), (11.45, 4.81), .68, chrome)
    tube(m, (13.55, 3.95), (14.0, 3.76), .68, chrome)
    # The bottom bracket shell
    ga.octo(m, "x", 8 - 1.2, 8 + 1.2, BB[0], BB[1] + 8, .7, RED_DARK)
    # Rear dropouts and the axle's nuts
    for x in (-1.0, 1.0):
        ga.octo(m, "x", 8 + x - .25 if x > 0 else 8 + x - .25, 8 + x + .25, REAR[0], REAR[1] + 8, .35, (200, 204, 210))
    # The rear sprocket on the drive side
    ga.octo(m, "x", 8 - 1.4, 8 - 1.2, REAR[0], REAR[1] + 8, .8, (170, 174, 180))
    # The chain: its top run and its bottom run
    m.box([8 - 1.36, 6.55, REAR[1] + 8], [8 - 1.24, 6.8, BB[1] + 8], {"*": chain})
    m.box([8 - 1.36, 3.4, REAR[1] + 8 + .3], [8 - 1.24, 3.65, BB[1] + 8 - .2], {"*": chain})
    # The chain guard over the chainring's top
    m.box([8 - 1.9, 5.8, BB[1] + 8 - 3.5], [8 - 1.78, 7.4, BB[1] + 8 + 1.6], {"west": guard, "east": chrome, "up": chrome, "down": chrome,
                                                                            "north": chrome, "south": chrome})
    # The seat post, the banana seat and the sissy bar
    tube(m, CLUSTER, (14.9, -5.77), .3, chrome)
    m.box([8 - 1.3, 14.9, -10.2 + 8], [8 + 1.3, 15.8, -3.5 + 8], {"up": banana_seat, "*": ga.facet((20, 20, 22), .9)})
    m.box([8 - 1.3, 15.3, -12.4 + 8], [8 + 1.3, 16.1, -10.0 + 8], {"up": banana_seat, "*": ga.facet((20, 20, 22), .9)},
          rotation={"origin": [8, 15.3, -10.0 + 8], "axis": "x", "angle": 22.5})
    for x in (-1.15, 1.15):
        tube(m, (5.6, -10.4), (15.0, -10.4), .2, chrome, x=x)
        tube(m, (15.6, -11.6), (20.4, -11.6), .2, chrome, x=x)
    m.box([8 - 1.35, 20.2, -11.8 + 8], [8 + 1.35, 20.6, -11.4 + 8], {"*": chrome})
    # The rear mudguard
    ring(m, REAR[0], REAR[1] + 8, WHEEL_R + .7, .25, .75, chrome, chrome, chrome, keep=lambda a: a >= 250 or a <= 30)
    # A red reflector at the guard's back
    m.box([8 - .45, 9.0, REAR[1] + 8 - 6.2], [8 + .45, 9.8, REAR[1] + 8 - 5.95], {"north": (210, 20, 20), "*": (120, 10, 10)})
    # The kickstand folded along the left chain stay
    m.box([8 + 1.3, 4.2, -5.6 + 8], [8 + 1.55, 4.5, -1.8 + 8], {"*": chrome})
    return m


def steering():
    m = Model("velo_guidon", 512, 16)
    for x in (-.95, .95):
        tube(m, FRONT, HEAD_LOW, .3, chrome, x=x)
    m.box([8 - 1.25, 10.6, HEAD_LOW[1] + 8 - .55], [8 + 1.25, 11.25, HEAD_LOW[1] + 8 + .55], {"*": chrome})
    # The stem up out of the head tube
    tube(m, HEAD_TOP, (15.4, 3.18), .32, chrome)
    by, bz = 15.4, 3.18 + 8
    # High-rise bars: a cross piece, two uprights, the grips swept back, the padded crossbar
    m.box([8 - 3.8, by - .22, bz - .22], [8 + 3.8, by + .22, bz + .22], {"*": chrome})
    for x in (-3.6, 3.6):
        m.box([8 + x - .22, by, bz - .22], [8 + x + .22, by + 2.0, bz + .22], {"*": chrome})
        m.box([8 + x - .34, by + 1.7, bz - 1.9], [8 + x + .34, by + 2.38, bz + .2], {"*": foam})
    m.box([8 - 3.4, by + .7, bz - .3], [8 + 3.4, by + 1.25, bz + .3], {"*": foam, "south": pad_face})
    # The bell on the left of the bars
    ga.octo(m, "y", by + .22, by + .62, 8 + 2.5, bz, .45, (220, 222, 228), end_b=ga.disc((236, 238, 244), (150, 152, 160)))
    # The round lamp on the fork crown, its glass forward
    ga.octo(m, "z", HEAD_LOW[1] + 8 + .6, HEAD_LOW[1] + 8 + 1.7, 8, 10.1, .8, (210, 214, 220), end_b=lamp_glass)
    # The front mudguard
    ring(m, FRONT[0], FRONT[1] + 8, WHEEL_R + .7, .25, .75, chrome, chrome, chrome, keep=lambda a: a <= 60 or a >= 330)
    return m


def pad_face(tile):
    foam(tile)
    w, h = tile.size
    d = ImageDraw.Draw(tile)
    font = ImageFont.truetype(kit_font(), max(8, int(h * .7)))
    d.text((w / 2, h / 2), "VOXA", font=font, fill=(220, 40, 40, 255), anchor="mm")


def lamp_glass(tile):
    w, h = tile.size
    d = ImageDraw.Draw(tile)
    d.ellipse((0, 0, w - 1, h - 1), fill=(170, 174, 180, 255))
    d.ellipse((w * .1, h * .1, w * .9, h * .9), fill=(250, 240, 200, 255))
    d.ellipse((w * .25, h * .2, w * .5, h * .45), fill=(255, 255, 250, 255))


# ------------------------------------------------------------------------------------------------ writing them

def write_part(m):
    kit.save(m.img, f"textures/block/{m.name}.png")
    kit.write_json(f"models/block/{m.name}.json", {
        "ambientocclusion": False,
        "textures": {"t": f"eraphones:block/{m.name}", "particle": f"eraphones:block/{m.name}"},
        "elements": [dict(e, faces={k: dict(v, texture="#t") for k, v in e["faces"].items()}) for e in m.elements]})


def moved(elements, key, dx, dy, dz):
    out = []
    for e in elements:
        e = dict(e)
        e["from"] = [round(e["from"][0] + dx, 4), round(e["from"][1] + dy, 4), round(e["from"][2] + dz, 4)]
        e["to"] = [round(e["to"][0] + dx, 4), round(e["to"][1] + dy, 4), round(e["to"][2] + dz, 4)]
        if "rotation" in e:
            r = dict(e["rotation"])
            o = r["origin"]
            r["origin"] = [round(o[0] + dx, 4), round(o[1] + dy, 4), round(o[2] + dz, 4)]
            e["rotation"] = r
        e["faces"] = {k: dict(v, texture=key) for k, v in e["faces"].items()}
        out.append(e)
    return out


def whole(parts):
    """The bike in one model, for the item: every part in its place, the cranks level."""
    frame_m, wheel_m, crank_m, pedal_m, steer_m = parts
    els = moved(frame_m.elements, "#f", 0, 0, 0) + moved(steer_m.elements, "#s", 0, 0, 0)
    for axle in (REAR, FRONT):
        els += moved(wheel_m.elements, "#w", 0, axle[0] - 8, axle[1])
    els += moved(crank_m.elements, "#c", 0, BB[0] - 8, BB[1])
    for side in (-1, 1):
        els += moved(pedal_m.elements, "#p", side * 2.45, BB[0] - 8 - side * CRANK_R, BB[1])
    tex = {k: f"eraphones:block/{n}" for k, n in (("f", "velo_cadre"), ("s", "velo_guidon"), ("w", "velo_roue"), ("c", "velo_pedalier"),
                                                   ("p", "velo_pedale"))}
    tex["particle"] = "eraphones:block/velo_cadre"
    kit.write_json("models/item/velo.json", {
        "textures": tex, "elements": els,
        "display": {
            "gui": {"rotation": [8, 90, 0], "translation": [0, -2.2, 0], "scale": [.52, .52, .52]},
            "ground": {"rotation": [0, 90, 0], "translation": [0, 1, 0], "scale": [.28, .28, .28]},
            "fixed": {"rotation": [0, 90, 0], "translation": [0, -2, 0], "scale": [.5, .5, .5]},
            "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, -1, 2], "scale": [.32, .32, .32]},
            "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, -1, 2], "scale": [.32, .32, .32]},
            "firstperson_righthand": {"rotation": [0, 100, 0], "translation": [1, 3, -2], "scale": [.22, .22, .22]},
            "firstperson_lefthand": {"rotation": [0, 80, 0], "translation": [1, 3, -2], "scale": [.22, .22, .22]}}})


def bell(rate=48000):
    """The bell: a bright ting-ting, its ring hanging on."""
    n = int(rate * 1.3)
    t = np.arange(n) / rate
    y = np.zeros(n)
    for at in (0.0, 0.16):
        i = int(at * rate)
        tt = np.arange(n - i) / rate
        tone = (np.sin(2 * np.pi * 2480 * tt) + .55 * np.sin(2 * np.pi * 5950 * tt) + .3 * np.sin(2 * np.pi * 3720 * tt)) * np.exp(-tt * 5.5)
        click = np.random.default_rng(4).normal(0, 1, n - i) * np.exp(-tt / .002) * .4
        y[i:] += tone * .5 + click
    return y / np.abs(y).max() * .8


def recipes():
    ga.shaped("velo", ["R S", "IRI", "W W"], {"R": "minecraft:red_dye", "S": "minecraft:leather", "I": "minecraft:iron_ingot",
                                             "W": "minecraft:iron_bars"})


def main():
    parts = (frame(), wheel(), crank(), pedal(), steering())
    for m in parts:
        write_part(m)
    whole(parts)
    recipes()


if __name__ == "__main__":
    main()
