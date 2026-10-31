package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.water.WaterJetModel;
import com.waylays.arcanes.entity.water.WaterJet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Pressure Jet: drawn by {@link WaterJetModel} from the caster's hand to where it stops. */
public class WaterJetRenderer extends EntityRenderer<WaterJet> {
    public WaterJetRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(WaterJet jet, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = jet.tickCount + partialTicks;
        Entity caster = jet.getCaster();
        Vec3 origin = jet.getPosition(partialTicks);
        Vec3 from = caster != null ? WaterJet.origin(caster, partialTicks) : origin;
        Vec3 end = jet.getEnd();
        boolean hit = from.distanceTo(end) < WaterJet.RANGE - .3f;
        WaterJetModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, jet, partialTicks), from.subtract(origin).toVector3f(),
                end.subtract(origin).toVector3f(), hit, age, jet.getId() * 31, light);
        super.render(jet, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(WaterJet jet, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(WaterJet jet) {
        return com.waylays.arcanes.client.render.water.WaterMesh.WATER;
    }
}
