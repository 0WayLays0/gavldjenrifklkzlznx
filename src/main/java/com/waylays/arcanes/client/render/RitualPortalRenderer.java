package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.client.render.ritual.PortalModel;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.entity.ritual.RitualPortal;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** The Portal: an upright oval gate facing its yaw, opening from a seam of light and folding shut (see {@link PortalModel}). */
public class RitualPortalRenderer extends EntityRenderer<RitualPortal> {
    private static final int COLOR = 0xC27BFF;
    private static final float OPEN = 30, CLOSE = 30;

    public RitualPortalRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RitualPortal portal, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = portal.tickCount + partialTicks;
        float open = Mth.clamp(age / OPEN, 0, 1);
        float closing = Mth.clamp((age - (portal.getLife() - CLOSE)) / CLOSE, 0, 1);
        poseStack.pushPose();
        Quaternionf turn = Axis.YP.rotationDegrees(-portal.getYRot());
        poseStack.mulPose(turn);
        PortalModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, portal, partialTicks, Vec3.ZERO, turn), age, open, closing, COLOR,
                portal.getId() * 17, packedLight);
        poseStack.popPose();
        super.render(portal, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(RitualPortal portal, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(RitualPortal portal) {
        return RitualMesh.SWIRL;
    }
}
