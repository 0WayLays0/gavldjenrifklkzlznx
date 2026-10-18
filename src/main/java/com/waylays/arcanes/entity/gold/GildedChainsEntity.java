package com.waylays.arcanes.entity.gold;

import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.gold.GoldFx;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import io.redspace.ironsspellbooks.entity.spells.root.PreventDismount;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
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
 * Invisible anchor for Gilded Chains: the bound target rides it and cannot dismount (Iron's {@link PreventDismount}),
 * so it stays pinned until the chains break. The chains themselves are drawn by the client renderer.
 */
public class GildedChainsEntity extends Entity implements PreventDismount, AntiMagicSusceptible {
    private static final EntityDataAccessor<Integer> DATA_DURATION = SynchedEntityData.defineId(GildedChainsEntity.class, EntityDataSerializers.INT);

    /** Number of chains around the target. */
    public static final int CHAIN_COUNT = 4;
    /** Ticks the chains take to burst out of the ground. */
    public static final int EMERGE_TICKS = 6;

    @Nullable
    private UUID ownerUUID;
    @Nullable
    private LivingEntity cachedOwner;

    public GildedChainsEntity(EntityType<? extends GildedChainsEntity> entityType, Level level) {
        super(entityType, level);
    }

    public GildedChainsEntity(Level level, LivingEntity owner) {
        this(ModEntities.GILDED_CHAINS.get(), level);
        setOwner(owner);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_DURATION, 60);
    }

    public int getDuration() {
        return this.entityData.get(DATA_DURATION);
    }

    public void setDuration(int duration) {
        this.entityData.set(DATA_DURATION, duration);
    }

    public void setOwner(@Nullable LivingEntity owner) {
        this.cachedOwner = owner;
        this.ownerUUID = owner == null ? null : owner.getUUID();
    }

    @Nullable
    public LivingEntity getOwner() {
        if (cachedOwner == null && ownerUUID != null && level() instanceof ServerLevel serverLevel && serverLevel.getEntity(ownerUUID) instanceof LivingEntity living) {
            cachedOwner = living;
        }
        return cachedOwner;
    }

    /** Ground point where chain {@code index} comes out, relative to this entity, for a target of the given width. */
    public static Vec3 anchorOffset(int index, float targetWidth) {
        float angle = (index * 90 + 45) * Mth.DEG_TO_RAD;
        float radius = targetWidth * .5f + 1.0f;
        return new Vec3(Mth.cos(angle) * radius, 0, Mth.sin(angle) * radius);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientParticles();
            return;
        }
        if (tickCount == 1) {
            emergeEffects();
        }
        Entity bound = getFirstPassenger();
        if (tickCount > getDuration() || bound == null || !bound.isAlive()) {
            breakChains();
        }
    }

    private float boundWidth() {
        Entity bound = getFirstPassenger();
        return bound == null ? .6f : bound.getBbWidth();
    }

    private float boundHeight() {
        Entity bound = getFirstPassenger();
        return bound == null ? 1.8f : bound.getBbHeight();
    }

    private void emergeEffects() {
        Level level = level();
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_PLACE, SoundSource.NEUTRAL, 2f, .6f);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_HIT, SoundSource.NEUTRAL, 2f, .8f);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_PLACE, SoundSource.NEUTRAL, .4f, 1.6f);
        float width = boundWidth();
        for (int i = 0; i < CHAIN_COUNT; i++) {
            Vec3 anchor = position().add(anchorOffset(i, width));
            GoldFx.particles(level, GoldFx.nuggetShards(), anchor.x, anchor.y + .1, anchor.z, 6, .1, .05, .1, .15);
            GoldFx.particles(level, GoldFx.sparks(), anchor.x, anchor.y + .1, anchor.z, 5, .05, .05, .05, .35);
            GoldFx.particles(level, GoldFx.DEEP_DUST, anchor.x, anchor.y + .2, anchor.z, 8, .2, .1, .2, .02);
        }
    }

    /** Client side: sparks where the chains break the ground, then slow glitter around the bound target. */
    private void clientParticles() {
        Level level = level();
        float width = boundWidth();
        if (tickCount <= EMERGE_TICKS) {
            for (int i = 0; i < CHAIN_COUNT; i++) {
                Vec3 anchor = position().add(anchorOffset(i, width));
                level.addParticle(GoldFx.sparks(), anchor.x, anchor.y + .05, anchor.z, (random.nextFloat() - .5f) * .2f, .25f + random.nextFloat() * .2f, (random.nextFloat() - .5f) * .2f);
            }
        } else if (tickCount % 3 == 0) {
            float height = boundHeight();
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = width * .5 + .3;
            level.addParticle(GoldFx.PALE_DUST, getX() + Math.cos(angle) * radius, getY() + random.nextDouble() * height, getZ() + Math.sin(angle) * radius, 0, .02, 0);
        }
    }

    /** Chains shatter in a burst of gold; discarding the anchor frees the target. */
    public void breakChains() {
        if (isRemoved()) {
            return;
        }
        Level level = level();
        if (!level.isClientSide) {
            double y = getY() + boundHeight() * .5;
            GoldFx.shatterBurst(level, getX(), y, getZ(), .8f);
            GoldFx.particles(level, GoldFx.chainShards(), getX(), y, getZ(), 16, boundWidth() * .5, boundHeight() * .3, boundWidth() * .5, .2);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_BREAK, SoundSource.NEUTRAL, 2f, .7f);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.NEUTRAL, 1.5f, .9f);
        }
        // Iron's blocks dismounting from a PreventDismount vehicle until it is removed: discard() frees the rider
        discard();
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (passenger.distanceToSqr(this) > 5 * 5) {
            // The target was teleported away
            breakChains();
        } else {
            passenger.setPos(getX(), getY(), getZ());
        }
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            breakChains();
            return true;
        }
        return false;
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        breakChains();
    }

    /** The chains reach well outside this small anchor: keep rendering while any of them is on screen. */
    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(2.5, 0, 2.5).expandTowards(0, 3, 0);
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
    public boolean isOnFire() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", tickCount);
        tag.putInt("Duration", getDuration());
        if (ownerUUID != null) {
            tag.putUUID("Owner", ownerUUID);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        tickCount = tag.getInt("Age");
        setDuration(tag.getInt("Duration"));
        if (tag.hasUUID("Owner")) {
            ownerUUID = tag.getUUID("Owner");
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
