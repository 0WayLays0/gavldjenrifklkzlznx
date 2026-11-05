#!/usr/bin/env python3
"""The music player's art and sounds: its window (a night-blue panel framed in gold, faint staves of music behind its
lists), its icons, the record that turns while it plays, the built-in tune "Valse des Arcanes" (an original waltz for
music box, bass and strings, synthesized here), and the sound events the player's music is played through (one per
range, each with the range as its attenuation distance; their file is a moment of silence, never heard: the player
streams the music itself).

Called by gen_assets.py (run that one). Needs ffmpeg (libmp3lame, libvorbis) for the sounds. All art and music is
original, drawn and synthesized here.
"""
import math
import os
import subprocess
import tempfile
import wave

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

import gen_assets as g

#: The window's measures, in GUI pixels; its texture is drawn at twice that.
W, H = 384, 240
S = 2
#: Its parts (GUI pixels): the title bar, the side list, the track list, the player bar.
SIDE = (8, 28, 102, 172)
LIST = (108, 28, 376, 172)
BAR = (8, 178, 376, 232)

GOLD = (222, 178, 92)
GOLD_LIGHT = (255, 230, 160)
GOLD_DARK = (128, 92, 40)
NIGHT_TOP = (34, 22, 66)
NIGHT_BOTTOM = (12, 10, 32)
VIOLET = (150, 96, 230)
CYAN = (110, 210, 255)

#: The ranges the player's music can be heard to (blocks); 0: only by the player.
RANGES = (8, 16, 24, 32, 48, 64)


def _rounded(d, box, r, fill=None, outline=None, width=1):
    d.rounded_rectangle(box, radius=r, fill=fill, outline=outline, width=width)


def _note(d, x, y, size, color):
    """An eighth note, its head at (x, y)."""
    d.ellipse((x - size * .55, y - size * .4, x + size * .55, y + size * .4), fill=color)
    d.line((x + size * .5, y, x + size * .5, y - size * 2.2), fill=color, width=max(1, int(size * .22)))
    d.line((x + size * .5, y - size * 2.2, x + size * 1.3, y - size * 1.5), fill=color, width=max(1, int(size * .22)))


