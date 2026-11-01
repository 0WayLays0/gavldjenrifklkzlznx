package com.waylays.arcanes.client.render.fire;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.RenderHelper;
import com.waylays.arcanes.client.render.water.WaterMesh;
import com.waylays.arcanes.spells.fire.HeatColors;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Drawing shared by the Stellar Fire renderers: living flames ({@link Batch}: meshes that lick and flicker, filaments of
 * fire streaming up them, white-hot at the heart and cooling to each heat's colour), walls and sheets of them, a halo
 * of light added round them, embers, smoke rising off the cooler flames. Pure geometry (the render lab draws it too).
 */
public final class FireMesh {
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
    /** Flame filaments, white on black, tiling (added, tinted per vertex, scrolled up through the flames). */
    public static final ResourceLocation NOISE = ArcanesOublies.id("textures/entity/starfire/flame_noise.png");
    /** A flame's body: white, see-through along the same filaments (drawn see-through, tinted, scrolled up). */
    public static final ResourceLocation BODY = ArcanesOublies.id("textures/entity/starfire/flame_body.png");
    /** An ember: a hot point in a soft glow, white on black (added). */
    public static final ResourceLocation EMBER = ArcanesOublies.id("textures/entity/starfire/ember.png");

    private static final int COLS = 4, ROWS = 8;

    private FireMesh() {}

    public static VertexConsumer light(MultiBufferSource buffer, ResourceLocation texture) {
        return buffer.getBuffer(RenderType.eyes(texture));
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

    // ------------------------------------------------------------------------------------------------ living flames

    /**
     * Flames to draw, gathered then drawn together ({@link #flush}): each a living tongue of fire, a strip of mesh
     * turned about its own axis to face the eye, round at its foot and tapering to a flickering tip, licking from side
     * to side as it rises, its filaments streaming up it. Each is drawn twice: its body, see-through, for substance in
     * daylight; then its light, added, white-hot at its foot and heart, cooling to the heat's tip colour as it rises and
     * fading out at its edges and tip.
     */
    public static final class Batch {
        private final WaterMesh.View view;
        private final int tier;
        private final float age;
        private final int light;
        private final java.util.List<float[]> licks = new java.util.ArrayList<>();

        public Batch(WaterMesh.View view, int tier, float age, int light) {
            this.view = view;
            this.tier = HeatColors.clamp(tier);
            this.age = age;
            this.light = light;
        }

        /** An upright flame at (x, y, z), {@code w} wide, {@code h} tall, its tip pushed {@code lean} blocks sideways; {@code alpha} 0 to 1. */
        public void lick(float x, float y, float z, float w, float h, float lean, int seed, float alpha) {
            if (alpha > .01f && h > .01f) {
                licks.add(new float[] {x, y, z, 0, 1, 0, w, h, alpha, seed, lean});
            }
        }

        /** A flame streaming along {@code dir} (unit) from {@code at}. */
        public void lickAlong(Vector3f at, Vector3f dir, float w, float h, int seed, float alpha) {
            if (alpha > .01f && h > .01f) {
                licks.add(new float[] {at.x, at.y, at.z, dir.x, dir.y, dir.z, w, h, alpha, seed, 0});
            }
        }

        /**
         * A wall of flames standing along {@code base} (a path), {@code height[i]} tall at each point, a flame every
         * {@code spacing} blocks of it, each a little different; {@code alpha[i]} how strong.
         */
        public void curtain(Vector3f[] base, float[] height, float[] alpha, float spacing, int seed) {
            float along = 0, next = 0;
            int k = 0;
            for (int i = 0; i + 1 < base.length; i++) {
                float len = base[i].distance(base[i + 1]);
                while (next <= along + len) {
                    float t = len < 1.0E-5f ? 0 : (next - along) / len;
                    Vector3f at = new Vector3f(base[i]).lerp(base[i + 1], t);
                    float h = Mth.lerp(t, height[i], height[i + 1]) * (.75f + .5f * WaterMesh.hash(seed + k * 7));
                    float a = Mth.lerp(t, alpha[i], alpha[i + 1]);
                    lick(at.x, at.y, at.z, spacing * 2.6f, h, 0, seed + k * 13, a);
                    next += spacing * (.8f + .4f * WaterMesh.hash(seed + k * 3));
                    k++;
                }
                along += len;
            }
        }

        /** Flames along {@code base}, each streaming out along {@code out[i]} (its length the flame's height). */
        public void sheet(Vector3f[] base, Vector3f[] out, float[] alpha, float spacing, int seed) {
            float along = 0, next = 0;
            int k = 0;
            for (int i = 0; i + 1 < base.length; i++) {
                float len = base[i].distance(base[i + 1]);
                while (next <= along + len) {
                    float t = len < 1.0E-5f ? 0 : (next - along) / len;
                    Vector3f at = new Vector3f(base[i]).lerp(base[i + 1], t);
                    Vector3f o = new Vector3f(out[i]).lerp(out[i + 1], t);
                    float h = o.length() * (.75f + .5f * WaterMesh.hash(seed + k * 7));
                    if (h > .02f) {
                        lickAlong(at, o.normalize(), spacing * 2.4f, h, seed + k * 13, Mth.lerp(t, alpha[i], alpha[i + 1]));
                    }
                    next += spacing * (.8f + .4f * WaterMesh.hash(seed + k * 3));
                    k++;
                }
                along += len;
            }
        }

        public boolean isEmpty() {
            return licks.isEmpty();
        }

        /** Draws them all: their bodies first, then their light (call it once the model has drawn everything else). */
        public void flush(MultiBufferSource buffer, PoseStack poseStack) {
            if (licks.isEmpty()) {
                return;
            }
            VertexConsumer body = buffer.getBuffer(RenderType.entityTranslucent(BODY));
            PoseStack.Pose p = poseStack.last();
            for (float[] l : licks) {
                mesh(body, p, l, false);
            }
            VertexConsumer glow = buffer.getBuffer(RenderType.eyes(NOISE));
            p = poseStack.last();
            for (float[] l : licks) {
                mesh(glow, p, l, true);
            }
        }

        private void mesh(VertexConsumer c, PoseStack.Pose p, float[] l, boolean lit) {
            Vector3f base = new Vector3f(l[0], l[1], l[2]);
            Vector3f axis = new Vector3f(l[3], l[4], l[5]);
            float w = l[6], h = l[7], alpha = l[8], lean = l[10];
            int seed = (int) l[9];
            Vector3f side = new Vector3f(axis).cross(new Vector3f(view.eye()).sub(base));
            if (side.lengthSquared() < 1.0E-8f) {
                side.set(1, 0, 0);
            }
            side.normalize();
            float s0 = WaterMesh.hash(seed) * 9;
            // Its height flickers, its tip licking from side to side as it rises
            float tall = h * (.82f + .36f * WaterMesh.noise(age * .13f, s0, 1.7f));
            Vector3f[][] pts = new Vector3f[ROWS][COLS];
            float[] tv = new float[ROWS];
            float scroll = age * .09f * .275f / Math.max(.25f, w) + WaterMesh.hash(seed + 5);
            for (int r = 0; r < ROWS; r++) {
                float v = (float) r / (ROWS - 1);
                float shape = v < .18f ? .5f + .5f * Mth.sin(v / .18f * Mth.HALF_PI) : (float) Math.pow(1 - (v - .18f) / .82f, .75f);
                float sway = (WaterMesh.noise(age * .11f - v * 1.6f, s0, 3.1f) * 2 - 1) * .45f * (float) Math.pow(v, 1.4f)
                        + (WaterMesh.noise(age * .3f - v * 4, s0 + 2, 5.3f) * 2 - 1) * .12f * v;
                float half = w * .5f * shape * (.9f + .25f * WaterMesh.noise(age * .2f - v * 3, s0 + 4, .7f));
                Vector3f centre = new Vector3f(axis).mul(v * tall).add(base).add(new Vector3f(side).mul(sway * w + lean * v * v));
                for (int k = 0; k < COLS; k++) {
                    float u = (float) k / (COLS - 1) * 2 - 1;
                    pts[r][k] = new Vector3f(side).mul(u * half).add(centre);
                }
                tv[r] = v * tall * .275f / Math.max(.25f, w) - scroll;
            }
            Vector3f n = WaterMesh.UP;
            for (int r = 0; r + 1 < ROWS; r++) {
                for (int k = 0; k + 1 < COLS; k++) {
                    vertex(c, p, pts[r][k], k, r, tv[r], alpha, lit, n, s0);
                    vertex(c, p, pts[r][k + 1], k + 1, r, tv[r], alpha, lit, n, s0);
                    vertex(c, p, pts[r + 1][k + 1], k + 1, r + 1, tv[r + 1], alpha, lit, n, s0);
                    vertex(c, p, pts[r + 1][k], k, r + 1, tv[r + 1], alpha, lit, n, s0);
                }
            }
        }

        private void vertex(VertexConsumer c, PoseStack.Pose p, Vector3f at, int k, int r, float tv, float alpha, boolean lit, Vector3f n, float s0) {
            float u = (float) k / (COLS - 1) * 2 - 1;
            float v = (float) r / (ROWS - 1);
            float edge = 1 - u * u;
            float top = 1 - v * v;
            // How hot it is here: white at its foot and heart, cooling as it rises and out to its edges
            float heat = Mth.clamp((float) Math.pow(1 - v, 1.15f) * (1 - .6f * u * u) * (.92f + .16f * WaterMesh.noise(age * .25f, s0 + r, k)), 0, 1);
            float tu = (u * .5f + .5f) * .55f + s0 * .1f;
            int argb;
            if (lit) {
                float k2 = alpha * edge * top * Mth.clamp(.35f + v * 4, 0, 1);
                argb = glow(ramp(heat), k2 * (.55f + .6f * heat));
            } else {
                int col = WaterMesh.mix(tip(tier), body(tier), Mth.clamp(heat * 1.4f, 0, 1));
                argb = WaterMesh.argb(col, alpha * .6f * edge * (1 - (float) Math.pow(v, 1.6f)));
            }
            RenderHelper.vertex(c, p, at.x, at.y, at.z, tu, 1 - tv, argb, lit ? LightTexture.FULL_BRIGHT : light, n.x, n.y, n.z);
        }

        /** The colour of the flame at a heat from 0 (its cool tip) to 1 (its white heart). */
        private int ramp(float t) {
            if (t < .35f) {
                return WaterMesh.mix(tip(tier), body(tier), t / .35f);
            }
            if (t < .75f) {
                return WaterMesh.mix(body(tier), WaterMesh.mix(body(tier), core(tier), .5f), (t - .35f) / .4f);
            }
            return WaterMesh.mix(WaterMesh.mix(body(tier), core(tier), .5f), core(tier), (t - .75f) / .25f);
        }

        private static int glow(int rgb, float k) {
            return WaterMesh.glow(rgb, k);
        }
    }

    /** Embers flying from a fire: hot points streaking along their way (added). */
    public static void ember(VertexConsumer c, PoseStack.Pose p, Vector3f eye, Vector3f head, Vector3f tail, float size, int rgb, float k, int light) {
        WaterMesh.streak(c, p, eye, head, tail, size, WaterMesh.glow(rgb, k), light);
    }

    /** Quaternion turning the pose to face the eye (for flat things that face the viewer). */
    public static Quaternionf facing(WaterMesh.View view) {
        return new Quaternionf(view.camera());
    }
}
