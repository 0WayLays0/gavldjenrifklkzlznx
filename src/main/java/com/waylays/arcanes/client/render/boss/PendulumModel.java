package com.waylays.arcanes.client.render.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.sand.SandMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Pendulum of Doom: from a rune circle high over the strip hangs a pendulum: a gear of gold at its pivot, a long rod of
 * black iron bound with gold, and a great crescent blade of folded steel, a skull of bone at its hub, its edge burning.
 * Drawn back to one end of the strip, trembling, it is let go: it swings through the strip, back and forth, a sheet of
 * light trailing its edge; then it rises back into the sky.
 */
public final class PendulumModel {
    private static final ResourceLocation STEEL = ArcanesOublies.id("textures/entity/boss/m_steel.png");
    private static final ResourceLocation BONE = ArcanesOublies.id("textures/entity/boss/m_bone.png");
    /** The blade: an outer arc (its edge) and an inner arc (its back), round a point above its hub. */
    private static final float EDGE = 3.6f, BACK = 2.6f, ARC_Y = 2.2f;
    private static final int ARC = 14;

    private PendulumModel() {}

    /** Its angle from hanging straight down (radians, along the strip) {@code hit} ticks after its first pass. */
    private static float angle(float hit, int swing, int swings, float amp) {
        if (hit < -swing * .5f) {
            return -amp;
        }
        float last = (swings - 1) * swing + swing * .5f;
        return amp * Mth.sin(Mth.PI * Math.min(hit, last) / swing);
    }

