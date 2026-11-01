package com.waylays.arcanes.client.render.sand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Djinn of the Sands: a spirit of whirling sand hovering at its master's shoulder: its tail a little whirlwind
 * spiralling down to the ground, a body and head of flowing sand, arms folded or flung out, golden cuffs at its wrists,
 * a turquoise jewel on its brow and eyes of turquoise light; when it hurls a bolt, its arm flings out and a stream of
 * sand races to its prey.
 */
public final class DjinnModel {
    private DjinnModel() {}

    /**
     * Under it (the pose's origin), facing {@code dir}; {@code age} of {@code life} ticks; {@code since} ticks since its last
     * bolt, which struck {@code target} (in the same frame, or null).
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f dir, float age, float life, float since, Vector3f target,
                            int seed, int light) {
        float appear = BossMesh.ramp(age, 0, 14);
        float vanish = BossMesh.ramp(age, life - 14, life);
        float k0 = appear * (1 - vanish);
        if (k0 < .01f) {
            return;
        }
        Vector3f fwd = new Vector3f(dir.x, 0, dir.z);
        if (fwd.lengthSquared() < 1.0E-6f) {
            fwd.set(0, 0, 1);
        }
        fwd.normalize();
        Vector3f right = new Vector3f(-fwd.z, 0, fwd.x);
        // Drawn a third larger than its sketch: everything below is in its own scale
        float scale = 1.3f;
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        view = new WaterMesh.View(view.camera(), new Vector3f(view.eye()).div(scale));
        if (target != null) {
            target = new Vector3f(target).div(scale);
        }
        float bob = .15f * Mth.sin(age * .1f);
        float hover = (1 - appear) * -1.5f + vanish * 1.2f;
        Vector3f chest = new Vector3f(0, 2.3f + bob + hover, 0);
        Vector3f headAt = new Vector3f(chest).add(0, .85f, 0);
        float throwing = target != null && since < 12 ? Mth.sin(Math.min(1, since / 12) * Mth.PI) : 0;
        VertexConsumer flow = BossMesh.see(buffer, SandMesh.FLOW);
        PoseStack.Pose p = poseStack.last();
        // Its tail: a little whirlwind of sand down to the ground
        for (int k = 0; k < 4; k++) {
            int n = 14;
            Vector3f[] pts = new Vector3f[n + 1];
            float[] w = new float[n + 1];
            int[] col = new int[n + 1];
            for (int i = 0; i <= n; i++) {
                float t = (float) i / n;
                float a = t * 7 + age * .45f + k * Mth.HALF_PI;
                float r = .08f + .42f * t * t;
                pts[i] = new Vector3f(Mth.cos(a) * r, (chest.y - .4f) * t, Mth.sin(a) * r);
                w[i] = .1f + .22f * t;
                col[i] = BossMesh.argb(k % 2 == 0 ? SandMesh.SAND : SandMesh.SAND_PALE, .75f * k0 * Math.min(1, t * 3));
            }
            SandMesh.ribbon(flow, p, view.eye(), pts, w, 1.5f, age * .4f, col, light);
        }
        // Its body and head of flowing sand
        int body = BossMesh.argb(0xFFFFFF, .95f * k0);
        SandMesh.ellipsoid(flow, poseStack, chest, .48f, .62f, .34f, (float) Math.atan2(fwd.x, fwd.z), age * .02f, body, light);
        SandMesh.ellipsoid(flow, poseStack, new Vector3f(chest).add(0, -.55f, 0), .32f, .3f, .26f, 0, age * .03f, body, light);
        SandMesh.ellipsoid(flow, poseStack, headAt, .27f, .32f, .27f, 0, -age * .02f, body, light);
        // A sash of sand winding round its body
        for (int k = 0; k < 2; k++) {
            int n = 16;
            Vector3f[] pts = new Vector3f[n + 1];
            float[] w = new float[n + 1];
            int[] col = new int[n + 1];
            for (int i = 0; i <= n; i++) {
                float t = (float) i / n;
                float a = t * Mth.TWO_PI * 1.2f + age * .12f + k * Mth.PI;
                pts[i] = new Vector3f(Mth.cos(a) * (.6f + .1f * t), chest.y - .7f + 1.2f * t, Mth.sin(a) * (.5f + .1f * t));
                w[i] = .16f * Mth.sin(t * Mth.PI) + .02f;
                col[i] = BossMesh.argb(SandMesh.SAND_PALE, .8f * k0 * Mth.sin(t * Mth.PI));
            }
            SandMesh.ribbon(flow, p, view.eye(), pts, w, 1.4f, age * .3f + k, col, light);
        }
        // Its arms: folded, or one flung out at its prey
        for (int s = -1; s <= 1; s += 2) {
            Vector3f shoulder = new Vector3f(chest).add(new Vector3f(right).mul(s * .48f)).add(0, .32f, 0);
            Vector3f hand;
            if (s == 1 && throwing > 0) {
                Vector3f to = new Vector3f(target).sub(shoulder).normalize();
                Vector3f rest = new Vector3f(chest).add(new Vector3f(fwd).mul(.45f)).add(new Vector3f(right).mul(.1f));
                hand = new Vector3f(rest).lerp(new Vector3f(shoulder).add(new Vector3f(to).mul(1.25f)), throwing);
            } else {
                hand = new Vector3f(chest).add(new Vector3f(fwd).mul(.42f)).add(new Vector3f(right).mul(-s * .12f)).add(0, -.08f + .05f * s, 0);
            }
            Vector3f elbow = new Vector3f(shoulder).add(hand).mul(.5f).add(new Vector3f(right).mul(s * .4f)).add(0, -.25f, 0);
            Vector3f[] arm = SandMesh.curve(shoulder, elbow, hand, 8);
            float[] r = new float[arm.length];
            for (int i = 0; i < arm.length; i++) {
                r[i] = .16f - .07f * i / (arm.length - 1);
            }
            SandMesh.tube(flow, p, arm, r, 7, 1.2f, age * .2f, SandMesh.colours(body, arm.length), light);
        }
        // Golden cuffs at its wrists and a band at its brow; the jewel and its eyes
        VertexConsumer gold = BossMesh.solid(buffer, SandMesh.GOLD_SKIN);
        p = poseStack.last();
        for (int s = -1; s <= 1; s += 2) {
            Vector3f shoulder = new Vector3f(chest).add(new Vector3f(right).mul(s * .48f)).add(0, .32f, 0);
            Vector3f wrist = s == 1 && throwing > 0
                    ? new Vector3f(chest).add(new Vector3f(fwd).mul(.45f)).lerp(new Vector3f(shoulder).add(new Vector3f(target).sub(shoulder).normalize().mul(1.0f)), throwing)
                    : new Vector3f(chest).add(new Vector3f(fwd).mul(.36f)).add(new Vector3f(right).mul(-s * .02f)).add(0, -.05f + .05f * s, 0);
            SandMesh.ellipsoid(gold, poseStack, wrist, .13f, .13f, .13f, 0, 0, 0xFFFFFFFF, light);
        }
        int m = 16;
        Vector3f[] band = new Vector3f[m + 1];
        float[] br = new float[m + 1];
        for (int i = 0; i <= m; i++) {
            float a = i * Mth.TWO_PI / m;
            band[i] = new Vector3f(Mth.cos(a) * .28f, .12f, Mth.sin(a) * .28f).add(headAt);
            br[i] = .045f;
        }
        SandMesh.tube(gold, p, band, br, 5, 2, 0, SandMesh.colours(0xFFFFFFFF, m + 1), light);
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        Vector3f face = new Vector3f(headAt).add(new Vector3f(fwd).mul(.24f));
        for (int s = -1; s <= 1; s += 2) {
            RitualMesh.billboard(glow, poseStack, view, new Vector3f(face).add(new Vector3f(right).mul(s * .1f)).add(0, .02f, 0), .34f, 0, -1,
                    BossMesh.glow(SandMesh.TURQUOISE, k0), light);
        }
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(face).add(0, .16f, 0), .22f, 0, -1, BossMesh.glow(SandMesh.TURQUOISE, k0), light);
        RitualMesh.billboard(glow, poseStack, view, chest, 2.4f, 0, -1, BossMesh.glow(SandMesh.SUN, .15f * k0), light);
        // The bolt
        if (target != null && since < 8) {
            float t = since / 8;
            Vector3f from = new Vector3f(chest).add(new Vector3f(right).mul(.5f)).add(new Vector3f(fwd).mul(.8f));
            Vector3f head = new Vector3f(from).lerp(target, Math.min(1, t * 2));
            VertexConsumer bolt = BossMesh.see(buffer, SandMesh.FLOW);
            Vector3f mid = new Vector3f(from).add(head).mul(.5f).add(0, .3f, 0);
            SandMesh.ribbon(bolt, poseStack.last(), view.eye(), SandMesh.curve(from, mid, head, 8), new float[] {.05f, .12f, .18f, .22f, .26f, .28f, .3f, .32f, .34f}, 1.5f,
                    age * .9f, SandMesh.colours(BossMesh.argb(SandMesh.SAND_PALE, (1 - t) * k0), 9), light);
            VertexConsumer dust = BossMesh.see(buffer, BossMesh.DUST);
            SandMesh.cloud(dust, poseStack, view, target, .8f + 1.5f * t, t, SandMesh.SAND, .8f * (1 - t), light);
        }
        SandMesh.grains(poseStack, buffer, view, .8f, chest.y + 1, age, 10, k0, seed, light);
        poseStack.popPose();
    }
}
