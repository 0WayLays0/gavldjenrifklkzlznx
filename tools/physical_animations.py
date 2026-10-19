#!/usr/bin/env python3
"""The fighters' body animations (player_animation/physical_skills.json), written with the rig in anim_rig.py.

Each move is keyed in ticks and its blows land on the tick its skill times them on (the HIT_TICK constants of the
skills). Iron's fades every casting animation in over its first two ticks, so a wind-up shows from tick 2 on; every
move ends back at rest so the fade out to the vanilla pose is short. Preview one with anim_preview.py.

Angles in degrees, positions in model pixels; see anim_rig.py for the axes. In short: arm x -90 points ahead, a
negative twist brings the right shoulder forward, a positive body x leans forward, a negative body y turns the hips
to the left (the right side forward), legs at a negative x step forward.
"""
from anim_rig import Anim, to_json

REST = (0, 0, 0)
#: Fighting stance: left foot forward, right foot back and turned out.
STANCE_L = (-16, -12, -4)
STANCE_R = (17, 22, 5)


def _legs(start, end, settle=2, release=3, left=STANCE_L, right=STANCE_R, ease_in="inout"):
    """Both legs into the stance by {@code start + settle} and back to rest by {@code end}."""
    return {
        "left_leg": [(start, REST), (start + settle, left, ease_in), (end - release, left), (end, REST)],
        "right_leg": [(start, REST), (start + settle, right, ease_in), (end - release, right), (end, REST)],
    }


# ---------------------------------------------------------------- bare hands

def punch():
    """Straight right: the rear fist chambered at the hip, then driven out to head height of whoever stands in front,
    hips and shoulders turning in behind it and the left hand snapping back to the hip. Lands on tick 4."""
    return Anim(12, **_legs(0, 12), **{
        "body": [(0, REST), (2, (2, 0, -2)), (4, (12, 0, 2), "out3"), (6, (10, 0, 1)), (12, REST)],
        "body_pos": [(0, REST), (2, (0, 0, .6)), (4, (0, -.5, -1.8), "out3"), (6, (0, -.4, -1.5)), (12, REST)],
        "twist": [(0, 0), (2, 25), (4, -38, "out3"), (6, -32), (12, 0)],
        "right_arm": [(0, REST), (2, (30, 0, 12)), (4, (-97, 16, 0), "out3"), (6, (-92, 14, 2)), (9, (-40, 6, 6)), (12, REST)],
        "right_arm_shift": [(0, REST), (2, (0, 0, 1.5)), (4, (0, 0, -2.5), "out3"), (6, (0, 0, -2)), (12, REST)],
        "left_arm": [(0, REST), (2, (-55, 25, -6)), (4, (28, 0, -8), "out3"), (7, (22, 0, -6)), (12, REST)],
        "left_arm_shift": [(0, REST), (2, (0, 0, -1)), (4, (0, 0, 1.5), "out3"), (7, (0, 0, 1)), (12, REST)],
    })


def punch_flurry():
    """Seven jabs from the guard, right and left every other tick, the chest turning behind each one, then the rear
    fist chambered and driven home. Jabs land on ticks 3 to 15, the last punch on tick 20."""
    jabs = list(range(3, 16, 2))
    guard_r, jab_r = (-72, -14, 4), (-95, 10, 0)
    guard_l, jab_l = (-72, 14, -4), (-95, -10, 0)
    right, left = [(0, REST), (2, guard_r)], [(0, REST), (2, guard_l)]
    right_shift, left_shift = [(0, REST), (2, (0, 0, 1))], [(0, REST), (2, (0, 0, 1))]
    twist = [(0, 0), (2, 0)]
    for i, t in enumerate(jabs):
        arm, shift, out, guard = (right, right_shift, jab_r, guard_r) if i % 2 == 0 else (left, left_shift, jab_l, guard_l)
        arm += [(t - 1, guard), (t, out, "out"), (t + 1.5, guard)]
        shift += [(t - 1, (0, 0, 1)), (t, (0, 0, -2.5), "out"), (t + 1.5, (0, 0, 1))]
        twist.append((t, -18 if i % 2 == 0 else 18, "out"))
    # The finisher: the right fist back to the hip, then home
    right += [(18, (30, 0, 12)), (20, (-97, 16, 0), "out3"), (22, (-92, 14, 2)), (24, (-40, 6, 6)), (27, REST)]
    right_shift += [(18, (0, 0, 1.5)), (20, (0, 0, -2.5), "out3"), (22, (0, 0, -2)), (27, REST)]
    left += [(17, guard_l), (18, (-55, 25, -6)), (20, (28, 0, -8), "out3"), (23, (22, 0, -6)), (27, REST)]
    left_shift += [(18, (0, 0, -1)), (20, (0, 0, 1.5), "out3"), (23, (0, 0, 1)), (27, REST)]
    twist += [(16, 0), (18, 25), (20, -38, "out3"), (22, -32), (27, 0)]
    bob = [(0, REST), (2, (8, 0, 0))] + [(t, (10, 0, 0) if i % 2 == 0 else (7, 0, 0)) for i, t in enumerate(jabs)]
    bob += [(18, (3, 0, -2)), (20, (13, 0, 2), "out3"), (22, (10, 0, 1)), (27, REST)]
    return Anim(27, **_legs(0, 27), **{
        "body": bob,
        "body_pos": [(0, REST), (2, (0, -.3, 0)), (16, (0, -.3, 0)), (18, (0, 0, .6)), (20, (0, -.6, -2), "out3"), (22, (0, -.5, -1.6)), (27, REST)],
        "twist": twist,
        "right_arm": right, "right_arm_shift": right_shift, "left_arm": left, "left_arm_shift": left_shift,
    })


