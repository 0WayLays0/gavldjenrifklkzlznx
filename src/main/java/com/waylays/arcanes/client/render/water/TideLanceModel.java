package com.waylays.arcanes.client.render.water;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import static com.waylays.arcanes.client.render.water.WaterMesh.*;

/**
 * Tide Lance, a spear of sea water in flight: a hard bright point, a body of clear turquoise water twisting round
 * itself and thinning into a trail behind, three streams of white water wound round it, a collar of spray thrown back
 * off its point like a bow wave, drops flung off its sides and a wake of spray and mist left hanging in the air.
 */
public final class TideLanceModel {
    private static final int POINTS = 16;

    private TideLanceModel() {}

    /** Drawn round the pose's origin, flying along {@code forward} (a unit vector in the pose's frame). */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, View view, Vector3f forward, float age, int seed, int light) {
        Vector3f f = new Vector3f(forward).normalize();
        Vector3f s1 = Math.abs(f.y) < .95f ? new Vector3f(f).cross(0, 1, 0).normalize() : new Vector3f(1, 0, 0);
        Vector3f s2 = new Vector3f(f).cross(s1).normalize();
        float grow = Mth.clamp(age / 3, 0, 1);
        // The body: point ahead, trail behind
        Vector3f[] axis = new Vector3f[POINTS];
        float[] body = new float[POINTS], skin = new float[POINTS];
        int[] bc = new int[POINTS], sc = new int[POINTS];
        for (int i = 0; i < POINTS; i++) {
            float t = (float) i / (POINTS - 1);
            float along = 1.1f - t * 3.4f * grow;
            float wob = .06f * t * t;
            axis[i] = new Vector3f(f).mul(along).add(new Vector3f(s1).mul(noise(t * 3 - age * .5f, seed, 0) * wob))
                    .add(new Vector3f(s2).mul(noise(t * 3 - age * .5f, seed, 4) * wob));
            float shape = t < .12f ? t / .12f : (float) Math.pow(1 - (t - .12f) / .88f, .8f);
            body[i] = .03f + .2f * shape;
            skin[i] = body[i] * 1.35f + .02f;
            bc[i] = argb(t < .1f ? FOAM_WHITE : mix(SHALLOW, CREST, .4f + .3f * Mth.sin(t * 9 - age)), (.88f - .4f * t * t));
            sc[i] = argb(FOAM_WHITE, .45f * Mth.sin(Math.min(1, t * 1.6f) * Mth.PI) * (1 - t * .6f));
        }
        VertexConsumer water = translucent(buffer, WATER);
        PoseStack.Pose p = poseStack.last();
        tube(water, p, axis, body, bc, 10, .3f, 2.4f, age * 1.3f, 1, 2.5f, seed, light, .55f);

        VertexConsumer foam = translucent(buffer, FOAM);
        p = poseStack.last();
        tube(foam, p, axis, skin, sc, 10, .6f, 2, age * 1.6f, .9f, -1.5f, seed + 9, light, .8f);
        // Three streams of white water wound round it
        for (int k = 0; k < 3; k++) {
            int n = 14;
            Vector3f[] pts = new Vector3f[n];
            float[] rad = new float[n];
            int[] col = new int[n];
            for (int i = 0; i < n; i++) {
                float t = (float) i / (n - 1);
                float a = k * Mth.TWO_PI / 3 + age * .9f - t * 8;
                float r = .2f + .25f * t * t;
                pts[i] = new Vector3f(f).mul(.75f - t * 3 * grow).add(new Vector3f(s1).mul(Mth.cos(a) * r)).add(new Vector3f(s2).mul(Mth.sin(a) * r));
                rad[i] = .045f * (1 - t * .6f);
                col[i] = argb(FOAM_WHITE, .85f * (1 - t));
            }
            tube(foam, p, pts, rad, col, 5, .3f, 3, age * 2, 1, 0, seed + k * 3, light, .8f);
        }
        // The collar of spray thrown back off its point
        for (int k = 0; k < 10; k++) {
            float a = k * Mth.TWO_PI / 10 + age * .3f;
            Vector3f radial = new Vector3f(s1).mul(Mth.cos(a)).add(new Vector3f(s2).mul(Mth.sin(a)));
            int n = 5;
            Vector3f[] pts = new Vector3f[n], side = new Vector3f[n];
            float[] w = new float[n];
            int[] col = new int[n];
            for (int i = 0; i < n; i++) {
                float u = (float) i / (n - 1);
                pts[i] = new Vector3f(f).mul(.75f - u * .9f).add(new Vector3f(radial).mul(.12f + .45f * u));
                side[i] = new Vector3f(f).cross(radial).normalize();
                w[i] = .06f + .16f * u;
                col[i] = argb(FOAM_WHITE, .8f * (1 - u));
            }
            ribbon(foam, p, pts, side, w, col, 2, age * .4f, light, true);
        }

        // A wake of spray and mist hanging behind it
        VertexConsumer spray = translucent(buffer, SPRAY);
        for (int k = 0; k < 10; k++) {
            float t = (age * .1f + hash(seed + k * 7)) % 1;
            Vector3f c = new Vector3f(f).mul(-(.6f + 3.2f * t) * grow).add(new Vector3f(s1).mul((hash(seed + k) - .5f) * .5f * (1 + t)))
                    .add(new Vector3f(s2).mul((hash(seed + k * 3) - .5f) * .5f * (1 + t)));
            puff(spray, poseStack, view, c.x, c.y, c.z, .35f + .6f * t, k + age * .05f, k & 3, argb(FOAM_WHITE, .7f * (1 - t)), light);
        }
        VertexConsumer mist = translucent(buffer, MIST);
        for (int k = 0; k < 5; k++) {
            float t = (age * .06f + hash(seed + k * 11)) % 1;
            Vector3f c = new Vector3f(f).mul(-(1 + 3.5f * t) * grow);
            puff(mist, poseStack, view, c.x, c.y, c.z, .8f + 1.2f * t, k, -1, argb(0xEAF6FF, .3f * (1 - t)), light);
        }
        // Drops flung off its sides
        VertexConsumer drops = translucent(buffer, STREAK);
        p = poseStack.last();
        for (int k = 0; k < 18; k++) {
            float t = (age * .15f + hash(seed + k * 13)) % 1;
            float a = hash(seed + k * 17) * Mth.TWO_PI;
            Vector3f radial = new Vector3f(s1).mul(Mth.cos(a)).add(new Vector3f(s2).mul(Mth.sin(a)));
            Vector3f head = new Vector3f(f).mul((.3f - 2.2f * t) * grow).add(new Vector3f(radial).mul(.2f + .6f * t)).add(0, -.3f * t * t, 0);
            Vector3f tail = new Vector3f(head).add(new Vector3f(f).mul(.35f));
            streak(drops, p, view.eye(), tail, head, .05f, argb(FOAM_WHITE, .85f * (1 - t)), light);
        }
    }
}
