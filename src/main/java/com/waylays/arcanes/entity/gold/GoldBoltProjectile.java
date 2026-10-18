package com.waylays.arcanes.entity.gold;

import com.waylays.arcanes.entity.SpellProjectile;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.gold.GoldBoltSpell;
import com.waylays.arcanes.spells.gold.GoldFx;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Gold Bolt: a spinning ingot. The direct hit takes full damage; the ingot then bursts into shards that hit
 * every enemy within {@link GoldBoltSpell#SPLASH_RADIUS} for {@link GoldBoltSpell#SPLASH_RATIO} of it.
 */
public class GoldBoltProjectile extends SpellProjectile {
    public static final float SPEED = 1.6f;

    public GoldBoltProjectile(EntityType<? extends GoldBoltProjectile> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public GoldBoltProjectile(Level level, LivingEntity shooter) {
        this(ModEntities.GOLD_BOLT.get(), level);
        setOwner(shooter);
    }

    public void shoot(Vec3 direction) {
        shoot(direction, SPEED);
    }

    @Override
    protected void onImpact(HitResult hit) {
        Entity owner = getOwner();
        Entity directTarget = null;
        Vec3 burst = hit.getLocation();
        if (hit instanceof EntityHitResult entityHit) {
            directTarget = entityHit.getEntity();
            // Burst on the target's surface, along the ingot's path
            Vec3 from = position().add(0, getBbHeight() * .5, 0);
            burst = directTarget.getBoundingBox().inflate(.3).clip(from, from.add(getDeltaMovement())).orElse(directTarget.getBoundingBox().getCenter());
            DamageSources.applyDamage(directTarget, getDamage(), SpellUtil.damageSource(ModSpells.GOLD_BOLT.get(), this, owner));
        }
        Entity excluded = directTarget;
        for (LivingEntity victim : SpellUtil.enemiesAround(level(), burst, GoldBoltSpell.SPLASH_RADIUS, owner, e -> e != excluded)) {
            DamageSources.applyDamage(victim, getDamage() * GoldBoltSpell.SPLASH_RATIO, SpellUtil.damageSource(ModSpells.GOLD_BOLT.get(), this, owner));
        }
        GoldFx.shatterBurst(level(), burst.x, burst.y, burst.z, 1f);
        level().playSound(null, burst.x, burst.y, burst.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.NEUTRAL, 2f, 1.1f + random.nextFloat() * .2f);
        level().playSound(null, burst.x, burst.y, burst.z, SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, .35f, 1.9f);
        level().playSound(null, burst.x, burst.y, burst.z, SoundEvents.ARMOR_EQUIP_GOLD, SoundSource.NEUTRAL, 1.5f, .8f);
        discard();
    }

    /** Client side, every tick: a dense gold dust core, a pale glitter spiral and stray sparks. */
    @Override
    protected void clientTick() {
        Level level = level();
        Vec3 motion = getDeltaMovement();
        double cx = getX();
        double cy = getY() + getBbHeight() * .5;
        double cz = getZ();
        level.addParticle(GoldFx.GOLD_DUST, cx, cy, cz, 0, 0, 0);
        level.addParticle(GoldFx.DEEP_DUST, cx - motion.x * .5, cy - motion.y * .5, cz - motion.z * .5, 0, 0, 0);

        float yHeading = -((float) (Mth.atan2(motion.z, motion.x) * Mth.RAD_TO_DEG) + 90.0F);
        float radius = .3f;
        for (int i = 0; i < 2; i++) {
            double radians = tickCount * .5f + i * Mth.PI;
            Vec3 swirl = new Vec3(Math.cos(radians) * radius, Math.sin(radians) * radius, 0).yRot(yHeading * Mth.DEG_TO_RAD);
            level.addParticle(GoldFx.PALE_DUST, cx + swirl.x, cy + swirl.y, cz + swirl.z, 0, 0, 0);
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(GoldFx.sparks(), cx, cy, cz, (random.nextFloat() - .5f) * .16f - motion.x * .1, .05 + (random.nextFloat() - .5f) * .16f, (random.nextFloat() - .5f) * .16f - motion.z * .1);
        }
    }
}
