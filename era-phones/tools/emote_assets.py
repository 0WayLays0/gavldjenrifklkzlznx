"""Emotes for Emotecraft in a creepy clown's style (INSPIRED: made here from scratch, pose by pose; no file, animation
or name taken from any film or show). Written to emotes/ (not to the mod's assets): a player puts them in their
.minecraft/emotes folder. emote_preview.py draws them on a blocky player to check them.

The format (Emotecraft JSON, version 3, degrees), as its player animator reads it:
- head, torso, rightArm, leftArm, rightLeg, leftLeg: a model part's pitch, yaw, roll (an arm or a leg goes forward
  with a negative pitch; the right ones go out to the side with a positive roll, the left ones with a negative one; the
  head looks down with a positive pitch, turns to its right with a positive yaw, tilts to its left with a positive
  roll); the arms and legs and the torso bend at their middle: an elbow bends with a NEGATIVE bend, a knee with a
  POSITIVE one, the torso leans forward with a positive one (carrying the head and the arms);
- body: the whole player: x, y, z in blocks (y up), a lean forward with a negative pitch, turned about a point 0.7
  above the feet.

Each emote is a list of poses at given ticks, eased from one to the next (a smooth curve through them, looping when
it loops), with layers added on top (breath, sway, shakes). Every tick is written out."""
import json
import math
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "emotes")
AUTHOR = "Époque RP"
PARTS = ("head", "torso", "rightArm", "leftArm", "rightLeg", "leftLeg", "body")
AXES = ("x", "y", "z", "pitch", "yaw", "roll", "bend")
MIRROR = {"rightArm": "leftArm", "leftArm": "rightArm", "rightLeg": "leftLeg", "leftLeg": "rightLeg"}


def file_safe(title):
    """A name that can also be a file's name on Windows (Emote Tweaks looks for a sound file named after the emote):
    no < > : " / \\ | ? *, no dot or space at the end."""
    t = title.replace(" : ", " - ")
    t = re.sub(r'[<>:"/\\|?*]', "", t).replace("…", "").rstrip(" .")
    return t


def mirror(pose):
    """The same pose on the other side: right and left swapped, every turn and tilt the other way."""
    out = {}
    for part, axes in pose.items():
        m = {}
        for a, v in axes.items():
            m[a] = -v if a in ("yaw", "roll", "x") else v
        out[MIRROR.get(part, part)] = m
    return out


def merge(*poses):
    out = {}
    for p in poses:
        for part, axes in p.items():
            out.setdefault(part, {}).update(axes)
    return out


def smooth(t):
    return t * t * (3 - 2 * t)


