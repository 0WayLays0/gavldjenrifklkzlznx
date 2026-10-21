package com.waylays.arcanes.spells.rune;

import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/** Shared look and rules of Rune Magic: the runes' colours, glyphs and stone chips, and who a support rune helps. */
public final class RuneFx {
    /** The glow of the runes: ice blue, its deep shade, silver for the bright core, sea green for the healing ones. */
    public static final int GLOW = 0x8FD8FF;
    public static final int DEEP = 0x2F80C8;
    public static final int SILVER = 0xE8F2FA;
    public static final int MEND = 0x7CF5C4;
    public static final DustParticleOptions GLOW_DUST = dust(GLOW, 1.1f);
    public static final DustParticleOptions MEND_DUST = dust(MEND, 1.1f);

    private RuneFx() {}

    public static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f), size);
    }

    /** Sparks lighting up around a circle on the ground. */
    public static void ring(Level level, Vec3 center, double radius, int count, ParticleOptions particle) {
        for (int i = 0; i < count; i++) {
            double a = i * Mth.TWO_PI / count;
            SpellUtil.particles(level, particle, center.x + Math.cos(a) * radius, center.y + .15, center.z + Math.sin(a) * radius, 1, .05, .05, .05, 0);
        }
    }

    /** Glyphs flying off a rune (the enchanting table's) in a puff of glow. */
    public static void glyphs(Level level, Vec3 at, int count, double spread) {
        SpellUtil.particles(level, ParticleTypes.ENCHANT, at.x, at.y, at.z, count, spread, spread, spread, .8);
        SpellUtil.particles(level, GLOW_DUST, at.x, at.y, at.z, count / 2 + 1, spread, spread, spread, 0);
    }

    /** Chips of stone breaking off. */
    public static void stone(Level level, Vec3 at, BlockState state, int count, double spread) {
        SpellUtil.particles(level, new BlockParticleOption(ParticleTypes.BLOCK, state), at.x, at.y, at.z, count, spread, spread * .5, spread, .15);
    }

    public static void sound(Level level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** Whether a support rune of {@code caster} helps {@code target}: Iron's own rule for its healing spells. */
    public static boolean helps(@Nullable LivingEntity caster, LivingEntity target) {
        return caster != null && target.isAlive() && !target.isSpectator() && Utils.shouldHealEntity(caster, target);
    }
}
