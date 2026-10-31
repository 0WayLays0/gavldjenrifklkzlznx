package com.waylays.arcanes.client.render.water;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import static com.waylays.arcanes.client.render.water.WaterMesh.*;

/**
 * Bubble Prison: a great soap bubble, its film nearly clear with swirling bands of rainbow colour sliding over it,
 * brighter at its rim where the film is seen edge on; a window of light reflected on it; little bubbles and a drop or
 * two running down inside it; it wobbles, shivers harder before it bursts, then bursts in a ring of drops and spray.
 */
public final class BubbleModel {
    private static final int SEGS = 22;
    private static final int RINGS = 14;

    private BubbleModel() {}

    /** Round the pose's origin; {@code r} its radius, {@code shiver} (0 to 1) how close it is to bursting, {@code burst} (0 to 1) how far it has burst. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, View view, float age, float r, float shiver, float burst, int seed, int light) {
        if (burst < 1 && r > .01f) {
            float wob = .03f + shiver * .09f;
            Vector3f[][] pts = new Vector3f[RINGS + 1][SEGS + 1];
            float[][] u = new float[RINGS + 1][SEGS + 1], v = new float[RINGS + 1][SEGS + 1];
            int[][] col = new int[RINGS + 1][SEGS + 1];
            Vector3f eye = new Vector3f(view.eye()).normalize();
            for (int j = 0; j <= RINGS; j++) {
                float f = j * Mth.PI / RINGS;
                for (int s = 0; s <= SEGS; s++) {
                    float a = s * Mth.TWO_PI / SEGS;
                    Vector3f n = new Vector3f(Mth.sin(f) * Mth.cos(a), Mth.cos(f), Mth.sin(f) * Mth.sin(a));
                    float bump = 1 + wob * noise(n.x * 1.5f + age * .08f, n.y * 1.5f, n.z * 1.5f + seed);
                    pts[j][s] = new Vector3f(n).mul(r * bump * (1 + burst * .4f));
                    u[j][s] = (float) s / SEGS * 2 + age * .004f;
                    v[j][s] = (float) j / RINGS + age * .01f + .15f * Mth.sin(a * 2 + age * .03f);
                    // Seen edge on at its rim, the film is brighter
                    float edge = 1 - Math.abs(n.dot(eye));
                    col[j][s] = argb(0xFFFFFF, (.22f + .6f * edge * edge * edge) * (1 - burst));
                }
            }
            VertexConsumer film = translucent(buffer, BUBBLE);
            grid(film, poseStack.last(), pts, u, v, col, light, .9f);
            // The window of light on it, little bubbles rising inside
            VertexConsumer drops = translucent(buffer, DROP);
            Vector3f toEye = new Vector3f(view.eye()).normalize();
            Vector3f glint = new Vector3f(toEye).mul(r * .85f).add(-r * .35f, r * .35f, 0);
            puff(drops, poseStack, view, glint.x, glint.y, glint.z, r * .3f, 0, -1, argb(0xFFFFFF, .9f * (1 - burst)), light);
            for (int k = 0; k < 10; k++) {
                float t = (age * .03f + hash(seed + k)) % 1;
                float a = hash(seed + k * 3) * Mth.TWO_PI, rr = r * .55f * hash(seed + k * 5);
                puff(drops, poseStack, view, Mth.cos(a + t) * rr, -r * .8f + t * r * 1.6f, Mth.sin(a + t) * rr, .08f + .1f * hash(seed + k * 7), 0, -1,
                        argb(0xE8FBFF, .7f * Mth.sin(t * Mth.PI) * (1 - burst)), light);
            }
        }
        if (burst > 0) {
            // It bursts: a ring of drops flung out, a puff of spray
            VertexConsumer spray = translucent(buffer, SPRAY);
            for (int k = 0; k < 10; k++) {
                float a = k * Mth.TWO_PI / 10;
                float out = r * (1 + 1.2f * burst);
                puff(spray, poseStack, view, Mth.cos(a) * out, (hash(seed + k) - .5f) * r - burst * 1.2f, Mth.sin(a) * out, .5f + .6f * burst, k, k & 3,
                        argb(FOAM_WHITE, .8f * (1 - burst)), light);
            }
            VertexConsumer streaks = translucent(buffer, STREAK);
            PoseStack.Pose p = poseStack.last();
            float t = burst * 12;
            for (int k = 0; k < 40; k++) {
                float a = hash(seed + k * 11) * Mth.TWO_PI, b = (hash(seed + k * 13) - .5f) * Mth.PI;
                Vector3f dir = new Vector3f(Mth.cos(a) * Mth.cos(b), Mth.sin(b), Mth.sin(a) * Mth.cos(b));
                Vector3f from = new Vector3f(dir).mul(r);
                Vector3f vel = new Vector3f(dir).mul(.18f + .1f * hash(seed + k));
                streak(streaks, p, view.eye(), thrown(from, vel, t, .03f), thrown(from, vel, Math.max(0, t - 1.5f), .03f), .07f, argb(FOAM_WHITE, .9f * (1 - burst)),
                        light);
            }
        }
    }
}
