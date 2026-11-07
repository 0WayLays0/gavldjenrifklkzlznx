package com.waylays.eraphones.horror;

import java.util.Random;

/**
 * The voices a game master can put on (with Simple Voice Chat): the sound of the microphone, 48 000 samples a second,
 * worked frame by frame. Each voice is a chain of stages, each keeping its own state from one frame to the next, so a
 * voice belongs to one speaker. Nothing here knows Simple Voice Chat: the samples come in, the samples go out.
 */
public final class VoiceFx {
    public static final int RATE = 48000;
    /** The voices, by the name the command takes. */
    public static final String[] VOICES = {"grave", "masque", "demon", "clown", "enfant", "radio", "egouts", "spectre"};

    private VoiceFx() {}

    public static boolean exists(String id) {
        for (String v : VOICES) {
            if (v.equals(id)) {
                return true;
            }
        }
        return false;
    }

    /** A fresh chain for the voice {@code id}; null if there is no such voice. */
    public static Chain chain(String id) {
        return switch (id) {
            // Deeper, nothing else
            case "grave" -> new Chain(1.1f, new Pitch(.8f, 40, 0, 0), new Biquad(Biquad.HIGH, 70, .7f, 0));
            // The masked caller: much deeper, rough, a metallic warble, the top taken off
            case "masque" -> new Chain(.85f, new Pitch(.7f, 42, 0, 0), new Biquad(Biquad.HIGH, 90, .7f, 0), new Biquad(Biquad.PEAK, 190, 1, 5),
                    new Drive(2.4f), new Ring(46, .2f), new Biquad(Biquad.LOW, 3400, .8f, 0), new Biquad(Biquad.LOW, 3400, .8f, 0));
            // Two voices at once, an octave and a fourth below, torn
            case "demon" -> new Chain(.6f, new Parallel(new Pitch(.5f, 48, 0, 0), .7f, new Pitch(.75f, 36, 0, 0), .45f), new Drive(4.5f),
                    new Biquad(Biquad.LOW, 2600, .8f, 0), new Echo(70, .25f, .25f, 2000));
            // Higher, wavering like a song, trembling, a little ring of echo
            case "clown" -> new Chain(1.4f, new Pitch(1.28f, 30, 5.5f, .035f), new Tremolo(7, .25f), new Biquad(Biquad.HIGH, 160, .7f, 0),
                    new Echo(120, .2f, .22f, 4000));
            // A small child
            case "enfant" -> new Chain(1.35f, new Pitch(1.5f, 26, 0, 0), new Biquad(Biquad.HIGH, 220, .7f, 0), new Biquad(Biquad.LOW, 6500, .7f, 0));
            // Through an old radio: a narrow band, clipped, crackling
            case "radio" -> new Chain(.75f, new Biquad(Biquad.HIGH, 450, .8f, 0), new Biquad(Biquad.LOW, 2600, .9f, 0), new Biquad(Biquad.PEAK, 1400, 1.2f, 6),
                    new Drive(6), new Crackle(9, .25f));
            // From down the drains: a little deeper, echoing far away
            case "egouts" -> new Chain(1, new Pitch(.92f, 40, 0, 0), new Echo(190, .5f, .45f, 2500), new Echo(330, .35f, .3f, 1800),
                    new Biquad(Biquad.LOW, 4200, .7f, 0));
            // A whisper with no breath behind it, echoing
            case "spectre" -> new Chain(1, new Whisper(), new Echo(140, .35f, .3f, 3000));
            default -> null;
        };
    }

    /** One step of a chain: a sample in, a sample out. */
    public interface Stage {
        float tick(float x);
    }

    /** The stages one after the other, then a gain and a soft limit so nothing clips. */
    public static final class Chain {
        private final Stage[] stages;
        private final float gain;

        public Chain(float gain, Stage... stages) {
            this.stages = stages;
            this.gain = gain;
        }

        public float tick(float x) {
            for (Stage s : stages) {
                x = s.tick(x);
            }
            x *= gain;
            if (!Float.isFinite(x)) {
                return 0;
            }
            // Soft above 0.8, never past 1
            float a = Math.abs(x);
            if (a > .8f) {
                float over = (a - .8f) / .2f;
                a = .8f + .2f * (float) Math.tanh(over);
                x = Math.copySign(a, x);
            }
            return x;
        }

