package com.waylays.arcanes.spells.wind;

import com.waylays.arcanes.entity.wind.WindWall;
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

/** Wind Wall: a wall of wind across the caster's way that no shot gets through and that throws enemies back (see WindWall). */
@AutoSpellConfig
public class WindWallSpell extends WindSpell {
    public WindWallSpell() {
        super("wind_wall", SpellRarity.RARE, 6, 20, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 2;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 45;
    }

    public static int seconds(int spellLevel) {
        return 7 + spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.blocks_shots"),
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(seconds(spellLevel) * 20, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide) {
            Vec3 f = Vec3.directionFromRotation(0, entity.getYRot());
            Vec3 at = com.waylays.arcanes.spells.SpellUtil.groundAt(level, entity.position().add(f.scale(3)), 4, 1);
            WindWall.raise(level, entity, this, at, entity.getYRot(), seconds(spellLevel) * 20, getPower(spellLevel, entity));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
