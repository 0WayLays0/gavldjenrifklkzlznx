package com.waylays.arcanes.spells.physical;

import com.waylays.arcanes.entity.physical.StrikeTrail;
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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Sword 3, Disarm: the point binds the blade of the nearest enemy in front (3 blocks), turns it in a tight circle and
 * flicks it out of their grip. A creature's weapon flies off and it fights bare-handed until it picks it back up (a
 * few seconds; nothing is dropped for good); a player keeps their weapon but their grip is gone for as long: Weakness,
 * a raised shield knocked aside, and the item in hand cannot be used. The flick lands on tick 6.
 */
@AutoSpellConfig
public class SwordDisarmSkill extends PhysicalSkill {
    public static final double REACH = 3;
    public static final int HIT_TICK = 6;

    public SwordDisarmSkill() {
        super("sword_disarm", SpellRarity.RARE, 5, 14);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 2;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 18;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(damageLine(getStrength(spellLevel, caster) * .4f), weaponLine(.6f),
                Component.translatable("ui.arcanes_oublies.disarmed", Utils.timeFromTicks(getDisarmTicks(spellLevel), 1)));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.SWORD_DISARM;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        SkillTimeline.after(entity, HIT_TICK, fighter -> {
            StrikeTrail.spawn(level, fighter, StrikeTrail.Style.SLASH, 2.2f, 100, 90, true, StrikeTrail.STEEL);
            LivingEntity target = Combat.nearestInArc(fighter, REACH, 45);
            if (target == null) {
                Combat.whiff(level, fighter);
                return;
            }
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, .4f, 1.8f);
            Combat.hit(this, fighter, target, blow(spellLevel, fighter, target, .4f, .6f), .2);
            disarm(level, fighter, target, getDisarmTicks(spellLevel));
        });
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** Takes {@code target}'s grip away for {@code ticks}. */
    public static void disarm(Level level, LivingEntity fighter, LivingEntity target, int ticks) {
        ItemStack weapon = target.getMainHandItem();
        if (target instanceof Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1, false, true, true));
            if (player.isBlocking()) {
                player.disableShield();
            }
            if (!weapon.isEmpty()) {
                player.getCooldowns().addCooldown(weapon.getItem(), ticks);
            }
            return;
        }
        if (weapon.isEmpty()) {
            return;
        }
        ItemStack held = weapon.copy();
        target.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        // What flies away is only for show: nobody can pick it up, and the creature gets its own back in a while
        ItemEntity flying = new ItemEntity(level, target.getX(), target.getEyeY() - .3, target.getZ(), held.copy());
        Vec3 away = target.position().subtract(fighter.position()).multiply(1, 0, 1).normalize().scale(.25);
        flying.setDeltaMovement(away.x, .35, away.z);
        flying.setNeverPickUp();
        level.addFreshEntity(flying);
        SkillTimeline.after(fighter, 30, f -> flying.discard());
        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        SkillTimeline.after(target, ticks, t -> {
            if (t.getMainHandItem().isEmpty()) {
                t.setItemSlot(EquipmentSlot.MAINHAND, held);
            }
        });
    }

    /** 3.5 s, +0.5 s per level. */
    public int getDisarmTicks(int spellLevel) {
        return 60 + spellLevel * 10;
    }
}
