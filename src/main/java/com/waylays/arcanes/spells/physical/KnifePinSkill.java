package com.waylays.arcanes.spells.physical;

import com.waylays.arcanes.entity.physical.Missile;
import com.waylays.arcanes.registry.ModEffects;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Throwing Knives 3, Pinning Throw: crouched low, a knife flung underhand at the feet of whoever the fighter aims at.
 * It pins a foot to the ground: no walking and no jumping for a few seconds (Pinned). Thrown on tick 5.
 */
@AutoSpellConfig
public class KnifePinSkill extends PhysicalSkill implements MissileSkill {
    public static final int THROW_TICK = 5;
    public static final float SPEED = 2.4f;

    public KnifePinSkill() {
        super("knife_pin", SpellRarity.UNCOMMON, 5, 12);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 2;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 14;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(damageLine(getStrength(spellLevel, caster) * .6f),
                Component.translatable("ui.arcanes_oublies.pinned", Utils.timeFromTicks(getPinTicks(spellLevel), 1)));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.KNIFE_PIN;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        SkillTimeline.after(entity, THROW_TICK, thrower -> {
            // Thrown low, from the hip
            Vec3 from = thrower.position().add(0, thrower.getBbHeight() * .45, 0).add(Combat.facing(thrower).scale(.3));
            Missile.shoot(thrower, Missile.Kind.KNIFE, this, spellLevel, getStrength(spellLevel, thrower) * .6f, from,
                    thrower.getLookAngle().add(0, -.06, 0), SPEED, 0);
            level.playSound(null, thrower.getX(), thrower.getY(), thrower.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, .5f, 1.6f);
        });
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onMissileHit(Missile missile, LivingEntity target) {
        missile.strike(target, missile.getDamage(), 0);
        target.addEffect(new MobEffectInstance(ModEffects.PINNED.get(), getPinTicks(missile.getSkillLevel()), 0, false, true, true));
        target.setDeltaMovement(target.getDeltaMovement().multiply(0, 1, 0));
        target.hurtMarked = true;
        missile.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.TRIDENT_HIT_GROUND, SoundSource.PLAYERS, .9f, 1.4f);
    }

    /** 2.4 s, +0.4 s per level. */
    public int getPinTicks(int spellLevel) {
        return 40 + spellLevel * 8;
    }
}
