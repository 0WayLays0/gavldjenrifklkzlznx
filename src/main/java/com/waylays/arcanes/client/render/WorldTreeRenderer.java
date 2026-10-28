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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The World Tree: a trunk braided of six great strands of living wood twisting round one another, a green heart glowing
 * between them near its foot; buttress roots spreading far over the ground; great branches reaching out and up, each
 * with its own heap of leaves, and a crown of azalea in flower over all. Roots lash out along the ground at the enemies
 * it seizes. It tears its way up out of the ground.
 * <p>
 * Its end takes four and a half seconds: its leaves turn gold, then rust, and fall heap by heap, turning over; its
 * heart dims to amber and goes out; its bark dries and greys; its branches sag, snap one by one and fall; its strands
 * unbraid and splay as it sinks back into the earth, its roots drawing in after it; and a last breath of life rises
 * from where it stood.
 */
public class WorldTreeRenderer extends EntityRenderer<WorldTree> {
    private static final int STRANDS = 6;
    private static final int ROOTS = 10;
    private static final int BRANCHES = 9;
    private static final BlockState OAK_LEAVES = Blocks.OAK_LEAVES.defaultBlockState();
    private static final int GREEN = 0x6FA83A;
    private static final int GOLD = 0xE2B030;
    private static final int RUST = 0xB4481C;
    private static final int DRY = 0x958B82;

