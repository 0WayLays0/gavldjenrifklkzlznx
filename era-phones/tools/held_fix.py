"""How things are held, set last over what each object's drawing gave, worked out from the hand itself so that each
thing sits in the fingers (nothing floating beside the hand).

The game puts a held item in the hand's own frame: in a player's right hand (the arm raised a little to hold), that
frame's x runs to the player's left, its y forward and its z up (roughly). For each thing: which way it stands (its
top up, its front facing out), how big it really is (its longest side, in blocks), and where it is gripped (a point of
its model): the rotation, the scale and the shift that put that point in the fist follow. The left hand mirrors it.

Flat and big things in the first person keep a hand's-size placing (see FIRST)."""
import json
import math
import os

import numpy as np

import kit

# The hand's frame, as the game builds it (arm raised 18 degrees), turned to the world's: columns are where the
# model's x, y, z go
HAND = np.array([[-1.0, 0.0, 0.0], [0.0, 0.31, 0.95], [0.0, 0.95, -0.31]])
# The fist, in that frame (model pixels): the hand's end is at z 0 (the arm going on up +z), its four pixels across
# y -4 to 0, x -2 to 2. A thing's grip goes just under the fingers; a handle, in them
FIST = np.array([0.0, -1.6, -0.8])
FIST_HANDLE = np.array([0.0, -2.0, 0.2])

# name: (how it stands, how big (blocks, its longest side), where it is gripped)
#   stands: "up" top up, front facing forward; "back" top up, front toward the holder; "flat" a flat thing held up,
#   face forward; "point" lying forward (a torch), its front forward
#   grip: "low" (its lower part), "mid" (its middle), "top" (by a handle on top), or a model point (x, y, z)
HELD = {
    "smartphone": ("back", .26, "low"), "clapet": ("back", .26, "low"), "gsm": ("back", .28, "low"),
    "radiotelephone": ("back", .36, "low"), "chargeur": ("up", .24, "low"),
    "lampe_torche": ("point", .38, "mid"), "piles": ("up", .16, "mid"), "talkie_walkie": ("up", .38, "low"),
    "cassette": ("flat", .3, "low"), "camescope": ("up", .55, "top"), "appareil_photo": ("up", .3, "mid"),
    "photo": ("flat", .3, "low"), "pellicule": ("up", .14, "mid"),
    "billet_1": ("flat", .3, "low"), "billet_5": ("flat", .3, "low"), "billet_10": ("flat", .3, "low"),
    "billet_20": ("flat", .3, "low"), "billet_50": ("flat", .3, "low"), "billet_100": ("flat", .3, "low"),
    "piece_10c": ("flat", .1, "mid"), "piece_25c": ("flat", .11, "mid"),
    "carte_identite": ("flat", .22, "low"), "feuille": ("flat", .3, "low"), "journal_papier": ("flat", .36, "low"),
    "telecommande_mj": ("up", .28, "low"), "baladeur": ("up", .26, "low"),
    "sac_a_dos_rouge": ("up", .5, "top"), "sac_a_dos_jean": ("up", .5, "top"), "sac_a_dos_militaire": ("up", .5, "top"),
    "sac_a_dos_cuir": ("up", .5, "top"), "sac_a_dos_jaune": ("up", .5, "top"), "sac_a_dos_violet": ("up", .5, "top"),
    "sac_a_dos_sport": ("up", .5, "top"), "sac_a_dos_rando": ("up", .55, "top"),
}

FLAT_FIRST = {
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [-1, 3.6, 1.13], "scale": [.5, .5, .5]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [-1, 3.6, 1.13], "scale": [.5, .5, .5]},
}
FIRST = {name: FLAT_FIRST for name, h in HELD.items() if h[0] == "flat"}
FIRST.update({
    "lampadaire": {
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [2, 0, 0], "scale": [.16, .16, .16]},
        "firstperson_lefthand": {"rotation": [0, 45, 0], "translation": [-2, 0, 0], "scale": [.16, .16, .16]}},
    "camescope": {
        "firstperson_righthand": {"rotation": [0, 180, 0], "translation": [3, 1, -1], "scale": [.4, .4, .4]},
        "firstperson_lefthand": {"rotation": [0, 180, 0], "translation": [-3, 1, -1], "scale": [.4, .4, .4]}},
})
# Big things that are not gripped: only brought down to a hand's size
THIRD_BIG = {
    "lampadaire": {"rotation": [0, 45, 0], "translation": [0, 1, 1], "scale": [.16, .16, .16]},
}


