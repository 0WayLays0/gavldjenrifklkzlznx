"""The event's things (original art, drawn here): the missing children's posters to stick on the walls (one child on
new, yellowed, torn, rain-washed, sun-faded or half torn-away paper; two, three, four children side by side; heaps
stuck one over another; whole walls covered, their edges meeting block after block), the children's school photos
drawn on them, photocopied; the code box, an old steel strongbox with brass number wheels, and its close-up; the star
fragment's glow and its item."""
import math

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

import bike_assets as ba
import gear_assets as ga
import kit
from models import Model, vgrad, round_alpha

FONTS = ba.kit_font().rsplit("/", 1)[0]
BOLD = FONTS + "/Oswald-Bold.woff"
HAND = FONTS + "/Caveat-Bold.woff"
MARKER = FONTS + "/PermanentMarker.woff"


def font(path, size):
    return ImageFont.truetype(path, max(6, int(size)))


# ------------------------------------------------------------------------------------------------ the children's photos

SKINS = [(236, 200, 172), (224, 182, 150), (196, 150, 112), (150, 104, 74), (110, 74, 52), (240, 210, 186)]
HAIRS = [(60, 40, 26), (110, 74, 40), (170, 120, 64), (220, 186, 120), (30, 24, 20), (140, 60, 30)]
SHIRTS = [(70, 90, 140), (150, 50, 50), (60, 110, 70), (200, 190, 160), (90, 70, 110), (40, 40, 48)]


def portrait(seed, w=120, h=150):
    """A child's school photo: the mottled backdrop, the shoulders and collar, the face, the hair (cut, colour and
    style by {seed}), the eyes, a shy smile. Drawn large and brought down, in colour; the posters photocopy it."""
    rng = np.random.default_rng(seed)
    k = 4
    W, H = w * k, h * k
    # The studio backdrop: a soft blue-grey cloud
    n = kit.tnoise(W, H, (5, 6), seed)
    base = np.array([120, 136, 160]) * (.75 + .35 * n[..., None])
    img = Image.fromarray(np.dstack([np.clip(base, 0, 255), np.full((H, W), 255)]).astype(np.uint8), "RGBA")
    d = ImageDraw.Draw(img)
    skin = SKINS[rng.integers(len(SKINS))]
    hair = HAIRS[rng.integers(len(HAIRS))]
    shirt = SHIRTS[rng.integers(len(SHIRTS))]
    style = int(rng.integers(6))
    cx, cy = W / 2 + rng.normal(0, W * .015), H * .44
    fw, fh = W * .2, H * .2
    # Shoulders and collar
    d.ellipse((cx - W * .48, H * .78, cx + W * .48, H * 1.35), fill=shirt + (255,))
    d.polygon([(cx - W * .12, H * .76), (cx, H * .86), (cx + W * .12, H * .76), (cx + W * .1, H * .82), (cx, H * .92), (cx - W * .1, H * .82)],
              fill=(235, 232, 225, 255))
    # The neck
    d.rectangle((cx - fw * .45, cy + fh * .6, cx + fw * .45, H * .82), fill=ga.shade(skin, .86))
    # Hair behind the head (long styles)
    if style == 1:
        # Falling either side of the face, down to the shoulders
        for sx in (-1, 1):
            x0, x1 = sorted((cx + sx * fw * 1.22, cx + sx * fw * .62))
            d.rounded_rectangle((x0, cy - fh * .9, x1, cy + fh * 1.45), radius=int(fw * .25), fill=ga.shade(hair, .8))
    # Ears, face
    for sx in (-1, 1):
        d.ellipse((cx + sx * fw * 1.02 - fw * .18, cy - fh * .1, cx + sx * fw * 1.02 + fw * .18, cy + fh * .35), fill=ga.shade(skin, .92))
    d.ellipse((cx - fw, cy - fh * 1.05, cx + fw, cy + fh * 1.0), fill=skin + (255,))
    # Cheeks a little rosy, the jaw's shadow
    for sx in (-1, 1):
        d.ellipse((cx + sx * fw * .55 - fw * .22, cy + fh * .2, cx + sx * fw * .55 + fw * .22, cy + fh * .45), fill=ga.shade((230, 150, 140), 1)[:3] + (60,))
    # Hair on top: by style
    top = ga.shade(hair, 1.0)
    if style == 0:
        # Short, side parting
        d.chord((cx - fw * 1.08, cy - fh * 1.25, cx + fw * 1.08, cy + fh * .2), 180, 360, fill=top)
        d.polygon([(cx - fw * 1.05, cy - fh * .3), (cx + fw * .2, cy - fh * .75), (cx + fw * 1.05, cy - fh * .35), (cx + fw, cy - fh * .9),
                   (cx - fw, cy - fh * .9)], fill=top)
    elif style == 1:
        # Long with a fringe
        d.chord((cx - fw * 1.1, cy - fh * 1.25, cx + fw * 1.1, cy + fh * .3), 180, 360, fill=top)
        d.rectangle((cx - fw * .9, cy - fh * .75, cx + fw * .9, cy - fh * .45), fill=top)
    elif style == 2:
        # Curly: many small curls
        for _ in range(70):
            a = rng.uniform(math.pi * .95, math.pi * 2.05)
            r = rng.uniform(.85, 1.15)
            x, y = cx + math.cos(a) * fw * r, cy - fh * .25 + math.sin(a) * fh * r * 1.05
            rr = fw * rng.uniform(.16, .26)
            d.ellipse((x - rr, y - rr, x + rr, y + rr), fill=ga.shade(hair, rng.uniform(.85, 1.1)))
    elif style == 3:
        # A cap, peak forward
        cap = SHIRTS[(seed + 2) % len(SHIRTS)]
        d.chord((cx - fw * 1.08, cy - fh * 1.3, cx + fw * 1.08, cy + fh * .1), 180, 360, fill=cap + (255,))
        d.ellipse((cx - fw * 1.15, cy - fh * .62, cx + fw * 1.15, cy - fh * .32), fill=ga.shade(cap, .7))
        d.rectangle((cx - fw * 1.0, cy - fh * .42, cx + fw * 1.0, cy - fh * .3), fill=ga.shade(hair, .9))
    elif style == 4:
        # Pigtails tied at the sides
        d.chord((cx - fw * 1.08, cy - fh * 1.22, cx + fw * 1.08, cy + fh * .1), 180, 360, fill=top)
        for sx in (-1, 1):
            d.ellipse((cx + sx * fw * 1.35 - fw * .35, cy - fh * .1, cx + sx * fw * 1.35 + fw * .35, cy + fh * .9), fill=top)
            d.ellipse((cx + sx * fw * 1.12 - fw * .1, cy - fh * .25, cx + sx * fw * 1.12 + fw * .1, cy - fh * .05), fill=(200, 40, 50, 255))
    else:
        # Bowl cut
        d.chord((cx - fw * 1.1, cy - fh * 1.25, cx + fw * 1.1, cy + fh * .55), 180, 360, fill=top)
        d.rectangle((cx - fw * 1.08, cy - fh * .38, cx + fw * 1.08, cy - fh * .22), fill=top)
    # The eyes: whites, irises, a glint; brows
    ey = cy - fh * .05
    for sx in (-1, 1):
        ex = cx + sx * fw * .4
        d.ellipse((ex - fw * .17, ey - fh * .08, ex + fw * .17, ey + fh * .08), fill=(245, 245, 240, 255))
        d.ellipse((ex - fw * .09, ey - fh * .08, ex + fw * .09, ey + fh * .08), fill=(70, 60, 50, 255))
        d.ellipse((ex - fw * .03, ey - fh * .05, ex + fw * .02, ey - fh * .01), fill=(255, 255, 255, 255))
        d.line((ex - fw * .2, ey - fh * .2 - sx * fh * .0, ex + fw * .2, ey - fh * .23), fill=ga.shade(hair, .8), width=int(fw * .07))
    if rng.random() < .3:
        # Glasses
        for sx in (-1, 1):
            ex = cx + sx * fw * .4
            d.ellipse((ex - fw * .27, ey - fh * .17, ex + fw * .27, ey + fh * .17), outline=(40, 40, 44, 255), width=int(fw * .05))
        d.line((cx - fw * .13, ey, cx + fw * .13, ey), fill=(40, 40, 44, 255), width=int(fw * .05))
    if rng.random() < .35:
        # Freckles
        for _ in range(14):
            x = cx + rng.uniform(-fw * .7, fw * .7)
            y = cy + rng.uniform(fh * .08, fh * .3)
            d.ellipse((x - 3, y - 3, x + 3, y + 3), fill=ga.shade(skin, .7))
    # Nose, mouth: a shy smile
    d.line((cx, cy + fh * .1, cx - fw * .06, cy + fh * .3), fill=ga.shade(skin, .78), width=int(fw * .06))
    d.arc((cx - fw * .3, cy + fh * .3, cx + fw * .3, cy + fh * .6), 20, 160, fill=(150, 70, 70, 255), width=int(fw * .07))
    img = img.resize((w, h), Image.LANCZOS)
    return img.filter(ImageFilter.GaussianBlur(.35))


