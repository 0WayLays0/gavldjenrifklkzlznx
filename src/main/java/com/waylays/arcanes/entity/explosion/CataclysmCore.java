package com.waylays.arcanes.entity.explosion;

import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.explosion.CataclysmSpell;
import com.waylays.arcanes.spells.explosion.ExplosionFx;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
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
 * Cataclysm: a sphere of energy that swells above the caster for three seconds, drinking in the light around it, then
 * goes off in a giant dome of white light, followed by a crown of secondary explosions all around.
 */
public class CataclysmCore extends Entity {
    public static final int GROW_TICKS = 60;
    public static final int CROWN_INTERVAL = 2;
    public static final int CROWN_BLASTS = 12;
    public static final double CROWN_RADIUS = 8;
    public static final int END_TICK = GROW_TICKS + 4 + CROWN_BLASTS * CROWN_INTERVAL;

    @Nullable
    private UUID ownerUUID;
    private float damage;
    /** Height of the ground under the core, where the blast and its crown hit. */
    private double groundY;

    public CataclysmCore(EntityType<? extends CataclysmCore> entityType, Level level) {
        super(entityType, level);
        noPhysics = true;
    }

    public CataclysmCore(Level level, LivingEntity owner, float damage, double groundY) {
        this(ModEntities.CATACLYSM_CORE.get(), level);
        this.ownerUUID = owner.getUUID();
        this.damage = damage;
        this.groundY = groundY;
    }

    public Vec3 center() {
        return position().add(0, getBbHeight() * .5, 0);
    }

    public boolean hasExploded() {
        return tickCount >= GROW_TICKS;
    }

