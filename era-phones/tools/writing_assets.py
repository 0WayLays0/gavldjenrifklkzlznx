"""The writing that appears on walls (/ecrire): a sheet of letters for each style, drawn from free fonts (Rock Salt and
Permanent Marker, Apache 2.0; Caveat, OFL; in tools/fonts with their licences) and then made into the thing itself:
wet blood written with a finger, its drips hanging under the strokes; chalk, grainy and broken; spray paint with its
soft overspray and a run or two. Each letter sits in a cell 64 wide and 96 tall, the lower third kept for drips, which
the game lets grow. A small file tells the game which letters there are and how far each moves the pen."""
import json
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

import kit

HERE = os.path.dirname(os.path.abspath(__file__))
CHARS = ("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 .,;:!?'\"-()/&+=*#%@_<>"
         "àâäçéèêëîïôöùûüÿœæÀÂÄÇÉÈÊËÎÏÔÖÙÛÜŸŒÆ")
CW, CH, BODY, BASE, PAD, COLS = 64, 96, 64, 50, 8, 16
SS = 3

STYLES = {
    "sang": ("RockSalt.woff", 40),
    "peinture": ("PermanentMarker.woff", 50),
    "craie": ("Caveat-Bold.woff", 60),
    "noir": ("RockSalt.woff", 40),
}


def glyph_mask(font, ch):
    """The letter as a mask the size of a cell (drawn three times larger), its pen advance in cell pixels."""
    m = Image.new("L", (CW * SS, CH * SS), 0)
    d = ImageDraw.Draw(m)
    d.text((PAD * SS, BASE * SS), ch, font=font, fill=255, anchor="ls")
    return m, font.getlength(ch) / SS


