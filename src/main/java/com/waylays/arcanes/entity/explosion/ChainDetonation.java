package com.waylays.arcanes.entity.explosion;

import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.explosion.ChainDetonationSpell;
import com.waylays.arcanes.spells.explosion.ExplosionFx;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Invisible conductor of Chain Detonation: blows up its current target, then leaps (with a crackling arc) to the
 * nearest enemy not hit yet, every {@link #JUMP_TICKS} ticks, each blast bigger than the last.
 */
public class ChainDetonation extends Entity {
    public static final int JUMP_TICKS = 7;

    @Nullable
    private UUID ownerUUID;
    @Nullable
    private UUID targetUUID;
    private float damage;
    private int jumpsLeft;
    private int jump;
    private final Set<UUID> hit = new HashSet<>();

    public ChainDetonation(EntityType<? extends ChainDetonation> entityType, Level level) {
        super(entityType, level);
        noPhysics = true;
    }

    public ChainDetonation(Level level, LivingEntity owner, LivingEntity firstTarget, float damage, int jumps) {
        this(ModEntities.CHAIN_DETONATION.get(), level);
        this.ownerUUID = owner.getUUID();
        this.targetUUID = firstTarget.getUUID();
        this.damage = damage;
        this.jumpsLeft = jumps;
        moveTo(firstTarget.getX(), firstTarget.getY(), firstTarget.getZ());
    }

    @Nullable
    private Entity find(@Nullable UUID id) {
        return id != null && level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(id) : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (tickCount == 1) {
            ExplosionFx.whine(level(), position(), 1.6f);
        }
        if ((tickCount - 1) % JUMP_TICKS != JUMP_TICKS - 1) {
            return;
        }
        Entity owner = find(ownerUUID);
        if (!(find(targetUUID) instanceof LivingEntity target) || !target.isAlive()) {
            discard();
            return;
        }
        Vec3 at = target.getBoundingBox().getCenter();
        float growth = 1 + jump * .25f;
        ExplosionFx.detonate(level(), at, .9f * growth, 2f + jump * .3f, damage * (1 + .15f * jump), ModSpells.CHAIN_DETONATION.get(), this, owner);
        hit.add(target.getUUID());
        jump++;
        if (--jumpsLeft <= 0) {
            discard();
            return;
        }
        LivingEntity next = null;
        List<LivingEntity> candidates = SpellUtil.enemiesAround(level(), at, ChainDetonationSpell.JUMP_RANGE, owner, e -> !hit.contains(e.getUUID()));
        for (LivingEntity e : candidates) {
            if (next == null || e.distanceToSqr(at) < next.distanceToSqr(at)) {
                next = e;
            }
        }
        if (next == null) {
            discard();
            return;
        }
        Vec3 to = next.getBoundingBox().getCenter();
        arc(at, to);
        ExplosionFx.whine(level(), to, 1.4f + jump * .15f);
        targetUUID = next.getUUID();
        moveTo(next.getX(), next.getY(), next.getZ());
    }

    /** A jagged arc of sparks from one victim to the next. */
    private void arc(Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        int steps = Math.max(4, (int) (delta.length() * 3));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(delta.scale(i / (double) steps)).add((random.nextFloat() - .5f) * .3f, (random.nextFloat() - .5f) * .3f, (random.nextFloat() - .5f) * .3f);
            SpellUtil.particles(level(), i % 3 == 0 ? ExplosionFx.CORE_DUST : ExplosionFx.ARCANE_DUST, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            if (i % 4 == 0) {
                SpellUtil.particles(level(), ExplosionFx.arcaneSparks(), p.x, p.y, p.z, 1, 0, 0, 0, .1);
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
