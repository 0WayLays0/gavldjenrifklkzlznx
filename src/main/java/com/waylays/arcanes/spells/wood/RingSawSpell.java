package com.waylays.arcanes.spells.wood;

import com.waylays.arcanes.entity.wood.RingSaw;
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

/** Ring Saw: a slice of trunk with thorns for teeth, hurled spinning, cuts on the way out and back (see RingSaw). */
@AutoSpellConfig
public class RingSawSpell extends WoodSpell {
    public RingSawSpell() {
        super("ring_saw", SpellRarity.RARE, 6, 9, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 7;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 45;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.cut_each_pass", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(RingSaw.OUT * RingSaw.SPEED, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        RingSaw.hurl(level, entity, this, getPower(spellLevel, entity));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
