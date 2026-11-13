"""The money and the papers in 3D (original art, drawn here): coins struck in relief (a sixteen-sided disc, its edge
reeded), dollar bills of no real issue engraved front and back, the identity card, a folded newspaper, a sheet of paper.
All the money shares one texture, so eight things cost the game a single picture. Each is held like what it is."""
import math
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

import kit
from models import Model, round_alpha

SS = 3  # drawn this many times larger, then brought down
LIB = "/usr/share/fonts/truetype/liberation/"
SERIF_B, SERIF, MONO = LIB + "LiberationSerif-Bold.ttf", LIB + "LiberationSerif-Regular.ttf", LIB + "LiberationMono-Bold.ttf"
SERIF_I = LIB + "LiberationSerif-Italic.ttf"
T11 = math.tan(math.radians(11.25))


def font(path, px):
    return ImageFont.truetype(path, max(5, int(px)))


def mix(a, b, k):
    return tuple(int(a[i] * (1 - k) + b[i] * k) for i in range(3))


def rgba(c, a=255):
    return tuple(int(v) for v in c[:3]) + (a,)


def finish(tile, big):
    tile.paste(big.resize(tile.size, Image.LANCZOS))


# ------------------------------------------------------------------------------------------------ a shared texture

class Sheet:
    """One texture several models paint their faces on."""
    def __init__(self, name, size):
        self.name, self.size = name, size
        self.img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        self.sky = np.zeros(size, int)  # how high each column is filled

    def alloc(self, w, h):
        """The lowest place a w x h tile fits (a skyline, bottom-left first), a pixel of air round it."""
        w, h = max(1, int(round(w))), max(1, int(round(h)))
        best = None
        for x in range(0, self.size - w + 1):
            y = int(self.sky[x:x + w + 1].max()) if x + w < self.size else int(self.sky[x:x + w].max())
            if best is None or y < best[1]:
                best = (x, y)
        x, y = best
        assert y + h <= self.size, f"{self.name}: texture full"
        self.sky[x:x + w + 1] = y + h + 1
        return (x, y, w, h)

    def save(self):
        kit.save(self.img, f"textures/item/{self.name}.png")


class Shared(Model):
    def __init__(self, name, sheet, density):
        super().__init__(name, sheet.size, density)
        self.sheet, self.img = sheet, sheet.img

    def _alloc(self, w, h):
        return self.sheet.alloc(w, h)

    def write(self, display):
        tex = f"eraphones:item/{self.sheet.name}"
        kit.write_json(f"models/item/{self.name}.json", {"textures": {"t": tex, "particle": tex}, "elements": self.elements,
                                                         "display": display})


def write_own(m, display):
    """A model with its own texture, written straight as the item's model."""
    kit.save(m.img, f"textures/item/{m.name}.png")
    tex = f"eraphones:item/{m.name}"
    kit.write_json(f"models/item/{m.name}.json", {"textures": {"t": tex, "particle": tex}, "elements": m.elements, "display": display})


# ------------------------------------------------------------------------------------------------ how they are held

def display(gui, fp, tp, ground=.5, fixed=1.0, gui_rot=(0, 0, 0), shown=True):
    """Face on in the inventory; held up before the eyes in first person, its face to them; upright in the hand in
    third person (the raised arm tilts the hand's frame back eighteen degrees, 72 rather than 90 rights it), its face
    turned out to whoever looks at the holder ({shown}: money, a card) or to the holder, reading (papers).
    {fp} and {tp} are (translation, scale)."""
    (ft, fs), (tt, ts) = fp, tp
    third = [72, 180, 0] if shown else [72, 0, 0]
    return {
        "gui": {"rotation": list(gui_rot), "translation": [0, 0, 0], "scale": [gui] * 3},
        "firstperson_righthand": {"rotation": [-6, -24, 4], "translation": ft, "scale": [fs] * 3},
        "firstperson_lefthand": {"rotation": [-6, 24, -4], "translation": ft, "scale": [fs] * 3},
        "thirdperson_righthand": {"rotation": third, "translation": tt, "scale": [ts] * 3},
        "thirdperson_lefthand": {"rotation": third, "translation": tt, "scale": [ts] * 3},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [ground] * 3},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [fixed] * 3},
    }


COIN_HELD = {10: display(2.2, ([-2, 3.5, -2], .62), ([1, 2.5, 2.6], .5), .55, 1.6, (12, -22, 0)),
             25: display(2.2, ([-2, 3.5, -2], .62), ([1, 2.5, 2.8], .5), .55, 1.6, (12, -22, 0))}
