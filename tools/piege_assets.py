#!/usr/bin/env python3
"""Traps' art: twelve trap blocks in the game's own blocky style, medieval (iron spikes rising from a plate, a bear trap
of blackened wrought iron with rusty teeth and leaf springs, rusty caltrops, a nail board, an old saw blade turning in
an oak frame, a fire vent under a forged grate in mossy stone, a gas vent in cracked stone, a rope snare on its stake,
rusty spikes on an iron-bound oak board, a tar pit with planks sinking in it, a powder keg under a loose board, a
gargoyle's face carved in mossy stone bricks that spits darts),
their textures (16 x 16, pixel art shaded in bands), block and item models, blockstates, loot tables (each trap keeps
the effect set on it with /piege), recipes and the traps' damage type.

Called by gen_assets.py (run that one, or this file alone). Original art.
"""
import json
import math
import os
import random

import numpy as np
from PIL import Image

import gen_assets as g

DATA = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "data", "arcanes_oublies")
NS = "arcanes_oublies"

#: The traps, in their order: (id, the face their model is drawn for)
TRAPS = ["floor_spikes", "bear_trap", "caltrops", "nail_board", "saw_trap", "fire_vent", "gas_vent", "snare", "wall_spikes", "tar_pit",
         "pressure_mine", "dart_trap"]


def write_data(rel, data):
    path = os.path.join(DATA, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
        f.write("\n")
    print("wrote", os.path.relpath(path))


# ---------------------------------------------------------------- textures

def canvas(seed):
    return np.zeros((16, 16, 4), np.float32), random.Random(seed)


def to_img(a):
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def put(a, x, y, c, alpha=255):
    if 0 <= x < 16 and 0 <= y < 16:
        a[y, x, :3] = c
        a[y, x, 3] = alpha


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)


def iron(seed=1, rust=0.12, base=(78, 80, 88)):
    """Wrought iron: hammered, darker at its lower and right edges, a few rust patches."""
    a, rnd = canvas(seed)
    for y in range(16):
        for x in range(16):
            n = rnd.uniform(-1, 1)
            k = 1 + .07 * n + (.05 if (y * 7 + x * 3 + seed) % 11 == 0 else 0)
            c = shade(base, k)
            if x == 0 or y == 0:
                c = shade(base, 1.28)
            if x == 15 or y == 15:
                c = shade(base, .7)
            put(a, x, y, c)
    # Hammer marks: short lighter strokes
    for _ in range(5):
        x, y = rnd.randrange(2, 13), rnd.randrange(2, 14)
        for i in range(rnd.randrange(2, 4)):
            put(a, x + i, y, shade(base, 1.18))
    # Rust
    for _ in range(int(rust * 40)):
        x, y = rnd.randrange(1, 15), rnd.randrange(1, 15)
        put(a, x, y, rnd.choice([(122, 72, 44), (142, 84, 48), (104, 60, 40)]))
    return a


def rivets(a, spots, base=(78, 80, 88)):
    for x, y in spots:
        put(a, x, y, shade(base, 1.6))
        put(a, x + 1, y + 1, shade(base, .55))
        put(a, x + 1, y, shade(base, 1.1))
        put(a, x, y + 1, shade(base, .9))


def trap_iron():
    a = iron(11)
    rivets(a, [(2, 2), (12, 2), (2, 12), (12, 12)])
    return to_img(a)


def trap_iron_plate():
    """The spikes' plate: nine square holes they rise through, dark, lit on their lower lip."""
    a = iron(12, rust=.18)
    for hx in (2, 7, 12):
        for hy in (2, 7, 12):
            for dx in range(2):
                for dy in range(2):
                    put(a, hx + dx, hy + dy, (18, 18, 22))
            for d in range(-1, 3):
                put(a, hx + d, hy - 1, (52, 54, 60))
                put(a, hx + d, hy + 2, (130, 132, 140))
    return to_img(a)


def trap_steel():
    """Bright steel for spikes, teeth and nails: light at the point (the top), darker toward the root, a bright edge."""
    a, rnd = canvas(13)
    for y in range(16):
        t = y / 15
        c = tuple(int(lo + (hi - lo) * (1 - t)) for lo, hi in zip((86, 88, 98), (214, 218, 226)))
        for x in range(16):
            k = 1.18 if x % 4 == 1 else (.82 if x % 4 == 3 else 1)
            put(a, x, y, shade(c, k * (1 + rnd.uniform(-.04, .04))))
    # Dark stains near the root
    for _ in range(10):
        put(a, rnd.randrange(16), rnd.randrange(11, 16), rnd.choice([(92, 34, 30), (70, 40, 34), (110, 66, 40)]))
    return to_img(a)


