package com.waylays.arcanes.entity.rune;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.rune.RuneFx;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.BlockParticleOption;
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

/**
 * A standing stone carved with runes. Two kinds:
 * <ul>
 * <li>MENDING: rises slowly, then every two seconds its runes pulse and heal the caster's allies around it;</li>
 * <li>MENHIR: bursts out of the ground at its set tick, throwing up and hurting whatever stands on it, then crumbles.</li>
 * </ul>
 * Times are game ticks, so a line of menhirs set off one after another stays in step on every client.
 */
public class Runestone extends SpellEffectEntity {
    public enum Kind { MENDING, MENHIR }

    public static final int MENDING_RISE = 14;
    public static final int MENHIR_RISE = 3;
    public static final int SINK_TICKS = 10;
    public static final int PULSE_TICKS = 40;
    public static final float MENDING_RADIUS = 5.5f;
    /** How long a menhir stands before crumbling. */
    public static final int MENHIR_HOLD = 28;

    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(Runestone.class, EntityDataSerializers.INT);
    /** Game tick it starts to rise, and the one it starts to sink. */
    private static final EntityDataAccessor<Integer> DATA_START = SynchedEntityData.defineId(Runestone.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_END = SynchedEntityData.defineId(Runestone.class, EntityDataSerializers.INT);
    /** Game tick of the last healing pulse. */
    private static final EntityDataAccessor<Integer> DATA_PULSE = SynchedEntityData.defineId(Runestone.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SCALE = SynchedEntityData.defineId(Runestone.class, EntityDataSerializers.FLOAT);

    private boolean erupted;

    public Runestone(EntityType<? extends Runestone> entityType, Level level) {
        super(entityType, level);
    }

    private static Runestone create(Level level, LivingEntity caster, Kind kind, Vec3 at, int start, int end, float scale, float amount) {
        Runestone stone = new Runestone(ModEntities.RUNESTONE.get(), level);
        stone.setOwner(caster);
        // A little turned, so a row of stones does not look cast from one mould
        stone.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360, 0);
        stone.entityData.set(DATA_KIND, kind.ordinal());
        stone.entityData.set(DATA_START, start);
        stone.entityData.set(DATA_END, end);
        stone.entityData.set(DATA_SCALE, scale);
        stone.damage = amount;
        level.addFreshEntity(stone);
        return stone;
    }

    /** A healing stone standing {@code life} ticks, healing {@code heal} per pulse. The caster's previous one crumbles. */
    public static Runestone mending(Level level, LivingEntity caster, Vec3 at, int life, float heal) {
        for (Runestone old : level.getEntitiesOfClass(Runestone.class, caster.getBoundingBox().inflate(96),
                s -> s.getKind() == Kind.MENDING && caster.getUUID().equals(s.getOwnerUUID()))) {
            old.crumble();
        }
        int now = (int) level.getGameTime();
        return create(level, caster, Kind.MENDING, at, now, now + life, 1f, heal);
    }

    /** A menhir bursting out at game tick {@code start}. */
    public static Runestone menhir(Level level, LivingEntity caster, Vec3 at, int start, float scale, float damage) {
        return create(level, caster, Kind.MENHIR, at, start, start + MENHIR_RISE + MENHIR_HOLD, scale, damage);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, 0);
        builder.define(DATA_START, 0);
        builder.define(DATA_END, 0);
        builder.define(DATA_PULSE, Integer.MIN_VALUE / 2);
        builder.define(DATA_SCALE, 1f);
    }

    public Kind getKind() {
        Kind[] all = Kind.values();
        return all[Mth.clamp(this.entityData.get(DATA_KIND), 0, all.length - 1)];
    }

    public int getStart() {
        return this.entityData.get(DATA_START);
    }

    public int getEnd() {
        return this.entityData.get(DATA_END);
    }

    public float getScale() {
        return this.entityData.get(DATA_SCALE);
    }

    public int riseTicks() {
        return getKind() == Kind.MENDING ? MENDING_RISE : MENHIR_RISE;
    }

    private float now(float partialTicks) {
        return (int) level().getGameTime() + partialTicks;
    }

    /** How far out of the ground: 0 buried, 1 fully risen (a menhir overshoots a little as it bursts out). */
    public float risen(float partialTicks) {
        float t = now(partialTicks);
        float up = Mth.clamp((t - getStart()) / riseTicks(), 0, 1);
        float rise = getKind() == Kind.MENHIR ? up * (1 + .25f * Mth.sin(up * Mth.PI)) : 1 - (1 - up) * (1 - up);
        float down = Mth.clamp((t - getEnd()) / SINK_TICKS, 0, 1);
        return rise * (1 - down * down);
    }