def slash_flurry():
    """Forehand, backhand, forehand, then a wide level cut: the blade crosses on ticks 3, 8, 13 and 18."""
    high, low = (-150, -30, 50), (-20, 30, 70)
    arm = [(0, (-18, 0, 0)), (1, high)]
    twist = [(0, 0), (1, 25)]
    for k, t in enumerate((3, 8, 13)):
        start, end = (high, low) if k % 2 == 0 else (low, high)
        arm += [(t - 1.5, start), (t + .5, end, "inout3")]
        twist += [(t - 1.5, 25 if k % 2 == 0 else -25), (t + .5, -25 if k % 2 == 0 else 25, "inout3")]
    arm += [(16, (-95, -60, 90)), (19, (-95, 70, 90), "inout3"), (21, (-80, 60, 70)), (25, (-18, 0, 0))]
    twist += [(16, 35), (19, -40, "inout3"), (21, -32), (25, 0)]
    return Anim(25, **_legs(0, 25), **{
        "body": [(0, REST), (2, (8, 0, 0)), (15, (8, 0, 0)), (19, (12, 0, 0)), (25, REST)],
        "body_pos": [(0, REST), (2, (0, -.4, 0)), (15, (0, -.4, 0)), (19, (0, -1, -1.2)), (21, (0, -.9, -1)), (25, REST)],
        "twist": twist, "right_arm": arm,
        "left_arm": [(0, REST), (2, (-25, 0, -18)), (15, (-25, 0, -18)), (19, (-10, 0, -40)), (25, REST)],
    })


def dash():
    """Leaning into a burst of speed, arms swept back, legs in full stride; the dash lasts ticks 0 to 5."""
    lean = [(0, REST), (1, (28, 0, 0), "out"), (5, (24, 0, 0)), (7, REST)]
    return Anim(7, **{
        "body": lean,
        "body_pos": [(0, REST), (1, (0, -1.5, 0), "out"), (5, (0, -1.5, 0)), (7, REST)],
        "right_arm": [(0, REST), (1, (50, 0, 14), "out"), (5, (45, 0, 12)), (7, REST)],
        "left_arm": [(0, REST), (1, (50, 0, -14), "out"), (5, (45, 0, -12)), (7, REST)],
        "right_leg": [(0, REST), (1, (-38, 0, 2), "out"), (5, (-34, 0, 2)), (7, REST)],
        "left_leg": [(0, REST), (1, (34, 0, -2), "out"), (5, (30, 0, -2)), (7, REST)],
    })


def leap():
    """A crouch, a spring with both arms thrown up, the legs tucked at the top, then reaching down to land."""
    return Anim(14, **{
        "body": [(0, (12, 0, 0)), (2, (-6, 0, 0), "out"), (8, (6, 0, 0)), (14, REST)],
        "body_pos": [(0, (0, -2.5, 0)), (2, REST, "out"), (14, REST)],
        "right_arm": [(0, (40, 0, 12)), (2, (-160, 0, 16), "out"), (8, (-100, 0, 22)), (14, REST)],
        "left_arm": [(0, (40, 0, -12)), (2, (-160, 0, -16), "out"), (8, (-100, 0, -22)), (14, REST)],
        "right_leg": [(0, (-24, 0, 4)), (2, (12, 0, 2), "out"), (7, (-40, 0, 4)), (11, (-10, 0, 2)), (14, REST)],
        "left_leg": [(0, (-20, 0, -4)), (2, (-12, 0, -2), "out"), (7, (-34, 0, -4)), (11, (-6, 0, -2)), (14, REST)],
    })


# ---------------------------------------------------------------- sword

#: The blade turned to point where the arm points (a thrust) rather than up.
POINT = (-90, 0, 0)


def sword_lunge():
    """A fencer's lunge: on guard, the point held level at whoever is in front, then the front foot shoots out, the
    body drops and the sword arm drives straight through while the other arm flies back for balance. The point lands
    on tick 7, as the body travels forward from tick 3."""
    guard = (-70, -18, 4)
    return Anim(17, **{
        "body": [(0, REST), (2, (6, 0, 0)), (3, (6, 0, 0)), (7, (16, 0, 0), "out"), (10, (15, 0, 0)), (17, REST)],
        "body_pos": [(0, REST), (2, (0, -1, 1)), (3, (0, -1, 1)), (7, (0, -3, -2), "out"), (10, (0, -2.8, -1.8)), (17, REST)],
        "twist": [(0, 0), (2, 20), (3, 18), (7, -28, "out3"), (10, -24), (17, 0)],
        "right_arm": [(0, (-18, 0, 0)), (2, guard), (3, guard), (7, (-112, 24, 0), "out3"), (10, (-110, 22, 0)), (14, (-40, 4, 4)),
                      (17, (-18, 0, 0))],
        "right_arm_shift": [(0, REST), (2, (0, 0, 2)), (3, (0, 0, 2)), (7, (0, 0, -3), "out3"), (10, (0, 0, -2.5)), (17, REST)],
        "right_item": [(0, REST), (2, (-59, 0, 0)), (3, (-59, 0, 0)), (7, POINT, "out3"), (12, POINT), (17, REST)],
        "left_arm": [(0, REST), (2, (-30, 0, -20)), (3, (-30, 0, -20)), (7, (45, 0, -28), "out"), (10, (40, 0, -24)), (17, REST)],
        "right_leg": [(0, REST), (2, (-22, 0, 3)), (3, (-24, 0, 3)), (7, (-42, 0, 2), "out"), (10, (-40, 0, 2)), (17, REST)],
        "left_leg": [(0, REST), (2, (18, -25, -5)), (3, (18, -25, -5)), (7, (40, -25, -4), "out"), (10, (38, -25, -4)), (17, REST)],
    })


# ---------------------------------------------------------------- dagger

