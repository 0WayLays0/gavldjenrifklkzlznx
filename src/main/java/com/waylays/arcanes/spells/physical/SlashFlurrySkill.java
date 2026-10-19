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
 * Slash Flurry: three cuts back and forth, forehand then backhand, then a wide level cut that pushes everyone back,
 * all in a second, through everyone in front (3.2 blocks).
 */
@AutoSpellConfig
public class SlashFlurrySkill extends PhysicalSkill {
    public static final double REACH = 3.2;
    /** When each cut lands, timed on the animation; the last one is the wide cut. */
    public static final int[] CUTS = {3, 8, 13, 18};

    public SlashFlurrySkill() {
        super("slash_flurry", SpellRarity.UNCOMMON, 5, 12);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 20;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float strength = getStrength(spellLevel, caster);
        return List.of(
                Component.translatable("ui.arcanes_oublies.cuts", CUTS.length - 1, damageLine(strength * .5f)),
                Component.translatable("ui.arcanes_oublies.finisher", damageLine(strength * .9f)),
                weaponLine(.4f)
        );
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.SLASH_FLURRY;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        for (int i = 0; i < CUTS.length; i++) {
            int index = i;
            SkillTimeline.after(entity, CUTS[i], fighter -> cut(level, spellLevel, fighter, index));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private void cut(Level level, int spellLevel, LivingEntity fighter, int index) {
        boolean last = index == CUTS.length - 1;
        boolean backhand = index % 2 == 1;
        if (last) {
            StrikeTrail.spawn(level, fighter, StrikeTrail.Style.SLASH, 3.2f, 170, 5, false, StrikeTrail.STEEL);
        } else {
            StrikeTrail.spawn(level, fighter, StrikeTrail.Style.SLASH, 2.9f, 120, backhand ? -30 : 30, backhand, StrikeTrail.STEEL);
        }
        level.playSound(null, fighter.getX(), fighter.getY(), fighter.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, last ? .9f : 1.2f + index * .1f);
        for (LivingEntity target : Combat.inArc(fighter, REACH, last ? 80 : 60)) {
            float damage = last ? blow(spellLevel, fighter, target, .9f, .6f) : blow(spellLevel, fighter, target, .5f, .4f);
            Combat.hit(this, fighter, target, damage, last ? .6 : .05);
        }
    }
}
