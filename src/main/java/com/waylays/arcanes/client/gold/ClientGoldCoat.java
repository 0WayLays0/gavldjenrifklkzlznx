package com.waylays.arcanes.client.gold;

import com.waylays.arcanes.effect.GildedEffect;
import com.waylays.arcanes.effect.MidasCurseEffect;
import com.waylays.arcanes.network.GoldCoatPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * What the client knows of the gold on each body (see {@link com.waylays.arcanes.spells.gold.GoldCoat}), and how high
 * the gold reaches now: a tenth of the body per coat of Gilding (half of it with all five), all of it as a Midas
 * Curse runs its course, a whole shining shell for Gilded Armor.
 */
public final class ClientGoldCoat {
    private static final Map<Integer, GoldCoatPacket> STATES = new HashMap<>();

    private ClientGoldCoat() {}

    public static void set(GoldCoatPacket state) {
        if (state.coats() <= 0 && state.midasDuration() <= 0 && !state.armor()) {
            STATES.remove(state.entityId());
        } else {
            STATES.put(state.entityId(), state);
        }
        if (STATES.size() > 512) {
            // Bodies long gone: forget them
            var level = Minecraft.getInstance().level;
            STATES.keySet().removeIf(id -> level == null || level.getEntity(id) == null);
        }
    }

    /** The state of {@code entity}'s gold, or null if it wears none. */
    public static GoldCoatPacket get(LivingEntity entity) {
        return STATES.get(entity.getId());
    }

    /** How high the gold reaches on {@code entity} (0 to 1 of its height), and the Midas statue's progress. */
    public static float height(GoldCoatPacket state, LivingEntity entity, float partialTicks) {
        float coats = state.coats() / (float) GildedEffect.MAX_COATS * .5f;
        float midas = 0;
        if (state.midasDuration() > 0) {
            float age = entity.level().getGameTime() + partialTicks - state.midasStart();
            midas = Mth.clamp(age / state.midasDuration() / MidasCurseEffect.TURNED, 0, 1.05f);
        }
        return Math.max(coats, midas);
    }

    public static void clear() {
        STATES.clear();
    }
}
