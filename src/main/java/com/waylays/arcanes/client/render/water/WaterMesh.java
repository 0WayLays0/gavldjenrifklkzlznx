package com.waylays.arcanes.client.render.water;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.RenderHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Drawing shared by the Water Magic renderers, made to keep water from ever looking like a smooth plastic shape: its
 * surfaces are pushed in and out by flowing noise, its outlines broken, its colour running from deep blue through
 * clear turquoise to white where it churns; white water is drawn as clumps of spray, flying drops as streaks along
 * their way. Pure geometry (it only needs a pose and buffers), so the render lab (tools/renderlab) can draw it too.
 */
public final class WaterMesh {
    /** Running water, pale (its colour and see-through are set per vertex): streaks along V, the way it flows. */
    public static final ResourceLocation WATER = ArcanesOublies.id("textures/entity/water/flow.png");
    /** Foam: lacy white water, see-through between its bubbles. */
    public static final ResourceLocation FOAM = ArcanesOublies.id("textures/entity/water/foam.png");
    /** Four clumps of spray (2 x 2): churning white water seen close, drawn on billboards. */
    public static final ResourceLocation SPRAY = ArcanesOublies.id("textures/entity/water/spray.png");
    /** A flying drop drawn stretched along its way: a bright head, a fading tail (V from head to tail). */
    public static final ResourceLocation STREAK = ArcanesOublies.id("textures/entity/water/streak.png");
    /** A soft puff of mist or steam. */
    public static final ResourceLocation MIST = ArcanesOublies.id("textures/entity/water/mist.png");
    /** A puff of cloud, white (shaded per vertex). */
    public static final ResourceLocation CLOUD = ArcanesOublies.id("textures/entity/water/cloud.png");
    /** The light that shines through waves onto what is under them (bright lines on black: additive). */
    public static final ResourceLocation CAUSTICS = ArcanesOublies.id("textures/entity/water/caustics.png");
    /** A round drop, its rim catching the light. */
    public static final ResourceLocation DROP = ArcanesOublies.id("textures/entity/water/drop.png");
    /** A soap film: rainbow swirls on clear. */
    public static final ResourceLocation BUBBLE = ArcanesOublies.id("textures/entity/water/bubble.png");
    /** A bolt of light across U (bright in its middle, black at its edges: drawn additive). */
    public static final ResourceLocation BOLT = ArcanesOublies.id("textures/entity/water/bolt.png");
    /** A rainbow's bands across U (red at 0, violet at 1), see-through at both edges. */
    public static final ResourceLocation RAINBOW = ArcanesOublies.id("textures/entity/water/rainbow.png");

    /** Deep water, open water, shallow sunlit water, the pale green of a thin crest, foam. */
    public static final int ABYSS = 0x0C2F5C;
    public static final int DEEP = 0x1A5C9E;
    public static final int OPEN = 0x2F86C8;
    public static final int SHALLOW = 0x5CC4E8;
    public static final int CREST = 0x9DEBE0;
    public static final int FOAM_WHITE = 0xF2FBFF;

    /** The normal given to spray, foam and drops: lit as from above, so white water stays white whichever way it faces. */
    public static final Vector3f UP = new Vector3f(0, 1, 0);

    private WaterMesh() {}

    /** The camera as a renderer sees it: its orientation, and where it is in the renderer's starting frame. */
    public record View(Quaternionf camera, Vector3f eye) {}

    public static VertexConsumer translucent(MultiBufferSource buffer, ResourceLocation texture) {
        return buffer.getBuffer(com.waylays.arcanes.client.render.SpellRenderTypes.veil(texture));
    }

    public static VertexConsumer additive(MultiBufferSource buffer, ResourceLocation texture) {
        return buffer.getBuffer(RenderType.eyes(texture));
    }

    // ------------------------------------------------------------------------------------------------- colour

    public static int argb(int rgb, float alpha) {
        return ((int) (Mth.clamp(alpha, 0, 1) * 255) << 24) | (rgb & 0xFFFFFF);
    }

