package com.waylays.arcanes.spells.physical;

import com.waylays.arcanes.entity.physical.Missile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Axe 2, Axe Throw: the axe drawn back behind the head and thrown overhand, tumbling end over end where the fighter
 * looks. It falls like anything thrown, hits the first enemy in its way hard and stays there (the fighter keeps their
 * own: it is a spare). It leaves the hand on tick 7.
 */
@AutoSpellConfig
public class AxeThrowSkill extends PhysicalSkill implements MissileSkill {
    public static final int THROW_TICK = 7;
    public static final float SPEED = 1.7f;

    public AxeThrowSkill() {
        super("axe_throw", SpellRarity.UNCOMMON, 5, 8);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 12;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(damageLine(getStrength(spellLevel, caster) * .8f), weaponLine(1f));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.AXE_THROW;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        SkillTimeline.after(entity, THROW_TICK, fighter -> {
            ItemStack held = fighter.getMainHandItem();
            float damage = getStrength(spellLevel, fighter) * .8f + weaponDamage(fighter, null);
            Missile.shoot(fighter, Missile.Kind.AXE, this, spellLevel, damage, Missile.launchPoint(fighter), fighter.getLookAngle(), SPEED, 0)
                    .withItem(held.getItem() instanceof AxeItem ? held : new ItemStack(Items.IRON_AXE));
            level.playSound(null, fighter.getX(), fighter.getY(), fighter.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, .8f, .7f);
        });
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onMissileHit(Missile missile, LivingEntity target) {
        if (missile.strike(target, missile.getDamage(), .6)) {
            missile.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.1f, .8f);
        }
    }
}
