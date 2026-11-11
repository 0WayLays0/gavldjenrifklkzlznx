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

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "emotes")
AUTHOR = "Époque RP"
PARTS = ("head", "torso", "rightArm", "leftArm", "rightLeg", "leftLeg", "body")
AXES = ("x", "y", "z", "pitch", "yaw", "roll", "bend")
MIRROR = {"rightArm": "leftArm", "leftArm": "rightArm", "rightLeg": "leftLeg", "leftLeg": "rightLeg"}


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
            "name": self.title,
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


# ------------------------------------------------------------------------------------------------ the skip

def skip():
    """The mocking skip: step, hop on the same foot as the other knee drives up high, land, the other side. The arms
    swing wide against the legs (elbows a little bent, out like a puppet's), the shoulders twist against the hips,
    the body bounces and leans in, and the head stays dead level, locked on its prey, a little tilted."""
    e = Emote("clown_sautille", "Clown : pas sautillé",
              "Il sautille d'un pied sur l'autre, genoux très hauts, bras qui balancent comme une marionnette ; "
              "le corps rebondit, la tête reste droite et fixe. Marche avec.", 20)
    land = {  # landing on the right foot: knees giving, the left leg trailing behind, the arms crossing the middle
        "body": {"y": -.07, "pitch": -13, "roll": -2, "yaw": 2},
        "torso": {"bend": 14, "yaw": 0},
        "rightLeg": {"pitch": -14, "bend": 32, "roll": 2},
        "leftLeg": {"pitch": 28, "bend": 46, "roll": -2},
        "rightArm": {"pitch": -10, "roll": 16, "bend": -28, "yaw": 0},
        "leftArm": {"pitch": -12, "roll": -16, "bend": -28, "yaw": 0},
        "head": {"pitch": -9, "roll": 9, "yaw": 0},
    }
    push = {  # off the right foot: the left knee coming through, the body rising
        "body": {"y": .07, "pitch": -12, "roll": -4, "yaw": 5},
        "torso": {"bend": 10, "yaw": -8},
        "rightLeg": {"pitch": 6, "bend": 10, "roll": 2},
        "leftLeg": {"pitch": -50, "bend": 70, "roll": -3},
        "rightArm": {"pitch": -62, "roll": 20, "bend": -48, "yaw": -6},
        "leftArm": {"pitch": 34, "roll": -22, "bend": -18, "yaw": 4},
        "head": {"pitch": -9, "roll": 12, "yaw": 3},
    }
    peak = {  # in the air: the left knee high, the right leg long beneath, the right arm up and out, the left back
        "body": {"y": .19, "pitch": -11, "roll": -5, "yaw": 7},
        "torso": {"bend": 8, "yaw": -13},
        "rightLeg": {"pitch": 20, "bend": 14, "roll": 3},
        "leftLeg": {"pitch": -86, "bend": 98, "roll": -5},
        "rightArm": {"pitch": -98, "roll": 28, "bend": -58, "yaw": -12},
        "leftArm": {"pitch": 52, "roll": -28, "bend": -24, "yaw": 6},
        "head": {"pitch": -8, "roll": 13, "yaw": 6},
    }
    fall = {  # coming down: the left leg reaching for the ground
        "body": {"y": .07, "pitch": -12, "roll": -3, "yaw": 4},
        "torso": {"bend": 11, "yaw": -7},
        "rightLeg": {"pitch": 24, "bend": 26, "roll": 2},
        "leftLeg": {"pitch": -34, "bend": 22, "roll": -3},
        "rightArm": {"pitch": -58, "roll": 22, "bend": -40, "yaw": -6},
        "leftArm": {"pitch": 26, "roll": -20, "bend": -20, "yaw": 3},
        "head": {"pitch": -9, "roll": 11, "yaw": 3},
    }
    e.key(0, land).key(3, push).key(5, peak).key(8, fall)
    e.key(10, merge(mirror(land), {"head": {"roll": 9}}))
    e.key(13, merge(mirror(push), {"head": {"roll": 7}}))
    e.key(15, merge(mirror(peak), {"head": {"roll": 6}}))
    e.key(18, merge(mirror(fall), {"head": {"roll": 8}}))
    # The wrists of the forward arm flick on each hop (the forearm a little further)
    e.layer(lambda t: {"rightArm": {"bend": -8 * max(0, wave(t, 20, .05))}, "leftArm": {"bend": -8 * max(0, -wave(t, 20, .05))}})
    return e