def dagger_stabs():
    """Three short stabs from the hip, low and close, the point always level: to the belly, to the chest, then a step
    in and a deep one to the throat. The point lands on ticks 3, 6 and 9."""
    lean = 10
    stabs = [(3, -86, 6), (6, -96, -6), (9, -108, 10)]
    chamber = (-48, 0, 8)
    arm = [(0, (-18, 0, 0)), (2, chamber)]
    item = [(0, REST), (2, (chamber[0] + lean, 0, 0))]
    shift = [(0, REST), (2, (0, 0, 2))]
    twist = [(0, 0), (2, 15)]
    for t, pitch, yaw in stabs:
        arm += [(t, (pitch, yaw, 0), "out3"), (t + 2, chamber)]
        item += [(t, (pitch + lean, 0, 0), "out3"), (t + 2, (chamber[0] + lean, 0, 0))]
        shift += [(t, (0, 0, -3), "out3"), (t + 2, (0, 0, 2))]
        twist += [(t, -24, "out3"), (t + 2, 12)]
    arm[-1] = (13, (-60, 4, 6))
    arm.append((16, (-18, 0, 0)))
    item[-1] = (13, (-50, 0, 0))
    item.append((16, REST))
    shift[-1] = (13, (0, 0, 1))
    shift.append((16, REST))
    twist[-1] = (13, -8)
    twist.append((16, 0))
    return Anim(16, **{
        "body": [(0, REST), (2, (lean, 0, 0)), (8, (lean, 0, 0)), (9, (lean + 6, 0, 0), "out"), (12, (lean + 4, 0, 0)), (16, REST)],
        "body_pos": [(0, REST), (2, (0, -1, 0)), (8, (0, -1, 0)), (9, (0, -1.8, -2), "out"), (12, (0, -1.6, -1.6)), (16, REST)],
        "twist": twist,
        "right_arm": arm, "right_arm_shift": shift, "right_item": item,
        "left_arm": [(0, REST), (2, (-58, 18, -8)), (8, (-58, 18, -8)), (9, (-20, 10, -14), "out"), (12, (-24, 10, -12)), (16, REST)],
        "right_leg": [(0, REST), (2, (-16, 0, 3)), (8, (-16, 0, 3)), (9, (-30, 0, 3), "out"), (12, (-28, 0, 3)), (16, REST)],
        "left_leg": [(0, REST), (2, (16, -20, -5)), (8, (16, -20, -5)), (9, (26, -20, -5), "out"), (12, (24, -20, -5)), (16, REST)],
    })


#: The vanilla arm pose while holding an item, where weapon moves start and end.
HOLD = (-18, 0, 0)


def _weapon(ticks, **tracks):
    """A weapon move: the right arm starts and ends in vanilla's holding pose (the tracks given do the rest)."""
    return Anim(ticks, **tracks)


# ---------------------------------------------------------------- moving

def evade():
    """A big leap backward: a quick crouch, a push off both feet with the arms thrown forward, knees up in the air,
    then landing low. The body travels back from tick 2 to tick 10."""
    return Anim(17, **{
        "body": [(0, REST), (2, (12, 0, 0)), (4, (-22, 0, 0), "out"), (9, (-12, 0, 0)), (11, (16, 0, 0), "out"), (13, (12, 0, 0)), (17, REST)],
        "body_pos": [(0, REST), (2, (0, -2.2, 0)), (4, (0, 0, 0), "out"), (11, (0, -2.4, 0), "out"), (13, (0, -2, 0)), (17, REST)],
        "right_arm": [(0, REST), (2, (30, 0, 10)), (4, (-110, 0, 18), "out"), (9, (-80, 0, 22)), (11, (-50, 0, 16)), (17, REST)],
        "left_arm": [(0, REST), (2, (30, 0, -10)), (4, (-110, 0, -18), "out"), (9, (-80, 0, -22)), (11, (-50, 0, -16)), (17, REST)],
        "right_leg": [(0, REST), (2, (-26, 0, 4)), (4, (10, 0, 3), "out"), (7, (-50, 0, 6)), (10, (-14, 0, 4)), (11, (-28, 0, 4)), (17, REST)],
        "left_leg": [(0, REST), (2, (-22, 0, -4)), (4, (14, 0, -3), "out"), (7, (-42, 0, -6)), (10, (-10, 0, -4)), (11, (-24, 0, -4)), (17, REST)],
    })


# ---------------------------------------------------------------- scythe

def scythe_hook():
    """Reaper's Hook: both hands on the haft, the scythe lifted high, then brought down far in front so the blade
    hooks behind whoever stands there, and dragged back to the hip with the body leaning away. Hooks on tick 9."""
    return _weapon(22, grip=[(0, 0), (3, 1), (18, 1), (22, 0)], grip_at=[(0, .12)], **_legs(0, 22), **{
        "body": [(0, REST), (5, (-8, 0, 0)), (9, (22, 0, 0), "out3"), (11, (18, 0, 0)), (15, (-10, 0, 0), "inout"), (22, REST)],
        "body_pos": [(0, REST), (5, (0, .5, .5)), (9, (0, -2, -2), "out3"), (11, (0, -1.8, -1.6)), (15, (0, -.5, 1.5)), (22, REST)],
        "twist": [(0, 0), (5, 22), (9, -12, "out3"), (15, -20), (22, 0)],
        "right_arm": [(0, HOLD), (5, (-168, -10, 12)), (9, (-72, 6, 6), "out3"), (11, (-68, 6, 6)), (15, (-15, 0, 12), "inout"), (22, HOLD)],
        "right_arm_shift": [(0, REST), (5, (0, -1, 0)), (9, (0, 0, -2), "out3"), (15, (0, 0, 2)), (22, REST)],
        "right_item": [(0, REST), (5, (-40, 0, 0)), (9, (-30, 0, 0)), (15, (-10, 0, 0)), (22, REST)],
        "left_arm": [(0, REST), (22, REST)],
    })


