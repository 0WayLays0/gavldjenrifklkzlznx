package com.waylays.arcanes.entity.token;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.token.TokenFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Token Wave: a breaking wave of soul tokens surging out of the ground and rolling ahead in a band. Tokens ride up its
 * back, over the crest and down its curling lip, spraying off the front; at the end of its run it crashes and
 * scatters. Each enemy it rolls over is struck once and thrown up.
 */
public class TokenWave extends SpellEffectEntity {
    /** Its top speed (blocks per tick), reached smoothly over the first few ticks (SURGE). */
    public static final float SPEED = .7f;
    public static final float SURGE = 3;
    public static final float WIDTH = 3.2f;
    /** Ticks it takes to rise to its full height, and to crash at the end of its run. */
    public static final int RISE_TICKS = 7;
    public static final int FADE_TICKS = 12;

    private static final EntityDataAccessor<Float> DATA_RANGE = SynchedEntityData.defineId(TokenWave.class, EntityDataSerializers.FLOAT);

    private final IntOpenHashSet hit = new IntOpenHashSet();
    @Nullable
    private String spellId;

    public TokenWave(EntityType<? extends TokenWave> entityType, Level level) {
        super(entityType, level);
    }

    public static TokenWave roll(Level level, LivingEntity caster, AbstractSpell spell, Vec3 from, float yaw, float range, float damage) {
        TokenWave wave = new TokenWave(ModEntities.TOKEN_WAVE.get(), level);
        wave.setOwner(caster);
        wave.spellId = spell.getSpellId();
        wave.damage = damage;
        wave.moveTo(from.x, from.y, from.z, yaw, 0);
        wave.entityData.set(DATA_RANGE, range);
        level.addFreshEntity(wave);
        return wave;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RANGE, 10f);
    }

    public float getRange() {
        return this.entityData.get(DATA_RANGE);
    }

    /** How far it has rolled after {@code age} ticks: it surges off slowly and runs on at full speed. */
    private static float distance(float age) {
        return SPEED * (age - SURGE * (1 - (float) Math.exp(-age / SURGE)));
    }

    /** How far the crest has rolled. */
    public float front(float partialTicks) {
        return Math.min(getRange(), distance(tickCount + partialTicks));
    }

    /** Ticks it takes to roll its whole way. */
    public int duration() {
        int ticks = 0;
        while (distance(ticks) < getRange() && ticks < 200) {
            ticks++;
        }
        return ticks;
    }

    /** How far it has risen out of the ground (0 to 1). */
    public float grown(float partialTicks) {
        float t = Mth.clamp((tickCount + partialTicks) / RISE_TICKS, 0, 1);
        return t * t * (3 - 2 * t);
    }

    /** Fades as it breaks at the end. */
    public float strength(float partialTicks) {
        return Mth.clamp(1 - (tickCount + partialTicks - duration()) / FADE_TICKS, 0, 1);
    }

    public Vec3 forward() {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (tickCount > duration() + FADE_TICKS) {
            discard();
            return;
        }
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (spell == null || tickCount > duration()) {
            return;
        }
        float front = front(0);
        Vec3 f = forward();
        Vec3 side = new Vec3(-f.z, 0, f.x);
        Vec3 c = position();
        Entity owner = getOwner();
        float range = getRange();
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(range + 2, 3, range + 2))) {
            if (hit.contains(e.getId()) || !e.isAlive() || SpellUtil.isAlly(owner, e)) {
                continue;
            }
            Vec3 d = e.position().subtract(c);
            double along = d.dot(f);
            double across = Math.abs(d.dot(side));
            // Under its face, from halfway up its back to its curling lip (further ahead in the middle)
            double lip = front + .9 + .45 * (1 - Math.min(1, across * across / (WIDTH * WIDTH * .25)));
            if (along < front - 1.8 || along > lip || across > WIDTH * .5 + e.getBbWidth() * .5 || e.getY() < c.y - 1.5 || e.getY() > c.y + 2.5) {
                continue;
            }
            hit.add(e.getId());
            e.invulnerableTime = 0;
            DamageSources.applyDamage(e, damage, SpellUtil.damageSource(spell, this, owner));
            double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
            e.setDeltaMovement(e.getDeltaMovement().add(f.scale(.45 * resist)).add(0, .6 * resist, 0));
            e.hurtMarked = true;
            TokenFx.impact((ServerLevel) level(), TokenFx.chest(e), .6f);
        }
        if (tickCount % 3 == 0) {
            TokenFx.clink(level(), c.add(f.scale(front)), .5f);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}
