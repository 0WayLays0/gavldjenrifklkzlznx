package com.waylays.arcanes.network;

import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * The addon's own packets. So far one: a fighter's body animation played outside of a cast (the riposte that answers
 * a parried blow), for everyone who sees them.
 */
public final class ArcanesNetwork {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ArcanesOublies.id("main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private ArcanesNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(PlayAnimationPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PlayAnimationPacket::encode)
                .decoder(PlayAnimationPacket::decode)
                .consumerMainThread(PlayAnimationPacket::handle)
                .add();
    }

    /** Plays the player animation {@code animation} on {@code player}, for them and everyone around. */
    public static void playAnimation(ServerPlayer player, ResourceLocation animation) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new PlayAnimationPacket(player.getId(), animation));
    }
}
