package com.waylays.arcanes.spells.wall;

import com.waylays.arcanes.entity.wall.BrickBolt;
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

/** Brick Bolt: a stone brick torn out of nowhere and hurled at the enemy; more bricks at higher levels. */
@AutoSpellConfig
public class BrickBoltSpell extends WallSpell {
    public static final float SPEED = 1.6f;

    public BrickBoltSpell() {
        super("brick_bolt", SpellRarity.COMMON, 10, 2, CastType.INSTANT, SpellAnimations.ANIMATION_INSTANT_CAST);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 15;
    }

    public static int bricks(int spellLevel) {
        return 1 + spellLevel / 4;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.projectile_count", bricks(spellLevel)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 look = entity.getLookAngle();
        Vec3 from = entity.getEyePosition().add(look.scale(.7)).add(0, -.2, 0);
        int n = bricks(spellLevel);
        for (int i = 0; i < n; i++) {
            float spread = n == 1 ? 0 : (i / (n - 1f) - .5f) * 10;
            Vec3 dir = look.yRot(spread * Mth.DEG_TO_RAD);
            BrickBolt.shoot(level, entity, this, BrickBolt.Kind.BOLT, from, dir.scale(SPEED), getPower(spellLevel, entity));
        }
        WallFx.sound(level, from, net.minecraft.sounds.SoundEvents.STONE_PLACE, 1f, .8f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
