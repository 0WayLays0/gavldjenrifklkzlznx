package com.waylays.arcanes.spells.glyph;

import com.waylays.arcanes.entity.glyph.GlyphWave;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Gale Glyph: a gust bursts out of the glyph in a narrow arc and throws enemies into the air. */
@AutoSpellConfig
public class GaleGlyphSpell extends GlyphSpell {
    public static final float HALF_ANGLE = 30;
    public static final float RANGE = 11;
    public static final int DURATION = 9;

    public GaleGlyphSpell() {
        super("gale_glyph", SpellRarity.UNCOMMON, 8, 6, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 25;
    }

    public float damage(int spellLevel, LivingEntity caster) {
        return getPower(spellLevel, caster) * .6f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(damage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(RANGE, 1)),
                Component.translatable("ui.arcanes_oublies.glyph_launch"));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        float damage = damage(spellLevel, entity);
        writeThen(level, entity, GlyphElement.GALE, l -> {
            Vec3 at = SpellUtil.groundAt(l, entity.position(), 2, 1);
            GlyphWave.roll(l, entity, this, GlyphElement.GALE, at, entity.getYRot(), HALF_ANGLE, RANGE, DURATION, damage);
            GlyphFx.impact(l, GlyphElement.GALE, writingSpot(entity), 1);
            GlyphFx.sound(l, at, SoundEvents.ENDER_DRAGON_FLAP, 1.5f, 1.3f);
        });
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
