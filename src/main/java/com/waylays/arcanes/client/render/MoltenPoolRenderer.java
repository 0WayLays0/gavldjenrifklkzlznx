package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.gold.MoltenPool;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A glowing pool of molten gold that spreads out, simmers (slow pulse) and dims as it cools.
 */
public class MoltenPoolRenderer extends EntityRenderer<MoltenPool> {
    private static final ResourceLocation POOL = ArcanesOublies.id("textures/entity/molten_pool.png");

    public MoltenPoolRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MoltenPool entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        float spread = Mth.clamp(age / 6f, 0f, 1f);
        float cool = Mth.clamp((entity.getExpires() - age) / 25f, 0f, 1f);
        float pulse = .85f + Mth.sin(age * .2f) * .15f;
        int v = (int) (255 * cool * pulse);
        int color = (255 << 24) | (v << 16) | (v << 8) | v;
        poseStack.pushPose();
        poseStack.translate(0, .02, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot() + age * .5f));
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(POOL)), poseStack.last(), MoltenPool.RADIUS * 1.25f * spread, color, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MoltenPool entity) {
        return POOL;
    }
}
