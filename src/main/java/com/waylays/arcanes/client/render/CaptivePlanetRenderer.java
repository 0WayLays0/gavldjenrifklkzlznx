package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.cosmos.CaptivePlanet;
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

import java.util.ArrayList;
import java.util.List;

/**
 * A captive planet: a small world turning in the air (a ringed gas giant, a blue world, an ice giant or a molten one),
 * lit from one side, in a glow of its own air, stars twinkling round it; the orbit its prisoner is swept round traced
 * in faint light, and a comet's tail of light behind the prisoner. When it slings its prisoner away the planet sinks
 * back into the dark and the tail follows the prisoner's flight.
 */
public class CaptivePlanetRenderer extends EntityRenderer<CaptivePlanet> {
    private static final int[] AIR = {0xFFD9A0, 0x7FB2FF, 0xB49CFF, 0xFF7A30};
    private static final Vector3f SUN = new Vector3f(-.8f, .6f, .4f);

    public CaptivePlanetRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(CaptivePlanet planet, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(CaptivePlanet planet, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = planet.tickCount + partialTicks;
        int slung = planet.getSlungAt();
        float grow = Mth.clamp(age / 10, 0, 1);
        grow = 1 - (1 - grow) * (1 - grow);
        float gone = slung < 0 ? 0 : Mth.clamp((age - slung) / 20, 0, 1);
        float size = 1.3f * grow * (1 - gone);
        int kind = Math.floorMod(planet.getKind(), CosmosRender.PLANETS.length);
        Vec3 origin = planet.getPosition(partialTicks);
        // The prisoner's tail of light
        List<Vec3> points = new ArrayList<>(planet.trail);
        if (points.size() > 1) {
            VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
            float width = slung < 0 ? .45f : .8f;
            for (int k = 0; k + 1 < points.size(); k++) {
                float t = 1 - (float) k / points.size();
                Vec3 a = points.get(k).subtract(origin), b = points.get(k + 1).subtract(origin);
                GlyphRender.beam(stroke, poseStack.last(), a, b, width * t, RuneRender.additive(Cosmos.ICE, .55f * t));
                GlyphRender.beam(stroke, poseStack.last(), a, b, width * .3f * t, RuneRender.additive(Cosmos.STAR, .8f * t));
            }
        }
        if (size <= .02f) {
            return;
        }
        // The orbit, traced faintly
        if (slung < 0) {
            VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
            int n = 64;
            for (int k = 0; k < n; k++) {
                Vec3 a = planet.orbitPoint(k * Mth.TWO_PI / n, CaptivePlanet.ORBIT);
                Vec3 b = planet.orbitPoint((k + 1) * Mth.TWO_PI / n, CaptivePlanet.ORBIT);
                float dash = (k + age * planet.getSpin() * 10) % 4 < 2 ? 1 : .4f;
                GlyphRender.beam(stroke, poseStack.last(), a, b, .08f, RuneRender.additive(AIR[kind], .35f * grow * dash));
            }
        }
        // The planet, its air, its rings
        poseStack.pushPose();
        poseStack.translate(0, -1.1, 0);
        CosmosRender.planet(poseStack, buffer, CosmosRender.PLANETS[kind], size, age * .004f, SUN, .12f, 0xFFFFFF);
        CosmosRender.atmosphere(poseStack, buffer, size, AIR[kind], .65f);
        GemRender.glow(poseStack, buffer, Vec3.ZERO, AIR[kind], size * 3.2f, .18f);
        if (kind == 0 || kind == 2) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(-planet.getTilt()));
            CosmosRender.ring(poseStack, buffer, size * 1.35f, size * (kind == 0 ? 2.3f : 1.8f), kind == 0 ? 0xFFFFFF : 0xC8C0FF, kind == 0 ? .85f : .5f);
        }
        poseStack.popPose();
        // Stars twinkling round it
        for (int k = 0; k < 8; k++) {
            float h = RuneRender.hash(planet.getId() * 31 + k);
            float a = h * Mth.TWO_PI + age * .01f;
            poseStack.pushPose();
            poseStack.translate(Mth.cos(a) * (2.2f + h * 2), -1.1 + (RuneRender.hash(k * 7 + planet.getId()) - .5f) * 3, Mth.sin(a) * (2.2f + h * 2));
            CosmosRender.twinkle(poseStack, buffer, .35f, k % 3 == 0 ? Cosmos.GOLD : Cosmos.STAR, grow * (1 - gone), age, planet.getId() + k);
            poseStack.popPose();
        }
        super.render(planet, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(CaptivePlanet planet) {
        return CosmosRender.PLANET_GAS;
    }
}
