package com.waylays.arcanes.spells.explosion;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.effect.ReactiveShieldEffect;
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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Reactive Shield: a crackling energy shell surrounds the caster; each time they are hit it takes part of the blow
 * and explodes, throwing attackers back. Three explosions (see {@link ReactiveShieldEffect}).
 */
@AutoSpellConfig
public class ReactiveShieldSpell extends AbstractSpell {
    public static final float RADIUS = 3.5f;
    public static final float FALLBACK_DAMAGE = 8f;

    private final ResourceLocation spellId = ArcanesOublies.id("reactive_shield");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(ModSchools.EXPLOSION_RESOURCE)
            .setMaxLevel(6)
            .setCooldownSeconds(35)
            .build();

    public ReactiveShieldSpell() {
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 60;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.arcanes_oublies.charges", ReactiveShieldEffect.CHARGES),
                Component.translatable("ui.arcanes_oublies.absorb", (int) (ReactiveShieldEffect.absorbRatio(spellLevel - 1) * 100)),
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(getDuration(spellLevel), 1))
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
        return Optional.of(SoundEvents.BEACON_ACTIVATE);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        ReactiveShieldEffect.shield(entity, spellLevel, getDuration(spellLevel), getDamage(spellLevel, entity));
        // The shell closes around the caster
        float r = entity.getBbHeight() * .7f;
        double cy = entity.getY() + entity.getBbHeight() * .5;
        for (int i = 0; i < 36; i++) {
            float a = i * Mth.TWO_PI / 36;
            SpellUtil.particles(level, i % 2 == 0 ? ExplosionFx.ARCANE_DUST : ExplosionFx.FIRE_DUST,
                    entity.getX() + Mth.cos(a) * r, cy + Mth.sin(a * 3) * r * .5, entity.getZ() + Mth.sin(a) * r, 1, 0, 0, 0, 0);
        }
        SpellUtil.particles(level, ExplosionFx.arcaneSparks(), entity.getX(), cy, entity.getZ(), 16, r * .5, r * .5, r * .5, .2);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1f, 1.3f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** 20 s at level 1, +2 s per level. */
    public int getDuration(int spellLevel) {
        return 400 + (spellLevel - 1) * 40;
    }

    public float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster);
    }
}