# ------------------------------------------------------------------------------------------------ the dance

def dance():
    """The clown's dance, 64 ticks (two bars of four beats): first a bouncing jig, knees giving on every beat, hips
    swaying from side to side, the elbows bent and the arms flapping like broken wings, the head swinging against the
    hips; then the wild bar: arms flung up overhead one after the other, legs kicked out in turn, the body twisting,
    the head thrown back and dropped."""
    e = Emote("clown_danse", "Clown : la danse",
              "Une gigue folle : il rebondit sur ses genoux, balance les hanches, bat des bras comme des ailes "
              "cassées, puis lance les bras au ciel et les jambes en l'air, la tête renversée.", 64)
    B = 8  # a beat
    # Bar one: the jig
    for i in range(4):
        t = i * B
        side = 1 if i % 2 == 0 else -1
        down = {
            "body": {"y": -.13, "x": .09 * side, "roll": 8 * side, "yaw": 12 * side, "pitch": -6},
            "torso": {"bend": 16, "yaw": -14 * side, "roll": 0},
            "rightLeg": {"pitch": -24, "bend": 52, "roll": 8},
            "leftLeg": {"pitch": -24, "bend": 52, "roll": -8},
            "rightArm": {"pitch": -30, "roll": 62, "bend": -100, "yaw": 0},
            "leftArm": {"pitch": -30, "roll": -62, "bend": -100, "yaw": 0},
            "head": {"roll": -20 * side, "yaw": -22 * side, "pitch": 6},
        }
        up = {
            "body": {"y": .05, "x": .03 * side, "roll": 2 * side, "yaw": 4 * side, "pitch": -2},
            "torso": {"bend": 6, "yaw": -4 * side, "roll": 0},
            "rightLeg": {"pitch": -4, "bend": 6, "roll": 4},
            "leftLeg": {"pitch": -4, "bend": 6, "roll": -4},
            "rightArm": {"pitch": -62, "roll": 78, "bend": -64, "yaw": 0},
            "leftArm": {"pitch": -62, "roll": -78, "bend": -64, "yaw": 0},
            "head": {"roll": -6 * side, "yaw": -6 * side, "pitch": -4},
        }
        # one leg lifts a little on the up (a jig): the side's
        lift = "rightLeg" if side > 0 else "leftLeg"
        up[lift] = {"pitch": -38, "bend": 58, "roll": 6 * side}
        e.key(t, down).key(t + 4, up)
    # Bar two: the wild one
    for i in range(4):
        t = 32 + i * B
        side = 1 if i % 2 == 0 else -1
        hi, lo = ("rightArm", "leftArm") if side > 0 else ("leftArm", "rightArm")
        kick = "leftLeg" if side > 0 else "rightLeg"
        stand = "rightLeg" if side > 0 else "leftLeg"
        fling = {
            "body": {"y": .1, "x": -.04 * side, "roll": -6 * side, "yaw": 22 * side, "pitch": 4},
            "torso": {"bend": -12, "yaw": 10 * side, "roll": -4 * side},
            hi: {"pitch": -168, "roll": 22 * side, "bend": -18, "yaw": 0},
            lo: {"pitch": -36, "roll": -70 * side, "bend": -96, "yaw": 0},
            kick: {"pitch": -64, "bend": 8, "roll": -10 * side},
            stand: {"pitch": 6, "bend": 18, "roll": 4 * side},
            "head": {"pitch": -34, "roll": 18 * side, "yaw": -10 * side},
        }
        drop = {
            "body": {"y": -.08, "x": .02 * side, "roll": 3 * side, "yaw": 8 * side, "pitch": -10},
            "torso": {"bend": 22, "yaw": 0, "roll": 0},
            hi: {"pitch": -70, "roll": 40 * side, "bend": -70, "yaw": 0},
            lo: {"pitch": -20, "roll": -40 * side, "bend": -60, "yaw": 0},
            kick: {"pitch": -10, "bend": 34, "roll": -4 * side},
            stand: {"pitch": -14, "bend": 40, "roll": 4 * side},
            "head": {"pitch": 22, "roll": -12 * side, "yaw": 8 * side},
        }
        e.key(t + 2, fling).key(t + 6, drop)
    # The forearms flap twice a beat in the jig; the head nods with the beat, a shade late
    e.layer(lambda t: {"rightArm": {"pitch": 14 * wave(t, 4) if t < 32 else 0},
                       "leftArm": {"pitch": -14 * wave(t, 4) if t < 32 else 0},
                       "head": {"pitch": 4 * wave(t - 1, 8)}})
    return e


