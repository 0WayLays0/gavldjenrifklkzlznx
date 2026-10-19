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
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Punch: the fist chambered at the hip, then a straight punch with the hips and shoulders turning in behind it. It
 * hits the nearest enemy in front (3 blocks) hard and shoves them back, with a burst of air at the fist.
 */
@AutoSpellConfig
public class PunchSkill extends PhysicalSkill {
    public static final double REACH = 3;
    public static final int HIT_TICK = 4;

    public PunchSkill() {
        super("punch", SpellRarity.COMMON, 5, 3);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 8;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(damageLine(getStrength(spellLevel, caster)), weaponLine(.25f));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.PUNCH;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        // A short step into the punch
        Combat.setMotion(entity, Combat.motion(entity).add(Combat.facing(entity).scale(.25)));
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.PLAYERS, 1f, .7f);
        SkillTimeline.after(entity, HIT_TICK, fighter -> {
            StrikeTrail.spawn(level, fighter, StrikeTrail.Style.PUNCH, 2.2f, 0, 0, false, StrikeTrail.AIR);
            List<LivingEntity> targets = Combat.inArc(fighter, REACH, 35);
            if (targets.isEmpty()) {
                level.playSound(null, fighter.getX(), fighter.getY(), fighter.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 1f, 1.2f);
                return;
            }
            LivingEntity target = targets.get(0);
            if (Combat.hit(this, fighter, target, blow(spellLevel, fighter, target, 1f, .25f), .8)) {
                Vec3 c = target.getBoundingBox().getCenter();
                level.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.3f, .8f);
                level.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1f, .7f);
            }
        });
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
