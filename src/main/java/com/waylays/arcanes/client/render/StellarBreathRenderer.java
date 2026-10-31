package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.fire.BreathModel;
import com.waylays.arcanes.client.render.fire.FireMesh;
import com.waylays.arcanes.entity.fire.StellarBreath;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Stellar Breath: drawn by {@link BreathModel} from the caster's hand. */
public class StellarBreathRenderer extends EntityRenderer<StellarBreath> {
    public StellarBreathRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(StellarBreath e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = e.tickCount + partialTicks;
        Entity caster = e.getCaster();
        Vec3 origin = e.getPosition(partialTicks);
        Vec3 from = caster != null ? StellarBreath.origin(caster, partialTicks) : origin;
        Vec3 dir = caster != null ? caster.getViewVector(partialTicks) : new Vec3(0, 0, 1);
        BreathModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, e, partialTicks), from.subtract(origin).toVector3f(), dir.toVector3f(),
                e.getReach(), age, e.getHeat(), e.getId() * 37, light);
        super.render(e, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(StellarBreath e, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(StellarBreath e) {
        return FireMesh.GLOW;
    }
}
