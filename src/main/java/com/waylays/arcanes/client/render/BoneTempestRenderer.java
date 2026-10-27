package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.entity.bone.BoneTempest;
import com.waylays.arcanes.spells.bone.BoneFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * The Bone Tempest: three rings of bones turning round the caster, each bone tumbling end over end as it goes, a
 * crimson glow on the ground under them; at the end they fly apart outwards.
 */
public class BoneTempestRenderer extends EntityRenderer<BoneTempest> {
    public BoneTempestRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BoneTempest tempest, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = tempest.tickCount + partialTicks;
        int life = tempest.getLife();
        float grow = Mth.clamp(age / BoneTempest.GROW, 0, 1);
        float scatter = Mth.clamp((age - life) / BoneTempest.SCATTER, 0, 1);
        if (scatter >= 1) {
            return;
        }
        VertexConsumer bone = BoneRender.buffer(buffer);
        PoseStack.Pose p = poseStack.last();
        int seed = tempest.getId() * 53;
        for (int ring = 0; ring < 3; ring++) {
            int n = BoneTempest.COUNT[ring];
            float radius = BoneTempest.RING[ring] * (.3f + .7f * grow) + scatter * 7;
            for (int i = 0; i < n; i++) {
                int s = seed + ring * 101 + i * 7;
                float a = i * Mth.TWO_PI / n + age * BoneTempest.SPIN[ring] + BoneFx.hash(s) * .4f;
                float y = BoneTempest.HEIGHT[ring] + .25f * Mth.sin(age * .15f + i) - scatter * scatter * 1.5f;
                float r = radius + .3f * Mth.sin(age * .1f + i * 1.3f);
                Vector3f c = new Vector3f(Mth.cos(a) * r, y, Mth.sin(a) * r);
                // Tumbling end over end
                float t = age * (.25f + .2f * BoneFx.hash(s + 1)) + BoneFx.hash(s + 2) * 6;
                Vector3f d = new Vector3f(Mth.cos(t) * Mth.cos(a + 1), Mth.sin(t), Mth.cos(t) * Mth.sin(a + 1)).mul(.32f + .12f * BoneFx.hash(s + 3));
                BoneRender.bone(bone, p, new Vector3f(c).sub(d), new Vector3f(c).add(d), .09f, packedLight, BoneRender.WHITE);
            }
        }
        VertexConsumer glow = buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW));
        poseStack.pushPose();
        poseStack.translate(0, .06f, 0);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotation(-age * .1f));
        RenderHelper.groundQuad(glow, poseStack.last(), BoneTempest.RADIUS * grow, RuneRender.additive(BoneFx.MARROW, .55f * grow * (1 - scatter)), GlyphRender.FULL);
        poseStack.popPose();
        super.render(tempest, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(BoneTempest tempest) {
        return BoneRender.BONE;
    }
}
