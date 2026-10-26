package com.waylays.arcanes.spells.cosmos;

import com.waylays.arcanes.entity.cosmos.TimeStopBubble;
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

/**
 * Time Stop: time stops in a bubble round the caster, for everything but them and their summons. Creatures freeze,
 * players cannot move or strike, arrows and spells hang in the air; the blows dealt to the frozen are kept and all
 * land together when time starts again.
 */
@AutoSpellConfig
public class TimeStopSpell extends CosmosSpell {
    public TimeStopSpell() {
        super("time_stop", SpellRarity.EPIC, 5, 50, CastType.INSTANT, SpellAnimations.SELF_CAST_TWO_HANDS);
        this.manaCostPerLevel = 10;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.castTime = 0;
        this.baseManaCost = 80;
    }

    public static float radius(int spellLevel) {
        return 6 + .5f * spellLevel;
    }

    public static int duration(int spellLevel) {
        return 60 + 12 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(radius(spellLevel), 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(duration(spellLevel), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide) {
            TimeStopBubble.stop(level, entity, radius(spellLevel), duration(spellLevel));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
