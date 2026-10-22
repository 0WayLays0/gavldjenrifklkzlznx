package com.waylays.arcanes.entity.wall;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.wall.WallFx;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * A fortification of Wall Magic built of real blocks (stone, mossy and cracked stone bricks): it is solid, shots break
 * on it and nobody walks through. It builds itself brick by brick, each block flying up out of the ground into its
 * place in a ripple, its mortar glowing like embers; when its time is up it crumbles and the blocks are taken away
 * (see {@link Masonry}: only into air, never into a build).
 * <ul>
 * <li>WALL: a crenellated wall, {@code size} blocks wide and {@code height} high, facing one of the four ways.</li>
 * <li>CAGE: the Oubliette, a closed cell of bricks round one creature ({@code 2 size - 1} blocks inside, a ring and
 * a lid).</li>
 * <li>BASTION: a square of crenellated walls {@code 2 size + 1} blocks across, a taller tower at each corner.</li>
 * <li>RING: the round wall of the Tyrant's Fortress, {@code size} blocks out.</li>
 * </ul>
 */
public class Rampart extends SpellEffectEntity {
    public enum Shape { WALL, CAGE, BASTION, RING }

    /** A block of the rampart: where (from the foot), when it rises, which way its outer face looks (0 to 3, or -1). */
    public record Cell(int dx, int dy, int dz, int delay, int face) {}

    /** Ticks a block takes to fly up into place, and for the rampart to crumble. */
    public static final int CELL_RISE = 5;
    public static final int CRUMBLE = 18;

