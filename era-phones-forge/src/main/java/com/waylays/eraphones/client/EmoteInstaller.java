package com.waylays.eraphones.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.waylays.eraphones.EraPhones;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * The clown emotes for Emotecraft, carried in the jar: on a client with Emotecraft, they are put in the game's
 * emotes folder (an older copy replaced), before Emotecraft reads it. An emote of this mod found there under another
 * file name, from an older version, has its name made safe too: Emote Tweaks makes a file name of an emote's name, and
 * Windows refuses a colon in it (the game crashed when the emote was played).
 */
public final class EmoteInstaller {
    private static final String[] EMOTES = {"clown_assis", "clown_assis_rebord", "clown_coucou", "clown_danse", "clown_marionnette",
            "clown_reverence", "clown_rire", "clown_sautille", "clown_tend_objet", "clown_tete_penchee", "clown_viens"};
    private static final String AUTHOR = "Époque RP";

    private EmoteInstaller() {}

    public static void install() {
        try {
            Path dir = FMLPaths.GAMEDIR.get().resolve("emotes");
            if (!ModList.get().isLoaded("emotecraft") && !Files.isDirectory(dir)) {
                return;
            }
            Files.createDirectories(dir);
            int written = 0;
            for (String name : EMOTES) {
                for (String ext : new String[] {".json", ".png"}) {
                    if (copy("/assets/eraphones/emotes/" + name + ext, dir.resolve(name + ext))) {
                        written++;
                    }
                }
            }
            int fixed = fixOld(dir);
            if (written > 0 || fixed > 0) {
                EraPhones.LOGGER.info("Clown emotes: {} file(s) put in {}, {} old one(s) given a safe name", written, dir, fixed);
            }
        } catch (Throwable t) {
            EraPhones.LOGGER.warn("The clown emotes could not be put in the emotes folder: {}", t.toString());
        }
    }

    /** Copies a resource of the jar to {@code to} unless it is there already as it is; whether it wrote it. */
    private static boolean copy(String resource, Path to) throws IOException {
        byte[] data;
        try (InputStream in = EmoteInstaller.class.getResourceAsStream(resource)) {
            if (in == null) {
                return false;
            }
            data = in.readAllBytes();
        }
        if (Files.exists(to) && Arrays.equals(Files.readAllBytes(to), data)) {
            return false;
        }
        Files.write(to, data);
        return true;
    }

    /** Emotes of this mod with a name Windows cannot make a file of: the name made safe. */
    private static int fixOld(Path dir) throws IOException {
        int fixed = 0;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "*.json")) {
            for (Path f : files) {
                try {
                    JsonElement root = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8));
                    if (!root.isJsonObject()) {
                        continue;
                    }
                    JsonObject o = root.getAsJsonObject();
                    if (!o.has("author") || !AUTHOR.equals(o.get("author").getAsString()) || !o.has("name")) {
                        continue;
                    }
                    String name = o.get("name").getAsString();
                    String safe = safe(name);
                    if (!safe.equals(name)) {
                        o.addProperty("name", safe);
                        Files.writeString(f, o.toString() + "\n", StandardCharsets.UTF_8);
                        fixed++;
                    }
                } catch (RuntimeException | IOException e) {
                    // Not an emote of this mod, or not readable: left as it is
                }
            }
        }
        return fixed;
    }

    /** A name that can also be a file's name on Windows. */
    static String safe(String name) {
        String s = name.replace(" : ", " - ").replaceAll("[<>:\"/\\\\|?*]", "").replace("…", "");
        while (s.endsWith(" ") || s.endsWith(".")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
