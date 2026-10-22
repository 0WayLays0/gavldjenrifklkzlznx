package com.waylays.arcanes.entity.ritual;

import com.waylays.arcanes.entity.SpellEffectEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Base of what a ritual leaves in the world: it lasts {@link #getLife()} ticks, has a size, a strength (more with a
 * chorus) and spares the ritual's participants.
 */
public abstract class RitualEntity extends SpellEffectEntity {
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(RitualEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(RitualEntity.class, EntityDataSerializers.FLOAT);

    protected final Set<UUID> allies = new HashSet<>();
    protected float power = 1;

    protected RitualEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LIFE, 200);
        builder.define(DATA_SIZE, 8f);
    }

    protected void setup(int life, float size, Set<UUID> allies, float power) {
        this.entityData.set(DATA_LIFE, life);
        this.entityData.set(DATA_SIZE, size);
        this.allies.addAll(allies);
        this.power = power;
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    public float getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    /** Whether the ritual turns against {@code e}: hostile creatures, never its participants nor other players. */
    protected boolean foe(Entity e) {
        return e instanceof LivingEntity living && living.isAlive() && e instanceof Enemy && !(e instanceof Player) && !allies.contains(e.getUUID());
    }

    /** Fading in over the first {@code in} ticks and out over the last {@code out}. */
    public float fade(float partialTicks, int in, int out) {
        float age = tickCount + partialTicks;
        return Math.max(0, Math.min(1, Math.min(age / in, (getLife() - age) / out)));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > getLife()) {
            discard();
        }
    }
}
