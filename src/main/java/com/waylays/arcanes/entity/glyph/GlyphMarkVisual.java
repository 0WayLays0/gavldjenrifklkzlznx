package com.waylays.arcanes.entity.glyph;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.glyph.GlyphElement;
import com.waylays.arcanes.spells.glyph.GlyphReaction;
import com.waylays.arcanes.spells.glyph.GlyphReactions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The element glyph glowing over an inscribed creature's head, for as long as the mark lasts. When a glyph of another
 * element sets off a reaction, the second glyph swoops down onto the first and both burst; a creature frozen by the
 * Freeze reaction is shown in a block of ice while it lasts.
 */
public class GlyphMarkVisual extends SpellEffectEntity {
    public static final int REACT_TICKS = 14;

    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(GlyphMarkVisual.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ELEMENT = SynchedEntityData.defineId(GlyphMarkVisual.class, EntityDataSerializers.INT);
    /** The element that struck the mark, and the reaction it set off (-1: none yet). */
    private static final EntityDataAccessor<Integer> DATA_SECOND = SynchedEntityData.defineId(GlyphMarkVisual.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_REACTION = SynchedEntityData.defineId(GlyphMarkVisual.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_REACT_TICK = SynchedEntityData.defineId(GlyphMarkVisual.class, EntityDataSerializers.INT);
    /** Tick the mark was last written (it flares then). */
    private static final EntityDataAccessor<Integer> DATA_MARKED_TICK = SynchedEntityData.defineId(GlyphMarkVisual.class, EntityDataSerializers.INT);

    public GlyphMarkVisual(EntityType<? extends GlyphMarkVisual> entityType, Level level) {
        super(entityType, level);
    }

    /** Shows (or refreshes) the mark of {@code element} over {@code target}. */
    public static void show(ServerLevel level, LivingEntity target, GlyphElement element) {
        GlyphMarkVisual visual = find(level, target);
        if (visual == null) {
            visual = spawn(level, target, element);
        }
        visual.entityData.set(DATA_ELEMENT, element.ordinal());
        visual.entityData.set(DATA_MARKED_TICK, visual.tickCount);
    }

    /** Plays the reaction of {@code element} striking the mark of {@code inscribed} over {@code target}. */
    public static void react(ServerLevel level, LivingEntity target, GlyphElement inscribed, GlyphElement element, GlyphReaction reaction) {
        GlyphMarkVisual visual = find(level, target);
        if (visual == null) {
            visual = spawn(level, target, inscribed);
        }
        visual.entityData.set(DATA_ELEMENT, inscribed.ordinal());
        visual.entityData.set(DATA_SECOND, element.ordinal());
        visual.entityData.set(DATA_REACTION, reaction.ordinal());
        visual.entityData.set(DATA_REACT_TICK, visual.tickCount);
    }

    @Nullable
    private static GlyphMarkVisual find(ServerLevel level, LivingEntity target) {
        List<GlyphMarkVisual> found = level.getEntitiesOfClass(GlyphMarkVisual.class, target.getBoundingBox().inflate(3),
                v -> v.getTargetId() == target.getId() && !v.isReacting() && !v.isRemoved());
        return found.isEmpty() ? null : found.get(0);
    }

    private static GlyphMarkVisual spawn(ServerLevel level, LivingEntity target, GlyphElement element) {
        GlyphMarkVisual visual = new GlyphMarkVisual(ModEntities.GLYPH_MARK.get(), level);
        visual.entityData.set(DATA_TARGET, target.getId());
        visual.entityData.set(DATA_ELEMENT, element.ordinal());
        visual.moveTo(target.getX(), target.getY() + target.getBbHeight(), target.getZ(), 0, 0);
        level.addFreshEntity(visual);
        return visual;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_TARGET, -1);
        builder.define(DATA_ELEMENT, 0);
        builder.define(DATA_SECOND, -1);
        builder.define(DATA_REACTION, -1);
        builder.define(DATA_REACT_TICK, 0);
        builder.define(DATA_MARKED_TICK, 0);
    }

    public int getTargetId() {
        return this.entityData.get(DATA_TARGET);
    }

    @Nullable
    public Entity getTarget() {
        return level().getEntity(getTargetId());
    }

    public GlyphElement getElement() {
        return GlyphElement.byId(this.entityData.get(DATA_ELEMENT));
    }

    public boolean isReacting() {
        return this.entityData.get(DATA_REACTION) >= 0;
    }

    @Nullable
    public GlyphReaction getReaction() {
        int id = this.entityData.get(DATA_REACTION);
        return id < 0 ? null : GlyphReaction.byId(id);
    }

    @Nullable
    public GlyphElement getSecond() {
        int id = this.entityData.get(DATA_SECOND);
        return id < 0 ? null : GlyphElement.byId(id);
    }

    /** Ticks since the reaction went off (negative: none). */
    public float sinceReaction(float partialTicks) {
        return isReacting() ? tickCount + partialTicks - this.entityData.get(DATA_REACT_TICK) : -1;
    }

    public float sinceMarked(float partialTicks) {
        return tickCount + partialTicks - this.entityData.get(DATA_MARKED_TICK);
    }

    private int reactionLife() {
        return getReaction() == GlyphReaction.FREEZE ? Math.max(REACT_TICKS, GlyphReactions.FREEZE_TICKS) : REACT_TICKS;
    }

    @Override
    public void tick() {
        super.tick();
        Entity target = getTarget();
        if (target != null) {
            setPos(target.getX(), target.getY() + target.getBbHeight(), target.getZ());
        }
        if (level().isClientSide) {
            return;
        }
        if (isReacting()) {
            if (sinceReaction(0) > reactionLife() || target == null) {
                discard();
            }
        } else if (!(target instanceof LivingEntity living) || !living.isAlive() || !living.hasEffect(GlyphReactions.mark(getElement()))) {
            discard();
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 64 * 64;
    }
}
