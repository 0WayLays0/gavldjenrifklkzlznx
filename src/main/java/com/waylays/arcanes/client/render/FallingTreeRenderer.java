package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.wood.FallingTree;
import com.waylays.arcanes.spells.wood.WoodFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Timber!: a great oak (a trunk of oak logs narrowing as it rises, bark roots splayed round its foot, crooked branches,
 * a round crown of azalea leaves, flowering here and there) growing out of the ground, swaying back, toppling forwards
 * onto the ground about its foot, then sinking back into the earth.
 */
public class FallingTreeRenderer extends EntityRenderer<FallingTree> {
    public FallingTreeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(FallingTree tree, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = tree.tickCount + partialTicks;
        float grown = tree.grown(age);
        float sunk = tree.sunk(age);
        if (grown <= .01f || sunk >= 1) {
            return;
        }
        float h = tree.getHeight();
        int seed = tree.getId() * 977;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-tree.getYRot()));
        poseStack.translate(0, -3.2f * sunk, 0);
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotation(tree.lean(age)));
        poseStack.scale(grown, grown, grown);
        drawTree(poseStack, buffer, packedLight, h, seed, age, tree.lean(age));
        poseStack.popPose();
        // The ground glowing where it broke out
        float glow = Mth.clamp(1 - age / (FallingTree.GROW + 10), 0, 1);
        if (glow > 0) {
            VertexConsumer light = buffer.getBuffer(RenderType.eyes(GlyphRender.GLOW));
            poseStack.translate(0, .05f, 0);
            RenderHelper.groundQuad(light, poseStack.last(), 3.5f, RuneRender.additive(WoodFx.LIFE, glow * .8f), GlyphRender.FULL);
        }
        poseStack.popPose();
        super.render(tree, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** The whole tree standing up from the origin, {@code h} tall: the logs and leaves first, the bark after. */
    static void drawTree(PoseStack poseStack, MultiBufferSource buffer, int light, float h, int seed, float age, float lean) {
        float trunkTop = h * .72f;
        int logs = Math.max(2, Mth.ceil(trunkTop));
        float step = trunkTop / logs;
        for (int i = 0; i < logs; i++) {
            float w = FallingTree.TRUNK * (1.15f - .3f * i / logs);
            float wobble = .05f * Mth.sin(i * 1.7f + seed);
            WoodRender.log(WoodRender.LOG, poseStack, buffer, light, wobble, i * step - .02f, -wobble, w, step + .04f);
        }
        // The crown: leaf blocks heaped round the top, the sway shaking it a little
        float shake = lean < Mth.HALF_PI - .1f ? .04f * Mth.sin(age * .5f) : 0;
        float cy = h * .78f;
        int n = 22;
        for (int i = 0; i < n; i++) {
            float a = WoodFx.hash(seed + i * 3) * Mth.TWO_PI;
            float up = WoodFx.hash(seed + i * 3 + 1) * 2 - 1;
            float rr = Mth.sqrt(1 - up * up);
            float rx = 2.6f + .3f * WoodFx.hash(seed + i + 50), ry = 1.9f;
            float x = Mth.cos(a) * rr * rx * .85f + shake * (i % 3), y = cy + up * ry, z = Mth.sin(a) * rr * rx * .85f;
            float s = 1.5f + .9f * WoodFx.hash(seed + i * 7);
            RuneRender.block(WoodRender.leaves(seed + i), poseStack, buffer, light, x, y - s * .5f, z, s, s, s,
                    (WoodFx.hash(seed + i * 11) - .5f) * 30, (WoodFx.hash(seed + i * 13) - .5f) * 30);
        }
        RuneRender.block(WoodRender.LEAVES, poseStack, buffer, light, 0, cy - 1.4f, 0, 3.2f, 3.4f, 3.2f, 0, 0);
        // Roots and branches, in bark
        VertexConsumer bark = WoodRender.bark(buffer);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < 6; k++) {
            float a = k * Mth.TWO_PI / 6 + WoodFx.hash(seed + k + 90) * .6f;
            float len = 1.6f + 1.2f * WoodFx.hash(seed + k + 91);
            Vector3f[] root = new Vector3f[6];
            for (int i = 0; i < root.length; i++) {
                float u = (float) i / (root.length - 1);
                float r = .3f + u * len;
                root[i] = new Vector3f(Mth.cos(a + u * .4f) * r, 1.1f * (1 - u) * (1 - u) - .2f, Mth.sin(a + u * .4f) * r);
            }
            BoneRender.curve(bark, p, root, .32f, .06f, 6, light, BoneRender.WHITE);
        }
        for (int k = 0; k < 5; k++) {
            float a = k * Mth.TWO_PI / 5 + WoodFx.hash(seed + k + 70) * .8f;
            float y0 = h * (.45f + .2f * WoodFx.hash(seed + k + 71));
            float len = 2 + 1.2f * WoodFx.hash(seed + k + 72);
            Vector3f[] branch = new Vector3f[6];
            for (int i = 0; i < branch.length; i++) {
                float u = (float) i / (branch.length - 1);
                float r = u * len;
                float kink = .25f * Mth.sin(u * 7 + k);
                branch[i] = new Vector3f(Mth.cos(a + kink) * r, y0 + u * len * .7f, Mth.sin(a + kink) * r);
            }
            BoneRender.curve(bark, p, branch, .2f, .05f, 6, light, BoneRender.WHITE);
        }
    }

    @Override
    public boolean shouldRender(FallingTree tree, Frustum frustum, double x, double y, double z) {
        float r = tree.getHeight() + 4;
        return frustum.isVisible(tree.getBoundingBox().inflate(r, r, r));
    }

    @Override
    public ResourceLocation getTextureLocation(FallingTree tree) {
        return WoodRender.BARK;
    }
}
