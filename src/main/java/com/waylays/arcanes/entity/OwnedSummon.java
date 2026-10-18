package com.waylays.arcanes.entity;

import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * A mob summoned by a spell: it fights for its owner, and the addon's spells never hurt it when its owner casts them
 * (see {@link com.waylays.arcanes.spells.SpellUtil#isAlly}).
 */
public interface OwnedSummon {
    @Nullable
    UUID getOwnerUUID();

    /** Server side: the summoner if it is still around. */
    @Nullable
    LivingEntity getOwner();
}
