package com.waylays.arcanes.spells.water;

import com.waylays.arcanes.entity.water.TidalWave;
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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/**
 * Tidal Wave: a wall of sea rises in front of the caster and rolls forwards, sweeping up every enemy it meets and
 * carrying them along before it breaks and crashes down on them (see TidalWave).
 */
@AutoSpellConfig
public class TidalWaveSpell extends WaterSpell {
    public TidalWaveSpell() {
        super("tidal_wave", SpellRarity.EPIC, 5, 25, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 12;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 80;
    }

    /** Ticks it rolls before it breaks. */
    public static int run(int spellLevel) {
        return 40 + 4 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(power * .5f, 1)),
                Component.translatable("ui.arcanes_oublies.crash_damage", Utils.stringTruncation(power, 1)),
                Component.translatable("ui.arcanes_oublies.wave_carries", Utils.stringTruncation(run(spellLevel) * TidalWave.SPEED, 1)),
                Component.translatable("ui.arcanes_oublies.wave_width", Utils.stringTruncation(TidalWave.WIDTH, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        float yaw = entity.getYRot();
        Vec3 ahead = entity.position().add(-Mth.sin(yaw * Mth.DEG_TO_RAD) * 5, 0, Mth.cos(yaw * Mth.DEG_TO_RAD) * 5);
        TidalWave.raise(level, entity, this, SpellUtil.groundAt(level, ahead, 4, 2), yaw, run(spellLevel), getPower(spellLevel, entity));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
