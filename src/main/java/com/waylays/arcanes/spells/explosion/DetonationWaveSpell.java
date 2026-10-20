package com.waylays.arcanes.spells.explosion;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.explosion.BlastEffect;
import com.waylays.arcanes.entity.explosion.DetonationWave;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Detonation Wave: the caster strikes the ground and a ring of explosions rolls outward around them, ring after ring
 * (see {@link DetonationWave}).
 */
@AutoSpellConfig
public class DetonationWaveSpell extends AbstractSpell {
    public static final double RING_SPACING = 1.8;

    private final ResourceLocation spellId = ArcanesOublies.id("detonation_wave");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(ModSchools.EXPLOSION_RESOURCE)
            .setMaxLevel(8)
            .setCooldownSeconds(18)
            .build();

    public DetonationWaveSpell() {
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 7;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 60;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(getRadius(spellLevel), 1))
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
        return Optional.of(SoundEvents.GENERIC_EXPLODE.value());
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        DetonationWave wave = new DetonationWave(level, entity, getDamage(spellLevel, entity), getRings(spellLevel));
        wave.moveTo(entity.getX(), entity.getY(), entity.getZ());
        level.addFreshEntity(wave);
        // The strike on the ground
        ExplosionFx.lightBlast(level, entity.position().add(0, .2, 0), 1.1f, BlastEffect.STYLE_FIRE);
        SpellUtil.particles(level, new BlastwaveParticleOptions(ExplosionFx.FIRE, 3f), entity.getX(), entity.getY() + .15, entity.getZ(), 1, 0, 0, 0, 0);
        SpellUtil.particles(level, new BlastwaveParticleOptions(ExplosionFx.ARCANE, 4.5f), entity.getX(), entity.getY() + .15, entity.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, .8f, .5f);
        SpellUtil.cameraShake(level, entity.position(), 8, 10);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** 3 rings at level 1, one more every 2 levels. */
    public int getRings(int spellLevel) {
        return 3 + spellLevel / 2;
    }

    public double getRadius(int spellLevel) {
        return DetonationWave.ringRadius(getRings(spellLevel) - 1) + DetonationWave.BAND;
    }

    public float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster);
    }
}
