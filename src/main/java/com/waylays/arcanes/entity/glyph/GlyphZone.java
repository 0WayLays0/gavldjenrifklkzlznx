package com.waylays.arcanes.entity.glyph;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.glyph.GlyphElement;
import com.waylays.arcanes.spells.glyph.GlyphFx;
import com.waylays.arcanes.spells.glyph.GlyphReaction;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
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

import javax.annotation.Nullable;
import java.util.List;

/**
 * Where a lasting reaction went off: a pool on the ground (Magma, Mire) or a whirling column (Steam, Firestorm,
 * Blizzard, Thunderstorm, Typhoon, Sandstorm). It pulses every half second, hurting the enemies in it in its own way.
 */
public class GlyphZone extends SpellEffectEntity {
    public static final int PULSE_TICKS = 10;
    public static final int GROW_TICKS = 8;
    public static final int FADE_TICKS = 12;
    public static final float POOL_RADIUS = 3;
    public static final float VORTEX_RADIUS = 2.5f;

    private static final EntityDataAccessor<Integer> DATA_REACTION = SynchedEntityData.defineId(GlyphZone.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(GlyphZone.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;

    public GlyphZone(EntityType<? extends GlyphZone> entityType, Level level) {
        super(entityType, level);
    }

    /** Opens the zone of {@code reaction} at {@code at}, or keeps one of the same kind already there open longer. */
    public static void open(ServerLevel level, @Nullable Entity caster, AbstractSpell spell, GlyphReaction reaction, Vec3 at, int life, float damage) {
        List<GlyphZone> near = level.getEntitiesOfClass(GlyphZone.class, new AABB(at, at).inflate(POOL_RADIUS),
                z -> z.getReaction() == reaction && !z.isRemoved() && z.tickCount < z.getLife());
        if (!near.isEmpty()) {
            GlyphZone zone = near.get(0);
            zone.entityData.set(DATA_LIFE, zone.tickCount + life);
            zone.damage = Math.max(zone.damage, damage);
            return;
        }
        GlyphZone zone = new GlyphZone(ModEntities.GLYPH_ZONE.get(), level);
        zone.setOwner(caster);
        zone.spellId = spell.getSpellId();
        zone.damage = damage;
        zone.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360, 0);
        zone.entityData.set(DATA_REACTION, reaction.ordinal());
        zone.entityData.set(DATA_LIFE, life);
        level.addFreshEntity(zone);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_REACTION, GlyphReaction.STEAM.ordinal());
        builder.define(DATA_LIFE, 80);
    }

    public GlyphReaction getReaction() {
        return GlyphReaction.byId(this.entityData.get(DATA_REACTION));
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    public boolean isPool() {
        return getReaction().zone == GlyphReaction.Zone.POOL;
    }

    public float getRadius() {
        return isPool() ? POOL_RADIUS : VORTEX_RADIUS;
    }

    /** 0 to 1: grows in, then fades out at the end. */
    public float strength(float partialTicks) {
        float age = tickCount + partialTicks;
        return Mth.clamp(age / GROW_TICKS, 0, 1) * Mth.clamp(1 - (age - getLife()) / FADE_TICKS, 0, 1);
    }

    private boolean inside(Entity e) {
        double r = getRadius();
        return e.distanceToSqr(getX(), e.getY(), getZ()) <= r * r && e.getY() > getY() - 1.5 && e.getY() < getY() + (isPool() ? 1.5 : 5);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientParticles();
            return;
        }
        if (tickCount > getLife() + FADE_TICKS) {
            discard();
            return;
        }
        if (tickCount < getLife() && tickCount % PULSE_TICKS == 0) {
            pulse((ServerLevel) level());
        }
    }

