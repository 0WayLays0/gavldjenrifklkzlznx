package com.waylays.arcanes.client.render.echo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * What Echo Magic is drawn with: rings and arcs of sound (cyan light), the sculk of the deep dark spreading on the
 * ground with its glowing veins, tendrils and the bone of a shrieker.
 */
public final class EchoMesh {
    public static final ResourceLocation WAVE = ArcanesOublies.id("textures/entity/echo/wave.png");
    public static final ResourceLocation ARC = ArcanesOublies.id("textures/entity/echo/arc.png");
    public static final ResourceLocation SCULK = ArcanesOublies.id("textures/entity/echo/sculk.png");
    public static final ResourceLocation VEINS = ArcanesOublies.id("textures/entity/echo/veins.png");
    public static final ResourceLocation RIPPLE = ArcanesOublies.id("textures/entity/echo/ripple.png");
    public static final ResourceLocation SCAN = ArcanesOublies.id("textures/entity/echo/scan.png");
    public static final ResourceLocation BONE_SKIN = ArcanesOublies.id("textures/entity/echo/bone.png");
    public static final ResourceLocation HEART = ArcanesOublies.id("textures/entity/echo/heart.png");

    public static final int NIGHT = 0x041216;
    public static final int SCULK_DARK = 0x062A30;
    public static final int SCULK_TEAL = 0x0D4F5A;
    public static final int ECHO = 0x2CE6F0;
    public static final int PALE = 0xC8FBFF;
    public static final int BONE = 0xE8E2CF;

    private EchoMesh() {}

    /** A ring of sound {@code r} across round {@code centre} in the plane of {@code u} and {@code v}, seen from both sides. */
    public static void ring(VertexConsumer c, PoseStack.Pose p, Vector3f centre, Vector3f u, Vector3f v, float r, int argb, int light) {
        // The texture's rim lies at 0.86 of its half-width
        BossMesh.plate(c, p, centre, u, v, r * 2 / .86f, argb, light);
    }

    /** A ring of sound lying on the ground at height {@code y}, seen from above. */
    public static void groundRing(VertexConsumer c, PoseStack.Pose p, float x, float y, float z, float r, int argb, int light) {
        BossMesh.pool(c, p, x, y, z, r * 2 / .86f, 0, argb, light);
    }

    /**
     * An arc of sound {@code r} round {@code centre} in the plane of {@code u} and {@code v}, from angle {@code a0} to
     * {@code a1} (radians, from {@code u} toward {@code v}), {@code width} wide, seen from both sides.
     */
    public static void arc(VertexConsumer c, PoseStack.Pose p, Vector3f centre, Vector3f u, Vector3f v, float r, float a0, float a1, float width, int argb,
                           int light) {
        int n = Math.max(3, (int) (Math.abs(a1 - a0) * r * 3));
        for (int i = 0; i < n; i++) {
            float t0 = (float) i / n, t1 = (float) (i + 1) / n;
            float b0 = Mth.lerp(t0, a0, a1), b1 = Mth.lerp(t1, a0, a1);
            Vector3f d0 = new Vector3f(u).mul(Mth.cos(b0)).add(new Vector3f(v).mul(Mth.sin(b0)));
            Vector3f d1 = new Vector3f(u).mul(Mth.cos(b1)).add(new Vector3f(v).mul(Mth.sin(b1)));
            Vector3f i0 = new Vector3f(d0).mul(r - width * .5f).add(centre), o0 = new Vector3f(d0).mul(r + width * .5f).add(centre);
            Vector3f i1 = new Vector3f(d1).mul(r - width * .5f).add(centre), o1 = new Vector3f(d1).mul(r + width * .5f).add(centre);
            BossMesh.quad2(c, p, i0, o0, o1, i1, t0, 1, t0, 0, t1, 0, t1, 1, argb, argb, light);
        }
    }

    /** Sculk spread on the ground round {@code (x, z)}, {@code size} across, its veins glowing {@code glow} (0 to 1). */
    public static void patch(PoseStack poseStack, MultiBufferSource buffer, float x, float y, float z, float size, float spin, float alpha, float glow,
                             int light) {
        if (size <= .05f) {
            return;
        }
        VertexConsumer sculk = BossMesh.ground(buffer, SCULK);
        BossMesh.pool(sculk, poseStack.last(), x, y, z, size, spin, BossMesh.argb(0xFFFFFF, alpha), light);
        VertexConsumer veins = BossMesh.light(buffer, VEINS);
        BossMesh.pool(veins, poseStack.last(), x, y + .01f, z, size, spin, BossMesh.glow(ECHO, glow * alpha), light);
    }

    /** A sculk tendril from {@code base}, {@code len} long, curling toward {@code bend}: dark at its root, its tip glowing. */
    public static void tendril(VertexConsumer c, PoseStack.Pose p, Vector3f base, Vector3f dir, Vector3f bend, float len, float thick, float alpha,
                               int light) {
        BossMesh.horn(c, p, base, dir, bend, len, thick, 5, 6, BossMesh.argb(SCULK_DARK, alpha), BossMesh.argb(0x3FD8E0, alpha), light);
    }

