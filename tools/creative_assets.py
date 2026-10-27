"""The addon's creative tab: its own background (a night-blue panel framed in gold, a faint arcane circle behind the
items), its scroller (a gold-set gem), and the sprites of the school bar beside it (a framed panel, the school
slots, the arrows). Original art; the item slots and the scroll track sit where the game expects them.
"""
import math

import numpy as np
from PIL import Image, ImageDraw

import gen_assets as g

OUTLINE = (12, 8, 22)
GOLD_HI = (255, 228, 156)
GOLD = (222, 172, 78)
GOLD_LO = (122, 82, 30)
SHADOW = (34, 24, 58)
BODY_TOP = (40, 30, 78)
BODY_BOT = (19, 14, 40)
SLOT_DARK = (7, 5, 15)
SLOT_IN = (17, 13, 34)
SLOT_HI = (82, 66, 138)
VIOLET = (122, 76, 255)
STAR = (236, 232, 255)

# Where the game puts things on a creative tab (the panel is 195 x 136)
W, H = 195, 136
SLOT = 18
GRID_X, GRID_Y = 8, 17
HOTBAR_Y = 111
TRACK = (174, 17, 188, 129)


def _px(img, x, y, c, a=255):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), tuple(c) + (a,))


def _blend(img, x, y, c, a):
    """Paints {c} over the pixel at (x, y) with opacity {a} (0 to 1)."""
    if 0 <= x < img.width and 0 <= y < img.height:
        r, gg, b, al = img.getpixel((x, y))
        img.putpixel((x, y), (int(r + (c[0] - r) * a), int(gg + (c[1] - gg) * a), int(b + (c[2] - b) * a), max(al, int(255 * a))))


def _frame(img, x0, y0, x1, y1, corner=2):
    """The gold frame of a panel from (x0, y0) to (x1, y1) inclusive: a dark outline, a bevelled gold band, a shadow."""
    def inside(x, y, inset):
        # Cut corners, as the game's panels have
        cx = min(x - x0, x1 - x)
        cy = min(y - y0, y1 - y)
        return cx >= inset and cy >= inset and not (cx < corner + inset and cy < corner + inset and cx + cy < corner + 2 * inset)

    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if not inside(x, y, 0):
                img.putpixel((x, y), (0, 0, 0, 0))
            elif not inside(x, y, 1):
                _px(img, x, y, OUTLINE)
            elif not inside(x, y, 2):
                # Light on the top and left, dark on the bottom and right
                top_left = min(x - x0, y - y0) <= min(x1 - x, y1 - y)
                _px(img, x, y, GOLD_HI if top_left else GOLD_LO)
            elif not inside(x, y, 3):
                _px(img, x, y, GOLD)
            elif not inside(x, y, 4):
                _px(img, x, y, SHADOW)