BILL_HELD = display(1.1, ([-1, 3, 0], .62), ([1, 2.5, 3.4], .5), .5, 1.0)
CARD_HELD = display(1.5, ([-1, 3, -1], .62), ([1, 2.5, 3], .55), .5, 1.2)
PAPER_HELD = display(1.15, ([-1.5, 2.5, 0], .6), ([1, 2.5, 4.5], .5), .5, 1.0, shown=False)
NEWS_HELD = display(1.15, ([-1.5, 2.5, 0], .6), ([1, 2.5, 4.5], .5), .5, 1.0, shown=False)


# ------------------------------------------------------------------------------------------------ coins

COINS = {10: (2.3, .34, "ONE DIME"), 25: (3.0, .44, "QUARTER DOLLAR")}
SILVER = (196, 199, 206)


def poly16(cx, cy, r):
    """The sixteen-sided outline of in-radius {r}, flat sides facing the axes (as the model's strips make it)."""
    rr = r / math.cos(math.radians(11.25))
    return [(cx + rr * math.cos(math.radians(11.25 + 22.5 * k)), cy + rr * math.sin(math.radians(11.25 + 22.5 * k))) for k in range(16)]


def arc_text(hm, s, f, cx, cy, radius, centre_deg, value, inward=True, spread=1.0):
    """Letters set round a circle, centred on {centre_deg} (0 = up, clockwise), their feet towards the centre."""
    widths = [f.getlength(ch) for ch in s]
    total = sum(widths) * spread
    ang = math.radians(centre_deg) - total / 2 / radius * (1 if inward else -1)
    for ch, w in zip(s, widths):
        step = w * spread / radius
        a = ang + step / 2 * (1 if inward else -1)
        size = int(f.size * 1.6)
        g = Image.new("L", (size, size), 0)
        ImageDraw.Draw(g).text((size / 2, size / 2), ch, font=f, fill=value, anchor="mm")
        g = g.rotate(-math.degrees(a) if inward else -math.degrees(a) + 180, resample=Image.BICUBIC)
        x = cx + math.sin(a) * radius - size / 2
        y = cy - math.cos(a) * radius - size / 2
        hm.paste(Image.fromarray(np.maximum(np.asarray(hm.crop((int(x), int(y), int(x) + size, int(y) + size))), np.asarray(g))),
                 (int(x), int(y)))
        ang += step * (1 if inward else -1)


def relief(h, base, k=7.0):
    """A struck face: {h} the height (0-1), lit from the top left, the raised parts polished brighter."""
    gx = (np.roll(h, -1, 1) - np.roll(h, 1, 1))
    gy = (np.roll(h, -1, 0) - np.roll(h, 1, 0))
    lam = np.clip(.5 - (gx + gy) * k, 0, 1)
    col = np.asarray(base, float)[None, None, :] * (.58 + .42 * h[..., None]) + (lam[..., None] - .5) * 210
    ys, xs = np.mgrid[0:h.shape[0], 0:h.shape[1]] / h.shape[0]
    sheen = np.exp(-(((xs - .3) ** 2 + (ys - .28) ** 2) / .08))
    col += sheen[..., None] * 34
    return np.clip(col, 0, 255)


def coin_face(cents, front):
    def p(tile):
        w, h = tile.size
        W = w * SS
        R = W / 2
        hm = Image.new("L", (W, W), 0)
        d = ImageDraw.Draw(hm)
        outline = poly16(R, R, R - 1)
        d.polygon(outline, fill=150)                      # the raised rim
        d.ellipse((R * .12, R * .12, W - R * .12, W - R * .12), fill=70)  # the field
        for k in range(72):                                # a ring of beads inside the rim
            a = math.radians(k * 5)
            bx, by = R + math.sin(a) * R * .82, R - math.cos(a) * R * .82
            d.ellipse((bx - R * .022, by - R * .022, bx + R * .022, by + R * .022), fill=120)
        if front:
            # A profile of no one in particular, facing left, hair tied back
            d.ellipse((R * .62, R * .48, R * 1.26, R * 1.18), fill=215)               # head
            d.polygon([(R * .64, R * .8), (R * .52, R * .9), (R * .6, R * .95), (R * .62, R * 1.0)], fill=215)  # nose
            d.polygon([(R * .78, R * 1.08), (R * 1.14, R * 1.06), (R * 1.18, R * 1.3), (R * .86, R * 1.3)], fill=205)  # neck
            d.pieslice((R * .5, R * 1.18, R * 1.5, R * 1.9), 180, 360, fill=195)        # shoulders
            for k in range(6):                                                          # hair
                d.arc((R * (.8 + k * .03), R * (.46 + k * .02), R * (1.32 - k * .01), R * (1.1 - k * .02)), 250, 70,
                      fill=245, width=max(1, int(R * .018)))
            d.ellipse((R * 1.16, R * .84, R * 1.34, R * 1.02), fill=235)               # the knot
            f = font(SERIF_B, R * .2)
            arc_text(hm, "LIBERTY", f, R, R, R * .62, -40, 230)
            d.text((R * 1.5, R * 1.36), "1980", font=font(SERIF_B, R * .16), fill=230, anchor="mm")
        else:
            f = font(SERIF_B, R * .15)
            arc_text(hm, "UNITED STATES OF AMERICA", f, R, R, R * .64, 0, 230, spread=.98)
            # A wreath: leaves along two arcs, open at the top
            for side in (-1, 1):
                for k in range(9):
                    a = math.radians(180 + side * (28 + k * 13))
                    lx, ly = R + math.sin(a) * R * .5, R - math.cos(a) * R * .5
                    leaf = Image.new("L", (int(R * .22), int(R * .1)), 0)
                    ImageDraw.Draw(leaf).ellipse((0, 0, leaf.width - 1, leaf.height - 1), fill=210)
                    leaf = leaf.rotate(math.degrees(a) * -1 + side * 50, expand=True, resample=Image.BICUBIC)
                    hm.paste(leaf, (int(lx - leaf.width / 2), int(ly - leaf.height / 2)), leaf)
            d.text((R, R * .95), str(cents), font=font(SERIF_B, R * .52), fill=235, anchor="mm")
            d.text((R, R * 1.38), "CENTS", font=font(SERIF_B, R * .16), fill=230, anchor="mm")
        hm = hm.filter(ImageFilter.GaussianBlur(SS * .7))
        H = np.asarray(hm, float) / 255
        col = relief(H, SILVER)
        mask = Image.new("L", (W, W), 0)
        ImageDraw.Draw(mask).polygon(outline, fill=255)
        img = Image.fromarray(np.dstack([col, np.asarray(mask)]).astype(np.uint8), "RGBA")
        finish(tile, img)
    return p