    private static final EntityDataAccessor<Integer> DATA_SHAPE = SynchedEntityData.defineId(Rampart.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SIZE = SynchedEntityData.defineId(Rampart.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HEIGHT = SynchedEntityData.defineId(Rampart.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FACING = SynchedEntityData.defineId(Rampart.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(Rampart.class, EntityDataSerializers.INT);

    private final Masonry masonry = new Masonry();
    @Nullable
    private List<Cell> cells;
    private int laidUpTo;
    private boolean crumbled;

    public Rampart(EntityType<? extends Rampart> entityType, Level level) {
        super(entityType, level);
    }

    /** Raises a rampart whose foot is the block at {@code at}; {@code yaw} picks which of the four ways it faces. */
    public static Rampart raise(Level level, @Nullable LivingEntity caster, Shape shape, Vec3 at, float yaw, int size, int height, int life) {
        Rampart rampart = new Rampart(ModEntities.RAMPART.get(), level);
        rampart.setOwner(caster);
        BlockPos foot = BlockPos.containing(at);
        rampart.moveTo(foot.getX() + .5, foot.getY(), foot.getZ() + .5, 0, 0);
        rampart.entityData.set(DATA_SHAPE, shape.ordinal());
        rampart.entityData.set(DATA_SIZE, size);
        rampart.entityData.set(DATA_HEIGHT, height);
        rampart.entityData.set(DATA_FACING, Math.floorMod(Math.round(yaw / 90), 4));
        rampart.entityData.set(DATA_LIFE, life);
        level.addFreshEntity(rampart);
        WallFx.rise(level, at, 1.6f);
        return rampart;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SHAPE, 0);
        builder.define(DATA_SIZE, 5);
        builder.define(DATA_HEIGHT, 3);
        builder.define(DATA_FACING, 0);
        builder.define(DATA_LIFE, 200);
    }

    public Shape getShape() {
        Shape[] all = Shape.values();
        return all[Mth.clamp(this.entityData.get(DATA_SHAPE), 0, all.length - 1)];
    }

    public int getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key != DATA_LIFE) {
            cells = null;
        }
    }

    /** Ends it early: it crumbles from now. */
    public void crumbleNow() {
        if (tickCount < getLife()) {
            this.entityData.set(DATA_LIFE, tickCount);
        }
    }

    /** The way a facing (0 to 3) looks: south, west, north, east, as a yaw does. */
    public static int[] step(int facing) {
        return switch (Math.floorMod(facing, 4)) {
            case 0 -> new int[]{0, 1};
            case 1 -> new int[]{-1, 0};
            case 2 -> new int[]{0, -1};
            default -> new int[]{1, 0};
        };
    }

    /** The facing (0 to 3) a horizontal offset points along. */
    private static int faceOf(int dx, int dz) {
        if (Math.abs(dz) >= Math.abs(dx)) {
            return dz >= 0 ? 0 : 2;
        }
        return dx < 0 ? 1 : 3;
    }

    /** Its blocks, in the order they rise (the same on both sides). */
    public List<Cell> cells() {
        if (cells != null) {
            return cells;
        }
        List<Cell> out = new ArrayList<>();
        int size = this.entityData.get(DATA_SIZE);
        int h = this.entityData.get(DATA_HEIGHT);
        switch (getShape()) {
            case WALL -> {
                int facing = this.entityData.get(DATA_FACING);
                int[] f = step(facing);
                int sx = -f[1], sz = f[0];
                int half = size / 2;
                for (int c = -half; c <= half; c++) {
                    for (int r = 0; r < h; r++) {
                        out.add(new Cell(sx * c, r, sz * c, Math.abs(c) * 2 + r * 3, facing));
                    }
                    if (Math.floorMod(c + half, 2) == 0) {
                        out.add(new Cell(sx * c, h, sz * c, Math.abs(c) * 2 + h * 3, facing));
                    }
                }
            }
            case CAGE -> {
                // A ring round the cell, course after course, running round; then the lid closes in from the rim
                List<int[]> rim = square(size);
                for (int r = 0; r < h; r++) {
                    for (int i = 0; i < rim.size(); i++) {
                        int dx = rim.get(i)[0], dz = rim.get(i)[1];
                        out.add(new Cell(dx, r, dz, i * 8 / rim.size() + r * 3, faceOf(dx, dz)));
                    }
                }
                for (int dx = -size; dx <= size; dx++) {
                    for (int dz = -size; dz <= size; dz++) {
                        int in = size - Math.max(Math.abs(dx), Math.abs(dz));
                        out.add(new Cell(dx, h, dz, h * 3 + 6 + in * 3, -1));
                    }
                }
            }
            case BASTION -> {
                List<int[]> rim = square(size);
                for (int i = 0; i < rim.size(); i++) {
                    int dx = rim.get(i)[0], dz = rim.get(i)[1];
                    boolean corner = Math.abs(dx) == size && Math.abs(dz) == size;
                    int delay = i * 28 / rim.size();
                    int top = corner ? h + 2 : (i % 2 == 0 ? h + 1 : h);
                    for (int r = 0; r < top; r++) {
                        out.add(new Cell(dx, r, dz, delay + r * 3, faceOf(dx, dz)));
                    }
                }
            }
            case RING -> {
                List<int[]> rim = circle(size);
                for (int[] p : rim) {
                    float a = (float) Math.atan2(p[1], p[0]);
                    int delay = (int) ((a + Math.PI) / (2 * Math.PI) * 40);
                    int top = Math.floorMod(p[0] + p[1], 2) == 0 ? h + 1 : h;
                    for (int r = 0; r < top; r++) {
                        out.add(new Cell(p[0], r, p[1], delay + r * 3, faceOf(p[0], p[1])));
                    }
                }
            }
        }
        out.sort((a, b) -> Integer.compare(a.delay(), b.delay()));
        cells = out;
        return out;
    }

    /** The border of a square {@code 2 r + 1} across, walked round in order. */
    private static List<int[]> square(int r) {
        List<int[]> out = new ArrayList<>();
        for (int x = -r; x < r; x++) out.add(new int[]{x, -r});
        for (int z = -r; z < r; z++) out.add(new int[]{r, z});
        for (int x = r; x > -r; x--) out.add(new int[]{x, r});
        for (int z = r; z > -r; z--) out.add(new int[]{-r, z});
        return out;
    }

    /** The blocks of a ring {@code r} out, one block thick. */
    private static List<int[]> circle(int r) {
        List<int[]> out = new ArrayList<>();
        for (int x = -r - 1; x <= r + 1; x++) {
            for (int z = -r - 1; z <= r + 1; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (Math.abs(d - r) < .5) {
                    out.add(new int[]{x, z});
                }
            }
        }
        return out;
    }

    /** Ticks until every block has risen. */
    public int built() {
        List<Cell> all = cells();
        return all.isEmpty() ? 0 : all.get(all.size() - 1).delay() + CELL_RISE;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        List<Cell> all = cells();
        BlockPos foot = blockPosition();
        Entity caster = getOwner();
        boolean placed = false;
        while (laidUpTo < all.size() && tickCount < getLife() && tickCount >= all.get(laidUpTo).delay() + CELL_RISE) {
            Cell c = all.get(laidUpTo++);
            BlockPos pos = foot.offset(c.dx(), c.dy(), c.dz());
            placed |= masonry.lay(level, pos, WallFx.brick((int) pos.asLong()), caster, position());
        }
        if (placed && tickCount % 2 == 0) {
            WallFx.sound(level, position(), SoundEvents.STONE_PLACE, 1.2f, .6f + random.nextFloat() * .2f);
        }
        if (tickCount >= getLife() && !crumbled) {
            crumbled = true;
            for (Masonry.Laid l : masonry.laid()) {
                if (random.nextInt(3) == 0) {
                    WallFx.dust(level, Vec3.atCenterOf(l.pos()), 3, .3);
                }
            }
            masonry.restore(level);
            WallFx.sound(level, position(), SoundEvents.STONE_BREAK, 1.6f, .5f);
            WallFx.sound(level, position(), SoundEvents.GRAVEL_BREAK, 1.4f, .6f);
        }
        if (tickCount > getLife() + CRUMBLE) {
            discard();
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
        tag.putInt("Shape", this.entityData.get(DATA_SHAPE));
        tag.putInt("Size", this.entityData.get(DATA_SIZE));
        tag.putInt("Height", this.entityData.get(DATA_HEIGHT));
        tag.putInt("Facing", this.entityData.get(DATA_FACING));
        tag.putInt("Life", getLife());
        masonry.save(tag);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_SHAPE, tag.getInt("Shape"));
        this.entityData.set(DATA_SIZE, tag.getInt("Size"));
        this.entityData.set(DATA_HEIGHT, tag.getInt("Height"));
        this.entityData.set(DATA_FACING, tag.getInt("Facing"));
        this.entityData.set(DATA_LIFE, tag.getInt("Life"));
        if (level() instanceof ServerLevel level) {
            masonry.load(tag, level);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
    }
}
