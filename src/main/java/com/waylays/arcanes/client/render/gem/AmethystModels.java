package com.waylays.arcanes.client.render.gem;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BellModel;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.boss.Sculpt;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The amethyst's newer spells. The amethyst resonates.
 * <ul>
 * <li>Amethyst Spikes: clusters of amethyst burst out of the ground one after another along a line, bigger and
 * bigger, ringing, then sink back.</li>
 * <li>Budding Amethyst: a block of budding amethyst half sunk in the ground grows buds, then clusters, then great
 * crystals out of every face, a ring of sound running out at each stage; full grown, it shatters.</li>
 * <li>Amethyst Chimes: a ring of gold over the caster's head, amethyst crystals hanging from it on silver threads,
 * swaying; when they ring they swing and blaze, a ring of sound runs out and a wave of it strikes a foe.</li>
 * </ul>
 */
public final class AmethystModels {
    private static final Vector3f U = new Vector3f(1, 0, 0), V = new Vector3f(0, 0, -1);
    private static final int AMETHYST = GemMesh.AMETHYST, LIGHT = GemMesh.AMETHYST_LIGHT;
    private static final ResourceLocation BUDDING = ResourceLocation.withDefaultNamespace("textures/block/budding_amethyst.png");

    private AmethystModels() {}

    // ---------------------------------------------------------------- spikes

