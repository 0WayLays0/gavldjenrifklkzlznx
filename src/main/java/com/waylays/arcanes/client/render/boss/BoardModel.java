package com.waylays.arcanes.client.render.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.fire.FirePillarModel;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Infernal Checkerboard: a board of five by five great squares rises out of the ground, slabs of obsidian and of bone-white
 * stone, each carved with a pentacle, framed in gold, a black rook of obsidian and gold standing guard at each corner.
 * The pentacles of one colour kindle, then those squares burst in towers of crimson hellfire; then the others' in towers
 * of violet soul-fire; then the board sinks back into the ground.
 */
public final class BoardModel {
    private static final ResourceLocation DARK = ArcanesOublies.id("textures/entity/boss/m_tile_dark.png");
    private static final ResourceLocation LIGHT = ArcanesOublies.id("textures/entity/boss/m_tile_light.png");
    private static final ResourceLocation SIGIL = ArcanesOublies.id("textures/entity/boss/m_tile_glow.png");
    private static final ResourceLocation OBSIDIAN = ArcanesOublies.id("textures/entity/boss/m_obsidian.png");
    /** The fire's heat for each colour: crimson, then violet. */
    private static final int[] FIRE = {0, 5};
    private static final float TILE = .22f;

    private BoardModel() {}

    /**
     * Round the board's middle (the pose's origin), turned {@code yaw} (degrees); {@code grid} squares of {@code cell}
     * a side; the first colour bursts {@code hit} ticks ago (negative before), the second {@code wave} ticks after it.
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float hit, float yaw, float cell, int grid, int wave,
                            int[] zones, int seed, int light) {
        float end = wave + 26;
        float sink = BossMesh.ramp(hit, end, end + 14);
        if (sink >= 1) {
            return;
        }
        int half = grid / 2;
        float yawRad = -yaw * Mth.DEG_TO_RAD;
        Quaternionf turn = new Quaternionf().rotationY(yawRad);
        float size = cell * grid;

        // ---- the squares, rising in a wave from the middle
        for (int pass = 0; pass < 2; pass++) {
            VertexConsumer c = BossMesh.solid(buffer, pass == 0 ? DARK : LIGHT);
            for (int gx = -half; gx <= half; gx++) {
                for (int gz = -half; gz <= half; gz++) {
                    int colour = Math.floorMod(gx + gz, 2);
                    if (colour != pass) {
                        continue;
                    }
                    float rise = BossMesh.ramp(age, Math.abs(gx) * 2 + Math.abs(gz) * 2, Math.abs(gx) * 2 + Math.abs(gz) * 2 + 8) * (1 - sink);
                    // A square shakes as it bursts, cracked and sunk a little after
                    float burst = hit - colour * wave;
                    float shake = burst >= -6 && burst < 0 ? Mth.sin(age * 3 + gx * 5 + gz * 7) * .04f : 0;
                    float drop = burst > 0 ? .08f * BossMesh.ramp(burst, 0, 4) : 0;
                    poseStack.pushPose();
                    poseStack.mulPose(turn);
                    poseStack.translate(gx * cell + shake, (rise - 1) * .6f - drop, gz * cell);
                    tile(c, poseStack, cell * .95f, light);
                    poseStack.popPose();
                }
            }
        }
        // ---- the frame of gold and the rooks
        VertexConsumer gold = BossMesh.solid(buffer, BellModel.GOLD);
        float frame = BossMesh.ramp(age, 6, 16) * (1 - sink);
        poseStack.pushPose();
        poseStack.mulPose(turn);
        poseStack.translate(0, (frame - 1) * .8f, 0);
        for (int s = 0; s < 4; s++) {
            poseStack.pushPose();
            poseStack.mulPose(new Quaternionf().rotationY(s * Mth.HALF_PI));
            poseStack.translate(0, .2f, size * .5f + .25f);
            Sculpt.block(gold, poseStack.last(), size + .5f, .42f, .5f, .08f, .5f, 0xFFFFFFFF, light);
            poseStack.popPose();
        }
        poseStack.popPose();
        float rooks = BossMesh.ramp(age, 8, 22) * (1 - sink);
        VertexConsumer stone = BossMesh.solid(buffer, OBSIDIAN);
        for (int s = 0; s < 4; s++) {
            rook(stone, poseStack, turn, s, size, rooks, light);
        }
        VertexConsumer trim = BossMesh.solid(buffer, BellModel.GOLD);
        for (int s = 0; s < 4; s++) {
            rookTrim(trim, poseStack, turn, s, size, rooks, light);
        }

        // ---- light: the pentacles kindling, the hellfire, the rooks' windows
        VertexConsumer sigil = BossMesh.light(buffer, SIGIL);
        for (int gx = -half; gx <= half; gx++) {
            for (int gz = -half; gz <= half; gz++) {
                int colour = Math.floorMod(gx + gz, 2);
                float burst = hit - colour * wave;
                float warm = BossMesh.ramp(burst, -30, 0) * (1 - BossMesh.ramp(burst, 14, 24)) * (1 - sink);
                float pulse = .6f + .4f * Mth.sin(age * (.3f + .5f * warm) + gx + gz);
                float rise = BossMesh.ramp(age, Math.abs(gx) * 2 + Math.abs(gz) * 2, Math.abs(gx) * 2 + Math.abs(gz) * 2 + 8);
                poseStack.pushPose();
                poseStack.mulPose(turn);
                poseStack.translate(gx * cell, (rise - 1) * .6f + TILE + .02f, gz * cell);
                float h = cell * .46f;
                Sculpt.quad(sigil, poseStack.last(), new Vector3f(-h, 0, h), new Vector3f(h, 0, h), new Vector3f(h, 0, -h), new Vector3f(-h, 0, -h),
                        BossMesh.glow(zones[colour], (.15f + .85f * warm) * pulse * rise), light);
                poseStack.popPose();
            }
        }
        for (int gx = -half; gx <= half; gx++) {
            for (int gz = -half; gz <= half; gz++) {
                int colour = Math.floorMod(gx + gz, 2);
                float burst = hit - colour * wave;
                if (burst < -14 || burst > 24) {
                    continue;
                }
                Vector3f at = turn.transform(new Vector3f(gx * cell, TILE, gz * cell));
                poseStack.pushPose();
                poseStack.translate(at.x, at.y, at.z);
                WaterMesh.View local = new WaterMesh.View(view.camera(), new Vector3f(view.eye()).sub(at));
                float warn = BossMesh.ramp(burst, -14, 0);
                float rise = BossMesh.ramp(burst, 0, 3);
                float fall = BossMesh.ramp(burst, 12, 24);
                FirePillarModel.draw(poseStack, buffer, local, age + gx * 3 + gz * 7, warn, rise, fall, 6.5f + BossMesh.hash(seed + gx * 5 + gz) * 2.5f, cell * .42f,
                        FIRE[colour], seed + gx * 31 + gz * 17, light);
                poseStack.popPose();
            }
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        for (int s = 0; s < 4; s++) {
            Vector3f top = turn.transform(new Vector3f((s % 2 == 0 ? -1 : 1) * (size * .5f + .9f), 4.2f * rooks, (s < 2 ? -1 : 1) * (size * .5f + .9f)));
            RitualMesh.billboard(glow, poseStack, view, top, 3.5f, 0, -1, BossMesh.glow(zones[s % 2], .55f * rooks), light);
        }
    }

    /** One square, lying flat round the origin, {@code w} a side: a thick slab, its face the carved square. */
    private static void tile(VertexConsumer c, PoseStack poseStack, float w, int light) {
        poseStack.pushPose();
        poseStack.translate(0, TILE * .5f, 0);
        poseStack.mulPose(new Quaternionf().rotationX(-Mth.HALF_PI));
        Sculpt.block(c, poseStack.last(), w, w, TILE, .06f, .08f, 0xFF8A8090, light);
        poseStack.popPose();
        float h = w * .5f - .06f;
        Sculpt.quad(c, poseStack.last(), new Vector3f(-h, TILE + .005f, h), new Vector3f(h, TILE + .005f, h), new Vector3f(h, TILE + .005f, -h),
                new Vector3f(-h, TILE + .005f, -h), 0xFFFFFFFF, light);
    }

