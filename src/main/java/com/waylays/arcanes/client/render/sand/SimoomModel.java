package com.waylays.arcanes.client.render.sand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Simoom: a towering funnel of sand, narrow at the ground and opening wide high up, its walls bands of sand racing round
 * at different speeds, a darker core turning inside; stones whirled up its sides; a skirt of dust billowing round its
 * foot; lightning flickering in it, each bolt leaving a glowing scar of glass on the ground.
 */
public final class SimoomModel {
    private static final int BANDS = 12;

    private SimoomModel() {}

    /** Its funnel's radius at {@code y} up a funnel {@code height} tall whose foot is {@code radius} across. */
    private static float funnel(float y, float height, float radius) {
        float t = Mth.clamp(y / height, 0, 1);
        return radius * (.32f + 1.05f * (float) Math.pow(t, 1.4f));
    }

    /**
     * At its foot (the pose's origin), {@code radius} across there; {@code age} of {@code life} ticks; {@code since}
     * ticks since lightning last struck, at {@code strike} (in the same frame).
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float radius, float age, float life, float since, Vector3f strike,
                            int seed, int light) {
        float rise = BossMesh.ramp(age, 0, 24);
        float fall = BossMesh.ramp(age, life - 16, life);
        float k0 = rise * (1 - fall);
        float height = 15 * (.3f + .7f * rise) * (1 - .5f * fall);
        // Its core, darker, turning
        VertexConsumer flow = BossMesh.see(buffer, SandMesh.FLOW);
        PoseStack.Pose p = poseStack.last();
        int n = 10;
        float[] ys = new float[n + 1], rs = new float[n + 1];
        for (int i = 0; i <= n; i++) {
            ys[i] = height * i / n;
            rs[i] = funnel(ys[i], height, radius) * .7f;
        }
        SandMesh.lathe(flow, p, new Vector3f(), ys, rs, 22, 3, -age * .15f, BossMesh.argb(0x9A7448, .55f * k0), light);
        // Its walls: bands of sand racing round
        for (int b = 0; b < BANDS; b++) {
            float y = height * (b + .5f) / BANDS;
            float r = funnel(y, height, radius);
            float speed = (.32f - .015f * b) * (b % 3 == 0 ? 1.2f : 1);
            for (int arc = 0; arc < 3; arc++) {
                int m = 10;
                Vector3f[] pts = new Vector3f[m + 1];
                float[] w = new float[m + 1];
                int[] col = new int[m + 1];
                float a0 = age * speed + arc * Mth.TWO_PI / 3 + b * .7f;
                for (int i = 0; i <= m; i++) {
                    float t = (float) i / m;
                    float a = a0 + t * 1.7f;
                    float wob = Mth.sin(a * 2 + age * .1f + b) * .25f;
                    pts[i] = new Vector3f(Mth.cos(a) * r, y + wob + t * .4f, Mth.sin(a) * r);
                    w[i] = (.7f + .09f * b) * Mth.sin(t * Mth.PI) + .05f;
                    col[i] = BossMesh.argb(b % 2 == 0 ? SandMesh.SAND : SandMesh.SAND_PALE, .8f * k0 * Mth.sin(t * Mth.PI));
                }
                SandMesh.ribbon(flow, p, view.eye(), pts, w, .8f, age * .3f + arc, col, light);
            }
        }
        // Stones whirled up its sides
        VertexConsumer stone = BossMesh.solid(buffer, SandMesh.SANDSTONE);
        p = poseStack.last();
        for (int k = 0; k < 12; k++) {
            float t = (age * .012f + BossMesh.hash(seed + k)) % 1;
            float y = height * .9f * t;
            float a = age * .3f + k * 2.3f + t * 6;
            float r = funnel(y, height, radius) * 1.05f;
            float s = (.12f + .14f * BossMesh.hash(seed + k * 3)) * k0;
            Vector3f c = new Vector3f(Mth.cos(a) * r, y, Mth.sin(a) * r);
            Vector3f ax = new Vector3f(Mth.cos(a + age * .2f), .3f, Mth.sin(a + age * .2f)).normalize().mul(s);
            Vector3f ay = new Vector3f(0, s * .8f, 0);
            Vector3f az = new Vector3f(ax).cross(ay).normalize().mul(s * .9f);
            BossMesh.box(stone, p, c, ax, ay, az, 0xFFFFFFFF, light);
        }
        // A skirt of dust round its foot
        VertexConsumer dust = BossMesh.see(buffer, BossMesh.DUST);
        for (int k = 0; k < 14; k++) {
            float a = k * Mth.TWO_PI / 14 + age * .08f;
            float r = radius * (1 + .3f * Mth.sin(age * .1f + k));
            SandMesh.cloud(dust, poseStack, view, new Vector3f(Mth.cos(a) * r, .6f + .4f * Mth.sin(k + age * .2f), Mth.sin(a) * r), 2.6f, a, k % 2 == 0 ? SandMesh.SAND : SandMesh.SAND_PALE,
                    .6f * k0, light);
        }
        VertexConsumer ground = BossMesh.ground(buffer, SandMesh.SWIRL);
        BossMesh.pool(ground, poseStack.last(), 0, .03f, 0, radius * 3.2f, -age * .08f, BossMesh.argb(0xFFFFFF, .8f * k0), light);
        // Lightning in it, and the glass it leaves
        if (strike != null && since < 40) {
            VertexConsumer scar = BossMesh.light(buffer, BossMesh.GLOW);
            BossMesh.pool(scar, poseStack.last(), strike.x, .05f, strike.z, 1.8f, 0, BossMesh.glow(0xBFF4FF, (1 - since / 40) * .8f), light);
            if (since < 6) {
                float f = 1 - since / 6;
                Vector3f[] bolt = BossMesh.jagged(new Vector3f(strike.x * .4f, height * .8f, strike.z * .4f), new Vector3f(strike.x, .1f, strike.z), 10, .35f,
                        seed + (int) (age - since));
                VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
                RitualMesh.thread(beam, poseStack.last(), view.eye(), bolt, .35f, SandMesh.colours(BossMesh.glow(0xDDF6FF, f), bolt.length), light);
                RitualMesh.thread(beam, poseStack.last(), view.eye(), bolt, .12f, SandMesh.colours(BossMesh.glow(0xFFFFFF, f), bolt.length), light);
                VertexConsumer flash = BossMesh.light(buffer, BossMesh.GLOW);
                RitualMesh.billboard(flash, poseStack, view, new Vector3f(strike.x * .7f, height * .5f, strike.z * .7f), 8 * f, 0, -1, BossMesh.glow(0xCFEFFF, .5f * f),
                        light);
            }
        }
        SandMesh.grains(poseStack, buffer, view, radius * 1.5f, height, age, 24, k0, seed, light);
    }
}
