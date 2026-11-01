package com.waylays.arcanes.entity.gold;

import com.waylays.arcanes.entity.SpellProjectile;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.gold.GoldFx;
import io.redspace.ironsspellbooks.damage.DamageSources;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
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

import java.util.Comparator;
import java.util.List;

/**
 * Gold Bolt: an ingot of gold hurled spinning, hot at the edges. It strikes what it meets
 * and throws out a spray of coins; then it ricochets to the nearest other enemy in sight (a quarter weaker each time),
 * as many times as it has ricochets left. When it has none, or meets a wall, it bursts in coins and shards.
 */
public class GoldBoltProjectile extends SpellProjectile {
    public static final float SPEED = 1.6f;
    public static final float RICOCHET_RANGE = 8;
    public static final float RICOCHET_DAMAGE = .75f;

    private final IntOpenHashSet struck = new IntOpenHashSet();
    private int ricochets = 1;

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

    public void setRicochets(int ricochets) {
        this.ricochets = ricochets;
    }

    @Override
    protected int lifetime() {
        return 80;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !struck.contains(target.getId()) && !SpellUtil.isAlly(getOwner(), target);
    }

    @Override
    protected void onImpact(HitResult hit) {
        Entity owner = getOwner();
        Vec3 at = hit.getLocation();
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target) {
            struck.add(target.getId());
            Vec3 from = position().add(0, getBbHeight() * .5, 0);
            at = target.getBoundingBox().inflate(.3).clip(from, from.add(getDeltaMovement())).orElse(target.getBoundingBox().getCenter());
            target.invulnerableTime = 0;
            DamageSources.applyDamage(target, getDamage(), SpellUtil.damageSource(ModSpells.GOLD_BOLT.get(), this, owner));
            GoldFx.coins(level(), at, 6, .3f);
            level().playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, .35f, 1.9f);
            level().playSound(null, at.x, at.y, at.z, SoundEvents.ARMOR_EQUIP_GOLD.value(), SoundSource.NEUTRAL, 1.5f, .8f + .15f * struck.size());
            if (ricochets > 0 && ricochet(target, at)) {
                return;
            }
        }
        GoldFx.shatterBurst(level(), at.x, at.y, at.z, 1f);
        GoldFx.coins(level(), at, 10, .4f);
        level().playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.NEUTRAL, 2f, 1.1f + random.nextFloat() * .2f);
        discard();
    }

    /** Off to the nearest other enemy in sight of {@code from}, if any. */
    private boolean ricochet(LivingEntity from, Vec3 at) {
        List<LivingEntity> next = SpellUtil.enemiesAround(level(), at, RICOCHET_RANGE, getOwner(),
                e -> !struck.contains(e.getId()) && from.hasLineOfSight(e));
        if (next.isEmpty()) {
            return false;
        }
        next.sort(Comparator.comparingDouble(e -> e.distanceToSqr(at)));
        Vec3 to = next.get(0).getBoundingBox().getCenter().subtract(at);
        ricochets--;
        setDamage(getDamage() * RICOCHET_DAMAGE);
        setPos(at.x, at.y - getBbHeight() * .5, at.z);
        setDeltaMovement(to.normalize().scale(SPEED));
        hasImpulse = true;
        level().playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.NEUTRAL, 1.2f, 1.6f);
        return true;
    }

    /** Client side, every tick: glitter left in the air behind it. */
    @Override
    protected void clientTick() {
        Vec3 motion = getDeltaMovement();
        double cx = getX(), cy = getY() + getBbHeight() * .5, cz = getZ();
        level().addParticle(GoldFx.PALE_DUST, cx - motion.x * .5, cy - motion.y * .5, cz - motion.z * .5, 0, 0, 0);
        if (random.nextInt(3) == 0) {
            level().addParticle(GoldFx.sparks(), cx, cy, cz, (random.nextFloat() - .5f) * .16f - motion.x * .1, .05,
                    (random.nextFloat() - .5f) * .16f - motion.z * .1);
        }
        if (random.nextInt(2) == 0) {
            float a = tickCount * .7f;
            level().addParticle(GoldFx.GOLD_DUST, cx + Mth.cos(a) * .25, cy + Mth.sin(a) * .25, cz, 0, 0, 0);
        }
    }
}
