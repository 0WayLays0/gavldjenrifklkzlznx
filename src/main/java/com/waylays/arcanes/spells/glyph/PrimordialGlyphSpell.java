package com.waylays.arcanes.spells.glyph;

import com.waylays.arcanes.entity.glyph.GlyphStorm;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * The Primordial Glyph (ultimate): a great glyph written across the sky over the caster, its six elements on a ring;
 * then, round the ring, each element hurls its bolt on the enemies below, and their reactions go off on every side.
 */
@AutoSpellConfig
public class PrimordialGlyphSpell extends GlyphSpell {
    public PrimordialGlyphSpell() {
        super("primordial_glyph", SpellRarity.LEGENDARY, 3, 120, CastType.LONG, null);
        this.manaCostPerLevel = 50;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 2;
        this.castTime = 30;
        this.baseManaCost = 250;
    }

    public static int duration(int spellLevel) {
        return GlyphStorm.WRITE_TICKS + 100 + 40 * spellLevel;
    }

    public float damage(int spellLevel, LivingEntity caster) {
        return getPower(spellLevel, caster) * .7f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.stringTruncation(damage(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.glyph_rain", Utils.timeFromTicks(GlyphStorm.STRIKE_TICKS, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(GlyphStorm.RADIUS, 1)),
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(duration(spellLevel), 1)));
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.BEACON_AMBIENT);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.BEACON_ACTIVATE);
    }

    /** While it is cast, the six elements' ink rises around the caster in a spiral. */
    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        float a = entity.tickCount * .45f;
        for (GlyphElement element : GlyphElement.values()) {
            float b = a + element.ordinal() * Mth.TWO_PI / 6;
            SpellUtil.particles(level, element.dust, entity.getX() + Mth.cos(b) * 1.4, entity.getY() + (entity.tickCount % 20) * .14, entity.getZ() + Mth.sin(b) * 1.4, 1, 0, 0, 0, 0);
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 at = SpellUtil.groundAt(level, entity.position(), 3, 1);
        GlyphStorm.write(level, entity, this, at, duration(spellLevel), damage(spellLevel, entity));
        GlyphFx.sound(level, at, SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, .5f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
