"""The VHS camcorder (original design, drawn here): a long black shoulder camcorder, its big lens with a chrome ring
in front, the viewfinder tube on the left with its rubber eyecup, a carrying handle on top with the microphone at
its front, the cassette door on the side with its window, a red REC button, the maker's name in red."""
from PIL import ImageDraw, ImageFont

import bike_assets as ba
import gear_assets as ga
from models import Model, vgrad, round_alpha, text, text_width

BODY = (34, 34, 38)


def side(tile):
    """The right side: the cassette door, its little window, the maker's name, a red REC button."""
    w, h = tile.size
    vgrad(tile, ga.shade(BODY, 1.25), ga.shade(BODY, .85))
    d = ImageDraw.Draw(tile)
    d.rounded_rectangle((w * .18, h * .2, w * .72, h * .82), radius=3, outline=(70, 70, 76, 255), width=2)
    d.rounded_rectangle((w * .3, h * .36, w * .58, h * .56), radius=2, fill=(20, 20, 24, 255), outline=(110, 110, 116, 255), width=1)
    d.rectangle((w * .34, h * .42, w * .54, h * .5), fill=(70, 60, 50, 255))
    font = ImageFont.truetype(ba.kit_font(), max(8, int(h * .16)))
    d.text((w * .45, h * .7), "VOXA  VHS", font=font, fill=(220, 40, 34, 255), anchor="mm")
    d.ellipse((w * .8, h * .38, w * .9, h * .58), fill=(220, 30, 26, 255), outline=(90, 10, 10, 255), width=1)
    round_alpha(tile, 4)


def plain(tile):
    vgrad(tile, ga.shade(BODY, 1.2), ga.shade(BODY, .8))
    round_alpha(tile, 4)


def lens_glass(tile):
    w, h = tile.size
    d = ImageDraw.Draw(tile)
    d.ellipse((0, 0, w - 1, h - 1), fill=(170, 172, 180, 255))
    d.ellipse((w * .1, h * .1, w * .9, h * .9), fill=(12, 14, 22, 255))
    d.ellipse((w * .28, h * .28, w * .72, h * .72), fill=(36, 44, 90, 255))
    d.ellipse((w * .3, h * .22, w * .48, h * .4), fill=(220, 230, 255, 200))


def camcorder():
    m = Model("camescope", 1024, 16)
    # The long body, its lens toward south (forward)
    m.body(5.2, 4.0, 10.8, 9.6, 2.0, 13.0, .7, plain, plain, (20, 20, 22), (90, 90, 96), corners=False)
    m.box([10.8, 4.4, 3.5], [10.9, 9.2, 11.5], {"east": side})
    # The lens: a barrel, a chrome ring, the glass
    ga.octo(m, "z", 13.0, 15.0, 8, 7.0, 2.1, (26, 26, 30))
    ga.octo(m, "z", 15.0, 15.6, 8, 7.0, 2.3, (190, 192, 198), end_b=lens_glass)
    # The handle on top, the microphone at its front
    m.box([7.4, 9.6, 4.0], [8.6, 11.6, 4.8], {"*": ga.facet(BODY, 1.0)})
    m.box([7.4, 9.6, 10.6], [8.6, 11.6, 11.4], {"*": ga.facet(BODY, 1.0)})
    m.box([7.3, 11.6, 4.0], [8.7, 12.4, 12.4], {"*": ga.facet((24, 24, 26), 1.1)})
    ga.octo(m, "z", 12.4, 14.6, 8, 12.0, .55, (60, 60, 66), end_b=ga.disc((30, 30, 34), (14, 14, 16)))
    # The viewfinder tube on the left, its eyecup at the back
    ga.octo(m, "z", 6.0, 11.0, 4.3, 9.0, .9, (30, 30, 34))
    ga.octo(m, "z", 5.0, 6.0, 4.3, 9.0, 1.15, (16, 16, 18), end_a=ga.disc((10, 10, 12), (40, 40, 44)))
    # The shoulder pad under it
    m.box([5.6, 3.3, 5.0], [10.4, 4.0, 10.0], {"*": ga.facet((50, 46, 42), 1.0)})
    d = {
        "gui": {"rotation": [25, -135, 0], "translation": [0, 0, 0], "scale": [.85, .85, .85]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [.5, .5, .5]},
        "fixed": {"rotation": [0, 90, 0], "translation": [0, 0, 0], "scale": [.9, .9, .9]},
        # On the shoulder, its lens forward
        "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [-2.5, 9, 0], "scale": [.85, .85, .85]},
        "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [2.5, 9, 0], "scale": [.85, .85, .85]},
        "firstperson_righthand": {"rotation": [0, 180, 0], "translation": [2, 2, -2], "scale": [.55, .55, .55]},
        "firstperson_lefthand": {"rotation": [0, 180, 0], "translation": [-2, 2, -2], "scale": [.55, .55, .55]},
    }
    ga.held(m, d)
    ga.shaped("camescope", ["BGB", "BRB", "BIB"], {"B": "minecraft:black_dye", "G": "minecraft:glass_pane", "R": "minecraft:redstone",
                                                  "I": "minecraft:iron_ingot"})


def main():
    camcorder()


if __name__ == "__main__":
    main()
