package com.waylays.arcanes.entity.book;

import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.book.BookFx;
import com.waylays.arcanes.spells.book.FinalChapterSpell;
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
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * The Final Chapter: a colossal grimoire floating above the area. It opens, its pages fly out to form a dome over
 * the area, every enemy under the dome is marked with a rune ({@link ChapterRune}) and the runes go off one after
 * the other; then the book slams shut. The entity sits at the book's spine; the dome is centred on the ground below.
 */
public class FinalChapter extends Entity {
    public static final int APPEAR_TICKS = 10;
    public static final int OPEN_TICKS = 28;
    public static final int PAGES_START = 22;
    public static final int DOME_PAGES = 64;
    /** Ticks between two pages leaving the book. */
    public static final float PAGE_INTERVAL = .5f;
    public static final int FLY_TICKS = 16;
    public static final int MARK_TICK = 50;
    public static final int DETONATE_TICK = 90;
    public static final int RUNE_INTERVAL = 3;
    public static final int CLOSE_TICK = 120;
    public static final int SLAM_TICKS = 5;
    public static final int END_TICK = 145;

    private static final EntityDataAccessor<Float> DATA_HEIGHT = SynchedEntityData.defineId(FinalChapter.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUUID;
    private float damage;

    public FinalChapter(EntityType<? extends FinalChapter> entityType, Level level) {
        super(entityType, level);
        noPhysics = true;
    }

    public FinalChapter(Level level, LivingEntity owner, float damage, float height) {
        this(ModEntities.FINAL_CHAPTER.get(), level);
        this.ownerUUID = owner.getUUID();
        this.damage = damage;
        this.entityData.set(DATA_HEIGHT, height);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_HEIGHT, 8f);
    }

    /** Height of the book above the ground (the centre of the dome). */
    public float getHeight() {
        return this.entityData.get(DATA_HEIGHT);
    }

    public Vec3 groundCenter() {
        return position().subtract(0, getHeight(), 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientParticles();
            return;
        }
        Vec3 c = position();
        if (tickCount == 1) {
            level().playSound(null, c.x, c.y, c.z, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 4f, .5f);
            level().playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 3f, .6f);
        }
        if (tickCount == APPEAR_TICKS) {
            level().playSound(null, c.x, c.y, c.z, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 6f, .35f);
            level().playSound(null, c.x, c.y, c.z, SoundEvents.CHEST_OPEN, SoundSource.PLAYERS, 3f, .5f);
        }
        if (tickCount >= PAGES_START && tickCount < PAGES_START + DOME_PAGES * PAGE_INTERVAL && tickCount % 3 == 0) {
            level().playSound(null, c.x, c.y, c.z, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 3f, .7f + random.nextFloat() * .6f);
        }
        if (tickCount == MARK_TICK) {
            mark();
        }
        if (tickCount == CLOSE_TICK) {
            level().playSound(null, c.x, c.y, c.z, SoundEvents.BOOK_PUT, SoundSource.PLAYERS, 6f, .3f);
        }
        if (tickCount == CLOSE_TICK + SLAM_TICKS) {
            slam(c);
        }
        if (tickCount >= END_TICK) {
            discard();
        }
    }

    /** Every enemy under the dome gets a rune; the nearest ones go off first. */
    private void mark() {
        Entity owner = ownerUUID != null && level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(ownerUUID) : null;
        Vec3 ground = groundCenter();
        float radius = FinalChapterSpell.RADIUS;
        List<LivingEntity> marked = SpellUtil.enemiesAround(level(), ground.add(0, radius * .5, 0), radius * 1.2, owner, e -> {
            double dx = e.getX() - ground.x;
            double dz = e.getZ() - ground.z;
            return dx * dx + dz * dz <= radius * radius && e.getY() > ground.y - 3 && e.getY() < ground.y + radius;
        });
        marked.sort(Comparator.comparingDouble(e -> e.distanceToSqr(ground)));
        for (int i = 0; i < marked.size(); i++) {
            int fuse = DETONATE_TICK - MARK_TICK + i * RUNE_INTERVAL;
            level().addFreshEntity(new ChapterRune(level(), owner instanceof LivingEntity living ? living : null, marked.get(i), damage, fuse));
        }
        Vec3 c = position();
        level().playSound(null, c.x, c.y, c.z, SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.PLAYERS, 4f, .6f);
        level().playSound(null, c.x, c.y, c.z, SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, SoundSource.PLAYERS, 3f, .7f);
    }

    /** The book slams shut: a boom, a shudder, and the dome of pages blows away. */
    private void slam(Vec3 c) {
        level().playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 4f, .4f);
        level().playSound(null, c.x, c.y, c.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 3f, .3f);
        level().playSound(null, c.x, c.y, c.z, SoundEvents.BOOK_PUT, SoundSource.PLAYERS, 6f, .5f);
        SpellUtil.particles(level(), BookFx.paper(), c.x, c.y - 1, c.z, 60, 2, 1, 2, .5);
        SpellUtil.particles(level(), ParticleTypes.ENCHANT, c.x, c.y - 1, c.z, 120, 2, 1, 2, 2);
        SpellUtil.cameraShake(level(), groundCenter(), 20, FinalChapterSpell.RADIUS + 16);
    }

    private void clientParticles() {
        Level level = level();
        Vec3 c = position();
        if (tickCount > APPEAR_TICKS && tickCount < CLOSE_TICK) {
            // Glyphs drifting down from the open pages
            for (int i = 0; i < 3; i++) {
                level.addParticle(ParticleTypes.ENCHANT, c.x + (random.nextFloat() - .5f) * 6, c.y - 2 - random.nextFloat() * 2, c.z + (random.nextFloat() - .5f) * 5,
                        0, -1.5, 0);
            }
            if (random.nextInt(3) == 0) {
                level.addParticle(BookFx.GILT_DUST, c.x + (random.nextFloat() - .5f) * 7, c.y - 3, c.z + (random.nextFloat() - .5f) * 6, 0, -.05, 0);
            }
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(FinalChapterSpell.RADIUS + 4, getHeight() + 4, FinalChapterSpell.RADIUS + 4);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
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
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", tickCount);
        tag.putFloat("Damage", damage);
        tag.putFloat("Height", getHeight());
        if (ownerUUID != null) {
            tag.putUUID("Owner", ownerUUID);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        tickCount = tag.getInt("Age");
        damage = tag.getFloat("Damage");
        this.entityData.set(DATA_HEIGHT, tag.getFloat("Height"));
        if (tag.hasUUID("Owner")) {
            ownerUUID = tag.getUUID("Owner");
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
