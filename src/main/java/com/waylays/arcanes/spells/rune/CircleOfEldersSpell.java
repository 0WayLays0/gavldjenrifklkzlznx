package com.waylays.arcanes.spells.rune;

import com.waylays.arcanes.entity.rune.ElderCircle;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.particles.ParticleTypes;
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
 * Circle of the Elders (ultimate): eight great runestones rise in a ring around the caster. Every second, the caster's
 * allies inside are healed and shielded while runic lightning leaps from the stones to the enemies inside.
 */
@AutoSpellConfig
public class CircleOfEldersSpell extends RuneSpell {
    public static final float RADIUS = 8;

    public CircleOfEldersSpell() {
        super("circle_of_elders", SpellRarity.LEGENDARY, 3, 120, CastType.LONG, null);
        this.manaCostPerLevel = 50;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 2;
        this.castTime = 30;
        this.baseManaCost = 250;
    }

    public static int duration(int spellLevel) {
        return ElderCircle.RISE_TICKS + 160 + 40 * spellLevel;
    }

    public float heal(int spellLevel, LivingEntity caster) {
        return getPower(spellLevel, caster) * .35f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.arc_damage", Utils.stringTruncation(getPower(spellLevel, caster), 1), ElderCircle.MAX_ARCS),
                Component.translatable("ui.arcanes_oublies.heal_per_second", Utils.stringTruncation(heal(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)),
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

    /** While it is cast, runes rise around the caster, spiralling up. */
    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        float a = entity.tickCount * .5f;
        for (int i = 0; i < 4; i++) {
            float b = a + i * Mth.HALF_PI;
            SpellUtil.particles(level, RuneFx.GLOW_DUST, entity.getX() + Mth.cos(b) * 1.6, entity.getY() + (entity.tickCount % 20) * .12, entity.getZ() + Mth.sin(b) * 1.6, 1, 0, 0, 0, 0);
        }
        SpellUtil.particles(level, ParticleTypes.ENCHANT, entity.getX(), entity.getY() + 1, entity.getZ(), 4, 1.5, .5, 1.5, .5);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 at = feet(entity);
        ElderCircle.raise(level, entity, at, RADIUS, duration(spellLevel), getPower(spellLevel, entity), heal(spellLevel, entity), spellLevel);
        RuneFx.ring(level, at, RADIUS, 48, RuneFx.GLOW_DUST);
        RuneFx.sound(level, at, SoundEvents.DEEPSLATE_BREAK, 2f, .4f);
        SpellUtil.cameraShake(level, at, 30, RADIUS + 12);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
