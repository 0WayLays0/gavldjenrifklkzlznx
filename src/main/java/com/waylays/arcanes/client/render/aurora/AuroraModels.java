package com.waylays.arcanes.client.render.aurora;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.gem.GemMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Aurora Magic's nearer spells.
 * <ul>
 * <li>Aurora Lance: a point of ice flying, three ribbons of the northern lights twisting round each other behind it,
 * stars shed in its wake; it breaks into light where it stops.</li>
 * <li>Polar Wisps: a wisp of light, a star turning in its heart, a ribbon of colours streaming behind it.</li>
 * <li>Borealis Curtain: a curtain of the northern lights unrolling down to the ground, folds rippling, a fainter curtain
 * behind it, its hem burning green on the ground.</li>
 * <li>Aurora Glide: a wave of light running from where the caster set off to where they are, low curtains along it.</li>
 * <li>Aurora Mantle: low curtains of light turning round the caster, ribbons spiralling up round them.</li>
 * <li>Boreal Blessing: a ring of curtains coming down out of the sky round the caster, then rising away in motes.</li>
 * </ul>
 */
public final class AuroraModels {
    private AuroraModels() {}

    private static Vector3f unit(Vector3f d) {
        Vector3f v = new Vector3f(d);
        if (v.lengthSquared() < 1.0E-6f) {
            v.set(0, 0, 1);
        }
        return v.normalize();
    }

    /** Light flung out where something of the aurora breaks, {@code t} (0 to 1) of the way through. */
    static void burst(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float t, float size, int seed, int light) {
        if (t >= 1) {
            return;
        }
        float out = 1 - (1 - t) * (1 - t), flash = (1 - t) * (1 - t);
        PoseStack.Pose p = poseStack.last();
        VertexConsumer band = BossMesh.light(buffer, AuroraMesh.BAND);
        for (int k = 0; k < 8; k++) {
            float a = k * Mth.TWO_PI / 8 + seed;
            Vector3f d = new Vector3f(Mth.cos(a), .4f * Mth.sin(a * 3 + seed), Mth.sin(a)).normalize();
            Vector3f[] pts = new Vector3f[6];
            for (int i = 0; i < 6; i++) {
                float s = out * (1 - i * .15f);
                pts[i] = new Vector3f(d).mul(size * 2.5f * s).add(0, .3f * Mth.sin(i + t * 6), 0);
            }
            AuroraMesh.ribbon(band, p, view.eye(), pts, .35f * size, flash, t * 40 + k * 10, light);
        }
        AuroraMesh.glow(poseStack, buffer, view, new Vector3f(), size * (1 + 3 * t), 0xFFFFFF, flash, light);
        AuroraMesh.glow(poseStack, buffer, view, new Vector3f(), size * (2.5f + 3 * t), AuroraMesh.TEAL, flash * .7f, light);
        AuroraMesh.stars(poseStack, buffer, view, new Vector3f(0, -size, 0), size * 2.5f * out + .2f, size * 2.5f, t * 60, 14, flash, seed, light);
    }

    // ---------------------------------------------------------------- lance

