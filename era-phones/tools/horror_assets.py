"""The game masters' horror pictures (original art, drawn here): the face that rushes at the screen (a pale, grinning
clown of no one's in particular: hollow eyes, black tears, a red nose, a mouth of needle teeth) and the red balloon that
floats in the world (64 x 128, its string included)."""
import math

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

import kit


def noise(h, w, seed, octaves=5):
    """Smooth value noise in [0, 1]."""
    rng = np.random.default_rng(seed)
    out = np.zeros((h, w), np.float32)
    amp, total = 1.0, 0.0
    for o in range(octaves):
        n = 4 * 2 ** o
        grid = rng.random((n + 1, n + 1)).astype(np.float32)
        img = Image.fromarray((grid * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC)
        out += np.asarray(img, np.float32) / 255 * amp
        total += amp
        amp *= .5
    return out / total


def face():
    s = 1024
    ys, xs = np.mgrid[0:s, 0:s].astype(np.float32) / s
    # The dark, a dull red glow behind the head
    glow = np.exp(-((xs - .5) ** 2 + (ys - .45) ** 2) / .08)
    rgb = np.dstack([.02 + .28 * glow, .004 + .015 * glow, .008 + .015 * glow])
    # The head: long, narrowing to the chin, pale, darker towards its edges, under the brows and in the cheeks
    u, v = (xs - .5) / (.33 - .06 * np.clip(ys - .5, 0, 1) * 2), (ys - .47) / .41
    r = np.hypot(u, v)
    head = np.clip((1 - r) * 60, 0, 1)
    grime = noise(s, s, 91)
    dirt = noise(s, s, 17, 6)
    shade = np.clip(1 - .6 * r ** 2.4 - .3 * np.clip(v, 0, 1) ** 2, 0, 1)
    for cx in (.37, .63):
        shade -= .32 * np.exp(-((xs - cx) ** 2 / .012 + (ys - .37) ** 2 / .006))
        shade -= .18 * np.exp(-((xs - cx + (cx - .5) * .25) ** 2 / .004 + (ys - .6) ** 2 / .006))
    skin = np.dstack([.84 * shade, .83 * shade, .78 * shade]) * (.86 + .22 * grime[..., None])
    skin = skin - np.clip(dirt - .55, 0, 1)[..., None] * np.array([.5, .55, .6])
    rgb = rgb * (1 - head[..., None]) + np.clip(skin, 0, 1) * head[..., None]
    img = kit.to_img(np.dstack([np.clip(rgb, 0, 1), np.ones((s, s))]))
    d = ImageDraw.Draw(img, "RGBA")

    def el(cx, cy, rx, ry, fill):
        d.ellipse(((cx - rx) * s, (cy - ry) * s, (cx + rx) * s, (cy + ry) * s), fill=fill)

    # Cracks in the paint
    rng = np.random.default_rng(23)
    for _ in range(9):
        x, y = rng.uniform(.25, .75), rng.uniform(.12, .5)
        pts = [(x * s, y * s)]
        for _ in range(7):
            x += rng.uniform(-.025, .025)
            y += rng.uniform(.005, .03)
            pts.append((x * s, y * s))
        d.line(pts, fill=(60, 52, 50, 150), width=2)
    # Black tears running down from the eyes
    rng = np.random.default_rng(5)
    for cx in (.37, .63):
        for k in range(3):
            x0 = cx + (k - 1) * .028 + rng.uniform(-.006, .006)
            length = .09 + rng.uniform(0, .14)
            pts = [((x0 + math.sin(t * 9 + k) * .004) * s, (.45 + t) * s) for t in np.linspace(0, length, 30)]
            d.line(pts, fill=(10, 6, 8, 215), width=int(s * (.011 - k * .002)))
            el(pts[-1][0] / s, pts[-1][1] / s, .008, .011, (10, 6, 8, 225))
    # The eye sockets: large, deep, black, slanting down to the nose, ragged at the edge
    for cx, tilt in ((.37, 1), (.63, -1)):
        sock = Image.new("L", (s, s), 0)
        sd = ImageDraw.Draw(sock)
        pts = []
        for i in range(40):
            a = i / 40 * math.tau
            rr = 1 + .12 * math.sin(a * 5 + cx * 20) + .06 * math.sin(a * 11)
            px, py = cx + math.cos(a) * .1 * rr, .395 + math.sin(a) * .085 * rr
            py += tilt * (px - cx) * .35
            pts.append((px * s, py * s))
        sd.polygon(pts, fill=255)
        sock = sock.filter(ImageFilter.GaussianBlur(s * .014))
        black = Image.new("RGBA", (s, s), (4, 2, 4, 255))
        img = Image.composite(black, img, sock)
        d = ImageDraw.Draw(img, "RGBA")
        # A pinprick of a pupil, far inside
        px = cx - tilt * .018
        for rad, col in ((.022, (200, 20, 0, 45)), (.009, (255, 90, 30, 120)), (.0045, (255, 240, 200, 255))):
            el(px, .41, rad, rad, col)
    # A red nose, worn
    el(.5, .545, .042, .04, (110, 6, 10, 255))
    el(.5, .545, .034, .032, (170, 14, 20, 255))
    el(.488, .532, .01, .008, (255, 160, 160, 160))
    # The grin: red lips stretched far too wide, black inside, uneven yellowed needle teeth with gaps
    mouth = [(.19, .615), (.3, .665), (.4, .695), (.5, .705), (.6, .695), (.7, .665), (.81, .615), (.76, .72), (.66, .8), (.5, .845), (.34, .8), (.24, .72)]
    d.polygon([(x * s, y * s) for x, y in mouth], fill=(105, 4, 10, 255))
    inner = [(.23, .64), (.32, .685), (.41, .71), (.5, .72), (.59, .71), (.68, .685), (.77, .64), (.72, .71), (.63, .775), (.5, .81), (.37, .775), (.28, .71)]
    d.polygon([(x * s, y * s) for x, y in inner], fill=(6, 1, 3, 255))

    def top(x):
        return .64 + .08 * max(0, 1 - ((x - .5) / .27) ** 2)

    def bot(x):
        return .71 + .1 * max(0, 1 - ((x - .5) / .27) ** 2)

    rng = np.random.default_rng(41)
    n = 13
    for i in range(n):
        xm = .27 + (i + .5) / n * .46 + rng.uniform(-.006, .006)
        half = .011 + rng.uniform(0, .005)
        length = .03 + rng.uniform(0, .035)
        col = (int(rng.uniform(200, 230)), int(rng.uniform(185, 210)), int(rng.uniform(130, 160)), 255)
        d.polygon([((xm - half) * s, top(xm - half) * s), ((xm + half) * s, top(xm + half) * s), (xm * s, (top(xm) + length) * s)], fill=col)
        if i % 4 != 2:
            xb = xm + rng.uniform(-.01, .01)
            lb = .02 + rng.uniform(0, .03)
            d.polygon([((xb - half) * s, bot(xb - half) * s), ((xb + half) * s, bot(xb + half) * s), (xb * s, (bot(xb) - lb) * s)], fill=col)
    # The lipstick smeared up into the cheeks
    for side in (-1, 1):
        pts = [((.5 + side * (.255 + t * .045)) * s, (.62 - t * .085 - t * t * .04) * s) for t in np.linspace(0, 1, 20)]
        d.line(pts, fill=(130, 8, 16, 225), width=int(s * .018))
    img = img.filter(ImageFilter.GaussianBlur(s * .0015))
    # Grain and a dark edge all round, fading to the black of the screen
    a = np.asarray(img, np.float32) / 255
    grain = np.random.default_rng(3).normal(0, .03, (s, s, 1))
    edge = np.clip(1 - (np.hypot(xs - .5, ys - .5) - .33) / .18, 0, 1)[..., None]
    a[..., :3] = np.clip((a[..., :3] + grain) * edge, 0, 1)
    out = Image.fromarray((a * 255).astype(np.uint8), "RGBA")
    return out.resize((256, 256), Image.LANCZOS)


def balloon():
    """A red balloon on its string (64 x 128), glossy, its knot under it, the string swaying down."""
    w, h = 64 * 4, 128 * 4
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
    u, v = (xs - w * .5) / (w * .43), (ys - h * .3) / (h * .27)
    v = np.where(v > 0, v * (1 - .2 * v), v)
    r = np.hypot(u, v)
    inside = np.clip((1 - r) * 40, 0, 1)
    shade = np.clip(1 - np.hypot(u + .35, v + .42) * .62, 0, 1)
    rim = np.clip((r - .8) * 4, 0, 1)
    rgb = np.dstack([.42 + .5 * shade + .1 * rim, .02 + .16 * shade ** 3, .03 + .16 * shade ** 3])
    spec = np.exp(-((u + .36) ** 2 * 1.6 + (v + .46) ** 2) / .018)[..., None]
    spec2 = np.exp(-((u - .42) ** 2 + (v - .2) ** 2) / .01)[..., None] * .25
    rgb = rgb + spec * .75 + spec2
    layer = kit.to_img(np.dstack([np.clip(rgb, 0, 1), inside]))
    img = Image.alpha_composite(img, layer)
    d = ImageDraw.Draw(img)
    kx, ky = w * .5, h * .573
    d.polygon([(kx - 12, ky + 12), (kx + 12, ky + 12), (kx + 4, ky - 6), (kx - 4, ky - 6)], fill=(130, 6, 12, 255))
    d.ellipse((kx - 6, ky + 8, kx + 6, ky + 16), fill=(110, 4, 10, 255))
    pts = [(kx + math.sin(t * .045) * 14 * (t / (h - ky)), ky + 14 + t) for t in range(0, int(h - ky - 16), 4)]
    d.line(pts, fill=(225, 225, 225, 235), width=4)
    return img.resize((64, 128), Image.LANCZOS)


def main():
    kit.save(face(), "textures/gui/horror/face.png")
    kit.save(balloon(), "textures/horror/balloon.png")


if __name__ == "__main__":
    main()
