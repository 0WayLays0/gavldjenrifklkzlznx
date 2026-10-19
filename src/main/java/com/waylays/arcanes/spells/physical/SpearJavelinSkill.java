package com.waylays.arcanes.spells.physical;

import com.waylays.arcanes.entity.physical.Missile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Spear 3, Javelin: a run-up stride and the spear (a spare one) hurled where the fighter looks, fast and far. It goes
 * through the first enemy and on into the next, and the head stays in the wound: the struck are slowed. It leaves the
 * hand on tick 8.
 */
@AutoSpellConfig
public class SpearJavelinSkill extends PhysicalSkill implements MissileSkill {
    public static final int THROW_TICK = 8;
    public static final float SPEED = 2.4f;

    public SpearJavelinSkill() {
        super("spear_javelin", SpellRarity.RARE, 5, 10);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 15;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(damageLine(getStrength(spellLevel, caster) * .9f), weaponLine(1f), Component.translatable("ui.arcanes_oublies.pierce", 2),
                Component.translatable("ui.arcanes_oublies.hamstring"));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.SPEAR_JAVELIN;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        SkillTimeline.after(entity, THROW_TICK, fighter -> {
            float damage = getStrength(spellLevel, fighter) * .9f + weaponDamage(fighter, null);
            Missile.shoot(fighter, Missile.Kind.JAVELIN, this, spellLevel, damage, Missile.launchPoint(fighter), fighter.getLookAngle(), SPEED, 0)
                    .withPierce(1);
            level.playSound(null, fighter.getX(), fighter.getY(), fighter.getZ(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1f, .8f);
        });
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onMissileHit(Missile missile, LivingEntity target) {
        if (missile.strike(target, missile.getDamage(), .8)) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1, false, true, true));
            missile.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1f, .9f);
        }
    }
}