    @Nullable
    private Entity getOwner() {
        return ownerUUID != null && level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(ownerUUID) : null;
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
            level().playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 4f, .5f);
            level().playSound(null, c.x, c.y, c.z, SoundEvents.PORTAL_TRIGGER, SoundSource.NEUTRAL, 1f, .5f);
        }
        if (tickCount < GROW_TICKS && tickCount % 10 == 0) {
            float t = tickCount / (float) GROW_TICKS;
            level().playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_AMBIENT, SoundSource.NEUTRAL, 3f, .5f + t);
            SpellUtil.cameraShake(level(), c, 10, 24);
        }
        if (tickCount == GROW_TICKS - 12) {
            ExplosionFx.whine(level(), c, 1.2f);
        }
        if (tickCount == GROW_TICKS) {
            explode(c);
        }
        int crownTick = tickCount - GROW_TICKS - 4;
        if (crownTick >= 0 && crownTick % CROWN_INTERVAL == 0 && crownTick / CROWN_INTERVAL < CROWN_BLASTS) {
            crown(crownTick / CROWN_INTERVAL);
        }
        if (tickCount >= END_TICK) {
            discard();
        }
    }

    private void explode(Vec3 c) {
        Entity owner = getOwner();
        Vec3 ground = new Vec3(c.x, groundY, c.z);
        for (LivingEntity e : SpellUtil.enemiesAround(level(), ground.add(0, (c.y - groundY) * .5, 0), CataclysmSpell.RADIUS + (c.y - groundY) * .5, owner, x -> true)) {
            double dx = e.getX() - c.x;
            double dz = e.getZ() - c.z;
            double flat = Math.sqrt(dx * dx + dz * dz);
            if (flat > CataclysmSpell.RADIUS || e.getY() < groundY - 3 || e.getY() > c.y + 3) {
                continue;
            }
            float falloff = (float) (1 - .4 * flat / CataclysmSpell.RADIUS);
            if (DamageSources.applyDamage(e, damage * falloff, SpellUtil.damageSource(ModSpells.CATACLYSM.get(), this, owner))) {
                double push = flat < 1.0E-3 ? 0 : 1.6 * falloff / flat;
                e.push(dx * push, .8 * falloff, dz * push);
                e.hurtMarked = true;
            }
        }
        // The dome of white light, the blast hitting the ground and the shock wave sweeping the whole screen
        ExplosionFx.visuals(level(), c, 6f, BlastEffect.STYLE_WHITE);
        ExplosionFx.lightBlast(level(), ground.add(0, .3, 0), 3f, BlastEffect.STYLE_FIRE);
        SpellUtil.particles(level(), new BlastwaveParticleOptions(ExplosionFx.CORE, 16f), ground.x, groundY + .2, ground.z, 1, 0, 0, 0, 0);
        SpellUtil.particles(level(), new BlastwaveParticleOptions(ExplosionFx.ARCANE, 26f), ground.x, groundY + .2, ground.z, 1, 0, 0, 0, 0);
        SpellUtil.particles(level(), ParticleTypes.END_ROD, c.x, c.y, c.z, 80, .5, .5, .5, 1.2);
        SpellUtil.particles(level(), ParticleTypes.EXPLOSION_EMITTER, ground.x, groundY + 1, ground.z, 3, 3, .5, 3, 0);
        level().playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL, 8f, .45f);
        level().playSound(null, c.x, c.y, c.z, SoundEvents.END_GATEWAY_SPAWN, SoundSource.NEUTRAL, 6f, .6f);
        level().playSound(null, c.x, c.y, c.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.NEUTRAL, 5f, .6f);
        level().playSound(null, c.x, c.y, c.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.NEUTRAL, 4f, .5f);
        SpellUtil.cameraShake(level(), c, 50, 64);
    }

    /** One of the crown of secondary explosions, going round the blast point. */
    private void crown(int index) {
        double a = index * Math.PI * 2 / CROWN_BLASTS + (index % 2) * .2;
        double r = CROWN_RADIUS + (index % 3 - 1) * 1.2;
        Vec3 at = SpellUtil.groundAt(level(), new Vec3(getX() + Math.cos(a) * r, groundY, getZ() + Math.sin(a) * r), 4, 1);
        ExplosionFx.detonate(level(), at.add(0, .4, 0), 1.8f, CataclysmSpell.CROWN_BLAST_RADIUS, damage * .3f, ModSpells.CATACLYSM.get(), this, getOwner(),
                index % 2 == 0 ? BlastEffect.STYLE_FIRE : BlastEffect.STYLE_ARCANE);
    }

    private void clientParticles() {
        if (hasExploded()) {
            return;
        }
        Level level = level();
        Vec3 c = center();
        float t = tickCount / (float) GROW_TICKS;
        // Light drawn in from all around (a portal particle travels from its offset back to its origin)
        for (int i = 0; i < 4 + (int) (t * 8); i++) {
            Vec3 dir = new Vec3(random.nextGaussian(), random.nextGaussian() * .7, random.nextGaussian()).normalize().scale(5 + random.nextFloat() * 5);
            level.addParticle(ParticleTypes.REVERSE_PORTAL, c.x + dir.x, c.y + dir.y, c.z + dir.z, -dir.x * .05, -dir.y * .05, -dir.z * .05);
            if (i % 2 == 0) {
                level.addParticle(ExplosionFx.FIRE_DUST, c.x + dir.x * .5, c.y + dir.y * .5, c.z + dir.z * .5, -dir.x * .08, -dir.y * .08, -dir.z * .08);
            }
        }
        if (random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.END_ROD, c.x + random.nextGaussian() * 6, c.y + random.nextGaussian() * 3, c.z + random.nextGaussian() * 6, 0, 0, 0);
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(10);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
    }

    @Override
    protected void defineSynchedData() {
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
        tag.putDouble("GroundY", groundY);
        if (ownerUUID != null) {
            tag.putUUID("Owner", ownerUUID);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        tickCount = tag.getInt("Age");
        damage = tag.getFloat("Damage");
        groundY = tag.getDouble("GroundY");
        if (tag.hasUUID("Owner")) {
            ownerUUID = tag.getUUID("Owner");
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
