package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.wall.Citadel;
import com.waylays.arcanes.spells.wall.WallFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The Fallen Citadel: eight crenellated brick walls rising one after another round an octagon, a tall tower at every
 * other corner, all of stone, mossy and cracked stone bricks. When it falls, each wall topples inward from its foot.
 */
public class CitadelRenderer extends EntityRenderer<Citadel> {
    public CitadelRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(Citadel citadel, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float fallen = citadel.fallen(partialTicks);
        int seed = citadel.getId() * 131;
        for (int i = 0; i < Citadel.WALLS; i++) {
            float risen = citadel.risen(i, partialTicks);
            if (risen <= .001f) {
                continue;
            }
            Vec3 a = citadel.corner(i);
            Vec3 b = citadel.corner((i + 1) % Citadel.WALLS);
            Vec3 mid = a.add(b).scale(.5);
            float length = (float) a.distanceTo(b);
            poseStack.pushPose();
            poseStack.translate(mid.x, -(1 - risen) * (fallen > 0 ? 1.6f : Citadel.WALL_HEIGHT), mid.z);
            // Turned so that its +Z faces the middle: it topples that way
            poseStack.mulPose(Axis.YP.rotation((float) Mth.atan2(-mid.x, -mid.z)));
            if (fallen > 0) {
                poseStack.mulPose(Axis.XP.rotationDegrees(fallen * (.9f + .1f * WallFx.hash(seed + i))));
            }
            WallPieceRenderer.wall(poseStack, buffer, packedLight, seed + i * 17, length + .4f, Citadel.WALL_HEIGHT);
            poseStack.popPose();
            if (i % 2 == 0) {
                tower(poseStack, buffer, packedLight, seed + i * 29, a, risen, fallen);
            }
        }
        super.render(citadel, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** A square tower on a corner: two blocks wide, wider battlements at the top. */
    private static void tower(PoseStack poseStack, MultiBufferSource buffer, int light, int seed, Vec3 at, float risen, float fallen) {
        poseStack.pushPose();
        poseStack.translate(at.x, -(1 - risen) * (fallen > 0 ? 1.6f : Citadel.TOWER_HEIGHT), at.z);
        poseStack.mulPose(Axis.YP.rotation((float) Mth.atan2(-at.x, -at.z)));
        if (fallen > 0) {
            poseStack.mulPose(Axis.XP.rotationDegrees(fallen * .95f));
        }
        float body = Citadel.TOWER_HEIGHT - .8f;
        for (int r = 0; r < Mth.ceil(body); r++) {
            float rowHeight = Math.min(1, body - r);
            for (int k = 0; k < 4; k++) {
                float x = (k & 1) - .5f;
                float z = (k >> 1) - .5f;
                RuneRender.block(WallFx.brick(seed + r * 7 + k), poseStack, buffer, light, x, r, z, 1.001f, rowHeight + .002f, 1.001f, 0, 0);
            }
        }
        // A wider ring of battlements, merlons on its corners
        RuneRender.block(WallFx.brick(seed + 91), poseStack, buffer, light, 0, body, 0, 2.6f, .35f, 2.6f, 0, 0);
        for (int k = 0; k < 4; k++) {
            float x = ((k & 1) * 2 - 1) * 1.05f;
            float z = ((k >> 1) * 2 - 1) * 1.05f;
            RuneRender.block(WallFx.brick(seed + 77 + k), poseStack, buffer, light, x, body + .35f, z, .5f, .45f, .5f, 0, 0);
        }
        poseStack.popPose();
    }

    @Override
    public boolean shouldRender(Citadel citadel, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(citadel.getBoundingBox().inflate(Citadel.RADIUS + 3, 1, Citadel.RADIUS + 3).expandTowards(0, Citadel.TOWER_HEIGHT + 2, 0));
    }

    @Override
    public ResourceLocation getTextureLocation(Citadel citadel) {
        return RuneRender.GLOW;
    }
}