class Emote:
    def __init__(self, name, title, description, end, loop_from=0):
        self.name, self.title, self.description = name, title, description
        self.end, self.loop_from = end, loop_from
        self.keys = {}
        self.layers = []

    def key(self, t, pose, hold=0):
        """The pose at tick t (and held still for `hold` ticks)."""
        for part, axes in pose.items():
            for a, v in axes.items():
                self.keys.setdefault((part, a), {})[t] = v
                if hold:
                    self.keys[(part, a)][t + hold] = v
        return self

    def layer(self, fn):
        self.layers.append(fn)
        return self

    def _curve(self, pts, t):
        """Catmull-Rom through the keys (looping over [loop_from, end] when the emote loops)."""
        ts = sorted(pts)
        period = self.end - self.loop_from
        cyc = [k for k in ts if self.loop_from <= k < self.end]

        def at(i, base):
            # the i-th key of the cycle, unrolled
            n = len(cyc)
            k = cyc[i % n]
            return k + (i // n) * period, pts[k]

        if t >= self.loop_from and len(cyc) >= 2:
            tt = (t - self.loop_from) % period + self.loop_from if t < self.end else self.loop_from
            # find the segment
            n = len(cyc)
            for i in range(-1, n + 1):
                k0, v0 = at(i, 0)
                k1, v1 = at(i + 1, 0)
                if k0 <= tt <= k1:
                    km, vm = at(i - 1, 0)
                    k2, v2 = at(i + 2, 0)
                    return self._cr(tt, km, vm, k0, v0, k1, v1, k2, v2)
            return pts[cyc[0]]
        if len(ts) == 1:
            return pts[ts[0]]
        if t <= ts[0]:
            return pts[ts[0]]
        if t >= ts[-1]:
            return pts[ts[-1]]
        for i in range(len(ts) - 1):
            if ts[i] <= t <= ts[i + 1]:
                km = ts[i - 1] if i > 0 else ts[i] - 1
                k2 = ts[i + 2] if i + 2 < len(ts) else ts[i + 1] + 1
                return self._cr(t, km, pts.get(km, pts[ts[i]]), ts[i], pts[ts[i]], ts[i + 1], pts[ts[i + 1]], k2,
                                pts.get(k2, pts[ts[i + 1]]))
        return pts[ts[-1]]

    @staticmethod
    def _cr(t, km, vm, k0, v0, k1, v1, k2, v2):
        """A cubic between (k0, v0) and (k1, v1), its slopes from the keys either side (uneven spacing allowed)."""
        h = max(1e-9, k1 - k0)
        s = (t - k0) / h
        m0 = (v1 - vm) / max(1e-9, k1 - km) * h
        m1 = (v2 - v0) / max(1e-9, k2 - k0) * h
        s2, s3 = s * s, s * s * s
        return (2 * s3 - 3 * s2 + 1) * v0 + (s3 - 2 * s2 + s) * m0 + (-2 * s3 + 3 * s2) * v1 + (s3 - s2) * m1

    def pose(self, t):
        out = {p: {} for p in PARTS}
        for (part, a), pts in self.keys.items():
            out[part][a] = self._curve(pts, t)
        for fn in self.layers:
            for part, axes in fn(t).items():
                for a, v in axes.items():
                    out[part][a] = out[part].get(a, 0) + v
        return out

    def write(self):
        moves = []
        used = set()
        frames = [self.pose(t) for t in range(self.end + 1)]
        if self.loop_from is not None:
            # the last tick is the loop's first, exactly
            frames[self.end] = frames[self.loop_from]
        for f in frames:
            for part, axes in f.items():
                for a, v in axes.items():
                    if abs(v) > 1e-4:
                        used.add((part, a))
        for t, f in enumerate(frames):
            m = {"tick": t, "easing": "LINEAR", "turn": 0}
            for part in PARTS:
                axes = {a: round(float(f[part].get(a, 0.0)), 4 if a in ("x", "y", "z") else 2) for a in AXES if (part, a) in used}
                if axes:
                    m[part] = axes
            moves.append(m)
        data = {
            "version": 3,
            "name": file_safe(self.title),
            "description": self.description,
            "author": AUTHOR,
            "emote": {
                "isLoop": True,
                "returnTick": self.loop_from,
                "beginTick": 0,
                "endTick": self.end,
                "stopTick": self.end + 6,
                "degrees": True,
                "nsfw": False,
                "moves": moves,
            },
        }
        os.makedirs(OUT, exist_ok=True)
        path = os.path.join(OUT, f"{self.name}.json")
        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, separators=(",", ":"))
            f.write("\n")
        print("wrote", os.path.relpath(path, os.path.join(HERE, "..")))


def wave(t, period, phase=0.0):
    return math.sin(2 * math.pi * (t / period + phase))


# ------------------------------------------------------------------------------------------------ helpers

import functools  # noqa: E402

import emote_kin  # noqa: E402


@functools.lru_cache(maxsize=None)
def _reach(side, target, prefer):
    return emote_kin.reach(side, target, dict(prefer) if prefer else None)[0]


def hand(side, target, **prefer):
    """The arm's angles that put its hand on `target` (model space: pixels, y down from the neck, -z in front, the right
    side toward -x)."""
    return dict(_reach(side, tuple(round(v, 1) for v in target), tuple(sorted(prefer.items()))))


def hands(target_right, **prefer):
    """Both hands, the left one the mirror of the right."""
    l = (-target_right[0], target_right[1], target_right[2])
    pl = {k: (-v if k in ("yaw", "roll") else v) for k, v in prefer.items()}
    return {"rightArm": hand("right", target_right, **prefer), "leftArm": hand("left", l, **pl)}


def bow(a, right_leg=0.0, left_leg=0.0, knees=0.0):
    """Bent forward at the hips by `a` degrees, the feet staying where they are (each leg turned back by its own angle
    too, if asked: a foot slid back); `knees`: both knees bent that much, the thighs forward and the body lowered so
    the feet stay under it."""
    drop = .75 * .9375 * (1 - math.cos(math.radians(knees / 2)))
    out = {"body": {"pitch": -a}, "rightLeg": {"pitch": -a + right_leg - knees / 2, "bend": knees},
           "leftLeg": {"pitch": -a + left_leg - knees / 2, "bend": knees}}
    if knees:
        out["body"]["y"] = -drop
    return out


