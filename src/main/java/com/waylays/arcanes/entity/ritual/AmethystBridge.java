package com.waylays.arcanes.entity.ritual;

import com.waylays.arcanes.entity.wall.Masonry;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The Amethyst Bridge: from the ritual's figure a bridge of real amethyst grows out, slice by slice, to where the
 * leader looked, arching over what lies between: a deck of amethyst three blocks wide with a stripe of calcite down its
 * middle, crystals standing along its edges for a rail, each slice ringing as it forms. It stands for {@link #LIFE}
 * ticks, then shatters from its far end back, everything that was there before put back. Nothing it lays goes into a
 * build (see {@link Masonry}).
 */
public class AmethystBridge extends RitualEntity {
    public static final int LIFE = 20 * 60 * 4;
    public static final int MAX_LENGTH = 64;
    /** Slices laid (and later shattered) each tick. */
    public static final int RATE = 1;

    private static final EntityDataAccessor<Vector3f> DATA_END = SynchedEntityData.defineId(AmethystBridge.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> DATA_LAID = SynchedEntityData.defineId(AmethystBridge.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SHATTERED = SynchedEntityData.defineId(AmethystBridge.class, EntityDataSerializers.INT);

    private final Masonry masonry = new Masonry();
    /** The blocks of each slice, in the order laid. */
    private final List<List<BlockPos>> slices = new ArrayList<>();
    private boolean fromSave;

    public AmethystBridge(EntityType<? extends AmethystBridge> entityType, Level level) {
        super(entityType, level);
    }

    /** Grows a bridge from {@code from} (its first slice) to {@code to}. */
    public static AmethystBridge raise(ServerLevel level, Vec3 from, Vec3 to, float power) {
        AmethystBridge bridge = new AmethystBridge(ModEntities.AMETHYST_BRIDGE.get(), level);
        Vec3 d = to.subtract(from);
        if (d.horizontalDistance() > MAX_LENGTH) {
            to = from.add(d.scale(MAX_LENGTH / d.horizontalDistance()));
        }
        bridge.moveTo(from.x, from.y, from.z, 0, 0);
        // Its life runs on past its standing, for it to shatter slice by slice
        bridge.setup((int) (LIFE * Math.min(2, power)) + MAX_LENGTH / RATE + 40, 3, Set.of(), power);
        bridge.entityData.set(DATA_END, to.toVector3f());
        level.addFreshEntity(bridge);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 2, .6f);
        return bridge;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_END, new Vector3f());
        builder.define(DATA_LAID, 0);
        builder.define(DATA_SHATTERED, 0);
    }

    public Vec3 getEnd() {
        return new Vec3(this.entityData.get(DATA_END));
    }

    /** How many slices it has: its length in blocks. */
    public int length() {
        return Math.max(1, Mth.ceil(getEnd().subtract(position()).horizontalDistance()));
    }

    /** Slices laid so far, and shattered so far (from the far end). */
    public int laid() {
        return this.entityData.get(DATA_LAID);
    }

    public int shattered() {
        return this.entityData.get(DATA_SHATTERED);
    }

    /** How long it stands before it begins to shatter. */
    public int standing() {
        return getLife() - MAX_LENGTH / RATE - 40;
    }

    /** Where slice {@code i}'s middle is: along the line, arching up in the middle of a long span. */
    public Vec3 slice(float i) {
        Vec3 a = position(), b = getEnd();
        int n = length();
        float t = Mth.clamp(i / n, 0, 1);
        float arch = Math.min(6, n * .12f);
        Vec3 p = a.lerp(b, t);
        return p.add(0, arch * 4 * t * (1 - t), 0);
    }

    /** The way along it, flat. */
    public Vec3 along() {
        Vec3 d = getEnd().subtract(position()).multiply(1, 0, 1);
        return d.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : d.normalize();
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (fromSave) {
            // Loaded again after a stop: it does not outlive one, everything goes back as it was
            masonry.restore(level);
            discard();
            return;
        }
        int n = length();
        if (tickCount < standing()) {
            for (int k = 0; k < RATE && laid() < n; k++) {
                lay(level, laid());
                this.entityData.set(DATA_LAID, laid() + 1);
            }
        } else {
            for (int k = 0; k < RATE && shattered() < slices.size(); k++) {
                List<BlockPos> slice = slices.get(slices.size() - 1 - shattered());
                for (BlockPos pos : slice) {
                    if (masonry.takeUp(level, pos)) {
                        SpellUtil.particles(level, net.minecraft.core.particles.ParticleTypes.END_ROD, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 2, .3, .3, .3, .05);
                    }
                }
                Vec3 c = slice(slices.size() - 1 - shattered());
                level.playSound(null, c.x, c.y, c.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1, .7f + level.random.nextFloat() * .4f);
                this.entityData.set(DATA_SHATTERED, shattered() + 1);
            }
            if (shattered() >= slices.size()) {
                masonry.restore(level);
                discard();
            }
        }
    }

    /** Lays slice {@code i}: three blocks of deck, crystals standing on its edges every other slice. */
    private void lay(ServerLevel level, int i) {
        Vec3 c = slice(i + .5f);
        Vec3 f = along();
        Vec3 side = new Vec3(-f.z, 0, f.x);
        List<BlockPos> slice = new ArrayList<>();
        for (int k = -1; k <= 1; k++) {
            BlockPos pos = BlockPos.containing(c.add(side.scale(k)).subtract(0, .5, 0));
            BlockState deck = k == 0 ? Blocks.CALCITE.defaultBlockState() : Blocks.AMETHYST_BLOCK.defaultBlockState();
            if (masonry.lay(level, pos, deck, null, c)) {
                slice.add(pos);
            }
            if (k != 0 && i % 2 == 0) {
                BlockPos rail = pos.above();
                BlockState crystal = (i % 6 == 0 ? Blocks.AMETHYST_CLUSTER : Blocks.LARGE_AMETHYST_BUD).defaultBlockState()
                        .setValue(AmethystClusterBlock.FACING, Direction.UP);
                if (masonry.lay(level, rail, crystal, null, c)) {
                    slice.add(rail);
                }
            }
        }
        slices.add(slice);
        SpellUtil.particles(level, ParticleTypes.END_ROD, c.x, c.y, c.z, 3, .8, .2, .8, .02);
        if (i % 2 == 0) {
            level.playSound(null, c.x, c.y, c.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, .7f + .6f * i / Math.max(1, length()));
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy() && level() instanceof ServerLevel level) {
            masonry.restore(level);
        }
        super.remove(reason);
    }

    @Override
    public boolean shouldBeSaved() {
        return !isRemoved();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        masonry.save(tag);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (level() instanceof ServerLevel level) {
            masonry.load(tag, level);
        }
        fromSave = true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256 * 256;
    }
}
