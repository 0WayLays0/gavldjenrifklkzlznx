package com.waylays.arcanes.client.render.ritual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.RenderHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import static com.waylays.arcanes.client.render.water.WaterMesh.*;

/**
 * Shared drawing for the rituals: chalk strokes and sigils laid on a plane (the ground, or a wall), lights, motes and
 * shafts of light. Pure geometry, so the render lab can draw it too.
 */
public final class RitualMesh {
    public static final ResourceLocation CHALK = ArcanesOublies.id("textures/entity/ritual/chalk.png");
    public static final ResourceLocation CHALK_GLOW = ArcanesOublies.id("textures/entity/ritual/chalk_glow.png");
    public static final ResourceLocation SIGILS = ArcanesOublies.id("textures/entity/ritual/sigils.png");
    public static final ResourceLocation SIGILS_GLOW = ArcanesOublies.id("textures/entity/ritual/sigils_glow.png");
    public static final ResourceLocation GLOW = ArcanesOublies.id("textures/entity/ritual/glow.png");
    public static final ResourceLocation MOTE = ArcanesOublies.id("textures/entity/ritual/mote.png");
    public static final ResourceLocation BEAM = ArcanesOublies.id("textures/entity/ritual/beam.png");
    public static final ResourceLocation SHOCK = ArcanesOublies.id("textures/entity/ritual/shock.png");
    public static final ResourceLocation VOID = ArcanesOublies.id("textures/entity/ritual/portal_void.png");
    public static final ResourceLocation SWIRL = ArcanesOublies.id("textures/entity/ritual/portal_swirl.png");
    public static final ResourceLocation RIM = ArcanesOublies.id("textures/entity/ritual/rim.png");
    public static final ResourceLocation CRYSTAL = ArcanesOublies.id("textures/entity/ritual/crystal.png");
    public static final ResourceLocation MENHIR = ArcanesOublies.id("textures/entity/ritual/menhir.png");

    /** Chalk, a warm white. */
    public static final int CHALK_WHITE = 0xF4F0E4;
    public static final int BROKEN = 0xFF3A2A;

    private RitualMesh() {}

    /**
     * A flat surface figures are drawn on: {@code origin} its middle (or foot, for a wall), {@code u} and {@code v} its
     * two directions (unit), {@code n} the way it faces; {@code mirror} when {@code u}, {@code v} turn the other way
     * round seen from {@code n} (textures are then flipped to read true).
     */
    public record Plane(Vector3f origin, Vector3f u, Vector3f v, Vector3f n, boolean mirror) {
        /** A plane, {@code mirror} worked out. */
        public static Plane of(Vector3f origin, Vector3f u, Vector3f v, Vector3f n) {
            return new Plane(origin, u, v, n, new Vector3f(u).cross(v).dot(n) < 0);
        }

        /** Point {@code (x, y)} of the plane, lifted {@code lift} off it. */
        public Vector3f at(float x, float y, float lift) {
            return new Vector3f(origin).add(u.x * x + v.x * y + n.x * lift, u.y * x + v.y * y + n.y * lift, u.z * x + v.z * y + n.z * lift);
        }

        /** The ground, turned so that its {@code u} lies at {@code angle} (radians, as the stations count it). */
        public static Plane ground(float angle) {
            float c = Mth.cos(angle), s = Mth.sin(angle);
            return new Plane(new Vector3f(), new Vector3f(c, 0, s), new Vector3f(-s, 0, c), new Vector3f(0, 1, 0), true);
        }

        /** A wall facing {@code (nx, nz)}: {@code u} along it (to the right as one faces it), {@code v} up. */
        public static Plane wall(float nx, float nz) {
            return new Plane(new Vector3f(), new Vector3f(nz, 0, -nx), new Vector3f(0, 1, 0), new Vector3f(nx, 0, nz), false);
        }
    }

    public static VertexConsumer chalk(MultiBufferSource buffer) {
        return buffer.getBuffer(RenderType.entityTranslucent(CHALK));
    }

