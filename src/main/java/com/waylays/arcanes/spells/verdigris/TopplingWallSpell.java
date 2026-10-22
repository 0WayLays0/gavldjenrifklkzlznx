package com.waylays.arcanes.spells.verdigris;

import com.waylays.arcanes.entity.verdigris.ToppleWall;
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

/** Toppling Wall: an ancient mossy wall bursts out of the ground before the caster, sways, and falls flat on the enemies in front. */
@AutoSpellConfig
public class TopplingWallSpell extends VerdigrisSpell {
    public TopplingWallSpell() {
        super("toppling_wall", SpellRarity.RARE, 6, 14, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 10;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 50;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.wall_size", Utils.stringTruncation(ToppleWall.WIDTH, 1), Utils.stringTruncation(ToppleWall.HEIGHT, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 dir = Vec3.directionFromRotation(0, entity.getYRot());
        Vec3 at = SpellUtil.groundAt(level, entity.position().add(dir.scale(1.6)), 3, 1);
        ToppleWall.raise(level, entity, this, at, entity.getYRot(), getPower(spellLevel, entity));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