    /**
     * Over the middle of its strip (the pose's origin), hung from {@code pivot}; the strip runs along {@code yaw}
     * (degrees); its first pass {@code hit} ticks ago (negative before), {@code swings} passes {@code swing} ticks apart;
     * the strip {@code length} long.
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f pivot, float age, float hit, int swing, int swings,
                            float yaw, float length, int zone, int seed, int light) {
        float form = BossMesh.ramp(age, 0, 18);
        float done = (swings - 1) * swing + swing * .5f;
        float rise = BossMesh.ramp(hit, done + 4, done + 24);
        if (rise >= 1) {
            return;
        }
        float rod = pivot.y - 1.3f;
        float amp = (float) Math.asin(Math.min(.95f, length * .5f / rod));
        float theta = angle(hit, swing, swings, amp);
        // Trembling before it is let go
        if (hit < -swing * .5f) {
            theta += Mth.sin(age * 1.7f) * .01f * BossMesh.ramp(hit, -40, -swing * .5f);
        }
        Vector3f top = new Vector3f(pivot).add(0, (1 - form) * 6 + rise * rise * 20, 0);
        Quaternionf frame = new Quaternionf().rotationY(-(yaw + 90) * Mth.DEG_TO_RAD).rotateZ(theta);

        // ---- iron: the rod; gold: its bands, the gear at the pivot, the hub
        VertexConsumer iron = BossMesh.solid(buffer, BellModel.IRON);
        place(poseStack, top, frame);
        Vector3f[] shaft = {new Vector3f(0, 0, 0), new Vector3f(0, -rod * .5f, 0), new Vector3f(0, -rod + 2.2f, 0)};
        Sculpt.sweep(iron, poseStack.last(), shaft, Sculpt.fill(.16f, 3), Sculpt.fill(.16f, 3), Sculpt.circle(10), false, null, .5f, Sculpt.fill(0xFFFFFFFF, 3), true,
                light);
        poseStack.popPose();
        VertexConsumer gold = BossMesh.solid(buffer, BellModel.GOLD);
        place(poseStack, top, frame);
        for (int k = 1; k <= 5; k++) {
            poseStack.pushPose();
            poseStack.translate(0, -rod * k / 6.5f, 0);
            Sculpt.torus(gold, poseStack.last(), .2f, .07f, 12, 5, 0, Mth.TWO_PI, 2, 0xFFFFFFFF, light);
            poseStack.popPose();
        }
        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotationZ(age * .05f).rotateX(Mth.HALF_PI));
        Sculpt.slab(gold, poseStack.last(), ClockModel.gear(1.4f, 1.15f, 14), .35f, .05f, .5f, 0xFFFFFFFF, 0xFFD0A050, light);
        poseStack.popPose();
        poseStack.translate(0, -rod + ARC_Y, 0);
        poseStack.mulPose(new Quaternionf().rotationX(Mth.HALF_PI));
        Sculpt.torus(gold, poseStack.last(), .75f, .16f, 20, 6, 0, Mth.TWO_PI, 4, 0xFFFFFFFF, light);
        poseStack.popPose();
        // ---- steel: the crescent blade, its edge down
        VertexConsumer steel = BossMesh.solid(buffer, STEEL);
        place(poseStack, top, frame);
        poseStack.translate(0, -rod, 0);
        Sculpt.slab(steel, poseStack.last(), crescent(), .32f, .14f, .3f, 0xFFFFFFFF, 0xFFE0E4F0, light);
        poseStack.popPose();
        // ---- bone: a skull at its hub, grinning out of each face
        VertexConsumer bone = BossMesh.solid(buffer, BONE);
        for (int side = -1; side <= 1; side += 2) {
            place(poseStack, top, frame);
            poseStack.translate(0, -rod + ARC_Y, side * .2f);
            poseStack.mulPose(new Quaternionf().rotationY(side < 0 ? Mth.PI : 0));
            poseStack.scale(1.1f, 1.1f, 1.1f);
            Figures.skull(bone, poseStack, .25f, light);
            poseStack.popPose();
        }

        // ---- light: its edge burning, the sheet it trails, the rune circle it hangs from
        Vector3f[] edge = new Vector3f[ARC + 1];
        for (int i = 0; i <= ARC; i++) {
            float a = Mth.lerp((float) i / ARC, -150, -30) * Mth.DEG_TO_RAD;
            edge[i] = frame.transform(new Vector3f(Mth.cos(a) * EDGE, -rod + ARC_Y + Mth.sin(a) * EDGE, 0)).add(top);
        }
        float speed = Math.abs(Mth.cos(Mth.PI * Math.min(hit, done) / swing)) * (hit >= -swing * .5f && hit <= done ? 1 : 0);
        VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
        SandMesh.ribbon(beam, poseStack.last(), view.eye(), edge, Sculpt.fill(.35f + .4f * speed, ARC + 1), 1, 0, Sculpt.fill(BossMesh.glow(zone, .6f + .4f * speed), ARC + 1),
                light);
        if (speed > .2f) {
            VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
            float dir = Math.signum(Mth.cos(Mth.PI * Math.min(hit, done) / swing));
            for (int i = 0; i < 8; i++) {
                float back = theta - dir * (i + 1) * .05f;
                Quaternionf trail = new Quaternionf().rotationY(-(yaw + 90) * Mth.DEG_TO_RAD).rotateZ(back);
                Vector3f p0 = trail.transform(new Vector3f(-EDGE * .7f, -rod - .8f, 0)).add(top), p1 = trail.transform(new Vector3f(EDGE * .7f, -rod - .8f, 0)).add(top);
                RitualMesh.shaft(glow, poseStack.last(), view.eye(), p0, p1, 1.2f, BossMesh.glow(zone, .3f * speed * (1 - i / 8f)), light);
            }
        }
        VertexConsumer circle = BossMesh.light(buffer, BossMesh.RUNES);
        BossMesh.annulus(circle, poseStack.last(), new Vector3f(top).add(0, .5f, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1), 1.8f, 5.5f, 48, 3, age * .02f, 0,
                BossMesh.glow(zone, .85f * (1 - rise)), BossMesh.glow(zone, .3f * (1 - rise)), light, true);
    }

    private static void place(PoseStack poseStack, Vector3f top, Quaternionf frame) {
        poseStack.pushPose();
        poseStack.translate(top.x, top.y, top.z);
        poseStack.mulPose(frame);
    }

    /** The blade's outline (counter-clockwise): the edge's arc below, the back's arc above it. */
    private static float[] crescent() {
        float[] s = new float[(ARC + 1) * 4];
        int k = 0;
        for (int i = 0; i <= ARC; i++) {
            float a = Mth.lerp((float) i / ARC, -150, -30) * Mth.DEG_TO_RAD;
            s[k++] = Mth.cos(a) * EDGE;
            s[k++] = ARC_Y + Mth.sin(a) * EDGE;
        }
        for (int i = 0; i <= ARC; i++) {
            float a = Mth.lerp((float) i / ARC, -40, -140) * Mth.DEG_TO_RAD;
            s[k++] = Mth.cos(a) * BACK;
            s[k++] = ARC_Y + Mth.sin(a) * BACK;
        }
        return s;
    }
}
