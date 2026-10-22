package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.gold.GoldBoltProjectile;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Gold Bolt: an ingot of polished gold rolling end over end along its flight, its edges hot, a streak of molten light
 * behind it and a little halo.
 */
public class GoldBoltRenderer extends EntityRenderer<GoldBoltProjectile> {
    public GoldBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GoldBoltProjectile bolt, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = bolt.tickCount + partialTicks;
        Vec3 mid = new Vec3(0, bolt.getBbHeight() * .5, 0);
        Vec3 v = bolt.getDeltaMovement();
        if (v.lengthSqr() > 1.0E-4) {
            GemRender.ray(poseStack, buffer, mid, mid.subtract(v.normalize().scale(2.4)), GoldRender.MOLTEN, .16f, .75f, age);
        }
        poseStack.pushPose();
        poseStack.translate(mid.x, mid.y, mid.z);
        if (v.lengthSqr() > 1.0E-4) {
            poseStack.mulPose(Axis.YP.rotation((float) Mth.atan2(v.x, v.z)));
            poseStack.mulPose(Axis.XP.rotation((float) -Mth.atan2(v.y, v.horizontalDistance())));
        }
        // Rolling end over end
        poseStack.mulPose(Axis.XP.rotationDegrees(age * 32));
        poseStack.translate(0, -.08, 0);
        GoldRender.solid(poseStack, buffer, GoldRender.INGOT, GoldRender.GOLD, .75f, age * 3, 1);
        GemRender.sparkles(poseStack, buffer, GemRender.Cut.BRILLIANT, GoldRender.PALE, .6f, age * 3, false, bolt.getId());
        poseStack.popPose();
        GemRender.glow(poseStack, buffer, mid, GoldRender.GOLD, .9f, .55f);
        GemRender.glow(poseStack, buffer, mid, GoldRender.MOLTEN, .45f, .5f);
        super.render(bolt, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(GoldBoltProjectile bolt) {
        return GoldRender.METAL;
    }
}