    public static VertexConsumer light(MultiBufferSource buffer, ResourceLocation texture) {
        return buffer.getBuffer(RenderType.eyes(texture));
    }

    /**
     * A stroke along the plane through {@code (xs[i], ys[i])} from length {@code from} to {@code to} along it,
     * {@code width} wide, its texture running along it a tile every {@code tile} blocks.
     */
    public static void stroke(VertexConsumer c, PoseStack.Pose p, Plane plane, float[] xs, float[] ys, float from, float to, float width, float lift, float tile,
                              int argb, int light) {
        float along = 0;
        Vector3f n = plane.n();
        for (int i = 0; i + 1 < xs.length; i++) {
            float dx = xs[i + 1] - xs[i], dy = ys[i + 1] - ys[i];
            float len = Mth.sqrt(dx * dx + dy * dy);
            float next = along + len;
            if (len < 1.0E-5f || next <= from) {
                along = next;
                continue;
            }
            if (along >= to) {
                break;
            }
            float t0 = Math.max(0, (from - along) / len), t1 = Math.min(1, (to - along) / len);
            float ax = xs[i] + dx * t0, ay = ys[i] + dy * t0, bx = xs[i] + dx * t1, by = ys[i] + dy * t1;
            // Across the stroke, in the plane; the ends pushed out a little so the joins close
            float sx = -dy / len * width * .5f, sy = dx / len * width * .5f;
            float ex = dx / len * width * .3f, ey = dy / len * width * .3f;
            float u0 = (along + len * t0) / tile, u1 = (along + len * t1) / tile;
            quad(c, p, plane, lift, argb, light, ax - ex + sx, ay - ey + sy, u0, 0, ax - ex - sx, ay - ey - sy, u0, 1, bx + ex - sx, by + ey - sy, u1, 1,
                    bx + ex + sx, by + ey + sy, u1, 0);
            along = next;
        }
    }

    /** Sigil {@code index} (0 to 15) of the atlas laid on the plane at {@code (x, y)}, {@code size} across, turned by {@code angle}. */
    public static void sigil(VertexConsumer c, PoseStack.Pose p, Plane plane, float x, float y, float size, float angle, int index, float lift, int argb,
                             int light) {
        float h = size * .5f;
        float cs = Mth.cos(angle) * h, sn = Mth.sin(angle) * h;
        float v0 = (index / 4) * .25f;
        float ul = (index % 4) * .25f, ur = ul + .25f;
        if (plane.mirror()) {
            ul = ur;
            ur = ul - .25f;
        }
        // Corners: the sigil's own up (V 0) toward +y of the plane turned by angle
        quad(c, p, plane, lift, argb, light, x - cs - sn, y - sn + cs, ul, v0, x - cs + sn, y - sn - cs, ul, v0 + .25f, x + cs + sn, y + sn - cs, ur, v0 + .25f,
                x + cs - sn, y + sn + cs, ur, v0);
    }

    /** A square of the whole texture laid on the plane at {@code (x, y)}, {@code size} across, turned by {@code angle}. */
    public static void flat(VertexConsumer c, PoseStack.Pose p, Plane plane, float x, float y, float size, float angle, float lift, int argb, int light) {
        float h = size * .5f;
        float cs = Mth.cos(angle) * h, sn = Mth.sin(angle) * h;
        float ul = plane.mirror() ? 1 : 0, ur = 1 - ul;
        quad(c, p, plane, lift, argb, light, x - cs - sn, y - sn + cs, ul, 0, x - cs + sn, y - sn - cs, ul, 1, x + cs + sn, y + sn - cs, ur, 1, x + cs - sn,
                y + sn + cs, ur, 0);
    }

