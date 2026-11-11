"""The player's arms worked out backwards: the angles (pitch, yaw, roll, bend) that put a hand on a point of the body or
in front of it (the face, the chest, the belly, a knee...), as emote_preview.py draws them and Emotecraft turns them.

Model space: pixels, y down from the neck, the front toward -z, the right side toward -x. A right hand's centre is 8.5
below the shoulder's pivot (-5, 2, 0) along the arm, the elbow 4 below it; the arm bends there (a negative bend folds
the forearm forward)."""
import math

import numpy as np


def rx(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


def ry(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


def rz(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


R = math.radians


def arm_points(side, pitch, yaw, roll, bend, torso_bend=0.0):
    """The elbow's and the hand's centres (model space) for these angles; side 'right' or 'left'."""
    sx = -1 if side == "right" else 1
    pivot = np.array([5.0 * sx, 2, 0])
    off = np.array([1.0 * sx, 0, 0])
    Rm = rz(R(roll)) @ ry(R(yaw)) @ rx(R(pitch))
    mid = np.array([0, 4.0, 0])
    elbow_l = off + mid
    hand_l = mid + rx(R(bend)) @ (off + np.array([0, 8.5, 0]) - mid)
    elbow = Rm @ elbow_l + pivot
    hand = Rm @ hand_l + pivot
    if torso_bend:
        w = np.array([0, 6, 0.])
        tb = rx(R(torso_bend))
        elbow = tb @ (elbow - w) + w
        hand = tb @ (hand - w) + w
    return elbow, hand


def inside_body(p, margin=0.5):
    """How far a point is inside the torso or the head (0 outside)."""
    d = 0.0
    for lo, hi in (((-4, 0, -2), (4, 12, 2)), ((-4, -8, -4), (4, 0, 4))):
        inside = min(p[0] - lo[0], hi[0] - p[0], p[1] - lo[1], hi[1] - p[1], p[2] - lo[2], hi[2] - p[2]) + margin
        d = max(d, inside)
    return d


def _fk_many(side, P, Y, Rr, B, torso_bend=0.0):
    """arm_points for arrays of angles (degrees): elbows and hands, shape (n, 3)."""
    sx = -1 if side == "right" else 1
    pivot = np.array([5.0 * sx, 2, 0])
    p, y, r, b = (np.radians(np.asarray(v, float)) for v in (P, Y, Rr, B))
    # hand in the arm's frame, the forearm bent about the elbow (x axis through (0, 4, 0))
    hy, hz = 4 + 4.5 * np.cos(b), 4.5 * np.sin(b)
    hand_l = np.stack([np.full_like(b, sx * 1.0), hy, hz], 1)
    elbow_l = np.stack([np.full_like(b, sx * 1.0), np.full_like(b, 4.0), np.zeros_like(b)], 1)

    def rot(v):
        # Rz(r) Ry(y) Rx(p) v
        x0, y0, z0 = v[:, 0], v[:, 1], v[:, 2]
        cp, sp = np.cos(p), np.sin(p)
        y1, z1 = y0 * cp - z0 * sp, y0 * sp + z0 * cp
        x1 = x0
        cy, sy = np.cos(y), np.sin(y)
        x2, z2 = x1 * cy + z1 * sy, -x1 * sy + z1 * cy
        y2 = y1
        cr, sr = np.cos(r), np.sin(r)
        x3, y3 = x2 * cr - y2 * sr, x2 * sr + y2 * cr
        return np.stack([x3, y3, z2], 1)

    elbow = rot(elbow_l) + pivot
    hand = rot(hand_l) + pivot
    if torso_bend:
        a = math.radians(torso_bend)
        for v in (elbow, hand):
            yy, zz = v[:, 1] - 6, v[:, 2]
            v[:, 1], v[:, 2] = yy * math.cos(a) - zz * math.sin(a) + 6, yy * math.sin(a) + zz * math.cos(a)
    return elbow, hand


def _inside_many(pts, margin=0.5):
    d = np.zeros(len(pts))
    for lo, hi in (((-4, 0, -2), (4, 12, 2)), ((-4, -8, -4), (4, 0, 4))):
        inside = np.minimum.reduce([pts[:, 0] - lo[0], hi[0] - pts[:, 0], pts[:, 1] - lo[1], hi[1] - pts[:, 1],
                                    pts[:, 2] - lo[2], hi[2] - pts[:, 2]]) + margin
        d = np.maximum(d, inside)
    return np.maximum(d, 0)


def reach(side, target, prefer=None, elbow_out=0.0, torso_bend=0.0):
    """The angles (pitch, yaw, roll, bend) that put the hand on `target` (model space), the elbow and the forearm kept
    out of the body, staying near `prefer` when several would do; elbow_out favours the elbow out to the side."""
    t = np.array(target, float)
    sx = -1 if side == "right" else 1
    pref = dict(pitch=-60.0, yaw=0.0, roll=-sx * 12.0, bend=-60.0)
    if prefer:
        pref.update(prefer)
    lo = np.array([-200, -90, -30 if sx < 0 else -120, -150.0])
    hi = np.array([70, 90, 120 if sx < 0 else 30, 0.0])

    def cost(A):
        elbow, hand = _fk_many(side, A[:, 0], A[:, 1], A[:, 2], A[:, 3], torso_bend)
        e = np.sum((hand - t) ** 2, 1)
        e += 40 * _inside_many(elbow) ** 2 + 20 * _inside_many((elbow + hand) / 2, 0) ** 2
        e += .0005 * ((A[:, 0] - pref["pitch"]) ** 2 + (A[:, 1] - pref["yaw"]) ** 2 + (A[:, 2] - pref["roll"]) ** 2)
        e -= elbow_out * elbow[:, 0] * sx * .2
        return e

    grids = np.meshgrid(np.arange(lo[0], hi[0] + 1, 10), np.arange(lo[1], hi[1] + 1, 15), np.arange(lo[2], hi[2] + 1, 10),
                        np.arange(lo[3], hi[3] + 1, 10), indexing="ij")
    A = np.stack([g.ravel() for g in grids], 1)
    c = cost(A)
    best = A[np.argmin(c)].copy()
    step = np.array([5.0, 7.5, 5.0, 5.0])
    rng = np.random.default_rng(1)
    for _ in range(60):
        cand = best + rng.normal(0, 1, (200, 4)) * step
        cand = np.clip(cand, lo, hi)
        cand = np.vstack([best, cand])
        cc = cost(cand)
        best = cand[np.argmin(cc)]
        step *= .93
    elbow, hand = _fk_many(side, [best[0]], [best[1]], [best[2]], [best[3]], torso_bend)
    out = {"pitch": round(float(best[0]), 2), "yaw": round(float(best[1]), 2), "roll": round(float(best[2]), 2), "bend": round(float(best[3]), 2)}
    return out, float(np.linalg.norm(hand[0] - t))


if __name__ == "__main__":
    for name, side, tgt in (("face R", "right", (-1.6, -4, -5.2)), ("face L", "left", (1.6, -4, -5.2)), ("belly R", "right", (-1.5, 8, -3)),
                            ("chest R", "right", (-.5, 3, -3)), ("mouth R", "right", (-.8, -2.5, -5))):
        print(name, reach(side, tgt))
