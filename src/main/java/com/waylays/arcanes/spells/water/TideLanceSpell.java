package com.waylays.arcanes.spells.water;

import com.waylays.arcanes.entity.water.TideLance;
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

/** Tide Lance: a spear of spiralling water flung from the hand runs through up to three enemies (see TideLance). */
@AutoSpellConfig
public class TideLanceSpell extends WaterSpell {
    public TideLanceSpell() {
        super("tide_lance", SpellRarity.COMMON, 10, 3, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 20;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.pierces", TideLance.PIERCE));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        TideLance.fling(level, entity, this, getPower(spellLevel, entity));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
