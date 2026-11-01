package com.waylays.arcanes.client.render.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.fire.FireMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Eclipse: a sun kindles high over the arena and a black disc slides across it; at totality its corona blazes out
 * round the dark, streamers of gold and violet, rays falling to the ground, and a shadow spreads over the arena. Then
 * the shadow strikes in three bands from the middle outward, each a ring of black-violet fire, and the eclipse passes:
 * the dark disc shrinks to a point and the light comes back in a flash.
 */
public final class EclipseModel {
    public static final int BANDS = 3;
    /** How long it takes to cover the sun (ticks). */
    public static final int TOTALITY = 32;
    /** How long a band's fire burns (ticks). */
    public static final int BURN = 16;
    /** How long it takes to pass, after the last band (ticks). */
    public static final int PASS = 20;
    private static final int GOLD = 0xFFD47A;
    private static final int PALE = 0xFFF6DE;
    private static final int VIOLET = 0x9A3CFF;

    private EclipseModel() {}

    /**
     * Drawn from the middle of the arena (the pose's origin, on the ground); {@code height} where the sun hangs over it,
     * {@code zone} the arena's radius, {@code radius} the sun's; {@code age} ticks since it was cast; {@code first}
     * ticks since the first band struck (negative before), the next ones {@code step} apart.
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float height, float zone, float radius, float age, float first,
                            float step, int seed, int light) {
        float kindle = BossMesh.ramp(age, 0, 10);
        float cover = BossMesh.ramp(age, 4, TOTALITY);
        float sinceLast = first - (BANDS - 1) * step;
        float pass = BossMesh.ramp(sinceLast, BURN, BURN + PASS);
        float total = cover * (1 - pass);
        Vector3f sun = new Vector3f(0, height, 0);

        // The shadow over the arena
        VertexConsumer shade = BossMesh.see(buffer, BossMesh.DISC);
        BossMesh.pool(shade, poseStack.last(), 0, .04f, 0, (zone + 3) * 2, 0, BossMesh.argb(0x06000C, .42f * total), light);

        // In the sky, seen face on
        poseStack.pushPose();
        poseStack.translate(sun.x, sun.y, sun.z);
        poseStack.mulPose(view.camera());
        PoseStack.Pose p = poseStack.last();
        Vector3f ux = new Vector3f(1, 0, 0), uy = new Vector3f(0, 1, 0);
        float rs = radius * (.3f + .7f * kindle) * (1 - .85f * pass);
        // The sun itself, bright until the dark covers it
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        float bare = kindle * (1 - cover * .85f) * (1 - pass) + pass * (1 - BossMesh.ramp(sinceLast, BURN + PASS, BURN + PASS + 8));
        flatGlow(glow, p, 0, 0, -.2f, rs * 4.2f, BossMesh.glow(PALE, bare));
        flatGlow(glow, p, 0, 0, -.2f, rs * 9, BossMesh.glow(GOLD, .45f * bare));
        // The corona: flowing out from behind the dark, brighter at totality, flaring as each band strikes
        float flare = 0;
        for (int k = 0; k < BANDS; k++) {
            float s = first - k * step;
            if (s >= 0 && s < BURN) {
                flare = Math.max(flare, 1 - s / BURN);
            }
        }
        float corona = total * (.75f + .25f * Mth.sin(age * .11f)) + flare * .5f * total;
        VertexConsumer noise = BossMesh.light(buffer, BossMesh.NOISE);
        BossMesh.annulus(noise, p, new Vector3f(0, 0, -.1f), ux, uy, rs * .95f, rs * (1.45f + .3f * flare), 48, 3, age * .004f, age * .02f,
                BossMesh.glow(PALE, corona), BossMesh.glow(GOLD, 0), light, false);
        BossMesh.annulus(noise, p, new Vector3f(0, 0, -.12f), ux, uy, rs * .98f, rs * (2.6f + .8f * flare), 48, 5, -age * .003f, age * .012f,
                BossMesh.glow(GOLD, .75f * corona), BossMesh.glow(VIOLET, 0), light, false);
        BossMesh.annulus(noise, p, new Vector3f(0, 0, -.14f), ux, uy, rs * 1.2f, rs * 4.2f, 48, 7, age * .002f, age * .008f, BossMesh.glow(VIOLET, .5f * corona),
                BossMesh.glow(VIOLET, 0), light, false);
        // Rays
        VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
        for (int k = 0; k < 18; k++) {
            float a = k * Mth.TWO_PI / 18 + age * .004f + BossMesh.hash(seed + k) * .2f;
            float len = rs * (2.4f + 2.2f * BossMesh.hash(seed + k * 3)) * (1 + .3f * flare);
            float w = rs * (.08f + .1f * BossMesh.hash(seed + k * 5));
            float k2 = corona * (.35f + .3f * Mth.sin(age * .2f + k * 1.7f));
            ray(beam, p, a, rs * .9f, len, w, BossMesh.glow(k % 3 == 0 ? VIOLET : GOLD, k2), light);
        }
        // The diamond ring: the last of the sun shining past the dark's edge as it covers it
        float ring = Math.max(0, 1 - Math.abs(cover - .93f) * 9) + Math.max(0, 1 - Math.abs(pass - .1f) * 9);
        if (ring > 0) {
            float a = .7f;
            float bx = Mth.cos(a) * rs, by = Mth.sin(a) * rs;
            flatGlow(glow(buffer), p, bx, by, .1f, rs * 1.6f, BossMesh.glow(0xFFFFFF, ring));
            VertexConsumer streak = BossMesh.light(buffer, BossMesh.BEAM);
            ray(streak, p, 0, -rs * 6, rs * 12, rs * .12f, BossMesh.glow(0xFFFFFF, ring * .8f), light, bx, by);
        }
        // The dark disc, sliding across, then shrinking away
        float slide = (1 - cover) * rs * 2.3f;
        VertexConsumer dark = BossMesh.see(buffer, BossMesh.DISC);
        float dr = rs * 1.02f;
        BossMesh.quad(dark, poseStack.last(), new Vector3f(slide - dr, -dr * .6f * (1 - cover) - dr, 0), new Vector3f(slide + dr, -dr * .6f * (1 - cover) - dr, 0),
                new Vector3f(slide + dr, -dr * .6f * (1 - cover) + dr, 0), new Vector3f(slide - dr, -dr * .6f * (1 - cover) + dr, 0), 0, 1, 1, 1, 1, 0, 0, 0,
                BossMesh.argb(0x030006, .985f * kindle), BossMesh.argb(0x030006, .985f * kindle), light, new Vector3f(0, 0, 1));
        // The flash as it passes
        if (pass > 0) {
            float f = Math.max(0, 1 - Math.abs(pass - .85f) * 5);
            flatGlow(glow(buffer), poseStack.last(), 0, 0, .2f, radius * (3 + 8 * f), BossMesh.glow(0xFFFFFF, f));
        }
        poseStack.popPose();

        // Rays of shadow-light falling on the arena
        VertexConsumer shafts = BossMesh.light(buffer, BossMesh.BEAM);
        for (int k = 0; k < 10; k++) {
            float a = k * Mth.TWO_PI / 10 + age * .006f + BossMesh.hash(seed + k * 7);
            float r = zone * (.3f + .6f * BossMesh.hash(seed + k * 9));
            Vector3f top = new Vector3f(Mth.cos(a) * rs * .8f, height, Mth.sin(a) * rs * .8f);
            Vector3f foot = new Vector3f(Mth.cos(a) * r, 0, Mth.sin(a) * r);
            RitualMesh.shaft(shafts, poseStack.last(), view.eye(), top, foot, 1.4f + rs * .3f, BossMesh.glow(VIOLET, .16f * total * (.6f + .4f * Mth.sin(age * .07f + k))),
                    light);
        }

        // The bands striking: rings of black-violet fire, a ring of light racing across each
        FireMesh.Batch fire = new FireMesh.Batch(view, 5, age, light);
        for (int k = 0; k < BANDS; k++) {
            float s = first - k * step;
            if (s < 0 || s > BURN + 4) {
                continue;
            }
            float r0 = zone * k / BANDS, r1 = zone * (k + 1) / BANDS;
            float rise = Mth.sin(Mth.PI * Mth.clamp(s / BURN, 0, 1));
            for (int j = 0; j < 2; j++) {
                float r = Mth.lerp((j + 1) / 3f, r0, r1);
                int n = Math.max(6, (int) (Mth.TWO_PI * r / 1.7f));
                Vector3f[] base = new Vector3f[n + 1];
                float[] h = new float[n + 1], al = new float[n + 1];
                for (int i = 0; i <= n; i++) {
                    float a = i * Mth.TWO_PI / n;
                    base[i] = new Vector3f(Mth.cos(a) * r, .05f, Mth.sin(a) * r);
                    h[i] = (2.2f + 1.6f * j) * rise;
                    al[i] = .9f * rise;
                }
                fire.curtain(base, h, al, .85f, seed + k * 31 + j * 7);
            }
            VertexConsumer wave = BossMesh.light(buffer, BossMesh.RING);
            float t = Mth.clamp(s / 8, 0, 1);
            float rr = Mth.lerp(t, Math.max(.5f, r0), r1);
            BossMesh.pool(wave, poseStack.last(), 0, .08f, 0, rr * 2.15f, 0, BossMesh.glow(0xD9A0FF, 1 - t), light);
        }
        fire.flush(buffer, poseStack);
    }

    private static VertexConsumer glow(MultiBufferSource buffer) {
        return BossMesh.light(buffer, BossMesh.GLOW);
    }

    /** A square of glow facing the eye (in the sky's face-on frame) round {@code (x, y)}, {@code z} toward the eye. */
    private static void flatGlow(VertexConsumer c, PoseStack.Pose p, float x, float y, float z, float size, int argb) {
        float h = size * .5f;
        BossMesh.quad(c, p, new Vector3f(x - h, y - h, z), new Vector3f(x + h, y - h, z), new Vector3f(x + h, y + h, z), new Vector3f(x - h, y + h, z), 0, 1, 1, 1,
                1, 0, 0, 0, argb, argb, LIGHT, new Vector3f(0, 0, 1));
    }

