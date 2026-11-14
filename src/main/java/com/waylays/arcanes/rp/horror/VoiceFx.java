package com.waylays.arcanes.rp.horror;

import java.util.Random;

/**
 * The voices a game master can put on (with Simple Voice Chat): the sound of the microphone, 48 000 samples a second,
 * worked frame by frame. Each voice is a chain of stages, each keeping its own state from one frame to the next, so a
 * voice belongs to one speaker. Nothing here knows Simple Voice Chat: the samples come in, the samples go out.
 */
public final class VoiceFx {
    public static final int RATE = 48000;
    /** The voices, by the name the command takes. */
    public static final String[] VOICES = {"masque", "demon", "clown", "possede", "legion", "robot", "geant", "grave", "enfant", "spectre",
            "egouts", "radio", "glitch"};

    public static boolean exists(String id) {
        for (String v : VOICES) {
            if (v.equals(id)) {
                return true;
            }
        }
        return false;
    }

    /** A fresh chain for the voice {@code id}, at full strength; null if there is no such voice. */
    public static Chain chain(String id) {
        return chain(id, 100);
    }

    /**
     * A fresh chain for the voice {@code id}, {@code strength} percent of it (the rest the speaker's own voice); null
     * if there is no such voice. The pitch and the shape of the mouth are moved apart (a deep voice keeps a human
     * mouth instead of sounding slowed down), then come the effects.
     */
    public static Chain chain(String id, int strength) {
        Stage[] stages = switch (id) {
            // The masked caller: a man's voice far too deep but still a mouth, hoarse, a little metallic, close in the ear
            case "masque" -> new Stage[] {new Lpc(Lpc.residual(new Pitch(.68f, 38, 0, 0)), .86f, 0), new Biquad(Biquad.HIGH, 80, .7f, 0),
                    new Biquad(Biquad.PEAK, 160, .9f, 5), new Biquad(Biquad.PEAK, 2600, 1.4f, 4), new Drive(2.8f), new Ring(38, .12f),
                    new Biquad(Biquad.LOW, 5200, .7f, 0)};
            // Two throats, an octave and a fifth below, huge mouths, torn, in a stone hall
            case "demon" -> new Stage[] {new Parallel(new Lpc(Lpc.residual(new Pitch(.5f, 46, 0, 0)), .74f, 0), .75f,
                    new Lpc(Lpc.residual(new Pitch(.67f, 40, 0, 0)), .82f, 0), .5f), new Drive(5), new Biquad(Biquad.LOW, 3200, .8f, 0),
                    new Reverb(.82f, .35f, .3f)};
            // High and sing-song through a wide grinning mouth, a deep growl hidden under it, a giggle in the tremble
            case "clown" -> new Stage[] {new Parallel(new Lpc(Lpc.residual(new Pitch(1.32f, 30, 5.5f, .045f)), .97f, 0), 1,
                    new Lpc(Lpc.residual(new Pitch(.52f, 44, 0, 0)), .8f, 0), .32f), new Tremolo(6.5f, .22f), new Reverb(.6f, .25f, .2f)};
            // Your words sung by something else: a minor chord speaks them, your own voice faint under it, a cathedral round it
            case "possede" -> new Stage[] {new Parallel(new Lpc(Lpc.chord(new float[] {55, 65.4f, 82.4f, 110}), 1, 0), .85f,
                    new Lpc(Lpc.residual(new Pitch(.75f, 40, 0, 0)), .9f, 0), .4f), new Drive(1.6f), new Reverb(.88f, .3f, .45f)};
            // Many voices at once, each a little off, some far below, slightly late
            case "legion" -> new Stage[] {new Chorus(new float[] {.97f, 1.035f, .8f, .6f, 1.12f}, new float[] {0, 23, 41, 67, 89},
                    new float[] {.6f, .6f, .55f, .5f, .35f}), new Reverb(.7f, .3f, .3f)};
            // A machine's voice: one flat buzzing note says your words
            case "robot" -> new Stage[] {new Lpc(Lpc.chord(new float[] {110}), 1, 0), new Ring(70, .25f), new Biquad(Biquad.PEAK, 1800, 2, 6)};
            // A giant: very deep, a big chest and throat, echoing
            case "geant" -> new Stage[] {new Lpc(Lpc.residual(new Pitch(.58f, 44, 0, 0)), .76f, 0), new Biquad(Biquad.PEAK, 120, .8f, 6),
                    new Reverb(.75f, .4f, .25f)};
            // Deeper, a man's voice, nothing more
            case "grave" -> new Stage[] {new Lpc(Lpc.residual(new Pitch(.8f, 40, 0, 0)), .92f, 0), new Biquad(Biquad.HIGH, 70, .7f, 0)};
            // A small child: high voice, small mouth
            case "enfant" -> new Stage[] {new Lpc(Lpc.residual(new Pitch(1.55f, 26, 0, 0)), 1.22f, 0), new Biquad(Biquad.HIGH, 200, .7f, 0)};
            // A breath with no throat behind it: your words whispered by the air, echoing
            case "spectre" -> new Stage[] {new Lpc(Lpc.noise(), 1.05f, .25f), new Biquad(Biquad.HIGH, 400, .7f, 0), new Reverb(.8f, .45f, .4f)};
            // From down the drains: a concrete tunnel, the voice going on long after
            case "egouts" -> new Stage[] {new Lpc(Lpc.residual(new Pitch(.9f, 40, 0, 0)), .95f, 0), new Biquad(Biquad.LOW, 3800, .7f, 0),
                    new Echo(170, .35f, .3f, 2000), new Reverb(.9f, .55f, .5f)};
            // Through an old radio: a narrow band, clipped, crackling
            case "radio" -> new Stage[] {new Biquad(Biquad.HIGH, 450, .8f, 0), new Biquad(Biquad.LOW, 2600, .9f, 0), new Biquad(Biquad.PEAK, 1400, 1.2f, 6),
                    new Drive(6), new Crackle(9, .25f)};
            // Heard through the phone, by the phone's time: the 1985 car phone's narrow, hissing, crackling analogue line
            case "telephone_radiotelephone" -> new Stage[] {new Biquad(Biquad.HIGH, 420, .8f, 0), new Biquad(Biquad.LOW, 2700, .9f, 0),
                    new Biquad(Biquad.PEAK, 1200, 1.1f, 4), new Drive(3.2f), new Crackle(6, .18f)};
            // The 1990s mobile: the old telephone band, a little squeezed, a faint digital edge
            case "telephone_gsm" -> new Stage[] {new Biquad(Biquad.HIGH, 320, .75f, 0), new Biquad(Biquad.LOW, 3300, .8f, 0),
                    new Biquad(Biquad.PEAK, 1700, 1.2f, 3), new Drive(1.8f)};
            // The 2000s flip phone: the same band, cleaner
            case "telephone_clapet" -> new Stage[] {new Biquad(Biquad.HIGH, 300, .75f, 0), new Biquad(Biquad.LOW, 3500, .8f, 0),
                    new Biquad(Biquad.PEAK, 2000, 1.0f, 2)};
            // Today's phone: wide and clear, only the ear-piece's small speaker
            case "telephone_smartphone" -> new Stage[] {new Biquad(Biquad.HIGH, 160, .7f, 0), new Biquad(Biquad.LOW, 7000, .7f, 0)};
            // A voice going wrong: jumping high and low, stuttering, falling to pieces now and then
            case "glitch" -> new Stage[] {new Glitch(), new Biquad(Biquad.HIGH, 120, .7f, 0)};
            default -> null;
        };
        return stages == null ? null : new Chain(Math.max(0, Math.min(100, strength)) / 100f, stages);
    }

