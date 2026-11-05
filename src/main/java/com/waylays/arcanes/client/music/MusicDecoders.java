package com.waylays.arcanes.client.music;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import net.minecraft.client.sounds.JOrbisAudioStream;

import javax.annotation.Nullable;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Turns a music file (MP3, OGG Vorbis or WAV, held in memory) into 16-bit samples; tells how long it lasts and what it
 * is called (its tags).
 */
public final class MusicDecoders {
    private MusicDecoders() {}

    /** A source of samples: 16-bit, interleaved over its channels. */
    public interface PcmSource extends Closeable {
        int rate();

        int channels();

        /** Fills {@code out} with samples; how many it put there, or -1 at the end. */
        int read(short[] out) throws IOException;
    }

    public static boolean supported(String ext) {
        return switch (ext.toLowerCase(Locale.ROOT)) {
            case "mp3", "ogg", "wav" -> true;
            default -> false;
        };
    }

    public static PcmSource open(byte[] data, String ext) throws IOException {
        return switch (ext.toLowerCase(Locale.ROOT)) {
            case "mp3" -> new Mp3(data);
            case "ogg" -> new Ogg(data);
            case "wav" -> new Wav(data);
            default -> throw new IOException("unsupported music file: " + ext);
        };
    }

    // ---------------------------------------------------------------- MP3

    static final class Mp3 implements PcmSource {
        private final Bitstream bits;
        private final Decoder decoder = new Decoder();
        private short[] pending;
        private int pendingAt, pendingLen;
        private int rate, channels;
        private boolean ended;

        Mp3(byte[] data) throws IOException {
            this.bits = new Bitstream(new ByteArrayInputStream(data));
            // The first frame tells its rate and channels
            if (!decodeNext()) {
                throw new IOException("no MP3 frame in the file");
            }
        }

        private boolean decodeNext() throws IOException {
            while (true) {
                Header h;
                try {
                    h = bits.readFrame();
                } catch (BitstreamException e) {
                    return false;
                }
                if (h == null) {
                    return false;
                }
                try {
                    SampleBuffer sb = (SampleBuffer) decoder.decodeFrame(h, bits);
                    rate = sb.getSampleFrequency();
                    channels = sb.getChannelCount();
                    pending = sb.getBuffer();
                    pendingLen = sb.getBufferLength();
                    pendingAt = 0;
                    return true;
                } catch (DecoderException | RuntimeException e) {
                    // A broken frame: skip it
                } finally {
                    bits.closeFrame();
                }
            }
        }

        /** Skips frames (without decoding them, but the last two, which feed the next) up to {@code ms} in. */
        void skip(long ms) throws IOException {
            double at = 0;
            while (at < ms - 60) {
                Header h;
                try {
                    h = bits.readFrame();
                } catch (BitstreamException e) {
                    ended = true;
                    return;
                }
                if (h == null) {
                    ended = true;
                    return;
                }
                at += h.ms_per_frame();
                bits.closeFrame();
            }
            pendingLen = 0;
            for (int k = 0; k < 2 && decodeNext(); k++) {
                // Decoded and dropped: they refill the decoder's reservoir
                pendingLen = 0;
            }
        }

        @Override
        public int rate() {
            return rate;
        }

        @Override
        public int channels() {
            return channels;
        }

        @Override
        public int read(short[] out) throws IOException {
            int n = 0;
            while (n < out.length) {
                if (pendingAt >= pendingLen) {
                    if (ended || !decodeNext()) {
                        ended = true;
                        break;
                    }
                }
                int take = Math.min(out.length - n, pendingLen - pendingAt);
                System.arraycopy(pending, pendingAt, out, n, take);
                pendingAt += take;
                n += take;
            }
            return n == 0 && ended ? -1 : n;
        }

        @Override
        public void close() {
            try {
                bits.close();
            } catch (BitstreamException ignored) {
            }
        }
    }

    // ---------------------------------------------------------------- OGG Vorbis (the game's own decoder)

    static final class Ogg implements PcmSource {
        private final JOrbisAudioStream stream;
        private final int rate, channels;
        private ByteBuffer pending;
        private boolean ended;

        Ogg(byte[] data) throws IOException {
            this.stream = new JOrbisAudioStream(new ByteArrayInputStream(data));
            AudioFormat f = stream.getFormat();
            this.rate = (int) f.getSampleRate();
            this.channels = f.getChannels();
        }