    /**
     * A quad on the plane through its four corners {@code (x, y, u, v)}, given counter-clockwise in the plane's own
     * {@code (x, y)}; wound so that it faces the plane's {@code n} (culled render types show it only from there).
     */
    private static void quad(VertexConsumer c, PoseStack.Pose p, Plane plane, float lift, int argb, int light, float x0, float y0, float u0, float v0, float x1,
                             float y1, float u1, float v1, float x2, float y2, float u2, float v2, float x3, float y3, float u3, float v3) {
        Vector3f n = plane.n();
        if (plane.mirror()) {
            put(c, p, plane.at(x3, y3, lift), u3, v3, argb, light, n);
            put(c, p, plane.at(x2, y2, lift), u2, v2, argb, light, n);
            put(c, p, plane.at(x1, y1, lift), u1, v1, argb, light, n);
            put(c, p, plane.at(x0, y0, lift), u0, v0, argb, light, n);
        } else {
            put(c, p, plane.at(x0, y0, lift), u0, v0, argb, light, n);
            put(c, p, plane.at(x1, y1, lift), u1, v1, argb, light, n);
            put(c, p, plane.at(x2, y2, lift), u2, v2, argb, light, n);
            put(c, p, plane.at(x3, y3, lift), u3, v3, argb, light, n);
        }
    }

