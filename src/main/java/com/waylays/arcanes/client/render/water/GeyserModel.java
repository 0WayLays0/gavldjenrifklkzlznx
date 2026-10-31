package com.waylays.arcanes.client.render.water;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import static com.waylays.arcanes.client.render.water.WaterMesh.*;

/**
 * Geyser, the way a geyser erupts: first the ground darkens, a pool of foam swells and bubbles and steam wisps rise
 * from it; then the water roars up in a bundle of jets, white and aerated, each on its own line and its own height,
 * their tops tearing into a churning crown of spray; drops arc out of it in fountains, sheets of water fall back round
 * the column, white water boils at its foot and steam billows round it all; at the end the crown collapses and falls
 * while the steam lingers.
 */
public final class GeyserModel {
    private static final int JETS = 7;

    private GeyserModel() {}

    /**
     * {@code warn} (0 to 1) how far the pool has swelled before it erupts, {@code column} (0 to 1) how high the water
     * stands, {@code end} (0 to 1) how far it has fallen back; {@code height} its full height, {@code radius} its
     * width at the foot.
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, View view, float age, float warn, float column, float end, float height,
                            float radius, int seed, int light) {
        float r = radius;
        float alive = 1 - end;
        float h = height * column;
        boolean erupting = column > 0;
        float pool = Math.min(warn, alive);

        // The wet ground and the pool on it
        VertexConsumer water = translucent(buffer, WATER);
        PoseStack.Pose p = poseStack.last();
        if (pool > 0) {
            disc(water, p, .03f, 0, r * (1.1f + .8f * pool), 28, 3, age * .01f, age * .02f, argb(water(.8f), .55f * pool), argb(water(.6f), 0), light);
        }
        // The jets: a bundle of them, each on its own line and its own height
        Vector3f[][] tops = new Vector3f[JETS][];
        if (erupting) {
            for (int k = 0; k < JETS; k++) {
                float a = k * Mth.TWO_PI / JETS + hash(seed + k) * .8f;
                float off = k == 0 ? 0 : r * (.25f + .2f * hash(seed + k * 3));
                float lean = k == 0 ? 0 : .06f + .1f * hash(seed + k * 5);
                float jh = h * (k == 0 ? 1 : .7f + .28f * hash(seed + k * 7)) * (1 + .06f * noise(age * .15f, k, seed));
                int n = 14;
                Vector3f[] axis = new Vector3f[n];
                float[] rad = new float[n], skin = new float[n];
                int[] col = new int[n], skinC = new int[n];
                for (int i = 0; i < n; i++) {
                    float t = (float) i / (n - 1);
                    float y = t * jh;
                    float wob = .12f * t;
                    axis[i] = new Vector3f(Mth.cos(a) * (off + lean * y) + noise(y * .4f - age * .3f, k, seed) * wob, y,
                            Mth.sin(a) * (off + lean * y) + noise(y * .4f - age * .3f, k + 9, seed) * wob);
                    float base = r * (k == 0 ? .42f : .26f + .1f * hash(seed + k * 11));
                    rad[i] = base * (1 + .7f * t * t) * (1 + .12f * Mth.sin(y * 2 - age * 1.5f));
                    skin[i] = rad[i] * 1.18f;
                    float top = Mth.clamp((1 - t) * 6, 0, 1);
                    col[i] = argb(mix(mix(SHALLOW, FOAM_WHITE, .45f), FOAM_WHITE, t), (.55f + .3f * t) * top * alive);
                    skinC[i] = argb(FOAM_WHITE, Mth.clamp(t * 1.6f, 0, .8f) * top * alive);
                }
                tube(water, p, axis, rad, col, 10, .45f, 1.6f, age * .9f, .45f, .2f, seed + k * 13, light, .7f);
                tops[k] = new Vector3f[]{axis[n - 1], new Vector3f(axis[n - 1]).sub(axis[n - 2]).normalize()};
            }
        }

        VertexConsumer foam = translucent(buffer, FOAM);
        p = poseStack.last();
        if (pool > 0 || erupting) {
            // White water boiling on the pool, rings of it running out over the ground
            disc(foam, p, .05f, 0, r * (.6f + .9f * Math.max(pool, erupting ? 1 : 0)), 24, 1.6f, age * .03f, -age * .02f,
                    argb(FOAM_WHITE, .8f * Math.max(pool, erupting ? alive : 0)), argb(FOAM_WHITE, .1f), light);
            if (erupting) {
                for (int k = 0; k < 3; k++) {
                    float t = (age * .045f + k / 3f) % 1;
                    disc(foam, p, .06f, r * (1.1f + 2.6f * t), r * (1.45f + 2.8f * t), 32, 1.6f, age * .02f, 0,
                            argb(FOAM_WHITE, .55f * (1 - t) * alive), argb(FOAM_WHITE, 0), light);
                }
            }
        }
        if (erupting) {
            // The foam skins of the jets, and sheets of water falling back round the column
            for (int k = 0; k < 14; k++) {
                float a = k * Mth.TWO_PI / 14 + hash(seed + k * 17) * .4f + age * .02f;
                int m = 9;
                Vector3f[] pts = new Vector3f[m], side = new Vector3f[m];
                float[] w = new float[m];
                int[] col = new int[m];
                float top = h * (.75f + .25f * hash(seed + k * 19));
                float spill = r * (.6f + .5f * hash(seed + k * 23));
                for (int i = 0; i < m; i++) {
                    float u = (float) i / (m - 1);
                    float out = r * .5f + spill * Mth.sin(u * 1.3f) + r * 1.3f * u;
                    float y = top + .7f * Mth.sin(u * Mth.PI * .6f) - (top * .95f) * u * u * (1 + end);
                    pts[i] = new Vector3f(Mth.cos(a) * out, Math.max(.05f, y), Mth.sin(a) * out);
                    side[i] = new Vector3f(-Mth.sin(a), 0, Mth.cos(a));
                    w[i] = (.35f + .7f * u) * (.7f + .3f * noise(u * 3 - age * .2f, k, seed));
                    col[i] = argb(FOAM_WHITE, .7f * (1 - u * .85f) * Mth.clamp(u * 6, 0, 1));
                }
                ribbon(foam, p, pts, side, w, col, .8f, -age * .25f, light, true);
            }
        }

        // The crown of spray churning on top, white water boiling at its foot
        VertexConsumer spray = translucent(buffer, SPRAY);
        if (erupting) {
            for (int k = 0; k < JETS; k++) {
                Vector3f top = tops[k][0];
                for (int j = 0; j < 4; j++) {
                    float t = (age * .06f + hash(seed + k * 31 + j * 7)) % 1;
                    float a = hash(seed + k * 37 + j) * Mth.TWO_PI;
                    float spread = r * (.3f + 1.1f * t) * (1 + end);
                    puff(spray, poseStack, view, top.x + Mth.cos(a) * spread, top.y + .2f + 1.2f * t - 2.5f * end * t, top.z + Mth.sin(a) * spread,
                            (1.1f + 1.4f * t) * (k == 0 ? 1.2f : 1), age * .03f + k + j, (k + j) & 3, argb(FOAM_WHITE, .9f * (1 - t * t) * alive), light);
                }
            }
            for (int k = 0; k < 12; k++) {
                float t = (age * .08f + hash(seed + k * 41)) % 1;
                float a = k * Mth.TWO_PI / 12 + hash(seed + k) * .5f;
                float out = r * (.7f + .8f * t);
                puff(spray, poseStack, view, Mth.cos(a) * out, .25f + .7f * t, Mth.sin(a) * out, 1 + .9f * t, k * 2.1f + age * .05f, k & 3,
                        argb(FOAM_WHITE, .8f * (1 - t) * alive), light);
            }
        } else if (pool > 0) {
            // Bubbles bursting on the pool
            for (int k = 0; k < 8; k++) {
                float t = (age * .12f + hash(seed + k)) % 1;
                float a = hash(seed + k * 3) * Mth.TWO_PI, rr = r * .7f * hash(seed + k * 5);
                puff(spray, poseStack, view, Mth.cos(a) * rr, .1f + .2f * t, Mth.sin(a) * rr, .3f + .4f * t * pool, k, k & 3,
                        argb(FOAM_WHITE, .7f * (1 - t) * pool), light);
            }
        }

        // Drops arcing out of the crown in fountains, and falling
        if (erupting) {
            VertexConsumer drops = translucent(buffer, STREAK);
            p = poseStack.last();
            Vector3f eye = view.eye();
            for (int k = 0; k < 28; k++) {
                Vector3f top = tops[k % JETS][0];
                float a = hash(seed + k * 43) * Mth.TWO_PI;
                float sp = .1f + .14f * hash(seed + k * 47);
                Vector3f v = new Vector3f(Mth.cos(a) * sp, .12f + .2f * hash(seed + k * 53), Mth.sin(a) * sp);
                for (int j = 0; j < 4; j++) {
                    float t = ((age * .5f + j * 7 + hash(seed + k) * 30) % 30);
                    Vector3f head = thrown(top, v, t, .03f);
                    if (head.y < 0) {
                        continue;
                    }
                    Vector3f tail = thrown(top, v, Math.max(0, t - 2.5f), .03f);
                    streak(drops, p, eye, head, tail, .09f, argb(FOAM_WHITE, .9f * alive), light);
                }
            }
        }

        // Steam billowing round it all
        VertexConsumer mist = translucent(buffer, MIST);
        float steam = erupting ? 1 : warn * .5f;
        if (end > 0) {
            steam = 1 - end * .5f;
        }
        for (int k = 0; k < 14; k++) {
            float t = (age * .02f + hash(seed + k * 59)) % 1;
            float a = hash(seed + k * 61) * Mth.TWO_PI + t * .8f;
            float out = r * (.6f + 1.4f * hash(seed + k * 67)) + t * 1.2f;
            float y = (erupting ? h * (.15f + .85f * hash(seed + k * 71)) : .3f) + t * 2.5f;
            puff(mist, poseStack, view, Mth.cos(a) * out, y, Mth.sin(a) * out, (erupting ? 2.2f : 1) + 2.5f * t, k + age * .01f, -1,
                    argb(0xF4FAFF, .32f * Mth.sin(t * Mth.PI) * steam), light);
        }
    }
}
