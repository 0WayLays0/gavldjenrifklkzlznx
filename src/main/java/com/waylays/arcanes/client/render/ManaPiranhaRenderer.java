package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.client.model.ManaPiranhaModel;
import com.waylays.arcanes.entity.errethys.ManaPiranha;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/**
 * Mana piranha: the piranha model in mana, its eyes and teeth shining, darting with short sways of the body and
 * pitching up or down with its course; stranded on land it lies on its side and thrashes.
 */
public class ManaPiranhaRenderer extends ManaCreatureRenderer<ManaPiranha, ManaPiranhaModel> {
    public ManaPiranhaRenderer(EntityRendererProvider.Context context) {
        super(context, new ManaPiranhaModel(context.bakeLayer(ManaPiranhaModel.LAYER)), .3f, .9f, true);
    }

    @Override
    protected void setupRotations(ManaPiranha entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks, float scale) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks, scale);
        boolean inWater = entity.isInWater();
        // Out of water it thrashes harder, lying on its side
        float wriggle = inWater ? 3.2f * Mth.sin(.7f * ageInTicks) : 6f * Mth.sin(1.1f * ageInTicks);
        poseStack.mulPose(Axis.YP.rotationDegrees(wriggle));
        if (inWater) {
            double dx = entity.getX() - entity.xo;
            double dy = entity.getY() - entity.yo;
            double dz = entity.getZ() - entity.zo;
            float pitch = (float) Mth.clamp(Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz) + 1.0E-3)), -50, 50);
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        } else {
            poseStack.translate(.25f, .15f, 0);
            poseStack.mulPose(Axis.ZP.rotationDegrees(90));
        }
    }
}
