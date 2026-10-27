package com.waylays.arcanes.entity.wall;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.wall.WallFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Siege: a great sigil burns over the target zone, then blazing blocks of masonry come hurtling down out of the
 * sky behind the caster, one after another, each landing on a sigil of its own; every other one is aimed at an enemy
 * in the zone. It draws nothing itself: the boulders are {@link BrickBolt}s (BOULDER).
 */
public class Siege extends SpellEffectEntity {
    public static final int TELEGRAPH = 16;
    /** Ticks a boulder takes to fall, and from how high and how far behind it comes. */
    public static final int FLIGHT = 24;
    public static final float HEIGHT = 26;
    public static final float BEHIND = 14;

    @Nullable
    private String spellId;
    private float radius = 8;
    private int boulders = 10;
    private int duration = 80;
    private float yaw;

    public Siege(EntityType<? extends Siege> entityType, Level level) {
        super(entityType, level);
    }

    public static Siege lay(Level level, LivingEntity caster, AbstractSpell spell, Vec3 at, float radius, int boulders, int duration, float damage) {
        Siege siege = new Siege(ModEntities.SIEGE.get(), level);
        siege.setOwner(caster);
        siege.spellId = spell.getSpellId();
        siege.damage = damage;
        siege.radius = radius;
        siege.boulders = boulders;
        siege.duration = duration;
        siege.yaw = caster.getYRot();
        siege.moveTo(at.x, at.y, at.z, caster.getYRot(), 0);
        level.addFreshEntity(siege);
        WallFx.sigil(level, at, radius + 1, TELEGRAPH + duration + FLIGHT);
        WallFx.sound(level, at, SoundEvents.BELL_RESONATE, 1.5f, .5f);
        return siege;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > TELEGRAPH + duration + FLIGHT + 5) {
            discard();
            return;
        }
        int t = tickCount - TELEGRAPH;
        if (t < 0 || t >= duration) {
            return;
        }
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        LivingEntity caster = getLivingOwner();
        if (spell == null || caster == null) {
            return;
        }
        for (int i = 0; i < boulders; i++) {
            if (i * duration / boulders != t) {
                continue;
            }
            Vec3 spot = null;
            if (i % 2 == 1) {
                List<LivingEntity> enemies = SpellUtil.enemiesAround(level, position(), radius + 1, caster, e -> true);
                if (!enemies.isEmpty()) {
                    LivingEntity e = enemies.get(random.nextInt(enemies.size()));
                    // Where it will be, roughly, when the block lands
                    spot = e.position().add(e.getDeltaMovement().multiply(FLIGHT * .5, 0, FLIGHT * .5));
                }
            }
            if (spot == null) {
                float a = WallFx.hash(getId() * 31 + i) * Mth.TWO_PI;
                float r = i == 0 ? 0 : Mth.sqrt(WallFx.hash(getId() * 17 + i)) * radius;
                spot = position().add(Mth.cos(a) * r, 0, Mth.sin(a) * r);
            }
            spot = SpellUtil.groundAt(level, spot, 6, 1);
            // From high up behind the caster, as if from catapults beyond the horizon
            Vec3 back = Vec3.directionFromRotation(0, yaw).scale(-BEHIND);
            Vec3 from = spot.add(back).add((random.nextDouble() - .5) * 6, HEIGHT, (random.nextDouble() - .5) * 6);
            Vec3 d = spot.subtract(from);
            double vy = (d.y + BrickBolt.BOULDER_GRAVITY * FLIGHT * (FLIGHT - 1) / 2) / FLIGHT;
            Vec3 velocity = new Vec3(d.x / FLIGHT, vy, d.z / FLIGHT);
            BrickBolt.shoot(level, caster, spell, BrickBolt.Kind.BOULDER, from, velocity, spot, damage);
            WallFx.sound(level, spot, SoundEvents.FIRECHARGE_USE, 1.5f, .5f);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return false;
    }
}
