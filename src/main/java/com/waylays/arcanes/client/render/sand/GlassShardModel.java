package com.waylays.arcanes.client.render.sand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/** A shard of glass in flight: clear, sea-green at its heart, tumbling, glinting, a trail of sparkles behind it. */
public final class GlassShardModel {
    private GlassShardModel() {}

    /** At the shard (the pose's origin), flying along {@code forward}. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f forward, float age, int seed, int light) {
        float spin = age * .6f + seed;
        Vector3f d = new Vector3f(forward).add(new Vector3f(Mth.sin(spin), Mth.cos(spin * .7f), Mth.sin(spin * 1.3f)).mul(.35f)).normalize();
        VertexConsumer glass = BossMesh.see(buffer, SandMesh.GLASS);
        RitualMesh.crystal(glass, poseStack.last(), new Vector3f(d).mul(-.65f), d, 1.4f, .28f, spin, BossMesh.argb(SandMesh.GLASS_TINT, .85f), light);
        VertexConsumer core = BossMesh.see(buffer, SandMesh.GLASS);
        RitualMesh.crystal(core, poseStack.last(), new Vector3f(d).mul(-.35f), d, .75f, .12f, -spin, BossMesh.argb(SandMesh.TURQUOISE, .8f), light);
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.MOTE);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(d).mul(.2f), .45f + .25f * Mth.sin(age * 1.7f), spin, -1, BossMesh.glow(0xFFFFFF, .7f), light);
        for (int k = 1; k < 6; k++) {
            Vector3f at = new Vector3f(forward).mul(-.5f * k).add((BossMesh.hash(seed + k) - .5f) * .3f, (BossMesh.hash(seed + k * 3) - .5f) * .3f, 0);
            RitualMesh.billboard(glow, poseStack, view, at, .22f, 0, -1, BossMesh.glow(k % 2 == 0 ? 0xFFFFFF : SandMesh.TURQUOISE, (1 - k / 6f) * .8f), light);
        }
    }
}
