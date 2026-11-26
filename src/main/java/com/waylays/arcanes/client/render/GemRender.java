package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Cut gems for Gem Magic's renderers: real faceted solids drawn in translucent glass (the facets behind show through
 * the ones in front), each facet shaded by the way it faces and now and then flashing white as the gem turns, a glow
 * at its heart, and sparkles twinkling on its corners.
 * <ul>
 * <li>BRILLIANT: a table, a crown of kite and star facets, a girdle and a pointed pavilion (ruby, diamond).</li>
 * <li>STEP: the emerald cut, an octagon stepped down in rings to a keel (emerald).</li>
 * <li>CRYSTAL: a six-sided point, as crystals grow (amethyst, shards).</li>
 * </ul>
 */
public final class GemRender {
    public static final ResourceLocation FACET = ArcanesOublies.id("textures/entity/gem/facet.png");
    public static final ResourceLocation SPARKLE = ArcanesOublies.id("textures/entity/gem/sparkle.png");
    public static final ResourceLocation SIGIL = ArcanesOublies.id("textures/entity/gem/sigil.png");

    public enum Cut { BRILLIANT, STEP, CRYSTAL }

    /** Each cut's triangles (9 floats each, about one block across and high, centred), and the corners that twinkle. */
    private static final float[][] MESH = new float[Cut.values().length][];
    private static final float[][] SPARKS = new float[Cut.values().length][];
    private static final Vector3f LIGHT = new Vector3f(.35f, 1, .55f).normalize();

    static {
        build(Cut.BRILLIANT, brilliant());
        build(Cut.STEP, step());
        build(Cut.CRYSTAL, crystal());
    }

