package com.waylays.arcanes.spells.wall;

import com.waylays.arcanes.entity.wall.WallPiece;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import net.minecraft.server.level.ServerLevel;
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

/** Crushing Walls: two walls burst out on either side of the enemy looked at and slam together on it. */
@AutoSpellConfig
public class CrushingWallsSpell extends WallSpell {
    public static final int RANGE = 20;
    public static final float GAP = 3.5f;

    public CrushingWallsSpell() {
        super("crushing_walls", SpellRarity.RARE, 5, 14, CastType.INSTANT, SpellAnimations.ANIMATION_INSTANT_CAST);
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 9;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 50;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.arcanes_oublies.crush_damage", Utils.stringTruncation(power * 2.8f, 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(RANGE, 1)));
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return Utils.preCastTargetHelper(level, entity, playerMagicData, this, RANGE, .35f);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targetData) {
            LivingEntity target = targetData.getTarget((ServerLevel) level);
            if (target != null) {
                crush(level, entity, target.position(), entity.getYRot() + 90, getPower(spellLevel, entity));
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** The two walls, on either side of {@code at} across {@code yaw}, sliding together. */
    public void crush(Level level, LivingEntity caster, Vec3 at, float yaw, float damage) {
        Vec3 dir = Vec3.directionFromRotation(0, yaw);
        float travel = GAP - WallPiece.THICK * .5f - .3f;
        for (int side = -1; side <= 1; side += 2) {
            Vec3 p = SpellUtil.groundAt(level, at.add(dir.scale(side * GAP)), 3, 1);
            WallPiece.raise(level, caster, this, WallPiece.Kind.SLAM, p, side < 0 ? yaw : yaw + 180, 3.2f, 3, 50, travel, 9, damage);
        }
    }
}
