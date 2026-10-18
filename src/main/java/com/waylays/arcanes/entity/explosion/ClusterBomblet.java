package com.waylays.arcanes.entity.explosion;

import com.waylays.arcanes.entity.SpellProjectile;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.explosion.ClusterBarrageSpell;
import com.waylays.arcanes.spells.explosion.ExplosionFx;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * One glowing sub-charge of Cluster Barrage: falls with gravity and explodes on whatever it hits.
 */
public class ClusterBomblet extends SpellProjectile {
    public ClusterBomblet(EntityType<? extends ClusterBomblet> entityType, Level level) {
        super(entityType, level);
    }

    public ClusterBomblet(Level level, @Nullable LivingEntity caster) {
        this(ModEntities.CLUSTER_BOMBLET.get(), level);
        setOwner(caster);
    }

    @Override
    protected double gravity() {
        return .06;
    }

    @Override
    protected int lifetime() {
        return 100;
    }

    @Override
    protected void onImpact(HitResult hit) {
        Vec3 at = hit instanceof EntityHitResult entityHit ? entityHit.getEntity().getBoundingBox().getCenter() : hit.getLocation().add(0, .3, 0);
        explode(at);
    }

    @Override
    protected void onExpire() {
        explode(position());
    }

    private void explode(Vec3 at) {
        ExplosionFx.detonate(level(), at, .9f, ClusterBarrageSpell.BOMBLET_RADIUS, getDamage(), ModSpells.CLUSTER_BARRAGE.get(), this, getOwner());
        discard();
    }

    @Override
    protected void clientTick() {
        Level level = level();
        double cy = getY() + getBbHeight() * .5;
        level.addParticle(ExplosionFx.FIRE_DUST, getX(), cy, getZ(), 0, 0, 0);
        if (random.nextInt(2) == 0) {
            level.addParticle(ExplosionFx.sparks(), getX(), cy, getZ(), 0, .05, 0);
        }
    }
}
