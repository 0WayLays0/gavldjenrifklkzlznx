#!/usr/bin/env python3
"""Token Magic art: the Soul Token (its model and textures, from tools/models/soul_token.bbmodel), the spell icons,
the Soul Ledger and its affinity ring.

Called by gen_assets.py (run that one). The Soul Token is the server team's own Blockbench model ("Jeton d'Âme"): this
script turns it into a Minecraft 1.21.1 item model (whose elements may only turn by up to 45 degrees, so the two rim
bars the model turns by 90 degrees are laid out flat) and shifts its colours from teal to cyan blue. The rest is drawn
here.
"""
import base64
import colorsys
import io
import json
import math
import os

from PIL import Image, ImageDraw

import gen_assets as g
from explosion_assets import fbm

SOURCE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "models", "soul_token.bbmodel")

# Token palette: cyan blue
CYAN = (51, 214, 255)
CYAN_LIGHT = (176, 242, 255)
CYAN_DEEP = (20, 120, 200)
RIM = (16, 82, 120)
RIM_DARK = (10, 50, 80)
NIGHT = (6, 14, 22)
SILVER = (214, 232, 238)
SILVER_DARK = (120, 146, 160)

# Blockbench paints the thin edges of a face with these colours by default; on the token they become rim
PLACEHOLDERS = {(0x5b, 0xbc, 0xf4), (0x43, 0xe8, 0x8d), (0xf8, 0xdd, 0x72), (0xf4, 0x86, 0x86)}


# ---------------------------------------------------------------- the Soul Token, from the Blockbench model

def _cyan(rgb):
    """Teal to cyan blue: the hue turns about 20 degrees, a little more saturated and brighter."""
    if rgb in PLACEHOLDERS:
        return RIM
    h, s, v = colorsys.rgb_to_hsv(*(c / 255 for c in rgb))
    h = (h + 20 / 360) % 1
    s = min(1, s * 1.1)
    v = min(1, v * 1.08)
    return tuple(round(c * 255) for c in colorsys.hsv_to_rgb(h, s, v))


def _texture(source):
    img = Image.open(io.BytesIO(base64.b64decode(source.split(",", 1)[1]))).convert("RGBA")
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, gr, b, a = px[x, y]
            if a:
                px[x, y] = _cyan((r, gr, b)) + (a,)
    return img


def _turned(element):
    """An element turned 90 degrees about Y (as Blockbench does it), laid out as the same box unturned."""
    ox, _, oz = element["origin"]
    corners = [(element["from"][0], element["from"][2]), (element["to"][0], element["to"][2])]
    # A turn of +90 degrees about Y takes (x, z) to (z, -x) around the origin
    turned = [(ox + (z - oz), oz - (x - ox)) for x, z in corners]
    xs = sorted(p[0] for p in turned)
    zs = sorted(p[1] for p in turned)
    faces = element["faces"]
    # The box's sides move round: what faced east now faces north, and so on
    moved = {"north": faces["east"], "east": faces["south"], "south": faces["west"], "west": faces["north"],
             "up": dict(faces["up"], rotation=90), "down": dict(faces["down"], rotation=270)}
    return [xs[0], element["from"][1], zs[0]], [xs[1], element["to"][1], zs[1]], moved


def soul_token():
    with open(SOURCE, encoding="utf-8") as f:
        model = json.load(f)
    textures = {}
    for i, tex in enumerate(model["textures"]):
        g.save(_texture(tex["source"]), f"textures/item/soul_token_{i}.png")
        textures[str(i)] = f"arcanes_oublies:item/soul_token_{i}"
    textures["particle"] = "arcanes_oublies:item/soul_token_0"
    uv_scale = {str(i): 16 / tex.get("uv_width", 16) for i, tex in enumerate(model["textures"])}
    elements = []
    for e in model["elements"]:
        if e.get("rotation", [0, 0, 0])[1] == 90:
            frm, to, faces = _turned(e)
        else:
            assert not any(e.get("rotation", [0, 0, 0])), "only turns of 90 degrees about Y are handled"
            frm, to, faces = e["from"], e["to"], e["faces"]
        out = {}
        for side, face in faces.items():
            k = uv_scale[str(face["texture"])]
            out[side] = {"uv": [round(u * k, 4) for u in face["uv"]], "texture": "#" + str(face["texture"])}
            if face.get("rotation"):
                out[side]["rotation"] = face["rotation"]
        elements.append({"from": [round(v, 4) for v in frm], "to": [round(v, 4) for v in to], "faces": out})
    # Display slots 1.21.1 knows (Blockbench also writes newer ones)
    known = {"thirdperson_righthand", "thirdperson_lefthand", "firstperson_righthand", "firstperson_lefthand", "gui", "head", "ground", "fixed"}
    display = {k: v for k, v in model.get("display", {}).items() if k in known}
    g.write_json("models/item/soul_token.json", {"textures": textures, "elements": elements, "display": display})


