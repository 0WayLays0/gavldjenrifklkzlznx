package com.waylays.eraphones;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The server's settings (config/serverconfig): who may use the game masters' commands of this mod, and whether the
 * two ends of a phone call hear each other's voice. The role play itself (/setname, /me, /horreur...) is in Arcanes
 * Oubliés now; the section and the names are those of before, so a server keeps what it had set.
 */
public final class EraConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue STAFF_PERMISSION;
    public static final ModConfigSpec.BooleanValue PHONE_VOICE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("roleplay");
        PHONE_VOICE = b.comment("With Simple Voice Chat: whether the two ends of a phone call hear each other's voice").define("phoneVoice", true);
        STAFF_PERMISSION = b.comment("The permission level needed for the game masters' commands (/panne, /lampe, /cassette graver, /radio emettre...)")
                .defineInRange("horreurPermission", 2, 0, 4);
        b.pop();
        SPEC = b.build();
    }

    private EraConfig() {}

    public static int staffPermission() {
        try {
            return STAFF_PERMISSION.get();
        } catch (IllegalStateException e) {
            return 2;
        }
    }

    public static boolean phoneVoice() {
        try {
            return PHONE_VOICE.get();
        } catch (IllegalStateException e) {
            return true;
        }
    }

    /** What a player is called: their character's name when Arcanes Oubliés gives them one, else their own. */
    public static String name(ServerPlayer p) {
        return p.getDisplayName().getString();
    }
}
