package com.waylays.arcanes.spells.verdigris;

import com.waylays.arcanes.entity.verdigris.CopperSpikes;
import com.waylays.arcanes.spells.SpellUtil;
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
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Copper Spikes: a wedge of jagged copper spikes bursts out of the ground ahead, row after row, impaling what stands there. */
@AutoSpellConfig
public class CopperSpikesSpell extends VerdigrisSpell {
    public CopperSpikesSpell() {
        super("copper_spikes", SpellRarity.COMMON, 10, 6, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 25;
    }

    public static int rows(int spellLevel) {
        return 6 + spellLevel / 2;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(1.6 + rows(spellLevel) * CopperSpikes.SPACING, 1)),
                Component.translatable("ui.arcanes_oublies.bleeding", Utils.timeFromTicks(CopperSpikes.BLEED_TICKS, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 at = SpellUtil.groundAt(level, entity.position(), 2, 1);
        CopperSpikes.burst(level, entity, this, at, entity.getYRot(), rows(spellLevel), getPower(spellLevel, entity));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
