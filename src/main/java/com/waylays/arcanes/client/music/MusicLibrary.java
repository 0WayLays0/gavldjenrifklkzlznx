package com.waylays.arcanes.client.music;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * The player's music: the files in the {@code arcanes_music} folder of the game (MP3, OGG and WAV, in subfolders too),
 * the tunes that come with the addon, the favourites, the playlists and the player's settings (kept in
 * {@code config/arcanes_oublies-music.json}).
 */
public final class MusicLibrary {
    /** The tunes that come with the addon (assets/arcanes_oublies/music). */
    private static final String[][] BUILT_IN = {{"valse_des_arcanes", "mp3", "Valse des Arcanes", "Arcanes Oublies"}};
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** A track: {@code id} (its path in the folder, or builtin:name), what it is called, who made it, its kind of file. */
    public static final class Track {
        public final String id;
        public final String title;
        @Nullable
        public final String artist;
        public final String ext;
        public final long size;
        @Nullable
        final Path file;
        @Nullable
        final ResourceLocation resource;
        /** How long it lasts, ms: 0 until it has been worked out. */
        public volatile int lengthMs;

        Track(String id, String title, @Nullable String artist, String ext, long size, @Nullable Path file, @Nullable ResourceLocation resource) {
            this.id = id;
            this.title = title;
            this.artist = artist;
            this.ext = ext;
            this.size = size;
            this.file = file;
            this.resource = resource;
        }

        public boolean builtIn() {
            return resource != null;
        }

        /** Reads the whole file. */
        public byte[] read() throws IOException {
            if (file != null) {
                return Files.readAllBytes(file);
            }
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(resource);
            if (res.isEmpty()) {
                throw new IOException("missing " + resource);
            }
            try (InputStream in = res.get().open()) {
                return in.readAllBytes();
            }
        }
    }

    public enum Repeat { OFF, ALL, ONE }

    private static final MusicLibrary INSTANCE = new MusicLibrary();

    private final List<Track> tracks = new ArrayList<>();
    private final Map<String, Track> byId = new ConcurrentHashMap<>();
    private final Set<String> favorites = new LinkedHashSet<>();
    private final Map<String, List<String>> playlists = new LinkedHashMap<>();
    public float volume = .8f;
    public int range = 24;
    public Repeat repeat = Repeat.ALL;
    public boolean shuffle;
    private boolean loaded;

    private MusicLibrary() {}

    public static MusicLibrary get() {
        if (!INSTANCE.loaded) {
            INSTANCE.loaded = true;
            INSTANCE.loadSettings();
            INSTANCE.scan();
        }
        return INSTANCE;
    }

    public static Path folder() {
        return FMLPaths.GAMEDIR.get().resolve("arcanes_music");
    }

    private static Path settingsFile() {
        return FMLPaths.CONFIGDIR.get().resolve("arcanes_oublies-music.json");
    }

    /** Opens the music folder in the system's file browser (made first if need be). */
    public static void openFolder() {
        try {
            Files.createDirectories(folder());
        } catch (IOException e) {
            ArcanesOublies.LOGGER.warn("Could not create the music folder", e);
        }
        Util.getPlatform().openFile(folder().toFile());
    }

