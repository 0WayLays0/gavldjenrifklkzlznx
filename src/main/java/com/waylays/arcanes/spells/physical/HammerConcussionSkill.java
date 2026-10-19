package com.waylays.arcanes.spells.physical;

import com.waylays.arcanes.entity.physical.StrikeTrail;
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
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Hammer 1, Concussive Blow: a flat two-handed swing at the head of the nearest enemy in front (3.2 blocks). The blow
 * spins them round: they end up facing the other way (a player's view is turned with them), dizzy (nausea) and
 * unsteady (slowness) for a few seconds. It connects on tick 8.
 */
@AutoSpellConfig
public class HammerConcussionSkill extends PhysicalSkill {
    public static final double REACH = 3.2;
    public static final int HIT_TICK = 8;

    public HammerConcussionSkill() {
        super("hammer_concussion", SpellRarity.UNCOMMON, 5, 12);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 16;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(damageLine(getStrength(spellLevel, caster) * .6f), weaponLine(1f),
                Component.translatable("ui.arcanes_oublies.dazed", Utils.timeFromTicks(getDazeTicks(spellLevel), 1)));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.HAMMER_CONCUSSION;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        SkillTimeline.after(entity, HIT_TICK - 2, fighter ->
                level.playSound(null, fighter.getX(), fighter.getY(), fighter.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, .7f, .5f));
        SkillTimeline.after(entity, HIT_TICK, fighter -> {
            StrikeTrail.spawn(level, fighter, StrikeTrail.Style.SLASH, 2.8f, 120, 0, false, StrikeTrail.DARK_STEEL, .8f);
            LivingEntity target = Combat.nearestInArc(fighter, REACH, 50);
            if (target == null) {
                Combat.whiff(level, fighter);
                return;
            }
            if (!Combat.hit(this, fighter, target, blow(spellLevel, fighter, target, .6f, 1f), .3)) {
                return;
            }
            Combat.turn(target, 180);
            int daze = getDazeTicks(spellLevel);
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, daze + 40, 0, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, daze, 0, false, true, true));
            if (target instanceof Mob mob) {
                mob.getNavigation().stop();
            }
            level.playSound(null, target.getX(), target.getEyeY(), target.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, .5f, 1.6f);
            level.playSound(null, target.getX(), target.getEyeY(), target.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.2f, .7f);
            // Stars round the head
            for (int i = 0; i < 8; i++) {
                float a = i * Mth.TWO_PI / 8;
                SpellUtil.particles(level, ParticleTypes.CRIT, target.getX() + Mth.cos(a) * .45, target.getEyeY() + .35, target.getZ() + Mth.sin(a) * .45,
                        1, 0, 0, 0, 0);
            }
        });
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** 2 s, +0.5 s per level. */
    public int getDazeTicks(int spellLevel) {
        return 30 + spellLevel * 10;
    }
}
