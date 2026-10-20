package com.waylays.arcanes.spells.boss;

import com.waylays.arcanes.entity.boss.BossTelegraph;
import net.minecraft.server.level.ServerLevel;

/** A Boss Magic spell: what happens when one of its warnings ({@link BossTelegraph}) runs out. */
public interface BossAttack {
    /** The warning ran out: the attack lands now, on whoever is still in the zone. */
    void onTelegraphFire(BossTelegraph telegraph, ServerLevel level);
}
