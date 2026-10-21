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

/** Tide Glyph: a wall of water pours out of the glyph and rolls ahead in an arc, throwing enemies back and putting out fires. */
@AutoSpellConfig
public class TideGlyphSpell extends GlyphSpell {
    public static final float HALF_ANGLE = 50;
    public static final int DURATION = 14;

    public TideGlyphSpell() {
        super("tide_glyph", SpellRarity.UNCOMMON, 8, 7, CastType.INSTANT, SpellAnimations.ANIMATION_INSTANT_CAST);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 30;
    }

    public static float range(int spellLevel) {
        return 8 + spellLevel * .5f;
    }

    public float damage(int spellLevel, LivingEntity caster) {
        return getPower(spellLevel, caster) * .8f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(damage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(range(spellLevel), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        float damage = damage(spellLevel, entity);
        float range = range(spellLevel);
        writeThen(level, entity, GlyphElement.TIDE, l -> {
            Vec3 at = SpellUtil.groundAt(l, entity.position(), 2, 1);
            GlyphWave.roll(l, entity, this, GlyphElement.TIDE, at, entity.getYRot(), HALF_ANGLE, range, DURATION, damage);
            GlyphFx.impact(l, GlyphElement.TIDE, writingSpot(entity), 1);
            GlyphFx.sound(l, at, SoundEvents.GENERIC_SPLASH, 1.5f, .7f);
        });
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
