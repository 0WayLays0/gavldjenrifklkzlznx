package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

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

    /** A six-sided crystal of gold one block high, .56 across: a prism tapering to a faceted point. */
    public static final float[] CRYSTAL = new Solid.Builder()
            .band(Solid.ring(6, 0, .28f, .28f, 0), Solid.ring(6, .6f, .23f, .23f, 0))
            .band(Solid.ring(6, .6f, .23f, .23f, 0), Solid.ring(6, .8f, .12f, .12f, .5f))
            .fan(Solid.ring(6, .8f, .12f, .12f, .5f), new float[]{0, 1, 0})
            .fan(new float[][]{{.28f, 0, 0}, {-.14f, 0, -.2425f}, {-.14f, 0, .2425f}}, new float[]{0, -.05f, 0})
            .build();

    /** A thin five-sided needle of gold one block high, .4 across. */
    public static final float[] NEEDLE = new Solid.Builder()
            .band(Solid.ring(5, 0, .2f, .2f, 0), Solid.ring(5, .3f, .17f, .17f, 0))
            .fan(Solid.ring(5, .3f, .17f, .17f, 0), new float[]{0, 1, 0})
            .build();

    /** A squat block of gold, as pyrite grows: a square prism one block high under a low four-sided point. */
    public static final float[] NUGGET = new Solid.Builder()
            .band(Solid.ring(4, 0, .4f, .4f, .5f), Solid.ring(4, .62f, .38f, .38f, .5f))
            .band(Solid.ring(4, .62f, .38f, .38f, .5f), Solid.ring(4, .8f, .22f, .22f, 0))
            .fan(Solid.ring(4, .8f, .22f, .22f, 0), new float[]{0, 1, 0})
            .build();

    /** The shapes gold crystals grow in, for clusters that never look alike. */
    public static final float[][] CRYSTALS = {CRYSTAL, SPIKE, NEEDLE, CRYSTAL, NUGGET, NEEDLE};

    /** A chain link (an oval loop of square gold wire) .4 long along Y, .24 wide along X, centred. */
    public static final float[] LINK = link(12, .12f, .2f, .035f);
    /** A padlock's body, hung under its shackle (a {@link #LINK}), and its keyhole. */
    public static final float[] PADLOCK = new Solid.Builder()
            .box(-.13f, -.34f, -.055f, .13f, -.06f, .055f)
            .band(new float[][]{{-.13f, -.06f, -.055f}, {.13f, -.06f, -.055f}, {.13f, -.06f, .055f}, {-.13f, -.06f, .055f}},
                    new float[][]{{-.11f, -.03f, -.045f}, {.11f, -.03f, -.045f}, {.11f, -.03f, .045f}, {-.11f, -.03f, .045f}})
            .quad(new float[]{-.11f, -.03f, .045f}, new float[]{.11f, -.03f, .045f}, new float[]{.11f, -.03f, -.045f}, new float[]{-.11f, -.03f, -.045f})
            .build();
    public static final float[] KEYHOLE = new Solid.Builder()
            .box(-.022f, -.19f, .05f, .022f, -.14f, .062f)
            .box(-.009f, -.26f, .05f, .009f, -.19f, .062f)
            .build();

    /** Spacing of the links of a chain: each one passes through the next. */
    public static final float LINK_PITCH = .27f;

    private GoldRender() {}

    private static float[] link(int segments, float rx, float ry, float wire) {
        float[][][] rings = new float[segments][][];
        for (int i = 0; i < segments; i++) {
            float t = i * Mth.TWO_PI / segments;
            float px = Mth.cos(t) * rx, py = Mth.sin(t) * ry;
            float gx = px / (rx * rx), gy = py / (ry * ry), gl = Mth.sqrt(gx * gx + gy * gy);
            gx /= gl;
            gy /= gl;
            rings[i] = new float[4][];
            for (int j = 0; j < 4; j++) {
                float a = j * Mth.HALF_PI + Mth.HALF_PI * .5f;
                float out = Mth.cos(a) * wire, side = Mth.sin(a) * wire;
                rings[i][j] = new float[]{px + gx * out, py + gy * out, side};
            }
        }
        Solid.Builder builder = new Solid.Builder();
        for (int i = 0; i < segments; i++) {
            builder.band(rings[i], rings[(i + 1) % segments]);
        }
        return builder.build();
    }

    /** One chain link centred at {@code at}, its length along {@code along}, turned {@code roll} degrees round it. */
    public static void link(VertexConsumer metal, PoseStack poseStack, Vec3 at, Vec3 along, float roll, float size, int rgb, float time, int light) {
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        Vec3 dir = along.normalize();
        poseStack.mulPose(new Quaternionf().rotationTo(0, 1, 0, (float) dir.x, (float) dir.y, (float) dir.z));
        poseStack.mulPose(Axis.YP.rotationDegrees(roll));
        poseStack.scale(size, size, size);
        Solid.draw(metal, poseStack.last(), LINK, rgb, 255, time, .5f, light);
        poseStack.popPose();
    }

    /**
     * A taut chain coming out of {@code from} toward {@code to}: {@code shown} blocks of it are out, its leading link
     * first, each link turned a quarter from the last.
     */
    public static void chain(VertexConsumer metal, PoseStack poseStack, Vec3 from, Vec3 to, float shown, float size, int rgb, float time, int light) {
        Vec3 span = to.subtract(from);
        double length = span.length();
        if (length < 1.0E-4) {
            return;
        }
        Vec3 dir = span.scale(1 / length);
        float pitch = LINK_PITCH * size;
        int count = (int) Math.ceil(Math.min(shown, length) / pitch) + 1;
        double lead = Math.min(shown, length) - pitch * .5;
        for (int k = 0; k < count; k++) {
            double d = lead - k * pitch;
            if (d < -pitch) {
                break;
            }
            link(metal, poseStack, from.add(dir.scale(d)), dir, k % 2 * 90 + 45, size, rgb, time + k * 7, light);
        }
    }

    /** A ring of links lying flat round the origin at {@code height}, {@code radius} across. */
    public static void loop(VertexConsumer metal, PoseStack poseStack, float radius, float height, float size, int rgb, float time, int light) {
        int count = Math.max(6, Math.round(Mth.TWO_PI * radius / (LINK_PITCH * size)));
        for (int k = 0; k < count; k++) {
            float a = k * Mth.TWO_PI / count;
            Vec3 at = new Vec3(Mth.cos(a) * radius, height, Mth.sin(a) * radius);
            link(metal, poseStack, at, new Vec3(-Mth.sin(a), 0, Mth.cos(a)), k % 2 * 90, size, rgb, time + k * 7, light);
        }
    }

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
