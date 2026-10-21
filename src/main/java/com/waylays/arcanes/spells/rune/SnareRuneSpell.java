package com.waylays.arcanes.spells.rune;

import com.waylays.arcanes.entity.rune.RuneSigil;
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
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Snare Rune: a small rune carved where the caster points, barely glowing. Once it is armed, the first enemy to step on
 * it makes it burst out in stone spikes: everyone around is hurt, slowed and thrown up. Three at most at once.
 */
@AutoSpellConfig
public class SnareRuneSpell extends RuneSpell {
    public static final double RANGE = 16;
    /** A snare left untouched crumbles after a minute. */
    public static final int LIFE = 1200;

    public SnareRuneSpell() {
        super("snare_rune", SpellRarity.UNCOMMON, 5, 8, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 25;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RuneSigil.BURST_RADIUS, 1)),
                Component.translatable("ui.arcanes_oublies.max_snares", RuneSigil.MAX_SNARES));
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.DEEPSLATE_PLACE);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 at = SpellUtil.aimedGround(level, entity, RANGE);
        RuneSigil.carve(level, entity, RuneSigil.Kind.SNARE, at, entity.getYRot(), RuneSigil.SNARE_RADIUS, 0, LIFE, spellLevel, getPower(spellLevel, entity));
        RuneFx.glyphs(level, at.add(0, .3, 0), 8, .4);
        RuneFx.sound(level, at, SoundEvents.AMETHYST_BLOCK_PLACE, 1f, .7f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
