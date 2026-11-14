package com.waylays.arcanes.rp.horror;

import net.neoforged.fml.ModList;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Who speaks with which voice (see {@link VoiceFx}), and who hears their own voice changed (to try it). Read from Simple
 * Voice Chat's thread, set by the commands: kept in maps safe for both.
 */
public final class Voices {
    private static final Map<UUID, String> VOICE = new ConcurrentHashMap<>();
    private static final Set<UUID> LISTENING = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Integer> STRENGTH = new ConcurrentHashMap<>();

    /** How strong {@code player}'s voice is put on, 10 to 100 percent (the rest their own). */
    public static int strength(UUID player) {
        return STRENGTH.getOrDefault(player, 100);
    }

    public static void setStrength(UUID player, int percent) {
        STRENGTH.put(player, Math.max(10, Math.min(100, percent)));
    }

    private Voices() {}

    /** Whether Simple Voice Chat is there to carry the voices. */
    public static boolean available() {
        return ModList.get() != null && ModList.get().isLoaded("voicechat");
    }

    /** {@code player}'s voice, or null for their own. */
    public static String of(UUID player) {
        return VOICE.get(player);
    }

    /** Gives {@code player} the voice {@code voice} (null: their own again). */
    public static void set(UUID player, String voice) {
        if (voice == null) {
            VOICE.remove(player);
        } else {
            VOICE.put(player, voice);
        }
    }

    public static boolean listening(UUID player) {
        return LISTENING.contains(player);
    }

    /** Toggles whether {@code player} hears their own voice as the others do; true if they now do. */
    public static boolean toggleListening(UUID player) {
        if (LISTENING.remove(player)) {
            return false;
        }
        LISTENING.add(player);
        return true;
    }
}
