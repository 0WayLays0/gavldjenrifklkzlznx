package com.waylays.arcanes.client.render.gem;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Cut gems for Gem Magic's newer models (and the shapes {@code GemRender} draws too): real faceted solids drawn in
 * see-through glass, each facet shaded by the way it faces and flashing white in its turn as the gem turns, a glow at
 * its heart and sparkles twinkling on its corners. Everything is drawn in the pose's own space, turned by a quaternion
 * and placed by hand, so the eye of the view stays true.
 * <ul>
 * <li>BRILLIANT: a table, a crown of kite and star facets, a girdle and a pointed pavilion (ruby, diamond).</li>
 * <li>STEP: the emerald cut, an octagon stepped down in rings to a keel (emerald).</li>
 * <li>CRYSTAL: a six-sided point, as crystals grow (amethyst, shards).</li>
 * </ul>
 */
public final class GemMesh {
    public static final ResourceLocation FACET = ArcanesOublies.id("textures/entity/gem/facet.png");
    public static final ResourceLocation SPARKLE = ArcanesOublies.id("textures/entity/gem/sparkle.png");

    public enum Cut { BRILLIANT, STEP, CRYSTAL }

    /** The five gems' colours and the bright tone of their light (as {@code Gem}). */
    public static final int RUBY = 0xE01848, RUBY_LIGHT = 0xFF8A9E;
    public static final int LAPIS = 0x2450C8, LAPIS_GOLD = 0xF2C33C;
    public static final int DIAMOND = 0x9FEAFF;
    public static final int AMETHYST = 0xA35BF0, AMETHYST_LIGHT = 0xE2C6FF;
    public static final int EMERALD = 0x1FCB6E, EMERALD_LIGHT = 0xB6FFD2;

    private static final float[][] MESH = new float[Cut.values().length][];
    private static final float[][] SPARKS = new float[Cut.values().length][];
    private static final Vector3f LIGHT = new Vector3f(.35f, 1, .55f).normalize();

    static {
        build(Cut.BRILLIANT, brilliant());
        build(Cut.STEP, step());
        build(Cut.CRYSTAL, crystal());
    }

    private GemMesh() {}

    /** A cut's triangles: 9 floats each, about one block across and high, centred on the origin. */
    public static float[] triangles(Cut cut) {
        return MESH[cut.ordinal()];
    }

    /** The corners of a cut that twinkle: 3 floats each. */
    public static float[] corners(Cut cut) {
        return SPARKS[cut.ordinal()];
    }

    private record Mesh(List<float[]> tris, List<float[]> sparks) {}

    private static void build(Cut cut, Mesh mesh) {
        float[] tris = new float[mesh.tris().size() * 9];
        for (int i = 0; i < mesh.tris().size(); i++) {
            System.arraycopy(mesh.tris().get(i), 0, tris, i * 9, 9);
        }
        float[] sparks = new float[mesh.sparks().size() * 3];
        for (int i = 0; i < mesh.sparks().size(); i++) {
            System.arraycopy(mesh.sparks().get(i), 0, sparks, i * 3, 3);
        }
        MESH[cut.ordinal()] = tris;
        SPARKS[cut.ordinal()] = sparks;
    }

    private static float[] ring(int n, float y, float r, float offset) {
        float[] out = new float[n * 3];
        for (int i = 0; i < n; i++) {
            float a = (i + offset) * Mth.TWO_PI / n;
            out[i * 3] = Mth.cos(a) * r;
            out[i * 3 + 1] = y;
            out[i * 3 + 2] = Mth.sin(a) * r;
        }
        return out;
    }

    private static float[] at(float[] ring, int i) {
        int n = ring.length / 3;
        int k = Math.floorMod(i, n) * 3;
        return new float[] {ring[k], ring[k + 1], ring[k + 2]};
    }

    private static void tri(List<float[]> out, float[] a, float[] b, float[] c) {
        out.add(new float[] {a[0], a[1], a[2], b[0], b[1], b[2], c[0], c[1], c[2]});
    }

    private static void band(List<float[]> out, float[] lower, float[] upper) {
        int n = lower.length / 3;
        for (int i = 0; i < n; i++) {
            tri(out, at(lower, i), at(lower, i + 1), at(upper, i + 1));
            tri(out, at(lower, i), at(upper, i + 1), at(upper, i));
        }
    }

    private static void fan(List<float[]> out, float[] ring, float[] centre) {
        int n = ring.length / 3;
        for (int i = 0; i < n; i++) {
            tri(out, at(ring, i), at(ring, i + 1), centre);
        }
    }

    private static void corners(List<float[]> sparks, float[] ring, int step) {
        for (int i = 0; i < ring.length / 3; i += step) {
            sparks.add(at(ring, i));
        }
    }

