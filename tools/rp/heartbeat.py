"""The heartbeat of fear (original sound, made here): one beat as heard from inside, a deep thud and its echo. The
noise is drawn from the same random sequence as when Époque RP made it, so the sound is the same."""
import os
import subprocess
import tempfile
import wave

import numpy as np

import kit

RATE = 48000


def band(x, lo, hi, rate=RATE):
    spec = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / rate)
    spec[(f < lo) | (f > hi)] = 0
    return np.fft.irfft(spec, len(x))


def ogg(name, samples, rate=RATE):
    samples = np.clip(samples, -1, 1)
    path = os.path.join(kit.ROOT, "sounds", f"{name}.ogg")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, "s.wav")
        with wave.open(wav, "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(rate)
            w.writeframes((samples * 32767).astype("<i2").tobytes())
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "4", path], check=True)
    print("wrote", os.path.relpath(path))


def main():
    rng = np.random.default_rng(9)
    rate = RATE
    # The draws Époque RP made first (a radio's hiss, a walkie-talkie's squelch), so the beat's noise is the same
    rng.normal(0, 1, rate * 4)
    for _ in range(60):
        rng.integers(0, rate * 4 - 200)
        rng.normal(0, 1, 200)
        rng.uniform(.3, 1)
    rng.normal(0, 1, int(rate * .32))
    n = int(rate * .62)
    t = np.arange(n) / rate

    def thud(at, f, k):
        tt = np.clip(t - at, 0, None)
        return np.sin(2 * np.pi * f * tt * (1 - tt * .8)) * np.exp(-tt * 22) * (t >= at) * k

    beat = thud(0, 52, 1) + thud(.21, 44, .7) + band(rng.normal(0, 1, n), 30, 160) * np.exp(-t * 18) * .25
    ogg("coeur_peur", beat / np.abs(beat).max() * .95)


if __name__ == "__main__":
    main()
