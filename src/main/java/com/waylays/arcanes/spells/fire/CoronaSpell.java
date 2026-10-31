package com.waylays.arcanes.spells.fire;

import com.waylays.arcanes.entity.fire.Corona;
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

/** Corona: a ring of fire races out from the caster over the ground, burning and throwing back all it passes (see Corona). */
@AutoSpellConfig
public class CoronaSpell extends StarfireSpell {
    public CoronaSpell() {
        super("corona", SpellRarity.UNCOMMON, 8, 12, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 40;
    }

    public static float radius(int spellLevel) {
        return 6 + .5f * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                heatLine(caster),
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(hot(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(radius(spellLevel), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Corona.flare(level, entity, this, radius(spellLevel), hot(spellLevel, entity), heat(entity));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
