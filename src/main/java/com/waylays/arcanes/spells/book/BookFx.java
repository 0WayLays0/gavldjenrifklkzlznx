package com.waylays.arcanes.spells.book;

import com.waylays.arcanes.entity.explosion.BlastEffect;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Look of the Book school: deep violet runes, gilded parchment, black ink. Pages, glyphs and ink particles.
 */
public final class BookFx {
    public static final Vector3f VIOLET = new Vector3f(0.6f, 0.36f, 1.0f);
    public static final Vector3f GILT = new Vector3f(1.0f, 0.84f, 0.45f);
    public static final Vector3f INK = new Vector3f(0.07f, 0.02f, 0.1f);
    public static final Vector3f INK_SHEEN = new Vector3f(0.32f, 0.12f, 0.5f);

    public static final DustParticleOptions VIOLET_DUST = new DustParticleOptions(VIOLET, 1.0f);
    public static final DustParticleOptions GILT_DUST = new DustParticleOptions(GILT, .8f);
    public static final DustParticleOptions INK_DUST = new DustParticleOptions(INK, 1.6f);
    public static final DustParticleOptions INK_SHEEN_DUST = new DustParticleOptions(INK_SHEEN, 1.2f);

    private BookFx() {}

    /** A scrap of paper tumbling through the air. */
    public static ParticleOptions paper() {
        return new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.PAPER));
    }

    /**
     * A rune going off: violet flash and fireball, shock ring, glyphs and torn pages flying out. Visual and sound
     * only. Server side.
     */
    public static void runeBurst(Level level, Vec3 at, float size) {
        BlastEffect.spawn(level, at, size, BlastEffect.STYLE_ARCANE);
        SpellUtil.particles(level, ParticleTypes.FLASH, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        SpellUtil.particles(level, new BlastwaveParticleOptions(VIOLET, size * 2.2f), at.x, at.y, at.z, 1, 0, 0, 0, 0);
        SpellUtil.particles(level, new BlastwaveParticleOptions(GILT, size * 1.4f), at.x, at.y, at.z, 1, 0, 0, 0, 0);
        SpellUtil.particles(level, ParticleTypes.ENCHANT, at.x, at.y, at.z, (int) (40 * size), .2, .2, .2, 1.5);
        SpellUtil.particles(level, paper(), at.x, at.y, at.z, (int) (14 * size), .2, .2, .2, .35);
        SpellUtil.particles(level, VIOLET_DUST, at.x, at.y, at.z, (int) (16 * size), size * .4, size * .4, size * .4, 0);
        float pitch = 1.1f - size * .1f + level.random.nextFloat() * .15f;
        level.playSound(null, at.x, at.y, at.z, SoundEvents.EVOKER_CAST_SPELL, SoundSource.NEUTRAL, 1.4f, pitch);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL, .8f + size * .3f, 1.3f);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BOOK_PAGE_TURN, SoundSource.NEUTRAL, 2f, .6f);
        SpellUtil.cameraShake(level, at, (int) (4 + size * 3), 5 + size * 4);
    }
}
