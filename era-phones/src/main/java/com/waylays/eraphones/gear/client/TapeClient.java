package com.waylays.eraphones.gear.client;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.gear.Tape;
import com.waylays.eraphones.gear.TapeNet;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

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
    /** Pieces sent each tick: about 1.2 MB a second. */
    private static final int PER_TICK = 2;
    private static byte[] bytes;
    private static int sent;
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
        return bytes == null || bytes.length == 0 ? 0 : sent / (float) bytes.length;
    }

    /** Asks the server to record {@code file} on the cassette in that hand. */
    static void start(boolean offhand, Path file, String title) {
        if (busy()) {
            return;
        }
        try {
            long size = Files.size(file);
            if (size <= 0 || size > TapeNet.MAX) {
                say(Component.translatable("gear.eraphones.burn_bad"), true);
                return;
            }
            bytes = Files.readAllBytes(file);
        } catch (IOException e) {
            say(Component.translatable("gear.eraphones.burn_read_failed", file.getFileName().toString()), true);
            return;
        }
        sent = 0;
        waiting = true;
        say(Component.translatable("gear.eraphones.burn_waiting"), false);
        PacketDistributor.sendToServer(new TapeNet.Begin(offhand, file.getFileName().toString(), title, bytes.length));
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
        bytes = null;
        sent = 0;
        sending = false;
        waiting = false;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!sending || bytes == null) {
            return;
        }
        if (Minecraft.getInstance().getConnection() == null) {
            reset();
            return;
        }
        for (int k = 0; k < PER_TICK && sent < bytes.length; k++) {
            int n = Math.min(TapeNet.CHUNK, bytes.length - sent);
            PacketDistributor.sendToServer(new TapeNet.Chunk(Arrays.copyOfRange(bytes, sent, sent + n)));
            sent += n;
        }
        if (sent >= bytes.length) {
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
