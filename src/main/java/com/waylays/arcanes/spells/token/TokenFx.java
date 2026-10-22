package com.waylays.arcanes.spells.token;

import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Shared look and sound of Token Magic: the soul tokens' cyan glow, their sparks and their clinks. */
public final class TokenFx {
    /** The tokens' glow: cyan blue, its deep shade and its pale core. */
    public static final int CYAN = 0x33D6FF;
    public static final int DEEP = 0x1478C8;
    public static final int PALE = 0xD6FAFF;
    public static final DustParticleOptions DUST = new DustParticleOptions(new Vector3f(.2f, .84f, 1f), 1f);

    private TokenFx() {}

    /** Server side: a puff of cyan sparks and soul wisps. */
    public static void burst(Level level, Vec3 at, int count, double spread) {
        SpellUtil.particles(level, DUST, at.x, at.y, at.z, count, spread, spread, spread, 0);
        SpellUtil.particles(level, ParticleTypes.GLOW, at.x, at.y, at.z, count / 2 + 1, spread, spread, spread, .05);
        SpellUtil.particles(level, ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y, at.z, count / 3 + 1, spread * .6, spread * .6, spread * .6, .02);
    }

    /** Server side: a token striking something, with its clink. */
    public static void impact(Level level, Vec3 at, float size) {
        burst(level, at, 6 + (int) (size * 8), size * .4);
        SpellUtil.particles(level, ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 6 + (int) (size * 6), size * .3, size * .3, size * .3, .2);
        sound(level, at, SoundEvents.CHAIN_HIT, .8f, 1.6f);
        sound(level, at, SoundEvents.EXPERIENCE_ORB_PICKUP, .5f, .8f + level.random.nextFloat() * .4f);
    }

    /** The clink of tokens. */
    public static void clink(Level level, Vec3 at, float volume) {
        sound(level, at, SoundEvents.CHAIN_PLACE, volume, 1.7f + level.random.nextFloat() * .3f);
        sound(level, at, SoundEvents.AMETHYST_BLOCK_CHIME, volume * .8f, 1.4f + level.random.nextFloat() * .4f);
    }

    public static void sound(Level level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** The middle of a creature's body. */
    public static Vec3 chest(Entity e) {
        return e.position().add(0, e.getBbHeight() * .55, 0);
    }

    /** Where a caster holds a token: before their chest, a little to the side of the casting hand. */
    public static Vec3 hand(LivingEntity caster) {
        return caster.getEyePosition().add(caster.getLookAngle().scale(.7)).add(0, -.35, 0);
    }
}
