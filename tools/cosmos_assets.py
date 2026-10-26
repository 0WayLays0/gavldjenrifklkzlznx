#!/usr/bin/env python3
"""Cosmos art: the textures the Cosmos renderers paint on their meshes (the starfield, nebulae and galaxies of the
sky, the planets and the moon, a planet's rings, a black hole's accretion disk, the TVA clock, the swirl of a time
door, the scanlines of a temporal echo, storm clouds), the fifteen spell icons, the Codex Out of Time (icon and 3D
book texture) and its affinity ring.

Called by gen_assets.py (run that one). All art is original. Planet textures are equirectangular (u round the
equator, v from the north pole down), drawn from noise on the sphere so the poles do not pinch; the additive ones
(stars, nebula, galaxy, accretion, vortex, scanlines) are light on black, the others carry alpha.
"""
import math

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

import gen_assets as g

# Time (the TVA's orange) and space (deep blue and violet, white and gold stars)
ORANGE = (255, 138, 30)
AMBER = (255, 179, 71)
EMBER = (217, 72, 15)
CREAM = (255, 231, 194)
BRONZE = (92, 52, 22)
NIGHT = (8, 8, 26)
DEEP = (16, 18, 56)
VIOLET = (122, 76, 255)
BLUE = (63, 107, 255)
STAR = (244, 246, 255)
GOLD = (255, 215, 122)
ICE = (159, 232, 255)
OUTLINE = (10, 8, 24)
WHITE = (255, 255, 255)

D = "textures/entity/cosmos/"


# ---------------------------------------------------------------- noise

def _lattice(cells, seed):
    return np.random.default_rng(seed).random((cells, cells))


def tile_noise(w, h, cells_x, cells_y, seed):
    """Smooth value noise tiling on both axes: cells_x by cells_y lattice cells over the picture."""
    grid = np.random.default_rng(seed).random((cells_y, cells_x))
    xs = np.arange(w) * cells_x / w
    ys = np.arange(h) * cells_y / h
    x0 = np.floor(xs).astype(int)
    y0 = np.floor(ys).astype(int)
    fx = xs - x0
    fy = ys - y0
    sx = fx * fx * (3 - 2 * fx)
    sy = fy * fy * (3 - 2 * fy)
    x1 = (x0 + 1) % cells_x
    y1 = (y0 + 1) % cells_y
    x0 %= cells_x
    y0 %= cells_y
    a = grid[np.ix_(y0, x0)]
    b = grid[np.ix_(y0, x1)]
    c = grid[np.ix_(y1, x0)]
    d = grid[np.ix_(y1, x1)]
    top = a + (b - a) * sx[None, :]
    bottom = c + (d - c) * sx[None, :]
    return top + (bottom - top) * sy[:, None]


def tile_fbm(w, h, cells, octaves, seed, aspect=1.0):
    total = np.zeros((h, w))
    amp, norm = 1.0, 0.0
    for o in range(octaves):
        cx = max(1, int(round(cells * (2 ** o))))
        cy = max(1, int(round(cells * aspect * (2 ** o))))
        total += tile_noise(w, h, cx, cy, seed + o * 17) * amp
        norm += amp
        amp *= .5
    return total / norm


def _noise3(p, cells, seed):
    """Value noise in 3D at points p (..., 3), the lattice {cells} across the unit sphere."""
    P = 64
    grid = np.random.default_rng(seed).random((P, P, P))
    q = (p * cells) % P
    i0 = np.floor(q).astype(int)
    f = q - i0
    s = f * f * (3 - 2 * f)
    i1 = (i0 + 1) % P
    i0 %= P
    out = 0
    for dx in (0, 1):
        for dy in (0, 1):
            for dz in (0, 1):
                ix = i1[..., 0] if dx else i0[..., 0]
                iy = i1[..., 1] if dy else i0[..., 1]
                iz = i1[..., 2] if dz else i0[..., 2]
                wx = s[..., 0] if dx else 1 - s[..., 0]
                wy = s[..., 1] if dy else 1 - s[..., 1]
                wz = s[..., 2] if dz else 1 - s[..., 2]
                out = out + grid[ix, iy, iz] * wx * wy * wz
    return out


def fbm3(p, cells, octaves, seed):
    total, amp, norm = 0, 1.0, 0.0
    for o in range(octaves):
        total = total + _noise3(p, cells * (2 ** o), seed + o * 13) * amp
        norm += amp
        amp *= .5
    return total / norm


def sphere_points(w, h):
    """The point of the unit sphere under each pixel of a w x h equirectangular picture, and its latitude."""
    u = (np.arange(w) + .5) / w
    v = (np.arange(h) + .5) / h
    lon = u[None, :] * 2 * math.pi
    lat = (.5 - v[:, None]) * math.pi
    x = np.cos(lat) * np.cos(lon)
    y = np.sin(lat) * np.ones_like(lon)
    z = np.cos(lat) * np.sin(lon)
    return np.stack([x, y, z], axis=-1), lat * np.ones_like(lon)


def ramp(t, stops):
    """Colour along stops [(t, (r, g, b)), ...] for an array t."""
    t = np.clip(t, 0, 1)
    out = np.zeros(t.shape + (3,))
    for (t0, c0), (t1, c1) in zip(stops, stops[1:]):
        m = (t >= t0) & (t <= t1)
        k = ((t - t0) / max(1e-6, t1 - t0))[m]
        for i in range(3):
            out[..., i][m] = c0[i] + (c1[i] - c0[i]) * k
    return out


