package com.waylays.eraphones.gear;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.registry.GearRegistry;
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
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The camcorder's films: the server starts and stops a recording (on a blank cassette the filmer carries) and writes
 * the pictures their screen sends, ten a second, straight into a film the televisions can play, with the voices
 * spoken near the camcorder (with Simple Voice Chat).
 */
@EventBusSubscriber(modid = EraPhones.MODID)
public final class CamNet {
    /** The longest film (seconds), and the most a frame may weigh squeezed. */
    public static final int LONGEST = 15 * 60, MAX = 30000;
    private static final Map<UUID, Recording> RECORDING = new ConcurrentHashMap<>();

    private CamNet() {}

    /** How far round a filmer voices are heard on the film (blocks). */
    public static final double HEARS = 8;

    static final class Recording {
        final String file;
        final VideoDecode.Writer writer;
        final long start = System.currentTimeMillis();
        /** The sound filmed (48 kHz): the voices of those speaking near the camcorder, each at the time it was said. */
        short[] sound = new short[Audio.RATE * 10];
        int soundLength;

        Recording(String file, VideoDecode.Writer writer) {
            this.file = file;
            this.writer = writer;
        }
    }

    public static void register(PayloadRegistrar r) {
        r.playToServer(Frame.TYPE, Frame.CODEC, CamNet::frame);
        r.playToClient(Rec.TYPE, Rec.CODEC, (p, c) -> com.waylays.eraphones.gear.client.CamClient.rec(p.on()));
    }

    public static boolean recording(ServerPlayer p) {
        return RECORDING.containsKey(p.getUUID());
    }

    /** Whether any camcorder is filming (voices are then worth decoding). */
    public static boolean anyRecording() {
        return !RECORDING.isEmpty();
    }

    /**
     * {@code speaker} said this (48 kHz, from their microphone): it goes on the films of the camcorders within
     * {@link #HEARS} blocks, louder the nearer, at the moment it was said.
     */
    public static void voice(ServerPlayer speaker, short[] pcm) {
        if (pcm == null || pcm.length == 0) {
            return;
        }
        for (Map.Entry<UUID, Recording> e : RECORDING.entrySet()) {
            ServerPlayer filmer = speaker.server.getPlayerList().getPlayer(e.getKey());
            if (filmer == null || filmer.level() != speaker.level()) {
                continue;
            }
            double d = filmer.distanceTo(speaker);
            if (d > HEARS) {
                continue;
            }
            float gain = (float) (d < 1 ? 1 : Math.max(.15, 1 - d / HEARS));
            Recording r = e.getValue();
            synchronized (r) {
                int at = (int) Math.max(0, (System.currentTimeMillis() - r.start) * Audio.RATE / 1000 - pcm.length);
                int end = Math.min(at + pcm.length, LONGEST * Audio.RATE);
                if (end <= at) {
                    continue;
                }
                if (end > r.sound.length) {
                    r.sound = java.util.Arrays.copyOf(r.sound, Math.min(LONGEST * Audio.RATE, Math.max(end, r.sound.length * 2)));
                }
                for (int i = at; i < end; i++) {
                    r.sound[i] = (short) Math.max(-32768, Math.min(32767, r.sound[i] + pcm[i - at] * gain));
                }
                r.soundLength = Math.max(r.soundLength, end);
            }
        }
    }

