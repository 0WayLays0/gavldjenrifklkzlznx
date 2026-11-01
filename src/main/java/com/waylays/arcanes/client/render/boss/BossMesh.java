package com.waylays.arcanes.client.render.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * What the great Boss Magic attacks are drawn with: their textures, and the shapes they share (fangs and thorns that
 * curve to a point, spheres, flat rings of light, the triangles of a fan of light).
 *
 * <p>Additive light ({@link #light}) is culled like the game's: its quads face the eye or are drawn both ways round.
 */
public final class BossMesh {
    public static final ResourceLocation MOON = ArcanesOublies.id("textures/entity/boss/moon.png");
    public static final ResourceLocation MOON_CRACKS = ArcanesOublies.id("textures/entity/boss/moon_cracks.png");
    public static final ResourceLocation DISC = ArcanesOublies.id("textures/entity/boss/disc.png");
    public static final ResourceLocation SPIKE = ArcanesOublies.id("textures/entity/boss/spike.png");
    public static final ResourceLocation FEATHER = ArcanesOublies.id("textures/entity/boss/feather.png");
    public static final ResourceLocation CHAIN = ArcanesOublies.id("textures/entity/boss/chain.png");
    public static final ResourceLocation IRON = ArcanesOublies.id("textures/entity/boss/iron.png");
    public static final ResourceLocation SWORD = ArcanesOublies.id("textures/entity/boss/sword.png");
    public static final ResourceLocation PRISM = ArcanesOublies.id("textures/entity/boss/prism.png");
    public static final ResourceLocation RUNES = ArcanesOublies.id("textures/entity/boss/rune_circle_0.png");
    public static final ResourceLocation RUNES_VOID = ArcanesOublies.id("textures/entity/boss/rune_circle_2.png");
    public static final ResourceLocation EDGE = ArcanesOublies.id("textures/entity/boss/edge.png");
    // Borrowed from the other schools
    public static final ResourceLocation GLOW = ArcanesOublies.id("textures/entity/ritual/glow.png");
    public static final ResourceLocation MOTE = ArcanesOublies.id("textures/entity/ritual/mote.png");
    public static final ResourceLocation BEAM = ArcanesOublies.id("textures/entity/ritual/beam.png");
    public static final ResourceLocation SWIRL = ArcanesOublies.id("textures/entity/ritual/portal_swirl.png");
    public static final ResourceLocation VOID = ArcanesOublies.id("textures/entity/ritual/portal_void.png");
    public static final ResourceLocation RING = ArcanesOublies.id("textures/entity/starfire/ring.png");
    public static final ResourceLocation NOISE = ArcanesOublies.id("textures/entity/starfire/flame_noise.png");
    public static final ResourceLocation SMOKE = ArcanesOublies.id("textures/entity/starfire/smoke.png");
    public static final ResourceLocation EMBER = ArcanesOublies.id("textures/entity/starfire/ember.png");
    public static final ResourceLocation CLOUD = ArcanesOublies.id("textures/entity/water/cloud.png");
    public static final ResourceLocation DUST = ArcanesOublies.id("textures/entity/wind/dust.png");
    public static final ResourceLocation SHEEN = ArcanesOublies.id("textures/entity/wind/sheen.png");

    public static final Vector3f UP = new Vector3f(0, 1, 0);

    private BossMesh() {}

    public static VertexConsumer light(MultiBufferSource buffer, ResourceLocation texture) {
        return buffer.getBuffer(RenderType.eyes(texture));
    }

    public static VertexConsumer see(MultiBufferSource buffer, ResourceLocation texture) {
        return buffer.getBuffer(RenderType.entityTranslucent(texture));
    }

    /**
     * See-through like {@link #see}, but only from the side its faces turn to: for what lies on the ground, which a
     * third-person camera dipping to the floor would otherwise see from beneath, covering the whole view.
     */
    public static VertexConsumer ground(MultiBufferSource buffer, ResourceLocation texture) {
        return buffer.getBuffer(RenderType.entityTranslucentCull(texture));
    }

    public static VertexConsumer solid(MultiBufferSource buffer, ResourceLocation texture) {
        return buffer.getBuffer(RenderType.entityCutout(texture));
    }

    public static int argb(int rgb, float alpha) {
        return WaterMesh.argb(rgb, alpha);
    }

    public static int glow(int rgb, float k) {
        return WaterMesh.glow(rgb, k);
    }

    public static int mix(int a, int b, float t) {
        return WaterMesh.mix(a, b, t);
    }

    public static float hash(int seed) {
        return WaterMesh.hash(seed);
    }

    /** Smooth 0 to 1 as {@code x} goes from {@code a} to {@code b}. */
    public static float ramp(float x, float a, float b) {
        float t = Mth.clamp((x - a) / (b - a), 0, 1);
        return t * t * (3 - 2 * t);
    }

    /** A unit vector square to {@code d}. */
    public static Vector3f across(Vector3f d) {
        Vector3f s = Math.abs(d.y) < .95f ? new Vector3f(d).cross(UP) : new Vector3f(d).cross(1, 0, 0);
        return s.normalize();
    }

    /**
     * A horn (a fang, a thorn) from {@code base} along {@code dir} (unit), {@code len} long, {@code r} thick at its root
     * and drawn to a point, bending toward {@code bend} as it goes (its point pushed {@code bend} times its length that
     * way); {@code sides} round and {@code segs} long. Its colour runs from {@code root} to {@code tip} (soon); the texture
     * wraps round it, its V from 1 at the root to 0 at the point.
     */
    public static void horn(VertexConsumer c, PoseStack.Pose p, Vector3f base, Vector3f dir, Vector3f bend, float len, float r, int sides, int segs, int root,
                            int tip, int light) {
        Vector3f[][] ring = new Vector3f[segs + 1][sides + 1];
        Vector3f[][] nrm = new Vector3f[segs + 1][sides + 1];
        Vector3f s1 = across(dir);
        for (int i = 0; i <= segs; i++) {
            float t = (float) i / segs;
            Vector3f at = new Vector3f(dir).mul(len * t).add(new Vector3f(bend).mul(len * t * t)).add(base);
            Vector3f tan = new Vector3f(dir).add(new Vector3f(bend).mul(2 * t)).normalize();
            Vector3f a = new Vector3f(s1).sub(new Vector3f(tan).mul(s1.dot(tan))).normalize();
            Vector3f b = new Vector3f(tan).cross(a).normalize();
            float rad = r * (float) Math.pow(1 - t, .85f);
            for (int s = 0; s <= sides; s++) {
                float ang = s * Mth.TWO_PI / sides;
                Vector3f out = new Vector3f(a).mul(Mth.cos(ang)).add(new Vector3f(b).mul(Mth.sin(ang)));
                ring[i][s] = new Vector3f(out).mul(rad).add(at);
                nrm[i][s] = out;
            }
        }
        for (int i = 0; i < segs; i++) {
            float t0 = (float) i / segs, t1 = (float) (i + 1) / segs;
            // The root's colour only near the root: most of a horn is the colour of its point
            int c0 = blend(root, tip, 1 - (1 - t0) * (1 - t0) * (1 - t0)), c1 = blend(root, tip, 1 - (1 - t1) * (1 - t1) * (1 - t1));
            for (int s = 0; s < sides; s++) {
                float u0 = (float) s / sides, u1 = (float) (s + 1) / sides;
                WaterMesh.put(c, p, ring[i][s], u0, 1 - t0, c0, light, nrm[i][s]);
                WaterMesh.put(c, p, ring[i][s + 1], u1, 1 - t0, c0, light, nrm[i][s + 1]);
                WaterMesh.put(c, p, ring[i + 1][s + 1], u1, 1 - t1, c1, light, nrm[i + 1][s + 1]);
                WaterMesh.put(c, p, ring[i + 1][s], u0, 1 - t1, c1, light, nrm[i + 1][s]);
            }
        }
    }

    /** {@code a} to {@code b} (alpha too). */
    public static int blend(int a, int b, float t) {
        t = Mth.clamp(t, 0, 1);
        int al = (int) Mth.lerp(t, a >>> 24, b >>> 24);
        return al << 24 | mix(a, b, t);
    }

    /**
     * A sphere round {@code centre}, {@code r} across, its texture wrapped round it once (turned by {@code turn}, 0 to
     * 1), from {@code v0} at its top to {@code v1} at its bottom of the latitudes (0 to 1: the whole of it). Faces out.
     */
    public static void sphere(VertexConsumer c, PoseStack.Pose p, Vector3f centre, float r, int rings, int segs, float turn, float lat0, float lat1, int argb,
                              int light) {
        for (int i = 0; i < rings; i++) {
            float a0 = Mth.lerp((float) i / rings, lat0, lat1), a1 = Mth.lerp((float) (i + 1) / rings, lat0, lat1);
            for (int s = 0; s < segs; s++) {
                float b0 = (float) s / segs, b1 = (float) (s + 1) / segs;
                corner(c, p, centre, r, a0, b0, turn, argb, light);
                corner(c, p, centre, r, a0, b1, turn, argb, light);
                corner(c, p, centre, r, a1, b1, turn, argb, light);
                corner(c, p, centre, r, a1, b0, turn, argb, light);
            }
        }
    }

    private static void corner(VertexConsumer c, PoseStack.Pose p, Vector3f centre, float r, float lat, float lon, float turn, int argb, int light) {
        float th = lat * Mth.PI, ph = lon * Mth.TWO_PI;
        Vector3f n = new Vector3f(Mth.sin(th) * Mth.cos(ph), Mth.cos(th), Mth.sin(th) * Mth.sin(ph));
        WaterMesh.put(c, p, new Vector3f(n).mul(r).add(centre), lon + turn, lat, argb, light, n);
    }

    /**
     * A flat ring from {@code r0} to {@code r1} round {@code centre} in the plane of {@code u} and {@code v}, facing
     * {@code u x v} (and the other way too if {@code both}): its texture wrapped round it {@code tiles} times along U,
     * running out from the middle along V (shifted by {@code flow}); colour {@code in} at its inner edge, {@code out}
     * at its outer.
     */
    public static void annulus(VertexConsumer c, PoseStack.Pose p, Vector3f centre, Vector3f u, Vector3f v, float r0, float r1, int segs, float tiles,
                               float turn, float flow, int in, int out, int light, boolean both) {
        Vector3f n = new Vector3f(u).cross(v).normalize();
        Vector3f back = new Vector3f(n).negate();
        for (int s = 0; s < segs; s++) {
            float a0 = s * Mth.TWO_PI / segs + turn, a1 = (s + 1) * Mth.TWO_PI / segs + turn;
            Vector3f i0 = point(centre, u, v, r0, a0), o0 = point(centre, u, v, r1, a0);
            Vector3f o1 = point(centre, u, v, r1, a1), i1 = point(centre, u, v, r0, a1);
            float tu0 = (float) s / segs * tiles, tu1 = (float) (s + 1) / segs * tiles;
            WaterMesh.put(c, p, i0, tu0, -flow, in, light, n);
            WaterMesh.put(c, p, o0, tu0, 1 - flow, out, light, n);
            WaterMesh.put(c, p, o1, tu1, 1 - flow, out, light, n);
            WaterMesh.put(c, p, i1, tu1, -flow, in, light, n);
            if (both) {
                WaterMesh.put(c, p, i1, tu1, -flow, in, light, back);
                WaterMesh.put(c, p, o1, tu1, 1 - flow, out, light, back);
                WaterMesh.put(c, p, o0, tu0, 1 - flow, out, light, back);
                WaterMesh.put(c, p, i0, tu0, -flow, in, light, back);
            }
        }
    }

    private static Vector3f point(Vector3f centre, Vector3f u, Vector3f v, float r, float a) {
        return new Vector3f(u).mul(Mth.cos(a) * r).add(new Vector3f(v).mul(Mth.sin(a) * r)).add(centre);
    }

    /** A square of the whole texture in the plane of {@code u} and {@code v} round {@code centre}, {@code size} across, both faces. */
    public static void plate(VertexConsumer c, PoseStack.Pose p, Vector3f centre, Vector3f u, Vector3f v, float size, int argb, int light) {
        float h = size * .5f;
        Vector3f a = new Vector3f(centre).sub(new Vector3f(u).mul(h)).sub(new Vector3f(v).mul(h));
        Vector3f b = new Vector3f(centre).add(new Vector3f(u).mul(h)).sub(new Vector3f(v).mul(h));
        Vector3f d = new Vector3f(centre).add(new Vector3f(u).mul(h)).add(new Vector3f(v).mul(h));
        Vector3f e = new Vector3f(centre).sub(new Vector3f(u).mul(h)).add(new Vector3f(v).mul(h));
        Vector3f n = new Vector3f(u).cross(v).normalize();
        quad(c, p, a, b, d, e, 0, 1, 1, 1, 1, 0, 0, 0, argb, argb, light, n);
        Vector3f back = new Vector3f(n).negate();
        quad(c, p, e, d, b, a, 0, 0, 1, 0, 1, 1, 0, 1, argb, argb, light, back);
    }

    /**
     * A quad through four points with their texture coordinates, its first two corners {@code argbA}, its last two
     * {@code argbB}.
     */
    public static void quad(VertexConsumer c, PoseStack.Pose p, Vector3f a, Vector3f b, Vector3f d, Vector3f e, float ua, float va, float ub, float vb, float ud,
                            float vd, float ue, float ve, int argbA, int argbB, int light, Vector3f n) {
        WaterMesh.put(c, p, a, ua, va, argbA, light, n);
        WaterMesh.put(c, p, b, ub, vb, argbA, light, n);
        WaterMesh.put(c, p, d, ud, vd, argbB, light, n);
        WaterMesh.put(c, p, e, ue, ve, argbB, light, n);
    }

    /** A quad seen from both sides (for light, which is culled): once, then the other way round. */
    public static void quad2(VertexConsumer c, PoseStack.Pose p, Vector3f a, Vector3f b, Vector3f d, Vector3f e, float ua, float va, float ub, float vb, float ud,
                             float vd, float ue, float ve, int argbA, int argbB, int light) {
        Vector3f n = new Vector3f(b).sub(a).cross(new Vector3f(e).sub(a));
        n = n.lengthSquared() < 1.0E-12f ? new Vector3f(UP) : n.normalize();
        quad(c, p, a, b, d, e, ua, va, ub, vb, ud, vd, ue, ve, argbA, argbB, light, n);
        Vector3f back = new Vector3f(n).negate();
        WaterMesh.put(c, p, e, ue, ve, argbB, light, back);
        WaterMesh.put(c, p, d, ud, vd, argbB, light, back);
        WaterMesh.put(c, p, b, ub, vb, argbA, light, back);
        WaterMesh.put(c, p, a, ua, va, argbA, light, back);
    }

    /** A glow lying on the ground at {@code (x, y, z)}, {@code size} across, facing up (seen from above). */
    public static void pool(VertexConsumer c, PoseStack.Pose p, float x, float y, float z, float size, float spin, int argb, int light) {
        float h = size * .5f;
        float cs = Mth.cos(spin) * h, sn = Mth.sin(spin) * h;
        Vector3f n = UP;
        WaterMesh.put(c, p, new Vector3f(x - cs + sn, y, z - sn - cs), 0, 0, argb, light, n);
        WaterMesh.put(c, p, new Vector3f(x - cs - sn, y, z - sn + cs), 0, 1, argb, light, n);
        WaterMesh.put(c, p, new Vector3f(x + cs - sn, y, z + sn + cs), 1, 1, argb, light, n);
        WaterMesh.put(c, p, new Vector3f(x + cs + sn, y, z + sn - cs), 1, 0, argb, light, n);
    }

    /** A jagged line from {@code a} to {@code b}: {@code n} points, each pushed aside up to {@code rough} of the length (none at the ends). */
    public static Vector3f[] jagged(Vector3f a, Vector3f b, int n, float rough, int seed) {
        Vector3f d = new Vector3f(b).sub(a);
        float len = d.length();
        Vector3f s1 = across(new Vector3f(d).normalize());
        Vector3f s2 = new Vector3f(d).normalize().cross(s1).normalize();
        Vector3f[] pts = new Vector3f[n];
        for (int i = 0; i < n; i++) {
            float t = (float) i / (n - 1);
            float k = i == 0 || i == n - 1 ? 0 : rough * len * Mth.sin(t * Mth.PI) * .5f;
            float ox = (hash(seed + i * 7) * 2 - 1) * k, oy = (hash(seed + i * 11) * 2 - 1) * k;
            pts[i] = new Vector3f(a).lerp(b, t).add(new Vector3f(s1).mul(ox)).add(new Vector3f(s2).mul(oy));
        }
        return pts;
    }

    /**
     * A quad through {@code a, b, d, e} (in order round it) wound to face {@code out} (light is culled: seen only from
     * the side it faces), its corners' texture coordinates and colours given in the same order.
     */
    public static void faced(VertexConsumer c, PoseStack.Pose p, Vector3f a, Vector3f b, Vector3f d, Vector3f e, float[] uv, int ca, int cb, int cd, int ce,
                             int light, Vector3f out) {
        Vector3f n = new Vector3f(b).sub(a).cross(new Vector3f(d).sub(a));
        if (n.lengthSquared() < 1.0E-14f) {
            n = new Vector3f(d).sub(b).cross(new Vector3f(e).sub(b));
        }
        boolean flip = n.dot(out) < 0;
        n = n.lengthSquared() < 1.0E-14f ? new Vector3f(out) : n.normalize();
        if (flip) {
            n.negate();
            WaterMesh.put(c, p, a, uv[0], uv[1], ca, light, n);
            WaterMesh.put(c, p, e, uv[6], uv[7], ce, light, n);
            WaterMesh.put(c, p, d, uv[4], uv[5], cd, light, n);
            WaterMesh.put(c, p, b, uv[2], uv[3], cb, light, n);
        } else {
            WaterMesh.put(c, p, a, uv[0], uv[1], ca, light, n);
            WaterMesh.put(c, p, b, uv[2], uv[3], cb, light, n);
            WaterMesh.put(c, p, d, uv[4], uv[5], cd, light, n);
            WaterMesh.put(c, p, e, uv[6], uv[7], ce, light, n);
        }
    }


    /** A box round {@code centre}, its half-extents along three axes. */
    public static void box(VertexConsumer c, PoseStack.Pose p, Vector3f centre, Vector3f hx, Vector3f hy, Vector3f hz, int argb, int light) {
        Vector3f[] k = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            k[i] = new Vector3f(centre).add(new Vector3f(hx).mul((i & 1) == 0 ? -1 : 1)).add(new Vector3f(hy).mul((i & 2) == 0 ? -1 : 1))
                    .add(new Vector3f(hz).mul((i & 4) == 0 ? -1 : 1));
        }
        int[][] faces = {{0, 4, 6, 2}, {5, 1, 3, 7}, {4, 5, 7, 6}, {1, 0, 2, 3}, {2, 6, 7, 3}, {0, 1, 5, 4}};
        float[] uv = {0, 1, 1, 1, 1, 0, 0, 0};
        for (int[] q : faces) {
            Vector3f mid = new Vector3f(k[q[0]]).add(k[q[1]]).add(k[q[2]]).add(k[q[3]]).mul(.25f);
            faced(c, p, k[q[0]], k[q[1]], k[q[2]], k[q[3]], uv, argb, argb, argb, argb, light, mid.sub(centre));
        }
    }

}
