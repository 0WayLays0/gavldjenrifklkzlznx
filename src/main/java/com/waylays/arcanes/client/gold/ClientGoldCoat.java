package com.waylays.arcanes.client.gold;

import com.waylays.arcanes.effect.MidasCurseEffect;
import com.waylays.arcanes.network.GoldCoatPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * What the client knows of the gold on each body (see {@link com.waylays.arcanes.spells.gold.GoldCoat}), and how high
 * the gold reaches now: all of it as a Midas Curse runs its course, a whole shining shell for Gilded Armor.
 */
public final class ClientGoldCoat {
    private static final Map<Integer, GoldCoatPacket> STATES = new HashMap<>();
    /** When each body's Gilded Armor began to flow up it (game time). */
    private static final Map<Integer, Long> ARMOR_SINCE = new HashMap<>();
    /** Ticks the liquid gold of Gilded Armor takes to flow from the feet to the head. */
    private static final float ARMOR_FLOW = 12;

    private ClientGoldCoat() {}

    public static void set(GoldCoatPacket state) {
        GoldCoatPacket before = STATES.get(state.entityId());
        var world = Minecraft.getInstance().level;
        if (state.armor() && (before == null || !before.armor()) && world != null) {
            ARMOR_SINCE.put(state.entityId(), world.getGameTime());
        } else if (!state.armor()) {
            ARMOR_SINCE.remove(state.entityId());
        }
        if (state.midasDuration() <= 0 && !state.armor()) {
            STATES.remove(state.entityId());
        } else {
            STATES.put(state.entityId(), state);
        }
        if (STATES.size() > 512) {
            // Bodies long gone: forget them
            var level = Minecraft.getInstance().level;
            STATES.keySet().removeIf(id -> level == null || level.getEntity(id) == null);
            ARMOR_SINCE.keySet().retainAll(STATES.keySet());
        }
    }

    /** The state of {@code entity}'s gold, or null if it wears none. */
    public static GoldCoatPacket get(LivingEntity entity) {
        return STATES.get(entity.getId());
    }

    /** How high the gold reaches on {@code entity} (0 to 1 of its height), and the Midas statue's progress. */
    public static float height(GoldCoatPacket state, LivingEntity entity, float partialTicks) {
        float midas = 0;
        if (state.midasDuration() > 0) {
            float age = entity.level().getGameTime() + partialTicks - state.midasStart();
            midas = Mth.clamp(age / state.midasDuration() / MidasCurseEffect.TURNED, 0, 1.05f);
        }
        return midas;
    }

    /** How far up {@code entity} its Gilded Armor has flowed (0 to 1). */
    public static float armor(LivingEntity entity, float partialTicks) {
        Long since = ARMOR_SINCE.get(entity.getId());
        return since == null ? 1 : Mth.clamp((entity.level().getGameTime() + partialTicks - since) / ARMOR_FLOW, 0, 1);
    }

    public static void clear() {
        ARMOR_SINCE.clear();
        STATES.clear();
    }
}
