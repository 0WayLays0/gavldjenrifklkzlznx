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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Crossbow 2, Bolt Burst: three bolts shot one after another, a fifth of a second apart, each where the fighter aims
 * at that moment, so a moving target can be followed. Shot on ticks 4, 8 and 12.
 */
@AutoSpellConfig
public class CrossbowBurstSkill extends PhysicalSkill implements MissileSkill {
    public static final int[] SHOTS = {4, 8, 12};
    public static final float SPEED = 3.2f;

    public CrossbowBurstSkill() {
        super("crossbow_burst", SpellRarity.UNCOMMON, 5, 10);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 15;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.arcanes_oublies.bolts", SHOTS.length, damageLine(getStrength(spellLevel, caster) * .6f)));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.CROSSBOW_BURST;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        for (int shot : SHOTS) {
            SkillTimeline.after(entity, shot, shooter -> {
                Missile.shoot(shooter, Missile.Kind.BOLT, this, spellLevel, getStrength(spellLevel, shooter) * .6f, Missile.launchPoint(shooter),
                        shooter.getLookAngle(), SPEED, .5f);
                level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1f, 1.1f);
            });
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onMissileHit(Missile missile, LivingEntity target) {
        missile.strike(target, missile.getDamage(), .2);
    }
}
