package com.waylays.arcanes.spells.water;

import com.waylays.arcanes.entity.water.Maelstrom;
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

/** Maelstrom: the ground where the caster points becomes a whirlpool drawing enemies in to its heart (see Maelstrom). */
@AutoSpellConfig
public class MaelstromSpell extends WaterSpell {
    public static final double RANGE = 18;

    public MaelstromSpell() {
        super("maelstrom", SpellRarity.RARE, 6, 22, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 60;
    }

    public static int life(int spellLevel) {
        return 120 + 15 * spellLevel;
    }

    public float churn(int spellLevel, LivingEntity caster) {
        return getPower(spellLevel, caster) * .45f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.cut_damage", Utils.stringTruncation(churn(spellLevel, caster), 1), Utils.timeFromTicks(10, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(Maelstrom.RADIUS, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(life(spellLevel), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Maelstrom.open(level, entity, this, aimedSpot(level, entity, RANGE), life(spellLevel), churn(spellLevel, entity));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
