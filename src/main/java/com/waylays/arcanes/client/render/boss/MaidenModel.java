package com.waylays.arcanes.client.render.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Iron Maidens: round each foe a tall casket of black iron rises out of the ground, banded and riveted with gold, a
 * stern iron face crowned with spikes on its head. Its two doors stand open, rows of spikes inside them and a red light
 * burning in its hollow; they creak while the warning runs, then slam shut on whoever is still inside. Red light leaks
 * from its seams; then it sinks back into the ground.
 */
public final class MaidenModel {
    private static final int ROWS = 12, COLS = 6;
    /** The casket's height (before scaling), and where its doors end and its head begins. */
    private static final float HEIGHT = 3.7f, SHOULDER = 2.75f;

    private MaidenModel() {}

    /** Half the casket's width at height {@code y}. */
    private static float width(float y) {
        float[] ys = {0, .5f, 1.2f, 1.7f, 2.2f, 2.6f, 2.75f, 2.9f, 3.2f, 3.5f, 3.7f};
        float[] ws = {.55f, .6f, .66f, .62f, .74f, .86f, .84f, .45f, .47f, .36f, 0};
        for (int i = 0; i + 1 < ys.length; i++) {
            if (y <= ys[i + 1]) {
                return Mth.lerp((y - ys[i]) / (ys[i + 1] - ys[i]), ws[i], ws[i + 1]);
            }
        }
        return 0;
    }

    /** A point on the casket's skin at height {@code y}, round it at angle {@code a} (0 its right side, half a turn its left, the front between). */
    private static Vector3f skin(float a, float y, float grow) {
        float w = width(y) * grow;
        return new Vector3f(Mth.cos(a) * w, y, Mth.sin(a) * w * .72f);
    }