    /** Motes of the deep dark's light drifting up round {@code (0, 0, 0)} within {@code radius}. */
    public static void souls(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float radius, float height, float age, int count,
                             float alpha, int seed, int light) {
        VertexConsumer motes = BossMesh.light(buffer, BossMesh.MOTE);
        for (int k = 0; k < count; k++) {
            float t = (age * (.012f + .008f * BossMesh.hash(seed + k)) + BossMesh.hash(seed + k * 3)) % 1;
            float a = BossMesh.hash(seed + k * 5) * Mth.TWO_PI + t * 1.5f;
            float r = radius * Mth.sqrt(BossMesh.hash(seed + k * 7));
            Vector3f at = new Vector3f(Mth.cos(a) * r, height * t, Mth.sin(a) * r);
            RitualMesh.billboard(motes, poseStack, view, at, .28f, 0, -1, BossMesh.glow(k % 3 == 0 ? PALE : ECHO, Mth.sin(t * Mth.PI) * alpha), light);
        }
    }

    /** Ripples of sound: a bright rim {@code r} across round {@code centre} and fainter rings inside it, seen from both sides. */
    public static void ripple(VertexConsumer c, PoseStack.Pose p, Vector3f centre, Vector3f u, Vector3f v, float r, int argb, int light) {
        // The texture's rim lies at 0.9 of its half-width
        BossMesh.plate(c, p, centre, u, v, r * 2 / .9f, argb, light);
    }

    /** Ripples of sound lying on the ground at height {@code y}, seen from above. */
    public static void groundRipple(VertexConsumer c, PoseStack.Pose p, float x, float y, float z, float r, float spin, int argb, int light) {
        BossMesh.pool(c, p, x, y, z, r * 2 / .9f, spin, argb, light);
    }

    /**
     * The front of a wave of sound: {@code rings} rings of light round {@code centre} in the plane of {@code u} and
     * {@code v}, the widest {@code r} across, the narrower ones pushed forward along {@code fwd} up to {@code depth}, so
     * that together they bulge like a shell; the outer ones brightest.
     */
    public static void front(VertexConsumer c, PoseStack.Pose p, Vector3f centre, Vector3f fwd, Vector3f u, Vector3f v, float r, float depth, int rings,
                             float alpha, int light) {
        for (int i = 0; i < rings; i++) {
            float t = (i + 1f) / rings;
            Vector3f at = new Vector3f(fwd).mul(depth * (1 - t * t)).add(centre);
            ring(c, p, at, u, v, r * t, BossMesh.glow(i % 2 == 0 ? ECHO : PALE, alpha * (.35f + .65f * t)), light);
        }
    }

    /** A rib, fang or horn of bone from {@code base} (see {@link BossMesh#horn}): stained with sculk at its root. */
    public static void bone(VertexConsumer c, PoseStack.Pose p, Vector3f base, Vector3f dir, Vector3f bend, float len, float thick, float alpha, int light) {
        BossMesh.horn(c, p, base, dir, bend, len, thick, 6, 7, BossMesh.argb(0xFFFFFF, alpha), BossMesh.argb(0xFFFFFF, alpha), light);
    }

    /**
     * A figure of light the height of a player standing at the pose's origin, facing {@code yaw} (radians), drawn with the
     * lines of {@link #SCAN}: its head, body, arms and legs, {@code spread} pushing its limbs apart.
     */
    public static void figure(VertexConsumer c, PoseStack.Pose p, float yaw, float spread, int argb, int light) {
        Vector3f x = new Vector3f(Mth.cos(yaw), 0, Mth.sin(yaw)), z = new Vector3f(-Mth.sin(yaw), 0, Mth.cos(yaw)), y = new Vector3f(0, 1, 0);
        part(c, p, x, y, z, 0, 1.76f, 0, .23f, .23f, .23f, argb, light);
        part(c, p, x, y, z, 0, 1.13f, 0, .24f, .36f, .13f, argb, light);
        for (int side = -1; side <= 1; side += 2) {
            part(c, p, x, y, z, side * (.43f + .06f * spread), 1.14f - .03f * spread, 0, .11f, .36f, .11f, argb, light);
            part(c, p, x, y, z, side * (.14f + .03f * spread), .375f, 0, .11f, .37f, .11f, argb, light);
        }
    }

    private static void part(VertexConsumer c, PoseStack.Pose p, Vector3f x, Vector3f y, Vector3f z, float px, float py, float pz, float hx, float hy, float hz,
                             int argb, int light) {
        Vector3f at = new Vector3f(x).mul(px).add(new Vector3f(y).mul(py)).add(new Vector3f(z).mul(pz));
        BossMesh.box(c, p, at, new Vector3f(x).mul(hx), new Vector3f(y).mul(hy), new Vector3f(z).mul(hz), argb, light);
    }

    /** Two unit vectors square to {@code d} and to each other. */
    public static Vector3f[] frame(Vector3f d) {
        Vector3f a = BossMesh.across(d);
        Vector3f b = new Vector3f(d).cross(a).normalize();
        return new Vector3f[] {a, b};
    }
}
