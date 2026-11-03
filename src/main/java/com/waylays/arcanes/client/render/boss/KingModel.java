package com.waylays.arcanes.client.render.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Spectral Kings: a dead king in pale plate rises at the end of his strip, kneeling on his planted greatsword, head bowed
 * under a crown of gold spikes, his royal cape pooled behind him, his legs lost in ghostly mist. When his time comes he
 * stands, levels the sword and charges through the strip in a blink, the cape streaming, the light of his eyes and his
 * engraved breastplate leaving a wake of ghost-fire; then he is gone.
 */
public final class KingModel {
    private static final ResourceLocation CLOTH = ArcanesOublies.id("textures/entity/boss/m_cloth.png");
    private static final ResourceLocation STEEL = ArcanesOublies.id("textures/entity/boss/m_steel.png");
    private static final ResourceLocation ENGRAVING = ArcanesOublies.id("textures/entity/boss/m_engraving.png");
    private static final float SCALE = 1.35f;
    private static final int ARMOUR = 0xFF7E88A6, CAPE = 0xFFFF6A88;

    private KingModel() {}

    /**
     * From his strip's start (the pose's origin) to {@code to}, facing {@code yaw} (degrees); he charges {@code hit} ticks ago
     * (negative before).
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f to, float age, float hit, float yaw, int zone, int seed,
                            int light) {
        float rise = BossMesh.ramp(age, 0, 14);
        float gone = BossMesh.ramp(hit, 5, 12);
        if (gone >= 1 || rise <= 0) {
            return;
        }
        // Kneeling (1) to charging (0): he stands in the last ticks before his strip lands
        float stand = BossMesh.ramp(hit, -7, 0);
        float dash = Mth.clamp(hit / 4, 0, 1);
        dash = dash * dash * (3 - 2 * dash);
        Vector3f at = new Vector3f().lerp(to, dash);
        // He rises out of the ground, then sinks into it when gone
        float sink = (1 - rise) * 3.2f + gone * 3.2f;
        Quaternionf turn = new Quaternionf().rotationY(-yaw * Mth.DEG_TO_RAD);
        Pose pose = new Pose(stand, age);

        VertexConsumer plate = BossMesh.solid(buffer, STEEL);
        place(poseStack, at, turn, sink);
        armour(plate, poseStack, pose, light);
        sword(plate, poseStack, pose, light);
        poseStack.popPose();
        VertexConsumer gold = BossMesh.solid(buffer, BellModel.GOLD);
        place(poseStack, at, turn, sink);
        trim(gold, poseStack, pose, light);
        poseStack.popPose();
        VertexConsumer cloth = BossMesh.solid(buffer, CLOTH);
        place(poseStack, at, turn, sink);
        cape(cloth, poseStack.last(), pose, seed, light);
        poseStack.popPose();

        // ---- light: the engraving on his breast, his eyes, the mist he stands in, the wake of his charge
        int g = BossMesh.glow(zone, .8f);
        VertexConsumer engraving = BossMesh.light(buffer, ENGRAVING);
        place(poseStack, at, turn, sink);
        poseStack.translate(0, pose.chest, pose.lean * .4f);
        poseStack.mulPose(new Quaternionf().rotationX(pose.bow));
        Sculpt.quad(engraving, poseStack.last(), new Vector3f(-.26f, -.05f, .43f), new Vector3f(.26f, -.05f, .43f), new Vector3f(.26f, .45f, .4f),
                new Vector3f(-.26f, .45f, .4f), g, light);
        poseStack.popPose();
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        Vector3f face = new Quaternionf().rotationX(pose.bow).transform(new Vector3f(0, pose.head - pose.chest - .03f, .28f));
        Vector3f eyes = world(at, turn, sink, face.add(0, pose.chest, pose.lean * .4f));
        Vector3f side = turn.transform(new Vector3f(.09f * SCALE, 0, 0));
        int eye = BossMesh.glow(zone, 1);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(eyes).add(side), .35f, 0, -1, eye, light);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(eyes).sub(side), .35f, 0, -1, eye, light);
        RitualMesh.billboard(glow, poseStack, view, eyes, 1.3f, 0, -1, BossMesh.glow(zone, .4f), light);
        RitualMesh.billboard(glow, poseStack, view, world(at, turn, sink, new Vector3f(0, 1.6f, 0)), 5.5f, 0, -1, BossMesh.glow(zone, .22f * (1 - gone)), light);
        // The wake: ghost-fire where he has passed
        if (hit > 0) {
            for (int k = 0; k < 14; k++) {
                float t = dash * k / 13f;
                float f = (1 - gone) * (.3f + .7f * k / 13f);
                Vector3f p = new Vector3f().lerp(to, t).add(0, 1.4f + Mth.sin(k * 1.7f) * .3f, 0);
                RitualMesh.billboard(glow, poseStack, view, p, 2.4f, k, -1, BossMesh.glow(zone, .35f * f), light);
            }
        }
        VertexConsumer mist = BossMesh.see(buffer, BossMesh.SMOKE);
        for (int k = 0; k < 7; k++) {
            float an = k * Mth.TWO_PI / 7 + age * .05f;
            Vector3f p = world(at, turn, sink, new Vector3f(Mth.cos(an) * .45f, .35f + (k % 3) * .25f, Mth.sin(an) * .45f - pose.lean));
            RitualMesh.billboard(mist, poseStack, view, p, 1.8f + (k % 2) * .7f, an, -1, BossMesh.argb(BossMesh.mix(zone, 0xFFFFFF, .35f), .6f * (1 - gone)), light);
        }
        RitualMesh.billboard(BossMesh.light(buffer, BossMesh.GLOW), poseStack, view, world(at, turn, sink, new Vector3f(0, .5f, -pose.lean)), 3.2f, 0, -1, BossMesh.glow(zone, .5f * (1 - gone)), light);
    }

    /** Where his body's parts are: kneeling (stand 0) to charging (stand 1). */
    private record Pose(float stand, float hips, float chest, float head, float bow, float lean, Vector3f grip, Vector3f blade, float age) {
        Pose(float stand, float age) {
            this(stand, Mth.lerp(stand, .62f, 1.0f), Mth.lerp(stand, 1.62f, 2.0f), Mth.lerp(stand, 2.66f, 3.05f), Mth.lerp(stand, .45f, -.12f),
                    Mth.lerp(stand, 0, .35f), new Vector3f(0, Mth.lerp(stand, 1.85f, 1.75f), Mth.lerp(stand, .62f, .78f)),
                    new Vector3f(0, -Mth.cos(stand * Mth.HALF_PI * .96f), Mth.sin(stand * Mth.HALF_PI * .96f)), age);
        }
    }

