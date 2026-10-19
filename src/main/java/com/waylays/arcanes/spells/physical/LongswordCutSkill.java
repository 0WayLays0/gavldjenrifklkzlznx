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
 * Longsword 1, Wrath Cut: the longsword raised over the right shoulder in both hands and cut down on a slant, from
 * the right shoulder to the left hip, through everyone in front (3.6 blocks, a wide arc). The weight of it staggers
 * them back. It strikes on tick 8.
 */
@AutoSpellConfig
public class LongswordCutSkill extends PhysicalSkill {
    public static final double REACH = 3.6;
    public static final int HIT_TICK = 8;

    public LongswordCutSkill() {
        super("longsword_cut", SpellRarity.UNCOMMON, 5, 8);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 14;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(damageLine(getStrength(spellLevel, caster) * .6f), weaponLine(1.3f), Component.translatable("ui.arcanes_oublies.stagger"));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.LONGSWORD_CUT;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        SkillTimeline.after(entity, HIT_TICK - 2, fighter ->
                Combat.setMotion(fighter, Combat.motion(fighter).add(Combat.facing(fighter).scale(.25))));
        SkillTimeline.after(entity, HIT_TICK, fighter -> {
            StrikeTrail.spawn(level, fighter, StrikeTrail.Style.SLASH, 3.4f, 140, -55, false, StrikeTrail.STEEL);
            level.playSound(null, fighter.getX(), fighter.getY(), fighter.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, .7f);
            List<LivingEntity> targets = Combat.inArc(fighter, REACH, 60);
            for (LivingEntity target : targets) {
                if (Combat.hit(this, fighter, target, blow(spellLevel, fighter, target, .6f, 1.3f), .6)) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, true, true));
                }
            }
            if (!targets.isEmpty()) {
                level.playSound(null, fighter.getX(), fighter.getY(), fighter.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1f, .8f);
            }
        });
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
