"""Draws an Emotecraft emote (JSON, version 3, degrees) on a blocky player, to look at before trying it in the game:
python3 emote_preview.py <emote.json> <out.png> [ticks,...] [--gif out.gif]

What it follows (as Emotecraft's player animator does): each part's pitch, yaw, roll as a model part's (Z, then Y,
then X), the arms' and legs' bend at their middle (an elbow bends forward with a negative bend, a knee backward with a
positive one; the bend's axis turns the way it bends), the torso's bend at the waist carrying the head and arms, the
whole body's x, y, z (blocks) and its turn about a point 0.7 above the feet."""
import json
import math
import sys

import numpy as np
from PIL import Image, ImageDraw


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
PARTS = ("head", "torso", "rightArm", "leftArm", "rightLeg", "leftLeg", "body")
AXES = ("x", "y", "z", "pitch", "yaw", "roll", "bend", "axis")


def track(moves, part, axis):
    pts = [(m["tick"], m[part][axis]) for m in moves if part in m and axis in m[part]]
    return pts


def value(pts, t):
    if not pts:
        return 0.0
    if t <= pts[0][0]:
        return pts[0][1]
    for (t0, v0), (t1, v1) in zip(pts, pts[1:]):
        if t0 <= t <= t1:
            return v0 + (v1 - v0) * (t - t0) / max(1e-9, t1 - t0)
    return pts[-1][1]


def pose_at(tracks, t):
    return {p: {a: value(tracks[p][a], t) for a in AXES} for p in PARTS}


def box(lo, hi):
    x0, y0, z0 = lo
    x1, y1, z1 = hi
    v = np.array([[x0, y0, z0], [x1, y0, z0], [x1, y1, z0], [x0, y1, z0], [x0, y0, z1], [x1, y0, z1], [x1, y1, z1], [x0, y1, z1]], float)
    faces = [(0, 1, 2, 3), (5, 4, 7, 6), (4, 0, 3, 7), (1, 5, 6, 2), (4, 5, 1, 0), (3, 2, 6, 7)]
    return v, faces


COL = {"head": (226, 196, 170), "torso": (210, 40, 40), "rightArm": (238, 238, 232), "leftArm": (238, 238, 232),
       "rightLeg": (60, 70, 160), "leftLeg": (60, 70, 160), "hand": (240, 240, 240), "face": (30, 30, 30), "hair": (230, 110, 30)}


