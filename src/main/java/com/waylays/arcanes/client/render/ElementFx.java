package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.clockwork.ClockMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

/**
 * What the elemental schools' renderers (Snow, Lava, Plant, Acid) draw with: the light their effects are lit by, glows
 * in layers, soft puffs, drifting motes, sparks thrown out, a whirling wall, a ring on the ground.
 */
public final class ElementFx {
    public static final int FULL = LightTexture.FULL_BRIGHT;

    private ElementFx() {}

    /**
     * The light an effect is lit by: taken a block above the scene (a scene lies on the ground, and the light inside
     * the floor is black, which turned every cloud and puff a dirty grey), never darker than {@code min} (0 to 15): a
     * spell gives some light of its own.
     */
    public static int light(Entity e, int min) {
        BlockPos at = e.blockPosition().above();
        int packed = LevelRenderer.getLightColor(e.level(), at);
        return LightTexture.pack(Math.max(min, LightTexture.block(packed)), Math.max(min, LightTexture.sky(packed)));
    }

    /** A glow in three layers: a wide soft halo of {@code rgb}, its body, and a near-white heart. */
    public static void bloom(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f at, float size, int rgb, float alpha) {
        if (alpha <= .01f) {
            return;
        }
        ClockMesh.glow(poseStack, buffer, view, at, size * 2.6f, rgb, .22f * alpha, FULL);
        ClockMesh.glow(poseStack, buffer, view, at, size * 1.2f, rgb, .55f * alpha, FULL);
        ClockMesh.glow(poseStack, buffer, view, at, size * .45f, BossMesh.mix(rgb, 0xFFFFFF, .65f), .85f * alpha, FULL);
    }

    /**
     * Soft puffs of {@code texture} (cloud, mist, smoke) round {@code at}, {@code count} of them drifting out to
     * {@code spread} and up by {@code rise} over their life ({@code rate} of it a tick), each swelling from
     * {@code size}; see-through, lit by {@code light}.
     */
    public static void puffs(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, ResourceLocation texture, Vector3f at, float spread, float rise,
                             float size, int count, float age, float rate, int rgb, float alpha, int seed, int light) {
        if (alpha <= .01f) {
            return;
        }
        VertexConsumer c = BossMesh.see(buffer, texture);
        for (int k = 0; k < count; k++) {
            float t = (age * rate + BossMesh.hash(seed + k * 7)) % 1;
            float a = BossMesh.hash(seed + k * 13) * Mth.TWO_PI, out = spread * (.3f + .7f * BossMesh.hash(seed + k * 17));
            Vector3f p = new Vector3f(Mth.cos(a) * out * t, rise * t + (BossMesh.hash(seed + k * 19) - .5f) * size, Mth.sin(a) * out * t).add(at);
            float fade = Mth.sin(Mth.PI * t);
            RitualMesh.billboard(c, poseStack, view, p, size * (.6f + 1.2f * t), a + age * .01f * (k % 2 == 0 ? 1 : -1), -1, BossMesh.argb(rgb, alpha * fade),
                    light);
        }
    }

    /**
     * Motes drifting round {@code at} within {@code radius} (and {@code height} up), twinkling: {@code count} of
     * {@code texture} (a flake, an ember, a mote), drawn as light.
     */
    public static void motes(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, ResourceLocation texture, Vector3f at, float radius,
                             float height, float size, int count, float age, int rgb, float alpha, int seed) {
        if (alpha <= .01f) {
            return;
        }
        VertexConsumer c = BossMesh.light(buffer, texture);
        for (int k = 0; k < count; k++) {
            float h1 = BossMesh.hash(seed + k), h2 = BossMesh.hash(seed + k * 3), h3 = BossMesh.hash(seed + k * 5);
            float a = h1 * Mth.TWO_PI + age * (.01f + .03f * h2) * (k % 2 == 0 ? 1 : -1);
            float rr = radius * Mth.sqrt(h2);
            float y = (h3 * height + age * .02f * (.5f + h1)) % Math.max(.01f, height);
            float tw = .45f + .55f * Mth.sin(age * (.15f + .2f * h3) + k * 2.1f);
            Vector3f p = new Vector3f(Mth.cos(a) * rr, y, Mth.sin(a) * rr).add(at);
            float edge = Mth.sin(Mth.PI * Mth.clamp(y / Math.max(.01f, height), 0, 1));
            RitualMesh.billboard(c, poseStack, view, p, size * (.6f + .6f * tw), age * .04f + k, -1, BossMesh.glow(rgb, alpha * tw * (.3f + .7f * edge)),
                    FULL);
        }
    }

