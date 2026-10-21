package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Drawing shared by the Glyph Magic renderers: glyphs written in light, stroke by stroke (ribbons of glow with a
 * white-hot core, the pen tip blazing where the writing has got to), and jagged arcs of lightning. Everything is
 * additive and full bright, and seen from both sides.
 */
public final class GlyphRender {
    public static final int FULL = LightTexture.FULL_BRIGHT;
    /** A stroke of light across its width (u), the same along it. */
    public static final ResourceLocation STROKE = ArcanesOublies.id("textures/entity/glyph/stroke.png");
    public static final ResourceLocation GLOW = ArcanesOublies.id("textures/entity/glyph/glow.png");
    public static final ResourceLocation RIPPLE = ArcanesOublies.id("textures/entity/glyph/ripple.png");
    public static final ResourceLocation SWIRL = ArcanesOublies.id("textures/entity/glyph/swirl.png");
    public static final ResourceLocation WAVE = ArcanesOublies.id("textures/entity/glyph/wave.png");

    /** Widths of the glow and of the core, for a glyph of size 1. */
    private static final float GLOW_WIDTH = .2f;
    private static final float CORE_WIDTH = .06f;

    private GlyphRender() {}

    /**
     * Writes {@code strokes} (glyph space, -1 to 1) in the XY plane, {@code size} from the centre to the edge, up to
     * {@code written} of their whole length (0 to 1), in {@code color} with a {@code core} centre, at {@code strength}
     * (0 to 1). {@code width} scales the strokes. Returns the pen tip (x, y in the plane) while it is writing, else null.
     */
    @Nullable
    public static float[] strokes(MultiBufferSource buffer, PoseStack.Pose pose, List<float[][]> strokes, float size, float written,
                                  int color, int core, float strength, float width) {
        if (written <= 0 || strength <= 0) {
            return null;
        }
        float total = 0;
        for (float[][] stroke : strokes) {
            total += length(stroke);
        }
        // Fully written: no pen tip left at the end of the last stroke from rounding
        float budget = written >= 1 ? Float.MAX_VALUE : total * written;
        float[] tip = null;
        int glow = RuneRender.additive(color, strength * .85f);
        int hot = RuneRender.additive(core, strength);
        VertexConsumer consumer = buffer.getBuffer(RenderType.eyes(STROKE));
        // Glow and core in one pass over the strokes: both use the stroke texture
        float left = budget;
        for (float[][] stroke : strokes) {
            if (left <= 0) {
                break;
            }
            for (int i = 1; i < stroke.length && left > 0; i++) {
                float ax = stroke[i - 1][0], ay = stroke[i - 1][1];
                float bx = stroke[i][0], by = stroke[i][1];
                float seg = Mth.sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay));
                if (seg > left) {
                    float t = left / seg;
                    bx = ax + (bx - ax) * t;
                    by = ay + (by - ay) * t;
                    tip = new float[]{bx * size, by * size};
                }
                left -= seg;
                ribbon(consumer, pose, ax * size, ay * size, bx * size, by * size, GLOW_WIDTH * size * width, glow);
                ribbon(consumer, pose, ax * size, ay * size, bx * size, by * size, CORE_WIDTH * size * width, hot);
            }
        }
        // Round off the ends of the strokes, and light the pen tip
        consumer = buffer.getBuffer(RenderType.eyes(GLOW));
        left = budget;
        for (float[][] stroke : strokes) {
            if (left <= 0) {
                break;
            }
            dot(consumer, pose, stroke[0][0] * size, stroke[0][1] * size, GLOW_WIDTH * size * width * .7f, glow);
            float len = length(stroke);
            if (len <= left) {
                float[] end = stroke[stroke.length - 1];
                dot(consumer, pose, end[0] * size, end[1] * size, GLOW_WIDTH * size * width * .7f, glow);
            }
            left -= len;
        }
        if (tip != null) {
            dot(consumer, pose, tip[0], tip[1], .3f * size * width, RuneRender.additive(core, strength));
            dot(consumer, pose, tip[0], tip[1], .6f * size * width, RuneRender.additive(color, strength * .7f));
        }
        return tip;
    }

    public static float length(float[][] stroke) {
        float len = 0;
        for (int i = 1; i < stroke.length; i++) {
            float dx = stroke[i][0] - stroke[i - 1][0];
            float dy = stroke[i][1] - stroke[i - 1][1];
            len += Mth.sqrt(dx * dx + dy * dy);
        }
        return len;
    }

    /** A flat ribbon from a to b in the XY plane, {@code width} wide, seen from both sides. */
    public static void ribbon(VertexConsumer consumer, PoseStack.Pose pose, float ax, float ay, float bx, float by, float width, int argb) {
        float dx = bx - ax, dy = by - ay;
        float len = Mth.sqrt(dx * dx + dy * dy);
        if (len < 1.0E-5f) {
            return;
        }
        // Stretched a little at both ends, so neighbouring segments overlap at the joints
        float ex = dx / len * width * .25f, ey = dy / len * width * .25f;
        float nx = -dy / len * width * .5f, ny = dx / len * width * .5f;
        float x0 = ax - ex, y0 = ay - ey, x1 = bx + ex, y1 = by + ey;
        RenderHelper.vertex(consumer, pose, x0 - nx, y0 - ny, 0, 0, 0, argb, FULL, 0, 0, 1);
        RenderHelper.vertex(consumer, pose, x0 + nx, y0 + ny, 0, 1, 0, argb, FULL, 0, 0, 1);
        RenderHelper.vertex(consumer, pose, x1 + nx, y1 + ny, 0, 1, 1, argb, FULL, 0, 0, 1);
        RenderHelper.vertex(consumer, pose, x1 - nx, y1 - ny, 0, 0, 1, argb, FULL, 0, 0, 1);
        RenderHelper.vertex(consumer, pose, x1 - nx, y1 - ny, 0, 0, 1, argb, FULL, 0, 0, -1);
        RenderHelper.vertex(consumer, pose, x1 + nx, y1 + ny, 0, 1, 1, argb, FULL, 0, 0, -1);
        RenderHelper.vertex(consumer, pose, x0 + nx, y0 + ny, 0, 1, 0, argb, FULL, 0, 0, -1);
        RenderHelper.vertex(consumer, pose, x0 - nx, y0 - ny, 0, 0, 0, argb, FULL, 0, 0, -1);
    }

    /** A round glow of radius {@code r} at (x, y) in the XY plane, seen from both sides. */
    public static void dot(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float r, int argb) {
        RenderHelper.vertex(consumer, pose, x - r, y - r, 0, 0, 1, argb, FULL, 0, 0, 1);
        RenderHelper.vertex(consumer, pose, x + r, y - r, 0, 1, 1, argb, FULL, 0, 0, 1);
        RenderHelper.vertex(consumer, pose, x + r, y + r, 0, 1, 0, argb, FULL, 0, 0, 1);
        RenderHelper.vertex(consumer, pose, x - r, y + r, 0, 0, 0, argb, FULL, 0, 0, 1);
        RenderHelper.vertex(consumer, pose, x - r, y + r, 0, 0, 0, argb, FULL, 0, 0, -1);
        RenderHelper.vertex(consumer, pose, x + r, y + r, 0, 1, 0, argb, FULL, 0, 0, -1);
        RenderHelper.vertex(consumer, pose, x + r, y - r, 0, 1, 1, argb, FULL, 0, 0, -1);
        RenderHelper.vertex(consumer, pose, x - r, y - r, 0, 0, 1, argb, FULL, 0, 0, -1);
    }

    /** A round glow in the XY plane at the origin. */
    public static void halo(MultiBufferSource buffer, PoseStack.Pose pose, ResourceLocation texture, float r, int argb) {
        dot(buffer.getBuffer(RenderType.eyes(texture)), pose, 0, 0, r, argb);
    }

    /** A jagged bolt from {@code from} to {@code to}: a white core in a glow of {@code color}; its kinks follow {@code seed}. */
    public static void lightning(MultiBufferSource buffer, PoseStack poseStack, Vec3 from, Vec3 to, int seed, int color, int core,
                                 float strength, float width) {
        Vec3 d = to.subtract(from);
        double length = d.length();
        if (length < 1.0E-3) {
            return;
        }
        int segments = Mth.clamp((int) (length * 1.5), 3, 14);
        Vec3 side = d.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 up = d.cross(side).normalize();
        float jag = (float) Math.min(.7, length * .08 + .15);
        Vec3[] points = new Vec3[segments + 1];
        for (int i = 0; i <= segments; i++) {
            float t = i / (float) segments;
            float wobble = i == 0 || i == segments ? 0 : jag;
            points[i] = from.add(d.scale(t))
                    .add(side.scale((RuneRender.hash(seed + i * 7) - .5f) * 2 * wobble))
                    .add(up.scale((RuneRender.hash(seed + i * 13) - .5f) * 2 * wobble));
        }
        VertexConsumer consumer = buffer.getBuffer(RenderType.eyes(STROKE));
        int glow = RuneRender.additive(color, strength);
        int hot = RuneRender.additive(core, strength);
        for (int i = 0; i < segments; i++) {
            beam(consumer, poseStack.last(), points[i], points[i + 1], .45f * width, glow);
            beam(consumer, poseStack.last(), points[i], points[i + 1], .14f * width, hot);
        }
    }

    /**
     * A strand of light from {@code a} to {@code b}, {@code width} wide: two crossed ribbons of the stroke texture, each
     * seen from both sides, so it reads from every angle.
     */
    public static void beam(VertexConsumer consumer, PoseStack.Pose pose, Vec3 a, Vec3 b, float width, int argb) {
        Vec3 dir = b.subtract(a);
        double len = dir.length();
        if (len < 1.0E-4) {
            return;
        }
        dir = dir.scale(1 / len);
        Vec3 side = dir.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 side2 = dir.cross(side).normalize();
        // Stretched a little at both ends, so the segments of a bolt overlap at their joints
        Vec3 ext = dir.scale(width * .25);
        Vec3 a2 = a.subtract(ext), b2 = b.add(ext);
        for (Vec3 off : new Vec3[]{side.scale(width * .5), side2.scale(width * .5)}) {
            quad(consumer, pose, a2.subtract(off), a2.add(off), b2.add(off), b2.subtract(off), argb);
            quad(consumer, pose, b2.subtract(off), b2.add(off), a2.add(off), a2.subtract(off), argb);
        }
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int argb) {
        RenderHelper.vertex(consumer, pose, (float) p0.x, (float) p0.y, (float) p0.z, 0, 0, argb, FULL, 0, 1, 0);
        RenderHelper.vertex(consumer, pose, (float) p1.x, (float) p1.y, (float) p1.z, 1, 0, argb, FULL, 0, 1, 0);
        RenderHelper.vertex(consumer, pose, (float) p2.x, (float) p2.y, (float) p2.z, 1, 1, argb, FULL, 0, 1, 0);
        RenderHelper.vertex(consumer, pose, (float) p3.x, (float) p3.y, (float) p3.z, 0, 1, argb, FULL, 0, 1, 0);
    }

    /** A colour lifted towards white by {@code t} (0 to 1). */
    public static int whiten(int rgb, float t) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        r += (int) ((255 - r) * t);
        g += (int) ((255 - g) * t);
        b += (int) ((255 - b) * t);
        return r << 16 | g << 8 | b;
    }
}
