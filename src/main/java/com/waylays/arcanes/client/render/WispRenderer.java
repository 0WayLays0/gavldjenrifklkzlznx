package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.fire.FireMesh;
import com.waylays.arcanes.client.render.fire.WispModel;
import com.waylays.arcanes.entity.fire.Wisp;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** A will-o'-wisp: drawn by {@link WispModel}. */
public class WispRenderer extends EntityRenderer<Wisp> {
    public WispRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(Wisp e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = e.tickCount + partialTicks;
        float fade = e.getEndTick() < 0 ? 0 : Math.min(1, (age - e.getEndTick()) / Wisp.FADE);
        Vec3 v = e.getPosition(partialTicks).subtract(e.xo, e.yo, e.zo);
        Vector3f trail = v.lengthSqr() < 1.0E-5 ? new Vector3f(0, 0, 1) : v.toVector3f();
        WispModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, e, partialTicks), trail, age, fade, e.getHeat(), e.getId() * 47, light);
        super.render(e, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(Wisp e, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(e.getBoundingBox().inflate(3));
    }

    @Override
    public ResourceLocation getTextureLocation(Wisp e) {
        return FireMesh.GLOW;
    }
}
