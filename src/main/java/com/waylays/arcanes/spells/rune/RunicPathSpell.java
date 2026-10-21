package com.waylays.arcanes.spells.rune;

import com.waylays.arcanes.entity.rune.RuneSigil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Runic Path: a strip of rune tiles carved ahead of the caster, lighting up one after another. Allies walking on it
 * run faster (Speed II, III at level 5) and jump higher.
 */
@AutoSpellConfig
public class RunicPathSpell extends RuneSpell {
    public static final float WIDTH = 2.4f;

    public RunicPathSpell() {
        super("runic_path", SpellRarity.COMMON, 5, 20, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.castTime = 0;
        this.baseManaCost = 25;
    }

    public static float length(int spellLevel) {
        return 9 + 1.5f * spellLevel;
    }

    public static int duration(int spellLevel) {
        return 140 + 20 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.length", Utils.stringTruncation(length(spellLevel), 1)),
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(duration(spellLevel), 1)),
                Component.translatable("ui.arcanes_oublies.path_speed", spellLevel >= 5 ? "III" : "II"));
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.AMETHYST_BLOCK_CHIME);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        float yaw = entity.getYRot();
        Vec3 start = feet(entity).add(direction(yaw).scale(.5));
        RuneSigil.carve(level, entity, RuneSigil.Kind.PATH, start, yaw, length(spellLevel), WIDTH, duration(spellLevel), spellLevel, 0);
        RuneFx.glyphs(level, start.add(0, .3, 0), 12, .6);
        RuneFx.sound(level, start, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f, 1.6f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
