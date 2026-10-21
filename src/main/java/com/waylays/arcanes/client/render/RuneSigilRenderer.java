package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.rune.RuneSigil;
import com.waylays.arcanes.spells.rune.RuneFx;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Runes carved into the ground: the ward's circle under a curtain of light with glyphs standing around it, the path's
 * tiles lighting up in a wave towards its end, the snare's faint rune and the stone spikes it bursts into.
 */
public class RuneSigilRenderer extends EntityRenderer<RuneSigil> {
    private static final int FULL = LightTexture.FULL_BRIGHT;
    private static final BlockState SPIKE = Blocks.POINTED_DRIPSTONE.defaultBlockState()
            .setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.UP).setValue(PointedDripstoneBlock.THICKNESS, DripstoneThickness.TIP);
    private static final BlockState SPIKE_BASE = Blocks.POINTED_DRIPSTONE.defaultBlockState()
            .setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.UP).setValue(PointedDripstoneBlock.THICKNESS, DripstoneThickness.FRUSTUM);

    public RuneSigilRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RuneSigil sigil, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = sigil.tickCount + partialTicks;
        switch (sigil.getKind()) {
            case WARD -> ward(sigil, age, partialTicks, poseStack, buffer, packedLight);
            case PATH -> path(sigil, age, partialTicks, poseStack, buffer, packedLight);
            case SNARE -> snare(sigil, age, partialTicks, poseStack, buffer, packedLight);
        }
        super.render(sigil, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private void ward(RuneSigil sigil, float age, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float lit = sigil.strength(partialTicks);
        if (lit <= 0) {
            return;
        }
        float appear = 1 - (1 - lit) * (1 - lit);
        float r = sigil.getSize();
        float breathe = .8f + .2f * Mth.sin(age * .12f);
        poseStack.pushPose();
        poseStack.translate(0, .02, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(age * .5f));
        // The grooves cut into the ground, lit by the world, then the same lines glowing
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.entityTranslucent(RuneRender.RING_CARVED)), poseStack.last(), r * 1.1f, RuneRender.alpha(0xFFFFFF, appear), light);
        poseStack.translate(0, .01, 0);
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), r * 1.1f * appear, RuneRender.additive(RuneFx.GLOW, lit * breathe), FULL);
        // A smaller ring of runes turning the other way
        poseStack.mulPose(Axis.YP.rotationDegrees(-age * 1.6f));
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), r * .55f * appear, RuneRender.additive(RuneFx.DEEP, lit * .9f), FULL);
        poseStack.popPose();

        // A curtain of light rising from the edge, its glow drifting up
        curtain(poseStack, buffer, r * appear, 1.7f * appear, age, RuneRender.additive(RuneFx.GLOW, lit * .55f));

        // Eight glyphs standing around the circle, bobbing, always turned to the viewer
        for (int i = 0; i < 8; i++) {
            float a = i * Mth.TWO_PI / 8 + age * .01f;
            float ignite = Mth.clamp(age / RuneSigil.LIGHT_TICKS * 8 - i, 0, 1);
            if (ignite <= 0) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(Mth.cos(a) * r * .92f, .9f + .12f * Mth.sin(age * .1f + i), Mth.sin(a) * r * .92f);
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            RuneRender.glyph(buffer.getBuffer(RenderType.eyes(RuneRender.GLYPHS)), poseStack.last(), sigil.getId() + i * 5, .55f,
                    RuneRender.additive(RuneFx.SILVER, lit * ignite * .95f), FULL);
            RenderHelper.billboard(buffer.getBuffer(RenderType.eyes(RuneRender.GLOW)), poseStack.last(), .45f, RuneRender.additive(RuneFx.DEEP, lit * ignite * .6f), FULL);
            poseStack.popPose();
        }
    }

    /** A ring-shaped band of light seen from inside and out, fading out towards its top, its streaks drifting up. */
    private static void curtain(PoseStack poseStack, MultiBufferSource buffer, float radius, float height, float age, int color) {
        int n = Mth.clamp(Mth.ceil(Mth.TWO_PI * radius / .7f), 16, 72);
        VertexConsumer c = buffer.getBuffer(RenderType.eyes(RuneRender.CURTAIN));
        PoseStack.Pose pose = poseStack.last();
        // Additive: black is invisible, so the top edge fades away
        int top = 0xFF000000;
        float v0 = -age * .02f;
        float v1 = v0 + 1;
        for (int i = 0; i < n; i++) {
            float a0 = i * Mth.TWO_PI / n;
            float a1 = (i + 1) * Mth.TWO_PI / n;
            float x0 = Mth.cos(a0) * radius, z0 = Mth.sin(a0) * radius;
            float x1 = Mth.cos(a1) * radius, z1 = Mth.sin(a1) * radius;
            float u0 = i / (float) n * 6, u1 = (i + 1) / (float) n * 6;
            float nx = Mth.cos((a0 + a1) * .5f), nz = Mth.sin((a0 + a1) * .5f);
            // Outside face, then inside face
            RenderHelper.vertex(c, pose, x0, 0, z0, u0, v1, color, FULL, nx, 0, nz);
            RenderHelper.vertex(c, pose, x0, height, z0, u0, v0, top, FULL, nx, 0, nz);
            RenderHelper.vertex(c, pose, x1, height, z1, u1, v0, top, FULL, nx, 0, nz);
            RenderHelper.vertex(c, pose, x1, 0, z1, u1, v1, color, FULL, nx, 0, nz);
            RenderHelper.vertex(c, pose, x1, 0, z1, u1, v1, color, FULL, -nx, 0, -nz);
            RenderHelper.vertex(c, pose, x1, height, z1, u1, v0, top, FULL, -nx, 0, -nz);
            RenderHelper.vertex(c, pose, x0, height, z0, u0, v0, top, FULL, -nx, 0, -nz);
            RenderHelper.vertex(c, pose, x0, 0, z0, u0, v1, color, FULL, -nx, 0, -nz);
        }
    }

    private void path(RuneSigil sigil, float age, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float lit = sigil.strength(partialTicks);
        if (lit <= 0) {
            return;
        }
        float length = sigil.getSize();
        float width = sigil.getWidth();
        int tiles = Math.max(1, Math.round(length / 1.25f));
        float step = length / tiles;
        float[] heights = groundHeights(sigil, tiles, step);
        Vec3 f = sigil.forward();
        float yaw = sigil.getYRot();
        for (int k = 0; k < tiles; k++) {
            // The tiles are carved one after another as the path appears
            float carved = Mth.clamp(age * 2.2f - k, 0, 1);
            if (carved <= 0) {
                continue;
            }
            float along = (k + .5f) * step;
            // A wave of light runs along the path, towards its end
            float wave = Math.max(0, Mth.sin(age * .3f - k * .7f));
            float glow = lit * (.35f + .65f * wave * wave);
            poseStack.pushPose();
            poseStack.translate(f.x * along, heights[k] + .02f, f.z * along);
            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
            float half = width * .46f * carved;
            RenderHelper.groundQuad(buffer.getBuffer(RenderType.entityTranslucent(RuneRender.TILE)), poseStack.last(), half, RuneRender.alpha(0xFFFFFF, lit), light);
            poseStack.translate(0, .012, 0);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            RuneRender.glyph(buffer.getBuffer(RenderType.eyes(RuneRender.GLYPHS)), poseStack.last(), sigil.getId() * 7 + k * 3, width * .55f * carved,
                    RuneRender.additive(RuneFx.GLOW, glow), FULL);
            poseStack.popPose();
        }
    }

    /** The height of the ground under each tile of a path, looked up once and kept on the entity. */
    private static float[] groundHeights(RuneSigil sigil, int tiles, float step) {
        if (sigil.clientCache instanceof float[] cached && cached.length == tiles) {
            return cached;
        }
        float[] out = new float[tiles];
        Level level = sigil.level();
        Vec3 f = sigil.forward();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int k = 0; k < tiles; k++) {
            double x = sigil.getX() + f.x * (k + .5) * step;
            double z = sigil.getZ() + f.z * (k + .5) * step;
            int top = Mth.floor(sigil.getY()) + 2;
            out[k] = 0;
            for (int y = top; y >= top - 5; y--) {
                pos.set(Mth.floor(x), y, Mth.floor(z));
                VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
                if (!shape.isEmpty()) {
                    out[k] = (float) (y + shape.max(Direction.Axis.Y) - sigil.getY());
                    break;
                }
            }
        }
        sigil.clientCache = out;
        return out;
    }

    private void snare(RuneSigil sigil, float age, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float burst = sigil.burstAge(partialTicks);
        float r = RuneSigil.SNARE_RADIUS;
        if (burst < 0) {
            float lit = sigil.strength(partialTicks);
            // Barely visible: the grooves, and a faint glow breathing slowly once armed
            float armed = Mth.clamp((age - RuneSigil.ARM_TICKS) / 10, 0, 1);
            poseStack.pushPose();
            poseStack.translate(0, .02, 0);
            poseStack.mulPose(Axis.YP.rotationDegrees(sigil.getId() * 37));
            RenderHelper.groundQuad(buffer.getBuffer(RenderType.entityTranslucent(RuneRender.RING_CARVED)), poseStack.last(), r, RuneRender.alpha(0xFFFFFF, .7f * lit), light);
            poseStack.translate(0, .01, 0);
            float breathe = .18f + .14f * Mth.sin(age * .08f);
            RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), r,
                    RuneRender.additive(RuneFx.GLOW, lit * (.12f + armed * breathe)), FULL);
            poseStack.popPose();
            return;
        }
        float t = burst / RuneSigil.BURST_TICKS;
        // A flash of light spreading from the rune
        poseStack.pushPose();
        poseStack.translate(0, .04, 0);
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.PULSE)), poseStack.last(), r + burst * .25f, RuneRender.additive(RuneFx.GLOW, 1 - t), FULL);
        poseStack.popPose();
        // Stone spikes thrust up out of the ground, then sink back
        float up = Mth.clamp(burst / 2.5f, 0, 1);
        float down = Mth.clamp((burst - 9) / 7, 0, 1);
        float grow = up * (1 - down);
        if (grow <= 0) {
            return;
        }
        for (int i = 0; i < 9; i++) {
            float a = i * Mth.TWO_PI / 9 + RuneRender.hash(sigil.getId() + i) * .6f;
            float d = i == 0 ? 0 : .45f + RuneRender.hash(sigil.getId() * 3 + i) * (RuneSigil.BURST_RADIUS - 1.1f);
            float h = (i == 0 ? 2.4f : 1.2f + RuneRender.hash(sigil.getId() * 5 + i) * 1.1f) * grow;
            float lean = d * 9;
            poseStack.pushPose();
            poseStack.translate(Mth.cos(a) * d, 0, Mth.sin(a) * d);
            // Leaning outwards
            poseStack.mulPose(Axis.YP.rotationDegrees(90 - a * Mth.RAD_TO_DEG));
            poseStack.mulPose(Axis.XP.rotationDegrees(lean));
            RuneRender.block(SPIKE_BASE, poseStack, buffer, light, 0, -.2f, 0, .9f, h * .55f, .9f, 0, 0);
            RuneRender.block(SPIKE, poseStack, buffer, light, 0, -.2f + h * .55f, 0, .9f, h * .55f, .9f, 0, 0);
            poseStack.popPose();
        }
    }

    @Override
    public boolean shouldRender(RuneSigil entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(RuneSigil entity) {
        return RuneRender.GLYPHS;
    }
}