def photocopy(photo, contrast=1.5, tone=(20, 18, 16), paper=(250, 248, 240)):
    """A photo as a photocopier gives it: grey, harsh, the darks blotchy, a little grain."""
    g = np.asarray(photo.convert("L"), float) / 255
    g = np.clip((g - .5) * contrast + .55, 0, 1)
    rng = np.random.default_rng(int(g.sum()) % 1000)
    g = np.clip(g + rng.normal(0, .05, g.shape), 0, 1)
    out = np.array(tone)[None, None, :] * (1 - g[..., None]) + np.array(paper)[None, None, :] * g[..., None]
    return Image.fromarray(np.dstack([out, np.full(g.shape, 255)]).astype(np.uint8), "RGBA")


# ------------------------------------------------------------------------------------------------ the posters

CHILDREN = [
    ("LUCAS MOREL", "9 ans", "garçon", "le 14 octobre, près du parc Elm"),
    ("EMMA GIRARD", "7 ans", "fille", "le 3 novembre, rue des Saules"),
    ("NOAH LEFÈVRE", "11 ans", "garçon", "le 21 octobre, au bord du canal"),
    ("CHLOÉ MARTIN", "8 ans", "fille", "le 29 septembre, à la sortie de l'école"),
    ("HUGO BERNARD", "10 ans", "garçon", "le 9 novembre, près du vieux cinéma"),
    ("LÉA DUBOIS", "12 ans", "fille", "le 17 octobre, à la bibliothèque"),
    ("JULES CARON", "8 ans", "garçon", "le 2 novembre, avec son vélo rouge"),
    ("MANON PETIT", "9 ans", "fille", "le 25 octobre, au parc des Peupliers"),
    ("THÉO ROUX", "13 ans", "garçon", "le 11 octobre, près des égouts de la 4e rue"),
    ("INÈS FABRE", "10 ans", "fille", "le 6 novembre, au marché"),
    ("LOUIS GARNIER", "7 ans", "garçon", "le 19 octobre, devant le milk-bar"),
    ("CAMILLE LEROY", "11 ans", "fille", "le 31 octobre, le soir d'Halloween"),
]
# Thirty years ago: the cycle before
OLD = ("PATRICK VIDAL", "10 ans", "garçon", "le 12 octobre, près de la voie ferrée")


