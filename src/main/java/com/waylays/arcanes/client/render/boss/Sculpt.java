package com.waylays.arcanes.client.render.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * The sculptor's tools the Boss models are made with, each drawing in the pose's own space (place and turn a part with
 * the pose stack): shapes turned on a lathe, sections swept along a path (blades, tentacles, chains, horns), shapes cut
 * from a plank with bevelled edges (crowns, clock hands, armour plates), rings, cut gems, flat discs.
 *
 * <p>Their faces are wound counter-clockwise seen from outside, their normals smoothed over gentle turns and kept sharp
 * over hard ones, so that the game's light models them; textures are laid by length (so many repeats a block) so a
 * material looks the same on every part.
 */
public final class Sculpt {
    /** Over a turn sharper than this (cosine between the faces' normals), an edge is kept sharp rather than smoothed. */
    private static final float CREASE = .72f;

    private Sculpt() {}

    // ---------------------------------------------------------------- faces

    public static void put(VertexConsumer c, PoseStack.Pose p, Vector3f at, float u, float v, int argb, int light, Vector3f n) {
        WaterMesh.put(c, p, at, u, v, argb, light, n);
    }

    private static Vector3f normal(Vector3f a, Vector3f b, Vector3f d) {
        Vector3f n = new Vector3f(b).sub(a).cross(new Vector3f(d).sub(a));
        return n.lengthSquared() < 1.0E-14f ? new Vector3f(0, 1, 0) : n.normalize();
    }

    /** A triangle (counter-clockwise from its front), drawn as a quad with its last corner twice. */
    public static void tri(VertexConsumer c, PoseStack.Pose p, Vector3f a, Vector3f b, Vector3f d, float ua, float va, float ub, float vb, float ud, float vd,
                           int argb, int light) {
        Vector3f n = normal(a, b, d);
        put(c, p, a, ua, va, argb, light, n);
        put(c, p, b, ub, vb, argb, light, n);
        put(c, p, d, ud, vd, argb, light, n);
        put(c, p, d, ud, vd, argb, light, n);
    }

    /** A flat quad (counter-clockwise from its front), its texture square laid from its first corner. */
    public static void quad(VertexConsumer c, PoseStack.Pose p, Vector3f a, Vector3f b, Vector3f d, Vector3f e, int argb, int light) {
        Vector3f n = normal(a, b, d);
        put(c, p, a, 0, 1, argb, light, n);
        put(c, p, b, 1, 1, argb, light, n);
        put(c, p, d, 1, 0, argb, light, n);
        put(c, p, e, 0, 0, argb, light, n);
    }

    // ---------------------------------------------------------------- the lathe

    /**
     * Turned all the way round the Y axis. {@code prof} lists {r, y} pairs from one end to the other; it faces out where
     * it climbs on the outside (and in where it comes down: the inside of a bell). U goes round ({@code wrap} repeats), V
     * along the profile by length ({@code vScale} repeats a block); the colour runs from {@code from} to {@code to} along it.
     */
    public static void lathe(VertexConsumer c, PoseStack.Pose p, float[] prof, int segs, float wrap, float vScale, int from, int to, int light) {
        lathe(c, p, prof, segs, 0, Mth.TWO_PI, wrap, vScale, from, to, light);
    }

