package com.waylays.arcanes.entity.book;

import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.book.BookFx;
import net.minecraft.core.particles.ParticleTypes;
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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Living Ink: a wave of ink spreading forward over the ground in a widening cone (along this entity's yaw). Every
 * enemy it washes over is seized by {@link InkHands}. The ink stain stays a moment after the wave is spent.
 */
public class InkWave extends Entity {
    public static final float SPEED = .8f;
    public static final int FADE_TICKS = 30;

    private static final EntityDataAccessor<Float> DATA_LENGTH = SynchedEntityData.defineId(InkWave.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUUID;
    private float damage;
    private int holdTicks;
    private final Set<UUID> seized = new HashSet<>();

    public InkWave(EntityType<? extends InkWave> entityType, Level level) {
        super(entityType, level);
        noPhysics = true;
    }

    public InkWave(Level level, LivingEntity owner, float damage, int holdTicks, float length) {
        this(ModEntities.INK_WAVE.get(), level);
        this.ownerUUID = owner.getUUID();
        this.damage = damage;
        this.holdTicks = holdTicks;
        this.entityData.set(DATA_LENGTH, length);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_LENGTH, 10f);
    }

    public float getLength() {
        return this.entityData.get(DATA_LENGTH);
    }

    /** How far the wave has spread, in blocks. */
    public float reach(float age) {
        return Math.min(getLength(), age * SPEED);
    }

    public static float halfWidth(float distance) {
        return .8f + distance * .28f;
    }

    public Vec3 forward() {
        return Vec3.directionFromRotation(0, getYRot());
    }

    public int spreadTicks() {
        return (int) Math.ceil(getLength() / SPEED);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (tickCount > spreadTicks() + FADE_TICKS) {
            discard();
            return;
        }
        if (tickCount > spreadTicks()) {
            return;
        }
        Vec3 forward = forward();
        Vec3 side = new Vec3(-forward.z, 0, forward.x);
        float front = reach(tickCount);
        float back = Math.max(0, front - SPEED * 1.5f);
        // Ink boiling up along the front of the wave
        float half = halfWidth(front);
        for (int i = 0; i < 4; i++) {
            Vec3 p = SpellUtil.groundAt(level(), position().add(forward.scale(front)).add(side.scale((random.nextFloat() * 2 - 1) * half)), 2, 1);
            SpellUtil.particles(level(), ParticleTypes.SQUID_INK, p.x, p.y + .1, p.z, 2, .1, .05, .1, .02);
            SpellUtil.particles(level(), i % 2 == 0 ? BookFx.INK_DUST : BookFx.INK_SHEEN_DUST, p.x, p.y + .2, p.z, 2, .2, .1, .2, 0);
        }
        if (tickCount % 3 == 1) {
            level().playSound(null, getX() + forward.x * front, getY(), getZ() + forward.z * front, SoundEvents.SQUID_SQUIRT, SoundSource.PLAYERS, 1f, .5f + random.nextFloat() * .2f);
        }
        Entity owner = ownerUUID != null && level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(ownerUUID) : null;
        double searchRadius = front + 2;
        for (LivingEntity e : SpellUtil.enemiesAround(level(), position(), searchRadius, owner, x -> !seized.contains(x.getUUID()))) {
            Vec3 rel = e.position().subtract(position());
            double along = rel.dot(forward);
            double across = Math.abs(rel.dot(side));
            if (along >= back - .5 && along <= front + .5 && across <= halfWidth((float) along) + e.getBbWidth() * .5 && Math.abs(rel.y) < 3) {
                seized.add(e.getUUID());
                level().addFreshEntity(new InkHands(level(), owner instanceof LivingEntity living ? living : null, e, damage, holdTicks));
            }
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(getLength() + 2);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
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
