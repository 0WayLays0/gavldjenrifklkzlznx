package com.waylays.arcanes.entity.rune;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.rune.RuneFx;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The Petrifying Rune on its victim: a rune lights up under its feet and stone climbs its legs, holding it in place
 * (see {@link com.waylays.arcanes.effect.PetrifiedEffect}) and grinding at it; when the hold ends the stone shatters
 * and hurts it once more.
 */
public class PetrifyBind extends SpellEffectEntity {
    /** Ticks the stone takes to climb, and to fly apart. */
    public static final int GROW_TICKS = 8;
    public static final int SHATTER_TICKS = 12;

    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(PetrifyBind.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SHATTER = SynchedEntityData.defineId(PetrifyBind.class, EntityDataSerializers.INT);

    private int duration = 60;
    private float grindDamage;

    public PetrifyBind(EntityType<? extends PetrifyBind> entityType, Level level) {
        super(entityType, level);
    }

    /** Petrifies {@code target} for {@code duration} ticks; a fresh rune replaces one already on it. */
    public static PetrifyBind bind(Level level, LivingEntity caster, LivingEntity target, int duration, float grindDamage, float shatterDamage) {
        for (PetrifyBind old : level.getEntitiesOfClass(PetrifyBind.class, target.getBoundingBox().inflate(2), b -> b.getTarget() == target)) {
            old.discard();
        }
        PetrifyBind bind = new PetrifyBind(ModEntities.PETRIFY_BIND.get(), level);
        bind.setOwner(caster);
        bind.entityData.set(DATA_TARGET, target.getId());
        bind.moveTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), 0);
        bind.duration = duration;
        bind.grindDamage = grindDamage;
        bind.damage = shatterDamage;
        target.addEffect(new MobEffectInstance(ModEffects.PETRIFIED, duration + 2, 0, false, true, true), caster);
        level.addFreshEntity(bind);
        RuneFx.stone(level, target.position().add(0, .3, 0), Blocks.DEEPSLATE.defaultBlockState(), 16, .4);
        RuneFx.sound(level, target.position(), SoundEvents.DEEPSLATE_PLACE, 1.4f, .6f);
        RuneFx.sound(level, target.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1f, .7f);
        return bind;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_TARGET, -1);
        builder.define(DATA_SHATTER, 0);
    }

    @Nullable
    public LivingEntity getTarget() {
        return level().getEntity(this.entityData.get(DATA_TARGET)) instanceof LivingEntity living ? living : null;
    }

    /** Ticks since the stone shattered, or -1. */
    public float shatterAge(float partialTicks) {
        int at = this.entityData.get(DATA_SHATTER);
        return at == 0 ? -1 : tickCount + partialTicks - at;
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity target = getTarget();
        if (target != null && shatterAge(0) < 0) {
            setPos(target.getX(), target.getY(), target.getZ());
        }
        if (level().isClientSide) {
            return;
        }
        int shatter = this.entityData.get(DATA_SHATTER);
        if (shatter != 0) {
            if (tickCount - shatter >= SHATTER_TICKS) {
                discard();
            }
            return;
        }
        if (target == null || !target.isAlive()) {
            this.entityData.set(DATA_SHATTER, Math.max(1, tickCount));
            return;
        }
        if (tickCount > GROW_TICKS && tickCount % 10 == 0 && grindDamage > 0) {
            target.invulnerableTime = 0;
            DamageSources.applyDamage(target, grindDamage, SpellUtil.damageSource(ModSpells.PETRIFYING_RUNE.get(), this, getOwner()));
        }
        if (tickCount >= duration) {
            shatter(target);
        }
    }

    /** The stone flies apart, hurting its victim once more. */
    private void shatter(LivingEntity target) {
        this.entityData.set(DATA_SHATTER, Math.max(1, tickCount));
        target.removeEffect(ModEffects.PETRIFIED);
        target.invulnerableTime = 0;
        DamageSources.applyDamage(target, damage, SpellUtil.damageSource(ModSpells.PETRIFYING_RUNE.get(), this, getOwner()));
        Vec3 at = target.position().add(0, target.getBbHeight() * .3, 0);
        RuneFx.stone(level(), at, Blocks.DEEPSLATE.defaultBlockState(), 40, .5);
        RuneFx.stone(level(), at, Blocks.COBBLED_DEEPSLATE.defaultBlockState(), 20, .5);
        RuneFx.glyphs(level(), at, 12, .5);
        RuneFx.sound(level(), at, SoundEvents.DEEPSLATE_BREAK, 1.6f, .8f);
        RuneFx.sound(level(), at, SoundEvents.GLASS_BREAK, .8f, .6f);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}
