package com.waylays.arcanes.spells.water;

import com.waylays.arcanes.entity.water.Geyser;
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

/** Geyser: the ground where the caster points bubbles, then a column of water bursts out and hurls what stands there (see Geyser). */
@AutoSpellConfig
public class GeyserSpell extends WaterSpell {
    public static final double RANGE = 20;

    public GeyserSpell() {
        super("geyser", SpellRarity.UNCOMMON, 8, 9, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 35;
    }

    public static float height(int spellLevel) {
        return 8 + .5f * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.spike_height", Utils.stringTruncation(height(spellLevel), 1)),
                Component.translatable("ui.irons_spellbooks.cast_range", Utils.stringTruncation(RANGE, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Geyser.burst(level, entity, this, aimedSpot(level, entity, RANGE), height(spellLevel), getPower(spellLevel, entity));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