    public static void lance(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f dir, float age, boolean done, float since, int seed, int light) {
        Vector3f d = unit(dir);
        if (done) {
            burst(poseStack, buffer, view, since / 10, .9f, seed, light);
            return;
        }
        float grow = BossMesh.ramp(age, 0, 3);
        PoseStack.Pose p = poseStack.last();
        AuroraMesh.ice(buffer, p, new Vector3f(d).mul(.25f), d, .42f * grow, 3.2f, age * 3, light);
        // Three ribbons twisting round each other behind it, and a bright core
        Vector3f u = BossMesh.across(d), v = new Vector3f(u).cross(d).normalize();
        VertexConsumer band = BossMesh.light(buffer, AuroraMesh.BAND);
        int n = 18;
        for (int s = 0; s < 3; s++) {
            Vector3f[] pts = new Vector3f[n];
            for (int i = 0; i < n; i++) {
                float t = (float) i / (n - 1);
                float a = s * Mth.TWO_PI / 3 + age * .5f + t * 7;
                float r = (.05f + .45f * t) * grow;
                pts[i] = new Vector3f(d).mul(-.3f - 5 * t * grow).add(new Vector3f(u).mul(Mth.cos(a) * r)).add(new Vector3f(v).mul(Mth.sin(a) * r));
            }
            AuroraMesh.ribbon(band, p, view.eye(), pts, .45f, .9f, age * 3 + s * 33, light);
        }
        Vector3f[] core = {new Vector3f(d).mul(-.1f), new Vector3f(d).mul(-1.5f), new Vector3f(d).mul(-3.2f)};
        RitualMesh.thread(band, p, view.eye(), core, .28f, new int[] {BossMesh.glow(0xFFFFFF, .9f), BossMesh.glow(AuroraMesh.TEAL, .6f), BossMesh.glow(AuroraMesh.VIOLET, 0)},
                light);
        AuroraMesh.glow(poseStack, buffer, view, new Vector3f(), 1.8f * grow, AuroraMesh.TEAL, .45f, light);
        AuroraMesh.stars(poseStack, buffer, view, new Vector3f(d).mul(-2.5f).add(0, -.5f, 0), 1, 1, age * 2, 10, .9f, seed, light);
    }

    // ---------------------------------------------------------------- wisps

    /** Moving by {@code motion} (blocks a tick, its last move). */
    public static void wisp(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f motion, float age, boolean done, float since, int index, int seed,
                            int light) {
        if (done) {
            burst(poseStack, buffer, view, since / 10, .6f, seed, light);
            return;
        }
        float grow = BossMesh.ramp(age, 0, 6);
        int hue = AuroraMesh.hue((index * .27f) % 1);
        float speed = motion.length();
        Vector3f back = speed > .02f ? new Vector3f(motion).normalize().negate() : new Vector3f(0, -1, 0);
        Vector3f u = BossMesh.across(back), v = new Vector3f(u).cross(back).normalize();
        PoseStack.Pose p = poseStack.last();
        VertexConsumer band = BossMesh.light(buffer, AuroraMesh.BAND);
        int n = 16;
        float length = 1 + Math.min(3.5f, speed * 4);
        for (int s = 0; s < 2; s++) {
            Vector3f[] pts = new Vector3f[n];
            for (int i = 0; i < n; i++) {
                float t = (float) i / (n - 1);
                float a = age * .4f + t * 6 + s * Mth.PI;
                float r = .25f * t;
                pts[i] = new Vector3f(back).mul(t * length).add(new Vector3f(u).mul(Mth.cos(a) * r)).add(new Vector3f(v).mul(Mth.sin(a) * r));
            }
            AuroraMesh.ribbon(band, p, view.eye(), pts, .4f * grow, .85f, age * 4 + index * 20 + s * 30, light);
        }
        float pulse = .85f + .15f * Mth.sin(age * .5f + index);
        AuroraMesh.glow(poseStack, buffer, view, new Vector3f(), .6f * grow * pulse, 0xFFFFFF, 1, light);
        AuroraMesh.glow(poseStack, buffer, view, new Vector3f(), 1.6f * grow, hue, .7f * pulse, light);
        VertexConsumer star = BossMesh.light(buffer, GemMesh.SPARKLE);
        RitualMesh.billboard(star, poseStack, view, new Vector3f(), 1.1f * grow * pulse, age * .08f, -1, BossMesh.glow(0xFFFFFF, .9f), light);
    }

    // ---------------------------------------------------------------- curtain

