package com.waylays.arcanes.spells.wind;

import com.waylays.arcanes.entity.wind.WindWard;
import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
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
 * Tailwind: the wind takes the caster's side: they and their allies close by run faster for a while, wrapped in rings
 * of wind that turn aside the shots coming at them (see WindWard).
 */
@AutoSpellConfig
public class TailwindSpell extends WindSpell {
    public static final float RADIUS = 8;

    public TailwindSpell() {
        super("tailwind", SpellRarity.UNCOMMON, 8, 30, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 40;
    }

    public static int seconds(int spellLevel) {
        return 10 + 2 * spellLevel;
    }

    public static int speed(int spellLevel) {
        return spellLevel >= 5 ? 1 : 0;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.tailwind_speed", speed(spellLevel) + 1),
                Component.translatable("ui.arcanes_oublies.turns_shots"),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(seconds(spellLevel) * 20, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide) {
            int ticks = seconds(spellLevel) * 20;
            for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(RADIUS),
                    x -> x.isAlive() && (x == entity || x instanceof Player && SpellUtil.isAlly(entity, x) || SpellUtil.isAlly(entity, x)))) {
                ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, speed(spellLevel), false, true, true));
                WindWard.on(level, ally, entity, ticks);
                WindFx.gust(level, ally.position().add(0, 1, 0), 4, .5);
            }
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BREEZE_WHIRL, SoundSource.PLAYERS, 1.4f, 1.3f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
