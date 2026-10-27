package com.waylays.arcanes.spells.cosmos;

import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The past of every living thing, as time magic sees it: where it stood, which way it looked and how hurt it was,
 * a few times a second over the last {@link #SPAN} ticks. The Reset Charge sends its targets back to where they
 * were and gives its allies back the health they had. Kept on both sides (the client draws the past as echoes), never saved.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public final class TimeTrail {
    /** Ticks between two samples. */
    public static final int EVERY = 4;
    /** How far back the past is kept, in ticks. */
    public static final int SPAN = 240;

    public record Sample(long tick, Vec3 pos, float yRot, float xRot, float health) {}

    private static final Map<Entity, ArrayDeque<Sample>> SERVER = new WeakHashMap<>();
    private static final Map<Entity, ArrayDeque<Sample>> CLIENT = new WeakHashMap<>();

    private TimeTrail() {}

    private static Map<Entity, ArrayDeque<Sample>> side(Entity entity) {
        return entity.level().isClientSide ? CLIENT : SERVER;
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living) || living.tickCount % EVERY != 0 || !living.isAlive()) {
            return;
        }
        long now = living.level().getGameTime();
        ArrayDeque<Sample> trail = side(living).computeIfAbsent(living, e -> new ArrayDeque<>());
        trail.addLast(new Sample(now, living.position(), living.getYRot(), living.getXRot(), living.getHealth()));
        while (!trail.isEmpty() && trail.peekFirst().tick() < now - SPAN) {
            trail.pollFirst();
        }
    }

    /**
     * Where and how {@code entity} was {@code ticksAgo} ticks ago (the closest sample at or before then, else the
     * oldest one kept); null if nothing was recorded yet.
     */
    @Nullable
    public static Sample ago(Entity entity, int ticksAgo) {
        ArrayDeque<Sample> trail = side(entity).get(entity);
        if (trail == null || trail.isEmpty()) {
            return null;
        }
        long when = entity.level().getGameTime() - ticksAgo;
        Sample best = trail.peekFirst();
        for (Sample s : trail) {
            if (s.tick() <= when) {
                best = s;
            } else {
                break;
            }
        }
        return best;
    }

    /** Where {@code entity} was {@code ticksAgo} ticks ago, or where it is if nothing was recorded. */
    public static Vec3 positionAgo(Entity entity, int ticksAgo) {
        Sample s = ago(entity, ticksAgo);
        return s != null ? s.pos() : entity.position();
    }

    /** The highest health {@code entity} had over the last {@code ticks} ticks. */
    public static float bestHealth(LivingEntity entity, int ticks) {
        float best = entity.getHealth();
        ArrayDeque<Sample> trail = side(entity).get(entity);
        if (trail != null) {
            long from = entity.level().getGameTime() - ticks;
            for (Sample s : trail) {
                if (s.tick() >= from) {
                    best = Math.max(best, s.health());
                }
            }
        }
        return best;
    }

    /** The samples of the last {@code ticks} ticks, oldest first. */
    public static List<Sample> since(Entity entity, int ticks) {
        List<Sample> out = new ArrayList<>();
        ArrayDeque<Sample> trail = side(entity).get(entity);
        if (trail != null) {
            long from = entity.level().getGameTime() - ticks;
            for (Sample s : trail) {
                if (s.tick() >= from) {
                    out.add(s);
                }
            }
        }
        return out;
    }
}
