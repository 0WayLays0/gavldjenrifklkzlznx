package com.waylays.arcanes.entity.token;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.token.TokenFx;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/**
 * A soul token drawn out of a creature (Soul Tribute): it flies in an arc to the caster and heals them as it reaches
 * them. Its path is worked out the same way on both sides, from where it started to where the caster is now.
 */
public class TokenMote extends SpellEffectEntity {
    public static final int FLIGHT = 16;

    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(TokenMote.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> DATA_START = SynchedEntityData.defineId(TokenMote.class, EntityDataSerializers.VECTOR3);

    private float heal;

    public TokenMote(EntityType<? extends TokenMote> entityType, Level level) {
        super(entityType, level);
    }

    public static void send(ServerLevel level, Vec3 from, LivingEntity caster, float heal) {
        TokenMote mote = new TokenMote(ModEntities.TOKEN_MOTE.get(), level);
        mote.setOwner(caster);
        mote.heal = heal;
        mote.moveTo(from.x, from.y, from.z, level.random.nextFloat() * 360, 0);
        mote.entityData.set(DATA_TARGET, caster.getId());
        mote.entityData.set(DATA_START, new Vector3f((float) from.x, (float) from.y, (float) from.z));
        level.addFreshEntity(mote);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_TARGET, -1);
        builder.define(DATA_START, new Vector3f());
    }

    @Nullable
    public Entity getTarget() {
        return level().getEntity(this.entityData.get(DATA_TARGET));
    }

    /** Where it is along its arc, {@code t} from 0 to 1, flying to {@code to}. */
    public Vec3 along(float t, Vec3 to) {
        Vector3f s = this.entityData.get(DATA_START);
        Vec3 from = new Vec3(s.x, s.y, s.z);
        float e = t * t * (3 - 2 * t);
        return from.lerp(to, e).add(0, Mth.sin(t * Mth.PI) * (1.5 + from.distanceTo(to) * .15), 0);
    }

    public float progress(float partialTicks) {
        return Mth.clamp((tickCount + partialTicks) / FLIGHT, 0, 1);
    }

    @Override
    public void tick() {
        super.tick();
        Entity target = getTarget();
        if (target == null) {
            if (!level().isClientSide) {
                discard();
            }
            return;
        }
        Vec3 p = along(progress(0), TokenFx.chest(target));
        setPos(p.x, p.y, p.z);
        if (level().isClientSide) {
            level().addParticle(TokenFx.DUST, p.x, p.y, p.z, 0, 0, 0);
            return;
        }
        if (tickCount >= FLIGHT) {
            if (target instanceof LivingEntity living && living.isAlive()) {
                living.heal(heal);
                TokenFx.burst(level(), p, 6, .3);
                TokenFx.sound(level(), p, SoundEvents.EXPERIENCE_ORB_PICKUP, .7f, 1.2f + random.nextFloat() * .4f);
            }
            discard();
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 64 * 64;
    }
}