    /** Round the foe's spot (the pose's origin), facing {@code yaw} (degrees); it slams shut {@code hit} ticks ago (negative before); {@code radius} its circle. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float hit, float yaw, float radius, int zone, int seed,
                            int light) {
        float rise = BossMesh.ramp(age, 0, 14);
        float sink = BossMesh.ramp(hit, 16, 30);
        if (sink >= 1 || rise <= 0) {
            return;
        }
        float scale = radius / 2 * 1.25f;
        // Open wide, creaking; slammed shut in three ticks
        float open = hit < 0 ? 1.95f + .08f * Mth.sin(age * .5f) * BossMesh.ramp(hit, -40, -5) : 1.95f * (1 - BossMesh.ramp(hit, 0, 3));
        float jolt = hit >= 0 && hit < 6 ? Mth.sin(hit * 3) * .03f * (1 - hit / 6) : 0;
        float y0 = (rise - 1) * HEIGHT * scale - sink * HEIGHT * scale;
        Quaternionf turn = new Quaternionf().rotationY(-yaw * Mth.DEG_TO_RAD);

        VertexConsumer iron = BossMesh.solid(buffer, BellModel.IRON);
        place(poseStack, turn, y0, scale, jolt);
        // The back half, the head, the base
        shell(iron, poseStack.last(), Mth.PI, Mth.TWO_PI, 0, HEIGHT, light);
        for (int side = -1; side <= 1; side += 2) {
            door(poseStack, side, open);
            shell(iron, poseStack.last(), side > 0 ? 0 : Mth.HALF_PI, side > 0 ? Mth.HALF_PI : Mth.PI, 0, SHOULDER, light);
            poseStack.popPose();
        }
        // The head's front, fixed over the doors: a stern face
        shell(iron, poseStack.last(), 0, Mth.PI, SHOULDER, HEIGHT, light);
        face(iron, poseStack, light);
        poseStack.pushPose();
        poseStack.translate(0, -.1f, 0);
        Sculpt.lathe(iron, poseStack.last(), new float[] {1.1f, 0, 1.15f, .1f, 1.05f, .2f, .8f, .26f, 0, .27f}, 24, 2, .5f, 0xFF8A8098, 0xFFB0A8C0, light);
        poseStack.popPose();
        poseStack.popPose();

        // ---- gold: bands, rivets, hinges, the spikes inside, the crown
        VertexConsumer gold = BossMesh.solid(buffer, BellModel.GOLD);
        place(poseStack, turn, y0, scale, jolt);
        for (float y : new float[] {.35f, 1.25f, 2.2f}) {
            band(gold, poseStack.last(), Mth.PI, Mth.TWO_PI, y, light);
            for (int side = -1; side <= 1; side += 2) {
                door(poseStack, side, open);
                band(gold, poseStack.last(), side > 0 ? 0 : Mth.HALF_PI, side > 0 ? Mth.HALF_PI : Mth.PI, y, light);
                poseStack.popPose();
            }
        }
        for (int side = -1; side <= 1; side += 2) {
            // The hinges
            for (float y : new float[] {.6f, 1.6f, 2.45f}) {
                Vector3f h = skin(side > 0 ? 0 : Mth.PI, y, 1.04f);
                poseStack.pushPose();
                poseStack.translate(h.x, y - .15f, h.z);
                Sculpt.lathe(gold, poseStack.last(), new float[] {.07f, 0, .08f, .02f, .08f, .28f, .07f, .3f, 0, .31f}, 8, 1, 1, 0xFFFFFFFF, 0xFFFFFFFF, light);
                poseStack.popPose();
            }
            // The spikes on the inside of the door, pointing in
            door(poseStack, side, open);
            for (int r = 0; r < 6; r++) {
                for (int k = 0; k < 3; k++) {
                    float a = side > 0 ? Mth.lerp((k + .5f) / 3, .1f, Mth.HALF_PI - .1f) : Mth.lerp((k + .5f) / 3, Mth.HALF_PI + .1f, Mth.PI - .1f);
                    float y = .3f + r * .42f;
                    Vector3f at = skin(a, y, .97f);
                    Vector3f in = new Vector3f(-Mth.cos(a), 0, -Mth.sin(a)).normalize();
                    BossMesh.horn(gold, poseStack.last(), at, in, new Vector3f(0, -.05f, 0), .38f, .05f, 5, 2, 0xFFB08040, 0xFFFFE0A0, light);
                }
            }
            poseStack.popPose();
        }
        // Spikes in the back too
        for (int r = 0; r < 6; r++) {
            for (int k = 0; k < 4; k++) {
                float a = Mth.lerp((k + .5f) / 4, Mth.PI + .2f, Mth.TWO_PI - .2f);
                float y = .3f + r * .42f;
                Vector3f at = skin(a, y, .97f);
                BossMesh.horn(gold, poseStack.last(), at, new Vector3f(-Mth.cos(a), 0, -Mth.sin(a)).normalize(), new Vector3f(), .3f, .045f, 5, 2, 0xFFB08040, 0xFFFFE0A0,
                        light);
            }
        }
        // The crown of spikes on its head
        for (int k = 0; k < 9; k++) {
            float a = k * Mth.TWO_PI / 9;
            Vector3f base = skin(a, 3.3f, 1.0f);
            BossMesh.horn(gold, poseStack.last(), base, new Vector3f(Mth.cos(a) * .4f, 1, Mth.sin(a) * .3f).normalize(), new Vector3f(), k % 2 == 0 ? .55f : .35f, .06f,
                    5, 3, 0xFFFFFFFF, 0xFFFFF0C0, light);
        }
        poseStack.popPose();

        // ---- light: its eyes, the red glow of its hollow, the light leaking from its seams once shut
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        Vector3f eye = turn.transform(new Vector3f(0, 3.22f, .38f).mul(scale)).add(0, y0, 0);
        Vector3f across = turn.transform(new Vector3f(.13f * scale, 0, 0));
        float burn = .7f + .3f * Mth.sin(age * .7f);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(eye).add(across), .35f * scale, 0, -1, BossMesh.glow(zone, burn), light);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(eye).sub(across), .35f * scale, 0, -1, BossMesh.glow(zone, burn), light);
        Vector3f heart = turn.transform(new Vector3f(0, 1.4f, .1f).mul(scale)).add(0, y0, 0);
        float inside = hit < 0 ? .6f * Mth.clamp(open / 1.95f, 0, 1) : 0;
        float leak = hit >= 0 ? (1 - BossMesh.ramp(hit, 4, 20)) : 0;
        RitualMesh.billboard(glow, poseStack, view, heart, 3.2f * scale, 0, -1, BossMesh.glow(zone, inside + .35f * leak), light);
        if (leak > 0) {
            VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
            Vector3f bottom = turn.transform(new Vector3f(0, .1f, .72f * .6f).mul(scale)).add(0, y0, 0);
            Vector3f top = turn.transform(new Vector3f(0, SHOULDER, .72f * .84f).mul(scale)).add(0, y0, 0);
            RitualMesh.shaft(beam, poseStack.last(), view.eye(), bottom, top, .5f * scale, BossMesh.glow(zone, leak), light);
        }
    }

    private static void place(PoseStack poseStack, Quaternionf turn, float y0, float scale, float jolt) {
        poseStack.pushPose();
        poseStack.translate(0, y0, 0);
        poseStack.mulPose(turn);
        poseStack.mulPose(new Quaternionf().rotationZ(jolt));
        poseStack.scale(scale, scale, scale);
    }

    /** Into the space of the door on {@code side} (+1 right, -1 left), swung open {@code open} radians about its hinge. */
    private static void door(PoseStack poseStack, int side, float open) {
        Vector3f hinge = skin(side > 0 ? 0 : Mth.PI, 1.3f, 1);
        poseStack.pushPose();
        poseStack.translate(hinge.x, 0, hinge.z);
        poseStack.mulPose(new Quaternionf().rotationY(side * open));
        poseStack.translate(-hinge.x, 0, -hinge.z);
    }

