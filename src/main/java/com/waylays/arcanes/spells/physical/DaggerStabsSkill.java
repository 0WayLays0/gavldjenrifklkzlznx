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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Dagger 1, Quick Stabs: three fast thrusts at the nearest enemy in front (2.6 blocks). Each one leaves it bleeding;
 * the third deepens the wound.
 */
@AutoSpellConfig
public class DaggerStabsSkill extends PhysicalSkill {
    public static final double LENGTH = 2.6;
    public static final int[] STABS = {2, 5, 8};

    public DaggerStabsSkill() {
        super("dagger_stabs", SpellRarity.UNCOMMON, 5, 6);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 2;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 10;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.stabs", STABS.length, damageLine(getStrength(spellLevel, caster) * .35f)),
                weaponLine(.5f),
                Component.translatable("ui.arcanes_oublies.bleeding", Utils.timeFromTicks(getBleedTicks(spellLevel), 1))
        );
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.QUICK_STABS;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        for (int i = 0; i < STABS.length; i++) {
            boolean deep = i == STABS.length - 1;
            SkillTimeline.after(entity, STABS[i], fighter -> {
                StrikeTrail.spawn(level, fighter, StrikeTrail.Style.STAB, (float) LENGTH, 0, 0, false, StrikeTrail.STEEL);
                List<LivingEntity> targets = Combat.inLine(fighter, LENGTH, .45);
                if (targets.isEmpty()) {
                    level.playSound(null, fighter.getX(), fighter.getY(), fighter.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, .8f, 1.5f);
                    return;
                }
                LivingEntity target = targets.get(0);
                if (Combat.hit(this, fighter, target, blow(spellLevel, fighter, target, .35f, .5f), 0)) {
                    target.addEffect(new MobEffectInstance(ModEffects.BLEEDING.get(), getBleedTicks(spellLevel), deep ? 1 : 0, false, true, true));
                    level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, .9f, 1.4f);
                }
            });
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** 4 s, +1 s per level. */
    public int getBleedTicks(int spellLevel) {
        return 60 + spellLevel * 20;
    }
}
