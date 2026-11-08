package com.waylays.eraphones.gear;

import com.waylays.eraphones.EraPhones;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The films on the video cassettes (MP4 files in the cassettes folder): their pictures prepared once in the
 * background into a file beside them (cassettes/.tv/), then read a frame at a time as a tape plays on a television.
 */
public final class Video {
    public enum State { LOADING, READY, FAILED }

    private static final Map<String, State> STATE = new ConcurrentHashMap<>();
    private static final Map<String, RandomAccessFile> OPEN = new ConcurrentHashMap<>();
    private static final Map<String, Integer> COUNT = new ConcurrentHashMap<>();

    private Video() {}

    public static boolean isVideo(@Nullable String file) {
        return file != null && file.toLowerCase().endsWith(".mp4");
    }

    private static Path cache(String file) {
        return Tape.folder().resolve(".tv").resolve(file + ".tvx");
    }

    /** Where the film {@code file} stands; starts preparing it if no one has yet. */
    public static State prepare(String file) {
        State s = STATE.get(file);
        if (s != null) {
            return s;
        }
        Path src = Tape.folder().resolve(file).normalize();
        Path out = cache(file);
        try {
            if (Files.isRegularFile(out) && Files.isRegularFile(src) && Files.getLastModifiedTime(out).compareTo(Files.getLastModifiedTime(src)) >= 0) {
                STATE.put(file, State.READY);
                return State.READY;
            }
        } catch (IOException ignored) {
            // Prepared again below
        }
        if (STATE.putIfAbsent(file, State.LOADING) != null) {
            return STATE.get(file);
        }
        Thread t = new Thread(() -> {
            try {
                if (!src.startsWith(Tape.folder()) || !Files.isRegularFile(src)) {
                    throw new IOException("no such film");
                }
                Files.createDirectories(out.getParent());
                Path part = out.resolveSibling(out.getFileName() + ".part");
                long t0 = System.currentTimeMillis();
                int n = VideoDecode.pictures(src.toFile(), part.toFile());
                Files.move(part, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                EraPhones.LOGGER.info("Film {} prepared: {} frames in {} s", file, n, (System.currentTimeMillis() - t0) / 1000);
                STATE.put(file, State.READY);
            } catch (Throwable e) {
                EraPhones.LOGGER.warn("The film {} cannot be read: {}", file, e.toString());
                STATE.put(file, State.FAILED);
            }
        }, "film-" + file);
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
        return State.LOADING;
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
    }
}
