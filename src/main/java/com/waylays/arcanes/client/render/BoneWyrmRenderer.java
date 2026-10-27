package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.entity.bone.BoneWyrm;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * The Bone Wyrm: a skull with its jaws working, then a long spine of vertebrae following the path the head took,
 * smaller and smaller to a spiked tail, small ribs hanging under the first of them.
 */
public class BoneWyrmRenderer extends EntityRenderer<BoneWyrm> {
    public BoneWyrmRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BoneWyrm wyrm, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = wyrm.tickCount + partialTicks;
        Vec3 origin = wyrm.getPosition(partialTicks);
        // The path behind the head, from where the head is drawn now
        List<Vec3> path = new ArrayList<>();
        path.add(origin);
        path.addAll(wyrm.trail);
        List<Vector3f> at = sample(path, origin, BoneWyrm.SEGMENTS + 1, BoneWyrm.SPACING);
        if (at.size() < 3) {
            return;
        }
        // The skull, turned the way the head is going
        Vector3f dir = new Vector3f(at.get(0)).sub(at.get(1));
        if (dir.lengthSquared() < 1.0E-6f) {
            dir.set(0, 0, 1);
        }
        poseStack.pushPose();
        poseStack.translate(at.get(0).x, at.get(0).y - .55f, at.get(0).z);
        poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 0, 1), dir.normalize()));
        BoneRender.skull(poseStack, buffer, 1.25f, .35f + .25f * Mth.sin(age * .35f), packedLight, 1);
        poseStack.popPose();
        // The spine after it (the skull's own glow has asked for another buffer, so the bone one is asked for again)
        VertexConsumer bone = BoneRender.buffer(buffer);
        PoseStack.Pose p = poseStack.last();
        int n = at.size() - 1;
        for (int k = 1; k < n; k++) {
            Vector3f along = new Vector3f(at.get(k - 1)).sub(at.get(k + 1));
            if (along.lengthSquared() < 1.0E-6f) {
                continue;
            }
            along.normalize();
            Vector3f up = new Vector3f(0, 1, 0).sub(new Vector3f(along).mul(along.y));
            if (up.lengthSquared() < 1.0E-4f) {
                up.set(1, 0, 0);
            }
            up.normalize();
            float t = (float) k / n;
            float s = .7f * (1 - .6f * t);
            BoneRender.vertebra(bone, p, at.get(k), along, up, s, packedLight, BoneRender.WHITE);
            if (k >= 2 && k <= 8) {
                // Small ribs hanging under the front of the body
                Vector3f right = new Vector3f(along).cross(up).normalize();
                for (int side = -1; side <= 1; side += 2) {
                    Vector3f[] rib = new Vector3f[5];
                    for (int i = 0; i < 5; i++) {
                        float u = i / 4f;
                        rib[i] = new Vector3f(at.get(k)).add(new Vector3f(right).mul(side * (.25f + .45f * Mth.sin(u * Mth.PI * .6f)) * s * 1.6f))
                                .add(new Vector3f(up).mul(-.9f * u * s * 1.6f));
                    }
                    BoneRender.curve(bone, p, rib, .07f, .04f, 5, packedLight, BoneRender.WHITE);
                }
            }
        }
        Vector3f tail = new Vector3f(at.get(n)).sub(at.get(n - 1));
        if (tail.lengthSquared() > 1.0E-6f) {
            BoneRender.spike(bone, p, at.get(n - 1), new Vector3f(at.get(n)).add(tail.normalize().mul(1.2f)), .16f, new Vector3f(0, .15f, 0), packedLight, BoneRender.WHITE);
        }
        super.render(wyrm, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** Points every {@code spacing} along the path (relative to {@code origin}), as many as {@code count} if the path is long enough. */
    private static List<Vector3f> sample(List<Vec3> path, Vec3 origin, int count, float spacing) {
        List<Vector3f> out = new ArrayList<>();
        out.add(new Vector3f(0, 0, 0));
        double need = spacing;
        for (int i = 0; i + 1 < path.size() && out.size() < count; i++) {
            Vec3 a = path.get(i), b = path.get(i + 1);
            double len = a.distanceTo(b);
            double done = 0;
            while (len - done >= need && out.size() < count) {
                done += need;
                Vec3 q = a.lerp(b, done / len).subtract(origin);
                out.add(new Vector3f((float) q.x, (float) q.y, (float) q.z));
                need = spacing;
            }
            need -= len - done;
        }
        return out;
    }

    @Override
    public boolean shouldRender(BoneWyrm wyrm, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(BoneWyrm wyrm) {
        return BoneRender.BONE;
    }
}
