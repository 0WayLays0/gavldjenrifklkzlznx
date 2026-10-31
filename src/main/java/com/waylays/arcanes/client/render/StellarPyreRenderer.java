package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.fire.FireMesh;
import com.waylays.arcanes.client.render.fire.PyreModel;
import com.waylays.arcanes.entity.fire.StellarPyre;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** The Pyre of Six Fires: drawn by {@link PyreModel} round the caster, where they are drawn. */
public class StellarPyreRenderer extends EntityRenderer<StellarPyre> {
    public StellarPyreRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(StellarPyre e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = e.tickCount + partialTicks;
        Entity owner = e.follows();
        Vec3 shift = owner != null && age < StellarPyre.LIFE ? owner.getPosition(partialTicks).subtract(e.getPosition(partialTicks)) : Vec3.ZERO;
        poseStack.pushPose();
        poseStack.translate(shift.x, shift.y, shift.z);
        PyreModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, e, partialTicks, shift, null), age, StellarPyre.tier(age), StellarPyre.step(age),
                StellarPyre.grow(age), StellarPyre.faded(age), StellarPyre.RADIUS, StellarPyre.HEIGHT, e.getId() * 71, light);
        poseStack.popPose();
        super.render(e, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(StellarPyre e, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(e.getBoundingBox().inflate(12, StellarPyre.HEIGHT + 6, 12));
    }

    @Override
    public ResourceLocation getTextureLocation(StellarPyre e) {
        return FireMesh.GLOW;
    }
}
