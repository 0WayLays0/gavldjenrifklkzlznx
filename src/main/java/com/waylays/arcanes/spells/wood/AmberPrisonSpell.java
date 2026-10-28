package com.waylays.arcanes.spells.wood;

import com.waylays.arcanes.entity.wood.AmberPrison;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Amber Prison: golden sap wells up where the caster points and sets in amber round the enemies there, holding them
 * fast until it bursts in cutting shards (see AmberPrison).
 */
@AutoSpellConfig
public class AmberPrisonSpell extends WoodSpell {
    public static final double RANGE = 20;
    public static final double SPOT = 4;

    public AmberPrisonSpell() {
        super("amber_prison", SpellRarity.EPIC, 5, 30, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 12;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 80;
    }

    public static int hold(int spellLevel) {
        return 50 + 10 * spellLevel;
    }

    public static int prisoners(int spellLevel) {
        return 2 + spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.encased_for", Utils.timeFromTicks(hold(spellLevel), 1), prisoners(spellLevel)),
                Component.translatable("ui.arcanes_oublies.shatter_damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(SPOT, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 spot = aimedSpot(level, entity, RANGE);
        List<LivingEntity> caught = SpellUtil.enemiesAround(level, spot.add(0, 1, 0), SPOT, entity, x -> true);
        caught.sort(Comparator.comparingDouble(e -> e.distanceToSqr(spot)));
        int n = Math.min(prisoners(spellLevel), caught.size());
        for (int i = 0; i < n; i++) {
            AmberPrison.encase(level, entity, this, caught.get(i), hold(spellLevel), getPower(spellLevel, entity));
        }
        // The sap wells up all over the spot
        SpellUtil.particles(level, ParticleTypes.LANDING_HONEY, spot.x, spot.y + .1, spot.z, 40, SPOT * .6, .05, SPOT * .6, 0);
        SpellUtil.particles(level, ParticleTypes.FALLING_HONEY, spot.x, spot.y + 1.5, spot.z, 20, SPOT * .5, .5, SPOT * .5, 0);
        WoodFx.sound(level, spot, SoundEvents.HONEY_BLOCK_SLIDE, 1.6f, .5f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