        public short[] process(short[] in) {
            short[] out = new short[in.length];
            for (int i = 0; i < in.length; i++) {
                float y = tick(in[i] / 32768f);
                out[i] = (short) Math.max(-32768, Math.min(32767, Math.round(y * 32767)));
            }
            return out;
        }
    }

    /**
     * The pitch moved by {@code ratio} without changing the speed: two heads read a delay line, sliding at a rate of
     * their own, each fading in and out as the other takes over. {@code vibrato} Hz wobbles the ratio by
     * {@code depth}.
     */
    public static final class Pitch implements Stage {
        private static final int SIZE = 8192, MASK = SIZE - 1;
        private final float[] line = new float[SIZE];
        private final float ratio, window, vibrato, depth;
        private int write;
        private float phase, lfo;

        public Pitch(float ratio, float windowMs, float vibrato, float depth) {
            this.ratio = ratio;
            this.window = windowMs * RATE / 1000f;
            this.vibrato = vibrato;
            this.depth = depth;
        }

        @Override
        public float tick(float x) {
            line[write] = x;
            float r = ratio;
            if (vibrato > 0) {
                lfo += vibrato / RATE;
                lfo -= (float) Math.floor(lfo);
                r *= 1 + depth * (float) Math.sin(lfo * Math.PI * 2);
            }
            phase += (1 - r) / window;
            phase -= (float) Math.floor(phase);
            float p2 = phase + .5f;
            p2 -= (float) Math.floor(p2);
            float y = read(phase * window) * hann(phase) + read(p2 * window) * hann(p2);
            write = (write + 1) & MASK;
            return y;
        }

        private static float hann(float p) {
            return .5f - .5f * (float) Math.cos(p * Math.PI * 2);
        }

        /** The line {@code delay} samples back (at least one), between two samples. */
        private float read(float delay) {
            float pos = write - 1 - delay;
            int i = (int) Math.floor(pos);
            float f = pos - i;
            return line[i & MASK] * (1 - f) + line[(i + 1) & MASK] * f;
        }
    }

    /** Two chains of one stage each, side by side, mixed. */
    public static final class Parallel implements Stage {
        private final Stage a, b;
        private final float ga, gb;

        public Parallel(Stage a, float ga, Stage b, float gb) {
            this.a = a;
            this.b = b;
            this.ga = ga;
            this.gb = gb;
        }

        @Override
        public float tick(float x) {
            return a.tick(x) * ga + b.tick(x) * gb;
        }
    }

    /** A filter of the cookbook kind: low pass, high pass, band pass, or a bell boosting {@code gainDb} round a frequency. */
    public static final class Biquad implements Stage {
        public static final int LOW = 0, HIGH = 1, BAND = 2, PEAK = 3;
        private final float b0, b1, b2, a1, a2;
        private float x1, x2, y1, y2;

        public Biquad(int type, float freq, float q, float gainDb) {
            double w = 2 * Math.PI * freq / RATE, cos = Math.cos(w), alpha = Math.sin(w) / (2 * q);
            double nb0, nb1, nb2, na0, na1, na2;
            switch (type) {
                case LOW -> {
                    nb0 = (1 - cos) / 2;
                    nb1 = 1 - cos;
                    nb2 = (1 - cos) / 2;
                    na0 = 1 + alpha;
                    na1 = -2 * cos;
                    na2 = 1 - alpha;
                }
                case HIGH -> {
                    nb0 = (1 + cos) / 2;
                    nb1 = -(1 + cos);
                    nb2 = (1 + cos) / 2;
                    na0 = 1 + alpha;
                    na1 = -2 * cos;
                    na2 = 1 - alpha;
                }
                case BAND -> {
                    nb0 = alpha;
                    nb1 = 0;
                    nb2 = -alpha;
                    na0 = 1 + alpha;
                    na1 = -2 * cos;
                    na2 = 1 - alpha;
                }
                default -> {
                    double a = Math.pow(10, gainDb / 40);
                    nb0 = 1 + alpha * a;
                    nb1 = -2 * cos;
                    nb2 = 1 - alpha * a;
                    na0 = 1 + alpha / a;
                    na1 = -2 * cos;
                    na2 = 1 - alpha / a;
                }
            }
            b0 = (float) (nb0 / na0);
            b1 = (float) (nb1 / na0);
            b2 = (float) (nb2 / na0);
            a1 = (float) (na1 / na0);
            a2 = (float) (na2 / na0);
        }

