package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.entity.water.WaterJet;
import com.waylays.arcanes.spells.water.WaterFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Pressure Jet: a white-hot thread of water rushing out of the hand inside a clear sheath, both racing along, a ring
 * of water where it leaves the hand, and at its end a fan of spray thrown back and drops flying.
 */
public class WaterJetRenderer extends EntityRenderer<WaterJet> {
    public WaterJetRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(WaterJet jet, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = jet.tickCount + partialTicks;
        Entity caster = jet.getCaster();
        Vec3 origin = new Vec3(Mth.lerp(partialTicks, jet.xo, jet.getX()), Mth.lerp(partialTicks, jet.yo, jet.getY()), Mth.lerp(partialTicks, jet.zo, jet.getZ()));
        Vec3 from = caster != null ? WaterJet.origin(caster, partialTicks) : origin;
        Vec3 end = jet.getEnd();
        Vec3 d = end.subtract(from);
        float len = (float) d.length();
        if (len < .1f) {
            return;
        }
        float grow = Mth.clamp(age / 3, 0, 1);
        Vec3 start = from.subtract(origin);
        poseStack.pushPose();
        poseStack.translate(start.x, start.y, start.z);
        BoneRender.alignUp(poseStack, new Vector3f((float) d.x, (float) d.y, (float) d.z));
        float l = len * grow;
        VertexConsumer water = WaterRender.water(buffer);
        PoseStack.Pose p = poseStack.last();
        // The core, near white, and its clear sheath, racing along
        WaterRender.funnel(water, p, .06f, 0, .06f, l, 6, 8, 2, 0, -age * .6f, 0xE8FAFF, .9f, .85f, light);
        WaterRender.funnel(water, p, .16f, 0, .2f, l, 6, 12, 3, .5f, -age * .35f, WaterRender.sea(.8f), .45f, .3f, light);
        // A ring of water where it leaves the hand
        WaterRender.ring(water, p, .05f, .12f, .32f + .04f * Mth.sin(age * 1.3f), 16, age * .2f, RuneRender.alpha(WaterFx.FOAM, .7f), RuneRender.alpha(WaterFx.FOAM, 0), light);
        // A fan of spray thrown back at its end
        poseStack.translate(0, l, 0);
        p = poseStack.last();
        int seed = jet.getId() * 31;
        for (int k = 0; k < 10; k++) {
            float a = k * Mth.TWO_PI / 10 + age * .3f;
            float r = .5f + .25f * Mth.sin(age * .9f + k);
            Vector3f tip = new Vector3f(Mth.cos(a) * r, -.35f - .2f * WaterFx.hash(seed + k), Mth.sin(a) * r);
            BoneRender.tube(water, p, new Vector3f(), tip, .07f, .01f, 5, light, RuneRender.alpha(WaterFx.FOAM, .6f));
        }
        poseStack.popPose();
        // Drops flying off its end
        VertexConsumer drops = WaterRender.drops(buffer);
        Quaternionf camera = this.entityRenderDispatcher.cameraOrientation();
        Vec3 tip = end.subtract(origin);
        for (int k = 0; k < 14; k++) {
            float t = (age * .15f + WaterFx.hash(seed + k)) % 1;
            float a = WaterFx.hash(seed + k * 3) * Mth.TWO_PI;
            float out = t * (.6f + .6f * WaterFx.hash(seed + k * 5));
            WaterRender.drop(drops, poseStack, camera, (float) tip.x + Mth.cos(a) * out, (float) tip.y + out * .8f - t * t * 1.5f, (float) tip.z + Mth.sin(a) * out,
                    .12f, RuneRender.alpha(WaterFx.FOAM, .9f * (1 - t)), light);
        }
        super.render(jet, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(WaterJet jet, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(WaterJet jet) {
        return WaterRender.WATER;
    }
}
