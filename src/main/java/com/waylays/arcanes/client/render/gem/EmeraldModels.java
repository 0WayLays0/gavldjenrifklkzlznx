package com.waylays.arcanes.client.render.gem;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.boss.Sculpt;
import com.waylays.arcanes.client.render.lotus.LotusMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.sand.SandMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The emerald's newer spells. The emerald heals.
 * <ul>
 * <li>Lifeline: a small emerald turning at the caster's breast, a beam of green light from it to the one it heals,
 * threads of light winding round the beam and motes running down it; a ring of green light at their feet.</li>
 * <li>Emerald Aegis: plates of emerald fly in from all round and close into a shell round the caster, then sink into
 * them in a shimmer of green.</li>
 * <li>Emerald Tree: a tree of crystal grows out of an emerald sigil, its branches tipped with emerald points, emerald
 * fruits hanging glowing; each time it heals, a fruit falls and a ring of green light runs out.</li>
 * <li>Emerald Thorns: a ring of green runs out over the ground, then a thicket of emerald thorns bursts out of it, and
 * streams of green light run back from them to the caster.</li>
 * </ul>
 */
public final class EmeraldModels {
    private static final Vector3f U = new Vector3f(1, 0, 0), V = new Vector3f(0, 0, -1);
    private static final int EMERALD = GemMesh.EMERALD, LIGHT = GemMesh.EMERALD_LIGHT;

    private EmeraldModels() {}

    // ---------------------------------------------------------------- lifeline

    /** From the caster (the pose's origin, at their feet) to {@code target} (the one it heals, relative), healing every {@code every} ticks. */
    public static void lifeline(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int life, Vector3f target, float since, int seed,
                                int light) {
        float k = BossMesh.ramp(age, 0, 6) * (1 - BossMesh.ramp(age, life - 8, life));
        if (k <= .01f) {
            return;
        }
        PoseStack.Pose p = poseStack.last();
        Vector3f heart = new Vector3f(0, 1.35f, 0);
        Vector3f to = target == null ? new Vector3f(heart) : new Vector3f(target);
        float pulse = since >= 0 && since < 8 ? 1 - since / 8 : 0;
        GemMesh.jewel(poseStack, buffer, view, GemMesh.Cut.STEP, new Vector3f(heart).add(0, .08f * Mth.sin(age * .2f), .0f),
                new Quaternionf().rotationY(age * .12f).rotateX(.4f), .32f * k, 1, EMERALD, .85f, age * 3, .7f + .3f * pulse, false, seed, light);
        float l = to.distance(heart);
        VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
        if (l > .6f) {
            GemMesh.ray(beam, p, view.eye(), heart, to, EMERALD, .22f * k, .8f + .2f * pulse, age);
            // Threads of light winding round the beam
            Vector3f d = new Vector3f(to).sub(heart).normalize();
            Vector3f u = BossMesh.across(d), v = new Vector3f(u).cross(d).normalize();
            for (int s = 0; s < 2; s++) {
                int n = 24;
                Vector3f[] pts = new Vector3f[n];
                int[] col = new int[n];
                for (int i = 0; i < n; i++) {
                    float t = (float) i / (n - 1);
                    float a = t * l * 3 - age * .4f + s * Mth.PI;
                    float r = .22f * Mth.sin(t * Mth.PI) + .04f;
                    pts[i] = new Vector3f(heart).lerp(to, t).add(new Vector3f(u).mul(Mth.cos(a) * r)).add(new Vector3f(v).mul(Mth.sin(a) * r));
                    col[i] = BossMesh.glow(LIGHT, .6f * k * Mth.sin(t * Mth.PI));
                }
                SandMesh.ribbon(beam, p, view.eye(), pts, Sculpt.fill(.06f, n), 1, 0, col, light);
            }
            VertexConsumer mote = BossMesh.light(buffer, BossMesh.MOTE);
            for (int i = 0; i < 8; i++) {
                float t = (age * .04f + i / 8f) % 1;
                RitualMesh.billboard(mote, poseStack, view, new Vector3f(heart).lerp(to, t), .22f, 0, -1, BossMesh.glow(i % 2 == 0 ? LIGHT : 0xFFFFFF, .9f * k), light);
            }
        } else {
            // Healing its own caster: light spiralling round them
            int n = 40;
            Vector3f[] pts = new Vector3f[n];
            int[] col = new int[n];
            for (int i = 0; i < n; i++) {
                float t = (float) i / (n - 1);
                float a = t * 9 + age * .25f;
                pts[i] = new Vector3f(Mth.cos(a) * .7f, .1f + t * 2, Mth.sin(a) * .7f);
                col[i] = BossMesh.glow(LIGHT, .7f * k * Mth.sin(t * Mth.PI));
            }
            SandMesh.ribbon(beam, p, view.eye(), pts, Sculpt.fill(.1f, n), 1, 0, col, light);
            to = new Vector3f(0, 1, 0);
        }
        // A ring at its patient's feet
        VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
        Vector3f feet = new Vector3f(to.x, to.y - 1.0f, to.z);
        BossMesh.annulus(ring, p, feet, U, V, .55f, .8f + .3f * pulse, 32, 1, age * .05f, 0, BossMesh.glow(LIGHT, 0), BossMesh.glow(EMERALD, (.6f + .4f * pulse) * k), light,
                true);
        if (pulse > 0) {
            VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
            RitualMesh.billboard(glow, poseStack, view, to, 1 + 1.5f * (1 - pulse), 0, -1, BossMesh.glow(LIGHT, pulse), light);
        }
        LotusMesh.motes(poseStack, buffer, view, feet, .6f, 2, age, 10, LIGHT, .8f * k, seed, light);
    }

