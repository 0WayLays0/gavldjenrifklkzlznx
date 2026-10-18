package com.waylays.arcanes.entity.book;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.book.BookFx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Bookmark's marker: a tall ribbon of violet silk floating upright over a rune circle, swaying, until its owner comes
 * back to it or its time runs out. Not saved: the spot itself is kept in the owner's recast.
 */
public class BookmarkMarker extends SpellEffectEntity {
    public static final int FADE_TICKS = 10;

    private static final EntityDataAccessor<Integer> DATA_LIFETIME = SynchedEntityData.defineId(BookmarkMarker.class, EntityDataSerializers.INT);
    /** Tick the marker started to fade (0: not fading). */
    private static final EntityDataAccessor<Integer> DATA_FADE = SynchedEntityData.defineId(BookmarkMarker.class, EntityDataSerializers.INT);

    public BookmarkMarker(EntityType<? extends BookmarkMarker> entityType, Level level) {
        super(entityType, level);
    }

    public BookmarkMarker(Level level, LivingEntity owner, int lifetime) {
        this(ModEntities.BOOKMARK_MARKER.get(), level);
        setOwner(owner);
        this.entityData.set(DATA_LIFETIME, lifetime);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_LIFETIME, 400);
        this.entityData.define(DATA_FADE, 0);
    }

    /** Ticks since the marker began to fade, or -1. */
    public float fadeAge(float partialTicks) {
        int fade = this.entityData.get(DATA_FADE);
        return fade == 0 ? -1 : tickCount + partialTicks - fade;
    }

    /** Server side: the owner left from here, or the time ran out. */
    public void fade() {
        if (this.entityData.get(DATA_FADE) == 0) {
            this.entityData.set(DATA_FADE, Math.max(1, tickCount));
            SpellUtil.particles(level(), BookFx.paper(), getX(), getY() + 1.2, getZ(), 14, .2, .6, .2, .08);
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.BOOK_PUT, SoundSource.PLAYERS, 1f, 1.2f);
        }
    }

    @Override
    public void tick() {
        super.tick();
        int fade = this.entityData.get(DATA_FADE);
        if (level().isClientSide) {
            if (fade == 0) {
                if (random.nextInt(3) == 0) {
                    level().addParticle(ParticleTypes.ENCHANT, getX() + (random.nextFloat() - .5f) * 1.4, getY() + .1, getZ() + (random.nextFloat() - .5f) * 1.4, 0, .6, 0);
                }
                if (random.nextInt(6) == 0) {
                    level().addParticle(BookFx.GILT_DUST, getX() + (random.nextFloat() - .5f) * .4, getY() + .3 + random.nextFloat() * 2, getZ() + (random.nextFloat() - .5f) * .4, 0, .02, 0);
                }
            }
            return;
        }
        if (fade == 0 && tickCount >= this.entityData.get(DATA_LIFETIME)) {
            fade();
        } else if (fade != 0 && tickCount - fade >= FADE_TICKS) {
            discard();
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(1.5, 3, 1.5);
    }
}
