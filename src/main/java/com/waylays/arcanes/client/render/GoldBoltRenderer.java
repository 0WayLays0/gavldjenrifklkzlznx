package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.gold.GoldBoltProjectile;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Gold Bolt: a full-bright gold ingot rolling along its flight path inside a pulsing golden halo.
 */
public class GoldBoltRenderer extends EntityRenderer<GoldBoltProjectile> {
    private static final ResourceLocation GLOW = ArcanesOublies.id("textures/entity/gold_glow.png");

    private final ItemRenderer itemRenderer;
    private final ItemStack ingot = new ItemStack(Items.GOLD_INGOT);

    public GoldBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(GoldBoltProjectile entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        poseStack.pushPose();
        poseStack.translate(0, entity.getBbHeight() * .5f, 0);

        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180f));
        float pulse = 1.15f + Mth.sin(age * .8f) * .15f;
        poseStack.scale(pulse, pulse, pulse);
        RenderHelper.billboard(buffer.getBuffer(RenderType.eyes(GLOW)), poseStack.last(), .5f, 0xFFFFFFFF, LightTexture.FULL_BRIGHT);
        poseStack.popPose();

        // Local +X along the flight direction, then roll around it
        Vec3 motion = entity.getDeltaMovement();
        float yaw = (float) Mth.atan2(motion.z, motion.x);
        float pitch = (float) Mth.atan2(motion.y, motion.horizontalDistance());
        poseStack.mulPose(Axis.YP.rotation(-yaw));
        poseStack.mulPose(Axis.ZP.rotation(pitch));
        poseStack.mulPose(Axis.XP.rotationDegrees(age * 35f));
        poseStack.scale(.9f, .9f, .9f);
        itemRenderer.renderStatic(ingot, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(GoldBoltProjectile entity) {
        return GLOW;
    }
}
