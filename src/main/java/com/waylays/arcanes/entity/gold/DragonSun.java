package com.waylays.arcanes.entity.gold;

import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.gold.GoldFx;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The giant sun the Golden Dragon pours out of: the sun stone of Eldorado, many times its size, blazing in the sky for
 * the moment the dragon takes to come out of it, then fading away. Only something to see.
 */
public class DragonSun extends Entity {
    /** Ticks it burns: the dragon's emergence and a little after. */
    public static final int LIFE = GoldenDragon.EMERGE_TICKS + 30;
    public static final float RADIUS = 3.4f;

    public DragonSun(EntityType<? extends DragonSun> entityType, Level level) {
        super(entityType, level);
        noPhysics = true;
    }

    public static DragonSun rise(Level level, Vec3 at) {
        DragonSun sun = new DragonSun(ModEntities.DRAGON_SUN.get(), level);
        sun.moveTo(at.x, at.y - sun.getBbHeight() * .5, at.z, level.random.nextFloat() * 360, 0);
        level.addFreshEntity(sun);
        return sun;
    }

    public Vec3 centre() {
        return position().add(0, getBbHeight() * .5, 0);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 c = centre();
        if (level().isClientSide) {
            if (tickCount < LIFE - 10) {
                for (int i = 0; i < 2; i++) {
                    double a = random.nextDouble() * Math.PI * 2;
                    double r = RADIUS * (1 + random.nextDouble() * .6);
                    level().addParticle(i == 0 ? GoldFx.sparks() : GoldFx.PALE_DUST, c.x + Math.cos(a) * r, c.y + Math.sin(a) * r, c.z + (random.nextDouble() - .5) * r,
                            0, -.04, 0);
                }
            }
            return;
        }
        if (tickCount == 1) {
            level().playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 4f, .5f);
            level().playSound(null, c.x, c.y, c.z, SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 3f, .6f);
        }
        if (tickCount == LIFE - 12) {
            level().playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 3f, .6f);
        }
        if (tickCount >= LIFE) {
            GoldFx.shatterBurst(level(), c.x, c.y, c.z, 2.5f);
            discard();
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(RADIUS * 2.5);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    /** Only a sight: not worth keeping across a reload. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
