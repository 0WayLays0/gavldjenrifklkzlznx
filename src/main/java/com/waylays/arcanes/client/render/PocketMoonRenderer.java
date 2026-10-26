package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.entity.cosmos.PocketMoon;
import com.waylays.arcanes.spells.cosmos.Cosmos;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * A pocket moon: a cratered grey moon lit from one side by a sun that does not move, so it waxes and wanes as it goes
 * round, in a cold silver glow; its orbit traced faintly round its caster; a silver streak behind it when thrown.
 */
public class PocketMoonRenderer extends EntityRenderer<PocketMoon> {
    private static final Vector3f SUN = new Vector3f(1, .25f, .2f);
    private static final int SILVER = 0xDDE6FF;

    public PocketMoonRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(PocketMoon moon, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(PocketMoon moon, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = moon.tickCount + partialTicks;
        float grow = Mth.clamp(age / 8, 0, 1);
        float fade = Mth.clamp((moon.getLife() - age) / 10, 0, 1);
        float size = PocketMoon.SIZE * grow * fade;
        Vec3 origin = moon.getPosition(partialTicks);
        VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
        // The orbit round its caster
        Entity caster = moon.getCaster();
        if (caster != null && moon.getState() == PocketMoon.State.ORBIT) {
            Vec3 c = caster.getPosition(partialTicks).subtract(origin).add(0, caster.getBbHeight() * .6, 0);
            int n = 48;
            for (int k = 0; k < n; k++) {
                float a0 = k * Mth.TWO_PI / n, a1 = (k + 1) * Mth.TWO_PI / n;
                Vec3 p0 = c.add(Mth.cos(a0) * PocketMoon.ORBIT, 0, Mth.sin(a0) * PocketMoon.ORBIT);
                Vec3 p1 = c.add(Mth.cos(a1) * PocketMoon.ORBIT, 0, Mth.sin(a1) * PocketMoon.ORBIT);
                GlyphRender.beam(stroke, poseStack.last(), p0, p1, .05f, RuneRender.additive(SILVER, .22f * grow * fade));
            }
        }
        // A streak behind it when thrown
        List<Vec3> trail = new ArrayList<>(moon.trail);
        if (moon.getState() != PocketMoon.State.ORBIT && trail.size() > 1) {
            for (int k = 0; k + 1 < trail.size(); k++) {
                float t = 1 - (float) k / trail.size();
                GlyphRender.beam(stroke, poseStack.last(), trail.get(k).subtract(origin), trail.get(k + 1).subtract(origin), .7f * t,
                        RuneRender.additive(SILVER, .5f * t));
            }
        }
        if (size <= .01f) {
            return;
        }
        GemRender.glow(poseStack, buffer, Vec3.ZERO, SILVER, size * 4, .35f);
        GemRender.glow(poseStack, buffer, Vec3.ZERO, Cosmos.STAR, size * 1.8f, .25f);
        CosmosRender.planet(poseStack, buffer, CosmosRender.MOON, size, age * .002f, SUN, .05f, 0xFFFFFF);
        CosmosRender.atmosphere(poseStack, buffer, size, SILVER, .35f);
        super.render(moon, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(PocketMoon moon) {
        return CosmosRender.MOON;
    }
}
