package com.waylays.arcanes.entity.explosion;

import com.waylays.arcanes.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * Purely visual: the fireball, white-hot flash and expanding shock rings of one Explosion school blast.
 * Spawned on the server so every nearby client draws it; never saved.
 */
public class BlastEffect extends Entity {
    /** Orange fireball with a violet shock ring. */
    public static final int STYLE_FIRE = 0;
    /** Violet-white arcane burst (Implosion). */
    public static final int STYLE_ARCANE = 1;
    /** Blinding white dome (Cataclysm). */
    public static final int STYLE_WHITE = 2;

    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(BlastEffect.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_STYLE = SynchedEntityData.defineId(BlastEffect.class, EntityDataSerializers.INT);

    public BlastEffect(EntityType<? extends BlastEffect> entityType, Level level) {
        super(entityType, level);
        noPhysics = true;
    }

    public static void spawn(Level level, Vec3 at, float size, int style) {
        if (level.isClientSide) {
            return;
        }
        BlastEffect effect = new BlastEffect(ModEntities.BLAST_EFFECT.get(), level);
        effect.entityData.set(DATA_SIZE, size);
        effect.entityData.set(DATA_STYLE, style);
        effect.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360f, 0);
        level.addFreshEntity(effect);
    }

    public float getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    public int getStyle() {
        return this.entityData.get(DATA_STYLE);
    }

    public int lifetime() {
        return (int) (10 + getSize() * 4);
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount > lifetime()) {
            discard();
        }
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_SIZE, 1f);
        this.entityData.define(DATA_STYLE, STYLE_FIRE);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(getSize() * 6);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
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
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
