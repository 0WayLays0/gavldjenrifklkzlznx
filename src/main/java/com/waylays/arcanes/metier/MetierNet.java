package com.waylays.arcanes.metier;

import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

/**
 * The trades' packets: the server starts a minigame on a client (what game, how hard, what is at stake), the client
 * sends back how it went, the server says what came of it; a station's recipe list and the recipe chosen; the editor's
 * trades, sent down whole and back up in pieces.
 */
public final class MetierNet {
    private MetierNet() {}

    public static void register(PayloadRegistrar r) {
        r.playToClient(Start.TYPE, Start.CODEC, Start::handle);
        r.playToClient(Outcome.TYPE, Outcome.CODEC, Outcome::handle);
        r.playToClient(Stop.TYPE, Stop.CODEC, Stop::handle);
        r.playToClient(Station.TYPE, Station.CODEC, Station::handle);
        r.playToClient(Editor.TYPE, Editor.CODEC, Editor::handle);
        r.playToServer(Result.TYPE, Result.CODEC, Result::handle);
        r.playToServer(Pick.TYPE, Pick.CODEC, Pick::handle);
        r.playToServer(Save.TYPE, Save.CODEC, Save::handle);
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payload(String name) {
        return new CustomPacketPayload.Type<>(ArcanesOublies.id("metier_" + name));
    }

    private static void writeStacks(RegistryFriendlyByteBuf buf, List<ItemStack> stacks) {
        buf.writeVarInt(stacks.size());
        for (ItemStack s : stacks) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, s);
        }
    }

    private static List<ItemStack> readStacks(RegistryFriendlyByteBuf buf) {
        int n = Math.min(buf.readVarInt(), 64);
        List<ItemStack> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }
        return out;
    }

    // ---------------------------------------------------------------- server to client

    /**
     * A minigame starts: {@code session}, the {@code game} (a {@link MetierDefs.Game} ordinal), how hard it is
     * ({@code difficulty}, 1 normal), a {@code seed}, its {@code title} and {@code subtitle}, what is worked
     * ({@code subject}: the block or the recipe's result), the {@code tool} used and the {@code inputs} (a recipe's).
     */
    public record Start(int session, int game, double difficulty, long seed, String title, String subtitle, ItemStack subject, ItemStack tool,
                        List<ItemStack> inputs, float perfectAt, float criticalBelow) implements CustomPacketPayload {
        public static final Type<Start> TYPE = payload("start");
        public static final StreamCodec<RegistryFriendlyByteBuf, Start> CODEC = CustomPacketPayload.codec(Start::write, Start::new);

        Start(RegistryFriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readVarInt(), buf.readDouble(), buf.readLong(), buf.readUtf(128), buf.readUtf(128),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), readStacks(buf), buf.readFloat(), buf.readFloat());
        }

        void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(session);
            buf.writeVarInt(game);
            buf.writeDouble(difficulty);
            buf.writeLong(seed);
            buf.writeUtf(title, 128);
            buf.writeUtf(subtitle, 128);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, subject);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, tool);
            writeStacks(buf, inputs);
            buf.writeFloat(perfectAt);
            buf.writeFloat(criticalBelow);
        }

        @Override
        public Type<Start> type() {
            return TYPE;
        }

        void handle(IPayloadContext context) {
            context.enqueueWork(() -> com.waylays.arcanes.client.metier.MetierClient.start(this));
        }
    }

    /** What came of it: the {@code outcome} ({@link MetierDefs.Outcome} ordinal), the items given, the durability lost. */
    public record Outcome(int session, int outcome, List<ItemStack> items, int durability) implements CustomPacketPayload {
        public static final Type<Outcome> TYPE = payload("outcome");
        public static final StreamCodec<RegistryFriendlyByteBuf, Outcome> CODEC = CustomPacketPayload.codec(Outcome::write, Outcome::new);

        Outcome(RegistryFriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readVarInt(), readStacks(buf), buf.readVarInt());
        }

        void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(session);
            buf.writeVarInt(outcome);
            writeStacks(buf, items);
            buf.writeVarInt(durability);
        }

        @Override
        public Type<Outcome> type() {
            return TYPE;
        }

        void handle(IPayloadContext context) {
            context.enqueueWork(() -> com.waylays.arcanes.client.metier.MetierClient.outcome(this));
        }
    }

    /** The minigame is called off by the server ({@code reason}: a translation key). */
    public record Stop(int session, String reason) implements CustomPacketPayload {
        public static final Type<Stop> TYPE = payload("stop");
        public static final StreamCodec<RegistryFriendlyByteBuf, Stop> CODEC = CustomPacketPayload.codec(Stop::write, Stop::new);

        Stop(RegistryFriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readUtf(128));
        }

        void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(session);
            buf.writeUtf(reason, 128);
        }

        @Override
        public Type<Stop> type() {
            return TYPE;
        }

        void handle(IPayloadContext context) {
            context.enqueueWork(() -> com.waylays.arcanes.client.metier.MetierClient.stop(this));
        }
    }

    /** A recipe as a station lists it: what it takes (and whether the player has it), what it gives, its wait. */
    public record RecipeView(String name, List<ItemStack> inputs, List<Boolean> have, ItemStack partial, ItemStack perfect, double difficulty,
                             int cooldown) {
        static RecipeView read(RegistryFriendlyByteBuf buf) {
            String name = buf.readUtf(128);
            List<ItemStack> inputs = readStacks(buf);
            List<Boolean> have = new ArrayList<>();
            for (int i = 0; i < inputs.size(); i++) {
                have.add(buf.readBoolean());
            }
            return new RecipeView(name, inputs, have, ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                    buf.readDouble(), buf.readVarInt());
        }

        void write(RegistryFriendlyByteBuf buf) {
            buf.writeUtf(name.length() > 128 ? name.substring(0, 128) : name, 128);
            writeStacks(buf, inputs);
            for (int i = 0; i < inputs.size(); i++) {
                buf.writeBoolean(i < have.size() && have.get(i));
            }
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, partial);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, perfect);
            buf.writeDouble(difficulty);
            buf.writeVarInt(cooldown);
        }

        public boolean ready() {
            return cooldown <= 0 && !have.contains(false);
        }
    }

    /** A station's recipes, for the player to choose one. */
    public record Station(String def, BlockPos pos, String title, int game, ItemStack icon, List<RecipeView> recipes, int cooldown)
            implements CustomPacketPayload {
        public static final Type<Station> TYPE = payload("station");
        public static final StreamCodec<RegistryFriendlyByteBuf, Station> CODEC = CustomPacketPayload.codec(Station::write, Station::new);

        Station(RegistryFriendlyByteBuf buf) {
            this(buf.readUtf(64), buf.readBlockPos(), buf.readUtf(128), buf.readVarInt(), ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), readRecipes(buf),
                    buf.readVarInt());
        }

        private static List<RecipeView> readRecipes(RegistryFriendlyByteBuf buf) {
            int n = Math.min(buf.readVarInt(), 128);
            List<RecipeView> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                list.add(RecipeView.read(buf));
            }
            return list;
        }

        void write(RegistryFriendlyByteBuf buf) {
            buf.writeUtf(def, 64);
            buf.writeBlockPos(pos);
            buf.writeUtf(title.length() > 128 ? title.substring(0, 128) : title, 128);
            buf.writeVarInt(game);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, icon);
            buf.writeVarInt(recipes.size());
            recipes.forEach(r -> r.write(buf));
            buf.writeVarInt(cooldown);
        }

        @Override
        public Type<Station> type() {
            return TYPE;
        }

        void handle(IPayloadContext context) {
            context.enqueueWork(() -> com.waylays.arcanes.client.metier.MetierClient.station(this));
        }
    }

    /** The trades, for the editor (JSON). */
    public record Editor(String json) implements CustomPacketPayload {
        public static final Type<Editor> TYPE = payload("editor");
        public static final StreamCodec<RegistryFriendlyByteBuf, Editor> CODEC = CustomPacketPayload.codec(Editor::write, Editor::new);

        Editor(RegistryFriendlyByteBuf buf) {
            this(buf.readUtf(1 << 21));
        }

        void write(RegistryFriendlyByteBuf buf) {
            buf.writeUtf(json, 1 << 21);
        }

        @Override
        public Type<Editor> type() {
            return TYPE;
        }

        void handle(IPayloadContext context) {
            context.enqueueWork(() -> com.waylays.arcanes.client.metier.MetierClient.editor(this));
        }
    }

    // ---------------------------------------------------------------- client to server

    /** How the minigame went: its {@code score} (0 to 1), whether it ended in a {@code critical} failure (or was given up). */
    public record Result(int session, float score, boolean critical) implements CustomPacketPayload {
        public static final Type<Result> TYPE = payload("result");
        public static final StreamCodec<RegistryFriendlyByteBuf, Result> CODEC = CustomPacketPayload.codec(Result::write, Result::new);

        Result(RegistryFriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readFloat(), buf.readBoolean());
        }

        void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(session);
            buf.writeFloat(score);
            buf.writeBoolean(critical);
        }

        @Override
        public Type<Result> type() {
            return TYPE;
        }

        void handle(IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) {
                context.enqueueWork(() -> MetierService.result(player, this));
            }
        }
    }

    /** A recipe chosen at a station ({@code recipe} -1: its own screen). */
    public record Pick(String def, BlockPos pos, int recipe) implements CustomPacketPayload {
        public static final Type<Pick> TYPE = payload("pick");
        public static final StreamCodec<RegistryFriendlyByteBuf, Pick> CODEC = CustomPacketPayload.codec(Pick::write, Pick::new);

        Pick(RegistryFriendlyByteBuf buf) {
            this(buf.readUtf(64), buf.readBlockPos(), buf.readVarInt());
        }

        void write(RegistryFriendlyByteBuf buf) {
            buf.writeUtf(def, 64);
            buf.writeBlockPos(pos);
            buf.writeVarInt(recipe);
        }

        @Override
        public Type<Pick> type() {
            return TYPE;
        }

        void handle(IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) {
                context.enqueueWork(() -> MetierService.pick(player, this));
            }
        }
    }

    /** The edited trades, in pieces (a client's packets stay small): piece {@code part} of {@code parts}. */
    public record Save(int part, int parts, String chunk) implements CustomPacketPayload {
        public static final Type<Save> TYPE = payload("save");
        public static final StreamCodec<RegistryFriendlyByteBuf, Save> CODEC = CustomPacketPayload.codec(Save::write, Save::new);
        public static final int CHUNK = 8000;

        Save(RegistryFriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readVarInt(), buf.readUtf(CHUNK));
        }

        void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(part);
            buf.writeVarInt(parts);
            buf.writeUtf(chunk, CHUNK);
        }

        @Override
        public Type<Save> type() {
            return TYPE;
        }

        void handle(IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) {
                context.enqueueWork(() -> MetierService.save(player, this));
            }
        }

        /** {@code json} cut into packets. */
        public static List<Save> split(String json) {
            List<Save> out = new ArrayList<>();
            int parts = Math.max(1, (json.length() + CHUNK - 1) / CHUNK);
            for (int i = 0; i < parts; i++) {
                out.add(new Save(i, parts, json.substring(i * CHUNK, Math.min(json.length(), (i + 1) * CHUNK))));
            }
            return out;
        }
    }
}
