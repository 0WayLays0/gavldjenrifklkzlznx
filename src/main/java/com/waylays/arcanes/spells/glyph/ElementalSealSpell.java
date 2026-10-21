package com.waylays.arcanes.spells.glyph;

import com.waylays.arcanes.entity.glyph.GlyphSeal;
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

import java.util.List;
import java.util.Optional;

/**
 * Elemental Seal: the six element glyphs, written in a ring around the caster, turn with them; one after another
 * they loose their bolt at the nearest enemy.
 */
@AutoSpellConfig
public class ElementalSealSpell extends GlyphSpell {
    public ElementalSealSpell() {
        super("elemental_seal", SpellRarity.RARE, 6, 30, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 60;
    }

    public static int duration(int spellLevel) {
        return 200 + 40 * spellLevel;
    }

    /** Ticks between two bolts. */
    public static int beat(int spellLevel) {
        return spellLevel >= 5 ? 14 : 20;
    }

    public float damage(int spellLevel, LivingEntity caster) {
        return getPower(spellLevel, caster) * .7f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(damage(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.glyph_beat", Utils.timeFromTicks(beat(spellLevel), 1)),
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(duration(spellLevel), 1)));
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.ENCHANTMENT_TABLE_USE);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        GlyphSeal.raise(level, entity, this, duration(spellLevel), damage(spellLevel, entity), beat(spellLevel));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
