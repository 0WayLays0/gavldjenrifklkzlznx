package com.waylays.arcanes.entity.rune;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.rune.RuneFx;
import com.waylays.arcanes.spells.rune.RuneSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;

/**
 * A rune carved into the ground. Three kinds:
 * <ul>
 * <li>WARD, a circle: the caster's allies standing in it are shielded (Resistance);</li>
 * <li>PATH, a strip of rune tiles: allies walking on it run and jump further;</li>
 * <li>SNARE, a small rune barely glowing: the first enemy to step on it makes it burst out in stone spikes.</li>
 * </ul>
 * Position: the circle's centre, or the start of the strip; yaw: the strip's direction.
 */
public class RuneSigil extends SpellEffectEntity {
    public enum Kind { WARD, PATH, SNARE }

    /** Ticks it takes to light up, and to fade at the end. */
    public static final int LIGHT_TICKS = 12;
    public static final int FADE_TICKS = 20;
    /** A snare is harmless this long after being carved. */
    public static final int ARM_TICKS = 20;
    public static final int BURST_TICKS = 16;
    public static final float SNARE_RADIUS = 1.2f;
    public static final float BURST_RADIUS = 2.6f;
    public static final int MAX_SNARES = 3;

    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(RuneSigil.class, EntityDataSerializers.INT);
    /** Circle radius, or strip length. */
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(RuneSigil.class, EntityDataSerializers.FLOAT);
    /** Strip width. */
    private static final EntityDataAccessor<Float> DATA_WIDTH = SynchedEntityData.defineId(RuneSigil.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(RuneSigil.class, EntityDataSerializers.INT);
    /** Tick the snare burst (0: not yet). */
    private static final EntityDataAccessor<Integer> DATA_BURST = SynchedEntityData.defineId(RuneSigil.class, EntityDataSerializers.INT);

    private int spellLevel = 1;
    /** Client side: the renderer's record of the ground under a path's tiles. */
    @Nullable
    public Object clientCache;

    public RuneSigil(EntityType<? extends RuneSigil> entityType, Level level) {
        super(entityType, level);
    }

    /** Carves a rune and adds it to the world. */
    public static RuneSigil carve(Level level, LivingEntity caster, Kind kind, Vec3 at, float yaw, float size, float width, int life, int spellLevel, float damage) {
        RuneSigil sigil = new RuneSigil(ModEntities.RUNE_SIGIL.get(), level);
        sigil.setOwner(caster);
        sigil.moveTo(at.x, at.y, at.z, yaw, 0);
        sigil.entityData.set(DATA_KIND, kind.ordinal());
        sigil.entityData.set(DATA_SIZE, size);
        sigil.entityData.set(DATA_WIDTH, width);
        sigil.entityData.set(DATA_LIFE, life);
        sigil.spellLevel = spellLevel;
        sigil.damage = damage;
        if (kind == Kind.SNARE) {
            // The oldest of the caster's snares crumbles when there are too many
            List<RuneSigil> snares = level.getEntitiesOfClass(RuneSigil.class, caster.getBoundingBox().inflate(96),
                    s -> s.getKind() == Kind.SNARE && caster.getUUID().equals(s.getOwnerUUID()) && s.burstAge(0) < 0);
            // Newest first
            snares.sort(Comparator.comparingInt(s -> s.tickCount));
            for (int i = MAX_SNARES - 1; i < snares.size(); i++) {
                snares.get(i).discard();
            }
        }
        level.addFreshEntity(sigil);
        return sigil;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, 0);
        builder.define(DATA_SIZE, 1f);
        builder.define(DATA_WIDTH, 1f);
        builder.define(DATA_LIFE, 200);
        builder.define(DATA_BURST, 0);
    }

    public Kind getKind() {
        Kind[] all = Kind.values();
        return all[Mth.clamp(this.entityData.get(DATA_KIND), 0, all.length - 1)];
    }

