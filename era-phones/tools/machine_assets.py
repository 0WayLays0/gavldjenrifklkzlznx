"""The answering machine (original design, drawn here): a low beige box, two little cassettes behind smoked windows
on top (the greeting and the messages), a red LED counter, a row of buttons (play, rewind, erase, the big red message
key), a speaker grille, the maker's name; its message light dark, or glowing red while new messages wait. And its
beep."""
import numpy as np
from PIL import Image, ImageDraw, ImageFont

import bike_assets as ba
import gear_assets as ga
import kit
from models import Model, vgrad, round_alpha, text, text_width

NAME = "repondeur"
BEIGE = (214, 204, 182)
LIT = {"block_light": 12, "sky_light": 12}


def top(tile):
    """The top: beige plastic, two smoked cassette windows with their reels, the counter, the maker's name."""
    w, h = tile.size
    vgrad(tile, ga.shade(BEIGE, 1.06), ga.shade(BEIGE, .92))
    d = ImageDraw.Draw(tile)
    for k in range(2):
        x0 = w * (.06 + k * .3)
        x1 = x0 + w * .26
        y0, y1 = h * .18, h * .62
        d.rounded_rectangle((x0, y0, x1, y1), radius=3, fill=(40, 36, 34, 255), outline=(150, 142, 126, 255), width=2)
        for c in (.3, .7):
            cx, cy = x0 + (x1 - x0) * c, (y0 + y1) / 2
            r = (y1 - y0) * .22
            d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(210, 206, 196, 255))
            d.ellipse((cx - r * .4, cy - r * .4, cx + r * .4, cy + r * .4), fill=(60, 56, 52, 255))
        d.text(((x0 + x1) / 2, y1 + h * .08), ("ANNONCE", "MESSAGES")[k], font=ImageFont.truetype(ba.kit_font(), max(8, int(h * .08))),
               fill=(110, 100, 86, 255), anchor="mm")
    # The speaker grille
    for y in range(int(h * .2), int(h * .62), 4):
        d.line((w * .68, y, w * .94, y), fill=(120, 112, 98, 255), width=2)
    text(d, "VOXA", int(w * .81 - text_width("VOXA", 2) / 2), int(h * .74), (120, 108, 90, 255), 2)
    round_alpha(tile, 6)


def front(lit):
    """The front edge: the red counter, the buttons, the message light."""
    def p(tile):
        w, h = tile.size
        vgrad(tile, ga.shade(BEIGE, .98), ga.shade(BEIGE, .82))
        d = ImageDraw.Draw(tile)
        d.rounded_rectangle((w * .05, h * .2, w * .22, h * .8), radius=2, fill=(30, 8, 8, 255))
        font = ImageFont.truetype(ba.kit_font(), max(8, int(h * .5)))
        d.text((w * .135, h * .5), "88" if not lit else "01", font=font, fill=(255, 60, 40, 255) if lit else (90, 24, 20, 255), anchor="mm")
        for k, c in enumerate(((70, 70, 76), (70, 70, 76), (70, 70, 76), (200, 40, 34))):
            x0 = w * (.3 + k * .13)
            d.rounded_rectangle((x0, h * .28, x0 + w * .1, h * .72), radius=2, fill=c + (255,), outline=(40, 40, 44, 255), width=1)
        d.ellipse((w * .86, h * .34, w * .92, h * .66), fill=(255, 40, 30, 255) if lit else (90, 20, 18, 255))
    return p


def machine(lit):
    m = Model(NAME + ("_message" if lit else ""), 1024, 24)
    side = ga.facet(BEIGE, .85)
    m.box([2.5, 0, 4.5], [13.5, 3.2, 11.5], {"up": top, "south": front(lit), "north": side, "east": side, "west": side,
                                             "down": (60, 56, 50)})
    # The handset-shaped lid hinge, a cord at the back
    m.box([2.8, 3.2, 4.8], [13.2, 3.5, 5.6], {"*": ga.facet(ga.shade(BEIGE, .9), 1.0)})
    m.box([7.6, .5, 3.2], [8.4, 1.0, 4.5], {"*": (40, 40, 44)})
    # Feet
    for x, z in ((3.3, 5.3), (12.7, 5.3), (3.3, 10.7), (12.7, 10.7)):
        m.box([x - .4, -.01, z - .4], [x + .4, 0, z + .4], {"down": (30, 30, 30)})
    if lit:
        # The message light glowing
        m.box([12.0, 1.1, 11.5], [12.7, 2.0, 11.62], {"south": (255, 50, 36)})
        m.elements[-1]["neoforge_data"] = LIT
        m.elements[-1]["shade"] = False
    return m


def beep(rate=48000):
    n = int(rate * .55)
    t = np.arange(n) / rate
    return np.sin(2 * np.pi * 1000 * t) * np.clip(t / .005, 0, 1) * np.clip((.5 - t) / .005, 0, 1) * .6


def main():
    off, on = machine(False), machine(True)
    for m in (off, on):
        kit.save(m.img, f"textures/block/{m.name}.png")
        tex = f"eraphones:block/{m.name}"
        kit.write_json(f"models/block/{m.name}.json", {
            "render_type": "minecraft:cutout", "textures": {"t": tex, "particle": tex},
            "elements": [dict(e, faces={k: dict(v, texture="#t") for k, v in e["faces"].items()}) for e in m.elements],
            "display": ga.BLOCK_DISPLAY})
    kit.write_json(f"models/item/{NAME}.json", {"parent": f"eraphones:block/{NAME}"})
    variants = {}
    for f, y in (("south", 0), ("west", 90), ("north", 180), ("east", 270)):
        for new in ("false", "true"):
            v = {"model": f"eraphones:block/{NAME}" + ("_message" if new == "true" else "")}
            if y:
                v["y"] = y
            variants[f"facing={f},nouveau={new}"] = v
    kit.write_json(f"blockstates/{NAME}.json", {"variants": variants})
    ga.write_data(f"loot_table/blocks/{NAME}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": f"eraphones:{NAME}"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    ga.shaped(NAME, ["PRP", "ICI"], {"P": "minecraft:white_dye", "R": "minecraft:redstone", "I": "minecraft:iron_ingot",
                                     "C": "eraphones:cassette"})


if __name__ == "__main__":
    main()
