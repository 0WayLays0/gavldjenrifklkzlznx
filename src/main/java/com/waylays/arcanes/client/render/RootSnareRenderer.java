package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.entity.wood.RootSnare;
import com.waylays.arcanes.spells.wood.WoodFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Grasping Roots: thick roots splayed on the ground round the prisoner, each winding up round it in a spiral, thinner
 * as it climbs, a few thorns on them, the ground glowing green where they broke out.
 */
public class RootSnareRenderer extends EntityRenderer<RootSnare> {
    private static final int POINTS = 12;

    public RootSnareRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RootSnare snare, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = snare.tickCount + partialTicks;
        float grown = snare.grown(age);
        if (grown <= .01f) {
            return;
        }
        float w = snare.getWidth();
        float h = snare.getHeight();
        VertexConsumer bark = WoodRender.bark(buffer);
        PoseStack.Pose p = poseStack.last();
        int seed = snare.getId() * 37;
        for (int k = 0; k < RootSnare.ROOTS; k++) {
            float a0 = k * Mth.TWO_PI / RootSnare.ROOTS + WoodFx.hash(seed + k) * .5f;
            float climb = (.6f + .4f * WoodFx.hash(seed + k + 7)) * grown;
            Vector3f[] pts = new Vector3f[POINTS];
            for (int i = 0; i < POINTS; i++) {
                float u = (float) i / (POINTS - 1);
                if (u < .3f) {
                    // Splayed out over the ground, then curling in to the body
                    float t = u / .3f;
                    float r = Mth.lerp(t, w + 1.1f, w + .1f);
                    pts[i] = new Vector3f(Mth.cos(a0) * r, -.15f + .15f * t, Mth.sin(a0) * r);
                } else {
                    float t = (u - .3f) / .7f * climb;
                    float a = a0 + t * Mth.PI * 1.6f;
                    float r = w * (1 - .25f * t) + .08f;
                    pts[i] = new Vector3f(Mth.cos(a) * r, t * h, Mth.sin(a) * r);
                }
            }
            BoneRender.curve(bark, p, pts, .17f, .05f, 6, packedLight, BoneRender.WHITE);
            for (int i = 4; i < POINTS - 1; i += 3) {
                Vector3f out = new Vector3f(pts[i].x, 0, pts[i].z).normalize().mul(.22f);
                WoodRender.thorn(bark, p, pts[i], new Vector3f(pts[i]).add(out).add(0, .08f, 0), .05f, packedLight, WoodRender.THORN);
            }
        }
        VertexConsumer glow = buffer.getBuffer(RenderType.eyes(GlyphRender.GLOW));
        poseStack.pushPose();
        poseStack.translate(0, .05f, 0);
        RenderHelper.groundQuad(glow, poseStack.last(), w + 1.6f, RuneRender.additive(WoodFx.LIFE, .5f * grown), GlyphRender.FULL);
        poseStack.popPose();
        super.render(snare, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(RootSnare snare) {
        return WoodRender.BARK;
    }
}
