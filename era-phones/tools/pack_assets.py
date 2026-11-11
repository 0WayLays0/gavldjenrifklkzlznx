"""The backpacks (original designs, drawn here), worn on the back: the red school bag with its flap and keyring, the
faded denim one with orange stitching, the army canvas one with its two pockets and buckles, the brown leather
rucksack with strapped flap, the yellow one covered in sewn-on patches, the purple and pink one with stars, the black
sports bag with white stripes, the big orange hiking pack with a bedroll on top. And the open bag's panel."""
import math

import numpy as np
from PIL import Image, ImageDraw, ImageFont

import bike_assets as ba
import gear_assets as ga
import kit
from models import Model, vgrad

NAMES = ["sac_a_dos_rouge", "sac_a_dos_jean", "sac_a_dos_militaire", "sac_a_dos_cuir", "sac_a_dos_jaune", "sac_a_dos_violet",
         "sac_a_dos_sport", "sac_a_dos_rando"]


def cloth(base, kind="weave", stitch=None, seed=0, edge=.72):
    """A fabric face: {kind} weave (cotton), denim (diagonal twill), leather (grain), nylon (sheen), canvas (coarse);
    a stitched seam round its edge in {stitch}."""
    def p(tile):
        w, h = tile.size
        rng = np.random.default_rng(seed + w * 3 + h * 7)
        ys, xs = np.mgrid[0:h, 0:w]
        a = np.zeros((h, w, 4))
        a[..., :3] = np.array(base[:3], float)
        a[..., 3] = 255
        if kind == "denim":
            a[..., :3] *= (.9 + .12 * (((xs + ys) % 4) < 2))[..., None]
            a[..., :3] += (kit.tnoise(w, h, (4, 4), seed)[..., None] - .5) * 40 * np.array([.6, .8, 1])
        elif kind == "leather":
            a[..., :3] *= (.86 + .2 * kit.tnoise(w, h, (6, 6), seed))[..., None]
            a[..., :3] *= (1 - .06 * (rng.random((h, w)) < .08))[..., None]
        elif kind == "nylon":
            a[..., :3] *= (.85 + .3 * np.clip(1 - np.abs(ys / max(1, h) - .35) * 3, 0, 1))[..., None]
        elif kind == "canvas":
            a[..., :3] *= (.88 + .1 * ((xs % 3 == 0) | (ys % 3 == 0)) + .1 * kit.tnoise(w, h, (5, 5), seed))[..., None]
        else:
            a[..., :3] *= (.92 + .08 * ((xs + ys) % 2))[..., None]
            a[..., :3] *= (.92 + .12 * kit.tnoise(w, h, (4, 4), seed))[..., None]
        a[..., :3] += rng.normal(0, 3, (h, w, 1))
        img = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, w - 1, h - 1), outline=ga.shade(base, edge), width=1)
        if stitch and w > 8 and h > 8:
            for x in range(3, w - 3, 4):
                d.line((x, 2, x + 1, 2), fill=tuple(stitch) + (255,))
                d.line((x, h - 3, x + 1, h - 3), fill=tuple(stitch) + (255,))
            for y in range(3, h - 3, 4):
                d.line((2, y, 2, y + 1), fill=tuple(stitch) + (255,))
                d.line((w - 3, y, w - 3, y + 1), fill=tuple(stitch) + (255,))
        tile.paste(img)
    return p


def with_extra(painter, extra):
    """{painter}, then {extra} drawn over."""
    def p(tile):
        painter(tile)
        extra(tile, ImageDraw.Draw(tile))
    return p


def zipper(y_frac=.12, color=(60, 60, 64)):
    def z(tile, d):
        w, h = tile.size
        y = int(h * y_frac)
        d.line((2, y, w - 3, y), fill=color + (255,), width=2)
        for x in range(3, w - 3, 2):
            d.point((x, y - 1), fill=(170, 170, 176, 255))
        d.rectangle((w * .7, y - 2, w * .7 + 3, y + 4), fill=(190, 190, 196, 255))
    return z


def patches(seed):
    def z(tile, d):
        w, h = tile.size
        rng = np.random.default_rng(seed)
        colors = [(200, 40, 40), (40, 90, 180), (40, 150, 70), (240, 240, 240), (230, 120, 30)]
        for k in range(5):
            r = rng.uniform(.1, .17) * min(w, h)
            x, y = rng.uniform(r, w - r), rng.uniform(r, h - r)
            c = colors[k % len(colors)]
            if k % 2:
                d.ellipse((x - r, y - r, x + r, y + r), fill=c + (255,), outline=(30, 30, 30, 255))
                d.ellipse((x - r * .45, y - r * .45, x + r * .45, y + r * .45), fill=(255, 230, 80, 255))
            else:
                d.rectangle((x - r, y - r * .7, x + r, y + r * .7), fill=c + (255,), outline=(30, 30, 30, 255))
                d.line((x - r * .6, y, x + r * .6, y), fill=(255, 255, 255, 255), width=max(1, int(r * .25)))
    return z


