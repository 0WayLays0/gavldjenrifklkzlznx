package com.waylays.arcanes.client.render.wind;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.RenderHelper;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Drawing shared by the Wind Magic renderers. Wind itself cannot be seen: it is drawn by what it carries and how it
 * moves: lines of wind (thin streaks running along its way, fading at both ends, as an artist draws a gust), a sheen of
 * moving air, curls of air, and the leaves and dust it sweeps along. The lines are lit as they are (wind is drawn, not
 * lit), so it reads by day and by night. Pure geometry: the render lab draws it too.
 */
public final class WindMesh {
    /** A line of wind: white, tapering at both ends, brightest just behind its head (U 0 tail to U 1 head). */
    public static final ResourceLocation STREAK = ArcanesOublies.id("textures/entity/wind/streak.png");
    /** Moving air against the light: faint streaks along U, tiling. */
    public static final ResourceLocation SHEEN = ArcanesOublies.id("textures/entity/wind/sheen.png");
    /** Four curls of air (2 x 2). */
    public static final ResourceLocation WISP = ArcanesOublies.id("textures/entity/wind/wisp.png");
    /** Four leaves (2 x 2): oak, birch, autumn, a blossom petal. */
    public static final ResourceLocation LEAF = ArcanesOublies.id("textures/entity/wind/leaf.png");
    /** A puff of dust. */
    public static final ResourceLocation DUST = ArcanesOublies.id("textures/entity/wind/dust.png");
    /** A ring of blown air. */
    public static final ResourceLocation RING = ArcanesOublies.id("textures/entity/wind/ring.png");

    /** Air (almost white), jade, deep teal, the shadow of a storm, dust. */
    public static final int AIR = 0xEFFFFA;
    public static final int JADE = 0x9EE8CF;
    public static final int TEAL = 0x46B09C;
    public static final int DEEP = 0x1E5C58;
    public static final int STORM = 0x5E6E70;
    public static final int DUSTY = 0xC4BCA6;
    public static final int FULL = LightTexture.FULL_BRIGHT;

    private WindMesh() {}

    public static VertexConsumer lines(MultiBufferSource buffer) {
        return buffer.getBuffer(RenderType.entityTranslucent(STREAK));
    }

    public static VertexConsumer see(MultiBufferSource buffer, ResourceLocation texture) {
        return buffer.getBuffer(RenderType.entityTranslucent(texture));
    }

    public static VertexConsumer leaves(MultiBufferSource buffer) {
        return buffer.getBuffer(RenderType.entityCutoutNoCull(LEAF));
    }

    /**
     * A line of wind through {@code pts} (its tail first, its head last), {@code width} wide, turned to face
     * {@code eye}: its texture runs from tail to head, so it tapers at both ends.
     */
    public static void line(VertexConsumer c, PoseStack.Pose p, Vector3f eye, Vector3f[] pts, float width, int rgb, float alpha) {
        if (alpha <= .01f || pts.length < 2) {
            return;
        }
        // A wide wash of jade under a bright core: it reads against the sky and the ground alike
        strip(c, p, eye, pts, width * 3.2f, WaterMesh.mix(rgb, TEAL, .75f), alpha * .3f);
        strip(c, p, eye, pts, width * 1.5f, rgb, alpha);
    }

    private static void strip(VertexConsumer c, PoseStack.Pose p, Vector3f eye, Vector3f[] pts, float width, int rgb, float alpha) {
        int argb = WaterMesh.argb(rgb, alpha);
        int n = pts.length;
        Vector3f prevSide = null;
        for (int i = 0; i + 1 < n; i++) {
            Vector3f a = pts[i], b = pts[i + 1];
            Vector3f d = new Vector3f(b).sub(a);
            if (d.lengthSquared() < 1.0E-10f) {
                continue;
            }
            Vector3f side = new Vector3f(d).cross(new Vector3f(eye).sub(a));
            if (side.lengthSquared() < 1.0E-10f) {
                side.set(0, 1, 0);
            }
            side.normalize().mul(width * .5f);
            Vector3f sa = prevSide != null ? prevSide : side;
            float u0 = (float) i / (n - 1), u1 = (float) (i + 1) / (n - 1);
            put(c, p, new Vector3f(a).sub(sa), u0, 1, argb);
            put(c, p, new Vector3f(a).add(sa), u0, 0, argb);
            put(c, p, new Vector3f(b).add(side), u1, 0, argb);
            put(c, p, new Vector3f(b).sub(side), u1, 1, argb);
            prevSide = side;
        }
    }

    /**
     * A line of wind running along {@code path}: only its stretch from {@code head - length} to {@code head} (both
     * along the path, 0 to 1) is drawn, so moving {@code head} on makes it race along.
     */
    public static void flow(VertexConsumer c, PoseStack.Pose p, Vector3f eye, Vector3f[] path, float head, float length, float width, int rgb, float alpha) {
        float tail = head - length;
        float h = Math.min(1, head), t = Math.max(0, tail);
        if (h <= t) {
            return;
        }
        // Fades in as its tail leaves the start and out as its head reaches the end
        float shown = (h - t) / length;
        int n = 9;
        Vector3f[] pts = new Vector3f[n];
        for (int i = 0; i < n; i++) {
            pts[i] = along(path, Mth.lerp((float) i / (n - 1), t, h));
        }
        line(c, p, eye, pts, width, rgb, alpha * Mth.clamp(shown * 1.5f, 0, 1));
    }

