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

/**
 * Razor Petals: one great lotus petal flying point first, spinning on itself like a thrown blade, its edge burning
 * pink-white, three ghosts of it fading behind and a wake of tiny petals. Where it strikes it falls apart into small
 * petals that whirl away, with a flash.
 */
public final class PetalModel {
    private PetalModel() {}

    /** At the pose's origin, flying along {@code dir}; once it has struck ({@code done}), {@code since} ticks ago. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f dir, float age, boolean done, float since, int seed,
                            int light) {
        Vector3f d = new Vector3f(dir);
        if (d.lengthSquared() < 1.0E-6f) {
            d.set(0, 0, 1);
        }
        d.normalize();
        int tip = BossMesh.mix(LotusMesh.TIP, LotusMesh.PINK, BossMesh.hash(seed) * .6f);
        if (!done) {
            flying(poseStack, buffer, view, d, age, tip, seed, light);
        } else {
            burst(poseStack, buffer, view, d, since, tip, seed, light);
        }
    }

    private static void flying(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f d, float age, int tip, int seed, int light) {
        float grow = BossMesh.ramp(age, 0, 3);
        float spin = age * .9f + seed;
        Vector3f side = BossMesh.across(d);
        Quaternionf base = LotusMesh.frame(d, new Vector3f(side).rotateAxis(spin, d.x, d.y, d.z));
        LotusMesh.Petal petal = LotusMesh.petal(1.05f, .64f, 0, .3f, -.12f).moved(base, new Vector3f(d).mul(-.55f * grow), grow);
        PoseStack.Pose p = poseStack.last();
        petal.draw(BossMesh.solid(buffer, LotusMesh.PETAL), p, BossMesh.argb(0xFFF6F8, 1), BossMesh.argb(tip, 1), light);

        // Its ghosts behind it, each turned back a little on the spin
        VertexConsumer ghost = BossMesh.light(buffer, LotusMesh.PETAL);
        for (int k = 1; k <= 3; k++) {
            Quaternionf q = LotusMesh.frame(d, new Vector3f(side).rotateAxis(spin - k * .45f, d.x, d.y, d.z));
            LotusMesh.Petal g = LotusMesh.petal(1.05f, .64f, 0, .3f, -.12f).moved(q, new Vector3f(d).mul(-.55f - k * .42f), grow * (1 - k * .12f));
            LotusMesh.ghost(ghost, p, g, BossMesh.glow(tip, .32f / k), light);
        }
        // Its edge burning
        VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
        petal.rim(beam, p, view.eye(), .07f, BossMesh.glow(0xFFE6F2, .9f), light);
        RitualMesh.shaft(beam, p, view.eye(), new Vector3f(d).mul(-.4f), new Vector3f(d).mul(-2.6f), .55f, BossMesh.glow(tip, .35f), light);
        // A wake of tiny petals, twirling off behind
        VertexConsumer small = BossMesh.solid(buffer, LotusMesh.PETAL);
        Vector3f up = new Vector3f(side).cross(d).normalize();
        for (int k = 0; k < 5; k++) {
            float back = 1 + k * .45f + (age * .4f % .45f);
            float a = age * .5f + k * 2.1f;
            Vector3f at = new Vector3f(d).mul(-back).add(new Vector3f(side).mul(Mth.cos(a) * .35f)).add(new Vector3f(up).mul(Mth.sin(a) * .35f));
            Quaternionf q = LotusMesh.frame(new Vector3f(side).rotateAxis(a * 1.7f, d.x, d.y, d.z), d);
            float s = .22f * (1 - k * .15f);
            LotusMesh.petal(1, .62f, .2f, .5f, .2f, 3, 3).moved(q, at, s).draw(small, p, BossMesh.argb(0xFFF6F8, 1), BossMesh.argb(tip, 1), light);
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(d).mul(.1f), 1.6f, 0, -1, BossMesh.glow(tip, .3f), light);
    }

    private static void burst(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f d, float since, int tip, int seed, int light) {
        float t = Mth.clamp(since / 10f, 0, 1);
        if (t >= 1) {
            return;
        }
        PoseStack.Pose p = poseStack.last();
        VertexConsumer solid = BossMesh.solid(buffer, LotusMesh.PETAL);
        for (int k = 0; k < 7; k++) {
            float a = k * Mth.TWO_PI / 7 + BossMesh.hash(seed + k) * .6f;
            Vector3f out = new Vector3f(Mth.cos(a), .5f + BossMesh.hash(seed + k * 3) * .8f, Mth.sin(a)).normalize();
            Vector3f at = new Vector3f(out).mul(.2f + 1.6f * (1 - (1 - t) * (1 - t))).add(0, -1.2f * t * t, 0);
            Quaternionf q = LotusMesh.frame(new Vector3f(out).rotateY(since * .5f + k), new Vector3f(0, 1, 0).rotateX(since * .4f + k));
            float s = .34f * (1 - t * .7f);
            LotusMesh.petal(1, .62f, .2f, .5f, .2f, 4, 3).moved(q, at, s).draw(solid, p, BossMesh.argb(0xFFF6F8, 1), BossMesh.argb(tip, 1), light);
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        float flash = (1 - t) * (1 - t);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(), .6f + 2.4f * t, 0, -1, BossMesh.glow(0xFFFFFF, flash), light);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(), 1.5f + 3 * t, 0, -1, BossMesh.glow(tip, flash * .7f), light);
    }
}
