package com.waylays.arcanes.ritual;

import com.waylays.arcanes.entity.ritual.AmethystBridge;
import com.waylays.arcanes.entity.ritual.Meteor;
import com.waylays.arcanes.entity.ritual.RitualDome;
import com.waylays.arcanes.entity.ritual.RitualPortal;
import com.waylays.arcanes.entity.ritual.StoneColossus;
import com.waylays.arcanes.entity.ritual.StormCloud;
import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/** What each ritual does once it is cast. */
public final class RitualEffects {
    public static final float BLESSING_RADIUS = 24;
    public static final int BLESSING_TICKS = 20 * 60 * 5;

    private RitualEffects() {}

    public static void manifest(ServerLevel level, RitualCircle circle, Set<UUID> allies, float power) {
        Vec3 c = circle.position();
        switch (circle.getRitual()) {
            case METEOR -> Meteor.fall(level, meteorTarget(level, circle), allies, power);
            case DOME -> RitualDome.raise(level, c, RitualDome.RADIUS * Math.min(1.5f, power), RitualDome.LIFE, allies, power);
            case STORM -> StormCloud.gather(level, c, StormCloud.RADIUS, (int) (StormCloud.LIFE * power), allies, power);
            case PORTAL -> portal(level, circle);
            case BLESSING -> blessing(level, c, power);
            case COLOSSUS -> StoneColossus.raise(level, c, circle.getOwnerUUID(), allies, power);
            case BRIDGE -> AmethystBridge.raise(level, bridgeStart(circle), lookTarget(level, circle, 56, 40), power);
        }
    }

    /** Where the bridge sets out from: the edge of the figure, the way the leader looks. */
    private static Vec3 bridgeStart(RitualCircle circle) {
        Vec3 to = circle.getOwner() != null ? circle.getOwner().getLookAngle() : Vec3.directionFromRotation(0, circle.getYRot());
        Vec3 flat = new Vec3(to.x, 0, to.z);
        flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
        return circle.position().add(flat.scale(RitualCircle.RADIUS + 1));
    }

    /** Where the leader looks (up to {@code range} blocks away), else {@code ahead} blocks ahead of the figure at its height. */
    private static Vec3 lookTarget(ServerLevel level, RitualCircle circle, double range, double ahead) {
        Entity leader = circle.getOwner();
        if (leader != null && leader.distanceToSqr(circle) < 40 * 40) {
            Vec3 eye = leader.getEyePosition();
            Vec3 end = eye.add(leader.getLookAngle().scale(range));
            HitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, leader));
            return hit.getType() == HitResult.Type.MISS ? new Vec3(end.x, circle.getY(), end.z) : hit.getLocation();
        }
        return circle.position().add(Vec3.directionFromRotation(0, circle.getYRot()).scale(ahead));
    }

    /** Where the leader looks (up to 64 blocks away), else 16 blocks ahead of the circle. */
    private static Vec3 meteorTarget(ServerLevel level, RitualCircle circle) {
        Entity leader = circle.getOwner();
        if (leader != null && leader.distanceToSqr(circle) < 40 * 40) {
            Vec3 eye = leader.getEyePosition();
            Vec3 end = eye.add(leader.getLookAngle().scale(64));
            HitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, leader));
            if (hit.getType() != HitResult.Type.MISS) {
                return hit.getLocation();
            }
        }
        Vec3 ahead = Vec3.directionFromRotation(0, circle.getYRot()).scale(16);
        return SpellUtil.groundAt(level, circle.position().add(ahead), 12, 1);
    }

    private static void portal(ServerLevel level, RitualCircle circle) {
        Vec3 to = circle.getAnchor();
        if (to == null) {
            return;
        }
        ServerLevel toLevel = level;
        if (circle.getAnchorLevel() != null) {
            ServerLevel other = level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, circle.getAnchorLevel()));
            if (other != null) {
                toLevel = other;
            }
        }
        // Drawn on a wall, it opens in the wall; else it stands over the figure
        Direction wall = circle.getWall();
        float yaw = wall != null ? wall.toYRot() : circle.getYRot();
        Vec3 at = wall != null ? circle.position().add(Vec3.atLowerCornerOf(wall.getNormal()).scale(.3)) : circle.position();
        RitualPortal.open(level, at, yaw, toLevel, to, RitualPortal.LIFE);
    }

    /** Every player around is healed, cleansed and strengthened for five minutes. */
    public static void blessing(ServerLevel level, Vec3 c, float power) {
        int amplifier = power >= 1.6f ? 1 : 0;
        for (Player player : level.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(c, c).inflate(BLESSING_RADIUS))) {
            if (player.distanceToSqr(c) > BLESSING_RADIUS * BLESSING_RADIUS) {
                continue;
            }
            for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
                if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                    player.removeEffect(effect.getEffect());
                }
            }
            player.setHealth(player.getMaxHealth());
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 60, 1));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, BLESSING_TICKS, 1 + amplifier));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, BLESSING_TICKS, amplifier));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, BLESSING_TICKS, amplifier));
            SpellUtil.particles(level, ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(), 30, .4, .8, .4, .3);
        }
        SpellUtil.particles(level, ParticleTypes.END_ROD, c.x, c.y + 3, c.z, 120, 2, 6, 2, .05);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.5f, .8f);
    }
}
