"""The street lamp (original design, drawn here): a galvanised grey pole on a round concrete foot, an access door and
the lamp's number on it; at the top an arm reaching over the street and the long "cobra" head at its end, its sodium
glass glowing orange at night (a lit and an unlit model). Three blocks tall: the foot's model holds the pole for two
blocks, the top block's the rest. Its sounds: the buzz of a lamp warming up, the click of one going out, and a whole
street losing its power."""
import numpy as np
from PIL import Image, ImageDraw, ImageFont

import bike_assets as ba
import gear_assets as ga
import kit
from models import Model, vgrad, metal_edge

NAME = "lampadaire"
LIT = {"block_light": 15, "sky_light": 15}
POLE_DARK, POLE_LIGHT = (92, 98, 104), (196, 202, 208)


def galvanised(tile):
    metal_edge(tile, POLE_DARK, POLE_LIGHT)
    w, h = tile.size
    rng = np.random.default_rng(w * 13 + h)
    a = np.asarray(tile, float)
    a[..., :3] += rng.normal(0, 4, (h, w, 1))
    tile.paste(Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA"))


def concrete(tile):
    w, h = tile.size
    rng = np.random.default_rng(w + 3 * h)
    v = 158 + rng.normal(0, 8, (h, w))
    tile.paste(Image.fromarray(np.dstack([v, v - 2, v - 6, np.full((h, w), 255)]).clip(0, 255).astype(np.uint8), "RGBA"))


def door(tile):
    galvanised(tile)
    w, h = tile.size
    d = ImageDraw.Draw(tile)
    d.rectangle((0, 0, w - 1, h - 1), outline=(70, 74, 80, 255), width=1)
    d.ellipse((w * .4, h * .45, w * .6, h * .55), fill=(60, 62, 68, 255))


def number(tile):
    w, h = tile.size
    vgrad(tile, (30, 50, 110), (20, 36, 86))
    d = ImageDraw.Draw(tile)
    font = ImageFont.truetype(ba.kit_font(), max(8, int(h * .62)))
    d.text((w / 2, h / 2), "1984", font=font, fill=(240, 240, 246, 255), anchor="mm")


def glass(on):
    """The head's underside: the sodium lamp's ribbed glass, orange and glowing when lit, a dull amber when not."""
    def p(tile):
        w, h = tile.size
        if on:
            vgrad(tile, (255, 196, 90), (255, 150, 40))
        else:
            vgrad(tile, (150, 120, 80), (110, 86, 56))
        d = ImageDraw.Draw(tile)
        for x in range(2, w, 4):
            d.line((x, 0, x, h), fill=(255, 220, 140, 255) if on else (128, 104, 70, 255), width=1)
        d.rectangle((0, 0, w - 1, h - 1), outline=(70, 74, 80, 255), width=2)
    return p


def lower():
    m = Model(NAME, 1024, 12)
    ga.octo(m, "y", 0, 2.4, 8, 8, 3.2, (150, 148, 142), end_b=ga.disc((160, 158, 152), (120, 118, 112)))
    ga.octo(m, "y", 2.4, 3.2, 8, 8, 1.9, POLE_DARK)
    ga.octo(m, "y", 3.2, 32, 8, 8, 1.25, galvanised)
    m.box([8 - .9, 5.5, 8 + 1.2], [8 + .9, 9.5, 8 + 1.32], {"south": door})
    m.box([8 - 1.0, 18, 8 + 1.2], [8 + 1.0, 19.5, 8 + 1.32], {"south": number})
    return m


def upper(on):
    m = Model(NAME + "_haut" + ("_allume" if on else ""), 1024, 16)
    ga.octo(m, "y", 0, 7.4, 8, 8, 1.15, galvanised, end_b=ga.disc(POLE_LIGHT, POLE_DARK))
    # The arm: a rise, then out over the street
    ba.tube(m, (6.6, 0.0), (10.2, 3.6), .55, galvanised)
    m.box([8 - .55, 9.7, 8 + 3.0], [8 + .55, 10.8, 8 + 18.5], {"*": galvanised})
    # The cobra head: housing above, the glowing glass under it, a photocell on top
    m.box([8 - 2.6, 10.0, 8 + 16.0], [8 + 2.6, 11.9, 8 + 23.5], {"*": galvanised, "up": ga.facet((170, 176, 182), 1.1)})
    m.box([8 - 2.1, 9.3, 8 + 16.6], [8 + 2.1, 10.0, 8 + 22.9], {"down": glass(on), "east": glass(on), "west": glass(on), "north": glass(on),
                                                                 "south": glass(on)})
    if on:
        m.elements[-1]["neoforge_data"] = LIT
        m.elements[-1]["shade"] = False
    ga.octo(m, "y", 11.9, 12.5, 8, 8 + 18.5, .55, (40, 40, 44))
    return m


def write(m, render_type="minecraft:cutout"):
    kit.save(m.img, f"textures/block/{m.name}.png")
    tex = f"eraphones:block/{m.name}"
    kit.write_json(f"models/block/{m.name}.json", {
        "render_type": render_type, "ambientocclusion": False,
        "textures": {"t": tex, "particle": tex},
        "elements": [dict(e, faces={k: dict(v, texture="#t") for k, v in e["faces"].items()}) for e in m.elements]})


def lamp():
    low, up_off, up_on = lower(), upper(False), upper(True)
    for m in (low, up_off, up_on):
        write(m)
    kit.write_json(f"models/block/{NAME}_milieu.json", {"textures": {"particle": f"eraphones:block/{NAME}"}, "elements": []})
    variants = {}
    for f, y in (("south", 0), ("west", 90), ("north", 180), ("east", 270)):
        for lit in ("false", "true"):
            for part, model in ((0, NAME), (1, NAME + "_milieu"), (2, NAME + "_haut" + ("_allume" if lit == "true" else ""))):
                v = {"model": f"eraphones:block/{model}"}
                if y:
                    v["y"] = y
                variants[f"facing={f},lit={lit},part={part}"] = v
    kit.write_json(f"blockstates/{NAME}.json", {"variants": variants})
    # The item: the whole lamp, small
    els = [dict(e, faces={k: dict(v, texture="#a") for k, v in e["faces"].items()}) for e in low.elements]
    els += ba.moved(up_on.elements, "#b", 0, 32, 0)
    scale = 16 / 48
    els = scaled(els, scale)
    kit.write_json(f"models/item/{NAME}.json", {
        "textures": {"a": f"eraphones:block/{NAME}", "b": f"eraphones:block/{NAME}_haut_allume", "particle": f"eraphones:block/{NAME}"},
        "elements": els, "display": {"gui": {"rotation": [20, 225, 0], "translation": [0, 0, 0], "scale": [.9, .9, .9]},
                                     "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [.5, .5, .5]},
                                     "thirdperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 1, 0], "scale": [.5, .5, .5]},
                                     "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2, 0], "scale": [.5, .5, .5]}}})
    ga.write_data(f"loot_table/blocks/{NAME}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": f"eraphones:{NAME}"}], "conditions": [
        {"condition": "minecraft:survives_explosion"},
        {"condition": "minecraft:block_state_property", "block": f"eraphones:{NAME}", "properties": {"part": "0"}}]}]})


