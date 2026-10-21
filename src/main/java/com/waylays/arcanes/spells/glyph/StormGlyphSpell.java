package com.waylays.arcanes.spells.glyph;

import com.waylays.arcanes.entity.glyph.GlyphArc;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Storm Glyph: the glyph of the storm cracks and lightning leaps from it to the enemy the caster looks at, then on
 * from enemy to enemy.
 */
@AutoSpellConfig
public class StormGlyphSpell extends GlyphSpell {
    public static final float RANGE = 18;
    /** How far the lightning leaps from one enemy to the next. */
    public static final float LEAP = 6;
    /** Half the width of the cone the first enemy is sought in, in degrees. */
    public static final float CONE = 14;

    public StormGlyphSpell() {
        super("storm_glyph", SpellRarity.UNCOMMON, 8, 5, CastType.INSTANT, SpellAnimations.ANIMATION_INSTANT_CAST);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 25;
    }

    /** How many enemies the lightning strikes. */
    public static int victims(int spellLevel) {
        return 3 + spellLevel / 3;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.glyph_chain", victims(spellLevel)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(RANGE, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        float damage = getPower(spellLevel, entity);
        int max = victims(spellLevel);
        writeThen(level, entity, GlyphElement.STORM, l -> strike(l, entity, damage, max));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private void strike(ServerLevel level, LivingEntity caster, float damage, int max) {
        Vec3 from = writingSpot(caster);
        List<Vec3> points = new ArrayList<>();
        points.add(from);
        LivingEntity first = aimed(level, caster);
        if (first == null) {
            // No one in sight: the lightning strikes where the caster looks
            Vec3 end = caster.getEyePosition().add(caster.getLookAngle().scale(RANGE));
            HitResult hit = level.clip(new ClipContext(caster.getEyePosition(), end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
            Vec3 to = hit.getLocation();
            GlyphArc.zap(level, GlyphElement.STORM, from, to);
            GlyphFx.impact(level, GlyphElement.STORM, to, .8f);
            return;
        }
        List<LivingEntity> chain = new ArrayList<>();
        LivingEntity current = first;
        while (current != null && chain.size() < max) {
            chain.add(current);
            LivingEntity last = current;
            current = SpellUtil.enemiesAround(level, last.position(), LEAP, caster, e -> !chain.contains(e) && last.hasLineOfSight(e)).stream()
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(last))).orElse(null);
        }
        for (LivingEntity e : chain) {
            points.add(e.position().add(0, e.getBbHeight() * .5, 0));
        }
        GlyphArc.zap(level, GlyphElement.STORM, points.toArray(Vec3[]::new));
        for (LivingEntity e : chain) {
            GlyphReactions.strike(level, caster, caster, e, GlyphElement.STORM, damage, this);
            GlyphFx.impact(level, GlyphElement.STORM, e.position().add(0, e.getBbHeight() * .5, 0), .6f);
        }
    }

    /** The enemy nearest to the middle of the caster's sight, within range and in view. */
    @Nullable
    private static LivingEntity aimed(ServerLevel level, LivingEntity caster) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        double cos = Math.cos(Math.toRadians(CONE));
        LivingEntity best = null;
        double bestDot = cos;
        for (LivingEntity e : SpellUtil.enemiesAround(level, eye, RANGE, caster, caster::hasLineOfSight)) {
            Vec3 d = e.getBoundingBox().getCenter().subtract(eye);
            double dot = d.normalize().dot(look);
            if (dot > bestDot) {
                bestDot = dot;
                best = e;
            }
        }
        return best;
    }
}