# ---------------------------------------------------------------- icons

def token(img, x, y, size=3, face=CYAN, rim=RIM):
    """A token seen from the front: a square face in a dark rim, a glint in its corner."""
    d = ImageDraw.Draw(img)
    d.rectangle((x, y, x + size, y + size), fill=rim + (255,))
    if size >= 2:
        d.rectangle((x + 1, y + 1, x + size - 1, y + size - 1), fill=face + (255,))
        img.putpixel((x + 1, y + 1), CYAN_LIGHT + (255,))
    return img


def _base(cx=8, cy=8, inner=(20, 60, 86)):
    return g.spell_background(cx, cy, inner, NIGHT)


def _line(img, points, color):
    ImageDraw.Draw(img).line(points, fill=color + (255,))


def token_throw_icon():
    """A token flying off, a dotted trail behind it and the swirl of the caster coming after it."""
    img = _base(9, 7)
    for k, (x, y) in enumerate(((2, 13), (4, 11), (6, 9))):
        img.putpixel((x, y), (CYAN_DEEP if k < 2 else CYAN) + (255,))
    d = ImageDraw.Draw(img)
    d.arc((1, 8, 7, 14), 200, 340, fill=CYAN_LIGHT + (255,))
    token(img, 9, 3, 4)
    _line(img, [(8, 6), (7, 7)], CYAN_LIGHT)
    return img


def token_wave_icon():
    """A wave of tokens rising out of the ground."""
    img = _base(8, 10)
    for i, x in enumerate(range(1, 15, 3)):
        y = 11 - round(5 * math.sin(i / 4 * math.pi))
        token(img, x, y, 2)
    _line(img, [(0, 14), (15, 14)], RIM)
    return img


def token_storm_icon():
    """Tokens whirling in a spiral."""
    img = _base()
    for k in range(9):
        a = k * .9
        r = 1.5 + k * .62
        token(img, round(7 + math.cos(a) * r), round(7 + math.sin(a) * r), 2 if k < 4 else 3)
    return img


def token_ricochet_icon():
    """A token bouncing from one mark to the next in a zigzag."""
    img = _base(8, 8)
    pts = [(1, 13), (5, 4), (9, 12), (14, 3)]
    _line(img, pts, CYAN_DEEP)
    for x, y in pts[1:3]:
        img.putpixel((x, y), CYAN_LIGHT + (255,))
    token(img, 11, 1, 3)
    return img


def coin_flip_icon():
    """A token tossed up spinning, half cyan, half gold, over an open hand."""
    img = _base(8, 6, (40, 50, 70))
    d = ImageDraw.Draw(img)
    d.rectangle((6, 2, 9, 5), fill=RIM + (255,))
    d.rectangle((7, 3, 7, 4), fill=CYAN + (255,))
    d.rectangle((8, 3, 8, 4), fill=(255, 210, 90, 255))
    d.arc((3, 0, 12, 9), 200, 330, fill=CYAN_LIGHT + (255,))
    # The hand
    d.rectangle((5, 11, 10, 12), fill=(214, 170, 130, 255))
    d.line((4, 10, 4, 11), fill=(214, 170, 130, 255))
    d.line((5, 13, 10, 13), fill=(150, 110, 80, 255))
    return img


def token_aegis_icon():
    """A ring of tokens turning around a shield."""
    img = _base()
    d = ImageDraw.Draw(img)
    d.polygon([(5, 5), (10, 5), (10, 9), (7.5, 12), (5, 9)], fill=SILVER_DARK + (255,), outline=SILVER + (255,))
    for k in range(6):
        a = k * math.tau / 6
        token(img, round(6.5 + math.cos(a) * 5.5), round(7 + math.sin(a) * 5.5), 2)
    return img


