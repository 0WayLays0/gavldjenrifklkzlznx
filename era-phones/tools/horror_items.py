"""The television and the frightening things (original art, drawn here): a cathode-ray set in a wooden cabinet with
rabbit ears; a music box with its dancer and its key, and the waltz it plays as its spring runs down; a porcelain
doll in a lace dress (its head, drawn by the game so it can turn, painted here); a red balloon on its string; the game
master's remote. Their block states, loot and recipes."""
import math

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

import gear_assets as ga
import kit
from models import Model, vgrad, round_alpha, text, text_width


# ------------------------------------------------------------------------------------------------ the television

# Where the glass is (model sixteenths, the set facing south); the game lays the lit screen over it (TvRenderer)
GX0, GX1, GY0, GY1, FRONT = 2.0, 10.4, 3.2, 9.5, 13.4
CX0, CX1, CY0, CY1 = 1, 15, .8, 13


def glass(tile, x0, x1, y0, y1, ux, uy):
    """The dead tube in the cabinet's face: dark grey-green glass, bulging, a soft window reflection."""
    d = ImageDraw.Draw(tile)
    w, h = tile.size
    gx0, gx1 = (GX0 - x0) / (x1 - x0) * w, (GX1 - x0) / (x1 - x0) * w
    gy0, gy1 = (y1 - GY1) / (y1 - y0) * h, (y1 - GY0) / (y1 - y0) * h
    pad = 4
    d.rounded_rectangle((gx0 - pad, gy0 - pad, gx1 + pad, gy1 + pad), radius=12, fill=(18, 16, 14, 255))
    g = Image.new("RGBA", (int(gx1 - gx0), int(gy1 - gy0)))
    gw, gh = g.size
    yy, xx = np.mgrid[0:gh, 0:gw]
    nx, ny = xx / gw * 2 - 1, yy / gh * 2 - 1
    bulge = 1 - .35 * (nx ** 2 + ny ** 2)
    base = np.array([44, 52, 48], float)[None, None, :] * bulge[..., None]
    refl = np.clip(1 - np.abs((xx / gw + yy / gh * .6) - .55) / .08, 0, 1) * (yy < gh * .55) * 60
    rgb = np.clip(base + refl[..., None], 0, 255)
    g = Image.fromarray(np.dstack([rgb, np.full((gh, gw), 255)]).astype(np.uint8), "RGBA")
    round_alpha(g, 14)
    tile.alpha_composite(g, (int(gx0), int(gy0)))