    /**
     * Bits thrown out from {@code at} and falling, {@code t} (0 to 1) of their flight: {@code count} sprites of
     * {@code texture}, out to {@code reach}, up to {@code up} high; drawn as light if {@code light} is {@code FULL},
     * else see-through.
     */
    public static void burst(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, ResourceLocation texture, Vector3f at, float reach, float up,
                             float size, int count, float t, int rgb, float alpha, int seed, boolean additive, int light) {
        if (t >= 1 || alpha <= .01f) {
            return;
        }
        VertexConsumer c = additive ? BossMesh.light(buffer, texture) : BossMesh.see(buffer, texture);
        float out = 1 - (1 - t) * (1 - t);
        for (int k = 0; k < count; k++) {
            float a = BossMesh.hash(seed + k) * Mth.TWO_PI, s = .5f + .5f * BossMesh.hash(seed + k * 3), lift = .4f + .6f * BossMesh.hash(seed + k * 5);
            Vector3f p = new Vector3f(Mth.cos(a) * reach * s * out, up * lift * (2.2f * t - 2.2f * t * t) * 1.8f, Mth.sin(a) * reach * s * out).add(at);
            int col = additive ? BossMesh.glow(rgb, alpha * (1 - t)) : BossMesh.argb(rgb, alpha * (1 - t * t));
            RitualMesh.billboard(c, poseStack, view, p, size * (1 - .4f * t), t * 6 + k, -1, col, additive ? FULL : light);
        }
    }

    /**
     * A wall whirling round {@code radius}, from {@code y0} to {@code y1}: {@code bands} rings of {@code texture} turning
     * at their own pace, thickest in their middle and fading to nothing at their top and bottom.
     */
    public static void wall(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation texture, float radius, float y0, float y1, int bands, float age,
                            float speed, int rgb, float alpha, int light) {
        VertexConsumer c = BossMesh.see(buffer, texture);
        int segs = 48;
        float bandH = (y1 - y0) / bands;
        for (int b = 0; b < bands; b++) {
            float by0 = y0 + b * bandH * .85f, by1 = by0 + bandH * 1.3f, mid = (by0 + by1) / 2;
            float r = radius * (1 - .04f * b);
            float turn = age * speed * (b % 2 == 0 ? 1 : -1.3f) + b;
            for (int s = 0; s < segs; s++) {
                float a0 = s * Mth.TWO_PI / segs, a1 = (s + 1) * Mth.TWO_PI / segs;
                float u0 = (float) s / segs * 4 + turn, u1 = (float) (s + 1) / segs * 4 + turn;
                Vector3f p00 = new Vector3f(Mth.cos(a0) * r, by0, Mth.sin(a0) * r), p10 = new Vector3f(Mth.cos(a1) * r, by0, Mth.sin(a1) * r);
                Vector3f p01 = new Vector3f(Mth.cos(a0) * r, mid, Mth.sin(a0) * r), p11 = new Vector3f(Mth.cos(a1) * r, mid, Mth.sin(a1) * r);
                Vector3f p02 = new Vector3f(Mth.cos(a0) * r, by1, Mth.sin(a0) * r), p12 = new Vector3f(Mth.cos(a1) * r, by1, Mth.sin(a1) * r);
                twoSided(c, poseStack.last(), p00, p10, p11, p01, u0, 1, u1, 1, u1, .5f, u0, .5f, BossMesh.argb(rgb, 0), BossMesh.argb(rgb, alpha), light);
                twoSided(c, poseStack.last(), p01, p11, p12, p02, u0, .5f, u1, .5f, u1, 0, u0, 0, BossMesh.argb(rgb, alpha), BossMesh.argb(rgb, 0), light);
            }
        }
    }

