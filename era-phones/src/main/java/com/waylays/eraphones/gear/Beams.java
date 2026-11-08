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
 * The torches' light. A lit torch held in either hand shines a beam where it points, up to {@link #REACH} blocks
 * away: invisible lights strung along it from the lamp out, fainter with distance, and the patch it falls on;
 * and drains its batteries (a full set lasts twenty minutes). The lights are taken away when the torch goes out,
 * leaves the hand or its holder leaves.
 */
@EventBusSubscriber(modid = EraPhones.MODID)
public final class Beams {
    public static final int REACH = 16;
    /** How far apart the lights along the beam are (blocks). */
    private static final double STEP = 3;
    /** Torches flickering (a game master's doing), by holder: until when (server ticks). */
    private static final Map<UUID, Long> FLICKER = new HashMap<>();
    private static final java.util.Random RANDOM = new java.util.Random();
    private static final java.util.Set<UUID> DARK = new java.util.HashSet<>();
    /** A full set of batteries lasts this many ticks. */
    public static final int LIFE = 24000;
    private static final Map<UUID, List<GlobalPos>> LIT = new HashMap<>();

    private Beams() {}

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        long now = event.getServer().getTickCount();
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) {
            ItemStack torch = lit(p);
            Long until = FLICKER.get(p.getUUID());
            if (until != null && until < now) {
                FLICKER.remove(p.getUUID());
                DARK.remove(p.getUUID());
                until = null;
            }
            // Flickering: out for a moment now and then, sometimes for longer, buzzing
            if (torch != null && until != null) {
                boolean dark = DARK.contains(p.getUUID());
                if (dark ? RANDOM.nextFloat() < .25f : RANDOM.nextFloat() < .18f) {
                    dark = !dark;
                    if (dark) {
                        DARK.add(p.getUUID());
                    } else {
                        DARK.remove(p.getUUID());
                    }
                    if (RANDOM.nextFloat() < .5f) {
                        p.level().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.LEVER_CLICK,
                                net.minecraft.sounds.SoundSource.PLAYERS, .15f, 1.8f + RANDOM.nextFloat() * .3f);
                    }
                }
                if (dark) {
                    torch = null;
                }
            }
            List<GlobalPos> lit = new ArrayList<>(8);
            if (torch != null) {
                if (p.tickCount % (LIFE / Gear.FULL) == 0 && !p.getAbilities().instabuild) {
                    Gear.setBattery(torch, Gear.battery(torch) - 1);
                }
                ServerLevel level = p.serverLevel();
                Vec3 eye = p.getEyePosition();
                Vec3 dir = p.getLookAngle();
                Vec3 end = eye.add(dir.scale(REACH));
                BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
                boolean struck = hit.getType() != HitResult.Type.MISS;
                double reach = struck ? hit.getLocation().distanceTo(eye) : REACH;
                // The beam itself: soft lights strung along it from the lamp out, each a little fainter, so the
                // ground and the walls before the holder are lit all the way, as a cone of light would
                for (double t = 1.5; t < reach - 1; t += STEP) {
                    place(level, BlockPos.containing(eye.add(dir.scale(t))), (int) Math.round(8 - t / REACH * 4), lit);
                }
                // The patch it falls on, brighter near, dimmer far
                if (struck) {
                    Vec3 at = hit.getLocation().subtract(dir.scale(.6));
                    place(level, BlockPos.containing(at), (int) Math.round(9 - reach / REACH * 4), lit);
                }
            }
            List<GlobalPos> before = LIT.getOrDefault(p.getUUID(), List.of());
            for (GlobalPos g : before) {
                if (!lit.contains(g)) {
                    clear(event.getServer().getLevel(g.dimension()), g.pos());
                }
            }
            if (lit.isEmpty()) {
                LIT.remove(p.getUUID());
            } else {
                LIT.put(p.getUUID(), lit);
            }
        }
    }

    /** Makes the torch {@code p} holds flicker for {@code ticks}. */
    public static void flicker(net.minecraft.server.MinecraftServer server, UUID p, int ticks) {
        FLICKER.put(p, (long) server.getTickCount() + ticks);
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
