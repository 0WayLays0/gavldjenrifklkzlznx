package com.waylays.arcanes.entity.explosion;

import com.waylays.arcanes.entity.SpellProjectile;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.explosion.ClusterBarrageSpell;
import com.waylays.arcanes.spells.explosion.ExplosionFx;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Cluster Barrage: a big glowing shell that climbs into the sky and bursts into {@link ClusterBarrageSpell#BOMBLETS}
 * glowing sub-charges, which rain down and explode one after the other.
 */
public class ClusterShell extends SpellProjectile {
    private int flightTicks = 20;

    public ClusterShell(EntityType<? extends ClusterShell> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public ClusterShell(Level level, LivingEntity caster, int flightTicks) {
        this(ModEntities.CLUSTER_SHELL.get(), level);
        setOwner(caster);
        this.flightTicks = flightTicks;
    }

    @Override
    protected int lifetime() {
        return flightTicks;
    }

    @Override
    protected void onImpact(HitResult hit) {
        burst(hit.getLocation().subtract(getDeltaMovement().normalize().scale(.5)));
    }

    @Override
    protected void onExpire() {
        burst(position().add(0, getBbHeight() * .5, 0));
    }

    private void burst(Vec3 at) {
        Level level = level();
        Entity owner = getOwner();
        double offset = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < ClusterBarrageSpell.BOMBLETS; i++) {
            double a = offset + i * Math.PI * 2 / ClusterBarrageSpell.BOMBLETS + (random.nextDouble() - .5) * .4;
            double speed = .12 + random.nextDouble() * .16;
            ClusterBomblet bomblet = new ClusterBomblet(level, owner instanceof LivingEntity living ? living : null);
            bomblet.setPos(at.x, at.y - bomblet.getBbHeight() * .5, at.z);
            // Different upward kicks: the bomblets land, and blow up, one after the other
            bomblet.setDeltaMovement(Math.cos(a) * speed, .05 + random.nextDouble() * .3, Math.sin(a) * speed);
            bomblet.setDamage(getDamage());
            level.addFreshEntity(bomblet);
        }
        // A firework of destruction
        ExplosionFx.lightBlast(level, at, 1.3f, BlastEffect.STYLE_ARCANE);
        SpellUtil.particles(level, ParticleTypes.FLASH, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        SpellUtil.particles(level, new BlastwaveParticleOptions(ExplosionFx.ARCANE, 4f), at.x, at.y, at.z, 1, 0, 0, 0, 0);
        SpellUtil.particles(level, ParticleTypes.FIREWORK, at.x, at.y, at.z, 40, .2, .2, .2, .3);
        SpellUtil.particles(level, ExplosionFx.sparks(), at.x, at.y, at.z, 30, .3, .3, .3, .6);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST_FAR, SoundSource.NEUTRAL, 4f, .7f);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.NEUTRAL, 2.5f, .8f);
        discard();
    }

    @Override
    protected void clientTick() {
        Level level = level();
        double cy = getY() + getBbHeight() * .5;
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.FLAME, getX(), cy, getZ(), (random.nextFloat() - .5f) * .1f, -.1, (random.nextFloat() - .5f) * .1f);
        }
        level.addParticle(ExplosionFx.ARCANE_DUST, getX() + (random.nextFloat() - .5f) * .6f, cy + (random.nextFloat() - .5f) * .6f, getZ() + (random.nextFloat() - .5f) * .6f, 0, 0, 0);
        level.addParticle(ParticleTypes.FIREWORK, getX(), cy, getZ(), (random.nextFloat() - .5f) * .1f, -.05, (random.nextFloat() - .5f) * .1f);
        if (random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.LARGE_SMOKE, getX(), cy, getZ(), 0, 0, 0);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Flight", flightTicks);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        flightTicks = tag.getInt("Flight");
    }
}
