package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.rune.ElderCircle;
import com.waylays.arcanes.spells.rune.RuneFx;
import net.minecraft.client.renderer.LightTexture;
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

/**
 * Circle of the Elders: a great carved circle on the ground, eight tall runestones standing on it (rising one after
 * another), bands of light linking their tops, and at every pulse a ring of healing light spreading and arcs of
 * runic lightning leaping from the stones to the enemies inside.
 */
public class ElderCircleRenderer extends EntityRenderer<ElderCircle> {
    private static final int FULL = LightTexture.FULL_BRIGHT;
    private static final BlockState POLISHED = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
    private static final BlockState TILES = Blocks.DEEPSLATE_TILES.defaultBlockState();
    private static final BlockState CHISELED = Blocks.CHISELED_DEEPSLATE.defaultBlockState();
    private static final float HEIGHT = 3.8f;

    public ElderCircleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ElderCircle circle, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = circle.tickCount + partialTicks;
        float r = circle.getRadius();
        float appear = Mth.clamp(age / 16, 0, 1);
        float fade = Mth.clamp(1 - (age - circle.getLife()) / ElderCircle.SINK_TICKS, 0, 1);
        float since = circle.sincePulse(partialTicks);
        float flash = since >= 0 ? Math.max(0, 1 - since / 12f) : 0;
        float lit = appear * fade;

