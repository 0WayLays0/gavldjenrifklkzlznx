package com.waylays.arcanes.entity.token;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.token.TokenFx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * A thrown soul token where it came to rest, turning slowly under a thread of light so its caster can find it. Casting
 * Token Throw again (the recast) brings the caster to it. Each caster has one at a time.
 */
public class TokenMarker extends SpellEffectEntity {
    public static final int FADE_TICKS = 10;

    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(TokenMarker.class, EntityDataSerializers.INT);

    public TokenMarker(EntityType<? extends TokenMarker> entityType, Level level) {
        super(entityType, level);
    }

    /** Lays the caster's token at {@code at} (their earlier one goes). */
    public static TokenMarker place(ServerLevel level, LivingEntity caster, Vec3 at, int life) {
        TokenMarker old = find(level, caster.getUUID());
        if (old != null) {
            old.discard();
        }
        TokenMarker marker = new TokenMarker(ModEntities.TOKEN_MARKER.get(), level);
        marker.setOwner(caster);
        marker.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360, 0);
        marker.entityData.set(DATA_LIFE, life);
        level.addFreshEntity(marker);
        TokenFx.clink(level, at, .9f);
        return marker;
    }

    /** The token a caster threw, if it is still lying somewhere loaded. */
    @Nullable
    public static TokenMarker find(ServerLevel level, UUID owner) {
        for (Entity e : level.getAllEntities()) {
            if (e instanceof TokenMarker marker && !marker.isRemoved() && owner.equals(marker.getOwnerUUID())) {
                return marker;
            }
        }
        return null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LIFE, 200);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    /** 1 while it lies there, fading at the end. */
    public float strength(float partialTicks) {
        return Mth.clamp(1 - (tickCount + partialTicks - getLife()) / FADE_TICKS, 0, 1) * Mth.clamp((tickCount + partialTicks) / 4, 0, 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (random.nextInt(4) == 0) {
                level().addParticle(ParticleTypes.GLOW, getX() + (random.nextDouble() - .5) * .4, getY() + .3, getZ() + (random.nextDouble() - .5) * .4, 0, .03, 0);
            }
            return;
        }
        if (tickCount > getLife() + FADE_TICKS) {
            discard();
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
