package com.waylays.arcanes.entity.fire;

import com.waylays.arcanes.entity.SpellEffectEntity;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Stellar Breath: as long as the caster holds it, a cone of fire roars out of their hand where they look, in their
 * heat; everything in it burns and catches fire. Held on, it stokes the caster's heat (see the spell).
 */
public class StellarBreath extends SpellEffectEntity {
    public static final float RANGE = 8;
    /** Cosine of the cone's half angle. */
    public static final double CONE = Math.cos(Math.toRadians(24));
    public static final int HIT_TICKS = 5;

    private static final EntityDataAccessor<Integer> DATA_OWNER = SynchedEntityData.defineId(StellarBreath.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HEAT = SynchedEntityData.defineId(StellarBreath.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_REACH = SynchedEntityData.defineId(StellarBreath.class, EntityDataSerializers.FLOAT);

    @Nullable
    private String spellId;
    private int heldUntil;

    public StellarBreath(EntityType<? extends StellarBreath> entityType, Level level) {
        super(entityType, level);
    }

    /** Keeps the caster's breath going a few more ticks (starting it if there is none), at heat {@code tier}. */
    public static StellarBreath hold(Level level, LivingEntity caster, AbstractSpell spell, float damage, int tier) {
        for (StellarBreath breath : level.getEntitiesOfClass(StellarBreath.class, caster.getBoundingBox().inflate(4),
                b -> !b.isRemoved() && caster.getUUID().equals(b.getOwnerUUID()))) {
            breath.heldUntil = breath.tickCount + 4;
            breath.damage = damage;
            breath.entityData.set(DATA_HEAT, tier);
            return breath;
        }
        StellarBreath breath = new StellarBreath(ModEntities.STELLAR_BREATH.get(), level);
        breath.setOwner(caster);
        breath.spellId = spell.getSpellId();
        breath.damage = damage;
        breath.heldUntil = 4;
        Vec3 from = origin(caster, 1);
        breath.moveTo(from.x, from.y, from.z, 0, 0);
        breath.entityData.set(DATA_OWNER, caster.getId());
        breath.entityData.set(DATA_HEAT, tier);
        level.addFreshEntity(breath);
        FireFx.sound(level, from, SoundEvents.BLAZE_SHOOT, 1.2f, .6f);
        return breath;
    }

    /** Where the fire leaves the caster's hand. */
    public static Vec3 origin(Entity caster, float partialTicks) {
        Vec3 look = caster.getViewVector(partialTicks);
        return caster.getEyePosition(partialTicks).add(look.scale(.8)).add(0, -.35, 0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER, -1);
        builder.define(DATA_HEAT, 0);
        builder.define(DATA_REACH, RANGE);
    }

    @Nullable
    public Entity getCaster() {
        return level().getEntity(this.entityData.get(DATA_OWNER));
    }

    public int getHeat() {
        return this.entityData.get(DATA_HEAT);
    }

    /** How far it reaches before something stops it. */
    public float getReach() {
        return this.entityData.get(DATA_REACH);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity owner = getLivingOwner();
        if (owner == null || !owner.isAlive() || tickCount > heldUntil) {
            discard();
            return;
        }
        Vec3 from = origin(owner, 1);
        Vec3 look = owner.getLookAngle();
        setPos(from.x, from.y, from.z);
        BlockHitResult block = level.clip(new ClipContext(from, from.add(look.scale(RANGE)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        float reach = block.getType() == HitResult.Type.MISS ? RANGE : (float) block.getLocation().distanceTo(from);
        this.entityData.set(DATA_REACH, reach);
        if (tickCount % HIT_TICKS == 0) {
            AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
            for (LivingEntity e : SpellUtil.enemiesAround(level, from, reach + 1, owner, x -> true)) {
                Vec3 to = e.getBoundingBox().getCenter().subtract(from);
                double d = to.length();
                if (d < .1 || to.normalize().dot(look) > CONE) {
                    FireFx.burn(e, damage, spell, this, owner, getHeat());
                }
            }
        }
        if (tickCount % 3 == 0) {
            FireFx.flames(level, from.add(look.scale(reach * .6)), 4, .6, getHeat());
        }
        if (tickCount % 10 == 0) {
            FireFx.sound(level, from, SoundEvents.FIRE_AMBIENT, 1.4f, .7f + .1f * getHeat());
            FireFx.sound(level, from, SoundEvents.BLAZE_BURN, .7f, .8f);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
