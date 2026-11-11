"""The personal cassette player (original design, drawn here): a slim metal-blue box, a smoked window on its front
showing the cassette's reels when one is in, the maker's name, a red light, four keys on top, a volume wheel on the
side, a belt clip behind, the headphones' plug. And the headphones worn while listening: a thin metal band over the
head and two round pads of orange foam on the ears."""
from PIL import ImageDraw, ImageFont

import bike_assets as ba
import gear_assets as ga
import kit
from models import Model, vgrad, round_alpha

BODY = (64, 92, 150)
METAL = (182, 186, 194)


def body_side(tile):
    vgrad(tile, ga.shade(BODY, 1.15), ga.shade(BODY, .85))


def front(loaded):
    """The front: brushed blue metal, the window (the reels and the label when a cassette is in), the name, a light."""
    def p(tile):
        w, h = tile.size
        vgrad(tile, ga.shade(BODY, 1.2), ga.shade(BODY, .9))
        d = ImageDraw.Draw(tile)
        for y in range(0, h, 2):
            d.line((0, y, w, y), fill=ga.shade(BODY, 1.05 if y % 4 else .98))
        # The window
        x0, y0, x1, y1 = w * .1, h * .14, w * .9, h * .58
        d.rounded_rectangle((x0 - 2, y0 - 2, x1 + 2, y1 + 2), radius=4, fill=(30, 34, 44, 255))
        d.rounded_rectangle((x0, y0, x1, y1), radius=3, fill=(22, 24, 30, 255))
        if loaded:
            # The cassette behind the glass: its label, its two reels with tape wound on
            d.rectangle((x0 + 3, y0 + 3, x1 - 3, y1 - 3), fill=(226, 220, 204, 255))
            d.rectangle((x0 + 3, y0 + 3, x1 - 3, y0 + (y1 - y0) * .28), fill=(200, 60, 50, 255))
            for c in (.32, .68):
                cx, cy = x0 + (x1 - x0) * c, y0 + (y1 - y0) * .62
                r = (y1 - y0) * .22
                d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(60, 40, 30, 255))
                d.ellipse((cx - r * .45, cy - r * .45, cx + r * .45, cy + r * .45), fill=(240, 240, 240, 255))
                d.ellipse((cx - r * .2, cy - r * .2, cx + r * .2, cy + r * .2), fill=(40, 40, 40, 255))
        font = ImageFont.truetype(ba.kit_font(), max(8, int(h * .1)))
        d.text((w / 2, h * .7), "VOXA", font=font, fill=(236, 236, 240, 255), anchor="mm")
        small = ImageFont.truetype(ba.kit_font(), max(6, int(h * .055)))
        d.text((w / 2, h * .8), "STEREO  CASSETTE", font=small, fill=(196, 204, 226, 255), anchor="mm")
        d.ellipse((w * .78, h * .86, w * .86, h * .92), fill=(230, 40, 30, 255))
        round_alpha(tile, 4)
    return p


def keys_top(tile):
    """The four keys on top: play, stop, wind back, wind on."""
    w, h = tile.size
    vgrad(tile, ga.shade(BODY, 1.05), ga.shade(BODY, .9))
    d = ImageDraw.Draw(tile)
    for k, c in enumerate(((220, 222, 228), (220, 222, 228), (220, 222, 228), (210, 60, 50))):
        x0 = w * (.08 + k * .22)
        d.rectangle((x0, h * .2, x0 + w * .18, h * .8), fill=c + (255,), outline=(60, 60, 70, 255))


def walkman(loaded):
    m = Model("baladeur" + ("_charge" if loaded else ""), 1024, 24)
    m.box([4.6, 1.0, 6.8], [11.4, 12.0, 9.4], {"south": front(loaded), "north": body_side, "east": body_side, "west": body_side,
                                              "up": keys_top, "down": body_side})
    # Keys standing up from the top
    for k in range(4):
        x = 5.2 + k * 1.55
        c = (210, 60, 50) if k == 3 else (222, 224, 230)
        m.box([x, 12.0, 7.4], [x + 1.2, 12.5, 8.8], {"*": ga.facet(c, 1.0)})
    # The volume wheel on the side, the belt clip behind, the headphones' plug
    ga.octo(m, "x", 11.4, 11.9, 9.0, 8.1, 1.0, (40, 40, 46), end_b=ga.disc((90, 90, 96), (30, 30, 34)))
    m.box([6.2, 4.0, 6.3], [9.8, 10.5, 6.8], {"*": ga.facet(METAL, 1.0)})
    m.box([5.0, 12.0, 8.2], [5.6, 13.2, 8.8], {"*": ga.facet(METAL, 1.1)})
    return m


def headphones():
    """Round the head (which is 4 to 12 every way here): the band over it, the cups and their orange foam on the ears."""
    m = Model("casque", 512, 24)
    band = ga.facet(METAL, 1.05)
    foam = (236, 128, 40)
    for side in (-1, 1):
        # The foam pad on the ear, the small dark cup outside it
        a0, a1 = (2.4, 3.55) if side < 0 else (12.45, 13.6)
        ga.octo(m, "x", a0, a1, 7.6, 8.0, 1.9, foam, end_a=ga.disc(ga.shade(foam, 1.05)[:3], ga.shade(foam, .8)[:3]),
                end_b=ga.disc(ga.shade(foam, 1.05)[:3], ga.shade(foam, .8)[:3]))
        c0, c1 = (2.0, 2.4) if side < 0 else (13.6, 14.0)
        ga.octo(m, "x", c0, c1, 7.6, 8.0, 1.2, (50, 52, 58))
        # The band's side, up to the top
        x0, x1 = (2.7, 3.15) if side < 0 else (12.85, 13.3)
        m.box([x0, 9.4, 7.6], [x1, 12.9, 8.4], {"*": band})
    m.box([2.7, 12.6, 7.5], [13.3, 13.1, 8.5], {"*": band})
    m.write({})


def main():
    empty, loaded = walkman(False), walkman(True)
    d = {
        "gui": {"rotation": [15, -20, 0], "translation": [0, 0, 0], "scale": [1.1, 1.1, 1.1]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [.5, .5, .5]},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [.9, .9, .9]},
        "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 1, 1], "scale": [.45, .45, .45]},
        "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 1, 1], "scale": [.45, .45, .45]},
        "firstperson_righthand": {"rotation": [0, -160, 0], "translation": [1, 3, 0], "scale": [.5, .5, .5]},
        "firstperson_lefthand": {"rotation": [0, 160, 0], "translation": [-1, 3, 0], "scale": [.5, .5, .5]},
    }
    ga.held(empty, d)
    loaded.write(d)
    kit.write_json("models/item/baladeur_charge.json", {"parent": "eraphones:item/baladeur_charge_3d"})
    # With a cassette in it, it shows
    kit.write_json("models/item/baladeur.json", {"parent": "eraphones:item/baladeur_3d", "overrides": [
        {"predicate": {"eraphones:charge": 1}, "model": "eraphones:item/baladeur_charge"}]})
    headphones()
    ga.shaped("baladeur", ["SIS", "IRI", "IBI"], {"S": "minecraft:string", "I": "minecraft:iron_ingot", "R": "minecraft:redstone",
                                                  "B": "eraphones:piles"})


if __name__ == "__main__":
    main()
