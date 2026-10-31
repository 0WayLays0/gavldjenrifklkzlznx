package com.waylays.arcanes.client.render.fire;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import static com.waylays.arcanes.client.render.water.WaterMesh.*;

/**
 * A will-o'-wisp of Stellar Fire: a small living flame, a bright heart that pulses in a little crown of tongues, two
 * motes of light circling it, a wispy tail of sparks and flame trailing where it has been.
 */
public final class WispModel {
    private WispModel() {}

    /** Round the pose's origin; {@code trail} where it is going from (unit, the tail streams the other way), {@code fade} (0 to 1). */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, View view, Vector3f trail, float age, float fade, int tier, int seed, int light) {
        Vector3f eye = view.eye();
        float alive = 1 - fade;
        float pulse = .85f + .15f * Mth.sin(age * .5f + seed);
        VertexConsumer glow = FireMesh.light(buffer, FireMesh.GLOW);
        FireMesh.halo(glow, poseStack, view, 0, 0, 0, 2.2f * pulse, FireMesh.glowOf(tier), .7f * alive, light);
        FireMesh.halo(glow, poseStack, view, 0, 0, 0, .7f * pulse, FireMesh.core(tier), alive, light);
        // Two motes circling it
        for (int k = 0; k < 2; k++) {
            float a = age * .3f + k * Mth.PI;
            FireMesh.halo(glow, poseStack, view, Mth.cos(a) * .5f, .1f * Mth.sin(a * 2), Mth.sin(a) * .5f, .25f, FireMesh.core(tier), alive, light);
        }
        // Its crown of tongues, and the tail streaming behind
        VertexConsumer flames = FireMesh.flames(buffer, tier);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < 5; k++) {
            float a = k * Mth.TWO_PI / 5 + age * .2f;
            FireMesh.tongue(flames, p, eye, Mth.cos(a) * .12f, -.3f, Mth.sin(a) * .12f, .45f, .65f * pulse, Mth.sin(a) * .08f, FireMesh.frame(age, k + seed),
                    argb(0xFFFFFF, alive), light);
        }
        Vector3f back = new Vector3f(trail).normalize().negate();
        for (int k = 0; k < 7; k++) {
            float t = (age * .2f + hash(seed + k * 3)) % 1;
            Vector3f c = new Vector3f(back).mul(t * 1.6f).add(0, .25f * t, 0);
            float h = .5f * (1 - t) + .1f;
            FireMesh.tongue(flames, p, eye, c.x, c.y - h * .4f, c.z, h * .7f, h, 0, FireMesh.frame(age, k), argb(0xFFFFFF, (1 - t) * alive), light);
        }
        VertexConsumer sparks = FireMesh.light(buffer, FireMesh.SPARK);
        p = poseStack.last();
        for (int k = 0; k < 8; k++) {
            float t = (age * .25f + hash(seed + k * 7)) % 1;
            Vector3f head = new Vector3f(back).mul(.2f + 1.8f * t).add((hash(seed + k) - .5f) * .5f * t, .3f * t, (hash(seed + k * 5) - .5f) * .5f * t);
            streak(sparks, p, eye, head, new Vector3f(head).add(new Vector3f(back).mul(.3f)), .05f, WaterMesh.glow(FireMesh.core(tier), (1 - t) * alive), light);
        }
    }
}
