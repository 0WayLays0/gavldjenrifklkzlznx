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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Sweeping Light: an eye of light opens over the caster and sweeps a beam round them, eight slices of the ground
 * (each marked beforehand) burning one after another all the way round.
 */
@AutoSpellConfig
public class SweepingLightSpell extends BossSpell {
    public static final int SLICES = 8;
    public static final float RADIUS = 11;
    /** Ticks between one slice and the next. */
    public static final int STEP = 3;

    public SweepingLightSpell() {
        super("sweeping_light", SpellRarity.RARE, 5, 20, BossAnimations.BEAM);
        this.manaCostPerLevel = 15;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 70;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)),
                warningInfo()
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.BEACON_AMBIENT);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 c = feet(entity);
            float yaw0 = facing(entity);
            float damage = getDamage(spellLevel, entity);
            int fire = 0;
            for (int k = 0; k < SLICES; k++) {
                BossTelegraph t = BossTelegraph.cone(level, entity, this, spellLevel, c, yaw0 + k * 360f / SLICES, RADIUS, 180f / SLICES).style(BossTelegraph.Style.HOLY)
                        .delayed(k * STEP).damage(damage).part("slice").spawn();
                if (k == 0) {
                    fire = t.getFireTick();
                }
            }
            BossProp.scene(level, BossProp.Kind.LASER, BossTelegraph.Style.HOLY, RADIUS, now(level), fire + STEP * SLICES + 14, c, c.add(0, 3.6, 0), c, fire - now(level),
                    yaw0);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onTelegraphFire(BossTelegraph t, ServerLevel level) {
        Vec3 c = t.position();
        for (LivingEntity target : t.targetsInside()) {
            if (t.strike(target, t.getDamage())) {
                target.igniteForSeconds(2);
                knockAway(target, c, .4, .25);
            }
        }
        Vec3 mid = c.add(t.forward().scale(RADIUS * .6));
        SpellUtil.particles(level, ParticleTypes.END_ROD, mid.x, mid.y + .3, mid.z, 16, RADIUS * .25, .2, RADIUS * .25, .03);
        SpellUtil.particles(level, ParticleTypes.FLAME, mid.x, mid.y + .2, mid.z, 10, RADIUS * .25, .1, RADIUS * .25, .02);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.GUARDIAN_ATTACK, SoundSource.HOSTILE, 1.2f, 1.6f);
    }
}
