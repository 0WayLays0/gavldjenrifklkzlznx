package com.waylays.arcanes.entity.rune;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.rune.RuneFx;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Circle of the Elders: eight great runestones rise in a ring around the caster. Every second their runes pulse:
 * the caster's allies inside the ring are healed and shielded, and arcs of runic lightning leap from the stones to the
 * enemies inside. Then the stones sink back into the ground.
 */
public class ElderCircle extends SpellEffectEntity {
    public static final int STONES = 8;
    /** Each stone rises this long after the one before it; the last one is up after {@link #RISE_TICKS}. */
    public static final int STONE_STAGGER = 3;
    public static final int RISE_TICKS = 30;
    public static final int SINK_TICKS = 20;
    public static final int PULSE_TICKS = 20;
    public static final int MAX_ARCS = 6;
    public static final int ARC_TICKS = 8;

    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(ElderCircle.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(ElderCircle.class, EntityDataSerializers.INT);
    /** Tick of the last pulse. */
    private static final EntityDataAccessor<Integer> DATA_PULSE = SynchedEntityData.defineId(ElderCircle.class, EntityDataSerializers.INT);
    @SuppressWarnings("unchecked")
    private static final EntityDataAccessor<Integer>[] DATA_ARCS = new EntityDataAccessor[MAX_ARCS];

    static {
        for (int i = 0; i < MAX_ARCS; i++) {
            DATA_ARCS[i] = SynchedEntityData.defineId(ElderCircle.class, EntityDataSerializers.INT);
        }
    }

    private float heal;
    private int spellLevel = 1;

    public ElderCircle(EntityType<? extends ElderCircle> entityType, Level level) {
        super(entityType, level);
    }

    public static ElderCircle raise(Level level, LivingEntity caster, Vec3 at, float radius, int life, float damage, float heal, int spellLevel) {
        ElderCircle circle = new ElderCircle(ModEntities.ELDER_CIRCLE.get(), level);
        circle.setOwner(caster);
        circle.moveTo(at.x, at.y, at.z, caster.getYRot(), 0);
        circle.entityData.set(DATA_RADIUS, radius);
        circle.entityData.set(DATA_LIFE, life);
        circle.damage = damage;
        circle.heal = heal;
        circle.spellLevel = spellLevel;
        level.addFreshEntity(circle);
        return circle;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RADIUS, 8f);
        builder.define(DATA_LIFE, 200);
        builder.define(DATA_PULSE, -1000);
        for (EntityDataAccessor<Integer> arc : DATA_ARCS) {
            builder.define(arc, -1);
        }
    }

    public float getRadius() {
        return this.entityData.get(DATA_RADIUS);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    /** Where the {@code i}-th stone stands, on the ground of the circle. */
    public Vec3 stone(int i) {
        float a = getYRot() * Mth.DEG_TO_RAD + i * Mth.TWO_PI / STONES;
        return position().add(Mth.cos(a) * getRadius(), 0, Mth.sin(a) * getRadius());
    }

    /** How far the {@code i}-th stone is out of the ground (0 to 1). */
    public float risen(int i, float partialTicks) {
        float age = tickCount + partialTicks;
        float up = Mth.clamp((age - i * STONE_STAGGER) / (RISE_TICKS - (STONES - 1) * STONE_STAGGER), 0, 1);
        float down = Mth.clamp((age - getLife()) / SINK_TICKS, 0, 1);
        return (1 - (1 - up) * (1 - up)) * (1 - down * down);
    }

    public float sincePulse(float partialTicks) {
        return tickCount + partialTicks - this.entityData.get(DATA_PULSE);
    }

    /** The enemy struck by the {@code i}-th arc of the last pulse, by entity id (-1: none). */
    public int arcTarget(int i) {
        return this.entityData.get(DATA_ARCS[i]);
    }

    /** The stone nearest to a point: arcs leap from it. */
    public int nearestStone(Vec3 p) {
        int best = 0;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < STONES; i++) {
            double d = stone(i).distanceToSqr(p);
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return best;
    }

    private boolean inside(Entity e) {
        double r = getRadius();
        return e.distanceToSqr(getX(), e.getY(), getZ()) <= r * r && e.getY() > getY() - 2 && e.getY() < getY() + 5;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (tickCount < getLife() && random.nextInt(2) == 0) {
                // Glyphs streaming up the stones
                Vec3 s = stone(random.nextInt(STONES));
                level().addParticle(ParticleTypes.ENCHANT, s.x + (random.nextDouble() - .5), s.y + random.nextDouble() * 3, s.z + (random.nextDouble() - .5), 0, .5, 0);
            }
            return;
        }
        if (tickCount >= getLife() + SINK_TICKS) {
            discard();
            return;
        }
        if (tickCount >= RISE_TICKS && tickCount < getLife() && (tickCount - RISE_TICKS) % PULSE_TICKS == 0) {
            pulse((ServerLevel) level());
        }
    }

    private void pulse(ServerLevel level) {
        this.entityData.set(DATA_PULSE, tickCount);
        LivingEntity caster = getLivingOwner();
        Entity owner = getOwner();
        double r = getRadius();
        AABB box = new AABB(position(), position()).inflate(r, 5, r);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, x -> inside(x) && RuneFx.helps(caster, x))) {
            if (e.getHealth() < e.getMaxHealth()) {
                e.heal(heal);
                SpellUtil.particles(level, RuneFx.MEND_DUST, e.getX(), e.getY() + e.getBbHeight() * .5, e.getZ(), 6, .3, .5, .3, 0);
            }
            e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, spellLevel >= 3 ? 1 : 0, true, true, true));
        }
        List<LivingEntity> enemies = SpellUtil.enemiesAround(level, position().add(0, 1, 0), r + 4, owner, this::inside);
        enemies.sort(Comparator.comparingDouble(e -> e.distanceToSqr(this)));
        for (int i = 0; i < MAX_ARCS; i++) {
            LivingEntity target = i < enemies.size() ? enemies.get(i) : null;
            this.entityData.set(DATA_ARCS[i], target == null ? -1 : target.getId());
            if (target != null) {
                target.invulnerableTime = 0;
                DamageSources.applyDamage(target, damage, SpellUtil.damageSource(ModSpells.CIRCLE_OF_ELDERS.get(), this, owner));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 1));
                Vec3 at = target.position().add(0, target.getBbHeight() * .5, 0);
                SpellUtil.particles(level, ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 10, .3, .4, .3, .1);
            }
        }
        RuneFx.ring(level, position(), r, 40, RuneFx.GLOW_DUST);
        RuneFx.sound(level, position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, .6f);
        if (!enemies.isEmpty()) {
            RuneFx.sound(level, position(), SoundEvents.LIGHTNING_BOLT_IMPACT, .7f, 1.6f);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
    }
}
