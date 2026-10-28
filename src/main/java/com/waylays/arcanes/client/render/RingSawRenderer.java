package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.wood.RingSaw;
import com.waylays.arcanes.spells.wood.WoodFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Ring Saw: a slice of trunk spinning flat, a little tilted: its rings on both faces, its rim of bark, sixteen thorns
 * round it for teeth, raked back as it spins; a green blur of its speed round it, its heart glowing.
 */
public class RingSawRenderer extends EntityRenderer<RingSaw> {
    private static final int TEETH = 16;

    public RingSawRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RingSaw saw, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = saw.tickCount + partialTicks;
        float r = RingSaw.RADIUS;
        float half = .11f;
        float grow = Mth.clamp(age / 3, .3f, 1);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-saw.getYRot()));
        poseStack.mulPose(Axis.XP.rotation(.25f));
        poseStack.mulPose(Axis.ZP.rotation(.15f * Mth.sin(age * .2f)));
        poseStack.scale(grow, grow, grow);
        poseStack.mulPose(Axis.YP.rotation(-age * 1.4f));
        PoseStack.Pose p = poseStack.last();
        // The rim of bark and its teeth
        VertexConsumer bark = WoodRender.bark(buffer);
        BoneRender.tube(bark, p, new Vector3f(0, -half, 0), new Vector3f(0, half, 0), r, r, 20, light, BoneRender.WHITE);
        for (int k = 0; k < TEETH; k++) {
            float a = k * Mth.TWO_PI / TEETH;
            Vector3f base = new Vector3f(Mth.cos(a) * r * .97f, 0, Mth.sin(a) * r * .97f);
            // Raked back against the spin
            float b = a - .55f;
            Vector3f tip = new Vector3f(Mth.cos(b) * (r + .32f), 0, Mth.sin(b) * (r + .32f));
            WoodRender.thorn(bark, p, base, tip, .09f, light, WoodRender.THORN);
        }
        // Its two faces: the rings
        VertexConsumer end = WoodRender.ends(buffer);
        WoodRender.disc(end, p, new Vector3f(0, half, 0), new Vector3f(0, 0, 1), new Vector3f(1, 0, 0), r * .99f, 20, light, BoneRender.WHITE);
        WoodRender.disc(end, p, new Vector3f(0, -half, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, 1), r * .99f, 20, light, BoneRender.WHITE);
        // A green blur of its speed, its heart glowing
        VertexConsumer ring = buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW));
        poseStack.pushPose();
        poseStack.translate(0, .01f, 0);
        RenderHelper.groundQuad(ring, poseStack.last(), r + .45f, RuneRender.additive(WoodFx.LIFE, .45f), GlyphRender.FULL);
        poseStack.popPose();
        VertexConsumer glow = buffer.getBuffer(RenderType.eyes(GlyphRender.GLOW));
        for (float y : new float[]{half + .01f, -half - .01f}) {
            poseStack.pushPose();
            poseStack.translate(0, y, 0);
            RenderHelper.groundQuad(glow, poseStack.last(), .22f, RuneRender.additive(WoodFx.LIFE_CORE, .8f), GlyphRender.FULL);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(saw, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(RingSaw saw) {
        return WoodRender.LOG_END;
    }
}
