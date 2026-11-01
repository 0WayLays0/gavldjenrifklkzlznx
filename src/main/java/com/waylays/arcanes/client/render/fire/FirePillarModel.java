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
        FireMesh.Batch fire = new FireMesh.Batch(view, tier, age, light);
        if (rise <= 0) {
            for (int k = 0; k < 9; k++) {
                float a = k * Mth.TWO_PI / 9 + hash(seed + k) * .5f;
                float rr = radius * (.3f + .6f * hash(seed + k * 3));
                float hh = (.3f + .7f * warn * hash(seed + k * 5)) * radius;
                fire.lick(Mth.cos(a) * rr, 0, Mth.sin(a) * rr, hh * .8f, hh, 0, seed + k, warn);
            }
            fire.flush(buffer, poseStack);
            return;
        }
        // The column: a crown of flames at its foot, a tall white-hot heart, flames breaking off it and rising
        for (int k = 0; k < 11; k++) {
            float a = k * Mth.TWO_PI / 11 + hash(seed + k * 3) * .4f;
            float rr = radius * (.55f + .2f * hash(seed + k * 5));
            float hh = h * (.45f + .3f * hash(seed + k * 7));
            fire.lick(Mth.cos(a) * rr, 0, Mth.sin(a) * rr, radius * 1.1f, hh, 0, seed + k * 17, alive);
        }
        for (int k = 0; k < 3; k++) {
            float a = k * Mth.TWO_PI / 3 + age * .02f;
            fire.lick(Mth.cos(a) * radius * .2f, 0, Mth.sin(a) * radius * .2f, radius * 1.5f, h * (.95f + .1f * k), 0, seed + 100 + k, alive);
        }
        for (int k = 0; k < 16; k++) {
            float t = (age * .045f + hash(seed + k * 7)) % 1;
            float a = hash(seed + k * 11) * Mth.TWO_PI + age * .04f + t * 2;
            float rr = radius * (.25f + .45f * hash(seed + k * 13)) * (1 - .4f * t);
            float y = h * (.25f + .8f * t);
            float hh = h * .4f * (1 - t * .7f);
            fire.lick(Mth.cos(a) * rr, y, Mth.sin(a) * rr, radius * (1.1f - .6f * t), hh, 0, seed + 200 + k, Mth.sin((1 - t) * Mth.HALF_PI) * alive);
        }
        // Smoke billowing over it
        VertexConsumer smoke = WaterMesh.translucent(buffer, FireMesh.SMOKE);
        float soot = Math.max(.15f, 1 - tier * .18f);
        for (int k = 0; k < 9; k++) {
            float t = (age * .03f + hash(seed + k * 17)) % 1;
            float a = hash(seed + k * 19) * Mth.TWO_PI;
            puff(smoke, poseStack, view, Mth.cos(a) * radius * t, h + .3f + 2.4f * t, Mth.sin(a) * radius * t, radius * (1.6f + 2 * t), k + age * .02f, -1,
                    argb(0xC8C0BC, .3f * soot * Mth.sin(t * Mth.PI) * alive), light);
        }
        // Embers flying up, swaying
        VertexConsumer embers = FireMesh.light(buffer, FireMesh.EMBER);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < 30; k++) {
            float t = (age * .06f + hash(seed + k * 23)) % 1;
            float a = hash(seed + k * 29) * Mth.TWO_PI + t * 3;
            float rr = radius * (.3f + 1.3f * t * hash(seed + k));
            Vector3f head = new Vector3f(Mth.cos(a) * rr, h * (.2f + 1.15f * t), Mth.sin(a) * rr);
            Vector3f tail = new Vector3f(Mth.cos(a - .3f) * rr, head.y - .35f, Mth.sin(a - .3f) * rr);
            FireMesh.ember(embers, p, eye, head, tail, .1f, k % 3 == 0 ? FireMesh.core(tier) : FireMesh.glowOf(tier), (1 - t) * alive, light);
        }
        fire.flush(buffer, poseStack);
    }
}