    private static final int LIGHT = 0xF000F0;

    /** A ray in the sky's face-on frame, out along angle {@code a} from {@code r0} to {@code r0 + len}, {@code w} wide (facing the eye). */
    private static void ray(VertexConsumer c, PoseStack.Pose p, float a, float r0, float len, float w, int argb, int light) {
        ray(c, p, a, r0, len, w, argb, light, 0, 0);
    }

    private static void ray(VertexConsumer c, PoseStack.Pose p, float a, float r0, float len, float w, int argb, int light, float ox, float oy) {
        float dx = Mth.cos(a), dy = Mth.sin(a);
        float sx = -dy * w * .5f, sy = dx * w * .5f;
        Vector3f s0 = new Vector3f(ox + dx * r0, oy + dy * r0, -.05f), s1 = new Vector3f(ox + dx * (r0 + len), oy + dy * (r0 + len), -.05f);
        BossMesh.quad(c, p, new Vector3f(s0).add(sx, sy, 0), new Vector3f(s0).sub(sx, sy, 0), new Vector3f(s1).sub(sx, sy, 0), new Vector3f(s1).add(sx, sy, 0), 1,
                0, 0, 0, 0, 1, 1, 1, argb, BossMesh.glow(0, 0), light, new Vector3f(0, 0, 1));
    }
}
