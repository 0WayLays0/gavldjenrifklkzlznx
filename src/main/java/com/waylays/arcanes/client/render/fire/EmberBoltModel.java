package com.waylays.arcanes.client.render.fire;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import static com.waylays.arcanes.client.render.water.WaterMesh.*;

/**
 * Ember Bolt in flight: a white-hot heart in a ball of flame, flames streaming back off it into a tail, a halo of its
 * colour round it, sparks peeling off behind and a thin trail of smoke.
 */
public final class EmberBoltModel {
    private EmberBoltModel() {}

    /** Round the pose's origin, flying along {@code forward}; {@code tier} its heat. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, View view, Vector3f forward, float age, int tier, int seed, int light) {
        Vector3f f = new Vector3f(forward).normalize();
        Vector3f eye = view.eye();
        float grow = Mth.clamp(age / 3, 0, 1);
        VertexConsumer glow = FireMesh.light(buffer, FireMesh.GLOW);
        FireMesh.halo(glow, poseStack, view, 0, 0, 0, 2.6f, FireMesh.glowOf(tier), .8f, light);
        FireMesh.halo(glow, poseStack, view, 0, 0, 0, .9f, FireMesh.core(tier), 1, light);
        // The tail: tongues of flame streaming back
        FireMesh.Batch fire = new FireMesh.Batch(view, tier, age, light);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < 14; k++) {
            float t = (age * .25f + hash(seed + k * 3)) % 1;
            Vector3f at = new Vector3f(f).mul(-t * 1.8f * grow).add((hash(seed + k) - .5f) * .2f * t, (hash(seed + k * 5) - .5f) * .2f * t, (hash(seed + k * 7) - .5f) * .2f * t);
            float h = .75f * (1 - t) + .2f;
            fire.lick(at.x, at.y - h * .45f, at.z, h * .8f, h, 0, seed + 31 * (k), 1 - t * t);
        }
        // Its ball of flame
        for (int k = 0; k < 4; k++) {
            float a = k * Mth.HALF_PI + age * .4f;
            fire.lick(Mth.cos(a) * .08f, -.32f, Mth.sin(a) * .08f, .6f, .75f, Mth.sin(a) * .1f, seed + 31 * (k + 7), 1);
        }
        // A thread of smoke behind
        VertexConsumer smoke = WaterMesh.translucent(buffer, FireMesh.SMOKE);
        float soot = Math.max(0, 1 - tier * .2f);
        for (int k = 0; k < 5; k++) {
            float t = (age * .08f + hash(seed + k * 11)) % 1;
            Vector3f at = new Vector3f(f).mul(-(1.2f + 2.4f * t) * grow).add(0, .3f * t, 0);
            puff(smoke, poseStack, view, at.x, at.y, at.z, .5f + .8f * t, k + age * .03f, -1, argb(0xFFFFFF, .45f * soot * (1 - t)), light);
        }
        // Sparks peeling off
        VertexConsumer sparks = FireMesh.light(buffer, FireMesh.SPARK);
        p = poseStack.last();
        for (int k = 0; k < 12; k++) {
            float t = (age * .2f + hash(seed + k * 13)) % 1;
            Vector3f side = new Vector3f((hash(seed + k * 17) - .5f), (hash(seed + k * 19) - .3f), (hash(seed + k * 23) - .5f)).mul(.9f * t);
            Vector3f head = new Vector3f(f).mul(-(.3f + 2 * t) * grow).add(side).add(0, -.4f * t * t, 0);
            Vector3f tail = new Vector3f(head).add(new Vector3f(f).mul(.35f));
            streak(sparks, p, eye, tail, head, .07f, glow(FireMesh.core(tier), 1 - t), light);
        }
        fire.flush(buffer, poseStack);
    }
}
