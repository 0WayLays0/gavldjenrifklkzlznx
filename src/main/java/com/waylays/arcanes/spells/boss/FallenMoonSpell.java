package com.waylays.arcanes.spells.boss;

import com.waylays.arcanes.entity.boss.BossProp;
import com.waylays.arcanes.entity.boss.BossTelegraph;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Fallen Moon (ultimate): a moon shows high over the spot the caster aims at and comes down on it, slowly at first, then
 * faster and faster, burning as it falls; everything under it when it strikes is crushed, and the blast rolls on round
 * the crater. The longest warning of all: there is time to run, and every reason to.
 */
@AutoSpellConfig
public class FallenMoonSpell extends BossSpell {
    public static final double RANGE = 34;
    public static final float RADIUS = 14;
    public static final float BLAST_RADIUS = 22;
    public static final float MOON_RADIUS = 7;
    /** How much longer than a plain warning it takes to fall. */
    public static final int EXTRA_FALL = 50;
    /** The blast rolls out this long after the impact. */
    public static final int BLAST_DELAY = 6;
    public static final float HEIGHT = 56;

    public FallenMoonSpell() {
        super("fallen_moon", SpellRarity.LEGENDARY, 3, 180, BossAnimations.SKY_CALL);
        this.manaCostPerLevel = 60;
        this.baseSpellPower = 26;
        this.spellPowerPerLevel = 6;
        this.castTime = 30;
        this.baseManaCost = 400;
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float damage = getDamage(spellLevel, caster);
        return List.of(
                Component.translatable("ui.arcanes_oublies.crash_damage", Utils.stringTruncation(damage, 1)),
                Component.translatable("ui.arcanes_oublies.blast_damage", Utils.stringTruncation(damage * .4f, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)),
                Component.translatable("ui.arcanes_oublies.warning_time", Utils.stringTruncation((BossTelegraph.warningTicks() + EXTRA_FALL) / 20f, 1))
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.BEACON_POWER_SELECT);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.WITHER_SPAWN);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @org.jetbrains.annotations.Nullable MagicData playerMagicData) {
        SpellUtil.particles(level, ParticleTypes.END_ROD, entity.getX(), entity.getY() + 2.4, entity.getZ(), 2, .6, .3, .6, .02);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel) {
            Vec3 c = aimedGround(level, entity, RANGE);
            float damage = getDamage(spellLevel, entity);
            BossTelegraph moon = BossTelegraph.circle(level, entity, this, spellLevel, c, RADIUS)
                    .style(BossTelegraph.Style.EARTH).delayed(EXTRA_FALL).damage(damage).part("moon").reachUp(14).spawn();
            BossTelegraph.ring(level, entity, this, spellLevel, c, RADIUS, BLAST_RADIUS)
                    .style(BossTelegraph.Style.CRIMSON).delayed(EXTRA_FALL + BLAST_DELAY).damage(damage * .4f).part("blast").reachUp(6).spawn();
            float a = level.random.nextFloat() * Mth.TWO_PI;
            Vec3 sky = c.add(Mth.cos(a) * 14, HEIGHT, Mth.sin(a) * 14);
            BossProp.scene(level, BossProp.Kind.MOON, BossTelegraph.Style.EARTH, MOON_RADIUS, now(level), moon.getFireTick(), c, sky, c, RADIUS, 0);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 4f, .4f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onTelegraphFire(BossTelegraph t, ServerLevel level) {
        Vec3 c = t.position();
        if ("moon".equals(t.getPart())) {
            for (LivingEntity target : t.targetsInside()) {
                if (t.strike(target, t.getDamage())) {
                    target.igniteForSeconds(6);
                    knockAway(target, c, 1.2, 1.1);
                }
            }
            BossFx.impact(level, c, RADIUS * .5f, t.getStyle().zone, SoundEvents.GENERIC_EXPLODE.value());
            SpellUtil.particles(level, ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 3, c.z, 4, RADIUS * .3, 1, RADIUS * .3, 0);
            SpellUtil.particles(level, ParticleTypes.LAVA, c.x, c.y + 1, c.z, 40, RADIUS * .4, .5, RADIUS * .4, 0);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 5f, .4f);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 4f, .5f);
            SpellUtil.cameraShake(level, c, 60, BLAST_RADIUS * 2.5f);
        } else {
            for (LivingEntity target : t.targetsInside()) {
                if (t.strike(target, t.getDamage())) {
                    knockAway(target, c, 1.4, .5);
                }
            }
            BossFx.shockwave(level, c, BLAST_RADIUS * 2.2f, t.getStyle().rune);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3f, .4f);
        }
    }
}
