package com.waylays.arcanes.entity.wood;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.wood.WoodFx;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Amber Prison: golden sap wells up out of the ground round an enemy, climbs over it and hardens into amber: it is
 * set fast inside, unable to move or strike. Before the end the amber clouds and cracks, then bursts in a spray of
 * shards that cut the prisoner and those beside it. Both sides work out the amber from its age; the server holds.
 */
public class AmberPrison extends SpellEffectEntity {
    public static final int RISE = 12;
    public static final int HARDEN = 8;
    public static final int CRACK = 14;
    public static final int SHATTER = 12;
    public static final float BURST = 2.5f;

    private static final EntityDataAccessor<Float> DATA_WIDTH = SynchedEntityData.defineId(AmberPrison.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEIGHT = SynchedEntityData.defineId(AmberPrison.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_HOLD = SynchedEntityData.defineId(AmberPrison.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;
    private int prisoner = -1;
    private float yaw;

    public AmberPrison(EntityType<? extends AmberPrison> entityType, Level level) {
        super(entityType, level);
    }

    public static AmberPrison encase(Level level, LivingEntity caster, AbstractSpell spell, LivingEntity target, int hold, float damage) {
        AmberPrison prison = new AmberPrison(ModEntities.AMBER_PRISON.get(), level);
        prison.setOwner(caster);
        prison.spellId = spell.getSpellId();
        prison.damage = damage;
        prison.prisoner = target.getId();
        prison.yaw = target.getYRot();
        prison.moveTo(target.getX(), target.getY(), target.getZ(), level.random.nextFloat() * 360, 0);
        prison.entityData.set(DATA_WIDTH, target.getBbWidth());
        prison.entityData.set(DATA_HEIGHT, target.getBbHeight());
        prison.entityData.set(DATA_HOLD, hold);
        level.addFreshEntity(prison);
        WoodFx.sound(level, target.position(), SoundEvents.HONEY_BLOCK_PLACE, 1.5f, .6f);
        WoodFx.sound(level, target.position(), SoundEvents.HONEY_BLOCK_SLIDE, 1.2f, .5f);
        SpellUtil.particles(level, ParticleTypes.LANDING_HONEY, target.getX(), target.getY() + .1, target.getZ(), 16, .6, .05, .6, 0);
        return prison;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_WIDTH, .6f);
        builder.define(DATA_HEIGHT, 1.8f);
        builder.define(DATA_HOLD, 60);
    }

    public float getWidth() {
        return this.entityData.get(DATA_WIDTH);
    }

    public float getHeight() {
        return this.entityData.get(DATA_HEIGHT);
    }

    /** The tick it bursts. */
    public int burstTick() {
        return RISE + this.entityData.get(DATA_HOLD);
    }

    /** How far the sap has climbed (0 to 1). */
    public float risen(float age) {
        float t = Mth.clamp(age / RISE, 0, 1);
        return 1 - (1 - t) * (1 - t);
    }

    /** How hard it has set (0 liquid sap, 1 amber). */
    public float hardened(float age) {
        return Mth.clamp((age - RISE * .6f) / HARDEN, 0, 1);
    }

    /** How far the cracks have run (0 to 1) before it bursts. */
    public float cracked(float age) {
        return Mth.clamp((age - (burstTick() - CRACK)) / CRACK, 0, 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > burstTick() + SHATTER) {
            discard();
            return;
        }
        Entity e = level.getEntity(prisoner);
        LivingEntity target = e instanceof LivingEntity living && living.isAlive() ? living : null;
        if (tickCount == burstTick()) {
            burst(level, target);
            return;
        }
        if (tickCount > burstTick()) {
            return;
        }
        if (tickCount % 6 == 0) {
            SpellUtil.particles(level, ParticleTypes.DRIPPING_HONEY, getX(), getY() + getHeight() * .7, getZ(), 2, getWidth() * .7, getHeight() * .3, getWidth() * .7, 0);
        }
        if (tickCount == RISE) {
            WoodFx.sound(level, position(), SoundEvents.HONEYCOMB_WAX_ON, 1.4f, .6f);
            AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
            if (target != null && spell != null) {
                target.invulnerableTime = 0;
                DamageSources.applyDamage(target, damage * .3f, SpellUtil.damageSource(spell, this, getLivingOwner()));
            }
        }
        if (tickCount == burstTick() - CRACK) {
            WoodFx.sound(level, position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, .5f);
        }
        if (target == null || tickCount < RISE / 2) {
            return;
        }
        // Set fast inside: it cannot move, turn or strike
        if (target.position().distanceToSqr(position()) > .01) {
            target.teleportTo(getX(), getY(), getZ());
        }
        target.setDeltaMovement(0, 0, 0);
        target.hurtMarked = true;
        target.setYRot(yaw);
        target.setYHeadRot(yaw);
        target.fallDistance = 0;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 10, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 10, 10, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 10, 4, false, false));
        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setTarget(null);
        }
    }

    /** It bursts: shards cut the prisoner and those beside it. */
    private void burst(ServerLevel level, @Nullable LivingEntity target) {
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        LivingEntity caster = getLivingOwner();
        Vec3 centre = position().add(0, getHeight() * .5, 0);
        if (spell != null) {
            for (LivingEntity e : SpellUtil.enemiesAround(level, centre, BURST + getWidth() * .5, caster, x -> true)) {
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, e == target ? damage : damage * .5f, SpellUtil.damageSource(spell, this, caster));
            }
        }
        SpellUtil.particles(level, ParticleTypes.FALLING_HONEY, centre.x, centre.y, centre.z, 30, getWidth(), getHeight() * .4, getWidth(), 0);
        SpellUtil.particles(level, ParticleTypes.WAX_OFF, centre.x, centre.y, centre.z, 20, getWidth(), getHeight() * .4, getWidth(), .2);
        WoodFx.sound(level, centre, SoundEvents.GLASS_BREAK, 1.4f, .6f);
        WoodFx.sound(level, centre, SoundEvents.AMETHYST_BLOCK_BREAK, 1.4f, .7f);
        WoodFx.sound(level, centre, SoundEvents.HONEY_BLOCK_BREAK, 1.2f, .7f);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}
