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
 * Clock of Fate: the ground round the caster becomes the face of a great clock. A rim of gold rises round it, twelve
 * numerals of gold stand at the hours, runes and the minutes burning in the ground; a gear turns under the caster's feet. Its minute hand, a long blade of gold, ticks back and forth while the warning runs, then sweeps
 * the whole face round, each hour of ground burning as it passes over it; the hour hand follows behind.
 */
public final class ClockModel {
    private static final ResourceLocation GLOW = ArcanesOublies.id("textures/entity/boss/m_clock_glow.png");
    private static final String[] NUMERALS = {"XII", "I", "II", "III", "IIII", "V", "VI", "VII", "VIII", "IX", "X", "XI"};

    private ClockModel() {}

    /** The minute hand's direction as a Minecraft yaw (degrees) {@code hit} ticks after the first hour burns. */
    private static float hand(float yaw0, float hit, int slices, int step) {
        float per = 360f / slices;
        if (hit < 0) {
            // Ticking back and forth, an hour before the first
            float tick = Mth.floor(-hit / 5) % 2 == 0 ? 0 : 4;
            return yaw0 - per - tick;
        }
        // Over the middle of each hour as it burns
        return yaw0 + Math.min(hit / step, slices) * per;
    }

    private static Vector3f dir(float yaw) {
        float r = yaw * Mth.DEG_TO_RAD;
        return new Vector3f(-Mth.sin(r), 0, Mth.cos(r));
    }

    /** Round the caster (the pose's origin), its first hour toward {@code yaw0}; {@code hit} ticks since the first hour burned. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float hit, float yaw0, int slices, int step, float radius,
                            int zone, int seed, int light) {
        float rise = BossMesh.ramp(age, 0, 16);
        float done = slices * step;
        float sink = BossMesh.ramp(hit, done + 6, done + 20);
        float k = rise * (1 - sink);
        if (k < .01f) {
            return;
        }
        float minute = hand(yaw0, hit, slices, step);
        float hour = yaw0 - 15 + (hit > 0 ? Math.min(hit, done) / done * 30 : 0);
        float lift = (k - 1) * 1.2f;

        // ---- gold: the rim, the numerals, the gear underfoot, the hands
        VertexConsumer gold = BossMesh.solid(buffer, BellModel.GOLD);
        poseStack.pushPose();
        poseStack.translate(0, lift + .12f, 0);
        Sculpt.torus(gold, poseStack.last(), radius + .2f, .22f, 96, 8, 0, Mth.TWO_PI, 24, 0xFFFFFFFF, light);
        Sculpt.torus(gold, poseStack.last(), radius - .55f, .1f, 96, 6, 0, Mth.TWO_PI, 24, 0xFFE0C080, light);
        for (int h = 0; h < 12; h++) {
            numeral(gold, poseStack, yaw0 + h * 30, radius - 1.6f, NUMERALS[h], light);
        }
        // The minute marks between them
        for (int m = 0; m < 60; m++) {
            if (m % 5 == 0) {
                continue;
            }
            Vector3f d = dir(yaw0 + m * 6);
            poseStack.pushPose();
            poseStack.translate(d.x * (radius - .55f), .05f, d.z * (radius - .55f));
            poseStack.mulPose(new Quaternionf().rotationY(-(yaw0 + m * 6) * Mth.DEG_TO_RAD));
            Sculpt.block(gold, poseStack.last(), .12f, .16f, .5f, .02f, 1, 0xFFE0C080, light);
            poseStack.popPose();
        }
        poseStack.popPose();
        // The gear under the caster's feet, turning
        poseStack.pushPose();
        poseStack.translate(0, lift + .1f, 0);
        poseStack.mulPose(new Quaternionf().rotationY(age * .03f).rotateX(-Mth.HALF_PI));
        Sculpt.slab(gold, poseStack.last(), gear(2.2f, 1.85f, 18), .2f, .04f, .4f, 0xFFFFFFFF, 0xFFD0A050, light);
        poseStack.popPose();
        // The hands: the hour hand, broad and short; the minute hand, a long blade
        handBlade(gold, poseStack, hour, lift + .75f, radius * .58f, 1.4f, light);
        handBlade(gold, poseStack, minute, lift + .5f, radius * .96f, 1.1f, light);
        poseStack.pushPose();
        poseStack.translate(0, lift + .9f, 0);
        Sculpt.lathe(gold, poseStack.last(), new float[] {.45f, -.3f, .55f, -.15f, .5f, 0, .3f, .12f, 0, .16f}, 16, 2, 1, 0xFFFFFFFF, 0xFFFFFFFF, light);
        poseStack.popPose();

        // ---- light: the numerals' glow in the ground, the hour burning under the hand, the blade's edge
        VertexConsumer face = BossMesh.light(buffer, GLOW);
        poseStack.pushPose();
        poseStack.translate(0, .06f, 0);
        Sculpt.disc(face, poseStack.last(), 0, radius, 64, (180 - yaw0) * Mth.DEG_TO_RAD, BossMesh.glow(zone, .75f * k), light, false);
        poseStack.popPose();
        VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
        Vector3f tip = dir(minute).mul(radius * .96f).add(0, lift + .55f, 0);
        RitualMesh.shaft(beam, poseStack.last(), view.eye(), new Vector3f(0, lift + .55f, 0), tip, .6f + (hit >= 0 && hit <= done ? .5f : 0),
                BossMesh.glow(zone, .6f * k), light);
        if (hit >= 0 && hit <= done + 4) {
            VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
            // A wake of light behind the sweeping hand
            for (int i = 0; i < 10; i++) {
                float a0 = minute - i * 4, a1 = minute - (i + 1) * 4;
                int c0 = BossMesh.glow(zone, .55f * (1 - i / 10f) * k), c1 = BossMesh.glow(zone, .55f * (1 - (i + 1) / 10f) * k);
                Vector3f p0 = dir(a0), p1 = dir(a1);
                BossMesh.quad2(glow, poseStack.last(), new Vector3f(p0).mul(1.5f).add(0, .3f, 0), new Vector3f(p0).mul(radius).add(0, .3f, 0),
                        new Vector3f(p1).mul(radius).add(0, .3f, 0), new Vector3f(p1).mul(1.5f).add(0, .3f, 0), 0, .5f, 1, .5f, 1, .5f, 0, .5f, c0, c1, light);
            }
        }
    }

    /** A gear's outline: {@code teeth} teeth between radius {@code inner} and {@code outer}. */
    static float[] gear(float outer, float inner, int teeth) {
        float[] s = new float[teeth * 8];
        for (int t = 0; t < teeth; t++) {
            float a = t * Mth.TWO_PI / teeth, w = Mth.TWO_PI / teeth;
            float[] as = {a, a + w * .18f, a + w * .32f, a + w * .5f};
            float[] rs = {inner, outer, outer, inner};
            for (int j = 0; j < 4; j++) {
                s[t * 8 + j * 2] = Mth.cos(as[j]) * rs[j];
                s[t * 8 + j * 2 + 1] = Mth.sin(as[j]) * rs[j];
            }
        }
        return s;
    }

