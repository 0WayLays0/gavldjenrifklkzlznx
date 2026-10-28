package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.entity.water.Geyser;
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
 * Geyser: first a pool of foam swelling on the ground, bubbles bursting in it; then a column of water roaring up (a
 * fast bright core inside a churning sheath, wider at the top), a crown of water spilling over at its top and falling
 * in sheets, spray everywhere, foam rings running out over the ground; then it all falls back.
 */
public class GeyserRenderer extends EntityRenderer<Geyser> {
    private static final int SHEETS = 12;

    public GeyserRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(Geyser geyser, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = geyser.tickCount + partialTicks;
        float col = geyser.column(age);
        float warn = Mth.clamp(age / Geyser.WARN, 0, 1);
        float end = Mth.clamp((age - Geyser.WARN - Geyser.RISE - Geyser.SPOUT) / Geyser.FALL, 0, 1);
        int seed = geyser.getId() * 53;
        float r = Geyser.RADIUS;
        // The foam on the ground: swelling while it warns, rings running out while it spouts
        VertexConsumer foam = WaterRender.foam(buffer);
        PoseStack.Pose p = poseStack.last();
        float pool = Math.min(warn, 1 - end);
        WaterRender.ring(foam, p, .04f, 0, r * (.6f + .6f * pool), 24, age * .03f, RuneRender.alpha(0xFFFFFF, .75f * pool), RuneRender.alpha(0xFFFFFF, .2f * pool), light);
        if (col > 0) {
            for (int k = 0; k < 3; k++) {
                float t = (age * .05f + k / 3f) % 1;
                WaterRender.ring(foam, p, .05f, r * (.8f + 2.4f * t), r * (1.1f + 2.4f * t), 28, age * .02f,
                        RuneRender.alpha(0xFFFFFF, .5f * (1 - t) * (1 - end)), RuneRender.alpha(0xFFFFFF, 0), light);
            }
        }
        VertexConsumer water = WaterRender.water(buffer);
        p = poseStack.last();
        // Bubbles bursting in the pool while it warns
        if (col <= 0) {
            for (int k = 0; k < 8; k++) {
                float t = (age * .12f + WaterFx.hash(seed + k)) % 1;
                float a = WaterFx.hash(seed + k * 3) * Mth.TWO_PI, rr = r * .6f * WaterFx.hash(seed + k * 5);
                BoneRender.ball(water, p, new Vector3f(Mth.cos(a) * rr, .05f, Mth.sin(a) * rr), .12f * Mth.sin(t * Mth.PI) * warn, light,
                        RuneRender.alpha(WaterRender.sea(.9f), .6f));
            }
        } else {
            float h = geyser.getHeight() * col;
            // The column: a fast bright core inside a churning sheath, wider at the top
            WaterRender.funnel(water, p, r * .45f, 0, r * .55f, h, 8, 14, 2, .3f, -age * .25f, 0xDDF6FF, .85f, .75f, light);
            WaterRender.funnel(water, p, r * .85f, 0, r * 1.2f, h * 1.02f, 8, 20, 3, -.6f, -age * .14f, WaterRender.sea(.7f), .5f, .35f, light);
            // The crown spilling over at the top, falling in sheets
            for (int k = 0; k < SHEETS; k++) {
                float a = k * Mth.TWO_PI / SHEETS + age * .05f;
                int n = 8;
                Vector3f[] pts = new Vector3f[n];
                Vector3f[] side = new Vector3f[n];
                float[] w = new float[n], al = new float[n];
                for (int i = 0; i < n; i++) {
                    float u = (float) i / (n - 1);
                    float out = r * (1.1f + 1.6f * u);
                    float y = h + .9f * Mth.sin(u * Mth.PI * .7f) - h * .45f * u * u;
                    pts[i] = new Vector3f(Mth.cos(a) * out, y, Mth.sin(a) * out);
                    side[i] = new Vector3f(-Mth.sin(a), 0, Mth.cos(a));
                    w[i] = .9f * (1 - u * .5f);
                    al[i] = .55f * (1 - u) * (1 - end);
                }
                WaterRender.ribbon(water, p, pts, side, w, al, WaterRender.sea(.8f), -age * .1f, light);
            }
        }
        // Spray everywhere
        if (col > 0) {
            VertexConsumer drops = WaterRender.drops(buffer);
            Quaternionf camera = this.entityRenderDispatcher.cameraOrientation();
            float h = geyser.getHeight() * col;
            for (int k = 0; k < 40; k++) {
                float t = (age * .04f + WaterFx.hash(seed + k)) % 1;
                float a = WaterFx.hash(seed + k * 7) * Mth.TWO_PI;
                float out = r * (.8f + 3 * t * WaterFx.hash(seed + k * 11));
                float y = h * (1 - t * t) + t * 1.5f;
                WaterRender.drop(drops, poseStack, camera, Mth.cos(a) * out, y, Mth.sin(a) * out, .18f, RuneRender.alpha(WaterFx.FOAM, .9f * (1 - t)), light);
            }
        }
        super.render(geyser, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(Geyser geyser, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(geyser.getBoundingBox().inflate(Geyser.RADIUS * 4, geyser.getHeight() + 3, Geyser.RADIUS * 4));
    }

    @Override
    public ResourceLocation getTextureLocation(Geyser geyser) {
        return WaterRender.WATER;
    }
}
