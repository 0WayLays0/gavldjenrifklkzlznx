package com.waylays.arcanes.entity;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * A mob summoned by a spell: it fights for its summoner, and the addon's spells never hurt it when its summoner casts
 * them (see {@link com.waylays.arcanes.spells.SpellUtil#isAlly}), and casting its spell again sends it away (see
 * {@link com.waylays.arcanes.spells.SummoningSpell}).
 * <p>
 * The method names must never be those of a Minecraft method (such as a horse's {@code getOwnerUUID}): Minecraft's
 * own methods are renamed in the released jar, so a summon inheriting one would stop implementing this interface
 * there and crash the game (while working fine in the development build).
 */
public interface OwnedSummon {
    @Nullable
    UUID getSummonerUUID();

    /** Server side: the summoner if it is still around. */
    @Nullable
    LivingEntity getSummoner();

    /** The spell that summoned it: its summoner casting that spell again sends it away. */
    AbstractSpell getSummoningSpell();

    /** Leaves the world in its own way (no drops). */
    void dismissSummon();
}
