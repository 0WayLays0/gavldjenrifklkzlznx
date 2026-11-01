package com.waylays.arcanes.client.render.sand;

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
 * Dune Serpent: a long serpent of flowing sand swimming through the ground, its back rising out of the sand and diving
 * back in as it goes, a ridge of sandstone spines along it; its head held high, wedge-shaped, glass eyes glowing
 * turquoise; it strikes with its jaws wide and fangs bared. Sand sprays where it breaks the surface.
 */
public final class SerpentModel {
    private static final int N = 26;
    private static final float LENGTH = 6.5f;

    private SerpentModel() {}

    /**
     * At its head's foot (the pose's origin), heading {@code dir}; {@code age} of {@code life} ticks; {@code sinceBite}
     * ticks since it last bit (large if never).
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f dir, float age, float life, float sinceBite, int seed,
                            int light) {
        float emerge = BossMesh.ramp(age, 0, 10);
        float sink = BossMesh.ramp(age, life - 12, life);
        float up = emerge * (1 - sink);
        float bite = sinceBite < 10 ? Mth.sin((1 - sinceBite / 10) * Mth.PI) : 0;
        Vector3f fwd = new Vector3f(dir.x, 0, dir.z);
        if (fwd.lengthSquared() < 1.0E-6f) {
            fwd.set(0, 0, 1);
        }
        fwd.normalize();
        Vector3f side = new Vector3f(-fwd.z, 0, fwd.x);
        Vector3f[] pts = new Vector3f[N + 1];
        float[] r = new float[N + 1];
        int[] col = new int[N + 1];
        for (int i = 0; i <= N; i++) {
            float s = LENGTH * i / N;
            float sway = Mth.sin(s * 1.3f - age * .45f) * .55f * Math.min(1, s / 1.5f);
            float y = .55f * Mth.sin(s * 1.05f - age * .3f) - .15f;
            // Its neck rears up, the head held level
            float neck = Math.max(0, 1 - s / 2.2f);
            float raise = (1.25f + 1.0f * bite) * neck * neck * (3 - 2 * neck);
            pts[i] = new Vector3f(fwd).mul(-s + .9f * bite * Math.max(0, 1 - s)).add(new Vector3f(side).mul(sway)).add(0, (y + raise) * up - 1.6f * (1 - up), 0);
            float t = s / LENGTH;
            r[i] = .25f * (t < .12f ? .8f + 1.7f * t : (float) Math.pow(1 - (t - .12f) / .88f, .7f)) + .02f;
            col[i] = 0xFFFFFFFF;
        }
        // Its body of sand
        VertexConsumer flow = BossMesh.see(buffer, SandMesh.FLOW);
        PoseStack.Pose p = poseStack.last();
        SandMesh.tube(flow, p, pts, r, 10, 1.4f, age * .25f, col, light);
        // Its head
        Vector3f head = pts[0];
        Vector3f look = new Vector3f(fwd).add(0, -.12f + .25f * bite, 0).normalize();
        poseStack.pushPose();
        poseStack.translate(head.x, head.y, head.z);
        poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 0, 1), look));
        float jaw = .15f + .7f * bite;
        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotationX(-jaw * .5f));
        SandMesh.ellipsoid(flow, poseStack, new Vector3f(0, .06f, .26f), .27f, .13f, .44f, 0, age * .01f, 0xFFFFFFFF, light);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotationX(jaw * .6f));
        SandMesh.ellipsoid(flow, poseStack, new Vector3f(0, -.08f, .22f), .22f, .07f, .38f, 0, age * .01f, 0xFFE8D0A0, light);
        poseStack.popPose();
        poseStack.popPose();
        // Its hood, spread wide behind its head like a cobra's
        float hood = .6f + .4f * Math.max(bite, up);
        SandMesh.ellipsoid(flow, poseStack, new Vector3f(pts[3]).add(0, .05f, 0), .5f * hood, .55f, .08f, (float) Math.atan2(fwd.x, fwd.z), age * .01f, 0xFFF0D8A8,
                light);
        // A ridge of spines along its back, and its fangs
        VertexConsumer stone = BossMesh.solid(buffer, BossMesh.SPIKE);
        p = poseStack.last();
        for (int i = 3; i < N - 2; i += 2) {
            if (pts[i].y < -.1f) {
                continue;
            }
            Vector3f tan = new Vector3f(pts[i - 1]).sub(pts[i + 1]).normalize();
            Vector3f out = new Vector3f(tan).cross(side).normalize();
            if (out.y < 0) {
                out.negate();
            }
            BossMesh.horn(stone, p, new Vector3f(pts[i]).add(new Vector3f(out).mul(r[i] * .8f)), out, new Vector3f(tan).mul(-.4f), .35f * r[i] / .34f + .1f, .07f, 4, 3,
                    0xFF9A6A38, 0xFFE0C080, light);
        }
        poseStack.pushPose();
        poseStack.translate(head.x, head.y, head.z);
        poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 0, 1), look));
        poseStack.mulPose(new Quaternionf().rotationX(-(.15f + .7f * bite) * .5f));
        PoseStack.Pose hp = poseStack.last();
        for (int s = -1; s <= 1; s += 2) {
            BossMesh.horn(stone, hp, new Vector3f(s * .12f, -.02f, .55f), new Vector3f(0, -1, .25f).normalize(), new Vector3f(0, 0, -.3f), .22f + .1f * bite, .035f, 4, 3,
                    0xFFF4EEDC, 0xFFFFFFFF, light);
        }
        poseStack.popPose();
        // Its glass eyes, glowing
        Vector3f eyeBase = new Vector3f(head).add(new Vector3f(look).mul(.35f)).add(0, .2f, 0);
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        for (int s = -1; s <= 1; s += 2) {
            Vector3f eye = new Vector3f(eyeBase).add(new Vector3f(side).mul(s * .2f));
            RitualMesh.billboard(glow, poseStack, view, eye, .35f + .2f * bite, 0, -1, BossMesh.glow(SandMesh.TURQUOISE, up), light);
        }
        // Sand spraying where it breaks the surface
        VertexConsumer dust = BossMesh.see(buffer, BossMesh.DUST);
        for (int i = 1; i <= N; i++) {
            if ((pts[i - 1].y > 0) != (pts[i].y > 0)) {
                SandMesh.cloud(dust, poseStack, view, new Vector3f(pts[i]).mul(1, 0, 1).add(0, .3f, 0), 1.1f, age * .05f + i, SandMesh.SAND, .55f * up, light);
            }
        }
        SandMesh.cloud(dust, poseStack, view, new Vector3f(0, .3f, 0), 1.6f + bite, age * .03f, SandMesh.SAND_PALE, .3f + .5f * bite * up, light);
    }
}
