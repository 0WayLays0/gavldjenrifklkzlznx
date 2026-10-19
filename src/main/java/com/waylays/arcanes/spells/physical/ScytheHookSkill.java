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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Scythe 3, Reaper's Hook: the scythe brought down far in front so the blade hooks behind whoever stands there (up to
 * 5.5 blocks away, in a narrow cone), then dragged back: they are cut and pulled to the fighter's feet, a little
 * slowed. The hook lands on tick 9 of the animation.
 */
@AutoSpellConfig
public class ScytheHookSkill extends PhysicalSkill {
    public static final double REACH = 5.5;
    public static final int HIT_TICK = 9;
    /** How far from the fighter the hooked land. */
    private static final double LANDING = 1.6;

    public ScytheHookSkill() {
        super("scythe_hook", SpellRarity.RARE, 5, 12);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 20;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(damageLine(getStrength(spellLevel, caster) * .5f), weaponLine(1f),
                Component.translatable("ui.arcanes_oublies.hook_pull", Utils.stringTruncation(REACH, 1)));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.SCYTHE_HOOK;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        SkillTimeline.after(entity, HIT_TICK - 4, fighter ->
                level.playSound(null, fighter.getX(), fighter.getY(), fighter.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, .6f, .5f));
        SkillTimeline.after(entity, HIT_TICK, fighter -> {
            StrikeTrail.spawn(level, fighter, StrikeTrail.Style.SLASH, (float) REACH, 70, 80, false, StrikeTrail.DARK_STEEL);
            List<LivingEntity> targets = Combat.inArc(fighter, REACH, 35);
            if (targets.isEmpty()) {
                Combat.whiff(level, fighter);
                return;
            }
            level.playSound(null, fighter.getX(), fighter.getY(), fighter.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1f, .6f);
            for (LivingEntity target : targets) {
                if (Combat.hit(this, fighter, target, blow(spellLevel, fighter, target, .5f, 1f), 0)) {
                    pull(fighter, target);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0, false, true, true));
                }
            }
        });
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** Drags {@code target} across the ground to land about {@link #LANDING} blocks in front of the fighter. */
    private static void pull(LivingEntity fighter, LivingEntity target) {
        Vec3 to = fighter.position().subtract(target.position()).multiply(1, 0, 1);
        double distance = to.length();
        if (distance <= LANDING) {
            return;
        }
        // Ground friction and drag eat about half the speed of a short hop
        double speed = Math.min(1.6, (distance - LANDING) * .3);
        Combat.setMotion(target, to.normalize().scale(speed).add(0, .32, 0));
    }
}