def token_rain_icon():
    """Tokens raining down from a dark cloud."""
    img = _base(8, 10)
    d = ImageDraw.Draw(img)
    d.ellipse((1, 0, 9, 4), fill=(40, 60, 76, 255))
    d.ellipse((6, 0, 14, 5), fill=(52, 74, 92, 255))
    for x, y in ((2, 7), (6, 10), (10, 7), (13, 11), (4, 12)):
        token(img, x, y, 2)
        img.putpixel((x, y - 2), CYAN_DEEP + (255,))
    return img


def token_mines_icon():
    """Three tokens lying on the ground, sparks warning over them."""
    img = _base(8, 11)
    _line(img, [(0, 13), (15, 13)], (60, 50, 40))
    for x in (1, 6, 11):
        token(img, x, 10, 3)
    for x, y in ((3, 7), (8, 6), (13, 7)):
        img.putpixel((x, y), (255, 240, 140, 255))
        img.putpixel((x, y - 1), (255, 200, 80, 255))
    return img


def soul_tribute_icon():
    """Tokens drawn out of three foes into a glowing heart."""
    img = _base(8, 8, (30, 60, 80))
    d = ImageDraw.Draw(img)
    heart = [(7, 9), (4, 6), (4, 4), (5, 3), (6, 3), (7, 4), (8, 3), (9, 3), (10, 4), (10, 6)]
    d.polygon(heart, fill=CYAN + (255,), outline=CYAN_LIGHT + (255,))
    for x, y in ((0, 12), (6, 13), (12, 12)):
        token(img, x, y, 2)
    _line(img, [(2, 11), (5, 8)], CYAN_DEEP)
    _line(img, [(7, 12), (7, 10)], CYAN_DEEP)
    _line(img, [(12, 11), (9, 8)], CYAN_DEEP)
    return img


def token_hand_icon():
    """A great hand made of tokens, reaching down."""
    img = _base(8, 9, (24, 70, 100))
    cells = [
        "..x.x.x.........",
        ".xx.x.x.x.......",
        ".xx.x.x.x.......",
        ".xxxxxxxx.......",
        ".xxxxxxxxxx.....",
        ".xxxxxxxxx......",
        "..xxxxxxx.......",
        "...xxxxx........",
        "...xxxxx........",
    ]
    for y, row in enumerate(cells):
        for x, c in enumerate(row):
            if c == "x":
                col = CYAN if (x + y) % 2 else CYAN_DEEP
                img.putpixel((x + 2, y + 3), col + (255,))
    for x, y in ((3, 3), (6, 4), (9, 5)):
        img.putpixel((x, y), CYAN_LIGHT + (255,))
    return img


# ---------------------------------------------------------------- the Soul Ledger

def soul_ledger_icon():
    """A ledger bound in night-blue leather, a soul token set in the cover."""
    img = g.from_grid([
        "................",
        "...oooooooooo...",
        "..obbbbbbbbbsco.",
        "..oboooooooocco.",
        "..obo......oLco.",
        "..obo......oLco.",
        "..obo......oLcco",
        "..obo......oLcco",
        "..obo......oLcco",
        "..obo......oLcco",
        "..oboooooooosco.",
        "..obsssssssssco.",
        "..osssssssssxco.",
        "...ooooooooooo..",
        "................",
        "................",
    ], {"o": NIGHT, "b": (24, 60, 92), "L": (36, 84, 120), "s": (14, 36, 60), "c": g.CREAM, "x": g.CREAM_SHADE})
    token(img, 5, 5, 3)
    return img


