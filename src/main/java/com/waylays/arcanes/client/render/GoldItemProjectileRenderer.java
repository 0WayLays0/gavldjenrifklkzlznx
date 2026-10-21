package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Gold projectiles: a full-bright item rolling along its flight path inside a pulsing golden halo.
 */
public class GoldItemProjectileRenderer<T extends Entity> extends EntityRenderer<T> {
    static final ResourceLocation GLOW = ArcanesOublies.id("textures/entity/gold_glow.png");

    private final ItemRenderer itemRenderer;
    private final ItemStack stack;
    private final float scale;
    private final float glowSize;
    private final float rollDegreesPerTick;

    public GoldItemProjectileRenderer(EntityRendererProvider.Context context, ItemStack stack, float scale, float glowSize, float rollDegreesPerTick) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.stack = stack;
        this.scale = scale;
        this.glowSize = glowSize;
        this.rollDegreesPerTick = rollDegreesPerTick;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        poseStack.pushPose();
        poseStack.translate(0, entity.getBbHeight() * .5f, 0);

        if (glowSize > 0) {
            poseStack.pushPose();
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            float pulse = glowSize * (1f + Mth.sin(age * .8f) * .13f);
            poseStack.scale(pulse, pulse, pulse);
            RenderHelper.billboard(buffer.getBuffer(RenderType.eyes(GLOW)), poseStack.last(), .5f, 0xFFFFFFFF, LightTexture.FULL_BRIGHT);
            poseStack.popPose();
        }

        // Local +X along the flight direction, then roll around it
        Vec3 motion = entity.getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-6) {
            poseStack.mulPose(Axis.YP.rotation((float) -Mth.atan2(motion.z, motion.x)));
            poseStack.mulPose(Axis.ZP.rotation((float) Mth.atan2(motion.y, motion.horizontalDistance())));
        }
        poseStack.mulPose(Axis.XP.rotationDegrees(age * rollDegreesPerTick));
        poseStack.scale(scale, scale, scale);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return GLOW;
    }
}
