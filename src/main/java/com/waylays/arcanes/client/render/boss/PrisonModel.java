package com.waylays.arcanes.client.render.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Rune Prison: standing stones of obsidian rise round the prison's ring, each tapering to a capstone of gold, runes
 * burning down its inner face; the wall of runes stretches between them (drawn by the wall's renderer).
 */
public final class PrisonModel {
    private static final ResourceLocation OBSIDIAN = ArcanesOublies.id("textures/entity/boss/m_obsidian.png");
    private static final ResourceLocation ENGRAVING = ArcanesOublies.id("textures/entity/boss/m_engraving.png");
    private static final int VIOLET = 0x9A3CFF;

    private PrisonModel() {}

    /** Round the prison's middle (the pose's origin): its stones on a ring {@code radius} across, {@code height} tall, risen {@code standing} (0 to 1). */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float radius, float height, float standing, float age, int light) {
        if (standing <= 0) {
            return;
        }
        int n = Math.max(6, Math.round(Mth.TWO_PI * radius / 4));
        float tall = height * 1.25f;
        float sunk = (1 - standing) * tall;
        // A stone's outline (seen face on), narrowing upward
        float[] stone = {-.55f, 0, .55f, 0, .42f, tall, -.42f, tall};
        VertexConsumer rock = BossMesh.solid(buffer, OBSIDIAN);
        for (int k = 0; k < n; k++) {
            place(poseStack, k, n, radius, sunk);
            Sculpt.slab(rock, poseStack.last(), stone, .7f, .08f, .5f, 0xFFB0A0C8, 0xFF8A7AA0, light);
            poseStack.popPose();
        }
        VertexConsumer gold = BossMesh.solid(buffer, BellModel.GOLD);
        for (int k = 0; k < n; k++) {
            place(poseStack, k, n, radius, sunk);
            poseStack.translate(0, tall, 0);
            poseStack.mulPose(new Quaternionf().rotationY(Mth.PI / 4));
            Sculpt.gem(gold, poseStack.last(), .62f, .55f, .02f, 4, 0xFFFFFFFF, light);
            poseStack.translate(0, -tall * .35f, 0);
            Sculpt.torus(gold, poseStack.last(), .55f, .05f, 4, 4, 0, Mth.TWO_PI, 2, 0xFFE0C080, light);
            poseStack.popPose();
        }
        // Runes burning down each stone's inner face
        VertexConsumer runes = BossMesh.light(buffer, ENGRAVING);
        int g = BossMesh.glow(VIOLET, (.6f + .4f * Mth.sin(age * .2f)) * standing);
        for (int k = 0; k < n; k++) {
            place(poseStack, k, n, radius, sunk);
            float z = .36f;
            Sculpt.quad(runes, poseStack.last(), new Vector3f(-.4f, tall * .12f, z), new Vector3f(.4f, tall * .12f, z), new Vector3f(.33f, tall * .82f, z),
                    new Vector3f(-.33f, tall * .82f, z), g, light);
            poseStack.popPose();
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        for (int k = 0; k < n; k++) {
            float a = k * Mth.TWO_PI / n;
            RitualMesh.billboard(glow, poseStack, view, new Vector3f(Mth.cos(a) * radius, tall - sunk + .4f, Mth.sin(a) * radius), 1.6f, 0, -1,
                    BossMesh.glow(0xFFD070, .5f * standing), light);
        }
    }

    /** Into the space of stone {@code k}: standing on the ring, its face turned to the middle. */
    private static void place(PoseStack poseStack, int k, int n, float radius, float sunk) {
        float a = k * Mth.TWO_PI / n;
        poseStack.pushPose();
        poseStack.translate(Mth.cos(a) * radius, -sunk, Mth.sin(a) * radius);
        // Its +Z (its face) toward the middle
        poseStack.mulPose(new Quaternionf().rotationY(-a - Mth.HALF_PI));
    }
}
