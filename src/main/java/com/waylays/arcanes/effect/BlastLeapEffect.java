package com.waylays.arcanes.effect;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.explosion.BlastLeapSpell;
import com.waylays.arcanes.spells.explosion.ExplosionFx;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Blast Leap in flight: the caster trails fire, dives at {@link #DIVE_TICK} and blows up where it lands. No fall
 * damage during the leap. Timed on the server clock, since the server does not know a player's real velocity.
 */
@Mod.EventBusSubscriber(modid = ArcanesOublies.MODID)
public class BlastLeapEffect extends MobEffect {
    public static final int DIVE_TICK = 12;
    public static final int MAX_TICKS = 100;
    private static final String TAG = ArcanesOublies.MODID + ":blast_leap";

    public BlastLeapEffect() {
        super(MobEffectCategory.BENEFICIAL, ModSchools.EXPLOSION_COLOR);
    }

    /** Starts the leap: {@code direction} is the horizontal direction of the jump, used again for the dive. */
    public static void leap(LivingEntity entity, Vec3 direction, float landingDamage) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Start", entity.level().getGameTime());
        tag.putFloat("Damage", landingDamage);
        tag.putDouble("DirX", direction.x);
        tag.putDouble("DirZ", direction.z);
        entity.getPersistentData().put(TAG, tag);
        entity.addEffect(new MobEffectInstance(ModEffects.BLAST_LEAP.get(), MAX_TICKS, 0, false, false, true));
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || !entity.hasEffect(ModEffects.BLAST_LEAP.get())) {
            return;
        }
        CompoundTag tag = entity.getPersistentData().getCompound(TAG);
        if (!tag.contains("Start")) {
            entity.removeEffect(ModEffects.BLAST_LEAP.get());
            return;
        }
        long age = level.getGameTime() - tag.getLong("Start");
        // Comet trail
        SpellUtil.particles(level, ParticleTypes.FLAME, entity.getX(), entity.getY() + .2, entity.getZ(), 3, .15, .1, .15, .02);
        SpellUtil.particles(level, ExplosionFx.FIRE_DUST, entity.getX(), entity.getY() + .4, entity.getZ(), 2, .2, .2, .2, 0);
        if (age % 2 == 0) {
            SpellUtil.particles(level, ParticleTypes.LARGE_SMOKE, entity.getX(), entity.getY() + .2, entity.getZ(), 1, .1, .1, .1, .01);
        }
        if (age == DIVE_TICK && !entity.onGround()) {
            // Nose-dive toward the ground
            entity.setDeltaMovement(tag.getDouble("DirX") * .45, -1.5, tag.getDouble("DirZ") * .45);
            entity.hurtMarked = true;
            SpellUtil.particles(level, ExplosionFx.sparks(), entity.getX(), entity.getY() + 1, entity.getZ(), 12, .3, .3, .3, .3);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.5f, .6f);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.PHANTOM_SWOOP, SoundSource.PLAYERS, 1f, 1.4f);
        }
        if (age > 4 && (entity.onGround() || entity.isInWater())) {
            land(level, entity, tag);
        }
    }

    private static void land(ServerLevel level, LivingEntity entity, CompoundTag tag) {
        entity.getPersistentData().remove(TAG);
        entity.removeEffect(ModEffects.BLAST_LEAP.get());
        entity.fallDistance = 0;
        if (entity.isInWater()) {
            return;
        }
        Vec3 at = entity.position().add(0, .3, 0);
        ExplosionFx.detonate(level, at, 1.8f, BlastLeapSpell.LANDING_RADIUS, tag.getFloat("Damage"), ModSpells.BLAST_LEAP.get(), entity, entity);
        SpellUtil.particles(level, new BlastwaveParticleOptions(ExplosionFx.FIRE, 5f), at.x, entity.getY() + .1, at.z, 1, 0, 0, 0, 0);
    }

    /** The leap never hurts its caster: no fall damage while the effect is on. */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity().hasEffect(ModEffects.BLAST_LEAP.get())) {
            event.setCanceled(true);
        }
    }
}
