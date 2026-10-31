package com.waylays.arcanes.client.render.water;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import static com.waylays.arcanes.client.render.water.WaterMesh.*;

/**
 * Typhoon, drawn as a real waterspout: a rope of grey water and spray hanging from a dark rotating storm cloud, bending
 * as it sways, thin in its middle and flaring into the cloud at its top; spray and mist streaming up round it in a
 * spiral; at its foot a wide skirt of white spray thrown out round and round, the water under it churned to foam;
 * rain falling in curtains all round; lightning in the cloud. It lowers itself out of the cloud as it forms and lifts
 * back into it, thinning, as it breaks up.
 */
public final class TyphoonModel {
    private static final int POINTS = 24;

    private TyphoonModel() {}

    /**
     * {@code formed} (0 to 1) how far it has come down out of the cloud, {@code gone} (0 to 1) how far it has broken
     * up; {@code flash} a lightning flash now (its strength, 0 for none), {@code bolt} which flash it is.
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, View view, float age, float formed, float gone, float flash, int bolt,
                            float height, float radius, int seed, int light) {
        float alive = 1 - gone;
        float base = height * (1 - formed) + height * .55f * gone;
        Vector3f[] axis = new Vector3f[POINTS];
        float[] r = new float[POINTS];
        for (int i = 0; i < POINTS; i++) {
            float t = (float) i / (POINTS - 1);
            float y = Mth.lerp(t, base, height);
            float u = y / height;
            float sway = 1.7f * u * (1 - u) * 3.2f;
            axis[i] = new Vector3f(sway * Mth.sin(u * 2.6f + age * .035f + seed), y, sway * Mth.cos(u * 2.1f + age * .028f + seed * 1.7f));
            r[i] = radius * (.16f + .1f * (float) Math.pow(1 - u, 6) + .85f * (float) Math.pow(u, 4)) * (1 - .5f * gone) * (1 + .1f * Mth.sin(u * 9 - age * .4f));
        }
        float spin = age * .25f;

        // The rope of water itself, and its sheath of spray
        VertexConsumer water = translucent(buffer, WATER);
        PoseStack.Pose p = poseStack.last();
        float[] core = new float[POINTS], sheath = new float[POINTS];
        int[] coreC = new int[POINTS], sheathC = new int[POINTS];
        for (int i = 0; i < POINTS; i++) {
            float u = (float) i / (POINTS - 1);
            core[i] = r[i] * .7f;
            sheath[i] = r[i];
            float top = Mth.clamp((1 - u) * 5, 0, 1), bottom = Mth.clamp(u * 8 + (formed >= 1 ? 1 : 0), 0, 1);
            coreC[i] = argb(mix(0x7D8C9C, SHALLOW, .3f), .8f * top * bottom * alive);
            sheathC[i] = argb(mix(0xC9D6E0, FOAM_WHITE, .4f), .5f * top * bottom * alive);
        }
        tube(water, p, axis, core, coreC, 12, .3f, 1.2f, age * .7f, .25f, 3, seed, light, .5f);
        VertexConsumer foam = translucent(buffer, FOAM);
        p = poseStack.last();
        tube(foam, p, axis, sheath, sheathC, 14, .55f, .9f, age * 1.1f, .3f, -2.2f, seed + 17, light, .8f);
        // The water churned to foam at its foot
        if (formed > .9f) {
            disc(foam, p, .06f, 0, radius * 1.6f, 32, 2.5f, Mth.cos(spin * .2f) * .5f, Mth.sin(spin * .2f) * .5f, argb(FOAM_WHITE, .7f * alive),
                    argb(FOAM_WHITE, 0), light);
        }

        // Spray streaming up round it, the skirt at its foot, the storm cloud
        VertexConsumer spray = translucent(buffer, SPRAY);
        for (int k = 0; k < 36; k++) {
            float t = (age * .03f + hash(seed + k * 7)) % 1;
            int i = Math.min(POINTS - 1, (int) (t * (POINTS - 1)));
            float a = hash(seed + k * 3) * Mth.TWO_PI + spin * (1.4f - t);
            float out = r[i] * (1.05f + .3f * hash(seed + k * 5));
            Vector3f c = new Vector3f(axis[i]).add(Mth.cos(a) * out, 0, Mth.sin(a) * out);
            puff(spray, poseStack, view, c.x, c.y, c.z, r[i] * 1.1f + .5f, a, k & 3, argb(0xEEF4F8, .6f * Mth.sin(t * Mth.PI) * alive), light);
        }
        if (formed > .9f) {
            for (int k = 0; k < 30; k++) {
                float t = (age * .05f + hash(seed + k * 11)) % 1;
                float a = hash(seed + k * 13) * Mth.TWO_PI + spin * .6f + t * 1.5f;
                float out = radius * (.4f + 1.3f * t) * (.8f + .4f * hash(seed + k));
                float y = 2.6f * Mth.sin(t * Mth.PI * .9f) * (1 - .4f * t);
                puff(spray, poseStack, view, Mth.cos(a) * out, y, Mth.sin(a) * out, 1.2f + 1.6f * t, a + age * .05f, k & 3,
                        argb(FOAM_WHITE, .8f * (1 - t) * alive), light);
            }
        }

        VertexConsumer cloud = translucent(buffer, CLOUD);
        float glow = flash;
        for (int k = 0; k < 44; k++) {
            float a = hash(seed + k * 17) * Mth.TWO_PI + age * .012f * (k % 2 == 0 ? 1 : .7f);
            float out = radius * (.4f + 2.6f * (float) Math.sqrt(hash(seed + k * 19)));
            float y = height + .6f + (hash(seed + k * 23) - .3f) * 1.8f + .3f * Mth.sin(age * .03f + k);
            float under = Mth.clamp((height + 1.2f - y) / 2, 0, 1);
            int shade = mix(mix(0xA9B2BE, 0x4A5260, under), 0xE8ECFF, glow * (.5f + .5f * under));
            float size = (3 + 2.5f * hash(seed + k * 29)) * (.6f + .4f * Math.max(formed, .5f)) * (1 - .3f * gone);
            puff(cloud, poseStack, view, Mth.cos(a) * out, y, Mth.sin(a) * out, size, k + age * .004f, -1, argb(shade, .92f * Math.max(alive, .3f)), light);
        }

        // Mist hanging round its foot
        VertexConsumer mist = translucent(buffer, MIST);
        if (formed > .9f) {
            for (int k = 0; k < 12; k++) {
                float t = (age * .02f + hash(seed + k * 31)) % 1;
                float a = hash(seed + k * 37) * Mth.TWO_PI + spin * .4f;
                float out = radius * (.6f + 1.6f * hash(seed + k * 41));
                puff(mist, poseStack, view, Mth.cos(a) * out, .8f + 2.5f * t, Mth.sin(a) * out, 3 + 3 * t, k + age * .01f, -1,
                        argb(0xEEF4FA, .35f * Mth.sin(t * Mth.PI) * alive), light);
            }
        }

        // Rain in curtains all round, drops spiralling up round it, spray flung off its skirt
        VertexConsumer drops = translucent(buffer, STREAK);
        p = poseStack.last();
        Vector3f eye = view.eye();
        for (int k = 0; k < 90; k++) {
            float t = (age * .08f + hash(seed + k * 43)) % 1;
            float a = hash(seed + k * 47) * Mth.TWO_PI + age * .004f;
            float out = radius * (1.2f + 2.4f * hash(seed + k * 53));
            float y = height * (1 - t);
            Vector3f head = new Vector3f(Mth.cos(a) * out - .4f * t, y, Mth.sin(a) * out);
            Vector3f tail = new Vector3f(head).add(.15f, 1.3f, 0);
            streak(drops, p, eye, head, tail, .06f, argb(0xB8C8DA, .45f * Math.min(1, t * 4) * Math.max(alive, .2f)), light);
        }
        for (int k = 0; k < 40; k++) {
            float t = (age * .04f + hash(seed + k * 59)) % 1;
            int i = Math.min(POINTS - 1, (int) (t * (POINTS - 1)));
            float a = hash(seed + k * 61) * Mth.TWO_PI + spin * 2 + t * 6;
            float out = r[i] * 1.3f + .3f;
            Vector3f head = new Vector3f(axis[i]).add(Mth.cos(a) * out, 0, Mth.sin(a) * out);
            Vector3f tail = new Vector3f(axis[i]).add(Mth.cos(a - .35f) * out, -.5f, Mth.sin(a - .35f) * out);
            streak(drops, p, eye, head, tail, .1f, argb(FOAM_WHITE, .85f * Mth.sin(t * Mth.PI) * alive), light);
        }
        if (formed > .9f) {
            for (int k = 0; k < 30; k++) {
                float t = (age * .1f + hash(seed + k * 67)) % 1;
                float a = hash(seed + k * 71) * Mth.TWO_PI + spin;
                Vector3f from = new Vector3f(Mth.cos(a) * radius * .5f, .3f, Mth.sin(a) * radius * .5f);
                Vector3f v = new Vector3f(-Mth.sin(a) * .25f + Mth.cos(a) * .12f, .18f, Mth.cos(a) * .25f + Mth.sin(a) * .12f);
                Vector3f head = thrown(from, v, t * 14, .03f);
                if (head.y < 0) {
                    continue;
                }
                streak(drops, p, eye, head, thrown(from, v, Math.max(0, t * 14 - 2), .03f), .1f, argb(FOAM_WHITE, .85f * (1 - t) * alive), light);
            }
        }

        // Lightning in the cloud, striking down outside the spout
        if (flash > 0) {
            VertexConsumer core2 = additive(buffer, BOLT);
            p = poseStack.last();
            float a = hash(bolt * 7 + seed) * Mth.TWO_PI;
            float out = radius * (1.6f + 1.4f * hash(bolt * 11 + seed));
            Vector3f at = new Vector3f(Mth.cos(a) * out, height + .3f, Mth.sin(a) * out);
            int segs = 12;
            for (int j = 0; j < segs; j++) {
                float drop = height / segs;
                Vector3f next = new Vector3f(at).add((hash(bolt * 31 + j) - .5f) * 1.6f, -drop * (.7f + .6f * hash(bolt * 37 + j)), (hash(bolt * 41 + j) - .5f) * 1.6f);
                if (next.y < 0) {
                    next.y = 0;
                }
                streak(core2, p, eye, at, next, .22f, glow(0xDDE6FF, flash), light);
                streak(core2, p, eye, at, next, .9f, glow(0x6F86FF, flash * .35f), light);
                at = next;
                if (at.y <= 0) {
                    break;
                }
            }
        }
    }
}
