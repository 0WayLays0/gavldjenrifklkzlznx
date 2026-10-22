package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Drawing shared by the Gold Magic renderers: polished gold (ingots, coins, spikes, blades) as solid shapes that
 * gleam as they turn, the Sun Stone of Eldorado burning on the ground, and molten gold's glow.
 */
public final class GoldRender {
    /** A white metal texture, tinted gold: it keeps the solids crisp and lets them be any shade of gold. */
    public static final ResourceLocation METAL = ArcanesOublies.id("textures/entity/gold/metal.png");
    public static final ResourceLocation COIN = ArcanesOublies.id("textures/entity/gold/coin.png");
    public static final ResourceLocation SIGIL = ArcanesOublies.id("textures/entity/gold/sun_sigil.png");

    /** Polished gold, its pale highlight, deep gold, and the glow of molten gold. */
    public static final int GOLD = 0xFFC93C;
    public static final int PALE = 0xFFF0A0;
    public static final int DEEP = 0xC88414;
    public static final int MOLTEN = 0xFF7A1A;

    /** An ingot one block long (along Z), as Minecraft's: a slanted bar, its top smaller than its base. */
    public static final float[] INGOT = new Solid.Builder()
            .band(new float[][]{{-.25f, 0, -.5f}, {.25f, 0, -.5f}, {.25f, 0, .5f}, {-.25f, 0, .5f}},
                    new float[][]{{-.17f, .22f, -.4f}, {.17f, .22f, -.4f}, {.17f, .22f, .4f}, {-.17f, .22f, .4f}})
            .quad(new float[]{-.17f, .22f, -.4f}, new float[]{-.17f, .22f, .4f}, new float[]{.17f, .22f, .4f}, new float[]{.17f, .22f, -.4f})
            .quad(new float[]{-.25f, 0, -.5f}, new float[]{.25f, 0, -.5f}, new float[]{.25f, 0, .5f}, new float[]{-.25f, 0, .5f})
            .build();

    /** A coin a block across (in the XZ plane, centred), its rim milled. */
    public static final float[] COIN_EDGE = new Solid.Builder()
            .band(Solid.ring(16, -.04f, .5f, .5f, 0), Solid.ring(16, .04f, .5f, .5f, 0))
            .build();

    /** A four-sided spike of gold one block high, faceted at its foot and its point. */
    public static final float[] SPIKE = new Solid.Builder()
            .band(Solid.ring(4, 0, .3f, .3f, .5f), Solid.ring(4, .18f, .26f, .26f, .5f))
            .band(Solid.ring(4, .18f, .26f, .26f, .5f), Solid.ring(4, .75f, .12f, .12f, .5f))
            .fan(Solid.ring(4, .75f, .12f, .12f, .5f), new float[]{0, 1, 0})
            .fan(new float[][]{{.3f, 0, 0}, {0, 0, -.3f}, {-.3f, 0, 0}, {0, 0, .3f}}, new float[]{0, -.05f, 0})
            .build();

    private GoldRender() {}

    public static VertexConsumer metal(MultiBufferSource buffer) {
        return buffer.getBuffer(RenderType.entityCutoutNoCull(METAL));
    }

    /** A solid of gold drawn at the origin, {@code size} scaled. */
    public static void solid(PoseStack poseStack, MultiBufferSource buffer, float[] shape, int rgb, float size, float time, float shine) {
        poseStack.pushPose();
        poseStack.scale(size, size, size);
        Solid.draw(metal(buffer), poseStack.last(), shape, rgb, 255, time, shine, GlyphRender.FULL);
        poseStack.popPose();
    }

    /** A gold coin {@code size} across, centred on the origin, lying in the XZ plane: its two faces stamped with the sun. */
    public static void coin(PoseStack poseStack, MultiBufferSource buffer, float size, float time, int seed) {
        poseStack.pushPose();
        poseStack.scale(size, size, size);
        Solid.draw(metal(buffer), poseStack.last(), COIN_EDGE, DEEP, 255, time, .6f, GlyphRender.FULL);
        VertexConsumer face = buffer.getBuffer(RenderType.entityCutoutNoCull(COIN));
        int tint = GlyphRender.whiten(GOLD, .15f + .25f * Math.max(0, Mth.sin(time * .2f + seed)));
        PoseStack.Pose pose = poseStack.last();
        int argb = 0xFF000000 | tint;
        RenderHelper.vertex(face, pose, -.5f, .041f, -.5f, 0, 0, argb, GlyphRender.FULL, 0, 1, 0);
        RenderHelper.vertex(face, pose, -.5f, .041f, .5f, 0, 1, argb, GlyphRender.FULL, 0, 1, 0);
        RenderHelper.vertex(face, pose, .5f, .041f, .5f, 1, 1, argb, GlyphRender.FULL, 0, 1, 0);
        RenderHelper.vertex(face, pose, .5f, .041f, -.5f, 1, 0, argb, GlyphRender.FULL, 0, 1, 0);
        RenderHelper.vertex(face, pose, .5f, -.041f, -.5f, 1, 0, argb, GlyphRender.FULL, 0, -1, 0);
        RenderHelper.vertex(face, pose, .5f, -.041f, .5f, 1, 1, argb, GlyphRender.FULL, 0, -1, 0);
        RenderHelper.vertex(face, pose, -.5f, -.041f, .5f, 0, 1, argb, GlyphRender.FULL, 0, -1, 0);
        RenderHelper.vertex(face, pose, -.5f, -.041f, -.5f, 0, 0, argb, GlyphRender.FULL, 0, -1, 0);
        poseStack.popPose();
    }

    /** The Sun Stone of Eldorado burning gold on the ground at the origin: {@code radius} wide, its rings turning. */
    public static void sigil(PoseStack poseStack, MultiBufferSource buffer, float radius, float strength, float spin) {
        if (strength <= .01f) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0, .045, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(SIGIL)), poseStack.last(), radius, RuneRender.additive(GOLD, strength), GlyphRender.FULL);
        poseStack.translate(0, .01, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-spin * 2.5f));
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), radius * 1.1f,
                RuneRender.additive(PALE, strength * .4f), GlyphRender.FULL);
        poseStack.popPose();
    }

    /** The colour of gold at {@code heat} (0 cold polished gold, 1 white-hot molten). */
    public static int heat(float heat) {
        heat = Mth.clamp(heat, 0, 1);
        return heat < .5f ? lerp(GOLD, MOLTEN, heat * 2) : lerp(MOLTEN, 0xFFE8B0, (heat - .5f) * 2);
    }

    public static int lerp(int a, int b, float t) {
        int r = (int) Mth.lerp(t, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
        int g = (int) Mth.lerp(t, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
        int bl = (int) Mth.lerp(t, a & 0xFF, b & 0xFF);
        return r << 16 | g << 8 | bl;
    }
}
