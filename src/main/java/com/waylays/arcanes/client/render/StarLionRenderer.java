package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.entity.cosmos.StarLion;
import com.waylays.arcanes.spells.cosmos.Cosmos;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The Lion of the stars, drawn as a constellation: a star at each joint, lines of pale gold between them, a mane of
 * rays round its head, a haze of blue nebula filling its body and two icy stars for eyes. It walks with a lion's
 * gait, sways its tail, raises a paw to swipe and stretches out when it leaps. It forms from stars coming down from
 * the sky, and the lines are drawn between them as they arrive.
 */
public class StarLionRenderer extends EntityRenderer<StarLion> {
    // The joints, in blocks, facing +Z
    private static final int HIP = 0, BACK = 1, WITHERS = 2, NECK = 3, HEAD = 4, SNOUT = 5, JAW = 6, EAR_L = 7, EAR_R = 8, CHEST = 9, BELLY = 10;
    private static final int FL = 11, FR = 14, HL = 17, HR = 21, TAIL = 25;
    private static final float[][] JOINTS = {
            {0, 1.0f, -.75f}, {0, 1.12f, 0}, {0, 1.24f, .62f}, {0, 1.5f, .95f}, {0, 1.6f, 1.32f}, {0, 1.38f, 1.74f}, {0, 1.16f, 1.46f},
            {.24f, 1.9f, 1.2f}, {-.24f, 1.9f, 1.2f}, {0, .86f, .74f}, {0, .8f, -.36f},
            // Front legs: shoulder, elbow, paw (left, then right)
            {.3f, 1.06f, .66f}, {.33f, .56f, .74f}, {.33f, .04f, .84f},
            {-.3f, 1.06f, .66f}, {-.33f, .56f, .74f}, {-.33f, .04f, .84f},
            // Hind legs: hip, knee, hock, paw
            {.3f, .98f, -.72f}, {.33f, .56f, -.5f}, {.33f, .3f, -.94f}, {.33f, .04f, -.84f},
            {-.3f, .98f, -.72f}, {-.33f, .56f, -.5f}, {-.33f, .3f, -.94f}, {-.33f, .04f, -.84f},
            // Tail, out to its tuft
            {0, 1.05f, -.95f}, {0, 1.28f, -1.45f}, {0, 1.08f, -1.86f}, {0, 1.24f, -2.18f},
    };
    private static final int[][] LINES = {
            {HIP, BACK}, {BACK, WITHERS}, {WITHERS, NECK}, {NECK, HEAD}, {HEAD, SNOUT}, {SNOUT, JAW}, {JAW, NECK}, {HEAD, EAR_L}, {HEAD, EAR_R},
            {WITHERS, CHEST}, {CHEST, BELLY}, {BELLY, HIP},
            {WITHERS, FL}, {FL, FL + 1}, {FL + 1, FL + 2}, {WITHERS, FR}, {FR, FR + 1}, {FR + 1, FR + 2}, {CHEST, FL}, {CHEST, FR},
            {HIP, HL}, {HL, HL + 1}, {HL + 1, HL + 2}, {HL + 2, HL + 3}, {HIP, HR}, {HR, HR + 1}, {HR + 1, HR + 2}, {HR + 2, HR + 3},
            {HIP, TAIL}, {TAIL, TAIL + 1}, {TAIL + 1, TAIL + 2}, {TAIL + 2, TAIL + 3},
    };
    /** The bright stars of the figure (the heart, the head, the tail's tip) and how big each joint's star is. */
    private static final float[] STAR_SIZE = new float[JOINTS.length];
    private static final int MANE_RAYS = 12;
    private static final Vec3 MANE = new Vec3(0, 1.46f, 1.06f);

    static {
        java.util.Arrays.fill(STAR_SIZE, .32f);
        STAR_SIZE[WITHERS] = .7f;
        STAR_SIZE[HEAD] = .6f;
        STAR_SIZE[TAIL + 3] = .6f;
        STAR_SIZE[HIP] = .5f;
        STAR_SIZE[SNOUT] = .4f;
        for (int paw : new int[]{FL + 2, FR + 2, HL + 3, HR + 3}) {
            STAR_SIZE[paw] = .42f;
        }
    }