    /** The casket's skin between the angles {@code a0} and {@code a1}, from {@code y0} up to {@code y1}: its outside, and its inside darker. */
    private static void shell(VertexConsumer c, PoseStack.Pose p, float a0, float a1, float y0, float y1, int light) {
        for (int i = 0; i < ROWS; i++) {
            float ya = Mth.lerp((float) i / ROWS, y0, y1), yb = Mth.lerp((float) (i + 1) / ROWS, y0, y1);
            for (int j = 0; j < COLS; j++) {
                float aa = Mth.lerp((float) j / COLS, a0, a1), ab = Mth.lerp((float) (j + 1) / COLS, a0, a1);
                Vector3f p00 = skin(aa, ya, 1), p01 = skin(ab, ya, 1), p11 = skin(ab, yb, 1), p10 = skin(aa, yb, 1);
                Vector3f n0 = new Vector3f(Mth.cos(aa), 0, Mth.sin(aa) / .72f).normalize(), n1 = new Vector3f(Mth.cos(ab), 0, Mth.sin(ab) / .72f).normalize();
                float u0 = (float) j / COLS, u1 = (float) (j + 1) / COLS;
                // Wound to face out (up the skin, round toward greater angles), then in
                Sculpt.put(c, p, p00, u0, ya * -.5f, 0xFFFFFFFF, light, n0);
                Sculpt.put(c, p, p10, u0, yb * -.5f, 0xFFFFFFFF, light, n0);
                Sculpt.put(c, p, p11, u1, yb * -.5f, 0xFFFFFFFF, light, n1);
                Sculpt.put(c, p, p01, u1, ya * -.5f, 0xFFFFFFFF, light, n1);
                Vector3f m0 = new Vector3f(n0).negate(), m1 = new Vector3f(n1).negate();
                Sculpt.put(c, p, p01, u1, ya * -.5f, 0xFF4A1A1A, light, m1);
                Sculpt.put(c, p, p11, u1, yb * -.5f, 0xFF4A1A1A, light, m1);
                Sculpt.put(c, p, p10, u0, yb * -.5f, 0xFF4A1A1A, light, m0);
                Sculpt.put(c, p, p00, u0, ya * -.5f, 0xFF4A1A1A, light, m0);
            }
        }
    }

    /** A band of gold round the skin at height {@code y}, between the angles {@code a0} and {@code a1}. */
    private static void band(VertexConsumer c, PoseStack.Pose p, float a0, float a1, float y, int light) {
        int n = 9;
        Vector3f[] pts = new Vector3f[n];
        for (int i = 0; i < n; i++) {
            pts[i] = skin(Mth.lerp((float) i / (n - 1), a0, a1), y, 1.02f);
        }
        Sculpt.sweep(c, p, pts, Sculpt.fill(.05f, n), Sculpt.fill(.09f, n), Sculpt.RECT, true, new Vector3f(0, 1, 0), 1, Sculpt.fill(0xFFFFFFFF, n), true, light);
    }

    /** The stern face on the front of its head: a brow, a long nose, closed lips, slits for eyes. */
    private static void face(VertexConsumer c, PoseStack poseStack, int light) {
        int iron = 0xFFC8C0D0, dark = 0xFF100808;
        Figures.box(c, poseStack, 0, 3.32f, .3f, .5f, .07f, .1f, .02f, iron, light);
        Figures.box(c, poseStack, 0, 3.12f, .36f, .08f, .3f, .1f, .02f, iron, light);
        Figures.box(c, poseStack, 0, 2.96f, .32f, .2f, .05f, .08f, .015f, iron, light);
        for (int side = -1; side <= 1; side += 2) {
            Figures.box(c, poseStack, side * .13f, 3.22f, .325f, .14f, .035f, .04f, .01f, dark, light);
            Figures.box(c, poseStack, side * .2f, 3.06f, .29f, .1f, .14f, .08f, .02f, iron, light);
        }
    }
}
