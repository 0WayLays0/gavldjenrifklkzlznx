package com.waylays.arcanes.entity.fire;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.fire.FireFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/**
 * Flame Crescent: a slash of Stellar Fire flying flat through the air, cutting through every enemy in its way (each
 * once), burning them and setting them alight; it goes out after {@link #LIFE} ticks or on a wall.
 */
public class FlameCrescent extends SpellEffectEntity {
    public static final float SPAN = 3.2f;
    public static final float SPEED = .9f;
    public static final int LIFE = 26;
    public static final int FADE = 6;

    private static final EntityDataAccessor<Integer> DATA_HEAT = SynchedEntityData.defineId(FlameCrescent.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_TILT = SynchedEntityData.defineId(FlameCrescent.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Vector3f> DATA_DIR = SynchedEntityData.defineId(FlameCrescent.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> DATA_END = SynchedEntityData.defineId(FlameCrescent.class, EntityDataSerializers.INT);

    private final IntOpenHashSet cut = new IntOpenHashSet();
    @Nullable
    private String spellId;

    public FlameCrescent(EntityType<? extends FlameCrescent> entityType, Level level) {
        super(entityType, level);
    }

    public static FlameCrescent slash(Level level, LivingEntity caster, AbstractSpell spell, Vec3 from, Vec3 dir, float tilt, float damage, int tier) {
        FlameCrescent c = new FlameCrescent(ModEntities.FLAME_CRESCENT.get(), level);
        c.setOwner(caster);
        c.spellId = spell.getSpellId();
        c.damage = damage;
        c.moveTo(from.x, from.y, from.z, 0, 0);
        c.entityData.set(DATA_HEAT, tier);
        c.entityData.set(DATA_TILT, tilt);
        c.entityData.set(DATA_DIR, dir.normalize().toVector3f());
        level.addFreshEntity(c);
        return c;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_HEAT, 0);
        builder.define(DATA_TILT, 0f);
        builder.define(DATA_DIR, new Vector3f(0, 0, 1));
        builder.define(DATA_END, -1);
    }

    public int getHeat() {
        return this.entityData.get(DATA_HEAT);
    }

    public float getTilt() {
        return this.entityData.get(DATA_TILT);
    }

    public Vec3 getDir() {
        return new Vec3(this.entityData.get(DATA_DIR));
    }

    public float faded(float age) {
        int end = this.entityData.get(DATA_END);
        return end < 0 ? 0 : Mth.clamp((age - end) / FADE, 0, 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        int end = this.entityData.get(DATA_END);
        if (end >= 0) {
            if (tickCount > end + FADE) {
                discard();
            }
            return;
        }
        Vec3 dir = getDir();
        Vec3 next = position().add(dir.scale(SPEED));
        if (tickCount > LIFE || !level.noCollision(new AABB(next, next).inflate(.2))) {
            this.entityData.set(DATA_END, tickCount);
            FireBurst.at(level, position(), .8f, getHeat());
            return;
        }
        setPos(next.x, next.y, next.z);
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        LivingEntity caster = getLivingOwner();
        Vec3 side = new Vec3(-dir.z, 0, dir.x).normalize();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(next, next).inflate(SPAN * .6, 1.5, SPAN * .6))) {
            if (!e.isAlive() || cut.contains(e.getId()) || SpellUtil.isAlly(caster, e) || e == caster) {
                continue;
            }
            Vec3 d = e.getBoundingBox().getCenter().subtract(next);
            if (Math.abs(d.dot(side)) < SPAN * .55 && Math.abs(d.dot(dir)) < 1.2 && Math.abs(d.y) < 1.5) {
                cut.add(e.getId());
                FireFx.burn(e, damage, spell, this, caster, getHeat());
                FireFx.embers(level, e.getBoundingBox().getCenter(), 8, .3, getHeat());
            }
        }
        if (tickCount % 2 == 0) {
            FireFx.flames(level, position(), 2, .6, getHeat());
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
