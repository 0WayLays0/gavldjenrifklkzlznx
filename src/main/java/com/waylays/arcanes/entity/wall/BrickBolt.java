package com.waylays.arcanes.entity.wall;

import com.waylays.arcanes.entity.SpellProjectile;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.wall.WallFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.Direction;
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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * A flying brick of Wall Magic, its mortar glowing:
 * <ul>
 * <li>BOLT: torn out of the ground beside the caster, it hovers a moment, then is hurled at the enemy (aimed when it
 * leaves); it strikes hard and bursts.</li>
 * <li>MORTAR: a lump of bricks lobbed high; a sigil burns where it will land; it bursts there, hurting all around.</li>
 * <li>FALLING: a brick dropped from the Brick Rain's cloud, bursting where it lands.</li>
 * <li>BOULDER: a great block of masonry hurled from the sky by the Siege, blazing; a sigil burns where it will land;
 * it smashes everything around, throwing it up.</li>
 * </ul>
 */
public class BrickBolt extends SpellProjectile {
    public enum Kind { BOLT, MORTAR, FALLING, BOULDER }

    public static final float MORTAR_RADIUS = 3.5f;
    public static final float FALLING_RADIUS = 1.6f;
    public static final float BOULDER_RADIUS = 3.4f;
    public static final double BOULDER_GRAVITY = .07;
    public static final float BOLT_SPEED = 1.7f;

    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(BrickBolt.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HOVER = SynchedEntityData.defineId(BrickBolt.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_TX = SynchedEntityData.defineId(BrickBolt.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TY = SynchedEntityData.defineId(BrickBolt.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TZ = SynchedEntityData.defineId(BrickBolt.class, EntityDataSerializers.FLOAT);

    @Nullable
    private String spellId;
    private int targetId = -1;

    public BrickBolt(EntityType<? extends BrickBolt> entityType, Level level) {
        super(entityType, level);
    }

    /** A brick flying at once with {@code velocity}; {@code target} is where it is meant to land (shown for a mortar). */
    public static BrickBolt shoot(Level level, LivingEntity owner, AbstractSpell spell, Kind kind, Vec3 from, Vec3 velocity, Vec3 target, float damage) {
        BrickBolt bolt = create(level, owner, spell, kind, from, target, damage);
        bolt.setDeltaMovement(velocity);
        level.addFreshEntity(bolt);
        return bolt;
    }

    /** A bolt rising out of the ground at {@code from}, hovering {@code hover} ticks, then hurled at {@code target} (or {@code aim}). */
    public static BrickBolt summon(Level level, LivingEntity owner, AbstractSpell spell, Vec3 from, int hover, @Nullable LivingEntity target, Vec3 aim, float damage) {
        BrickBolt bolt = create(level, owner, spell, Kind.BOLT, from, aim, damage);
        bolt.entityData.set(DATA_HOVER, hover);
        bolt.targetId = target != null ? target.getId() : -1;
        level.addFreshEntity(bolt);
        return bolt;
    }

    private static BrickBolt create(Level level, LivingEntity owner, AbstractSpell spell, Kind kind, Vec3 from, Vec3 target, float damage) {
        BrickBolt bolt = new BrickBolt(ModEntities.BRICK_BOLT.get(), level);
        bolt.setOwner(owner);
        bolt.spellId = spell.getSpellId();
        bolt.entityData.set(DATA_KIND, kind.ordinal());
        bolt.entityData.set(DATA_TX, (float) target.x);
        bolt.entityData.set(DATA_TY, (float) target.y);
        bolt.entityData.set(DATA_TZ, (float) target.z);
        bolt.setDamage(damage);
        bolt.setPos(from.x, from.y - bolt.getBbHeight() * .5, from.z);
        return bolt;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, 0);
        builder.define(DATA_HOVER, 0);
        builder.define(DATA_TX, 0f);
        builder.define(DATA_TY, 0f);
        builder.define(DATA_TZ, 0f);
    }

    public Kind getKind() {
        Kind[] all = Kind.values();
        return all[Mth.clamp(this.entityData.get(DATA_KIND), 0, all.length - 1)];
    }

    public int getHover() {
        return this.entityData.get(DATA_HOVER);
    }

    /** Where it is meant to land. */
    public Vec3 getTarget() {
        return new Vec3(this.entityData.get(DATA_TX), this.entityData.get(DATA_TY), this.entityData.get(DATA_TZ));
    }

    @Override
    protected double gravity() {
        return switch (getKind()) {
            case BOLT -> 0;
            case MORTAR -> .05;
            case FALLING -> .04;
            case BOULDER -> BOULDER_GRAVITY;
        };
    }

    @Override
    protected int lifetime() {
        return getKind() == Kind.BOLT ? getHover() + 50 : 140;
    }

    @Override
    public void tick() {
        if (getKind() == Kind.BOLT && tickCount < getHover()) {
            // Hovering where it rose, waiting its turn
            setDeltaMovement(Vec3.ZERO);
            if (!level().isClientSide && tickCount == 1) {
                WallFx.dust(level(), position(), 8, .3);
                WallFx.sound(level(), position(), SoundEvents.STONE_BREAK, .8f, 1.2f);
            }
            return;
        }
        if (getKind() == Kind.BOLT && tickCount == getHover() && !level().isClientSide) {
            launch();
        }
        super.tick();
    }

    private void launch() {
        Vec3 from = position().add(0, getBbHeight() * .5, 0);
        Entity target = targetId >= 0 ? level().getEntity(targetId) : null;
        Vec3 aim = target instanceof LivingEntity living && living.isAlive() ? living.getBoundingBox().getCenter() : getTarget();
        Vec3 dir = aim.subtract(from);
        dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : dir.normalize();
        setDeltaMovement(dir.scale(BOLT_SPEED));
        hasImpulse = true;
        WallFx.sound(level(), from, SoundEvents.PLAYER_ATTACK_SWEEP, .8f, .6f);
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
            LivingEntity struck = hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living ? living : null;
            if (getKind() == Kind.BOLT) {
                if (struck != null && spell != null) {
                    struck.invulnerableTime = 0;
                    DamageSources.applyDamage(struck, damage, SpellUtil.damageSource(spell, this, getOwner()));
                    Vec3 push = getDeltaMovement().normalize();
                    double resist = 1 - Mth.clamp(struck.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
                    struck.setDeltaMovement(struck.getDeltaMovement().add(push.scale(.8 * resist)).add(0, .25 * resist, 0));
                    struck.hurtMarked = true;
                }
                WallFx.impact(level, at, 1, 4);
                WallFx.sound(level, at, SoundEvents.STONE_BREAK, 1.3f, .7f);
            } else {
                float radius = switch (getKind()) {
                    case MORTAR -> MORTAR_RADIUS;
                    case BOULDER -> BOULDER_RADIUS;
                    default -> FALLING_RADIUS;
                };
                if (spell != null) {
                    for (LivingEntity e : SpellUtil.enemiesAround(level, at, radius, getOwner(), x -> true)) {
                        hurtInBlast(e, at, radius, spell);
                    }
                    // Whatever it struck square is hurt, wherever its middle is
                    if (struck != null && !SpellUtil.isAlly(getOwner(), struck) && struck.getBoundingBox().getCenter().distanceTo(at) > radius) {
                        hurtInBlast(struck, at, radius, spell);
                    }
                }
                boolean big = getKind() == Kind.MORTAR || getKind() == Kind.BOULDER;
                WallFx.impact(level, at, big ? radius : 1.2f, getKind() == Kind.BOULDER ? 16 : big ? 12 : 3);
                WallFx.slam(level, at, big ? 1.8f : .8f);
                if (big) {
                    boolean boulder = getKind() == Kind.BOULDER;
                    level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.PLAYERS, boulder ? 2f : 1.2f, boulder ? .6f : .8f);
                    SpellUtil.cameraShake(level, at, boulder ? 14 : 10, boulder ? 20 : 14);
                    if (boulder) {
                        WallFx.embers(level, at.add(0, .5, 0), 24, 1.4);
                    }
                }
            }
        }
        discard();
    }

    private void hurtInBlast(LivingEntity e, Vec3 at, float radius, AbstractSpell spell) {
        float falloff = (float) Mth.clamp(1 - .5 * e.position().distanceTo(at) / radius, .5, 1);
        e.invulnerableTime = 0;
        DamageSources.applyDamage(e, damage * falloff, SpellUtil.damageSource(spell, this, getOwner()));
        if (getKind() == Kind.MORTAR || getKind() == Kind.BOULDER) {
            boolean boulder = getKind() == Kind.BOULDER;
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, boulder ? 60 : 50, boulder ? 2 : 1));
            Vec3 away = e.position().subtract(at).multiply(1, 0, 1);
            away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize();
            double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
            e.setDeltaMovement(e.getDeltaMovement().add(away.scale((boulder ? .9 : .7) * resist)).add(0, (boulder ? .7 : .4) * Math.max(.4, resist), 0));
            e.hurtMarked = true;
        }
    }

    @Override
    protected void onExpire() {
        if (getKind() != Kind.BOLT) {
            onImpact(new BlockHitResult(position(), Direction.UP, blockPosition(), false));
        }
    }

    @Override
    protected void clientTick() {
        if (getKind() == Kind.BOLT && tickCount < getHover()) {
            return;
        }
        Vec3 p = position().add(0, getBbHeight() * .5, 0);
        level().addParticle(WallFx.EMBER_DUST, p.x, p.y, p.z, 0, 0, 0);
        if (random.nextInt(3) == 0) {
            level().addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, p.x, p.y, p.z, 0, 0, 0);
        }
    }
}