def _body(img, x0, y0, x1, y1, seed):
    """The night-blue body of a panel with a few faint stars."""
    for y in range(y0, y1 + 1):
        t = (y - y0) / max(1, y1 - y0)
        c = g.lerp(BODY_TOP, BODY_BOT, t)
        for x in range(x0, x1 + 1):
            _px(img, x, y, c)
    rnd = np.random.default_rng(seed)
    for _ in range((x1 - x0) * (y1 - y0) // 90):
        x, y = int(rnd.integers(x0, x1 + 1)), int(rnd.integers(y0, y1 + 1))
        _blend(img, x, y, STAR, .12 + rnd.random() * .3)


def _stud(img, x, y):
    """A small gold diamond set on the frame."""
    for dx, dy, c in ((0, -1, GOLD_HI), (-1, 0, GOLD_HI), (0, 0, (255, 246, 214)), (1, 0, GOLD_LO), (0, 1, GOLD_LO)):
        _px(img, x + dx, y + dy, c)


def _gold_rule(img, x0, x1, y, gem_at=None):
    """A thin gold line fading out at both ends, with a gem in it."""
    for x in range(x0, x1 + 1):
        t = (x - x0) / max(1, x1 - x0)
        a = min(1, min(t, 1 - t) * 6)
        _blend(img, x, y, GOLD, .85 * a)
        _blend(img, x, y + 1, GOLD_LO, .5 * a)
    if gem_at is not None:
        for dx, dy in ((0, -2), (-1, -1), (0, -1), (1, -1), (-2, 0), (-1, 0), (1, 0), (2, 0), (-1, 1), (0, 1), (1, 1), (0, 2)):
            _px(img, gem_at + dx, y + dy, GOLD if abs(dx) + abs(dy) == 2 else VIOLET)
        _px(img, gem_at, y, (214, 196, 255))
        _px(img, gem_at - 1, y - 1, (255, 255, 255))


def _slot(img, x, y, size=SLOT):
    """An item slot sunk into the panel: dark on its top and left edges, lit on its bottom and right."""
    for j in range(size):
        for i in range(size):
            if i == 0 or j == 0:
                c = SLOT_DARK
            elif i == size - 1 or j == size - 1:
                c = SLOT_HI
            else:
                # A little lighter toward the middle
                d = math.hypot(i - size / 2, j - size / 2) / (size / 2)
                c = g.lerp((30, 24, 58), SLOT_IN, min(1, d))
            _px(img, x + i, y + j, c)


def _arcane_circle(img, cx, cy, r, strength=1.0):
    """A faint circle of runes behind the items."""
    color = (96, 78, 170)
    for k in range(720):
        a = k / 720 * math.tau
        for rr, op in ((r, .45), (r - 4, .3), (r * .55, .25)):
            _blend(img, int(round(cx + math.cos(a) * rr)), int(round(cy + math.sin(a) * rr)), color, op * strength)
    for k in range(24):
        a = k / 24 * math.tau
        for s in range(3 if k % 2 else 5):
            rr = r - 1 - s
            _blend(img, int(round(cx + math.cos(a) * rr)), int(round(cy + math.sin(a) * rr)), color, .45 * strength)
    # A six-pointed star inside
    pts = [(cx + math.cos(a) * r * .55, cy + math.sin(a) * r * .55) for a in (k / 6 * math.tau - math.pi / 2 for k in range(6))]
    for tri in ((0, 2, 4), (1, 3, 5)):
        for a, b in ((tri[0], tri[1]), (tri[1], tri[2]), (tri[2], tri[0])):
            (x0, y0), (x1, y1) = pts[a], pts[b]
            n = int(math.hypot(x1 - x0, y1 - y0) * 2)
            for s in range(n + 1):
                t = s / n
                _blend(img, int(round(x0 + (x1 - x0) * t)), int(round(y0 + (y1 - y0) * t)), color, .35 * strength)


def tab_background():
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    _body(img, 0, 0, W - 1, H - 1, 31)
    # The title band
    for y in range(4, 16):
        for x in range(4, W - 4):
            _blend(img, x, y, (70, 54, 120), .35 * (1 - (y - 4) / 12))
    _gold_rule(img, 6, 168, 15)
    for row in range(5):
        for col in range(9):
            _slot(img, GRID_X + col * SLOT, GRID_Y + row * SLOT)
    # The circle shows faintly through the slots, behind the items
    _arcane_circle(img, 88, 62, 44, .6)
    _gold_rule(img, 8, 169, 108, 88)
    for col in range(9):
        _slot(img, GRID_X + col * SLOT, HOTBAR_Y)
    # The scroll track
    x0, y0, x1, y1 = TRACK
    for y in range(y0, y1):
        for x in range(x0, x1):
            c = SLOT_DARK if x == x0 or y == y0 else SLOT_HI if x == x1 - 1 or y == y1 - 1 else (13, 10, 27)
            _px(img, x, y, c)
    _frame(img, 0, 0, W - 1, H - 1, corner=2)
    for x, y in ((3, 3), (W - 4, 3), (3, H - 4), (W - 4, H - 4)):
        _stud(img, x, y)
    return img


def scroller():
    """The scroll bar's handle: a violet gem set in gold."""
    img = Image.new("RGBA", (12, 15), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 11, 14), fill=OUTLINE + (255,))
    d.rectangle((1, 1, 10, 13), fill=GOLD + (255,))
    d.line((1, 1, 10, 1), fill=GOLD_HI + (255,))
    d.line((1, 1, 1, 13), fill=GOLD_HI + (255,))
    d.line((10, 2, 10, 13), fill=GOLD_LO + (255,))
    d.line((2, 13, 10, 13), fill=GOLD_LO + (255,))
    d.polygon([(6, 3), (9, 7), (6, 11), (3, 7)], fill=VIOLET + (255,), outline=(60, 30, 140, 255))
    img.putpixel((5, 5), (230, 220, 255, 255))
    img.putpixel((5, 6), (180, 160, 255, 255))
    return img


def side_panel():
    """The school bar's panel, drawn stretched (nine-slice): the same gold frame and night-blue body."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    for y in range(32):
        for x in range(32):
            _px(img, x, y, g.lerp(BODY_TOP, BODY_BOT, .45))
    _frame(img, 0, 0, 31, 31, corner=2)
    for x, y in ((3, 3), (28, 3), (3, 28), (28, 28)):
        _stud(img, x, y)
    return img


def entry(kind):
    """A school's slot in the bar (22 x 22): sunk in the panel; lit violet when hovered; framed in gold when chosen."""
    img = Image.new("RGBA", (22, 22), (0, 0, 0, 0))
    _slot(img, 0, 0, 22)
    if kind == "hover":
        for i in range(22):
            for j in range(22):
                if i in (0, 21) or j in (0, 21):
                    _px(img, i, j, (150, 120, 255))
    elif kind == "selected":
        for i in range(22):
            for j in range(22):
                edge = min(i, j, 21 - i, 21 - j)
                if edge == 0:
                    _px(img, i, j, OUTLINE)
                elif edge == 1:
                    _px(img, i, j, GOLD_HI if i + j < 22 else GOLD_LO)
                elif edge == 2:
                    _px(img, i, j, GOLD)
        for x, y in ((1, 1), (20, 1), (1, 20), (20, 20)):
            _px(img, x, y, (255, 246, 214))
    return img


def arrow(up, lit):
    """A gold chevron (9 x 5) for scrolling the bar."""
    img = Image.new("RGBA", (9, 5), (0, 0, 0, 0))
    c = GOLD_HI if lit else GOLD
    for k in range(5):
        y = 4 - k if up else k
        for x in range(k, 9 - k):
            _px(img, x, y, c if x in (k, 8 - k) or k == 0 else GOLD_LO if not lit else GOLD)
    return img


def main():
    g.save(tab_background(), "textures/gui/creative/tab.png")
    g.save(scroller(), "textures/gui/sprites/creative/scroller.png")
    g.save(side_panel(), "textures/gui/sprites/creative/panel.png")
    g.write_json("textures/gui/sprites/creative/panel.png.mcmeta",
                 {"gui": {"scaling": {"type": "nine_slice", "width": 32, "height": 32, "border": 5}}})
    for kind in ("normal", "hover", "selected"):
        g.save(entry(kind), f"textures/gui/sprites/creative/entry{'' if kind == 'normal' else '_' + kind}.png")
    for up in (True, False):
        for lit in (False, True):
            g.save(arrow(up, lit), f"textures/gui/sprites/creative/arrow_{'up' if up else 'down'}{'_lit' if lit else ''}.png")


if __name__ == "__main__":
    main()
