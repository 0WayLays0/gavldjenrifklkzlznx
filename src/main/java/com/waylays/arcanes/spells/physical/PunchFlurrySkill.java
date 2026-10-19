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
 * Punch Flurry: feet planted, the fighter throws seven jabs in under a second, right and left from the guard, at
 * everyone in front of them (2.8 blocks), then chambers the rear fist and drives a last straight punch that sends them
 * back.
 */
@AutoSpellConfig
public class PunchFlurrySkill extends PhysicalSkill {
    public static final double REACH = 2.8;
    /** When each jab lands, timed on the animation; the last one is the big punch. */
    public static final int[] BLOWS = {3, 5, 7, 9, 11, 13, 15, 20};

    public PunchFlurrySkill() {
        super("punch_flurry", SpellRarity.UNCOMMON, 5, 10);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 15;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float strength = getStrength(spellLevel, caster);
        return List.of(
                Component.translatable("ui.arcanes_oublies.jabs", BLOWS.length - 1, damageLine(strength * .3f)),
                Component.translatable("ui.arcanes_oublies.finisher", damageLine(strength * .9f)),
                weaponLine(.1f)
        );
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.PUNCH_FLURRY;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        // Feet planted while the fists fly
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BLOWS[BLOWS.length - 1] + 2, 1, false, false, false));
        for (int i = 0; i < BLOWS.length; i++) {
            boolean last = i == BLOWS.length - 1;
            SkillTimeline.after(entity, BLOWS[i], fighter -> jab(level, spellLevel, fighter, last));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private void jab(Level level, int spellLevel, LivingEntity fighter, boolean last) {
        StrikeTrail.spawn(level, fighter, StrikeTrail.Style.PUNCH, last ? 2.2f : 1.6f, 0, 0, false, StrikeTrail.AIR);
        boolean landed = false;
        for (LivingEntity target : Combat.inArc(fighter, REACH, 40)) {
            float damage = last ? blow(spellLevel, fighter, target, .9f, .3f) : blow(spellLevel, fighter, target, .3f, .1f);
            landed |= Combat.hit(this, fighter, target, damage, last ? 1 : .05);
        }
        level.playSound(null, fighter.getX(), fighter.getY(), fighter.getZ(), landed ? SoundEvents.PLAYER_ATTACK_KNOCKBACK : SoundEvents.PLAYER_ATTACK_NODAMAGE,
                SoundSource.PLAYERS, last ? 1.3f : .8f, last ? .8f : 1.2f + fighter.getRandom().nextFloat() * .3f);
    }
}
