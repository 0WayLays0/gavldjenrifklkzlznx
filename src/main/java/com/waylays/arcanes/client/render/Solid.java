package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Solid shapes built of triangles for spell renderers (ingots, coins, spikes, blades, scales...), and their drawing as
 * polished metal: each face shaded by the way it faces a fixed light, catching a flash of light now and then as the
 * thing turns. A shape is a float array, 9 floats a triangle.
 */
public final class Solid {
    private static final Vector3f LIGHT = new Vector3f(.4f, 1, .6f).normalize();

    private Solid() {}

    /** Builds a shape. */
    public static final class Builder {
        private final List<float[]> tris = new ArrayList<>();

        public Builder tri(float[] a, float[] b, float[] c) {
            tris.add(new float[]{a[0], a[1], a[2], b[0], b[1], b[2], c[0], c[1], c[2]});
            return this;
        }

        public Builder quad(float[] a, float[] b, float[] c, float[] d) {
            return tri(a, b, c).tri(a, c, d);
        }

        /** The side between two rings of the same number of points. */
        public Builder band(float[][] lower, float[][] upper) {
            int n = lower.length;
            for (int i = 0; i < n; i++) {
                quad(lower[i], lower[(i + 1) % n], upper[(i + 1) % n], upper[i]);
            }
            return this;
        }

        /** A ring closed by a point. */
        public Builder fan(float[][] ring, float[] tip) {
            int n = ring.length;
            for (int i = 0; i < n; i++) {
                tri(ring[i], ring[(i + 1) % n], tip);
            }
            return this;
        }

        /** A box from (x0, y0, z0) to (x1, y1, z1). */
        public Builder box(float x0, float y0, float z0, float x1, float y1, float z1) {
            float[][] lo = {{x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}, {x0, y0, z1}};
            float[][] hi = {{x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}};
            band(lo, hi);
            quad(hi[0], hi[1], hi[2], hi[3]);
            quad(lo[3], lo[2], lo[1], lo[0]);
            return this;
        }

        public float[] build() {
            float[] out = new float[tris.size() * 9];
            for (int i = 0; i < tris.size(); i++) {
                System.arraycopy(tris.get(i), 0, out, i * 9, 9);
            }
            return out;
        }
    }

    /** {@code n} points round the Y axis at height {@code y}, {@code rx} by {@code rz} across, turned by {@code offset} steps. */
    public static float[][] ring(int n, float y, float rx, float rz, float offset) {
        float[][] out = new float[n][];
        for (int i = 0; i < n; i++) {
            float a = (i + offset) * Mth.TWO_PI / n;
            out[i] = new float[]{Mth.cos(a) * rx, y, Mth.sin(a) * rz};
        }
        return out;
    }

    /**
     * Draws {@code shape} in {@code rgb}: each face shaded by how it faces the light, flashing toward white in turn as
     * {@code time} goes on ({@code shine} how often and how much), with full light.
     */
    public static void draw(VertexConsumer consumer, PoseStack.Pose pose, float[] shape, int rgb, int alpha, float time, float shine, int light) {
        int r0 = (rgb >> 16) & 0xFF, g0 = (rgb >> 8) & 0xFF, b0 = rgb & 0xFF;
        for (int i = 0; i < shape.length; i += 9) {
            float ux = shape[i + 3] - shape[i], uy = shape[i + 4] - shape[i + 1], uz = shape[i + 5] - shape[i + 2];
            float vx = shape[i + 6] - shape[i], vy = shape[i + 7] - shape[i + 1], vz = shape[i + 8] - shape[i + 2];
            float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
            float len = Mth.sqrt(nx * nx + ny * ny + nz * nz);
            if (len < 1.0E-7f) {
                continue;
            }
            nx /= len;
            ny /= len;
            nz /= len;
            float lit = nx * LIGHT.x() + ny * LIGHT.y() + nz * LIGHT.z();
            float shade = .45f + .55f * Math.max(0, lit) + .15f * Math.max(0, -lit);
            float flash = 0;
            if (shine > 0) {
                float phase = (time * .05f * shine + RuneRender.hash(i / 9 * 17) * 9) % 9;
                flash = phase < 1 ? Mth.sin(phase * Mth.PI) * Math.min(1, shine) : 0;
            }
            int r = Mth.clamp((int) Mth.lerp(flash * .7f, r0 * shade, 255), 0, 255);
            int g = Mth.clamp((int) Mth.lerp(flash * .7f, g0 * shade, 255), 0, 255);
            int b = Mth.clamp((int) Mth.lerp(flash * .7f, b0 * shade, 255), 0, 255);
            int argb = alpha << 24 | r << 16 | g << 8 | b;
            RenderHelper.vertex(consumer, pose, shape[i], shape[i + 1], shape[i + 2], 0, 0, argb, light, nx, ny, nz);
            RenderHelper.vertex(consumer, pose, shape[i + 3], shape[i + 4], shape[i + 5], 1, 0, argb, light, nx, ny, nz);
            RenderHelper.vertex(consumer, pose, shape[i + 6], shape[i + 7], shape[i + 8], .5f, 1, argb, light, nx, ny, nz);
            RenderHelper.vertex(consumer, pose, shape[i + 6], shape[i + 7], shape[i + 8], .5f, 1, argb, light, nx, ny, nz);
        }
    }
}