def hang(a):
    """The arms hanging straight down while bent forward by `a`."""
    return {"rightArm": {"pitch": -a, "roll": 4, "yaw": 0, "bend": -6}, "leftArm": {"pitch": -a, "roll": -4, "yaw": 0, "bend": -6}}


def leg_points(side, pitch, bend):
    """A leg's knee and foot (model space) for these angles."""
    sx = -1 if side == "right" else 1
    hip = np.array([1.9 * sx, 12, .1])
    a, b = math.radians(pitch), math.radians(pitch + bend)
    knee = hip + np.array([0, 6 * math.cos(a), 6 * math.sin(a)])
    foot = knee + np.array([0, 6 * math.cos(b), 6 * math.sin(b)])
    return knee, foot


import numpy as np  # noqa: E402


# ------------------------------------------------------------------------------------------------ the skip

def skip():
    """The mocking skip, light and bouncing, not a run: upright, a step then a hop on the same foot while the other knee
    springs up high, a long float, a soft landing; the arms swing up wide and out to the sides like a puppet's, the
    hands loose; the body sways; the head stays dead level, locked on its prey, a little tilted."""
    e = Emote("clown_sautille", "Clown : pas sautillé",
              "Il sautille d'un pied sur l'autre, léger, genoux très hauts, bras qui montent en grand sur les côtés "
              "comme une marionnette ; la tête reste droite et fixe. Marche avec.", 24)

    def side(t0, mirror_it):
        land = {  # down on the right foot, the knee giving a little, the left foot behind
            "body": {"y": -.05, "pitch": -4, "roll": -3, "yaw": 3},
            "rightLeg": {"pitch": -8, "bend": 26, "roll": 2}, "leftLeg": {"pitch": 18, "bend": 30, "roll": -2},
            "rightArm": {"pitch": -20, "roll": 24, "yaw": 0, "bend": -14}, "leftArm": {"pitch": -24, "roll": -24, "yaw": 0, "bend": -14},
            "head": {"pitch": -2, "roll": 8, "yaw": -3}}
        spring = {  # off the right foot, the left knee coming up
            "body": {"y": .12, "pitch": -3, "roll": -4, "yaw": 5},
            "rightLeg": {"pitch": 6, "bend": 4, "roll": 2}, "leftLeg": {"pitch": -62, "bend": 80, "roll": -3},
            "rightArm": {"pitch": -92, "roll": 34, "yaw": -6, "bend": -26}, "leftArm": {"pitch": 14, "roll": -30, "yaw": 0, "bend": -12},
            "head": {"pitch": -1, "roll": 9, "yaw": -5}}
        float_ = {  # the top of the hop: the left knee at the hip, the right toe pointed down, the right arm high
            "body": {"y": .28, "pitch": -2, "roll": -4, "yaw": 6},
            "rightLeg": {"pitch": 10, "bend": 6, "roll": 2}, "leftLeg": {"pitch": -92, "bend": 96, "roll": -4},
            "rightArm": {"pitch": -128, "roll": 40, "yaw": -8, "bend": -30}, "leftArm": {"pitch": 24, "roll": -38, "yaw": 2, "bend": -14},
            "head": {"pitch": 0, "roll": 9, "yaw": -6}}
        drop = {
            "body": {"y": .12, "pitch": -3, "roll": -3, "yaw": 4},
            "rightLeg": {"pitch": 4, "bend": 10, "roll": 2}, "leftLeg": {"pitch": -40, "bend": 34, "roll": -3},
            "rightArm": {"pitch": -76, "roll": 32, "yaw": -4, "bend": -22}, "leftArm": {"pitch": 6, "roll": -30, "yaw": 0, "bend": -12},
            "head": {"pitch": -1, "roll": 8, "yaw": -4}}
        poses = [(0, land), (3, spring), (6, float_), (9, drop)]
        for dt, p in poses:
            if mirror_it:
                p = mirror(p)
                p["head"]["roll"] = -p["head"]["roll"] * .4 + 8
            e.key(t0 + dt, p)

    side(0, False)
    side(12, True)
    return e


# ------------------------------------------------------------------------------------------------ the dance