# ---------------------------------------------------------------- sword

def sword_disarm():
    """Disarm: the point engages the enemy's blade, binds it in a tight circle and flicks it up and away out of
    their grip. The flick lands on tick 6."""
    return _weapon(15, **{
        "body": [(0, REST), (3, (8, 0, 0)), (6, (12, 0, 0)), (9, (6, 0, 0)), (15, REST)],
        "body_pos": [(0, REST), (3, (0, -.6, -1)), (6, (0, -.8, -1.6)), (15, REST)],
        "twist": [(0, 0), (3, -15), (5, 10), (6, -22, "out"), (9, -15), (15, 0)],
        "right_arm": [(0, HOLD), (3, (-88, 10, 0)), (4.5, (-96, -18, 0)), (5.5, (-84, -4, 0)), (6, (-128, 14, 0), "out"), (9, (-120, 10, 0)),
                      (15, HOLD)],
        "right_arm_shift": [(0, REST), (3, (0, 0, -2)), (6, (0, 0, -2.5)), (15, REST)],
        "right_item": [(0, REST), (3, (-80, 0, 0)), (4.5, (-80, 0, 30)), (5.5, (-80, 0, -30)), (6, (-40, 0, 0), "out"), (9, (-40, 0, 0)), (15, REST)],
        "left_arm": [(0, REST), (3, (-30, 0, -24)), (9, (-30, 0, -24)), (15, REST)],
        **_legs(0, 15, left=(-12, -10, -3), right=(14, 20, 4)),
    })


# ---------------------------------------------------------------- dagger

def dagger_finisher():
    """Coup de Grâce: the dagger turned point down in the fist and raised high, then driven down into the enemy in
    front with the whole body dropping behind it. The point goes in on tick 6."""
    return _weapon(16, **{
        "body": [(0, REST), (4, (-6, 0, 0)), (6, (22, 0, 0), "out3"), (9, (20, 0, 0)), (16, REST)],
        "body_pos": [(0, REST), (4, (0, .8, .5)), (6, (0, -2.6, -1.8), "out3"), (9, (0, -2.4, -1.6)), (16, REST)],
        "twist": [(0, 0), (4, 18), (6, -18, "out3"), (9, -14), (16, 0)],
        "right_arm": [(0, HOLD), (4, (-172, -6, 8)), (6, (-62, 6, 4), "out3"), (9, (-58, 6, 4)), (13, (-40, 0, 6)), (16, HOLD)],
        "right_arm_shift": [(0, REST), (4, (0, -1, .5)), (6, (0, 0, -2), "out3"), (16, REST)],
        # Point down in the fist: level ahead while it is raised, slanting down into the enemy as it comes down
        "right_item": [(0, REST), (3, (-172, 0, 0)), (4, (-172, 0, 0)), (6, (-110, 0, 0), "out3"), (11, (-110, 0, 0)), (16, REST)],
        "left_arm": [(0, REST), (4, (-60, 20, -10)), (6, (-20, 10, -20), "out3"), (9, (-24, 10, -18)), (16, REST)],
        **_legs(0, 16, left=(-20, -10, -3), right=(20, 20, 4)),
    })


# ---------------------------------------------------------------- hammer

def hammer_concussion():
    """Concussive Blow: the hammer drawn back past the right shoulder with both hands, then swung flat across at head
    height, the hips and shoulders unwinding behind it. It connects on tick 8."""
    return _weapon(19, grip=[(0, 0), (3, 1), (13, 1), (17, 0)], grip_at=[(0, .1)], **_legs(0, 19), **{
        "body": [(0, REST), (5, (2, 0, 0)), (8, (10, 0, 0), "out3"), (11, (8, 0, 0)), (19, REST)],
        "body_pos": [(0, REST), (5, (0, -.5, .8)), (8, (0, -1.2, -1.5), "out3"), (11, (0, -1, -1.2)), (19, REST)],
        "twist": [(0, 0), (5, 40), (8, -20, "out3"), (11, -42), (19, 0)],
        "right_arm": [(0, HOLD), (5, (-96, 62, 6)), (8, (-92, -6, 0), "out3"), (11, (-88, -50, 0)), (19, HOLD)],
        "right_arm_shift": [(0, REST), (5, (0, 0, 1.5)), (8, (0, 0, -1.5), "out3"), (19, REST)],
        "right_item": [(0, REST), (4, (-70, 0, 0)), (13, (-70, 0, 0)), (19, REST)],
        "left_arm": [(0, REST), (19, REST)],
    })


def hammer_quake():
    """Earthshaker: the hammer heaved high over the head with both hands, the body rising, then brought down into
    the ground in front with everything behind it. It strikes the ground on tick 11."""
    return _weapon(25, grip=[(0, 0), (3, 1), (20, 1), (24, 0)], grip_at=[(0, .1)], **{
        "body": [(0, REST), (8, (-14, 0, 0)), (11, (32, 0, 0), "out3"), (15, (28, 0, 0)), (25, REST)],
        "body_pos": [(0, REST), (8, (0, 1, .8)), (11, (0, -3.4, -1.5), "out3"), (15, (0, -3, -1.2)), (25, REST)],
        "right_arm": [(0, HOLD), (8, (-175, -8, 10)), (11, (-48, -4, 4), "out3"), (15, (-44, -4, 4)), (25, HOLD)],
        "right_arm_shift": [(0, REST), (8, (0, -1, 0)), (11, (0, 0, -1), "out3"), (25, REST)],
        "right_item": [(0, REST), (8, (-50, 0, 0)), (11, (-40, 0, 0)), (20, (-40, 0, 0)), (25, REST)],
        "left_arm": [(0, REST), (25, REST)],
        "right_leg": [(0, REST), (8, (4, 0, 6)), (11, (-24, 0, 10), "out"), (20, (-24, 0, 10)), (25, REST)],
        "left_leg": [(0, REST), (8, (-4, 0, -6)), (11, (26, 0, -10), "out"), (20, (26, 0, -10)), (25, REST)],
    })