    /**
     * A see-through quad (the see-through render type is drawn from both sides), lit as if it faced up whichever side
     * is seen (a cloud, a sheet of spray: shading it by its slant would turn it grey).
     */
    public static void twoSided(VertexConsumer c, PoseStack.Pose p, Vector3f a, Vector3f b, Vector3f d, Vector3f e, float ua, float va, float ub, float vb,
                                float ud, float vd, float ue, float ve, int argbA, int argbB, int light) {
        BossMesh.quad(c, p, a, b, d, e, ua, va, ub, vb, ud, vd, ue, ve, argbA, argbB, light, BossMesh.UP);
    }

    /**
     * A quad lying on the ground, wound so that it faces up whichever order its corners come in (a ground quad wound
     * the other way faces down and is culled: the river, the furrow and the ice ribbon were never seen).
     */
    public static void flat(VertexConsumer c, PoseStack.Pose p, Vector3f a, Vector3f b, Vector3f d, Vector3f e, float ua, float va, float ub, float vb,
                            float ud, float vd, float ue, float ve, int argbA, int argbB, int light) {
        float ny = (b.z - a.z) * (e.x - a.x) - (b.x - a.x) * (e.z - a.z);
        if (ny >= 0) {
            BossMesh.quad(c, p, a, b, d, e, ua, va, ub, vb, ud, vd, ue, ve, argbA, argbB, light, BossMesh.UP);
        } else {
            WaterMesh.put(c, p, a, ua, va, argbA, light, BossMesh.UP);
            WaterMesh.put(c, p, e, ue, ve, argbB, light, BossMesh.UP);
            WaterMesh.put(c, p, d, ud, vd, argbB, light, BossMesh.UP);
            WaterMesh.put(c, p, b, ub, vb, argbA, light, BossMesh.UP);
        }
    }

    /**
     * A lumpy ball round {@code centre}: a sphere {@code r} across, its surface pushed in and out by noise ({@code rough}
     * of its radius, {@code grain} how fine; {@code time} makes it churn), squashed to {@code squash} of its height: a
     * snowball, a gob of magma, a rock, a puffball, a wobbling bubble. Its texture wraps round it once.
     */
    public static void lumpy(VertexConsumer c, PoseStack.Pose p, Vector3f centre, float r, int rings, int segs, float rough, float grain, float time,
                             int seed, float squash, int argb, int light) {
        if (r <= .001f) {
            return;
        }
        Vector3f[][] pts = new Vector3f[rings + 1][segs + 1];
        Vector3f[][] nrm = new Vector3f[rings + 1][segs + 1];
        for (int i = 0; i <= rings; i++) {
            float th = Mth.PI * i / rings;
            for (int j = 0; j <= segs; j++) {
                float ph = Mth.TWO_PI * (j % segs) / segs;
                Vector3f d = new Vector3f(Mth.sin(th) * Mth.cos(ph), Mth.cos(th), Mth.sin(th) * Mth.sin(ph));
                float bump = 1 + rough * WaterMesh.turbulence(d.x * grain + seed * .37f, d.y * grain + time, d.z * grain - seed * .11f);
                pts[i][j] = new Vector3f(d.x * r * bump, d.y * r * bump * squash, d.z * r * bump).add(centre);
                nrm[i][j] = new Vector3f(d.x, d.y / Math.max(.2f, squash), d.z).normalize();
            }
        }
        for (int i = 0; i < rings; i++) {
            for (int j = 0; j < segs; j++) {
                float u0 = (float) j / segs, u1 = (float) (j + 1) / segs, v0 = (float) i / rings, v1 = (float) (i + 1) / rings;
                // Wound to face out (a solid render type culls the inside)
                WaterMesh.put(c, p, pts[i][j], u0, v0, argb, light, nrm[i][j]);
                WaterMesh.put(c, p, pts[i][j + 1], u1, v0, argb, light, nrm[i][j + 1]);
                WaterMesh.put(c, p, pts[i + 1][j + 1], u1, v1, argb, light, nrm[i + 1][j + 1]);
                WaterMesh.put(c, p, pts[i + 1][j], u0, v1, argb, light, nrm[i + 1][j]);
            }
        }
    }