# ------------------------------------------------------------------------------------------------ the tilted head

def tilt():
    """Standing still, the head tilts over onto the shoulder (a slow overshoot, settling), the shoulders sag, the arms
    hang; then it breathes, sways, watches, and twitches: a snap of the head with a jerk of the arm, later a sharp turn
    of the head the other way, held, then slowly back."""
    e = Emote("clown_tete_penchee", "Clown : tête penchée",
              "Immobile, la tête se penche lentement sur l'épaule ; il respire, se balance, observe… et sursaute.", 130, 30)
    rest = {"body": {"pitch": 0, "x": 0}, "torso": {"bend": 0, "roll": 0},
            "head": {"roll": 0, "pitch": 0, "yaw": 0},
            "rightArm": {"pitch": 0, "roll": 4, "bend": -4}, "leftArm": {"pitch": 0, "roll": -4, "bend": -4},
            "leftLeg": {"bend": 0}, "rightLeg": {"bend": 0}}
    tilted = {"body": {"pitch": -5, "x": 0}, "torso": {"bend": 9, "roll": -3},
              "head": {"roll": 60, "pitch": 6, "yaw": -4},
              "rightArm": {"pitch": -6, "roll": 2, "bend": -14}, "leftArm": {"pitch": -10, "roll": -9, "bend": -12},
              "leftLeg": {"bend": 8}, "rightLeg": {"bend": 2}}
    e.key(0, rest).key(4, rest)
    e.key(24, merge(tilted, {"head": {"roll": 67, "pitch": 9}}))
    e.key(30, tilted)
    e.key(54, tilted)
    # twitch one: a snap of the head, the right arm jerked up, the body jolted
    e.key(56, merge(tilted, {"head": {"roll": 80, "pitch": -12, "yaw": 6}, "rightArm": {"pitch": -34, "bend": -50, "roll": 10},
                             "body": {"x": .03}, "torso": {"bend": 4}}))
    e.key(59, merge(tilted, {"head": {"roll": 54, "pitch": 4}, "rightArm": {"pitch": -12, "bend": -20}}))
    e.key(66, tilted)
    e.key(90, tilted)
    # twitch two: the head turned sharp to its left, held, then slowly back
    e.key(92, merge(tilted, {"head": {"yaw": -42, "roll": 48, "pitch": -6}, "torso": {"roll": 4}}))
    e.key(102, merge(tilted, {"head": {"yaw": -40, "roll": 50, "pitch": -4}, "torso": {"roll": 4}}))
    e.key(122, tilted)
    # breath (the chest rising), a slow sway, the head drifting as it watches
    e.layer(lambda t: {} if t < 26 else {"torso": {"bend": 2.2 * wave(t, 50)}, "body": {"roll": 1.8 * wave(t, 100, .25)},
                                          "head": {"roll": 2.5 * wave(t, 100, .6), "yaw": 5 * wave(t, 100, .1)},
                                          "rightArm": {"pitch": 2 * wave(t, 100, .3)}, "leftArm": {"pitch": -2 * wave(t, 100, .3)}})
    return e


# ------------------------------------------------------------------------------------------------ sitting

def sit():
    """Sitting on the ground, knees drawn up and hugged, rocking slowly back and forth, the head on one side, looking
    up from under the brow; now and then the head rolls over to the other side."""
    e = Emote("clown_assis", "Clown : assis, se balance",
              "Assis par terre, il serre ses genoux et se balance doucement d'avant en arrière, la tête penchée, "
              "les yeux levés vers toi.", 120, 0)
    base = {"body": {"y": -.66, "pitch": 0},
            "torso": {"bend": 26},
            "rightLeg": {"pitch": -118, "bend": 120, "roll": 6}, "leftLeg": {"pitch": -118, "bend": 120, "roll": -6},
            "rightArm": {"pitch": -58, "roll": -16, "bend": -64, "yaw": 10}, "leftArm": {"pitch": -58, "roll": 16, "bend": -64, "yaw": -10},
            "head": {"pitch": -26, "roll": 24, "yaw": 0}}
    e.key(0, base)
    e.key(54, base)
    e.key(66, merge(base, {"head": {"pitch": -24, "roll": -24, "yaw": 4}}))
    e.key(108, merge(base, {"head": {"pitch": -24, "roll": -24, "yaw": 4}}))
    e.key(118, base)
    # rocking: back and forth about the hips, the head lagging behind it
    e.layer(lambda t: {"body": {"pitch": 9 * wave(t, 40)}, "torso": {"bend": 4 * wave(t, 40, .1)},
                       "head": {"pitch": -5 * wave(t, 40, .2)}})
    return e


