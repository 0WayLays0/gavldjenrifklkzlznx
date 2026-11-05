package com.waylays.arcanes.client.music;

import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.client.sounds.AudioStream;
import org.lwjgl.BufferUtils;

import javax.annotation.Nullable;
import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A music file played as the game's sound engine wants it: one channel (only a single channel is placed in the world,
 * heard louder near its source and fading with distance), 16-bit, a piece at a time. It starts {@code fromMs} in, starts
 * over at the end while it {@link #setLoop loops}, and notes how loud each twentieth of a second is (the player's bars
 * dance to it).
 */
public final class MusicStream implements AudioStream {
    /** The loudness noted for each slice of the music this long, ms. */
    public static final int LEVEL_MS = 50;

    private final byte[] data;
    private final String ext;
    private MusicDecoders.PcmSource source;
    private final AudioFormat format;
    private final int channels;
    private volatile boolean loop;
    private volatile boolean ended;
    private short[] buffer = new short[8192];
    /** Samples left to drop at the start (seeking where the decoder cannot skip), and the fade-in over the first. */
    private long dropFrames;
    private int fadeIn;
    private final int fadeLength;
    private final Levels levels;
    private double levelSum;
    private int levelCount;
    private final int levelFrames;

    /** The loudness of the slices of a track, as far as it has been played, shared by the streams that play it. */
    public static final class Levels {
        private float[] values = new float[1024];
        private int size;

        synchronized void set(int index, float v) {
            if (index < 0 || index > 200_000) {
                return;
            }
            if (index >= values.length) {
                float[] grown = new float[Math.max(values.length * 2, index + 64)];
                System.arraycopy(values, 0, grown, 0, size);
                values = grown;
            }
            values[index] = v;
            size = Math.max(size, index + 1);
        }

        /** The loudness {@code ms} into the track, 0 to 1 (0 where it has not been played yet). */
        public synchronized float at(long ms) {
            int i = (int) (ms / LEVEL_MS);
            return i >= 0 && i < size ? values[i] : 0;
        }
    }

    public MusicStream(byte[] data, String ext, long fromMs, boolean loop, @Nullable Levels levels) throws IOException {
        this.data = data;
        this.ext = ext;
        this.loop = loop;
        this.levels = levels != null ? levels : new Levels();
        this.source = MusicDecoders.open(data, ext);
        this.channels = Math.max(1, source.channels());
        this.format = new AudioFormat(source.rate(), 16, 1, true, false);
        this.levelFrames = Math.max(1, source.rate() * LEVEL_MS / 1000);
        this.fadeLength = source.rate() / 40;
        this.fadeIn = fromMs > 0 ? fadeLength : 0;
        seek(fromMs);
    }

    /** The slice of the track this stream will hand out next ({@link #LEVEL_MS} long each), counted from its start. */
    private long frameAt;

    private void seek(long ms) throws IOException {
        frameAt = ms * source.rate() / 1000;
        if (source instanceof MusicDecoders.Mp3 mp3) {
            mp3.skip(ms);
        } else {
            dropFrames = frameAt;
        }
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    /** Whether it has played to its end (and does not loop). */
    public boolean ended() {
        return ended;
    }

    public Levels levels() {
        return levels;
    }

    @Override
    public AudioFormat getFormat() {
        return format;
    }

    @Nullable
    @Override
    public ByteBuffer read(int size) throws IOException {
        if (ended) {
            return null;
        }
        int frames = Math.max(1, size / 2);
        ByteBuffer out = BufferUtils.createByteBuffer(frames * 2);
        int written = 0;
        int restarts = 0;
        while (written < frames) {
            int want = Math.min(buffer.length / channels, frames - written) * channels;
            int n = source.read(want == buffer.length ? buffer : shorter(want));
            if (n < 0) {
                if (loop && restarts++ < 2) {
                    source.close();
                    source = MusicDecoders.open(data, ext);
                    frameAt = 0;
                    continue;
                }
                ended = true;
                break;
            }
            short[] in = want == buffer.length ? buffer : scratch;
            int got = n / channels;
            for (int f = 0; f < got; f++) {
                if (dropFrames > 0) {
                    dropFrames--;
                    continue;
                }
                int sum = 0;
                for (int c = 0; c < channels; c++) {
                    sum += in[f * channels + c];
                }
                float v = (float) sum / channels;
                if (fadeIn > 0) {
                    v *= 1 - (float) fadeIn / fadeLength;
                    fadeIn--;
                }
                short s = (short) Math.max(-32768, Math.min(32767, Math.round(v)));
                out.putShort(s);
                written++;
                note(s);
                if (written >= frames) {
                    // What is left of this read is dropped: rare (the read asked for no more than fits)
                    break;
                }
            }
        }
        out.flip();
        if (out.remaining() == 0) {
            return null;
        }
        return out;
    }

    private short[] scratch = new short[0];

    private short[] shorter(int n) {
        if (scratch.length != n) {
            scratch = new short[n];
        }
        return scratch;
    }

    /** Adds a sample to the loudness of the slice it is in. */
    private void note(short s) {
        double v = s / 32768.0;
        levelSum += v * v;
        if (++levelCount >= levelFrames) {
            float rms = (float) Math.sqrt(levelSum / levelCount);
            levels.set((int) (frameAt / levelFrames), Math.min(1, rms * 2.2f));
            levelSum = 0;
            levelCount = 0;
        }
        frameAt++;
    }

    @Override
    public void close() {
        try {
            source.close();
        } catch (IOException e) {
            ArcanesOublies.LOGGER.debug("closing a music stream", e);
        }
    }
}
