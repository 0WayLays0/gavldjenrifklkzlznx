package com.waylays.eraphones.rp;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Speaking and acting in character: who hears what (in range, and only those who see the one speaking), how it shows
 * in the chat (the character's name, never the account's) and in a bubble over the head.
 */
public final class RpService {
    public static final int SAY = 0xFFFFFF, ME = 0xC9A8FF, DO = 0xFFC870, ROLL = 0xFFD45A, TRY_OK = 0x7CE08A, TRY_KO = 0xFF7A7A, WHISPER = 0xA8B8C8,
            SHOUT = 0xFFFFFF, NAME = 0x8FD0FF;

    private RpService() {}

    /** The character's name, or "A stranger" until they have one. */
    public static String name(ServerPlayer p) {
        RpData.Identity id = RpData.get(p.server).identity(p.getUUID());
        return id == null || id.full().isEmpty() ? Component.translatable("rp.eraphones.stranger").getString() : id.full();
    }

    public static boolean named(ServerPlayer p) {
        RpData.Identity id = RpData.get(p.server).identity(p.getUUID());
        return id != null && !id.full().isEmpty();
    }

    static MutableComponent colored(String s, int rgb) {
        return Component.literal(s).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
    }

    static MutableComponent nameOf(ServerPlayer p, int rgb) {
        return colored(name(p), rgb).withStyle(ChatFormatting.BOLD);
    }

    /**
     * Sends {@code line} to everyone within {@code range} of {@code from} who sees them (and to {@code from}); those
     * between {@code range} and {@code farRange} get {@code far} instead, if given. A bubble goes over the head.
     */
    public static void tell(ServerPlayer from, int range, Component line, int farRange, Component far, String bubble, int bubbleColor) {
        for (ServerPlayer p : from.server.getPlayerList().getPlayers()) {
            if (p.level() != from.level() || !RpVisibility.sees(p, from)) {
                continue;
            }
            double d = p.distanceTo(from);
            if (p == from || d <= range) {
                p.sendSystemMessage(line);
                if (bubble != null && RpConfig.get(RpConfig.BUBBLES, false) && p != from) {
                    PacketDistributor.sendToPlayer(p, new RpNet.Bubble(from.getId(), bubble, bubbleColor));
                }
            } else if (far != null && d <= farRange) {
                p.sendSystemMessage(far);
            }
        }
        if (bubble != null && RpConfig.get(RpConfig.BUBBLES, false)) {
            PacketDistributor.sendToPlayer(from, new RpNet.Bubble(from.getId(), bubble, bubbleColor));
        }
    }

    public static void say(ServerPlayer p, String text) {
        Component line = Component.empty().append(nameOf(p, NAME)).append(colored(" " + Component.translatable("rp.eraphones.says").getString() + " ", 0x9A9AA8))
                .append(colored("« " + text + " »", SAY));
        tell(p, RpConfig.get(RpConfig.SAY_RANGE, 20), line, 0, null, "« " + text + " »", SAY);
        // On the phone, the other end hears it too
        com.waylays.eraphones.server.CallService.relay(p, text);
    }

    public static void me(ServerPlayer p, String text) {
        Component line = Component.empty().append(colored("✦ ", ME)).append(nameOf(p, ME))
                .append(colored(" " + text, ME).withStyle(ChatFormatting.ITALIC));
        tell(p, RpConfig.get(RpConfig.ACTION_RANGE, 20), line, 0, null, "* " + text, ME);
    }

    public static void doing(ServerPlayer p, String text) {
        Component line = Component.empty().append(colored("◆ ", DO)).append(colored(text, DO).withStyle(ChatFormatting.ITALIC))
                .append(colored("  (" + name(p) + ")", 0x8A8070));
        tell(p, RpConfig.get(RpConfig.ACTION_RANGE, 20), line, 0, null, "◆ " + text, DO);
    }

