package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

/**
 * Drawing shared by the Wood Magic renderers: real wood blocks (logs, azalea leaves, flowering or not), and rounded
 * roots, branches, vines and thorns drawn with a bark texture (the same shapes as the bones of Bone Magic, in bark).
 */
public final class WoodRender {
    public static final ResourceLocation BARK = ArcanesOublies.id("textures/entity/wood/bark.png");
    public static final ResourceLocation VINE = ArcanesOublies.id("textures/entity/wood/vine.png");
    public static final ResourceLocation LEAF = ArcanesOublies.id("textures/entity/wood/leaf.png");
    public static final BlockState LOG = Blocks.OAK_LOG.defaultBlockState();
    public static final BlockState DARK_LOG = Blocks.DARK_OAK_LOG.defaultBlockState();
    public static final BlockState LEAVES = Blocks.AZALEA_LEAVES.defaultBlockState();
    public static final BlockState FLOWERS = Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState();
    /** Thorns: the bark darkened. */
    public static final int THORN = 0xFF6E5236;

    private WoodRender() {}

    public static VertexConsumer bark(MultiBufferSource buffer) {
        return buffer.getBuffer(RenderType.entityCutoutNoCull(BARK));
    }

    public static VertexConsumer vine(MultiBufferSource buffer) {
        return buffer.getBuffer(RenderType.entityCutoutNoCull(VINE));
    }

    /** Leaves, flowering ones here and there for a seed. */
    public static BlockState leaves(int seed) {
        return RuneRender.hash(seed) < .3f ? FLOWERS : LEAVES;
    }

    /** A log standing up, {@code width} wide and {@code height} high, its bottom middle at (x, y, z). */
    public static void log(BlockState state, PoseStack poseStack, MultiBufferSource buffer, int light, float x, float y, float z, float width, float height) {
        RuneRender.block(state, poseStack, buffer, light, x, y, z, width, height, width, 0, 0);
    }

