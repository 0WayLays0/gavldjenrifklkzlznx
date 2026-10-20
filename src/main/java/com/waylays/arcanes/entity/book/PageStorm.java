package com.waylays.arcanes.entity.book;

import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.book.BookFx;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Page Storm: a whirlwind of runic pages that follows its target and slashes it every {@link #CUT_INTERVAL} ticks.
 * The pages themselves are drawn by the renderer around the target (synced by id).
 */
public class PageStorm extends Entity implements AntiMagicSusceptible {
    public static final int CUT_INTERVAL = 8;
    public static final int GATHER_TICKS = 8;
    public static final int SCATTER_TICKS = 10;

    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(PageStorm.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PAGES = SynchedEntityData.defineId(PageStorm.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DURATION = SynchedEntityData.defineId(PageStorm.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUUID;
    private float damage;

    public PageStorm(EntityType<? extends PageStorm> entityType, Level level) {
        super(entityType, level);
        noPhysics = true;
    }

    public PageStorm(Level level, LivingEntity owner, LivingEntity target, float damage, int duration, int pages) {
        this(ModEntities.PAGE_STORM.get(), level);
        this.ownerUUID = owner.getUUID();
        this.damage = damage;
        this.entityData.set(DATA_TARGET, target.getId());
        this.entityData.set(DATA_DURATION, duration);
        this.entityData.set(DATA_PAGES, pages);
        moveTo(target.getX(), target.getY(), target.getZ());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_TARGET, -1);
        builder.define(DATA_PAGES, 10);
        builder.define(DATA_DURATION, 60);
    }

    @Nullable
    public LivingEntity getTarget() {
        return level().getEntity(this.entityData.get(DATA_TARGET)) instanceof LivingEntity living ? living : null;
    }

    public int getPages() {
        return this.entityData.get(DATA_PAGES);
    }

    public int getDuration() {
        return this.entityData.get(DATA_DURATION);
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity target = getTarget();
        if (target != null) {
            setPos(target.getX(), target.getY(), target.getZ());
        }
        if (level().isClientSide) {
            clientParticles(target);
            return;
        }
        if (target == null || !target.isAlive() || tickCount > getDuration() + SCATTER_TICKS) {
            scatter();
            return;
        }
        if (tickCount > getDuration()) {
            return;
        }
        if (tickCount == 1) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 2f, .7f);
        }
        if (tickCount % 6 == 0) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.2f, .9f + random.nextFloat() * .6f);
        }
        if (tickCount >= GATHER_TICKS && tickCount % CUT_INTERVAL == 0) {
            cut(target);
        }
    }

    private void cut(LivingEntity target) {
        Entity owner = ownerUUID != null && level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(ownerUUID) : null;
        // Each cut goes through hurt immunity: the storm is many small slashes, not one big hit
        target.invulnerableTime = 0;
        if (DamageSources.applyDamage(target, damage, SpellUtil.damageSource(ModSpells.PAGE_STORM.get(), this, owner))) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CUT_INTERVAL + 4, 1, false, false, false));
            Vec3 c = target.getBoundingBox().getCenter();
            SpellUtil.particles(level(), ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 1, .2, .2, .2, 0);
            SpellUtil.particles(level(), BookFx.paper(), c.x, c.y, c.z, 4, .3, .4, .3, .15);
            level().playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, .7f, 1.5f + random.nextFloat() * .3f);
        }
    }

    /** The storm blows itself out: pages fly off in every direction. */
    private void scatter() {
        Vec3 c = position().add(0, 1, 0);
        SpellUtil.particles(level(), BookFx.paper(), c.x, c.y, c.z, 20, .4, .6, .4, .3);
        SpellUtil.particles(level(), ParticleTypes.ENCHANT, c.x, c.y, c.z, 30, .3, .6, .3, 1);
        level().playSound(null, c.x, c.y, c.z, SoundEvents.BOOK_PUT, SoundSource.PLAYERS, 1.5f, .8f);
        discard();
    }

    private void clientParticles(@Nullable LivingEntity target) {
        if (target == null || tickCount > getDuration()) {
            return;
        }
        Level level = level();
        float r = target.getBbWidth() * .8f + .7f;
        double a = random.nextDouble() * Math.PI * 2;
        double y = target.getY() + random.nextDouble() * target.getBbHeight() * 1.2;
        // Glyphs and scraps of paper caught in the wind
        level.addParticle(ParticleTypes.ENCHANT, target.getX() + Math.cos(a) * r, y, target.getZ() + Math.sin(a) * r, -Math.sin(a) * .3, .05, Math.cos(a) * .3);
        if (tickCount % 3 == 0) {
            level.addParticle(BookFx.paper(), target.getX() + Math.cos(a) * r, y, target.getZ() + Math.sin(a) * r, -Math.sin(a) * .25, .04, Math.cos(a) * .25);
        }
        if (tickCount % 2 == 0) {
            level.addParticle(BookFx.VIOLET_DUST, target.getX() + Math.cos(a + 1) * r, y, target.getZ() + Math.sin(a + 1) * r, 0, 0, 0);
        }
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        discard();
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(3);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
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
