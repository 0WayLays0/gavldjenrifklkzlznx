package com.waylays.arcanes.spells.water;

import com.waylays.arcanes.entity.water.RainCloud;
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

/** Cleansing Rain: a rain cloud follows the caster, mending and washing clean their allies, soaking their enemies (see RainCloud). */
@AutoSpellConfig
public class CleansingRainSpell extends WaterSpell {
    public CleansingRainSpell() {
        super("cleansing_rain", SpellRarity.RARE, 6, 30, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 2;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 55;
    }

    public static int life(int spellLevel) {
        return 200 + 30 * spellLevel;
    }

    public float mend(int spellLevel, LivingEntity caster) {
        return getPower(spellLevel, caster) * .6f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.heal_per_pulse", Utils.stringTruncation(mend(spellLevel, caster), 1), Utils.timeFromTicks(RainCloud.PULSE, 1)),
                Component.translatable("ui.arcanes_oublies.rain_cleanses", Utils.timeFromTicks(RainCloud.PULSE, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RainCloud.RADIUS, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(life(spellLevel), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        RainCloud.gather(level, entity, life(spellLevel), mend(spellLevel, entity));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
