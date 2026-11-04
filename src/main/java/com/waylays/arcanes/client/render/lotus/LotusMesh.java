package com.waylays.arcanes.client.render.lotus;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.boss.Sculpt;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.sand.SandMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * What Lotus Magic's models are made of: the lotus flower itself (rings of real, cupped petals, white at their root and
 * blushing pink to their tips, veined, round a heart of gold stamens and a green seed head), its broad round leaves, its
 * stems, the seed pod, the knotted root that grows in the mud, and the light of the lotus (a soft pink, and the gold of
 * its heart).
 * <p>
 * Shapes are built in the model's own space (a flower stands on its origin, one block is about its outer petals' length)
 * and drawn under a pose that only moves and scales them, so the eye can be carried into that space with {@link #eye}.
 */
public final class LotusMesh {
    public static final ResourceLocation PETAL = ArcanesOublies.id("textures/entity/lotus/petal.png");
    public static final ResourceLocation LEAF = ArcanesOublies.id("textures/entity/lotus/leaf.png");
    public static final ResourceLocation POD = ArcanesOublies.id("textures/entity/lotus/pod.png");
    public static final ResourceLocation FIBRE = ArcanesOublies.id("textures/entity/lotus/fibre.png");
    public static final ResourceLocation ROOT = ArcanesOublies.id("textures/entity/lotus/root.png");
    public static final ResourceLocation MUD = ArcanesOublies.id("textures/entity/lotus/mud.png");
    public static final ResourceLocation MANDALA = ArcanesOublies.id("textures/entity/lotus/mandala.png");

    /** The pink of the school, a deeper rose, the white of a petal's root, the gold of the heart, leaf and stem greens. */
    public static final int PINK = 0xF58AC0;
    public static final int ROSE = 0xD8417F;
    public static final int BLUSH = 0xFFD6E8;
    /** What a petal's tip is tinted (the texture blushes on its own; this only deepens it a little). */
    public static final int TIP = 0xFAC8E0;
    public static final int GOLD = 0xFFD36E;
    public static final int STAMEN = 0xFFE07A;
    public static final int SEED_HEAD = 0xD8E070;
    public static final int STEM = 0x6E9E4A;
    public static final int SEPAL = 0x7FA65A;
    public static final int JADE = 0x3FB8A8;
    public static final int WHITE = 0xFFFFFFFF;

    /** A petal's grid: rows from root to tip, columns across. */
    public static final int ROWS = 8, COLS = 6;

    private LotusMesh() {}

    /** The eye (relative to a pose's origin) carried into a model drawn at {@code at}, {@code size} times its own size. */
    public static Vector3f eye(WaterMesh.View view, Vector3f at, float size) {
        return new Vector3f(view.eye()).sub(at).div(Math.max(size, 1.0E-4f));
    }

    // ---------------------------------------------------------------- the petal

    /** One petal's surface: its grid (root row first), the line down its middle, its two edges. */
    public record Petal(Vector3f[][] grid, Vector3f[] spine, Vector3f[] left, Vector3f[] right) {
        /** Drawn on both sides, its colour running from {@code root} at its root to {@code tip} at its tip. */
        public void draw(VertexConsumer c, PoseStack.Pose p, int root, int tip, int light) {
            int rows = grid.length - 1, cols = grid[0].length - 1;
            for (int i = 0; i < rows; i++) {
                int ca = BossMesh.blend(root, tip, (float) i / rows), cb = BossMesh.blend(root, tip, (float) (i + 1) / rows);
                for (int j = 0; j < cols; j++) {
                    float u0 = (float) j / cols, u1 = (float) (j + 1) / cols, v0 = (float) i / rows, v1 = (float) (i + 1) / rows;
                    BossMesh.quad2(c, p, grid[i][j], grid[i][j + 1], grid[i + 1][j + 1], grid[i + 1][j], u0, v0, u1, v0, u1, v1, u0, v1, ca, cb, light);
                }
            }
        }

        /** Its outline, from the root up one edge, round the tip and down the other. */
        public Vector3f[] outline() {
            int n = left.length;
            Vector3f[] edge = new Vector3f[2 * n - 1];
            for (int i = 0; i < n; i++) {
                edge[i] = left[i];
                edge[n - 1 + i] = right[n - 1 - i];
            }
            return edge;
        }

        /** A glowing line along its outline, {@code width} wide, facing {@code eye}. */
        public void rim(VertexConsumer c, PoseStack.Pose p, Vector3f eye, float width, int argb, int light) {
            Vector3f[] edge = outline();
            SandMesh.ribbon(c, p, eye, edge, Sculpt.fill(width, edge.length), 1, 0, Sculpt.fill(argb, edge.length), light);
        }

        /** Its tip. */
        public Vector3f tip() {
            return spine[spine.length - 1];
        }

        /** The same petal turned by {@code q}, scaled by {@code scale}, then moved to {@code at}. */
        public Petal moved(Quaternionf q, Vector3f at, float scale) {
            int rows = grid.length - 1, cols = grid[0].length - 1;
            Vector3f[][] g = new Vector3f[rows + 1][cols + 1];
            Vector3f[] s = new Vector3f[rows + 1], l = new Vector3f[rows + 1], r = new Vector3f[rows + 1];
            for (int i = 0; i <= rows; i++) {
                for (int j = 0; j <= cols; j++) {
                    g[i][j] = q.transform(new Vector3f(grid[i][j]).mul(scale)).add(at);
                }
                s[i] = q.transform(new Vector3f(spine[i]).mul(scale)).add(at);
                l[i] = g[i][0];
                r[i] = g[i][cols];
            }
            return new Petal(g, s, l, r);
        }
    }

    /**
     * A lotus petal standing on the origin: {@code len} long, {@code width} across at its widest (two fifths up), its
     * root narrow and its tip pointed. It rises along +Y leaning {@code tilt} (radians) out toward +Z, leaning
     * {@code curl} more by its tip (less, if negative: the tip turning back in); its sides cupped in (toward -Z) by
     * {@code cup} of its half-width.
     */
    public static Petal petal(float len, float width, float tilt, float cup, float curl) {
        return petal(len, width, tilt, cup, curl, ROWS, COLS);
    }

    /** {@link #petal} with a grid of {@code rows} by {@code cols} (fewer for the small petals of a storm). */
    public static Petal petal(float len, float width, float tilt, float cup, float curl, int rows, int cols) {
        Vector3f[][] grid = new Vector3f[rows + 1][cols + 1];
        Vector3f[] spine = new Vector3f[rows + 1], left = new Vector3f[rows + 1], right = new Vector3f[rows + 1];
        Vector3f at = new Vector3f();
        for (int i = 0; i <= rows; i++) {
            float v = (float) i / rows;
            float t = tilt + curl * v * v;
            Vector3f dir = new Vector3f(0, Mth.cos(t), Mth.sin(t));
            Vector3f inward = new Vector3f(0, Mth.sin(t), -Mth.cos(t));
            if (i > 0) {
                at.add(new Vector3f(dir).mul(len / rows));
            }
            spine[i] = new Vector3f(at);
            float s = (float) Math.pow(Math.max(0, Mth.sin(Mth.PI * (float) Math.pow(v, .85f))), .75f);
            float w = width * .5f * (s + .2f * (1 - v) * (1 - v) * (1 - v));
            // The spine stands a little proud of the cupped sides: a keel down its back
            for (int j = 0; j <= cols; j++) {
                float u = (float) j / cols * 2 - 1;
                grid[i][j] = new Vector3f(at).add(u * w, 0, 0).add(new Vector3f(inward).mul(cup * u * u * w));
            }
            left[i] = grid[i][0];
            right[i] = grid[i][cols];
        }
        return new Petal(grid, spine, left, right);
    }

    /** The turn that carries a petal's +Y onto {@code y} and leans its +Z as near {@code z} as it can. */
    public static Quaternionf frame(Vector3f y, Vector3f z) {
        Vector3f ny = new Vector3f(y).normalize();
        Vector3f nx = new Vector3f(ny).cross(z);
        if (nx.lengthSquared() < 1.0E-8f) {
            nx = BossMesh.across(ny);
        }
        nx.normalize();
        Vector3f nz = new Vector3f(nx).cross(ny).normalize();
        return new org.joml.Matrix3f(nx, ny, nz).getNormalizedRotation(new Quaternionf());
    }

    /** Draws a petal as light (added, glowing), its grid tinted {@code argb}. */
    public static void ghost(VertexConsumer c, PoseStack.Pose p, Petal petal, int argb, int light) {
        petal.draw(c, p, argb, argb, light);
    }

    // ---------------------------------------------------------------- the flower

    /**
     * One ring of petals: how many, turned how far (a fraction of the step between them), how long and wide, their
     * root's distance from the middle and height, how far they lean when closed and when open, how cupped, how they
     * curl at the tip.
     */
    public record Ring(int count, float offset, float len, float width, float root, float y, float closed, float open, float cup, float curl) {}

    /** The lotus: two broad outer rings, then smaller ones cupped round the heart. */
    public static final Ring[] LOTUS = {
            new Ring(8, 0, 1.0f, .62f, .2f, .06f, .3f, 1.28f, .55f, .35f),
            new Ring(8, .5f, .92f, .58f, .18f, .1f, .2f, .92f, .6f, .12f),
            new Ring(7, .2f, .78f, .5f, .15f, .14f, .12f, .55f, .66f, -.12f),
            new Ring(5, .1f, .6f, .4f, .12f, .17f, .05f, .26f, .72f, -.32f)};

    /** A great lotus of many petals (for the Thousand-Petal Lotus): five rings, from wide open to still cupped. */
    public static final Ring[] THOUSAND = {
            new Ring(12, 0, 1.0f, .5f, .3f, .04f, .35f, 1.38f, .5f, .3f),
            new Ring(12, .5f, .92f, .48f, .27f, .08f, .28f, 1.12f, .55f, .2f),
            new Ring(10, .25f, .84f, .46f, .23f, .12f, .2f, .84f, .6f, .05f),
            new Ring(9, .1f, .72f, .42f, .19f, .15f, .12f, .55f, .66f, -.12f),
            new Ring(7, .6f, .58f, .36f, .15f, .18f, .05f, .3f, .72f, -.3f)};

    /**
     * The petals of a flower of {@code rings}, open {@code open} (0 a closed bud, 1 open; a little past 1 flung wide),
     * turned {@code spin} radians, each petal stirring a little with {@code age}.
     */
    public static List<Petal> petals(Ring[] rings, float open, float spin, float age, int seed) {
        List<Petal> out = new ArrayList<>();
        for (int r = 0; r < rings.length; r++) {
            Ring ring = rings[r];
            // The outer rings open first, the inner ones after
            float o = Mth.clamp(open * (1 + r * .12f) - r * .12f, 0, 1.3f);
            for (int k = 0; k < ring.count; k++) {
                float a = spin + (k + ring.offset) * Mth.TWO_PI / ring.count;
                float stir = .03f * Mth.sin(age * .07f + k * 1.9f + r * 2.3f + seed);
                float tilt = Mth.lerp(o, ring.closed, ring.open) + stir;
                // A closed bud's petals overlap: they hug the heart and twist a little round it
                float len = ring.len * (.92f + .08f * BossMesh.hash(seed + r * 31 + k * 7));
                Petal p = petal(len, ring.width, tilt, ring.cup * (1.15f - .3f * Math.min(o, 1)), ring.curl * Math.min(o, 1));
                Quaternionf q = new Quaternionf().rotationY(a);
                out.add(p.moved(q, q.transform(new Vector3f(0, ring.y, ring.root * (.55f + .45f * Math.min(o, 1)))), 1));
            }
        }
        return out;
    }

    /** Draws {@code petals}, white at the root blushing to {@code tip}. */
    public static void drawPetals(MultiBufferSource buffer, PoseStack.Pose p, List<Petal> petals, int root, int tip, int light) {
        VertexConsumer c = BossMesh.solid(buffer, PETAL);
        for (Petal pt : petals) {
            pt.draw(c, p, root, tip, light);
        }
    }

    /** Four green sepals cradling the flower from below, spreading as it opens. */
    public static void sepals(MultiBufferSource buffer, PoseStack.Pose p, float open, float spin, int light) {
        VertexConsumer c = BossMesh.solid(buffer, FIBRE);
        for (int k = 0; k < 4; k++) {
            float a = spin + (k + .25f) * Mth.HALF_PI;
            Petal s = petal(.42f, .3f, Mth.lerp(Math.min(open, 1), .5f, 1.75f), .5f, .3f);
            Quaternionf q = new Quaternionf().rotationY(a);
            s.moved(q, q.transform(new Vector3f(0, -.02f, .1f)), 1).draw(c, p, BossMesh.argb(SEPAL, 1), BossMesh.argb(0x557A3C, 1), light);
        }
    }

    /**
     * The heart of the flower: a seed head of yellow-green, flat on top, its seeds set in it; round it a crown of golden
     * stamens, spreading as the flower opens.
     */
    public static void heart(MultiBufferSource buffer, PoseStack.Pose p, float open, float spin, int light) {
        float o = Mth.clamp(open, 0, 1);
        VertexConsumer fibre = BossMesh.solid(buffer, FIBRE);
        // Its stamens: fine filaments, each tipped with a fat anther
        for (int k = 0; k < 22; k++) {
            float a = spin * 1.3f + k * Mth.TWO_PI / 22 + .4f * BossMesh.hash(k * 13);
            float reach = .2f + .1f * o + .05f * BossMesh.hash(k * 7);
            Vector3f from = new Vector3f(Mth.sin(a) * .12f, .14f, Mth.cos(a) * .12f);
            Vector3f to = new Vector3f(Mth.sin(a) * reach, .27f + .06f * BossMesh.hash(k * 3) - .04f * o, Mth.cos(a) * reach);
            Vector3f[] s = Sculpt.curve(from, new Vector3f(from).lerp(to, .5f).add(0, .05f, 0), to, 4);
            Sculpt.sweep(fibre, p, s, Sculpt.fill(.009f, 4), Sculpt.fill(.009f, 4), Sculpt.circle(4), false, null, 2, Sculpt.fill(BossMesh.argb(0xFFF2B0, 1), 4), false,
                    light);
            Vector3f d = new Vector3f(to).sub(from).normalize();
            Vector3f[] anther = {new Vector3f(to).sub(new Vector3f(d).mul(.01f)), new Vector3f(to).add(new Vector3f(d).mul(.03f)),
                    new Vector3f(to).add(new Vector3f(d).mul(.065f))};
            Sculpt.sweep(fibre, p, anther, new float[] {.01f, .024f, .008f}, new float[] {.01f, .024f, .008f}, Sculpt.circle(5), false, null, 2,
                    Sculpt.fill(BossMesh.argb(STAMEN, 1), 3), true, light);
        }
        // The seed head's flanks
        Sculpt.lathe(fibre, p, new float[] {.05f, .02f, .1f, .08f, .15f, .15f, .165f, .2f, .155f, .225f}, 16, 2, 2, BossMesh.argb(0x9DB84E, 1),
                BossMesh.argb(SEED_HEAD, 1), light);
        // Its flat top, the seeds in their pits
        podTop(BossMesh.solid(buffer, POD), p, .226f, .156f, light);
    }

    /** Where the pits of a seed head lie on its top (fractions of its radius, in pairs): one in the middle, six round it, ten round those. */
    public static final float[] PITS = pits();

    private static float[] pits() {
        float[] out = new float[34];
        int k = 2;
        for (int i = 0; i < 6; i++) {
            float a = i * Mth.TWO_PI / 6;
            out[k++] = Mth.cos(a) * .38f;
            out[k++] = Mth.sin(a) * .38f;
        }
        for (int i = 0; i < 10; i++) {
            float a = (i + .5f) * Mth.TWO_PI / 10;
            out[k++] = Mth.cos(a) * .74f;
            out[k++] = Mth.sin(a) * .74f;
        }
        return out;
    }

    /** The flat top of a seed head, {@code r} across at height {@code y}, facing up: the pod texture laid on it. */
    public static void podTop(VertexConsumer pod, PoseStack.Pose p, float y, float r, int light) {
        Vector3f top = new Vector3f(0, y, 0);
        int segs = 18;
        for (int s = 0; s < segs; s++) {
            float a0 = s * Mth.TWO_PI / segs, a1 = (s + 1) * Mth.TWO_PI / segs;
            Vector3f o0 = new Vector3f(Mth.cos(a0) * r, y, Mth.sin(a0) * r), o1 = new Vector3f(Mth.cos(a1) * r, y, Mth.sin(a1) * r);
            Sculpt.tri(pod, p, top, o1, o0, .5f, .5f, .5f + Mth.cos(a1) * .5f, .5f + Mth.sin(a1) * .5f, .5f + Mth.cos(a0) * .5f, .5f + Mth.sin(a0) * .5f, WHITE,
                    light);
        }
    }

    /**
     * A whole lotus at {@code at}, {@code size} times its own size: sepals, petals, heart; open {@code open}; its petals'
     * outlines glowing {@code glow} (0 for none) in {@code glowRgb}. Returns its petals (in its own space).
     */
    public static List<Petal> bloom(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Ring[] rings, Vector3f at, float size, float open,
                                    float spin, float age, int root, int tip, float glow, int glowRgb, int seed, int light) {
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        poseStack.scale(size, size, size);
        PoseStack.Pose p = poseStack.last();
        List<Petal> petals = petals(rings, open, spin, age, seed);
        drawPetals(buffer, p, petals, root, tip, light);
        sepals(buffer, p, open, spin + .4f, light);
        heart(buffer, p, open, spin, light);
        if (glow > .01f) {
            Vector3f eye = eye(view, at, size);
            VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
            for (Petal pt : petals) {
                pt.rim(beam, p, eye, .05f, BossMesh.glow(glowRgb, glow), light);
            }
            VertexConsumer g = BossMesh.light(buffer, BossMesh.GLOW);
            RitualMesh.billboard(g, poseStack, new WaterMesh.View(view.camera(), eye), new Vector3f(0, .3f, 0), 1.1f, 0, -1, BossMesh.glow(GOLD, glow * .8f), light);
        }
        poseStack.popPose();
        return petals;
    }

    // ---------------------------------------------------------------- leaves, stems, roots

    /**
     * A round lotus leaf lying flat at {@code at}, {@code r} across, turned {@code turn}: its edge waving and lifting
     * {@code lift} (a shallow bowl), drawn on both sides.
     */
    public static void leaf(VertexConsumer c, PoseStack.Pose p, Vector3f at, float r, float turn, float lift, int seed, int argb, int light) {
        int segs = 22, rings = 3;
        Vector3f[][] pts = new Vector3f[rings + 1][segs + 1];
        float[][] us = new float[rings + 1][segs + 1], vs = new float[rings + 1][segs + 1];
        for (int i = 0; i <= rings; i++) {
            float f = (float) i / rings;
            for (int s = 0; s <= segs; s++) {
                float a = s * Mth.TWO_PI / segs;
                float wave = 1 + .05f * Mth.sin(a * 7 + seed) * f;
                float rr = r * f * wave;
                float y = lift * f * f * r + .03f * r * Mth.sin(a * 5 + seed * 1.3f) * f * f;
                pts[i][s] = new Vector3f(Mth.cos(a + turn) * rr, y, Mth.sin(a + turn) * rr).add(at);
                us[i][s] = .5f + Mth.cos(a) * .5f * f * wave * .98f;
                vs[i][s] = .5f + Mth.sin(a) * .5f * f * wave * .98f;
            }
        }
        for (int i = 0; i < rings; i++) {
            for (int s = 0; s < segs; s++) {
                BossMesh.quad2(c, p, pts[i][s], pts[i + 1][s], pts[i + 1][s + 1], pts[i][s + 1], us[i][s], vs[i][s], us[i + 1][s], vs[i + 1][s], us[i + 1][s + 1],
                        vs[i + 1][s + 1], us[i][s + 1], vs[i][s + 1], argb, argb, light);
            }
        }
    }

    /** A green stem along {@code path}, {@code r} thick. */
    public static void stem(VertexConsumer fibre, PoseStack.Pose p, Vector3f[] path, float r, int light) {
        Sculpt.sweep(fibre, p, path, Sculpt.fill(r, path.length), Sculpt.fill(r, path.length), Sculpt.circle(6), false, null, 1.5f,
                Sculpt.fade(BossMesh.argb(0x557A3C, 1), BossMesh.argb(STEM, 1), path.length), false, light);
    }

    /**
     * The knotted root of the lotus along {@code path}: swollen lengths pinched at {@code nodes} knots, {@code r} thick,
     * thinner toward its end.
     */
    public static void root(VertexConsumer c, PoseStack.Pose p, Vector3f[] path, float r, int nodes, int light) {
        int n = path.length;
        float[] rs = new float[n];
        for (int i = 0; i < n; i++) {
            float t = (float) i / (n - 1);
            float f = t * nodes - (float) Math.floor(t * nodes);
            float swell = (float) Math.pow(Math.max(0, Mth.sin(Mth.PI * f)), .45f);
            rs[i] = r * (1 - .55f * t) * (.5f + .5f * swell);
        }
        rs[n - 1] = Math.min(rs[n - 1], r * .15f);
        Sculpt.sweep(c, p, path, rs, rs, Sculpt.circle(10), false, null, .8f, Sculpt.fade(BossMesh.argb(0xF2E2C4, 1), BossMesh.argb(0xC8A47A, 1), n), true, light);
    }

    /** A path of {@code n} points along a curve from {@code a} to {@code b}, bowed toward {@code c1} then {@code c2}. */
    public static Vector3f[] path(Vector3f a, Vector3f c1, Vector3f c2, Vector3f b, int n) {
        return Sculpt.bezier(a, c1, c2, b, n);
    }

    /** Small glowing motes rising round {@code at} within {@code radius}, {@code count} of them (pollen, fireflies). */
    public static void motes(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f at, float radius, float height, float age, int count,
                             int rgb, float alpha, int seed, int light) {
        if (alpha <= .01f) {
            return;
        }
        VertexConsumer g = BossMesh.light(buffer, BossMesh.MOTE);
        for (int k = 0; k < count; k++) {
            float h = BossMesh.hash(seed + k * 17);
            float life = (age * (.012f + .01f * h) + h) % 1;
            float a = BossMesh.hash(seed + k * 5) * Mth.TWO_PI + age * .01f * (h - .5f);
            float r = radius * (float) Math.sqrt(BossMesh.hash(seed + k * 29));
            Vector3f m = new Vector3f(at).add(Mth.cos(a) * r + .2f * Mth.sin(age * .05f + k), life * height, Mth.sin(a) * r);
            float fade = Mth.sin(life * Mth.PI);
            RitualMesh.billboard(g, poseStack, view, m, .12f + .1f * h, 0, -1, BossMesh.glow(rgb, alpha * fade), light);
        }
    }
}