def dance():
    """The clown's dance, 96 ticks, three parts of four beats: the jig (hopping from foot to foot, the free leg flicked
    out, both hands up by the head flapping from one side to the other together, the head bobbing against them); the
    shimmy (crouched low, the elbows out like wings flapping, the shoulders shaking, the head swinging); and the
    flourish (arms flung up in turn, a kick, then a full spin on the spot and a bow of the head)."""
    e = Emote("clown_danse", "Clown : la danse",
              "Sa gigue de clown : sautille d'un pied sur l'autre les mains agitées au-dessus de la tête, puis "
              "accroupi il bat des coudes et secoue les épaules, puis bras au ciel, coup de pied et pirouette.", 96)
    B = 8
    up_r = hand("right", (-9, -11, -3), roll=60)
    up_l = hand("left", (9, -11, -3), roll=-60)
    over_r = hand("right", (-2, -13, -4), roll=30)
    over_l = hand("left", (2, -13, -4), roll=-30)
    for i in range(4):
        t = i * B
        s = 1 if i % 2 == 0 else -1
        hop_leg, free_leg = ("rightLeg", "leftLeg") if s > 0 else ("leftLeg", "rightLeg")
        # both hands up, swung over to one side (s): the near one out, the far one over the head
        arms = {"rightArm": up_r, "leftArm": over_l} if s > 0 else {"rightArm": over_r, "leftArm": up_l}
        land = merge({"body": {"y": -.08, "x": .06 * s, "roll": 10 * s, "yaw": -8 * s, "pitch": -4},
                      hop_leg: {"pitch": -6, "bend": 30, "roll": 3 * s}, free_leg: {"pitch": 10, "bend": 70, "roll": 18 * s},
                      "head": {"roll": -22 * s, "pitch": 6, "yaw": 10 * s}}, arms)
        hop = merge({"body": {"y": .12, "x": .03 * s, "roll": 4 * s, "yaw": -3 * s, "pitch": -2},
                     hop_leg: {"pitch": 4, "bend": 6, "roll": 2 * s}, free_leg: {"pitch": -30, "bend": 60, "roll": 24 * s},
                     "head": {"roll": -8 * s, "pitch": -6, "yaw": 4 * s}}, arms)
        e.key(t, land).key(t + 4, hop)
    wing_r = hand("right", (-12, 1, -4), roll=80)
    wing_l = hand("left", (12, 1, -4), roll=-80)
    flap_r = hand("right", (-11, -6, -3), roll=90)
    flap_l = hand("left", (11, -6, -3), roll=-90)
    for i in range(4):
        t = 32 + i * B
        s = 1 if i % 2 == 0 else -1
        low = merge(bow(14), {"body": {"y": -.2, "x": .05 * s, "roll": 6 * s, "yaw": 14 * s, "pitch": -14},
                              "rightLeg": {"bend": 62, "roll": 10}, "leftLeg": {"bend": 62, "roll": -10},
                              "head": {"roll": 18 * s, "pitch": -10, "yaw": -18 * s}, "rightArm": wing_r, "leftArm": wing_l})
        low["rightLeg"]["pitch"] = -14 - 20
        low["leftLeg"]["pitch"] = -14 - 20
        high = merge(low, {"body": {"y": -.12, "x": -.02 * s, "roll": -4 * s, "yaw": -6 * s, "pitch": -12},
                           "rightArm": flap_r, "leftArm": flap_l, "head": {"roll": -10 * s, "pitch": -4, "yaw": 10 * s}})
        e.key(t, low).key(t + 4, high)
    sky_r = hand("right", (-5, -18, -2), roll=10)
    sky_l = hand("left", (5, -18, -2), roll=-10)
    rest = {"body": {"y": 0, "x": 0, "roll": 0, "yaw": 0, "pitch": 0}, "rightLeg": {"pitch": 0, "bend": 4, "roll": 2},
            "leftLeg": {"pitch": 0, "bend": 4, "roll": -2}, "rightArm": {"pitch": -10, "roll": 20, "yaw": 0, "bend": -20},
            "leftArm": {"pitch": -10, "roll": -20, "yaw": 0, "bend": -20}, "head": {"roll": 0, "pitch": 0, "yaw": 0}}
    e.key(64, merge(rest, {"rightArm": sky_r, "body": {"roll": -6, "y": .06}, "head": {"pitch": -20, "roll": 10}}))
    e.key(68, merge(rest, {"leftArm": sky_l, "rightArm": {"pitch": -20, "roll": 50, "yaw": 0, "bend": -40}, "body": {"roll": 6, "y": .06},
                           "head": {"pitch": -20, "roll": -10}}))
    e.key(72, merge(rest, {"rightArm": sky_r, "leftArm": sky_l, "rightLeg": {"pitch": -70, "bend": 10}, "body": {"y": .1, "pitch": 6},
                           "head": {"pitch": -30}}))
    e.key(76, merge(rest, {"rightArm": {"pitch": -30, "roll": 70, "yaw": 0, "bend": -30}, "leftArm": {"pitch": -30, "roll": -70, "yaw": 0, "bend": -30},
                           "body": {"y": .04, "yaw": 0}}))
    # ta-da: a hop to one side, to the other, then arms flung wide and up, a little bow of the head
    e.key(82, merge(rest, {"rightArm": {"pitch": -30, "roll": 80, "yaw": 0, "bend": -20}, "leftArm": {"pitch": -30, "roll": -80, "yaw": 0, "bend": -20},
                           "body": {"y": .12, "x": .1, "roll": -8}, "leftLeg": {"pitch": -10, "bend": 40, "roll": -16}}))
    e.key(86, merge(rest, {"rightArm": {"pitch": -150, "roll": 40, "yaw": 0, "bend": -10}, "leftArm": {"pitch": -150, "roll": -40, "yaw": 0, "bend": -10},
                           "body": {"y": .02, "x": 0}, "head": {"pitch": -14}}))
    e.key(91, merge(rest, bow(14), {"head": {"pitch": 16, "roll": 14}, "rightArm": {"pitch": -60, "roll": 50, "yaw": 0, "bend": -40},
                                    "leftArm": {"pitch": -40, "roll": -60, "yaw": 0, "bend": -20}}))
    # The hands flutter all the while (the forearms quiver)
    e.layer(lambda t: {"rightArm": {"bend": 10 * wave(t, 3)}, "leftArm": {"bend": 10 * wave(t, 3, .5)}} if t < 64 else {})
    return e


