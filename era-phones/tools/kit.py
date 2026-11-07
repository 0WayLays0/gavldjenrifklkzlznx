"""Shared drawing helpers for the Era Phones art (original art, drawn here)."""
import json
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "eraphones")
S = 3


def save(img, rel):
    path = os.path.join(ROOT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path, optimize=True)
    print("wrote", os.path.relpath(path))


def write_json(rel, data):
    path = os.path.join(ROOT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
        f.write("\n")
    print("wrote", os.path.relpath(path))


def from_grid(rows, legend, size=16):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == size, (y, row, len(row))
        for x, ch in enumerate(row):
            if ch != ".":
                c = legend[ch]
                img.putpixel((x, y), c + (255,) if len(c) == 3 else c)
    return img


def to_np(img):
    return np.asarray(img, dtype=np.float32) / 255.0


def to_img(a):
    return Image.fromarray(np.clip(a * 255, 0, 255).astype(np.uint8), "RGBA")


def mask_rounded(w, h, box, r, scale=S):
    m = Image.new("L", (w, h), 0)
    ImageDraw.Draw(m).rounded_rectangle(tuple(v * scale for v in box), radius=r * scale, fill=255)
    return m


def tnoise(w, h, cells, seed):
    """Smooth value noise in [0, 1], tiling both across and down."""
    rnd = np.random.default_rng(seed)
    grid = rnd.random((cells[1], cells[0]))
    ys, xs = np.mgrid[0:h, 0:w]
    gx, gy = xs / w * cells[0], ys / h * cells[1]
    x0, y0 = gx.astype(int), gy.astype(int)
    fx, fy = gx - x0, gy - y0
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    x1, y1 = (x0 + 1) % cells[0], (y0 + 1) % cells[1]
    x0, y0 = x0 % cells[0], y0 % cells[1]
    a, b, c, d = grid[y0, x0], grid[y0, x1], grid[y1, x0], grid[y1, x1]
    return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy


def gradient(w, h, stops):
    ys = np.linspace(0, 1, h)[:, None, None] * np.ones((1, w, 1))
    out = np.zeros((h, w, 3))
    for (p0, c0), (p1, c1) in zip(stops, stops[1:]):
        t = np.clip((ys - p0) / max(1e-6, p1 - p0), 0, 1)
        inside = ((ys >= p0) & (ys <= p1)).astype(float)
        col = np.array(c0) * (1 - t) + np.array(c1) * t
        out = out * (1 - inside) + col / 255 * inside
    return out


def stars(img, n, seed, size=1.0, color=(255, 255, 255)):
    rng = np.random.default_rng(seed)
    d = ImageDraw.Draw(img)
    w, h = img.size
    for _ in range(n):
        x, y = rng.random() * w, rng.random() * h
        r = (rng.random() ** 3) * 2.2 * size + .5
        a = int(120 + rng.random() * 135)
        d.ellipse((x - r, y - r, x + r, y + r), fill=color + (a,))
    return img


def squircle(n, top, bottom):
    ss = n * 4
    ys, xs = np.mgrid[0:ss, 0:ss].astype(np.float32)
    u, v = (xs + .5) / ss * 2 - 1, (ys + .5) / ss * 2 - 1
    sq = (np.abs(u) ** 4.6 + np.abs(v) ** 4.6) ** (1 / 4.6)
    alpha = np.clip((1 - sq) * ss * .5, 0, 1)
    t = ((v + 1) / 2)[..., None]
    rgb = np.array(top) / 255 * (1 - t) + np.array(bottom) / 255 * t
    sheen = np.clip(1 - (v + .9) * 1.4, 0, 1) * .22 * np.clip(1 - sq, 0, 1) * 3
    rgb = rgb + sheen[..., None]
    rim = np.clip(1 - np.abs(sq - .95) * 40, 0, 1) * .25
    rgb = rgb + rim[..., None]
    return to_img(np.dstack([np.clip(rgb, 0, 1), alpha])), ss


def icon(top, bottom, draw, n=96):
    """A glossy rounded-square app icon, {draw} putting its glyph on it (in units of the icon's side)."""
    img, ss = squircle(n, top, bottom)
    glyph = Image.new("RGBA", (ss, ss), (0, 0, 0, 0))
    d = ImageDraw.Draw(glyph)
    draw(d, ss, glyph)
    shadow = glyph.filter(ImageFilter.GaussianBlur(ss * .02))
    sh = np.asarray(shadow, np.float32).copy()
    sh[..., :3] = 0
    sh[..., 3] *= .35
    img = Image.alpha_composite(img, Image.fromarray(sh.astype(np.uint8), "RGBA").transform(img.size, Image.AFFINE, (1, 0, 0, 0, 1, -ss * .012)))
    img = Image.alpha_composite(img, glyph)
    return img.resize((n, n), Image.LANCZOS)


W_ = (255, 255, 255, 255)


def poly(d, s, pts, fill):
    d.polygon([(x * s, y * s) for x, y in pts], fill=fill)


def ell(d, s, x, y, rx, ry, fill=None, outline=None, width=1):
    d.ellipse(((x - rx) * s, (y - ry) * s, (x + rx) * s, (y + ry) * s), fill=fill, outline=outline, width=int(width))


def rounded(n, r):
    ss = n * 4
    img = Image.new("RGBA", (ss, ss), (0, 0, 0, 0))
    ImageDraw.Draw(img).rounded_rectangle((0, 0, ss - 1, ss - 1), radius=r * 4, fill=(255, 255, 255, 255))
    return img.resize((n, n), Image.LANCZOS)


def glow():
    n = 64
    ys, xs = np.mgrid[0:n, 0:n].astype(np.float32)
    r = np.hypot(xs - n / 2 + .5, ys - n / 2 + .5) / (n / 2)
    a = np.clip(1 - r, 0, 1) ** 2
    return to_img(np.dstack([np.ones((n, n)), np.ones((n, n)), np.ones((n, n)), a]))


def shell(w, h, box, r, c_dark, c_mid, c_light, seed, brushed=True, scale=S):
    """A plastic or metal shell inside {box} (units): a shaded body, its rim catching the light; returns RGBA."""
    W, H = int(w * scale), int(h * scale)
    ys, xs = np.mgrid[0:H, 0:W].astype(np.float32)
    t = xs / W * .55 + ys / H * .45
    k = (np.sin(t * math.tau * .9) * .5 + .5)[..., None]
    rgb = np.array(c_dark) * (1 - k) + np.array(c_mid) * k
    if brushed:
        rgb = rgb * (.9 + tnoise(W, H, (2, 120), seed)[..., None] * .1)
    else:
        rgb = rgb * (.95 + tnoise(W, H, (40, 80), seed)[..., None] * .05)
    body = mask_rounded(W, H, box, r, scale)
    inner = mask_rounded(W, H, (box[0] + 1.6, box[1] + 1.6, box[2] - 1.6, box[3] - 1.6), max(1, r - 1.6), scale)
    edge = np.asarray(body, np.float32) / 255 - np.asarray(inner, np.float32) / 255
    blur = np.asarray(Image.fromarray((np.clip(edge, 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(2)), np.float32) / 255
    rgb = rgb + blur[..., None] * (np.array(c_light) - rgb) * .5
    # Light from the top left, shade to the bottom right
    light = np.clip(1 - (xs / W + ys / H) * .6, 0, 1)[..., None]
    rgb = rgb * (.82 + .3 * light)
    alpha = np.asarray(body, np.float32) / 255
    return to_img(np.dstack([np.clip(rgb, 0, 1), alpha]))
