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
 * Leaf Storm: a whirlwind of razor-edged leaves (and azalea petals) gathers in front of the caster and wanders slowly
 * the way they looked, drawing enemies into it, slashing them over and over, blinding them in its whirl, lifting the
 * lighter ones off the ground. It scatters to the winds when its time is up.
 */
public class LeafStorm extends SpellEffectEntity {
    public static final float RADIUS = 3;
    public static final float HEIGHT = 6.5f;
    public static final float SPEED = .12f;
    public static final int GATHER = 14;
    public static final int SCATTER = 14;

    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(LeafStorm.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;

    public LeafStorm(EntityType<? extends LeafStorm> entityType, Level level) {
        super(entityType, level);
    }

    public static LeafStorm raise(Level level, LivingEntity caster, AbstractSpell spell, Vec3 at, float yaw, int life, float damage) {
        LeafStorm storm = new LeafStorm(ModEntities.LEAF_STORM.get(), level);
        storm.setOwner(caster);
        storm.spellId = spell.getSpellId();
        storm.damage = damage;
        storm.moveTo(at.x, at.y, at.z, yaw, 0);
        storm.entityData.set(DATA_LIFE, life);
        level.addFreshEntity(storm);
        WoodFx.sound(level, at, SoundEvents.AZALEA_LEAVES_BREAK, 1.5f, .5f);
        WoodFx.sound(level, at, SoundEvents.ELYTRA_FLYING, .6f, 1.4f);
        return storm;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LIFE, 120);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    /** How strong the whirl is (0 to 1): gathering, then scattering at the end. */
    public float strength(float age) {
        float in = Mth.clamp(age / GATHER, 0, 1);
        float out = Mth.clamp((age - getLife()) / SCATTER, 0, 1);
        return in * (1 - out);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > getLife() + SCATTER) {
            discard();
            return;
        }
        if (tickCount == getLife()) {
            WoodFx.leaves(level, position().add(0, 2, 0), 60, RADIUS);
            WoodFx.sound(level, position(), SoundEvents.AZALEA_LEAVES_BREAK, 2, .7f);
        }
        if (tickCount >= getLife()) {
            return;
        }
        // It wanders the way it was sent, hugging the ground
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        Vec3 next = position().add(-Mth.sin(yaw) * SPEED, 0, Mth.cos(yaw) * SPEED);
        Vec3 ground = SpellUtil.groundAt(level, next, 3, 2);
        setPos(next.x, Mth.lerp(.25, getY(), ground.y), next.z);
        if (tickCount % 12 == 0) {
            WoodFx.sound(level, position(), SoundEvents.AZALEA_LEAVES_STEP, 1.4f, .6f + random.nextFloat() * .3f);
            WoodFx.sound(level, position(), SoundEvents.PHANTOM_FLAP, .6f, 1.6f);
        }
        if (tickCount % 3 == 0) {
            WoodFx.leaves(level, position().add(0, 1 + random.nextDouble() * 4, 0), 3, RADIUS * .6);
        }
        if (tickCount < GATHER / 2) {
            return;
        }
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        LivingEntity caster = getLivingOwner();
        Vec3 axis = position();
        for (LivingEntity e : SpellUtil.enemiesAround(level, axis.add(0, HEIGHT * .5, 0), RADIUS + 3.5, caster,
                x -> x.getY() < getY() + HEIGHT && x.getY() + x.getBbHeight() > getY() - 1)) {
            Vec3 to = new Vec3(axis.x - e.getX(), 0, axis.z - e.getZ());
            double d = to.length();
            double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
            Vec3 in = d < 1.0E-3 ? Vec3.ZERO : to.scale(1 / d);
            // Drawn in and whirled round
            Vec3 round = new Vec3(-in.z, 0, in.x);
            double pull = d > RADIUS ? .12 : .04;
            Vec3 v = e.getDeltaMovement().add(in.scale(pull * resist)).add(round.scale(.1 * resist));
            if (d < RADIUS) {
                v = new Vec3(v.x, Math.max(v.y, .06 * resist + (e.getY() < getY() + 2.5 ? .05 : -.02)), v.z);
            }
            e.setDeltaMovement(v);
            e.hurtMarked = true;
            if (d < RADIUS + .5 && spell != null && tickCount % 8 == 0) {
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, damage, SpellUtil.damageSource(spell, this, caster));
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, false));
                WoodFx.leaves(level, e.getBoundingBox().getCenter(), 4, .3);
                WoodFx.sound(level, e.position(), SoundEvents.SWEET_BERRY_BUSH_BREAK, .8f, 1.3f);
            }
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
