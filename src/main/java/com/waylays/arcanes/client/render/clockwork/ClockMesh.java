package com.waylays.arcanes.client.render.clockwork;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.boss.Sculpt;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * What Clockwork Magic's machines are made of: gears of brushed brass, copper and blued steel, riveted brass plates, red
 * enamel, cores of blue energy behind glass; steam puffing from their vents, sparks, and blue arcs leaping between them.
 * Each piece takes the buffer source and fetches its own consumer, so pieces of different metals can be drawn one after
 * the other in any order.
 */
public final class ClockMesh {
    public static final ResourceLocation BRASS = ArcanesOublies.id("textures/entity/clockwork/brass.png");
    public static final ResourceLocation COPPER = ArcanesOublies.id("textures/entity/clockwork/copper.png");
    public static final ResourceLocation STEEL = ArcanesOublies.id("textures/entity/clockwork/steel.png");
    public static final ResourceLocation PLATE = ArcanesOublies.id("textures/entity/clockwork/plate.png");
    public static final ResourceLocation ENAMEL = ArcanesOublies.id("textures/entity/clockwork/enamel.png");
    public static final ResourceLocation CORE = ArcanesOublies.id("textures/entity/clockwork/core.png");
    public static final ResourceLocation CANVAS = ArcanesOublies.id("textures/entity/clockwork/canvas.png");
    public static final ResourceLocation SLEEPER = ArcanesOublies.id("textures/entity/clockwork/sleeper.png");

    public static final int WHITE = 0xFFFFFFFF;
    /** The rim of a piece: its metal a shade darker. */
    public static final int RIM = 0xFFB8A890;
    public static final int BLUE = 0x5ACCFF;
    public static final int BLUE_WHITE = 0xE2FAFF;
    public static final int EMBER = 0xFF963C;
    public static final int STEAM = 0xEEF2F4;

    private ClockMesh() {}

    // ---------------------------------------------------------------- gears

