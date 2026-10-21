package com.waylays.arcanes.spells.glyph;

import com.waylays.arcanes.entity.glyph.GlyphBolt;
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

/** Stone Glyph: the glyph hardens into a boulder that is hurled where the caster looks and bursts on landing. */
@AutoSpellConfig
public class StoneGlyphSpell extends GlyphSpell {
    public static final float SPEED = 1.25f;
    public static final double GRAVITY = .035;
    public static final float SPLASH = 2.5f;

    public StoneGlyphSpell() {
        super("stone_glyph", SpellRarity.RARE, 6, 8, CastType.INSTANT, SpellAnimations.ANIMATION_INSTANT_CAST);
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 40;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(SPLASH, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        float damage = getPower(spellLevel, entity);
        writeThen(level, entity, GlyphElement.STONE, l -> {
            Vec3 from = writingSpot(entity);
            Vec3 dir = entity.getLookAngle().add(0, .08, 0);
            GlyphBolt.shoot(l, entity, this, GlyphElement.STONE, from, dir, SPEED, damage)
                    .withGravity(GRAVITY).withSplash(SPLASH).withSize(1.8f);
            GlyphFx.burst(l, GlyphElement.STONE, from, 8, .3);
        });
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
