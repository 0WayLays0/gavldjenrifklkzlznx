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
 * Lotus Throne: a great lotus wide open under the caster's feet, turning slowly on a pool of pink light; behind the
 * caster's head a halo, a ring of light with a crown of light petals round it; small petals circling the caster. When it
 * looses a petal, the petal flies off the flower spinning to its target and bursts there.
 */
public final class ThroneModel {
    /** A petal flies to its target in this many ticks. */
    private static final float FLIGHT = 6;

    private ThroneModel() {}

    /** Under the caster (the pose's origin), who faces {@code dir}; its last petal loosed {@code since} ticks ago at {@code target} (or null). */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f dir, float age, int life, float since, Vector3f target,
                            int seed, int light) {
        float grow = BossMesh.ramp(age, 0, 12);
        float fade = 1 - BossMesh.ramp(age, life - 15, life);
        float k = grow * fade;
        if (k <= .01f) {
            return;
        }
        PoseStack.Pose p = poseStack.last();
        // ---- the pool of light it turns on
        VertexConsumer pool = BossMesh.light(buffer, BossMesh.GLOW);
        BossMesh.pool(pool, p, 0, .06f, 0, 4.2f * k, age * .01f, BossMesh.glow(LotusMesh.PINK, .35f * k), light);
        BossMesh.pool(pool, p, 0, .07f, 0, 2.2f * k, -age * .02f, BossMesh.glow(LotusMesh.GOLD, .3f * k), light);
        VertexConsumer mandala = BossMesh.light(buffer, LotusMesh.MANDALA);
        BossMesh.pool(mandala, p, 0, .08f, 0, 4.6f * k, age * .008f, BossMesh.glow(LotusMesh.PINK, .3f * k), light);

        // ---- the throne itself
        LotusMesh.bloom(poseStack, buffer, view, LotusMesh.LOTUS, new Vector3f(0, -.12f, 0), 1.45f * k, .9f + .04f * Mth.sin(age * .05f), age * .008f, age,
                BossMesh.argb(0xFFF8FA, 1), BossMesh.argb(LotusMesh.TIP, 1), .35f, LotusMesh.PINK, seed, light);
        // Leaves spread under it
        VertexConsumer leaf = BossMesh.solid(buffer, LotusMesh.LEAF);
        for (int i = 0; i < 3; i++) {
            float a = i * Mth.TWO_PI / 3 + .5f - age * .004f;
            LotusMesh.leaf(leaf, p, new Vector3f(Mth.cos(a) * 1.25f * k, -.02f, Mth.sin(a) * 1.25f * k), .85f * k, a, .1f, seed + i, BossMesh.argb(0xFFFFFF, 1),
                    light);
        }

        // ---- the halo behind the caster's head
        Vector3f f = new Vector3f(dir.x, 0, dir.z);
        if (f.lengthSquared() < 1.0E-6f) {
            f.set(0, 0, 1);
        }
        f.normalize();
        Vector3f side = new Vector3f(f.z, 0, -f.x);
        Vector3f up = new Vector3f(0, 1, 0);
        Vector3f halo = new Vector3f(f).mul(-.42f).add(0, 1.62f, 0);
        VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
        BossMesh.annulus(ring, p, halo, side, up, .34f * k, .46f * k, 40, 1, 0, 0, BossMesh.glow(LotusMesh.GOLD, .9f * k), BossMesh.glow(LotusMesh.GOLD, .2f * k),
                light, true);
        VertexConsumer ghost = BossMesh.light(buffer, LotusMesh.PETAL);
        for (int i = 0; i < 12; i++) {
            float a = i * Mth.TWO_PI / 12 + age * .015f;
            Vector3f out = new Vector3f(side).mul(Mth.cos(a)).add(new Vector3f(up).mul(Mth.sin(a)));
            Quaternionf q = LotusMesh.frame(out, f);
            float len = (i % 2 == 0 ? .42f : .3f) * k;
            LotusMesh.ghost(ghost, p, LotusMesh.petal(1, .55f, 0, .25f, 0, 4, 3).moved(q, new Vector3f(halo).add(new Vector3f(out).mul(.44f * k)), len),
                    BossMesh.glow(i % 2 == 0 ? LotusMesh.PINK : LotusMesh.GOLD, .55f * k), light);
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, halo, 1.5f * k, 0, -1, BossMesh.glow(LotusMesh.GOLD, .22f * k), light);

        // ---- small petals circling the caster
        VertexConsumer solid = BossMesh.solid(buffer, LotusMesh.PETAL);
        for (int i = 0; i < 8; i++) {
            float a = i * Mth.TWO_PI / 8 + age * .06f;
            float y = .7f + .45f * Mth.sin(age * .07f + i * 1.3f);
            Vector3f at = new Vector3f(Mth.cos(a) * 1.15f, y, Mth.sin(a) * 1.15f).mul(1, 1, 1);
            Vector3f tangent = new Vector3f(-Mth.sin(a), .15f, Mth.cos(a));
            Quaternionf q = LotusMesh.frame(tangent, new Vector3f(Mth.cos(a), 0, Mth.sin(a)).rotateAxis(age * .2f + i, tangent.x, tangent.y, tangent.z));
            LotusMesh.petal(1, .62f, .1f, .45f, .1f, 4, 3).moved(q, at, .26f * k).draw(solid, p, BossMesh.argb(0xFFF6F8, 1), BossMesh.argb(LotusMesh.TIP, 1), light);
        }

        // ---- a petal loosed at a foe
        if (target != null && since >= 0 && since < FLIGHT + 8) {
            Vector3f from = new Vector3f(0, .9f, 0);
            float t = Math.min(1, since / FLIGHT);
            if (t < 1) {
                Vector3f at = new Vector3f(from).lerp(target, t).add(0, Mth.sin(t * Mth.PI) * .6f, 0);
                Vector3f d = new Vector3f(target).sub(from).normalize();
                Quaternionf q = LotusMesh.frame(d, BossMesh.across(d).rotateAxis(since * 1.2f, d.x, d.y, d.z));
                LotusMesh.Petal pt = LotusMesh.petal(1, .62f, 0, .3f, -.1f).moved(q, new Vector3f(at).sub(new Vector3f(d).mul(.25f)), .5f);
                pt.draw(BossMesh.solid(buffer, LotusMesh.PETAL), p, BossMesh.argb(0xFFF6F8, 1), BossMesh.argb(LotusMesh.TIP, 1), light);
                VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
                RitualMesh.shaft(beam, p, view.eye(), at, new Vector3f(at).sub(new Vector3f(d).mul(1.4f)), .3f, BossMesh.glow(LotusMesh.PINK, .5f), light);
            } else {
                float b = (since - FLIGHT) / 8;
                VertexConsumer flash = BossMesh.light(buffer, BossMesh.GLOW);
                RitualMesh.billboard(flash, poseStack, view, target, .6f + 1.4f * b, 0, -1, BossMesh.glow(LotusMesh.PINK, (1 - b) * .8f), light);
            }
        }
        LotusMesh.motes(poseStack, buffer, view, new Vector3f(0, .2f, 0), 1.6f, 2.4f, age, 14, LotusMesh.GOLD, .7f * k, seed, light);
    }
}
