package com.waylays.arcanes.spells.wall;

import com.waylays.arcanes.entity.wall.BrickStorm;
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
 * Brick Rain: a sigil burns where the caster points, a vortex of bricks gathers in the air above it, turning, and pours
 * its bricks down, the first right on the spot.
 */
@AutoSpellConfig
public class BrickRainSpell extends WallSpell {
    public static final float RANGE = 26;
    public static final float RADIUS = 4;
    public static final int TICKS = 60;

    public BrickRainSpell() {
        super("brick_rain", SpellRarity.UNCOMMON, 8, 16, CastType.INSTANT, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 40;
    }

    public static int bricks(int spellLevel) {
        return 14 + 2 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.brick_count", bricks(spellLevel)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 at = aimedSpot(level, entity, RANGE);
        BrickStorm.gather(level, entity, this, at, RADIUS, bricks(spellLevel), TICKS, getPower(spellLevel, entity));
        WallFx.sound(level, at, net.minecraft.sounds.SoundEvents.STONE_BREAK, 1.5f, .5f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
