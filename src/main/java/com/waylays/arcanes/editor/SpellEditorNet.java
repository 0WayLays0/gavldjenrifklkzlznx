package com.waylays.arcanes.editor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The spell editor's packets: the server tells every client the editor's own stats (they are written into the spells on
 * the clients too, so tooltips and casting agree) and opens the editor for a game master; the editor sends back what
 * was changed, for one spell or for many at once.
 */
public final class SpellEditorNet {
    private static final int MAX_JSON = 1 << 20;

    private SpellEditorNet() {}

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Sync.TYPE, Sync.CODEC, Sync::handle);
        registrar.playToClient(Open.TYPE, Open.CODEC, Open::handle);
        registrar.playToClient(Saved.TYPE, Saved.CODEC, Saved::handle);
        registrar.playToServer(Edit.TYPE, Edit.CODEC, Edit::handle);
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payload(String name) {
        return new CustomPacketPayload.Type<>(ArcanesOublies.id("spell_editor_" + name));
    }

    /** Every spell's editor stats, as the file has them. */
    public record Sync(String json) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = payload("sync");
        public static final StreamCodec<FriendlyByteBuf, Sync> CODEC = CustomPacketPayload.codec(Sync::write, Sync::new);

        Sync(FriendlyByteBuf buf) {
            this(buf.readUtf(MAX_JSON));
        }

        void write(FriendlyByteBuf buf) {
            buf.writeUtf(json, MAX_JSON);
        }

        public static Sync now() {
            return new Sync(SpellTuning.toJson(SpellTuning.all()).toString());
        }

        @Override
        public Type<Sync> type() {
            return TYPE;
        }

        void handle(IPayloadContext context) {
            context.enqueueWork(() -> {
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                SpellTuning.replace(SpellTuning.fromJson(root));
                com.waylays.arcanes.editor.client.SpellEditorClient.synced();
            });
        }
    }

    /** Opens the editor. */
    public record Open() implements CustomPacketPayload {
        public static final Type<Open> TYPE = payload("open");
        public static final StreamCodec<FriendlyByteBuf, Open> CODEC = StreamCodec.unit(new Open());

        @Override
        public Type<Open> type() {
            return TYPE;
        }

        void handle(IPayloadContext context) {
            context.enqueueWork(com.waylays.arcanes.editor.client.SpellEditorClient::open);
        }
    }

    /** What became of an edit: how many spells were changed, and the parameters that could not be read (if any). */
    public record Saved(int spells, List<String> errors) implements CustomPacketPayload {
        public static final Type<Saved> TYPE = payload("saved");
        public static final StreamCodec<FriendlyByteBuf, Saved> CODEC = CustomPacketPayload.codec(Saved::write, Saved::new);

        Saved(FriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readList(b -> b.readUtf(512)));
        }

        void write(FriendlyByteBuf buf) {
            buf.writeVarInt(spells);
            buf.writeCollection(errors.size() > 20 ? errors.subList(0, 20) : errors, (b, s) -> b.writeUtf(s.length() > 512 ? s.substring(0, 512) : s, 512));
        }

        @Override
        public Type<Saved> type() {
            return TYPE;
        }

        void handle(IPayloadContext context) {
            context.enqueueWork(() -> com.waylays.arcanes.editor.client.SpellEditorClient.saved(spells, errors));
        }
    }

    /**
     * A game master's changes, for each of {@code spells}: Iron's config parameters ({@code iron}: the parameter's key and
     * its value as JSON, an empty string to go back to the spell's default) and the editor's own stats ({@code tune}: the
     * stat's key and its value, NaN to go back to the spell's own). With {@code reset}, everything is taken off first.
     */
    public record Edit(List<ResourceLocation> spells, Map<String, String> iron, Map<String, Double> tune, boolean reset)
            implements CustomPacketPayload {
        public static final Type<Edit> TYPE = payload("edit");
        public static final StreamCodec<FriendlyByteBuf, Edit> CODEC = CustomPacketPayload.codec(Edit::write, Edit::new);
        /** At most this many spells in one packet (a client's packets to the server stay small). */
        public static final int MAX_SPELLS = 400;

        Edit(FriendlyByteBuf buf) {
            this(readSpells(buf), readIron(buf), readTune(buf), buf.readBoolean());
        }

        private static List<ResourceLocation> readSpells(FriendlyByteBuf buf) {
            int n = Math.min(buf.readVarInt(), MAX_SPELLS);
            List<ResourceLocation> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                list.add(buf.readResourceLocation());
            }
            return list;
        }

        private static Map<String, String> readIron(FriendlyByteBuf buf) {
            int n = Math.min(buf.readVarInt(), 256);
            Map<String, String> map = new LinkedHashMap<>();
            for (int i = 0; i < n; i++) {
                map.put(buf.readUtf(256), buf.readUtf(4096));
            }
            return map;
        }

        private static Map<String, Double> readTune(FriendlyByteBuf buf) {
            int n = Math.min(buf.readVarInt(), 64);
            Map<String, Double> map = new LinkedHashMap<>();
            for (int i = 0; i < n; i++) {
                map.put(buf.readUtf(64), buf.readDouble());
            }
            return map;
        }

        void write(FriendlyByteBuf buf) {
            List<ResourceLocation> list = spells.size() > MAX_SPELLS ? spells.subList(0, MAX_SPELLS) : spells;
            buf.writeVarInt(list.size());
            list.forEach(buf::writeResourceLocation);
            buf.writeVarInt(iron.size());
            iron.forEach((k, v) -> {
                buf.writeUtf(k, 256);
                buf.writeUtf(v, 4096);
            });
            buf.writeVarInt(tune.size());
            tune.forEach((k, v) -> {
                buf.writeUtf(k, 64);
                buf.writeDouble(v);
            });
            buf.writeBoolean(reset);
        }

        @Override
        public Type<Edit> type() {
            return TYPE;
        }

        void handle(IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) {
                context.enqueueWork(() -> SpellEditorServer.edit(player, this));
            }
        }
    }
}