def television():
    m = Model("television", size=1024)

    def front(tile):
        ga.wood(tile, dark=(70, 40, 20), light=(122, 76, 40), seed=11)
        w, h = tile.size
        d = ImageDraw.Draw(tile)
        glass(tile, CX0, CX1, CY0, CY1, 0, 0)
        # The panel on the right: brushed silver, the channel dial, the speaker's slots, the maker's name, a red light
        px0 = (11.0 - CX0) / (CX1 - CX0) * w
        d.rounded_rectangle((px0, 10, w - 10, h - 10), radius=6, fill=(176, 178, 184, 255), outline=(90, 92, 98, 255), width=2)
        for y in range(int(h * .58), h - 18, 5):
            d.rounded_rectangle((px0 + 8, y, w - 18, y + 2), radius=1, fill=(40, 40, 44, 255))
        text(d, "VOXA", int(px0 + (w - 10 - px0) / 2 - text_width("VOXA", 2) / 2), int(h * .46), (50, 50, 56, 255), 2)
        d.ellipse((w - 26, h - 26, w - 18, h - 18), fill=(220, 40, 30, 255))
        round_alpha(tile, 16)

    def back(tile):
        ga.wood(tile, dark=(60, 34, 18), light=(100, 62, 34), seed=12)
        d = ImageDraw.Draw(tile)
        w, h = tile.size
        for k in range(8):
            d.rectangle((24, 24 + k * 14, w - 24, 30 + k * 14), fill=(28, 18, 10, 255))
        round_alpha(tile, 16)

    m.body(CX0, CY0, CX1, CY1, 2, FRONT, 1.0, front, back, (56, 32, 16), (138, 88, 48))
    # The rubber bezel round the tube
    rubber = {"*": (24, 22, 20), "south": ga.facet((30, 28, 26), 1.0)}
    for frm, to in (([GX0 - .4, GY1, FRONT], [GX1 + .4, GY1 + .4, FRONT + .45]), ([GX0 - .4, GY0 - .4, FRONT], [GX1 + .4, GY0, FRONT + .45]),
                    ([GX0 - .4, GY0, FRONT], [GX0, GY1, FRONT + .45]), ([GX1, GY0, FRONT], [GX1 + .4, GY1, FRONT + .45])):
        m.box(frm, to, rubber)
    # The knobs: channels, volume
    silver = (190, 192, 198)
    ga.octo(m, "z", FRONT, FRONT + .7, 12.8, 10.6, .95, (40, 40, 44), end_b=ga.disc((60, 60, 66), (24, 24, 28), mark=(230, 230, 236)))
    # The volume knob: silver, on a dark collar so it stands out of the panel
    ga.octo(m, "z", FRONT, FRONT + .2, 12.8, 8.2, .95, (34, 34, 38), end_b=ga.disc((44, 44, 50), (20, 20, 24)))
    ga.octo(m, "z", FRONT + .2, FRONT + .7, 12.8, 8.2, .7, silver, end_b=ga.disc(silver, (110, 112, 118), mark=(40, 40, 44)))
    # Feet
    for x, z in ((2.2, 3.2), (13.8, 3.2), (2.2, 12.2), (13.8, 12.2)):
        m.box([x - .6, 0, z - .6], [x + .6, CY0, z + .6], {"*": (24, 22, 20)})
    # The rabbit ears: a dome, two rods spread apart
    ga.octo(m, "y", CY1, CY1 + .9, 8, 6.5, 1.4, (34, 34, 38), end_b=ga.disc((50, 50, 56), (30, 30, 34)))
    rod = {"*": ga.lengthwise((120, 124, 132), (240, 242, 248))}
    for angle in (22.5, -22.5):
        m.box([7.85, CY1 + .6, 6.35], [8.15, CY1 + 9.5, 6.65], rod, rotation={"origin": [8, CY1 + .6, 6.5], "axis": "z", "angle": angle})
    # Cut out, so the round knobs' caps have no square corners
    ga.write_block(m, ga.BLOCK_DISPLAY, render_type="minecraft:cutout")
    # Its states: facing four ways, on or off (the screen itself is drawn by the game)
    variants = {}
    for f, y in (("south", 0), ("west", 90), ("north", 180), ("east", 270)):
        for lit in ("false", "true"):
            variants[f"facing={f},lit={lit}"] = {"model": "eraphones:block/television", "y": y} if y else {"model": "eraphones:block/television"}
    kit.write_json("blockstates/television.json", {"variants": variants})


# ------------------------------------------------------------------------------------------------ the music box

def music_box():
    m = Model("boite_a_musique", size=512)
    gold = (206, 164, 74)

    def side(tile):
        ga.wood(tile, dark=(92, 30, 26), light=(140, 52, 40), seed=21)
        w, h = tile.size
        d = ImageDraw.Draw(tile)
        d.rectangle((0, 0, w - 1, 3), fill=ga.shade(gold, 1))
        d.rectangle((0, h - 4, w - 1, h - 1), fill=ga.shade(gold, .8))
        # A little painted garland
        for k in range(6):
            cx = w * (k + .5) / 6
            d.ellipse((cx - 4, h / 2 - 4, cx + 4, h / 2 + 4), fill=(230, 210, 170, 255))
            d.ellipse((cx - 2, h / 2 - 2, cx + 2, h / 2 + 2), fill=(170, 40, 50, 255))

    def velvet(tile):
        w, h = tile.size
        vgrad(tile, (150, 20, 34), (96, 10, 20))
        d = ImageDraw.Draw(tile)
        d.rectangle((0, 0, w - 1, h - 1), outline=ga.shade(gold, .9), width=3)
        # The brass cylinder and comb seen through glass at the back
        d.rectangle((w * .12, h * .1, w * .88, h * .28), fill=ga.shade(gold, .95))
        for k in range(18):
            x = w * .14 + k * w * .04
            d.line((x, h * .1, x, h * .28), fill=ga.shade(gold, .6))

    m.box([4, 0, 5], [12, 4.5, 11], {"south": side, "north": side, "east": side, "west": side, "up": velvet, "down": (60, 20, 16)})

    def mirror(tile):
        w, h = tile.size
        vgrad(tile, (200, 206, 214), (140, 146, 156))
        d = ImageDraw.Draw(tile)
        d.rectangle((0, 0, w - 1, h - 1), outline=ga.shade(gold, 1), width=4)
        d.line((w * .2, h * .2, w * .5, h * .05), fill=(240, 244, 250, 255), width=3)

    # The lid, open, leaning back, a mirror inside it
    m.box([4, 4.5, 10.4], [12, 10.5, 11], {"south": mirror, "north": side, "east": side, "west": side, "up": side},
          rotation={"origin": [8, 4.5, 11], "axis": "x", "angle": -22.5})
    # The dancer on her stand
    white = (240, 236, 238)
    ga.octo(m, "y", 4.5, 4.9, 8, 7.4, .8, gold)
    ga.octo(m, "y", 4.9, 7.0, 8, 7.4, .22, (236, 206, 190))
    ga.octo(m, "y", 5.9, 6.2, 8, 7.4, .9, white)
    ga.octo(m, "y", 6.9, 7.5, 8, 7.4, .3, (236, 206, 190))
    # The key on the right side
    ga.octo(m, "x", 12, 12.9, 2.3, 8, .3, gold)
    m.box([12.9, 1.2, 7.8], [13.2, 3.4, 8.2], {"*": ga.facet(gold, 1)})
    ga.write_block(m, ga.BLOCK_DISPLAY)


