package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * A ball of unstable energy: a hot core, a flickering glow and two crackling shells spinning opposite ways.
 */
public class GlowOrbRenderer<T extends Entity> extends EntityRenderer<T> {
    private static final ResourceLocation ORB = ArcanesOublies.id("textures/entity/unstable_orb.png");

    private final float size;
    private final int glowColor;

    /** {@code glowColor}: tint of the outer glow (0xRRGGBB). */
    public GlowOrbRenderer(EntityRendererProvider.Context context, float size, int glowColor) {
        super(context);
        this.size = size;
        this.glowColor = 0xFF000000 | glowColor;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        // Unstable: the glow flickers
        float flicker = 1 + Mth.sin(age * 2.3f) * .08f + Mth.sin(age * 5.1f) * .05f;
        poseStack.pushPose();
        poseStack.translate(0, entity.getBbHeight() * .5f, 0);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180f));
        BlastEffectRenderer.layer(buffer, BlastEffectRenderer.CORE, poseStack, size * 2.6f * flicker, 0, glowColor);
        BlastEffectRenderer.layer(buffer, ORB, poseStack, size * 1.5f, age * 25f, 0xFFFFFFFF);
        BlastEffectRenderer.layer(buffer, ORB, poseStack, size * 1.2f, -age * 40f + 45f, 0xFFC0C0C0);
        BlastEffectRenderer.layer(buffer, BlastEffectRenderer.CORE, poseStack, size * .9f, 0, 0xFFFFFFFF);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return ORB;
    }
}
