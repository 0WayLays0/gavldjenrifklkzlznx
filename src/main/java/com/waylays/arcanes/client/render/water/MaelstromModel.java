package com.waylays.arcanes.client.render.water;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import static com.waylays.arcanes.client.render.water.WaterMesh.*;

/**
 * Maelstrom, drawn as a real whirlpool: a raised ring of churning water whose inner slope winds down in a spiral to a
 * dark throat, turning ever faster the nearer it gets; arms of foam wound into the spiral; a white crest running round
 * its rim, spray flung off it, drops racing round; it opens out of the ground and sinks back into it.
 */
public final class MaelstromModel {
    private static final int RINGS = 16;
    private static final int SEGS = 56;

    private MaelstromModel() {}

    /** {@code opened} (0 to 1) how wide open it is; {@code radius} its full radius. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, View view, float age, float opened, float radius, int seed, int light) {
        if (opened <= .01f) {
            return;
        }
        float rr = radius * (.4f + .6f * opened);
        float rim = rr * .82f, crest = .95f * opened;
        Vector3f[][] pts = new Vector3f[RINGS + 1][SEGS + 1];
        float[][] u = new float[RINGS + 1][SEGS + 1], v = new float[RINGS + 1][SEGS + 1];
        int[][] col = new int[RINGS + 1][SEGS + 1];
        float[][] fu = new float[RINGS + 1][SEGS + 1], fv = new float[RINGS + 1][SEGS + 1];
        int[][] fcol = new int[RINGS + 1][SEGS + 1];
        Vector3f[][] lace = new Vector3f[RINGS + 1][SEGS + 1];
        for (int i = 0; i <= RINGS; i++) {
            float t = (float) i / RINGS;
            float r = .25f + (rr * 1.12f - .25f) * t;
            float h;
            if (r <= rim) {
                float k = r / rim;
                h = crest * (float) Math.pow(k, 1.8f) + .04f;
            } else {
                float k = (r - rim) / (rr * 1.12f - rim);
                h = crest * (1 - k * k * (3 - 2 * k)) + .02f;
            }
            float omega = .03f + .32f * (1 - Math.min(1, r / rim)) * (1 - Math.min(1, r / rim));
            float spiral = (float) Math.log(r + .3f) * 1.6f;
            for (int s = 0; s <= SEGS; s++) {
                float a = s * Mth.TWO_PI / SEGS;
                float swirl = a - age * omega;
                float wave = .09f * opened * Mth.sin(swirl * 6 + spiral * 3) * Math.min(1, r / rim);
                pts[i][s] = new Vector3f(Mth.cos(a) * r, h + wave, Mth.sin(a) * r);
                lace[i][s] = new Vector3f(Mth.cos(a) * r, h + wave + .03f, Mth.sin(a) * r);
                u[i][s] = (float) s / SEGS * 6 - age * omega * 6 / Mth.TWO_PI;
                v[i][s] = spiral + r * .15f;
                float depth = Math.min(1, r / rim);
                int c = r <= rim ? mix(water(.12f + .8f * depth * depth), OPEN, .25f * depth) : water(.95f - .25f * (r - rim) / (rr * 1.12f - rim));
                col[i][s] = argb(c, (.92f - .2f * Math.max(0, (r - rim) / (rr * .3f))) * opened);
                float arms = (float) Math.pow(Math.max(0, Mth.sin(a * 3 - age * omega * 3 + spiral * 4)), 3);
                float edge = Mth.clamp((1 - Math.abs(r - rim) / (rr * .1f)), 0, 1);
                fu[i][s] = u[i][s] * .7f;
                fv[i][s] = v[i][s] * 1.3f;
                fcol[i][s] = argb(FOAM_WHITE, Mth.clamp(arms * .55f * depth + edge * .65f, 0, .9f) * opened * Mth.clamp((r - .5f), 0, 1));
            }
        }
        VertexConsumer water = translucent(buffer, WATER);
        PoseStack.Pose p = poseStack.last();
        grid(water, p, pts, u, v, col, light, .5f);
        // The dark throat at its heart
        disc(water, p, .03f, 0, .7f, 16, 1, age * .2f, 0, argb(ABYSS, .97f * opened), argb(ABYSS, .9f * opened), light);

        VertexConsumer foam = translucent(buffer, FOAM);
        p = poseStack.last();
        grid(foam, p, lace, fu, fv, fcol, light, .8f);

        // Spray flung off its rim, clumps of foam riding round
        VertexConsumer spray = translucent(buffer, SPRAY);
        for (int k = 0; k < 26; k++) {
            float t = (age * .06f + hash(seed + k * 7)) % 1;
            float a = hash(seed + k * 3) * Mth.TWO_PI - age * .05f;
            float out = rim + (hash(seed + k * 5) - .3f) * .8f + .6f * t;
            puff(spray, poseStack, view, Mth.cos(a) * out, crest + .15f + .6f * Mth.sin(t * Mth.PI), Mth.sin(a) * out, .6f + .7f * t, a + age * .05f, k & 3,
                    argb(FOAM_WHITE, .8f * (1 - t) * opened), light);
        }
        for (int k = 0; k < 14; k++) {
            float t = (age * .025f + hash(seed + k * 13)) % 1;
            float r = rim * (1 - t * .9f);
            float a = hash(seed + k * 17) * Mth.TWO_PI - age * (.03f + .3f * t * t);
            float h = crest * (float) Math.pow(r / rim, 1.8f) + .15f;
            puff(spray, poseStack, view, Mth.cos(a) * r, h, Mth.sin(a) * r, .5f + .4f * (1 - t), a, (k + 2) & 3, argb(FOAM_WHITE, .7f * (1 - t * t) * opened), light);
        }

        // Drops racing round it
        VertexConsumer drops = translucent(buffer, STREAK);
        p = poseStack.last();
        Vector3f eye = view.eye();
        for (int k = 0; k < 40; k++) {
            float r = rim * (.3f + .8f * hash(seed + k * 19));
            float omega = .05f + .3f * (1 - Math.min(1, r / rim));
            float a = hash(seed + k * 23) * Mth.TWO_PI - age * omega;
            float h = crest * (float) Math.pow(Math.min(1, r / rim), 1.8f) + .25f;
            Vector3f head = new Vector3f(Mth.cos(a) * r, h, Mth.sin(a) * r);
            Vector3f tail = new Vector3f(Mth.cos(a + .35f) * r, h, Mth.sin(a + .35f) * r);
            streak(drops, p, eye, head, tail, .08f, argb(FOAM_WHITE, .75f * opened), light);
        }
    }
}
