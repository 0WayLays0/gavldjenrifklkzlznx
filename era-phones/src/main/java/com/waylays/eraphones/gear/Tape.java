package com.waylays.eraphones.gear;

import com.waylays.eraphones.EraPhones;
import net.neoforged.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * The cassettes' recordings: MP3 files the game masters put in config/eraphones/cassettes on the server, read once
 * (in the background) and kept; and a deck, which plays a tape like a real one: forward, paused, and wound back or
 * forward at speed, the sound running past backwards or squealing forwards.
 */
public final class Tape {
    /** How much faster the tape runs when wound. */
    public static final int WIND = 7;
    private static final Map<String, short[]> LOADED = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> LOADING = new ConcurrentHashMap<>();

    private Tape() {}

    public static Path folder() {
        return FMLPaths.CONFIGDIR.get().resolve("eraphones").resolve("cassettes");
    }

    /** The recordings there are (file names). */
    public static List<String> files() {
        List<String> out = new ArrayList<>();
        try {
            Files.createDirectories(folder());
            try (Stream<Path> s = Files.list(folder())) {
                s.filter(p -> {
                    String n = p.getFileName().toString().toLowerCase();
                    return n.endsWith(".mp3") || n.endsWith(".mp4");
                }).forEach(p -> out.add(p.getFileName().toString()));
            }
        } catch (IOException e) {
            EraPhones.LOGGER.warn("Cannot list the cassettes: {}", e.toString());
        }
        out.sort(String::compareToIgnoreCase);
        return out;
    }

    /** The recording {@code file}, if read; starts reading it otherwise (null until then). */
    @Nullable
    public static short[] sound(String file) {
        if (Video.isVideo(file)) {
            // A film's sound comes with its pictures, read and kept by Video
            return Video.sound(file);
        }
        short[] pcm = LOADED.get(file);
        if (pcm != null) {
            return pcm;
        }
        Audio audio = Audio.get();
        if (audio != null && LOADING.putIfAbsent(file, true) == null) {
            Thread t = new Thread(() -> {
                try {
                    Path p = folder().resolve(file).normalize();
                    if (!p.startsWith(folder()) || !Files.isRegularFile(p)) {
                        EraPhones.LOGGER.warn("No cassette recording {}", file);
                        return;
                    }
                    short[] s = audio.decodeMp3(p);
                    if (s != null) {
                        LOADED.put(file, s);
                    }
                } catch (Throwable e) {
                    EraPhones.LOGGER.warn("The cassette {} cannot be read: {}", file, e.toString());
                } finally {
                    LOADING.remove(file);
                }
            }, "cassette-" + file);
            t.setDaemon(true);
            t.start();
        }
        return null;
    }

    /** Mixed down to one channel and brought to 48 000 samples a second. */
    public static short[] resample(short[] interleaved, int channels, float rate) {
        channels = Math.max(1, channels);
        int frames = interleaved.length / channels;
        float[] mono = new float[frames];
        for (int i = 0; i < frames; i++) {
            float sum = 0;
            for (int c = 0; c < channels; c++) {
                sum += interleaved[i * channels + c];
            }
            mono[i] = sum / channels;
        }
        double step = rate / Audio.RATE;
        int n = (int) Math.floor((frames - 1) / step) + 1;
        short[] out = new short[Math.max(0, n)];
        for (int i = 0; i < out.length; i++) {
            double t = i * step;
            int k = (int) t;
            float f = (float) (t - k);
            float v = mono[Math.min(k, frames - 1)] * (1 - f) + mono[Math.min(k + 1, frames - 1)] * f;
            out[i] = (short) Math.max(-32768, Math.min(32767, Math.round(v)));
        }
        return out;
    }

