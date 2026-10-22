package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.verdigris.CopperSpikes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The wedge of copper spikes: each a jagged stepped spike, green and mossy at its foot, weathered higher up, bright
 * copper at the tip, leaning outward, bursting out of broken earth row after row.
 */
public class CopperSpikesRenderer extends EntityRenderer<CopperSpikes> {
    public CopperSpikesRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(CopperSpikes spikes, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        for (CopperSpikes.Spike spike : spikes.spikes()) {
            float risen = spikes.risen(spike.row(), partialTicks);
            if (risen <= .001f) {
                continue;
            }
            float h = spike.height();
            int seed = spike.seed();
            float x = (float) spike.at().x;
            float y = (float) spike.at().y;
            float z = (float) spike.at().z;
            float ground = Mth.clamp(risen, 0, 1);
            VerdigrisRender.block(VerdigrisRender.COARSE_DIRT, poseStack, buffer, packedLight, x, y - .04f, z, 1.1f * ground, .08f, 1.1f * ground);

            poseStack.pushPose();
            poseStack.translate(x, y, z);
            poseStack.mulPose(Axis.XP.rotationDegrees(spike.tiltX()));
            poseStack.mulPose(Axis.ZP.rotationDegrees(spike.tiltZ()));
            poseStack.translate(0, -(1 - risen) * h, 0);
            float turn = RuneRender.hash(seed) * 90;
            VerdigrisRender.turned(VerdigrisRender.OXIDIZED, poseStack, buffer, packedLight, 0, -.3f, 0, .58f, .3f + .4f * h, .58f, turn);
            VerdigrisRender.turned(VerdigrisRender.WEATHERED_CUT, poseStack, buffer, packedLight, 0, .4f * h, 0, .4f, .32f * h, .4f, turn + 30);
            VerdigrisRender.turned(VerdigrisRender.EXPOSED, poseStack, buffer, packedLight, 0, .72f * h, 0, .24f, .2f * h, .24f, turn + 55);
            VerdigrisRender.turned(VerdigrisRender.COPPER, poseStack, buffer, packedLight, 0, .92f * h, 0, .11f, .14f * h, .11f, turn + 80);
            if (RuneRender.hash(seed * 3) < .55f) {
                // Moss clinging to its foot, and on some a vine
                VerdigrisRender.turned(VerdigrisRender.MOSS, poseStack, buffer, packedLight, 0, 0, 0, .66f, .14f, .66f, turn);
                if (RuneRender.hash(seed * 5) < .5f) {
                    poseStack.pushPose();
                    poseStack.mulPose(Axis.YP.rotationDegrees(turn));
                    VerdigrisRender.vines(poseStack, buffer, packedLight, .4f * h + .1f, .58f, .58f, VerdigrisRender.hanging(seed, .4f * h));
                    poseStack.popPose();
                }
            }
            poseStack.popPose();
        }
        super.render(spikes, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(CopperSpikes spikes, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(CopperSpikes spikes) {
        return RuneRender.GLOW;
    }
}
