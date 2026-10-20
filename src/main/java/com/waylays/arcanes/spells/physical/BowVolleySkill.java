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
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Bow 2, Volley: five arrows nocked together and loosed at once in a fan 40 degrees wide, where the archer looks.
 * Each hits for a share of the shot; a bow's Power adds to them. Loosed on tick 12.
 */
@AutoSpellConfig
public class BowVolleySkill extends PhysicalSkill implements MissileSkill {
    public static final int SHOT_TICK = 12;
    public static final int ARROWS = 5;
    public static final float FAN = 40;
    public static final float SPEED = 2.4f;

    public BowVolleySkill() {
        super("bow_volley", SpellRarity.UNCOMMON, 5, 9);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 14;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.arcanes_oublies.arrows", ARROWS, damageLine(getStrength(spellLevel, caster) * .55f)));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.BOW_SHOT;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        SkillTimeline.after(entity, SHOT_TICK, archer -> {
            float damage = getStrength(spellLevel, archer) * .55f * enchantBonus(archer, Enchantments.POWER, .1f);
            Vec3 look = archer.getLookAngle();
            for (int i = 0; i < ARROWS; i++) {
                float yaw = -FAN / 2 + FAN * i / (ARROWS - 1);
                Missile.shoot(archer, Missile.Kind.ARROW, this, spellLevel, damage, Missile.launchPoint(archer), look.yRot(yaw * Mth.DEG_TO_RAD), SPEED, 1);
            }
            level.playSound(null, archer.getX(), archer.getY(), archer.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.2f, .8f);
            level.playSound(null, archer.getX(), archer.getY(), archer.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1f, 1.1f);
        });
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onMissileHit(Missile missile, LivingEntity target) {
        missile.strike(target, missile.getDamage(), .2);
    }
}
