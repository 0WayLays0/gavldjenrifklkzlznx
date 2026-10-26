package com.waylays.arcanes.entity.cosmos;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.cosmos.Cosmos;
import com.waylays.arcanes.spells.cosmos.CosmosFx;
import com.waylays.arcanes.spells.cosmos.PruningBatonSpell;
import com.waylays.arcanes.spells.cosmos.TimeTrail;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * The TVA's pruning baton in the caster's hand for one blow of its combo: a rod of dark steel with a glowing orange
 * tip that leaves a trail of time through the air. The first blow sweeps from the right, the second back from the
 * left: what they strike is sent back to where it was three seconds ago (an echo of it is left where it stood). The
 * third comes down overhead and slams the ground: a wave of orange light throws back all round, and the pruning line
 * is far higher. What any blow strikes when already weak enough is pruned: it crumbles into orange light and is gone.
 */
public class PruningBaton extends SpellEffectEntity {
    public static final int SWING_TICKS = 8;
    public static final int HIT_TICK = 4;
    public static final int LIFE = 14;
    public static final float REACH = 4.2f;
    /** How far back a struck target is sent, in ticks. */
    public static final int BACK = 60;

    /** The slam comes down a little later than the sweeps. */
    public static final int SLAM_TICK = 6;
    public static final float SLAM_RADIUS = 3.4f;

