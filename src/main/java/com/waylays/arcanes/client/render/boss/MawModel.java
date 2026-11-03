package com.waylays.arcanes.client.render.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.resources.ResourceLocation;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * The Abyssal Maw: the ground opens into a gullet of violet night, swirling, and two rows of great fangs, bone at
 * their points and dark at their roots, rise out of the earth round it; motes of light are drawn down into it. Then it
 * bites: the fangs snap in over the middle, a flash of violet, and they sink back as the gullet closes.
 */
public final class MawModel {
    /** How long its fangs take to snap shut (ticks). */
    public static final int SNAP = 4;
    /** How long after the bite it is gone (ticks). */
    public static final int CLOSE = 26;
    private static final int OUTER = 16, FEELERS = 7;
    /** Its rows of fangs: how many, how far out, how long (of the radius). */
    private static final int[] ROWS = {16, 12, 9};
    private static final float[] RING = {.95f, .74f, .52f}, LENGTH = {.6f, .45f, .32f};
    private static final ResourceLocation FLESH = ArcanesOublies.id("textures/entity/boss/m_flesh.png");
    private static final ResourceLocation BONE = ArcanesOublies.id("textures/entity/boss/m_bone.png");
    private static final int VIOLET = 0x9A3CFF;

    private MawModel() {}

