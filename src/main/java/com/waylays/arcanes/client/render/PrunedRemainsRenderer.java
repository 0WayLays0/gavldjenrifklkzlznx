package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.cosmos.PrunedRemains;
import com.waylays.arcanes.spells.cosmos.Cosmos;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * A creature being pruned: its body all orange light, crumbling away from its feet up behind a seam of white-hot
 * light, a ring of orange light flaring on the ground under it.
 */
public class PrunedRemainsRenderer extends EntityRenderer<PrunedRemains> {
    public PrunedRemainsRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(PrunedRemains remains, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Entity victim = remains.getVictim();
        float age = remains.tickCount + partialTicks;
        float line = PrunedRemains.crumbled(age);
        float flare = Mth.clamp(1 - age / (PrunedRemains.CRUMBLE_TICKS + 4), 0, 1);
        if (victim != null && line < 1) {
            Vec3 off = victim.getPosition(partialTicks).subtract(remains.getPosition(partialTicks));
            poseStack.pushPose();
            poseStack.translate(off.x, off.y, off.z);
            CosmosRender.echo(victim, poseStack, buffer, partialTicks, Cosmos.ORANGE, .95f, age * .05f, line, 1);
            poseStack.popPose();
        }
        float w = victim != null ? victim.getBbWidth() : 1;
        poseStack.pushPose();
        poseStack.translate(0, .04, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 6));
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), (w + .8f) * (1 + age * .03f),
                RuneRender.additive(Cosmos.ORANGE, .8f * flare), GlyphRender.FULL);
        poseStack.popPose();
        super.render(remains, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(PrunedRemains remains) {
        return CosmosRender.SCANLINES;
    }
}
