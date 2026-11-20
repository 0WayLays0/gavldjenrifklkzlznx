package com.waylays.arcanes;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server config (per world: {@code world/serverconfig/arcanes_oublies-server.toml}), for the staff.
 */
public final class ArcanesConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue EXPLOSIONS_BREAK_BLOCKS;
    public static final ModConfigSpec.IntValue BOSS_WARNING_TICKS;
    public static final ModConfigSpec.IntValue MIN_CAST_TICKS;

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
        builder.push("spells");
        MIN_CAST_TICKS = builder
                .comment("The shortest cast time of any spell a player casts, in ticks (20 = 1 s): Iron's spells, this mod's and every add-on's.",
                        "Instant spells are charged that long before they go off; charged spells shorter than that are drawn out to it.",
                        "Channelled spells and spells cast by mobs are left as they are. 0 turns it off.")
                .defineInRange("minCastTicks", 20, 0, 200);
        builder.pop();
        SPEC = builder.build();
    }

    private ArcanesConfig() {}

    public static int bossWarningTicks() {
        return SPEC.isLoaded() ? BOSS_WARNING_TICKS.get() : 70;
    }

    /** The shortest cast time of a player's spell, in ticks (see {@code spells.CastTimeFloor}). */
    public static int minCastTicks() {
        return SPEC.isLoaded() ? MIN_CAST_TICKS.get() : 20;
    }

    public static boolean explosionsBreakBlocks() {
        return SPEC.isLoaded() && EXPLOSIONS_BREAK_BLOCKS.get();
    }
}
