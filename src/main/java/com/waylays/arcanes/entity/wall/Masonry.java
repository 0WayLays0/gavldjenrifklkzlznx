package com.waylays.arcanes.entity.wall;

import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Real blocks laid by Wall Magic, and taken away again. A block only goes where there is air or something that can be
 * replaced (grass, snow, water...), never into a build, and never where the caster could not build themselves; what was
 * there before is put back when the spell ends. The record is saved with the spell's entity, so a server stop in the
 * middle does not leave walls behind: they come down when the entity is loaded again. A laid block broken by a player
 * gives nothing: it crumbles back into what was there (spells are no brick quarry).
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public final class Masonry {
    /** One laid block: where, what it is, what was there. */
    public record Laid(BlockPos pos, BlockState state, BlockState before) {}

    /** Every block laid and not yet taken away, by dimension: what was there before it. */
    private static final Map<ResourceKey<Level>, Map<BlockPos, BlockState>> LIVE = new HashMap<>();

    private final List<Laid> laid = new ArrayList<>();

    private static Map<BlockPos, BlockState> live(ServerLevel level) {
        return LIVE.computeIfAbsent(level.dimension(), k -> new HashMap<>());
    }

    public List<Laid> laid() {
        return laid;
    }

    /**
     * Lays {@code state} at {@code pos} if it may go there, pushing out of the way whoever stands in it (away from
     * {@code from}). Returns whether it was laid.
     */
    public boolean lay(ServerLevel level, BlockPos pos, BlockState state, @Nullable Entity caster, Vec3 from) {
        BlockState before = level.getBlockState(pos);
        if (!before.canBeReplaced() || before.hasBlockEntity() || level.isOutsideBuildHeight(pos)) {
            return false;
        }
        if (caster instanceof Player player && !level.mayInteract(player, pos)) {
            return false;
        }
        AABB cell = new AABB(pos);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, cell)) {
            Vec3 away = e.position().subtract(from).multiply(1, 0, 1);
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
            e.setPos(e.getX() + away.x * .8, e.getY(), e.getZ() + away.z * .8);
            e.setDeltaMovement(away.scale(.4).add(0, .2, 0));
            e.hurtMarked = true;
        }
        level.setBlock(pos, state, Block.UPDATE_ALL);
        laid.add(new Laid(pos.immutable(), state, before));
        live(level).put(pos.immutable(), before);
        return true;
    }

    /** Takes every laid block away, putting back what was there (unless someone changed the block since). */
    public void restore(ServerLevel level) {
        Map<BlockPos, BlockState> live = live(level);
        for (Laid l : laid) {
            live.remove(l.pos());
            if (level.isLoaded(l.pos()) && level.getBlockState(l.pos()) == l.state()) {
                level.setBlock(l.pos(), l.before(), Block.UPDATE_ALL);
            }
        }
        laid.clear();
    }

    public void save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Laid l : laid) {
            CompoundTag t = new CompoundTag();
            t.putLong("Pos", l.pos().asLong());
            t.put("State", NbtUtils.writeBlockState(l.state()));
            t.put("Before", NbtUtils.writeBlockState(l.before()));
            list.add(t);
        }
        tag.put("Laid", list);
    }

    public void load(CompoundTag tag, ServerLevel level) {
        laid.clear();
        var blocks = level.registryAccess().lookupOrThrow(Registries.BLOCK);
        for (Tag t : tag.getList("Laid", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            Laid l = new Laid(BlockPos.of(c.getLong("Pos")), NbtUtils.readBlockState(blocks, c.getCompound("State")),
                    NbtUtils.readBlockState(blocks, c.getCompound("Before")));
            laid.add(l);
            live(level).put(l.pos(), l.before());
        }
    }

    /** A laid block broken by a player crumbles back into what was there, and drops nothing. */
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            Map<BlockPos, BlockState> live = LIVE.get(level.dimension());
            BlockState before = live == null ? null : live.remove(event.getPos());
            if (before != null) {
                event.setCanceled(true);
                level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, event.getPos(), Block.getId(event.getState()));
                level.setBlock(event.getPos(), before, Block.UPDATE_ALL);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        LIVE.clear();
    }
}
