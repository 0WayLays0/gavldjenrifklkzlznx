package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.water.Typhoon;
import com.waylays.arcanes.spells.water.WaterFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Typhoon: a waterspout as tall as a tower: a bright core twisting fast inside a body of water turning in spiral
 * bands, a veil of mist round it, bands of foam winding up it; a heaving mound of foam and spray at its foot; at its top a
 * storm cloud spreading out, lightning flickering in it and down the column; drops spiralling up.
 */
public class TyphoonRenderer extends EntityRenderer<Typhoon> {
    private static final int BANDS = 4;
    private static final int PUFFS = 22;

    public TyphoonRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    private static float radius(float t) {
        return Typhoon.RADIUS * (.55f + .45f * t * t) + .4f;
    }

    @Override
    public void render(Typhoon typhoon, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = typhoon.tickCount + partialTicks;
        float s = typhoon.strength(age);
        if (s <= .01f) {
            return;
        }
        float h = Typhoon.HEIGHT * (.3f + .7f * s);
        int seed = typhoon.getId() * 71;
        boolean flash = typhoon.flashing(age);
        Quaternionf camera = this.entityRenderDispatcher.cameraOrientation();
        VertexConsumer water = WaterRender.water(buffer);
        PoseStack.Pose p = poseStack.last();
        // The column: core, body, veil, each a stack of slices (narrow at the foot, wide at the top)
        int steps = 8;
        for (int j = 0; j < steps; j++) {
            float t0 = (float) j / steps, t1 = (float) (j + 1) / steps;
            float y0 = t0 * h, y1 = t1 * h;
            WaterRender.funnel(water, p, radius(t0) * .3f, y0, radius(t1) * .3f, y1, 1, 12, 2, .4f, -age * .3f, 0xDDF4FF, .8f * s, .7f * s, light);
            WaterRender.funnel(water, p, radius(t0) * .75f, y0, radius(t1) * .75f, y1, 1, 24, 4, .5f, -age * .12f + age * .02f, WaterRender.sea(.6f), .5f * s, .45f * s, light);
            WaterRender.funnel(water, p, radius(t0) * 1.05f, y0, radius(t1) * 1.05f, y1, 1, 28, 3, -.3f, -age * .06f, 0xC8DCEA, .22f * s, .2f * s, light);
        }
        // Bands of foam winding up it
        VertexConsumer foam = WaterRender.foam(buffer);
        p = poseStack.last();
        for (int b = 0; b < BANDS; b++) {
            int n = 24;
            Vector3f[] pts = new Vector3f[n];
            Vector3f[] side = new Vector3f[n];
            float[] w = new float[n], al = new float[n];
            for (int i = 0; i < n; i++) {
                float u = (float) i / (n - 1);
                float a = b * Mth.TWO_PI / BANDS + u * 9 + age * .2f;
                float r = radius(u) * .8f;
                pts[i] = new Vector3f(Mth.cos(a) * r, u * h, Mth.sin(a) * r);
                side[i] = new Vector3f(0, 1, 0);
                w[i] = .5f + .4f * u;
                al[i] = .6f * Mth.sin(u * Mth.PI) * s;
            }
            WaterRender.ribbon(foam, p, pts, side, w, al, 0xFFFFFF, age * .03f, light);
        }
        // A heaving mound of foam at its foot
        for (int k = 0; k < 3; k++) {
            float t = (age * .04f + k / 3f) % 1;
            WaterRender.ring(foam, p, .05f + .1f * k, Typhoon.RADIUS * (.5f + 1.2f * t), Typhoon.RADIUS * (.9f + 1.4f * t), 32, age * .04f,
                    RuneRender.alpha(0xFFFFFF, .65f * (1 - t) * s), RuneRender.alpha(0xFFFFFF, 0), light);
        }
        // The storm cloud spreading out at its top
        VertexConsumer puffs = buffer.getBuffer(RenderType.entityTranslucent(WaterRender.CLOUD));
        for (int k = 0; k < PUFFS; k++) {
            float a = WaterFx.hash(seed + k * 3) * Mth.TWO_PI + age * .015f;
            float rr = (1.5f + 5.5f * WaterFx.hash(seed + k * 3 + 1)) * s;
            float y = h + (WaterFx.hash(seed + k * 3 + 2) - .3f) * 1.6f;
            float size = 2.2f + 1.6f * WaterFx.hash(seed + k * 7);
            int shade = flash ? 0xE6F0FF : GoldRender.lerp(0x3C4654, 0x8A96A6, WaterFx.hash(seed + k * 11));
            poseStack.pushPose();
            poseStack.translate(Mth.cos(a) * rr, y, Mth.sin(a) * rr);
            poseStack.mulPose(camera);
            poseStack.mulPose(Axis.ZP.rotation(k + age * .01f));
            RenderHelper.billboard(puffs, poseStack.last(), size, RuneRender.alpha(shade, .9f * s), light);
            poseStack.popPose();
        }
        // Drops spiralling up
        VertexConsumer drops = WaterRender.drops(buffer);
        for (int k = 0; k < 60; k++) {
            float t = (age * .012f + WaterFx.hash(seed + k)) % 1;
            float a = WaterFx.hash(seed + k * 5) * Mth.TWO_PI + age * .3f;
            float r = radius(t) * (1.1f + .3f * WaterFx.hash(seed + k * 7));
            WaterRender.drop(drops, poseStack, camera, Mth.cos(a) * r, t * h, Mth.sin(a) * r, .22f, RuneRender.alpha(WaterFx.FOAM, .8f * s), light);
        }
        // Lightning in the cloud and down the column
        if (flash) {
            VertexConsumer bolt = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
            int cycle = (int) (age / 37);
            for (int b = 0; b < 3; b++) {
                Vec3 prev = new Vec3((WaterFx.hash(seed + cycle * 7 + b) - .5f) * 6, h + .5, (WaterFx.hash(seed + cycle * 7 + b + 3) - .5f) * 6);
                for (int k = 0; k < 7; k++) {
                    Vec3 next = prev.add((WaterFx.hash(seed + cycle * 31 + b * 7 + k) - .5f) * 2.2f, -h * .1f, (WaterFx.hash(seed + cycle * 37 + b * 7 + k) - .5f) * 2.2f);
                    GlyphRender.beam(bolt, poseStack.last(), prev, next, .22f, RuneRender.additive(0xCFE4FF, .95f));
                    GlyphRender.beam(bolt, poseStack.last(), prev, next, .6f, RuneRender.additive(0x6A8CFF, .4f));
                    prev = next;
                }
            }
        }
        super.render(typhoon, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(Typhoon typhoon, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(typhoon.getBoundingBox().inflate(10, Typhoon.HEIGHT + 4, 10));
    }

    @Override
    public ResourceLocation getTextureLocation(Typhoon typhoon) {
        return WaterRender.WATER;
    }
}
