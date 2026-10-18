package com.waylays.arcanes.entity.gold;

import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.gold.GoldFx;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
 * A glowing pool of molten gold left by Molten Torrent. Burns and slows enemies standing in it, then cools down.
 * New gobs landing on an existing pool refresh it instead of stacking pools.
 */
public class MoltenPool extends Entity implements AntiMagicSusceptible {
    public static final float RADIUS = 1.2f;
    public static final int LIFETIME = 100;
    private static final int DAMAGE_INTERVAL = 10;

    /** Tick (of this entity's own clock) at which the pool has cooled down; synced so the client fades it in time. */
    private static final EntityDataAccessor<Integer> DATA_EXPIRES = SynchedEntityData.defineId(MoltenPool.class, EntityDataSerializers.INT);

    private float damage;
    @Nullable
    private UUID ownerUUID;

    public MoltenPool(EntityType<? extends MoltenPool> entityType, Level level) {
        super(entityType, level);
    }

    /** Refreshes the pool already there, or pours a new one. */
    public static void spreadAt(Level level, @Nullable LivingEntity owner, Vec3 at, float damage) {
        for (MoltenPool pool : level.getEntitiesOfClass(MoltenPool.class, new AABB(at, at).inflate(RADIUS, .6, RADIUS))) {
            pool.setExpires(pool.tickCount + LIFETIME);
            return;
        }
        MoltenPool pool = new MoltenPool(ModEntities.MOLTEN_POOL.get(), level);
        pool.damage = damage;
        pool.ownerUUID = owner == null ? null : owner.getUUID();
        pool.moveTo(at.x, at.y + .01, at.z, level.random.nextFloat() * 360f, 0);
        level.addFreshEntity(pool);
    }

    @Nullable
    private Entity getOwner() {
        return ownerUUID != null && level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(ownerUUID) : null;
    }

    @Override
    public void tick() {
        super.tick();
        Level level = level();
        if (level.isClientSide) {
            if (random.nextInt(6) == 0) {
                double a = random.nextDouble() * Math.PI * 2;
                double r = random.nextDouble() * RADIUS * .8;
                level.addParticle(random.nextInt(3) == 0 ? ParticleTypes.LAVA : GoldFx.MOLTEN_DUST, getX() + Math.cos(a) * r, getY() + .05, getZ() + Math.sin(a) * r, 0, .02, 0);
            }
            if (random.nextInt(12) == 0) {
                level.addParticle(ParticleTypes.SMOKE, getX() + (random.nextFloat() - .5f) * RADIUS, getY() + .1, getZ() + (random.nextFloat() - .5f) * RADIUS, 0, .03, 0);
            }
            return;
        }
        if (tickCount % DAMAGE_INTERVAL == 0) {
            Entity owner = getOwner();
            AABB area = new AABB(getX() - RADIUS, getY() - .2, getZ() - RADIUS, getX() + RADIUS, getY() + .6, getZ() + RADIUS);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area)) {
                if (e.isAlive() && !SpellUtil.isAlly(owner, e)
                        && DamageSources.applyDamage(e, damage, SpellUtil.damageSource(ModSpells.MOLTEN_TORRENT.get(), this, owner))) {
                    e.setSecondsOnFire(2);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
                }
            }
        }
        if (tickCount > getExpires()) {
            discard();
        }
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        discard();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_EXPIRES, LIFETIME);
    }

    public int getExpires() {
        return this.entityData.get(DATA_EXPIRES);
    }

    private void setExpires(int tick) {
        this.entityData.set(DATA_EXPIRES, tick);
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
        tag.putInt("Expires", getExpires());
        tag.putFloat("Damage", damage);
        if (ownerUUID != null) {
            tag.putUUID("Owner", ownerUUID);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        tickCount = tag.getInt("Age");
        setExpires(tag.getInt("Expires"));
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