def hammer_ram():
    """Battering Ram: the hammer held level in both hands, head forward, pulled back to the chest, then rammed
    straight out with a step in. It slams on tick 7."""
    return _weapon(16, grip=[(0, 0), (2, 1), (12, 1), (15, 0)], grip_at=[(0, .55)], **_legs(0, 16, left=(-22, -8, -3)), **{
        "body": [(0, REST), (4, (-4, 0, 0)), (7, (16, 0, 0), "out3"), (10, (14, 0, 0)), (16, REST)],
        "body_pos": [(0, REST), (4, (0, -.6, 1.2)), (7, (0, -1.6, -3), "out3"), (10, (0, -1.4, -2.6)), (16, REST)],
        "twist": [(0, 0), (4, 14), (7, -10, "out3"), (16, 0)],
        "right_arm": [(0, HOLD), (4, (-58, 4, 8)), (7, (-102, 4, 0), "out3"), (10, (-100, 4, 0)), (16, HOLD)],
        "right_arm_shift": [(0, REST), (4, (0, 0, 3)), (7, (0, 0, -3.5), "out3"), (10, (0, 0, -3)), (16, REST)],
        "right_item": [(0, REST), (3, POINT), (12, POINT), (16, REST)],
        "left_arm": [(0, REST), (16, REST)],
    })


# ---------------------------------------------------------------- axe

def axe_cleave():
    """Cleave: the axe raised straight over the head, then chopped down hard on whoever is in front, the body
    folding over it. It bites on tick 8."""
    return _weapon(17, **_legs(0, 17), **{
        "body": [(0, REST), (5, (-8, 0, 0)), (8, (24, 0, 0), "out3"), (11, (20, 0, 0)), (17, REST)],
        "body_pos": [(0, REST), (5, (0, .5, .5)), (8, (0, -2.2, -1.5), "out3"), (11, (0, -2, -1.2)), (17, REST)],
        "twist": [(0, 0), (5, 12), (8, -10, "out3"), (17, 0)],
        "right_arm": [(0, HOLD), (5, (-176, 4, 6)), (8, (-50, 2, 2), "out3"), (11, (-46, 2, 2)), (17, HOLD)],
        "right_arm_shift": [(0, REST), (5, (0, -1, 1)), (8, (0, 0, -1.5), "out3"), (17, REST)],
        "right_item": [(0, REST), (5, (-30, 0, 0)), (8, (-20, 0, 0)), (17, REST)],
        "left_arm": [(0, REST), (5, (-40, 0, -20)), (8, (20, 0, -24), "out3"), (11, (16, 0, -22)), (17, REST)],
    })


def axe_throw():
    """Axe Throw: the axe drawn back behind the head, the free arm pointing at the target, then whipped forward
    over the shoulder and let go on tick 7."""
    return _weapon(16, **_legs(0, 16), **{
        "body": [(0, REST), (5, (-12, 0, 0)), (7, (16, 0, 0), "out3"), (10, (14, 0, 0)), (16, REST)],
        "body_pos": [(0, REST), (5, (0, 0, 1)), (7, (0, -1.4, -1.6), "out3"), (16, REST)],
        "twist": [(0, 0), (5, 35), (7, -30, "out3"), (10, -26), (16, 0)],
        "right_arm": [(0, HOLD), (5, (-200, 0, 14)), (7, (-110, 4, 6), "out3"), (9, (-40, 4, 8)), (16, HOLD)],
        "right_item": [(0, REST), (5, (-20, 0, 0)), (7, (-60, 0, 0)), (12, REST)],
        "left_arm": [(0, REST), (5, (-92, 10, -8)), (7, (-30, 0, -18), "out3"), (10, (-20, 0, -14)), (16, REST)],
    })


def axe_sunder():
    """Armor Splitter: the axe cocked high over the right shoulder, then brought down on a slant through the
    enemy's guard and armor, from the right shoulder to the left hip. It bites on tick 8."""
    return _weapon(18, **_legs(0, 18), **{
        "body": [(0, REST), (5, (-6, 0, 4)), (8, (18, 0, -4), "out3"), (11, (16, 0, -3)), (18, REST)],
        "body_pos": [(0, REST), (5, (0, .3, .5)), (8, (0, -1.8, -1.4), "out3"), (11, (0, -1.6, -1.2)), (18, REST)],
        "twist": [(0, 0), (5, 32), (8, -26, "out3"), (11, -32), (18, 0)],
        "right_arm": [(0, HOLD), (5, (-155, 34, 34)), (8, (-46, -32, -4), "out3"), (11, (-40, -36, -4)), (18, HOLD)],
        "right_arm_shift": [(0, REST), (5, (0, -1, 1)), (8, (0, 0, -1.5), "out3"), (18, REST)],
        "right_item": [(0, REST), (5, (-30, 0, 0)), (8, (-20, 0, 0)), (18, REST)],
        "left_arm": [(0, REST), (5, (-50, 10, -20)), (8, (10, 0, -26), "out3"), (11, (8, 0, -24)), (18, REST)],
    })


# ---------------------------------------------------------------- spear

