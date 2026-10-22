#!/usr/bin/env python3
"""Ritual art: the Ritual Chalk (a stick of white chalk bound in violet cord, its tip glowing faintly).

Called by gen_assets.py (run that one). The rituals themselves are drawn in game.
"""
import gen_assets as g

CHALK = (238, 234, 222)
CHALK_SHADE = (196, 190, 176)
CHALK_DARK = (150, 144, 132)
CORD = (122, 70, 170)
CORD_LIGHT = (170, 116, 220)
GLOW = (200, 170, 255)
OUTLINE = (40, 36, 44)


def ritual_chalk_icon():
    return g.from_grid([
        "................",
        "............gg..",
        "...........gwwg.",
        "..........owwso.",
        ".........owwso..",
        "........owwso...",
        ".......occso....",
        "......oCcso.....",
        ".....owwso......",
        "....owwso.......",
        "...owwso........",
        "..owwso.........",
        "..owso..........",
        "..oso...........",
        "...o............",
        "................",
    ], {"o": OUTLINE, "w": CHALK, "s": CHALK_SHADE, "c": CORD, "C": CORD_LIGHT, "g": GLOW})


def main():
    g.save(ritual_chalk_icon(), "textures/item/ritual_chalk.png")
    g.write_json("models/item/ritual_chalk.json", {"parent": "item/handheld", "textures": {"layer0": "arcanes_oublies:item/ritual_chalk"}})


if __name__ == "__main__":
    main()
