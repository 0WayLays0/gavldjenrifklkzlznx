package com.waylays.arcanes.entity.bone;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.bone.BoneFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
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

import javax.annotation.Nullable;

/**
 * The Rib Cage: great ribs burst out of the ground on both sides of the enemy and curve over it until they lock above,
 * a spine along the top: the prisoner is held fast inside, the ribs grinding into it, until they snap shut on it at the
 * end and crumble. Both sides work out the cage from its age; the server holds the prisoner.
 */
public class RibCage extends SpellEffectEntity {
    public static final int CLOSE = 10;
    public static final int SNAP = 6;
    public static final int CRUMBLE = 14;
    public static final int RIBS = 5;

    private static final EntityDataAccessor<Float> DATA_WIDTH = SynchedEntityData.defineId(RibCage.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEIGHT = SynchedEntityData.defineId(RibCage.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_HOLD = SynchedEntityData.defineId(RibCage.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;
    private int prisoner = -1;

    public RibCage(EntityType<? extends RibCage> entityType, Level level) {
        super(entityType, level);
    }

    public static RibCage close(Level level, LivingEntity caster, AbstractSpell spell, LivingEntity target, int hold, float damage) {
        RibCage cage = new RibCage(ModEntities.RIB_CAGE.get(), level);
        cage.setOwner(caster);
        cage.spellId = spell.getSpellId();
        cage.damage = damage;
        cage.prisoner = target.getId();
        cage.moveTo(target.getX(), target.getY(), target.getZ(), caster.getYRot(), 0);
        cage.entityData.set(DATA_WIDTH, Math.max(1.1f, target.getBbWidth() * .75f + .7f));
        cage.entityData.set(DATA_HEIGHT, Math.max(2f, target.getBbHeight() + .6f));
        cage.entityData.set(DATA_HOLD, hold);
        level.addFreshEntity(cage);
        BoneFx.erupt(level, target.position(), 1.4f);
        return cage;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_WIDTH, 1.3f);
        builder.define(DATA_HEIGHT, 2.4f);
        builder.define(DATA_HOLD, 60);
    }

    public float getWidth() {
        return this.entityData.get(DATA_WIDTH);
    }

    public float getHeight() {
        return this.entityData.get(DATA_HEIGHT);
    }

    public int getHold() {
        return this.entityData.get(DATA_HOLD);
    }

    /** The tick the ribs snap shut. */
    public int snaps() {
        return CLOSE + getHold();
    }

    /** How far the ribs have curved over (0 to 1). */
    public float closed(float age) {
        float t = Mth.clamp(age / CLOSE, 0, 1);
        return 1 - (1 - t) * (1 - t) * (1 - t);
    }

    /** How tight the ribs have snapped at the end (0 to 1). */
    public float snapped(float age) {
        return Mth.clamp((age - snaps()) / SNAP, 0, 1);
    }

    /** How far the cage has crumbled into the ground (0 to 1). */
    public float crumbled(float age) {
        return Mth.clamp((age - snaps() - SNAP) / CRUMBLE, 0, 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > snaps() + SNAP + CRUMBLE) {
            discard();
            return;
        }
        Entity e = level.getEntity(prisoner);
        LivingEntity target = e instanceof LivingEntity living && living.isAlive() ? living : null;
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        LivingEntity caster = getLivingOwner();
        if (target != null && tickCount >= CLOSE / 2 && tickCount < snaps() + SNAP) {
            // Held fast in the middle
            if (target.position().distanceToSqr(position()) > .16) {
                target.teleportTo(getX(), Math.max(getY(), Math.min(target.getY(), getY() + .5)), getZ());
            }
            target.setDeltaMovement(0, Math.min(0, target.getDeltaMovement().y), 0);
            target.hurtMarked = true;
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 5, false, false));
        }
        if (tickCount == CLOSE) {
            BoneFx.sound(level, position(), SoundEvents.BONE_BLOCK_PLACE, 1.5f, .5f);
            BoneFx.splinters(level, position().add(0, 1, 0), 18, .8);
        }
        if (target != null && spell != null && tickCount > CLOSE && tickCount < snaps() && (tickCount - CLOSE) % 20 == 0) {
            target.invulnerableTime = 0;
            DamageSources.applyDamage(target, damage * .3f, SpellUtil.damageSource(spell, this, caster));
            BoneFx.marrow(level, target.getBoundingBox().getCenter(), 6, .3);
            BoneFx.sound(level, position(), SoundEvents.SKELETON_STEP, 1f, .6f);
        }
        if (tickCount == snaps() + SNAP / 2) {
            if (target != null && spell != null) {
                target.invulnerableTime = 0;
                DamageSources.applyDamage(target, damage, SpellUtil.damageSource(spell, this, caster));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
                BoneFx.marrow(level, target.getBoundingBox().getCenter(), 20, .4);
            }
            BoneFx.crack(level, position(), 1.5f);
            BoneFx.sound(level, position(), SoundEvents.BONE_BLOCK_BREAK, 1.6f, .5f);
        }
        if (tickCount == snaps() + SNAP) {
            BoneFx.splinters(level, position().add(0, 1, 0), 40, 1);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}
