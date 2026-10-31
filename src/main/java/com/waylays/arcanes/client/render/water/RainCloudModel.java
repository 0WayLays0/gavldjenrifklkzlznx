package com.waylays.arcanes.client.render.water;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import static com.waylays.arcanes.client.render.water.WaterMesh.*;

/**
 * Cleansing Rain: a small cumulus cloud riding over the caster, its flat base grey with rain, its billowing top white
 * in the sun; rain falling from its base in streaks; the ground under it wet and dimpled with rings where the drops
 * land, a little mist where they splash; and a faint rainbow arching behind it.
 */
public final class RainCloudModel {
    private static final int PUFFS = 26;
    private static final int RAIN = 110;
    private static final int RINGS = 16;

    private RainCloudModel() {}

    /** {@code thick} (0 to 1) how far it has gathered (or thinned away at its end); {@code radius} and {@code height} its size. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, View view, float age, float thick, float radius, float height, int seed, int light) {
        if (thick <= .01f) {
            return;
        }
        float r = radius;
        // The wet ground, rings spreading where drops land
        VertexConsumer water = translucent(buffer, WATER);
        PoseStack.Pose p = poseStack.last();
        disc(water, p, .03f, 0, r * .95f, 28, 3, age * .005f, 0, argb(water(.85f), .32f * thick), argb(water(.85f), 0), light);
        VertexConsumer foam = translucent(buffer, FOAM);
        p = poseStack.last();
        for (int k = 0; k < RINGS; k++) {
            float t = (age * .07f + hash(seed + k * 11)) % 1;
            int cycle = (int) (age * .07f + hash(seed + k * 11));
            float a = hash(seed + k * 13 + cycle * 7) * Mth.TWO_PI;
            float d = Mth.sqrt(hash(seed + k * 17 + cycle * 5)) * r * .85f;
            poseStack.pushPose();
            poseStack.translate(Mth.cos(a) * d, 0, Mth.sin(a) * d);
            disc(foam, poseStack.last(), .05f, .05f + .32f * t, .12f + .4f * t, 12, 1, 0, 0, argb(FOAM_WHITE, .6f * (1 - t) * thick), argb(FOAM_WHITE, 0), light);
            poseStack.popPose();
        }

        // The rainbow, arching behind the cloud as seen from the eye
        Vector3f eye = view.eye();
        Vector3f away = new Vector3f(-eye.x, 0, -eye.z);
        if (away.lengthSquared() < .01f) {
            away.set(0, 0, 1);
        }
        away.normalize();
        Vector3f across = new Vector3f(-away.z, 0, away.x);
        VertexConsumer bow = translucent(buffer, RAINBOW);
        p = poseStack.last();
        int n = 24;
        Vector3f[] arc = new Vector3f[n], side = new Vector3f[n];
        float[] w = new float[n];
        int[] col = new int[n];
        float br = r * 1.5f;
        for (int i = 0; i < n; i++) {
            float a = Mth.PI * i / (n - 1);
            arc[i] = new Vector3f(away).mul(r * 1.6f).add(new Vector3f(across).mul(Mth.cos(a) * br)).add(0, Mth.sin(a) * br * .9f, 0);
            side[i] = new Vector3f(across).mul(Mth.cos(a)).add(0, Mth.sin(a), 0).normalize();
            w[i] = 1.1f;
            col[i] = argb(0xFFFFFF, .5f * thick * Mth.clamp(Mth.sin(a) * 2.5f, 0, 1));
        }
        ribbon(bow, p, arc, side, w, col, .1f, 0, light, true);

        // The cloud: a flat grey base, billowing white top
        VertexConsumer cloud = translucent(buffer, CLOUD);
        for (int k = 0; k < PUFFS; k++) {
            float a = hash(seed + k * 5) * Mth.TWO_PI + age * .003f * (k % 2 == 0 ? 1 : -1);
            float d = Mth.sqrt(hash(seed + k * 5 + 1)) * r * .7f;
            float lift = hash(seed + k * 5 + 2);
            float dome = 1 - (d / (r * .7f)) * (d / (r * .7f));
            float y = height + lift * 1.6f * dome + .1f * Mth.sin(age * .04f + k);
            float size = (2 + 1.6f * hash(seed + k * 5 + 3)) * (.5f + .5f * thick) * (.7f + .5f * dome);
            float low = Mth.clamp((height + .9f - y) / 1.2f, 0, 1);
            int shade = mix(0xF6F8FC, 0x7C8798, low);
            puff(cloud, poseStack, view, Mth.cos(a) * d, y, Mth.sin(a) * d, size, k * 1.3f + age * .002f, -1, argb(shade, .95f * thick), light);
        }
        // Mist where the drops splash
        VertexConsumer mist = translucent(buffer, MIST);
        for (int k = 0; k < 6; k++) {
            float t = (age * .02f + hash(seed + k * 29)) % 1;
            float a = hash(seed + k * 31) * Mth.TWO_PI;
            float d = r * .6f * hash(seed + k * 37);
            puff(mist, poseStack, view, Mth.cos(a) * d, .4f + .5f * t, Mth.sin(a) * d, 1.6f + 1.2f * t, k, -1, argb(0xEEF6FF, .18f * Mth.sin(t * Mth.PI) * thick), light);
        }

        // Rain falling in streaks
        VertexConsumer drops = translucent(buffer, STREAK);
        p = poseStack.last();
        for (int k = 0; k < RAIN * thick; k++) {
            float a = hash(seed + k * 3) * Mth.TWO_PI;
            float d = Mth.sqrt(hash(seed + k * 3 + 1)) * r * .85f;
            float t = (age * (.09f + .03f * hash(seed + k)) + hash(seed + k * 7)) % 1;
            float y = (height - .2f) * (1 - t);
            Vector3f head = new Vector3f(Mth.cos(a) * d, y, Mth.sin(a) * d);
            Vector3f tail = new Vector3f(head).add(.05f, .9f, 0);
            streak(drops, p, eye, head, tail, .05f, argb(0xD6E8FA, .7f * thick), light);
        }
    }
}
