package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Moss, Copper and Rust Magic is drawn from the game's own blocks, scaled and stacked: old copper at every stage of
 * verdigris, cut and chiselled copper, mossy stone, moss, and vines hanging down it all.
 */
public final class VerdigrisRender {
    public static final BlockState COPPER = Blocks.COPPER_BLOCK.defaultBlockState();
    public static final BlockState EXPOSED = Blocks.EXPOSED_COPPER.defaultBlockState();
    public static final BlockState WEATHERED = Blocks.WEATHERED_COPPER.defaultBlockState();
    public static final BlockState OXIDIZED = Blocks.OXIDIZED_COPPER.defaultBlockState();
    public static final BlockState WEATHERED_CUT = Blocks.WEATHERED_CUT_COPPER.defaultBlockState();
    public static final BlockState OXIDIZED_CUT = Blocks.OXIDIZED_CUT_COPPER.defaultBlockState();
    public static final BlockState EXPOSED_CHISELED = Blocks.EXPOSED_CHISELED_COPPER.defaultBlockState();
    public static final BlockState OXIDIZED_CHISELED = Blocks.OXIDIZED_CHISELED_COPPER.defaultBlockState();
    public static final BlockState OXIDIZED_GRATE = Blocks.OXIDIZED_COPPER_GRATE.defaultBlockState();
    public static final BlockState RAW_COPPER = Blocks.RAW_COPPER_BLOCK.defaultBlockState();
    public static final BlockState MOSSY_BRICKS = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
    public static final BlockState CRACKED_BRICKS = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
    public static final BlockState CHISELED_BRICKS = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
    public static final BlockState MOSSY_COBBLE = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
    public static final BlockState MOSS = Blocks.MOSS_BLOCK.defaultBlockState();
    public static final BlockState MOSS_CARPET = Blocks.MOSS_CARPET.defaultBlockState();
    public static final BlockState COARSE_DIRT = Blocks.COARSE_DIRT.defaultBlockState();
    /** The green the vines are tinted with: a deep, old green. */
    public static final int VINE_GREEN = 0x4F7F2A;

    private static final BlockState[] VINE_SIDES = {
            Blocks.VINE.defaultBlockState().setValue(VineBlock.NORTH, true),
            Blocks.VINE.defaultBlockState().setValue(VineBlock.EAST, true),
            Blocks.VINE.defaultBlockState().setValue(VineBlock.SOUTH, true),
            Blocks.VINE.defaultBlockState().setValue(VineBlock.WEST, true)};
    private static final BlockState VINE_ALL = Blocks.VINE.defaultBlockState()
            .setValue(VineBlock.NORTH, true).setValue(VineBlock.EAST, true).setValue(VineBlock.SOUTH, true).setValue(VineBlock.WEST, true);

    private VerdigrisRender() {}

    /** A block scaled to {@code sx} x {@code sy} x {@code sz}, its bottom centre at (x, y, z). */
    public static void block(BlockState state, PoseStack poseStack, MultiBufferSource buffer, int light, float x, float y, float z, float sx, float sy, float sz) {
        RuneRender.block(state, poseStack, buffer, light, x, y, z, sx, sy, sz, 0, 0);
    }

    /** The same, turned {@code yaw} degrees about its upright axis. */
    public static void turned(BlockState state, PoseStack poseStack, MultiBufferSource buffer, int light, float x, float y, float z,
                              float sx, float sy, float sz, float yaw) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        RuneRender.block(state, poseStack, buffer, light, 0, 0, 0, sx, sy, sz, 0, 0);
        poseStack.popPose();
    }

    /** A block whose tinted faces (vines, leaves) take {@code rgb}. */
    public static void tinted(BlockState state, PoseStack poseStack, MultiBufferSource buffer, int light, float x, float y, float z,
                              float sx, float sy, float sz, int rgb) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.scale(sx, sy, sz);
        poseStack.translate(-.5, 0, -.5);
        BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
        BakedModel model = blocks.getBlockModel(state);
        blocks.getModelRenderer().renderModel(poseStack.last(), buffer.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS)),
                state, model, ((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f, light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    /**
     * Vines hanging from {@code top} down the sides of a box {@code width} x {@code depth} centred on the axis: on each
     * side (north, east, south, west) whose length is above zero, that many blocks down, one block at a time so the
     * leaves keep their size.
     */
    public static void vines(PoseStack poseStack, MultiBufferSource buffer, int light, float top, float width, float depth, float[] lengths) {
        // A vine's leaves sit 0.8 pixels inside its block: the block is made a little larger than the box
        float sx = (width + .12f) / (1 - 1.6f / 16);
        float sz = (depth + .12f) / (1 - 1.6f / 16);
        for (int side = 0; side < 4; side++) {
            float length = lengths[side % lengths.length];
            float y = top;
            while (length > .05f) {
                float step = Math.min(1, length);
                tinted(VINE_SIDES[side], poseStack, buffer, light, 0, y - step, 0, sx, step, sz, VINE_GREEN);
                y -= step;
                length -= step;
            }
        }
    }

    /** Vines wrapped all around a box, from {@code bottom} up {@code height}. */
    public static void wrap(PoseStack poseStack, MultiBufferSource buffer, int light, float bottom, float width, float height) {
        float s = (width + .12f) / (1 - 1.6f / 16);
        float y = bottom;
        float left = height;
        while (left > .05f) {
            float step = Math.min(1, left);
            tinted(VINE_ALL, poseStack, buffer, light, 0, y, 0, s, step, s, VINE_GREEN);
            y += step;
            left -= step;
        }
    }

    /** How long the vines hang on each side of something seeded {@code seed}, up to {@code most}: some sides bare. */
    public static float[] hanging(int seed, float most) {
        float[] out = new float[4];
        for (int i = 0; i < 4; i++) {
            float h = RuneRender.hash(seed * 31 + i * 7);
            out[i] = h < .35f ? 0 : most * (.35f + .65f * RuneRender.hash(seed * 13 + i));
        }
        return out;
    }
}
