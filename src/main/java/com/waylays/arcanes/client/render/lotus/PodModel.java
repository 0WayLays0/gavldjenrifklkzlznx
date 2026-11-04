package com.waylays.arcanes.client.render.lotus;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.boss.Sculpt;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Seed Pod: a lotus seed head floats at the caster's shoulder, its flat top pitted with seeds turned toward the foe, its
 * stem curling down beneath it to a small leaf, a soft glow behind it. When it shoots, it kicks back, a seed flies from
 * it in an arc with a streak of light, and where the seed strikes a little lotus springs open and fades.
 */
public final class PodModel {
    /** A seed flies for this many ticks, then a flower opens where it struck for {@link #BLOOM} more. */
    private static final float FLIGHT = 5, BLOOM = 20;
    /** The pod floats this high (as the scene says) and is drawn this big. */
    private static final float HEIGHT = 2.2f, SIZE = .62f;

    private PodModel() {}

    /** At the caster's shoulder (the pose's origin); facing {@code dir}; its last seed shot {@code since} ticks ago at {@code target} (or null). */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f dir, float age, int life, float since, Vector3f target,
                            int seed, int light) {
        float grow = BossMesh.ramp(age, 0, 10);
        float fade = 1 - BossMesh.ramp(age, life - 12, life);
        float k = grow * fade;
        if (k <= .01f) {
            return;
        }
        Vector3f centre = new Vector3f(0, HEIGHT + .1f * Mth.sin(age * .08f), 0);
        // It turns its top toward where it shoots, else ahead and up
        Vector3f aim = new Vector3f(dir.x, 0, dir.z);
        if (aim.lengthSquared() < 1.0E-6f) {
            aim.set(0, 0, 1);
        }
        aim.normalize();
        boolean shooting = target != null && since >= 0 && since < FLIGHT + BLOOM;
        if (shooting) {
            Vector3f to = new Vector3f(target).sub(centre);
            if (to.lengthSquared() > 1.0E-4f) {
                aim = to.normalize();
            }
        }
        float kick = shooting && since < 6 ? (float) Math.exp(-since / 2) : 0;
        Vector3f axis = new Vector3f(aim).mul(.55f).add(0, .85f, 0).normalize();
        centre.sub(new Vector3f(aim).mul(.25f * kick));
        Quaternionf q = LotusMesh.frame(axis, new Vector3f(aim).negate());
        float s = SIZE * k;

        // ---- the head itself: green flanks swelling to its flat top, the seeds set in their pits
        poseStack.pushPose();
        poseStack.translate(centre.x, centre.y, centre.z);
        poseStack.mulPose(q);
        poseStack.mulPose(new Quaternionf().rotationY(age * .01f));
        poseStack.scale(s, s, s);
        PoseStack.Pose p = poseStack.last();
        VertexConsumer fibre = BossMesh.solid(buffer, LotusMesh.FIBRE);
        Sculpt.lathe(fibre, p, new float[] {.05f, -.78f, .11f, -.62f, .23f, -.4f, .37f, -.17f, .46f, .0f, .5f, .1f, .49f, .17f, .45f, .2f}, 22, 3, 1.5f,
                BossMesh.argb(0x6E9448, 1), BossMesh.argb(0xC2CC6E, 1), light);
        // A rim round its top
        poseStack.pushPose();
        poseStack.translate(0, .195f, 0);
        Sculpt.torus(fibre, poseStack.last(), .44f, .025f, 24, 5, 0, Mth.TWO_PI, 2, BossMesh.argb(0xB8C468, 1), light);
        poseStack.popPose();
        LotusMesh.podTop(BossMesh.solid(buffer, LotusMesh.POD), p, .2f, .45f, light);
        VertexConsumer seeds = BossMesh.solid(buffer, LotusMesh.FIBRE);
        for (int i = 0; i < LotusMesh.PITS.length; i += 2) {
            Vector3f at = new Vector3f(LotusMesh.PITS[i] * .45f, .2f, LotusMesh.PITS[i + 1] * .45f);
            BossMesh.sphere(seeds, p, at, .055f, 4, 8, 0, 0, .55f, BossMesh.argb(0x8AA040, 1), light);
        }
        poseStack.popPose();

        // ---- its stem, curling down to a small leaf
        Vector3f foot = q.transform(new Vector3f(0, -.78f * s, 0)).add(centre);
        Vector3f end = new Vector3f(foot).add(new Vector3f(aim).mul(-.4f)).add(.15f * Mth.sin(age * .05f), -.75f * k, 0);
        Vector3f[] stem = LotusMesh.path(foot, new Vector3f(foot).add(q.transform(new Vector3f(0, -.5f * k, 0))), new Vector3f(end).add(.3f, .3f, 0), end, 8);
        LotusMesh.stem(BossMesh.solid(buffer, LotusMesh.FIBRE), poseStack.last(), stem, .045f * k, light);
        LotusMesh.leaf(BossMesh.solid(buffer, LotusMesh.LEAF), poseStack.last(), end, .3f * k, age * .01f, .35f, seed, BossMesh.argb(0xFFFFFF, 1), light);

        // ---- its glow
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, centre, 1.6f * k, 0, -1, BossMesh.glow(LotusMesh.GOLD, .18f + .4f * kick), light);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(centre).add(new Vector3f(axis).mul(.2f)), 2.6f * k, 0, -1, BossMesh.glow(LotusMesh.PINK, .1f), light);

        // ---- the seed in flight, then the flower where it struck
        if (shooting) {
            Vector3f from = new Vector3f(centre).add(new Vector3f(axis).mul(.2f));
            if (since < FLIGHT) {
                float t = since / FLIGHT;
                Vector3f at = new Vector3f(from).lerp(target, t).add(0, Mth.sin(t * Mth.PI) * .5f, 0);
                Vector3f back = new Vector3f(from).lerp(target, Math.max(0, t - .35f)).add(0, Mth.sin(Math.max(0, t - .35f) * Mth.PI) * .5f, 0);
                VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
                RitualMesh.shaft(beam, poseStack.last(), view.eye(), back, at, .25f, BossMesh.glow(LotusMesh.GOLD, .55f), light);
                VertexConsumer nut = BossMesh.solid(buffer, LotusMesh.FIBRE);
                BossMesh.sphere(nut, poseStack.last(), at, .1f, 5, 8, since, 0, 1, BossMesh.argb(0x7A8A3C, 1), light);
            } else {
                float b = (since - FLIGHT) / BLOOM;
                float open = BossMesh.ramp(b, 0, .35f);
                float shrink = 1 - BossMesh.ramp(b, .6f, 1);
                LotusMesh.bloom(poseStack, buffer, view, LotusMesh.LOTUS, new Vector3f(target).add(0, -.4f, 0), .5f * shrink, .1f + .9f * open, since * .05f, age,
                        BossMesh.argb(0xFFF8FA, 1), BossMesh.argb(LotusMesh.TIP, 1), .6f * shrink, LotusMesh.PINK, seed, light);
                VertexConsumer flash = BossMesh.light(buffer, BossMesh.GLOW);
                RitualMesh.billboard(flash, poseStack, view, target, .8f + 1.5f * b, 0, -1, BossMesh.glow(LotusMesh.GOLD, (1 - b) * .7f), light);
            }
        }
    }
}