def ledge():
    """Sitting on a ledge (stand at the edge of a block, facing out): legs dangling and swinging like a child's, the
    hands holding the edge, the head lolling slowly from one shoulder to the other."""
    e = Emote("clown_assis_rebord", "Clown : assis au bord",
              "Assis au bord d'un bloc (mets-toi au bord, face au vide), les jambes qui se balancent comme un enfant, "
              "la tête qui roule d'une épaule à l'autre.", 96, 0)
    base = {"body": {"y": -.7, "z": -.12, "pitch": 0},
            "torso": {"bend": 12},
            "rightLeg": {"pitch": -88, "bend": 90, "roll": 3}, "leftLeg": {"pitch": -88, "bend": 90, "roll": -3},
            "rightArm": {"pitch": 18, "roll": 18, "bend": -8}, "leftArm": {"pitch": 18, "roll": -18, "bend": -8},
            "head": {"pitch": -4}}
    e.key(0, base)
    # legs swinging in turn; the head rolling slowly from side to side, a little late on the legs
    e.layer(lambda t: {"rightLeg": {"bend": 30 * wave(t, 24)}, "leftLeg": {"bend": -30 * wave(t, 24)},
                       "head": {"roll": 26 * wave(t, 96), "yaw": 8 * wave(t, 96, .25)},
                       "torso": {"roll": 3 * wave(t, 96, .5)},
                       "rightArm": {"pitch": 2 * wave(t, 24)}, "leftArm": {"pitch": -2 * wave(t, 24)}})
    return e


# ------------------------------------------------------------------------------------------------ with something in hand

def offer():
    """Holding something out (whatever is in the right hand: a balloon...), the arm not quite stretched, the other
    hand behind the back, leaning in, the head on one side; the hand bobbing the thing gently, come and take it."""
    e = Emote("clown_tend_objet", "Clown : tend l'objet",
              "Il te tend ce qu'il a en main droite (un ballon…), le bras pas tout à fait tendu, l'autre main dans le "
              "dos, penché vers toi, la tête sur le côté. Viens le prendre…", 60, 0)
    base = {"body": {"pitch": -6},
            "torso": {"bend": 9, "yaw": -6},
            "rightArm": {"pitch": -50, "roll": 6, "yaw": -12, "bend": -20},
            "leftArm": {"pitch": 32, "roll": 18, "bend": -78, "yaw": 0},
            "rightLeg": {"pitch": -6, "bend": 4}, "leftLeg": {"pitch": 6, "bend": 10},
            "head": {"roll": -20, "pitch": -2, "yaw": 6}}
    e.key(0, base)
    e.key(30, merge(base, {"head": {"roll": -26, "pitch": 2, "yaw": 2}}))
    e.layer(lambda t: {"rightArm": {"pitch": 4 * wave(t, 20), "bend": -4 * wave(t, 20, .2)},
                       "body": {"roll": 1.5 * wave(t, 60)}, "torso": {"bend": 1.5 * wave(t, 60, .25)}})
    return e


def beckon():
    """Come here: the right forearm curling in toward him, twice, slow out, quick in; leaning a little more each time;
    the head on one side, chin down, eyes up."""
    e = Emote("clown_viens", "Clown : viens…",
              "Il te fait signe de venir, l'avant-bras qui se replie lentement vers lui, la tête penchée, le menton "
              "baissé, les yeux levés.", 40, 0)
    out = {"body": {"pitch": -6}, "torso": {"bend": 10},
           "rightArm": {"pitch": -66, "roll": 10, "yaw": -14, "bend": -22},
           "leftArm": {"pitch": 0, "roll": -6, "bend": -10},
           "head": {"roll": 22, "pitch": 10, "yaw": 0}}
    curl = merge(out, {"rightArm": {"pitch": -74, "roll": 10, "yaw": -14, "bend": -118}, "body": {"pitch": -9}, "torso": {"bend": 13},
                       "head": {"roll": 26, "pitch": 12}})
    e.key(0, out).key(8, out).key(14, curl).key(20, out).key(28, out).key(34, curl)
    return e


