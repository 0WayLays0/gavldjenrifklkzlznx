package com.waylays.arcanes.spells.bone;

import com.waylays.arcanes.entity.bone.GiantSpike;
import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.world.phys.Vec3;
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

/** Colossal Spike: a spike of bone as tall as a tree bursts out of the ground where the caster looks (see GiantSpike). */
@AutoSpellConfig
public class ColossalSpikeSpell extends BoneSpell {
    public static final float RANGE = 28;

    public ColossalSpikeSpell() {
        super("colossal_spike", SpellRarity.EPIC, 5, 26, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 12;
        this.baseSpellPower = 16;
        this.spellPowerPerLevel = 3;
        this.castTime = 0;
        this.baseManaCost = 80;
    }

    public static float height(int spellLevel) {
        return 9 + spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.spike_height", Utils.stringTruncation(height(spellLevel), 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(RANGE, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 at = SpellUtil.groundAt(level, aimedSpot(level, entity, RANGE), 4, 1);
        GiantSpike.raise(level, entity, this, at, height(spellLevel), getPower(spellLevel, entity));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
