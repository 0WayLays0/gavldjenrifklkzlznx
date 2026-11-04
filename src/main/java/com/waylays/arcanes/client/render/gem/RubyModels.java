package com.waylays.arcanes.client.render.gem;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.boss.Sculpt;
import com.waylays.arcanes.client.render.lotus.LotusMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The ruby's newer spells. The ruby burns: focused light.
 * <ul>
 * <li>Ruby Shards: a long ruby crystal flying point first, spinning, a trail of fire and embers behind it; it shatters
 * where it stops.</li>
 * <li>Ruby Nova: a great ruby rises over its jeweller's sigil, gathering light from all round, then looses a ring of
 * rays over the ground and breaks; a ring of fire runs out.</li>
 * <li>Ruby Meteor: a giant ruby falls burning out of the sky onto its sigil, strikes with a flash and a ring of fire,
 * and leaves a molten pool where ruby crystals stand in the heat.</li>
 * </ul>
 */
public final class RubyModels {
    private static final Vector3f U = new Vector3f(1, 0, 0), V = new Vector3f(0, 0, -1);
    private static final int RUBY = GemMesh.RUBY, LIGHT = GemMesh.RUBY_LIGHT, FIRE = 0xFF7A30;

    private RubyModels() {}

    /** Turns +Y onto {@code d}, spun {@code spin} round it. */
    static Quaternionf along(Vector3f d, float spin) {
        return LotusMesh.frame(d, BossMesh.across(d).rotateAxis(spin, d.x, d.y, d.z));
    }

    // ---------------------------------------------------------------- shards

    public static void shard(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f dir, float age, boolean done, float since, int seed,
                             int light) {
        Vector3f d = new Vector3f(dir);
        if (d.lengthSquared() < 1.0E-6f) {
            d.set(0, 0, 1);
        }
        d.normalize();
        if (done) {
            burst(poseStack, buffer, view, since / 10, RUBY, .7f, seed, light);
            return;
        }
        float grow = BossMesh.ramp(age, 0, 3);
        GemMesh.jewel(poseStack, buffer, view, GemMesh.Cut.CRYSTAL, new Vector3f(), along(d, age * .8f + seed), .5f * grow, 2.6f, RUBY, .82f, age * 3, .9f, false,
                seed, light);
        VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
        PoseStack.Pose p = poseStack.last();
        GemMesh.ray(beam, p, view.eye(), new Vector3f(d).mul(-.6f), new Vector3f(d).mul(-3.2f), FIRE, .35f, .7f * grow, age);
        VertexConsumer ember = BossMesh.light(buffer, BossMesh.EMBER);
        for (int k = 0; k < 7; k++) {
            float t = (age * .3f + k / 7f) % 1;
            Vector3f side = BossMesh.across(d).rotateAxis(k * 2.4f + age * .3f, d.x, d.y, d.z);
            Vector3f at = new Vector3f(d).mul(-.8f - t * 2.5f).add(side.mul(.15f + .35f * t));
            RitualMesh.billboard(ember, poseStack, view, at, .25f * (1 - t), age * .2f + k, -1, BossMesh.glow(k % 2 == 0 ? FIRE : LIGHT, (1 - t)), light);
        }
    }

