#!/usr/bin/env python3
"""A small rig for the fighters' body animations, written out as Player Animator's Bedrock-format JSON.

Player Animator (which Iron's plays casting animations with) moves the vanilla player model's parts one by one: the
arms and the head are not attached to the torso, so a torso twist leaves the shoulders where they were, and every key
time is cut down to a whole tick ((int) (seconds * 20) in float, so 0.35 s lands on tick 6). This rig puts the
hierarchy back and keys every tick:

- body: the whole model, turned about the hips (lean, turn, spin) and moved (lunge, drop into a stance);
- twist: the chest turning about the spine, carrying the shoulders and the arms;
- arms: turned relative to the chest, plus a shoulder shift (reaching out, pulling back);
- legs: turned relative to the ground, so a lean of the body does not tilt them (a turn or a spin carries them);
- items: the held item turned in the hand;
- grip: the left hand brought onto the right hand's weapon (two-handed weapons), aimed at a point along it.

The head is never keyed, so it keeps following where the player looks.

Units and axes (the vanilla model's): angles in degrees, positions in model pixels. x is toward the player's left,
y is down, z is backward. A part turned by (x, y, z) is Rz(z) * Ry(y) * Rx(x): an arm at x -90 points straight
ahead, a positive y turns the front of a part toward the player's right, a positive z swings the right arm out to the
side and the left arm across the chest. The body's lean is a positive x (forward), its turn a positive y (to the
right), a positive z leans it to the left. Its position is the Bedrock one: x left, y up, z backward.

An animation is a set of tracks, each a list of keys (tick, value, easing into that key). It is sampled at every
tick and each sample is written as its own key, so what plays is exactly what was sampled; the preview
(anim_preview.py) draws it back from the JSON the way Player Animator plays it.
"""
import math

import numpy as np

TICK = .05
#: The body turns about the hips, 0.7 block above the feet (12.07 model pixels below the neck).
HIPS = np.array([0, 12.069, 0])
#: The player model is drawn at 15/16 scale.
SCALE = .9375
PIVOTS = {
    "right_arm": np.array([-5., 2, 0]),
    "left_arm": np.array([5., 2, 0]),
    "right_leg": np.array([-1.9, 12, .1]),
    "left_leg": np.array([1.9, 12, .1]),
}


# ---------------------------------------------------------------- rotations