    public WorldTreeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(WorldTree tree, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = tree.tickCount + partialTicks;
        float risen = tree.risen(age);
        // Ticks since its end began (negative while it stands)
        float end = age - tree.getLife();
        if (risen <= .005f || end > WorldTree.WITHER) {
            return;
        }
        int seed = tree.getId() * 7121;
        float h = WorldTree.HEIGHT;
        float autumn = Mth.clamp(end / 25, 0, 1);
        float dry = Mth.clamp((end - 25) / 30, 0, 1);
        float unravel = Mth.clamp((end - 50) / 38, 0, 1);
        float rootsIn = Mth.clamp((end - 55) / 30, 0, 1);
        float sink = unravel * unravel * h * .7f;
        int leafColour = autumn < .5f ? GoldRender.lerp(GREEN, GOLD, autumn * 2) : GoldRender.lerp(GOLD, RUST, (autumn - .5f) * 2);
        int barkTint = 0xFF000000 | GoldRender.lerp(0xFFFFFF, DRY, dry);
        float alive = risen * (1 - Mth.clamp(end / 60, 0, 1));
        poseStack.pushPose();
        poseStack.translate(0, -h * .45f * (1 - risen) - sink, 0);
        float spin = Math.floorMod(seed, 360) * Mth.DEG_TO_RAD;
        poseStack.mulPose(Axis.YP.rotation(spin));
        float sway = end > 0 ? 0 : .006f * Mth.sin(age * .03f);
        float spread = .35f + .65f * risen;
        Vector3f[] ends = new Vector3f[BRANCHES];
        for (int b = 0; b < BRANCHES; b++) {
            ends[b] = branchEnd(b, seed, h);
        }

        // The crown: heaps of leaves at the branch ends and over the top (azalea in flower while it lives, oak leaves
        // turning gold and rust as it dies, falling heap by heap)
        for (int b = 0; b < BRANCHES; b++) {
            Vector3f tip = new Vector3f(ends[b]).mul(spread, 1, spread);
            for (int k = 0; k < 6; k++) {
                int s = seed + b * 61 + k * 7;
                float size = (2.2f + 1.3f * WoodFx.hash(s)) * spread;
                float x = tip.x + (WoodFx.hash(s + 1) - .5f) * 3.4f * spread + sway * tip.y;
                float y = tip.y * risen + (WoodFx.hash(s + 2) - .3f) * 2.2f;
                float z = tip.z + (WoodFx.hash(s + 3) - .5f) * 3.4f * spread;
                leafHeap(poseStack, buffer, light, s, x, y, z, size, end, autumn, leafColour, sink);
            }
        }
        for (int k = 0; k < 14; k++) {
            int s = seed + 999 + k * 13;
            float a = WoodFx.hash(s) * Mth.TWO_PI;
            float r = Mth.sqrt(WoodFx.hash(s + 1)) * 5.5f * spread;
            float size = (3 + 1.4f * WoodFx.hash(s + 2)) * spread;
            leafHeap(poseStack, buffer, light, s, Mth.cos(a) * r + sway * h, (h + 1.5f + WoodFx.hash(s + 3) * 2.5f) * risen, Mth.sin(a) * r, size, end, autumn, leafColour, sink);
        }

        VertexConsumer bark = WoodRender.bark(buffer);
        PoseStack.Pose p = poseStack.last();
        // The trunk: six strands braided round one another, thinning as they rise; unbraiding and splaying at the end
        int points = 16;
        for (int s = 0; s < STRANDS; s++) {
            Vector3f[] strand = new Vector3f[points];
            for (int i = 0; i < points; i++) {
                float u = (float) i / (points - 1);
                float a = s * Mth.TWO_PI / STRANDS + u * 3.4f * (1 - .7f * unravel);
                float r = Mth.lerp(u, 2.1f, .7f) * (1 + .12f * Mth.sin(u * 11 + s));
                r += 1.6f * (1 - u) * (1 - u) * (1 - u) * (1 - u);
                r *= 1 + unravel * (.4f + 1.6f * u * u);
                float top = (h * .82f + 1.5f) * (1 - .3f * unravel);
                strand[i] = new Vector3f(Mth.cos(a) * r + sway * u * h, -1.5f + u * top * risen - unravel * u * u * 2, Mth.sin(a) * r);
            }
            BoneRender.curve(bark, p, strand, 1.15f, .5f * (1 - .4f * unravel), 9, light, barkTint);
        }
        // Buttress roots spreading far over the ground, drawing in at the end
        for (int k = 0; k < ROOTS; k++) {
            float a = k * Mth.TWO_PI / ROOTS + (WoodFx.hash(seed + k) - .5f) * .4f;
            float len = (7 + 4 * WoodFx.hash(seed + k + 20)) * (.3f + .7f * risen) * (1 - rootsIn);
            Vector3f[] root = new Vector3f[10];
            for (int i = 0; i < root.length; i++) {
                float u = (float) i / (root.length - 1);
                float r = 1.6f + u * len;
                float wiggle = .25f * Mth.sin(u * 6 + k);
                root[i] = new Vector3f(Mth.cos(a + wiggle) * r, 3.2f * (1 - u) * (1 - u) * (1 - u) - .35f - .3f * u * u + sink * u * .9f, Mth.sin(a + wiggle) * r);
            }
            BoneRender.curve(bark, p, root, 1.0f, .12f, 8, light, barkTint);
        }
        // The great branches, each with two lesser ones off it: sagging, snapping and falling at the end
        for (int b = 0; b < BRANCHES; b++) {
            float snapAt = 35 + 25 * WoodFx.hash(seed + b * 3 + 7);
            float fell = end - snapAt;
            float fallShrink = fell > 0 ? Mth.clamp(1 - (fell - 12) / 10, 0, 1) : 1;
            if (fallShrink <= 0) {
                continue;
            }
            Vector3f tip = new Vector3f(ends[b]).mul(Math.max(spread, .35f), 1, Math.max(spread, .35f));
            Vector3f start = new Vector3f(tip.x * .08f + sway * tip.y * .6f, tip.y * .62f, tip.z * .08f);
            float sag = end > 25 ? Mth.clamp((end - 25) / Math.max(1, snapAt - 25), 0, 1) * .35f : 0;
            Vector3f[] limb = new Vector3f[7];
            for (int i = 0; i < limb.length; i++) {
                float u = (float) i / (limb.length - 1);
                Vector3f at = new Vector3f(start).lerp(tip, u);
                at.y = Mth.lerp(u * u, start.y, tip.y);
                at.add(.3f * Mth.sin(u * 7 + b), 0, .3f * Mth.cos(u * 6 + b));
                at.y *= risen;
                // Sagging: the far end drops
                at.y -= sag * u * u * 4;
                limb[i] = at;
            }
            if (fell > 0) {
                // Snapped: it drops, turning, and crumbles where it lands
                // (the ground is at y = sink in this frame, the tree having sunk)
                float drop = Math.min(.04f * fell * fell, Math.max(0, limb[3].y - sink - .6f));
                Vector3f pivot = new Vector3f(limb[3]);
                float flat = Math.max(.01f, Mth.sqrt(pivot.x * pivot.x + pivot.z * pivot.z));
                for (int i = 0; i < limb.length; i++) {
                    Vector3f rel = new Vector3f(limb[i]).sub(pivot).mul(fallShrink);
                    rel.rotateAxis(Math.min(fell * .06f, 1.2f), -pivot.z / flat, 0, pivot.x / flat);
                    limb[i] = rel.add(pivot).sub(0, drop, 0);
                }
            }
            BoneRender.curve(bark, p, limb, .62f * fallShrink, .16f * fallShrink, 7, light, barkTint);
            for (int t = 0; t < 2; t++) {
                Vector3f from = limb[3 + t];
                float a = (t == 0 ? 1 : -1) * .9f + b;
                Vector3f to = new Vector3f(from).add(Mth.cos(a) * 2.6f * spread * fallShrink, 2.2f * risen * fallShrink - sag * 2, Mth.sin(a) * 2.6f * spread * fallShrink);
                BoneRender.curve(bark, p, new Vector3f[]{from, new Vector3f(from).lerp(to, .5f).add(0, .5f * fallShrink, 0), to}, .26f * fallShrink, .07f * fallShrink, 5, light, barkTint);
            }
        }
        // Roots lashing out along the ground at those it seizes
        float since = age - tree.getLashTick();
        if (end < 0 && since >= 0 && since < WorldTree.LASH_TICKS) {
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
                    double y = .2 + Math.sin(u * Math.PI) * 1.2 + (to.y + e.getBbHeight() * .5) * u * u;
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

        // The heart glowing between the strands near its foot (dimming to amber as it dies), motes of life, its ground
        VertexConsumer glow = buffer.getBuffer(RenderType.eyes(GlyphRender.GLOW));
        float beat = .7f + .3f * Mth.sin(age * .12f);
        int heart = GoldRender.lerp(WoodFx.LIFE, 0xE08A30, autumn);
        poseStack.pushPose();
        poseStack.translate(0, 3.5f * risen, 0);
        poseStack.mulPose(Axis.YP.rotation(-spin));
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        RenderHelper.billboard(glow, poseStack.last(), 3.2f, RuneRender.additive(heart, .7f * beat * alive), GlyphRender.FULL);
        RenderHelper.billboard(glow, poseStack.last(), 1.3f, RuneRender.additive(WoodFx.LIFE_CORE, .9f * beat * alive), GlyphRender.FULL);
        poseStack.popPose();
        for (int i = 0; i < 70; i++) {
            float t = (WoodFx.hash(seed + i) + age * (.003f + .003f * WoodFx.hash(seed + i * 3))) % 1;
            float a = WoodFx.hash(seed + i * 5) * Mth.TWO_PI + t * 3;
            float r = 2.5f + WoodFx.hash(seed + i * 7) * (WorldTree.CROWN - 2);
            billboard(poseStack, glow, spin, Mth.cos(a) * r, .5f + t * h * .9f, Mth.sin(a) * r, .2f,
                    RuneRender.additive(i % 4 == 0 ? WoodFx.LIFE_CORE : WoodFx.LIFE, Mth.sin(t * Mth.PI) * alive));
        }
        poseStack.pushPose();
        poseStack.translate(0, .07f + h * .45f * (1 - risen) + sink, 0);
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
        // A last breath of life rising from where it stood, and a ring running out over the ground
        float last = Mth.clamp((end - 68) / (WorldTree.WITHER - 68), 0, 1);
        if (last > 0) {
            VertexConsumer motes = buffer.getBuffer(RenderType.eyes(GlyphRender.GLOW));
            float fade = Mth.sin(last * Mth.PI);
            for (int i = 0; i < 60; i++) {
                float a = WoodFx.hash(seed + i * 17) * Mth.TWO_PI;
                float r = Mth.sqrt(WoodFx.hash(seed + i * 17 + 1)) * 6;
                float y = last * (4 + 10 * WoodFx.hash(seed + i * 17 + 2)) + .3f;
                billboard(poseStack, motes, spin, Mth.cos(a) * r, y + sink, Mth.sin(a) * r, .25f,
                        RuneRender.additive(i % 3 == 0 ? WoodFx.LIFE_CORE : WoodFx.LIFE, fade));
            }
            poseStack.pushPose();
            poseStack.translate(0, .09f + sink, 0);
            RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), 2 + last * 14,
                    RuneRender.additive(WoodFx.LIFE_CORE, fade * .9f), GlyphRender.FULL);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(tree, entityYaw, partialTicks, poseStack, buffer, light);
    }

