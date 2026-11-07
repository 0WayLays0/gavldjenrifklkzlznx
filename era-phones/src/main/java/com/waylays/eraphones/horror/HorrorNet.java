package com.waylays.eraphones.horror;

import com.waylays.eraphones.EraPhones;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** What a game master sends one player's senses: a whisper in the head, fog, the lights going out, a red balloon... */
public final class HorrorNet {
    private HorrorNet() {}

    public static void register(PayloadRegistrar r) {
        r.playToClient(Effect.TYPE, Effect.CODEC, (p, c) -> com.waylays.eraphones.horror.client.HorrorClient.effect(p));
    }

    /**
     * The effect {@code kind} ("murmure", "brouillard", "coupure", "sang", "ballon", "tremblement", or "stop"
     * to end them all) for {@code ticks}, with its words if it has any, at a place if it has one.
     */
    public record Effect(String kind, int ticks, String text, double x, double y, double z) implements CustomPacketPayload {
        public static final Type<Effect> TYPE = new Type<>(EraPhones.id("horror_effect"));

        public Effect {
            kind = kind.length() > 32 ? kind.substring(0, 32) : kind;
            text = text == null ? "" : text.length() > 512 ? text.substring(0, 512) : text;
        }

        public static final StreamCodec<FriendlyByteBuf, Effect> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeUtf(p.kind, 32);
            b.writeVarInt(p.ticks);
            b.writeUtf(p.text, 512);
            b.writeDouble(p.x);
            b.writeDouble(p.y);
            b.writeDouble(p.z);
        }, b -> new Effect(b.readUtf(32), b.readVarInt(), b.readUtf(512), b.readDouble(), b.readDouble(), b.readDouble()));

        @Override
        public Type<Effect> type() {
            return TYPE;
        }
    }
}
