package com.waylays.arcanes.entity.wood;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.wood.WoodFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Timber!: a great oak bursts out of the ground beside the caster and grows in a moment, sways back creaking, then
 * topples forwards: what is under it when it lands is crushed (hurt hard, pressed into the ground, dazed) and what is
 * near thrown aside. It lies there a moment, then sinks back into the ground. Both sides work out its fall from its age.
 */
public class FallingTree extends SpellEffectEntity {
    public static final int GROW = 16;
    public static final int SWAY = 14;
    public static final int FALL = 12;
    public static final int LIE = 30;
    public static final int SINK = 16;
    public static final float TRUNK = .9f;

    private static final EntityDataAccessor<Float> DATA_HEIGHT = SynchedEntityData.defineId(FallingTree.class, EntityDataSerializers.FLOAT);

    @Nullable
    private String spellId;

    public FallingTree(EntityType<? extends FallingTree> entityType, Level level) {
        super(entityType, level);
    }

    /** A tree {@code height} tall, rising at {@code at} and falling towards {@code yaw}. */
    public static FallingTree raise(Level level, LivingEntity caster, AbstractSpell spell, Vec3 at, float yaw, float height, float damage) {
        FallingTree tree = new FallingTree(ModEntities.FALLING_TREE.get(), level);
        tree.setOwner(caster);
        tree.spellId = spell.getSpellId();
        tree.damage = damage;
        tree.moveTo(at.x, at.y, at.z, yaw, 0);
        tree.entityData.set(DATA_HEIGHT, height);
        level.addFreshEntity(tree);
        WoodFx.sprout(level, at, 1.6f);
        WoodFx.chips(level, at.add(0, .2, 0), 24, .7);
        return tree;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_HEIGHT, 10f);
    }

    public float getHeight() {
        return this.entityData.get(DATA_HEIGHT);
    }

    public static int impact() {
        return GROW + SWAY + FALL;
    }

    /** How far it has grown (0 to 1). */
    public float grown(float age) {
        float t = Mth.clamp(age / GROW, 0, 1);
        return 1 - (1 - t) * (1 - t) * (1 - t);
    }

    /** Its lean in radians: back a little as it sways, then down flat onto the ground. */
    public float lean(float age) {
        if (age < GROW) {
            return 0;
        }
        if (age < GROW + SWAY) {
            float t = (age - GROW) / SWAY;
            return -.16f * Mth.sin(t * Mth.PI) + .05f * Mth.sin(t * Mth.PI * 3);
        }
        if (age < impact()) {
            float t = (age - GROW - SWAY) / FALL;
            return Mth.HALF_PI * t * t;
        }
        // A small bounce as it lands
        float t = age - impact();
        return Mth.HALF_PI - .06f * Mth.sin(Math.min(t, 6) / 6 * Mth.PI);
    }

    /** How far it has sunk back into the ground at the end (0 to 1). */
    public float sunk(float age) {
        return Mth.clamp((age - impact() - LIE) / SINK, 0, 1);
    }

    /** Unit vector the tree falls along. */
    public Vec3 forward() {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > impact() + LIE + SINK) {
            discard();
            return;
        }
        if (tickCount == GROW) {
            WoodFx.creak(level, position().add(0, 4, 0), 1.6f);
        }
        if (tickCount == GROW + SWAY) {
            WoodFx.creak(level, position().add(0, 4, 0), 2);
            WoodFx.sound(level, position(), net.minecraft.sounds.SoundEvents.CHERRY_WOOD_BREAK, 2, .4f);
        }
        if (tickCount == impact()) {
            land(level);
        }
        if (tickCount == impact() + LIE) {
            WoodFx.leaves(level, position().add(forward().scale(getHeight() * .8)).add(0, 1, 0), 30, 2);
        }
    }

    /** It lands: whatever it falls on is crushed, whatever is near thrown aside. */
    private void land(ServerLevel level) {
        Vec3 f = forward();
        Vec3 side = new Vec3(-f.z, 0, f.x);
        float reach = getHeight() + 2.5f;
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        LivingEntity caster = getLivingOwner();
        if (spell != null) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(reach + 2, 4, reach + 2))) {
                if (!e.isAlive() || e == caster || SpellUtil.isAlly(caster, e) || Math.abs(e.getY() - getY()) > 3) {
                    continue;
                }
                Vec3 d = e.position().subtract(position());
                double along = d.dot(f), across = d.dot(side);
                if (along < -.5 || along > reach) {
                    continue;
                }
                // The trunk narrow near its foot, the crown wide at the end
                double width = along < getHeight() * .55 ? 1.3 : 3.2;
                double half = width + e.getBbWidth() * .5;
                if (Math.abs(across) > half + 1.5) {
                    continue;
                }
                boolean under = Math.abs(across) <= half;
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, under ? damage : damage * .4f, SpellUtil.damageSource(spell, this, caster));
                double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
                if (under) {
                    e.setDeltaMovement(e.getDeltaMovement().multiply(.2, 0, .2).add(0, -.6, 0));
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
                    e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
                } else {
                    Vec3 out = side.scale(Math.signum(across) * .8 * resist);
                    e.setDeltaMovement(e.getDeltaMovement().add(out).add(0, .35 * resist, 0));
                }
                e.hurtMarked = true;
            }
        }
        for (int k = 0; k <= 6; k++) {
            Vec3 at = position().add(f.scale(reach * k / 6.0)).add(0, .3, 0);
            WoodFx.chips(level, at, 8, .6);
            if (k >= 4) {
                WoodFx.leaves(level, at.add(0, .8, 0), 16, 1.6);
            }
        }
        Vec3 crown = position().add(f.scale(getHeight() * .8));
        WoodFx.crash(level, crown, 2.2f);
        WoodFx.sound(level, crown, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), .7f, .5f);
        WoodFx.sound(level, crown, net.minecraft.sounds.SoundEvents.AZALEA_LEAVES_BREAK, 2, .6f);
        SpellUtil.cameraShake(level, crown, 12, 18);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
    }
}
