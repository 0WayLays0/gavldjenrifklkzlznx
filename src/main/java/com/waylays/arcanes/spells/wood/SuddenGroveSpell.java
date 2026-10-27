package com.waylays.arcanes.spells.wood;

import com.waylays.arcanes.entity.wood.SuddenGrove;
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

/** Sudden Grove: real trees spring up round the caster; friends inside are mended, enemies tangled (see SuddenGrove). */
@AutoSpellConfig
public class SuddenGroveSpell extends WoodSpell {
    public SuddenGroveSpell() {
        super("sudden_grove", SpellRarity.EPIC, 5, 60, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 15;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 90;
    }

    public static int trees(int spellLevel) {
        return 5 + spellLevel;
    }

    public static int life(int spellLevel) {
        return 300 + 40 * spellLevel;
    }

    public static float mend(int spellLevel) {
        return 1 + .5f * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.trees", trees(spellLevel)),
                Component.translatable("ui.arcanes_oublies.sprout_damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.heal_per_pulse", Utils.stringTruncation(mend(spellLevel), 1), Utils.timeFromTicks(20, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(life(spellLevel), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        SuddenGrove.plant(level, entity, this, trees(spellLevel), life(spellLevel), getPower(spellLevel, entity), mend(spellLevel));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