    /** {@code samples} as minutes and seconds, the way a tape counter shows them. */
    public static String clock(long samples) {
        long s = Math.max(0, samples / Audio.RATE);
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    public enum Mode { STOP, PLAY, PAUSE, REWIND, FORWARD }

    /**
     * A tape in a deck: where it is, what it is doing. {@link #frame()} is called from the sound's own thread every
     * 20 ms; the rest from the game's.
     */
    public static final class Deck {
        public volatile Mode mode = Mode.STOP;
        public volatile long pos;
        public volatile String file;
        private final Random random = new Random();
        private float wow, hiss;
        /** A film's sound through a worn tape and a small television speaker: its filters' memory, its drop-outs. */
        private float low, high, lastIn, drop = 1, dropTo = 1;

        /** The next 20 ms of what comes out of the deck; null when the tape stops or reaches an end. */
        @Nullable
        public short[] frame() {
            Mode m = mode;
            String f = file;
            if (f == null || m == Mode.STOP || m == Mode.PAUSE) {
                return null;
            }
            short[] pcm = sound(f);
            short[] out = new short[Audio.FRAME];
            if (pcm == null) {
                return out;
            }
            long p = pos;
            switch (m) {
                case PLAY -> {
                    if (p >= pcm.length) {
                        mode = Mode.STOP;
                        pos = pcm.length;
                        return null;
                    }
                    // A little wow and flutter, a little hiss: the sound of a tape
                    boolean film = Video.isVideo(f);
                    if (film && random.nextFloat() < .02f) {
                        dropTo = random.nextFloat() < .35f ? .25f + random.nextFloat() * .4f : 1;
                    }
                    for (int i = 0; i < out.length; i++) {
                        wow += 1f / Audio.RATE;
                        double w = film ? 2.4 : 1;
                        double at = p + i + (Math.sin(wow * Math.PI * 2 * .55) * 18 + Math.sin(wow * Math.PI * 2 * 7.3) * 1.5) * w;
                        float v = sample(pcm, at) * .92f;
                        if (film) {
                            // A small speaker: no deep bass, no bright top; pushed a little too hard; the tape dropping out
                            low += (v - low) * .32f;
                            high = .985f * (high + low - lastIn);
                            lastIn = low;
                            drop += (dropTo - drop) * .002f;
                            float x = high * 1.7f * drop / 32768f;
                            v = (float) Math.tanh(x) * 26000 + (random.nextFloat() - .5f) * 900;
                        } else {
                            v += (random.nextFloat() - .5f) * 260;
                        }
                        out[i] = (short) Math.max(-32768, Math.min(32767, v));
                    }
                    pos = p + out.length;
                }
                case REWIND, FORWARD -> {
                    int dir = m == Mode.REWIND ? -1 : 1;
                    if ((dir < 0 && p <= 0) || (dir > 0 && p >= pcm.length)) {
                        mode = Mode.STOP;
                        pos = dir < 0 ? 0 : pcm.length;
                        return null;
                    }
                    // The heads still touch the tape: the sound flies past, high and backwards when rewinding
                    for (int i = 0; i < out.length; i++) {
                        double at = p + (double) dir * i * WIND;
                        hiss = hiss * .97f + (random.nextFloat() - .5f) * .03f;
                        out[i] = (short) Math.max(-32768, Math.min(32767, sample(pcm, at) * .55f + hiss * 9000));
                    }
                    pos = Math.max(0, Math.min(pcm.length, p + (long) dir * out.length * WIND));
                }
                default -> {
                    return null;
                }
            }
            return out;
        }

        /**
         * Moves the tape on by one game tick when no sound is drawn from it (no Simple Voice Chat): a film still runs
         * on the television, silent.
         */
        public void advance() {
            Mode m = mode;
            String f = file;
            if (f == null || m == Mode.STOP || m == Mode.PAUSE) {
                return;
            }
            short[] pcm = sound(f);
            if (pcm == null) {
                return;
            }
            int step = Audio.RATE / 20 * (m == Mode.PLAY ? 1 : WIND) * (m == Mode.REWIND ? -1 : 1);
            long p = pos + step;
            if (p <= 0 || p >= pcm.length) {
                pos = Math.max(0, Math.min(pcm.length, p));
                mode = Mode.STOP;
            } else {
                pos = p;
            }
        }

        private static float sample(short[] pcm, double at) {
            if (at < 0 || at >= pcm.length - 1) {
                return 0;
            }
            int k = (int) at;
            float f = (float) (at - k);
            return pcm[k] * (1 - f) + pcm[k + 1] * f;
        }
    }
}
