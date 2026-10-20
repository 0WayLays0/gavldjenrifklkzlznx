package com.waylays.arcanes.spells.physical;

import com.waylays.arcanes.entity.physical.StrikeTrail;
import com.waylays.arcanes.registry.ModEffects;
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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Dagger 3, Coup de Grâce: the dagger turned point down in the fist and driven into the nearest enemy in front (2.8
 * blocks). It finishes off the wounded: far more against one under a third of its health, more against one already
 * bleeding, pinned or slowed. The point goes in on tick 6.
 */
@AutoSpellConfig
public class DaggerFinisherSkill extends PhysicalSkill {
    public static final double REACH = 2.8;
    public static final int HIT_TICK = 6;
    /** Below this share of its health, an enemy takes {@link #EXECUTE} times the blow. */
    public static final float LOW_HEALTH = .35f;
    public static final float EXECUTE = 2.5f;
    /** Against a bleeding, pinned or slowed enemy. */
    public static final float WOUNDED = 1.6f;

    public DaggerFinisherSkill() {
        super("dagger_finisher", SpellRarity.RARE, 5, 14);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 18;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(damageLine(getStrength(spellLevel, caster) * .8f), weaponLine(1f),
                Component.translatable("ui.arcanes_oublies.execute", Utils.stringTruncation(EXECUTE, 1), Math.round(LOW_HEALTH * 100)),
                Component.translatable("ui.arcanes_oublies.wounded_bonus", Utils.stringTruncation(WOUNDED, 1)));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.DAGGER_FINISHER;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        SkillTimeline.after(entity, HIT_TICK, fighter -> {
            StrikeTrail.spawn(level, fighter, StrikeTrail.Style.STAB, (float) REACH, 0, 0, false, StrikeTrail.STEEL, .45f);
            LivingEntity target = Combat.nearestInArc(fighter, REACH, 40);
            if (target == null) {
                Combat.whiff(level, fighter);
                return;
            }
            float multiplier = multiplier(target);
            if (Combat.hit(this, fighter, target, blow(spellLevel, fighter, target, .8f, 1f) * multiplier, .1)) {
                Vec3 c = target.getBoundingBox().getCenter();
                level.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.2f, multiplier > 2 ? .7f : 1f);
                if (multiplier > 1) {
                    SpellUtil.particles(level, ParticleTypes.DAMAGE_INDICATOR, c.x, c.y, c.z, 8, .25, .3, .25, .2);
                }
            }
        });
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** How much more the blow does against {@code target} for its wounds. */
    public static float multiplier(LivingEntity target) {
        if (target.getHealth() < target.getMaxHealth() * LOW_HEALTH) {
            return EXECUTE;
        }
        if (target.hasEffect(ModEffects.BLEEDING) || target.hasEffect(ModEffects.PINNED) || target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
            return WOUNDED;
        }
        return 1;
    }
}
