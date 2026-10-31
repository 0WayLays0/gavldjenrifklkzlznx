package com.waylays.arcanes.client.render.water;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import static com.waylays.arcanes.client.render.water.WaterMesh.*;

/**
 * Tidal Wave, drawn as a real breaking wave rolling away along +Z: a long gentle back, deep blue, laced with foam and
 * streaming up towards the crest; the crest thin enough for the light to come through it turquoise; a steep hollow
 * face in front; the lip thrown forward off the crest and curling down into a tube, white at its edge, dropping water;
 * spray torn off the crest and blown back over it; white water churning at its foot. Its height rises and falls along
 * its length, and it curls further in some places than in others. At the end the lip crashes down and the whole wave
 * collapses into a rolling bank of white water.
 */
public final class TidalWaveModel {
    private static final int ACROSS = 30;
    /** The body's profile, from the foot of its face up over the crest and down its back: (forward, up), a wave one high. */
    private static final float[][] BODY = {
            {.62f, 0}, {.44f, .16f}, {.36f, .34f}, {.38f, .52f}, {.46f, .7f}, {.52f, .84f}, {.42f, .96f}, {.2f, 1.01f}, {-.1f, .97f},
            {-.38f, .86f}, {-.68f, .68f}, {-.98f, .48f}, {-1.28f, .28f}, {-1.56f, .11f}, {-1.85f, 0}};
    private static final int TOP = 7;
    private static final int LIP = 9;

    private TidalWaveModel() {}