    /**
     * A gear in the pose's XY plane turning round Z, {@code r} to the tips of its {@code teeth}, {@code thick} deep: its
     * teeth round a rim, {@code spokes} spokes to a hub (none: a solid web), of {@code metal}.
     */
    public static void gear(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation metal, float r, int teeth, float thick, int spokes, int argb,
                            int light) {
        VertexConsumer c = BossMesh.solid(buffer, metal);
        PoseStack.Pose p = poseStack.last();
        float tooth = Math.max(.06f, r * .15f), root = r - tooth;
        float w = Mth.TWO_PI / teeth;
        int rim = BossMesh.blend(argb, 0xFF000000 | RIM, .5f);
        for (int t = 0; t < teeth; t++) {
            float a = t * w;
            float[] outline = {
                    Mth.cos(a - w * .27f) * (root - .03f), Mth.sin(a - w * .27f) * (root - .03f),
                    Mth.cos(a - w * .15f) * r, Mth.sin(a - w * .15f) * r,
                    Mth.cos(a + w * .15f) * r, Mth.sin(a + w * .15f) * r,
                    Mth.cos(a + w * .27f) * (root - .03f), Mth.sin(a + w * .27f) * (root - .03f)};
            Sculpt.slab(c, p, outline, thick * .9f, Math.min(.03f, thick * .2f), .5f, argb, rim, light);
        }
        float h = thick * .5f;
        float inner = spokes > 0 ? root - Math.max(.08f, r * .16f) : 0;
        float hub = Math.max(.06f, r * .2f);
        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotationX(Mth.HALF_PI));
        PoseStack.Pose q = poseStack.last();
        int segs = Math.max(24, teeth * 2);
        if (spokes > 0) {
            Sculpt.lathe(c, q, new float[] {inner, -h, root, -h, root, h, inner, h, inner, -h}, segs, 2, .5f, argb, argb, light);
            // The hub, standing proud of the rim
            Sculpt.lathe(c, q, new float[] {0, -h * 1.5f, hub, -h * 1.5f, hub, h * 1.5f, 0, h * 1.5f}, 16, 1, .5f, argb, argb, light);
        } else {
            // A solid web, a boss raised at its middle
            Sculpt.lathe(c, q, new float[] {0, -h * .7f, root, -h * .7f, root, -h, root, h, root, h * .7f, hub * 1.6f, h * .7f, hub, h * 1.5f, 0, h * 1.5f}, segs, 2,
                    .5f, argb, argb, light);
        }
        poseStack.popPose();
        for (int k = 0; k < spokes; k++) {
            float a = k * Mth.TWO_PI / spokes;
            poseStack.pushPose();
            poseStack.mulPose(new Quaternionf().rotationZ(a));
            poseStack.translate((inner + hub) * .5f, 0, 0);
            Sculpt.block(c, poseStack.last(), inner - hub + .04f, Math.max(.05f, r * .13f), thick * .7f, .015f, .5f, argb, light);
            poseStack.popPose();
        }
    }

    /** A gear turning at {@code at}, its axle along {@code axis}, turned {@code turn} radians. */
    public static void gearAt(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation metal, Vector3f at, Vector3f axis, float turn, float r, int teeth,
                              float thick, int spokes, int argb, int light) {
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        poseStack.mulPose(toward(axis));
        poseStack.mulPose(new Quaternionf().rotationZ(turn));
        gear(poseStack, buffer, metal, r, teeth, thick, spokes, argb, light);
        poseStack.popPose();
    }

    /** The turn that takes local +Z to {@code dir}. */
    public static Quaternionf toward(Vector3f dir) {
        Vector3f d = new Vector3f(dir);
        if (d.lengthSquared() < 1.0E-8f) {
            return new Quaternionf();
        }
        return new Quaternionf().rotationTo(new Vector3f(0, 0, 1), d.normalize());
    }

    /** How far a gear of {@code teeth} turns while one of {@code drive} teeth turns {@code a}, meshing with it (the other way). */
    public static float mesh(float a, int drive, int teeth) {
        return -a * drive / teeth;
    }

    /**
     * A spoked wheel in the pose's XY plane turning round Z, {@code r} to the tread of its steel tyre (a flange inside it):
     * {@code spokes} spokes of {@code body} to a brass hub.
     */
    public static void wheel(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation body, float r, float thick, int spokes, int argb, int light) {
        float h = thick * .5f;
        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotationX(Mth.HALF_PI));
        PoseStack.Pose q = poseStack.last();
        Sculpt.lathe(BossMesh.solid(buffer, STEEL), q, new float[] {r * .8f, -h, r, -h, r, h * .3f, r * 1.1f, h * .3f, r * 1.1f, h, r * .8f, h, r * .8f, -h}, 32, 2, .5f,
                WHITE, WHITE, light);
        Sculpt.lathe(BossMesh.solid(buffer, BRASS), q, new float[] {0, -h * 1.6f, r * .2f, -h * 1.6f, r * .2f, h * 1.6f, 0, h * 1.6f}, 12, 1, .5f, WHITE, WHITE, light);
        poseStack.popPose();
        for (int k = 0; k < spokes; k++) {
            poseStack.pushPose();
            poseStack.mulPose(new Quaternionf().rotationZ(k * Mth.TWO_PI / spokes));
            poseStack.translate(r * .5f, 0, 0);
            Sculpt.block(BossMesh.solid(buffer, body), poseStack.last(), r * .64f, Math.max(.05f, r * .12f), thick * .6f, .01f, .5f, argb, light);
            poseStack.popPose();
        }
    }

    /** {@code poly} (pairs of x, y) wound counter-clockwise, as {@link Sculpt#slab} wants it. */
    public static float[] ccw(float[] poly) {
        float area = 0;
        int m = poly.length / 2;
        for (int i = 0; i < m; i++) {
            int j = (i + 1) % m;
            area += poly[2 * i] * poly[2 * j + 1] - poly[2 * j] * poly[2 * i + 1];
        }
        if (area >= 0) {
            return poly;
        }
        float[] r = new float[poly.length];
        for (int i = 0; i < m; i++) {
            r[2 * i] = poly[2 * (m - 1 - i)];
            r[2 * i + 1] = poly[2 * (m - 1 - i) + 1];
        }
        return r;
    }

    // ---------------------------------------------------------------- turned pieces

    /** A cylinder along Y from {@code y0} to {@code y1}, closed at both ends. */
    public static void cylinder(PoseStack.Pose p, MultiBufferSource buffer, ResourceLocation metal, float r, float y0, float y1, int segs, int argb, int light) {
        Sculpt.lathe(BossMesh.solid(buffer, metal), p, new float[] {0, y0, r, y0, r, y1, 0, y1}, segs, 1, .5f, argb, argb, light);
    }

    /** Turned round Y from the profile {@code prof} ({r, y} pairs, bottom to top). */
    public static void turned(PoseStack.Pose p, MultiBufferSource buffer, ResourceLocation metal, float[] prof, int segs, int argb, int light) {
        Sculpt.lathe(BossMesh.solid(buffer, metal), p, prof, segs, 1, .5f, argb, argb, light);
    }

    /** A rod of {@code metal} from {@code a} to {@code b}, {@code r} round. */
    public static void rod(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation metal, Vector3f a, Vector3f b, float r, int argb, int light) {
        Vector3f d = new Vector3f(b).sub(a);
        float len = d.length();
        if (len < 1.0E-4f) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(a.x, a.y, a.z);
        poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 1, 0), d.normalize()));
        cylinder(poseStack.last(), buffer, metal, r, 0, len, 8, argb, light);
        poseStack.popPose();
    }

    /** A box of {@code metal} {@code w} x {@code h} x {@code d} round the pose's origin, its edges bevelled. */
    public static void box(PoseStack.Pose p, MultiBufferSource buffer, ResourceLocation metal, float w, float h, float d, int argb, int light) {
        Sculpt.block(BossMesh.solid(buffer, metal), p, w, h, d, Math.min(.04f, Math.min(w, Math.min(h, d)) * .2f), .5f, argb, light);
    }

    /** A box at {@code at}, {@code w} x {@code h} x {@code d}. */
    public static void boxAt(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation metal, float x, float y, float z, float w, float h, float d, int argb,
                             int light) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        box(poseStack.last(), buffer, metal, w, h, d, argb, light);
        poseStack.popPose();
    }

    /** A coiled spring along Y from 0 to {@code height}, {@code r} round, {@code turns} turns of wire {@code wire} thick. */
    public static void spring(PoseStack.Pose p, MultiBufferSource buffer, ResourceLocation metal, float r, float height, float turns, float wire, float spin,
                              int argb, int light) {
        int n = Math.max(8, (int) (turns * 14));
        Vector3f[] path = new Vector3f[n + 1];
        for (int i = 0; i <= n; i++) {
            float t = (float) i / n, a = spin + t * turns * Mth.TWO_PI;
            path[i] = new Vector3f(Mth.cos(a) * r, t * height, Mth.sin(a) * r);
        }
        Sculpt.sweep(BossMesh.solid(buffer, metal), p, path, Sculpt.fill(wire, n + 1), Sculpt.fill(wire, n + 1), Sculpt.circle(6), false,
                new Vector3f(0, 1, 0), .5f, Sculpt.fill(argb, n + 1), true, light);
    }

    /** A pipe of {@code metal} along {@code path}, {@code r} round. */
    public static void pipe(PoseStack.Pose p, MultiBufferSource buffer, ResourceLocation metal, Vector3f[] path, float r, int argb, int light) {
        int n = path.length;
        Sculpt.sweep(BossMesh.solid(buffer, metal), p, path, Sculpt.fill(r, n), Sculpt.fill(r, n), Sculpt.circle(8), false, new Vector3f(0, 1, 0), .5f,
                Sculpt.fill(argb, n), true, light);
    }

    // ---------------------------------------------------------------- energy, steam, sparks

    /** A core of blue energy at {@code at}, {@code r} round, swirling as {@code time} goes, with its glow; {@code alpha} bright. */
    public static void core(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f at, float r, float time, float alpha, int light) {
        if (alpha <= .01f) {
            return;
        }
        float pulse = .85f + .15f * Mth.sin(time * .35f);
        BossMesh.sphere(BossMesh.light(buffer, CORE), poseStack.last(), at, r, 8, 12, time * .01f, 0, 1, BossMesh.glow(0xFFFFFF, alpha * pulse), light);
        VertexConsumer g = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(g, poseStack, view, at, r * 5 * pulse, 0, -1, BossMesh.glow(BLUE, alpha * .7f), light);
        RitualMesh.billboard(g, poseStack, view, at, r * 2.2f, 0, -1, BossMesh.glow(BLUE_WHITE, alpha * .8f), light);
    }

    /** A soft glow of {@code rgb} at {@code at}. */
    public static void glow(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f at, float size, int rgb, float alpha, int light) {
        if (alpha > .01f) {
            RitualMesh.billboard(BossMesh.light(buffer, BossMesh.GLOW), poseStack, view, at, size, 0, -1, BossMesh.glow(rgb, alpha), light);
        }
    }

    /**
     * Steam puffing from {@code at} along {@code dir}: {@code count} puffs, each rising, swelling and thinning out as it
     * goes {@code reach} along, at {@code rate} of its way a tick; {@code alpha} thick.
     */
    public static void steam(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f at, Vector3f dir, float reach, float size, int count,
                             float time, float rate, float alpha, int seed, int light) {
        smoke(poseStack, buffer, view, at, dir, reach, size, count, time, rate, alpha, STEAM, seed, light);
    }

    /** Like {@link #steam}, of {@code rgb}: soot from a smokestack, smoke from a blast. */
    public static void smoke(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f at, Vector3f dir, float reach, float size, int count,
                             float time, float rate, float alpha, int rgb, int seed, int light) {
        if (alpha <= .01f) {
            return;
        }
        VertexConsumer c = BossMesh.see(buffer, rgb == STEAM ? BossMesh.CLOUD : BossMesh.SMOKE);
        Vector3f d = new Vector3f(dir);
        if (d.lengthSquared() > 1.0E-6f) {
            d.normalize();
        }
        for (int k = 0; k < count; k++) {
            float t = (time * rate + (float) k / count + BossMesh.hash(seed + k) * .3f) % 1;
            float drift = BossMesh.hash(seed + k * 3) * 2 - 1;
            Vector3f s = new Vector3f(d).mul(reach * t).add(at).add(drift * reach * .15f * t, reach * .25f * t * t, (BossMesh.hash(seed + k * 5) * 2 - 1) * reach * .15f * t);
            float a = alpha * Mth.sin(Mth.PI * Math.min(1, t * 1.4f + .05f)) * (1 - t * .5f);
            RitualMesh.billboard(c, poseStack, view, s, size * (.5f + 1.6f * t), seed + k + time * .02f * (k % 2 == 0 ? 1 : -1), -1, BossMesh.argb(rgb, a), light);
        }
    }

    /** Sparks flung from {@code at}: {@code count} of them, each flying out and falling over its short life. */
    public static void sparks(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f at, float spread, int count, float time, float alpha,
                              int seed, int light) {
        if (alpha <= .01f) {
            return;
        }
        VertexConsumer c = BossMesh.light(buffer, BossMesh.EMBER);
        for (int k = 0; k < count; k++) {
            float t = (time * .08f + BossMesh.hash(seed + k * 7)) % 1;
            float a = BossMesh.hash(seed + k * 13) * Mth.TWO_PI, up = .3f + BossMesh.hash(seed + k * 17);
            Vector3f s = new Vector3f(Mth.cos(a) * spread * t, (up * t - t * t) * spread, Mth.sin(a) * spread * t).add(at);
            RitualMesh.billboard(c, poseStack, view, s, .12f + .1f * (1 - t), k, -1, BossMesh.glow(k % 3 == 0 ? 0xFFE0A0 : EMBER, alpha * (1 - t)), light);
        }
    }

    /** An arc of blue lightning from {@code a} to {@code b}, jagged afresh every few ticks; {@code alpha} bright. */
    public static void arc(PoseStack poseStack, MultiBufferSource buffer, Vector3f eye, Vector3f a, Vector3f b, float time, float width, float alpha, int seed,
                           int light) {
        if (alpha <= .01f) {
            return;
        }
        int flick = Mth.floor(time / 2);
        Vector3f[] pts = BossMesh.jagged(a, b, 9, .35f, seed + flick * 101);
        VertexConsumer c = BossMesh.light(buffer, BossMesh.BEAM);
        PoseStack.Pose p = poseStack.last();
        float f = alpha * (.7f + .3f * BossMesh.hash(seed + flick));
        for (int i = 0; i + 1 < pts.length; i++) {
            RitualMesh.shaft(c, p, eye, pts[i], pts[i + 1], width * 2.4f, BossMesh.glow(BLUE, f * .6f), light);
            RitualMesh.shaft(c, p, eye, pts[i], pts[i + 1], width, BossMesh.glow(BLUE_WHITE, f), light);
        }
    }

    /** A ring of light lying flat at height {@code y}, from {@code r0} to {@code r1}, bright at {@code r1}. */
    public static void ring(PoseStack.Pose p, MultiBufferSource buffer, float y, float r0, float r1, int rgb, float alpha, int light) {
        if (alpha <= .01f) {
            return;
        }
        BossMesh.annulus(BossMesh.light(buffer, BossMesh.GLOW), p, new Vector3f(0, y, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1), Math.max(.01f, r0), r1, 48, 1,
                0, 0, BossMesh.glow(rgb, 0), BossMesh.glow(rgb, alpha), light, true);
    }

    /** A burst where a machine breaks: gears and cogs flung out, a flash, steam and sparks, {@code t} (0 to 1) of the way. */
    public static void burst(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float t, float size, int seed, int light) {
        if (t >= 1) {
            return;
        }
        float out = 1 - (1 - t) * (1 - t), fade = 1 - t;
        for (int k = 0; k < 6; k++) {
            float a = k * Mth.TWO_PI / 6 + seed;
            float up = BossMesh.hash(seed + k) * .8f + .2f;
            Vector3f at = new Vector3f(Mth.cos(a) * size * 2 * out, (up * out - out * out * .8f) * size * 2, Mth.sin(a) * size * 2 * out);
            float r = size * (.18f + .12f * BossMesh.hash(seed + k * 5)) * (1 - t * .4f);
            gearAt(poseStack, buffer, k % 3 == 0 ? COPPER : BRASS, at, new Vector3f(Mth.sin(a + k), 1, Mth.cos(a)), t * 20 + k, r, 8, r * .3f, 0, WHITE, light);
        }
        glow(poseStack, buffer, view, new Vector3f(), size * (1.5f + 3 * t), BLUE_WHITE, fade * fade, light);
        glow(poseStack, buffer, view, new Vector3f(), size * (3 + 3 * t), EMBER, fade * fade * .6f, light);
        sparks(poseStack, buffer, view, new Vector3f(), size * 2.5f, 12, t * 12, fade, seed, light);
        steam(poseStack, buffer, view, new Vector3f(), new Vector3f(0, 1, 0), size * 2, size * 1.6f, 5, t * 10, .1f, fade * .7f, seed, light);
    }
}
