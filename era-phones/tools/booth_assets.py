"""The phone booth (original design, drawn here): two blocks tall, open at the front; a frame of brushed aluminium,
smoked glass on both sides, a kick plate below; a light box on the roof that glows, "TELEPHONE" on its four sides, and
a lamp under it lighting the inside; on the back wall a steel payphone (its handset on the hook, its armoured cord, a
chrome keypad, the coin slot, the little window and the return cup), a card of instructions above it, a shelf with a
phone book under it. Its sounds: the bell, the coin falling in, a key pressed."""
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

import gear_assets as ga
import kit
from models import Model, vgrad, round_alpha, metal_edge, text, text_width

NAME = "cabine_telephonique"
HERE = os.path.dirname(os.path.abspath(__file__))
SIGN_FONT = os.path.join(HERE, "fonts", "Oswald-Bold.woff")
LIT = {"block_light": 15, "sky_light": 15}

ALU_DARK, ALU_LIGHT = (128, 134, 142), (226, 230, 236)
ENAMEL = (40, 66, 104)
STEEL = (168, 172, 180)


# ------------------------------------------------------------------------------------------------ painters

def brushed(tile, vertical=True, base=(190, 196, 204), seed=1):
    """Brushed aluminium: fine streaks along the grain, a soft sheen across."""
    w, h = tile.size
    rng = np.random.default_rng(seed + w * 7 + h)
    if vertical:
        streak = rng.normal(0, 1, (1, w)).repeat(h, 0)
        sheen = np.sin(np.linspace(0, np.pi, w))[None, :].repeat(h, 0)
    else:
        streak = rng.normal(0, 1, (h, 1)).repeat(w, 1)
        sheen = np.sin(np.linspace(0, np.pi, h))[:, None].repeat(w, 1)
    v = np.array(base, float)[None, None, :] * (.86 + .14 * sheen[..., None]) + streak[..., None] * 5
    a = np.dstack([np.clip(v, 0, 255), np.full((h, w), 255)])
    tile.paste(Image.fromarray(a.astype(np.uint8), "RGBA"))


def alu(vertical=True, base=(190, 196, 204)):
    def p(tile):
        brushed(tile, vertical, base)
    return p


def alu_edge(tile):
    metal_edge(tile, ALU_DARK, ALU_LIGHT)


