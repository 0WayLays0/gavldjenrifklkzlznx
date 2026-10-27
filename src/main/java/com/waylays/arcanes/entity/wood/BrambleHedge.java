package com.waylays.arcanes.entity.wood;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.wood.WoodFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Brambles of Wood Magic, thorny stems bursting out of the ground and arching over one another:
 * <ul>
 * <li>LINE: a hedge across the way, rising from its middle outwards (the Bramble Hedge): enemies are torn by it and
 * thrown back out on the side they came from: it holds the way shut.</li>
 * <li>PATCH: a round thicket (where a seed pod burst): enemies in it are torn and held back.</li>
 * </ul>
 * Both sides work out every stem from the entity. When its time is up the brambles wither back into the ground.
 */
public class BrambleHedge extends SpellEffectEntity {
    public enum Mode { LINE, PATCH }

    public static final int GROW = 10;
    public static final int WITHER = 16;
    public static final float HEIGHT = 2.2f;
    /** Half the depth of a hedge. */
    public static final float DEPTH = .9f;
    /** Ticks an enemy is safe after the thorns have torn it. */
    public static final int GRACE = 10;

    private static final EntityDataAccessor<Integer> DATA_MODE = SynchedEntityData.defineId(BrambleHedge.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(BrambleHedge.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_DURATION = SynchedEntityData.defineId(BrambleHedge.class, EntityDataSerializers.INT);

    /**
     * One stem: it springs from (x0, z0) at the tick {@code start}, arches up to {@code height} and comes down at
     * (x1, z1) (or curls up into the air when {@code curl}).
     */
    public record Stem(float x0, float z0, float x1, float z1, float height, int start, boolean curl, int seed) {}

    private final Int2IntOpenHashMap lastHit = new Int2IntOpenHashMap();
    @Nullable
    private List<Stem> stems;
    @Nullable
    private String spellId;

    public BrambleHedge(EntityType<? extends BrambleHedge> entityType, Level level) {
        super(entityType, level);
    }

    /** {@code size}: a hedge's length or a thicket's radius. A hedge runs across {@code yaw}. */
    public static BrambleHedge grow(Level level, @Nullable LivingEntity caster, AbstractSpell spell, Mode mode, Vec3 at, float yaw, float size, int duration, float damage) {
        BrambleHedge hedge = new BrambleHedge(ModEntities.BRAMBLE_HEDGE.get(), level);
        hedge.setOwner(caster);
        hedge.spellId = spell.getSpellId();
        hedge.damage = damage;
        hedge.moveTo(at.x, at.y, at.z, yaw, 0);
        hedge.entityData.set(DATA_MODE, mode.ordinal());
        hedge.entityData.set(DATA_SIZE, size);
        hedge.entityData.set(DATA_DURATION, duration);
        level.addFreshEntity(hedge);
        WoodFx.sprout(level, at, 1.3f);
        return hedge;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_MODE, 0);
        builder.define(DATA_SIZE, 8f);
        builder.define(DATA_DURATION, 200);
    }

    public Mode getMode() {
        Mode[] all = Mode.values();
        return all[Mth.clamp(this.entityData.get(DATA_MODE), 0, all.length - 1)];
    }

    public float getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    public int getDuration() {
        return this.entityData.get(DATA_DURATION);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        stems = null;
    }

    /** How far the brambles have grown (0 to 1) at {@code age}, withering away at the end. */
    public float grown(Stem stem, float age) {
        float up = Mth.clamp((age - stem.start()) / GROW, 0, 1);
        float down = Mth.clamp((age - getDuration()) / WITHER, 0, 1);
        return (1 - (1 - up) * (1 - up)) * (1 - down);
    }

    /** Every stem, worked out the same way on both sides (in the entity's own frame: +X along a hedge, +Z across). */
    public List<Stem> stems() {
        if (stems != null) {
            return stems;
        }
        List<Stem> out = new ArrayList<>();
        int seed = getId() * 6007;
        float size = getSize();
        if (getMode() == Mode.LINE) {
            int count = Math.max(4, (int) (size * 2.2f));
            for (int k = 0; k < count; k++) {
                float x0 = -size * .5f + size * (k + WoodFx.hash(seed + k) * .6f) / count;
                float z0 = (WoodFx.hash(seed + k + 1) - .5f) * DEPTH * 1.8f;
                float reach = (.9f + WoodFx.hash(seed + k + 2) * 1.1f) * (k % 2 == 0 ? 1 : -1);
                float x1 = Mth.clamp(x0 + reach, -size * .5f, size * .5f);
                float z1 = (WoodFx.hash(seed + k + 3) - .5f) * DEPTH * 1.8f;
                float h = HEIGHT * (.6f + .45f * WoodFx.hash(seed + k + 4));
                // From the middle outwards
                int start = (int) (Math.abs(x0) / Math.max(1, size * .5f) * 8);
                out.add(new Stem(x0, z0, x1, z1, h, start, k % 5 == 2, seed + k * 41));
            }
        } else {
            int count = Math.max(6, (int) (size * size * 2.2f));
            for (int k = 0; k < count; k++) {
                float a = WoodFx.hash(seed + k * 3) * Mth.TWO_PI;
                float r = Mth.sqrt(WoodFx.hash(seed + k * 3 + 1)) * size;
                float x0 = Mth.cos(a) * r, z0 = Mth.sin(a) * r;
                float b = a + (WoodFx.hash(seed + k * 3 + 2) - .5f) * 3;
                float reach = .8f + WoodFx.hash(seed + k + 9) * .9f;
                float x1 = x0 + Mth.cos(b) * reach, z1 = z0 + Mth.sin(b) * reach;
                float len = Mth.sqrt(x1 * x1 + z1 * z1);
                if (len > size) {
                    x1 *= size / len;
                    z1 *= size / len;
                }
                float h = HEIGHT * (.4f + .4f * WoodFx.hash(seed + k + 4)) * (1 - .35f * r / Math.max(.1f, size));
                out.add(new Stem(x0, z0, x1, z1, h, (int) (WoodFx.hash(seed + k + 5) * 6), k % 4 == 1, seed + k * 41));
            }
        }
        stems = out;
        return out;
    }

    /** Where {@code e} stands in the hedge's own frame: (along, across). */
    private double[] local(LivingEntity e) {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        double c = Mth.cos(yaw), s = Mth.sin(yaw);
        double dx = e.getX() - getX(), dz = e.getZ() - getZ();
        return new double[]{dx * c + dz * s, -dx * s + dz * c};
    }

    /** Whether {@code e} is in the brambles. */
    public boolean inside(LivingEntity e) {
        if (e.getY() > getY() + HEIGHT || e.getY() + e.getBbHeight() < getY() - .5) {
            return false;
        }
        if (getMode() == Mode.LINE) {
            double[] l = local(e);
            return Math.abs(l[0]) < getSize() * .5 + e.getBbWidth() * .5 && Math.abs(l[1]) < DEPTH + e.getBbWidth() * .5;
        }
        return Math.hypot(e.getX() - getX(), e.getZ() - getZ()) < getSize() + e.getBbWidth() * .5;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > getDuration() + WITHER) {
            discard();
            return;
        }
        if (tickCount == getDuration()) {
            WoodFx.creak(level, position(), 1);
            WoodFx.leaves(level, position().add(0, 1, 0), 20, getSize() * .4);
        }
        if (tickCount < 4 || tickCount > getDuration()) {
            return;
        }
        if (tickCount < GROW + 8 && tickCount % 3 == 0) {
            for (Stem s : stems()) {
                if (s.start() == tickCount - 3) {
                    Vec3 at = toWorld(s.x0(), s.z0());
                    WoodFx.chips(level, at.add(0, .1, 0), 3, .2);
                }
            }
            WoodFx.sprout(level, position(), .8f);
        }
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        LivingEntity caster = getLivingOwner();
        double reach = getMode() == Mode.LINE ? getSize() * .5 + 2 : getSize() + 1.5;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(reach, HEIGHT + 1, reach))) {
            if (!e.isAlive() || caster == e || SpellUtil.isAlly(caster, e) || !inside(e)) {
                continue;
            }
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3, false, true));
            if (getMode() == Mode.LINE) {
                // Thrown back out on the side it came from
                float yaw = getYRot() * Mth.DEG_TO_RAD;
                Vec3 across = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
                double side = local(e)[1] >= 0 ? 1 : -1;
                double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
                Vec3 v = e.getDeltaMovement();
                double into = v.dot(across) * -side;
                if (into > 0) {
                    v = v.add(across.scale(into * side));
                }
                e.setDeltaMovement(v.add(across.scale(side * .35 * Math.max(.3, resist))).add(0, .08, 0));
                e.hurtMarked = true;
            }
            if (spell != null && tickCount - lastHit.getOrDefault(e.getId(), -100) >= GRACE) {
                lastHit.put(e.getId(), tickCount);
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, damage, SpellUtil.damageSource(spell, this, caster));
                WoodFx.leaves(level, e.position().add(0, .8, 0), 5, .3);
                WoodFx.sound(level, e.position(), SoundEvents.SWEET_BERRY_BUSH_BREAK, 1, .7f);
            }
        }
    }

    /** A point of the entity's own frame (+X along, +Z across) in the world, on its ground. */
    public Vec3 toWorld(float x, float z) {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        float c = Mth.cos(yaw), s = Mth.sin(yaw);
        return position().add(x * c - z * s, 0, x * s + z * c);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
