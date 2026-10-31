package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.ritual.RitualFigureModel;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.ritual.Ritual;
import com.waylays.arcanes.ritual.RitualCircle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** A ritual's figure, drawn in chalk on the ground or on a wall, as it is drawn, waits, builds and is cast (see {@link RitualFigureModel}). */
public class RitualCircleRenderer extends EntityRenderer<RitualCircle> {
    public RitualCircleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RitualCircle circle, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Ritual ritual = circle.getRitual();
        Direction wall = circle.getWall();
        RitualMesh.Plane plane = wall != null ? RitualMesh.Plane.wall(wall.getStepX(), wall.getStepZ())
                : RitualMesh.Plane.ground(circle.getYRot() * Mth.DEG_TO_RAD);
        Vector3f[] stations = new Vector3f[ritual.participants];
        for (int i = 0; i < stations.length; i++) {
            Vec3 s = circle.station(i);
            stations[i] = new Vector3f((float) s.x, (float) s.y, (float) s.z);
        }
        int state = switch (circle.getState()) {
            case DRAWING -> RitualFigureModel.DRAWING;
            case WAITING -> RitualFigureModel.WAITING;
            case CHANNELING -> RitualFigureModel.CHANNELING;
            case DONE -> RitualFigureModel.DONE;
            case FAILED -> RitualFigureModel.FAILED;
        };
        RitualFigureModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, circle, partialTicks), ritual.figure.ordinal(), plane, stations,
                circle.getOccupied(), state, circle.sinceState(partialTicks), circle.drawn(partialTicks), circle.progress(partialTicks),
                circle.tickCount + partialTicks, ritual.color, circle.getId() * 31, packedLight);
        super.render(circle, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(RitualCircle circle, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(RitualCircle circle) {
        return RitualMesh.CHALK;
    }
}
