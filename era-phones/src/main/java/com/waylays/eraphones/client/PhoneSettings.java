package com.waylays.eraphones.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.waylays.eraphones.EraPhones;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What a phone keeps on this computer, by number (config/eraphones.json): its wallpaper and finish, its sounds, its
 * notes and marked places, its best game score, its screen's colours; its photos are in eraphones/photos/number.
 */
public final class PhoneSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Store store;

    private static final class Store {
        Map<String, PhoneSettings> lines = new HashMap<>();
    }

    public int wallpaper;
    public int finish;
    public boolean sounds = true;
    public List<String> notes = new ArrayList<>();
    public List<String> waypoints = new ArrayList<>();
    public int snakeBest;
    /** The flip phone's colour theme, the GSM screen's contrast. */
    public int theme;
    public int contrast = 2;

    private static Store store() {
        if (store == null) {
            store = new Store();
            try {
                Path p = file();
                if (Files.exists(p)) {
                    Store read = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), Store.class);
                    if (read != null && read.lines != null) {
                        store = read;
                    }
                }
            } catch (Exception e) {
                EraPhones.LOGGER.warn("Could not read the phones' settings", e);
            }
        }
        return store;
    }

    /** The settings of the phone in use. */
    public static PhoneSettings get() {
        return of(PhoneClient.number);
    }

    public static PhoneSettings of(String number) {
        PhoneSettings s = store().lines.computeIfAbsent(number == null ? "" : number, k -> new PhoneSettings());
        if (s.notes == null) {
            s.notes = new ArrayList<>();
        }
        if (s.waypoints == null) {
            s.waypoints = new ArrayList<>();
        }
        return s;
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("eraphones.json");
    }

    public void save() {
        try {
            Files.writeString(file(), GSON.toJson(store()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            EraPhones.LOGGER.warn("Could not save the phones' settings", e);
        }
    }

    /** Where the photos of the phone in use are kept. */
    public static Path photos() {
        String n = PhoneClient.number == null || PhoneClient.number.isEmpty() ? "sans_numero" : PhoneClient.number.replaceAll("[^0-9A-Za-z_-]", "_");
        return FMLPaths.GAMEDIR.get().resolve("eraphones").resolve("photos").resolve(n);
    }
}