    /** Small crystals flung out of a breaking gem, {@code t} (0 to 1) of the way through, and its flash. */
    static void burst(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float t, int rgb, float size, int seed, int light) {
        if (t >= 1) {
            return;
        }
        VertexConsumer glass = BossMesh.see(buffer, GemMesh.FACET);
        PoseStack.Pose p = poseStack.last();
        float out = 1 - (1 - t) * (1 - t);
        for (int k = 0; k < 9; k++) {
            float a = BossMesh.hash(seed + k) * Mth.TWO_PI;
            Vector3f d = new Vector3f(Mth.cos(a), .2f + BossMesh.hash(seed + k * 3) * 1.1f, Mth.sin(a)).normalize();
            Vector3f at = new Vector3f(d).mul(size * (.2f + 2.2f * out)).add(0, -1.5f * t * t, 0);
            GemMesh.gem(glass, p, GemMesh.Cut.CRYSTAL, at, along(d, t * 9 + k), size * .3f * (1 - t * .6f), 1.6f, rgb, .8f * (1 - t * .5f), t * 40 + k, light);
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        float flash = (1 - t) * (1 - t);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(), size * (1 + 3 * t), 0, -1, BossMesh.glow(0xFFFFFF, flash), light);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(), size * (2.5f + 3 * t), 0, -1, BossMesh.glow(rgb, flash * .8f), light);
    }

    // ---------------------------------------------------------------- nova

    /** On the ground at the pose's origin, {@code radius} round; its rays loosed at {@code charge}. */
    public static void nova(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int life, int charge, float radius, int seed,
                            int light) {
        float hit = age - charge;
        float fade = 1 - BossMesh.ramp(age, life - 10, life);
        PoseStack.Pose p = poseStack.last();
        // ---- its sigil on the ground, a ring of fire running out when it looses
        VertexConsumer sigil = BossMesh.light(buffer, GemSigil.SIGIL);
        float drawn = BossMesh.ramp(age, 0, 10);
        BossMesh.pool(sigil, p, 0, .06f, 0, 5.5f * drawn, age * .03f, BossMesh.glow(RUBY, (.7f + .3f * Mth.sin(age * .4f)) * fade), light);
        BossMesh.pool(sigil, p, 0, .07f, 0, 3.4f * drawn, -age * .05f, BossMesh.glow(LIGHT, .5f * fade), light);
        if (hit < 0) {
            // ---- the ruby rising, gathering light from all round it
            float rise = BossMesh.ramp(age, 0, charge);
            Vector3f at = new Vector3f(0, 1 + 2.2f * rise + .08f * Mth.sin(age * .3f), 0);
            float size = .5f + .8f * rise;
            GemMesh.jewel(poseStack, buffer, view, GemMesh.Cut.BRILLIANT, at, new Quaternionf().rotationY(age * .15f).rotateX(.25f * Mth.sin(age * .1f)), size, 1, RUBY,
                    .85f, age * 3, .5f + .7f * rise, false, seed, light);
            VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
            for (int k = 0; k < 10; k++) {
                float a = k * Mth.TWO_PI / 10 + age * .05f;
                float t = (age * .07f + k * .37f) % 1;
                Vector3f from = new Vector3f(Mth.cos(a) * 4.5f * (1 - t), .3f + 2 * t, Mth.sin(a) * 4.5f * (1 - t));
                Vector3f to = new Vector3f(from).lerp(at, .25f);
                RitualMesh.shaft(beam, p, view.eye(), from, to, .18f, BossMesh.glow(k % 2 == 0 ? RUBY : FIRE, .7f * Mth.sin(t * Mth.PI) * rise), light);
            }
            return;
        }
        // ---- the rays loosed over the ground, the ruby breaking
        Vector3f heart = new Vector3f(0, 3.2f, 0);
        float reach = BossMesh.ramp(hit, 0, 4);
        float out = (float) Math.exp(-hit / 6);
        VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
        for (int k = 0; k < 16; k++) {
            float a = k * Mth.TWO_PI / 16 + seed;
            Vector3f end = new Vector3f(Mth.cos(a) * radius * reach, .3f, Mth.sin(a) * radius * reach);
            GemMesh.ray(beam, p, view.eye(), heart, end, RUBY, .3f, out, age);
        }
        VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
        float rr = radius * (.2f + 1.1f * BossMesh.ramp(hit, 0, 10));
        BossMesh.annulus(ring, p, new Vector3f(0, .15f, 0), U, V, rr * .7f, rr, 56, 1, 0, 0, BossMesh.glow(FIRE, 0), BossMesh.glow(FIRE, .9f * out * fade), light,
                true);
        VertexConsumer scorch = BossMesh.light(buffer, BossMesh.NOISE);
        BossMesh.annulus(scorch, p, new Vector3f(0, .08f, 0), U, V, 0, radius, 48, 3, 0, age * .01f, BossMesh.glow(FIRE, .25f * fade),
                BossMesh.glow(RUBY, 0), light, false);
        poseStack.pushPose();
        poseStack.translate(heart.x, heart.y, heart.z);
        burst(poseStack, buffer, view, hit / 14, RUBY, 1.6f, seed, light);
        poseStack.popPose();
    }

    // ---------------------------------------------------------------- meteor

    /** Landing at the pose's origin, {@code radius} round, falling for {@code fall} ticks from {@code height}. */
    public static void meteor(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int life, int fall, float height, float radius,
                              int seed, int light) {
        float hit = age - fall;
        float fade = 1 - BossMesh.ramp(age, life - 15, life);
        PoseStack.Pose p = poseStack.last();
        // ---- the warning on the ground: its sigil, a ring closing in
        VertexConsumer sigil = BossMesh.light(buffer, GemSigil.SIGIL);
        if (hit < 0) {
            float drawn = BossMesh.ramp(age, 0, 8);
            BossMesh.pool(sigil, p, 0, .06f, 0, radius * 2.2f * drawn, age * .04f, BossMesh.glow(RUBY, .8f), light);
            VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
            float rr = radius * (1.6f - .6f * BossMesh.ramp(age, 0, fall));
            BossMesh.annulus(ring, p, new Vector3f(0, .1f, 0), U, V, rr * .9f, rr, 48, 1, 0, 0, BossMesh.glow(RUBY, .2f), BossMesh.glow(FIRE, .8f), light, true);
            // ---- the ruby falling, burning
            float t = Mth.clamp(age / fall, 0, 1);
            float y = height * (1 - t * t) + 1.2f;
            Vector3f at = new Vector3f(1.5f * (1 - t), y, -1 * (1 - t));
            Vector3f d = new Vector3f(-1.5f, -2 * height * t - .1f, 1).normalize();
            GemMesh.jewel(poseStack, buffer, view, GemMesh.Cut.BRILLIANT, at, new Quaternionf().rotationY(age * .2f).rotateZ(age * .13f), 2.4f, 1, RUBY, .9f, age * 3,
                    1, false, seed, light);
            VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
            GemMesh.ray(beam, p, view.eye(), at, new Vector3f(at).sub(new Vector3f(d).mul(6 + 8 * t)), FIRE, 1.5f, .9f, age);
            VertexConsumer ember = BossMesh.light(buffer, BossMesh.EMBER);
            for (int k = 0; k < 14; k++) {
                float s = (age * .25f + k / 14f) % 1;
                Vector3f e = new Vector3f(at).sub(new Vector3f(d).mul(1.5f + s * 7)).add(BossMesh.across(d).rotateAxis(k * 2.2f, d.x, d.y, d.z).mul(.6f + s));
                RitualMesh.billboard(ember, poseStack, view, e, .7f * (1 - s), k, -1, BossMesh.glow(k % 2 == 0 ? FIRE : LIGHT, 1 - s), light);
            }
            VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
            RitualMesh.billboard(glow, poseStack, view, at, 6, 0, -1, BossMesh.glow(FIRE, .5f), light);
            return;
        }
        // ---- the strike: a flash, a ring of fire
        float strike = (float) Math.exp(-hit / 5);
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(0, 1, 0), radius * (1.5f + 2 * (1 - strike)), 0, -1, BossMesh.glow(0xFFFFFF, strike), light);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(0, 1, 0), radius * 3.5f, 0, -1, BossMesh.glow(FIRE, .7f * strike), light);
        VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
        for (int k = 0; k < 2; k++) {
            float rt = Mth.clamp((hit - k * 3) / 16, 0, 1);
            if (rt > 0 && rt < 1) {
                float rr = radius * (.3f + 2.2f * rt);
                BossMesh.annulus(ring, p, new Vector3f(0, .15f, 0), U, V, rr * .75f, rr, 56, 1, 0, 0, BossMesh.glow(FIRE, 0), BossMesh.glow(k == 0 ? 0xFFFFFF : FIRE,
                        .9f * (1 - rt)), light, true);
            }
        }
        // ---- the molten pool: glowing, flowing, cracked
        VertexConsumer pool = BossMesh.light(buffer, BossMesh.NOISE);
        float spread = BossMesh.ramp(hit, 0, 8) * fade;
        BossMesh.annulus(pool, p, new Vector3f(0, .07f, 0), U, V, 0, radius * .85f * spread, 48, 3, age * .002f, age * .015f, BossMesh.glow(0xFFC060, .9f),
                BossMesh.glow(RUBY, .6f), light, false);
        VertexConsumer cracks = BossMesh.light(buffer, BossMesh.RUNES);
        BossMesh.pool(cracks, p, 0, .09f, 0, radius * 1.9f * spread, seed, BossMesh.glow(FIRE, .5f * spread), light);
        // ---- the ruby half sunk at its heart, crystals standing in the heat round it
        float sink = BossMesh.ramp(hit, 20, life - fall);
        VertexConsumer glass = BossMesh.see(buffer, GemMesh.FACET);
        GemMesh.gem(glass, p, GemMesh.Cut.BRILLIANT, new Vector3f(0, .9f - 1.6f * sink, 0), new Quaternionf().rotationX(.6f).rotateY(seed), 2.2f * fade, 1, RUBY, .9f,
                age * 2, light);
        for (int k = 0; k < 9; k++) {
            float a = k * Mth.TWO_PI / 9 + BossMesh.hash(seed + k) * .5f;
            float d = radius * (.35f + .4f * BossMesh.hash(seed + k * 7));
            Vector3f out = new Vector3f(Mth.cos(a) * .45f, 1, Mth.sin(a) * .45f).normalize();
            float grow = BossMesh.ramp(hit, 2 + k, 8 + k) * fade;
            GemMesh.gem(glass, p, GemMesh.Cut.CRYSTAL, new Vector3f(Mth.cos(a) * d, .25f * grow, Mth.sin(a) * d), along(out, k), (.35f + .25f * BossMesh.hash(seed + k))
                    * grow, 2, RUBY, .85f, age * 2 + k * 9, light);
        }
        VertexConsumer ember = BossMesh.light(buffer, BossMesh.EMBER);
        for (int k = 0; k < 20; k++) {
            float s = (age * (.015f + .01f * BossMesh.hash(k)) + BossMesh.hash(k * 3)) % 1;
            float a = BossMesh.hash(k * 5) * Mth.TWO_PI, d = radius * .8f * (float) Math.sqrt(BossMesh.hash(k * 9));
            RitualMesh.billboard(ember, poseStack, view, new Vector3f(Mth.cos(a) * d, .2f + s * 3, Mth.sin(a) * d), .3f, age * .1f + k, -1,
                    BossMesh.glow(k % 2 == 0 ? FIRE : LIGHT, Mth.sin(s * Mth.PI) * fade), light);
        }
        RitualMesh.billboard(glow(buffer), poseStack, view, new Vector3f(0, .6f, 0), radius * 2.2f, 0, -1, BossMesh.glow(FIRE, .25f * fade), light);
    }

    private static VertexConsumer glow(MultiBufferSource buffer) {
        return BossMesh.light(buffer, BossMesh.GLOW);
    }

    /** Where the jeweller's sigil texture lives (as {@code GemRender.SIGIL}). */
    static final class GemSigil {
        static final net.minecraft.resources.ResourceLocation SIGIL = com.waylays.arcanes.ArcanesOublies.id("textures/entity/gem/sigil.png");

        private GemSigil() {}
    }

    /** A ring of light lying on the ground. */
    static void ring(VertexConsumer c, PoseStack.Pose p, float r0, float r1, int in, int out, int light) {
        BossMesh.annulus(c, p, new Vector3f(0, .1f, 0), U, V, r0, r1, 48, 1, 0, 0, in, out, light, true);
    }

    /** {@code n} points round a circle of {@code r} at height {@code y}. */
    static Vector3f[] circle(float r, float y, int n, float turn) {
        Vector3f[] pts = new Vector3f[n + 1];
        for (int k = 0; k <= n; k++) {
            float a = k * Mth.TWO_PI / n + turn;
            pts[k] = new Vector3f(Mth.cos(a) * r, y, Mth.sin(a) * r);
        }
        return pts;
    }

    static float[] fill(float v, int n) {
        return Sculpt.fill(v, n);
    }
}