    private static final float[] ROOK = {1.05f, 0, 1.1f, .15f, .95f, .3f, .9f, .42f, .72f, .6f, .62f, .9f, .58f, 2.4f, .62f, 2.6f, .8f, 2.85f, .95f, 3.0f, .95f, 3.55f,
            .7f, 3.55f, .7f, 3.4f, 0, 3.4f};

    private static Vector3f corner(Quaternionf turn, int s, float size, float rooks) {
        return turn.transform(new Vector3f((s % 2 == 0 ? -1 : 1) * (size * .5f + .9f), (rooks - 1) * 4.5f, (s < 2 ? -1 : 1) * (size * .5f + .9f)));
    }

    /** A rook of obsidian at corner {@code s}: a tower with a crown of battlements. */
    private static void rook(VertexConsumer c, PoseStack poseStack, Quaternionf turn, int s, float size, float rooks, int light) {
        if (rooks <= 0) {
            return;
        }
        Vector3f at = corner(turn, s, size, rooks);
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        poseStack.scale(1.2f, 1.2f, 1.2f);
        Sculpt.lathe(c, poseStack.last(), ROOK, 24, 2, .4f, 0xFFB0A8C0, 0xFF8A8098, light);
        for (int k = 0; k < 6; k++) {
            float a = k * Mth.TWO_PI / 6;
            poseStack.pushPose();
            poseStack.translate(Mth.cos(a) * .82f, 3.75f, Mth.sin(a) * .82f);
            poseStack.mulPose(new Quaternionf().rotationY(-a));
            Sculpt.block(c, poseStack.last(), .3f, .42f, .5f, .04f, .5f, 0xFFB0A8C0, light);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void rookTrim(VertexConsumer c, PoseStack poseStack, Quaternionf turn, int s, float size, float rooks, int light) {
        if (rooks <= 0) {
            return;
        }
        Vector3f at = corner(turn, s, size, rooks);
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        poseStack.scale(1.2f, 1.2f, 1.2f);
        for (float[] ring : new float[][] {{1.08f, .16f, .06f}, {.62f, .92f, .05f}, {.62f, 2.38f, .05f}, {.96f, 3.02f, .05f}}) {
            poseStack.pushPose();
            poseStack.translate(0, ring[1], 0);
            Sculpt.torus(c, poseStack.last(), ring[0], ring[2], 28, 6, 0, Mth.TWO_PI, 6, 0xFFFFFFFF, light);
            poseStack.popPose();
        }
        poseStack.translate(0, 1.7f, .6f);
        poseStack.mulPose(new Quaternionf().rotationX(Mth.HALF_PI));
        Sculpt.gem(c, poseStack.last(), .14f, .06f, .05f, 6, 0xFFFF5060, light);
        poseStack.popPose();
    }
}