    /** Along {@code dir} from the pose's origin, {@code count} clusters a block apart, one a tick. */
    public static void spikes(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f dir, float age, int life, int count, int seed, int light) {
        Vector3f d = new Vector3f(dir.x, 0, dir.z);
        if (d.lengthSquared() < 1.0E-6f) {
            d.set(0, 0, 1);
        }
        d.normalize();
        Vector3f side = new Vector3f(d.z, 0, -d.x);
        float sink = BossMesh.ramp(age, life - 10, life);
        PoseStack.Pose p = poseStack.last();
        VertexConsumer glass = BossMesh.see(buffer, GemMesh.FACET);
        Vector3f[] tips = new Vector3f[count];
        float[] grown = new float[count];
        for (int k = 0; k < count; k++) {
            float local = age - (k + 1);
            float up = local < 0 ? 0 : Math.min(1.15f, BossMesh.ramp(local, 0, 2.5f) * 1.15f) - .15f * BossMesh.ramp(local, 2.5f, 5);
            grown[k] = up;
            if (up <= .01f) {
                continue;
            }
            Vector3f base = new Vector3f(d).mul(k + 1.2f).add(new Vector3f(side).mul((BossMesh.hash(seed + k) - .5f) * .4f));
            float scale = (.65f + .5f * k / count) * (1 - .8f * sink);
            for (int c = 0; c < 4; c++) {
                float a = c * Mth.TWO_PI / 4 + BossMesh.hash(seed + k * 7 + c);
                Vector3f lean = new Vector3f(Mth.cos(a), 0, Mth.sin(a)).mul(c == 0 ? .1f : .55f).add(0, 1, 0).normalize();
                float size = (c == 0 ? .75f : .42f + .12f * BossMesh.hash(seed + c + k)) * scale;
                float stretch = c == 0 ? 2.8f : 2f;
                Vector3f at = new Vector3f(base).add(new Vector3f(lean).mul(size * stretch * .5f * up)).add(Mth.cos(a) * (c == 0 ? 0 : .2f), -(1 - up) * size * stretch
                        - sink * .8f, Mth.sin(a) * (c == 0 ? 0 : .2f));
                GemMesh.gem(glass, p, GemMesh.Cut.CRYSTAL, at, RubyModels.along(lean, k + c), size, stretch, c == 0 ? AMETHYST : BossMesh.mix(AMETHYST, LIGHT, .3f), .82f,
                        age * 3 + k * 7 + c, light);
                if (c == 0) {
                    tips[k] = at;
                }
            }
        }
        for (int k = 0; k < count; k++) {
            if (tips[k] != null) {
                GemMesh.sparkles(poseStack, buffer, view, GemMesh.Cut.CRYSTAL, tips[k], new Quaternionf(), .5f, 2.4f, AMETHYST, age * 2, false, seed + k, light);
            }
        }
        // A ring of sound and a glow where each bursts out
        VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
        for (int k = 0; k < count; k++) {
            float local = age - (k + 1);
            if (local < 0 || local > 8) {
                continue;
            }
            float t = local / 8;
            Vector3f base = new Vector3f(d).mul(k + 1.2f).add(0, .1f, 0);
            BossMesh.annulus(ring, p, base, U, V, .3f + 1.4f * t, .5f + 1.6f * t, 24, 1, 0, 0, BossMesh.glow(LIGHT, 0), BossMesh.glow(LIGHT, .8f * (1 - t)), light, true);
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        for (int k = 0; k < count; k++) {
            if (grown[k] > .01f) {
                RitualMesh.billboard(glow, poseStack, view, new Vector3f(d).mul(k + 1.2f).add(0, .5f, 0), 1.8f, 0, -1, BossMesh.glow(AMETHYST, .3f * (1 - sink)), light);
            }
        }
    }

    // ---------------------------------------------------------------- budding amethyst

    /** On the ground at the pose's origin, {@code radius} round, growing a stage every {@code stage} ticks (three), shattering at the fourth. */
    public static void budding(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int life, float radius, int stage, int seed,
                               int light) {
        float shatter = 4f * stage;
        float hit = age - shatter;
        PoseStack.Pose p = poseStack.last();
        // Its rings of sound, one at each stage
        VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
        for (int s = 1; s <= 4; s++) {
            float local = age - s * stage;
            if (local >= 0 && local < 16) {
                float t = local / 16;
                float rr = radius * (s == 4 ? 1.3f : .75f) * (.2f + .9f * t);
                BossMesh.annulus(ring, p, new Vector3f(0, .12f, 0), U, V, rr * .8f, rr, 48, 1, 0, 0, BossMesh.glow(LIGHT, 0),
                        BossMesh.glow(s == 4 ? 0xFFFFFF : LIGHT, .9f * (1 - t)), light, true);
            }
        }
        VertexConsumer sigil = BossMesh.light(buffer, RubyModels.GemSigil.SIGIL);
        BossMesh.pool(sigil, p, 0, .06f, 0, radius * 1.3f * BossMesh.ramp(age, 0, 8) * (1 - BossMesh.ramp(hit, 0, 10)), age * .02f, BossMesh.glow(AMETHYST, .7f), light);
        if (hit < 0) {
            // The block, half sunk and tilted
            Quaternionf tilt = new Quaternionf().rotationY(seed).rotateX(.25f).rotateZ(.15f);
            poseStack.pushPose();
            poseStack.translate(0, .3f, 0);
            poseStack.mulPose(tilt);
            Sculpt.block(BossMesh.solid(buffer, BUDDING), poseStack.last(), 1.1f, 1.1f, 1.1f, .04f, 1 / 1.1f, 0xFFFFFFFF, light);
            poseStack.popPose();
            // Its crystals: buds, then clusters, then great crystals, out of every face
            VertexConsumer glass = BossMesh.see(buffer, GemMesh.FACET);
            Vector3f[] faces = {new Vector3f(1, 0, 0), new Vector3f(-1, 0, 0), new Vector3f(0, 1, 0), new Vector3f(0, 0, 1), new Vector3f(0, 0, -1)};
            for (int s = 1; s <= 3; s++) {
                float grow = BossMesh.ramp(age, s * stage - 3, s * stage + 3);
                if (grow <= .01f) {
                    continue;
                }
                for (int f = 0; f < faces.length; f++) {
                    for (int c = 0; c < (s == 3 ? 1 : 2); c++) {
                        Vector3f n = tilt.transform(new Vector3f(faces[f]));
                        Vector3f off = tilt.transform(new Vector3f(faces[f]).mul(.55f).add(perp(faces[f], (BossMesh.hash(seed + f * 7 + c * 3 + s) - .5f) * .7f,
                                (BossMesh.hash(seed + f * 11 + c * 5 + s) - .5f) * .7f)));
                        Vector3f dir = new Vector3f(n).add(BossMesh.hash(seed + f + s) * .3f - .15f, .3f, BossMesh.hash(seed + f * 3 + s) * .3f - .15f).normalize();
                        float size = (s == 1 ? .22f : s == 2 ? .36f : .62f) * grow;
                        float stretch = s == 3 ? 2.8f : 2f;
                        Vector3f at = new Vector3f(off).add(0, .3f, 0).add(new Vector3f(dir).mul(size * stretch * .45f));
                        GemMesh.gem(glass, p, GemMesh.Cut.CRYSTAL, at, RubyModels.along(dir, f + s), size, stretch, BossMesh.mix(AMETHYST, LIGHT, s * .1f), .82f,
                                age * 3 + f * 5 + s * 11, light);
                    }
                }
            }
            float full = BossMesh.ramp(age, 0, shatter);
            VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
            float pulse = .5f + .5f * Mth.sin(age * (.2f + .3f * full));
            RitualMesh.billboard(glow, poseStack, view, new Vector3f(0, .9f, 0), 2.5f + 2 * full, 0, -1, BossMesh.glow(AMETHYST, (.25f + .35f * full) * pulse), light);
            GemMesh.sparkles(poseStack, buffer, view, GemMesh.Cut.CRYSTAL, new Vector3f(0, 1, 0), new Quaternionf(), 1.2f * full + .4f, 2, AMETHYST, age * 2, false, seed,
                    light);
            return;
        }
        // Shattered: its crystals flying apart
        poseStack.pushPose();
        poseStack.translate(0, 1, 0);
        RubyModels.burst(poseStack, buffer, view, hit / 14, AMETHYST, 2.4f, seed, light);
        poseStack.popPose();
    }

    private static Vector3f perp(Vector3f n, float a, float b) {
        Vector3f u = Math.abs(n.y) > .5f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        Vector3f w = new Vector3f(n).cross(u).normalize();
        Vector3f v = new Vector3f(n).cross(w).normalize();
        return w.mul(a).add(v.mul(b));
    }

    // ---------------------------------------------------------------- chimes

    /** Over the caster's head (the pose's origin is at their feet); they ring every {@code every} ticks from {@code first}. */
    public static void chimes(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int life, int first, int every, float since,
                              Vector3f target, int seed, int light) {
        float k = BossMesh.ramp(age, 0, 10) * (1 - BossMesh.ramp(age, life - 12, life));
        if (k <= .01f) {
            return;
        }
        float local = age < first ? 99 : (age - first) % every;
        float ring = local < 99 ? (float) Math.exp(-local / 4) : 0;
        Vector3f top = new Vector3f(0, 2.75f, 0);
        PoseStack.Pose p = poseStack.last();
        // The ring of gold
        poseStack.pushPose();
        poseStack.translate(top.x, top.y, top.z);
        poseStack.mulPose(new Quaternionf().rotationY(age * .02f));
        poseStack.scale(k, k, k);
        Sculpt.torus(BossMesh.solid(buffer, BellModel.GOLD), poseStack.last(), .62f, .05f, 40, 6, 0, Mth.TWO_PI, 8, 0xFFFFFFFF, light);
        poseStack.popPose();
        // The crystals hanging from it on their threads, swaying, swinging hard when they ring
        int n = 8;
        Vector3f[] hang = new Vector3f[n], tip = new Vector3f[n];
        float[] len = new float[n];
        for (int i = 0; i < n; i++) {
            float a = i * Mth.TWO_PI / n + age * .02f;
            hang[i] = new Vector3f(Mth.cos(a) * .62f * k, top.y, Mth.sin(a) * .62f * k);
            len[i] = .35f + .3f * BossMesh.hash(seed + i * 3);
            float swing = .12f * Mth.sin(age * .1f + i * 1.3f) + .45f * ring * Mth.sin(local * 1.2f + i);
            Vector3f d = new Vector3f(Mth.sin(swing) * Mth.cos(a + 1.5f), -Mth.cos(swing), Mth.sin(swing) * Mth.sin(a + 1.5f));
            tip[i] = new Vector3f(hang[i]).add(new Vector3f(d).mul(len[i] * k));
        }
        VertexConsumer thread = BossMesh.solid(buffer, BellModel.IRON);
        for (int i = 0; i < n; i++) {
            Sculpt.sweep(thread, p, new Vector3f[] {hang[i], tip[i]}, Sculpt.fill(.008f, 2), Sculpt.fill(.008f, 2), Sculpt.circle(4), false, null, 1,
                    Sculpt.fill(0xFFDDE0F0, 2), false, light);
        }
        VertexConsumer glass = BossMesh.see(buffer, GemMesh.FACET);
        for (int i = 0; i < n; i++) {
            Vector3f d = new Vector3f(tip[i]).sub(hang[i]).normalize();
            float size = (.16f + .06f * BossMesh.hash(seed + i)) * k;
            GemMesh.gem(glass, p, GemMesh.Cut.CRYSTAL, new Vector3f(tip[i]).add(new Vector3f(d).mul(size * 1.2f)), RubyModels.along(d, i), size, 2.6f,
                    BossMesh.mix(AMETHYST, 0xFFFFFF, .25f * ring), .85f, age * 3 + i * 9, light);
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        for (int i = 0; i < n; i++) {
            RitualMesh.billboard(glow, poseStack, view, tip[i], .5f + .8f * ring, 0, -1, BossMesh.glow(LIGHT, .25f + .6f * ring), light);
        }
        RitualMesh.billboard(glow, poseStack, view, top, 2.4f * k, 0, -1, BossMesh.glow(AMETHYST, .2f + .3f * ring), light);
        // Its ring of sound, and a wave of it at a foe
        if (ring > .02f) {
            float t = local / 16;
            VertexConsumer wave = BossMesh.light(buffer, BossMesh.RING);
            float rr = .8f + 6 * Math.min(1, t);
            BossMesh.annulus(wave, p, new Vector3f(top).add(0, -.3f, 0), U, V, rr * .85f, rr, 48, 1, 0, 0, BossMesh.glow(LIGHT, 0), BossMesh.glow(LIGHT, .7f * ring),
                    light, true);
        }
        if (target != null && since >= 0 && since < 10) {
            float s = 1 - since / 10;
            VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
            Vector3f from = new Vector3f(top).add(0, -.4f, 0);
            GemMesh.ray(beam, p, view.eye(), from, target, AMETHYST, .25f, s, age);
            VertexConsumer pulse = BossMesh.light(buffer, BossMesh.RING);
            Vector3f d = new Vector3f(target).sub(from);
            float l = d.length();
            d.normalize();
            Vector3f u = BossMesh.across(d), v = new Vector3f(u).cross(d).normalize();
            for (int i = 0; i < 4; i++) {
                float f = (since * .25f + i * .25f) % 1;
                BossMesh.annulus(pulse, p, new Vector3f(from).add(new Vector3f(d).mul(l * f)), u, v, .2f, .45f, 20, 1, 0, 0, BossMesh.glow(LIGHT, 0),
                        BossMesh.glow(LIGHT, .8f * s), light, true);
            }
        }
    }
}