def to_image(rgb, alpha=None):
    rgb = np.clip(rgb, 0, 255).astype(np.uint8)
    if alpha is None:
        alpha = np.full(rgb.shape[:2], 255, np.uint8)
    else:
        alpha = np.clip(alpha, 0, 255).astype(np.uint8)
    return Image.fromarray(np.dstack([rgb, alpha]), "RGBA")


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


# ---------------------------------------------------------------- sky

def stars():
    """256x256 tileable starfield, light on black: faint star dust, hundreds of stars of every warmth, the brightest
    with four spikes."""
    S = 256
    img = np.zeros((S, S, 3))
    dust = tile_fbm(S, S, 6, 4, 101)
    img += (dust[..., None] ** 3) * np.array([18, 20, 46])
    rng = np.random.default_rng(7)
    yy, xx = np.mgrid[0:S, 0:S]
    tints = [(200, 216, 255), (244, 246, 255), (255, 244, 220), (255, 214, 160), (255, 190, 140)]
    for k in range(900):
        x, y = rng.random() * S, rng.random() * S
        b = rng.random() ** 6
        r = .45 + b * 1.4
        c = np.array(tints[rng.integers(len(tints))])
        for ox in (-S, 0, S):
            for oy in (-S, 0, S):
                cx, cy = x + ox, y + oy
                if -6 < cx < S + 6 and -6 < cy < S + 6:
                    x0, x1 = int(max(0, cx - 8)), int(min(S, cx + 9))
                    y0, y1 = int(max(0, cy - 8)), int(min(S, cy + 9))
                    if x0 >= x1 or y0 >= y1:
                        continue
                    dx = xx[y0:y1, x0:x1] - cx
                    dy = yy[y0:y1, x0:x1] - cy
                    blob = np.exp(-(dx * dx + dy * dy) / (2 * r * r)) * (.35 + .65 * b)
                    if b > .55:
                        spike = (np.exp(-dy * dy / .3) * np.exp(-np.abs(dx) / (2.5 + 4 * b))
                                 + np.exp(-dx * dx / .3) * np.exp(-np.abs(dy) / (2.5 + 4 * b))) * .6 * b
                        blob = blob + spike
                    img[y0:y1, x0:x1] += blob[..., None] * c
    return to_image(img)


def nebula():
    """256x256 tileable nebula, light on black: violet and blue clouds threaded with filaments, pink and a touch of
    the TVA's orange where they are thickest."""
    S = 256
    base = tile_fbm(S, S, 3, 5, 211)
    warp = tile_fbm(S, S, 4, 4, 223)
    ridge = 1 - np.abs(tile_fbm(S, S, 5, 5, 237) * 2 - 1)
    cloud = smooth(.42, .78, base * .7 + warp * .3)
    fil = smooth(.72, .97, ridge) * cloud
    hue = tile_fbm(S, S, 2, 3, 251)
    col = ramp(hue, [(0, (40, 60, 210)), (.45, (120, 70, 255)), (.75, (230, 90, 200)), (1, (255, 140, 60))])
    img = col * (cloud[..., None] * .55 + fil[..., None] * .9)
    img += np.array([255, 230, 255]) * (fil ** 3)[..., None] * .35
    return to_image(img)


def galaxy():
    """128x128 spiral galaxy seen at a slant, light on black: a bright core and two arms of stars."""
    S = 128
    yy, xx = np.mgrid[0:S, 0:S]
    x = (xx - S / 2 + .5) / (S / 2)
    y = (yy - S / 2 + .5) / (S / 2) / .55
    r = np.sqrt(x * x + y * y) + 1e-6
    a = np.arctan2(y, x)
    arms = (np.cos(2 * (a - 2.6 * np.log(r + .05))) * .5 + .5) ** 3
    grain = tile_fbm(S, S, 10, 3, 307)
    disc = np.exp(-r * 2.6)
    light = arms * disc * (.5 + grain) * 1.4 + np.exp(-r * r * 60) * 1.3
    col = ramp(np.clip(r * 1.2, 0, 1), [(0, (255, 240, 220)), (.3, (220, 200, 255)), (1, (90, 110, 255))])
    img = col * np.clip(light, 0, 1.5)[..., None] * np.where(r < 1, 1, 0)[..., None]
    return to_image(img)


# ---------------------------------------------------------------- planets

def planet_gas():
    """128x64 banded gas giant (cream, tan, rust and white belts swirled by storms, a great red spot)."""
    W, H = 128, 64
    p, lat = sphere_points(W, H)
    turb = fbm3(p, 3, 5, 401)
    bands = np.sin(lat * 9 + (turb - .5) * 2.4) * .5 + .5
    fine = np.sin(lat * 31 + (turb - .5) * 4) * .5 + .5
    t = bands * .75 + fine * .25
    col = ramp(t, [(0, (120, 64, 36)), (.3, (196, 128, 74)), (.55, (232, 200, 150)), (.8, (250, 238, 214)), (1, (210, 160, 110))])
    # The spot: an oval storm south of the equator
    spot_lon, spot_lat = 2.2, -.38
    lon = (np.arange(W) + .5) / W * 2 * math.pi
    dlon = (lon[None, :] - spot_lon + math.pi) % (2 * math.pi) - math.pi
    d = np.sqrt((dlon / .42) ** 2 + ((lat - spot_lat) / .13) ** 2)
    spot = smooth(1, .45, d)
    col = col * (1 - spot[..., None]) + np.array([200, 80, 50]) * spot[..., None]
    col *= (.85 + .3 * turb)[..., None]
    return to_image(col)