    /** The point {@code t} (0 to 1) of the way along {@code path}, its points taken as evenly spaced. */
    public static Vector3f along(Vector3f[] path, float t) {
        float f = Mth.clamp(t, 0, 1) * (path.length - 1);
        int i = Math.min(path.length - 2, (int) f);
        return new Vector3f(path[i]).lerp(path[i + 1], f - i);
    }

    /**
     * A helix round the upright through ({@code cx}, {@code cz}): {@code n} points from height {@code y0} to
     * {@code y1}, its radius from {@code r0} to {@code r1}, turning {@code sweep} radians from angle {@code a0}.
     */
    public static Vector3f[] helix(float cx, float cz, float r0, float r1, float a0, float sweep, float y0, float y1, int n) {
        Vector3f[] out = new Vector3f[n];
        for (int i = 0; i < n; i++) {
            float t = (float) i / (n - 1);
            float a = a0 + sweep * t, r = Mth.lerp(t, r0, r1);
            out[i] = new Vector3f(cx + Mth.cos(a) * r, Mth.lerp(t, y0, y1), cz + Mth.sin(a) * r);
        }
        return out;
    }

    /** An arc of {@code n} points round {@code centre} in the plane of {@code u}, {@code v} (unit), radius {@code r}. */
    public static Vector3f[] arc(Vector3f centre, Vector3f u, Vector3f v, float r, float a0, float sweep, int n) {
        Vector3f[] out = new Vector3f[n];
        for (int i = 0; i < n; i++) {
            float a = a0 + sweep * i / (n - 1);
            out[i] = new Vector3f(centre).add(new Vector3f(u).mul(Mth.cos(a) * r)).add(new Vector3f(v).mul(Mth.sin(a) * r));
        }
        return out;
    }

    /** A leaf tumbling at {@code at}, {@code size} across, turned by {@code a} and {@code b}; {@code variant} 0 to 3. */
    public static void leaf(VertexConsumer c, PoseStack.Pose p, Vector3f at, float size, float a, float b, int variant, int light) {
        float h = size * .5f;
        Vector3f x = new Vector3f(Mth.cos(a), Mth.sin(b) * .6f, Mth.sin(a)).normalize().mul(h);
        Vector3f y = new Vector3f(-Mth.sin(a) * Mth.cos(b), Mth.cos(b), Mth.cos(a) * Mth.cos(b) * .3f).normalize().mul(h);
        float u0 = (variant & 1) * .5f, v0 = (variant >> 1 & 1) * .5f;
        Vector3f n = new Vector3f(x).cross(y).normalize();
        RenderHelper.vertex(c, p, at.x - x.x - y.x, at.y - x.y - y.y, at.z - x.z - y.z, u0, v0 + .5f, 0xFFFFFFFF, light, n.x, n.y, n.z);
        RenderHelper.vertex(c, p, at.x + x.x - y.x, at.y + x.y - y.y, at.z + x.z - y.z, u0 + .5f, v0 + .5f, 0xFFFFFFFF, light, n.x, n.y, n.z);
        RenderHelper.vertex(c, p, at.x + x.x + y.x, at.y + x.y + y.y, at.z + x.z + y.z, u0 + .5f, v0, 0xFFFFFFFF, light, n.x, n.y, n.z);
        RenderHelper.vertex(c, p, at.x - x.x + y.x, at.y - x.y + y.y, at.z - x.z + y.z, u0, v0, 0xFFFFFFFF, light, n.x, n.y, n.z);
    }

    /** A flat ring of blown air on the ground (the pose's y = {@code y}), {@code r} across; wound to show from above. */
    public static void ring(VertexConsumer c, PoseStack.Pose p, float y, float r, float spin, int rgb, float alpha) {
        int argb = WaterMesh.argb(rgb, alpha);
        float cs = Mth.cos(spin) * r, sn = Mth.sin(spin) * r;
        put(c, p, new Vector3f(-cs + sn, y, -sn - cs), 0, 0, argb);
        put(c, p, new Vector3f(-cs - sn, y, -sn + cs), 0, 1, argb);
        put(c, p, new Vector3f(cs - sn, y, sn + cs), 1, 1, argb);
        put(c, p, new Vector3f(cs + sn, y, sn - cs), 1, 0, argb);
    }

    private static void put(VertexConsumer c, PoseStack.Pose p, Vector3f at, float u, float v, int argb) {
        RenderHelper.vertex(c, p, at.x, at.y, at.z, u, v, argb, FULL, 0, 1, 0);
    }

    public static float hash(int seed) {
        return WaterMesh.hash(seed);
    }
}
