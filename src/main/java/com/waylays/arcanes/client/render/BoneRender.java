package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.spells.bone.BoneFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Drawing shared by the Bone Magic renderers: real rounded bones (a shaft swelling into a double knob at each end),
 * spikes curving like horns, tubes along curves (ribs, a whip), vertebrae, a skull with its jaw and glowing eye
 * sockets, and a skeletal hand that can close. All lit like any entity, with one ivory texture (u round, v along).
 * Positions are in the pose's units (blocks).
 */
public final class BoneRender {
    public static final ResourceLocation BONE = ArcanesOublies.id("textures/entity/bone/bone.png");
    public static final int WHITE = 0xFFFFFFFF;
    /** The dark of eye sockets and the nose. */
    public static final int HOLLOW = 0xFF2A1612;
    private static final Vector3f UP = new Vector3f(0, 1, 0);

    private BoneRender() {}

    public static VertexConsumer buffer(MultiBufferSource buffer) {
        return buffer.getBuffer(RenderType.entityCutoutNoCull(BONE));
    }

    private static void vertex(VertexConsumer c, PoseStack.Pose p, Vector3f pos, float u, float v, Vector3f n, int light, int argb) {
        c.addVertex(p, pos.x, pos.y, pos.z)
                .setColor((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(p, n.x, n.y, n.z);
    }

    /** Two unit vectors at right angles to {@code d} (a unit vector) and to each other. */
    private static Vector3f[] basis(Vector3f d) {
        Vector3f ref = Math.abs(d.y) > .9f ? new Vector3f(1, 0, 0) : UP;
        Vector3f e1 = new Vector3f(d).cross(ref).normalize();
        Vector3f e2 = new Vector3f(d).cross(e1).normalize();
        return new Vector3f[]{e1, e2};
    }

    private static Vector3f add(Vector3f a, Vector3f b, float s) {
        return new Vector3f(b).mul(s).add(a);
    }

    /** A tube from {@code a} to {@code b}, {@code ra} wide at a and {@code rb} at b. */
    public static void tube(VertexConsumer c, PoseStack.Pose p, Vector3f a, Vector3f b, float ra, float rb, int sides, int light, int argb) {
        Vector3f d = new Vector3f(b).sub(a);
        float len = d.length();
        if (len < 1.0E-4f) {
            return;
        }
        d.div(len);
        Vector3f[] e = basis(d);
        float slope = (ra - rb) / len;
        for (int i = 0; i < sides; i++) {
            float t0 = i * Mth.TWO_PI / sides, t1 = (i + 1) * Mth.TWO_PI / sides;
            Vector3f r0 = add(new Vector3f(e[0]).mul(Mth.cos(t0)), e[1], Mth.sin(t0));
            Vector3f r1 = add(new Vector3f(e[0]).mul(Mth.cos(t1)), e[1], Mth.sin(t1));
            Vector3f n0 = add(r0, d, slope).normalize();
            Vector3f n1 = add(r1, d, slope).normalize();
            float u0 = (float) i / sides, u1 = (float) (i + 1) / sides;
            vertex(c, p, add(a, r0, ra), u0, 0, n0, light, argb);
            vertex(c, p, add(b, r0, rb), u0, 1, n0, light, argb);
            vertex(c, p, add(b, r1, rb), u1, 1, n1, light, argb);
            vertex(c, p, add(a, r1, ra), u1, 0, n1, light, argb);
        }
    }

    /** A closed ellipsoid round {@code center}, its half axes {@code ax}, {@code ay}, {@code az} (at right angles). */
    public static void blob(VertexConsumer c, PoseStack.Pose p, Vector3f center, Vector3f ax, Vector3f ay, Vector3f az, int segs, int rings,
                            int light, int argb) {
        Vector3f nx = new Vector3f(ax).div(Math.max(1.0E-6f, ax.lengthSquared()));
        Vector3f ny = new Vector3f(ay).div(Math.max(1.0E-6f, ay.lengthSquared()));
        Vector3f nz = new Vector3f(az).div(Math.max(1.0E-6f, az.lengthSquared()));
        for (int j = 0; j < rings; j++) {
            float f0 = j * Mth.PI / rings, f1 = (j + 1) * Mth.PI / rings;
            for (int i = 0; i < segs; i++) {
                float t0 = i * Mth.TWO_PI / segs, t1 = (i + 1) * Mth.TWO_PI / segs;
                corner(c, p, center, ax, ay, az, nx, ny, nz, f0, t0, light, argb);
                corner(c, p, center, ax, ay, az, nx, ny, nz, f1, t0, light, argb);
                corner(c, p, center, ax, ay, az, nx, ny, nz, f1, t1, light, argb);
                corner(c, p, center, ax, ay, az, nx, ny, nz, f0, t1, light, argb);
            }
        }
    }

    private static void corner(VertexConsumer c, PoseStack.Pose p, Vector3f center, Vector3f ax, Vector3f ay, Vector3f az,
                               Vector3f nx, Vector3f ny, Vector3f nz, float f, float t, int light, int argb) {
        float sx = Mth.sin(f) * Mth.cos(t), sy = Mth.cos(f), sz = Mth.sin(f) * Mth.sin(t);
        Vector3f pos = new Vector3f(center).add(new Vector3f(ax).mul(sx)).add(new Vector3f(ay).mul(sy)).add(new Vector3f(az).mul(sz));
        Vector3f n = new Vector3f(nx).mul(sx).add(new Vector3f(ny).mul(sy)).add(new Vector3f(nz).mul(sz));
        if (n.lengthSquared() < 1.0E-10f) {
            n.set(0, 1, 0);
        }
        vertex(c, p, pos, t / Mth.TWO_PI, f / Mth.PI, n.normalize(), light, argb);
    }

    /** A round blob {@code r} wide. */
    public static void ball(VertexConsumer c, PoseStack.Pose p, Vector3f center, float r, int light, int argb) {
        blob(c, p, center, new Vector3f(r, 0, 0), new Vector3f(0, r, 0), new Vector3f(0, 0, r), 8, 5, light, argb);
    }

    /** A bone from {@code a} to {@code b}, {@code r} thick: a shaft narrowing in the middle, a double knob at each end. */
    public static void bone(VertexConsumer c, PoseStack.Pose p, Vector3f a, Vector3f b, float r, int light, int argb) {
        Vector3f d = new Vector3f(b).sub(a);
        float len = d.length();
        if (len < 1.0E-4f) {
            return;
        }
        d.div(len);
        Vector3f[] e = basis(d);
        Vector3f a1 = add(a, d, r * .5f), b1 = add(b, d, -r * .5f), mid = new Vector3f(a).add(b).mul(.5f);
        tube(c, p, a1, mid, r * .66f, r * .5f, 7, light, argb);
        tube(c, p, mid, b1, r * .5f, r * .66f, 7, light, argb);
        for (Vector3f end : new Vector3f[]{a, b}) {
            for (int side = -1; side <= 1; side += 2) {
                Vector3f k = add(end, e[0], side * r * .42f);
                blob(c, p, k, new Vector3f(e[0]).mul(r * .6f), new Vector3f(d).mul(r * .66f), new Vector3f(e[1]).mul(r * .6f), 7, 4, light, argb);
            }
        }
    }

    /** A spike from {@code base} to {@code tip}, {@code r} thick at its foot, bowed by {@code bend} at its middle. */
    public static void spike(VertexConsumer c, PoseStack.Pose p, Vector3f base, Vector3f tip, float r, Vector3f bend, int light, int argb) {
        int segs = 5;
        Vector3f prev = new Vector3f(base);
        float prevR = r;
        for (int k = 1; k <= segs; k++) {
            float t = (float) k / segs;
            Vector3f at = new Vector3f(tip).sub(base).mul(t).add(base).add(new Vector3f(bend).mul(4 * t * (1 - t)));
            float rr = r * (float) Math.pow(1 - t, .85);
            tube(c, p, prev, at, prevR, rr, 6, light, argb);
            prev = at;
            prevR = rr;
        }
        // A rounded foot so it sits on the ground
        ball(c, p, base, r * 1.05f, light, argb);
    }

    /** A tube along {@code points}, {@code r0} thick at the first and {@code r1} at the last. */
    public static void curve(VertexConsumer c, PoseStack.Pose p, Vector3f[] points, float r0, float r1, int sides, int light, int argb) {
        for (int i = 0; i + 1 < points.length; i++) {
            float ta = (float) i / (points.length - 1), tb = (float) (i + 1) / (points.length - 1);
            tube(c, p, points[i], points[i + 1], Mth.lerp(ta, r0, r1), Mth.lerp(tb, r0, r1), sides, light, argb);
        }
    }

    /**
     * A vertebra at {@code center}: a rounded body across the spine (which runs along {@code along}), a spine sticking
     * out towards {@code up} and back, and two side processes. {@code s} is its width.
     */
    public static void vertebra(VertexConsumer c, PoseStack.Pose p, Vector3f center, Vector3f along, Vector3f up, float s, int light, int argb) {
        Vector3f right = new Vector3f(along).cross(up).normalize();
        blob(c, p, center, new Vector3f(right).mul(s * .48f), new Vector3f(along).mul(s * .3f), new Vector3f(up).mul(s * .42f), 9, 5, light, argb);
        Vector3f top = add(center, up, s * .3f);
        spike(c, p, top, add(add(center, up, s * 1.05f), along, -s * .35f), s * .16f, new Vector3f(), light, argb);
        for (int side = -1; side <= 1; side += 2) {
            spike(c, p, add(center, right, side * s * .3f), add(add(center, right, side * s * .9f), up, s * .12f), s * .12f, new Vector3f(), light, argb);
        }
    }

    /**
     * A skull standing on the origin, facing +Z, {@code s} high: the cranium, brow and cheekbones, the upper jaw and its
     * teeth, the lower jaw dropped by {@code jaw} radians, dark sockets with eyes glowing crimson ({@code glow} 0 to 1).
     */
    public static void skull(PoseStack poseStack, MultiBufferSource buffer, float s, float jaw, int light, float glow) {
        VertexConsumer c = buffer(buffer);
        PoseStack.Pose p = poseStack.last();
        blob(c, p, new Vector3f(0, .62f * s, -.08f * s), new Vector3f(.5f * s, 0, 0), new Vector3f(0, .47f * s, 0), new Vector3f(0, 0, .58f * s), 14, 9, light, WHITE);
        blob(c, p, new Vector3f(0, .5f * s, .37f * s), new Vector3f(.43f * s, 0, 0), new Vector3f(0, .1f * s, 0), new Vector3f(0, 0, .12f * s), 10, 5, light, WHITE);
        for (int side = -1; side <= 1; side += 2) {
            blob(c, p, new Vector3f(side * .31f * s, .3f * s, .3f * s), new Vector3f(.13f * s, 0, 0), new Vector3f(0, .12f * s, 0), new Vector3f(0, 0, .14f * s), 7, 4, light, WHITE);
            bone(c, p, new Vector3f(side * .33f * s, .3f * s, .25f * s), new Vector3f(side * .42f * s, .35f * s, -.15f * s), .07f * s, light, WHITE);
        }
        blob(c, p, new Vector3f(0, .19f * s, .33f * s), new Vector3f(.3f * s, 0, 0), new Vector3f(0, .15f * s, 0), new Vector3f(0, 0, .2f * s), 10, 5, light, WHITE);
        for (int k = -3; k <= 3; k++) {
            blob(c, p, new Vector3f(k * .075f * s, .05f * s, (.47f - Math.abs(k) * .025f) * s), new Vector3f(.034f * s, 0, 0),
                    new Vector3f(0, .06f * s, 0), new Vector3f(0, 0, .034f * s), 5, 3, light, WHITE);
        }
        // The lower jaw, hinged under the ears
        poseStack.pushPose();
        poseStack.translate(0, .2f * s, .02f * s);
        poseStack.mulPose(com.mojang.math.Axis.XP.rotation(jaw));
        PoseStack.Pose pj = poseStack.last();
        blob(c, pj, new Vector3f(0, -.13f * s, .22f * s), new Vector3f(.29f * s, 0, 0), new Vector3f(0, .08f * s, 0), new Vector3f(0, 0, .24f * s), 10, 5, light, WHITE);
        for (int k = -3; k <= 3; k++) {
            blob(c, pj, new Vector3f(k * .07f * s, -.07f * s, (.42f - Math.abs(k) * .025f) * s), new Vector3f(.03f * s, 0, 0),
                    new Vector3f(0, .05f * s, 0), new Vector3f(0, 0, .03f * s), 5, 3, light, WHITE);
        }
        for (int side = -1; side <= 1; side += 2) {
            bone(c, pj, new Vector3f(side * .27f * s, -.13f * s, .06f * s), new Vector3f(side * .31f * s, .02f * s, -.08f * s), .06f * s, light, WHITE);
        }
        poseStack.popPose();
        // Sockets and nose, hollow
        for (int side = -1; side <= 1; side += 2) {
            blob(c, p, new Vector3f(side * .19f * s, .42f * s, .39f * s), new Vector3f(.13f * s, 0, 0), new Vector3f(0, .12f * s, 0),
                    new Vector3f(0, 0, .09f * s), 8, 4, light, HOLLOW);
        }
        blob(c, p, new Vector3f(0, .28f * s, .45f * s), new Vector3f(.055f * s, 0, 0), new Vector3f(0, .08f * s, 0), new Vector3f(0, 0, .05f * s), 6, 3, light, HOLLOW);
        // The eyes, glowing (after the bone: a buffer is not written to again once another has been asked for)
        if (glow > .01f) {
            VertexConsumer eyes = buffer.getBuffer(RenderType.eyes(GlyphRender.GLOW));
            for (int side = -1; side <= 1; side += 2) {
                poseStack.pushPose();
                poseStack.translate(side * .19f * s, .42f * s, .49f * s);
                RenderHelper.billboard(eyes, poseStack.last(), .2f * s, RuneRender.additive(BoneFx.MARROW_CORE, glow), GlyphRender.FULL);
                RenderHelper.billboard(eyes, poseStack.last(), .38f * s, RuneRender.additive(BoneFx.MARROW, glow * .6f), GlyphRender.FULL);
                poseStack.popPose();
            }
        }
    }

    /**
     * A skeletal hand with its wrist on the origin, fingers reaching up +Y, the palm facing +Z, {@code s} long from wrist
     * to knuckles; {@code curl} (0 open to 1 closed into a fist) bends the fingers towards +Z. The last bone of each
     * finger is a claw.
     */
    public static void hand(VertexConsumer c, PoseStack.Pose p, float s, float curl, int light, int argb) {
        float[] spread = {-.42f, -.14f, .14f, .42f};
        float[] lengths = {.92f, 1f, .96f, .8f};
        bone(c, p, new Vector3f(-.18f * s, -.55f * s, 0), new Vector3f(-.15f * s, .05f * s, 0), .11f * s, light, argb);
        bone(c, p, new Vector3f(.18f * s, -.55f * s, 0), new Vector3f(.15f * s, .05f * s, 0), .11f * s, light, argb);
        ball(c, p, new Vector3f(0, .08f * s, 0), .2f * s, light, argb);
        for (int f = 0; f < 4; f++) {
            Vector3f knuckle = new Vector3f(spread[f] * s, .95f * s * lengths[f], 0);
            bone(c, p, new Vector3f(spread[f] * .45f * s, .1f * s, 0), knuckle, .085f * s, light, argb);
            Vector3f dir = new Vector3f(spread[f] * .25f, 1, 0).normalize();
            Vector3f at = knuckle;
            float[] seg = {.5f, .36f, .3f};
            for (int k = 0; k < 3; k++) {
                // Each joint bends towards the palm
                float bend = curl * (k == 0 ? 1.25f : 1.1f);
                dir.rotateAxis(bend, 1, 0, 0);
                Vector3f next = add(at, dir, seg[k] * s * lengths[f]);
                if (k < 2) {
                    bone(c, p, at, next, (.075f - .01f * k) * s, light, argb);
                } else {
                    spike(c, p, at, add(next, dir, .1f * s), .06f * s, new Vector3f(0, 0, .05f * s), light, argb);
                }
                at = next;
            }
        }
        // The thumb, from the side of the palm
        Vector3f dir = new Vector3f(.8f, .6f, .2f).normalize();
        Vector3f at = new Vector3f(.3f * s, .15f * s, .05f * s);
        for (int k = 0; k < 3; k++) {
            dir.rotateAxis(curl * .9f, 0, 0, 1).rotateAxis(curl * .5f, 1, 0, 0);
            Vector3f next = add(at, dir, (.42f - .08f * k) * s);
            if (k < 2) {
                bone(c, p, at, next, (.085f - .012f * k) * s, light, argb);
            } else {
                spike(c, p, at, add(next, dir, .08f * s), .06f * s, new Vector3f(), light, argb);
            }
            at = next;
        }
    }

    /** Turns the pose so that +Y points along {@code dir}. */
    public static void alignUp(PoseStack poseStack, Vector3f dir) {
        if (dir.lengthSquared() < 1.0E-8f) {
            return;
        }
        poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 1, 0), new Vector3f(dir).normalize()));
    }
}
