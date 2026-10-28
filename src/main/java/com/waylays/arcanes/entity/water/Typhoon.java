package com.waylays.arcanes.entity.water;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.water.WaterFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Typhoon: a waterspout as tall as a tower, a storm cloud crackling with lightning on top of it, moves slowly the way
 * the caster looked. It sucks enemies in; inside, they are lifted and spun round the column, battered over and over;
 * when it breaks up it flings them out. Rain falls all round it.
 */
public class Typhoon extends SpellEffectEntity {
    public static final float RADIUS = 3.6f;
    public static final float HEIGHT = 16;
    public static final float SPEED = .08f;
    public static final int FORM = 24;
    public static final int BREAK = 20;
    public static final float PULL = 9;

    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(Typhoon.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;

    public Typhoon(EntityType<? extends Typhoon> entityType, Level level) {
        super(entityType, level);
    }

    public static Typhoon raise(Level level, LivingEntity caster, AbstractSpell spell, Vec3 at, float yaw, int life, float damage) {
        Typhoon typhoon = new Typhoon(ModEntities.TYPHOON.get(), level);
        typhoon.setOwner(caster);
        typhoon.spellId = spell.getSpellId();
        typhoon.damage = damage;
        typhoon.moveTo(at.x, at.y, at.z, yaw, 0);
        typhoon.entityData.set(DATA_LIFE, life);
        level.addFreshEntity(typhoon);
        WaterFx.crash(level, at, 2);
        WaterFx.sound(level, at, SoundEvents.ELYTRA_FLYING, 1, .6f);
        return typhoon;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LIFE, 200);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    /** How whole it is (0 to 1): forming, then breaking up at the end. */
    public float strength(float age) {
        float in = Mth.clamp(age / FORM, 0, 1);
        float out = Mth.clamp((age - getLife()) / BREAK, 0, 1);
        return in * (1 - out);
    }

    /** Whether lightning flashes in its cloud at {@code age} (a few ticks every so often). */
    public boolean flashing(float age) {
        int cycle = (int) (age / 37);
        return age - cycle * 37 < 3 && WaterFx.hash(getId() * 13 + cycle) < .7f && age > FORM;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > getLife() + BREAK) {
            discard();
            return;
        }
        LivingEntity caster = getLivingOwner();
        if (tickCount == getLife()) {
            // It breaks up, flinging out whatever it held
            for (LivingEntity e : SpellUtil.enemiesAround(level, position().add(0, HEIGHT * .4, 0), HEIGHT * .6, caster, x -> true)) {
                Vec3 out = e.position().subtract(position()).multiply(1, 0, 1);
                out = out.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : out.normalize();
                e.setDeltaMovement(out.scale(1.2).add(0, .5, 0));
                e.hurtMarked = true;
            }
            WaterFx.splash(level, position().add(0, 3, 0), 80, RADIUS);
            WaterFx.crash(level, position(), 2.5f);
        }
        if (tickCount >= getLife()) {
            return;
        }
        // It wanders the way it was sent
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        Vec3 next = position().add(-Mth.sin(yaw) * SPEED, 0, Mth.cos(yaw) * SPEED);
        Vec3 ground = SpellUtil.groundAt(level, next, 3, 2);
        setPos(next.x, Mth.lerp(.25, getY(), ground.y), next.z);
        if (tickCount % 3 == 0) {
            SpellUtil.particles(level, ParticleTypes.RAIN, getX(), getY() + 6, getZ(), 30, RADIUS * 2.5, 3, RADIUS * 2.5, 0);
            WaterFx.spray(level, position().add(0, .2, 0), 6, RADIUS);
        }
        if (tickCount % 20 == 0) {
            WaterFx.sound(level, position(), SoundEvents.ELYTRA_FLYING, .8f, .5f);
            WaterFx.sound(level, position(), SoundEvents.WEATHER_RAIN_ABOVE, 1.5f, .8f);
        }
        if (flashing(tickCount) && !flashing(tickCount - 1)) {
            WaterFx.sound(level, position().add(0, HEIGHT, 0), SoundEvents.LIGHTNING_BOLT_THUNDER, 1.2f, 1.4f);
        }
        if (tickCount < FORM / 2) {
            return;
        }
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        for (LivingEntity e : SpellUtil.enemiesAround(level, position().add(0, HEIGHT * .4, 0), PULL + HEIGHT * .4, caster,
                x -> x.getY() < getY() + HEIGHT && Math.hypot(x.getX() - getX(), x.getZ() - getZ()) < PULL)) {
            Vec3 to = new Vec3(getX() - e.getX(), 0, getZ() - e.getZ());
            double d = to.length();
            double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
            Vec3 in = d < 1.0E-3 ? Vec3.ZERO : to.scale(1 / d);
            Vec3 round = new Vec3(-in.z, 0, in.x);
            if (d > RADIUS) {
                e.setDeltaMovement(e.getDeltaMovement().add(in.scale(.13 * resist)).add(round.scale(.06 * resist)));
            } else {
                // Inside: lifted and spun round the column
                double height = e.getY() - getY();
                double lift = height < HEIGHT * .45 ? .12 : -.02;
                Vec3 v = e.getDeltaMovement().scale(.5).add(round.scale(.32 * resist)).add(in.scale((d > RADIUS * .6 ? .08 : -.02) * resist));
                e.setDeltaMovement(v.x, lift + .05 * Mth.sin(tickCount * .3f), v.z);
                e.fallDistance = 0;
                WaterFx.soak(e, 30);
                if (spell != null && tickCount % 10 == 0) {
                    WaterFx.hurtInPlace(e, damage, SpellUtil.damageSource(spell, this, caster));
                }
            }
            e.hurtMarked = true;
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 192 * 192;
    }
}
