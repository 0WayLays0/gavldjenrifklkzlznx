package com.waylays.arcanes.entity.book;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEffects;
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

import javax.annotation.Nullable;

/**
 * The look of Seal of Silence on its victim: a violet wax seal drops onto it and stamps, then a glyph seals its mouth
 * and a ring of runes turns around its head for as long as it is silenced.
 */
public class SilenceSeal extends SpellEffectEntity {
    public static final int STAMP_TICK = 7;

    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(SilenceSeal.class, EntityDataSerializers.INT);
    /** Tick the seal started to break (0: still holding). */
    private static final EntityDataAccessor<Integer> DATA_BREAK = SynchedEntityData.defineId(SilenceSeal.class, EntityDataSerializers.INT);
    public static final int BREAK_TICKS = 6;

    public SilenceSeal(EntityType<? extends SilenceSeal> entityType, Level level) {
        super(entityType, level);
    }

    public SilenceSeal(Level level, LivingEntity owner, LivingEntity target) {
        this(ModEntities.SILENCE_SEAL.get(), level);
        setOwner(owner);
        this.entityData.set(DATA_TARGET, target.getId());
        moveTo(target.getX(), target.getY(), target.getZ());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_TARGET, -1);
        this.entityData.define(DATA_BREAK, 0);
    }

    @Nullable
    public LivingEntity getTarget() {
        return level().getEntity(this.entityData.get(DATA_TARGET)) instanceof LivingEntity living ? living : null;
    }

    /** Ticks since the seal started to break, or -1. */
    public float breakAge(float partialTicks) {
        int at = this.entityData.get(DATA_BREAK);
        return at == 0 ? -1 : tickCount + partialTicks - at;
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity target = getTarget();
        if (target != null) {
            setPos(target.getX(), target.getY(), target.getZ());
        }
        if (level().isClientSide) {
            if (target != null && tickCount > STAMP_TICK && breakAge(0) < 0 && random.nextInt(4) == 0) {
                level().addParticle(BookFx.VIOLET_DUST, target.getX() + (random.nextFloat() - .5f) * .8, target.getEyeY() + (random.nextFloat() - .5f) * .4,
                        target.getZ() + (random.nextFloat() - .5f) * .8, 0, 0, 0);
            }
            return;
        }
        int broken = this.entityData.get(DATA_BREAK);
        if (broken != 0) {
            if (tickCount - broken >= BREAK_TICKS) {
                discard();
            }
            return;
        }
        if (tickCount == STAMP_TICK && target != null) {
            double y = target.getEyeY();
            level().playSound(null, target.getX(), y, target.getZ(), SoundEvents.BOOK_PUT, SoundSource.PLAYERS, 1.5f, .6f);
            level().playSound(null, target.getX(), y, target.getZ(), SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, SoundSource.PLAYERS, 1f, 1.3f);
            SpellUtil.particles(level(), ParticleTypes.ENCHANT, target.getX(), y, target.getZ(), 30, .3, .3, .3, 1);
            SpellUtil.particles(level(), BookFx.VIOLET_DUST, target.getX(), y, target.getZ(), 16, .35, .25, .35, 0);
        }
        // Gone with the effect (time out, milk, death)
        if (tickCount > 2 && (target == null || !target.isAlive() || !target.hasEffect(ModEffects.SILENCE.get()))) {
            this.entityData.set(DATA_BREAK, tickCount);
            if (target != null) {
                level().playSound(null, target.getX(), target.getEyeY(), target.getZ(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1f, 1.4f);
                SpellUtil.particles(level(), ParticleTypes.ENCHANT, target.getX(), target.getEyeY(), target.getZ(), 20, .3, .2, .3, 1.2);
            }
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(2, 4, 2);
    }
}