    /** {@link #lathe} over the angles {@code a0} to {@code a1} only (radians round from +X toward +Z). */
    public static void lathe(VertexConsumer c, PoseStack.Pose p, float[] prof, int segs, float a0, float a1, float wrap, float vScale, int from, int to,
                             int light) {
        int n = prof.length / 2;
        if (n < 2) {
            return;
        }
        // Each stretch's normal in the profile's plane (out along r, up along y), and the length along it so far
        float[] sr = new float[n - 1], sy = new float[n - 1];
        float[] len = new float[n];
        for (int i = 0; i + 1 < n; i++) {
            float dr = prof[2 * i + 2] - prof[2 * i], dy = prof[2 * i + 3] - prof[2 * i + 1];
            float l = Mth.sqrt(dr * dr + dy * dy);
            sr[i] = l < 1.0E-6f ? 1 : dy / l;
            sy[i] = l < 1.0E-6f ? 0 : -dr / l;
            len[i + 1] = len[i] + l;
        }
        float total = Math.max(len[n - 1], 1.0E-4f);
        for (int i = 0; i + 1 < n; i++) {
            // The normal at each end of this stretch: shared with the next stretch over a gentle turn
            float[] na = endNormal(sr, sy, i, i - 1), nb = endNormal(sr, sy, i, i + 1);
            int ca = BossMesh.blend(from, to, len[i] / total), cb = BossMesh.blend(from, to, len[i + 1] / total);
            for (int s = 0; s < segs; s++) {
                float t0 = Mth.lerp((float) s / segs, a0, a1), t1 = Mth.lerp((float) (s + 1) / segs, a0, a1);
                float u0 = (float) s / segs * wrap, u1 = (float) (s + 1) / segs * wrap;
                float c0 = Mth.cos(t0), s0 = Mth.sin(t0), c1 = Mth.cos(t1), s1 = Mth.sin(t1);
                float r0 = prof[2 * i], y0 = prof[2 * i + 1], r1 = prof[2 * i + 2], y1 = prof[2 * i + 3];
                float v0 = len[i] * vScale, v1 = len[i + 1] * vScale;
                put(c, p, new Vector3f(c0 * r0, y0, s0 * r0), u0, v0, ca, light, new Vector3f(c0 * na[0], na[1], s0 * na[0]));
                put(c, p, new Vector3f(c0 * r1, y1, s0 * r1), u0, v1, cb, light, new Vector3f(c0 * nb[0], nb[1], s0 * nb[0]));
                put(c, p, new Vector3f(c1 * r1, y1, s1 * r1), u1, v1, cb, light, new Vector3f(c1 * nb[0], nb[1], s1 * nb[0]));
                put(c, p, new Vector3f(c1 * r0, y0, s1 * r0), u1, v0, ca, light, new Vector3f(c1 * na[0], na[1], s1 * na[0]));
            }
        }
    }

    private static float[] endNormal(float[] sr, float[] sy, int i, int j) {
        if (j < 0 || j >= sr.length || sr[i] * sr[j] + sy[i] * sy[j] < CREASE) {
            return new float[] {sr[i], sy[i]};
        }
        float r = sr[i] + sr[j], y = sy[i] + sy[j];
        float l = Mth.sqrt(r * r + y * y);
        return new float[] {r / l, y / l};
    }

    // ---------------------------------------------------------------- the sweep

    /** A loop of {x, y} pairs round a circle, {@code n} points, counter-clockwise. */
    public static float[] circle(int n) {
        float[] s = new float[2 * n];
        for (int k = 0; k < n; k++) {
            float a = k * Mth.TWO_PI / n;
            s[2 * k] = Mth.cos(a);
            s[2 * k + 1] = Mth.sin(a);
        }
        return s;
    }

    /** A blade's section: a flat diamond, its edges at x = +-1 and its spine at y = +-1. */
    public static final float[] DIAMOND = {1, 0, 0, 1, -1, 0, 0, -1};
    /** A flat bar's section (a strap, a plank seen end on). */
    public static final float[] RECT = {1, -1, 1, 1, -1, 1, -1, -1};
    /** A single-edged blade's section: keen at x = 1, a flat back at x = -1. */
    public static final float[] WEDGE = {1, 0, -.6f, .9f, -1, .7f, -1, -.7f, -.6f, -.9f};