    /** Across the ground at the pose's origin along {@code dir}, {@code length} long, {@code height} high. */
    public static void curtain(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f dir, float age, int life, float length, float height, int seed,
                               int light) {
        Vector3f d = unit(new Vector3f(dir.x, 0, dir.z));
        Vector3f side = new Vector3f(d.z, 0, -d.x);
        float unroll = BossMesh.ramp(age, 0, 14);
        float fade = 1 - BossMesh.ramp(age, life - 15, life);
        if (fade <= .01f) {
            return;
        }
        PoseStack.Pose p = poseStack.last();
        // Two curtains, the nearer brighter, the hem of each coming down from the sky as it unrolls
        for (int layer = 0; layer < 2; layer++) {
            int n = 26;
            Vector3f[] hems = new Vector3f[n];
            float top = height + .8f + layer * 1.2f;
            float hem = Mth.lerp(unroll, top - .3f, layer * .2f);
            for (int i = 0; i < n; i++) {
                float t = (float) i / (n - 1) - .5f;
                float fold = .7f * Mth.sin(t * 7 + age * .03f + layer * 1.3f) + .25f * Mth.sin(t * 17 - age * .05f);
                hems[i] = new Vector3f(d).mul(t * length * (1 + .1f * layer)).add(new Vector3f(side).mul(fold - layer * .8f)).add(0, hem, 0);
            }
            AuroraMesh.curtain(buffer, p, hems, top - hem, age, (layer == 0 ? 1 : .45f) * fade, age * .01f + layer * .3f, light);
        }
        // Its hem burning green on the ground, and the ground glowing under it
        if (unroll > .9f) {
            VertexConsumer band = BossMesh.light(buffer, AuroraMesh.BAND);
            int n = 26;
            Vector3f[] line = new Vector3f[n];
            for (int i = 0; i < n; i++) {
                float t = (float) i / (n - 1) - .5f;
                float fold = .7f * Mth.sin(t * 7 + age * .03f) + .25f * Mth.sin(t * 17 - age * .05f);
                line[i] = new Vector3f(d).mul(t * length).add(new Vector3f(side).mul(fold)).add(0, .15f, 0);
            }
            int[] col = new int[n];
            for (int i = 0; i < n; i++) {
                col[i] = BossMesh.glow(AuroraMesh.GREEN, .8f * fade * (.7f + .3f * Mth.sin(i * .9f + age * .3f)));
            }
            com.waylays.arcanes.client.render.sand.SandMesh.ribbon(band, p, view.eye(), line, AuroraMesh.fill(.6f, n), .5f, age * .02f, col, light);
            VertexConsumer pool = BossMesh.light(buffer, BossMesh.GLOW);
            for (int i = 0; i < 7; i++) {
                float t = (i / 6f - .5f) * length;
                BossMesh.pool(pool, p, d.x * t, .06f, d.z * t, 3.2f, i, BossMesh.glow(AuroraMesh.GREEN, .25f * fade), light);
            }
        }
        AuroraMesh.stars(poseStack, buffer, view, new Vector3f(0, .5f, 0), length * .55f, height + 2, age, 30, .8f * fade * unroll, seed, light);
    }

    // ---------------------------------------------------------------- glide

    /** From where the caster set off (the pose's origin) to where they are ({@code to}). */
    public static void glide(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f to, float age, int life, int glide, int seed, int light) {
        float fade = 1 - BossMesh.ramp(age, glide, life);
        float l = to.length();
        if (fade <= .01f || l < .3f) {
            return;
        }
        Vector3f d = new Vector3f(to).div(l);
        Vector3f side = BossMesh.across(d);
        PoseStack.Pose p = poseStack.last();
        int n = Math.max(6, (int) (l * 2));
        // Ribbons riding the wave
        VertexConsumer band = BossMesh.light(buffer, AuroraMesh.BAND);
        for (int s = 0; s < 3; s++) {
            Vector3f[] pts = new Vector3f[n];
            for (int i = 0; i < n; i++) {
                float t = (float) i / (n - 1);
                float wave = Mth.sin(t * 9 - age * .6f + s * 2.1f);
                pts[i] = new Vector3f(to).mul(t).add(new Vector3f(side).mul((s - 1) * .5f + .2f * wave)).add(0, .6f + .5f * wave + s * .25f, 0);
            }
            AuroraMesh.ribbon(band, p, view.eye(), pts, .5f, .9f * fade, age * 3 + s * 40, light);
        }
        // Low curtains along it
        Vector3f[] hems = new Vector3f[n];
        for (int i = 0; i < n; i++) {
            float t = (float) i / (n - 1);
            hems[i] = new Vector3f(to).mul(t).add(new Vector3f(side).mul(.4f * Mth.sin(t * 6 + age * .1f)));
        }
        AuroraMesh.curtain(buffer, p, hems, 2.2f, age, .8f * fade, age * .02f, light);
        AuroraMesh.stars(poseStack, buffer, view, new Vector3f(to).mul(.5f), l * .5f + .5f, 2.5f, age, 16, fade, seed, light);
    }

