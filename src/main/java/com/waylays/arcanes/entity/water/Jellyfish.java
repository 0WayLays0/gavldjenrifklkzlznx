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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import javax.annotation.Nullable;

/**
 * A jellyfish of water, glowing, one of a swarm: it swims through the air in slow pulses after the nearest enemy and
 * stings it (poisoned, numbed, slowed nearly to a stop), a few times, then fades. With nothing to sting it drifts round
 * its caster.
 */
public class Jellyfish extends SpellEffectEntity {
    public static final int STINGS = 3;
    public static final int FADE = 16;
    public static final float HUNT = 18;
    /** Colours of the swarm: cyan, violet, pink, aqua. */
    public static final int[] HUES = {0x7CF2FF, 0xB58CFF, 0xFF8CD8, 0x6FFFD0};

    private static final EntityDataAccessor<Integer> DATA_HUE = SynchedEntityData.defineId(Jellyfish.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STING = SynchedEntityData.defineId(Jellyfish.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_END = SynchedEntityData.defineId(Jellyfish.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;
    private int life = 300;
    private int stings = STINGS;
    private int cooldown;
    private int target = -1;
    private Vec3 motion = Vec3.ZERO;

    public Jellyfish(EntityType<? extends Jellyfish> entityType, Level level) {
        super(entityType, level);
    }

    public static Jellyfish release(Level level, LivingEntity caster, AbstractSpell spell, Vec3 at, int hue, int life, float damage) {
        Jellyfish jelly = new Jellyfish(ModEntities.JELLYFISH.get(), level);
        jelly.setOwner(caster);
        jelly.spellId = spell.getSpellId();
        jelly.damage = damage;
        jelly.life = life;
        jelly.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360, 0);
        jelly.entityData.set(DATA_HUE, hue);
        level.addFreshEntity(jelly);
        WaterFx.bubbles(level, at, 10, .3);
        return jelly;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_HUE, 0);
        builder.define(DATA_STING, -100);
        builder.define(DATA_END, -1);
    }

    public int getHue() {
        return HUES[Math.floorMod(this.entityData.get(DATA_HUE), HUES.length)];
    }

    /** The tick it last stung. */
    public int getStingTick() {
        return this.entityData.get(DATA_STING);
    }

    /** The tick it began to fade, or -1. */
    public int getEndTick() {
        return this.entityData.get(DATA_END);
    }

    /** Its swimming pulse (0 relaxed to 1 contracted), its own rhythm. */
    public float pulse(float age) {
        return Math.max(0, Mth.sin(age * .28f + getId()));
    }

    private void fade() {
        if (getEndTick() < 0) {
            this.entityData.set(DATA_END, tickCount);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity caster = getLivingOwner();
        if (getEndTick() >= 0) {
            if (tickCount > getEndTick() + FADE) {
                discard();
            }
            return;
        }
        if (caster == null || !caster.isAlive() || tickCount > life || stings <= 0) {
            fade();
            return;
        }
        Entity e = target < 0 ? null : level.getEntity(target);
        LivingEntity prey = e instanceof LivingEntity living && living.isAlive() && living.distanceTo(this) < HUNT + 4 ? living : null;
        if (prey == null) {
            prey = SpellUtil.enemiesAround(level, caster.position(), HUNT, caster, x -> true).stream()
                    .min(Comparator.comparingDouble(x -> x.distanceToSqr(this))).orElse(null);
            target = prey == null ? -1 : prey.getId();
        }
        // Swimming in pulses: a push each time its bell closes
        Vec3 goal;
        if (prey != null) {
            goal = prey.position().add(0, prey.getBbHeight() * .8, 0);
        } else {
            float a = tickCount * .03f + getId();
            goal = caster.position().add(Mth.cos(a) * 2.5, 2.4 + .4 * Mth.sin(tickCount * .07f + getId()), Mth.sin(a) * 2.5);
        }
        Vec3 to = goal.subtract(position());
        double dist = to.length();
        float push = pulse(tickCount) * (prey != null ? .1f : .06f);
        motion = motion.scale(.86).add(dist < 1.0E-3 ? Vec3.ZERO : to.scale(1 / dist).scale(push));
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        if (--cooldown <= 0 && prey != null && prey.getBoundingBox().inflate(.6).contains(position().add(0, -.4, 0))) {
            sting(level, caster, prey);
        }
    }

    /** It stings: poisoned, numbed, slowed nearly to a stop. */
    private void sting(ServerLevel level, LivingEntity caster, LivingEntity prey) {
        cooldown = 20;
        stings--;
        this.entityData.set(DATA_STING, tickCount);
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (spell != null) {
            prey.invulnerableTime = 0;
            DamageSources.applyDamage(prey, damage, SpellUtil.damageSource(spell, this, caster));
        }
        prey.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
        prey.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 4));
        prey.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
        SpellUtil.particles(level, ParticleTypes.ELECTRIC_SPARK, prey.getX(), prey.getY() + prey.getBbHeight() * .6, prey.getZ(), 8, .3, .4, .3, .05);
        WaterFx.sound(level, prey.position(), SoundEvents.SLIME_SQUISH_SMALL, 1.2f, 1.4f);
        WaterFx.sound(level, prey.position(), SoundEvents.BEE_STING, .8f, .6f);
        // Pushed back a little off its prey
        motion = position().subtract(prey.position()).normalize().scale(.25).add(0, .15, 0);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}