def build(P):
    """The player's boxes in model space (pixels, y down, its front toward -z), each (vertices, faces, colour)."""
    out = []

    def rot(p):
        return rz(R(p["roll"])) @ ry(R(p["yaw"])) @ rx(R(p["pitch"]))

    def bend_m(p, sign):
        ax = R(p["axis"])
        return ry(ax) @ rx(sign * 0 + R(p["bend"])) @ ry(-ax)

    # The torso: its rotation about the neck, its bend at the waist (y 6) carrying all above it
    T = P["torso"]
    t_rot = rot(T)
    # positive torso bend: the top leans forward
    tb = rx(R(T["bend"]))

    def upper(v):
        # a point above the waist: bent about the waist, then the torso's turn about the neck
        w = np.array([0, 6, 0.])
        return t_rot @ (tb @ (v - w) + w)

    def lower_torso(v):
        return t_rot @ v

    v, f = box((-4, 0, -2), (4, 6, 2))
    out.append((np.array([upper(p) for p in v]), f, COL["torso"]))
    v, f = box((-4, 6, -2), (4, 12, 2))
    out.append((np.array([lower_torso(p) for p in v]), f, COL["torso"]))
    # The head on the neck
    Hm = rot(P["head"])
    v, f = box((-4, -8, -4), (4, 0, 4))
    hv = np.array([upper(Hm @ p) for p in v])
    out.append((hv, f, COL["head"]))
    # a face (eyes, a wide red grin) on its front, a tuft of hair each side
    for lo, hi, c in [((-3, -5.5, -4.1), (-1.2, -4.0, -4.0), COL["face"]), ((1.2, -5.5, -4.1), (3, -4.0, -4.0), COL["face"]),
                      ((-3.2, -2.8, -4.1), (3.2, -1.6, -4.0), (200, 20, 30)), ((-5.5, -7, -2), (-4, -4, 2), COL["hair"]),
                      ((4, -7, -2), (5.5, -4, 2), COL["hair"]), ((-1.2, -4.4, -5.6), (1.2, -2.6, -4), (225, 20, 30))]:
        v, f = box(lo, hi)
        out.append((np.array([upper(Hm @ p) for p in v]), f, c))

    def limb(part, pivot, lo, hi, mid, sign, colour, carried):
        p = P[part]
        Rm = rot(p)
        Bm = bend_m(p, sign)
        midp = np.array([0, mid, 0.])
        for a, b, lower in ((lo[1], mid, False), (mid, hi[1], True)):
            v, f = box((lo[0], a, lo[2]), (hi[0], b, hi[2]))
            pts = []
            for q in v:
                if lower:
                    q = Bm @ (q - midp) + midp
                q = Rm @ q + np.array(pivot, float)
                pts.append(upper(q) if carried else lower_torso(q))
            out.append((np.array(pts), f, tuple(int(ch * .78) for ch in colour) if lower else colour))
        # the hand (a white glove) at the end of an arm
        if part.endswith("Arm"):
            v, f = box((lo[0] - .3, hi[1] - 2.5, lo[2] - .3), (hi[0] + .3, hi[1], hi[2] + .3))
            pts = [Bm @ (q - midp) + midp for q in v]
            pts = [upper(Rm @ q + np.array(pivot, float)) for q in pts]
            out.append((np.array(pts), f, COL["hand"]))

    limb("rightArm", (-5, 2, 0), (-3, -2, -2), (1, 10, 2), 4, -1, (232, 232, 226), True)
    limb("leftArm", (5, 2, 0), (-1, -2, -2), (3, 10, 2), 4, -1, (232, 232, 226), True)
    limb("rightLeg", (-1.9, 12, 0.1), (-2, 0, -2), (2, 12, 2), 6, 1, COL["rightLeg"], False)
    limb("leftLeg", (1.9, 12, 0.1), (-2, 0, -2), (2, 12, 2), 6, 1, COL["leftLeg"], False)
    return out


def to_world(P, pts):
    """Model pixels to the world (blocks, y up, the player facing -z), with the whole body's offset and turn."""
    b = P["body"]
    w = np.stack([-pts[:, 0] / 16, 1.501 - pts[:, 1] / 16, pts[:, 2] / 16], 1) * .9375
    M = rz(R(b["roll"])) @ ry(R(b["yaw"])) @ rx(R(b["pitch"]))
    piv = np.array([0, .7, 0])
    w = (M @ (w - piv).T).T + piv + np.array([b["x"], b["y"], b["z"]])
    return w


def render(P, view_yaw, size=(260, 300), scale=95, ground=0.0, clear=False, pitch=-12):
    img = Image.new("RGBA" if clear else "RGB", size, (0, 0, 0, 0) if clear else (236, 240, 244))
    d = ImageDraw.Draw(img)
    cam = ry(R(view_yaw)) @ rx(R(pitch))
    light = np.array([.4, .8, -.45])
    light /= np.linalg.norm(light)
    polys = []
    cx, cy = size[0] / 2, size[1] * .86
    # the ground line
    for gx in ([] if clear else np.linspace(-1.2, 1.2, 13)):
        a = cam.T @ np.array([gx, ground, -1.2])
        bb = cam.T @ np.array([gx, ground, 1.2])
        d.line((cx - a[0] * scale, cy - a[1] * scale, cx - bb[0] * scale, cy - bb[1] * scale), fill=(214, 218, 224))
    for v, faces, colour in build(P):
        w = to_world(P, v)
        c = (cam.T @ w.T).T
        for f in faces:
            q = c[list(f)]
            n = np.cross(q[1] - q[0], q[2] - q[0])
            if np.linalg.norm(n) < 1e-9:
                continue
            wn = np.cross(w[f[1]] - w[f[0]], w[f[2]] - w[f[0]])
            wn /= max(1e-9, np.linalg.norm(wn))
            k = .55 + .45 * abs(float(wn @ light))
            polys.append((q[:, 2].mean(), [(cx - p[0] * scale, cy - p[1] * scale) for p in q], tuple(int(min(255, ch * k)) for ch in colour)))
    polys.sort(key=lambda p: -p[0])
    for _, pts, col in polys:
        d.polygon(pts, fill=col + ((255,) if clear else ()), outline=tuple(max(0, ch - 40) for ch in col) + ((255,) if clear else ()))
    return img


