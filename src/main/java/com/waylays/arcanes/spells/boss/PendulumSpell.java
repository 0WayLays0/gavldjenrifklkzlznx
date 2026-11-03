package com.waylays.arcanes.spells.boss;

import com.waylays.arcanes.entity.boss.BossProp;
import com.waylays.arcanes.entity.boss.BossTelegraph;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import java.util.List;
import java.util.Optional;

/**
 * Pendulum of Doom: a strip is marked across the spot aimed at, and a pendulum hangs over it from a rune circle in the
 * sky: a long rod, a great crescent blade of steel. Drawn back, it is let go: it swings through the strip three times,
 * each pass cutting all along it, then rises back into the sky.
 */
@AutoSpellConfig
public class PendulumSpell extends BossSpell {
    public static final double RANGE = 16;
    public static final float LENGTH = 20, WIDTH = 2.6f, HEIGHT = 15;
    public static final int SWINGS = 3;
    /** Ticks between one pass and the next. */
    public static final int SWING = 16;

    public PendulumSpell() {
        super("pendulum", SpellRarity.RARE, 5, 25, BossAnimations.SKY_CALL);
        this.manaCostPerLevel = 12;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 70;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.length", Utils.stringTruncation(LENGTH, 1)),
                warningInfo()
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.CHAIN_PLACE);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 center = aimedGround(level, entity, RANGE);
            // The strip runs across the caster's view, so that they see the blade swing through it
            float yaw = facing(entity) + 90;
            Vec3 along = direction(yaw);
            Vec3 start = SpellUtil.groundAt(level, center.subtract(along.scale(LENGTH * .5)), 4, 1);
            float damage = getDamage(spellLevel, entity);
            int fire = 0;
            for (int k = 0; k < SWINGS; k++) {
                BossTelegraph t = BossTelegraph.line(level, entity, this, spellLevel, start, yaw, LENGTH, WIDTH).style(BossTelegraph.Style.CRIMSON).delayed(k * SWING)
                        .damage(damage).part("swing").spawn();
                if (k == 0) {
                    fire = t.getFireTick();
                }
            }
            BossProp.scene(level, BossProp.Kind.PENDULUM, BossTelegraph.Style.CRIMSON, LENGTH, now(level), fire + SWING * SWINGS + 30, center,
                    center.add(0, HEIGHT, 0), start, fire - now(level), yaw);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onTelegraphFire(BossTelegraph t, ServerLevel level) {
        Vec3 c = t.center();
        for (LivingEntity target : t.targetsInside()) {
            if (t.strike(target, t.getDamage())) {
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
                knockAway(target, c, .6, .35);
            }
        }
        Vec3 f = t.forward();
        SpellUtil.particles(level, ParticleTypes.SWEEP_ATTACK, c.x, c.y + 1, c.z, 8, f.x * LENGTH * .35, .3, f.z * LENGTH * .35, 0);
        SpellUtil.particles(level, ParticleTypes.CRIT, c.x, c.y + 1, c.z, 30, f.x * LENGTH * .35, .5, f.z * LENGTH * .35, .3);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 3f, .5f);
        level.playSound(null, c.x, c.y + 6, c.z, SoundEvents.CHAIN_BREAK, SoundSource.HOSTILE, 1.5f, .5f);
    }
}
