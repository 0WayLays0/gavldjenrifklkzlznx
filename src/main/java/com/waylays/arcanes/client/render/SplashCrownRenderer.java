package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.water.SplashCrown;
import com.waylays.arcanes.spells.water.WaterFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A splash: a crown of water (a ring of points leaning out, joined by a thin wall) leaping up and falling back, a ring
 * of foam spreading over the ground, drops thrown out and falling.
 */
public class SplashCrownRenderer extends EntityRenderer<SplashCrown> {
    private static final int POINTS = 14;
    private static final int DROPS = 26;

    public SplashCrownRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SplashCrown crown, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = crown.tickCount + partialTicks;
        float t = Mth.clamp(age / SplashCrown.LIFE, 0, 1);
        float s = crown.getSize();
        // Up fast, then down: its height
        float up = Mth.sin(Mth.PI * Math.min(1, t * 1.3f)) * (1 - t * .3f);
        float fade = 1 - t * t;
        int seed = crown.getId() * 97;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(crown.getYRot()));
        VertexConsumer water = WaterRender.water(buffer);
        PoseStack.Pose p = poseStack.last();
        float r = s * (.45f + .9f * t);
        float h = s * 1.3f * up;
        // The crown's wall
        WaterRender.funnel(water, p, r * .9f, 0, r * 1.15f, h * .55f, 2, 20, 3, .2f, age * .05f, WaterRender.sea(.7f), .55f * fade, .25f * fade, light);
        // Its points, leaning out, a bead at each tip
        for (int k = 0; k < POINTS; k++) {
            float a = k * Mth.TWO_PI / POINTS + WaterFx.hash(seed + k) * .3f;
            float len = h * (.7f + .5f * WaterFx.hash(seed + k + 3));
            Vector3f base = new Vector3f(Mth.cos(a) * r, 0, Mth.sin(a) * r);
            Vector3f tip = new Vector3f(Mth.cos(a) * (r + len * .35f), len, Mth.sin(a) * (r + len * .35f));
            BoneRender.tube(water, p, base, tip, .09f * s, .03f * s, 6, light, RuneRender.alpha(WaterRender.sea(.85f), .6f * fade));
            BoneRender.ball(water, p, tip, .07f * s * (1 - t * .5f), light, RuneRender.alpha(WaterFx.FOAM, .7f * fade));
        }
        // Foam spreading over the ground
        VertexConsumer foam = WaterRender.foam(buffer);
        WaterRender.ring(foam, poseStack.last(), .04f, r * .6f, r * 1.6f, 24, age * .02f, RuneRender.alpha(0xFFFFFF, .7f * fade), RuneRender.alpha(0xFFFFFF, 0), light);
        poseStack.popPose();
        // Drops thrown out and falling
        VertexConsumer drops = WaterRender.drops(buffer);
        Quaternionf camera = this.entityRenderDispatcher.cameraOrientation();
        for (int k = 0; k < DROPS; k++) {
            float a = WaterFx.hash(seed + k * 5) * Mth.TWO_PI;
            float out = (.08f + .12f * WaterFx.hash(seed + k * 5 + 1)) * s;
            float vy = (.25f + .25f * WaterFx.hash(seed + k * 5 + 2)) * s;
            float y = vy * age - .03f * age * age;
            if (y < 0) {
                continue;
            }
            WaterRender.drop(drops, poseStack, camera, Mth.cos(a) * (r + out * age), y, Mth.sin(a) * (r + out * age), .14f * s,
                    RuneRender.alpha(WaterFx.FOAM, .9f * fade), light);
        }
        super.render(crown, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SplashCrown crown) {
        return WaterRender.WATER;
    }
}
