package com.waylays.arcanes.entity.wind;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.wind.WindFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Cyclone: a whirlwind that wanders forward along the ground, drawing in every enemy near, carrying them up and round
 * it, battering them; when it dies it flings them out.
 */
public class Cyclone extends SpellEffectEntity {
    public static final int LIFE = 90;
    public static final int FORM = 12;
    public static final int FADE = 14;
    public static final float HEIGHT = 6.5f;
    public static final float TOP = 2.6f;
    public static final double REACH = 4;
    public static final double SPEED = .1;

    private static final EntityDataAccessor<Float> DATA_YAW = SynchedEntityData.defineId(Cyclone.class, EntityDataSerializers.FLOAT);

    @Nullable
    private String spellId;

    public Cyclone(EntityType<? extends Cyclone> entityType, Level level) {
        super(entityType, level);
    }

    public static Cyclone loose(Level level, LivingEntity caster, AbstractSpell spell, Vec3 at, Vec3 way, float damage) {
        Cyclone c = new Cyclone(ModEntities.CYCLONE.get(), level);
        c.setOwner(caster);
        c.spellId = spell.getSpellId();
        c.damage = damage;
        c.moveTo(at.x, at.y, at.z, 0, 0);
        c.entityData.set(DATA_YAW, (float) Mth.atan2(way.z, way.x));
        level.addFreshEntity(c);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BREEZE_WHIRL, SoundSource.PLAYERS, 2, .7f);
        return c;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_YAW, 0f);
    }

    public float grow(float age) {
        return Mth.clamp(age / FORM, 0, 1);
    }

    public float fade(float age) {
        return Mth.clamp((age - LIFE) / FADE, 0, 1);
    }

    @Override
    public void tick() {
        super.tick();
        float a = this.entityData.get(DATA_YAW);
        if (tickCount < LIFE) {
            // It wanders on, weaving a little, keeping to the ground
            float weave = .5f * Mth.sin(tickCount * .08f);
            Vec3 step = new Vec3(Mth.cos(a + weave), 0, Mth.sin(a + weave)).scale(SPEED);
            Vec3 next = position().add(step);
            BlockPos below = BlockPos.containing(next.x, next.y - .5, next.z);
            double y = next.y;
            if (level().getBlockState(below).isAir()) {
                y -= .2;
            } else if (!level().getBlockState(BlockPos.containing(next)).isAir()) {
                y += .5;
            }
            setPos(next.x, y, next.z);
        }
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > LIFE + FADE) {
            discard();
            return;
        }
        LivingEntity caster = getLivingOwner();
        if (tickCount % 15 == 0) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.BREEZE_WHIRL, SoundSource.PLAYERS, 1.4f, .6f + .1f * level.random.nextFloat());
            WindFx.dust(level, position(), 6, 1.2);
        }
        if (tickCount == LIFE) {
            // It dies: everything it held is flung out
            for (LivingEntity e : SpellUtil.enemiesAround(level, position().add(0, HEIGHT * .5, 0), REACH + 2, caster, x -> true)) {
                Vec3 out = e.position().subtract(position()).multiply(1, 0, 1);
                out = out.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : out.normalize();
                WindFx.throwBy(e, out.scale(1.3).add(0, .5, 0));
            }
            WindFx.gust(level, position().add(0, 1, 0), 16, 1.5);
            return;
        }
        if (tickCount > LIFE || tickCount < FORM / 2) {
            return;
        }
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        for (LivingEntity e : SpellUtil.enemiesAround(level, position().add(0, HEIGHT * .4, 0), REACH + HEIGHT * .4, caster,
                x -> x.getY() > getY() - 1.5 && x.getY() < getY() + HEIGHT && Math.hypot(x.getX() - getX(), x.getZ() - getZ()) < REACH + 1)) {
            Vec3 to = new Vec3(getX() - e.getX(), 0, getZ() - e.getZ());
            double d = to.length();
            Vec3 in = d < 1.0E-3 ? Vec3.ZERO : to.scale(1 / d);
            Vec3 round = new Vec3(-in.z, 0, in.x);
            double k = WindFx.give(e);
            double up = e.getY() - getY() < HEIGHT * .7 ? .12 : -.02;
            Vec3 v = e.getDeltaMovement().scale(.55).add(in.scale(.09 * k)).add(round.scale(.32 * k)).add(0, up * k + .04, 0);
            e.setDeltaMovement(v);
            e.hurtMarked = true;
            e.fallDistance = 0;
            if (spell != null && tickCount % 10 == 0) {
                Vec3 keep = e.getDeltaMovement();
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, damage, SpellUtil.damageSource(spell, this, caster));
                e.setDeltaMovement(keep);
            }
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
