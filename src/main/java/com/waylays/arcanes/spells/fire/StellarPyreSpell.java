package com.waylays.arcanes.spells.fire;

import com.waylays.arcanes.entity.fire.StellarPyre;
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
 * The Pyre of Six Fires (Stellar Fire's ultimate): after a long chant a vortex of fire rises round the caster and
 * burns through the six heats, red to violet, then bursts in a violet supernova (see StellarPyre).
 */
@AutoSpellConfig
public class StellarPyreSpell extends StarfireSpell {
    public StellarPyreSpell() {
        super("stellar_pyre", SpellRarity.LEGENDARY, 3, 160, CastType.LONG, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 50;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 2;
        this.castTime = 40;
        this.baseManaCost = 250;
    }

    @Override
    protected int stokes() {
        return 0;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float p = getPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.arcanes_oublies.pyre_burn", Utils.stringTruncation(p, 1), Utils.stringTruncation(p * HeatColors.POWER[HeatColors.TIERS - 1], 1),
                        Utils.timeFromTicks(10, 1)),
                Component.translatable("ui.arcanes_oublies.pyre_nova", Utils.stringTruncation(p * 3, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(StellarPyre.RADIUS, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(StellarPyre.LIFE, 1)));
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.BLAZE_AMBIENT);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.BLAZE_SHOOT);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @org.jetbrains.annotations.Nullable MagicData playerMagicData) {
        if (entity.tickCount % 2 == 0) {
            float a = entity.tickCount * .4f;
            int tier = Math.min(HeatColors.TIERS - 1, entity.tickCount / 7 % HeatColors.TIERS);
            FireFx.flames(level, entity.position().add(Mth.cos(a) * 2.5, .3, Mth.sin(a) * 2.5), 3, .1, tier);
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        StellarPyre.kindle(level, entity, this, getPower(spellLevel, entity));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
