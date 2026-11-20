#!/usr/bin/env python3
"""Trades' art: the Medical Table (a low oak table under a linen cloth marked with a red cross, a roll of bandage, a
jar of salve and a phial on it), its models, blockstate, loot table and recipe; the Alembic (a copper still on a small brick furnace: the furnace with its fire mouth, the riveted
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


# ---------------------------------------------------------------- the medical table

WOOD = [(70, 44, 26), (98, 64, 38), (128, 86, 52), (154, 108, 66), (178, 132, 84)]
LINEN = [(176, 172, 162), (206, 202, 192), (230, 228, 220), (246, 245, 240)]
CROSS = [(150, 24, 30), (196, 40, 44), (226, 70, 70)]


def oak_texture():
    """16 x 16: the table's oak, its grain running along, a knot, the planks' seam darker."""
    rnd = random.Random(4300)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        band = 2 + (1 if (y // 2) % 3 == 0 else 0) - (1 if y in (7, 15) else 0)
        for x in range(16):
            level = band + (1 if rnd.random() < .12 else 0) - (1 if rnd.random() < .1 else 0)
            img.putpixel((x, y), WOOD[max(0, min(4, level))] + (255,))
    for x in range(16):
        img.putpixel((x, 7), WOOD[0] + (255,))
        img.putpixel((x, 15), WOOD[0] + (255,))
    for (x, y) in ((11, 3), (12, 3), (11, 4)):
        img.putpixel((x, y), WOOD[0] + (255,))
    return img


def cloth_texture(top=True):
    """16 x 16: white linen, a fine weave and soft folds; {top}: a red cross in its middle; else its hanging hem."""
    rnd = random.Random(4400 if top else 4401)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            fold = 2 + (1 if (x + y // 3) % 7 == 0 else 0) - (1 if (x * 3 + y) % 11 == 0 else 0)
            if (x + y) % 2 == 0 and rnd.random() < .3:
                fold -= 1
            img.putpixel((x, y), LINEN[max(0, min(3, fold))] + (255,))
    if top:
        for y in range(3, 13):
            for x in range(6, 10):
                img.putpixel((x, y), CROSS[1 if x < 9 else 0] + (255,))
        for y in range(6, 10):
            for x in range(3, 13):
                img.putpixel((x, y), CROSS[1 if y < 9 else 0] + (255,))
        for x in range(6, 9):
            img.putpixel((x, 3), CROSS[2] + (255,))
        for x in range(3, 6):
            img.putpixel((x, 6), CROSS[2] + (255,))
    else:
        for x in range(16):
            img.putpixel((x, 13), LINEN[0] + (255,))
            img.putpixel((x, 14), LINEN[3] + (255,))
            img.putpixel((x, 15), LINEN[1] + (255,))
    return img


def bandage_texture():
    """16 x 16: a roll of bandage: cream turns wound round, their edges shaded, the loose end hanging."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            ring = int(((x - 7.5) ** 2 + (y - 7.5) ** 2) ** .5)
            c = LINEN[3] if ring % 3 == 0 else LINEN[2] if ring % 3 == 1 else LINEN[1]
            img.putpixel((x, y), c + (255,))
    return img


def salve_texture():
    """16 x 16: a glazed earthenware jar of salve: its body dark brown, a cream glaze band, the cork lid."""
    img = Image.new("RGBA", (16, 16))
    body = [(84, 52, 34), (112, 72, 46), (140, 94, 60)]
    for y in range(16):
        for x in range(16):
            c = body[2] if x < 4 else body[1] if x < 12 else body[0]
            if 5 <= y <= 7:
                c = (226, 214, 180) if x < 12 else (196, 184, 150)
            if y < 3:
                c = (176, 140, 90) if x < 10 else (150, 116, 72)
            img.putpixel((x, y), c + (255,))
    return img


def phial_texture():
    """16 x 16: a phial of green tincture: clear glass at the neck, the liquid below, a highlight."""
    img = Image.new("RGBA", (16, 16))
    tincture = [(40, 110, 70), (70, 160, 100), (130, 210, 150)]
    for y in range(16):
        for x in range(16):
            if y < 5:
                c = GLASS[0] if x > 11 else GLASS[1]
            else:
                c = tincture[2] if y == 5 else tincture[1] if x < 11 else tincture[0]
            img.putpixel((x, y), c + (255,))
    for y in range(1, 15):
        img.putpixel((3, y), GLASS[2] + (255,))
    return img