def poster(child, seed, w=300, h=420, paper=(250, 248, 240), ink=(18, 16, 14), red=False, tabs=True):
    """One poster, upright: DISPARU(E) across the top, the photo, the name and what the child looked like, when they
    were last seen, a number to call; and the strips at the bottom to tear off, some already gone."""
    name, age, kind, seen = child
    rng = np.random.default_rng(seed)
    img = Image.new("RGBA", (w, h), paper + (255,))
    # The paper's grain
    a = np.asarray(img, float)
    a[..., :3] += (kit.tnoise(w, h, (9, 12), seed)[..., None] - .5) * 10 + rng.normal(0, 2.5, (h, w, 1))
    img = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")
    d = ImageDraw.Draw(img)
    head = "DISPARUE" if kind == "fille" else "DISPARU"
    hc = (176, 24, 22) if red else ink
    f = font(BOLD, h * .135)
    d.text((w / 2, h * .085), head, font=f, fill=hc + (255,), anchor="mm")
    d.text((w / 2, h * .175), "AVEZ-VOUS VU CET ENFANT ?", font=font(BOLD, h * .043), fill=ink + (255,), anchor="mm")
    # The photo
    pw, ph = int(w * .54), int(w * .54 * 1.25)
    photo = photocopy(portrait(seed, pw, ph), paper=paper, tone=ink)
    px, py = (w - pw) // 2, int(h * .21)
    img.alpha_composite(photo, (px, py))
    d.rectangle((px - 1, py - 1, px + pw, py + ph), outline=ink + (255,), width=2)
    y = py + ph + h * .045
    d.text((w / 2, y), name, font=font(BOLD, h * .062), fill=ink + (255,), anchor="mm")
    y += h * .055
    d.text((w / 2, y), f"{age} · vu(e) pour la dernière fois", font=font(BOLD, h * .034), fill=ink + (255,), anchor="mm")
    y += h * .038
    d.text((w / 2, y), seen, font=font(BOLD, h * .034), fill=ink + (255,), anchor="mm")
    y += h * .05
    number = f"555-{1000 + (seed * 37) % 9000:04d}"
    d.text((w / 2, y), f"APPELEZ LE {number}", font=font(BOLD, h * .046), fill=hc + (255,), anchor="mm")
    if tabs:
        # The strips to tear off, their phone number sideways; some already taken
        n = 8
        tw = w / n
        top = h * .912
        for i in range(n):
            x0 = i * tw
            if rng.random() < .35:
                # Torn off: a ragged stub left
                stub = Image.new("RGBA", (int(tw), int(h - top)), (0, 0, 0, 0))
                img.paste(stub, (int(x0), int(top + 9)))
                ys = [top + 3 + rng.uniform(0, 5) for _ in range(5)]
                for k in range(4):
                    d.polygon([(x0 + k * tw / 4, ys[k]), (x0 + (k + 1) * tw / 4, ys[k + 1]), (x0 + (k + 1) * tw / 4, h), (x0 + k * tw / 4, h)],
                              fill=(0, 0, 0, 0))
                continue
            d.line((x0, top, x0, h), fill=ga.shade(paper, .75), width=1)
            t = Image.new("RGBA", (int(h - top), int(tw)), (0, 0, 0, 0))
            fs = tw * .55
            while fs > 6 and font(BOLD, fs).getlength(number) > (h - top) - 6:
                fs -= 1
            ImageDraw.Draw(t).text(((h - top) / 2, tw / 2), number, font=font(BOLD, fs), fill=ink + (255,), anchor="mm")
            img.alpha_composite(t.rotate(90, expand=True), (int(x0), int(top)))
        d.line((0, top, w, top), fill=ga.shade(paper, .8), width=1)
    return img