        @Override
        public float tick(float x) {
            float y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            if (Math.abs(y) < 1e-20f) {
                y = 0;
            }
            x2 = x1;
            x1 = x;
            y2 = y1;
            y1 = y;
            return y;
        }
    }

    /** Overdriven: pushed into a curve that rounds the peaks off. */
    public static final class Drive implements Stage {
        private final float drive, norm;

        public Drive(float drive) {
            this.drive = drive;
            this.norm = (float) (1 / Math.tanh(drive));
        }

        @Override
        public float tick(float x) {
            return (float) Math.tanh(x * drive) * norm * .7f;
        }
    }

    /** Partly multiplied by a low tone: the voice turns metallic and warbles. */
    public static final class Ring implements Stage {
        private final float freq, mix;
        private float phase;

        public Ring(float freq, float mix) {
            this.freq = freq;
            this.mix = mix;
        }

        @Override
        public float tick(float x) {
            phase += freq / RATE;
            phase -= (float) Math.floor(phase);
            return x * (1 - mix + mix * (float) Math.sin(phase * Math.PI * 2));
        }
    }

    /** The loudness wavering. */
    public static final class Tremolo implements Stage {
        private final float freq, depth;
        private float phase;

        public Tremolo(float freq, float depth) {
            this.freq = freq;
            this.depth = depth;
        }

        @Override
        public float tick(float x) {
            phase += freq / RATE;
            phase -= (float) Math.floor(phase);
            return x * (1 - depth * (.5f + .5f * (float) Math.sin(phase * Math.PI * 2)));
        }
    }

    /** An echo: the voice comes back {@code delayMs} later, each time softer and duller. */
    public static final class Echo implements Stage {
        private final float[] line;
        private final float feedback, mix, damp;
        private int pos;
        private float low;

        public Echo(float delayMs, float feedback, float mix, float dampHz) {
            this.line = new float[Math.max(1, Math.round(delayMs * RATE / 1000f))];
            this.feedback = feedback;
            this.mix = mix;
            this.damp = (float) (1 - Math.exp(-2 * Math.PI * dampHz / RATE));
        }

        @Override
        public float tick(float x) {
            float back = line[pos];
            low += (back - low) * damp;
            line[pos] = x + low * feedback;
            pos = (pos + 1) % line.length;
            return x + low * mix;
        }
    }

    /** Clicks and pops, as from a bad line, {@code rate} a second, while there is a voice. */
    public static final class Crackle implements Stage {
        private final Random random = new Random(7);
        private final float rate, amount;
        private float env, click;

        public Crackle(float rate, float amount) {
            this.rate = rate;
            this.amount = amount;
        }

        @Override
        public float tick(float x) {
            env = Math.max(Math.abs(x), env * .9995f);
            if (random.nextFloat() < rate / RATE) {
                click = (random.nextFloat() * 2 - 1) * amount;
            }
            click *= .9f;
            float hiss = (random.nextFloat() * 2 - 1) * .02f;
            return x + (click + hiss) * Math.min(1, env * 4);
        }
    }

    /**
     * A whisper: the voice's own sound is mostly replaced by breath (noise, shaped to the voice's loudness and
     * filtered to where whispers live), with a thin edge of the voice kept so the words still come through.
     */
    public static final class Whisper implements Stage {
        private final Random random = new Random(11);
        private final Biquad band = new Biquad(Biquad.BAND, 2400, .6f, 0), band2 = new Biquad(Biquad.BAND, 1200, .8f, 0);
        private final Biquad edge = new Biquad(Biquad.HIGH, 1800, .7f, 0);
        private final Pitch low = new Pitch(.85f, 40, 0, 0);
        private float env;

        @Override
        public float tick(float x) {
            float a = Math.abs(x);
            env += (a - env) * (a > env ? .02f : .0015f);
            float n = random.nextFloat() * 2 - 1;
            float breath = (band.tick(n) * .8f + band2.tick(n) * .5f) * env * 2.2f;
            return breath + edge.tick(x) * .6f + low.tick(x) * .12f;
        }
    }
}