def soul_ledger_model_texture():
    """64x64 texture of the 3D book, laid out like the other books: night-blue leather, silver corners, a ring of
    soul tokens set in the cover, old pages, a cyan stone on the clasp."""
    s = 4
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    def box(u0, v0, u1, v1, fill):
        d.rectangle((u0 * s, v0 * s, u1 * s - 1, v1 * s - 1), fill=fill + (255,))

    dark, mid, light = (12, 32, 52), (24, 60, 92), (40, 92, 130)
    W, H = 12 * s, 9 * s
    for y in range(H):
        for x in range(W):
            img.putpixel((x, y), g.lerp(dark, light, fbm(x / 5, y / 5, 3, 57) * .9) + (255,))
    cx, cy = W // 2, H // 2
    for k in range(8):
        a = k * math.tau / 8
        token(img, round(cx - 2 + math.cos(a) * 11), round(cy - 2 + math.sin(a) * 11), 4)
    token(img, cx - 4, cy - 4, 8)
    d.rectangle((0, 0, W - 1, H - 1), outline=NIGHT + (255,))
    d.rectangle((1, 1, W - 2, H - 2), outline=SILVER_DARK + (255,))
    for x0, y0 in ((2, 2), (W - 6, 2), (2, H - 6), (W - 6, H - 6)):
        d.rectangle((x0, y0, x0 + 3, y0 + 3), fill=SILVER_DARK + (255,))
        img.putpixel((x0, y0), SILVER + (255,))
    box(12, 0, 13, 9, dark)
    d.line((12 * s, 0, 12 * s, H - 1), fill=SILVER_DARK + (255,))
    for y in range(0, 8 * s):
        for x in range(13 * s, 16 * s):
            img.putpixel((x, y), g.lerp(g.CREAM, (180, 165, 140), fbm(x / 3, y / 3, 2, 9) * .7) + (255,))
    for y in range(1, 8 * s, 2):
        d.line((13 * s, y, 16 * s - 1, y), fill=g.CREAM_SHADE + (255,))
    for y in range(9 * s, 12 * s):
        for x in range(0, 12 * s):
            t = abs(y - 10.5 * s) / (1.5 * s)
            img.putpixel((x, y), g.lerp(mid, dark, t) + (255,))
    for x in range(3, 12 * s - 3, 8):
        token(img, x, 9 * s + 4, 3)
    box(0, 12, 12, 15, g.CREAM)
    for x in range(0, 12 * s, 2):
        d.line((x, 12 * s, x, 15 * s - 1), fill=g.CREAM_SHADE + (255,))
    box(12, 9, 14, 11, SILVER_DARK)
    d.line((12 * s + 1, 9 * s + 1, 14 * s - 2, 9 * s + 1), fill=SILVER + (255,))
    for y in range(9 * s, 11 * s):
        for x in range(14 * s, 16 * s):
            t = math.hypot(x - 14.8 * s, y - 9.8 * s) / (1.4 * s)
            img.putpixel((x, y), g.lerp(CYAN_LIGHT, CYAN_DEEP, min(1, t ** .8)) + (255,))
    box(12, 11, 16, 12, dark)
    for x in range(12 * s + 1, 16 * s, 3):
        img.putpixel((x, 11 * s + 1), SILVER + (255,))
        img.putpixel((x, 12 * s - 2), SILVER + (255,))
    return img


def affinity_ring_token():
    return g.from_grid([
        "................",
        "......oooo......",
        ".....oRRRRo.....",
        ".....oRCLRo.....",
        "....oooooooo....",
        "...ohllllbbso...",
        "..ohlo....obso..",
        "..olo......oso..",
        "..olo......oso..",
        "..olo......oso..",
        "..obso....osso..",
        "...obbbbbssso...",
        "....oooooooo....",
        "................",
        "................",
        "................",
    ], {"o": NIGHT, "h": SILVER, "l": (184, 204, 214), "b": SILVER_DARK, "s": (80, 100, 112),
        "R": RIM, "C": CYAN, "L": CYAN_LIGHT})


def main():
    soul_token()
    for name, icon in (("token_throw", token_throw_icon), ("token_wave", token_wave_icon), ("token_storm", token_storm_icon),
                       ("token_ricochet", token_ricochet_icon), ("coin_flip", coin_flip_icon), ("token_aegis", token_aegis_icon),
                       ("token_rain", token_rain_icon), ("token_mines", token_mines_icon), ("soul_tribute", soul_tribute_icon),
                       ("token_hand", token_hand_icon)):
        g.save(icon(), f"textures/gui/spell_icons/{name}.png")
    g.save(affinity_ring_token(), "textures/item/affinity_ring_token.png")
    g.save(soul_ledger_icon(), "textures/item/soul_ledger.png")
    g.save(soul_ledger_model_texture(), "textures/item/soul_ledger_3d.png")
    g.write_json("models/item/affinity_ring_token.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/affinity_ring_token"}})
    g.book_models("soul_ledger")


if __name__ == "__main__":
    main()
