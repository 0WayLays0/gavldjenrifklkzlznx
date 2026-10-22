package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.gold.MoltenPool;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * A pool of Molten Torrent: a puddle of gold with a ragged edge that spreads out, simmering white-hot and orange with
 * bubbles of light, then cools to a plate of polished gold, dull and still, that breaks up into coins.
 */
public class MoltenPoolRenderer extends EntityRenderer<MoltenPool> {
    private static final ResourceLocation POOL = ArcanesOublies.id("textures/entity/molten_pool.png");
    private static final int EDGE = 14;

    public MoltenPoolRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MoltenPool pool, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = pool.tickCount + partialTicks;
        float spread = Mth.clamp(age / 6f, .05f, 1f);
        float heat = Mth.clamp((pool.getExpires() - age) / 25f, 0f, 1f);
        float gone = Mth.clamp((age - pool.getExpires() - MoltenPool.COOLED_TICKS + 6) / 6f, 0, 1);
        int seed = pool.getId();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(pool.getYRot()));
        float size = MoltenPool.RADIUS * spread * (1 - gone * .6f);
        poseStack.scale(size, 1, size);
        int light = heat > .05f ? GlyphRender.FULL : LightTexture.pack(Math.max(8, LightTexture.block(packedLight)), LightTexture.sky(packedLight));
        Solid.draw(GoldRender.metal(buffer), poseStack.last(), puddle(seed), GoldRender.heat(heat * .9f), 255, age, heat > .05f ? .3f : .9f, light);
        poseStack.popPose();

        if (heat > .01f) {
            float pulse = .85f + Mth.sin(age * .2f) * .15f;
            poseStack.pushPose();
            poseStack.translate(0, .075, 0);
            poseStack.mulPose(Axis.YP.rotationDegrees(pool.getYRot() + age * .5f));
            RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(POOL)), poseStack.last(), MoltenPool.RADIUS * 1.1f * spread,
                    RuneRender.additive(0xFFFFFF, heat * pulse), LightTexture.FULL_BRIGHT);
            poseStack.popPose();
            // Bubbles of light rising and bursting on the surface
            for (int k = 0; k < 3; k++) {
                float phase = (age * .05f + RuneRender.hash(seed * 7 + k)) % 1;
                float a = RuneRender.hash(seed * 13 + k) * Mth.TWO_PI, r = RuneRender.hash(seed * 17 + k) * .7f * MoltenPool.RADIUS * spread;
                GemRender.glow(poseStack, buffer, new Vec3(Mth.cos(a) * r, .1, Mth.sin(a) * r), GoldRender.PALE, .35f + phase * .3f,
                        heat * Mth.sin(phase * Mth.PI));
            }
        }
        super.render(pool, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** A flat puddle about a block across, its edge ragged after {@code seed}: a low rim and a slightly domed top. */
    private static float[] puddle(int seed) {
        float[][] rim = new float[EDGE][];
        float[][] lip = new float[EDGE][];
        for (int i = 0; i < EDGE; i++) {
            float a = i * Mth.TWO_PI / EDGE;
            float r = .82f + .3f * RuneRender.hash(seed * 31 + i);
            rim[i] = new float[]{Mth.cos(a) * r, .01f, Mth.sin(a) * r};
            lip[i] = new float[]{Mth.cos(a) * r * .9f, .06f, Mth.sin(a) * r * .9f};
        }
        return new Solid.Builder().band(rim, lip).fan(lip, new float[]{0, .07f, 0}).build();
    }

    @Override
    public ResourceLocation getTextureLocation(MoltenPool pool) {
        return GoldRender.METAL;
    }
}
