package com.waylays.eraphones.rp;

import net.minecraftforge.common.ForgeConfigSpec;

/** The role-play settings of the server (config/serverconfig, sent to the players): how far each way of speaking carries. */
public final class RpConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue LOCAL_CHAT;
    public static final ForgeConfigSpec.IntValue SAY_RANGE, ACTION_RANGE, WHISPER_RANGE, SHOUT_RANGE, HIDE_PERMISSION, HORROR_PERMISSION, VOICE_PERMISSION, HOTBAR_SLOTS;
    public static final ForgeConfigSpec.BooleanValue NAMEPLATES, BUBBLES, PHONE_VOICE, LIMITED_INVENTORY;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
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
        PHONE_VOICE = b.comment("With Simple Voice Chat: whether the two ends of a phone call hear each other's voice").define("phoneVoice", true);
        HORROR_PERMISSION = b.comment("The permission level needed for /horreur (sounds, visions, fog, a red balloon...)").defineInRange("horreurPermission", 2, 0, 4);
        VOICE_PERMISSION = b.comment("The permission level needed for /voix (with Simple Voice Chat: a changed voice)").defineInRange("voixPermission", 2, 0, 4);
        LIMITED_INVENTORY = b.comment("Whether players (not in creative or spectator) carry only the first hotbar slots, and a backpack")
                .define("limitedInventory", true);
        HOTBAR_SLOTS = b.comment("How many hotbar slots players may use when the inventory is limited").defineInRange("hotbarSlots", 5, 1, 9);
        b.pop();
        SPEC = b.build();
    }

    private RpConfig() {}

    public static int get(ForgeConfigSpec.IntValue v, int fallback) {
        try {
            return v.get();
        } catch (IllegalStateException e) {
            return fallback;
        }
    }

    public static boolean get(ForgeConfigSpec.BooleanValue v, boolean fallback) {
        try {
            return v.get();
        } catch (IllegalStateException e) {
            return fallback;
        }
    }
}
