package com.waylays.arcanes.spells.gold;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.effect.MidasCurseEffect;
import com.waylays.arcanes.network.GoldCoatPacket;
import com.waylays.arcanes.registry.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The gold that Gold magic lays on a body, as everyone around sees it: a Midas Curse turning it to gold from the feet up
 * into a statue, Gilded Armor's shining shell. The server tells the clients that see the body (see
 * {@link GoldCoatPacket}); each client draws the gold on the body's own model.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public final class GoldCoat {
    /** Bodies whose gold changed this tick: told once at the end of it, when every effect has settled. */
    private static final Set<LivingEntity> PENDING = new LinkedHashSet<>();

    private GoldCoat() {}

    public static void syncLater(LivingEntity entity) {
        if (!entity.level().isClientSide) {
            PENDING.add(entity);
        }
    }

    /** The effects the client draws as gold on the body. */
    private static boolean drawn(@Nullable MobEffectInstance effect) {
        return effect != null && (effect.is(ModEffects.MIDAS_CURSE) || effect.is(ModEffects.GILDED_ARMOR));
    }

    private static boolean gilded(LivingEntity entity) {
        return entity.hasEffect(ModEffects.MIDAS_CURSE) || entity.hasEffect(ModEffects.GILDED_ARMOR);
    }

    private static GoldCoatPacket state(LivingEntity entity) {
        long midasStart = 0;
        int midasDuration = 0;
        if (entity.hasEffect(ModEffects.MIDAS_CURSE)) {
            midasStart = MidasCurseEffect.start(entity);
            midasDuration = MidasCurseEffect.total(entity);
        }
        boolean armor = entity.hasEffect(ModEffects.GILDED_ARMOR);
        return new GoldCoatPacket(entity.getId(), midasStart, midasDuration, armor);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) {
            return;
        }
        for (LivingEntity entity : PENDING) {
            if (entity.level() instanceof ServerLevel && !entity.isRemoved()) {
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, state(entity));
            }
        }
        PENDING.clear();
    }

    /** Whoever comes to see a body in gold is told of it. */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        Entity target = event.getTarget();
        if (target instanceof LivingEntity living && event.getEntity() instanceof ServerPlayer player && gilded(living)) {
            PacketDistributor.sendToPlayer(player, state(living));
        }
    }

    @SubscribeEvent
    public static void onAdded(MobEffectEvent.Added event) {
        if (drawn(event.getEffectInstance())) {
            syncLater(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onRemoved(MobEffectEvent.Remove event) {
        if (drawn(event.getEffectInstance())) {
            syncLater(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onExpired(MobEffectEvent.Expired event) {
        if (drawn(event.getEffectInstance())) {
            syncLater(event.getEntity());
        }
    }
}
