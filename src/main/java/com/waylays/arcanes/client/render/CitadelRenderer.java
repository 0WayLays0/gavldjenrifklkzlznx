package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.wall.Citadel;
import com.waylays.arcanes.spells.wall.WallFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The Tyrant's Fortress: a great sigil burning under it, eight towers of brick (two blocks square, eight high, crowned
 * with battlements and a brazier of embers) bursting up one after another round the wall (the wall itself is a
 * rampart of real blocks); their mortar glows. When it collapses, the towers burst outward in flying bricks.
 */
public class CitadelRenderer extends EntityRenderer<Citadel> {
    public CitadelRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(Citadel citadel, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = citadel.tickCount + partialTicks;
        int collapsed = citadel.getCollapsed();
        float since = collapsed >= 0 ? age - collapsed : -1;
        float glow = collapsed >= 0 ? Math.max(0, 1 - since / 12) : Mth.clamp(age / 10, 0, 1) * (.45f + .15f * Mth.sin(age * .1f));
        WallRender.sigil(poseStack, buffer, Citadel.RADIUS + 3, glow, age * .6f);
        int seed = citadel.getId() * 131;
        // Three passes, the bricks, their mortar, then the braziers: a buffer must not be written to again once another
        // one has been asked for (asking for it sends the first off)
        int rows = (int) Citadel.TOWER_HEIGHT;
        for (int pass = 0; pass < 3; pass++) {
            if (collapsed >= 0 && pass > 0) {
                break;
            }
            VertexConsumer stroke = pass == 1 ? buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE)) : null;
            for (int i = 0; i < Citadel.TOWERS; i++) {
                Vec3 at = citadel.tower(i);
                float risen = citadel.risen(i, partialTicks);
                if (risen <= .001f) {
                    continue;
                }
                Vec3 out = at.normalize();
                poseStack.pushPose();
                poseStack.translate(at.x, collapsed >= 0 ? 0 : -(1 - risen) * Citadel.TOWER_HEIGHT, at.z);
                if (pass == 2) {
                    // The brazier on top
                    poseStack.translate(0, rows + .9f, 0);
                    poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
                    float flicker = .75f + .25f * Mth.sin(age * .9f + i * 2);
                    GlyphRender.halo(buffer, poseStack.last(), GlyphRender.GLOW, 1.6f, WallRender.ember(.6f, .8f * flicker));
                    GlyphRender.halo(buffer, poseStack.last(), GlyphRender.GLOW, .7f, WallRender.ember(1, flicker));
                    poseStack.popPose();
                    continue;
                }
                for (int r = 0; r < rows; r++) {
                    for (int k = 0; k < 4; k++) {
                        float x = (k & 1) - .5f;
                        float z = (k >> 1) - .5f;
                        int s = seed + i * 101 + r * 7 + k;
                        BlockState state = WallFx.brick(s);
                        if (collapsed >= 0) {
                            float h = RuneRender.hash(s);
                            WallRender.fragment(state, poseStack, buffer, packedLight, x, r + .5f, z,
                                    (float) out.x * (.2f + .35f * h) + (RuneRender.hash(s + 1) - .5f) * .2f, .25f + .3f * RuneRender.hash(s + 2),
                                    (float) out.z * (.2f + .35f * h) + (RuneRender.hash(s + 3) - .5f) * .2f, since, Citadel.AFTER, .9f, s);
                        } else if (pass == 0) {
                            WallRender.brick(state, poseStack, buffer, packedLight, x, r, z, 1.001f);
                        } else if (r % 2 == 0) {
                            WallRender.mortar(stroke, poseStack.last(), x, r, z, Math.abs(out.x) > Math.abs(out.z) ? 1 : 0, .07f,
                                    WallRender.ember(.5f, .35f + .1f * Mth.sin(age * .2f + r + k)));
                        }
                    }
                }
                if (collapsed < 0 && pass == 0) {
                    // Battlements
                    for (int k = 0; k < 4; k++) {
                        float x = ((k & 1) * 2 - 1) * .75f;
                        float z = ((k >> 1) * 2 - 1) * .75f;
                        RuneRender.block(WallFx.brick(seed + i * 11 + k), poseStack, buffer, packedLight, x, rows, z, .5f, .6f, .5f, 0, 0);
                    }
                }
                poseStack.popPose();
            }
        }
        super.render(citadel, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(Citadel citadel, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(Citadel citadel) {
        return WallRender.SIGIL;
    }
}
