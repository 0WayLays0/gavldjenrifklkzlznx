package com.waylays.arcanes.entity.wall;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.wall.WallFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * A stretch of brick wall (stone, mossy and cracked stone bricks only) bursting out of the ground. What it does depends
 * on its kind:
 * <ul>
 * <li>STAND: it stands there, stopping enemy shots and keeping enemies from walking through.</li>
 * <li>PILLAR: the same, one brick wide (the Brick Cage's bars).</li>
 * <li>SLAM: once risen it slides forward and crushes what is caught at the end (the Crushing Walls).</li>
 * <li>PUSH: it slides forward, sweeping the enemies before it (the Rampart Charge).</li>
 * <li>BURST: it rises, then blows apart forward in a spray of bricks (the Wall Burst).</li>
 * </ul>
 * It never moves on its own: both sides work out where it is from its age, so it slides smoothly.
 */
public class WallPiece extends SpellEffectEntity {
    public enum Kind { STAND, PILLAR, SLAM, PUSH, BURST }

    public static final int RISE = 6;
    public static final int SINK = 10;
    public static final float THICK = .9f;
    /** Ticks after it has risen before a burst wall blows apart, and how far the bricks fly. */
    public static final int BURST_DELAY = 10;
    public static final float BURST_RANGE = 7;

    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(WallPiece.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_WIDTH = SynchedEntityData.defineId(WallPiece.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEIGHT = SynchedEntityData.defineId(WallPiece.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(WallPiece.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_TRAVEL = SynchedEntityData.defineId(WallPiece.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_TRAVEL_TICKS = SynchedEntityData.defineId(WallPiece.class, EntityDataSerializers.INT);

    private final IntOpenHashSet hit = new IntOpenHashSet();
    @Nullable
    private String spellId;

    public WallPiece(EntityType<? extends WallPiece> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * A wall {@code width} wide and {@code height} high, its foot centred on {@code at}, facing {@code yaw} (its
     * face looks that way; SLAM and PUSH walls slide {@code travel} blocks that way over {@code travelTicks}).
     */
    public static WallPiece raise(Level level, LivingEntity caster, AbstractSpell spell, Kind kind, Vec3 at, float yaw,
                                  float width, float height, int life, float travel, int travelTicks, float damage) {
        WallPiece wall = new WallPiece(ModEntities.WALL_PIECE.get(), level);
        wall.setOwner(caster);
        wall.spellId = spell.getSpellId();
        wall.damage = damage;
        wall.moveTo(at.x, at.y, at.z, yaw, 0);
        wall.entityData.set(DATA_KIND, kind.ordinal());
        wall.entityData.set(DATA_WIDTH, width);
        wall.entityData.set(DATA_HEIGHT, height);
        wall.entityData.set(DATA_LIFE, life);
        wall.entityData.set(DATA_TRAVEL, travel);
        wall.entityData.set(DATA_TRAVEL_TICKS, Math.max(1, travelTicks));
        level.addFreshEntity(wall);
        return wall;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, 0);
        builder.define(DATA_WIDTH, 3f);
        builder.define(DATA_HEIGHT, 3f);
        builder.define(DATA_LIFE, 100);
        builder.define(DATA_TRAVEL, 0f);
        builder.define(DATA_TRAVEL_TICKS, 1);
    }

    public Kind getKind() {
        Kind[] all = Kind.values();
        return all[Mth.clamp(this.entityData.get(DATA_KIND), 0, all.length - 1)];
    }

    public float getWidth() {
        return this.entityData.get(DATA_WIDTH);
    }

    public float getHeight() {
        return this.entityData.get(DATA_HEIGHT);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    public Vec3 forward() {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
    }

    /** How far it has slid (0 to 1 of its travel), easing in and out. */
    public float travelled(float partialTicks) {
        float t = Mth.clamp((tickCount + partialTicks - RISE) / this.entityData.get(DATA_TRAVEL_TICKS), 0, 1);
        return getKind() == Kind.SLAM ? t * t : t * t * (3 - 2 * t);
    }

    /** Where its foot is now, from where it rose. */
    public Vec3 offset(float partialTicks) {
        return forward().scale(this.entityData.get(DATA_TRAVEL) * travelled(partialTicks));
    }

    /** How far out of the ground (0 to 1), with a small overshoot as it bursts out; sinking at the end. */
    public float risen(float partialTicks) {
        float age = tickCount + partialTicks;
        float up = Mth.clamp(age / RISE, 0, 1);
        float down = Mth.clamp((age - getLife()) / SINK, 0, 1);
        return up * (1 + .12f * Mth.sin(up * Mth.PI)) * (1 - down * down);
    }

    private int arrival() {
        return RISE + this.entityData.get(DATA_TRAVEL_TICKS);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        ServerLevel level = (ServerLevel) level();
        if (tickCount == 1) {
            WallFx.rise(level, position(), 1.2f);
            WallFx.dust(level, position().add(0, .3, 0), 18, getWidth() * .3);
        }
        if (tickCount > getLife() + SINK) {
            discard();
            return;
        }
        if (tickCount == getLife()) {
            WallFx.dust(level, position().add(offset(0)).add(0, .5, 0), 14, getWidth() * .3);
        }
        if (tickCount < RISE || tickCount >= getLife()) {
            return;
        }
        switch (getKind()) {
            case STAND, PILLAR -> hold(level);
            case SLAM, PUSH -> slide(level);
            case BURST -> {
                hold(level);
                if (tickCount == RISE + BURST_DELAY) {
                    burst(level);
                }
            }
        }
    }

    @Nullable
    private AbstractSpell spell() {
        return spellId == null ? null : SpellRegistry.getSpell(spellId);
    }

    private void hurt(LivingEntity e, float amount) {
        AbstractSpell spell = spell();
        if (spell != null) {
            e.invulnerableTime = 0;
            DamageSources.applyDamage(e, amount, SpellUtil.damageSource(spell, this, getOwner()));
        }
    }

    private AABB box(Vec3 c, double reach) {
        double r = getWidth() * .5 + reach;
        return new AABB(c.x - r, c.y - 1, c.z - r, c.x + r, c.y + getHeight() + 1, c.z + r);
    }

    /** Where {@code e} is from the wall's middle: along its facing, then across it. */
    private double[] local(Entity e, Vec3 c) {
        Vec3 f = forward();
        Vec3 d = e.position().subtract(c);
        return new double[]{d.dot(f), d.x * -f.z + d.z * f.x};
    }

    private boolean inside(Entity e, Vec3 c, double extra) {
        double[] l = local(e, c);
        return Math.abs(l[0]) < THICK * .5 + e.getBbWidth() * .5 + extra && Math.abs(l[1]) < getWidth() * .5 + e.getBbWidth() * .5
                && e.getY() > c.y - 1.5 && e.getY() < c.y + getHeight();
    }

    /** Whether a shot is in the wall or goes through it this tick (fast arrows cross it between two ticks). */
    private boolean crosses(Projectile shot, Vec3 c) {
        if (shot.getY() < c.y - 1 || shot.getY() > c.y + getHeight() + .3) {
            return false;
        }
        double[] now = local(shot, c);
        Vec3 f = forward();
        Vec3 motion = shot.getDeltaMovement();
        double along = now[0] + motion.dot(f);
        double across = now[1] + motion.x * -f.z + motion.z * f.x;
        double reach = THICK * .5 + .35;
        boolean through = Math.abs(now[0]) < reach || Math.abs(along) < reach || Math.signum(now[0]) != Math.signum(along);
        return through && Math.min(Math.abs(now[1]), Math.abs(across)) < getWidth() * .5 + .3;
    }

    /** A standing wall stops enemy shots and keeps enemies out of it, pushed back to the side they came from. */
    private void hold(ServerLevel level) {
        Vec3 c = position().add(offset(0));
        Entity owner = getOwner();
        for (Projectile shot : level.getEntitiesOfClass(Projectile.class, box(c, 5))) {
            if (shot.getOwner() != null && SpellUtil.isAlly(owner, shot.getOwner())) {
                continue;
            }
            if (crosses(shot, c)) {
                WallFx.dust(level, shot.position(), 9, .2);
                WallFx.sound(level, shot.position(), net.minecraft.sounds.SoundEvents.STONE_HIT, 1f, .8f);
                shot.discard();
            }
        }
        Vec3 f = forward();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box(c, 1))) {
            if (!e.isAlive() || SpellUtil.isAlly(owner, e) || !inside(e, c, 0)) {
                continue;
            }
            double side = Math.signum(local(e, c)[0]);
            Vec3 out = f.scale(side == 0 ? 1 : side);
            e.setDeltaMovement(out.scale(.35).add(0, Math.max(0, e.getDeltaMovement().y), 0));
            e.hurtMarked = true;
        }
    }

    /** A sliding wall sweeps the enemies before it, hurting each once; a crushing wall hurts them again as it arrives. */
    private void slide(ServerLevel level) {
        Vec3 c = position().add(offset(0));
        Vec3 f = forward();
        Entity owner = getOwner();
        boolean moving = tickCount < arrival();
        float speed = this.entityData.get(DATA_TRAVEL) / this.entityData.get(DATA_TRAVEL_TICKS);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box(c, 2))) {
            if (!e.isAlive() || SpellUtil.isAlly(owner, e)) {
                continue;
            }
            double[] l = local(e, c);
            boolean ahead = l[0] > -THICK * .5 && l[0] < THICK * .5 + e.getBbWidth() * .5 + .5
                    && Math.abs(l[1]) < getWidth() * .5 + e.getBbWidth() * .5 && e.getY() > c.y - 1.5 && e.getY() < c.y + getHeight();
            if (!ahead) {
                continue;
            }
            if (moving) {
                double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
                e.setDeltaMovement(f.scale(Math.max(.3, speed * 1.3) * Math.max(.4, resist)).add(0, .1, 0));
                e.hurtMarked = true;
                if (hit.add(e.getId())) {
                    hurt(e, getKind() == Kind.SLAM ? damage * .4f : damage);
                }
            }
            if (getKind() == Kind.SLAM && tickCount == arrival()) {
                hurt(e, damage);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
            }
        }
        if (moving && tickCount % 3 == 0) {
            WallFx.dust(level, c.add(0, .2, 0), 6, getWidth() * .3);
            WallFx.sound(level, c, net.minecraft.sounds.SoundEvents.GRINDSTONE_USE, .5f, .5f);
        }
        if (tickCount == arrival()) {
            WallFx.slam(level, c, 1.4f);
            WallFx.dust(level, c.add(f.scale(.6)).add(0, 1, 0), 30, getWidth() * .4);
            SpellUtil.cameraShake(level, c, 8, 12);
        }
    }

    /** The wall blows apart forward: bricks fly in a cone, hurting and throwing back the enemies there. */
    private void burst(ServerLevel level) {
        Vec3 c = position().add(0, getHeight() * .5, 0);
        Vec3 f = forward();
        Entity owner = getOwner();
        for (LivingEntity e : SpellUtil.enemiesAround(level, c, BURST_RANGE, owner, x -> {
            Vec3 d = x.position().add(0, x.getBbHeight() * .5, 0).subtract(c);
            double along = d.dot(f);
            return along > 0 && along < BURST_RANGE && Math.abs(d.x * -f.z + d.z * f.x) < getWidth() * .5 + along * .55;
        })) {
            double along = e.position().subtract(c).dot(f);
            hurt(e, damage * (float) (1 - .4 * along / BURST_RANGE));
            double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
            e.setDeltaMovement(e.getDeltaMovement().add(f.scale(1.1 * resist)).add(0, .4 * resist, 0));
            e.hurtMarked = true;
        }
        for (int i = 0; i < 6; i++) {
            WallFx.dust(level, c.add(f.scale(i * 1.1)), 16, .5 + i * .25);
        }
        WallFx.slam(level, c, 1.6f);
        level.playSound(null, c.x, c.y, c.z, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, .8f);
        SpellUtil.cameraShake(level, c, 10, 14);
        this.entityData.set(DATA_LIFE, tickCount + 1);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
