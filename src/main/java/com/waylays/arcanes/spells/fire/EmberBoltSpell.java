package com.waylays.arcanes.spells.fire;

import com.waylays.arcanes.entity.fire.EmberBolt;
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

import java.util.List;

/** Ember Bolt: a ball of Stellar Fire flung from the hand, bursting on what it strikes (see EmberBolt). */
@AutoSpellConfig
public class EmberBoltSpell extends StarfireSpell {
    public EmberBoltSpell(int tier) {
        super("ember_bolt", tier, SpellRarity.COMMON, 10, 2, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 18;
        costsByHeat();
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                heatLine(caster),
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(hot(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.splash_damage", Utils.stringTruncation(hot(spellLevel, caster) * .5f, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        EmberBolt.fling(level, entity, this, hot(spellLevel, entity), heat(entity));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
