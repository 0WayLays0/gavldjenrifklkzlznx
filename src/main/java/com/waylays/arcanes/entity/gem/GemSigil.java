package com.waylays.arcanes.entity.gem;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.gem.Gem;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** The incantation of Gem Magic: the jeweller's sigil (a cut stone's plan) burning on the ground in a gem's colour. */
public class GemSigil extends SpellEffectEntity {
    private static final EntityDataAccessor<Integer> DATA_GEM = SynchedEntityData.defineId(GemSigil.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(GemSigil.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(GemSigil.class, EntityDataSerializers.INT);

    public GemSigil(EntityType<? extends GemSigil> entityType, Level level) {
        super(entityType, level);
    }

    public static GemSigil burn(Level level, Vec3 at, Gem gem, float radius, int life) {
        GemSigil sigil = new GemSigil(ModEntities.GEM_SIGIL.get(), level);
        sigil.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 45, 0);
        sigil.entityData.set(DATA_GEM, gem.ordinal());
        sigil.entityData.set(DATA_RADIUS, radius);
        sigil.entityData.set(DATA_LIFE, life);
        level.addFreshEntity(sigil);
        return sigil;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_GEM, 0);
        builder.define(DATA_RADIUS, 2f);
        builder.define(DATA_LIFE, 30);
    }

    public Gem getGem() {
        return Gem.of(this.entityData.get(DATA_GEM));
    }

    public float getRadius() {
        return this.entityData.get(DATA_RADIUS);
    }

    /** How strongly it burns: drawn in, then dying out. */
    public float strength(float partialTicks) {
        float age = tickCount + partialTicks;
        int life = this.entityData.get(DATA_LIFE);
        return Mth.clamp(Math.min(age / 7, (life - age) / 10), 0, 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > this.entityData.get(DATA_LIFE)) {
            discard();
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
