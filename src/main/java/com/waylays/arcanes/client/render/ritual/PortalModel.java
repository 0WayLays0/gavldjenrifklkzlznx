package com.waylays.arcanes.client.render.ritual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import static com.waylays.arcanes.client.render.water.WaterMesh.*;

/**
 * The Portal: an upright oval gate. Through it, the far side's violet night (a void of haze and stars); over it, arms
 * of light winding into its heart, turning; round it, a burning rim, and shards of amethyst wheeling round the rim;
 * motes drawn in from all round, spiralling into it; a pool of light at its foot. It opens from a thin seam of light
 * that splits and widens, and closes by folding back into a point with a flash.
 */
public final class PortalModel {
    public static final float WIDTH = 1.25f, HEIGHT = 1.85f;
    private static final int RINGS = 6, SECTORS = 28;

    private PortalModel() {}

    /**
     * In the pose's frame, standing on its origin facing {@code +z} (the renderer turns it to its yaw); {@code open}
     * (0 to 1) how far it has opened, {@code closing} (0 to 1) how far it has closed; {@code color} its light.
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, View view, float age, float open, float closing, int color, int seed, int light) {
        Vector3f eye = view.eye();
        // Opening: a seam of light first (thin, full height), then it widens; closing: it folds into a point
        float seam = Mth.clamp(open * 3, 0, 1);
        float widen = Mth.clamp((open - .25f) / .75f, 0, 1);
        widen = widen * widen * (3 - 2 * widen);
        float shut = closing * closing;
        float sx = WIDTH * Math.max(.04f, widen) * (1 - shut);
        float sy = HEIGHT * (.2f + .8f * seam) * (1 - shut * .9f);
        float cy = HEIGHT + .15f;
        int hot = mix(color, 0xFFFFFF, .5f);
        float alive = Math.min(seam, 1 - closing);
        float flash = Math.max(0, 1 - Math.abs(closing - .85f) * 6) + Math.max(0, 1 - Math.abs(open - .25f) * 8) * .7f;
        float spin = age * .045f;

        // The far side, seen through it
        if (widen > 0) {
            VertexConsumer far = buffer.getBuffer(RenderType.entityTranslucent(RitualMesh.VOID));
            disc(far, poseStack.last(), 0, cy, sx, sy, 0, spin * .2f, argb(0xFFFFFF, widen * (1 - closing)), light, 1);
        }
        // Arms of light winding into its heart, two layers turning against each other
        VertexConsumer swirl = RitualMesh.light(buffer, RitualMesh.SWIRL);
        PoseStack.Pose p = poseStack.last();
        for (int side = -1; side <= 1; side += 2) {
            float z = .02f * side;
            disc(swirl, p, z, cy, sx * .98f, sy * .98f, spin * 2.2f, 0, glow(color, .8f * alive), light, side);
            disc(swirl, p, z * 2, cy, sx * .8f, sy * .8f, -spin * 3.1f + 1, 0, glow(hot, .55f * alive), light, side);
        }
        // The burning rim
        VertexConsumer rim = RitualMesh.light(buffer, RitualMesh.RIM);
        p = poseStack.last();
        int n = 48;
        Vector3f[] pts = new Vector3f[n + 1];
        Vector3f[] out = new Vector3f[n + 1];
        float[] w = new float[n + 1];
        int[] col = new int[n + 1];
        for (int i = 0; i <= n; i++) {
            float a = i * Mth.TWO_PI / n;
            float wobble = 1 + .03f * Mth.sin(a * 5 + age * .3f) + .02f * Mth.sin(a * 11 - age * .5f);
            pts[i] = new Vector3f(Mth.cos(a) * sx * wobble, cy + Mth.sin(a) * sy * wobble, 0);
            out[i] = new Vector3f(Mth.cos(a) * sy, Mth.sin(a) * sx, 0).normalize();
            w[i] = (.28f + .1f * Mth.sin(a * 3 - age * .4f)) * (.6f + .4f * seam) + flash * .4f;
            col[i] = glow(i % 2 == 0 ? hot : color, Math.min(1, alive + flash));
        }
        twoSided(rim, p, pts, out, w, col, .6f, age * .05f, light);
        float[] w2 = new float[n + 1];
        for (int i = 0; i <= n; i++) {
            w2[i] = w[i] * 2.6f;
            col[i] = glow(color, .5f * Math.min(1, alive + flash));
        }
        twoSided(rim, p, pts, out, w2, col, .4f, -age * .03f, light);
        // Its light, and the pool of light at its foot
        VertexConsumer glow = RitualMesh.light(buffer, RitualMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(0, cy, 0), (sy * 3.4f + flash * 5) * Math.max(seam, .3f), 0, -1,
                glow(color, (.45f * alive + .8f * flash)), light);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(0, cy, 0), .9f + 1.2f * widen, 0, -1, glow(0xFFFFFF, .7f * alive), light);
        RitualMesh.flat(glow, poseStack.last(), RitualMesh.Plane.of(new Vector3f(0, .04f, .6f), new Vector3f(1, 0, 0), new Vector3f(0, 0, 1),
                new Vector3f(0, 1, 0)), 0, 0, 3.6f * Math.max(.3f, widen), 0, 0, glow(color, .6f * alive), light);
        // Motes drawn in, spiralling into its heart
        VertexConsumer motes = RitualMesh.light(buffer, RitualMesh.MOTE);
        for (int k = 0; k < 26 && widen > 0; k++) {
            float t = (age * (.02f + .01f * hash(seed + k)) + hash(seed + k * 3)) % 1;
            float a = hash(seed + k * 5) * Mth.TWO_PI + t * 4;
            float r = (1 - t) * (2.4f + 1.4f * hash(seed + k * 7));
            float z = (hash(seed + k * 11) - .5f) * 2.2f * (1 - t);
            Vector3f m = new Vector3f(Mth.cos(a) * r * (sx / WIDTH), cy + Mth.sin(a) * r * (sy / HEIGHT), z);
            RitualMesh.billboard(motes, poseStack, view, m, .3f * (.4f + .6f * (1 - t)), 0, -1, glow(k % 3 == 0 ? 0xFFFFFF : hot, Mth.sin(t * Mth.PI) * alive),
                    light);
        }
        // Shards of amethyst wheeling round the rim, catching its light
        VertexConsumer shards = buffer.getBuffer(RenderType.entityTranslucent(RitualMesh.CRYSTAL));
        p = poseStack.last();
        int count = 9;
        float ring = Math.max(seam * .4f, widen);
        for (int k = 0; k < count; k++) {
            float a = k * Mth.TWO_PI / count + spin * 1.3f + .2f * Mth.sin(age * .05f + k);
            float r = 1.28f + .12f * Mth.sin(age * .08f + k * 2);
            Vector3f c = new Vector3f(Mth.cos(a) * (sx + .45f) * r / 1.28f, cy + Mth.sin(a) * (sy + .45f) * r / 1.28f, .15f * Mth.sin(age * .07f + k));
            Vector3f along = new Vector3f(-Mth.sin(a), Mth.cos(a), .3f * Mth.cos(age * .06f + k)).normalize();
            float len = (.45f + .25f * hash(seed + k * 13)) * ring;
            if (len > .02f) {
                RitualMesh.crystal(shards, p, new Vector3f(c).sub(new Vector3f(along).mul(len * .4f)), along, len, .12f * ring, age * .1f + k, argb(0xFFFFFF,
                        .95f * Math.min(1, ring * 2) * (1 - closing)), light);
            }
        }
        VertexConsumer sparks = RitualMesh.light(buffer, RitualMesh.MOTE);
        for (int k = 0; k < count; k++) {
            float a = k * Mth.TWO_PI / count + spin * 1.3f + .2f * Mth.sin(age * .05f + k);
            Vector3f c = new Vector3f(Mth.cos(a) * (sx + .45f), cy + Mth.sin(a) * (sy + .45f), .15f * Mth.sin(age * .07f + k));
            RitualMesh.billboard(sparks, poseStack, view, c, .55f * ring, age * .1f, -1, glow(hot, .6f * ring * (1 - closing)), light);
        }
        // Closing: a flash where it folds away
        if (closing > .6f) {
            VertexConsumer burst = RitualMesh.light(buffer, RitualMesh.GLOW);
            float k = Math.max(0, 1 - Math.abs(closing - .9f) * 5);
            RitualMesh.billboard(burst, poseStack, view, new Vector3f(0, cy, 0), 2 + 5 * k, 0, -1, glow(0xFFFFFF, k), light);
        }
    }

    /** A ribbon seen from both sides (light is drawn one-sided): once, then again the other way round. */
    private static void twoSided(VertexConsumer c, PoseStack.Pose p, Vector3f[] pts, Vector3f[] side, float[] w, int[] col, float vPerBlock, float flow, int light) {
        ribbon(c, p, pts, side, w, col, vPerBlock, flow, light, true);
        int n = pts.length;
        Vector3f[] rp = new Vector3f[n], rs = new Vector3f[n];
        float[] rw = new float[n];
        int[] rc = new int[n];
        for (int i = 0; i < n; i++) {
            rp[i] = pts[n - 1 - i];
            rs[i] = side[n - 1 - i];
            rw[i] = w[n - 1 - i];
            rc[i] = col[n - 1 - i];
        }
        ribbon(c, p, rp, rs, rw, rc, vPerBlock, -flow, light, true);
    }

