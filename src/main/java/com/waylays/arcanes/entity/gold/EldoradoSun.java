package com.waylays.arcanes.entity.gold;

import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.gold.GoldFx;
import com.waylays.arcanes.spells.gold.SunOfEldoradoSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Sun of Eldorado: a golden sun hanging above the caster. It grows, fires golden beams at the nearest enemies every
 * half second, then collapses and explodes. Beam targets are synced so the client can draw the beams.
 */
public class EldoradoSun extends Entity {
    public static final int GROW_TICKS = 20;
    public static final int FIRST_VOLLEY = 24;
    public static final int VOLLEY_INTERVAL = 10;
    public static final int COLLAPSE_TICK = 150;
    public static final int EXPLODE_TICK = 162;
    public static final int MAX_BEAMS = 3;
    /** Ticks a beam stays visible after a volley. */
    public static final int BEAM_TICKS = 5;

    private static final List<EntityDataAccessor<Integer>> DATA_BEAMS = List.of(
            SynchedEntityData.defineId(EldoradoSun.class, EntityDataSerializers.INT),
            SynchedEntityData.defineId(EldoradoSun.class, EntityDataSerializers.INT),
            SynchedEntityData.defineId(EldoradoSun.class, EntityDataSerializers.INT));
    private static final EntityDataAccessor<Integer> DATA_VOLLEY_TICK = SynchedEntityData.defineId(EldoradoSun.class, EntityDataSerializers.INT);

    private float beamDamage;
    private float explosionDamage;
    @Nullable
    private UUID ownerUUID;

    public EldoradoSun(EntityType<? extends EldoradoSun> entityType, Level level) {
        super(entityType, level);
        noPhysics = true;
    }

    public EldoradoSun(Level level, LivingEntity owner, float beamDamage, float explosionDamage) {
        this(ModEntities.ELDORADO_SUN.get(), level);
        this.ownerUUID = owner.getUUID();
        this.beamDamage = beamDamage;
        this.explosionDamage = explosionDamage;
    }

    @Override
    protected void defineSynchedData() {
        for (EntityDataAccessor<Integer> beam : DATA_BEAMS) {
            this.entityData.define(beam, -1);
        }
        this.entityData.define(DATA_VOLLEY_TICK, -100);
    }

    /** Entity ids hit by the last volley (-1 = none). */
    public int getBeamTarget(int index) {
        return this.entityData.get(DATA_BEAMS.get(index));
    }

    public int getVolleyTick() {
        return this.entityData.get(DATA_VOLLEY_TICK);
    }

