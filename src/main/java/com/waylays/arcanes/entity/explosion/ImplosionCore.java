package com.waylays.arcanes.entity.explosion;

import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.explosion.ExplosionFx;
import com.waylays.arcanes.spells.explosion.ImplosionSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
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
import java.util.UUID;

/**
 * Implosion: a point of black light that drags every enemy around it into its centre, contracts, then goes off in a
 * blinding white sphere.
 */
public class ImplosionCore extends Entity implements AntiMagicSusceptible {
    public static final int CONTRACT_TICK = 40;
    public static final int DETONATE_TICK = 52;

    private static final EntityDataAccessor<Float> DATA_PULL_RADIUS = SynchedEntityData.defineId(ImplosionCore.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUUID;
    private float damage;

    public ImplosionCore(EntityType<? extends ImplosionCore> entityType, Level level) {
        super(entityType, level);
        noPhysics = true;
    }

    public ImplosionCore(Level level, LivingEntity owner, float damage, float pullRadius) {
        this(ModEntities.IMPLOSION_CORE.get(), level);
        this.ownerUUID = owner.getUUID();
        this.damage = damage;
        this.entityData.set(DATA_PULL_RADIUS, pullRadius);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_PULL_RADIUS, 6f);
    }

    public float getPullRadius() {
        return this.entityData.get(DATA_PULL_RADIUS);
    }

    public Vec3 center() {
        return position().add(0, getBbHeight() * .5, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientParticles();
            return;
        }
        Vec3 c = center();
        if (tickCount == 1) {
            level().playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 2f, .5f);
            level().playSound(null, c.x, c.y, c.z, SoundEvents.PORTAL_TRIGGER, SoundSource.NEUTRAL, .6f, 1.6f);
        }
        if (tickCount % 12 == 0 && tickCount < CONTRACT_TICK) {
            level().playSound(null, c.x, c.y, c.z, SoundEvents.PORTAL_AMBIENT, SoundSource.NEUTRAL, 1.2f, .6f + tickCount * .02f);
        }
        if (tickCount == CONTRACT_TICK) {
            level().playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.NEUTRAL, 2.5f, .6f);
            ExplosionFx.whine(level(), c, 2f);
        }
        if (tickCount < DETONATE_TICK) {
            pull(c);
        } else {
            detonate(c);
        }
    }

    private void pull(Vec3 c) {
        Entity owner = ownerUUID != null && level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(ownerUUID) : null;
        float radius = getPullRadius();
        // The pull grows stronger as the core contracts
        double force = tickCount < CONTRACT_TICK ? .09 : .2;
        for (LivingEntity e : SpellUtil.enemiesAround(level(), c, radius, owner, x -> true)) {
            Vec3 toward = c.subtract(e.getBoundingBox().getCenter());
            double dist = toward.length();
            if (dist < .6) {
                e.setDeltaMovement(e.getDeltaMovement().scale(.3));
            } else {
                double strength = force * (.5 + .5 * (1 - dist / radius));
                e.setDeltaMovement(e.getDeltaMovement().scale(.75).add(toward.scale(strength / dist)));
            }
            e.fallDistance = 0;
            e.hurtMarked = true;
        }
    }

    private void detonate(Vec3 c) {
        Entity owner = ownerUUID != null && level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(ownerUUID) : null;
        ExplosionFx.detonate(level(), c, 2.4f, ImplosionSpell.RADIUS, damage, ModSpells.IMPLOSION.get(), this, owner, BlastEffect.STYLE_WHITE);
        SpellUtil.particles(level(), new BlastwaveParticleOptions(ExplosionFx.ARCANE, 7f), c.x, c.y, c.z, 1, 0, 0, 0, 0);
        SpellUtil.particles(level(), ParticleTypes.END_ROD, c.x, c.y, c.z, 40, .2, .2, .2, .5);
        level().playSound(null, c.x, c.y, c.z, SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.NEUTRAL, 2f, .5f);
        discard();
    }

    private void clientParticles() {
        Level level = level();
        Vec3 c = center();
        float radius = getPullRadius();
        boolean contracting = tickCount >= CONTRACT_TICK;
        // Light and dust sucked into the core (a portal particle travels from its offset back to its origin)
        for (int i = 0; i < (contracting ? 8 : 4); i++) {
            Vec3 dir = new Vec3(random.nextGaussian(), random.nextGaussian() * .6, random.nextGaussian()).normalize().scale(radius * (.5 + random.nextFloat() * .5));
            level.addParticle(ParticleTypes.REVERSE_PORTAL, c.x + dir.x, c.y + dir.y, c.z + dir.z, -dir.x * .06, -dir.y * .06, -dir.z * .06);
            level.addParticle(ParticleTypes.PORTAL, c.x, c.y, c.z, dir.x, dir.y, dir.z);
        }
        if (random.nextInt(2) == 0) {
            Vec3 dir = new Vec3(random.nextGaussian(), random.nextGaussian() * .4, random.nextGaussian()).normalize().scale(2.5);
            level.addParticle(ExplosionFx.ARCANE_DUST, c.x + dir.x, c.y + dir.y, c.z + dir.z, -dir.x * .15, -dir.y * .15, -dir.z * .15);
        }
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        discard();
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(4);
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
        tag.putFloat("PullRadius", getPullRadius());
        if (ownerUUID != null) {
            tag.putUUID("Owner", ownerUUID);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        tickCount = tag.getInt("Age");
        damage = tag.getFloat("Damage");
        this.entityData.set(DATA_PULL_RADIUS, tag.getFloat("PullRadius"));
        if (tag.hasUUID("Owner")) {
            ownerUUID = tag.getUUID("Owner");
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
