package com.waylays.arcanes.spells.fire;

import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/** Shared look and sound of Stellar Fire on the server side: flames, embers and smoke in the colour of a heat. */
public final class FireFx {
    private FireFx() {}

    public static DustParticleOptions dust(int tier, float size) {
        int c = HeatColors.BODY[HeatColors.clamp(tier)];
        return new DustParticleOptions(new Vector3f(((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f), size);
    }

    /** Minecraft's own flame closest to the heat. */
    public static ParticleOptions flame(int tier) {
        return switch (HeatColors.clamp(tier)) {
            case 0, 1, 2 -> ParticleTypes.FLAME;
            case 3 -> ParticleTypes.END_ROD;
            case 4 -> ParticleTypes.SOUL_FIRE_FLAME;
            default -> ParticleTypes.DRAGON_BREATH;
        };
    }

    /** Flames licking up at {@code at}. */
    public static void flames(Level level, Vec3 at, int count, double spread, int tier) {
        SpellUtil.particles(level, flame(tier), at.x, at.y, at.z, count, spread, spread * .6, spread, .02);
        SpellUtil.particles(level, dust(tier, 1.6f), at.x, at.y, at.z, count, spread, spread * .6, spread, .02);
    }

    /** Embers flying from {@code at}. */
    public static void embers(Level level, Vec3 at, int count, double spread, int tier) {
        SpellUtil.particles(level, dust(tier, .8f), at.x, at.y, at.z, count, spread, spread, spread, .1);
        if (tier <= 2) {
            SpellUtil.particles(level, ParticleTypes.LAVA, at.x, at.y, at.z, Math.max(1, count / 6), spread * .5, spread * .5, spread * .5, 0);
        }
    }

    public static void smoke(Level level, Vec3 at, int count, double spread) {
        SpellUtil.particles(level, ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, count, spread, spread * .5, spread, .02);
    }

    /** Sets {@code e} burning, longer the hotter the flame. */
    public static void ignite(LivingEntity e, int tier) {
        e.igniteForSeconds(2 + HeatColors.clamp(tier));
    }

    /** Burns {@code e}: {@code amount} damage of the spell's, and sets it alight. */
    public static void burn(LivingEntity e, float amount, @Nullable AbstractSpell spell, Entity direct, @Nullable Entity caster, int tier) {
        if (spell != null) {
            e.invulnerableTime = 0;
            DamageSources.applyDamage(e, amount, SpellUtil.damageSource(spell, direct, caster));
        }
        ignite(e, tier);
    }

    /** A burst of flame: its roar, its light. */
    public static void burst(Level level, Vec3 at, float size, int tier) {
        flames(level, at, (int) (20 * size), size * .6, tier);
        embers(level, at, (int) (16 * size), size * .5, tier);
        smoke(level, at.add(0, size * .5, 0), (int) (6 * size), size * .6);
        sound(level, at, SoundEvents.FIRECHARGE_USE, 1.2f * size, .8f + .1f * tier);
        sound(level, at, SoundEvents.BLAZE_SHOOT, .8f * size, .6f + .12f * tier);
    }

    public static void sound(Level level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }
}
