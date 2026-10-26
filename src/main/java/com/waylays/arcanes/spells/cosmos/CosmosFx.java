package com.waylays.arcanes.spells.cosmos;

import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Shared look and sound of Cosmos. Time comes as orange dust and the ticking of clocks; space as star motes and the
 * hum of the deep. What time prunes crumbles into orange dust and is gone.
 */
public final class CosmosFx {
    /** The TVA's orange, its amber, white starlight, violet nebula. */
    public static final DustParticleOptions TIME = new DustParticleOptions(new Vector3f(1f, .54f, .12f), 1.1f);
    public static final DustParticleOptions AMBER = new DustParticleOptions(new Vector3f(1f, .72f, .3f), .8f);
    public static final DustParticleOptions STAR = new DustParticleOptions(new Vector3f(.95f, .96f, 1f), .7f);
    public static final DustParticleOptions NEBULA = new DustParticleOptions(new Vector3f(.48f, .32f, 1f), 1.3f);
    public static final DustParticleOptions ICE = new DustParticleOptions(new Vector3f(.62f, .9f, 1f), 1f);
    /** Pruned: orange dust going grey as it falls apart. */
    public static final DustColorTransitionOptions PRUNED = new DustColorTransitionOptions(new Vector3f(1f, .6f, .15f), new Vector3f(.22f, .1f, .05f), 1.4f);

    private CosmosFx() {}

    /** Server side: orange motes of time. */
    public static void time(Level level, Vec3 at, int count, double spread) {
        SpellUtil.particles(level, TIME, at.x, at.y, at.z, count, spread, spread, spread, .02);
        SpellUtil.particles(level, AMBER, at.x, at.y, at.z, Math.max(1, count / 3), spread, spread, spread, .02);
    }

    /** Server side: white and gold star motes. */
    public static void stars(Level level, Vec3 at, int count, double spread) {
        SpellUtil.particles(level, STAR, at.x, at.y, at.z, count, spread, spread, spread, .02);
        SpellUtil.particles(level, ParticleTypes.END_ROD, at.x, at.y, at.z, Math.max(1, count / 4), spread, spread, spread, .02);
    }

    /** Server side: one mote flying from {@code from} along {@code velocity} (a particle sent with a count of 0 moves so). */
    public static void streak(Level level, Vec3 from, Vec3 velocity, boolean star) {
        if (level instanceof ServerLevel server) {
            for (var player : server.players()) {
                server.sendParticles(player, star ? ParticleTypes.END_ROD : ParticleTypes.FLAME, true, from.x, from.y, from.z, 0, velocity.x, velocity.y, velocity.z, 1);
            }
        }
    }

    /**
     * Prunes {@code victim}: it is struck by {@code source} for far more than it has left, its body crumbles into orange
     * dust at once instead of falling over, and the TVA's dull hum closes over it.
     */
    public static void prune(ServerLevel level, LivingEntity victim, DamageSource source) {
        AABB box = victim.getBoundingBox();
        Vec3 c = box.getCenter();
        int count = (int) Math.min(120, 30 + box.getSize() * 40);
        SpellUtil.particles(level, PRUNED, c.x, c.y, c.z, count, box.getXsize() * .35, box.getYsize() * .35, box.getZsize() * .35, .03);
        SpellUtil.particles(level, TIME, c.x, c.y, c.z, count / 2, box.getXsize() * .4, box.getYsize() * .4, box.getZsize() * .4, .08);
        sound(level, c, SoundEvents.BEACON_DEACTIVATE, 1.2f, 1.6f);
        sound(level, c, SoundEvents.SAND_BREAK, 1.2f, .6f);
        victim.invulnerableTime = 0;
        victim.hurt(source, 10000f);
        if (victim.isDeadOrDying() && !(victim instanceof Player)) {
            // Gone at once: nothing is left to fall over
            victim.deathTime = 19;
            victim.setInvisible(true);
        }
    }

    /** The tick of a clock: higher {@code pitch}, smaller clock. */
    public static void tick(Level level, Vec3 at, float volume, float pitch) {
        sound(level, at, SoundEvents.NOTE_BLOCK_HAT.value(), volume, pitch);
        sound(level, at, SoundEvents.COMPARATOR_CLICK, volume * .7f, pitch * .9f);
    }

    /** Time folding: a door opening, something sent back. */
    public static void fold(Level level, Vec3 at, float volume, float pitch) {
        sound(level, at, SoundEvents.BEACON_ACTIVATE, volume, pitch);
        sound(level, at, SoundEvents.ENDERMAN_TELEPORT, volume * .6f, pitch * .7f);
    }

    /** The hum of the deep: what space spells are called with. */
    public static void hum(Level level, Vec3 at, float volume, float pitch) {
        sound(level, at, SoundEvents.BEACON_AMBIENT, volume, pitch);
        sound(level, at, SoundEvents.AMETHYST_BLOCK_RESONATE, volume * .6f, pitch * 1.4f);
    }

    public static void sound(Level level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }
}