# ------------------------------------------------------------------------------------------------ the tilted head

def tilt():
    """Standing still, the head tilts over onto the shoulder (a slow overshoot, settling), the shoulders sag, the arms
    hang; then it breathes, sways, watches, and twitches: a snap of the head with a jerk of the arm, later a sharp turn
    of the head the other way, held, then slowly back."""
    e = Emote("clown_tete_penchee", "Clown : tête penchée",
              "Immobile, la tête se penche lentement sur l'épaule ; il respire, se balance, observe… et sursaute.", 130, 30)
    rest = {"body": {"pitch": 0, "x": 0}, "head": {"roll": 0, "pitch": 0, "yaw": 0},
            "rightArm": {"pitch": 0, "roll": 4, "bend": -4}, "leftArm": {"pitch": 0, "roll": -4, "bend": -4},
            "rightLeg": {"pitch": 0, "bend": 0}, "leftLeg": {"pitch": 0, "bend": 0}}
    tilted = merge(bow(6), {"body": {"x": 0}, "head": {"roll": 60, "pitch": -2, "yaw": -4},
                            "rightArm": {"pitch": -10, "roll": 2, "bend": -16}, "leftArm": {"pitch": -12, "roll": -10, "bend": -14},
                            "leftLeg": {"bend": 8}, "rightLeg": {"bend": 2}})
    e.key(0, rest).key(4, rest)
    e.key(24, merge(tilted, {"head": {"roll": 67, "pitch": 1}}))
    e.key(30, tilted)
    e.key(54, tilted)
    e.key(56, merge(tilted, {"head": {"roll": 80, "pitch": -16, "yaw": 6}, "rightArm": {"pitch": -38, "bend": -54, "roll": 12},
                             "body": {"x": .03}}))
    e.key(59, merge(tilted, {"head": {"roll": 54, "pitch": -4}, "rightArm": {"pitch": -16, "bend": -22}}))
    e.key(66, tilted)
    e.key(90, tilted)
    e.key(92, merge(tilted, {"head": {"yaw": -42, "roll": 48, "pitch": -8}}))
    e.key(102, merge(tilted, {"head": {"yaw": -40, "roll": 50, "pitch": -6}}))
    e.key(122, tilted)
    e.layer(lambda t: {} if t < 26 else {"body": {"roll": 1.8 * wave(t, 100, .25), "pitch": -1 * wave(t, 50)},
                                          "head": {"roll": 2.5 * wave(t, 100, .6), "yaw": 5 * wave(t, 100, .1)},
                                          "rightArm": {"pitch": 2 * wave(t, 100, .3)}, "leftArm": {"pitch": -2 * wave(t, 100, .3)}})
    return e


