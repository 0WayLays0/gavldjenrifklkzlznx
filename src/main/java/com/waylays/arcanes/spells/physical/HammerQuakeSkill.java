package com.waylays.arcanes.spells.physical;

import com.waylays.arcanes.entity.physical.StrikeTrail;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Hammer 2, Earthshaker: the hammer brought down into the ground just in front, with everything behind it. The
 * shock runs through the ground: everyone standing within 4 blocks of the impact is hurt (fully at the impact, half
 * at the edge), thrown off their feet and slowed. Whoever is off the ground escapes it. It strikes on tick 11.
 */
@AutoSpellConfig
public class HammerQuakeSkill extends PhysicalSkill {
    public static final int HIT_TICK = 11;
    /** The hammer lands this far in front. */
    public static final double IMPACT = 1.8;

    public HammerQuakeSkill() {
        super("hammer_quake", SpellRarity.RARE, 5, 16);
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 25;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(damageLine(getStrength(spellLevel, caster) * .9f), weaponLine(1f),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(getRadius(spellLevel), 1)));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.HAMMER_QUAKE;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        SkillTimeline.after(entity, HIT_TICK - 3, fighter ->
                level.playSound(null, fighter.getX(), fighter.getY(), fighter.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, .8f, .4f));
        SkillTimeline.after(entity, HIT_TICK, fighter -> {
            Vec3 impact = fighter.position().add(Combat.facing(fighter).scale(IMPACT));
            double radius = getRadius(spellLevel);
            level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1f, .5f);
            level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, .5f, .7f);
            StrikeTrail.spawn(level, fighter, StrikeTrail.Style.SPIN, (float) radius, 360, 0, false, StrikeTrail.DARK_STEEL, .05f);
            Combat.groundDust(level, impact, 30, .6);
            for (int i = 0; i < 12; i++) {
                float a = i * Mth.TWO_PI / 12;
                Combat.groundDust(level, impact.add(Mth.cos(a) * radius * .6, 0, Mth.sin(a) * radius * .6), 6, .4);
            }
            SpellUtil.cameraShake(level, impact, 12, (float) radius * 2.5f);
            for (LivingEntity target : SpellUtil.enemiesAround(level, impact, radius, fighter, e -> e.onGround() && e != fighter)) {
                double distance = Math.sqrt(target.distanceToSqr(impact.x, target.getY(), impact.z));
                float falloff = (float) (1 - .5 * Math.min(1, distance / radius));
                if (Combat.hit(this, fighter, target, blow(spellLevel, fighter, target, .9f, 1f) * falloff, 0)) {
                    Vec3 out = target.position().subtract(impact).multiply(1, 0, 1);
                    out = out.lengthSqr() < 1.0E-4 ? Combat.facing(fighter) : out.normalize();
                    Combat.setMotion(target, new Vec3(out.x * .3, .5, out.z * .3));
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1, false, true, true));
                }
            }
        });
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** 4 blocks, +0.25 per level. */
    public double getRadius(int spellLevel) {
        return 3.75 + spellLevel * .25;
    }
}
