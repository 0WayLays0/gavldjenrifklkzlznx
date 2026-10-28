package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.wood.AmberPrison;
import com.waylays.arcanes.spells.wood.WoodFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Amber Prison: golden sap welling up round the prisoner, wobbling as it climbs, setting into a lumpy shell of amber
 * you can see the prisoner through, a warm light inside it and a leaf caught in it, drops running down it, a pool at
 * its foot; before the end pale cracks run over it, then it bursts into shards that fly and fall.
 */
public class AmberPrisonRenderer extends EntityRenderer<AmberPrison> {
    private static final int SHARDS = 14;
    private static final int LIQUID = 0xFFC83C;
    private static final int SET = 0xE0861C;

    public AmberPrisonRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(AmberPrison prison, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = prison.tickCount + partialTicks;
        float w = prison.getWidth(), h = prison.getHeight();
        int seed = prison.getId() * 613;
        float since = age - prison.burstTick();
        if (since >= 0) {
            shards(poseStack, buffer, light, since, w, h, seed);
            return;
        }
        float risen = prison.risen(age);
        float set = prison.hardened(age);
        int colour = GoldRender.lerp(LIQUID, SET, set);
        float alpha = .5f + .25f * set;
        float wobble = (1 - set) * .06f;
        float rx = w * .5f + .32f, ry = h * .5f + .18f;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(prison.getYRot()));
        // A leaf caught in it
        VertexConsumer leaf = WoodRender.leafBuffer(buffer);
        poseStack.pushPose();
        poseStack.translate(rx * .55f, h * .55f * risen, rx * .3f);
        poseStack.mulPose(Axis.ZP.rotation(.6f));
        poseStack.mulPose(Axis.XP.rotation(.4f));
        WoodRender.leaf(leaf, poseStack.last(), .28f, 0xFFB86E2A, light);
        poseStack.popPose();
        // The warm light inside
        VertexConsumer glow = buffer.getBuffer(RenderType.eyes(GlyphRender.GLOW));
        poseStack.pushPose();
        poseStack.translate(0, ry * risen, 0);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        RenderHelper.billboard(glow, poseStack.last(), Math.max(rx, ry) * 1.3f, RuneRender.additive(0xFF9A2A, .35f * risen), GlyphRender.FULL);
        poseStack.popPose();
        // The shell: one great lump and a few lesser ones, rising from the ground
        VertexConsumer amber = buffer.getBuffer(RenderType.entityTranslucent(WoodRender.AMBER));
        PoseStack.Pose p = poseStack.last();
        int argb = RuneRender.alpha(colour, alpha);
        float lift = ry * risen;
        BoneRender.blob(amber, p, new Vector3f(0, lift, 0),
                new Vector3f(rx * (1 + wobble * Mth.sin(age * .5f)), 0, 0), new Vector3f(0, ry * risen, 0), new Vector3f(0, 0, rx * (1 + wobble * Mth.cos(age * .45f))),
                7, 5, light, argb);
        for (int k = 0; k < 3; k++) {
            float a = WoodFx.hash(seed + k) * Mth.TWO_PI;
            float y = (.35f + .5f * WoodFx.hash(seed + k + 3)) * h * risen;
            float r = rx * (.45f + .2f * WoodFx.hash(seed + k + 5));
            Vector3f c = new Vector3f(Mth.cos(a) * rx * .6f, y, Mth.sin(a) * rx * .6f);
            BoneRender.blob(amber, p, c, new Vector3f(r, 0, 0), new Vector3f(0, r * .9f * risen, 0), new Vector3f(0, 0, r), 6, 4, light, argb);
        }
        // Drops running down it
        for (int k = 0; k < 5; k++) {
            float a = k * Mth.TWO_PI / 5 + WoodFx.hash(seed + k * 7);
            float t = (age * .02f + WoodFx.hash(seed + k * 11)) % 1;
            float y = lift + ry * risen * (.9f - 1.8f * t);
            float yy = (y - lift) / Math.max(.01f, ry);
            float ring = Mth.sqrt(Math.max(0, 1 - yy * yy));
            Vector3f c = new Vector3f(Mth.cos(a) * rx * ring * 1.02f, y, Mth.sin(a) * rx * ring * 1.02f);
            BoneRender.blob(amber, p, c, new Vector3f(.05f, 0, 0), new Vector3f(0, .08f, 0), new Vector3f(0, 0, .05f), 5, 3, light, RuneRender.alpha(colour, .8f));
        }
        // The pool at its foot
        poseStack.pushPose();
        poseStack.translate(0, .03f, 0);
        RenderHelper.groundQuad(amber, poseStack.last(), rx + .5f, RuneRender.alpha(colour, .55f * risen), light);
        poseStack.popPose();
        // Pale cracks running over it before it bursts
        float cracked = prison.cracked(age);
        if (cracked > 0) {
            VertexConsumer crack = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
            for (int k = 0; k < 6; k++) {
                float a0 = WoodFx.hash(seed + k * 13) * Mth.TWO_PI;
                float b0 = (WoodFx.hash(seed + k * 13 + 1) - .5f) * 2;
                Vec3 prev = null;
                int n = 6;
                for (int i = 0; i <= n * cracked; i++) {
                    float u = (float) i / n;
                    float a = a0 + u * 1.4f * (k % 2 == 0 ? 1 : -1);
                    float yy = Mth.clamp(b0 + u * (k % 3 - 1) * .9f, -.95f, .95f);
                    float ring = Mth.sqrt(1 - yy * yy) * 1.03f;
                    Vec3 at = new Vec3(Mth.cos(a) * rx * ring, lift + yy * ry * 1.03f, Mth.sin(a) * rx * ring);
                    if (prev != null) {
                        GlyphRender.beam(crack, poseStack.last(), prev, at, .05f, RuneRender.additive(0xFFF0C0, .9f));
                    }
                    prev = at;
                }
            }
        }
        poseStack.popPose();
        super.render(prison, entityYaw, partialTicks, poseStack, buffer, light);
    }

    /** Shards of amber flying out and falling. */
    private static void shards(PoseStack poseStack, MultiBufferSource buffer, int light, float t, float w, float h, int seed) {
        float fade = Mth.clamp(1 - t / AmberPrison.SHATTER, 0, 1);
        if (fade <= 0) {
            return;
        }
        VertexConsumer amber = buffer.getBuffer(RenderType.entityTranslucent(WoodRender.AMBER));
        for (int k = 0; k < SHARDS; k++) {
            float a = WoodFx.hash(seed + k * 3) * Mth.TWO_PI;
            float out = .18f + .2f * WoodFx.hash(seed + k * 3 + 1);
            float up = .1f + .25f * WoodFx.hash(seed + k * 3 + 2);
            float y0 = h * (.2f + .7f * WoodFx.hash(seed + k * 5));
            float y = Math.max(.05f, y0 + up * t - .04f * t * t);
            poseStack.pushPose();
            poseStack.translate(Mth.cos(a) * (w * .5f + out * t), y, Mth.sin(a) * (w * .5f + out * t));
            poseStack.mulPose(Axis.XP.rotation(t * .5f + k));
            poseStack.mulPose(Axis.YP.rotation(t * .3f));
            float s = (.1f + .1f * WoodFx.hash(seed + k * 7)) * (.4f + .6f * fade);
            BoneRender.blob(amber, poseStack.last(), new Vector3f(), new Vector3f(s, 0, 0), new Vector3f(0, s * 1.6f, 0), new Vector3f(0, 0, s * .7f), 4, 2, light,
                    RuneRender.alpha(SET, .8f * fade));
            poseStack.popPose();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(AmberPrison prison) {
        return WoodRender.AMBER;
    }
}
