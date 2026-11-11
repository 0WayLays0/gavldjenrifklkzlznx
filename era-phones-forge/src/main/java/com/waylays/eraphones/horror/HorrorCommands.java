package com.waylays.eraphones.horror;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.waylays.eraphones.rp.RpConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The game masters' commands of fear: /horreur (sounds, a whisper in the head, fog, the lights going out, blood,
 * a red balloon, floating, dizziness, the ground shaking, and stop) and /voix (a changed voice, with Simple Voice Chat).
 */
public final class HorrorCommands {
    private static final String[] TOOLS = {"son", "murmure", "brouillard", "coupure", "sang", "ballon", "flotter", "vertige", "tremblement", "stop"};

    private HorrorCommands() {}

    private interface Act {
        int run(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> targets) throws CommandSyntaxException;
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("horreur").requires(s -> s.hasPermission(RpConfig.get(RpConfig.HORROR_PERMISSION, 2)))
                .executes(HorrorCommands::help)
                .then(Commands.literal("son").then(Commands.argument("joueurs", EntityArgument.players())
                        .then(Commands.argument("son", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(HorrorService.sounds(), b))
                                .executes(c -> sound(c, false))
                                .then(Commands.literal("ici").executes(c -> sound(c, true))))))
                .then(Commands.literal("murmure").then(Commands.argument("joueurs", EntityArgument.players())
                        .then(Commands.argument("texte", StringArgumentType.greedyString()).executes(c -> on(c, (cc, t) -> {
                            String text = StringArgumentType.getString(cc, "texte");
                            HorrorService.show(t, "murmure", 60 + Math.min(400, text.length() * 3), text, null);
                            return done(cc, "murmure", t);
                        })))))
                .then(timed("brouillard", 60, (c, t, s) -> show(c, t, "brouillard", s)))
                .then(timed("coupure", 8, (c, t, s) -> show(c, t, "coupure", s)))
                .then(timed("sang", 8, (c, t, s) -> show(c, t, "sang", s)))
                .then(timed("tremblement", 4, (c, t, s) -> show(c, t, "tremblement", s)))
                .then(timed("flotter", 6, (c, t, s) -> {
                    HorrorService.levitate(t, s * 20);
                    return done(c, "flotter", t);
                }))
                .then(timed("peur", 30, (c, t, s) -> show(c, t, "peur", s)))
                .then(timed("vertige", 15, (c, t, s) -> {
                    HorrorService.dizzy(t, s * 20);
                    return done(c, "vertige", t);
                }))
                .then(Commands.literal("ballon").then(Commands.argument("joueurs", EntityArgument.players())
                        .executes(c -> on(c, (cc, t) -> balloon(cc, t, null)))
                        .then(Commands.literal("ici").executes(c -> on(c, (cc, t) -> balloon(cc, t, cc.getSource().getPosition().add(0, 1.6, 0)))))))
                .then(Commands.literal("stop").then(Commands.argument("joueurs", EntityArgument.players()).executes(c -> on(c, (cc, t) -> {
                    HorrorService.stop(t);
                    cc.getSource().sendSuccess(() -> Component.translatable("rp.eraphones.horror.stopped", t.size()).withStyle(ChatFormatting.GRAY), true);
                    return t.size();
                })))));

        // The narrator: words over everyone's screen, or over one player's alone
        d.register(Commands.literal("narration").requires(s -> s.hasPermission(RpConfig.get(RpConfig.HORROR_PERMISSION, 2)))
                .then(Commands.argument("texte", StringArgumentType.greedyString())
                        .executes(c -> narrate(c, c.getSource().getServer().getPlayerList().getPlayers()))));
        d.register(Commands.literal("narrationalone").requires(s -> s.hasPermission(RpConfig.get(RpConfig.HORROR_PERMISSION, 2)))
                .then(Commands.argument("joueurs", EntityArgument.players()).then(Commands.argument("texte", StringArgumentType.greedyString())
                        .executes(c -> narrate(c, EntityArgument.getPlayers(c, "joueurs"))))));