def load(path):
    data = json.load(open(path, encoding="utf-8"))
    moves = data["emote"]["moves"]
    tracks = {p: {a: track(moves, p, a) for a in AXES} for p in PARTS}
    return data, tracks


def sheet(path, out, ticks, views=(-35, 90)):
    data, tracks = load(path)
    W, H = 220, 260
    img = Image.new("RGB", (W * len(ticks), H * len(views) + 18), (255, 255, 255))
    d = ImageDraw.Draw(img)
    d.text((4, 2), data["name"], fill=(0, 0, 0))
    for i, t in enumerate(ticks):
        P = pose_at(tracks, t)
        for j, vy in enumerate(views):
            img.paste(render(P, vy, (W, H), 88), (W * i, 18 + H * j))
        d.text((W * i + 4, 20), f"t={t}", fill=(0, 0, 0))
    img.save(out)


def gif(path, out, view=-35):
    data, tracks = load(path)
    end = data["emote"]["endTick"]
    frames = [render(pose_at(tracks, t), view, (260, 300), 95) for t in range(0, end + 1)]
    frames[0].save(out, save_all=True, append_images=frames[1:], duration=50, loop=0)


def icon(path, out, tick, view=-28, side=256):
    """The emote's icon: its most telling pose, drawn big and smooth, on a dark round of circus red and gold."""
    from PIL import ImageFilter
    data, tracks = load(path)
    big = 4
    fig = render(pose_at(tracks, tick), view, (side * big * 2, side * big * 2), side * big * .8, clear=True, pitch=-10)
    box = fig.getbbox()
    fig = fig.crop(box)
    room = side * .78
    k = min(room / fig.width, room / fig.height)
    fig = fig.resize((max(1, int(fig.width * k)), max(1, int(fig.height * k))), Image.LANCZOS)
    S = side * big
    bg = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(bg)
    m = S * .03
    # a round: gold rim, deep red, darker to the edge, a few faint stripes like a circus tent
    d.ellipse((m, m, S - m, S - m), fill=(214, 168, 70, 255))
    inner = S * .07
    d.ellipse((inner, inner, S - inner, S - inner), fill=(70, 10, 16, 255))
    import math as _m
    cx = cy = S / 2
    r = S / 2 - inner
    for i in range(12):
        a0 = i / 12 * 2 * _m.pi
        a1 = (i + .5) / 12 * 2 * _m.pi
        d.polygon([(cx, cy), (cx + r * _m.cos(a0), cy + r * _m.sin(a0)), (cx + r * _m.cos(a1), cy + r * _m.sin(a1))], fill=(92, 16, 24, 255))
    glow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse((S * .25, S * .2, S * .75, S * .7), fill=(255, 200, 120, 70))
    glow = glow.filter(ImageFilter.GaussianBlur(S * .08))
    bg = Image.alpha_composite(bg, glow)
    mask = Image.new("L", (S, S), 0)
    ImageDraw.Draw(mask).ellipse((inner, inner, S - inner, S - inner), fill=255)
    vign = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(vign).ellipse((inner, inner, S - inner, S - inner), outline=(0, 0, 0, 150), width=int(S * .06))
    vign = vign.filter(ImageFilter.GaussianBlur(S * .03))
    bg = Image.alpha_composite(bg, Image.composite(vign, Image.new("RGBA", (S, S), (0, 0, 0, 0)), mask))
    bg = bg.resize((side, side), Image.LANCZOS)
    # the figure's shadow, then the figure
    shadow = Image.new("RGBA", fig.size, (0, 0, 0, 0))
    shadow.putalpha(fig.split()[3].point(lambda a: a * 0.5))
    shadow = shadow.filter(ImageFilter.GaussianBlur(3))
    x, y = int((side - fig.width) / 2), int((side - fig.height) / 2 + side * .02)
    bg.alpha_composite(shadow, (x + 3, y + 4))
    bg.alpha_composite(fig, (x, y))
    bg.save(out)


if __name__ == "__main__":
    args = sys.argv[1:]
    if "--gif" in args:
        k = args.index("--gif")
        gif(args[0], args[k + 1])
        args = args[:k]
    if len(args) >= 3:
        sheet(args[0], args[1], [int(x) for x in args[2].split(",")])
