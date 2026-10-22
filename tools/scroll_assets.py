"""The addon's spell scrolls: Iron's Spells' own scroll, recoloured per school when the game loads.

Iron's license forbids reusing its assets, so no scroll picture ships with the addon. Instead the block atlas gets a
"paletted_permutations" source (the way armor trims are coloured): the game itself recolours Iron's
irons_spellbooks:item/scroll into irons_spellbooks:item/scroll_arcanes_<school>, swapping each of its eleven colours
(KEY) for the school's (PALETTES). Only those colours ship with the addon. Called from gen_assets.main().
"""
import json
import os

from PIL import Image

import gen_assets as g

ASSETS = os.path.join(g.ROOT, "..")

#: The eleven colours of Iron's scroll, in the order the palettes follow: the paper (mid, light, highlight, shadow,
#: mid-dark), the writing (bright, mid, dark) and the rod ends (light, mid, dark).
KEY = ["b2a684", "dad2bc", "ebebeb", "847651", "968b6c", "524b39", "423c2d", "393325", "7c4b34", "5b3523", "492c1e"]

PALETTES = {
    # A sheet of gold leaf, the writing engraved in dark gold, white-gold rod ends
    "gold": ["d4a432", "e8bf48", "f8de78", "a87818", "bf8e25", "6b3e04", "5a3203", "472802", "fff6c8", "f4d060", "c08a1c"],
    # Charred paper, the writing glowing like embers
    "explosion": ["3d3330", "4a3e3a", "5c4d46", "241e1c", "302826", "ffd36a", "ff8a2a", "e0501a", "ffd36a", "ff8a2a", "c2401a"],
    # Old parchment written in violet ink
    "book": ["cdbb94", "e2d3ae", "f1e8cf", "a8936a", "b9a47a", "9a5cff", "7a3ee0", "5a28b0", "b88cff", "8a52e8", "5a28b0"],
    # Night-blue paper, the writing in sky-blue mana
    "errethys": ["1f3550", "27425f", "31526f", "142339", "1a2c44", "e6fbff", "87ceeb", "4fa3d8", "e6fbff", "87ceeb", "3d86c0"],
    # Tanned leather, steel rod ends
    "physical": ["8a5a36", "9c6a42", "b07c50", "5e3a20", "74492a", "f2dcc0", "d9b48a", "b88a5a", "e0e4e8", "a8b0b8", "6c757e"],
    # Black vellum, crimson writing, gold rod ends
    "boss": ["2a1a1c", "352022", "45292c", "160d0e", "1f1314", "ff6a78", "e0283c", "a01428", "ffe08a", "e0a83a", "a06a14"],
    # Grey stone, the runes glowing violet, silver rod ends
    "rune": ["6e6a64", "807b74", "948f87", "46433f", "5a5752", "f2e6ff", "b57bff", "7a3fd8", "e6eaee", "b2b9c1", "737b85"],
    # Ivory paper, the writing in three of the six inks (flame, tide, gale), gold rod ends
    "glyph": ["d9cfb2", "ebe3cb", "f8f4e8", "a89d80", "c0b597", "ff8a3a", "3f9bff", "4fd49a", "ffe08a", "d8a840", "9a6c1c"],
}


def _strip(colors):
    img = Image.new("RGBA", (len(colors), 1))
    for i, c in enumerate(colors):
        img.putpixel((i, 0), (int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16), 255))
    return img


def recolour(base, school):
    """What the game makes of Iron's scroll for a school (for previews: the game does it itself)."""
    mapping = {tuple(int(k[i:i + 2], 16) for i in (0, 2, 4)): tuple(int(v[i:i + 2], 16) for i in (0, 2, 4))
               for k, v in zip(KEY, PALETTES[school])}
    out = base.convert("RGBA").copy()
    for y in range(out.height):
        for x in range(out.width):
            r, gr, b, a = out.getpixel((x, y))
            if a and (r, gr, b) in mapping:
                out.putpixel((x, y), mapping[(r, gr, b)] + (a,))
    return out


def main():
    g.save(_strip(KEY), "textures/item/scroll_palettes/key.png")
    for school, colors in PALETTES.items():
        assert len(colors) == len(KEY), school
        g.save(_strip(colors), "textures/item/scroll_palettes/%s.png" % school)
        g.write_json("models/item/scroll_%s.json" % school,
                     {"parent": "item/generated", "textures": {"layer0": "irons_spellbooks:item/scroll_arcanes_%s" % school}})
    path = os.path.join(ASSETS, "minecraft", "atlases", "blocks.json")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump({"sources": [{
            "type": "paletted_permutations",
            "textures": ["irons_spellbooks:item/scroll"],
            "palette_key": "arcanes_oublies:item/scroll_palettes/key",
            "permutations": {"arcanes_" + s: "arcanes_oublies:item/scroll_palettes/" + s for s in PALETTES},
        }]}, f, indent=2)
        f.write("\n")
    print("wrote", os.path.relpath(path))
