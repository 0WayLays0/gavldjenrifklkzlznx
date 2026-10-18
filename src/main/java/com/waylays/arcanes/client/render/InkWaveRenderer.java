package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.InkWave;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The wave of living ink: glossy black splats spreading over the ground in a widening cone, with faint violet runes
 * drifting in the ink, fading once the wave is spent.
 */
public class InkWaveRenderer extends EntityRenderer<InkWave> {
    private static final ResourceLocation SPLASH = ArcanesOublies.id("textures/entity/ink_splash.png");
    private static final ResourceLocation RUNES = ArcanesOublies.id("textures/entity/page_runes.png");

    public InkWaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(InkWave entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        float reach = entity.reach(age);
        float fade = 1 - Mth.clamp((age - entity.spreadTicks()) / InkWave.FADE_TICKS, 0f, 1f);
        if (fade <= 0) {
            return;
        }
        Vec3 forward = entity.forward();
        Vec3 side = new Vec3(-forward.z, 0, forward.x);
        int alpha = (int) (230 * fade);
        for (int i = 0; i * .9f <= reach; i++) {
            float along = i * .9f;
            float half = InkWave.halfWidth(along);
            // Two or three splats across the cone, placed by a fixed pseudo-random pattern
            int across = 1 + (int) (half / 1.2f);
            for (int j = 0; j < across; j++) {
                float h = hash(i * 7 + j * 13);
                float lateral = across == 1 ? (h - .5f) * .6f : ((j / (float) (across - 1)) * 2 - 1) * half * .8f + (h - .5f) * .5f;
                float grow = Mth.clamp((reach - along) / 1.5f, 0f, 1f);
                float size = (.9f + h * .8f + along * .08f) * grow;
                if (size <= 0) {
                    continue;
                }
                poseStack.pushPose();
                poseStack.translate(forward.x * along + side.x * lateral, .02f + (i + j) % 3 * .003f, forward.z * along + side.z * lateral);
                poseStack.mulPose(Axis.YP.rotationDegrees(h * 360f));
                RenderHelper.groundQuad(buffer.getBuffer(RenderType.entityTranslucent(SPLASH)), poseStack.last(), size, (alpha << 24) | 0xFFFFFF, packedLight);
                if ((i + j) % 3 == 0) {
                    // Runes drifting in the ink
                    float glow = fade * (.35f + .25f * Mth.sin(age * .2f + i));
                    poseStack.translate(0, .01f, 0);
                    poseStack.mulPose(Axis.YP.rotationDegrees(age * 2f));
                    int v = (int) (Mth.clamp(glow, 0f, 1f) * 255);
                    RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RUNES)), poseStack.last(), size * .45f, 0xFF000000 | (v * 2 / 3 << 16) | (v / 3 << 8) | v, LightTexture.FULL_BRIGHT);
                }
                poseStack.popPose();
            }
        }
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static float hash(int n) {
        n = (n << 13) ^ n;
        return ((n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff) / (float) 0x7fffffff;
    }

    @Override
    public ResourceLocation getTextureLocation(InkWave entity) {
        return SPLASH;
    }
}
