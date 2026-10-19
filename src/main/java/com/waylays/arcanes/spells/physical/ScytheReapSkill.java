package com.waylays.arcanes.spells.physical;

import com.waylays.arcanes.entity.physical.StrikeTrail;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Scythe 1, Reap: the fighter winds the scythe back, then sweeps it low from left to right through everything in a
 * wide half-circle in front (4.5 blocks), cutting at the legs: the victims are slowed down. Long reach, slow wind-up.
 */
@AutoSpellConfig
public class ScytheReapSkill extends PhysicalSkill {
    public static final double REACH = 4.5;
    public static final int HIT_TICK = 23;

    public ScytheReapSkill() {
        super("scythe_reap", SpellRarity.UNCOMMON, 5, 8);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 15;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(damageLine(getStrength(spellLevel, caster) * .5f), weaponLine(1.2f), Component.translatable("ui.arcanes_oublies.hamstring"));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.SCYTHE_SWEEP;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        SkillTimeline.after(entity, HIT_TICK - 6, fighter ->
                level.playSound(null, fighter.getX(), fighter.getY(), fighter.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, .6f, .5f));
        SkillTimeline.after(entity, HIT_TICK, fighter -> {
            StrikeTrail.spawn(level, fighter, StrikeTrail.Style.SLASH, 4.3f, 200, -5, true, StrikeTrail.DARK_STEEL, .3f);
            level.playSound(null, fighter.getX(), fighter.getY(), fighter.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.4f, .6f);
            for (LivingEntity target : Combat.inArc(fighter, REACH, 100)) {
                if (Combat.hit(this, fighter, target, blow(spellLevel, fighter, target, .5f, 1.2f), .2)) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40 + spellLevel * 8, 1, false, true, true));
                }
            }
            Combat.groundDust(level, fighter.position().add(Combat.facing(fighter).scale(2)), 10, 1.2);
        });
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
