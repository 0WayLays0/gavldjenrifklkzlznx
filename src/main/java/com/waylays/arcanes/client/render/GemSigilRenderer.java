package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.entity.gem.GemSigil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** The jeweller's sigil burning on the ground in its gem's colour, turning, flaring as it is drawn. */
public class GemSigilRenderer extends EntityRenderer<GemSigil> {
    public GemSigilRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GemSigil sigil, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = sigil.tickCount + partialTicks;
        float strength = sigil.strength(partialTicks);
        // Drawn in from small, a flare as it lands
        float grow = Math.min(1, .6f + age / 15f);
        GemRender.sigil(poseStack, buffer, sigil.getGem().color, sigil.getRadius() * grow, strength, sigil.getYRot() + age * 1.6f);
        super.render(sigil, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(GemSigil sigil) {
        return GemRender.SIGIL;
    }
}