        @Override
        public int rate() {
            return rate;
        }

        @Override
        public int channels() {
            return channels;
        }

        @Override
        public int read(short[] out) throws IOException {
            int n = 0;
            while (n < out.length) {
                if (pending == null || pending.remaining() < 2) {
                    if (ended) {
                        break;
                    }
                    ByteBuffer next = stream.read(out.length * 2);
                    if (next == null || next.remaining() < 2) {
                        ended = true;
                        break;
                    }
                    pending = next.order(ByteOrder.nativeOrder());
                }
                while (n < out.length && pending.remaining() >= 2) {
                    out[n++] = pending.getShort();
                }
            }
            return n == 0 && ended ? -1 : n;
        }

        @Override
        public void close() throws IOException {
            stream.close();
        }
    }

    // ---------------------------------------------------------------- WAV (Java's own reader)

    static final class Wav implements PcmSource {
        private final AudioInputStream in;
        private final int rate, channels;
        private final byte[] bytes = new byte[16384];

        Wav(byte[] data) throws IOException {
            try {
                AudioInputStream src = AudioSystem.getAudioInputStream(new BufferedInputStream(new ByteArrayInputStream(data)));
                AudioFormat f = src.getFormat();
                AudioFormat pcm = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, f.getSampleRate(), 16, f.getChannels(), f.getChannels() * 2,
                        f.getSampleRate(), false);
                this.in = AudioSystem.getAudioInputStream(pcm, src);
                this.rate = (int) f.getSampleRate();
                this.channels = f.getChannels();
            } catch (Exception e) {
                throw new IOException("unreadable WAV file", e);
            }
        }

        @Override
        public int rate() {
            return rate;
        }

        @Override
        public int channels() {
            return channels;
        }

        @Override
        public int read(short[] out) throws IOException {
            int want = Math.min(out.length * 2, bytes.length);
            int got = 0;
            while (got < want) {
                int r = in.read(bytes, got, want - got);
                if (r < 0) {
                    break;
                }
                got += r;
            }
            if (got < 2) {
                return -1;
            }
            int n = got / 2;
            for (int i = 0; i < n; i++) {
                out[i] = (short) (bytes[2 * i] & 0xFF | bytes[2 * i + 1] << 8);
            }
            return n;
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }

    // ---------------------------------------------------------------- length and tags

    /** How long the music lasts, ms (0 if it cannot be told). */
    public static int lengthMs(byte[] data, String ext) {
        try {
            switch (ext.toLowerCase(Locale.ROOT)) {
                case "mp3" -> {
                    Bitstream bits = new Bitstream(new ByteArrayInputStream(data));
                    double ms = 0;
                    Header h;
                    while ((h = bits.readFrame()) != null) {
                        ms += h.ms_per_frame();
                        bits.closeFrame();
                    }
                    return (int) ms;
                }
                case "ogg" -> {
                    return oggLength(data);
                }
                case "wav" -> {
                    AudioFileFormat f = AudioSystem.getAudioFileFormat(new BufferedInputStream(new ByteArrayInputStream(data)));
                    if (f.getFrameLength() > 0) {
                        return (int) (f.getFrameLength() * 1000L / (long) f.getFormat().getFrameRate());
                    }
                    return 0;
                }
                default -> {
                    return 0;
                }
            }
        } catch (Exception e) {
            return 0;
        }
    }

    /** An OGG Vorbis file's length: the granule (sample) count on its last page over its sample rate. */
    private static int oggLength(byte[] d) {
        int rate = 0;
        for (int i = 0; i + 16 < d.length && i < 4096; i++) {
            if (d[i] == 1 && d[i + 1] == 'v' && d[i + 2] == 'o' && d[i + 3] == 'r' && d[i + 4] == 'b' && d[i + 5] == 'i' && d[i + 6] == 's') {
                rate = le32(d, i + 12);
                break;
            }
        }
        if (rate <= 0) {
            return 0;
        }
        for (int i = d.length - 27; i >= 0; i--) {
            if (d[i] == 'O' && d[i + 1] == 'g' && d[i + 2] == 'g' && d[i + 3] == 'S') {
                long granule = le32(d, i + 6) & 0xFFFFFFFFL | (long) le32(d, i + 10) << 32;
                return granule > 0 ? (int) (granule * 1000 / rate) : 0;
            }
        }
        return 0;
    }