def reeded(across):
    """A coin's edge: fine ridges running from face to face."""
    def p(tile):
        w, h = tile.size
        a = np.zeros((h, w, 4))
        idx = np.arange(h)[:, None] if across else np.arange(w)[None, :]
        k = (np.sin(idx * 2.2) > 0)
        base = np.asarray(SILVER, float)
        a[..., :3] = np.where(k[..., None], base * .95, base * .62)
        a[..., 3] = 255
        tile.paste(Image.fromarray(a.astype(np.uint8), "RGBA"))
    return p


def coin(sheet, cents, money_id):
    r, t, _ = COINS[cents]
    m = Shared(money_id, sheet, 40)
    c = 8
    z0, z1 = 8 - t / 2, 8 + t / 2
    a = r * T11
    for ang in (0, 22.5, 45, -22.5):
        rot = None if ang == 0 else {"origin": [8, 8, 8], "axis": "z", "angle": ang}
        m.box([c - r, c - a, z0], [c + r, c + a, z1], {"east": reeded(True), "west": reeded(True)}, rotation=rot)
        m.box([c - a, c - r, z0], [c + a, c + r, z1], {"up": reeded(False), "down": reeded(False)}, rotation=rot)
    m.box([c - r, c - r, z1], [c + r, c + r, z1 + .01], {"south": coin_face(cents, True)})
    m.box([c - r, c - r, z0 - .01], [c + r, c + r, z0], {"north": coin_face(cents, False)})
    m.write(COIN_HELD[cents])


# ------------------------------------------------------------------------------------------------ bills

WORDS = {1: "ONE DOLLAR", 5: "FIVE DOLLARS", 10: "TEN DOLLARS", 20: "TWENTY DOLLARS", 50: "FIFTY DOLLARS", 100: "ONE HUNDRED DOLLARS"}


def guilloche(w, h, seed, density=1.0):
    """Two families of fine waves crossing, as banknote engravers turn them: a 0-1 ink mask."""
    ys, xs = np.mgrid[0:h, 0:w].astype(float)
    xs /= h
    ys /= h
    r = random.Random(seed)
    m = np.zeros((h, w))
    for k in range(2):
        f = 34 * density + k * 7
        ph = r.random() * 6
        wave = np.sin(ys * f + np.sin(xs * (5 + k * 2) + ph) * (2.6 + k))
        m = np.maximum(m, np.clip(1 - np.abs(wave) / .22, 0, 1))
    return m


