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
 * Crimson Lotus: a lotus of crimson glass rises over the spot, three rings of cupped petals edged in gold, floating on a
 * ring of black obsidian leaves; in its heart a seed pod of gold, stamens round it, a light beating in it. The heart
 * bursts in a flash; then the six great outer petals break away and fling themselves out flat along their wedges, their
 * gilded edges burning as they slice; then the whole flower shatters into crimson motes.
 */
public final class LotusModel {
    private static final ResourceLocation GLASS = ArcanesOublies.id("textures/entity/boss/m_crimson_glass.png");
    private static final ResourceLocation OBSIDIAN = ArcanesOublies.id("textures/entity/boss/m_obsidian.png");
    private static final int PETAL_ROWS = 9, PETAL_COLS = 6;
    /** The flower floats this high over the ground. */
    private static final float FLOAT = 1.1f;
    /** How much bigger than its rings say the whole flower is drawn. */
    private static final float SIZE = 1.4f;

    private LotusModel() {}

    /** One ring of petals: how many, turned how far, how long and wide, and how open (radians from upright) closed and open. */
    private record Ring(int count, float offset, float length, float width, float closed, float open, float cup) {}

    private static final Ring[] RINGS = {new Ring(6, 0, 3.1f, 1.45f, .42f, 1.32f, .45f), new Ring(6, .5f, 2.4f, 1.15f, .3f, 1.0f, .5f),
            new Ring(5, .25f, 1.6f, .8f, .16f, .55f, .55f)};

