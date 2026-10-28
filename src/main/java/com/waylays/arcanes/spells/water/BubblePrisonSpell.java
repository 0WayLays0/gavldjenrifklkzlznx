package com.waylays.arcanes.spells.water;

import com.waylays.arcanes.entity.water.WaterBubble;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.Comparator;

/**
 * Bubble Prison: a great soap bubble closes round the enemy looked at (or the one nearest where the caster points),
 * carries it up into the air, drowning, then bursts and lets it fall (see WaterBubble).
 */
@AutoSpellConfig
public class BubblePrisonSpell extends WaterSpell {
    public static final double RANGE = 22;

    public BubblePrisonSpell() {
        super("bubble_prison", SpellRarity.UNCOMMON, 8, 14, CastType.INSTANT, SpellAnimations.ANIMATION_INSTANT_CAST);
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 35;
    }

    public static int hold(int spellLevel) {
        return 50 + 6 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.burst_damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(hold(spellLevel), 1)),
                Component.translatable("ui.irons_spellbooks.cast_range", Utils.stringTruncation(RANGE, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        LivingEntity target = aimed(level, entity, RANGE);
        if (target == null) {
            Vec3 spot = aimedSpot(level, entity, RANGE);
            target = SpellUtil.enemiesAround(level, spot.add(0, 1, 0), 4, entity, x -> true).stream()
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(spot))).orElse(null);
        }
        if (target != null) {
            WaterBubble.trap(level, entity, this, target, hold(spellLevel), getPower(spellLevel, entity));
        } else {
            WaterFx.bubbles(level, aimedSpot(level, entity, RANGE).add(0, 1, 0), 20, .6);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
