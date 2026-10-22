package com.waylays.arcanes.entity.gem;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.gem.Gem;
import com.waylays.arcanes.spells.gem.GemFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Amethyst Resonance: clusters of amethyst burst out of the ground in a ring round the caster and ring together, three
 * times, each note higher. Each time a wave of sound rolls out across the ground: it hurts and dazes the enemies it
 * passes, and shatters the enemy shots it meets. Then the clusters break.
 */
public class AmethystResonance extends SpellEffectEntity {
    public static final int CLUSTERS = 8;
    public static final float RING = 3.2f;
    public static final int RISE = 12;
    public static final int PULSES = 3;
    public static final int PULSE_TICKS = 20;
    /** Ticks a wave takes to roll out to its full range. */
    public static final int WAVE_TICKS = 12;
    public static final float RANGE = 8;

    private static final EntityDataAccessor<Float> DATA_RANGE = SynchedEntityData.defineId(AmethystResonance.class, EntityDataSerializers.FLOAT);

    @Nullable
    private String spellId;
    private final IntOpenHashSet struck = new IntOpenHashSet();

    public AmethystResonance(EntityType<? extends AmethystResonance> entityType, Level level) {
        super(entityType, level);
    }

    public static AmethystResonance ring(Level level, LivingEntity caster, AbstractSpell spell, Vec3 at, float range, float damage) {
        AmethystResonance ring = new AmethystResonance(ModEntities.AMETHYST_RESONANCE.get(), level);
        ring.setOwner(caster);
        ring.spellId = spell.getSpellId();
        ring.damage = damage;
        ring.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360, 0);
        ring.entityData.set(DATA_RANGE, range);
        level.addFreshEntity(ring);
        GemFx.sigil(level, at, Gem.AMETHYST, RING + 1, RISE + 12);
        return ring;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RANGE, RANGE);
    }

    public float getRange() {
        return this.entityData.get(DATA_RANGE);
    }

    /** The tick pulse {@code k} (1 to {@link #PULSES}) rings. */
    public static int pulseAt(int k) {
        return RISE + (k - 1) * PULSE_TICKS + 8;
    }

    /** When the clusters break. */
    public static int end() {
        return pulseAt(PULSES) + WAVE_TICKS + 8;
    }

    /** Where cluster {@code i} stands, from the middle. */
    public Vec3 cluster(int i) {
        float a = getYRot() * Mth.DEG_TO_RAD + i * Mth.TWO_PI / CLUSTERS;
        return new Vec3(Mth.cos(a) * RING, 0, Mth.sin(a) * RING);
    }

    /** How far wave {@code k}'s front has rolled (0 when not rolling). */
    public float wave(int k, float age) {
        float t = (age - pulseAt(k)) / WAVE_TICKS;
        return t < 0 || t > 1 ? 0 : getRange() * (1 - (1 - t) * (1 - t));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount == 1) {
            GemFx.sound(level, position(), net.minecraft.sounds.SoundEvents.AMETHYST_CLUSTER_PLACE, 1.6f, .7f);
        }
        if (tickCount >= end()) {
            for (int i = 0; i < CLUSTERS; i++) {
                GemFx.shatter(level, position().add(cluster(i)).add(0, .6, 0), Gem.AMETHYST, .9f, 6);
            }
            discard();
            return;
        }
        for (int k = 1; k <= PULSES; k++) {
            if (tickCount == pulseAt(k)) {
                struck.clear();
                GemFx.chime(level, position(), 2f, .7f + .25f * k);
                for (int i = 0; i < CLUSTERS; i++) {
                    GemFx.glitter(level, position().add(cluster(i)).add(0, 1, 0), Gem.AMETHYST, 4, .3);
                }
            }
            float r1 = wave(k, tickCount);
            if (r1 > 0) {
                roll(level, k, wave(k, tickCount - 1), r1);
            }
        }
    }

    /** The wave's front passes from {@code r0} to {@code r1}: what it meets there. */
    private void roll(ServerLevel level, int k, float r0, float r1) {
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        LivingEntity owner = getLivingOwner();
        Vec3 c = position();
        for (LivingEntity e : SpellUtil.enemiesAround(level, c.add(0, 1, 0), r1 + 1, owner, x -> Math.abs(x.getY() - c.y) < 4)) {
            double d = Math.sqrt(e.distanceToSqr(c.x, e.getY(), c.z));
            if (d >= r0 - .5 && d <= r1 + .5 && struck.add(e.getId())) {
                if (spell != null) {
                    e.invulnerableTime = 0;
                    DamageSources.applyDamage(e, damage * (.6f + .2f * k), SpellUtil.damageSource(spell, this, owner));
                }
                e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                GemFx.glitter(level, e.getBoundingBox().getCenter(), Gem.AMETHYST, 8, .3);
            }
        }
        // The note shatters the enemy shots it meets
        for (Projectile shot : level.getEntitiesOfClass(Projectile.class, new AABB(c, c).inflate(r1 + 1, 4, r1 + 1),
                p -> !(p instanceof GemShard) && !(p.getOwner() != null && SpellUtil.isAlly(owner, p.getOwner())))) {
            double d = Math.sqrt(shot.distanceToSqr(c.x, shot.getY(), c.z));
            if (d >= r0 - .8 && d <= r1 + .8) {
                GemFx.glitter(level, shot.position(), Gem.AMETHYST, 6, .2);
                shot.discard();
            }
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