def stars(seed, color=(255, 214, 240)):
    def z(tile, d):
        w, h = tile.size
        rng = np.random.default_rng(seed)
        for _ in range(7):
            r = rng.uniform(.05, .1) * min(w, h)
            x, y = rng.uniform(r, w - r), rng.uniform(r, h - r)
            pts = []
            for k in range(10):
                a = -math.pi / 2 + k * math.pi / 5
                rr = r if k % 2 == 0 else r * .45
                pts.append((x + math.cos(a) * rr, y + math.sin(a) * rr))
            d.polygon(pts, fill=color + (255,))
    return z


def stripes(color=(236, 236, 236)):
    def z(tile, d):
        w, h = tile.size
        for k, x in enumerate((w * .3, w * .38, w * .46)):
            d.polygon([(x, h), (x + w * .05, h), (x + w * .45, 0), (x + w * .4, 0)], fill=color + (255,))
        font = ImageFont.truetype(ba.kit_font(), max(8, int(h * .14)))
        d.text((w * .25, h * .2), "VOXA", font=font, fill=color + (255,), anchor="mm")
    return z


def keyring(tile, d):
    w, h = tile.size
    d.ellipse((w * .78, h * .1, w * .9, h * .26), outline=(200, 200, 206, 255), width=2)
    d.rectangle((w * .8, h * .26, w * .9, h * .42), fill=(240, 200, 40, 255), outline=(60, 50, 10, 255))


def buckle(m, x, y, z):
    m.box([x - .7, y - .5, z], [x + .7, y + .5, z + .25], {"*": ga.facet((190, 192, 196), 1.05)})
    m.box([x - .35, y - .2, z + .25], [x + .35, y + .2, z + .3], {"*": (40, 40, 44)})