# ------------------------------------------------------------------------------------------------ the doll

def doll():
    m = Model("poupee", size=512)
    pink, lace = (214, 150, 168), (246, 240, 236)

    def dress(tile):
        w, h = tile.size
        vgrad(tile, ga.shade(pink, 1.05), ga.shade(pink, .85))
        d = ImageDraw.Draw(tile)
        for y in range(6, h, 10):
            for x in range(4, w, 10):
                d.ellipse((x - 1.5, y - 1.5, x + 1.5, y + 1.5), fill=(236, 200, 210, 255))
        # Faded, a stain or two
        d.ellipse((w * .6, h * .5, w * .8, h * .65), fill=(198, 138, 150, 255))

    def hem(tile):
        w, h = tile.size
        vgrad(tile, lace, (220, 214, 210))
        d = ImageDraw.Draw(tile)
        for x in range(0, w, 6):
            d.pieslice((x, h - 8, x + 6, h + 4), 180, 360, fill=(255, 252, 250, 255))
        for x in range(3, w, 6):
            d.ellipse((x - 1, h * .4, x + 1, h * .4 + 2), fill=(200, 190, 186, 255))

    skin = (238, 224, 212)
    m.box([4.6, 0, 5.4], [11.4, 3.2, 10.6], {"*": dress, "down": hem})
    m.box([4.4, 0, 5.2], [11.6, .8, 10.8], {"*": hem})
    m.box([5.8, 3.2, 6.4], [10.2, 8.9, 9.4], {"*": dress})
    m.box([6.4, 8.6, 6.8], [9.6, 9.4, 9.0], {"*": hem})                      # the collar
    for x0 in (4.8, 10.2):
        m.box([x0, 4.4, 6.8], [x0 + 1, 8.6, 8.6], {"*": dress})               # sleeves
        m.box([x0 + .1, 3.4, 7.1], [x0 + .9, 4.4, 8.3], {"*": skin})          # hands
    for x0 in (6.2, 8.4):
        m.box([x0, .2, 10.6], [x0 + 1.4, 1.5, 12.4], {"*": (250, 248, 246)}) # stockings
        m.box([x0 - .05, .1, 12.4], [x0 + 1.45, 1.6, 13.4], {"*": ga.facet((24, 22, 24), 1.1)})  # shoes
    ga.write_block(m, ga.BLOCK_DISPLAY)
    doll_head()


