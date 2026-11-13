package com.waylays.arcanes.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The addon's own packets: a fighter's body animation played outside of a cast (the riposte that answers a parried
 * blow), for everyone who sees them; and how much gold covers a body (Gold magic's coats, Midas statues).
 */
public final class ArcanesNetwork {
    private static final String VERSION = "1";

    private ArcanesNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToClient(PlayAnimationPacket.TYPE, PlayAnimationPacket.STREAM_CODEC, PlayAnimationPacket::handle);
        registrar.playToClient(GoldCoatPacket.TYPE, GoldCoatPacket.STREAM_CODEC, GoldCoatPacket::handle);
        registrar.playToServer(AltarRitualPacket.TYPE, AltarRitualPacket.STREAM_CODEC, AltarRitualPacket::handle);
        MusicPackets.register(registrar);
        com.waylays.arcanes.editor.SpellEditorNet.register(registrar);
        com.waylays.arcanes.metier.MetierNet.register(registrar);
    }

    /** Plays the player animation {@code animation} on {@code player}, for them and everyone around. */
    public static void playAnimation(ServerPlayer player, ResourceLocation animation) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new PlayAnimationPacket(player.getId(), animation));
    }
}
