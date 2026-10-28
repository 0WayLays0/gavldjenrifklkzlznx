package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.water.TidalWave;
import com.waylays.arcanes.spells.water.WaterFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Tidal Wave: a wall of sea across the way, its back sloping, its face steep and hollow, its crest curling over in a
 * lip of white water; deep blue at its foot, bright green-blue in its thin crest; the crest rising and falling along its
 * length and tapering away at its ends; foam churning at its foot, spray torn off its crest; at the end the lip pitches
 * down and the whole wave collapses.
 */
public class TidalWaveRenderer extends EntityRenderer<TidalWave> {
    private static final int ACROSS = 26;
    /** The wave's profile, back to front: (forward, up) of a wave one high, from its back foot over the crest to its lip. */
    private static final float[][] PROFILE = {
            {-3.6f, 0}, {-2.6f, .22f}, {-1.7f, .5f}, {-.9f, .78f}, {-.3f, .94f}, {.15f, 1}, {.6f, .99f}, {1.0f, .9f}, {1.25f, .76f}, {1.3f, .62f}};
    /** Its face, from under the lip down to its front foot. */
    private static final float[][] FACE = {{1.05f, .68f}, {.8f, .52f}, {.62f, .34f}, {.55f, .16f}, {.6f, 0}};

    public TidalWaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(TidalWave wave, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = wave.tickCount + partialTicks;
        float stand = wave.stand(age);
        if (stand <= .01f) {
            return;
        }
        float broken = wave.broken(age);
        int seed = wave.getId() * 83;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-wave.getYRot()));
        VertexConsumer water = WaterRender.water(buffer);
        PoseStack.Pose p = poseStack.last();
        float w = TidalWave.WIDTH;
        // The wave: strips across its width along its profile, then its face
        for (int i = 0; i < ACROSS; i++) {
            float x0 = -w * .5f + w * i / ACROSS, x1 = -w * .5f + w * (i + 1) / ACROSS;
            strip(water, p, PROFILE, x0, x1, age, stand, broken, seed, light, true);
            strip(water, p, FACE, x0, x1, age, stand, broken, seed, light, false);
        }
        // White water: along its lip and churning at its foot
        VertexConsumer foam = WaterRender.foam(buffer);
        p = poseStack.last();
        int n = ACROSS + 1;
        Vector3f[] lip = new Vector3f[n], foot = new Vector3f[n], side = new Vector3f[n];
        float[] lw = new float[n], la = new float[n], fw = new float[n], fa = new float[n];
        for (int i = 0; i < n; i++) {
            float x = -w * .5f + w * i / ACROSS;
            float hh = height(x, age, seed) * stand;
            float[] tip = curl(PROFILE[PROFILE.length - 2], broken);
            lip[i] = new Vector3f(x, tip[1] * hh, tip[0] * Math.max(hh, .5f) / TidalWave.HEIGHT * 2.2f);
            foot[i] = new Vector3f(x, .06f, .9f + .2f * Mth.sin(age * .3f + i));
            side[i] = new Vector3f(0, .3f, 1).normalize();
            lw[i] = 1.1f;
            la[i] = .8f * taper(x);
            fw[i] = 1.6f;
            fa[i] = .7f * taper(x) * stand;
        }
        WaterRender.ribbon(foam, p, lip, side, lw, la, 0xFFFFFF, age * .05f, light);
        Vector3f[] flat = new Vector3f[n];
        for (int i = 0; i < n; i++) {
            flat[i] = new Vector3f(0, 0, 1);
        }
        WaterRender.ribbon(foam, p, foot, flat, fw, fa, 0xFFFFFF, age * .04f, light);
        // Spray torn off its crest
        VertexConsumer drops = WaterRender.drops(buffer);
        Quaternionf camera = this.entityRenderDispatcher.cameraOrientation();
        for (int k = 0; k < 50; k++) {
            float t = (age * .07f + WaterFx.hash(seed + k)) % 1;
            float x = (WaterFx.hash(seed + k * 3) - .5f) * w * .9f;
            float hh = height(x, age, seed) * stand;
            WaterRender.drop(drops, poseStack, camera, x, hh * (1 + .3f * t) + 1.2f * t - 1.8f * t * t, .3f - 1.6f * t, .2f,
                    RuneRender.alpha(WaterFx.FOAM, .85f * (1 - t) * taper(x)), light);
        }
        poseStack.popPose();
        super.render(wave, entityYaw, partialTicks, poseStack, buffer, light);
    }

    /** The wave's height across it: rising and falling along its length, tapering away at its ends. */
    private static float height(float x, float age, int seed) {
        float rolling = 1 + .1f * Mth.sin(x * .7f + age * .12f + seed) + .06f * Mth.sin(x * 1.9f - age * .2f);
        return TidalWave.HEIGHT * rolling * taper(x);
    }

    private static float taper(float x) {
        float e = Math.abs(x) / (TidalWave.WIDTH * .5f);
        return Mth.clamp((1 - e) * 4, 0, 1) * (.6f + .4f * (1 - e * e));
    }

    /** A point of the profile with its lip pitched down by {@code broken}. */
    private static float[] curl(float[] pt, float broken) {
        if (pt[0] < .5f || broken <= 0) {
            return pt;
        }
        float k = (pt[0] - .5f) * broken;
        return new float[]{pt[0] + k * .8f, pt[1] - k * .9f};
    }

    /** One strip of the wave, from x0 to x1, along {@code profile}; deep at its foot, bright in its crest. */
    private static void strip(VertexConsumer c, PoseStack.Pose p, float[][] profile, float x0, float x1, float age, float stand, float broken, int seed,
                              int light, boolean back) {
        float h0 = height(x0, age, seed) * stand, h1 = height(x1, age, seed) * stand;
        float scale = TidalWave.HEIGHT;
        for (int j = 0; j + 1 < profile.length; j++) {
            float[] a = curl(profile[j], broken), b = curl(profile[j + 1], broken);
            // Its length front to back grows with its height
            float za0 = a[0] * Math.max(h0, .5f) / scale * 2.2f, zb0 = b[0] * Math.max(h0, .5f) / scale * 2.2f;
            float za1 = a[0] * Math.max(h1, .5f) / scale * 2.2f, zb1 = b[0] * Math.max(h1, .5f) / scale * 2.2f;
            float shade0 = back ? a[1] : .3f + .5f * a[1], shade1 = back ? b[1] : .3f + .5f * b[1];
            int ca = RuneRender.alpha(WaterRender.sea(.15f + .85f * shade0), .62f + .2f * (1 - shade0));
            int cb = RuneRender.alpha(WaterRender.sea(.15f + .85f * shade1), .62f + .2f * (1 - shade1));
            float u0 = x0 * .25f + age * .02f, u1 = x1 * .25f + age * .02f;
            float v0 = j * .3f - age * .05f, v1 = (j + 1) * .3f - age * .05f;
            RenderHelper.vertex(c, p, x0, a[1] * h0, za0, u0, v0, ca, light, 0, 1, 0);
            RenderHelper.vertex(c, p, x0, b[1] * h0, zb0, u0, v1, cb, light, 0, 1, 0);
            RenderHelper.vertex(c, p, x1, b[1] * h1, zb1, u1, v1, cb, light, 0, 1, 0);
            RenderHelper.vertex(c, p, x1, a[1] * h1, za1, u1, v0, ca, light, 0, 1, 0);
        }
    }

    @Override
    public boolean shouldRender(TidalWave wave, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(wave.getBoundingBox().inflate(TidalWave.WIDTH, TidalWave.HEIGHT + 3, TidalWave.WIDTH));
    }

    @Override
    public ResourceLocation getTextureLocation(TidalWave wave) {
        return WaterRender.WATER;
    }
}
