package com.waylays.arcanes.entity.cosmos;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.cosmos.Cosmos;
import com.waylays.arcanes.spells.cosmos.CosmosFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * A belt of asteroids round a spot: tumbling rocks of every size going round in a ring, a cage. What crosses the ring
 * is struck and thrown back inside, and shots that meet it are smashed. When the caster closes their hand (casts
 * again), or when its time is up, the belt falls in on itself: the rocks spin faster and faster as they close in,
 * dragging everything inside to the middle, and crash together there, crushing it.
 * Where each rock is follows from the belt's age alone, so server and client agree without sending them.
 */
public class AsteroidBelt extends SpellEffectEntity {
    public static final float RADIUS = 5.5f;
    /** The ring is this thick and this tall. */
    public static final float BAND = 1.1f;
    public static final float TOP = 2.6f;
    public static final float ORBIT_SPEED = .045f;
    public static final int GATHER_TICKS = 14;
    public static final int CRUSH_TICKS = 14;
    public static final int AFTER_TICKS = 10;
    public static final float CRUSH_RADIUS = 2.6f;

    private static final EntityDataAccessor<Integer> DATA_COUNT = SynchedEntityData.defineId(AsteroidBelt.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(AsteroidBelt.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CRUSH = SynchedEntityData.defineId(AsteroidBelt.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;
    private final Map<Integer, Integer> struck = new HashMap<>();

    public AsteroidBelt(EntityType<? extends AsteroidBelt> entityType, Level level) {
        super(entityType, level);
    }

    public static AsteroidBelt raise(Level level, LivingEntity caster, AbstractSpell spell, Vec3 at, int count, int life, float damage) {
        AsteroidBelt old = of(level, caster);
        if (old != null) {
            old.crush();
        }
        AsteroidBelt belt = new AsteroidBelt(ModEntities.ASTEROID_BELT.get(), level);
        belt.setOwner(caster);
        belt.spellId = spell.getSpellId();
        belt.damage = damage;
        belt.moveTo(at.x, at.y, at.z, 0, 0);
        belt.entityData.set(DATA_COUNT, count);
        belt.entityData.set(DATA_LIFE, life);
        level.addFreshEntity(belt);
        CosmosFx.hum(level, at, 1.4f, .7f);
        CosmosFx.sound(level, at, SoundEvents.GRINDSTONE_USE, 1, .5f);
        return belt;
    }

    /** The caster's belt that has not begun to fall in yet. */
    @Nullable
    public static AsteroidBelt of(Level level, LivingEntity caster) {
        for (AsteroidBelt belt : level.getEntitiesOfClass(AsteroidBelt.class, caster.getBoundingBox().inflate(64), b -> caster.getUUID().equals(b.getOwnerUUID()))) {
            if (!belt.isCrushing()) {
                return belt;
            }
        }
        return null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_COUNT, 16);
        builder.define(DATA_LIFE, 200);
        builder.define(DATA_CRUSH, -1);
    }

    public int getCount() {
        return this.entityData.get(DATA_COUNT);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    /** The tick the belt began to fall in, or -1. */
    public int getCrushStart() {
        return this.entityData.get(DATA_CRUSH);
    }

    public boolean isCrushing() {
        return getCrushStart() >= 0;
    }

    /** How far it has fallen in (0 to 1) at {@code age}. */
    public float crushProgress(float age) {
        int start = getCrushStart();
        return start < 0 ? 0 : Mth.clamp((age - start) / CRUSH_TICKS, 0, 1);
    }

    /** The ring's radius at {@code age}: it gathers from further out, then falls in. */
    public float radiusAt(float age) {
        float gather = Mth.clamp(age / GATHER_TICKS, 0, 1);
        float r = RADIUS + 3 * (1 - gather) * (1 - gather);
        float c = crushProgress(age);
        return Mth.lerp(c * c, r, .5f);
    }

    /** Closes the belt: it falls in on itself. */
    public void crush() {
        if (!isCrushing()) {
            this.entityData.set(DATA_CRUSH, tickCount);
            CosmosFx.sound(level(), position(), SoundEvents.GRINDSTONE_USE, 1.6f, .7f);
            CosmosFx.sound(level(), position(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), 1.2f, .6f);
        }
    }

    // ------------------------------------------------------------------ the rocks (shared by server and client)

    /** A rock's own number between 0 and 1 for {@code salt}. */
    public float trait(int rock, int salt) {
        int h = (getId() * 73856093) ^ (rock * 19349663) ^ (salt * 83492791);
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        h ^= h >>> 15;
        return (h & 0xFFFF) / 65535f;
    }

    /** Where rock {@code k} is at {@code age}, relative to the belt's foot. */
    public Vec3 rock(int k, float age) {
        int n = getCount();
        float r = radiusAt(age) + (trait(k, 1) - .5f) * BAND * (1 - crushProgress(age));
        float speed = .8f + .4f * trait(k, 2);
        float angle = k * Mth.TWO_PI / n + (trait(k, 3) - .5f) * .35f + age * ORBIT_SPEED * speed;
        int start = getCrushStart();
        if (start >= 0) {
            // Closing in, they whirl faster and faster, as a skater drawing in their arms
            float c = crushProgress(age);
            angle += c * c * c * 7;
        }
        float y = .35f + trait(k, 4) * (TOP - .6f) + .15f * Mth.sin(age * .07f + k);
        y = Mth.lerp(crushProgress(age) * crushProgress(age), y, 1.1f);
        return new Vec3(Mth.cos(angle) * r, y, Mth.sin(angle) * r);
    }

    public float rockSize(int k) {
        float t = trait(k, 5);
        return .35f + .7f * t * t;
    }

    // ------------------------------------------------------------------ server

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity caster = getLivingOwner();
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (spell == null) {
            discard();
            return;
        }
        if (!isCrushing() && (tickCount >= getLife() || caster == null || !caster.isAlive())) {
            crush();
        }
        int start = getCrushStart();
        if (start < 0) {
            ring(level, caster, spell);
            if (tickCount % 30 == 0) {
                CosmosFx.sound(level, position(), SoundEvents.GRINDSTONE_USE, .5f, .4f + random.nextFloat() * .1f);
            }
            return;
        }
        int t = tickCount - start;
        if (t < CRUSH_TICKS) {
            gather(level, caster, tickCount);
        } else if (t == CRUSH_TICKS) {
            impact(level, caster, spell);
        } else if (t > CRUSH_TICKS + AFTER_TICKS) {
            discard();
        }
    }

    private boolean inBand(Entity e, double r) {
        double y = e.getY() - getY();
        return y < TOP + .3 && y + e.getBbHeight() > 0;
    }

    /** The ring strikes and throws back what crosses it, and smashes shots. */
    private void ring(ServerLevel level, @Nullable LivingEntity caster, AbstractSpell spell) {
        float r = radiusAt(tickCount);
        if (tickCount < GATHER_TICKS) {
            return;
        }
        for (LivingEntity e : SpellUtil.enemiesAround(level, position(), r + 3, caster, e -> true)) {
            double dx = e.getX() - getX(), dz = e.getZ() - getZ();
            double d = Math.sqrt(dx * dx + dz * dz);
            if (Math.abs(d - r) > BAND * .5 + e.getBbWidth() * .5 || !inBand(e, r)) {
                continue;
            }
            Integer last = struck.get(e.getId());
            if (last != null && tickCount - last < 10) {
                continue;
            }
            struck.put(e.getId(), tickCount);
            DamageSources.applyDamage(e, damage * .5f, SpellUtil.damageSource(spell, this, caster));
            // Thrown back inside: the belt is a cage
            Vec3 in = d < 1.0E-3 ? new Vec3(1, 0, 0) : new Vec3(-dx / d, 0, -dz / d);
            e.setDeltaMovement(in.scale(.75).add(0, .25, 0));
            e.hurtMarked = true;
            Vec3 c = e.getBoundingBox().getCenter();
            SpellUtil.particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.TUFF.defaultBlockState()), c.x, c.y, c.z, 14, .3, .3, .3, .1);
            SpellUtil.particles(level, ParticleTypes.CRIT, c.x, c.y, c.z, 6, .3, .3, .3, .2);
            CosmosFx.sound(level, c, SoundEvents.STONE_BREAK, 1.2f, .7f);
        }
        AABB box = getBoundingBox().inflate(r + 2, 0, r + 2).expandTowards(0, TOP + 1, 0);
        for (Projectile shot : level.getEntitiesOfClass(Projectile.class, box)) {
            Entity shooter = shot.getOwner();
            if (shooter != null && (shooter == caster || Cosmos.isAlly(caster, shooter))) {
                continue;
            }
            double dx = shot.getX() - getX(), dz = shot.getZ() - getZ();
            double d = Math.sqrt(dx * dx + dz * dz);
            if (Math.abs(d - r) < BAND && inBand(shot, r)) {
                SpellUtil.particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLED_DEEPSLATE.defaultBlockState()),
                        shot.getX(), shot.getY(), shot.getZ(), 10, .2, .2, .2, .1);
                CosmosFx.sound(level, shot.position(), SoundEvents.STONE_HIT, 1, 1.2f);
                shot.discard();
            }
        }
    }

    /** Falling in, the ring drags what is inside along to the middle. */
    private void gather(ServerLevel level, @Nullable LivingEntity caster, float age) {
        float r = radiusAt(age + 1);
        for (LivingEntity e : SpellUtil.enemiesAround(level, position(), RADIUS + 2, caster, e -> true)) {
            if (!inBand(e, r)) {
                continue;
            }
            double dx = e.getX() - getX(), dz = e.getZ() - getZ();
            double d = Math.sqrt(dx * dx + dz * dz);
            double keep = Math.max(.4, r - .5);
            if (d > keep && d < RADIUS + BAND) {
                double move = d - keep;
                e.setDeltaMovement(-dx / d * move, Math.max(e.getDeltaMovement().y, .05), -dz / d * move);
                e.hurtMarked = true;
            }
        }
        if (tickCount % 3 == 0) {
            for (int k = 0; k < 3; k++) {
                Vec3 p = position().add(rock(random.nextInt(getCount()), age));
                SpellUtil.particles(level, ParticleTypes.FLAME, p.x, p.y, p.z, 1, .1, .1, .1, .01);
            }
        }
    }

    /** The rocks crash together in the middle. */
    private void impact(ServerLevel level, @Nullable LivingEntity caster, AbstractSpell spell) {
        Vec3 c = position().add(0, 1.1, 0);
        for (LivingEntity e : SpellUtil.enemiesAround(level, c, CRUSH_RADIUS + 1, caster, e -> true)) {
            DamageSources.applyDamage(e, damage * 2.4f, SpellUtil.damageSource(spell, this, caster));
            Vec3 out = e.position().subtract(position()).multiply(1, 0, 1);
            out = out.lengthSqr() < 1.0E-4 ? Vec3.ZERO : out.normalize().scale(.4);
            e.setDeltaMovement(out.x, .65, out.z);
            e.hurtMarked = true;
        }
        SpellUtil.particles(level, new BlastwaveParticleOptions(1f, .55f, .2f, CRUSH_RADIUS * 2), c.x, getY() + .1, c.z, 1, 0, 0, 0, 0);
        SpellUtil.particles(level, ParticleTypes.EXPLOSION, c.x, c.y, c.z, 4, 1, .6, 1, 0);
        SpellUtil.particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.TUFF.defaultBlockState()), c.x, c.y, c.z, 80, 1.2, .8, 1.2, .3);
        SpellUtil.particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLED_DEEPSLATE.defaultBlockState()), c.x, c.y, c.z, 60, 1.2, .8, 1.2, .3);
        SpellUtil.particles(level, ParticleTypes.LAVA, c.x, c.y, c.z, 12, .8, .4, .8, 0);
        CosmosFx.stars(level, c, 30, 1.2);
        CosmosFx.sound(level, c, SoundEvents.GENERIC_EXPLODE.value(), 1.6f, .7f);
        CosmosFx.sound(level, c, SoundEvents.ANVIL_LAND, 1.2f, .5f);
        SpellUtil.cameraShake(level, c, 12, 14);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
