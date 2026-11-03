package com.waylays.arcanes.client.render.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The parts the Boss figures share (the Reaper, the spectral kings, the maidens' faces): a skull, a skeletal hand
 * closed round a haft, a robe falling in folds to a tattered hem, a hood. Each draws in the pose's own space.
 */
public final class Figures {
    private Figures() {}

    /**
     * A skull about one unit high round the origin, its face toward +Z, its jaw hanging open {@code jaw} (radians).
     * {@code bone} is drawn with a bone texture; the dark of its sockets and nose with the same buffer, darkened.
     */
    public static void skull(VertexConsumer bone, PoseStack poseStack, float jaw, int light) {
        int white = 0xFFFFFFFF, dark = 0xFF2A2018, shade = 0xFFB8AA96;
        // The cranium, a little long front to back
        poseStack.pushPose();
        poseStack.translate(0, .1f, -.06f);
        poseStack.scale(.9f, 1, 1.12f);
        Sculpt.lathe(bone, poseStack.last(), BellModel.sphere(.5f, 12), 18, 1, 1, white, white, light);
        poseStack.popPose();
        // The brow, the cheekbones, the upper jaw
        box(bone, poseStack, 0, .12f, .4f, .78f, .13f, .2f, .04f, white, light);
        for (int side = -1; side <= 1; side += 2) {
            box(bone, poseStack, side * .27f, -.16f, .32f, .2f, .14f, .24f, .03f, shade, light);
            // The sockets: dark hollows under the brow
            poseStack.pushPose();
            poseStack.translate(side * .19f, -.02f, .41f);
            poseStack.scale(1, .82f, .5f);
            Sculpt.lathe(bone, poseStack.last(), BellModel.sphere(.14f, 6), 10, 1, 1, dark, dark, light);
            poseStack.popPose();
        }
        poseStack.pushPose();
        poseStack.translate(0, -.17f, .45f);
        poseStack.scale(.6f, 1, .4f);
        Sculpt.lathe(bone, poseStack.last(), BellModel.sphere(.09f, 5), 6, 1, 1, dark, dark, light);
        poseStack.popPose();
        box(bone, poseStack, 0, -.31f, .3f, .5f, .14f, .3f, .03f, white, light);
        teeth(bone, poseStack, -.4f, .42f, light);
        // The lower jaw, hinged under the ears
        poseStack.pushPose();
        poseStack.translate(0, -.3f, .02f);
        poseStack.mulPose(new Quaternionf().rotationX(jaw));
        box(bone, poseStack, 0, -.22f, .27f, .5f, .12f, .3f, .03f, white, light);
        for (int side = -1; side <= 1; side += 2) {
            box(bone, poseStack, side * .25f, -.1f, .1f, .1f, .26f, .34f, .02f, shade, light);
        }
        teeth(bone, poseStack, -.12f, .42f, light);
        poseStack.popPose();
    }

    private static void teeth(VertexConsumer bone, PoseStack poseStack, float y, float z, int light) {
        for (int k = 0; k < 8; k++) {
            float a = (k - 3.5f) * .2f;
            box(bone, poseStack, Mth.sin(a) * .26f, y, z - (1 - Mth.cos(a)) * .3f, .055f, .1f, .06f, .01f, 0xFFF4EEE0, light);
        }
    }

    /** A bevelled box {@code w x h x d} centred on {@code (x, y, z)}. */
    public static void box(VertexConsumer c, PoseStack poseStack, float x, float y, float z, float w, float h, float d, float bevel, int argb, int light) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        Sculpt.block(c, poseStack.last(), w, h, d, bevel, 1, argb, light);
        poseStack.popPose();
    }

    /**
     * A skeletal hand closed round a haft running along X through the origin, {@code r} across: the bones of the palm
     * behind it (toward -Z) and four fingers curled over and round it, the thumb across. About half a unit wide.
     */
    public static void claw(VertexConsumer bone, PoseStack poseStack, float r, int light) {
        int white = 0xFFFFFFFF;
        // The palm's bones, behind the haft
        for (int k = 0; k < 4; k++) {
            float x = (k - 1.5f) * .11f;
            Vector3f[] meta = {new Vector3f(x * .6f, -.32f, -r - .12f), new Vector3f(x, -.05f, -r - .06f)};
            Sculpt.sweep(bone, poseStack.last(), meta, Sculpt.fill(.035f, 2), Sculpt.fill(.035f, 2), Sculpt.circle(6), false, null, 1, Sculpt.fill(white, 2), true, light);
            // Each finger over the top of the haft and round to the front, its tip tucked under
            int n = 7;
            Vector3f[] pts = new Vector3f[n];
            for (int i = 0; i < n; i++) {
                // From behind the haft, over its top, round to the front and under
                float a = Mth.lerp((float) i / (n - 1), -.3f, Mth.PI + .5f);
                float rr = r + .045f;
                pts[i] = new Vector3f(x, Mth.sin(a) * rr, -Mth.cos(a) * rr);
            }
            Sculpt.sweep(bone, poseStack.last(), pts, Sculpt.taper(.034f, .022f, n, 1), Sculpt.taper(.034f, .022f, n, 1), Sculpt.circle(6), false, null, 1,
                    Sculpt.fill(white, n), true, light);
            // Its knuckles
            poseStack.pushPose();
            poseStack.translate(pts[2].x, pts[2].y, pts[2].z);
            Sculpt.lathe(bone, poseStack.last(), BellModel.sphere(.045f, 5), 6, 1, 1, white, white, light);
            poseStack.popPose();
        }
        // The thumb, along the haft in front
        Vector3f[] thumb = {new Vector3f(-.2f, -.3f, -r - .08f), new Vector3f(-.26f, -.1f, r * .2f), new Vector3f(-.2f, -.02f, r + .05f), new Vector3f(-.08f, .02f, r + .06f)};
        Sculpt.sweep(bone, poseStack.last(), thumb, Sculpt.taper(.04f, .025f, 4, 1), Sculpt.taper(.04f, .025f, 4, 1), Sculpt.circle(6), false, null, 1,
                Sculpt.fill(white, 4), true, light);
    }

    /**
     * A robe hanging round the Y axis from {@code top} (radius {@code r0}) down toward {@code bottom} (radius {@code r1}),
     * in {@code folds} folds, its hem torn: each strip of it ends at its own height, up to {@code tear} above the bottom,
     * and sways with {@code wind}. Drawn both sides (its inside darker). U round it, V down it by length.
     */
    public static void robe(VertexConsumer c, PoseStack.Pose p, float top, float bottom, float r0, float r1, int folds, float tear, float wind, int seed, int argb,
                            int inside, int light) {
        int segs = folds * 4;
        int rows = 10;
        for (int s = 0; s < segs; s++) {
            float a0 = s * Mth.TWO_PI / segs, a1 = (s + 1) * Mth.TWO_PI / segs;
            // Each strip's own length, torn
            float h = BossMesh.hash(seed + s * 3);
            float end = bottom + tear * (h * h);
            for (int i = 0; i < rows; i++) {
                float t0 = (float) i / rows, t1 = (float) (i + 1) / rows;
                float y0 = Mth.lerp(t0, top, end), y1 = Mth.lerp(t1, top, end);
                Vector3f a = fold(a0, t0, y0, r0, r1, folds, wind, seed), b = fold(a1, t0, y0, r0, r1, folds, wind, seed);
                Vector3f d = fold(a1, t1, y1, r0, r1, folds, wind, seed), e = fold(a0, t1, y1, r0, r1, folds, wind, seed);
                float u0 = (float) s / segs * folds, u1 = (float) (s + 1) / segs * folds;
                // Wound to face out (round from +X toward +Z, then down), then the inside the other way; darker toward the hem
                Vector3f n0 = new Vector3f(a.x, 0, a.z).normalize(), n1 = new Vector3f(b.x, 0, b.z).normalize();
                int hi = BossMesh.blend(argb, 0xFF0A0610, t0 * .5f), lo = BossMesh.blend(argb, 0xFF0A0610, t1 * .5f);
                Sculpt.put(c, p, a, u0, y0 * -.5f, hi, light, n0);
                Sculpt.put(c, p, b, u1, y0 * -.5f, hi, light, n1);
                Sculpt.put(c, p, d, u1, y1 * -.5f, lo, light, n1);
                Sculpt.put(c, p, e, u0, y1 * -.5f, lo, light, n0);
                Vector3f m0 = new Vector3f(n0).negate(), m1 = new Vector3f(n1).negate();
                Sculpt.put(c, p, a, u0, y0 * -.5f, inside, light, m0);
                Sculpt.put(c, p, e, u0, y1 * -.5f, inside, light, m0);
                Sculpt.put(c, p, d, u1, y1 * -.5f, inside, light, m1);
                Sculpt.put(c, p, b, u1, y0 * -.5f, inside, light, m1);
            }
        }
    }

    private static Vector3f fold(float a, float t, float y, float r0, float r1, int folds, float wind, int seed) {
        float r = Mth.lerp((float) Math.pow(t, .8f), r0, r1) * (1 + .09f * Mth.sin(a * folds) * t + .03f * Mth.sin(a * folds * 2.3f + seed));
        // The lower it hangs the more it sways
        float sway = wind * t * t;
        return new Vector3f(Mth.cos(a) * r + sway * Mth.sin(y * .7f + seed) * .3f, y, Mth.sin(a) * r + sway);
    }

    /**
     * A hood round a head at the origin, {@code r} across, open toward +Z over {@code open} radians, its peak drawn up a
     * little and back; both sides drawn, its inside deep shadow.
     */
    public static void hood(VertexConsumer c, PoseStack poseStack, float r, float open, int argb, int light) {
        float[] prof = {r * 1.25f, -r * 1.25f, r * 1.22f, -r * .6f, r * 1.12f, 0, r * .95f, r * .6f, r * .6f, r * 1.0f, r * .2f, r * 1.22f, 0, r * 1.3f};
        // Turned so that the gap faces +Z (the lathe's angles run from +X toward +Z)
        float a0 = Mth.HALF_PI + open * .5f, a1 = Mth.HALF_PI + Mth.TWO_PI - open * .5f;
        poseStack.pushPose();
        poseStack.translate(0, 0, -r * .15f);
        Sculpt.lathe(c, poseStack.last(), prof, 20, a0, a1, 2, .5f, argb, argb, light);
        // Its inside, darker (the same shape, a hair smaller, wound the other way)
        float[] in = new float[prof.length];
        for (int i = 0; i < prof.length; i += 2) {
            in[prof.length - 2 - i] = prof[i] * .97f;
            in[prof.length - 1 - i] = prof[i + 1] * .97f;
        }
        Sculpt.lathe(c, poseStack.last(), in, 20, a0, a1, 2, .5f, 0xFF0C0810, 0xFF0C0810, light);
        poseStack.popPose();
    }
}
