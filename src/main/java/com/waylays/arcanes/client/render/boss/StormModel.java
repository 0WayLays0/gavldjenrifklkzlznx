package com.waylays.arcanes.client.render.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Wrath of the Heavens: a storm gathers over the arena, a great wheel of dark cloud turning round a glowing eye, its
 * underside lit from within by flickers of lightning; and the bolts it throws down, jagged and forked, blue-white,
 * flickering two or three times where they strike. When its wrath is spent the storm unwinds and thins away.
 */
public final class StormModel {
    /** How long a bolt shows (ticks). */
    public static final int BOLT = 8;
    /** How long the storm takes to gather, and to thin away (ticks). */
    public static final int GATHER = 30;
    private static final int ARMS = 3;
    private static final int PUFFS = 22;
    private static final int CLOUD = 0x323A4A;
    private static final int FLASH = 0xBFD8FF;

    private StormModel() {}

    /**
     * The storm, drawn from the middle of the arena (the pose's origin, on the ground); {@code height} where it hangs,
     * {@code radius} how far it spreads; {@code age} ticks since it began to gather, {@code since} ticks since its wrath
     * was spent (negative before).
     */
    public static void storm(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float height, float radius, float age, float since, int seed,
                             int light) {
        float gather = BossMesh.ramp(age, 0, GATHER);
        float thin = since > 0 ? BossMesh.ramp(since, 0, GATHER) : 0;
        float body = gather * (1 - thin);
        if (body <= .01f) {
            return;
        }
        float turn = age * .012f;
        // The cloud: arms of dark puffs winding in to the eye, lower toward it, lit from within now and then
        VertexConsumer cloud = BossMesh.see(buffer, BossMesh.CLOUD);
        int lit = (int) (age / 5);
        for (int arm = 0; arm < ARMS; arm++) {
            for (int k = 0; k < PUFFS; k++) {
                float t = (float) k / (PUFFS - 1);
                float r = radius * (.12f + .95f * t) * (.4f + .6f * gather);
                float a = arm * Mth.TWO_PI / ARMS + t * 3.2f - turn * (2 - t) + BossMesh.hash(seed + arm * 50 + k) * .25f;
                float y = height - 2.5f * (1 - t) + (BossMesh.hash(seed + k * 3 + arm) - .5f) * 1.2f;
                float size = (4 + 5 * t) * (.7f + .5f * BossMesh.hash(seed + k * 7 + arm));
                boolean flash = BossMesh.hash(seed + lit * 31 + k + arm * 7) > .9f;
                int col = flash ? BossMesh.mix(CLOUD, FLASH, .55f) : BossMesh.mix(CLOUD, 0x5A6478, BossMesh.hash(seed + k * 11 + arm));
                WaterMesh.puff(cloud, poseStack, view, Mth.cos(a) * r, y, Mth.sin(a) * r, size, a * 2 + k, -1, BossMesh.argb(col, .88f * body), light);
            }
        }
        // A flat roof of cloud over it all
        for (int k = 0; k < 14; k++) {
            float a = k * Mth.TWO_PI / 14 + turn * .5f;
            float r = radius * (.5f + .5f * BossMesh.hash(seed + k * 13)) * gather;
            WaterMesh.puff(cloud, poseStack, view, Mth.cos(a) * r, height + 1.6f, Mth.sin(a) * r, 9, a, -1, BossMesh.argb(0x262C38, .8f * body), light);
        }
        // The eye, glowing and turning
        VertexConsumer swirl = BossMesh.light(buffer, BossMesh.SWIRL);
        Vector3f eye = new Vector3f(0, height - 2.4f, 0);
        float er = radius * .32f * gather;
        BossMesh.plate(swirl, poseStack.last(), eye, new Vector3f(Mth.cos(-age * .05f), 0, Mth.sin(-age * .05f)), new Vector3f(-Mth.sin(-age * .05f), 0,
                Mth.cos(-age * .05f)), er * 2, BossMesh.glow(0x6AA8FF, .8f * body), light);
        // Rain falling in curtains from its underside
        VertexConsumer rain = BossMesh.light(buffer, BossMesh.BEAM);
        for (int k = 0; k < 40; k++) {
            float a = BossMesh.hash(seed + k * 19) * Mth.TWO_PI + turn;
            float r = radius * (.2f + .75f * BossMesh.hash(seed + k * 23)) * gather;
            float fall = (age * .9f + BossMesh.hash(seed + k * 29) * height) % height;
            Vector3f top = new Vector3f(Mth.cos(a) * r, height - 1.5f - fall, Mth.sin(a) * r);
            RitualMesh.shaft(rain, poseStack.last(), view.eye(), top, new Vector3f(top).add(.4f, -3.5f, .2f), .08f, BossMesh.glow(0x9AB8E0, .45f * body), light);
        }
        // The eye's frame: a ring of gold, rays of gold round it, and the black slit of its pupil
        VertexConsumer gold = BossMesh.solid(buffer, BellModel.GOLD);
        poseStack.pushPose();
        poseStack.translate(eye.x, eye.y + .1f, eye.z);
        poseStack.mulPose(new org.joml.Quaternionf().rotationY(age * .01f));
        Sculpt.torus(gold, poseStack.last(), er * 1.02f, er * .07f, 48, 6, 0, Mth.TWO_PI, 8, 0xFFFFFFFF, light);
        Sculpt.torus(gold, poseStack.last(), er * 1.2f, er * .035f, 48, 5, 0, Mth.TWO_PI, 8, 0xFFE0C080, light);
        for (int k = 0; k < 16; k++) {
            float a = k * Mth.TWO_PI / 16;
            float len = er * (k % 2 == 0 ? .7f : .4f);
            BossMesh.horn(gold, poseStack.last(), new Vector3f(Mth.cos(a) * er * 1.05f, 0, Mth.sin(a) * er * 1.05f), new Vector3f(Mth.cos(a), -.15f, Mth.sin(a))
                    .normalize(), new Vector3f(0, -.1f, 0), len, er * .06f, 5, 3, 0xFFFFFFFF, 0xFFFFF0C0, light);
        }
        poseStack.popPose();
        VertexConsumer dark = BossMesh.solid(buffer, BellModel.IRON);
        poseStack.pushPose();
        poseStack.translate(eye.x, eye.y - .05f, eye.z);
        // The pupil follows the bolts: it narrows as it strikes
        float narrow = .35f + .25f * Mth.sin(age * .2f);
        poseStack.mulPose(new org.joml.Quaternionf().rotationY(-age * .03f).rotateX(Mth.HALF_PI));
        Sculpt.slab(dark, poseStack.last(), new float[] {0, -er * .8f, er * .12f * narrow * 2, -er * .3f, er * .14f * narrow * 2, 0, er * .12f * narrow * 2, er * .3f, 0,
                er * .8f, -er * .12f * narrow * 2, er * .3f, -er * .14f * narrow * 2, 0, -er * .12f * narrow * 2, -er * .3f}, .08f, .02f, 1, 0xFF05060A, 0xFF05060A, light);
        poseStack.popPose();
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, eye, er * 2.6f, 0, -1, BossMesh.glow(0x5A8CFF, .5f * body), light);
        // Lightning flickering inside the cloud
        for (int k = 0; k < 6; k++) {
            int beat = (int) (age / 3) + k * 17;
            if (BossMesh.hash(seed + beat * 13) > .55f) {
                continue;
            }
            float a = BossMesh.hash(seed + beat * 7) * Mth.TWO_PI;
            float r = radius * .8f * BossMesh.hash(seed + beat * 5);
            RitualMesh.billboard(glow, poseStack, view, new Vector3f(Mth.cos(a) * r, height - .5f, Mth.sin(a) * r), 7, 0, -1,
                    BossMesh.glow(FLASH, .7f * body), light);
        }
    }

    /**
     * A bolt, drawn from where it strikes (the pose's origin, on the ground) down from {@code from} (in the cloud);
     * {@code since} ticks since it struck (it shows from 0 to {@link #BOLT}).
     */
    public static void bolt(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f from, float since, int seed, int light) {
        if (since < 0 || since > BOLT) {
            return;
        }
        // It flickers: bright, dim, bright again, fading
        float k = since < 2 ? 1 : since < 3 ? .35f : since < 5 ? .85f : .5f * (1 - (since - 5) / (BOLT - 5));
        int flicker = since < 3 ? 0 : 1;
        Vector3f[] main = BossMesh.jagged(from, new Vector3f(0, 0, 0), 14, .16f, seed + flicker * 3);
        VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
        PoseStack.Pose p = poseStack.last();
        strand(beam, p, view, main, 1.4f, BossMesh.glow(0x6A9CFF, .8f * k), light);
        strand(beam, p, view, main, .38f, BossMesh.glow(0xFFFFFF, k), light);
        // Its forks
        for (int f = 0; f < 4; f++) {
            int at = 3 + (int) (BossMesh.hash(seed + f * 11) * 8);
            Vector3f start = main[at];
            Vector3f d = new Vector3f(main[at + 1]).sub(main[at]).normalize();
            Vector3f out = new Vector3f(BossMesh.hash(seed + f * 3) - .5f, 0, BossMesh.hash(seed + f * 5) - .5f).normalize().mul(.8f);
            Vector3f end = new Vector3f(d).add(out).normalize().mul(3 + 4 * BossMesh.hash(seed + f * 7)).add(start);
            Vector3f[] fork = BossMesh.jagged(start, end, 6, .25f, seed + f * 19 + flicker);
            strand(beam, p, view, fork, .7f, BossMesh.glow(0x6A9CFF, .6f * k), light);
            strand(beam, p, view, fork, .2f, BossMesh.glow(0xFFFFFF, .8f * k), light);
        }
        // Where it strikes
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(0, .6f, 0), 5 * k + 2, 0, -1, BossMesh.glow(FLASH, k), light);
        BossMesh.pool(glow, poseStack.last(), 0, .06f, 0, 4.5f, 0, BossMesh.glow(0x6A9CFF, k), light);
        VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
        float t = since / BOLT;
        BossMesh.pool(ring, poseStack.last(), 0, .08f, 0, 1.5f + 6 * t, 0, BossMesh.glow(FLASH, 1 - t), light);
        VertexConsumer sparks = BossMesh.light(buffer, BossMesh.MOTE);
        for (int j = 0; j < 12; j++) {
            float sa = BossMesh.hash(seed + j * 3) * Mth.TWO_PI;
            Vector3f v = new Vector3f(Mth.cos(sa) * .3f, .35f + .3f * BossMesh.hash(seed + j * 5), Mth.sin(sa) * .3f);
            Vector3f at = WaterMesh.thrown(new Vector3f(0, .2f, 0), v, since * 1.5f, .06f);
            if (at.y > 0) {
                RitualMesh.billboard(sparks, poseStack, view, at, .3f, 0, -1, BossMesh.glow(j % 2 == 0 ? 0xFFFFFF : FLASH, 1 - t), light);
            }
        }
    }

    private static void strand(VertexConsumer c, PoseStack.Pose p, WaterMesh.View view, Vector3f[] pts, float width, int argb, int light) {
        int[] col = new int[pts.length];
        java.util.Arrays.fill(col, argb);
        RitualMesh.thread(c, p, view.eye(), pts, width, col, light);
    }
}
