package com.waylays.arcanes.spells.gem;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * The five gems of Gem Magic, each with its own colour and nature: the ruby burns (focused light), the lapis seals
 * (the night sky and its golden stars), the diamond refracts and endures, the amethyst resonates, the emerald heals.
 */
public enum Gem {
    RUBY(0xE01848, 0xFF8A9E),
    LAPIS(0x2450C8, 0xF2C33C),
    DIAMOND(0x9FEAFF, 0xFFFFFF),
    AMETHYST(0xA35BF0, 0xE2C6FF),
    EMERALD(0x1FCB6E, 0xB6FFD2);

    /** Its colour, and the bright tone of its light (the lapis's is the gold of its flecks). */
    public final int color;
    public final int light;
    public final DustParticleOptions dust;
    public final DustParticleOptions glint;

    Gem(int color, int light) {
        this.color = color;
        this.light = light;
        this.dust = new DustParticleOptions(rgb(color), 1.1f);
        this.glint = new DustParticleOptions(rgb(light), .7f);
    }

    private static Vector3f rgb(int c) {
        return new Vector3f(((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f);
    }

    public static Gem of(int ordinal) {
        Gem[] all = values();
        return all[Mth.clamp(ordinal, 0, all.length - 1)];
    }
}
