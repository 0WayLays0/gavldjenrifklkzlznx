package com.waylays.arcanes.entity.fire;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.fire.FireFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;

/**
 * A will-o'-wisp of Stellar Fire: a little living flame that circles its caster until an enemy comes near, then darts
 * at it and bursts on it, burning it. With nothing to hunt it goes out at the end of its life.
 */
public class Wisp extends SpellEffectEntity {
    public static final float HUNT = 14;
    public static final float SPEED = .55f;
    public static final int FADE = 10;

    private static final EntityDataAccessor<Integer> DATA_HEAT = SynchedEntityData.defineId(Wisp.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_END = SynchedEntityData.defineId(Wisp.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;
    private int index;
    private int life = 300;
    private int target = -1;
    private Vec3 motion = Vec3.ZERO;

    public Wisp(EntityType<? extends Wisp> entityType, Level level) {
        super(entityType, level);
    }

    public static Wisp kindle(Level level, LivingEntity caster, AbstractSpell spell, int index, int life, float damage, int tier) {
        Wisp wisp = new Wisp(ModEntities.WISP.get(), level);
        wisp.setOwner(caster);
        wisp.spellId = spell.getSpellId();
        wisp.damage = damage;
        wisp.index = index;
        wisp.life = life;
        Vec3 at = caster.position().add(0, 2, 0);
        wisp.moveTo(at.x, at.y, at.z, 0, 0);
        wisp.entityData.set(DATA_HEAT, tier);
        level.addFreshEntity(wisp);
        return wisp;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_HEAT, 0);
        builder.define(DATA_END, -1);
    }

    public int getHeat() {
        return this.entityData.get(DATA_HEAT);
    }

    /** The tick it went out, or -1. */
    public int getEndTick() {
        return this.entityData.get(DATA_END);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (getEndTick() >= 0) {
            if (tickCount > getEndTick() + FADE) {
                discard();
            }
            return;
        }
        LivingEntity caster = getLivingOwner();
        if (caster == null || !caster.isAlive() || tickCount > life) {
            goOut();
            return;
        }
        Entity e = target < 0 ? null : level.getEntity(target);
        LivingEntity prey = e instanceof LivingEntity living && living.isAlive() && living.distanceTo(this) < HUNT + 6 ? living : null;
        if (prey == null && tickCount > 10) {
            prey = SpellUtil.enemiesAround(level, caster.position(), HUNT, caster, x -> true).stream()
                    .min(Comparator.comparingDouble(x -> x.distanceToSqr(this))).orElse(null);
            target = prey == null ? -1 : prey.getId();
        }
        Vec3 goal;
        if (prey != null) {
            goal = prey.getBoundingBox().getCenter();
        } else {
            float a = tickCount * .08f + index * 2.1f;
            goal = caster.position().add(Mth.cos(a) * 1.8, 2.2 + .3 * Mth.sin(tickCount * .1f + index), Mth.sin(a) * 1.8);
        }
        Vec3 to = goal.subtract(position());
        double dist = to.length();
        motion = motion.scale(.8).add(dist < 1.0E-3 ? Vec3.ZERO : to.scale(Math.min(SPEED, dist) / dist * .35));
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        if (prey != null && prey.getBoundingBox().inflate(.4).contains(position())) {
            AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
            FireFx.burn(prey, damage, spell, this, caster, getHeat());
            FireBurst.at(level, prey.position().add(0, prey.getBbHeight() * .3, 0), .9f, getHeat());
            goOut();
        }
        if (tickCount % 4 == 0) {
            FireFx.embers(level, position(), 1, .1, getHeat());
        }
    }

    private void goOut() {
        if (getEndTick() < 0) {
            this.entityData.set(DATA_END, tickCount);
            FireFx.sound(level(), position(), SoundEvents.FIRE_EXTINGUISH, .5f, 1.6f);
        }
    }

    /** Where it is heading (for the tail it trails). */
    public Vec3 getMotion() {
        return motion;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}
