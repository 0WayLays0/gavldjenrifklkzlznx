package com.waylays.arcanes.spells.physical;

import com.waylays.arcanes.entity.physical.StrikeTrail;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Axe 1, Cleave: the axe raised straight over the head and chopped down hard on the nearest enemy in front (3.2
 * blocks). A raised shield does not stop it: the axe knocks it aside first (it cannot be raised again for 5 seconds),
 * then bites. It bites on tick 8.
 */
@AutoSpellConfig
public class AxeCleaveSkill extends PhysicalSkill {
    public static final double REACH = 3.2;
    public static final int HIT_TICK = 8;

    public AxeCleaveSkill() {
        super("axe_cleave", SpellRarity.UNCOMMON, 5, 8);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 12;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(damageLine(getStrength(spellLevel, caster) * .7f), weaponLine(1.2f), Component.translatable("ui.arcanes_oublies.shield_breaker"));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.AXE_CLEAVE;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        SkillTimeline.after(entity, HIT_TICK, fighter -> {
            StrikeTrail.spawn(level, fighter, StrikeTrail.Style.SLASH, 2.6f, 80, 90, false, StrikeTrail.STEEL);
            LivingEntity target = Combat.nearestInArc(fighter, REACH, 40);
            if (target == null) {
                Combat.whiff(level, fighter);
                return;
            }
            if (target instanceof Player player && player.isBlocking()) {
                player.disableShield(true);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 1f, .9f);
            }
            if (Combat.hit(this, fighter, target, blow(spellLevel, fighter, target, .7f, 1.2f), .3)) {
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.1f, .7f);
            }
        });
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
