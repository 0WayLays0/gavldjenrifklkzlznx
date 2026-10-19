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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Throwing Knives 2, Fan of Knives: the throwing hand swept across from right to left, letting five knives go one
 * after another as it crosses the front: they fly out in a fan 60 degrees wide. Thrown on ticks 5 to 7.
 */
@AutoSpellConfig
public class KnifeFanSkill extends PhysicalSkill implements MissileSkill {
    /** The middle knife leaves on this tick; the others a tick before and after, from the right to the left. */
    public static final int THROW_TICK = 6;
    public static final int KNIVES = 5;
    public static final float FAN = 60;
    public static final float SPEED = 2.2f;

    public KnifeFanSkill() {
        super("knife_fan", SpellRarity.UNCOMMON, 5, 8);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 12;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.arcanes_oublies.knives", KNIVES, damageLine(getStrength(spellLevel, caster) * .6f)));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.KNIFE_FAN;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        for (int i = 0; i < KNIVES; i++) {
            // From the right (negative yaw turns toward the right of where the look points) to the left
            float yaw = -FAN / 2 + FAN * i / (KNIVES - 1);
            int tick = THROW_TICK - 1 + (i * 3) / KNIVES;
            SkillTimeline.after(entity, tick, thrower -> {
                Vec3 dir = thrower.getLookAngle().yRot(yaw * Mth.DEG_TO_RAD);
                Missile.shoot(thrower, Missile.Kind.KNIFE, this, spellLevel, getStrength(spellLevel, thrower) * .6f, Missile.launchPoint(thrower), dir, SPEED, .5f);
                level.playSound(null, thrower.getX(), thrower.getY(), thrower.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, .3f, 1.9f);
            });
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onMissileHit(Missile missile, LivingEntity target) {
        if (missile.strike(target, missile.getDamage(), .1)) {
            missile.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, .6f, 1.6f);
        }
    }
}