    /** On the ground (the pose's origin); its heart bursts {@code hit} ticks ago (negative before); its petals reach {@code radius}, turned {@code yaw} (degrees). */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float hit, float radius, float yaw, int zone, int seed,
                            int light) {
        float grow = BossMesh.ramp(age, 0, 18);
        float gone = BossMesh.ramp(hit, 22, 32);
        float scale = grow * (1 - gone) * SIZE;
        if (scale < .02f) {
            return;
        }
        // Opening as the warning runs; flung open when the heart bursts
        float bloom = .35f * BossMesh.ramp(age, 6, 40) + .65f * BossMesh.ramp(hit, -2, 4);
        float fling = BossMesh.ramp(hit, 8, 12);
        float base = -yaw * Mth.DEG_TO_RAD;
        float spin = age * .006f;
        float burst = hit >= 0 ? (float) Math.exp(-hit / 4) : 0;
        float heart = .55f + .25f * Mth.sin(age * .35f) + burst * 1.5f;

        // ---- the leaves it floats on (obsidian)
        VertexConsumer stone = BossMesh.solid(buffer, OBSIDIAN);
        poseStack.pushPose();
        poseStack.translate(0, .15f, 0);
        poseStack.scale(scale, scale, scale);
        for (int k = 0; k < 7; k++) {
            float a = base + k * Mth.TWO_PI / 7 + .2f;
            poseStack.pushPose();
            poseStack.mulPose(new Quaternionf().rotationY(-a).rotateX(-Mth.HALF_PI + .12f));
            Sculpt.slab(stone, poseStack.last(), LEAF, .14f, .05f, .5f, 0xFF8A7A96, 0xFF3A3040, light);
            poseStack.popPose();
        }
        poseStack.popPose();

        // ---- the glass petals; the gold of their edges and of the heart
        VertexConsumer glass = BossMesh.see(buffer, GLASS);
        Petal[][] petals = new Petal[RINGS.length][];
        for (int r = 0; r < RINGS.length; r++) {
            Ring ring = RINGS[r];
            petals[r] = new Petal[ring.count];
            for (int k = 0; k < ring.count; k++) {
                float a = base + spin * (r % 2 == 0 ? 1 : -1) + (k + ring.offset) * Mth.TWO_PI / ring.count;
                float tilt = Mth.lerp(bloom, ring.closed, ring.open);
                Vector3f root = new Vector3f(Mth.sin(a) * .2f * (3 - r) * SIZE, FLOAT + .1f * r, Mth.cos(a) * .2f * (3 - r) * SIZE);
                float slide = 0;
                if (r == 0) {
                    // The great petals break away and lie flat out along their wedges
                    tilt = Mth.lerp(fling, tilt, Mth.HALF_PI + .02f);
                    slide = fling * (radius * .55f);
                    root.y = Mth.lerp(fling, root.y, .25f);
                }
                float big = r == 0 ? 1 + .45f * fling : 1;
                Petal pt = petal(root, a, tilt, ring.length * scale * big, ring.width * scale * big, ring.cup * (1 - fling * (r == 0 ? .8f : 0)), slide);
                petals[r][k] = pt;
                int tint = BossMesh.argb(0xFFFFFF, Math.min(1, scale));
                pt.draw(glass, poseStack.last(), tint, light);
            }
        }
        VertexConsumer gold = BossMesh.solid(buffer, BellModel.GOLD);
        for (Petal[] ring : petals) {
            for (Petal pt : ring) {
                pt.rim(gold, poseStack.last(), .035f * scale, light);
            }
        }
        // The seed pod and its stamens
        poseStack.pushPose();
        poseStack.translate(0, FLOAT + .2f, 0);
        poseStack.scale(scale, scale, scale);
        Sculpt.lathe(gold, poseStack.last(), new float[] {.15f, -.3f, .5f, -.12f, .62f, .15f, .6f, .32f, .46f, .42f, 0, .44f}, 20, 3, 1, 0xFFB08040, 0xFFFFFFFF,
                light);
        for (int k = 0; k < 16; k++) {
            float a = k * Mth.TWO_PI / 16 + spin * 2;
            Vector3f from = new Vector3f(Mth.sin(a) * .55f, .3f, Mth.cos(a) * .55f);
            Vector3f to = new Vector3f(Mth.sin(a) * (.95f + .25f * bloom), .95f + .15f * (k % 3), Mth.cos(a) * (.95f + .25f * bloom));
            Vector3f[] s = Sculpt.curve(from, new Vector3f(from).lerp(to, .5f).add(0, .25f, 0), to, 5);
            Sculpt.sweep(gold, poseStack.last(), s, Sculpt.fill(.025f, 5), Sculpt.fill(.025f, 5), Sculpt.circle(5), false, null, 1, Sculpt.fill(0xFFFFFFFF, 5), true,
                    light);
        }
        poseStack.popPose();

        // ---- light: the heart, its burst, the burning edges of the flung petals, motes as it shatters
        VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
        for (Petal[] ring : petals) {
            for (Petal pt : ring) {
                SandMesh.ribbon(beam, poseStack.last(), view.eye(), pt.spine, Sculpt.taper(.18f * scale, .02f, PETAL_ROWS + 1, 1), 1, 0,
                        Sculpt.fill(BossMesh.glow(zone, .35f + .25f * burst), PETAL_ROWS + 1), light);
            }
        }
        if (fling > 0 && gone < 1) {
            float edge = fling * (1 - BossMesh.ramp(hit, 14, 22));
            for (Petal pt : petals[0]) {
                SandMesh.ribbon(beam, poseStack.last(), view.eye(), pt.left, Sculpt.fill(.4f, PETAL_ROWS + 1), 1, 0, Sculpt.fill(BossMesh.glow(zone, edge), PETAL_ROWS + 1),
                        light);
                SandMesh.ribbon(beam, poseStack.last(), view.eye(), pt.right, Sculpt.fill(.4f, PETAL_ROWS + 1), 1, 0, Sculpt.fill(BossMesh.glow(zone, edge), PETAL_ROWS + 1),
                        light);
            }
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        Vector3f core = new Vector3f(0, FLOAT + .75f * scale, 0);
        RitualMesh.billboard(glow, poseStack, view, core, (1.6f + 5 * burst) * scale + 1, 0, -1, BossMesh.glow(0xFFFFFF, Math.min(1, heart * .6f)), light);
        RitualMesh.billboard(glow, poseStack, view, core, (4 + 9 * burst) * scale, 0, -1, BossMesh.glow(zone, Math.min(1, heart * .5f)), light);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(0, .3f, 0), radius * 1.6f * scale, 0, -1, BossMesh.glow(zone, .12f + .3f * burst), light);
        if (gone > 0) {
            for (int k = 0; k < 40; k++) {
                float a = BossMesh.hash(seed + k) * Mth.TWO_PI;
                float r = BossMesh.hash(seed + k * 3) * radius * .8f * (.4f + gone);
                float y = FLOAT + BossMesh.hash(seed + k * 7) * 2 + gone * 2.5f;
                RitualMesh.billboard(glow, poseStack, view, new Vector3f(Mth.sin(a) * r, y, Mth.cos(a) * r), .35f, 0, -1, BossMesh.glow(zone, 1 - gone), light);
            }
        }
    }

    /** A leaf of the pad, its stem at the origin, reaching out along +Y. */
    private static final float[] LEAF = {0, 0, .55f, .5f, .9f, 1.4f, .95f, 2.2f, .7f, 2.9f, .2f, 3.3f, 0, 3.0f, -.2f, 3.3f, -.7f, 2.9f, -.95f, 2.2f, -.9f, 1.4f,
            -.55f, .5f};

    /** One petal's surface: its grid, the line down its middle, its two edges. */
    private record Petal(Vector3f[][] grid, Vector3f[] spine, Vector3f[] left, Vector3f[] right) {
        void draw(VertexConsumer c, PoseStack.Pose p, int tint, int light) {
            for (int i = 0; i < PETAL_ROWS; i++) {
                for (int j = 0; j < PETAL_COLS; j++) {
                    Vector3f a = grid[i][j], b = grid[i][j + 1], d = grid[i + 1][j + 1], e = grid[i + 1][j];
                    float u0 = (float) j / PETAL_COLS, u1 = (float) (j + 1) / PETAL_COLS, v0 = (float) i / PETAL_ROWS, v1 = (float) (i + 1) / PETAL_ROWS;
                    BossMesh.quad2(c, p, a, b, d, e, u0, v0, u1, v0, u1, v1, u0, v1, tint, tint, light);
                }
            }
        }

        void rim(VertexConsumer c, PoseStack.Pose p, float r, int light) {
            int n = left.length;
            Vector3f[] edge = new Vector3f[2 * n - 1];
            for (int i = 0; i < n; i++) {
                edge[i] = left[n - 1 - i];
                edge[n - 1 + i] = right[i];
            }
            Sculpt.sweep(c, p, edge, Sculpt.fill(r, edge.length), Sculpt.fill(r, edge.length), Sculpt.circle(5), false, null, 1, Sculpt.fill(0xFFFFFFFF, edge.length),
                    false, light);
        }
    }

    /**
     * A petal from {@code root}, turned {@code a} round the flower, leaning {@code tilt} from upright, {@code len} long and
     * {@code width} wide at its widest, its sides cupped up by {@code cup}; slid {@code slide} out along its way.
     */
    private static Petal petal(Vector3f root, float a, float tilt, float len, float width, float cup, float slide) {
        Vector3f out = new Vector3f(Mth.sin(a), 0, Mth.cos(a));
        Vector3f side = new Vector3f(out.z, 0, -out.x);
        Vector3f start = new Vector3f(root).add(new Vector3f(out).mul(slide));
        Vector3f[][] grid = new Vector3f[PETAL_ROWS + 1][PETAL_COLS + 1];
        Vector3f[] spine = new Vector3f[PETAL_ROWS + 1], left = new Vector3f[PETAL_ROWS + 1], right = new Vector3f[PETAL_ROWS + 1];
        Vector3f at = new Vector3f(start);
        for (int i = 0; i <= PETAL_ROWS; i++) {
            float v = (float) i / PETAL_ROWS;
            // Leaning out more toward the tip
            float t = tilt + v * .45f;
            Vector3f dir = new Vector3f(out).mul(Mth.sin(t)).add(0, Mth.cos(t), 0);
            Vector3f inward = new Vector3f(out).mul(-Mth.cos(t)).add(0, Mth.sin(t), 0);
            if (i > 0) {
                at.add(new Vector3f(dir).mul(len / PETAL_ROWS));
            }
            spine[i] = new Vector3f(at);
            float w = width * .5f * (float) Math.pow(Math.max(0, Mth.sin(Mth.PI * Math.min(1f, (float) Math.pow(v, .75f)))), .8f) + (i == 0 ? width * .12f : 0);
            for (int j = 0; j <= PETAL_COLS; j++) {
                float u = (float) j / PETAL_COLS * 2 - 1;
                grid[i][j] = new Vector3f(at).add(new Vector3f(side).mul(u * w)).add(new Vector3f(inward).mul(cup * u * u * w));
            }
            left[i] = grid[i][0];
            right[i] = grid[i][PETAL_COLS];
        }
        return new Petal(grid, spine, left, right);
    }
}
