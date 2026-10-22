package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.token.TokenWave;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Token Wave: rows of soul tokens leaping out of the ground in an arc at the wave's front, tumbling forward, the crest
 * a little behind the front, as the wave rolls on.
 */
public class TokenWaveRenderer extends EntityRenderer<TokenWave> {
    private static final int ROWS = 6;
    private static final int COLUMNS = 7;

    public TokenWaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(TokenWave wave, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float strength = wave.strength(partialTicks);
        if (strength <= 0) {
            return;
        }
        float age = wave.tickCount + partialTicks;
        float front = wave.front(partialTicks);
        Vec3 f = wave.forward();
        Vec3 side = new Vec3(-f.z, 0, f.x);
        float yaw = wave.getYRot();
        for (int j = 0; j < ROWS; j++) {
            float d = front - j * .45f;
            if (d < .3f) {
                continue;
            }
            float u = j / (ROWS - 1f);
            float h = Mth.sin(u * Mth.PI) * 1.4f * (1 - .3f * u) * strength;
            for (int c = 0; c < COLUMNS; c++) {
                float lateral = (c / (COLUMNS - 1f) - .5f) * TokenWave.WIDTH;
                // Uneven, so the crest looks poured rather than stacked
                float jitter = .15f * Mth.sin(c * 2.1f + j * 1.3f + age * .3f);
                Vec3 p = f.scale(d + jitter).add(side.scale(lateral));
                poseStack.pushPose();
                poseStack.translate(p.x, h + .2 + .25f * Mth.sin(c * 1.7f + j), p.z);
                poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
                poseStack.mulPose(Axis.XP.rotationDegrees(age * 28 + j * 40 + c * 17));
                TokenRender.token(poseStack, buffer, .36f * strength, GlyphRender.FULL);
                poseStack.popPose();
            }
        }
        super.render(wave, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(TokenWave entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(TokenWave entity) {
        return GlyphRender.GLOW;
    }
}