def giggle():
    """The giggle: hunched, the hands to the mouth, the shoulders shaking; then the head thrown back in a wide laugh,
    arms flung open, and back into the hunch."""
    e = Emote("clown_rire", "Clown : rire",
              "Recroquevillé, les mains devant la bouche, les épaules qui tressautent… puis il renverse la tête et "
              "éclate de rire, les bras ouverts.", 70, 0)
    hunch = {"body": {"pitch": -6, "y": -.03}, "torso": {"bend": 24, "roll": 0},
             "rightArm": {"pitch": -46, "roll": -18, "bend": -128, "yaw": 14},
             "leftArm": {"pitch": -46, "roll": 18, "bend": -128, "yaw": -14},
             "rightLeg": {"bend": 12}, "leftLeg": {"bend": 12},
             "head": {"pitch": 8, "roll": 10}}
    laugh = {"body": {"pitch": 6, "y": .02}, "torso": {"bend": -14, "roll": 0},
             "rightArm": {"pitch": -40, "roll": 52, "bend": -46, "yaw": 0},
             "leftArm": {"pitch": -40, "roll": -52, "bend": -46, "yaw": 0},
             "rightLeg": {"bend": 4}, "leftLeg": {"bend": 4},
             "head": {"pitch": -42, "roll": -6}}
    e.key(0, hunch).key(34, hunch).key(40, laugh).key(54, laugh).key(62, hunch)
    # the shaking: quick, small, the head with it; stronger in the laugh
    e.layer(lambda t: {"torso": {"bend": (2.5 if t < 36 or t > 60 else 4) * wave(t, 3)},
                       "body": {"y": .012 * wave(t, 3, .25)},
                       "head": {"pitch": 3 * wave(t, 3, .5)}})
    return e


def peekaboo():
    """Peek-a-boo: the hands over the face, swaying a little; then flung open, leaning in all at once."""
    e = Emote("clown_coucou", "Clown : coucou !",
              "Les mains devant le visage, il se balance… puis les ouvre d'un coup en se penchant vers toi : coucou !", 70, 0)
    hide = {"body": {"pitch": -2, "y": 0}, "torso": {"bend": 8},
            "rightArm": {"pitch": -78, "roll": -26, "bend": -112, "yaw": 18},
            "leftArm": {"pitch": -78, "roll": 26, "bend": -112, "yaw": -18},
            "rightLeg": {"bend": 4}, "leftLeg": {"bend": 4},
            "head": {"pitch": 14, "roll": 4, "yaw": 0}}
    boo = {"body": {"pitch": -16, "y": .04}, "torso": {"bend": 20},
           "rightArm": {"pitch": -64, "roll": 74, "bend": -26, "yaw": 0},
           "leftArm": {"pitch": -64, "roll": -74, "bend": -26, "yaw": 0},
           "rightLeg": {"bend": 14}, "leftLeg": {"bend": 14},
           "head": {"pitch": -18, "roll": 14, "yaw": 0}}
    e.key(0, hide).key(22, merge(hide, {"head": {"pitch": 18}, "torso": {"bend": 10}}))
    e.key(25, merge(boo, {"body": {"pitch": -20, "y": .06}, "rightArm": {"roll": 82}, "leftArm": {"roll": -82}}))
    e.key(28, boo).key(46, boo).key(64, hide)
    e.layer(lambda t: {"body": {"roll": (3 if t < 22 else .5) * wave(t, 22)}, "head": {"roll": (4 if t < 22 else 1) * wave(t, 22, .15)}})
    return e


def bow():
    """A theatrical bow: the right foot slides back, the right arm sweeps across the belly, the left flung out, a deep
    bend; the head stays up, looking at you the whole way down; held, then slowly up."""
    e = Emote("clown_reverence", "Clown : révérence",
              "Une grande révérence de théâtre : un pied glisse en arrière, un bras sur le ventre, l'autre ouvert, "
              "il se plie… sans jamais te quitter des yeux.", 90, 0)
    stand = {"body": {"pitch": 0, "y": 0}, "torso": {"bend": 0},
             "rightLeg": {"pitch": 0, "bend": 0}, "leftLeg": {"pitch": 0, "bend": 0},
             "rightArm": {"pitch": 0, "roll": 4, "bend": -4, "yaw": 0}, "leftArm": {"pitch": 0, "roll": -4, "bend": -4, "yaw": 0},
             "head": {"pitch": 0, "roll": 0}}
    low = {"body": {"pitch": -14, "y": -.06}, "torso": {"bend": 48},
           "rightLeg": {"pitch": 26, "bend": 22}, "leftLeg": {"pitch": -10, "bend": 22},
           "rightArm": {"pitch": -58, "roll": -42, "bend": -84, "yaw": 20},
           "leftArm": {"pitch": -24, "roll": -78, "bend": -10, "yaw": 0},
           "head": {"pitch": -54, "roll": 10}}
    e.key(0, stand).key(6, stand)
    e.key(14, merge(stand, {"rightLeg": {"pitch": 18, "bend": 8}, "leftArm": {"pitch": -40, "roll": -60, "bend": -20},
                            "rightArm": {"pitch": -30, "roll": -20, "bend": -60, "yaw": 10}}))
    e.key(28, low).key(60, merge(low, {"head": {"roll": 18}})).key(80, stand)
    return e