def backpack(i):
    name = NAMES[i]
    m = Model(name, 1024, 20)
    # The styles: base colour, the cloth, stitching, the front's extras, what the bag has
    S = [
        dict(base=(184, 46, 40), kind="weave", stitch=(236, 206, 196), flap=True, front=keyring, accent=(150, 30, 28)),
        dict(base=(78, 106, 156), kind="denim", stitch=(228, 140, 50), flap=False, front=None, accent=(60, 84, 128)),
        dict(base=(96, 108, 60), kind="canvas", stitch=(70, 78, 44), flap=True, front=None, accent=(70, 80, 44), sides=True, two=True),
        dict(base=(122, 74, 38), kind="leather", stitch=(222, 196, 150), flap=True, front=None, accent=(96, 56, 28), straps=True),
        dict(base=(226, 178, 58), kind="canvas", stitch=(120, 84, 20), flap=False, front=patches(4), accent=(190, 140, 40), body_extra=patches(9)),
        dict(base=(140, 76, 170), kind="nylon", stitch=(250, 180, 220), flap=False, front=stars(3), accent=(226, 110, 170)),
        dict(base=(40, 40, 46), kind="nylon", stitch=(120, 120, 128), flap=False, front=stripes(), accent=(30, 30, 34), sides=True),
        dict(base=(216, 112, 42), kind="nylon", stitch=(60, 60, 64), flap=True, front=None, accent=(120, 120, 128), sides=True, roll=True),
    ][i]
    base, accent = S["base"], S["accent"]
    body = cloth(base, S["kind"], S["stitch"], seed=i)
    body_front = with_extra(body, S["body_extra"]) if S.get("body_extra") else body
    acc = cloth(accent, S["kind"], S["stitch"], seed=i + 20)
    # The bag: its back against the player (low z), its front (high z) toward whoever looks
    x0, x1, y0, y1, z0, z1 = 3.2, 12.8, 1.6, 13.6, 5.6, 10.0
    m.box([x0, y0, z0], [x1, y1, z1], {"south": body_front, "north": cloth(ga.shade(base, .8), S["kind"], None, seed=i + 5), "east": body,
                                       "west": body, "up": body, "down": cloth(ga.shade(base, .7), S["kind"], None, seed=i + 6)})
    # The rounded top
    m.box([x0 + .5, y1, z0 + .4], [x1 - .5, y1 + .7, z1 - .4], {"*": body})
    # The carrying loop
    m.box([7.2, y1 + .7, 7.4], [8.8, y1 + 1.4, 8.2], {"*": acc})
    # The front pocket (two, side by side, for the army bag)
    if S.get("two"):
        for px in ((3.9, 7.8), (8.2, 12.1)):
            m.box([px[0], 2.4, z1], [px[1], 7.6, z1 + 1.3], {"south": acc, "east": acc, "west": acc, "up": acc, "down": acc})
            m.box([px[0] - .1, 6.4, z1 + 1.3], [px[1] + .1, 7.8, z1 + 1.55], {"*": acc})
            buckle(m, (px[0] + px[1]) / 2, 6.6, z1 + 1.55)
    else:
        front = with_extra(acc, S["front"]) if S["front"] else acc
        front = with_extra(front, zipper())
        m.box([4.0, 2.4, z1], [12.0, 8.2, z1 + 1.4], {"south": front, "east": acc, "west": acc, "up": acc, "down": acc})
    # The flap over the top, hanging down the front
    if S["flap"]:
        fl = cloth(ga.shade(accent, 1.05), S["kind"], S["stitch"], seed=i + 30)
        m.box([x0 - .15, 8.8, z1], [x1 + .15, y1 + .75, z1 + .45], {"south": fl, "east": fl, "west": fl, "down": fl, "up": fl})
        m.box([x0 - .15, y1 + .7, z0 + .2], [x1 + .15, y1 + 1.0, z1 + .45], {"*": fl})
        if S.get("straps") or i in (0, 7):
            for sx in (5.6, 10.4):
                m.box([sx - .45, 7.4, z1 + .45], [sx + .45, 12.8, z1 + .62], {"*": cloth(ga.shade(accent, .8), "leather", None, seed=i + 40)})
                buckle(m, sx, 9.4, z1 + .62)
    # Side pockets
    if S.get("sides"):
        sp = cloth(ga.shade(accent, .95), S["kind"], S["stitch"], seed=i + 50)
        for sx0, sx1 in ((x0 - 1.0, x0), (x1, x1 + 1.0)):
            m.box([sx0, 2.2, 6.4], [sx1, 7.6, 9.4], {"*": sp})
    # The bedroll, strapped on top
    if S.get("roll"):
        ga.octo(m, "x", 2.4, 13.6, y1 + 2.4, 7.8, 1.7, (64, 120, 160), end_a=ga.disc((90, 150, 190), (40, 90, 120)),
                end_b=ga.disc((90, 150, 190), (40, 90, 120)))
        for sx in (4.6, 11.4):
            m.box([sx - .4, y1 + .6, 5.9], [sx + .4, y1 + 4.2, 9.7], {"*": (50, 50, 54)})
    d = {
        "gui": {"rotation": [18, -28, 0], "translation": [0, -1, 0], "scale": [.95, .95, .95]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [.5, .5, .5]},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [.8, .8, .8]},
        # Carried by its loop
        "thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [0, -3, 1], "scale": [.6, .6, .6]},
        "thirdperson_lefthand": {"rotation": [0, -90, 0], "translation": [0, -3, 1], "scale": [.6, .6, .6]},
        "firstperson_righthand": {"rotation": [0, -150, 0], "translation": [2, 0, 0], "scale": [.5, .5, .5]},
        "firstperson_lefthand": {"rotation": [0, 150, 0], "translation": [-2, 0, 0], "scale": [.5, .5, .5]},
    }
    for e in m.elements:
        assert all(-16 <= v <= 32 for v in e["from"] + e["to"]), f"{name}: out of bounds"
    ga.held(m, d)
    dye = ["red_dye", "blue_dye", "green_dye", "brown_dye", "yellow_dye", "purple_dye", "black_dye", "orange_dye"][i]
    ga.shaped(name, ["LSL", "LDL", "LLL"], {"L": "minecraft:leather", "S": "minecraft:string", "D": f"minecraft:{dye}"})


def panel():
    """The open bag's panel: tan canvas, a darker hem stitched all round, rounded corners (176 x 140, drawn twice as big)."""
    W, H = 352, 280
    n = kit.tnoise(W, H, (10, 8), 77)
    ys, xs = np.mgrid[0:H, 0:W]
    base = np.array([196, 174, 132]) * (.9 + .12 * n[..., None]) * (.95 + .05 * (((xs // 2) + (ys // 2)) % 2))[..., None]
    img = Image.fromarray(np.dstack([np.clip(base, 0, 255), np.full((H, W), 255)]).astype(np.uint8), "RGBA")
    mask = Image.new("L", (W, H), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, W - 1, H - 1), radius=14, fill=255)
    img.putalpha(mask)
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((0, 0, W - 1, H - 1), radius=14, outline=(92, 70, 44, 255), width=4)
    d.rounded_rectangle((8, 8, W - 9, H - 9), radius=10, outline=(150, 124, 86, 255), width=2)
    for x in range(20, W - 20, 10):
        d.line((x, 14, x + 5, 14), fill=(110, 84, 52, 255), width=2)
        d.line((x, H - 15, x + 5, H - 15), fill=(110, 84, 52, 255), width=2)
    for y in range(20, H - 20, 10):
        d.line((14, y, 14, y + 5), fill=(110, 84, 52, 255), width=2)
        d.line((W - 15, y, W - 15, y + 5), fill=(110, 84, 52, 255), width=2)
    # A seam between the pockets and the hands
    d.line((24, 190, W - 24, 190), fill=(140, 114, 78, 255), width=2)
    kit.save(img, "textures/gui/pack/sac.png")


def main():
    for i in range(len(NAMES)):
        backpack(i)
    panel()


if __name__ == "__main__":
    main()
