package com.waylays.arcanes.spells.physical;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server side: who a fighting move reaches (an arc in front, a full circle, a thrust line), how it hits, and how the
 * fighter is moving. A move never reaches through walls, never hurts its fighter's allies, and leaves armor stands
 * alone.
 */
@Mod.EventBusSubscriber(modid = ArcanesOublies.MODID)
public final class Combat {
    /** Each player's last step, newest first: players move on their own client, the server only sees where they end up. */
    private static final Map<UUID, Vec3[]> STEPS = new HashMap<>();

    private Combat() {}

    // ---------------------------------------------------------------- where the fighter faces and goes

    /** Where the fighter looks, on the ground plane. */
    public static Vec3 facing(LivingEntity fighter) {
        float yaw = fighter.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
    }

    /** How the fighter moved during the last tick (for a player, from their last steps). */
    public static Vec3 motion(LivingEntity fighter) {
        if (fighter instanceof Player) {
            Vec3[] steps = STEPS.get(fighter.getUUID());
            return steps == null ? Vec3.ZERO : steps[1];
        }
        return fighter.getDeltaMovement();
    }

    /** Sets the fighter's speed, players included (their client is told). */
    public static void setMotion(LivingEntity fighter, Vec3 velocity) {
        fighter.setDeltaMovement(velocity);
        fighter.hasImpulse = true;
        fighter.hurtMarked = true;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        Vec3 pos = event.player.position();
        Vec3[] steps = STEPS.computeIfAbsent(event.player.getUUID(), k -> new Vec3[]{pos, Vec3.ZERO});
        Vec3 step = pos.subtract(steps[0]);
        // A teleport is not a step
        steps[1] = step.lengthSqr() > 4 ? Vec3.ZERO : step;
        steps[0] = pos;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STEPS.remove(event.getEntity().getUUID());
    }

    // ---------------------------------------------------------------- who a move reaches

    private static List<LivingEntity> candidates(LivingEntity fighter, double reach) {
        AABB box = fighter.getBoundingBox().inflate(reach + 1, 1.5, reach + 1);
        return fighter.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != fighter && e.isAlive() && !(e instanceof ArmorStand)
                && !SpellUtil.isAlly(fighter, e) && fighter.hasLineOfSight(e));
    }

    /** Distance from the fighter to the edge of {@code e}, on the ground plane. */
    private static double reachTo(LivingEntity fighter, LivingEntity e) {
        return Math.max(0, Math.hypot(e.getX() - fighter.getX(), e.getZ() - fighter.getZ()) - e.getBbWidth() * .5);
    }

    private static boolean atHeight(LivingEntity fighter, LivingEntity e) {
        return e.getBoundingBox().maxY >= fighter.getY() - 1 && e.getY() <= fighter.getY() + fighter.getBbHeight() + 1;
    }

    /** Everyone within {@code reach} in front, up to {@code halfAngle} degrees either side of where the fighter faces. */
    public static List<LivingEntity> inArc(LivingEntity fighter, double reach, double halfAngle) {
        Vec3 facing = facing(fighter);
        double minDot = Math.cos(Math.toRadians(halfAngle));
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : candidates(fighter, reach)) {
            Vec3 to = new Vec3(e.getX() - fighter.getX(), 0, e.getZ() - fighter.getZ());
            double distance = reachTo(fighter, e);
            // Someone right against the fighter is always reached
            boolean inFront = to.lengthSqr() < .36 || to.normalize().dot(facing) >= minDot;
            if (distance <= reach && inFront && atHeight(fighter, e)) {
                out.add(e);
            }
        }
        out.sort(Comparator.comparingDouble(e -> e.distanceToSqr(fighter)));
        return out;
    }

    /** Everyone within {@code reach} all around the fighter. */
    public static List<LivingEntity> around(LivingEntity fighter, double reach) {
        return inArc(fighter, reach, 180);
    }

    /** Everyone along a thrust of {@code length} blocks from the fighter's chest where they aim, {@code width} wide. */
    public static List<LivingEntity> inLine(LivingEntity fighter, double length, double width) {
        Vec3 from = fighter.getEyePosition().subtract(0, .4, 0);
        Vec3 to = from.add(fighter.getLookAngle().scale(length));
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : candidates(fighter, length)) {
            AABB box = e.getBoundingBox().inflate(width);
            if (box.contains(from) || box.clip(from, to).isPresent()) {
                out.add(e);
            }
        }
        out.sort(Comparator.comparingDouble(e -> e.distanceToSqr(fighter)));
        return out;
    }

    // ---------------------------------------------------------------- hitting

    /**
     * One blow: {@code damage} as physical damage (armor and Physical Resistance reduce it), then a shove of
     * {@code knockback} away from the fighter. Blows of a flurry go through the short immunity after a hit.
     */
    public static boolean hit(PhysicalSkill skill, LivingEntity fighter, LivingEntity target, float damage, double knockback) {
        target.invulnerableTime = 0;
        boolean hurt = DamageSources.applyDamage(target, damage, skill.getDamageSource(fighter, fighter));
        if (hurt && knockback > 0) {
            target.knockback(knockback, fighter.getX() - target.getX(), fighter.getZ() - target.getZ());
            target.hurtMarked = true;
        }
        if (hurt) {
            Vec3 c = target.getBoundingBox().getCenter();
            SpellUtil.particles(fighter.level(), ParticleTypes.CRIT, c.x, c.y, c.z, 6, .25, .3, .25, .3);
        }
        return hurt;
    }

    /** Dust and grit of the ground under {@code pos} thrown up (a stomp, a take-off, a skid). */
    public static void groundDust(Level level, Vec3 pos, int count, double spread) {
        BlockPos below = BlockPos.containing(pos.x, pos.y - .2, pos.z);
        BlockState state = level.getBlockState(below);
        if (!state.isAir()) {
            SpellUtil.particles(level, new BlockParticleOption(ParticleTypes.BLOCK, state), pos.x, pos.y + .1, pos.z, count, spread, .05, spread, .15);
        }
        SpellUtil.particles(level, ParticleTypes.POOF, pos.x, pos.y + .1, pos.z, Math.max(1, count / 4), spread * .6, .02, spread * .6, .02);
    }
}
