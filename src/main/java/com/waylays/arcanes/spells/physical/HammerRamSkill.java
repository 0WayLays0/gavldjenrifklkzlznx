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
 * Hammer 3, Battering Ram: the hammer held level in both hands and rammed straight into the nearest enemy in front
 * (3 blocks), with a step in. It sends them flying back several blocks; one who slams into a wall on the way takes a
 * second blow. It slams on tick 7.
 */
@AutoSpellConfig
public class HammerRamSkill extends PhysicalSkill {
    public static final double REACH = 3;
    public static final int HIT_TICK = 7;
    /** For how long after the blow a wall in the way counts as a slam. */
    public static final int FLIGHT_TICKS = 10;

    public HammerRamSkill() {
        super("hammer_ram", SpellRarity.UNCOMMON, 5, 10);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 14;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float strength = getStrength(spellLevel, caster);
        return List.of(damageLine(strength * .5f), weaponLine(.8f), Component.translatable("ui.arcanes_oublies.wall_slam", damageLine(strength * .6f)));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.HAMMER_RAM;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        SkillTimeline.after(entity, HIT_TICK - 2, fighter ->
                Combat.setMotion(fighter, Combat.motion(fighter).add(Combat.facing(fighter).scale(.3))));
        SkillTimeline.after(entity, HIT_TICK, fighter -> {
            StrikeTrail.spawn(level, fighter, StrikeTrail.Style.PUNCH, 2.6f, 0, 0, false, StrikeTrail.AIR);
            List<LivingEntity> line = Combat.inLine(fighter, REACH, .6);
            if (line.isEmpty()) {
                Combat.whiff(level, fighter);
                return;
            }
            LivingEntity target = line.get(0);
            if (!Combat.hit(this, fighter, target, blow(spellLevel, fighter, target, .5f, .8f), 0)) {
                return;
            }
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.PLAYERS, .8f, .8f);
            Vec3 dir = Combat.facing(fighter);
            double speed = 1.5 + spellLevel * .1;
            Combat.setMotion(target, new Vec3(dir.x * speed, .35, dir.z * speed));
            boolean[] slammed = {false};
            for (int t = 1; t <= FLIGHT_TICKS; t++) {
                SkillTimeline.after(target, t, flying -> {
                    if (slammed[0] || !flying.horizontalCollision) {
                        return;
                    }
                    slammed[0] = true;
                    level.playSound(null, flying.getX(), flying.getY(), flying.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, .6f, .7f);
                    Combat.groundDust(level, flying.position().add(dir.scale(flying.getBbWidth() * .5 + .3)).add(0, flying.getBbHeight() * .5, 0), 14, .3);
                    Combat.hit(this, fighter, flying, getStrength(spellLevel, fighter) * .6f, 0);
                });
            }
        });
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
