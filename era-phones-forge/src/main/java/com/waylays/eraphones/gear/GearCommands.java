package com.waylays.eraphones.gear;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.waylays.eraphones.registry.GearRegistry;
import com.waylays.eraphones.rp.RpConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The game masters' commands for the things: recording cassettes (/cassette), going on the air (/radio), making out an
 * identity card (/identite), printing the newspaper (/journal).
 */
public final class GearCommands {
    private GearCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("cassette").requires(s -> s.hasPermission(RpConfig.get(RpConfig.HORROR_PERMISSION, 2)))
                .then(Commands.literal("liste").executes(GearCommands::tapes))
                .then(Commands.literal("graver").then(Commands.argument("fichier", StringArgumentType.string())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Tape.files().stream().map(f -> f.contains(" ") ? "\"" + f + "\"" : f), b))
                        .then(Commands.argument("titre", StringArgumentType.greedyString()).executes(GearCommands::record)))));
        d.register(Commands.literal("radio").requires(s -> s.hasPermission(RpConfig.get(RpConfig.HORROR_PERMISSION, 2)))
                .then(Commands.literal("emettre").then(Commands.argument("frequence", StringArgumentType.word()).executes(GearCommands::onAir)))
                .then(Commands.literal("arreter").executes(c -> {
                    Stations.set(c.getSource().getServer(), c.getSource().getPlayerOrException().getUUID(), null);
                    c.getSource().sendSuccess(() -> Component.translatable("gear.eraphones.off_air").withStyle(ChatFormatting.GRAY), false);
                    return 1;
                })));
        d.register(Commands.literal("lampe").requires(s -> s.hasPermission(RpConfig.get(RpConfig.HORROR_PERMISSION, 2)))
                .then(Commands.argument("joueurs", EntityArgument.players())
                        .then(Commands.literal("eteindre").executes(c -> torches(c, "eteindre", 0)))
                        .then(Commands.literal("clignoter").executes(c -> torches(c, "clignoter", 10))
                                .then(Commands.argument("secondes", IntegerArgumentType.integer(1, 600))
                                        .executes(c -> torches(c, "clignoter", IntegerArgumentType.getInteger(c, "secondes")))))
                        .then(Commands.literal("casser").executes(c -> torches(c, "casser", 0)))
                        .then(Commands.literal("reparer").executes(c -> torches(c, "reparer", 0)))));
        d.register(Commands.literal("identite").requires(s -> s.hasPermission(RpConfig.get(RpConfig.HORROR_PERMISSION, 2)))
                .then(Commands.literal("delivrer").then(Commands.argument("joueur", EntityArgument.player()).executes(GearCommands::issue))));
        d.register(Commands.literal("journal").requires(s -> s.hasPermission(RpConfig.get(RpConfig.HORROR_PERMISSION, 2)))
                .then(Commands.literal("imprimer").then(Commands.argument("exemplaires", IntegerArgumentType.integer(1, 64))
                        .then(Commands.argument("article", StringArgumentType.greedyString()).executes(GearCommands::print)))));
    }

    /** What a game master does to the targets' torches: out, flickering, broken, mended. */
    private static int torches(CommandContext<CommandSourceStack> c, String what, int seconds) throws CommandSyntaxException {
        java.util.Collection<ServerPlayer> targets = EntityArgument.getPlayers(c, "joueurs");
        int n = 0;
        for (ServerPlayer p : targets) {
            if (what.equals("clignoter")) {
                Beams.flicker(c.getSource().getServer(), p.getUUID(), seconds * 20);
                n++;
                continue;
            }
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                ItemStack s = p.getInventory().getItem(i);
                if (!(s.getItem() instanceof FlashlightItem)) {
                    continue;
                }
                n++;
                switch (what) {
                    case "eteindre" -> Gear.setOn(s, false);
                    case "casser" -> {
                        Gear.setOn(s, false);
                        GearRegistry.BROKEN.get().set(s, true);
                    }
                    default -> GearRegistry.BROKEN.get().remove(s);
                }
            }
            if (what.equals("casser") || what.equals("eteindre")) {
                p.level().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.LEVER_CLICK, net.minecraft.sounds.SoundSource.PLAYERS,
                        .4f, what.equals("casser") ? .6f : 1.2f);
            }
        }
        int count = n;
        c.getSource().sendSuccess(() -> Component.translatable("gear.eraphones.torches_done", what, count).withStyle(ChatFormatting.GRAY), false);
        return count;
    }

    private static int tapes(CommandContext<CommandSourceStack> c) {
        List<String> files = Tape.files();
        c.getSource().sendSuccess(() -> Component.translatable("gear.eraphones.tapes", Tape.folder().toString()).withStyle(ChatFormatting.GRAY), false);
        if (files.isEmpty()) {
            c.getSource().sendSuccess(() -> Component.translatable("gear.eraphones.tapes_none").withStyle(ChatFormatting.DARK_GRAY), false);
        }
        for (String f : files) {
            c.getSource().sendSuccess(() -> Component.literal("  " + f).withStyle(ChatFormatting.WHITE), false);
        }
        return files.size();
    }

    private static int record(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        String file = StringArgumentType.getString(c, "fichier");
        String title = StringArgumentType.getString(c, "titre");
        if (!Tape.files().contains(file)) {
            c.getSource().sendFailure(Component.translatable("gear.eraphones.tape_missing", file, Tape.folder().toString()));
            return 0;
        }
        ItemStack tape = new ItemStack(GearRegistry.CASSETTE.get());
        GearRegistry.TAPE.get().set(tape, file);
        GearRegistry.TAPE_LABEL.get().set(tape, title.length() > 40 ? title.substring(0, 40) : title);
        GearRegistry.TAPE_POS.get().set(tape, 0L);
        ServerPlayer p = c.getSource().getPlayerOrException();
        if (!p.getInventory().add(tape)) {
            p.drop(tape, false);
        }
        Tape.sound(file);
        c.getSource().sendSuccess(() -> Component.translatable("gear.eraphones.tape_recorded", title).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int onAir(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        String raw = StringArgumentType.getString(c, "frequence").replace(',', '.');
        int freq;
        try {
            freq = (int) Math.round(Double.parseDouble(raw) * 10);
        } catch (NumberFormatException e) {
            freq = -1;
        }
        if (freq < Stations.MIN || freq > Stations.MAX) {
            c.getSource().sendFailure(Component.translatable("gear.eraphones.bad_frequency"));
            return 0;
        }
        int f = freq;
        Stations.set(c.getSource().getServer(), c.getSource().getPlayerOrException().getUUID(), f);
        c.getSource().sendSuccess(() -> Component.translatable("gear.eraphones.on_air", Stations.format(f)).withStyle(ChatFormatting.GREEN), false);
        if (Audio.get() == null) {
            c.getSource().sendFailure(Component.translatable("gear.eraphones.needs_voicechat"));
        }
        return 1;
    }

    private static int issue(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(c, "joueur");
        ItemStack card = new ItemStack(GearRegistry.ID_CARD.get());
        IdCardItem.issue(card, target);
        if (!target.getInventory().add(card)) {
            target.drop(card, false);
        }
        c.getSource().sendSuccess(() -> Component.translatable("gear.eraphones.card_issued", GearRegistry.CARD_NAME.get().get(card)).withStyle(ChatFormatting.GRAY), true);
        return 1;
    }

    private static int print(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        int copies = IntegerArgumentType.getInteger(c, "exemplaires");
        String raw = StringArgumentType.getString(c, "article");
        int bar = raw.indexOf('|');
        String title = (bar >= 0 ? raw.substring(0, bar) : raw).strip();
        String text = bar >= 0 ? raw.substring(bar + 1).strip() : "";
        ItemStack paper = new ItemStack(GearRegistry.NEWSPAPER.get(), copies);
        GearRegistry.NEWS_TITLE.get().set(paper, title.length() > 90 ? title.substring(0, 90) : title);
        GearRegistry.NEWS_TEXT.get().set(paper, text.length() > 1500 ? text.substring(0, 1500) : text);
        ServerPlayer p = c.getSource().getPlayerOrException();
        if (!p.getInventory().add(paper)) {
            p.drop(paper, false);
        }
        c.getSource().sendSuccess(() -> Component.translatable("gear.eraphones.printed", copies, title).withStyle(ChatFormatting.GRAY), false);
        return copies;
    }
}
