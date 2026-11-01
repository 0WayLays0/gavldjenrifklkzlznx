package com.waylays.arcanes.client.render.wind;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Gale: a blast of wind from the hand, a cone of lines racing out and curling, dust and leaves blown along with it, a
 * puff of air bursting at the hand.
 */
public final class GaleModel {
    private GaleModel() {}

    /** From the pose's origin along {@code forward}, {@code t} (0 to 1) how far through it is, {@code range} its reach. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f forward, float t, float range, int seed, int light) {
        Vector3f eye = view.eye();
        Vector3f f = new Vector3f(forward).normalize();
        Vector3f s = GustBladeModel.side(f);
        Vector3f u = new Vector3f(s).cross(f).normalize();
        float fade = 1 - t;
        VertexConsumer lines = WindMesh.lines(buffer);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < 34; k++) {
            float a = WindMesh.hash(seed + k * 3) * Mth.TWO_PI, spread = .5f * Mth.sqrt(WindMesh.hash(seed + k * 5));
            Vector3f dir = new Vector3f(f).add(new Vector3f(s).mul(Mth.cos(a) * spread)).add(new Vector3f(u).mul(Mth.sin(a) * spread)).normalize();
            Vector3f[] path = new Vector3f[7];
            float curl = (WindMesh.hash(seed + k * 7) - .5f) * 1.6f;
            for (int i = 0; i < 7; i++) {
                float d = range * i / 6;
                path[i] = new Vector3f(dir).mul(d).add(new Vector3f(s).mul(curl * (d / range) * (d / range)));
            }
            float head = Mth.clamp(t * (1.3f + .5f * WindMesh.hash(seed + k)) + .05f, 0, 1.3f);
            WindMesh.flow(lines, p, eye, path, head, .35f, .1f + .1f * WindMesh.hash(seed + k * 11), WaterMesh.mix(WindMesh.AIR, WindMesh.JADE,
                    WindMesh.hash(seed + k * 13) * .6f), .9f * fade);
        }
        // Dust blown along
        VertexConsumer dust = WindMesh.see(buffer, WindMesh.DUST);
        for (int k = 0; k < 8; k++) {
            float d = range * t * (.3f + .7f * WindMesh.hash(seed + k * 17));
            Vector3f at = new Vector3f(f).mul(d).add(new Vector3f(s).mul((WindMesh.hash(seed + k * 19) - .5f) * d * .6f)).add(0, -.6f, 0);
            WaterMesh.puff(dust, poseStack, view, at.x, at.y, at.z, .8f + 2.2f * t, k + t * 3, -1, WaterMesh.argb(WindMesh.DUSTY, .4f * fade), light);
        }
        VertexConsumer wisps = WindMesh.see(buffer, WindMesh.WISP);
        float r = .6f + 1.8f * t;
        WaterMesh.puff(wisps, poseStack, view, f.x * .4f, f.y * .4f, f.z * .4f, r * 1.4f, t * 6, seed & 3, WaterMesh.argb(WindMesh.AIR, .7f * fade), light);
        // Leaves blown along
        VertexConsumer leaves = WindMesh.leaves(buffer);
        p = poseStack.last();
        for (int k = 0; k < 10; k++) {
            float d = range * Mth.clamp(t * (1.1f + .4f * WindMesh.hash(seed + k * 23)), 0, 1);
            Vector3f at = new Vector3f(f).mul(d).add(new Vector3f(s).mul((WindMesh.hash(seed + k * 29) - .5f) * d * .5f))
                    .add(new Vector3f(u).mul((WindMesh.hash(seed + k * 31) - .5f) * d * .4f));
            WindMesh.leaf(leaves, p, at, .25f, t * 20 + k, t * 15 + k * 2, k & 3, light);
        }
    }
}
