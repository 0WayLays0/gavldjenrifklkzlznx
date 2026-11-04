#!/usr/bin/env python3
"""Raised ornaments for the grander spellbooks (the Boreal Codex, the Clockmaker's Codex): pieces added on top of the
3D book that gen_assets.book_models() writes, textured from four spare squares of the book's texture.

The book texture keeps the grimoire layout (gen_assets.book_models), drawn at 8 pixels a unit (128 x 128) so its art is
finer; the squares at u/v 12..16 x 12..16, unused by the plain book, hold the ornaments' materials:
METAL (12..14, 12..14), METAL_DARK (14..16, 12..14), JEWEL (12..14, 14..16) and GLOW (14..16, 14..16).
"""
import json
import os

import gen_assets as g

#: The book's measures (as gen_assets.book_models lays it out): covers' outer faces, its length and height.
BACK_X, FRONT_X = 5.8, 10.2
Z0, Z1, H = 3, 13, 7.6
#: The raised panel stands .15 proud of each cover.
BACK_OUT, FRONT_OUT = BACK_X - .15, FRONT_X + .15

METAL = (12, 12, 14, 14)
METAL_DARK = (14, 12, 16, 14)
JEWEL = (12, 14, 14, 16)
GLOW = (14, 14, 16, 16)


def face(region):
    return {"uv": list(region), "texture": "#book"}


def solid(frm, to, region, rotation=None):
    e = {"from": [round(v, 4) for v in frm], "to": [round(v, 4) for v in to],
         "faces": {k: face(region) for k in ("north", "south", "east", "west", "up", "down")}}
    if rotation:
        e["rotation"] = rotation
    return e


def on_cover(front, depth0, depth1):
    """The x span of a piece standing {depth0}..{depth1} proud of the panel of the front (or back) cover."""
    if front:
        return FRONT_OUT + depth0, FRONT_OUT + depth1
    return BACK_OUT - depth1, BACK_OUT - depth0


def turned(front, x0, x1, cy, cz, half_y, half_z, region, angle):
    """A plate on a cover, {half_y} x {half_z} round (cy, cz), turned {angle} degrees in the cover's plane."""
    rot = {"origin": [(x0 + x1) / 2, cy, cz], "axis": "x", "angle": angle} if angle else None
    return solid([x0, cy - half_y, cz - half_z], [x1, cy + half_y, cz + half_z], region, rot)


def add(name, elements):
    """Adds {elements} to the 3D model of book {name} (written by gen_assets.book_models just before)."""
    rel = f"models/item/{name}_3d.json"
    with open(os.path.join(g.ROOT, rel), encoding="utf-8") as f:
        data = json.load(f)
    data["elements"] += elements
    g.write_json(rel, data)
