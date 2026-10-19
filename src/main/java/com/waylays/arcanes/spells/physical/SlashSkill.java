package com.waylays.arcanes.spells.physical;

import com.waylays.arcanes.entity.physical.StrikeTrail;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Slash: one fast diagonal cut, with a blade or the edge of the hand, through everyone in a wide arc in front
 * (3.2 blocks). A blade adds a good share of its damage.
 */
@AutoSpellConfig
public class SlashSkill extends PhysicalSkill {
    public static final double REACH = 3.2;
    public static final int HIT_TICK = 3;

    public SlashSkill() {
        super("slash", SpellRarity.COMMON, 5, 4);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 10;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(damageLine(getStrength(spellLevel, caster) * .8f), weaponLine(.6f));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.SLASH;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        SkillTimeline.after(entity, HIT_TICK, fighter -> {
            StrikeTrail.spawn(level, fighter, StrikeTrail.Style.SLASH, 3f, 130, 35, false, StrikeTrail.STEEL);
            level.playSound(null, fighter.getX(), fighter.getY(), fighter.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 1.1f);
            for (LivingEntity target : Combat.inArc(fighter, REACH, 60)) {
                Combat.hit(this, fighter, target, blow(spellLevel, fighter, target, .8f, .6f), .3);
            }
        });
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
