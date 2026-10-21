package com.waylays.arcanes.spells.rune;

import com.waylays.arcanes.entity.rune.Runestone;
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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Mending Runestone: a standing stone carved with sea-green runes rises where the caster points. Every two seconds its
 * runes pulse and heal the caster's allies around it. One stone per caster: a new one sends the old one back down.
 */
@AutoSpellConfig
public class MendingRunestoneSpell extends RuneSpell {
    public static final double RANGE = 12;

    public MendingRunestoneSpell() {
        super("mending_runestone", SpellRarity.RARE, 5, 30, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 2;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 55;
    }

    public float heal(int spellLevel, LivingEntity caster) {
        return getPower(spellLevel, caster) * .75f;
    }

    public static int duration(int spellLevel) {
        return Runestone.MENDING_RISE + 160 + 20 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.heal_per_pulse", Utils.stringTruncation(heal(spellLevel, caster), 1),
                        Utils.timeFromTicks(Runestone.PULSE_TICKS, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(Runestone.MENDING_RADIUS, 1)),
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(duration(spellLevel), 1)));
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.AMETHYST_BLOCK_CHIME);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 at = SpellUtil.aimedGround(level, entity, RANGE);
        if (at.distanceToSqr(entity.position()) < 2.5 * 2.5) {
            at = SpellUtil.groundAt(level, entity.position().add(direction(entity.getYRot()).scale(2.5)), 3, 1);
        }
        Runestone.mending(level, entity, at, duration(spellLevel), heal(spellLevel, entity));
        RuneFx.stone(level, at.add(0, .2, 0), Blocks.DEEPSLATE.defaultBlockState(), 24, .5);
        RuneFx.sound(level, at, SoundEvents.DEEPSLATE_PLACE, 1.5f, .5f);
        RuneFx.sound(level, at, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f, 1.2f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