def path(name):
    return os.path.join(kit.ROOT, "models", "item", f"{name}.json")


def elements(name):
    """The model's elements, following its parents (only this mod's)."""
    seen = 0
    while seen < 4:
        with open(path(name), encoding="utf-8") as f:
            m = json.load(f)
        if "elements" in m:
            return m["elements"]
        parent = m.get("parent", "")
        if not parent.startswith("eraphones:item/"):
            return []
        name = parent.split("/", 1)[1]
        seen += 1
    return []


def bounds(els):
    lo = np.array([1e9] * 3)
    hi = np.array([-1e9] * 3)
    for e in els:
        lo = np.minimum(lo, np.minimum(e["from"], e["to"]))
        hi = np.maximum(hi, np.maximum(e["from"], e["to"]))
    return lo, hi


def euler_xyz(r):
    """Angles (degrees) a, b, c with r = Rx(a) Ry(b) Rz(c), as the game turns an item."""
    b = math.asin(max(-1.0, min(1.0, r[0, 2])))
    if abs(math.cos(b)) > 1e-6:
        a = math.atan2(-r[1, 2], r[2, 2])
        c = math.atan2(-r[0, 1], r[0, 0])
    else:
        a = math.atan2(r[2, 1], r[1, 1])
        c = 0.0
    return [round(math.degrees(v), 2) for v in (a, b, c)]


def stance(kind):
    """Where the model's x, y, z go in the world (player facing +z, up +y): its top up; its front (z) forward, or
    toward the holder for "back"."""
    up = np.array([0, 1.0, 0])
    # "back": its front toward the holder; "point": lying forward, its front (north, -z: a lamp's lens) ahead of the holder
    fwd = np.array([0, 0, -1.0]) if kind in ("back", "point") else np.array([0, 0, 1.0])
    return np.column_stack([np.cross(up, fwd), up, fwd])


def third(name, kind, size, grip):
    els = elements(name)
    if not els:
        return None
    lo, hi = bounds(els)
    longest = float(max(hi - lo))
    scale = size / (longest / 16)
    rot = HAND.T @ stance(kind)
    c = (lo + hi) / 2
    if isinstance(grip, tuple):
        g = np.array(grip, float)
    elif grip == "low":
        g = np.array([c[0], lo[1] + .22 * (hi[1] - lo[1]), c[2]])
    elif grip == "top":
        g = np.array([c[0], hi[1] - .06 * (hi[1] - lo[1]), c[2]])
    else:
        g = c
    tr = (FIST_HANDLE if grip == "top" else FIST) - rot @ ((g - 8) * scale)
    t = {"rotation": euler_xyz(rot), "translation": [round(float(v), 2) for v in tr], "scale": [round(scale, 3)] * 3}
    return {"thirdperson_righthand": t, "thirdperson_lefthand": t}


def set_display(name, display):
    with open(path(name), encoding="utf-8") as f:
        model = json.load(f)
    d = dict(model.get("display", {}))
    d.update(display)
    model["display"] = d
    kit.write_json(f"models/item/{name}.json", model)


def main():
    for name, (kind, size, grip) in HELD.items():
        if not os.path.exists(path(name)):
            continue
        d = third(name, kind, size, grip)
        if d:
            set_display(name, d)
    for name, d in THIRD_BIG.items():
        set_display(name, {"thirdperson_righthand": d, "thirdperson_lefthand": d})
    for name, d in FIRST.items():
        if os.path.exists(path(name)):
            set_display(name, d)


if __name__ == "__main__":
    main()
