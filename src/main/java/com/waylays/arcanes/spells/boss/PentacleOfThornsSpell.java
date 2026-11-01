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
 * The Pentacle of Thorns: a five-pointed star is marked on the ground round the spot the caster aims at, its strokes
 * one after another; giant blood-red thorns burst out along each stroke as its warning runs out, and last of all its
 * heart erupts. The spaces between the strokes are safe, the heart is not.
 */
@AutoSpellConfig
public class PentacleOfThornsSpell extends BossSpell {
    public static final double RANGE = 26;
    public static final float RADIUS = 9;
    public static final float WIDTH = 2.6f;
    public static final float HEART = 3.2f;
    /** Each stroke bursts out this many ticks after the one before. */
    public static final int STROKE = 6;
    /** How long before it bursts out a stroke's split starts to glow. */
    public static final int GLOW = 20;

    public PentacleOfThornsSpell() {
        super("pentacle_of_thorns", SpellRarity.EPIC, 3, 30, BossAnimations.GROUND_SLAM);
        this.manaCostPerLevel = 30;
        this.baseSpellPower = 14;
        this.spellPowerPerLevel = 4;
        this.castTime = 0;
        this.baseManaCost = 100;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float damage = getDamage(spellLevel, caster);
        return List.of(
                Component.translatable("ui.arcanes_oublies.thorn_damage", Utils.stringTruncation(damage, 1)),
                Component.translatable("ui.arcanes_oublies.heart_damage", Utils.stringTruncation(damage * 1.3f, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)),
                warningInfo()
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.EVOKER_PREPARE_ATTACK);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel) {
            Vec3 c = SpellUtil.aimedSpot(level, entity, RANGE);
            float damage = getDamage(spellLevel, entity);
            // The star's points, the first one toward the caster
            Vec3 toward = entity.position().subtract(c).multiply(1, 0, 1);
            float base = toward.lengthSqr() < 1.0E-4 ? 0 : (float) Mth.atan2(toward.z, toward.x);
            Vec3[] points = new Vec3[5];
            for (int j = 0; j < 5; j++) {
                float a = base + j * Mth.TWO_PI / 5;
                points[j] = c.add(Mth.cos(a) * RADIUS, 0, Mth.sin(a) * RADIUS);
            }
            int last = 0;
            for (int j = 0; j < 5; j++) {
                Vec3 a = SpellUtil.groundAt(level, points[(j * 2) % 5], 5, 1);
                Vec3 b = points[(j * 2 + 2) % 5];
                Vec3 d = b.subtract(a).multiply(1, 0, 1);
                float length = (float) d.length();
                BossTelegraph t = BossTelegraph.line(level, entity, this, spellLevel, a, yawOf(d.x, d.z), length, WIDTH)
                        .style(BossTelegraph.Style.CRIMSON).delayed(j * STROKE).damage(damage).part("stroke").spawn();
                BossProp.scene(level, BossProp.Kind.THORNS, BossTelegraph.Style.CRIMSON, WIDTH, t.getFireTick() - GLOW, t.getFireTick(), a, a,
                        a.add(d.x, 0, d.z), 0, 0);
                last = j * STROKE;
            }
            BossTelegraph.circle(level, entity, this, spellLevel, SpellUtil.groundAt(level, c, 5, 1), HEART)
                    .style(BossTelegraph.Style.CRIMSON).delayed(last + STROKE + 4).damage(damage * 1.3f).part("heart").spawn();
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onTelegraphFire(BossTelegraph t, ServerLevel level) {
        for (LivingEntity target : t.targetsInside()) {
            if (t.strike(target, t.getDamage())) {
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 50, 0));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1));
                target.setDeltaMovement(target.getDeltaMovement().add(0, .55, 0));
                target.hurtMarked = true;
            }
        }
        Vec3 c = t.center();
        if ("heart".equals(t.getPart())) {
            // The heart: thorns bursting out of it every way
            int now = now(level);
            for (int k = 0; k < 6; k++) {
                float a = k * Mth.TWO_PI / 6;
                Vec3 to = c.add(Mth.cos(a) * HEART * 1.2, 0, Mth.sin(a) * HEART * 1.2);
                BossProp.scene(level, BossProp.Kind.THORNS, BossTelegraph.Style.CRIMSON, 1.6f, now - GLOW, now, c, c, to, 0, 0);
            }
            BossFx.impact(level, c, HEART, t.getStyle().zone, SoundEvents.EVOKER_FANGS_ATTACK);
        } else {
            Vec3 f = t.forward();
            SpellUtil.particles(level, BossFx.dust(t.getStyle().zone, 2f), c.x, c.y + .8, c.z, 40, f.x * t.getSize() * .4 + .6, .5, f.z * t.getSize() * .4 + .6, .05);
            SpellUtil.particles(level, ParticleTypes.CRIMSON_SPORE, c.x, c.y + .5, c.z, 30, f.x * t.getSize() * .4 + .6, .5, f.z * t.getSize() * .4 + .6, .02);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.HOSTILE, 2.5f, .7f);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.POINTED_DRIPSTONE_LAND, SoundSource.HOSTILE, 2.5f, .5f);
            SpellUtil.cameraShake(level, c, 8, 10);
        }
    }
}