    private static final EntityDataAccessor<Integer> DATA_CASTER = SynchedEntityData.defineId(PruningBaton.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STAGE = SynchedEntityData.defineId(PruningBaton.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;
    private int spellLevel = 1;

    public PruningBaton(EntityType<? extends PruningBaton> entityType, Level level) {
        super(entityType, level);
    }

    /** One blow of the combo: {@code stage} 1 sweeps from the right, 2 back from the left, 3 slams down overhead. */
    public static void swing(Level level, LivingEntity caster, AbstractSpell spell, int spellLevel, float damage, int stage) {
        PruningBaton baton = new PruningBaton(ModEntities.PRUNING_BATON.get(), level);
        baton.setOwner(caster);
        baton.spellId = spell.getSpellId();
        baton.spellLevel = spellLevel;
        baton.damage = damage;
        baton.moveTo(caster.getX(), caster.getY(), caster.getZ(), caster.getYRot(), 0);
        baton.entityData.set(DATA_CASTER, caster.getId());
        baton.entityData.set(DATA_STAGE, stage);
        level.addFreshEntity(baton);
        CosmosFx.sound(level, caster.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1, 1.1f + .15f * stage);
        CosmosFx.sound(level, caster.position(), SoundEvents.BEACON_POWER_SELECT, .5f, 1.6f + .2f * stage);
    }

    /** Which blow of the combo it is (1 to 3). */
    public int getStage() {
        return this.entityData.get(DATA_STAGE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_CASTER, -1);
        builder.define(DATA_STAGE, 1);
    }

    @Nullable
    public Entity getCaster() {
        return level().getEntity(this.entityData.get(DATA_CASTER));
    }

    @Override
    public void tick() {
        super.tick();
        Entity caster = getCaster();
        if (caster != null) {
            setPos(caster.getX(), caster.getY(), caster.getZ());
        }
        if (level().isClientSide) {
            return;
        }
        if (tickCount >= LIFE || caster == null) {
            discard();
            return;
        }
        if (level() instanceof ServerLevel level && caster instanceof LivingEntity living) {
            if (getStage() < 3 && tickCount == HIT_TICK) {
                strike(level, living);
            } else if (getStage() >= 3 && tickCount == SLAM_TICK) {
                slam(level, living);
            }
        }
    }

    private void strike(ServerLevel level, LivingEntity caster) {
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (spell == null) {
            return;
        }
        Vec3 eye = caster.getEyePosition();
        Vec3 look = Vec3.directionFromRotation(0, caster.getYRot());
        for (LivingEntity target : SpellUtil.enemiesAround(level, eye, REACH, caster, e -> {
            Vec3 d = e.getBoundingBox().getCenter().subtract(eye);
            Vec3 flat = new Vec3(d.x, 0, d.z);
            return flat.lengthSqr() < 1.0E-4 || flat.normalize().dot(look) > .45;
        })) {
            Vec3 at = target.position();
            boolean hurt = DamageSources.applyDamage(target, damage, SpellUtil.damageSource(spell, this, caster));
            if (!target.isAlive()) {
                CosmosFx.time(level, target.getBoundingBox().getCenter(), 20, .4);
                continue;
            }
            if (!Cosmos.isBoss(target) && target.getHealth() <= PruningBatonSpell.pruneLine(spellLevel, target.getMaxHealth())) {
                CosmosFx.prune(level, target, SpellUtil.damageSource(spell, this, caster));
                continue;
            }
            if (!hurt) {
                continue;
            }
            // Back to where it was three seconds ago, leaving an echo where it stood
            TimeTrail.Sample past = TimeTrail.ago(target, BACK);
            if (past != null && past.pos().distanceTo(at) > 1 && past.pos().distanceTo(at) < 18) {
                TemporalEcho.leave(level, target, at, target.getYRot(), 16, Cosmos.ORANGE);
                target.teleportTo(level, past.pos().x, past.pos().y, past.pos().z, Set.of(), past.yRot(), target.getXRot());
                target.setDeltaMovement(Vec3.ZERO);
                target.hurtMarked = true;
                target.resetFallDistance();
                Vec3 step = past.pos().subtract(at).scale(1 / 8.0);
                for (int k = 0; k <= 8; k++) {
                    CosmosFx.time(level, at.add(step.scale(k)).add(0, target.getBbHeight() * .5, 0), 2, .15);
                }
                CosmosFx.fold(level, past.pos(), .7f, 1.5f);
            } else {
                Vec3 push = target.position().subtract(caster.position()).multiply(1, 0, 1).normalize().scale(.6);
                target.push(push.x, .2, push.z);
                target.hurtMarked = true;
            }
        }
    }

    /** The third blow: the baton slams the ground in front, a wave of orange light throws back what is round it. */
    private void slam(ServerLevel level, LivingEntity caster) {
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (spell == null) {
            return;
        }
        Vec3 look = Vec3.directionFromRotation(0, caster.getYRot());
        Vec3 at = SpellUtil.groundAt(level, caster.position().add(look.scale(1.8)), 2, 1);
        for (LivingEntity target : SpellUtil.enemiesAround(level, at.add(0, .8, 0), SLAM_RADIUS, caster, e -> true)) {
            boolean hurt = DamageSources.applyDamage(target, damage * 1.6f, SpellUtil.damageSource(spell, this, caster));
            if (!target.isAlive()) {
                continue;
            }
            if (!Cosmos.isBoss(target) && target.getHealth() <= PruningBatonSpell.pruneLine(spellLevel, target.getMaxHealth()) * 1.8f) {
                CosmosFx.prune(level, target, SpellUtil.damageSource(spell, this, caster));
            } else if (hurt) {
                Vec3 push = target.position().subtract(at).multiply(1, 0, 1);
                push = push.lengthSqr() < 1.0E-4 ? look : push.normalize();
                target.push(push.x * .9, .55, push.z * .9);
                target.hurtMarked = true;
            }
        }
        SpellUtil.particles(level, new io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions(1f, .55f, .12f, SLAM_RADIUS * 2), at.x, at.y + .05, at.z, 1, 0, 0, 0, 0);
        SpellUtil.particles(level, new io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions(1f, .85f, .55f, SLAM_RADIUS), at.x, at.y + .1, at.z, 1, 0, 0, 0, 0);
        CosmosFx.time(level, at.add(0, .3, 0), 40, SLAM_RADIUS * .4);
        SpellUtil.particles(level, net.minecraft.core.particles.ParticleTypes.CRIT, at.x, at.y + .2, at.z, 30, 1.2, .2, 1.2, .5);
        SpellUtil.cameraShake(level, at, 8, 10);
        CosmosFx.sound(level, at, SoundEvents.ANVIL_LAND, .6f, 1.6f);
        CosmosFx.sound(level, at, SoundEvents.BEACON_DEACTIVATE, 1, 1.4f);
        CosmosFx.sound(level, at, SoundEvents.GENERIC_EXPLODE.value(), .5f, 1.6f);
    }

    /** How far the swing has gone (0 to 1) at {@code age}. */
    public static float swing(float age) {
        float t = Math.min(1, age / SWING_TICKS);
        return t * t * (3 - 2 * t);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 64 * 64;
    }
}
