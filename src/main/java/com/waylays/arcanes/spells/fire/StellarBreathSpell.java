package com.waylays.arcanes.spells.fire;

import com.waylays.arcanes.entity.fire.StellarBreath;
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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * Stellar Breath: held, a cone of fire roars from the hand; it stokes the caster's heat as it starts and again every
 * two seconds it is held, so it burns hotter and hotter (see StellarBreath).
 */
@AutoSpellConfig
public class StellarBreathSpell extends StarfireSpell {
    public static final int STOKE_TICKS = 40;

    public StellarBreathSpell() {
        super("stellar_breath", SpellRarity.UNCOMMON, 8, 10, CastType.CONTINUOUS, null);
        this.manaCostPerLevel = 1;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 100;
        this.baseManaCost = 4;
    }

    /** Iron's calls onCast over and over while it is held: the heat is stoked by the time held instead. */
    @Override
    protected int stokes() {
        return 0;
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    public float damage(int spellLevel, LivingEntity caster) {
        return hot(spellLevel, caster) * .45f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                heatLine(caster),
                Component.translatable("ui.arcanes_oublies.damage_per_second", Utils.stringTruncation(damage(spellLevel, caster) * 20f / StellarBreath.HIT_TICKS, 1)),
                Component.translatable("ui.arcanes_oublies.stokes_while_held", Utils.timeFromTicks(STOKE_TICKS, 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(StellarBreath.RANGE, 1)));
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @org.jetbrains.annotations.Nullable MagicData playerMagicData) {
        StellarBreath breath = StellarBreath.hold(level, entity, this, damage(spellLevel, entity), heat(entity));
        if (breath.tickCount % STOKE_TICKS == 1) {
            Heat.stoke(entity, 1);
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        StellarBreath.hold(level, entity, this, damage(spellLevel, entity), heat(entity));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
