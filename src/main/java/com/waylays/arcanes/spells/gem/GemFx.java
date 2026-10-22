package com.waylays.arcanes.spells.gem;

import com.waylays.arcanes.entity.gem.GemShatter;
import com.waylays.arcanes.entity.gem.GemSigil;
import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Shared look and sound of Gem Magic. Every spell is called with the jeweller's sigil burning on the ground in its
 * gem's colour (the plan of a cut stone seen from above); gems ring like crystal and break into shards of light.
 */
public final class GemFx {
    private GemFx() {}

    /** The jeweller's sigil on the ground at {@code at}, {@code radius} wide, for {@code life} ticks: the incantation. */
    public static void sigil(Level level, Vec3 at, Gem gem, float radius, int life) {
        if (!level.isClientSide) {
            GemSigil.burn(level, at, gem, radius, life);
        }
    }

    /** A gem breaking: shards of it flying out, glitter and the ring of breaking crystal. */
    public static void shatter(Level level, Vec3 at, Gem gem, float size, int shards) {
        if (!level.isClientSide) {
            GemShatter.burst(level, at, gem, size, shards);
            glitter(level, at, gem, (int) (10 * size) + 6, size * .4);
            sound(level, at, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.2f, .8f + level.random.nextFloat() * .3f);
            sound(level, at, SoundEvents.GLASS_BREAK, .7f, 1.2f + level.random.nextFloat() * .2f);
        }
    }

    /** Server side: motes of the gem's colour and glints of its light. */
    public static void glitter(Level level, Vec3 at, Gem gem, int count, double spread) {
        SpellUtil.particles(level, gem.dust, at.x, at.y, at.z, count, spread, spread, spread, .02);
        SpellUtil.particles(level, gem.glint, at.x, at.y, at.z, Math.max(1, count / 2), spread, spread, spread, .03);
        if (gem == Gem.DIAMOND || gem == Gem.LAPIS) {
            SpellUtil.particles(level, ParticleTypes.END_ROD, at.x, at.y, at.z, Math.max(1, count / 5), spread * .6, spread * .6, spread * .6, .02);
        }
    }

    /** A clear crystal note: higher {@code pitch}, higher note. */
    public static void chime(Level level, Vec3 at, float volume, float pitch) {
        sound(level, at, SoundEvents.AMETHYST_BLOCK_CHIME, volume, pitch);
        sound(level, at, SoundEvents.AMETHYST_BLOCK_RESONATE, volume * .6f, pitch);
    }

    public static void sound(Level level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }
}