def blood(mask, rng, drips_ok=True):
    """Wet blood: deep red, darker where thick, a glint along the top of the strokes; drips running down from the
    lowest points into the cell's lower third, each ending in a bead; a few specks."""
    # A finger is wider than a pen: the strokes thickened, their edges ragged
    a = np.asarray(mask.filter(ImageFilter.MaxFilter(SS * 2 + 1)).filter(ImageFilter.GaussianBlur(SS * .8)), float) / 255
    h, w = a.shape
    noise = np.asarray(Image.fromarray((rng.random((h // 5, w // 5)) * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC), float) / 255
    a = np.clip((a - .3 + (noise - .5) * .3) * 3, 0, 1)
    # Drips: from the lowest inked points of a few columns
    drips = Image.new("L", (w, h), 0)
    dd = ImageDraw.Draw(drips)
    cols = [x for x in range(0, w, 3) if a[:BODY * SS, x].max() > .5] if drips_ok else []
    for _ in range(rng.choice([0, 1, 1, 2]) if cols else 0):
        x = cols[rng.integers(0, len(cols))]
        ys = np.nonzero(a[:BODY * SS, x] > .5)[0]
        y0 = ys.max() - SS
        length = rng.uniform(.15, 1) ** 1.5 * (CH * SS - y0 - 8 * SS)
        width = rng.uniform(2.6, 4.4) * SS
        dd.line((x, y0, x, y0 + length), fill=255, width=int(width))
        r = width * .9
        dd.ellipse((x - r, y0 + length - r * .6, x + r, y0 + length + r * 1.2), fill=255)
    for _ in range(rng.integers(0, 3)):
        x, y = rng.uniform(0, w), rng.uniform(0, BODY * SS)
        r = rng.uniform(.8, 1.6) * SS
        dd.ellipse((x - r, y - r, x + r, y + r), fill=200)
    a = np.maximum(a, np.asarray(drips.filter(ImageFilter.GaussianBlur(SS * .6)), float) / 255)
    a = np.clip((a - .3) * 2.5, 0, 1)
    # Colour: thick blood darker, a wet glint up and to the left
    thick = np.asarray(Image.fromarray((a * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(SS * 2)), float) / 255
    glint = np.clip(a - np.roll(np.roll(a, SS * 2, 0), SS * 2, 1), 0, 1)
    r = 104 - thick * 46 + glint * 70
    g = 2 + glint * 16
    b = 6 + glint * 16
    return np.dstack([r, g, b, a * 245])


def chalk(mask, rng, drips_ok=True):
    """Chalk: off-white, broken by the wall's grain, a little dust round the strokes."""
    a = np.asarray(mask.filter(ImageFilter.GaussianBlur(SS * .5)), float) / 255
    h, w = a.shape
    grain = rng.random((h, w))
    streak = np.asarray(Image.fromarray((rng.random((h // 2, w // 12)) * 255).astype(np.uint8)).resize((w, h), Image.BILINEAR), float) / 255
    core = np.clip(a * 1.4 - (grain * .55 + streak * .35), 0, 1)
    dust = np.asarray(mask.filter(ImageFilter.GaussianBlur(SS * 3)), float) / 255 * .18 * (grain > .6)
    alpha = np.clip(core + dust, 0, 1)
    v = 236 - grain * 22
    return np.dstack([v, v, v - 8, alpha * 235])


def paint(mask, rng, drips_ok=True):
    """Spray paint: a dense black core, the soft overspray round it, now and then a run of paint down the wall."""
    a = np.asarray(mask, float) / 255
    h, w = a.shape
    halo = np.asarray(mask.filter(ImageFilter.GaussianBlur(SS * 2.6)), float) / 255
    speck = (rng.random((h, w)) < halo * .25) * .5
    runs = Image.new("L", (w, h), 0)
    rd = ImageDraw.Draw(runs)
    cols = [x for x in range(0, w, 3) if a[:BODY * SS, x].max() > .5] if drips_ok else []
    if cols and rng.random() < .22:
        x = cols[rng.integers(0, len(cols))]
        y0 = np.nonzero(a[:BODY * SS, x] > .5)[0].max() - SS
        length = rng.uniform(.2, 1) ** 1.4 * (CH * SS - y0 - 6 * SS)
        rd.line((x, y0, x, y0 + length), fill=255, width=int(SS * 3))
        rd.ellipse((x - SS * 2.2, y0 + length - SS * 1.4, x + SS * 2.2, y0 + length + SS * 2.4), fill=255)
    core = np.maximum(a, np.asarray(runs.filter(ImageFilter.GaussianBlur(SS * .5)), float) / 255)
    alpha = np.clip(np.maximum(core, halo * .45 + speck), 0, 1)
    v = 18 + (1 - core) * 20
    return np.dstack([v, v, v + 4, alpha * 250])


def soot(mask, rng, drips_ok=True):
    """Black written by hand in soot or charcoal: deep black where pressed, smudged grey at the edges, a fingertip's
    smear here and there."""
    a = np.asarray(mask.filter(ImageFilter.MaxFilter(SS + 2 if SS % 2 else SS + 1)), float) / 255
    h, w = a.shape
    grain = np.asarray(Image.fromarray((rng.random((h // 2, w // 2)) * 255).astype(np.uint8)).resize((w, h), Image.BILINEAR), float) / 255
    smudge = np.asarray(mask.filter(ImageFilter.GaussianBlur(SS * 2.2)), float) / 255
    smear = np.roll(smudge, SS * 3, 1) * (rng.random() < .3) * .35
    core = np.clip(a * 1.2 - grain * .25, 0, 1)
    alpha = np.clip(np.maximum(core, smudge * .4 + smear), 0, 1)
    v = 12 + (1 - core) * 40
    return np.dstack([v, v, v, alpha * 250])


PAINT = {"sang": blood, "craie": chalk, "peinture": paint, "noir": soot}


def sheet(style):
    fname, size = STYLES[style]
    font = ImageFont.truetype(os.path.join(HERE, "fonts", fname), size * SS)
    rows = math.ceil(len(CHARS) / COLS)
    img = Image.new("RGBA", (CW * COLS, CH * rows), (0, 0, 0, 0))
    adv = []
    rng = np.random.default_rng(len(style) * 31)
    for i, ch in enumerate(CHARS):
        mask, advance = glyph_mask(font, ch)
        rgba = PAINT[style](mask, rng, ch.isalnum())
        cell = Image.fromarray(np.clip(rgba, 0, 255).astype(np.uint8), "RGBA").resize((CW, CH), Image.LANCZOS)
        img.paste(cell, ((i % COLS) * CW, (i // COLS) * CH))
        adv.append(round(advance + (2 if style != "craie" else 0), 1))
    kit.save(img, f"textures/writing/{style}.png")
    kit.write_json(f"writing/{style}.json", {"chars": CHARS, "cell": [CW, CH], "body": BODY, "baseline": BASE, "pad": PAD,
                                              "cols": COLS, "advance": adv})


def main():
    for style in STYLES:
        sheet(style)


if __name__ == "__main__":
    main()
