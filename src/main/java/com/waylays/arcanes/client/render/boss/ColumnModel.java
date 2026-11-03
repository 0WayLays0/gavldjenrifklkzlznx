package com.waylays.arcanes.client.render.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Toppling Columns: a column of an old temple rises out of the ground at the head of its strip, as tall as the strip is
 * long: a square plinth, mouldings, a fluted shaft of veined marble built of drums, a capital with its scrolls, a slab on
 * top. Cracks of light open in it; it leans, and topples down the strip, crashing along it; its drums break apart and
 * roll, dust billows, and the ruins sink back into the ground.
 */
public final class ColumnModel {
    private static final ResourceLocation MARBLE = ArcanesOublies.id("textures/entity/boss/m_marble.png");
    private static final ResourceLocation CRACKS = ArcanesOublies.id("textures/entity/boss/m_cracks.png");
    /** Ticks it takes to fall, ending as its strip's warning runs out. */
    private static final float FALL = 14;
    private static final int DRUMS = 7, FLUTES = 20;
    private static final float[] FLUTED = fluted();

    private ColumnModel() {}

    /**
     * Standing at the head of its strip (the pose's origin), falling toward {@code to}; it lands {@code hit} ticks ago
     * (negative before); {@code width} its strip's width.
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f to, float age, float hit, float width, int zone, int seed,
                            int light) {
        float rise = BossMesh.ramp(age, 0, 16);
        float sink = BossMesh.ramp(hit, 14, 30);
        if (sink >= 1) {
            return;
        }
        Vector3f along = new Vector3f(to.x, 0, to.z);
        float length = along.length();
        along.normalize();
        float height = length * .97f;
        float r = width * .45f;
        // Leaning, then falling ever faster; a bounce as it lands
        float t = Mth.clamp((hit + FALL) / FALL, 0, 1);
        float lean = hit < -FALL ? BossMesh.ramp(hit, -FALL - 12, -FALL) * .05f : 0;
        float tilt = Math.max(lean, Mth.HALF_PI * t * t);
        if (hit > 0 && hit < 6) {
            tilt -= Mth.sin(hit / 6 * Mth.PI) * .04f;
        }
        float yawRad = (float) Math.atan2(along.x, along.z);
        float crack = BossMesh.ramp(hit, -FALL - 20, -FALL) * (1 - sink);
        float broken = BossMesh.ramp(hit, 0, 10);
        float dropY = (rise - 1) * height - sink * r * 2.5f;

        VertexConsumer marble = BossMesh.solid(buffer, MARBLE);
        poseStack.pushPose();
        poseStack.translate(0, dropY, 0);
        poseStack.mulPose(new Quaternionf().rotationY(yawRad));
        // The plinth stays; the rest falls about its front edge
        Sculpt.block(marble, poseStack.last(), r * 2.9f, .5f, r * 2.9f, .06f, .25f, 0xFFD8D2CA, light);
        poseStack.translate(0, .25f, r * 1.2f);
        poseStack.mulPose(new Quaternionf().rotationX(tilt));
        poseStack.translate(0, 0, -r * 1.2f);
        column(marble, poseStack, height, r, broken, seed, light);
        poseStack.popPose();

        // ---- light: the cracks opening in it, the dust of its fall, the flash where it lands
        if (crack > 0) {
            VertexConsumer glow = BossMesh.light(buffer, CRACKS);
            poseStack.pushPose();
            poseStack.translate(0, dropY, 0);
            poseStack.mulPose(new Quaternionf().rotationY(yawRad));
            poseStack.translate(0, .25f, r * 1.2f);
            poseStack.mulPose(new Quaternionf().rotationX(tilt));
            poseStack.translate(0, 0, -r * 1.2f);
            float shaft0 = .9f, shaft1 = height - 1.9f;
            int g = BossMesh.glow(zone, crack * (.6f + .4f * Mth.sin(age * .6f)));
            for (int d = 0; d < DRUMS; d++) {
                float y0 = Mth.lerp((float) d / DRUMS, shaft0, shaft1), y1 = Mth.lerp((float) (d + 1) / DRUMS, shaft0, shaft1);
                poseStack.pushPose();
                drum(poseStack, d, broken, r, y0, y1, seed);
                Sculpt.lathe(glow, poseStack.last(), new float[] {r * 1.015f, 0, r * .97f * 1.015f, y1 - y0}, 24, 2, .35f, g, g, light);
                poseStack.popPose();
            }
            poseStack.popPose();
        }
        if (hit >= 0) {
            float f = 1 - BossMesh.ramp(hit, 0, 26);
            VertexConsumer dust = BossMesh.see(buffer, BossMesh.CLOUD);
            for (int k = 0; k < 14; k++) {
                float s = (k + .5f) / 14f;
                Vector3f p = new Vector3f(along).mul(s * length).add(0, .6f + BossMesh.hash(seed + k) * 1.2f + hit * .03f, 0);
                p.add(new Vector3f(along.z, 0, -along.x).mul((BossMesh.hash(seed + k * 3) - .5f) * width * 1.6f));
                RitualMesh.billboard(dust, poseStack, view, p, 3 + hit * .12f, k, -1, BossMesh.argb(0xD8D0C4, .55f * f), light);
            }
            if (hit < 8) {
                VertexConsumer ring = BossMesh.light(buffer, BossMesh.GLOW);
                for (int k = 0; k < 6; k++) {
                    Vector3f p = new Vector3f(along).mul((k + .5f) / 6f * length).add(0, .4f, 0);
                    RitualMesh.billboard(ring, poseStack, view, p, 4 + hit, 0, -1, BossMesh.glow(zone, (1 - hit / 8) * .7f), light);
                }
            }
        }
    }

    /** Where drum {@code d} goes once broken apart: rolled aside, tumbled, a little apart from the next. */
    private static void drum(PoseStack poseStack, int d, float broken, float r, float y0, float y1, int seed) {
        if (broken <= 0) {
            return;
        }
        float side = (BossMesh.hash(seed + d * 5) - .5f) * 2.4f * r;
        float spin = (BossMesh.hash(seed + d * 9) - .5f) * .7f;
        poseStack.translate(side * broken, d * .18f * broken, -r * .3f * broken);
        float mid = (y0 + y1) * .5f;
        poseStack.translate(0, mid, 0);
        poseStack.mulPose(new Quaternionf().rotationZ(spin * broken).rotateX(spin * .5f * broken));
        poseStack.translate(0, -mid, 0);
    }

