package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

/**
 * Drawing shared by the Wood Magic renderers: real wood blocks (logs, azalea leaves, flowering or not), and rounded
 * roots, branches, vines and thorns drawn with a bark texture (the same shapes as the bones of Bone Magic, in bark).
 */
public final class WoodRender {
    public static final ResourceLocation BARK = ArcanesOublies.id("textures/entity/wood/bark.png");
    public static final ResourceLocation VINE = ArcanesOublies.id("textures/entity/wood/vine.png");
    public static final ResourceLocation LEAF = ArcanesOublies.id("textures/entity/wood/leaf.png");
    /** A log's cut end: its rings, its heart, cracks, a rim of bark. */
    public static final ResourceLocation LOG_END = ArcanesOublies.id("textures/entity/wood/log_end.png");
    /** A wood shaving: a thin pale strip, its grain along it, ragged edges. */
    public static final ResourceLocation SHAVING = ArcanesOublies.id("textures/entity/wood/shaving.png");
    /** A soft streak of wind and a soft puff of dust (white, see-through: tinted and faded in game). */
    public static final ResourceLocation WIND = ArcanesOublies.id("textures/entity/wood/wind.png");
    public static final ResourceLocation DUST = ArcanesOublies.id("textures/entity/wood/dust.png");
    /** Amber: warm gold with darker streaks and little bubbles (drawn see-through). */
    public static final ResourceLocation AMBER = ArcanesOublies.id("textures/entity/wood/amber.png");
    /** Pale split wood, the grain running along it. */
    public static final ResourceLocation GRAIN = ArcanesOublies.id("textures/entity/wood/grain.png");
    public static final BlockState LOG = Blocks.OAK_LOG.defaultBlockState();
    public static final BlockState DARK_LOG = Blocks.DARK_OAK_LOG.defaultBlockState();
    public static final BlockState LEAVES = Blocks.AZALEA_LEAVES.defaultBlockState();
    public static final BlockState FLOWERS = Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState();
    /** Thorns: the bark darkened, a little red. */
    public static final int THORN = 0xFF5E3A26;

    private WoodRender() {}

    public static VertexConsumer bark(MultiBufferSource buffer) {
        return buffer.getBuffer(RenderType.entityCutoutNoCull(BARK));
    }

    public static VertexConsumer vine(MultiBufferSource buffer) {
        return buffer.getBuffer(RenderType.entityCutoutNoCull(VINE));
    }

    /** Leaves, flowering ones here and there for a seed. */
    public static BlockState leaves(int seed) {
        return RuneRender.hash(seed) < .3f ? FLOWERS : LEAVES;
    }

    /** A log standing up, {@code width} wide and {@code height} high, its bottom middle at (x, y, z). */
    public static void log(BlockState state, PoseStack poseStack, MultiBufferSource buffer, int light, float x, float y, float z, float width, float height) {
        RuneRender.block(state, poseStack, buffer, light, x, y, z, width, height, width, 0, 0);
    }

