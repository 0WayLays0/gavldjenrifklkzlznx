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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Crossbow 3, Harpoon: a barbed bolt on a rope. If it bites into an enemy, the fighter hauls them in to their feet;
 * if it bites into a wall, the rope hauls the fighter there instead (and a fall after it does not hurt). The rope is
 * about 24 blocks long. Shot on tick 4.
 */
@AutoSpellConfig
public class CrossbowHarpoonSkill extends PhysicalSkill implements MissileSkill {
    public static final int SHOT_TICK = 4;
    public static final float SPEED = 2.6f;
    /** Ticks of flight before the rope runs out. */
    public static final int ROPE_TICKS = 11;
    /** How long a fall after being hauled does not hurt. */
    private static final int SAFE_FALL_TICKS = 40;

    public CrossbowHarpoonSkill() {
        super("crossbow_harpoon", SpellRarity.RARE, 5, 12);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 2;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 16;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(damageLine(getStrength(spellLevel, caster) * .5f),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(SPEED * ROPE_TICKS * .9, 0)));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.CROSSBOW_SHOT;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        SkillTimeline.after(entity, SHOT_TICK, shooter -> {
            Missile.shoot(shooter, Missile.Kind.HARPOON, this, spellLevel, getStrength(spellLevel, shooter) * .5f, Missile.launchPoint(shooter),
                    shooter.getLookAngle(), SPEED, 0).withMaxFlight(ROPE_TICKS);
            level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.2f, .6f);
        });
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onMissileHit(Missile missile, LivingEntity target) {
        missile.strike(target, missile.getDamage(), 0);
        if (missile.getOwner() instanceof LivingEntity shooter) {
            haul(target, shooter.position(), 1.4);
            missile.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LEASH_KNOT_PLACE, SoundSource.PLAYERS, 1.2f, .7f);
        }
    }

    @Override
    public void onMissileHitBlock(Missile missile, BlockHitResult hit) {
        if (missile.getOwner() instanceof LivingEntity shooter) {
            haul(shooter, hit.getLocation(), 1);
            missile.level().playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), SoundEvents.LEASH_KNOT_PLACE, SoundSource.PLAYERS, 1.2f, .9f);
        }
        missile.discard();
    }

    /** Throws {@code who} toward {@code to}, to land about {@code stopShort} blocks from it. */
    private static void haul(LivingEntity who, Vec3 to, double stopShort) {
        Vec3 d = to.subtract(who.position());
        double distance = d.length();
        if (distance <= stopShort) {
            return;
        }
        Vec3 flat = d.multiply(1, 0, 1);
        double speed = Math.min(2.2, (distance - stopShort) * .2 + .3);
        Vec3 dir = flat.lengthSqr() > 1.0E-4 ? flat.normalize() : Vec3.ZERO;
        double lift = Math.max(.35, Math.min(1, d.y * .18 + .35));
        Combat.setMotion(who, new Vec3(dir.x * speed, lift, dir.z * speed));
        for (int t = 0; t <= SAFE_FALL_TICKS; t += 2) {
            SkillTimeline.after(who, t, hauled -> hauled.fallDistance = 0);
        }
    }
}