def spear_thrust():
    """Long Thrust: the spear held low in both hands, point forward, drawn back, then driven out full length with
    a step in. The point lands on tick 5."""
    return _weapon(15, grip=[(0, 0), (2, 1), (11, 1), (14, 0)], grip_at=[(0, .62)], **{
        "body": [(0, REST), (3, (4, 0, 0)), (5, (16, 0, 0), "out3"), (8, (14, 0, 0)), (15, REST)],
        "body_pos": [(0, REST), (3, (0, -1, 1)), (5, (0, -2.4, -2.6), "out3"), (8, (0, -2.2, -2.2)), (15, REST)],
        "twist": [(0, 0), (3, 24), (5, -14, "out3"), (8, -10), (15, 0)],
        "right_arm": [(0, HOLD), (3, (-46, 14, 10)), (5, (-100, 8, 2), "out3"), (8, (-98, 8, 2)), (15, HOLD)],
        "right_arm_shift": [(0, REST), (3, (0, 0, 3)), (5, (0, 0, -3), "out3"), (8, (0, 0, -2.5)), (15, REST)],
        "right_item": [(0, REST), (2, (-58, 0, 0)), (3, (-58, 0, 0)), (5, (-94, 0, 0), "out3"), (11, (-94, 0, 0)), (15, REST)],
        "left_arm": [(0, REST), (15, REST)],
        "right_leg": [(0, REST), (3, (12, 18, 4)), (5, (26, 18, 4), "out"), (11, (24, 18, 4)), (15, REST)],
        "left_leg": [(0, REST), (3, (-14, -10, -3)), (5, (-36, -10, -3), "out"), (11, (-34, -10, -3)), (15, REST)],
    })


def spear_charge():
    """Charge: the spear couched under the arm, point low and forward, the body leaning into a run with the legs
    striding; the run lasts ticks 1 to 10."""
    stride = [(t, (-40, 0, 2) if k % 2 == 0 else (34, 0, 2)) for k, t in enumerate(range(2, 11, 2))]
    stride_l = [(t, (34, 0, -2) if k % 2 == 0 else (-40, 0, -2)) for k, t in enumerate(range(2, 11, 2))]
    return _weapon(16, grip=[(0, 0), (2, 1), (11, 1), (14, 0)], grip_at=[(0, .55)], **{
        "body": [(0, REST), (2, (22, 0, 0)), (10, (22, 0, 0)), (12, (8, 0, 0)), (16, REST)],
        "body_pos": [(0, REST), (2, (0, -1.4, 0)), (10, (0, -1.4, 0)), (16, REST)],
        "twist": [(0, 0), (2, -10), (10, -10), (16, 0)],
        "right_arm": [(0, HOLD), (2, (-86, 10, 6)), (10, (-86, 10, 6)), (16, HOLD)],
        "right_arm_shift": [(0, REST), (2, (0, 0, 2)), (10, (0, 0, 2)), (16, REST)],
        "right_item": [(0, REST), (2, (-74, 0, 0)), (11, (-74, 0, 0)), (16, REST)],
        "left_arm": [(0, REST), (16, REST)],
        "right_leg": [(0, REST)] + stride + [(12, REST)],
        "left_leg": [(0, REST)] + stride_l + [(12, REST)],
    })


def spear_javelin():
    """Javelin: the spear lifted over the shoulder, point forward, the free arm aiming, the body leaning back, then
    a long throwing stride and the arm whipping through. It leaves the hand on tick 8."""
    return _weapon(18, **{
        "body": [(0, REST), (6, (-12, 0, 0)), (8, (18, 0, 0), "out3"), (11, (16, 0, 0)), (18, REST)],
        "body_pos": [(0, REST), (6, (0, -.6, 1)), (8, (0, -1.8, -2), "out3"), (18, REST)],
        "twist": [(0, 0), (6, 38), (8, -30, "out3"), (11, -26), (18, 0)],
        "right_arm": [(0, HOLD), (6, (-160, 4, 18)), (8, (-96, 4, 6), "out3"), (10, (-40, 4, 8)), (18, HOLD)],
        "right_arm_shift": [(0, REST), (6, (0, -1, 2)), (8, (0, 0, -2), "out3"), (18, REST)],
        "right_item": [(0, REST), (5, (-110, 0, 0)), (8, (-80, 0, 0)), (12, REST)],
        "left_arm": [(0, REST), (6, (-96, 10, -6)), (8, (-24, 0, -20), "out3"), (11, (-20, 0, -16)), (18, REST)],
        "right_leg": [(0, REST), (6, (14, 20, 4)), (8, (30, 20, 4), "out"), (13, (28, 20, 4)), (18, REST)],
        "left_leg": [(0, REST), (6, (-20, -10, -3)), (8, (-34, -10, -3), "out"), (13, (-32, -10, -3)), (18, REST)],
    })


# ---------------------------------------------------------------- longsword (two hands on the grip)

def _longsword(ticks, **tracks):
    return _weapon(ticks, grip=[(0, 0), (3, 1), (ticks - 4, 1), (ticks - 1, 0)], grip_at=[(0, .1)], left_arm=[(0, REST), (ticks, REST)], **tracks)


def longsword_cut():
    """Wrath Cut: the longsword raised over the right shoulder, then cut down on a slant through the enemy in
    front, from the right shoulder to the left hip, stepping in. It strikes on tick 8."""
    return _longsword(20, **_legs(0, 20), **{
        "body": [(0, REST), (5, (-6, 0, 3)), (8, (18, 0, -3), "out3"), (11, (16, 0, -2)), (20, REST)],
        "body_pos": [(0, REST), (5, (0, 0, .6)), (8, (0, -1.8, -1.8), "out3"), (11, (0, -1.6, -1.5)), (20, REST)],
        "twist": [(0, 0), (5, 30), (8, -28, "out3"), (11, -34), (20, 0)],
        "right_arm": [(0, HOLD), (5, (-150, 30, 26)), (8, (-58, -26, -2), "out3"), (11, (-50, -34, -2)), (20, HOLD)],
        "right_arm_shift": [(0, REST), (5, (0, -1, 1)), (8, (0, 0, -1.5), "out3"), (20, REST)],
        "right_item": [(0, REST), (5, (-40, 0, 0)), (8, (-60, 0, 0)), (11, (-60, 0, 0)), (20, REST)],
    })


