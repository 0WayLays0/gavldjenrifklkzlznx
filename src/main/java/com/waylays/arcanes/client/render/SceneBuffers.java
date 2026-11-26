package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A buffer source that lets a renderer draw with several render types at once, interleaved as it likes. The world's
 * buffer source keeps one shared builder for render types that have no buffer of their own: asking it for a second type
 * ends the first one's batch, and a vertex then written to the first consumer crashes the game ("Not building!"). Here
 * each render type records its vertices on its own; {@link #flush} then writes them out, one render type after another.
 * <p>
 * Use: {@code SceneBuffers b = SceneBuffers.begin(); ... draw with b ...; b.flush(buffer);} on the render thread.
 */
public final class SceneBuffers implements MultiBufferSource {
    private static SceneBuffers spare;

    private final Map<RenderType, Recorder> recorders = new LinkedHashMap<>();
    private boolean inUse;

    private SceneBuffers() {}

    /** A cleared one to draw into (the same one each frame, unless it is already in use). */
    public static SceneBuffers begin() {
        SceneBuffers b = spare;
        if (b == null || b.inUse) {
            b = new SceneBuffers();
            if (spare == null) {
                spare = b;
            }
        }
        b.inUse = true;
        return b;
    }

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        return recorders.computeIfAbsent(type, t -> new Recorder());
    }

    /**
     * Writes out everything recorded into {@code target}, one render type after another, then clears: the solid parts
     * first, so that the see-through parts and the light drawn after them show in front of them, not hidden by them.
     */
    public void flush(MultiBufferSource target) {
        try {
            for (int pass = 0; pass < 2; pass++) {
                for (Map.Entry<RenderType, Recorder> entry : recorders.entrySet()) {
                    Recorder r = entry.getValue();
                    if (r.count > 0 && solid(entry.getKey()) == (pass == 0)) {
                        r.replay(target.getBuffer(entry.getKey()));
                        r.clear();
                    }
                }
            }
            for (Recorder r : recorders.values()) {
                r.clear();
            }
        } finally {
            inUse = false;
        }
    }

    /** Whether {@code type} draws solid (the game's cutout and solid entity types). */
    private static boolean solid(RenderType type) {
        String name = type.toString();
        return name.contains("entity_cutout") || name.contains("entity_solid");
    }

    /** Records vertices as they are written (already transformed by the caller's pose), to write them out later. */
    private static final class Recorder implements VertexConsumer {
        private static final int COLOR = 1, UV = 2, UV1 = 4, UV2 = 8, NORMAL = 16;
        // Per vertex: x y z, r g b a, u v, uv1 (2), uv2 (2), nx ny nz, and the flags of what was set
        private static final int STRIDE = 17;
        private float[] data = new float[STRIDE * 256];
        private int count;

        private int at() {
            return (count - 1) * STRIDE;
        }

        private void flag(int bit) {
            if (count > 0) {
                data[at() + 16] = (int) data[at() + 16] | bit;
            }
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            if ((count + 1) * STRIDE > data.length) {
                data = Arrays.copyOf(data, data.length * 2);
            }
            count++;
            int i = at();
            data[i] = x;
            data[i + 1] = y;
            data[i + 2] = z;
            data[i + 16] = 0;
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            if (count > 0) {
                int i = at();
                data[i + 3] = r;
                data[i + 4] = g;
                data[i + 5] = b;
                data[i + 6] = a;
                flag(COLOR);
            }
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            if (count > 0) {
                int i = at();
                data[i + 7] = u;
                data[i + 8] = v;
                flag(UV);
            }
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            if (count > 0) {
                int i = at();
                data[i + 9] = u;
                data[i + 10] = v;
                flag(UV1);
            }
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            if (count > 0) {
                int i = at();
                data[i + 11] = u;
                data[i + 12] = v;
                flag(UV2);
            }
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            if (count > 0) {
                int i = at();
                data[i + 13] = x;
                data[i + 14] = y;
                data[i + 15] = z;
                flag(NORMAL);
            }
            return this;
        }

        void replay(VertexConsumer c) {
            for (int v = 0; v < count; v++) {
                int i = v * STRIDE;
                int set = (int) data[i + 16];
                c.addVertex(data[i], data[i + 1], data[i + 2]);
                if ((set & COLOR) != 0) {
                    c.setColor((int) data[i + 3], (int) data[i + 4], (int) data[i + 5], (int) data[i + 6]);
                }
                if ((set & UV) != 0) {
                    c.setUv(data[i + 7], data[i + 8]);
                }
                if ((set & UV1) != 0) {
                    c.setUv1((int) data[i + 9], (int) data[i + 10]);
                }
                if ((set & UV2) != 0) {
                    c.setUv2((int) data[i + 11], (int) data[i + 12]);
                }
                if ((set & NORMAL) != 0) {
                    c.setNormal(data[i + 13], data[i + 14], data[i + 15]);
                }
            }
        }

        void clear() {
            count = 0;
            if (data.length > STRIDE * 65536) {
                data = new float[STRIDE * 256];
            }
        }
    }
}
