package com.waylays.arcanes.entity.wood;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.wood.WoodFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Ring Saw: a slice of trunk (its rings on both faces, its bark round the rim, thorns all round it for teeth) is
 * hurled spinning flat like a saw blade. It cuts through every enemy on its way out, comes back to the caster's hand
 * and cuts them again on the way, then is caught. A wall sends it back early.
 */
public class RingSaw extends SpellEffectEntity {
    public static final float RADIUS = .9f;
    public static final int OUT = 18;
    public static final float SPEED = .95f;
    public static final float BACK_SPEED = 1.15f;
    /** Ticks an enemy is safe after a cut (so one pass cuts once). */
    public static final int GRACE = 8;

    private static final EntityDataAccessor<Integer> DATA_BACK = SynchedEntityData.defineId(RingSaw.class, EntityDataSerializers.INT);

    private final Int2IntOpenHashMap lastCut = new Int2IntOpenHashMap();
    @Nullable
    private String spellId;
    private Vec3 motion = Vec3.ZERO;

    public RingSaw(EntityType<? extends RingSaw> entityType, Level level) {
        super(entityType, level);
    }

    public static RingSaw hurl(Level level, LivingEntity caster, AbstractSpell spell, float damage) {
        RingSaw saw = new RingSaw(ModEntities.RING_SAW.get(), level);
        saw.setOwner(caster);
        saw.spellId = spell.getSpellId();
        saw.damage = damage;
        Vec3 look = caster.getLookAngle();
        Vec3 from = caster.getEyePosition().add(look.scale(1.2)).subtract(0, .4, 0);
        saw.moveTo(from.x, from.y, from.z, caster.getYRot(), 0);
        saw.motion = look.scale(SPEED);
        level.addFreshEntity(saw);
        WoodFx.sound(level, from, SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f, .7f);
        return saw;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_BACK, -1);
    }

    /** The tick it turned back, or -1 while it is still going out. */
    public int getBackTick() {
        return this.entityData.get(DATA_BACK);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity caster = getLivingOwner();
        if (caster == null || !caster.isAlive() || tickCount > 100) {
            WoodFx.chips(level, position(), 16, .4);
            discard();
            return;
        }
        if (getBackTick() < 0) {
            Vec3 next = position().add(motion);
            HitResult wall = level.clip(new ClipContext(position(), next.add(motion.normalize().scale(RADIUS)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (wall.getType() == HitResult.Type.BLOCK) {
                WoodFx.chips(level, wall.getLocation(), 12, .3);
                WoodFx.sound(level, wall.getLocation(), SoundEvents.WOOD_HIT, 1.4f, .8f);
                turnBack();
            } else {
                setPos(next.x, next.y, next.z);
                // It slows a little as it nears the turn
                motion = motion.scale(tickCount > OUT - 6 ? .9 : 1);
                if (tickCount >= OUT) {
                    turnBack();
                }
            }
        } else {
            Vec3 hand = caster.getEyePosition().subtract(0, .4, 0);
            Vec3 to = hand.subtract(position());
            if (to.length() < 1.4) {
                WoodFx.sound(level, hand, SoundEvents.WOOD_PLACE, 1.2f, 1.2f);
                discard();
                return;
            }
            float speed = Math.min(BACK_SPEED, .3f + (tickCount - getBackTick()) * .12f);
            motion = to.normalize().scale(Math.min(speed, to.length()));
            setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        }
        if (tickCount % 4 == 0) {
            WoodFx.sound(level, position(), SoundEvents.BAMBOO_WOOD_HIT, .8f, 1.6f);
        }
        cut(level, caster);
    }

    private void turnBack() {
        if (getBackTick() < 0) {
            this.entityData.set(DATA_BACK, tickCount);
        }
    }

    /** Every enemy at its edge is cut, once a pass. */
    private void cut(ServerLevel level, LivingEntity caster) {
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (spell == null) {
            return;
        }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(RADIUS + .6))) {
            if (!e.isAlive() || e == caster || SpellUtil.isAlly(caster, e) || tickCount - lastCut.getOrDefault(e.getId(), -100) < GRACE) {
                continue;
            }
            if (e.getBoundingBox().inflate(RADIUS * .7).contains(position())) {
                lastCut.put(e.getId(), tickCount);
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, damage, SpellUtil.damageSource(spell, this, caster));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1));
                WoodFx.chips(level, e.position().add(0, e.getBbHeight() * .6, 0), 10, .3);
                WoodFx.sound(level, e.position(), SoundEvents.PLAYER_ATTACK_CRIT, 1.2f, .8f);
            }
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
