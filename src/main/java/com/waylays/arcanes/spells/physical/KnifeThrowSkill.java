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

import java.util.List;

/**
 * Throwing Knives 1, Precise Throw: one knife flicked overhand where the fighter aims, fast and straight. It leaves
 * the struck bleeding, and from far away (8 blocks or more) it finds a weak spot and hits harder. Thrown on tick 5.
 */
@AutoSpellConfig
public class KnifeThrowSkill extends PhysicalSkill implements MissileSkill {
    public static final int THROW_TICK = 5;
    public static final float SPEED = 2.6f;
    public static final double LONG_THROW = 8;
    public static final float LONG_THROW_BONUS = 1.5f;

    public KnifeThrowSkill() {
        super("knife_throw", SpellRarity.COMMON, 5, 4);
        this.manaCostPerLevel = 1;
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 8;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(damageLine(getStrength(spellLevel, caster)), weaponLine(.3f),
                Component.translatable("ui.arcanes_oublies.long_throw", Utils.stringTruncation(LONG_THROW_BONUS, 1), Utils.stringTruncation(LONG_THROW, 0)),
                Component.translatable("ui.arcanes_oublies.bleeding", Utils.timeFromTicks(60, 1)));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.KNIFE_THROW;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        SkillTimeline.after(entity, THROW_TICK, thrower -> {
            float damage = getStrength(spellLevel, thrower) + weaponDamage(thrower, null) * .3f;
            Missile.shoot(thrower, Missile.Kind.KNIFE, this, spellLevel, damage, Missile.launchPoint(thrower), thrower.getLookAngle(), SPEED, 0);
            level.playSound(null, thrower.getX(), thrower.getY(), thrower.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, .5f, 1.8f);
        });
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onMissileHit(Missile missile, LivingEntity target) {
        boolean far = missile.getOwner() != null && missile.getOwner().distanceTo(target) >= LONG_THROW;
        if (missile.strike(target, missile.getDamage() * (far ? LONG_THROW_BONUS : 1), .1)) {
            target.addEffect(new MobEffectInstance(ModEffects.BLEEDING.get(), 60, 0, false, true, true));
            missile.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, .8f, far ? 1.2f : 1.5f);
        }
    }
}
