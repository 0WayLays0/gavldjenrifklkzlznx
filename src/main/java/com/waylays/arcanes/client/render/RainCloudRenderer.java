package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.entity.water.RainCloud;
import com.waylays.arcanes.spells.water.WaterFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * Cleansing Rain: a small storm cloud over the caster (puffs heaped together, pale on top and dark slate underneath,
 * slowly churning), rain falling from it in streaks, rings spreading where the drops land, and a faint blue circle on
 * the ground where its rain reaches.
 */
public class RainCloudRenderer extends EntityRenderer<RainCloud> {
    private static final int PUFFS = 26;
    private static final int STREAKS = 120;
    private static final int RIPPLES = 30;

    public RainCloudRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RainCloud cloud, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = cloud.tickCount + partialTicks;
        float thick = cloud.thickness(age);
        if (thick <= .01f) {
            return;
        }
        Entity owner = cloud.follows();
        poseStack.pushPose();
        if (owner != null) {
            // Over the caster where they are drawn, not a tick behind
            Vec3 at = owner.getPosition(partialTicks);
            poseStack.translate(at.x - Mth.lerp(partialTicks, cloud.xo, cloud.getX()), at.y - Mth.lerp(partialTicks, cloud.yo, cloud.getY()),
                    at.z - Mth.lerp(partialTicks, cloud.zo, cloud.getZ()));
        }
        int seed = cloud.getId() * 61;
        float r = RainCloud.RADIUS;
        float top = RainCloud.HEIGHT;
        Quaternionf camera = this.entityRenderDispatcher.cameraOrientation();
        // Rain falling in streaks
        VertexConsumer rain = buffer.getBuffer(RenderType.entityTranslucent(WaterRender.RAIN));
        for (int k = 0; k < STREAKS * thick; k++) {
            float a = WaterFx.hash(seed + k * 3) * Mth.TWO_PI;
            float rr = Mth.sqrt(WaterFx.hash(seed + k * 3 + 1)) * r * .9f;
            float t = (age * (.09f + .03f * WaterFx.hash(seed + k)) + WaterFx.hash(seed + k * 7)) % 1;
            float y = top - .3f - t * top;
            Vec3 a0 = new Vec3(Mth.cos(a) * rr, y + .55f, Mth.sin(a) * rr);
            Vec3 a1 = new Vec3(Mth.cos(a) * rr - .04f, y, Mth.sin(a) * rr);
            GlyphRender.beam(rain, poseStack.last(), a0, a1, .05f, RuneRender.alpha(0xCFE8FF, .7f * thick));
        }
        // Rings spreading where the drops land, and the circle its rain reaches
        VertexConsumer foam = WaterRender.foam(buffer);
        for (int k = 0; k < RIPPLES * thick; k++) {
            float t = (age * .06f + WaterFx.hash(seed + k * 11)) % 1;
            float a = WaterFx.hash(seed + k * 13 + (int) (age * .06f + WaterFx.hash(seed + k * 11))) * Mth.TWO_PI;
            float rr = Mth.sqrt(WaterFx.hash(seed + k * 17 + (int) (age * .06f + WaterFx.hash(seed + k * 11)))) * r * .9f;
            poseStack.pushPose();
            poseStack.translate(Mth.cos(a) * rr, 0, Mth.sin(a) * rr);
            WaterRender.ring(foam, poseStack.last(), .03f, .05f + .35f * t, .1f + .4f * t, 10, 0, RuneRender.alpha(0xFFFFFF, .7f * (1 - t) * thick),
                    RuneRender.alpha(0xFFFFFF, 0), light);
            poseStack.popPose();
        }
        VertexConsumer circle = buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW));
        poseStack.pushPose();
        poseStack.translate(0, .04f, 0);
        RenderHelper.groundQuad(circle, poseStack.last(), r, RuneRender.additive(WaterFx.GLOW, .35f * thick), GlyphRender.FULL);
        poseStack.popPose();
        // The cloud: puffs, pale on top, slate below, churning
        VertexConsumer puffs = buffer.getBuffer(RenderType.entityTranslucent(WaterRender.CLOUD));
        for (int k = 0; k < PUFFS; k++) {
            float a = WaterFx.hash(seed + k * 5) * Mth.TWO_PI + age * .004f * (k % 2 == 0 ? 1 : -1);
            float rr = Mth.sqrt(WaterFx.hash(seed + k * 5 + 1)) * r * .75f;
            float y = top + (WaterFx.hash(seed + k * 5 + 2) - .3f) * 1.1f + .1f * Mth.sin(age * .05f + k);
            float size = (1.4f + 1.1f * WaterFx.hash(seed + k * 5 + 3)) * (.5f + .5f * thick);
            int shade = GoldRender.lerp(0x5A6878, 0xE4ECF4, Mth.clamp((y - top + .4f) / 1.2f, 0, 1));
            poseStack.pushPose();
            poseStack.translate(Mth.cos(a) * rr, y, Mth.sin(a) * rr);
            poseStack.mulPose(camera);
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotation(k + age * .01f));
            RenderHelper.billboard(puffs, poseStack.last(), size, RuneRender.alpha(shade, .85f * thick), light);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(cloud, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(RainCloud cloud, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(cloud.getBoundingBox().inflate(RainCloud.RADIUS + 2, RainCloud.HEIGHT + 3, RainCloud.RADIUS + 2));
    }

    @Override
    public ResourceLocation getTextureLocation(RainCloud cloud) {
        return WaterRender.CLOUD;
    }
}