    /**
     * The section (a counter-clockwise loop of {x, y} pairs) swept along {@code path}: at each point laid square to the
     * path, x along a side carried down the path without twisting (first as near {@code hint} as it can be), y across both,
     * scaled by {@code sx[i]}, {@code sy[i]}. A sharp section keeps every corner's edge (blades); a smooth one is shaded
     * round (tentacles, cords). U goes round the section, V along the path by length ({@code vScale} repeats a block).
     * The ends are closed if {@code caps}.
     */
    public static void sweep(VertexConsumer c, PoseStack.Pose p, Vector3f[] path, float[] sx, float[] sy, float[] sec, boolean sharp, Vector3f hint, float vScale,
                             int[] argb, boolean caps, int light) {
        int n = path.length, m = sec.length / 2;
        if (n < 2 || m < 3) {
            return;
        }
        Vector3f[] xs = new Vector3f[n], ys = new Vector3f[n], ts = new Vector3f[n];
        float[] v = new float[n];
        Vector3f side = null;
        for (int i = 0; i < n; i++) {
            Vector3f t = new Vector3f(path[Math.min(n - 1, i + 1)]).sub(path[Math.max(0, i - 1)]);
            if (t.lengthSquared() < 1.0E-12f) {
                t = i > 0 ? new Vector3f(ts[i - 1]) : new Vector3f(0, 1, 0);
            }
            t.normalize();
            if (side == null) {
                side = hint != null ? new Vector3f(hint) : BossMesh.across(t);
            }
            side = new Vector3f(side).sub(new Vector3f(t).mul(side.dot(t)));
            if (side.lengthSquared() < 1.0E-8f) {
                side = BossMesh.across(t);
            }
            side.normalize();
            ts[i] = t;
            xs[i] = new Vector3f(side);
            ys[i] = new Vector3f(t).cross(side).normalize();
            v[i] = i == 0 ? 0 : v[i - 1] + path[i].distance(path[i - 1]) * vScale;
        }
        // The section's edge normals (out of each edge) and its corners' (smoothed)
        float[] ex = new float[m], ey = new float[m];
        for (int k = 0; k < m; k++) {
            int k1 = (k + 1) % m;
            float dx = sec[2 * k1] - sec[2 * k], dy = sec[2 * k1 + 1] - sec[2 * k + 1];
            float l = Mth.sqrt(dx * dx + dy * dy);
            ex[k] = l < 1.0E-6f ? 0 : dy / l;
            ey[k] = l < 1.0E-6f ? 1 : -dx / l;
        }
        for (int i = 0; i + 1 < n; i++) {
            for (int k = 0; k < m; k++) {
                int k1 = (k + 1) % m;
                float u0 = (float) k / m, u1 = (float) (k + 1) / m;
                float[] na = sharp ? new float[] {ex[k], ey[k]} : corner(ex, ey, k, m), nb = sharp ? new float[] {ex[k], ey[k]} : corner(ex, ey, k1, m);
                Vector3f a0 = at(path, xs, ys, sx, sy, sec, i, k), b0 = at(path, xs, ys, sx, sy, sec, i, k1);
                Vector3f a1 = at(path, xs, ys, sx, sy, sec, i + 1, k), b1 = at(path, xs, ys, sx, sy, sec, i + 1, k1);
                put(c, p, a0, u0, v[i], argb[i], light, dir(xs, ys, sx, sy, i, na));
                put(c, p, b0, u1, v[i], argb[i], light, dir(xs, ys, sx, sy, i, nb));
                put(c, p, b1, u1, v[i + 1], argb[i + 1], light, dir(xs, ys, sx, sy, i + 1, nb));
                put(c, p, a1, u0, v[i + 1], argb[i + 1], light, dir(xs, ys, sx, sy, i + 1, na));
            }
        }
        if (caps) {
            cap(c, p, path, xs, ys, sx, sy, sec, 0, new Vector3f(ts[0]).negate(), argb[0], light, true);
            cap(c, p, path, xs, ys, sx, sy, sec, n - 1, ts[n - 1], argb[n - 1], light, false);
        }
    }

    private static float[] corner(float[] ex, float[] ey, int k, int m) {
        int j = (k + m - 1) % m;
        float x = ex[k] + ex[j], y = ey[k] + ey[j];
        float l = Mth.sqrt(x * x + y * y);
        return l < 1.0E-6f ? new float[] {ex[k], ey[k]} : new float[] {x / l, y / l};
    }

