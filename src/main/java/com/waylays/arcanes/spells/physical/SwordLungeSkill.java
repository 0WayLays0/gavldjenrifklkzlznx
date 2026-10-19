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
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Sword 2, Lunge: the fighter springs forward about two blocks and drives the point straight ahead, through everyone
 * on the line (3.5 blocks).
 */
@AutoSpellConfig
public class SwordLungeSkill extends PhysicalSkill {
    public static final double LENGTH = 3.5;
    /** The spring forward, then the thrust as the arm drives in. */
    public static final int LUNGE_START = 3;
    public static final int HIT_TICK = 7;

    public SwordLungeSkill() {
        super("sword_lunge", SpellRarity.UNCOMMON, 5, 8);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 14;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(damageLine(getStrength(spellLevel, caster) * .5f), weaponLine(1.1f), Component.translatable("ui.arcanes_oublies.pierce_line"));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.LUNGE;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 dir = Combat.facing(entity);
        for (int t = LUNGE_START; t <= HIT_TICK; t++) {
            double v = t == HIT_TICK ? .1 : .55;
            SkillTimeline.after(entity, t, fighter -> {
                double fall = fighter.onGround() ? 0 : Math.min(0, Combat.motion(fighter).y);
                Combat.setMotion(fighter, new Vec3(dir.x * v, fall, dir.z * v));
            });
        }
        SkillTimeline.after(entity, LUNGE_START, fighter -> Combat.groundDust(level, fighter.position(), 8, .3));
        SkillTimeline.after(entity, HIT_TICK, fighter -> {
            StrikeTrail.spawn(level, fighter, StrikeTrail.Style.STAB, (float) LENGTH, 0, 0, false, StrikeTrail.STEEL);
            level.playSound(null, fighter.getX(), fighter.getY(), fighter.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1f, 1.3f);
            for (LivingEntity target : Combat.inLine(fighter, LENGTH, .5)) {
                Combat.hit(this, fighter, target, blow(spellLevel, fighter, target, .5f, 1.1f), .4);
            }
        });
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
