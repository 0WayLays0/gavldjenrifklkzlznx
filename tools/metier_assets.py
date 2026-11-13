#!/usr/bin/env python3
"""Trades' art: the Alembic (a copper still on a small brick furnace: the furnace with its fire mouth, the riveted
copper pot and dome, the swan-neck pipe down to a glass receiver holding a violet distillate), its block and item
models, blockstate, loot table and recipe; and the minigames' art (metier_art.py).

Called by gen_assets.py (run that one). Pixel style, shaded in bands. Original art.
"""
import json
import os
import random

from PIL import Image

import gen_assets as g

DATA = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "data", "arcanes_oublies")

BRICK = [(92, 46, 34), (120, 60, 42), (146, 76, 52), (164, 92, 64)]
MORTAR = (70, 62, 58)
COPPER = [(96, 46, 24), (150, 78, 40), (196, 112, 60), (226, 150, 92), (250, 200, 150)]
VERDI = (88, 150, 120)
GLASS = [(150, 176, 196), (200, 222, 236), (238, 248, 255)]
LIQUID = [(86, 40, 130), (130, 70, 190), (176, 120, 236)]


def write_data(rel, data):
    path = os.path.join(DATA, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
        f.write("\n")
    print("wrote", os.path.relpath(path))


def brick_texture(front=False):
    """16 x 16: courses of dark fire-brick; {front}: with the fire's mouth, glowing."""
    rnd = random.Random(4101 if front else 4100)
    img = Image.new("RGBA", (16, 16), MORTAR + (255,))
    for row in range(4):
        y0 = row * 4
        offset = 0 if row % 2 == 0 else 4
        for bx in range(-1, 3):
            x0 = bx * 8 + offset
            shade = rnd.randint(0, 2)
            for y in range(y0, y0 + 3):
                for x in range(x0, x0 + 7):
                    if 0 <= x < 16:
                        c = BRICK[shade + (1 if y == y0 else 0)]
                        if rnd.random() < .12:
                            c = BRICK[max(0, shade - 1)]
                        img.putpixel((x, y), c + (255,))
    if front:
        # An arched mouth, the fire inside
        fire = [(255, 236, 160), (255, 190, 70), (240, 120, 30), (170, 50, 20)]
        for y in range(7, 15):
            for x in range(4, 12):
                dx = abs(x - 7.5)
                if y < 9 and dx > 2.5 + (y - 7):
                    continue
                depth = (y - 7) / 8
                c = fire[min(3, int(dx / 1.4 + (1 - depth) * 1.2))]
                img.putpixel((x, y), c + (255,))
        for x in range(3, 13):
            img.putpixel((x, 15), (40, 30, 28, 255))
        for y in range(7, 15):
            img.putpixel((3, y), (60, 40, 34, 255))
            img.putpixel((12, y), (60, 40, 34, 255))
    return img


def copper_texture():
    """16 x 16: hammered copper, lit from the top left, a band of rivets and a touch of verdigris."""
    rnd = random.Random(4200)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            t = (x + y) / 30
            level = 3 - int(t * 3.2)
            if rnd.random() < .18:
                level += rnd.choice((-1, 1))
            c = COPPER[max(0, min(4, level))]
            img.putpixel((x, y), c + (255,))
    for x in range(1, 16, 3):
        img.putpixel((x, 3), COPPER[4] + (255,))
        img.putpixel((x, 4), COPPER[0] + (255,))
        img.putpixel((x, 12), COPPER[4] + (255,))
        img.putpixel((x, 13), COPPER[0] + (255,))
    for x in range(16):
        img.putpixel((x, 0), COPPER[1] + (255,))
        img.putpixel((x, 15), COPPER[0] + (255,))
    for (x, y) in ((13, 9), (14, 9), (14, 10), (2, 14)):
        img.putpixel((x, y), VERDI + (255,))
    return img


def glass_texture():
    """16 x 16: the receiver's glass, its lower half holding the violet distillate, a highlight down one side."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            if y >= 7:
                c = LIQUID[2] if y == 7 else LIQUID[1] if x > 3 else LIQUID[2]
                if x > 12 or y > 13:
                    c = LIQUID[0]
            else:
                c = GLASS[0] if x > 11 else GLASS[1]
            img.putpixel((x, y), c + (255,))
    for y in range(1, 14):
        img.putpixel((2, y), GLASS[2] + (255,))
    for (x, y) in ((6, 10), (9, 12), (11, 9)):
        img.putpixel((x, y), (220, 190, 255, 255))
    return img


def alembic_model():
    def el(frm, to, tex, top=None, front=None, uv=None):
        faces = {}
        for side in ("north", "south", "east", "west", "down", "up"):
            t = tex
            if side == "up" and top:
                t = top
            if side == "north" and front:
                t = front
            f = {"texture": t}
            if uv:
                f["uv"] = uv
            faces[side] = f
        return {"from": frm, "to": to, "faces": faces}

    return {
        "parent": "block/block",
        "render_type": "minecraft:cutout",
        "textures": {"particle": "arcanes_oublies:block/alembic_copper", "brick": "arcanes_oublies:block/alembic_brick",
                     "front": "arcanes_oublies:block/alembic_front", "copper": "arcanes_oublies:block/alembic_copper",
                     "glass": "arcanes_oublies:block/alembic_glass"},
        "elements": [
            # The furnace
            el([1, 0, 1], [15, 6, 15], "#brick", front="#front"),
            el([0.5, 6, 0.5], [15.5, 7, 15.5], "#brick"),
            # The pot, its dome and neck
            el([3, 7, 3], [13, 12, 13], "#copper"),
            el([4, 12, 4], [12, 13.5, 12], "#copper"),
            el([5.5, 13.5, 5.5], [10.5, 14.5, 10.5], "#copper"),
            el([7, 14.5, 7], [9, 16, 9], "#copper"),
            # The swan neck down to the receiver
            el([9, 15, 7.5], [14.5, 16, 8.5], "#copper"),
            el([13.5, 10, 7.5], [14.5, 15, 8.5], "#copper"),
            # The receiver
            el([12, 7, 10.5], [15, 10, 13.5], "#glass"),
            el([13, 10, 11.5], [14, 11, 12.5], "#glass"),
            # A tap on the pot
            el([1.5, 9, 7.5], [3, 10, 8.5], "#copper"),
        ],
        "display": {
            "gui": {"rotation": [30, 225, 0], "scale": [0.625, 0.625, 0.625]},
            "ground": {"translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
            "fixed": {"scale": [0.5, 0.5, 0.5]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.4, 0.4, 0.4]},
            "firstperson_lefthand": {"rotation": [0, 225, 0], "scale": [0.4, 0.4, 0.4]},
        },
    }


def main():
    import metier_art
    metier_art.main()
    g.save(brick_texture(), "textures/block/alembic_brick.png")
    g.save(brick_texture(front=True), "textures/block/alembic_front.png")
    g.save(copper_texture(), "textures/block/alembic_copper.png")
    g.save(glass_texture(), "textures/block/alembic_glass.png")
    g.write_json("models/block/alembic.json", alembic_model())
    g.write_json("models/item/alembic.json", {"parent": "arcanes_oublies:block/alembic"})
    g.write_json("blockstates/alembic.json", {"variants": {
        f"facing={f}": {"model": "arcanes_oublies:block/alembic", "y": y} for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    write_data("loot_table/blocks/alembic.json", {"type": "minecraft:block", "pools": [{
        "rolls": 1, "entries": [{"type": "minecraft:item", "name": "arcanes_oublies:alembic"}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    write_data("recipe/alembic.json", {
        "type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": [" C ", "CGC", "BFB"],
        "key": {"C": {"item": "minecraft:copper_ingot"}, "G": {"item": "minecraft:glass_bottle"}, "B": {"item": "minecraft:bricks"},
                "F": {"item": "minecraft:furnace"}},
        "result": {"id": "arcanes_oublies:alembic", "count": 1}})


if __name__ == "__main__":
    main()
