package com.waylays.arcanes.entity.wall;

import com.waylays.arcanes.entity.SpellProjectile;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.wall.WallFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * A flying brick of Wall Magic:
 * <ul>
 * <li>BOLT: one stone brick hurled straight; it hurts what it hits and shatters.</li>
 * <li>MORTAR: a lump of bricks lobbed high; it bursts where it lands, hurting everything around.</li>
 * <li>FALLING: a brick dropped from the sky (the Brick Rain), bursting where it lands.</li>
 * </ul>
 */
public class BrickBolt extends SpellProjectile {
    public enum Kind { BOLT, MORTAR, FALLING }

    public static final float MORTAR_RADIUS = 3;
    public static final float FALLING_RADIUS = 1.4f;

    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(BrickBolt.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;

    public BrickBolt(EntityType<? extends BrickBolt> entityType, Level level) {
        super(entityType, level);
    }

    public static BrickBolt shoot(Level level, LivingEntity owner, AbstractSpell spell, Kind kind, Vec3 from, Vec3 velocity, float damage) {
        BrickBolt bolt = new BrickBolt(ModEntities.BRICK_BOLT.get(), level);
        bolt.setOwner(owner);
        bolt.spellId = spell.getSpellId();
        bolt.entityData.set(DATA_KIND, kind.ordinal());
        bolt.setDamage(damage);
        bolt.setPos(from.x, from.y - bolt.getBbHeight() * .5, from.z);
        bolt.setDeltaMovement(velocity);
        level.addFreshEntity(bolt);
        return bolt;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, 0);
    }

    public Kind getKind() {
        Kind[] all = Kind.values();
        return all[Mth.clamp(this.entityData.get(DATA_KIND), 0, all.length - 1)];
    }

    @Override
    protected double gravity() {
        return switch (getKind()) {
            case BOLT -> 0;
            case MORTAR -> .05;
            case FALLING -> .04;
        };
    }

    @Override
    protected int lifetime() {
        return getKind() == Kind.BOLT ? 50 : 140;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !SpellUtil.isAlly(getOwner(), target);
    }

    @Override
    protected void onImpact(HitResult hit) {
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (level() instanceof ServerLevel level) {
            Vec3 at = hit.getLocation();
            if (getKind() == Kind.BOLT) {
                if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target && spell != null) {
                    DamageSources.applyDamage(target, damage, SpellUtil.damageSource(spell, this, getOwner()));
                    Vec3 push = getDeltaMovement().normalize();
                    double resist = 1 - Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
                    target.setDeltaMovement(target.getDeltaMovement().add(push.scale(.5 * resist)).add(0, .15 * resist, 0));
                    target.hurtMarked = true;
                }
                WallFx.dust(level, at, 18, .25);
                WallFx.sound(level, at, net.minecraft.sounds.SoundEvents.STONE_BREAK, 1.1f, .8f);
            } else {
                float radius = getKind() == Kind.MORTAR ? MORTAR_RADIUS : FALLING_RADIUS;
                if (spell != null) {
                    for (LivingEntity e : SpellUtil.enemiesAround(level, at, radius, getOwner(), x -> true)) {
                        float falloff = (float) (1 - .5 * e.position().distanceTo(at) / radius);
                        e.invulnerableTime = 0;
                        DamageSources.applyDamage(e, damage * falloff, SpellUtil.damageSource(spell, this, getOwner()));
                        if (getKind() == Kind.MORTAR) {
                            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                            Vec3 away = e.position().subtract(at).multiply(1, 0, 1);
                            away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize();
                            e.setDeltaMovement(e.getDeltaMovement().add(away.scale(.6)).add(0, .35, 0));
                            e.hurtMarked = true;
                        }
                    }
                }
                WallFx.dust(level, at.add(0, .3, 0), getKind() == Kind.MORTAR ? 45 : 14, radius * .4);
                WallFx.slam(level, at, getKind() == Kind.MORTAR ? 1.6f : .8f);
                if (getKind() == Kind.MORTAR) {
                    SpellUtil.cameraShake(level, at, 8, 12);
                }
            }
        }
        discard();
    }

    @Override
    protected void onExpire() {
        if (getKind() != Kind.BOLT) {
            onImpact(new net.minecraft.world.phys.BlockHitResult(position(), net.minecraft.core.Direction.UP, blockPosition(), false));
        }
    }

    @Override
    protected void clientTick() {
        if (random.nextInt(3) == 0) {
            Vec3 p = position().add(0, getBbHeight() * .5, 0);
            level().addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, p.x, p.y, p.z, 0, 0, 0);
        }
    }
}
