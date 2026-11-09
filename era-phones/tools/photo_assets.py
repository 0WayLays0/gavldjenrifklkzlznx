"""The instant camera and its pictures (original art, drawn here): the camera, charcoal and cream, its lens, flash and
red button; a pack of film; a photograph in its white frame; the white frame the game shows a photograph in; the
shutter's click and the motor pushing the print out."""
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

import gear_assets as ga
import gear_items as gi
import kit
from models import Model, vgrad, round_alpha, text, text_width


def camera():
    m = Model("appareil_photo")
    cream, char = (226, 216, 196), (40, 40, 44)

    def front(tile):
        w, h = tile.size
        vgrad(tile, ga.shade(cream, 1.04), ga.shade(cream, .9))
        d = ImageDraw.Draw(tile)
        # The dark band at the top with the viewfinder and the flash window, the slot the print comes out of below
        d.rectangle((0, 0, w, h * .3), fill=ga.shade(char, 1))
        d.rounded_rectangle((w * .08, h * .07, w * .24, h * .22), radius=3, fill=(18, 20, 26, 255))
        d.rounded_rectangle((w * .6, h * .06, w * .92, h * .24), radius=3, fill=(214, 220, 226, 255))
        for k in range(6):
            x = w * (.62 + k * .05)
            d.line((x, h * .07, x, h * .23), fill=(180, 186, 194, 255), width=1)
        d.rectangle((w * .12, h * .9, w * .88, h * .94), fill=(30, 30, 34, 255))
        text(d, "VOXA", int(w * .78 - text_width("VOXA", 2) / 2), int(h * .74), (90, 70, 50, 255), 2)
        round_alpha(tile, 8)

    def back(tile):
        vgrad(tile, ga.shade(char, 1.1), ga.shade(char, .85))
        d = ImageDraw.Draw(tile)
        w, h = tile.size
        d.rounded_rectangle((w * .1, h * .1, w * .32, h * .3), radius=4, fill=(12, 12, 16, 255), outline=(80, 80, 86, 255), width=2)
        for k in range(5):
            d.line((w * .45, h * (.55 + k * .07), w * .9, h * (.55 + k * .07)), fill=(30, 30, 34, 255), width=2)
        round_alpha(tile, 8)

    m.body(2, 3, 14, 10, 5, 11, 1.0, front, back, (24, 24, 28), (90, 90, 96))
    # The lens, standing out of the cream front: a black barrel, a chrome ring, the glass
    ga.octo(m, "z", 11, 12.4, 6.6, 5.6, 1.9, (30, 30, 34))
    ga.octo(m, "z", 12.4, 12.7, 6.6, 5.6, 1.7, (190, 192, 198), end_b=lens_glass)
    # The red shutter button on top, the flash bar's frosted top
    m.box([11.2, 10, 8.6], [12.6, 10.5, 10], {"*": ga.facet((206, 36, 30), 1.1)})
    m.box([9.5, 10, 9.6], [13.6, 10.3, 11], {"*": (200, 204, 210)})
    d = dict(ga.upright_display(1.0, .5, .55))
    # Held to take a picture: its lens away from the holder
    d["firstperson_righthand"] = {"rotation": [-4, 152, 6], "translation": [-2.5, 4, -1], "scale": [.42] * 3}
    d["firstperson_lefthand"] = {"rotation": [-4, -152, -6], "translation": [-2.5, 4, -1], "scale": [.42] * 3}
    d["thirdperson_righthand"] = {"rotation": [72, 180, 0], "translation": [0, 1.5, 3], "scale": [.55] * 3}
    d["thirdperson_lefthand"] = {"rotation": [72, 180, 0], "translation": [0, 1.5, 3], "scale": [.55] * 3}
    ga.held(m, d)


def lens_glass(tile):
    w, h = tile.size
    d = ImageDraw.Draw(tile)
    d.ellipse((0, 0, w - 1, h - 1), fill=(150, 154, 162, 255))
    d.ellipse((w * .12, h * .12, w * .88, h * .88), fill=(14, 16, 26, 255))
    d.ellipse((w * .3, h * .3, w * .7, h * .7), fill=(30, 40, 80, 255))
    d.ellipse((w * .28, h * .22, w * .46, h * .4), fill=(220, 230, 255, 200))


def film_pack():
    m = Model("pellicule")

    def front(tile):
        w, h = tile.size
        vgrad(tile, (40, 40, 44), (20, 20, 24))
        d = ImageDraw.Draw(tile)
        d.rectangle((w * .1, h * .12, w * .9, h * .5), fill=(236, 230, 214, 255))
        d.rectangle((w * .1, h * .5, w * .9, h * .56), fill=(200, 40, 34, 255))
        text(d, "VOXA", int(w / 2 - text_width("VOXA", 2) / 2), int(h * .2), (40, 36, 30, 255), 2)
        text(d, "10", int(w / 2 - text_width("10", 2) / 2), int(h * .66), (220, 220, 226, 255), 2)
        round_alpha(tile, 4)

    def back(tile):
        vgrad(tile, (30, 30, 34), (16, 16, 18))
        round_alpha(tile, 4)

    m.body(3.5, 3, 12.5, 12, 7.2, 8.8, .4, front, back, (14, 14, 16), (60, 60, 66))
    ga.held(m, ga.upright_display(1.0))


