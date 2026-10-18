package com.waylays.arcanes.entity.explosion;

import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.explosion.ExplosionFx;
import com.waylays.arcanes.spells.explosion.SigilsOfRuinSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * A rune of ruin glowing on the ground. Armed after {@link #ARM_TICKS}; an enemy stepping on it sets it off, and a
 * blast sets off the owner's other sigils nearby in a cascade.
 */
public class RuinSigil extends Entity implements AntiMagicSusceptible {
    public static final int ARM_TICKS = 15;
    public static final int FUSE_TICKS = 6;
    public static final int LIFETIME = 600;
    public static final float TRIGGER_RADIUS = 1.3f;
    public static final float CASCADE_RANGE = 6f;

    /** Tick at which the fuse was lit (-1: not lit); synced so the client flares the rune. */
    private static final EntityDataAccessor<Integer> DATA_LIT = SynchedEntityData.defineId(RuinSigil.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUUID;
    private float damage;

    public RuinSigil(EntityType<? extends RuinSigil> entityType, Level level) {
        super(entityType, level);
    }

    public RuinSigil(Level level, LivingEntity owner, float damage) {
        this(ModEntities.RUIN_SIGIL.get(), level);
        this.ownerUUID = owner.getUUID();
        this.damage = damage;
        setYRot(owner.getRandom().nextFloat() * 360f);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_LIT, -1);
    }

    public int getLitTick() {
        return this.entityData.get(DATA_LIT);
    }

    public boolean isLit() {
        return getLitTick() >= 0;
    }

    @Nullable
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    @Nullable
    private Entity getOwner() {
        return ownerUUID != null && level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(ownerUUID) : null;
    }

    /** Lights the fuse (no-op if already lit). */
    public void light(int extraDelay) {
        if (!isLit()) {
            this.entityData.set(DATA_LIT, tickCount + extraDelay);
            ExplosionFx.whine(level(), position(), 1.8f);
        }
    }

    @Override
    public void tick() {
        super.tick();
        Level level = level();
        if (level.isClientSide) {
            if (tickCount % 4 == 0) {
                double a = random.nextDouble() * Math.PI * 2;
                level.addParticle(isLit() ? ExplosionFx.FIRE_DUST : ExplosionFx.EMBER_DUST, getX() + Math.cos(a) * .9, getY() + .05, getZ() + Math.sin(a) * .9, 0, .03, 0);
            }
            return;
        }
        if (!isLit()) {
            if (tickCount > LIFETIME) {
                discard();
                return;
            }
            if (tickCount >= ARM_TICKS && tickCount % 2 == 0) {
                Entity owner = getOwner();
                AABB area = new AABB(getX() - TRIGGER_RADIUS, getY() - .2, getZ() - TRIGGER_RADIUS, getX() + TRIGGER_RADIUS, getY() + 2, getZ() + TRIGGER_RADIUS);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area)) {
                    if (e.isAlive() && !SpellUtil.isAlly(owner, e)) {
                        light(0);
                        break;
                    }
                }
            }
            return;
        }
        if (tickCount >= getLitTick() + FUSE_TICKS) {
            explode();
        }
    }

    private void explode() {
        Entity owner = getOwner();
        ExplosionFx.detonate(level(), position().add(0, .6, 0), 1.4f, SigilsOfRuinSpell.RADIUS, damage, ModSpells.SIGILS_OF_RUIN.get(), this, owner);
        // Cascade: the blast sets off the owner's other sigils nearby, one after the other
        for (RuinSigil other : level().getEntitiesOfClass(RuinSigil.class, getBoundingBox().inflate(CASCADE_RANGE))) {
            if (other != this && ownerUUID != null && ownerUUID.equals(other.ownerUUID)) {
                other.light((int) (other.distanceTo(this) * 1.5f));
            }
        }
        discard();
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
        tag.putFloat("Damage", damage);
        if (ownerUUID != null) {
            tag.putUUID("Owner", ownerUUID);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        tickCount = tag.getInt("Age");
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