def ribbed(tile):
    """The booth's back, outside: aluminium pressed in vertical ribs."""
    brushed(tile, True)
    d = ImageDraw.Draw(tile)
    w, h = tile.size
    step = max(6, w // 14)
    for x in range(step // 2, w, step):
        d.line((x, 0, x, h), fill=(150, 156, 164, 255), width=2)
        d.line((x + 2, 0, x + 2, h), fill=(232, 236, 240, 255), width=1)


def glass(tile, glints=True):
    """Smoked glass: grey-blue, see-through, a long slanted glint or two (seen from outside), a little grime along the
    bottom."""
    w, h = tile.size
    ys, xs = np.mgrid[0:h, 0:w]
    a = np.full((h, w), 70.0)
    col = np.zeros((h, w, 3)) + np.array([118, 140, 150], float)
    for k, (off, width, strength) in enumerate(((.18, .05, 80), (.27, .018, 60), (.68, .03, 45)) if glints else ()):
        band = np.exp(-(((xs / w + ys / h * .55) - off - .3) / width) ** 2)
        a += band * strength
        col += band[..., None] * np.array([90, 90, 80])
    grime = np.clip((ys / h - .82) / .18, 0, 1) * 50
    a += grime
    col -= grime[..., None] * .6
    tile.paste(Image.fromarray(np.dstack([np.clip(col, 0, 255), np.clip(a, 0, 255)]).astype(np.uint8), "RGBA"))


def concrete(tile):
    w, h = tile.size
    rng = np.random.default_rng(w + h)
    v = 150 + rng.normal(0, 7, (h, w)) + (rng.random((h, w)) < .02) * -40
    tile.paste(Image.fromarray(np.dstack([v, v - 2, v - 6, np.full((h, w), 255)]).clip(0, 255).astype(np.uint8), "RGBA"))


def sign(tile):
    """The light box's face: a milky white panel lit from inside, "TELEPHONE" in deep blue, a blue rule under it."""
    w, h = tile.size
    S = 3
    img = Image.new("RGBA", (w * S, h * S), (0, 0, 0, 255))
    a = np.zeros((h * S, w * S, 3))
    ys = np.linspace(0, 1, h * S)[:, None]
    a[:] = (np.array([246, 249, 255], float) * (1 - .06 * np.abs(ys - .5) * 2)[..., None])
    img = Image.fromarray(a.astype(np.uint8), "RGB").convert("RGBA")
    d = ImageDraw.Draw(img)
    size = int(h * S * .62)
    font = ImageFont.truetype(SIGN_FONT, size)
    word = "TELEPHONE"
    while font.getlength(word) > w * S * .86:
        size -= 2
        font = ImageFont.truetype(SIGN_FONT, size)
    d.text((w * S / 2, h * S * .47), word, font=font, fill=(22, 52, 120, 255), anchor="mm")
    d.rectangle((w * S * .12, h * S * .84, w * S * .88, h * S * .88), fill=(22, 52, 120, 255))
    d.rectangle((0, 0, w * S - 1, h * S - 1), outline=(150, 156, 166, 255), width=S * 2)
    tile.paste(img.resize((w, h), Image.LANCZOS))


def ceiling_lamp(tile):
    """The lamp under the light box: a frosted panel, warm white, a grid of the diffuser over it."""
    w, h = tile.size
    vgrad(tile, (255, 248, 230), (250, 240, 214))
    d = ImageDraw.Draw(tile)
    d.rectangle((0, 0, w - 1, h - 1), outline=(170, 172, 176, 255), width=max(2, w // 24))
    for k in range(1, 6):
        x = int(w * k / 6)
        d.line((x, 0, x, h), fill=(236, 226, 200, 255), width=1)
        y = int(h * k / 6)
        d.line((0, y, w, y), fill=(236, 226, 200, 255), width=1)


def enamel(tile):
    """The back wall inside: blue enamel, a little lighter at the top where the lamp falls on it."""
    vgrad(tile, ga.shade(ENAMEL, 1.25), ga.shade(ENAMEL, .85))


def steel(tile):
    """The payphone's body: brushed stainless, slightly darker below."""
    w, h = tile.size
    brushed(tile, False, STEEL, seed=4)
    shade = np.linspace(1.05, .86, h)[:, None, None]
    a = np.asarray(tile, float)
    a[..., :3] *= shade
    tile.paste(Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA"))


def payphone_face(tile):
    """The payphone's front: the little window (a coin drawn in it), the coin slot top right, the plate where the
    keypad sits, the maker's name, the return cup's opening below."""
    steel(tile)
    w, h = tile.size
    d = ImageDraw.Draw(tile)
    # The window: dark green glass with a coin and "25" in it
    wx0, wy0, wx1, wy1 = w * .1, h * .07, w * .62, h * .19
    d.rounded_rectangle((wx0, wy0, wx1, wy1), radius=3, fill=(16, 34, 24, 255), outline=(90, 94, 100, 255), width=2)
    cy = (wy0 + wy1) / 2
    r = (wy1 - wy0) * .3
    d.ellipse((wx0 + 8, cy - r, wx0 + 8 + 2 * r, cy + r), outline=(110, 220, 140, 255), width=2)
    text(d, "25", int(wx0 + 14 + 2 * r), int(cy - 5), (110, 220, 140, 255), 2)
    # The coin slot: a chrome bezel, a black slit
    sx0, sy0, sx1, sy1 = w * .7, h * .06, w * .9, h * .2
    d.rounded_rectangle((sx0, sy0, sx1, sy1), radius=4, fill=(214, 218, 224, 255), outline=(110, 114, 120, 255), width=2)
    d.rectangle(((sx0 + sx1) / 2 - 2, sy0 + 6, (sx0 + sx1) / 2 + 2, sy1 - 6), fill=(10, 10, 12, 255))
    # The plate the keys stand on
    d.rounded_rectangle((w * .2, h * .27, w * .8, h * .74), radius=6, fill=(56, 58, 64, 255), outline=(30, 30, 34, 255), width=2)
    # The return cup: a dark opening with a flap
    d.rounded_rectangle((w * .32, h * .82, w * .68, h * .95), radius=5, fill=(20, 20, 24, 255), outline=(200, 204, 210, 255), width=2)
    d.rectangle((w * .34, h * .82, w * .66, h * .86), fill=(150, 154, 160, 255))
    text(d, "VOXA", int(w * .5 - text_width("VOXA", 2) / 2), int(h * .765), (60, 62, 70, 255), 2)
    round_alpha(tile, 6)


def instructions(tile):
    """The card above the payphone: a blue band, three steps drawn as pictures (lift the handset, a coin, the keys)
    and lines of small print."""
    w, h = tile.size
    vgrad(tile, (242, 240, 232), (226, 222, 210))
    d = ImageDraw.Draw(tile)
    d.rectangle((0, 0, w, h * .2), fill=(22, 52, 120, 255))
    text(d, "VOXA", int(w / 2 - text_width("VOXA", 2) / 2), int(h * .04), (240, 240, 246, 255), 2)
    cols = 3
    for k in range(cols):
        x0 = w * (.06 + k * .32)
        x1 = x0 + w * .26
        y0, y1 = h * .28, h * .62
        d.rounded_rectangle((x0, y0, x1, y1), radius=4, outline=(22, 52, 120, 255), width=2)
        cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
        text(d, str(k + 1), int(x0 + 3), int(y0 + 3), (22, 52, 120, 255), 1)
        if k == 0:
            d.arc((cx - 9, cy - 8, cx + 9, cy + 10), 200, 340, fill=(30, 30, 34, 255), width=4)
        elif k == 1:
            d.ellipse((cx - 7, cy - 7, cx + 7, cy + 7), fill=(200, 200, 206, 255), outline=(110, 110, 116, 255), width=2)
        else:
            for i in range(3):
                for j in range(3):
                    d.rectangle((cx - 9 + i * 7, cy - 9 + j * 7, cx - 5 + i * 7, cy - 5 + j * 7), fill=(60, 60, 66, 255))
    for k in range(3):
        y = h * (.7 + k * .09)
        d.line((w * .08, y, w * (.92 - k * .15), y), fill=(120, 116, 108, 255), width=2)
    round_alpha(tile, 3)


def phone_book(tile):
    """The phone book's cover: yellow, a dark band, a handset drawn on it."""
    w, h = tile.size
    vgrad(tile, (238, 196, 40), (214, 168, 24))
    d = ImageDraw.Draw(tile)
    d.rectangle((0, h * .62, w, h * .74), fill=(30, 30, 34, 255))
    d.arc((w * .38, h * .18, w * .62, h * .5), 200, 340, fill=(30, 30, 34, 255), width=3)


def pages(tile):
    w, h = tile.size
    vgrad(tile, (238, 232, 214), (222, 214, 192))
    d = ImageDraw.Draw(tile)
    for y in range(1, h, 2):
        d.line((0, y, w, y), fill=(200, 192, 170, 255), width=1)


def handset_black(tile):
    vgrad(tile, (40, 40, 44), (14, 14, 16))


def cord(tile):
    """The armoured cord: rings of chrome."""
    w, h = tile.size
    tile.paste(Image.new("RGBA", tile.size, (150, 154, 162, 255)))
    d = ImageDraw.Draw(tile)
    long_axis_vertical = h >= w
    n = (h if long_axis_vertical else w)
    for k in range(0, n, 4):
        if long_axis_vertical:
            d.line((0, k, w, k), fill=(230, 234, 240, 255), width=2)
            d.line((0, k + 2, w, k + 2), fill=(90, 94, 100, 255), width=1)
        else:
            d.line((k, 0, k, h), fill=(230, 234, 240, 255), width=2)
            d.line((k + 2, 0, k + 2, h), fill=(90, 94, 100, 255), width=1)


# ------------------------------------------------------------------------------------------------ the model

def booth():
    m = Model(NAME, size=1024, density=8)
    lit = []

    def box(frm, to, paint, rotation=None, glow=False):
        m.box(frm, to, paint, rotation)
        if glow:
            lit.append(len(m.elements) - 1)

    post = {"*": alu(True), "up": alu_edge, "down": alu_edge}
    rail = {"*": alu(False), "east": alu_edge, "west": alu_edge}
    # The plinth: a slab of concrete
    box([0, 0, 0], [16, 1, 16], {"*": concrete})
    # The four posts
    for x0, z0 in ((0, 0), (14.8, 0), (0, 14.8), (14.8, 14.8)):
        box([x0, 1, z0], [x0 + 1.2, 29, z0 + 1.2], post)
    # The back wall: ribbed aluminium outside, blue enamel inside
    box([1.2, 1, .3], [14.8, 29, 1.0], {"north": ribbed, "south": enamel})
    # The sides: a kick plate, two panes of smoked glass, rails between them
    for x0, x1 in ((.3, .9), (15.1, 15.7)):
        box([x0, 1, 1.2], [x1, 8, 14.8], {"east": alu(False), "west": alu(False), "up": alu_edge})
        box([x0, 8, 1.2], [x1, 8.6, 14.8], rail)
        box([x0, 17.6, 1.2], [x1, 18.2, 14.8], rail)
        box([x0, 27.4, 1.2], [x1, 28, 14.8], rail)
        gx0, gx1 = (x0 + x1) / 2 - .06, (x0 + x1) / 2 + .06
        outer, inner = ("west", "east") if x0 < 8 else ("east", "west")
        panes = {outer: glass, inner: lambda tile: glass(tile, False)}
        box([gx0, 8.6, 1.2], [gx1, 17.6, 14.8], panes)
        box([gx0, 18.2, 1.2], [gx1, 27.4, 14.8], panes)
    # The front: a header bar across the opening
    box([1.2, 27.4, 15.0], [14.8, 29, 15.8], {"south": alu(False), "north": alu(False), "down": alu_edge})
    # The light box: its four lit faces, the lamp under it, an aluminium cap on top
    box([1.2, 28.98, 1.2], [14.8, 29, 14.8], {"down": ceiling_lamp}, glow=True)
    box([0, 31.4, 0], [16, 32, 16], {"up": alu(False), "north": alu_edge, "south": alu_edge, "east": alu_edge, "west": alu_edge,
                                     "down": alu_edge})

    # The payphone on the back wall, drawn finer on its own texture
    pm = Model(NAME + "_publiphone", size=1024, density=24)

    plit = []

    def pbox(frm, to, paint, glow=False):
        pm.box(frm, to, paint)
        if glow:
            plit.append(len(pm.elements) - 1)

    # The light box's four lit faces, on the fine texture so that the letters are sharp
    pbox([.2, 29, .2], [15.8, 31.4, 15.8], {"north": sign, "south": sign, "east": sign, "west": sign}, glow=True)

    pm.body(4.6, 12.4, 11.4, 22.6, 1.0, 3.4, .6, payphone_face, steel, (110, 114, 120), (220, 224, 230))
    # Its keypad: four rows of three chrome keys
    labels = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"]
    kw, kh = 1.0, .95
    for i, lab in enumerate(labels):
        c, r = i % 3, i // 3
        pm.key(6.2 + c * 1.3, 18.85 - r * 1.25, kw, kh, 3.4, .35, (206, 210, 216), lab, (30, 30, 34, 255))
    # The hook the handset hangs on, on the left side
    pbox([3.6, 21.6, 1.7], [4.6, 22.1, 2.9], {"*": ga.facet((200, 204, 210), 1.0)})
    # The handset, hanging down the left side: grip, earpiece above, mouthpiece below
    pbox([3.55, 14.6, 1.7], [4.55, 21.0, 2.9], {"*": handset_black})
    pbox([3.25, 19.4, 1.5], [4.55, 21.6, 3.1], {"*": handset_black, "west": ga.disc((34, 34, 38), (20, 20, 22), None)})
    pbox([3.25, 14.0, 1.5], [4.55, 16.2, 3.1], {"*": handset_black, "west": ga.disc((34, 34, 38), (20, 20, 22), None)})
    # The armoured cord: down from the handset, along the bottom, up into the payphone
    c = {"*": cord}
    pbox([3.8, 11.3, 2.0], [4.3, 14.0, 2.5], c)
    pbox([3.8, 10.9, 2.0], [5.6, 11.4, 2.5], c)
    pbox([5.2, 11.3, 2.0], [5.7, 12.4, 2.5], c)
    # The card of instructions above it
    pbox([4.8, 23.2, 1.0], [11.2, 27.0, 1.12], {"south": instructions})
    # The shelf under it and the phone book lying on it
    box([2.5, 9.6, 1.0], [13.5, 10.1, 5.6], {"up": alu(False), "down": alu(False), "south": alu_edge, "east": alu_edge, "west": alu_edge})
    box([8.2, 10.1, 1.6], [12.4, 10.9, 4.8], {"up": phone_book, "south": pages, "east": pages, "west": pages, "north": pages})
    # The bracket under the shelf
    box([7.6, 7.6, 1.0], [8.4, 9.6, 3.0], {"*": alu(True)})

    for model, which in ((m, lit), (pm, plit)):
        for i in which:
            model.elements[i]["neoforge_data"] = LIT
            model.elements[i]["shade"] = False
    write(m, pm)


def write(m, pm):
    kit.save(m.img, f"textures/block/{m.name}.png")
    kit.save(pm.img, f"textures/block/{pm.name}.png")
    kit.write_json(f"models/block/{m.name}.json", {
        "render_type": "minecraft:translucent",
        "ambientocclusion": False,
        "textures": {"t": f"eraphones:block/{m.name}", "p": f"eraphones:block/{pm.name}", "particle": f"eraphones:block/{m.name}"},
        "elements": [dict(e, faces={k: dict(v, texture="#t") for k, v in e["faces"].items()}) for e in m.elements]
        + [dict(e, faces={k: dict(v, texture="#p") for k, v in e["faces"].items()}) for e in pm.elements],
        "display": {
            "gui": {"rotation": [20, 205, 0], "translation": [0, -3.2, 0], "scale": [.38, .38, .38]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [.18, .18, .18]},
            "fixed": {"rotation": [0, 180, 0], "translation": [0, -4, 0], "scale": [.38, .38, .38]},
            "thirdperson_righthand": {"rotation": [0, 135, 0], "translation": [0, 0, 0], "scale": [.2, .2, .2]},
            "thirdperson_lefthand": {"rotation": [0, 135, 0], "translation": [0, 0, 0], "scale": [.2, .2, .2]},
            "firstperson_righthand": {"rotation": [0, 160, 0], "translation": [0, -1, 0], "scale": [.22, .22, .22]},
            "firstperson_lefthand": {"rotation": [0, 160, 0], "translation": [0, -1, 0], "scale": [.22, .22, .22]}}})
    # The upper half shows nothing of its own: the lower half's model is two blocks tall
    kit.write_json(f"models/block/{m.name}_haut.json", {"textures": {"particle": f"eraphones:block/{m.name}"}, "elements": []})
    kit.write_json(f"models/item/{m.name}.json", {"parent": f"eraphones:block/{m.name}"})
    variants = {}
    for f, y in (("south", 0), ("west", 90), ("north", 180), ("east", 270)):
        lower = {"model": f"eraphones:block/{m.name}"}
        if y:
            lower["y"] = y
        variants[f"facing={f},half=lower"] = lower
        variants[f"facing={f},half=upper"] = {"model": f"eraphones:block/{m.name}_haut"}
    kit.write_json(f"blockstates/{m.name}.json", {"variants": variants})
    ga.write_data(f"loot_table/blocks/{m.name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": f"eraphones:{m.name}"}], "conditions": [
        {"condition": "minecraft:survives_explosion"},
        {"condition": "minecraft:block_state_property", "block": f"eraphones:{m.name}", "properties": {"half": "lower"}}]}]})