    public float getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    public float getWidth() {
        return this.entityData.get(DATA_WIDTH);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    /** Ticks since the snare burst, or -1. */
    public float burstAge(float partialTicks) {
        int at = this.entityData.get(DATA_BURST);
        return at == 0 ? -1 : tickCount + partialTicks - at;
    }

    /** How lit the rune is: 0 to 1 while it lights up, 1, then back to 0 as it fades out. */
    public float strength(float partialTicks) {
        float age = tickCount + partialTicks;
        return Mth.clamp(Math.min(age / LIGHT_TICKS, (getLife() - age) / FADE_TICKS), 0, 1);
    }

    public Vec3 forward() {
        return RuneSpell.direction(getYRot());
    }

    /** Whether a point stands on the rune (a strip's tiles, or the circle), up to two blocks above it. */
    public boolean covers(Vec3 p) {
        double dy = p.y - getY();
        if (dy < -1 || dy > 2) {
            return false;
        }
        double dx = p.x - getX();
        double dz = p.z - getZ();
        if (getKind() != Kind.PATH) {
            return dx * dx + dz * dz <= getSize() * getSize();
        }
        Vec3 f = forward();
        double along = dx * f.x + dz * f.z;
        double across = dx * f.z - dz * f.x;
        return along >= -.5 && along <= getSize() + .5 && Math.abs(across) <= getWidth() * .5;
    }

    private AABB area() {
        double reach = getKind() == Kind.PATH ? getSize() + getWidth() : getSize();
        return new AABB(getX() - reach, getY() - 1, getZ() - reach, getX() + reach, getY() + 2.5, getZ() + reach);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientTick();
            return;
        }
        if (tickCount >= getLife()) {
            discard();
            return;
        }
        LivingEntity caster = getLivingOwner();
        switch (getKind()) {
            case WARD -> {
                if (tickCount % 10 == 0 && caster != null) {
                    for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, area(), x -> covers(x.position()) && RuneFx.helps(caster, x))) {
                        e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, spellLevel >= 4 ? 1 : 0, true, true, true));
                    }
                }
            }
            case PATH -> {
                if (tickCount % 5 == 0 && caster != null) {
                    for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, area(), x -> covers(x.position()) && RuneFx.helps(caster, x))) {
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, spellLevel >= 5 ? 2 : 1, true, false, true));
                        e.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 0, true, false, true));
                    }
                }
            }
            case SNARE -> {
                int burst = this.entityData.get(DATA_BURST);
                if (burst != 0) {
                    if (tickCount - burst >= BURST_TICKS) {
                        discard();
                    }
                } else if (tickCount >= ARM_TICKS
                        && !SpellUtil.enemiesAround(level(), position().add(0, .5, 0), SNARE_RADIUS + .5, getOwner(), e -> !e.isSpectator() && covers(e.position())).isEmpty()) {
                    burst((ServerLevel) level());
                }
            }
        }
    }

    /** The snare goes off: stone spikes burst out of it, hurting, slowing and throwing up whatever stands around. */
    private void burst(ServerLevel level) {
        this.entityData.set(DATA_BURST, Math.max(1, tickCount));
        Entity owner = getOwner();
        Vec3 c = position();
        for (LivingEntity e : SpellUtil.enemiesAround(level, c.add(0, .5, 0), BURST_RADIUS, owner, x -> true)) {
            if (DamageSources.applyDamage(e, damage, SpellUtil.damageSource(ModSpells.SNARE_RUNE.get(), this, owner))) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
                e.setDeltaMovement(e.getDeltaMovement().add(0, .55 * resist, 0));
                e.hurtMarked = true;
            }
        }
        RuneFx.stone(level, c.add(0, .3, 0), Blocks.DEEPSLATE.defaultBlockState(), 40, 1.2);
        RuneFx.stone(level, c.add(0, .3, 0), Blocks.POINTED_DRIPSTONE.defaultBlockState(), 20, .8);
        RuneFx.glyphs(level, c.add(0, .6, 0), 24, .9);
        SpellUtil.particles(level, ParticleTypes.EXPLOSION, c.x, c.y + .4, c.z, 1, 0, 0, 0, 0);
        RuneFx.sound(level, c, SoundEvents.POINTED_DRIPSTONE_LAND, 2f, .6f);
        RuneFx.sound(level, c, SoundEvents.DEEPSLATE_BREAK, 2f, .7f);
        RuneFx.sound(level, c, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.2f, .8f);
    }

    private void clientTick() {
        float lit = strength(0);
        if (lit <= 0) {
            return;
        }
        switch (getKind()) {
            case WARD -> {
                // Glyphs drifting up off the edge of the circle
                if (random.nextInt(3) == 0) {
                    double a = random.nextDouble() * Mth.TWO_PI;
                    double r = getSize() * (.85 + random.nextDouble() * .15);
                    level().addParticle(ParticleTypes.ENCHANT, getX() + Math.cos(a) * r, getY() + .2, getZ() + Math.sin(a) * r, 0, .6, 0);
                }
            }
            case PATH -> {
                if (random.nextInt(2) == 0) {
                    Vec3 f = forward();
                    double along = random.nextDouble() * getSize();
                    double across = (random.nextDouble() - .5) * getWidth();
                    level().addParticle(RuneFx.GLOW_DUST, getX() + f.x * along + f.z * across, getY() + .1, getZ() + f.z * along - f.x * across, 0, .02, 0);
                }
            }
            case SNARE -> {
                if (burstAge(0) < 0 && tickCount >= ARM_TICKS && random.nextInt(14) == 0) {
                    level().addParticle(RuneFx.GLOW_DUST, getX() + (random.nextDouble() - .5) * SNARE_RADIUS, getY() + .1,
                            getZ() + (random.nextDouble() - .5) * SNARE_RADIUS, 0, .01, 0);
                }
            }
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}