    /** A square of light (or a sigil, {@code index} 0 to 15, or -1 for the whole texture) facing the camera. */
    public static void billboard(VertexConsumer c, PoseStack poseStack, View view, Vector3f at, float size, float spin, int index, int argb, int light) {
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        poseStack.mulPose(view.camera());
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotation(spin));
        PoseStack.Pose p = poseStack.last();
        float h = size * .5f;
        float u0 = index < 0 ? 0 : (index % 4) * .25f, v0 = index < 0 ? 0 : (index / 4) * .25f, d = index < 0 ? 1 : .25f;
        RenderHelper.vertex(c, p, -h, -h, 0, u0, v0 + d, argb, light, 0, 1, 0);
        RenderHelper.vertex(c, p, h, -h, 0, u0 + d, v0 + d, argb, light, 0, 1, 0);
        RenderHelper.vertex(c, p, h, h, 0, u0 + d, v0, argb, light, 0, 1, 0);
        RenderHelper.vertex(c, p, -h, h, 0, u0, v0, argb, light, 0, 1, 0);
        poseStack.popPose();
    }

    /** A shaft of light from {@code a} to {@code b}, {@code width} wide, turned to face {@code eye}. */
    public static void shaft(VertexConsumer c, PoseStack.Pose p, Vector3f eye, Vector3f a, Vector3f b, float width, int argb, int light) {
        Vector3f d = new Vector3f(b).sub(a);
        if (d.lengthSquared() < 1.0E-8f) {
            return;
        }
        Vector3f mid = new Vector3f(a).add(b).mul(.5f);
        Vector3f side = new Vector3f(d).cross(new Vector3f(eye).sub(mid));
        if (side.lengthSquared() < 1.0E-10f) {
            side.set(1, 0, 0);
        }
        side.normalize().mul(width * .5f);
        float len = d.length();
        put(c, p, new Vector3f(a).sub(side), 0, 0, argb, light, UP);
        put(c, p, new Vector3f(a).add(side), 1, 0, argb, light, UP);
        put(c, p, new Vector3f(b).add(side), 1, len * .25f, argb, light, UP);
        put(c, p, new Vector3f(b).sub(side), 0, len * .25f, argb, light, UP);
    }

    /** A thread of light through {@code pts} (a curve), {@code width} wide, facing {@code eye}. */
    public static void thread(VertexConsumer c, PoseStack.Pose p, Vector3f eye, Vector3f[] pts, float width, int[] argb, int light) {
        for (int i = 0; i + 1 < pts.length; i++) {
            Vector3f a = pts[i], b = pts[i + 1];
            Vector3f d = new Vector3f(b).sub(a);
            Vector3f side = new Vector3f(d).cross(new Vector3f(eye).sub(a));
            if (side.lengthSquared() < 1.0E-10f) {
                continue;
            }
            side.normalize().mul(width * .5f);
            put(c, p, new Vector3f(a).sub(side), 0, 0, argb[i], light, UP);
            put(c, p, new Vector3f(a).add(side), 1, 0, argb[i], light, UP);
            put(c, p, new Vector3f(b).add(side), 1, 1, argb[i + 1], light, UP);
            put(c, p, new Vector3f(b).sub(side), 0, 1, argb[i + 1], light, UP);
        }
    }

    /** A box from {@code (x0, y0, z0)} to {@code (x1, y1, z1)} along the given axes from {@code at}, its texture over each face. */
    public static void block(VertexConsumer c, PoseStack.Pose p, Vector3f at, Vector3f ax, Vector3f ay, Vector3f az, float x0, float y0, float z0, float x1,
                             float y1, float z1, float vScale, int argb, int light) {
        Vector3f[] k = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            float x = (i & 1) == 0 ? x0 : x1, y = (i & 2) == 0 ? y0 : y1, z = (i & 4) == 0 ? z0 : z1;
            k[i] = new Vector3f(at).add(ax.x * x + ay.x * y + az.x * z, ax.y * x + ay.y * y + az.y * z, ax.z * x + ay.z * y + az.z * z);
        }
        int[][] faces = {{0, 4, 6, 2}, {5, 1, 3, 7}, {4, 5, 7, 6}, {1, 0, 2, 3}, {2, 6, 7, 3}, {0, 1, 5, 4}};
        Vector3f[] normals = {new Vector3f(ax).negate(), new Vector3f(ax), new Vector3f(az), new Vector3f(az).negate(), new Vector3f(ay), new Vector3f(ay).negate()};
        for (int f = 0; f < 6; f++) {
            int[] q = faces[f];
            float v1 = f < 4 ? vScale : 1;
            put(c, p, k[q[0]], 0, v1, argb, light, normals[f]);
            put(c, p, k[q[1]], 1, v1, argb, light, normals[f]);
            put(c, p, k[q[2]], 1, 0, argb, light, normals[f]);
            put(c, p, k[q[3]], 0, 0, argb, light, normals[f]);
        }
    }

    /** An elongated crystal (two pyramids base to base) from {@code base} along {@code dir} (unit), {@code len} long, {@code r} thick. */
    public static void crystal(VertexConsumer c, PoseStack.Pose p, Vector3f base, Vector3f dir, float len, float r, float spin, int argb, int light) {
        Vector3f s1 = Math.abs(dir.y) < .95f ? new Vector3f(dir).cross(0, 1, 0).normalize() : new Vector3f(1, 0, 0);
        Vector3f s2 = new Vector3f(dir).cross(s1).normalize();
        Vector3f tip = new Vector3f(base).add(new Vector3f(dir).mul(len));
        Vector3f waist = new Vector3f(base).add(new Vector3f(dir).mul(len * .3f));
        Vector3f[] ring = new Vector3f[4];
        for (int k = 0; k < 4; k++) {
            float a = spin + k * Mth.HALF_PI;
            ring[k] = new Vector3f(waist).add(new Vector3f(s1).mul(Mth.cos(a) * r)).add(new Vector3f(s2).mul(Mth.sin(a) * r));
        }
        for (int k = 0; k < 4; k++) {
            Vector3f a = ring[k], b = ring[(k + 1) % 4];
            Vector3f nUp = new Vector3f(b).sub(a).cross(new Vector3f(tip).sub(a)).normalize();
            put(c, p, a, 0, 1, argb, light, nUp);
            put(c, p, b, 1, 1, argb, light, nUp);
            put(c, p, tip, .5f, 0, argb, light, nUp);
            put(c, p, tip, .5f, 0, argb, light, nUp);
            Vector3f nDown = new Vector3f(base).sub(a).cross(new Vector3f(b).sub(a)).normalize();
            put(c, p, b, 1, 0, argb, light, nDown);
            put(c, p, a, 0, 0, argb, light, nDown);
            put(c, p, base, .5f, .4f, argb, light, nDown);
            put(c, p, base, .5f, .4f, argb, light, nDown);
        }
    }
}
