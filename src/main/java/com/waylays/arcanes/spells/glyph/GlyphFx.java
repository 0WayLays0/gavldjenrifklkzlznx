package com.waylays.arcanes.spells.glyph;

import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Particles and sounds of Glyph Magic: what each element sheds where it strikes, and the trail of its bolts. */
public final class GlyphFx {
    private GlyphFx() {}

    /** Server side: a puff of the element's ink and its particle. */
    public static void burst(Level level, GlyphElement element, Vec3 at, int count, double spread) {
        SpellUtil.particles(level, element.dust, at.x, at.y, at.z, count, spread, spread, spread, 0);
        SpellUtil.particles(level, element.particle(), at.x, at.y, at.z, count / 2 + 1, spread, spread, spread, .04);
    }

    /** Server side: where a bolt lands or an element erupts, with its sound. */
    public static void impact(Level level, GlyphElement element, Vec3 at, float radius) {
        double r = radius * .5;
        int n = 8 + (int) (radius * 8);
        burst(level, element, at, n, r);
        switch (element) {
            case FIRE -> {
                SpellUtil.particles(level, ParticleTypes.FLAME, at.x, at.y, at.z, n, r, r * .6, r, .08);
                SpellUtil.particles(level, ParticleTypes.LAVA, at.x, at.y, at.z, 3, r * .5, .1, r * .5, 0);
                sound(level, at, SoundEvents.FIRECHARGE_USE, 1f, 1.1f);
            }
            case FROST -> {
                SpellUtil.particles(level, ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, n, r, r, r, .06);
                SpellUtil.particles(level, ParticleTypes.ITEM_SNOWBALL, at.x, at.y, at.z, n / 2, r, r * .5, r, .1);
                sound(level, at, SoundEvents.GLASS_BREAK, .8f, 1.5f);
            }
            case STORM -> {
                SpellUtil.particles(level, ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, n * 2, r, r, r, .3);
                SpellUtil.particles(level, ParticleTypes.FLASH, at.x, at.y, at.z, 1, 0, 0, 0, 0);
                sound(level, at, SoundEvents.LIGHTNING_BOLT_IMPACT, .7f, 1.6f);
            }
            case TIDE -> {
                SpellUtil.particles(level, ParticleTypes.SPLASH, at.x, at.y + .2, at.z, n * 3, r, .3, r, .3);
                SpellUtil.particles(level, ParticleTypes.BUBBLE_POP, at.x, at.y + .3, at.z, n, r, r * .5, r, .05);
                sound(level, at, SoundEvents.PLAYER_SPLASH, .9f, 1.2f);
            }
            case GALE -> {
                SpellUtil.particles(level, ParticleTypes.CLOUD, at.x, at.y + .2, at.z, n, r, r * .4, r, .15);
                SpellUtil.particles(level, ParticleTypes.POOF, at.x, at.y + .3, at.z, n / 2, r, r * .4, r, .1);
                sound(level, at, SoundEvents.PHANTOM_SWOOP, .9f, 1.4f);
            }
            case STONE -> {
                SpellUtil.particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COARSE_DIRT.defaultBlockState()), at.x, at.y + .2, at.z, n * 2, r, .3, r, .2);
                SpellUtil.particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), at.x, at.y + .2, at.z, n, r, .3, r, .2);
                sound(level, at, SoundEvents.DEEPSLATE_BREAK, 1.2f, .7f);
            }
        }
    }

    /** Client side: what a bolt of the element leaves behind it. */
    public static void trail(Level level, GlyphElement element, Vec3 at, float size, RandomSource random) {
        double s = .15 * size;
        double x = at.x + (random.nextDouble() - .5) * s;
        double y = at.y + (random.nextDouble() - .5) * s;
        double z = at.z + (random.nextDouble() - .5) * s;
        level.addParticle(element.dust, x, y, z, 0, 0, 0);
        switch (element) {
            case FIRE -> {
                level.addParticle(ParticleTypes.FLAME, x, y, z, 0, .01, 0);
                if (random.nextInt(3) == 0) {
                    level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, .02, 0);
                }
            }
            case FROST -> level.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, 0, -.02, 0);
            case STORM -> level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, (random.nextDouble() - .5) * .2, (random.nextDouble() - .5) * .2, (random.nextDouble() - .5) * .2);
            case TIDE -> level.addParticle(ParticleTypes.FALLING_WATER, x, y, z, 0, 0, 0);
            case GALE -> {
                if (random.nextInt(2) == 0) {
                    level.addParticle(ParticleTypes.CLOUD, x, y, z, 0, 0, 0);
                }
            }
            case STONE -> level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COARSE_DIRT.defaultBlockState()), x, y, z, 0, 0, 0);
        }
    }

    /** The sound of a glyph being written in an element. */
    public static void writing(Level level, GlyphElement element, Vec3 at) {
        sound(level, at, SoundEvents.AMETHYST_BLOCK_RESONATE, .7f, 1.4f + element.ordinal() * .08f);
        sound(level, at, switch (element) {
            case FIRE -> SoundEvents.BLAZE_SHOOT;
            case FROST -> SoundEvents.AMETHYST_CLUSTER_BREAK;
            case STORM -> SoundEvents.BEACON_POWER_SELECT;
            case TIDE -> SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE;
            case GALE -> SoundEvents.ENDER_DRAGON_FLAP;
            case STONE -> SoundEvents.DEEPSLATE_PLACE;
        }, .6f, 1.3f);
    }

    public static void sound(Level level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }
}