    private static Vector3f at(Vector3f[] path, Vector3f[] xs, Vector3f[] ys, float[] sx, float[] sy, float[] sec, int i, int k) {
        return new Vector3f(xs[i]).mul(sec[2 * k] * sx[i]).add(new Vector3f(ys[i]).mul(sec[2 * k + 1] * sy[i])).add(path[i]);
    }

    private static Vector3f dir(Vector3f[] xs, Vector3f[] ys, float[] sx, float[] sy, int i, float[] nrm) {
        // A section squashed one way has its normals pushed the other
        float x = nrm[0] / Math.max(sx[i], 1.0E-4f), y = nrm[1] / Math.max(sy[i], 1.0E-4f);
        Vector3f d = new Vector3f(xs[i]).mul(x).add(new Vector3f(ys[i]).mul(y));
        return d.lengthSquared() < 1.0E-12f ? new Vector3f(xs[i]) : d.normalize();
    }

    private static void cap(VertexConsumer c, PoseStack.Pose p, Vector3f[] path, Vector3f[] xs, Vector3f[] ys, float[] sx, float[] sy, float[] sec, int i,
                            Vector3f n, int argb, int light, boolean reverse) {
        int m = sec.length / 2;
        if (sx[i] < 1.0E-4f && sy[i] < 1.0E-4f) {
            return;
        }
        for (int k = 1; k + 1 < m; k++) {
            int a = 0, b = reverse ? k + 1 : k, d = reverse ? k : k + 1;
            Vector3f pa = at(path, xs, ys, sx, sy, sec, i, a), pb = at(path, xs, ys, sx, sy, sec, i, b), pd = at(path, xs, ys, sx, sy, sec, i, d);
            put(c, p, pa, sec[2 * a] * .5f + .5f, sec[2 * a + 1] * .5f + .5f, argb, light, n);
            put(c, p, pb, sec[2 * b] * .5f + .5f, sec[2 * b + 1] * .5f + .5f, argb, light, n);
            put(c, p, pd, sec[2 * d] * .5f + .5f, sec[2 * d + 1] * .5f + .5f, argb, light, n);
            put(c, p, pd, sec[2 * d] * .5f + .5f, sec[2 * d + 1] * .5f + .5f, argb, light, n);
        }
    }

    /** The same colour {@code n} times. */
    public static int[] fill(int argb, int n) {
        int[] a = new int[n];
        java.util.Arrays.fill(a, argb);
        return a;
    }

    /** The same size {@code n} times. */
    public static float[] fill(float v, int n) {
        float[] a = new float[n];
        java.util.Arrays.fill(a, v);
        return a;
    }

