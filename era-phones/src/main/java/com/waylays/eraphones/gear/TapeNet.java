package com.waylays.eraphones.gear;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.registry.GearRegistry;
import com.waylays.eraphones.EraConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Recording a cassette from the game: a game master uses a blank cassette, picks an MP3 in their own cassettes folder
 * and it is sent to the server in pieces (unless the server has it already, as in a world of one's own), kept in the
 * server's cassettes folder, and written on the cassette in their hand, for good.
 */
@EventBusSubscriber(modid = EraPhones.MODID)
public final class TapeNet {
    /** One piece of a file on its way (bytes): under the 32 KiB a packet to the server may carry. */
    public static final int CHUNK = 30000;
    public static final int TITLE = 40;
    private static final Map<UUID, Upload> UPLOADS = new HashMap<>();

    private TapeNet() {}

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payload(String name) {
        return new CustomPacketPayload.Type<>(EraPhones.id("tape_" + name));
    }

    public static void register(PayloadRegistrar r) {
        r.playToClient(Open.TYPE, Open.CODEC, (p, c) -> com.waylays.eraphones.gear.client.TapeClient.open(p));
        r.playToClient(State.TYPE, State.CODEC, (p, c) -> com.waylays.eraphones.gear.client.TapeClient.state(p));
        r.playToServer(Begin.TYPE, Begin.CODEC, TapeNet::begin);
        r.playToServer(Chunk.TYPE, Chunk.CODEC, TapeNet::chunk);
        r.playToServer(End.TYPE, End.CODEC, TapeNet::end);
    }

    public static boolean staff(ServerPlayer p) {
        return p.hasPermissions(EraConfig.staffPermission());
    }

    /** A blank cassette used by {@code p}: a game master gets the recording screen, anyone else is told what it is. */
    public static void blankUsed(ServerPlayer p, InteractionHand hand) {
        if (staff(p)) {
            PacketDistributor.sendToPlayer(p, new Open(hand == InteractionHand.OFF_HAND));
        } else {
            p.displayClientMessage(Component.translatable("gear.eraphones.tape_blank_staff").withStyle(ChatFormatting.GRAY), true);
        }
    }

    /** A file name made safe to keep on the server (letters, digits, spaces and a few marks; .mp3), or null. */
    public static String clean(String name) {
        if (name == null) {
            return null;
        }
        String base = name.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1);
        String ext = base.toLowerCase().endsWith(".mp3") ? ".mp3" : base.toLowerCase().endsWith(".mp4") ? ".mp4" : null;
        if (ext == null) {
            return null;
        }
        base = base.substring(0, base.length() - 4);
        StringBuilder b = new StringBuilder();
        for (char ch : base.toCharArray()) {
            b.append(Character.isLetterOrDigit(ch) || " _-().,'".indexOf(ch) >= 0 ? ch : '_');
        }
        String s = b.toString().trim();
        while (s.startsWith(".")) {
            s = s.substring(1);
        }
        if (s.isEmpty()) {
            return null;
        }
        return (s.length() > 60 ? s.substring(0, 60) : s) + ext;
    }

    private static void reply(ServerPlayer p, String state, String detail) {
        PacketDistributor.sendToPlayer(p, new State(state, detail));
    }

    private static void begin(Begin b, IPayloadContext c) {
        if (!(c.player() instanceof ServerPlayer p) || !staff(p)) {
            return;
        }
        abort(p.getUUID());
        String file = clean(b.file());
        if (file == null || b.size() <= 0) {
            reply(p, "error", "gear.eraphones.burn_bad");
            return;
        }
        try {
            Files.createDirectories(Tape.folder());
            Path dest = Tape.folder().resolve(file);
            if (Files.isRegularFile(dest) && Files.size(dest) == b.size()) {
                // The server has it already (a world of one's own shares the folder): no need to send it
                burn(p, b.offhand(), file, b.title());
                return;
            }
            String base = file.substring(0, file.length() - 4), ext = file.substring(file.length() - 4);
            for (int k = 2; Files.exists(dest); k++) {
                file = base + " (" + k + ")" + ext;
                dest = Tape.folder().resolve(file);
            }
            Path part = Tape.folder().resolve(file + ".part");
            UPLOADS.put(p.getUUID(), new Upload(file, part, Files.newOutputStream(part), b.size(), b.offhand(), b.title()));
            reply(p, "send", file);
        } catch (IOException e) {
            EraPhones.LOGGER.warn("Cannot take the cassette recording {}: {}", file, e.toString());
            reply(p, "error", "gear.eraphones.burn_failed");
        }
    }

    private static void chunk(Chunk ch, IPayloadContext c) {
        if (!(c.player() instanceof ServerPlayer p)) {
            return;
        }
        Upload u = UPLOADS.get(p.getUUID());
        if (u == null) {
            return;
        }
        if (u.received + ch.data().length > u.size) {
            abort(p.getUUID());
            reply(p, "error", "gear.eraphones.burn_bad");
            return;
        }
        try {
            u.out.write(ch.data());
            u.received += ch.data().length;
        } catch (IOException e) {
            abort(p.getUUID());
            reply(p, "error", "gear.eraphones.burn_failed");
        }
    }

    private static void end(End e, IPayloadContext c) {
        if (!(c.player() instanceof ServerPlayer p)) {
            return;
        }
        Upload u = UPLOADS.remove(p.getUUID());
        if (u == null) {
            return;
        }
        try {
            u.out.close();
            if (u.received != u.size) {
                Files.deleteIfExists(u.part);
                reply(p, "error", "gear.eraphones.burn_failed");
                return;
            }
            Files.move(u.part, Tape.folder().resolve(u.file), StandardCopyOption.REPLACE_EXISTING);
            burn(p, u.offhand, u.file, u.title);
        } catch (IOException ex) {
            EraPhones.LOGGER.warn("Cannot keep the cassette recording {}: {}", u.file, ex.toString());
            reply(p, "error", "gear.eraphones.burn_failed");
        }
    }

    /** Writes {@code file} on the blank cassette in {@code p}'s hand, labelled {@code title}, for good. */
    private static void burn(ServerPlayer p, boolean offhand, String file, String title) {
        ItemStack stack = p.getItemInHand(offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!(stack.getItem() instanceof CassetteItem) || stack.get(GearRegistry.TAPE.get()) != null) {
            reply(p, "error", "gear.eraphones.burn_no_cassette");
            return;
        }
        String label = title == null || title.isBlank() ? file.substring(0, file.length() - 4) : title.trim();
        label = label.length() > TITLE ? label.substring(0, TITLE) : label;
        stack.set(GearRegistry.TAPE.get(), file);
        stack.set(GearRegistry.TAPE_LABEL.get(), label);
        stack.set(GearRegistry.TAPE_POS.get(), 0L);
        Tape.sound(file);
        if (Video.isVideo(file)) {
            Video.prepare(file);
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, .7f, 1.2f);
        reply(p, "done", label);
    }

    private static void abort(UUID id) {
        Upload u = UPLOADS.remove(id);
        if (u != null) {
            try {
                u.out.close();
                Files.deleteIfExists(u.part);
            } catch (IOException ignored) {
                // Nothing more to do with a half-sent file
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        abort(event.getEntity().getUUID());
    }

    private static final class Upload {
        final String file, title;
        final Path part;
        final OutputStream out;
        final long size;
        final boolean offhand;
        long received;

        Upload(String file, Path part, OutputStream out, long size, boolean offhand, String title) {
            this.file = file;
            this.part = part;
            this.out = out;
            this.size = size;
            this.offhand = offhand;
            this.title = title;
        }
    }

    // ---------------------------------------------------------------------------------------------------- packets

    /** To a game master: open the recording screen for the blank cassette in that hand. */
    public record Open(boolean offhand) implements CustomPacketPayload {
        public static final Type<Open> TYPE = payload("open");
        public static final StreamCodec<FriendlyByteBuf, Open> CODEC = CustomPacketPayload.codec((p, b) -> b.writeBoolean(p.offhand),
                b -> new Open(b.readBoolean()));

        @Override
        public Type<Open> type() {
            return TYPE;
        }
    }

    /** To the game master: "send" (the file, start sending), "done" (the label), "error" (what went wrong, a key). */
    public record State(String state, String detail) implements CustomPacketPayload {
        public static final Type<State> TYPE = payload("state");
        public static final StreamCodec<FriendlyByteBuf, State> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeUtf(p.state, 16);
            b.writeUtf(p.detail, 256);
        }, b -> new State(b.readUtf(16), b.readUtf(256)));

        public State {
            state = state.length() > 16 ? state.substring(0, 16) : state;
            detail = detail == null ? "" : detail.length() > 256 ? detail.substring(0, 256) : detail;
        }

        @Override
        public Type<State> type() {
            return TYPE;
        }
    }

    /** From a game master: record {@code file} ({@code size} bytes) on the cassette in that hand, labelled {@code title}. */
    public record Begin(boolean offhand, String file, String title, long size) implements CustomPacketPayload {
        public static final Type<Begin> TYPE = payload("begin");
        public static final StreamCodec<FriendlyByteBuf, Begin> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeBoolean(p.offhand);
            b.writeUtf(p.file, 128);
            b.writeUtf(p.title, TITLE);
            b.writeVarLong(p.size);
        }, b -> new Begin(b.readBoolean(), b.readUtf(128), b.readUtf(TITLE), b.readVarLong()));

        public Begin {
            file = file.length() > 128 ? file.substring(0, 128) : file;
            title = title == null ? "" : title.length() > TITLE ? title.substring(0, TITLE) : title;
        }

        @Override
        public Type<Begin> type() {
            return TYPE;
        }
    }

    /** A piece of the file on its way. */
    public record Chunk(byte[] data) implements CustomPacketPayload {
        public static final Type<Chunk> TYPE = payload("chunk");
        public static final StreamCodec<FriendlyByteBuf, Chunk> CODEC = CustomPacketPayload.codec((p, b) -> b.writeByteArray(p.data),
                b -> new Chunk(b.readByteArray(CHUNK)));

        @Override
        public Type<Chunk> type() {
            return TYPE;
        }
    }

    /** The whole file sent. */
    public record End() implements CustomPacketPayload {
        public static final Type<End> TYPE = payload("end");
        public static final StreamCodec<FriendlyByteBuf, End> CODEC = CustomPacketPayload.codec((p, b) -> {
        }, b -> new End());

        @Override
        public Type<End> type() {
            return TYPE;
        }
    }
}