    private static Mesh brilliant() {
        List<float[]> t = new ArrayList<>();
        List<float[]> s = new ArrayList<>();
        float[] table = ring(8, .3f, .27f, 0);
        float[] girdle = ring(16, .1f, .5f, 0);
        float[] girdleLow = ring(16, .05f, .5f, 0);
        float[] pavilion = ring(8, -.2f, .27f, .5f);
        fan(t, table, new float[] {0, .3f, 0});
        for (int i = 0; i < 8; i++) {
            float[] ti = at(table, i), tn = at(table, i + 1);
            tri(t, ti, at(girdle, 2 * i), at(girdle, 2 * i + 1));
            tri(t, ti, at(girdle, 2 * i + 1), tn);
            tri(t, tn, at(girdle, 2 * i + 1), at(girdle, 2 * i + 2));
        }
        band(t, girdleLow, girdle);
        for (int i = 0; i < 8; i++) {
            float[] p = at(pavilion, i);
            tri(t, at(girdleLow, 2 * i + 2), at(girdleLow, 2 * i + 1), p);
            tri(t, at(girdleLow, 2 * i + 1), at(girdleLow, 2 * i), at(pavilion, i - 1));
            tri(t, at(girdleLow, 2 * i + 1), at(pavilion, i - 1), p);
        }
        fan(t, pavilion, new float[] {0, -.55f, 0});
        corners(s, table, 1);
        corners(s, girdle, 2);
        s.add(new float[] {0, -.55f, 0});
        return new Mesh(t, s);
    }

    private static float[] octagon(float y, float sx, float sz) {
        float a = .5f, b = .36f, c = .12f;
        float[][] p = {{a - c, b}, {a, b - c}, {a, -(b - c)}, {a - c, -b}, {-(a - c), -b}, {-a, -(b - c)}, {-a, b - c}, {-(a - c), b}};
        float[] out = new float[24];
        for (int i = 0; i < 8; i++) {
            out[i * 3] = p[i][0] * sx;
            out[i * 3 + 1] = y;
            out[i * 3 + 2] = p[i][1] * sz;
        }
        return out;
    }

    private static Mesh step() {
        List<float[]> t = new ArrayList<>();
        List<float[]> s = new ArrayList<>();
        float[][] rings = {octagon(-.42f, .5f, .06f), octagon(-.3f, .45f, .4f), octagon(-.12f, .78f, .74f), octagon(.04f, 1, 1), octagon(.1f, 1, 1),
                octagon(.2f, .84f, .82f), octagon(.28f, .64f, .6f)};
        for (int i = 0; i + 1 < rings.length; i++) {
            band(t, rings[i], rings[i + 1]);
        }
        fan(t, rings[rings.length - 1], new float[] {0, .28f, 0});
        fan(t, rings[0], new float[] {0, -.42f, 0});
        corners(s, rings[4], 1);
        corners(s, rings[6], 2);
        return new Mesh(t, s);
    }

    private static Mesh crystal() {
        List<float[]> t = new ArrayList<>();
        List<float[]> s = new ArrayList<>();
        float[] base = ring(6, -.5f, .2f, 0);
        float[] top = ring(6, .2f, .24f, 0);
        float[] tip = {0, .55f, 0};
        band(t, base, top);
        for (int i = 0; i < 6; i++) {
            tri(t, at(top, i), at(top, i + 1), tip);
        }
        fan(t, base, new float[] {0, -.5f, 0});
        s.add(tip);
        corners(s, top, 2);
        return new Mesh(t, s);
    }

    // ---------------------------------------------------------------- drawing

    /**
     * A gem of {@code cut} at {@code at}, turned by {@code q}, {@code size} across ({@code stretch} times as tall along its
     * own Y), in {@code rgb}, {@code alpha} opaque (about .7 reads as a gem); {@code time} (ticks) makes its facets flash in
     * turn. Drawn on {@code c}, which should be see-through (entity translucent) on the facet texture.
     */
    public static void gem(VertexConsumer c, PoseStack.Pose p, Cut cut, Vector3f at, Quaternionf q, float size, float stretch, int rgb, float alpha, float time,
                           int light) {
        float[] tris = MESH[cut.ordinal()];
        int a = (int) (Mth.clamp(alpha, 0, 1) * 255);
        int r0 = (rgb >> 16) & 0xFF, g0 = (rgb >> 8) & 0xFF, b0 = rgb & 0xFF;
        Vector3f[] v = new Vector3f[3];
        for (int i = 0; i < tris.length; i += 9) {
            for (int k = 0; k < 3; k++) {
                v[k] = q.transform(new Vector3f(tris[i + 3 * k] * size, tris[i + 3 * k + 1] * size * stretch, tris[i + 3 * k + 2] * size)).add(at);
            }
            Vector3f n = new Vector3f(v[1]).sub(v[0]).cross(new Vector3f(v[2]).sub(v[0]));
            if (n.lengthSquared() < 1.0E-12f) {
                continue;
            }
            n.normalize();
            // Outward
            Vector3f mid = new Vector3f(v[0]).add(v[1]).add(v[2]).div(3).sub(at);
            if (n.dot(mid) < 0) {
                n.negate();
            }
            int facet = i / 9;
            float shade = .5f + .5f * Math.abs(n.dot(LIGHT));
            float phase = (time * .07f + BossMesh.hash(facet * 31 + cut.ordinal()) * 7) % 7;
            float flash = phase < 1 ? Mth.sin(phase * Mth.PI) : 0;
            int r = (int) Mth.lerp(flash * .75f, r0 * shade, 255);
            int g = (int) Mth.lerp(flash * .75f, g0 * shade, 255);
            int b = (int) Mth.lerp(flash * .75f, b0 * shade, 255);
            int argb = a << 24 | Mth.clamp(r, 0, 255) << 16 | Mth.clamp(g, 0, 255) << 8 | Mth.clamp(b, 0, 255);
            WaterMesh.put(c, p, v[0], 0, 0, argb, light, n);
            WaterMesh.put(c, p, v[1], 1, 0, argb, light, n);
            WaterMesh.put(c, p, v[2], .5f, 1, argb, light, n);
            WaterMesh.put(c, p, v[2], .5f, 1, argb, light, n);
        }
    }

