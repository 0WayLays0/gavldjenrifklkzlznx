package com.waylays.arcanes.client.render.fire;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.RenderHelper;
import com.waylays.arcanes.client.render.water.WaterMesh;
import com.waylays.arcanes.spells.fire.HeatColors;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Drawing shared by the Stellar Fire renderers: flames as upright tongues of fire (four frames of each, in each heat's
 * colours, flickering), curtains of fire for walls and trails, a halo of light added round them, embers flying as
 * sparks, smoke rising off the cooler flames. Pure geometry (the render lab draws it too); the shapes themselves come
 * from {@link WaterMesh}'s helpers.
 */
public final class FireMesh {
    private static final ResourceLocation[] FLAME = new ResourceLocation[HeatColors.TIERS];
    private static final ResourceLocation[] WALL = new ResourceLocation[HeatColors.TIERS];
    /** A soft round light, white on black (added, tinted per vertex). */
    public static final ResourceLocation GLOW = ArcanesOublies.id("textures/entity/starfire/glow.png");
    /** A spark, a bright line on black (added, tinted per vertex), V from its head to its tail. */
    public static final ResourceLocation SPARK = ArcanesOublies.id("textures/entity/starfire/spark.png");
    /** A puff of dark smoke. */
    public static final ResourceLocation SMOKE = ArcanesOublies.id("textures/entity/starfire/smoke.png");
    /** A band of light across U (bright in its middle, black at its edges), even along V: for slashes and beams (added). */
    public static final ResourceLocation BEAM = ArcanesOublies.id("textures/entity/starfire/beam.png");
    /** A ring of light, white on black (added, tinted per vertex). */
    public static final ResourceLocation RING = ArcanesOublies.id("textures/entity/starfire/ring.png");

    static {
        for (int t = 0; t < HeatColors.TIERS; t++) {
            FLAME[t] = ArcanesOublies.id("textures/entity/starfire/flame_" + t + ".png");
            WALL[t] = ArcanesOublies.id("textures/entity/starfire/wall_" + t + ".png");
        }
    }

    private FireMesh() {}

    /** Four tongues of fire in a heat's colours (2 x 2), each with its white heart, its body and its cooling tips. */
    public static ResourceLocation flame(int tier) {
        return FLAME[HeatColors.clamp(tier)];
    }

    /** A curtain of fire in a heat's colours: flames along U (it tiles), rising up V from its foot (V 1) to its tips (V 0). */
    public static ResourceLocation wall(int tier) {
        return WALL[HeatColors.clamp(tier)];
    }

    public static VertexConsumer flames(MultiBufferSource buffer, int tier) {
        return buffer.getBuffer(RenderType.entityTranslucent(flame(tier)));
    }

    public static VertexConsumer walls(MultiBufferSource buffer, int tier) {
        return buffer.getBuffer(RenderType.entityTranslucent(wall(tier)));
    }

    public static VertexConsumer light(MultiBufferSource buffer, ResourceLocation texture) {
        return buffer.getBuffer(RenderType.eyes(texture));
    }

    /**
     * A tongue of fire standing at (x, y, z), {@code w} wide and {@code h} tall, turned about the upright to face the
     * eye (so it never leans), leaning by {@code lean} (blocks its tip is pushed sideways); frame {@code frame} (0 to 3).
     */
    public static void tongue(VertexConsumer c, PoseStack.Pose p, Vector3f eye, float x, float y, float z, float w, float h, float lean, int frame, int argb,
                              int light) {
        float dx = eye.x - x, dz = eye.z - z;
        float len = Mth.sqrt(dx * dx + dz * dz);
        float sx = len < 1.0E-4f ? 1 : -dz / len, sz = len < 1.0E-4f ? 0 : dx / len;
        float hw = w * .5f;
        float u0 = (frame & 1) * .5f, v0 = (frame >> 1 & 1) * .5f, u1 = u0 + .5f, v1 = v0 + .5f;
        float lx = sx * lean, lz = sz * lean;
        Vector3f n = WaterMesh.UP;
        RenderHelper.vertex(c, p, x - sx * hw, y, z - sz * hw, u0, v1, argb, light, n.x, n.y, n.z);
        RenderHelper.vertex(c, p, x + sx * hw, y, z + sz * hw, u1, v1, argb, light, n.x, n.y, n.z);
        RenderHelper.vertex(c, p, x + sx * hw + lx, y + h, z + sz * hw + lz, u1, v0, argb, light, n.x, n.y, n.z);
        RenderHelper.vertex(c, p, x - sx * hw + lx, y + h, z - sz * hw + lz, u0, v0, argb, light, n.x, n.y, n.z);
    }

    /**
     * A tongue of fire streaming along {@code dir} (unit): its foot at {@code at}, its tip {@code h} along, {@code w}
     * wide, turned about its own axis to face the eye (for flames carried by a stream: a breath, a slash).
     */
    public static void tongueAlong(VertexConsumer c, PoseStack.Pose p, Vector3f eye, Vector3f at, Vector3f dir, float w, float h, int frame, int argb, int light) {
        Vector3f tip = new Vector3f(dir).mul(h).add(at);
        Vector3f side = new Vector3f(dir).cross(new Vector3f(eye).sub(at));
        if (side.lengthSquared() < 1.0E-10f) {
            side.set(1, 0, 0);
        }
        side.normalize().mul(w * .5f);
        float u0 = (frame & 1) * .5f, v0 = (frame >> 1 & 1) * .5f, u1 = u0 + .5f, v1 = v0 + .5f;
        Vector3f n = WaterMesh.UP;
        RenderHelper.vertex(c, p, at.x - side.x, at.y - side.y, at.z - side.z, u0, v1, argb, light, n.x, n.y, n.z);
        RenderHelper.vertex(c, p, at.x + side.x, at.y + side.y, at.z + side.z, u1, v1, argb, light, n.x, n.y, n.z);
        RenderHelper.vertex(c, p, tip.x + side.x, tip.y + side.y, tip.z + side.z, u1, v0, argb, light, n.x, n.y, n.z);
        RenderHelper.vertex(c, p, tip.x - side.x, tip.y - side.y, tip.z - side.z, u0, v0, argb, light, n.x, n.y, n.z);
    }