    // ---------------------------------------------------------------- aegis

    public static void aegis(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int life, int seed, int light) {
        float close = BossMesh.ramp(age, 0, 12);
        float sink = BossMesh.ramp(age, life - 22, life - 4);
        float fade = 1 - BossMesh.ramp(age, life - 6, life);
        if (fade <= .01f) {
            return;
        }
        PoseStack.Pose p = poseStack.last();
        Vector3f centre = new Vector3f(0, 1, 0);
        float r = Mth.lerp(close, 4, 1.15f) * (1 - .5f * sink);
        VertexConsumer glass = BossMesh.see(buffer, GemMesh.FACET);
        int n = 16;
        for (int i = 0; i < n; i++) {
            // Spread over a sphere (a golden spiral), turning slowly
            float y = 1 - 2 * (i + .5f) / n;
            float rr = Mth.sqrt(1 - y * y);
            float a = i * 2.39996f + age * .02f;
            Vector3f out = new Vector3f(Mth.cos(a) * rr, y * .9f, Mth.sin(a) * rr).normalize();
            Vector3f at = new Vector3f(out).mul(r).add(centre);
            GemMesh.gem(glass, p, GemMesh.Cut.STEP, at, LotusMesh.frame(out, new Vector3f(0, 1, 0)), .55f * fade, .35f, EMERALD, .7f * fade, age * 3 + i * 7, light);
        }
        VertexConsumer dome = BossMesh.see(buffer, BossMesh.SHEEN);
        BossMesh.sphere(dome, p, centre, r * 1.02f, 10, 24, age * .01f, 0, 1, BossMesh.argb(0xB6FFD2, .14f * close * fade), light);
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, centre, 3.2f, 0, -1, BossMesh.glow(EMERALD, (.25f + .5f * sink) * fade), light);
        LotusMesh.motes(poseStack, buffer, view, new Vector3f(0, .1f, 0), 1.3f, 2.2f, age, 16, LIGHT, .9f * fade, seed, light);
        VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
        BossMesh.annulus(ring, p, new Vector3f(0, .1f, 0), U, V, 1.1f, 1.4f, 32, 1, age * .03f, 0, BossMesh.glow(EMERALD, 0), BossMesh.glow(LIGHT, .6f * fade), light,
                false);
    }

    // ---------------------------------------------------------------- tree

    /** Grown at the pose's origin over {@code grow} ticks, healing {@code radius} round; it last healed {@code since} ticks ago. */
    public static void tree(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int life, int grow, float radius, float since, int seed,
                            int light) {
        float g = BossMesh.ramp(age, 0, grow) * (1 - BossMesh.ramp(age, life - 15, life));
        if (g <= .01f) {
            return;
        }
        PoseStack.Pose p = poseStack.last();
        float pulse = since >= 0 && since < 12 ? 1 - since / 12 : 0;
        // Its sigil, and a ring of green running out when it heals
        VertexConsumer sigil = BossMesh.light(buffer, RubyModels.GemSigil.SIGIL);
        BossMesh.pool(sigil, p, 0, .06f, 0, radius * 1.15f * g, age * .01f, BossMesh.glow(EMERALD, .6f + .3f * pulse), light);
        if (pulse > 0) {
            VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
            float rr = radius * (1 - pulse * .9f);
            BossMesh.annulus(ring, p, new Vector3f(0, .12f, 0), U, V, rr * .85f, rr, 48, 1, 0, 0, BossMesh.glow(LIGHT, 0), BossMesh.glow(LIGHT, pulse), light, true);
        }
        // Trunk and branches of pale green crystal (paths in the tree's own space, grown by g)
        Vector3f[][] branches = branches(seed, g, age);
        VertexConsumer wood = BossMesh.see(buffer, GemMesh.FACET);
        for (int i = 0; i < branches.length; i++) {
            Vector3f[] b = branches[i];
            float r0 = i == 0 ? .3f : .13f;
            Sculpt.sweep(wood, p, b, Sculpt.taper(r0 * g, r0 * .35f * g, b.length, 1), Sculpt.taper(r0 * g, r0 * .35f * g, b.length, 1), Sculpt.circle(6), true, null,
                    1, Sculpt.fade(BossMesh.argb(0x9FE8B8, .75f), BossMesh.argb(0xD8FFE6, .6f), b.length), true, light);
        }
        // Emerald points at the branch tips, fruits hanging under them
        VertexConsumer glass = BossMesh.see(buffer, GemMesh.FACET);
        Vector3f[] fruits = new Vector3f[branches.length - 1];
        for (int i = 1; i < branches.length; i++) {
            Vector3f[] b = branches[i];
            Vector3f end = b[b.length - 1], d = new Vector3f(end).sub(b[b.length - 2]).normalize();
            GemMesh.gem(glass, p, GemMesh.Cut.CRYSTAL, new Vector3f(end).add(new Vector3f(d).mul(.18f * g)), RubyModels.along(d, i), .3f * g, 2.2f, EMERALD, .85f,
                    age * 3 + i * 7, light);
            Vector3f mid = b[b.length / 2];
            fruits[i - 1] = new Vector3f(mid).add(0, -.25f * g + .03f * Mth.sin(age * .1f + i), 0);
        }
        // A fruit falls when it heals
        int falling = since >= 0 && since < 12 ? (int) (BossMesh.hash(seed + (int) (age - since)) * fruits.length) : -1;
        for (int i = 0; i < fruits.length; i++) {
            Vector3f at = new Vector3f(fruits[i]);
            if (i == falling) {
                float t = since / 12;
                at.y = Math.max(.15f, at.y - 6 * t * t);
            }
            GemMesh.gem(glass, p, GemMesh.Cut.STEP, at, new Quaternionf().rotationY(age * .05f + i), .28f * g, 1, EMERALD, .9f, age * 4 + i * 13, light);
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        for (Vector3f f : fruits) {
            RitualMesh.billboard(glow, poseStack, view, f, .7f * g, 0, -1, BossMesh.glow(LIGHT, .45f + .4f * pulse), light);
        }
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(0, 1.8f, 0), 4.5f * g, 0, -1, BossMesh.glow(EMERALD, .18f + .3f * pulse), light);
        LotusMesh.motes(poseStack, buffer, view, new Vector3f(0, .2f, 0), radius * .6f * g, 3, age, 18, LIGHT, .8f * g, seed, light);
    }

    /** The trunk, then the branches, each a path, grown by {@code g}, swaying a little with {@code age}. */
    private static Vector3f[][] branches(int seed, float g, float age) {
        int n = 7;
        Vector3f[][] out = new Vector3f[n + 1][];
        float h = 3.1f * g;
        out[0] = Sculpt.bezier(new Vector3f(0, -.1f, 0), new Vector3f(.25f, h * .35f, 0), new Vector3f(-.2f, h * .7f, .1f), new Vector3f(0, h, 0), 10);
        for (int i = 0; i < n; i++) {
            float a = i * Mth.TWO_PI / n + BossMesh.hash(seed + i) * .6f;
            float y0 = h * (.45f + .5f * i / n);
            Vector3f from = new Vector3f(Mth.cos(a) * .05f, y0, Mth.sin(a) * .05f);
            float len = (1.7f - .6f * i / n) * g;
            float sway = .05f * Mth.sin(age * .05f + i);
            Vector3f to = new Vector3f(Mth.cos(a + sway) * len, y0 + .5f * len, Mth.sin(a + sway) * len);
            out[i + 1] = Sculpt.curve(from, new Vector3f(from).lerp(to, .5f).add(0, .35f * len, 0), to, 7);
        }
        return out;
    }

    // ---------------------------------------------------------------- thorns

    /** Round the caster (the pose's origin), {@code radius} round; the thorns burst out at {@code rise}. */
    public static void thorns(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int life, int rise, float radius, int seed, int light) {
        PoseStack.Pose p = poseStack.last();
        float hit = age - rise;
        // The ring of green running out
        VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
        float rt = BossMesh.ramp(age, 0, rise + 2);
        if (rt < 1) {
            float rr = radius * rt;
            BossMesh.annulus(ring, p, new Vector3f(0, .12f, 0), U, V, Math.max(.01f, rr - 1), rr, 56, 1, 0, 0, BossMesh.glow(EMERALD, 0), BossMesh.glow(LIGHT, .9f), light,
                    true);
        }
        if (hit < 0) {
            return;
        }
        float out = BossMesh.ramp(hit, 0, 3) * (1 - BossMesh.ramp(age, life - 10, life));
        VertexConsumer glass = BossMesh.see(buffer, GemMesh.FACET);
        Vector3f[] spots = new Vector3f[28];
        for (int i = 0; i < spots.length; i++) {
            float a = BossMesh.hash(seed + i * 7) * Mth.TWO_PI;
            float d = radius * (.25f + .75f * (float) Math.sqrt(BossMesh.hash(seed + i * 11)));
            spots[i] = new Vector3f(Mth.cos(a) * d, 0, Mth.sin(a) * d);
            for (int c = 0; c < 3; c++) {
                float b = a + c * 2.1f + i;
                Vector3f lean = new Vector3f(Mth.cos(b), 0, Mth.sin(b)).mul(c == 0 ? .15f : .5f).add(0, 1, 0).normalize();
                float size = (c == 0 ? .42f : .28f) * out;
                float stretch = c == 0 ? 4f : 3f;
                Vector3f at = new Vector3f(spots[i]).add(new Vector3f(lean).mul(size * stretch * .45f)).add(0, -.1f, 0);
                GemMesh.gem(glass, p, GemMesh.Cut.CRYSTAL, at, RubyModels.along(lean, i + c), size, stretch, c == 0 ? EMERALD : BossMesh.mix(EMERALD, 0x0A5A32, .4f), .85f,
                        age * 3 + i * 5 + c, light);
            }
        }
        // Streams of life running back from the thorns to the caster
        if (hit < 16) {
            VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
            float s = 1 - hit / 16;
            Vector3f heart = new Vector3f(0, 1.1f, 0);
            for (int i = 0; i < spots.length; i += 2) {
                Vector3f from = new Vector3f(spots[i]).add(0, .8f, 0);
                Vector3f[] path = Sculpt.curve(from, new Vector3f(from).lerp(heart, .5f).add(0, 1.2f, 0), heart, 10);
                int[] col = new int[path.length];
                for (int j = 0; j < path.length; j++) {
                    float t = (float) j / (path.length - 1);
                    float head = Mth.clamp(1 - Math.abs(t - hit / 10f) * 3, 0, 1);
                    col[j] = BossMesh.glow(LIGHT, s * (.15f + .85f * head));
                }
                SandMesh.ribbon(beam, p, view.eye(), path, Sculpt.fill(.12f, path.length), 1, 0, col, light);
            }
            VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
            RitualMesh.billboard(glow, poseStack, view, heart, 2.5f, 0, -1, BossMesh.glow(LIGHT, .6f * s), light);
        }
    }
}