    /**
     * One heap of leaves: azalea in flower while the tree lives; as it dies, oak leaves turning {@code colour}, then
     * falling (each heap at its own time), turning over and shrinking away.
     */
    private static void leafHeap(PoseStack poseStack, MultiBufferSource buffer, int light, int s, float x, float y, float z, float size,
                                 float end, float autumn, int colour, float sink) {
        float tiltX = (WoodFx.hash(s + 4) - .5f) * 24, tiltZ = (WoodFx.hash(s + 5) - .5f) * 24;
        if (end <= 0) {
            RuneRender.block(WoodRender.leaves(s), poseStack, buffer, light, x, y - size * .5f, z, size, size * .8f, size, tiltX, tiltZ);
            return;
        }
        float fallAt = 18 + 35 * WoodFx.hash(s + 6);
        float fell = end - fallAt;
        if (fell > 0) {
            float shrink = Mth.clamp(1 - (fell - 10) / 14, 0, 1);
            if (shrink <= 0) {
                return;
            }
            // Falling, drifting and turning over, down to the ground
            y = Math.max(sink + size * .3f * shrink, y - .02f * fell * fell);
            x += (WoodFx.hash(s + 7) - .5f) * fell * .12f;
            z += (WoodFx.hash(s + 8) - .5f) * fell * .12f;
            tiltX += fell * 9 * (WoodFx.hash(s + 9) - .5f);
            tiltZ += fell * 7;
            size *= shrink;
        }
        // A heap a little lighter or darker than the rest
        int c = GoldRender.lerp(colour, WoodFx.hash(s + 10) < .5f ? 0xFFE08A : 0x6A3010, .2f * WoodFx.hash(s + 11) * autumn);
        WoodRender.tintedBlock(OAK_LEAVES, poseStack, buffer, light, x, y - size * .5f, z, size, size * .8f, size, tiltX, tiltZ, c);
    }

    /** A glow facing the camera at (x, y, z) of the tree's turned frame. */
    private void billboard(PoseStack poseStack, VertexConsumer glow, float spin, float x, float y, float z, float half, int argb) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.YP.rotation(-spin));
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        RenderHelper.billboard(glow, poseStack.last(), half, argb, GlyphRender.FULL);
        poseStack.popPose();
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
