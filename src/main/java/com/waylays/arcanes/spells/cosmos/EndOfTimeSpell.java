package com.waylays.arcanes.spells.cosmos;

import com.waylays.arcanes.entity.cosmos.TimeStorm;
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

/**
 * The End of Time (ultimate): the caster calls up the storm at the end of all timelines where they aim. It rolls
 * after their foes, swallowing what it covers in darkness and wearing it away, lashing out with tendrils of orange
 * lightning that strike and drag in; what it leaves weak enough is devoured, pruned from time.
 */
@AutoSpellConfig
public class EndOfTimeSpell extends CosmosSpell {
    public static final int RANGE = 24;

    public EndOfTimeSpell() {
        super("end_of_time", SpellRarity.LEGENDARY, 3, 150, CastType.LONG, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 35;
        this.baseSpellPower = 7;
        this.spellPowerPerLevel = 2;
        this.castTime = 40;
        this.baseManaCost = 170;
    }

    public static int life(int spellLevel) {
        return 180 + 40 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.arcanes_oublies.storm_inside", Utils.stringTruncation(power * .45f * 2, 1)),
                Component.translatable("ui.arcanes_oublies.storm_tendrils", TimeStorm.TENDRILS, Utils.stringTruncation(power * .9f, 1)),
                Component.translatable("ui.arcanes_oublies.prune_line", Math.round(Math.min(.3f, .1f + .02f * (spellLevel + 3)) * 100)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(life(spellLevel), 1)));
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (entity.tickCount % 2 == 0) {
            Vec3 at = SpellUtil.aimedSpot(level, entity, RANGE);
            SpellUtil.particles(level, CosmosFx.NEBULA, at.x, at.y + 1, at.z, 6, 2, 1, 2, 0);
            CosmosFx.time(level, at.add(0, 1, 0), 3, 2);
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide) {
            TimeStorm.unleash(level, entity, this, spellLevel, SpellUtil.aimedSpot(level, entity, RANGE), life(spellLevel), getPower(spellLevel, entity));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
