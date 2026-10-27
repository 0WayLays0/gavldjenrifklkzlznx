package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.wood.WorldTree;
import com.waylays.arcanes.spells.wood.WoodFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The World Tree: a trunk braided of six great strands of living wood twisting round one another, a green heart glowing
 * between them near its foot; buttress roots spreading far over the ground; great branches reaching out and up, each
 * with its own heap of leaves, and a crown of azalea in flower over all. Roots lash out along the ground at the enemies
 * it seizes. It tears its way up out of the ground and, at the end, sheds its crown and sinks back.
 */
public class WorldTreeRenderer extends EntityRenderer<WorldTree> {
    private static final int STRANDS = 6;
    private static final int ROOTS = 10;
    private static final int BRANCHES = 9;

    public WorldTreeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(WorldTree tree, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = tree.tickCount + partialTicks;
        float risen = tree.risen(age);
        float withered = tree.withered(age);
        if (risen <= .005f || withered >= 1) {
            return;
        }
        int seed = tree.getId() * 7121;
        float h = WorldTree.HEIGHT;
        float crown = 1 - Mth.clamp(withered * 2, 0, 1);
        poseStack.pushPose();
        poseStack.translate(0, -h * .45f * (1 - risen) - 8 * withered * withered, 0);
        float spin = Math.floorMod(seed, 360) * Mth.DEG_TO_RAD;
        poseStack.mulPose(Axis.YP.rotation(spin));
        float sway = .006f * Mth.sin(age * .03f);
        // The crown: heaps of leaves at the branch ends and over the top
        float spread = (.35f + .65f * risen) * crown;
        Vector3f[] ends = new Vector3f[BRANCHES];
        for (int b = 0; b < BRANCHES; b++) {
            ends[b] = branchEnd(b, seed, h);
        }
        if (spread > .02f) {
            for (int b = 0; b < BRANCHES; b++) {
                Vector3f end = new Vector3f(ends[b]).mul(spread, 1, spread);
                for (int k = 0; k < 6; k++) {
                    int s = seed + b * 61 + k * 7;
                    float s1 = (2.2f + 1.3f * WoodFx.hash(s)) * spread;
                    float x = end.x + (WoodFx.hash(s + 1) - .5f) * 3.4f * spread + sway * end.y;
                    float y = end.y * risen + (WoodFx.hash(s + 2) - .3f) * 2.2f;
                    float z = end.z + (WoodFx.hash(s + 3) - .5f) * 3.4f * spread;
                    RuneRender.block(WoodRender.leaves(s), poseStack, buffer, light, x, y - s1 * .5f, z, s1, s1 * .8f, s1,
                            (WoodFx.hash(s + 4) - .5f) * 24, (WoodFx.hash(s + 5) - .5f) * 24);
                }
            }
            for (int k = 0; k < 14; k++) {
                int s = seed + 999 + k * 13;
                float a = WoodFx.hash(s) * Mth.TWO_PI;
                float r = Mth.sqrt(WoodFx.hash(s + 1)) * 5.5f * spread;
                float s1 = (3 + 1.4f * WoodFx.hash(s + 2)) * spread;
                RuneRender.block(WoodRender.leaves(s), poseStack, buffer, light, Mth.cos(a) * r + sway * h, (h + 1.5f + WoodFx.hash(s + 3) * 2.5f) * risen - s1 * .5f,
                        Mth.sin(a) * r, s1, s1 * .75f, s1, 0, 0);
            }
        }
        VertexConsumer bark = WoodRender.bark(buffer);
        PoseStack.Pose p = poseStack.last();
        // The trunk: six strands braided round one another, thinning as they rise
        int points = 16;
        for (int s = 0; s < STRANDS; s++) {
            Vector3f[] strand = new Vector3f[points];
            for (int i = 0; i < points; i++) {
                float u = (float) i / (points - 1);
                float a = s * Mth.TWO_PI / STRANDS + u * 3.4f;
                float r = Mth.lerp(u, 2.1f, .7f) * (1 + .12f * Mth.sin(u * 11 + s));
                // Flaring out at the foot
                r += 1.6f * (1 - u) * (1 - u) * (1 - u) * (1 - u);
                strand[i] = new Vector3f(Mth.cos(a) * r + sway * u * h, -1.5f + u * (h * .82f + 1.5f) * risen, Mth.sin(a) * r);
            }
            BoneRender.curve(bark, p, strand, 1.15f, .5f, 9, light, BoneRender.WHITE);
        }
        // Buttress roots spreading far over the ground
        for (int k = 0; k < ROOTS; k++) {
            float a = k * Mth.TWO_PI / ROOTS + (WoodFx.hash(seed + k) - .5f) * .4f;
            float len = (7 + 4 * WoodFx.hash(seed + k + 20)) * (.3f + .7f * risen);
            Vector3f[] root = new Vector3f[10];
            for (int i = 0; i < root.length; i++) {
                float u = (float) i / (root.length - 1);
                float r = 1.6f + u * len;
                float wiggle = .25f * Mth.sin(u * 6 + k);
                root[i] = new Vector3f(Mth.cos(a + wiggle) * r, 3.2f * (1 - u) * (1 - u) * (1 - u) - .35f - .3f * u * u, Mth.sin(a + wiggle) * r);
            }
            BoneRender.curve(bark, p, root, 1.0f, .12f, 8, light, BoneRender.WHITE);
        }
        // The great branches, each with two lesser ones off it
        for (int b = 0; b < BRANCHES; b++) {
            Vector3f end = new Vector3f(ends[b]).mul(spread > 0 ? Math.max(spread, .35f) : .35f, 1, spread > 0 ? Math.max(spread, .35f) : .35f);
            Vector3f start = new Vector3f(end.x * .08f + sway * end.y * .6f, end.y * .62f, end.z * .08f);
            Vector3f[] limb = new Vector3f[7];
            for (int i = 0; i < limb.length; i++) {
                float u = (float) i / (limb.length - 1);
                Vector3f at = new Vector3f(start).lerp(end, u);
                at.y = Mth.lerp(u * u, start.y, end.y);
                at.add(.3f * Mth.sin(u * 7 + b), 0, .3f * Mth.cos(u * 6 + b));
                at.y *= risen;
                limb[i] = at;
            }
            BoneRender.curve(bark, p, limb, .62f, .16f, 7, light, BoneRender.WHITE);
            for (int t = 0; t < 2; t++) {
                Vector3f from = limb[3 + t];
                float a = (t == 0 ? 1 : -1) * .9f + b;
                Vector3f to = new Vector3f(from).add(Mth.cos(a) * 2.6f * spread, 2.2f * risen, Mth.sin(a) * 2.6f * spread);
                BoneRender.curve(bark, p, new Vector3f[]{from, new Vector3f(from).lerp(to, .5f).add(0, .5f, 0), to}, .26f, .07f, 5, light, BoneRender.WHITE);
            }
        }
        // Roots lashing out along the ground at those it seizes
        float since = age - tree.getLashTick();
        if (since >= 0 && since < WorldTree.LASH_TICKS) {
            float out = since < 4 ? since / 4 : since > WorldTree.LASH_TICKS - 5 ? (WorldTree.LASH_TICKS - since) / 5 : 1;
            Vec3 origin = new Vec3(Mth.lerp(partialTicks, tree.xo, tree.getX()), Mth.lerp(partialTicks, tree.yo, tree.getY()), Mth.lerp(partialTicks, tree.zo, tree.getZ()));
            for (int id : tree.getLashed()) {
                Entity e = tree.level().getEntity(id);
                if (e == null) {
                    continue;
                }
                Vec3 to = e.getPosition(partialTicks).subtract(origin).yRot(-spin);
                Vec3 flat = new Vec3(to.x, 0, to.z);
                double dist = flat.length();
                if (dist < 2) {
                    continue;
                }
                Vec3 dir = flat.scale(1 / dist);
                Vec3 side = new Vec3(-dir.z, 0, dir.x);
                Vector3f[] lash = new Vector3f[12];
                for (int i = 0; i < lash.length; i++) {
                    float u = (float) i / (lash.length - 1) * out;
                    double along = 2 + (dist - 2) * u;
                    double wave = Math.sin(u * 9 + age * .6f) * .5 * (1 - u);
                    double y = .2 + Math.sin(u * Mth.PI) * 1.2 + (to.y + e.getBbHeight() * .5) * u * u;
                    Vec3 at = dir.scale(along).add(side.scale(wave));
                    lash[i] = new Vector3f((float) at.x, (float) y, (float) at.z);
                }
                BoneRender.curve(bark, p, lash, .45f, .12f, 6, light, BoneRender.WHITE);
                for (int i = 2; i < lash.length - 1; i += 2) {
                    Vector3f o = new Vector3f((float) side.x, .4f, (float) side.z).mul(i % 4 == 0 ? .5f : -.5f);
                    WoodRender.thorn(bark, p, lash[i], new Vector3f(lash[i]).add(o), .12f, light, WoodRender.THORN);
                }
            }
        }
        // The heart glowing between the strands near its foot, motes of life drifting up round it, and its ground
        VertexConsumer glow = buffer.getBuffer(RenderType.eyes(GlyphRender.GLOW));
        float alive = risen * (1 - withered);
        float beat = .7f + .3f * Mth.sin(age * .12f);
        poseStack.pushPose();
        poseStack.translate(0, 3.5f * risen, 0);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        RenderHelper.billboard(glow, poseStack.last(), 3.2f, RuneRender.additive(WoodFx.LIFE, .7f * beat * alive), GlyphRender.FULL);
        RenderHelper.billboard(glow, poseStack.last(), 1.3f, RuneRender.additive(WoodFx.LIFE_CORE, .9f * beat * alive), GlyphRender.FULL);
        poseStack.popPose();
        for (int i = 0; i < 70; i++) {
            float t = (WoodFx.hash(seed + i) + age * (.003f + .003f * WoodFx.hash(seed + i * 3))) % 1;
            float a = WoodFx.hash(seed + i * 5) * Mth.TWO_PI + t * 3;
            float r = 2.5f + WoodFx.hash(seed + i * 7) * (WorldTree.CROWN - 2);
            poseStack.pushPose();
            poseStack.translate(Mth.cos(a) * r, .5f + t * h * .9f, Mth.sin(a) * r);
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            RenderHelper.billboard(glow, poseStack.last(), .2f, RuneRender.additive(i % 4 == 0 ? WoodFx.LIFE_CORE : WoodFx.LIFE, Mth.sin(t * Mth.PI) * alive), GlyphRender.FULL);
            poseStack.popPose();
        }
        poseStack.pushPose();
        poseStack.translate(0, .07f + h * .45f * (1 - risen), 0);
        RenderHelper.groundQuad(glow, poseStack.last(), WorldTree.CROWN + 3, RuneRender.additive(WoodFx.LIFE, .3f * alive), GlyphRender.FULL);
        VertexConsumer ring = buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW));
        poseStack.mulPose(Axis.YP.rotation(age * .006f));
        RenderHelper.groundQuad(ring, poseStack.last(), WorldTree.CROWN + 3, RuneRender.additive(WoodFx.LIFE, .5f * alive), GlyphRender.FULL);
        poseStack.popPose();
        // The ground torn open as it breaks out
        float torn = Mth.clamp(1 - Math.abs(age - WorldTree.BREAK_OUT) / 18, 0, 1);
        if (torn > 0) {
            poseStack.pushPose();
            poseStack.translate(0, .08f + h * .45f * (1 - risen), 0);
            RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(GlyphRender.GLOW)), poseStack.last(), WorldTree.QUAKE, RuneRender.additive(WoodFx.LIFE_CORE, torn * .8f), GlyphRender.FULL);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(tree, entityYaw, partialTicks, poseStack, buffer, light);
    }

    /** Where a great branch ends (before the crown spreads), round the top of the trunk. */
    private static Vector3f branchEnd(int b, int seed, float h) {
        float a = b * Mth.TWO_PI / BRANCHES + (WoodFx.hash(seed + b * 3) - .5f) * .5f;
        float r = 6.5f + 3 * WoodFx.hash(seed + b * 3 + 1);
        float y = h * (.68f + .2f * WoodFx.hash(seed + b * 3 + 2));
        return new Vector3f(Mth.cos(a) * r, y, Mth.sin(a) * r);
    }

    @Override
    public boolean shouldRender(WorldTree tree, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(tree.getBoundingBox().inflate(WorldTree.REACH + 4, WorldTree.HEIGHT + 6, WorldTree.REACH + 4));
    }

    @Override
    public ResourceLocation getTextureLocation(WorldTree tree) {
        return WoodRender.BARK;
    }
}
