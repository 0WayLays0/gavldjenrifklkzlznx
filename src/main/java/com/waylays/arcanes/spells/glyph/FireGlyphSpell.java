package com.waylays.arcanes.spells.glyph;

import com.waylays.arcanes.entity.glyph.GlyphBolt;
import com.waylays.arcanes.spells.boss.BossScheduler;
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

/** Fire Glyph: the flame glyph, written in the air, looses its comets one after another where the caster looks. */
@AutoSpellConfig
public class FireGlyphSpell extends GlyphSpell {
    public static final float SPEED = 1.3f;
    /** Ticks between two comets, and degrees between their paths. */
    public static final int STAGGER = 2;
    public static final float SPREAD = 4;

    public FireGlyphSpell() {
        super("fire_glyph", SpellRarity.COMMON, 10, 3, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 15;
    }

    public static int comets(int spellLevel) {
        return 3 + (spellLevel - 1) / 3;
    }

    public float damage(int spellLevel, LivingEntity caster) {
        return getPower(spellLevel, caster) * .5f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.projectile_count", comets(spellLevel)),
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(damage(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.glyph_burn"));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        int n = comets(spellLevel);
        float damage = damage(spellLevel, entity);
        Vec3 at = writeThen(level, entity, GlyphElement.FIRE, l -> {
            for (int i = 0; i < n; i++) {
                float side = (i % 2 == 0 ? 1 : -1) * ((i + 1) / 2) * SPREAD;
                BossScheduler.after(l, i * STAGGER, ll -> {
                    if (!entity.isAlive()) {
                        return;
                    }
                    Vec3 from = writingSpot(entity);
                    Vec3 dir = Vec3.directionFromRotation(entity.getXRot(), entity.getYRot() + side);
                    GlyphBolt.shoot(ll, entity, this, GlyphElement.FIRE, from, dir, SPEED, damage);
                    GlyphFx.burst(ll, GlyphElement.FIRE, from, 4, .1);
                });
            }
        });
        GlyphFx.burst(level, GlyphElement.FIRE, at, 6, .3);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
