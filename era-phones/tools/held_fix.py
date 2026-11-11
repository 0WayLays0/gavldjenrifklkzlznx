"""How things are held, set last over what each object's drawing gave (the same for each kind of thing, so they all
sit alike in the hand): flat things (bills, coins, cards, a sheet, a newspaper, a photo, a badge) held as the game
holds its own flat items, a corner toward the thumb; the big things (the phone booth, the street lamp, the
camcorder) brought down to what fits in a hand."""
import json
import os

import kit

FLAT = {
    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [.5, .5, .5]},
    "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [.5, .5, .5]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [-1, 3.6, 1.13], "scale": [.5, .5, .5]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [-1, 3.6, 1.13], "scale": [.5, .5, .5]},
}

FLAT_ITEMS = ["billet_1", "billet_5", "billet_10", "billet_20", "billet_50", "billet_100", "piece_10c", "piece_25c", "carte_identite",
              "feuille", "journal_papier", "photo", "badge"]

BIG = {
    "cabine_telephonique": {
        "thirdperson_righthand": {"rotation": [0, 135, 0], "translation": [0, 2, 1], "scale": [.14, .14, .14]},
        "thirdperson_lefthand": {"rotation": [0, 135, 0], "translation": [0, 2, 1], "scale": [.14, .14, .14]},
        "firstperson_righthand": {"rotation": [0, 160, 0], "translation": [1.5, 1, 0], "scale": [.16, .16, .16]},
        "firstperson_lefthand": {"rotation": [0, 200, 0], "translation": [-1.5, 1, 0], "scale": [.16, .16, .16]},
    },
    "lampadaire": {
        "thirdperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 1, 1], "scale": [.16, .16, .16]},
        "thirdperson_lefthand": {"rotation": [0, 45, 0], "translation": [0, 1, 1], "scale": [.16, .16, .16]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [2, 0, 0], "scale": [.16, .16, .16]},
        "firstperson_lefthand": {"rotation": [0, 45, 0], "translation": [-2, 0, 0], "scale": [.16, .16, .16]},
    },
    "camescope": {
        "firstperson_righthand": {"rotation": [0, 180, 0], "translation": [3, 1, -1], "scale": [.4, .4, .4]},
        "firstperson_lefthand": {"rotation": [0, 180, 0], "translation": [-3, 1, -1], "scale": [.4, .4, .4]},
    },
}


def set_display(name, display):
    path = os.path.join(kit.ROOT, "models", "item", f"{name}.json")
    with open(path, encoding="utf-8") as f:
        model = json.load(f)
    d = dict(model.get("display", {}))
    d.update(display)
    model["display"] = d
    kit.write_json(f"models/item/{name}.json", model)


def main():
    for name in FLAT_ITEMS:
        set_display(name, FLAT)
    for name, display in BIG.items():
        set_display(name, display)


if __name__ == "__main__":
    main()
