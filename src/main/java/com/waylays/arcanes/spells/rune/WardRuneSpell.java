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
 * Ward Rune: a circle of runes carved into the ground at the caster's feet, lit with a curtain of light. The caster's
 * allies standing in it are shielded (Resistance I, II from level 4) for as long as it glows.
 */
@AutoSpellConfig
public class WardRuneSpell extends RuneSpell {
    public WardRuneSpell() {
        super("ward_rune", SpellRarity.UNCOMMON, 5, 25, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.castTime = 0;
        this.baseManaCost = 40;
    }

    public static float radius(int spellLevel) {
        return 3.75f + .25f * spellLevel;
    }

    public static int duration(int spellLevel) {
        return 130 + 30 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(radius(spellLevel), 1)),
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(duration(spellLevel), 1)),
                Component.translatable("ui.arcanes_oublies.ward_resistance", spellLevel >= 4 ? "II" : "I"));
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.BEACON_ACTIVATE);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 at = feet(entity);
        float radius = radius(spellLevel);
        RuneSigil.carve(level, entity, RuneSigil.Kind.WARD, at, entity.getYRot(), radius, 0, duration(spellLevel), spellLevel, 0);
        RuneFx.ring(level, at, radius, 32, RuneFx.GLOW_DUST);
        RuneFx.glyphs(level, at.add(0, .5, 0), 20, radius * .4);
        RuneFx.sound(level, at, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5f, .8f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
