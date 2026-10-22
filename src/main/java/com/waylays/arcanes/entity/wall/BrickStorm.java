package com.waylays.arcanes.entity.wall;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.wall.WallFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
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

import javax.annotation.Nullable;

/**
 * The Brick Rain: first a sigil burns on the ground where it will fall; then a vortex of bricks gathers in the air
 * above, turning, its mortar glowing; then it pours its bricks down, the first right on the middle, the rest all over
 * the circle, each bursting where it lands. Then the vortex breaks up.
 */
public class BrickStorm extends SpellEffectEntity {
    public static final int TELEGRAPH = 20;
    public static final int GATHER = 12;
    public static final int RAIN_START = TELEGRAPH + GATHER;
    public static final int FADE = 16;
    public static final float HEIGHT = 11;

    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(BrickStorm.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_BRICKS = SynchedEntityData.defineId(BrickStorm.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DURATION = SynchedEntityData.defineId(BrickStorm.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;

    public BrickStorm(EntityType<? extends BrickStorm> entityType, Level level) {
        super(entityType, level);
    }

    public static BrickStorm gather(Level level, LivingEntity caster, AbstractSpell spell, Vec3 at, float radius, int bricks, int duration, float damage) {
        BrickStorm storm = new BrickStorm(ModEntities.BRICK_STORM.get(), level);
        storm.setOwner(caster);
        storm.spellId = spell.getSpellId();
        storm.damage = damage;
        storm.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360, 0);
        storm.entityData.set(DATA_RADIUS, radius);
        storm.entityData.set(DATA_BRICKS, bricks);
        storm.entityData.set(DATA_DURATION, duration);
        level.addFreshEntity(storm);
        return storm;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RADIUS, 3.5f);
        builder.define(DATA_BRICKS, 12);
        builder.define(DATA_DURATION, 60);
    }

    public float getRadius() {
        return this.entityData.get(DATA_RADIUS);
    }

    public int getDuration() {
        return this.entityData.get(DATA_DURATION);
    }

    /** How much of the vortex there is (0 to 1): gathering, then breaking up. */
    public float vortex(float partialTicks) {
        float age = tickCount + partialTicks;
        return Mth.clamp((age - TELEGRAPH) / GATHER, 0, 1) * Mth.clamp(1 - (age - RAIN_START - getDuration()) / FADE, 0, 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        int duration = getDuration();
        if (tickCount > RAIN_START + duration + FADE) {
            discard();
            return;
        }
        if (tickCount == TELEGRAPH) {
            WallFx.sound(level, position().add(0, HEIGHT, 0), SoundEvents.STONE_BREAK, 2f, .5f);
            WallFx.sound(level, position().add(0, HEIGHT, 0), SoundEvents.GRINDSTONE_USE, 1.5f, .4f);
        }
        if (tickCount < RAIN_START || tickCount >= RAIN_START + duration) {
            return;
        }
        int bricks = this.entityData.get(DATA_BRICKS);
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        LivingEntity caster = getLivingOwner();
        if (spell == null || caster == null) {
            return;
        }
        // Bricks i whose turn falls on this tick
        for (int i = 0; i < bricks; i++) {
            if (RAIN_START + i * duration / bricks != tickCount) {
                continue;
            }
            float a = WallFx.hash(getId() * 31 + i) * Mth.TWO_PI;
            float r = i == 0 ? 0 : Mth.sqrt(WallFx.hash(getId() * 17 + i)) * getRadius();
            Vec3 spot = position().add(Mth.cos(a) * r, 0, Mth.sin(a) * r);
            Vec3 from = spot.add((random.nextDouble() - .5) * 1.5, HEIGHT - .5, (random.nextDouble() - .5) * 1.5);
            Vec3 velocity = spot.subtract(from).scale(1 / 18.0).add(0, -.1, 0);
            BrickBolt.shoot(level, caster, spell, BrickBolt.Kind.FALLING, from, velocity, spot, damage);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
