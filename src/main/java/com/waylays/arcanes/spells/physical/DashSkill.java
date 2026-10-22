package com.waylays.arcanes.spells.physical;

import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Dash: a burst of a few steps, 4 to 5 blocks, the way the fighter is already moving (so a step back or to the side
 * dodges), else straight ahead. It lasts a quarter of a second and stops dead, so it never carries further.
 */
@AutoSpellConfig
public class DashSkill extends PhysicalSkill {
    /** Ticks the burst lasts. */
    public static final int STEPS = 5;

    public DashSkill() {
        super("dash", SpellRarity.COMMON, 3, 3);
        this.manaCostPerLevel = 1;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.castTime = 0;
        this.baseManaCost = 6;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getDistance(spellLevel), 1)));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.DASH;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 moving = Combat.motion(entity).multiply(1, 0, 1);
        Vec3 dir = moving.lengthSqr() > .03 * .03 ? moving.normalize() : Combat.facing(entity);
        double speed = getDistance(spellLevel) / STEPS;
        Combat.groundDust(level, entity.position(), 12, .3);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, .8f, 1.7f);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 1f, 1.4f);
        for (int i = 0; i <= STEPS; i++) {
            // The last beat stops the dash dead
            double v = i == STEPS ? .12 : speed;
            SkillTimeline.after(entity, i, fighter -> {
                double fall = fighter.onGround() ? 0 : Math.min(0, Combat.motion(fighter).y);
                Combat.setMotion(fighter, new Vec3(dir.x * v, fall, dir.z * v));
                fighter.fallDistance = 0;
                if (v > .2) {
                    SpellUtil.particles(level, ParticleTypes.CLOUD, fighter.getX(), fighter.getY() + .2, fighter.getZ(), 2, .15, .05, .15, .01);
                }
            });
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** 4 blocks, +0.5 per level (5 at level 3). */
    public double getDistance(int spellLevel) {
        return 4 + (spellLevel - 1) * .5;
    }
}