# ------------------------------------------------------------------------------------------------ sitting

def sit():
    """Sitting on the ground, knees drawn up, the arms wrapped round the shins (the hands clasped in front of them),
    leaning over the knees and rocking slowly on the hips, the head on one side looking up from under the brow; now and
    then the head rolls over to the other side."""
    e = Emote("clown_assis", "Clown : assis, se balance",
              "Assis par terre, il serre ses jambes dans ses bras et se balance doucement d'avant en arrière, "
              "la tête penchée, les yeux levés vers toi.", 120, 0)
    lean = 22
    thigh = -120 - lean
    legs = {"rightLeg": {"pitch": thigh, "bend": 124, "roll": 5}, "leftLeg": {"pitch": thigh, "bend": 124, "roll": -5}}
    knee, foot = leg_points("right", thigh, 124)
    shin_mid = (knee + foot) / 2
    # the hands clasped in front of the shins, a little below the knees
    grip = (-1.0, float(shin_mid[1] - 1.0), float(shin_mid[2] - 3.2))
    arms = hands(grip, roll=20, pitch=-70)
    base = merge({"body": {"y": -.64, "pitch": -lean}, "head": {"pitch": -30 + 0, "roll": 22, "yaw": 0}}, legs, arms)
    e.key(0, base).key(54, base)
    other = merge(base, {"head": {"pitch": -28, "roll": -22, "yaw": 4}})
    e.key(66, other).key(108, other).key(118, base)
    # rocking: the whole body back and forth on the hips (the arms hold the legs), the head lagging a little
    e.layer(lambda t: {"body": {"pitch": 8 * wave(t, 40)}, "head": {"pitch": -4 * wave(t, 40, .15)}})
    return e


def ledge():
    """Sitting on a ledge (stand at the edge of a block, facing out): legs dangling and swinging like a child's, the
    hands holding the edge, the head rolling slowly from one shoulder to the other."""
    e = Emote("clown_assis_rebord", "Clown : assis au bord",
              "Assis au bord d'un bloc (mets-toi au bord, face au vide), les jambes qui se balancent comme un enfant, "
              "la tête qui roule lentement d'une épaule à l'autre.", 168, 0)
    base = {"body": {"y": -.7, "z": -.12, "pitch": 0},
            "rightLeg": {"pitch": -88, "bend": 90, "roll": 3}, "leftLeg": {"pitch": -88, "bend": 90, "roll": -3},
            "rightArm": {"pitch": 18, "roll": 18, "bend": -8}, "leftArm": {"pitch": 18, "roll": -18, "bend": -8},
            "head": {"pitch": -4}}
    e.key(0, base)
    e.layer(lambda t: {"rightLeg": {"bend": 30 * wave(t, 28)}, "leftLeg": {"bend": -30 * wave(t, 28)},
                       "head": {"roll": 26 * wave(t, 168), "yaw": 8 * wave(t, 168, .25)},
                       "body": {"roll": 2 * wave(t, 168, .5)},
                       "rightArm": {"pitch": 2 * wave(t, 28)}, "leftArm": {"pitch": -2 * wave(t, 28)}})
    return e


# ------------------------------------------------------------------------------------------------ with something in hand

def offer():
    """Holding something out (whatever is in the right hand: a balloon...) at chest height, the arm almost straight, the
    left hand tucked behind the back; leaning in a little, the head on one side; the hand bobbing the thing gently up
    and down, come and take it, now and then pushed a little closer."""
    e = Emote("clown_tend_objet", "Clown : tend l'objet",
              "Il te tend ce qu'il a en main droite (un ballon…), à hauteur de poitrine, l'autre main dans le dos, "
              "penché vers toi, la tête sur le côté. Viens le prendre…", 80, 0)
    behind = hand("left", (2.5, 9, 3.4), roll=10, pitch=30)
    base = merge(bow(6), {"rightArm": {"pitch": -72, "roll": -4, "yaw": -6, "bend": -14}, "leftArm": behind,
                          "head": {"roll": -18, "pitch": -4, "yaw": 4}})
    closer = merge(bow(12), {"rightArm": {"pitch": -80, "roll": -4, "yaw": -6, "bend": -4}, "leftArm": behind,
                             "head": {"roll": -24, "pitch": -8, "yaw": 2}, "body": {"z": -.06}})
    e.key(0, base).key(40, base).key(50, closer).key(62, closer).key(74, base)
    e.layer(lambda t: {"rightArm": {"pitch": 5 * wave(t, 20)}, "head": {"roll": 2 * wave(t, 80)}})
    return e