    /** A hand of the clock pointing toward {@code yaw}, {@code len} long, {@code width} wide at its broadest, {@code y} over the ground. */
    private static void handBlade(VertexConsumer c, PoseStack poseStack, float yaw, float y, float len, float width, int light) {
        float w = width * .5f;
        // From a counterweight behind the pivot, a slim shaft, a pierced lozenge, then a spear point
        float[] outline = {-.12f * len, 0, -.1f * len, -w * .8f, -.03f * len, -w * .35f, .45f * len, -w * .3f, .55f * len, -w, .65f * len, -w * .3f,
                .82f * len, -w * .45f, len, 0, .82f * len, w * .45f, .65f * len, w * .3f, .55f * len, w, .45f * len, w * .3f, -.03f * len, w * .35f, -.1f * len,
                w * .8f};
        poseStack.pushPose();
        poseStack.translate(0, y, 0);
        // The outline lies along +X; turn it to point along the hand's way, flat on the ground
        poseStack.mulPose(new Quaternionf().rotationY(-(yaw + 90) * Mth.DEG_TO_RAD).rotateX(-Mth.HALF_PI));
        Sculpt.slab(c, poseStack.last(), reverseIfClockwise(outline), .14f, .04f, .5f, 0xFFFFFFFF, 0xFFD0A050, light);
        poseStack.popPose();
    }

    private static float[] reverseIfClockwise(float[] poly) {
        float area = 0;
        int m = poly.length / 2;
        for (int i = 0; i < m; i++) {
            int j = (i + 1) % m;
            area += poly[2 * i] * poly[2 * j + 1] - poly[2 * j] * poly[2 * i + 1];
        }
        if (area >= 0) {
            return poly;
        }
        float[] r = new float[poly.length];
        for (int i = 0; i < m; i++) {
            r[2 * i] = poly[2 * (m - 1 - i)];
            r[2 * i + 1] = poly[2 * (m - 1 - i) + 1];
        }
        return r;
    }

    /** A numeral of gold standing at the hour toward {@code yaw}, {@code r} from the middle, leaning back to be read from it. */
    private static void numeral(VertexConsumer c, PoseStack poseStack, float yaw, float r, String text, int light) {
        Vector3f d = dir(yaw);
        float h = .95f, stroke = .16f;
        float width = 0;
        for (char ch : text.toCharArray()) {
            width += ch == 'I' ? .32f : .7f;
        }
        poseStack.pushPose();
        poseStack.translate(d.x * r, 0, d.z * r);
        // Facing the middle, tilted back
        poseStack.mulPose(new Quaternionf().rotationY(-(yaw + 180) * Mth.DEG_TO_RAD).rotateX(-.55f));
        float x = -width * .5f;
        for (char ch : text.toCharArray()) {
            if (ch == 'I') {
                bar(c, poseStack, x + .12f, 0, x + .12f, h, stroke, light);
                x += .32f;
            } else if (ch == 'V') {
                bar(c, poseStack, x, h, x + .3f, 0, stroke, light);
                bar(c, poseStack, x + .3f, 0, x + .6f, h, stroke, light);
                x += .7f;
            } else {
                bar(c, poseStack, x, h, x + .6f, 0, stroke, light);
                bar(c, poseStack, x, 0, x + .6f, h, stroke, light);
                x += .7f;
            }
        }
        // Its serifs: a bar along the top and the bottom
        bar(c, poseStack, -width * .5f - .08f, 0, width * .5f - .05f, 0, stroke * .7f, light);
        bar(c, poseStack, -width * .5f - .08f, h, width * .5f - .05f, h, stroke * .7f, light);
        poseStack.popPose();
    }

    /** A bar of gold from (x0, y0) to (x1, y1) in the pose's XY plane. */
    private static void bar(VertexConsumer c, PoseStack poseStack, float x0, float y0, float x1, float y1, float w, int light) {
        float dx = x1 - x0, dy = y1 - y0;
        float len = Mth.sqrt(dx * dx + dy * dy);
        poseStack.pushPose();
        poseStack.translate((x0 + x1) * .5f, (y0 + y1) * .5f, 0);
        poseStack.mulPose(new Quaternionf().rotationZ((float) Math.atan2(dy, dx)));
        Sculpt.block(c, poseStack.last(), len + w, w, w, .03f, 1, 0xFFFFFFFF, light);
        poseStack.popPose();
    }
}
