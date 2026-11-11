"""Padlocks, keys, badges and the card reader (original designs, drawn here): a brass padlock with its steel shackle,
a brass key with a round bow and a paper tag, a white plastic access badge with a photo square and a magnetic
stripe, and a grey card reader with its slot and its light (amber waiting, green, red)."""
from PIL import ImageDraw, ImageFont

import bike_assets as ba
import gear_assets as ga
import kit
from models import Model, vgrad, round_alpha

BRASS = (212, 168, 74)
STEEL = (176, 180, 188)


def brass(tile):
    w, h = tile.size
    vgrad(tile, ga.shade(BRASS, 1.18), ga.shade(BRASS, .8))
    d = ImageDraw.Draw(tile)
    d.rectangle((0, 0, w - 1, h - 1), outline=ga.shade(BRASS, .6), width=1)


def lock_front(tile):
    brass(tile)
    w, h = tile.size
    d = ImageDraw.Draw(tile)
    # The keyhole
    d.ellipse((w * .4, h * .3, w * .6, h * .5), fill=(40, 30, 16, 255))
    d.polygon([(w * .45, h * .45), (w * .55, h * .45), (w * .58, h * .72), (w * .42, h * .72)], fill=(40, 30, 16, 255))
    round_alpha(tile, 3)


def padlock():
    m = Model("cadenas", 512, 24)
    m.box([5.5, 2, 7.2], [10.5, 7.5, 8.8], {"south": lock_front, "north": lock_front, "east": brass, "west": brass, "up": brass, "down": brass})
    # The shackle: two legs and the curve over the top
    for x in (6.2, 9.2):
        m.box([x, 7.5, 7.6], [x + .6, 10.2, 8.4], {"*": ga.facet(STEEL, 1.05)})
    m.box([6.2, 10.2, 7.6], [9.8, 10.8, 8.4], {"*": ga.facet(STEEL, 1.1)})
    d = {"gui": {"rotation": [15, -25, 0], "translation": [0, 0, 0], "scale": [1.3, 1.3, 1.3]},
         "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [.5, .5, .5]},
         "fixed": {"rotation": [0, 0, 0], "translation": [0, -2.5, 0], "scale": [1, 1, 1]},
         "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 0, 1], "scale": [.5, .5, .5]},
         "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 0, 1], "scale": [.5, .5, .5]},
         "firstperson_righthand": {"rotation": [0, -20, 0], "translation": [1, 3, 0], "scale": [.6, .6, .6]},
         "firstperson_lefthand": {"rotation": [0, 20, 0], "translation": [-1, 3, 0], "scale": [.6, .6, .6]}}
    ga.held(m, d)
    ga.shaped("cadenas", [" I ", "I I", "GGG"], {"I": "minecraft:iron_nugget", "G": "minecraft:gold_ingot"})


def key():
    m = Model("cle", 512, 28)
    b = ga.facet(BRASS, 1.0)
    # The round bow with its hole, the shank, the bit's teeth
    ga.octo(m, "z", 7.6, 8.4, 4.2, 8.0, 1.8, BRASS, end_a=ga.disc(BRASS, ga.shade(BRASS, .7)[:3]), end_b=ga.disc(BRASS, ga.shade(BRASS, .7)[:3]))
    m.box([5.9, 7.6, 7.75], [12.6, 8.4, 8.25], {"*": b})
    m.box([10.6, 6.6, 7.75], [11.2, 7.6, 8.25], {"*": b})
    m.box([11.8, 6.2, 7.75], [12.6, 7.6, 8.25], {"*": b})
    # The paper tag on its string
    m.box([1.4, 9.2, 7.9], [3.4, 12.0, 8.1], {"*": (236, 228, 204)})
    d = {"gui": {"rotation": [0, 0, -30], "translation": [0, 0, 0], "scale": [1.2, 1.2, 1.2]},
         "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [.5, .5, .5]},
         "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
         "thirdperson_righthand": {"rotation": [0, 90, -35], "translation": [0, 1, 1], "scale": [.5, .5, .5]},
         "thirdperson_lefthand": {"rotation": [0, -90, 35], "translation": [0, 1, 1], "scale": [.5, .5, .5]},
         "firstperson_righthand": {"rotation": [0, -100, 25], "translation": [1, 3, 0], "scale": [.55, .55, .55]},
         "firstperson_lefthand": {"rotation": [0, 100, -25], "translation": [-1, 3, 0], "scale": [.55, .55, .55]}}
    ga.held(m, d)


