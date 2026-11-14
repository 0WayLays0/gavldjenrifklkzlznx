"""The game masters' horror pictures (original art, drawn here, first for Époque RP): the red balloon that floats in the world (64 x 12
string included) and the blood at the edges of the sight (a veil, a spatter, runs)."""
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
    kit.save(balloon(), "textures/horror/balloon.png")
    blood()


# ------------------------------------------------------------------------------------------------ blood on the screen

def _shade_blood(thick, light=(-.6, -.8)):
    """Blood of a given thickness field: deep red where thick, thinner and brighter at the edges, a wet shine where
    the surface faces the light; its alpha follows the thickness softly."""
    gy, gx = np.gradient(thick)
    nx, ny = -gx * 6, -gy * 6
    nz = np.ones_like(thick)
    norm = np.sqrt(nx ** 2 + ny ** 2 + nz ** 2)
    lx, ly = light
    lz = .6
    ln = math.sqrt(lx * lx + ly * ly + lz * lz)
    diffuse = np.clip((nx * lx + ny * ly + nz * lz) / (norm * ln), 0, 1)
    # Half-vector shine
    hx, hy, hz = lx / ln, ly / ln, lz / ln + 1
    hn = math.sqrt(hx * hx + hy * hy + hz * hz)
    spec = np.clip((nx * hx + ny * hy + nz * hz) / (norm * hn), 0, 1) ** 40
    t = np.clip(thick, 0, 1)
    base = np.dstack([.30 + .32 * (1 - t), .006 + .02 * (1 - t), .012 + .02 * (1 - t)])
    rgb = base * (.55 + .6 * diffuse[..., None]) + spec[..., None] * np.array([1, .75, .75]) * .9 * (t[..., None] > .2)
    alpha = np.clip(t * 3.2, 0, 1) * .97
    return np.dstack([np.clip(rgb, 0, 1), alpha])


def blood_splat(seed, n=256):
    rng = np.random.default_rng(seed)
    ss = n * 2
    ys, xs = np.mgrid[0:ss, 0:ss].astype(np.float32) / ss
    field = np.zeros((ss, ss), np.float32)
    # The main pool: blobs round the middle
    for _ in range(7):
        cx, cy = .5 + rng.normal(0, .06), .5 + rng.normal(0, .06)
        r = rng.uniform(.04, .1)
        field += .8 * np.exp(-((xs - cx) ** 2 + (ys - cy) ** 2) / (r * r))
    # Spikes flung outwards, droplets at their ends
    for _ in range(int(rng.integers(14, 22))):
        a = rng.uniform(0, math.tau)
        length = rng.uniform(.18, .44)
        width = rng.uniform(.008, .022)
        for k in range(30):
            t = k / 29
            cx, cy = .5 + math.cos(a) * length * t, .5 + math.sin(a) * length * t
            w = width * (1 - .6 * t)
            field += .7 * np.exp(-((xs - cx) ** 2 + (ys - cy) ** 2) / (w * w))
        cx, cy = .5 + math.cos(a) * (length + .03), .5 + math.sin(a) * (length + .03)
        r = rng.uniform(.01, .028)
        field += np.exp(-((xs - cx) ** 2 + (ys - cy) ** 2) / (r * r))
    for _ in range(40):
        a, d = rng.uniform(0, math.tau), rng.uniform(.2, .47)
        r = rng.uniform(.004, .012)
        cx, cy = .5 + math.cos(a) * d, .5 + math.sin(a) * d
        field += np.exp(-((xs - cx) ** 2 + (ys - cy) ** 2) / (r * r))
    thick = np.clip((field - .45) * 1.6, 0, 1)
    # Uneven: thicker pools and thinner smears inside, a fine mist of spray round it
    thick *= .55 + .45 * noise(ss, ss, seed + 1, 5)
    mist = (rng.random((ss, ss)) > .9965) * np.clip(1 - np.hypot(xs - .5, ys - .5) / .5, 0, 1) * .8
    thick = np.maximum(thick, mist.astype(np.float32))
    thick = np.asarray(Image.fromarray((thick * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(2)), np.float32) / 255
    img = kit.to_img(_shade_blood(thick))
    return img.resize((n, n), Image.LANCZOS)


def blood_drip(w=32, h=512):
    ss = 2
    W, H = w * ss, h * ss
    ys, xs = np.mgrid[0:H, 0:W].astype(np.float32)
    t = ys / H
    # Thin at the top, a little wider down the run, a drop swelling at the end
    width = (W * .16) * (.7 + .5 * t) + np.exp(-((t - .955) / .03) ** 2) * W * .22
    centre = W * .5 + np.sin(t * 9) * W * .03
    thick = np.clip(1 - ((xs - centre) / np.maximum(width, 1)) ** 2, 0, 1) ** .7
    thick *= np.clip((.985 - t) * 60, 0, 1)
    thick *= np.clip(t * 8, 0, 1) * .6 + .4
    img = kit.to_img(_shade_blood(thick, light=(-.7, -.3)))
    return img.resize((w, h), Image.LANCZOS)


def blood_edge(n=512):
    rng = np.random.default_rng(77)
    ss = n
    ys, xs = np.mgrid[0:ss, 0:ss].astype(np.float32) / ss
    edge = np.minimum(np.minimum(xs, 1 - xs), np.minimum(ys, 1 - ys))
    wobble = noise(ss, ss, 12, 5)
    wob2 = noise(ss, ss, 31, 6)
    smear = np.clip((.035 + .06 * wobble - edge) * 14, 0, 1) * (.3 + .7 * wob2)
    # Heavier along the top, runs coming down from it
    top = np.clip((.03 + .05 * wobble - ys) * 16, 0, 1) * (.5 + .5 * wob2)
    runs = np.zeros_like(xs)
    for _ in range(7):
        x0 = rng.uniform(.03, .97)
        length = rng.uniform(.06, .2)
        w = rng.uniform(.004, .012)
        runs += np.exp(-((xs - x0 - np.sin(ys * 20 + x0 * 9) * .004) / w) ** 2) * (ys < length) * np.clip((length - ys) * 20, 0, 1)
    thick = np.clip(np.maximum(smear * (.55 + .45 * wobble), np.maximum(top, np.clip(runs, 0, 1))), 0, 1)
    img = kit.to_img(_shade_blood(thick))
    return img


def blood():
    for k in range(4):
        kit.save(blood_splat(400 + k), f"textures/gui/horror/blood_splat_{k}.png")
    kit.save(blood_drip(), "textures/gui/horror/blood_drip.png")
    kit.save(blood_edge(), "textures/gui/horror/blood_edge.png")


if __name__ == "__main__":
    main()
