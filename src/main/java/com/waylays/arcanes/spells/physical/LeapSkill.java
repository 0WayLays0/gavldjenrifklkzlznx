package com.waylays.arcanes.spells.physical;

import com.waylays.arcanes.ArcanesOublies;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

import java.util.List;

/**
 * Leap: a jump from both legs, 3 blocks high at level 1 and 6 at level 3, keeping the run-up. Feet on the ground
 * only. Coming down from the leap does not hurt: the fall is counted from the top of the leap, so only a fall deeper
 * than the leap's own height (off a cliff) does.
 */
@AutoSpellConfig
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public class LeapSkill extends PhysicalSkill {
    /** Where the leaper's data keeps how high they leapt, and until when the leap shields their landing. */
    private static final String HEIGHT_KEY = ArcanesOublies.MODID + ".leap_height";
    private static final String UNTIL_KEY = ArcanesOublies.MODID + ".leap_until";

    public LeapSkill() {
        super("leap", SpellRarity.COMMON, 3, 4);
        this.manaCostPerLevel = 1;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.castTime = 0;
        this.baseManaCost = 6;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.arcanes_oublies.leap_height", Utils.stringTruncation(getHeight(spellLevel), 1)));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.LEAP;
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
        Vec3 run = Combat.motion(entity).multiply(1, 0, 1).scale(1.15);
        if (run.lengthSqr() > .45 * .45) {
            run = run.normalize().scale(.45);
        }
        Combat.setMotion(entity, new Vec3(run.x, getJumpSpeed(spellLevel), run.z));
        entity.fallDistance = 0;
        entity.getPersistentData().putFloat(HEIGHT_KEY, (float) getHeight(spellLevel));
        entity.getPersistentData().putLong(UNTIL_KEY, level.getGameTime() + 100);
        Combat.groundDust(level, entity.position(), 18, .35);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.GOAT_LONG_JUMP, SoundSource.PLAYERS, 1f, 1.1f);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, .6f, .6f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** How high it goes: 3 blocks at level 1, +1.5 per level (6 at level 3). */
    public double getHeight(int spellLevel) {
        return 3 + (spellLevel - 1) * 1.5;
    }

    /** The upward speed that carries the leaper {@link #getHeight} blocks up (found by halving, gravity as in the game). */
    public double getJumpSpeed(int spellLevel) {
        double target = getHeight(spellLevel);
        double low = .4, high = 2;
        for (int i = 0; i < 30; i++) {
            double mid = (low + high) * .5;
            if (peak(mid) < target) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return high;
    }

    /** How high a body thrown up at {@code speed} rises: each tick it moves, then gravity (0.08) and drag (2%) act. */
    private static double peak(double speed) {
        double y = 0;
        double top = 0;
        double v = speed;
        for (int t = 0; t < 80 && v > 0; t++) {
            y += v;
            top = Math.max(top, y);
            v = (v - .08) * .98;
        }
        return top;
    }

    /** Landing from a leap: the leap's own height is not counted in the fall. */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(UNTIL_KEY)) {
            return;
        }
        if (entity.level().getGameTime() <= data.getLong(UNTIL_KEY)) {
            event.setDistance(Math.max(0, event.getDistance() - data.getFloat(HEIGHT_KEY) - 1));
        }
        data.remove(UNTIL_KEY);
        data.remove(HEIGHT_KEY);
    }
}