    /**
     * An upright oval of {@code rx} by {@code ry} centred {@code cy} up, its texture turned by {@code turn} round its
     * middle (shifted by {@code drift}); {@code facing} 1 to face {@code +z}, -1 for its back.
     */
    private static void disc(VertexConsumer c, PoseStack.Pose p, float z, float cy, float rx, float ry, float turn, float drift, int argb, int light, int facing) {
        Vector3f n = new Vector3f(0, 0, facing);
        for (int r = 0; r < RINGS; r++) {
            float r0 = (float) r / RINGS, r1 = (float) (r + 1) / RINGS;
            for (int s = 0; s < SECTORS; s++) {
                float a0 = s * Mth.TWO_PI / SECTORS, a1 = (s + 1) * Mth.TWO_PI / SECTORS;
                if (facing < 0) {
                    float t = a0;
                    a0 = a1;
                    a1 = t;
                }
                corner(c, p, z, cy, rx, ry, r0, a0, turn, drift, argb, light, n);
                corner(c, p, z, cy, rx, ry, r1, a0, turn, drift, argb, light, n);
                corner(c, p, z, cy, rx, ry, r1, a1, turn, drift, argb, light, n);
                corner(c, p, z, cy, rx, ry, r0, a1, turn, drift, argb, light, n);
            }
        }
    }

    private static void corner(VertexConsumer c, PoseStack.Pose p, float z, float cy, float rx, float ry, float r, float a, float turn, float drift, int argb,
                               int light, Vector3f n) {
        float x = Mth.cos(a) * r, y = Mth.sin(a) * r;
        float tu = Mth.cos(a + turn) * r, tv = Mth.sin(a + turn) * r;
        put(c, p, new Vector3f(x * rx, cy + y * ry, z), .5f + tu * .5f + drift, .5f - tv * .5f, argb, light, n);
    }
}
