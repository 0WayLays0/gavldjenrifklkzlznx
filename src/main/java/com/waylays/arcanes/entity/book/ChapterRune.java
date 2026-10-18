package com.waylays.arcanes.entity.book;

import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.book.BookFx;
import com.waylays.arcanes.spells.book.FinalChapterSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * A rune of The Final Chapter marked on one enemy: it hovers over the victim, glowing brighter and spinning faster
 * until its fuse runs out, then explodes on the victim (and, weaker, on the enemies right next to it).
 */
public class ChapterRune extends Entity {
    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(ChapterRune.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FUSE = SynchedEntityData.defineId(ChapterRune.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUUID;
    private float damage;

    public ChapterRune(EntityType<? extends ChapterRune> entityType, Level level) {
        super(entityType, level);
        noPhysics = true;
    }

    public ChapterRune(Level level, @Nullable LivingEntity owner, LivingEntity target, float damage, int fuse) {
        this(ModEntities.CHAPTER_RUNE.get(), level);
        this.ownerUUID = owner == null ? null : owner.getUUID();
        this.damage = damage;
        this.entityData.set(DATA_TARGET, target.getId());
        this.entityData.set(DATA_FUSE, fuse);
        moveTo(target.getX(), target.getY(), target.getZ(), random.nextFloat() * 360f, 0);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_TARGET, -1);
        this.entityData.define(DATA_FUSE, 40);
    }

    @Nullable
    public LivingEntity getTarget() {
        return level().getEntity(this.entityData.get(DATA_TARGET)) instanceof LivingEntity living ? living : null;
    }

    /** Ticks from the marking to the explosion. */
    public int getFuse() {
        return this.entityData.get(DATA_FUSE);
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity target = getTarget();
        if (target != null) {
            setPos(target.getX(), target.getY(), target.getZ());
        }
        if (level().isClientSide) {
            return;
        }
        if (target == null || !target.isAlive()) {
            SpellUtil.particles(level(), ParticleTypes.ENCHANT, getX(), getY() + 1, getZ(), 20, .3, .3, .3, 1);
            discard();
            return;
        }
        if (tickCount == 1) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.5f, 1.2f);
        }
        if (tickCount == getFuse() - 10) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.2f, 1.6f);
        }
        if (tickCount >= getFuse()) {
            explode(target);
        }
    }

    private void explode(LivingEntity target) {
        Entity owner = ownerUUID != null && level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(ownerUUID) : null;
        Vec3 at = target.getBoundingBox().getCenter();
        target.invulnerableTime = 0;
        DamageSources.applyDamage(target, damage, SpellUtil.damageSource(ModSpells.THE_FINAL_CHAPTER.get(), this, owner));
        // The blast also catches whoever stands right beside the victim, weaker
        for (LivingEntity e : SpellUtil.enemiesAround(level(), at, FinalChapterSpell.RUNE_SPLASH_RADIUS, owner, x -> x != target)) {
            DamageSources.applyDamage(e, damage * .4f, SpellUtil.damageSource(ModSpells.THE_FINAL_CHAPTER.get(), this, owner));
        }
        BookFx.runeBurst(level(), at, 1.5f);
        discard();
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(2, 4, 2);
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

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
