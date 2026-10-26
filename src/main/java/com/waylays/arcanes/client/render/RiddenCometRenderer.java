package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.entity.cosmos.RiddenComet;
import com.waylays.arcanes.spells.cosmos.Cosmos;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * A ridden comet: a tumbling nucleus of ice in a bright coma, a long straight ion tail of blue light streaming
 * straight back, and a broad pale dust tail curving away beneath it, stars glinting in both. Diving, it burns: its
 * coma and tails turn the orange of re-entry.
 */
public class RiddenCometRenderer extends EntityRenderer<RiddenComet> {
    private static final Vector3f SUN = new Vector3f(-.6f, .8f, .3f);

    public RiddenCometRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = .8f;
    }

    @Override
    public boolean shouldRender(RiddenComet comet, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(RiddenComet comet, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = comet.tickCount + partialTicks;
        float burn = comet.getDive() < 0 ? 0 : Mth.clamp((age - comet.getDive()) / 10, 0, 1);
        Vec3 motion = comet.getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-4) {
            motion = Vec3.directionFromRotation(comet.getXRot(), comet.getYRot());
        }
        Vec3 back = motion.normalize().scale(-1);
        Vec3 centre = new Vec3(0, .8, 0);
        int coma = GoldRender.lerp(Cosmos.ICE, Cosmos.ORANGE, burn);
        int ion = GoldRender.lerp(Cosmos.BLUE, Cosmos.EMBER, burn);
        int dust = GoldRender.lerp(Cosmos.CREAM, Cosmos.AMBER, burn);
        // The ion tail: straight back, long and blue
        VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
        int n = 14;
        for (int k = 0; k < n; k++) {
            float t0 = (float) k / n, t1 = (float) (k + 1) / n;
            Vec3 a = centre.add(back.scale(t0 * 11)), b = centre.add(back.scale(t1 * 11));
            float fade = 1 - t0;
            float flick = .85f + .15f * Mth.sin(age * .9f + k);
            GlyphRender.beam(stroke, poseStack.last(), a, b, (1.6f - t0) * flick, RuneRender.additive(ion, .45f * fade));
            GlyphRender.beam(stroke, poseStack.last(), a, b, (.5f - t0 * .3f), RuneRender.additive(Cosmos.STAR, .7f * fade));
        }
        // The dust tail: broad, pale, curving away below
        Vec3 side = back.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 prev = centre;
        for (int k = 1; k <= n; k++) {
            float t = (float) k / n;
            Vec3 p = centre.add(back.scale(t * 8)).add(0, -t * t * 2.2, 0).add(side.scale(t * t * 1.2));
            float fade = 1 - t;
            GlyphRender.beam(stroke, poseStack.last(), prev, p, 1.2f + t * 1.8f, RuneRender.additive(dust, .32f * fade));
            prev = p;
        }
        // The coma
        GemRender.glow(poseStack, buffer, centre, coma, 3.4f, .55f);
        GemRender.glow(poseStack, buffer, centre, Cosmos.STAR, 1.6f, .85f);
        // The nucleus, tumbling
        poseStack.pushPose();
        poseStack.translate(centre.x, centre.y, centre.z);
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(age * 4));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(age * 2.5f));
        CosmosRender.planet(poseStack, buffer, CosmosRender.MOON, .75f, age * .01f, SUN, .35f, GoldRender.lerp(0xC8EEFF, 0xFFB070, burn));
        poseStack.popPose();
        // Stars glinting in the tails
        for (int k = 0; k < 7; k++) {
            float h = RuneRender.hash(comet.getId() * 13 + k);
            float t = (h + age * .03f) % 1;
            poseStack.pushPose();
            Vec3 p = centre.add(back.scale(t * 9)).add(side.scale((RuneRender.hash(k * 7) - .5f) * 2 * t)).add(0, -t * 1.2, 0);
            poseStack.translate(p.x, p.y, p.z);
            CosmosRender.twinkle(poseStack, buffer, .45f, k % 2 == 0 ? Cosmos.STAR : dust, 1 - t, age, comet.getId() + k);
            poseStack.popPose();
        }
        super.render(comet, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(RiddenComet comet) {
        return CosmosRender.MOON;
    }
}
