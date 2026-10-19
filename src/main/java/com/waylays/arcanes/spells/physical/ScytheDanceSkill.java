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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Scythe 2, Reaper's Dance: a slash in front (4 blocks), then the fighter whirls round with the scythe at arm's length
 * and cuts everything all around (4.5 blocks), throwing it back.
 */
@AutoSpellConfig
public class ScytheDanceSkill extends PhysicalSkill {
    public static final int SLASH_TICK = 15;
    public static final int SPIN_TICK = 37;

    public ScytheDanceSkill() {
        super("scythe_dance", SpellRarity.RARE, 5, 14);
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 25;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float strength = getStrength(spellLevel, caster);
        return List.of(
                Component.translatable("ui.arcanes_oublies.first_cut", damageLine(strength * .5f)),
                Component.translatable("ui.arcanes_oublies.spin_cut", damageLine(strength * .8f)),
                weaponLine(1.3f)
        );
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.SCYTHE_SPIN;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        SkillTimeline.after(entity, SLASH_TICK, fighter -> {
            StrikeTrail.spawn(level, fighter, StrikeTrail.Style.SLASH, 3.8f, 150, 25, false, StrikeTrail.DARK_STEEL);
            level.playSound(null, fighter.getX(), fighter.getY(), fighter.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, .7f);
            for (LivingEntity target : Combat.inArc(fighter, 4, 70)) {
                Combat.hit(this, fighter, target, blow(spellLevel, fighter, target, .5f, .9f), .2);
            }
        });
        SkillTimeline.after(entity, SPIN_TICK, fighter -> {
            StrikeTrail.spawn(level, fighter, StrikeTrail.Style.SPIN, 4.3f, 360, 0, true, StrikeTrail.DARK_STEEL, .5f);
            level.playSound(null, fighter.getX(), fighter.getY(), fighter.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5f, .5f);
            level.playSound(null, fighter.getX(), fighter.getY(), fighter.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1f, .6f);
            for (LivingEntity target : Combat.around(fighter, 4.5)) {
                Combat.hit(this, fighter, target, blow(spellLevel, fighter, target, .8f, 1.3f), .9);
            }
            Combat.groundDust(level, fighter.position(), 16, 1.5);
        });
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