def doll_head():
    """The head's picture, as the game folds it round the head: front, back, right, left; top, bottom (64 px each)."""
    S = 4
    c = 64 * S
    img = Image.new("RGBA", (c * 4, c * 2), (0, 0, 0, 0))
    skin = (240, 228, 218)
    hair_dark, hair = (72, 34, 22), (122, 58, 32)

    def hairs(d, x0, y0, w, h, seed):
        rng = np.random.default_rng(seed)
        d.rectangle((x0, y0, x0 + w, y0 + h), fill=hair + (255,))
        for _ in range(int(w * h / 900)):
            cx, cy = x0 + rng.uniform(0, w), y0 + rng.uniform(0, h)
            r = rng.uniform(14, 26)
            d.arc((cx - r, cy - r, cx + r, cy + r), rng.uniform(0, 360), rng.uniform(0, 360) + 200, fill=hair_dark + (255,), width=S * 2)

    d = ImageDraw.Draw(img)
    # Front: the face
    d.rectangle((0, 0, c, c), fill=skin + (255,))
    for cx in (c * .3, c * .7):
        d.ellipse((cx - c * .07, c * .62 - c * .04, cx + c * .07, c * .62 + c * .04), fill=(240, 160, 160, 140))
    for cx in (c * .32, c * .68):
        d.ellipse((cx - c * .1, c * .4, cx + c * .1, c * .56), fill=(250, 250, 250, 255))
        d.ellipse((cx - c * .075, c * .405, cx + c * .075, c * .555), fill=(26, 24, 30, 255))
        d.ellipse((cx - c * .03, c * .42, cx + c * .0, c * .45), fill=(255, 255, 255, 255))
        for k in range(5):
            a = math.radians(200 + k * 35)
            d.line((cx + math.cos(a) * c * .1, c * .48 + math.sin(a) * c * .08, cx + math.cos(a) * c * .14, c * .48 + math.sin(a) * c * .12),
                   fill=(30, 20, 20, 255), width=S)
    d.ellipse((c * .44, c * .72, c * .56, c * .8), fill=(170, 30, 40, 255))
    d.line((c * .5, c * .58, c * .49, c * .66), fill=(200, 170, 160, 255), width=S)
    # A crack across the porcelain
    d.line([(c * .7, 0), (c * .66, c * .14), (c * .74, c * .24), (c * .69, c * .36)], fill=(110, 96, 90, 255), width=S)
    hairs(d, 0, 0, c, c * .22, 1)
    hairs(d, 0, 0, c * .1, c * .7, 2)
    hairs(d, c * .9, 0, c * .1, c * .7, 3)
    # Back, sides, top: hair, a red ribbon at the back
    hairs(d, c, 0, c, c, 4)
    d.polygon([(c * 1.35, c * .3), (c * 1.5, c * .4), (c * 1.65, c * .3), (c * 1.65, c * .5), (c * 1.5, c * .4), (c * 1.35, c * .5)],
              fill=(180, 20, 30, 255))
    hairs(d, c * 2, 0, c, c, 5)
    hairs(d, c * 3, 0, c, c, 6)
    hairs(d, 0, c, c, c, 7)
    d.rectangle((c, c, c * 2, c * 2), fill=skin + (255,))
    img = img.filter(ImageFilter.GaussianBlur(S * .35)).resize((64 * 4, 64 * 2), Image.LANCZOS)
    kit.save(img, "textures/block/poupee_tete.png")


# ------------------------------------------------------------------------------------------------ in hand

def balloon():
    """A red balloon on a string: a glossy egg shape over a little knot, the string hanging down."""
    m = Model("ballon_rouge")
    red = (206, 26, 30)
    profile = ((7.0, 7.5, .5), (7.5, 8.2, 1.6), (8.2, 9.2, 2.3), (9.2, 11.0, 2.75), (11.0, 12.8, 2.8), (12.8, 14.0, 2.45),
               (14.0, 14.9, 1.8), (14.9, 15.4, 1.0))
    for a0, a1, r in profile:
        ga.octo(m, "y", a0, a1, 8, 8, r, red)
    m.box([7.5, 6.5, 7.5], [8.5, 7.1, 8.5], {"*": ga.facet((170, 20, 24), 1)})
    m.box([7.95, 0, 7.95], [8.05, 6.5, 8.05], {"*": (236, 232, 226)})
    # A shine on its face
    m.box([6.0, 11.6, 10.79], [6.9, 13.2, 10.8], {"south": (255, 190, 186)})
    d = {
        "gui": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1.0] * 3},
        "firstperson_righthand": {"rotation": [-4, -20, 6], "translation": [-1, 6, -1], "scale": [.5] * 3},
        "firstperson_lefthand": {"rotation": [-4, 20, -6], "translation": [-1, 6, -1], "scale": [.5] * 3},
        "thirdperson_righthand": {"rotation": [72, 0, 0], "translation": [0, 0, 7], "scale": [.85] * 3},
        "thirdperson_lefthand": {"rotation": [72, 0, 0], "translation": [0, 0, 7], "scale": [.85] * 3},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [.5] * 3},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1.0] * 3},
        "head": {"rotation": [0, 0, 0], "translation": [0, 14, 0], "scale": [1.0] * 3},
    }
    ga.held(m, d)