def longsword_sweep():
    """Half-Moon: the longsword drawn back low on the right, then swept flat in a wide half circle across the
    front with a step in. The blade crosses the front on tick 8."""
    return _longsword(19, **_legs(0, 19), **{
        "body": [(0, REST), (5, (6, 0, 0)), (8, (12, 0, 0), "out3"), (11, (10, 0, 0)), (19, REST)],
        "body_pos": [(0, REST), (5, (0, -1, .5)), (8, (0, -1.8, -1.4), "out3"), (19, REST)],
        "twist": [(0, 0), (5, 42), (8, -8, "out3"), (11, -45), (19, 0)],
        "right_arm": [(0, HOLD), (5, (-78, 70, 0)), (8, (-84, 6, 0), "out3"), (11, (-80, -66, 0)), (19, HOLD)],
        "right_arm_shift": [(0, REST), (5, (0, 0, 1)), (8, (0, 0, -1.5), "out3"), (19, REST)],
        "right_item": [(0, REST), (4, (-80, 0, 0)), (12, (-80, 0, 0)), (19, REST)],
    })


def longsword_guard():
    """Parry: hands raised before the face, the blade slanted across the body to catch a blow, held through the
    longest parry window (ticks 2 to 20), then lowered."""
    guard = (-104, -24, 0)
    return _longsword(26, **_legs(0, 26, left=(-12, -10, -3), right=(14, 20, 4)), **{
        "body": [(0, REST), (2, (4, 0, 0)), (20, (4, 0, 0)), (26, REST)],
        "body_pos": [(0, REST), (2, (0, -.8, .4)), (20, (0, -.8, .4)), (26, REST)],
        "twist": [(0, 0), (2, 12), (20, 12), (26, 0)],
        "right_arm": [(0, HOLD), (2, guard, "out"), (20, guard), (26, HOLD)],
        "right_arm_shift": [(0, REST), (2, (0, -.5, -1)), (20, (0, -.5, -1)), (26, REST)],
        "right_item": [(0, REST), (2, (-20, 0, 40), "out"), (20, (-20, 0, 40)), (26, REST)],
    })


def longsword_riposte():
    """Riposte: straight from the parry, the point driven into the attacker with a step in. It lands on tick 3."""
    return _longsword(12, **_legs(0, 12, settle=1), **{
        "body": [(0, (4, 0, 0)), (3, (16, 0, 0), "out3"), (6, (14, 0, 0)), (12, REST)],
        "body_pos": [(0, (0, -.8, .4)), (3, (0, -2, -2.4), "out3"), (6, (0, -1.8, -2)), (12, REST)],
        "twist": [(0, 12), (3, -20, "out3"), (6, -16), (12, 0)],
        "right_arm": [(0, (-104, -24, 0)), (3, (-104, 16, 0), "out3"), (6, (-100, 14, 0)), (12, HOLD)],
        "right_arm_shift": [(0, (0, -.5, -1)), (3, (0, 0, -3), "out3"), (12, REST)],
        "right_item": [(0, (-20, 0, 40)), (3, POINT, "out3"), (8, POINT), (12, REST)],
    })


# ---------------------------------------------------------------- bow and crossbow

def _bow(aim, lean, ticks=20, release=12):
    """The bow raised in the main hand at {@code aim} degrees, the other hand nocking at the bow and drawing back to
    the cheek, then letting go on tick {@code release}."""
    bow = (aim, -6, 0)
    nock, drawn = (aim, 32, 0), (aim + 2, 62, 0)
    return Anim(ticks, **_legs(0, ticks, left=(-10, -30, -4), right=(10, 30, 4)), **{
        "body": [(0, REST), (4, (lean, 0, 0)), (release + 3, (lean, 0, 0)), (ticks, REST)],
        "twist": [(0, 0), (4, -24), (release, -30), (release + 3, -26), (ticks, 0)],
        "right_arm": [(0, HOLD), (4, bow, "out"), (release + 3, bow), (ticks, HOLD)],
        "left_arm": [(0, REST), (4, nock, "out"), (release - 1, drawn, "inout"), (release, (aim + 4, 78, 0), "out"), (release + 3, (aim + 4, 74, 0)), (ticks, REST)],
        "left_arm_shift": [(0, REST), (4, (0, 0, -1)), (release - 1, (0, 0, 3), "inout"), (release, (0, 0, 4), "out"), (release + 3, (0, 0, 3.5)), (ticks, REST)],
    })


def bow_shot():
    """A shot from the bow, the arrow loosed on tick 12."""
    return _bow(-92, 0)


def bow_rain():
    """A shot high into the sky, leaning back, the arrow loosed on tick 12."""
    return _bow(-150, -16)


