package com.waylays.arcanes.spells.echo;

import com.waylays.arcanes.entity.echo.EchoScene;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/**
 * Shared workings of Echo Magic: Resonance (the sound left ringing in a creature, up to {@link #MAX} levels, making
 * Echo blows strike harder and waiting to be made to burst) and the sound of it.
 */
public final class EchoFx {
    /** The most Resonance a creature can hold. */
    public static final int MAX = 5;
    /** How long Resonance rings on after the last echo that added to it (ticks). */
    public static final int RINGS = 200;
    /** How much harder Echo blows strike for each level of Resonance. */
    public static final float BONUS = .1f;

    private EchoFx() {}

    /** How much Resonance {@code e} holds (0 to {@link #MAX}). */
    public static int resonance(LivingEntity e) {
        MobEffectInstance r = e.getEffect(ModEffects.RESONANCE);
        return r == null ? 0 : r.getAmplifier() + 1;
    }

    /** Adds {@code levels} of Resonance to {@code e} (up to {@link #MAX}), ringing on afresh. */
    public static void resonate(LivingEntity e, int levels) {
        int now = Math.min(MAX, resonance(e) + levels);
        if (now > 0) {
            e.addEffect(new MobEffectInstance(ModEffects.RESONANCE, RINGS, now - 1));
        }
    }

    /**
     * An Echo blow: {@code damage}, harder for the Resonance {@code target} already holds, then {@code resonance} more
     * levels of it. Returns whether it hurt.
     */
    public static boolean hit(AbstractSpell spell, Entity direct, @Nullable Entity caster, LivingEntity target, float damage, int resonance) {
        float hard = damage * (1 + BONUS * resonance(target));
        boolean hurt = DamageSources.applyDamage(target, hard, SpellUtil.damageSource(spell, direct, caster));
        if (hurt && target.isAlive() && resonance > 0) {
            resonate(target, resonance);
        }
        return hurt;
    }

    /**
     * Makes the Resonance in {@code target} burst: {@code perLevel} damage for each level it held, the Resonance spent.
     * Returns how many levels burst (0 if it held none).
     */
    public static int burst(ServerLevel level, AbstractSpell spell, Entity direct, @Nullable LivingEntity caster, LivingEntity target, float perLevel) {
        int levels = resonance(target);
        if (levels <= 0) {
            return 0;
        }
        target.removeEffect(ModEffects.RESONANCE);
        target.invulnerableTime = 0;
        DamageSources.applyDamage(target, perLevel * levels, SpellUtil.damageSource(spell, direct, caster));
        Vec3 mid = target.position().add(0, target.getBbHeight() * .5, 0);
        EchoScene.visual(level, caster, spell, EchoScene.Kind.BURST, mid, new Vector3f(0, 1, 0), levels, 0);
        SpellUtil.particles(level, ParticleTypes.SCULK_CHARGE_POP, mid.x, mid.y, mid.z, 6 + 4 * levels, .4, .5, .4, .08);
        level.playSound(null, mid.x, mid.y, mid.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.2f, .6f + .1f * levels);
        level.playSound(null, mid.x, mid.y, mid.z, SoundEvents.SCULK_SHRIEKER_BREAK, SoundSource.PLAYERS, 1.4f, 1.2f);
        return levels;
    }

    /** Server side: a pulse of the deep dark's light at {@code at}. */
    public static void pulse(Level level, Vec3 at, int count, double spread) {
        SpellUtil.particles(level, ParticleTypes.SCULK_SOUL, at.x, at.y, at.z, Math.max(1, count / 3), spread, spread * .5, spread, .02);
        SpellUtil.particles(level, ParticleTypes.SCULK_CHARGE_POP, at.x, at.y, at.z, count, spread, spread * .5, spread, .04);
    }
}
