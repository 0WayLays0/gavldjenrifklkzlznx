package com.waylays.arcanes.effect;

import com.waylays.arcanes.registry.ModSchools;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;

/**
 * Bleeding (left by a dagger's cuts): a little physical damage every second, more at higher levels, with drops of
 * blood. Milk stops it like any effect.
 */
public class BleedingEffect extends MobEffect {
    public static final int INTERVAL = 20;

    public BleedingEffect() {
        super(MobEffectCategory.HARMFUL, 0x8A0E12);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % INTERVAL == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return true;
        }
        DamageSource source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ModSchools.PHYSICAL_STRIKE));
        entity.invulnerableTime = 0;
        entity.hurt(source, 1 + amplifier);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
                entity.getX(), entity.getY() + entity.getBbHeight() * .6, entity.getZ(), 6, entity.getBbWidth() * .3, entity.getBbHeight() * .2, entity.getBbWidth() * .3, .05);
        return true;
    }
}
