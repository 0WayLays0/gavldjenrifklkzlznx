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
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Glyph Resonance: every glyph the caster has inscribed on the enemies around answers at once. Each mark bursts on
 * its bearer and its element splashes onto the enemies beside it, which may set off their own marks' reactions.
 */
@AutoSpellConfig
public class GlyphResonanceSpell extends GlyphSpell {
    public static final float RADIUS = 16;
    public static final float SPLASH = 2.5f;

    public GlyphResonanceSpell() {
        super("glyph_resonance", SpellRarity.EPIC, 5, 12, CastType.INSTANT, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 7;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 60;
    }

    public float damage(int spellLevel, LivingEntity caster) {
        return getPower(spellLevel, caster) * 1.2f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.glyph_per_mark", Utils.stringTruncation(damage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)));
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.BELL_RESONATE);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel serverLevel) {
            resonate(serverLevel, entity, damage(spellLevel, entity));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** Sets off every mark within reach. Returns how many went off. */
    public int resonate(ServerLevel level, LivingEntity caster, float damage) {
        Vec3 chest = caster.position().add(0, caster.getBbHeight() * .6, 0);
        List<LivingEntity> marked = SpellUtil.enemiesAround(level, chest, RADIUS, caster, e -> GlyphReactions.markOn(e) != null);
        // All the marks are read before any goes off, so a burst cannot wipe a mark before its turn
        List<GlyphElement> elements = marked.stream().map(GlyphReactions::markOn).toList();
        for (int k = 0; k < marked.size(); k++) {
            LivingEntity e = marked.get(k);
            GlyphElement element = elements.get(k);
            e.removeEffect(GlyphReactions.mark(element));
            Vec3 at = e.position().add(0, e.getBbHeight() * .5, 0);
            GlyphArc.zap(level, element, chest, at);
            e.invulnerableTime = 0;
            DamageSources.applyDamage(e, damage, SpellUtil.damageSource(this, caster, caster));
            GlyphFx.impact(level, element, at, SPLASH);
            for (LivingEntity other : SpellUtil.enemiesAround(level, at, SPLASH, caster, x -> x != e)) {
                GlyphReactions.strike(level, caster, caster, other, element, damage * .4f, this);
            }
        }
        GlyphFx.burst(level, GlyphElement.STORM, chest, 12, .6);
        return marked.size();
    }
}
