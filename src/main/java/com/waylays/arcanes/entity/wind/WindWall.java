package com.waylays.arcanes.entity.wind;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.wind.WindFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Wind Wall: a wall of wind roaring up out of the ground across the caster's way: no shot gets through it (each is
 * thrown back the way it came), and any enemy that touches it is struck and thrown back.
 */
public class WindWall extends SpellEffectEntity {
    public static final float WIDTH = 7;
    public static final float HEIGHT = 4;
    public static final int RISE = 8;
    public static final int FALL = 10;

    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(WindWall.class, EntityDataSerializers.INT);

    private final Int2IntOpenHashMap lastHit = new Int2IntOpenHashMap();
    @Nullable
    private String spellId;

    public WindWall(EntityType<? extends WindWall> entityType, Level level) {
        super(entityType, level);
    }

    /** A wall standing at {@code at}, across the way {@code yaw} looks, for {@code life} ticks. */
    public static WindWall raise(Level level, LivingEntity caster, AbstractSpell spell, Vec3 at, float yaw, int life, float damage) {
        WindWall wall = new WindWall(ModEntities.WIND_WALL.get(), level);
        wall.setOwner(caster);
        wall.spellId = spell.getSpellId();
        wall.damage = damage;
        wall.moveTo(at.x, at.y, at.z, yaw, 0);
        wall.entityData.set(DATA_LIFE, life);
        level.addFreshEntity(wall);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BREEZE_WHIRL, SoundSource.PLAYERS, 2, .8f);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, .8f, 1.4f);
        return wall;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LIFE, 160);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    /** How strong it stands at {@code age} (0 to 1). */
    public float alive(float age) {
        return Mth.clamp(age / RISE, 0, 1) * (1 - Mth.clamp((age - getLife()) / FALL, 0, 1));
    }

    /** The way it faces (the way the caster looked). */
    public Vec3 facing() {
        return Vec3.directionFromRotation(0, getYRot());
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > getLife() + FALL) {
            discard();
            return;
        }
        if (tickCount > getLife()) {
            return;
        }
        Vec3 f = facing();
        Vec3 along = new Vec3(-f.z, 0, f.x);
        Vec3 c = position().add(0, HEIGHT * .5, 0);
        AABB box = new AABB(c, c).inflate(Math.abs(along.x) * WIDTH * .5 + .9, HEIGHT * .5 + .5, Math.abs(along.z) * WIDTH * .5 + .9);
        LivingEntity caster = getLivingOwner();
        // Shots striking it are thrown back the way they came
        if (WindFx.deflect(level, box, c.subtract(f.scale(3)), caster, 1) > 0) {
            level.playSound(null, c.x, c.y, c.z, SoundEvents.BREEZE_DEFLECT, SoundSource.PLAYERS, 1.2f, 1);
        }
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, x -> x.isAlive() && !SpellUtil.isAlly(caster, x) && x != caster)) {
            double side = e.position().subtract(position()).dot(f);
            Vec3 away = f.scale(side >= 0 ? 1 : -1);
            WindFx.throwBy(e, away.scale(.8).add(0, .3, 0));
            if (spell != null && tickCount - lastHit.getOrDefault(e.getId(), -100) >= 10) {
                lastHit.put(e.getId(), tickCount);
                Vec3 keep = e.getDeltaMovement();
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, damage, SpellUtil.damageSource(spell, this, caster));
                e.setDeltaMovement(keep);
            }
        }
        if (tickCount % 10 == 0) {
            WindFx.dust(level, position(), 5, WIDTH * .3);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
