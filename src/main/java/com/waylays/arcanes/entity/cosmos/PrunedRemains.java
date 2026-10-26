package com.waylays.arcanes.entity.cosmos;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.cosmos.CosmosFx;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Something being pruned by the TVA: it stops dead, its body turns to orange light, and that light crumbles away
 * from its feet up into rising embers until nothing is left. Then it is gone (its loot falls where it stood). A
 * player is struck down at once and their body crumbles the same way.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public class PrunedRemains extends SpellEffectEntity {
    public static final int CRUMBLE_TICKS = 18;
    private static final String TAG = "arcanes_oublies_pruning";
    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(PrunedRemains.class, EntityDataSerializers.INT);

    @Nullable
    private LivingEntity victim;
    @Nullable
    private DamageSource source;

    public PrunedRemains(EntityType<? extends PrunedRemains> entityType, Level level) {
        super(entityType, level);
    }

    /** Prunes {@code victim}: it crumbles away and is struck down by {@code source}. */
    public static void prune(ServerLevel level, LivingEntity victim, DamageSource source) {
        PrunedRemains remains = new PrunedRemains(ModEntities.PRUNED_REMAINS.get(), level);
        remains.moveTo(victim.getX(), victim.getY(), victim.getZ(), victim.getYRot(), 0);
        remains.entityData.set(DATA_TARGET, victim.getId());
        remains.victim = victim;
        remains.source = source;
        level.addFreshEntity(remains);
        Vec3 c = victim.getBoundingBox().getCenter();
        CosmosFx.sound(level, c, SoundEvents.BEACON_DEACTIVATE, 1.3f, 1.4f);
        CosmosFx.sound(level, c, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), .9f, 1.6f);
        SpellUtil.particles(level, new BlastwaveParticleOptions(1f, .55f, .12f, victim.getBbWidth() * 2 + 1), victim.getX(), victim.getY() + .05, victim.getZ(), 1, 0, 0, 0, 0);
        if (victim instanceof Player) {
            strike(victim, source);
            return;
        }
        // It stops dead and nothing else can touch it: it is already gone from time
        victim.getPersistentData().putBoolean(TAG, true);
        victim.setInvulnerable(true);
        victim.setInvisible(true);
        victim.setNoGravity(true);
        victim.setDeltaMovement(Vec3.ZERO);
        victim.hurtMarked = true;
        if (victim instanceof Mob mob) {
            mob.setNoAi(true);
        }
    }

    private static void strike(LivingEntity victim, DamageSource source) {
        victim.setInvulnerable(false);
        victim.invulnerableTime = 0;
        victim.hurt(source, 10000f);
        if (victim.isDeadOrDying() && !(victim instanceof Player)) {
            // Gone at once: nothing is left to fall over
            victim.deathTime = 19;
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_TARGET, -1);
    }

    @Nullable
    public Entity getVictim() {
        return level().getEntity(this.entityData.get(DATA_TARGET));
    }

    /** How far it has crumbled at {@code age}: 0 whole, 1 gone. */
    public static float crumbled(float age) {
        return Math.min(1, Math.max(0, (age - 3) / (CRUMBLE_TICKS - 3)));
    }

    @Override
    public void tick() {
        super.tick();
        Entity target = getVictim();
        if (target != null) {
            float line = crumbled(tickCount);
            double y = target.getY() + target.getBbHeight() * line;
            double w = target.getBbWidth() * .5;
            if (level().isClientSide) {
                for (int k = 0; k < 4; k++) {
                    level().addParticle(k % 2 == 0 ? CosmosFx.PRUNED : CosmosFx.TIME, target.getX() + (random.nextDouble() - .5) * w * 2, y,
                            target.getZ() + (random.nextDouble() - .5) * w * 2, 0, .06 + random.nextDouble() * .05, 0);
                }
            }
        }
        if (!(level() instanceof ServerLevel)) {
            return;
        }
        if (victim != null && victim.isAlive() && !(victim instanceof Player)) {
            victim.setDeltaMovement(Vec3.ZERO);
            victim.setPos(getX(), getY(), getZ());
        }
        if (tickCount >= CRUMBLE_TICKS) {
            if (victim != null && victim.isAlive() && source != null) {
                victim.getPersistentData().remove(TAG);
                strike(victim, source);
            }
            if (tickCount >= CRUMBLE_TICKS + 4) {
                discard();
            }
        }
    }

    /** Something being pruned when the world closed is gone when it comes back. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity().getPersistentData().getBoolean(TAG)) {
            event.setCanceled(true);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}