    public StarLionRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0;
    }

    @Override
    public void render(StarLion lion, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = lion.tickCount + partialTicks;
        Vec3[] p = pose(lion, partialTicks, age);
        float yaw = -Mth.rotLerp(partialTicks, lion.yBodyRotO, lion.yBodyRot) * Mth.DEG_TO_RAD;
        // Coming down from the sky, joint by joint
        float[] arrived = new float[p.length];
        for (int i = 0; i < p.length; i++) {
            float start = 3 + i * .6f + RuneRender.hash(i * 7 + 3) * 4;
            float t = Mth.clamp((age - start) / 10, 0, 1);
            arrived[i] = t;
            Vec3 world = p[i].yRot(yaw);
            if (t < 1) {
                Vec3 sky = world.add((RuneRender.hash(i * 11) - .5f) * 6, 9 + RuneRender.hash(i * 13) * 5, (RuneRender.hash(i * 17) - .5f) * 6);
                float e = 1 - (1 - t) * (1 - t) * (1 - t);
                world = sky.lerp(world, e);
            }
            p[i] = world;
        }
        float hurt = lion.hurtTime > 0 ? lion.hurtTime / 10f : 0;
        int lineColour = GoldRender.lerp(Cosmos.GOLD, 0xFF4030, hurt);
        float fade = lion.isAlive() ? 1 : .4f;
        VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
        for (int[] line : LINES) {
            float shown = Math.min(arrived[line[0]], arrived[line[1]]);
            if (shown < 1) {
                shown = Mth.clamp((shown - .8f) * 5, 0, 1);
            }
            if (shown <= 0) {
                continue;
            }
            Vec3 a = p[line[0]], b = p[line[1]];
            Vec3 end = a.lerp(b, shown);
            GlyphRender.beam(stroke, poseStack.last(), a, end, .22f, RuneRender.additive(Cosmos.BLUE, .35f * fade));
            GlyphRender.beam(stroke, poseStack.last(), a, end, .07f, RuneRender.additive(lineColour, .9f * fade));
        }
        // The mane: rays round the head, shimmering
        float mane = Math.min(arrived[NECK], arrived[HEAD]);
        if (mane > 0) {
            Vec3 centre = rotate(MANE.add(0, bob(lion, partialTicks), 0), yaw);
            for (int k = 0; k < MANE_RAYS; k++) {
                float a = k * Mth.TWO_PI / MANE_RAYS;
                float len = (k % 2 == 0 ? .78f : .5f) * (1 + .08f * Mth.sin(age * .2f + k)) * mane;
                Vec3 dir = new Vec3(Mth.cos(a), Mth.sin(a) * .95f, -.25f).normalize();
                Vec3 from = centre.add(rotate(dir.scale(.3), yaw));
                Vec3 to = centre.add(rotate(dir.scale(.3 + len), yaw));
                GlyphRender.beam(stroke, poseStack.last(), from, to, .16f, RuneRender.additive(Cosmos.ORANGE, .45f * fade));
                GlyphRender.beam(stroke, poseStack.last(), from, to, .05f, RuneRender.additive(Cosmos.CREAM, .8f * fade));
            }
        }
        // The nebula of its body
        float body = Math.min(arrived[BACK], arrived[CHEST]);
        if (body > 0) {
            for (int i : new int[]{BACK, WITHERS, HIP, CHEST, BELLY, NECK}) {
                GemRender.glow(poseStack, buffer, p[i].add(0, -.12, 0), i % 2 == 0 ? Cosmos.BLUE : Cosmos.VIOLET, 1.1f, .2f * body * fade);
            }
        }
        // The stars
        for (int i = 0; i < p.length; i++) {
            if (arrived[i] <= 0 && age < 3) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(p[i].x, p[i].y, p[i].z);
            int colour = i == WITHERS ? 0xCFE0FF : i == TAIL + 3 ? 0xFFF4D6 : Cosmos.STAR;
            CosmosRender.twinkle(poseStack, buffer, STAR_SIZE[i] * (1.3f - .3f * arrived[i]), colour, .95f * fade, age, i * 31 + lion.getId());
            poseStack.popPose();
        }
        // Its eyes
        if (arrived[HEAD] >= 1) {
            for (int side = -1; side <= 1; side += 2) {
                Vec3 eye = p[HEAD].add(rotate(new Vec3(side * .14, -.06, .2), yaw));
                poseStack.pushPose();
                poseStack.translate(eye.x, eye.y, eye.z);
                CosmosRender.star(poseStack, buffer, .3f, Cosmos.ICE, 1, age * 3);
                poseStack.popPose();
            }
        }
        super.render(lion, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static Vec3 rotate(Vec3 v, float yaw) {
        return v.yRot(yaw);
    }

    /** The head's bob as it walks. */
    private static float bob(StarLion lion, float partialTicks) {
        float limb = lion.walkAnimation.position(partialTicks);
        float amount = Math.min(1, lion.walkAnimation.speed(partialTicks));
        return Mth.sin(limb * 1.33f) * .05f * amount;
    }

    /** The joints of the lion as it stands now, facing +Z. */
    private static Vec3[] pose(StarLion lion, float partialTicks, float age) {
        Vec3[] p = new Vec3[JOINTS.length];
        for (int i = 0; i < p.length; i++) {
            p[i] = new Vec3(JOINTS[i][0], JOINTS[i][1], JOINTS[i][2]);
        }
        float limb = lion.walkAnimation.position(partialTicks) * .6662f;
        float amount = Math.min(1, lion.walkAnimation.speed(partialTicks));
        float leap = Mth.clamp((age - lion.getPounceTick()) / 14, 0, 1);
        float leaping = leap > 0 && leap < 1 ? Mth.sin(leap * Mth.PI) : 0;
        float swipe = Mth.clamp((age - lion.getSwipeTick()) / 7, 0, 1);
        float swiping = swipe > 0 && swipe < 1 ? Mth.sin(swipe * Mth.PI) : 0;
        float breathe = Mth.sin(age * .1f) * .02f;
        float bob = bob(lion, partialTicks);
        // Body and head
        for (int i : new int[]{BACK, WITHERS, CHEST, BELLY}) {
            p[i] = p[i].add(0, breathe, 0);
        }
        for (int i : new int[]{NECK, HEAD, SNOUT, JAW, EAR_L, EAR_R}) {
            p[i] = p[i].add(0, bob + breathe, 0);
        }
        // The jaw drops as it roars and strikes
        float roar = Math.max(leaping, swiping);
        p[JAW] = p[JAW].add(0, -.18 * roar, -.05 * roar);
        // Legs: a lion's walk, diagonal pairs together; stretched out fore and aft as it leaps
        leg(p, FL, 3, Mth.sin(limb) * .9f * amount - 1.1f * leaping - 1.5f * swiping);
        leg(p, FR, 3, Mth.sin(limb + Mth.PI) * .9f * amount - 1.1f * leaping);
        leg(p, HL, 4, Mth.sin(limb + Mth.PI) * .8f * amount + 1.0f * leaping);
        leg(p, HR, 4, Mth.sin(limb) * .8f * amount + 1.0f * leaping);
        // The tail sways, more toward its tip
        float sway = Mth.sin(age * .12f) * .35f + Mth.sin(limb) * .2f * amount;
        Vec3 root = p[TAIL];
        for (int k = 1; k < 4; k++) {
            Vec3 rel = p[TAIL + k].subtract(root);
            p[TAIL + k] = root.add(rel.yRot(sway * k * .5f)).add(0, .3 * leaping * k / 3, 0);
        }
        return p;
    }

    /** Swings the leg from joint {@code first} ({@code count} joints) by {@code angle} round its top, forward when negative. */
    private static void leg(Vec3[] p, int first, int count, float angle) {
        Vec3 top = p[first];
        for (int k = 1; k < count; k++) {
            Vec3 rel = p[first + k].subtract(top);
            // A bent knee when the leg is swung back
            float bend = k == count - 1 && angle > 0 ? angle * .4f : 0;
            p[first + k] = top.add(rel.xRot(-(angle + bend)));
        }
    }

    @Override
    public ResourceLocation getTextureLocation(StarLion lion) {
        return GlyphRender.STROKE;
    }
}
