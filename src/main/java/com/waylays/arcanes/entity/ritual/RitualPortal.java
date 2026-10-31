package com.waylays.arcanes.entity.ritual;

import com.waylays.arcanes.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * The Portal: a swirling gate standing upright for a minute, over the figure or in the wall it was drawn on, and its
 * twin at the anchor marked with the leader's chalk. A player who steps into one comes out of the other, a step out in
 * front of it (or wherever there is room). It faces its yaw.
 */
public class RitualPortal extends RitualEntity {
    public static final int LIFE = 20 * 60;
    public static final int COOLDOWN = 60;
    private static final String COOLDOWN_KEY = "arcanes_oublies.portal_cooldown";

    private Vec3 target = Vec3.ZERO;
    @Nullable
    private ResourceKey<Level> targetLevel;

    public RitualPortal(EntityType<? extends RitualPortal> entityType, Level level) {
        super(entityType, level);
    }

    /** Opens a portal at {@code at} leading to {@code to} in {@code toLevel}, and its twin there leading back. */
    public static void open(ServerLevel level, Vec3 at, float yaw, ServerLevel toLevel, Vec3 to, int life) {
        RitualPortal here = new RitualPortal(ModEntities.RITUAL_PORTAL.get(), level);
        here.moveTo(at.x, at.y, at.z, yaw, 0);
        here.setup(life, 1, Set.of(), 1);
        here.target = to;
        here.targetLevel = toLevel.dimension();
        level.addFreshEntity(here);
        RitualPortal there = new RitualPortal(ModEntities.RITUAL_PORTAL.get(), toLevel);
        there.moveTo(to.x, to.y, to.z, yaw, 0);
        there.setup(life, 1, Set.of(), 1);
        there.target = at;
        there.targetLevel = level.dimension();
        toLevel.addFreshEntity(there);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.2f, 1.2f);
        toLevel.playSound(null, to.x, to.y, to.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.2f, 1.2f);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (random.nextInt(2) == 0) {
                double a = random.nextDouble() * Math.PI * 2;
                level().addParticle(ParticleTypes.PORTAL, getX() + Math.cos(a) * 1.2, getY() + 1.4 + Math.sin(a) * 1.6, getZ(), 0, 0, 0);
            }
            return;
        }
        if (tickCount < 30 || targetLevel == null || !(level() instanceof ServerLevel level)) {
            return;
        }
        ServerLevel destination = level.getServer().getLevel(targetLevel);
        if (destination == null) {
            return;
        }
        long now = level.getGameTime();
        if (tickCount % 40 == 0) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.PORTAL_AMBIENT, SoundSource.PLAYERS, .6f, 1.2f);
        }
        for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(.6, 2, .6))) {
            if (player.getPersistentData().getLong(COOLDOWN_KEY) > now) {
                continue;
            }
            player.getPersistentData().putLong(COOLDOWN_KEY, now + COOLDOWN);
            Vec3 out = exit(destination, player);
            float facing = (float) Math.toDegrees(Math.atan2(-(out.x - target.x), out.z - target.z));
            player.teleportTo(destination, out.x, out.y, out.z, Set.of(), facing, 0);
            destination.playSound(null, out.x, out.y, out.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, .8f);
        }
    }

    /**
     * Where a player comes out of the twin: a step in front of it so as not to walk straight back in, else behind it
     * or beside it, wherever there is room for them.
     */
    private Vec3 exit(ServerLevel destination, Player player) {
        Vec3 front = Vec3.directionFromRotation(0, getYRot());
        Vec3 side = new Vec3(-front.z, 0, front.x);
        for (Vec3 way : new Vec3[] {front, front.reverse(), side, side.reverse()}) {
            Vec3 out = target.add(way.scale(1.8));
            if (destination.noCollision(player, player.getDimensions(player.getPose()).makeBoundingBox(out))) {
                return out;
            }
        }
        return target.add(front.scale(1.8));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