def scaled(els, k):
    """The elements brought down by {k} round the block's centre (x, z) and its floor (y)."""
    out = []
    for e in els:
        e = dict(e)

        def s(p):
            return [round(8 + (p[0] - 8) * k, 4), round(p[1] * k, 4), round(8 + (p[2] - 8) * k, 4)]
        e["from"], e["to"] = s(e["from"]), s(e["to"])
        if "rotation" in e:
            r = dict(e["rotation"])
            r["origin"] = s(r["origin"])
            e["rotation"] = r
        e.pop("neoforge_data", None)
        out.append(e)
    return out


# ------------------------------------------------------------------------------------------------ sounds

def buzz(rate=48000):
    """A sodium lamp striking: a 100 Hz hum with its buzzing overtones, crackles, swelling."""
    rng = np.random.default_rng(8)
    n = int(rate * 1.6)
    t = np.arange(n) / rate
    hum = sum(np.sin(2 * np.pi * 100 * k * t) / k for k in range(1, 9))
    env = np.clip(t / .08, 0, 1) * np.clip((1.6 - t) / .4, 0, 1) * (.6 + .4 * np.abs(np.sin(2 * np.pi * 3.3 * t)))
    crack = np.zeros(n)
    for _ in range(14):
        k = rng.integers(0, n - 300)
        crack[k:k + 300] += rng.normal(0, 1, 300) * np.exp(-np.arange(300) / 40)
    y = hum * env * .5 + crack * .25
    return y / np.abs(y).max() * .8


def click(rate=48000):
    n = int(rate * .5)
    t = np.arange(n) / rate
    rng = np.random.default_rng(9)
    y = rng.normal(0, 1, n) * np.exp(-t / .006) + np.sin(2 * np.pi * 900 * t) * np.exp(-t / .03) * .4
    hum = np.sin(2 * np.pi * 100 * t) * np.exp(-t / .12) * .4
    y = y + hum
    return y / np.abs(y).max() * .8


def power_down(rate=48000):
    """A whole street losing its power: the hum of the wires sinking away, a heavy clunk, a fizz, silence."""
    rng = np.random.default_rng(10)
    n = int(rate * 2.4)
    t = np.arange(n) / rate
    f = 120 * np.clip(1 - t / 1.6, .05, 1) ** 1.5
    phase = 2 * np.pi * np.cumsum(f) / rate
    whine = (np.sin(phase) + .5 * np.sin(2 * phase) + .3 * np.sin(3 * phase)) * np.clip((1.7 - t) / 1.7, 0, 1)
    clunk_t = np.clip(t - 1.55, 0, None)
    clunk = (np.sin(2 * np.pi * 60 * clunk_t) * 1.2 + rng.normal(0, 1, n) * .6) * np.exp(-clunk_t / .08) * (t >= 1.55)
    fizz = ga.band(rng.normal(0, 1, n), 3000, 9000) * np.exp(-clunk_t / .3) * (t >= 1.55) * .3
    y = whine * .4 + clunk + fizz
    return y / np.abs(y).max() * .85


def main():
    lamp()
    ga.shaped(NAME, [" I ", " I ", "SIS"], {"I": "minecraft:iron_ingot", "S": "minecraft:smooth_stone"})


if __name__ == "__main__":
    main()
