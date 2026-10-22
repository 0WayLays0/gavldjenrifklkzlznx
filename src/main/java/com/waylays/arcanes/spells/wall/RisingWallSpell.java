package com.waylays.arcanes.spells.wall;

import com.waylays.arcanes.entity.wall.Rampart;
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

/**
 * Rising Wall: a crenellated wall of real stone bricks builds itself ahead, block by block flying up out of the
 * ground, its mortar glowing: nobody walks through it and shots break on it. It crumbles when its time is up.
 */
@AutoSpellConfig
public class RisingWallSpell extends WallSpell {
    public RisingWallSpell() {
        super("rising_wall", SpellRarity.UNCOMMON, 6, 12, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.castTime = 0;
        this.baseManaCost = 35;
    }

    public static int width(int spellLevel) {
        return spellLevel >= 4 ? 7 : 5;
    }

    public static int height(int spellLevel) {
        return spellLevel >= 3 ? 4 : 3;
    }

    public static int duration(int spellLevel) {
        return 140 + 20 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.wall_size", width(spellLevel), height(spellLevel)),
                Component.translatable("ui.arcanes_oublies.solid_wall"),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(duration(spellLevel), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 dir = Vec3.directionFromRotation(0, entity.getYRot());
        Vec3 at = SpellUtil.groundAt(level, entity.position().add(dir.scale(3)), 3, 1);
        Rampart.raise(level, entity, Rampart.Shape.WALL, at, entity.getYRot(), width(spellLevel), height(spellLevel), duration(spellLevel));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
