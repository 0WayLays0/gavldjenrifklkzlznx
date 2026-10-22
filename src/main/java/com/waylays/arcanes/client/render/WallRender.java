package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.spells.wall.WallFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Drawing shared by the Wall Magic renderers: bricks (the three stone brick blocks), their mortar glowing like embers,
 * the mason's sigil burning on the ground, and bricks flying apart.
 */
public final class WallRender {
    public static final ResourceLocation SIGIL = ArcanesOublies.id("textures/entity/wall/sigil.png");

    private WallRender() {}

    /** A brick block {@code size} wide, its bottom middle at (x, y, z). */
    public static void brick(BlockState state, PoseStack poseStack, MultiBufferSource buffer, int light, float x, float y, float z, float size) {
        RuneRender.block(state, poseStack, buffer, light, x, y, z, size, size, size, 0, 0);
    }

    /** The colour of the mortar at {@code heat} (0 to 1): from dull ember to a hot orange. */
    public static int ember(float heat, float strength) {
        int c = heat > .5f ? lerp(WallFx.EMBER, WallFx.EMBER_CORE, (heat - .5f) * 2) : WallFx.EMBER;
        return RuneRender.additive(c, strength);
    }

    private static int lerp(int a, int b, float t) {
        int r = (int) Mth.lerp(t, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
        int g = (int) Mth.lerp(t, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
        int bl = (int) Mth.lerp(t, a & 0xFF, b & 0xFF);
        return r << 16 | g << 8 | bl;
    }

    /**
     * The glowing mortar of a block one wide whose bottom middle is at (x, y, z): the outline of its two faces across
     * {@code face} (0 or 2: the faces looking along Z; 1 or 3: along X; -1: its top), a hair outside the block so it
     * shows on a real block too.
     */
    public static void mortar(VertexConsumer stroke, PoseStack.Pose pose, float x, float y, float z, int face, float width, int argb) {
        float h = .501f;
        if (face < 0) {
            square(stroke, pose, new Vec3(x - h, y + 1.002, z - h), new Vec3(2 * h, 0, 0), new Vec3(0, 0, 2 * h), width, argb);
            return;
        }
        boolean alongZ = face % 2 == 0;
        for (int side = -1; side <= 1; side += 2) {
            if (alongZ) {
                square(stroke, pose, new Vec3(x - h, y - .001, z + side * h), new Vec3(2 * h, 0, 0), new Vec3(0, 1.002, 0), width, argb);
            } else {
                square(stroke, pose, new Vec3(x + side * h, y - .001, z - h), new Vec3(0, 0, 2 * h), new Vec3(0, 1.002, 0), width, argb);
            }
        }
    }

    private static void square(VertexConsumer stroke, PoseStack.Pose pose, Vec3 o, Vec3 u, Vec3 v, float width, int argb) {
        Vec3 a = o, b = o.add(u), c = o.add(u).add(v), d = o.add(v);
        GlyphRender.beam(stroke, pose, a, b, width, argb);
        GlyphRender.beam(stroke, pose, b, c, width, argb);
        GlyphRender.beam(stroke, pose, c, d, width, argb);
        GlyphRender.beam(stroke, pose, d, a, width, argb);
    }

    /** The mason's sigil burning on the ground at the origin: {@code radius} wide, turning, with a dimmer twin under it. */
    public static void sigil(PoseStack poseStack, MultiBufferSource buffer, float radius, float strength, float spin) {
        if (strength <= .01f) {
            return;
        }
        VertexConsumer sigil = buffer.getBuffer(RenderType.eyes(SIGIL));
        poseStack.pushPose();
        poseStack.translate(0, .04, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        RenderHelper.groundQuad(sigil, poseStack.last(), radius, RuneRender.additive(WallFx.EMBER, strength), GlyphRender.FULL);
        poseStack.translate(0, .01, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-spin * 2.3f));
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), radius * 1.15f,
                RuneRender.additive(WallFx.EMBER_CORE, strength * .35f), GlyphRender.FULL);
        poseStack.popPose();
    }

    /**
     * A brick flying apart from (x, y, z), {@code t} ticks after: thrown with velocity (vx, vy, vz) per tick, falling,
     * tumbling, shrinking away at the end of {@code life}.
     */
    public static void fragment(BlockState state, PoseStack poseStack, MultiBufferSource buffer, int light, float x, float y, float z,
                                float vx, float vy, float vz, float t, int life, float size, int seed) {
        if (t < 0 || t > life) {
            return;
        }
        float px = x + vx * t;
        float py = y + vy * t - .045f * t * t;
        float pz = z + vz * t;
        if (py < -1) {
            return;
        }
        float shrink = 1 - Mth.clamp((t - life * .6f) / (life * .4f), 0, 1);
        poseStack.pushPose();
        poseStack.translate(px, py, pz);
        poseStack.mulPose(Axis.XP.rotationDegrees(t * (14 + RuneRender.hash(seed) * 20)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(t * (10 + RuneRender.hash(seed + 1) * 24)));
        float s = size * shrink;
        RuneRender.block(state, poseStack, buffer, light, 0, -s * .5f, 0, s, s, s, 0, 0);
        poseStack.popPose();
    }
}
