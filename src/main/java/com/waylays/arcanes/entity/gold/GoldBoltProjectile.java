package com.waylays.arcanes.entity.gold;

import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.gold.GoldBoltSpell;
import com.waylays.arcanes.spells.gold.GoldFx;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Optional;

public class GoldBoltProjectile extends AbstractMagicProjectile {
    public GoldBoltProjectile(EntityType<? extends GoldBoltProjectile> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    public GoldBoltProjectile(Level level, LivingEntity shooter) {
        this(ModEntities.GOLD_BOLT.get(), level);
        setOwner(shooter);
    }

    @Override
    public float getSpeed() {
        return 1.6f;
    }

    @Override
    public Optional<SoundEvent> getImpactSound() {
        return Optional.of(SoundEvents.AMETHYST_CLUSTER_BREAK);
    }

    @Override
    protected void doImpactSound(SoundEvent sound) {
        Level level = level();
        level.playSound(null, getX(), getY(), getZ(), sound, SoundSource.NEUTRAL, 2f, 1.1f + Utils.random.nextFloat() * .2f);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, .35f, 1.9f);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.ARMOR_EQUIP_GOLD, SoundSource.NEUTRAL, 1.5f, .8f);
    }

    @Override
    protected void onHitBlock(BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        // Burst where the ingot touched, not where this tick's movement started
        setPos(blockHitResult.getLocation());
        shatter(null);
        discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        super.onHitEntity(entityHitResult);
        if (isRemoved()) {
            // Hit a magic shield: the base class already treated it as a block impact
            return;
        }
        Entity target = entityHitResult.getEntity();
        // Burst on the target's surface, along the ingot's path
        Vec3 from = position().add(0, getBbHeight() * .5, 0);
        Vec3 contact = target.getBoundingBox().inflate(.3).clip(from, from.add(getDeltaMovement())).orElse(target.getBoundingBox().getCenter());
        setPos(contact.x, contact.y - getBbHeight() * .5, contact.z);
        DamageSources.applyDamage(target, getDamage(), ModSpells.GOLD_BOLT.get().getDamageSource(this, getOwner()));
        shatter(target);
        discard();
    }

    /** The ingot bursts into shards that hit everything around the impact, except the caster and the direct target. */
    private void shatter(@Nullable Entity directTarget) {
        if (level().isClientSide) {
            return;
        }
        Entity owner = getOwner();
        float splash = getDamage() * GoldBoltSpell.SPLASH_RATIO;
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(GoldBoltSpell.SPLASH_RADIUS))) {
            if (victim != owner && victim != directTarget && victim.isAlive() && victim.distanceToSqr(this) <= GoldBoltSpell.SPLASH_RADIUS * GoldBoltSpell.SPLASH_RADIUS) {
                DamageSources.applyDamage(victim, splash, ModSpells.GOLD_BOLT.get().getDamageSource(this, owner));
            }
        }
    }

    @Override
    public void impactParticles(double x, double y, double z) {
        GoldFx.shatterBurst(level(), x, y + getBbHeight() * .5, z, 1f);
    }

    /** Client side, every tick: a dense gold dust core, a pale glitter spiral and stray sparks. */
    @Override
    public void trailParticles() {
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
            double radians = (tickCount * .5f + i * Mth.PI);
            Vec3 swirl = new Vec3(Math.cos(radians) * radius, Math.sin(radians) * radius, 0).yRot(yHeading * Mth.DEG_TO_RAD);
            level.addParticle(GoldFx.PALE_DUST, cx + swirl.x, cy + swirl.y, cz + swirl.z, 0, 0, 0);
        }
        if (random.nextInt(3) == 0) {
            Vec3 jitter = Utils.getRandomVec3(.08f);
            level.addParticle(GoldFx.sparks(), cx, cy, cz, jitter.x - motion.x * .1, jitter.y + .05, jitter.z - motion.z * .1);
        }
    }
}
