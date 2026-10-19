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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Sword 1, Rising Cut: an upward cut from low to high that lifts the enemies in front off their feet (about a block
 * and a half), open to a follow-up.
 */
@AutoSpellConfig
public class SwordRisingCutSkill extends PhysicalSkill {
    public static final double REACH = 3.3;
    public static final int HIT_TICK = 5;

    public SwordRisingCutSkill() {
        super("sword_rising_cut", SpellRarity.UNCOMMON, 5, 7);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 12;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(damageLine(getStrength(spellLevel, caster) * .5f), weaponLine(.9f), Component.translatable("ui.arcanes_oublies.launch"));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.RISING_CUT;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        SkillTimeline.after(entity, HIT_TICK, fighter -> {
            StrikeTrail.spawn(level, fighter, StrikeTrail.Style.SLASH, 3f, 110, 80, false, StrikeTrail.STEEL);
            level.playSound(null, fighter.getX(), fighter.getY(), fighter.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.1f, 1.3f);
            for (LivingEntity target : Combat.inArc(fighter, REACH, 45)) {
                if (Combat.hit(this, fighter, target, blow(spellLevel, fighter, target, .5f, .9f), 0)) {
                    Vec3 v = target.getDeltaMovement();
                    Combat.setMotion(target, new Vec3(v.x * .2, .5 + spellLevel * .02, v.z * .2));
                }
            }
        });
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