    /** The column standing on the origin: mouldings, drums of fluted shaft, capital and its scrolls, abacus. */
    private static void column(VertexConsumer c, PoseStack poseStack, float height, float r, float broken, int seed, int light) {
        int white = 0xFFFFFFFF, shade = 0xFFC8C0B8;
        PoseStack.Pose p = poseStack.last();
        // The base's mouldings
        Sculpt.lathe(c, p, new float[] {r * 1.3f, 0, r * 1.34f, .12f, r * 1.25f, .24f, r * 1.12f, .3f, r * 1.18f, .42f, r * 1.12f, .52f, r * 1.02f, .6f, r, .9f}, 28, 3,
                .4f, shade, white, light);
        // The shaft: drums of fluted marble, narrowing a little toward the top
        float shaft0 = .9f, shaft1 = height - 1.9f;
        for (int d = 0; d < DRUMS; d++) {
            float y0 = Mth.lerp((float) d / DRUMS, shaft0, shaft1), y1 = Mth.lerp((float) (d + 1) / DRUMS, shaft0, shaft1);
            float r0 = r * (1 - .1f * d / DRUMS), r1 = r * (1 - .1f * (d + 1) / DRUMS);
            poseStack.pushPose();
            drum(poseStack, d, broken, r, y0, y1, seed);
            Vector3f[] path = {new Vector3f(0, y0 + .02f, 0), new Vector3f(0, (y0 + y1) * .5f, 0), new Vector3f(0, y1 - .02f, 0)};
            Sculpt.sweep(c, poseStack.last(), path, new float[] {r0, (r0 + r1) * .5f, r1}, new float[] {r0, (r0 + r1) * .5f, r1}, FLUTED, false, new Vector3f(1, 0, 0), .35f,
                    Sculpt.fill(white, 3), true, light);
            poseStack.popPose();
        }
        // The capital: a necking ring, the echinus swelling out, the scrolls, the abacus slab
        poseStack.pushPose();
        drum(poseStack, DRUMS, broken, r, shaft1, height, seed);
        p = poseStack.last();
        poseStack.pushPose();
        poseStack.translate(0, shaft1 + .1f, 0);
        Sculpt.torus(c, poseStack.last(), r * .92f, .09f, 28, 6, 0, Mth.TWO_PI, 4, shade, light);
        poseStack.popPose();
        Sculpt.lathe(c, p, new float[] {r * .9f, shaft1, r * .9f, shaft1 + .45f, r * 1.05f, shaft1 + .8f, r * 1.3f, shaft1 + 1.1f, r * 1.32f, shaft1 + 1.25f, 0,
                shaft1 + 1.27f}, 28, 3, .4f, shade, white, light);
        for (int side = -1; side <= 1; side += 2) {
            for (int front = -1; front <= 1; front += 2) {
                Vector3f[] spiral = new Vector3f[18];
                for (int i = 0; i < 18; i++) {
                    float a = i / 17f * Mth.TWO_PI * 1.4f;
                    float rr = r * .42f * (1 - i / 17f * .8f);
                    // The scroll curls in on itself at each corner of the capital, front and back
                    spiral[i] = new Vector3f(side * (r * 1.12f + rr * Mth.cos(a)), shaft1 + .95f + rr * Mth.sin(a), front * r * .95f);
                }
                Sculpt.sweep(c, p, spiral, Sculpt.taper(.12f, .06f, 18, 1), Sculpt.taper(.18f, .08f, 18, 1), Sculpt.circle(6), false, new Vector3f(0, 0, 1), .5f,
                        Sculpt.fill(shade, 18), true, light);
            }
        }
        poseStack.pushPose();
        poseStack.translate(0, shaft1 + 1.27f + .22f, 0);
        Sculpt.block(c, poseStack.last(), r * 2.9f, .44f, r * 2.9f, .07f, .25f, white, light);
        poseStack.popPose();
        poseStack.popPose();
    }

    /** A fluted shaft's section: a circle with {@code FLUTES} hollows run down it. */
    private static float[] fluted() {
        int per = 4;
        int n = FLUTES * per;
        float[] s = new float[2 * n];
        for (int k = 0; k < n; k++) {
            float a = k * Mth.TWO_PI / n;
            float f = (k % per) / (float) per;
            // The hollow of each flute, a sharp arris between
            float depth = .07f * Mth.sin(f * Mth.PI);
            s[2 * k] = Mth.cos(a) * (1 - depth);
            s[2 * k + 1] = Mth.sin(a) * (1 - depth);
        }
        return s;
    }
}
