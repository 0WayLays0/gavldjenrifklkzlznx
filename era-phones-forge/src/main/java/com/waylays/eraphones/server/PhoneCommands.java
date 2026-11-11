package com.waylays.eraphones.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.waylays.eraphones.Numbers;
import com.waylays.eraphones.item.PhoneItem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The game masters' tools ({@code /telephone}, permission level 2): write or call from a hidden number, trouble a
 * phone, publish the town's news, look up the numbers someone carries, recharge a phone.
 */
public final class PhoneCommands {
    private PhoneCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("telephone").requires(s -> s.hasPermission(2))
                .then(Commands.literal("inconnu")
                        .then(Commands.literal("sms").then(Commands.argument("numero", StringArgumentType.word())
                                .then(Commands.argument("message", StringArgumentType.greedyString()).executes(PhoneCommands::hiddenSms))))
                        .then(Commands.literal("appel").then(Commands.argument("numero", StringArgumentType.word())
                                .then(Commands.argument("paroles", StringArgumentType.greedyString()).executes(PhoneCommands::hiddenCall)))))
                .then(Commands.literal("hanter").then(Commands.argument("numero", StringArgumentType.word())
                        .executes(c -> haunt(c, 10, ""))
                        .then(Commands.argument("secondes", IntegerArgumentType.integer(1, 120))
                                .executes(c -> haunt(c, IntegerArgumentType.getInteger(c, "secondes"), ""))
                                .then(Commands.argument("message", StringArgumentType.greedyString())
                                        .executes(c -> haunt(c, IntegerArgumentType.getInteger(c, "secondes"), StringArgumentType.getString(c, "message")))))))
                .then(Commands.literal("journal").then(Commands.argument("article", StringArgumentType.greedyString()).executes(PhoneCommands::news)))
                .then(Commands.literal("numeros").then(Commands.argument("cible", EntityArgument.player()).executes(PhoneCommands::numbers)))
                .then(Commands.literal("recharger").executes(PhoneCommands::recharge)));
    }

    private static String number(CommandContext<CommandSourceStack> c) {
        String raw = StringArgumentType.getString(c, "numero");
        String n = Numbers.normalize(raw);
        if (n == null || !LineData.get(c.getSource().getServer()).exists(n)) {
            c.getSource().sendFailure(Component.translatable("command.eraphones.unknown_number", raw));
            return null;
        }
        return n;
    }

    private static int hiddenSms(CommandContext<CommandSourceStack> c) {
        String n = number(c);
        if (n == null) {
            return 0;
        }
        String text = StringArgumentType.getString(c, "message");
        PhoneService.deliver(c.getSource().getServer(), LineData.get(c.getSource().getServer()), Numbers.HIDDEN, n, text);
        c.getSource().sendSuccess(() -> Component.translatable("command.eraphones.sent", n), true);
        return 1;
    }

    private static int hiddenCall(CommandContext<CommandSourceStack> c) {
        String n = number(c);
        if (n == null) {
            return 0;
        }
        List<String> lines = new ArrayList<>();
        for (String l : Arrays.asList(StringArgumentType.getString(c, "paroles").split("\\|"))) {
            if (!l.isBlank()) {
                lines.add(l.strip());
            }
        }
        if (!CallService.hidden(c.getSource().getServer(), n, lines)) {
            c.getSource().sendFailure(Component.translatable("command.eraphones.unreachable", n));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.translatable("command.eraphones.calling", n), true);
        return 1;
    }

    private static int haunt(CommandContext<CommandSourceStack> c, int seconds, String text) {
        String n = number(c);
        if (n == null) {
            return 0;
        }
        if (!PhoneEvents.haunt(c.getSource().getServer(), n, seconds * 20, text)) {
            c.getSource().sendFailure(Component.translatable("command.eraphones.unreachable", n));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.translatable("command.eraphones.haunted", n, seconds), true);
        return 1;
    }

    private static int news(CommandContext<CommandSourceStack> c) {
        String raw = StringArgumentType.getString(c, "article");
        int bar = raw.indexOf('|');
        String title = (bar >= 0 ? raw.substring(0, bar) : raw).strip();
        String text = bar >= 0 ? raw.substring(bar + 1).strip() : "";
        if (title.length() > 90) {
            title = title.substring(0, 90);
        }
        if (text.length() > 500) {
            text = text.substring(0, 500);
        }
        LineData d = LineData.get(c.getSource().getServer());
        d.news(title, text, PhoneService.now());
        String t = title;
        for (ServerPlayer p : c.getSource().getServer().getPlayerList().getPlayers()) {
            for (Map.Entry<String, ItemStack> e : PhoneService.carried(p).entrySet()) {
                com.waylays.eraphones.Era era = PhoneItem.era(e.getValue());
                // The smartphone's Journal and the flip phone's news both carry the town's articles
                if ((era == com.waylays.eraphones.Era.SMART || era == com.waylays.eraphones.Era.FLIP) && PhoneItem.battery(e.getValue()) > 0) {
                    com.waylays.eraphones.compat.PacketDistributor.sendToPlayer(p, new com.waylays.eraphones.network.PhoneNet.Notice("news", t, text, e.getKey()));
                    if (PhoneService.isOpen(p.getUUID())) {
                        PhoneService.sendNews(p, d);
                    }
                    break;
                }
            }
        }
        c.getSource().sendSuccess(() -> Component.translatable("command.eraphones.published", t), true);
        return 1;
    }

    private static int numbers(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(c, "cible");
        Map<String, ItemStack> phones = PhoneService.carried(target);
        if (phones.isEmpty()) {
            c.getSource().sendSuccess(() -> Component.translatable("command.eraphones.no_phones"), false);
            return 0;
        }
        for (Map.Entry<String, ItemStack> e : phones.entrySet()) {
            c.getSource().sendSuccess(() -> Component.translatable("command.eraphones.number_line", e.getKey(), e.getValue().getHoverName(),
                    PhoneItem.battery(e.getValue()) / 10), false);
        }
        return phones.size();
    }

    private static int recharge(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ItemStack st = p.getMainHandItem();
        if (!(st.getItem() instanceof PhoneItem)) {
            st = p.getOffhandItem();
        }
        if (!(st.getItem() instanceof PhoneItem)) {
            c.getSource().sendFailure(Component.translatable("command.eraphones.hold_phone"));
            return 0;
        }
        PhoneItem.setBattery(st, PhoneItem.FULL);
        c.getSource().sendSuccess(() -> Component.translatable("message.eraphones.charged"), false);
        return 1;
    }
}
