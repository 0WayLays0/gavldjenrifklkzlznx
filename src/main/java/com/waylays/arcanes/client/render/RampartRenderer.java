package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.wall.Rampart;
import com.waylays.arcanes.spells.wall.WallFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * A rampart of real blocks, while it builds and while it stands: each block flies up out of the ground into its place
 * (the real block takes over once it is there), the mortar between the blocks glows like embers (flaring as each block
 * lands), runes burn on some of them, and the mason's sigil burns underneath while it builds. When it crumbles, its
 * blocks fall apart and sink.
 */
public class RampartRenderer extends EntityRenderer<Rampart> {
    public RampartRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(Rampart rampart, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = rampart.tickCount + partialTicks;
        int life = rampart.getLife();
        List<Rampart.Cell> cells = rampart.cells();
        BlockPos foot = rampart.blockPosition();
        float built = rampart.built();
        float sigilStrength = Mth.clamp(age / 6, 0, 1) * Mth.clamp(1 - (age - built) / 25, 0, 1);
        float radius = switch (rampart.getShape()) {
            case WALL -> 3.5f;
            case CAGE -> 1.4f + rampart.getSize();
            case BASTION -> 5;
            case RING -> 11;
        };
        WallRender.sigil(poseStack, buffer, radius, sigilStrength, age * 1.2f);
        // Three passes, one buffer each: a buffer must not be written to again once another one has been asked for
        // (asking for it sends the first off)
        for (Rampart.Cell c : cells) {
            int seed = (int) foot.offset(c.dx(), c.dy(), c.dz()).asLong();
            BlockState state = WallFx.brick(seed);
            float since = age - c.delay();
            if (since < 0) {
                continue;
            }
            if (age >= life) {
                // Crumbling: each block topples out and sinks, the top ones first
                float t = age - life - RuneRender.hash(seed) * 5;
                float dir = c.face() < 0 ? 0 : c.face() * Mth.HALF_PI;
                float vx = -Mth.sin(dir) * .06f + (RuneRender.hash(seed + 1) - .5f) * .08f;
                float vz = Mth.cos(dir) * .06f + (RuneRender.hash(seed + 2) - .5f) * .08f;
                WallRender.fragment(state, poseStack, buffer, packedLight, c.dx(), c.dy() + .5f, c.dz(), vx, .12f, vz, Math.max(0, t),
                        Rampart.CRUMBLE, 1, seed);
            } else if (since < Rampart.CELL_RISE) {
                // Flying up out of the ground into its place, a little past it and back (the real block takes over)
                float t = since / Rampart.CELL_RISE;
                float lift = -(1 - t) * (1 - t) * 2.4f + .15f * Mth.sin(t * Mth.PI);
                poseStack.pushPose();
                poseStack.translate(c.dx(), c.dy() + lift, c.dz());
                poseStack.mulPose(Axis.YP.rotationDegrees((1 - t) * 40 * (RuneRender.hash(seed) - .5f)));
                WallRender.brick(state, poseStack, buffer, packedLight, 0, 0, 0, 1);
                poseStack.popPose();
            }
        }
        if (age >= life) {
            super.render(rampart, entityYaw, partialTicks, poseStack, buffer, packedLight);
            return;
        }
        // Standing blocks: their mortar glows, flaring as each lands, and dies down at the end
        VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
        float fade = Mth.clamp((life - age) / 20, 0, 1);
        for (Rampart.Cell c : cells) {
            float landed = age - c.delay() - Rampart.CELL_RISE;
            if (landed < 0) {
                continue;
            }
            int seed = (int) foot.offset(c.dx(), c.dy(), c.dz()).asLong();
            float flare = Math.max(0, 1 - landed / 10);
            float pulse = .4f + .15f * Mth.sin(age * .15f + RuneRender.hash(seed) * 6);
            WallRender.mortar(stroke, poseStack.last(), c.dx(), c.dy(), c.dz(), c.face(), .07f,
                    WallRender.ember(.4f + .6f * flare, Math.min(1, pulse + flare) * fade));
        }
        // Runes burning on the outer face of some blocks
        VertexConsumer glyphs = buffer.getBuffer(RenderType.eyes(RuneRender.GLYPHS));
        for (Rampart.Cell c : cells) {
            float landed = age - c.delay() - Rampart.CELL_RISE;
            int seed = (int) foot.offset(c.dx(), c.dy(), c.dz()).asLong();
            if (landed < 0 || c.face() < 0 || RuneRender.hash(seed * 7) >= .14f) {
                continue;
            }
            float strength = (.55f + .25f * Mth.sin(age * .1f + seed) + Math.max(0, 1 - landed / 10)) * fade;
            rune(glyphs, poseStack, c, seed, Math.min(1, strength));
        }
        super.render(rampart, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** A rune burning on the outer face of a block. */
    private static void rune(VertexConsumer glyphs, PoseStack poseStack, Rampart.Cell c, int seed, float strength) {
        int[] f = Rampart.step(c.face());
        poseStack.pushPose();
        poseStack.translate(c.dx() + f[0] * .505f, c.dy() + .5f, c.dz() + f[1] * .505f);
        poseStack.mulPose(Axis.YP.rotation((float) Mth.atan2(f[0], f[1])));
        RuneRender.glyph(glyphs, poseStack.last(), seed, .7f, WallRender.ember(.8f, strength), GlyphRender.FULL);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRender(Rampart rampart, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(rampart.getBoundingBox().inflate(14, 8, 14));
    }

    @Override
    public ResourceLocation getTextureLocation(Rampart rampart) {
        return WallRender.SIGIL;
    }
}
