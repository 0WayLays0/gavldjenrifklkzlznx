package com.waylays.arcanes.spells.physical;

import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Evade: a big leap backward to get out of a fight, 6 to 8 blocks straight back from where the fighter faces, in a
 * low arc (under a block high). It pushes off both feet, so it needs the ground, and it lands without a fall.
 */
@AutoSpellConfig
public class EvadeSkill extends PhysicalSkill {
    /** The body travels from this tick, one beat a tick, then stops. */
    public static final int TAKE_OFF = 2;
    public static final int BEATS = 8;
    /** Upward speed of the first beat; each beat after loses as much as gravity would, so the arc lands level. */
    private static final double RISE = .35;
    private static final double FALL = .1;

    public EvadeSkill() {
        super("evade", SpellRarity.COMMON, 3, 8);
        this.manaCostPerLevel = 1;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.castTime = 0;
        this.baseManaCost = 8;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getDistance(spellLevel), 1)));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.EVADE;
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (entity.onGround()) {
            return true;
        }
        refuse(entity, "ui.arcanes_oublies.needs_ground");
        return false;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 back = Combat.facing(entity).scale(-1);
        double speed = getDistance(spellLevel) / BEATS;
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 1f, 1.2f);
        SkillTimeline.after(entity, TAKE_OFF, fighter -> {
            Combat.groundDust(level, fighter.position(), 16, .35);
            level.playSound(null, fighter.getX(), fighter.getY(), fighter.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, .8f, 1.4f);
        });
        for (int i = 0; i <= BEATS; i++) {
            int beat = i;
            SkillTimeline.after(entity, TAKE_OFF + i, fighter -> {
                fighter.fallDistance = 0;
                if (beat == BEATS) {
                    // Stops dead on landing
                    Combat.setMotion(fighter, new Vec3(back.x * .1, Math.min(0, Combat.motion(fighter).y), back.z * .1));
                    Combat.groundDust(level, fighter.position(), 10, .3);
                    return;
                }
                Combat.setMotion(fighter, new Vec3(back.x * speed, RISE - FALL * beat, back.z * speed));
                SpellUtil.particles(level, ParticleTypes.CLOUD, fighter.getX(), fighter.getY() + .3, fighter.getZ(), 1, .15, .1, .15, .01);
            });
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** 6 blocks, +1 per level. */
    public double getDistance(int spellLevel) {
        return 6 + (spellLevel - 1);
    }
}
