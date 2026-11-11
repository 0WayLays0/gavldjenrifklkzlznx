package com.waylays.eraphones.rp;

import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Seen by one only (/voir): a character can choose who sees them; for everyone else they are not in the world at all
 * (taken out of their game, so neither drawn nor heard, nor can they be struck), and what they say reaches only those
 * who see them. Shown again, they are put back in everyone's world.
 */
public final class RpVisibility {
    /** Who is, right now, kept out of whose world: hidden player, the players they were taken from. */
    private static final Map<UUID, Set<UUID>> TAKEN = new HashMap<>();

    private RpVisibility() {}

    public static boolean hidden(ServerPlayer target) {
        return RpData.get(target.server).seenOnlyBy(target.getUUID()) != null;
    }

    /** Whether {@code viewer} sees {@code target}. */
    public static boolean sees(ServerPlayer viewer, ServerPlayer target) {
        if (viewer == target) {
            return true;
        }
        Set<UUID> only = RpData.get(target.server).seenOnlyBy(target.getUUID());
        return only == null || only.contains(viewer.getUUID());
    }

    /** The players who see {@code target}, besides themselves. */
    public static List<ServerPlayer> viewers(ServerPlayer target) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : target.server.getPlayerList().getPlayers()) {
            if (p != target && sees(p, target)) {
                out.add(p);
            }
        }
        return out;
    }

    private static com.waylays.eraphones.mixin.TrackedEntityAccessor tracked(ServerPlayer target) {
        Object t = ((com.waylays.eraphones.mixin.ChunkMapAccessor) target.serverLevel().getChunkSource().chunkMap).eraphones$entityMap().get(target.getId());
        return (com.waylays.eraphones.mixin.TrackedEntityAccessor) t;
    }

    /** Takes {@code target} out of the world of each player who should not see them, and back into the world of those who should. */
    public static void apply(ServerPlayer target) {
        com.waylays.eraphones.mixin.TrackedEntityAccessor t = tracked(target);
        Set<UUID> taken = TAKEN.computeIfAbsent(target.getUUID(), k -> new HashSet<>());
        if (t == null) {
            return;
        }
        for (ServerPlayerConnection c : new ArrayList<>(t.eraphones$seenBy())) {
            ServerPlayer p = c.getPlayer();
            boolean sees = sees(p, target);
            if (!sees && taken.add(p.getUUID())) {
                p.connection.send(new ClientboundRemoveEntitiesPacket(target.getId()));
            } else if (sees && taken.remove(p.getUUID())) {
                // Paired again: the game sends it all anew
                t.eraphones$removePlayer(p);
                t.eraphones$updatePlayer(p);
            }
        }
        if (taken.isEmpty()) {
            TAKEN.remove(target.getUUID());
        }
    }

    /** {@code viewer} just came near {@code target}: if they should not see them, they are taken out of their world. */
    public static void startedTracking(ServerPlayer viewer, ServerPlayer target) {
        if (!sees(viewer, target)) {
            viewer.connection.send(new ClientboundRemoveEntitiesPacket(target.getId()));
            TAKEN.computeIfAbsent(target.getUUID(), k -> new HashSet<>()).add(viewer.getUUID());
        } else {
            Set<UUID> taken = TAKEN.get(target.getUUID());
            if (taken != null) {
                taken.remove(viewer.getUUID());
            }
        }
    }

    static void forget(UUID player) {
        TAKEN.remove(player);
        for (Set<UUID> s : TAKEN.values()) {
            s.remove(player);
        }
    }
}
