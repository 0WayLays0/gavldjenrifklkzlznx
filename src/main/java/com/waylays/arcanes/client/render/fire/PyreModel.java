package com.waylays.arcanes.client.render.fire;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import static com.waylays.arcanes.client.render.water.WaterMesh.*;

/**
 * The Pyre of Six Fires (Stellar Fire's ultimate): a towering vortex of fire round the caster, three walls of flame
 * wound round it in a spiral, tongues and sparks streaming up it in a whirl, a ring of fire on the ground at its foot,
 * its light flooding the place. It burns through the six heats one after another, red to violet, each a flash brighter
 * than the last, growing taller and hotter.
 */
public final class PyreModel {
    private PyreModel() {}

    /**
     * {@code tier} the heat it burns at now, {@code step} (0 to 1) how far into that heat (it flares as each begins),
     * {@code grow} (0 to 1) how far it has risen, {@code fade} (0 to 1) how far it has died away; {@code radius},
     * {@code height} its full size.
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, View view, float age, int tier, float step, float grow, float fade, float radius,
                            float height, int seed, int light) {
        Vector3f eye = view.eye();
        float alive = (1 - fade) * grow;
        float h = height * (.6f + .08f * tier) * grow * (1 - .5f * fade);
        float flare = Math.max(0, 1 - step * 5);
        float spin = age * (.06f + .012f * tier);
        // Its light
        VertexConsumer glow = FireMesh.light(buffer, FireMesh.GLOW);
        for (int k = 0; k < 5; k++) {
            FireMesh.halo(glow, poseStack, view, 0, h * (.1f + .22f * k), 0, radius * 4, FireMesh.glowOf(tier), (.35f + .4f * flare) * alive, light);
        }
        FireMesh.groundRing(glow, poseStack.last(), .05f, radius * 3, FireMesh.glowOf(tier), .7f * alive, light);
        VertexConsumer ring = FireMesh.light(buffer, FireMesh.RING);
        FireMesh.groundRing(ring, poseStack.last(), .06f, radius * 1.16f, FireMesh.core(tier), alive, light);
        // Three walls of flame wound round it in a spiral
        VertexConsumer walls = FireMesh.walls(buffer, tier);
        PoseStack.Pose p = poseStack.last();
        int n = 40;
        for (int arm = 0; arm < 3; arm++) {
            Vector3f[] base = new Vector3f[n];
            float[] hh = new float[n];
            int[] col = new int[n];
            for (int i = 0; i < n; i++) {
                float u = (float) i / (n - 1);
                float a = arm * Mth.TWO_PI / 3 + spin + u * 4.5f;
                float r = radius * (.35f + .65f * u) * (1 + .08f * Mth.sin(age * .2f + u * 7));
                base[i] = new Vector3f(Mth.cos(a) * r, u * h * .8f, Mth.sin(a) * r);
                hh[i] = h * .32f * (.7f + .3f * Mth.sin(u * Mth.PI));
                col[i] = argb(0xFFFFFF, .9f * alive * Mth.clamp(Math.min(u, 1 - u) * 6, 0, 1));
            }
            FireMesh.curtain(walls, p, base, hh, col, 1.6f, age * .03f, light);
        }
        // The ring of fire at its foot
        Vector3f[] base = new Vector3f[49];
        float[] hh = new float[49];
        int[] col = new int[49];
        for (int i = 0; i <= 48; i++) {
            float a = i * Mth.TWO_PI / 48;
            base[i] = new Vector3f(Mth.cos(a) * radius, 0, Mth.sin(a) * radius);
            hh[i] = (1.4f + .8f * noise(a * 3, age * .15f, seed)) * alive;
            col[i] = argb(0xFFFFFF, .95f * alive);
        }
        FireMesh.curtain(walls, p, base, hh, col, 1.3f, -age * .02f, light);
        // Tongues streaming up it in a whirl
        VertexConsumer flames = FireMesh.flames(buffer, tier);
        p = poseStack.last();
        for (int k = 0; k < 70; k++) {
            float t = (age * .05f + hash(seed + k * 3)) % 1;
            float a = hash(seed + k * 5) * Mth.TWO_PI + spin * 2 + t * 5;
            float r = radius * (.3f + .8f * t) * (.6f + .5f * hash(seed + k * 7));
            float y = h * t;
            float th = (1.2f + 1.6f * hash(seed + k)) * (1 - t * .4f) * (1 + .4f * flare);
            FireMesh.tongue(flames, p, eye, Mth.cos(a) * r, y, Mth.sin(a) * r, th * .6f, th, 0, FireMesh.frame(age, k), argb(0xFFFFFF, (1 - t * t) * alive), light);
        }
        // Smoke only while it burns cool
        VertexConsumer smoke = WaterMesh.translucent(buffer, FireMesh.SMOKE);
        float soot = Math.max(0, 1 - tier * .3f);
        for (int k = 0; k < 10; k++) {
            float t = (age * .02f + hash(seed + k * 11)) % 1;
            float a = hash(seed + k * 13) * Mth.TWO_PI + spin;
            puff(smoke, poseStack, view, Mth.cos(a) * radius * (.5f + t), h + 1 + 3 * t, Mth.sin(a) * radius * (.5f + t), radius * (1.4f + t), k, -1,
                    argb(0xC8C0BC, .35f * soot * Mth.sin(t * Mth.PI) * alive), light);
        }
        // Sparks whirling up
        VertexConsumer sparks = FireMesh.light(buffer, FireMesh.SPARK);
        p = poseStack.last();
        for (int k = 0; k < 60; k++) {
            float t = (age * .06f + hash(seed + k * 17)) % 1;
            float a = hash(seed + k * 19) * Mth.TWO_PI + spin * 3 + t * 7;
            float r = radius * (.4f + 1.1f * t);
            Vector3f head = new Vector3f(Mth.cos(a) * r, h * 1.1f * t, Mth.sin(a) * r);
            Vector3f tail = new Vector3f(Mth.cos(a - .25f) * r, head.y - .5f, Mth.sin(a - .25f) * r);
            streak(sparks, p, eye, head, tail, .1f, glow(FireMesh.core(tier), (1 - t) * alive), light);
        }
    }
}
