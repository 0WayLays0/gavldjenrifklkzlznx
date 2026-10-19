package com.waylays.arcanes.spells.physical;

import com.waylays.arcanes.entity.physical.Missile;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;

/** A fighting move that throws or shoots {@link Missile}s, and decides what they do (server side). */
public interface MissileSkill {
    /** The missile just hit {@code target}: its damage, effects, pull... ({@link Missile#strike} deals the damage). */
    void onMissileHit(Missile missile, LivingEntity target);

    /** The missile just hit a block (before it sticks there). */
    default void onMissileHitBlock(Missile missile, BlockHitResult hit) {}

    /** Each tick of the missile's flight. */
    default void tickMissile(Missile missile) {}
}
