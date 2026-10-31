package renderlab;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Records every quad drawn through it, with its render type, so raster.py can draw them. */
public class Recorder implements MultiBufferSource {
    private final List<String> lines = new ArrayList<>();
    private Consumer current;

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        if (current != null) {
            current.closed = true;
        }
        String tex = type.texture == null ? "-" : type.texture.getNamespace() + "/" + type.texture.getPath();
        lines.add("B " + type.blend + " " + (type.cull ? 1 : 0) + " " + tex);
        current = new Consumer();
        return current;
    }

    /** Marks a reference box (a figure for scale): opaque, coloured. */
    public void box(double x0, double y0, double z0, double x1, double y1, double z1, int rgb) {
        lines.add(String.format(java.util.Locale.ROOT, "X %.3f %.3f %.3f %.3f %.3f %.3f %d", x0, y0, z0, x1, y1, z1, rgb));
    }

    public void save(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(path))) {
            for (String l : lines) {
                w.println(l);
            }
        }
    }

    private final class Consumer implements VertexConsumer {
        boolean closed;
        float x, y, z, u, v, nx, ny = 1, nz;
        int r = 255, g = 255, b = 255, a = 255, light = 0xF000F0;
        boolean open;

        private void flush() {
            if (open) {
                if (closed) {
                    throw new IllegalStateException("vertex written to a buffer after another getBuffer call");
                }
                lines.add(String.format(java.util.Locale.ROOT, "V %.4f %.4f %.4f %.4f %.4f %d %d %d %d %d %.3f %.3f %.3f",
                        x, y, z, u, v, r, g, b, a, light, nx, ny, nz));
            }
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            flushPending();
            this.x = x;
            this.y = y;
            this.z = z;
            open = true;
            return this;
        }

        private void flushPending() {
            flush();
            open = false;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            this.u = u;
            this.v = v;
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            this.light = u | v << 16;
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            nx = x;
            ny = y;
            nz = z;
            // The normal is the last thing set on a vertex: it is complete
            flushPending();
            return this;
        }
    }
}
