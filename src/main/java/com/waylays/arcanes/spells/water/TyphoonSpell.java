package com.waylays.arcanes.spells.water;

import com.waylays.arcanes.entity.water.Typhoon;
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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.Optional;

/**
 * Typhoon: a waterspout as tall as a tower rises in front of the caster and wanders off the way they look, sucking
 * enemies in, spinning them round and battering them, flinging them out when it breaks up (see Typhoon).
 */
@AutoSpellConfig
public class TyphoonSpell extends WaterSpell {
    public static final double AHEAD = 5;

    public TyphoonSpell() {
        super("typhoon", SpellRarity.EPIC, 5, 40, CastType.LONG, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 15;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 1;
        this.castTime = 20;
        this.baseManaCost = 90;
    }

    public static int life(int spellLevel) {
        return 160 + 20 * spellLevel;
    }

    public float batter(int spellLevel, LivingEntity caster) {
        return getPower(spellLevel, caster) * .35f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.cut_damage", Utils.stringTruncation(batter(spellLevel, caster), 1), Utils.timeFromTicks(10, 1)),
                Component.translatable("ui.arcanes_oublies.pull_radius", Utils.stringTruncation(Typhoon.PULL, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(life(spellLevel), 1)));
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.WEATHER_RAIN_ABOVE);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.LIGHTNING_BOLT_THUNDER);
    }

    /** While the caster calls it, the wind and the spray start turning round them. */
    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @org.jetbrains.annotations.Nullable MagicData playerMagicData) {
        if (entity.tickCount % 2 == 0) {
            float a = entity.tickCount * .6f;
            WaterFx.spray(level, entity.position().add(Mth.cos(a) * 1.6, .3, Mth.sin(a) * 1.6), 2, .1);
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        float yaw = entity.getYRot();
        Vec3 ahead = entity.position().add(-Mth.sin(yaw * Mth.DEG_TO_RAD) * AHEAD, 0, Mth.cos(yaw * Mth.DEG_TO_RAD) * AHEAD);
        Typhoon.raise(level, entity, this, SpellUtil.groundAt(level, ahead, 4, 2), yaw, life(spellLevel), batter(spellLevel, entity));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