def rx(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


def ry(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


def rz(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def rot(v):
    """Rz * Ry * Rx of a vector of degrees, the order model parts turn in."""
    x, y, z = (math.radians(a) for a in v)
    return rz(z) @ ry(y) @ rx(x)


def _near(a, ref):
    """{@code a} plus whole turns, as close to {@code ref} as can be."""
    return a + 2 * math.pi * round((ref - a) / (2 * math.pi))


def euler(m, prev=None):
    """Degrees (x, y, z) with Rz * Ry * Rx = m, the solution closest to {@code prev} (so a sequence never jumps a
    whole turn: Player Animator interpolates the raw values)."""
    sy = max(-1., min(1., -m[2, 0]))
    y = math.asin(sy)
    p = [math.radians(a) for a in prev] if prev is not None else [0., 0., 0.]
    if abs(math.cos(y)) > 1e-6:
        x = math.atan2(m[2, 1], m[2, 2])
        z = math.atan2(m[1, 0], m[0, 0])
        candidates = [(x, y, z), (x + math.pi, math.pi - y, z + math.pi)]
    else:
        # Straight up or down: only x - z (or x + z) is fixed; keep x where it was
        x = p[0]
        rest = m @ (ry(y) @ rx(x)).T
        candidates = [(x, y, math.atan2(rest[1, 0], rest[0, 0]))]
    best = None
    for c in candidates:
        c = tuple(_near(a, r) for a, r in zip(c, p))
        d = sum((a - r) ** 2 for a, r in zip(c, p))
        if best is None or d < best[0]:
            best = (d, c)
    return tuple(math.degrees(a) for a in best[1])


# ---------------------------------------------------------------- easings and tracks

def _back(f, s=1.70158):
    f -= 1
    return 1 + (s + 1) * f ** 3 + s * f ** 2


EASES = {
    "linear": lambda f: f,
    "in": lambda f: f * f,
    "out": lambda f: 1 - (1 - f) ** 2,
    "inout": lambda f: .5 - .5 * math.cos(math.pi * f),
    "in3": lambda f: f ** 3,
    "out3": lambda f: 1 - (1 - f) ** 3,
    "inout3": lambda f: 4 * f ** 3 if f < .5 else 1 - (-2 * f + 2) ** 3 / 2,
    # Overshoots a little and settles: a blow that stops on its target
    "back": _back,
    "back_soft": lambda f: _back(f, .9),
    # Most of the way at once: a snap
    "snap": lambda f: 1 if f >= 1 else 1 - 2 ** (-10 * f),
}

CHANNELS = {
    "body_pos": (0, 0, 0), "body": (0, 0, 0), "twist": 0,
    "right_arm": None, "left_arm": None, "right_arm_shift": (0, 0, 0), "left_arm_shift": (0, 0, 0),
    "right_leg": None, "left_leg": None, "right_item": None, "left_item": None,
    # How much the left hand holds the weapon in the right (0 free, 1 on it), and where along it: 0 is the bottom
    # of the item's picture, 1 its top (a sword's pommel and point)
    "grip": 0, "grip_at": .2,
}


def sample(track, t):
    """The value of a track [(tick, value, easing), ...] at tick {@code t}."""
    if t <= track[0][0]:
        return np.array(track[0][1], dtype=float)
    for (t0, v0, *_), (t1, v1, *e) in zip(track, track[1:]):
        if t <= t1:
            f = (t - t0) / (t1 - t0) if t1 > t0 else 1
            f = EASES[e[0] if e else "inout"](f)
            return np.array(v0, dtype=float) + (np.array(v1, dtype=float) - np.array(v0, dtype=float)) * f
    return np.array(track[-1][1], dtype=float)


class Anim:
    """An animation: its length in ticks and its tracks (missing ones stay at rest; a missing arm, leg or item is
    left to vanilla)."""

    def __init__(self, ticks, **tracks):
        for name in tracks:
            if name not in CHANNELS:
                raise ValueError(f"unknown channel {name}")
        self.ticks = ticks
        self.tracks = tracks

    def pose(self, t):
        pose = {}
        for name, rest in CHANNELS.items():
            if name in self.tracks:
                pose[name] = sample(self.tracks[name], t)
            elif rest is not None:
                pose[name] = np.array(rest, dtype=float)
        return pose


# ---------------------------------------------------------------- where the right hand's item is

def _m4(r=None, t=(0, 0, 0)):
    m = np.eye(4)
    if r is not None:
        m[:3, :3] = r
    m[:3, 3] = t
    return m


def item_point(arm_rot, arm_at, item_rot, u):
    """Model-space point (pixels, in the body's frame) at {@code u} along the diagonal of the right hand's item
    picture, through vanilla's third-person handheld transforms (as anim_preview.py draws it)."""
    m = _m4(arm_rot, arm_at)
    m = m @ _m4(rx(math.radians(-90))) @ _m4(ry(math.pi)) @ _m4(t=(1, 2, -10))
    if item_rot is not None:
        m = m @ _m4(rot(item_rot))
    m = m @ _m4(t=(0, 4, .5)) @ _m4(ry(math.radians(-90)) @ rz(math.radians(55))) @ np.diag([.85, .85, .85, 1]) @ _m4(t=(-8, -8, -8))
    return (m @ np.array([u * 16, u * 16, 8, 1]))[:3]


def aim_arm(shoulder, target):
    """Degrees (x, y, 0) turning an arm hung at {@code shoulder} to point at {@code target}."""
    d = target - shoulder
    d = d / np.linalg.norm(d)
    x = -math.acos(max(-1., min(1., d[1])))
    y = math.atan2(-d[0], -d[2]) if abs(math.sin(x)) > 1e-6 else 0
    return np.array([math.degrees(x), math.degrees(y), 0])


# ---------------------------------------------------------------- the pose as Player Animator's flat values

def solve(pose, prev=None):
    """{bone: {"rotation": (x, y, z), "position": (x, y, z)}} in Player Animator's JSON terms for one pose."""
    prev = prev or {}
    out = {}
    body = tuple(pose["body"])
    out["body"] = {"rotation": body, "position": tuple(pose["body_pos"])}
    twist = float(pose["twist"])
    chest = ry(math.radians(twist))
    out["torso"] = {"rotation": (0, twist, 0)}
    grip = float(pose["grip"])
    for arm in ("right_arm", "left_arm"):
        if arm not in pose:
            continue
        m = chest @ rot(pose[arm])
        at = chest @ (PIVOTS[arm] + pose[arm + "_shift"])
        if arm == "left_arm" and grip > 0 and "right_arm" in pose:
            right = chest @ rot(pose["right_arm"])
            right_at = chest @ (PIVOTS["right_arm"] + pose["right_arm_shift"])
            target = item_point(right, right_at, pose.get("right_item"), float(pose["grip_at"]))
            free = np.array(euler(m))
            held = aim_arm(at, target)
            held = np.array([a + 360 * round((f - a) / 360) for a, f in zip(held, free)])
            m = rot(free + (held - free) * grip)
        d = at - PIVOTS[arm]
        out[arm] = {"rotation": euler(m, prev.get(arm, {}).get("rotation")), "position": (d[0], -d[1], d[2])}
    lean = rot(body)
    turn = ry(math.radians(body[1]))
    for leg in ("right_leg", "left_leg"):
        if leg not in pose:
            continue
        m = lean.T @ turn @ rot(pose[leg])
        out[leg] = {"rotation": euler(m, prev.get(leg, {}).get("rotation"))}
    for item in ("right_item", "left_item"):
        if item in pose:
            out[item] = {"rotation": tuple(pose[item])}
    return out


def _key(tick):
    # A quarter tick in, so the float truncation lands on the right tick
    return f"{(tick + .25) * TICK:.4f}"


def to_json(anim):
    """The Bedrock-format animation: one key per tick for every keyed bone."""
    bones = {}
    prev = None
    for tick in range(anim.ticks + 1):
        flat = solve(anim.pose(tick), prev)
        prev = flat
        for bone, channels in flat.items():
            for channel, v in channels.items():
                bones.setdefault(bone, {}).setdefault(channel, {})[_key(tick)] = {"vector": [round(float(a), 3) for a in v]}
    # Drop channels that never move from rest
    for bone in list(bones):
        for channel in list(bones[bone]):
            if all(all(abs(a) < 1e-3 for a in k["vector"]) for k in bones[bone][channel].values()) and bone in ("body", "torso"):
                del bones[bone][channel]
        if not bones[bone]:
            del bones[bone]
    return {"animation_length": round((anim.ticks - .25) * TICK, 4), "bones": bones}