    /**
     * A block like {@link RuneRender#block}, its tinted faces (leaves, grass) coloured {@code rgb}: oak leaves can be
     * drawn green, gold or rust this way.
     */
    public static void tintedBlock(BlockState state, PoseStack poseStack, MultiBufferSource buffer, int light,
                                   float x, float y, float z, float sx, float sy, float sz, float tiltX, float tiltZ, int rgb) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        if (tiltX != 0) {
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(tiltX));
        }
        if (tiltZ != 0) {
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(tiltZ));
        }
        poseStack.scale(sx, sy, sz);
        poseStack.translate(-.5, 0, -.5);
        var blocks = net.minecraft.client.Minecraft.getInstance().getBlockRenderer();
        blocks.getModelRenderer().renderModel(poseStack.last(),
                buffer.getBuffer(RenderType.entityCutoutNoCull(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)), state, blocks.getBlockModel(state),
                ((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    /** A thorny vine along {@code points}: a green stem, thorns off it every other point. */
    public static void thornyVine(VertexConsumer vine, PoseStack.Pose p, Vector3f[] points, float r, int light, int seed) {
        BoneRender.curve(vine, p, points, r, r * .55f, 5, light, BoneRender.WHITE);
        for (int i = 1; i + 1 < points.length; i += 2) {
            Vector3f along = new Vector3f(points[i + 1]).sub(points[i - 1]);
            if (along.lengthSquared() < 1.0E-6f) {
                continue;
            }
            along.normalize();
            Vector3f side = new Vector3f(along).cross(RuneRender.hash(seed + i) > .5f ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0));
            if (side.lengthSquared() < 1.0E-6f) {
                continue;
            }
            side.normalize().mul((RuneRender.hash(seed + i * 3) > .5f ? 1 : -1) * r * 4.2f);
            thorn(vine, p, points[i], new Vector3f(points[i]).add(side).add(new Vector3f(along).mul(r * 1.8f)), r * .8f, light, THORN);
        }
    }

    /** A thorn: a slim four-sided cone from {@code base} to {@code tip} (cheap: many are drawn). */
    public static void thorn(VertexConsumer c, PoseStack.Pose p, Vector3f base, Vector3f tip, float r, int light, int argb) {
        BoneRender.tube(c, p, base, tip, r, .002f, 4, light, argb);
    }

    /**
     * A curved plate of bark round the vertical axis: the slice of a ring {@code r} out from it between the angles
     * {@code a0} and {@code a1}, from {@code y0} up to {@code y1}, {@code thick} thick, its top edge ragged.
     */
    public static void plate(VertexConsumer c, PoseStack.Pose p, float a0, float a1, float r, float y0, float y1, float thick, int light, int argb, int seed) {
        int steps = 4;
        float ri = r - thick;
        for (int i = 0; i < steps; i++) {
            float t0 = Mth.lerp((float) i / steps, a0, a1), t1 = Mth.lerp((float) (i + 1) / steps, a0, a1);
            // A ragged top: each column a little higher or lower
            float h0 = y1 - (y1 - y0) * .12f * RuneRender.hash(seed + i), h1 = y1 - (y1 - y0) * .12f * RuneRender.hash(seed + i + 1);
            float c0 = Mth.cos(t0), s0 = Mth.sin(t0), c1 = Mth.cos(t1), s1 = Mth.sin(t1);
            float u0 = (float) i / steps, u1 = (float) (i + 1) / steps;
            float v1 = (y1 - y0) / 1.5f;
            // Outside
            RenderHelper.vertex(c, p, c0 * r, y0, s0 * r, u0, v1, argb, light, c0, 0, s0);
            RenderHelper.vertex(c, p, c0 * r, h0, s0 * r, u0, 0, argb, light, c0, 0, s0);
            RenderHelper.vertex(c, p, c1 * r, h1, s1 * r, u1, 0, argb, light, c1, 0, s1);
            RenderHelper.vertex(c, p, c1 * r, y0, s1 * r, u1, v1, argb, light, c1, 0, s1);
            // Inside, darker
            int in = darker(argb);
            RenderHelper.vertex(c, p, c1 * ri, y0, s1 * ri, u1, v1, in, light, -c1, 0, -s1);
            RenderHelper.vertex(c, p, c1 * ri, h1, s1 * ri, u1, 0, in, light, -c1, 0, -s1);
            RenderHelper.vertex(c, p, c0 * ri, h0, s0 * ri, u0, 0, in, light, -c0, 0, -s0);
            RenderHelper.vertex(c, p, c0 * ri, y0, s0 * ri, u0, v1, in, light, -c0, 0, -s0);
            // Top edge
            RenderHelper.vertex(c, p, c0 * ri, h0, s0 * ri, u0, .1f, argb, light, 0, 1, 0);
            RenderHelper.vertex(c, p, c1 * ri, h1, s1 * ri, u1, .1f, argb, light, 0, 1, 0);
            RenderHelper.vertex(c, p, c1 * r, h1, s1 * r, u1, 0, argb, light, 0, 1, 0);
            RenderHelper.vertex(c, p, c0 * r, h0, s0 * r, u0, 0, argb, light, 0, 1, 0);
        }
        // The two side edges
        for (float a : new float[]{a0, a1}) {
            float ca = Mth.cos(a), sa = Mth.sin(a);
            float h = y1 - (y1 - y0) * .12f * RuneRender.hash(seed + (a == a0 ? 0 : steps));
            float n = a == a0 ? -1 : 1;
            RenderHelper.vertex(c, p, ca * ri, y0, sa * ri, 0, 1, argb, light, -sa * n, 0, ca * n);
            RenderHelper.vertex(c, p, ca * ri, h, sa * ri, 0, 0, argb, light, -sa * n, 0, ca * n);
            RenderHelper.vertex(c, p, ca * r, h, sa * r, .1f, 0, argb, light, -sa * n, 0, ca * n);
            RenderHelper.vertex(c, p, ca * r, y0, sa * r, .1f, 1, argb, light, -sa * n, 0, ca * n);
        }
    }

    /** One leaf (the leaf texture, tinted), {@code size} long, lying in the XY plane, seen from both sides. */
    public static void leaf(VertexConsumer c, PoseStack.Pose p, float size, int argb, int light) {
        RenderHelper.rect(c, p, size, size, argb, light, true);
    }

    /** Leaf colours: spring greens, now and then an azalea's pink. */
    public static int leafColour(int seed) {
        float h = RuneRender.hash(seed * 13 + 5);
        if (h < .12f) {
            return 0xFFF2A6D8;
        }
        if (h < .2f) {
            return 0xFFE4FFB0;
        }
        return h < .6f ? 0xFF7CC444 : 0xFF4E9A30;
    }

    private static int darker(int argb) {
        int r = ((argb >> 16) & 0xFF) * 6 / 10, g = ((argb >> 8) & 0xFF) * 6 / 10, b = (argb & 0xFF) * 6 / 10;
        return (argb & 0xFF000000) | r << 16 | g << 8 | b;
    }

    public static VertexConsumer ends(MultiBufferSource buffer) {
        return buffer.getBuffer(RenderType.entityCutoutNoCull(LOG_END));
    }

    public static VertexConsumer grain(MultiBufferSource buffer) {
        return buffer.getBuffer(RenderType.entityCutoutNoCull(GRAIN));
    }

    /**
     * A round face {@code r} wide round {@code centre}, in the plane of the unit vectors {@code e1} and {@code e2}, facing
     * {@code e1 x e2}; the texture laid on it as a disc (its middle at the centre).
     */
    public static void disc(VertexConsumer c, PoseStack.Pose p, Vector3f centre, Vector3f e1, Vector3f e2, float r, int sides, int light, int argb) {
        Vector3f n = new Vector3f(e1).cross(e2).normalize();
        for (int i = 0; i < sides; i++) {
            float a0 = i * Mth.TWO_PI / sides, a1 = (i + 1) * Mth.TWO_PI / sides;
            Vector3f p0 = new Vector3f(centre).add(new Vector3f(e1).mul(Mth.cos(a0) * r)).add(new Vector3f(e2).mul(Mth.sin(a0) * r));
            Vector3f p1 = new Vector3f(centre).add(new Vector3f(e1).mul(Mth.cos(a1) * r)).add(new Vector3f(e2).mul(Mth.sin(a1) * r));
            RenderHelper.vertex(c, p, centre.x, centre.y, centre.z, .5f, .5f, argb, light, n.x, n.y, n.z);
            RenderHelper.vertex(c, p, p0.x, p0.y, p0.z, .5f + .5f * Mth.cos(a0), .5f + .5f * Mth.sin(a0), argb, light, n.x, n.y, n.z);
            RenderHelper.vertex(c, p, p1.x, p1.y, p1.z, .5f + .5f * Mth.cos(a1), .5f + .5f * Mth.sin(a1), argb, light, n.x, n.y, n.z);
            RenderHelper.vertex(c, p, centre.x, centre.y, centre.z, .5f, .5f, argb, light, n.x, n.y, n.z);
        }
    }

    /** A box {@code 2hx} x {@code 2hy} x {@code 2hz} round the origin, the whole texture on each face. */
    public static void box(VertexConsumer c, PoseStack.Pose p, float hx, float hy, float hz, int light, int argb) {
        float[][] faces = {
                {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        for (float[] n : faces) {
            Vector3f nn = new Vector3f(n[0], n[1], n[2]);
            Vector3f u = Math.abs(n[1]) > .5f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
            Vector3f v = new Vector3f(nn).cross(u);
            Vector3f half = new Vector3f(hx, hy, hz);
            Vector3f centre = new Vector3f(nn).mul(half);
            Vector3f du = new Vector3f(u).mul(half), dv = new Vector3f(v).mul(half);
            Vector3f[] q = {
                    new Vector3f(centre).sub(du).sub(dv), new Vector3f(centre).add(du).sub(dv),
                    new Vector3f(centre).add(du).add(dv), new Vector3f(centre).sub(du).add(dv)};
            float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
            for (int i = 0; i < 4; i++) {
                RenderHelper.vertex(c, p, q[i].x, q[i].y, q[i].z, uv[i][0], uv[i][1], argb, light, nn.x, nn.y, nn.z);
            }
        }
    }

    /**
     * A log lying along X, {@code length} long and {@code r} thick, round the origin: its bark with a knot or two, stubs
     * of sawn-off branches, ivy wound round it with its leaves, its two cut ends showing their rings. Asks for its
     * buffers itself (bark, vine, leaves, ends, in that order).
     */
    public static void log(PoseStack poseStack, MultiBufferSource buffer, int light, float length, float r, int seed, boolean ivy) {
        PoseStack.Pose p = poseStack.last();
        float h = length * .5f;
        VertexConsumer bark = bark(buffer);
        BoneRender.tube(bark, p, new Vector3f(-h, 0, 0), new Vector3f(h, 0, 0), r, r * .94f, 12, light, BoneRender.WHITE);
        // Knots
        for (int k = 0; k < 2; k++) {
            float x = (RuneRender.hash(seed + k * 3) - .5f) * length * .7f;
            float a = RuneRender.hash(seed + k * 3 + 1) * Mth.TWO_PI;
            Vector3f at = new Vector3f(x, Mth.cos(a) * r * .92f, Mth.sin(a) * r * .92f);
            BoneRender.blob(bark, p, at, new Vector3f(r * .3f, 0, 0), new Vector3f(0, r * .22f, 0), new Vector3f(0, 0, r * .22f), 7, 4, light, 0xFFB09070);
        }
        // Stubs of branches, sawn off
        Vector3f[][] stubs = new Vector3f[2][];
        for (int k = 0; k < 2; k++) {
            float x = (k == 0 ? -.25f : .3f) * length + (RuneRender.hash(seed + k * 7) - .5f) * .3f;
            float a = RuneRender.hash(seed + k * 7 + 1) * Mth.TWO_PI;
            Vector3f out = new Vector3f(.25f, Mth.cos(a), Mth.sin(a)).normalize();
            Vector3f base = new Vector3f(x, Mth.cos(a) * r * .8f, Mth.sin(a) * r * .8f);
            Vector3f tip = new Vector3f(base).add(new Vector3f(out).mul(r * .75f));
            BoneRender.tube(bark, p, base, tip, r * .3f, r * .24f, 7, light, BoneRender.WHITE);
            stubs[k] = new Vector3f[]{tip, out};
        }
        float[][] ivyAt = null;
        if (ivy) {
            // Ivy wound round it
            VertexConsumer vine = vine(buffer);
            int n = 26;
            Vector3f[] wound = new Vector3f[n];
            ivyAt = new float[n][];
            for (int i = 0; i < n; i++) {
                float u = (float) i / (n - 1);
                float a = u * Mth.TWO_PI * 2.3f + seed;
                float rr = r * 1.04f;
                wound[i] = new Vector3f(-h * .85f + u * length * .85f, Mth.cos(a) * rr, Mth.sin(a) * rr);
                ivyAt[i] = new float[]{wound[i].x, wound[i].y, wound[i].z, a};
            }
            BoneRender.curve(vine, p, wound, .035f, .03f, 4, light, BoneRender.WHITE);
            VertexConsumer leaf = leafBuffer(buffer);
            for (int i = 1; i < n; i += 2) {
                float[] w = ivyAt[i];
                poseStack.pushPose();
                poseStack.translate(w[0], w[1], w[2]);
                poseStack.mulPose(com.mojang.math.Axis.XP.rotation(w[3] - Mth.HALF_PI));
                poseStack.mulPose(com.mojang.math.Axis.YP.rotation(RuneRender.hash(seed + i) * 2));
                poseStack.translate(0, .06f, 0);
                poseStack.mulPose(com.mojang.math.Axis.XP.rotation(-1.2f));
                leaf(leaf, poseStack.last(), .2f + .08f * RuneRender.hash(seed + i * 5), leafColour(seed + i * 3), light);
                poseStack.popPose();
            }
        }
        // The cut ends and the ends of the stubs
        VertexConsumer end = ends(buffer);
        p = poseStack.last();
        disc(end, p, new Vector3f(h, 0, 0), new Vector3f(0, 1, 0), new Vector3f(0, 0, 1), r * .94f, 14, light, BoneRender.WHITE);
        disc(end, p, new Vector3f(-h, 0, 0), new Vector3f(0, 0, 1), new Vector3f(0, 1, 0), r, 14, light, BoneRender.WHITE);
        for (Vector3f[] stub : stubs) {
            Vector3f out = stub[1];
            Vector3f e1 = new Vector3f(out).cross(new Vector3f(1, 0, 0));
            if (e1.lengthSquared() < 1.0E-4f) {
                e1.set(0, 1, 0);
            }
            e1.normalize();
            Vector3f e2 = new Vector3f(out).cross(e1).normalize();
            disc(end, p, stub[0], e1, e2, r * .24f, 8, light, BoneRender.WHITE);
        }
    }

    public static VertexConsumer leafBuffer(MultiBufferSource buffer) {
        return buffer.getBuffer(RenderType.entityCutoutNoCull(LEAF));
    }
}
