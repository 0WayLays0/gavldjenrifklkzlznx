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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Bubble Prison: a great soap bubble, its skin swirling with rainbows, closes round an enemy and carries it slowly up
 * into the air. Inside the air runs out: it starts to drown. Then the bubble bursts and its prisoner falls.
 * The bubble follows its rise on both sides from its age; the server holds the prisoner in it.
 */
public class WaterBubble extends SpellEffectEntity {
    public static final int FORM = 8;
    public static final float LIFT = 5;

    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(WaterBubble.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_HOLD = SynchedEntityData.defineId(WaterBubble.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_FOOT = SynchedEntityData.defineId(WaterBubble.class, EntityDataSerializers.FLOAT);

    @Nullable
    private String spellId;
    private int prisoner = -1;

    public WaterBubble(EntityType<? extends WaterBubble> entityType, Level level) {
        super(entityType, level);
    }

    public static WaterBubble trap(Level level, LivingEntity caster, AbstractSpell spell, LivingEntity target, int hold, float damage) {
        WaterBubble bubble = new WaterBubble(ModEntities.WATER_BUBBLE.get(), level);
        bubble.setOwner(caster);
        bubble.spellId = spell.getSpellId();
        bubble.damage = damage;
        bubble.prisoner = target.getId();
        float r = Math.max(target.getBbWidth(), target.getBbHeight()) * .5f + .45f;
        bubble.moveTo(target.getX(), target.getY(), target.getZ(), level.random.nextFloat() * 360, 0);
        bubble.entityData.set(DATA_RADIUS, r);
        bubble.entityData.set(DATA_HOLD, hold);
        bubble.entityData.set(DATA_FOOT, (float) target.getY());
        level.addFreshEntity(bubble);
        WaterFx.sound(level, target.position(), SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 1.5f, 1.2f);
        WaterFx.bubbles(level, target.position().add(0, target.getBbHeight() * .5, 0), 20, r * .6);
        return bubble;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RADIUS, 1.4f);
        builder.define(DATA_HOLD, 60);
        builder.define(DATA_FOOT, 0f);
    }

    public float getRadius() {
        return this.entityData.get(DATA_RADIUS);
    }

    /** The tick it bursts. */
    public int burstTick() {
        return this.entityData.get(DATA_HOLD);
    }

    /** How far it has risen (blocks) at {@code age}, easing in and out. */
    public float risen(float age) {
        float t = Mth.clamp((age - FORM) / (burstTick() - FORM), 0, 1);
        return LIFT * t * t * (3 - 2 * t);
    }

    /** Height of its prisoner's feet above where it was caught, the bubble rising with it. */
    public float footY(float age) {
        return this.entityData.get(DATA_FOOT) + risen(age);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Entity e = level.getEntity(prisoner);
        LivingEntity target = e instanceof LivingEntity living && living.isAlive() ? living : null;
        if (tickCount >= burstTick() || target == null && tickCount > FORM) {
            burst(level, target);
            return;
        }
        if (target == null) {
            return;
        }
        // Held in the middle of the bubble as it rises
        double y = footY(tickCount);
        if (target.position().distanceToSqr(getX(), y, getZ()) > .01) {
            target.teleportTo(getX(), y, getZ());
        }
        target.setDeltaMovement(0, 0, 0);
        target.hurtMarked = true;
        target.fallDistance = 0;
        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        // Drowning inside
        if (!target.canBreatheUnderwater()) {
            target.setAirSupply(Math.max(-20, target.getAirSupply() - 12));
            AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
            if (target.getAirSupply() <= 0 && tickCount % 20 == 0 && spell != null) {
                target.invulnerableTime = 0;
                DamageSources.applyDamage(target, damage * .25f, SpellUtil.damageSource(spell, this, getLivingOwner()));
            }
        }
        if (tickCount % 4 == 0) {
            SpellUtil.particles(level, ParticleTypes.BUBBLE_POP, getX(), y + target.getBbHeight() * .5, getZ(), 2, getRadius() * .4, getRadius() * .4, getRadius() * .4, .02);
        }
    }

    /** It bursts: a splash, a sting of water, and its prisoner falls. */
    private void burst(ServerLevel level, @Nullable LivingEntity target) {
        Vec3 c = new Vec3(getX(), footY(tickCount) + getRadius() * .8, getZ());
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (target != null) {
            if (spell != null) {
                target.invulnerableTime = 0;
                DamageSources.applyDamage(target, damage, SpellUtil.damageSource(spell, this, getLivingOwner()));
            }
            target.setAirSupply(Math.max(target.getAirSupply(), 0));
            WaterFx.soak(target, 60);
        }
        WaterFx.splash(level, c, 40, getRadius() * .7);
        WaterFx.sound(level, c, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 2, .6f);
        WaterFx.sound(level, c, SoundEvents.PLAYER_SPLASH, 1.2f, 1.1f);
        SplashCrown.at(level, new Vec3(getX(), this.entityData.get(DATA_FOOT), getZ()), 1.3f);
        discard();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}
