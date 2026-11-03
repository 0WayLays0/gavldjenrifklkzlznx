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
        // The crystal: a long eight-sided prism drawn to a point at both ends, see-through, its facets lit from within
        int sides = 8;
        Vector3f top = new Vector3f(heart).add(0, HALF * 1.25f * s, 0), bottom = new Vector3f(heart).add(0, -HALF * 1.25f * s, 0);
        Vector3f[] upper = new Vector3f[sides], lower = new Vector3f[sides];
        for (int k = 0; k < sides; k++) {
            float a = spin + k * Mth.TWO_PI / sides;
            upper[k] = new Vector3f(Mth.cos(a) * 1.35f * s, HALF * .45f * s, Mth.sin(a) * 1.35f * s).add(heart);
            lower[k] = new Vector3f(Mth.cos(a) * 1.35f * s, -HALF * .45f * s, Mth.sin(a) * 1.35f * s).add(heart);
        }
        for (int pass = 0; pass < 2; pass++) {
            VertexConsumer c = pass == 0 ? BossMesh.see(buffer, BossMesh.PRISM) : BossMesh.light(buffer, BossMesh.PRISM);
            PoseStack.Pose p = poseStack.last();
            for (int k = 0; k < sides; k++) {
                int j = (k + 1) % sides;
                int tint = pass == 0 ? BossMesh.argb(BossMesh.mix(0xE6F4FF, COLOURS[(k + (int) (age / 6)) % COLOURS.length], .3f), .5f)
                        : BossMesh.glow(BossMesh.mix(0xBFE6FF, COLOURS[(k + (int) (age / 5)) % COLOURS.length], .4f), .4f + .2f * Mth.sin(age * .2f + k));
                Vector3f mid = new Vector3f(upper[k]).add(upper[j]).mul(.5f).sub(heart);
                BossMesh.faced(c, p, upper[k], upper[j], top, top, new float[] {0, 1, 1, 1, .5f, 0, .5f, 0}, tint, tint, tint, tint, light,
                        new Vector3f(mid).add(new Vector3f(top).sub(heart).mul(.3f)));
                BossMesh.faced(c, p, lower[k], upper[k], upper[j], lower[j], new float[] {0, 1, 0, 0, 1, 0, 1, 1}, tint, tint, tint, tint, light, new Vector3f(mid.x, 0,
                        mid.z));
                BossMesh.faced(c, p, lower[j], lower[k], bottom, bottom, new float[] {1, 0, 0, 0, .5f, 1, .5f, 1}, tint, tint, tint, tint, light,
                        new Vector3f(mid.x, 0, mid.z).add(new Vector3f(bottom).sub(heart).mul(.3f)));
            }
        }
        // Its golden cage: three rings turning about it, tilted every way, and a crown and a foot of gold
        VertexConsumer gold = BossMesh.solid(buffer, BellModel.GOLD);
        for (int r = 0; r < 3; r++) {
            poseStack.pushPose();
            poseStack.translate(heart.x, heart.y, heart.z);
            poseStack.mulPose(new org.joml.Quaternionf().rotationY(age * (.02f + r * .012f) + r * 2.1f).rotateX(.5f + r * .55f).rotateZ(age * .01f * (r - 1)));
            Sculpt.torus(gold, poseStack.last(), (3.0f + r * .45f) * s, .1f * s, 56, 6, 0, Mth.TWO_PI, 12, 0xFFFFFFFF, light);
            for (int k = 0; k < 4; k++) {
                float a = k * Mth.HALF_PI;
                poseStack.pushPose();
                poseStack.translate(Mth.cos(a) * (3.0f + r * .45f) * s, 0, Mth.sin(a) * (3.0f + r * .45f) * s);
                Sculpt.gem(gold, poseStack.last(), .2f * s, .12f * s, .2f * s, 6, 0xFFFFFFFF, light);
                poseStack.popPose();
            }
            poseStack.popPose();
        }
        for (int end = -1; end <= 1; end += 2) {
            poseStack.pushPose();
            poseStack.translate(heart.x, heart.y + end * HALF * .45f * s, heart.z);
            poseStack.mulPose(new org.joml.Quaternionf().rotationY(spin));
            Sculpt.torus(gold, poseStack.last(), 1.42f * s, .1f * s, 8, 5, 0, Mth.TWO_PI, 4, 0xFFFFFFFF, light);
            poseStack.popPose();
        }
        // Small crystals orbiting it, one of each colour
        for (int pass = 0; pass < 2; pass++) {
            VertexConsumer c = pass == 0 ? BossMesh.see(buffer, BossMesh.PRISM) : BossMesh.light(buffer, BossMesh.PRISM);
            for (int k = 0; k < COLOURS.length; k++) {
                float a = -age * .03f + k * Mth.TWO_PI / COLOURS.length;
                poseStack.pushPose();
                poseStack.translate(heart.x + Mth.cos(a) * 4.6f * s, heart.y + Mth.sin(a * 2 + k) * .8f * s, heart.z + Mth.sin(a) * 4.6f * s);
                poseStack.mulPose(new org.joml.Quaternionf().rotationY(age * .1f + k).rotateX(.4f));
                int tint = pass == 0 ? BossMesh.argb(BossMesh.mix(COLOURS[k], 0xFFFFFF, .3f), .7f) : BossMesh.glow(COLOURS[k], .7f);
                Sculpt.gem(c, poseStack.last(), .45f * s, .5f * s, .7f * s, 6, tint, light);
                poseStack.popPose();
            }
        }
        // Its edges, catching every colour
        VertexConsumer edge = BossMesh.light(buffer, BossMesh.BEAM);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < sides; k++) {
            int c = BossMesh.glow(COLOURS[(k + (int) (age / 4)) % COLOURS.length], .75f);
            int w = BossMesh.glow(0xFFFFFF, .5f);
            RitualMesh.thread(edge, p, view.eye(), new Vector3f[] {top, upper[k]}, .12f, new int[] {w, c}, light);
            RitualMesh.thread(edge, p, view.eye(), new Vector3f[] {lower[k], bottom}, .12f, new int[] {c, w}, light);
            RitualMesh.thread(edge, p, view.eye(), new Vector3f[] {upper[k], lower[k]}, .1f, new int[] {c, c}, light);
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
