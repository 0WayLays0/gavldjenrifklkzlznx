package com.waylays.arcanes.spells.rune;

import com.waylays.arcanes.entity.rune.EdgeGlyphs;
import com.waylays.arcanes.registry.ModEffects;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Edge Rune: a rune carved into the weapons of the caster and of the allies around them. For a while, every blow they
 * strike in close combat deals the rune's damage on top (see {@link com.waylays.arcanes.effect.RuneEdgeEffect}).
 */
@AutoSpellConfig
public class EdgeRuneSpell extends RuneSpell {
    public static final double SHARE_RADIUS = 6;

    public EdgeRuneSpell() {
        super("edge_rune", SpellRarity.UNCOMMON, 5, 30, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 2;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 35;
    }

    public static int duration(int spellLevel) {
        return 400 + 80 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.edge_bonus", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(duration(spellLevel), 1)),
                Component.translatable("ui.arcanes_oublies.shared_radius", Utils.stringTruncation(SHARE_RADIUS, 1)));
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.SMITHING_TABLE_USE);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        int duration = duration(spellLevel);
        for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(SHARE_RADIUS),
                e -> e.distanceToSqr(entity) <= SHARE_RADIUS * SHARE_RADIUS && RuneFx.helps(entity, e))) {
            ally.addEffect(new MobEffectInstance(ModEffects.RUNE_EDGE, duration, spellLevel - 1, false, true, true), entity);
            EdgeGlyphs.attach(level, ally);
            Vec3 hand = ally.position().add(0, ally.getBbHeight() * .5, 0);
            RuneFx.glyphs(level, hand, 10, .4);
        }
        RuneFx.sound(level, entity.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f, 1.5f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
