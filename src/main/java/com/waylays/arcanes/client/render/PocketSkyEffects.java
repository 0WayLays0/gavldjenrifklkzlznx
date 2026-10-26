package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.spells.cosmos.Cosmos;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The sky of the Pocket Universe, out of time: deep space all round (stars, a violet nebula across it, far galaxies),
 * a great ringed planet hanging above the horizon, a blue world and a violet moon, a white sun that never moves, and
 * overhead the TVA's giant clock, its hands stopped a moment before midnight, its second hand trembling on the spot.
 * No clouds, no rain, no sunrise.
 */
public class PocketSkyEffects extends DimensionSpecialEffects {
    /** How far away the sky is drawn (it is drawn first and behind everything, so only its directions matter). */
    private static final float FAR = 90;
    private static final Vec3 SUN = new Vec3(-1, .35, .25).normalize();

    public PocketSkyEffects() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return fogColor;
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, double camX, double camY, double camZ,
                                Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        return true;
    }

    @Override
    public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, net.minecraft.client.renderer.LightTexture lightTexture,
                                     double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public boolean tickRain(ClientLevel level, int ticks, Camera camera) {
        return true;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera, Matrix4f projectionMatrix,
                             boolean isFoggy, Runnable setupFog) {
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(modelViewMatrix);
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        float time = ticks + partialTick;
        draw(poseStack, buffer, time);
        // The sky is out of reach of fog and of the depth of the world drawn over it
        FogRenderer.setupNoFog();
        buffer.endBatch();
        RenderSystem.clear(256, Minecraft.ON_OSX);
        setupFog.run();
        return true;
    }

    /** The whole sky round the origin of the pose (the camera). */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, float time) {
        // Stars, then the nebula drawn across them at a slant
        CosmosRender.skySphere(poseStack, buffer, CosmosRender.STARS, FAR, 8, 0xFFFFFF, 1);
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(28));
        poseStack.mulPose(Axis.XP.rotationDegrees(-12));
        CosmosRender.skySphere(poseStack, buffer, CosmosRender.NEBULA, FAR * .97f, 2, 0xFFFFFF, .6f);
        poseStack.popPose();
        // Far galaxies
        galaxy(poseStack, buffer, 150, 32, 16, 20);
        galaxy(poseStack, buffer, -70, 55, 11, -35);
        galaxy(poseStack, buffer, 20, -8, 9, 60);
        // The sun that never moves
        poseStack.pushPose();
        Vec3 sun = SUN.scale(FAR * .9);
        poseStack.translate(sun.x, sun.y, sun.z);
        CosmosRender.star(poseStack, buffer, 26, 0xFFF4D8, 1, 0);
        GemRender.glow(poseStack, buffer, Vec3.ZERO, Cosmos.AMBER, 16, .55f);
        poseStack.popPose();
        Vector3f sunDir = CosmosRender.sunFrom(SUN);
        // The great ringed planet above the horizon
        planet(poseStack, buffer, new Vec3(.55, .32, -.77), 62, 13, CosmosRender.PLANET_GAS, time * .00015f, sunDir, 0xFFE6C8, 22, true);
        // A blue world and a violet moon
        planet(poseStack, buffer, new Vec3(-.7, .18, -.69), 70, 4.5f, CosmosRender.PLANET_OCEAN, time * .0004f, sunDir, 0x7FB2FF, 0, false);
        planet(poseStack, buffer, new Vec3(.3, .5, .81), 74, 2.6f, CosmosRender.PLANET_ICE, time * .0006f, sunDir, 0xB49CFF, 0, false);
        // The TVA's clock overhead, stopped a moment before midnight
        poseStack.pushPose();
        Vec3 up = new Vec3(.08, .95, -.3).normalize();
        Vec3 at = up.scale(FAR * .62);
        poseStack.translate(at.x, at.y, at.z);
        poseStack.mulPose(new Quaternionf().rotationTo(0, 0, 1, (float) -up.x, (float) -up.y, (float) -up.z));
        GlyphRender.halo(buffer, poseStack.last(), GlyphRender.GLOW, 30, RuneRender.additive(Cosmos.ORANGE, .22f));
        // The second hand never gets past its tick: it trembles on it
        float tremble = (time % 20 < 2 ? .004f : 0) + .0015f * Mth.sin(time * 2.7f);
        CosmosRender.clock(poseStack, buffer, 19, .95f, 11.92f / 12f, 59f / 60f, 58f / 60f + tremble);
        poseStack.popPose();
    }

    private static void galaxy(PoseStack poseStack, MultiBufferSource buffer, float yaw, float pitch, float size, float roll) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.translate(0, 0, -FAR * .93f);
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        RenderHelper.rect(buffer.getBuffer(RenderType.eyes(CosmosRender.GALAXY)), poseStack.last(), size * 2, size * 2, 0xFFFFFFFF, GlyphRender.FULL, true);
        poseStack.popPose();
    }

    private static void planet(PoseStack poseStack, MultiBufferSource buffer, Vec3 dir, float distance, float radius,
                               net.minecraft.resources.ResourceLocation texture, float spin, Vector3f sun, int air, float ringOuter, boolean rings) {
        Vec3 at = dir.normalize().scale(distance);
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        CosmosRender.planet(poseStack, buffer, texture, radius, spin, sun, .06f, 0xFFFFFF);
        CosmosRender.atmosphere(poseStack, buffer, radius, air, .55f);
        if (rings) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(-18));
            poseStack.mulPose(Axis.XP.rotationDegrees(14));
            CosmosRender.ring(poseStack, buffer, radius * 1.25f, ringOuter, 0xFFFFFF, .85f);
        }
        poseStack.popPose();
    }
}
