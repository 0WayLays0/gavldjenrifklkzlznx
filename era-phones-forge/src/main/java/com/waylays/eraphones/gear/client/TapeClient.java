package com.waylays.eraphones.gear.client;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.gear.Tape;
import com.waylays.eraphones.gear.TapeNet;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import com.waylays.eraphones.compat.PacketDistributor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Recording a cassette, the game master's side: the folder their recordings go in (opened for them), and the file
 * they pick sent to the server a piece at a time, a few pieces a tick, while they go on playing.
 */
@EventBusSubscriber(modid = EraPhones.MODID, value = Dist.CLIENT)
public final class TapeClient {
    /** Pieces sent each tick: about 3 MB a second. */
    private static final int PER_TICK = 5;
    /** The file being sent, read a piece at a time (any size), how long it is, how much has gone. */
    private static java.nio.channels.FileChannel file;
    private static long size, sent;
    private static boolean sending, waiting;
    /** What the screen shows under the list: a key and its argument, and whether it is bad news. */
    static Component status;
    static boolean statusBad;

    private TapeClient() {}

    /** The server lets a game master record the blank cassette in that hand: the folder opens, and the screen. */
    public static void open(TapeNet.Open p) {
        openFolder();
        Minecraft.getInstance().setScreen(new BurnScreen(p.offhand()));
    }

    public static void openFolder() {
        Path dir = Tape.folder();
        try {
            Files.createDirectories(dir);
            Util.getPlatform().openPath(dir);
        } catch (Exception e) {
            say(Component.translatable("gear.eraphones.burn_folder_failed", dir.toString()), true);
        }
    }

    static boolean busy() {
        return sending || waiting;
    }

    /** The share of the file sent, 0 to 1. */
    static float progress() {
        return size <= 0 ? 0 : (float) ((double) sent / size);
    }

    /** Asks the server to record {@code file} on the cassette in that hand. */
    static void start(boolean offhand, Path path, String title) {
        if (busy()) {
            return;
        }
        try {
            reset();
            size = Files.size(path);
            if (size <= 0) {
                say(Component.translatable("gear.eraphones.burn_bad"), true);
                return;
            }
            file = java.nio.channels.FileChannel.open(path, java.nio.file.StandardOpenOption.READ);
        } catch (IOException e) {
            reset();
            say(Component.translatable("gear.eraphones.burn_read_failed", path.getFileName().toString()), true);
            return;
        }
        sent = 0;
        waiting = true;
        say(Component.translatable("gear.eraphones.burn_waiting"), false);
        PacketDistributor.sendToServer(new TapeNet.Begin(offhand, path.getFileName().toString(), title, size));
    }

    public static void state(TapeNet.State s) {
        switch (s.state()) {
            case "send" -> {
                waiting = false;
                sending = true;
                sent = 0;
            }
            case "done" -> {
                reset();
                Component done = Component.translatable("gear.eraphones.burn_done", s.detail());
                say(done, false);
                Minecraft mc = Minecraft.getInstance();
                if (mc.screen instanceof BurnScreen) {
                    mc.setScreen(null);
                }
                if (mc.player != null) {
                    mc.player.displayClientMessage(done.copy().withStyle(ChatFormatting.GREEN), true);
                }
            }
            default -> {
                reset();
                say(Component.translatable(s.detail().isEmpty() ? "gear.eraphones.burn_failed" : s.detail()), true);
                Minecraft mc = Minecraft.getInstance();
                if (!(mc.screen instanceof BurnScreen) && mc.player != null) {
                    mc.player.displayClientMessage(status.copy().withStyle(ChatFormatting.RED), true);
                }
            }
        }
    }

    private static void say(Component c, boolean bad) {
        status = c;
        statusBad = bad;
    }

    private static void reset() {
        if (file != null) {
            try {
                file.close();
            } catch (IOException ignored) {
                // Done with it anyway
            }
        }
        file = null;
        size = 0;
        sent = 0;
        sending = false;
        waiting = false;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!sending || file == null) {
            return;
        }
        if (Minecraft.getInstance().getConnection() == null) {
            reset();
            return;
        }
        try {
            for (int k = 0; k < PER_TICK && sent < size; k++) {
                java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocate((int) Math.min(TapeNet.CHUNK, size - sent));
                while (buf.hasRemaining() && file.read(buf, sent + buf.position()) > 0) {
                    // Filled a piece
                }
                PacketDistributor.sendToServer(new TapeNet.Chunk(Arrays.copyOf(buf.array(), buf.position())));
                sent += buf.position();
                if (buf.position() == 0) {
                    throw new IOException("the file shrank");
                }
            }
        } catch (IOException e) {
            reset();
            say(Component.translatable("gear.eraphones.burn_failed"), true);
            return;
        }
        if (sent >= size) {
            PacketDistributor.sendToServer(new TapeNet.End());
            sending = false;
            waiting = true;
            say(Component.translatable("gear.eraphones.burn_waiting"), false);
        }
    }

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
        status = null;
    }
}
