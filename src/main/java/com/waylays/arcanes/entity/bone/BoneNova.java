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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The Bone Nova (the hedgehog): dozens of spikes of bone burst out of the caster's body in every direction at once,
 * impaling and throwing back everything close, then draw back in. It follows its caster; both sides work out the
 * spikes from its age.
 */
public class BoneNova extends SpellEffectEntity {
    public static final int GROW = 3;
    public static final int HOLD = 8;
    public static final int RETRACT = 6;
    public static final int SPIKES = 38;

    private static final EntityDataAccessor<Float> DATA_REACH = SynchedEntityData.defineId(BoneNova.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_OWNER = SynchedEntityData.defineId(BoneNova.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;

    public BoneNova(EntityType<? extends BoneNova> entityType, Level level) {
        super(entityType, level);
    }

    public static BoneNova burst(Level level, LivingEntity caster, AbstractSpell spell, float reach, float damage) {
        BoneNova nova = new BoneNova(ModEntities.BONE_NOVA.get(), level);
        nova.setOwner(caster);
        nova.spellId = spell.getSpellId();
        nova.damage = damage;
        nova.moveTo(caster.getX(), caster.getY(), caster.getZ(), 0, 0);
        nova.entityData.set(DATA_REACH, reach);
        nova.entityData.set(DATA_OWNER, caster.getId());
        level.addFreshEntity(nova);
        BoneFx.crack(level, caster.position(), 1.4f);
        BoneFx.sound(level, caster.position(), SoundEvents.BONE_BLOCK_BREAK, 1.5f, .6f);
        return nova;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_REACH, 3f);
        builder.define(DATA_OWNER, -1);
    }

    public float getReach() {
        return this.entityData.get(DATA_REACH);
    }

    @Nullable
    public Entity body() {
        return level().getEntity(this.entityData.get(DATA_OWNER));
    }

    /** How far the spikes are out (0 to 1). */
    public static float out(float age) {
        if (age < GROW) {
            float k = age / GROW;
            return 1 - (1 - k) * (1 - k);
        }
        return 1 - Mth.clamp((age - GROW - HOLD) / RETRACT, 0, 1);
    }

    /** Spike {@code i}'s direction: spread evenly over the sphere, none straight down. */
    public static Vec3 direction(int i) {
        float y = 1 - 1.7f * (i + .5f) / SPIKES;
        float r = Mth.sqrt(Math.max(0, 1 - y * y));
        float a = i * 2.3999632f;
        return new Vec3(Mth.cos(a) * r, y, Mth.sin(a) * r);
    }

    @Override
    public void tick() {
        super.tick();
        Entity body = body();
        if (body != null) {
            setPos(body.getX(), body.getY(), body.getZ());
        }
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > GROW + HOLD + RETRACT) {
            discard();
            return;
        }
        if (tickCount != GROW) {
            return;
        }
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        LivingEntity caster = getLivingOwner();
        float h = body == null ? 1 : body.getBbHeight() * .55f;
        Vec3 c = position().add(0, h, 0);
        if (spell != null) {
            for (LivingEntity e : SpellUtil.enemiesAround(level, c, getReach() + .7, caster, x -> true)) {
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, damage, SpellUtil.damageSource(spell, this, caster));
                double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
                Vec3 away = e.getBoundingBox().getCenter().subtract(c).normalize();
                e.setDeltaMovement(e.getDeltaMovement().add(away.scale(1.1 * resist)).add(0, .3 * resist, 0));
                e.hurtMarked = true;
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                BoneFx.marrow(level, e.getBoundingBox().getCenter(), 8, .3);
            }
        }
        BoneFx.splinters(level, c, 30, getReach() * .4);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}