    /** A thorny vine along {@code points}: a green stem, thorns off it every other point. */
    public static void thornyVine(VertexConsumer vine, PoseStack.Pose p, Vector3f[] points, float r, int light, int seed) {
        BoneRender.curve(vine, p, points, r, r * .55f, 5, light, BoneRender.WHITE);
        for (int i = 1; i + 1 < points.length; i += 2) {
            Vector3f along = new Vector3f(points[i + 1]).sub(points[i - 1]);
            if (along.lengthSquared() < 1.0E-6f) {
                continue;
            }
            along.normalize();
            Vector3f side = new Vector3f(along).cross(RuneRender.hash(seed + i) > .5f ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0));
            if (side.lengthSquared() < 1.0E-6f) {
                continue;
            }
            side.normalize().mul((RuneRender.hash(seed + i * 3) > .5f ? 1 : -1) * r * 3.2f);
            thorn(vine, p, points[i], new Vector3f(points[i]).add(side).add(new Vector3f(along).mul(r * 1.5f)), r * .6f, light, THORN);
        }
    }

    /** A thorn: a slim four-sided cone from {@code base} to {@code tip} (cheap: many are drawn). */
    public static void thorn(VertexConsumer c, PoseStack.Pose p, Vector3f base, Vector3f tip, float r, int light, int argb) {
        BoneRender.tube(c, p, base, tip, r, .002f, 4, light, argb);
    }

    /**
     * A curved plate of bark round the vertical axis: the slice of a ring {@code r} out from it between the angles
     * {@code a0} and {@code a1}, from {@code y0} up to {@code y1}, {@code thick} thick, its top edge ragged.
     */
    public static void plate(VertexConsumer c, PoseStack.Pose p, float a0, float a1, float r, float y0, float y1, float thick, int light, int argb, int seed) {
        int steps = 4;
        float ri = r - thick;
        for (int i = 0; i < steps; i++) {
            float t0 = Mth.lerp((float) i / steps, a0, a1), t1 = Mth.lerp((float) (i + 1) / steps, a0, a1);
            // A ragged top: each column a little higher or lower
            float h0 = y1 - (y1 - y0) * .12f * RuneRender.hash(seed + i), h1 = y1 - (y1 - y0) * .12f * RuneRender.hash(seed + i + 1);
            float c0 = Mth.cos(t0), s0 = Mth.sin(t0), c1 = Mth.cos(t1), s1 = Mth.sin(t1);
            float u0 = (float) i / steps, u1 = (float) (i + 1) / steps;
            float v1 = (y1 - y0) / 1.5f;
            // Outside
            RenderHelper.vertex(c, p, c0 * r, y0, s0 * r, u0, v1, argb, light, c0, 0, s0);
            RenderHelper.vertex(c, p, c0 * r, h0, s0 * r, u0, 0, argb, light, c0, 0, s0);
            RenderHelper.vertex(c, p, c1 * r, h1, s1 * r, u1, 0, argb, light, c1, 0, s1);
            RenderHelper.vertex(c, p, c1 * r, y0, s1 * r, u1, v1, argb, light, c1, 0, s1);
            // Inside, darker
            int in = darker(argb);
            RenderHelper.vertex(c, p, c1 * ri, y0, s1 * ri, u1, v1, in, light, -c1, 0, -s1);
            RenderHelper.vertex(c, p, c1 * ri, h1, s1 * ri, u1, 0, in, light, -c1, 0, -s1);
            RenderHelper.vertex(c, p, c0 * ri, h0, s0 * ri, u0, 0, in, light, -c0, 0, -s0);
            RenderHelper.vertex(c, p, c0 * ri, y0, s0 * ri, u0, v1, in, light, -c0, 0, -s0);
            // Top edge
            RenderHelper.vertex(c, p, c0 * ri, h0, s0 * ri, u0, .1f, argb, light, 0, 1, 0);
            RenderHelper.vertex(c, p, c1 * ri, h1, s1 * ri, u1, .1f, argb, light, 0, 1, 0);
            RenderHelper.vertex(c, p, c1 * r, h1, s1 * r, u1, 0, argb, light, 0, 1, 0);
            RenderHelper.vertex(c, p, c0 * r, h0, s0 * r, u0, 0, argb, light, 0, 1, 0);
        }
        // The two side edges
        for (float a : new float[]{a0, a1}) {
            float ca = Mth.cos(a), sa = Mth.sin(a);
            float h = y1 - (y1 - y0) * .12f * RuneRender.hash(seed + (a == a0 ? 0 : steps));
            float n = a == a0 ? -1 : 1;
            RenderHelper.vertex(c, p, ca * ri, y0, sa * ri, 0, 1, argb, light, -sa * n, 0, ca * n);
            RenderHelper.vertex(c, p, ca * ri, h, sa * ri, 0, 0, argb, light, -sa * n, 0, ca * n);
            RenderHelper.vertex(c, p, ca * r, h, sa * r, .1f, 0, argb, light, -sa * n, 0, ca * n);
            RenderHelper.vertex(c, p, ca * r, y0, sa * r, .1f, 1, argb, light, -sa * n, 0, ca * n);
        }
    }

    /** One leaf (the leaf texture, tinted), {@code size} long, lying in the XY plane, seen from both sides. */
    public static void leaf(VertexConsumer c, PoseStack.Pose p, float size, int argb, int light) {
        RenderHelper.rect(c, p, size, size, argb, light, true);
    }

    /** Leaf colours: spring greens, now and then an azalea's pink. */
    public static int leafColour(int seed) {
        float h = RuneRender.hash(seed * 13 + 5);
        if (h < .12f) {
            return 0xFFF2A6D8;
        }
        if (h < .2f) {
            return 0xFFE4FFB0;
        }
        return h < .6f ? 0xFF7CC444 : 0xFF4E9A30;
    }

    private static int darker(int argb) {
        int r = ((argb >> 16) & 0xFF) * 6 / 10, g = ((argb >> 8) & 0xFF) * 6 / 10, b = (argb & 0xFF) * 6 / 10;
        return (argb & 0xFF000000) | r << 16 | g << 8 | b;
    }

    public static VertexConsumer leafBuffer(MultiBufferSource buffer) {
        return buffer.getBuffer(RenderType.entityCutoutNoCull(LEAF));
    }
}
