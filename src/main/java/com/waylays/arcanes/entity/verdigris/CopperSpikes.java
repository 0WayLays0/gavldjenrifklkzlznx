package com.waylays.arcanes.entity.verdigris;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.verdigris.VerdigrisFx;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Copper Spikes: a wedge of jagged copper spikes bursting out of the ground ahead of the caster, row after row, each
 * a little further; they impale what stands over them (hurt, thrown up, bleeding), stand a moment and sink back.
 */
public class CopperSpikes extends SpellEffectEntity {
    /** A spike: where it stands (relative to the origin, on the ground), its row, how tall, how it leans (degrees about X and Z). */
    public record Spike(Vec3 at, int row, float height, float tiltX, float tiltZ, int seed) {}

    public static final int ROW_TICKS = 2;
    public static final int RISE = 3;
    public static final int HOLD = 30;
    public static final int SINK = 10;
    public static final float SPACING = 1.1f;
    public static final int BLEED_TICKS = 60;

    private static final EntityDataAccessor<Integer> DATA_ROWS = SynchedEntityData.defineId(CopperSpikes.class, EntityDataSerializers.INT);

    private final IntOpenHashSet hit = new IntOpenHashSet();
    @Nullable
    private List<Spike> spikes;
    @Nullable
    private String spellId;

    public CopperSpikes(EntityType<? extends CopperSpikes> entityType, Level level) {
        super(entityType, level);
    }

    public static CopperSpikes burst(Level level, LivingEntity caster, AbstractSpell spell, Vec3 from, float yaw, int rows, float damage) {
        CopperSpikes spikes = new CopperSpikes(ModEntities.COPPER_SPIKES.get(), level);
        spikes.setOwner(caster);
        spikes.spellId = spell.getSpellId();
        spikes.damage = damage;
        spikes.moveTo(from.x, from.y, from.z, yaw, 0);
        spikes.entityData.set(DATA_ROWS, rows);
        level.addFreshEntity(spikes);
        return spikes;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ROWS, 8);
    }

    public int getRows() {
        return this.entityData.get(DATA_ROWS);
    }

    /** The spikes, laid out the same way on both sides (from the entity's id), standing on the ground where they are. */
    public List<Spike> spikes() {
        if (spikes == null) {
            List<Spike> out = new ArrayList<>();
            float yaw = getYRot() * Mth.DEG_TO_RAD;
            Vec3 f = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
            Vec3 side = new Vec3(-f.z, 0, f.x);
            int seed = getId() * 131;
            double lastY = getY();
            for (int r = 0; r < getRows(); r++) {
                double d = 1.6 + r * SPACING;
                // Odd counts, so a spike always stands on the middle line; at most five abreast
                int count = Math.min(5, 1 + 2 * (r / 2));
                double half = .35 * d;
                for (int k = 0; k < count; k++) {
                    int s = seed + r * 17 + k * 5;
                    double lateral = count == 1 ? 0 : (k / (count - 1.0) - .5) * 2 * half;
                    lateral += (VerdigrisFx.hash(s) - .5) * .6;
                    double along = d + (VerdigrisFx.hash(s + 1) - .5) * .5;
                    Vec3 p = position().add(f.scale(along)).add(side.scale(lateral));
                    Vec3 ground = SpellUtil.groundAt(level(), new Vec3(p.x, lastY, p.z), 3, 1);
                    // Each leans outward from the middle of the wedge, a little forward
                    float lean = (float) (lateral / Math.max(1, half)) * 18;
                    float fwd = 8 + VerdigrisFx.hash(s + 2) * 10;
                    float leanX = (float) (f.x * fwd + side.x * lean);
                    float leanZ = (float) (f.z * fwd + side.z * lean);
                    out.add(new Spike(ground.subtract(position()), r, 1.3f + VerdigrisFx.hash(s + 3) * 1.1f + r * .05f, leanZ, -leanX, s));
                }
                lastY = position().y + out.get(out.size() - 1).at().y;
            }
            spikes = out;
        }
        return spikes;
    }

    /** How far spike row {@code row} is out of the ground (0 to 1). */
    public float risen(int row, float partialTicks) {
        float age = tickCount + partialTicks - row * ROW_TICKS;
        float up = Mth.clamp(age / RISE, 0, 1);
        float down = Mth.clamp((age - RISE - HOLD) / SINK, 0, 1);
        return up * (1 + .25f * Mth.sin(up * Mth.PI)) * (1 - down * down);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        int life = getRows() * ROW_TICKS + RISE + HOLD + SINK;
        if (tickCount > life) {
            discard();
            return;
        }
        if (tickCount % ROW_TICKS != 0 || tickCount / ROW_TICKS >= getRows()) {
            return;
        }
        int row = tickCount / ROW_TICKS;
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        Entity owner = getOwner();
        ServerLevel level = (ServerLevel) level();
        for (Spike spike : spikes()) {
            if (spike.row() != row) {
                continue;
            }
            Vec3 at = position().add(spike.at());
            VerdigrisFx.debris(level, at.add(0, .3, 0), 5, .3);
            for (LivingEntity e : SpellUtil.enemiesAround(level, at.add(0, 1, 0), 2, owner,
                    x -> !hit.contains(x.getId()) && x.distanceToSqr(at.x, x.getY(), at.z) <= .9 * .9 + x.getBbWidth() * .5 && x.getY() > at.y - 1.5 && x.getY() < at.y + 2)) {
                hit.add(e.getId());
                if (spell != null) {
                    e.invulnerableTime = 0;
                    DamageSources.applyDamage(e, damage, SpellUtil.damageSource(spell, this, owner));
                }
                double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
                e.setDeltaMovement(e.getDeltaMovement().multiply(.5, 0, .5).add(0, .65 * resist, 0));
                e.hurtMarked = true;
                e.addEffect(new MobEffectInstance(ModEffects.BLEEDING, BLEED_TICKS, 0));
            }
        }
        VerdigrisFx.rise(level, position().add(new Vec3(-Mth.sin(getYRot() * Mth.DEG_TO_RAD), 0, Mth.cos(getYRot() * Mth.DEG_TO_RAD)).scale(1.6 + row * SPACING)), .6f);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}
