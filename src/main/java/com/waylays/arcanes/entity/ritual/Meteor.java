package com.waylays.arcanes.entity.ritual;

import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.UUID;

/**
 * The Guided Meteor: a burning rock falls slowly out of the sky onto the spot, trailing fire and smoke, and strikes
 * it with a blast (no block is broken). It hurts and burns hostile creatures around, sparing the participants.
 */
public class Meteor extends RitualEntity {
    public static final int FALL = 60;
    public static final float RADIUS = 7;
    public static final float DAMAGE = 30;
    /** Where it comes from, relative to where it lands. */
    public static final Vec3 FROM = new Vec3(-18, 46, -10);

    public Meteor(EntityType<? extends Meteor> entityType, Level level) {
        super(entityType, level);
    }

    public static Meteor fall(ServerLevel level, Vec3 at, Set<UUID> allies, float power) {
        Meteor meteor = new Meteor(ModEntities.METEOR.get(), level);
        meteor.moveTo(at.x, at.y, at.z, 0, 0);
        meteor.setup(FALL + 80, 3 * power, allies, power);
        level.addFreshEntity(meteor);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 3f, .5f);
        return meteor;
    }

    /** How far down it has come (0 in the sky, 1 landed). */
    public float fallen(float partialTicks) {
        float t = Mth.clamp((tickCount + partialTicks) / FALL, 0, 1);
        return t * t;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount < FALL) {
            Vec3 p = position().add(FROM.scale(1 - fallen(0)));
            SpellUtil.particles(level, ParticleTypes.LARGE_SMOKE, p.x, p.y, p.z, 6, 1, 1, 1, .02);
            SpellUtil.particles(level, ParticleTypes.FLAME, p.x, p.y, p.z, 10, .9, .9, .9, .05);
            if (tickCount % 10 == 0) {
                level.playSound(null, p.x, p.y, p.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 3f, .4f);
            }
        }
        if (tickCount == FALL) {
            impact(level);
        }
    }

    private void impact(ServerLevel level) {
        Vec3 c = position();
        float radius = RADIUS * Math.min(1.5f, power);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius, 4, radius), this::foe)) {
            double d = e.position().distanceTo(c);
            if (d > radius) {
                continue;
            }
            e.invulnerableTime = 0;
            e.hurt(level.damageSources().magic(), (float) (DAMAGE * power * (1 - .6 * d / radius)));
            e.igniteForSeconds(5);
            Vec3 away = e.position().subtract(c).multiply(1, 0, 1);
            away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize();
            double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
            e.setDeltaMovement(away.scale(1.4 * resist).add(0, .7 * resist, 0));
            e.hurtMarked = true;
        }
        SpellUtil.particles(level, ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 1, c.z, 3, 1.5, .5, 1.5, 0);
        SpellUtil.particles(level, ParticleTypes.LAVA, c.x, c.y + .5, c.z, 60, radius * .4, .3, radius * .4, .5);
        SpellUtil.particles(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, c.x, c.y + 1, c.z, 40, radius * .5, 1, radius * .5, .03);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 4f, .5f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 3f, .6f);
        SpellUtil.cameraShake(level, c, 40, radius + 40);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256 * 256;
    }
}
