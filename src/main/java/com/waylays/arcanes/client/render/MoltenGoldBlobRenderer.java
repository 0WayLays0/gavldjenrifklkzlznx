package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.entity.gold.MoltenGoldBlob;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * A gob of Molten Torrent: a short streak of molten gold drawn back along its flight, white at its core, with a hot
 * glow round its head. One after another, the gobs make a running stream.
 */
public class MoltenGoldBlobRenderer extends EntityRenderer<MoltenGoldBlob> {
    public MoltenGoldBlobRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MoltenGoldBlob blob, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = blob.tickCount + partialTicks;
        Vec3 head = new Vec3(0, blob.getBbHeight() * .5, 0);
        Vec3 motion = blob.getDeltaMovement();
        // Cooling a little as it flies
        float heat = Math.max(.45f, 1 - age / 40f);
        GemRender.ray(poseStack, buffer, head, head.subtract(motion.scale(1.3)), GoldRender.heat(heat), .2f, .95f, age + blob.getId());
        GemRender.glow(poseStack, buffer, head, GoldRender.MOLTEN, .7f, .7f);
        super.render(blob, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MoltenGoldBlob blob) {
        return GoldRender.METAL;
    }
}