    public static void roll(ServerPlayer p, int count, int sides) {
        int total = 0;
        StringBuilder each = new StringBuilder();
        for (int i = 0; i < count; i++) {
            int r = 1 + p.getRandom().nextInt(sides);
            total += r;
            if (count > 1) {
                each.append(i == 0 ? "" : " + ").append(r);
            }
        }
        String what = count == 1 ? Component.translatable("rp.eraphones.rolls_one", sides).getString()
                : Component.translatable("rp.eraphones.rolls_many", count, sides).getString();
        Component line = Component.empty().append(colored("⚄ ", ROLL)).append(nameOf(p, ROLL))
                .append(colored(" " + what + " ", 0xD8C8A0))
                .append(colored(String.valueOf(total), ROLL).withStyle(ChatFormatting.BOLD))
                .append(colored(count > 1 ? "  (" + each + ")" : "", 0x8A8070));
        String bubble = "⚄ " + (count == 1 ? Component.translatable("rp.eraphones.die_one", sides).getString()
                : Component.translatable("rp.eraphones.die_many", count, sides).getString()) + " " + total;
        tell(p, RpConfig.get(RpConfig.ACTION_RANGE, 20), line, 0, null, bubble, ROLL);
    }

    public static void attempt(ServerPlayer p, String text) {
        boolean ok = p.getRandom().nextBoolean();
        String verdict = Component.translatable(ok ? "rp.eraphones.success" : "rp.eraphones.failure").getString();
        Component line = Component.empty().append(colored("⚖ ", ok ? TRY_OK : TRY_KO)).append(nameOf(p, ME))
                .append(colored(" " + Component.translatable("rp.eraphones.tries").getString() + " " + text + " — ", ME).withStyle(ChatFormatting.ITALIC))
                .append(colored(verdict, ok ? TRY_OK : TRY_KO).withStyle(ChatFormatting.BOLD));
        tell(p, RpConfig.get(RpConfig.ACTION_RANGE, 20), line, 0, null, "* " + text + " — " + verdict, ok ? TRY_OK : TRY_KO);
    }

    public static void whisper(ServerPlayer p, String text) {
        int r = RpConfig.get(RpConfig.WHISPER_RANGE, 4);
        Component line = Component.empty().append(nameOf(p, WHISPER))
                .append(colored(" " + Component.translatable("rp.eraphones.whispers").getString() + " ", 0x7A8490))
                .append(colored("« " + text + " »", WHISPER).withStyle(ChatFormatting.ITALIC));
        Component far = Component.empty().append(colored(name(p), WHISPER)).append(colored(" " + Component.translatable("rp.eraphones.whispers_something").getString(),
                0x7A8490).withStyle(ChatFormatting.ITALIC));
        tell(p, r, line, r * 3, far, null, WHISPER);
    }

    public static void shout(ServerPlayer p, String text) {
        Component line = Component.empty().append(nameOf(p, 0xFF9A7A)).append(colored(" " + Component.translatable("rp.eraphones.shouts").getString() + " ", 0xB89080))
                .append(colored("« " + text + " ! »", SHOUT).withStyle(ChatFormatting.BOLD));
        tell(p, RpConfig.get(RpConfig.SHOUT_RANGE, 48), line, 0, null, "« " + text + " ! »", 0xFFB09A);
    }

    /** Out of character: everyone, under the account's name (and marked as such). */
    public static void ooc(ServerPlayer p, String text) {
        Component line = Component.empty().append(colored("[HRP] ", 0x6A6A78)).append(colored(p.getGameProfile().getName(), 0x9A9AA8))
                .append(colored(" : " + text, 0x9A9AA8));
        for (ServerPlayer o : p.server.getPlayerList().getPlayers()) {
            o.sendSystemMessage(line);
        }
    }

    // ---------------------------------------------------------------- names for the clients

    public static Map<UUID, String> names(MinecraftServer server) {
        Map<UUID, String> out = new HashMap<>();
        RpData.get(server).identities().forEach((k, v) -> {
            if (!v.full().isEmpty()) {
                out.put(k, v.full());
            }
        });
        return out;
    }

    public static void syncName(ServerPlayer p) {
        Map<UUID, String> one = new HashMap<>();
        one.put(p.getUUID(), name(p));
        PacketDistributor.sendToAllPlayers(new RpNet.Names(one, false));
        p.refreshDisplayName();
        p.refreshTabListName();
    }
}