    /** Writes a film's sound next to its pictures (raw 16-bit, little end first). */
    private static void saveSound(Recording r) {
        short[] pcm;
        synchronized (r) {
            pcm = java.util.Arrays.copyOf(r.sound, r.soundLength);
        }
        if (pcm.length == 0) {
            return;
        }
        try {
            java.nio.ByteBuffer b = java.nio.ByteBuffer.allocate(pcm.length * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            b.asShortBuffer().put(pcm);
            Files.write(Video.soundOf(r.file), b.array());
        } catch (IOException e) {
            EraPhones.LOGGER.warn("A film's sound could not be kept: {}", e.toString());
        }
    }

    /** Starts filming on the first blank cassette {@code p} carries; false (and why, told) if there is none. */
    public static boolean start(ServerPlayer p) {
        ItemStack tape = ItemStack.EMPTY;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.getItem() instanceof CassetteItem && s.get(GearRegistry.TAPE.get()) == null) {
                tape = s;
                break;
            }
        }
        if (tape.isEmpty()) {
            p.displayClientMessage(Component.translatable("camcorder.eraphones.no_tape").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        String file = "vhs-" + UUID.randomUUID().toString().substring(0, 8) + ".vhs";
        try {
            Files.createDirectories(Video.cache(file).getParent());
            VideoDecode.Writer w = new VideoDecode.Writer(Video.cache(file).toFile());
            RECORDING.put(p.getUUID(), new Recording(file, w));
        } catch (IOException e) {
            EraPhones.LOGGER.warn("A film could not be started: {}", e.toString());
            return false;
        }
        tape.set(GearRegistry.TAPE.get(), file);
        tape.set(GearRegistry.TAPE_LABEL.get(), "VHS · " + com.waylays.eraphones.EraConfig.name(p));
        p.level().playSound(null, p.blockPosition(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.PLAYERS, .4f, 1.9f);
        PacketDistributor.sendToPlayer(p, new Rec(true));
        return true;
    }

    public static void stop(ServerPlayer p) {
        Recording r = RECORDING.remove(p.getUUID());
        if (r == null) {
            return;
        }
        int frames = r.writer.frames();
        try {
            r.writer.close();
        } catch (IOException e) {
            EraPhones.LOGGER.warn("A film could not be finished: {}", e.toString());
        }
        saveSound(r);
        Video.forget(r.file);
        PacketDistributor.sendToPlayer(p, new Rec(false));
        int s = frames / VideoDecode.FPS;
        p.displayClientMessage(Component.translatable("camcorder.eraphones.saved", String.format("%d:%02d", s / 60, s % 60)), true);
        p.level().playSound(null, p.blockPosition(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.PLAYERS, .4f, 1.6f);
    }

    /** A picture from a filmer's screen, written at its time (repeated to fill a gap, as a tape keeps running). */
    private static void frame(Frame f, IPayloadContext c) {
        if (!(c.player() instanceof ServerPlayer p) || f.data().length == 0 || f.data().length > MAX) {
            return;
        }
        Recording r = RECORDING.get(p.getUUID());
        if (r == null) {
            return;
        }
        long due = (System.currentTimeMillis() - r.start) * VideoDecode.FPS / 1000 + 1;
        try {
            int written = r.writer.frames();
            int copies = (int) Math.max(1, Math.min(10, due - written));
            for (int k = 0; k < copies && r.writer.frames() < LONGEST * VideoDecode.FPS; k++) {
                r.writer.add(f.data());
            }
        } catch (IOException e) {
            EraPhones.LOGGER.warn("A film frame could not be written: {}", e.toString());
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (RECORDING.isEmpty()) {
            return;
        }
        Iterator<UUID> it = RECORDING.keySet().iterator();
        while (it.hasNext()) {
            UUID id = it.next();
            ServerPlayer p = event.getServer().getPlayerList().getPlayer(id);
            Recording r = RECORDING.get(id);
            boolean holding = p != null && (p.getMainHandItem().is(GearRegistry.CAMCORDER.get()) || p.getOffhandItem().is(GearRegistry.CAMCORDER.get()));
            boolean full = r != null && r.writer.frames() >= LONGEST * VideoDecode.FPS;
            if (p == null) {
                Recording gone = RECORDING.remove(id);
                if (gone != null) {
                    try {
                        gone.writer.close();
                    } catch (IOException ignored) {
                        // Lost anyway
                    }
                    saveSound(gone);
                    Video.forget(gone.file);
                }
            } else if (!holding || full) {
                stop(p);
            }
        }
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) {
            stop(p);
        }
    }

    public record Frame(byte[] data) implements CustomPacketPayload {
        public static final Type<Frame> TYPE = new Type<>(EraPhones.id("camcorder_frame"));
        public static final StreamCodec<FriendlyByteBuf, Frame> CODEC = CustomPacketPayload.codec((p, b) -> b.writeByteArray(p.data),
                b -> new Frame(b.readByteArray(MAX)));

        @Override
        public Type<Frame> type() {
            return TYPE;
        }
    }

    public record Rec(boolean on) implements CustomPacketPayload {
        public static final Type<Rec> TYPE = new Type<>(EraPhones.id("camcorder_rec"));
        public static final StreamCodec<FriendlyByteBuf, Rec> CODEC = CustomPacketPayload.codec((p, b) -> b.writeBoolean(p.on),
                b -> new Rec(b.readBoolean()));

        @Override
        public Type<Rec> type() {
            return TYPE;
        }
    }
}
