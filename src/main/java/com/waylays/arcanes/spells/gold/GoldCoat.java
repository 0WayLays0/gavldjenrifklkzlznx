package com.waylays.arcanes.spells.gold;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.effect.GildedEffect;
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
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The coat of gold that Gold magic lays on a body, as everyone around sees it: the coats of Gilding climbing from its
 * feet, a Midas Curse turning it to gold from the feet up into a statue, Gilded Armor's shining shell. The server tells
 * the clients that see the body (see {@link GoldCoatPacket}); each client draws the gold on the body's own model.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public final class GoldCoat {
    /** Bodies whose coat changed this tick: told once at the end of it, when every effect has settled. */
    private static final Set<LivingEntity> PENDING = new LinkedHashSet<>();

    private static final String LAST_TRICKLE = "arcanes_oublies.last_trickle";

    private GoldCoat() {}

    /** Lays {@code coats} more coats of gold on {@code target} (up to five). Returns the coats it now wears. */
    public static int gild(LivingEntity target, int coats) {
        if (target.level().isClientSide || !target.isAlive() || coats <= 0) {
            return GildedEffect.coats(target);
        }
        int now = Math.min(GildedEffect.MAX_COATS, GildedEffect.coats(target) + coats);
        target.addEffect(new MobEffectInstance(ModEffects.GILDED, 160 + 20 * now, now - 1, false, true, true));
        syncLater(target);
        return now;
    }

    /**
     * One more coat of gold on {@code target}, unless gold that trickles on (a stream, a pool) gave it one less than
     * {@code every} ticks ago. Returns the coats it now wears.
     */
    public static int gildEvery(LivingEntity target, int every) {
        long now = target.level().getGameTime();
        long last = target.getPersistentData().getLong(LAST_TRICKLE);
        if (now - last < every && now >= last) {
            return GildedEffect.coats(target);
        }
        target.getPersistentData().putLong(LAST_TRICKLE, now);
        return gild(target, 1);
    }

    public static void syncLater(LivingEntity entity) {
        if (!entity.level().isClientSide) {
            PENDING.add(entity);
        }
    }

    @Nullable
    private static GoldCoatPacket state(LivingEntity entity) {
        int coats = GildedEffect.coats(entity);
        long midasStart = 0;
        int midasDuration = 0;
        if (entity.hasEffect(ModEffects.MIDAS_CURSE)) {
            midasStart = MidasCurseEffect.start(entity);
            midasDuration = MidasCurseEffect.total(entity);
        }
        boolean armor = entity.hasEffect(ModEffects.GILDED_ARMOR);
        return new GoldCoatPacket(entity.getId(), coats, midasStart, midasDuration, armor);
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

    /** Whoever comes to see a gilded body is told how gilded it is. */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        Entity target = event.getTarget();
        if (target instanceof LivingEntity living && event.getEntity() instanceof ServerPlayer player
                && (living.hasEffect(ModEffects.GILDED) || living.hasEffect(ModEffects.MIDAS_CURSE) || living.hasEffect(ModEffects.GILDED_ARMOR))) {
            PacketDistributor.sendToPlayer(player, state(living));
        }
    }
}
