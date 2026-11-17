package com.waylays.arcanes.rp;

import net.neoforged.neoforge.common.ModConfigSpec;

/** The role-play settings of the server (config/serverconfig, sent to the players): how far each way of speaking carries. */
public final class RpConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue LOCAL_CHAT;
    public static final ModConfigSpec.IntValue SAY_RANGE, ACTION_RANGE, WHISPER_RANGE, SHOUT_RANGE, HIDE_PERMISSION, HORROR_PERMISSION, VOICE_PERMISSION;
    public static final ModConfigSpec.IntValue ME_RANGE, MAX_RANGE, WARP_PERMISSION, SETWARP_PERMISSION, INVSEE_PERMISSION, REPAIR_PERMISSION,
            BACK_PERMISSION;
    public static final ModConfigSpec.BooleanValue NAMEPLATES, BUBBLES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("roleplay");
        LOCAL_CHAT = b.comment("The chat is speech: heard only around the one who speaks, under their character's name. /hrp stays global.")
                .define("localChat", true);
        SAY_RANGE = b.comment("How far speech carries (blocks)").defineInRange("sayRange", 20, 2, 200);
        ACTION_RANGE = b.comment("How far /do, /roll and /try are seen (blocks)").defineInRange("actionRange", 20, 2, 200);
        ME_RANGE = b.comment("How far /me is seen (blocks)").defineInRange("meRange", 25, 2, 200);
        MAX_RANGE = b.comment("The furthest a player may carry /me, /do, /try, /roll, /chuchoter, /crier or /hrp by giving a range (blocks)")
                .defineInRange("maxRange", 200, 2, 10000);
        WHISPER_RANGE = b.comment("How far a whisper is heard (blocks)").defineInRange("whisperRange", 4, 1, 20);
        SHOUT_RANGE = b.comment("How far a shout carries (blocks)").defineInRange("shoutRange", 48, 10, 400);
        HIDE_PERMISSION = b.comment("The permission level needed for /voir (2: game masters)").defineInRange("voirPermission", 2, 0, 4);
        NAMEPLATES = b.comment("Whether the character's name shows over their head").define("nameplates", true);
        BUBBLES = b.comment("Whether what is said and done also shows in a bubble over the head (off: in the chat only)").define("bubbles", false);
        HORROR_PERMISSION = b.comment("The permission level needed for /horreur (sounds, visions, fog, a red balloon...)").defineInRange("horreurPermission", 2, 0, 4);
        WARP_PERMISSION = b.comment("The permission level needed for /warp and /warplist (0: everyone)").defineInRange("warpPermission", 0, 0, 4);
        SETWARP_PERMISSION = b.comment("The permission level needed for /setwarp and /delwarp").defineInRange("setwarpPermission", 2, 0, 4);
        BACK_PERMISSION = b.comment("The permission level needed for /back").defineInRange("backPermission", 2, 0, 4);
        INVSEE_PERMISSION = b.comment("The permission level needed for /invsee").defineInRange("invseePermission", 2, 0, 4);
        REPAIR_PERMISSION = b.comment("The permission level needed for /repair").defineInRange("repairPermission", 2, 0, 4);
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
