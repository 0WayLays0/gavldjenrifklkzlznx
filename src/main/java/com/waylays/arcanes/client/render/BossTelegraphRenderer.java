package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.boss.BossTelegraph;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Boss Magic warning drawn on the ground: the danger zone in its colour, laid over the terrain block by block so it
 * reads on slopes and steps; a second, brighter layer filling it up until the attack lands (the countdown); a bright
 * edge pulsing faster and faster; turning runes (or chevrons racing along a line); symbols rising from it; and, for
 * attacks from above, a great rune circle in the sky. When the attack lands the zone flashes and fades.
 */
public class BossTelegraphRenderer extends EntityRenderer<BossTelegraph> {
    static final ResourceLocation ZONE = ArcanesOublies.id("textures/entity/boss/zone.png");
    static final ResourceLocation EDGE = ArcanesOublies.id("textures/entity/boss/edge.png");
    static final ResourceLocation CHEVRONS = ArcanesOublies.id("textures/entity/boss/chevrons.png");
    static final ResourceLocation GLYPHS = ArcanesOublies.id("textures/entity/boss/glyphs.png");
    static final ResourceLocation SKY = ArcanesOublies.id("textures/entity/boss/sky_circle.png");
    static final ResourceLocation[] CIRCLES = {
            ArcanesOublies.id("textures/entity/boss/rune_circle_0.png"),
            ArcanesOublies.id("textures/entity/boss/rune_circle_1.png"),
            ArcanesOublies.id("textures/entity/boss/rune_circle_2.png"),
    };
    private static final int LIGHT = LightTexture.FULL_BRIGHT;
    private static final float EDGE_WIDTH = .28f;
    private static final int GLYPH_COUNT = 7;
    private static final int GLYPH_LIFE = 34;

    public BossTelegraphRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BossTelegraph t, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float since = t.sinceFired(partialTicks);
        float time = t.tickCount + partialTicks;
        float fade = since <= 0 ? Mth.clamp(time / 5f, 0, 1) : 1 - Mth.clamp(since / BossTelegraph.LINGER_TICKS, 0, 1);
        if (fade <= 0) {
            return;
        }
        float p = t.progress(partialTicks);
        boolean fired = since > 0;
        BossTelegraph.Style style = t.getStyle();
        Ground ground = Ground.of(t);
        Zone zone = new Zone(t, ground);
        PoseStack.Pose pose = poseStack.last();
        // The pulse quickens as the attack nears
        float pulse = .5f + .5f * Mth.sin(time * (.2f + p * p * 1.3f));

        // The whole danger zone
        float zoneAlpha = fired ? .7f * fade : (.16f + .12f * p + .07f * pulse) * fade;
        zone.surface(buffer.getBuffer(RenderType.entityTranslucentEmissive(ZONE)), pose, 0, 1, Uv.FILL, argb(style.zone, zoneAlpha), .03f, 0, 0);
        if (!fired) {
            // The countdown: a brighter layer filling the zone
            zone.surface(buffer.getBuffer(RenderType.entityTranslucentEmissive(ZONE)), pose, 0, p, Uv.FILL, argb(style.zone, .3f * fade), .045f, 0, 0);
        }
        // Runes turning in the zone, or chevrons racing along a line or a cone
        float runeLight = fired ? fade : (.45f + .35f * p + .2f * pulse) * fade;
        int runeColor = additive(fired ? lighten(style.rune, .6f) : style.rune, runeLight);
        if (t.getShape() == BossTelegraph.Shape.LINE || t.getShape() == BossTelegraph.Shape.CONE) {
            zone.surface(buffer.getBuffer(RenderType.eyes(CHEVRONS)), pose, 0, 1, Uv.CHEVRONS, runeColor, .06f, 0, time * (.06f + .25f * p));
        } else {
            ResourceLocation circle = CIRCLES[Math.floorMod(t.getId(), CIRCLES.length)];
            zone.surface(buffer.getBuffer(RenderType.eyes(circle)), pose, 0, 1, Uv.RUNES, runeColor, .06f, time * (1.2f + 6f * p * p) * Mth.DEG_TO_RAD, 0);
        }
        // The edge
        float edgeLight = fired ? fade : (.6f + .4f * pulse) * fade;
        zone.edges(buffer.getBuffer(RenderType.eyes(EDGE)), pose, additive(lighten(style.zone, .25f + .5f * p), edgeLight), .075f);

