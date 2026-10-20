package com.waylays.arcanes.entity.boss;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * The Rune Prison's wall: a circle of standing runes. The caster's side passes through it; anyone else who tries is
 * hurt and thrown back to the side they came from, in or out. It sinks back into the ground when its time is up.
 */
public class RuneWall extends SpellEffectEntity {
    public static final float HEIGHT = 4f;
    /** How far from the circle a body counts as touching the wall. */
    public static final float THICKNESS = .9f;
    public static final int RISE_TICKS = 10;
    public static final int HIT_COOLDOWN = 10;

    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(RuneWall.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_START = SynchedEntityData.defineId(RuneWall.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_END = SynchedEntityData.defineId(RuneWall.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;
    /** Which side of the wall each creature was last seen on (true: inside). */
    private final Map<UUID, Boolean> sides = new HashMap<>();
    private final Map<UUID, Long> lastHit = new HashMap<>();

    public RuneWall(EntityType<? extends RuneWall> entityType, Level level) {
        super(entityType, level);
    }

    public static RuneWall spawn(Level level, @Nullable LivingEntity caster, AbstractSpell spell, Vec3 center, float radius, int duration, float damage) {
        RuneWall wall = new RuneWall(ModEntities.RUNE_WALL.get(), level);
        wall.setOwner(caster);
        wall.spellId = spell.getSpellId();
        wall.damage = damage;
        wall.setPos(center);
        int now = (int) level.getGameTime();
        wall.entityData.set(DATA_RADIUS, radius);
        wall.entityData.set(DATA_START, now);
        wall.entityData.set(DATA_END, now + duration);
        level.addFreshEntity(wall);
        return wall;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RADIUS, 8f);
        builder.define(DATA_START, 0);
        builder.define(DATA_END, 0);
    }

    public float getRadius() {
        return this.entityData.get(DATA_RADIUS);
    }

    /** 0 sunk, 1 standing: rises at the start, sinks at the end. */
    public float standing(float partialTicks) {
        float now = (int) level().getGameTime() + partialTicks;
        float up = (now - this.entityData.get(DATA_START)) / RISE_TICKS;
        float down = (this.entityData.get(DATA_END) - now) / RISE_TICKS;
        return Mth.clamp(Math.min(up, down), 0, 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        int now = (int) level.getGameTime();
        if (now >= this.entityData.get(DATA_END) || tickCount > 2400) {
            discard();
            return;
        }
        float r = getRadius();
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(r + 3, HEIGHT + 2, r + 3))) {
            if (!e.isAlive() || e.isSpectator() || SpellUtil.isAlly(getOwner(), e) || e.getY() > getY() + HEIGHT || e.getY() + e.getBbHeight() < getY() - 1) {
                continue;
            }
            Vec3 d = e.position().subtract(position()).multiply(1, 0, 1);
            double dist = d.length();
            boolean inside = dist < r;
            if (Math.abs(dist - r) > THICKNESS + e.getBbWidth() * .5) {
                sides.put(e.getUUID(), inside);
                continue;
            }
            // Touching the wall: back to the side it came from
            boolean wasInside = sides.getOrDefault(e.getUUID(), inside);
            Vec3 out = dist < 1.0E-3 ? new Vec3(1, 0, 0) : d.scale(1 / dist);
            Vec3 push = out.scale(wasInside ? -.7 : .7);
            e.setDeltaMovement(push.x, .25, push.z);
            e.hurtMarked = true;
            Long last = lastHit.get(e.getUUID());
            if (spell != null && (last == null || now - last >= HIT_COOLDOWN)) {
                lastHit.put(e.getUUID(), (long) now);
                DamageSources.applyDamage(e, damage, SpellUtil.damageSource(spell, this, getOwner()));
                SpellUtil.particles(level, ParticleTypes.ENCHANTED_HIT, e.getX(), e.getY() + e.getBbHeight() * .5, e.getZ(), 10, .3, .3, .3, .2);
                level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.SHULKER_BULLET_HIT, SoundSource.HOSTILE, 1.2f, .6f);
            }
        }
        if (tickCount % 30 == 0) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 2f, .5f);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (spellId != null) {
            tag.putString("Spell", spellId);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        spellId = tag.contains("Spell") ? tag.getString("Spell") : null;
    }
}
