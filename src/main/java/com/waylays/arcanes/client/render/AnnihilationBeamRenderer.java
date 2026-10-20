package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.boss.AnnihilationBeam;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The Annihilation Beam: an upright rune circle growing and spinning faster while it charges, then a thick beam of
 * crimson energy with a white-hot core rushing down the strip, flickering, and shrinking away when it ends.
 */
public class AnnihilationBeamRenderer extends EntityRenderer<AnnihilationBeam> {
    private static final int LIGHT = LightTexture.FULL_BRIGHT;
    private static final int CRIMSON = 0xFF3048;

    public AnnihilationBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(AnnihilationBeam beam, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float charge = beam.charge(partialTicks);
        float firing = beam.firing(partialTicks);
        float age = beam.tickCount + partialTicks;
        float end = 1 - Mth.clamp((firing - AnnihilationBeam.DURATION) / 8f, 0, 1);
        if (end <= 0) {
            return;
        }
        boolean fired = firing >= 0;
        // The rune circle standing at the start of the strip, facing down it
        poseStack.pushPose();
        poseStack.translate(0, AnnihilationBeam.HEIGHT, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-beam.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        float size = (fired ? 2.6f : .6f + 2f * charge) * end;
        poseStack.mulPose(Axis.YP.rotationDegrees(age * (fired ? 25 : 3 + 12 * charge)));
        VertexConsumer circle = buffer.getBuffer(RenderType.eyes(BossTelegraphRenderer.CIRCLES[1]));
        int light = BossTelegraphRenderer.additive(fired ? BossTelegraphRenderer.lighten(CRIMSON, .4f) : CRIMSON, (fired ? 1 : .4f + .6f * charge) * end);
        RenderHelper.groundQuad(circle, poseStack.last(), size, light, LIGHT);
        poseStack.mulPose(Axis.XP.rotationDegrees(180));
        RenderHelper.groundQuad(circle, poseStack.last(), size, light, LIGHT);
        poseStack.popPose();

        if (fired) {
            Vec3 f = beam.forward();
            Vec3 from = new Vec3(0, AnnihilationBeam.HEIGHT, 0);
            Vec3 to = from.add(f.scale(beam.getLength()));
            float flicker = .9f + .1f * Mth.sin(age * 2.3f);
            float width = beam.getWidth() * .8f * end * flicker;
            float scroll = age * .35f;
            VertexConsumer c = buffer.getBuffer(RenderType.eyes(BossPropRenderer.BEAM));
            // Outer crimson body, then the white-hot core; offset along the beam to make the energy run
            RenderHelper.strand(c, poseStack.last(), from.subtract(f.scale(scroll % 4)), to, width, 4, BossTelegraphRenderer.additive(CRIMSON, end), LIGHT);
            RenderHelper.strand(c, poseStack.last(), from.subtract(f.scale((scroll * 1.7f) % 4)), to, width * .4f, 4, BossTelegraphRenderer.additive(0xFFF4F0, end), LIGHT);
        }
        super.render(beam, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(AnnihilationBeam entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(AnnihilationBeam entity) {
        return BossPropRenderer.BEAM;
    }
}