def aged(img, seed, yellow=0.0, stains=0, fade=0.0, rain=0.0, sun=0.0):
    """The paper growing old: yellowing, brown stains, the ink fading, rain streaks running down, the sun's bleaching."""
    rng = np.random.default_rng(seed)
    a = np.asarray(img, float).copy()
    h, w = a.shape[:2]
    rgb = a[..., :3]
    if fade:
        rgb = 255 - (255 - rgb) * (1 - fade)
    if yellow:
        tint = np.array([240, 214, 160])
        rgb = rgb * (1 - yellow) + rgb * tint / 255 * yellow
    if sun:
        # Sepia, washed out
        lum = rgb.mean(axis=2, keepdims=True)
        sep = np.concatenate([lum * 1.0 + 30, lum * .9 + 22, lum * .7 + 10], axis=2)
        rgb = rgb * (1 - sun) + sep * sun
    for _ in range(stains):
        cx, cy, r = rng.uniform(0, w), rng.uniform(0, h), rng.uniform(w * .06, w * .2)
        ys, xs = np.mgrid[0:h, 0:w]
        dd = np.sqrt((xs - cx) ** 2 + (ys - cy) ** 2) / r
        ring = np.clip(1 - np.abs(dd - .95) * 6, 0, 1) * .35 + np.clip(1 - dd, 0, 1) * .12
        rgb = rgb * (1 - ring[..., None] * np.array([.1, .3, .6]))
    if rain:
        # Streaks down from the top, the ink running in them
        for _ in range(int(14 * rain)):
            x = rng.uniform(0, w)
            width = rng.uniform(2, 7)
            length = rng.uniform(h * .3, h)
            ys, xs = np.mgrid[0:h, 0:w]
            m = np.clip(1 - np.abs(xs - x - np.sin(ys / 30 + x) * 3) / width, 0, 1) * (ys < length) * np.clip(1 - ys / max(1, length), 0, 1) ** .3
            rgb = rgb * (1 - m[..., None] * .18) + m[..., None] * np.array([90, 84, 70]) * .1
        a[..., :3] = rgb
        img = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")
        img = Image.fromarray(np.asarray(img), "RGBA")
        blurred = img.filter(ImageFilter.GaussianBlur(1.2))
        mask = Image.fromarray((np.clip(rng.random((h, w)) * 0 + .5, 0, 1) * 255).astype(np.uint8))
        img = Image.composite(blurred, img, mask)
        return img
    a[..., :3] = rgb
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def torn(img, seed, corner=None, half=False):
    """Paper torn: a corner ripped away (ragged), or the whole lower part gone but a ragged band."""
    rng = np.random.default_rng(seed)
    w, h = img.size
    mask = Image.new("L", (w, h), 255)
    d = ImageDraw.Draw(mask)
    if half:
        y0 = h * .5
        pts = [(0, h), (0, y0 + rng.uniform(-20, 30))]
        x = 0
        while x < w:
            x += rng.uniform(8, 22)
            pts.append((min(w, x), y0 + rng.uniform(-35, 45) + x / w * 30))
        pts += [(w, h)]
        d.polygon(pts, fill=0)
    if corner:
        cx = w if "e" in corner else 0
        cy = 0 if "n" in corner else h
        r = rng.uniform(.28, .4) * w
        pts = [(cx, cy)]
        steps = 9
        for k in range(steps + 1):
            t = k / steps
            ang = t * math.pi / 2
            rr = r * rng.uniform(.75, 1.1)
            x = cx + (-1 if cx else 1) * rr * math.cos(ang)
            y = cy + (1 if cy == 0 else -1) * rr * math.sin(ang)
            pts.append((x, y))
        d.polygon(pts, fill=0)
    out = img.copy()
    out.putalpha(Image.fromarray(np.minimum(np.asarray(img)[..., 3], np.asarray(mask))))
    # The torn edge shows the paper's white fibres
    edge = mask.filter(ImageFilter.MaxFilter(5))
    a = np.asarray(out).copy()
    fib = (np.asarray(edge) > 0) & (np.asarray(mask) == 0)
    a[fib] = [238, 234, 222, 255]
    return Image.fromarray(a, "RGBA")


def tape(img, seed, corners=("nw", "ne")):
    """Strips of yellowed tape over the corners."""
    rng = np.random.default_rng(seed + 5)
    w, h = img.size
    for c in corners:
        tw, th = w * .26, w * .085
        strip = Image.new("RGBA", (int(tw), int(th)), (236, 226, 190, 150))
        sd = ImageDraw.Draw(strip)
        for x in range(0, int(tw), 3):
            sd.line((x, 0, x, th), fill=(225, 214, 176, 150 + int(rng.integers(0, 40))))
        ang = (35 if c in ("nw", "se") else -35) + rng.uniform(-8, 8)
        s = strip.rotate(ang, expand=True, resample=Image.BICUBIC)
        x = 0 if "w" in c else w - s.width
        y = 0 if "n" in c else h - s.height
        x += (-s.width * .22) if "w" in c else s.width * .22
        y += (-s.height * .25) if "n" in c else s.height * .25
        img.alpha_composite(s, (int(max(0, x)), int(max(0, y))))
    return img


def solid(img):
    """Edges either there or not (blocks drawn cut out): alpha to 0 or 255."""
    a = np.asarray(img).copy()
    a[..., 3] = np.where(a[..., 3] >= 128, 255, 0)
    return Image.fromarray(a, "RGBA")


def single(kind, seed, child):
    p = poster(child, seed, red=kind in (1, 4))
    if kind == 0:
        p = tape(p, seed, ("nw", "ne", "sw", "se"))
    elif kind == 1:
        p = tape(aged(p, seed, yellow=.45, stains=2), seed, ("nw", "ne"))
    elif kind == 2:
        p = tape(torn(aged(p, seed, yellow=.2, stains=1), seed, corner="se"), seed, ("nw", "ne"))
    elif kind == 3:
        p = tape(aged(p, seed, yellow=.15, rain=1.0), seed, ("nw", "ne"))
    elif kind == 4:
        p = tape(aged(p, seed, fade=.4, sun=.85, stains=3), seed, ("nw",))
    else:
        p = tape(torn(aged(p, seed, yellow=.3, stains=1), seed, half=True), seed, ("nw", "ne"))
    return p


