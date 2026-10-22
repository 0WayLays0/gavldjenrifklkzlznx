package com.waylays.arcanes.entity.verdigris;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.verdigris.VerdigrisFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nullable;

/**
 * Strangling Vines: moss spreads under the victim and vines climb its legs and body, holding it in place and
 * squeezing it every second; then they wither back into the ground.
 */
public class VineSnare extends SpellEffectEntity {
    public static final int GROW = 10;
    public static final int WITHER = 10;
    public static final int SQUEEZE_TICKS = 20;

    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(VineSnare.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(VineSnare.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;

    public VineSnare(EntityType<? extends VineSnare> entityType, Level level) {
        super(entityType, level);
    }

    public static VineSnare bind(Level level, LivingEntity caster, AbstractSpell spell, LivingEntity target, int life, float damage) {
        VineSnare snare = new VineSnare(ModEntities.VINE_SNARE.get(), level);
        snare.setOwner(caster);
        snare.spellId = spell.getSpellId();
        snare.damage = damage;
        snare.moveTo(target.getX(), target.getY(), target.getZ(), level.random.nextFloat() * 360, 0);
        snare.entityData.set(DATA_TARGET, target.getId());
        snare.entityData.set(DATA_LIFE, life);
        level.addFreshEntity(snare);
        target.addEffect(new MobEffectInstance(ModEffects.ENTANGLED, life, 0, false, true, true));
        return snare;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_TARGET, -1);
        builder.define(DATA_LIFE, 80);
    }

    @Nullable
    public Entity getTarget() {
        return level().getEntity(this.entityData.get(DATA_TARGET));
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    /** How far the vines have climbed (0 to 1), withering at the end. */
    public float grown(float partialTicks) {
        float age = tickCount + partialTicks;
        return Mth.clamp(age / GROW, 0, 1) * Mth.clamp(1 - (age - getLife()) / WITHER, 0, 1);
    }

    @Override
    public void tick() {
        super.tick();
        Entity target = getTarget();
        if (level().isClientSide) {
            return;
        }
        if (!(target instanceof LivingEntity living) || !living.isAlive() || tickCount > getLife() + WITHER) {
            if (target instanceof LivingEntity living && living.hasEffect(ModEffects.ENTANGLED)) {
                living.removeEffect(ModEffects.ENTANGLED);
            }
            discard();
            return;
        }
        if (tickCount < getLife() && tickCount % SQUEEZE_TICKS == SQUEEZE_TICKS / 2) {
            AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
            if (spell != null) {
                living.invulnerableTime = 0;
                DamageSources.applyDamage(living, damage, SpellUtil.damageSource(spell, this, getOwner()));
            }
            VerdigrisFx.chips(level(), living.position().add(0, living.getBbHeight() * .4, 0), Blocks.VINE.defaultBlockState(), 8, .3);
            VerdigrisFx.sound(level(), living.position(), SoundEvents.MOSS_STEP, 1f, .6f);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 64 * 64;
    }
}