        // Fear: blurred sight, a pounding heart, for as long as the game master says
        d.register(Commands.literal("peur").requires(s -> s.hasPermission(RpConfig.get(RpConfig.HORROR_PERMISSION, 2)))
                // The fear gauge: on or off for the whole server, how afraid each one is, set by hand
                .then(Commands.literal("jauge")
                        .executes(HorrorCommands::gaugeInfo)
                        .then(Commands.literal("on").executes(c -> gaugeSwitch(c, true)))
                        .then(Commands.literal("off").executes(c -> gaugeSwitch(c, false)))
                        .then(Commands.literal("info").executes(HorrorCommands::gaugeInfo))
                        // A staff member feels it themselves, to try it (again: stops), and may set their own level
                        .then(Commands.literal("test").executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            boolean on = Fear.toggleTest(p);
                            if (on && !Fear.enabled(c.getSource().getServer())) {
                                c.getSource().sendSuccess(() -> Component.translatable("rp.eraphones.fear.test_disabled").withStyle(ChatFormatting.GOLD), false);
                            }
                            c.getSource().sendSuccess(() -> Component.translatable(on ? "rp.eraphones.fear.test_on" : "rp.eraphones.fear.test_off")
                                    .withStyle(ChatFormatting.DARK_RED), false);
                            return 1;
                        }).then(Commands.argument("valeur", IntegerArgumentType.integer(0, 100)).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            if (!Fear.testing(p)) {
                                Fear.toggleTest(p);
                            }
                            int v = IntegerArgumentType.getInteger(c, "valeur");
                            Fear.set(p, v);
                            c.getSource().sendSuccess(() -> Component.translatable("rp.eraphones.fear.test_set", v).withStyle(ChatFormatting.DARK_RED), false);
                            return 1;
                        })))
                        .then(Commands.argument("joueurs", EntityArgument.players())
                                .then(Commands.argument("valeur", IntegerArgumentType.integer(0, 100)).executes(c -> {
                                    Collection<ServerPlayer> t = EntityArgument.getPlayers(c, "joueurs");
                                    int v = IntegerArgumentType.getInteger(c, "valeur");
                                    int n = 0;
                                    for (ServerPlayer p : t) {
                                        if (!Fear.exempt(p)) {
                                            Fear.set(p, v);
                                            n++;
                                        }
                                    }
                                    int done = n;
                                    c.getSource().sendSuccess(() -> Component.translatable("rp.eraphones.fear.set", done, v).withStyle(ChatFormatting.DARK_RED), true);
                                    return done;
                                }))))
                .then(Commands.argument("joueurs", EntityArgument.players())
                        .executes(c -> show(c, EntityArgument.getPlayers(c, "joueurs"), "peur", 30))
                        .then(Commands.argument("secondes", IntegerArgumentType.integer(1, 600))
                                .executes(c -> show(c, EntityArgument.getPlayers(c, "joueurs"), "peur", IntegerArgumentType.getInteger(c, "secondes"))))));

        // Words written on what the game master looks at: /ecrire <style> [petit|moyen|grand] <texte> ("|" starts a line)
        com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> write = Commands.literal("ecrire")
                .requires(s -> s.hasPermission(RpConfig.get(RpConfig.HORROR_PERMISSION, 2)));
        for (String style : new String[] {"sang", "craie", "peinture", "noir"}) {
            com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> st = Commands.literal(style)
                    .then(Commands.argument("texte", StringArgumentType.greedyString()).executes(c -> write(c, style, .45f)));
            for (String[] size : new String[][] {{"petit", ".25"}, {"moyen", ".45"}, {"grand", ".9"}, {"enorme", "1.6"}}) {
                st.then(Commands.literal(size[0]).then(Commands.argument("texte", StringArgumentType.greedyString())
                        .executes(c -> write(c, style, Float.parseFloat(size[1])))));
            }
            write.then(st);
        }
        write.then(Commands.literal("effacer").executes(c -> rubOut(c, 4)));
        write.then(Commands.literal("effacertout").executes(c -> rubOutAll(c, 48))
                .then(Commands.argument("rayon", IntegerArgumentType.integer(1, 256)).executes(c -> rubOutAll(c, IntegerArgumentType.getInteger(c, "rayon")))));
        d.register(write);

        // The narrator in the chat alone: words, nothing on the screen
        d.register(Commands.literal("narrationchat").requires(s -> s.hasPermission(RpConfig.get(RpConfig.HORROR_PERMISSION, 2)))
                .then(Commands.argument("texte", StringArgumentType.greedyString())
                        .executes(c -> narrateChat(c, c.getSource().getServer().getPlayerList().getPlayers()))));
        d.register(Commands.literal("narrationchatalone").requires(s -> s.hasPermission(RpConfig.get(RpConfig.HORROR_PERMISSION, 2)))
                .then(Commands.argument("joueurs", EntityArgument.players()).then(Commands.argument("texte", StringArgumentType.greedyString())
                        .executes(c -> narrateChat(c, EntityArgument.getPlayers(c, "joueurs"))))));

        d.register(Commands.literal("voix").requires(s -> s.hasPermission(RpConfig.get(RpConfig.VOICE_PERMISSION, 2)))
                .executes(HorrorCommands::voices)
                .then(Commands.literal("ecoute").executes(HorrorCommands::listen))
                .then(Commands.literal("dose").then(Commands.argument("pourcent", IntegerArgumentType.integer(10, 100))
                        .executes(c -> dose(c, List.of(c.getSource().getPlayerOrException())))
                        .then(Commands.argument("joueurs", EntityArgument.players()).executes(c -> dose(c, EntityArgument.getPlayers(c, "joueurs"))))))
                .then(Commands.argument("voix", StringArgumentType.word())
                        .suggests((c, b) -> {
                            List<String> all = new ArrayList<>(List.of(VoiceFx.VOICES));
                            all.add("normale");
                            return SharedSuggestionProvider.suggest(all, b);
                        })
                        .executes(c -> voice(c, List.of(c.getSource().getPlayerOrException())))
                        .then(Commands.argument("joueurs", EntityArgument.players()).executes(c -> voice(c, EntityArgument.getPlayers(c, "joueurs"))))));
    }

    private static int narrate(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> targets) {
        String text = StringArgumentType.getString(c, "texte");
        HorrorService.show(targets, "narration", 80 + Math.min(500, text.length() * 4), text, null);
        Component line = Component.literal("\u2726 " + text).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        for (ServerPlayer p : targets) {
            p.sendSystemMessage(line);
        }
        c.getSource().sendSuccess(() -> Component.translatable("rp.eraphones.narration.sent", targets.size()).withStyle(ChatFormatting.DARK_GRAY), false);
        return targets.size();
    }

    private static int narrateChat(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> targets) {
        Component line = Component.literal("\u2726 " + StringArgumentType.getString(c, "texte")).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        for (ServerPlayer p : targets) {
            p.sendSystemMessage(line);
        }
        c.getSource().sendSuccess(() -> Component.translatable("rp.eraphones.narration.sent", targets.size()).withStyle(ChatFormatting.DARK_GRAY), false);
        return targets.size();
    }

    /** What the game master looks at, within 32 blocks: the face hit, or null. */
    private static net.minecraft.world.phys.BlockHitResult looked(ServerPlayer p) {
        Vec3 eye = p.getEyePosition();
        net.minecraft.world.phys.BlockHitResult hit = p.level().clip(new net.minecraft.world.level.ClipContext(eye, eye.add(p.getLookAngle().scale(32)),
                net.minecraft.world.level.ClipContext.Block.OUTLINE, net.minecraft.world.level.ClipContext.Fluid.NONE, p));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? null : hit;
    }

    private static int write(CommandContext<CommandSourceStack> c, String style, float size) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        net.minecraft.world.phys.BlockHitResult hit = looked(p);
        if (hit == null) {
            c.getSource().sendFailure(Component.translatable("rp.eraphones.writing.nowhere"));
            return 0;
        }
        String text = StringArgumentType.getString(c, "texte").replace("\\n", "|");
        net.minecraft.core.Direction face = hit.getDirection();
        Vec3 at = hit.getLocation().add(Vec3.atLowerCornerOf(face.getNormal()).scale(.012));
        WallWriting w = new WallWriting(com.waylays.eraphones.registry.GearRegistry.WRITING.get(), p.level());
        w.setPos(at.x, at.y, at.z);
        w.setup(text.length() > 200 ? text.substring(0, 200) : text, style, face.get3DDataValue(), size, p.getYRot() + 180);
        p.level().addFreshEntity(w);
        c.getSource().sendSuccess(() -> Component.translatable("rp.eraphones.writing.written").withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int rubOut(CommandContext<CommandSourceStack> c, double reach) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        net.minecraft.world.phys.BlockHitResult hit = looked(p);
        Vec3 at = hit == null ? p.getEyePosition().add(p.getLookAngle().scale(3)) : hit.getLocation();
        int n = 0;
        for (WallWriting w : p.level().getEntitiesOfClass(WallWriting.class, new net.minecraft.world.phys.AABB(at, at).inflate(reach))) {
            w.rubOut();
            n++;
        }
        int count = n;
        c.getSource().sendSuccess(() -> Component.translatable("rp.eraphones.writing.erased", count).withStyle(ChatFormatting.GRAY), false);
        return n;
    }

    private static int rubOutAll(CommandContext<CommandSourceStack> c, int radius) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        int n = 0;
        for (WallWriting w : p.level().getEntitiesOfClass(WallWriting.class, p.getBoundingBox().inflate(radius))) {
            w.rubOut();
            n++;
        }
        int count = n;
        c.getSource().sendSuccess(() -> Component.translatable("rp.eraphones.writing.erased", count).withStyle(ChatFormatting.GRAY), false);
        return n;
    }

    private interface Timed {
        int run(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> targets, int seconds) throws CommandSyntaxException;
    }

    /** /horreur {@code name} players [seconds]. */
    private static ArgumentBuilder<CommandSourceStack, ?> timed(String name, int seconds, Timed act) {
        return Commands.literal(name).then(Commands.argument("joueurs", EntityArgument.players())
                .executes(c -> act.run(c, EntityArgument.getPlayers(c, "joueurs"), seconds))
                .then(Commands.argument("secondes", IntegerArgumentType.integer(1, 600))
                        .executes(c -> act.run(c, EntityArgument.getPlayers(c, "joueurs"), IntegerArgumentType.getInteger(c, "secondes")))));
    }

    private static int on(CommandContext<CommandSourceStack> c, Act act) throws CommandSyntaxException {
        return act.run(c, EntityArgument.getPlayers(c, "joueurs"));
    }

    private static int show(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> t, String kind, int seconds) {
        HorrorService.show(t, kind, seconds * 20, "", null);
        return done(c, kind, t);
    }

    private static int balloon(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> t, Vec3 at) {
        HorrorService.show(t, "ballon", 400, "", at);
        return done(c, "ballon", t);
    }

    private static int sound(CommandContext<CommandSourceStack> c, boolean here) throws CommandSyntaxException {
        Collection<ServerPlayer> t = EntityArgument.getPlayers(c, "joueurs");
        String name = StringArgumentType.getString(c, "son");
        if (!HorrorService.sound(t, name, here ? c.getSource().getPosition().add(0, 1.6, 0) : null)) {
            c.getSource().sendFailure(Component.translatable("rp.eraphones.horror.unknown_sound", name, String.join(", ", HorrorService.sounds())));
            return 0;
        }
        return done(c, name, t);
    }

    private static int done(CommandContext<CommandSourceStack> c, String what, Collection<ServerPlayer> t) {
        c.getSource().sendSuccess(() -> Component.translatable("rp.eraphones.horror.sent", what, t.size()).withStyle(ChatFormatting.GRAY), false);
        return t.size();
    }

    private static int help(CommandContext<CommandSourceStack> c) {
        for (String tool : TOOLS) {
            c.getSource().sendSuccess(() -> Component.literal("/horreur " + tool + " ").withStyle(ChatFormatting.DARK_RED)
                    .append(Component.translatable("rp.eraphones.horror.help." + tool).withStyle(ChatFormatting.GRAY)), false);
        }
        c.getSource().sendSuccess(() -> Component.translatable("rp.eraphones.horror.sounds", String.join(", ", HorrorService.sounds()))
                .withStyle(ChatFormatting.DARK_GRAY), false);
        c.getSource().sendSuccess(() -> Component.literal("/voix ").withStyle(ChatFormatting.DARK_RED)
                .append(Component.translatable("rp.eraphones.horror.help.voix").withStyle(ChatFormatting.GRAY)), false);
        return TOOLS.length;
    }

    // ---------------------------------------------------------------- /voix

    private static Component voiceName(String id) {
        return Component.translatable("rp.eraphones.voice.name." + (id == null ? "normale" : id));
    }

    private static int voices(CommandContext<CommandSourceStack> c) {
        if (!Voices.available()) {
            c.getSource().sendFailure(Component.translatable("rp.eraphones.voice.missing"));
        }
        if (c.getSource().getEntity() instanceof ServerPlayer p) {
            c.getSource().sendSuccess(() -> Component.translatable("rp.eraphones.voice.current", voiceName(Voices.of(p.getUUID()))).withStyle(ChatFormatting.GRAY), false);
        }
        for (String v : VoiceFx.VOICES) {
            c.getSource().sendSuccess(() -> Component.literal("  " + v + "  ").withStyle(ChatFormatting.DARK_RED)
                    .append(Component.translatable("rp.eraphones.voice.desc." + v).withStyle(ChatFormatting.GRAY)), false);
        }
        c.getSource().sendSuccess(() -> Component.translatable("rp.eraphones.voice.usage").withStyle(ChatFormatting.DARK_GRAY), false);
        return VoiceFx.VOICES.length;
    }

    private static int voice(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> targets) {
        String id = StringArgumentType.getString(c, "voix");
        String voice = id.equals("normale") ? null : id;
        if (voice != null && !VoiceFx.exists(voice)) {
            c.getSource().sendFailure(Component.translatable("rp.eraphones.voice.unknown", id, String.join(", ", VoiceFx.VOICES)));
            return 0;
        }
        if (!Voices.available()) {
            c.getSource().sendFailure(Component.translatable("rp.eraphones.voice.missing"));
        }
        for (ServerPlayer p : targets) {
            Voices.set(p.getUUID(), voice);
            c.getSource().sendSuccess(() -> Component.translatable(voice == null ? "rp.eraphones.voice.normal" : "rp.eraphones.voice.set",
                    p.getDisplayName(), voiceName(voice)).withStyle(ChatFormatting.GRAY), false);
        }
        return targets.size();
    }

    private static int dose(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> targets) {
        int percent = IntegerArgumentType.getInteger(c, "pourcent");
        for (ServerPlayer p : targets) {
            Voices.setStrength(p.getUUID(), percent);
            c.getSource().sendSuccess(() -> Component.translatable("rp.eraphones.voice.dose_set", p.getDisplayName(), percent).withStyle(ChatFormatting.GRAY), false);
        }
        return targets.size();
    }

    private static int listen(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        boolean on = Voices.toggleListening(p.getUUID());
        c.getSource().sendSuccess(() -> Component.translatable(on ? "rp.eraphones.voice.listen_on" : "rp.eraphones.voice.listen_off").withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int gaugeSwitch(CommandContext<CommandSourceStack> c, boolean on) {
        Fear.setEnabled(c.getSource().getServer(), on);
        c.getSource().sendSuccess(() -> Component.translatable(on ? "rp.eraphones.fear.on" : "rp.eraphones.fear.off")
                .withStyle(ChatFormatting.DARK_RED), true);
        return 1;
    }

    private static int gaugeInfo(CommandContext<CommandSourceStack> c) {
        net.minecraft.server.MinecraftServer server = c.getSource().getServer();
        boolean on = Fear.enabled(server);
        c.getSource().sendSuccess(() -> Component.translatable(on ? "rp.eraphones.fear.state_on" : "rp.eraphones.fear.state_off")
                .withStyle(ChatFormatting.DARK_RED), false);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Component who = Component.literal("  " + com.waylays.eraphones.rp.RpService.name(p) + " (" + p.getGameProfile().getName() + ") : ");
            Component what = Fear.exempt(p) && !Fear.testing(p) ? Component.translatable("rp.eraphones.fear.staff").withStyle(ChatFormatting.DARK_GRAY)
                    : Component.literal(Math.round(Fear.get(p)) + " %").withStyle(Fear.get(p) > 66 ? ChatFormatting.RED : Fear.get(p) > 33 ? ChatFormatting.GOLD : ChatFormatting.GRAY);
            c.getSource().sendSuccess(() -> who.copy().withStyle(ChatFormatting.GRAY).append(what), false);
        }
        return 1;
    }
}
