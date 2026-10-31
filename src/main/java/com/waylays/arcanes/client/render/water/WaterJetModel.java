package com.waylays.arcanes.client.render.water;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import static com.waylays.arcanes.client.render.water.WaterMesh.*;

/**
 * Pressure Jet, the way a jet of water under great pressure really looks: a glassy core leaving the hand, thin and
 * hard, wrapped in a sheath that swells in bulges racing along it; further out the water tears, turns white and
 * frays, drops peeling off it; its line whips a little. Where it strikes, a sheet of water fans back off the surface,
 * spray and drops fly, mist hangs.
 */
public final class WaterJetModel {
    private WaterJetModel() {}

    /**
     * {@code from} (the hand) and {@code end} (where it stops) in the pose's frame; {@code hit} if it struck something
     * there (else it just frays out at its full reach).
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, View view, Vector3f from, Vector3f end, boolean hit, float age, int seed, int light) {
        Vector3f d = new Vector3f(end).sub(from);
        float len = d.length();
        if (len < .1f) {
            return;
        }
        Vector3f dir = new Vector3f(d).div(len);
        Vector3f s1 = Math.abs(dir.y) < .95f ? new Vector3f(dir).cross(0, 1, 0).normalize() : new Vector3f(1, 0, 0);
        Vector3f s2 = new Vector3f(dir).cross(s1).normalize();
        float grow = Mth.clamp(age / 3, 0, 1);
        float l = len * grow;
        int n = Math.max(12, (int) (l * 3));
        Vector3f[] axis = new Vector3f[n];
        float[] core = new float[n], sheath = new float[n], skin = new float[n];
        int[] coreC = new int[n], sheathC = new int[n], skinC = new int[n];
        for (int i = 0; i < n; i++) {
            float t = (float) i / (n - 1);
            float dist = t * l;
            // Its line whips a little, more further out
            float whip = .01f + .07f * t * t;
            float w1 = noise(dist * .5f - age * .45f, seed, 0) * whip, w2 = noise(dist * .5f - age * .45f, seed + 7.3f, 3) * whip;
            axis[i] = new Vector3f(from).add(new Vector3f(dir).mul(dist)).add(new Vector3f(s1).mul(w1)).add(new Vector3f(s2).mul(w2));
            float bulge = Math.max(0, Mth.sin(dist * 2.6f - age * 2.3f));
            core[i] = (.032f + .018f * t) * (1 + .25f * bulge);
            sheath[i] = (.06f + .07f * t) * (1 + .45f * bulge * (.5f + t));
            skin[i] = sheath[i] * (1.25f + .6f * t);
            float fade = Mth.clamp((1 - t) * 14, 0, 1);
            coreC[i] = argb(mix(FOAM_WHITE, SHALLOW, .15f + .2f * t), .95f * fade);
            sheathC[i] = argb(mix(SHALLOW, FOAM_WHITE, .25f + .3f * t), (.5f - .1f * t) * fade);
            skinC[i] = argb(FOAM_WHITE, Mth.clamp((t - .45f) * 1.4f, 0, .5f) * fade);
        }
        VertexConsumer water = translucent(buffer, WATER);
        PoseStack.Pose p = poseStack.last();
        tube(water, p, axis, core, coreC, 8, .2f, 3, age * 1.6f, 1.2f, 0, seed, light, .6f);
        tube(water, p, axis, sheath, sheathC, 10, .3f, 2.2f, age * 1.1f, .8f, .35f, seed + 31, light, .5f);

        VertexConsumer foam = translucent(buffer, FOAM);
        p = poseStack.last();
        // The torn white skin near its end, and threads of water fraying off it
        tube(foam, p, axis, skin, skinC, 10, .6f, 1.6f, age * 1.3f, .6f, -.4f, seed + 57, light, .8f);
        for (int k = 0; k < 6; k++) {
            float t0 = .2f + .1f * k + .05f * hash(seed + k);
            int m = 10;
            Vector3f[] th = new Vector3f[m];
            float[] tr = new float[m];
            int[] tc = new int[m];
            for (int i = 0; i < m; i++) {
                float u = (float) i / (m - 1);
                float t = t0 + (1 - t0) * u;
                int at = Math.min(n - 1, (int) (t * (n - 1)));
                float a = k * 2.4f + u * 5 - age * .4f;
                float off = sheath[at] * (1 + 2.5f * u * u);
                th[i] = new Vector3f(axis[at]).add(new Vector3f(s1).mul(Mth.cos(a) * off)).add(new Vector3f(s2).mul(Mth.sin(a) * off));
                tr[i] = .018f + .012f * Mth.sin(u * Mth.PI);
                tc[i] = argb(FOAM_WHITE, .7f * Mth.sin(u * Mth.PI));
            }
            tube(foam, p, th, tr, tc, 4, .3f, 3, age * 1.5f, 1, 0, seed + k * 5, light, .8f);
        }
        // Water swirling off the hand where it leaves it
        for (int k = 0; k < 3; k++) {
            int m = 8;
            Vector3f[] pts = new Vector3f[m], side = new Vector3f[m];
            float[] w = new float[m];
            int[] col = new int[m];
            for (int i = 0; i < m; i++) {
                float u = (float) i / (m - 1);
                float a = k * Mth.TWO_PI / 3 + age * .7f + u * 2.4f;
                float r = .2f + .08f * u;
                Vector3f radial = new Vector3f(s1).mul(Mth.cos(a)).add(new Vector3f(s2).mul(Mth.sin(a)));
                pts[i] = new Vector3f(from).add(new Vector3f(dir).mul(-.05f + .45f * u)).add(new Vector3f(radial).mul(r));
                side[i] = new Vector3f(dir);
                w[i] = .09f * Mth.sin(u * Mth.PI) + .02f;
                col[i] = argb(FOAM_WHITE, .75f * Mth.sin(u * Mth.PI));
            }
            ribbon(foam, p, pts, side, w, col, 1.5f, age * .3f, light, true);
        }
        // The sheet fanning back off what it strikes
        if (hit && grow >= 1) {
            for (int k = 0; k < 22; k++) {
                float phi = k * Mth.TWO_PI / 22 + hash(seed + k) * .3f;
                float theta = .9f + .55f * hash(seed + k * 3);
                float flicker = .8f + .35f * noise(age * .35f, k, seed);
                Vector3f out = new Vector3f(dir).mul(-Mth.cos(theta)).add(new Vector3f(s1).mul(Mth.cos(phi) * Mth.sin(theta)))
                        .add(new Vector3f(s2).mul(Mth.sin(phi) * Mth.sin(theta))).mul(.42f * flicker);
                int m = 7;
                Vector3f[] pts = new Vector3f[m], side = new Vector3f[m];
                float[] w = new float[m];
                int[] col = new int[m];
                Vector3f across = new Vector3f(out).cross(dir);
                if (across.lengthSquared() < 1.0E-6f) {
                    across.set(s1);
                }
                across.normalize();
                for (int i = 0; i < m; i++) {
                    float u = (float) i / (m - 1) * 5;
                    pts[i] = thrown(end, out, u, .06f);
                    side[i] = across;
                    w[i] = (.1f + .4f * u / 5) * flicker;
                    col[i] = argb(i < 2 ? mix(SHALLOW, FOAM_WHITE, .6f) : FOAM_WHITE, .9f * (1 - u / 5));
                }
                ribbon(foam, p, pts, side, w, col, 1.2f, age * .5f, light, true);
            }
        }

        // Clumps of spray: where it frays out, and where it strikes
        VertexConsumer spray = translucent(buffer, SPRAY);
        for (int k = 0; k < 10; k++) {
            float t = (age * .07f + hash(seed + k * 13)) % 1;
            float at = l * (.55f + .45f * hash(seed + k * 17)) + t * .6f;
            if (at > l + .3f) {
                continue;
            }
            Vector3f c = new Vector3f(from).add(new Vector3f(dir).mul(Math.min(at, l)))
                    .add(new Vector3f(s1).mul((hash(seed + k) - .5f) * .3f)).add(new Vector3f(s2).mul((hash(seed + k * 3) - .5f) * .3f));
            puff(spray, poseStack, view, c.x, c.y, c.z, .35f + .5f * t, age * .05f + k, k & 3, argb(FOAM_WHITE, .7f * (1 - t)), light);
        }
        if (hit && grow >= 1) {
            for (int k = 0; k < 12; k++) {
                float t = (age * .1f + hash(seed + k * 29)) % 1;
                Vector3f c = new Vector3f(end).sub(new Vector3f(dir).mul(.2f + .5f * t))
                        .add(new Vector3f(s1).mul((hash(seed + k * 5) - .5f) * 1.4f * t)).add(new Vector3f(s2).mul((hash(seed + k * 7) - .5f) * 1.4f * t));
                puff(spray, poseStack, view, c.x, c.y, c.z, .7f + 1.2f * t, k * 1.7f + age * .04f, (k + 1) & 3, argb(FOAM_WHITE, .9f * (1 - t)), light);
            }
        }

        // Drops peeling off it, and flying back off what it strikes
        VertexConsumer drops = translucent(buffer, STREAK);
        p = poseStack.last();
        Vector3f eye = view.eye();
        for (int k = 0; k < 40; k++) {
            float t = (age * .12f + hash(seed + k * 7)) % 1;
            float d0 = l * hash(seed + k * 11) * .9f;
            float a = hash(seed + k * 19) * Mth.TWO_PI;
            Vector3f radial = new Vector3f(s1).mul(Mth.cos(a)).add(new Vector3f(s2).mul(Mth.sin(a)));
            float travel = t * 2.2f;
            float frac = d0 / Math.max(.1f, l);
            Vector3f head = new Vector3f(from).add(new Vector3f(dir).mul(Math.min(l + .5f, d0 + travel)))
                    .add(new Vector3f(radial).mul(.12f + .25f * frac + .3f * t * frac)).add(0, -.25f * t * t, 0);
            Vector3f tail = new Vector3f(head).sub(new Vector3f(dir).mul(.35f + .3f * frac));
            streak(drops, p, eye, head, tail, .06f, argb(FOAM_WHITE, .9f * (1 - t)), light);
        }
        if (hit && grow >= 1) {
            for (int k = 0; k < 30; k++) {
                float t = (age * .09f + hash(seed + k * 23)) % 1;
                float phi = hash(seed + k * 31) * Mth.TWO_PI, theta = .7f + .7f * hash(seed + k * 37);
                Vector3f v = new Vector3f(dir).mul(-Mth.cos(theta)).add(new Vector3f(s1).mul(Mth.cos(phi) * Mth.sin(theta)))
                        .add(new Vector3f(s2).mul(Mth.sin(phi) * Mth.sin(theta))).mul(.25f + .2f * hash(seed + k));
                Vector3f head = thrown(end, v, t * 12, .045f);
                Vector3f tail = thrown(end, v, Math.max(0, t * 12 - 1.6f), .045f);
                streak(drops, p, eye, head, tail, .08f, argb(FOAM_WHITE, .95f * (1 - t)), light);
            }
        }

        // Mist hanging where it strikes
        if (hit && grow >= 1) {
            VertexConsumer mist = translucent(buffer, MIST);
            for (int k = 0; k < 7; k++) {
                float t = (age * .03f + hash(seed + k * 41)) % 1;
                Vector3f c = new Vector3f(end).sub(new Vector3f(dir).mul(.4f + .6f * t)).add(new Vector3f(s1).mul((hash(seed + k) - .5f) * 1.6f))
                        .add(0, .3f * t, 0);
                puff(mist, poseStack, view, c.x, c.y, c.z, 1 + 1.4f * t, k + age * .01f, -1, argb(0xE8F6FF, .35f * Mth.sin(t * Mth.PI)), light);
            }
        }
    }
}
