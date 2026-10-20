#!/usr/bin/env python3
"""The Boss Magic casters' body animations (player_animation/boss_spells.json), written with the rig in anim_rig.py.

Big, slow, theatrical gestures: the caster of a boss attack should look like they are calling it down. Each ends back
at rest. Preview one with anim_preview.py. Angles in degrees, positions in model pixels (see anim_rig.py): arm x -90
points ahead, -180 straight up; a right arm's positive z swings it out to the side (a left arm's negative z); a
negative twist brings the right shoulder forward; a positive body x leans forward.
"""
from anim_rig import Anim, to_json

REST = (0, 0, 0)
#: Feet planted wide, the weight low.
WIDE_R = (-6, 10, 9)
WIDE_L = (6, -10, -9)


def _planted(start, end, settle=3, release=4, right=WIDE_R, left=WIDE_L):
    return {
        "right_leg": [(0, REST), (start, REST), (start + settle, right), (end - release, right), (end, REST)],
        "left_leg": [(0, REST), (start, REST), (start + settle, left), (end - release, left), (end, REST)],
    }


def sky_call():
    """Both arms raised to the sky, the body arching back as if lifting the heavens, a held breath, then both arms
    thrown down to call what falls. The arms come down on tick 13."""
    return Anim(26, **_planted(1, 26), **{
        "body": [(0, REST), (3, (2, 0, 0)), (9, (-14, 0, 0), "out"), (11, (-16, 0, 0)), (13, (12, 0, 0), "in3"), (17, (10, 0, 0)), (26, REST)],
        "body_pos": [(0, REST), (9, (0, .8, 0), "out"), (11, (0, 1, 0)), (13, (0, -1.4, -.6), "in3"), (17, (0, -1.2, -.5)), (26, REST)],
        "right_arm": [(0, REST), (3, (-40, 0, 30)), (9, (-172, -6, 22), "out"), (11, (-176, -6, 26)), (13, (-62, 4, 30), "in3"), (17, (-55, 4, 28)), (26, REST)],
        "left_arm": [(0, REST), (3, (-40, 0, -30)), (9, (-172, 6, -22), "out"), (11, (-176, 6, -26)), (13, (-62, -4, -30), "in3"), (17, (-55, -4, -28)), (26, REST)],
        "right_arm_shift": [(0, REST), (9, (0, -1, 0)), (13, (0, 0, -1)), (26, REST)],
        "left_arm_shift": [(0, REST), (9, (0, -1, 0)), (13, (0, 0, -1)), (26, REST)],
    })


def blade_call():
    """The right arm raised high as if holding the hilt of a blade the size of a tower, the left hand pointing at
    the strip, then the cut brought down across the body on tick 11."""
    return Anim(24, **_planted(1, 24, right=(-8, 14, 8), left=(10, -8, -6)), **{
        "body": [(0, REST), (8, (-8, 0, 0), "out"), (11, (16, 0, 0), "in3"), (15, (14, 0, 0)), (24, REST)],
        "body_pos": [(0, REST), (8, (0, .6, .4)), (11, (0, -1.2, -1.2), "in3"), (15, (0, -1, -1)), (24, REST)],
        "twist": [(0, 0), (8, 24, "out"), (11, -26, "in3"), (15, -22), (24, 0)],
        "right_arm": [(0, REST), (8, (-178, 0, 12), "out"), (11, (-42, 14, -18), "in3"), (15, (-38, 12, -16)), (24, REST)],
        "right_arm_shift": [(0, REST), (8, (0, -1.2, .5)), (11, (0, 0, -1.5), "in3"), (24, REST)],
        "left_arm": [(0, REST), (5, (-80, -14, -6)), (8, (-92, -10, -4)), (11, (-30, 0, -14), "out"), (24, REST)],
    })


