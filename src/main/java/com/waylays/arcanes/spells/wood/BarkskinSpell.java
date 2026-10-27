package com.waylays.arcanes.spells.wood;

import com.waylays.arcanes.entity.wood.BarkShell;
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

/** Barkskin: living bark grows over the caster: far less damage, hardly pushed back, thorns for strikers (see BarkShell). */
@AutoSpellConfig
public class BarkskinSpell extends WoodSpell {
    public BarkskinSpell() {
        super("barkskin", SpellRarity.RARE, 5, 40, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 50;
    }

    public static int duration(int spellLevel) {
        return 300 + 60 * spellLevel;
    }

    public static float mend(int spellLevel) {
        return 1 + .5f * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage_reduction", Math.round((1 - BarkShell.GUARD) * 100)),
                Component.translatable("ui.arcanes_oublies.thorn_damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.heal_per_pulse", Utils.stringTruncation(mend(spellLevel), 1), Utils.timeFromTicks(40, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(duration(spellLevel), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        BarkShell.grow(level, entity, this, duration(spellLevel), getPower(spellLevel, entity), mend(spellLevel));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
