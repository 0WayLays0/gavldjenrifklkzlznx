package com.waylays.arcanes.client.phone;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.waylays.arcanes.ArcanesOublies;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** The phone's own settings and notes, kept on this computer (config/arcanes_oublies-phone.json). */
public final class PhoneSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static PhoneSettings instance;

    public int wallpaper;
    public int finish;
    public boolean sounds = true;
    public boolean quiet;
    public List<String> notes = new ArrayList<>();
    public List<String> waypoints = new ArrayList<>();

    public static PhoneSettings get() {
        if (instance == null) {
            instance = new PhoneSettings();
            try {
                Path p = file();
                if (Files.exists(p)) {
                    PhoneSettings read = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), PhoneSettings.class);
                    if (read != null) {
                        instance = read;
                    }
                }
            } catch (Exception e) {
                ArcanesOublies.LOGGER.warn("Could not read the phone's settings", e);
            }
            if (instance.notes == null) {
                instance.notes = new ArrayList<>();
            }
            if (instance.waypoints == null) {
                instance.waypoints = new ArrayList<>();
            }
        }
        return instance;
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("arcanes_oublies-phone.json");
    }

    public void save() {
        try {
            Files.writeString(file(), GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException e) {
            ArcanesOublies.LOGGER.warn("Could not save the phone's settings", e);
        }
    }

    /** Where the photos are kept. */
    public static Path photos() {
        return FMLPaths.GAMEDIR.get().resolve("arcanes_phone").resolve("photos");
    }
}