    /** Air so hot (or cold, or foul) that it bends the light: rippling lines, light against dark (see GaleSceneRenderer). */
    public static final ResourceLocation WARP = com.waylays.arcanes.ArcanesOublies.id("textures/entity/gale/warp.png");

    /**
     * A haze rising from {@code at}: {@code layers} sheets of bent air facing the eye, {@code width} wide and
     * {@code height} tall, their ripples drifting up at {@code speed} (tiles a tick), tinted {@code rgb}, fading out at
     * their top: heat over lava, cold over snow, fumes over acid.
     */
    public static void haze(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f at, float width, float height, int layers,
                            float age, float speed, int rgb, float alpha) {
        if (alpha <= .01f) {
            return;
        }
        VertexConsumer c = BossMesh.see(buffer, WARP);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < layers; k++) {
            float off = (k - (layers - 1) / 2f) * width * .35f;
            Vector3f base = new Vector3f(at);
            Vector3f toEye = new Vector3f(view.eye()).sub(base);
            toEye.y = 0;
            if (toEye.lengthSquared() < 1.0E-6f) {
                toEye.set(0, 0, 1);
            }
            Vector3f side = new Vector3f(toEye).cross(0, 1, 0).normalize();
            base.add(new Vector3f(side).mul(off));
            Vector3f s = new Vector3f(side).mul(width * .5f);
            float v0 = age * speed * (1 + k * .3f) + k * .37f, v1 = v0 + height / 2;
            float u0 = k * .31f, u1 = u0 + width / 2;
            int bottom = BossMesh.argb(rgb, alpha * (.9f - k * .15f)), top = BossMesh.argb(rgb, 0);
            // Bottom edge, then the top, fading
            BossMesh.quad(c, p, new Vector3f(base).sub(s), new Vector3f(base).add(s), new Vector3f(base).add(s).add(0, height, 0),
                    new Vector3f(base).sub(s).add(0, height, 0), u0, v1, u1, v1, u1, v0, u0, v0, bottom, top, FULL, BossMesh.UP);
        }
    }

    /** A soft shadow on the ground under something, {@code radius} across, at {@code at} (its y the ground's). */
    public static void shadow(PoseStack poseStack, MultiBufferSource buffer, Vector3f at, float radius, float alpha) {
        if (alpha <= .01f || radius <= .01f) {
            return;
        }
        VertexConsumer c = BossMesh.ground(buffer, BossMesh.GLOW);
        int col = BossMesh.argb(0x101018, alpha);
        Vector3f a = new Vector3f(at).add(-radius, .02f, -radius), b = new Vector3f(at).add(radius, .02f, -radius);
        Vector3f d = new Vector3f(at).add(radius, .02f, radius), e = new Vector3f(at).add(-radius, .02f, radius);
        flat(c, poseStack.last(), a, b, d, e, 0, 0, 1, 0, 1, 1, 0, 1, col, col, FULL);
    }

    /** A flat ring of light on the ground from {@code r0} to {@code r1}, at {@code y}. */
    public static void groundRing(PoseStack poseStack, MultiBufferSource buffer, float y, float r0, float r1, int rgb, float alpha) {
        if (alpha > .01f && r1 > r0) {
            ClockMesh.ring(poseStack.last(), buffer, y, Math.max(0, r0), r1, rgb, alpha, FULL);
        }
    }

    /** {@code d} as a float vector. */
    public static Vector3f v(net.minecraft.world.phys.Vec3 d) {
        return new Vector3f((float) d.x, (float) d.y, (float) d.z);
    }
}
