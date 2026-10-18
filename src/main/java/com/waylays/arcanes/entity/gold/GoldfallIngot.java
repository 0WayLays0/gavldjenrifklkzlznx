package com.waylays.arcanes.entity.gold;

import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.gold.GoldFx;
import com.waylays.arcanes.spells.gold.GoldfallSpell;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Goldfall: a massive ingot dropped from the sky. Crashes into the ground and hurts every enemy around the impact.
 */
public class GoldfallIngot extends GoldProjectile {
    public GoldfallIngot(EntityType<? extends GoldfallIngot> entityType, Level level) {
        super(entityType, level);
    }

    public GoldfallIngot(Level level, LivingEntity caster) {
        this(ModEntities.GOLDFALL_INGOT.get(), level);
        setOwner(caster);
    }

    @Override
    protected double gravity() {
        return .06;
    }

    @Override
    protected int lifetime() {
        return 10 * 20;
    }

    @Override
    protected void onImpact(HitResult hit) {
        Vec3 at = hit.getLocation();
        GoldFx.areaDamage(level(), at, GoldfallSpell.IMPACT_RADIUS, getDamage(), ModSpells.GOLDFALL.get(), this, getOwner());
        GoldFx.shatterBurst(level(), at.x, at.y + .3, at.z, 1.4f);
        GoldFx.particles(level(), GoldFx.DEEP_DUST, at.x, at.y + .2, at.z, 25, 1.2, .1, 1.2, .02);
        GoldFx.cameraShake(level(), at, 8, 10);
        level().playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 1.4f, .7f + random.nextFloat() * .2f);
        level().playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.NEUTRAL, 2f, .8f);
        level().playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL, .5f, 1.6f);
        discard();
    }

    /** Client side: a comet tail of molten gold. */
    @Override
    protected void clientTick() {
        Level level = level();
        double cy = getY() + getBbHeight() * .5;
        for (int i = 0; i < 2; i++) {
            level.addParticle(GoldFx.DEEP_DUST, getX() + (random.nextFloat() - .5f) * .6f, cy + random.nextFloat() * .8f, getZ() + (random.nextFloat() - .5f) * .6f, 0, .02, 0);
        }
        level.addParticle(GoldFx.GOLD_DUST, getX(), cy, getZ(), 0, 0, 0);
        if (random.nextInt(2) == 0) {
            level.addParticle(GoldFx.sparks(), getX(), cy, getZ(), (random.nextFloat() - .5f) * .2f, .1f, (random.nextFloat() - .5f) * .2f);
        }
    }
}
