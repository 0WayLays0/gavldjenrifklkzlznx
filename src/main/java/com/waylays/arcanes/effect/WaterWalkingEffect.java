package com.waylays.arcanes.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Water Walking (given by the staff with /marchesurleau): water bears its holder up like ground, as lava bears up a
 * strider; crouching, they sink and swim as usual (see {@code LivingEntityWaterWalkMixin}).
 */
public class WaterWalkingEffect extends MobEffect {
    public WaterWalkingEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x5AB4FF);
    }
}
