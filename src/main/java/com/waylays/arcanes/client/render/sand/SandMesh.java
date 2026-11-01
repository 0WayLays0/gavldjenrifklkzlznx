package com.waylays.arcanes.client.render.sand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * What Sand Magic is drawn with: sand in clouds and streaming grains, carved sandstone, glass, the linen of bandages,
 * glowing glyphs, the swirl of quicksand, the hide of a sandworm.
 */
public final class SandMesh {
    public static final ResourceLocation SANDSTONE = ArcanesOublies.id("textures/entity/sand/sandstone.png");
    public static final ResourceLocation GLASS = ArcanesOublies.id("textures/entity/sand/glass.png");
    public static final ResourceLocation LINEN = ArcanesOublies.id("textures/entity/sand/linen.png");
    public static final ResourceLocation GLYPHS = ArcanesOublies.id("textures/entity/sand/hieroglyphs.png");
    public static final ResourceLocation SWIRL = ArcanesOublies.id("textures/entity/sand/swirl.png");
    public static final ResourceLocation WORM = ArcanesOublies.id("textures/entity/sand/worm.png");
    public static final ResourceLocation STREAK = ArcanesOublies.id("textures/entity/water/streak.png");

    public static final int SAND_PALE = 0xF6E2AA;
    public static final int SAND = 0xE0B866;
    public static final int SANDSTONE_TONE = 0xCEA05C;
    public static final int UMBER = 0x784E28;
    public static final int GOLD = 0xFFCC50;
    public static final int TURQUOISE = 0x40C8C4;
    public static final int GLASS_TINT = 0xD8F6F2;

    private SandMesh() {}

    /** A cloud of sand at {@code at}, {@code size} across, turned by {@code spin}, {@code alpha} see-through. */
    public static void cloud(VertexConsumer c, PoseStack poseStack, WaterMesh.View view, Vector3f at, float size, float spin, int rgb, float alpha, int light) {
        if (alpha > .01f && size > .01f) {
            WaterMesh.puff(c, poseStack, view, at.x, at.y, at.z, size, spin, -1, BossMesh.argb(rgb, alpha), light);
        }
    }

    /** A streak of flying sand from {@code tail} to {@code head}, {@code width} wide. */
    public static void streak(VertexConsumer c, PoseStack.Pose p, WaterMesh.View view, Vector3f head, Vector3f tail, float width, int rgb, float alpha, int light) {
        if (alpha > .01f) {
            WaterMesh.streak(c, p, view.eye(), head, tail, width, BossMesh.argb(rgb, alpha), light);
        }
    }

    /**
     * A block of carved sandstone round {@code centre}, its half-extents {@code hx, hy, hz} along the axes, its texture
     * tiled a repeat every two blocks.
     */
    public static void block(VertexConsumer c, PoseStack.Pose p, Vector3f centre, float hx, float hy, float hz, int argb, int light) {
        Vector3f[] k = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            k[i] = new Vector3f(centre).add((i & 1) == 0 ? -hx : hx, (i & 2) == 0 ? -hy : hy, (i & 4) == 0 ? -hz : hz);
        }
        int[][] faces = {{0, 4, 6, 2}, {5, 1, 3, 7}, {4, 5, 7, 6}, {1, 0, 2, 3}, {2, 6, 7, 3}, {0, 1, 5, 4}};
        for (int[] q : faces) {
            Vector3f mid = new Vector3f(k[q[0]]).add(k[q[1]]).add(k[q[2]]).add(k[q[3]]).mul(.25f);
            float w = k[q[0]].distance(k[q[1]]) * .5f, h = k[q[1]].distance(k[q[2]]) * .5f;
            float[] uv = {0, h, w, h, w, 0, 0, 0};
            BossMesh.faced(c, p, k[q[0]], k[q[1]], k[q[2]], k[q[3]], uv, argb, argb, argb, argb, light, mid.sub(centre));
        }
    }

    /** Grains of sand thrown up from round {@code (0, 0, 0)}, falling back: {@code t} 0 to 1 through their flight. */
    public static void spray(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float radius, float height, float t, int count, int seed,
                             int light) {
        VertexConsumer dust = BossMesh.see(buffer, BossMesh.DUST);
        for (int k = 0; k < count; k++) {
            float a = BossMesh.hash(seed + k) * Mth.TWO_PI;
            float r = radius * (.3f + .7f * BossMesh.hash(seed + k * 3)) * (.4f + .6f * t);
            float h = height * (.5f + .5f * BossMesh.hash(seed + k * 5)) * 4 * t * (1 - t);
            cloud(dust, poseStack, view, new Vector3f(Mth.cos(a) * r, h + .2f, Mth.sin(a) * r), .8f + 1.6f * t, a + t, k % 3 == 0 ? SAND_PALE : SAND,
                    .75f * (1 - t * t), light);
        }
    }
}
