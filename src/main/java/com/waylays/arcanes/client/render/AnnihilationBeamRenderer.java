package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.boss.CannonModel;
import com.waylays.arcanes.client.render.water.WaterMesh;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.boss.AnnihilationBeam;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The Annihilation Beam: drawn by its rune engine's model ({@link CannonModel}).
 */
public class AnnihilationBeamRenderer extends EntityRenderer<AnnihilationBeam> {
    private static final int LIGHT = LightTexture.FULL_BRIGHT;
    private static final int CRIMSON = 0xFF3048;

    public AnnihilationBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(AnnihilationBeam beam, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = beam.tickCount + partialTicks;
        WaterMesh.View view = WaterViews.of(this.entityRenderDispatcher, beam, partialTicks);
        CannonModel.draw(poseStack, buffer, view, beam.forward().toVector3f(), (float) AnnihilationBeam.HEIGHT, beam.charge(partialTicks), beam.firing(partialTicks),
                AnnihilationBeam.DURATION, age, beam.getLength(), beam.getWidth(), LIGHT);
        super.render(beam, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(AnnihilationBeam entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(AnnihilationBeam entity) {
        return BossPropRenderer.BEAM;
    }
}