    private static int le32(byte[] d, int i) {
        return d[i] & 0xFF | (d[i + 1] & 0xFF) << 8 | (d[i + 2] & 0xFF) << 16 | (d[i + 3] & 0xFF) << 24;
    }

    /** The title and artist written in the file, if any (ID3 for MP3, Vorbis comments for OGG). */
    public record Tags(@Nullable String title, @Nullable String artist) {
        static final Tags NONE = new Tags(null, null);
    }

    public static Tags tags(InputStream in, String ext) {
        try {
            byte[] head = in.readNBytes(256 * 1024);
            return switch (ext.toLowerCase(Locale.ROOT)) {
                case "mp3" -> id3(head);
                case "ogg" -> vorbisComments(head);
                default -> Tags.NONE;
            };
        } catch (Exception e) {
            return Tags.NONE;
        }
    }

    /** ID3v2.3 / 2.4 at the start of an MP3: its TIT2 (title) and TPE1 (artist) frames. */
    private static Tags id3(byte[] d) {
        if (d.length < 10 || d[0] != 'I' || d[1] != 'D' || d[2] != '3') {
            return Tags.NONE;
        }
        int version = d[3];
        int size = (d[6] & 0x7F) << 21 | (d[7] & 0x7F) << 14 | (d[8] & 0x7F) << 7 | d[9] & 0x7F;
        int end = Math.min(d.length, 10 + size);
        int i = 10;
        String title = null, artist = null;
        while (i + 10 <= end) {
            String id = new String(d, i, 4, StandardCharsets.ISO_8859_1);
            if (id.charAt(0) == 0) {
                break;
            }
            int len = version >= 4 ? (d[i + 4] & 0x7F) << 21 | (d[i + 5] & 0x7F) << 14 | (d[i + 6] & 0x7F) << 7 | d[i + 7] & 0x7F
                    : (d[i + 4] & 0xFF) << 24 | (d[i + 5] & 0xFF) << 16 | (d[i + 6] & 0xFF) << 8 | d[i + 7] & 0xFF;
            int body = i + 10;
            if (len <= 0 || body + len > end) {
                break;
            }
            if (id.equals("TIT2")) {
                title = text(d, body, len);
            } else if (id.equals("TPE1")) {
                artist = text(d, body, len);
            }
            i = body + len;
        }
        return new Tags(blank(title), blank(artist));
    }

    private static String text(byte[] d, int at, int len) {
        int enc = d[at];
        byte[] b = new byte[len - 1];
        System.arraycopy(d, at + 1, b, 0, len - 1);
        String s = switch (enc) {
            case 1 -> new String(b, StandardCharsets.UTF_16);
            case 2 -> new String(b, StandardCharsets.UTF_16BE);
            case 3 -> new String(b, StandardCharsets.UTF_8);
            default -> new String(b, StandardCharsets.ISO_8859_1);
        };
        return s.replace("\u0000", "").trim();
    }

    /** Vorbis comments (TITLE=, ARTIST=) in the second header of an OGG Vorbis file. */
    private static Tags vorbisComments(byte[] d) {
        for (int i = 0; i + 7 < d.length; i++) {
            if (d[i] == 3 && d[i + 1] == 'v' && d[i + 2] == 'o' && d[i + 3] == 'r' && d[i + 4] == 'b' && d[i + 5] == 'i' && d[i + 6] == 's') {
                int p = i + 7;
                int vendor = le32(d, p);
                p += 4 + vendor;
                if (vendor < 0 || p + 4 > d.length) {
                    return Tags.NONE;
                }
                int count = le32(d, p);
                p += 4;
                String title = null, artist = null;
                for (int k = 0; k < count && p + 4 <= d.length; k++) {
                    int len = le32(d, p);
                    p += 4;
                    if (len < 0 || p + len > d.length) {
                        break;
                    }
                    String c = new String(d, p, len, StandardCharsets.UTF_8);
                    p += len;
                    int eq = c.indexOf('=');
                    if (eq > 0) {
                        String key = c.substring(0, eq).toUpperCase(Locale.ROOT);
                        if (key.equals("TITLE")) {
                            title = c.substring(eq + 1);
                        } else if (key.equals("ARTIST")) {
                            artist = c.substring(eq + 1);
                        }
                    }
                }
                return new Tags(blank(title), blank(artist));
            }
        }
        return Tags.NONE;
    }

    @Nullable
    private static String blank(@Nullable String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
