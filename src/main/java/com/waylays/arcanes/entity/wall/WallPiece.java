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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * A wall of Wall Magic that moves or blows apart (the solid, standing ones are {@link Rampart}s). It builds itself
 * brick by brick, each block flying up out of the ground in a ripple from the middle, its mortar glowing; then:
 * <ul>
 * <li>SLAM: it slides forward, faster and faster, and slams into its twin (the Crushing Walls): what is caught
 * between is crushed, and both walls burst into flying bricks.</li>
 * <li>PUSH: it charges forward, ploughing the ground and sweeping the enemies before it, then crashes and blows apart
 * forward (the Rampart Charge).</li>
 * <li>BURST: it stands while its mortar glows hotter and hotter, shaking, then blows apart forward in a cone of
 * bricks (the Wall Burst).</li>
 * <li>HERD: it slides forward like a slam, sweeping the enemies before it without hurting them, and bursts when it
 * arrives (the walls of the Closing Keep, which does the crushing).</li>
 * <li>TOPPLE: it stands, shaking, its mortar heating, then topples forward flat onto everything before it, as long
 * as it is high, and bursts where it lands (the Great Rampart).</li>
 * </ul>
 * It never moves on its own: both sides work out where it is from its age, so it slides smoothly.
 */
public class WallPiece extends SpellEffectEntity {
    public enum Kind { SLAM, PUSH, BURST, HERD, TOPPLE }

