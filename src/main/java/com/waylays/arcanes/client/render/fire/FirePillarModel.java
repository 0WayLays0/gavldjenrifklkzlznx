package com.waylays.arcanes.client.render.fire;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import static com.waylays.arcanes.client.render.water.WaterMesh.*;

/**
 * A pillar of Stellar Fire bursting out of the ground: first the ground glows and smoulders in a widening ring, cracks
 * of light in it; then a column of fire roars up, a twisting curtain of flame round a white-hot heart, tongues licking
 * up its sides and off its top, sparks flying up, smoke billowing over it; then it sinks back and the ground glows a
 * moment longer.
 */
public final class FirePillarModel {
    private FirePillarModel() {}

    /**
     * {@code warn} (0 to 1) how far the ground has kindled before it bursts, {@code rise} (0 to 1) how high the fire
     * stands, {@code end} (0 to 1) how far it has sunk back; {@code height}, {@code radius} its size; {@code tier} its heat.
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, View view, float age, float warn, float rise, float end, float height, float radius,
                            int tier, int seed, int light) {
        Vector3f eye = view.eye();
        float alive = 1 - end;
        float h = height * rise * (1 - end * .7f);
        // The ground kindling: a glowing ring, then the light of the fire on it
        VertexConsumer ring = FireMesh.light(buffer, FireMesh.RING);
        FireMesh.groundRing(ring, poseStack.last(), .05f, radius * (.6f + .7f * warn), FireMesh.glowOf(tier), .9f * Math.min(warn, alive + .3f), light);
        VertexConsumer glow = FireMesh.light(buffer, FireMesh.GLOW);
        FireMesh.groundRing(glow, poseStack.last(), .07f, radius * (1.2f + 2.4f * rise), FireMesh.glowOf(tier), (.4f * warn + .6f * rise) * alive, light);
        if (rise > 0) {
            for (int k = 0; k < 4; k++) {
                FireMesh.halo(glow, poseStack, view, 0, h * (.2f + .25f * k), 0, radius * 3.5f, FireMesh.glowOf(tier), .35f * rise * alive, light);
            }
        }
        // Little flames kindling in the ring
        VertexConsumer flames = FireMesh.flames(buffer, tier);
        PoseStack.Pose p = poseStack.last();
        if (rise <= 0) {
            for (int k = 0; k < 10; k++) {
                float a = k * Mth.TWO_PI / 10 + hash(seed + k) * .5f;
                float rr = radius * (.4f + .6f * hash(seed + k * 3));
                float hh = .25f + .6f * warn * hash(seed + k * 5);
                FireMesh.tongue(flames, p, eye, Mth.cos(a) * rr, 0, Mth.sin(a) * rr, hh * .7f, hh, 0, FireMesh.frame(age, k), argb(0xFFFFFF, warn), light);
            }
            return;
        }
        // The column: a twisting curtain of fire round a white-hot heart
        VertexConsumer walls = FireMesh.walls(buffer, tier);
        p = poseStack.last();
        int n = 10;
        Vector3f[] axis = new Vector3f[n];
        float[] outer = new float[n], inner = new float[n];
        int[] oc = new int[n], ic = new int[n];
        for (int i = 0; i < n; i++) {
            float t = (float) i / (n - 1);
            axis[i] = new Vector3f(noise(t * 2, age * .1f, seed) * .15f * t, t * h, noise(t * 2, age * .1f, seed + 5) * .15f * t);
            outer[i] = radius * (.85f + .25f * t) * (1 - .4f * end);
            inner[i] = radius * .45f * (1 - .5f * t);
            float top = Mth.clamp((1 - t) * 3, 0, 1);
            oc[i] = argb(0xFFFFFF, .95f * top * alive);
            ic[i] = argb(0xFFFFFF, .9f * top * alive);
        }
        tube(walls, p, axis, outer, oc, 14, .35f, 1.5f, -1, -1 / Math.max(1, h), 1.5f + .3f * Mth.sin(age * .1f), seed + age * .3f, light, 1);
        tube(walls, p, axis, inner, ic, 8, .25f, 2, -1, -1 / Math.max(1, h), -1 + .3f * Mth.cos(age * .13f), seed + 7 + age * .4f, light, 1);
        // Tongues licking up its sides and off its top
        flames = FireMesh.flames(buffer, tier);
        p = poseStack.last();
        for (int k = 0; k < 34; k++) {
            float t = (age * .08f + hash(seed + k * 7)) % 1;
            float a = hash(seed + k * 11) * Mth.TWO_PI + age * .05f;
            float rr = radius * (.6f + .6f * hash(seed + k * 13));
            float y = h * (.1f + .95f * t);
            float hh = (1 + 1.4f * hash(seed + k)) * (1 - t * .5f) * radius;
            FireMesh.tongue(flames, p, eye, Mth.cos(a) * rr, y - hh * .3f, Mth.sin(a) * rr, hh * .55f, hh, Mth.sin(a + age * .2f) * .3f, FireMesh.frame(age, k),
                    argb(0xFFFFFF, (1 - t * t) * alive), light);
        }
        // Smoke billowing over it
        VertexConsumer smoke = WaterMesh.translucent(buffer, FireMesh.SMOKE);
        float soot = Math.max(.15f, 1 - tier * .18f);
        for (int k = 0; k < 9; k++) {
            float t = (age * .03f + hash(seed + k * 17)) % 1;
            float a = hash(seed + k * 19) * Mth.TWO_PI;
            puff(smoke, poseStack, view, Mth.cos(a) * radius * t, h + .3f + 2.4f * t, Mth.sin(a) * radius * t, radius * (1.6f + 2 * t), k + age * .02f, -1,
                    argb(0xC8C0BC, .35f * soot * Mth.sin(t * Mth.PI) * alive), light);
        }
        // Sparks flying up
        VertexConsumer sparks = FireMesh.light(buffer, FireMesh.SPARK);
        p = poseStack.last();
        for (int k = 0; k < 26; k++) {
            float t = (age * .07f + hash(seed + k * 23)) % 1;
            float a = hash(seed + k * 29) * Mth.TWO_PI + t * 2;
            float rr = radius * (.3f + 1.2f * t * hash(seed + k));
            Vector3f head = new Vector3f(Mth.cos(a) * rr, h * (.2f + 1.1f * t), Mth.sin(a) * rr);
            streak(sparks, p, eye, head, new Vector3f(head).add(0, -.45f, 0), .07f, glow(FireMesh.core(tier), (1 - t) * alive), light);
        }
    }
}