def puppet():
    """A puppet on strings: hanging, limp, feet barely on the ground; one string at a time pulled (an arm yanked up, a
    knee, the head) and let go to drop and bounce; the body swaying like something hung."""
    e = Emote("clown_marionnette", "Clown : marionnette",
              "Pendu à des fils invisibles, mou, à peine posé sur la pointe des pieds ; un fil tire un bras, un genou, "
              "la tête… qui retombent.", 96, 0)
    limp = {"body": {"y": .04, "pitch": -3}, "torso": {"bend": 14},
            "rightArm": {"pitch": 4, "roll": 8, "bend": -8, "yaw": 0}, "leftArm": {"pitch": 4, "roll": -8, "bend": -8, "yaw": 0},
            "rightLeg": {"pitch": 4, "bend": 10, "roll": 2}, "leftLeg": {"pitch": 6, "bend": 12, "roll": -2},
            "head": {"pitch": 34, "roll": 22, "yaw": 0}}
    e.key(0, limp).key(6, limp)
    # the right arm yanked up, dropped, a bounce
    e.key(9, merge(limp, {"rightArm": {"pitch": -128, "roll": 30, "bend": -64}, "torso": {"bend": 6}}))
    e.key(14, merge(limp, {"rightArm": {"pitch": 10, "roll": 6, "bend": -4}}))
    e.key(17, merge(limp, {"rightArm": {"pitch": -14, "roll": 10, "bend": -16}}))
    e.key(22, limp)
    # the left knee pulled up, dropped
    e.key(28, merge(limp, {"leftLeg": {"pitch": -70, "bend": 84}, "body": {"y": .08, "roll": -4}}))
    e.key(33, merge(limp, {"leftLeg": {"pitch": 8, "bend": 6}}))
    e.key(36, merge(limp, {"leftLeg": {"pitch": -12, "bend": 22}}))
    e.key(42, limp)
    # the head yanked up, held, let go: it lolls to the other side
    e.key(48, merge(limp, {"head": {"pitch": -24, "roll": 0}, "torso": {"bend": 2}, "body": {"pitch": 2}}))
    e.key(54, merge(limp, {"head": {"pitch": -22, "roll": -4}, "torso": {"bend": 2}}))
    e.key(60, merge(limp, {"head": {"pitch": 36, "roll": -26}}))
    e.key(64, merge(limp, {"head": {"pitch": 28, "roll": -20}}))
    # both arms pulled out like a scarecrow's, dropped
    e.key(72, merge(limp, {"rightArm": {"pitch": -10, "roll": 88, "bend": -30}, "leftArm": {"pitch": -10, "roll": -88, "bend": -30},
                           "head": {"pitch": 20, "roll": -18}, "body": {"y": .1}}))
    e.key(78, merge(limp, {"rightArm": {"pitch": 6, "roll": 4}, "leftArm": {"pitch": 6, "roll": -4}, "head": {"pitch": 30, "roll": 4}}))
    e.key(82, merge(limp, {"rightArm": {"roll": 18, "bend": -14}, "leftArm": {"roll": -18, "bend": -14}, "head": {"pitch": 32, "roll": 14}}))
    e.key(90, limp)
    e.layer(lambda t: {"body": {"roll": 3 * wave(t, 48), "yaw": 6 * wave(t, 96)},
                       "rightLeg": {"roll": -1.5 * wave(t, 48)}, "leftLeg": {"roll": -1.5 * wave(t, 48)}})
    return e


ALL = (skip, dance, tilt, sit, ledge, offer, beckon, giggle, peekaboo, bow, puppet)


def main():
    if os.path.isdir(OUT):
        for f in os.listdir(OUT):
            if f.endswith(".json"):
                os.remove(os.path.join(OUT, f))
    for fn in ALL:
        fn().write()


if __name__ == "__main__":
    main()
