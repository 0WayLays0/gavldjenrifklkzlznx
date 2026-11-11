package com.waylays.eraphones.compat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;

/** Sending the mod's packets, as NeoForge 1.21 names it. */
public final class PacketDistributor {
    private PacketDistributor() {}

    private static Net.Envelope wrap(CustomPacketPayload p) {
        return new Net.Envelope(p);
    }

    public static void sendToServer(CustomPacketPayload payload, CustomPacketPayload... more) {
        Net.CHANNEL.sendToServer(wrap(payload));
        for (CustomPacketPayload p : more) {
            Net.CHANNEL.sendToServer(wrap(p));
        }
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload, CustomPacketPayload... more) {
        if (player.connection == null) {
            return;
        }
        Net.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), wrap(payload));
        for (CustomPacketPayload p : more) {
            Net.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), wrap(p));
        }
    }

    public static void sendToAllPlayers(CustomPacketPayload payload, CustomPacketPayload... more) {
        if (ServerLifecycleHooks.getCurrentServer() == null) {
            return;
        }
        for (ServerPlayer p : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            sendToPlayer(p, payload, more);
        }
    }

    public static void sendToPlayersInDimension(ServerLevel level, CustomPacketPayload payload, CustomPacketPayload... more) {
        for (ServerPlayer p : level.players()) {
            sendToPlayer(p, payload, more);
        }
    }

    public static void sendToPlayersNear(ServerLevel level, @Nullable ServerPlayer excluded, double x, double y, double z, double radius,
                                         CustomPacketPayload payload, CustomPacketPayload... more) {
        for (ServerPlayer p : level.players()) {
            if (p != excluded && p.distanceToSqr(x, y, z) <= radius * radius) {
                sendToPlayer(p, payload, more);
            }
        }
    }

    public static void sendToPlayersTrackingEntity(Entity entity, CustomPacketPayload payload, CustomPacketPayload... more) {
        Net.CHANNEL.send(net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY.with(() -> entity), wrap(payload));
        for (CustomPacketPayload p : more) {
            Net.CHANNEL.send(net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY.with(() -> entity), wrap(p));
        }
    }

    public static void sendToPlayersTrackingEntityAndSelf(Entity entity, CustomPacketPayload payload, CustomPacketPayload... more) {
        Net.CHANNEL.send(net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), wrap(payload));
        for (CustomPacketPayload p : more) {
            Net.CHANNEL.send(net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), wrap(p));
        }
    }
}
