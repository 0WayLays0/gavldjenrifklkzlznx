package com.waylays.arcanes.entity.cosmos;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.OwnedSummon;
import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.cosmos.CosmosFx;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A bubble where time has stopped for everything but its caster (and the caster's summons): creatures stand frozen
 * mid-stride, players cannot move, strike or cast, arrows and spells hang in the air, falling things stay up. What is
 * done to the frozen meanwhile does not happen yet: every blow is kept, and all of it lands at once when time starts
 * again, as the bubble bursts. Arrows loosed inside it stop a step away and fly on then.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public class TimeStopBubble extends SpellEffectEntity {
    private static final String TAG = "arcanes_oublies_frozen";
    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(TimeStopBubble.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(TimeStopBubble.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CASTER = SynchedEntityData.defineId(TimeStopBubble.class, EntityDataSerializers.INT);

    /** Everything frozen on the server, and the bubble holding it. */
    private static final Map<UUID, TimeStopBubble> FROZEN = new HashMap<>();

    /** How something was before it froze, and the blows kept for it. */
    private static final class Frozen {
        final Entity entity;
        final Vec3 pos;
        final Vec3 velocity;
        final boolean noGravity;
        final boolean noAi;
        float pending;
        @Nullable
        DamageSource source;

        Frozen(Entity entity) {
            this.entity = entity;
            this.pos = entity.position();
            this.velocity = entity.getDeltaMovement();
            this.noGravity = entity.isNoGravity();
            this.noAi = entity instanceof Mob mob && mob.isNoAi();
        }
    }

    private final Map<UUID, Frozen> held = new HashMap<>();
    private boolean released;

    public TimeStopBubble(EntityType<? extends TimeStopBubble> entityType, Level level) {
        super(entityType, level);
    }

    public static TimeStopBubble stop(Level level, LivingEntity caster, float radius, int life) {
        TimeStopBubble bubble = new TimeStopBubble(ModEntities.TIME_STOP_BUBBLE.get(), level);
        bubble.setOwner(caster);
        bubble.moveTo(caster.getX(), caster.getY(), caster.getZ(), 0, 0);
        bubble.entityData.set(DATA_RADIUS, radius);
        bubble.entityData.set(DATA_LIFE, life);
        bubble.entityData.set(DATA_CASTER, caster.getId());
        level.addFreshEntity(bubble);
        CosmosFx.sound(level, caster.position(), SoundEvents.BELL_BLOCK, 1.5f, .6f);
        CosmosFx.sound(level, caster.position(), SoundEvents.BEACON_ACTIVATE, 1.2f, .5f);
        CosmosFx.time(level, caster.position().add(0, 1, 0), 60, radius * .4);
        return bubble;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RADIUS, 7f);
        builder.define(DATA_LIFE, 100);
        builder.define(DATA_CASTER, -1);
    }

    public float getRadius() {
        return this.entityData.get(DATA_RADIUS);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    /** The caster's entity id (time goes on for it), for the client. */
    public int getCasterId() {
        return this.entityData.get(DATA_CASTER);
    }

    public boolean inside(Entity e) {
        float r = getRadius();
        return e.getBoundingBox().getCenter().distanceToSqr(position().add(0, 1, 0)) <= r * r;
    }

    /** Whether time stops for {@code e} in this bubble: not for the caster, its summons, the passing spell visuals. */
    private boolean stops(Entity e, @Nullable Entity owner) {
        if (e == owner || e instanceof SpellEffectEntity && !(e instanceof com.waylays.arcanes.entity.SpellProjectile) || e.isSpectator()) {
            return false;
        }
        if (owner != null && e instanceof OwnedSummon summon && owner.getUUID().equals(summon.getSummonerUUID())) {
            return false;
        }
        return !(e instanceof Player player && player.isCreative());
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount >= getLife()) {
            release(level);
            discard();
            return;
        }
        Entity owner = getOwner();
        float r = getRadius();
        for (Entity e : level.getEntities(this, new AABB(position(), position()).inflate(r + 1), e -> e.isAlive() && !held.containsKey(e.getUUID()) && inside(e))) {
            if (stops(e, owner) && !FROZEN.containsKey(e.getUUID())) {
                freeze(e);
            }
        }
        held.values().removeIf(f -> {
            if (!f.entity.isAlive() || f.entity.level() != level) {
                FROZEN.remove(f.entity.getUUID());
                return true;
            }
            hold(f);
            return false;
        });
        if (tickCount % 20 == 0) {
            CosmosFx.tick(level, position(), 1, .5f);
        }
    }

    private void freeze(Entity e) {
        Frozen f = new Frozen(e);
        held.put(e.getUUID(), f);
        FROZEN.put(e.getUUID(), this);
        e.getPersistentData().putInt(TAG, (f.noGravity ? 1 : 0) | (f.noAi ? 2 : 0));
        e.setNoGravity(true);
        if (e instanceof Mob mob) {
            mob.setNoAi(true);
            mob.getNavigation().stop();
        }
        hold(f);
        CosmosFx.time(level(), e.getBoundingBox().getCenter(), 6, .3);
    }

    private static void hold(Frozen f) {
        Entity e = f.entity;
        e.setDeltaMovement(Vec3.ZERO);
        if (e.position().distanceToSqr(f.pos) > 1.0E-4) {
            if (e instanceof ServerPlayer player) {
                player.connection.teleport(f.pos.x, f.pos.y, f.pos.z, player.getYRot(), player.getXRot());
            } else {
                e.setPos(f.pos.x, f.pos.y, f.pos.z);
            }
        }
        e.resetFallDistance();
        e.hurtMarked = true;
        if (e instanceof ServerPlayer player && MagicData.getPlayerMagicData(player).isCasting()) {
            Utils.serverSideCancelCast(player);
        }
    }

    /** Time starts again: everything moves on as it was, and every blow kept lands now. */
    private void release(ServerLevel level) {
        if (released) {
            return;
        }
        released = true;
        for (Frozen f : held.values()) {
            Entity e = f.entity;
            FROZEN.remove(e.getUUID());
            e.getPersistentData().remove(TAG);
            e.setNoGravity(f.noGravity);
            if (e instanceof Mob mob) {
                mob.setNoAi(f.noAi);
            }
            if (!(e instanceof LivingEntity)) {
                e.setDeltaMovement(f.velocity);
            }
            e.hurtMarked = true;
            if (f.pending > 0 && f.source != null && e instanceof LivingEntity living && living.isAlive()) {
                living.invulnerableTime = 0;
                living.hurt(f.source, f.pending);
                Vec3 c = living.getBoundingBox().getCenter();
                SpellUtil.particles(level, ParticleTypes.CRIT, c.x, c.y, c.z, (int) Math.min(40, 6 + f.pending * 2), .3, .4, .3, .4);
                CosmosFx.time(level, c, 12, .4);
            }
        }
        held.clear();
        Vec3 c = position().add(0, 1, 0);
        CosmosFx.sound(level, c, SoundEvents.GLASS_BREAK, 1.5f, .7f);
        CosmosFx.sound(level, c, SoundEvents.BELL_RESONATE, 1.2f, 1.2f);
        CosmosFx.time(level, c, 80, getRadius() * .5);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel level) {
            release(level);
        }
        super.remove(reason);
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        // Time cannot be cancelled: the bubble holds until it bursts
    }

    // ------------------------------------------------------------------ the frozen

    public static boolean isFrozen(Entity e) {
        return !e.level().isClientSide && FROZEN.containsKey(e.getUUID());
    }

    /** A blow on something frozen is kept, to land when time starts again. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        TimeStopBubble bubble = victim.level().isClientSide ? null : FROZEN.get(victim.getUUID());
        if (bubble == null) {
            return;
        }
        Frozen f = bubble.held.get(victim.getUUID());
        if (f != null) {
            f.pending += event.getAmount();
            f.source = event.getSource();
            Vec3 c = victim.getBoundingBox().getCenter();
            SpellUtil.particles(victim.level(), CosmosFx.AMBER, c.x, c.y, c.z, 4, .2, .3, .2, 0);
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (isFrozen(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (isFrozen(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isFrozen(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onHitBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (isFrozen(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /** Something saved while frozen (the world closed mid-spell) comes back unfrozen. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        Entity e = event.getEntity();
        if (e.level().isClientSide || !e.getPersistentData().contains(TAG) || FROZEN.containsKey(e.getUUID())) {
            return;
        }
        int was = e.getPersistentData().getInt(TAG);
        e.getPersistentData().remove(TAG);
        e.setNoGravity((was & 1) != 0);
        if (e instanceof Mob mob) {
            mob.setNoAi((was & 2) != 0);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