    /** Ticks a block takes to fly into place; after the wall is built, ticks before a burst wall blows apart. */
    public static final int CELL_RISE = 5;
    public static final int BURST_DELAY = 14;
    public static final float BURST_RANGE = 8;
    /** Ticks the flying bricks last after it blows apart. */
    public static final int SHATTER_TICKS = 26;
    public static final float THICK = 1;
    /** A toppling wall: ticks it sways once built, then ticks it takes to fall. */
    public static final int TOPPLE_WAIT = 12;
    public static final int TOPPLE_FALL = 12;

    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(WallPiece.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_WIDTH = SynchedEntityData.defineId(WallPiece.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEIGHT = SynchedEntityData.defineId(WallPiece.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TRAVEL = SynchedEntityData.defineId(WallPiece.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_TRAVEL_TICKS = SynchedEntityData.defineId(WallPiece.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SHATTERED = SynchedEntityData.defineId(WallPiece.class, EntityDataSerializers.INT);

    private final IntOpenHashSet hit = new IntOpenHashSet();
    @Nullable
    private String spellId;

    public WallPiece(EntityType<? extends WallPiece> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * A wall {@code width} wide and {@code height} high, its foot centred on {@code at}, facing {@code yaw} (it slides
     * {@code travel} blocks that way over {@code travelTicks}, once built).
     */
    public static WallPiece raise(Level level, LivingEntity caster, AbstractSpell spell, Kind kind, Vec3 at, float yaw,
                                  float width, float height, float travel, int travelTicks, float damage) {
        WallPiece wall = new WallPiece(ModEntities.WALL_PIECE.get(), level);
        wall.setOwner(caster);
        wall.spellId = spell.getSpellId();
        wall.damage = damage;
        wall.moveTo(at.x, at.y, at.z, yaw, 0);
        wall.entityData.set(DATA_KIND, kind.ordinal());
        wall.entityData.set(DATA_WIDTH, width);
        wall.entityData.set(DATA_HEIGHT, height);
        wall.entityData.set(DATA_TRAVEL, travel);
        wall.entityData.set(DATA_TRAVEL_TICKS, Math.max(1, travelTicks));
        level.addFreshEntity(wall);
        WallFx.rise(level, at, 1.3f);
        return wall;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, 0);
        builder.define(DATA_WIDTH, 3f);
        builder.define(DATA_HEIGHT, 3f);
        builder.define(DATA_TRAVEL, 0f);
        builder.define(DATA_TRAVEL_TICKS, 1);
        builder.define(DATA_SHATTERED, -1);
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

    public int columns() {
        return Math.max(1, Math.round(getWidth()));
    }

    public int rows() {
        return Mth.ceil(getHeight());
    }

    /** When block (column, row) starts to fly into place: a ripple out from the middle, the rows one on another. */
    public int cellDelay(int column, int row) {
        return (int) (Math.abs(column - (columns() - 1) * .5f) * 1.5f) + row * 2;
    }

    /** Ticks until every block is in place. */
    public int built() {
        return cellDelay(0, rows() - 1) + CELL_RISE;
    }

    /** The tick it blew apart, or -1. */
    public int getShattered() {
        return this.entityData.get(DATA_SHATTERED);
    }

    public Vec3 forward() {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
    }

    /** How far it has slid (0 to 1 of its travel): a slam speeds up to the end, a charge eases in and out. */
    public float travelled(float partialTicks) {
        if (getKind() == Kind.BURST || getKind() == Kind.TOPPLE) {
            return 0;
        }
        float t = Mth.clamp((tickCount + partialTicks - built()) / this.entityData.get(DATA_TRAVEL_TICKS), 0, 1);
        return getKind() == Kind.PUSH ? t * t * (3 - 2 * t) : t * t;
    }

    /** How far a toppling wall has fallen (radians, 0 standing to a right angle lying flat), speeding up as it goes. */
    public float tilt(float partialTicks) {
        if (getKind() != Kind.TOPPLE) {
            return 0;
        }
        float t = Mth.clamp((tickCount + partialTicks - built() - TOPPLE_WAIT) / TOPPLE_FALL, 0, 1);
        return t * t * Mth.HALF_PI;
    }

    /** Where its foot is now, from where it rose. */
    public Vec3 offset(float partialTicks) {
        return forward().scale(this.entityData.get(DATA_TRAVEL) * travelled(partialTicks));
    }

    /** How hot its mortar glows (0 to 1): a burst wall heats up before it blows. */
    public float heat(float partialTicks) {
        float age = tickCount + partialTicks;
        if (getKind() == Kind.BURST) {
            return .45f + .55f * Mth.clamp((age - built()) / BURST_DELAY, 0, 1);
        }
        if (getKind() == Kind.TOPPLE) {
            return .45f + .55f * Mth.clamp((age - built()) / (TOPPLE_WAIT + TOPPLE_FALL), 0, 1);
        }
        return .5f + .3f * travelled(partialTicks);
    }

    private int arrival() {
        return built() + this.entityData.get(DATA_TRAVEL_TICKS);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        int shattered = getShattered();
        if (shattered >= 0) {
            if (tickCount > shattered + SHATTER_TICKS) {
                discard();
            }
            return;
        }
        if (tickCount > 260) {
            discard();
            return;
        }
        if (tickCount < built()) {
            if (tickCount % 3 == 0) {
                WallFx.sound(level, position(), SoundEvents.STONE_PLACE, 1f, .6f + random.nextFloat() * .3f);
            }
            return;
        }
        switch (getKind()) {
            case SLAM, PUSH, HERD -> slide(level);
            case TOPPLE -> topple(level);
            case BURST -> {
                if (tickCount % 4 == 0) {
                    WallFx.embers(level, position().add(0, getHeight() * .5, 0), 4, getWidth() * .3);
                    WallFx.sound(level, position(), SoundEvents.STONE_HIT, .8f, .5f + .5f * heat(0));
                }
                if (tickCount == built() + BURST_DELAY) {
                    blow(level, BURST_RANGE, damage);
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

    /** Where {@code e} is from the wall's middle: along its facing, then across it. */
    private double[] local(Entity e, Vec3 c) {
        Vec3 f = forward();
        Vec3 d = e.position().subtract(c);
        return new double[]{d.dot(f), d.x * -f.z + d.z * f.x};
    }

    /** A sliding wall sweeps the enemies before it, hurting each once; then it slams or crashes. */
    private void slide(ServerLevel level) {
        Vec3 c = position().add(offset(0));
        Vec3 f = forward();
        Entity owner = getOwner();
        boolean moving = tickCount < arrival();
        float speed = this.entityData.get(DATA_TRAVEL) / this.entityData.get(DATA_TRAVEL_TICKS);
        double r = getWidth() * .5 + 2;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c.x - r, c.y - 1, c.z - r, c.x + r, c.y + getHeight() + 1, c.z + r))) {
            if (!e.isAlive() || SpellUtil.isAlly(owner, e)) {
                continue;
            }
            double[] l = local(e, c);
            boolean ahead = l[0] > -THICK * .5 && l[0] < THICK * .5 + e.getBbWidth() * .5 + .6
                    && Math.abs(l[1]) < getWidth() * .5 + e.getBbWidth() * .5 && e.getY() > c.y - 1.5 && e.getY() < c.y + getHeight();
            if (!ahead || !moving) {
                continue;
            }
            double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
            e.setDeltaMovement(f.scale(Math.max(.35, speed * 1.4) * Math.max(.4, resist)).add(0, .12, 0));
            e.hurtMarked = true;
            if (getKind() != Kind.HERD && hit.add(e.getId())) {
                hurt(e, getKind() == Kind.SLAM ? damage * .4f : damage * .6f);
            }
        }
        if (moving && tickCount % 2 == 0) {
            // It ploughs the ground: chips and embers thrown up at its foot
            Vec3 foot = c.add(f.scale(THICK * .6));
            WallFx.dust(level, foot.add(0, .1, 0), 8, getWidth() * .35);
            WallFx.embers(level, foot.add(0, .2, 0), 3, getWidth() * .3);
            WallFx.sound(level, c, SoundEvents.GRINDSTONE_USE, .7f, .4f);
        }
        if (tickCount == arrival()) {
            if (getKind() == Kind.SLAM) {
                crush(level, c);
            } else if (getKind() == Kind.HERD) {
                WallFx.slam(level, c, 1.2f);
                shatter(level);
            } else {
                blow(level, BURST_RANGE * .75f, damage);
            }
        }
    }

    /** A toppling wall sways and shudders, heating, then falls flat forward: what it lands on is crushed. */
    private void topple(ServerLevel level) {
        int fallen = built() + TOPPLE_WAIT + TOPPLE_FALL;
        if (tickCount < fallen) {
            if (tickCount % 3 == 0) {
                WallFx.embers(level, position().add(0, getHeight() * .5, 0), 5, getWidth() * .35);
                WallFx.sound(level, position(), SoundEvents.STONE_HIT, 1f, .4f + .4f * heat(0));
            }
            if (tickCount == built() + TOPPLE_WAIT) {
                WallFx.sound(level, position(), SoundEvents.GRINDSTONE_USE, 2f, .3f);
                WallFx.dust(level, position().add(0, .3, 0), 30, getWidth() * .4);
            }
            return;
        }
        if (tickCount != fallen) {
            return;
        }
        Vec3 f = forward();
        Vec3 foot = position().add(f.scale(THICK * .5));
        float reach = getHeight() + .8f;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(getWidth() * .5 + reach + 2, 4, getWidth() * .5 + reach + 2))) {
            if (!e.isAlive() || SpellUtil.isAlly(getOwner(), e)) {
                continue;
            }
            double[] l = local(e, foot);
            if (l[0] < -1 || l[0] > reach || Math.abs(l[1]) > getWidth() * .5 + e.getBbWidth() * .5 + .3 || e.getY() > foot.y + 3.5 || e.getY() < foot.y - 2.5) {
                continue;
            }
            hurt(e, damage);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3));
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
            e.setDeltaMovement(e.getDeltaMovement().multiply(.2, 0, .2).add(0, -.5, 0));
            e.hurtMarked = true;
        }
        // Bursts of bricks all along where it landed
        Vec3 across = new Vec3(-f.z, 0, f.x);
        int bursts = Math.max(2, Math.round(getWidth() / 3));
        for (int k = 0; k < bursts; k++) {
            float s = (k + .5f) / bursts - .5f;
            Vec3 at = foot.add(across.scale(s * getWidth())).add(f.scale(getHeight() * .55));
            WallFx.impact(level, at, 2.2f, 4);
        }
        WallFx.slam(level, foot.add(f.scale(getHeight() * .5)), 3f);
        level.playSound(null, foot.x, foot.y, foot.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2f, .5f);
        SpellUtil.cameraShake(level, foot, 22, getWidth() + 14);
        shatter(level);
    }

    /** The twin walls meet: what is between them is crushed and dazed, and both walls burst. */
    private void crush(ServerLevel level, Vec3 c) {
        Vec3 front = c.add(forward().scale(THICK * .5));
        for (LivingEntity e : SpellUtil.enemiesAround(level, front.add(0, 1, 0), getWidth() * .5 + 1, getOwner(), x -> true)) {
            hurt(e, damage);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
            e.setDeltaMovement(0, .5, 0);
            e.hurtMarked = true;
        }
        WallFx.impact(level, front, getWidth() * .6f, 8);
        WallFx.slam(level, front, 2f);
        SpellUtil.cameraShake(level, front, 14, 16);
        shatter(level);
    }

    /** It blows apart forward: bricks fly in a cone, hurting and throwing back the enemies there. */
    private void blow(ServerLevel level, float range, float amount) {
        Vec3 c = position().add(offset(0)).add(0, getHeight() * .5, 0);
        Vec3 f = forward();
        for (LivingEntity e : SpellUtil.enemiesAround(level, c, range, getOwner(), x -> {
            Vec3 d = x.position().add(0, x.getBbHeight() * .5, 0).subtract(c);
            double along = d.dot(f);
            return along > -.5 && along < range && Math.abs(d.x * -f.z + d.z * f.x) < getWidth() * .5 + along * .6;
        })) {
            double along = e.position().subtract(c).dot(f);
            hurt(e, amount * (float) (1 - .4 * Math.max(0, along) / range));
            double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
            e.setDeltaMovement(e.getDeltaMovement().add(f.scale(1.2 * resist)).add(0, .45 * resist, 0));
            e.hurtMarked = true;
        }
        WallFx.impact(level, position().add(offset(0)).add(f.scale(1.5)), getWidth() * .7f, 10);
        WallFx.slam(level, c, 1.8f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.4f, .7f);
        SpellUtil.cameraShake(level, c, 12, 16);
        shatter(level);
    }

    private void shatter(ServerLevel level) {
        this.entityData.set(DATA_SHATTERED, tickCount);
        Vec3 c = getKind() == Kind.TOPPLE ? position().add(forward().scale(getHeight() * .5)).add(0, .5, 0)
                : position().add(offset(0)).add(0, getHeight() * .5, 0);
        WallFx.dust(level, c, 40, getWidth() * .4);
        WallFx.embers(level, c, 24, getWidth() * .4);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