    /** {@code rgb} scaled by {@code k} (for additive light), opaque. */
    public static int glow(int rgb, float k) {
        float s = Mth.clamp(k, 0, 1);
        return 0xFF000000 | (int) (((rgb >> 16) & 0xFF) * s) << 16 | (int) (((rgb >> 8) & 0xFF) * s) << 8 | (int) ((rgb & 0xFF) * s);
    }

    public static int mix(int a, int b, float t) {
        t = Mth.clamp(t, 0, 1);
        int r = (int) Mth.lerp(t, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
        int g = (int) Mth.lerp(t, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
        int bl = (int) Mth.lerp(t, a & 0xFF, b & 0xFF);
        return r << 16 | g << 8 | bl;
    }

    /** The colour of water by how deep it looks (0 deepest, 1 a thin sunlit crest), then foam above 1. */
    public static int water(float t) {
        if (t < .33f) {
            return mix(ABYSS, DEEP, t / .33f);
        }
        if (t < .66f) {
            return mix(DEEP, OPEN, (t - .33f) / .33f);
        }
        if (t < 1) {
            return mix(OPEN, SHALLOW, (t - .66f) / .34f);
        }
        return mix(SHALLOW, FOAM_WHITE, t - 1);
    }

    // -------------------------------------------------------------------------------------------------- noise

    public static float hash(int seed) {
        int h = seed * 0x9E3779B1;
        h ^= h >>> 15;
        h *= 0x85EBCA77;
        h ^= h >>> 13;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }

    private static float lattice(int x, int y, int z) {
        return hash(x * 73856093 ^ y * 19349663 ^ z * 83492791) * 2 - 1;
    }

    /** Smooth value noise in [-1, 1]. */
    public static float noise(float x, float y, float z) {
        int x0 = Mth.floor(x), y0 = Mth.floor(y), z0 = Mth.floor(z);
        float fx = x - x0, fy = y - y0, fz = z - z0;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        fz = fz * fz * (3 - 2 * fz);
        float a = Mth.lerp(fx, lattice(x0, y0, z0), lattice(x0 + 1, y0, z0));
        float b = Mth.lerp(fx, lattice(x0, y0 + 1, z0), lattice(x0 + 1, y0 + 1, z0));
        float c = Mth.lerp(fx, lattice(x0, y0, z0 + 1), lattice(x0 + 1, y0, z0 + 1));
        float d = Mth.lerp(fx, lattice(x0, y0 + 1, z0 + 1), lattice(x0 + 1, y0 + 1, z0 + 1));
        return Mth.lerp(fz, Mth.lerp(fy, a, b), Mth.lerp(fy, c, d));
    }

    /** Two octaves of noise: big swells with ripples on them. */
    public static float turbulence(float x, float y, float z) {
        return noise(x, y, z) * .7f + noise(x * 2.3f + 17, y * 2.3f, z * 2.3f - 5) * .3f;
    }

    // --------------------------------------------------------------------------------------------- primitives

    /**
     * A tube of water along {@code axis}, {@code radius[i]} round point i, {@code segs} round. Its surface is pushed in
     * and out by noise ({@code rough} of the radius; {@code grain} sets how fine) flowing along it with {@code flow},
     * so its outline is never smooth. The texture runs along it, {@code vPerBlock} tiles a block, scrolled by
     * {@code flow}, wound {@code twist} times round from end to end. Colour and alpha per point.
     */
    public static void tube(VertexConsumer c, PoseStack.Pose p, Vector3f[] axis, float[] radius, int[] argb, int segs, float rough, float grain,
                            float flow, float vPerBlock, float twist, float seed, int light) {
        tube(c, p, axis, radius, argb, segs, rough, grain, flow, vPerBlock, twist, seed, light, 0);
    }

    /** As {@link #tube}, its normals turned {@code lit} (0 to 1) of the way up, so white water is not shaded grey on its sides. */
    public static void tube(VertexConsumer c, PoseStack.Pose p, Vector3f[] axis, float[] radius, int[] argb, int segs, float rough, float grain,
                            float flow, float vPerBlock, float twist, float seed, int light, float lit) {
        int n = axis.length;
        if (n < 2) {
            return;
        }
        Vector3f[][] ring = new Vector3f[n][segs + 1];
        Vector3f[][] norm = new Vector3f[n][segs + 1];
        float[] along = new float[n];
        Vector3f normal = null;
        for (int i = 0; i < n; i++) {
            Vector3f t = new Vector3f(axis[Math.min(n - 1, i + 1)]).sub(axis[Math.max(0, i - 1)]);
            if (t.lengthSquared() < 1.0E-10f) {
                t.set(0, 1, 0);
            }
            t.normalize();
            if (normal == null) {
                normal = Math.abs(t.y) < .9f ? new Vector3f(0, 1, 0).cross(t) : new Vector3f(1, 0, 0).cross(t);
            } else {
                // Carried along the tube without twisting
                normal.sub(new Vector3f(t).mul(normal.dot(t)));
            }
            if (normal.lengthSquared() < 1.0E-10f) {
                normal = Math.abs(t.y) < .9f ? new Vector3f(0, 1, 0).cross(t) : new Vector3f(1, 0, 0).cross(t);
            }
            normal.normalize();
            Vector3f bi = new Vector3f(t).cross(normal).normalize();
            if (i > 0) {
                along[i] = along[i - 1] + axis[i].distance(axis[i - 1]);
            }
            for (int s = 0; s <= segs; s++) {
                float a = s * Mth.TWO_PI / segs;
                float ca = Mth.cos(a), sa = Mth.sin(a);
                float bump = 1 + rough * turbulence(ca * grain + seed, sa * grain, along[i] * grain - flow);
                Vector3f dir = new Vector3f(normal).mul(ca).add(new Vector3f(bi).mul(sa));
                ring[i][s] = new Vector3f(dir).mul(radius[i] * bump).add(axis[i]);
                norm[i][s] = lit > 0 ? new Vector3f(dir).mul(1 - lit).add(0, lit, 0).normalize() : dir;
            }
        }
        for (int i = 0; i + 1 < n; i++) {
            for (int s = 0; s < segs; s++) {
                float u0 = (float) s / segs + twist * along[i] / Math.max(.01f, along[n - 1]);
                float u1 = (float) (s + 1) / segs + twist * along[i] / Math.max(.01f, along[n - 1]);
                float u2 = (float) (s + 1) / segs + twist * along[i + 1] / Math.max(.01f, along[n - 1]);
                float u3 = (float) s / segs + twist * along[i + 1] / Math.max(.01f, along[n - 1]);
                float v0 = along[i] * vPerBlock - flow, v1 = along[i + 1] * vPerBlock - flow;
                put(c, p, ring[i][s], u0, v0, argb[i], light, norm[i][s]);
                put(c, p, ring[i + 1][s], u3, v1, argb[i + 1], light, norm[i + 1][s]);
                put(c, p, ring[i + 1][s + 1], u2, v1, argb[i + 1], light, norm[i + 1][s + 1]);
                put(c, p, ring[i][s + 1], u1, v0, argb[i], light, norm[i][s + 1]);
            }
        }
    }

    /** One quad of a grid of points: {@code pts[i][j]} with its texture {@code (u, v)} and colour, normals from its neighbours. */
    public static void grid(VertexConsumer c, PoseStack.Pose p, Vector3f[][] pts, float[][] u, float[][] v, int[][] argb, int light) {
        grid(c, p, pts, u, v, argb, light, 0);
    }

    /** As {@link #grid}, its normals turned {@code lit} (0 to 1) of the way up (water seen from below is not black). */
    public static void grid(VertexConsumer c, PoseStack.Pose p, Vector3f[][] pts, float[][] u, float[][] v, int[][] argb, int light, float lit) {
        int ni = pts.length, nj = pts[0].length;
        Vector3f[][] nrm = new Vector3f[ni][nj];
        for (int i = 0; i < ni; i++) {
            for (int j = 0; j < nj; j++) {
                Vector3f di = new Vector3f(pts[Math.min(ni - 1, i + 1)][j]).sub(pts[Math.max(0, i - 1)][j]);
                Vector3f dj = new Vector3f(pts[i][Math.min(nj - 1, j + 1)]).sub(pts[i][Math.max(0, j - 1)]);
                Vector3f n = di.cross(dj);
                n = n.lengthSquared() < 1.0E-12f ? new Vector3f(0, 1, 0) : n.normalize();
                nrm[i][j] = lit > 0 ? n.mul(1 - lit).add(0, lit, 0).normalize() : n;
            }
        }
        for (int i = 0; i + 1 < ni; i++) {
            for (int j = 0; j + 1 < nj; j++) {
                if ((argb[i][j] | argb[i + 1][j] | argb[i + 1][j + 1] | argb[i][j + 1]) >>> 24 == 0) {
                    continue;
                }
                put(c, p, pts[i][j], u[i][j], v[i][j], argb[i][j], light, nrm[i][j]);
                put(c, p, pts[i][j + 1], u[i][j + 1], v[i][j + 1], argb[i][j + 1], light, nrm[i][j + 1]);
                put(c, p, pts[i + 1][j + 1], u[i + 1][j + 1], v[i + 1][j + 1], argb[i + 1][j + 1], light, nrm[i + 1][j + 1]);
                put(c, p, pts[i + 1][j], u[i + 1][j], v[i + 1][j], argb[i + 1][j], light, nrm[i + 1][j]);
            }
        }
    }

    /**
     * A billboard facing the camera at (x, y, z) in the current pose, {@code size} across, turned by {@code spin}:
     * one of the texture's four quarters ({@code variant} 0 to 3), or the whole texture ({@code variant} -1).
     */
    public static void puff(VertexConsumer c, PoseStack poseStack, View view, float x, float y, float z, float size, float spin, int variant, int argb,
                            int light) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        Quaternionf turn = new Quaternionf(view.camera()).rotateZ(spin);
        poseStack.mulPose(turn);
        // Lit from above like the ground it hangs over, not shaded as a wall facing the camera
        Vector3f up = new Quaternionf(turn).conjugate().transform(new Vector3f(0, 1, 0));
        float h = size * .5f;
        float u0 = 0, v0 = 0, u1 = 1, v1 = 1;
        if (variant >= 0) {
            u0 = (variant & 1) * .5f;
            v0 = (variant >> 1 & 1) * .5f;
            u1 = u0 + .5f;
            v1 = v0 + .5f;
        }
        PoseStack.Pose p = poseStack.last();
        RenderHelper.vertex(c, p, -h, -h, 0, u0, v1, argb, light, up.x, up.y, up.z);
        RenderHelper.vertex(c, p, h, -h, 0, u1, v1, argb, light, up.x, up.y, up.z);
        RenderHelper.vertex(c, p, h, h, 0, u1, v0, argb, light, up.x, up.y, up.z);
        RenderHelper.vertex(c, p, -h, h, 0, u0, v0, argb, light, up.x, up.y, up.z);
        poseStack.popPose();
    }

    /**
     * A streak from {@code head} back to {@code tail} (in the pose's own frame), {@code width} wide, turned to face
     * {@code eye} (the camera, in the same frame): a drop flying, blurred along its way.
     */
    public static void streak(VertexConsumer c, PoseStack.Pose p, Vector3f eye, Vector3f head, Vector3f tail, float width, int argb, int light) {
        Vector3f d = new Vector3f(head).sub(tail);
        if (d.lengthSquared() < 1.0E-8f) {
            d.set(0, .01f, 0);
        }
        Vector3f toEye = new Vector3f(eye).sub(head);
        Vector3f side = new Vector3f(d).cross(toEye);
        if (side.lengthSquared() < 1.0E-10f) {
            side.set(1, 0, 0);
        }
        side.normalize().mul(width * .5f);
        Vector3f n = UP;
        put(c, p, new Vector3f(head).add(side), 1, 0, argb, light, n);
        put(c, p, new Vector3f(head).sub(side), 0, 0, argb, light, n);
        put(c, p, new Vector3f(tail).sub(side), 0, 1, argb, light, n);
        put(c, p, new Vector3f(tail).add(side), 1, 1, argb, light, n);
    }

    /** A ribbon through {@code pts}, {@code width[i]} wide across {@code side[i]}, texture along it ({@code vPerBlock}, scrolled by {@code flow}). */
    public static void ribbon(VertexConsumer c, PoseStack.Pose p, Vector3f[] pts, Vector3f[] side, float[] width, int[] argb, float vPerBlock, float flow,
                              int light) {
        ribbon(c, p, pts, side, width, argb, vPerBlock, flow, light, false);
    }

    /** As {@link #ribbon}, {@code lit} as from above (for foam and spray) instead of shaded by its own facing. */
    public static void ribbon(VertexConsumer c, PoseStack.Pose p, Vector3f[] pts, Vector3f[] side, float[] width, int[] argb, float vPerBlock, float flow,
                              int light, boolean lit) {
        float along = 0;
        for (int i = 0; i + 1 < pts.length; i++) {
            Vector3f a = pts[i], b = pts[i + 1];
            float next = along + a.distance(b);
            Vector3f sa = new Vector3f(side[i]).mul(width[i] * .5f), sb = new Vector3f(side[i + 1]).mul(width[i + 1] * .5f);
            Vector3f n = lit ? new Vector3f(UP) : new Vector3f(b).sub(a).cross(side[i]);
            if (n.lengthSquared() < 1.0E-10f) {
                n.set(0, 1, 0);
            }
            n.normalize();
            float v0 = along * vPerBlock - flow, v1 = next * vPerBlock - flow;
            put(c, p, new Vector3f(a).sub(sa), 0, v0, argb[i], light, n);
            put(c, p, new Vector3f(a).add(sa), 1, v0, argb[i], light, n);
            put(c, p, new Vector3f(b).add(sb), 1, v1, argb[i + 1], light, n);
            put(c, p, new Vector3f(b).sub(sb), 0, v1, argb[i + 1], light, n);
            along = next;
        }
    }

    /** A flat disc (or ring, from {@code r0}) at height {@code y}, its texture laid flat over it, {@code scale} blocks a tile. */
    public static void disc(VertexConsumer c, PoseStack.Pose p, float y, float r0, float r1, int segs, float scale, float du, float dv, int argb0, int argb1,
                            int light) {
        for (int s = 0; s < segs; s++) {
            float a0 = s * Mth.TWO_PI / segs, a1 = (s + 1) * Mth.TWO_PI / segs;
            float x00 = Mth.cos(a0) * r0, z00 = Mth.sin(a0) * r0, x01 = Mth.cos(a0) * r1, z01 = Mth.sin(a0) * r1;
            float x11 = Mth.cos(a1) * r1, z11 = Mth.sin(a1) * r1, x10 = Mth.cos(a1) * r0, z10 = Mth.sin(a1) * r0;
            Vector3f up = new Vector3f(0, 1, 0);
            put(c, p, new Vector3f(x00, y, z00), x00 / scale + du, z00 / scale + dv, argb0, light, up);
            put(c, p, new Vector3f(x01, y, z01), x01 / scale + du, z01 / scale + dv, argb1, light, up);
            put(c, p, new Vector3f(x11, y, z11), x11 / scale + du, z11 / scale + dv, argb1, light, up);
            put(c, p, new Vector3f(x10, y, z10), x10 / scale + du, z10 / scale + dv, argb0, light, up);
        }
    }

    public static void put(VertexConsumer c, PoseStack.Pose p, Vector3f at, float u, float v, int argb, int light, Vector3f n) {
        RenderHelper.vertex(c, p, at.x, at.y, at.z, u, v, argb, light, n.x, n.y, n.z);
    }

    /** Where a stream of drops thrown from {@code from} at {@code velocity} (blocks a tick) is after {@code t} ticks, falling. */
    public static Vector3f thrown(Vector3f from, Vector3f velocity, float t, float gravity) {
        return new Vector3f(from).add(velocity.x * t, velocity.y * t - .5f * gravity * t * t, velocity.z * t);
    }
}