        if (!fired) {
            glyphs(t, zone, buffer, poseStack, time, style, fade);
        }
        if (t.getSkyHeight() > 0) {
            skyCircle(t, buffer, poseStack, time, p, fade, style, fired);
        }
        super.render(t, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** Runic symbols rising from the zone and fading, a few at a time. */
    private void glyphs(BossTelegraph t, Zone zone, MultiBufferSource buffer, PoseStack poseStack, float time, BossTelegraph.Style style, float fade) {
        VertexConsumer c = buffer.getBuffer(RenderType.eyes(GLYPHS));
        for (int i = 0; i < GLYPH_COUNT; i++) {
            float life = (time + i * GLYPH_LIFE / (float) GLYPH_COUNT) / GLYPH_LIFE;
            int cycle = (int) life;
            float f = life - cycle;
            RandomSource rnd = RandomSource.create(t.getId() * 7919L + i * 104729L + cycle * 31L);
            float[] at = zone.randomPoint(rnd);
            int glyph = rnd.nextInt(16);
            float y = zone.height(at[0], at[1]) + .3f + f * 2.2f;
            float alpha = Mth.sin(f * Mth.PI) * fade;
            poseStack.pushPose();
            poseStack.translate(at[0], y, at[1]);
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            poseStack.mulPose(Axis.YP.rotationDegrees(180f));
            float u0 = (glyph % 4) * .25f;
            float v0 = (glyph / 4) * .25f;
            RenderHelper.rectUv(c, poseStack.last(), .7f, .7f, u0, v0, u0 + .25f, v0 + .25f, additive(style.rune, alpha), LIGHT, false);
            poseStack.popPose();
        }
    }

    /** A great circle of runes turning in the sky over the zone, where the attack comes from. */
    private void skyCircle(BossTelegraph t, MultiBufferSource buffer, PoseStack poseStack, float time, float p, float fade, BossTelegraph.Style style, boolean fired) {
        float size = Math.max(3, t.getShape() == BossTelegraph.Shape.LINE ? t.getSize2() * 1.5f : t.getSize()) * 1.15f;
        float light = (fired ? 1 : .55f + .45f * p) * fade;
        double dx = t.center().x - t.getX();
        double dz = t.center().z - t.getZ();
        poseStack.pushPose();
        poseStack.translate(dx, t.getSkyHeight(), dz);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * (1.5f + 4 * p)));
        // Seen from below: drawn on both faces
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer c = buffer.getBuffer(RenderType.eyes(SKY));
        RenderHelper.groundQuad(c, pose, size, additive(style.rune, light), LIGHT);
        poseStack.mulPose(Axis.XP.rotationDegrees(180));
        RenderHelper.groundQuad(c, poseStack.last(), size, additive(style.rune, light), LIGHT);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(dx, t.getSkyHeight() - .3, dz);
        poseStack.mulPose(Axis.YP.rotationDegrees(-time * (2.5f + 7 * p)));
        int inner = additive(style.zone, light * .8f);
        RenderHelper.groundQuad(c, poseStack.last(), size * .55f, inner, LIGHT);
        poseStack.mulPose(Axis.XP.rotationDegrees(180));
        RenderHelper.groundQuad(c, poseStack.last(), size * .55f, inner, LIGHT);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRender(BossTelegraph entity, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        // Big flat zones: drawn as long as they are in range, even when their middle is off screen
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(BossTelegraph entity) {
        return ZONE;
    }

    // ---------------------------------------------------------------- colours

    static int argb(int rgb, float alpha) {
        return ((int) (Mth.clamp(alpha, 0, 1) * 255) << 24) | (rgb & 0xFFFFFF);
    }

    /** For additive textures: the brightness is in the colour itself. */
    static int additive(int rgb, float light) {
        light = Mth.clamp(light, 0, 1);
        int r = (int) (((rgb >> 16) & 0xFF) * light);
        int g = (int) (((rgb >> 8) & 0xFF) * light);
        int b = (int) ((rgb & 0xFF) * light);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    static int lighten(int rgb, float toWhite) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        r += (int) ((255 - r) * toWhite);
        g += (int) ((255 - g) * toWhite);
        b += (int) ((255 - b) * toWhite);
        return r << 16 | g << 8 | b;
    }

    // ---------------------------------------------------------------- the zone as a mesh over the ground

    private enum Uv { FILL, RUNES, CHEVRONS }

    /** The shape of a warning, cut into small quads whose corners sit on the ground. Coordinates relative to the entity. */
    private static final class Zone {
        private final BossTelegraph t;
        private final Ground ground;
        private final BossTelegraph.Shape shape;
        private final float size;
        private final float size2;
        private final float fx;
        private final float fz;
        private final float heading;

        Zone(BossTelegraph t, Ground ground) {
            this.t = t;
            this.ground = ground;
            this.shape = t.getShape();
            this.size = Math.max(.1f, t.getSize());
            this.size2 = t.getSize2();
            float yaw = t.getYRot() * Mth.DEG_TO_RAD;
            this.fx = -Mth.sin(yaw);
            this.fz = Mth.cos(yaw);
            this.heading = (float) Mth.atan2(fz, fx);
        }

        float height(float x, float z) {
            return ground.height(t.getX() + x, t.getZ() + z) - (float) t.getY();
        }

        /** Range of the first parameter (radius, or distance along a line), from {@code from} to {@code to} of the way. */
        private float[] aRange(float from, float to) {
            return switch (shape) {
                case RING -> new float[]{size2 + (size - size2) * from, size2 + (size - size2) * to};
                default -> new float[]{size * from, size * to};
            };
        }

        /** Range of the second parameter (angle, or distance across a line). */
        private float[] bRange() {
            return switch (shape) {
                case CIRCLE, RING -> new float[]{0, Mth.TWO_PI};
                case LINE -> new float[]{-size2 * .5f, size2 * .5f};
                case CONE -> new float[]{-size2 * Mth.DEG_TO_RAD, size2 * Mth.DEG_TO_RAD};
            };
        }

        private float px(float a, float b) {
            return switch (shape) {
                case CIRCLE, RING -> a * Mth.cos(b);
                case LINE -> fx * a + fz * b;
                case CONE -> a * Mth.cos(heading + b);
            };
        }

        private float pz(float a, float b) {
            return switch (shape) {
                case CIRCLE, RING -> a * Mth.sin(b);
                case LINE -> fz * a - fx * b;
                case CONE -> a * Mth.sin(heading + b);
            };
        }

        /** How close to the edge (0 in the heart of the zone, 1 on its edge): the fill texture is brighter there. */
        private float edgeness(float a, float b) {
            return switch (shape) {
                case CIRCLE -> a / size;
                case RING -> 1 - Math.min(a - size2, size - a) / Math.max(.01f, (size - size2) * .5f);
                case LINE -> Math.max(Math.abs(b) / Math.max(.01f, size2 * .5f), 1 - Math.min(a, size - a) / Math.max(.01f, Math.min(size, size2) * .5f));
                case CONE -> Math.max(a / size, Math.abs(b) / Math.max(.01f, size2 * Mth.DEG_TO_RAD));
            };
        }

        void surface(VertexConsumer c, PoseStack.Pose pose, float from, float to, Uv uv, int color, float lift, float rot, float scroll) {
            if (to <= from) {
                return;
            }
            float[] a = aRange(from, to);
            float[] b = bRange();
            int na = Math.max(1, Mth.ceil((a[1] - a[0]) / 1.0f));
            int nb = switch (shape) {
                case LINE -> Math.max(1, Mth.ceil(size2 / 1.0f));
                default -> Mth.clamp(Mth.ceil((b[1] - b[0]) * Math.max(a[1], 1) / 1.0f), 8, 128);
            };
            for (int i = 0; i < na; i++) {
                float a0 = Mth.lerp(i / (float) na, a[0], a[1]);
                float a1 = Mth.lerp((i + 1) / (float) na, a[0], a[1]);
                for (int j = 0; j < nb; j++) {
                    float b0 = Mth.lerp(j / (float) nb, b[0], b[1]);
                    float b1 = Mth.lerp((j + 1) / (float) nb, b[0], b[1]);
                    vertex(c, pose, a0, b0, uv, color, lift, rot, scroll);
                    vertex(c, pose, a0, b1, uv, color, lift, rot, scroll);
                    vertex(c, pose, a1, b1, uv, color, lift, rot, scroll);
                    vertex(c, pose, a1, b0, uv, color, lift, rot, scroll);
                }
            }
        }

        private void vertex(VertexConsumer c, PoseStack.Pose pose, float a, float b, Uv uv, int color, float lift, float rot, float scroll) {
            float x = px(a, b);
            float z = pz(a, b);
            float u;
            float v;
            switch (uv) {
                case FILL -> {
                    u = Mth.clamp(edgeness(a, b), 0, 1) * .97f + .015f;
                    v = .5f;
                }
                case RUNES -> {
                    float cos = Mth.cos(rot);
                    float sin = Mth.sin(rot);
                    u = .5f + (x * cos - z * sin) / (2 * size);
                    v = .5f + (x * sin + z * cos) / (2 * size);
                }
                default -> {
                    if (shape == BossTelegraph.Shape.LINE) {
                        u = .5f + b / Math.max(.01f, size2);
                        v = (a - scroll * size2) / Math.max(.01f, size2);
                    } else {
                        float half = Math.max(.01f, size2 * Mth.DEG_TO_RAD);
                        u = .5f + b / (2 * half);
                        v = (a - scroll * 4) / Math.max(1, size / 5);
                    }
                }
            }
            RenderHelper.vertex(c, pose, x, height(x, z) + lift, z, u, v, color, LIGHT, 0, 1, 0);
        }

        /** Bright bands along the zone's edges. */
        void edges(VertexConsumer c, PoseStack.Pose pose, int color, float lift) {
            float w = EDGE_WIDTH;
            switch (shape) {
                case CIRCLE -> arcBand(c, pose, size - w, size, 0, Mth.TWO_PI, color, lift);
                case RING -> {
                    arcBand(c, pose, size - w, size, 0, Mth.TWO_PI, color, lift);
                    arcBand(c, pose, size2, size2 + w, 0, Mth.TWO_PI, color, lift);
                }
                case LINE -> {
                    float h = size2 * .5f;
                    straightBand(c, pose, 0, h - w, size, h - w, 0, 1, color, lift);
                    straightBand(c, pose, 0, -h + w, size, -h + w, 0, -1, color, lift);
                    straightBand(c, pose, w, -h, w, h, -1, 0, color, lift);
                    straightBand(c, pose, size - w, -h, size - w, h, 1, 0, color, lift);
                }
                case CONE -> {
                    float half = size2 * Mth.DEG_TO_RAD;
                    arcBand(c, pose, size - w, size, -half, half, color, lift);
                    radialBand(c, pose, -half, w, color, lift);
                    radialBand(c, pose, half, -w, color, lift);
                }
            }
        }

        private void arcBand(VertexConsumer c, PoseStack.Pose pose, float r0, float r1, float b0, float b1, int color, float lift) {
            int n = Mth.clamp(Mth.ceil((b1 - b0) * r1 / .8f), 12, 160);
            for (int j = 0; j < n; j++) {
                float a0 = Mth.lerp(j / (float) n, b0, b1);
                float a1 = Mth.lerp((j + 1) / (float) n, b0, b1);
                bandVertex(c, pose, polarX(r0, a0), polarZ(r0, a0), 0, j, color, lift);
                bandVertex(c, pose, polarX(r1, a0), polarZ(r1, a0), 1, j, color, lift);
                bandVertex(c, pose, polarX(r1, a1), polarZ(r1, a1), 1, j + 1, color, lift);
                bandVertex(c, pose, polarX(r0, a1), polarZ(r0, a1), 0, j + 1, color, lift);
            }
        }

        private float polarX(float r, float b) {
            return shape == BossTelegraph.Shape.CONE ? r * Mth.cos(heading + b) : r * Mth.cos(b);
        }

        private float polarZ(float r, float b) {
            return shape == BossTelegraph.Shape.CONE ? r * Mth.sin(heading + b) : r * Mth.sin(b);
        }

        /** A band centred on the segment (along a, across b) from start to end, {@code 2 * EDGE_WIDTH} wide... clipped to the zone: it spans the segment and one width outward along (na, nb). */
        private void straightBand(VertexConsumer c, PoseStack.Pose pose, float a0, float b0, float a1, float b1, float na, float nb, int color, float lift) {
            float w = EDGE_WIDTH;
            float len = (float) Math.hypot(a1 - a0, b1 - b0);
            int n = Math.max(1, Mth.ceil(len / .8f));
            for (int j = 0; j < n; j++) {
                float s0 = j / (float) n;
                float s1 = (j + 1) / (float) n;
                float pa0 = Mth.lerp(s0, a0, a1);
                float pb0 = Mth.lerp(s0, b0, b1);
                float pa1 = Mth.lerp(s1, a0, a1);
                float pb1 = Mth.lerp(s1, b0, b1);
                bandVertex(c, pose, px(pa0, pb0), pz(pa0, pb0), 0, j, color, lift);
                bandVertex(c, pose, px(pa0 + na * w, pb0 + nb * w), pz(pa0 + na * w, pb0 + nb * w), 1, j, color, lift);
                bandVertex(c, pose, px(pa1 + na * w, pb1 + nb * w), pz(pa1 + na * w, pb1 + nb * w), 1, j + 1, color, lift);
                bandVertex(c, pose, px(pa1, pb1), pz(pa1, pb1), 0, j + 1, color, lift);
            }
        }

        /** One straight side of a cone, from the apex out, a band {@code width} wide toward the inside. */
        private void radialBand(VertexConsumer c, PoseStack.Pose pose, float angle, float width, int color, float lift) {
            float dx = Mth.cos(heading + angle);
            float dz = Mth.sin(heading + angle);
            // Inward: turned a quarter toward the cone's axis
            float ix = -dz * Math.signum(width);
            float iz = dx * Math.signum(width);
            float w = Math.abs(width);
            int n = Math.max(1, Mth.ceil(size / .8f));
            for (int j = 0; j < n; j++) {
                float r0 = size * j / n;
                float r1 = size * (j + 1) / n;
                bandVertex(c, pose, dx * r0, dz * r0, 0, j, color, lift);
                bandVertex(c, pose, dx * r0 + ix * w, dz * r0 + iz * w, 1, j, color, lift);
                bandVertex(c, pose, dx * r1 + ix * w, dz * r1 + iz * w, 1, j + 1, color, lift);
                bandVertex(c, pose, dx * r1, dz * r1, 0, j + 1, color, lift);
            }
        }

        private void bandVertex(VertexConsumer c, PoseStack.Pose pose, float x, float z, float u, float v, int color, float lift) {
            RenderHelper.vertex(c, pose, x, height(x, z) + lift, z, u, v * .5f, color, LIGHT, 0, 1, 0);
        }

        /** A random point in the zone (relative x, z). */
        float[] randomPoint(RandomSource rnd) {
            float[] a = aRange(0, 1);
            float[] b = bRange();
            float pa = shape == BossTelegraph.Shape.LINE ? Mth.lerp(rnd.nextFloat(), a[0], a[1])
                    : (float) Math.sqrt(Mth.lerp(rnd.nextFloat(), a[0] * a[0], a[1] * a[1]));
            float pb = Mth.lerp(rnd.nextFloat(), b[0], b[1]);
            return new float[]{px(pa, pb), pz(pa, pb)};
        }
    }

    // ---------------------------------------------------------------- the ground under a zone

    /** Heights of the ground under a zone, one per block column, refreshed every second (blocks get broken). */
    static final class Ground {
        private final Level level;
        private final double baseY;
        private final int builtAt;
        private final Long2FloatOpenHashMap heights = new Long2FloatOpenHashMap();
        private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        private Ground(Level level, double baseY, int builtAt) {
            this.level = level;
            this.baseY = baseY;
            this.builtAt = builtAt;
        }

        static Ground of(BossTelegraph t) {
            if (t.clientGround instanceof Ground g && t.tickCount - g.builtAt <= 20 && g.level == t.level()) {
                return g;
            }
            Ground fresh = new Ground(t.level(), t.getY(), t.tickCount);
            t.clientGround = fresh;
            return fresh;
        }

        float height(double x, double z) {
            int bx = Mth.floor(x);
            int bz = Mth.floor(z);
            long key = BlockPos.asLong(bx, 0, bz);
            if (heights.containsKey(key)) {
                return heights.get(key);
            }
            float h = scan(bx, bz);
            heights.put(key, h);
            return h;
        }

        /** The top of the first solid block (or liquid) going down from just above the zone. */
        private float scan(int x, int z) {
            int top = Mth.floor(baseY) + 2;
            int bottom = Mth.floor(baseY) - 8;
            for (int y = top; y >= bottom; y--) {
                pos.set(x, y, z);
                BlockState state = level.getBlockState(pos);
                FluidState fluid = state.getFluidState();
                if (!fluid.isEmpty()) {
                    return y + fluid.getHeight(level, pos);
                }
                VoxelShape shape = state.getCollisionShape(level, pos);
                if (!shape.isEmpty()) {
                    return y + (float) shape.max(Direction.Axis.Y);
                }
            }
            return (float) baseY;
        }
    }
}
