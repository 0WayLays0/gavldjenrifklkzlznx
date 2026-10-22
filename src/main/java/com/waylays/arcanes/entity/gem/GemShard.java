package com.waylays.arcanes.entity.gem;

import com.waylays.arcanes.entity.SpellProjectile;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * A shard of gem flung at an enemy and steering after it: a star of the Lapis Constellation, a plate of the Diamond
 * Shell striking back. It bursts in glitter on what it hits.
 */
public class GemShard extends SpellProjectile {
    public static final float SPEED = .55f;
    public static final float MAX_SPEED = 1.4f;

    private static final EntityDataAccessor<Integer> DATA_GEM = SynchedEntityData.defineId(GemShard.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(GemShard.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;

    public GemShard(EntityType<? extends GemShard> entityType, Level level) {
        super(entityType, level);
    }

    public static GemShard fling(Level level, LivingEntity owner, AbstractSpell spell, Gem gem, Vec3 from, LivingEntity target, float damage) {
        GemShard shard = new GemShard(ModEntities.GEM_SHARD.get(), level);
        shard.setOwner(owner);
        shard.spellId = spell.getSpellId();
        shard.setDamage(damage);
        shard.entityData.set(DATA_GEM, gem.ordinal());
        shard.entityData.set(DATA_TARGET, target.getId());
        shard.setPos(from.x, from.y - shard.getBbHeight() * .5, from.z);
        Vec3 dir = target.getBoundingBox().getCenter().subtract(from);
        // Off at an angle first, then it curves in
        Vec3 side = dir.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
        shard.setDeltaMovement(dir.normalize().scale(SPEED).add(side.scale((level.random.nextFloat() - .5f) * .8f)).add(0, .25, 0));
        level.addFreshEntity(shard);
        return shard;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_GEM, 0);
        builder.define(DATA_TARGET, -1);
    }

    public Gem getGem() {
        return Gem.of(this.entityData.get(DATA_GEM));
    }

    @Override
    protected int lifetime() {
        return 80;
    }

    @Override
    public void tick() {
        Entity target = level().getEntity(this.entityData.get(DATA_TARGET));
        if (target instanceof LivingEntity living && living.isAlive()) {
            // Steers after its target, faster and faster
            Vec3 want = living.getBoundingBox().getCenter().subtract(position()).normalize();
            Vec3 v = getDeltaMovement();
            double speed = Math.min(MAX_SPEED, v.length() + .06);
            setDeltaMovement(v.normalize().scale(.75).add(want.scale(.25 + Math.min(.5, tickCount * .03))).normalize().scale(speed));
        }
        super.tick();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !SpellUtil.isAlly(getOwner(), target);
    }

    @Override
    protected void onImpact(HitResult hit) {
        if (level() instanceof ServerLevel level) {
            Vec3 at = hit.getLocation();
            AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
            if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living && spell != null) {
                living.invulnerableTime = 0;
                DamageSources.applyDamage(living, damage, SpellUtil.damageSource(spell, this, getOwner()));
            }
            GemFx.shatter(level, at, getGem(), .7f, 6);
            GemFx.sound(level, at, SoundEvents.AMETHYST_BLOCK_HIT, 1f, 1.4f);
        }
        discard();
    }

    @Override
    protected void onExpire() {
        GemFx.glitter(level(), position(), getGem(), 8, .2);
    }

    @Override
    protected void clientTick() {
        Vec3 p = position().add(0, getBbHeight() * .5, 0);
        level().addParticle(getGem().glint, p.x, p.y, p.z, 0, 0, 0);
    }
}
