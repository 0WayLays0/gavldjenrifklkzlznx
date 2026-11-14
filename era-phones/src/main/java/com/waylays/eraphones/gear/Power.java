package com.waylays.eraphones.gear;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.registry.GearRegistry;
import com.waylays.eraphones.EraConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The neighbourhood's electricity, which a game master can cut: within a radius the street lamps buzz and go out,
 * the televisions die, the video recorders stop, the phone booths go dark and dead. Or it can only falter: the lamps
 * flicker. Cuts last until ended or for a number of seconds; they do not outlast the server.
 */
@EventBusSubscriber(modid = EraPhones.MODID)
public final class Power {
    record Outage(ResourceKey<Level> dimension, BlockPos center, int radius, long end, boolean flicker) {
        boolean covers(Level level, BlockPos pos) {
            return level.dimension() == dimension && pos.distSqr(center) <= (double) radius * radius
                    && (end < 0 || level.getGameTime() < end);
        }
    }

    private static final List<Outage> OUTAGES = new CopyOnWriteArrayList<>();

    private Power() {}

    /** Whether the power is off at {@code pos}. */
    public static boolean cut(Level level, BlockPos pos) {
        for (Outage o : OUTAGES) {
            if (!o.flicker && o.covers(level, pos)) {
                return true;
            }
        }
        return false;
    }

    /** Whether the power falters at {@code pos} (the lamps flicker). */
    public static boolean flickers(Level level, BlockPos pos) {
        for (Outage o : OUTAGES) {
            if (o.flicker && o.covers(level, pos)) {
                return true;
            }
        }
        return false;
    }

    public static void start(ServerLevel level, BlockPos center, int radius, int seconds, boolean flicker) {
        long end = seconds > 0 ? level.getGameTime() + seconds * 20L : -1;
        OUTAGES.add(new Outage(level.dimension(), center.immutable(), radius, end, flicker));
        if (!flicker) {
            for (ServerPlayer p : level.players()) {
                if (p.blockPosition().distSqr(center) <= (double) (radius + 16) * (radius + 16)) {
                    p.playNotifySound(GearRegistry.POWER_DOWN.get(), SoundSource.BLOCKS, .9f, 1f);
                }
            }
        }
    }

    /** Ends the cuts over {@code pos} (all of them if null); how many. */
    public static int end(ServerLevel level, BlockPos pos) {
        int n = 0;
        for (Outage o : OUTAGES) {
            if (pos == null || o.covers(level, pos)) {
                OUTAGES.remove(o);
                n++;
            }
        }
        return n;
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("panne").requires(s -> s.hasPermission(EraConfig.staffPermission()))
                .then(Commands.literal("fin").executes(c -> stop(c)))
                .then(Commands.literal("clignote").then(Commands.argument("rayon", IntegerArgumentType.integer(1, 256))
                        .executes(c -> cut(c, 10, true))
                        .then(Commands.argument("secondes", IntegerArgumentType.integer(1, 3600)).executes(c -> cut(c, IntegerArgumentType.getInteger(c, "secondes"), true)))))
                .then(Commands.argument("rayon", IntegerArgumentType.integer(1, 256))
                        .executes(c -> cut(c, 0, false))
                        .then(Commands.argument("secondes", IntegerArgumentType.integer(1, 86400)).executes(c -> cut(c, IntegerArgumentType.getInteger(c, "secondes"), false)))));
    }

    private static int cut(CommandContext<CommandSourceStack> c, int seconds, boolean flicker) {
        int radius = IntegerArgumentType.getInteger(c, "rayon");
        start(c.getSource().getLevel(), BlockPos.containing(c.getSource().getPosition()), radius, seconds, flicker);
        c.getSource().sendSuccess(() -> Component.translatable(flicker ? "power.eraphones.flicker" : "power.eraphones.cut", radius), true);
        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> c) {
        int n = end(c.getSource().getLevel(), null);
        c.getSource().sendSuccess(() -> Component.translatable("power.eraphones.back", n), true);
        return n;
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        OUTAGES.clear();
    }
}
