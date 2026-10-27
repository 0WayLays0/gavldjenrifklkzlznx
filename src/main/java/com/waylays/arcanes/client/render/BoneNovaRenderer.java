package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.entity.bone.BoneNova;
import com.waylays.arcanes.spells.bone.BoneFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** The Bone Nova: spikes of bone bursting out of the caster's body in every direction, then drawing back in. */
public class BoneNovaRenderer extends EntityRenderer<BoneNova> {
    public BoneNovaRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BoneNova nova, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = nova.tickCount + partialTicks;
        float out = BoneNova.out(age);
        if (out <= .01f) {
            return;
        }
        Entity body = nova.body();
        float h = body == null ? 1 : body.getBbHeight() * .55f;
        float reach = nova.getReach();
        VertexConsumer bone = BoneRender.buffer(buffer);
        PoseStack.Pose p = poseStack.last();
        int seed = nova.getId() * 17;
        for (int i = 0; i < BoneNova.SPIKES; i++) {
            Vec3 d = BoneNova.direction(i);
            float len = reach * (.75f + .35f * BoneFx.hash(seed + i)) * out;
            Vector3f dir = new Vector3f((float) d.x, (float) d.y, (float) d.z);
            Vector3f base = new Vector3f(dir).mul(.3f).add(0, h, 0);
            Vector3f tip = new Vector3f(dir).mul(.3f + len).add(0, h, 0);
            BoneRender.spike(bone, p, base, tip, .1f + .08f * BoneFx.hash(seed + i + 50), new Vector3f(), packedLight, BoneRender.WHITE);
        }
        super.render(nova, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(BoneNova nova) {
        return BoneRender.BONE;
    }
}
