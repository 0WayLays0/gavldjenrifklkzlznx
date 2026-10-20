package com.waylays.arcanes.spells.physical;

import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Server side: the beats of a fighting move played out over the next ticks (each jab of a flurry, each step of a dash,
 * the strike timed on the swing of the animation). A beat is dropped if its fighter has died or left the world.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public final class SkillTimeline {
    private record Beat(LivingEntity fighter, long at, Consumer<LivingEntity> action) {}

    private static final List<Beat> BEATS = new ArrayList<>();
    private static long now;

    private SkillTimeline() {}

    /** Runs {@code action} on the fighter in {@code ticks} ticks (right away for 0). */
    public static void after(LivingEntity fighter, int ticks, Consumer<LivingEntity> action) {
        if (ticks <= 0) {
            action.accept(fighter);
        } else {
            BEATS.add(new Beat(fighter, now + ticks, action));
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        now++;
        if (BEATS.isEmpty()) {
            return;
        }
        List<Beat> due = new ArrayList<>();
        for (Iterator<Beat> it = BEATS.iterator(); it.hasNext(); ) {
            Beat beat = it.next();
            if (!beat.fighter().isAlive()) {
                it.remove();
            } else if (beat.at() <= now) {
                it.remove();
                due.add(beat);
            }
        }
        for (Beat beat : due) {
            try {
                beat.action().accept(beat.fighter());
            } catch (RuntimeException e) {
                // A broken move must never take the server down
                ArcanesOublies.LOGGER.error("Fighting move beat failed for {}", beat.fighter(), e);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        BEATS.clear();
    }
}