# ------------------------------------------------------------------------------------------------ the payphone's screen

def gui():
    """The payphone as its user sees it, two pixels a unit: a steel panel 230 x 250 (the LCD's bezel at 20,18 to
    210,62; the keypad's well at 50,70 to 180,206; the coin slot at 188,78 to 214,120, "25" under it; the cradle at 8,70
    to 44,206), and its loose pieces: the handset (32 x 120 at 0,0), a key up and down (34 x 24 at 64,0 and 64,48), the
    green and red buttons (56 x 20 at 64,96 and 64,136)."""
    S = 2
    W, H = 230 * S, 250 * S
    panel = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    body = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    brushed(body, False, (176, 180, 188), seed=9)
    a = np.asarray(body, float)
    a[..., :3] *= np.linspace(1.06, .84, H)[:, None, None]
    body = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")
    mask = Image.new("L", (W, H), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, W - 1, H - 1), radius=14 * S, fill=255)
    panel.paste(body, (0, 0), mask)
    d = ImageDraw.Draw(panel)
    d.rounded_rectangle((0, 0, W - 1, H - 1), radius=14 * S, outline=(96, 100, 108, 255), width=2 * S)
    d.rounded_rectangle((2 * S, 2 * S, W - 2 * S - 1, H - 2 * S - 1), radius=12 * S, outline=(232, 236, 242, 255), width=S)

    def well(x0, y0, x1, y1, fill, r=5):
        d.rounded_rectangle((x0 * S, y0 * S, x1 * S, y1 * S), radius=r * S, fill=fill)
        d.rounded_rectangle((x0 * S, y0 * S, x1 * S, y1 * S), radius=r * S, outline=(70, 72, 78, 255), width=S)
        d.line((x0 * S + r * S, y1 * S, x1 * S - r * S, y1 * S), fill=(236, 240, 246, 255), width=S)

    # The screws in the corners
    for x, y in ((9, 9), (221, 9), (9, 241), (221, 241)):
        d.ellipse(((x - 3.5) * S, (y - 3.5) * S, (x + 3.5) * S, (y + 3.5) * S), fill=(150, 154, 160, 255), outline=(80, 82, 88, 255), width=S)
        d.line(((x - 2.4) * S, y * S, (x + 2.4) * S, y * S), fill=(70, 72, 78, 255), width=S)
    # The LCD's bezel and its glass (the text is drawn by the game)
    well(20, 18, 210, 62, (30, 32, 36, 255), 6)
    lcd = Image.new("RGBA", (186 * S, 40 * S))
    vgrad(lcd, (44, 70, 46), (30, 52, 34))
    ld = ImageDraw.Draw(lcd)
    for y in range(0, lcd.size[1], 3):
        ld.line((0, y, lcd.size[0], y), fill=(36, 60, 38, 255), width=1)
    panel.alpha_composite(lcd, (22 * S, 20 * S))
    # The keypad's well, the cradle, the coin slot
    well(50, 70, 180, 206, (52, 54, 60, 255), 8)
    well(8, 70, 44, 206, (60, 62, 68, 255), 8)
    d.rounded_rectangle((20 * S, 74 * S, 32 * S, 82 * S), radius=2 * S, fill=(196, 200, 206, 255), outline=(90, 92, 98, 255), width=S)
    d.rounded_rectangle((188 * S, 78 * S, 214 * S, 120 * S), radius=5 * S, fill=(214, 218, 224, 255), outline=(100, 104, 110, 255), width=S)
    d.rectangle((199.5 * S, 84 * S, 202.5 * S, 114 * S), fill=(8, 8, 10, 255))
    f = ImageFont.truetype(SIGN_FONT, 11 * S)
    d.text((201 * S, 130 * S), "25¢", font=f, fill=(70, 72, 80, 255), anchor="mm")
    # The coin return, and the maker's name
    well(186, 176, 216, 204, (18, 18, 22, 255), 6)
    d.rectangle((188 * S, 176 * S, 214 * S, 182 * S), fill=(160, 164, 170, 255))
    f = ImageFont.truetype(SIGN_FONT, 10 * S)
    d.text((115 * S, 228 * S), "VOXA", font=f, fill=(84, 86, 94, 255), anchor="mm")
    kit.save(panel, "textures/gui/gear/publiphone.png")

    pieces = Image.new("RGBA", (256, 512), (0, 0, 0, 0))
    # The handset: black, its earpiece and mouthpiece wider, a soft light down its left side
    hs = Image.new("RGBA", (32 * S, 120 * S), (0, 0, 0, 0))
    hd = ImageDraw.Draw(hs)
    hd.rounded_rectangle((6 * S, 10 * S, 24 * S, 110 * S), radius=6 * S, fill=(26, 26, 30, 255))
    for y0 in (0, 92):
        hd.rounded_rectangle((1 * S, y0 * S, 31 * S, (y0 + 28) * S), radius=9 * S, fill=(30, 30, 34, 255))
        for k in range(3):
            for j in range(3):
                cx, cy = (11 + j * 5) * S, (y0 + 9 + k * 5) * S
                hd.ellipse((cx - 2, cy - 2, cx + 2, cy + 2), fill=(10, 10, 12, 255))
    hl = Image.new("RGBA", hs.size, (0, 0, 0, 0))
    ImageDraw.Draw(hl).rounded_rectangle((3 * S, 4 * S, 7 * S, 116 * S), radius=2 * S, fill=(120, 124, 132, 90))
    hs.alpha_composite(hl.filter(ImageFilter.GaussianBlur(2)))
    pieces.alpha_composite(hs, (0, 0))

    def key_sprite(pressed):
        k = Image.new("RGBA", (34 * S, 24 * S), (0, 0, 0, 0))
        kd = ImageDraw.Draw(k)
        top, bot = ((206, 210, 218), (150, 154, 162)) if not pressed else ((160, 164, 172), (196, 200, 208))
        face = Image.new("RGBA", k.size)
        vgrad(face, top, bot)
        m = Image.new("L", k.size, 0)
        ImageDraw.Draw(m).rounded_rectangle((0, 0, k.size[0] - 1, k.size[1] - 1), radius=4 * S, fill=255)
        k.paste(face, (0, 0), m)
        kd.rounded_rectangle((0, 0, k.size[0] - 1, k.size[1] - 1), radius=4 * S, outline=(80, 82, 90, 255), width=S)
        if not pressed:
            kd.line((4 * S, S * 1.5, 30 * S, S * 1.5), fill=(248, 250, 252, 255), width=S)
        return k

    pieces.alpha_composite(key_sprite(False), (64 * S, 0))
    pieces.alpha_composite(key_sprite(True), (64 * S, 48 * S))

    def button(c0, c1, y):
        b = Image.new("RGBA", (56 * S, 20 * S), (0, 0, 0, 0))
        face = Image.new("RGBA", b.size)
        vgrad(face, c0, c1)
        m = Image.new("L", b.size, 0)
        ImageDraw.Draw(m).rounded_rectangle((0, 0, b.size[0] - 1, b.size[1] - 1), radius=6 * S, fill=255)
        b.paste(face, (0, 0), m)
        ImageDraw.Draw(b).rounded_rectangle((0, 0, b.size[0] - 1, b.size[1] - 1), radius=6 * S, outline=(30, 30, 34, 255), width=S)
        pieces.alpha_composite(b, (64 * S, y * S))

    button((96, 196, 104), (40, 130, 56), 96)
    button((226, 92, 80), (160, 36, 30), 136)
    kit.save(pieces, "textures/gui/gear/publiphone_pieces.png")


