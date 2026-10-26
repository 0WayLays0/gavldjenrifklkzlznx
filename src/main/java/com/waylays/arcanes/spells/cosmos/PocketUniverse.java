package com.waylays.arcanes.spells.cosmos;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.cosmos.TimeDoor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Pocket Universe, out of time: a dimension of deep space (see the dimension type {@code arcanes_oublies:pocket}
 * and its sky, client/render/PocketSkyEffects) where each caster owns a floating island of their own, far from
 * everyone else's. Time stands still there (the sun never moves, no weather, nothing spawns), the island grows with
 * the level the spell is cast at, and no one but its owner can break, place or use anything on it. Whoever falls off
 * is put back on their island. The way in is the spell's time door; the way out is the door standing on the island,
 * or the spell cast again.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public final class PocketUniverse {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, ArcanesOublies.id("pocket"));
    /** Islands lie this far apart along X (plot 1 at X = 2048, plot 2 at 4096...): no one ever sees another's. */
    public static final int SPACING = 2048;
    /** The height of an island's grass. */
    public static final int SURFACE = 100;
    /** Below this, someone has fallen off their island: they are put back on it. */
    public static final int VOID_Y = SURFACE - 70;
    public static final int MAX_LEVEL = 6;

    private PocketUniverse() {}

    /** The radius of the island at a spell level. */
    public static int radius(int level) {
        return 8 + 3 * Mth.clamp(level, 1, MAX_LEVEL);
    }

    public static BlockPos centre(int plot) {
        return new BlockPos(plot * SPACING, SURFACE, 0);
    }

    /** The plot a position is in (0: between plots, owned by no one). */
    public static int plotAt(BlockPos pos) {
        int plot = Math.round(pos.getX() / (float) SPACING);
        int dx = pos.getX() - plot * SPACING;
        return Math.abs(dx) < SPACING / 2 - 16 && Math.abs(pos.getZ()) < SPACING / 2 - 16 ? plot : 0;
    }

    /** Where someone arrives on an island: on the grass in front of its door. */
    public static Vec3 arrival(int plot) {
        BlockPos c = centre(plot);
        return new Vec3(c.getX() + .5, SURFACE + 1, c.getZ() + 3.5);
    }

    // ------------------------------------------------------------------ saved data

    /** Where a traveller came from, to be sent back there. */
    public record Return(ResourceKey<Level> dimension, Vec3 pos, float yRot, float xRot) {}

    /** Who owns which plot, how far each island has grown, and where everyone inside came from. */
    public static final class Plots extends SavedData {
        private final Map<UUID, Integer> plots = new HashMap<>();
        private final Map<UUID, Integer> grown = new HashMap<>();
        private final Map<UUID, Return> returns = new HashMap<>();
        private int next = 1;

        public int plotOf(UUID owner) {
            Integer plot = plots.get(owner);
            if (plot == null) {
                plot = next++;
                plots.put(owner, plot);
                setDirty();
            }
            return plot;
        }

        @Nullable
        public UUID ownerOf(int plot) {
            for (var e : plots.entrySet()) {
                if (e.getValue() == plot) {
                    return e.getKey();
                }
            }
            return null;
        }

        public boolean hasPlot(UUID owner) {
            return plots.containsKey(owner);
        }

        public int grown(UUID owner) {
            return grown.getOrDefault(owner, 0);
        }

        void setGrown(UUID owner, int level) {
            grown.put(owner, level);
            setDirty();
        }

        @Nullable
        public Return returnOf(UUID traveller) {
            return returns.get(traveller);
        }

        void setReturn(UUID traveller, @Nullable Return ret) {
            if (ret == null) {
                returns.remove(traveller);
            } else {
                returns.put(traveller, ret);
            }
            setDirty();
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putInt("Next", next);
            ListTag list = new ListTag();
            for (var e : plots.entrySet()) {
                CompoundTag p = new CompoundTag();
                p.putUUID("Owner", e.getKey());
                p.putInt("Plot", e.getValue());
                p.putInt("Grown", grown(e.getKey()));
                list.add(p);
            }
            tag.put("Plots", list);
            ListTag back = new ListTag();
            for (var e : returns.entrySet()) {
                CompoundTag r = new CompoundTag();
                r.putUUID("Traveller", e.getKey());
                r.putString("Dimension", e.getValue().dimension().location().toString());
                r.putDouble("X", e.getValue().pos().x);
                r.putDouble("Y", e.getValue().pos().y);
                r.putDouble("Z", e.getValue().pos().z);
                r.putFloat("YRot", e.getValue().yRot());
                r.putFloat("XRot", e.getValue().xRot());
                back.add(r);
            }
            tag.put("Returns", back);
            return tag;
        }

        static Plots load(CompoundTag tag, HolderLookup.Provider registries) {
            Plots plots = new Plots();
            plots.next = Math.max(1, tag.getInt("Next"));
            for (Tag t : tag.getList("Plots", Tag.TAG_COMPOUND)) {
                CompoundTag p = (CompoundTag) t;
                plots.plots.put(p.getUUID("Owner"), p.getInt("Plot"));
                plots.grown.put(p.getUUID("Owner"), p.getInt("Grown"));
                plots.next = Math.max(plots.next, p.getInt("Plot") + 1);
            }
            for (Tag t : tag.getList("Returns", Tag.TAG_COMPOUND)) {
                CompoundTag r = (CompoundTag) t;
                ResourceLocation dim = ResourceLocation.tryParse(r.getString("Dimension"));
                if (dim != null) {
                    plots.returns.put(r.getUUID("Traveller"), new Return(ResourceKey.create(Registries.DIMENSION, dim),
                            new Vec3(r.getDouble("X"), r.getDouble("Y"), r.getDouble("Z")), r.getFloat("YRot"), r.getFloat("XRot")));
                }
            }
            return plots;
        }
    }

    public static Plots plots(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Plots::new, Plots::load, null), "arcanes_oublies_pockets");
    }

    @Nullable
    public static ServerLevel level(MinecraftServer server) {
        return server.getLevel(DIMENSION);
    }

    // ------------------------------------------------------------------ travel

    /**
     * Takes {@code traveller} into {@code owner}'s pocket universe (the owner's own island, grown to {@code spellLevel}
     * first), remembering where they came from. Returns false if the pocket universe is missing.
     */
    public static boolean enter(ServerPlayer traveller, ServerPlayer owner, int spellLevel) {
        MinecraftServer server = traveller.server;
        ServerLevel pocket = level(server);
        if (pocket == null) {
            return false;
        }
        Plots plots = plots(server);
        int plot = plots.plotOf(owner.getUUID());
        grow(pocket, plots, owner.getUUID(), plot, spellLevel);
        if (traveller.level().dimension() != DIMENSION) {
            plots.setReturn(traveller.getUUID(), new Return(traveller.level().dimension(), traveller.position(), traveller.getYRot(), traveller.getXRot()));
        }
        Vec3 to = arrival(plot);
        traveller.teleportTo(pocket, to.x, to.y, to.z, 180, 0);
        traveller.resetFallDistance();
        CosmosFx.fold(pocket, to.add(0, 1, 0), 1, 1.2f);
        CosmosFx.time(pocket, to.add(0, 1, 0), 30, .6);
        return true;
    }

    /** Sends {@code traveller} back where they came from (or to the world's spawn if that place is gone). */
    public static void leave(ServerPlayer traveller) {
        MinecraftServer server = traveller.server;
        Plots plots = plots(server);
        Return ret = plots.returnOf(traveller.getUUID());
        plots.setReturn(traveller.getUUID(), null);
        ServerLevel level = ret == null ? null : server.getLevel(ret.dimension());
        Vec3 to;
        float yRot = traveller.getYRot(), xRot = traveller.getXRot();
        if (level == null || level.dimension() == DIMENSION) {
            level = server.overworld();
            BlockPos spawn = level.getSharedSpawnPos();
            to = new Vec3(spawn.getX() + .5, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ()), spawn.getZ() + .5);
        } else {
            to = ret.pos();
            yRot = ret.yRot();
            xRot = ret.xRot();
        }
        traveller.teleportTo(level, to.x, to.y, to.z, yRot, xRot);
        traveller.resetFallDistance();
        CosmosFx.fold(level, to.add(0, 1, 0), 1, 1.4f);
        CosmosFx.time(level, to.add(0, 1, 0), 30, .6);
    }

    public static boolean isIn(Player player) {
        return player.level().dimension() == DIMENSION;
    }

    // ------------------------------------------------------------------ the island

    /** Grows {@code owner}'s island on {@code plot} up to {@code level} (builds it the first time), and sets its door. */
    public static void grow(ServerLevel pocket, Plots plots, UUID owner, int plot, int level) {
        int from = plots.grown(owner);
        int to = Mth.clamp(level, 1, MAX_LEVEL);
        BlockPos c = centre(plot);
        if (to > from) {
            build(pocket, c, from, to, plot * 341L + 17);
            plots.setGrown(owner, to);
        }
        TimeDoor.standAtIsland(pocket, c);
    }

    /** How far the island reaches out at an angle: a ragged edge, the same every time for a plot. */
    private static double edge(int radius, double angle, long seed) {
        double wobble = .08 * Math.sin(angle * 3 + seed % 7) + .05 * Math.sin(angle * 5 + seed % 11) + .03 * Math.sin(angle * 9 + seed % 13);
        return radius * (.9 + wobble);
    }

    /** How deep the island goes under the grass at a distance {@code t} (0 centre, 1 edge) from its middle. */
    private static int depth(int radius, double t, int x, int z, long seed) {
        double bump = (Mth.sin((float) (x * .37 + seed)) + Mth.cos((float) (z * .41 + seed * .5))) * .12;
        return (int) Math.round(2 + radius * .95 * Math.pow(Math.max(0, 1 - t), 1.4) * (1 + bump));
    }

    /**
     * Builds the island round {@code c} out to the size of {@code to}, leaving whatever the island of {@code from}
     * already holds as it is: only empty blocks are filled, and only the new ring and the deeper rock under the old
     * island are added (what its owner dug out or built stays so).
     */
    public static void build(ServerLevel level, BlockPos c, int from, int to, long seed) {
        int r = radius(to);
        int r0 = from > 0 ? radius(from) : -1;
        RandomSource random = RandomSource.create(seed + to);
        for (int dx = -r - 3; dx <= r + 3; dx++) {
            for (int dz = -r - 3; dz <= r + 3; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                double angle = Math.atan2(dz, dx);
                double e = edge(r, angle, seed);
                if (dist > e) {
                    continue;
                }
                int depth = depth(r, dist / e, dx, dz, seed);
                int top = SURFACE;
                int keepAbove = Integer.MIN_VALUE;
                if (r0 > 0) {
                    double e0 = edge(r0, angle, seed);
                    if (dist <= e0) {
                        // Under the old island: only the deeper rock
                        keepAbove = SURFACE - depth(r0, dist / e0, dx, dz, seed);
                    }
                }
                for (int y = top - depth; y <= top; y++) {
                    if (keepAbove != Integer.MIN_VALUE && y >= keepAbove) {
                        break;
                    }
                    BlockPos pos = c.offset(dx, y - c.getY(), dz);
                    if (!level.getBlockState(pos).isAir()) {
                        continue;
                    }
                    level.setBlock(pos, layer(top - y, depth, random), Block.UPDATE_CLIENTS);
                }
            }
        }
        if (from == 0) {
            decorate(level, c, r, random, seed);
        }
    }

    private static BlockState layer(int below, int depth, RandomSource random) {
        if (below == 0) {
            return Blocks.GRASS_BLOCK.defaultBlockState();
        }
        if (below <= 2) {
            return Blocks.DIRT.defaultBlockState();
        }
        if (below >= depth - 1 && depth > 5) {
            return random.nextInt(4) == 0 ? Blocks.TUFF.defaultBlockState() : Blocks.DEEPSLATE.defaultBlockState();
        }
        int k = random.nextInt(14);
        return k == 0 ? Blocks.ANDESITE.defaultBlockState() : k == 1 ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.STONE.defaultBlockState();
    }

    /**
     * The island's look: a giant stopped clock set in the ground round its door (a face of dark stone, an orange rim,
     * glowing hour marks, its hands stopped a moment before midnight), lamp posts, flowers and grass, two trees, a
     * stream running off the edge into the void, amethyst hanging under the rock, and three islets floating round it.
     */
    private static void decorate(ServerLevel level, BlockPos c, int r, RandomSource random, long seed) {
        clockFloor(level, c);
        for (int k = 0; k < 4; k++) {
            double a = Mth.PI / 4 + k * Mth.HALF_PI;
            BlockPos post = c.offset((int) Math.round(Math.cos(a) * 6), 1, (int) Math.round(Math.sin(a) * 6));
            set(level, post, Blocks.DARK_OAK_FENCE.defaultBlockState());
            set(level, post.above(), Blocks.DARK_OAK_FENCE.defaultBlockState());
            set(level, post.above(2), Blocks.LANTERN.defaultBlockState());
        }
        stream(level, c, r, seed);
        for (int k = 0; k < 3; k++) {
            double a = (seed % 100) * .1 + k * Mth.TWO_PI / 3 + .4;
            int d = r + 7 + k * 2;
            islet(level, c.offset((int) (Math.cos(a) * d), -2 + k * 4, (int) (Math.sin(a) * d)), 2 + (k + (int) (seed % 2)) % 2, random);
        }
        decoratePlants(level, c, r, random, seed);
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    /** The stopped clock in the ground round the island's middle, 4 blocks across its face. */
    private static void clockFloor(ServerLevel level, BlockPos c) {
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > 4.6) {
                    continue;
                }
                BlockState state = d > 3.8 ? Blocks.ORANGE_TERRACOTTA.defaultBlockState()
                        : d < 1 ? Blocks.WAXED_COPPER_BLOCK.defaultBlockState() : Blocks.POLISHED_DEEPSLATE.defaultBlockState();
                set(level, c.offset(dx, 0, dz), state);
            }
        }
        // Hour marks that glow, twelve at the top (toward the door)
        for (int h = 0; h < 12; h++) {
            double a = h * Mth.TWO_PI / 12 - Mth.HALF_PI;
            set(level, c.offset((int) Math.round(Math.cos(a) * 3.3), 0, (int) Math.round(Math.sin(a) * 3.3)),
                    h == 0 ? Blocks.SHROOMLIGHT.defaultBlockState() : Blocks.OCHRE_FROGLIGHT.defaultBlockState());
        }
        // The hands, stopped a moment before midnight: both toward the door, the minute hand a touch to the left
        for (int k = 1; k <= 3; k++) {
            set(level, c.offset(0, 0, -k), Blocks.ORANGE_CONCRETE.defaultBlockState());
        }
        set(level, c.offset(-1, 0, -2), Blocks.ORANGE_CONCRETE.defaultBlockState());
    }

    /** A stream running from a spring to the island's edge, and off it into the void. */
    private static void stream(ServerLevel level, BlockPos c, int r, long seed) {
        double a = Mth.PI * .75 + (seed % 7) * .1;
        for (int d = r - 5; d <= r + 1; d++) {
            BlockPos pos = c.offset((int) Math.round(Math.cos(a) * d), 0, (int) Math.round(Math.sin(a) * d));
            BlockState here = level.getBlockState(pos);
            if (here.is(Blocks.GRASS_BLOCK) || here.is(Blocks.DIRT)) {
                set(level, pos.below(), Blocks.STONE.defaultBlockState());
                set(level, pos, Blocks.WATER.defaultBlockState());
            }
        }
    }

    /** A little floating island: a lens of rock with grass on top. */
    private static void islet(ServerLevel level, BlockPos at, int radius, RandomSource random) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > radius + .3) {
                    continue;
                }
                int depth = (int) Math.round((radius + 1.5) * (1 - d / (radius + 1)));
                for (int y = -depth; y <= 0; y++) {
                    BlockPos pos = at.offset(dx, y, dz);
                    if (level.getBlockState(pos).isAir()) {
                        set(level, pos, y == 0 ? Blocks.GRASS_BLOCK.defaultBlockState() : y == -depth && random.nextInt(3) == 0
                                ? Blocks.AMETHYST_BLOCK.defaultBlockState() : Blocks.STONE.defaultBlockState());
                    }
                }
            }
        }
    }

    /** Flowers and grass on top, two trees, amethyst hanging under the rock. */
    private static void decoratePlants(ServerLevel level, BlockPos c, int r, RandomSource random, long seed) {
        BlockState[] plants = {Blocks.SHORT_GRASS.defaultBlockState(), Blocks.SHORT_GRASS.defaultBlockState(), Blocks.CORNFLOWER.defaultBlockState(),
                Blocks.ALLIUM.defaultBlockState(), Blocks.OXEYE_DAISY.defaultBlockState(), Blocks.AZURE_BLUET.defaultBlockState()};
        for (int k = 0; k < r * 3; k++) {
            int dx = random.nextInt(2 * r) - r, dz = random.nextInt(2 * r) - r;
            if (dx * dx + dz * dz > (r - 2) * (r - 2) || dx * dx + dz * dz < 36) {
                continue;
            }
            BlockPos pos = c.offset(dx, 1, dz);
            if (level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK) && level.getBlockState(pos).isAir()) {
                level.setBlock(pos, plants[random.nextInt(plants.length)], Block.UPDATE_CLIENTS);
            }
        }
        var trees = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        for (int k = 0; k < 2; k++) {
            double a = (seed % 360) * Mth.DEG_TO_RAD + k * Mth.PI;
            BlockPos pos = c.offset((int) (Math.cos(a) * r * .55), 1, (int) (Math.sin(a) * r * .55));
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            trees.getHolder(k == 0 ? TreeFeatures.OAK : TreeFeatures.BIRCH)
                    .ifPresent(tree -> tree.value().place(level, level.getChunkSource().getGenerator(), random, pos));
        }
        for (int k = 0; k < r; k++) {
            int dx = random.nextInt(2 * r) - r, dz = random.nextInt(2 * r) - r;
            for (int y = SURFACE - 2; y > SURFACE - r - 4; y--) {
                BlockPos pos = c.offset(dx, y - c.getY(), dz);
                if (level.getBlockState(pos).isAir() && !level.getBlockState(pos.above()).isAir()) {
                    level.setBlock(pos, Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(net.minecraft.world.level.block.AmethystClusterBlock.FACING,
                            net.minecraft.core.Direction.DOWN), Block.UPDATE_CLIENTS);
                    break;
                }
            }
        }
    }

    // ------------------------------------------------------------------ protection

    /** Whether {@code player} may change the block at {@code pos} of the pocket universe: on their own plot only. */
    public static boolean canBuild(Player player, BlockPos pos) {
        if (player.isCreative() && player.hasPermissions(2)) {
            return true;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return true;
        }
        int plot = plotAt(pos);
        if (plot == 0) {
            return false;
        }
        Plots plots = plots(level.getServer());
        return plots.hasPlot(player.getUUID()) && plots.plotOf(player.getUUID()) == plot;
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == DIMENSION && !canBuild(event.getPlayer(), event.getPos())) {
            event.setCanceled(true);
            event.getPlayer().displayClientMessage(Component.translatable("ui.arcanes_oublies.pocket_protected"), true);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == DIMENSION
                && event.getEntity() instanceof Player player && !canBuild(player, event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == DIMENSION && !canBuild(event.getEntity(), event.getPos())) {
            event.setCanceled(true);
            event.getEntity().displayClientMessage(Component.translatable("ui.arcanes_oublies.pocket_protected"), true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel().dimension() == DIMENSION) {
            event.getAffectedBlocks().clear();
        }
    }

    /**
     * Whoever falls off an island is put back on the island they fell from (or their own, if they strayed between
     * plots); someone with no island to go back to is sent home.
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().dimension() != DIMENSION || player.getY() > VOID_Y) {
            return;
        }
        int plot = plotAt(player.blockPosition());
        Plots plots = plots(player.server);
        if (plot == 0 || plots.ownerOf(plot) == null) {
            if (!plots.hasPlot(player.getUUID())) {
                player.resetFallDistance();
                leave(player);
                return;
            }
            plot = plots.plotOf(player.getUUID());
        }
        Vec3 to = arrival(plot);
        player.teleportTo(player.serverLevel(), to.x, to.y, to.z, player.getYRot(), 0);
        player.resetFallDistance();
        player.setDeltaMovement(Vec3.ZERO);
        CosmosFx.fold(player.level(), to.add(0, 1, 0), .8f, 1.6f);
    }
}
