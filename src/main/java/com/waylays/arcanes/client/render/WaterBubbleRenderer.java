package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.water.WaterBubble;
import com.waylays.arcanes.spells.water.WaterFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Bubble Prison: a great soap bubble round its prisoner, near clear, rainbows swirling over its skin, a bright rim and
 * a glint of light on it, little bubbles rising inside; it wobbles as it rises and shivers just before it bursts.
 */
public class WaterBubbleRenderer extends EntityRenderer<WaterBubble> {
    public WaterBubbleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(WaterBubble bubble, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = bubble.tickCount + partialTicks;
        float formed = Mth.clamp(age / WaterBubble.FORM, 0, 1);
        formed = 1 - (1 - formed) * (1 - formed);
        float r = bubble.getRadius() * formed;
        if (r <= .01f) {
            return;
        }
        // Drawn round where the prisoner is now (it rises; the entity stays below)
        float lift = bubble.footY(age) - (float) Mth.lerp(partialTicks, bubble.yo, bubble.getY());
        float shiver = Mth.clamp((age - bubble.burstTick() + 12) / 12, 0, 1);
        float wob = .04f + shiver * .08f;
        int seed = bubble.getId() * 17;
        Quaternionf camera = this.entityRenderDispatcher.cameraOrientation();
        poseStack.pushPose();
        poseStack.translate(0, lift + bubble.getRadius() - .45f + .1f, 0);
        // Little bubbles rising inside
        VertexConsumer drops = WaterRender.drops(buffer);
        for (int k = 0; k < 12; k++) {
            float t = (age * .03f + WaterFx.hash(seed + k)) % 1;
            float a = WaterFx.hash(seed + k * 3) * Mth.TWO_PI, rr = r * .6f * WaterFx.hash(seed + k * 5);
            WaterRender.drop(drops, poseStack, camera, Mth.cos(a + t) * rr, -r * .8f + t * r * 1.6f, Mth.sin(a + t) * rr, .1f + .08f * WaterFx.hash(seed + k * 7),
                    RuneRender.alpha(0xE8FBFF, .7f * Mth.sin(t * Mth.PI)), light);
        }
        // Its skin, rainbows swirling over it
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotation(age * .02f));
        VertexConsumer skin = buffer.getBuffer(RenderType.entityTranslucent(WaterRender.BUBBLE));
        float sx = 1 + wob * Mth.sin(age * .45f), sy = 1 + wob * Mth.sin(age * .45f + 2), sz = 1 + wob * Mth.cos(age * .4f);
        WaterRender.sphere(skin, poseStack.last(), new Vector3f(), r * sx, r * sy, r * sz, 16, 10, RuneRender.alpha(0xFFFFFF, .42f), light);
        poseStack.popPose();
        // A bright rim and a glint of light
        VertexConsumer rim = buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW));
        poseStack.pushPose();
        poseStack.mulPose(camera);
        RenderHelper.billboard(rim, poseStack.last(), r * 1.08f, RuneRender.additive(0xB8E8FF, .45f + .3f * shiver), light);
        poseStack.popPose();
        VertexConsumer glint = buffer.getBuffer(RenderType.eyes(GlyphRender.GLOW));
        poseStack.pushPose();
        poseStack.mulPose(camera);
        poseStack.translate(-r * .45f, r * .45f, r * .2f);
        RenderHelper.billboard(glint, poseStack.last(), r * .22f, RuneRender.additive(0xFFFFFF, .8f), light);
        poseStack.popPose();
        poseStack.popPose();
        super.render(bubble, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(WaterBubble bubble) {
        return WaterRender.BUBBLE;
    }
}