    @Nullable
    private Entity getOwner() {
        return ownerUUID != null && level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(ownerUUID) : null;
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
        if (tickCount == 1) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 3f, .7f);
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.BELL_RESONATE, SoundSource.NEUTRAL, 2f, 1f);
        }
        if (tickCount >= FIRST_VOLLEY && tickCount < COLLAPSE_TICK && (tickCount - FIRST_VOLLEY) % VOLLEY_INTERVAL == 0) {
            volley();
        }
        if (tickCount == COLLAPSE_TICK) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.NEUTRAL, 3f, .5f);
        }
        if (tickCount >= EXPLODE_TICK) {
            explode();
        }
    }

    private void volley() {
        Entity owner = getOwner();
        Vec3 from = center();
        List<LivingEntity> targets = SpellUtil.enemiesAround(level(), from, SunOfEldoradoSpell.RANGE, owner, this::canSee);
        targets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(from)));
        for (int i = 0; i < MAX_BEAMS; i++) {
            LivingEntity target = i < targets.size() ? targets.get(i) : null;
            this.entityData.set(DATA_BEAMS.get(i), target == null ? -1 : target.getId());
            if (target != null) {
                DamageSources.applyDamage(target, beamDamage, SpellUtil.damageSource(ModSpells.SUN_OF_ELDORADO.get(), this, owner));
                Vec3 hit = target.getBoundingBox().getCenter();
                SpellUtil.particles(level(), GoldFx.sparks(), hit.x, hit.y, hit.z, 8, .2, .2, .2, .3);
                target.setSecondsOnFire(2);
            }
        }
        if (!targets.isEmpty()) {
            this.entityData.set(DATA_VOLLEY_TICK, tickCount);
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.NEUTRAL, 1.5f, 1.4f + random.nextFloat() * .3f);
        }
    }

    private boolean canSee(LivingEntity target) {
        Vec3 from = center();
        return level().clip(new ClipContext(from, target.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }

    private void explode() {
        Entity owner = getOwner();
        Vec3 c = center();
        for (LivingEntity e : SpellUtil.enemiesAround(level(), c, SunOfEldoradoSpell.EXPLOSION_RADIUS, owner, x -> true)) {
            if (DamageSources.applyDamage(e, explosionDamage, SpellUtil.damageSource(ModSpells.SUN_OF_ELDORADO.get(), this, owner))) {
                Vec3 push = e.position().subtract(c);
                push = push.lengthSqr() < 1.0E-4 ? new Vec3(0, 1, 0) : push.normalize();
                e.push(push.x * 1.2, .5, push.z * 1.2);
                e.hurtMarked = true;
            }
        }
        GoldFx.shatterBurst(level(), c.x, c.y, c.z, 3.2f);
        SpellUtil.particles(level(), GoldFx.sparks(), c.x, c.y, c.z, 60, .5, .5, .5, .9);
        SpellUtil.particles(level(), GoldFx.nuggetShards(), c.x, c.y, c.z, 50, .5, .5, .5, .6);
        SpellUtil.cameraShake(level(), c, 20, 24);
        level().playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL, 4f, .7f);
        level().playSound(null, c.x, c.y, c.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.NEUTRAL, 3f, .5f);
        level().playSound(null, c.x, c.y, c.z, SoundEvents.BELL_BLOCK, SoundSource.NEUTRAL, 3f, .5f);
        discard();
    }

    private void clientParticles() {
        Level level = level();
        Vec3 c = center();
        if (tickCount < COLLAPSE_TICK) {
            // Glitter falling from the sun like golden rain
            for (int i = 0; i < 2; i++) {
                level.addParticle(GoldFx.PALE_DUST, c.x + (random.nextFloat() - .5f) * 4, c.y - 1, c.z + (random.nextFloat() - .5f) * 4, 0, -.08, 0);
            }
            if (random.nextInt(3) == 0) {
                level.addParticle(GoldFx.sparks(), c.x, c.y, c.z, (random.nextFloat() - .5f) * .6f, (random.nextFloat() - .5f) * .6f, (random.nextFloat() - .5f) * .6f);
            }
        } else {
            // Collapse: light is sucked into the core
            for (int i = 0; i < 6; i++) {
                Vec3 dir = new Vec3(random.nextFloat() - .5f, random.nextFloat() - .5f, random.nextFloat() - .5f).normalize().scale(3);
                level.addParticle(GoldFx.GOLD_DUST, c.x + dir.x, c.y + dir.y, c.z + dir.z, -dir.x * .25, -dir.y * .25, -dir.z * .25);
            }
        }
    }

    /** The sun and its beams reach far: keep rendering while any of it is on screen. */
    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(SunOfEldoradoSpell.RANGE);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
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
        tag.putFloat("BeamDamage", beamDamage);
        tag.putFloat("ExplosionDamage", explosionDamage);
        if (ownerUUID != null) {
            tag.putUUID("Owner", ownerUUID);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        tickCount = tag.getInt("Age");
        beamDamage = tag.getFloat("BeamDamage");
        explosionDamage = tag.getFloat("ExplosionDamage");
        if (tag.hasUUID("Owner")) {
            ownerUUID = tag.getUUID("Owner");
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