    /**
     * {@code stand} (0 to 1) how tall it stands, {@code broken} (0 to 1) how far it has crashed at the end;
     * {@code width} and {@code height} its full size.
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, View view, float age, float stand, float broken, float width, float height,
                            int seed, int light) {
        if (stand <= .01f) {
            return;
        }
        int nx = ACROSS + 1, nb = BODY.length;
        float[] xs = new float[nx], hs = new float[nx], curls = new float[nx], fades = new float[nx];
        for (int i = 0; i < nx; i++) {
            float x = -width * .5f + width * i / ACROSS;
            float e = Math.abs(x) / (width * .5f);
            float taper = Mth.clamp((1 - e) * 3.5f, 0, 1);
            xs[i] = x;
            fades[i] = taper;
            float swell = 1 + .14f * noise(x * .22f + age * .03f, seed, 0) + .06f * noise(x * .8f - age * .06f, seed, 5);
            hs[i] = height * stand * swell * (.35f + .65f * (float) Math.sqrt(taper)) * (1 - .75f * broken);
            // It curls over more as it rolls on, sooner in some places than in others
            float curl = Mth.clamp(.3f + .45f * stand + .2f * noise(x * .3f - age * .04f, seed, 9) + age * .004f, 0, .95f);
            curls[i] = Mth.clamp(curl + broken * 1.2f, 0, 1.4f);
        }
        float depth = 1.05f;

        // The body
        Vector3f[][] body = new Vector3f[nx][nb];
        float[][] bu = new float[nx][nb], bv = new float[nx][nb];
        int[][] bc = new int[nx][nb];
        for (int i = 0; i < nx; i++) {
            float h = hs[i];
            float along = 0;
            for (int j = 0; j < nb; j++) {
                float fz = BODY[j][0], fy = BODY[j][1];
                if (j > 0) {
                    along += (float) Math.hypot(fz - BODY[j - 1][0], fy - BODY[j - 1][1]);
                }
                float ripple = .04f * noise(xs[i] * .7f, j * .8f - age * .2f, seed) * h;
                float z = fz * h * depth + (fz > 0 ? .3f * curls[i] * fz * h : 0);
                body[i][j] = new Vector3f(xs[i], Math.max(0, fy * h + ripple), z);
                bu[i][j] = xs[i] * .25f;
                bv[i][j] = -along * h * .35f + age * .06f;
                bc[i][j] = argb(bodyColour(j, fy), bodyAlpha(j, fy) * fades[i]);
            }
        }
        VertexConsumer water = translucent(buffer, WATER);
        PoseStack.Pose p = poseStack.last();
        grid(water, p, body, bu, bv, bc, light, .35f);

        // The lip: thrown forward off the crest and curling down
        int nl = LIP;
        Vector3f[][] lip = new Vector3f[nx][nl];
        float[][] lu = new float[nx][nl], lv = new float[nx][nl];
        int[][] lc = new int[nx][nl];
        for (int i = 0; i < nx; i++) {
            float h = hs[i];
            Vector3f start = body[i][5];
            float len = h * (.38f + .32f * Math.min(1, curls[i]));
            float angle = .55f;
            Vector3f at = new Vector3f(start);
            for (int j = 0; j < nl; j++) {
                float s = (float) j / (nl - 1);
                if (j > 0) {
                    float step = len / (nl - 1);
                    angle -= curls[i] * 3.4f / (nl - 1);
                    at = new Vector3f(at).add(0, Mth.sin(angle) * step, Mth.cos(angle) * step);
                }
                float ripple = .05f * noise(xs[i] * .9f, s * 2 - age * .3f, seed + 3) * h;
                lip[i][j] = new Vector3f(at.x, Math.max(0, at.y + ripple), at.z);
                lu[i][j] = xs[i] * .25f;
                lv[i][j] = s * len * .35f - age * .08f;
                lc[i][j] = argb(mix(CREST, FOAM_WHITE, s * s), (.7f + .2f * s) * fades[i] * (1 - Math.max(0, broken - .3f)));
            }
        }
        grid(water, p, lip, lu, lv, lc, light, .65f);

        VertexConsumer foam = translucent(buffer, FOAM);
        p = poseStack.last();
        // Foam lace over its back, thickest near the crest, and on its face
        Vector3f[][] lace = new Vector3f[nx][nb];
        float[][] fu = new float[nx][nb], fv = new float[nx][nb];
        int[][] fc = new int[nx][nb];
        for (int i = 0; i < nx; i++) {
            for (int j = 0; j < nb; j++) {
                lace[i][j] = new Vector3f(body[i][j]).add(0, .03f, j < TOP ? .03f : -.03f);
                fu[i][j] = xs[i] * .3f + .3f;
                fv[i][j] = bv[i][j] * 1.3f + .5f;
                float nearCrest = 1 - Math.min(1, Math.abs(j - TOP) / 5f);
                float patch = Mth.clamp(.5f + noise(xs[i] * .35f, j * .5f - age * .05f, seed + 11) * 1.2f, 0, 1);
                fc[i][j] = argb(FOAM_WHITE, (.25f + .6f * nearCrest) * patch * fades[i] + broken * .7f);
            }
        }
        grid(foam, p, lace, fu, fv, fc, light, .8f);
        // White water along the crest and the lip's edge
        Vector3f[] crest = new Vector3f[nx], edge = new Vector3f[nx], foot = new Vector3f[nx], acrossX = new Vector3f[nx], flat = new Vector3f[nx];
        float[] cw = new float[nx], ew = new float[nx], footW = new float[nx];
        int[] cc = new int[nx], ec = new int[nx], footC = new int[nx];
        for (int i = 0; i < nx; i++) {
            crest[i] = new Vector3f(body[i][TOP]).add(0, .05f, 0);
            edge[i] = new Vector3f(lip[i][nl - 1]);
            foot[i] = new Vector3f(xs[i], .06f, body[i][0].z + .3f + .5f * curls[i] + .2f * noise(xs[i], age * .1f, seed));
            acrossX[i] = new Vector3f(0, .7f, -.7f).normalize();
            flat[i] = new Vector3f(0, 0, 1);
            cw[i] = (.5f + .35f * noise(xs[i] * .6f, age * .1f, seed + 2)) * (hs[i] / height + .3f);
            ew[i] = .45f;
            footW[i] = 1.4f + 1.6f * curls[i] + 2 * broken;
            cc[i] = argb(FOAM_WHITE, .85f * fades[i]);
            ec[i] = argb(FOAM_WHITE, .8f * fades[i] * (1 - broken));
            footC[i] = argb(FOAM_WHITE, (.55f + .35f * broken) * fades[i]);
        }
        Vector3f[] lipSide = new Vector3f[nx];
        for (int i = 0; i < nx; i++) {
            lipSide[i] = new Vector3f(0, 1, 0);
        }
        ribbonAcross(foam, p, crest, acrossX, cw, cc, light);
        // Lines of foam climbing its back towards the crest
        for (int k = 0; k < 3; k++) {
            float t = (age * .02f + k / 3f) % 1;
            float jf = nb - 1.5f - t * (nb - 1.5f - TOP - 1);
            int j0 = (int) jf;
            float f = jf - j0;
            Vector3f[] line = new Vector3f[nx], up = new Vector3f[nx];
            float[] lw = new float[nx];
            int[] lc2 = new int[nx];
            for (int i = 0; i < nx; i++) {
                line[i] = new Vector3f(body[i][j0]).lerp(body[i][Math.min(nb - 1, j0 + 1)], f).add(0, .04f, 0);
                up[i] = new Vector3f(0, .5f, -.85f).normalize();
                lw[i] = (.25f + .25f * noise(xs[i] * .5f, k * 3 + age * .05f, seed)) * (hs[i] / height);
                lc2[i] = argb(FOAM_WHITE, .55f * Mth.sin(t * Mth.PI) * fades[i] * Mth.clamp(.6f + noise(xs[i] * .4f, k, seed + 5), 0, 1));
            }
            ribbonAcross(foam, p, line, up, lw, lc2, light);
        }
        ribbonAcross(foam, p, edge, lipSide, ew, ec, light);
        ribbonAcross(foam, p, foot, flat, footW, footC, light);

        // Clumps of white water: blown off the crest, churning at its foot, and the bank it collapses into
        VertexConsumer spray = translucent(buffer, SPRAY);
        for (int k = 0; k < 46; k++) {
            int i = (int) (hash(seed + k * 7) * nx);
            float t = (age * .07f + hash(seed + k * 11)) % 1;
            Vector3f c = new Vector3f(crest[i]).add((hash(seed + k) - .5f) * .6f, .2f + 1.2f * t - .9f * t * t, -.3f - 2.2f * t);
            puff(spray, poseStack, view, c.x, c.y, c.z, .7f + 1.1f * t, age * .04f + k, k & 3, argb(FOAM_WHITE, .85f * (1 - t) * fades[i] * (1 - broken)), light);
        }
        for (int k = 0; k < 30; k++) {
            int i = (int) (hash(seed + k * 13) * nx);
            float t = (age * .09f + hash(seed + k * 17)) % 1;
            Vector3f c = new Vector3f(foot[i]).add((hash(seed + k * 3) - .5f) * .8f, .25f + .9f * Mth.sin(t * Mth.PI) * (.5f + curls[i]), .5f * t);
            puff(spray, poseStack, view, c.x, c.y, c.z, .9f + 1.3f * t + broken, age * .05f + k, (k + 1) & 3, argb(FOAM_WHITE, .8f * (1 - t) * fades[i]), light);
        }
        if (broken > 0) {
            for (int k = 0; k < 40; k++) {
                float x = (hash(seed + k * 19) - .5f) * width * .9f;
                float t = (age * .1f + hash(seed + k * 23)) % 1;
                puff(spray, poseStack, view, x, .3f + height * .5f * (1 - broken) * hash(seed + k) + .8f * t, .5f + 2.5f * broken * hash(seed + k * 29),
                        1.4f + 1.4f * t, age * .03f + k, k & 3, argb(FOAM_WHITE, .9f * (1 - t) * Mth.sin(Math.min(1, broken * 1.3f) * Mth.PI)), light);
            }
        }

        // Spray streaking back off the crest, drops falling off the lip
        VertexConsumer drops = translucent(buffer, STREAK);
        p = poseStack.last();
        Vector3f eye = view.eye();
        for (int k = 0; k < 70; k++) {
            int i = (int) (hash(seed + k * 31) * nx);
            float t = (age * .1f + hash(seed + k * 37)) % 1;
            boolean off = k % 3 == 0;
            Vector3f head, tail;
            if (off) {
                Vector3f from = edge[i];
                Vector3f v = new Vector3f(0, -.02f, .05f);
                head = thrown(from, v, t * 14, .025f);
                tail = thrown(from, v, Math.max(0, t * 14 - 2), .025f);
            } else {
                Vector3f from = crest[i];
                Vector3f v = new Vector3f((hash(seed + k) - .5f) * .05f, .12f + .08f * hash(seed + k * 3), -.14f - .1f * hash(seed + k * 5));
                head = thrown(from, v, t * 14, .022f);
                tail = thrown(from, v, Math.max(0, t * 14 - 1.8f), .022f);
            }
            if (head.y < 0) {
                continue;
            }
            streak(drops, p, eye, head, tail, .1f, argb(FOAM_WHITE, .9f * (1 - t) * fades[i] * (1 - broken * .5f)), light);
        }
    }

    /** A ribbon running across the wave (along X) through {@code pts}. */
    private static void ribbonAcross(VertexConsumer c, PoseStack.Pose p, Vector3f[] pts, Vector3f[] side, float[] width, int[] argb, int light) {
        ribbon(c, p, pts, side, width, argb, .3f, 0, light, true);
    }

    private static int bodyColour(int j, float fy) {
        if (j < TOP) {
            // The face: dark at its foot, lighter as it thins towards the crest
            return water(.3f + .75f * fy * fy);
        }
        if (j <= TOP + 1) {
            return mix(CREST, SHALLOW, .3f);
        }
        // The back: turquoise near the crest where the light comes through, deep blue down at its foot
        return mix(water(.35f + .3f * fy), CREST, Mth.clamp((fy - .75f) * 3, 0, 1));
    }

    private static float bodyAlpha(int j, float fy) {
        return j >= TOP - 1 && j <= TOP + 2 ? .72f : .88f - .1f * fy;
    }
}