def medical_table_model():
    def el(frm, to, faces, rot=None):
        out = {"from": frm, "to": to, "faces": {side: {"texture": tex, **({"uv": uv} if uv else {})} for side, (tex, uv) in faces.items()}}
        if rot:
            out["rotation"] = rot
        return out

    def all6(tex, uv=None, top=None):
        f = {side: (tex, uv) for side in ("north", "south", "east", "west", "down", "up")}
        if top:
            f["up"] = (top, None)
        return f

    legs = [el([x, 0, z], [x + 2, 9.5, z + 2], all6("#oak", [0, 0, 2, 9.5])) for x, z in ((1, 1), (13, 1), (1, 13), (13, 13))]
    return {
        "parent": "block/block",
        "render_type": "minecraft:cutout",
        "textures": {"particle": "arcanes_oublies:block/medical_oak", "oak": "arcanes_oublies:block/medical_oak",
                     "cloth": "arcanes_oublies:block/medical_cloth", "hem": "arcanes_oublies:block/medical_cloth_side",
                     "bandage": "arcanes_oublies:block/medical_bandage", "salve": "arcanes_oublies:block/medical_salve",
                     "phial": "arcanes_oublies:block/medical_phial"},
        "elements": legs + [
            # A stretcher between the legs, the top board
            el([2, 3, 1.5], [14, 4.5, 2.5], all6("#oak", [0, 0, 12, 1.5])),
            el([2, 3, 13.5], [14, 4.5, 14.5], all6("#oak", [0, 0, 12, 1.5])),
            el([0, 9.5, 0], [16, 11, 16], all6("#oak")),
            # The linen over it, hanging down the front and back
            el([0.5, 11, 0.5], [15.5, 11.5, 15.5], all6("#cloth", top="#cloth")),
            el([0.5, 7, 0], [15.5, 11.5, 0.5], all6("#hem", [0, 7, 15, 11.5])),
            el([0.5, 7, 15.5], [15.5, 11.5, 16], all6("#hem", [0, 7, 15, 11.5])),
            # A roll of bandage, a jar of salve, a phial with its cork
            el([2, 11.5, 9.5], [6.5, 14.5, 12.5], {"north": ("#bandage", None), "south": ("#bandage", None), "east": ("#hem", [0, 4, 3, 7]),
                                                     "west": ("#hem", [0, 4, 3, 7]), "up": ("#hem", [0, 0, 4.5, 3]), "down": ("#hem", None)}),
            el([10, 11.5, 2.5], [13, 14.5, 5.5], all6("#salve")),
            el([10.5, 11.5, 9], [12.5, 15, 11], all6("#phial")),
            el([11, 15, 9.5], [12, 16, 10.5], all6("#oak", [0, 0, 1, 1])),
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
    g.save(oak_texture(), "textures/block/medical_oak.png")
    g.save(cloth_texture(), "textures/block/medical_cloth.png")
    g.save(cloth_texture(top=False), "textures/block/medical_cloth_side.png")
    g.save(bandage_texture(), "textures/block/medical_bandage.png")
    g.save(salve_texture(), "textures/block/medical_salve.png")
    g.save(phial_texture(), "textures/block/medical_phial.png")
    g.write_json("models/block/medical_table.json", medical_table_model())
    g.write_json("models/item/medical_table.json", {"parent": "arcanes_oublies:block/medical_table"})
    g.write_json("blockstates/medical_table.json", {"variants": {
        f"facing={f}": {"model": "arcanes_oublies:block/medical_table", "y": y} for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    write_data("loot_table/blocks/medical_table.json", {"type": "minecraft:block", "pools": [{
        "rolls": 1, "entries": [{"type": "minecraft:item", "name": "arcanes_oublies:medical_table"}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    write_data("recipe/medical_table.json", {
        "type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["WRW", "PPP", "S S"],
        "key": {"W": {"item": "minecraft:white_wool"}, "R": {"item": "minecraft:red_dye"}, "P": {"tag": "minecraft:planks"},
                "S": {"item": "minecraft:stick"}},
        "result": {"id": "arcanes_oublies:medical_table", "count": 1}})


if __name__ == "__main__":
    main()
