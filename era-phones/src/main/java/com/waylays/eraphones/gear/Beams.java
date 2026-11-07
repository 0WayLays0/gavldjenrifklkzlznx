package com.waylays.eraphones.gear;

import com.waylays.eraphones.EraPhones;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The torches' light. A lit torch held in either hand lights where it points, up to {@link #REACH} blocks away (an
 * invisible light set in the air just before what it hits, and a softer one round whoever holds it), moving with it;
 * and drains its batteries (a full set lasts twenty minutes). The lights are taken away when the torch goes out,
 * leaves the hand or its holder leaves.
 */
@EventBusSubscriber(modid = EraPhones.MODID)
public final class Beams {
    public static final int REACH = 24;
    /** A full set of batteries lasts this many ticks. */
    public static final int LIFE = 24000;
    private static final Map<UUID, List<GlobalPos>> LIT = new HashMap<>();

    private Beams() {}

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) {
            ItemStack torch = lit(p);
            List<GlobalPos> now = new ArrayList<>(2);
            if (torch != null) {
                if (p.tickCount % (LIFE / Gear.FULL) == 0 && !p.getAbilities().instabuild) {
                    Gear.setBattery(torch, Gear.battery(torch) - 1);
                }
                ServerLevel level = p.serverLevel();
                Vec3 eye = p.getEyePosition();
                Vec3 end = eye.add(p.getLookAngle().scale(REACH));
                BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
                Vec3 at = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation().subtract(p.getLookAngle().scale(.6));
                double dist = at.distanceTo(eye);
                // Brighter near, fainter far, as a real beam spreads
                int level1 = (int) Math.round(15 - Math.max(0, dist - 6) / (REACH - 6) * 6);
                place(level, BlockPos.containing(at), level1, now);
                place(level, BlockPos.containing(eye), 7, now);
            }
            List<GlobalPos> before = LIT.getOrDefault(p.getUUID(), List.of());
            for (GlobalPos g : before) {
                if (!now.contains(g)) {
                    clear(event.getServer().getLevel(g.dimension()), g.pos());
                }
            }
            if (now.isEmpty()) {
                LIT.remove(p.getUUID());
            } else {
                LIT.put(p.getUUID(), now);
            }
        }
    }

    /** The lit torch {@code p} holds, if any. */
    private static ItemStack lit(ServerPlayer p) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack s = p.getItemInHand(hand);
            if (s.getItem() instanceof FlashlightItem && Gear.on(s)) {
                return s;
            }
        }
        return null;
    }

    private static void place(ServerLevel level, BlockPos pos, int light, List<GlobalPos> lit) {
        if (!level.isLoaded(pos)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        GlobalPos g = GlobalPos.of(level.dimension(), pos);
        if (state.isAir()) {
            level.setBlock(pos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, Math.max(1, Math.min(15, light))), 2);
            lit.add(g);
        } else if (state.is(Blocks.LIGHT) && !lit.contains(g)) {
            if (state.getValue(LightBlock.LEVEL) != light && ours(g)) {
                level.setBlock(pos, state.setValue(LightBlock.LEVEL, Math.max(1, Math.min(15, light))), 2);
            }
            if (ours(g)) {
                lit.add(g);
            }
        }
    }

    /** Whether a light there was set by a torch (and not placed by someone). */
    private static boolean ours(GlobalPos g) {
        for (List<GlobalPos> l : LIT.values()) {
            if (l.contains(g)) {
                return true;
            }
        }
        return false;
    }

    private static void clear(ServerLevel level, BlockPos pos) {
        if (level != null && level.isLoaded(pos) && level.getBlockState(pos).is(Blocks.LIGHT)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
    }

    private static void clearAll(net.minecraft.server.MinecraftServer server, UUID id) {
        List<GlobalPos> l = LIT.remove(id);
        if (l != null) {
            for (GlobalPos g : l) {
                if (!ours(g)) {
                    clear(server.getLevel(g.dimension()), g.pos());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            clearAll(p.server, p.getUUID());
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            clearAll(p.server, p.getUUID());
        }
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        for (UUID id : new ArrayList<>(LIT.keySet())) {
            clearAll(event.getServer(), id);
        }
    }
}
