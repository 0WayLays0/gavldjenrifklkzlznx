package com.waylays.arcanes.spells.glyph;

import com.waylays.arcanes.entity.glyph.GlyphArc;
import com.waylays.arcanes.entity.glyph.GlyphMarkVisual;
import com.waylays.arcanes.entity.glyph.GlyphZone;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The heart of Glyph Magic: a glyph striking an enemy inscribes its element on it. A glyph of another element striking
 * an inscribed enemy wipes the mark and sets off the reaction of the two elements (see {@link GlyphReaction}).
 */
public final class GlyphReactions {
    /** How long a mark stays on its victim. */
    public static final int MARK_TICKS = 120;
    public static final int FREEZE_TICKS = 50;
    public static final float BURST_RADIUS = 3;
    public static final float ELECTROCUTE_RADIUS = 4;
    public static final float MAGNETIZE_RADIUS = 6;
    /** How long a lasting reaction's zone stays, and how much of the blow it deals at each pulse. */
    public static final int ZONE_TICKS = 80;
    public static final float ZONE_SHARE = .35f;

    private GlyphReactions() {}

    public static Holder<MobEffect> mark(GlyphElement element) {
        return ModEffects.GLYPH_MARKS.get(element.ordinal());
    }

    /** The element inscribed on a creature, if any. */
    @Nullable
    public static GlyphElement markOn(LivingEntity target) {
        for (GlyphElement element : GlyphElement.values()) {
            if (target.hasEffect(mark(element))) {
                return element;
            }
        }
        return null;
    }

    /**
     * A glyph of {@code element} strikes {@code target} for {@code damage}: the element touches it (burns, chills,
     * drenches...), then either inscribes its mark or reacts with the mark already there. Returns the reaction, if one
     * went off.
     */
    @Nullable
    public static GlyphReaction strike(ServerLevel level, @Nullable Entity caster, Entity direct, LivingEntity target, GlyphElement element,
                                       float damage, AbstractSpell spell) {
        if (!target.isAlive()) {
            return null;
        }
        GlyphElement inscribed = markOn(target);
        GlyphReaction reaction = inscribed != null && inscribed != element ? GlyphReaction.of(inscribed, element) : null;
        if (reaction != null) {
            target.removeEffect(mark(inscribed));
        }
        hurt(target, damage * multiplier(reaction), spell, direct, caster);
        touch(target, element);
        if (reaction != null) {
            react(level, caster, direct, target, reaction, damage, spell);
            GlyphMarkVisual.react(level, target, inscribed, element, reaction);
            announce(caster, reaction);
        } else if (target.isAlive()) {
            target.addEffect(new MobEffectInstance(mark(element), MARK_TICKS, 0, false, false, true));
            GlyphMarkVisual.show(level, target, element);
        }
        return reaction;
    }

    /** How hard the blow that sets a reaction off hits its victim. */
    public static float multiplier(@Nullable GlyphReaction reaction) {
        if (reaction == null) {
            return 1;
        }
        return switch (reaction) {
            case MELT -> 1.6f;
            case SHATTER -> 2f;
            case OVERLOAD -> 1.3f;
            case ELECTROCUTE -> 1.2f;
            case FREEZE -> .6f;
            default -> reaction.zone != null ? 1.2f : 1f;
        };
    }

    private static void hurt(LivingEntity target, float amount, AbstractSpell spell, Entity direct, @Nullable Entity caster) {
        // Glyphs and their reactions land in quick succession: none of them is lost to the victim's grace period
        target.invulnerableTime = 0;
        DamageSources.applyDamage(target, amount, SpellUtil.damageSource(spell, direct, caster));
    }