    /**
     * A curtain of fire along {@code base}, its flames streaming out along {@code out[i]} from each point (its tips at
     * {@code base[i] + out[i]}), tiling along it every {@code tile} blocks.
     */
    public static void sheet(VertexConsumer c, PoseStack.Pose p, Vector3f[] base, Vector3f[] out, int[] argb, float tile, float shift, int light) {
        float along = 0;
        Vector3f n = WaterMesh.UP;
        for (int i = 0; i + 1 < base.length; i++) {
            Vector3f a = base[i], b = base[i + 1];
            Vector3f at = new Vector3f(a).add(out[i]), bt = new Vector3f(b).add(out[i + 1]);
            float next = along + a.distance(b);
            float u0 = along / tile + shift, u1 = next / tile + shift;
            RenderHelper.vertex(c, p, a.x, a.y, a.z, u0, 1, argb[i], light, n.x, n.y, n.z);
            RenderHelper.vertex(c, p, b.x, b.y, b.z, u1, 1, argb[i + 1], light, n.x, n.y, n.z);
            RenderHelper.vertex(c, p, bt.x, bt.y, bt.z, u1, 0, argb[i + 1], light, n.x, n.y, n.z);
            RenderHelper.vertex(c, p, at.x, at.y, at.z, u0, 0, argb[i], light, n.x, n.y, n.z);
            along = next;
        }
    }

    /** A flickering frame for a flame, its own rhythm. */
    public static int frame(float age, int seed) {
        return ((int) (age * .35f) + seed) & 3;
    }

    /** A halo of {@code rgb} light {@code size} across at (x, y, z), {@code k} (0 to 1) how bright. */
    public static void halo(VertexConsumer c, PoseStack poseStack, WaterMesh.View view, float x, float y, float z, float size, int rgb, float k, int light) {
        WaterMesh.puff(c, poseStack, view, x, y, z, size, 0, -1, WaterMesh.glow(rgb, k), light);
    }

    /** A flat ring of light on the ground (or wherever the pose lies), {@code r} across. */
    public static void groundRing(VertexConsumer c, PoseStack.Pose p, float y, float r, int rgb, float k, int light) {
        int argb = WaterMesh.glow(rgb, k);
        Vector3f n = WaterMesh.UP;
        RenderHelper.vertex(c, p, -r, y, -r, 0, 0, argb, light, n.x, n.y, n.z);
        RenderHelper.vertex(c, p, -r, y, r, 0, 1, argb, light, n.x, n.y, n.z);
        RenderHelper.vertex(c, p, r, y, r, 1, 1, argb, light, n.x, n.y, n.z);
        RenderHelper.vertex(c, p, r, y, -r, 1, 0, argb, light, n.x, n.y, n.z);
    }

    /**
     * A curtain of fire standing along {@code base} (a path), {@code height[i]} tall at each point, its flames tiling
     * along it every {@code tile} blocks (with the wall texture of the heat bound).
     */
    public static void curtain(VertexConsumer c, PoseStack.Pose p, Vector3f[] base, float[] height, int[] argb, float tile, float shift, int light) {
        float along = 0;
        Vector3f n = WaterMesh.UP;
        for (int i = 0; i + 1 < base.length; i++) {
            Vector3f a = base[i], b = base[i + 1];
            float next = along + a.distance(b);
            float u0 = along / tile + shift, u1 = next / tile + shift;
            RenderHelper.vertex(c, p, a.x, a.y, a.z, u0, 1, argb[i], light, n.x, n.y, n.z);
            RenderHelper.vertex(c, p, b.x, b.y, b.z, u1, 1, argb[i + 1], light, n.x, n.y, n.z);
            RenderHelper.vertex(c, p, b.x, b.y + height[i + 1], b.z, u1, 0, argb[i + 1], light, n.x, n.y, n.z);
            RenderHelper.vertex(c, p, a.x, a.y + height[i], a.z, u0, 0, argb[i], light, n.x, n.y, n.z);
            along = next;
        }
    }

    /** The colour of a heat: its heart, its body, its tips, its light. */
    public static int core(int tier) {
        return HeatColors.CORE[HeatColors.clamp(tier)];
    }

    public static int body(int tier) {
        return HeatColors.BODY[HeatColors.clamp(tier)];
    }

    public static int tip(int tier) {
        return HeatColors.TIP[HeatColors.clamp(tier)];
    }

    public static int glowOf(int tier) {
        return HeatColors.GLOW[HeatColors.clamp(tier)];
    }

    /** Quaternion turning the pose to face the eye (for flat things that face the viewer). */
    public static Quaternionf facing(WaterMesh.View view) {
        return new Quaternionf(view.camera());
    }
}
