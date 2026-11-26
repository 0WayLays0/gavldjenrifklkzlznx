package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.water.Jellyfish;
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
 * A jellyfish of water: a clear bell with glowing canals running out from its middle, closing and opening as it swims,
 * a ring of light round its rim, four glowing hearts in it; long thin tentacles hanging from the rim and four frilled
 * arms from its middle, swaying and trailing; a flash when it stings; it fades away at its end.
 */
public class JellyfishRenderer extends EntityRenderer<Jellyfish> {
    private static final int TENTACLES = 12;
    private static final int ARMS = 4;
    private static final int SEGS = 18;
    private static final int RINGS = 7;

    public JellyfishRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(Jellyfish jelly, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = jelly.tickCount + partialTicks;
        float fade = Mth.clamp(age / 10, 0, 1);
        if (jelly.getEndTick() >= 0) {
            fade *= Mth.clamp(1 - (age - jelly.getEndTick()) / Jellyfish.FADE, 0, 1);
        }
        if (fade <= .01f) {
            return;
        }
        int hue = jelly.getHue();
        float pulse = jelly.pulse(age);
        float sting = Mth.clamp(1 - (age - jelly.getStingTick()) / 8, 0, 1);
        float r = .55f * (1 - .18f * pulse);
        float h = .5f * (1 + .15f * pulse);
        int seed = jelly.getId() * 29;
        poseStack.pushPose();
        poseStack.translate(0, .5f, 0);
        poseStack.mulPose(Axis.YP.rotation(age * .01f + seed));
        poseStack.mulPose(Axis.XP.rotation(.12f * Mth.sin(age * .05f + seed)));
        // Tentacles from the rim and frilled arms from the middle, swaying, trailing
        VertexConsumer water = WaterRender.water(buffer);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < TENTACLES + ARMS; k++) {
            boolean arm = k >= TENTACLES;
            float a = arm ? (k - TENTACLES) * Mth.TWO_PI / ARMS + .4f : k * Mth.TWO_PI / TENTACLES;
            float from = arm ? r * .2f : r * .95f;
            float len = arm ? 1.1f + .2f * WaterFx.hash(seed + k) : 1.6f + .8f * WaterFx.hash(seed + k);
            int n = 10;
            Vector3f[] pts = new Vector3f[n];
            Vector3f[] side = new Vector3f[n];
            float[] w = new float[n], al = new float[n];
            for (int i = 0; i < n; i++) {
                float u = (float) i / (n - 1);
                float sway = .18f * u * Mth.sin(age * .12f - u * 4 + k);
                float rr = from * (1 - .25f * u) + sway;
                pts[i] = new Vector3f(Mth.cos(a + sway) * rr, -u * len - .05f, Mth.sin(a + sway) * rr);
                side[i] = new Vector3f(-Mth.sin(a), 0, Mth.cos(a));
                w[i] = arm ? .16f * (1 - .5f * u) * (1 + .4f * Mth.sin(u * 14 + age * .2f)) : .035f;
                al[i] = (arm ? .5f : .45f) * (1 - u * .7f) * fade;
            }
            WaterRender.ribbon(water, p, pts, side, w, al, GoldRender.lerp(hue, 0xFFFFFF, .3f), age * .02f, light);
        }
        // The bell
        VertexConsumer bell = buffer.getBuffer(com.waylays.arcanes.client.render.SpellRenderTypes.veil(WaterRender.JELLY));
        p = poseStack.last();
        int body = RuneRender.alpha(GoldRender.lerp(hue, 0xFFFFFF, .45f), .5f * fade);
        for (int j = 0; j < RINGS; j++) {
            float f0 = j * Mth.HALF_PI * 1.08f / RINGS, f1 = (j + 1) * Mth.HALF_PI * 1.08f / RINGS;
            for (int i = 0; i < SEGS; i++) {
                float t0 = i * Mth.TWO_PI / SEGS, t1 = (i + 1) * Mth.TWO_PI / SEGS;
                bellVertex(bell, p, r, h, f0, t0, body, light);
                bellVertex(bell, p, r, h, f1, t0, body, light);
                bellVertex(bell, p, r, h, f1, t1, body, light);
                bellVertex(bell, p, r, h, f0, t1, body, light);
            }
        }
        // A ring of light round its rim, its four hearts, a flash when it stings
        VertexConsumer rim = buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW));
        poseStack.pushPose();
        poseStack.translate(0, -.02f, 0);
        RenderHelper.groundQuad(rim, poseStack.last(), r * 1.15f, RuneRender.additive(hue, (.7f + .3f * pulse) * fade), GlyphRender.FULL);
        poseStack.popPose();
        VertexConsumer glow = buffer.getBuffer(RenderType.eyes(GlyphRender.GLOW));
        Quaternionf camera = this.entityRenderDispatcher.cameraOrientation();
        for (int k = 0; k < 4; k++) {
            float a = k * Mth.HALF_PI + .4f;
            poseStack.pushPose();
            poseStack.translate(Mth.cos(a) * r * .32f, h * .35f, Mth.sin(a) * r * .32f);
            poseStack.mulPose(Axis.XP.rotation(-.12f * Mth.sin(age * .05f + seed)));
            poseStack.mulPose(Axis.YP.rotation(-(age * .01f + seed)));
            poseStack.mulPose(camera);
            RenderHelper.billboard(glow, poseStack.last(), .12f, RuneRender.additive(hue, .9f * fade), GlyphRender.FULL);
            poseStack.popPose();
        }
        poseStack.pushPose();
        poseStack.translate(0, h * .3f, 0);
        poseStack.mulPose(Axis.XP.rotation(-.12f * Mth.sin(age * .05f + seed)));
        poseStack.mulPose(Axis.YP.rotation(-(age * .01f + seed)));
        poseStack.mulPose(camera);
        RenderHelper.billboard(glow, poseStack.last(), .9f + 1.2f * sting, RuneRender.additive(hue, (.3f + .7f * sting) * fade), GlyphRender.FULL);
        poseStack.popPose();
        poseStack.popPose();
        super.render(jelly, entityYaw, partialTicks, poseStack, buffer, light);
    }

    /** A point of the bell: a dome {@code r} wide and {@code h} high, its rim flaring a little out; the texture seen from above. */
    private static void bellVertex(VertexConsumer c, PoseStack.Pose p, float r, float h, float f, float t, int argb, int light) {
        float sf = Mth.sin(f), cf = Mth.cos(f);
        float flare = 1 + .12f * Math.max(0, f - 1.2f) * 3;
        float x = Mth.cos(t) * sf * r * flare, z = Mth.sin(t) * sf * r * flare, y = cf * h;
        float u = .5f + .5f * sf * Mth.cos(t), v = .5f + .5f * sf * Mth.sin(t);
        RenderHelper.vertex(c, p, x, y, z, u, v, argb, light, Mth.cos(t) * sf, cf, Mth.sin(t) * sf);
    }

    @Override
    public ResourceLocation getTextureLocation(Jellyfish jelly) {
        return WaterRender.JELLY;
    }
}
