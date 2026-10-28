package com.waylays.arcanes.spells.water;

import com.waylays.arcanes.entity.water.WaterJet;
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
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/** Pressure Jet: hold to shoot a jet of water thin as a blade that cuts and drives back what it touches (see WaterJet). */
@AutoSpellConfig
public class PressureJetSpell extends WaterSpell {
    public PressureJetSpell() {
        super("pressure_jet", SpellRarity.UNCOMMON, 8, 12, CastType.CONTINUOUS, null);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 80;
        this.baseManaCost = 12;
    }

    public float damage(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .35f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.damage_per_second", Utils.stringTruncation(damage(spellLevel, caster) * 20f / WaterJet.HIT_TICKS, 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(WaterJet.RANGE, 1)));
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        WaterJet.hold(level, entity, this, damage(spellLevel, entity));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        WaterJet.hold(level, entity, this, damage(spellLevel, entity));
    }
}
