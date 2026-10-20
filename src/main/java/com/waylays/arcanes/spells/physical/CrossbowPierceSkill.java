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
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Crossbow 1, Piercing Bolt: a heavy bolt shot flat and very fast where the fighter aims. It goes through 3 enemies
 * and into a fourth (one more per level of Piercing on the crossbow), hitting each one hard. Shot on tick 4.
 */
@AutoSpellConfig
public class CrossbowPierceSkill extends PhysicalSkill implements MissileSkill {
    public static final int SHOT_TICK = 4;
    public static final float SPEED = 3.4f;
    public static final int PIERCE = 3;

    public CrossbowPierceSkill() {
        super("crossbow_pierce", SpellRarity.UNCOMMON, 5, 8);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 14;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(damageLine(getStrength(spellLevel, caster)), Component.translatable("ui.arcanes_oublies.pierce", PIERCE + 1));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.CROSSBOW_SHOT;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        SkillTimeline.after(entity, SHOT_TICK, shooter -> {
            int pierce = PIERCE + enchantLevel(shooter, Enchantments.PIERCING);
            Missile.shoot(shooter, Missile.Kind.BOLT, this, spellLevel, getStrength(spellLevel, shooter), Missile.launchPoint(shooter), shooter.getLookAngle(),
                    SPEED, 0).withPierce(pierce);
            level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.2f, .8f);
        });
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onMissileHit(Missile missile, LivingEntity target) {
        if (missile.strike(target, missile.getDamage(), .4)) {
            missile.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.CROSSBOW_HIT, SoundSource.PLAYERS, 1f, .9f);
        }
    }
}