    /** Looks through the music folder again. */
    public synchronized void scan() {
        tracks.clear();
        byId.clear();
        for (String[] b : BUILT_IN) {
            Track t = new Track("builtin:" + b[0], b[2], b[3], b[1], 0, null, ArcanesOublies.id("music/" + b[0] + "." + b[1]));
            add(t);
        }
        Path dir = folder();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            ArcanesOublies.LOGGER.warn("Could not create the music folder {}", dir, e);
        }
        List<Track> found = new ArrayList<>();
        try (Stream<Path> files = Files.walk(dir, 4)) {
            files.filter(Files::isRegularFile).forEach(p -> {
                String name = p.getFileName().toString();
                int dot = name.lastIndexOf('.');
                if (dot <= 0) {
                    return;
                }
                String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
                if (!MusicDecoders.supported(ext)) {
                    return;
                }
                String id = dir.relativize(p).toString().replace('\\', '/');
                MusicDecoders.Tags tags = MusicDecoders.Tags.NONE;
                long size = 0;
                try (InputStream in = Files.newInputStream(p)) {
                    size = Files.size(p);
                    tags = MusicDecoders.tags(in, ext);
                } catch (IOException e) {
                    ArcanesOublies.LOGGER.debug("Could not read the tags of {}", p, e);
                }
                String title = tags.title() != null ? tags.title() : name.substring(0, dot).replace('_', ' ');
                found.add(new Track(id, title, tags.artist(), ext, size, p, null));
            });
        } catch (IOException e) {
            ArcanesOublies.LOGGER.warn("Could not look through the music folder {}", dir, e);
        }
        found.sort(Comparator.comparing((Track t) -> t.title.toLowerCase(Locale.ROOT)));
        found.forEach(this::add);
        // Their lengths, worked out away from the game's thread
        List<Track> all = new ArrayList<>(tracks);
        CompletableFuture.runAsync(() -> {
            for (Track t : all) {
                if (t.lengthMs == 0) {
                    try {
                        t.lengthMs = MusicDecoders.lengthMs(t.read(), t.ext);
                    } catch (Exception | LinkageError e) {
                        ArcanesOublies.LOGGER.debug("Could not tell how long {} lasts", t.id, e);
                    }
                }
            }
        }, Util.backgroundExecutor());
    }

    private void add(Track t) {
        tracks.add(t);
        byId.put(t.id, t);
    }

    public synchronized List<Track> all() {
        return new ArrayList<>(tracks);
    }

    @Nullable
    public Track track(String id) {
        return byId.get(id);
    }

    public int fileCount() {
        int n = 0;
        for (Track t : all()) {
            if (!t.builtIn()) {
                n++;
            }
        }
        return n;
    }

    // ---------------------------------------------------------------- favourites and playlists

    public boolean favorite(Track t) {
        return favorites.contains(t.id);
    }

    public void toggleFavorite(Track t) {
        if (!favorites.remove(t.id)) {
            favorites.add(t.id);
        }
        save();
    }

    public List<Track> favorites() {
        List<Track> out = new ArrayList<>();
        for (String id : favorites) {
            Track t = byId.get(id);
            if (t != null) {
                out.add(t);
            }
        }
        return out;
    }

    public List<String> playlistNames() {
        return new ArrayList<>(playlists.keySet());
    }

    public List<Track> playlist(String name) {
        List<Track> out = new ArrayList<>();
        for (String id : playlists.getOrDefault(name, List.of())) {
            Track t = byId.get(id);
            if (t != null) {
                out.add(t);
            }
        }
        return out;
    }

    public int playlistSize(String name) {
        return playlist(name).size();
    }

    /** Makes a playlist (its name trimmed and made unique); returns the name it got. */
    public String createPlaylist(String name) {
        String base = name.trim().isEmpty() ? "Playlist" : name.trim();
        if (base.length() > 32) {
            base = base.substring(0, 32);
        }
        String n = base;
        for (int k = 2; playlists.containsKey(n); k++) {
            n = base + " " + k;
        }
        playlists.put(n, new ArrayList<>());
        save();
        return n;
    }

    public void deletePlaylist(String name) {
        playlists.remove(name);
        save();
    }

    public boolean inPlaylist(String name, Track t) {
        return playlists.getOrDefault(name, List.of()).contains(t.id);
    }

    public void addToPlaylist(String name, Track t) {
        List<String> ids = playlists.get(name);
        if (ids != null && !ids.contains(t.id)) {
            ids.add(t.id);
            save();
        }
    }

    public void removeFromPlaylist(String name, Track t) {
        List<String> ids = playlists.get(name);
        if (ids != null && ids.remove(t.id)) {
            save();
        }
    }

    /** Moves a track one place up ({@code by} -1) or down (+1) in a playlist. */
    public void move(String name, Track t, int by) {
        List<String> ids = playlists.get(name);
        if (ids == null) {
            return;
        }
        int i = ids.indexOf(t.id), j = i + by;
        if (i >= 0 && j >= 0 && j < ids.size()) {
            ids.set(i, ids.get(j));
            ids.set(j, t.id);
            save();
        }
    }

    // ---------------------------------------------------------------- settings

    private void loadSettings() {
        Path f = settingsFile();
        if (!Files.exists(f)) {
            return;
        }
        try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
            JsonObject o = GSON.fromJson(r, JsonObject.class);
            if (o == null) {
                return;
            }
            if (o.has("volume")) {
                volume = Math.max(0, Math.min(1, o.get("volume").getAsFloat()));
            }
            if (o.has("range")) {
                range = MusicSoundInstance.snap(o.get("range").getAsInt());
            }
            if (o.has("repeat")) {
                try {
                    repeat = Repeat.valueOf(o.get("repeat").getAsString());
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (o.has("shuffle")) {
                shuffle = o.get("shuffle").getAsBoolean();
            }
            if (o.has("favorites")) {
                for (JsonElement e : o.getAsJsonArray("favorites")) {
                    favorites.add(e.getAsString());
                }
            }
            if (o.has("playlists")) {
                JsonObject pl = o.getAsJsonObject("playlists");
                for (String name : pl.keySet()) {
                    List<String> ids = new ArrayList<>();
                    for (JsonElement e : pl.getAsJsonArray(name)) {
                        ids.add(e.getAsString());
                    }
                    playlists.put(name, ids);
                }
            }
        } catch (Exception e) {
            ArcanesOublies.LOGGER.warn("Could not read the music player's settings {}", f, e);
        }
    }

    public void save() {
        JsonObject o = new JsonObject();
        o.addProperty("volume", volume);
        o.addProperty("range", range);
        o.addProperty("repeat", repeat.name());
        o.addProperty("shuffle", shuffle);
        JsonArray fav = new JsonArray();
        favorites.forEach(fav::add);
        o.add("favorites", fav);
        JsonObject pl = new JsonObject();
        playlists.forEach((name, ids) -> {
            JsonArray a = new JsonArray();
            ids.forEach(a::add);
            pl.add(name, a);
        });
        o.add("playlists", pl);
        Path f = settingsFile();
        try {
            Files.createDirectories(f.getParent());
            try (Writer w = Files.newBufferedWriter(f, StandardCharsets.UTF_8)) {
                GSON.toJson(o, w);
            }
        } catch (IOException e) {
            ArcanesOublies.LOGGER.warn("Could not save the music player's settings {}", f, e);
        }
    }
}
