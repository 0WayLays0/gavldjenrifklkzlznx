package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.water.AbyssalDome;
import com.waylays.arcanes.spells.water.WaterFx;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The Abyssal Dome: a dome of deep water, clear blue, the sunlight on its surface rippling; the light that comes through
 * it dancing over the ground in bright caustic lines and falling in slanting shafts; kelp swaying round its rim,
 * bubbles rising, shoals of fish (silver, gold, teal) wheeling round inside, a manta ray gliding round under its roof,
 * its wings beating slowly. It swells out of the caster at first; at its end its roof falls in and the water spreads
 * out over the ground in a ring of foam.
 */
public class AbyssalDomeRenderer extends EntityRenderer<AbyssalDome> {
    private static final int BANDS = 12;
    private static final int SEGS = 48;
    private static final int KELP = 26;
    private static final int RAYS = 9;
    private static final int BUBBLES = 80;
    private static final int[] SHOAL_COLOURS = {0xDCEBF4, 0xFFB45E, 0x7FE6D2};
    private static final int SHOAL = 15;

    public AbyssalDomeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(AbyssalDome dome, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = dome.tickCount + partialTicks;
        float formed = dome.formed(age);
        float fall = dome.collapsed(age);
        if (formed <= .01f || fall >= 1) {
            return;
        }
        float big = dome.getRadius();
        float r = big * formed * (1 + .3f * fall);
        float h = big * formed * (1 - fall) * (1 - fall);
        float fade = 1 - fall * fall;
        int seed = dome.getId() * 37;
        int light = LightTexture.pack(Math.max(LightTexture.block(packedLight), 10), LightTexture.sky(packedLight));
        poseStack.pushPose();

        // Caustic light dancing over the ground, two layers drifting across each other
        VertexConsumer caustics = WaterRender.light(buffer, WaterRender.CAUSTICS);
        PoseStack.Pose p = poseStack.last();
        disc(caustics, p, .07f, r, 6.5f, age * .011f, age * .006f, WaterFx.GLOW, .5f * fade);
        disc(caustics, p, .09f, r, 4.3f, -age * .007f, age * .012f, WaterFx.SHALLOW, .35f * fade);
        // ...and the sun on its surface
        for (int j = 0; j < BANDS; j++) {
            float s0 = band(j), s1 = band(j + 1);
            float glint = .16f * fade * (.6f + .4f * (float) j / BANDS);
            WaterRender.funnel(caustics, p, r * Mth.cos(s0), h * Mth.sin(s0), r * Mth.cos(s1), h * Mth.sin(s1), 1, SEGS, 5, 0, age * .004f,
                    RuneRender.additive(WaterFx.GLOW, glint), 1, 1, LightTexture.FULL_BRIGHT);
        }

        // Shafts of light slanting down through it
        VertexConsumer rays = WaterRender.light(buffer, WaterRender.RAY);
        float yaw = 180f - this.entityRenderDispatcher.camera.getYRot();
        for (int k = 0; k < RAYS; k++) {
            float a = WaterFx.hash(seed + k * 7) * Mth.TWO_PI, d = Mth.sqrt(WaterFx.hash(seed + k * 11)) * r * .7f;
            float x = Mth.cos(a + age * .002f) * d, z = Mth.sin(a + age * .002f) * d;
            float top = h * Mth.sqrt(Math.max(0, 1 - d * d / (r * r)));
            float flicker = .55f + .45f * Mth.sin(age * (.03f + .02f * WaterFx.hash(seed + k)) + k * 2.1f);
            if (top < .5f) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(x, 0, z);
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            float w = 1.2f + 1.6f * WaterFx.hash(seed + k * 3);
            int c = RuneRender.additive(WaterFx.GLOW, .3f * flicker * fade * formed);
            PoseStack.Pose rp = poseStack.last();
            RenderHelper.vertex(rays, rp, -w * .5f + top * .18f, top, 0, 0, 0, c, LightTexture.FULL_BRIGHT, 0, 0, 1);
            RenderHelper.vertex(rays, rp, -w * .5f, 0, 0, 0, 1, c, LightTexture.FULL_BRIGHT, 0, 0, 1);
            RenderHelper.vertex(rays, rp, w * .5f, 0, 0, 1, 1, c, LightTexture.FULL_BRIGHT, 0, 0, 1);
            RenderHelper.vertex(rays, rp, w * .5f + top * .18f, top, 0, 1, 0, c, LightTexture.FULL_BRIGHT, 0, 0, 1);
            poseStack.popPose();
        }

        // The dome of water itself, deeper blue at its foot, paler at its crown
        VertexConsumer water = WaterRender.water(buffer);
        p = poseStack.last();
        for (int j = 0; j < BANDS; j++) {
            float s0 = band(j), s1 = band(j + 1);
            float t0 = (float) j / BANDS, t1 = (float) (j + 1) / BANDS;
            WaterRender.funnel(water, p, r * Mth.cos(s0), h * Mth.sin(s0), r * Mth.cos(s1), h * Mth.sin(s1), 1, SEGS, 6, .15f, age * .006f,
                    WaterRender.sea(.25f + .55f * t0), (.36f - .14f * t0) * fade, (.36f - .14f * t1) * fade, light);
        }

        // Kelp swaying round its rim
        VertexConsumer kelp = buffer.getBuffer(RenderType.entityTranslucent(WaterRender.KELP));
        p = poseStack.last();
        for (int k = 0; k < KELP; k++) {
            float a = k * Mth.TWO_PI / KELP + WaterFx.hash(seed + k) * .2f;
            float d = r * (.74f + .16f * WaterFx.hash(seed + k * 5));
            float tall = Math.min(h * .55f, 3 + 3.5f * WaterFx.hash(seed + k * 9)) * formed;
            int n = 9;
            Vector3f[] pts = new Vector3f[n], across = new Vector3f[n], along = new Vector3f[n];
            float[] width = new float[n], alpha = new float[n];
            for (int i = 0; i < n; i++) {
                float t = (float) i / (n - 1);
                float sway = t * t;
                float sx = Mth.sin(age * .05f + k * 1.7f + t * 2.2f) * .7f * sway, sz = Mth.cos(age * .043f + k * 2.3f + t * 1.6f) * .6f * sway;
                pts[i] = new Vector3f(Mth.cos(a) * d + sx, t * tall, Mth.sin(a) * d + sz);
                across[i] = new Vector3f(-Mth.sin(a), 0, Mth.cos(a));
                along[i] = new Vector3f(Mth.cos(a), 0, Mth.sin(a));
                width[i] = .55f - .3f * t;
                alpha[i] = .95f * fade;
            }
            WaterRender.ribbon(kelp, p, pts, across, width, alpha, 0xFFFFFF, 0, light);
            WaterRender.ribbon(kelp, p, pts, along, width, alpha, 0xE0FFE0, .5f, light);
        }

        // Shoals of fish wheeling round inside
        VertexConsumer fish = buffer.getBuffer(RenderType.entityTranslucent(WaterRender.FISH));
        for (int s = 0; s < SHOAL_COLOURS.length; s++) {
            float dir = s % 2 == 0 ? 1 : -1;
            float rs = r * (.32f + .14f * s);
            float ys = h * (.28f + .14f * s);
            float spin = dir * age * (.018f - .003f * s) + s * 2.1f;
            for (int f = 0; f < SHOAL; f++) {
                int fs = seed + s * 101 + f * 13;
                float th = spin + (WaterFx.hash(fs) - .5f) * 1.1f;
                float dr = (WaterFx.hash(fs + 1) - .5f) * 2.4f;
                float dy = (WaterFx.hash(fs + 2) - .5f) * 1.6f + .25f * Mth.sin(age * .07f + f);
                float x = Mth.cos(th) * (rs + dr), z = Mth.sin(th) * (rs + dr);
                float tx = -Mth.sin(th) * dir, tz = Mth.cos(th) * dir;
                float len = (.55f + .3f * WaterFx.hash(fs + 3)) * formed;
                poseStack.pushPose();
                poseStack.translate(x, Math.max(.6f, ys + dy), z);
                poseStack.mulPose(Axis.YP.rotation((float) Math.atan2(-tz, tx)));
                fish(fish, poseStack.last(), len, age * .9f + f * 1.3f, RuneRender.alpha(SHOAL_COLOURS[s], .95f * fade), light);
                poseStack.popPose();
            }
        }

        // A manta ray gliding round under its roof
        VertexConsumer manta = buffer.getBuffer(RenderType.entityTranslucent(WaterRender.MANTA));
        float mth = -age * .011f + seed;
        float mr = r * .5f;
        poseStack.pushPose();
        poseStack.translate(Mth.cos(mth) * mr, Math.max(1.5f, h * .62f + .5f * Mth.sin(age * .03f)), Mth.sin(mth) * mr);
        // Heading along its round (its head is -z), banking into the turn
        poseStack.mulPose(Axis.YP.rotation(-mth));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-14));
        manta(manta, poseStack.last(), 5.2f * formed, 4.6f * formed, age, RuneRender.alpha(0xFFFFFF, .97f * fade), light);
        poseStack.popPose();

        // Bubbles rising up through it
        VertexConsumer bubbles = WaterRender.drops(buffer);
        Quaternionf camera = this.entityRenderDispatcher.cameraOrientation();
        for (int k = 0; k < BUBBLES; k++) {
            int bs = seed + k * 17;
            float a = WaterFx.hash(bs) * Mth.TWO_PI, d = Mth.sqrt(WaterFx.hash(bs + 1)) * r * .9f;
            float top = h * Mth.sqrt(Math.max(0, 1 - d * d / (r * r)));
            float t = (age * (.006f + .006f * WaterFx.hash(bs + 2)) + WaterFx.hash(bs + 3)) % 1;
            float x = Mth.cos(a) * d + .2f * Mth.sin(age * .2f + k), z = Mth.sin(a) * d + .2f * Mth.cos(age * .17f + k);
            float size = (.1f + .22f * WaterFx.hash(bs + 4)) * (.7f + .6f * t);
            WaterRender.drop(bubbles, poseStack, camera, x, t * top, z, size, RuneRender.alpha(0xE8FBFF, .75f * fade * Mth.clamp((1 - t) * 6, 0, 1)), light);
        }

        // White water round its foot; spreading out over the ground as it falls
        VertexConsumer foam = WaterRender.foam(buffer);
        p = poseStack.last();
        WaterRender.ring(foam, p, .12f, r - .8f, r + .5f, SEGS, age * .01f, RuneRender.alpha(0xFFFFFF, 0), RuneRender.alpha(0xFFFFFF, .75f * fade), light);
        if (fall > 0) {
            float out = r + 4 * fall;
            WaterRender.ring(foam, p, .1f, r * .2f, out, SEGS, -age * .02f, RuneRender.alpha(0xFFFFFF, .5f * (1 - fall)),
                    RuneRender.alpha(0xFFFFFF, .9f * (1 - fall)), light);
        }
        poseStack.popPose();
        super.render(dome, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** The angle up the dome of band edge {@code j}, from its foot (0) to its crown. */
    private static float band(int j) {
        return Mth.HALF_PI * j / BANDS;
    }

    /** A flat disc of additive light on the ground, its texture {@code scale} blocks wide, scrolled by (du, dv), fading at its rim. */
    private static void disc(VertexConsumer c, PoseStack.Pose p, float y, float r, float scale, float du, float dv, int rgb, float strength) {
        int rings = 8;
        for (int j = 0; j < rings; j++) {
            float r0 = r * j / rings, r1 = r * (j + 1) / rings;
            int c0 = RuneRender.additive(rgb, strength * (1 - (float) Math.pow((float) j / rings, 4)));
            int c1 = RuneRender.additive(rgb, strength * (1 - (float) Math.pow((float) (j + 1) / rings, 4)));
            for (int i = 0; i < SEGS; i++) {
                float a0 = i * Mth.TWO_PI / SEGS, a1 = (i + 1) * Mth.TWO_PI / SEGS;
                float x00 = Mth.cos(a0) * r0, z00 = Mth.sin(a0) * r0, x01 = Mth.cos(a0) * r1, z01 = Mth.sin(a0) * r1;
                float x11 = Mth.cos(a1) * r1, z11 = Mth.sin(a1) * r1, x10 = Mth.cos(a1) * r0, z10 = Mth.sin(a1) * r0;
                RenderHelper.vertex(c, p, x00, y, z00, x00 / scale + du, z00 / scale + dv, c0, LightTexture.FULL_BRIGHT, 0, 1, 0);
                RenderHelper.vertex(c, p, x01, y, z01, x01 / scale + du, z01 / scale + dv, c1, LightTexture.FULL_BRIGHT, 0, 1, 0);
                RenderHelper.vertex(c, p, x11, y, z11, x11 / scale + du, z11 / scale + dv, c1, LightTexture.FULL_BRIGHT, 0, 1, 0);
                RenderHelper.vertex(c, p, x10, y, z10, x10 / scale + du, z10 / scale + dv, c0, LightTexture.FULL_BRIGHT, 0, 1, 0);
            }
        }
    }

    /**
     * A fish {@code len} long, head to +x: its side (the texture) upright, and again lying flat so it shows from above;
     * its back half flicking from side to side as it swims.
     */
    private static void fish(VertexConsumer c, PoseStack.Pose p, float len, float phase, int argb, int light) {
        float half = len * .5f, tall = len * .2f;
        float flick = Mth.sin(phase) * len * .22f;
        // Upright: front half, then the tail half bending sideways (z)
        RenderHelper.vertex(c, p, 0, -tall, 0, .5f, 1, argb, light, 0, 0, 1);
        RenderHelper.vertex(c, p, half, -tall, 0, 1, 1, argb, light, 0, 0, 1);
        RenderHelper.vertex(c, p, half, tall, 0, 1, 0, argb, light, 0, 0, 1);
        RenderHelper.vertex(c, p, 0, tall, 0, .5f, 0, argb, light, 0, 0, 1);
        RenderHelper.vertex(c, p, -half, -tall, flick, 0, 1, argb, light, 0, 0, 1);
        RenderHelper.vertex(c, p, 0, -tall, 0, .5f, 1, argb, light, 0, 0, 1);
        RenderHelper.vertex(c, p, 0, tall, 0, .5f, 0, argb, light, 0, 0, 1);
        RenderHelper.vertex(c, p, -half, tall, flick, 0, 0, argb, light, 0, 0, 1);
        // Flat: the same, so it shows from above and below
        float wide = tall * .8f;
        RenderHelper.vertex(c, p, 0, 0, wide, .5f, 1, argb, light, 0, 1, 0);
        RenderHelper.vertex(c, p, half, 0, wide, 1, 1, argb, light, 0, 1, 0);
        RenderHelper.vertex(c, p, half, 0, -wide, 1, 0, argb, light, 0, 1, 0);
        RenderHelper.vertex(c, p, 0, 0, -wide, .5f, 0, argb, light, 0, 1, 0);
        RenderHelper.vertex(c, p, -half, 0, wide + flick, 0, 1, argb, light, 0, 1, 0);
        RenderHelper.vertex(c, p, 0, 0, wide, .5f, 1, argb, light, 0, 1, 0);
        RenderHelper.vertex(c, p, 0, 0, -wide, .5f, 0, argb, light, 0, 1, 0);
        RenderHelper.vertex(c, p, -half, 0, -wide + flick, 0, 0, argb, light, 0, 1, 0);
    }

    /**
     * A manta ray seen from above (the texture: wings across it, head to its top), {@code span} wide and {@code chord}
     * long, swimming to -z; laid on a bending grid so its wings beat in a slow wave from its body out to their tips and
     * its tail sways.
     */
    private static void manta(VertexConsumer c, PoseStack.Pose p, float span, float chord, float age, int argb, int light) {
        int nx = 12, nz = 6;
        float[][] y = new float[nx + 1][nz + 1];
        float[][] xs = new float[nx + 1][nz + 1];
        for (int i = 0; i <= nx; i++) {
            float u = (float) i / nx;
            float out = Math.abs(u - .5f) * 2;
            for (int j = 0; j <= nz; j++) {
                float v = (float) j / nz;
                y[i][j] = Mth.sin(age * .16f - out * 1.6f - v * .8f) * (float) Math.pow(out, 1.5f) * span * .17f;
                xs[i][j] = (u - .5f) * span + (v > .6f ? Mth.sin(age * .12f - v * 3) * (v - .6f) * chord * .25f : 0);
            }
        }
        for (int i = 0; i < nx; i++) {
            for (int j = 0; j < nz; j++) {
                float u0 = (float) i / nx, u1 = (float) (i + 1) / nx, v0 = (float) j / nz, v1 = (float) (j + 1) / nz;
                float z0 = (v0 - .5f) * chord, z1 = (v1 - .5f) * chord;
                RenderHelper.vertex(c, p, xs[i][j], y[i][j], z0, u0, v0, argb, light, 0, 1, 0);
                RenderHelper.vertex(c, p, xs[i][j + 1], y[i][j + 1], z1, u0, v1, argb, light, 0, 1, 0);
                RenderHelper.vertex(c, p, xs[i + 1][j + 1], y[i + 1][j + 1], z1, u1, v1, argb, light, 0, 1, 0);
                RenderHelper.vertex(c, p, xs[i + 1][j], y[i + 1][j], z0, u1, v0, argb, light, 0, 1, 0);
            }
        }
    }

    @Override
    public boolean shouldRender(AbyssalDome dome, Frustum frustum, double x, double y, double z) {
        float r = dome.getRadius() + 6;
        return frustum.isVisible(dome.getBoundingBox().inflate(r, r, r));
    }

    @Override
    public ResourceLocation getTextureLocation(AbyssalDome dome) {
        return WaterRender.WATER;
    }
}