    /**
     * Drawn from the middle of the maw (the pose's origin, on the ground), {@code radius} across; {@code age} ticks since
     * it opened, {@code until} ticks until it bites (negative after).
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float radius, float age, float until, int seed, int light) {
        float open = BossMesh.ramp(age, 0, 30);
        float rise = BossMesh.ramp(age, 18, 52);
        float bite = until < 0 ? BossMesh.ramp(-until, 0, SNAP) : 0;
        float sink = until < 0 ? BossMesh.ramp(-until, 12, CLOSE) : 0;
        float gape = open * (1 - sink);
        float urge = until > 0 ? 1 - Mth.clamp(until / 30, 0, 1) : 1;

        // The gullet: violet night, its swirl drawing everything down
        VertexConsumer night = BossMesh.see(buffer, BossMesh.VOID);
        BossMesh.pool(night, poseStack.last(), 0, .03f, 0, radius * 1.9f * gape, age * .01f, BossMesh.argb(0xFFFFFF, .96f * gape), light);
        VertexConsumer swirl = BossMesh.light(buffer, BossMesh.SWIRL);
        BossMesh.pool(swirl, poseStack.last(), 0, .05f, 0, radius * 1.85f * gape, age * (.04f + .12f * urge), BossMesh.glow(VIOLET, .8f * gape), light);
        BossMesh.pool(swirl, poseStack.last(), 0, .06f, 0, radius * 1.1f * gape, -age * (.07f + .2f * urge), BossMesh.glow(0xE0A8FF, .55f * gape), light);
        VertexConsumer rim = BossMesh.light(buffer, BossMesh.RING);
        BossMesh.pool(rim, poseStack.last(), 0, .07f, 0, radius * 2.05f * gape, 0, BossMesh.glow(VIOLET, (.7f + .3f * Mth.sin(age * .25f)) * gape), light);

        // The gums: rings of raw flesh, one under each row of fangs, swelling out of the ground
        float swell = rise * (1 - sink);
        VertexConsumer flesh = BossMesh.solid(buffer, FLESH);
        for (int row = 0; row < 3; row++) {
            float rr = radius * RING[row];
            poseStack.pushPose();
            poseStack.translate(0, -.35f + .45f * swell, 0);
            Sculpt.torus(flesh, poseStack.last(), rr, radius * (.11f - row * .02f) * (.3f + .7f * swell), 48, 8, 0, Mth.TWO_PI, 6, 0xFFFFFFFF, light);
            poseStack.popPose();
        }
        // Feelers round the rim, groping, drawn in as it bites
        for (int f = 0; f < FEELERS; f++) {
            float a = (f + .5f) * Mth.TWO_PI / FEELERS + BossMesh.hash(seed + f * 13) * .3f;
            Vector3f out = new Vector3f(Mth.cos(a), 0, Mth.sin(a));
            int n = 9;
            Vector3f[] pts = new Vector3f[n];
            float len = radius * (.9f + .4f * BossMesh.hash(seed + f * 17)) * swell;
            for (int i = 0; i < n; i++) {
                float t = (float) i / (n - 1);
                float wave = Mth.sin(age * .2f + f * 1.7f + t * 4) * .5f * t;
                float reach = Mth.lerp(bite, 1, -.3f);
                Vector3f side = new Vector3f(-out.z, 0, out.x);
                pts[i] = new Vector3f(out).mul(radius * 1.05f + len * t * .55f * reach).add(0, len * Mth.sin(t * 1.4f) * .7f, 0).add(new Vector3f(side).mul(wave));
            }
            Sculpt.sweep(flesh, poseStack.last(), pts, Sculpt.taper(radius * .07f, .02f, n, 1), Sculpt.taper(radius * .07f, .02f, n, 1), Sculpt.circle(7), false, null,
                    1, Sculpt.fade(0xFFFFFFFF, 0xFFE0A0C0, n), true, light);
        }
        // The fangs: three rows, bone at their points and dark at their roots, curved in toward the gullet
        VertexConsumer bone = BossMesh.solid(buffer, BONE);
        PoseStack.Pose p = poseStack.last();
        Vector3f[] tips = new Vector3f[ROWS[0] + ROWS[1] + ROWS[2]];
        int next = 0;
        for (int row = 0; row < 3; row++) {
            for (int i = 0; i < ROWS[row]; i++) {
                int k = next++;
                float a = (i + row * .5f) * Mth.TWO_PI / ROWS[row] + BossMesh.hash(seed + k) * .1f;
                float rr = radius * RING[row];
                float len = radius * (LENGTH[row] + .22f * BossMesh.hash(seed + k * 3));
                float thick = len * (row == 0 ? .2f : .17f);
                float up = BossMesh.ramp(rise * 1.3f - BossMesh.hash(seed + k * 5) * .3f - row * .08f, 0, 1);
                float y = -len * (1 - up) - len * 1.15f * sink;
                Vector3f outward = new Vector3f(Mth.cos(a), 0, Mth.sin(a));
                Vector3f in = new Vector3f(outward).negate();
                float tilt = (.32f - row * .08f) + bite * (1.05f + row * .1f);
                Vector3f dir = new Vector3f(0, Mth.cos(tilt), 0).add(new Vector3f(in).mul(Mth.sin(tilt))).normalize();
                Vector3f base = new Vector3f(outward).mul(rr).add(0, y, 0);
                Vector3f bend = new Vector3f(in).mul(.3f);
                BossMesh.horn(bone, p, base, dir, bend, len, thick, 8, 7, 0xFF3A2030, 0xFFFFFFFF, light);
                tips[k] = new Vector3f(dir).mul(len).add(new Vector3f(bend).mul(len)).add(base);
            }
        }
        // Their roots glowing, and a shine on their points
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        for (int k = 0; k < OUTER; k++) {
            float a = k * Mth.TWO_PI / OUTER;
            RitualMesh.billboard(glow, poseStack, view, new Vector3f(Mth.cos(a) * radius * .93f, .3f, Mth.sin(a) * radius * .93f), 1.6f, 0, -1,
                    BossMesh.glow(VIOLET, .6f * rise * (1 - sink)), light);
        }
        for (Vector3f tip : tips) {
            if (tip.y > .2f) {
                RitualMesh.billboard(glow, poseStack, view, tip, .5f, 0, -1, BossMesh.glow(0xFFFFFF, .35f * (1 - sink)), light);
            }
        }
        // Motes drawn down into it
        VertexConsumer motes = BossMesh.light(buffer, BossMesh.MOTE);
        for (int k = 0; k < 30 && gape > 0; k++) {
            float t = (age * (.018f + .01f * BossMesh.hash(seed + k)) * (1 + urge) + BossMesh.hash(seed + k * 3)) % 1;
            float a = BossMesh.hash(seed + k * 7) * Mth.TWO_PI + t * 3;
            float r = radius * (1.6f - 1.6f * t);
            Vector3f at = new Vector3f(Mth.cos(a) * r, .2f + 2.5f * (1 - t) * BossMesh.hash(seed + k * 11), Mth.sin(a) * r);
            RitualMesh.billboard(motes, poseStack, view, at, .35f, 0, -1, BossMesh.glow(k % 4 == 0 ? 0xFFFFFF : 0xC890FF, Mth.sin(t * Mth.PI) * gape), light);
        }
        // The bite
        if (until < 0 && -until < 12) {
            float f = 1 - -until / 12;
            VertexConsumer flash = BossMesh.light(buffer, BossMesh.GLOW);
            RitualMesh.billboard(flash, poseStack, view, new Vector3f(0, radius * .5f, 0), radius * (2.5f + 2 * (1 - f)), 0, -1, BossMesh.glow(0xD9A0FF, f), light);
            VertexConsumer wave = BossMesh.light(buffer, BossMesh.RING);
            BossMesh.pool(wave, poseStack.last(), 0, .1f, 0, radius * 2 * (1 + 1.5f * (1 - f)), 0, BossMesh.glow(VIOLET, f), light);
        }
    }
}
