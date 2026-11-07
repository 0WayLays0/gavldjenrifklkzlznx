package com.waylays.eraphones.gear;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.gear.block.RadioBlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The radio: the stations on the air (a game master speaking on a frequency, /radio emettre), and the radios in the
 * world that are loaded, so a voice on the air reaches each one switched on to its frequency. Frequencies are in
 * tenths of a megahertz (88.0 to 108.0 FM: 880 to 1080).
 */
@EventBusSubscriber(modid = EraPhones.MODID)
public final class Stations {
    public static final int MIN = 880, MAX = 1080;
    /** Who is on the air, on what frequency. */
    private static final Map<UUID, Integer> ON_AIR = new ConcurrentHashMap<>();
    /** The radios loaded in the world. */
    public static final Set<RadioBlockEntity> RADIOS = ConcurrentHashMap.newKeySet();

    private Stations() {}

    public static Integer frequency(UUID speaker) {
        return ON_AIR.get(speaker);
    }

    /** {@code speaker} goes on the air on {@code freq}, or off it (null). */
    public static void set(MinecraftServer server, UUID speaker, Integer freq) {
        if (freq == null) {
            ON_AIR.remove(speaker);
        } else {
            ON_AIR.put(speaker, freq);
        }
        PacketDistributor.sendToAllPlayers(packet());
    }

    /** Whether a radio set to {@code freq} picks up something (a station within a tenth). */
    public static boolean tuned(int radio, int station) {
        return Math.abs(radio - station) <= 1;
    }

    public static GearNet.Stations packet() {
        return new GearNet.Stations(new ArrayList<>(ON_AIR.values()));
    }

    public static String format(int freq) {
        return String.format("%d.%d", freq / 10, freq % 10);
    }

    public static List<RadioBlockEntity> radios(int freq) {
        List<RadioBlockEntity> out = new ArrayList<>();
        for (RadioBlockEntity r : RADIOS) {
            if (!r.isRemoved() && r.on() && tuned(r.frequency(), freq)) {
                out.add(r);
            }
        }
        return out;
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            PacketDistributor.sendToPlayer(p, packet());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (ON_AIR.remove(event.getEntity().getUUID()) != null) {
            PacketDistributor.sendToAllPlayers(packet());
        }
    }
}
