package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.client.render.fire.EmberBoltModel;
import com.waylays.arcanes.client.render.fire.FireMesh;
import com.waylays.arcanes.entity.fire.EmberBolt;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Ember Bolt: drawn by {@link EmberBoltModel}, along the way it flies. */
public class EmberBoltRenderer extends EntityRenderer<EmberBolt> {
    public EmberBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EmberBolt e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = e.tickCount + partialTicks;
        Quaternionf turn = Axis.YP.rotationDegrees(Mth.lerp(partialTicks, e.yRotO, e.getYRot())).mul(Axis.XP.rotationDegrees(-Mth.lerp(partialTicks, e.xRotO, e.getXRot())));
        Vector3f forward = turn.transform(new Vector3f(0, 0, 1));
        float mid = e.getBbHeight() * .5f;
        poseStack.pushPose();
        poseStack.translate(0, mid, 0);
        EmberBoltModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, e, partialTicks, new Vec3(0, mid, 0), null), forward, age, e.getHeat(),
                e.getId() * 31, light);
        poseStack.popPose();
        super.render(e, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(EmberBolt e, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(EmberBolt e) {
        return FireMesh.GLOW;
    }
}