def badge_face(tile):
    w, h = tile.size
    vgrad(tile, (246, 246, 242), (226, 228, 230))
    d = ImageDraw.Draw(tile)
    d.rectangle((0, 0, w, h * .22), fill=(40, 90, 160, 255))
    font = ImageFont.truetype(ba.kit_font(), max(8, int(h * .14)))
    d.text((w * .5, h * .11), "ACCÈS", font=font, fill=(255, 255, 255, 255), anchor="mm")
    d.rectangle((w * .08, h * .32, w * .38, h * .82), fill=(170, 176, 186, 255), outline=(120, 124, 132, 255))
    d.ellipse((w * .15, h * .38, w * .31, h * .58), fill=(120, 126, 136, 255))
    d.rectangle((w * .12, h * .6, w * .34, h * .8), fill=(120, 126, 136, 255))
    for k in range(3):
        d.line((w * .46, h * (.4 + k * .14), w * .9, h * (.4 + k * .14)), fill=(150, 156, 166, 255), width=2)
    round_alpha(tile, 4)


def badge_back(tile):
    w, h = tile.size
    vgrad(tile, (240, 240, 236), (222, 224, 226))
    d = ImageDraw.Draw(tile)
    d.rectangle((0, h * .2, w, h * .38), fill=(30, 30, 34, 255))
    round_alpha(tile, 4)


def badge():
    m = Model("badge", 512, 32)
    m.box([3, 5, 7.85], [13, 11.2, 8.15], {"south": badge_face, "north": badge_back, "*": (230, 230, 228)})
    d = {"gui": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1.3, 1.3, 1.3]},
         "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [.5, .5, .5]},
         "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
         "thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [0, 1, 1], "scale": [.5, .5, .5]},
         "thirdperson_lefthand": {"rotation": [0, -90, 0], "translation": [0, 1, 1], "scale": [.5, .5, .5]},
         "firstperson_righthand": {"rotation": [0, -160, 10], "translation": [1, 3, 0], "scale": [.6, .6, .6]},
         "firstperson_lefthand": {"rotation": [0, 160, -10], "translation": [-1, 3, 0], "scale": [.6, .6, .6]}}
    ga.held(m, d)
    ga.shaped("badge", ["P", "R"], {"P": "minecraft:paper", "R": "minecraft:redstone"})


def reader_front(light):
    colors = [(230, 150, 40), (60, 230, 90), (240, 50, 40)]

    def p(tile):
        w, h = tile.size
        vgrad(tile, (92, 98, 106), (64, 68, 76))
        d = ImageDraw.Draw(tile)
        d.rounded_rectangle((w * .1, h * .08, w * .9, h * .4), radius=3, fill=(30, 32, 36, 255))
        d.ellipse((w * .38, h * .16, w * .62, h * .32), fill=colors[light] + (255,))
        font = ImageFont.truetype(ba.kit_font(), max(6, int(h * .08)))
        d.text((w * .5, h * .5), "BADGE", font=font, fill=(200, 204, 210, 255), anchor="mm")
        for k in range(3):
            d.line((w * .3, h * (.62 + k * .1), w * .7, h * (.62 + k * .1)), fill=(120, 126, 134, 255), width=1)
        round_alpha(tile, 3)
    return p


def reader():
    side = ga.facet((80, 86, 94), 1.0)
    for light in range(3):
        m = Model("lecteur_carte" + ["", "_vert", "_rouge"][light], 512, 28)
        m.box([5, 4, 0], [11, 13, 1.6], {"south": reader_front(light), "east": side, "west": side, "up": side, "down": side, "north": side})
        # The slot down its side
        m.box([10.6, 6, 1.6], [11, 11.5, 1.9], {"*": (24, 24, 28)})
        if light:
            m.elements[0]["neoforge_data"] = {"block_light": 12, "sky_light": 12}
        kit.save(m.img, f"textures/block/{m.name}.png")
        tex = f"eraphones:block/{m.name}"
        kit.write_json(f"models/block/{m.name}.json", {
            "render_type": "minecraft:cutout", "textures": {"t": tex, "particle": tex},
            "elements": [dict(e, faces={k: dict(v, texture="#t") for k, v in e["faces"].items()}) for e in m.elements],
            "display": ga.BLOCK_DISPLAY})
    kit.write_json("models/item/lecteur_carte.json", {"parent": "eraphones:block/lecteur_carte"})
    variants = {}
    for f, y in (("south", 0), ("west", 90), ("north", 180), ("east", 270)):
        for light in range(3):
            v = {"model": "eraphones:block/lecteur_carte" + ["", "_vert", "_rouge"][light]}
            if y:
                v["y"] = y
            variants[f"facing={f},voyant={light}"] = v
    kit.write_json("blockstates/lecteur_carte.json", {"variants": variants})
    ga.write_data("loot_table/blocks/lecteur_carte.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "eraphones:lecteur_carte"}]}]})


def main():
    padlock()
    key()
    badge()
    reader()


if __name__ == "__main__":
    main()
