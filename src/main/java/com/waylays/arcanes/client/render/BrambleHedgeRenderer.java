package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.wood.BrambleHedge;
import com.waylays.arcanes.spells.wood.WoodFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.List;

/**
 * Brambles: thorny green stems springing out of the ground and arching over one another (some curling up into the
 * air), thorns all along them, leaves and azalea blossoms on them, the ground glowing green where they broke out.
 */
public class BrambleHedgeRenderer extends EntityRenderer<BrambleHedge> {
    private static final int POINTS = 9;

    public BrambleHedgeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BrambleHedge hedge, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = hedge.tickCount + partialTicks;
        List<BrambleHedge.Stem> stems = hedge.stems();
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-hedge.getYRot()));
        Vector3f[][] shapes = new Vector3f[stems.size()][];
        float[] grown = new float[stems.size()];
        for (int k = 0; k < stems.size(); k++) {
            BrambleHedge.Stem s = stems.get(k);
            grown[k] = hedge.grown(s, age);
            if (grown[k] > .02f) {
                shapes[k] = shape(s, grown[k], age);
            }
        }
        // The stems and their thorns
        VertexConsumer vine = WoodRender.vine(buffer);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < stems.size(); k++) {
            if (shapes[k] != null) {
                WoodRender.thornyVine(vine, p, shapes[k], .085f * (.5f + .5f * grown[k]), packedLight, stems.get(k).seed());
            }
        }
        // Leaves along them, a blossom here and there
        VertexConsumer leaf = WoodRender.leafBuffer(buffer);
        for (int k = 0; k < stems.size(); k++) {
            if (shapes[k] == null) {
                continue;
            }
            int seed = stems.get(k).seed();
            for (int i = 2; i < POINTS; i += 2) {
                Vector3f at = shapes[k][i];
                poseStack.pushPose();
                poseStack.translate(at.x, at.y, at.z);
                poseStack.mulPose(Axis.YP.rotation(WoodFx.hash(seed + i) * Mth.TWO_PI));
                poseStack.mulPose(Axis.XP.rotation(.5f + WoodFx.hash(seed + i * 5) + .1f * Mth.sin(age * .1f + i)));
                poseStack.translate(0, .12f, 0);
                WoodRender.leaf(leaf, poseStack.last(), .32f * grown[k], WoodRender.leafColour(seed + i), packedLight);
                poseStack.popPose();
            }
        }
        // The ground glowing where they broke out, fading as they settle
        float glow = Mth.clamp(1 - (age - 4) / 30, 0, 1) * .6f + .15f;
        float withered = Mth.clamp(1 - (age - hedge.getDuration()) / BrambleHedge.WITHER, 0, 1);
        VertexConsumer light = buffer.getBuffer(RenderType.eyes(GlyphRender.GLOW));
        if (hedge.getMode() == BrambleHedge.Mode.LINE) {
            int spots = Math.max(2, (int) (hedge.getSize() / 2));
            for (int i = 0; i < spots; i++) {
                float x = -hedge.getSize() * .5f + hedge.getSize() * (i + .5f) / spots;
                poseStack.pushPose();
                poseStack.translate(x, .04f, 0);
                RenderHelper.groundQuad(light, poseStack.last(), 1.6f, RuneRender.additive(WoodFx.LIFE, glow * withered * .7f), GlyphRender.FULL);
                poseStack.popPose();
            }
        } else {
            poseStack.pushPose();
            poseStack.translate(0, .04f, 0);
            RenderHelper.groundQuad(light, poseStack.last(), hedge.getSize() + .8f, RuneRender.additive(WoodFx.LIFE, glow * withered * .7f), GlyphRender.FULL);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(hedge, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** A stem's points as it stands at {@code grown}: an arch from its root to where it comes down, or a curl. */
    private static Vector3f[] shape(BrambleHedge.Stem s, float grown, float age) {
        int n = Math.max(2, Math.round(POINTS * grown));
        Vector3f[] pts = new Vector3f[n];
        float sway = .06f * Mth.sin(age * .07f + s.seed());
        for (int i = 0; i < n; i++) {
            float u = (float) i / (POINTS - 1);
            if (s.curl()) {
                // Up into the air and curling over at the top
                float a = u * Mth.PI * 1.4f;
                float r = .35f * u;
                pts[i] = new Vector3f(s.x0() + Mth.sin(a) * r + sway * u, s.height() * 1.2f * Mth.sin(u * Mth.HALF_PI) - .1f, s.z0() + (1 - Mth.cos(a)) * r * .6f);
            } else {
                float x = Mth.lerp(u, s.x0(), s.x1()), z = Mth.lerp(u, s.z0(), s.z1());
                float y = s.height() * 4 * u * (1 - u) * (1 - .15f * u) - .15f + sway * Mth.sin(u * Mth.PI);
                pts[i] = new Vector3f(x, y, z);
            }
        }
        return pts;
    }

    @Override
    public boolean shouldRender(BrambleHedge hedge, Frustum frustum, double x, double y, double z) {
        float r = hedge.getSize() + 2;
        return frustum.isVisible(hedge.getBoundingBox().inflate(r, 3, r));
    }

    @Override
    public ResourceLocation getTextureLocation(BrambleHedge hedge) {
        return WoodRender.VINE;
    }
}
