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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The Great Rampart: an immense crenellated wall builds itself across the caster's path, sways, shuddering, its mortar
 * heating, then topples forward flat onto everything before it (see WallPiece, TOPPLE).
 */
@AutoSpellConfig
public class GreatRampartSpell extends WallSpell {
    public GreatRampartSpell() {
        super("great_rampart", SpellRarity.EPIC, 5, 32, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 15;
        this.baseSpellPower = 16;
        this.spellPowerPerLevel = 3;
        this.castTime = 0;
        this.baseManaCost = 85;
    }

    public static float width(int spellLevel) {
        return 13 + 2 * spellLevel;
    }

    public static float height(int spellLevel) {
        return 6 + .5f * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.crush_damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.wall_size", Utils.stringTruncation(width(spellLevel), 1),
                        Utils.stringTruncation(height(spellLevel), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 dir = Vec3.directionFromRotation(0, entity.getYRot());
        Vec3 at = SpellUtil.groundAt(level, entity.position().add(dir.scale(2.5)), 3, 1);
        WallPiece.raise(level, entity, this, WallPiece.Kind.TOPPLE, at, entity.getYRot(), width(spellLevel), height(spellLevel), 0, 1,
                getPower(spellLevel, entity));
        WallFx.sigil(level, at, width(spellLevel) * .5f, 40);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
