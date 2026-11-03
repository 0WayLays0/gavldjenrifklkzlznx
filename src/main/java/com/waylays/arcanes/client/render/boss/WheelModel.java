package com.waylays.arcanes.client.render.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.fire.FireMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraft.resources.ResourceLocation;
import com.waylays.arcanes.ArcanesOublies;

/**
 * The Executioner's Wheel: a great wheel of black iron, its rim ringed with curved blades and wrapped in red fire, a
 * circle of runes turning at its hub. It forms at the head of its strip spinning faster and faster, then rolls down the
 * whole of it, cutting a burning furrow and throwing sparks behind; at the end it bursts apart in fire.
 */
public final class WheelModel {
    private static final ResourceLocation IRON = BellModel.IRON, GOLD = BellModel.GOLD;
    private static final ResourceLocation STEEL = ArcanesOublies.id("textures/entity/boss/m_steel.png");
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
        // The wheel's own space: its axle along Y, turned by its spin
        Quaternionf frame = new Matrix3f(new Vector3f(fwd), new Vector3f(axis), new Vector3f(fwd).cross(axis).normalize()).getNormalizedRotation(new Quaternionf())
                .rotateY(spin);
        // ---- iron: the rim, an inner ring, the spokes, the hub
        VertexConsumer iron = BossMesh.solid(buffer, IRON);
        place(poseStack, centre, frame);
        PoseStack.Pose p = poseStack.last();
        Sculpt.torus(iron, p, r, r * .09f, 64, 8, 0, Mth.TWO_PI, 12, 0xFFFFFFFF, light);
        Sculpt.torus(iron, p, r * .62f, r * .05f, 48, 6, 0, Mth.TWO_PI, 8, 0xFFD8D0E0, light);
        for (int k = 0; k < SPOKES; k++) {
            float a = k * Mth.TWO_PI / SPOKES;
            poseStack.pushPose();
            poseStack.mulPose(new Quaternionf().rotationY(-a));
            poseStack.translate(r * .55f, 0, 0);
            Sculpt.block(iron, poseStack.last(), r * .9f, r * .07f, r * .1f, r * .015f, .5f, 0xFFFFFFFF, light);
            poseStack.popPose();
        }
        Sculpt.lathe(iron, p, new float[] {0, -r * .26f, r * .16f, -r * .25f, r * .2f, -r * .16f, r * .2f, r * .16f, r * .16f, r * .25f, 0, r * .26f}, 20, 2, 1,
                0xFFFFFFFF, 0xFFFFFFFF, light);
        poseStack.popPose();
        // ---- gold: studs round the rim, caps on the spokes, the hub's spikes
        VertexConsumer gold = BossMesh.solid(buffer, GOLD);
        place(poseStack, centre, frame);
        p = poseStack.last();
        for (int k = 0; k < 16; k++) {
            float a = (k + .5f) * Mth.TWO_PI / 16;
            for (int side = -1; side <= 1; side += 2) {
                poseStack.pushPose();
                poseStack.translate(Mth.cos(a) * r, side * r * .085f, Mth.sin(a) * r);
                poseStack.mulPose(new Quaternionf().rotationX(side < 0 ? Mth.PI : 0));
                Sculpt.lathe(gold, poseStack.last(), new float[] {r * .05f, 0, r * .045f, r * .025f, 0, r * .035f}, 8, 1, 1, 0xFFFFFFFF, 0xFFFFFFFF, light);
                poseStack.popPose();
            }
        }
        for (int k = 0; k < SPOKES; k++) {
            float a = k * Mth.TWO_PI / SPOKES;
            for (float rr : new float[] {r * .23f, r * .62f, r * .9f}) {
                poseStack.pushPose();
                poseStack.translate(Mth.cos(a) * rr, 0, Mth.sin(a) * rr);
                Sculpt.torus(gold, poseStack.last(), r * .055f, r * .025f, 10, 5, 0, Mth.TWO_PI, 1, 0xFFFFFFFF, light);
                poseStack.popPose();
            }
        }
        for (int side = -1; side <= 1; side += 2) {
            BossMesh.horn(gold, p, new Vector3f(0, side * r * .25f, 0), new Vector3f(0, side, 0), new Vector3f(), r * .32f, r * .09f, 8, 3, 0xFFFFFFFF, 0xFFFFF0C0,
                    light);
            Sculpt.torus(gold, p, r * .2f, r * .03f, 20, 5, 0, Mth.TWO_PI, 3, 0xFFFFFFFF, light);
        }
        poseStack.popPose();
        // ---- steel: the blades, curved, sweeping back from the rim against its turning
        VertexConsumer steel = BossMesh.solid(buffer, STEEL);
        place(poseStack, centre, frame);
        p = poseStack.last();
        Vector3f[][] edges = new Vector3f[BLADES][];
        for (int k = 0; k < BLADES; k++) {
            float a0 = k * Mth.TWO_PI / BLADES;
            int n = 7;
            Vector3f[] path = new Vector3f[n];
            float[] wid = new float[n], th = new float[n];
            Vector3f[] edge = new Vector3f[n];
            for (int i = 0; i < n; i++) {
                float t = (float) i / (n - 1);
                float a = a0 + .6f * t;
                float rr = r * (.95f + .55f * t);
                path[i] = new Vector3f(Mth.cos(a) * rr, 0, Mth.sin(a) * rr);
                wid[i] = r * .13f * (float) Math.pow(1 - t, .6f) + .01f;
                th[i] = r * .02f * (1 - t) + .005f;
                // The edge leads: toward the way the wheel turns
                edge[i] = new Vector3f(-Mth.sin(a), 0, Mth.cos(a)).mul(-wid[i]).add(path[i]);
            }
            Sculpt.sweep(steel, p, path, wid, th, Sculpt.WEDGE, true, new Vector3f(0, 0, 1), .5f / r, Sculpt.fill(0xFFE0D8D8, n), true, light);
            edges[k] = edge;
        }
        poseStack.popPose();
        // ---- light: the edges white-hot, the runes at the hub, the glow
        VertexConsumer edge = BossMesh.light(buffer, BossMesh.BEAM);
        for (Vector3f[] lead : edges) {
            Vector3f[] world = new Vector3f[lead.length];
            int[] col = new int[lead.length];
            for (int i = 0; i < lead.length; i++) {
                world[i] = frame.transform(new Vector3f(lead[i])).add(centre);
                col[i] = BossMesh.glow(i > lead.length - 3 ? 0xFFFFFF : 0xFF5A3A, fade * (.6f + .4f * i / (lead.length - 1)));
            }
            RitualMesh.thread(edge, poseStack.last(), view.eye(), world, r * .08f, col, light);
        }
        VertexConsumer runes = BossMesh.light(buffer, BossMesh.RUNES);
        p = poseStack.last();
        float cs = Mth.cos(-spin), sn = Mth.sin(-spin);
        Vector3f ru = new Vector3f(fwd).mul(cs).add(new Vector3f(up).mul(sn)), rv = new Vector3f(fwd).mul(-sn).add(new Vector3f(up).mul(cs));
        for (int side = -1; side <= 1; side += 2) {
            Vector3f at = new Vector3f(axis).mul(side * r * .14f).add(centre);
            BossMesh.plate(runes, p, at, ru, rv, r * 1.15f, BossMesh.glow(0xFF4030, fade * (.75f + .25f * Mth.sin(age * .3f))), light);
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, centre, r * 1.1f, 0, -1, BossMesh.glow(0xFF4030, .45f * fade), light);
        RitualMesh.billboard(glow, poseStack, view, centre, r * 3.4f, 0, -1, BossMesh.glow(0xB01810, .3f * fade), light);
    }

    private static void place(PoseStack poseStack, Vector3f centre, Quaternionf frame) {
        poseStack.pushPose();
        poseStack.translate(centre.x, centre.y, centre.z);
        poseStack.mulPose(frame);
    }

}
