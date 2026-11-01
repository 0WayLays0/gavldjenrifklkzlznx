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
 * Wrath of the Heavens (ultimate): a storm gathers over the caster, a great wheel of dark cloud turning round a glowing
 * eye, and hurls bolt after bolt at the foes beneath it. Each bolt is marked where its foe stands when it is called, so
 * keep moving: the storm hunts, but it never strikes without warning.
 */
@AutoSpellConfig
public class WrathOfTheHeavensSpell extends BossSpell {
    public static final float RADIUS = 16;
    public static final float BOLT_RADIUS = 2.4f;
    public static final float STORM_HEIGHT = 20;
    /** A new bolt is called every this many ticks. */
    public static final int CALL = 6;

    public WrathOfTheHeavensSpell() {
        super("wrath_of_the_heavens", SpellRarity.LEGENDARY, 3, 90, BossAnimations.SKY_CALL);
        this.manaCostPerLevel = 40;
        this.baseSpellPower = 12;
        this.spellPowerPerLevel = 3;
        this.castTime = 20;
        this.baseManaCost = 220;
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    public static int bolts(int spellLevel) {
        return 8 + 2 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.bolts", bolts(spellLevel), Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)),
                warningInfo()
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.EVOKER_PREPARE_SUMMON);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.LIGHTNING_BOLT_THUNDER);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @org.jetbrains.annotations.Nullable MagicData playerMagicData) {
        SpellUtil.particles(level, ParticleTypes.ELECTRIC_SPARK, entity.getX(), entity.getY() + 2.3, entity.getZ(), 3, .5, .3, .5, .1);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 c = feet(entity);
            float damage = getDamage(spellLevel, entity);
            int count = bolts(spellLevel);
            int now = now(level);
            int spent = now + count * CALL + BossTelegraph.warningTicks() + 4;
            BossProp.scene(level, BossProp.Kind.STORM, BossTelegraph.Style.STAR, RADIUS, now, spent, c, c.add(0, STORM_HEIGHT, 0), c, 0, 0);
            for (int i = 0; i < count; i++) {
                int turn = i;
                BossScheduler.after(server, i * CALL, l -> {
                    // The foes under the storm in turn, nearest first; somewhere in it if there are none
                    List<LivingEntity> foes = foes(l, entity, c, RADIUS, 8);
                    Vec3 spot;
                    if (!foes.isEmpty()) {
                        spot = foes.get(turn % foes.size()).position();
                    } else {
                        float a = l.random.nextFloat() * Mth.TWO_PI;
                        float r = 3 + l.random.nextFloat() * (RADIUS - 3);
                        spot = c.add(Mth.cos(a) * r, 0, Mth.sin(a) * r);
                    }
                    Vec3 ground = SpellUtil.groundAt(l, spot, 5, 1);
                    BossTelegraph t = BossTelegraph.circle(l, entity, this, spellLevel, ground, BOLT_RADIUS)
                            .style(BossTelegraph.Style.STAR).damage(damage).part("bolt").spawn();
                    Vec3 cloud = c.add((ground.x - c.x) * .4 + l.random.nextFloat() * 4 - 2, STORM_HEIGHT - 2, (ground.z - c.z) * .4 + l.random.nextFloat() * 4 - 2);
                    BossProp.scene(l, BossProp.Kind.BOLT, BossTelegraph.Style.STAR, 1, t.getFireTick(), t.getFireTick(), ground, cloud, ground, 0, 0);
                });
            }
            level.playSound(null, c.x, c.y + STORM_HEIGHT, c.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 3f, .5f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onTelegraphFire(BossTelegraph t, ServerLevel level) {
        Vec3 c = t.position();
        for (LivingEntity target : t.targetsInside()) {
            if (t.strike(target, t.getDamage())) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3));
                knockAway(target, c, .4, .3);
            }
        }
        SpellUtil.particles(level, ParticleTypes.ELECTRIC_SPARK, c.x, c.y + .5, c.z, 40, .6, .8, .6, .4);
        SpellUtil.particles(level, ParticleTypes.END_ROD, c.x, c.y + .5, c.z, 10, .3, .5, .3, .15);
        BossFx.shockwave(level, c, t.getSize() * 2.2f, t.getStyle().rune);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 2.5f, .9f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 1.6f, 1.2f);
        SpellUtil.cameraShake(level, c, 6, 10);
    }
}
