package com.waylays.arcanes.spells.gold;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.effect.GildedArmorEffect;
import com.waylays.arcanes.registry.ModEffects;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * Gilded Armor: liquid gold flows up the caster's body and hardens into armor that sends part of every hit back
 * (see {@link GildedArmorEffect}).
 */
@AutoSpellConfig
public class GildedArmorSpell extends AbstractSpell {
    private final ResourceLocation spellId = ArcanesOublies.id("gilded_armor");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(ModSchools.GOLD_RESOURCE)
            .setMaxLevel(6)
            .setCooldownSeconds(45)
            .build();

    public GildedArmorSpell() {
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 50;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        int amplifier = getAmplifier(spellLevel);
        return List.of(
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(getDuration(spellLevel), 1)),
                Component.translatable("ui.arcanes_oublies.armor", 3 * (amplifier + 1)),
                Component.translatable("ui.arcanes_oublies.reflect", (int) (GildedArmorEffect.reflectRatio(amplifier) * 100))
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
        return Optional.of(SoundEvents.ARMOR_EQUIP_GOLD);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        entity.addEffect(new MobEffectInstance(ModEffects.GILDED_ARMOR, getDuration(spellLevel), getAmplifier(spellLevel), false, true, true));
        // Liquid gold spiralling up from the feet to the head
        float w = entity.getBbWidth() * .7f;
        float h = entity.getBbHeight();
        for (int i = 0; i < 40; i++) {
            float f = i / 40f;
            float angle = f * Mth.TWO_PI * 3;
            SpellUtil.particles(level, i % 3 == 0 ? GoldFx.PALE_DUST : GoldFx.DEEP_DUST,
                    entity.getX() + Mth.cos(angle) * w, entity.getY() + f * h, entity.getZ() + Mth.sin(angle) * w, 1, 0, .02, 0, .01);
        }
        SpellUtil.particles(level, GoldFx.sparks(), entity.getX(), entity.getY() + h * .5, entity.getZ(), 12, w * .5, h * .3, w * .5, .25);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5f, .8f);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, .4f, 1.8f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** 10 s at level 1, +2 s per level. */
    public int getDuration(int spellLevel) {
        return 200 + (spellLevel - 1) * 40;
    }

    public int getAmplifier(int spellLevel) {
        return (spellLevel - 1) / 2;
    }
}