def planet_ocean():
    """128x64 living world: deep and shallow seas, green and ochre lands, white caps at the poles, swirls of cloud."""
    W, H = 128, 64
    p, lat = sphere_points(W, H)
    land = fbm3(p, 2.2, 6, 503)
    height = smooth(.5, .72, land)
    sea = ramp(smooth(.25, .52, land), [(0, (14, 40, 110)), (1, (40, 120, 200))])
    ground = ramp(height, [(0, (70, 140, 60)), (.5, (120, 150, 70)), (1, (170, 140, 90))])
    is_land = (land > .52)[..., None]
    col = np.where(is_land, ground, sea)
    ice = smooth(.78, .9, np.abs(np.sin(lat)) + (fbm3(p, 4, 3, 509) - .5) * .2)
    col = col * (1 - ice[..., None]) + np.array([236, 244, 250]) * ice[..., None]
    clouds = smooth(.55, .75, fbm3(p * np.array([1, 2.2, 1]), 2.5, 5, 521))
    col = col * (1 - clouds[..., None] * .85) + np.array([250, 252, 255]) * clouds[..., None] * .85
    return to_image(col)


def planet_ice():
    """128x64 ice giant: deep azure and violet in soft bands, pale streaks, a dark storm."""
    W, H = 128, 64
    p, lat = sphere_points(W, H)
    turb = fbm3(p, 2.5, 4, 601)
    t = np.sin(lat * 6 + (turb - .5) * 1.5) * .5 + .5
    col = ramp(t, [(0, (40, 60, 170)), (.5, (70, 120, 230)), (1, (110, 90, 230))])
    streak = smooth(.8, .95, np.sin(lat * 23 + turb * 3) * .5 + .5) * smooth(.4, .8, fbm3(p, 5, 3, 611))
    col = col * (1 - streak[..., None] * .6) + np.array([220, 236, 255]) * streak[..., None] * .6
    lon = (np.arange(W) + .5) / W * 2 * math.pi
    dlon = (lon[None, :] - 4.1 + math.pi) % (2 * math.pi) - math.pi
    d = np.sqrt((dlon / .3) ** 2 + ((lat + .3) / .12) ** 2)
    col *= (1 - smooth(1, .4, d) * .55)[..., None]
    return to_image(col)


def planet_ember():
    """128x64 molten world: a crust of black and brown rock split by glowing orange cracks (the TVA's orange)."""
    W, H = 128, 64
    p, lat = sphere_points(W, H)
    crust = fbm3(p, 3, 5, 701)
    ridge = 1 - np.abs(fbm3(p, 4, 4, 709) * 2 - 1)
    crack = smooth(.9, .985, ridge) * smooth(.35, .6, crust)
    rock = ramp(crust, [(0, (22, 14, 12)), (.6, (60, 36, 26)), (1, (96, 64, 44))])
    glow = ramp(crack, [(0, (120, 30, 0)), (.5, (255, 120, 20)), (1, (255, 230, 150))])
    col = rock * (1 - crack[..., None]) + glow * crack[..., None]
    return to_image(col)


def moon():
    """128x64 moon: grey regolith, dark seas, and craters with bright rims (the largest throwing rays)."""
    W, H = 128, 64
    p, lat = sphere_points(W, H)
    base = fbm3(p, 3, 6, 801)
    seas = smooth(.55, .7, fbm3(p, 1.5, 4, 809))
    col = ramp(base, [(0, (110, 110, 116)), (1, (200, 200, 206))])
    col = col * (1 - seas[..., None] * .4)
    rng = np.random.default_rng(811)
    for k in range(60):
        v = rng.normal(size=3)
        v /= np.linalg.norm(v)
        r = .04 + rng.random() ** 3 * .22
        d = np.arccos(np.clip(p @ v, -1, 1)) / r
        rim = np.exp(-((d - 1) / .18) ** 2)
        bowl = smooth(1, .2, d)
        col = col * (1 - bowl[..., None] * .2) + np.array([235, 235, 240]) * rim[..., None] * .12
        if r > .17:
            rays = (np.exp(-np.maximum(d - 1, 0) * .8) * smooth(1, 1.6, d)
                    * (np.cos(np.arctan2(p[..., 1] - v[1], p[..., 0] - v[0]) * 9) * .5 + .5))
            col += np.array([60, 60, 64]) * rays[..., None] * .5
    return to_image(col)


# ---------------------------------------------------------------- rings, disk, glows

def ring():
    """128x8 planetary ring, u across its width (inner edge to outer), v along it: bands and gaps of dusty ice."""
    W, H = 128, 8
    u = (np.arange(W) + .5) / W
    rng = np.random.default_rng(901)
    dens = np.zeros(W)
    for k in range(14):
        c, w, a = rng.random(), .01 + rng.random() * .08, .4 + rng.random() * .6
        dens += a * np.exp(-((u - c) / w) ** 2)
    dens = np.clip(dens, 0, 1) * smooth(0, .08, u) * smooth(1, .9, u)
    dens *= np.where(np.abs(u - .62) < .03, .1, 1)
    col = ramp(u, [(0, (180, 170, 150)), (.5, (226, 214, 190)), (1, (160, 150, 140))])
    alpha = (dens * 230)[None, :].repeat(H, 0)
    return to_image(col[None, :, :].repeat(H, 0), alpha)


def accretion():
    """256x64 accretion disk, light on black, u round the disk (tiling), v from the inner edge out: white-hot within,
    orange, then deep red, streaked by the gas spiralling in."""
    W, H = 256, 64
    v = (np.arange(H) + .5) / H
    streak = tile_fbm(W, H, 24, 4, 1003, aspect=.08)
    streak2 = tile_fbm(W, H, 60, 3, 1011, aspect=.05)
    heat = 1 - v
    col = ramp(heat, [(0, (120, 10, 0)), (.4, (230, 70, 10)), (.7, (255, 160, 60)), (.9, (255, 236, 190)), (1, (255, 255, 255))])
    fade = smooth(0, .12, v) * smooth(1, .55, v)
    light = (fade[:, None] * (.45 + .75 * streak * (.6 + .6 * streak2)))
    return to_image(col[:, None, :].repeat(W, 1) * light[..., None])


