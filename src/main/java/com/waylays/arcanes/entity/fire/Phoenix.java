package com.waylays.arcanes.entity.fire;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.fire.FireFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/**
 * The Phoenix: a great bird of Stellar Fire takes wing from the caster's hand and sweeps out the way they look,
 * swooping, burning everything it flies through; it wheels round in a wide turn and flies back alongside, then bursts
 * in a blaze. Its flight is set from where it took wing (the same on both sides).
 */
public class Phoenix extends SpellEffectEntity {
    public static final int OUT = 30;
    public static final int TURN = 20;
    public static final int BACK = 30;
    public static final int LIFE = OUT + TURN + BACK;
    public static final int FADE = 12;
    public static final float SPEED = .8f;
    public static final float WHEEL = 3.5f;
    public static final float REACH = 2.6f;

    private static final EntityDataAccessor<Vector3f> DATA_START = SynchedEntityData.defineId(Phoenix.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Float> DATA_YAW = SynchedEntityData.defineId(Phoenix.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_HEAT = SynchedEntityData.defineId(Phoenix.class, EntityDataSerializers.INT);

    private final Int2IntOpenHashMap lastHit = new Int2IntOpenHashMap();
    @Nullable
    private String spellId;

    public Phoenix(EntityType<? extends Phoenix> entityType, Level level) {
        super(entityType, level);
    }

    public static Phoenix rise(Level level, LivingEntity caster, AbstractSpell spell, float damage, int tier) {
        Phoenix bird = new Phoenix(ModEntities.PHOENIX.get(), level);
        bird.setOwner(caster);
        bird.spellId = spell.getSpellId();
        bird.damage = damage;
        float yaw = caster.getYRot();
        Vec3 start = caster.position().add(Vec3.directionFromRotation(0, yaw).scale(1.5));
        bird.entityData.set(DATA_START, start.toVector3f());
        bird.entityData.set(DATA_YAW, yaw);
        bird.entityData.set(DATA_HEAT, tier);
        Vec3 at = bird.path(0);
        bird.moveTo(at.x, at.y, at.z, yaw, 0);
        level.addFreshEntity(bird);
        FireFx.sound(level, at, SoundEvents.BLAZE_SHOOT, 2, .5f);
        FireFx.sound(level, at, SoundEvents.ENDER_DRAGON_FLAP, 2, 1.4f);
        return bird;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_START, new Vector3f());
        builder.define(DATA_YAW, 0f);
        builder.define(DATA_HEAT, 0);
    }

    public int getHeat() {
        return this.entityData.get(DATA_HEAT);
    }

    /** Where it is {@code t} ticks into its flight: out, wheeling round, back alongside. */
    public Vec3 path(float t) {
        Vec3 s = new Vec3(this.entityData.get(DATA_START));
        Vec3 f = Vec3.directionFromRotation(0, this.entityData.get(DATA_YAW));
        Vec3 side = new Vec3(-f.z, 0, f.x);
        float out = OUT * SPEED;
        float h = 2.4f + 1.1f * Mth.sin(t * .12f) - .6f * Mth.clamp(1 - t / 6, 0, 1);
        Vec3 flat;
        if (t <= OUT) {
            flat = s.add(f.scale(t * SPEED));
        } else if (t <= OUT + TURN) {
            float a = (t - OUT) / TURN * Mth.PI;
            Vec3 centre = s.add(f.scale(out)).add(side.scale(WHEEL));
            flat = centre.add(side.scale(-Mth.cos(a) * WHEEL)).add(f.scale(Mth.sin(a) * WHEEL));
        } else {
            flat = s.add(f.scale(out - (t - OUT - TURN) * SPEED)).add(side.scale(2 * WHEEL));
        }
        return flat.add(0, h, 0);
    }

    /** The way it faces {@code t} ticks into its flight. */
    public Vec3 heading(float t) {
        Vec3 d = path(t + .5f).subtract(path(t - .5f));
        return d.lengthSqr() < 1.0E-6 ? Vec3.directionFromRotation(0, this.entityData.get(DATA_YAW)) : d.normalize();
    }

    /** How far it has burnt out (0 to 1). */
    public float faded(float age) {
        return Mth.clamp((age - LIFE) / FADE, 0, 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > LIFE + FADE) {
            discard();
            return;
        }
        LivingEntity caster = getLivingOwner();
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (tickCount == LIFE) {
            FireBurst.at(level, position(), 2.2f, getHeat());
            for (LivingEntity e : SpellUtil.enemiesAround(level, position(), 3.5, caster, x -> true)) {
                FireFx.burn(e, damage * .8f, spell, this, caster, getHeat());
            }
        }
        if (tickCount >= LIFE) {
            return;
        }
        Vec3 at = path(tickCount);
        setPos(at.x, at.y, at.z);
        for (LivingEntity e : SpellUtil.enemiesAround(level, at, REACH, caster, x -> true)) {
            if (tickCount - lastHit.getOrDefault(e.getId(), -100) >= 15) {
                lastHit.put(e.getId(), tickCount);
                FireFx.burn(e, damage, spell, this, caster, getHeat());
            }
        }
        if (tickCount % 2 == 0) {
            FireFx.flames(level, at, 3, .8, getHeat());
            FireFx.embers(level, at.add(0, -.5, 0), 2, .6, getHeat());
        }
        if (tickCount % 12 == 0) {
            FireFx.sound(level, at, SoundEvents.ENDER_DRAGON_FLAP, 1.2f, 1.6f);
            FireFx.sound(level, at, SoundEvents.FIRE_AMBIENT, 1.4f, .8f);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 192 * 192;
    }
}
