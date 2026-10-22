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

/** Rising Wall: a brick wall bursts out of the ground ahead; it stops enemy shots and keeps enemies from crossing. */
@AutoSpellConfig
public class RisingWallSpell extends WallSpell {
    public static final float WIDTH = 5;
    public static final float HEIGHT = 3;

    public RisingWallSpell() {
        super("rising_wall", SpellRarity.UNCOMMON, 6, 12, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.castTime = 0;
        this.baseManaCost = 35;
    }

    public static int duration(int spellLevel) {
        return 100 + 20 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.wall_size", Utils.stringTruncation(WIDTH, 1), Utils.stringTruncation(HEIGHT, 1)),
                Component.translatable("ui.arcanes_oublies.stops_shots"),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(duration(spellLevel), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 dir = Vec3.directionFromRotation(0, entity.getYRot());
        Vec3 at = SpellUtil.groundAt(level, entity.position().add(dir.scale(3)), 3, 1);
        WallPiece.raise(level, entity, this, WallPiece.Kind.STAND, at, entity.getYRot(), WIDTH, HEIGHT, duration(spellLevel), 0, 1, 0);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
