package com.waylays.arcanes.rp;

import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** The role play's packets: the characters' names (for the names over heads), and a bubble over someone's head. */
public final class RpNet {
    private RpNet() {}

    public static void register(PayloadRegistrar r) {
        r.playToClient(Names.TYPE, Names.CODEC, (p, c) -> com.waylays.arcanes.rp.client.RpClient.names(p));
        r.playToClient(Bubble.TYPE, Bubble.CODEC, (p, c) -> com.waylays.arcanes.rp.client.RpClient.bubble(p));
        r.playToClient(Nameplates.TYPE, Nameplates.CODEC, (p, c) -> com.waylays.arcanes.rp.client.RpClient.nameplates(p));
        com.waylays.arcanes.rp.horror.HorrorNet.register(r);
        com.waylays.arcanes.rp.arms.Arms.register(r);
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payload(String name) {
        return new CustomPacketPayload.Type<>(ArcanesOublies.id("rp_" + name));
    }

    /** Characters' names by player ({@code replace}: the whole list, else these to add or change). */
    public record Names(Map<UUID, String> names, boolean replace) implements CustomPacketPayload {
        public static final Type<Names> TYPE = payload("names");
        public static final StreamCodec<FriendlyByteBuf, Names> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeBoolean(p.replace);
            b.writeVarInt(p.names.size());
            p.names.forEach((k, v) -> {
                b.writeUUID(k);
                b.writeUtf(v, 64);
            });
        }, b -> {
            boolean replace = b.readBoolean();
            int n = Math.min(b.readVarInt(), 4096);
            Map<UUID, String> m = new HashMap<>();
            for (int i = 0; i < n; i++) {
                m.put(b.readUUID(), b.readUtf(64));
            }
            return new Names(m, replace);
        });

        @Override
        public Type<Names> type() {
            return TYPE;
        }
    }

    /** Whether the names over the heads are hidden. */
    public record Nameplates(boolean hidden) implements CustomPacketPayload {
        public static final Type<Nameplates> TYPE = payload("nameplates");
        public static final StreamCodec<FriendlyByteBuf, Nameplates> CODEC = CustomPacketPayload.codec((p, b) -> b.writeBoolean(p.hidden),
                b -> new Nameplates(b.readBoolean()));

        @Override
        public Type<Nameplates> type() {
            return TYPE;
        }
    }

    /** Words or an action over the head of the entity {@code entity}, in {@code color}, for a few seconds. */
    public record Bubble(int entity, String text, int color) implements CustomPacketPayload {
        public static final Type<Bubble> TYPE = payload("bubble");
        public static final StreamCodec<FriendlyByteBuf, Bubble> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeVarInt(p.entity);
            b.writeUtf(p.text, 300);
            b.writeInt(p.color);
        }, b -> new Bubble(b.readVarInt(), b.readUtf(300), b.readInt()));

        @Override
        public Type<Bubble> type() {
            return TYPE;
        }
    }
}
