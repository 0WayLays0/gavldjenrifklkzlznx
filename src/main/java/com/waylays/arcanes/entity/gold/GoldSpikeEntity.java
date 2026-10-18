package com.waylays.arcanes.entity.gold;

import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.gold.GoldFx;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
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
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * One gold crystal spike of Golden Spikes. Waits {@code warmup} ticks, bursts out of the ground (hurting and tossing
 * whatever stands on it), stays a moment, then sinks back.
 */
public class GoldSpikeEntity extends Entity implements AntiMagicSusceptible {
    private static final EntityDataAccessor<Integer> DATA_WARMUP = SynchedEntityData.defineId(GoldSpikeEntity.class, EntityDataSerializers.INT);

    /** Ticks the spike stays out after erupting, sinking included. */
    public static final int OUT_TICKS = 24;
    public static final int RISE_TICKS = 3;
    public static final int SINK_TICKS = 6;

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
    protected void defineSynchedData() {
        this.entityData.define(DATA_WARMUP, 0);
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
        AABB area = new AABB(getX() - .6, getY(), getZ() - .6, getX() + .6, getY() + 1.8, getZ() + .6);
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (victim.isAlive() && !GoldFx.isAlly(owner, victim)
                    && DamageSources.applyDamage(victim, damage, GoldFx.damageSource(ModSpells.GOLDEN_SPIKES.get(), this, owner))) {
                Vec3 v = victim.getDeltaMovement();
                victim.setDeltaMovement(v.x, Math.max(v.y, .55), v.z);
                victim.hurtMarked = true;
            }
        }
        GoldFx.particles(level(), GoldFx.nuggetShards(), getX(), getY() + .3, getZ(), 8, .25, .1, .25, .2);
        GoldFx.particles(level(), GoldFx.sparks(), getX(), getY() + .5, getZ(), 6, .2, .4, .2, .3);
        GoldFx.particles(level(), GoldFx.DEEP_DUST, getX(), getY() + .1, getZ(), 6, .4, .05, .4, .02);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.NEUTRAL, 1.2f, .7f + random.nextFloat() * .3f);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.ARMOR_EQUIP_GOLD, SoundSource.NEUTRAL, 1f, .6f);
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
        tag.putFloat("Damage", damage);
        if (ownerUUID != null) {
            tag.putUUID("Owner", ownerUUID);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        tickCount = tag.getInt("Age");
        setWarmup(tag.getInt("Warmup"));
        damage = tag.getFloat("Damage");
        if (tag.hasUUID("Owner")) {
            ownerUUID = tag.getUUID("Owner");
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
