package com.waylays.arcanes.rp.warp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.rp.RpConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Warps: named places of the world one goes back to. /setwarp and /delwarp for game masters, /warp and /warplist for
 * everyone (both levels in the server's role-play settings). A warp's name is written as one likes and looked up
 * whatever its capitals ("Taverne", "taverne" and "TAVERNE" are the same warp).
 */
public final class Warps extends SavedData {
    private static final String NAME = ArcanesOublies.MODID + "_warps";

    /** A warp: its name as written, its world and where one stands there, looking which way. */
    public record Warp(String name, ResourceLocation world, double x, double y, double z, float yaw, float pitch) {}

    /** The warps by lower-case name. */
    private final Map<String, Warp> warps = new TreeMap<>();

    public static Warps get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Warps::new, Warps::load, null), NAME);
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public Warp find(String name) {
        return warps.get(key(name));
    }

    public Iterable<Warp> all() {
        return warps.values();
    }

    public int size() {
        return warps.size();
    }

    /** Sets (or moves) a warp; true if it already existed. */
    public boolean put(Warp w) {
        boolean had = warps.put(key(w.name()), w) != null;
        setDirty();
        return had;
    }

    public Warp remove(String name) {
        Warp w = warps.remove(key(name));
        if (w != null) {
            setDirty();
        }
        return w;
    }

    private static Warps load(CompoundTag tag, HolderLookup.Provider registries) {
        Warps data = new Warps();
        CompoundTag all = tag.getCompound("warps");
        for (String k : all.getAllKeys()) {
            CompoundTag w = all.getCompound(k);
            ResourceLocation world = ResourceLocation.tryParse(w.getString("world"));
            if (world != null) {
                Warp warp = new Warp(w.getString("name"), world, w.getDouble("x"), w.getDouble("y"), w.getDouble("z"), w.getFloat("yaw"), w.getFloat("pitch"));
                data.warps.put(key(warp.name()), warp);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag all = new CompoundTag();
        for (Map.Entry<String, Warp> e : warps.entrySet()) {
            Warp w = e.getValue();
            CompoundTag t = new CompoundTag();
            t.putString("name", w.name());
            t.putString("world", w.world().toString());
            t.putDouble("x", w.x());
            t.putDouble("y", w.y());
            t.putDouble("z", w.z());
            t.putFloat("yaw", w.yaw());
            t.putFloat("pitch", w.pitch());
            all.put(e.getKey(), t);
        }
        tag.put("warps", all);
        return tag;
    }

    // ---------------------------------------------------------------- commands

    private static final SuggestionProvider<CommandSourceStack> NAMES = (c, b) -> {
        java.util.List<String> names = new java.util.ArrayList<>();
        for (Warp w : get(c.getSource().getServer()).all()) {
            names.add(w.name());
        }
        return SharedSuggestionProvider.suggest(names, b);
    };

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("warp").requires(s -> s.hasPermission(RpConfig.get(RpConfig.WARP_PERMISSION, 0)))
                .then(Commands.argument("nom", StringArgumentType.word()).suggests(NAMES).executes(Warps::warp)));
        d.register(Commands.literal("warplist").requires(s -> s.hasPermission(RpConfig.get(RpConfig.WARP_PERMISSION, 0)))
                .executes(Warps::list));
        d.register(Commands.literal("setwarp").requires(s -> s.hasPermission(RpConfig.get(RpConfig.SETWARP_PERMISSION, 2)))
                .then(Commands.argument("nom", StringArgumentType.word()).suggests(NAMES).executes(Warps::set)));
        d.register(Commands.literal("delwarp").requires(s -> s.hasPermission(RpConfig.get(RpConfig.SETWARP_PERMISSION, 2)))
                .then(Commands.argument("nom", StringArgumentType.word()).suggests(NAMES).executes(Warps::delete)));
    }

    private static int warp(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(c, "nom");
        Warp w = get(p.server).find(name);
        if (w == null) {
            c.getSource().sendFailure(Component.translatable("rp.arcanes_oublies.warp.unknown", name));
            return 0;
        }
        ServerLevel level = p.server.getLevel(ResourceKey.create(Registries.DIMENSION, w.world()));
        if (level == null) {
            c.getSource().sendFailure(Component.translatable("rp.arcanes_oublies.warp.no_world", w.name()));
            return 0;
        }
        p.teleportTo(level, w.x(), w.y(), w.z(), w.yaw(), w.pitch());
        level.playSound(null, w.x(), w.y(), w.z(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, .6f, 1.2f);
        c.getSource().sendSuccess(() -> Component.translatable("rp.arcanes_oublies.warp.done", gold(w.name())).withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> c) {
        Warps data = get(c.getSource().getServer());
        if (data.size() == 0) {
            c.getSource().sendSuccess(() -> Component.translatable("rp.arcanes_oublies.warp.none").withStyle(ChatFormatting.GRAY), false);
            return 0;
        }
        MutableComponent line = Component.translatable("rp.arcanes_oublies.warp.list", data.size()).withStyle(ChatFormatting.GRAY).append(" ");
        boolean first = true;
        for (Warp w : data.all()) {
            if (!first) {
                line.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY));
            }
            first = false;
            // Each name can be clicked to go there
            line.append(gold(w.name()).withStyle(s -> s.withUnderlined(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/warp " + w.name()))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("rp.arcanes_oublies.warp.hover")))));
        }
        c.getSource().sendSuccess(() -> line, false);
        return data.size();
    }

    private static int set(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(c, "nom");
        if (!name.matches("[\\p{L}0-9_\\-]{1,32}")) {
            c.getSource().sendFailure(Component.translatable("rp.arcanes_oublies.warp.bad_name"));
            return 0;
        }
        Level level = p.level();
        boolean moved = get(p.server).put(new Warp(name, level.dimension().location(), p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot()));
        c.getSource().sendSuccess(() -> Component.translatable(moved ? "rp.arcanes_oublies.warp.moved" : "rp.arcanes_oublies.warp.set", gold(name))
                .withStyle(ChatFormatting.GRAY), true);
        return 1;
    }

    private static int delete(CommandContext<CommandSourceStack> c) {
        String name = StringArgumentType.getString(c, "nom");
        Warp w = get(c.getSource().getServer()).remove(name);
        if (w == null) {
            c.getSource().sendFailure(Component.translatable("rp.arcanes_oublies.warp.unknown", name));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.translatable("rp.arcanes_oublies.warp.deleted", gold(w.name())).withStyle(ChatFormatting.GRAY), true);
        return 1;
    }

    private static MutableComponent gold(String s) {
        return Component.literal(s).withStyle(ChatFormatting.GOLD);
    }
}