    /** {@code n} colours from {@code from} to {@code to}. */
    public static int[] fade(int from, int to, int n) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = BossMesh.blend(from, to, n == 1 ? 0 : (float) i / (n - 1));
        }
        return a;
    }

    /** {@code n} points along a quadratic curve from {@code a} through (near) {@code mid} to {@code b}. */
    public static Vector3f[] curve(Vector3f a, Vector3f mid, Vector3f b, int n) {
        Vector3f[] pts = new Vector3f[n];
        for (int i = 0; i < n; i++) {
            float t = (float) i / (n - 1), s = 1 - t;
            pts[i] = new Vector3f(a).mul(s * s).add(new Vector3f(mid).mul(2 * s * t)).add(new Vector3f(b).mul(t * t));
        }
        return pts;
    }

    /** {@code n} points along a cubic curve from {@code a} to {@code b}, leaving {@code a} toward {@code c1} and coming to {@code b} from {@code c2}. */
    public static Vector3f[] bezier(Vector3f a, Vector3f c1, Vector3f c2, Vector3f b, int n) {
        Vector3f[] pts = new Vector3f[n];
        for (int i = 0; i < n; i++) {
            float t = (float) i / (n - 1), s = 1 - t;
            pts[i] = new Vector3f(a).mul(s * s * s).add(new Vector3f(c1).mul(3 * s * s * t)).add(new Vector3f(c2).mul(3 * s * t * t)).add(new Vector3f(b).mul(t * t * t));
        }
        return pts;
    }

    /** {@code n} values from {@code a} to {@code b}, eased by the power {@code pow} (1 straight). */
    public static float[] taper(float a, float b, int n, float pow) {
        float[] r = new float[n];
        for (int i = 0; i < n; i++) {
            float t = n == 1 ? 0 : (float) i / (n - 1);
            r[i] = Mth.lerp((float) Math.pow(t, pow), a, b);
        }
        return r;
    }

    // ---------------------------------------------------------------- the plank

    /**
     * A shape cut from a plank: {@code outline} (a counter-clockwise loop of {x, y} pairs, need not be convex) in the XY
     * plane, {@code depth} thick along Z round 0, its edges bevelled {@code bevel} deep (0 for none). Its faces show the
     * texture flat ({@code uvScale} repeats a block, from the origin); its rim the colour {@code rim}.
     */
    public static void slab(VertexConsumer c, PoseStack.Pose p, float[] outline, float depth, float bevel, float uvScale, int argb, int rim, int light) {
        int m = outline.length / 2;
        if (m < 3) {
            return;
        }
        float h = depth * .5f;
        float b = Math.min(bevel, h * .9f);
        float[] inner = b > 0 ? inset(outline, b) : outline;
        int[] tris = triangulate(inner);
        Vector3f front = new Vector3f(0, 0, 1), back = new Vector3f(0, 0, -1);
        for (int t = 0; t + 2 < tris.length; t += 3) {
            int i = tris[t], j = tris[t + 1], k = tris[t + 2];
            face(c, p, inner, i, j, k, h, uvScale, argb, light, front, false);
            face(c, p, inner, i, k, j, -h, uvScale, argb, light, back, true);
        }
        float hb = h - b;
        for (int k = 0; k < m; k++) {
            int k1 = (k + 1) % m;
            float dx = outline[2 * k1] - outline[2 * k], dy = outline[2 * k1 + 1] - outline[2 * k + 1];
            float l = Mth.sqrt(dx * dx + dy * dy);
            if (l < 1.0E-6f) {
                continue;
            }
            Vector3f out = new Vector3f(dy / l, -dx / l, 0);
            Vector3f o0 = new Vector3f(outline[2 * k], outline[2 * k + 1], 0), o1 = new Vector3f(outline[2 * k1], outline[2 * k1 + 1], 0);
            float u0 = 0, u1 = l * uvScale;
            // The rim
            Vector3f a = new Vector3f(o0).add(0, 0, hb), bb = new Vector3f(o0).add(0, 0, -hb), d = new Vector3f(o1).add(0, 0, -hb), e = new Vector3f(o1).add(0, 0, hb);
            put(c, p, a, u0, 0, rim, light, out);
            put(c, p, bb, u0, depth * uvScale, rim, light, out);
            put(c, p, d, u1, depth * uvScale, rim, light, out);
            put(c, p, e, u1, 0, rim, light, out);
            if (b > 0) {
                Vector3f i0 = new Vector3f(inner[2 * k], inner[2 * k + 1], 0), i1 = new Vector3f(inner[2 * k1], inner[2 * k1 + 1], 0);
                Vector3f nf = new Vector3f(out).add(0, 0, 1).normalize(), nk = new Vector3f(out).add(0, 0, -1).normalize();
                // The bevel to the front face, then to the back
                put(c, p, new Vector3f(i0).add(0, 0, h), u0, 0, argb, light, nf);
                put(c, p, new Vector3f(o0).add(0, 0, hb), u0, b * uvScale, argb, light, nf);
                put(c, p, new Vector3f(o1).add(0, 0, hb), u1, b * uvScale, argb, light, nf);
                put(c, p, new Vector3f(i1).add(0, 0, h), u1, 0, argb, light, nf);
                put(c, p, new Vector3f(o0).add(0, 0, -hb), u0, b * uvScale, argb, light, nk);
                put(c, p, new Vector3f(i0).add(0, 0, -h), u0, 0, argb, light, nk);
                put(c, p, new Vector3f(i1).add(0, 0, -h), u1, 0, argb, light, nk);
                put(c, p, new Vector3f(o1).add(0, 0, -hb), u1, b * uvScale, argb, light, nk);
            }
        }
    }

    private static void face(VertexConsumer c, PoseStack.Pose p, float[] poly, int i, int j, int k, float z, float uv, int argb, int light, Vector3f n,
                             boolean mirror) {
        int[] idx = {i, j, k, k};
        for (int q : idx) {
            float x = poly[2 * q], y = poly[2 * q + 1];
            put(c, p, new Vector3f(x, y, z), (mirror ? -x : x) * uv, -y * uv, argb, light, n);
        }
    }

    /** The outline moved in by {@code d} along its edges' normals (corners mitred, not too far). */
    public static float[] inset(float[] poly, float d) {
        int m = poly.length / 2;
        float[] out = new float[poly.length];
        for (int k = 0; k < m; k++) {
            int j = (k + m - 1) % m, k1 = (k + 1) % m;
            float[] n0 = edgeIn(poly, j, k), n1 = edgeIn(poly, k, k1);
            float bx = n0[0] + n1[0], by = n0[1] + n1[1];
            float bl = Mth.sqrt(bx * bx + by * by);
            if (bl < 1.0E-5f) {
                bx = n1[0];
                by = n1[1];
                bl = 1;
            }
            bx /= bl;
            by /= bl;
            float cos = bx * n1[0] + by * n1[1];
            float reach = d / Math.max(cos, .35f);
            out[2 * k] = poly[2 * k] + bx * reach;
            out[2 * k + 1] = poly[2 * k + 1] + by * reach;
        }
        return out;
    }

    private static float[] edgeIn(float[] poly, int a, int b) {
        float dx = poly[2 * b] - poly[2 * a], dy = poly[2 * b + 1] - poly[2 * a + 1];
        float l = Mth.sqrt(dx * dx + dy * dy);
        return l < 1.0E-6f ? new float[] {0, 0} : new float[] {-dy / l, dx / l};
    }

    /** The outline (a counter-clockwise loop) cut into triangles by clipping ears; a list of corner indices, three by three. */
    public static int[] triangulate(float[] poly) {
        int m = poly.length / 2;
        List<Integer> left = new ArrayList<>();
        for (int k = 0; k < m; k++) {
            left.add(k);
        }
        List<Integer> tris = new ArrayList<>();
        int guard = 0;
        while (left.size() > 3 && guard++ < m * m) {
            boolean cut = false;
            for (int q = 0; q < left.size(); q++) {
                int a = left.get((q + left.size() - 1) % left.size()), b = left.get(q), d = left.get((q + 1) % left.size());
                if (cross(poly, a, b, d) <= 1.0E-7f) {
                    continue;
                }
                boolean empty = true;
                for (int r : left) {
                    if (r != a && r != b && r != d && inside(poly, r, a, b, d)) {
                        empty = false;
                        break;
                    }
                }
                if (empty) {
                    tris.add(a);
                    tris.add(b);
                    tris.add(d);
                    left.remove(q);
                    cut = true;
                    break;
                }
            }
            if (!cut) {
                break;
            }
        }
        if (left.size() >= 3) {
            // What is left (if the outline was not quite simple), as a fan
            for (int q = 1; q + 1 < left.size(); q++) {
                tris.add(left.get(0));
                tris.add(left.get(q));
                tris.add(left.get(q + 1));
            }
        }
        int[] out = new int[tris.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = tris.get(i);
        }
        return out;
    }

    private static float cross(float[] poly, int a, int b, int d) {
        return (poly[2 * b] - poly[2 * a]) * (poly[2 * d + 1] - poly[2 * a + 1]) - (poly[2 * b + 1] - poly[2 * a + 1]) * (poly[2 * d] - poly[2 * a]);
    }

    private static boolean inside(float[] poly, int r, int a, int b, int d) {
        float px = poly[2 * r], py = poly[2 * r + 1];
        return side(px, py, poly, a, b) >= 0 && side(px, py, poly, b, d) >= 0 && side(px, py, poly, d, a) >= 0;
    }

    private static float side(float px, float py, float[] poly, int a, int b) {
        return (poly[2 * b] - poly[2 * a]) * (py - poly[2 * a + 1]) - (poly[2 * b + 1] - poly[2 * a + 1]) * (px - poly[2 * a]);
    }

    /** A rectangle {@code w} by {@code h} round the origin, its corners cut {@code cut} deep: an outline for {@link #slab}. */
    public static float[] rect(float w, float h, float cut) {
        float x = w * .5f, y = h * .5f;
        if (cut <= 0) {
            return new float[] {-x, -y, x, -y, x, y, -x, y};
        }
        return new float[] {-x + cut, -y, x - cut, -y, x, -y + cut, x, y - cut, x - cut, y, -x + cut, y, -x, y - cut, -x, -y + cut};
    }

    /** A plate {@code w} by {@code h} by {@code d} round the origin, every edge bevelled {@code bevel}: an armour plate, a brick, a tile. */
    public static void block(VertexConsumer c, PoseStack.Pose p, float w, float h, float d, float bevel, float uvScale, int argb, int light) {
        slab(c, p, rect(w, h, bevel), d, bevel, uvScale, argb, argb, light);
    }

    // ---------------------------------------------------------------- rings, gems, discs

    /**
     * A ring round the Y axis, {@code big} across to the middle of its tube, the tube {@code small} across; over the
     * angles {@code a0} to {@code a1}. U along the ring ({@code wrap} repeats), V round its tube.
     */
    public static void torus(VertexConsumer c, PoseStack.Pose p, float big, float small, int segs, int sides, float a0, float a1, float wrap, int argb,
                             int light) {
        for (int s = 0; s < segs; s++) {
            float t0 = Mth.lerp((float) s / segs, a0, a1), t1 = Mth.lerp((float) (s + 1) / segs, a0, a1);
            for (int k = 0; k < sides; k++) {
                float b0 = k * Mth.TWO_PI / sides, b1 = (k + 1) * Mth.TWO_PI / sides;
                float u0 = (float) s / segs * wrap, u1 = (float) (s + 1) / segs * wrap, v0 = (float) k / sides, v1 = (float) (k + 1) / sides;
                torusPut(c, p, big, small, t0, b0, u0, v0, argb, light);
                torusPut(c, p, big, small, t0, b1, u0, v1, argb, light);
                torusPut(c, p, big, small, t1, b1, u1, v1, argb, light);
                torusPut(c, p, big, small, t1, b0, u1, v0, argb, light);
            }
        }
    }

    private static void torusPut(VertexConsumer c, PoseStack.Pose p, float big, float small, float t, float b, float u, float v, int argb, int light) {
        float ct = Mth.cos(t), st = Mth.sin(t), cb = Mth.cos(b), sb = Mth.sin(b);
        Vector3f n = new Vector3f(ct * cb, sb, st * cb);
        put(c, p, new Vector3f(ct * (big + small * cb), small * sb, st * (big + small * cb)), u, v, argb, light, n);
    }

    /**
     * A cut gem, its girdle {@code r} across round the Y axis: a flat table on top of a crown {@code crown} high, a
     * pavilion {@code pavilion} deep down to its point; {@code facets} round. Each facet catches the light on its own.
     */
    public static void gem(VertexConsumer c, PoseStack.Pose p, float r, float crown, float pavilion, int facets, int argb, int light) {
        float tr = r * .58f;
        for (int k = 0; k < facets; k++) {
            float a0 = k * Mth.TWO_PI / facets, a1 = (k + 1) * Mth.TWO_PI / facets, am = (a0 + a1) * .5f;
            Vector3f g0 = new Vector3f(Mth.cos(a0) * r, 0, Mth.sin(a0) * r), g1 = new Vector3f(Mth.cos(a1) * r, 0, Mth.sin(a1) * r);
            Vector3f t0 = new Vector3f(Mth.cos(a0) * tr, crown, Mth.sin(a0) * tr), t1 = new Vector3f(Mth.cos(a1) * tr, crown, Mth.sin(a1) * tr);
            Vector3f top = new Vector3f(0, crown, 0), point = new Vector3f(0, -pavilion, 0);
            Vector3f mid = new Vector3f(Mth.cos(am) * r * .82f, crown * .45f, Mth.sin(am) * r * .82f);
            // The crown in two facets, the table, the pavilion
            tri(c, p, g0, mid, g1, 0, 1, .5f, .5f, 1, 1, argb, light);
            tri(c, p, g0, t0, mid, 0, 1, 0, 0, .5f, .5f, argb, light);
            tri(c, p, mid, t1, g1, .5f, .5f, 1, 0, 1, 1, argb, light);
            tri(c, p, mid, t0, t1, .5f, .5f, 0, 0, 1, 0, argb, light);
            tri(c, p, top, t1, t0, .5f, .5f, 1, 0, 0, 0, argb, light);
            tri(c, p, g0, g1, point, 0, 0, 1, 0, .5f, 1, argb, light);
        }
    }

    /**
     * A flat ring lying in the XZ plane facing up, from {@code r0} to {@code r1}: the texture laid flat over the disc of
     * radius {@code r1} (its middle at the texture's middle), turned {@code turn}; seen from both sides if {@code both}.
     */
    public static void disc(VertexConsumer c, PoseStack.Pose p, float r0, float r1, int segs, float turn, int argb, int light, boolean both) {
        Vector3f up = new Vector3f(0, 1, 0), down = new Vector3f(0, -1, 0);
        for (int s = 0; s < segs; s++) {
            float a0 = s * Mth.TWO_PI / segs, a1 = (s + 1) * Mth.TWO_PI / segs;
            Vector3f i0 = new Vector3f(Mth.cos(a0) * r0, 0, Mth.sin(a0) * r0), i1 = new Vector3f(Mth.cos(a1) * r0, 0, Mth.sin(a1) * r0);
            Vector3f o0 = new Vector3f(Mth.cos(a0) * r1, 0, Mth.sin(a0) * r1), o1 = new Vector3f(Mth.cos(a1) * r1, 0, Mth.sin(a1) * r1);
            float[] ui0 = flat(i0, r1, turn), ui1 = flat(i1, r1, turn), uo0 = flat(o0, r1, turn), uo1 = flat(o1, r1, turn);
            // Wound to face up (round from +X toward +Z, which seen from above is counter-clockwise)
            put(c, p, i0, ui0[0], ui0[1], argb, light, up);
            put(c, p, i1, ui1[0], ui1[1], argb, light, up);
            put(c, p, o1, uo1[0], uo1[1], argb, light, up);
            put(c, p, o0, uo0[0], uo0[1], argb, light, up);
            if (both) {
                put(c, p, o0, uo0[0], uo0[1], argb, light, down);
                put(c, p, o1, uo1[0], uo1[1], argb, light, down);
                put(c, p, i1, ui1[0], ui1[1], argb, light, down);
                put(c, p, i0, ui0[0], ui0[1], argb, light, down);
            }
        }
    }

    private static float[] flat(Vector3f v, float r, float turn) {
        float cs = Mth.cos(turn), sn = Mth.sin(turn);
        float x = v.x * cs - v.z * sn, z = v.x * sn + v.z * cs;
        return new float[] {x / (2 * r) + .5f, z / (2 * r) + .5f};
    }
}
