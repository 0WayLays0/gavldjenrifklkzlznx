package com.waylays.arcanes.spells.physical;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Leap: a jump from both legs, about twice as high as a normal jump (2.4 to 3 blocks, never high enough to get hurt
 * landing back on the same ground), keeping the run-up. Feet on the ground only.
 */
@AutoSpellConfig
public class LeapSkill extends PhysicalSkill {
    public LeapSkill() {
        super("leap", SpellRarity.COMMON, 3, 4);
        this.manaCostPerLevel = 1;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.castTime = 0;
        this.baseManaCost = 6;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.arcanes_oublies.leap_height", Utils.stringTruncation(getHeight(spellLevel), 1)));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.LEAP;
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (entity.onGround()) {
            return true;
        }
        refuse(entity, "ui.arcanes_oublies.needs_ground");
        return false;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 run = Combat.motion(entity).multiply(1, 0, 1).scale(1.15);
        if (run.lengthSqr() > .45 * .45) {
            run = run.normalize().scale(.45);
        }
        Combat.setMotion(entity, new Vec3(run.x, getJumpSpeed(spellLevel), run.z));
        entity.fallDistance = 0;
        Combat.groundDust(level, entity.position(), 18, .35);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.GOAT_LONG_JUMP, SoundSource.PLAYERS, 1f, 1.1f);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, .6f, .6f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** Upward speed: 0.6 at level 1 (about 2.4 blocks high), +0.04 per level (about 3 blocks at level 3). */
    public double getJumpSpeed(int spellLevel) {
        return .6 + (spellLevel - 1) * .04;
    }

    /** Roughly how high it goes. */
    public double getHeight(int spellLevel) {
        double y = 0;
        double v = getJumpSpeed(spellLevel);
        double top = 0;
        for (int t = 0; t < 40; t++) {
            y += v;
            top = Math.max(top, y);
            v = (v - .08) * .98;
        }
        return top;
    }
}