        // The great circle on the ground
        poseStack.pushPose();
        poseStack.translate(0, .03, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(age * .3f));
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.entityTranslucent(RuneRender.RING_CARVED)), poseStack.last(), (r + 1.4f) * appear, RuneRender.alpha(0xFFFFFF, lit), packedLight);
        poseStack.translate(0, .01, 0);
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), (r + 1.4f) * appear,
                RuneRender.additive(RuneFx.GLOW, lit * (.55f + .45f * flash)), FULL);
        poseStack.mulPose(Axis.YP.rotationDegrees(-age * 1.1f));
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), r * .55f * appear, RuneRender.additive(RuneFx.DEEP, lit), FULL);
        poseStack.popPose();

        // The healing ring of each pulse
        if (since >= 0 && since < 18) {
            float t = since / 18;
            poseStack.pushPose();
            poseStack.translate(0, .06, 0);
            RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.PULSE)), poseStack.last(), r * (1 - (1 - t) * (1 - t)),
                    RuneRender.additive(RuneFx.MEND, (1 - t) * .9f), FULL);
            poseStack.popPose();
        }

        // The stones
        Vec3 origin = circle.position();
        Vec3[] tops = new Vec3[ElderCircle.STONES];
        for (int i = 0; i < ElderCircle.STONES; i++) {
            Vec3 at = circle.stone(i).subtract(origin);
            float risen = circle.risen(i, partialTicks);
            tops[i] = at.add(0, HEIGHT * risen - .3f, 0);
            if (risen <= .001f) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(at.x, -(1 - risen) * HEIGHT, at.z);
            // Facing the middle of the circle
            float toCentre = (float) (Mth.atan2(-at.x, -at.z) * Mth.RAD_TO_DEG);
            poseStack.mulPose(Axis.YP.rotationDegrees(toCentre));
            float lean = (RuneRender.hash(circle.getId() + i) - .5f) * 5;
            RuneRender.block(POLISHED, poseStack, buffer, packedLight, 0, 0, 0, 1.5f, 1.4f, 1.15f, 0, 0);
            RuneRender.block(TILES, poseStack, buffer, packedLight, 0, 1.4f, 0, 1.3f, 1.45f, 1.0f, lean * .4f, 0);
            RuneRender.block(CHISELED, poseStack, buffer, packedLight, 0, 2.85f, 0, 1.0f, .95f, .8f, lean, lean * .5f);
            int color = RuneRender.additive(RuneFx.GLOW, (.6f + .4f * flash) * fade);
            for (int side = 0; side < 2; side++) {
                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(side * 180));
                for (int k = 0; k < 3; k++) {
                    poseStack.pushPose();
                    poseStack.translate(0, .7f + k * 1.05f, (k == 2 ? .4f : k == 1 ? .5f : .575f) + .012f);
                    RuneRender.glyph(buffer.getBuffer(RenderType.eyes(RuneRender.GLYPHS)), poseStack.last(), circle.getId() + i * 3 + k + side * 9,
                            .78f - k * .1f, color, FULL);
                    poseStack.popPose();
                }
                poseStack.popPose();
            }
            poseStack.popPose();
        }

        // Bands of light from stone top to stone top
        float band = lit * (.3f + .5f * flash);
        if (band > 0) {
            for (int i = 0; i < ElderCircle.STONES; i++) {
                Vec3 a = tops[i];
                Vec3 b = tops[(i + 1) % ElderCircle.STONES];
                if (circle.risen(i, partialTicks) < .95f || circle.risen((i + 1) % ElderCircle.STONES, partialTicks) < .95f) {
                    continue;
                }
                RenderHelper.strand(buffer.getBuffer(RenderType.eyes(RuneRender.ARC)), poseStack.last(), a, b, .22f, 3, RuneRender.additive(RuneFx.GLOW, band), FULL);
            }
        }

        // Arcs of runic lightning to the enemies struck by the last pulse
        if (since >= 0 && since < ElderCircle.ARC_TICKS) {
            float arcFade = 1 - since / ElderCircle.ARC_TICKS;
            int pulseTick = Mth.floor(age - since);
            for (int k = 0; k < ElderCircle.MAX_ARCS; k++) {
                Entity target = circle.level().getEntity(circle.arcTarget(k));
                if (target == null) {
                    continue;
                }
                Vec3 to = target.getPosition(partialTicks).add(0, target.getBbHeight() * .5, 0).subtract(origin);
                Vec3 from = tops[circle.nearestStone(target.position())];
                lightning(buffer, poseStack, from, to, circle.getId() * 131 + pulseTick * 17 + k, arcFade);
                poseStack.pushPose();
                poseStack.translate(to.x, to.y, to.z);
                poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
                RenderHelper.billboard(buffer.getBuffer(RenderType.eyes(RuneRender.GLOW)), poseStack.last(), .7f, RuneRender.additive(RuneFx.SILVER, arcFade), FULL);
                poseStack.popPose();
            }
        }
        super.render(circle, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** A jagged bolt from {@code from} to {@code to}: a bright core in a wider glow, its kinks fixed for one pulse. */
    private static void lightning(MultiBufferSource buffer, PoseStack poseStack, Vec3 from, Vec3 to, int seed, float strength) {
        int segments = 7;
        Vec3 d = to.subtract(from);
        Vec3 side = d.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 up = d.cross(side).normalize();
        Vec3[] points = new Vec3[segments + 1];
        for (int i = 0; i <= segments; i++) {
            float t = i / (float) segments;
            float wobble = i == 0 || i == segments ? 0 : .55f * Mth.sin(t * Mth.PI);
            points[i] = from.add(d.scale(t))
                    .add(side.scale((RuneRender.hash(seed + i * 7) - .5f) * 2 * wobble))
                    .add(up.scale((RuneRender.hash(seed + i * 13) - .5f) * 2 * wobble));
        }
        VertexConsumer glow = buffer.getBuffer(RenderType.eyes(RuneRender.ARC));
        for (int i = 0; i < segments; i++) {
            RenderHelper.strand(glow, poseStack.last(), points[i], points[i + 1], .5f, 2, RuneRender.additive(RuneFx.DEEP, strength), FULL);
            RenderHelper.strand(glow, poseStack.last(), points[i], points[i + 1], .16f, 2, RuneRender.additive(RuneFx.SILVER, strength), FULL);
        }
    }

    @Override
    public boolean shouldRender(ElderCircle entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(ElderCircle entity) {
        return RuneRender.GLYPHS;
    }
}
