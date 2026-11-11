package com.waylays.eraphones.horror;

import com.waylays.eraphones.EraPhones;
import net.minecraft.network.FriendlyByteBuf;
import com.waylays.eraphones.compat.StreamCodec;
import com.waylays.eraphones.compat.CustomPacketPayload;
import com.waylays.eraphones.compat.PayloadRegistrar;

/** What a game master sends one player's senses: a whisper in the head, fog, the lights going out, a red balloon... */
public final class HorrorNet {
    private HorrorNet() {}

    public static void register(PayloadRegistrar r) {
        r.playToClient(Effect.TYPE, Effect.CODEC, (p, c) -> com.waylays.eraphones.horror.client.HorrorClient.effect(p));
        r.playToClient(Gauge.TYPE, Gauge.CODEC, (p, c) -> com.waylays.eraphones.horror.client.HorrorClient.gauge(p.level()));
        r.playToClient(OpenRemote.TYPE, OpenRemote.CODEC, (p, c) -> com.waylays.eraphones.horror.client.HorrorClient.openRemote(p));
        r.playToServer(Remote.TYPE, Remote.CODEC, (p, c) -> {
            if (!(c.player() instanceof net.minecraft.server.level.ServerPlayer gm) || !RemoteItem.allowed(gm)) {
                return;
            }
            java.util.List<net.minecraft.server.level.ServerPlayer> t = new java.util.ArrayList<>();
            for (String id : p.targets()) {
                try {
                    net.minecraft.server.level.ServerPlayer sp = gm.server.getPlayerList().getPlayer(java.util.UUID.fromString(id));
                    if (sp != null) {
                        t.add(sp);
                    }
                } catch (IllegalArgumentException ignored) {
                    // Not a player's id
                }
            }
            RemoteItem.act(gm, t, p.action(), p.arg());
        });
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

    /** To a game master: the players about (their ids and how they are named) for the remote's board. */
    public record OpenRemote(java.util.List<String> ids, java.util.List<String> names) implements CustomPacketPayload {
        public static final Type<OpenRemote> TYPE = new Type<>(EraPhones.id("horror_remote_open"));
        public static final StreamCodec<FriendlyByteBuf, OpenRemote> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeVarInt(p.ids.size());
            for (int i = 0; i < p.ids.size(); i++) {
                b.writeUtf(p.ids.get(i), 64);
                b.writeUtf(p.names.get(i), 64);
            }
        }, b -> {
            int n = Math.min(256, b.readVarInt());
            java.util.List<String> ids = new java.util.ArrayList<>(), names = new java.util.ArrayList<>();
            for (int i = 0; i < n; i++) {
                ids.add(b.readUtf(64));
                names.add(b.readUtf(64));
            }
            return new OpenRemote(ids, names);
        });

        @Override
        public Type<OpenRemote> type() {
            return TYPE;
        }
    }

    /** From a game master's remote: do {@code action} (with {@code arg}, its words) to the players {@code targets}. */
    public record Remote(java.util.List<String> targets, String action, String arg) implements CustomPacketPayload {
        public static final Type<Remote> TYPE = new Type<>(EraPhones.id("horror_remote"));
        public static final StreamCodec<FriendlyByteBuf, Remote> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeVarInt(p.targets.size());
            for (String t : p.targets) {
                b.writeUtf(t, 64);
            }
            b.writeUtf(p.action, 32);
            b.writeUtf(p.arg, 512);
        }, b -> {
            int n = Math.min(256, b.readVarInt());
            java.util.List<String> t = new java.util.ArrayList<>();
            for (int i = 0; i < n; i++) {
                t.add(b.readUtf(64));
            }
            return new Remote(t, b.readUtf(32), b.readUtf(512));
        });

        public Remote {
            action = action.length() > 32 ? action.substring(0, 32) : action;
            arg = arg == null ? "" : arg.length() > 512 ? arg.substring(0, 512) : arg;
        }

        @Override
        public Type<Remote> type() {
            return TYPE;
        }
    }

    /** The fear gauge's level for its player (0 to 100). */
    public record Gauge(int level) implements CustomPacketPayload {
        public static final Type<Gauge> TYPE = new Type<>(EraPhones.id("fear_gauge"));
        public static final StreamCodec<FriendlyByteBuf, Gauge> CODEC = CustomPacketPayload.codec((p, b) -> b.writeVarInt(p.level),
                b -> new Gauge(Math.max(0, Math.min(100, b.readVarInt()))));

        @Override
        public Type<Gauge> type() {
            return TYPE;
        }
    }
}
