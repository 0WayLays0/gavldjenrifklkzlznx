package com.waylays.arcanes.entity.gem;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.gem.Gem;
import com.waylays.arcanes.spells.gem.GemFx;
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
 * The Lapis Seal: four tablets of lapis rise out of the ground round the enemy, golden runes burning on them, and bind
 * it with chains of golden light. Sealed, it can neither cast nor move, and the seal burns it; when its time is up
 * the tablets break and the seal bursts.
 */
public class LapisSeal extends SpellEffectEntity {
    public static final int TABLETS = 4;
    public static final float RADIUS = 1.7f;
    public static final int RISE = 14;
    public static final int BREAK = 12;

    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(LapisSeal.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(LapisSeal.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;

    public LapisSeal(EntityType<? extends LapisSeal> entityType, Level level) {
        super(entityType, level);
    }

    public static LapisSeal seal(Level level, LivingEntity caster, AbstractSpell spell, LivingEntity target, int life, float damage) {
        LapisSeal seal = new LapisSeal(ModEntities.LAPIS_SEAL.get(), level);
        seal.setOwner(caster);
        seal.spellId = spell.getSpellId();
        seal.damage = damage;
        seal.moveTo(target.getX(), target.getY(), target.getZ(), level.random.nextFloat() * 90, 0);
        seal.entityData.set(DATA_TARGET, target.getId());
        seal.entityData.set(DATA_LIFE, RISE + life);
        level.addFreshEntity(seal);
        GemFx.sigil(level, target.position(), Gem.LAPIS, RADIUS + 1, RISE + 10);
        GemFx.sound(level, target.position(), SoundEvents.ENCHANTMENT_TABLE_USE, 1.5f, .6f);
        return seal;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_TARGET, -1);
        builder.define(DATA_LIFE, 80);
    }

    @Nullable
    public LivingEntity getTarget() {
        return level().getEntity(this.entityData.get(DATA_TARGET)) instanceof LivingEntity living ? living : null;
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    /** How far the tablets have risen (0 to 1, with an overshoot). */
    public float risen(float partialTicks) {
        float t = Mth.clamp((tickCount + partialTicks) / RISE, 0, 1);
        return t * (1 + .15f * Mth.sin(t * Mth.PI));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        int life = getLife();
        if (tickCount > life + BREAK) {
            discard();
            return;
        }
        LivingEntity target = getTarget();
        if (tickCount >= life) {
            return;
        }
        if (target == null || !target.isAlive()) {
            this.entityData.set(DATA_LIFE, tickCount);
            breakSeal(level, null);
            return;
        }
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (tickCount == RISE) {
            int left = life - RISE;
            target.addEffect(new MobEffectInstance(ModEffects.SILENCE, left, 0));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, left, 4));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, left, 0));
            GemFx.sound(level, target.position(), SoundEvents.CHAIN_PLACE, 1.6f, .7f);
            GemFx.sound(level, target.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.6f, .5f);
        }
        if (tickCount >= RISE) {
            // Bound where it stands
            if (target.position().distanceToSqr(position()) > .5 * .5) {
                target.teleportTo(getX(), target.getY(), getZ());
            }
            target.setDeltaMovement(0, Math.min(0, target.getDeltaMovement().y), 0);
            target.hurtMarked = true;
            if ((tickCount - RISE) % 20 == 10 && spell != null) {
                target.invulnerableTime = 0;
                DamageSources.applyDamage(target, damage * .25f, SpellUtil.damageSource(spell, this, getOwner()));
                GemFx.glitter(level, target.getBoundingBox().getCenter(), Gem.LAPIS, 10, .4);
            }
        }
        if (tickCount == life - 1) {
            breakSeal(level, spell);
        }
    }

    /** The tablets break and the seal bursts on its prisoner. */
    private void breakSeal(ServerLevel level, @Nullable AbstractSpell spell) {
        LivingEntity target = getTarget();
        if (target != null && target.isAlive() && spell != null) {
            target.invulnerableTime = 0;
            DamageSources.applyDamage(target, damage, SpellUtil.damageSource(spell, this, getOwner()));
            target.removeEffect(ModEffects.SILENCE);
            target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
        for (int i = 0; i < TABLETS; i++) {
            GemFx.shatter(level, position().add(tablet(i, tickCount)).add(0, .7, 0), Gem.LAPIS, .9f, 6);
        }
        GemFx.sound(level, position(), SoundEvents.CHAIN_BREAK, 1.6f, .6f);
    }

    /** Where tablet {@code i} stands, from the middle, at age {@code age}: they turn slowly round their prisoner. */
    public Vec3 tablet(int i, float age) {
        float a = getYRot() * Mth.DEG_TO_RAD + i * Mth.TWO_PI / TABLETS + age * .012f;
        return new Vec3(Mth.cos(a) * RADIUS, 0, Mth.sin(a) * RADIUS);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
