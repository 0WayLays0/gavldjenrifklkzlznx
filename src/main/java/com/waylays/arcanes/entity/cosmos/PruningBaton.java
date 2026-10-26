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
 * The TVA's pruning baton in the caster's hand for one swing: a rod of dark steel with a glowing orange tip that leaves
 * a trail of time through the air. What it strikes is sent back to where it was three seconds ago (an echo of it is
 * left where it stood), and what it strikes when already weak enough is pruned: it crumbles into orange dust.
 */
public class PruningBaton extends SpellEffectEntity {
    public static final int SWING_TICKS = 8;
    public static final int HIT_TICK = 4;
    public static final int LIFE = 14;
    public static final float REACH = 4.2f;
    /** How far back a struck target is sent, in ticks. */
    public static final int BACK = 60;

    private static final EntityDataAccessor<Integer> DATA_CASTER = SynchedEntityData.defineId(PruningBaton.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;
    private int spellLevel = 1;

    public PruningBaton(EntityType<? extends PruningBaton> entityType, Level level) {
        super(entityType, level);
    }

    public static void swing(Level level, LivingEntity caster, AbstractSpell spell, int spellLevel, float damage) {
        PruningBaton baton = new PruningBaton(ModEntities.PRUNING_BATON.get(), level);
        baton.setOwner(caster);
        baton.spellId = spell.getSpellId();
        baton.spellLevel = spellLevel;
        baton.damage = damage;
        baton.moveTo(caster.getX(), caster.getY(), caster.getZ(), caster.getYRot(), 0);
        baton.entityData.set(DATA_CASTER, caster.getId());
        level.addFreshEntity(baton);
        CosmosFx.sound(level, caster.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1, 1.3f);
        CosmosFx.sound(level, caster.position(), SoundEvents.BEACON_POWER_SELECT, .5f, 1.8f);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_CASTER, -1);
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
        if (tickCount == HIT_TICK && level() instanceof ServerLevel level && caster instanceof LivingEntity living) {
            strike(level, living);
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
