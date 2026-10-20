package com.waylays.arcanes.spells.boss;

import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Server side: the later steps of a Boss Magic attack (a rift that cracks open a little further every few ticks,
 * stars marked one after another). Unlike a fighter's move, an attack goes on if its caster falls: a step belongs to
 * its level, not to the caster.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public final class BossScheduler {
    private record Step(ServerLevel level, long at, Consumer<ServerLevel> action) {}

    private static final List<Step> STEPS = new ArrayList<>();

    private BossScheduler() {}

    /** Runs {@code action} in {@code level} in {@code ticks} ticks (right away for 0). */
    public static void after(ServerLevel level, int ticks, Consumer<ServerLevel> action) {
        if (ticks <= 0) {
            action.accept(level);
        } else {
            STEPS.add(new Step(level, level.getGameTime() + ticks, action));
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (STEPS.isEmpty() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        List<Step> due = new ArrayList<>();
        for (Iterator<Step> it = STEPS.iterator(); it.hasNext(); ) {
            Step step = it.next();
            if (step.level() == level && level.getGameTime() >= step.at()) {
                due.add(step);
                it.remove();
            }
        }
        // Run after the sweep: a step may schedule more steps
        due.forEach(step -> step.action().accept(level));
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        STEPS.clear();
    }
}
