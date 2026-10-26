package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Asteroids: lumps of rock with flat faces catching the light, no two alike (each a sphere of facets pushed in and out
 * by its own bumps and dents), one block across, drawn as solid rock.
 */
public final class AsteroidRender {
    public static final int SHAPES = 6;
    private static final float[][] ROCKS = new float[SHAPES][];
    /** Rock colours: grey, brown, basalt, rust, sand, slate. */
    public static final int[] TINTS = {0xD8D2CC, 0xC8A890, 0x9A949A, 0xD09070, 0xE8DCC0, 0xA8B0C0};

    static {
        for (int k = 0; k < SHAPES; k++) {
            ROCKS[k] = rock(901L + k * 7919L);
        }
    }

    private AsteroidRender() {}

    /** What rocks are drawn with: solid, the moon's craters speckling each facet. */
    public static VertexConsumer buffer(MultiBufferSource buffer) {
        return buffer.getBuffer(RenderType.entityCutoutNoCull(CosmosRender.MOON));
    }

    private static float[] norm(float[] v) {
        float l = Mth.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return new float[]{v[0] / l, v[1] / l, v[2] / l};
    }

    private static float[] mid(float[] a, float[] b) {
        return norm(new float[]{a[0] + b[0], a[1] + b[1], a[2] + b[2]});
    }

    /** An octahedron split twice into a sphere of facets, each corner pushed out or in by bumps of its own. */
    private static float[] rock(long seed) {
        List<float[][]> faces = new ArrayList<>();
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    float[] a = {sx, 0, 0}, b = {0, sy, 0}, c = {0, 0, sz};
                    // (X, Y, Z) faces out; a mirrored corner turns the other way
                    faces.add(sx * sy * sz > 0 ? new float[][]{a, b, c} : new float[][]{a, c, b});
                }
            }
        }
        for (int level = 0; level < 2; level++) {
            List<float[][]> split = new ArrayList<>();
            for (float[][] f : faces) {
                float[] ab = mid(f[0], f[1]), bc = mid(f[1], f[2]), ca = mid(f[2], f[0]);
                split.add(new float[][]{f[0], ab, ca});
                split.add(new float[][]{ab, f[1], bc});
                split.add(new float[][]{ca, bc, f[2]});
                split.add(new float[][]{ab, bc, ca});
            }
            faces = split;
        }
        Random random = new Random(seed);
        int bumps = 7;
        float[][] dirs = new float[bumps][];
        float[] amps = new float[bumps];
        for (int k = 0; k < bumps; k++) {
            dirs[k] = norm(new float[]{(float) random.nextGaussian(), (float) random.nextGaussian(), (float) random.nextGaussian()});
            amps[k] = (random.nextFloat() - .45f) * .5f;
        }
        float squash = .7f + random.nextFloat() * .3f;
        Solid.Builder builder = new Solid.Builder();
        for (float[][] f : faces) {
            float[][] p = new float[3][];
            for (int i = 0; i < 3; i++) {
                float[] v = f[i];
                float r = 1;
                for (int k = 0; k < bumps; k++) {
                    float d = v[0] * dirs[k][0] + v[1] * dirs[k][1] + v[2] * dirs[k][2];
                    r += amps[k] * Math.max(0, d) * Math.max(0, d) * Math.max(0, d);
                }
                p[i] = new float[]{v[0] * r * .5f, v[1] * r * .5f * squash, v[2] * r * .5f};
            }
            builder.tri(p[0], p[1], p[2]);
        }
        return builder.build();
    }

    /**
     * Rock {@code shape} at the origin, {@code size} across, turned by {@code spin} degrees about a tumbling axis of its
     * own, in {@code rgb} (a rock tint, or heated toward orange).
     */
    public static void rock(PoseStack poseStack, VertexConsumer consumer, int shape, float size, float spin, int rgb, float time) {
        int k = Math.floorMod(shape, SHAPES);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(spin + k * 47));
        poseStack.mulPose(Axis.XP.rotationDegrees(spin * .6f + k * 31));
        poseStack.scale(size, size, size);
        Solid.draw(consumer, poseStack.last(), ROCKS[k], rgb, 255, time, .15f, GlyphRender.FULL);
        poseStack.popPose();
    }
}
