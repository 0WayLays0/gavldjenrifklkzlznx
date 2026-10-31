package com.waylays.arcanes.entity.fire;

import com.waylays.arcanes.entity.SpellProjectile;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
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
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Ember Bolt: a ball of Stellar Fire flung from the hand, burning in the caster's heat. It bursts on what it strikes,
 * burning everything close by (the one it strikes hardest) and setting it alight.
 */
public class EmberBolt extends SpellProjectile {
    public static final float SPEED = 1.5f;
    public static final float SPLASH = 1.8f;

    private static final EntityDataAccessor<Integer> DATA_HEAT = SynchedEntityData.defineId(EmberBolt.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;

    public EmberBolt(EntityType<? extends EmberBolt> entityType, Level level) {
        super(entityType, level);
    }

    public static EmberBolt fling(Level level, LivingEntity caster, AbstractSpell spell, float damage, int tier) {
        EmberBolt bolt = new EmberBolt(ModEntities.EMBER_BOLT.get(), level);
        bolt.setOwner(caster);
        bolt.spellId = spell.getSpellId();
        bolt.setDamage(damage);
        bolt.entityData.set(DATA_HEAT, tier);
        Vec3 look = caster.getLookAngle();
        Vec3 from = caster.getEyePosition().add(look.scale(.8)).subtract(0, .25, 0);
        bolt.setPos(from.x, from.y - bolt.getBbHeight() * .5, from.z);
        bolt.setDeltaMovement(look.scale(SPEED));
        bolt.setYRot((float) (Mth.atan2(look.x, look.z) * Mth.RAD_TO_DEG));
        bolt.setXRot((float) (Mth.atan2(look.y, look.horizontalDistance()) * Mth.RAD_TO_DEG));
        bolt.yRotO = bolt.getYRot();
        bolt.xRotO = bolt.getXRot();
        level.addFreshEntity(bolt);
        FireFx.sound(level, from, SoundEvents.BLAZE_SHOOT, 1, .9f + .1f * tier);
        return bolt;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HEAT, 0);
    }

    public int getHeat() {
        return this.entityData.get(DATA_HEAT);
    }

    @Override
    protected double gravity() {
        return .006;
    }

    @Override
    protected int lifetime() {
        return 50;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !SpellUtil.isAlly(getOwner(), target);
    }

    @Override
    protected void onImpact(HitResult hit) {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        Vec3 at = hit.getLocation();
        LivingEntity struck = hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity e ? e : null;
        if (struck != null) {
            FireFx.burn(struck, damage, spell, this, getOwner(), getHeat());
        }
        for (LivingEntity e : SpellUtil.enemiesAround(level, at, SPLASH, getOwner(), x -> x != struck)) {
            FireFx.burn(e, damage * .5f, spell, this, getOwner(), getHeat());
        }
        FireBurst.at(level, at.subtract(0, hit.getType() == HitResult.Type.BLOCK ? 0 : .4, 0), 1.1f, getHeat());
        discard();
    }

    @Override
    protected void onExpire() {
        if (level() instanceof ServerLevel level) {
            FireBurst.at(level, position(), .7f, getHeat());
        }
        super.onExpire();
    }
}
