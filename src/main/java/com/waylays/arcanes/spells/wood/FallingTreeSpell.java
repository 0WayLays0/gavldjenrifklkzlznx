package com.waylays.arcanes.spells.wood;

import com.waylays.arcanes.entity.wood.FallingTree;
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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/** Timber!: a great oak bursts up beside the caster, sways, and topples onto what is in front of them (see FallingTree). */
@AutoSpellConfig
public class FallingTreeSpell extends WoodSpell {
    public FallingTreeSpell() {
        super("falling_tree", SpellRarity.RARE, 6, 14, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 12;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 55;
    }

    public static float height(int spellLevel) {
        return 9 + .6f * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.crush_damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(height(spellLevel) + 2.5f, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        float yaw = entity.getYRot();
        float r = yaw * Mth.DEG_TO_RAD;
        Vec3 foot = SpellUtil.groundAt(level, entity.position().add(-Mth.sin(r) * 1.8, 0, Mth.cos(r) * 1.8), 3, 2);
        FallingTree.raise(level, entity, this, foot, yaw, height(spellLevel), getPower(spellLevel, entity));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
