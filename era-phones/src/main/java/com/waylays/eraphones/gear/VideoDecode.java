package com.waylays.eraphones.gear;

import org.jcodec.api.FrameGrab;
import org.jcodec.api.PictureWithMetadata;
import org.jcodec.codecs.aac.AACDecoder;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.AudioBuffer;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * An MP4 film made fit for a television of the time (pure Java, nothing to install): its pictures brought down to
 * {@link #W} by {@link #H}, {@link #FPS} a second, in 4 bits a colour, each packed and squeezed, kept in a file beside
 * the film so it is done once; and its sound, as 16-bit samples.
 */
public final class VideoDecode {
    public static final int W = 96, H = 72, FPS = 10;
    private static final int MAGIC = 0x54565831; // TVX1

    private VideoDecode() {}

    /** The sound of a film: samples interleaved, how many channels, at what rate. */
    public record Sound(short[] pcm, int channels, int rate) {}

    /** Reads the pictures of {@code mp4} into {@code out}; returns how many frames it holds. */
    public static int pictures(File mp4, File out) throws Exception {
        return pictures(mp4, out, pct -> {
        });
    }

    /** The name of the film's picture format if this library reads it (H.264), else what it is, or "?" when unknown. */
    public static String codec(File mp4) {
        try (FileChannelWrapper ch = NIOUtils.readableChannel(mp4)) {
            DemuxerTrack v = MP4Demuxer.createMP4Demuxer(ch).getVideoTrack();
            if (v == null) {
                return "?";
            }
            org.jcodec.common.Codec c = v.getMeta().getCodec();
            return c == null ? "?" : c.name();
        } catch (Throwable e) {
            return "?";
        }
    }

    /** As {@link #pictures(File, File)}, telling {@code progress} how far it is (0 to 100). */
    public static int pictures(File mp4, File out, java.util.function.IntConsumer progress) throws Exception {
        try (FileChannelWrapper ch = NIOUtils.readableChannel(mp4); RandomAccessFile f = new RandomAccessFile(out, "rw")) {
            FrameGrab grab = FrameGrab.createFrameGrab(ch);
            double total = 0;
            try {
                total = grab.getVideoTrack().getMeta().getTotalDuration();
            } catch (Throwable ignored) {
                // Unknown length: no progress shown
            }
            f.setLength(0);
            f.writeInt(MAGIC);
            f.writeInt(W);
            f.writeInt(H);
            f.writeInt(FPS);
            f.writeInt(0);
            // The frames, each after its length; their places noted, written at the end
            long[] at = new long[1024];
            int n = 0;
            double next = 0;
            PictureWithMetadata pm;
            Deflater def = new Deflater(6);
            while ((pm = grab.getNativeFrameWithMetadata()) != null) {
                double t = pm.getTimestamp(), d = pm.getDuration() > 0 ? pm.getDuration() : 1.0 / 25;
                if (total > 0) {
                    progress.accept((int) Math.min(99, t * 100 / total));
                }
                byte[] packed = null;
                while (next < t + d - 1e-6) {
                    if (packed == null) {
                        packed = squeeze(def, pack(pm.getPicture()));
                    }
                    if (n == at.length) {
                        at = java.util.Arrays.copyOf(at, n * 2);
                    }
                    at[n++] = f.getFilePointer();
                    f.writeInt(packed.length);
                    f.write(packed);
                    next += 1.0 / FPS;
                }
            }
            long table = f.getFilePointer();
            for (int i = 0; i < n; i++) {
                f.writeLong(at[i]);
            }
            f.writeLong(table);
            f.seek(16);
            f.writeInt(n);
            return n;
        }
    }

    /** Frame {@code index} of a prepared film, as kept (squeezed); null past its end. */
    public static byte[] frame(RandomAccessFile f, int index) throws IOException {
        f.seek(16);
        int n = f.readInt();
        if (index < 0 || index >= n) {
            return null;
        }
        f.seek(f.length() - 8);
        long table = f.readLong();
        f.seek(table + 8L * index);
        f.seek(f.readLong());
        byte[] b = new byte[f.readInt()];
        f.readFully(b);
        return b;
    }

    /** How many frames a prepared film holds. */
    public static int count(RandomAccessFile f) throws IOException {
        f.seek(16);
        return f.readInt();
    }

    /** A frame unsqueezed: {@link #W} x {@link #H} colours, 0xRGB with 4 bits each. */
    public static short[] unpack(byte[] squeezed) throws DataFormatException {
        Inflater inf = new Inflater();
        inf.setInput(squeezed);
        byte[] raw = new byte[W * H * 2];
        int got = 0;
        while (got < raw.length && !inf.finished()) {
            int k = inf.inflate(raw, got, raw.length - got);
            if (k == 0 && (inf.needsInput() || inf.needsDictionary())) {
                break;
            }
            got += k;
        }
        inf.end();
        short[] px = new short[W * H];
        for (int i = 0; i < px.length; i++) {
            px[i] = (short) (((raw[i * 2] & 0xFF) << 8) | (raw[i * 2 + 1] & 0xFF));
        }
        return px;
    }

    private static byte[] squeeze(Deflater def, byte[] raw) {
        def.reset();
        def.setInput(raw);
        def.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream(raw.length / 3);
        byte[] buf = new byte[8192];
        while (!def.finished()) {
            out.write(buf, 0, def.deflate(buf));
        }
        return out.toByteArray();
    }

    /** A decoded picture brought down to the screen, letterboxed to keep its shape, 4 bits a colour. */
    static byte[] pack(Picture p) {
        byte[] out = new byte[W * H * 2];
        int sw = p.getCroppedWidth(), sh = p.getCroppedHeight(), sx0 = p.getStartX(), sy0 = p.getStartY();
        ColorSpace cs = p.getColor();
        boolean full = cs == ColorSpace.YUV420J || cs == ColorSpace.YUV422J || cs == ColorSpace.YUV444J;
        boolean rgb = cs == ColorSpace.RGB || cs == ColorSpace.BGR;
        byte[] y = p.getPlaneData(0);
        byte[] u = rgb ? null : p.getPlaneData(1), v = rgb ? null : p.getPlaneData(2);
        int yw = p.getPlaneWidth(0);
        int cw = rgb ? 0 : p.getPlaneWidth(1), chh = rgb ? 0 : p.getPlaneHeight(1);
        int ph = p.getPlaneHeight(0);
        double scale = Math.min(W / (double) sw, H / (double) sh);
        int dw = Math.max(1, (int) Math.round(sw * scale)), dh = Math.max(1, (int) Math.round(sh * scale));
        int ox = (W - dw) / 2, oy = (H - dh) / 2;
        for (int j = 0; j < dh; j++) {
            for (int i = 0; i < dw; i++) {
                // The average of a few points across the cell, so fine detail does not shimmer
                int r = 0, g = 0, b = 0, k = 0;
                for (int a = 0; a < 2; a++) {
                    for (int c = 0; c < 2; c++) {
                        int sx = sx0 + Math.min(sw - 1, (int) ((i + .25 + .5 * c) / scale));
                        int sy = sy0 + Math.min(sh - 1, (int) ((j + .25 + .5 * a) / scale));
                        int R, G, B;
                        if (rgb) {
                            int at = (sy * yw + sx) * 3;
                            R = (y[at] + 128);
                            G = (y[at + 1] + 128);
                            B = (y[at + 2] + 128);
                            if (cs == ColorSpace.BGR) {
                                int t = R;
                                R = B;
                                B = t;
                            }
                        } else {
                            double Y = y[Math.min(y.length - 1, sy * yw + sx)] + 128;
                            int cx = Math.min(cw - 1, sx * cw / yw), cy = Math.min(chh - 1, sy * chh / ph);
                            double U = u[cy * cw + cx], V = v[cy * cw + cx];
                            if (!full) {
                                Y = (Y - 16) * 1.164;
                                U *= 1.138;
                                V *= 1.138;
                            }
                            R = (int) (Y + 1.402 * V);
                            G = (int) (Y - .344 * U - .714 * V);
                            B = (int) (Y + 1.772 * U);
                        }
                        r += clamp(R);
                        g += clamp(G);
                        b += clamp(B);
                        k++;
                    }
                }
                int o = ((oy + j) * W + ox + i) * 2;
                int rr = r / k >> 4, gg = g / k >> 4, bb = b / k >> 4;
                out[o] = (byte) rr;
                out[o + 1] = (byte) (gg << 4 | bb);
            }
        }
        return out;
    }

    private static int clamp(int c) {
        return c < 0 ? 0 : Math.min(255, c);
    }

    /** How long the film's pictures last (seconds), 0 if unknown. */
    public static double duration(File mp4) {
        try (FileChannelWrapper ch = NIOUtils.readableChannel(mp4)) {
            DemuxerTrack v = MP4Demuxer.createMP4Demuxer(ch).getVideoTrack();
            return v == null ? 0 : v.getMeta().getTotalDuration();
        } catch (Exception e) {
            return 0;
        }
    }

    /** The film's sound (its first track; AAC), or null when it has none. */
    public static Sound sound(File mp4) throws Exception {
        try (FileChannelWrapper ch = NIOUtils.readableChannel(mp4)) {
            MP4Demuxer dm = MP4Demuxer.createMP4Demuxer(ch);
            List<DemuxerTrack> tracks = dm.getAudioTracks();
            if (tracks.isEmpty()) {
                return null;
            }
            DemuxerTrack t = tracks.get(0);
            AACDecoder dec = new AACDecoder(t.getMeta().getCodecPrivate());
            short[] pcm = new short[1 << 20];
            int n = 0, channels = 0, rate = 0;
            ByteBuffer buf = ByteBuffer.allocate(1 << 17);
            Packet pk;
            while ((pk = t.nextFrame()) != null) {
                buf.clear();
                AudioBuffer ab;
                try {
                    ab = dec.decodeFrame(pk.getData(), buf);
                } catch (Exception e) {
                    continue; // A damaged frame: a moment of silence
                }
                if (ab == null || ab.getData() == null) {
                    continue;
                }
                channels = ab.getFormat().getChannels();
                rate = ab.getFormat().getSampleRate();
                ByteBuffer d = ab.getData().duplicate().order(ab.getFormat().isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
                int bytes = ab.getFormat().getSampleSizeInBits() / 8;
                int samples = d.remaining() / Math.max(1, bytes);
                if (n + samples > pcm.length) {
                    pcm = java.util.Arrays.copyOf(pcm, Math.max(pcm.length * 2, n + samples));
                }
                for (int i = 0; i < samples; i++) {
                    if (bytes == 2) {
                        pcm[n++] = d.getShort();
                    } else if (bytes == 3) {
                        int hi = d.get(), mid = d.get() & 0xFF, lo = d.get() & 0xFF;
                        pcm[n++] = (short) (ab.getFormat().isBigEndian() ? (hi << 8 | mid) : ((lo << 8) | mid));
                    } else {
                        pcm[n++] = (short) (d.get() << 8);
                    }
                }
            }
            if (n == 0 || channels == 0) {
                return null;
            }
            return new Sound(java.util.Arrays.copyOf(pcm, n), channels, rate);
        }
    }
}
