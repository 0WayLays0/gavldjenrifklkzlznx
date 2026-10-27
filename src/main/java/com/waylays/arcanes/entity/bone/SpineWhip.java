package com.waylays.arcanes.entity.bone;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.bone.BoneFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
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
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The Spine Whip: a whip of vertebrae, a spike at its tip, grows out of the caster's hand and lashes round in front of
 * them from right to left, the far end trailing behind the near one like a real whip; whatever it sweeps through is
 * struck and flung aside. Both sides work out the whip from the entity's age.
 */
public class SpineWhip extends SpellEffectEntity {
    public static final int SEGMENTS = 16;
    public static final int GROW = 4;
    public static final int SWING = 9;
    public static final int LIFE = GROW + SWING + 6;
    /** How far each vertebra lags behind the one before it (ticks). */
    public static final float LAG = .22f;
    /** The arc it sweeps, either side of straight ahead (radians). */
    public static final float ARC = 1.45f;
    public static final float HEIGHT = 1.25f;

    private static final EntityDataAccessor<Float> DATA_REACH = SynchedEntityData.defineId(SpineWhip.class, EntityDataSerializers.FLOAT);

    private final IntOpenHashSet hit = new IntOpenHashSet();
    @Nullable
    private String spellId;

    public SpineWhip(EntityType<? extends SpineWhip> entityType, Level level) {
        super(entityType, level);
    }

    public static SpineWhip lash(Level level, LivingEntity caster, AbstractSpell spell, float reach, float damage) {
        SpineWhip whip = new SpineWhip(ModEntities.SPINE_WHIP.get(), level);
        whip.setOwner(caster);
        whip.spellId = spell.getSpellId();
        whip.damage = damage;
        whip.moveTo(caster.getX(), caster.getY(), caster.getZ(), caster.getYRot(), 0);
        whip.entityData.set(DATA_REACH, reach);
        level.addFreshEntity(whip);
        BoneFx.rattle(level, caster.position(), 1);
        return whip;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_REACH, 7f);
    }

    public float getReach() {
        return this.entityData.get(DATA_REACH);
    }

    /** The whip's angle from straight ahead at {@code t} ticks (right to left), eased. */
    public static float angle(float t) {
        float s = Mth.clamp((t - GROW) / SWING, 0, 1);
        s = s * s * (3 - 2 * s);
        return ARC - 2 * ARC * s;
    }

    /** How far out the whip has grown (0 to 1), and how far it has drawn back in at the end. */
    public static float extension(float t) {
        float grow = Mth.clamp(t / GROW, 0, 1);
        float back = Mth.clamp((t - GROW - SWING) / 6, 0, 1);
        return (1 - (1 - grow) * (1 - grow)) * (1 - back * back);
    }

    /** Where vertebra {@code i} (0 at the hand, the last at the tip) is at {@code t}, from the entity. */
    public Vec3 point(int i, float t) {
        float lagged = t - LAG * i;
        float a = getYRot() * Mth.DEG_TO_RAD + angle(lagged);
        float d = .8f + (i + 1) * (getReach() - .8f) / SEGMENTS * extension(lagged);
        // It sags a little along its length and whips up at the end of the swing
        float h = HEIGHT - .25f * i / SEGMENTS + .3f * Mth.sin(Mth.clamp((lagged - GROW) / SWING, 0, 1) * Mth.PI) * i / SEGMENTS;
        return new Vec3(-Mth.sin(a) * d, h, Mth.cos(a) * d);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > LIFE) {
            discard();
            return;
        }
        if (tickCount == GROW) {
            BoneFx.sound(level, position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f, .6f);
        }
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        LivingEntity caster = getLivingOwner();
        if (spell == null || tickCount < GROW || tickCount > GROW + SWING + SEGMENTS * LAG + 1) {
            return;
        }
        float reach = getReach();
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        for (LivingEntity e : SpellUtil.enemiesAround(level, position().add(0, 1, 0), reach + 1.5, caster, x -> !hit.contains(x.getId()))) {
            double dx = e.getX() - getX(), dz = e.getZ() - getZ();
            double dist = Math.hypot(dx, dz);
            if (dist < .5 || e.getY() > getY() + 3 || e.getY() < getY() - 1.5) {
                continue;
            }
            // Its angle from straight ahead, and the whip's at its distance, now and a tick ago: struck when it passes
            float phi = Mth.wrapDegrees((float) (Math.atan2(-dx, dz) - yaw) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
            int i = Mth.clamp((int) ((dist - .8) / (reach - .8) * SEGMENTS), 0, SEGMENTS - 1);
            float now = angle(tickCount - LAG * i), before = angle(tickCount - 1 - LAG * i);
            if (phi > now - .2f && phi < before + .2f) {
                hit.add(e.getId());
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, damage, SpellUtil.damageSource(spell, this, caster));
                // Flung aside, the way the whip swings (towards the caster's left)
                double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
                float side = yaw + phi - Mth.HALF_PI;
                e.setDeltaMovement(e.getDeltaMovement().add(-Mth.sin(side) * .9 * resist, .35 * resist, Mth.cos(side) * .9 * resist));
                e.hurtMarked = true;
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
                BoneFx.crack(level, e.position(), 1);
                BoneFx.marrow(level, e.getBoundingBox().getCenter(), 8, .3);
            }
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}
