package com.waylays.arcanes.entity.fire;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.fire.FireFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Searing Brand: a sigil of Stellar Fire burnt over a creature's head. It smoulders for {@link #FUSE} ticks, tightening
 * and blazing brighter, then a pillar of fire bursts up out of the ground under the creature.
 */
public class SearingBrand extends SpellEffectEntity {
    public static final int FUSE = 60;

    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(SearingBrand.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HEAT = SynchedEntityData.defineId(SearingBrand.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;

    public SearingBrand(EntityType<? extends SearingBrand> entityType, Level level) {
        super(entityType, level);
    }

    public static SearingBrand mark(Level level, LivingEntity caster, AbstractSpell spell, LivingEntity target, float damage, int tier) {
        SearingBrand brand = new SearingBrand(ModEntities.SEARING_BRAND.get(), level);
        brand.setOwner(caster);
        brand.spellId = spell.getSpellId();
        brand.damage = damage;
        brand.moveTo(target.getX(), target.getY(), target.getZ(), 0, 0);
        brand.entityData.set(DATA_TARGET, target.getId());
        brand.entityData.set(DATA_HEAT, tier);
        level.addFreshEntity(brand);
        FireFx.sound(level, target.position(), SoundEvents.FIRECHARGE_USE, 1, 1.4f);
        return brand;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_TARGET, -1);
        builder.define(DATA_HEAT, 0);
    }

    @Nullable
    public Entity getTarget() {
        return level().getEntity(this.entityData.get(DATA_TARGET));
    }

    public int getHeat() {
        return this.entityData.get(DATA_HEAT);
    }

    public float fuse(float age) {
        return Mth.clamp(age / FUSE, 0, 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Entity target = getTarget();
        if (target != null && target.isAlive()) {
            setPos(target.getX(), target.getY(), target.getZ());
        }
        if (tickCount % 15 == 0 && tickCount < FUSE) {
            FireFx.sound(level, position(), SoundEvents.FIRE_AMBIENT, 1, .8f + tickCount * .015f);
        }
        if (tickCount >= FUSE) {
            AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
            if (spell != null) {
                FirePillar.raise(level, getLivingOwner(), spell, position(), 0, false, damage, getHeat());
            }
            discard();
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}
