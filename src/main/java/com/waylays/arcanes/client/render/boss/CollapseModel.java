package com.waylays.arcanes.client.render.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.sand.SandMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Collapse: a black hole opens over the spot. Its horizon is a ball of utter black ringed by a thin burning ring of
 * light; round it a disc of matter swirls, white-hot within and the attack's colour without, its far side bent up over
 * the top of the horizon; rocks torn from the ground spiral in on threads of light, stretched and swallowed. Then it
 * falls in on itself to a point of light, and bursts: a white shell racing out, rays, a ring over the ground.
 */
public final class CollapseModel {
    private static final ResourceLocation ROCK = ArcanesOublies.id("textures/entity/boss/m_obsidian.png");
    private static final float HORIZON = 2.2f;
    private static final int ROCKS = 26;

    private CollapseModel() {}

    /** Over its spot (the pose's origin) at {@code from}; it falls in {@code hit} ticks ago (negative before), the blast eight ticks later; it pulls from {@code pull}. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f from, float age, float hit, float pull, int zone, int seed,
                            int light) {
        float open = BossMesh.ramp(age, 0, 20);
        // Shrinking to nothing as it falls in, gone in the blast
        float fall = BossMesh.ramp(hit, -4, 2);
        float k = open * (1 - fall);
        float blast = BossMesh.ramp(hit, 8, 24);
        float boom = hit >= 8 ? 1 - blast : 0;
        Vector3f c = new Vector3f(from);
        float r = HORIZON * (.4f + .6f * open) * (1 - fall * .98f);
        float spin = age * .09f + (hit > -10 && hit < 2 ? (hit + 10) * (hit + 10) * .01f : 0);

        if (k > .01f) {
            // ---- the horizon: utter black
            VertexConsumer dark = BossMesh.solid(buffer, ROCK);
            poseStack.pushPose();
            poseStack.translate(c.x, c.y, c.z);
            Sculpt.lathe(dark, poseStack.last(), BellModel.sphere(r, 16), 24, 1, 1, 0xFF000000, 0xFF000000, light);
            poseStack.popPose();
            // ---- rocks torn up and spiralling in
            for (int i = 0; i < ROCKS; i++) {
                Vector3f at = rock(i, age, pull, c, seed);
                if (at == null) {
                    continue;
                }
                float d = at.distance(c);
                float big = (.18f + BossMesh.hash(seed + i * 11) * .4f) * Mth.clamp((d - r) / 3, 0, 1);
                if (big < .02f) {
                    continue;
                }
                poseStack.pushPose();
                poseStack.translate(at.x, at.y, at.z);
                poseStack.mulPose(new Quaternionf().rotationXYZ(age * .2f + i, age * .13f, i * 2.1f));
                // Stretched toward the hole as they near it
                poseStack.scale(big, big * (1 + 2 / Math.max(d, 1)), big);
                Sculpt.gem(dark, poseStack.last(), 1, .8f, 1, 5, 0xFF8A8090, light);
                poseStack.popPose();
            }
        }

        // ---- light: the disc, its bent far side, the burning ring, the threads of the rocks; the fall and the blast
        VertexConsumer swirl = BossMesh.light(buffer, BossMesh.SWIRL);
        if (k > .01f) {
            Quaternionf tilt = new Quaternionf().rotationX(.38f).rotateZ(.12f);
            Vector3f u = tilt.transform(new Vector3f(1, 0, 0)), v = tilt.transform(new Vector3f(0, 0, 1));
            int hot = BossMesh.glow(BossMesh.mix(0xFFFFFF, 0xFFE0A0, .3f), k), mid = BossMesh.glow(BossMesh.mix(zone, 0xFF60C0, .3f), k), far = BossMesh.glow(zone, 0);
            for (int layer = 0; layer < 3; layer++) {
                BossMesh.annulus(swirl, poseStack.last(), c, u, v, r * 1.15f, r * 2.4f, 64, 2 + layer, -spin * (1 + layer * .3f), spin * .3f + layer * .3f, hot, mid, light,
                        true);
            }
            BossMesh.annulus(swirl, poseStack.last(), c, u, v, r * 2.4f, r * 4.6f * (.6f + .4f * open), 64, 3, -spin * .6f, spin * .2f, mid, far, light, true);
            BossMesh.annulus(swirl, poseStack.last(), c, u, v, r * 2.2f, r * 3.6f * (.6f + .4f * open), 64, 4, -spin * .8f, spin * .25f + .5f, mid, far, light, true);
            // The far side, bent up over the top: a ring always facing the eye
            Vector3f toEye = new Vector3f(view.eye()).sub(c).normalize();
            Vector3f eu = BossMesh.across(toEye), ev = new Vector3f(toEye).cross(eu).normalize();
            BossMesh.annulus(swirl, poseStack.last(), c, eu, ev, r * 1.05f, r * 1.9f, 48, 1, spin * .5f, 0, hot, BossMesh.glow(zone, .5f * k), light, false);
        }
        VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
        if (k > .01f) {
            RitualMesh.billboard(ring, poseStack, view, c, r * 2.25f, spin, -1, BossMesh.glow(0xFFFFFF, k), light);
            RitualMesh.billboard(ring, poseStack, view, c, r * 2.6f, -spin, -1, BossMesh.glow(zone, .8f * k), light);
        }
        // The blast's ring racing over the ground
        if (hit >= 8 && blast < 1) {
            BossMesh.pool(ring, poseStack.last(), 0, .15f, 0, pull * 2.4f * blast + 2, 0, BossMesh.glow(zone, boom), light);
            BossMesh.pool(ring, poseStack.last(), 0, .2f, 0, pull * 1.8f * blast + 1, 1, BossMesh.glow(0xFFFFFF, boom * .8f), light);
        }
        VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
        if (k > .01f) {
            for (int i = 0; i < ROCKS; i += 2) {
                Vector3f[] trail = new Vector3f[6];
                boolean ok = true;
                for (int j = 0; j < 6; j++) {
                    trail[j] = rock(i, age - j * 1.5f, pull, c, seed);
                    if (trail[j] == null) {
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    SandMesh.ribbon(beam, poseStack.last(), view.eye(), trail, Sculpt.taper(.25f, .02f, 6, 1), 1, 0, Sculpt.fade(BossMesh.glow(zone, .7f * k), BossMesh.glow(zone, 0), 6),
                            light);
                }
            }
        }
        // Rays of the blast
        if (hit >= 8 && blast < 1) {
            for (int i = 0; i < 18; i++) {
                float a = BossMesh.hash(seed + i * 5) * Mth.TWO_PI, b = (BossMesh.hash(seed + i * 9) - .5f) * 2.2f;
                Vector3f dir = new Vector3f(Mth.cos(a) * Mth.cos(b), Mth.sin(b), Mth.sin(a) * Mth.cos(b));
                Vector3f tip = new Vector3f(c).add(new Vector3f(dir).mul(pull * 1.4f * (.3f + blast)));
                RitualMesh.shaft(beam, poseStack.last(), view.eye(), new Vector3f(c).add(new Vector3f(dir).mul(pull * .3f * blast)), tip, 1.2f * boom + .2f,
                        BossMesh.glow(i % 3 == 0 ? 0xFFFFFF : zone, boom), light);
            }
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        if (k > .01f) {
            RitualMesh.billboard(glow, poseStack, view, c, r * 7, 0, -1, BossMesh.glow(zone, .3f * k), light);
        }
        // Falling in: a point of light
        if (hit > -4 && hit < 9) {
            float pt = hit < 2 ? BossMesh.ramp(hit, -4, 2) : 1 - BossMesh.ramp(hit, 6, 9);
            RitualMesh.billboard(glow, poseStack, view, c, 1.5f + 3 * pt, 0, -1, BossMesh.glow(0xFFFFFF, pt), light);
        }
        if (hit >= 8 && blast < 1) {
            RitualMesh.billboard(glow, poseStack, view, c, pull * 2.6f * (.2f + blast), 0, -1, BossMesh.glow(0xFFFFFF, boom * .9f), light);
            RitualMesh.billboard(glow, poseStack, view, c, pull * 3.4f * (.25f + blast), 0, -1, BossMesh.glow(zone, boom), light);
        }
    }

    /** Where rock {@code i} is at {@code t}: torn up from the ground round the spot, spiralling in to the horizon; null once swallowed or before it rises. */
    private static Vector3f rock(int i, float t, float pull, Vector3f c, int seed) {
        float start = BossMesh.hash(seed + i * 3) * 30;
        float life = 26 + BossMesh.hash(seed + i * 7) * 14;
        float s = (t - start) % life;
        if (t < start || s < 0) {
            return null;
        }
        float f = s / life;
        float r0 = pull * (.55f + .4f * BossMesh.hash(seed + i * 13));
        float rad = r0 * (1 - f) * (1 - f) + HORIZON * .9f * f;
        float a = BossMesh.hash(seed + i * 17) * Mth.TWO_PI + f * f * 9;
        float y = Mth.lerp(f * (2 - f), 0, c.y) + Mth.sin(f * Mth.PI) * 1.2f;
        return new Vector3f(c.x + Mth.cos(a) * rad, y, c.z + Mth.sin(a) * rad);
    }
}
