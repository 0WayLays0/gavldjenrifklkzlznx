package com.waylays.arcanes.client.render.gem;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The diamond's newer spells. The diamond refracts and endures.
 * <ul>
 * <li>Diamond Lance: a long point of diamond shot down a line faster than the eye, a white beam behind it fanning into
 * the seven colours of the rainbow; it breaks into fire where it stops.</li>
 * <li>Diamond Dust: a dome of diamond dust glittering in every colour, small diamonds tumbling in it, a white haze.</li>
 * <li>Diamond Bastion: a great cluster of diamond bursts out of the ground, a dome of pale light over it; at the end
 * its crystals fly apart.</li>
 * </ul>
 */
public final class DiamondModels {
    private static final Vector3f U = new Vector3f(1, 0, 0), V = new Vector3f(0, 0, -1);
    private static final int DIAMOND = GemMesh.DIAMOND;
    private static final int[] RAINBOW = {0xFF3A3A, 0xFF9A2A, 0xFFE83A, 0x4AE85A, 0x3AB8FF, 0x5A5AFF, 0xC05AFF};

    private DiamondModels() {}

    // ---------------------------------------------------------------- lance

    /** From the caster's eye (the pose's origin) along {@code dir}, {@code length} long; shot {@code age} ticks ago. */
    public static void lance(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f dir, float age, int life, float length, int flight,
                             int seed, int light) {
        Vector3f d = new Vector3f(dir);
        if (d.lengthSquared() < 1.0E-6f) {
            d.set(0, 0, 1);
        }
        d.normalize();
        float reach = BossMesh.ramp(age, 0, flight);
        float fade = 1 - BossMesh.ramp(age, flight, life);
        Vector3f tip = new Vector3f(d).mul(length * reach);
        PoseStack.Pose p = poseStack.last();
        // The beam and its rainbow, fanning out behind the point
        VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
        Vector3f side = BossMesh.across(d), up = new Vector3f(side).cross(d).normalize();
        GemMesh.ray(beam, p, view.eye(), new Vector3f(d).mul(.6f), tip, 0xFFFFFF, .3f, fade, age);
        for (int k = 0; k < RAINBOW.length; k++) {
            float off = (k - 3) * .09f;
            Vector3f start = new Vector3f(d).mul(.6f);
            Vector3f end = new Vector3f(tip).add(new Vector3f(side).mul(off * length * .12f)).add(new Vector3f(up).mul(off * .3f));
            RitualMesh.shaft(beam, p, view.eye(), start, end, .16f, BossMesh.glow(RAINBOW[k], .55f * fade), light);
        }
        if (reach < 1) {
            GemMesh.jewel(poseStack, buffer, view, GemMesh.Cut.CRYSTAL, tip, RubyModels.along(d, age), .45f, 4, DIAMOND, .7f, age * 4, 1, true, seed, light);
        } else {
            poseStack.pushPose();
            poseStack.translate(tip.x, tip.y, tip.z);
            RubyModels.burst(poseStack, buffer, view, (age - flight) / 10, DIAMOND, .9f, seed, light);
            VertexConsumer sparkle = BossMesh.light(buffer, GemMesh.SPARKLE);
            float t = Mth.clamp((age - flight) / 10, 0, 1);
            for (int k = 0; k < 12; k++) {
                float a = k * Mth.TWO_PI / 12;
                Vector3f o = new Vector3f(Mth.cos(a), Mth.sin(a * 2) * .5f, Mth.sin(a)).mul(.3f + 1.8f * t);
                RitualMesh.billboard(sparkle, poseStack, view, o, .5f * (1 - t), k, -1, BossMesh.glow(RAINBOW[k % 7], 1 - t), light);
            }
            poseStack.popPose();
        }
    }

    // ---------------------------------------------------------------- dust

