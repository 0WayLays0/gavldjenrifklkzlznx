package com.waylays.arcanes.entity.fire;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.fire.FireFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Corona: a ring of Stellar Fire racing out from the caster over the ground; everything it passes through is burnt,
 * set alight and thrown back.
 */
public class Corona extends SpellEffectEntity {
    public static final int SPREAD = 20;
    public static final int FADE = 8;
    public static final float HEIGHT = 1.6f;

    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(Corona.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_HEAT = SynchedEntityData.defineId(Corona.class, EntityDataSerializers.INT);

    private final IntOpenHashSet burnt = new IntOpenHashSet();
    @Nullable
    private String spellId;

    public Corona(EntityType<? extends Corona> entityType, Level level) {
        super(entityType, level);
    }

    public static Corona flare(Level level, LivingEntity caster, AbstractSpell spell, float radius, float damage, int tier) {
        Corona ring = new Corona(ModEntities.CORONA.get(), level);
        ring.setOwner(caster);
        ring.spellId = spell.getSpellId();
        ring.damage = damage;
        ring.moveTo(caster.getX(), caster.getY(), caster.getZ(), 0, 0);
        ring.entityData.set(DATA_RADIUS, radius);
        ring.entityData.set(DATA_HEAT, tier);
        level.addFreshEntity(ring);
        FireFx.sound(level, caster.position(), SoundEvents.FIRECHARGE_USE, 2, .6f + .1f * tier);
        FireFx.sound(level, caster.position(), SoundEvents.BLAZE_SHOOT, 1.5f, .5f);
        return ring;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RADIUS, 6f);
        builder.define(DATA_HEAT, 0);
    }

    public int getHeat() {
        return this.entityData.get(DATA_HEAT);
    }

    /** How far it has spread at {@code age}. */
    public float reach(float age) {
        float t = Mth.clamp(age / SPREAD, 0, 1);
        return .8f + (this.entityData.get(DATA_RADIUS) - .8f) * (1 - (1 - t) * (1 - t));
    }

    /** How far it has faded (0 to 1). */
    public float faded(float age) {
        return Mth.clamp((age - SPREAD) / FADE, 0, 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > SPREAD + FADE) {
            discard();
            return;
        }
        float r = reach(tickCount);
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        LivingEntity caster = getLivingOwner();
        Vec3 c = position();
        for (LivingEntity e : SpellUtil.enemiesAround(level, c, r + 2, caster, x -> Math.abs(x.getY() - c.y) < 3)) {
            double d = Math.hypot(e.getX() - c.x, e.getZ() - c.z);
            if (d > r + .6 || burnt.contains(e.getId())) {
                continue;
            }
            burnt.add(e.getId());
            FireFx.burn(e, damage, spell, this, caster, getHeat());
            Vec3 away = new Vec3(e.getX() - c.x, 0, e.getZ() - c.z);
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
            double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
            e.setDeltaMovement(e.getDeltaMovement().add(away.scale(.9 * resist)).add(0, .35 * resist, 0));
            e.hurtMarked = true;
        }
        if (tickCount % 2 == 0 && tickCount <= SPREAD) {
            for (int k = 0; k < 8; k++) {
                float a = k * Mth.TWO_PI / 8 + tickCount * .3f;
                FireFx.flames(level, c.add(Mth.cos(a) * r, .3, Mth.sin(a) * r), 2, .3, getHeat());
            }
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
