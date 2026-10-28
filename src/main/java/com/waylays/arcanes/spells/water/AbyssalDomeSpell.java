package com.waylays.arcanes.spells.water;

import com.waylays.arcanes.entity.water.AbyssalDome;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import java.util.List;
import java.util.Optional;

/**
 * The Abyssal Dome (Water Magic's ultimate): after a long chant the sea rises round the caster in a great dome of deep
 * water: their side breathe and swim in it, their enemies drown in it, and at its end it crashes down (see AbyssalDome).
 */
@AutoSpellConfig
public class AbyssalDomeSpell extends WaterSpell {
    public AbyssalDomeSpell() {
        super("abyssal_dome", SpellRarity.LEGENDARY, 3, 150, CastType.LONG, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 50;
        this.baseSpellPower = 12;
        this.spellPowerPerLevel = 3;
        this.castTime = 30;
        this.baseManaCost = 250;
    }

    public static float radius(int spellLevel) {
        return 11 + spellLevel;
    }

    public static int life(int spellLevel) {
        return 240 + 60 * spellLevel;
    }

    public float drown(int spellLevel, LivingEntity caster) {
        return getPower(spellLevel, caster) * .3f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float drown = drown(spellLevel, caster);
        return List.of(
                Component.translatable("ui.arcanes_oublies.drown_damage", Utils.stringTruncation(drown, 1), Utils.timeFromTicks(AbyssalDome.DROWN_TICKS, 1)),
                Component.translatable("ui.arcanes_oublies.crash_damage", Utils.stringTruncation(drown * 2.5f, 1)),
                Component.translatable("ui.arcanes_oublies.dome_allies"),
                Component.translatable("ui.arcanes_oublies.slows_shots"),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(radius(spellLevel), 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(life(spellLevel), 1)));
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.CONDUIT_AMBIENT_SHORT);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.CONDUIT_ACTIVATE);
    }

    /** While the caster chants, water wells up round their feet and bubbles rise about them. */
    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @org.jetbrains.annotations.Nullable MagicData playerMagicData) {
        if (entity.tickCount % 2 == 0) {
            float a = entity.tickCount * .35f;
            WaterFx.spray(level, entity.position().add(Mth.cos(a) * 2.2, .2, Mth.sin(a) * 2.2), 2, .2);
            WaterFx.bubbles(level, entity.position().add(0, 1, 0), 3, .8);
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        AbyssalDome.raise(level, entity, this, entity.position(), radius(spellLevel), life(spellLevel), drown(spellLevel, entity));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
