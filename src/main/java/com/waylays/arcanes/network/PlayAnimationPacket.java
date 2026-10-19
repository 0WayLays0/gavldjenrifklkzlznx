package com.waylays.arcanes.network;

import com.waylays.arcanes.client.ClientAnimations;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server to client: play a player animation on the player with this entity id. */
public record PlayAnimationPacket(int entityId, ResourceLocation animation) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeResourceLocation(animation);
    }

    public static PlayAnimationPacket decode(FriendlyByteBuf buf) {
        return new PlayAnimationPacket(buf.readVarInt(), buf.readResourceLocation());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientAnimations.play(entityId, animation));
    }
}
