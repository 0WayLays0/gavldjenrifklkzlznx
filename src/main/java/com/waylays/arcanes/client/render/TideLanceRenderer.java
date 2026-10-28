package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.water.TideLance;
import com.waylays.arcanes.spells.water.WaterFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * A Tide Lance: a slim spindle of clear water drawn to a point, three streams twisting round it and trailing off behind
 * in a spiral wake, foam beading at its point.
 */
public class TideLanceRenderer extends EntityRenderer<TideLance> {
    private static final int STREAMS = 3;
    private static final int POINTS = 14;

    public TideLanceRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(TideLance lance, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = lance.tickCount + partialTicks;
        poseStack.pushPose();
        poseStack.translate(0, lance.getBbHeight() * .5f, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, lance.yRotO, lance.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTicks, lance.xRotO, lance.getXRot())));
        // Along +Z now: the point ahead, the wake behind
        VertexConsumer water = WaterRender.water(buffer);
        PoseStack.Pose p = poseStack.last();
        BoneRender.blob(water, p, new Vector3f(0, 0, .1f), new Vector3f(.14f, 0, 0), new Vector3f(0, .14f, 0), new Vector3f(0, 0, .75f), 8, 6, light,
                RuneRender.alpha(WaterRender.sea(.9f), .75f));
        BoneRender.spike(water, p, new Vector3f(0, 0, .6f), new Vector3f(0, 0, 1.25f), .1f, new Vector3f(), light, RuneRender.alpha(WaterFx.FOAM, .8f));
        // Three streams twisting round it, trailing off behind
        for (int s = 0; s < STREAMS; s++) {
            Vector3f[] pts = new Vector3f[POINTS];
            Vector3f[] side = new Vector3f[POINTS];
            float[] w = new float[POINTS], a = new float[POINTS];
            for (int i = 0; i < POINTS; i++) {
                float u = (float) i / (POINTS - 1);
                float ang = s * Mth.TWO_PI / STREAMS + age * .9f - u * 7;
                float r = .17f + .25f * u * u;
                pts[i] = new Vector3f(Mth.cos(ang) * r, Mth.sin(ang) * r, .7f - u * 2.6f);
                side[i] = new Vector3f(-Mth.sin(ang), Mth.cos(ang), 0);
                w[i] = .13f * (1 - u * .6f);
                a[i] = .7f * (1 - u);
            }
            WaterRender.ribbon(water, p, pts, side, w, a, WaterRender.sea(.75f), -age * .15f, light);
        }
        poseStack.popPose();
        super.render(lance, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(TideLance lance) {
        return WaterRender.WATER;
    }
}
