package com.waylays.arcanes;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server config (per world: {@code world/serverconfig/arcanes_oublies-server.toml}), for the staff.
 */
public final class ArcanesConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue EXPLOSIONS_BREAK_BLOCKS;
    public static final ModConfigSpec.IntValue BOSS_WARNING_TICKS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("explosion");
        EXPLOSIONS_BREAK_BLOCKS = builder
                .comment("Do Explosion school spells break blocks? Off by default (RP server): they only hurt enemies.")
                .define("explosionsBreakBlocks", false);
        builder.pop();
        builder.push("boss");
        BOSS_WARNING_TICKS = builder
                .comment("How long the warning runes of a Boss Magic attack stay on the ground before it lands, in ticks (20 = 1 s).",
                        "3 to 4 s (60 to 80) leaves players time to see the zone and get out of it.")
                .defineInRange("warningTicks", 70, 40, 200);
        builder.pop();
        SPEC = builder.build();
    }

    private ArcanesConfig() {}

    public static int bossWarningTicks() {
        return SPEC.isLoaded() ? BOSS_WARNING_TICKS.get() : 70;
    }

    public static boolean explosionsBreakBlocks() {
        return SPEC.isLoaded() && EXPLOSIONS_BREAK_BLOCKS.get();
    }
}
