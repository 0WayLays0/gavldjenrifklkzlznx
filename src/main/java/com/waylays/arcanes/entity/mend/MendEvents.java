package com.waylays.arcanes.entity.mend;

import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Mending Magic's hooks into what happens to its allies (server side): the health each one had before its last wounds
 * (for Rewind), the wisps that answer a wound, the cocoons no blow passes, the second lives that undo a death.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public final class MendEvents {
    /** How far back Rewind can go. */
    public static final int REWIND_TICKS = 100;

    private static final Map<LivingEntity, ArrayDeque<float[]>> HISTORY = new WeakHashMap<>();
    private static final Map<Integer, MendScene> WISPS = new HashMap<>();
    private static final Map<Integer, MendScene> COCOONS = new HashMap<>();
    private static final Map<Integer, MendScene> SECONDS = new HashMap<>();

    private MendEvents() {}

    static void track(MendScene scene, LivingEntity ally, MendScene.Kind kind) {
        if (scene.level().isClientSide) {
            return;
        }
        Map<Integer, MendScene> map = map(kind);
        if (map != null) {
            MendScene old = map.put(ally.getId(), scene);
            if (old != null && old != scene && !old.isRemoved()) {
                old.finish(0);
            }
        }
    }

    static void untrack(MendScene scene) {
        Map<Integer, MendScene> map = map(scene.getKind());
        if (map != null) {
            map.values().removeIf(s -> s == scene);
        }
    }

    private static Map<Integer, MendScene> map(MendScene.Kind kind) {
        return switch (kind) {
            case WISP -> WISPS;
            case COCOON -> COCOONS;
            case SECOND -> SECONDS;
            default -> null;
        };
    }

    private static boolean live(MendScene s) {
        return s != null && !s.isRemoved() && !s.isDone();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncoming(LivingIncomingDamageEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide) {
            return;
        }
        if (live(COCOONS.get(e.getId()))) {
            event.setCanceled(true);
            return;
        }
        // Its health before this blow, for Rewind
        ArrayDeque<float[]> h = HISTORY.computeIfAbsent(e, k -> new ArrayDeque<>());
        long now = e.level().getGameTime();
        h.addLast(new float[] {now, e.getHealth()});
        while (!h.isEmpty() && (h.peekFirst()[0] < now - REWIND_TICKS || h.size() > 64)) {
            h.removeFirst();
        }
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        LivingEntity e = event.getEntity();
        MendScene wisp = WISPS.get(e.getId());
        if (!e.level().isClientSide && live(wisp) && event.getNewDamage() > 0) {
            wisp.wounded(event.getNewDamage());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity e = event.getEntity();
        MendScene second = SECONDS.get(e.getId());
        if (!e.level().isClientSide && live(second)) {
            event.setCanceled(true);
            second.spare(e);
        }
    }

    /** The most health {@code e} had in the last {@link #REWIND_TICKS} ticks before a blow, or -1 if it took none. */
    public static float healthBefore(LivingEntity e) {
        ArrayDeque<float[]> h = HISTORY.get(e);
        if (h == null) {
            return -1;
        }
        long now = e.level().getGameTime();
        float best = -1;
        for (float[] r : h) {
            if (r[0] >= now - REWIND_TICKS) {
                best = Math.max(best, r[1]);
            }
        }
        return best;
    }
}
