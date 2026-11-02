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
 * Reaper's Scythe: a giant spectral scythe rises behind the caster and reaps twice round them: first the half in front
 * of them, then the half behind (each marked beforehand). Those it reaps wither.
 */
@AutoSpellConfig
public class ReapersScytheSpell extends BossSpell {
    public static final float RADIUS = 9;
    /** Ticks between the first sweep and the second. */
    public static final int SWEEP = 12;

    public ReapersScytheSpell() {
        super("reapers_scythe", SpellRarity.EPIC, 3, 30, BossAnimations.BLADE_CALL);
        this.manaCostPerLevel = 30;
        this.baseSpellPower = 15;
        this.spellPowerPerLevel = 4;
        this.castTime = 0;
        this.baseManaCost = 110;
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
        return Optional.of(SoundEvents.WITHER_AMBIENT);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 c = feet(entity);
            float yaw = facing(entity);
            float damage = getDamage(spellLevel, entity);
            BossTelegraph first = BossTelegraph.cone(level, entity, this, spellLevel, c, yaw, RADIUS, 90).style(BossTelegraph.Style.VOID).damage(damage).part("reap").spawn();
            BossTelegraph.cone(level, entity, this, spellLevel, c, yaw + 180, RADIUS, 90).style(BossTelegraph.Style.VOID).delayed(SWEEP).damage(damage).part("reap").spawn();
            int fire = first.getFireTick();
            BossProp.scene(level, BossProp.Kind.SCYTHE, BossTelegraph.Style.VOID, RADIUS, now(level), fire + SWEEP + 18, c, c, c, fire - now(level), yaw);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onTelegraphFire(BossTelegraph t, ServerLevel level) {
        Vec3 c = t.position();
        for (LivingEntity target : t.targetsInside()) {
            if (t.strike(target, t.getDamage())) {
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
                knockAway(target, c, .7, .3);
            }
        }
        Vec3 f = t.forward();
        Vec3 mid = c.add(f.scale(RADIUS * .55));
        SpellUtil.particles(level, ParticleTypes.SOUL, mid.x, mid.y + .6, mid.z, 30, RADIUS * .4, .4, RADIUS * .4, .03);
        SpellUtil.particles(level, ParticleTypes.SWEEP_ATTACK, mid.x, mid.y + 1, mid.z, 6, RADIUS * .35, .2, RADIUS * .35, 0);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 3f, .5f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 1.2f, .6f);
    }
}