def rim():
    """64x64 white ring of glow (an atmosphere seen edge-on, the photon ring), tinted in game."""
    S = 64
    yy, xx = np.mgrid[0:S, 0:S]
    r = np.hypot(xx - S / 2 + .5, yy - S / 2 + .5) / (S / 2)
    inner = np.exp(-((r - .74) / .12) ** 2)
    outer = np.exp(-((r - .74) / .07) ** 2)
    v = np.where(r < .74, inner, outer) * np.where(r < 1, 1, 0)
    return to_image(np.dstack([v * 255] * 3))


def vortex():
    """128x128 swirl of a time door, light on black: arms of orange winding into a cream-white heart."""
    S = 128
    yy, xx = np.mgrid[0:S, 0:S]
    x = (xx - S / 2 + .5) / (S / 2)
    y = (yy - S / 2 + .5) / (S / 2)
    r = np.sqrt(x * x + y * y) + 1e-6
    a = np.arctan2(y, x)
    arms = (np.cos(3 * (a + 3.2 * np.log(r))) * .5 + .5) ** 2
    grain = tile_fbm(S, S, 8, 3, 1101)
    light = (arms * (.6 + .6 * grain) * smooth(1, .5, r) + smooth(.35, 0, r) * 1.2) * smooth(1, .85, r)
    col = ramp(np.clip(1 - r, 0, 1), [(0, EMBER), (.4, ORANGE), (.75, AMBER), (1, CREAM)])
    return to_image(col * np.clip(light, 0, 1.3)[..., None])


def haze():
    """64x128 inside of a time door, light on black (scrolled upward in game): orange wisps rising, brighter toward the
    frame."""
    W, H = 64, 128
    n = tile_fbm(W, H, 4, 4, 1201, aspect=2)
    wisp = smooth(.45, .9, tile_fbm(W, H, 6, 3, 1213, aspect=.6))
    u = (np.arange(W) + .5) / W
    edge = (np.abs(u - .5) * 2) ** 3
    light = (.35 + .4 * n + .55 * wisp) * (.55 + .7 * edge[None, :])
    col = ramp(np.clip(light * .8, 0, 1), [(0, EMBER), (.5, ORANGE), (.85, AMBER), (1, CREAM)])
    return to_image(col * np.clip(light, 0, 1.2)[..., None])


def scanlines():
    """64x64 tileable scanlines with flecks, light on black: the glitch of a temporal echo (scrolled in game)."""
    S = 64
    y = np.arange(S)
    lines = np.where(y % 4 == 0, 1.0, np.where(y % 4 == 1, .45, .12))
    noise = tile_fbm(S, S, 16, 2, 1301)
    band = smooth(.6, .8, tile_noise(S, S, 1, 8, 1307))
    light = lines[:, None] * (.6 + .4 * noise) + band * .35
    return to_image(np.dstack([np.clip(light, 0, 1) * 255] * 3))


def cloud():
    """64x64 puff of storm cloud, white with alpha (tinted in game)."""
    S = 64
    yy, xx = np.mgrid[0:S, 0:S]
    r = np.hypot(xx - S / 2 + .5, yy - S / 2 + .5) / (S / 2)
    n = tile_fbm(S, S, 4, 4, 1401)
    alpha = smooth(1, .2, r + (n - .5) * .5) * (.55 + .45 * n)
    shade = 150 + 105 * n
    return to_image(np.dstack([shade] * 3), alpha * 255)