    private GemRender() {}

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
        return new float[]{ring[k], ring[k + 1], ring[k + 2]};
    }

    private static void tri(List<float[]> out, float[] a, float[] b, float[] c) {
        out.add(new float[]{a[0], a[1], a[2], b[0], b[1], b[2], c[0], c[1], c[2]});
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

    /** Table, crown (kites and stars between an 8-sided table and a 16-sided girdle), girdle, pavilion to a point. */
    private static Mesh brilliant() {
        List<float[]> t = new ArrayList<>();
        List<float[]> s = new ArrayList<>();
        float[] table = ring(8, .3f, .27f, 0);
        float[] girdle = ring(16, .1f, .5f, 0);
        float[] girdleLow = ring(16, .05f, .5f, 0);
        float[] pavilion = ring(8, -.2f, .27f, .5f);
        fan(t, table, new float[]{0, .3f, 0});
        for (int i = 0; i < 8; i++) {
            float[] ti = at(table, i), tn = at(table, i + 1);
            tri(t, ti, at(girdle, 2 * i), at(girdle, 2 * i + 1));
            tri(t, ti, at(girdle, 2 * i + 1), tn);
            tri(t, tn, at(girdle, 2 * i + 1), at(girdle, 2 * i + 2));
        }
        band(t, girdleLow, girdle);
        // Pavilion: the girdle comes down to a ring of mains, then to the point
        for (int i = 0; i < 8; i++) {
            float[] p = at(pavilion, i);
            tri(t, at(girdleLow, 2 * i + 2), at(girdleLow, 2 * i + 1), p);
            tri(t, at(girdleLow, 2 * i + 1), at(girdleLow, 2 * i), at(pavilion, i - 1));
            tri(t, at(girdleLow, 2 * i + 1), at(pavilion, i - 1), p);
        }
        fan(t, pavilion, new float[]{0, -.55f, 0});
        corners(s, table, 1);
        corners(s, girdle, 2);
        s.add(new float[]{0, -.55f, 0});
        return new Mesh(t, s);
    }

    /** One ring of the emerald cut: an oblong octagon (a rectangle with its corners cut), scaled. */
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

    /** The emerald cut: stepped rings up to a flat table, and down to a keel. */
    private static Mesh step() {
        List<float[]> t = new ArrayList<>();
        List<float[]> s = new ArrayList<>();
        float[][] rings = {
                octagon(-.42f, .5f, .06f),
                octagon(-.3f, .45f, .4f),
                octagon(-.12f, .78f, .74f),
                octagon(.04f, 1, 1),
                octagon(.1f, 1, 1),
                octagon(.2f, .84f, .82f),
                octagon(.28f, .64f, .6f)};
        for (int i = 0; i + 1 < rings.length; i++) {
            band(t, rings[i], rings[i + 1]);
        }
        fan(t, rings[rings.length - 1], new float[]{0, .28f, 0});
        fan(t, rings[0], new float[]{0, -.42f, 0});
        corners(s, rings[4], 1);
        corners(s, rings[6], 2);
        return new Mesh(t, s);
    }

    /** A six-sided crystal point. */
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
        fan(t, base, new float[]{0, -.5f, 0});
        s.add(tip);
        corners(s, top, 2);
        return new Mesh(t, s);
    }

    /**
     * A gem of {@code cut} in {@code rgb}, {@code size} across, centred on the origin, {@code alpha} opaque (about .7
     * reads as a gem). {@code time} (in ticks) makes its facets flash in turn as if it turned in the light.
     */
    public static void gem(PoseStack poseStack, MultiBufferSource buffer, Cut cut, int rgb, float size, float alpha, float time) {
        float[] tris = MESH[cut.ordinal()];
        VertexConsumer consumer = buffer.getBuffer(com.waylays.arcanes.client.render.SpellRenderTypes.veil(FACET));
        poseStack.pushPose();
        poseStack.scale(size, size, size);
        PoseStack.Pose pose = poseStack.last();
        int a = (int) (Mth.clamp(alpha, 0, 1) * 255);
        int r0 = (rgb >> 16) & 0xFF, g0 = (rgb >> 8) & 0xFF, b0 = rgb & 0xFF;
        for (int i = 0; i < tris.length; i += 9) {
            float ux = tris[i + 3] - tris[i], uy = tris[i + 4] - tris[i + 1], uz = tris[i + 5] - tris[i + 2];
            float vx = tris[i + 6] - tris[i], vy = tris[i + 7] - tris[i + 1], vz = tris[i + 8] - tris[i + 2];
            float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
            float len = Mth.sqrt(nx * nx + ny * ny + nz * nz);
            if (len < 1.0E-6f) {
                continue;
            }
            nx /= len;
            ny /= len;
            nz /= len;
            // Outward
            float cx = tris[i] + tris[i + 3] + tris[i + 6], cy = tris[i + 1] + tris[i + 4] + tris[i + 7], cz = tris[i + 2] + tris[i + 5] + tris[i + 8];
            if (nx * cx + ny * cy + nz * cz < 0) {
                nx = -nx;
                ny = -ny;
                nz = -nz;
            }
            int facet = i / 9;
            float shade = .5f + .5f * Math.abs(nx * LIGHT.x() + ny * LIGHT.y() + nz * LIGHT.z());
            // A facet catches the light for a moment, each in its turn
            float phase = (time * .07f + RuneRender.hash(facet * 31 + cut.ordinal()) * 7) % 7;
            float flash = phase < 1 ? Mth.sin(phase * Mth.PI) : 0;
            int r = (int) Mth.lerp(flash * .75f, r0 * shade, 255);
            int g = (int) Mth.lerp(flash * .75f, g0 * shade, 255);
            int b = (int) Mth.lerp(flash * .75f, b0 * shade, 255);
            int argb = a << 24 | Mth.clamp(r, 0, 255) << 16 | Mth.clamp(g, 0, 255) << 8 | Mth.clamp(b, 0, 255);
            RenderHelper.vertex(consumer, pose, tris[i], tris[i + 1], tris[i + 2], 0, 0, argb, GlyphRender.FULL, nx, ny, nz);
            RenderHelper.vertex(consumer, pose, tris[i + 3], tris[i + 4], tris[i + 5], 1, 0, argb, GlyphRender.FULL, nx, ny, nz);
            RenderHelper.vertex(consumer, pose, tris[i + 6], tris[i + 7], tris[i + 8], .5f, 1, argb, GlyphRender.FULL, nx, ny, nz);
            RenderHelper.vertex(consumer, pose, tris[i + 6], tris[i + 7], tris[i + 8], .5f, 1, argb, GlyphRender.FULL, nx, ny, nz);
        }
        poseStack.popPose();
    }

    /** The glow at the heart of a gem {@code size} across, facing the camera ({@code strength} 0 to 1). */
    public static void heart(PoseStack poseStack, MultiBufferSource buffer, int rgb, float size, float strength) {
        if (strength <= .01f) {
            return;
        }
        poseStack.pushPose();
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        GlyphRender.halo(buffer, poseStack.last(), GlyphRender.GLOW, size * .75f, RuneRender.additive(rgb, strength * .7f));
        GlyphRender.halo(buffer, poseStack.last(), GlyphRender.GLOW, size * .3f, RuneRender.additive(GlyphRender.whiten(rgb, .6f), strength));
        poseStack.popPose();
    }

    /**
     * Sparkles twinkling on the corners of a gem of {@code cut}, {@code size} across, as it is drawn by {@link #gem}
     * (call it with the same pose). {@code rainbow}: the fire of a diamond, each sparkle its own colour.
     */
    public static void sparkles(PoseStack poseStack, MultiBufferSource buffer, Cut cut, int rgb, float size, float time, boolean rainbow, int seed) {
        float[] sparks = SPARKS[cut.ordinal()];
        VertexConsumer consumer = buffer.getBuffer(RenderType.eyes(SPARKLE));
        for (int i = 0; i < sparks.length; i += 3) {
            float h = RuneRender.hash(seed + i * 7);
            float tw = Mth.sin(time * (.18f + .1f * h) + h * 40);
            if (tw <= .55f) {
                continue;
            }
            float strength = (tw - .55f) / .45f;
            int color = rainbow ? Mth.hsvToRgb((h * 3 + time * .01f) % 1, .55f, 1) : GlyphRender.whiten(rgb, .5f);
            poseStack.pushPose();
            poseStack.translate(sparks[i] * size, sparks[i + 1] * size, sparks[i + 2] * size);
            poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
            poseStack.mulPose(Axis.ZP.rotationDegrees(time * 3 + h * 90));
            RenderHelper.billboard(consumer, poseStack.last(), size * (.12f + .14f * strength), RuneRender.additive(color, strength), GlyphRender.FULL);
            poseStack.popPose();
        }
    }

    /** The jeweller's sigil burning on the ground at the origin in {@code rgb}: {@code radius} wide, turning. */
    public static void sigil(PoseStack poseStack, MultiBufferSource buffer, int rgb, float radius, float strength, float spin) {
        if (strength <= .01f) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0, .045, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(SIGIL)), poseStack.last(), radius, RuneRender.additive(rgb, strength), GlyphRender.FULL);
        poseStack.translate(0, .01, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-spin * 2.2f));
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), radius * 1.12f,
                RuneRender.additive(GlyphRender.whiten(rgb, .4f), strength * .35f), GlyphRender.FULL);
        poseStack.popPose();
    }

    /**
     * A ray of light from {@code a} to {@code b} (relative to the pose): a wide glow of {@code rgb}, a brighter band and
     * a white core, {@code width} across, flickering a little.
     */
    public static void ray(PoseStack poseStack, MultiBufferSource buffer, Vec3 a, Vec3 b, int rgb, float width, float strength, float time) {
        VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
        float flicker = 1 + .12f * Mth.sin(time * 1.7f) + .06f * Mth.sin(time * 4.3f);
        PoseStack.Pose pose = poseStack.last();
        GlyphRender.beam(stroke, pose, a, b, width * 1.9f * flicker, RuneRender.additive(rgb, strength * .35f));
        GlyphRender.beam(stroke, pose, a, b, width * flicker, RuneRender.additive(GlyphRender.whiten(rgb, .25f), strength * .8f));
        GlyphRender.beam(stroke, pose, a, b, width * .35f, RuneRender.additive(GlyphRender.whiten(rgb, .85f), strength));
    }

    /** A soft glow facing the camera at {@code at} (relative to the pose). */
    public static void glow(PoseStack poseStack, MultiBufferSource buffer, Vec3 at, int rgb, float size, float strength) {
        if (strength <= .01f) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        GlyphRender.halo(buffer, poseStack.last(), GlyphRender.GLOW, size, RuneRender.additive(rgb, strength));
        poseStack.popPose();
    }

    /** A whole gem: the facets, the heart and the sparkles. */
    public static void jewel(PoseStack poseStack, MultiBufferSource buffer, Cut cut, int rgb, float size, float alpha, float time,
                             float glow, boolean rainbow, int seed) {
        gem(poseStack, buffer, cut, rgb, size, alpha, time);
        heart(poseStack, buffer, rgb, size, glow);
        sparkles(poseStack, buffer, cut, rgb, size, time, rainbow, seed);
    }
}
