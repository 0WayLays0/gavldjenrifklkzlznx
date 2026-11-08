package com.waylays.eraphones.gear;

import com.waylays.eraphones.EraPhones;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * The films on the video cassettes (MP4 files in the cassettes folder), prepared once in the background: a film in a
 * format the built-in reader does not know (H.265/HEVC from phones, VP9, AV1...) is first converted with ffmpeg when the
 * server has it; then its sound is read (silence if it cannot be), and its pictures are kept in a file beside it
 * (cassettes/.tv/), read a frame at a time as a tape plays on a television. Its sound is given out only once its
 * pictures are ready, so the tape does not run ahead of them.
 */
public final class Video {
    public enum State { LOADING, READY, FAILED }

    private static final Map<String, State> STATE = new ConcurrentHashMap<>();
    private static final Map<String, Integer> PROGRESS = new ConcurrentHashMap<>();
    private static final Map<String, String> WHY = new ConcurrentHashMap<>();
    private static final Map<String, short[]> SOUND = new ConcurrentHashMap<>();
    private static final Map<String, RandomAccessFile> OPEN = new ConcurrentHashMap<>();
    private static final Map<String, Integer> COUNT = new ConcurrentHashMap<>();

    private Video() {}

    public static boolean isVideo(@Nullable String file) {
        return file != null && file.toLowerCase().endsWith(".mp4");
    }

    private static Path cache(String file) {
        return Tape.folder().resolve(".tv").resolve(file + ".tvx");
    }

    /** How far the preparing is (0 to 100). */
    public static int progress(String file) {
        return PROGRESS.getOrDefault(file, 0);
    }

    /** Why a film could not be prepared (a format's name, or "?"). */
    public static String why(String file) {
        return WHY.getOrDefault(file, "?");
    }

    /** The film's sound at 48 kHz, once its pictures are ready too; null until then. */
    @Nullable
    public static short[] sound(String file) {
        return prepare(file) == State.READY ? SOUND.get(file) : null;
    }

    /** Where the film {@code file} stands; starts preparing it if no one has yet. */
    public static State prepare(String file) {
        State s = STATE.get(file);
        if (s != null) {
            return s;
        }
        if (STATE.putIfAbsent(file, State.LOADING) != null) {
            return STATE.get(file);
        }
        Thread t = new Thread(() -> {
            Path src = Tape.folder().resolve(file).normalize();
            try {
                if (!src.startsWith(Tape.folder()) || !Files.isRegularFile(src)) {
                    throw new IOException("no such film");
                }
                Path out = cache(file);
                Files.createDirectories(out.getParent());
                File use = src.toFile();
                String codec = VideoDecode.codec(use);
                if (!"H264".equals(codec)) {
                    Path conv = out.resolveSibling(file + ".h264.mp4");
                    if (!(Files.isRegularFile(conv) && Files.getLastModifiedTime(conv).compareTo(Files.getLastModifiedTime(src)) >= 0)
                            && !convert(src, conv)) {
                        WHY.put(file, "?".equals(codec) ? "HEVC/VP9/AV1" : codec);
                        throw new IOException("format " + codec + " not readable, and no ffmpeg to convert it");
                    }
                    use = conv.toFile();
                }
                // Its sound: read if it can be, else silence as long as its pictures
                short[] pcm = null;
                try {
                    VideoDecode.Sound snd = VideoDecode.sound(use);
                    if (snd != null) {
                        pcm = Tape.resample(snd.pcm(), snd.channels(), snd.rate());
                    }
                } catch (Throwable e) {
                    EraPhones.LOGGER.warn("The sound of the film {} cannot be read ({}): it will be silent", file, e.toString());
                }
                if (pcm == null) {
                    double seconds = VideoDecode.duration(use);
                    pcm = new short[(int) Math.min(Integer.MAX_VALUE - 8, Math.max(1, seconds > 0 ? seconds : 600) * Audio.RATE)];
                }
                SOUND.put(file, pcm);
                boolean fresh = Files.isRegularFile(out) && Files.getLastModifiedTime(out).compareTo(Files.getLastModifiedTime(src)) >= 0;
                if (!fresh) {
                    Path part = out.resolveSibling(out.getFileName() + ".part");
                    long t0 = System.currentTimeMillis();
                    int n = VideoDecode.pictures(use, part.toFile(), pct -> PROGRESS.put(file, pct));
                    Files.move(part, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    EraPhones.LOGGER.info("Film {} prepared: {} frames in {} s", file, n, (System.currentTimeMillis() - t0) / 1000);
                }
                PROGRESS.put(file, 100);
                STATE.put(file, State.READY);
            } catch (Throwable e) {
                EraPhones.LOGGER.warn("The film {} cannot be read: {}", file, e.toString());
                WHY.putIfAbsent(file, "?");
                STATE.put(file, State.FAILED);
            }
        }, "film-" + file);
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
        return State.LOADING;
    }

    /**
     * Converts {@code src} to a small H.264 film the reader knows, with ffmpeg if the server has it (on its PATH);
     * false when there is no ffmpeg or it failed.
     */
    private static boolean convert(Path src, Path dst) {
        try {
            Path part = dst.resolveSibling(dst.getFileName() + ".part.mp4");
            Process p = new ProcessBuilder("ffmpeg", "-y", "-loglevel", "error", "-i", src.toString(), "-vf", "scale=320:-2", "-r", "15",
                    "-c:v", "libx264", "-preset", "veryfast", "-pix_fmt", "yuv420p", "-c:a", "aac", "-ac", "1", "-b:a", "96k", part.toString())
                    .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            if (!p.waitFor(2, TimeUnit.HOURS) || p.exitValue() != 0) {
                p.destroyForcibly();
                return false;
            }
            Files.move(part, dst, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            return false; // No ffmpeg here
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** Frame {@code index} of a prepared film, squeezed as kept; null when not ready or past the end. */
    @Nullable
    public static synchronized byte[] frame(String file, int index) {
        if (prepare(file) != State.READY) {
            return null;
        }
        try {
            RandomAccessFile f = OPEN.get(file);
            if (f == null) {
                f = new RandomAccessFile(cache(file).toFile(), "r");
                OPEN.put(file, f);
                COUNT.put(file, VideoDecode.count(f));
            }
            return VideoDecode.frame(f, Math.min(index, COUNT.getOrDefault(file, 1) - 1));
        } catch (IOException e) {
            EraPhones.LOGGER.warn("Cannot read the film {}: {}", file, e.toString());
            return null;
        }
    }

    /** The frame showing when the tape is at {@code samples}. */
    public static int index(long samples) {
        return (int) (Math.max(0, samples) * VideoDecode.FPS / Audio.RATE);
    }

    /** Lets go of the files when the server stops. */
    public static synchronized void close() {
        for (RandomAccessFile f : OPEN.values()) {
            try {
                f.close();
            } catch (IOException ignored) {
                // Closing anyway
            }
        }
        OPEN.clear();
        COUNT.clear();
        STATE.clear();
        SOUND.clear();
        PROGRESS.clear();
        WHY.clear();
    }
}
