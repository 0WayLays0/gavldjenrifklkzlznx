package com.waylays.arcanes.client.render.fire;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import static com.waylays.arcanes.client.render.water.WaterMesh.*;

/**
 * Stellar Breath: a roaring cone of fire poured from the hand, white-hot where it leaves it, tongues of flame tumbling
 * out along it and spreading, its light thrown round it, sparks racing ahead and smoke curling off its far end.
 */
public final class BreathModel {
    private BreathModel() {}

    /** From {@code from} along {@code dir} (unit) for {@code range} blocks (it reaches where it can), at heat {@code tier}. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, View view, Vector3f from, Vector3f dir, float range, float age, int tier, int seed,
                            int light) {
        Vector3f eye = view.eye();
        float grow = Mth.clamp(age / 5, 0, 1);
        float reach = range * grow;
        Vector3f s1 = Math.abs(dir.y) < .95f ? new Vector3f(dir).cross(0, 1, 0).normalize() : new Vector3f(1, 0, 0);
        Vector3f s2 = new Vector3f(dir).cross(s1).normalize();
        // Its light, strongest at the hand
        VertexConsumer glow = FireMesh.light(buffer, FireMesh.GLOW);
        FireMesh.halo(glow, poseStack, view, from.x, from.y, from.z, 1.4f, FireMesh.core(tier), 1, light);
        for (int k = 0; k < 6; k++) {
            float d = reach * (k + .5f) / 6;
            Vector3f c = new Vector3f(from).add(new Vector3f(dir).mul(d));
            FireMesh.halo(glow, poseStack, view, c.x, c.y, c.z, 1.6f + d * .9f, FireMesh.glowOf(tier), .55f * (1 - (float) k / 7), light);
        }
        // Tongues tumbling out along it, spreading
        VertexConsumer flames = FireMesh.flames(buffer, tier);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < 56; k++) {
            float t = (age * .14f + hash(seed + k * 3)) % 1;
            float d = t * reach;
            float spread = .1f + .4f * d;
            float a = hash(seed + k * 5) * Mth.TWO_PI, r = Mth.sqrt(hash(seed + k * 7)) * spread;
            Vector3f c = new Vector3f(from).add(new Vector3f(dir).mul(d)).add(new Vector3f(s1).mul(Mth.cos(a) * r)).add(new Vector3f(s2).mul(Mth.sin(a) * r))
                    .add(0, .3f * t * t, 0);
            // Carried along the stream, rising a little as it slows
            Vector3f along = new Vector3f(dir).add(0, .25f + .6f * t, 0).normalize();
            float h = .5f + 1.6f * t * (.7f + .6f * hash(seed + k));
            FireMesh.tongueAlong(flames, p, eye, new Vector3f(c).sub(new Vector3f(along).mul(h * .35f)), along, h * .7f, h, FireMesh.frame(age, k),
                    argb(0xFFFFFF, Math.min(1, (1 - t) * 1.6f)), light);
        }
        // Smoke curling off its far end
        VertexConsumer smoke = WaterMesh.translucent(buffer, FireMesh.SMOKE);
        float soot = Math.max(0, 1 - tier * .2f);
        for (int k = 0; k < 6; k++) {
            float t = (age * .05f + hash(seed + k * 13)) % 1;
            Vector3f c = new Vector3f(from).add(new Vector3f(dir).mul(reach * (.7f + .3f * t))).add(0, .6f + 1.5f * t, 0);
            puff(smoke, poseStack, view, c.x, c.y, c.z, 1.2f + 2 * t, k + age * .02f, -1, argb(0xC8C0BC, .35f * soot * Mth.sin(t * Mth.PI)), light);
        }
        // Sparks racing ahead
        VertexConsumer sparks = FireMesh.light(buffer, FireMesh.SPARK);
        p = poseStack.last();
        for (int k = 0; k < 24; k++) {
            float t = (age * .2f + hash(seed + k * 17)) % 1;
            float d = t * reach * 1.1f;
            float a = hash(seed + k * 19) * Mth.TWO_PI, r = (.1f + .5f * d) * hash(seed + k * 23);
            Vector3f head = new Vector3f(from).add(new Vector3f(dir).mul(d)).add(new Vector3f(s1).mul(Mth.cos(a) * r)).add(new Vector3f(s2).mul(Mth.sin(a) * r));
            Vector3f tail = new Vector3f(head).sub(new Vector3f(dir).mul(.6f));
            streak(sparks, p, eye, head, tail, .07f, glow(FireMesh.core(tier), 1 - t), light);
        }
    }
}