    private static void place(PoseStack poseStack, Vector3f at, Quaternionf turn, float sink) {
        poseStack.pushPose();
        poseStack.translate(at.x, at.y - sink, at.z);
        poseStack.mulPose(turn);
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    private static Vector3f world(Vector3f at, Quaternionf turn, float sink, Vector3f local) {
        return turn.transform(new Vector3f(local).mul(SCALE)).add(at).sub(0, sink, 0);
    }

    private static void armour(VertexConsumer c, PoseStack poseStack, Pose pose, int light) {
        // The tassets: three lames, one over the next, over the mist
        poseStack.pushPose();
        poseStack.translate(0, pose.hips, pose.lean * .2f);
        for (int k = 0; k < 3; k++) {
            float y = .2f - k * .2f, r = .38f + k * .05f;
            Sculpt.lathe(c, poseStack.last(), new float[] {r + .04f, y - .24f, r + .06f, y - .2f, r, y, r - .04f, y + .04f}, 18, 2, .8f, 0xFF4A5064, ARMOUR, light);
        }
        poseStack.popPose();
        // The breastplate (wide, shallow), turned to the bow of his body
        poseStack.pushPose();
        poseStack.translate(0, pose.chest, pose.lean * .4f);
        poseStack.mulPose(new Quaternionf().rotationX(pose.bow));
        poseStack.pushPose();
        poseStack.scale(1.18f, 1, .82f);
        Sculpt.lathe(c, poseStack.last(), new float[] {.34f, -.62f, .38f, -.4f, .44f, -.15f, .48f, .12f, .47f, .32f, .4f, .48f, .24f, .56f, 0, .58f}, 22, 2, .8f,
                ARMOUR, ARMOUR, light);
        poseStack.popPose();
        // The gorget
        poseStack.translate(0, .58f, 0);
        Sculpt.lathe(c, poseStack.last(), new float[] {.26f, -.02f, .24f, .1f, .2f, .16f, 0, .17f}, 16, 1, 1, ARMOUR, ARMOUR, light);
        // The helm: a dome, a face plate with its slit, the cheeks
        poseStack.translate(0, pose.head - pose.chest - .58f, 0);
        Sculpt.lathe(c, poseStack.last(), new float[] {.25f, -.24f, .27f, -.05f, .27f, .12f, .22f, .27f, .12f, .35f, 0, .37f}, 18, 1, 1, ARMOUR, ARMOUR, light);
        Figures.box(c, poseStack, 0, -.12f, .23f, .34f, .16f, .1f, .03f, ARMOUR, light);
        Figures.box(c, poseStack, 0, .07f, .25f, .36f, .1f, .08f, .03f, ARMOUR, light);
        poseStack.popPose();
        // The pauldrons, the arms down to the hands on the grip
        for (int side = -1; side <= 1; side += 2) {
            Vector3f shoulder = new Vector3f(side * .55f, pose.chest + .38f, pose.lean * .45f);
            poseStack.pushPose();
            poseStack.translate(shoulder.x, shoulder.y, shoulder.z);
            poseStack.mulPose(new Quaternionf().rotationZ(-side * .5f));
            Sculpt.lathe(c, poseStack.last(), new float[] {.34f, -.2f, .36f, -.05f, .32f, .1f, .22f, .2f, 0, .24f}, 14, 1, 1, ARMOUR, ARMOUR, light);
            Sculpt.lathe(c, poseStack.last(), new float[] {.3f, -.34f, .33f, -.22f}, 14, 1, 1, ARMOUR, ARMOUR, light);
            poseStack.popPose();
            Vector3f hand = new Vector3f(pose.grip).add(side * .07f, side * .06f, 0);
            Vector3f elbow = new Vector3f(shoulder).lerp(hand, .5f).add(side * .22f, -.18f, -.12f);
            Vector3f[] arm = Sculpt.curve(new Vector3f(shoulder).add(0, -.12f, 0), elbow, hand, 7);
            float[] r = {.15f, .14f, .13f, .14f, .13f, .14f, .16f};
            Sculpt.sweep(c, poseStack.last(), arm, r, r, Sculpt.circle(10), false, null, .8f, Sculpt.fill(ARMOUR, 7), true, light);
            Figures.box(c, poseStack, hand.x, hand.y, hand.z, .2f, .2f, .22f, .04f, ARMOUR, light);
        }
    }

    private static void trim(VertexConsumer c, PoseStack poseStack, Pose pose, int light) {
        // A belt of gold over the tassets
        poseStack.pushPose();
        poseStack.translate(0, pose.hips + .22f, pose.lean * .2f);
        Sculpt.torus(c, poseStack.last(), .4f, .05f, 24, 6, 0, Mth.TWO_PI, 6, 0xFFFFFFFF, light);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0, pose.chest, pose.lean * .4f);
        poseStack.mulPose(new Quaternionf().rotationX(pose.bow));
        // The crown: a band, eight spikes, a gem on its brow
        poseStack.translate(0, pose.head - pose.chest + .2f, 0);
        Sculpt.lathe(c, poseStack.last(), new float[] {.27f, 0, .3f, .02f, .3f, .1f, .27f, .12f}, 20, 4, 1, 0xFFFFFFFF, 0xFFFFFFFF, light);
        for (int k = 0; k < 8; k++) {
            float a = k * Mth.TWO_PI / 8 + Mth.HALF_PI;
            Vector3f base = new Vector3f(Mth.cos(a) * .28f, .1f, Mth.sin(a) * .28f);
            BossMesh.horn(c, poseStack.last(), base, new Vector3f(Mth.cos(a) * .2f, 1, Mth.sin(a) * .2f).normalize(), new Vector3f(), k % 2 == 0 ? .34f : .2f,
                    .055f, 5, 3, 0xFFFFFFFF, 0xFFFFF4C0, light);
        }
        poseStack.pushPose();
        poseStack.translate(0, .06f, .3f);
        poseStack.mulPose(new Quaternionf().rotationX(Mth.HALF_PI));
        Sculpt.gem(c, poseStack.last(), .07f, .04f, .03f, 6, 0xFFFF6080, light);
        poseStack.popPose();
        poseStack.popPose();
        // The pauldrons' gilded rims and spikes
        for (int side = -1; side <= 1; side += 2) {
            poseStack.pushPose();
            poseStack.translate(side * .55f, pose.chest + .38f, pose.lean * .45f);
            poseStack.mulPose(new Quaternionf().rotationZ(-side * .5f));
            poseStack.translate(0, -.2f, 0);
            Sculpt.torus(c, poseStack.last(), .34f, .03f, 16, 5, 0, Mth.TWO_PI, 4, 0xFFFFFFFF, light);
            BossMesh.horn(c, poseStack.last(), new Vector3f(0, .38f, 0), new Vector3f(0, 1, 0), new Vector3f(side * -.1f, 0, 0), .4f, .07f, 5, 3, 0xFFFFFFFF, 0xFFFFF4C0,
                    light);
            poseStack.popPose();
        }
        // The sword's cross, grip wrapping and pommel
        Vector3f d = pose.blade;
        Vector3f cross = new Vector3f(pose.grip).add(new Vector3f(d).mul(.22f));
        poseStack.pushPose();
        poseStack.translate(cross.x, cross.y, cross.z);
        poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 1, 0), d));
        Sculpt.slab(c, poseStack.last(), new float[] {-.55f, -.04f, -.2f, -.06f, 0, -.1f, .2f, -.06f, .55f, -.04f, .62f, .06f, .2f, .06f, 0, .1f, -.2f, .06f, -.62f, .06f},
                .1f, .02f, 2, 0xFFFFFFFF, 0xFFE0C070, light);
        poseStack.translate(0, -.48f, 0);
        Sculpt.lathe(c, poseStack.last(), BellModel.sphere(.08f, 8), 10, 1, 1, 0xFFFFFFFF, 0xFFFFFFFF, light);
        poseStack.popPose();
    }

    private static void sword(VertexConsumer c, PoseStack poseStack, Pose pose, int light) {
        Vector3f d = pose.blade;
        Vector3f base = new Vector3f(pose.grip).add(new Vector3f(d).mul(.27f));
        int n = 8;
        Vector3f[] pts = new Vector3f[n];
        float[] w = new float[n], t = new float[n];
        for (int i = 0; i < n; i++) {
            float f = (float) i / (n - 1);
            pts[i] = new Vector3f(base).add(new Vector3f(d).mul(f * 2.3f));
            w[i] = f < .85f ? .13f - f * .03f : .1f * (1 - f) / .15f + .005f;
            t[i] = .035f * (1 - f) + .008f;
        }
        Sculpt.sweep(c, poseStack.last(), pts, w, t, Sculpt.DIAMOND, true, new Vector3f(1, 0, 0), .4f, Sculpt.fill(0xFFFFFFFF, n), true, light);
        // The grip, wrapped
        Vector3f[] grip = {new Vector3f(pose.grip).add(new Vector3f(d).mul(-.45f)), new Vector3f(pose.grip).add(new Vector3f(d).mul(.2f))};
        Sculpt.sweep(c, poseStack.last(), grip, Sculpt.fill(.045f, 2), Sculpt.fill(.045f, 2), Sculpt.circle(8), false, null, 3, Sculpt.fill(0xFF3A3040, 2), true, light);
    }

    /** His cape, from the shoulders: pooled behind him while he kneels, streaming back as he charges. */
    private static void cape(VertexConsumer c, PoseStack.Pose p, Pose pose, int seed, int light) {
        int cols = 8, rows = 10;
        Vector3f top = new Vector3f(0, pose.chest + .42f, pose.lean * .45f - .32f);
        Vector3f fall = new Vector3f(0, -1, -.25f).lerp(new Vector3f(0, -.35f, -1), pose.stand).normalize();
        float len = Mth.lerp(pose.stand, 2.1f, 2.4f);
        Vector3f[][] grid = new Vector3f[rows + 1][cols + 1];
        for (int i = 0; i <= rows; i++) {
            float v = (float) i / rows;
            for (int j = 0; j <= cols; j++) {
                float u = (float) j / cols - .5f;
                float width = Mth.lerp(v, .95f, 1.5f);
                float wave = Mth.sin(v * 5 - pose.age * (.3f + pose.stand * .6f) + u * 2 + seed) * .14f * v;
                Vector3f at = new Vector3f(top).add(new Vector3f(fall).mul(v * len)).add(u * width, 0, 0);
                // Bellied out at the back, pooling on the ground while he kneels
                at.add(0, wave * pose.stand, -Math.abs(u) * .1f + wave * (1 - pose.stand) - .15f * Mth.sin(v * Mth.PI));
                if (at.y < .02f) {
                    at.z -= (.02f - at.y) * .8f;
                    at.y = .02f;
                }
                grid[i][j] = at;
            }
        }
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Vector3f a = grid[i][j], b = grid[i][j + 1], d = grid[i + 1][j + 1], e = grid[i + 1][j];
                float u0 = (float) j / cols, u1 = (float) (j + 1) / cols, v0 = (float) i / rows * 2, v1 = (float) (i + 1) / rows * 2;
                int c0 = BossMesh.blend(CAPE, 0xFF401018, (float) i / rows * .6f), c1 = BossMesh.blend(CAPE, 0xFF401018, (float) (i + 1) / rows * .6f);
                Vector3f n = new Vector3f(b).sub(a).cross(new Vector3f(e).sub(a)).normalize();
                Sculpt.put(c, p, a, u0, v0, c0, light, n);
                Sculpt.put(c, p, e, u0, v1, c1, light, n);
                Sculpt.put(c, p, d, u1, v1, c1, light, n);
                Sculpt.put(c, p, b, u1, v0, c0, light, n);
                Vector3f m = new Vector3f(n).negate();
                Sculpt.put(c, p, b, u1, v0, 0xFF200810, light, m);
                Sculpt.put(c, p, d, u1, v1, 0xFF200810, light, m);
                Sculpt.put(c, p, e, u0, v1, 0xFF200810, light, m);
                Sculpt.put(c, p, a, u0, v0, 0xFF200810, light, m);
            }
        }
    }
}
