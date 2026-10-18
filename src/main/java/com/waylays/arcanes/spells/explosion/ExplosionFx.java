package com.waylays.arcanes.spells.explosion;

import com.waylays.arcanes.ArcanesConfig;
import com.waylays.arcanes.entity.explosion.BlastEffect;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import io.redspace.ironsspellbooks.particle.FogParticleOptions;
import io.redspace.ironsspellbooks.particle.SparkParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

/**
 * Look and rules of the Explosion school: palette, the shared detonation (damage with falloff, knockback, fireball,
 * shock rings, smoke, sound, camera shake) and the rising whine that announces a blast.
 */
public final class ExplosionFx {
    public static final Vector3f CORE = new Vector3f(1.0f, 0.95f, 0.8f);
    public static final Vector3f FIRE = new Vector3f(1.0f, 0.45f, 0.1f);
    public static final Vector3f EMBER = new Vector3f(0.8f, 0.15f, 0.05f);
    public static final Vector3f ARCANE = new Vector3f(0.75f, 0.25f, 1.0f);

    public static final DustParticleOptions FIRE_DUST = new DustParticleOptions(FIRE, 1.2f);
    public static final DustParticleOptions EMBER_DUST = new DustParticleOptions(EMBER, 1.5f);
    public static final DustParticleOptions ARCANE_DUST = new DustParticleOptions(ARCANE, 1.0f);
    public static final DustParticleOptions CORE_DUST = new DustParticleOptions(CORE, .7f);

    private ExplosionFx() {}

    public static ParticleOptions sparks() {
        return new SparkParticleOptions(FIRE);
    }

    public static ParticleOptions arcaneSparks() {
        return new SparkParticleOptions(ARCANE);
    }

    /**
     * One Explosion school blast. Enemies within {@code radius} take {@code damage} (half at the edge) and are thrown
     * outward; the caster and its allies are never hurt. {@code size} scales the visuals (1 = a grenade, 6 = Cataclysm).
     * Breaks blocks only when the staff enabled it in the server config. Returns the enemies hit. Server side.
     */
    public static List<LivingEntity> detonate(Level level, Vec3 at, float size, float radius, float damage,
                                              AbstractSpell spell, Entity direct, @Nullable Entity caster) {
        return detonate(level, at, size, radius, damage, spell, direct, caster, BlastEffect.STYLE_FIRE);
    }

    public static List<LivingEntity> detonate(Level level, Vec3 at, float size, float radius, float damage,
                                              AbstractSpell spell, Entity direct, @Nullable Entity caster, int style) {
        List<LivingEntity> hit = SpellUtil.enemiesAround(level, at, radius, caster, e -> true);
        for (LivingEntity e : hit) {
            Vec3 away = e.getBoundingBox().getCenter().subtract(at);
            double dist = away.length();
            float falloff = (float) (1 - .5 * Math.min(1, dist / radius));
            if (DamageSources.applyDamage(e, damage * falloff, SpellUtil.damageSource(spell, direct, caster))) {
                Vec3 push = dist < 1.0E-3 ? new Vec3(0, 1, 0) : away.scale(1 / dist);
                double strength = (.35 + size * .18) * falloff;
                e.push(push.x * strength, .25 + .1 * size * falloff, push.z * strength);
                e.hurtMarked = true;
            }
        }
        visuals(level, at, size, style);
        if (ArcanesConfig.explosionsBreakBlocks()) {
            breakBlocks(level, at, radius * .6f, caster);
        }
        return hit;
    }

    /** The look and sound of a blast, without damage. Server side. */
    public static void visuals(Level level, Vec3 at, float size, int style) {
        BlastEffect.spawn(level, at, size, style);
        Vector3f ring = style == BlastEffect.STYLE_FIRE ? ARCANE : CORE;
        SpellUtil.particles(level, ParticleTypes.FLASH, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        SpellUtil.particles(level, new BlastwaveParticleOptions(CORE, size * 1.8f), at.x, at.y, at.z, 1, 0, 0, 0, 0);
        SpellUtil.particles(level, new BlastwaveParticleOptions(ring, size * 2.6f), at.x, at.y, at.z, 1, 0, 0, 0, 0);
        SpellUtil.particles(level, ParticleTypes.EXPLOSION, at.x, at.y, at.z, (int) (2 + size * 3), size * .5, size * .4, size * .5, 0);
        if (size >= 2f) {
            SpellUtil.particles(level, ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
        SpellUtil.particles(level, style == BlastEffect.STYLE_ARCANE ? arcaneSparks() : sparks(), at.x, at.y, at.z, (int) (12 * size), .2, .2, .2, .6);
        SpellUtil.particles(level, ParticleTypes.FLAME, at.x, at.y, at.z, (int) (10 * size), .3, .3, .3, .2 * size);
        SpellUtil.particles(level, ParticleTypes.LAVA, at.x, at.y, at.z, (int) (2 * size), .3, .3, .3, 0);
        SpellUtil.particles(level, ParticleTypes.LARGE_SMOKE, at.x, at.y + .3, at.z, (int) (6 * size), size * .4, size * .3, size * .4, .04);
        SpellUtil.particles(level, new FogParticleOptions(EMBER, size * .8f), at.x, at.y, at.z, 2, .2, .2, .2, .02);

        float rand = level.random.nextFloat() * .15f;
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL, 1.5f + size * .8f, Math.max(.5f, 1.25f - .15f * size) + rand);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.NEUTRAL, 1.2f + size * .3f, .6f + rand);
        if (size >= 1.8f) {
            level.playSound(null, at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.NEUTRAL, .3f + size * .05f, .5f);
        }
        SpellUtil.cameraShake(level, at, (int) (5 + size * 4), 6 + size * 5);
    }

    /**
     * A small silent blast for effects that set off dozens at once (Detonation Wave, Cataclysm crown): fireball and
     * a few particles, no sound or camera shake (the caller plays those once). Server side.
     */
    public static void lightBlast(Level level, Vec3 at, float size, int style) {
        BlastEffect.spawn(level, at, size, style);
        SpellUtil.particles(level, ParticleTypes.EXPLOSION, at.x, at.y + .3, at.z, 1, size * .3, size * .2, size * .3, 0);
        SpellUtil.particles(level, style == BlastEffect.STYLE_ARCANE ? arcaneSparks() : sparks(), at.x, at.y + .3, at.z, (int) (5 * size), .2, .2, .2, .45);
        SpellUtil.particles(level, ParticleTypes.FLAME, at.x, at.y + .2, at.z, (int) (5 * size), .3, .2, .3, .12);
        SpellUtil.particles(level, ParticleTypes.LARGE_SMOKE, at.x, at.y + .5, at.z, 2, size * .3, size * .2, size * .3, .03);
    }

    /** The rising hiss that announces a blast. */
    public static void whine(Level level, Vec3 at, float pitch) {
        level.playSound(null, at.x, at.y, at.z, SoundEvents.TNT_PRIMED, SoundSource.NEUTRAL, 1f, pitch);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.NEUTRAL, .6f, 1.4f + pitch * .3f);
    }

    /** Only with the staff option on: breaks soft blocks in a sphere (no bedrock, obsidian or other blast-proof blocks). */
    private static void breakBlocks(Level level, Vec3 at, float radius, @Nullable Entity caster) {
        int r = (int) Math.ceil(radius);
        BlockPos center = BlockPos.containing(at);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            if (pos.distToCenterSqr(at) > radius * radius) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && state.getDestroySpeed(level, pos) >= 0 && state.getBlock().getExplosionResistance() < 9) {
                level.destroyBlock(pos, true, caster);
            }
        }
    }
}
