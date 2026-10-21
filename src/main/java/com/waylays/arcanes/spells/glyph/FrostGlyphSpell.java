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

/** Frost Glyph: the snowflake glyph shatters into a fan of ice needles; each one goes through an enemy and chills it. */
@AutoSpellConfig
public class FrostGlyphSpell extends GlyphSpell {
    public static final float SPEED = 1.8f;
    /** The whole fan's width in degrees. */
    public static final float FAN = 32;
    public static final int PIERCE = 1;

    public FrostGlyphSpell() {
        super("frost_glyph", SpellRarity.COMMON, 10, 4, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 18;
    }

    public static int needles(int spellLevel) {
        return 5 + (spellLevel - 1) / 3;
    }

    public float damage(int spellLevel, LivingEntity caster) {
        return getPower(spellLevel, caster) * .45f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.projectile_count", needles(spellLevel)),
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(damage(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.pierce", PIERCE));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        int n = needles(spellLevel);
        float damage = damage(spellLevel, entity);
        writeThen(level, entity, GlyphElement.FROST, l -> {
            Vec3 from = writingSpot(entity);
            for (int i = 0; i < n; i++) {
                float yaw = entity.getYRot() + (n == 1 ? 0 : (i / (n - 1f) - .5f) * FAN);
                GlyphBolt.shoot(l, entity, this, GlyphElement.FROST, from, Vec3.directionFromRotation(entity.getXRot(), yaw), SPEED, damage)
                        .withPierce(PIERCE).withSize(.8f);
            }
            GlyphFx.burst(l, GlyphElement.FROST, from, 10, .3);
        });
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
