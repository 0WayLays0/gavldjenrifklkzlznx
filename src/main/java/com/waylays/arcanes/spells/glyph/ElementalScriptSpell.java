package com.waylays.arcanes.spells.glyph;

import com.waylays.arcanes.entity.glyph.GlyphSign;
import com.waylays.arcanes.spells.SpellUtil;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Elemental Script: a line of glyphs is written along the ground towards where the caster looks, each in the next
 * element; each one erupts as soon as it is written. An enemy caught between two of them suffers both, and their
 * reaction.
 */
@AutoSpellConfig
public class ElementalScriptSpell extends GlyphSpell {
    public static final float SPACING = 2.2f;
    public static final float FIRST = 2.5f;
    public static final float ERUPTION = 1.9f;
    public static final float SIZE = 1.5f;
    /** Ticks between two glyphs, and ticks to write one. */
    public static final int STAGGER = 5;
    public static final int DRAW = 6;

    public ElementalScriptSpell() {
        super("elemental_script", SpellRarity.EPIC, 5, 15, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 10;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 70;
    }

    public static int glyphs(int spellLevel) {
        return 4 + spellLevel / 2;
    }

    public float damage(int spellLevel, LivingEntity caster) {
        return getPower(spellLevel, caster) * .9f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.glyph_count", glyphs(spellLevel)),
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.stringTruncation(damage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(ERUPTION, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel serverLevel) {
            int n = glyphs(spellLevel);
            float damage = damage(spellLevel, entity);
            Vec3 dir = direction(entity.getYRot());
            Vec3 base = entity.position();
            // The script starts with a random element, then follows the order of the elements
            int start = serverLevel.random.nextInt(GlyphElement.values().length);
            for (int i = 0; i < n; i++) {
                GlyphElement element = GlyphElement.byId((start + i) % GlyphElement.values().length);
                Vec3 at = SpellUtil.groundAt(level, base.add(dir.scale(FIRST + i * SPACING)), 4, 1).add(0, .05, 0);
                int delay = i * STAGGER;
                GlyphSign.write(level, element, at, entity.getYRot(), 0, SIZE, delay, DRAW, true);
                BossScheduler.after(serverLevel, delay, l -> GlyphFx.writing(l, element, at));
                BossScheduler.after(serverLevel, delay + DRAW, l -> erupt(l, entity, element, at, damage));
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private void erupt(ServerLevel level, LivingEntity caster, GlyphElement element, Vec3 at, float damage) {
        for (LivingEntity e : SpellUtil.enemiesAround(level, at.add(0, .8, 0), ERUPTION, caster, x -> true)) {
            GlyphReactions.strike(level, caster, caster, e, element, damage, this);
        }
        GlyphFx.impact(level, element, at.add(0, .4, 0), ERUPTION);
    }

    /** The horizontal direction of a yaw. */
    private static Vec3 direction(float yaw) {
        float r = yaw * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(r), 0, Mth.cos(r));
    }
}
