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
 * Longsword 2, Half-Moon: the longsword drawn back low on the right, then swept flat in a wide half circle with a
 * step in, through everyone in front and to the sides (4 blocks), throwing them back. The blade crosses the front on
 * tick 8.
 */
@AutoSpellConfig
public class LongswordSweepSkill extends PhysicalSkill {
    public static final double REACH = 4;
    public static final int HIT_TICK = 8;

    public LongswordSweepSkill() {
        super("longsword_sweep", SpellRarity.RARE, 5, 12);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 18;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(damageLine(getStrength(spellLevel, caster) * .5f), weaponLine(1.1f));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.LONGSWORD_SWEEP;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        SkillTimeline.after(entity, HIT_TICK - 2, fighter ->
                Combat.setMotion(fighter, Combat.motion(fighter).add(Combat.facing(fighter).scale(.3))));
        SkillTimeline.after(entity, HIT_TICK, fighter -> {
            StrikeTrail.spawn(level, fighter, StrikeTrail.Style.SLASH, 3.8f, 200, -8, false, StrikeTrail.STEEL);
            level.playSound(null, fighter.getX(), fighter.getY(), fighter.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.4f, .8f);
            for (LivingEntity target : Combat.inArc(fighter, REACH, 95)) {
                Combat.hit(this, fighter, target, blow(spellLevel, fighter, target, .5f, 1.1f), 1);
            }
        });
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
