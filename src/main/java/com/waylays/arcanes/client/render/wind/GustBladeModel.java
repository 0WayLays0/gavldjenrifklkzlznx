package com.waylays.arcanes.client.render.wind;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Gust Blade: a crescent of cutting wind flung flat through the air, bowed forward: layered lines of wind along its
 * edge, the foremost the brightest, lines streaming back off it the way it came, a sheen of moving air across it, and
 * a leaf or two caught in its wake.
 */
public final class GustBladeModel {
    private static final int N = 13;

    private GustBladeModel() {}

    /** Round the pose's origin, flying along {@code forward}, {@code span} wide. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f forward, float age, float span, float alpha, int seed,
                            int light) {
        Vector3f eye = view.eye();
        VertexConsumer lines = WindMesh.lines(buffer);
        PoseStack.Pose p = poseStack.last();
        blade(lines, p, eye, new Vector3f(), forward, span, age, alpha, seed);
        // A sheen of moving air across it
        Vector3f f = new Vector3f(forward).normalize();
        Vector3f s = side(f);
        VertexConsumer sheen = WindMesh.see(buffer, WindMesh.SHEEN);
        p = poseStack.last();
        Vector3f[] arc = arc(new Vector3f(), f, s, span, 0);
        Vector3f[] back = new Vector3f[N];
        float[] w = new float[N];
        int[] col = new int[N];
        for (int i = 0; i < N; i++) {
            float t = (float) i / (N - 1) * 2 - 1;
            back[i] = new Vector3f(f);
            w[i] = (.9f * (1 - t * t) + .1f) * span * .4f;
            col[i] = WaterMesh.argb(WindMesh.AIR, .35f * alpha * (1 - t * t));
        }
        WaterMesh.ribbon(sheen, p, arc, back, w, col, .5f, age * .2f, WindMesh.FULL, true);
        // Leaves caught in its wake
        VertexConsumer leaves = WindMesh.leaves(buffer);
        p = poseStack.last();
        for (int k = 0; k < 3; k++) {
            float t = (age * .12f + WindMesh.hash(seed + k * 7)) % 1;
            Vector3f at = WindMesh.along(arc, WindMesh.hash(seed + k * 3)).sub(new Vector3f(f).mul(.5f + 2.5f * t));
            WindMesh.leaf(leaves, p, at, .28f * (1 - t * .5f), age * .4f + k, age * .3f + k * 2, (seed + k) & 3, light);
        }
    }

    /** A blade's lines alone, at {@code centre} flying along {@code forward}: for blades drawn in numbers (one pass). */
    public static void blade(VertexConsumer lines, PoseStack.Pose p, Vector3f eye, Vector3f centre, Vector3f forward, float span, float age, float alpha,
                             int seed) {
        Vector3f f = new Vector3f(forward).normalize();
        Vector3f s = side(f);
        // Its edge: layered lines, the foremost the brightest
        for (int k = 0; k < 5; k++) {
            Vector3f[] pts = arc(centre, f, s, span, k * .11f * span);
            WindMesh.line(lines, p, eye, pts, (.22f - .03f * k) * span * .6f, WaterMesh.mix(WindMesh.AIR, WindMesh.JADE, k * .2f), alpha * (.95f - .15f * k));
        }
        // Lines streaming back off it
        Vector3f[] edge = arc(centre, f, s, span, 0);
        for (int j = 0; j < 9; j++) {
            float t = (j + .5f) / 9;
            Vector3f head = WindMesh.along(edge, t);
            float len = (1.2f + 1.2f * WindMesh.hash(seed + j * 5)) * span * .5f * (.4f + .6f * Mth.sin(t * Mth.PI));
            Vector3f[] trail = new Vector3f[6];
            for (int i = 0; i < 6; i++) {
                float u = 1 - i / 5f;
                trail[i] = new Vector3f(head).sub(new Vector3f(f).mul(len * u)).add(new Vector3f(s).mul(.08f * Mth.sin(age * .6f + j + u * 3) * u));
            }
            WindMesh.line(lines, p, eye, trail, .07f * span * .6f, WindMesh.AIR, alpha * .55f);
        }
    }

    /** The crescent: bowed forward at its middle, its horns swept back; pushed back {@code back} blocks. */
    private static Vector3f[] arc(Vector3f centre, Vector3f f, Vector3f s, float span, float back) {
        Vector3f[] pts = new Vector3f[N];
        float r = span * .5f;
        for (int i = 0; i < N; i++) {
            float a = -1.15f + 2.3f * i / (N - 1);
            pts[i] = new Vector3f(centre).add(new Vector3f(s).mul(Mth.sin(a) * r)).add(new Vector3f(f).mul((Mth.cos(a) - 1) * r * .5f - back));
        }
        return pts;
    }

    static Vector3f side(Vector3f f) {
        return Math.abs(f.y) < .95f ? new Vector3f(f).cross(0, 1, 0).normalize() : new Vector3f(1, 0, 0);
    }
}
