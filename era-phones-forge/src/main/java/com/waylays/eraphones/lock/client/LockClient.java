package com.waylays.eraphones.lock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.lock.LockRegistry;
import com.waylays.eraphones.lock.Locks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import java.util.HashSet;
import java.util.Set;

/** The padlocks seen: one hangs on each face of a locked door, gate or trapdoor, on the front of a locked chest. */
@EventBusSubscriber(modid = EraPhones.MODID, value = Dist.CLIENT)
public final class LockClient {
    private static final Set<Long> LOCKED = new HashSet<>();
    private static ItemStack padlock;

    private LockClient() {}

    public static void positions(Locks.Positions p) {
        LOCKED.clear();
        LOCKED.addAll(p.blocks());
    }

    public static boolean keyKnown(BlockPos pos) {
        return LOCKED.contains(pos.asLong());
    }

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        LOCKED.clear();
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || LOCKED.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) {
            return;
        }
        if (padlock == null) {
            padlock = new ItemStack(LockRegistry.PADLOCK.get());
        }
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        for (long l : LOCKED) {
            BlockPos pos = BlockPos.of(l);
            if (pos.distToCenterSqr(cam) > 48 * 48) {
                continue;
            }
            BlockState s = level.getBlockState(pos);
            if (s.getBlock() instanceof DoorBlock && s.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
                continue;
            }
            VoxelShape shape = s.getShape(level, pos);
            if (shape.isEmpty()) {
                continue;
            }
            AABB box = shape.bounds();
            Vec3 c = new Vec3(pos.getX() + (box.minX + box.maxX) / 2, pos.getY() + (box.minY + box.maxY) / 2, pos.getZ() + (box.minZ + box.maxZ) / 2);
            if (s.getBlock() instanceof DoorBlock) {
                // At the handle's height, on the lower half's top
                c = new Vec3(c.x, pos.getY() + .82, c.z);
            }
            int light = LevelRenderer.getLightColor(level, pos);
            if (s.getBlock() instanceof ChestBlock || !s.hasProperty(BlockStateProperties.OPEN)) {
                Direction front = s.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? s.getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.SOUTH;
                double reach = front.getAxis() == Direction.Axis.X ? (box.maxX - box.minX) / 2 : (box.maxZ - box.minZ) / 2;
                draw(pose, buffers, cam, c.add(Vec3.atLowerCornerOf(front.getNormal()).scale(reach + .03)), front, light, level);
                continue;
            }
            // A thin thing: a padlock on each of its two faces
            double sx = box.maxX - box.minX, sy = box.maxY - box.minY, sz = box.maxZ - box.minZ;
            Direction a = sx <= sz && sx <= sy ? Direction.EAST : sz <= sy ? Direction.SOUTH : Direction.UP;
            double half = (a == Direction.EAST ? sx : a == Direction.SOUTH ? sz : sy) / 2 + .03;
            for (Direction d : new Direction[] {a, a.getOpposite()}) {
                draw(pose, buffers, cam, c.add(Vec3.atLowerCornerOf(d.getNormal()).scale(half)), d, light, level);
            }
        }
        buffers.endBatch();
    }

    private static void draw(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Vec3 at, Direction facing, int light, Level level) {
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
        if (facing.getAxis().isHorizontal()) {
            pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        } else {
            pose.mulPose(Axis.XP.rotationDegrees(facing == Direction.UP ? -90 : 90));
        }
        pose.scale(.42f, .42f, .42f);
        Minecraft.getInstance().getItemRenderer().renderStatic(padlock, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers, level, 0);
        pose.popPose();
    }
}