def _crossbow(shots, ticks):
    """The crossbow shouldered in both hands and aimed, kicking back at each shot (on the ticks {@code shots})."""
    aim_r, aim_l = (-86, -16, 0), (-88, 34, 0)
    kick_r, kick_l = (-104, -16, 0), (-104, 34, 0)
    right, left, body, shift = [(0, HOLD), (3, aim_r, "out")], [(0, REST), (3, aim_l, "out")], [(0, REST), (3, (2, 0, 0))], [(0, REST), (3, (0, 0, 1))]
    for t in shots:
        right += [(t - .5, aim_r), (t, kick_r, "snap"), (t + 2, aim_r)]
        left += [(t - .5, aim_l), (t, kick_l, "snap"), (t + 2, aim_l)]
        body += [(t - .5, (2, 0, 0)), (t, (-6, 0, 0), "snap"), (t + 2, (2, 0, 0))]
        shift += [(t - .5, (0, 0, 1)), (t, (0, 0, 2.5), "snap"), (t + 2, (0, 0, 1))]
    end = shots[-1] + 2
    right += [(end + 2, aim_r), (ticks, HOLD)]
    left += [(end + 2, aim_l), (ticks, REST)]
    body += [(end + 2, (2, 0, 0)), (ticks, REST)]
    shift += [(end + 2, (0, 0, 1)), (ticks, REST)]
    return Anim(ticks, **_legs(0, ticks, left=(-12, -14, -3), right=(12, 24, 4)), **{
        "body": body, "twist": [(0, 0), (3, -12), (end + 2, -12), (ticks, 0)],
        "right_arm": right, "left_arm": left, "right_arm_shift": shift, "left_arm_shift": shift,
    })


def crossbow_shot():
    """One crossbow shot on tick 4."""
    return _crossbow([4], 12)


def crossbow_burst():
    """Three crossbow shots, on ticks 4, 8 and 12."""
    return _crossbow([4, 8, 12], 20)


# ---------------------------------------------------------------- throwing knives

def knife_throw():
    """Precise Throw: the knife drawn back by the ear, the free hand pointing at the mark, then flicked forward
    overhand and let go on tick 5."""
    return _weapon(12, **_legs(0, 12, left=(-14, -10, -3), right=(14, 20, 4)), **{
        "body": [(0, REST), (3, (-6, 0, 0)), (5, (12, 0, 0), "out3"), (12, REST)],
        "twist": [(0, 0), (3, 28), (5, -24, "out3"), (8, -20), (12, 0)],
        "right_arm": [(0, HOLD), (3, (-188, 0, 16)), (5, (-96, 4, 4), "out3"), (7, (-56, 4, 6)), (12, HOLD)],
        "right_item": [(0, REST), (3, (40, 0, 0)), (5, POINT, "out3"), (8, REST)],
        "left_arm": [(0, REST), (3, (-90, 12, -6)), (5, (-30, 0, -20), "out3"), (12, REST)],
    })


def knife_fan():
    """Fan of Knives: the throwing hand swept from far right across to the left at chest height, the knives let go
    one after another as it crosses the front; the middle one on tick 6."""
    return _weapon(14, **_legs(0, 14, left=(-14, -10, -6), right=(14, 20, 6)), **{
        "body": [(0, REST), (4, (4, 0, 0)), (7, (10, 0, 0)), (14, REST)],
        "body_pos": [(0, REST), (4, (0, -1.4, 0)), (7, (0, -1.6, -.5)), (14, REST)],
        "twist": [(0, 0), (4, 44), (8, -40, "inout3"), (10, -42), (14, 0)],
        "right_arm": [(0, HOLD), (4, (-86, 80, 0)), (8, (-90, -50, 0), "inout3"), (10, (-80, -56, 0)), (14, HOLD)],
        "right_item": [(0, REST), (4, POINT), (10, POINT), (14, REST)],
        "left_arm": [(0, REST), (4, (-30, 0, -20)), (8, (10, 0, -40)), (14, REST)],
    })


def knife_pin():
    """Pinning Throw: crouched low, the knife swung back past the hip, then flung forward underhand at the enemy's
    feet and let go on tick 5."""
    return _weapon(13, **_legs(0, 13, left=(-24, -10, -4), right=(22, 20, 4)), **{
        "body": [(0, REST), (3, (16, 0, 0)), (5, (20, 0, 0)), (13, REST)],
        "body_pos": [(0, REST), (3, (0, -2.4, 0)), (5, (0, -2.6, -1)), (13, REST)],
        "twist": [(0, 0), (3, 22), (5, -16, "out3"), (13, 0)],
        "right_arm": [(0, HOLD), (3, (46, 6, 10)), (5, (-58, 2, 4), "out3"), (7, (-74, 2, 4)), (13, HOLD)],
        "right_item": [(0, REST), (3, (-30, 0, 0)), (5, POINT, "out3"), (8, REST)],
        "left_arm": [(0, REST), (3, (-50, 10, -12)), (5, (-20, 0, -20)), (13, REST)],
    })


def animations():
    anims = {
        "punch": punch(),
        "punch_flurry": punch_flurry(),
        "slash_flurry": slash_flurry(),
        "dash": dash(),
        "leap": leap(),
        "sword_lunge": sword_lunge(),
        "dagger_stabs": dagger_stabs(),
        "evade": evade(),
        "scythe_hook": scythe_hook(),
        "sword_disarm": sword_disarm(),
        "dagger_finisher": dagger_finisher(),
        "hammer_concussion": hammer_concussion(),
        "hammer_quake": hammer_quake(),
        "hammer_ram": hammer_ram(),
        "axe_cleave": axe_cleave(),
        "axe_throw": axe_throw(),
        "axe_sunder": axe_sunder(),
        "spear_thrust": spear_thrust(),
        "spear_charge": spear_charge(),
        "spear_javelin": spear_javelin(),
        "longsword_cut": longsword_cut(),
        "longsword_sweep": longsword_sweep(),
        "longsword_guard": longsword_guard(),
        "longsword_riposte": longsword_riposte(),
        "bow_shot": bow_shot(),
        "bow_rain": bow_rain(),
        "crossbow_shot": crossbow_shot(),
        "crossbow_burst": crossbow_burst(),
        "knife_throw": knife_throw(),
        "knife_fan": knife_fan(),
        "knife_pin": knife_pin(),
    }
    return {"format_version": "1.8.0", "animations": {name: to_json(a) for name, a in anims.items()}}
