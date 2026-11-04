package com.waylays.arcanes.client.render.lotus;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * Lotus Lanterns: a lantern shaped as a half-open lotus, its petals thin as paper and lit from within, set on a little
 * leaf, a flame dancing in its heart; it bobs as it drifts, sparks trailing under it. When it reaches its foe it bursts
 * into a flower of fire: petals of flame flung out, a flash, a ring.
 */
public final class LanternModel {
    private static final float SIZE = .62f;

    private LanternModel() {}

    /** At the pose's origin; once it has burst ({@code done}), {@code since} ticks ago. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int life, boolean done, float since, int index, int seed,
                            int light) {
        if (done) {
            burst(poseStack, buffer, view, since, seed, light);
            return;
        }
        float grow = BossMesh.ramp(age, 0, 8);
        float s = SIZE * grow;
        float flicker = .85f + .1f * Mth.sin(age * 1.9f + index) + .05f * Mth.sin(age * 4.3f + index * 2);
        Vector3f at = new Vector3f(0, -.35f + .08f * Mth.sin(age * .12f + index), 0);
        float sway = .12f * Mth.sin(age * .07f + index * 1.3f);

        // ---- its leaf, beneath
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        poseStack.mulPose(new Quaternionf().rotationZ(sway).rotateX(sway * .6f));
        LotusMesh.leaf(BossMesh.solid(buffer, LotusMesh.LEAF), poseStack.last(), new Vector3f(0, -.02f, 0), .55f * s / SIZE * .8f, index, .2f, seed,
                BossMesh.argb(0xFFFFFF, 1), light);
        // ---- its paper petals, lit from within: warm at the root, pink at the tip
        poseStack.scale(s, s, s);
        PoseStack.Pose p = poseStack.last();
        List<LotusMesh.Petal> petals = LotusMesh.petals(LotusMesh.LOTUS, .48f, index * .7f + age * .004f, age, seed);
        VertexConsumer paper = BossMesh.see(buffer, LotusMesh.PETAL);
        for (LotusMesh.Petal pt : petals) {
            pt.draw(paper, p, BossMesh.argb(0xFFF4D8, .8f), BossMesh.argb(0xFFC2DC, .72f), light);
        }
        // The glow showing through them
        VertexConsumer through = BossMesh.light(buffer, LotusMesh.PETAL);
        for (LotusMesh.Petal pt : petals) {
            pt.draw(through, p, BossMesh.glow(0xFFC070, .6f * flicker), BossMesh.glow(0xFF90C0, .25f * flicker), light);
        }
        poseStack.popPose();

        // ---- the flame in its heart, and its warm halo
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        Vector3f flame = new Vector3f(at).add(0, .32f * s / SIZE, 0);
        RitualMesh.billboard(glow, poseStack, view, flame, .5f * grow * flicker, 0, -1, BossMesh.glow(0xFFF4D0, .95f), light);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(flame).add(0, .08f, 0), .9f * grow * flicker, 0, -1, BossMesh.glow(0xFF9A40, .7f), light);
        RitualMesh.billboard(glow, poseStack, view, flame, 3.2f * grow, 0, -1, BossMesh.glow(0xFFA060, .35f * flicker), light);
        // Sparks trailing beneath it
        VertexConsumer mote = BossMesh.light(buffer, BossMesh.MOTE);
        for (int k = 0; k < 6; k++) {
            float t = ((age * .05f + k / 6f) % 1);
            Vector3f m = new Vector3f(at).add(.25f * Mth.sin(k * 2.3f + age * .1f), -t * 1.2f, .25f * Mth.cos(k * 1.7f + age * .1f));
            RitualMesh.billboard(mote, poseStack, view, m, .12f, 0, -1, BossMesh.glow(k % 2 == 0 ? 0xFFC060 : LotusMesh.PINK, (1 - t) * .8f * grow), light);
        }
    }

    private static void burst(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float since, int seed, int light) {
        float t = Mth.clamp(since / 12f, 0, 1);
        if (t >= 1) {
            return;
        }
        PoseStack.Pose p = poseStack.last();
        // Petals of flame flung out
        VertexConsumer fire = BossMesh.light(buffer, LotusMesh.PETAL);
        float out = 1 - (1 - t) * (1 - t);
        for (int k = 0; k < 12; k++) {
            float a = k * Mth.TWO_PI / 12 + BossMesh.hash(seed + k) * .3f;
            Vector3f dir = new Vector3f(Mth.cos(a), .35f + .5f * BossMesh.hash(seed + k * 5), Mth.sin(a)).normalize();
            Quaternionf q = LotusMesh.frame(dir, new Vector3f(0, 1, 0));
            int c = BossMesh.mix(0xFFC060, LotusMesh.PINK, t);
            LotusMesh.ghost(fire, p, LotusMesh.petal(1, .6f, 0, .4f, .2f, 5, 4).moved(q, new Vector3f(dir).mul(.3f + 2.2f * out), .8f * (1 - t * .5f)),
                    BossMesh.glow(c, .8f * (1 - t)), light);
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        float flash = (1 - t) * (1 - t);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(), 1 + 3 * t, 0, -1, BossMesh.glow(0xFFF4D0, flash), light);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(), 3 + 4 * t, 0, -1, BossMesh.glow(0xFF8040, .7f * flash), light);
        VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
        float rr = .5f + 3 * out;
        BossMesh.annulus(ring, p, new Vector3f(), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1), rr * .75f, rr, 32, 1, 0, 0, BossMesh.glow(0xFFC060, 0),
                BossMesh.glow(LotusMesh.PINK, .8f * (1 - t)), light, true);
    }
}
