package com.waylays.arcanes.entity.token;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Coin Flip: a big soul token tossed up from the caster's hand, spinning, and caught again; then it shows the side it
 * fell on (heads in gold, tails in cyan) for a moment. Only a picture: the spell decides what the toss does.
 */
public class TokenFlip extends SpellEffectEntity {
    public static final int TOSS_TICKS = 18;
    public static final int SHOW_TICKS = 14;
    public static final int UNKNOWN = 0, HEADS = 1, TAILS = 2;

    private static final EntityDataAccessor<Integer> DATA_OWNER = SynchedEntityData.defineId(TokenFlip.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_RESULT = SynchedEntityData.defineId(TokenFlip.class, EntityDataSerializers.INT);

    public TokenFlip(EntityType<? extends TokenFlip> entityType, Level level) {
        super(entityType, level);
    }

    public static TokenFlip toss(Level level, LivingEntity caster) {
        TokenFlip flip = new TokenFlip(ModEntities.TOKEN_FLIP.get(), level);
        flip.setOwner(caster);
        flip.entityData.set(DATA_OWNER, caster.getId());
        flip.moveTo(caster.getX(), caster.getY(), caster.getZ(), caster.getYRot(), 0);
        level.addFreshEntity(flip);
        return flip;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER, -1);
        builder.define(DATA_RESULT, UNKNOWN);
    }

    @Nullable
    public Entity getBearer() {
        return level().getEntity(this.entityData.get(DATA_OWNER));
    }

    public int getResult() {
        return this.entityData.get(DATA_RESULT);
    }

    public void setResult(int result) {
        this.entityData.set(DATA_RESULT, result);
    }

    /** Height over the hand while it is in the air (a throw up and the fall back). */
    public float height(float partialTicks) {
        float t = Mth.clamp((tickCount + partialTicks) / TOSS_TICKS, 0, 1);
        return 4 * t * (1 - t) * 1.8f;
    }

    /** Where the hand holding it is, relative to the bearer's feet. */
    public static Vec3 hand(Entity bearer) {
        float yaw = bearer.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw) * .55 - Mth.cos(yaw) * .3, bearer.getBbHeight() * .62, Mth.cos(yaw) * .55 - Mth.sin(yaw) * .3);
    }

    @Override
    public void tick() {
        super.tick();
        Entity bearer = getBearer();
        if (bearer != null) {
            setPos(bearer.getX(), bearer.getY(), bearer.getZ());
        }
        if (!level().isClientSide && (bearer == null || tickCount > TOSS_TICKS + SHOW_TICKS)) {
            discard();
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 64 * 64;
    }
}