def ground_slam():
    """Both fists raised overhead, the body rising, then driven into the ground before the feet, the body folding
    over them. The fists strike the ground on tick 11."""
    return Anim(26, **{
        "body": [(0, REST), (8, (-12, 0, 0), "out"), (11, (34, 0, 0), "in3"), (16, (30, 0, 0)), (26, REST)],
        "body_pos": [(0, REST), (8, (0, 1.2, .8), "out"), (11, (0, -3.6, -1.6), "in3"), (16, (0, -3.2, -1.3)), (26, REST)],
        "right_arm": [(0, REST), (8, (-170, -6, 14), "out"), (11, (-38, -6, 8), "in3"), (16, (-34, -6, 8)), (26, REST)],
        "left_arm": [(0, REST), (8, (-170, 6, -14), "out"), (11, (-38, 6, -8), "in3"), (16, (-34, 6, -8)), (26, REST)],
        "right_arm_shift": [(0, REST), (8, (0, -1, 0)), (11, (0, 0, -1), "in3"), (26, REST)],
        "left_arm_shift": [(0, REST), (8, (0, -1, 0)), (11, (0, 0, -1), "in3"), (26, REST)],
        "right_leg": [(0, REST), (8, (4, 0, 6)), (11, (-26, 0, 12), "out"), (20, (-26, 0, 12)), (26, REST)],
        "left_leg": [(0, REST), (8, (-4, 0, -6)), (11, (28, 0, -12), "out"), (20, (28, 0, -12)), (26, REST)],
    })


def beam():
    """Both hands thrust forward, palms together, the body braced against the recoil of what they hold back, a slow
    shudder, then let go."""
    hands_r, hands_l = (-90, 20, 0), (-90, -20, 0)
    shudder_r = [(5 + k, (hands_r[0] - (2 if k % 2 else 0), hands_r[1], hands_r[2])) for k in range(1, 15)]
    shudder_l = [(5 + k, (hands_l[0] - (0 if k % 2 else 2), hands_l[1], hands_l[2])) for k in range(1, 15)]
    return Anim(28, **_planted(1, 28, right=(10, 12, 6), left=(-18, -8, -6)), **{
        "body": [(0, REST), (5, (8, 0, 0), "out"), (8, (-4, 0, 0)), (20, (-3, 0, 0)), (28, REST)],
        "body_pos": [(0, REST), (5, (0, -.6, -1.2), "out"), (8, (0, -.6, .3)), (20, (0, -.6, .2)), (28, REST)],
        "right_arm": [(0, REST), (5, hands_r, "out3")] + shudder_r + [(22, (-80, 16, 4)), (28, REST)],
        "left_arm": [(0, REST), (5, hands_l, "out3")] + shudder_l + [(22, (-80, -16, -4)), (28, REST)],
        "right_arm_shift": [(0, REST), (5, (0, 0, -2), "out3"), (8, (0, 0, -1)), (20, (0, 0, -1)), (28, REST)],
        "left_arm_shift": [(0, REST), (5, (0, 0, -2), "out3"), (8, (0, 0, -1)), (20, (0, 0, -1)), (28, REST)],
    })


def prison():
    """Arms flung wide as if spreading a cage around the target, then brought together before the chest, the hands
    closing on tick 13."""
    return Anim(24, **_planted(1, 24), **{
        "body": [(0, REST), (6, (-8, 0, 0), "out"), (13, (6, 0, 0), "in3"), (17, (5, 0, 0)), (24, REST)],
        "body_pos": [(0, REST), (6, (0, .5, 0)), (13, (0, -.6, -.5), "in3"), (24, REST)],
        "right_arm": [(0, REST), (6, (-20, 0, 98), "out"), (9, (-28, 0, 102)), (13, (-92, 28, 0), "in3"), (17, (-90, 26, 0)), (24, REST)],
        "left_arm": [(0, REST), (6, (-20, 0, -98), "out"), (9, (-28, 0, -102)), (13, (-92, -28, 0), "in3"), (17, (-90, -26, 0)), (24, REST)],
        "right_arm_shift": [(0, REST), (6, (-.6, 0, 0)), (13, (0, 0, -1.5), "in3"), (24, REST)],
        "left_arm_shift": [(0, REST), (6, (.6, 0, 0)), (13, (0, 0, -1.5), "in3"), (24, REST)],
    })


def animations():
    anims = {
        "boss_sky_call": sky_call(),
        "boss_blade_call": blade_call(),
        "boss_ground_slam": ground_slam(),
        "boss_beam": beam(),
        "boss_prison": prison(),
    }
    return {"format_version": "1.8.0", "animations": {name: to_json(a) for name, a in anims.items()}}