    private void pulse(ServerLevel level) {
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (spell == null) {
            return;
        }
        Entity owner = getOwner();
        GlyphReaction reaction = getReaction();
        Vec3 c = position();
        List<LivingEntity> victims = SpellUtil.enemiesAround(level, c.add(0, 1, 0), getRadius() + 4, owner, this::inside);
        if (reaction == GlyphReaction.THUNDERSTORM) {
            // A bolt from the clouds onto one of them, twice as hard
            if (!victims.isEmpty()) {
                LivingEntity e = victims.get(random.nextInt(victims.size()));
                Vec3 to = e.position().add(0, e.getBbHeight() * .5, 0);
                GlyphArc.zap(level, GlyphElement.STORM, c.add(0, 5.5, 0), c.add((random.nextDouble() - .5) * 2, 3.5, (random.nextDouble() - .5) * 2), to);
                hurt(e, damage * 2, spell, owner);
                GlyphFx.impact(level, GlyphElement.STORM, to, 1);
            }
            return;
        }
        for (LivingEntity e : victims) {
            hurt(e, damage, spell, owner);
            switch (reaction) {
                case STEAM -> e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
                case FIRESTORM -> {
                    e.igniteForSeconds(3);
                    pull(e, c, .18, .12);
                }
                case MAGMA -> {
                    e.igniteForSeconds(3);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1));
                }
                case BLIZZARD -> {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2));
                    e.setTicksFrozen(Math.min(e.getTicksRequiredToFreeze() + 40, e.getTicksFrozen() + 30));
                    pull(e, c, .12, .05);
                }
                case TYPHOON -> {
                    e.clearFire();
                    pull(e, c, .35, .25);
                }
                case MIRE -> {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3));
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20, 0));
                }
                case SANDSTORM -> {
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
                    pull(e, c, .15, .08);
                }
                default -> {
                }
            }
        }
        if (tickCount % (PULSE_TICKS * 2) == 0) {
            GlyphFx.sound(level, c, switch (reaction) {
                case STEAM -> SoundEvents.FIRE_EXTINGUISH;
                case FIRESTORM -> SoundEvents.BLAZE_SHOOT;
                case MAGMA -> SoundEvents.LAVA_POP;
                case BLIZZARD -> SoundEvents.POWDER_SNOW_STEP;
                case TYPHOON -> SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE;
                case MIRE -> SoundEvents.MUD_STEP;
                default -> SoundEvents.SAND_BREAK;
            }, .8f, .8f);
        }
    }

    private void hurt(LivingEntity e, float amount, AbstractSpell spell, @Nullable Entity owner) {
        e.invulnerableTime = 0;
        DamageSources.applyDamage(e, amount, SpellUtil.damageSource(spell, this, owner));
    }

    /** Draws a creature in towards the eye of the column, lifting it a little. */
    private static void pull(LivingEntity e, Vec3 center, double strength, double lift) {
        Vec3 d = center.subtract(e.position()).multiply(1, 0, 1);
        double dist = d.length();
        double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
        Vec3 in = dist > .4 ? d.scale(strength / dist) : Vec3.ZERO;
        // And round it, as the wind turns
        Vec3 round = dist > .4 ? new Vec3(-d.z, 0, d.x).scale(strength * .8 / dist) : Vec3.ZERO;
        e.setDeltaMovement(e.getDeltaMovement().add(in.add(round).scale(resist)).add(0, lift * resist, 0));
        e.hurtMarked = true;
    }

    private void clientParticles() {
        if (tickCount > getLife()) {
            return;
        }
        GlyphReaction reaction = getReaction();
        float r = getRadius();
        for (int i = 0; i < 2; i++) {
            double a = random.nextDouble() * Mth.TWO_PI;
            double d = Math.sqrt(random.nextDouble()) * r;
            double x = getX() + Math.cos(a) * d;
            double z = getZ() + Math.sin(a) * d;
            double y = getY() + (isPool() ? .1 : random.nextDouble() * 4.5);
            // Swirling round the column's eye
            double vx = isPool() ? 0 : -Math.sin(a) * .25;
            double vz = isPool() ? 0 : Math.cos(a) * .25;
            switch (reaction) {
                case STEAM -> level().addParticle(ParticleTypes.CLOUD, x, y, z, vx, .05, vz);
                case FIRESTORM -> level().addParticle(ParticleTypes.FLAME, x, y, z, vx, .08, vz);
                case MAGMA -> level().addParticle(i == 0 ? ParticleTypes.LAVA : ParticleTypes.SMOKE, x, y, z, 0, .04, 0);
                case BLIZZARD -> level().addParticle(ParticleTypes.SNOWFLAKE, x, y, z, vx, -.02, vz);
                case THUNDERSTORM -> level().addParticle(i == 0 ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.CLOUD, x, y, z, vx, .02, vz);
                case TYPHOON -> level().addParticle(i == 0 ? ParticleTypes.SPLASH : ParticleTypes.BUBBLE_POP, x, y, z, vx, .1, vz);
                case MIRE -> level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState()), x, y + .1, z, 0, .05, 0);
                case SANDSTORM -> level().addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()), x, y, z, vx, 0, vz);
                default -> {
                }
            }
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}
