package com.waylays.arcanes.rp;

import net.neoforged.neoforge.common.ModConfigSpec;

/** The role-play settings of the server (config/serverconfig, sent to the players): how far each way of speaking carries. */
public final class RpConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue LOCAL_CHAT;
    public static final ModConfigSpec.IntValue SAY_RANGE, ACTION_RANGE, WHISPER_RANGE, SHOUT_RANGE, HIDE_PERMISSION, HORROR_PERMISSION, VOICE_PERMISSION;
    public static final ModConfigSpec.BooleanValue NAMEPLATES, BUBBLES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("roleplay");
        LOCAL_CHAT = b.comment("The chat is speech: heard only around the one who speaks, under their character's name. /hrp stays global.")
                .define("localChat", true);
        SAY_RANGE = b.comment("How far speech carries (blocks)").defineInRange("sayRange", 20, 2, 200);
        ACTION_RANGE = b.comment("How far /me, /do, /roll and /try are seen (blocks)").defineInRange("actionRange", 20, 2, 200);
        WHISPER_RANGE = b.comment("How far a whisper is heard (blocks)").defineInRange("whisperRange", 4, 1, 20);
        SHOUT_RANGE = b.comment("How far a shout carries (blocks)").defineInRange("shoutRange", 48, 10, 400);
        HIDE_PERMISSION = b.comment("The permission level needed for /voir (2: game masters)").defineInRange("voirPermission", 2, 0, 4);
        NAMEPLATES = b.comment("Whether the character's name shows over their head").define("nameplates", true);
        BUBBLES = b.comment("Whether what is said and done also shows in a bubble over the head (off: in the chat only)").define("bubbles", false);
        HORROR_PERMISSION = b.comment("The permission level needed for /horreur (sounds, visions, fog, a red balloon...)").defineInRange("horreurPermission", 2, 0, 4);
        VOICE_PERMISSION = b.comment("The permission level needed for /voix (with Simple Voice Chat: a changed voice)").defineInRange("voixPermission", 2, 0, 4);
        b.pop();
        SPEC = b.build();
    }

    private RpConfig() {}

    public static int get(ModConfigSpec.IntValue v, int fallback) {
        try {
            return v.get();
        } catch (IllegalStateException e) {
            return fallback;
        }
    }

    public static boolean get(ModConfigSpec.BooleanValue v, boolean fallback) {
        try {
            return v.get();
        } catch (IllegalStateException e) {
            return fallback;
        }
    }
}
