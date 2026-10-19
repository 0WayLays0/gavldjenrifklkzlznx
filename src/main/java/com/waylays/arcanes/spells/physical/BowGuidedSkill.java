package com.waylays.arcanes.spells.physical;

import com.waylays.arcanes.entity.physical.Missile;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Bow 1, Guided Arrow: an arrow the archer steers with their eyes. For a few seconds after it leaves the bow it bends
 * its flight toward whatever the archer looks at (the creature under their eyes, up to 48 blocks away), without
 * dropping; then it flies on like any arrow. A bow's Power adds to it. It is loosed on tick 12.
 */
@AutoSpellConfig
public class BowGuidedSkill extends PhysicalSkill implements MissileSkill {
    public static final int SHOT_TICK = 12;
    public static final float SPEED = 1.4f;
    /** How sharply it can turn, in degrees a tick: enough to curve onto a target a few blocks off its line. */
    public static final double MAX_TURN = 20;
    public static final double GUIDE_RANGE = 48;

    public BowGuidedSkill() {
        super("bow_guided", SpellRarity.UNCOMMON, 5, 10);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 15;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(damageLine(getStrength(spellLevel, caster)),
                Component.translatable("ui.arcanes_oublies.guided", Utils.timeFromTicks(getGuideTicks(spellLevel), 1)));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.BOW_SHOT;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.CROSSBOW_LOADING_MIDDLE, SoundSource.PLAYERS, .8f, 1.4f);
        SkillTimeline.after(entity, SHOT_TICK, archer -> {
            float damage = getStrength(spellLevel, archer) * enchantBonus(archer, Enchantments.POWER_ARROWS, .1f);
            Missile.shoot(archer, Missile.Kind.ARROW, this, spellLevel, damage, Missile.launchPoint(archer), archer.getLookAngle(), SPEED, 0)
                    .setNoGravity(true);
            level.playSound(null, archer.getX(), archer.getY(), archer.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1f, .9f);
        });
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void tickMissile(Missile missile) {
        if (!(missile.getOwner() instanceof LivingEntity archer) || !archer.isAlive() || archer.level() != missile.level()
                || missile.getFlightTicks() > getGuideTicks(missile.getSkillLevel())) {
            missile.setNoGravity(false);
            return;
        }
        Vec3 to = aimPoint(archer).subtract(missile.position());
        if (to.lengthSqr() < .25) {
            return;
        }
        Vec3 velocity = missile.getDeltaMovement();
        Vec3 want = to.normalize();
        Vec3 heading = velocity.lengthSqr() > 1.0E-6 ? velocity.normalize() : want;
        missile.setDeltaMovement(turnToward(heading, want, Math.toRadians(MAX_TURN)).scale(SPEED));
    }

    /** {@code heading} turned toward {@code want} (both unit vectors) by at most {@code maxAngle} radians. */
    public static Vec3 turnToward(Vec3 heading, Vec3 want, double maxAngle) {
        double angle = Math.acos(Math.max(-1, Math.min(1, heading.dot(want))));
        double sin = Math.sin(angle);
        if (angle <= maxAngle || sin < 1.0E-6) {
            return angle <= maxAngle ? want : heading;
        }
        double t = maxAngle / angle;
        return heading.scale(Math.sin((1 - t) * angle) / sin).add(want.scale(Math.sin(t * angle) / sin));
    }

    /** What the archer looks at: the middle of the creature under their eyes, else the block, else a point far ahead. */
    public static Vec3 aimPoint(LivingEntity archer) {
        HitResult sight = Utils.raycastForEntity(archer.level(), archer, (float) GUIDE_RANGE, true, .3f);
        if (sight instanceof EntityHitResult entityHit) {
            return entityHit.getEntity().getBoundingBox().getCenter();
        }
        return sight.getType() == HitResult.Type.MISS ? archer.getEyePosition().add(archer.getLookAngle().scale(GUIDE_RANGE)) : sight.getLocation();
    }

    @Override
    public void onMissileHit(Missile missile, LivingEntity target) {
        if (missile.strike(target, missile.getDamage(), .4)) {
            missile.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 1f, 1f);
        }
    }

    /** 3 s, +0.5 s per level. */
    public int getGuideTicks(int spellLevel) {
        return 50 + spellLevel * 10;
    }
}