    /** Ticks since the last healing pulse. */
    public float sincePulse(float partialTicks) {
        return now(partialTicks) - this.entityData.get(DATA_PULSE);
    }

    /** Ticks since it started to sink (negative before). */
    public float sinking(float partialTicks) {
        return now(partialTicks) - getEnd();
    }

    /** Starts to sink now. */
    public void crumble() {
        int now = (int) level().getGameTime();
        if (getEnd() > now) {
            this.entityData.set(DATA_END, now);
        }
    }

    @Override
    public void tick() {
        super.tick();
        int now = (int) level().getGameTime();
        if (level().isClientSide) {
            // Dust and chips round the foot while it moves through the ground
            if ((now >= getStart() && now < getStart() + riseTicks() + 2 || now >= getEnd() && now < getEnd() + SINK_TICKS) && random.nextInt(2) == 0) {
                double a = random.nextDouble() * Mth.TWO_PI;
                double r = .6 * getScale();
                level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE.defaultBlockState()),
                        getX() + Math.cos(a) * r, getY() + .1, getZ() + Math.sin(a) * r, 0, .2, 0);
            }
            return;
        }
        if (now >= getEnd() + SINK_TICKS || tickCount > 20 * 120) {
            discard();
            return;
        }
        if (getKind() == Kind.MENDING) {
            int since = now - getStart() - MENDING_RISE;
            if (since >= 0 && now < getEnd() && since % PULSE_TICKS == 0) {
                pulse((ServerLevel) level(), now);
            }
        } else if (!erupted && now >= getStart()) {
            erupted = true;
            erupt((ServerLevel) level());
        }
    }

    private void pulse(ServerLevel level, int now) {
        this.entityData.set(DATA_PULSE, now);
        LivingEntity caster = getLivingOwner();
        Vec3 c = position();
        AABB box = new AABB(c, c).inflate(MENDING_RADIUS, 3, MENDING_RADIUS);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                x -> x.distanceToSqr(c.x, x.getY(), c.z) <= MENDING_RADIUS * MENDING_RADIUS && RuneFx.helps(caster, x))) {
            if (e.getHealth() < e.getMaxHealth()) {
                e.heal(damage);
                SpellUtil.particles(level, RuneFx.MEND_DUST, e.getX(), e.getY() + e.getBbHeight() * .5, e.getZ(), 8, .3, .5, .3, 0);
                SpellUtil.particles(level, ParticleTypes.HAPPY_VILLAGER, e.getX(), e.getY() + e.getBbHeight() * .6, e.getZ(), 3, .3, .4, .3, 0);
            }
        }
        RuneFx.ring(level, c, MENDING_RADIUS, 28, RuneFx.MEND_DUST);
        RuneFx.sound(level, c, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4f, 1.3f);
        RuneFx.sound(level, c, SoundEvents.BEACON_POWER_SELECT, .6f, 1.8f);
    }

    /** The menhir bursts out: what stands on it is thrown up and hurt. */
    private void erupt(ServerLevel level) {
        Entity owner = getOwner();
        Vec3 c = position();
        float r = .9f * getScale() + .4f;
        for (LivingEntity e : SpellUtil.enemiesAround(level, c.add(0, 1, 0), r + 1.5, owner,
                x -> x.distanceToSqr(c.x, x.getY(), c.z) <= r * r && x.getY() > c.y - 1.5 && x.getY() < c.y + 2.5)) {
            if (DamageSources.applyDamage(e, damage, SpellUtil.damageSource(ModSpells.MENHIR_SURGE.get(), this, owner))) {
                double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
                e.setDeltaMovement(e.getDeltaMovement().multiply(.5, 0, .5).add(0, .85 * resist, 0));
                e.hurtMarked = true;
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
            }
        }
        RuneFx.stone(level, c.add(0, .4, 0), Blocks.DEEPSLATE.defaultBlockState(), 26, .7);
        RuneFx.glyphs(level, c.add(0, 1.2, 0), 10, .5);
        RuneFx.sound(level, c, SoundEvents.DEEPSLATE_BREAK, 1.6f, .55f);
        RuneFx.sound(level, c, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, .5f, .5f);
        SpellUtil.cameraShake(level, c, 6, 10);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
