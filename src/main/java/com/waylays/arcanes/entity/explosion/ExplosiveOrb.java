package com.waylays.arcanes.entity.explosion;

import com.waylays.arcanes.entity.SpellProjectile;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.explosion.BlastOrbSpell;
import com.waylays.arcanes.spells.explosion.ExplosionFx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Blast Orb: an unstable orb of energy crackling with sparks. Bursts into a double shock wave on impact, or in mid-air
 * when its energy runs out.
 */
public class ExplosiveOrb extends SpellProjectile {
    public static final float SPEED = 1.1f;

    public ExplosiveOrb(EntityType<? extends ExplosiveOrb> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public ExplosiveOrb(Level level, LivingEntity caster) {
        this(ModEntities.EXPLOSIVE_ORB.get(), level);
        setOwner(caster);
    }

    @Override
    protected int lifetime() {
        return 60;
    }

    @Override
    protected void onImpact(HitResult hit) {
        Vec3 at = hit.getLocation();
        if (hit instanceof EntityHitResult entityHit) {
            at = entityHit.getEntity().getBoundingBox().getCenter();
        }
        explode(at);
    }

    @Override
    protected void onExpire() {
        explode(position().add(0, getBbHeight() * .5, 0));
    }

    private void explode(Vec3 at) {
        ExplosionFx.detonate(level(), at, 1.3f, BlastOrbSpell.RADIUS, getDamage(), ModSpells.BLAST_ORB.get(), this, getOwner());
        discard();
    }

    @Override
    protected void clientTick() {
        Level level = level();
        double cy = getY() + getBbHeight() * .5;
        level.addParticle(ExplosionFx.FIRE_DUST, getX(), cy, getZ(), 0, 0, 0);
        level.addParticle(random.nextBoolean() ? ExplosionFx.ARCANE_DUST : ExplosionFx.CORE_DUST,
                getX() + (random.nextFloat() - .5f) * .5f, cy + (random.nextFloat() - .5f) * .5f, getZ() + (random.nextFloat() - .5f) * .5f, 0, 0, 0);
        if (random.nextInt(2) == 0) {
            level.addParticle(ExplosionFx.sparks(), getX(), cy, getZ(), (random.nextFloat() - .5f) * .3f, (random.nextFloat() - .5f) * .3f, (random.nextFloat() - .5f) * .3f);
        }
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SMOKE, getX(), cy, getZ(), 0, .02, 0);
        }
    }
}