    /** Sparkles twinkling on the corners of a gem drawn by {@link #gem} with the same placing ({@code rainbow}: a diamond's fire). */
    public static void sparkles(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Cut cut, Vector3f at, Quaternionf q, float size, float stretch,
                                int rgb, float time, boolean rainbow, int seed, int light) {
        float[] sparks = SPARKS[cut.ordinal()];
        VertexConsumer c = BossMesh.light(buffer, SPARKLE);
        for (int i = 0; i < sparks.length; i += 3) {
            float h = BossMesh.hash(seed + i * 7);
            float tw = Mth.sin(time * (.18f + .1f * h) + h * 40);
            if (tw <= .5f) {
                continue;
            }
            float strength = (tw - .5f) / .5f;
            int color = rainbow ? Mth.hsvToRgb((h * 3 + time * .01f) % 1, .55f, 1) : BossMesh.mix(rgb, 0xFFFFFF, .5f);
            Vector3f s = q.transform(new Vector3f(sparks[i] * size, sparks[i + 1] * size * stretch, sparks[i + 2] * size)).add(at);
            RitualMesh.billboard(c, poseStack, view, s, size * (.18f + .2f * strength), time * .05f + h * 3, -1, BossMesh.glow(color, strength), light);
        }
    }

    /** A whole jewel: its facets, the glow at its heart ({@code glow} 0 to 1) and its sparkles. */
    public static void jewel(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Cut cut, Vector3f at, Quaternionf q, float size, float stretch,
                             int rgb, float alpha, float time, float glow, boolean rainbow, int seed, int light) {
        gem(BossMesh.see(buffer, FACET), poseStack.last(), cut, at, q, size, stretch, rgb, alpha, time, light);
        if (glow > .01f) {
            VertexConsumer g = BossMesh.light(buffer, BossMesh.GLOW);
            RitualMesh.billboard(g, poseStack, view, at, size * 1.6f * Math.max(1, stretch * .6f), 0, -1, BossMesh.glow(rgb, glow * .6f), light);
            RitualMesh.billboard(g, poseStack, view, at, size * .6f, 0, -1, BossMesh.glow(BossMesh.mix(rgb, 0xFFFFFF, .6f), glow), light);
        }
        sparkles(poseStack, buffer, view, cut, at, q, size, stretch, rgb, time, rainbow, seed, light);
    }

    /** A ray of light from {@code a} to {@code b}: a wide glow of {@code rgb}, a brighter band and a white core, {@code width} across. */
    public static void ray(VertexConsumer beam, PoseStack.Pose p, Vector3f eye, Vector3f a, Vector3f b, int rgb, float width, float strength, float time) {
        float flicker = 1 + .12f * Mth.sin(time * 1.7f) + .06f * Mth.sin(time * 4.3f);
        RitualMesh.shaft(beam, p, eye, a, b, width * 1.9f * flicker, BossMesh.glow(rgb, strength * .35f), 0xF000F0);
        RitualMesh.shaft(beam, p, eye, a, b, width * flicker, BossMesh.glow(BossMesh.mix(rgb, 0xFFFFFF, .25f), strength * .8f), 0xF000F0);
        RitualMesh.shaft(beam, p, eye, a, b, width * .35f, BossMesh.glow(BossMesh.mix(rgb, 0xFFFFFF, .85f), strength), 0xF000F0);
    }

    /** A five-pointed star's outline (counter-clockwise), {@code r} to its points, {@code inner} of that to its notches, a point up (+Y). */
    public static float[] star(float r, float inner) {
        float[] s = new float[20];
        for (int k = 0; k < 10; k++) {
            float a = Mth.HALF_PI + k * Mth.PI / 5;
            float rr = k % 2 == 0 ? r : r * inner;
            s[2 * k] = Mth.cos(a) * rr;
            s[2 * k + 1] = Mth.sin(a) * rr;
        }
        return s;
    }
}
