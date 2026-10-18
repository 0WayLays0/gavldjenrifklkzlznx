package com.waylays.arcanes.entity.book;

import com.waylays.arcanes.effect.ParchmentAegisEffect;
import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.book.BookFx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The pages of Parchment Aegis, drawn circling their bearer; the count is synced from the bearer's aegis. Visual only:
 * the rules live in {@link ParchmentAegisEffect}.
 */
public class AegisPages extends SpellEffectEntity {
    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(AegisPages.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PAGES = SynchedEntityData.defineId(AegisPages.class, EntityDataSerializers.INT);

    public AegisPages(EntityType<? extends AegisPages> entityType, Level level) {
        super(entityType, level);
    }

    private AegisPages(Level level, LivingEntity bearer) {
        this(ModEntities.AEGIS_PAGES.get(), level);
        setOwner(bearer);
        this.entityData.set(DATA_TARGET, bearer.getId());
        this.entityData.set(DATA_PAGES, ParchmentAegisEffect.pages(bearer));
        moveTo(bearer.getX(), bearer.getY(), bearer.getZ());
    }

    /** Server side: puts a ring of pages around {@code bearer}, replacing the old one. */
    public static void attach(LivingEntity bearer) {
        of(bearer).forEach(AegisPages::discard);
        bearer.level().addFreshEntity(new AegisPages(bearer.level(), bearer));
    }

    public static List<AegisPages> of(LivingEntity bearer) {
        return bearer.level().getEntitiesOfClass(AegisPages.class, bearer.getBoundingBox().inflate(2), p -> p.getTarget() == bearer);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_TARGET, -1);
        this.entityData.define(DATA_PAGES, 0);
    }

    @Nullable
    public LivingEntity getTarget() {
        return level().getEntity(this.entityData.get(DATA_TARGET)) instanceof LivingEntity living ? living : null;
    }

    public int getPages() {
        return this.entityData.get(DATA_PAGES);
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity bearer = getTarget();
        if (bearer != null) {
            setPos(bearer.getX(), bearer.getY(), bearer.getZ());
        }
        if (level().isClientSide) {
            return;
        }
        if (bearer == null || !bearer.isAlive() || !bearer.hasEffect(ModEffects.PARCHMENT_AEGIS.get())) {
            double y = getY() + (bearer == null ? 1 : bearer.getBbHeight() * .5);
            SpellUtil.particles(level(), BookFx.paper(), getX(), y, getZ(), 3 + 2 * getPages(), .5, .4, .5, .2);
            SpellUtil.particles(level(), ParticleTypes.ENCHANT, getX(), y, getZ(), 20, .4, .4, .4, 1);
            discard();
            return;
        }
        int pages = ParchmentAegisEffect.pages(bearer);
        if (pages != getPages()) {
            this.entityData.set(DATA_PAGES, pages);
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(3, 3, 3);
    }
}
