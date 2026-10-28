package com.waylays.arcanes.entity.wood;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.wood.WoodFx;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Splinter Cyclone: a tornado of wood (curled shavings, chips, splinters, flakes of bark, split planks, broken sticks,
 * and sawdust everywhere) gathers in front of the caster and wanders slowly the way they looked. It draws enemies
 * into it and whirls them round, shreds them over and over, blinds them with sawdust and lifts the lighter ones off
 * the ground; now and then it flings splinters at those round it. At the end it bursts apart and its wood rains down.
 */
public class SplinterStorm extends SpellEffectEntity {
    public static final float RADIUS = 3.2f;
    public static final float HEIGHT = 8;
    public static final float SPEED = .1f;
    public static final int GATHER = 16;
    public static final int SCATTER = 18;
    public static final float FLING = 8;

    private static final BlockParticleOption SAWDUST = new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.OAK_PLANKS.defaultBlockState());
    private static final BlockParticleOption PLANKS = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_PLANKS.defaultBlockState());
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(SplinterStorm.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;

    public SplinterStorm(EntityType<? extends SplinterStorm> entityType, Level level) {
        super(entityType, level);
    }

    public static SplinterStorm raise(Level level, LivingEntity caster, AbstractSpell spell, Vec3 at, float yaw, int life, float damage) {
        SplinterStorm storm = new SplinterStorm(ModEntities.SPLINTER_STORM.get(), level);
        storm.setOwner(caster);
        storm.spellId = spell.getSpellId();
        storm.damage = damage;
        storm.moveTo(at.x, at.y, at.z, yaw, 0);
        storm.entityData.set(DATA_LIFE, life);
        level.addFreshEntity(storm);
        WoodFx.crash(level, at, 1.2f);
        WoodFx.sound(level, at, SoundEvents.ELYTRA_FLYING, .7f, 1.2f);
        return storm;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LIFE, 120);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    /** How strong the whirl is (0 to 1): gathering, then bursting apart at the end. */
    public float strength(float age) {
        float in = Mth.clamp(age / GATHER, 0, 1);
        float out = Mth.clamp((age - getLife()) / SCATTER, 0, 1);
        return in * (1 - out);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > getLife() + SCATTER) {
            discard();
            return;
        }
        if (tickCount == getLife()) {
            WoodFx.chips(level, position().add(0, 3, 0), 80, RADIUS);
            SpellUtil.particles(level, SAWDUST, getX(), getY() + 4, getZ(), 80, RADIUS, 2, RADIUS, 0);
            WoodFx.crash(level, position(), 1.5f);
        }
        if (tickCount >= getLife()) {
            return;
        }
        // It wanders the way it was sent, hugging the ground
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        Vec3 next = position().add(-Mth.sin(yaw) * SPEED, 0, Mth.cos(yaw) * SPEED);
        Vec3 ground = SpellUtil.groundAt(level, next, 3, 2);
        setPos(next.x, Mth.lerp(.25, getY(), ground.y), next.z);
        if (tickCount % 10 == 0) {
            WoodFx.sound(level, position(), SoundEvents.ELYTRA_FLYING, .5f, 1.5f);
            WoodFx.sound(level, position(), SoundEvents.WOOD_HIT, 1, .5f + random.nextFloat() * .3f);
        }
        if (tickCount % 2 == 0) {
            SpellUtil.particles(level, SAWDUST, getX(), getY() + 1 + random.nextDouble() * 5, getZ(), 4, RADIUS * .6, 1, RADIUS * .6, 0);
            SpellUtil.particles(level, PLANKS, getX(), getY() + .2, getZ(), 3, 1.5, .1, 1.5, .1);
        }
        if (tickCount < GATHER / 2) {
            return;
        }
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        LivingEntity caster = getLivingOwner();
        Vec3 axis = position();
        boolean fling = tickCount % 20 == 0;
        for (LivingEntity e : SpellUtil.enemiesAround(level, axis.add(0, HEIGHT * .5, 0), FLING + 1, caster,
                x -> x.getY() < getY() + HEIGHT && x.getY() + x.getBbHeight() > getY() - 1)) {
            Vec3 to = new Vec3(axis.x - e.getX(), 0, axis.z - e.getZ());
            double d = to.length();
            double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
            if (d < RADIUS + 3.5) {
                Vec3 in = d < 1.0E-3 ? Vec3.ZERO : to.scale(1 / d);
                // Drawn in and whirled round
                Vec3 round = new Vec3(-in.z, 0, in.x);
                double pull = d > RADIUS ? .12 : .04;
                Vec3 v = e.getDeltaMovement().add(in.scale(pull * resist)).add(round.scale(.11 * resist));
                if (d < RADIUS) {
                    v = new Vec3(v.x, Math.max(v.y, .06 * resist + (e.getY() < getY() + 3 ? .05 : -.02)), v.z);
                }
                e.setDeltaMovement(v);
                e.hurtMarked = true;
            }
            if (spell == null) {
                continue;
            }
            if (d < RADIUS + .5 && tickCount % 8 == 0) {
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, damage, SpellUtil.damageSource(spell, this, caster));
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, false));
                WoodFx.chips(level, e.getBoundingBox().getCenter(), 6, .3);
                WoodFx.sound(level, e.position(), SoundEvents.WOOD_BREAK, .8f, 1.4f);
            } else if (fling && d >= RADIUS + .5) {
                // Splinters flung out of the whirl
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, damage * .5f, SpellUtil.damageSource(spell, this, caster));
                WoodFx.chips(level, e.getBoundingBox().getCenter(), 8, .2);
            }
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
