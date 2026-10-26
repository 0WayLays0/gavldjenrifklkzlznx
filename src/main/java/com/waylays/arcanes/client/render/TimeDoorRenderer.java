package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.cosmos.TimeDoor;
import com.waylays.arcanes.spells.cosmos.Cosmos;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * A TVA time door: the frame of orange light, the haze rising in it and the swirl at its heart (see
 * {@link CosmosRender#door}), a pool of orange light on the ground at its foot. The door out of an island also
 * carries a small clock over its lintel, stopped like everything there.
 */
public class TimeDoorRenderer extends EntityRenderer<TimeDoor> {
    public TimeDoorRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(TimeDoor door, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = door.tickCount + partialTicks;
        float open = door.openness(age);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-door.getYRot()));
        // The pool of light at its foot
        poseStack.pushPose();
        poseStack.translate(0, .03, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 1.5f));
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), 1.6f * Math.max(.2f, open),
                RuneRender.additive(Cosmos.ORANGE, .55f * open), GlyphRender.FULL);
        poseStack.popPose();
        CosmosRender.door(poseStack, buffer, TimeDoor.WIDTH, TimeDoor.HEIGHT, open, age);
        if (door.getMode() == TimeDoor.Mode.EXIT) {
            poseStack.pushPose();
            poseStack.translate(0, TimeDoor.HEIGHT + .55f, 0);
            CosmosRender.clock(poseStack, buffer, .42f, open, 11.92f / 12f, 59f / 60f, -1);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(door, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(TimeDoor door) {
        return CosmosRender.HAZE;
    }
}