    // ---------------------------------------------------------------- mantle

    /** Round the caster (the pose's origin). The curtains stay below the eyes, so the caster still sees. */
    public static void mantle(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int life, float radius, int seed, int light) {
        float k = BossMesh.ramp(age, 0, 10) * (1 - BossMesh.ramp(age, life - 15, life));
        if (k <= .01f) {
            return;
        }
        PoseStack.Pose p = poseStack.last();
        for (int layer = 0; layer < 2; layer++) {
            int n = 30;
            float r = radius * (.85f + .25f * layer);
            Vector3f[] hems = AuroraMesh.arc(r, .02f, n, age * (layer == 0 ? .03f : -.025f), Mth.TWO_PI);
            AuroraMesh.curtain(buffer, p, hems, 1.25f - .25f * layer, age, (layer == 0 ? .8f : .45f) * k, age * .02f, light);
        }
        VertexConsumer band = BossMesh.light(buffer, AuroraMesh.BAND);
        for (int s = 0; s < 3; s++) {
            int n = 24;
            Vector3f[] pts = new Vector3f[n];
            for (int i = 0; i < n; i++) {
                float t = (float) i / (n - 1);
                float a = s * Mth.TWO_PI / 3 + t * 5 + age * .12f;
                pts[i] = new Vector3f(Mth.cos(a) * radius * (1.1f - .2f * t), .1f + t * 1.3f, Mth.sin(a) * radius * (1.1f - .2f * t));
            }
            AuroraMesh.ribbon(band, p, view.eye(), pts, .3f, .8f * k, age * 2 + s * 30, light);
        }
        VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
        AuroraMesh.ring(ring, p, .06f, radius * .7f, radius * 1.35f, AuroraMesh.GREEN, .6f * k, light);
        AuroraMesh.stars(poseStack, buffer, view, new Vector3f(), radius * 1.4f, 1.6f, age, 16, .8f * k, seed, light);
    }

    // ---------------------------------------------------------------- blessing

    /** Round the caster (the pose's origin), {@code radius} round; the curtains reach the ground at {@code land}. */
    public static void blessing(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int life, float radius, int land, int seed, int light) {
        float down = BossMesh.ramp(age, 0, land);
        float after = Math.max(0, age - land);
        float rise = BossMesh.ramp(after, 4, life - land);
        float fade = 1 - rise;
        if (fade <= .01f) {
            return;
        }
        PoseStack.Pose p = poseStack.last();
        float r = radius * (1 - .35f * down);
        float hem = Mth.lerp(down, 14, 0) + rise * 6;
        Vector3f[] hems = AuroraMesh.arc(r, hem, 44, age * .02f, Mth.TWO_PI);
        AuroraMesh.curtain(buffer, p, hems, 6, age, fade, age * .01f, light);
        Vector3f[] inner = AuroraMesh.arc(r * .6f, hem + 2, 32, -age * .03f, Mth.TWO_PI);
        AuroraMesh.curtain(buffer, p, inner, 4, age + 20, .5f * fade, age * .01f, light);
        // As they land: a flash, a ring running out, a column of light
        if (after > 0) {
            float flash = (float) Math.exp(-after / 5);
            VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
            float rr = radius * (.3f + 1.1f * BossMesh.ramp(after, 0, 14));
            AuroraMesh.ring(ring, p, .1f, rr * .8f, rr, AuroraMesh.TEAL, .9f * flash, light);
            VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
            GemMesh.ray(beam, p, view.eye(), new Vector3f(0, 0, 0), new Vector3f(0, 12, 0), AuroraMesh.GREEN, 1.4f, flash, age);
            AuroraMesh.glow(poseStack, buffer, view, new Vector3f(0, 1, 0), 6, AuroraMesh.TEAL, .6f * flash, light);
        }
        AuroraMesh.stars(poseStack, buffer, view, new Vector3f(0, rise * 4, 0), radius, 6, age, 40, fade, seed, light);
    }
}