def beckon():
    """Come here: the right hand held out, palm up, then the forearm drawn slowly in toward the chest, twice, the body
    leaning back with it; the left hand on the hip; the head on one side, chin down, eyes up."""
    e = Emote("clown_viens", "Clown : viens…",
              "Il te fait signe de venir : la main tendue vers toi puis ramenée lentement contre lui, deux fois, "
              "l'autre main sur la hanche, la tête penchée, le menton baissé.", 60, 0)
    hip = hand("left", (5.5, 10.5, -1.0), roll=-40, pitch=10)
    out = merge(bow(10), {"rightArm": {"pitch": -78, "roll": 6, "yaw": -8, "bend": -10}, "leftArm": hip,
                          "head": {"roll": 20, "pitch": 4, "yaw": 0}})
    curl = merge(bow(2), {"rightArm": hand("right", (-2.5, 2, -6), pitch=-60), "leftArm": hip,
                          "head": {"roll": 24, "pitch": 8, "yaw": 0}})
    e.key(0, out).key(8, out).key(20, curl).key(26, out).key(34, out).key(46, curl).key(54, out)
    return e


def giggle():
    """The laugh: both hands clutching the belly; first leaning back, the head thrown back (ha, ha, ha: the shoulders
    jolting), then folded forward over the belly, the head bobbing down, then up again."""
    e = Emote("clown_rire", "Clown : rire",
              "Il se tient le ventre et éclate de rire : la tête renversée en arrière, les épaules qui tressautent, "
              "puis plié en deux, puis il se redresse.", 80, 0)
    belly = hands((-1.6, 8.6, -3.2), roll=30, yaw=-40)
    back = merge(bow(-10), {"head": {"pitch": -38, "roll": 6}}, belly)
    fold = merge(bow(34, knees=16), {"head": {"pitch": 10, "roll": -8}}, belly)
    e.key(0, back).key(22, back).key(32, fold).key(56, fold).key(68, back)
    # ha-ha-ha: quick jolts of the body and the head, stronger at the peak of each phase
    e.layer(lambda t: {"body": {"y": .018 * wave(t, 4), "pitch": 2.5 * wave(t, 4, .25)}, "head": {"pitch": 4 * wave(t, 4, .5)}})
    return e


def peekaboo():
    """Peek-a-boo, plainly: the hands over the face, swaying, the head shaking a little (where is he?); then the hands
    flung open beside the face, fingers out (BOO), the body lunging forward on bent knees; held, trembling; then the
    hands slowly back over the face."""
    e = Emote("clown_coucou", "Clown : coucou !",
              "Il se cache le visage dans les mains en se balançant… puis les ouvre d'un coup de chaque côté du visage "
              "en se jetant vers toi : COUCOU ! Puis il se cache à nouveau.", 90, 0)
    face = hands((-1.6, -4.0, -5.4), roll=35, pitch=-120)
    opened = hands((-9.0, -5.0, -6.5), roll=80, pitch=-110)
    hide = merge(bow(4, knees=6), {"head": {"pitch": 10, "roll": 0, "yaw": 0}}, face)
    boo = merge(bow(18, knees=34), {"head": {"pitch": -16, "roll": 16, "yaw": 0}, "body": {"z": -.12}}, opened)
    e.key(0, hide).key(28, hide)
    e.key(31, merge(boo, {"body": {"z": -.2}}))
    e.key(34, boo).key(56, boo).key(72, hide)
    e.layer(lambda t: {"body": {"roll": 4 * wave(t, 28)}, "head": {"yaw": 6 * wave(t, 14)}} if t < 28 or t > 70 else
            {"rightArm": {"roll": 2 * wave(t, 2)}, "leftArm": {"roll": -2 * wave(t, 2)}})
    return e


