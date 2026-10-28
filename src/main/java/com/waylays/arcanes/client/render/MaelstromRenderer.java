package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.entity.water.Maelstrom;
import com.waylays.arcanes.spells.water.WaterFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Maelstrom: a bowl of water raised at its rim and sinking to its heart, its rings turning faster the nearer the heart,
 * pale at the rim and deep blue at the middle; arms of foam spiralling in; a dark funnel at its heart; a low wall of
 * water churning round its rim and spray thrown off it.
 */
public class MaelstromRenderer extends EntityRenderer<Maelstrom> {
    private static final int RINGS = 9;
    private static final int ARMS = 5;

    public MaelstromRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /** Height of the bowl's surface {@code u} of the way out from its heart. */
    private static float surface(float u, float open) {
        return (.05f + .55f * u * u) * open;
    }

    @Override
    public void render(Maelstrom whirl, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = whirl.tickCount + partialTicks;
        float open = whirl.opened(age);
        if (open <= .01f) {
            return;
        }
        float R = Maelstrom.RADIUS * open;
        int seed = whirl.getId() * 43;
        VertexConsumer water = WaterRender.water(buffer);
        PoseStack.Pose p = poseStack.last();
        // The bowl: rings turning faster towards the heart, deep blue at the middle
        for (int j = 0; j < RINGS; j++) {
            float u0 = (float) j / RINGS, u1 = (float) (j + 1) / RINGS;
            float spin = age * (.09f - .07f * u0);
            int c0 = RuneRender.alpha(WaterRender.sea(u0 * .8f + .1f), .8f), c1 = RuneRender.alpha(WaterRender.sea(u1 * .8f + .1f), .75f);
            ringSlope(water, p, u0 * R, surface(u0, open), u1 * R, surface(u1, open), spin, c0, c1, light);
        }
        // A low wall of water churning round the rim
        WaterRender.funnel(water, p, R, 0, R * .97f, surface(1, open) + .25f, 2, 40, 6, .2f, age * .03f, WaterRender.sea(.8f), .6f, .35f, light);
        // The dark funnel at its heart
        WaterRender.funnel(water, p, .9f * open, surface(.16f, open), .15f, -.6f, 4, 16, 2, 1.5f, age * .2f, 0x0C2E5A, .9f, .9f, light);
        // Arms of foam spiralling in
        VertexConsumer foam = WaterRender.foam(buffer);
        p = poseStack.last();
        for (int a = 0; a < ARMS; a++) {
            int n = 16;
            Vector3f[] pts = new Vector3f[n];
            Vector3f[] side = new Vector3f[n];
            float[] w = new float[n], al = new float[n];
            for (int i = 0; i < n; i++) {
                float u = 1 - (float) i / (n - 1) * .9f;
                float ang = a * Mth.TWO_PI / ARMS + (1 - u) * 5.5f + age * (.09f - .07f * u) * 1.1f;
                pts[i] = new Vector3f(Mth.cos(ang) * u * R, surface(u, open) + .03f, Mth.sin(ang) * u * R);
                side[i] = new Vector3f(Mth.cos(ang), 0, Mth.sin(ang));
                w[i] = .55f * u + .1f;
                al[i] = .75f * Mth.sin(u * Mth.PI) * open;
            }
            WaterRender.ribbon(foam, p, pts, side, w, al, 0xFFFFFF, age * .02f, light);
        }
        // Spray thrown off the rim
        VertexConsumer drops = WaterRender.drops(buffer);
        Quaternionf camera = this.entityRenderDispatcher.cameraOrientation();
        for (int k = 0; k < 30; k++) {
            float t = (age * .05f + WaterFx.hash(seed + k)) % 1;
            float a = WaterFx.hash(seed + k * 3) * Mth.TWO_PI + age * .02f;
            float out = R * (.95f + .4f * t);
            WaterRender.drop(drops, poseStack, camera, Mth.cos(a) * out, surface(1, open) + 1.4f * t - 1.6f * t * t, Mth.sin(a) * out, .16f,
                    RuneRender.alpha(WaterFx.FOAM, .8f * (1 - t) * open), light);
        }
        super.render(whirl, entityYaw, partialTicks, poseStack, buffer, light);
    }

    /** A sloping ring of the bowl, from radius {@code r0} at height {@code y0} out to {@code r1} at {@code y1}. */
    private static void ringSlope(VertexConsumer c, PoseStack.Pose p, float r0, float y0, float r1, float y1, float spin, int c0, int c1, int light) {
        int segs = 32;
        for (int i = 0; i < segs; i++) {
            float a0 = i * Mth.TWO_PI / segs, a1 = (i + 1) * Mth.TWO_PI / segs;
            float u0 = (float) i / segs * 3 + spin, u1 = (float) (i + 1) / segs * 3 + spin;
            RenderHelper.vertex(c, p, Mth.cos(a0) * r0, y0, Mth.sin(a0) * r0, u0, 0, c0, light, 0, 1, 0);
            RenderHelper.vertex(c, p, Mth.cos(a0) * r1, y1, Mth.sin(a0) * r1, u0, 1, c1, light, 0, 1, 0);
            RenderHelper.vertex(c, p, Mth.cos(a1) * r1, y1, Mth.sin(a1) * r1, u1, 1, c1, light, 0, 1, 0);
            RenderHelper.vertex(c, p, Mth.cos(a1) * r0, y0, Mth.sin(a1) * r0, u1, 0, c0, light, 0, 1, 0);
        }
    }

    @Override
    public boolean shouldRender(Maelstrom whirl, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(whirl.getBoundingBox().inflate(Maelstrom.RADIUS + 3, 3, Maelstrom.RADIUS + 3));
    }

    @Override
    public ResourceLocation getTextureLocation(Maelstrom whirl) {
        return WaterRender.WATER;
    }
}