    /** One step of a chain: a sample in, a sample out. */
    public interface Stage {
        float tick(float x);
    }

    /**
     * The stages one after the other; the voice's level kept to the speaker's own (slowly, so words keep their
     * shape); mixed with the dry voice by strength; a soft limit so nothing clips.
     */
    public static final class Chain {
        private final Stage[] stages;
        private final float wet;
        private float inLevel = 1e-3f, outLevel = 1e-3f, gain = 1;

        public Chain(float wet, Stage... stages) {
            this.stages = stages;
            this.wet = wet;
        }

        public float tick(float in) {
            float x = in;
            for (Stage s : stages) {
                x = s.tick(x);
            }
            if (!Float.isFinite(x)) {
                x = 0;
            }
            inLevel += (in * in - inLevel) * .0005f;
            outLevel += (x * x - outLevel) * .0005f;
            if (inLevel > 1e-6f) {
                float target = (float) Math.sqrt(inLevel / Math.max(outLevel, 1e-9f));
                target = Math.max(.25f, Math.min(4f, target));
                gain += (target - gain) * .0008f;
            }
            x = x * gain * wet + in * (1 - wet);
            // Soft above 0.8, never past 1
            float a = Math.abs(x);
            if (a > .8f) {
                a = .8f + .2f * (float) Math.tanh((a - .8f) / .2f);
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
     * The voice split into its source (the throat's buzz, what is left once the mouth is taken out: linear
     * prediction) and its mouth (a filter, measured every few milliseconds); the source is replaced (moved in pitch,
     * or a chord, or breath) and played back through the mouth, itself made larger or smaller by {@code formant}
     * (below 1: a bigger mouth, deeper; above: smaller). {@code breath}: how much of the source becomes breath.
     */
    public static final class Lpc implements Stage {
        private static final int ORDER = 32, WINDOW = 960, HOP = 240, HIST = 4096, HMASK = HIST - 1;
        private final Source source;
        private final float formant, breath;
        private final float[] hist = new float[HIST];
        private final float[] a = new float[ORDER + 1], s = new float[ORDER + 1];
        private final float[] xs = new float[ORDER], ys = new float[ORDER];
        private final double[] win = new double[WINDOW];
        private final Random random = new Random(3);
        private int pos, count;
        private float resGain = 1, synGain = 1;

        /** What replaces the throat's buzz. */
        public interface Source {
            float next(float residual);
        }

        public static Source residual(Stage pitch) {
            return pitch::tick;
        }

        /** Saw waves at these frequencies, together. */
        public static Source chord(float[] hz) {
            float[] ph = new float[hz.length];
            return r -> {
                float sum = 0;
                for (int i = 0; i < hz.length; i++) {
                    ph[i] += hz[i] / RATE;
                    ph[i] -= (float) Math.floor(ph[i]);
                    sum += ph[i] * 2 - 1;
                }
                return sum / (float) Math.sqrt(hz.length) * .3f;
            };
        }

        public static Source noise() {
            Random r = new Random(5);
            return x -> (r.nextFloat() * 2 - 1) * .3f;
        }

        public Lpc(Source source, float formant, float breath) {
            this.source = source;
            this.formant = formant;
            this.breath = breath;
            a[0] = 1;
            s[0] = 1;
            for (int i = 0; i < WINDOW; i++) {
                win[i] = .5 - .5 * Math.cos(2 * Math.PI * (i + .5) / WINDOW);
            }
        }

        @Override
        public float tick(float x) {
            hist[pos] = x;
            pos = (pos + 1) & HMASK;
            if (++count >= HOP) {
                count = 0;
                analyse();
            }
            // The source: the voice with its mouth taken out
            float e = x;
            for (int i = 1; i <= ORDER; i++) {
                e += a[i] * xs[i - 1];
            }
            System.arraycopy(xs, 0, xs, 1, ORDER - 1);
            xs[0] = x;
            float src = source.next(e * resGain);
            if (breath > 0) {
                src = src * (1 - breath) + (random.nextFloat() * 2 - 1) * Math.abs(e) * resGain * breath * 3;
            }
            // Through the new mouth
            float y = src * synGain;
            for (int i = 1; i <= ORDER; i++) {
                y -= s[i] * ys[i - 1];
            }
            if (!Float.isFinite(y) || Math.abs(y) > 50) {
                y = 0;
                java.util.Arrays.fill(ys, 0);
            }
            System.arraycopy(ys, 0, ys, 1, ORDER - 1);
            ys[0] = y;
            return y;
        }

        /** The mouth measured on the last window, as it is (to take it out) and resized (to put it back). */
        private void analyse() {
            double[] frame = new double[WINDOW], stretched = new double[WINDOW];
            int start = pos - WINDOW;
            for (int i = 0; i < WINDOW; i++) {
                frame[i] = hist[(start + i) & HMASK] * win[i];
                // Read slower or faster: the mouth's resonances move down or up by the factor
                double t = WINDOW - 1 - (WINDOW - 1 - i) * formant;
                int k = (int) Math.floor(t);
                double f = t - k;
                double v = hist[(start + k) & HMASK] * (1 - f) + hist[(start + k + 1) & HMASK] * f;
                stretched[i] = v * win[i];
            }
            double err = levinson(frame, a);
            double err2 = levinson(stretched, s);
            double energy = 0;
            for (double v : frame) {
                energy += v * v;
            }
            if (energy < 1e-9 || err <= 0 || err2 <= 0) {
                return;
            }
            // Whitened source to unit-ish level; the new mouth's gain matched to the old one's
            resGain = (float) Math.min(400, .1 / Math.sqrt(err / (WINDOW * .375)));
            synGain = (float) (Math.sqrt(err2 / (WINDOW * .375)) / .1);
        }

        /** Linear prediction by Levinson-Durbin into {@code out}; the error left. */
        private static double levinson(double[] x, float[] out) {
            double[] r = new double[ORDER + 1];
            for (int lag = 0; lag <= ORDER; lag++) {
                double sum = 0;
                for (int i = lag; i < x.length; i++) {
                    sum += x[i] * x[i - lag];
                }
                r[lag] = sum;
            }
            if (r[0] < 1e-12) {
                return 0;
            }
            r[0] *= 1.0001;
            for (int lag = 1; lag <= ORDER; lag++) {
                // A little smoothing of the spectrum: steadier filters
                double g = 2 * Math.PI * 40 * lag / RATE;
                r[lag] *= Math.exp(-.5 * g * g);
            }
            double[] c = new double[ORDER + 1], tmp = new double[ORDER + 1];
            c[0] = 1;
            double err = r[0];
            for (int i = 1; i <= ORDER; i++) {
                double acc = r[i];
                for (int j = 1; j < i; j++) {
                    acc += c[j] * r[i - j];
                }
                double k = -acc / err;
                System.arraycopy(c, 0, tmp, 0, i);
                for (int j = 1; j < i; j++) {
                    c[j] = tmp[j] + k * tmp[i - j];
                }
                c[i] = k;
                err *= 1 - k * k;
                if (err <= 0) {
                    return 0;
                }
            }
            for (int i = 0; i <= ORDER; i++) {
                out[i] = (float) c[i];
            }
            return err;
        }
    }

    /** Several copies of the voice, each moved in pitch and a little late, together. */
    public static final class Chorus implements Stage {
        private final Pitch[] voices;
        private final float[][] delays;
        private final int[] lengths, at;
        private final float[] gains;

        public Chorus(float[] ratios, float[] delayMs, float[] gains) {
            voices = new Pitch[ratios.length];
            delays = new float[ratios.length][];
            lengths = new int[ratios.length];
            at = new int[ratios.length];
            this.gains = gains;
            for (int i = 0; i < ratios.length; i++) {
                voices[i] = new Pitch(ratios[i], 34 + i * 4, .3f + i * .17f, .006f);
                lengths[i] = Math.max(1, Math.round(delayMs[i] * RATE / 1000));
                delays[i] = new float[lengths[i]];
            }
        }

        @Override
        public float tick(float x) {
            float sum = 0;
            for (int i = 0; i < voices.length; i++) {
                float v = voices[i].tick(x);
                float out = delays[i][at[i]];
                delays[i][at[i]] = v;
                at[i] = (at[i] + 1) % lengths[i];
                sum += out * gains[i];
            }
            return sum;
        }
    }

    /** A room: eight combs and four all-passes (a small hall of the classic kind); {@code size} how long it rings. */
    public static final class Reverb implements Stage {
        private static final int[] COMBS = {1557, 1617, 1491, 1422, 1277, 1356, 1188, 1116}, PASSES = {556, 441, 341, 225};
        private final float[][] comb = new float[8][], pass = new float[4][];
        private final int[] ci = new int[8], pi = new int[4];
        private final float[] low = new float[8];
        private final float feedback, damp, mix;

        public Reverb(float size, float damp, float mix) {
            this.feedback = .7f + .28f * size;
            this.damp = damp;
            this.mix = mix;
            for (int i = 0; i < 8; i++) {
                comb[i] = new float[COMBS[i] * RATE / 44100];
            }
            for (int i = 0; i < 4; i++) {
                pass[i] = new float[PASSES[i] * RATE / 44100];
            }
        }

        @Override
        public float tick(float x) {
            float in = x * .015f, out = 0;
            for (int i = 0; i < 8; i++) {
                float y = comb[i][ci[i]];
                low[i] = y * (1 - damp) + low[i] * damp;
                comb[i][ci[i]] = in + low[i] * feedback;
                ci[i] = (ci[i] + 1) % comb[i].length;
                out += y;
            }
            for (int i = 0; i < 4; i++) {
                float b = pass[i][pi[i]];
                pass[i][pi[i]] = out + b * .5f;
                out = b - out;
                pi[i] = (pi[i] + 1) % pass[i].length;
            }
            return x * (1 - mix * .5f) + out * mix * 3;
        }
    }

    /**
     * A voice going wrong: every so often it jumps to another pitch, stutters (a scrap of the last instant repeated),
     * or crushes into a few bits, then comes back.
     */
    public static final class Glitch implements Stage {
        private static final int SIZE = 1 << 14;
        private final Random random = new Random(13);
        private final float[] line = new float[SIZE];
        private final Pitch[] pitches = {new Pitch(1, 30, 0, 0), new Pitch(.6f, 40, 0, 0), new Pitch(1.6f, 26, 0, 0), new Pitch(.45f, 46, 0, 0)};
        private int pos, left, mode, loopStart, loopLen, loopAt;
        private float hold;
        private int holdLeft;

        @Override
        public float tick(float x) {
            line[pos] = x;
            pos = (pos + 1) & (SIZE - 1);
            if (--left <= 0) {
                float r = random.nextFloat();
                mode = r < .45f ? 0 : r < .6f ? 1 : r < .72f ? 2 : r < .86f ? 3 : 4;
                left = (int) (RATE * (mode == 0 ? .25f + random.nextFloat() * .6f : .08f + random.nextFloat() * .25f));
                loopLen = (int) (RATE * (.03f + random.nextFloat() * .07f));
                loopStart = pos - loopLen;
                loopAt = 0;
            }
            float y0 = pitches[0].tick(x), y1 = pitches[1].tick(x), y2 = pitches[2].tick(x), y3 = pitches[3].tick(x);
            return switch (mode) {
                case 1 -> y1;
                case 2 -> y2;
                case 3 -> {
                    float v = line[(loopStart + loopAt) & (SIZE - 1)];
                    loopAt = (loopAt + 1) % loopLen;
                    yield v;
                }
                case 4 -> {
                    if (--holdLeft <= 0) {
                        holdLeft = 6;
                        hold = Math.round(y3 * 6) / 6f;
                    }
                    yield hold;
                }
                default -> y0;
            };
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
