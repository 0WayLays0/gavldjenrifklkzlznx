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
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import javax.annotation.Nullable;

/**
 * A glowing pool of molten gold left by Molten Torrent. While molten it burns and slows enemies standing in it; then it
 * cools into a plate of solid gold that holds their feet and creeps up their legs (a coat of gold every second), and at
 * last it breaks up into coins. New gobs landing on an existing pool make it molten again instead of stacking pools.
 */
public class MoltenPool extends Entity implements AntiMagicSusceptible {
    public static final float RADIUS = 1.2f;
    public static final int LIFETIME = 100;
    /** Ticks it stays as solid gold once cooled. */
    public static final int COOLED_TICKS = 80;
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
            if (!isMolten()) {
                if (random.nextInt(10) == 0) {
                    level.addParticle(GoldFx.PALE_DUST, getX() + (random.nextFloat() - .5f) * RADIUS * 1.6f, getY() + .1, getZ() + (random.nextFloat() - .5f) * RADIUS * 1.6f, 0, .01, 0);
                }
                return;
            }
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
        Entity owner = getOwner();
        AABB area = new AABB(getX() - RADIUS, getY() - .2, getZ() - RADIUS, getX() + RADIUS, getY() + .6, getZ() + RADIUS);
        if (isMolten()) {
            if (tickCount % DAMAGE_INTERVAL == 0) {
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area)) {
                    if (e.isAlive() && !SpellUtil.isAlly(owner, e)
                            && DamageSources.applyDamage(e, damage, SpellUtil.damageSource(ModSpells.MOLTEN_TORRENT.get(), this, owner))) {
                        e.igniteForSeconds(2);
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
                    }
                }
            }
        } else {
            if (tickCount == getExpires() + 1) {
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.LAVA_EXTINGUISH, SoundSource.NEUTRAL, .6f, 1.4f);
            }
            if (tickCount % 20 == 0) {
                // Solid gold holds the feet of those standing in it, and climbs
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area)) {
                    if (e.isAlive() && e.onGround() && !SpellUtil.isAlly(owner, e)) {
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 3));
                        GoldFx.gildEvery(e, 20);
                    }
                }
            }
            if (tickCount > getExpires() + COOLED_TICKS) {
                GoldFx.coins(level, position().add(0, .2, 0), 6, .25f);
                SpellUtil.particles(level, GoldFx.nuggetShards(), getX(), getY() + .1, getZ(), 10, RADIUS * .5, .05, RADIUS * .5, .1);
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.NEUTRAL, 1f, 1.4f);
                discard();
            }
        }
    }

    /** Molten until it has cooled; then solid gold. */
    public boolean isMolten() {
        return tickCount <= getExpires();
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        discard();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_EXPIRES, LIFETIME);
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
}
