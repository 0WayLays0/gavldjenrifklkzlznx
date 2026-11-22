package com.waylays.cinematic.studio;

import net.minecraftforge.common.ForgeConfigSpec;

/** Per-world settings (serverconfig/cinematic-server.toml). */
public final class StudioConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue PERMISSION;
    public static final ForgeConfigSpec.IntValue MAX_TAKE_SECONDS;
    public static final ForgeConfigSpec.IntValue MAX_SKIN_PIXELS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        PERMISSION = b.comment("Permission level needed to direct (create actors, record, play scenes). The owner of a solo world always can.")
                .defineInRange("directorPermissionLevel", 2, 0, 4);
        MAX_TAKE_SECONDS = b.comment("Longest take, in seconds").defineInRange("maxTakeSeconds", 600, 10, 3600);
        MAX_SKIN_PIXELS = b.comment("Largest skin side, in pixels (64 to 4096)").defineInRange("maxSkinSize", 2048, 64, 4096);
        SPEC = b.build();
    }

    private StudioConfig() {}
}
