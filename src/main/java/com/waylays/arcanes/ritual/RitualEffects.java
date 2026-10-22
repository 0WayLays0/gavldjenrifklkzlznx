package com.waylays.arcanes.ritual;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.ritual.Meteor;
import com.waylays.arcanes.entity.ritual.RitualDome;
import com.waylays.arcanes.entity.ritual.RitualPortal;
import com.waylays.arcanes.entity.ritual.StormCloud;
import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/** What each ritual does once it is cast. */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public final class RitualEffects {
    public static final float BLESSING_RADIUS = 24;
    public static final int BLESSING_TICKS = 20 * 60 * 5;
    public static final int COLOSSUS_TICKS = 20 * 60 * 3;
    public static final float COLOSSUS_SCALE = 2.5f;
    private static final String COLOSSUS_UNTIL = "arcanes_oublies.colossus_until";

    private RitualEffects() {}

    public static void manifest(ServerLevel level, RitualCircle circle, Set<UUID> allies, float power) {
        Vec3 c = circle.position();
        switch (circle.getRitual()) {
            case METEOR -> Meteor.fall(level, meteorTarget(level, circle), allies, power);
            case DOME -> RitualDome.raise(level, c, RitualDome.RADIUS * Math.min(1.5f, power), RitualDome.LIFE, allies, power);
            case STORM -> StormCloud.gather(level, c, StormCloud.RADIUS, (int) (StormCloud.LIFE * power), allies, power);
            case PORTAL -> portal(level, circle);
            case BLESSING -> blessing(level, c, power);
            case COLOSSUS -> colossus(level, c, power);
        }
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
        RitualPortal.open(level, circle.position(), toLevel, to, RitualPortal.LIFE);
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

    /** A colossus of stone (a giant iron golem made by the players) wakes and fights for them for three minutes. */
    public static IronGolem colossus(ServerLevel level, Vec3 c, float power) {
        IronGolem golem = EntityType.IRON_GOLEM.create(level);
        if (golem == null) {
            return null;
        }
        golem.moveTo(c.x, c.y, c.z, level.random.nextFloat() * 360, 0);
        golem.setPlayerCreated(true);
        golem.getAttribute(Attributes.SCALE).setBaseValue(COLOSSUS_SCALE);
        golem.getAttribute(Attributes.MAX_HEALTH).setBaseValue(300 * power);
        golem.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(25 * power);
        golem.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        golem.setHealth(golem.getMaxHealth());
        golem.setCustomName(Component.translatable("entity.arcanes_oublies.colossus"));
        golem.getPersistentData().putLong(COLOSSUS_UNTIL, level.getGameTime() + COLOSSUS_TICKS);
        level.addFreshEntity(golem);
        SpellUtil.particles(level, ParticleTypes.EXPLOSION, c.x, c.y + 2, c.z, 8, 1.5, 2, 1.5, 0);
        SpellUtil.particles(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, c.x, c.y + 1, c.z, 40, 2, 1, 2, .03);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 3f, .4f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 2f, .4f);
        return golem;
    }

    /** The colossus crumbles when its time is up. */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof IronGolem) || !(entity.level() instanceof ServerLevel level) || entity.tickCount % 20 != 0) {
            return;
        }
        long until = entity.getPersistentData().getLong(COLOSSUS_UNTIL);
        if (until > 0 && level.getGameTime() > until) {
            SpellUtil.particles(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, entity.getX(), entity.getY() + 2, entity.getZ(), 40, 1.5, 2, 1.5, .03);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.IRON_GOLEM_DEATH, SoundSource.PLAYERS, 2f, .5f);
            entity.discard();
        }
    }
}