def place(canvas, p, x, y, scale, angle, wrap=False):
    """Sticks poster {p} on {canvas} with its top-left near (x, y), turned by {angle}; round the edges too if {wrap}."""
    q = p.resize((max(1, int(p.width * scale)), max(1, int(p.height * scale))), Image.LANCZOS)
    q = q.rotate(angle, expand=True, resample=Image.BICUBIC)
    W, H = canvas.size
    offs = [(0, 0)]
    if wrap:
        offs = [(dx, dy) for dx in (-W, 0, W) for dy in (-H, 0, H)]
    for dx, dy in offs:
        paste_clip(canvas, q, int(x + dx), int(y + dy))


def paste_clip(canvas, q, x, y):
    W, H = canvas.size
    x0, y0 = max(0, x), max(0, y)
    x1, y1 = min(W, x + q.width), min(H, y + q.height)
    if x1 <= x0 or y1 <= y0:
        return
    canvas.alpha_composite(q.crop((x0 - x, y0 - y, x1 - x, y1 - y)), (x0, y0))


def wall_backing(size, seed):
    """Under a covered wall: older posters' scraps, glue, bits of brick showing (no hole: the block is all paper)."""
    n = kit.tnoise(size, size, (6, 6), seed)
    base = np.array([214, 206, 190]) * (.82 + .25 * n[..., None])
    img = Image.fromarray(np.dstack([np.clip(base, 0, 255), np.full((size, size), 255)]).astype(np.uint8), "RGBA")
    return img


def poster_textures():
    out = {}
    # One child each, six kinds of paper (the fifth is thirty years old)
    for kind in range(6):
        child = OLD if kind == 4 else CHILDREN[kind]
        c = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
        p = single(kind, 11 + kind, child)
        place(c, p, 128 - p.width * .55 / 2, 128 - p.height * .55 / 2 + 2, .55, [1.5, -2, 3, -1, 2.5, -3][kind])
        out[f"affiche_disparu_{kind + 1}"] = c
    # Two side by side (two layouts), three, four
    c = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    for i, (x, ang) in enumerate([(22, 2), (262, -2.5)]):
        place(c, single([0, 1][i], 30 + i, CHILDREN[6 + i]), x, 70, .76, ang)
    out["affiches_disparus_2_a"] = c
    c = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    place(c, single(3, 40, CHILDREN[8]), 30, 40, .72, -4)
    place(c, single(2, 41, CHILDREN[9]), 250, 120, .72, 5)
    out["affiches_disparus_2_b"] = c
    c = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    for i, (x, y, ang) in enumerate([(14, 20, 3), (178, 46, -2), (338, 14, 4)]):
        place(c, single([0, 1, 3][i], 50 + i, CHILDREN[(10 + i) % 12]), x, y, .52, ang)
    out["affiches_disparus_3"] = c
    c = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    for i, (x, y, ang) in enumerate([(40, 6, 1.5), (268, 10, -2), (42, 258, -1), (270, 262, 2.5)]):
        place(c, single([0, 1, 2, 3][i], 60 + i, CHILDREN[i]), x, y, .56, ang)
    out["affiches_disparus_4"] = c
    # Heaps: stuck one over another, every way
    for name, seed, count in (("affiches_disparus_tas_a", 70, 6), ("affiches_disparus_tas_b", 80, 7)):
        rng = np.random.default_rng(seed)
        c = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
        for i in range(count):
            p = single(int(rng.integers(0, 6)), seed + i, CHILDREN[(seed + i) % 12])
            s = rng.uniform(.42, .55)
            place(c, p, rng.uniform(10, 512 - 300 * s - 10), rng.uniform(10, 512 - 420 * s - 10), s, rng.uniform(-14, 14))
        out[name] = c
    # Whole walls: posters everywhere, running over the block's edges into the next (the picture tiles)
    for name, seed in (("affiches_disparus_mur_a", 90), ("affiches_disparus_mur_b", 100), ("affiches_disparus_mur_c", 110)):
        rng = np.random.default_rng(seed)
        c = wall_backing(512, seed)
        for i in range(22):
            p = single(int(rng.integers(0, 6)), seed + i, CHILDREN[(seed + i) % 12])
            s = rng.uniform(.4, .48)
            place(c, p, rng.uniform(0, 512), rng.uniform(0, 512), s, rng.uniform(-10, 10), wrap=True)
        out[name] = c
    return out


def posters():
    for name, img in poster_textures().items():
        kit.save(solid(img), f"textures/block/{name}.png")
        tex = f"eraphones:block/{name}"
        kit.write_json(f"models/block/{name}.json", {
            "render_type": "minecraft:cutout", "ambientocclusion": False,
            "textures": {"t": tex, "particle": tex},
            "elements": [{"from": [0, 0, .05], "to": [16, 16, .3], "faces": {
                "south": {"uv": [0, 0, 16, 16], "texture": "#t"},
                "north": {"uv": [16, 0, 0, 16], "texture": "#t"}}}]})
        kit.write_json(f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": tex}})
        variants = {f"facing={f}": ({"model": tex, "y": y} if y else {"model": tex})
                    for f, y in (("south", 0), ("west", 90), ("north", 180), ("east", 270))}
        kit.write_json(f"blockstates/{name}.json", {"variants": variants})
        ga.write_data(f"loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
            {"type": "minecraft:item", "name": f"eraphones:{name}"}]}]})


