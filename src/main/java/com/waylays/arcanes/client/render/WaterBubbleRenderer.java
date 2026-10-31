package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.water.BubbleModel;
import com.waylays.arcanes.client.render.water.WaterMesh;
import com.waylays.arcanes.entity.water.WaterBubble;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Bubble Prison: drawn by {@link BubbleModel} round where its prisoner is now (it rises; the entity stays below). */
public class WaterBubbleRenderer extends EntityRenderer<WaterBubble> {
    public WaterBubbleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(WaterBubble bubble, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = bubble.tickCount + partialTicks;
        float formed = Mth.clamp(age / WaterBubble.FORM, 0, 1);
        formed = 1 - (1 - formed) * (1 - formed);
        float r = bubble.getRadius() * formed;
        if (r <= .01f) {
            return;
        }
        float lift = bubble.footY(age) - (float) Mth.lerp(partialTicks, bubble.yo, bubble.getY());
        float shiver = Mth.clamp((age - bubble.burstTick() + 12) / 12, 0, 1);
        float up = lift + bubble.getRadius() - .45f + .1f;
        poseStack.pushPose();
        poseStack.translate(0, up, 0);
        BubbleModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, bubble, partialTicks, new Vec3(0, up, 0), null), age, r, shiver, 0,
                bubble.getId() * 17, light);
        poseStack.popPose();
        super.render(bubble, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(WaterBubble bubble) {
        return WaterMesh.BUBBLE;
    }
}
