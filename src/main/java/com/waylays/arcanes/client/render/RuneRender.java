package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Drawing shared by the Rune Magic renderers: carved stone built from scaled blocks (lit by the world), and glowing
 * rune glyphs (drawn additive, full bright) laid on it or on the ground.
 */
public final class RuneRender {
    /** 4 x 4 rune glyphs, white on transparent. */
    public static final ResourceLocation GLYPHS = ArcanesOublies.id("textures/entity/rune/glyphs.png");
    /** A circle of runes: the grooves cut into the ground, and the same lines glowing. */
    public static final ResourceLocation RING_CARVED = ArcanesOublies.id("textures/entity/rune/ring_carved.png");
    public static final ResourceLocation RING_GLOW = ArcanesOublies.id("textures/entity/rune/ring_glow.png");
    public static final ResourceLocation PULSE = ArcanesOublies.id("textures/entity/rune/pulse.png");
    public static final ResourceLocation GLOW = ArcanesOublies.id("textures/entity/rune/glow.png");
    public static final ResourceLocation ARC = ArcanesOublies.id("textures/entity/rune/arc.png");
    public static final ResourceLocation TILE = ArcanesOublies.id("textures/entity/rune/tile_carved.png");
    public static final ResourceLocation CURTAIN = ArcanesOublies.id("textures/entity/rune/curtain.png");

    private RuneRender() {}

    /** Glyph {@code index} (0 to 15) of the sheet, a square of side {@code size} in the XY plane facing +Z. */
    public static void glyph(VertexConsumer consumer, PoseStack.Pose pose, int index, float size, int argb, int light) {
        int i = Math.floorMod(index, 16);
        float u = (i % 4) / 4f;
        float v = (i / 4) / 4f;
        RenderHelper.rectUv(consumer, pose, size, size, u, v, u + .25f, v + .25f, argb, light, false);
    }

    /**
     * A block scaled to {@code sx} x {@code sy} x {@code sz}, standing on ({@code x}, {@code y}, {@code z}) (the
     * middle of its bottom face), leaning {@code tiltX} and {@code tiltZ} degrees.
     */
    public static void block(BlockState state, PoseStack poseStack, MultiBufferSource buffer, int light,
                             float x, float y, float z, float sx, float sy, float sz, float tiltX, float tiltZ) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        if (tiltX != 0) {
            poseStack.mulPose(Axis.XP.rotationDegrees(tiltX));
        }
        if (tiltZ != 0) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(tiltZ));
        }
        poseStack.scale(sx, sy, sz);
        poseStack.translate(-.5, 0, -.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poseStack, buffer, light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    /** A colour with its alpha set to {@code alpha} (0 to 1). */
    public static int alpha(int rgb, float alpha) {
        return ((int) (Mth.clamp(alpha, 0, 1) * 255) << 24) | (rgb & 0xFFFFFF);
    }

    /** For additive layers (eyes): the colour scaled down to fade, fully opaque. */
    public static int additive(int rgb, float strength) {
        float s = Mth.clamp(strength, 0, 1);
        int r = (int) (((rgb >> 16) & 0xFF) * s);
        int g = (int) (((rgb >> 8) & 0xFF) * s);
        int b = (int) ((rgb & 0xFF) * s);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /** A steady pseudo-random number in [0, 1) for a seed, so a stone keeps its shape from frame to frame. */
    public static float hash(int seed) {
        int h = seed * 0x9E3779B1;
        h ^= h >>> 15;
        h *= 0x85EBCA77;
        h ^= h >>> 13;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }
}
