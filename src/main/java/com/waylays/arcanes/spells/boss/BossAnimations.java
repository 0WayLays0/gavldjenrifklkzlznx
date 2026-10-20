package com.waylays.arcanes.spells.boss;

import com.waylays.arcanes.ArcanesOublies;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;

import java.util.List;

/** The casters' body animations (player_animation/boss_spells.json, written by tools/boss_animations.py). */
public final class BossAnimations {
    /** Both arms raised to the sky, then brought down: what falls from above (meteors, stars, the last judgment). */
    public static final AnimationHolder SKY_CALL = own("boss_sky_call");
    /** The right arm raised high, then cut down: a blade or a cross of light. */
    public static final AnimationHolder BLADE_CALL = own("boss_blade_call");
    /** Both fists overhead, then into the ground: a rift, a nova. */
    public static final AnimationHolder GROUND_SLAM = own("boss_ground_slam");
    /** Both hands thrust forward and held: a beam, an orb. */
    public static final AnimationHolder BEAM = own("boss_beam");
    /** Arms spread wide, then clasped: a prison of runes. */
    public static final AnimationHolder PRISON = own("boss_prison");

    public static final List<AnimationHolder> ALL = List.of(SKY_CALL, BLADE_CALL, GROUND_SLAM, BEAM, PRISON);

    private BossAnimations() {}

    private static AnimationHolder own(String name) {
        return new AnimationHolder(ArcanesOublies.MODID + ":" + name, true, true);
    }
}
