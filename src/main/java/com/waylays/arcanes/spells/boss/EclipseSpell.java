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
 * Eclipse (ultimate): a sun kindles high over the caster and a black disc slides across it; in the shadow of the
 * eclipse the arena round the caster is struck in three bands, from the middle outward, each a ring of black fire that
 * leaves its victims in darkness and weakened. Each band lands a little after the one inside it: step into a band that
 * has already burned.
 */
@AutoSpellConfig
public class EclipseSpell extends BossSpell {
    public static final float RADIUS = 15.5f;
    public static final int BANDS = 3;
    /** Each band lands this many ticks after the one inside it. */
    public static final int STEP = 14;
    public static final float SKY_HEIGHT = 22;

    public EclipseSpell() {
        super("eclipse", SpellRarity.LEGENDARY, 3, 120, BossAnimations.SKY_CALL);
        this.manaCostPerLevel = 50;
        this.baseSpellPower = 16;
        this.spellPowerPerLevel = 4;
        this.castTime = 20;
        this.baseManaCost = 250;
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.bands", BANDS, Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)),
                warningInfo()
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.BEACON_DEACTIVATE);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.WARDEN_SONIC_CHARGE);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @org.jetbrains.annotations.Nullable MagicData playerMagicData) {
        float a = entity.tickCount * .3f;
        for (int i = 0; i < 4; i++) {
            float b = a + i * Mth.HALF_PI;
            SpellUtil.particles(level, BossFx.dust(0x9A3CFF, 1.4f), entity.getX() + Mth.cos(b) * 1.6, entity.getY() + 2.2, entity.getZ() + Mth.sin(b) * 1.6, 1, 0, 0, 0, 0);
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel) {
            Vec3 c = feet(entity);
            float damage = getDamage(spellLevel, entity);
            float band = RADIUS / BANDS;
            BossTelegraph first = BossTelegraph.circle(level, entity, this, spellLevel, c, band)
                    .style(BossTelegraph.Style.VOID).damage(damage).part("band").spawn();
            for (int k = 1; k < BANDS; k++) {
                BossTelegraph.ring(level, entity, this, spellLevel, c, band * k, band * (k + 1))
                        .style(BossTelegraph.Style.VOID).delayed(k * STEP).damage(damage).part("band").spawn();
            }
            BossProp.scene(level, BossProp.Kind.ECLIPSE, BossTelegraph.Style.VOID, RADIUS, now(level), first.getFireTick(), c, c.add(0, SKY_HEIGHT, 0), c, STEP, 0);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 3f, .5f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onTelegraphFire(BossTelegraph t, ServerLevel level) {
        Vec3 c = t.position();
        for (LivingEntity target : t.targetsInside()) {
            if (t.strike(target, t.getDamage())) {
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
                knockAway(target, c, .4, .35);
            }
        }
        float r = t.getShape() == BossTelegraph.Shape.RING ? (t.getSize() + t.getSize2()) * .5f : t.getSize() * .5f;
        int n = (int) (r * 3) + 8;
        for (int i = 0; i < n; i++) {
            float a = i * Mth.TWO_PI / n;
            Vec3 p = SpellUtil.groundAt(level, c.add(Mth.cos(a) * r, 0, Mth.sin(a) * r), 5, 1);
            SpellUtil.particles(level, ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y + .3, p.z, 2, .3, .4, .3, .03);
            SpellUtil.particles(level, ParticleTypes.SQUID_INK, p.x, p.y + .5, p.z, 1, .3, .3, .3, .02);
        }
        BossFx.shockwave(level, c, t.getSize() * 2.2f, t.getStyle().rune);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.5f, .6f + .15f * t.getSize2() / RADIUS);
        SpellUtil.cameraShake(level, c, 14, RADIUS + 6);
    }
}
