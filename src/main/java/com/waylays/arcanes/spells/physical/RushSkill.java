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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Rush: unlike the Dash, which bursts a fixed few steps, the Rush runs at the enemy the fighter looks at (10 to 14
 * blocks away), steering after it as it moves, and stops right in front of it with a shoulder blow that staggers it.
 * With no one in sight, it is a long low run straight ahead (7 blocks) that stops dead.
 */
@AutoSpellConfig
public class RushSkill extends PhysicalSkill {
    /** Blocks per tick while rushing, and the most ticks a rush may last. */
    public static final double SPEED = 1.1;
    public static final int MAX_TICKS = 16;
    public static final double FREE_DISTANCE = 7;

    public RushSkill() {
        super("rush", SpellRarity.UNCOMMON, 3, 8);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 2;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 10;
    }

    /** How far away the enemy may be: 10 blocks, +2 per level. */
    public static double getRange(int spellLevel) {
        return 10 + (spellLevel - 1) * 2;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(spellLevel), 1)),
                damageLine(getStrength(spellLevel, caster) * .5f));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.DASH;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        LivingEntity prey = findPrey(level, entity, getRange(spellLevel));
        Vec3 straight = Combat.facing(entity);
        Combat.groundDust(level, entity.position(), 14, .3);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, .9f, 1.2f);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 1f, .9f);
        int freeTicks = (int) Math.round(FREE_DISTANCE / SPEED);
        // Shared by the ticks of the run: whether it has ended
        boolean[] done = {false};
        for (int i = 0; i <= MAX_TICKS; i++) {
            int tick = i;
            SkillTimeline.after(entity, i, fighter -> {
                if (done[0]) {
                    return;
                }
                fighter.fallDistance = 0;
                double fall = fighter.onGround() ? 0 : Math.min(0, Combat.motion(fighter).y);
                if (prey == null) {
                    if (tick >= freeTicks) {
                        stop(fighter, straight);
                        done[0] = true;
                        return;
                    }
                    Combat.setMotion(fighter, new Vec3(straight.x * SPEED, fall, straight.z * SPEED));
                } else {
                    Vec3 to = prey.position().subtract(fighter.position()).multiply(1, 0, 1);
                    double reach = prey.getBbWidth() * .5 + fighter.getBbWidth() * .5 + .5;
                    if (!prey.isAlive() || to.length() <= reach + SPEED * .5 || tick == MAX_TICKS) {
                        Vec3 dir = to.lengthSqr() > 1.0E-4 ? to.normalize() : straight;
                        stop(fighter, dir);
                        if (prey.isAlive() && to.length() <= reach + 1.2) {
                            bash(level, spellLevel, fighter, prey, dir);
                        }
                        done[0] = true;
                        return;
                    }
                    // Steers after the enemy, and slows down so as to stop at arm's length
                    double v = Math.min(SPEED, to.length() - reach);
                    Vec3 step = to.normalize().scale(v);
                    Combat.setMotion(fighter, new Vec3(step.x, fall, step.z));
                }
                SpellUtil.particles(level, ParticleTypes.CLOUD, fighter.getX(), fighter.getY() + .2, fighter.getZ(), 2, .15, .05, .15, .01);
                if (tick % 3 == 0) {
                    SpellUtil.particles(level, ParticleTypes.SWEEP_ATTACK, fighter.getX(), fighter.getY() + 1, fighter.getZ(), 1, .1, .2, .1, 0);
                }
            });
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private static void stop(LivingEntity fighter, Vec3 dir) {
        Combat.setMotion(fighter, new Vec3(dir.x * .1, Math.min(0, Combat.motion(fighter).y), dir.z * .1));
        Combat.groundDust(fighter.level(), fighter.position(), 10, .3);
    }

    /** The shoulder blow at the end of the run: light damage, a shove and a stagger. */
    private void bash(Level level, int spellLevel, LivingEntity fighter, LivingEntity prey, Vec3 dir) {
        float damage = blow(spellLevel, fighter, prey, .5f, .3f);
        if (Combat.hit(this, fighter, prey, damage, .7)) {
            prey.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2));
            Vec3 c = prey.getBoundingBox().getCenter();
            SpellUtil.particles(level, ParticleTypes.CRIT, c.x, c.y, c.z, 10, .3, .3, .3, .3);
        }
        level.playSound(null, prey.getX(), prey.getY(), prey.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.1f, .8f);
    }

    /** The enemy the fighter looks at within reach, else the nearest one in a narrow cone ahead. */
    @Nullable
    public static LivingEntity findPrey(Level level, LivingEntity fighter, double range) {
        HitResult hit = Utils.raycastForEntity(level, fighter, (float) range, true, .4f);
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living && living.isAlive()
                && !(living instanceof ArmorStand) && !SpellUtil.isAlly(fighter, living)) {
            return living;
        }
        List<LivingEntity> ahead = Combat.inArc(fighter, range, 15);
        return ahead.isEmpty() ? null : ahead.get(0);
    }
}
