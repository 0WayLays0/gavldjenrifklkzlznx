package com.waylays.arcanes.spells.physical;

import com.waylays.arcanes.entity.physical.Missile;
import com.waylays.arcanes.spells.SpellUtil;
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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Bow 3, Arrow Rain: arrows loosed high into the sky one after another, to come down a second later on the spot the
 * archer looked at (up to 32 blocks away): a hail of arrows over a circle 3.5 blocks wide. Never on the archer's
 * allies. The first goes up on tick 12.
 */
@AutoSpellConfig
public class BowRainSkill extends PhysicalSkill implements MissileSkill {
    public static final int SHOT_TICK = 12;
    /** The arrows start coming down this long after the shot, one every other tick. */
    public static final int FALL_DELAY = 20;
    public static final double RANGE = 32;
    public static final double RADIUS = 3.5;
    private static final double DROP_HEIGHT = 14;

    public BowRainSkill() {
        super("bow_rain", SpellRarity.RARE, 5, 18);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 25;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.arcanes_oublies.arrows", getArrows(spellLevel), damageLine(getStrength(spellLevel, caster) * .5f)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.BOW_RAIN;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        SkillTimeline.after(entity, SHOT_TICK, archer -> {
            Vec3 spot = target(level, archer);
            float damage = getStrength(spellLevel, archer) * .5f * enchantBonus(archer, Enchantments.POWER_ARROWS, .1f);
            // Arrows going up, for show: they vanish high above
            for (int i = 0; i < 3; i++) {
                Missile.shoot(archer, Missile.Kind.ARROW, this, spellLevel, 0, Missile.launchPoint(archer), new Vec3(0, 1, 0), 2.6f, 6)
                        .withMaxFlight(8);
            }
            level.playSound(null, archer.getX(), archer.getY(), archer.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.2f, .7f);
            RandomSource random = archer.getRandom();
            for (int i = 0; i < getArrows(spellLevel); i++) {
                SkillTimeline.after(archer, FALL_DELAY + i * 2, a -> {
                    double angle = random.nextDouble() * Mth.TWO_PI;
                    double r = RADIUS * Math.sqrt(random.nextDouble());
                    Vec3 from = spot.add(Math.cos(angle) * r, DROP_HEIGHT, Math.sin(angle) * r);
                    Missile.shoot(a, Missile.Kind.ARROW, this, spellLevel, damage, from, new Vec3(random.nextGaussian() * .03, -1, random.nextGaussian() * .03),
                            2.2f, 0);
                    if (random.nextInt(3) == 0) {
                        level.playSound(null, from.x, spot.y + 2, from.z, SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, .5f, 1.5f);
                    }
                });
            }
        });
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** Where the archer looks, on the ground: the first block in sight, or the ground under the point at full range. */
    public static Vec3 target(Level level, LivingEntity archer) {
        Vec3 eye = archer.getEyePosition();
        Vec3 far = eye.add(archer.getLookAngle().scale(RANGE));
        BlockHitResult sight = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, archer));
        return sight.getType() == HitResult.Type.MISS ? SpellUtil.groundAt(level, far, 24, 2) : sight.getLocation();
    }

    @Override
    public void onMissileHit(Missile missile, LivingEntity target) {
        if (missile.getDamage() > 0) {
            missile.strike(target, missile.getDamage(), 0);
        }
    }

    /** 10 arrows, +2 per level. */
    public int getArrows(int spellLevel) {
        return 8 + spellLevel * 2;
    }
}
