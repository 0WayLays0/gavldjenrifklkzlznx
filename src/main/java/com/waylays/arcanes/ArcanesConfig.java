package com.waylays.arcanes;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Server config (per world: {@code world/serverconfig/arcanes_oublies-server.toml}), for the staff.
 */
public final class ArcanesConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue EXPLOSIONS_BREAK_BLOCKS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("explosion");
        EXPLOSIONS_BREAK_BLOCKS = builder
                .comment("Do Explosion school spells break blocks? Off by default (RP server): they only hurt enemies.")
                .define("explosionsBreakBlocks", false);
        builder.pop();
        SPEC = builder.build();
    }

    private ArcanesConfig() {}

    public static boolean explosionsBreakBlocks() {
        return SPEC.isLoaded() && EXPLOSIONS_BREAK_BLOCKS.get();
    }
}
