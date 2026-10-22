package com.waylays.arcanes.entity.gold;

import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.gold.GoldFx;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import javax.annotation.Nullable;

/**
 * One cluster of gold crystals of Golden Spikes. Waits {@code warmup} ticks while the ground glows, bursts out of the
 * ground white-hot (hurting, gilding and tossing whatever stands on it), stays a moment cooling, then sinks back. The
 * last of a wave is a crown of spikes: wider, higher, and it throws out a spray of coins.
 */
public class GoldSpikeEntity extends Entity implements AntiMagicSusceptible {
    private static final EntityDataAccessor<Integer> DATA_WARMUP = SynchedEntityData.defineId(GoldSpikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_CROWN = SynchedEntityData.defineId(GoldSpikeEntity.class, EntityDataSerializers.BOOLEAN);

    /** Ticks the spike stays out after erupting, sinking included. */
    public static final int OUT_TICKS = 24;
    public static final int RISE_TICKS = 3;
    public static final int SINK_TICKS = 6;
    /** Reach of a spike cluster, and of the crown that ends the wave. */
    public static final float RADIUS = .6f;
    public static final float CROWN_RADIUS = 1.8f;

    private float damage;
    @Nullable
    private UUID ownerUUID;

    public GoldSpikeEntity(EntityType<? extends GoldSpikeEntity> entityType, Level level) {
        super(entityType, level);
    }

    public GoldSpikeEntity(Level level, LivingEntity owner, float damage, int warmup) {
        this(ModEntities.GOLD_SPIKE.get(), level);
        this.ownerUUID = owner.getUUID();
        this.damage = damage;
        setWarmup(warmup);
        setYRot(owner.getRandom().nextFloat() * 360f);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_WARMUP, 0);
        builder.define(DATA_CROWN, false);
    }

    public boolean isCrown() {
        return this.entityData.get(DATA_CROWN);
    }

    public void setCrown(boolean crown) {
        this.entityData.set(DATA_CROWN, crown);
    }

    public int getWarmup() {
        return this.entityData.get(DATA_WARMUP);
    }

    public void setWarmup(int warmup) {
        this.entityData.set(DATA_WARMUP, warmup);
    }

    @Nullable
    private Entity getOwner() {
        return ownerUUID != null && level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(ownerUUID) : null;
    }

    @Override
    public void tick() {
        super.tick();
        int warmup = getWarmup();
        if (level().isClientSide) {
            if (tickCount < warmup && tickCount % 2 == 0) {
                // The ground glows where the spike is about to come out
                level().addParticle(GoldFx.PALE_DUST, getX() + (random.nextFloat() - .5f) * .6f, getY() + .1, getZ() + (random.nextFloat() - .5f) * .6f, 0, .05, 0);
            }
            return;
        }
        if (tickCount == warmup) {
            erupt();
        }
        if (tickCount > warmup + OUT_TICKS) {
            discard();
        }
    }

    private void erupt() {
        Entity owner = getOwner();
        boolean crown = isCrown();
        float radius = crown ? CROWN_RADIUS : RADIUS;
        AABB area = new AABB(getX() - radius, getY(), getZ() - radius, getX() + radius, getY() + (crown ? 2.6 : 1.8), getZ() + radius);
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (victim.isAlive() && !SpellUtil.isAlly(owner, victim)
                    && DamageSources.applyDamage(victim, damage * (crown ? 1.5f : 1), SpellUtil.damageSource(ModSpells.GOLDEN_SPIKES.get(), this, owner))) {
                GoldFx.gild(victim, crown ? 2 : 1);
                Vec3 v = victim.getDeltaMovement();
                victim.setDeltaMovement(v.x, Math.max(v.y, crown ? .8 : .55), v.z);
                victim.hurtMarked = true;
            }
        }
        Level level = level();
        SpellUtil.particles(level, GoldFx.nuggetShards(), getX(), getY() + .3, getZ(), crown ? 20 : 8, radius * .4, .1, radius * .4, .2);
        SpellUtil.particles(level, GoldFx.sparks(), getX(), getY() + .5, getZ(), crown ? 16 : 6, radius * .35, .4, radius * .35, .3);
        SpellUtil.particles(level, GoldFx.DEEP_DUST, getX(), getY() + .1, getZ(), crown ? 20 : 6, radius * .7, .05, radius * .7, .02);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.NEUTRAL, 1.2f, .7f + random.nextFloat() * .3f);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.ARMOR_EQUIP_GOLD, SoundSource.NEUTRAL, 1f, .6f);
        if (crown) {
            GoldFx.coins(level, position().add(0, 1.2, 0), 14, .4f);
            SpellUtil.cameraShake(level, position(), 8, 10);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 1.2f, .7f);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.NEUTRAL, 2f, .6f);
        }
    }

    /** A crown spreads well outside this one-block box. */
    @Override
    public AABB getBoundingBoxForCulling() {
        return isCrown() ? getBoundingBox().inflate(1.5, 1, 1.5) : getBoundingBox().inflate(.5);
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        discard();
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", tickCount);
        tag.putInt("Warmup", getWarmup());
        tag.putBoolean("Crown", isCrown());
        tag.putFloat("Damage", damage);
        if (ownerUUID != null) {
            tag.putUUID("Owner", ownerUUID);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        tickCount = tag.getInt("Age");
        setWarmup(tag.getInt("Warmup"));
        setCrown(tag.getBoolean("Crown"));
        damage = tag.getFloat("Damage");
        if (tag.hasUUID("Owner")) {
            ownerUUID = tag.getUUID("Owner");
        }
    }
}