def photo_item():
    """A photograph seen as a thing: a white frame, a picture of a dusky street in it (the real one is seen when it is
    looked at), the black back of instant film."""
    m = Model("photo", 512, 24)

    def front(tile):
        w, h = tile.size
        S = 3
        img = Image.new("RGBA", (w * S, h * S), (246, 244, 236, 255))
        d = ImageDraw.Draw(img)
        x0, y0, x1 = w * S * .08, w * S * .08, w * S * .92
        y1 = y0 + (x1 - x0)
        pic = Image.new("RGB", (int(x1 - x0), int(y1 - y0)))
        pw, ph = pic.size
        a = np.zeros((ph, pw, 3))
        ys = np.linspace(0, 1, ph)[:, None]
        sky = np.array([80, 96, 120], float)[None, None, :] * (1 - ys[..., None]) + np.array([170, 130, 100], float)[None, None, :] * ys[..., None]
        a[:] = sky
        a[int(ph * .62):] = np.array([52, 50, 48])
        pic = Image.fromarray(a.astype(np.uint8), "RGB")
        pd = ImageDraw.Draw(pic)
        for k, (hx, hh) in enumerate(((.05, .3), (.25, .42), (.5, .25), (.7, .38))):
            pd.rectangle((pw * hx, ph * (.62 - hh), pw * (hx + .18), ph * .62), fill=(36, 34, 38))
            pd.rectangle((pw * (hx + .05), ph * (.62 - hh + .06), pw * (hx + .09), ph * (.62 - hh + .1)), fill=(230, 190, 110))
        pd.ellipse((pw * .78, ph * .1, pw * .9, ph * .22), fill=(240, 220, 180))
        pic = pic.filter(ImageFilter.GaussianBlur(S * .6))
        img.paste(pic, (int(x0), int(y0)))
        d.rectangle((x0, y0, x1, y1), outline=(200, 196, 186, 255), width=S)
        gi.finish(tile, img)

    def back(tile):
        vgrad(tile, (34, 34, 38), (18, 18, 20))

    edge = gi.edge_of((236, 234, 226))
    m.box([3.8, 2.2, 7.96], [12.2, 12.4, 8.04], {"south": front, "north": back, "up": edge, "down": edge, "east": edge, "west": edge})
    gi.write_own(m, gi.PAPER_HELD)


def polaroid_frame():
    """The frame a photograph is looked at in: 150 x 180 units at two pixels a unit, its picture at (11, 11), 128 square."""
    W, H = 300, 360
    rng = np.random.default_rng(5)
    a = np.ones((H, W, 3)) * np.array([247, 245, 238], float) + rng.normal(0, 2.2, (H, W, 1))
    ys, xs = np.mgrid[0:H, 0:W]
    edge = np.minimum(np.minimum(xs, W - 1 - xs), np.minimum(ys, H - 1 - ys))
    a *= (1 - .08 * np.exp(-edge / 5.0))[..., None]
    a *= (1 - .04 * ys / H)[..., None]
    img = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    d = ImageDraw.Draw(img)
    d.rectangle((20, 20, 279, 279), fill=(30, 30, 34, 255))
    d.rectangle((19, 19, 280, 280), outline=(206, 202, 192, 255), width=2)
    round_alpha(img, 6)
    kit.save(img, "textures/gui/gear/polaroid.png")


def shutter(rate=48000):
    """The shutter's snap, then the little motor pushing the print out."""
    rng = np.random.default_rng(3)
    n = int(rate * 1.6)
    t = np.arange(n) / rate
    click = rng.normal(0, 1, n) * np.exp(-t / .006) * .9 + np.sin(2 * np.pi * 1900 * t) * np.exp(-t / .02) * .4
    click2 = np.roll(rng.normal(0, 1, n) * np.exp(-t / .005) * .6, int(.045 * rate))
    on = (t > .25) & (t < 1.45)
    motor = (np.sign(np.sin(2 * np.pi * (180 + 40 * np.clip((t - .25) / 1.2, 0, 1)) * t)) * .25
             + np.sin(2 * np.pi * 360 * t) * .15) * on * np.clip((t - .25) / .05, 0, 1) * np.clip((1.45 - t) / .1, 0, 1)
    whir = ga.band(rng.normal(0, 1, n), 1200, 5000) * on * .12
    y = click + click2 + motor + whir
    return y / np.abs(y).max() * .85


def recipes():
    ga.shaped("appareil_photo", ["IGI", "BRB"], {"I": "minecraft:iron_ingot", "G": "minecraft:glass_pane", "B": "minecraft:black_dye",
                                                  "R": "minecraft:redstone"})
    ga.shaped("pellicule", ["PPP", "BIB"], {"P": "minecraft:paper", "B": "minecraft:black_dye", "I": "minecraft:ink_sac"})


def main():
    recipes()
    camera()
    film_pack()
    photo_item()
    polaroid_frame()