# ------------------------------------------------------------------------------------------------ sounds

def bell(rate=48000):
    """An electromechanical bell: a hammer beating fast between two bells for a second, twice."""
    n = int(rate * 2.0)
    t = np.arange(n) / rate
    y = np.zeros(n)
    for start in (0.0, 1.0 - .55):
        for k in range(int(.4 * 20)):
            at = start + k / 20
            i = int(at * rate)
            tt = np.arange(n - i) / rate
            f = 1180 if k % 2 == 0 else 1420
            ring = (np.sin(2 * np.pi * f * tt) + .5 * np.sin(2 * np.pi * f * 2.76 * tt) + .25 * np.sin(2 * np.pi * f * 5.4 * tt)) * np.exp(-tt * 9)
            y[i:] += ring * .4
    y += ga.band(np.random.default_rng(2).normal(0, 1, n), 2000, 8000) * .02
    return y / np.abs(y).max() * .85


def coin(rate=48000):
    """A coin dropped in: it slides, knocks twice inside the box and settles with the relay's click."""
    rng = np.random.default_rng(5)
    n = int(rate * .9)
    t = np.arange(n) / rate
    y = ga.band(rng.normal(0, 1, n), 2500, 9000) * np.exp(-t / .05) * .25
    for at, f, k in ((.08, 3900, 1.0), (.2, 3300, .7), (.31, 2700, .5), (.62, 900, .4)):
        tt = np.clip(t - at, 0, None)
        y += (np.sin(2 * np.pi * f * tt) + .6 * np.sin(2 * np.pi * f * 1.52 * tt)) * np.exp(-tt * 40) * (t >= at) * k
    tt = np.clip(t - .62, 0, None)
    y += rng.normal(0, 1, n) * np.exp(-tt / .004) * (t >= .62) * .5
    return y / np.abs(y).max() * .85


def key(rate=48000):
    """A key pressed: the click of the key and its two tones."""
    n = int(rate * .2)
    t = np.arange(n) / rate
    tone = (np.sin(2 * np.pi * 770 * t) + np.sin(2 * np.pi * 1336 * t)) * .35 * np.clip((.17 - t) / .02, 0, 1) * (t > .015)
    click = np.random.default_rng(1).normal(0, 1, n) * np.exp(-t / .003) * .6
    return np.clip(tone + click, -1, 1) * .8


def recipes():
    ga.shaped(NAME, ["III", "GPG", "ISI"], {"I": "minecraft:iron_ingot", "G": "minecraft:glass_pane", "P": "eraphones:gsm",
                                             "S": "minecraft:smooth_stone_slab"})


def main():
    booth()
    gui()
    recipes()


if __name__ == "__main__":
    main()