def reverence():
    """A real bow: the right foot slides back, the right hand comes to the belly, the left arm sweeps out wide to the
    side, and the whole body bends forward from the hips, the head going down with it; held, then slowly back up."""
    e = Emote("clown_reverence", "Clown : révérence",
              "Une vraie révérence : un pied glisse en arrière, une main sur le ventre, l'autre bras qui s'ouvre sur "
              "le côté, et il se plie en avant, la tête baissée ; puis il se redresse lentement.", 90, 0)
    stand = merge(bow(0), {"rightArm": {"pitch": 0, "roll": 4, "yaw": 0, "bend": -4}, "leftArm": {"pitch": 0, "roll": -4, "yaw": 0, "bend": -4},
                           "head": {"pitch": 0, "roll": 0}, "rightLeg": {"bend": 0}, "leftLeg": {"bend": 0}})
    tummy = hand("right", (-.5, 7.5, -3.0), roll=30, yaw=-50)
    step = merge(bow(0, right_leg=22), {"rightArm": tummy, "leftArm": {"pitch": -30, "roll": -70, "yaw": 0, "bend": -14},
                                         "head": {"pitch": 0}, "rightLeg": {"bend": 6}, "leftLeg": {"bend": 8}})
    down = merge(bow(52, right_leg=24), {"rightArm": tummy, "leftArm": {"pitch": -40, "roll": -62, "yaw": 0, "bend": -10},
                                         "head": {"pitch": 18, "roll": 0}, "rightLeg": {"bend": 8}, "leftLeg": {"bend": 12}})
    e.key(0, stand).key(6, stand).key(16, step).key(32, down).key(58, down).key(76, step).key(86, stand)
    return e


def puppet():
    """A marionette: lifted a little off the ground, hanging from its strings, the wrists held up by them (the hands
    higher than the elbows, the forearms dangling up), the head lolling; it 'walks' in the air: a knee and the
    opposite hand pulled up together, dropped, the other pair; the body bobbing on the strings."""
    e = Emote("clown_marionnette", "Clown : marionnette",
              "Une marionnette pendue à ses fils : un peu soulevé, poignets tenus en l'air, tête qui pend ; il « marche » "
              "dans le vide, un genou et la main opposée tirés vers le haut, puis l'autre.", 48, 0)
    wrist_r = hand("right", (-10.5, 3, -4), roll=70, pitch=-40)
    wrist_l = hand("left", (10.5, 3, -4), roll=-70, pitch=-40)
    pulled_r = hand("right", (-9.5, -6, -4), roll=60, pitch=-90)
    pulled_l = hand("left", (9.5, -6, -4), roll=-60, pitch=-90)
    hang_ = {"body": {"y": .1}, "rightLeg": {"pitch": 8, "bend": 10, "roll": 3}, "leftLeg": {"pitch": 8, "bend": 10, "roll": -3},
             "rightArm": wrist_r, "leftArm": wrist_l, "head": {"pitch": 30, "roll": 0}}
    a = merge(hang_, {"body": {"y": .16, "roll": -4}, "leftLeg": {"pitch": -55, "bend": 70}, "rightArm": pulled_r,
                      "head": {"pitch": 26, "roll": -24}})
    b = merge(hang_, {"body": {"y": .16, "roll": 4}, "rightLeg": {"pitch": -55, "bend": 70}, "leftArm": pulled_l,
                      "head": {"pitch": 26, "roll": 24}})
    e.key(0, hang_).key(8, a).key(16, merge(hang_, {"head": {"roll": -12}})).key(24, hang_).key(32, b).key(40, merge(hang_, {"head": {"roll": 12}}))
    return e


ALL = (skip, dance, tilt, sit, ledge, offer, beckon, giggle, peekaboo, reverence, puppet)
# The tick of each emote's icon: its most telling pose
ICON_TICK = {"clown_sautille": 6, "clown_danse": 4, "clown_tete_penchee": 44, "clown_assis": 10, "clown_assis_rebord": 7,
             "clown_tend_objet": 20, "clown_viens": 8, "clown_rire": 10, "clown_coucou": 40, "clown_reverence": 40,
             "clown_marionnette": 8}


def main():
    import emote_preview
    if os.path.isdir(OUT):
        for f in os.listdir(OUT):
            if f.endswith(".json") or f.endswith(".png"):
                os.remove(os.path.join(OUT, f))
    for fn in ALL:
        e = fn()
        e.write()
        # Emotecraft shows the picture of the same name beside the emote as its icon
        emote_preview.icon(os.path.join(OUT, e.name + ".json"), os.path.join(OUT, e.name + ".png"), ICON_TICK.get(e.name, 0))


if __name__ == "__main__":
    main()
