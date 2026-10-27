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
import net.minecraft.sounds.SoundSource;
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
 * The Colossal Spike: the ground cracks and glows crimson over the spot, rumbling; then a spike of bone as tall as a
 * tree bursts out of it in an instant, a ring of lesser spikes round its foot, impaling and hurling high whatever stood
 * there. It stands a while, then crumbles back. Both sides work it out from its age.
 */
public class GiantSpike extends SpellEffectEntity {
    public static final int TELEGRAPH = 14;
    public static final int RISE = 4;
    public static final int STAND = 50;
    public static final int CRUMBLE = 16;
    public static final float CORE = 2.2f;
    public static final float RING = 4.6f;

    private static final EntityDataAccessor<Float> DATA_HEIGHT = SynchedEntityData.defineId(GiantSpike.class, EntityDataSerializers.FLOAT);

    @Nullable
    private String spellId;

    public GiantSpike(EntityType<? extends GiantSpike> entityType, Level level) {
        super(entityType, level);
    }

    public static GiantSpike raise(Level level, LivingEntity caster, AbstractSpell spell, Vec3 at, float height, float damage) {
        GiantSpike spike = new GiantSpike(ModEntities.GIANT_SPIKE.get(), level);
        spike.setOwner(caster);
        spike.spellId = spell.getSpellId();
        spike.damage = damage;
        spike.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360, 0);
        spike.entityData.set(DATA_HEIGHT, height);
        level.addFreshEntity(spike);
        BoneFx.sound(level, at, SoundEvents.BONE_BLOCK_HIT, 1.5f, .4f);
        return spike;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_HEIGHT, 10f);
    }

    public float getHeight() {
        return this.entityData.get(DATA_HEIGHT);
    }

    /** How far out of the ground it stands (0 to 1): bursting up fast, then crumbling back. */
    public float risen(float age) {
        float up = Mth.clamp((age - TELEGRAPH) / RISE, 0, 1);
        float down = Mth.clamp((age - TELEGRAPH - RISE - STAND) / CRUMBLE, 0, 1);
        return (1 - (1 - up) * (1 - up)) * (1 - down * down);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > TELEGRAPH + RISE + STAND + CRUMBLE) {
            discard();
            return;
        }
        if (tickCount < TELEGRAPH && tickCount % 3 == 0) {
            BoneFx.splinters(level, position().add(0, .2, 0), 6, 1);
            BoneFx.marrow(level, position().add(0, .2, 0), 4, 1.2);
            BoneFx.sound(level, position(), SoundEvents.BONE_BLOCK_STEP, 1.2f, .5f);
        }
        if (tickCount == TELEGRAPH) {
            erupt(level);
        }
        if (tickCount == TELEGRAPH + RISE + STAND) {
            BoneFx.sound(level, position(), SoundEvents.BONE_BLOCK_BREAK, 2f, .4f);
            BoneFx.splinters(level, position().add(0, 2, 0), 40, 1.2);
        }
    }

    private void erupt(ServerLevel level) {
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        LivingEntity caster = getLivingOwner();
        if (spell != null) {
            for (LivingEntity e : SpellUtil.enemiesAround(level, position().add(0, 1, 0), RING, caster, x -> x.getY() < getY() + 4 && x.getY() > getY() - 2)) {
                double d = Math.hypot(e.getX() - getX(), e.getZ() - getZ());
                boolean core = d < CORE + e.getBbWidth() * .5;
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, core ? damage : damage * .5f, SpellUtil.damageSource(spell, this, caster));
                double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
                Vec3 away = new Vec3(e.getX() - getX(), 0, e.getZ() - getZ());
                away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize();
                e.setDeltaMovement(e.getDeltaMovement().multiply(.2, 0, .2).add(away.scale((core ? .3 : .8) * resist))
                        .add(0, (core ? 1.35 : .6) * Math.max(.4, resist), 0));
                e.hurtMarked = true;
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                BoneFx.marrow(level, e.getBoundingBox().getCenter(), 14, .4);
            }
        }
        BoneFx.splinters(level, position().add(0, 1, 0), 60, 1.5);
        BoneFx.erupt(level, position(), 2.5f);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.6f, .5f);
        SpellUtil.cameraShake(level, position(), 16, 24);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
    }
}
