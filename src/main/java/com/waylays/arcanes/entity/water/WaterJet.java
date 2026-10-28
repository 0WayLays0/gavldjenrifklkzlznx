package com.waylays.arcanes.entity.water;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.water.WaterFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/**
 * Pressure Jet: as long as the caster holds it, a jet of water thin as a blade shoots from their hand: whatever it
 * touches is cut, soaked and driven back step by step; where it strikes it bursts into spray and mist.
 */
public class WaterJet extends SpellEffectEntity {
    public static final float RANGE = 16;
    public static final int HIT_TICKS = 4;

    private static final EntityDataAccessor<Integer> DATA_OWNER = SynchedEntityData.defineId(WaterJet.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> DATA_END = SynchedEntityData.defineId(WaterJet.class, EntityDataSerializers.VECTOR3);

    @Nullable
    private String spellId;
    private int heldUntil;

    public WaterJet(EntityType<? extends WaterJet> entityType, Level level) {
        super(entityType, level);
    }

    /** Keeps the caster's jet going a few more ticks (starting it if there is none). */
    public static WaterJet hold(Level level, LivingEntity caster, AbstractSpell spell, float damage) {
        for (WaterJet jet : level.getEntitiesOfClass(WaterJet.class, caster.getBoundingBox().inflate(4),
                j -> !j.isRemoved() && caster.getUUID().equals(j.getOwnerUUID()))) {
            jet.heldUntil = jet.tickCount + 4;
            jet.damage = damage;
            return jet;
        }
        WaterJet jet = new WaterJet(ModEntities.WATER_JET.get(), level);
        jet.setOwner(caster);
        jet.spellId = spell.getSpellId();
        jet.damage = damage;
        jet.heldUntil = 4;
        Vec3 from = origin(caster, 1);
        jet.moveTo(from.x, from.y, from.z, 0, 0);
        jet.entityData.set(DATA_OWNER, caster.getId());
        jet.entityData.set(DATA_END, from.toVector3f());
        level.addFreshEntity(jet);
        WaterFx.sound(level, from, SoundEvents.BUCKET_EMPTY, 1.2f, 1.4f);
        return jet;
    }

    /** Where the jet leaves the caster's hand. */
    public static Vec3 origin(Entity caster, float partialTicks) {
        Vec3 look = caster.getViewVector(partialTicks);
        return caster.getEyePosition(partialTicks).add(look.scale(.8)).add(0, -.3, 0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER, -1);
        builder.define(DATA_END, new Vector3f());
    }

    @Nullable
    public Entity getCaster() {
        return level().getEntity(this.entityData.get(DATA_OWNER));
    }

    public Vec3 getEnd() {
        return new Vec3(this.entityData.get(DATA_END));
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
        Vec3 to = from.add(look.scale(RANGE));
        BlockHitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
        // It stops in the first enemy it meets
        LivingEntity first = null;
        double nearest = from.distanceTo(end);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, end).inflate(1))) {
            if (!e.isAlive() || SpellUtil.isAlly(owner, e)) {
                continue;
            }
            var clip = e.getBoundingBox().inflate(.3).clip(from, end);
            if (clip.isPresent() && clip.get().distanceTo(from) < nearest) {
                nearest = clip.get().distanceTo(from);
                first = e;
                end = clip.get();
            }
        }
        this.entityData.set(DATA_END, end.toVector3f());
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (first != null && tickCount % HIT_TICKS == 0 && spell != null) {
            first.invulnerableTime = 0;
            DamageSources.applyDamage(first, damage, SpellUtil.damageSource(spell, this, owner));
            double resist = 1 - Mth.clamp(first.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
            first.setDeltaMovement(first.getDeltaMovement().add(look.x * .32 * resist, .04, look.z * .32 * resist));
            first.hurtMarked = true;
            WaterFx.soak(first, 40);
        }
        if (tickCount % 2 == 0) {
            WaterFx.spray(level, end, 4, .25);
            WaterFx.mist(level, end, 2, .3);
        }
        if (tickCount % 10 == 0) {
            WaterFx.sound(level, from, SoundEvents.WATER_AMBIENT, 1, 1.6f);
            WaterFx.sound(level, end, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, .5f, 1.6f);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
