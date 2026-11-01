package com.waylays.arcanes.client.render.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.fire.FireMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * The Executioner's Wheel: a great wheel of black iron, its rim ringed with curved blades and wrapped in red fire, a
 * circle of runes turning at its hub. It forms at the head of its strip spinning faster and faster, then rolls down the
 * whole of it, cutting a burning furrow and throwing sparks behind; at the end it bursts apart in fire.
 */
public final class WheelModel {
    private static final int BLADES = 8;
    private static final int SPOKES = 6;
    private static final int RIM = 40;
    /** How long it takes to burst at the end (ticks). */
    public static final int BURST = 12;

    private WheelModel() {}

    /**
     * Drawn from the head of its strip (the pose's origin, on the ground); {@code run} the strip (to its far end),
     * {@code radius} the wheel's, {@code age} ticks since it formed, {@code rolled} 0 to 1 how far down the strip it
     * has rolled, {@code since} ticks since it reached the end (negative before).
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f run, float radius, float age, float rolled,
                            float since, int seed, int light) {
        float length = run.length();
        Vector3f fwd = new Vector3f(run.x, 0, run.z).normalize();
        Vector3f up = new Vector3f(0, 1, 0);
        Vector3f axis = new Vector3f(up).cross(fwd).normalize();
        float form = BossMesh.ramp(age, 0, 16);
        float burst = BossMesh.ramp(since, 0, BURST);
        float r = radius * (.2f + .8f * form) * (1 + .25f * burst);
        float s = length * rolled;
        Vector3f centre = new Vector3f(fwd).mul(s).add(0, r, 0);
        // Spinning up where it formed, then rolling (a turn for every 2 pi radii it rolls)
        float spin = .004f * age * age + s / Math.max(.5f, radius);
        float fade = 1 - burst;

        if (fade > .01f) {
            body(poseStack, buffer, view, centre, fwd, up, axis, r, spin, fade, age, seed, light);
        }
        // Fire wrapped round it, streaming back as it rolls
        FireMesh.Batch fire = new FireMesh.Batch(view, 0, age, light);
        float rolling = rolled > 0 && rolled < 1 ? 1 : 0;
        for (int k = 0; k < 14; k++) {
            float a = k * Mth.TWO_PI / 14 - spin;
            Vector3f at = new Vector3f(fwd).mul(Mth.cos(a) * r).add(new Vector3f(up).mul(Mth.sin(a) * r)).add(centre);
            Vector3f stream = new Vector3f(up).mul(.8f).add(new Vector3f(fwd).mul(-.9f * rolling - .1f)).normalize();
            fire.lickAlong(at, stream, .95f * r / 2.6f, (1.5f + 1.1f * BossMesh.hash(seed + k)) * (r / 2.6f) * (1 + .6f * rolling), seed + k * 5,
                    (.7f + .3f * form) * fade);
        }
        // The furrow it burns down the strip, cooling behind it
        if (s > .1f) {
            int n = Math.max(2, (int) (s / .8f));
            Vector3f[] base = new Vector3f[n + 1];
            float[] h = new float[n + 1], al = new float[n + 1];
            for (int i = 0; i <= n; i++) {
                float x = s * i / n;
                base[i] = new Vector3f(fwd).mul(x).add(0, .05f, 0);
                float behind = s - x;
                al[i] = (float) Math.exp(-behind / 7) * (1 - .6f * burst) * Mth.clamp(behind * 2, 0, 1);
                h[i] = .9f * al[i];
            }
            fire.curtain(base, h, al, .55f, seed + 77);
            VertexConsumer furrow = BossMesh.light(buffer, BossMesh.NOISE);
            PoseStack.Pose p = poseStack.last();
            for (int i = 0; i < n; i++) {
                float k0 = .9f * (float) Math.exp(-(s - s * i / n) / 10) * (1 - burst * .5f), k1 = .9f * (float) Math.exp(-(s - s * (i + 1) / n) / 10) * (1 - burst * .5f);
                Vector3f a = new Vector3f(base[i]).add(new Vector3f(axis).mul(.75f)), b = new Vector3f(base[i]).sub(new Vector3f(axis).mul(.75f));
                Vector3f d = new Vector3f(base[i + 1]).sub(new Vector3f(axis).mul(.75f)), e = new Vector3f(base[i + 1]).add(new Vector3f(axis).mul(.75f));
                float v0 = s * i / n * .3f - age * .05f, v1 = s * (i + 1) / n * .3f - age * .05f;
                BossMesh.faced(furrow, p, a, b, d, e, new float[] {0, v0, 1, v0, 1, v1, 0, v1}, BossMesh.glow(0xFF3020, k0), BossMesh.glow(0xFF3020, k0),
                        BossMesh.glow(0xFF6030, k1), BossMesh.glow(0xFF6030, k1), light, up);
            }
        }
        fire.flush(buffer, poseStack);

        // Sparks thrown up behind it where it bites the ground
        if (rolling > 0 || (since > 0 && since < 6)) {
            VertexConsumer sparks = BossMesh.light(buffer, BossMesh.MOTE);
            Vector3f contact = new Vector3f(fwd).mul(s);
            for (int k = 0; k < 18; k++) {
                float life = (age * .09f + BossMesh.hash(seed + k * 3)) % 1;
                float sideways = (BossMesh.hash(seed + k * 7) - .5f) * 1.6f;
                Vector3f v = new Vector3f(fwd).mul(-.35f - .2f * BossMesh.hash(seed + k * 5)).add(0, .28f + .2f * BossMesh.hash(seed + k * 11), 0)
                        .add(new Vector3f(axis).mul(sideways * .2f));
                Vector3f at = WaterMesh.thrown(contact, v, life * 12, .05f);
                if (at.y > 0) {
                    RitualMesh.billboard(sparks, poseStack, view, at, .3f * (1 - life), 0, -1, BossMesh.glow(life < .4f ? 0xFFE0A0 : 0xFF5030, 1 - life), light);
                }
            }
        }
        // Bursting apart at the end of its run
        if (since > 0 && since < BURST) {
            VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
            RitualMesh.billboard(glow, poseStack, view, centre, r * (2.5f + 3 * burst), 0, -1, BossMesh.glow(0xFF6040, 1 - burst), light);
            RitualMesh.billboard(glow, poseStack, view, centre, r * 1.5f, 0, -1, BossMesh.glow(0xFFFFFF, (1 - burst) * (1 - burst)), light);
        }
    }

    private static void body(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f centre, Vector3f fwd, Vector3f up, Vector3f axis,
                             float r, float spin, float fade, float age, int seed, int light) {
        // The rim and an inner ring: tubes of black iron
        VertexConsumer iron = BossMesh.see(buffer, BossMesh.IRON);
        PoseStack.Pose p = poseStack.last();
        int metal = BossMesh.argb(0xC8B8B8, fade);
        ring(iron, p, centre, fwd, up, r, r * .085f, spin, metal, light);
        ring(iron, p, centre, fwd, up, r * .62f, r * .05f, spin, metal, light);
        // Spokes and hub
        for (int k = 0; k < SPOKES; k++) {
            float a = k * Mth.TWO_PI / SPOKES - spin;
            Vector3f d = new Vector3f(fwd).mul(Mth.cos(a)).add(new Vector3f(up).mul(Mth.sin(a)));
            Vector3f mid = new Vector3f(d).mul(r * .55f).add(centre);
            BossMesh.box(iron, p, mid, new Vector3f(d).mul(r * .45f), new Vector3f(d).cross(axis).normalize().mul(r * .045f), new Vector3f(axis).mul(r * .045f),
                    metal, light);
        }
        BossMesh.box(iron, p, centre, new Vector3f(fwd).mul(r * .16f), new Vector3f(up).mul(r * .16f), new Vector3f(axis).mul(r * .2f), metal, light);
        // The blades: curved, sweeping back from the rim against its turning
        VertexConsumer steel = BossMesh.see(buffer, BossMesh.SWORD);
        p = poseStack.last();
        int blade = BossMesh.argb(0x9A8080, .95f * fade);
        Vector3f[][] edges = new Vector3f[BLADES][];
        for (int k = 0; k < BLADES; k++) {
            float a0 = k * Mth.TWO_PI / BLADES - spin;
            int n = 6;
            Vector3f[] lead = new Vector3f[n + 1], trail = new Vector3f[n + 1];
            for (int i = 0; i <= n; i++) {
                float t = (float) i / n;
                float a = a0 + .55f * t;
                float rr = r * (1 + .5f * t);
                float w = r * .2f * (float) Math.pow(1 - t, .7f);
                Vector3f out = new Vector3f(fwd).mul(Mth.cos(a)).add(new Vector3f(up).mul(Mth.sin(a)));
                Vector3f tan = new Vector3f(fwd).mul(-Mth.sin(a)).add(new Vector3f(up).mul(Mth.cos(a)));
                Vector3f mid = new Vector3f(out).mul(rr).add(centre);
                lead[i] = new Vector3f(tan).mul(-w * .5f).add(mid);
                trail[i] = new Vector3f(tan).mul(w * .5f).add(mid);
            }
            for (int i = 0; i < n; i++) {
                float v0 = (float) i / n, v1 = (float) (i + 1) / n;
                BossMesh.quad2(steel, p, lead[i], trail[i], trail[i + 1], lead[i + 1], 0, 1 - v0, 1, 1 - v0, 1, 1 - v1, 0, 1 - v1, blade, blade, light);
            }
            edges[k] = lead;
        }
        // Their edges, white-hot
        VertexConsumer edge = BossMesh.light(buffer, BossMesh.BEAM);
        p = poseStack.last();
        for (Vector3f[] lead : edges) {
            int[] col = new int[lead.length];
            for (int i = 0; i < lead.length; i++) {
                col[i] = BossMesh.glow(i > lead.length - 3 ? 0xFFFFFF : 0xFF5A3A, fade * (.6f + .4f * i / (lead.length - 1)));
            }
            RitualMesh.thread(edge, p, view.eye(), lead, r * .09f, col, light);
        }
        // Runes turning at its hub, both faces
        VertexConsumer runes = BossMesh.light(buffer, BossMesh.RUNES);
        p = poseStack.last();
        float cs = Mth.cos(-spin), sn = Mth.sin(-spin);
        Vector3f ru = new Vector3f(fwd).mul(cs).add(new Vector3f(up).mul(sn)), rv = new Vector3f(fwd).mul(-sn).add(new Vector3f(up).mul(cs));
        for (int side = -1; side <= 1; side += 2) {
            Vector3f at = new Vector3f(axis).mul(side * r * .21f).add(centre);
            BossMesh.plate(runes, p, at, ru, rv, r * 1.15f, BossMesh.glow(0xFF4030, fade * (.75f + .25f * Mth.sin(age * .3f))), light);
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, centre, r * 1.1f, 0, -1, BossMesh.glow(0xFF4030, .55f * fade), light);
        RitualMesh.billboard(glow, poseStack, view, centre, r * 3.4f, 0, -1, BossMesh.glow(0xB01810, .35f * fade), light);
    }

    /** A tube bent into a circle of {@code r} round {@code centre} in the plane of {@code u} and {@code v}. */
    private static void ring(VertexConsumer c, PoseStack.Pose p, Vector3f centre, Vector3f u, Vector3f v, float r, float thick, float spin, int argb, int light) {
        Vector3f[] axis = new Vector3f[RIM + 1];
        float[] rad = new float[RIM + 1];
        int[] col = new int[RIM + 1];
        for (int i = 0; i <= RIM; i++) {
            float a = i * Mth.TWO_PI / RIM - spin;
            axis[i] = new Vector3f(u).mul(Mth.cos(a) * r).add(new Vector3f(v).mul(Mth.sin(a) * r)).add(centre);
            rad[i] = thick;
            col[i] = argb;
        }
        // Close the loop smoothly: the tube works its ends' tangents from their neighbours
        axis[RIM] = new Vector3f(axis[0]);
        WaterMesh.tube(c, p, axis, rad, col, 6, 0, 1, 0, .8f, 0, 0, light);
    }
}