def clock():
    """256x256 TVA clock face, with alpha: a bronze bezel edged in glowing orange, a dark glass face with fine rings,
    sixty minute ticks, twelve bright hour bars (a double bar at twelve), a ring of art-deco segments, a hub. The
    hands are drawn in game."""
    S = 256
    B = S * 4
    img = Image.new("RGBA", (B, B), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = B / 2

    def circle(r, **kw):
        d.ellipse((c - r, c - r, c + r, c + r), **kw)
    R = B * .49
    circle(R, fill=BRONZE + (255,))
    circle(R * .985, outline=ORANGE + (255,), width=int(B * .012))
    circle(R * .93, fill=(28, 16, 10, 255))
    circle(R * .9, fill=(20, 12, 20, 205))
    for k in range(3):
        circle(R * (.82 - k * .16), outline=(140, 70, 30, 120), width=max(2, int(B * .003)))
    for m in range(60):
        a = m / 60 * math.tau - math.pi / 2
        hour = m % 5 == 0
        r0 = R * (.74 if hour else .8)
        r1 = R * .87
        w = int(B * (.022 if hour else .006))
        col = (AMBER if hour else ORANGE) + (255,)
        if m == 0:
            for off in (-.035, .035):
                aa = a + off
                d.line((c + math.cos(aa) * r0 * .96, c + math.sin(aa) * r0 * .96, c + math.cos(aa) * r1, c + math.sin(aa) * r1), fill=CREAM + (255,), width=w)
        else:
            d.line((c + math.cos(a) * r0, c + math.sin(a) * r0, c + math.cos(a) * r1, c + math.sin(a) * r1), fill=col, width=w)
    # Art-deco ring: twelve arcs between the hours, and a glowing inner circle
    for k in range(12):
        a0 = math.degrees(k / 12 * math.tau) + 4
        d.arc((c - R * .6, c - R * .6, c + R * .6, c + R * .6), a0, a0 + 22, fill=ORANGE + (230,), width=int(B * .014))
    circle(R * .42, outline=AMBER + (200,), width=int(B * .006))
    circle(R * .1, fill=BRONZE + (255,), outline=ORANGE + (255,), width=int(B * .01))
    img = img.resize((S, S), Image.LANCZOS)
    glow = img.filter(ImageFilter.GaussianBlur(3))
    out = Image.alpha_composite(glow, img)
    return out


# ---------------------------------------------------------------- icons

def _c(c, a=255):
    return tuple(int(v) for v in c) + (a,)


def _icon(draw, cx=.5, cy=.5, inner=(30, 30, 80)):
    return g.painted(64, draw, (cx, cy, inner, NIGHT), 16)


def _stars(d, S, seed, n=7):
    rnd = np.random.default_rng(seed)
    for _ in range(n):
        x, y = rnd.random() * S, rnd.random() * S
        r = S * (.008 + rnd.random() * .012)
        d.ellipse((x - r, y - r, x + r, y + r), fill=_c(STAR, 200))


def _planet(d, S, x, y, r, base, light, dark):
    d.ellipse(((x - r) * S, (y - r) * S, (x + r) * S, (y + r) * S), fill=_c(dark))
    d.ellipse(((x - r * .92) * S, (y - r * .98) * S, (x + r * .7) * S, (y + r * .78) * S), fill=_c(base))
    d.ellipse(((x - r * .7) * S, (y - r * .75) * S, (x - r * .05) * S, (y - r * .15) * S), fill=_c(light, 200))


def _door(d, S, x, y, w, h):
    for k, a in ((.14, 60), (.08, 120)):
        d.rounded_rectangle(((x - w / 2 - k) * S, (y - h / 2 - k) * S, (x + w / 2 + k) * S, (y + h / 2 + k) * S), radius=S * .03, outline=_c(ORANGE, a), width=S // 20)
    d.rectangle(((x - w / 2) * S, (y - h / 2) * S, (x + w / 2) * S, (y + h / 2) * S), fill=_c(EMBER, 230))
    for k, col in ((.12, AMBER), (.3, CREAM)):
        i = w * k
        d.rectangle(((x - w / 2 + i) * S, (y - h / 2 + i) * S, (x + w / 2 - i) * S, (y + h / 2 - i) * S), fill=_c(col, 230))


def _clock(d, S, x, y, r, hands=(.0, .33)):
    d.ellipse(((x - r) * S, (y - r) * S, (x + r) * S, (y + r) * S), fill=_c(BRONZE), outline=_c(ORANGE), width=max(1, S // 24))
    d.ellipse(((x - r * .78) * S, (y - r * .78) * S, (x + r * .78) * S, (y + r * .78) * S), fill=_c((30, 18, 22)))
    for k in range(12):
        a = k / 12 * math.tau
        d.line(((x + math.cos(a) * r * .55) * S, (y + math.sin(a) * r * .55) * S, (x + math.cos(a) * r * .74) * S, (y + math.sin(a) * r * .74) * S),
               fill=_c(AMBER), width=max(1, S // 40))
    for t, L in zip(hands, (.65, .45)):
        a = t * math.tau - math.pi / 2
        d.line((x * S, y * S, (x + math.cos(a) * r * L) * S, (y + math.sin(a) * r * L) * S), fill=_c(CREAM), width=max(1, S // 24))


def _silhouette(d, S, x, y, h, color, a=255):
    """A standing figure {h} tall with its feet at (x, y)."""
    w = h * .28
    d.ellipse(((x - w * .45) * S, (y - h) * S, (x + w * .45) * S, (y - h + w * .9) * S), fill=_c(color, a))
    d.rounded_rectangle(((x - w * .6) * S, (y - h + w * .95) * S, (x + w * .6) * S, (y - h * .38) * S), radius=S * .02, fill=_c(color, a))
    d.rectangle(((x - w * .5) * S, (y - h * .4) * S, (x - w * .08) * S, y * S), fill=_c(color, a))
    d.rectangle(((x + w * .08) * S, (y - h * .4) * S, (x + w * .5) * S, y * S), fill=_c(color, a))


def pocket_universe_icon():
    def draw(d, img, S):
        _stars(d, S, 1, 10)
        # The floating island under its stopped clock
        d.polygon([(.18 * S, .66 * S), (.82 * S, .66 * S), (.6 * S, .78 * S), (.5 * S, .95 * S), (.4 * S, .8 * S)], fill=_c((90, 70, 60)))
        d.rectangle((.18 * S, .6 * S, .82 * S, .67 * S), fill=_c((70, 170, 70)))
        _door(d, S, .5, .5, .16, .22)
        _clock(d, S, .5, .22, .16, (0, .25))
    return _icon(draw, .5, .4, (40, 30, 90))


def time_door_icon():
    def draw(d, img, S):
        _door(d, S, .5, .52, .42, .72)
        _silhouette(d, S, .5, .86, .5, (60, 20, 0), 180)
    return _icon(draw, .5, .5, (110, 50, 10))


def pruning_baton_icon():
    def draw(d, img, S):
        for k in range(14):
            a = k / 14 * math.tau
            r = .1 + (k % 3) * .05
            d.ellipse(((.72 + math.cos(a) * r - .03) * S, (.3 + math.sin(a) * r - .03) * S, (.72 + math.cos(a) * r + .03) * S, (.3 + math.sin(a) * r + .03) * S), fill=_c(AMBER))
        d.line((.18 * S, .85 * S, .66 * S, .37 * S), fill=_c((60, 50, 50)), width=S // 9)
        d.line((.18 * S, .85 * S, .66 * S, .37 * S), fill=_c((140, 130, 130)), width=S // 20)
        d.line((.6 * S, .43 * S, .74 * S, .29 * S), fill=_c(ORANGE), width=S // 7)
        d.line((.62 * S, .41 * S, .72 * S, .31 * S), fill=_c(CREAM), width=S // 16)
    return _icon(draw, .7, .3, (110, 50, 10))


def time_loop_icon():
    def draw(d, img, S):
        _silhouette(d, S, .3, .82, .5, ORANGE, 90)
        _silhouette(d, S, .5, .82, .5, ORANGE, 160)
        _silhouette(d, S, .68, .82, .5, (40, 30, 60), 255)
        d.ellipse((.12 * S, .45 * S, .88 * S, .68 * S), outline=_c(GOLD), width=S // 14)
        d.polygon([(.84 * S, .5 * S), (.95 * S, .58 * S), (.8 * S, .62 * S)], fill=_c(GOLD))
    return _icon(draw, .5, .55, (90, 50, 20))


def variant_icon():
    def draw(d, img, S):
        _silhouette(d, S, .35, .9, .7, (50, 40, 80))
        for k, dx in enumerate((-.03, 0, .03)):
            _silhouette(d, S, .66 + dx, .9, .7, ORANGE, 80 + k * 50)
        for y in range(8):
            d.line((.45 * S, (.3 + y * .07) * S, .9 * S, (.3 + y * .07) * S), fill=_c(CREAM, 70), width=1)
    return _icon(draw, .65, .55, (90, 40, 20))


def time_stop_icon():
    def draw(d, img, S):
        d.ellipse((.06 * S, .06 * S, .94 * S, .94 * S), outline=_c(ORANGE, 160), width=S // 18)
        d.ellipse((.12 * S, .12 * S, .88 * S, .88 * S), fill=_c((40, 30, 60)))
        _clock(d, S, .5, .5, .3, (.0, .0))
        d.rectangle((.4 * S, .4 * S, .46 * S, .6 * S), fill=_c(CREAM))
        d.rectangle((.54 * S, .4 * S, .6 * S, .6 * S), fill=_c(CREAM))
    return _icon(draw, .5, .5, (80, 50, 40))


def captive_orbit_icon():
    def draw(d, img, S):
        _stars(d, S, 3)
        d.ellipse((.08 * S, .32 * S, .92 * S, .68 * S), outline=_c(ICE, 160), width=S // 24)
        _planet(d, S, .5, .5, .2, (200, 140, 90), (250, 220, 170), (110, 60, 40))
        d.ellipse((.8 * S, .38 * S, .92 * S, .5 * S), fill=_c((200, 60, 60)))
        for k in range(4):
            d.ellipse(((.72 - k * .07) * S, (.36 + k * .0) * S, (.76 - k * .07) * S, (.4) * S), fill=_c(ICE, 160 - k * 35))
    return _icon(draw, .5, .5, (40, 40, 100))


def comet_ride_icon():
    def draw(d, img, S):
        _stars(d, S, 4)
        for k in range(10):
            t = k / 10
            r = .03 + t * .12
            d.ellipse(((.15 + t * .5 - r) * S, (.85 - t * .5 - r) * S, (.15 + t * .5 + r) * S, (.85 - t * .5 + r) * S), fill=_c(ICE, int(40 + 150 * t)))
        d.ellipse((.6 * S, .18 * S, .86 * S, .44 * S), fill=_c(WHITE))
        d.ellipse((.64 * S, .22 * S, .8 * S, .38 * S), fill=_c((190, 220, 240)))
        _silhouette(d, S, .74, .24, .2, (30, 20, 50))
    return _icon(draw, .7, .3, (30, 50, 110))


def pocket_moon_icon():
    def draw(d, img, S):
        _stars(d, S, 5)
        d.ellipse((.1 * S, .55 * S, .9 * S, .8 * S), outline=_c(STAR, 110), width=S // 28)
        _silhouette(d, S, .5, .86, .5, (40, 30, 70))
        d.ellipse((.62 * S, .14 * S, .9 * S, .42 * S), fill=_c((210, 210, 216)))
        d.ellipse((.68 * S, .18 * S, .76 * S, .26 * S), fill=_c((150, 150, 160)))
        d.ellipse((.77 * S, .3 * S, .83 * S, .36 * S), fill=_c((160, 160, 168)))
    return _icon(draw, .75, .3, (50, 50, 100))


def living_constellation_icon():
    def draw(d, img, S):
        pts = [(.2, .75), (.32, .4), (.55, .2), (.75, .45), (.62, .8)]
        d.line([(x * S, y * S) for x, y in pts], fill=_c(GOLD, 200), width=S // 28)
        d.line((pts[1][0] * S, pts[1][1] * S, pts[3][0] * S, pts[3][1] * S), fill=_c(GOLD, 150), width=S // 32)
        for x, y in pts:
            d.ellipse(((x - .05) * S, (y - .05) * S, (x + .05) * S, (y + .05) * S), fill=_c(STAR))
            d.line(((x - .09) * S, y * S, (x + .09) * S, y * S), fill=_c(STAR), width=1)
            d.line((x * S, (y - .09) * S, x * S, (y + .09) * S), fill=_c(STAR), width=1)
    return _icon(draw, .5, .5, (40, 40, 110))


def asteroid_belt_icon():
    def draw(d, img, S):
        rnd = np.random.default_rng(9)
        for k in range(11):
            a = k / 11 * math.tau
            x, y = .5 + math.cos(a) * .36, .5 + math.sin(a) * .2
            r = .04 + rnd.random() * .05
            d.polygon([((x + math.cos(b) * r * (.7 + rnd.random() * .5)) * S, (y + math.sin(b) * r * (.7 + rnd.random() * .5)) * S)
                       for b in np.linspace(0, math.tau, 7)[:-1]], fill=_c((150, 130, 115)), outline=_c((70, 60, 55)))
        d.ellipse((.4 * S, .4 * S, .6 * S, .6 * S), fill=_c((255, 200, 150), 120))
    return _icon(draw, .5, .5, (60, 45, 70))


def zero_gravity_icon():
    def draw(d, img, S):
        d.ellipse((.06 * S, .06 * S, .94 * S, .94 * S), outline=_c(BLUE, 160), width=S // 22)
        _silhouette(d, S, .45, .62, .4, (40, 30, 70))
        for x, y, r in ((.7, .3, .07), (.28, .3, .05), (.72, .72, .06)):
            d.rectangle(((x - r) * S, (y - r) * S, (x + r) * S, (y + r) * S), fill=_c((140, 110, 80)))
        for x in (.3, .5, .7):
            d.line((x * S, .92 * S, x * S, .8 * S), fill=_c(ICE, 160), width=S // 32)
            d.polygon([((x - .04) * S, .82 * S), ((x + .04) * S, .82 * S), (x * S, .74 * S)], fill=_c(ICE, 160))
    return _icon(draw, .5, .5, (30, 40, 110))


def black_hole_icon():
    def draw(d, img, S):
        d.ellipse((.04 * S, .36 * S, .96 * S, .64 * S), fill=_c((200, 70, 10)))
        d.ellipse((.12 * S, .41 * S, .88 * S, .59 * S), fill=_c((255, 170, 70)))
        d.ellipse((.22 * S, .2 * S, .78 * S, .76 * S), fill=_c((255, 220, 160)))
        d.ellipse((.26 * S, .24 * S, .74 * S, .72 * S), fill=_c((0, 0, 0)))
        d.chord((.04 * S, .36 * S, .96 * S, .64 * S), 0, 180, fill=_c((255, 150, 50)))
        d.chord((.2 * S, .42 * S, .8 * S, .58 * S), 0, 180, fill=_c((255, 236, 200)))
    return _icon(draw, .5, .5, (70, 30, 30))


def reset_charge_icon():
    def draw(d, img, S):
        d.ellipse((.04 * S, .04 * S, .96 * S, .96 * S), outline=_c(ORANGE, 90), width=S // 16)
        d.rounded_rectangle((.36 * S, .2 * S, .64 * S, .86 * S), radius=S * .05, fill=_c((60, 50, 50)))
        for y in (.32, .48, .64):
            d.rectangle((.36 * S, y * S, .64 * S, (y + .05) * S), fill=_c(ORANGE))
        d.ellipse((.42 * S, .1 * S, .58 * S, .26 * S), fill=_c(CREAM))
        d.arc((.12 * S, .12 * S, .88 * S, .88 * S), 200, 340, fill=_c(AMBER), width=S // 16)
        d.polygon([(.12 * S, .4 * S), (.2 * S, .3 * S), (.24 * S, .44 * S)], fill=_c(AMBER))
    return _icon(draw, .5, .5, (110, 50, 10))


def end_of_time_icon():
    def draw(d, img, S):
        rnd = np.random.default_rng(13)
        for k in range(16):
            x, y, r = .2 + rnd.random() * .6, .15 + rnd.random() * .4, .1 + rnd.random() * .12
            col = (120, 40, 140) if k % 3 else (220, 90, 40)
            d.ellipse(((x - r) * S, (y - r) * S, (x + r) * S, (y + r) * S), fill=_c(col, 200))
        for x0 in (.35, .62):
            pts = [(x0, .45), (x0 - .06, .62), (x0 + .03, .7), (x0 - .04, .9)]
            d.line([(x * S, y * S) for x, y in pts], fill=_c(AMBER), width=S // 22)
        d.ellipse((.42 * S, .28 * S, .58 * S, .4 * S), fill=_c(CREAM))
    return _icon(draw, .5, .35, (90, 30, 60))


ICONS = [
    ("pocket_universe", pocket_universe_icon),
    ("time_door", time_door_icon),
    ("pruning_baton", pruning_baton_icon),
    ("time_loop", time_loop_icon),
    ("variant", variant_icon),
    ("time_stop", time_stop_icon),
    ("captive_orbit", captive_orbit_icon),
    ("comet_ride", comet_ride_icon),
    ("pocket_moon", pocket_moon_icon),
    ("living_constellation", living_constellation_icon),
    ("asteroid_belt", asteroid_belt_icon),
    ("zero_gravity", zero_gravity_icon),
    ("black_hole", black_hole_icon),
    ("reset_charge", reset_charge_icon),
    ("end_of_time", end_of_time_icon),
]


# ---------------------------------------------------------------- the Codex Out of Time

def codex_icon():
    def draw(d, img, S):
        d.rounded_rectangle((.16 * S, .08 * S, .84 * S, .92 * S), radius=S * .05, fill=_c((14, 14, 40)), outline=_c(OUTLINE), width=S // 32)
        d.rectangle((.76 * S, .12 * S, .84 * S, .88 * S), fill=_c(CREAM))
        d.rectangle((.16 * S, .08 * S, .26 * S, .92 * S), fill=_c((40, 26, 20)))
        for y in (.2, .5, .8):
            d.rectangle((.16 * S, (y - .02) * S, .26 * S, (y + .02) * S), fill=_c(ORANGE))
        rnd = np.random.default_rng(17)
        for _ in range(9):
            x, y = .3 + rnd.random() * .42, .14 + rnd.random() * .72
            d.ellipse(((x - .012) * S, (y - .012) * S, (x + .012) * S, (y + .012) * S), fill=_c(STAR))
        _clock(d, S, .51, .5, .19, (.0, .3))
        d.rounded_rectangle((.2 * S, .1 * S, .78 * S, .9 * S), radius=S * .04, outline=_c(AMBER, 200), width=S // 40)
    return g.painted(64, draw, None, 16)


def codex_model_texture():
    """64x64 texture of the 3D book, laid out like the other books: a cover of deep space (nebula and stars) framed
    in orange, the TVA clock set in its middle, a bronze spine banded in orange, gilt pages, orange glowing studs."""
    s = 4
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    W, H = 12 * s, 9 * s
    neb = nebula().resize((W, H), Image.LANCZOS)
    sky = Image.new("RGBA", (W, H), DEEP + (255,))
    sky = Image.alpha_composite(sky, Image.blend(Image.new("RGBA", (W, H), (0, 0, 0, 0)), neb, .8))
    img.paste(sky, (0, 0))
    rnd = np.random.default_rng(23)
    for _ in range(26):
        x, y = int(rnd.random() * W), int(rnd.random() * H)
        img.putpixel((x, y), STAR + (255,) if rnd.random() < .7 else GOLD + (255,))
    d.rectangle((1, 1, W - 2, H - 2), outline=ORANGE + (255,))
    d.rectangle((3, 3, W - 4, H - 4), outline=(150, 70, 20, 255))
    face = clock().resize((22, 22), Image.LANCZOS)
    img.alpha_composite(face, (W // 2 - 11, H // 2 - 11))
    cx, cy = W // 2, H // 2
    d.line((cx, cy, cx, cy - 7), fill=CREAM + (255,))
    d.line((cx, cy, cx + 4, cy + 2), fill=CREAM + (255,))

    def box(u0, v0, u1, v1, fill):
        d.rectangle((u0 * s, v0 * s, u1 * s - 1, v1 * s - 1), fill=fill + (255,))
    box(12, 0, 13, 9, (120, 60, 20))
    from explosion_assets import fbm
    for y in range(0, 8 * s):
        for x in range(13 * s, 16 * s):
            img.putpixel((x, y), g.lerp(g.CREAM, (176, 168, 140), fbm(x / 3, y / 3, 2, 29) * .8) + (255,))
    for y in range(1, 8 * s, 2):
        d.line((13 * s, y, 16 * s - 1, y), fill=g.CREAM_SHADE + (255,))
    for x in range(0, 12 * s):
        for y in range(9 * s, 12 * s):
            img.putpixel((x, y), g.lerp((40, 24, 16), (70, 42, 24), fbm(x / 4, y / 4, 2, 43)) + (255,))
    for y in (9 * s + 2, 12 * s - 3):
        d.line((0, y, 12 * s - 1, y), fill=ORANGE + (255,))
    box(0, 12, 12, 15, g.CREAM)
    for x in range(0, 12 * s, 2):
        d.line((x, 12 * s, x, 15 * s - 1), fill=g.CREAM_SHADE + (255,))
    box(12, 9, 14, 11, ORANGE)
    for y in range(9 * s, 11 * s):
        for x in range(14 * s, 16 * s):
            t = math.hypot(x - 14.8 * s, y - 9.8 * s) / (1.4 * s)
            img.putpixel((x, y), g.lerp(CREAM, AMBER, min(1, t ** .8)) + (255,))
    box(12, 11, 16, 12, (120, 60, 20))
    return img


def affinity_ring_cosmos():
    return g.from_grid([
        "................",
        "......oooo......",
        ".....obBBbo.....",
        "....oOOOOOOo....",
        "....oooooooo....",
        "...ohllllbbso...",
        "..ohlo....obso..",
        "..olo......oso..",
        "..olo......oso..",
        "..olo......oso..",
        "..obso....osso..",
        "...obbbbbssso...",
        "....oooooooo....",
        "................",
        "................",
        "................",
    ], {"o": OUTLINE, "h": (236, 240, 255), "l": (190, 196, 220), "b": (130, 136, 170), "s": (84, 88, 120),
        "B": BLUE, "O": ORANGE})


def main():
    g.save(stars(), D + "stars.png")
    g.save(nebula(), D + "nebula.png")
    g.save(galaxy(), D + "galaxy.png")
    g.save(planet_gas(), D + "planet_gas.png")
    g.save(planet_ocean(), D + "planet_ocean.png")
    g.save(planet_ice(), D + "planet_ice.png")
    g.save(planet_ember(), D + "planet_ember.png")
    g.save(moon(), D + "moon.png")
    g.save(ring(), D + "ring.png")
    g.save(accretion(), D + "accretion.png")
    g.save(rim(), D + "rim.png")
    g.save(vortex(), D + "vortex.png")
    g.save(haze(), D + "haze.png")
    g.save(scanlines(), D + "scanlines.png")
    g.save(cloud(), D + "cloud.png")
    g.save(clock(), D + "clock.png")
    for name, icon in ICONS:
        g.save(icon(), f"textures/gui/spell_icons/{name}.png")
    g.save(affinity_ring_cosmos(), "textures/item/affinity_ring_cosmos.png")
    g.write_json("models/item/affinity_ring_cosmos.json", {"parent": "item/generated", "textures": {"layer0": "arcanes_oublies:item/affinity_ring_cosmos"}})
    g.save(codex_icon(), "textures/item/codex_out_of_time.png")
    g.save(codex_model_texture(), "textures/item/codex_out_of_time_3d.png")
    g.book_models("codex_out_of_time")


if __name__ == "__main__":
    main()
