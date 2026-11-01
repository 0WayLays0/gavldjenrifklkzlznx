package com.waylays.arcanes.client.render.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * The Divine Prism: a great crystal hangs over its caster, turning, drawing motes of every colour into its heart. Then
 * it splits the light: one after the other, beams of red, orange, gold, green, blue and violet lance down from its
 * point and sweep out along the ground, each leaving a burning line of its colour, round and round like the hand of a
 * clock. Its light spent, it cracks into a flash.
 */
public final class PrismModel {
    public static final int[] COLOURS = {0xFF4A4A, 0xFF9A30, 0xFFE24A, 0x5AFF7A, 0x4AB4FF, 0xB86AFF};
    /** Ticks a beam takes to sweep from the prism's foot out to the end of its line (centred on when it strikes). */
    public static final int SWEEP = 8;
    /** How long it takes to break after its last beam (ticks). */
    public static final int BREAK = 16;
    private static final float HALF = 3f;

    private PrismModel() {}

    /**
     * Drawn from under the prism (the pose's origin, on the ground); {@code height} where it hangs, {@code age} ticks
     * since it formed; {@code first} ticks since its first beam struck (negative before), the {@code count} beams
     * {@code interval} ticks apart along the lines at angles {@code a0 + k * step} (radians, round from {@code +x}
     * toward {@code +z}), {@code length} long.
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float height, float age, float first, int count, float a0,
                            float step, float interval, float length, int seed, int light) {
        float form = BossMesh.ramp(age, 0, 20);
        float sinceLast = first - (count - 1) * interval;
        float breaking = BossMesh.ramp(sinceLast, SWEEP, SWEEP + BREAK);
        float spin = age * .045f + age * age * .0004f;
        Vector3f heart = new Vector3f(0, height + .25f * Mth.sin(age * .08f), 0);
        float s = form * (1 + .3f * breaking) * (1 - breaking * breaking);

        // The beams, their burning lines, where they strike
        for (int k = 0; k < count; k++) {
            float since = first - k * interval;
            if (since < -SWEEP * .5f || since > 60) {
                continue;
            }
            int colour = COLOURS[k % COLOURS.length];
            float a = a0 + k * step;
            Vector3f dir = new Vector3f(Mth.cos(a), 0, Mth.sin(a));
            float sweep = Mth.clamp((since + SWEEP * .5f) / SWEEP, 0, 1);
            float reach = 1.2f + (length - 1.2f) * sweep;
            float cool = 1 - BossMesh.ramp(since, SWEEP, 60);
            // The line it burns
            VertexConsumer scorch = BossMesh.light(buffer, BossMesh.NOISE);
            PoseStack.Pose p = poseStack.last();
            Vector3f side = new Vector3f(-dir.z, 0, dir.x).mul(.55f);
            int n = Math.max(2, (int) (reach / 1.2f));
            for (int i = 0; i < n; i++) {
                float r0 = 1.2f + (reach - 1.2f) * i / n, r1 = 1.2f + (reach - 1.2f) * (i + 1) / n;
                Vector3f c0 = new Vector3f(dir).mul(r0).add(0, .06f, 0), c1 = new Vector3f(dir).mul(r1).add(0, .06f, 0);
                float k0 = cool * (.55f + .45f * (float) Math.exp(-(reach - r0) / 3)), k1 = cool * (.55f + .45f * (float) Math.exp(-(reach - r1) / 3));
                float v0 = r0 * .25f - age * .03f, v1 = r1 * .25f - age * .03f;
                BossMesh.faced(scorch, p, new Vector3f(c0).add(side), new Vector3f(c0).sub(side), new Vector3f(c1).sub(side), new Vector3f(c1).add(side),
                        new float[] {1, v0, 0, v0, 0, v1, 1, v1}, BossMesh.glow(colour, k0), BossMesh.glow(colour, k0), BossMesh.glow(colour, k1),
                        BossMesh.glow(colour, k1), light, BossMesh.UP);
            }
            // The beam itself, while it sweeps
            float on = since < SWEEP ? 1 : 1 - BossMesh.ramp(since, SWEEP, SWEEP + 4);
            if (on > 0 && s > .05f) {
                Vector3f from = new Vector3f(heart).add(0, -HALF * s, 0);
                Vector3f hit = new Vector3f(dir).mul(reach).add(0, .1f, 0);
                VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
                RitualMesh.shaft(beam, poseStack.last(), view.eye(), from, hit, 1.3f, BossMesh.glow(colour, .9f * on), light);
                RitualMesh.shaft(beam, poseStack.last(), view.eye(), from, hit, .45f, BossMesh.glow(0xFFFFFF, .85f * on), light);
                VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
                RitualMesh.billboard(glow, poseStack, view, new Vector3f(hit).add(0, .3f, 0), 3.2f, 0, -1, BossMesh.glow(colour, on), light);
                RitualMesh.billboard(glow, poseStack, view, new Vector3f(hit).add(0, .3f, 0), 1.3f, 0, -1, BossMesh.glow(0xFFFFFF, on), light);
                RitualMesh.billboard(glow, poseStack, view, from, 2.6f, 0, -1, BossMesh.glow(colour, .8f * on), light);
                VertexConsumer sparks = BossMesh.light(buffer, BossMesh.MOTE);
                for (int j = 0; j < 8; j++) {
                    float life = (age * .15f + BossMesh.hash(seed + k * 31 + j)) % 1;
                    float sa = BossMesh.hash(seed + j * 5 + k) * Mth.TWO_PI;
                    Vector3f v = new Vector3f(Mth.cos(sa) * .18f, .25f + .15f * BossMesh.hash(seed + j * 7), Mth.sin(sa) * .18f);
                    Vector3f at = WaterMesh.thrown(hit, v, life * 10, .05f);
                    RitualMesh.billboard(sparks, poseStack, view, at, .3f, 0, -1, BossMesh.glow(j % 2 == 0 ? 0xFFFFFF : colour, (1 - life) * on), light);
                }
            }
        }

        if (s <= .02f) {
            flash(poseStack, buffer, view, heart, breaking, light);
            return;
        }
        // Motes of every colour drawn into its heart
        VertexConsumer motes = BossMesh.light(buffer, BossMesh.MOTE);
        float charge = first < 0 ? 1 : 1 - BossMesh.ramp(first, 0, 10);
        for (int k = 0; k < 24 && charge > 0; k++) {
            float t = (age * (.03f + .015f * BossMesh.hash(seed + k)) + BossMesh.hash(seed + k * 3)) % 1;
            float a = BossMesh.hash(seed + k * 7) * Mth.TWO_PI + t * 2.5f;
            float r = 6 * (1 - t);
            Vector3f at = new Vector3f(Mth.cos(a) * r, (BossMesh.hash(seed + k * 11) - .4f) * 5 * (1 - t), Mth.sin(a) * r).add(heart);
            RitualMesh.billboard(motes, poseStack, view, at, .4f, 0, -1, BossMesh.glow(COLOURS[k % COLOURS.length], Mth.sin(t * Mth.PI) * charge * form), light);
        }
        // The crystal: two pyramids base to base, see-through, its facets lit from within
        Vector3f top = new Vector3f(heart).add(0, HALF * s, 0), bottom = new Vector3f(heart).add(0, -HALF * s, 0);
        Vector3f[] ring = new Vector3f[4];
        for (int k = 0; k < 4; k++) {
            float a = spin + k * Mth.HALF_PI;
            ring[k] = new Vector3f(Mth.cos(a) * 1.8f * s, 0, Mth.sin(a) * 1.8f * s).add(heart);
        }
        for (int pass = 0; pass < 2; pass++) {
            VertexConsumer c = pass == 0 ? BossMesh.see(buffer, BossMesh.PRISM) : BossMesh.light(buffer, BossMesh.PRISM);
            PoseStack.Pose p = poseStack.last();
            for (int k = 0; k < 4; k++) {
                Vector3f a = ring[k], b = ring[(k + 1) % 4];
                int tint = pass == 0 ? BossMesh.argb(BossMesh.mix(0xE6F4FF, COLOURS[(k + (int) (age / 6)) % COLOURS.length], .25f), .55f)
                        : BossMesh.glow(BossMesh.mix(0xBFE6FF, COLOURS[(k * 2 + (int) (age / 5)) % COLOURS.length], .35f), .45f + .2f * Mth.sin(age * .2f + k));
                Vector3f mid = new Vector3f(a).add(b).mul(.5f);
                BossMesh.faced(c, p, a, b, top, top, new float[] {0, 1, 1, 1, .5f, 0, .5f, 0}, tint, tint, tint, tint, light,
                        new Vector3f(mid).sub(heart).add(new Vector3f(top).sub(heart).mul(.3f)));
                BossMesh.faced(c, p, b, a, bottom, bottom, new float[] {1, 0, 0, 0, .5f, 1, .5f, 1}, tint, tint, tint, tint, light,
                        new Vector3f(mid).sub(heart).add(new Vector3f(bottom).sub(heart).mul(.3f)));
            }
        }
        // Its edges, catching every colour
        VertexConsumer edge = BossMesh.light(buffer, BossMesh.BEAM);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < 4; k++) {
            int c = BossMesh.glow(COLOURS[(k + (int) (age / 4)) % COLOURS.length], .8f);
            int w = BossMesh.glow(0xFFFFFF, .5f);
            RitualMesh.thread(edge, p, view.eye(), new Vector3f[] {top, ring[k]}, .16f, new int[] {w, c}, light);
            RitualMesh.thread(edge, p, view.eye(), new Vector3f[] {ring[k], bottom}, .16f, new int[] {c, w}, light);
            RitualMesh.thread(edge, p, view.eye(), new Vector3f[] {ring[k], ring[(k + 1) % 4]}, .16f, new int[] {c, c}, light);
        }
        // Its heart, a halo round it
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, heart, 3.6f * s, 0, -1, BossMesh.glow(0xFFFFFF, .85f), light);
        RitualMesh.billboard(glow, poseStack, view, heart, 10 * s, 0, -1, BossMesh.glow(0xCFE8FF, .35f), light);
        VertexConsumer halo = BossMesh.light(buffer, BossMesh.RING);
        float hs = Mth.sin(age * .05f) * .3f;
        BossMesh.plate(halo, poseStack.last(), heart, new Vector3f(Mth.cos(hs), Mth.sin(hs) * .3f, 0).normalize(), new Vector3f(0, -Mth.sin(hs) * .3f, 1).normalize(),
                7 * s, BossMesh.glow(0xFFF0C0, .6f * form), light);
        flash(poseStack, buffer, view, heart, breaking, light);
    }

    private static void flash(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f heart, float breaking, int light) {
        if (breaking <= 0) {
            return;
        }
        float f = Math.max(0, 1 - Math.abs(breaking - .75f) * 4);
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, heart, 3 + 9 * f, 0, -1, BossMesh.glow(0xFFFFFF, f), light);
        VertexConsumer motes = BossMesh.light(buffer, BossMesh.MOTE);
        for (int k = 0; k < 18; k++) {
            float a = k * 2.4f;
            float r = 6 * breaking;
            Vector3f at = new Vector3f(Mth.cos(a) * r, Mth.sin(k * 1.3f) * r * .6f, Mth.sin(a) * r).add(heart);
            RitualMesh.billboard(motes, poseStack, view, at, .5f, 0, -1, BossMesh.glow(COLOURS[k % COLOURS.length], 1 - breaking), light);
        }
    }
}