def window_texture():
    """768 x 480: the window drawn at twice its GUI size."""
    w, h = W * S, H * S
    ys = np.linspace(0, 1, h)[:, None]
    xs = np.linspace(0, 1, w)[None, :]
    top, bottom = np.array(NIGHT_TOP, float), np.array(NIGHT_BOTTOM, float)
    rgb = top[None, None, :] * (1 - ys[..., None]) + bottom[None, None, :] * ys[..., None] + np.zeros((h, w, 1))
    # A soft violet glow from the upper left and a cyan one from the lower right
    glow = np.exp(-(((xs - .2) / .35) ** 2 + ((ys - .15) / .3) ** 2))
    rgb += np.array([60, 30, 90])[None, None, :] * glow[..., None]
    glow2 = np.exp(-(((xs - .85) / .3) ** 2 + ((ys - .9) / .25) ** 2))
    rgb += np.array([10, 40, 70])[None, None, :] * glow2[..., None]
    rng = np.random.default_rng(4401)
    rgb += rng.normal(0, 2.5, rgb.shape)
    img = Image.fromarray(np.clip(rgb, 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    # Rounded corners: the window's shape
    mask = Image.new("L", (w, h), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, w - 1, h - 1), radius=14 * S // 2, fill=255)
    img.putalpha(mask)
    over = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(over)
    # Faint staves across the window, notes resting on them
    for k, y0 in enumerate((40, 96, 152, 200)):
        for i in range(5):
            y = (y0 + i * 3) * S
            d.line((10 * S, y, (W - 10) * S, y), fill=(200, 180, 255, 14), width=1)
        for j in range(9):
            x = (24 + j * 40 + (k * 17) % 30) * S
            _note(d, x, (y0 + (j * 7 + k * 3) % 12) * S, 3.2 * S, (220, 200, 255, 20))
    # The parts: dark glass panes with a thin gold edge
    for box, r in ((SIDE, 6), (LIST, 6), (BAR, 8)):
        x0, y0, x1, y1 = (v * S for v in box)
        _rounded(d, (x0, y0, x1, y1), r * S // 2 * 2, fill=(6, 4, 18, 150), outline=GOLD_DARK + (200,), width=1 * S // 2 + 1)
        d.line((x0 + 6, y0 + 2, x1 - 6, y0 + 2), fill=(255, 255, 255, 18), width=1)
    # The title bar's band
    _rounded(d, (6 * S, 4 * S, (W - 6) * S, 23 * S), 8, fill=(255, 255, 255, 10))
    img = Image.alpha_composite(img, over)
    # The gold frame: an outer rule, a highlight, an inner rule
    d = ImageDraw.Draw(img)
    _rounded(d, (1, 1, w - 2, h - 2), 14, outline=GOLD_DARK + (255,), width=4)
    _rounded(d, (3, 3, w - 4, h - 4), 12, outline=GOLD + (255,), width=2)
    _rounded(d, (2, 2, w - 3, h - 3), 13, outline=GOLD_LIGHT + (140,), width=1)
    _rounded(d, (8, 8, w - 9, h - 9), 9, outline=GOLD_DARK + (180,), width=1)
    # Flourishes at the corners: a curl of gold and a small note in it
    for cx, cy, sx, sy in ((14, 14, 1, 1), (w - 15, 14, -1, 1), (14, h - 15, 1, -1), (w - 15, h - 15, -1, -1)):
        for r, a in ((16, 255), (11, 200)):
            d.arc((cx - r + sx * r, cy - r + sy * r, cx + r + sx * r, cy + r + sy * r), 0, 360, fill=GOLD + (a,), width=2)
        d.ellipse((cx + sx * 16 - 3, cy + sy * 16 - 3, cx + sx * 16 + 3, cy + sy * 16 + 3), fill=GOLD_LIGHT + (255,))
    # A lyre of gold at the top middle, over the frame
    lx, ly = w // 2, 4
    d.polygon([(lx - 20, ly), (lx + 20, ly), (lx + 12, ly + 12), (lx - 12, ly + 12)], fill=NIGHT_TOP + (255,), outline=GOLD + (255,))
    for i in range(4):
        x = lx - 7 + i * 4.7
        d.line((x, ly + 2, x, ly + 11), fill=GOLD_LIGHT + (255,), width=1)
    d.arc((lx - 16, ly - 6, lx - 4, ly + 12), 90, 270, fill=GOLD + (255,), width=2)
    d.arc((lx + 4, ly - 6, lx + 16, ly + 12), 270, 90, fill=GOLD + (255,), width=2)
    return img


# ------------------------------------------------------------------------------------------------------- icons

#: The icons, in order on their sheet (8 a row, each 16 GUI pixels drawn at 32).
ICONS = ("play", "pause", "prev", "next", "shuffle", "repeat", "repeat_one", "star", "star_full", "plus", "folder", "refresh",
         "trash", "note", "speaker", "range", "search", "library", "close", "playlist", "disc", "muted", "lock", "check")


def _icon(name):
    """One icon, white (tinted in game), drawn at 4x and brought down to 32 x 32."""
    n = 128
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = (255, 255, 255, 255)
    lw = 12

    def poly(pts):
        d.polygon([(x * n, y * n) for x, y in pts], fill=c)

    def line(pts, width=lw):
        d.line([(x * n, y * n) for x, y in pts], fill=c, width=width, joint="curve")

    if name == "play":
        poly([(.28, .16), (.84, .5), (.28, .84)])
    elif name == "pause":
        d.rounded_rectangle((.24 * n, .16 * n, .42 * n, .84 * n), radius=6, fill=c)
        d.rounded_rectangle((.58 * n, .16 * n, .76 * n, .84 * n), radius=6, fill=c)
    elif name in ("prev", "next"):
        s = 1 if name == "next" else -1
        cx = .5
        poly([(cx - s * .32, .2), (cx + s * .08, .5), (cx - s * .32, .8)])
        poly([(cx - s * .02, .2), (cx + s * .36, .5), (cx - s * .02, .8)])
        d.rectangle(((cx + s * .36 - .05) * n if s > 0 else (cx + s * .36 - .02) * n, .2 * n,
                     (cx + s * .36 + .02) * n if s > 0 else (cx + s * .36 + .05) * n, .8 * n), fill=c)
    elif name == "shuffle":
        line([(.12, .3), (.36, .3), (.64, .7), (.82, .7)])
        line([(.12, .7), (.36, .7), (.64, .3), (.82, .3)])
        poly([(.78, .18), (.94, .3), (.78, .42)])
        poly([(.78, .58), (.94, .7), (.78, .82)])
    elif name in ("repeat", "repeat_one"):
        d.arc((.14 * n, .2 * n, .86 * n, .8 * n), 200, 340, fill=c, width=lw)
        d.arc((.14 * n, .2 * n, .86 * n, .8 * n), 20, 160, fill=c, width=lw)
        poly([(.7, .1), (.9, .26), (.68, .36)])
        poly([(.3, .9), (.1, .74), (.32, .64)])
        if name == "repeat_one":
            d.text((.42 * n, .3 * n), "1", fill=c)
            line([(.47, .36), (.53, .3), (.53, .7)], width=10)
    elif name in ("star", "star_full"):
        pts = []
        for k in range(10):
            a = -math.pi / 2 + k * math.pi / 5
            r = .44 if k % 2 == 0 else .19
            pts.append((.5 + math.cos(a) * r, .53 + math.sin(a) * r))
        if name == "star_full":
            poly(pts)
        else:
            d.polygon([(x * n, y * n) for x, y in pts], outline=c, width=9)
    elif name == "plus":
        d.rounded_rectangle((.42 * n, .14 * n, .58 * n, .86 * n), radius=5, fill=c)
        d.rounded_rectangle((.14 * n, .42 * n, .86 * n, .58 * n), radius=5, fill=c)
    elif name == "folder":
        d.rounded_rectangle((.1 * n, .24 * n, .46 * n, .4 * n), radius=6, fill=c)
        d.rounded_rectangle((.1 * n, .32 * n, .9 * n, .82 * n), radius=8, fill=c)
    elif name == "refresh":
        d.arc((.16 * n, .16 * n, .84 * n, .84 * n), 30, 320, fill=c, width=lw)
        poly([(.66, .08), (.9, .26), (.62, .36)])
    elif name == "trash":
        d.rounded_rectangle((.24 * n, .3 * n, .76 * n, .9 * n), radius=7, fill=c)
        d.rectangle((.16 * n, .18 * n, .84 * n, .26 * n), fill=c)
        d.rectangle((.4 * n, .1 * n, .6 * n, .18 * n), fill=c)
    elif name == "note":
        d.ellipse((.16 * n, .6 * n, .46 * n, .86 * n), fill=c)
        d.rectangle((.38 * n, .16 * n, .46 * n, .74 * n), fill=c)
        poly([(.42, .14), (.82, .26), (.82, .4), (.42, .3)])
    elif name in ("speaker", "muted"):
        poly([(.1, .38), (.3, .38), (.54, .16), (.54, .84), (.3, .62), (.1, .62)])
        if name == "speaker":
            d.arc((.4 * n, .3 * n, .72 * n, .7 * n), -55, 55, fill=c, width=9)
            d.arc((.4 * n, .16 * n, .92 * n, .84 * n), -55, 55, fill=c, width=9)
        else:
            line([(.64, .34), (.92, .66)], width=10)
            line([(.64, .66), (.92, .34)], width=10)
    elif name == "range":
        d.ellipse((.42 * n, .42 * n, .58 * n, .58 * n), fill=c)
        for r in (.22, .36):
            d.arc(((.5 - r) * n, (.5 - r) * n, (.5 + r) * n, (.5 + r) * n), -50, 50, fill=c, width=8)
            d.arc(((.5 - r) * n, (.5 - r) * n, (.5 + r) * n, (.5 + r) * n), 130, 230, fill=c, width=8)
    elif name == "search":
        d.ellipse((.12 * n, .12 * n, .64 * n, .64 * n), outline=c, width=lw)
        line([(.58, .58), (.88, .88)], width=14)
    elif name == "library":
        for i, h0 in enumerate((.16, .26, .2, .3)):
            d.rounded_rectangle(((.12 + i * .2) * n, h0 * n, (.26 + i * .2) * n, .86 * n), radius=4, fill=c)
    elif name == "close":
        line([(.22, .22), (.78, .78)], width=14)
        line([(.22, .78), (.78, .22)], width=14)
    elif name == "playlist":
        for y in (.24, .44, .64):
            d.rounded_rectangle((.08 * n, (y - .04) * n, .56 * n, (y + .04) * n), radius=4, fill=c)
        d.ellipse((.58 * n, .66 * n, .8 * n, .86 * n), fill=c)
        d.rectangle((.74 * n, .2 * n, .8 * n, .78 * n), fill=c)
        poly([(.77, .18), (.94, .26), (.94, .36), (.77, .3)])
    elif name == "disc":
        d.ellipse((.08 * n, .08 * n, .92 * n, .92 * n), fill=c)
        d.ellipse((.4 * n, .4 * n, .6 * n, .6 * n), fill=(0, 0, 0, 0))
        d.arc((.2 * n, .2 * n, .8 * n, .8 * n), 200, 250, fill=(0, 0, 0, 0), width=5)
    elif name == "lock":
        d.rounded_rectangle((.2 * n, .44 * n, .8 * n, .9 * n), radius=8, fill=c)
        d.arc((.3 * n, .12 * n, .7 * n, .6 * n), 180, 360, fill=c, width=10)
    elif name == "check":
        line([(.16, .52), (.4, .76), (.86, .24)], width=14)
    small = img.resize((32, 32), Image.LANCZOS)
    return small


def icon_sheet():
    sheet = Image.new("RGBA", (8 * 32, ((len(ICONS) + 7) // 8) * 32), (0, 0, 0, 0))
    for i, name in enumerate(ICONS):
        sheet.alpha_composite(_icon(name), ((i % 8) * 32, (i // 8) * 32))
    return sheet


def record_texture():
    """128 x 128: a record of black vinyl, its grooves catching a little light, a label of violet and gold."""
    n = 512
    ys, xs = np.mgrid[0:n, 0:n].astype(np.float32)
    r = np.hypot(xs - n / 2 + .5, ys - n / 2 + .5) / (n / 2)
    ang = np.arctan2(ys - n / 2, xs - n / 2)
    grooves = .5 + .5 * np.sin(r * 260)
    base = 18 + 10 * grooves * (r > .36) * (r < .97)
    # A lighter band between tracks
    for rb in (.55, .72, .86):
        base += 18 * np.exp(-((r - rb) / .006) ** 2)
    sheen = 22 * np.exp(-((np.sin(ang * 2) - 1) ** 2) / .05) * (r > .36)
    v = np.clip(base + sheen, 0, 255)
    rgb = np.dstack([v * .95, v * .9, v * 1.1])
    alpha = np.where(r <= 1, 255, 0)
    img = Image.fromarray(np.dstack([np.clip(rgb, 0, 255), alpha]).astype(np.uint8), "RGBA")
    d = ImageDraw.Draw(img)
    c = n / 2
    # The label
    lr = .34 * c
    d.ellipse((c - lr, c - lr, c + lr, c + lr), fill=(110, 60, 190, 255))
    d.ellipse((c - lr * .88, c - lr * .88, c + lr * .88, c + lr * .88), outline=GOLD + (255,), width=6)
    for k in range(5):
        y = c - 30 + k * 12
        d.line((c - 70, y, c + 70, y), fill=(255, 230, 160, 80), width=2)
    _note(d, c - 26, c + 18, 22, GOLD_LIGHT + (255,))
    _note(d, c + 22, c + 4, 18, GOLD + (255,))
    d.ellipse((c - 9, c - 9, c + 9, c + 9), fill=(20, 16, 30, 255))
    d.ellipse((c - 4, c - 4, c + 4, c + 4), fill=(0, 0, 0, 0))
    img = img.filter(ImageFilter.SMOOTH)
    return img.resize((128, 128), Image.LANCZOS)


def shine_texture():
    """128 x 128: the light on a record (laid over it, not turned with it)."""
    n = 128
    ys, xs = np.mgrid[0:n, 0:n].astype(np.float32)
    r = np.hypot(xs - n / 2 + .5, ys - n / 2 + .5) / (n / 2)
    ang = np.arctan2(ys - n / 2, xs - n / 2)
    a = 90 * np.exp(-((ang + 2.3) / .35) ** 2) + 60 * np.exp(-((ang - .84) / .3) ** 2)
    a = a * (r > .38) * (r < .98) * np.clip((r - .38) * 4, 0, 1)
    return Image.fromarray(np.dstack([np.full((n, n), 255), np.full((n, n), 255), np.full((n, n), 255), np.clip(a, 0, 255)]).astype(np.uint8), "RGBA")


# ------------------------------------------------------------------------------------------------------- sound

RATE = 44100


def _env(n, attack, decay):
    t = np.arange(n) / RATE
    return np.minimum(1, t / max(attack, 1e-4)) * np.exp(-t / decay)


def _music_box(freq, dur):
    n = int(dur * RATE)
    t = np.arange(n) / RATE
    tone = (np.sin(2 * np.pi * freq * t) + .35 * np.sin(2 * np.pi * freq * 2.01 * t) * np.exp(-t * 3)
            + .18 * np.sin(2 * np.pi * freq * 4.2 * t) * np.exp(-t * 9) + .08 * np.sin(2 * np.pi * freq * 6.8 * t) * np.exp(-t * 14))
    return tone * _env(n, .002, .55)


def _bass(freq, dur):
    n = int(dur * RATE)
    t = np.arange(n) / RATE
    return (np.sin(2 * np.pi * freq * t) + .3 * np.sin(2 * np.pi * freq * 2 * t)) * _env(n, .01, .5)


def _strings(freqs, dur):
    n = int(dur * RATE)
    t = np.arange(n) / RATE
    out = np.zeros(n)
    for f in freqs:
        for det in (-.004, 0, .005):
            ph = np.random.default_rng(int(f)).random() * 6.28
            saw = 2 * ((f * (1 + det) * t + ph) % 1) - 1
            out += saw
    # A soft low-pass: a running average
    k = 24
    out = np.convolve(out, np.ones(k) / k, mode="same")
    env = np.minimum(1, t / .25) * np.minimum(1, (dur - t) / .3)
    return out * env / (len(freqs) * 3)


def _midi(note):
    return 440 * 2 ** ((note - 69) / 12)


def valse_des_arcanes():
    """An original waltz in A minor (3/4, 132 to the minute): a music box over a bass and soft strings, 24 bars."""
    beat = 60 / 132
    bar = 3 * beat
    # Chords (root, third, fifth as MIDI) and the melody, bar by bar (note, beats)
    am, f, c, g, dm, e = (57, 60, 64), (53, 57, 60), (48, 52, 55), (55, 59, 62), (50, 53, 57), (52, 56, 59)
    chords = [am, f, c, g, am, dm, e, am] * 3
    melody = [
        [(76, 1), (72, 1), (69, 1)], [(77, 2), (76, 1)], [(72, 1), (74, 1), (76, 1)], [(74, 3)],
        [(76, 1), (79, 1), (81, 1)], [(77, 1), (76, 1), (74, 1)], [(71, 1), (72, 1), (74, 1)], [(69, 3)],
        [(81, 1), (76, 1), (72, 1)], [(81, 1), (77, 1), (72, 1)], [(79, 1), (76, 1), (72, 1)], [(79, 1), (74, 1), (71, 1)],
        [(76, 1), (72, 1), (69, 1)], [(77, 1), (74, 1), (69, 1)], [(76, 1), (80, 1), (83, 1)], [(81, 3)],
        [(76, 1), (72, 1), (69, 1)], [(77, 2), (76, 1)], [(72, 1), (74, 1), (76, 1)], [(79, 2), (77, 1)],
        [(76, 1), (79, 1), (81, 1)], [(77, 1), (76, 1), (74, 1)], [(71, 1), (74, 1), (80, 1)], [(81, 3)],
    ]
    total = int((len(chords) * bar + 2.5) * RATE)
    left, right = np.zeros(total), np.zeros(total)

    def add(sig, at, pan=0.0, gain=1.0):
        i = int(at * RATE)
        j = min(total, i + len(sig))
        left[i:j] += sig[:j - i] * gain * (1 - pan) * .5
        right[i:j] += sig[:j - i] * gain * (1 + pan) * .5

    for b, chord in enumerate(chords):
        t0 = b * bar
        # Bass on the downbeat, the chord's upper notes on beats two and three
        add(_bass(_midi(chord[0] - 12), bar), t0, 0, .55)
        for k in (1, 2):
            for i, nt in enumerate(chord[1:]):
                add(_music_box(_midi(nt + 12), beat * 1.5) * .12, t0 + k * beat, -.35 + .7 * i, 1)
        add(_strings([_midi(nt) for nt in chord], bar + .25), t0, 0, .22)
        at = t0
        for nt, beats in melody[b]:
            add(_music_box(_midi(nt + 12), beats * beat + .6), at, .15 * math.sin(nt), .42)
            at += beats * beat
    # A little room: two echoes, crossed
    for delay, gain in ((.11, .28), (.23, .16)):
        d = int(delay * RATE)
        l2, r2 = left.copy(), right.copy()
        left[d:] += r2[:-d] * gain
        right[d:] += l2[:-d] * gain
    mix = np.stack([left, right], 1)
    mix /= np.max(np.abs(mix)) * 1.12
    # Fade in and out
    fade = int(.02 * RATE)
    mix[:fade] *= np.linspace(0, 1, fade)[:, None]
    tail = int(2.0 * RATE)
    mix[-tail:] *= np.linspace(1, 0, tail)[:, None]
    return (mix * 32767).astype(np.int16)


def _write_wav(path, pcm, channels):
    with wave.open(path, "wb") as w:
        w.setnchannels(channels)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(pcm.tobytes())


def _encode(pcm, channels, out_rel, args):
    out = os.path.join(g.ROOT, out_rel)
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        src = os.path.join(tmp, "in.wav")
        _write_wav(src, pcm, channels)
        subprocess.run(["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-i", src, *args, "-map_metadata", "-1", "-fflags", "+bitexact",
                        "-flags:a", "+bitexact", out], check=True)
    print("wrote", os.path.relpath(out))


def sounds():
    _encode(valse_des_arcanes(), 2, "music/valse_des_arcanes.mp3", ["-c:a", "libmp3lame", "-b:a", "112k"])
    silence = np.zeros((int(.25 * RATE), 1), np.int16)
    _encode(silence, 1, "sounds/music/silence.ogg", ["-c:a", "libvorbis", "-q:a", "0"])
    events = {}
    for r in RANGES:
        events[f"music_player.r{r}"] = {"sounds": [{"name": "arcanes_oublies:music/silence", "stream": True, "attenuation_distance": r}]}
    events["music_player.self"] = {"sounds": [{"name": "arcanes_oublies:music/silence", "stream": True}]}
    g.write_json("sounds.json", events)


def main():
    g.save(window_texture(), "textures/gui/music/window.png")
    g.save(icon_sheet(), "textures/gui/music/icons.png")
    g.save(record_texture(), "textures/gui/music/record.png")
    g.save(shine_texture(), "textures/gui/music/record_shine.png")
    sounds()


if __name__ == "__main__":
    main()
