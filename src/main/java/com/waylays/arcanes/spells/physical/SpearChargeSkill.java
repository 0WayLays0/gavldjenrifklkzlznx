package com.waylays.arcanes.spells.physical;

import com.waylays.arcanes.entity.physical.StrikeTrail;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Spear 2, Charge: the spear couched low and the fighter runs straight ahead (about 7 blocks in half a second). The
 * first enemy in the way is skewered, hard, and thrown back, and the charge stops there. Never further than the run.
 */
@AutoSpellConfig
public class SpearChargeSkill extends PhysicalSkill {
    /** The run lasts these ticks, one stride a tick. */
    public static final int START = 1;
    public static final int STRIDES = 10;
    public static final double SPEED = .7;

    public SpearChargeSkill() {
        super("spear_charge", SpellRarity.RARE, 5, 14);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 20;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(damageLine(getStrength(spellLevel, caster) * .8f), weaponLine(1.2f),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(SPEED * STRIDES, 1)));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.SPEAR_CHARGE;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 dir = Combat.facing(entity);
        boolean[] done = {false};
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.HORSE_GALLOP, SoundSource.PLAYERS, .6f, 1.4f);
        for (int i = 0; i <= STRIDES; i++) {
            boolean last = i == STRIDES;
            SkillTimeline.after(entity, START + i, fighter -> {
                if (done[0]) {
                    return;
                }
                fighter.fallDistance = 0;
                double fall = fighter.onGround() ? 0 : Math.min(0, Combat.motion(fighter).y);
                if (last) {
                    Combat.setMotion(fighter, new Vec3(dir.x * .1, fall, dir.z * .1));
                    return;
                }
                Combat.setMotion(fighter, new Vec3(dir.x * SPEED, fall, dir.z * SPEED));
                SpellUtil.particles(level, ParticleTypes.CLOUD, fighter.getX(), fighter.getY() + .1, fighter.getZ(), 1, .2, .05, .2, .01);
                List<LivingEntity> ahead = Combat.inLine(fighter, 1.8, .5);
                if (ahead.isEmpty()) {
                    return;
                }
                done[0] = true;
                LivingEntity target = ahead.get(0);
                StrikeTrail.spawn(level, fighter, StrikeTrail.Style.STAB, 2.5f, 0, 0, false, StrikeTrail.STEEL);
                Combat.setMotion(fighter, new Vec3(dir.x * .1, fall, dir.z * .1));
                if (Combat.hit(this, fighter, target, blow(spellLevel, fighter, target, .8f, 1.2f), 0)) {
                    Combat.setMotion(target, new Vec3(dir.x * 1.2, .35, dir.z * 1.2));
                    level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.3f, .7f);
                    level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, .8f);
                }
            });
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
