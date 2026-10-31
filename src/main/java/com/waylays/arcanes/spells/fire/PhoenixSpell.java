package com.waylays.arcanes.spells.fire;

import com.waylays.arcanes.entity.fire.Phoenix;
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

/** Phoenix: a great bird of fire takes wing from the caster's hand, sweeps out and back, burning all it flies through (see Phoenix). */
@AutoSpellConfig
public class PhoenixSpell extends StarfireSpell {
    public PhoenixSpell() {
        super("phoenix", SpellRarity.EPIC, 5, 35, CastType.LONG, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 15;
        this.baseSpellPower = 9;
        this.spellPowerPerLevel = 2;
        this.castTime = 20;
        this.baseManaCost = 100;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                heatLine(caster),
                Component.translatable("ui.arcanes_oublies.phoenix_burn", Utils.stringTruncation(hot(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.phoenix_blaze", Utils.stringTruncation(hot(spellLevel, caster) * .8f, 1)));
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.BLAZE_AMBIENT);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.ENDER_DRAGON_GROWL);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @org.jetbrains.annotations.Nullable MagicData playerMagicData) {
        if (entity.tickCount % 2 == 0) {
            float a = entity.tickCount * .5f;
            FireFx.flames(level, entity.position().add(Mth.cos(a) * 1.2, 1.2 + .3 * Mth.sin(a * 2), Mth.sin(a) * 1.2), 2, .1, heat(entity));
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Phoenix.rise(level, entity, this, hot(spellLevel, entity), heat(entity));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