    /** What the element does by itself to what it touches. */
    private static void touch(LivingEntity target, GlyphElement element) {
        switch (element) {
            case FIRE -> {
                if (!target.isInWaterOrRain()) {
                    target.igniteForSeconds(3);
                }
            }
            case FROST -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
                target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze(), target.getTicksFrozen() + 60));
            }
            case TIDE -> target.clearFire();
            default -> {
            }
        }
    }

    private static void react(ServerLevel level, @Nullable Entity caster, Entity direct, LivingEntity target, GlyphReaction reaction,
                              float damage, AbstractSpell spell) {
        Vec3 at = target.position().add(0, target.getBbHeight() * .5, 0);
        switch (reaction) {
            case MELT -> {
                // Fire on ice (or ice on fire): a burst of scalding steam
                target.clearFire();
                SpellUtil.particles(level, ParticleTypes.CLOUD, at.x, at.y, at.z, 24, .5, .6, .5, .12);
                GlyphFx.impact(level, GlyphElement.FIRE, at, 1.2f);
                GlyphFx.sound(level, at, SoundEvents.FIRE_EXTINGUISH, 1f, .8f);
            }
            case OVERLOAD -> {
                // The spark meets the flame: an explosion throwing everyone around
                for (LivingEntity e : SpellUtil.enemiesAround(level, at, BURST_RADIUS, caster, x -> x != target)) {
                    hurt(e, damage, spell, direct, caster);
                    push(e, at, .9, .35);
                }
                push(target, at.add(0, -.5, 0), .6, .45);
                SpellUtil.particles(level, ParticleTypes.EXPLOSION, at.x, at.y, at.z, 3, .8, .5, .8, 0);
                GlyphFx.impact(level, GlyphElement.FIRE, at, BURST_RADIUS);
                GlyphFx.impact(level, GlyphElement.STORM, at, 1.5f);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2f, 1.3f);
            }
            case FREEZE -> {
                // Drenched then chilled: frozen solid in a block of ice
                target.addEffect(new MobEffectInstance(ModEffects.GLYPH_FROZEN, FREEZE_TICKS, 0, false, false, true));
                target.setTicksFrozen(target.getTicksRequiredToFreeze() + FREEZE_TICKS * 2);
                GlyphFx.impact(level, GlyphElement.FROST, at, 1.5f);
                GlyphFx.sound(level, at, SoundEvents.GLASS_PLACE, 1.2f, .7f);
            }
            case SHATTER -> {
                // Stone breaks the ice: shards everywhere
                target.removeEffect(ModEffects.GLYPH_FROZEN);
                GlyphFx.impact(level, GlyphElement.FROST, at, 2f);
                GlyphFx.impact(level, GlyphElement.STONE, at, 1.2f);
                GlyphFx.sound(level, at, SoundEvents.GLASS_BREAK, 1.4f, .6f);
            }
            case SUPERCONDUCT -> {
                // Cold lightning: everyone around is left weak
                for (LivingEntity e : SpellUtil.enemiesAround(level, at, BURST_RADIUS, caster, x -> true)) {
                    if (e != target) {
                        hurt(e, damage * .6f, spell, direct, caster);
                    }
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
                }
                SpellUtil.particles(level, ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 40, BURST_RADIUS * .5, .6, BURST_RADIUS * .5, .1);
                GlyphFx.impact(level, GlyphElement.STORM, at, BURST_RADIUS);
            }
            case ELECTROCUTE -> {
                // Lightning in water: it leaps to everyone nearby and holds them still
                Vec3[] arc = new Vec3[]{at, null};
                for (LivingEntity e : SpellUtil.enemiesAround(level, at, ELECTROCUTE_RADIUS, caster, x -> true)) {
                    if (e != target) {
                        hurt(e, damage * .6f, spell, direct, caster);
                        arc[1] = e.position().add(0, e.getBbHeight() * .5, 0);
                        GlyphArc.zap(level, GlyphElement.STORM, arc);
                    }
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 3));
                }
                GlyphFx.impact(level, GlyphElement.STORM, at, 2f);
                GlyphFx.impact(level, GlyphElement.TIDE, at, 1f);
            }
            case MAGNETIZE -> {
                // Charged stone: it drags everything around onto its victim
                for (LivingEntity e : SpellUtil.enemiesAround(level, at, MAGNETIZE_RADIUS, caster, x -> x != target)) {
                    Vec3 d = at.subtract(e.position());
                    double dist = d.length();
                    if (dist > .5) {
                        double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
                        e.setDeltaMovement(d.scale(Math.min(1.4, dist * .3) / dist * resist).add(0, .2 * resist, 0));
                        e.hurtMarked = true;
                    }
                    hurt(e, damage * .4f, spell, direct, caster);
                    GlyphArc.zap(level, GlyphElement.STORM, at, e.position().add(0, e.getBbHeight() * .5, 0));
                }
                GlyphFx.impact(level, GlyphElement.STONE, at, 1.5f);
                GlyphFx.sound(level, at, SoundEvents.BEACON_POWER_SELECT, 1f, .6f);
            }
            default -> {
                // A lasting reaction: its zone opens where the victim stands
                GlyphZone.open(level, caster, spell, reaction, SpellUtil.groundAt(level, target.position(), 3, 1), ZONE_TICKS, damage * ZONE_SHARE);
                GlyphFx.impact(level, reaction.a, at, 1.5f);
                GlyphFx.impact(level, reaction.b, at, 1.5f);
            }
        }
    }

    private static void push(LivingEntity e, Vec3 from, double strength, double lift) {
        Vec3 d = e.position().subtract(from).multiply(1, 0, 1);
        Vec3 away = d.lengthSqr() < 1.0E-4 ? Vec3.ZERO : d.normalize();
        double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
        e.setDeltaMovement(e.getDeltaMovement().add(away.scale(strength * resist)).add(0, lift * resist, 0));
        e.hurtMarked = true;
    }

    /** The name of the reaction on the casting player's screen, in the colours of its two elements. */
    private static void announce(@Nullable Entity caster, GlyphReaction reaction) {
        if (caster instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("ui.arcanes_oublies.reaction." + reaction.id)
                    .withStyle(Style.EMPTY.withColor(reaction.color()).withBold(true)), true);
        }
    }
}
