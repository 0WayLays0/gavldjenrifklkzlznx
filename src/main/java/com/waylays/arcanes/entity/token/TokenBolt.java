package com.waylays.arcanes.entity.token;

import com.waylays.arcanes.entity.SpellProjectile;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.token.TokenFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;

/**
 * A soul token flung through the air, flipping end over end. Thrown (it stays where it lands, for the caster to
 * teleport to), ricocheting from enemy to enemy, falling from the sky (it bursts where it lands) or striking once.
 */
public class TokenBolt extends SpellProjectile {
    public enum Mode { THROW, RICOCHET, FALL, STRIKE }

    /** How far a ricochet looks for its next enemy. */
    public static final double BOUNCE_RANGE = 8;

    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(TokenBolt.class, EntityDataSerializers.FLOAT);

    private final IntOpenHashSet struck = new IntOpenHashSet();
    @Nullable
    private String spellId;
    private Mode mode = Mode.STRIKE;
    private int bounces;
    private float splash;
    private double fall;
    private int markerLife = 200;

    public TokenBolt(EntityType<? extends TokenBolt> entityType, Level level) {
        super(entityType, level);
    }

    public static TokenBolt shoot(Level level, @Nullable LivingEntity owner, AbstractSpell spell, Mode mode, Vec3 from, Vec3 direction, float speed, float damage) {
        TokenBolt bolt = new TokenBolt(ModEntities.TOKEN_BOLT.get(), level);
        bolt.setOwner(owner);
        bolt.spellId = spell.getSpellId();
        bolt.mode = mode;
        bolt.setDamage(damage);
        bolt.setPos(from.x, from.y - bolt.getBbHeight() * .5, from.z);
        bolt.shoot(direction, speed);
        level.addFreshEntity(bolt);
        return bolt;
    }

    public TokenBolt withBounces(int count) {
        this.bounces = count;
        return this;
    }

    public TokenBolt withSplash(float radius) {
        this.splash = radius;
        return this;
    }

    public TokenBolt withGravity(double gravity) {
        this.fall = gravity;
        return this;
    }

    public TokenBolt withSize(float size) {
        this.entityData.set(DATA_SIZE, size);
        return this;
    }

    /** For a thrown token: how long it stays where it lands. */
    public TokenBolt withMarkerLife(int ticks) {
        this.markerLife = ticks;
        return this;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SIZE, 1f);
    }

    public float getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    @Override
    protected double gravity() {
        return fall;
    }

    @Override
    protected int lifetime() {
        return mode == Mode.THROW ? 40 : 80;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        // Hits are the server's business: the client never stops a token at a creature (a ricochet goes on)
        return !level().isClientSide && super.canHitEntity(target) && !struck.contains(target.getId())
                && !SpellUtil.isAlly(getOwner(), target) && !(target instanceof TokenBolt);
    }

    @Override
    protected void onImpact(HitResult hit) {
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (spell == null || !(level() instanceof ServerLevel level)) {
            discard();
            return;
        }
        Vec3 at = hit.getLocation();
        Entity owner = getOwner();
        LivingEntity victim = hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living ? living : null;
        if (victim != null) {
            struck.add(victim.getId());
        }
        switch (mode) {
            case THROW -> {
                Vec3 rest;
                if (victim != null) {
                    hurt(victim, damage, spell, owner);
                    rest = SpellUtil.groundAt(level, victim.position(), 3, 2);
                } else if (hit instanceof BlockHitResult blockHit) {
                    // Stuck in the wall or lying on the floor, on the caster's side of it
                    rest = at.add(Vec3.atLowerCornerOf(blockHit.getDirection().getNormal()).scale(.3));
                } else {
                    rest = at;
                }
                if (owner instanceof LivingEntity caster) {
                    TokenMarker.place(level, caster, rest, markerLife);
                }
                TokenFx.impact(level, at, .8f);
            }
            case RICOCHET -> {
                if (victim != null) {
                    hurt(victim, damage, spell, owner);
                    TokenFx.impact(level, at, .6f);
                    if (bounces-- > 0) {
                        LivingEntity next = SpellUtil.enemiesAround(level, victim.position(), BOUNCE_RANGE, owner,
                                        e -> !struck.contains(e.getId()) && victim.hasLineOfSight(e)).stream()
                                .min(Comparator.comparingDouble(e -> e.distanceToSqr(victim))).orElse(null);
                        if (next != null) {
                            // Off to the next one, from where it struck
                            Vec3 from = TokenFx.chest(victim);
                            setPos(from.x, from.y - getBbHeight() * .5, from.z);
                            shoot(TokenFx.chest(next).subtract(from), (float) getDeltaMovement().length());
                            TokenFx.clink(level, from, .6f);
                            return;
                        }
                    }
                } else {
                    TokenFx.impact(level, at, .5f);
                }
            }
            case FALL -> {
                for (LivingEntity e : SpellUtil.enemiesAround(level, at, Math.max(splash, .8f), owner, x -> true)) {
                    hurt(e, damage, spell, owner);
                }
                TokenFx.impact(level, at, Math.max(splash, .6f));
            }
            case STRIKE -> {
                if (victim != null) {
                    hurt(victim, damage, spell, owner);
                }
                TokenFx.impact(level, at, .7f);
            }
        }
        discard();
    }

    private void hurt(LivingEntity target, float amount, AbstractSpell spell, @Nullable Entity owner) {
        // Tokens land in quick succession: none of them is lost to the victim's grace period
        target.invulnerableTime = 0;
        DamageSources.applyDamage(target, amount, SpellUtil.damageSource(spell, this, owner));
    }

    @Override
    protected void onExpire() {
        // A thrown token that met nothing drops to the ground where it ran out
        if (mode == Mode.THROW && level() instanceof ServerLevel level && getOwner() instanceof LivingEntity caster) {
            TokenMarker.place(level, caster, SpellUtil.groundAt(level, position(), 8, 2), markerLife);
        }
    }

    @Override
    protected void clientTick() {
        Vec3 p = position().add(0, getBbHeight() * .5, 0);
        level().addParticle(TokenFx.DUST, p.x, p.y, p.z, 0, 0, 0);
        if (random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.GLOW, p.x, p.y, p.z, 0, 0, 0);
        }
    }
}
