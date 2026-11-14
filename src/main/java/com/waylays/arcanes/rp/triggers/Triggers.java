package com.waylays.arcanes.rp.triggers;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.rp.horror.HorrorService;
import com.waylays.arcanes.rp.RpConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Zone triggers, for a game master alone: an invisible box in the world; when a player walks into it, what was set
 * for it happens to them by itself (a sound behind them, a balloon, the fog, the lights out, a whisper, a message on
 * their phone, a street's power cut, their torch breaking, any command...), with pauses in between if wanted; once
 * only, once for each player, or every time. Players in creative or spectator set nothing off. Kept with the world.
 *
 * <p>/declencheur pos1|pos2 [x y z], creer &lt;nom&gt;, action &lt;nom&gt; &lt;what&gt; ..., mode, actif, tester, reset,
 * vider, supprimer, info, liste, montrer.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public final class Triggers extends SavedData {
    private static final String NAME = com.waylays.arcanes.rp.Legacy.PREFIX + "declencheurs";
    public static final String[] MODES = {"une_fois", "chaque_joueur", "toujours"};
    private static final String[] TIMED = {"brouillard", "coupure", "sang", "peur", "tremblement", "flotter"};
    private static final String[] TEXTS = {"murmure", "narration", "commande"};

    static final class Zone {
        String name;
        ResourceKey<Level> dim;
        BlockPos min, max;
        String mode = "une_fois";
        int every;
        boolean enabled = true, fired;
        final Set<UUID> firedFor = new HashSet<>();
        final Map<UUID, Long> last = new HashMap<>();
        final List<String> actions = new ArrayList<>();

        AABB box() {
            return new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1);
        }

        BlockPos center() {
            return BlockPos.containing(box().getCenter());
        }
    }

    private final Map<String, Zone> zones = new LinkedHashMap<>();

    // Not kept: the corners each game master picked, who is in which zone, what waits to happen, who sees the zones
    private static final Map<UUID, BlockPos> POS1 = new HashMap<>(), POS2 = new HashMap<>();
    private static final Map<String, Set<UUID>> INSIDE = new HashMap<>();
    private static final List<Pending> PENDING = new ArrayList<>();
    private static final Map<UUID, Long> SHOWING = new HashMap<>();
    /** The game masters who set the zones off themselves (even in creative), to try them: /declencheur moi. */
    private static final Set<UUID> TESTERS = new HashSet<>();

    private record Pending(String zone, UUID player, int from, long at) {}

    public static Triggers get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(Triggers::new, Triggers::load, null), NAME);
    }

    // ---------------------------------------------------------------- kept with the world

    private static Triggers load(CompoundTag tag, HolderLookup.Provider registries) {
        Triggers t = new Triggers();
        for (Tag e : tag.getList("Zones", Tag.TAG_COMPOUND)) {
            CompoundTag z = (CompoundTag) e;
            Zone zone = new Zone();
            zone.name = z.getString("Name");
            zone.dim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(z.getString("Dim")));
            zone.min = BlockPos.of(z.getLong("Min"));
            zone.max = BlockPos.of(z.getLong("Max"));
            zone.mode = z.getString("Mode");
            zone.every = z.getInt("Every");
            zone.enabled = z.getBoolean("Enabled");
            zone.fired = z.getBoolean("Fired");
            for (Tag a : z.getList("Actions", Tag.TAG_STRING)) {
                zone.actions.add(a.getAsString());
            }
            for (Tag a : z.getList("FiredFor", Tag.TAG_STRING)) {
                try {
                    zone.firedFor.add(UUID.fromString(a.getAsString()));
                } catch (IllegalArgumentException ignored) {
                    // Not a player's id
                }
            }
            t.zones.put(zone.name, zone);
        }
        return t;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Zone zone : zones.values()) {
            CompoundTag z = new CompoundTag();
            z.putString("Name", zone.name);
            z.putString("Dim", zone.dim.location().toString());
            z.putLong("Min", zone.min.asLong());
            z.putLong("Max", zone.max.asLong());
            z.putString("Mode", zone.mode);
            z.putInt("Every", zone.every);
            z.putBoolean("Enabled", zone.enabled);
            z.putBoolean("Fired", zone.fired);
            ListTag actions = new ListTag();
            for (String a : zone.actions) {
                actions.add(StringTag.valueOf(a));
            }
            z.put("Actions", actions);
            ListTag fired = new ListTag();
            for (UUID id : zone.firedFor) {
                fired.add(StringTag.valueOf(id.toString()));
            }
            z.put("FiredFor", fired);
            list.add(z);
        }
        tag.put("Zones", list);
        return tag;
    }

    // ---------------------------------------------------------------- watching the zones

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long now = server.overworld().getGameTime();
        if (!PENDING.isEmpty()) {
            List<Pending> due = new ArrayList<>();
            PENDING.removeIf(p -> {
                if (p.at <= now) {
                    due.add(p);
                    return true;
                }
                return false;
            });
            Triggers t = get(server);
            for (Pending p : due) {
                Zone z = t.zones.get(p.zone);
                ServerPlayer pl = server.getPlayerList().getPlayer(p.player);
                if (z != null && pl != null) {
                    t.run(z, pl, p.from, now);
                }
            }
        }
        if (now % 5 != 0) {
            return;
        }
        Triggers t = get(server);
        for (Zone z : new ArrayList<>(t.zones.values())) {
            ServerLevel level = server.getLevel(z.dim);
            Set<UUID> inside = INSIDE.computeIfAbsent(z.name, k -> new HashSet<>());
            if (level == null || !z.enabled) {
                inside.clear();
                continue;
            }
            AABB box = z.box();
            Set<UUID> now_in = new HashSet<>();
            for (ServerPlayer p : level.players()) {
                if ((p.isCreative() && !TESTERS.contains(p.getUUID())) || p.isSpectator() || !box.contains(p.position())) {
                    continue;
                }
                now_in.add(p.getUUID());
                if (!inside.contains(p.getUUID())) {
                    t.fire(z, p, now, false);
                }
            }
            inside.clear();
            inside.addAll(now_in);
        }
        if (now % 10 == 0 && !SHOWING.isEmpty()) {
            show(server, t, now);
        }
    }

    /** {@code p} walked into {@code z}: what is set for it happens, if its mode lets it (always, when tested). */
    private void fire(Zone z, ServerPlayer p, long now, boolean test) {
        if (!test) {
            switch (z.mode) {
                case "chaque_joueur" -> {
                    if (!z.firedFor.add(p.getUUID())) {
                        return;
                    }
                }
                case "toujours" -> {
                    Long last = z.last.get(p.getUUID());
                    if (last != null && now - last < z.every * 20L) {
                        return;
                    }
                    z.last.put(p.getUUID(), now);
                }
                default -> {
                    if (z.fired) {
                        return;
                    }
                    z.fired = true;
                }
            }
            setDirty();
        }
        run(z, p, 0, now);
        // The game masters see it go off, above their hotbar
        Component note = Component.translatable("event.arcanes_oublies.trigger.fired", z.name, p.getGameProfile().getName()).withStyle(ChatFormatting.GOLD);
        for (ServerPlayer gm : p.server.getPlayerList().getPlayers()) {
            if (gm.hasPermissions(RpConfig.get(RpConfig.HORROR_PERMISSION, 2))) {
                gm.displayClientMessage(note, true);
            }
        }
    }

    /** Does the zone's actions from {@code from} on, until a pause (the rest waits for it). */
    private void run(Zone z, ServerPlayer p, int from, long now) {
        for (int i = from; i < z.actions.size(); i++) {
            String a = z.actions.get(i);
            int sp = a.indexOf(' ');
            String kind = sp < 0 ? a : a.substring(0, sp), arg = sp < 0 ? "" : a.substring(sp + 1);
            if (kind.equals("attendre")) {
                PENDING.add(new Pending(z.name, p.getUUID(), i + 1, now + Math.max(1, num(arg, 1)) * 20L));
                return;
            }
            try {
                act(z, p, kind, arg);
            } catch (RuntimeException e) {
                ArcanesOublies.LOGGER.warn("Trigger {}: \"{}\" failed: {}", z.name, a, e.toString());
            }
        }
    }

    private static int num(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim().split(" ")[0]);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static void act(Zone z, ServerPlayer p, String kind, String arg) {
        List<ServerPlayer> one = List.of(p);
        switch (kind) {
            case "son" -> HorrorService.sound(one, arg, null);
            case "ballon" -> HorrorService.show(one, "ballon", 400, "", null);
            case "yeux" -> HorrorService.show(one, "yeux", com.waylays.arcanes.rp.RpService.EYES_TICKS, "", null);
            case "flotter" -> HorrorService.levitate(one, num(arg, 6) * 20);
            case "murmure" -> HorrorService.show(one, "murmure", 60 + Math.min(400, arg.length() * 3), arg, null);
            case "narration" -> {
                HorrorService.show(one, "narration", 80 + Math.min(500, arg.length() * 4), arg, null);
                p.sendSystemMessage(Component.literal("✦ " + arg).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
            // With Époque RP: its power cuts, flashlights and phones, through its commands
            case "panne", "clignote" -> {
                String[] w = arg.split(" ");
                epoque(p, Vec3.atCenterOf(z.center()), "panne " + (kind.equals("clignote") ? "clignote " : "") + num(w[0], 24) + " " + (w.length > 1 ? num(w[1], 60) : 60));
            }
            case "lampe" -> epoque(p, p.position(), "epoquerp lampe_casser " + p.getGameProfile().getName());
            case "sms" -> {
                int sp = arg.indexOf(' ');
                if (sp > 0) {
                    epoque(p, p.position(), "epoquerp sms " + p.getGameProfile().getName() + " " + arg.substring(0, sp) + " " + arg.substring(sp + 1));
                }
            }
            case "commande" -> {
                MinecraftServer server = p.server;
                CommandSourceStack src = server.createCommandSourceStack().withEntity(p).withPosition(p.position())
                        .withRotation(p.getRotationVector()).withLevel(p.serverLevel()).withPermission(2).withSuppressedOutput();
                server.getCommands().performPrefixedCommand(src, arg.replace("{joueur}", p.getGameProfile().getName()));
            }
            default -> {
                for (String t : TIMED) {
                    if (t.equals(kind)) {
                        HorrorService.show(one, kind, num(arg, 8) * 20, "", null);
                    }
                }
            }
        }
    }

    /** Runs one of Époque RP's commands at {@code at}, if that mod is there (nothing otherwise). */
    private static void epoque(ServerPlayer p, net.minecraft.world.phys.Vec3 at, String command) {
        MinecraftServer server = p.server;
        if (server.getCommands().getDispatcher().getRoot().getChild(command.substring(0, command.indexOf(' '))) == null) {
            return;
        }
        CommandSourceStack src = server.createCommandSourceStack().withPosition(at).withLevel(p.serverLevel()).withPermission(4).withSuppressedOutput();
        server.getCommands().performPrefixedCommand(src, command);
    }

    /** The zones drawn in dust for the game masters who asked to see them: their edges, in red (in yellow if off). */
    private static void show(MinecraftServer server, Triggers t, long now) {
        SHOWING.entrySet().removeIf(e -> e.getValue() < now || server.getPlayerList().getPlayer(e.getKey()) == null);
        for (UUID id : SHOWING.keySet()) {
            ServerPlayer gm = server.getPlayerList().getPlayer(id);
            if (gm == null) {
                continue;
            }
            for (Zone z : t.zones.values()) {
                if (!z.dim.equals(gm.level().dimension()) || z.center().distSqr(gm.blockPosition()) > 96 * 96) {
                    continue;
                }
                DustParticleOptions dust = new DustParticleOptions(z.enabled ? new Vector3f(1f, .15f, .1f) : new Vector3f(1f, .85f, .2f), 1.2f);
                AABB b = z.box();
                double step = Math.max(.5, Math.max(b.getXsize(), Math.max(b.getYsize(), b.getZsize())) / 24);
                for (double[] e : edges(b)) {
                    double len = Math.sqrt(Math.pow(e[3] - e[0], 2) + Math.pow(e[4] - e[1], 2) + Math.pow(e[5] - e[2], 2));
                    for (double k = 0; k <= len; k += step) {
                        double f = len == 0 ? 0 : k / len;
                        gm.serverLevel().sendParticles(gm, dust, true, e[0] + (e[3] - e[0]) * f, e[1] + (e[4] - e[1]) * f, e[2] + (e[5] - e[2]) * f,
                                1, 0, 0, 0, 0);
                    }
                }
            }
        }
    }

    private static List<double[]> edges(AABB b) {
        List<double[]> out = new ArrayList<>();
        double[] xs = {b.minX, b.maxX}, ys = {b.minY, b.maxY}, zs = {b.minZ, b.maxZ};
        for (double y : ys) {
            for (double z : zs) {
                out.add(new double[] {b.minX, y, z, b.maxX, y, z});
            }
            for (double x : xs) {
                out.add(new double[] {x, y, b.minZ, x, y, b.maxZ});
            }
        }
        for (double x : xs) {
            for (double z : zs) {
                out.add(new double[] {x, b.minY, z, x, b.maxY, z});
            }
        }
        return out;
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        POS1.clear();
        POS2.clear();
        TESTERS.clear();
        INSIDE.clear();
        PENDING.clear();
        SHOWING.clear();
    }

    /** For the tests: a zone from {@code min} to {@code max} doing {@code action} each time anyone walks in. */
    public static void putForTest(ServerLevel level, String name, BlockPos min, BlockPos max, String action) {
        Triggers t = get(level.getServer());
        Zone z = new Zone();
        z.name = name;
        z.dim = level.dimension();
        z.min = min;
        z.max = max;
        z.mode = "toujours";
        z.actions.add(action);
        t.zones.put(name, z);
    }

    /** For the tests: what the server's watch does for one player now (walking into a zone sets it off). */
    public static void watchForTest(ServerPlayer p) {
        Triggers t = get(p.server);
        long now = p.server.overworld().getGameTime();
        for (Zone z : new ArrayList<>(t.zones.values())) {
            if (!z.enabled || !z.dim.equals(p.level().dimension()) || (p.isCreative() && !TESTERS.contains(p.getUUID())) || p.isSpectator()) {
                continue;
            }
            Set<UUID> inside = INSIDE.computeIfAbsent(z.name, k -> new HashSet<>());
            if (z.box().contains(p.position()) && inside.add(p.getUUID())) {
                t.fire(z, p, now, false);
            }
        }
    }

    public static void removeForTest(MinecraftServer server, String name) {
        get(server).zones.remove(name);
        INSIDE.remove(name);
    }

    // ---------------------------------------------------------------- /declencheur

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> named() {
        return Commands.argument("nom", StringArgumentType.word()).suggests((c, b) ->
                SharedSuggestionProvider.suggest(get(c.getSource().getServer()).zones.keySet(), b));
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("declencheur")
                .requires(s -> s.hasPermission(RpConfig.get(RpConfig.HORROR_PERMISSION, 2)))
                .executes(Triggers::help);
        root.then(corner("pos1", POS1)).then(corner("pos2", POS2));
        root.then(Commands.literal("creer").then(named().executes(Triggers::create)));
        root.then(Commands.literal("supprimer").then(named().executes(c -> edit(c, (t, z) -> {
            t.zones.remove(z.name);
            INSIDE.remove(z.name);
            return "event.arcanes_oublies.trigger.deleted";
        }))));
        root.then(Commands.literal("vider").then(named().executes(c -> edit(c, (t, z) -> {
            z.actions.clear();
            return "event.arcanes_oublies.trigger.cleared";
        }))));
        root.then(Commands.literal("reset").then(named().executes(c -> edit(c, (t, z) -> {
            z.fired = false;
            z.firedFor.clear();
            z.last.clear();
            return "event.arcanes_oublies.trigger.reset";
        }))));
        root.then(Commands.literal("actif").then(named().then(Commands.argument("oui", BoolArgumentType.bool()).executes(c -> edit(c, (t, z) -> {
            z.enabled = BoolArgumentType.getBool(c, "oui");
            return z.enabled ? "event.arcanes_oublies.trigger.on" : "event.arcanes_oublies.trigger.off";
        })))));
        LiteralArgumentBuilder<CommandSourceStack> mode = Commands.literal("mode");
        ArgumentBuilder<CommandSourceStack, ?> modeNamed = named();
        for (String m : MODES) {
            ArgumentBuilder<CommandSourceStack, ?> lit = Commands.literal(m).executes(c -> setMode(c, m, 0));
            if (m.equals("toujours")) {
                lit.then(Commands.argument("secondes", IntegerArgumentType.integer(0, 86400))
                        .executes(c -> setMode(c, m, IntegerArgumentType.getInteger(c, "secondes"))));
            }
            modeNamed.then(lit);
        }
        root.then(mode.then(modeNamed));
        root.then(Commands.literal("action").then(actions(named())));
        root.then(Commands.literal("tester").then(named()
                .executes(c -> test(c, List.of(c.getSource().getPlayerOrException())))
                .then(Commands.argument("joueur", EntityArgument.players()).executes(c -> test(c, EntityArgument.getPlayers(c, "joueur"))))));
        root.then(Commands.literal("info").then(named().executes(Triggers::info)));
        root.then(Commands.literal("liste").executes(Triggers::list));
        root.then(Commands.literal("moi").executes(c -> {
            ServerPlayer gm = c.getSource().getPlayerOrException();
            boolean on = TESTERS.add(gm.getUUID());
            if (!on) {
                TESTERS.remove(gm.getUUID());
            }
            INSIDE.values().forEach(set -> set.remove(gm.getUUID()));
            c.getSource().sendSuccess(() -> Component.translatable(on ? "event.arcanes_oublies.trigger.me_on" : "event.arcanes_oublies.trigger.me_off")
                    .withStyle(ChatFormatting.GOLD), false);
            return 1;
        }));
        root.then(Commands.literal("montrer").executes(c -> showZones(c, 60))
                .then(Commands.argument("secondes", IntegerArgumentType.integer(0, 3600)).executes(c -> showZones(c, IntegerArgumentType.getInteger(c, "secondes")))));
        d.register(root);
    }

    private static ArgumentBuilder<CommandSourceStack, ?> corner(String which, Map<UUID, BlockPos> into) {
        return Commands.literal(which).executes(c -> corner(c, into, c.getSource().getPlayerOrException().blockPosition(), which))
                .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(c -> corner(c, into, BlockPosArgument.getBlockPos(c, "pos"), which)));
    }

    private static int corner(CommandContext<CommandSourceStack> c, Map<UUID, BlockPos> into, BlockPos pos, String which) throws CommandSyntaxException {
        into.put(c.getSource().getPlayerOrException().getUUID(), pos);
        c.getSource().sendSuccess(() -> Component.translatable("event.arcanes_oublies.trigger.corner", which, pos.getX(), pos.getY(), pos.getZ())
                .withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int create(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer gm = c.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(c, "nom");
        BlockPos a = POS1.get(gm.getUUID()), b = POS2.get(gm.getUUID());
        if (a == null || b == null) {
            c.getSource().sendFailure(Component.translatable("event.arcanes_oublies.trigger.no_corners"));
            return 0;
        }
        Triggers t = get(c.getSource().getServer());
        Zone z = t.zones.getOrDefault(name, new Zone());
        z.name = name;
        z.dim = gm.level().dimension();
        z.min = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
        z.max = new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
        t.zones.put(name, z);
        t.setDirty();
        c.getSource().sendSuccess(() -> Component.translatable("event.arcanes_oublies.trigger.created", name,
                z.max.getX() - z.min.getX() + 1, z.max.getY() - z.min.getY() + 1, z.max.getZ() - z.min.getZ() + 1).withStyle(ChatFormatting.GOLD), true);
        SHOWING.put(gm.getUUID(), gm.level().getGameTime() + 200);
        if (gm.isCreative() && !TESTERS.contains(gm.getUUID())) {
            c.getSource().sendSuccess(() -> Component.translatable("event.arcanes_oublies.trigger.creative_hint").withStyle(ChatFormatting.YELLOW), false);
        }
        return 1;
    }

    private interface Edit {
        String apply(Triggers t, Zone z) throws CommandSyntaxException;
    }

    private static int edit(CommandContext<CommandSourceStack> c, Edit e) throws CommandSyntaxException {
        Triggers t = get(c.getSource().getServer());
        String name = StringArgumentType.getString(c, "nom");
        Zone z = t.zones.get(name);
        if (z == null) {
            c.getSource().sendFailure(Component.translatable("event.arcanes_oublies.trigger.unknown", name));
            return 0;
        }
        String key = e.apply(t, z);
        t.setDirty();
        c.getSource().sendSuccess(() -> Component.translatable(key, name).withStyle(ChatFormatting.GRAY), true);
        return 1;
    }

    private static int setMode(CommandContext<CommandSourceStack> c, String mode, int seconds) throws CommandSyntaxException {
        return edit(c, (t, z) -> {
            z.mode = mode;
            z.every = seconds;
            z.fired = false;
            z.firedFor.clear();
            z.last.clear();
            return "event.arcanes_oublies.trigger.mode." + mode;
        });
    }

    /** /declencheur action &lt;nom&gt; ... : each kind of action, with what it needs. */
    private static ArgumentBuilder<CommandSourceStack, ?> actions(ArgumentBuilder<CommandSourceStack, ?> zone) {
        zone.then(Commands.literal("son").then(Commands.argument("son", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(HorrorService.sounds(), b))
                .executes(c -> add(c, "son " + StringArgumentType.getString(c, "son")))));
        for (String simple : new String[] {"ballon", "yeux", "lampe"}) {
            zone.then(Commands.literal(simple).executes(c -> add(c, simple)));
        }
        for (String timed : TIMED) {
            zone.then(Commands.literal(timed).executes(c -> add(c, timed + " 8"))
                    .then(Commands.argument("secondes", IntegerArgumentType.integer(1, 600))
                            .executes(c -> add(c, timed + " " + IntegerArgumentType.getInteger(c, "secondes")))));
        }
        for (String text : TEXTS) {
            zone.then(Commands.literal(text).then(Commands.argument("texte", StringArgumentType.greedyString())
                    .executes(c -> add(c, text + " " + StringArgumentType.getString(c, "texte")))));
        }
        for (String cut : new String[] {"panne", "clignote"}) {
            zone.then(Commands.literal(cut).then(Commands.argument("rayon", IntegerArgumentType.integer(1, 256))
                    .then(Commands.argument("secondes", IntegerArgumentType.integer(1, 3600)).executes(c -> add(c, cut + " "
                            + IntegerArgumentType.getInteger(c, "rayon") + " " + IntegerArgumentType.getInteger(c, "secondes"))))));
        }
        zone.then(Commands.literal("sms").then(Commands.argument("de", StringArgumentType.word())
                .then(Commands.argument("texte", StringArgumentType.greedyString())
                        .executes(c -> add(c, "sms " + StringArgumentType.getString(c, "de") + " " + StringArgumentType.getString(c, "texte"))))));
        zone.then(Commands.literal("attendre").then(Commands.argument("secondes", IntegerArgumentType.integer(1, 3600))
                .executes(c -> add(c, "attendre " + IntegerArgumentType.getInteger(c, "secondes")))));
        return zone;
    }

    private static int add(CommandContext<CommandSourceStack> c, String action) throws CommandSyntaxException {
        Triggers t = get(c.getSource().getServer());
        String name = StringArgumentType.getString(c, "nom");
        Zone z = t.zones.get(name);
        if (z == null) {
            c.getSource().sendFailure(Component.translatable("event.arcanes_oublies.trigger.unknown", name));
            return 0;
        }
        z.actions.add(action);
        t.setDirty();
        int n = z.actions.size();
        c.getSource().sendSuccess(() -> Component.translatable("event.arcanes_oublies.trigger.added", n, action, name).withStyle(ChatFormatting.GRAY), false);
        return n;
    }

    private static int test(CommandContext<CommandSourceStack> c, java.util.Collection<ServerPlayer> who) {
        Triggers t = get(c.getSource().getServer());
        String name = StringArgumentType.getString(c, "nom");
        Zone z = t.zones.get(name);
        if (z == null) {
            c.getSource().sendFailure(Component.translatable("event.arcanes_oublies.trigger.unknown", name));
            return 0;
        }
        for (ServerPlayer p : who) {
            t.fire(z, p, c.getSource().getServer().overworld().getGameTime(), true);
        }
        c.getSource().sendSuccess(() -> Component.translatable("event.arcanes_oublies.trigger.tested", name, who.size()).withStyle(ChatFormatting.GRAY), false);
        return who.size();
    }

    private static int info(CommandContext<CommandSourceStack> c) {
        Triggers t = get(c.getSource().getServer());
        String name = StringArgumentType.getString(c, "nom");
        Zone z = t.zones.get(name);
        if (z == null) {
            c.getSource().sendFailure(Component.translatable("event.arcanes_oublies.trigger.unknown", name));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.translatable("event.arcanes_oublies.trigger.info", z.name, z.min.toShortString(), z.max.toShortString(),
                Component.translatable("event.arcanes_oublies.trigger.mode_name." + z.mode), Component.translatable(z.enabled
                        ? "event.arcanes_oublies.trigger.state_on" : "event.arcanes_oublies.trigger.state_off")).withStyle(ChatFormatting.GOLD), false);
        for (int i = 0; i < z.actions.size(); i++) {
            int n = i + 1;
            String a = z.actions.get(i);
            c.getSource().sendSuccess(() -> Component.literal("  " + n + ". " + a).withStyle(ChatFormatting.GRAY), false);
        }
        if (z.actions.isEmpty()) {
            c.getSource().sendSuccess(() -> Component.translatable("event.arcanes_oublies.trigger.no_actions").withStyle(ChatFormatting.DARK_GRAY), false);
        }
        if (z.mode.equals("une_fois") && z.fired) {
            c.getSource().sendSuccess(() -> Component.translatable("event.arcanes_oublies.trigger.spent", z.name).withStyle(ChatFormatting.YELLOW), false);
        }
        return z.actions.size();
    }

    private static int list(CommandContext<CommandSourceStack> c) {
        Triggers t = get(c.getSource().getServer());
        if (t.zones.isEmpty()) {
            c.getSource().sendSuccess(() -> Component.translatable("event.arcanes_oublies.trigger.none").withStyle(ChatFormatting.GRAY), false);
        }
        for (Zone z : t.zones.values()) {
            c.getSource().sendSuccess(() -> Component.literal((z.enabled ? "● " : "○ ") + z.name + "  ").withStyle(z.enabled
                    ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY).append(Component.literal(z.center().toShortString() + "  · " + z.actions.size() + "  · ")
                    .withStyle(ChatFormatting.GRAY)).append(Component.translatable("event.arcanes_oublies.trigger.mode_name." + z.mode).withStyle(ChatFormatting.GRAY)),
                    false);
        }
        return t.zones.size();
    }

    private static int showZones(CommandContext<CommandSourceStack> c, int seconds) throws CommandSyntaxException {
        ServerPlayer gm = c.getSource().getPlayerOrException();
        if (seconds == 0) {
            SHOWING.remove(gm.getUUID());
        } else {
            SHOWING.put(gm.getUUID(), gm.level().getGameTime() + seconds * 20L);
        }
        c.getSource().sendSuccess(() -> Component.translatable(seconds == 0 ? "event.arcanes_oublies.trigger.hidden" : "event.arcanes_oublies.trigger.shown", seconds)
                .withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int help(CommandContext<CommandSourceStack> c) {
        for (String line : new String[] {"pos", "creer", "action", "attendre", "mode", "tester", "moi", "gerer"}) {
            c.getSource().sendSuccess(() -> Component.translatable("event.arcanes_oublies.trigger.help." + line).withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }
}
