package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.cosmos.TimeDoor;
import com.waylays.arcanes.spells.cosmos.Cosmos;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

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
        // The arc of light joining the pair, its dashes flowing from the way in to the way out
        TimeDoor partner = door.getPartner();
        if (door.getMode() == TimeDoor.Mode.GATE_IN && partner != null) {
            Vec3 from = new Vec3(0, TimeDoor.HEIGHT + .2, 0);
            Vec3 to = partner.getPosition(partialTicks).subtract(door.getPosition(partialTicks)).add(0, TimeDoor.HEIGHT + .2, 0);
            Vec3 mid = from.add(to).scale(.5).add(0, to.horizontalDistance() * .3 + 2.5, 0);
            VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
            int n = 40;
            Vec3 prev = from;
            for (int k = 1; k <= n; k++) {
                float t = (float) k / n;
                Vec3 p = from.scale((1 - t) * (1 - t)).add(mid.scale(2 * t * (1 - t))).add(to.scale(t * t));
                float dash = ((k - age * .5f) % 5 + 5) % 5;
                float lit = dash < 2.5f ? 1 : .25f;
                GlyphRender.beam(stroke, poseStack.last(), prev, p, .22f * lit, RuneRender.additive(Cosmos.ORANGE, .6f * lit * open));
                GlyphRender.beam(stroke, poseStack.last(), prev, p, .07f, RuneRender.additive(Cosmos.CREAM, .7f * lit * open));
                prev = p;
            }
        }
        super.render(door, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** The pair's arc reaches far out of the door's box: drawn whenever the door is near. */
    @Override
    public boolean shouldRender(TimeDoor door, Frustum frustum, double x, double y, double z) {
        return door.getMode() == TimeDoor.Mode.GATE_IN || super.shouldRender(door, frustum, x, y, z);
    }

    @Override
    public ResourceLocation getTextureLocation(TimeDoor door) {
        return CosmosRender.HAZE;
    }
}
