package com.waylays.arcanes.spells.wood;

import com.waylays.arcanes.entity.wood.FlyingLog;
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

/** Log Hurl: a great log torn up out of the ground is hurled and rolls on, bowling over every enemy in its way (see FlyingLog). */
@AutoSpellConfig
public class LogHurlSpell extends WoodSpell {
    public LogHurlSpell() {
        super("log_hurl", SpellRarity.COMMON, 10, 6, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 7;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 25;
    }

    /** How far the log rolls once it is down. */
    public static float roll(int spellLevel) {
        return 10 + spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.rolls_for", Utils.stringTruncation(roll(spellLevel), 1)),
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.stringTruncation(getPower(spellLevel, caster) * .5f, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        FlyingLog.hurl(level, entity, this, getPower(spellLevel, entity), roll(spellLevel));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