def remote():
    """A television remote of the time: black, its rows of grey keys, a red one, a little window at the top."""
    m = Model("telecommande_mj")

    def front(tile):
        w, h = tile.size
        vgrad(tile, (40, 40, 44), (20, 20, 22))
        d = ImageDraw.Draw(tile)
        d.rounded_rectangle((6, 6, w - 7, 18), radius=3, fill=(60, 14, 18, 255))
        d.ellipse((w / 2 - 3, 10, w / 2 + 3, 15), fill=(255, 60, 50, 255))
        d.ellipse((w - 18, 24, w - 8, 34), fill=(200, 36, 30, 255))
        for r in range(6):
            for k in range(3):
                x = 8 + k * (w - 16) / 3
                y = 40 + r * 13
                d.rounded_rectangle((x + 1, y, x + (w - 16) / 3 - 2, y + 9), radius=2, fill=(150, 150, 156, 255))
        text(d, "VOXA", (w - text_width("VOXA", 1)) // 2, h - 12, (130, 130, 136, 255), 1)
        round_alpha(tile, 6)

    def back(tile):
        vgrad(tile, (34, 34, 38), (18, 18, 20))
        round_alpha(tile, 6)

    m.body(6.2, 2, 9.8, 14, 7.4, 8.6, .6, front, back, (14, 14, 16), (70, 70, 78))
    ga.held(m, ga.upright_display(1.0))


# ------------------------------------------------------------------------------------------------ sound

def music_box_tune(rate=48000):
    """A waltz in A minor of no one's, struck on a comb: each note a bright tine dying away, a bass note at each bar; the
    spring runs down at the end, slower and flatter, and stops."""
    melody = [(76, 2), (69, 1), (72, 1), (71, 1), (69, 1), (76, 2), (77, 1), (76, 3),
              (74, 2), (67, 1), (71, 1), (69, 1), (67, 1), (74, 2), (76, 1), (72, 3),
              (81, 2), (79, 1), (77, 1), (76, 1), (74, 1), (76, 2), (72, 1), (69, 3),
              (71, 1), (72, 1), (74, 1), (76, 2), (68, 1), (69, 3),
              (76, 2), (69, 1), (72, 1), (71, 1), (69, 1), (76, 2), (77, 1), (76, 3), (74, 2), (71, 1), (69, 3)]
    beat = .36
    total_beats = sum(b for _, b in melody)
    out = np.zeros(int(rate * 30))
    t = 0.0
    beats_done = 0

    def strike(at, midi, vol):
        f = 440 * 2 ** ((midi - 69) / 12)
        n = int(rate * 2.4)
        tt = np.arange(n) / rate
        tone = (np.sin(2 * np.pi * f * tt) + .45 * np.sin(2 * np.pi * f * 2.0 * tt) * np.exp(-tt * 9)
                + .22 * np.sin(2 * np.pi * f * 5.4 * tt) * np.exp(-tt * 22))
        env = np.exp(-tt * 2.6) * np.clip(tt / .002, 0, 1)
        k = int(at * rate)
        out[k:k + n] += (tone * env * vol)[:max(0, min(n, len(out) - k))]

    for i, (midi, b) in enumerate(melody):
        run_down = max(0, beats_done / total_beats - .7) / .3
        slow = 1 + run_down ** 2 * 1.6
        flat = -run_down ** 3 * .45
        strike(t, midi + flat, .5)
        if beats_done % 3 == 0:
            strike(t, midi - 24 + flat, .3)
        t += beat * b * slow
        beats_done += b
    end = int((t + 1.8) * rate)
    y = out[:end]
    # A small wooden box round it: a short, warm echo
    echo = np.zeros_like(y)
    for delay, g in ((.031, .28), (.047, .2), (.083, .12)):
        k = int(delay * rate)
        echo[k:] += y[:-k] * g
    y = y + echo
    return y / np.abs(y).max() * .85


def recipes():
    ga.shaped("television", ["PGP", "PRP", "P P"], {"P": "minecraft:dark_oak_planks", "G": "minecraft:glass_pane", "R": "minecraft:redstone"})


def main():
    television()
    recipes()
