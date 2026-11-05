package com.waylays.arcanes.client.music;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.network.MusicPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Adds music from a link straight to an audio file (an address ending in .mp3, .ogg or .wav, or one a server answers with
 * such a file): it is downloaded into the music folder and then plays like any other. Video pages (YouTube, TikTok...)
 * are not audio files and are refused.
 */
public final class MusicDownloader {
    private static final HttpClient CLIENT = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(10)).build();
    private static volatile boolean busy;

    private MusicDownloader() {}

    public static boolean isLink(String s) {
        String t = s.trim().toLowerCase(Locale.ROOT);
        return t.startsWith("http://") || t.startsWith("https://");
    }

    public static boolean busy() {
        return busy;
    }

    /** Downloads {@code link} in the background, then refreshes the library and plays it. */
    public static void download(String link) {
        if (busy) {
            return;
        }
        busy = true;
        say(Component.translatable("ui.arcanes_oublies.music.downloading"));
        CompletableFuture.runAsync(() -> {
            try {
                URI uri = URI.create(link.trim());
                HttpResponse<InputStream> r = CLIENT.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(60))
                        .header("User-Agent", "ArcanesOublies-MusicPlayer").GET().build(), HttpResponse.BodyHandlers.ofInputStream());
                if (r.statusCode() / 100 != 2) {
                    throw new IllegalStateException("HTTP " + r.statusCode());
                }
                String type = r.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT);
                String path = uri.getPath() == null ? "" : uri.getPath();
                String ext = ext(path, type);
                if (ext == null) {
                    say(Component.translatable("ui.arcanes_oublies.music.not_audio"));
                    return;
                }
                byte[] data;
                try (InputStream in = r.body(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] buf = new byte[65536];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        out.write(buf, 0, n);
                        if (out.size() > MusicPackets.MAX_FILE) {
                            say(Component.translatable("ui.arcanes_oublies.music.too_big", MusicPackets.MAX_FILE / (1024 * 1024)));
                            return;
                        }
                    }
                    data = out.toByteArray();
                }
                String name = path.substring(path.lastIndexOf('/') + 1).replaceAll("\\.[A-Za-z0-9]{2,4}$", "").replaceAll("[^A-Za-z0-9 _.-]", "_");
                if (name.isBlank()) {
                    name = "link_" + Integer.toHexString(link.hashCode());
                }
                Path folder = MusicLibrary.folder();
                Files.createDirectories(folder);
                Path file = folder.resolve(name + "." + ext);
                Files.write(file, data);
                Minecraft.getInstance().execute(() -> {
                    MusicLibrary lib = MusicLibrary.get();
                    lib.scan();
                    say(Component.translatable("ui.arcanes_oublies.music.downloaded", file.getFileName().toString()));
                });
            } catch (Exception e) {
                ArcanesOublies.LOGGER.warn("Could not download music from {}", link, e);
                say(Component.translatable("ui.arcanes_oublies.music.download_failed", String.valueOf(e.getMessage())));
            } finally {
                busy = false;
            }
        });
    }

    /** The file's kind, from its name or the type the server gave; null if it is no audio file the player reads. */
    private static String ext(String path, String type) {
        String p = path.toLowerCase(Locale.ROOT);
        for (String e : new String[] {"mp3", "ogg", "wav"}) {
            if (p.endsWith("." + e)) {
                return e;
            }
        }
        if (type.contains("audio/mpeg") || type.contains("audio/mp3")) {
            return "mp3";
        }
        if (type.contains("ogg")) {
            return "ogg";
        }
        if (type.contains("wav")) {
            return "wav";
        }
        return null;
    }

    private static void say(Component c) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null) {
                mc.player.displayClientMessage(c, true);
            }
        });
    }
}
