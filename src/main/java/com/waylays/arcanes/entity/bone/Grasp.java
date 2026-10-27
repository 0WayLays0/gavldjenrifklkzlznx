package com.waylays.arcanes.entity.bone;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.bone.BoneFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The Skeletal Grasp: a giant hand of bone bursts out of the ground under the enemy, its forearm after it, closes into a
 * fist round it, lifts it high and crushes it, then drives it down into the ground with a quake, and sinks back. Both
 * sides work out the hand from its age; the server carries the prisoner in the fist.
 */
public class Grasp extends SpellEffectEntity {
    public static final int EMERGE = 10;
    public static final int CLOSE = 16;
    public static final int LIFTED = 30;
    public static final int SLAM = 44;
    public static final int IMPACT = 50;
    public static final int END = 72;
    /** The hand's size: wrist to knuckles. */
    public static final float SIZE = 1.7f;
    public static final float LIFT = 3.2f;
    public static final float SLAM_RADIUS = 2.8f;

    @Nullable
    private String spellId;
    private int prisoner = -1;

    public Grasp(EntityType<? extends Grasp> entityType, Level level) {
        super(entityType, level);
    }

    public static Grasp seize(Level level, LivingEntity caster, AbstractSpell spell, LivingEntity target, float damage) {
        Grasp grasp = new Grasp(ModEntities.GRASP.get(), level);
        grasp.setOwner(caster);
        grasp.spellId = spell.getSpellId();
        grasp.damage = damage;
        grasp.prisoner = target.getId();
        // Set so that the fist closes right on the target: the palm faces the caster's way
        float yaw = caster.getYRot();
        Vec3 fist = fistLocal(CLOSE);
        Vec3 off = toWorld(fist, yaw);
        grasp.moveTo(target.getX() - off.x, target.getY(), target.getZ() - off.z, yaw, 0);
        level.addFreshEntity(grasp);
        BoneFx.erupt(level, target.position(), 1.6f);
        return grasp;
    }

    /** How high the wrist stands at {@code t} (it bursts out of the ground, lifts, slams down, sinks). */
    public static float wrist(float t) {
        if (t < EMERGE) {
            float k = t / EMERGE;
            return -3.5f + 3.6f * (1 - (1 - k) * (1 - k));
        }
        if (t < CLOSE) {
            return .1f;
        }
        if (t < LIFTED) {
            float k = (t - CLOSE) / (LIFTED - CLOSE);
            return .1f + LIFT * k * k * (3 - 2 * k);
        }
        if (t < SLAM) {
            return .1f + LIFT + .15f * Mth.sin((t - LIFTED) * .9f);
        }
        if (t < IMPACT) {
            float k = (t - SLAM) / (IMPACT - SLAM);
            return .1f + LIFT * (1 - k * k) - .4f * k * k;
        }
        float k = Mth.clamp((t - IMPACT - 8) / (END - IMPACT - 8), 0, 1);
        return -.3f - 4f * k * k;
    }

    /** How closed the hand is (0 open to 1 a fist). */
    public static float curl(float t) {
        if (t < EMERGE) {
            return 0;
        }
        if (t < CLOSE) {
            float k = (t - EMERGE) / (CLOSE - EMERGE);
            return k * k * (3 - 2 * k) * .95f;
        }
        if (t < IMPACT + 4) {
            return .95f;
        }
        return .95f * (1 - Mth.clamp((t - IMPACT - 4) / 8, 0, 1));
    }

    /** Where the fist's hollow is, in the hand's own frame (+Z the way it faces), at {@code t}. */
    public static Vec3 fistLocal(float t) {
        return new Vec3(0, wrist(t) + SIZE * .95f, SIZE * .45f);
    }

    public static Vec3 toWorld(Vec3 local, float yawDegrees) {
        float yaw = yawDegrees * Mth.DEG_TO_RAD;
        float c = Mth.cos(yaw), s = Mth.sin(yaw);
        return new Vec3(local.x * c - local.z * s, local.y, local.x * s + local.z * c);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > END) {
            discard();
            return;
        }
        Entity e = level.getEntity(prisoner);
        LivingEntity target = e instanceof LivingEntity living && living.isAlive() ? living : null;
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        LivingEntity caster = getLivingOwner();
        if (target != null && tickCount >= CLOSE - 2 && tickCount <= IMPACT) {
            // Carried in the fist
            Vec3 fist = position().add(toWorld(fistLocal(tickCount), getYRot()));
            target.teleportTo(fist.x, fist.y - target.getBbHeight() * .5, fist.z);
            target.setDeltaMovement(Vec3.ZERO);
            target.hurtMarked = true;
            target.fallDistance = 0;
        }
        if (tickCount == CLOSE) {
            BoneFx.crack(level, position(), 1.4f);
            BoneFx.sound(level, position(), SoundEvents.BONE_BLOCK_PLACE, 1.4f, .5f);
        }
        if (target != null && spell != null && tickCount > CLOSE && tickCount < SLAM && (tickCount - CLOSE) % 8 == 0) {
            target.invulnerableTime = 0;
            DamageSources.applyDamage(target, damage * .25f, SpellUtil.damageSource(spell, this, caster));
            BoneFx.marrow(level, target.getBoundingBox().getCenter(), 6, .3);
            BoneFx.sound(level, target.position(), SoundEvents.SKELETON_STEP, 1.2f, .5f);
        }
        if (tickCount == IMPACT) {
            Vec3 at = position().add(toWorld(new Vec3(0, 0, SIZE * .45), getYRot()));
            if (spell != null) {
                for (LivingEntity x : SpellUtil.enemiesAround(level, at.add(0, .5, 0), SLAM_RADIUS, caster, y -> true)) {
                    boolean held = x == target;
                    x.invulnerableTime = 0;
                    DamageSources.applyDamage(x, held ? damage : damage * .5f, SpellUtil.damageSource(spell, this, caster));
                    double resist = 1 - Mth.clamp(x.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
                    Vec3 away = x.position().subtract(at).multiply(1, 0, 1);
                    away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize();
                    x.setDeltaMovement(x.getDeltaMovement().add(away.scale(.5 * resist)).add(0, (held ? .2 : .55) * resist, 0));
                    x.hurtMarked = true;
                    x.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                }
            }
            BoneFx.splinters(level, at.add(0, .3, 0), 40, 1.2);
            BoneFx.marrow(level, at.add(0, .5, 0), 20, 1);
            BoneFx.sound(level, at, SoundEvents.GENERIC_EXPLODE.value(), 1.4f, .6f);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.BONE_BLOCK_BREAK, SoundSource.PLAYERS, 2f, .5f);
            SpellUtil.cameraShake(level, at, 14, 18);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
