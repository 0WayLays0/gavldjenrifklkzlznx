package com.waylays.arcanes.entity.bone;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.bone.BoneFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Bone spikes bursting out of the ground, each a cluster of three leaning horns of bone, standing a moment, then
 * crumbling back:
 * <ul>
 * <li>LINE: one after another in a line running out from the caster (the Bone Spikes), taller the further they go.</li>
 * <li>ZONE: all over a circle, two a tick, for the zone's whole time (the Ossuary), the ground in it dragging at the
 * feet of those inside.</li>
 * </ul>
 * Whatever a spike bursts up under is impaled: hurt and thrown up. Both sides work out every spike from the entity.
 */
public class SpikeField extends SpellEffectEntity {
    public enum Mode { LINE, ZONE }

    /** Ticks a spike takes to burst up, and stands in all. */
    public static final int RISE = 3;
    public static final int SPIKE_LIFE = 26;
    public static final float LINE_STEP = .9f;
    public static final int ZONE_START = 10;
    /** Ticks an enemy in the zone is safe after a spike has impaled it. */
    public static final int ZONE_GRACE = 8;

    private static final EntityDataAccessor<Integer> DATA_MODE = SynchedEntityData.defineId(SpikeField.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(SpikeField.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_DURATION = SynchedEntityData.defineId(SpikeField.class, EntityDataSerializers.INT);

    /** One spike: where (from the entity), the tick it bursts up, how high, which way and how far it leans. */
    public record Spike(float x, float z, int start, float height, float leanX, float leanZ, int seed) {}

    private final Int2IntOpenHashMap lastHit = new Int2IntOpenHashMap();
    @Nullable
    private List<Spike> spikes;
    @Nullable
    private String spellId;

    public SpikeField(EntityType<? extends SpikeField> entityType, Level level) {
        super(entityType, level);
    }

    /** {@code size}: a line's length or a zone's radius; {@code duration}: a zone's time (a line takes as long as it runs). */
    public static SpikeField erupt(Level level, LivingEntity caster, AbstractSpell spell, Mode mode, Vec3 at, float yaw, float size, int duration, float damage) {
        SpikeField field = new SpikeField(ModEntities.SPIKE_FIELD.get(), level);
        field.setOwner(caster);
        field.spellId = spell.getSpellId();
        field.damage = damage;
        field.moveTo(at.x, at.y, at.z, yaw, 0);
        field.entityData.set(DATA_MODE, mode.ordinal());
        field.entityData.set(DATA_SIZE, size);
        field.entityData.set(DATA_DURATION, duration);
        level.addFreshEntity(field);
        return field;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_MODE, 0);
        builder.define(DATA_SIZE, 10f);
        builder.define(DATA_DURATION, 100);
    }

    public Mode getMode() {
        Mode[] all = Mode.values();
        return all[Mth.clamp(this.entityData.get(DATA_MODE), 0, all.length - 1)];
    }

    public float getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    public int getDuration() {
        return this.entityData.get(DATA_DURATION);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        spikes = null;
    }

    /** Every spike, worked out the same way on both sides. */
    public List<Spike> spikes() {
        if (spikes != null) {
            return spikes;
        }
        List<Spike> out = new ArrayList<>();
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        float fx = -Mth.sin(yaw), fz = Mth.cos(yaw);
        int seed = getId() * 7919;
        if (getMode() == Mode.LINE) {
            int count = Math.max(1, (int) (getSize() / LINE_STEP));
            for (int k = 0; k < count; k++) {
                float along = 1.4f + k * LINE_STEP;
                float across = (BoneFx.hash(seed + k) - .5f) * .8f;
                float t = (float) k / count;
                float height = 1.5f + 1.4f * t + .4f * BoneFx.hash(seed + k + 3);
                // Leaning forward, the way the line runs
                float lean = .35f + .25f * BoneFx.hash(seed + k + 5);
                out.add(new Spike(fx * along - fz * across, fz * along + fx * across, (int) (k * .7f), height,
                        fx * lean + (BoneFx.hash(seed + k + 7) - .5f) * .3f, fz * lean + (BoneFx.hash(seed + k + 9) - .5f) * .3f, seed + k * 31));
            }
        } else {
            int count = getDuration() * 2 / 3;
            float radius = getSize();
            for (int i = 0; i < count; i++) {
                float a = BoneFx.hash(seed + i * 3) * Mth.TWO_PI;
                float r = Mth.sqrt(BoneFx.hash(seed + i * 3 + 1)) * radius;
                float x = Mth.cos(a) * r, z = Mth.sin(a) * r;
                float out1 = .2f + .3f * BoneFx.hash(seed + i * 3 + 2);
                float len = Math.max(.01f, Mth.sqrt(x * x + z * z));
                out.add(new Spike(x, z, ZONE_START + i * 3 / 2, 1.3f + 1.1f * BoneFx.hash(seed + i + 77), x / len * out1, z / len * out1, seed + i * 31));
            }
        }
        spikes = out;
        return out;
    }

    /** The last tick anything happens. */
    public int end() {
        int last = 0;
        for (Spike s : spikes()) {
            last = Math.max(last, s.start());
        }
        return last + SPIKE_LIFE + 4;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > end()) {
            discard();
            return;
        }
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        LivingEntity caster = getLivingOwner();
        boolean zone = getMode() == Mode.ZONE;
        if (zone && tickCount % 10 == 0 && tickCount < end() - SPIKE_LIFE) {
            for (LivingEntity e : SpellUtil.enemiesAround(level, position(), getSize() + .5, caster, x -> true)) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, 1));
            }
        }
        int sounds = 0;
        for (Spike s : spikes()) {
            if (s.start() != tickCount) {
                continue;
            }
            Vec3 at = position().add(s.x(), 0, s.z());
            if (sounds++ < 2) {
                BoneFx.erupt(level, at, zone ? .7f : 1f);
            }
            BoneFx.splinters(level, at.add(0, .2, 0), 4, .3);
            if (spell == null) {
                continue;
            }
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(1.2, s.height() + 1, 1.2))) {
                double d = Math.hypot(e.getX() - at.x, e.getZ() - at.z);
                if (!e.isAlive() || SpellUtil.isAlly(caster, e) || d > .9 + e.getBbWidth() * .5 || e.getY() > at.y + s.height() || e.getY() < at.y - 1.5) {
                    continue;
                }
                if (zone && tickCount - lastHit.getOrDefault(e.getId(), -100) < ZONE_GRACE) {
                    continue;
                }
                lastHit.put(e.getId(), tickCount);
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, damage, SpellUtil.damageSource(spell, this, caster));
                double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
                e.setDeltaMovement(e.getDeltaMovement().multiply(.4, 0, .4).add(s.leanX() * .4 * resist, .55 * Math.max(.5, resist), s.leanZ() * .4 * resist));
                e.hurtMarked = true;
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
                BoneFx.marrow(level, e.getBoundingBox().getCenter(), 8, .3);
            }
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