def paper(w, h, tint, seed):
    """Banknote paper: a tinted rag paper with red and blue silk fibres in it."""
    rng = np.random.default_rng(seed)
    base = np.asarray(mix(tint, (238, 238, 224), .62), float)
    img = np.ones((h, w, 3)) * base
    img += rng.normal(0, 3.5, (h, w, 1))
    pic = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    d = ImageDraw.Draw(pic)
    r = random.Random(seed)
    for _ in range(int(w * h / 9000)):
        x, y = r.random() * w, r.random() * h
        a = r.random() * math.pi
        L = r.uniform(.006, .016) * w
        col = (190, 60, 60, 120) if r.random() < .5 else (60, 80, 180, 120)
        d.line((x, y, x + math.cos(a) * L, y + math.sin(a) * L), fill=col, width=max(1, SS // 2))
    return pic


def ink_mask(pic, mask, color, alpha=1.0):
    m = np.clip(np.asarray(mask, float), 0, 1) * alpha
    a = np.asarray(pic, float)
    a[..., :3] = a[..., :3] * (1 - m[..., None]) + np.asarray(color, float)[None, None, :] * m[..., None]
    pic.paste(Image.fromarray(a.astype(np.uint8), "RGBA"))


def frame(pic, W, H, ink, seed):
    """The border: a band of guilloche between engraved rules, rosettes in the corners."""
    band = np.zeros((H, W))
    g = guilloche(W, H, seed)
    m = int(H * .045)
    b = int(H * .13)
    band[m:H - m, m:W - m] = 1
    band[b:H - b, b:W - b] = 0
    ink_mask(pic, band * g, ink, .8)
    d = ImageDraw.Draw(pic)
    lw = max(1, SS)
    d.rectangle((m, m, W - m, H - m), outline=rgba(ink), width=lw)
    d.rectangle((b, b, W - b, H - b), outline=rgba(ink), width=lw)
    d.rectangle((m - SS * 2, m - SS * 2, W - m + SS * 2, H - m + SS * 2), outline=rgba(ink, 140), width=max(1, SS // 2))


def corner_numbers(pic, W, H, value, ink, paper_col):
    d = ImageDraw.Draw(pic)
    f = font(SERIF_B, H * .2)
    for cx, cy in ((H * .16, H * .2), (W - H * .16, H * .2), (H * .16, H * .8), (W - H * .16, H * .8)):
        r = H * .14
        d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=rgba(paper_col), outline=rgba(ink), width=SS)
        for k in range(12):
            a = math.radians(k * 30)
            d.line((cx + math.cos(a) * r * .72, cy + math.sin(a) * r * .72, cx + math.cos(a) * r, cy + math.sin(a) * r), fill=rgba(ink, 150),
                   width=max(1, SS // 2))
        d.text((cx, cy), str(value), font=f if value < 100 else font(SERIF_B, H * .15), fill=rgba(ink), anchor="mm")


def bill_front(value, tint):
    def p(tile):
        w, h = tile.size
        W, H = w * SS, h * SS
        ink = (30, 46, 38)
        pic = paper(W, H, tint, value)
        frame(pic, W, H, ink, value)
        d = ImageDraw.Draw(pic)
        light = mix(tint, (250, 250, 240), .78)
        # The portrait: an oval of fine rings, a bust of no one, engraved in lines
        ox0, oy0, ox1, oy1 = W * .39, H * .17, W * .61, H * .83
        d.ellipse((ox0, oy0, ox1, oy1), fill=rgba(light), outline=rgba(ink), width=SS * 2)
        for k in range(1, 9):
            e = k * H * .006
            d.ellipse((ox0 + e, oy0 + e, ox1 - e, oy1 - e), outline=rgba(ink, 70), width=1)
        sil = Image.new("L", (W, H), 0)
        sd = ImageDraw.Draw(sil)
        cx = (ox0 + ox1) / 2
        sd.ellipse((cx - H * .11, H * .27, cx + H * .11, H * .55), fill=255)
        sd.rectangle((cx - H * .05, H * .5, cx + H * .05, H * .62), fill=255)
        sd.pieslice((cx - H * .25, H * .58, cx + H * .25, H * 1.05), 180, 360, fill=255)
        clip = Image.new("L", (W, H), 0)
        ImageDraw.Draw(clip).ellipse((ox0 + SS * 4, oy0 + SS * 4, ox1 - SS * 4, oy1 - SS * 4), fill=255)
        ys = np.arange(H)[:, None]
        hatch = ((ys // SS) % 2 == 0).astype(float) * np.ones((1, W))
        sm = np.asarray(sil, float) / 255 * np.asarray(clip, float) / 255
        ink_mask(pic, sm * (.55 + .45 * hatch), ink)
        # The seals: a dark one with a star on the left, a toothed green one on the right
        d = ImageDraw.Draw(pic)
        sx, sy, sr = W * .24, H * .52, H * .17
        d.ellipse((sx - sr, sy - sr, sx + sr, sy + sr), fill=rgba(ink), outline=rgba(ink))
        d.ellipse((sx - sr * .78, sy - sr * .78, sx + sr * .78, sy + sr * .78), outline=rgba(light), width=SS)
        star = [(sx + math.sin(math.radians(k * 36)) * sr * (.5 if k % 2 == 0 else .2), sy - math.cos(math.radians(k * 36)) * sr * (.5 if k % 2 == 0 else .2))
                for k in range(10)]
        d.polygon(star, fill=rgba(light))
        green = (40, 118, 72)
        tx, ty, tr = W * .76, H * .6, H * .14
        teeth = [(tx + math.cos(math.radians(k * 7.5)) * tr * (1 if k % 2 == 0 else .88), ty + math.sin(math.radians(k * 7.5)) * tr * (1 if k % 2 == 0 else .88))
                 for k in range(48)]
        d.polygon(teeth, fill=rgba(green))
        d.ellipse((tx - tr * .72, ty - tr * .72, tx + tr * .72, ty + tr * .72), fill=rgba(light), outline=rgba(green), width=SS)
        d.rectangle((tx - tr * .4, ty - tr * .05, tx + tr * .4, ty + tr * .05), fill=rgba(green))
        d.polygon([(tx, ty - tr * .45), (tx - tr * .3, ty - tr * .1), (tx + tr * .3, ty - tr * .1)], fill=rgba(green))
        # Words and numbers
        d.text((W / 2, H * .095), "THE UNITED STATES OF AMERICA", font=font(SERIF_B, H * .085), fill=rgba(ink), anchor="mm")
        d.text((W / 2, H * .905), WORDS[value], font=font(SERIF_B, H * .095), fill=rgba(ink), anchor="mm")
        serial = f"A {40519280 + value * 7331:08d} B"[:12]
        d.text((W * .76, H * .3), serial, font=font(MONO, H * .075), fill=rgba(green), anchor="mm")
        d.text((W * .25, H * .8), serial, font=font(MONO, H * .075), fill=rgba(green), anchor="mm")
        d.text((W * .655, H * .78), "SERIES 1980", font=font(SERIF_I, H * .055), fill=rgba(ink), anchor="lm")
        corner_numbers(pic, W, H, value, ink, light)
        # A soft fold down the middle, the paper handled
        a = np.asarray(pic, float)
        xs = np.arange(W)[None, :] / W
        fold = 1 - .05 * np.exp(-((xs - .5) / .012) ** 2) + .03 * np.exp(-((xs - .515) / .02) ** 2)
        a[..., :3] *= fold[..., None]
        finish(tile, Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA"))
    return p


def bill_back(value, tint):
    def p(tile):
        w, h = tile.size
        W, H = w * SS, h * SS
        ink = (34, 104, 66)
        pic = paper(W, H, tint, value + 99)
        frame(pic, W, H, ink, value + 50)
        d = ImageDraw.Draw(pic)
        light = mix(tint, (250, 250, 240), .78)
        # A building of columns in the oval, a lawn before it
        ox0, oy0, ox1, oy1 = W * .3, H * .16, W * .7, H * .84
        d.ellipse((ox0, oy0, ox1, oy1), fill=rgba(light), outline=rgba(ink), width=SS * 2)
        cx = W / 2
        g = guilloche(W, H, value + 7, 1.6)
        sky = Image.new("L", (W, H), 0)
        ImageDraw.Draw(sky).ellipse((ox0 + SS * 4, oy0 + SS * 4, ox1 - SS * 4, H * .5), fill=255)
        ink_mask(pic, np.asarray(sky, float) / 255 * g * .35, ink)
        d = ImageDraw.Draw(pic)
        bw, by = W * .15, H * .66
        d.rectangle((cx - bw, by - H * .04, cx + bw, by), fill=rgba(ink))                    # steps
        d.rectangle((cx - bw * .9, by - H * .3, cx + bw * .9, by - H * .25), fill=rgba(ink))   # entablature
        d.polygon([(cx - bw * .95, by - H * .3), (cx + bw * .95, by - H * .3), (cx, by - H * .43)], fill=rgba(ink))
        d.polygon([(cx - bw * .7, by - H * .315), (cx + bw * .7, by - H * .315), (cx, by - H * .395)], fill=rgba(light))
        for k in range(8):
            x = cx - bw * .8 + k * bw * 1.6 / 7
            d.rectangle((x - SS * 2, by - H * .25, x + SS * 2, by - H * .04), fill=rgba(ink))
        d.rectangle((cx - bw * 1.3, by, cx + bw * 1.3, by + H * .02), fill=rgba(ink))
        for k in range(6):
            yy = by + H * (.04 + k * .022)
            d.line((cx - bw * (1.4 - k * .1), yy, cx + bw * (1.4 - k * .1), yy), fill=rgba(ink, 150), width=max(1, SS // 2))
        d.text((W / 2, H * .095), "THE UNITED STATES OF AMERICA", font=font(SERIF_B, H * .085), fill=rgba(ink), anchor="mm")
        d.text((W / 2, H * .905), WORDS[value], font=font(SERIF_B, H * .095), fill=rgba(ink), anchor="mm")
        d.text((W * .2, H * .5), str(value), font=font(SERIF_B, H * .32), fill=rgba(ink), anchor="mm")
        d.text((W * .8, H * .5), str(value), font=font(SERIF_B, H * .32), fill=rgba(ink), anchor="mm")
        corner_numbers(pic, W, H, value, ink, light)
        finish(tile, pic)
    return p


def edge_of(color):
    def p(tile):
        w, h = tile.size
        tile.paste(Image.new("RGBA", (w, h), rgba(color)))
    return p


def bill(sheet, value, tint, money_id):
    m = Shared(money_id, sheet, 24)
    x0, x1, y0, y1, z0, z1 = 1.3, 14.7, 5.15, 10.85, 7.95, 8.05
    edge = edge_of(mix(tint, (232, 232, 218), .6))
    m.box([x0, y0, z0], [x1, y1, z1], {"south": bill_front(value, tint), "north": bill_back(value, tint),
                                       "east": edge, "west": edge, "up": edge, "down": edge})
    m.write(BILL_HELD)


# ------------------------------------------------------------------------------------------------ identity card

def card_front(tile):
    w, h = tile.size
    W, H = w * SS, h * SS
    pic = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    a = np.zeros((H, W, 4))
    ys = np.linspace(0, 1, H)[:, None, None]
    a[..., :3] = np.asarray((240, 244, 232), float) * (1 - ys) + np.asarray((206, 222, 210), float) * ys
    a[..., 3] = 255
    pic = Image.fromarray(a.astype(np.uint8), "RGBA")
    ink_mask(pic, guilloche(W, H, 3, .9), (70, 120, 100), .14)
    d = ImageDraw.Draw(pic)
    band = H * .19
    d.rectangle((0, 0, W, band), fill=(36, 74, 124, 255))
    d.line((0, band, W, band), fill=(206, 172, 82, 255), width=SS * 2)
    d.text((W * .05, band / 2), "ID", font=font(SERIF_B, band * .62), fill=(244, 238, 220, 255), anchor="lm")
    for k in range(3):
        d.rectangle((W * (.17 + k * .05), band * .42, W * (.2 + k * .05), band * .58), fill=(206, 172, 82, 255))
    sx, sy, sr = W * .9, band / 2, band * .38
    d.ellipse((sx - sr, sy - sr, sx + sr, sy + sr), fill=(214, 184, 96, 255), outline=(150, 120, 50, 255), width=SS)
    # The photograph
    px0, py0, px1, py1 = W * .05, H * .27, W * .31, H * .86
    d.rectangle((px0, py0, px1, py1), fill=(168, 186, 196, 255), outline=(90, 100, 110, 255), width=SS)
    pcx = (px0 + px1) / 2
    d.ellipse((pcx - W * .062, H * .36, pcx + W * .062, H * .6), fill=(150, 112, 88, 255))
    d.ellipse((pcx - W * .068, H * .33, pcx + W * .068, H * .46), fill=(70, 52, 40, 255))
    d.pieslice((pcx - W * .12, H * .62, pcx + W * .12, H * 1.06), 180, 360, fill=(54, 64, 86, 255))
    d.rectangle((px0, H * .86, px1, py1), fill=(168, 186, 196, 255))
    d.rectangle((px0, py0, px1, py1), outline=(90, 100, 110, 255), width=SS)
    # Printed fields
    lab = font(SERIF_B, H * .055)
    for k, (label, length) in enumerate((("NAME", .5), ("NUMBER", .36), ("BORN", .3), ("HEIGHT", .2))):
        y = H * (.3 + k * .135)
        d.text((W * .36, y), label, font=lab, fill=(80, 100, 96, 255), anchor="lm")
        d.rectangle((W * .36, y + H * .045, W * (.36 + length), y + H * .075), fill=(46, 54, 58, 255))
    # The signature
    pts = []
    for k in range(40):
        t = k / 39
        pts.append((W * (.62 + t * .3), H * (.86 + math.sin(t * 17) * .025 - t * .03)))
    d.line(pts, fill=(30, 40, 110, 255), width=SS)
    d.text((W * .95, H * .3), "1980", font=font(SERIF_B, H * .06), fill=(90, 110, 104, 255), anchor="rm")
    finish(tile, pic)
    round_alpha(tile, int(tile.size[1] * .08))


def card_back(tile):
    w, h = tile.size
    W, H = w * SS, h * SS
    a = np.zeros((H, W, 4))
    a[..., :3] = (226, 234, 224)
    a[..., 3] = 255
    pic = Image.fromarray(a.astype(np.uint8), "RGBA")
    ink_mask(pic, guilloche(W, H, 11, .9), (70, 120, 100), .12)
    d = ImageDraw.Draw(pic)
    for k in range(5):
        d.rectangle((W * .06, H * (.14 + k * .1), W * (.9 - (k % 2) * .25), H * (.16 + k * .1)), fill=(120, 136, 130, 255))
    d.rectangle((W * .05, H * .7, W * .95, H * .92), fill=(250, 250, 244, 255), outline=(150, 160, 156, 255), width=SS)
    m = font(MONO, H * .075)
    d.text((W * .07, H * .76), "ID<<1980<<<<<<<<<<<<<<<<<<", font=m, fill=(60, 60, 60, 255), anchor="lm")
    d.text((W * .07, H * .86), "<<<<<<<<<<<<<<<<<<<<<<<<<<", font=m, fill=(60, 60, 60, 255), anchor="lm")
    finish(tile, pic)
    round_alpha(tile, int(tile.size[1] * .08))


def id_card():
    m = Model("carte_identite", 512, 40)
    m.body(3.7, 5.3, 12.3, 10.7, 7.96, 8.04, .45, card_front, card_back, (150, 166, 160), (226, 234, 228))
    write_own(m, CARD_HELD)


# ------------------------------------------------------------------------------------------------ newspaper, sheet

def greek(d, x, y, width, f, color, r, gap=1.0):
    """A line of print too small to read: words of random lengths in the face given."""
    cx = x
    while cx < x + width:
        n = r.randint(2, 8)
        word = "".join(r.choice("etaoinshrdlucmfwyp") for _ in range(n))
        wl = f.getlength(word)
        if cx + wl > x + width:
            break
        d.text((cx, y), word, font=f, fill=color, anchor="lm")
        cx += wl + f.size * .35 * gap


def news_front(tile):
    w, h = tile.size
    W, H = w * SS, h * SS
    rng = np.random.default_rng(5)
    a = np.ones((H, W, 3)) * np.asarray((236, 228, 206), float) + rng.normal(0, 4, (H, W, 1))
    pic = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    d = ImageDraw.Draw(pic)
    ink = (28, 24, 22, 255)
    d.text((W / 2, H * .075), "The Town Courier", font=font(SERIF_B, H * .072), fill=ink, anchor="mm")
    d.line((W * .05, H * .125, W * .95, H * .125), fill=ink, width=SS)
    d.line((W * .05, H * .135, W * .95, H * .135), fill=ink, width=max(1, SS // 2))
    small = font(SERIF, H * .022)
    d.text((W * .05, H * .15), "EVENING EDITION", font=small, fill=ink, anchor="lm")
    d.text((W * .95, H * .15), "1980  -  25c", font=small, fill=ink, anchor="rm")
    d.line((W * .05, H * .165, W * .95, H * .165), fill=ink, width=max(1, SS // 2))
    r = random.Random(4)
    head = font(SERIF_B, H * .048)
    greek(d, W * .05, H * .2, W * .9, head, ink, r, 1.2)
    greek(d, W * .05, H * .25, W * .7, head, ink, r, 1.2)
    # A photograph in halftone
    px0, py0, px1, py1 = W * .05, H * .29, W * .55, H * .58
    ys, xs = np.mgrid[0:int(py1 - py0), 0:int(px1 - px0)]
    scene = .55 + .35 * np.sin(xs / (px1 - px0) * 3 + 1) * np.cos(ys / (py1 - py0) * 2) - .3 * (ys / (py1 - py0) > .7)
    cell = SS * 3
    dots = Image.new("L", (int(px1 - px0), int(py1 - py0)), 255)
    dd = ImageDraw.Draw(dots)
    for yy in range(0, dots.height, cell):
        for xx in range(0, dots.width, cell):
            v = 1 - scene[min(yy, dots.height - 1), min(xx, dots.width - 1)]
            rr = cell * .55 * max(0, v)
            dd.ellipse((xx + cell / 2 - rr, yy + cell / 2 - rr, xx + cell / 2 + rr, yy + cell / 2 + rr), fill=40)
    pic.paste(Image.merge("RGBA", (dots, dots, dots, Image.new("L", dots.size, 255))), (int(px0), int(py0)))
    d.rectangle((px0, py0, px1, py1), outline=ink, width=SS)
    body = font(SERIF, H * .019)
    for col in range(3):
        x0 = W * (.05 + col * .307)
        top = H * .61 if col < 2 else H * .29
        if col == 2:
            pass
        for k in range(int((H * .95 - top) / (H * .026))):
            greek(d, x0, top + k * H * .026, W * .285, body, (60, 54, 48, 255), r, .8)
    for col in range(1):
        x0 = W * .58
        for k in range(int((H * .58 - H * .29) / (H * .026))):
            greek(d, x0, H * .29 + k * H * .026, W * .37, body, (60, 54, 48, 255), r, .8)
    finish(tile, pic)


def news_back(tile):
    w, h = tile.size
    W, H = w * SS, h * SS
    rng = np.random.default_rng(6)
    a = np.ones((H, W, 3)) * np.asarray((232, 224, 200), float) + rng.normal(0, 4, (H, W, 1))
    pic = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    d = ImageDraw.Draw(pic)
    ink = (28, 24, 22, 255)
    r = random.Random(9)
    body = font(SERIF, H * .019)
    sub = font(SERIF_B, H * .032)
    for col in range(3):
        x0 = W * (.05 + col * .307)
        y = H * .05
        greek(d, x0, y, W * .285, sub, ink, r)
        y += H * .045
        while y < H * .95:
            if r.random() < .05 and y < H * .7:
                d.rectangle((x0, y, x0 + W * .285, y + H * .14), outline=ink, width=SS)
                d.text((x0 + W * .1425, y + H * .07), "SALE", font=font(SERIF_B, H * .05), fill=ink, anchor="mm")
                y += H * .16
                continue
            greek(d, x0, y, W * .285, body, (60, 54, 48, 255), r, .8)
            y += H * .026
    finish(tile, pic)


def stack(across):
    """The edge of the folded pages: thin lines of paper."""
    def p(tile):
        w, h = tile.size
        a = np.zeros((h, w, 4))
        idx = np.arange(h)[:, None] if across else np.arange(w)[None, :]
        k = (idx % 3 == 0)
        a[..., :3] = np.where(k[..., None], (190, 182, 160), (232, 224, 200))
        a[..., 3] = 255
        tile.paste(Image.fromarray(a.astype(np.uint8), "RGBA"))
    return p


def fold(tile):
    w, h = tile.size
    a = np.zeros((h, w, 4))
    ys = np.linspace(-1, 1, h)[:, None, None]
    a[..., :3] = np.asarray((236, 228, 204), float) * (1 - .18 * ys ** 2)
    a[..., 3] = 255
    tile.paste(Image.fromarray(a.astype(np.uint8), "RGBA"))


def newspaper():
    m = Model("journal_papier", 512, 24)
    m.box([3.5, 2, 7.7], [12.5, 14, 8.3], {"south": news_front, "north": news_back, "up": fold, "down": stack(True),
                                           "east": stack(False), "west": stack(False)})
    write_own(m, NEWS_HELD)


def note_front(tile):
    w, h = tile.size
    W, H = w * SS, h * SS
    rng = np.random.default_rng(8)
    a = np.ones((H, W, 3)) * np.asarray((248, 247, 240), float) + rng.normal(0, 2.5, (H, W, 1))
    pic = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    d = ImageDraw.Draw(pic)
    for k in range(4, 34):
        y = H * k / 34
        d.line((0, y, W, y), fill=(150, 176, 214, 255), width=max(1, SS // 2))
    d.line((W * .14, 0, W * .14, H), fill=(214, 90, 90, 255), width=max(1, SS // 2))
    # A few lines in blue ink, a hand that is nobody's
    r = random.Random(12)
    for k in range(5, 17):
        y = H * k / 34 - H * .008
        x = W * .18
        end = W * (.92 - (r.random() * .3 if k % 4 == 3 else 0))
        pts = []
        while x < end:
            t = x / W
            pts.append((x, y + math.sin(t * 160 + k) * H * .006 * r.uniform(.6, 1.4) - (H * .006 if r.random() < .15 else 0)))
            x += SS * 1.5
            if r.random() < .03 and len(pts) > 2:
                d.line(pts, fill=(36, 56, 140, 255), width=SS)
                pts = []
                x += W * .025
        if len(pts) > 1:
            d.line(pts, fill=(36, 56, 140, 255), width=SS)
    finish(tile, pic)


def note_back(tile):
    w, h = tile.size
    W, H = w * SS, h * SS
    a = np.ones((H, W, 4)) * np.asarray((244, 243, 236, 255), float)
    pic = Image.fromarray(a.astype(np.uint8), "RGBA")
    d = ImageDraw.Draw(pic)
    for k in range(4, 34):
        y = H * k / 34
        d.line((0, y, W, y), fill=(196, 208, 226, 255), width=max(1, SS // 2))
    d.line((W * .86, 0, W * .86, H), fill=(230, 170, 170, 255), width=max(1, SS // 2))
    finish(tile, pic)


def sheet():
    m = Model("feuille", 512, 24)
    white = edge_of((236, 234, 226))
    m.box([3.6, 1.8, 7.97], [12.4, 14.2, 8.03], {"south": note_front, "north": note_back, "up": white, "down": white, "east": white,
                                                 "west": white})
    write_own(m, PAPER_HELD)


# ------------------------------------------------------------------------------------------------ all of them

def main():
    newspaper()
    sheet()