    public static void dust(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int life, float radius, int seed, int light) {
        float k = BossMesh.ramp(age, 0, 10) * (1 - BossMesh.ramp(age, life - 12, life));
        if (k <= .01f) {
            return;
        }
        PoseStack.Pose p = poseStack.last();
        // The haze
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(0, 1.2f, 0), radius * 2.6f * k, 0, -1, BossMesh.glow(0xDFF6FF, .18f), light);
        // Glints of every colour swirling in a dome
        VertexConsumer sparkle = BossMesh.light(buffer, GemMesh.SPARKLE);
        for (int i = 0; i < 220; i++) {
            float h = BossMesh.hash(seed + i * 7), h2 = BossMesh.hash(seed + i * 13), h3 = BossMesh.hash(seed + i * 19);
            float a = h * Mth.TWO_PI + age * (.03f + .04f * h2) * (i % 2 == 0 ? 1 : -1);
            float rr = radius * k * (float) Math.sqrt(h2);
            float y = .1f + (radius * .8f) * h3 * (1 - rr / (radius + .01f) * .5f) + .2f * Mth.sin(age * .05f + i);
            float tw = Mth.sin(age * (.2f + .2f * h3) + i * 3.1f);
            if (tw < -.2f) {
                continue;
            }
            int c = Mth.hsvToRgb((h * 5 + age * .01f) % 1, .45f, 1);
            RitualMesh.billboard(sparkle, poseStack, view, new Vector3f(Mth.cos(a) * rr, y, Mth.sin(a) * rr), .12f + .2f * Math.max(0, tw), age * .05f + i, -1,
                    BossMesh.glow(i % 4 == 0 ? 0xFFFFFF : c, .4f + .6f * Math.max(0, tw)), light);
        }
        // Small diamonds tumbling in it
        VertexConsumer glass = BossMesh.see(buffer, GemMesh.FACET);
        Vector3f[] gems = new Vector3f[10];
        for (int i = 0; i < gems.length; i++) {
            float a = i * Mth.TWO_PI / gems.length + age * .02f;
            float rr = radius * k * (.3f + .5f * BossMesh.hash(seed + i * 3));
            gems[i] = new Vector3f(Mth.cos(a) * rr, .6f + 1.4f * BossMesh.hash(seed + i * 5) + .2f * Mth.sin(age * .07f + i), Mth.sin(a) * rr);
            GemMesh.gem(glass, p, GemMesh.Cut.BRILLIANT, gems[i], new Quaternionf().rotationXYZ(age * .05f + i, age * .07f, i), .28f * k, 1, DIAMOND, .7f,
                    age * 4 + i * 11, light);
        }
        for (int i = 0; i < gems.length; i++) {
            GemMesh.sparkles(poseStack, buffer, view, GemMesh.Cut.BRILLIANT, gems[i], new Quaternionf().rotationXYZ(age * .05f + i, age * .07f, i), .28f * k, 1, DIAMOND,
                    age * 2, true, seed + i, light);
        }
        VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
        BossMesh.annulus(ring, poseStack.last(), new Vector3f(0, .1f, 0), U, V, radius * .85f * k, radius * k, 48, 1, age * .02f, 0, BossMesh.glow(0xDFF6FF, 0),
                BossMesh.glow(0xDFF6FF, .5f), light, false);
    }

    // ---------------------------------------------------------------- bastion

    /** Out of the ground at the pose's origin, guarding {@code radius} round; it shatters {@code shatter} ticks before its end. */
    public static void bastion(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int life, float radius, int shatter, int seed,
                               int light) {
        float rise = BossMesh.ramp(age, 0, 10);
        float hit = age - (life - shatter);
        PoseStack.Pose p = poseStack.last();
        if (hit < 0) {
            float hum = .85f + .15f * Mth.sin(age * .12f);
            // The dome of pale light
            VertexConsumer dome = BossMesh.see(buffer, BossMesh.SHEEN);
            BossMesh.sphere(dome, p, new Vector3f(0, 0, 0), radius * rise, 10, 32, age * .002f, 0, .5f, BossMesh.argb(0xCFF4FF, .16f * hum), light);
            // The cluster
            VertexConsumer glass = BossMesh.see(buffer, GemMesh.FACET);
            List3 cluster = cluster(seed);
            for (int i = 0; i < cluster.n; i++) {
                float grow = BossMesh.ramp(age, i * .8f, 8 + i * .8f);
                Vector3f at = new Vector3f(cluster.at[i]).mul(1, grow, 1);
                GemMesh.gem(glass, p, GemMesh.Cut.CRYSTAL, at, RubyModels.along(cluster.dir[i], i), cluster.size[i] * grow, cluster.stretch[i], DIAMOND, .62f,
                        age * 3 + i * 13, light);
            }
            for (int i = 0; i < cluster.n; i += 2) {
                float grow = BossMesh.ramp(age, i * .8f, 8 + i * .8f);
                GemMesh.sparkles(poseStack, buffer, view, GemMesh.Cut.CRYSTAL, new Vector3f(cluster.at[i]).mul(1, grow, 1), RubyModels.along(cluster.dir[i], i),
                        cluster.size[i] * grow, cluster.stretch[i], DIAMOND, age * 2, true, seed + i, light);
            }
            VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
            RitualMesh.billboard(glow, poseStack, view, new Vector3f(0, 1.6f, 0), 4.5f * rise, 0, -1, BossMesh.glow(0xCFF4FF, .35f * hum), light);
            VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
            BossMesh.annulus(ring, poseStack.last(), new Vector3f(0, .1f, 0), U, V, radius * .93f * rise, radius * rise, 64, 1, age * .01f, 0, BossMesh.glow(DIAMOND,
                    0), BossMesh.glow(0xFFFFFF, .6f * hum), light, false);
            return;
        }
        // ---- it shatters: its crystals fly apart
        float t = Mth.clamp(hit / shatter, 0, 1);
        float out = 1 - (1 - t) * (1 - t);
        VertexConsumer glass = BossMesh.see(buffer, GemMesh.FACET);
        List3 cluster = cluster(seed);
        for (int i = 0; i < cluster.n; i++) {
            Vector3f fly = new Vector3f(cluster.at[i].x, .6f, cluster.at[i].z);
            if (fly.lengthSquared() < .01f) {
                fly.set(0, 1, 0);
            }
            fly.normalize();
            Vector3f at = new Vector3f(cluster.at[i]).add(new Vector3f(fly).mul(5 * out)).add(0, -2 * t * t, 0);
            GemMesh.gem(glass, p, GemMesh.Cut.CRYSTAL, at, RubyModels.along(cluster.dir[i], i + hit * .4f), cluster.size[i] * (1 - t * .7f), cluster.stretch[i], DIAMOND,
                    .62f * (1 - t * .6f), age * 3, light);
        }
        poseStack.pushPose();
        poseStack.translate(0, 1.5f, 0);
        RubyModels.burst(poseStack, buffer, view, t, DIAMOND, 2.2f, seed, light);
        poseStack.popPose();
        VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
        float rr = radius * (.4f + 1.4f * out);
        BossMesh.annulus(ring, poseStack.last(), new Vector3f(0, .12f, 0), U, V, rr * .8f, rr, 56, 1, 0, 0, BossMesh.glow(0xFFFFFF, 0), BossMesh.glow(0xFFFFFF,
                .9f * (1 - t)), light, true);
    }

    /** The cluster's crystals: where each stands, which way it points, how big and how drawn out. */
    private record List3(int n, Vector3f[] at, Vector3f[] dir, float[] size, float[] stretch) {}

    private static List3 cluster(int seed) {
        int n = 11;
        Vector3f[] at = new Vector3f[n], dir = new Vector3f[n];
        float[] size = new float[n], stretch = new float[n];
        at[0] = new Vector3f(0, 1.5f, 0);
        dir[0] = new Vector3f(0, 1, 0);
        size[0] = 1.1f;
        stretch[0] = 2.8f;
        for (int i = 1; i < n; i++) {
            float a = i * Mth.TWO_PI / (n - 1) + BossMesh.hash(seed + i) * .4f;
            boolean inner = i % 2 == 0;
            float r = inner ? .55f : 1.05f;
            Vector3f out = new Vector3f(Mth.cos(a), 0, Mth.sin(a));
            dir[i] = new Vector3f(out).mul(inner ? .35f : .75f).add(0, 1, 0).normalize();
            size[i] = inner ? .62f : .42f;
            stretch[i] = inner ? 2.2f : 1.8f;
            at[i] = new Vector3f(out).mul(r).add(new Vector3f(dir[i]).mul(size[i] * stretch[i] * .45f));
        }
        for (int i = 0; i < n; i++) {
            at[i].mul(1.5f);
            size[i] *= 1.5f;
        }
        return new List3(n, at, dir, size, stretch);
    }
}