def trap_wood():
    """Dark weathered planks, laid along x, nail heads at their ends."""
    a, rnd = canvas(14)
    cols = [(74, 50, 32), (92, 63, 40), (110, 76, 48), (128, 90, 58)]
    for y in range(16):
        board = y // 4
        for x in range(16):
            g_ = (math.sin(x * .7 + board * 2.1 + y * .3) + 1) / 2
            c = cols[1 + int(g_ * 2.2)] if (y % 4) else cols[0]
            if (x + board * 5) % 16 == 0 and y % 4:
                c = cols[0]
            put(a, x, y, shade(c, 1 + rnd.uniform(-.05, .05)))
    for y in (2, 6, 10, 14):
        put(a, 1, y, (150, 152, 158))
        put(a, 14, y, (150, 152, 158))
    return to_img(a)


def trap_rope():
    """Twisted hemp: light and dark strands crossing on the diagonal."""
    a, rnd = canvas(15)
    for y in range(16):
        for x in range(16):
            s = (x + y) % 4
            c = [(176, 146, 98), (150, 120, 76), (122, 96, 60), (164, 134, 88)][s]
            put(a, x, y, shade(c, 1 + rnd.uniform(-.06, .06)))
    return to_img(a)


def trap_stone(seed=16, moss=0.0):
    """Rough stone with cracks; {moss}: patches of moss."""
    a, rnd = canvas(seed)
    for y in range(16):
        for x in range(16):
            v = 112 + rnd.randint(-14, 14)
            if (x // 4 + y // 5) % 2 == 0:
                v += 8
            put(a, x, y, (v, v, v + 2))
    for _ in range(3):
        x, y = rnd.randrange(16), rnd.randrange(16)
        for _ in range(rnd.randrange(4, 8)):
            put(a, x, y, (64, 64, 66))
            x += rnd.choice([-1, 0, 1])
            y += 1
    for _ in range(int(moss * 60)):
        x, y = rnd.randrange(16), rnd.randrange(16)
        put(a, x, y, rnd.choice([(70, 104, 52), (86, 120, 60), (58, 88, 44)]))
    return a


def trap_stone_img():
    return to_img(trap_stone())


def trap_grate(hot=False):
    """An iron grate: a frame, bars across; between them darkness, or a fire's glow."""
    a = iron(17, rust=.25, base=FORGED)
    rnd = random.Random(18)
    for y in range(2, 14):
        if y % 3 == 1:
            continue
        for x in range(2, 14):
            if hot:
                t = rnd.random()
                c = (255, 226, 120) if t > .8 else (250, 150, 40) if t > .4 else (200, 70, 20)
            else:
                c = (20, 18, 18) if rnd.random() > .2 else (60, 30, 20)
            put(a, x, y, c)
    return to_img(a)


def trap_gas(active=False):
    """Cracked stone round a vent; the cracks and the vent's lip stained a sickly green (bright while it breathes)."""
    a = trap_stone(19)
    rnd = random.Random(20)
    green = [(150, 220, 70), (190, 255, 110)] if active else [(96, 140, 52), (120, 168, 62)]
    for ang in range(0, 360, 60):
        r = math.radians(ang + rnd.uniform(-15, 15))
        for s in range(3, 8):
            x, y = int(round(8 + math.cos(r) * s)), int(round(8 + math.sin(r) * s))
            put(a, x, y, rnd.choice(green))
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 2.2:
                put(a, x, y, (24, 30, 20) if not active else (80, 120, 40))
            elif d < 3.1:
                put(a, x, y, green[1])
    return to_img(a)


def trap_tar():
    """Black tar, glossy, with bubbles."""
    a, rnd = canvas(21)
    for y in range(16):
        for x in range(16):
            v = 22 + rnd.randint(0, 10)
            put(a, x, y, (v + 4, v, v - 2))
    for _ in range(6):
        x, y = rnd.randrange(1, 14), rnd.randrange(1, 14)
        put(a, x, y, (80, 76, 70))
        put(a, x + 1, y, (52, 48, 44))
        put(a, x, y + 1, (40, 36, 32))
    for _ in range(8):
        put(a, rnd.randrange(16), rnd.randrange(16), (110, 104, 96))
    return to_img(a)



def trap_dart():
    """Mossy stone bricks."""
    a, rnd = canvas(23)
    for y in range(16):
        for x in range(16):
            row = y // 4
            off = 4 if row % 2 else 0
            mortar = y % 4 == 3 or (x + off) % 8 == 7
            v = 70 if mortar else 116 + rnd.randint(-10, 10)
            put(a, x, y, (v, v, v + 2))
    for _ in range(26):
        x, y = rnd.randrange(16), rnd.randrange(16)
        if y % 4 == 3 or rnd.random() < .5:
            put(a, x, y, rnd.choice([(70, 104, 52), (86, 120, 60), (58, 88, 44)]))
    return to_img(a)




# ---------------------------------------------------------------- medieval materials

FORGED = (48, 46, 48)


def trap_forged(seed=40):
    """Blackened wrought iron, hammered by hand: dull facets, heavy rust in patches, a couple of nail heads."""
    a, rnd = canvas(seed)
    for y in range(16):
        for x in range(16):
            facet = ((x // 3) * 7 + (y // 3) * 13 + seed) % 5
            k = .86 + .07 * facet + rnd.uniform(-.06, .06)
            c = shade(FORGED, k)
            if x == 15 or y == 15:
                c = shade(FORGED, .7)
            put(a, x, y, c)
    for _ in range(4):
        cx, cy = rnd.randrange(16), rnd.randrange(16)
        for _ in range(rnd.randrange(5, 11)):
            put(a, cx, cy, rnd.choice([(112, 58, 32), (92, 48, 30), (130, 70, 38), (76, 42, 30)]))
            cx += rnd.choice([-1, 0, 1])
            cy += rnd.choice([-1, 0, 1])
    for x, y in ((3, 4), (12, 11)):
        put(a, x, y, (96, 92, 92))
        put(a, x + 1, y + 1, (28, 26, 28))
    return to_img(a)


def trap_rust():
    """Old iron for teeth, spikes and points: worn grey at the point (the top), rust thick toward the root, pitted."""
    a, rnd = canvas(41)
    for y in range(16):
        t = y / 15
        c = tuple(int(lo + (hi - lo) * (1 - t)) for lo, hi in zip((104, 56, 32), (150, 140, 128)))
        for x in range(16):
            k = 1.12 if x % 4 == 1 else (.84 if x % 4 == 3 else 1)
            put(a, x, y, shade(c, k * (1 + rnd.uniform(-.06, .06))))
    for _ in range(18):
        put(a, rnd.randrange(16), rnd.randrange(4, 16), rnd.choice([(70, 36, 24), (128, 66, 34), (60, 40, 32)]))
    return to_img(a)


OAK = [(66, 44, 28), (88, 60, 36), (108, 74, 46), (126, 88, 56)]


def oak(seed=42, strap=False):
    """Rough-hewn oak boards, standing up: grain, knots, axe marks; {strap}: a band of wrought iron across, nailed."""
    a, rnd = canvas(seed)
    for x in range(16):
        board = x // 5
        for y in range(16):
            g_ = (math.sin(y * .55 + board * 2.7 + x * .4) + 1) / 2
            c = OAK[0] if x % 5 == 4 else OAK[1 + int(g_ * 2.4)]
            put(a, x, y, shade(c, 1 + rnd.uniform(-.06, .06)))
    for _ in range(2):
        kx, ky = rnd.randrange(1, 14), rnd.randrange(2, 14)
        put(a, kx, ky, OAK[0])
        put(a, kx + 1, ky, (54, 36, 22))
        put(a, kx, ky + 1, OAK[1])
    if strap:
        for x in range(16):
            for y in (6, 7, 8, 9):
                put(a, x, y, shade(FORGED, 1.15 if y == 6 else .8 if y == 9 else 1) if (x * 5 + y) % 7 else (110, 58, 32))
        for x in (2, 7, 12):
            put(a, x, 7, (104, 100, 100))
            put(a, x, 8, (30, 28, 30))
    return a


def trap_oak():
    return to_img(oak())


def trap_oak_strapped():
    return to_img(oak(43, True))


def trap_saw_frame():
    """The saw's frame seen from above: oak boards, a dark slot north to south the blade turns in, iron at its ends."""
    a = oak(44)
    for y in range(1, 15):
        put(a, 7, y, (14, 12, 10))
        put(a, 8, y, (14, 12, 10))
        put(a, 6, y, (54, 36, 22))
        put(a, 9, y, (140, 100, 64))
    for x in range(16):
        for y in (0, 1, 14, 15):
            put(a, x, y, shade(FORGED, 1.1 if y in (0, 14) else .85))
    return to_img(a)


def trap_rust_blade():
    """A big old saw blade: rusted through, its teeth dark and chipped, a square hub."""
    a = np.zeros((32, 32, 4), np.float32)
    rnd = random.Random(45)
    for y in range(32):
        for x in range(32):
            dx, dy = x - 15.5, y - 15.5
            d = math.hypot(dx, dy)
            ang = math.atan2(dy, dx)
            tooth = (ang / (2 * math.pi) * 12) % 1
            edge = 13.0 + 2.6 * (1 - tooth)
            if d > edge:
                continue
            t = d / 15
            base = (132 - 30 * t, 96 - 40 * t, 70 - 34 * t)
            c = tuple(v * (1 + rnd.uniform(-.1, .1)) for v in base)
            if d > 12.4:
                c = (96, 84, 76) if rnd.random() > .2 else (60, 40, 30)
            if max(abs(dx), abs(dy)) < 3:
                c = (40, 38, 40) if max(abs(dx), abs(dy)) > 1.5 else (90, 86, 86)
            a[y, x, :3] = c
            a[y, x, 3] = 255
    return to_img(a)


def trap_keg(top=False, lit=False):
    """A keg of black powder: oak staves bound by two iron hoops; {top}: its lid with the fuse's hole (glowing once lit)."""
    a, rnd = canvas(46 + top)
    if not top:
        for x in range(16):
            for y in range(16):
                c = OAK[0] if x % 4 == 3 else OAK[1 + (x // 4 + y // 6) % 3]
                put(a, x, y, shade(c, 1 + rnd.uniform(-.05, .05)))
            for y in (2, 3, 12, 13):
                put(a, x, y, shade(FORGED, 1.2 if y in (2, 12) else .9))
        return to_img(a)
    for y in range(16):
        for x in range(16):
            c = OAK[0] if y % 5 == 4 else OAK[2 + (y // 5) % 2]
            put(a, x, y, shade(c, 1 + rnd.uniform(-.05, .05)))
    # A crude black cross daubed on it: powder, keep off
    for i in range(3, 13):
        put(a, i, i, (24, 20, 18))
        put(a, 15 - i, i, (24, 20, 18))
    put(a, 11, 4, (255, 170, 60) if lit else (30, 26, 24))
    put(a, 12, 4, (255, 230, 120) if lit else (30, 26, 24))
    return to_img(a)


def trap_dart_face():
    """A gargoyle's face carved in mossy stone bricks: a heavy brow, hollow eyes, a nose, an open mouth the darts fly from."""
    a, rnd = canvas(47)
    for y in range(16):
        for x in range(16):
            v = 112 + rnd.randint(-10, 10)
            put(a, x, y, (v, v, v + 2))
    rows = [
        "................",
        ".bbbbbbbbbbbbbb.",
        ".bhbbhhhhhhbbhb.",
        ".bhhhbbhhbbhhhb.",
        ".bhheeehheeehhb.",
        ".bhhekehhekehhb.",
        ".bhhhhhnnhhhhhb.",
        ".bhhhhnnnnhhhhb.",
        ".bhhhhnddnhhhhb.",
        ".bhhhhhhhhhhhhb.",
        ".bhhmtmmmmtmhhb.",
        ".bhhmmmmmmmmhhb.",
        ".bhhmmtmmtmmhhb.",
        ".bhhhhhhhhhhhhb.",
        ".bbbbbbbbbbbbbb.",
        "................",
    ]
    pal = {"b": (66, 66, 68), "h": (124, 124, 126), "e": (36, 36, 38), "k": (8, 6, 6), "n": (148, 148, 150), "d": (40, 40, 42),
           "m": (12, 10, 10), "t": (184, 178, 160)}
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                c = pal[ch]
                if ch == "h":
                    c = shade(c, 1 + rnd.uniform(-.07, .07) + (.08 if y < 3 else 0))
                put(a, x, y, c)
    for _ in range(22):
        x, y = rnd.randrange(16), rnd.randrange(16)
        if rows[y][x] in ".b":
            put(a, x, y, rnd.choice([(70, 104, 52), (86, 120, 60), (58, 88, 44)]))
    return to_img(a)


def trap_stone_moss():
    return to_img(trap_stone(48, moss=.35))


# ---------------------------------------------------------------- models

def faces(tex, only=None, up=None, down=None):
    out = {}
    for f in ("north", "south", "east", "west", "up", "down"):
        if only and f not in only:
            continue
        t = up if f == "up" and up else down if f == "down" and down else tex
        out[f] = {"texture": "#" + t}
    return out


def box(frm, to, tex, only=None, up=None, down=None, rot=None):
    e = {"from": list(frm), "to": list(to), "faces": faces(tex, only, up, down)}
    if rot:
        e["rotation"] = rot
    return e


def model(textures, elements, parent="block/block"):
    t = {k: f"{NS}:block/{v}" for k, v in textures.items()}
    t.setdefault("particle", t[next(iter(textures))])
    return {"parent": parent, "render_type": "minecraft:cutout", "textures": t, "elements": elements}


SPIKES = [(x, z) for x in (2, 7, 12) for z in (2, 7, 12)]


def floor_spikes(up):
    els = [box((0, 0, 0), (16, 2, 16), "iron", up="plate")]
    for i, (x, z) in enumerate(SPIKES):
        if up:
            h = 10 + (i % 2) * 2
            els.append(box((x, 2, z), (x + 2, h, z + 2), "steel", only=("north", "south", "east", "west", "up")))
            els.append(box((x + .5, h, z + .5), (x + 1.5, h + 3, z + 1.5), "steel"))
        else:
            els.append(box((x + .5, 1.5, z + .5), (x + 1.5, 2.6, z + 1.5), "steel"))
    return model({"plate": "trap_iron_plate", "iron": "trap_iron", "steel": "trap_steel"}, els)


def bear_trap(closed):
    els = [box((5, 0, 5), (11, 1, 11), "iron"), box((6, 1, 6), (10, 1.5, 10), "steel"),
           # The hinge bar the jaws turn on, and the springs at its ends
           box((0.5, 0, 7.5), (15.5, 1.5, 8.5), "iron"), box((0, 0, 6.5), (2, 2, 9.5), "iron"), box((14, 0, 6.5), (16, 2, 9.5), "iron"),
           # The chain and its stake
           box((7.5, 0, 11), (8.5, 1, 13), "iron"), box((7, 0, 13), (9, 1, 14), "iron"), box((7.5, 0, 14), (8.5, 3, 15), "wood")]
    # Its leaf springs, out past the hinge at both ends: two bars and the loop joining them
    for x0, x1, xe in ((-4, 0, -5), (16, 20, 20)):
        els += [box((x0, 0, 6.5), (x1, 1, 7.5), "iron"), box((x0, .8, 8.5), (x1, 1.8, 9.5), "iron"),
                box((xe, 0, 6.5), (xe + 1, 1.8, 9.5), "iron")]
    if not closed:
        for side in (-1, 1):
            z0 = 1 if side < 0 else 14
            zr = (2, 7) if side < 0 else (9, 14)
            els.append(box((2, 0, z0), (14, 1, z0 + 1), "iron"))
            els.append(box((1, 0, zr[0]), (2, 1, zr[1]), "iron"))
            els.append(box((14, 0, zr[0]), (15, 1, zr[1]), "iron"))
            zt = 2 if side < 0 else 13
            for x in (3, 5, 7, 9, 11, 13):
                els.append(box((x - .5, 1, zt), (x + .5, 3, zt + 1), "steel"))
    else:
        els.append(box((2, 1, 7), (14, 7, 8), "iron"))
        els.append(box((2, 1, 8), (14, 6.5, 9), "iron"))
        els.append(box((1, 0, 7), (2, 7, 9), "iron"))
        els.append(box((14, 0, 7), (15, 7, 9), "iron"))
        for x in (3, 5, 7, 9, 11, 13):
            els.append(box((x - .5, 6.5, 8), (x + .5, 8, 9), "steel"))
            els.append(box((x + .5, 7, 7), (x + 1.5, 8.5, 8), "steel"))
    return model({"iron": "trap_forged", "steel": "trap_rust", "wood": "trap_oak"}, els)


CALTROPS = [(3, 3), (9, 2), (13, 6), (5, 9), (11, 12), (2, 13), (8, 7)]


def caltrops():
    els = []
    for i, (x, z) in enumerate(CALTROPS):
        ang = [-45, -22.5, 22.5, 45][i % 4]
        els.append(box((x - 1.5, 0, z), (x + 1.5, 1, z + 1), "steel", rot={"origin": [x + .5, 0, z + .5], "axis": "y", "angle": ang}))
        els.append(box((x, 0, z - 1), (x + 1, 1, z + 2), "steel", rot={"origin": [x + .5, 0, z + .5], "axis": "y", "angle": ang}))
        els.append(box((x, 0, z), (x + 1, 3, z + 1), "steel"))
    return model({"steel": "trap_rust"}, els)


def nail_board():
    els = [box((1, 0, 1), (15, 1.5, 15), "wood"), box((1, 0, 3), (15, 2, 4), "wood"), box((1, 0, 12), (15, 2, 13), "wood")]
    rnd = random.Random(31)
    for x in (3, 6, 9, 12):
        for z in (2.5, 5.5, 8.5, 11.5):
            dx, dz = rnd.uniform(-.6, .6), rnd.uniform(-.6, .6)
            h = rnd.choice([4, 4.5, 5])
            els.append(box((x + dx, 1.5, z + dz), (x + dx + 1, h, z + dz + 1), "steel"))
    return model({"wood": "trap_wood", "steel": "trap_steel"}, els)


def saw_trap(with_blade):
    els = [box((0, 0, 0), (16, 3, 16), "oak", up="slot"), box((0, 3, 0), (2, 4.5, 16), "iron"), box((14, 3, 0), (16, 4.5, 16), "iron"),
           box((6, 3, 0), (10, 4, 1.5), "iron"), box((6, 3, 14.5), (10, 4, 16), "iron")]
    if with_blade:
        # The blade, for the item (in the world it is drawn turning)
        els.append({"from": [8, -2, 2], "to": [8, 10, 14], "faces": {"east": {"texture": "#blade", "uv": [0, 0, 16, 16]},
                                                                     "west": {"texture": "#blade", "uv": [0, 0, 16, 16]}}})
    return model({"oak": "trap_oak_strapped", "iron": "trap_forged", "slot": "trap_saw_frame", "blade": "trap_rust_blade"}, els)


def fire_vent(lit):
    grate = "trap_grate_hot" if lit else "trap_grate"
    els = [box((0, 0, 0), (16, 3, 16), "stone", up="grate")]
    for x, z in ((0, 0), (14, 0), (0, 14), (14, 14)):
        els.append(box((x, 3, z), (x + 2, 4, z + 2), "stone"))
    return model({"stone": "trap_stone_moss", "grate": grate}, els)


def gas_vent(active):
    gas = "trap_gas_active" if active else "trap_gas"
    els = [box((0, 0, 0), (16, 2, 16), "stone", up="gas"), box((4, 2, 4), (12, 3, 5), "stone"), box((4, 2, 11), (12, 3, 12), "stone"),
           box((4, 2, 5), (5, 3, 11), "stone"), box((11, 2, 5), (12, 3, 11), "stone")]
    return model({"stone": "trap_stone_moss", "gas": gas}, els)


def snare(sprung):
    els = [box((1, 0, 7), (3, 8, 9), "wood"), box((1.5, 8, 7.5), (2.5, 9, 8.5), "wood")]
    if not sprung:
        els += [box((3, 6, 7.5), (6, 7, 8.5), "rope"), box((6, 0.5, 7.5), (7, 6, 8.5), "rope"),
                box((6, 0, 4), (14, 1, 5), "rope"), box((6, 0, 11), (14, 1, 12), "rope"), box((6, 0, 5), (7, 1, 11), "rope"),
                box((13, 0, 5), (14, 1, 11), "rope"), box((9, 0, 7), (11, 1, 9), "wood")]
    else:
        els += [box((3, 6, 7.5), (8, 7, 8.5), "rope"), box((8, 1, 7.5), (9, 7, 8.5), "rope"), box((8, 0, 6), (12, 1, 7), "rope"),
                box((8, 0, 9), (12, 1, 10), "rope"), box((11, 0, 7), (12, 1, 9), "rope"), box((12, 0, 6), (14, .5, 10), "wood")]
    return model({"wood": "trap_oak", "rope": "trap_rope"}, els)


def wall_spikes():
    els = [box((0, 0, 13.5), (16, 16, 16), "board")]
    for x in (2, 7, 12):
        for y in (2, 7, 12):
            els.append(box((x, y, 8), (x + 2, y + 2, 14), "steel"))
            els.append(box((x + .5, y + .5, 4), (x + 1.5, y + 1.5, 8), "steel"))
    return model({"board": "trap_oak_strapped", "steel": "trap_rust"}, els)


def tar_pit():
    els = [box((0, 0, 0), (16, 1, 16), "tar"), box((3, 1, 4), (5, 1.5, 6), "tar"), box((10, 1, 9), (12, 2, 11), "tar"),
           box((6, 1, 12), (7, 1.5, 13), "tar"), box((12, 1, 3), (13, 1.5, 4), "tar"),
           # Old planks sinking in it at its edges
           box((0, 0, 0), (2.5, 1.8, 9), "oak", rot={"origin": [1, 0, 4], "axis": "x", "angle": -22.5}),
           box((10, 0, 13.5), (16, 1.6, 16), "oak", rot={"origin": [13, 0, 15], "axis": "z", "angle": 22.5})]
    return model({"tar": "trap_tar", "oak": "trap_oak"}, els)


def pressure_mine(clicked):
    """A keg of black powder hidden under a loose board; stepped on, the board sinks and the fuse is lit."""
    lid = "trap_keg_top_lit" if clicked else "trap_keg_top"
    board = 5.4 if clicked else 6.2
    els = [box((3.5, 0, 3.5), (12.5, 6, 12.5), "keg", up="lid"),
           box((1, board, 5), (15, board + 1, 11), "oak", rot={"origin": [8, board, 8], "axis": "y", "angle": 22.5}),
           box((11, 6, 7.5), (14, 7, 8.5), "rope"), box((13, 5.5, 7.5), (14, 6.5, 8.5), "rope")]
    return model({"keg": "trap_keg", "lid": lid, "oak": "trap_oak", "rope": "trap_rope"}, els)


def dart_trap():
    return {"parent": "block/orientable", "textures": {"top": f"{NS}:block/trap_dart_side", "front": f"{NS}:block/trap_dart_face",
                                                       "side": f"{NS}:block/trap_dart_side", "particle": f"{NS}:block/trap_dart_side"}}


def blockstate(trap, models_for, wall=False):
    variants = {}
    for active in (False, True):
        for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            v = {"model": f"{NS}:block/{models_for(active)}"}
            if y:
                v["y"] = y
            variants[f"active={str(active).lower()},facing={f}"] = v
    return {"variants": variants}


def main():
    for name, img in (("trap_iron", trap_iron()), ("trap_iron_plate", trap_iron_plate()), ("trap_steel", trap_steel()),
                      ("trap_wood", trap_wood()), ("trap_rope", trap_rope()), ("trap_stone", trap_stone_img()),
                      ("trap_grate", trap_grate()), ("trap_grate_hot", trap_grate(True)), ("trap_gas", trap_gas()),
                      ("trap_gas_active", trap_gas(True)), ("trap_tar", trap_tar()), ("trap_dart_side", trap_dart()), ("trap_forged", trap_forged()),
                      ("trap_rust", trap_rust()), ("trap_oak", trap_oak()), ("trap_oak_strapped", trap_oak_strapped()),
                      ("trap_saw_frame", trap_saw_frame()), ("trap_rust_blade", trap_rust_blade()), ("trap_keg", trap_keg()),
                      ("trap_keg_top", trap_keg(top=True)), ("trap_keg_top_lit", trap_keg(top=True, lit=True)),
                      ("trap_dart_face", trap_dart_face()), ("trap_stone_moss", trap_stone_moss())):
        g.save(img, f"textures/block/{name}.png")
    two = {
        "floor_spikes": (floor_spikes(False), floor_spikes(True)), "bear_trap": (bear_trap(False), bear_trap(True)),
        "fire_vent": (fire_vent(False), fire_vent(True)), "gas_vent": (gas_vent(False), gas_vent(True)),
        "snare": (snare(False), snare(True)), "pressure_mine": (pressure_mine(False), pressure_mine(True)),
    }
    one = {"caltrops": caltrops(), "nail_board": nail_board(), "saw_trap": saw_trap(False), "wall_spikes": wall_spikes(), "tar_pit": tar_pit(),
           "dart_trap": dart_trap()}
    for trap, (idle, active) in two.items():
        g.write_json(f"models/block/{trap}.json", idle)
        g.write_json(f"models/block/{trap}_active.json", active)
        g.write_json(f"blockstates/{trap}.json", blockstate(trap, lambda a, t=trap: t + ("_active" if a else "")))
    for trap, m in one.items():
        g.write_json(f"models/block/{trap}.json", m)
        g.write_json(f"blockstates/{trap}.json", blockstate(trap, lambda a, t=trap: t))
    g.write_json("models/block/saw_trap_item.json", saw_trap(True))
    # The items look like the trap at work where that shows it best
    shown = {"floor_spikes": "floor_spikes_active", "saw_trap": "saw_trap_item"}
    for trap in TRAPS:
        g.write_json(f"models/item/{trap}.json", {"parent": f"{NS}:block/{shown.get(trap, trap)}"})
        write_data(f"loot_table/blocks/{trap}.json", {"type": "minecraft:block", "pools": [{
            "rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{NS}:{trap}", "functions": [
                {"function": "minecraft:copy_components", "source": "block_entity", "include": [f"{NS}:piege"]}]}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    write_data("damage_type/trap.json", {"exhaustion": 0.1, "message_id": f"{NS}.trap", "scaling": "when_caused_by_living_non_player"})
    I, N, S = {"item": "minecraft:iron_ingot"}, {"item": "minecraft:iron_nugget"}, {"item": "minecraft:string"}
    recipes = {
        "floor_spikes": (["NNN", "III"], {"N": N, "I": I}, 1),
        "bear_trap": (["I I", "NPN", "ICI"], {"I": I, "N": N, "P": {"item": "minecraft:heavy_weighted_pressure_plate"}, "C": {"item": "minecraft:chain"}}, 1),
        "caltrops": (["N N", " N ", "N N"], {"N": N}, 2),
        "nail_board": (["NNN", "PPP"], {"N": N, "P": {"tag": "minecraft:planks"}}, 1),
        "saw_trap": ([" I ", "IRI", "CCC"], {"I": I, "R": {"item": "minecraft:redstone"}, "C": {"item": "minecraft:cobblestone"}}, 1),
        "fire_vent": (["BBB", "CFC"], {"B": {"item": "minecraft:iron_bars"}, "C": {"item": "minecraft:cobblestone"}, "F": {"item": "minecraft:flint_and_steel"}}, 1),
        "gas_vent": (["CSC", "CPC"], {"C": {"item": "minecraft:cobblestone"}, "S": {"item": "minecraft:spider_eye"},
                                      "P": {"item": "minecraft:stone_pressure_plate"}}, 1),
        "snare": ([" SS", "S S", "TS "], {"S": S, "T": {"item": "minecraft:stick"}}, 1),
        "wall_spikes": (["IN", "IN", "IN"], {"I": I, "N": N}, 2),
        "tar_pit": (["CSC"], {"C": {"item": "minecraft:coal"}, "S": {"item": "minecraft:slime_ball"}}, 2),
        "pressure_mine": (["NPN", "NTN"], {"N": N, "P": {"item": "minecraft:stone_pressure_plate"}, "T": {"item": "minecraft:tnt"}}, 1),
        "dart_trap": (["CCC", "CBC", "CRC"], {"C": {"item": "minecraft:mossy_cobblestone"}, "B": {"item": "minecraft:bow"},
                                              "R": {"item": "minecraft:redstone"}}, 1),
    }
    for trap, (pattern, key, count) in recipes.items():
        write_data(f"recipe/{trap}.json", {"type": "minecraft:crafting_shaped", "category": "redstone", "pattern": pattern, "key": key,
                                           "result": {"id": f"{NS}:{trap}", "count": count}})


if __name__ == "__main__":
    main()
