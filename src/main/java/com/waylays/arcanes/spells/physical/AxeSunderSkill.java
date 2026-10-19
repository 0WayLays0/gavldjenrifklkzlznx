package com.waylays.arcanes.spells.physical;

import com.waylays.arcanes.entity.physical.StrikeTrail;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Axe 3, Armor Splitter: a slanting chop through the guard and armor of the nearest enemy in front (3.2 blocks), from
 * the right shoulder down. Its armor is split open (Sundered: a quarter of the armor and toughness gone, half from level
 * 4, for several seconds) and every piece it wears takes wear. It bites on tick 8.
 */
@AutoSpellConfig
public class AxeSunderSkill extends PhysicalSkill {
    public static final double REACH = 3.2;
    public static final int HIT_TICK = 8;

    public AxeSunderSkill() {
        super("axe_sunder", SpellRarity.RARE, 5, 14);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 18;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(damageLine(getStrength(spellLevel, caster) * .6f), weaponLine(1f),
                Component.translatable("ui.arcanes_oublies.sundered", Math.round(25 * (getSunderLevel(spellLevel) + 1)),
                        Utils.timeFromTicks(getSunderTicks(spellLevel), 1)));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.AXE_SUNDER;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        SkillTimeline.after(entity, HIT_TICK, fighter -> {
            StrikeTrail.spawn(level, fighter, StrikeTrail.Style.SLASH, 2.6f, 110, -50, false, StrikeTrail.STEEL);
            LivingEntity target = Combat.nearestInArc(fighter, REACH, 45);
            if (target == null) {
                Combat.whiff(level, fighter);
                return;
            }
            if (!Combat.hit(this, fighter, target, blow(spellLevel, fighter, target, .6f, 1f), .2)) {
                return;
            }
            target.addEffect(new MobEffectInstance(ModEffects.SUNDERED.get(), getSunderTicks(spellLevel), getSunderLevel(spellLevel), false, true, true));
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack piece = target.getItemBySlot(slot);
                if (piece.isDamageableItem()) {
                    piece.hurtAndBreak(2 + spellLevel, target, wearer -> wearer.broadcastBreakEvent(slot));
                }
            }
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.PLAYERS, .7f, 1.2f);
        });
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** Sundered I, Sundered II from level 4. */
    public int getSunderLevel(int spellLevel) {
        return spellLevel >= 4 ? 1 : 0;
    }

    /** 8 s, +1 s per level. */
    public int getSunderTicks(int spellLevel) {
        return 140 + spellLevel * 20;
    }
}
