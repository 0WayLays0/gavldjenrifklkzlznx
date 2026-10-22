package com.waylays.arcanes.spells.wall;

import com.waylays.arcanes.entity.wall.WallPiece;
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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Rampart Charge: a wall bursts up before the caster and charges ahead, sweeping the enemies away before it. */
@AutoSpellConfig
public class RampartChargeSpell extends WallSpell {
    public RampartChargeSpell() {
        super("rampart_charge", SpellRarity.RARE, 6, 12, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 45;
    }

    public static float distance(int spellLevel) {
        return 8 + spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(distance(spellLevel), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 dir = Vec3.directionFromRotation(0, entity.getYRot());
        Vec3 at = SpellUtil.groundAt(level, entity.position().add(dir.scale(1.5)), 3, 1);
        float travel = distance(spellLevel);
        WallPiece.raise(level, entity, this, WallPiece.Kind.PUSH, at, entity.getYRot(), 4, 2.5f, WallPiece.RISE + (int) (travel * 2) + 10,
                travel, (int) (travel * 2), getPower(spellLevel, entity));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
