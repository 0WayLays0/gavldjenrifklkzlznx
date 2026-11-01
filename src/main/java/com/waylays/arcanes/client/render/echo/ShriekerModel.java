package com.waylays.arcanes.client.render.echo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * A shrieker of the Deep Choir: it rises out of the ground, a block of sculk crowned with jaws of bone round a soul of
 * cyan light; when it shrieks the jaws gape, rings of sound pour up out of it and a sonic boom leaps at its foe.
 */
public final class ShriekerModel {
    /** How long a shriek shows (ticks). */
    public static final int SHRIEK = BoomModel.LIFE;

    private ShriekerModel() {}

    /**
     * At its foot (the pose's origin); {@code age} of {@code life} ticks; {@code sinceShriek} ticks since it last shrieked
     * (large if never), at {@code target} (in the same frame, or null).
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float life, float sinceShriek, Vector3f target,
                            int seed, int light) {
        float rise = BossMesh.ramp(age, 0, 12);
        float sink = BossMesh.ramp(age, life - 12, life);
        float y0 = -1.2f * (1 - rise) - 1.2f * sink;
        float cry = sinceShriek < SHRIEK ? 1 - sinceShriek / SHRIEK : 0;
        EchoMesh.patch(poseStack, buffer, 0, .03f, 0, 3.2f * rise, seed, 1 - sink, .5f + .5f * cry, light);
        // The block of sculk, its glowing band
        VertexConsumer block = BossMesh.see(buffer, EchoMesh.SCULK);
        BossMesh.box(block, poseStack.last(), new Vector3f(0, y0 + .4f, 0), new Vector3f(.5f, 0, 0), new Vector3f(0, .4f, 0), new Vector3f(0, 0, .5f), 0xFFFFFFFF,
                light);
        VertexConsumer band = BossMesh.light(buffer, EchoMesh.VEINS);
        BossMesh.box(band, poseStack.last(), new Vector3f(0, y0 + .45f, 0), new Vector3f(.51f, 0, 0), new Vector3f(0, .25f, 0), new Vector3f(0, 0, .51f),
                BossMesh.glow(EchoMesh.ECHO, .5f + .5f * cry), light);
        // Its jaws of bone, gaping as it shrieks
        VertexConsumer bone = BossMesh.see(buffer, BossMesh.SPIKE);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < 8; k++) {
            float a = k * Mth.TWO_PI / 8 + Mth.PI / 8;
            float r = .44f;
            Vector3f base = new Vector3f(Mth.cos(a) * r, y0 + .78f, Mth.sin(a) * r);
            float open = .2f + .9f * cry;
            Vector3f out = new Vector3f(Mth.cos(a), 0, Mth.sin(a));
            Vector3f dir = new Vector3f(out).mul(open).add(0, 1, 0).normalize();
            Vector3f bend = new Vector3f(out).mul(-.35f * (1 - cry));
            float len = (k % 2 == 0 ? .75f : .55f) * rise;
            BossMesh.horn(bone, p, base, dir, bend, len, .1f, 5, 4, BossMesh.argb(0x8A8470, 1), BossMesh.argb(EchoMesh.BONE, 1), light);
        }
        Vector3f soul = new Vector3f(0, y0 + 1.05f, 0);
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, soul, .9f + .9f * cry, 0, -1, BossMesh.glow(EchoMesh.ECHO, (.7f + .3f * Mth.sin(age * .4f)) * (1 - sink)), light);
        RitualMesh.billboard(glow, poseStack, view, soul, .4f + .4f * cry, 0, -1, BossMesh.glow(EchoMesh.PALE, 1 - sink), light);
        if (cry <= 0) {
            return;
        }
        // Rings pouring up out of it
        VertexConsumer wave = BossMesh.light(buffer, EchoMesh.WAVE);
        p = poseStack.last();
        for (int k = 0; k < 4; k++) {
            float t = Mth.clamp((1 - cry) * 1.6f - k * .15f, 0, 1);
            EchoMesh.ring(wave, p, new Vector3f(0, y0 + 1.1f + t * 3, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, 1), .4f + 1.2f * t,
                    BossMesh.glow(EchoMesh.PALE, 1 - t), light);
        }
        if (target != null) {
            Vector3f d = new Vector3f(target).sub(soul);
            float len = d.length();
            if (len > .5f) {
                poseStack.pushPose();
                poseStack.translate(soul.x, soul.y, soul.z);
                WaterMesh.View from = new WaterMesh.View(view.camera(), new Vector3f(view.eye()).sub(soul));
                BoomModel.draw(poseStack, buffer, from, d.normalize(), len, .7f, sinceShriek, light);
                poseStack.popPose();
            }
        }
    }
}
