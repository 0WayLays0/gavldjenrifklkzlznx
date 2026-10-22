package com.waylays.arcanes.entity.wall;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.wall.WallFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The Fallen Citadel (Wall Magic's ultimate): a whole fortress of stone bricks rises around the target, wall after
 * wall, eight crenellated walls joined by four towers. Those inside are shut in; the towers lob bricks down on them.
 * Then every wall topples inward at once, burying the inside under the fortress.
 */
public class Citadel extends SpellEffectEntity {
    public static final int WALLS = 8;
    public static final float RADIUS = 6.5f;
    public static final float WALL_HEIGHT = 4;
    public static final float TOWER_HEIGHT = 6.5f;
    public static final int STAGGER = 2;
    public static final int RISE = 8;
    public static final int RISEN = (WALLS - 1) * STAGGER + RISE;
    public static final int HOLD = 60;
    public static final int FALL = 12;
    public static final int IMPACT = RISEN + HOLD + FALL;
    public static final int RUBBLE = 30;
    public static final int SINK = 14;
    public static final int VOLLEY_TICKS = 15;

    @Nullable
    private String spellId;

    public Citadel(EntityType<? extends Citadel> entityType, Level level) {
        super(entityType, level);
    }

    public static Citadel raise(Level level, LivingEntity caster, AbstractSpell spell, Vec3 at, float damage) {
        Citadel citadel = new Citadel(ModEntities.CITADEL.get(), level);
        citadel.setOwner(caster);
        citadel.spellId = spell.getSpellId();
        citadel.damage = damage;
        citadel.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 45, 0);
        level.addFreshEntity(citadel);
        return citadel;
    }

    /** Corner {@code i} of the octagon, relative to the middle. */
    public Vec3 corner(int i) {
        float a = getYRot() * Mth.DEG_TO_RAD + i * Mth.TWO_PI / WALLS;
        return new Vec3(Mth.cos(a) * RADIUS, 0, Mth.sin(a) * RADIUS);
    }

    /** How far wall {@code i} (from corner i to corner i+1) is out of the ground (0 to 1), sinking at the very end. */
    public float risen(int i, float partialTicks) {
        float t = Mth.clamp((tickCount + partialTicks - i * STAGGER) / RISE, 0, 1);
        float sink = Mth.clamp((tickCount + partialTicks - IMPACT - RUBBLE) / SINK, 0, 1);
        return (1 - (1 - t) * (1 - t)) * (1 - sink);
    }

    /** How far the walls have toppled inward, in degrees (0 standing, 90 flat). */
    public float fallen(float partialTicks) {
        float t = Mth.clamp((tickCount + partialTicks - RISEN - HOLD) / FALL, 0, 1);
        return 90 * t * t;
    }

    private boolean inside(Entity e) {
        return e.distanceToSqr(getX(), e.getY(), getZ()) <= (RADIUS - .5) * (RADIUS - .5) && e.getY() > getY() - 2 && e.getY() < getY() + WALL_HEIGHT;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        ServerLevel level = (ServerLevel) level();
        if (tickCount > IMPACT + RUBBLE + SINK) {
            discard();
            return;
        }
        if (tickCount < RISEN && tickCount % STAGGER == 0) {
            int i = tickCount / STAGGER;
            Vec3 mid = position().add(corner(i).add(corner((i + 1) % WALLS)).scale(.5));
            WallFx.rise(level, mid, 1.4f);
            WallFx.dust(level, mid.add(0, .4, 0), 20, 1.5);
        }
        if (tickCount > RISEN && tickCount < RISEN + HOLD) {
            shutIn(level);
            if ((tickCount - RISEN) % VOLLEY_TICKS == 0) {
                volley(level);
            }
        }
        if (tickCount == RISEN + HOLD) {
            WallFx.sound(level, position(), SoundEvents.STONE_BREAK, 2f, .4f);
        }
        if (tickCount == IMPACT) {
            collapse(level);
        }
    }

    /** Those inside who reach the walls are pushed back in. */
    private void shutIn(ServerLevel level) {
        for (LivingEntity e : SpellUtil.enemiesAround(level, position().add(0, 1, 0), RADIUS + 1, getOwner(), this::inside)) {
            Vec3 d = e.position().subtract(position()).multiply(1, 0, 1);
            if (d.length() > RADIUS - 2) {
                e.setDeltaMovement(d.normalize().scale(-.4).add(0, .1, 0));
                e.hurtMarked = true;
            }
        }
    }

    /** Each tower lobs a brick at an enemy inside. */
    private void volley(ServerLevel level) {
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        LivingEntity caster = getLivingOwner();
        if (spell == null || caster == null) {
            return;
        }
        var inside = SpellUtil.enemiesAround(level, position().add(0, 1, 0), RADIUS, getOwner(), this::inside);
        for (int t = 0; t < WALLS; t += 2) {
            Vec3 from = position().add(corner(t)).add(0, TOWER_HEIGHT + .5, 0);
            Vec3 to = inside.isEmpty() ? position().add((random.nextDouble() - .5) * RADIUS, 0, (random.nextDouble() - .5) * RADIUS)
                    : inside.get(random.nextInt(inside.size())).position();
            int ticks = 14;
            double g = .04;
            Vec3 d = to.subtract(from);
            Vec3 velocity = new Vec3(d.x / ticks, d.y / ticks + .5 * g * (ticks - 1), d.z / ticks);
            BrickBolt.shoot(level, caster, spell, BrickBolt.Kind.FALLING, from, velocity, damage * .3f);
        }
        WallFx.sound(level, position(), SoundEvents.CROSSBOW_SHOOT, 1.4f, .5f);
    }

    /** The walls fall in: everything inside is buried, those just outside hurt by the blast of dust and stone. */
    private void collapse(ServerLevel level) {
        Vec3 c = position();
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        for (LivingEntity e : SpellUtil.enemiesAround(level, c.add(0, 1, 0), RADIUS + 3, getOwner(), x -> x.getY() > c.y - 2 && x.getY() < c.y + TOWER_HEIGHT)) {
            boolean in = inside(e);
            if (spell != null) {
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, in ? damage * 3 : damage, SpellUtil.damageSource(spell, this, getOwner()));
            }
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, in ? 100 : 40, in ? 3 : 1));
            e.setDeltaMovement(e.getDeltaMovement().multiply(.2, 0, .2).add(0, -.3, 0));
            e.hurtMarked = true;
        }
        for (int i = 0; i < WALLS; i++) {
            Vec3 at = c.add(corner(i).scale(.55));
            WallFx.dust(level, at.add(0, .5, 0), 36, 1.8);
        }
        level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.2f, .5f);
        WallFx.slam(level, c, 2.5f);
        SpellUtil.cameraShake(level, c, 34, RADIUS + 20);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
    }
}
