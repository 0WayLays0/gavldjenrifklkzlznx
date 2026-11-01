package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.client.render.water.TideLanceModel;
import com.waylays.arcanes.client.render.water.WaterMesh;
import com.waylays.arcanes.entity.water.TideLance;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Tide Lance: drawn by {@link TideLanceModel}, along the way it flies. */
public class TideLanceRenderer extends EntityRenderer<TideLance> {
    public TideLanceRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(TideLance lance, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = lance.tickCount + partialTicks;
        // Its heading is the way it flies (its rotations follow vanilla's projectile convention, not the model's)
        Vec3 v = lance.getDeltaMovement();
        Vector3f forward = v.lengthSqr() > 1.0E-6 ? new Vector3f((float) v.x, (float) v.y, (float) v.z).normalize() : new Vector3f(0, 0, 1);
        float mid = lance.getBbHeight() * .5f;
        poseStack.pushPose();
        poseStack.translate(0, mid, 0);
        TideLanceModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, lance, partialTicks, new Vec3(0, mid, 0), null), forward, age,
                lance.getId() * 41, light);
        poseStack.popPose();
        super.render(lance, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(TideLance lance) {
        return WaterMesh.WATER;
    }
}
