package com.waylays.arcanes.rp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The role-play commands: /setname, /me, /do, /roll, /try, /chuchoter, /crier, /hrp, /desc, /examiner, /voir, and
 * /rp (the list, and for game masters who plays whom and naming someone else's character). /me, /do, /try,
 * /chuchoter, /crier and /hrp take a range in blocks before their text ("/me 40 lève la main"), /roll before its dice.
 */
public final class RpCommands {
    private static final int MAX_TEXT = 256;

    private RpCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("setname")
                .then(Commands.argument("prenom", StringArgumentType.word()).then(Commands.argument("nom", StringArgumentType.greedyString())
                        .executes(RpCommands::setName))));
        // The game's own /me is replaced (it shows the account's name)
        if (remove(d, "me")) {
            d.register(Commands.literal("me").then(Commands.argument("action", StringArgumentType.greedyString())
                    .executes(c -> act(c, "action", RpService::me))));
        }
        d.register(Commands.literal("do").then(Commands.argument("description", StringArgumentType.greedyString())
                .executes(c -> act(c, "description", RpService::doing))));
        d.register(Commands.literal("try").then(Commands.argument("action", StringArgumentType.greedyString())
                .executes(c -> act(c, "action", RpService::attempt))));
        d.register(Commands.literal("chuchoter").then(Commands.argument("message", StringArgumentType.greedyString())
                .executes(c -> act(c, "message", RpService::whisper))));
        d.register(Commands.literal("crier").then(Commands.argument("message", StringArgumentType.greedyString())
                .executes(c -> act(c, "message", RpService::shout))));
        d.register(Commands.literal("hrp").then(Commands.argument("message", StringArgumentType.greedyString())
                .executes(c -> act(c, "message", RpService::ooc))));
        d.register(Commands.literal("roll").executes(c -> roll(c, "100", 0))
                .then(Commands.argument("des", StringArgumentType.word()).executes(c -> roll(c, StringArgumentType.getString(c, "des"), 0))
                        // "/roll 30 1d20": the range first, then the dice
                        .then(Commands.argument("des2", StringArgumentType.word()).executes(c -> {
                            int range;
                            try {
                                range = Integer.parseInt(StringArgumentType.getString(c, "des"));
                            } catch (NumberFormatException e) {
                                c.getSource().sendFailure(Component.translatable("rp.arcanes_oublies.bad_dice"));
                                return 0;
                            }
                            return roll(c, StringArgumentType.getString(c, "des2"), Math.max(1, Math.min(RpConfig.get(RpConfig.MAX_RANGE, 200), range)));
                        }))));
        d.register(Commands.literal("desc").executes(RpCommands::clearDescription)
                .then(Commands.argument("texte", StringArgumentType.greedyString()).executes(RpCommands::description)));
        d.register(Commands.literal("examiner").then(Commands.argument("cible", EntityArgument.player()).executes(RpCommands::examine)));
        d.register(Commands.literal("voir").requires(s -> s.hasPermission(RpConfig.get(RpConfig.HIDE_PERMISSION, 2)))
                .then(Commands.literal("tous").executes(RpCommands::showAll))
                .then(Commands.literal("personne").executes(c -> hide(c, Set.of())))
                .then(Commands.argument("joueurs", EntityArgument.players()).executes(c -> {
                    Set<UUID> ids = new HashSet<>();
                    for (ServerPlayer p : EntityArgument.getPlayers(c, "joueurs")) {
                        ids.add(p.getUUID());
                    }
                    return hide(c, ids);
                })));
        d.register(Commands.literal("pseudos").requires(s -> s.hasPermission(2))
                .then(Commands.literal("cacher").executes(c -> nameplates(c, true)))
                .then(Commands.literal("afficher").executes(c -> nameplates(c, false))));
        d.register(Commands.literal("rp").executes(RpCommands::help)
                .then(Commands.literal("qui").requires(s -> s.hasPermission(2)).executes(RpCommands::who))
                .then(Commands.literal("nom").requires(s -> s.hasPermission(2)).then(Commands.argument("cible", EntityArgument.player())
                        .then(Commands.argument("prenom", StringArgumentType.word()).then(Commands.argument("nom", StringArgumentType.greedyString())
                                .executes(RpCommands::nameOther))))));
    }

    /** Takes a command the game already has out of the tree, so it can be given anew; false if it could not. */
    private static boolean remove(CommandDispatcher<CommandSourceStack> d, String name) {
        try {
            for (String field : new String[] {"children", "literals"}) {
                java.lang.reflect.Field f = com.mojang.brigadier.tree.CommandNode.class.getDeclaredField(field);
                f.setAccessible(true);
                ((java.util.Map<?, ?>) f.get(d.getRoot())).remove(name);
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            com.waylays.arcanes.ArcanesOublies.LOGGER.warn("Could not replace the /{} command", name, e);
            return d.getRoot().getChild(name) == null;
        }
    }

    private interface Action {
        void run(ServerPlayer p, String text, int range);
    }

    /** "12 text": a range in blocks put before the text; null if the text does not start with one. */
    static final java.util.regex.Pattern RANGED = java.util.regex.Pattern.compile("^(\\d{1,6})\\s+(\\S.*)$", java.util.regex.Pattern.DOTALL);

    private static String text(CommandContext<CommandSourceStack> c, String arg) {
        String t = StringArgumentType.getString(c, arg).strip();
        return t.length() > MAX_TEXT ? t.substring(0, MAX_TEXT) : t;
    }

    /** The range and the text of "30 the text", or null if the text does not start with a range. */
    public static java.util.regex.Matcher ranged(String text) {
        java.util.regex.Matcher m = RANGED.matcher(text);
        return m.matches() ? m : null;
    }

    /** Runs {@code a} on the text; "/me 30 text" carries it 30 blocks (up to the server's limit) instead of the usual range. */
    private static int act(CommandContext<CommandSourceStack> c, String arg, Action a) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String t = text(c, arg);
        int range = 0;
        java.util.regex.Matcher m = RANGED.matcher(t);
        if (m.matches()) {
            range = Math.max(1, Math.min(RpConfig.get(RpConfig.MAX_RANGE, 200), Integer.parseInt(m.group(1))));
            t = m.group(2).strip();
        }
        if (t.isEmpty()) {
            return 0;
        }
        a.run(p, t, range);
        return 1;
    }

    private static boolean validName(String s) {
        return s.length() >= 2 && s.length() <= 16 && s.matches("[\\p{L}][\\p{L}'\\- ]*");
    }

    private static String capital(String s) {
        StringBuilder b = new StringBuilder();
        boolean up = true;
        for (char ch : s.toCharArray()) {
            b.append(up ? Character.toUpperCase(ch) : ch);
            up = ch == ' ' || ch == '-' || ch == '\'';
        }
        return b.toString();
    }

    private static int setName(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        return name(c, c.getSource().getPlayerOrException());
    }

    private static int nameOther(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        return name(c, EntityArgument.getPlayer(c, "cible"));
    }

    private static int name(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        String first = StringArgumentType.getString(c, "prenom").strip(), last = StringArgumentType.getString(c, "nom").strip();
        if (!validName(first) || !validName(last)) {
            c.getSource().sendFailure(Component.translatable("rp.arcanes_oublies.bad_name"));
            return 0;
        }
        RpData.get(p.server).setName(p.getUUID(), capital(first), capital(last));
        RpService.syncName(p);
        c.getSource().sendSuccess(() -> Component.translatable("rp.arcanes_oublies.named", RpService.name(p)).withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int roll(CommandContext<CommandSourceStack> c, String spec, int range) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String s = spec.toLowerCase().replace(" ", "");
        int count = 1, sides;
        try {
            if (s.contains("d")) {
                String[] parts = s.split("d", 2);
                count = parts[0].isEmpty() ? 1 : Integer.parseInt(parts[0]);
                sides = Integer.parseInt(parts[1]);
            } else {
                sides = Integer.parseInt(s);
            }
        } catch (NumberFormatException e) {
            c.getSource().sendFailure(Component.translatable("rp.arcanes_oublies.bad_dice"));
            return 0;
        }
        if (count < 1 || count > 20 || sides < 2 || sides > 1000) {
            c.getSource().sendFailure(Component.translatable("rp.arcanes_oublies.bad_dice"));
            return 0;
        }
        RpService.roll(p, count, sides, range);
        return 1;
    }

    private static int description(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        RpData.get(p.server).setDescription(p.getUUID(), text(c, "texte"));
        c.getSource().sendSuccess(() -> Component.translatable("rp.arcanes_oublies.description_set").withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int clearDescription(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        RpData.get(p.server).setDescription(p.getUUID(), "");
        c.getSource().sendSuccess(() -> Component.translatable("rp.arcanes_oublies.description_cleared").withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int examine(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer me = c.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(c, "cible");
        if (!RpVisibility.sees(me, target) || target.level() != me.level() || target.distanceTo(me) > 12) {
            c.getSource().sendFailure(Component.translatable("rp.arcanes_oublies.too_far"));
            return 0;
        }
        RpData.Identity id = RpData.get(me.server).identity(target.getUUID());
        me.sendSystemMessage(Component.empty().append(RpService.colored("❖ ", RpService.NAME)).append(RpService.nameOf(target, RpService.NAME)));
        String desc = id == null || id.description().isEmpty() ? Component.translatable("rp.arcanes_oublies.no_description").getString() : id.description();
        me.sendSystemMessage(RpService.colored("   " + desc, 0xC8C8D0).withStyle(ChatFormatting.ITALIC));
        return 1;
    }

    private static int hide(CommandContext<CommandSourceStack> c, Set<UUID> viewers) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        viewers = new HashSet<>(viewers);
        viewers.remove(p.getUUID());
        RpData.get(p.server).hide(p.getUUID(), viewers);
        RpVisibility.apply(p);
        if (viewers.isEmpty()) {
            c.getSource().sendSuccess(() -> Component.translatable("rp.arcanes_oublies.hidden_all").withStyle(ChatFormatting.DARK_PURPLE), false);
        } else {
            StringBuilder names = new StringBuilder();
            for (UUID id : viewers) {
                ServerPlayer v = p.server.getPlayerList().getPlayer(id);
                names.append(names.isEmpty() ? "" : ", ").append(v == null ? "?" : RpService.name(v));
            }
            c.getSource().sendSuccess(() -> Component.translatable("rp.arcanes_oublies.seen_only_by", names.toString()).withStyle(ChatFormatting.DARK_PURPLE), false);
        }
        return 1;
    }

    private static int showAll(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        RpData.get(p.server).show(p.getUUID());
        RpVisibility.apply(p);
        c.getSource().sendSuccess(() -> Component.translatable("rp.arcanes_oublies.seen_by_all").withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int nameplates(CommandContext<CommandSourceStack> c, boolean hidden) {
        RpNameplates.set(c.getSource().getServer(), hidden);
        c.getSource().sendSuccess(() -> Component.translatable(hidden ? "rp.arcanes_oublies.names_hidden" : "rp.arcanes_oublies.names_shown")
                .withStyle(ChatFormatting.GRAY), true);
        return 1;
    }

    private static int help(CommandContext<CommandSourceStack> c) {
        for (String line : new String[] {"setname", "me", "do", "roll", "try", "chuchoter", "crier", "hrp", "desc", "examiner", "voir", "pseudos",
                "warp", "warplist"}) {
            c.getSource().sendSuccess(() -> Component.empty().append(RpService.colored("/" + line + " ", RpService.NAME))
                    .append(Component.translatable("rp.arcanes_oublies.help." + line).withStyle(ChatFormatting.GRAY)), false);
        }
        return 1;
    }

    private static int who(CommandContext<CommandSourceStack> c) {
        Collection<ServerPlayer> players = c.getSource().getServer().getPlayerList().getPlayers();
        for (ServerPlayer p : players) {
            String hidden = RpVisibility.hidden(p) ? "  ◌" : "";
            c.getSource().sendSuccess(() -> Component.literal(RpService.name(p) + "  ←  " + p.getGameProfile().getName() + hidden)
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return players.size();
    }
}