# ------------------------------------------------------------------------------------------------ the code box

STEEL = (64, 78, 72)
BRASS_D, BRASS_L = (120, 88, 34), (226, 186, 104)


def painted_steel(tile, k=1.0):
    w, h = tile.size
    vgrad(tile, ga.shade(STEEL, 1.12 * k), ga.shade(STEEL, .86 * k))
    rng = np.random.default_rng(w * 7 + h)
    d = ImageDraw.Draw(tile)
    # Worn: scratches showing the bare metal, rust freckles
    for _ in range(max(2, w * h // 900)):
        x, y = rng.uniform(0, w), rng.uniform(0, h)
        L = rng.uniform(3, 10)
        a = rng.uniform(0, math.pi)
        d.line((x, y, x + math.cos(a) * L, y + math.sin(a) * L), fill=(150, 156, 152, 255), width=1)
    for _ in range(max(1, w * h // 1500)):
        x, y = rng.uniform(0, w), rng.uniform(0, h)
        d.ellipse((x, y, x + 2, y + 2), fill=(120, 70, 40, 255))
    d.rectangle((0, 0, w - 1, h - 1), outline=ga.shade(STEEL, .6), width=1)


def box_front(tile):
    """The front: steel, a brass plate with the four wheels' windows (their digits showing), a keyhole below."""
    painted_steel(tile)
    w, h = tile.size
    d = ImageDraw.Draw(tile)
    px0, py0, px1, py1 = w * .16, h * .14, w * .84, h * .64
    d.rounded_rectangle((px0, py0, px1, py1), radius=3, fill=BRASS_D + (255,))
    d.rounded_rectangle((px0 + 2, py0 + 2, px1 - 2, py1 - 2), radius=2, fill=(196, 156, 78, 255))
    for i in range(4):
        cx = px0 + (px1 - px0) * (i + .5) / 4
        ww = (px1 - px0) / 4 * .62
        d.rectangle((cx - ww / 2, py0 + (py1 - py0) * .18, cx + ww / 2, py0 + (py1 - py0) * .82), fill=(26, 22, 16, 255))
        d.rectangle((cx - ww / 2 + 1, py0 + (py1 - py0) * .24, cx + ww / 2 - 1, py0 + (py1 - py0) * .76), fill=BRASS_L + (255,))
        d.text((cx, (py0 + py1) / 2), "0", font=font(BOLD, (py1 - py0) * .45), fill=(40, 30, 14, 255), anchor="mm")
    # Keyhole escutcheon
    kx, ky = w / 2, h * .8
    d.ellipse((kx - w * .06, ky - h * .1, kx + w * .06, ky + h * .1), fill=BRASS_D + (255,))
    d.ellipse((kx - w * .02, ky - h * .06, kx + w * .02, ky), fill=(20, 16, 12, 255))
    d.polygon([(kx - w * .012, ky - h * .02), (kx + w * .012, ky - h * .02), (kx + w * .02, ky + h * .06), (kx - w * .02, ky + h * .06)],
              fill=(20, 16, 12, 255))


def lid_top(tile):
    painted_steel(tile, 1.05)
    w, h = tile.size
    d = ImageDraw.Draw(tile)
    d.rectangle((w * .08, h * .12, w * .92, h * .88), outline=ga.shade(STEEL, .7), width=2)
    d.text((w / 2, h / 2), "1958", font=font(BOLD, h * .22), fill=ga.shade(STEEL, .62), anchor="mm")


def felt(tile):
    vgrad(tile, (110, 22, 26), (70, 12, 16))


def code_box(open_):
    m = Model("boite_code" + ("_ouverte" if open_ else ""), 1024, 24)
    side = painted_steel
    # The body
    m.box([2.5, 0, 4], [13.5, 5.0, 12], {"south": box_front, "north": side, "east": side, "west": side, "down": (30, 34, 32),
                                         "up": felt if open_ else side})
    # Little feet
    for x, z in ((3.2, 4.7), (12.8, 4.7), (3.2, 11.3), (12.8, 11.3)):
        m.box([x - .45, -.01, z - .45], [x + .45, 0, z + .45], {"down": (30, 30, 30)})
    # The hinge at the back, the lid on it (open: tilted back 45 degrees)
    m.box([3.5, 5.0, 3.6], [12.5, 5.6, 4.2], {"*": ga.facet(BRASS_D, 1.0)})
    rot = {"origin": [8, 5.0, 4.0], "axis": "x", "angle": -45} if open_ else None
    m.box([2.4, 5.0, 3.9], [13.6, 6.4, 12.1], {"up": lid_top, "south": side, "north": side, "east": side, "west": side,
                                               "down": felt if open_ else side}, rotation=rot)
    # The carrying handle on the lid
    for x in (5.8, 10.2):
        m.box([x - .35, 6.4, 7.6], [x + .35, 7.2, 8.4], {"*": ga.facet((170, 172, 176), 1.0)}, rotation=rot)
    m.box([5.45, 7.2, 7.6], [10.55, 7.7, 8.4], {"*": ga.facet((190, 192, 196), 1.05)}, rotation=rot)
    if open_:
        # Inside: dark, a fold of red felt
        m.box([3.2, 4.0, 4.7], [12.8, 4.99, 11.3], {"up": felt})
    return m


def code_box_assets():
    closed, opened = code_box(False), code_box(True)
    for m in (closed, opened):
        kit.save(m.img, f"textures/block/{m.name}.png")
        tex = f"eraphones:block/{m.name}"
        kit.write_json(f"models/block/{m.name}.json", {
            "render_type": "minecraft:cutout", "textures": {"t": tex, "particle": tex},
            "elements": [dict(e, faces={k: dict(v, texture="#t") for k, v in e["faces"].items()}) for e in m.elements],
            "display": ga.BLOCK_DISPLAY})
    kit.write_json("models/item/boite_code.json", {"parent": "eraphones:block/boite_code"})
    variants = {}
    for f, y in (("south", 0), ("west", 90), ("north", 180), ("east", 270)):
        for o in ("false", "true"):
            v = {"model": "eraphones:block/boite_code" + ("_ouverte" if o == "true" else "")}
            if y:
                v["y"] = y
            variants[f"facing={f},ouverte={o}"] = v
    kit.write_json("blockstates/boite_code.json", {"variants": variants})
    ga.write_data("loot_table/blocks/boite_code.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "eraphones:boite_code"}]}]})


def code_screen():
    """The close-up: the box's front plate (472 x 300, drawn at twice the screen's size) and the drum of digits."""
    W, H = 472, 300
    n = kit.tnoise(W, H, (8, 5), 3)
    base = np.array(STEEL) * (.8 + .3 * n[..., None])
    img = Image.fromarray(np.dstack([np.clip(base, 0, 255), np.full((H, W), 255)]).astype(np.uint8), "RGBA")
    rng = np.random.default_rng(4)
    # Brushed lines, scratches, rust (drawn over, blended)
    over = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    o = ImageDraw.Draw(over)
    for y in range(0, H, 2):
        o.line((0, y, W, y), fill=(255, 255, 255, int(rng.integers(0, 12))))
    for _ in range(50):
        x, y = rng.uniform(0, W), rng.uniform(0, H)
        L, a = rng.uniform(6, 30), rng.uniform(0, math.pi)
        o.line((x, y, x + math.cos(a) * L, y + math.sin(a) * L), fill=(170, 176, 172, 70), width=1)
    for _ in range(40):
        x, y, r = rng.uniform(0, W), rng.uniform(0, H), rng.uniform(1, 4)
        o.ellipse((x - r, y - r, x + r, y + r), fill=(126, 72, 38, 110))
    img.alpha_composite(over)
    d = ImageDraw.Draw(img)
    # The bevelled edge
    for i in range(6):
        c = int(40 + i * 12)
        d.rectangle((i, i, W - 1 - i, H - 1 - i), outline=(c, c + 8, c + 4, 255))
    # Rivets in the corners
    for x, y in ((22, 22), (W - 22, 22), (22, H - 22), (W - 22, H - 22)):
        d.ellipse((x - 7, y - 7, x + 7, y + 7), fill=(40, 46, 44, 255))
        d.ellipse((x - 6, y - 6, x + 5, y + 5), fill=(150, 156, 152, 255))
        d.ellipse((x - 3, y - 4, x, y - 1), fill=(220, 224, 222, 255))
    # The brass plate the wheels sit in
    x0, y0, x1, y1 = 36, 50, W - 36, 196
    d.rounded_rectangle((x0 - 3, y0 - 3, x1 + 3, y1 + 3), radius=10, fill=(40, 30, 14, 255))
    plate = Image.new("RGBA", (x1 - x0, y1 - y0))
    vgrad(plate, BRASS_L, BRASS_D)
    pa = np.asarray(plate, float)
    pa[..., :3] += rng.normal(0, 5, pa.shape[:2])[..., None]
    for y in range(pa.shape[0]):
        pa[y, :, :3] += math.sin(y * .9) * 3
    plate = Image.fromarray(np.clip(pa, 0, 255).astype(np.uint8), "RGBA")
    mask = Image.new("L", plate.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, plate.width - 1, plate.height - 1), radius=8, fill=255)
    img.paste(plate, (x0, y0), mask)
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((x0 + 6, y0 + 6, x1 - 6, y1 - 6), radius=6, outline=(120, 86, 30, 255), width=2)
    d.text((W / 2, 30), "SÛRETÉ  ·  N° 1958", font=font(BOLD, 20), fill=(24, 28, 26, 255), anchor="mm")
    d.text((W / 2 + 1, 29), "SÛRETÉ  ·  N° 1958", font=font(BOLD, 20), fill=(120, 132, 126, 255), anchor="mm")
    kit.save(img, "textures/gui/event/boite_code.png")
    # The drum: digits 8, 9, 0 .. 9, 0, 1 down a brass band (each 52 x 56)
    cw, ch = 52, 56
    order = [8, 9] + list(range(10)) + [0, 1]
    strip = Image.new("RGBA", (cw, ch * len(order)), (0, 0, 0, 255))
    xs = np.linspace(-1, 1, cw)
    roll = np.clip(1 - xs ** 2 * .55, 0, 1)
    band = (np.array(BRASS_D)[None, :] + (np.array(BRASS_L) - np.array(BRASS_D))[None, :] * roll[:, None])
    sa = np.zeros((ch * len(order), cw, 4))
    sa[..., :3] = band[None, :, :]
    sa[..., 3] = 255
    strip = Image.fromarray(sa.astype(np.uint8), "RGBA")
    d = ImageDraw.Draw(strip)
    f = font(BOLD, 42)
    for i, digit in enumerate(order):
        cy = i * ch + ch / 2
        d.text((cw / 2 + 1, cy + 1), str(digit), font=f, fill=(250, 220, 150, 255), anchor="mm")
        d.text((cw / 2, cy), str(digit), font=f, fill=(46, 30, 10, 255), anchor="mm")
        d.line((4, i * ch, cw - 4, i * ch), fill=(90, 64, 22, 255), width=1)
    kit.save(strip, "textures/gui/event/boite_code_chiffres.png")


# ------------------------------------------------------------------------------------------------ the star fragment

def fragment_textures():
    # The crystal: near white (the shards take their colours from the drawing), faint streaks inside
    s = 64
    rng = np.random.default_rng(12)
    a = np.zeros((s, s, 4))
    ys, xs = np.mgrid[0:s, 0:s] / s
    a[..., :3] = 230 + 25 * (1 - ys[..., None])
    for _ in range(9):
        x0 = rng.uniform(0, 1)
        m = np.clip(1 - np.abs(xs - x0 - (ys - .5) * rng.uniform(-.3, .3)) * 40, 0, 1)
        a[..., :3] -= m[..., None] * np.array([28, 18, 0])
    a[..., 3] = 255
    kit.save(Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA"), "textures/entity/fragment_etoile.png")
    # The halo (added to what is behind it: black is nothing)
    s = 128
    ys, xs = np.mgrid[0:s, 0:s]
    r = np.sqrt((xs - s / 2 + .5) ** 2 + (ys - s / 2 + .5) ** 2) / (s / 2)
    g = np.clip(np.exp(-r * r * 5.5) - np.exp(-5.5), 0, 1) / (1 - math.exp(-5.5))
    img = np.dstack([g * 255, g * 236, g * 196, np.full((s, s), 255)])
    kit.save(Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGBA"), "textures/entity/fragment_halo.png")
    # The glint: four long rays, four short
    dx, dy = (xs - s / 2 + .5) / (s / 2), (ys - s / 2 + .5) / (s / 2)
    rays = np.exp(-np.abs(dy) * 60) * np.clip(1 - np.abs(dx), 0, 1) ** 2 + np.exp(-np.abs(dx) * 60) * np.clip(1 - np.abs(dy), 0, 1) ** 2
    u, v = (dx + dy) / math.sqrt(2), (dx - dy) / math.sqrt(2)
    rays += .5 * (np.exp(-np.abs(v) * 70) * np.clip(1 - np.abs(u) * 1.8, 0, 1) ** 2 + np.exp(-np.abs(u) * 70) * np.clip(1 - np.abs(v) * 1.8, 0, 1) ** 2)
    rays += np.exp(-(dx * dx + dy * dy) * 60)
    rays = np.clip(rays, 0, 1)
    img = np.dstack([rays * 255, rays * 248, rays * 230, np.full((s, s), 255)])
    kit.save(Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGBA"), "textures/entity/fragment_eclat.png")
    # The item: the shards drawn in their colours, with their glow
    s = 64
    k = 4
    big = Image.new("RGBA", (s * k, s * k), (0, 0, 0, 0))
    gl = np.zeros((s * k, s * k))
    yy, xx = np.mgrid[0:s * k, 0:s * k]
    c = s * k / 2
    rr = np.sqrt((xx - c) ** 2 + (yy - c) ** 2) / c
    gl = np.clip(1 - rr, 0, 1) ** 2.2
    glow = np.dstack([np.full_like(gl, 255), np.full_like(gl, 214), np.full_like(gl, 140), gl * 150])
    big.alpha_composite(Image.fromarray(glow.astype(np.uint8), "RGBA"))
    d = ImageDraw.Draw(big)
    shards = [(-90, .42, .1), (90, .28, .08), (-30, .3, .07), (-150, .3, .07), (25, .26, .06), (155, .25, .06), (-60, .24, .05),
              (-120, .23, .05), (55, .2, .05), (125, .2, .05)]
    for ang, length, thick in shards:
        a = math.radians(ang)
        L, T = length * s * k, thick * s * k
        tip = (c + math.cos(a) * L, c + math.sin(a) * L)
        nx, ny = -math.sin(a), math.cos(a)
        mid = (c + math.cos(a) * L * .35, c + math.sin(a) * L * .35)
        left = (mid[0] + nx * T, mid[1] + ny * T)
        right = (mid[0] - nx * T, mid[1] - ny * T)
        root = (c - math.cos(a) * T * .5, c - math.sin(a) * T * .5)
        d.polygon([root, left, tip], fill=(255, 244, 214, 255))
        d.polygon([root, right, tip], fill=(196, 206, 255, 255))
        d.line([left, tip, right], fill=(255, 255, 255, 255), width=3)
    d.ellipse((c - 14, c - 14, c + 14, c + 14), fill=(255, 255, 240, 255))
    item = big.resize((s, s), Image.LANCZOS)
    kit.save(item, "textures/item/fragment_etoile.png")
    kit.write_json("models/item/fragment_etoile.json", {"parent": "minecraft:item/generated",
                                                       "textures": {"layer0": "eraphones:item/fragment_etoile"}})


def main():
    posters()
    code_box_assets()
    code_screen()
    fragment_textures()


if __name__ == "__main__":
    main()
