"""Three emotes for Emotecraft, in a creepy clown's style (INSPIRED: they are made here from scratch, keyframe by keyframe;
no file, animation or name is taken from any film or show):

- clown_sautille: the mocking skip, hopping from one foot to the other, knees high, arms swinging wide like a
  puppet's, the body leaning in and bouncing while the head stays dead level, locked on its prey;
- clown_danse: the loose, jerky dance, swaying from hip to hip, arms flailing up like a marionette's on slack
  strings, knees giving way in turn, the head tilting against the body;
- clown_tete_penchee: standing still, the head slowly tilting over to the shoulder, arms hanging, a faint sway,
  and now and then a sudden twitch.

Emotecraft's JSON (degrees): each part's pitch, yaw, roll (and bend for the limbs) as the game turns a model's part
(the arms and legs raised forward with a negative pitch, out to the side with a positive roll on the right); the
whole body's lean and its x, y, z offset. Written to emotes/ (not to the mod's assets): they are put in a player's
.minecraft/emotes folder."""
import json
import math
import os

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "emotes")
AUTHOR = "Époque RP"

PARTS = ("head", "torso", "rightArm", "leftArm", "rightLeg", "leftLeg", "body")


def pos(v):
    return max(0.0, v)


def skip(t):
    """The skip, 16 ticks for two hops: the right foot lands and the left knee comes up, then the other way."""
    phi = t / 16 * 2 * math.pi
    s1, s2 = pos(math.sin(phi)), pos(-math.sin(phi))
    hop = abs(math.sin(phi))
    sway = s1 - s2
    body_yaw, body_roll = 7 * sway, 5 * sway
    return {
        "body": {"y": round(.11 * hop, 4), "pitch": -10, "yaw": body_yaw, "roll": body_roll},
        # The head stays level and turned to its prey, whatever the body does
        "head": {"pitch": -6, "yaw": -body_yaw, "roll": -body_roll},
        "leftLeg": {"pitch": -72 * s1 + 14 * s2, "bend": 85 * s1 + 12 * s2, "roll": -3},
        "rightLeg": {"pitch": -72 * s2 + 14 * s1, "bend": 85 * s2 + 12 * s1, "roll": 3},
        # Arms against the legs, wide and loose, elbows a little bent, opening out on each hop
        "rightArm": {"pitch": -78 * s1 + 48 * s2, "roll": 14 + 10 * hop, "yaw": 8 * s1, "bend": 28 + 22 * s1},
        "leftArm": {"pitch": -78 * s2 + 48 * s1, "roll": -14 - 10 * hop, "yaw": -8 * s2, "bend": 28 + 22 * s2},
    }


def dance(t):
    """The dance, 24 ticks: one sway from hip to hip, the knees and the arms on every half of it."""
    a = t / 24 * 2 * math.pi
    b = 2 * a
    side = math.sin(a)
    return {
        "body": {"x": round(.13 * side, 4), "y": round(-.05 * abs(math.sin(b)), 4), "roll": 9 * side, "yaw": 6 * math.sin(a + .6)},
        "torso": {"yaw": 12 * math.sin(a)},
        # The head tilts against the sway, bobs with the beat, a little late
        "head": {"roll": -16 * math.sin(a - .4), "pitch": 9 * math.sin(b - .5), "yaw": -10 * math.sin(a - .3)},
        "rightLeg": {"bend": 38 * pos(math.sin(b)), "pitch": -10 * pos(math.sin(b)), "roll": 6 + 4 * side},
        "leftLeg": {"bend": 38 * pos(-math.sin(b)), "pitch": -10 * pos(-math.sin(b)), "roll": -6 + 4 * side},
        # Arms up, flailing like a puppet's on slack strings, never together
        "rightArm": {"pitch": -105 + 55 * math.sin(b), "roll": 38 + 24 * math.sin(a + 1), "yaw": 15 * math.sin(b + 1),
                     "bend": 55 + 40 * math.sin(b + .7)},
        "leftArm": {"pitch": -105 + 55 * math.sin(b + math.pi), "roll": -38 - 24 * math.sin(a + 1 + math.pi),
                    "yaw": -15 * math.sin(b + 1 + math.pi), "bend": 55 + 40 * math.sin(b + .7 + math.pi)},
    }


def tilt(t):
    """Standing still: 20 ticks for the head to tilt over, then 60 ticks looped: a faint sway, a twitch at 48."""
    if t <= 20:
        k = t / 20
        k = k * k * (3 - 2 * k)
        sway = 0
    else:
        k = 1
        sway = math.sin((t - 20) / 60 * 2 * math.pi)
    twitch = math.exp(-((t - 48) / 1.6) ** 2) if t > 20 else 0
    return {
        "body": {"pitch": -9 * k, "roll": 2.5 * sway},
        "head": {"roll": 58 * k + 14 * twitch + 3 * sway, "pitch": 14 * k - 6 * twitch, "yaw": 6 * k * sway},
        "rightArm": {"pitch": -10 * k - 22 * twitch, "roll": 3, "bend": 12 * k + 20 * twitch},
        "leftArm": {"pitch": -10 * k + 4 * sway, "roll": -3, "bend": 12 * k},
        "rightLeg": {"roll": 2},
        "leftLeg": {"roll": -2, "bend": 6 * k},
    }


def frame(values):
    out = {}
    for part, axes in values.items():
        out[part] = {k: round(float(v), 3) for k, v in axes.items()}
    return out


def emote(name, title, description, fn, end, loop_from, step=2):
    moves = []
    for t in range(0, end + 1, step):
        m = {"tick": t, "easing": "LINEAR", "turn": 0}
        m.update(frame(fn(t)))
        moves.append(m)
    data = {
        "name": title,
        "description": description,
        "author": AUTHOR,
        "emote": {
            "isLoop": "true",
            "returnTick": loop_from,
            "beginTick": 0,
            "endTick": end,
            "stopTick": end + 4,
            "degrees": True,
            "nsfw": False,
            "moves": moves,
        },
    }
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, f"{name}.json")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=1)
        f.write("\n")
    print("wrote", os.path.relpath(path, os.path.join(HERE, "..")))
    return data


def main():
    emote("clown_sautille", "Clown : pas sautillé",
          "Il sautille d'un pied sur l'autre, genoux hauts, bras ballants comme une marionnette, la tête droite et fixe.",
          skip, 16, 0)
    emote("clown_danse", "Clown : danse désarticulée",
          "Une danse molle et saccadée, d'une hanche sur l'autre, les bras qui battent l'air, la tête qui penche.",
          dance, 24, 0)
    emote("clown_tete_penchee", "Clown : tête penchée",
          "Immobile, la tête se penche lentement sur l'épaule, les bras pendants, puis un sursaut de temps en temps.",
          tilt, 80, 20)


if __name__ == "__main__":
    main()
