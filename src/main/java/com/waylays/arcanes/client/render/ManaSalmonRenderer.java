package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.errethys.ManaSalmon;
import net.minecraft.client.model.SalmonModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/**
 * Mana salmon: Minecraft's salmon shape in mana, swaying from side to side as salmon swim and pitching up or down
 * with its course; stranded on land it lies on its side and thrashes.
 */
public class ManaSalmonRenderer extends ManaCreatureRenderer<ManaSalmon, SalmonModel<ManaSalmon>> {
    public ManaSalmonRenderer(EntityRendererProvider.Context context) {
        super(context, new SalmonModel<>(context.bakeLayer(ModelLayers.SALMON)), .35f, 1.1f, false);
    }

    @Override
    protected void setupRotations(ManaSalmon entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);
        boolean inWater = entity.isInWater();
        // Out of water it thrashes harder, lying on its side
        float wriggle = inWater ? 4.3f * Mth.sin(.6f * ageInTicks) : 5.6f * Mth.sin(1.02f * ageInTicks);
        poseStack.mulPose(Axis.YP.rotationDegrees(wriggle));
        if (inWater) {
            double dx = entity.getX() - entity.xo;
            double dy = entity.getY() - entity.yo;
            double dz = entity.getZ() - entity.zo;
            float pitch = (float) Mth.clamp(Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz) + 1.0E-3)), -55, 55);
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        }
        poseStack.translate(0, 0, -.4f);
        if (!inWater) {
            poseStack.translate(.2f, .1f, 0);
            poseStack.mulPose(Axis.ZP.rotationDegrees(90));
        }
    }
}
