package com.waylays.arcanes.spells.explosion;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.effect.BlastLeapEffect;
import com.waylays.arcanes.registry.ModSchools;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Blast Leap: an explosion under the caster's feet throws them into the sky (it never hurts them); they dive back
 * down and the landing sets off a second, bigger blast (see {@link BlastLeapEffect}).
 */
@AutoSpellConfig
public class BlastLeapSpell extends AbstractSpell {
    public static final float LAUNCH_RADIUS = 2.5f;
    public static final float LANDING_RADIUS = 3.5f;

    private final ResourceLocation spellId = ArcanesOublies.id("blast_leap");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(ModSchools.EXPLOSION_RESOURCE)
            .setMaxLevel(8)
            .setCooldownSeconds(10)
            .build();

    public BlastLeapSpell() {
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 30;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.landing_damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(LANDING_RADIUS, 1))
        );
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.FIREWORK_ROCKET_LAUNCH);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 look = entity.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z);
        forward = forward.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0, entity.getYRot()) : forward.normalize();
        float damage = getDamage(spellLevel, entity);
        // The launch blast: hurts what stands around the caster, never the caster
        ExplosionFx.detonate(level, entity.position().add(0, .3, 0), 1.1f, LAUNCH_RADIUS, damage * .5f, this, entity, entity);
        double reach = .55 + spellLevel * .04;
        entity.setDeltaMovement(forward.x * reach, 1.25, forward.z * reach);
        entity.hurtMarked = true;
        entity.hasImpulse = true;
        entity.fallDistance = 0;
        BlastLeapEffect.leap(entity, forward, damage);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    public float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster);
    }
}
