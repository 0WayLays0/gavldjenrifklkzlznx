package com.waylays.arcanes.spells.glyph;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/** The six elements a glyph can be written in, with their ink colours and the particles they shed. */
public enum GlyphElement {
    FIRE("fire", 0xFF7A2A, 0xFFE0A0),
    FROST("frost", 0x8FE6FF, 0xF0FCFF),
    STORM("storm", 0xFFE45C, 0xFFFDE8),
    TIDE("tide", 0x3F9BFF, 0xD2EBFF),
    GALE("gale", 0x9AF5C8, 0xF2FFF8),
    STONE("stone", 0xD08F52, 0xF6DEC0);

    public final String id;
    /** The ink's colour, and the white-hot colour of its core. */
    public final int color;
    public final int core;
    public final DustParticleOptions dust;

    GlyphElement(String id, int color, int core) {
        this.id = id;
        this.color = color;
        this.core = core;
        this.dust = new DustParticleOptions(new Vector3f(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f), 1.1f);
    }

    public static GlyphElement byId(int ordinal) {
        GlyphElement[] all = values();
        return all[Mth.clamp(ordinal, 0, all.length - 1)];
    }

    /** The particle the element sheds around what it touches. */
    public ParticleOptions particle() {
        return switch (this) {
            case FIRE -> ParticleTypes.FLAME;
            case FROST -> ParticleTypes.SNOWFLAKE;
            case STORM -> ParticleTypes.ELECTRIC_SPARK;
            case TIDE -> ParticleTypes.SPLASH;
            case GALE -> ParticleTypes.CLOUD;
            case STONE -> ParticleTypes.CRIT;
        };
    }
}
